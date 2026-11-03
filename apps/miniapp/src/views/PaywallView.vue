<template>
  <div class="screen full">
    <TopBar :title="tt('paywall_title')" variant="mono-accent">
      <template #left>
        <span style="cursor: pointer; display: inline-flex" @click="router.back()">
          <Icon name="arrow-left" :size="26" />
        </span>
      </template>
    </TopBar>

    <div style="padding: 18px 16px 0; display: flex; flex-direction: column; gap: 14px">
      <AppCard accent :radius="20">
        <div style="padding: 16px">
          <div class="row" style="gap: 12px">
            <Icon name="sparkles" :size="26" :style="{ color: 'var(--accent)' }" />
            <span style="font-size: 20px; font-weight: 700">{{ tt('paywall_title') }}</span>
          </div>
          <div class="muted" style="margin-top: 10px; line-height: 19px">{{ tt('paywall_desc') }}</div>
        </div>
      </AppCard>

      <AppCard v-for="p in products" :key="p.id">
        <div class="row" style="justify-content: space-between; gap: 10px">
          <div>
            <div style="font-size: 17px; font-weight: 600">{{ p.title }}</div>
            <div class="muted" style="margin-top: 4px">{{ p.stars }} ★</div>
          </div>
          <button class="btn" style="width: auto; padding: 12px 20px" :disabled="busy" @click="buy(p.id)">
            {{ tt('pay') }} {{ p.stars }} ★
          </button>
        </div>
      </AppCard>

      <div v-if="status" class="card">
        <span class="muted">{{ status }}</span>
      </div>
      <div v-if="auth.quota" class="card">
        <span class="muted">
          {{ auth.quota.plan === 'pro'
            ? `Pro · ${auth.quota.analysesUsed}/${auth.quota.analysesLimit}`
            : `Free · ${auth.quota.analysesUsed}/${auth.quota.analysesLimit}` }}
        </span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { api } from '../api/client';
import { useAppStore } from '../stores/app';
import { useAuthStore } from '../stores/auth';
import { t } from '../i18n/T';
import { openPaymentInvoice } from '../tma';
import type { InvoiceResponse, ProductId } from '@tennis-ai/shared';
import { AppCard, TopBar } from '../components/ui';
import Icon from '../components/Icon.vue';

const router = useRouter();
const app = useAppStore();
const auth = useAuthStore();

const tt = (key: string) => t(app.lang, key);

const products: Array<{ id: ProductId; title: string; stars: number }> = [
  { id: 'pro_1m', title: '1 month', stars: 250 },
  { id: 'pro_3m', title: '3 months', stars: 600 },
  { id: 'pro_12m', title: '12 months', stars: 2000 },
];

const busy = ref(false);
const status = ref('');

async function buy(productId: ProductId): Promise<void> {
  busy.value = true;
  status.value = '';
  try {
    const res = await api<InvoiceResponse>('/payments/invoice', {
      method: 'POST',
      body: JSON.stringify({ productId }),
    });
    openPaymentInvoice(res.link);
    void auth.refresh().catch(() => undefined);
  } catch (e) {
    status.value = `${tt('failed')}: ${e instanceof Error ? e.message : e}`;
  } finally {
    busy.value = false;
  }
}
</script>
