/**
 * Pure helpers for scripts/manifest.ts: everything about a manifest entry that
 * does not need the TypeScript program. Kept separate so it is unit-tested
 * (scripts/manifest-lib.test.ts) without building the package.
 */

/** The canonical docs site. Every URL in the manifest points here. */
export const DOCS_SITE = 'https://monarchui.dev';
export const MANIFEST_SCHEMA_URL = `${DOCS_SITE}/schema/manifest.json`;
export const PACKAGE = '@rookcom/monarch-react';
export const TOKENS_CSS = '@rookcom/monarch-tokens/css';

export type Scope = 'rook' | 'general';

/**
 * Exports that encode a Rook platform contract (Rook cookies, Rook ID, the Rook
 * Intelligence API, the Rook bar and rail). docs/guides/rook-specific-exports.md
 * lists them; each has `Rook` in its name, starts with `rook` or is a `ROOK_*` constant.
 */
export function scopeOf(name: string): Scope {
  return /Rook|^rook|^ROOK_/.test(name) ? 'rook' : 'general';
}

/** `.` → `@rookcom/monarch-react`; `./ai` → `@rookcom/monarch-react/ai`. */
export function moduleOf(entry: string): string {
  return entry === '.' ? PACKAGE : `${PACKAGE}/${entry.replace(/^\.\//, '')}`;
}

/** The exact import line for one export. */
export function importLineOf(name: string, entry: string): string {
  return `import { ${name} } from '${moduleOf(entry)}';`;
}

/**
 * Stylesheets an app needs for an export when it does not load the full
 * `styles.css` (docs/guides/getting-started.md §2 and the `styles/subpath-css`
 * conformance rule): the tokens, core, and the entry's own sheet for a subpath.
 */
export function cssOf(entry: string): string[] {
  const sheets = [TOKENS_CSS, `${PACKAGE}/styles/core.css`];
  if (entry !== '.') sheets.push(`${PACKAGE}/styles/${entry.replace(/^\.\//, '')}.css`);
  return sheets;
}

/**
 * The entry an export is listed under, given every entry that ships it in
 * source order: the first alias entry (`./rook`) when there is one, else the
 * first entry. The others follow in `alsoEntries`, in their original order.
 */
export function preferredEntry(
  entries: readonly string[],
  aliases: readonly string[],
): { entry: string; alsoEntries?: string[] } {
  const first = entries[0];
  if (first === undefined) throw new Error('an export needs at least one entry');
  const entry = entries.find((e) => aliases.includes(e)) ?? first;
  const rest = entries.filter((e) => e !== entry);
  return rest.length ? { entry, alsoEntries: rest } : { entry };
}

/** The per-component stylesheet, the smallest option, when the folder has one. */
export function componentCssOf(folderId: string): string {
  return `${PACKAGE}/styles/components/${folderId}.css`;
}

export function docsUrlOf(folderId: string): string {
  return `${DOCS_SITE}/components/${folderId}/`;
}

/** `SelectTrigger` → `select-trigger`. */
export function kebab(name: string): string {
  return name
    .replace(/([a-z0-9])([A-Z])/g, '$1-$2')
    .replace(/([A-Z])([A-Z][a-z])/g, '$1-$2')
    .toLowerCase();
}

const CLASS_NAME = /^mn-[a-z0-9]+(?:(?:-|__|--)[a-z0-9]+)*$/;

/**
 * Picks the root class of an export from the `mn-*` literals on its root
 * element, keeping only classes that a stylesheet really selects:
 * `mn-<kebab name>` first, then a class whose block names part of the export
 * (`mn-select` for SelectTrigger), then the most specific (last) literal.
 */
export function pickRootClass(
  exportName: string,
  candidates: readonly string[],
  cssClasses: ReadonlySet<string>,
  /** Every `mn-*` literal in the component's function: `mn-<kebab name>` wins from here too. */
  anywhere: readonly string[] = [],
): string | undefined {
  const k = kebab(exportName);
  const named = `mn-${k}`;
  if ([...candidates, ...anywhere].includes(named) && cssClasses.has(named)) return named;
  const valid = [...new Set(candidates)].filter((c) => CLASS_NAME.test(c) && cssClasses.has(c));
  if (valid.length === 0) return undefined;
  const words = new Set(k.split('-'));
  const related = valid.filter((c) => {
    const block = c.slice(3).split('__')[0] ?? '';
    return block.split('-').every((w) => words.has(w));
  });
  if (related.length) return related[related.length - 1];
  return valid[valid.length - 1];
}

/** Every class a stylesheet selects (`.mn-button`, `.mn-button__icon`). */
export function cssClassesOf(css: string): Set<string> {
  const out = new Set<string>();
  for (const m of css.matchAll(/\.(mn-[a-z0-9_-]+)/gi)) out.add((m[1] as string).toLowerCase());
  return out;
}

/** `Default \`secondary\`` / `Defaults to \`md\`` in a description. */
export function defaultFromDescription(description: string): string | undefined {
  const m = /\bDefaults?(?: to| is)?:? `([^`]+)`/.exec(description);
  return m?.[1];
}

/** Normalises a default to how it is written in code: `'md'`, `4`, `true`. */
export function normaliseDefault(text: string, values?: readonly (string | number)[]): string {
  const t = text.trim();
  if (/^".*"$/.test(t)) return `'${t.slice(1, -1)}'`;
  // Radix writes `@defaultValue automatic`: a bare member of a string union.
  if (values?.includes(t) && !/^'.*'$/.test(t)) return `'${t}'`;
  return t;
}

/**
 * A doc comment's indented example (two or more spaces, after a blank line) as
 * a fenced code block, so Markdown renders it as code, not a paragraph.
 */
export function fenceIndentedCode(text: string): string {
  const lines = text.split('\n');
  const out: string[] = [];
  let i = 0;
  while (i < lines.length) {
    const line = lines[i] as string;
    const prevBlank = i === 0 || (lines[i - 1] as string).trim() === '';
    if (prevBlank && /^ {2,}\S/.test(line)) {
      const block: string[] = [];
      while (
        i < lines.length &&
        (/^ {2,}/.test(lines[i] as string) || (lines[i] as string) === '')
      ) {
        block.push(lines[i] as string);
        i++;
      }
      while (block.length && (block[block.length - 1] as string).trim() === '') block.pop();
      const indent = Math.min(
        ...block.filter((l) => l.trim()).map((l) => l.length - l.trimStart().length),
      );
      out.push('```tsx', ...block.map((l) => l.slice(indent)), '```');
      if (i < lines.length) out.push('');
      continue;
    }
    out.push(line);
    i++;
  }
  return out.join('\n');
}

/**
 * TypeScript prints a type it cannot name in scope as `import("<absolute path>").X`.
 * The path is the build machine's, so it is rewritten to the module an app
 * would import: `import("react")`, `import("@rookcom/monarch-react/ai")`.
 */
export function portableType(text: string): string {
  return text.replace(/import\("([^"]+)"\)/g, (whole, path: string) => {
    const dep = /\/node_modules\/((?:@[^/]+\/)?[^/]+)(?:\/.*)?$/.exec(path);
    if (dep?.[1]) return `import("${dep[1].replace(/^@types\//, '')}")`;
    const own = /\/packages\/react\/src\/(?:entries\/([a-z-]+)|index)$/.exec(path);
    if (own) return `import("${own[1] ? `${PACKAGE}/${own[1]}` : PACKAGE}")`;
    const src = /\/packages\/react\/src\/.*$/.exec(path);
    if (src) return `import("${PACKAGE}")`;
    return whole;
  });
}
