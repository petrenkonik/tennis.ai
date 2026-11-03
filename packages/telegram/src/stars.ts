// Telegram Stars (XTR) via the Bot API. Important: Stars do not support auto-renew —
// a subscription = buying a period, its expiry is stored on the backend, and the bot
// reminds the user to renew (see reference/DECISIONS.md §2 "Payments").

export interface StarsProduct {
  id: 'pro_1m' | 'pro_3m' | 'pro_12m';
  title: string;
  months: number;
  stars: number;
}

export const PRO_PRODUCTS: StarsProduct[] = [
  { id: 'pro_1m', title: 'Tennis AI Pro — 1 month', months: 1, stars: 250 },
  { id: 'pro_3m', title: 'Tennis AI Pro — 3 months', months: 3, stars: 600 },
  { id: 'pro_12m', title: 'Tennis AI Pro — 12 months', months: 12, stars: 2000 },
];

export function productById(id: string): StarsProduct | undefined {
  return PRO_PRODUCTS.find((p) => p.id === id);
}

export interface BotApiResponse<T> {
  ok: boolean;
  result?: T;
  description?: string;
}

export async function callBotApi<T>(botToken: string, method: string, body: Record<string, unknown>): Promise<BotApiResponse<T>> {
  const res = await fetch(`https://api.telegram.org/bot${botToken}/${method}`, {
    method: 'POST',
    headers: { 'content-type': 'application/json' },
    body: JSON.stringify(body),
  });
  return (await res.json()) as BotApiResponse<T>;
}

/**
 * createInvoiceLink for Stars: currency=XTR, empty provider_token (required for XTR),
 * prices — a single item in stars. The client opens the link via Telegram.WebApp.openInvoice.
 */
export async function createStarsInvoiceLink(
  botToken: string,
  opts: { title: string; description: string; payload: string; stars: number },
): Promise<{ ok: boolean; link?: string; error?: string }> {
  const res = await callBotApi<string>(botToken, 'createInvoiceLink', {
    title: opts.title,
    description: opts.description,
    payload: opts.payload,
    currency: 'XTR',
    prices: [{ label: opts.title, amount: opts.stars }],
  });
  if (!res.ok || !res.result) return { ok: false, error: res.description ?? 'createInvoiceLink failed' };
  return { ok: true, link: res.result };
}

export function buildSubscriptionPayload(userId: number, productId: string): string {
  return `sub:${userId}:${productId}`;
}

/** payload of the form `sub:<userId>:<productId>` → parts; null for a foreign/malformed payload. */
export function parseSubscriptionPayload(payload: string): { userId: number; productId: string } | null {
  const parts = payload.split(':');
  if (parts.length !== 3 || parts[0] !== 'sub') return null;
  const userId = Number(parts[1]);
  if (!Number.isFinite(userId)) return null;
  return { userId, productId: parts[2] };
}
