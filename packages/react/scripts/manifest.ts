/**
 * Emits dist/manifest.json: every public component with its props, types,
 * defaults and doc comments, read from the TypeScript declarations.
 *
 * This is the machine-readable contract. The docs site, the conformance
 * engine (`components/duplicated-primitive`) and AI coding agents consume it
 * instead of guessing from source.
 */
import { existsSync, readFileSync, writeFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import ts from 'typescript';

const here = dirname(fileURLToPath(import.meta.url));
const pkgDir = join(here, '..');
import { ENTRY_FILES } from './component-folders.js';

/** Package entries that exist today (`.`, and subpaths such as `./marketing`). */
const entries = Object.entries(ENTRY_FILES)
  .map(([name, file]) => ({ name, file: join(pkgDir, 'src', file) }))
  .filter((e) => existsSync(e.file));
const pkg = JSON.parse(readFileSync(join(pkgDir, 'package.json'), 'utf8')) as {
  name: string;
  version: string;
};

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

interface PropDoc {
  name: string;
  type: string;
  required: boolean;
  description: string;
}
interface ComponentDoc {
  name: string;
  /** Package entry it is imported from: `.` or a subpath such as `./marketing`. */
  entry: string;
  /** Other entries that re-export the same declaration (e.g. `./data` for Table). */
  alsoEntries?: string[];
  kind: 'component' | 'hook' | 'utility';
  description: string;
  className?: string;
  clientOnly: boolean;
  props: PropDoc[];
}

const docOf = (sym: ts.Symbol | undefined) =>
  sym ? ts.displayPartsToString(sym.getDocumentationComment(checker)).trim() : '';

function propsOf(type: ts.Type): PropDoc[] {
  const out: PropDoc[] = [];
  for (const p of type.getProperties()) {
    const decl = p.valueDeclaration ?? p.declarations?.[0];
    if (!decl) continue;
    // Skip the hundreds of inherited DOM attributes; keep Monarch's own and the common ones.
    const file = decl.getSourceFile().fileName;
    const own = file.includes('/packages/react/src/');
    const common = ['className', 'children', 'disabled', 'id', 'asChild'].includes(p.name);
    if (!own && !common) continue;
    const t = checker.getTypeOfSymbolAtLocation(p, decl);
    out.push({
      name: p.name,
      type: checker
        .typeToString(t, decl, ts.TypeFormatFlags.NoTruncation)
        .replace(/ \| undefined$/, ''),
      required: !(p.flags & ts.SymbolFlags.Optional),
      description: docOf(p),
    });
  }
  return out.sort(
    (a, b) => Number(b.required) - Number(a.required) || a.name.localeCompare(b.name),
  );
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
    const type = checker.getTypeOfSymbolAtLocation(sym, decl);
    const name = exp.name;
    const file = decl.getSourceFile();
    const clientOnly = /^['"]use client['"]/.test(file.text.trimStart());
    // SCREAMING_CASE constants (ROOK_THEME_COOKIE) are utilities, not components.
    const kind: ComponentDoc['kind'] = /^use[A-Z]/.test(name)
      ? 'hook'
      : /^[A-Z]/.test(name) && !/^[A-Z][A-Z0-9]*_[A-Z0-9_]+$/.test(name)
        ? 'component'
        : 'utility';
    let props: PropDoc[] = [];
    if (kind === 'component') {
      // Class components (error boundaries) take props through the constructor.
      const sig = type.getCallSignatures()[0] ?? type.getConstructSignatures()[0];
      const param = sig?.getParameters()[0];
      if (param) {
        const pt = checker.getTypeOfSymbolAtLocation(param, decl);
        props = propsOf(pt);
      } else {
        // forwardRef / Radix re-exports expose props as the first type argument.
        const ref = checker.getTypeArguments(type as ts.TypeReference)[0];
        if (ref) props = propsOf(ref);
      }
    }
    const className = /mn-[a-z-]+/.exec(file.text)?.[0];
    const existing = components.find((c) => c.name === name);
    if (existing) {
      // The same declaration may be re-exported by a subpath for grouping
      // (Table from `.` and `./data`); two different declarations may not share a name.
      if (declOf.get(name) !== decl)
        throw new Error(`${name} is exported from two package entries`);
      existing.alsoEntries = [...(existing.alsoEntries ?? []), entryName];
      continue;
    }
    declOf.set(name, decl);
    components.push({
      name,
      entry: entryName,
      kind,
      description: docOf(sym) || docOf(exp),
      ...(className && kind === 'component' ? { className } : {}),
      clientOnly,
      props,
    });
  }
}
components.sort((a, b) => a.name.localeCompare(b.name));

const manifest = {
  $schema: 'https://monarch.rookhq.com/schema/manifest.json',
  package: pkg.name,
  version: pkg.version,
  generatedBy: 'packages/react/scripts/manifest.ts',
  styles: '@rookcom/monarch-react/styles.css',
  tokens: '@rookcom/monarch-tokens/css',
  components,
};
writeFileSync(join(pkgDir, 'dist/manifest.json'), JSON.stringify(manifest, null, 2) + '\n');
console.log(
  `manifest.json: ${components.length} exports, ${components.reduce((n, c) => n + c.props.length, 0)} documented props`,
);
