import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { defineConfig, type Plugin, type PluginOption } from 'vite';
import react from '@vitejs/plugin-react';
import { appearanceScript } from '@rookcom/monarch-tokens/appearance';
import { productScript } from './src/lib/appearance.js';
import { monarchDocs } from './plugins/monarch-docs.js';
import { guides } from './plugins/guides.js';

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

/**
 * Per-entry styles, exactly as an app should load them: the root stylesheet
 * (`styles/core.css`) is global, and any module that imports a subpath entry
 * (`@rookcom/monarch-react/ai`) also imports that entry's stylesheet. Each page
 * then ships only the component CSS it uses (docs/guides/getting-started.md).
 */
function entryStyles(): Plugin {
  const ENTRY_IMPORT = /from\s+['"]@rookcom\/monarch-react\/([a-z-]+)['"]/g;
  return {
    name: 'monarch-entry-styles',
    enforce: 'pre',
    transform(code, id) {
      if (!/\.(tsx?|jsx?)$/.test(id) || id.includes('node_modules')) return null;
      const entries = new Set<string>();
      for (const m of code.matchAll(ENTRY_IMPORT)) if (m[1] && m[1] !== 'styles') entries.add(m[1]);
      if (entries.size === 0) return null;
      const imports = [...entries]
        .map((e) => `import '@rookcom/monarch-react/styles/${e}.css';`)
        .join('\n');
      // Appended, not prepended: imports are hoisted anyway, and source map
      // lines stay where they were.
      return { code: `${code}\n${imports}\n`, map: null };
    },
  };
}

/** Weights preloaded for the first paint: body text (400) and headings (600), latin subset. */
const PRELOADED_FONTS = [
  /^assets\/poppins-400-latin-[\w-]{8}\.woff2$/,
  /^assets\/poppins-600-latin-[\w-]{8}\.woff2$/,
];

/**
 * Self-hosted Poppins (src/styles/fonts.css): preloads the weights the first
 * paint needs, so text never waits for the stylesheet to be parsed before its
 * font is requested, and ships the font's licence next to the files.
 */
function fonts(): Plugin {
  let base = '/';
  return {
    name: 'monarch-fonts',
    apply: 'build',
    configResolved(config) {
      base = config.base;
    },
    generateBundle() {
      this.emitFile({
        type: 'asset',
        fileName: 'assets/Poppins-OFL.txt',
        source: readFileSync(new URL('./src/fonts/OFL.txt', import.meta.url)),
      });
    },
    transformIndexHtml: {
      order: 'post',
      handler(html, ctx) {
        const files = Object.keys(ctx.bundle ?? {}).filter((f) =>
          PRELOADED_FONTS.some((re) => re.test(f)),
        );
        if (files.length !== PRELOADED_FONTS.length)
          throw new Error(
            `monarch-fonts: expected ${PRELOADED_FONTS.length} preloaded fonts, found ${files.join(', ') || 'none'}`,
          );
        return {
          html,
          tags: files.map((f) => ({
            tag: 'link',
            attrs: {
              rel: 'preload',
              href: `${base}${f}`,
              as: 'font',
              type: 'font/woff2',
              crossorigin: '',
            },
            injectTo: 'head-prepend' as const,
          })),
        };
      },
    },
  };
}

const repoRoot = fileURLToPath(new URL('../..', import.meta.url));

// GitHub Pages serves the site at /monarch/; local dev and tests use /.
export default defineConfig({
  base: process.env.DOCS_BASE ?? '/',
  // plugin-react ships types against a newer Vite major; the runtime hook API is the same.
  plugins: [
    entryStyles(),
    monarchDocs({ repoRoot }),
    guides(),
    react() as unknown as PluginOption,
    appearance(),
    fonts(),
  ],
  server: { port: 5173, strictPort: true },
  preview: { port: 4173, strictPort: true },
  build: {
    outDir: 'dist',
    sourcemap: true,
    target: 'es2022',
    // The manifest maps each page to its chunks: the prerender step writes
    // preload links from it, and the performance budget reads it.
    manifest: true,
    // Fonts stay files (cacheable, preloadable), never inlined into CSS.
    assetsInlineLimit: (file) => (file.endsWith('.woff2') ? false : undefined),
  },
  ssr: {
    // The server build for prerendering bundles the workspace packages so
    // their CSS imports resolve the same way they do in the browser build.
    noExternal: [/^@rookcom\//],
  },
});
