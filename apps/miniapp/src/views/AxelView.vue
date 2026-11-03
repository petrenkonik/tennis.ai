<template>
  <div class="screen full" style="display: flex; flex-direction: column">
    <!-- Шапка (CLONE_SPEC §2.2) -->
    <div class="row" style="padding: 10px 14px; gap: 8px">
      <Icon name="sparkles" :size="22" :style="{ color: 'var(--accent)' }" />
      <span style="font-size: 22px; font-weight: 700">Axel</span>
      <span
        :style="{
          borderRadius: '14px',
          border: '1px solid #66792f',
          padding: '4px 12px',
          color: 'var(--accent-soft)',
          fontSize: '14px',
          cursor: 'pointer',
        }"
        @click="
          app.voiceModeAuto = !app.voiceModeAuto;
        "
      >
        {{ app.voiceModeAuto ? tt('auto') : tt('manual') }}
      </span>
      <span class="grow"></span>
      <!-- Speaker: нативный TTS вкл/выкл (в оригинале — голос Axel; тут speechSynthesis WebView) -->
      <Icon
        name="volume"
        :size="22"
        :style="{ color: app.ttsOn ? 'var(--accent)' : 'var(--text-secondary)', cursor: 'pointer' }"
        @click="toggleTts"
      />
      <span
        :style="{
          width: '26px',
          height: '26px',
          borderRadius: '7px',
          background: 'var(--stop-red)',
          display: 'inline-flex',
          alignItems: 'center',
          justifyContent: 'center',
          cursor: 'pointer',
          opacity: busy ? 1 : 0.85,
        }"
        @click="stop"
      >
        <span :style="{ width: '10px', height: '10px', borderRadius: '2px', background: '#fff' }" />
      </span>
      <Icon name="arrow-down" :size="24" :style="{ color: 'var(--text-secondary)' }" @click="router.back()" />
    </div>

    <!-- Idle: голограф + «Meet Axel AI» + подсказки 2×2 -->
    <div v-if="messages.length === 0" style="flex: 1; overflow-y: auto; padding-bottom: 12px">
      <div style="padding: 0 20px">
        <svg class="hologram" viewBox="0 0 300 300" style="height: 300px" aria-hidden="true">
          <defs>
            <radialGradient id="holo-glow" cx="50%" cy="55%" r="50%">
              <stop offset="0%" stop-color="#5ce8c8" stop-opacity="0.16" />
              <stop offset="100%" stop-color="#5ce8c8" stop-opacity="0" />
            </radialGradient>
          </defs>
          <circle cx="150" cy="160" r="125" fill="url(#holo-glow)" />
          <!-- голова + волосы -->
          <circle cx="150" cy="88" r="25" fill="none" stroke="#5ce8c8" stroke-width="2.2" stroke-linecap="round" />
          <path
            d="M126 80 a25 25 0 0 1 48 0"
            fill="none"
            stroke="#5ce8c8"
            stroke-opacity="0.5"
            stroke-width="5"
            stroke-linecap="round"
          />
          <!-- шея -->
          <path d="M143 113 v12" stroke="#5ce8c8" stroke-width="2.2" stroke-linecap="round" />
          <path d="M157 113 v12" stroke="#5ce8c8" stroke-width="2.2" stroke-linecap="round" />
          <!-- плечи и торс (поло) -->
          <path
            d="M136 128 L164 128 L182 140 Q196 152 194 262 L106 262 Q104 152 118 140 Z"
            fill="none"
            stroke="#5ce8c8"
            stroke-opacity="0.5"
            stroke-width="2.2"
            stroke-linecap="round"
          />
          <!-- воротник поло -->
          <path d="M150 130 L132 148" stroke="#5ce8c8" stroke-width="2.4" stroke-linecap="round" />
          <path d="M150 130 L168 148" stroke="#5ce8c8" stroke-width="2.4" stroke-linecap="round" />
          <!-- частицы на торсе -->
          <circle cx="128" cy="180" r="2.4" fill="#5ce8c8" fill-opacity="0.6" />
          <circle cx="168" cy="200" r="1.8" fill="#5ce8c8" fill-opacity="0.5" />
          <circle cx="150" cy="225" r="2.6" fill="#5ce8c8" fill-opacity="0.55" />
          <circle cx="118" cy="235" r="1.6" fill="#5ce8c8" fill-opacity="0.45" />
          <circle cx="182" cy="235" r="2" fill="#5ce8c8" fill-opacity="0.5" />
        </svg>
      </div>

      <div style="text-align: center; margin-top: 18px">
        <span style="font-size: 34px; font-weight: 700">{{ tt('meet') }} </span>
        <span style="font-size: 34px; font-weight: 700; color: var(--accent)">Axel AI</span>
      </div>
      <div class="muted" style="text-align: center; margin-top: 10px; font-size: 17px">{{ tt('companion') }}</div>

      <div style="padding: 22px 16px 0; display: flex; flex-direction: column; gap: 12px">
        <div v-for="(row, ri) in suggestionRows" :key="ri" class="row" style="gap: 12px; align-items: stretch">
          <AppCard
            v-for="s in row"
            :key="s"
            clickable
            :radius="16"
            class="grow"
            :style="{ height: '150px' }"
            @click="send(tt(s))"
          >
            <div style="height: 100%; display: flex; flex-direction: column; align-items: center; justify-content: center; text-align: center; padding: 14px">
              <div style="font-size: 16px; font-weight: 700">{{ tt(s) }}</div>
              <div class="muted" style="margin-top: 6px; line-height: 17px">{{ tt(bodyKey(s)) }}</div>
            </div>
          </AppCard>
        </div>
      </div>
    </div>

    <!-- Переписка: пузыри (пользователь — accent справа, Axel — #1B1B1D слева) -->
    <div v-else ref="chatEl" style="flex: 1; overflow-y: auto; padding: 6px 14px; display: flex; flex-direction: column; gap: 8px">
      <div v-for="(m, i) in messages" :key="i" class="bubble" :class="m.role === 'user' ? 'user' : 'coach'">
        {{ m.text }}
      </div>
      <div v-if="busy" class="muted" style="padding: 6px">{{ tt('thinking') }}</div>
    </div>

    <div v-if="error" style="padding: 0 16px 6px; color: var(--stop-red); font-size: 12px">{{ error }}</div>

    <!-- Нижняя панель: Record/Upload · input · Speak/Send -->
    <div class="row" style="background: var(--bar); padding: 8px 10px; gap: 8px; align-items: flex-end">
      <LabeledCircle :label="tt('record')" icon="videocam" @click="router.push('/record')" />
      <LabeledCircle :label="tt('upload')" icon="upload" @click="router.push('/record')" />
      <div
        class="grow"
        :style="{
          background: '#1b1b1d',
          borderRadius: '26px',
          padding: '14px 16px',
        }"
      >
        <input
          v-model="draft"
          class="input"
          style="font-size: 15px"
          :placeholder="tt('reply_hint')"
          @keyup.enter="send()"
        />
      </div>
      <LabeledCircle :label="tt('speak')" icon="mic" @click="toastNow(tt('toast_voice_in'))" />
      <LabeledCircle :label="tt('send')" icon="send" filled @click="send()" />
    </div>

    <!-- Тост (аналог Toast в клоне) -->
    <div
      v-if="toast"
      style="
        position: fixed;
        bottom: 96px;
        left: 50%;
        transform: translateX(-50%);
        background: #1b1b1d;
        border: 1px solid var(--card-border);
        color: var(--text);
        border-radius: 12px;
        padding: 10px 16px;
        font-size: 13px;
        z-index: 60;
        max-width: 86vw;
      "
    >
      {{ toast }}
    </div>
  </div>
</template>

<script setup lang="ts">
import { defineComponent, h, nextTick, onBeforeUnmount, ref, watch, type CSSProperties } from 'vue';
import { useRouter } from 'vue-router';
import { api } from '../api/client';
import { useAppStore } from '../stores/app';
import { speak, stopSpeaking, ttsAvailable } from '../services/tts';
import { t } from '../i18n/T';
import type { StrokeType } from '@tennis-ai/shared';
import { AppCard } from '../components/ui';
import Icon from '../components/Icon.vue';

const router = useRouter();
const app = useAppStore();

const tt = (key: string) => t(app.lang, key);

// CLONE_SPEC §2.2 — карточки-подсказки оригинала (тексты из REA; обрезанные на скриншотах дополнены)
const SUGGESTIONS = ['s1_title', 's2_title', 's3_title', 's4_title'] as const;
const suggestionRows: string[][] = [SUGGESTIONS.slice(0, 2), SUGGESTIONS.slice(2)];

function bodyKey(titleKey: string): string {
  return titleKey.replace('_title', '_body');
}

interface ClientAction {
  tool: string;
  args: Record<string, unknown>;
}

const messages = ref<Array<{ role: 'user' | 'coach'; text: string }>>([]);
const draft = ref('');
const busy = ref(false);
const error = ref<string | null>(null);
const toast = ref('');
const chatEl = ref<HTMLElement | null>(null);
let abort: AbortController | null = null;
let toastTimer: ReturnType<typeof setTimeout> | undefined;

function toastNow(text: string): void {
  toast.value = text;
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => (toast.value = ''), 2200);
}

onBeforeUnmount(() => {
  abort?.abort();
  stopSpeaking();
  clearTimeout(toastTimer);
});

watch(
  () => [messages.value.length, busy.value] as const,
  async () => {
    await nextTick();
    chatEl.value?.scrollTo({ top: chatEl.value.scrollHeight });
  },
);

/** Красный stop: отменяет текущий запрос и заглушает речь (как job?.cancel() в клоне). */
function stop(): void {
  stopSpeaking();
  if (busy.value) {
    abort?.abort();
    busy.value = false;
  }
}

function toggleTts(): void {
  if (!ttsAvailable()) {
    toastNow(tt('toast_voice_out'));
    return;
  }
  app.ttsOn = !app.ttsOn;
  if (!app.ttsOn) stopSpeaking();
}

/** Навигация Axel: destination из navigate() → роут мини-аппа. */
const DESTINATION_ROUTES: Record<string, string> = {
  home: '/',
  record: '/record',
  live: '/live',
  library: '/analyses',
  drills: '/drills',
  drill_library: '/drills',
  training_plans: '/drills',
  results: '/analyses',
  paywall: '/paywall',
  coach_home: '/',
};

function applyActions(actions: ClientAction[]): void {
  for (const a of actions) {
    switch (a.tool) {
      case 'navigate': {
        const dest = String(a.args.destination ?? 'home');
        const route = DESTINATION_ROUTES[dest];
        if (route) void router.push(route);
        break;
      }
      case 'analyze_video':
      case 'start_live': {
        // mode: camera | gallery обрабатывается на RecordView; strokeType прокидываем query-параметром.
        const stroke = typeof a.args.strokeType === 'string' ? (a.args.strokeType as StrokeType) : '';
        void router.push(stroke ? `/record?stroke=${stroke}` : '/record');
        break;
      }
      case 'open_drill_detail':
        void router.push('/drills');
        break;
      case 'set_app_language': {
        const code = String(a.args.code ?? a.args.language ?? '');
        if (code.startsWith('ru') || code.startsWith('en')) app.lang = code.startsWith('ru') ? 'ru' : 'en';
        break;
      }
      default:
        break;
    }
  }
}

async function send(text = draft.value): Promise<void> {
  const clean = text.trim();
  if (!clean || busy.value) return;
  draft.value = '';
  messages.value.push({ role: 'user', text: clean });
  const history = messages.value
    .slice(0, -1)
    .slice(-16)
    .map((m) => ({ role: m.role === 'user' ? 'user' : 'assistant', content: m.text }));
  busy.value = true;
  error.value = null;
  abort = new AbortController();
  try {
    const res = await api<{ reply: string; actions?: ClientAction[] }>(
      '/coach/chat',
      {
        method: 'POST',
        body: JSON.stringify({ message: clean, history }),
        signal: abort.signal,
      },
    );
    messages.value.push({ role: 'coach', text: res.reply });
    // Озвучка ответа нативным TTS (speaker on — как голос Axel в оригинале).
    if (app.ttsOn) speak(res.reply, app.lang);
    applyActions(res.actions ?? []);
  } catch (e) {
    if ((e as Error).name !== 'AbortError') {
      error.value = `GLM: ${e instanceof Error ? e.message : e}`.slice(0, 160);
    }
  } finally {
    busy.value = false;
    abort = null;
  }
}

/** Круглая кнопка с подписью (AxelScreen.LabeledCircle). */
const LabeledCircle = defineComponent({
  name: 'LabeledCircle',
  props: {
    label: { type: String, required: true },
    icon: { type: String, required: true },
    filled: { type: Boolean, default: false },
  },
  emits: ['click'],
  setup(props, { emit }) {
    return () =>
      h('div', { style: { textAlign: 'center' } as CSSProperties }, [
        h('div', { style: { fontSize: '11px', color: 'var(--text-secondary)' } as CSSProperties }, props.label),
        h('div', { style: { height: '4px' } as CSSProperties }),
        h(
          'div',
          {
            style: {
              width: '46px',
              height: '46px',
              borderRadius: '50%',
              background: props.filled ? 'var(--accent)' : '#1b1b1d',
              border: props.filled ? 'none' : '1px solid #2c2c30',
              color: props.filled ? 'var(--on-accent)' : 'var(--text)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              cursor: 'pointer',
            } as CSSProperties,
            onClick: () => emit('click'),
          },
          h(Icon, { name: props.icon, size: 20 }),
        ),
      ]);
  },
});
</script>
