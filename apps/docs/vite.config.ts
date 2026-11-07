import { existsSync, readFileSync } from 'node:fs';
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
 * Per-component styles, exactly as an app can load them: the root stylesheet
 * (`styles/core.css`) is global, and a module that imports components from a
 * subpath entry (`import { AiThread } from '@rookcom/monarch-react/ai'`) also
 * imports `styles/components/<id>.css` for each of them, so each page ships
 * only the component CSS it uses. Names are mapped to component folders with
 * registry/components.json.
 */
function entryStyles(): Plugin {
  const IMPORT = /import\s*\{([^}]*)\}\s*from\s*['"]@rookcom\/monarch-react\/([a-z-]+)['"]/g;
  let folderOf: Map<string, string> | null = null;
  const lookup = () => {
    if (folderOf) return folderOf;
    const registry = JSON.parse(
      readFileSync(new URL('../../registry/components.json', import.meta.url), 'utf8'),
    ) as { components: { exports: string[]; source?: string; entry: string }[] };
    folderOf = new Map();
    for (const c of registry.components)
      if (c.source && c.entry !== '.')
        for (const e of c.exports) folderOf.set(e, c.source.split('/').pop() ?? '');
    return folderOf;
  };
  return {
    name: 'monarch-entry-styles',
    enforce: 'pre',
    buildStart() {
      folderOf = null;
    },
    transform(code, id) {
      if (!/\.(tsx?|jsx?)$/.test(id) || id.includes('node_modules')) return null;
      const sheets = new Set<string>();
      for (const m of code.matchAll(IMPORT)) {
        if (m[2] === 'styles') continue;
        for (const raw of (m[1] ?? '').split(',')) {
          const name = raw
            .trim()
            .replace(/^type\s+/, '')
            .split(/\s+as\s+/)[0];
          if (!name || raw.trim().startsWith('type ')) continue;
          const folder = lookup().get(name);
          // Folders whose CSS ships in core.css have no sheet of their own.
          if (
            folder &&
            existsSync(
              new URL(`../../packages/react/dist/styles/components/${folder}.css`, import.meta.url),
            )
          )
            sheets.add(`@rookcom/monarch-react/styles/components/${folder}.css`);
        }
      }
      if (sheets.size === 0) return null;
      const imports = [...sheets].map((s) => `import '${s}';`).join('\n');
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
