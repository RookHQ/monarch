/**
 * Concatenates the stylesheets listed in src/styles/order.json, in that
 * declared order, into dist/styles.css, wrapped in the `monarch.components`
 * cascade layer. The layer order is declared first so tokens < base <
 * components regardless of import order, and unlayered app CSS still wins when
 * an app deliberately overrides.
 *
 * The order is explicit and never derived from the file system, so moving or
 * renaming a folder can never reorder the cascade. The build fails when:
 *   - a stylesheet contains a raw colour (components use Monarch variables only);
 *   - a .css file under src is not listed in order.json, or is listed twice,
 *     or a listed file does not exist;
 *   - two stylesheets define the same top-level `.mn-*` block (see ownerBlocks);
 *   - two stylesheets define the same @keyframes name.
 *
 * See docs/architecture/SOURCE-LAYOUT.md#css-order.
 */
import { existsSync, mkdirSync, readFileSync, readdirSync, writeFileSync } from 'node:fs';
import { dirname, join, relative, sep } from 'node:path';
import { fileURLToPath } from 'node:url';

const here = dirname(fileURLToPath(import.meta.url));
const srcDir = join(here, '../src');
const orderFile = join(srcDir, 'styles/order.json');
const out = join(here, '../dist/styles.css');

/** Global foundation: shared resets and keyframes. Exempt from the block-ownership check. */
const FOUNDATION = 'styles/foundation.css';

const RAW_COLOR = /#[0-9a-f]{3,8}\b|\brgba?\(|\bhsla?\((?!var\(--mn-avatar-hue)/i;

const errors: string[] = [];
const toPosix = (p: string) => p.split(sep).join('/');

function cssFilesUnder(dir: string): string[] {
  const found: string[] = [];
  for (const entry of readdirSync(dir, { withFileTypes: true })) {
    const abs = join(dir, entry.name);
    if (entry.isDirectory()) found.push(...cssFilesUnder(abs));
    else if (entry.name.endsWith('.css')) found.push(toPosix(relative(srcDir, abs)));
  }
  return found;
}

/** Removes comments so selectors and at-rules can be read reliably. */
const stripComments = (css: string) => css.replace(/\/\*[\s\S]*?\*\//g, '');

/**
 * The `.mn-*` blocks a stylesheet defines: for each selector, the first
 * `.mn-` class it names, reduced to its BEM block (`.mn-dialog__close` and
 * `.mn-choice--switch` belong to `mn-dialog` and `mn-choice`). Context such as
 * `:is(.dark, [data-mn-theme='dark']) .mn-avatar` or `[data-variant] > .mn-tabs__x`
 * is skipped because it names no `.mn-` class before the subject.
 */
function ownerBlocks(css: string): Set<string> {
  const blocks = new Set<string>();
  const text = stripComments(css);
  for (const m of text.matchAll(/([^{};]+)\{/g)) {
    const prelude = (m[1] ?? '').trim();
    if (prelude.startsWith('@') || /^(from|to|\d+%)/.test(prelude)) continue;
    for (const selector of prelude.split(',')) {
      const cls = /\.(mn-[a-z0-9-]+?)(?:__|--|[^a-z0-9-]|$)/.exec(selector.trim());
      if (cls?.[1]) blocks.add(cls[1]);
    }
  }
  return blocks;
}

const keyframesOf = (css: string) =>
  [...stripComments(css).matchAll(/@keyframes\s+([a-z0-9-]+)/gi)].map((m) => m[1] as string);

const { order } = JSON.parse(readFileSync(orderFile, 'utf8')) as { order: string[] };

// Every stylesheet is declared exactly once, and every declaration exists.
const declared = new Set<string>();
for (const p of order) {
  if (declared.has(p)) errors.push(`styles/order.json lists ${p} twice`);
  declared.add(p);
  if (!existsSync(join(srcDir, p)))
    errors.push(`styles/order.json lists ${p}, which does not exist`);
}
for (const f of cssFilesUnder(srcDir))
  if (!declared.has(f))
    errors.push(
      `${f} is not in src/styles/order.json, so it would never ship: add it in cascade order`,
    );

const blockOwner = new Map<string, string>();
const keyframeOwner = new Map<string, string>();
const chunks: string[] = [];
const chunkByPath = new Map<string, string>();
for (const p of order) {
  const abs = join(srcDir, p);
  if (!existsSync(abs)) continue;
  const css = readFileSync(abs, 'utf8');
  css.split('\n').forEach((line, i) => {
    if (RAW_COLOR.test(line))
      errors.push(`${p}:${i + 1}: raw colour in component CSS: ${line.trim()}`);
  });
  if (p !== FOUNDATION)
    for (const block of ownerBlocks(css)) {
      const prev = blockOwner.get(block);
      if (prev && prev !== p) errors.push(`.${block} is defined by both ${prev} and ${p}`);
      else blockOwner.set(block, p);
    }
  // Theme coverage (docs/guides/theming.md): every stylesheet handles forced
  // colours, and every animated one handles reduced motion.
  if (!css.includes('forced-colors'))
    errors.push(
      `${p}: no @media (forced-colors: active) block (a comment saying why none is needed also counts)`,
    );
  if (
    /\b(animation|transition)\s*:/.test(stripComments(css)) &&
    !css.includes('prefers-reduced-motion')
  )
    errors.push(`${p}: animates but has no @media (prefers-reduced-motion: reduce) block`);
  for (const name of keyframesOf(css)) {
    const prev = keyframeOwner.get(name);
    if (prev) errors.push(`@keyframes ${name} is defined by both ${prev} and ${p}`);
    else keyframeOwner.set(name, p);
  }
  chunks.push(`/* ${p} */\n${css.trim()}`);
  chunkByPath.set(p, `/* ${p} */\n${css.trim()}`);
}

if (errors.length) {
  for (const e of errors) console.error(`error: ${e}`);
  process.exit(1);
}

const sheet = (parts: string[], title: string) =>
  `/*! ${title}. Generated by @rookcom/monarch-react. Do not edit. */\n@layer monarch.tokens, monarch.base, monarch.components;\n\n@layer monarch.components {\n${parts
    .join('\n\n')
    .replace(/^(?=.)/gm, '  ')}\n}\n`;
mkdirSync(dirname(out), { recursive: true });
writeFileSync(out, sheet(chunks, 'Monarch components'));

// ---------------------------------------------------------------- per-entry sheets
//
// dist/styles/core.css holds the shared styles and every component of the root
// entry; dist/styles/<entry>.css holds a subpath entry's components plus any
// component from another subpath they compose. An app loads core.css and the
// sheets of the entries it imports, instead of the whole styles.css.

const entryFiles: Record<string, string> = { core: 'index.ts' };
for (const f of readdirSync(join(srcDir, 'entries')))
  if (f.endsWith('.ts')) entryFiles[f.slice(0, -3)] = `entries/${f}`;

const INDEX_IMPORT = /from '((?:\.\.?\/)+[^']*?)\/index\.js'/g;
const folderOf = (fromFile: string, spec: string) =>
  toPosix(relative(srcDir, join(srcDir, dirname(fromFile), spec)));

/** Component folder → the entry that exports it. */
const entryOfFolder = new Map<string, string>();
for (const [name, file] of Object.entries(entryFiles))
  for (const m of readFileSync(join(srcDir, file), 'utf8').matchAll(INDEX_IMPORT)) {
    const folder = folderOf(file, m[1] ?? '');
    if (!entryOfFolder.has(folder) || name === 'core') entryOfFolder.set(folder, name);
  }

/** Component folders a folder's runtime source imports, through their index files. */
function importsOf(folder: string): string[] {
  const dir = join(srcDir, folder);
  if (!existsSync(dir)) return [];
  const out: string[] = [];
  for (const f of readdirSync(dir))
    if (/\.tsx?$/.test(f) && !/\.(test|docs)\.tsx?$/.test(f))
      for (const m of readFileSync(join(dir, f), 'utf8').matchAll(INDEX_IMPORT))
        out.push(folderOf(`${folder}/${f}`, m[1] ?? ''));
  return out;
}

const folderOfCss = (p: string) => (p.startsWith('styles/') ? null : dirname(p));
const covered = new Set<string>();
const stylesDir = join(dirname(out), 'styles');
mkdirSync(stylesDir, { recursive: true });
for (const name of Object.keys(entryFiles)) {
  const wanted = new Set<string>();
  const visit = (folder: string) => {
    if (wanted.has(folder)) return;
    const owner = entryOfFolder.get(folder) ?? 'core';
    if (name !== 'core' && owner === 'core') return; // already in core.css
    if (name === 'core' && owner !== 'core') return;
    wanted.add(folder);
    importsOf(folder).forEach(visit);
  };
  for (const [folder, owner] of entryOfFolder) if (owner === name) visit(folder);
  const parts = order
    .filter((p) => {
      const folder = folderOfCss(p);
      return folder === null ? name === 'core' : wanted.has(folder);
    })
    .map((p) => (covered.add(p), chunkByPath.get(p)))
    .filter((c): c is string => c !== undefined);
  writeFileSync(join(stylesDir, `${name}.css`), sheet(parts, `Monarch ${name} styles`));
}
console.log(
  `styles.css: ${order.length} sources, ${(Buffer.byteLength(readFileSync(out)) / 1024).toFixed(1)} KiB`,
);
const missing = order.filter((p) => !covered.has(p));
if (missing.length) {
  for (const p of missing)
    console.error(`error: ${p} is in no per-entry stylesheet; export its component from an entry`);
  process.exit(1);
}
console.log(`styles/: ${Object.keys(entryFiles).length} per-entry sheets`);

// ---------------------------------------------------------------- per-component sheets
//
// dist/styles/components/<id>.css holds one component folder's CSS plus that of
// every non-core folder it composes, so a page that uses three AI components
// loads three small sheets instead of styles/ai.css. core.css is still required.

const componentsDir = join(stylesDir, 'components');
mkdirSync(componentsDir, { recursive: true });
let componentSheets = 0;
for (const [folder, owner] of entryOfFolder) {
  if (owner === 'core') continue;
  const wanted = new Set<string>();
  const visit = (f: string) => {
    if (wanted.has(f) || (entryOfFolder.get(f) ?? 'core') === 'core') return;
    wanted.add(f);
    importsOf(f).forEach(visit);
  };
  visit(folder);
  const parts = order
    .filter((p) => {
      const f = folderOfCss(p);
      return f !== null && wanted.has(f);
    })
    .map((p) => chunkByPath.get(p))
    .filter((c): c is string => c !== undefined);
  const id = folder.split('/').pop() ?? folder;
  writeFileSync(join(componentsDir, `${id}.css`), sheet(parts, `Monarch ${id} styles`));
  componentSheets += 1;
}
console.log(`styles/components/: ${componentSheets} per-component sheets (non-core entries)`);
