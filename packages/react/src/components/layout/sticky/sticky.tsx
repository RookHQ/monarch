'use client';

import {
  forwardRef,
  useEffect,
  useRef,
  useState,
  type CSSProperties,
  type ForwardedRef,
  type HTMLAttributes,
} from 'react';
import { cx } from '../../../internal/cx.js';
import type { LayoutElement } from '../stack/index.js';

/** Root class, declared before any other `mn-` string so dist/manifest.json reads it as the root. */
const ROOT = 'mn-sticky';

export interface StickyProps extends HTMLAttributes<HTMLElement> {
  /** The element to render. Default `div`; use `header` or `footer` where it fits. */
  as?: LayoutElement | undefined;
  /** Stick to the top (default) or the bottom of the scroll area. */
  edge?: 'top' | 'bottom' | undefined;
  /** Distance from the edge while stuck: a CSS length or a number of pixels. Default 0. */
  offset?: string | number | undefined;
  /** Add a surface background and shadow while stuck. Default true. */
  elevateWhenStuck?: boolean | undefined;
  /** Called when the element becomes stuck or comes unstuck. */
  onStuckChange?: ((stuck: boolean) => void) | undefined;
}

/** The nearest ancestor that scrolls, or null for the viewport. */
function scrollParent(el: HTMLElement): HTMLElement | null {
  let node = el.parentElement;
  while (node && node !== document.body && node !== document.documentElement) {
    const { overflowY } = getComputedStyle(node);
    if (/(auto|scroll|overlay)/.test(overflowY)) return node;
    node = node.parentElement;
  }
  return null;
}

/**
 * Keeps its content pinned to the top or bottom edge while the page (or the
 * nearest scrolling ancestor) scrolls, and knows when it is stuck: it sets
 * `data-stuck` and calls `onStuckChange`, so a header can gain a shadow or
 * a compact layout only while pinned.
 */
export const Sticky = forwardRef<HTMLElement, StickyProps>(function Sticky(
  {
    as: Tag = 'div',
    edge = 'top',
    offset = 0,
    elevateWhenStuck = true,
    onStuckChange,
    className,
    style,
    ...rest
  },
  forwarded: ForwardedRef<HTMLElement>,
) {
  const ref = useRef<HTMLElement | null>(null);
  const sentinel = useRef<HTMLSpanElement | null>(null);
  const [stuck, setStuck] = useState(false);
  const onChange = useRef(onStuckChange);
  onChange.current = onStuckChange;
  const last = useRef(false);
  const offsetCss = typeof offset === 'number' ? `${offset}px` : offset;

  useEffect(() => {
    const el = ref.current;
    const mark = sentinel.current;
    if (!el || !mark || typeof IntersectionObserver !== 'function') return;
    const px = Number.parseFloat(getComputedStyle(el)[edge]) || 0;
    const root = scrollParent(el);
    const margin = edge === 'top' ? `${-(px + 1)}px 0px 0px 0px` : `0px 0px ${-(px + 1)}px 0px`;
    const observer = new IntersectionObserver(
      ([entry]) => {
        if (!entry) return;
        const bounds = entry.rootBounds;
        const rect = entry.boundingClientRect;
        // Stuck when the marker has scrolled past the edge, not when it is out of view the other way.
        const next =
          !entry.isIntersecting &&
          (bounds ? (edge === 'top' ? rect.top < bounds.top : rect.bottom > bounds.bottom) : true);
        if (next === last.current) return;
        last.current = next;
        setStuck(next);
        onChange.current?.(next);
      },
      { root, rootMargin: margin, threshold: [0] },
    );
    observer.observe(mark);
    return () => observer.disconnect();
  }, [edge, offsetCss]);

  const marker = (
    <span ref={sentinel} className="mn-sticky__sentinel" aria-hidden data-edge={edge} />
  );

  return (
    <>
      {edge === 'top' ? marker : null}
      <Tag
        ref={(node: HTMLElement | null) => {
          ref.current = node;
          if (typeof forwarded === 'function') forwarded(node);
          else if (forwarded) forwarded.current = node;
        }}
        {...rest}
        className={cx(ROOT, className)}
        style={{ '--mn-sticky-offset': offsetCss, ...style } as CSSProperties}
        data-edge={edge}
        data-stuck={stuck ? '' : undefined}
        data-elevate={elevateWhenStuck ? '' : undefined}
      />
      {edge === 'bottom' ? marker : null}
    </>
  );
});
