import { defineConfig, type Plugin, type PluginOption } from 'vite';
import react from '@vitejs/plugin-react';
import { appearanceScript } from '@rookcom/monarch-tokens/appearance';

/** Injects Monarch's pre-paint appearance script, exactly as an app would. */
function appearance(): Plugin {
  return {
    name: 'monarch-appearance',
    transformIndexHtml: (html) =>
      html.replace(
        '<!--appearance-script-->',
        `<script>${appearanceScript({ defaultTheme: 'light' })}</script>`,
      ),
  };
}

// GitHub Pages serves the site at /monarch/; local dev and tests use /.
export default defineConfig({
  base: process.env.DOCS_BASE ?? '/',
  // plugin-react ships types against a newer Vite major; the runtime hook API is the same.
  plugins: [react() as unknown as PluginOption, appearance()],
  server: { port: 5173, strictPort: true },
  preview: { port: 4173, strictPort: true },
  build: { outDir: 'dist', sourcemap: true, target: 'es2022' },
});
