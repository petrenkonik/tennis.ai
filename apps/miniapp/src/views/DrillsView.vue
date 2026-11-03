<template>
  <div class="screen full">
    <TopBar :title="tt('drills_title')" variant="mono-accent">
      <template #left>
        <span style="cursor: pointer; display: inline-flex" @click="router.back()">
          <Icon name="arrow-left" :size="26" />
        </span>
      </template>
    </TopBar>

    <div style="padding: 18px 16px 0; display: flex; flex-direction: column; gap: 14px">
      <div v-if="loading" class="card">
        <span class="row" style="gap: 10px">
          <span class="spinner" style="margin: 0"></span>
          <span class="muted">{{ tt('loading') }}</span>
        </span>
      </div>

      <div v-else-if="drills.length === 0" class="card">
        <span class="muted">{{ tt('empty_list') }}</span>
      </div>

      <AppCard v-for="d in drills" :key="d.slug">
        <div style="font-size: 17px; font-weight: 600">{{ d.title }}</div>
        <div class="muted" style="margin: 6px 0 4px">{{ d.objective }}</div>
        <div class="dim" style="font-size: 13px">
          {{ d.repsSets }} · {{ d.difficulty }} · {{ tt('targets') }}:
          {{ (d.targetMetrics ?? []).join(', ') || '—' }}
        </div>
      </AppCard>

      <button class="btn secondary" :disabled="loading" @click="load(true)">
        {{ tt('only_weaknesses') }}
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { api } from '../api/client';
import { useAppStore } from '../stores/app';
import { t } from '../i18n/T';
import { AppCard, TopBar } from '../components/ui';
import Icon from '../components/Icon.vue';

const router = useRouter();
const app = useAppStore();
const tt = (key: string) => t(app.lang, key);

interface DrillRow {
  slug: string;
  title: string;
  objective?: string;
  repsSets?: string;
  difficulty?: string;
  targetMetrics?: string[];
}

const drills = ref<DrillRow[]>([]);
const loading = ref(true);

async function load(recommended = false): Promise<void> {
  loading.value = true;
  try {
    drills.value = await api<DrillRow[]>(recommended ? '/drills/recommended' : '/drills');
  } finally {
    loading.value = false;
  }
}

onMounted(() => void load());
</script>
