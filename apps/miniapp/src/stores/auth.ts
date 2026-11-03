import { computed, ref } from 'vue';
import { defineStore } from 'pinia';
import { api, loginWithTelegram, setToken } from '../api/client';
import type { QuotaState, UserDto } from '@tennis-ai/shared';

export const useAuthStore = defineStore('auth', () => {
  const user = ref<UserDto | null>(null);
  const ready = ref(false);
  const error = ref<string | null>(null);

  const quota = computed<QuotaState | null>(() => user.value?.quota ?? null);
  const isPro = computed(() => quota.value?.plan === 'pro');

  /** Local development outside Telegram: VITE_DEV_OFFLINE=1 serves a mock user without the API. */
  const DEV_USER: UserDto = {
    id: 'dev-local',
    telegramId: 0,
    firstName: 'Dev',
    language: 'ru',
    xp: 0,
    quota: { plan: 'free', analysesUsed: 0, analysesLimit: 9, periodEndsAt: null },
    profile: { dominantHand: 'right' },
  };

  async function login(): Promise<void> {
    error.value = null;
    if (import.meta.env.DEV && import.meta.env.VITE_DEV_OFFLINE === '1') {
      user.value = DEV_USER;
      ready.value = true;
      return;
    }
    try {
      user.value = await loginWithTelegram();
    } catch (e) {
      setToken(null);
      error.value = e instanceof Error ? e.message : String(e);
    } finally {
      ready.value = true;
    }
  }

  async function refresh(): Promise<void> {
    if (!tokenPresent()) return;
    user.value = await api<UserDto>('/users/me');
  }

  function tokenPresent(): boolean {
    return localStorage.getItem('tennis_ai_jwt') !== null;
  }

  return { user, ready, error, quota, isPro, login, refresh };
});
