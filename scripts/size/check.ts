// Bundle-size budgets of @rookcom/monarch-react.
//
//   npm run size:check              fail when an entry or component is over its budget
//   npm run size:check -- --update  rewrite registry/size-budgets.json from this build
//
// Measures every public entry and every component folder of packages/react/dist:
// bundled on its own, minified by esbuild, package imports left external, then
// gzipped. esbuild is already installed (tsx and Vite depend on it); if it cannot
// be loaded, the script gzips the unminified module graph instead and says so,
// and refuses to compare against budgets recorded the other way.
//
// Needs a build first (`npm run build`). See docs/governance/performance-budgets.md.
import { existsSync, readFileSync, writeFileSync } from 'node:fs';
import { dirname, join, relative } from 'node:path';
import { fileURLToPath } from 'node:url';
import { gzipSync } from 'node:zlib';
import { format, resolveConfig } from 'prettier';
import { ENTRY_FILES, listFolders } from '../../packages/react/scripts/component-folders.js';
import {
  compareBudgets,
  formatComparison,
  kib,
  largest,
  passes,
  updateBudgets,
  type Measured,
  type SizeBudgets,
  type SizeMethod,
} from '../../packages/react/scripts/size-budget.js';

const ROOT = fileURLToPath(new URL('../..', import.meta.url));
const DIST = join(ROOT, 'packages/react/dist');
const OUT = join(ROOT, 'registry/size-budgets.json');

const gzip = (code: string | Uint8Array): number => gzipSync(code, { level: 9 }).length;

/** Name → built module: package entries and component folders. */
function targets(): { entries: Record<string, string>; components: Record<string, string> } {
  const entries: Record<string, string> = {};
  for (const [entry, file] of Object.entries(ENTRY_FILES))
    entries[entry] = join(DIST, file.replace(/\.ts$/, '.js'));
  const components: Record<string, string> = {};
  for (const f of listFolders()) {
    if (!f.files.includes('index.ts')) continue;
    components[f.id] = join(DIST, f.rel, 'index.js');
  }
  const missing = [...Object.values(entries), ...Object.values(components)].filter(
    (p) => !existsSync(p),
  );
  if (missing.length)
    throw new Error(
      `${missing.length} built module(s) missing, such as ${relative(ROOT, missing[0] ?? '')}: run \`npm run build\` first`,
    );
  return { entries, components };
}

interface Esbuild {
  build(options: Record<string, unknown>): Promise<{
    outputFiles: { path: string; contents: Uint8Array }[];
  }>;
}

async function loadEsbuild(): Promise<Esbuild | null> {
  try {
    return (await import('esbuild')) as unknown as Esbuild;
  } catch {
    return null;
  }
}

/** Each module bundled alone (no shared chunks), minified, packages external. */
async function withEsbuild(esbuild: Esbuild, files: Record<string, string>) {
  const names = Object.keys(files);
  const result = await esbuild.build({
    entryPoints: names.map((name, i) => ({ in: files[name], out: `m${i}` })),
    outdir: join(ROOT, 'node_modules/.cache/monarch-size'),
    bundle: true,
    write: false,
    minify: true,
    format: 'esm',
    platform: 'browser',
    target: 'es2022',
    packages: 'external',
    legalComments: 'none',
    logLevel: 'silent',
  });
  const sizes: Record<string, number> = {};
  for (const file of result.outputFiles) {
    const index = /m(\d+)\.js$/.exec(file.path)?.[1];
    const name = index === undefined ? undefined : names[Number(index)];
    if (name !== undefined) sizes[name] = gzip(file.contents);
  }
  return sizes;
}

/** Without esbuild: the module and every relative import it reaches, unminified, gzipped. */
function withGzipOnly(files: Record<string, string>) {
  const sizes: Record<string, number> = {};
  for (const [name, entry] of Object.entries(files)) {
    const seen = new Set<string>();
    const parts: string[] = [];
    const visit = (file: string) => {
      if (seen.has(file) || !existsSync(file)) return;
      seen.add(file);
      const code = readFileSync(file, 'utf8');
      parts.push(code);
      for (const m of code.matchAll(/(?:from|import)\s*['"](\.{1,2}\/[^'"]+)['"]/g))
        visit(join(dirname(file), m[1] ?? ''));
    };
    visit(entry);
    sizes[name] = gzip(parts.join('\n'));
  }
  return sizes;
}

async function measure(): Promise<Measured> {
  const { entries, components } = targets();
  const esbuild = await loadEsbuild();
  const method: SizeMethod = esbuild ? 'esbuild-minify-gzip' : 'gzip';
  if (!esbuild)
    console.warn('esbuild could not be loaded: measuring gzip of the unminified module graph.');
  const size = (files: Record<string, string>) =>
    esbuild ? withEsbuild(esbuild, files) : Promise.resolve(withGzipOnly(files));
  return { method, entries: await size(entries), components: await size(components) };
}

let measured: Measured;
try {
  measured = await measure();
} catch (e) {
  console.error(`size:check: ${(e as Error).message}`);
  process.exit(1);
}
const previous = existsSync(OUT)
  ? (JSON.parse(readFileSync(OUT, 'utf8')) as SizeBudgets)
  : undefined;
const summary = `${Object.keys(measured.entries).length} entries, ${Object.keys(measured.components).length} components (${measured.method}); root entry ${kib(measured.entries['.'] ?? 0)}; largest components: ${largest(
  measured.components,
  3,
)
  .map(([name, bytes]) => `${name} ${kib(bytes)}`)
  .join(', ')}`;

if (process.argv.includes('--update')) {
  const next = updateBudgets(measured, previous);
  const text = await format(JSON.stringify(next), { ...(await resolveConfig(OUT)), filepath: OUT });
  writeFileSync(OUT, text);
  console.log(`wrote ${relative(ROOT, OUT)}: ${summary}`);
  const core = measured.entries['.'] ?? 0;
  if (core > next.coreHardCap) {
    console.error(
      `The root entry is ${kib(core)}, over its hard cap of ${kib(next.coreHardCap)}. --update does not raise the cap.`,
    );
    process.exit(1);
  }
} else {
  if (!previous) {
    console.error('registry/size-budgets.json is missing: run `npm run size:check -- --update`');
    process.exit(1);
  }
  const comparison = compareBudgets(measured, previous);
  if (!passes(comparison)) {
    console.error(formatComparison(comparison));
    process.exit(1);
  }
  console.log(`size budgets hold: ${summary}`);
}
