/**
 * Native TTS via the Web Speech API (speechSynthesis) — what Axel's voice does in the
 * original (`axelSpeak`/Gemini TTS), but without an external service: the WebView engine's voice.
 * Port of the original's flags: ttsOn (speaker in Axel) + one-shot screen voice
 * (`axel_recording_guide_shown_` → SPEAK_ONCE_KEYS).
 */

export function ttsAvailable(): boolean {
  return typeof window !== 'undefined' && 'speechSynthesis' in window;
}

/** One-shot voice guide prompts (flag in localStorage, as in the original). */
const SPEAK_ONCE_KEYS = 'axel_guide_';

export function spokenOnce(key: string): boolean {
  return localStorage.getItem(SPEAK_ONCE_KEYS + key) !== null;
}

export function markSpoken(key: string): void {
  localStorage.setItem(SPEAK_ONCE_KEYS + key, '1');
}

export function speak(text: string, lang: 'ru' | 'en'): void {
  if (!ttsAvailable() || !text.trim()) return;
  stopSpeaking();
  const u = new SpeechSynthesisUtterance(text);
  u.lang = lang === 'ru' ? 'ru-RU' : 'en-US';
  u.rate = 1;
  const voice = window.speechSynthesis.getVoices().find((v) => v.lang.toLowerCase().startsWith(u.lang.slice(0, 2)));
  if (voice) u.voice = voice;
  window.speechSynthesis.speak(u);
}

export function stopSpeaking(): void {
  if (ttsAvailable()) window.speechSynthesis.cancel();
}
