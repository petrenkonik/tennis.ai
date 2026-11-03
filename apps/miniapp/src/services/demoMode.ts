/**
 * Demo-mode flag for visitors who opened the app outside Telegram (no initData).
 * The flag is persisted so a page reload stays in demo; a real Telegram launch
 * clears it (see stores/auth.ts).
 */

const DEMO_KEY = 'tennis_ai_demo';

let demo = false;
try {
  demo = localStorage.getItem(DEMO_KEY) === '1';
} catch {
  // Storage unavailable (private mode) — demo works for the current session only.
}

export function isDemo(): boolean {
  return demo;
}

export function enableDemo(): void {
  demo = true;
  try {
    localStorage.setItem(DEMO_KEY, '1');
  } catch {
    // Ignore — in-memory flag is enough for the session.
  }
}

export function disableDemo(): void {
  demo = false;
  try {
    localStorage.removeItem(DEMO_KEY);
  } catch {
    // Ignore.
  }
}
