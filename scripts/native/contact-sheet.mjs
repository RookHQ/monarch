#!/usr/bin/env node
// Builds one offline HTML page that shows every screenshot baseline of a native platform, by family, with its
// component id and theme. Open it in a browser to review the images before a pull request.
//
//   node scripts/native/contact-sheet.mjs --platform android [--out <file>] [--embed]
//
// The images are linked by relative path, so the page works offline next to the repository. --embed copies them
// into the page as data URIs (one self-contained file, much larger).
//
// Android baselines: platforms/android/previews/<id>.<light|dark>.png (the default images) and
//   platforms/android/monarch-compose/src/test/screenshots/<id>/<id>.<theme>[.x2][.rtl].png (the core variants).
// iOS baselines: platforms/ios/previews/<id>.<light|dark>.png and platforms/ios/Tests/Snapshots/**/<id>.<theme>...png.
import { existsSync, mkdirSync, readdirSync, readFileSync, statSync, writeFileSync } from 'node:fs';
import { dirname, join, relative, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..', '..');
const args = process.argv.slice(2);
const option = (name) => {
  const at = args.indexOf(`--${name}`);
  return at >= 0 ? args[at + 1] : undefined;
};
const platform = option('platform');
const embed = args.includes('--embed');
const layouts = {
  android: {
    previews: 'platforms/android/previews',
    variants: 'platforms/android/monarch-compose/src/test/screenshots',
    inventory: 'platforms/android/inventory',
    out: 'platforms/android/build/contact-sheet.html',
  },
  ios: {
    previews: 'platforms/ios/previews',
    variants: 'platforms/ios/Tests/Snapshots',
    inventory: 'platforms/ios/inventory',
    out: 'platforms/ios/.build/contact-sheet.html',
  },
};
if (!layouts[platform]) {
  console.error(
    'usage: node scripts/native/contact-sheet.mjs --platform android|ios [--out file] [--embed]',
  );
  process.exit(2);
}
const layout = layouts[platform];
const out = resolve(root, option('out') ?? layout.out);

const walk = (dir) =>
  !existsSync(dir)
    ? []
    : readdirSync(dir).flatMap((name) => {
        const path = join(dir, name);
        return statSync(path).isDirectory() ? walk(path) : name.endsWith('.png') ? [path] : [];
      });

// <id>.<theme>[.x2][.rtl].png; the theme is light, dark, hc-light or hc-dark.
const parse = (file) => {
  const match = /^(.+?)\.(light|dark|hc-light|hc-dark)((?:\.x2)?(?:\.rtl)?)\.png$/.exec(
    file.split('/').pop(),
  );
  return match
    ? { id: match[1], theme: match[2], mode: match[3].replace(/^\./, '') || 'default' }
    : null;
};

const components = new Map();
for (const file of [...walk(join(root, layout.previews)), ...walk(join(root, layout.variants))]) {
  const parsed = parse(file);
  if (!parsed) continue;
  if (!components.has(parsed.id)) components.set(parsed.id, []);
  components.get(parsed.id).push({ ...parsed, file });
}

const family = (id) => {
  const path = join(root, layout.inventory, `${id}.json`);
  try {
    return JSON.parse(readFileSync(path, 'utf8')).family ?? 'other';
  } catch {
    return 'other';
  }
};
const families = new Map();
for (const id of [...components.keys()].sort()) {
  const name = family(id);
  if (!families.has(name)) families.set(name, []);
  families.get(name).push(id);
}

const escape = (text) =>
  text.replace(/[&<>"]/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' })[c]);
const source = (file) =>
  embed
    ? `data:image/png;base64,${readFileSync(file).toString('base64')}`
    : relative(dirname(out), file).split('\\').join('/');
const figure = (image) =>
  `<figure><img loading="lazy" src="${source(image.file)}" alt="${escape(image.id)} ${image.theme} ${image.mode}">` +
  `<figcaption>${escape(image.id)} · ${image.theme}${image.mode === 'default' ? '' : ` · ${image.mode}`}</figcaption></figure>`;

const order = ['light', 'dark', 'hc-light', 'hc-dark'];
const byTheme = (a, b) =>
  order.indexOf(a.theme) - order.indexOf(b.theme) || a.mode.localeCompare(b.mode);
let count = 0;
const sections = [...families.entries()]
  .sort(([a], [b]) => a.localeCompare(b))
  .map(([name, ids]) => {
    const cards = ids
      .map((id) => {
        const images = components.get(id).sort(byTheme);
        count += images.length;
        const main = images.filter(
          (i) => i.mode === 'default' && (i.theme === 'light' || i.theme === 'dark'),
        );
        const more = images.filter((i) => !main.includes(i));
        return (
          `<article id="${escape(id)}" data-id="${escape(id)}"><h3>${escape(id)}</h3><div class="pair">${main.map(figure).join('')}</div>` +
          (more.length
            ? `<details><summary>${more.length} more: themes, large text, right to left</summary><div class="more">${more.map(figure).join('')}</div></details>`
            : '') +
          '</article>'
        );
      })
      .join('\n');
    return `<section><h2>${escape(name)} <small>${ids.length}</small></h2><div class="grid">${cards}</div></section>`;
  })
  .join('\n');

const html = `<!doctype html>
<html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<title>Monarch ${platform} contact sheet</title>
<style>
  :root { color-scheme: light dark; font-family: system-ui, sans-serif; }
  body { margin: 0; padding: 0 16px 48px; background: Canvas; color: CanvasText; }
  header { position: sticky; top: 0; background: Canvas; padding: 12px 0; border-bottom: 1px solid #8884; z-index: 1; }
  input { font: inherit; padding: 6px 10px; width: 260px; }
  h2 { margin: 28px 0 8px; } h2 small { font-weight: 400; opacity: .6; }
  .grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(680px, 1fr)); gap: 16px; }
  article { border: 1px solid #8886; border-radius: 6px; padding: 8px; }
  h3 { margin: 0 0 6px; font: 600 14px ui-monospace, monospace; }
  .pair, .more { display: flex; gap: 8px; align-items: flex-start; flex-wrap: wrap; }
  figure { margin: 0; } img { display: block; width: 320px; max-width: 100%; border: 1px solid #8884; }
  figcaption { font: 11px ui-monospace, monospace; opacity: .7; padding-top: 2px; }
  summary { cursor: pointer; margin: 8px 0; font-size: 13px; }
</style></head><body>
<header><strong>Monarch ${platform}: ${components.size} components, ${count} images</strong>
 <input id="filter" type="search" placeholder="Filter by id" aria-label="Filter by component id"></header>
${sections}
<script>
  document.getElementById('filter').addEventListener('input', (event) => {
    const text = event.target.value.trim().toLowerCase();
    document.querySelectorAll('article').forEach((card) => { card.hidden = text && !card.dataset.id.includes(text); });
  });
</script></body></html>
`;
mkdirSync(dirname(out), { recursive: true });
writeFileSync(out, html);
console.log(`${out}\n${components.size} components, ${count} images`);
