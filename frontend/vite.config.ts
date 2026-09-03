import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// 复赛一体交付：dev 用 proxy 直连 8080 后端；build 产物经 scripts/copy-static.mjs 进 Spring Boot static
export default defineConfig({
  plugins: [react()],
  base: './',
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
  build: {
    outDir: 'dist',
  },
});
