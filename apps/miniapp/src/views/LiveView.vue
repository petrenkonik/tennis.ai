<template>
  <div class="screen full" style="background: #000; position: relative; overflow: hidden">
    <!-- Header — on idle and summary; in live/prepare the screen is given to the camera -->
    <TopBar v-if="phase === 'idle' || phase === 'summary'" :title="tt('live_title')" variant="mono-accent">
      <template #left>
        <span style="cursor: pointer; display: inline-flex" @click="router.back()">
          <Icon name="arrow-left" :size="26" />
        </span>
      </template>
    </TopBar>

    <!-- Live feed + skeleton (v-show: the element stays alive so srcObject is not reset) -->
    <div v-show="streamOn" style="position: absolute; inset: 0">
      <video
        ref="videoEl"
        autoplay
        muted
        playsinline
        :style="{
          width: '100%',
          height: '100%',
          objectFit: 'cover',
          transform: mirrored ? 'scaleX(-1)' : undefined,
        }"
      ></video>
      <canvas
        ref="canvasEl"
        :style="{
          position: 'absolute',
          inset: 0,
          width: '100%',
          height: '100%',
          transform: mirrored ? 'scaleX(-1)' : undefined,
        }"
      ></canvas>

      <!-- Top overlays (clone: LIVE · stroke + Stop; original: REC / HD @ 30FPS / AI TRACKING) -->
      <div style="position: absolute; top: 12px; left: 12px; right: 12px; display: flex; flex-direction: column; gap: 8px">
        <div class="row" style="justify-content: space-between">
          <span
            class="mono-label"
            :style="{
              background: 'rgba(16, 24, 20, 0.67)',
              color: 'var(--accent)',
              padding: '8px 12px',
              borderRadius: '10px',
              fontSize: '13px',
            }"
          >
            LIVE · {{ tt(stroke) }}
          </span>
          <button
            class="btn secondary"
            style="width: auto; padding: 8px 16px"
            @click="stop"
          >
            {{ tt('stop') }}
          </button>
        </div>
        <div class="row" style="gap: 6px; flex-wrap: wrap">
          <LiveChip>
            <span :style="{ width: '8px', height: '8px', borderRadius: '50%', background: 'var(--stop-red)', display: 'inline-block' }" />
            {{ tt('rec') }}
          </LiveChip>
          <LiveChip>{{ tt('hd_30fps') }}</LiveChip>
          <LiveChip color="var(--accent)">{{ tt('ai_tracking_active') }}</LiveChip>
          <LiveChip :color="inFrame ? 'var(--accent)' : 'var(--text-secondary)'">
            {{ tt('subject_in_frame') }} {{ inFrame ? '✓' : '…' }}
          </LiveChip>
        </div>
      </div>

      <!-- Prepare: "Get in position. 15 seconds." → "Go!" -->
      <div
        v-if="phase === 'prepare'"
        style="
          position: absolute;
          inset: 0;
          background: rgba(0, 0, 0, 0.45);
          display: flex;
          flex-direction: column;
          align-items: center;
          justify-content: center;
          gap: 10px;
        "
      >
        <div class="mono-label" style="font-size: 16px; color: #fff">{{ tt('get_in_position') }}</div>
        <div style="font-size: 72px; font-weight: 700; color: var(--accent); font-family: var(--mono)">
          {{ countdown > 0 ? countdown : tt('go') }}
        </div>
      </div>

      <!-- LIVE CORRECTION: red correction card with a 15-second window -->
      <div
        v-if="phase === 'live' && correction"
        style="
          position: absolute;
          bottom: 20px;
          left: 16px;
          right: 16px;
          background: rgba(229, 84, 75, 0.92);
          border-radius: 16px;
          padding: 16px;
        "
      >
        <div class="mono-label" style="font-size: 12px; color: #fff; opacity: 0.85">
          {{ tt('live_correction') }} · {{ correctionLeft }}s
        </div>
        <div style="font-weight: 700; color: #fff; margin-top: 4px; font-size: 16px">{{ correction.label }}</div>
        <div style="color: #fff; margin-top: 4px; font-size: 14px; line-height: 19px">{{ correction.correction }}</div>
      </div>

      <!-- Stop zone: TAP TO STOP across the bottom -->
      <div
        v-if="phase === 'live' && !correction"
        class="mono-label"
        style="position: absolute; bottom: 28px; left: 0; right: 0; text-align: center; color: rgba(255, 255, 255, 0.75); font-size: 13px"
      >
        {{ tt('tap_to_stop') }}
      </div>
    </div>

    <!-- IDLE: stroke selection + START LIVE ANALYSIS -->
    <div
      v-if="phase === 'idle'"
      style="flex: 1; display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 24px; text-align: center"
    >
      <Icon name="sensors" :size="56" :style="{ color: 'var(--accent)' }" />
      <div class="mono-label" style="font-size: 16px; color: var(--accent); margin-top: 18px">
        {{ tt('live_title') }}
      </div>
      <div class="muted" style="margin-top: 8px; font-size: 15px">{{ tt('live_desc') }}</div>

      <div class="muted" style="margin-top: 18px; font-size: 13px">{{ tt('tap_rec_when_ready') }}</div>
      <div class="row" style="gap: 6px; margin-top: 8px">
        <span
          v-for="s in (['forehand', 'backhand', 'serve'] as const)"
          :key="s"
          class="chip small"
          :class="{ active: stroke === s }"
          @click="stroke = s"
        >
          {{ tt(s) }}
        </span>
      </div>

      <div v-if="camError" class="card" style="margin-top: 22px; max-width: 420px">
        <span class="muted">{{ camError === 'denied' ? tt('camera_denied') : tt('live_unavailable') }}</span>
      </div>

      <button
        class="btn"
        style="margin-top: 22px; width: auto; padding: 12px 22px"
        :disabled="starting"
        @click="start"
      >
        {{ starting ? tt('checking_video') : tt('start_live_analysis') }}
      </button>
      <button class="btn secondary" style="margin-top: 10px; width: auto; padding: 12px 22px" @click="router.push('/record')">
        {{ tt('auto_detect') }}
      </button>
    </div>

    <!-- SUMMARY: GOOD / ERRORS (the original's final lines verbatim) -->
    <div
      v-if="phase === 'summary'"
      style="flex: 1; display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 24px; text-align: center"
    >
      <Icon :name="verdict === 'excellent' ? 'check' : 'target'" :size="56" :style="{ color: verdict === 'excellent' ? 'var(--accent)' : 'var(--amber)' }" />
      <div style="font-size: 19px; font-weight: 700; margin-top: 16px; max-width: 420px">
        {{ verdict === 'excellent' ? tt('all_metrics_corrected') : tt('errors_not_fixable') }}
      </div>
      <div class="row" style="gap: 14px; margin-top: 14px; font-size: 14px">
        <span style="color: var(--good)">● {{ correctedCount }}</span>
        <span style="color: var(--stop-red)">● {{ errorCount }}</span>
      </div>
      <div class="row" style="gap: 10px; margin-top: 22px">
        <button class="btn secondary" style="width: auto; padding: 12px 22px" @click="resetToIdle">{{ tt('try_again') }}</button>
        <button class="btn" style="width: auto; padding: 12px 22px" @click="router.push('/drills')">{{ tt('drills') }}</button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, defineComponent, h, nextTick, onBeforeUnmount, ref } from 'vue';
import { useRouter } from 'vue-router';
import { useAppStore } from '../stores/app';
import { t } from '../i18n/T';
import { CALCULATORS, evaluateMetric } from '@tennis-ai/engine';
import { METRIC_DEFINITIONS } from '@tennis-ai/shared';
import type { Calibration, MetricEvaluation, PoseFrame, StrokeType } from '@tennis-ai/shared';
import type { PoseLandmarker } from '@mediapipe/tasks-vision';
import { hasDetectablePose, loadPoseLandmarker, toPoseFrame } from '../services/poseService';
import { speak, stopSpeaking, ttsAvailable } from '../services/tts';
import { TopBar } from '../components/ui';
import Icon from '../components/Icon.vue';

const router = useRouter();
const app = useAppStore();
const tt = (key: string) => t(app.lang, key);

/** Chip overlay on top of the feed (semi-transparent plate, like 0xAA101814 in the clone). */
const LiveChip = defineComponent({
  props: { color: { type: String, default: '#fff' } },
  setup(props, { slots }) {
    return () =>
      h(
        'span',
        {
          style: {
            background: 'rgba(16, 24, 20, 0.67)',
            color: props.color,
            padding: '4px 10px',
            borderRadius: '8px',
            fontSize: '11px',
            fontFamily: 'var(--mono)',
            display: 'inline-flex',
            alignItems: 'center',
            gap: '5px',
          },
        },
        [slots.default?.()],
      );
  },
});

type Phase = 'idle' | 'prepare' | 'live' | 'summary';
const phase = ref<Phase>('idle');
const stroke = ref<StrokeType>('forehand');
const starting = ref(false);
const camError = ref<'denied' | 'unsupported' | null>(null);
const streamOn = ref(false);
const mirrored = ref(false);
const countdown = ref(15);
const inFrame = ref(false);
const correction = ref<MetricEvaluation | null>(null);
const correctionLeft = ref(15);
const correctedCount = ref(0);
const errorCount = ref(0);
const verdict = ref<'excellent' | 'drills'>('drills');

const videoEl = ref<HTMLVideoElement | null>(null);
const canvasEl = ref<HTMLCanvasElement | null>(null);

/** BlazePose skeleton: connection pairs (port of CONNECTIONS from the clone's LiveAnalysisScreen.kt). */
const CONNECTIONS: Array<[number, number]> = [
  [11, 12], [11, 13], [13, 15], [12, 14], [14, 16],
  [11, 23], [12, 24], [23, 24],
  [23, 25], [25, 27], [24, 26], [26, 28],
  [27, 31], [28, 32], [27, 29], [28, 30],
];

/** Live metrics: per-frame calculators with a restored proRange for the selected stroke. */
const liveDefs = computed(() =>
  METRIC_DEFINITIONS.filter(
    (d) => d.strokeTypes.includes(stroke.value) && d.calculator !== null && d.proRange !== null && CALCULATORS[d.calculator!],
  ),
);

function calibration(): Calibration {
  return { handedness: app.handRight ? 'right' : 'left' };
}

// --- Stream and loop ---
let stream: MediaStream | null = null;
let landmarker: PoseLandmarker | null = null;
let raf = 0;
let countdownTimer: ReturnType<typeof setInterval> | undefined;
let windowTimer: ReturnType<typeof setInterval> | undefined;
let lastDetectAt = 0;
const correctedCodes = new Set<string>();
const errorCodes = new Set<string>();
let currentCode: string | null = null;

async function start(): Promise<void> {
  if (starting.value) return;
  starting.value = true;
  camError.value = null;
  try {
    if (!navigator.mediaDevices?.getUserMedia) {
      camError.value = 'unsupported';
      return;
    }
    try {
      stream = await navigator.mediaDevices.getUserMedia({
        video: { facingMode: 'environment', width: { ideal: 1280 }, height: { ideal: 720 } },
        audio: false,
      });
      mirrored.value = false;
    } catch {
      // front camera as a fallback (preview is mirrored)
      stream = await navigator.mediaDevices.getUserMedia({ video: true, audio: false });
      mirrored.value = true;
    }
    await nextTick();
    if (videoEl.value) {
      videoEl.value.srcObject = stream;
      await videoEl.value.play().catch(() => undefined);
    }
    streamOn.value = true;
    // "Get in position. 15 seconds." — port of the original's readiness countdown
    phase.value = 'prepare';
    countdown.value = 15;
    countdownTimer = setInterval(() => {
      countdown.value -= 1;
      if (countdown.value <= 0) {
        clearInterval(countdownTimer);
        void goLive();
      }
    }, 1000);
  } catch {
    camError.value = 'denied';
  } finally {
    starting.value = false;
  }
}

async function goLive(): Promise<void> {
  try {
    landmarker = await loadPoseLandmarker();
  } catch {
    stopStream();
    phase.value = 'idle';
    camError.value = 'unsupported';
    return;
  }
  phase.value = 'live';
  windowTimer = setInterval(() => {
    if (correction.value && correctionLeft.value > 0) correctionLeft.value -= 1;
  }, 1000);
  raf = requestAnimationFrame(loop);
}

function loop(): void {
  raf = requestAnimationFrame(loop);
  const v = videoEl.value;
  if (!v || v.readyState < 2 || !landmarker) return;
  const now = performance.now();
  if (now - lastDetectAt < 66) return; // detection ~15 fps, drawing — every frame
  lastDetectAt = now;
  const result = landmarker.detectForVideo(v, now);
  const frame = toPoseFrame(0, now, v.videoWidth, v.videoHeight, result);
  inFrame.value = hasDetectablePose(frame);
  drawSkeleton(frame);
  updateCorrection(frame);
}

/** Port of updateLiveMetrics (clone): the first metric outside the pro range → correction card. */
function updateCorrection(frame: PoseFrame): void {
  const cal = calibration();
  for (const def of liveDefs.value) {
    const value = CALCULATORS[def.calculator!]({
      frames: [frame],
      phase: def.phase,
      phases: [],
      detections: [],
      calibration: cal,
      fps: 30,
    });
    if (value === null || !Number.isFinite(value)) continue;
    const ev = evaluateMetric(def, value);
    if (ev.status === 'red') {
      if (!errorCodes.has(def.code)) errorCodes.add(def.code);
      if (currentCode !== def.code) {
        // new correction: a 15-second window to fix it (port of the original's countdown)
        currentCode = def.code;
        correction.value = ev;
        correctionLeft.value = 15;
        const phrase = ev.correction;
        if (phrase && app.ttsOn && ttsAvailable()) speak(phrase, app.lang);
      }
      return;
    }
    if (ev.status === 'green' && currentCode === def.code) {
      correctedCodes.add(def.code);
      currentCode = null;
      correction.value = null;
      return;
    }
  }
}

function drawSkeleton(frame: PoseFrame): void {
  const c = canvasEl.value;
  if (!c) return;
  const cw = c.clientWidth;
  const ch = c.clientHeight;
  if (cw === 0 || ch === 0) return;
  if (c.width !== cw || c.height !== ch) {
    c.width = cw;
    c.height = ch;
  }
  const ctx = c.getContext('2d');
  if (!ctx) return;
  ctx.clearRect(0, 0, cw, ch);
  const bw = frame.width;
  const bh = frame.height;
  if (!bw || !bh) return;
  // FILL_CENTER: proportional scale + centering (like SkeletonOverlay in the clone)
  const scale = Math.max(cw / bw, ch / bh);
  const sw = bw * scale;
  const sh = bh * scale;
  const offX = (cw - sw) / 2;
  const offY = (ch - sh) / 2;
  const pts = frame.landmarks.map((lm) => ({ x: offX + lm.x * sw, y: offY + lm.y * sh, v: lm.visibility ?? 0 }));
  const visible = (i: number) => pts[i]?.v >= 0.5;
  ctx.strokeStyle = '#9ACD32';
  ctx.lineWidth = 3;
  ctx.lineCap = 'round';
  for (const [a, b] of CONNECTIONS) {
    if (!visible(a) || !visible(b)) continue;
    ctx.beginPath();
    ctx.moveTo(pts[a].x, pts[a].y);
    ctx.lineTo(pts[b].x, pts[b].y);
    ctx.stroke();
  }
  ctx.fillStyle = '#fff';
  for (const i of [11, 12, 13, 14, 15, 16, 23, 24, 25, 26, 27, 28]) {
    if (!visible(i)) continue;
    ctx.beginPath();
    ctx.arc(pts[i].x, pts[i].y, 3, 0, Math.PI * 2);
    ctx.fill();
  }
}

function stop(): void {
  if (phase.value !== 'prepare' && phase.value !== 'live') return;
  cancelAnimationFrame(raf);
  clearInterval(countdownTimer);
  clearInterval(windowTimer);
  // Verdict per the original's final lines: everything corrected → "Excellent form!", otherwise — drills.
  const open = [...errorCodes].filter((code) => !correctedCodes.has(code)).length;
  correctedCount.value = correctedCodes.size;
  errorCount.value = errorCodes.size;
  verdict.value = errorCodes.size === 0 || open === 0 ? 'excellent' : 'drills';
  const phrase = verdict.value === 'excellent' ? tt('all_metrics_corrected') : tt('errors_not_fixable');
  if (app.ttsOn && ttsAvailable()) speak(phrase, app.lang);
  stopStream();
  stopSpeaking();
  phase.value = 'summary';
}

function stopStream(): void {
  stream?.getTracks().forEach((tr) => tr.stop());
  stream = null;
  streamOn.value = false;
  if (videoEl.value) videoEl.value.srcObject = null;
  const c = canvasEl.value;
  c?.getContext('2d')?.clearRect(0, 0, c.width, c.height);
}

function resetToIdle(): void {
  correctedCodes.clear();
  errorCodes.clear();
  currentCode = null;
  correction.value = null;
  correctedCount.value = 0;
  errorCount.value = 0;
  inFrame.value = false;
  phase.value = 'idle';
}

onBeforeUnmount(() => {
  cancelAnimationFrame(raf);
  clearInterval(countdownTimer);
  clearInterval(windowTimer);
  stopStream();
  stopSpeaking();
});
</script>
