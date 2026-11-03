/**
 * Нативный TTS через Web Speech API (speechSynthesis) — то, что в оригинале делает
 * голос Axel (`axelSpeak`/Gemini TTS), но без внешнего сервиса: голос движка WebView.
 * Порт флагов оригинала: ttsOn (speaker в Axel) + одноразовый голосовой экранов
 * (`axel_recording_guide_shown_` → SPEAK_ONCE_KEYS).
 */

export function ttsAvailable(): boolean {
  return typeof window !== 'undefined' && 'speechSynthesis' in window;
}

/** Одноразовые голосовые подсказки-проводники (флаг в localStorage, как в оригинале). */
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
