/**
 * Prerenders every page of the docs site to static HTML (docs/guides/docs-site.md).
 *
 * Runs after the client build (`dist/`) and the server build of
 * src/entry-server.tsx (`dist-ssr/`). For each route it writes
 * `dist/<route>/index.html`: the page's HTML inside #root (marked with its
 * route, so the client knows it may hydrate), its title, description and
 * canonical link, and preload links for exactly the chunks and styles the page
 * needs. It also writes `404.html`, `sitemap.xml` and `robots.txt`, then
 * removes `dist-ssr/`.
 *
 * DOCS_SITE_URL is the public address of the site, used for canonical links
 * and the sitemap (default: https://monarch.rookhq.com + the base path).
 */
import { mkdirSync, readFileSync, rmSync, writeFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import path from 'node:path';

const here = path.dirname(fileURLToPath(import.meta.url));
const dist = path.join(here, '../dist');
const ssrDir = path.join(here, '../dist-ssr');

const template = readFileSync(path.join(dist, 'index.html'), 'utf8');
const manifest = JSON.parse(readFileSync(path.join(dist, '.vite/manifest.json'), 'utf8'));
const entryKey = Object.keys(manifest).find((k) => manifest[k].isEntry);
if (!entryKey) throw new Error('prerender: no entry chunk in the build manifest');

// The base path, from the entry script the client build wrote: `/` or `/monarch/`.
const entrySrc = new RegExp(`src="([^"]*)${manifest[entryKey].file}"`).exec(template);
if (!entrySrc) throw new Error('prerender: entry script not found in dist/index.html');
const base = entrySrc[1];
const siteUrl = new URL(process.env.DOCS_SITE_URL ?? `https://monarch.rookhq.com${base}`);
if (!siteUrl.pathname.endsWith('/')) siteUrl.pathname += '/';

const { render, ROUTES, ROUTE_ALIASES, NOT_FOUND_PATH } = await import(
  path.join(ssrDir, 'entry-server.js')
);

/** Chunks the entry already loads: never preloaded twice. */
const initial = new Set();
(function walk(key) {
  if (initial.has(key)) return;
  initial.add(key);
  for (const k of manifest[key].imports ?? []) walk(k);
})(entryKey);

/** The JS chunks and CSS files a set of modules needs beyond the entry, in load order. */
function assetsOf(keys) {
  const js = [];
  const css = [];
  const seen = new Set();
  const visit = (key) => {
    if (seen.has(key) || initial.has(key)) return;
    seen.add(key);
    const chunk = manifest[key];
    if (!chunk) throw new Error(`prerender: ${key} is not in the build manifest`);
    for (const k of chunk.imports ?? []) visit(k);
    js.push(chunk.file);
    for (const f of chunk.css ?? []) if (!css.includes(f)) css.push(f);
  };
  for (const key of keys) visit(key);
  return { js, css };
}

const escapeHtml = (s) =>
  s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');

/** Public address of a route. */
const urlOf = (route) => new URL(route === '/' ? '' : `${route.slice(1)}/`, siteUrl).toString();

function page(route, rendered, { notFound = false } = {}) {
  const { js, css } = assetsOf(rendered.modules);
  const head = [
    ...css.map((f) => `<link rel="stylesheet" crossorigin href="${base}${f}" data-route-css>`),
    ...js.map((f) => `<link rel="modulepreload" crossorigin href="${base}${f}">`),
    notFound
      ? '<meta name="robots" content="noindex">'
      : `<link rel="canonical" href="${urlOf(route)}">`,
  ].join('\n    ');
  let html = template
    .replace(/<title>[\s\S]*?<\/title>/, `<title>${escapeHtml(rendered.title)}</title>`)
    .replace('</head>', `  ${head}\n  </head>`)
    .replace(
      '<div id="root"></div>',
      `<div id="root" data-route="${escapeHtml(route)}">${rendered.html}</div>`,
    );
  if (rendered.description)
    html = html.replace(
      /(<meta\s+name="description"\s+content=")[^"]*("\s*\/?>)/,
      (_, a, b) => `${a}${escapeHtml(rendered.description)}${b}`,
    );
  // Share cards carry the page's own title and description, and its address.
  const meta = (attr, name, value) => {
    const re = new RegExp(`(<meta\\s+${attr}="${name}"\\s+content=")[^"]*("\\s*\\/?>)`);
    html = html.replace(re, (_, a, b) => `${a}${escapeHtml(value)}${b}`);
  };
  meta('property', 'og:title', rendered.title);
  meta('name', 'twitter:title', rendered.title);
  if (rendered.description) {
    meta('property', 'og:description', rendered.description);
    meta('name', 'twitter:description', rendered.description);
  }
  if (!notFound)
    html = html.replace(
      '</head>',
      `  <meta property="og:url" content="${urlOf(route)}">\n  </head>`,
    );
  if (!html.includes(`data-route="${escapeHtml(route)}"`))
    throw new Error('prerender: #root not found in dist/index.html');
  return html;
}

const write = (route, html) => {
  const file = route === '/' ? 'index.html' : `${route.slice(1)}/index.html`;
  mkdirSync(path.dirname(path.join(dist, file)), { recursive: true });
  writeFileSync(path.join(dist, file), html);
};
const started = Date.now();
const htmlOf = new Map();
for (const route of ROUTES) {
  const html = page(route, await render(route));
  htmlOf.set(route, html);
  write(route, html);
}
// Renamed pages: the old address serves the page itself (canonical: the new
// address), and the client replaces the address before hydrating.
for (const [from, to] of Object.entries(ROUTE_ALIASES)) {
  const html = htmlOf.get(to);
  if (!html) throw new Error(`prerender: alias ${from} points at ${to}, which is not a page`);
  write(from, html);
}
writeFileSync(
  path.join(dist, '404.html'),
  page(NOT_FOUND_PATH, await render(NOT_FOUND_PATH), { notFound: true }),
);

writeFileSync(
  path.join(dist, 'sitemap.xml'),
  `<?xml version="1.0" encoding="UTF-8"?>\n<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">\n${ROUTES.map(
    (r) => `  <url><loc>${urlOf(r)}</loc></url>`,
  ).join('\n')}\n</urlset>\n`,
);
writeFileSync(
  path.join(dist, 'robots.txt'),
  `User-agent: *\nAllow: /\n\nSitemap: ${new URL('sitemap.xml', siteUrl)}\n`,
);
rmSync(ssrDir, { recursive: true, force: true });
console.log(
  `Prerendered ${ROUTES.length} pages, ${Object.keys(ROUTE_ALIASES).length} renamed addresses and 404.html in ${((Date.now() - started) / 1000).toFixed(1)}s (${siteUrl})`,
);
