import { useCallback, useEffect, useRef, useState } from 'react';
import { useIsomorphicLayoutEffect } from './layout-effect.js';

// useIsomorphicLayoutEffect warns during server rendering; measuring only matters in the browser.
const useIsoLayoutEffect = typeof window === 'undefined' ? useEffect : useIsomorphicLayoutEffect;

/**
 * Which items fit in a row (the priority+ pattern). Each visible item costs
 * its width plus the gap after it; when not everything fits, room is kept
 * for the More control. `pinned` (the current item) is always kept visible.
 * Items keep their order and are cut at the first one that does not fit.
 */
export function fitItems(
  widths: readonly number[],
  available: number,
  moreWidth: number,
  gap: number,
  pinned = -1,
): number[] {
  const all = widths.map((_, i) => i);
  const total = widths.reduce((sum, w) => sum + w, 0) + gap * Math.max(0, widths.length - 1);
  if (total <= available) return all;
  let budget = available - moreWidth;
  if (pinned >= 0 && pinned < widths.length) budget -= (widths[pinned] ?? 0) + gap;
  const visible: number[] = [];
  for (const i of all) {
    if (i === pinned) continue;
    const cost = (widths[i] ?? 0) + gap;
    if (cost > budget) break;
    budget -= cost;
    visible.push(i);
  }
  if (pinned >= 0 && pinned < widths.length) visible.push(pinned);
  return visible.sort((a, b) => a - b);
}

const same = (a: readonly number[] | null, b: readonly number[]) =>
  !!a && a.length === b.length && a.every((v, i) => v === b[i]);

/**
 * Measures a row of items and returns the indices that fit. `measureRef`
 * holds one hidden clone per item followed by a clone of the More control;
 * `containerRef` is the element whose width is available. Returns null until
 * measured (and on the server), meaning "show everything".
 */
export function useOverflowItems(count: number, pinned: number, signature: string) {
  const containerRef = useRef<HTMLDivElement>(null);
  const measureRef = useRef<HTMLDivElement>(null);
  const [visible, setVisible] = useState<number[] | null>(null);

  const compute = useCallback(() => {
    const container = containerRef.current;
    const measure = measureRef.current;
    if (!container || !measure) return;
    const available = container.clientWidth;
    if (!available) {
      setVisible(null);
      return;
    }
    const kids = Array.from(measure.children) as HTMLElement[];
    const widths = kids.slice(0, count).map((k) => k.getBoundingClientRect().width);
    const moreWidth = kids[count]?.getBoundingClientRect().width ?? 0;
    const style = getComputedStyle(measure);
    const gap = Number.parseFloat(style.columnGap || style.gap || '0') || 0;
    const next = fitItems(widths, available, moreWidth, gap, pinned);
    setVisible((prev) => (same(prev, next) ? prev : next));
    // signature: re-measure when labels change.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [count, pinned, signature]);

  useIsoLayoutEffect(() => {
    compute();
    if (typeof ResizeObserver === 'undefined') return;
    const observer = new ResizeObserver(() => compute());
    if (containerRef.current) observer.observe(containerRef.current);
    if (measureRef.current) observer.observe(measureRef.current);
    return () => observer.disconnect();
  }, [compute]);

  return { containerRef, measureRef, visible };
}
