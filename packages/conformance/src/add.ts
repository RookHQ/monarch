/**
 * `monarch add <Component>`: what an app needs to use a component — the import
 * line, the stylesheets and a minimal example — from the manifest. With
 * `--write`, it adds missing stylesheet imports next to the app's existing
 * Monarch CSS imports (its root layout or global stylesheet).
 */
import { readFileSync, writeFileSync } from 'node:fs';
import { join } from 'node:path';
import { DEFAULT_EXCLUDE, styleSiteOf, walk } from './check.js';
import { applyEdits, styleImportLine } from './fixes.js';
import {
  cssOf,
  findExport,
  importOf,
  scopeOf,
  type Manifest,
  type ManifestExport,
  type ManifestProp,
} from './manifest.js';
import { emptyStyleImports, readStyleImports } from './subpath-css.js';

/** A placeholder value for a required prop in the example. */
function exampleValue(p: ManifestProp): string {
  if (p.values?.length) return JSON.stringify(p.values[0]);
  if (p.type === 'string') return `"${p.name}"`;
  if (p.type === 'boolean') return '{true}';
  if (p.type === 'number') return '{0}';
  if (/^\(.*\) => /.test(p.type)) return '{() => {}}';
  if (/\[\]$|^readonly /.test(p.type)) return '{[]}';
  return `{/* ${p.type} */ undefined}`;
}

/** A minimal JSX example: the required props, and children when it takes them. */
export function exampleOf(e: ManifestExport): string {
  if (e.kind === 'hook') return `const value = ${e.name}();`;
  if (e.kind !== 'component') return `${e.name}`;
  const required = e.props.filter((p) => p.required && p.name !== 'children');
  const attrs = required.map((p) => `${p.name}=${exampleValue(p)}`);
  const takesChildren = e.props.some((p) => p.name === 'children');
  const open = [e.name, ...attrs].join(' ');
  return takesChildren ? `<${open}>…</${e.name}>` : `<${open} />`;
}

/** Other exports documented on the same page (SelectTrigger, SelectItem … for Select). */
export function siblingsOf(manifest: Manifest, e: ManifestExport): ManifestExport[] {
  if (!e.docs) return [];
  return manifest.components.filter((c) => c.docs === e.docs && c.name !== e.name);
}

export interface AddPlan {
  readonly export: ManifestExport;
  readonly import: string;
  readonly css: readonly string[];
  readonly example: string;
  readonly docs?: string;
  readonly siblings: readonly string[];
  readonly notes: readonly string[];
}

export function planAdd(manifest: Manifest, name: string): AddPlan {
  const e = findExport(manifest, name);
  if (!e) throw new Error(`${name} is not an export of ${manifest.package}@${manifest.version}`);
  const notes: string[] = [];
  if (scopeOf(e) === 'rook')
    notes.push(
      'This export is Rook-specific (Rook cookies, Rook ID or the Rook Intelligence API). Outside Rook, see https://monarchui.dev/ for the general alternative.',
    );
  if (e.usesClient ?? e.clientOnly)
    notes.push(
      "The module is marked 'use client'. React Server Components can render it, but only with serialisable props: pass event handlers and render props from a client component.",
    );
  return {
    export: e,
    import: importOf(e),
    css: cssOf(e),
    example: exampleOf(e),
    ...(e.docs ? { docs: e.docs } : {}),
    siblings: siblingsOf(manifest, e).map((s) => s.name),
    notes,
  };
}

export function addText(plan: AddPlan): string {
  const lines = [
    `${plan.export.name} (${plan.export.kind}, from ${plan.import.replace(/^.* from '([^']+)';$/, '$1')})`,
    '',
    plan.import,
    '',
    '// Styles, once in the root layout (or load @rookcom/monarch-react/styles.css for everything):',
    ...plan.css.map((s) => `import '${s}';`),
    '',
    plan.example,
  ];
  if (plan.siblings.length) lines.push('', `Related exports: ${plan.siblings.join(', ')}`);
  if (plan.docs) lines.push('', `Docs: ${plan.docs}`);
  for (const n of plan.notes) lines.push('', `Note: ${n}`);
  return lines.join('\n') + '\n';
}

export interface WriteResult {
  /** File changed, relative to the root, or null when nothing was needed or possible. */
  readonly file: string | null;
  readonly added: readonly string[];
  readonly message: string;
}

/**
 * Inserts the stylesheets the app does not import yet, after the last Monarch
 * stylesheet import in its root layout or global CSS (a file that imports
 * @rookcom/monarch-react styles wins over one that only imports the tokens).
 */
export function writeCss(root: string, sheets: readonly string[]): WriteResult {
  const files = walk(root, ['.'], DEFAULT_EXCLUDE);
  const imported = emptyStyleImports();
  const tokens = new Set<string>();
  const sites: NonNullable<ReturnType<typeof styleSiteOf>>[] = [];
  for (const file of files) {
    const text = readFileSync(join(root, file), 'utf8');
    readStyleImports(text, imported);
    for (const m of text.matchAll(/@rookcom\/monarch-tokens\/css(?:\/[a-z0-9/-]+)?/g))
      tokens.add(m[0]);
    const site = styleSiteOf(file, text.split(/\r?\n/));
    if (site) sites.push(site);
  }
  const missing = sheets.filter((s) => {
    if (s.startsWith('@rookcom/monarch-tokens/')) return !tokens.has(s);
    if (imported.all) return false;
    const entry = /\/styles\/([a-z0-9-]+)\.css$/.exec(s)?.[1];
    const component = /\/styles\/components\/([a-z0-9-]+)\.css$/.exec(s)?.[1];
    if (entry) return !imported.entries.has(entry);
    if (component) return !imported.components.has(component);
    return true;
  });
  if (missing.length === 0)
    return { file: null, added: [], message: 'Every stylesheet it needs is already imported.' };
  const site = sites.find((s) => s.react) ?? sites[0];
  if (!site)
    return {
      file: null,
      added: [],
      message: `No file imports Monarch CSS yet. Add these lines to your root layout:\n${missing.map((s) => `import '${s}';`).join('\n')}`,
    };
  const path = join(root, site.file);
  const text = applyEdits(readFileSync(path, 'utf8'), [
    {
      file: site.file,
      line: site.after + 1,
      column: 1,
      length: 0,
      text: missing.map((s) => `${styleImportLine(site.file, s)}\n`).join(''),
    },
  ]);
  writeFileSync(path, text);
  return {
    file: site.file,
    added: missing,
    message: `Added ${missing.join(', ')} to ${site.file}.`,
  };
}
