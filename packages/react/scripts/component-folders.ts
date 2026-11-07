/**
 * The source-layout model of @rookcom/monarch-react, shared by
 * check-components.ts, new-component.ts and scripts/registry/build-components.mjs.
 *
 *   src/components/<family>/<id>/   primitives and components
 *   src/patterns/<family>/<id>/     patterns (and templates until they get a home)
 *
 * <family> is the registry category without its number (`03-actions` → `actions`);
 * <id> is the registry id. See docs/architecture/SOURCE-LAYOUT.md.
 */
import { existsSync, readdirSync, readFileSync } from 'node:fs';
import { dirname, join, relative } from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';
import ts from 'typescript';

export const PKG_DIR = join(dirname(fileURLToPath(import.meta.url)), '..');
export const SRC_DIR = join(PKG_DIR, 'src');
export const REPO_ROOT = join(PKG_DIR, '..', '..');

/** The 22 taxonomy categories (docs/architecture/COMPONENT-TAXONOMY.md), as folder names. */
export const FAMILIES = [
  'layout',
  'typography',
  'actions',
  'forms',
  'navigation',
  'data-display',
  'data-grid',
  'feedback',
  'overlays',
  'scheduling',
  'charts',
  'files',
  'search',
  'identity',
  'collaboration',
  'workflow',
  'intelligence',
  'crm',
  'technical',
  'billing',
  'geo',
  'interaction',
] as const;
export type Family = (typeof FAMILIES)[number];

/** `03-actions` → `actions`. */
export const familyOfCategory = (category: string): string => category.replace(/^\d+-/, '');

/** Which top-level folder a registry layer lives in. */
export const ROOT_OF_LAYER: Record<string, 'components' | 'patterns'> = {
  primitive: 'components',
  component: 'components',
  pattern: 'patterns',
  template: 'patterns',
};

/** Entry point (package.json export) → the source file that is that entry's index. */
export const ENTRY_FILES: Record<string, string> = {
  '.': 'index.ts',
  './data': 'entries/data.ts',
  './charts': 'entries/charts.ts',
  './scheduling': 'entries/scheduling.ts',
  './collaboration': 'entries/collaboration.ts',
  './patterns': 'entries/patterns.ts',
  './marketing': 'entries/marketing.ts',
  './editor': 'entries/editor.ts',
  './shell': 'entries/shell.ts',
  './ai': 'entries/ai.ts',
};

/**
 * Folders that existed before the Monarch 0.4 restructure. Only these may mark
 * docs sections `legacy-incomplete`; every new component ships complete docs.
 */
export const LEGACY_FOLDERS = new Set([
  'alert',
  'avatar',
  'badge',
  'breadcrumb',
  'button',
  'card',
  'checkbox',
  'combobox',
  'data-table',
  'description-list',
  'dialog',
  'dropdown-menu',
  'empty-state',
  'error-state',
  'filter-bar',
  'form-field',
  'kbd',
  'link',
  'page-header',
  'pagination',
  'popover',
  'progress-bar',
  'select',
  'separator',
  'settings-section',
  'skeleton',
  'spinner',
  'split-layout',
  'stat-card',
  'switch',
  'tabs',
  'text-field',
  'toast',
  'toolbar',
  'tooltip',
  'visually-hidden',
]);

/**
 * Folders with no stylesheet of their own, and why. Every other folder must
 * have `<id>.css`, listed in src/styles/order.json.
 */
export const NO_OWN_CSS: Record<string, string> = {
  'dropdown-menu':
    'its .mn-menu styles are shared with Select and Combobox, so they live in styles/menu.css',
  'consent-manager':
    'it renders no markup of its own: a provider, hooks and a gate; the banner and preferences have their own styles',
  'ai-conversation':
    'it renders no markup: the message model, the useAiConversation hook and its transport adapters',
  'ai-scroll-anchor': 'it is a headless hook; the thread that uses it owns the styles',
  'visually-hidden':
    '.mn-visually-hidden is a foundation utility (styles/foundation.css) that other components and apps use directly',
};

export interface ComponentFolder {
  /** Registry id, which is the folder name. */
  id: string;
  family: string;
  root: 'components' | 'patterns';
  /** Absolute path. */
  dir: string;
  /** Path relative to packages/react/src, e.g. `components/actions/button`. */
  rel: string;
  files: string[];
}

/** Every component folder, sorted by path. */
export function listFolders(): ComponentFolder[] {
  const out: ComponentFolder[] = [];
  for (const root of ['components', 'patterns'] as const) {
    const rootDir = join(SRC_DIR, root);
    if (!existsSync(rootDir)) continue;
    for (const family of readdirSync(rootDir, { withFileTypes: true })) {
      if (!family.isDirectory()) continue;
      for (const id of readdirSync(join(rootDir, family.name), { withFileTypes: true })) {
        if (!id.isDirectory()) continue;
        const dir = join(rootDir, family.name, id.name);
        out.push({
          id: id.name,
          family: family.name,
          root,
          dir,
          rel: `${root}/${family.name}/${id.name}`,
          files: readdirSync(dir).sort(),
        });
      }
    }
  }
  return out.sort((a, b) => a.rel.localeCompare(b.rel));
}

/** Stray files directly under src/components, src/components/<family>, and the same for patterns. */
export function strayFiles(): string[] {
  const out: string[] = [];
  for (const root of ['components', 'patterns']) {
    const rootDir = join(SRC_DIR, root);
    if (!existsSync(rootDir)) continue;
    for (const e of readdirSync(rootDir, { withFileTypes: true })) {
      if (!e.isDirectory()) out.push(`${root}/${e.name}`);
      else
        for (const f of readdirSync(join(rootDir, e.name), { withFileTypes: true }))
          if (!f.isDirectory()) out.push(`${root}/${e.name}/${f.name}`);
    }
  }
  return out;
}

export const docsFileOf = (f: ComponentFolder): string | undefined =>
  [`${f.id}.docs.tsx`, `${f.id}.docs.ts`].find((n) => f.files.includes(n));

const parse = (file: string) =>
  ts.createSourceFile(file, readFileSync(file, 'utf8'), ts.ScriptTarget.ES2022, true);

/** Value (non-type) names a module exports, following `export *` within src. */
export function valueExports(file: string, seen = new Set<string>()): string[] {
  if (seen.has(file)) return [];
  seen.add(file);
  const sf = parse(file);
  const names: string[] = [];
  const isExported = (n: ts.Node) =>
    ts.canHaveModifiers(n) &&
    (ts.getModifiers(n) ?? []).some((m) => m.kind === ts.SyntaxKind.ExportKeyword);
  for (const st of sf.statements) {
    if (ts.isExportDeclaration(st)) {
      if (st.isTypeOnly) continue;
      const spec =
        st.moduleSpecifier && ts.isStringLiteral(st.moduleSpecifier)
          ? st.moduleSpecifier.text
          : null;
      if (!st.exportClause) {
        if (spec?.startsWith('.')) names.push(...valueExports(resolveSpecifier(file, spec), seen));
        continue;
      }
      if (ts.isNamedExports(st.exportClause))
        for (const el of st.exportClause.elements) if (!el.isTypeOnly) names.push(el.name.text);
    } else if (ts.isVariableStatement(st) && isExported(st)) {
      for (const d of st.declarationList.declarations)
        if (ts.isIdentifier(d.name)) names.push(d.name.text);
    } else if (
      (ts.isFunctionDeclaration(st) || ts.isClassDeclaration(st)) &&
      isExported(st) &&
      st.name
    ) {
      names.push(st.name.text);
    }
  }
  return [...new Set(names)].sort();
}

/** `./button.js` from `/x/index.ts` → `/x/button.tsx` (or .ts). */
export function resolveSpecifier(fromFile: string, spec: string): string {
  const base = join(dirname(fromFile), spec).replace(/\.js$/, '');
  for (const ext of ['.tsx', '.ts', '/index.ts']) if (existsSync(base + ext)) return base + ext;
  throw new Error(`cannot resolve ${spec} from ${relative(SRC_DIR, fromFile)}`);
}

/** Module specifiers a file imports or re-exports (excluding `import type`). */
export function importsOf(file: string): string[] {
  const sf = parse(file);
  const out: string[] = [];
  for (const st of sf.statements) {
    if (ts.isImportDeclaration(st) && ts.isStringLiteral(st.moduleSpecifier)) {
      if (st.importClause?.isTypeOnly) continue;
      out.push(st.moduleSpecifier.text);
    } else if (
      ts.isExportDeclaration(st) &&
      st.moduleSpecifier &&
      ts.isStringLiteral(st.moduleSpecifier)
    ) {
      if (!st.isTypeOnly) out.push(st.moduleSpecifier.text);
    }
  }
  return out;
}

/** Bare package name of a specifier: `@radix-ui/react-slot/x` → `@radix-ui/react-slot`. */
export const packageOf = (spec: string) =>
  spec.startsWith('@') ? spec.split('/').slice(0, 2).join('/') : (spec.split('/')[0] ?? spec);

/** The runtime files of a folder: everything but tests, docs and the index. */
export const runtimeFiles = (f: ComponentFolder) =>
  f.files.filter((n) => /\.tsx?$/.test(n) && !/\.(test|docs)\.tsx?$/.test(n) && n !== 'index.ts');

/** What a folder's runtime code really depends on, for checking the docs `dependencies` section. */
export function actualDependencies(f: ComponentFolder, all: ComponentFolder[]) {
  const packages = new Set<string>();
  const components = new Set<string>();
  for (const n of runtimeFiles(f)) {
    const file = join(f.dir, n);
    for (const spec of importsOf(file)) {
      if (!spec.startsWith('.')) {
        packages.add(packageOf(spec));
        continue;
      }
      const target = join(dirname(file), spec);
      const other = all.find((o) => o !== f && (target + '/').startsWith(o.dir + '/'));
      if (other) components.add(other.id);
    }
  }
  return { packages: [...packages].sort(), components: [...components].sort() };
}

/** Imports a folder's docs file. Needs a built dist (docs import '@rookcom/monarch-react'). */
export async function loadDocs(f: ComponentFolder): Promise<unknown> {
  const name = docsFileOf(f);
  if (!name) return undefined;
  const mod = (await import(pathToFileURL(join(f.dir, name)).href)) as { default?: unknown };
  return mod.default;
}
