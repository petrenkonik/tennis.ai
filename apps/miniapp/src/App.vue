<template>
  <div v-if="!auth.ready" class="screen full" style="display: flex; align-items: center; justify-content: center">
    <div style="text-align: center">
      <div class="spinner"></div>
      <p class="muted">{{ t(app.lang, 'starting') }}</p>
    </div>
  </div>
  <div
    v-else-if="auth.error"
    class="screen full"
    style="display: flex; align-items: center; justify-content: center; padding: 24px"
  >
    <div class="card" style="width: 100%">
      <p style="margin: 0 0 10px">{{ errorText }}</p>
      <button class="btn" @click="auth.login()">{{ t(app.lang, 'retry_login') }}</button>
      <button
        v-if="!inTelegram"
        class="btn secondary"
        style="margin-top: 10px"
        @click="auth.enterDemo()"
      >
        {{ t(app.lang, 'try_demo') }}
      </button>
    </div>
  </div>
  <router-view v-else />

  <!-- Demo badge: persistent reminder that no real account/backend is used. -->
  <span
    v-if="auth.isDemoUser"
    style="
      position: fixed;
      bottom: 76px;
      right: 12px;
      z-index: 60;
      font-size: 11px;
      font-weight: 700;
      letter-spacing: 0.1em;
      padding: 4px 12px;
      border-radius: 999px;
      background: var(--accent);
      color: #fff;
    "
  >
    {{ t(app.lang, 'demo_badge') }}
  </span>

  <template v-if="chrome">
    <nav v-if="showNav" class="bottom-nav">
      <a
        v-for="tab in tabs"
        :key="tab.key"
        :class="{ 'router-link-active': tab.active }"
        @click="go(tab)"
      >
        <Icon :name="tab.icon" :size="26" />
      </a>
    </nav>
    <AxelFab v-if="fab" @click="router.push('/axel')" />
  </template>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useAuthStore } from './stores/auth';
import { useAppStore } from './stores/app';
import { t } from './i18n/T';
import { getInitDataRaw } from './tma';
import Icon from './components/Icon.vue';
import { AxelFab } from './components/ui';

const route = useRoute();
const router = useRouter();
const auth = useAuthStore();
const app = useAppStore();

// Telegram environment is fixed for the lifetime of the page.
const inTelegram = getInitDataRaw() !== null;
// 'not_in_telegram' from the auth store is an i18n key, not a raw message.
const errorText = computed(() =>
  auth.error === 'not_in_telegram' ? t(app.lang, 'not_in_telegram') : auth.error,
);

onMounted(() => {
  void auth.login().then(() => {
    // Profile defaults from Telegram on first launch (AppState.initFromTelegram).
    if (auth.user) app.initFromTelegram(auth.user.firstName, auth.user.profile.dominantHand);
  });
});

// Bottom-navigation tabs (CLONE_SPEC §3): select/edit/axel/record/live/analysis/drills/paywall — fullscreen.
const chrome = computed(() => auth.ready && !auth.error);
const tabRoutes = ['/', '/videos', '/analyses', '/dashboard/player', '/dashboard/coach', '/profile'];
const showNav = computed(() => tabRoutes.includes(route.path));
const fab = showNav;

/** The grid tab leads to select_dashboard; the rest lead to their tab roots. */
function dashboardTarget(): string {
  return route.path === '/dashboard/player' || route.path === '/dashboard/coach'
    ? route.path
    : '/dashboard/select';
}

const tabs = computed(() => [
  { key: 'home', icon: 'home', to: '/', active: route.path === '/' },
  { key: 'videos', icon: 'videocam', to: '/videos', active: route.path === '/videos' },
  { key: 'analyses', icon: 'chart', to: '/analyses', active: route.path === '/analyses' },
  { key: 'dashboard', icon: 'grid', to: dashboardTarget(), active: route.path.startsWith('/dashboard') },
  { key: 'profile', icon: 'person', to: '/profile', active: route.path === '/profile' },
]);

function go(tab: { to: string }): void {
  void router.push(tab.to);
}
</script>
