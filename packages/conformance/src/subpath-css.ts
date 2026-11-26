/**
 * `styles/subpath-css`: components imported from a subpath entry of @rookcom/monarch-react
 * (`/patterns`, `/data`, `/charts`, `/ai`, …) are styled by `styles/<entry>.css` or
 * `styles/components/<id>.css`, not by `styles/core.css`. An app that loads only core.css
 * renders them unstyled. This is a project-level rule: it reads every scanned file for
 * imports from subpath entries and for Monarch stylesheet imports, then compares the two.
 *
 * Lexical like the other rules, and it prefers missing a case over inventing one: names it
 * does not know (a newer Monarch, a hook) never demand a sheet on their own.
 */
import { ALIAS_ENTRY_COMPONENTS, ENTRY_COMPONENT_SHEETS } from './entry-styles.js';

export const SUBPATH_CSS_RULE = 'styles/subpath-css';

const PKG = '@rookcom/monarch-react';

export interface SubpathImport {
  readonly file: string;
  readonly line: number;
  readonly column: number;
  /** Entry without `./`: `patterns`, `ai`, … */
  readonly entry: string;
  /** Imported names, or null when they cannot be read (`* as M`, a default, a dynamic import). */
  readonly names: readonly string[] | null;
}

export interface StyleImports {
  /** `@rookcom/monarch-react/styles.css`: every component. */
  all: boolean;
  /** `styles/<entry>.css`, including `core`. */
  readonly entries: Set<string>;
  /** `styles/components/<id>.css`. */
  readonly components: Set<string>;
}

export interface SubpathCssFinding {
  readonly file: string;
  readonly line: number;
  readonly column: number;
  readonly message: string;
  /** The stylesheets to import, as package specifiers (the fix). */
  readonly sheets: readonly string[];
}

/** Blanks comments (keeping offsets and newlines) so commented-out imports do not count. */
function stripComments(text: string): string {
  const blank = (m: string) => m.replace(/[^\n]/g, ' ');
  return text.replace(/\/\*[\s\S]*?\*\//g, blank).replace(/^[ \t]*\/\/.*$/gm, blank);
}

function position(text: string, index: number): { line: number; column: number } {
  const before = text.slice(0, index);
  const line = before.split('\n').length;
  return { line, column: index - before.lastIndexOf('\n') };
}

const ENTRY = '([a-z][a-z0-9-]*)';
/** `import { A, B as C } from '…/entry'`, `export { A } from`, `import * as M from`. */
const STATIC_IMPORT_RE = new RegExp(
  `\\b(?:import|export)\\s+(type\\s+)?([\\w$\\s{},*]*?)\\s*from\\s*(['"])${PKG.replace('/', '\\/')}\\/${ENTRY}\\3`,
  'g',
);
/** `import '…/entry'` and `import('…/entry')`. */
const BARE_IMPORT_RE = new RegExp(
  `\\bimport\\s*(?:\\(\\s*)?(['"])${PKG.replace('/', '\\/')}\\/${ENTRY}\\1`,
  'g',
);
const STYLE_IMPORT_RE = new RegExp(
  `${PKG.replace('/', '\\/')}\\/(?:dist\\/)?styles(?:\\.css|\\/components\\/([a-z0-9-]+)\\.css|\\/([a-z0-9-]+)\\.css)`,
  'g',
);

/** Names in an import clause; null when the clause is not a plain `{ … }` list. */
function namesOf(clause: string): string[] | null {
  const braces = /^\{([\s\S]*)\}$/.exec(clause.trim());
  if (!braces) return null;
  return (braces[1] ?? '')
    .split(',')
    .map((s) => s.trim())
    .filter((s) => s && !s.startsWith('type '))
    .map((s) => s.split(/\s+as\s+/)[0]?.trim() ?? '')
    .filter(Boolean);
}

/** Imports from subpath entries in one code file. Type-only imports render nothing and are skipped. */
export function readSubpathImports(file: string, text: string): SubpathImport[] {
  const code = stripComments(text);
  const out: SubpathImport[] = [];
  for (const m of code.matchAll(STATIC_IMPORT_RE)) {
    // `export default App` on the line above would start the match early: re-anchor on the
    // last import/export keyword of the match.
    const last = [...m[0].matchAll(/\b(?:import|export)\b/g)].pop()?.index ?? 0;
    const statement = /^(?:import|export)\s+(type\s+)?([\s\S]*?)\s*from\s*['"]/.exec(
      m[0].slice(last),
    );
    if (!statement || statement[1]) continue; // import type { … }
    const names = namesOf(statement[2] ?? '');
    if (names && names.length === 0) continue; // only `type X` specifiers
    out.push({ file, ...position(code, m.index + last), entry: m[4] ?? '', names });
  }
  for (const m of code.matchAll(BARE_IMPORT_RE))
    out.push({ file, ...position(code, m.index), entry: m[2] ?? '', names: null });
  return out.sort((a, b) => a.line - b.line || a.column - b.column);
}

/** Adds the Monarch component stylesheets a file loads (JS/TS imports or CSS `@import`). */
export function readStyleImports(text: string, into: StyleImports): void {
  for (const m of stripComments(text).matchAll(STYLE_IMPORT_RE)) {
    if (m[1]) into.components.add(m[1]);
    else if (m[2]) into.entries.add(m[2]);
    else into.all = true;
  }
}

export function emptyStyleImports(): StyleImports {
  return { all: false, entries: new Set(), components: new Set() };
}

const looksLikeComponent = (name: string) => /^[A-Z][A-Za-z0-9]*$/.test(name) && /[a-z]/.test(name);
const importLine = (path: string) => `\`import '${PKG}/${path}';\``;

/**
 * One finding per subpath entry whose components are used but not styled, at the first import
 * that needs a missing sheet. The message names the exact import to add.
 */
export function subpathCssFindings(
  imports: readonly SubpathImport[],
  styles: StyleImports,
): SubpathCssFinding[] {
  if (styles.all) return [];
  const byEntry = new Map<string, SubpathImport[]>();
  for (const i of imports) byEntry.set(i.entry, [...(byEntry.get(i.entry) ?? []), i]);

  const findings: SubpathCssFinding[] = [];
  for (const [entry, list] of byEntry) {
    if (styles.entries.has(entry)) continue;
    // An alias entry (`rook`) re-exports components another entry owns: the owner's sheet
    // styles them too, so `…/rook` with styles/shell.css loaded is complete.
    const alias = ALIAS_ENTRY_COMPONENTS[entry];
    const known = alias
      ? Object.fromEntries(Object.entries(alias).map(([name, c]) => [name, c.sheet]))
      : ENTRY_COMPONENT_SHEETS[entry];
    // Not a component entry this version knows, and nothing names a component: leave it.
    const sheets = known ? new Set(Object.values(known)) : new Set<string>();
    const loadedHere = [...styles.components].some((s) => sheets.has(s));

    const missing = new Map<string, string[]>(); // sheet → names
    let unreadable: SubpathImport | undefined;
    let first: SubpathImport | undefined;
    for (const i of list) {
      if (i.names === null || !known) {
        const components = i.names?.filter(looksLikeComponent) ?? null;
        if (components === null || components.length > 0) {
          unreadable ??= i;
          first ??= i;
        }
        continue;
      }
      for (const name of i.names) {
        const sheet = known[name];
        if (!sheet || styles.components.has(sheet)) continue;
        const owner = alias?.[name]?.owner;
        if (owner && styles.entries.has(owner)) continue;
        missing.set(sheet, [...(missing.get(sheet) ?? []), name]);
        first ??= i;
      }
    }
    // An import we cannot read is only reported when nothing of this entry is styled at all.
    if (missing.size === 0 && (!unreadable || loadedHere)) continue;
    const at = first ?? list[0];
    if (!at) continue;

    const used = [...new Set([...missing.values()].flat())].sort();
    const what = used.length
      ? `${used.map((n) => `\`${n}\``).join(', ')} ${used.length === 1 ? 'comes' : 'come'} from ${PKG}/${entry}`
      : `Components from ${PKG}/${entry} are imported`;
    const fix = loadedHere
      ? `Add ${[...missing.keys()]
          .sort()
          .map((s) => importLine(`styles/components/${s}.css`))
          .join(' and ')} (or ${importLine(`styles/${entry}.css`)} for the whole entry)`
      : `Add ${importLine(`styles/${entry}.css`)} next to your other Monarch CSS imports (or ${importLine('styles.css')} for every component)`;
    const toImport = loadedHere
      ? [...missing.keys()].sort().map((s) => `${PKG}/styles/components/${s}.css`)
      : [`${PKG}/styles/${entry}.css`];
    findings.push({
      file: at.file,
      line: at.line,
      column: at.column,
      sheets: toImport,
      message: `${what}, but no file loads ${used.length === 1 ? 'its' : 'their'} styles, so ${used.length === 1 ? 'it renders' : 'they render'} unstyled; styles/core.css does not include subpath components. ${fix}.`,
    });
  }
  return findings;
}
