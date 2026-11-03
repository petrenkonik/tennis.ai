<template>
  <div class="screen full">
    <TopBar :title="tt('video_preview')" variant="mono-accent">
      <template #left>
        <span style="cursor: pointer; display: inline-flex" @click="retake">
          <Icon name="arrow-left" :size="26" />
        </span>
      </template>
    </TopBar>

    <div v-if="!clip" class="card" style="margin: 16px">
      <span class="muted">{{ tt('empty_list') }}</span>
    </div>

    <div v-else style="padding: 16px; display: flex; flex-direction: column; gap: 14px">
      <!-- Зацикленный плеер превью (порт video_preview_screen: loop через setLooping) -->
      <video
        ref="videoEl"
        :src="clip.url"
        loop
        autoplay
        muted
        playsinline
        :style="{
          width: '100%',
          maxHeight: '46vh',
          borderRadius: '18px',
          background: '#000',
          objectFit: 'contain',
        }"
        @loadedmetadata="onMeta"
      ></video>

      <!-- CHECKING VIDEO... → гейт качества (тексты — дословные литералы оригинала) -->
      <div v-if="verdict === 'checking'" class="card">
        <span class="row" style="gap: 10px">
          <span class="spinner" style="margin: 0"></span>
          <span class="mono-caption" style="font-size: 13px">{{ tt('checking_video') }}</span>
        </span>
      </div>

      <template v-else-if="verdict === 'fail'">
        <div class="card" style="border-color: var(--stop-red)">
          <div class="mono-label" style="font-size: 14px; color: var(--stop-red)">
            {{ tt('video_too_low_quality') }}
          </div>
          <div class="muted" style="margin-top: 8px; line-height: 19px">{{ tt(failKey) }}</div>
        </div>
        <button class="btn" style="background: var(--stop-red)" @click="retake">{{ tt('retake_video') }}</button>
      </template>

      <template v-else>
        <!-- Проверки пройдены -->
        <div class="card accent">
          <span class="muted" style="color: var(--accent-soft)">{{ tt('good_angle_lighting_tip') }}</span>
        </div>
        <div v-if="tooLong" class="card" style="border-color: var(--amber)">
          <span class="muted" style="color: var(--amber)">{{ tt('clip_too_long') }}</span>
        </div>

        <!-- TRIM YOUR STROKE: RangeSlider с двумя ручками (_previewTrim; исходно без подсказки —
             что ручки тащатся, говорит только голос Axel, дефект UX оригинала воспроизведён) -->
        <template v-if="duration > 0">
          <div class="mono-caption" style="font-size: 12px; color: var(--text-secondary); letter-spacing: 2px">
            {{ tt('trim_your_stroke') }}
          </div>
          <div
            ref="track"
            :style="{
              position: 'relative',
              height: '44px',
              display: 'flex',
              alignItems: 'center',
              touchAction: 'none',
              cursor: 'pointer',
            }"
            @pointerdown="onPointerDown"
            @pointermove="onPointerMove"
            @pointerup="onPointerUp"
            @pointercancel="onPointerUp"
          >
            <div style="position: absolute; left: 0; right: 0; height: 4px; border-radius: 2px; background: var(--track)"></div>
            <div
              :style="{
                position: 'absolute',
                height: '4px',
                borderRadius: '2px',
                background: 'var(--accent)',
                left: `${(trimStart / duration) * 100}%`,
                width: `${((trimEnd - trimStart) / duration) * 100}%`,
              }"
            ></div>
            <div
              v-for="h in ([trimStart, trimEnd] as const)"
              :key="h === trimStart ? 's' : 'e'"
              :style="{
                position: 'absolute',
                width: '22px',
                height: '22px',
                borderRadius: '50%',
                background: 'var(--accent)',
                border: '3px solid #050505',
                boxShadow: '0 0 0 1px var(--accent)',
                left: `calc(${(h / duration) * 100}% - 11px)`,
              }"
            ></div>
          </div>
          <div class="row mono-caption" style="justify-content: space-between; font-size: 12px; color: var(--text-secondary)">
            <span>{{ trimStart.toFixed(1) }}s</span>
            <span>{{ (trimEnd - trimStart).toFixed(1) }}s</span>
            <span>{{ trimEnd.toFixed(1) }}s</span>
          </div>
        </template>
        <div v-else class="card"><span class="muted">{{ tt('clip_duration_unknown') }}</span></div>

        <div v-if="status" class="card">
          <span class="row" style="gap: 10px">
            <span v-if="busy" class="spinner" style="margin: 0"></span>
            <span>{{ status }}</span>
          </span>
        </div>

        <button class="btn" :disabled="busy" @click="analyze">{{ busy ? tt('working') : tt('analyze_upper') }}</button>
        <button class="btn secondary" :disabled="busy" @click="retake">{{ tt('retake_video') }}</button>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { useAnalysisStore, type AnalyzeStage } from '../stores/analysis';
import { useAppStore } from '../stores/app';
import { speak, spokenOnce, markSpoken, stopSpeaking, ttsAvailable } from '../services/tts';
import { t } from '../i18n/T';
import { TopBar } from '../components/ui';
import Icon from '../components/Icon.vue';

const router = useRouter();
const analysis = useAnalysisStore();
const app = useAppStore();

const tt = (key: string) => t(app.lang, key);

const clip = computed(() => analysis.pendingPreview);
const videoEl = ref<HTMLVideoElement | null>(null);
const track = ref<HTMLElement | null>(null);

/** verdict 'checking' → 'ok' | 'fail'; failKey — ключ текста причины (ландшафт/квадрат/качество). */
const verdict = ref<'checking' | 'ok' | 'fail'>('checking');
const failKey = ref('video_too_low_quality');
const duration = ref(0);
const tooLong = ref(false);

// Окно трима (порт trimStartSec/_previewTrim); минимальное окно 0.5 c.
const trimStart = ref(0);
const trimEnd = ref(0);
const busy = ref(false);
const status = ref('');

onMounted(() => {
  if (!clip.value) {
    void router.replace('/record');
    return;
  }
  // Одноразовый голосовой гид экрана ревью (как Axel в оригинале: единственный момент,
  // когда клип ещё можно обрезать или переснять; флаг — порт axel_recording_guide_shown_).
  if (app.ttsOn && ttsAvailable() && !spokenOnce('preview')) {
    speak(tt('review_guide'), app.lang);
    markSpoken('preview');
  }
});

onBeforeUnmount(() => stopSpeaking());

/** Гейт качества (GAP §A: ландшафт, квадратный кроп, низкое разрешение; длительность — предупреждение). */
function onMeta(): void {
  const v = videoEl.value;
  if (!v) return;
  const w = v.videoWidth;
  const h = v.videoHeight;
  const fail = (key: string): void => {
    failKey.value = key;
    verdict.value = 'fail';
  };
  if (!w || !h) return fail('video_too_low_quality');
  if (w > h) return fail('landscape_clip');
  if (Math.abs(w - h) / Math.max(w, h) < 0.06) return fail('square_crop');
  if (Math.min(w, h) < 360) return fail('video_too_low_quality');
  verdict.value = 'ok';
  const d = Number.isFinite(v.duration) ? v.duration : 0;
  duration.value = d;
  trimStart.value = 0;
  trimEnd.value = d;
  tooLong.value = d > 5.2; // протокол съёмки: один удар, ≤ 5 c
}

function retake(): void {
  analysis.clearPendingPreview();
  router.back();
}

// ---- Ручки трима: указатель берёт ближайшую ручку и тащит её (RangeSlider оригинала) ----
let activeHandle: 'start' | 'end' | null = null;

function posToSec(clientX: number): number {
  const el = track.value;
  if (!el || duration.value <= 0) return 0;
  const rect = el.getBoundingClientRect();
  const ratio = Math.min(1, Math.max(0, (clientX - rect.left) / rect.width));
  return ratio * duration.value;
}

function onPointerDown(e: PointerEvent): void {
  if (busy.value || duration.value <= 0) return;
  const sec = posToSec(e.clientX);
  activeHandle = Math.abs(sec - trimStart.value) <= Math.abs(sec - trimEnd.value) ? 'start' : 'end';
  track.value?.setPointerCapture(e.pointerId);
  moveHandle(sec);
}

function onPointerMove(e: PointerEvent): void {
  if (!activeHandle) return;
  moveHandle(posToSec(e.clientX));
}

function onPointerUp(): void {
  activeHandle = null;
}

function moveHandle(sec: number): void {
  const minWindow = 0.5;
  if (activeHandle === 'start') trimStart.value = Math.min(Math.max(0, sec), trimEnd.value - minWindow);
  else trimEnd.value = Math.max(Math.min(duration.value, sec), trimStart.value + minWindow);
}

async function analyze(): Promise<void> {
  const c = clip.value;
  if (!c || busy.value) return;
  stopSpeaking();
  busy.value = true;
  const trim =
    duration.value > 0 && (trimStart.value > 0.05 || trimEnd.value < duration.value - 0.05)
      ? { startSec: trimStart.value, endSec: trimEnd.value }
      : undefined;
  try {
    const res = await analysis.analyzeVideo(c.file, c.strokeType, (stage: AnalyzeStage, done?: number, total?: number) => {
      if (stage === 'pose') status.value = total !== undefined ? `${tt('stage_pose')} ${done}/${total}` : tt('stage_pose');
      else if (stage === 'engine') status.value = tt('stage_engine');
      else status.value = tt('stage_upload');
    }, trim);
    analysis.clearPendingPreview();
    await router.push(`/analysis/${res.id}`);
  } catch (err) {
    status.value = `${tt('failed')}: ${err instanceof Error ? err.message : err}`;
  } finally {
    busy.value = false;
  }
}
</script>
