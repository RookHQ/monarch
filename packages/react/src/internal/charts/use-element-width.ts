import { useCallback, useEffect, useLayoutEffect, useRef, useState } from 'react';

const useIsoLayoutEffect = typeof window === 'undefined' ? useEffect : useLayoutEffect;

/**
 * The content width of an element, kept current with ResizeObserver.
 * Undefined on the server and in the first client render (so hydration
 * matches); charts lay out at a fallback width until it is known.
 */
export function useElementWidth<T extends HTMLElement>(): [
  (node: T | null) => void,
  number | undefined,
] {
  const [width, setWidth] = useState<number | undefined>(undefined);
  const nodeRef = useRef<T | null>(null);
  const observer = useRef<ResizeObserver | null>(null);

  const measure = useCallback((node: T) => {
    const w = node.getBoundingClientRect().width;
    if (w > 0) setWidth((prev) => (prev === undefined || Math.abs(prev - w) >= 0.5 ? w : prev));
  }, []);

  const ref = useCallback(
    (node: T | null) => {
      observer.current?.disconnect();
      observer.current = null;
      nodeRef.current = node;
      if (!node) return;
      if (typeof ResizeObserver !== 'undefined') {
        observer.current = new ResizeObserver(() => measure(node));
        observer.current.observe(node);
      }
    },
    [measure],
  );

  useIsoLayoutEffect(() => {
    if (nodeRef.current) measure(nodeRef.current);
    return () => observer.current?.disconnect();
  }, [measure]);

  return [ref, width];
}
