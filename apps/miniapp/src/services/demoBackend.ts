/**
 * Local mock backend for the demo mode — replaces fetch() in api/client.ts when
 * isDemo() is true. Only server-backed routes are stubbed; the CV pipeline
 * (pose + engine) stays real and runs on-device.
 *
 * Analysis sessions are persisted in localStorage (results only — video blobs
 * live only for the current session as object URLs). Shapes mirror the real API:
 * quota exhaustion throws ApiError(402) exactly like AnalysesService.create.
 */

import { ApiError } from '../api/client';
import { DEMO_DRILLS } from './demoDrills';
import { useAppStore } from '../stores/app';
import { t } from '../i18n/T';
import type { UserDto } from '@tennis-ai/shared';

/** Same mock user as stores/auth.ts (DEMO_USER) — served for GET /users/me in demo. */
const DEMO_USER: UserDto = {
  id: 'demo-user',
  telegramId: 0,
  firstName: 'Demo',
  language: 'ru',
  xp: 0,
  quota: { plan: 'free', analysesUsed: 0, analysesLimit: 9, periodEndsAt: null },
  profile: { dominantHand: 'right' },
};

const DEMO_QUOTA_LIMIT = 9;
const STATE_KEY = 'tennis_ai_demo_state';

/** Analysis document as stored by the demo backend (mirrors the API document: results merged in). */
interface DemoAnalysis {
  id: string;
  strokeType: string;
  status: 'created' | 'analyzing' | 'completed' | 'failed';
  videos: { original: string | null };
  createdAt: string;
  trimStartSec?: number | null;
  trimEndSec?: number | null;
  fps?: number | null;
  frameCount?: number | null;
  phases?: unknown[];
  metrics?: unknown[];
  overall?: { score: number | null } | null;
  topWeaknesses?: unknown[];
  notes?: unknown[];
}

interface DemoState {
  analyses: DemoAnalysis[];
  analysesUsed: number;
}

function loadState(): DemoState {
  try {
    const raw = localStorage.getItem(STATE_KEY);
    if (raw) return JSON.parse(raw) as DemoState;
  } catch {
    // Corrupted state — start fresh.
  }
  return { analyses: [], analysesUsed: 0 };
}

function saveState(state: DemoState): void {
  try {
    localStorage.setItem(STATE_KEY, JSON.stringify(state));
  } catch {
    // Storage full/unavailable — demo results are best-effort.
  }
}

function lang(): 'ru' | 'en' {
  return useAppStore().lang;
}

function newId(): string {
  return typeof crypto !== 'undefined' && 'randomUUID' in crypto
    ? crypto.randomUUID()
    : `demo-${Date.now()}-${Math.random().toString(36).slice(2, 10)}`;
}

export async function demoFetch<T>(path: string, opts: RequestInit = {}): Promise<T> {
  const route = path.split('?')[0];
  const body: Record<string, unknown> = opts.body ? JSON.parse(String(opts.body)) : {};
  const state = loadState();

  // --- Users ---
  if (route === '/users/me') return DEMO_USER as T;

  // --- Uploads (no S3 in demo — uploads are skipped in client.ts) ---
  if (route === '/uploads/presign' && opts.method === 'POST') {
    return { uploadUrl: 'demo://skip', key: `demo/${newId()}` } as T;
  }
  if (route === '/analysis/detect-stroke' && opts.method === 'POST') {
    // Real stroke classification is server-side; demo defaults to serve (RecordView has manual chips).
    return { strokeType: 'serve' } as T;
  }

  // --- Analyses ---
  if (route === '/analyses') {
    if (opts.method === 'POST') {
      if (state.analysesUsed >= DEMO_QUOTA_LIMIT) {
        throw new ApiError(
          402,
          `Quota exhausted: free plan, ${state.analysesUsed}/${DEMO_QUOTA_LIMIT} analyses used`,
        );
      }
      state.analysesUsed += 1;
      const doc: DemoAnalysis = {
        id: newId(),
        strokeType: String(body.strokeType ?? 'serve'),
        status: 'created',
        videos: { original: null },
        createdAt: new Date().toISOString(),
        trimStartSec: typeof body.trimStartSec === 'number' ? body.trimStartSec : null,
        trimEndSec: typeof body.trimEndSec === 'number' ? body.trimEndSec : null,
        fps: null,
        frameCount: null,
        phases: [],
        metrics: [],
        overall: null,
        topWeaknesses: [],
        notes: [],
      };
      state.analyses.push(doc);
      saveState(state);
      return { id: doc.id, status: doc.status, quotaRemaining: DEMO_QUOTA_LIMIT - state.analysesUsed } as T;
    }
    return state.analyses as T;
  }

  const analysisMatch = route.match(/^\/analyses\/([^/]+)(?:\/(results|video|annotated))?$/);
  if (analysisMatch) {
    const [, id, sub] = analysisMatch;
    const doc = state.analyses.find((a) => a.id === id);
    if (!doc) throw new ApiError(404, 'analysis not found');
    if (sub === 'results' && opts.method === 'POST') {
      doc.status = 'completed';
      doc.fps = typeof body.fps === 'number' ? body.fps : null;
      doc.frameCount = typeof body.frameCount === 'number' ? body.frameCount : null;
      doc.phases = Array.isArray(body.phases) ? body.phases : [];
      doc.metrics = Array.isArray(body.metrics) ? body.metrics : [];
      doc.overall = (body.overall as { score: number | null } | undefined) ?? null;
      doc.topWeaknesses = Array.isArray(body.topWeaknesses) ? body.topWeaknesses : [];
      doc.notes = Array.isArray(body.notes) ? body.notes : [];
      saveState(state);
      return doc as T;
    }
    if (sub === 'annotated') return doc as T; // no annotated rendering in demo
    if (sub === 'video') return { url: null } as T; // original blob is session-only
    return doc as T;
  }

  // --- Drills ---
  if (route === '/drills') return DEMO_DRILLS as T;
  if (route === '/drills/recommended') return DEMO_DRILLS.slice(0, 3) as T;

  // --- Coach (Axel) ---
  if (route === '/coach/chat' && opts.method === 'POST') {
    return { reply: t(lang(), 'demo_coach_reply') } as T;
  }

  // --- Payments (PaywallView short-circuits in demo; this is a fallback) ---
  if (route === '/payments/invoice' && opts.method === 'POST') {
    throw new ApiError(400, t(lang(), 'demo_paywall'));
  }

  throw new ApiError(404, `demo backend: unhandled route ${route}`);
}
