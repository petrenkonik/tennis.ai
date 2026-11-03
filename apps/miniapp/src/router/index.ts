import { createRouter, createWebHashHistory } from 'vue-router';

// Hash history: more reliable inside the Telegram WebView (no server rewrite config needed).
// Navigation map — CLONE_SPEC §3: tabs home|videos|analyses|dashboard|profile;
// select/edit/axel/record/live/analysis/drills/paywall — fullscreen, without bottom nav.
export const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    { path: '/', component: () => import('../views/HomeView.vue') },
    { path: '/videos', component: () => import('../views/VideosView.vue') },
    { path: '/analyses', component: () => import('../views/AnalysesView.vue') },
    { path: '/dashboard', redirect: '/dashboard/select' },
    { path: '/dashboard/select', component: () => import('../views/DashboardSelectView.vue') },
    { path: '/dashboard/player', component: () => import('../views/PlayerDashboardView.vue') },
    { path: '/dashboard/coach', component: () => import('../views/CoachDashboardView.vue') },
    { path: '/profile', component: () => import('../views/ProfileView.vue') },
    { path: '/profile/edit', component: () => import('../views/EditProfileView.vue') },
    { path: '/axel', component: () => import('../views/AxelView.vue') },
    { path: '/record', component: () => import('../views/RecordView.vue') },
    { path: '/preview', component: () => import('../views/PreviewView.vue') },
    { path: '/live', component: () => import('../views/LiveView.vue') },
    { path: '/analysis/:id', component: () => import('../views/AnalysisView.vue') },
    { path: '/drills', component: () => import('../views/DrillsView.vue') },
    { path: '/paywall', component: () => import('../views/PaywallView.vue') },
  ],
});
