/**
 * Emits dist/manifest.json: every public export with its props, types,
 * defaults, allowed values, doc comments, import line, stylesheets and docs
 * page, read from the TypeScript declarations. Also writes the manifest's JSON
 * Schema to the docs site (apps/docs/public/schema/manifest.json).
 *
 * This is the machine-readable contract. The docs site, the conformance CLI
 * (`monarch search`, `monarch add`, `monarch mcp`) and AI coding agents consume
 * it instead of guessing from source.
 */
import { existsSync, mkdirSync, readFileSync, writeFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { format, resolveConfig } from 'prettier';
import ts from 'typescript';
import {
  ALIAS_ENTRIES,
  ENTRY_FILES,
  listFolders,
  docsFileOf,
  valueExports,
} from './component-folders.js';
import {
  DOCS_SITE,
  MANIFEST_SCHEMA_URL,
  PACKAGE,
  TOKENS_CSS,
  componentCssOf,
  cssClassesOf,
  cssOf,
  docsUrlOf,
  importLineOf,
  pickRootClass,
  preferredEntry,
  scopeOf,
  type Scope,
} from './manifest-lib.js';
import { MANIFEST_SCHEMA } from './manifest-schema.js';
import type { SizeBudgets } from './size-budget.js';
import {
  allClassLiterals,
  destructuringDefaults,
  docOf,
  implementationOf,
  propsOf,
  rootClassCandidates,
  type PropDoc,
} from './manifest-ts.js';

const here = dirname(fileURLToPath(import.meta.url));
const pkgDir = join(here, '..');
const repoRoot = join(pkgDir, '..', '..');

/** Package entries that exist today (`.`, and subpaths such as `./marketing`). */
const entries = Object.entries(ENTRY_FILES)
  .map(([name, file]) => ({ name, file: join(pkgDir, 'src', file) }))
  .filter((e) => existsSync(e.file));
const pkg = JSON.parse(readFileSync(join(pkgDir, 'package.json'), 'utf8')) as {
  name: string;
  version: string;
  dependencies?: Record<string, string>;
  peerDependencies?: Record<string, string>;
};
const dependencies = Object.keys({ ...pkg.dependencies, ...pkg.peerDependencies }).filter(
  (d) => !['react', 'react-dom', '@rookcom/monarch-tokens'].includes(d),
);

const program = ts.createProgram(
  entries.map((e) => e.file),
  {
    jsx: ts.JsxEmit.ReactJSX,
    module: ts.ModuleKind.ESNext,
    moduleResolution: ts.ModuleResolutionKind.Bundler,
    target: ts.ScriptTarget.ES2022,
    strict: true,
    skipLibCheck: true,
  },
);
const checker = program.getTypeChecker();

/** Every class a Monarch stylesheet selects, to verify root classes against. */
const cssClasses = new Set<string>();
/** Export name → its component folder (docs page and per-component sheet). */
const folderOfExport = new Map<string, { id: string; docs: boolean; css: boolean }>();
for (const f of listFolders()) {
  for (const n of f.files.filter((x) => x.endsWith('.css')))
    for (const c of cssClassesOf(readFileSync(join(f.dir, n), 'utf8'))) cssClasses.add(c);
  const index = join(f.dir, 'index.ts');
  if (!existsSync(index)) continue;
  const info = { id: f.id, docs: Boolean(docsFileOf(f)), css: f.files.includes(`${f.id}.css`) };
  for (const name of valueExports(index))
    if (!folderOfExport.has(name)) folderOfExport.set(name, info);
}
const stylesDir = join(pkgDir, 'src/styles');
for (const n of ['foundation.css', 'choice.css', 'menu.css', 'chart.css', 'shell-account.css'])
  if (existsSync(join(stylesDir, n)))
    for (const c of cssClassesOf(readFileSync(join(stylesDir, n), 'utf8'))) cssClasses.add(c);

/**
 * Component folder → gzip bytes, from registry/size-budgets.json (`npm run
 * size:check -- --update`). The file is committed, so the manifest does not
 * depend on measuring the build that is writing it.
 */
const sizeBudgetsFile = join(repoRoot, 'registry/size-budgets.json');
const componentSizes: Readonly<Record<string, { gzip: number }>> = existsSync(sizeBudgetsFile)
  ? (JSON.parse(readFileSync(sizeBudgetsFile, 'utf8')) as SizeBudgets).components
  : {};

interface ComponentDoc {
  name: string;
  /** Package entry it is imported from: `.` or a subpath such as `./marketing`. */
  entry: string;
  /** Other entries that re-export the same declaration (e.g. `./data` for Table). */
  alsoEntries?: string[];
  kind: 'component' | 'hook' | 'utility';
  scope: Scope;
  description: string;
  className?: string;
  usesClient: boolean;
  /** @deprecated Same value as usesClient; kept for 0.14 consumers. */
  clientOnly: boolean;
  import: string;
  css: string[];
  componentCss?: string;
  docs?: string;
  /** Minified, gzipped bytes of the component's module, package imports external. */
  size?: { gzip: number };
  props: PropDoc[];
}

/** The doc comment of a component's props type, when the component itself has none. */
function propsTypeDoc(type: ts.Type | undefined): string {
  const sym = type?.aliasSymbol ?? type?.getSymbol();
  if (!sym || sym.getName() === '__type') return '';
  const decl = sym.declarations?.[0];
  if (!decl?.getSourceFile().fileName.includes('/packages/react/src/')) return '';
  return docOf(checker, sym);
}

const components: ComponentDoc[] = [];
const declOf = new Map<string, ts.Declaration>();
for (const { name: entryName, file: entryFile } of entries) {
  const source = program.getSourceFile(entryFile);
  if (!source) throw new Error(`entry not found: ${entryFile}`);
  const moduleSymbol = checker.getSymbolAtLocation(source);
  if (!moduleSymbol) throw new Error(`no module symbol: ${entryFile}`);
  for (const exp of checker.getExportsOfModule(moduleSymbol)) {
    const sym = exp.flags & ts.SymbolFlags.Alias ? checker.getAliasedSymbol(exp) : exp;
    const decl = sym.valueDeclaration ?? sym.declarations?.[0];
    if (!decl || !(sym.flags & ts.SymbolFlags.Value)) continue;
    const name = exp.name;
    const existing = components.find((c) => c.name === name);
    if (existing) {
      // The same declaration may be re-exported by a subpath for grouping
      // (Table from `.` and `./data`); two different declarations may not share a name.
      if (declOf.get(name) !== decl)
        throw new Error(`${name} is exported from two package entries`);
      existing.alsoEntries = [...(existing.alsoEntries ?? []), entryName];
      continue;
    }
    const type = checker.getTypeOfSymbolAtLocation(sym, decl);
    const file = decl.getSourceFile();
    const usesClient = /^['"]use client['"]/.test(file.text.trimStart());
    // SCREAMING_CASE constants (ROOK_THEME_COOKIE) are utilities, not components.
    const kind: ComponentDoc['kind'] = /^use[A-Z]/.test(name)
      ? 'hook'
      : /^[A-Z]/.test(name) && !/^[A-Z][A-Z0-9]*_[A-Z0-9_]+$/.test(name)
        ? 'component'
        : 'utility';
    const impl = implementationOf(decl);
    let props: PropDoc[] = [];
    let propsType: ts.Type | undefined;
    if (kind === 'component') {
      // Class components (error boundaries) take props through the constructor.
      const sig = type.getCallSignatures()[0] ?? type.getConstructSignatures()[0];
      const param = sig?.getParameters()[0];
      if (param) propsType = checker.getTypeOfSymbolAtLocation(param, decl);
      // forwardRef / Radix re-exports expose props as the first type argument.
      else propsType = checker.getTypeArguments(type as ts.TypeReference)[0];
      if (propsType)
        props = propsOf(propsType, {
          checker,
          dependencies,
          defaults: destructuringDefaults(impl),
        });
    }
    const className =
      kind === 'component'
        ? pickRootClass(name, rootClassCandidates(impl), cssClasses, allClassLiterals(impl))
        : undefined;
    const folder = folderOfExport.get(name);
    const description = docOf(checker, sym) || docOf(checker, exp) || propsTypeDoc(propsType);
    declOf.set(name, decl);
    components.push({
      name,
      entry: entryName,
      kind,
      scope: scopeOf(name),
      description,
      ...(className ? { className } : {}),
      usesClient,
      clientOnly: usesClient,
      import: importLineOf(name, entryName),
      css: cssOf(entryName),
      ...(folder?.css && kind === 'component' ? { componentCss: componentCssOf(folder.id) } : {}),
      ...(folder?.docs ? { docs: docsUrlOf(folder.id) } : {}),
      ...(folder && componentSizes[folder.id]
        ? { size: { gzip: componentSizes[folder.id]?.gzip ?? 0 } }
        : {}),
      props,
    });
  }
}
// An export that an alias entry (`./rook`) carries is listed under that entry
// first: its import line and stylesheets point there, and the entries that
// still ship it (deprecated paths) follow in `alsoEntries`.
for (const c of components) {
  const preferred = preferredEntry([c.entry, ...(c.alsoEntries ?? [])], ALIAS_ENTRIES);
  if (preferred.entry === c.entry) continue;
  c.entry = preferred.entry;
  if (preferred.alsoEntries) c.alsoEntries = preferred.alsoEntries;
  c.import = importLineOf(c.name, c.entry);
  c.css = cssOf(c.entry);
}
components.sort((a, b) => a.name.localeCompare(b.name));

const manifest = {
  $schema: MANIFEST_SCHEMA_URL,
  package: pkg.name,
  version: pkg.version,
  generatedBy: 'packages/react/scripts/manifest.ts',
  styles: `${PACKAGE}/styles.css`,
  tokens: TOKENS_CSS,
  docs: `${DOCS_SITE}/`,
  components,
};
writeFileSync(join(pkgDir, 'dist/manifest.json'), JSON.stringify(manifest, null, 2) + '\n');

const schemaOut = join(repoRoot, 'apps/docs/public/schema/manifest.json');
mkdirSync(dirname(schemaOut), { recursive: true });
// Formatted like every other committed JSON file, so `format:check` stays clean.
writeFileSync(
  schemaOut,
  await format(JSON.stringify(MANIFEST_SCHEMA), {
    ...(await resolveConfig(schemaOut)),
    filepath: schemaOut,
  }),
);

const count = (f: (c: ComponentDoc) => boolean) => components.filter(f).length;
console.log(
  `manifest.json: ${components.length} exports, ${components.reduce((n, c) => n + c.props.length, 0)} documented props, ` +
    `${count((c) => Boolean(c.className))} root classes, ${count((c) => c.scope === 'rook')} Rook-specific`,
);
