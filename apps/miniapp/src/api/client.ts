import { getInitDataRaw } from '../tma';
import type { UserDto } from '@tennis-ai/shared';

const BASE = import.meta.env.VITE_API_URL ?? 'http://localhost:3000/api';
const TOKEN_KEY = 'tennis_ai_jwt';

let token: string | null = localStorage.getItem(TOKEN_KEY);

export function setToken(t: string | null): void {
  token = t;
  if (t) localStorage.setItem(TOKEN_KEY, t);
  else localStorage.removeItem(TOKEN_KEY);
}

export function getToken(): string | null {
  return token;
}

export class ApiError extends Error {
  constructor(
    public status: number,
    message: string,
  ) {
    super(message);
  }
}

export async function api<T>(path: string, opts: RequestInit = {}): Promise<T> {
  const res = await fetch(`${BASE}${path}`, {
    ...opts,
    headers: {
      'content-type': 'application/json',
      ...(token ? { authorization: `Bearer ${token}` } : {}),
      ...((opts.headers as Record<string, string>) ?? {}),
    },
  });
  if (!res.ok) {
    const body = await res.text().catch(() => '');
    throw new ApiError(res.status, body || res.statusText);
  }
  return (await res.json()) as T;
}

/** POST /api/auth/telegram — exchanges initData for a JWT. Throws if not running inside Telegram. */
export async function loginWithTelegram(): Promise<UserDto> {
  const initData = getInitDataRaw();
  if (!initData) throw new ApiError(0, 'Not running inside Telegram (initData unavailable)');
  const res = await api<{ accessToken: string; user: UserDto }>('/auth/telegram', {
    method: 'POST',
    body: JSON.stringify({ initData }),
  });
  setToken(res.accessToken);
  return res.user;
}

/** Uploads a file to S3 via presigned PUT (the file goes directly, bypassing the API). */
export async function uploadToPresigned(url: string, file: Blob, contentType: string): Promise<void> {
  const res = await fetch(url, {
    method: 'PUT',
    headers: { 'content-type': contentType },
    body: file,
  });
  if (!res.ok) throw new ApiError(res.status, `upload failed: ${res.statusText}`);
}
