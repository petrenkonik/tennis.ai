import { init, retrieveLaunchParams } from '@telegram-apps/sdk';

let initialized = false;

/** Initializes @telegram-apps/sdk. Outside Telegram (browser dev) — silently continue. */
export function initTma(): void {
  if (initialized) return;
  try {
    init();
    initialized = true;
  } catch {
    // Not in a Telegram environment — dev mode.
  }
}

/** Raw initData for POST /api/auth/telegram; null outside Telegram. */
export function getInitDataRaw(): string | null {
  try {
    const raw = retrieveLaunchParams().initDataRaw as string | null | undefined;
    return raw ?? null;
  } catch {
    return null;
  }
}

/** Opens a Stars invoice (t.me/$ link from createInvoiceLink). */
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
