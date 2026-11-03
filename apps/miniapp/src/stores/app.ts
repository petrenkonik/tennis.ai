import { computed, ref, watch } from 'vue';
import { defineStore } from 'pinia';
import {
  DEFAULT_LANG,
  type Experience,
  type Lang,
  type MainGoal,
  type SkillLevel,
} from '../i18n/T';

/**
 * Shared application state — port of `tennis-ai-android/AppState.kt`:
 * language, player profile (Edit Profile), settings and the Axel mode chip.
 * Persisted to localStorage (an analogue of the clone's SharedPreferences).
 */

const STORE_KEY = 'tennis_ai_app_state';

export const useAppStore = defineStore('app', () => {
  // --- Language (Russian is the default, as in the clone) ---
  const lang = ref<Lang>(DEFAULT_LANG);

  // --- Profile (CLONE_SPEC §4) ---
  const name = ref('');
  const heightCm = ref('');
  const age = ref('');
  const weightKg = ref('');
  const skill = ref<SkillLevel>('BEGINNER');
  const experience = ref<Experience>('UNDER_1');
  const handRight = ref(true);
  const goal = ref<MainGoal>('IMPROVE');
  const progress = ref(78); // value from the original screenshot; recomputed after a manual Save

  // --- Settings ---
  const pushOn = ref(true);
  const aiTipsOn = ref(true);
  const emailOn = ref(true);
  const voiceModeAuto = ref(false); // Auto/Manual chip in Axel
  const ttsOn = ref(true); // speaker in Axel: native TTS voices the answers

  // --- Restore (before the first Save — defaults as in AppState.kt) ---
  let persisted = false;
  try {
    const raw = localStorage.getItem(STORE_KEY);
    if (raw) {
      const p = JSON.parse(raw) as Record<string, unknown>;
      if (p.lang === 'en' || p.lang === 'ru') lang.value = p.lang;
      if (typeof p.name === 'string') name.value = p.name;
      if (typeof p.heightCm === 'string') heightCm.value = p.heightCm;
      if (typeof p.age === 'string') age.value = p.age;
      if (typeof p.weightKg === 'string') weightKg.value = p.weightKg;
      if (typeof p.skill === 'string') skill.value = p.skill as SkillLevel;
      if (typeof p.experience === 'string') experience.value = p.experience as Experience;
      if (typeof p.handRight === 'boolean') handRight.value = p.handRight;
      if (typeof p.goal === 'string') goal.value = p.goal as MainGoal;
      if (typeof p.progress === 'number') progress.value = p.progress;
      if (typeof p.pushOn === 'boolean') pushOn.value = p.pushOn;
      if (typeof p.aiTipsOn === 'boolean') aiTipsOn.value = p.aiTipsOn;
      if (typeof p.emailOn === 'boolean') emailOn.value = p.emailOn;
      if (typeof p.voiceModeAuto === 'boolean') voiceModeAuto.value = p.voiceModeAuto;
      if (typeof p.ttsOn === 'boolean') ttsOn.value = p.ttsOn;
      persisted = true;
    }
  } catch {
    // corrupted JSON — start from defaults
  }

  watch(
    [lang, name, heightCm, age, weightKg, skill, experience, handRight, goal, progress, pushOn, aiTipsOn, emailOn, voiceModeAuto, ttsOn],
    () => {
      localStorage.setItem(
        STORE_KEY,
        JSON.stringify({
          lang: lang.value,
          name: name.value,
          heightCm: heightCm.value,
          age: age.value,
          weightKg: weightKg.value,
          skill: skill.value,
          experience: experience.value,
          handRight: handRight.value,
          goal: goal.value,
          progress: progress.value,
          pushOn: pushOn.value,
          aiTipsOn: aiTipsOn.value,
          emailOn: emailOn.value,
          voiceModeAuto: voiceModeAuto.value,
          ttsOn: ttsOn.value,
        }),
      );
    },
    { deep: true },
  );

  /** Fill state of the 9 profile fields → "Progress: N%" (CLONE_SPEC §2.6). */
  function recomputeProgress(): void {
    const filled = [
      name.value.trim().length > 0,
      heightCm.value.length > 0,
      age.value.length > 0,
      weightKg.value.length > 0,
      true,
      true,
      true,
      true,
      true, // skill/experience/hand/goal + presets
    ];
    progress.value = Math.min(100, Math.max(0, Math.round((filled.filter(Boolean).length * 100) / 9)));
  }

  /** "Save" in Edit Profile — progress recompute + persist (watch above). */
  function saveProfile(): void {
    recomputeProgress();
  }

  /** Defaults from the Telegram profile on first launch (name; hand — from the API profile). */
  function initFromTelegram(firstName?: string, dominantHand?: 'left' | 'right'): void {
    if (!persisted && name.value.length === 0 && firstName) name.value = firstName;
    if (!persisted && dominantHand) handRight.value = dominantHand !== 'left';
  }

  const langRef = computed(() => lang.value);

  return {
    lang,
    langRef,
    name,
    heightCm,
    age,
    weightKg,
    skill,
    experience,
    handRight,
    goal,
    progress,
    pushOn,
    aiTipsOn,
    emailOn,
    voiceModeAuto,
    ttsOn,
    recomputeProgress,
    saveProfile,
    initFromTelegram,
  };
});
