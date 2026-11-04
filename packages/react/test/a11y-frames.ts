import axe from 'axe-core';
import { expect } from 'vitest';

/**
 * expectNoA11yViolations for markup that contains iframes: jsdom frames are
 * empty about:blank documents that axe cannot message, so the frame contents
 * are skipped (`iframes: false`). The host markup, including each iframe's
 * title, is still checked. Real frames are covered by the cross-browser axe run.
 */
export async function expectNoA11yViolationsWithFrames(root: Element = document.body) {
  const result = await axe.run(root, {
    iframes: false,
    rules: { 'color-contrast': { enabled: false }, region: { enabled: false } },
  });
  const summary = result.violations.map(
    (v) => `${v.id}: ${v.help} (${v.nodes.map((n) => n.target.join(' ')).join(', ')})`,
  );
  expect(summary).toEqual([]);
}
