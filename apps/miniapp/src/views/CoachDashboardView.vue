<template>
  <div class="screen">
    <!-- Top bar: person + COACH DASHBOARD monospace WHITE (CLONE_SPEC §2.5) -->
    <TopBar :title="tt('coach_dashboard')" icon="person" variant="mono-white" />

    <div style="padding: 18px 16px 0">
      <div style="text-align: center">
        <div style="font-size: 28px; font-weight: 700; color: var(--accent)">{{ tt('welcome_coach') }}</div>
        <div class="muted" style="margin-top: 6px; font-size: 15px">{{ tt('student_overview') }}</div>
      </div>

      <div style="display: flex; flex-direction: column; gap: 14px; margin-top: 22px">
        <div class="row" style="gap: 14px; align-items: stretch">
          <StatCell icon="person" value="0" :label="tt('my_students')" class="grow" />
          <StatCell icon="bars" value="—" :label="tt('analysis')" class="grow" />
        </div>
        <div class="row" style="gap: 14px; align-items: stretch">
          <StatCell icon="doc" value="0" :label="tt('training_plans')" mono class="grow" />
          <StatCell icon="chat" value="—" :label="tt('messages')" mono class="grow" />
        </div>

        <AppCard accent>
          <div class="row" style="padding: 16px; gap: 14px">
            <Icon name="gift" :size="26" :style="{ color: 'var(--accent)' }" />
            <div class="grow">
              <div style="font-size: 17px; font-weight: 700">{{ tt('get_1_year_free') }}</div>
              <div class="muted" style="margin-top: 2px; line-height: 17px; font-size: 13px">
                {{ tt('add_students_free') }}
              </div>
            </div>
            <Icon name="arrow-right" :size="20" :style="{ color: 'var(--text-secondary)' }" />
          </div>
        </AppCard>

        <div style="height: 8px"></div>
        <SectionBar :title="tt('recent_activity')" />

        <AppCard>
          <div style="text-align: center; padding: 20px 12px">
            <Icon name="person" :size="30" :style="{ color: 'var(--text-dim)', margin: '0 auto' }" />
            <div class="muted" style="margin-top: 10px; font-size: 15px">{{ tt('no_student_activity') }}</div>
            <div class="dim" style="margin-top: 6px; font-size: 13px">{{ tt('add_students_to_see') }}</div>
          </div>
        </AppCard>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useAppStore } from '../stores/app';
import { t } from '../i18n/T';
import { AppCard, SectionBar, TopBar } from '../components/ui';
import Icon from '../components/Icon.vue';
import { StatCell } from '../components/StatCell';

const app = useAppStore();
const tt = (key: string) => t(app.lang, key);
</script>
