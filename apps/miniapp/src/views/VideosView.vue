<template>
  <div class="screen">
    <TopBar :title="tt('my_videos')" icon="videocam" variant="mono-accent" />

    <div style="padding: 18px 16px 0; display: flex; flex-direction: column; gap: 14px">
      <!-- Стат-карточка: TOTAL / Completed / Analyzing (CLONE_SPEC §2.8) -->
      <AppCard>
        <div class="row" style="justify-content: space-between; padding: 20px 0">
          <div v-for="s in stats" :key="s.label" class="grow" style="text-align: center">
            <div :style="{ fontSize: '32px', fontWeight: 700, color: s.color }">{{ s.value }}</div>
            <div class="muted" style="margin-top: 8px">
              <span class="mono-caption" :style="{ fontSize: s.mono ? '13px' : '12px', letterSpacing: s.mono ? '2px' : undefined }">
                {{ s.label }}
              </span>
            </div>
          </div>
        </div>
      </AppCard>

      <!-- Сетка 2×2 карточек с цветной границей (CLONE_SPEC §2.9): внутренняя плитка
           videocam + счётчик + VIDEOS, ниже — название и «No videos yet» -->
      <div v-for="(row, ri) in bucketRows" :key="ri" class="row" style="gap: 14px; align-items: stretch">
        <AppCard
          v-for="b in row"
          :key="b.key"
          clickable
          class="grow"
          :border-color="`color-mix(in srgb, ${b.color} 45%, transparent)`"
          @click="openBucket(b)"
        >
          <div>
            <div
              :style="{
                height: '110px',
                borderRadius: '14px',
                background: `color-mix(in srgb, ${b.color} 10%, transparent)`,
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                justifyContent: 'center',
                gap: '6px',
              }"
            >
              <Icon :name="'videocam'" :size="28" :style="{ color: b.color }" />
              <span :style="{ fontSize: '26px', fontWeight: 700, color: b.color }">{{ count(b) }}</span>
              <MonoCaption :size="11" :color="`color-mix(in srgb, ${b.color} 75%, transparent)`">
                {{ tt('videos_upper') }}
              </MonoCaption>
            </div>
            <div style="font-size: 18px; font-weight: 600; margin-top: 12px">{{ tt(b.titleKey) }}</div>
            <div class="muted" style="margin-top: 4px">{{ sessions(count(b), app.lang) }}</div>
          </div>
        </AppCard>
      </div>

      <!-- Раскрытый список сессий бакета (у TMA есть реальные видео из API — функциональное расширение) -->
      <template v-if="opened">
        <div v-if="openedList.length === 0" class="muted" style="text-align: center; padding: 8px">
          {{ tt('no_videos_yet') }}
        </div>
        <AppCard v-for="a in openedList" :key="a.id">
          <div class="row" style="justify-content: space-between; gap: 10px">
            <div class="grow">
              <div style="font-weight: 600; text-transform: capitalize">{{ a.strokeType }}</div>
              <div class="muted" style="margin-top: 4px">
                {{ new Date(a.createdAt).toLocaleString() }}
                · {{ a.overallScore != null ? a.overallScore + '/100' : a.status }}
              </div>
            </div>
            <button
              class="btn secondary"
              style="width: auto; padding: 10px 14px"
              @click="router.push(`/analysis/${a.id}`)"
            >
              {{ tt('open') }}
            </button>
          </div>
          <!-- Действия библиотеки оригинала (video_library_screen): RE-ANALYZE / Share / Download / Delete Video -->
          <div class="row" style="gap: 8px; margin-top: 10px; flex-wrap: wrap">
            <button
              class="btn ghost"
              style="width: auto; padding: 8px 12px; font-size: 13px"
              :disabled="busyId === a.id"
              @click="reanalyze(a)"
            >
              {{ busyId === a.id ? tt('preparing') : tt('re_analyze') }}
            </button>
            <button
              class="btn ghost"
              style="width: auto; padding: 8px 12px; font-size: 13px"
              :disabled="busyId === a.id"
              @click="shareVideo(a)"
            >
              {{ tt('share') }}
            </button>
            <button
              class="btn ghost"
              style="width: auto; padding: 8px 12px; font-size: 13px"
              :disabled="busyId === a.id"
              @click="downloadVideo(a)"
            >
              {{ tt('download') }}
            </button>
            <button
              class="btn ghost"
              style="width: auto; padding: 8px 12px; font-size: 13px; color: var(--stop-red); border-color: var(--stop-red)"
              @click="confirmDelete = a"
            >
              {{ tt('delete_video') }}
            </button>
          </div>
        </AppCard>
      </template>
    </div>

    <!-- Delete Video: подтверждение оригинала (delete_video_confirm, CANCEL/DELETE) -->
    <div
      v-if="confirmDelete"
      style="
        position: fixed;
        inset: 0;
        z-index: 50;
        background: rgba(0, 0, 0, 0.6);
        display: flex;
        align-items: center;
        justify-content: center;
        padding: 24px;
      "
      @click.self="confirmDelete = null"
    >
      <div class="card" style="width: 100%; border-radius: 20px">
        <div style="font-weight: 700; font-size: 18px">{{ tt('delete_video') }}</div>
        <div class="muted" style="margin-top: 8px">{{ tt('delete_video_confirm') }}</div>
        <div class="row" style="gap: 10px; margin-top: 16px">
          <button class="btn secondary grow" @click="confirmDelete = null">{{ tt('cancel_upper') }}</button>
          <button class="btn grow" style="background: var(--stop-red); color: #fff" @click="doDelete">
            {{ tt('delete_upper') }}
          </button>
        </div>
      </div>
    </div>

    <!-- Тост (аналог Toast в клоне) -->
    <div
      v-if="toast"
      style="
        position: fixed;
        bottom: 96px;
        left: 50%;
        transform: translateX(-50%);
        background: #1b1b1d;
        border: 1px solid var(--card-border);
        color: var(--text);
        border-radius: 12px;
        padding: 10px 16px;
        font-size: 13px;
        z-index: 60;
        max-width: 86vw;
      "
    >
      {{ toast }}
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { useAppStore } from '../stores/app';
import { useAnalysisStore } from '../stores/analysis';
import { sessions, t } from '../i18n/T';
import type { AnalysisDto } from '@tennis-ai/shared';
import { AppCard, MonoCaption, TopBar } from '../components/ui';
import Icon from '../components/Icon.vue';

const router = useRouter();
const app = useAppStore();
const analysis = useAnalysisStore();

const tt = (key: string) => t(app.lang, key);

interface Bucket {
  key: 'serve' | 'forehand' | 'backhand' | 'drills';
  titleKey: string;
  color: string;
}

// Бакеты и цвета — как в LibraryScreens.kt: serve blue, forehand lime, backhand amber, drills red.
const buckets: Bucket[] = [
  { key: 'serve', titleKey: 'serve', color: 'var(--serve-blue)' },
  { key: 'forehand', titleKey: 'forehand', color: 'var(--accent)' },
  { key: 'backhand', titleKey: 'backhand', color: 'var(--amber)' },
  { key: 'drills', titleKey: 'drills', color: 'var(--red)' },
];

const bucketRows: Bucket[][] = [buckets.slice(0, 2), buckets.slice(2)];

const stats = computed(() => {
  const list = analysis.list;
  return [
    { value: list.length, label: tt('total'), color: 'var(--text)', mono: true },
    {
      value: list.filter((a) => a.status === 'completed').length,
      label: tt('completed'),
      color: 'var(--accent)',
      mono: false,
    },
    {
      value: list.filter((a) => a.status === 'analyzing').length,
      label: tt('analyzing'),
      color: 'var(--amber)',
      mono: false,
    },
  ];
});

function count(b: Bucket): number {
  if (b.key === 'drills') return 0;
  return analysis.list.filter((a) => a.strokeType === b.key).length;
}

const opened = ref<Bucket | null>(null);
const openedList = computed<AnalysisDto[]>(() =>
  opened.value && opened.value.key !== 'drills'
    ? analysis.list.filter((a) => a.strokeType === opened.value?.key)
    : [],
);

function openBucket(b: Bucket): void {
  if (b.key === 'drills') {
    void router.push('/drills');
    return;
  }
  opened.value = opened.value === b ? null : b;
}

// --- Действия сессии (порт video_library_screen: share_plus, download, RE-ANALYZE, Delete Video) ---
const toast = ref('');
const busyId = ref<string | null>(null);
const confirmDelete = ref<AnalysisDto | null>(null);
let toastTimer: ReturnType<typeof setTimeout> | undefined;

function toastNow(text: string): void {
  toast.value = text;
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => (toast.value = ''), 2200);
}

/** Скачивание через anchor+blob; в Telegram WebView <a download> обычно работает, иначе — только тост. */
function saveBlob(file: File): void {
  const url = URL.createObjectURL(file);
  const link = document.createElement('a');
  link.href = url;
  link.download = file.name;
  link.click();
  setTimeout(() => URL.revokeObjectURL(url), 5000);
}

async function downloadVideo(a: AnalysisDto): Promise<void> {
  if (busyId.value) return;
  busyId.value = a.id;
  try {
    saveBlob(await analysis.fetchVideoFile(a.id, a.strokeType));
    toastNow(tt('video_saved_to'));
  } catch {
    toastNow(tt('download_failed'));
  } finally {
    busyId.value = null;
  }
}

type ShareDataWithFiles = { files?: File[]; title?: string; text?: string; url?: string };

/** Порт share_plus: системный share-лист с файлом; без navigator.share — скачивание как фолбэк. */
async function shareVideo(a: AnalysisDto): Promise<void> {
  if (busyId.value) return;
  busyId.value = a.id;
  try {
    const file = await analysis.fetchVideoFile(a.id, a.strokeType);
    const nav = navigator as Navigator & {
      canShare?: (data: ShareDataWithFiles) => boolean;
      share?: (data: ShareDataWithFiles) => Promise<void>;
    };
    if (nav.canShare?.({ files: [file] }) && nav.share) {
      await nav.share({ files: [file], title: 'Tennis AI' });
    } else {
      saveBlob(file);
      toastNow(tt('video_saved_to'));
    }
  } catch (e) {
    if (!(e instanceof DOMException && e.name === 'AbortError')) toastNow(tt('download_failed'));
  } finally {
    busyId.value = null;
  }
}

/** RE-ANALYZE: оригинал сессии заново проходит /preview → анализ (квота списывается при создании сессии, как в оригинале). */
async function reanalyze(a: AnalysisDto): Promise<void> {
  if (busyId.value) return;
  busyId.value = a.id;
  try {
    const file = await analysis.fetchVideoFile(a.id, a.strokeType);
    analysis.setPendingPreview(file, a.strokeType);
    void router.push('/preview');
  } catch {
    toastNow(tt('download_failed'));
  } finally {
    busyId.value = null;
  }
}

async function doDelete(): Promise<void> {
  const a = confirmDelete.value;
  if (!a) return;
  confirmDelete.value = null;
  try {
    await analysis.remove(a.id);
    toastNow(tt('video_deleted'));
  } catch {
    toastNow(tt('delete_failed'));
  }
}

onMounted(() => {
  void analysis.fetchList().catch(() => undefined);
});
</script>
