import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';

export default defineConfig({
  plugins: [vue()],
  server: { port: 5173, host: true },
  build: {
    target: 'es2022',
    // MediaPipe/YOLO models and the worker are loaded separately, as chunks
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
