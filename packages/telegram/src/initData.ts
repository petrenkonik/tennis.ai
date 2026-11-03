import { createHmac, timingSafeEqual } from 'node:crypto';

// Верификация initData Telegram Mini App по официальной схеме:
// secret_key = HMAC_SHA256(key="WebAppData", data=bot_token)
// hash       = HMAC_SHA256(key=secret_key, data=data_check_string)
// https://core.telegram.org/bots/webapps#validating-data-received-via-the-mini-app

export interface TmaUser {
  id: number;
  first_name?: string;
  last_name?: string;
  username?: string;
  photo_url?: string;
  language_code?: string;
  is_premium?: boolean;
}

export interface InitDataValidation {
  ok: boolean;
  user?: TmaUser;
  authDate?: number;
  reason?: string;
}

export function validateInitData(initData: string, botToken: string, maxAgeSec = 86_400): InitDataValidation {
  if (!initData || !botToken) return { ok: false, reason: 'initData or bot token missing' };

  const params = new URLSearchParams(initData);
  const hash = params.get('hash');
  if (!hash) return { ok: false, reason: 'hash missing' };
  params.delete('hash');
  // signature ( Bot API 7.10+, для валидации третьими сторонами ) не участвует в data_check_string
  params.delete('signature');

  const dataCheckString = [...params.entries()]
    .sort(([a], [b]) => (a < b ? -1 : a > b ? 1 : 0))
    .map(([k, v]) => `${k}=${v}`)
    .join('\n');

  const secretKey = createHmac('sha256', 'WebAppData').update(botToken).digest();
  const computed = createHmac('sha256', secretKey).update(dataCheckString).digest('hex');

  const a = Buffer.from(computed, 'hex');
  const b = Buffer.from(hash, 'hex');
  if (a.length !== b.length || !timingSafeEqual(a, b)) {
    return { ok: false, reason: 'hash mismatch' };
  }

  const authDate = Number(params.get('auth_date'));
  if (!authDate || Date.now() / 1000 - authDate > maxAgeSec) {
    return { ok: false, reason: 'auth_date expired or missing' };
  }

  const rawUser = params.get('user');
  if (!rawUser) return { ok: false, reason: 'user missing' };
  try {
    const user = JSON.parse(rawUser) as TmaUser;
    if (!user?.id) return { ok: false, reason: 'user.id missing' };
    return { ok: true, user, authDate };
  } catch {
    return { ok: false, reason: 'user JSON malformed' };
  }
}
