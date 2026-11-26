/**
 * Search, answer-engine and AI-crawler check for the built docs site.
 *
 * Fails when a page is missing what makes it findable and quotable: a title,
 * description, canonical link, share image, valid structured data, content in
 * the HTML itself (not behind a script), or its Markdown copy; or when
 * llms.txt, the sitemap or the share image point at files that do not exist.
 */
import { existsSync, readFileSync, readdirSync, statSync } from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const dist = fileURLToPath(new URL('../../apps/docs/dist/', import.meta.url));
if (!existsSync(path.join(dist, 'index.html'))) {
  console.error('Docs SEO check: apps/docs/dist is missing; run `npm run build:docs` first.');
  process.exit(1);
}
const problems = [];
const fail = (where, what) => problems.push(`${where}: ${what}`);

function pages(dir = dist) {
  const out = [];
  for (const e of readdirSync(dir, { withFileTypes: true })) {
    const full = path.join(dir, e.name);
    if (e.isDirectory() && e.name !== 'assets') out.push(...pages(full));
    else if (e.name === 'index.html') out.push(full);
  }
  return out;
}

const sitemap = readFileSync(path.join(dist, 'sitemap.xml'), 'utf8');
const origin = new URL(/<loc>([^<]+)<\/loc>/.exec(sitemap)[1]).origin;
const fileOf = (url) => path.join(dist, new URL(url).pathname.replace(/\/$/, '/index.html'));

let count = 0;
for (const file of pages()) {
  const html = readFileSync(file, 'utf8');
  const rel = path.relative(dist, file) || 'index.html';
  count += 1;
  const get = (re) => re.exec(html)?.[1];
  if (!get(/<title>([^<]{3,})<\/title>/)) fail(rel, 'no title');
  if (!get(/<meta\s+name="description"\s+content="([^"]{20,})"/)) fail(rel, 'no description');
  if (!get(/<link rel="canonical" href="([^"]+)"/)?.startsWith(origin))
    fail(rel, 'no canonical link');
  const og = get(/<meta\s+property="og:image"\s+content="([^"]+)"/);
  if (!og || !existsSync(fileOf(og))) fail(rel, 'og:image missing or not a file in the build');
  if (!/<meta\s+name="twitter:card"\s+content="summary_large_image"/.test(html))
    fail(rel, 'twitter card is not summary_large_image');
  if (!/<h1[ >]/.test(html)) fail(rel, 'no <h1>');
  if (/<div hidden id="S:|\$RC\(/.test(html))
    fail(rel, 'content is behind a Suspense script, not in the HTML');
  if (!existsSync(file.replace(/index\.html$/, 'index.md'))) fail(rel, 'no Markdown copy');
  const ld = [...html.matchAll(/<script type="application\/ld\+json">([\s\S]*?)<\/script>/g)];
  if (ld.length !== 1) fail(rel, `${ld.length} JSON-LD blocks (expected 1)`);
  for (const m of ld) {
    try {
      const data = JSON.parse(m[1]);
      if (data['@context'] !== 'https://schema.org' || !Array.isArray(data['@graph']))
        fail(rel, 'JSON-LD is not a schema.org graph');
    } catch {
      fail(rel, 'JSON-LD is not valid JSON');
    }
  }
}

for (const m of readFileSync(path.join(dist, 'llms.txt'), 'utf8').matchAll(
  /\]\((https:\/\/[^)\s]+)\)/g,
)) {
  const url = new URL(m[1]);
  if (url.origin !== origin) continue;
  if (!existsSync(path.join(dist, url.pathname)))
    fail('llms.txt', `links to ${url.pathname}, which does not exist`);
}
for (const m of sitemap.matchAll(/<loc>([^<]+)<\/loc>/g))
  if (!existsSync(fileOf(m[1]))) fail('sitemap.xml', `lists ${m[1]}, which does not exist`);
if (!/<lastmod>\d{4}-\d{2}-\d{2}<\/lastmod>/.test(sitemap)) fail('sitemap.xml', 'no lastmod dates');
if (!/Sitemap: /.test(readFileSync(path.join(dist, 'robots.txt'), 'utf8')))
  fail('robots.txt', 'no Sitemap line');
if (statSync(path.join(dist, 'llms-full.txt')).size < 10_000) fail('llms-full.txt', 'looks empty');
const sec = path.join(dist, '.well-known/security.txt');
if (!existsSync(sec)) fail('security.txt', 'missing');
else {
  const text = readFileSync(sec, 'utf8');
  const expires = /^Expires: (.+)$/m.exec(text)?.[1];
  if (!/^Contact: /m.test(text)) fail('security.txt', 'no Contact');
  if (!expires || Date.parse(expires) < Date.now() + 30 * 864e5)
    fail('security.txt', 'expires within 30 days: renew it');
}

if (problems.length) {
  console.error(`Docs SEO check failed (${problems.length}):`);
  for (const p of problems.slice(0, 30)) console.error(`  ${p}`);
  process.exit(1);
}
console.log(
  `Docs SEO check ok: ${count} pages with title, description, canonical, share image, structured data, content in the HTML and a Markdown copy.`,
);
