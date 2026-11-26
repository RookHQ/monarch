/**
 * A small semver implementation: enough for npm-style ranges used in Rook
 * package.json files (^, ~, >=, >, <=, <, =, x-ranges, hyphen ranges, ||).
 */

export interface Version {
  readonly major: number;
  readonly minor: number;
  readonly patch: number;
  readonly prerelease: readonly string[];
}

type Op = '>=' | '>' | '<=' | '<' | '=';
interface Comparator {
  readonly op: Op;
  readonly v: Version;
}

const FULL = /^v?(\d+)\.(\d+)\.(\d+)(?:-([0-9A-Za-z.-]+))?(?:\+[0-9A-Za-z.-]+)?$/;
const PARTIAL = /^v?(\d+|[xX*])(?:\.(\d+|[xX*]))?(?:\.(\d+|[xX*]))?(?:-([0-9A-Za-z.-]+))?$/;

export function parseVersion(input: string): Version | null {
  const m = FULL.exec(input.trim());
  if (!m) return null;
  return {
    major: Number(m[1]),
    minor: Number(m[2]),
    patch: Number(m[3]),
    prerelease: m[4] ? m[4].split('.') : [],
  };
}

export function compare(a: Version, b: Version): number {
  for (const k of ['major', 'minor', 'patch'] as const) {
    if (a[k] !== b[k]) return a[k] < b[k] ? -1 : 1;
  }
  const pa = a.prerelease;
  const pb = b.prerelease;
  if (pa.length === 0 && pb.length === 0) return 0;
  if (pa.length === 0) return 1;
  if (pb.length === 0) return -1;
  for (let i = 0; i < Math.max(pa.length, pb.length); i++) {
    const x = pa[i];
    const y = pb[i];
    if (x === undefined) return -1;
    if (y === undefined) return 1;
    if (x === y) continue;
    const nx = /^\d+$/.test(x);
    const ny = /^\d+$/.test(y);
    if (nx && ny) return Number(x) < Number(y) ? -1 : 1;
    if (nx) return -1;
    if (ny) return 1;
    return x < y ? -1 : 1;
  }
  return 0;
}

const v = (major: number, minor = 0, patch = 0, prerelease: readonly string[] = []): Version => ({
  major,
  minor,
  patch,
  prerelease,
});

interface Partial3 {
  major: number | null;
  minor: number | null;
  patch: number | null;
  pre: readonly string[];
}

function parsePartial(s: string): Partial3 | null {
  const m = PARTIAL.exec(s.trim());
  if (!m) return null;
  const n = (x: string | undefined) => (x === undefined || /^[xX*]$/.test(x) ? null : Number(x));
  const major = n(m[1]);
  const minor = major === null ? null : n(m[2]);
  const patch = minor === null ? null : n(m[3]);
  return { major, minor, patch, pre: m[4] ? m[4].split('.') : [] };
}

/** Expands one space-separated comparator set into primitive comparators. */
function expand(token: string): Comparator[] | null {
  const m = /^(\^|~>?|>=|<=|>|<|=)?\s*(.*)$/.exec(token);
  if (!m) return null;
  const op = m[1] ?? '';
  const p = parsePartial(m[2] ?? '');
  if (!p) return null;
  const { major, minor, patch, pre } = p;
  if (major === null) {
    return op === '<' || op === '>' ? [{ op: '<', v: v(0, 0, 0, ['0']) }] : [];
  }
  const lower = v(major, minor ?? 0, patch ?? 0, pre);
  switch (op) {
    case '^': {
      let upper: Version;
      if (major > 0 || minor === null) upper = v(major + 1);
      else if (minor > 0 || patch === null) upper = v(0, minor + 1);
      else upper = v(0, 0, patch + 1);
      return [
        { op: '>=', v: lower },
        { op: '<', v: upper },
      ];
    }
    case '~':
    case '~>': {
      const upper = minor === null ? v(major + 1) : v(major, minor + 1);
      return [
        { op: '>=', v: lower },
        { op: '<', v: upper },
      ];
    }
    case '>=':
      return [{ op: '>=', v: lower }];
    case '<':
      return [{ op: '<', v: lower }];
    case '>':
      if (minor === null) return [{ op: '>=', v: v(major + 1) }];
      if (patch === null) return [{ op: '>=', v: v(major, minor + 1) }];
      return [{ op: '>', v: lower }];
    case '<=':
      if (minor === null) return [{ op: '<', v: v(major + 1) }];
      if (patch === null) return [{ op: '<', v: v(major, minor + 1) }];
      return [{ op: '<=', v: lower }];
    default: {
      if (minor === null)
        return [
          { op: '>=', v: lower },
          { op: '<', v: v(major + 1) },
        ];
      if (patch === null)
        return [
          { op: '>=', v: lower },
          { op: '<', v: v(major, minor + 1) },
        ];
      return [{ op: '=', v: lower }];
    }
  }
}

function parseSet(set: string): Comparator[] | null {
  const trimmed = set.trim();
  const hyphen = /^(\S+)\s+-\s+(\S+)$/.exec(trimmed);
  if (hyphen) {
    const lo = expand(`>=${hyphen[1]}`);
    const hiP = parsePartial(hyphen[2] ?? '');
    if (!lo || !hiP || hiP.major === null) return null;
    const hi =
      hiP.patch !== null
        ? expand(`<=${hyphen[2]}`)
        : expand(`<${hiP.minor === null ? hiP.major + 1 : `${hiP.major}.${hiP.minor + 1}`}`);
    return hi ? [...lo, ...hi] : null;
  }
  // Normalise ">= 1.2" into ">=1.2".
  const tokens = trimmed
    .replace(/(>=|<=|>|<|=|\^|~)\s+/g, '$1')
    .split(/\s+/)
    .filter(Boolean);
  const out: Comparator[] = [];
  for (const t of tokens) {
    const c = expand(t);
    if (!c) return null;
    out.push(...c);
  }
  return out;
}

export interface Range {
  readonly sets: readonly (readonly Comparator[])[];
}

export function parseRange(range: string): Range | null {
  const sets: Comparator[][] = [];
  for (const part of range.split('||')) {
    const set = parseSet(part);
    if (!set) return null;
    sets.push(set);
  }
  return { sets };
}

function test(c: Comparator, ver: Version): boolean {
  const r = compare(ver, c.v);
  switch (c.op) {
    case '>=':
      return r >= 0;
    case '>':
      return r > 0;
    case '<=':
      return r <= 0;
    case '<':
      return r < 0;
    case '=':
      return r === 0;
  }
}

export function satisfies(version: string | Version, range: string): boolean {
  const ver = typeof version === 'string' ? parseVersion(version) : version;
  const r = parseRange(range);
  if (!ver || !r) return false;
  return r.sets.some((set) => {
    if (!set.every((c) => test(c, ver))) return false;
    // A prerelease only matches a set that names the same major.minor.patch with a prerelease.
    if (ver.prerelease.length === 0) return true;
    return set.some(
      (c) =>
        c.v.prerelease.length > 0 &&
        c.v.major === ver.major &&
        c.v.minor === ver.minor &&
        c.v.patch === ver.patch,
    );
  });
}

/** Lowest version a range admits (what `npm install` could resolve at worst). */
export function minVersion(range: string): Version | null {
  const r = parseRange(range);
  if (!r) return null;
  let best: Version | null = null;
  for (const set of r.sets) {
    let lo: Version = v(0, 0, 0);
    for (const c of set) {
      if (c.op === '>=' || c.op === '=') {
        if (compare(c.v, lo) > 0) lo = c.v;
      } else if (c.op === '>') {
        const next = c.v.prerelease.length
          ? v(c.v.major, c.v.minor, c.v.patch)
          : v(c.v.major, c.v.minor, c.v.patch + 1);
        if (compare(next, lo) > 0) lo = next;
      }
    }
    if (!set.every((c) => test(c, lo))) continue;
    if (!best || compare(lo, best) < 0) best = lo;
  }
  return best;
}

export function format(ver: Version): string {
  return `${ver.major}.${ver.minor}.${ver.patch}${ver.prerelease.length ? '-' + ver.prerelease.join('.') : ''}`;
}
