<template>
  <div class="screen full">
    <TopBar :title="tt('analyze_title')" variant="mono-accent">
      <template #left>
        <span style="cursor: pointer; display: inline-flex" @click="router.back()">
          <Icon name="arrow-left" :size="26" />
        </span>
      </template>
    </TopBar>

    <div style="padding: 18px 16px 0; display: flex; flex-direction: column; gap: 14px">
      <!-- Stroke type: chips -->
      <AppCard>
        <div class="mono-caption" style="font-size: 12px; color: var(--text-secondary)">{{ tt('stroke_type') }}</div>
        <div class="row" style="gap: 8px; margin-top: 10px; flex-wrap: wrap">
          <span
            v-for="s in strokeTypes"
            :key="s"
            class="chip small"
            :class="{ active: strokeType === s }"
            @click="strokeType = s"
          >
            {{ tt(s) }}
          </span>
        </div>
      </AppCard>

      <!-- Auto-detect from a frame -->
      <AppCard>
        <div class="muted">{{ tt('auto_detect_frame') }}</div>
        <label class="btn ghost" style="margin-top: 10px; text-align: center">
          <input type="file" accept="image/*" capture="environment" style="display: none" @change="onFramePicked" />
          {{ frameName ?? tt('pick_video') }}
        </label>
      </AppCard>

      <!-- Video: selection goes to VIDEO PREVIEW (review/trim/quality — as in the original) -->
      <AppCard>
        <div class="muted">{{ tt('video_label') }}</div>
        <label class="btn ghost" style="margin-top: 10px; text-align: center">
          <input type="file" accept="video/*" capture="environment" style="display: none" @change="onVideoPicked" />
          {{ fileName ?? tt('pick_video') }}
        </label>
      </AppCard>

      <div v-if="status" class="card">
        <span class="row" style="gap: 10px">
          <span v-if="busy" class="spinner" style="margin: 0"></span>
          <span>{{ status }}</span>
        </span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useAnalysisStore } from '../stores/analysis';
import { useAppStore } from '../stores/app';
import { t } from '../i18n/T';
import type { StrokeType } from '@tennis-ai/shared';
import { AppCard, TopBar } from '../components/ui';
import Icon from '../components/Icon.vue';

const router = useRouter();
const route = useRoute();
const analysis = useAnalysisStore();
const app = useAppStore();

const tt = (key: string) => t(app.lang, key);

const strokeTypes: StrokeType[] = ['serve', 'forehand', 'backhand'];
const strokeType = ref<StrokeType>('serve');
const fileName = ref<string | null>(null);
const frameName = ref<string | null>(null);
const busy = ref(false);
const status = ref('');

onMounted(() => {
  // Home passes a preset stroke: /record?stroke=forehand (CLONE_SPEC §2.1)
  const q = String(route.query.stroke ?? '');
  if (q === 'serve' || q === 'forehand' || q === 'backhand') strokeType.value = q;
});

function onVideoPicked(e: Event): void {
  const input = e.target as HTMLInputElement;
  const picked = input.files?.[0];
  if (!picked) return;
  fileName.value = picked.name;
  // Next — the original's review screen: loop preview, quality gate, trim, ANALYZE/RETAKE.
  analysis.setPendingPreview(picked, strokeType.value);
  void router.push('/preview');
}

async function onFramePicked(e: Event): Promise<void> {
  const input = e.target as HTMLInputElement;
  const frame = input.files?.[0];
  if (!frame) return;
  frameName.value = frame.name;
  busy.value = true;
  status.value = tt('detecting');
  try {
    strokeType.value = await analysis.detectStrokeFromFrame(frame);
    status.value = `${tt('detected')}: ${tt(strokeType.value)}`;
  } catch (err) {
    status.value = `${tt('failed')}: ${err instanceof Error ? err.message : err}`;
  } finally {
    busy.value = false;
  }
}
</script>
