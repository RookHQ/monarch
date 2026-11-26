import axe from 'axe-core';
import { expect } from 'vitest';

/**
 * Runs axe-core against rendered output. Colour contrast is excluded here
 * because jsdom has no layout or CSS; contrast is guaranteed by the token
 * tests in packages/tokens/test/contrast.test.ts instead.
 */
export async function expectNoA11yViolations(root: Element = document.body) {
  const result = await axe.run(root, {
    rules: { 'color-contrast': { enabled: false }, region: { enabled: false } },
  });
  const summary = result.violations.map(
    (v) => `${v.id}: ${v.help} (${v.nodes.map((n) => n.target.join(' ')).join(', ')})`,
  );
  expect(summary).toEqual([]);
}
