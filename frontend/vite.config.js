import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// El gateway Spring Boot sirve el build (mismo origen, sin CORS). En desarrollo se hace proxy al gateway.
export default defineConfig({
  plugins: [react()],
  build: { outDir: '../backend/api-gateway/src/main/resources/static', emptyOutDir: true },
  server: {
    port: 5173,
    proxy: { '/api': 'http://127.0.0.1:4000', '/uploads': 'http://127.0.0.1:4000' },
  },
  test: { environment: 'jsdom', globals: true, setupFiles: './src/setupTests.js' },
});