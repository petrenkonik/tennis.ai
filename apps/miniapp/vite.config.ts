import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';

export default defineConfig({
  plugins: [vue()],
  server: { port: 5173, host: true },
  build: {
    target: 'es2022',
    // MediaPipe/YOLO-модели и воркер грузятся отдельно, чанками
    rollupOptions: {
      output: {
        manualChunks: {
          telegram: ['@telegram-apps/sdk'],
          vendor: ['vue', 'vue-router', 'pinia'],
        },
      },
    },
  },
  worker: { format: 'es' },
});
