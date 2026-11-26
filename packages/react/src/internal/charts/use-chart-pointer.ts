import {
  useCallback,
  useState,
  type FocusEvent,
  type KeyboardEvent,
  type PointerEvent,
} from 'react';
import { useControllable } from './use-controllable.js';

export type ActiveSource = 'pointer' | 'keyboard';

export interface ChartPointerOptions {
  /** Number of inspectable positions (points or categories). */
  count: number;
  /** Controlled active index; `null` for none. Leave undefined for uncontrolled. */
  activeIndex?: number | null | undefined;
  onActiveIndexChange?: ((index: number | null) => void) | undefined;
  /** Maps a pointer position inside the plot (px from its left/top) to an index. */
  indexAt: (x: number, y: number) => number | null;
  /** Enter, Space or click on the active position. */
  onSelect?: ((index: number) => void) | undefined;
}

/**
 * Hover, touch and keyboard inspection shared by every chart. The plot is one
 * tab stop; arrow keys move between positions (Home/End, PageUp/PageDown in
 * tenths), Escape hides the tooltip. Pointer and keyboard drive the same
 * active index, so the tooltip and crosshair behave identically.
 */
export function useChartPointer({
  count,
  activeIndex,
  onActiveIndexChange,
  indexAt,
  onSelect,
}: ChartPointerOptions) {
  const [active, setActive] = useControllable<number | null>(
    activeIndex,
    null,
    onActiveIndexChange,
  );
  const [source, setSource] = useState<ActiveSource>('pointer');
  const clamp = (i: number) => Math.max(0, Math.min(count - 1, i));
  const current = active !== null && active >= 0 && active < count ? active : null;

  const onKeyDown = useCallback(
    (e: KeyboardEvent<HTMLElement>) => {
      if (count === 0) return;
      const page = Math.max(1, Math.ceil(count / 10));
      const from = current ?? -1;
      let next: number | null | undefined;
      switch (e.key) {
        case 'ArrowRight':
        case 'ArrowDown':
          next = current === null ? 0 : clamp(from + 1);
          break;
        case 'ArrowLeft':
        case 'ArrowUp':
          next = current === null ? count - 1 : clamp(from - 1);
          break;
        case 'Home':
          next = 0;
          break;
        case 'End':
          next = count - 1;
          break;
        case 'PageDown':
          next = clamp((current ?? 0) + page);
          break;
        case 'PageUp':
          next = clamp((current ?? 0) - page);
          break;
        case 'Escape':
          if (current === null) return;
          next = null;
          break;
        case 'Enter':
        case ' ':
          if (current !== null && onSelect) {
            e.preventDefault();
            onSelect(current);
          }
          return;
        default:
          return;
      }
      e.preventDefault();
      setSource('keyboard');
      setActive(next);
    },
    // clamp depends only on count
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [count, current, onSelect, setActive],
  );

  const fromPointer = useCallback(
    (e: PointerEvent<HTMLElement>) => {
      const rect = e.currentTarget.getBoundingClientRect();
      const i = indexAt(e.clientX - rect.left, e.clientY - rect.top);
      setSource('pointer');
      setActive(i === null ? null : clamp(i));
    },
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [indexAt, setActive, count],
  );

  const plotProps = {
    tabIndex: count > 0 ? 0 : -1,
    onKeyDown,
    onPointerMove: fromPointer,
    onPointerDown: fromPointer,
    onPointerLeave: (e: PointerEvent<HTMLElement>) => {
      // Touch keeps the last tapped position; a mouse leaving clears it
      // unless the keyboard is driving.
      if (e.pointerType === 'mouse' && source === 'pointer') setActive(null);
    },
    onFocus: (e: FocusEvent<HTMLElement>) => {
      if (e.target !== e.currentTarget) return;
      // Focus from the keyboard shows the first position at once; a click
      // already set the position from the pointer.
      if (current === null && e.currentTarget.matches(':focus-visible')) {
        setSource('keyboard');
        setActive(0);
      }
    },
    onBlur: (e: FocusEvent<HTMLElement>) => {
      if (e.currentTarget.contains(e.relatedTarget as Node | null)) return;
      setActive(null);
    },
    onClick: () => {
      if (current !== null && onSelect) onSelect(current);
    },
  };

  return { active: current, source, setActive, plotProps };
}
