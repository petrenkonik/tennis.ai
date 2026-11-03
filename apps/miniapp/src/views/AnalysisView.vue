<template>
  <div class="screen full">
    <TopBar :title="tt('results_title')" variant="mono-accent">
      <template #left>
        <span style="cursor: pointer; display: inline-flex" @click="router.push('/')">
          <Icon name="arrow-left" :size="26" />
        </span>
      </template>
    </TopBar>

    <div v-if="!report" class="card" style="margin: 16px">
      <span class="row" style="gap: 10px">
        <span v-if="!loadError" class="spinner" style="margin: 0"></span>
        <span>{{ loadError ?? tt('loading') }}</span>
      </span>
    </div>

    <div v-else style="padding: 16px; display: flex; flex-direction: column; gap: 14px">
      <!-- Session player (results_screen.dart): loop, speed — PlaySpeedReset, phase chips seek it (_seekToMoment) -->
      <div v-if="videoUrl" class="card" style="padding: 0; overflow: hidden">
        <video
          ref="videoEl"
          :src="videoUrl"
          loop
          muted
          autoplay
          playsinline
          style="display: block; width: 100%; max-height: 46vh; object-fit: contain; background: #000"
        ></video>
        <div class="row" style="padding: 8px 12px; gap: 6px; border-top: 1px solid var(--hairline)">
          <span
            v-for="s in SPEEDS"
            :key="s"
            class="chip small"
            :class="{ active: speed === s }"
            style="font-family: var(--mono)"
            @click="applySpeed(s)"
          >
            {{ s === 1 ? '1x' : s + 'x' }}
          </span>
        </div>
      </div>

      <!-- Score /100: colored square + "ANALYSIS COMPLETE" (ResultsScreen.kt) -->
      <AppCard>
        <div class="row" style="padding: 18px; gap: 16px">
          <div
            :style="{
              width: '76px',
              height: '76px',
              borderRadius: '16px',
              background: scoreColor,
              display: 'flex',
              flexDirection: 'column',
              alignItems: 'center',
              justifyContent: 'center',
              flex: 'none',
            }"
          >
            <span :style="{ fontSize: '26px', fontWeight: 700, color: '#101408' }">{{ overallScore ?? '—' }}</span>
            <span style="font-size: 11px; color: #2a331a">/100</span>
          </div>
          <div class="grow">
            <div style="font-weight: 600; font-size: 17px">{{ tt('analysis_complete') }}</div>
            <div class="muted" style="margin-top: 4px; line-height: 18px">
              {{ tt('red_explainer') }}
            </div>
            <div style="margin-top: 6px; font-size: 13px">
              <span style="color: var(--good)">● {{ greens }}</span>
              <span style="color: var(--stop-red); margin-left: 10px">● {{ reds }}</span>
              <span class="muted" style="margin-left: 10px">● {{ unknowns }}</span>
            </div>
          </div>
        </div>
      </AppCard>

      <!-- Phases: M0–M6 chips, the contact phase (last) selected by default -->
      <div>
        <div class="mono-caption" style="font-size: 12px; color: var(--text-secondary); margin-bottom: 10px">
          {{ tt('stroke_phases') }}
        </div>
        <div class="row" style="gap: 6px; flex-wrap: wrap">
          <span
            v-for="p in phases"
            :key="p.code"
            class="chip small"
            :class="{ active: p.code === selectedPhase }"
            style="font-family: var(--mono)"
            @click="selectPhase(p.code)"
          >
            {{ p.code }}
          </span>
        </div>
      </div>

      <!-- Metrics of the selected phase -->
      <template v-if="selectedPhase">
        <div style="font-weight: 600; font-size: 16px">{{ phaseName(selectedPhase) }}</div>
        <AppCard v-if="phaseMetrics.length > 0">
        <template v-for="(m, i) in phaseMetrics" :key="m.code">
          <div v-if="i > 0" style="height: 1px; background: var(--hairline)"></div>
          <div class="metric-row">
            <span class="dot" :class="m.status"></span>
            <div class="grow">
              <div style="font-size: 15px">{{ m.label }}</div>
              <div class="muted" style="margin-top: 2px">{{ m.displayValue }} · pro: {{ m.proRangeLabel }}</div>
              <div v-if="m.correction" style="margin-top: 2px; color: var(--stop-red); font-size: 13px">
                {{ m.correction }}
              </div>
            </div>
          </div>
        </template>
      </AppCard>
      <div v-else class="card">
        <span class="muted">{{ tt('no_metrics_phase') }}</span>
      </div>
      </template>

      <!-- Weaknesses → drills -->
      <template v-if="topWeaknesses.length > 0">
        <div class="section-bar"><span class="mono-label" style="font-size: 14px">{{ tt('weaknesses') }}</span></div>
        <AppCard>
          <div class="muted" style="margin-bottom: 10px">{{ topWeaknesses.join(', ') }}</div>
          <button class="btn" @click="router.push('/drills')">{{ tt('recommended_drills') }}</button>
        </AppCard>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useAnalysisStore } from '../stores/analysis';
import { useAppStore } from '../stores/app';
import { t } from '../i18n/T';
import { PHASE_NAMES } from '@tennis-ai/engine';
import type { AnalysisOutcome, PhaseCode } from '@tennis-ai/shared';
import { AppCard, TopBar } from '../components/ui';
import Icon from '../components/Icon.vue';

const route = useRoute();
const router = useRouter();
const analysis = useAnalysisStore();
const app = useAppStore();

const tt = (key: string) => t(app.lang, key);

const report = ref<(Record<string, unknown> & Partial<AnalysisOutcome>) | null>(null);
const loadError = ref<string | null>(null);

interface MetricRow {
  code: string;
  label: string;
  phase: PhaseCode;
  status: string;
  displayValue: string;
  proRangeLabel: string;
  correction: string | null;
}
interface PhaseRow {
  code: PhaseCode;
  startFrame: number;
  endFrame: number;
}

const metrics = computed<MetricRow[]>(() => (report.value?.metrics as MetricRow[] | undefined) ?? []);
const phases = computed<PhaseRow[]>(() => (report.value?.phases as PhaseRow[] | undefined) ?? []);
const overallScore = computed<number | null>(() => {
  const o = report.value?.overall as { score: number | null } | undefined;
  return o?.score ?? null;
});
const greens = computed(() => metrics.value.filter((m) => m.status === 'green').length);
const reds = computed(() => metrics.value.filter((m) => m.status === 'red').length);
const unknowns = computed(() => metrics.value.filter((m) => m.status === 'unknown').length);
const topWeaknesses = computed<string[]>(() => (report.value?.topWeaknesses as string[] | undefined) ?? []);

/** Color of the score square (analogous to scoreColor in the clone): green/amber/red. */
const scoreColor = computed(() => {
  const s = overallScore.value;
  if (s === null) return 'var(--track)';
  if (s >= 67) return 'var(--good)';
  if (s >= 34) return 'var(--amber)';
  return 'var(--stop-red)';
});

function phaseName(code: PhaseCode): string {
  const stroke = (report.value?.strokeType as keyof typeof PHASE_NAMES | undefined) ?? 'serve';
  return `${code} · ${PHASE_NAMES[stroke]?.[code] ?? code}`;
}

// The contact phase (last detected) is selected by default, as in the clone (selected = M5).
const selectedPhase = ref<PhaseCode | null>(null);
const phaseMetrics = computed<MetricRow[]>(() => {
  const sel = selectedPhase.value;
  if (!sel) return [];
  return metrics.value.filter((m) => m.phase === sel);
});

// --- Results player (port of results_screen.dart): loop + _applyPlaybackSpeed + _seekToMoment ---
const SPEEDS = [0.25, 0.5, 1] as const;
const videoEl = ref<HTMLVideoElement | null>(null);
const videoUrl = ref<string | null>(null);
const speed = ref<number>(1);

/** Port of _applyPlaybackSpeed; the 0.25/0.5/1 set is the closest reconstruction (PlaySpeedReset). */
function applySpeed(s: number): void {
  speed.value = s;
  if (videoEl.value) videoEl.value.playbackRate = s;
}

/** Port of _seekToMoment: phase timecode = trimStart + startFrame / fps (frames are sampled from trimStart). */
function seekToPhase(p: PhaseRow): void {
  const fps = report.value?.fps as number | undefined;
  const v = videoEl.value;
  if (!v || !fps || v.readyState === 0) return;
  const trimStart = (report.value?.trimStartSec as number | null) ?? 0;
  v.currentTime = trimStart + p.startFrame / fps;
  void v.play().catch(() => {});
}

function selectPhase(code: PhaseCode): void {
  selectedPhase.value = code;
  const p = phases.value.find((x) => x.code === code);
  if (p) seekToPhase(p);
}

onMounted(async () => {
  try {
    const id = String(route.params.id);
    report.value = await analysis.fetchOne(id);
    const last = phases.value.at(-1)?.code;
    if (last) selectedPhase.value = last;
    analysis
      .fetchVideoUrl(id)
      .then((url) => {
        videoUrl.value = url;
      })
      .catch(() => {
        videoUrl.value = null;
      });
  } catch (e) {
    loadError.value = e instanceof Error ? e.message : String(e);
  }
});
</script>
