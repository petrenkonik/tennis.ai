import {
  ForbiddenException,
  Injectable,
  Logger,
  ServiceUnavailableException,
} from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model } from 'mongoose';
import { Env } from '../../config/env';
import { Analysis, AnalysisDocument } from '../../common/schemas/analysis.schema';
import { Drill, DrillDocument } from '../../common/schemas/drill.schema';
import { Plan, PlanDocument } from '../../common/schemas/plan.schema';
import { User, UserDocument } from '../../common/schemas/user.schema';
import { S3Service } from '../uploads/s3.service';
import { UsersService } from '../users/users.service';
import type { JwtPayload } from '../../common/current-user.decorator';
import { AXEL_TOOLS, TOOL_CALL_NUDGE } from './axel-tools';
import type { DetectStrokeResponse } from '@tennis-ai/shared';

/** Action for the mini app: a tool whose execution happens on the client. */
export interface ClientAction {
  tool: string;
  args: Record<string, unknown>;
}

export interface CoachChatResult {
  reply: string;
  actions: ClientAction[];
}

interface GlmToolCall {
  id: string;
  type: 'function';
  function: { name: string; arguments: string };
}

interface GlmMessage {
  role: 'system' | 'user' | 'assistant' | 'tool';
  content: string | null;
  tool_calls?: GlmToolCall[];
  tool_call_id?: string;
}

/**
 * Axel system prompt — assembled from verbatim fragments of the original
 * Dart AOT (reference/dart-aot-recovery/AXEL_RECOVERY.md; in the original it
 * is also assembled by interpolating constants, there is no single string in the snapshot).
 */
const AXEL_SYSTEM_PROMPT = `You are Axel, an expert tennis coach inside the Tennis AI app. You are talking to the player themselves.
You coach them about their stroke technique based on biomechanical analysis: the stroke is split into phases M0-M6 (ready, stance/split step, toss/unit turn, loading, extension/drive, contact point, recovery) and each metric is green when its value is inside the range the best players sit in, red when it is outside.

Language: always reply in the language the player is using in the conversation. Your reasoning may be in any language; the reply is always in theirs.

How to read results, once and briefly when results are first discussed: red is outside the range the best players sit in, green is inside it. This is the only time you will say it. A score is a score out of 100, never a percentage.

Rules:
- Use your tools to fetch real data (history, drills, plan, profile) instead of guessing.
- When the player wants to record or analyze a stroke, use analyze_video -- never navigate to a calibration screen.
- During onboarding or missing-fact questions: ask ONE question per message, store each answer with save_player_profile.
- Never invent a fault or a correction. If detailed metrics did not come through, say the score and point to the screen.
- Nothing flagged in an analysis: say their result is in and what it is, explain nothing is red, and end on a real question.
- When recommending a drill after a first analysis, present it as a first suggestion drawn from this single analysis; invite more analyses to sharpen the picture -- an invitation, never a condition.
- Be concise, warm and concrete. Coach like a person, not like a report.`;

const COACH_MODE_SUFFIX = `

You are talking to a COACH, not the player. They need data they can act on and relay to their student: metrics, trends, comparisons, what to work on in the next lesson. Do not address them as if they hit the strokes.`;

const MAX_TOOL_ROUNDS = 6;

@Injectable()
export class CoachService {
  private readonly logger = new Logger(CoachService.name);

  constructor(
    @InjectModel(Analysis.name) private readonly analyses: Model<AnalysisDocument>,
    @InjectModel(Drill.name) private readonly drills: Model<DrillDocument>,
    @InjectModel(Plan.name) private readonly plans: Model<PlanDocument>,
    @InjectModel(User.name) private readonly usersModel: Model<UserDocument>,
    private readonly s3: S3Service,
    private readonly users: UsersService,
  ) {}

  async chat(
    u: JwtPayload,
    message: string,
    analysisId?: string,
    history: Array<{ role: 'user' | 'assistant'; content: string }> = [],
  ): Promise<CoachChatResult> {
    const key = Env.glmKey();
    if (!key) throw new ServiceUnavailableException('GLM_API_KEY is not configured');

    const user = await this.users.me(u.sub);

    // Analysis context (when the chat is bound to a session) — as in the original, the data goes into the prompt.
    let analysisContext = '';
    if (analysisId) {
      const doc = await this.analyses.findById(analysisId);
      if (!doc || doc.userId !== u.sub) throw new ForbiddenException();
      analysisContext = `\n\nAnalysis context (JSON):\n${JSON.stringify({
        strokeType: doc.strokeType,
        status: doc.status,
        phases: doc.phases,
        metrics: doc.metrics,
        overall: doc.overall,
        topWeaknesses: doc.topWeaknesses,
      })}`;
    }

    const isCoach = Array.isArray((user as unknown as { roles?: string[] }).roles)
      ? (user as unknown as { roles: string[] }).roles.includes('coach')
      : false;

    const messages: GlmMessage[] = [
      { role: 'system', content: AXEL_SYSTEM_PROMPT + (isCoach ? COACH_MODE_SUFFIX : '') },
      ...history.slice(-16).map((h) => ({ role: h.role, content: h.content }) as GlmMessage),
      {
        role: 'user',
        content: `Player: ${user.firstName ?? 'Player'}, level: ${user.profile?.level ?? 'unknown'}${analysisContext}\n\n${message}`,
      },
    ];

    const clientActions: ClientAction[] = [];

    for (let round = 0; round < MAX_TOOL_ROUNDS; round++) {
      const data = await this.completion(key, messages);
      const choice = data.choices?.[0]?.message;
      if (!choice) throw new ServiceUnavailableException('Empty GLM response');

      const toolCalls = choice.tool_calls ?? [];
      if (toolCalls.length === 0) {
        return { reply: (choice.content ?? '').trim(), actions: clientActions };
      }

      messages.push({
        role: 'assistant',
        content: choice.content ?? '',
        tool_calls: toolCalls,
      });

      for (const call of toolCalls) {
        let args: Record<string, unknown> = {};
        try {
          args = JSON.parse(call.function.arguments || '{}') as Record<string, unknown>;
        } catch {
          args = {};
        }
        const result = await this.executeTool(u, call.function.name, args, clientActions);
        messages.push({
          role: 'tool',
          tool_call_id: call.id,
          content: JSON.stringify(result).slice(0, 6000),
        });
      }
    }

    // Ran out of rounds — ask for a final answer without tools.
    const final = await this.completion(key, messages, /* disableTools */ true);
    return { reply: (final.choices?.[0]?.message?.content ?? '').trim(), actions: clientActions };
  }

  private async completion(
    key: string,
    messages: GlmMessage[],
    disableTools = false,
  ): Promise<{
    choices?: Array<{ message: { content: string | null; tool_calls?: GlmToolCall[] } }>;
  }> {
    const body: Record<string, unknown> = {
      model: Env.glmModel(),
      messages,
      temperature: 0.6,
      max_tokens: 1024,
    };
    if (!disableTools) {
      body.tools = AXEL_TOOLS.map((t) => ({ type: 'function', function: t.function }));
      body.tool_choice = 'auto';
    }
    const res = await fetch(`${Env.glmBaseUrl()}/chat/completions`, {
      method: 'POST',
      headers: { 'content-type': 'application/json', authorization: `Bearer ${key}` },
      body: JSON.stringify(body),
    });
    if (!res.ok) {
      this.logger.error(`glm error ${res.status}: ${(await res.text()).slice(0, 500)}`);
      throw new ServiceUnavailableException('GLM request failed');
    }
    return (await res.json()) as { choices?: Array<{ message: { content: string | null; tool_calls?: GlmToolCall[] } }> };
  }

  /** Tool execution. SERVER — here; CLIENT — passed to the mini app as an action. */
  private async executeTool(
    u: JwtPayload,
    name: string,
    args: Record<string, unknown>,
    clientActions: ClientAction[],
  ): Promise<unknown> {
    const tool = AXEL_TOOLS.find((t) => t.function.name === name);
    if (!tool) return { error: `unknown tool: ${name}` };

    if (tool.execution === 'client') {
      clientActions.push({ tool: name, args });
      return { ok: true, handedToClient: name };
    }

    switch (name) {
      case 'fetch_analysis_history':
        return this.fetchHistory(u);
      case 'fetch_recommended_drills':
        return this.fetchDrills(u, args);
      case 'get_training_plan':
        return this.fetchPlan(u);
      case 'get_player_profile':
        return this.users.me(u.sub);
      case 'save_player_profile':
        return this.saveProfile(u, args);
      case 'complete_onboarding':
        await this.usersModel.updateOne({ _id: u.sub }, { $set: { 'profile.onboarded': true } });
        return { ok: true };
      case 'set_session_intent':
        await this.usersModel.updateOne(
          { _id: u.sub },
          { $set: { 'profile.sessionIntent': args, 'profile.sessionIntentAt': new Date() } },
        );
        return { ok: true };
      case 'record_correction':
        await this.usersModel.updateOne(
          { _id: u.sub },
          {
            $set: {
              [`profile.coachingMemory.${String(args.metricCode ?? 'unknown')}`]: {
                advice: args.advice ?? '',
                worked: args.worked ?? null,
                at: new Date(),
              },
            },
          },
        );
        return { ok: true };
      case 'start_drill_walkthrough':
        return this.startWalkthrough(String(args.drillSlug ?? ''));
      case 'advance_drill_walkthrough':
        return this.advanceWalkthrough(u);
      case 'send_message_to_coach':
        // No player<->coach link in v1 yet — an honest answer instead of a silent send.
        return { sent: false, reason: 'coach link is not available yet' };
      case 'schedule_drill_reminder':
        return { scheduled: false, reason: 'push notifications are not configured yet' };
      case 'propose_drill':
        await this.usersModel.updateOne(
          { _id: u.sub },
          { $push: { 'profile.coachProposals': { ...args, at: new Date() } } },
        );
        return {
          ok: true,
          note: 'Proposal saved for expert review. You may share the idea with the player now, saying plainly it is an extra idea from you and not yet part of the validated library.',
        };
      default:
        return { error: `unhandled server tool: ${name}` };
    }
  }

  private async fetchHistory(u: JwtPayload): Promise<unknown> {
    const docs = await this.analyses
      .find({ userId: u.sub, status: 'completed' })
      .sort({ createdAt: -1 })
      .limit(50)
      .lean();
    const scores = docs.map((d) => d.overall?.score).filter((s): s is number => typeof s === 'number');
    const byStroke: Record<string, number> = {};
    for (const d of docs) byStroke[d.strokeType] = (byStroke[d.strokeType] ?? 0) + 1;
    const last = docs[0];
    const lastDate = last ? (last as unknown as { createdAt?: Date }).createdAt : undefined;
    return {
      totalSessions: docs.length,
      averageScore: scores.length ? Math.round(scores.reduce((a, b) => a + b, 0) / scores.length) : null,
      breakdownByStroke: byStroke,
      mostRecent: last
        ? {
            date: lastDate,
            strokeType: last.strokeType,
            score: last.overall?.score ?? null,
            redMetrics: (last.metrics as Array<{ id?: string; code?: string; status?: string }>)
              .filter((m) => m?.status === 'red')
              .map((m) => m.id ?? m.code ?? 'unknown'),
          }
        : null,
    };
  }

  private async fetchDrills(u: JwtPayload, args: Record<string, unknown>): Promise<unknown> {
    let weaknesses: string[] = [];
    if (typeof args.analysisId === 'string' && args.analysisId) {
      const doc = await this.analyses.findById(args.analysisId);
      if (doc && doc.userId === u.sub) weaknesses = doc.topWeaknesses ?? [];
    } else {
      const last = await this.analyses
        .findOne({ userId: u.sub, status: 'completed' })
        .sort({ createdAt: -1 })
        .lean();
      if (last) weaknesses = last.topWeaknesses ?? [];
    }
    const fault = typeof args.fault === 'string' ? args.fault : '';
    const metricNeedles = (fault ? [fault] : weaknesses).map((w) => w.toLowerCase().replace(/\s+/g, '_'));
    const pool = metricNeedles.length
      ? await this.drills.find({ targetMetrics: { $in: metricNeedles } }).limit(5).lean()
      : [];
    const drills = pool.length ? pool : await this.drills.find().limit(3).lean();
    return { weaknesses, drills: drills.map((d) => ({ slug: d.slug, title: d.title, repsSets: d.repsSets, difficulty: d.difficulty })) };
  }

  private async fetchPlan(u: JwtPayload): Promise<unknown> {
    const plan = await this.plans.findOne({ userId: u.sub, status: 'active' }).sort({ createdAt: -1 }).lean();
    return plan ?? { active: false };
  }

  private async saveProfile(u: JwtPayload, args: Record<string, unknown>): Promise<unknown> {
    const set: Record<string, unknown> = {};
    if (typeof args.firstName === 'string') set.firstName = args.firstName;
    for (const key of [
      'age', 'heightCm', 'weightKg', 'dominantHand', 'level', 'goal',
      'playStyle', 'strongestShot', 'matchProblems', 'perceivedWeakness',
    ]) {
      if (args[key] !== undefined) set[`profile.${key}`] = args[key];
    }
    if (typeof args.hasCoach === 'boolean') set['profile.hasCoach'] = args.hasCoach;
    if (typeof args.playsMatches === 'boolean') set['profile.playsMatches'] = args.playsMatches;
    if (Object.keys(set).length) await this.usersModel.updateOne({ _id: u.sub }, { $set: set });
    return { ok: true, stored: Object.keys(set) };
  }

  private async startWalkthrough(slug: string): Promise<unknown> {
    const drill = await this.drills.findOne({ slug }).lean();
    if (!drill) return { error: `no drill: ${slug}` };
    const steps = [
      `Objective: ${drill.objective ?? drill.title}`,
      `Setup: ${drill.setup ?? '—'}`,
      `How to do it: ${(drill.howTo ?? []).join(' ') || '—'}`,
      `Key points: ${(drill.keyPoints ?? []).join(' ') || '—'}`,
      `Progression: ${drill.progression ?? '—'}`,
    ];
    return { slug, steps, firstStep: steps[0], totalSteps: steps.length };
  }

  private async advanceWalkthrough(u: JwtPayload): Promise<unknown> {
    // The walkthrough state is kept by the client in the conversation: the steps have already been returned to the model.
    return { note: 'The walkthrough state lives in the conversation; say the next step from the steps already fetched.' , advanced: false, hint: 're-call start_drill_walkthrough if steps are lost' };
  }

  /** POST /api/analysis/detect-stroke — frame classification (port of CF strokeDetect) via GLM vision. */
  async detectStroke(u: JwtPayload, frameKey: string): Promise<DetectStrokeResponse> {
    const key = Env.glmKey();
    if (!key) throw new ServiceUnavailableException('GLM_API_KEY is not configured');

    const buf = await this.s3.getObjectBuffer(frameKey);
    if (!buf) throw new ServiceUnavailableException(`frame not readable: ${frameKey}`);
    // The key is issued by presign within the user's scope — someone else's frame cannot get through.
    await this.users.me(u.sub);

    const res = await fetch(`${Env.glmBaseUrl()}/chat/completions`, {
      method: 'POST',
      headers: { 'content-type': 'application/json', authorization: `Bearer ${key}` },
      body: JSON.stringify({
        model: Env.glmVisionModel(),
        temperature: 0,
        max_tokens: 8,
        messages: [
          {
            role: 'user',
            content: [
              {
                type: 'image_url',
                image_url: { url: `data:image/jpeg;base64,${buf.toString('base64')}` },
              },
              {
                type: 'text',
                text: 'Classify the tennis stroke being performed in this image. Answer with exactly one word: serve, forehand or backhand.',
              },
            ],
          },
        ],
      }),
    });
    if (!res.ok) throw new ServiceUnavailableException('GLM vision request failed');
    const data = (await res.json()) as { choices?: Array<{ message?: { content?: string } }> };
    const text = (data.choices?.[0]?.message?.content ?? '').trim().toLowerCase();
    if (text.includes('serve')) return { strokeType: 'serve' };
    if (text.includes('backhand')) return { strokeType: 'backhand' };
    if (text.includes('forehand')) return { strokeType: 'forehand' };
    throw new ServiceUnavailableException(`unrecognized stroke classification: ${text}`);
  }
}
