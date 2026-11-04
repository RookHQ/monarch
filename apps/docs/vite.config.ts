import { fileURLToPath } from 'node:url';
import { defineConfig, type Plugin, type PluginOption } from 'vite';
import react from '@vitejs/plugin-react';
import { appearanceScript } from '@rookcom/monarch-tokens/appearance';
import { productScript } from './src/lib/appearance.js';
import { monarchDocs } from './plugins/monarch-docs.js';

/**
 * Injects Monarch's pre-paint appearance script, exactly as an app would, plus
 * the docs-only product accent preview. `system` follows the visitor's OS until
 * they choose a theme.
 */
function appearance(): Plugin {
  return {
    name: 'monarch-appearance',
    transformIndexHtml: (html) =>
      html.replace(
        '<!--appearance-script-->',
        `<script>${appearanceScript({ defaultTheme: 'system' })}</script><script>${productScript}</script>`,
      ),
  };
}

const repoRoot = fileURLToPath(new URL('../..', import.meta.url));

// GitHub Pages serves the site at /monarch/; local dev and tests use /.
export default defineConfig({
  base: process.env.DOCS_BASE ?? '/',
  // plugin-react ships types against a newer Vite major; the runtime hook API is the same.
  plugins: [monarchDocs({ repoRoot }), react() as unknown as PluginOption, appearance()],
  server: { port: 5173, strictPort: true },
  preview: { port: 4173, strictPort: true },
  build: { outDir: 'dist', sourcemap: true, target: 'es2022' },
});
