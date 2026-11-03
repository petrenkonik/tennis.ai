<template>
  <div class="screen">
    <TopBar :title="tt('my_analyses')" icon="chart" variant="mono-accent" />

    <div style="padding: 18px 16px 0; display: flex; flex-direction: column; gap: 14px">
      <!-- Стат-карточка: TOTAL / Completed / Analyzing (CLONE_SPEC §2.8) -->
      <AppCard>
        <div class="row" style="justify-content: space-between; padding: 20px 0">
          <div v-for="s in stats" :key="s.label" class="grow" style="text-align: center">
            <div :style="{ fontSize: '32px', fontWeight: 700, color: s.color }">{{ s.value }}</div>
            <div class="muted" style="margin-top: 8px">
              <span
                class="mono-caption"
                :style="{ fontSize: s.mono ? '13px' : '12px', letterSpacing: s.mono ? '2px' : undefined }"
              >
                {{ s.label }}
              </span>
            </div>
          </div>
        </div>
      </AppCard>

      <!-- Сетка 2×2 с цветной границей: плитка activity + no_data (ключ-дефект оригинала — дословно) -->
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
                border: `1px solid color-mix(in srgb, ${b.color} 40%, transparent)`,
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                justifyContent: 'center',
                gap: '8px',
              }"
            >
              <Icon :name="'activity'" :size="30" :style="{ color: b.color }" />
              <template v-if="count(b) > 0">
                <span :style="{ fontSize: '24px', fontWeight: 700, color: b.color }">{{ count(b) }}</span>
              </template>
              <MonoCaption v-else :size="12" :color="`color-mix(in srgb, ${b.color} 75%, transparent)`">
                {{ tt('no_data') }}
              </MonoCaption>
            </div>
            <div style="font-size: 18px; font-weight: 600; margin-top: 12px">{{ tt(b.titleKey) }}</div>
            <div class="muted" style="margin-top: 4px">{{ sessions(count(b), app.lang) }}</div>
            <div v-if="b.key !== 'drills'" class="row" style="gap: 8px; margin-top: 10px">
              <PillBadge :color="'var(--accent)'" icon="check">{{ done(b) }}</PillBadge>
              <PillBadge :color="'var(--amber)'" icon="clock">{{ busy(b) }}</PillBadge>
            </div>
          </div>
        </AppCard>
      </div>

      <!-- Раскрытый список отчётов бакета (реальные данные API; в клоне бакет статичен) -->
      <template v-if="opened">
        <div v-if="openedList.length === 0" class="muted" style="text-align: center; padding: 8px">
          {{ tt('empty_list') }}
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
              {{ tt('report') }}
            </button>
            <button
              class="btn ghost"
              style="width: auto; padding: 10px 14px; color: var(--stop-red); border-color: var(--stop-red)"
              @click="confirmDelete = a"
            >
              {{ tt('delete_session') }}
            </button>
          </div>
        </AppCard>
      </template>
    </div>

    <!-- Delete Session: подтверждение оригинала (delete_session_confirm) -->
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
        <div style="font-weight: 700; font-size: 18px">{{ tt('delete_session') }}</div>
        <div class="muted" style="margin-top: 8px">{{ tt('delete_session_confirm') }}</div>
        <div class="row" style="gap: 10px; margin-top: 16px">
          <button class="btn secondary grow" @click="confirmDelete = null">{{ tt('cancel_upper') }}</button>
          <button class="btn grow" style="background: var(--stop-red); color: #fff" @click="del">
            {{ tt('delete_upper') }}
          </button>
        </div>
      </div>
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
import { AppCard, MonoCaption, PillBadge, TopBar } from '../components/ui';
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

// Бакеты и цвета — как в LibraryScreens.kt.
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

function perStroke(b: Bucket, status?: AnalysisDto['status']): number {
  if (b.key === 'drills') return 0;
  return analysis.list.filter((a) => a.strokeType === b.key && (status === undefined || a.status === status))
    .length;
}

const count = (b: Bucket) => perStroke(b);
const done = (b: Bucket) => perStroke(b, 'completed');
const busy = (b: Bucket) => perStroke(b, 'analyzing') + perStroke(b, 'created');

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

// Delete Session — с подтверждением оригинала (delete_session_confirm).
const confirmDelete = ref<AnalysisDto | null>(null);

async function del(): Promise<void> {
  const a = confirmDelete.value;
  if (!a) return;
  confirmDelete.value = null;
  // Каскад: API удаляет Mongo-записи и объекты хранилища сессии.
  await analysis.remove(a.id).catch(() => undefined);
}

onMounted(() => {
  void analysis.fetchList().catch(() => undefined);
});
</script>
