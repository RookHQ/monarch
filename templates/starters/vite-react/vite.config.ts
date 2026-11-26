import tailwindcss from '@tailwindcss/vite';
import react from '@vitejs/plugin-react';
import { defineConfig } from 'vite';
import { appearanceScript } from '@rookcom/monarch-tokens/appearance';
import brand from './brand.json';

// A colour in brand.json sets the accent on :root. Without one, the neutral accent applies.
const appearance = appearanceScript(brand.accent ? {} : { product: 'neutral' });

export default defineConfig({
  plugins: [
    react(),
    tailwindcss(),
    {
      // Sets the theme, contrast and accent on <html> before first paint.
      name: 'monarch-appearance',
      transformIndexHtml: () => [{ tag: 'script', children: appearance, injectTo: 'head-prepend' }],
    },
  ],
});
