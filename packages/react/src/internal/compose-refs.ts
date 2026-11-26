import { useCallback, type ForwardedRef, type MutableRefObject } from 'react';

/**
 * One stable callback ref that fills a component's own ref and the ref it was
 * given.
 *
 * Writing the two assignments inline (`ref={(node) => { … }}`) makes a new
 * function on every render, and React then calls the old one with `null` and
 * the new one with the element after each render: a callback ref passed by the
 * app is told the element went away and came back every time, and one that
 * sets state loops.
 */
export function useComposedRef<T>(
  inner: MutableRefObject<T | null>,
  forwarded: ForwardedRef<T>,
): (node: T | null) => void {
  return useCallback(
    (node: T | null) => {
      inner.current = node;
      if (typeof forwarded === 'function') forwarded(node);
      else if (forwarded) forwarded.current = node;
    },
    [inner, forwarded],
  );
}
