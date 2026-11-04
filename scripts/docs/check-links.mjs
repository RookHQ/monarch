/**
 * Internal link check for the built docs site (docs/guides/docs-site.md).
 *
 * Reads every prerendered page in apps/docs/dist and checks each link and
 * asset reference that stays on the site: the target page or file must exist,
 * and a `#fragment` must name an element on the target page. Screenshot routes
 * (`#/__gallery…`, `#/__marketing`) are client-only and only checked by name.
 * Links inside live example previews are the example's own demo data (`#home`,
 * `/w/acme/inbox`) and are skipped. Links to other sites are not fetched.
 */
import { existsSync, readFileSync, readdirSync } from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const dist = fileURLToPath(new URL('../../apps/docs/dist/', import.meta.url));
if (!existsSync(path.join(dist, 'index.html'))) {
  console.error('Docs link check: apps/docs/dist is missing; run `npm run build:docs` first.');
  process.exit(1);
}
const TEST_ROUTE = /^#\/__(gallery(\/[a-z0-9-]+)?|marketing)$/;

function htmlFiles(dir = dist) {
  const out = [];
  for (const entry of readdirSync(dir, { withFileTypes: true })) {
    const full = path.join(dir, entry.name);
    if (entry.isDirectory() && entry.name !== 'assets' && !entry.name.startsWith('.'))
      out.push(...htmlFiles(full));
    else if (entry.name.endsWith('.html')) out.push(full);
  }
  return out;
}

/** Removes every live example preview (`<div class="docs-example__preview…">…</div>`). */
function withoutPreviews(html) {
  let out = '';
  let at = 0;
  const open = /<div class="docs-example__preview[^"]*"/g;
  for (let m = open.exec(html); m; m = open.exec(html)) {
    out += html.slice(at, m.index);
    const tags = /<div[\s>]|<\/div>/g;
    tags.lastIndex = m.index;
    let depth = 0;
    let end = html.length;
    for (let t = tags.exec(html); t; t = tags.exec(html)) {
      depth += t[0] === '</div>' ? -1 : 1;
      if (depth === 0) {
        end = tags.lastIndex;
        break;
      }
    }
    at = end;
    open.lastIndex = end;
  }
  return out + html.slice(at);
}

const files = htmlFiles();
const read = new Map(files.map((f) => [f, readFileSync(f, 'utf8')]));
// The base path, from the entry script: `/` or `/monarch/`.
const base =
  /<script type="module"[^>]*src="([^"]*?)assets\//.exec(
    read.get(path.join(dist, 'index.html')),
  )?.[1] ?? '/';
const origin = 'http://site.invalid';
const idsOf = new Map();
const ids = (file) => {
  let set = idsOf.get(file);
  if (!set) {
    set = new Set([...read.get(file).matchAll(/\sid="([^"]+)"/g)].map((m) => m[1]));
    idsOf.set(file, set);
  }
  return set;
};

/** The dist file a same-site URL path resolves to, or null. */
function target(pathname) {
  if (!pathname.startsWith(base)) return null;
  const rel = decodeURIComponent(pathname.slice(base.length));
  const file = path.join(dist, rel.endsWith('/') || rel === '' ? `${rel}index.html` : rel);
  return existsSync(file) ? file : null;
}

const errors = [];
let checked = 0;
for (const [file, html] of read) {
  const rel = path.relative(dist, file).split(path.sep).join('/');
  const pageUrl = new URL(base + rel.replace(/(^|\/)index\.html$/, '$1'), origin);
  const refs = [...withoutPreviews(html).matchAll(/\s(?:href|src)="([^"]+)"/g)].map((m) =>
    m[1].replace(/&amp;/g, '&'),
  );
  for (const ref of new Set(refs)) {
    if (TEST_ROUTE.test(new URL(ref, pageUrl).hash) && new URL(ref, pageUrl).pathname === base) {
      checked++;
      continue;
    }
    const url = new URL(ref, pageUrl);
    if (url.origin !== origin) continue;
    checked++;
    const dest = target(url.pathname);
    if (!dest) {
      errors.push(`${rel}: ${ref} → no such page or file`);
      continue;
    }
    const fragment = decodeURIComponent(url.hash.slice(1));
    if (fragment && dest.endsWith('.html') && !ids(dest).has(fragment))
      errors.push(`${rel}: ${ref} → no element with id "${fragment}" on that page`);
  }
}

if (errors.length) {
  console.error(
    `Docs link check: ${errors.length} broken of ${checked} internal links:\n- ${errors.join('\n- ')}`,
  );
  process.exit(1);
}
console.log(`Docs link check ok: ${checked} internal links and assets in ${files.length} pages`);
