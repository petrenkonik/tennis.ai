import { init, retrieveLaunchParams } from '@telegram-apps/sdk';

let initialized = false;

/** Инициализация @telegram-apps/sdk. Вне Telegram (dev в браузере) — молча продолжаем. */
export function initTma(): void {
  if (initialized) return;
  try {
    init();
    initialized = true;
  } catch {
    // Не в среде Telegram — dev-режим.
  }
}

/** Сырой initData для POST /api/auth/telegram; null вне Telegram. */
export function getInitDataRaw(): string | null {
  try {
    const raw = retrieveLaunchParams().initDataRaw as string | null | undefined;
    return raw ?? null;
  } catch {
    return null;
  }
}

/** Открыть Stars-инвойс (t.me/$ ссылка из createInvoiceLink). */
export function openPaymentInvoice(link: string): void {
  const w = window as unknown as { Telegram?: { WebApp?: { openInvoice?: (url: string) => void } } };
  try {
    w.Telegram?.WebApp?.openInvoice?.(link);
    return;
  } catch {
    // fallthrough
  }
  window.open(link, '_blank');
}
