<template>
  <div class="screen">
    <TopBar title="AXEL AI" variant="serif" :title-size="24" icon="">
      <template #left><Icon name="arrow-left" :size="26" /></template>
    </TopBar>

    <div style="padding: 16px">
      <!-- REAL-TIME ANALYSIS: accent border only, no fill (audit §1 item 3) -->
      <button class="btn secondary" style="height: 52px" @click="router.push('/live')">
        <span class="row" style="gap: 10px; justify-content: center">
          <Icon name="sensors" :size="20" />
          <span class="mono-label" style="font-size: 15px">{{ tt('realtime_analysis') }}</span>
        </span>
      </button>

      <!-- TRAINING MODULES: monospace label + line to the edge -->
      <div class="row" style="margin: 18px 0; gap: 12px">
        <MonoCaption :size="13">{{ tt('training_modules') }}</MonoCaption>
        <div class="grow" style="height: 1px; background: var(--hairline)"></div>
      </div>

      <div style="display: flex; flex-direction: column; gap: 14px">
        <AppCard v-for="m in modules" :key="m.titleKey" clickable @click="open(m)">
          <div style="padding: 14px">
            <div class="row" style="gap: 14px">
              <IconTile :icon="m.icon" :color="m.color" />
              <div class="grow">
                <div style="font-size: 18px; font-weight: 600">{{ tt(m.titleKey) }}</div>
                <div class="muted" style="margin-top: 2px; line-height: 17px">{{ tt(m.subtitleKey) }}</div>
              </div>
              <RingPercent :percent="0" :color="m.color" :size="44" />
            </div>
            <div style="height: 12px"></div>
            <!-- In the original the progress line is ~84% wide, the label sits BELOW it, on the right (audit §1 item 1) -->
            <div style="width: 84%; height: 3px; border-radius: 2px; background: var(--track)"></div>
            <div style="height: 5px"></div>
            <div class="row" style="justify-content: flex-end">
              <MonoCaption :size="11" color="var(--text-dim)" style="letter-spacing: 1.5px">
                {{ m.tagKey ? tt(m.tagKey) : sessions(strokeSessions[m.stroke ?? ''] ?? 0, app.lang) }}
              </MonoCaption>
            </div>
          </div>
        </AppCard>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { useAppStore } from '../stores/app';
import { useAnalysisStore } from '../stores/analysis';
import { sessions, t } from '../i18n/T';
import type { StrokeType } from '@tennis-ai/shared';
import { AppCard, IconTile, MonoCaption, RingPercent, TopBar } from '../components/ui';
import Icon from '../components/Icon.vue';

const router = useRouter();
const app = useAppStore();
const analysis = useAnalysisStore();

const tt = (key: string) => t(app.lang, key);

interface Module {
  titleKey: string;
  subtitleKey: string;
  icon: string;
  color: string;
  tagKey: string | null;
  stroke: StrokeType | null;
}

const modules: Module[] = [
  { titleKey: 'coach_mode', subtitleKey: 'realtime_for_coaches', icon: 'clipboard', color: 'var(--orange)', tagKey: 'coach', stroke: null },
  { titleKey: 'auto_detect', subtitleKey: 'auto_detect_desc', icon: 'scan', color: 'var(--accent)', tagKey: 'new', stroke: null },
  { titleKey: 'serve_analysis', subtitleKey: 'full_motion_posture', icon: 'videocam', color: 'var(--serve-blue)', tagKey: null, stroke: 'serve' },
  { titleKey: 'forehand', subtitleKey: 'rotation_extension', icon: 'play', color: 'var(--accent)', tagKey: null, stroke: 'forehand' },
  { titleKey: 'backhand', subtitleKey: 'handed_technique', icon: 'play', color: 'var(--amber)', tagKey: null, stroke: 'backhand' },
  { titleKey: 'ai_drills', subtitleKey: 'personalized_exercises', icon: 'dumbbell', color: 'var(--red)', tagKey: null, stroke: null },
];

/** Actual session count per stroke type — from the API analyses list. */
const strokeSessions = computed<Record<string, number>>(() => {
  const acc: Record<string, number> = { serve: 0, forehand: 0, backhand: 0 };
  for (const a of analysis.list) acc[a.strokeType] = (acc[a.strokeType] ?? 0) + 1;
  return acc;
});

// CLONE_SPEC §2.1: Coach Mode/REAL-TIME → Live; Serve/Forehand/Backhand → analysis with a preset
// stroke; Auto Detect → analysis (manual selection); AI Drills → Axel.
function open(m: Module): void {
  if (m.titleKey === 'coach_mode') void router.push('/live');
  else if (m.titleKey === 'ai_drills') void router.push('/axel');
  else if (m.stroke) void router.push(`/record?stroke=${m.stroke}`);
  else void router.push('/record');
}

onMounted(() => {
  void analysis.fetchList().catch(() => undefined);
});
</script>
