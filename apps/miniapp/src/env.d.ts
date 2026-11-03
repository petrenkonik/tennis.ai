/// <reference types="vite/client" />

declare module '*.vue' {
  import type { DefineComponent } from 'vue';
  const component: DefineComponent<Record<string, never>, Record<string, never>, unknown>;
  export default component;
}

interface ImportMetaEnv {
  readonly VITE_API_URL?: string;
  /** Самохостинг MediaPipe WASM (по умолчанию CDN jsdelivr, версия = @mediapipe/tasks-vision). */
  readonly VITE_MEDIAPIPE_WASM_URL?: string;
  /** Самохостинг модели pose_landmarker_lite.task (по умолчанию Google storage). */
  readonly VITE_POSE_MODEL_URL?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
