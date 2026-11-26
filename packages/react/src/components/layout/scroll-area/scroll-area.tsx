'use client';

import {
  forwardRef,
  useCallback,
  useEffect,
  useRef,
  useState,
  type CSSProperties,
  type ForwardedRef,
  type HTMLAttributes,
  type ReactNode,
} from 'react';
import { useIsomorphicLayoutEffect } from '../../../internal/layout-effect.js';
import { cx } from '../../../internal/cx.js';

/** Root class, declared before any other `mn-` string so dist/manifest.json reads it as the root. */
const ROOT = 'mn-scroll-area';

const useIsoLayoutEffect = typeof window === 'undefined' ? useEffect : useIsomorphicLayoutEffect;

export interface ScrollAreaProps extends Omit<HTMLAttributes<HTMLDivElement>, 'children'> {
  /**
   * Accessible name of the scrolling region, such as "Activity" or "Members".
   * Used while the content overflows, when the area becomes a focusable,
   * labelled region so keyboard users can scroll it.
   */
  label: string;
  /** Which way the content scrolls. Default `vertical`. */
  orientation?: 'vertical' | 'horizontal' | 'both' | undefined;
  /** Fixed height: a CSS length or a number of pixels. */
  height?: string | number | undefined;
  /** Maximum height before it scrolls: a CSS length or a number of pixels. */
  maxHeight?: string | number | undefined;
  /** Fade the edges where more content is hidden. Default true. */
  shadows?: boolean | undefined;
  /** Called when the content starts or stops overflowing. */
  onScrollableChange?: ((scrollable: boolean) => void) | undefined;
  /** Props for the scrolling element (the viewport), such as `onScroll`. */
  viewportProps?: HTMLAttributes<HTMLDivElement> | undefined;
  children?: ReactNode;
}

const length = (v: string | number | undefined) => (typeof v === 'number' ? `${v}px` : v);

interface Overflow {
  scrollable: boolean;
  start: boolean;
  end: boolean;
}

const same = (a: Overflow, b: Overflow) =>
  a.scrollable === b.scrollable && a.start === b.start && a.end === b.end;

/**
 * A scroll container with thin, themed scrollbars. It is a labelled,
 * keyboard-focusable region only while its content actually overflows, so it
 * never adds an empty tab stop. The ref points to the scrolling element.
 */
export const ScrollArea = forwardRef<HTMLDivElement, ScrollAreaProps>(function ScrollArea(
  {
    label,
    orientation = 'vertical',
    height,
    maxHeight,
    shadows = true,
    onScrollableChange,
    viewportProps,
    className,
    style,
    children,
    ...rest
  },
  forwarded: ForwardedRef<HTMLDivElement>,
) {
  const viewport = useRef<HTMLDivElement | null>(null);
  const content = useRef<HTMLDivElement | null>(null);
  const [overflow, setOverflow] = useState<Overflow>({
    scrollable: false,
    start: false,
    end: false,
  });
  const current = useRef(overflow);
  const onChange = useRef(onScrollableChange);
  onChange.current = onScrollableChange;

  const measure = useCallback(() => {
    const el = viewport.current;
    if (!el) return;
    const vertical = orientation !== 'horizontal';
    const horizontal = orientation !== 'vertical';
    const overY = vertical && el.scrollHeight > el.clientHeight + 1;
    const overX = horizontal && el.scrollWidth > el.clientWidth + 1;
    // scrollLeft is negative in right-to-left layouts; distance from the start is its magnitude.
    const left = Math.abs(el.scrollLeft);
    const next: Overflow = {
      scrollable: overY || overX,
      start: (overY && el.scrollTop > 0) || (overX && left > 0),
      end:
        (overY && el.scrollTop + el.clientHeight < el.scrollHeight - 1) ||
        (overX && left + el.clientWidth < el.scrollWidth - 1),
    };
    const prev = current.current;
    if (same(prev, next)) return;
    current.current = next;
    setOverflow(next);
    if (prev.scrollable !== next.scrollable) onChange.current?.(next.scrollable);
  }, [orientation]);

  useIsoLayoutEffect(() => {
    measure();
  });

  useEffect(() => {
    const el = viewport.current;
    if (!el || typeof ResizeObserver !== 'function') return;
    const observer = new ResizeObserver(() => measure());
    observer.observe(el);
    if (content.current) observer.observe(content.current);
    return () => observer.disconnect();
  }, [measure]);

  const { onScroll, className: viewportClassName, ...viewportRest } = viewportProps ?? {};
  const { scrollable } = overflow;

  return (
    <div
      {...rest}
      className={cx(ROOT, className)}
      style={
        {
          ...(height !== undefined ? { '--mn-scroll-area-height': length(height) } : null),
          ...(maxHeight !== undefined
            ? { '--mn-scroll-area-max-height': length(maxHeight) }
            : null),
          ...style,
        } as CSSProperties
      }
      data-orientation={orientation}
      data-scrollable={scrollable ? '' : undefined}
      data-overflow-start={shadows && overflow.start ? '' : undefined}
      data-overflow-end={shadows && overflow.end ? '' : undefined}
    >
      <div
        {...viewportRest}
        ref={(node) => {
          viewport.current = node;
          if (typeof forwarded === 'function') forwarded(node);
          else if (forwarded) forwarded.current = node;
        }}
        className={cx('mn-scroll-area__viewport', viewportClassName)}
        role={scrollable ? 'region' : undefined}
        aria-label={scrollable ? label : undefined}
        // Focusable only while it scrolls, so keyboard users can scroll it (axe scrollable-region-focusable).
        // eslint-disable-next-line jsx-a11y/no-noninteractive-tabindex
        tabIndex={scrollable ? 0 : undefined}
        onScroll={(e) => {
          onScroll?.(e);
          measure();
        }}
      >
        <div ref={content} className="mn-scroll-area__content">
          {children}
        </div>
      </div>
    </div>
  );
});
