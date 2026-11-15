import { defineConfig } from 'vite';

export default defineConfig({
  base: './',
  build: {
    outDir: '../../app/src/main/assets/companion',
    emptyOutDir: true,
    rollupOptions: {
      output: {
        entryFileNames: 'companion.js',
        inlineDynamicImports: true,
        assetFileNames: '[name][extname]',
      },
    },
  },
});
