/**
 * Bundle-size budgets (registry/size-budgets.json): the pure part, shared by
 * scripts/size/check.ts, which measures dist, and manifest.ts, which copies
 * each component's size into the manifest. Sizes are bytes of minified,
 * gzipped ESM with every package import left external, so a number is the
 * cost of Monarch's own code for that entry or component.
 *
 * See docs/governance/performance-budgets.md.
 */

/** How much a size may grow before the check fails: ten per cent, in whole-number arithmetic. */
export const HEADROOM_PER_CENT = 110;
/** Headroom of the hard cap over the root entry's size when the cap is first set, per cent. */
export const CAP_HEADROOM_PER_CENT = 125;

export type SizeMethod = 'esbuild-minify-gzip' | 'gzip';

export interface SizeRow {
  /** Bytes when the budgets were last updated. */
  readonly gzip: number;
  /** The most it may be: `gzip` × 1.10, rounded up. */
  readonly budget: number;
}

export interface SizeBudgets {
  readonly $description: string;
  readonly method: SizeMethod;
  /**
   * The absolute ceiling of the root entry (`.`), in bytes. `--update` never
   * raises it: raising it is a decision, made by editing this number.
   */
  readonly coreHardCap: number;
  /** Package entry (`.`, `./ai`) → size. */
  readonly entries: Readonly<Record<string, SizeRow>>;
  /** Component folder id (`button`) → size. */
  readonly components: Readonly<Record<string, SizeRow>>;
}

export interface Measured {
  readonly method: SizeMethod;
  readonly entries: Readonly<Record<string, number>>;
  readonly components: Readonly<Record<string, number>>;
}

export const DESCRIPTION =
  'Bundle-size budgets of @rookcom/monarch-react, written by `npm run size:check -- --update`. Bytes of minified, gzipped ESM with package imports external. `gzip` is the size when last updated; `budget` is gzip × 1.10 rounded up; `coreHardCap` is the ceiling of the root entry and is only changed by hand. `npm run size:check` fails when an entry or component is over its budget. See docs/governance/performance-budgets.md.';

export const budgetFor = (gzip: number): number => Math.ceil((gzip * HEADROOM_PER_CENT) / 100);

/** The first hard cap: the root entry × 1.25, rounded up to a whole KiB. */
export const initialCap = (core: number): number =>
  Math.ceil((core * CAP_HEADROOM_PER_CENT) / 100 / 1024) * 1024;

const sorted = <T>(record: Readonly<Record<string, T>>): Record<string, T> =>
  Object.fromEntries(Object.entries(record).sort(([a], [b]) => a.localeCompare(b)));

const rows = (sizes: Readonly<Record<string, number>>): Record<string, SizeRow> =>
  sorted(
    Object.fromEntries(
      Object.entries(sizes).map(([k, gzip]) => [k, { gzip, budget: budgetFor(gzip) }]),
    ),
  );

/** New budgets from a measurement. An existing hard cap is kept. */
export function updateBudgets(measured: Measured, previous?: SizeBudgets): SizeBudgets {
  return {
    $description: DESCRIPTION,
    method: measured.method,
    coreHardCap: previous?.coreHardCap ?? initialCap(measured.entries['.'] ?? 0),
    entries: rows(measured.entries),
    components: rows(measured.components),
  };
}

export interface Offender {
  readonly kind: 'entry' | 'component';
  readonly name: string;
  readonly gzip: number;
  readonly budget: number;
  /** Bytes over the budget. */
  readonly over: number;
}

export interface Comparison {
  /** Over budget, largest overshoot first. */
  readonly exceeded: readonly Offender[];
  /** Measured, but with no budget yet. */
  readonly missing: readonly string[];
  /** Budgeted, but no longer built. */
  readonly stale: readonly string[];
  /** Set when the root entry is over the hard cap. */
  readonly capExceeded?: { readonly gzip: number; readonly cap: number };
  /** Set when the budgets were recorded with another method. */
  readonly methodMismatch?: { readonly recorded: SizeMethod; readonly current: SizeMethod };
}

export function compareBudgets(measured: Measured, budgets: SizeBudgets): Comparison {
  const exceeded: Offender[] = [];
  const missing: string[] = [];
  const stale: string[] = [];
  const groups = [
    ['entry', measured.entries, budgets.entries],
    ['component', measured.components, budgets.components],
  ] as const;
  for (const [kind, sizes, recorded] of groups) {
    for (const [name, gzip] of Object.entries(sizes)) {
      const row = recorded[name];
      if (!row) missing.push(`${kind} ${name}`);
      else if (gzip > row.budget)
        exceeded.push({ kind, name, gzip, budget: row.budget, over: gzip - row.budget });
    }
    for (const name of Object.keys(recorded)) if (!(name in sizes)) stale.push(`${kind} ${name}`);
  }
  exceeded.sort((a, b) => b.over - a.over || a.name.localeCompare(b.name));
  const core = measured.entries['.'] ?? 0;
  return {
    exceeded,
    missing: missing.sort(),
    stale: stale.sort(),
    ...(core > budgets.coreHardCap
      ? { capExceeded: { gzip: core, cap: budgets.coreHardCap } }
      : {}),
    ...(measured.method !== budgets.method
      ? { methodMismatch: { recorded: budgets.method, current: measured.method } }
      : {}),
  };
}

export const passes = (c: Comparison): boolean =>
  c.exceeded.length === 0 &&
  c.missing.length === 0 &&
  c.stale.length === 0 &&
  !c.capExceeded &&
  !c.methodMismatch;

export const kib = (bytes: number): string => `${(bytes / 1024).toFixed(2)} KiB`;

/** The check's output: what failed and the largest overshoots first. */
export function formatComparison(c: Comparison, top = 10): string {
  const out: string[] = [];
  if (c.methodMismatch)
    out.push(
      `Budgets were recorded with "${c.methodMismatch.recorded}" but this run measures with "${c.methodMismatch.current}"; sizes are not comparable. Run with --update on a machine that has esbuild.`,
    );
  if (c.capExceeded)
    out.push(
      `The root entry is ${kib(c.capExceeded.gzip)}, over its hard cap of ${kib(c.capExceeded.cap)}. Move code to a subpath entry, or raise coreHardCap by hand with a reason.`,
    );
  if (c.exceeded.length) {
    out.push(`${c.exceeded.length} over budget. Largest overshoot first:`);
    for (const o of c.exceeded.slice(0, top))
      out.push(
        `  ${o.kind.padEnd(9)} ${o.name.padEnd(32)} ${kib(o.gzip).padStart(11)}  budget ${kib(o.budget).padStart(11)}  +${o.over} B`,
      );
    if (c.exceeded.length > top) out.push(`  … ${c.exceeded.length - top} more`);
  }
  if (c.missing.length)
    out.push(
      `${c.missing.length} with no budget yet (${c.missing.slice(0, 5).join(', ')}${c.missing.length > 5 ? ', …' : ''}).`,
    );
  if (c.stale.length)
    out.push(
      `${c.stale.length} budgeted but no longer built (${c.stale.slice(0, 5).join(', ')}${c.stale.length > 5 ? ', …' : ''}).`,
    );
  if (out.length)
    out.push(
      'If the growth is intended, run `npm run size:check -- --update` and commit the file.',
    );
  return out.join('\n');
}

/** The largest rows, for the summary line and the docs. */
export function largest(
  sizes: Readonly<Record<string, number>>,
  count: number,
): [name: string, gzip: number][] {
  return Object.entries(sizes)
    .sort(([a, x], [b, y]) => y - x || a.localeCompare(b))
    .slice(0, count);
}
