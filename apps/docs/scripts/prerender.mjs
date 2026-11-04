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
 * and the sitemap (default: https://monarchui.dev + the base path).
 */
import { execSync } from 'node:child_process';
import { mkdirSync, readFileSync, rmSync, writeFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import path from 'node:path';
import { mainToMarkdown } from './markdown.mjs';
import { SITE, jsonLd, llmsTxt, robotsTxt } from './seo.mjs';

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
const siteUrl = new URL(process.env.DOCS_SITE_URL ?? `https://monarchui.dev${base}`);
if (!siteUrl.pathname.endsWith('/')) siteUrl.pathname += '/';

const { render, renderExamplesMarkdown, SITE_DATA, ROUTES, ROUTE_ALIASES, NOT_FOUND_PATH } =
  await import(path.join(ssrDir, 'entry-server.js'));

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

const mdUrlOf = (route) =>
  new URL(route === '/' ? 'index.md' : `${route.slice(1)}/index.md`, siteUrl).toString();
const ogUrl = new URL(SITE.ogImage.path, siteUrl).toString();

/** The date of the commit being built (CI checks out one commit), for lastmod and dateModified. */
const modified = (() => {
  try {
    return execSync('git log -1 --format=%cs', { cwd: here, stdio: ['ignore', 'pipe', 'ignore'] })
      .toString()
      .trim();
  } catch {
    return new Date().toISOString().slice(0, 10);
  }
})();

/**
 * React prerenders a page body behind a Suspense boundary: the HTML holds a
 * loading placeholder, the real content sits in a hidden block at the end,
 * and an inline script moves it into place (after a short delay). Crawlers
 * that do not run scripts would see only the placeholder, and people would
 * wait for the swap. This does what the script does, at build time: the
 * content goes where the placeholder was, in the markup React hydrates.
 */
function inlineBoundaries(html) {
  const boundary = /<!--\$\?--><template id="(B:\d+)"><\/template>[\s\S]*?<!--\/\$-->/g;
  let out = html;
  for (const [whole, id] of [...html.matchAll(boundary)]) {
    const segId = id.replace('B:', 'S:');
    const open = `<div hidden id="${segId}">`;
    const start = out.indexOf(open);
    if (start === -1) throw new Error(`prerender: segment ${segId} not found`);
    let depth = 0;
    let end = -1;
    const tags = /<div[\s>]|<\/div>/g;
    tags.lastIndex = start;
    for (let m = tags.exec(out); m; m = tags.exec(out)) {
      depth += m[0] === '</div>' ? -1 : 1;
      if (depth === 0) {
        end = m.index;
        break;
      }
    }
    if (end === -1) throw new Error(`prerender: segment ${segId} is not closed`);
    const content = out.slice(start + open.length, end);
    out = out.slice(0, start) + out.slice(end + '</div>'.length);
    out = out.replace(whole, () => `<!--$-->${content}<!--/$-->`);
  }
  // React's completion scripts are no longer needed.
  out = out.replace(
    /<script>(?=[^<]*\$R[BCVT]|requestAnimationFrame\(function\(\)\{\$RT)[\s\S]*?<\/script>/g,
    '',
  );
  if (/id="[SB]:\d+"|\$RC\(/.test(out))
    throw new Error('prerender: a Suspense boundary was left in the page');
  return out;
}

const titleOf = new Map();
const crumbsOf = (route) => {
  const parts = route.split('/').filter(Boolean);
  const crumbs = [{ name: 'Monarch', url: urlOf('/') }];
  for (let i = 1; i <= parts.length; i += 1) {
    const r = `/${parts.slice(0, i).join('/')}`;
    const name = (titleOf.get(r) ?? parts[i - 1]).replace(/ · Monarch$/, '');
    crumbs.push({ name, url: urlOf(r) });
  }
  return crumbs;
};

function page(route, rendered, { notFound = false } = {}) {
  const { js, css } = assetsOf(rendered.modules);
  const head = [
    ...css.map((f) => `<link rel="stylesheet" crossorigin href="${base}${f}" data-route-css>`),
    ...js.map((f) => `<link rel="modulepreload" crossorigin href="${base}${f}">`),
    notFound
      ? '<meta name="robots" content="noindex">'
      : `<link rel="canonical" href="${urlOf(route)}">`,
  ].join('\n    ');
  const seo = notFound
    ? ''
    : [
        `<meta name="robots" content="index,follow,max-image-preview:large,max-snippet:-1,max-video-preview:-1">`,
        `<meta name="author" content="${escapeHtml(SITE.maker.name)}">`,
        `<meta property="og:locale" content="en_US">`,
        `<meta property="og:url" content="${urlOf(route)}">`,
        `<meta property="og:image" content="${ogUrl}">`,
        `<meta property="og:image:type" content="image/png">`,
        `<meta property="og:image:width" content="${SITE.ogImage.width}">`,
        `<meta property="og:image:height" content="${SITE.ogImage.height}">`,
        `<meta property="og:image:alt" content="${escapeHtml(SITE.ogImage.alt)}">`,
        `<meta name="twitter:image" content="${ogUrl}">`,
        `<meta name="twitter:image:alt" content="${escapeHtml(SITE.ogImage.alt)}">`,
        `<link rel="alternate" type="text/markdown" href="${mdUrlOf(route)}">`,
        `<link rel="alternate" type="text/plain" href="${new URL('llms.txt', siteUrl)}" title="llms.txt">`,
        jsonLd({
          route,
          title: rendered.title,
          description: rendered.description ?? SITE.description,
          url: urlOf(route),
          siteUrl: urlOf('/'),
          crumbs: crumbsOf(route),
          modified,
        }),
      ].join('\n    ');
  let html = template
    .replace(/<title>[\s\S]*?<\/title>/, `<title>${escapeHtml(rendered.title)}</title>`)
    .replace('</head>', `  ${head}\n    ${seo}\n  </head>`)
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
  html = inlineBoundaries(html);
  if (!html.includes(`data-route="${escapeHtml(route)}"`))
    throw new Error('prerender: #root not found in dist/index.html');
  return html;
}

const started = Date.now();
const htmlOf = new Map();
const renderedOf = new Map();
for (const route of ROUTES) {
  const rendered = await render(route);
  renderedOf.set(route, rendered);
  titleOf.set(route, rendered.title);
}
const write = (route, html, file = 'index.html') => {
  const rel = route === '/' ? file : `${route.slice(1)}/${file}`;
  mkdirSync(path.dirname(path.join(dist, rel)), { recursive: true });
  writeFileSync(path.join(dist, rel), html);
};
const markdownPages = [];
const mdOf = new Map();
for (const route of ROUTES) {
  const rendered = renderedOf.get(route);
  const html = page(route, rendered);
  htmlOf.set(route, html);
  write(route, html);
  // The same page as Markdown, for assistants and crawlers.
  let md = mainToMarkdown(html, urlOf(route));
  const examples = await renderExamplesMarkdown(route);
  if (examples) md += `\n## All example code\n\n${examples}\n`;
  md = `Source: ${urlOf(route)}\n\n${md}`;
  write(route, md, 'index.md');
  markdownPages.push(md);
  mdOf.set(route, md);
}
// Renamed pages: the old address serves the page itself (canonical: the new
// address), and the client replaces the address before hydrating.
for (const [from, to] of Object.entries(ROUTE_ALIASES)) {
  const html = htmlOf.get(to);
  if (!html) throw new Error(`prerender: alias ${from} points at ${to}, which is not a page`);
  write(from, html);
  write(from, mdOf.get(to), 'index.md');
}
writeFileSync(
  path.join(dist, '404.html'),
  page(NOT_FOUND_PATH, await render(NOT_FOUND_PATH), { notFound: true }),
);

writeFileSync(
  path.join(dist, 'sitemap.xml'),
  `<?xml version="1.0" encoding="UTF-8"?>\n<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">\n${ROUTES.map(
    (r) => `  <url><loc>${urlOf(r)}</loc><lastmod>${modified}</lastmod></url>`,
  ).join('\n')}\n</urlset>\n`,
);
writeFileSync(path.join(dist, 'robots.txt'), robotsTxt(siteUrl));
writeFileSync(
  path.join(dist, 'llms.txt'),
  llmsTxt({ siteUrl, mdUrl: (p) => mdUrlOf(p), data: SITE_DATA }),
);
writeFileSync(
  path.join(dist, 'llms-full.txt'),
  `# ${SITE.name}: full documentation\n\n> ${SITE.description}\n\n${markdownPages.join('\n\n---\n\n')}`,
);
rmSync(ssrDir, { recursive: true, force: true });
console.log(
  `Prerendered ${ROUTES.length} pages, ${Object.keys(ROUTE_ALIASES).length} renamed addresses and 404.html in ${((Date.now() - started) / 1000).toFixed(1)}s (${siteUrl})`,
);
