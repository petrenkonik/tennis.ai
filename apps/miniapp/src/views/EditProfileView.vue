<template>
  <div class="screen full">
    <!-- Шапка: back · Edit Profile · Save -->
    <div class="row" style="height: 54px; padding: 0 8px; position: relative">
      <span class="side-btn" @click="router.back()"><Icon name="arrow-left" :size="26" /></span>
      <span
        class="grow"
        style="text-align: center; font-size: 19px; font-weight: 700; padding: 0 64px; white-space: nowrap; overflow: hidden"
      >
        {{ tt('edit_profile') }}
      </span>
      <span
        class="side-btn"
        style="color: var(--accent); font-weight: 600; font-size: 17px"
        @click="
          app.saveProfile();
          router.back();
        "
      >
        {{ tt('save') }}
      </span>
    </div>

    <div style="padding: 10px 16px 0">
      <!-- Сегмент General | Additional -->
      <div :style="{ background: 'var(--card)', borderRadius: '16px', height: '52px', padding: '5px', display: 'flex' }">
        <div
          v-for="(seg, i) in [tt('general'), tt('additional')]"
          :key="seg"
          class="grow"
          :style="{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            borderRadius: '12px',
            background: tab === i ? 'var(--accent)' : 'transparent',
            color: tab === i ? 'var(--on-accent)' : 'var(--text-secondary)',
            fontWeight: 600,
            fontSize: '16px',
            cursor: 'pointer',
          }"
          @click="tab = i"
        >
          {{ seg }}
        </div>
      </div>
    </div>

    <div style="padding: 18px 16px 0; display: flex; flex-direction: column; gap: 14px">
      <template v-if="tab === 0">
        <!-- General — восстановлен по логике (в скриншотах не показан, CLONE_SPEC §6.3) -->
        <FieldRow>
          <input class="input" v-model="app.name" :placeholder="tt('name')" />
        </FieldRow>
        <FieldRow>
          <input class="input" v-model="app.heightCm" inputmode="numeric" :placeholder="tt('height_cm')" />
        </FieldRow>
        <FieldRow>
          <input class="input" v-model="app.age" inputmode="numeric" :placeholder="tt('age')" />
        </FieldRow>
      </template>

      <template v-else>
        <!-- Additional — 1:1 по скриншоту -->
        <FieldRow>
          <div class="row" style="gap: 12px">
            <Icon name="scale" :size="20" :style="{ color: 'var(--accent)' }" />
            <input
              class="input"
              v-model="weight"
              inputmode="numeric"
              :placeholder="tt('weight_placeholder')"
              style="flex: 1"
            />
            <span v-if="weight.length > 0" class="muted" style="font-size: 15px">
              {{ tt('weight_placeholder').split(' ')[1] }}
            </span>
          </div>
        </FieldRow>

        <div class="mono-label" style="font-size: 13px; letter-spacing: 2px; color: var(--text-secondary); padding-top: 6px">
          {{ tt('skill_level') }}
        </div>
        <div class="chip-flow">
          <span
            v-for="(label, i) in SKILL_LEVELS.map((s) => skillLabel(s, app.lang))"
            :key="label"
            class="chip"
            :class="{ active: SKILL_LEVELS[i] === app.skill }"
            @click="app.skill = SKILL_LEVELS[i]"
          >
            {{ label }}
          </span>
        </div>

        <div class="mono-label" style="font-size: 13px; letter-spacing: 2px; color: var(--text-secondary); padding-top: 6px">
          {{ tt('years_experience') }}
        </div>
        <div class="chip-flow">
          <span
            v-for="(label, i) in EXPERIENCES.map((e) => expLabel(e, app.lang))"
            :key="label"
            class="chip"
            :class="{ active: EXPERIENCES[i] === app.experience }"
            @click="app.experience = EXPERIENCES[i]"
          >
            {{ label }}
          </span>
        </div>

        <div class="mono-label" style="font-size: 13px; letter-spacing: 2px; color: var(--text-secondary); padding-top: 6px">
          {{ tt('dominant_hand') }}
        </div>
        <div class="row" style="gap: 12px">
          <span
            v-for="right in [true, false]"
            :key="String(right)"
            class="chip grow"
            :class="{ active: app.handRight === right }"
            :style="{ justifyContent: 'center' }"
            @click="app.handRight = right"
          >
            <Icon name="hand" :size="18" />
            {{ handLabel(right, app.lang) }}
          </span>
        </div>

        <div class="mono-label" style="font-size: 13px; letter-spacing: 2px; color: var(--text-secondary); padding-top: 6px">
          {{ tt('main_goal') }}
        </div>
        <div class="chip-flow">
          <span
            v-for="(label, i) in MAIN_GOALS.map((g) => goalChip(g, app.lang))"
            :key="label"
            class="chip"
            :class="{ active: MAIN_GOALS[i] === app.goal }"
            @click="app.goal = MAIN_GOALS[i]"
          >
            {{ label }}
          </span>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { defineComponent, h, ref, watch, type CSSProperties } from 'vue';
import { useRouter } from 'vue-router';
import { useAppStore } from '../stores/app';
import {
  EXPERIENCES,
  MAIN_GOALS,
  SKILL_LEVELS,
  expLabel,
  goalChip,
  handLabel,
  skillLabel,
  t,
} from '../i18n/T';
import { AppCard } from '../components/ui';
import Icon from '../components/Icon.vue';

const router = useRouter();
const app = useAppStore();

const tt = (key: string) => t(app.lang, key);
const tab = ref(1); // 0 = General, 1 = Additional (как на скриншоте оригинала)

// Вес: только цифры, до 3 знаков — как в клоне (weightKg.filter(isDigit).take(3))
const weight = ref(app.weightKg);
watch(
  weight,
  (v) => {
    const clean = v.replace(/\D/g, '').slice(0, 3);
    if (clean !== v) weight.value = clean;
    app.weightKg = clean;
  },
  { immediate: true },
);

/** Строка-поле (EditProfile.FieldRow): карточка 56px с контентом. */
const FieldRow = defineComponent({
  name: 'FieldRow',
  setup(_, { slots }) {
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
                style: { padding: '0 16px', height: '56px' } as CSSProperties,
              },
              slots.default?.(),
            ),
        },
      );
  },
});
</script>

<style scoped>
.chip-flow {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.side-btn {
  display: inline-flex;
  align-items: center;
  padding: 8px;
  color: var(--text-secondary);
  cursor: pointer;
}
</style>
