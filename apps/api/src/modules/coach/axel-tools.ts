/**
 * Axel coach: function-calling toolset recovered verbatim from the original
 * Dart AOT (reference/dart-aot-recovery/AXEL_RECOVERY.md — the offsets are
 * listed there too). Format — OpenAI/GLM tools: { type: 'function', function }.
 *
 * execution: 'server' — executed in CoachService; 'client' — returned to the
 * mini app as an action (navigate/analyze/voice/…).
 */
export type AxelToolExecution = 'server' | 'client';

export interface AxelTool {
  execution: AxelToolExecution;
  function: {
    name: string;
    description: string;
    parameters: Record<string, unknown>;
  };
}

/** Verbatim descriptions from the snapshot — omissions are marked with “…” */
export const AXEL_TOOLS: AxelTool[] = [
  {
    execution: 'client',
    function: {
      name: 'analyze_video',
      description:
        "Start an analysis for a stroke. Parameter mode: camera: film the stroke now (default). gallery: the player already HAS a clip on their phone and wants to import it -- 'j'ai une video', 'je veux uploader', 'importer'. Choosing gallery opens the photo library directly; do not tell them to press a button, you are opening it. " +
        "Parameter strokeType: the stroke the player named, if they named one -- 'analyse my serve', 'my forehand'. Fill it whenever they say it, even in passing: it is what stops the app asking them again. " +
        'If the player is missing profile facts the analysis needs, explain in ONE honest sentence why it is needed and ask ONE question before calling.',
      parameters: {
        type: 'object',
        properties: {
          mode: { type: 'string', enum: ['camera', 'gallery'], description: 'Where the clip comes from.' },
          strokeType: { type: 'string', enum: ['serve', 'forehand', 'backhand'] },
        },
        required: ['mode'],
      },
    },
  },
  {
    execution: 'client',
    function: {
      name: 'navigate',
      description:
        "Open an app screen. ONLY these exact destination values exist -- generated from the app's real route list, so this is always in sync with what the app can actually show. Player-only and coach-only destinations are both listed here, but the app will refuse a coach-only one for a player account and vice versa. To show ONE specific drill's screen (with video, sets, reps), use open_drill_detail instead -- it needs an id, generic navigate can't carry one. Do NOT use this for 'serve_setup', 'serve_analysis', 'forehand_analysis', or 'backhand_analysis' when the player wants to record or analyze a stroke -- those look like the right destination but land on a calibration screen, not the camera. Use analyze_video instead; it opens the actual recording screen. You cannot send anyone to the analysis screen: it needs a video already submitted.",
      parameters: {
        type: 'object',
        properties: {
          destination: {
            type: 'string',
            enum: ['home', 'dashboard', 'record', 'library', 'results', 'drills', 'drill_library', 'plan', 'training_plans', 'profile', 'coach_home', 'coach_students', 'coach_messages', 'paywall', 'settings', 'trophy'],
          },
        },
        required: ['destination'],
      },
    },
  },
  {
    execution: 'client',
    function: {
      name: 'open_drill_detail',
      description:
        "Open the visual detail screen for ONE specific drill (video, sets, reps, tips). Use this whenever the player asks to SEE a drill screen, not just hear it narrated. Different from start_drill_walkthrough, which reads it aloud step by step -- use both together when the player wants both.",
      parameters: {
        type: 'object',
        properties: { drillSlug: { type: 'string', description: 'The drill identifier.' } },
        required: ['drillSlug'],
      },
    },
  },
  {
    execution: 'server',
    function: {
      name: 'start_drill_walkthrough',
      description:
        "Start a step-by-step voice walkthrough of a drill: Objective, Setup, How to do it, Key points, Progression, What to look for. Use this when the player says they have a headset on and want you to talk them through a drill. Say ONLY the first step's content in your reply -- do not read the whole drill at once. The player will say 'next' or similar to advance; call advance_drill_walkthrough at that point.",
      parameters: {
        type: 'object',
        properties: { drillSlug: { type: 'string' } },
        required: ['drillSlug'],
      },
    },
  },
  {
    execution: 'server',
    function: {
      name: 'advance_drill_walkthrough',
      description:
        "Move to the next step of the drill walkthrough started with start_drill_walkthrough. Returns the next step's content, or tells you the walkthrough is finished. Say ONLY that one step, nothing else.",
      parameters: { type: 'object', properties: {}, required: [] },
    },
  },
  {
    execution: 'server',
    function: {
      name: 'fetch_analysis_history',
      description:
        "Fetch the player's analysis session history (total sessions, average score, breakdown by stroke, and details of the most recent analysis including score, red metrics, and date) to answer questions about their progress.",
      parameters: { type: 'object', properties: {}, required: [] },
    },
  },
  {
    execution: 'server',
    function: {
      name: 'fetch_recommended_drills',
      description:
        'Fetch recommended drills or the active training plan. Optionally pass an analysisId to get drills targeted at the weaknesses found in a specific analysis.',
      parameters: {
        type: 'object',
        properties: {
          analysisId: { type: 'string' },
          fault: { type: 'string', description: "The fault, exactly as it appears in the analysis (e.g. 'ARMS POSITION', 'WRIST AND RACKET POSITION'). Omit to get the drill for the fault currently being worked on." },
        },
        required: [],
      },
    },
  },
  {
    execution: 'server',
    function: {
      name: 'get_training_plan',
      description: "Fetch the player's active training plan. If no active plan exists, generate a new one.",
      parameters: { type: 'object', properties: {}, required: [] },
    },
  },
  {
    execution: 'server',
    function: {
      name: 'get_player_profile',
      description:
        'Fetch the current player profile (name, age, height, weight, handedness, level, goal, whether they have a coach). Use this to personalize advice or check what info you already have.',
      parameters: { type: 'object', properties: {}, required: [] },
    },
  },
  {
    execution: 'server',
    function: {
      name: 'save_player_profile',
      description: 'Store facts the player gives about themselves (first name, age, height, weight, handedness, level, goal, play style, strongest shot, match problems, perceived weakness, whether they have a coach). One field per call is fine; call it as soon as each fact is clear.',
      parameters: {
        type: 'object',
        properties: {
          firstName: { type: 'string' },
          age: { type: 'number' },
          heightCm: { type: 'number' },
          weightKg: { type: 'number' },
          dominantHand: { type: 'string', enum: ['left', 'right'] },
          level: { type: 'string', enum: ['beginner', 'intermediate', 'advanced', 'pro'] },
          goal: { type: 'string' },
          playStyle: { type: 'string', description: 'e.g. baseliner, all-court, serve-and-volley, counterpuncher.' },
          strongestShot: { type: 'string' },
          matchProblems: { type: 'string', description: "What gives them trouble IN A MATCH. Their words, not a diagnosis." },
          perceivedWeakness: { type: 'string', description: 'What the player THEMSELVES feels is their weak point (self-reported).' },
          hasCoach: { type: 'boolean' },
          playsMatches: { type: 'boolean', description: 'Competitive vs recreational.' },
        },
        required: [],
      },
    },
  },
  {
    execution: 'server',
    function: {
      name: 'complete_onboarding',
      description: 'Mark onboarding complete. Call this first if the account does not exist yet, before storing profile facts.',
      parameters: { type: 'object', properties: {}, required: [] },
    },
  },
  {
    execution: 'server',
    function: {
      name: 'set_session_intent',
      description:
        "Record what the player wants to do THIS session, as soon as they say it in their own words -- e.g. 'I just want to do a drill today', 'I'm alone with my tripod', 'give me a full analysis'. Call this once, as soon as the intent is clear, so the app doesn't push the wrong flow. Do not ask the player to restate it once set.",
      parameters: {
        type: 'object',
        properties: {
          intent: { type: 'string', enum: ['full_analysis', 'drill_only', 'solo_tripod', 'quick_question'] },
          strokeType: { type: 'string', enum: ['serve', 'forehand', 'backhand'] },
        },
        required: ['intent'],
      },
    },
  },
  {
    execution: 'server',
    function: {
      name: 'record_correction',
      description:
        'Record which coaching correction was given for a metric and whether it worked (coaching memory). If this is the first time coaching this metric, there is no history yet -- record anyway.',
      parameters: {
        type: 'object',
        properties: {
          metricCode: { type: 'string', description: "E.g. 'ARMS_POSITION'." },
          advice: { type: 'string' },
          worked: { type: 'boolean' },
        },
        required: ['metricCode', 'advice'],
      },
    },
  },
  {
    execution: 'client',
    function: {
      name: 'set_app_language',
      description:
        "Change the language of the whole app -- menus, screens, your own voice and the microphone -- when the player asks for it: 'parle-moi en francais', 'switch to Spanish', 'can you speak my language', or when they answer your onboarding language check. Call it as soon as they ask; do not send them to the profile screen to do it by hand, and do not ask them to confirm. Only these are available: en, fr, es, it, de, zh, hi, pt, tr, ar. If they ask for anything else, say plainly that you do not have it yet and keep going in the language you are already using -- never call this with a code that is not on the list.",
      parameters: {
        type: 'object',
        properties: { code: { type: 'string', enum: ['en', 'fr', 'es', 'it', 'de', 'zh', 'hi', 'pt', 'tr', 'ar'] } },
        required: ['code'],
      },
    },
  },
  {
    execution: 'client',
    function: {
      name: 'restart_voice_input',
      description:
        "Restart voice input when the player says it is not working -- 'the mic isn't picking me up', 'you're not hearing me', 'the microphone is stuck'. Call it once, then tell them to try again. If they say it still fails after this, do NOT call it again -- tell them plainly it may be a permission issue.",
      parameters: { type: 'object', properties: {}, required: [] },
    },
  },
  {
    execution: 'client',
    function: {
      name: 'set_voice_enabled',
      description:
        "Turn your own voice on or off, when the player asks -- 'stop talking', 'be quiet', 'I'll read instead', or later 'you can speak again'. Turning it off does not end the conversation: you keep replying in writing.",
      parameters: {
        type: 'object',
        properties: { enabled: { type: 'boolean' } },
        required: ['enabled'],
      },
    },
  },
  {
    execution: 'client',
    function: {
      name: 'set_speech_rate',
      description:
        "Change how fast you speak, when the player asks -- 'talk faster', 'you're going too fast', 'slow down a bit'. Apply it immediately and confirm in one short sentence. Do NOT offer this on your own; only when they ask.",
      parameters: {
        type: 'object',
        properties: { rate: { type: 'number', description: '0.5 (slow) .. 2.0 (fast), 1.0 = normal.' } },
        required: ['rate'],
      },
    },
  },
  {
    execution: 'server',
    function: {
      name: 'send_message_to_coach',
      description: "Send a text message to the player's coach (if they have one). Use this when the player wants to contact their coach.",
      parameters: {
        type: 'object',
        properties: { text: { type: 'string' } },
        required: ['text'],
      },
    },
  },
  {
    execution: 'server',
    function: {
      name: 'schedule_drill_reminder',
      description:
        'Schedule a daily local notification reminding the player to do a drill. Use this whenever the player agrees to a daily reminder for a drill you just recommended -- do not ask again if they already said yes. Defaults to 6pm local time if no time is given.',
      parameters: {
        type: 'object',
        properties: {
          drillSlug: { type: 'string' },
          time: { type: 'string', description: 'HH:mm local, default 18:00.' },
        },
        required: ['drillSlug'],
      },
    },
  },
  {
    execution: 'server',
    function: {
      name: 'propose_drill',
      description:
        'Save a drill idea of your own for expert review. After saving you may share the idea with the player, saying plainly it is an extra idea from you and not yet part of the validated library. Do not promise it will be adopted.',
      parameters: {
        type: 'object',
        properties: {
          title: { type: 'string' },
          objective: { type: 'string' },
          targetMetric: { type: 'string' },
        },
        required: ['title', 'objective'],
      },
    },
  },
];

/** System nudge for when the model emitted a call as text instead of a tool_call (from the snapshot). */
export const TOOL_CALL_NUDGE =
  '[SYSTEM: you just WROTE a tool call as text instead of emitting it. Nothing ran, and nothing opened. Emit the real function call now. Do not describe it, do not tell the player it is done.]';
