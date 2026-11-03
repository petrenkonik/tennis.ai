<template>
  <div class="screen">
    <TopBar :title="tt('my_profile')" icon="person" variant="serif" :title-size="22">
      <template #left><Icon name="arrow-left" :size="26" /></template>
    </TopBar>

    <div style="padding: 16px; display: flex; flex-direction: column; gap: 14px">
      <!-- Ambassador Program (cardAccent, €0 on the right) -->
      <AppCard accent :radius="20">
        <div class="row" style="padding: 16px; gap: 14px">
          <div
            :style="{
              width: '52px',
              height: '52px',
              borderRadius: '14px',
              background: 'color-mix(in srgb, var(--accent) 18%, transparent)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              color: 'var(--accent)',
              flex: 'none',
            }"
          >
            <Icon name="person" :size="26" />
          </div>
          <div class="grow">
            <div style="font-size: 19px; font-weight: 700">{{ tt('ambassador_program') }}</div>
            <div class="muted" style="margin-top: 2px">{{ tt('ambassador_subtitle') }}</div>
          </div>
          <div style="font-size: 26px; font-weight: 700; color: var(--accent)">€0</div>
        </div>
      </AppCard>

      <!-- 2×2 info chips (values from the profile — as in the original) -->
      <div class="row" style="gap: 14px; align-items: stretch">
        <InfoChip icon="bars" :label="`${tt('level')}: ${skillLabel(app.skill, app.lang)}`" class="grow" />
        <InfoChip icon="clock" :label="expShort(app.experience, app.lang)" class="grow" />
      </div>
      <div class="row" style="gap: 14px; align-items: stretch">
        <InfoChip icon="target" :label="`${tt('goal')}: ${goalChip(app.goal, app.lang)}`" class="grow" />
        <InfoChip icon="trend" :label="`${tt('progress')}: ${app.progress}%`" class="grow" />
      </div>

      <!-- Free Plan; in the TMA the card leads to the Stars paywall (a deliberate deviation from the clone:
           the Mini App has real payments, CLONE_SPEC §2.6 → /paywall) -->
      <AppCard clickable :radius="20" @click="router.push('/paywall')">
        <div style="padding: 16px">
          <div class="row">
            <span style="font-size: 22px">🎾</span>
            <span style="font-size: 20px; font-weight: 700; margin-left: 12px" class="grow">
              {{ tt('free_plan') }}
            </span>
            <span
              :style="{
                borderRadius: '14px',
                background: 'var(--track)',
                padding: '6px 14px',
                fontSize: '12px',
                fontWeight: 700,
                color: quotaPro ? 'var(--accent)' : 'var(--text)',
              }"
            >
              {{ quotaPro ? 'PRO' : tt('free') }}
            </span>
          </div>
          <div class="muted" style="margin-top: 8px">{{ tt('free_desc') }}</div>
        </div>
      </AppCard>
    </div>

    <div style="padding: 4px 16px 0">
      <SectionBar :title="tt('settings')" />
    </div>
    <div style="padding: 12px 16px 0; display: flex; flex-direction: column; gap: 12px">
      <ToggleRow icon="bell" :label="tt('push_notifications')" v-model="app.pushOn" />
      <ToggleRow icon="cpu" :label="tt('ai_coaching_tips')" v-model="app.aiTipsOn" />
      <ToggleRow icon="mail" :label="tt('email_notifications')" v-model="app.emailOn" />
      <AppCard clickable :radius="16" @click="showLang = true">
        <div class="row" style="padding: 0 16px; height: 56px; gap: 14px">
          <Icon name="globe" :size="22" :style="{ color: 'var(--text)' }" />
          <span class="grow" style="font-size: 16px">{{ tt('language') }}</span>
          <span class="muted" style="font-size: 14px">{{ app.lang === 'ru' ? 'Русский' : 'English' }}</span>
          <Icon name="chevron-right" :size="20" :style="{ color: 'var(--text-secondary)' }" />
        </div>
      </AppCard>
      <AppCard :radius="16">
        <div class="row" style="padding: 0 16px; height: 56px; gap: 14px">
          <Icon name="info" :size="22" :style="{ color: 'var(--text)' }" />
          <span class="grow" style="font-size: 16px">{{ tt('app_version') }}</span>
          <Icon name="chevron-right" :size="20" :style="{ color: 'var(--text-secondary)' }" />
        </div>
      </AppCard>
    </div>

    <!-- Language selection dialog: RU (default) / EN -->
    <div
      v-if="showLang"
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
      @click.self="showLang = false"
    >
      <div class="card" style="width: 100%; border-radius: 20px">
        <div style="font-weight: 700; font-size: 18px; margin-bottom: 10px">{{ tt('language') }}</div>
        <div
          v-for="l in (['ru', 'en'] as const)"
          :key="l"
          class="row"
          style="gap: 8px; padding: 8px 0; cursor: pointer"
          @click="
            app.lang = l;
            showLang = false;
          "
        >
          <span
            :style="{
              width: '20px',
              height: '20px',
              borderRadius: '50%',
              border: `2px solid ${app.lang === l ? 'var(--accent)' : 'var(--text-secondary)'}`,
              display: 'inline-flex',
              alignItems: 'center',
              justifyContent: 'center',
            }"
          >
            <span
              v-if="app.lang === l"
              :style="{ width: '10px', height: '10px', borderRadius: '50%', background: 'var(--accent)' }"
            />
          </span>
          <span style="font-size: 16px">{{ l === 'ru' ? 'Русский' : 'English' }}</span>
        </div>
        <button class="btn ghost" style="margin-top: 10px" @click="showLang = false">{{ tt('back') }}</button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, defineComponent, h, ref, type CSSProperties } from 'vue';
import { useRouter } from 'vue-router';
import { useAppStore } from '../stores/app';
import { useAuthStore } from '../stores/auth';
import { expShort, goalChip, skillLabel, t } from '../i18n/T';
import { AppCard, SectionBar, ToggleRow, TopBar } from '../components/ui';
import Icon from '../components/Icon.vue';

const router = useRouter();
const app = useAppStore();
const auth = useAuthStore();

const tt = (key: string) => t(app.lang, key);
const showLang = ref(false);
const quotaPro = computed(() => auth.quota?.plan === 'pro');

/** Profile mini-chip (ProfileScreens.InfoChip): accent icon + 14px text. */
const InfoChip = defineComponent({
  name: 'InfoChip',
  props: { icon: { type: String, required: true }, label: { type: String, required: true } },
  setup(props) {
    return () =>
      h(
        AppCard,
        { radius: 16, style: { padding: '0' } as CSSProperties },
        {
          default: () =>
            h(
              'div',
              {
                class: 'row',
                style: { padding: '0 12px', height: '56px', gap: '10px' } as CSSProperties,
              },
              [
                h(Icon, { name: props.icon, size: 20, style: { color: 'var(--accent)' } as CSSProperties }),
                h(
                  'span',
                  {
                    style: {
                      fontSize: '14px',
                      color: 'var(--text)',
                      whiteSpace: 'nowrap',
                      overflow: 'hidden',
                      textOverflow: 'ellipsis',
                    } as CSSProperties,
                  },
                  props.label,
                ),
              ],
            ),
        },
      );
  },
});
</script>
