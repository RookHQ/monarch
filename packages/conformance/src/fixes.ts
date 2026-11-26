/**
 * Machine-applicable fixes: how findings carry them, and `monarch check --fix`,
 * which writes them. Only fixes whose rewrite is unambiguous are offered:
 *
 *   imports/package-path        a deep `dist/` import → the public subpath (safe)
 *   styles/subpath-css          the missing stylesheet import, next to the other
 *                               Monarch CSS imports (safe)
 *   color/no-hard-coded-color   a hex that equals exactly one semantic token →
 *                               `var(--mn-color-…)`; safe only when the token has
 *                               that value in every theme, else review
 *   api/deprecated              the replacement name (review: imports may change)
 */
import { readFileSync, writeFileSync } from 'node:fs';
import { join } from 'node:path';
import { COLOUR_TOKENS, type ColourToken } from './bundled-data.js';
import type { Finding, Fix, TextEdit } from './types.js';

/** A fix as a rule produces it, before the finding knows its file. */
export interface RawFix {
  readonly description: string;
  readonly safety: Fix['safety'];
  readonly edits: readonly Omit<TextEdit, 'file'>[];
}

export const withFile = (fix: RawFix, file: string): Fix => ({
  ...fix,
  edits: fix.edits.map((e) => ({ ...e, file })),
});

/** `#FFF` → `#ffffff`; alpha forms are not tokens. */
export function normaliseHex(hex: string): string | null {
  const h = hex.replace(/^#/, '').toLowerCase();
  if (/^[0-9a-f]{6}$/.test(h)) return `#${h}`;
  if (/^[0-9a-f]{3}$/.test(h))
    return `#${h
      .split('')
      .map((c) => c + c)
      .join('')}`;
  return null;
}

export function colourTokenOf(
  hex: string,
  tokens: Readonly<Record<string, ColourToken>> = COLOUR_TOKENS,
): ColourToken | undefined {
  const key = normaliseHex(hex);
  return key ? tokens[key] : undefined;
}

/** The fix for a colour literal `#hex` found at line/column, when a token matches exactly. */
export function colourFix(hex: string, line: number, column: number): RawFix | undefined {
  const token = colourTokenOf(hex);
  if (!token) return undefined;
  return {
    description: token.invariant
      ? `Use var(${token.variable}), which has this value in every theme`
      : `Use var(${token.variable}), which has this value in the light theme and adapts in dark and more-contrast themes`,
    safety: token.invariant ? 'safe' : 'review',
    edits: [{ line, column, length: hex.length + 1, text: `var(${token.variable})` }],
  };
}

/** Deep imports into a package's dist/ folder and the public subpath that replaces each. */
const PACKAGE_PATHS: readonly [RegExp, string][] = [
  [/^@rookcom\/monarch-tokens\/dist\/css\/monarch\.css$/, '@rookcom/monarch-tokens/css'],
  [/^@rookcom\/monarch-tokens\/dist\/css\/(base|compat)\.css$/, '@rookcom/monarch-tokens/css/$1'],
  [
    /^@rookcom\/monarch-tokens\/dist\/css\/products\/([a-z0-9-]+)\.css$/,
    '@rookcom/monarch-tokens/css/products/$1',
  ],
  [
    /^@rookcom\/monarch-tokens\/dist\/(tokens|contrast-report)\.json$/,
    '@rookcom/monarch-tokens/$1.json',
  ],
  [/^@rookcom\/monarch-tokens\/dist\/index\.js$/, '@rookcom/monarch-tokens'],
  [/^@rookcom\/monarch-react\/dist\/styles\.css$/, '@rookcom/monarch-react/styles.css'],
  [
    /^@rookcom\/monarch-react\/dist\/styles\/((?:components\/)?[a-z0-9-]+)\.css$/,
    '@rookcom/monarch-react/styles/$1.css',
  ],
  [/^@rookcom\/monarch-react\/dist\/entries\/([a-z-]+)(?:\.js)?$/, '@rookcom/monarch-react/$1'],
  [/^@rookcom\/monarch-react\/dist\/index(?:\.js)?$/, '@rookcom/monarch-react'],
  [/^@rookcom\/monarch-react\/dist\/manifest\.json$/, '@rookcom/monarch-react/manifest.json'],
];

/** The public specifier for a deep `dist/` import, or undefined when it is not one. */
export function publicPathOf(specifier: string): string | undefined {
  for (const [re, to] of PACKAGE_PATHS) if (re.test(specifier)) return specifier.replace(re, to);
  return undefined;
}

/** The import statement that loads a stylesheet, in the syntax of the file it goes into. */
export function styleImportLine(file: string, sheet: string): string {
  return /\.s?css$/.test(file) ? `@import '${sheet}';` : `import '${sheet}';`;
}

/** Applies edits to one file's text. Edits are applied bottom-up so positions stay valid. */
export function applyEdits(text: string, edits: readonly TextEdit[]): string {
  const eol = text.includes('\r\n') ? '\r\n' : '\n';
  const lines = text.split(/\r?\n/);
  const unique = [...new Map(edits.map((e) => [JSON.stringify(e), e])).values()];
  const ordered = unique.sort((a, b) => b.line - a.line || b.column - a.column);
  for (const e of ordered) {
    const index = e.line - 1;
    if (index >= lines.length) {
      // An insertion after the last line.
      if (lines[lines.length - 1] === '')
        lines.splice(lines.length - 1, 0, e.text.replace(/\n$/, ''));
      else lines.push(e.text.replace(/\n$/, ''));
      continue;
    }
    const line = lines[index] ?? '';
    const next = line.slice(0, e.column - 1) + e.text + line.slice(e.column - 1 + e.length);
    lines.splice(index, 1, ...next.split('\n'));
  }
  return lines.join(eol);
}

export interface FixSummary {
  /** Files written, relative to the root. */
  readonly files: readonly string[];
  readonly applied: number;
  /** Fixes left alone because they need review (without `all`). */
  readonly skipped: number;
}

/** Writes the fixes of these findings. `all` also applies fixes that need review. */
export function applyFixes(
  root: string,
  findings: readonly Finding[],
  opts: { readonly all?: boolean } = {},
): FixSummary {
  const byFile = new Map<string, TextEdit[]>();
  let applied = 0;
  let skipped = 0;
  for (const f of findings) {
    if (!f.fix) continue;
    if (f.fix.safety !== 'safe' && !opts.all) {
      skipped++;
      continue;
    }
    applied++;
    for (const e of f.fix.edits) byFile.set(e.file, [...(byFile.get(e.file) ?? []), e]);
  }
  for (const [file, edits] of byFile) {
    const path = join(root, file);
    writeFileSync(path, applyEdits(readFileSync(path, 'utf8'), edits));
  }
  return { files: [...byFile.keys()].sort(), applied, skipped };
}
