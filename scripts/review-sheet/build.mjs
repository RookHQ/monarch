#!/usr/bin/env node
// Builds the side-by-side review sheet: one offline HTML page with, for each primitive of
// consolidation/visual-spec/primitives.md, the web image, the Android image and the iOS image (light and dark),
// the primitive id, its status and a notes cell. Needs no dependencies and no network.
//
//   node scripts/review-sheet/build.mjs [--out <file>] [--embed] [--root <repo>] [--previews <dir>]
//
// Default output: platforms/review-sheet/index.html (build output, git-ignored). Images are linked by relative
// path; --embed copies them into the page as data URIs for a single-file copy.
//
// Where the images come from, per cell:
//   web      apps/docs/tests/__screenshots__/visual/gallery/<web id>/<theme>-default-390-linux.png (1280 fallback)
//   native   1. curated:  platforms/review-previews/<android|ios>/<primitive id>.<light|dark>.png
//               (written by the Android preview pipeline and the iOS preview host; one image per primitive)
//            2. current look, before restyle: the stacked preview of the nearest component today
//               (android: platforms/android/previews/<id>.<theme>.png; ios: platforms/ios/previews/ or
//               platforms/ios/Tests/Snapshots/**/<id>.<theme>.png). Marked with a banner on the cell.
//            3. placeholder.
import { existsSync, mkdirSync, readFileSync, writeFileSync } from 'node:fs';
import { dirname, join, relative, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const here = dirname(fileURLToPath(import.meta.url));
export const defaultRoot = resolve(here, '..', '..');
export const THEMES = ['light', 'dark'];
export const PLATFORMS = ['web', 'android', 'ios'];
export const PREVIEW_DIR = 'platforms/review-previews';
export const GALLERY_DIR = 'apps/docs/tests/__screenshots__/visual/gallery';

export function loadPrimitives() {
  return JSON.parse(readFileSync(join(here, 'primitives.json'), 'utf8')).primitives;
}

export function loadStatus() {
  return JSON.parse(readFileSync(join(here, 'status.json'), 'utf8')).rows ?? {};
}

const first = (root, candidates) =>
  candidates.find((candidate) => existsSync(join(root, candidate))) ?? null;

function findSnapshot(root, id, theme) {
  const base = join(root, 'platforms/ios/Tests/Snapshots');
  if (!existsSync(base)) return null;
  for (const dir of [id, '']) {
    const folder = join(base, dir);
    if (!existsSync(folder)) continue;
    for (const name of [`${id}.${theme}.png`]) {
      if (existsSync(join(folder, name))) return relative(root, join(folder, name));
    }
  }
  return null;
}

/** Resolve one image. Returns { path (repo relative) | null, kind: 'web'|'curated'|'current'|'missing', note }. */
export function resolveImage(root, primitive, platform, theme, previews = PREVIEW_DIR) {
  if (platform === 'web') {
    if (!primitive.web)
      return { path: null, kind: 'missing', note: 'no web gallery image for this primitive' };
    const dir = `${GALLERY_DIR}/${primitive.web}`;
    const path = first(root, [
      `${dir}/${theme}-default-390-linux.png`,
      `${dir}/${theme}-default-1280-linux.png`,
    ]);
    return path
      ? { path, kind: 'web', note: path.includes('-1280-') ? 'web 1280 (no 390 image)' : '' }
      : { path: null, kind: 'missing', note: `no web image in gallery/${primitive.web}` };
  }
  const curated = first(root, [`${previews}/${platform}/${primitive.id}.${theme}.png`]);
  if (curated) return { path: curated, kind: 'curated', note: '' };
  const component = primitive[platform];
  if (component) {
    const current =
      platform === 'android'
        ? first(root, [`platforms/android/previews/${component}.${theme}.png`])
        : (first(root, [`platforms/ios/previews/${component}.${theme}.png`]) ??
          findSnapshot(root, component, theme));
    if (current)
      return {
        path: current,
        kind: 'current',
        note: `current look (before restyle): ${component}`,
      };
  }
  return { path: null, kind: 'missing', note: `no ${platform} image yet` };
}

const esc = (text) =>
  String(text).replace(
    /[&<>"]/g,
    (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' })[c],
  );

function dataUri(root, path) {
  return `data:image/png;base64,${readFileSync(join(root, path)).toString('base64')}`;
}

export function collect(root, previews = PREVIEW_DIR) {
  const status = loadStatus();
  return loadPrimitives().map((primitive) => ({
    ...primitive,
    status: status[primitive.id]?.status ?? 'baseline',
    notes: status[primitive.id]?.notes ?? '',
    images: Object.fromEntries(
      PLATFORMS.map((platform) => [
        platform,
        Object.fromEntries(
          THEMES.map((theme) => [theme, resolveImage(root, primitive, platform, theme, previews)]),
        ),
      ]),
    ),
  }));
}

export function summarize(rows) {
  const count = { rows: rows.length, curated: 0, current: 0, missing: 0 };
  for (const row of rows)
    for (const platform of ['android', 'ios'])
      for (const theme of THEMES)
        count[
          row.images[platform][theme].kind === 'web' ? 'missing' : row.images[platform][theme].kind
        ]++;
  return count;
}

function cell(root, outDir, image, embed, label) {
  if (!image.path) {
    return `<div class="img missing" role="img" aria-label="${esc(label)}: ${esc(image.note)}"><span>${esc(image.note)}</span></div>`;
  }
  const src = embed
    ? dataUri(root, image.path)
    : relative(outDir, join(root, image.path)).split('\\').join('/');
  const banner = image.kind === 'current' ? `<span class="banner">${esc(image.note)}</span>` : '';
  return `<div class="img ${image.kind}">${banner}<a href="${esc(src)}" target="_blank" rel="noopener"><img loading="lazy" src="${esc(src)}" alt="${esc(label)}"></a></div>`;
}

export function buildHtml({ root = defaultRoot, out, embed = false, previews = PREVIEW_DIR } = {}) {
  const rows = collect(root, previews);
  const outDir = dirname(out ?? join(root, 'platforms/review-sheet/index.html'));
  const sum = summarize(rows);
  const before = sum.current > 0;
  const groups = [...new Set(rows.map((row) => row.group))];
  const body = groups
    .map((group) => {
      const trs = rows
        .filter((row) => row.group === group)
        .map((row) => {
          const cols = PLATFORMS.map(
            (platform) =>
              `<td class="plat plat-${platform}">${THEMES.map(
                (theme) =>
                  `<figure><figcaption>${theme}</figcaption>${cell(root, outDir, row.images[platform][theme], embed, `${row.id} ${platform} ${theme}`)}</figure>`,
              ).join('')}</td>`,
          ).join('');
          return `<tr id="${esc(row.id)}" data-status="${esc(row.status)}"><th scope="row"><a href="#${esc(row.id)}">${esc(row.id)}</a><span class="status s-${esc(row.status)}">${esc(row.status)}</span></th>${cols}<td class="notes" contenteditable="true" data-note="${esc(row.id)}">${esc(row.notes)}</td></tr>`;
        })
        .join('\n');
      return `<tbody><tr class="group"><td colspan="5">${esc(group)}</td></tr>\n${trs}</tbody>`;
    })
    .join('\n');
  return `<!doctype html>
<html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<title>Monarch review sheet</title>
<style>
:root{--bg:#fff;--fg:#1a1a1f;--muted:#6b6b76;--line:#dcdce2;--warn:#fff4d6;--warnfg:#7a4b00;--ph:#f3f3f6}
@media (prefers-color-scheme:dark){:root{--bg:#141418;--fg:#ececf1;--muted:#9a9aa6;--line:#2e2e36;--warn:#3a2d0a;--warnfg:#ffd479;--ph:#1c1c22}}
body{margin:0;background:var(--bg);color:var(--fg);font:14px/1.4 system-ui,sans-serif}
header{position:sticky;top:0;z-index:2;background:var(--bg);border-bottom:1px solid var(--line);padding:10px 16px}
h1{font-size:16px;margin:0 0 4px}.sum{color:var(--muted);font-size:12px}
.top-banner{margin:8px 0 0;padding:8px 12px;background:var(--warn);color:var(--warnfg);border-radius:6px;font-weight:600}
table{border-collapse:collapse;width:100%;min-width:1200px}
th,td{border-bottom:1px solid var(--line);padding:8px;vertical-align:top;text-align:left}
thead th{position:sticky;top:0;background:var(--bg)}
tr.group td{background:var(--ph);font-weight:600;text-transform:uppercase;font-size:11px;letter-spacing:.04em}
th[scope=row]{width:130px;font-weight:600}th[scope=row] a{color:inherit;text-decoration:none}
.status{display:block;margin-top:4px;font-size:11px;font-weight:500;color:var(--muted)}
.s-done{color:#1a7f37}.s-review{color:#0969da}.s-in-progress{color:#9a6700}
.plat{display:grid;grid-template-columns:1fr 1fr;gap:8px;min-width:300px}
figure{margin:0}figcaption{font-size:11px;color:var(--muted);margin-bottom:2px}
.img img{display:block;max-width:100%;height:auto;border:1px solid var(--line);border-radius:4px}
.img.missing{display:flex;align-items:center;justify-content:center;min-height:72px;border:1px dashed var(--line);border-radius:4px;background:var(--ph);color:var(--muted);font-size:11px;text-align:center;padding:6px}
.banner{display:block;font-size:10px;background:var(--warn);color:var(--warnfg);padding:1px 4px;border-radius:3px 3px 0 0}
.notes{min-width:160px;font-size:12px;outline:none}.notes:focus{background:var(--ph)}
</style></head><body>
<header><h1>Monarch review sheet: web | Android | iOS, light and dark</h1>
<div class="sum">${sum.rows} primitives. Native images: ${sum.curated} curated, ${sum.current} current look, ${sum.missing} missing.</div>
${before ? '<div class="top-banner">Current look (before restyle): cells marked with a banner show today&rsquo;s stacked previews, not the restyled primitive.</div>' : ''}
</header>
<table><thead><tr><th>Primitive</th><th>Web</th><th>Android</th><th>iOS</th><th>Notes (kept in this browser)</th></tr></thead>
${body}
</table>
<script>
(function(){var k='monarch-review-notes';var s={};try{s=JSON.parse(localStorage.getItem(k)||'{}')}catch(e){}
document.querySelectorAll('[data-note]').forEach(function(n){var id=n.dataset.note;if(s[id])n.textContent=s[id];
n.addEventListener('input',function(){s[id]=n.textContent;try{localStorage.setItem(k,JSON.stringify(s))}catch(e){}})})})();
</script>
</body></html>
`;
}

function main(argv) {
  const opt = (name) => {
    const at = argv.indexOf(`--${name}`);
    return at >= 0 ? argv[at + 1] : undefined;
  };
  const root = opt('root') ? resolve(opt('root')) : defaultRoot;
  const embed = argv.includes('--embed');
  const out = resolve(
    root,
    opt('out') ??
      (embed
        ? 'platforms/review-sheet/review-sheet.single.html'
        : 'platforms/review-sheet/index.html'),
  );
  const html = buildHtml({ root, out, embed, previews: opt('previews') ?? PREVIEW_DIR });
  mkdirSync(dirname(out), { recursive: true });
  writeFileSync(out, html);
  console.log(`review sheet: ${out}`);
}

if (process.argv[1] && resolve(process.argv[1]) === fileURLToPath(import.meta.url))
  main(process.argv.slice(2));
