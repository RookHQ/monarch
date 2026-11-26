import { describe, expect, it } from 'vitest';
import {
  DESCRIPTION,
  budgetFor,
  compareBudgets,
  formatComparison,
  initialCap,
  kib,
  largest,
  passes,
  updateBudgets,
  type Measured,
} from './size-budget.js';

const measured: Measured = {
  method: 'esbuild-minify-gzip',
  entries: { '.': 100_000, './ai': 50_000 },
  components: { button: 1000, dialog: 2001 },
};

describe('size budgets', () => {
  it('allows ten per cent of growth, rounded up', () => {
    expect(budgetFor(1000)).toBe(1100);
    expect(budgetFor(2001)).toBe(2202);
    expect(budgetFor(0)).toBe(0);
  });

  it('sets the first hard cap a quarter above the root entry, on a whole KiB', () => {
    expect(initialCap(100_000)).toBe(125_952);
    expect(initialCap(100_000) % 1024).toBe(0);
  });

  it('writes sorted rows with size and budget', () => {
    const b = updateBudgets({ ...measured, components: { dialog: 2001, button: 1000 } });
    expect(b.$description).toBe(DESCRIPTION);
    expect(b.method).toBe('esbuild-minify-gzip');
    expect(Object.keys(b.components)).toEqual(['button', 'dialog']);
    expect(b.components.button).toEqual({ gzip: 1000, budget: 1100 });
    expect(b.entries['./ai']).toEqual({ gzip: 50_000, budget: 55_000 });
    expect(b.coreHardCap).toBe(125_952);
  });

  it('never raises an existing hard cap on update', () => {
    const first = updateBudgets(measured);
    const grown = updateBudgets({ ...measured, entries: { '.': 200_000 } }, first);
    expect(grown.coreHardCap).toBe(first.coreHardCap);
    expect(grown.entries['.']?.gzip).toBe(200_000);
  });

  it('passes at the budget and fails one byte over', () => {
    const budgets = updateBudgets(measured);
    const at = { ...measured, components: { button: 1100, dialog: 2001 } };
    expect(passes(compareBudgets(at, budgets))).toBe(true);
    const over = compareBudgets(
      { ...measured, components: { button: 1101, dialog: 2001 } },
      budgets,
    );
    expect(passes(over)).toBe(false);
    expect(over.exceeded).toEqual([
      { kind: 'component', name: 'button', gzip: 1101, budget: 1100, over: 1 },
    ]);
  });

  it('lists the largest overshoot first, entries and components together', () => {
    const budgets = updateBudgets(measured);
    const c = compareBudgets(
      {
        ...measured,
        entries: { '.': 110_500, './ai': 50_000 },
        components: { button: 1200, dialog: 9000 },
      },
      budgets,
    );
    expect(c.exceeded.map((o) => o.name)).toEqual(['dialog', '.', 'button']);
    const text = formatComparison(c, 2);
    expect(text).toContain('3 over budget');
    expect(text.indexOf('dialog')).toBeLessThan(text.indexOf('entry'));
    expect(text).toContain('… 1 more');
    expect(text).toContain('--update');
  });

  it('fails on a component with no budget and on a budget with no component', () => {
    const budgets = updateBudgets(measured);
    const c = compareBudgets(
      { ...measured, components: { button: 1000, tabs: 10 }, entries: { '.': 100_000 } },
      budgets,
    );
    expect(c.missing).toEqual(['component tabs']);
    expect(c.stale).toEqual(['component dialog', 'entry ./ai']);
    expect(passes(c)).toBe(false);
    expect(formatComparison(c)).toContain('no budget yet (component tabs)');
  });

  it('holds the root entry to the hard cap even when its budget was raised', () => {
    const budgets = {
      ...updateBudgets({ ...measured, entries: { '.': 130_000 } }),
      coreHardCap: 125_952,
    };
    const c = compareBudgets({ ...measured, entries: { '.': 130_000 } }, budgets);
    expect(c.exceeded).toEqual([]);
    expect(c.capExceeded).toEqual({ gzip: 130_000, cap: 125_952 });
    expect(passes(c)).toBe(false);
    expect(formatComparison(c)).toContain('hard cap');
  });

  it('refuses to compare sizes measured in different ways', () => {
    const c = compareBudgets({ ...measured, method: 'gzip' }, updateBudgets(measured));
    expect(c.methodMismatch).toEqual({ recorded: 'esbuild-minify-gzip', current: 'gzip' });
    expect(passes(c)).toBe(false);
  });

  it('prints nothing when everything is within budget', () => {
    expect(formatComparison(compareBudgets(measured, updateBudgets(measured)))).toBe('');
  });

  it('formats sizes and picks the largest', () => {
    expect(kib(1536)).toBe('1.50 KiB');
    expect(largest({ a: 1, b: 3, c: 2 }, 2)).toEqual([
      ['b', 3],
      ['c', 2],
    ]);
  });
});
