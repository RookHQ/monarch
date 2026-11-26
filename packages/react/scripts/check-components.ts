/**
 * `npm run components:check` (part of `npm run verify`).
 *
 * Enforces the source layout and the component docs contract
 * (docs/architecture/SOURCE-LAYOUT.md):
 *
 *  1. Every folder sits at src/{components|patterns}/<family>/<id>/, where <id>
 *     is a registry id, <family> its category and the root matches its layer.
 *  2. Every folder has <id>.tsx, index.ts, <id>.test.tsx, <id>.docs.tsx and
 *     (unless exempted in NO_OWN_CSS) <id>.css. No stray files or sub-folders.
 *  3. Every value a folder exports is exported by a package entry; new
 *     components are exported from the entry their registry entry names.
 *  4. Every docs file has the 16 sections, none empty; `legacy-incomplete` only
 *     on pre-0.4 folders and always with a reason; related ids exist; the
 *     declared dependencies match the imports; testing claims match the test.
 *
 * Requires a built package (`npm run build`): docs files import
 * '@rookcom/monarch-react' like an app does.
 */
import { existsSync, readFileSync } from 'node:fs';
import { join } from 'node:path';
import { DOC_SECTIONS, isIncomplete, type ComponentDocs } from '../src/internal/component-docs.js';
import {
  ENTRY_FILES,
  FAMILIES,
  LEGACY_FOLDERS,
  NO_OWN_CSS,
  REPO_ROOT,
  ROOT_OF_LAYER,
  SRC_DIR,
  actualDependencies,
  docsFileOf,
  familyOfCategory,
  listFolders,
  loadDocs,
  resolveSpecifier,
  strayFiles,
  valueExports,
  type ComponentFolder,
} from './component-folders.js';
import ts from 'typescript';

interface RegistryEntry {
  id: string;
  category: string;
  layer: string;
  entry: string;
  package: string;
}

const errors: string[] = [];
const fail = (where: string, msg: string) => errors.push(`${where}: ${msg}`);

const registry = JSON.parse(readFileSync(join(REPO_ROOT, 'registry/components.json'), 'utf8')) as {
  components: RegistryEntry[];
};
const byId = new Map(registry.components.map((c) => [c.id, c]));
const folders = listFolders();
const cssOrder = new Set(
  (JSON.parse(readFileSync(join(SRC_DIR, 'styles/order.json'), 'utf8')) as { order: string[] })
    .order,
);

for (const s of strayFiles())
  fail(`src/${s}`, 'files belong in a component folder (src/<root>/<family>/<id>/)');

// ---------------------------------------------------------------- 1 + 2: layout
const ALLOWED = (id: string) =>
  new RegExp(`^${id}(\\.[a-z0-9-]+)?\\.(tsx|ts|css)$|^${id}\\.(test|docs)\\.tsx?$|^index\\.ts$`);
for (const f of folders) {
  const where = `src/${f.rel}`;
  const reg = byId.get(f.id);
  if (!/^[a-z][a-z0-9]*(-[a-z0-9]+)*$/.test(f.id))
    fail(where, 'folder name must be a kebab-case registry id');
  if (!(FAMILIES as readonly string[]).includes(f.family))
    fail(where, `unknown family "${f.family}"; use one of ${FAMILIES.join(', ')}`);
  if (!reg) fail(where, `"${f.id}" is not a registry id (registry/components.json)`);
  else {
    const fam = familyOfCategory(reg.category);
    if (fam !== f.family)
      fail(where, `registry category ${reg.category} files it under family "${fam}"`);
    const root = ROOT_OF_LAYER[reg.layer];
    if (root && root !== f.root) fail(where, `layer ${reg.layer} belongs in src/${root}/`);
    if (reg.package !== '@rookcom/monarch-react')
      fail(where, `registry places ${f.id} in ${reg.package}, not @rookcom/monarch-react`);
  }
  const required = [`${f.id}.tsx`, 'index.ts', `${f.id}.test.tsx`];
  if (!NO_OWN_CSS[f.id]) required.push(`${f.id}.css`);
  for (const r of required) if (!f.files.includes(r)) fail(where, `missing ${r}`);
  if (!docsFileOf(f)) fail(where, `missing ${f.id}.docs.tsx`);
  const ok = ALLOWED(f.id);
  for (const n of f.files) {
    if (!ok.test(n))
      fail(`${where}/${n}`, `unexpected file; files are named ${f.id}[.<part>].tsx|ts|css`);
    if (n.endsWith('.css') && !cssOrder.has(`${f.rel}/${n}`))
      fail(`${where}/${n}`, 'not listed in src/styles/order.json');
  }
}

// ---------------------------------------------------------------- 3: entries
const folderOfFile = (abs: string) => folders.find((f) => abs.startsWith(f.dir + '/'));
const exportedFrom = new Map<string, Map<string, Set<string>>>(); // entry -> folder id -> names
for (const [entry, file] of Object.entries(ENTRY_FILES)) {
  const abs = join(SRC_DIR, file);
  if (!existsSync(abs)) continue;
  const sf = ts.createSourceFile(abs, readFileSync(abs, 'utf8'), ts.ScriptTarget.ES2022, true);
  const map = new Map<string, Set<string>>();
  for (const st of sf.statements) {
    if (
      !ts.isExportDeclaration(st) ||
      !st.moduleSpecifier ||
      !ts.isStringLiteral(st.moduleSpecifier)
    )
      continue;
    const spec = st.moduleSpecifier.text;
    const target = resolveSpecifier(abs, spec);
    const folder = folderOfFile(target);
    if (!folder) continue;
    if (!spec.endsWith('/index.js'))
      fail(`src/${file}`, `import ${spec} through the folder's index.js`);
    const names =
      st.exportClause && ts.isNamedExports(st.exportClause)
        ? st.exportClause.elements
            .filter((e) => !e.isTypeOnly && !st.isTypeOnly)
            .map((e) => e.name.text)
        : valueExports(target);
    const set = map.get(folder.id) ?? new Set<string>();
    names.forEach((n) => set.add(n));
    map.set(folder.id, set);
  }
  exportedFrom.set(entry, map);
}
for (const f of folders) {
  const where = `src/${f.rel}`;
  if (!f.files.includes('index.ts')) continue;
  const names = valueExports(join(f.dir, 'index.ts'));
  if (names.length === 0) fail(where, 'index.ts exports no values');
  const entries = [...exportedFrom.entries()].filter(([, m]) => m.has(f.id)).map(([e]) => e);
  if (entries.length === 0) {
    fail(
      where,
      `not exported from any package entry; add it to src/${ENTRY_FILES[byId.get(f.id)?.entry ?? '.']}`,
    );
    continue;
  }
  const published = new Set(entries.flatMap((e) => [...(exportedFrom.get(e)?.get(f.id) ?? [])]));
  for (const n of names)
    if (!published.has(n)) fail(where, `${n} is exported by index.ts but by no package entry`);
  const want = byId.get(f.id)?.entry;
  if (!LEGACY_FOLDERS.has(f.id) && want && !entries.includes(want))
    fail(where, `registry entry is "${want}" but it is exported from ${entries.join(', ')}`);
}

// ---------------------------------------------------------------- 4: docs
const nonEmpty = (v: unknown): boolean => {
  if (v == null) return false;
  if (typeof v === 'string') return v.trim().length > 0;
  if (Array.isArray(v)) return v.length > 0;
  if (typeof v === 'object') return Object.values(v).some(nonEmpty);
  return true;
};
const sameSet = (a: string[], b: string[]) =>
  a.length === b.length && [...a].sort().every((x, i) => x === [...b].sort()[i]);

/** Content checks per section, for a complete section. */
function checkSection(where: string, id: string, s: Record<string, unknown>, f: ComponentFolder) {
  const items = s.items as unknown[] | undefined;
  switch (id) {
    case 'purpose':
      if (!nonEmpty(s.summary) || !nonEmpty(s.useWhen))
        fail(where, 'purpose needs summary and useWhen');
      return;
    case 'apiReference':
    case 'typedProperties':
      if (s.source !== 'typescript') fail(where, `${id} is generated: set source: 'typescript'`);
      return;
    case 'keyboard':
      if (!nonEmpty(items) && !nonEmpty(s.notes))
        fail(where, 'keyboard: list the keys, or say in notes why there are none');
      return;
    case 'accessibility':
      if (!nonEmpty(items)) fail(where, 'accessibility needs items');
      return;
    case 'doDont':
      if (!nonEmpty(s.do) || !nonEmpty(s.dont)) fail(where, "doDont needs both do and don't");
      return;
    case 'examples':
      if (!nonEmpty(items)) fail(where, 'examples needs at least one live example');
      for (const ex of (items ?? []) as { title?: unknown; render?: unknown; code?: unknown }[])
        if (
          typeof ex.render !== 'function' ||
          !nonEmpty(ex.title) ||
          (ex.code !== undefined && !nonEmpty(ex.code))
        )
          fail(where, 'every example needs title and render() (code, if given, must not be empty)');
      return;
    case 'related':
      if (!nonEmpty(items)) fail(where, 'related needs items');
      for (const r of (items ?? []) as { id: string }[])
        if (!byId.has(r.id)) fail(where, `related id "${r.id}" is not in the registry`);
      return;
    case 'dependencies': {
      const real = actualDependencies(f, folders);
      const pk = (s.packages as string[] | undefined) ?? [];
      const cm = (s.components as string[] | undefined) ?? [];
      if (!sameSet(pk, real.packages))
        fail(where, `dependencies.packages is [${pk}] but the source imports [${real.packages}]`);
      if (!sameSet(cm, real.components))
        fail(
          where,
          `dependencies.components is [${cm}] but the source composes [${real.components}]`,
        );
      return;
    }
    case 'testing': {
      const test = join(f.dir, `${f.id}.test.tsx`);
      const text = existsSync(test) ? readFileSync(test, 'utf8') : '';
      if (!nonEmpty(s.unit)) fail(where, 'testing.unit must list what the test proves');
      if (s.axe === true && !text.includes('expectNoA11yViolations'))
        fail(where, 'testing.axe is true but the test never calls expectNoA11yViolations');
      if (s.ssr === true && !/renderToString|renderToStaticMarkup/.test(text))
        fail(where, 'testing.ssr is true but the test never renders on the server');
      if (s.axe === false && text.includes('expectNoA11yViolations'))
        fail(where, 'testing.axe should be true');
      return;
    }
    default:
      if (!nonEmpty(items) && !nonEmpty(s.notes))
        fail(where, `${id} needs items, or notes saying why there are none`);
  }
}

let complete = 0;
let incompleteSections = 0;
for (const f of folders) {
  const name = docsFileOf(f);
  if (!name) continue;
  const where = `src/${f.rel}/${name}`;
  let docs: ComponentDocs | undefined;
  try {
    docs = (await loadDocs(f)) as ComponentDocs | undefined;
  } catch (e) {
    fail(where, `cannot be imported: ${(e as Error).message}`);
    continue;
  }
  if (!docs || typeof docs !== 'object') {
    fail(where, 'must `export default { … } satisfies ComponentDocs`');
    continue;
  }
  if (docs.id !== f.id) fail(where, `id is "${docs.id}", expected "${f.id}"`);
  if (/\bTODO\b/.test(readFileSync(join(f.dir, name), 'utf8')))
    fail(where, 'still has TODO markers from the scaffold; replace them with what the code does');
  if (!nonEmpty(docs.title) || !nonEmpty(docs.summary))
    fail(where, 'title and summary are required');
  const sections = (docs.sections ?? {}) as unknown as Record<string, unknown>;
  for (const k of Object.keys(sections))
    if (!(DOC_SECTIONS as readonly string[]).includes(k)) fail(where, `unknown section "${k}"`);
  let ok = true;
  for (const id of DOC_SECTIONS) {
    const s = sections[id];
    if (s === undefined) {
      fail(where, `missing section "${id}"`);
      ok = false;
      continue;
    }
    if (isIncomplete(s as never)) {
      ok = false;
      incompleteSections++;
      const reason = (s as { reason?: string }).reason;
      if (!LEGACY_FOLDERS.has(f.id))
        fail(where, `"${id}" is legacy-incomplete, which only pre-0.4 components may use`);
      if (!nonEmpty(reason)) fail(where, `"${id}" is legacy-incomplete without a reason`);
      continue;
    }
    checkSection(where, id, s as Record<string, unknown>, f);
  }
  if (ok) complete++;
}

if (errors.length) {
  for (const e of errors) console.error(`error: ${e}`);
  console.error(`\n${errors.length} problem(s). See docs/architecture/SOURCE-LAYOUT.md.`);
  process.exit(1);
}
console.log(
  `components: ${folders.length} folders conform; docs complete for ${complete}, ${incompleteSections} legacy-incomplete section(s) elsewhere`,
);
