import type { MockInstance } from 'vitest';

/**
 * React 18 warns that `useLayoutEffect` "does nothing on the server" whenever a
 * server render sees one. Components use `useIsomorphicLayoutEffect`, which is a
 * plain effect where there is no DOM, but these tests server-render inside jsdom,
 * where `document` exists, so React 18 still prints it. It is never a mismatch.
 */
const REACT_18_LAYOUT_EFFECT_ON_SERVER = /useLayoutEffect does nothing on the server/;

/** The console errors a hydration test must fail on: everything but that React 18 notice. */
export function hydrationErrors(spy: MockInstance): unknown[][] {
  return spy.mock.calls.filter(
    (args) => !args.some((a) => typeof a === 'string' && REACT_18_LAYOUT_EFFECT_ON_SERVER.test(a)),
  );
}
