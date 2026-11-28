/**
 * Exports that moved to another entry of the same package (`RookRail` from
 * `/shell` to `/rook`). The old path keeps working for a deprecation period;
 * `api/deprecated` (the CLI) and `monarch/no-deprecated-api` (the ESLint
 * plugin) both use this module to find such imports and to write the fix.
 *
 * The fix only changes where a name is imported from, never the name, so the
 * rest of the file is untouched: it is safe.
 */
import type { RawFix } from './fixes.js';
import type { ImportPathDeprecation } from './types.js';

/** One name of an import list: `type Foo as Bar`. */
export interface ImportedName {
  /** The exported name, `Foo`. */
  readonly name: string;
  /** The specifier as written, `type Foo as Bar`. */
  readonly text: string;
}

/** True when `name` is one of the exports the deprecation moved. */
export function isMovedName(d: ImportPathDeprecation, name: string): boolean {
  if (d.names.includes(name)) return true;
  if (!d.pattern) return false;
  try {
    return new RegExp(d.pattern).test(name);
  } catch {
    return false; // an unreadable pattern in a registry file never matches
  }
}

/** The deprecation that covers an import of `names` from `source`, and the names it moved. */
export function movedImports(
  deprecations: readonly ImportPathDeprecation[],
  source: string,
  names: readonly ImportedName[],
): { deprecation: ImportPathDeprecation; moved: ImportedName[]; kept: ImportedName[] } | null {
  for (const deprecation of deprecations) {
    if (deprecation.from !== source) continue;
    const moved = names.filter((n) => isMovedName(deprecation, n.name));
    if (moved.length === 0) continue;
    return { deprecation, moved, kept: names.filter((n) => !moved.includes(n)) };
  }
  return null;
}

export function movedMessage(d: ImportPathDeprecation, moved: readonly ImportedName[]): string {
  const names = moved.map((n) => `\`${n.name}\``).join(', ');
  const verb = moved.length === 1 ? 'has' : 'have';
  const removal = d.removeNoEarlierThan
    ? `; ${d.from} stops exporting it no earlier than ${d.removeNoEarlierThan} (90-day window)`
    : '';
  return `${names} ${verb} moved to ${d.to} (since ${d.since})${removal}. Import from ${d.to}${d.message ? `. ${d.message}` : ''}`;
}

/** `import [type] { a, b } from 'x'` rebuilt for the kept and the moved names. */
export function splitImport(
  parts: {
    readonly keyword: string;
    readonly quote: string;
    readonly semicolon: string;
    readonly from: string;
    readonly to: string;
  },
  kept: readonly ImportedName[],
  moved: readonly ImportedName[],
): string {
  const line = (names: readonly ImportedName[], source: string) =>
    `${parts.keyword} { ${names.map((n) => n.text).join(', ')} } from ${parts.quote}${source}${parts.quote}${parts.semicolon}`;
  return kept.length
    ? `${line(kept, parts.from)}\n${line(moved, parts.to)}`
    : line(moved, parts.to);
}

/** Reads `a, type B, C as D` into names. */
export function readImportList(list: string): ImportedName[] {
  return list
    .split(',')
    .map((s) => s.trim().replace(/\s+/g, ' '))
    .filter(Boolean)
    .map((text) => ({
      text,
      name:
        text
          .replace(/^type\s+/, '')
          .split(/\s+as\s+/)[0]
          ?.trim() ?? '',
    }))
    .filter((n) => /^[\w$]+$/.test(n.name));
}

export interface ImportPathFinding {
  readonly line: number;
  readonly column: number;
  readonly message: string;
  readonly fix?: RawFix;
}

const NAMED_IMPORT_RE =
  /(^|[\n;])([ \t]*)((?:import|export)(?:\s+type)?)\s*\{([^}]*)\}\s*from\s*(['"])([^'"\n]+)\5[ \t]*(;?)/g;

function positionOf(text: string, index: number): { line: number; column: number } {
  const before = text.slice(0, index);
  return { line: before.split('\n').length, column: index - before.lastIndexOf('\n') };
}

/**
 * Imports (and re-exports) of moved names from their old path in one file.
 * One finding per statement. The fix changes the module when every name
 * moved, and splits a one-line statement in two when only some did; a mixed
 * statement over several lines is reported without a fix.
 */
export function scanImportPaths(
  text: string,
  deprecations: readonly ImportPathDeprecation[],
): ImportPathFinding[] {
  const out: ImportPathFinding[] = [];
  if (deprecations.length === 0) return out;
  for (const m of text.matchAll(NAMED_IMPORT_RE)) {
    const [whole, lead = '', indent = '', keyword = '', list = '', quote = '', source = '', semi] =
      m;
    const hit = movedImports(deprecations, source, readImportList(list));
    if (!hit) continue;
    const start = m.index + lead.length + indent.length;
    const statement = whole.slice(lead.length + indent.length);
    const at = positionOf(text, start);
    const sourceAt = positionOf(text, start + statement.lastIndexOf(source));
    const { deprecation, moved, kept } = hit;
    let fix: RawFix | undefined;
    if (kept.length === 0)
      fix = {
        description: `Import from ${deprecation.to}`,
        safety: 'safe',
        edits: [{ ...sourceAt, length: source.length, text: deprecation.to }],
      };
    else if (!statement.includes('\n'))
      fix = {
        description: `Import ${moved.map((n) => n.name).join(', ')} from ${deprecation.to}`,
        safety: 'safe',
        edits: [
          {
            ...at,
            length: statement.length,
            text: splitImport(
              {
                keyword: keyword.replace(/\s+/g, ' '),
                quote,
                semicolon: semi ?? '',
                from: source,
                to: deprecation.to,
              },
              kept,
              moved,
            ).replace(/\n/g, `\n${indent}`),
          },
        ],
      };
    out.push({ ...at, message: movedMessage(deprecation, moved), ...(fix ? { fix } : {}) });
  }
  return out;
}
