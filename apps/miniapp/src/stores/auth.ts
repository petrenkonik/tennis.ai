import { computed, ref } from 'vue';
import { defineStore } from 'pinia';
import { api, loginWithTelegram, setToken } from '../api/client';
import { disableDemo, enableDemo, isDemo } from '../services/demoMode';
import { getInitDataRaw } from '../tma';
import type { QuotaState, UserDto } from '@tennis-ai/shared';

export const useAuthStore = defineStore('auth', () => {
  const user = ref<UserDto | null>(null);
  const ready = ref(false);
  const error = ref<string | null>(null);

  const quota = computed<QuotaState | null>(() => user.value?.quota ?? null);
  const isPro = computed(() => quota.value?.plan === 'pro');

  /**
   * Mock user for the demo mode (visitors outside Telegram, see services/demoBackend.ts)
   * and for local development (VITE_DEV_OFFLINE=1). The special error value
   * 'not_in_telegram' is an i18n key — App.vue translates it for display.
   */
  const DEMO_USER: UserDto = {
    id: 'demo-user',
    telegramId: 0,
    firstName: 'Demo',
    language: 'ru',
    xp: 0,
    quota: { plan: 'free', analysesUsed: 0, analysesLimit: 9, periodEndsAt: null },
    profile: { dominantHand: 'right' },
  };

  const isDemoUser = computed(() => isDemo() && user.value?.id === DEMO_USER.id);

  /** Enters the app without the backend: mock user + demo API stubs. */
  function enterDemo(): void {
    enableDemo();
    user.value = DEMO_USER;
    error.value = null;
    ready.value = true;
  }

  async function login(): Promise<void> {
    error.value = null;
    if (import.meta.env.DEV && import.meta.env.VITE_DEV_OFFLINE === '1') {
      enterDemo();
      return;
    }
    if (!getInitDataRaw()) {
      // Visitor outside Telegram: resume a previous demo session, or stop on the
      // error card where App.vue offers the "Try demo" button.
      if (isDemo()) enterDemo();
      else error.value = 'not_in_telegram';
      ready.value = true;
      return;
    }
    // Real Telegram launch — a stale demo flag must not shadow the real account.
    disableDemo();
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

  return { user, ready, error, quota, isPro, isDemoUser, enterDemo, login, refresh };
});
