<template>
  <div class="screen full" style="display: flex; flex-direction: column; padding: 0 20px; text-align: center">
    <div style="flex: 0.9"></div>
    <Icon name="grid" :size="56" :style="{ color: 'var(--accent)', margin: '0 auto' }" />
    <div style="height: 22px"></div>
    <GlowText :text="tt('select_dashboard')" :size="27" />
    <div style="height: 44px"></div>

    <AppCard clickable :radius="20" @click="router.push('/dashboard/player')">
      <div class="row" style="padding: 16px; gap: 16px; text-align: left">
        <div
          :style="{
            width: '56px',
            height: '56px',
            borderRadius: '16px',
            background: '#17220d',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            color: 'var(--accent)',
            flex: 'none',
          }"
        >
          <Icon name="person" :size="28" />
        </div>
        <div class="grow">
          <div class="mono-label" style="font-size: 15px; color: var(--accent)">{{ tt('player_dashboard') }}</div>
          <div class="muted" style="margin-top: 4px; line-height: 18px; font-size: 14px">
            {{ tt('track_personal_progress') }}
          </div>
        </div>
        <Icon name="arrow-right" :size="22" :style="{ color: 'var(--text-secondary)' }" />
      </div>
    </AppCard>

    <div style="height: 18px"></div>

    <AppCard clickable :radius="20" @click="router.push('/dashboard/coach')">
      <div class="row" style="padding: 16px; gap: 16px; text-align: left">
        <div
          :style="{
            width: '56px',
            height: '56px',
            borderRadius: '16px',
            background: '#17220d',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            color: 'var(--accent)',
            flex: 'none',
          }"
        >
          <Icon name="person" :size="28" />
        </div>
        <div class="grow">
          <div class="mono-label" style="font-size: 15px; color: var(--accent)">{{ tt('coach_dashboard') }}</div>
          <!-- The original's misaligned i18n key — reproduced verbatim (CLONE_SPEC §2.3) -->
          <div class="muted" style="margin-top: 4px; line-height: 18px; font-size: 14px">
            {{ t(app.lang, 'manage_students_perf') }}
          </div>
          <div class="muted" style="margin-top: 4px; line-height: 18px; font-size: 14px">
            {{ t(app.lang, 'manage_students_perf') }}
          </div>
        </div>
        <Icon name="arrow-right" :size="22" :style="{ color: 'var(--text-secondary)' }" />
      </div>
    </AppCard>

    <div style="flex: 1"></div>
    <span
      class="mono-label"
      style="font-size: 14px; color: var(--text-secondary); cursor: pointer; padding: 16px"
      @click="router.back()"
    >
      {{ tt('back') }}
    </span>
    <div style="flex: 0.7"></div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router';
import { useAppStore } from '../stores/app';
import { t } from '../i18n/T';
import { AppCard, GlowText } from '../components/ui';
import Icon from '../components/Icon.vue';

const router = useRouter();
const app = useAppStore();
const tt = (key: string) => t(app.lang, key);
</script>
