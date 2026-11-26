import { readdirSync, readFileSync } from 'node:fs';
import { dirname, join, relative } from 'node:path';
import { fileURLToPath } from 'node:url';
import { describe, expect, it } from 'vitest';

/*
 * A box that scrolls sideways clips its content only for descendants whose containing block is
 * the box itself or something inside it. Visually hidden text (`.mn-visually-hidden`, an app's
 * `sr-only`) is `position: absolute`, so inside an unpositioned scroller it is laid out against
 * some ancestor further up, escapes the clip and makes the whole page scroll sideways on a phone.
 * Every Monarch rule that lets a box scroll horizontally (`overflow` or `overflow-x` set to `auto`
 * or `scroll`) therefore also makes that box positioned.
 */

const SRC = join(dirname(fileURLToPath(import.meta.url)), '..', 'src');

/** Scrollers that are positioned by another Monarch class on the same element. */
const POSITIONED_ELSEWHERE: Record<string, string> = {
  ".mn-ai-artifact-canvas[data-placement='sheet']": 'rendered on SheetContent (.mn-sheet is fixed)',
};

interface Rule {
  file: string;
  selectors: string[];
  body: string;
}

function cssFiles(dir: string): string[] {
  return readdirSync(dir, { withFileTypes: true }).flatMap((entry) => {
    const path = join(dir, entry.name);
    if (entry.isDirectory()) return cssFiles(path);
    return entry.name.endsWith('.css') ? [path] : [];
  });
}

/** Innermost `selector { declarations }` blocks; good enough for Monarch's flat stylesheets. */
function rules(file: string): Rule[] {
  const css = readFileSync(file, 'utf8').replace(/\/\*[\s\S]*?\*\//g, '');
  return [...css.matchAll(/([^{}]+)\{([^{}]*)\}/g)].map((m) => ({
    file: relative(SRC, file),
    selectors: m[1]!
      .split(',')
      .map((s) => s.trim().replace(/\s+/g, ' '))
      .filter(Boolean),
    body: m[2]!,
  }));
}

const all = cssFiles(SRC).flatMap(rules);
const scrollsSideways = (body: string) =>
  /(?:^|[;\s])overflow(?:-x)?\s*:\s*(?:auto|scroll)\b/.test(body);
const positioned = (body: string) =>
  /(?:^|[;\s])position\s*:\s*(?:relative|absolute|fixed|sticky)\b/.test(body);

describe('horizontal scroll containers', () => {
  it('finds the stylesheets', () => {
    expect(all.length).toBeGreaterThan(100);
  });

  it('are a containing block, so visually hidden text inside them is clipped', () => {
    const missing: string[] = [];
    for (const rule of all.filter((r) => scrollsSideways(r.body))) {
      for (const selector of rule.selectors) {
        if (selector in POSITIONED_ELSEWHERE) continue;
        const ok = all.some((r) => r.selectors.includes(selector) && positioned(r.body));
        if (!ok) missing.push(`${rule.file}: ${selector}`);
      }
    }
    expect(missing, 'add `position: relative` to these scroll containers').toEqual([]);
  });

  it('include the Table wrapper', () => {
    const wrap = all.find((r) => r.selectors.includes('.mn-table-wrap'));
    expect(wrap?.file).toBe(join('components', 'data-grid', 'data-table', 'data-table.css'));
    expect(wrap?.body).toMatch(/overflow\s*:\s*auto/);
    expect(wrap?.body).toMatch(/position\s*:\s*relative/);
  });
});
