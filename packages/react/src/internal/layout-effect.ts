import { useEffect, useLayoutEffect } from 'react';

/**
 * `useLayoutEffect` in the browser, `useEffect` where there is no DOM.
 *
 * Layout effects measure or position before paint, so they must stay layout
 * effects in the browser. On the server neither kind runs, but React 18 warns
 * ("useLayoutEffect does nothing on the server") on every server render of a
 * component that calls `useLayoutEffect`; React 19 does not. Use this instead
 * of `useLayoutEffect` in every component that can be server-rendered.
 */
export const useIsomorphicLayoutEffect: typeof useLayoutEffect =
  typeof document !== 'undefined' ? useLayoutEffect : useEffect;
