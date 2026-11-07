/**
 * Docs site performance budget (docs/guides/docs-site.md#performance-budget).
 *
 * Reads the production build (`npm run build:docs`, apps/docs/dist) and fails when:
 *   - the home page's initial JavaScript or CSS (everything its prerendered HTML
 *     loads before any interaction) exceeds its budget, gzip;
 *   - any other page's initial JavaScript or CSS exceeds the page budget;
 *   - any single JavaScript chunk exceeds the chunk budget (the entry has its own);
 *   - a page's initial JavaScript contains code it must not load up front
 *     (the editor, charts, other pages' docs files, the search palette), checked
 *     against the sources listed in each chunk's source map.
 * `--report` also prints every page and the largest chunks, gzip and brotli.
 * Raise a limit only in a reviewed PR that explains why the page needs it.
 */
import { existsSync, readFileSync, readdirSync } from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { brotliCompressSync, constants, gzipSync } from 'node:zlib';

const KB = 1024;
// 156: the eager page index grows ~70 B gzip per component; wave 10 added 28 (2026-11-07).
const HOME_JS = 156 * KB;
const HOME_CSS = 60 * KB;
// Component pages load their own docs file and the component it documents; the
// editor's pages are the heaviest (the editor itself is about 115 KB).
const PAGE_JS = 290 * KB;
const PAGE_CSS = 60 * KB;
const ENTRY_CHUNK = 135 * KB;
const CHUNK = 125 * KB;

/**
 * Code a page must not load before interaction. `everywhere` rules hold on
 * every page; the others on every page except component pages, which load the
 * component they document (an editor page loads the editor).
 */
const NEVER_INITIAL = [
  [/features[/\\]search[/\\]search-(dialog|index)/, 'the search palette', 'everywhere'],
  [/appearance-panel|nav-drawer/, 'a lazily opened panel', 'everywhere'],
  [/[/\\]marked[/\\]/, 'the Markdown parser (guides are rendered at build time)', 'everywhere'],
  [/@tiptap\/|prosemirror-|rich-text-editor/, 'the rich text editor'],
  [/[/\\]charts[/\\]/, 'chart components'],
  [/command-palette/, 'the command palette'],
];

const dist = fileURLToPath(new URL('../../apps/docs/dist/', import.meta.url));
if (!existsSync(path.join(dist, 'index.html'))) {
  console.error(
    'Docs performance budget: apps/docs/dist is missing; run `npm run build:docs` first.',
  );
  process.exit(1);
}
const report = process.argv.includes('--report');

const sizes = new Map();
function sizeOf(file) {
  let s = sizes.get(file);
  if (!s) {
    const raw = readFileSync(path.join(dist, file));
    s = {
      raw: raw.length,
      gzip: gzipSync(raw, { level: 9 }).length,
      brotli: brotliCompressSync(raw, {
        params: { [constants.BROTLI_PARAM_QUALITY]: 11 },
      }).length,
    };
    sizes.set(file, s);
  }
  return s;
}
const sourcesOf = (file) => {
  const map = path.join(dist, `${file}.map`);
  return existsSync(map) ? JSON.parse(readFileSync(map, 'utf8')).sources : [];
};

/** Every prerendered page (each `index.html` under dist, plus 404.html), keyed by route. */
function pages(dir = dist, route = '') {
  const out = [];
  for (const entry of readdirSync(dir, { withFileTypes: true })) {
    if (entry.isDirectory() && entry.name !== 'assets' && !entry.name.startsWith('.'))
      out.push(...pages(path.join(dir, entry.name), `${route}/${entry.name}`));
    else if (entry.name === 'index.html') out.push([route || '/', path.join(dir, entry.name)]);
  }
  return out;
}

const html = (file) => readFileSync(file, 'utf8');
// The base path, from the entry script: assets are referenced as `<base>assets/…`.
const assetsOfPage = (source) => {
  const js = [
    ...source.matchAll(/<script type="module"[^>]*src="[^"]*?(assets\/[^"]+\.js)"/g),
    ...source.matchAll(/<link rel="modulepreload"[^>]*href="[^"]*?(assets\/[^"]+\.js)"/g),
  ].map((m) => m[1]);
  const css = [
    ...source.matchAll(/<link rel="stylesheet"[^>]*href="[^"]*?(assets\/[^"]+\.css)"/g),
  ].map((m) => m[1]);
  return { js: [...new Set(js)], css: [...new Set(css)] };
};
const total = (files, key = 'gzip') => files.reduce((n, f) => n + sizeOf(f)[key], 0);
const kb = (n) => `${(n / KB).toFixed(1)} KB`;

const errors = [];
const rows = [];
const all = pages();
const notFound = path.join(dist, '404.html');
if (existsSync(notFound)) all.push(['404.html', notFound]);
if (all.length < 2) errors.push('no prerendered pages found in apps/docs/dist');

for (const [route, file] of all) {
  const source = html(file);
  const { js, css } = assetsOfPage(source);
  // A renamed page's old address serves the new page: judge it by the page it renders.
  const rendered = /<div id="root" data-route="([^"]+)"/.exec(source)?.[1] ?? route;
  const isHome = route === '/';
  const jsGz = total(js);
  const cssGz = total(css);
  rows.push({
    route,
    js: jsGz,
    jsBr: total(js, 'brotli'),
    css: cssGz,
    cssBr: total(css, 'brotli'),
    n: js.length,
  });
  const [jsMax, cssMax] = isHome ? [HOME_JS, HOME_CSS] : [PAGE_JS, PAGE_CSS];
  const label = isHome ? 'home page' : route;
  if (jsGz > jsMax) errors.push(`${label}: initial JS ${kb(jsGz)} gzip exceeds ${kb(jsMax)}`);
  if (cssGz > cssMax) errors.push(`${label}: initial CSS ${kb(cssGz)} gzip exceeds ${kb(cssMax)}`);
  const ownDocs = rendered.startsWith('/components/') ? `${rendered.slice(12)}.docs.tsx` : null;
  for (const chunk of js) {
    for (const source of sourcesOf(chunk)) {
      if (source.endsWith('.docs.tsx') && !(ownDocs && source.endsWith(`/${ownDocs}`)))
        errors.push(`${label}: loads another page's docs file up front (${path.basename(source)})`);
      for (const [re, what, scope] of NEVER_INITIAL)
        if ((scope === 'everywhere' || !ownDocs) && re.test(source))
          errors.push(`${label}: loads ${what} up front (${chunk}: ${source})`);
    }
  }
}

const entry = assetsOfPage(html(path.join(dist, 'index.html'))).js[0];
const chunks = readdirSync(path.join(dist, 'assets'))
  .filter((f) => f.endsWith('.js'))
  .map((f) => `assets/${f}`);
for (const chunk of chunks) {
  const { gzip } = sizeOf(chunk);
  const max = chunk === entry ? ENTRY_CHUNK : CHUNK;
  if (gzip > max) errors.push(`${chunk}: ${kb(gzip)} gzip exceeds the ${kb(max)} chunk budget`);
}

const home = rows.find((r) => r.route === '/');
const worst = [...rows].sort((a, b) => b.js - a.js)[0];
const summary = `${rows.length} pages; home ${kb(home?.js ?? 0)} JS + ${kb(home?.css ?? 0)} CSS gzip; heaviest page ${worst?.route} ${kb(worst?.js ?? 0)} JS; ${chunks.length} chunks`;

if (report) {
  const pad = (s, n) => String(s).padEnd(n);
  console.log(
    `${pad('page', 44)} ${pad('JS gzip', 10)} ${pad('JS br', 10)} ${pad('CSS gzip', 10)} ${pad('CSS br', 10)} chunks`,
  );
  for (const r of [...rows].sort((a, b) => b.js - a.js))
    console.log(
      `${pad(r.route, 44)} ${pad(kb(r.js), 10)} ${pad(kb(r.jsBr), 10)} ${pad(kb(r.css), 10)} ${pad(kb(r.cssBr), 10)} ${r.n}`,
    );
  console.log(`\n${pad('largest chunks', 56)} ${pad('raw', 11)} ${pad('gzip', 10)} brotli`);
  for (const c of [
    ...chunks,
    ...readdirSync(path.join(dist, 'assets'))
      .filter((f) => f.endsWith('.css'))
      .map((f) => `assets/${f}`),
  ]
    .map((f) => [f, sizeOf(f)])
    .sort((a, b) => b[1].gzip - a[1].gzip)
    .slice(0, 25))
    console.log(
      `${pad(c[0], 56)} ${pad(kb(c[1].raw), 11)} ${pad(kb(c[1].gzip), 10)} ${kb(c[1].brotli)}`,
    );
  console.log('');
}

if (errors.length) {
  console.error(
    `Docs performance budget exceeded (${summary}):\n- ${[...new Set(errors)].join('\n- ')}`,
  );
  process.exit(1);
}
console.log(`Docs performance budget ok: ${summary}`);
