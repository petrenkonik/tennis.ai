<template>
  <div class="screen">
    <!-- Top bar: globe + AXEL AI monospace WHITE (CLONE_SPEC §2.4) -->
    <TopBar title="AXEL AI" icon="globe" variant="mono-white" />

    <div style="padding: 18px 16px 0">
      <div style="text-align: center">
        <div style="font-size: 28px; font-weight: 700; color: var(--accent)">{{ tt('my_dashboard') }}</div>
        <!-- The original's misaligned i18n key — verbatim (CLONE_SPEC §2.4) -->
        <div class="muted" style="margin-top: 6px; font-size: 15px">{{ t(app.lang, 'track_progression') }}</div>
      </div>

      <div style="display: flex; flex-direction: column; gap: 14px; margin-top: 22px">
        <div class="row" style="gap: 14px; align-items: stretch">
          <StatCell icon="target" :value="String(sessionsCount)" :label="tt('my_sessions')" class="grow" />
          <StatCell icon="bars" :value="skillScore" :label="tt('skill_analysis')" class="grow" />
        </div>
        <div class="row" style="gap: 14px; align-items: stretch">
          <StatCell icon="doc" value="0" :label="tt('training_plans')" mono class="grow" />
          <StatCell icon="chat" value="0" :label="tt('messages')" mono class="grow" />
        </div>

        <!-- Referral banner (cardAccent) -->
        <AppCard accent clickable @click="router.push('/paywall')">
          <div class="row" style="padding: 16px; gap: 14px">
            <Icon name="gift" :size="26" :style="{ color: 'var(--accent)' }" />
            <div class="grow">
              <div style="font-size: 17px; font-weight: 700">{{ tt('get_1_year_free') }}</div>
              <div class="muted" style="margin-top: 2px; line-height: 17px; font-size: 13px">
                {{ tt('invite_10_friends') }}
              </div>
            </div>
            <Icon name="arrow-right" :size="20" :style="{ color: 'var(--text-secondary)' }" />
          </div>
        </AppCard>

        <div style="height: 8px"></div>
        <SectionBar :title="tt('recent_activity')" />

        <!-- The original's empty state ("record_first_video" — key verbatim) or the latest sessions -->
        <AppCard v-if="analysis.list.length === 0" clickable @click="router.push('/record')">
          <div style="text-align: center; padding: 20px 12px">
            <Icon name="videocam" :size="30" :style="{ color: 'var(--text-dim)', margin: '0 auto' }" />
            <div class="muted" style="margin-top: 10px; font-size: 15px">{{ tt('no_sessions_yet') }}</div>
            <div class="dim" style="margin-top: 6px; font-size: 13px">{{ t(app.lang, 'record_first_video') }}</div>
          </div>
        </AppCard>
        <AppCard v-for="a in recent" :key="a.id" clickable @click="router.push(`/analysis/${a.id}`)">
          <div class="row" style="justify-content: space-between; gap: 10px">
            <div style="font-weight: 600; text-transform: capitalize">{{ a.strokeType }}</div>
            <div class="muted">
              {{ a.overallScore != null ? a.overallScore + '/100' : a.status }}
              · {{ new Date(a.createdAt).toLocaleDateString() }}
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
import { t } from '../i18n/T';
import { AppCard, SectionBar, TopBar } from '../components/ui';
import Icon from '../components/Icon.vue';
import { StatCell } from '../components/StatCell';

const router = useRouter();
const app = useAppStore();
const analysis = useAnalysisStore();

const tt = (key: string) => t(app.lang, key);

const sessionsCount = computed(() => analysis.list.length);
const skillScore = computed(() => {
  const done = analysis.list.filter((a) => a.status === 'completed' && a.overallScore != null);
  const last = done[0];
  return last && last.overallScore != null ? String(last.overallScore) : '—';
});
const recent = computed(() => analysis.list.slice(0, 3));

onMounted(() => {
  void analysis.fetchList().catch(() => undefined);
});
</script>
