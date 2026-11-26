'use client';

import {
  forwardRef,
  useCallback,
  useEffect,
  useImperativeHandle,
  useRef,
  useState,
  type HTMLAttributes,
  type ReactElement,
  type ReactNode,
  type Ref,
  type RefAttributes,
} from 'react';
import { useIsomorphicLayoutEffect } from '../../../internal/layout-effect.js';
import { cx } from '../../../internal/cx.js';
import { Spinner } from '../../feedback/spinner/index.js';

// useIsomorphicLayoutEffect warns during server rendering; measuring only matters in the browser.
const useIsoLayoutEffect = typeof window === 'undefined' ? useEffect : useIsomorphicLayoutEffect;

/* ------------------------------------------------------------------ hook */

/** One rendered item of a virtualizer. */
export interface VirtualItem {
  /** Index in the full list. */
  index: number;
  /** Stable key (from `getItemKey`), also the measurement key. */
  key: string | number;
  /** Offset from the top of the list, in px. */
  start: number;
  /** Measured height, or the estimate until the item has been measured. */
  size: number;
}

export type VirtualAlign = 'start' | 'center' | 'end' | 'auto';

export interface UseVirtualizerOptions {
  /** Number of items in the full list. */
  count: number;
  /** The element that scrolls. Called after mount, so a ref's `current` is fine. */
  getScrollElement: () => HTMLElement | null;
  /** Height in px of an item not measured yet, or a function of its index. */
  estimateSize: number | ((index: number) => number);
  /** Stable key of an item. Measured heights follow the key, so a re-sorted list keeps them. Default: the index. */
  getItemKey?: ((index: number) => string | number) | undefined;
  /** Extra pixels rendered above and below the view. Default 320. */
  overscan?: number | undefined;
  /** Items rendered before the scroller has a height (server rendering, first paint). Default 20. */
  initialCount?: number | undefined;
  /** Pixels between the top of the scroller's content and the first item, e.g. a sticky header. Default 0. */
  scrollMargin?: number | undefined;
  /** Indexes rendered even when they are out of view, e.g. the row that holds focus. */
  keepMounted?: readonly number[] | undefined;
}

export interface Virtualizer {
  /** The items to render, in index order. Position each one at `start`. */
  items: VirtualItem[];
  /** Height of the whole list in px: give it to the element that holds the items. */
  totalSize: number;
  /** Ref callback for each rendered item element. The element needs `data-index`. */
  measureElement: (element: HTMLElement | null) => void;
  /** Scrolls so the item at `index` is visible. `auto` (default) scrolls only when it is not. */
  scrollToIndex: (
    index: number,
    options?: { align?: VirtualAlign | undefined; behavior?: ScrollBehavior | undefined },
  ) => void;
  /** Offset of an item from the top of the list, in px. */
  getOffset: (index: number) => number;
  /** Indexes of the first and last item inside the view (without overscan); -1 when empty. */
  visibleRange: { start: number; end: number };
}

/** Index of the last entry of sorted `offsets` that is ≤ `value`, clamped to the items. */
function findIndex(offsets: Float64Array, count: number, value: number): number {
  let lo = 0;
  let hi = Math.max(0, count - 1);
  while (lo < hi) {
    const mid = (lo + hi + 1) >> 1;
    if ((offsets[mid] ?? 0) <= value) lo = mid;
    else hi = mid - 1;
  }
  return lo;
}

/**
 * Windowing for long lists and grids: works out which items are near the
 * view from measured (or estimated) heights, so only those are rendered.
 * Heights are measured with ResizeObserver and kept by key. VirtualList and
 * DataGrid are built on it; use it directly for custom layouts.
 */
export function useVirtualizer({
  count,
  getScrollElement,
  estimateSize,
  getItemKey,
  overscan = 320,
  initialCount = 20,
  scrollMargin = 0,
  keepMounted,
}: UseVirtualizerOptions): Virtualizer {
  const sizes = useRef(new Map<string | number, number>());
  const [, setVersion] = useState(0);
  const [view, setView] = useState({ top: 0, height: 0 });
  const getScroll = useRef(getScrollElement);
  getScroll.current = getScrollElement;
  const keyOf = useCallback((i: number) => (getItemKey ? getItemKey(i) : i), [getItemKey]);
  const keyRef = useRef(keyOf);
  keyRef.current = keyOf;
  const estimate = (i: number) =>
    typeof estimateSize === 'number' ? estimateSize : estimateSize(i);

  // Offsets are rebuilt every render: O(count), cheap even for tens of thousands of rows.
  const offsets = new Float64Array(count + 1);
  for (let i = 0; i < count; i++) {
    offsets[i + 1] = (offsets[i] ?? 0) + (sizes.current.get(keyOf(i)) ?? estimate(i));
  }
  const totalSize = offsets[count] ?? 0;
  const offsetsRef = useRef(offsets);
  offsetsRef.current = offsets;

  const readView = useCallback(() => {
    const el = getScroll.current();
    if (!el) return;
    setView((v) =>
      v.top === el.scrollTop && v.height === el.clientHeight
        ? v
        : { top: el.scrollTop, height: el.clientHeight },
    );
  }, []);

  useIsoLayoutEffect(() => {
    const el = getScroll.current();
    if (!el) return;
    readView();
    el.addEventListener('scroll', readView, { passive: true });
    const ro = typeof ResizeObserver === 'undefined' ? null : new ResizeObserver(() => readView());
    ro?.observe(el);
    return () => {
      el.removeEventListener('scroll', readView);
      ro?.disconnect();
    };
  }, [readView]);

  // Measure items when they mount and whenever they resize.
  const observer = useRef<ResizeObserver | null>(null);
  const measureOne = useCallback((el: HTMLElement): boolean => {
    const index = Number(el.dataset.index);
    if (!Number.isFinite(index)) return false;
    const h = el.getBoundingClientRect().height;
    const key = keyRef.current(index);
    if (h > 0 && sizes.current.get(key) !== h) {
      sizes.current.set(key, h);
      return true;
    }
    return false;
  }, []);
  // Item elements handed to `measureElement`, so the ones that mounted before
  // the observer existed (the whole first render) are observed too.
  const mountedItems = useRef(new Set<HTMLElement>());
  useEffect(() => {
    if (typeof ResizeObserver === 'undefined') return;
    const ro = new ResizeObserver((entries) => {
      let changed = false;
      for (const entry of entries) {
        const el = entry.target as HTMLElement;
        if (!el.isConnected) {
          // Scrolled out of the window: stop watching it.
          ro.unobserve(el);
          mountedItems.current.delete(el);
        } else if (measureOne(el)) changed = true;
      }
      if (changed) setVersion((v) => v + 1);
    });
    observer.current = ro;
    for (const el of mountedItems.current) {
      if (el.isConnected) ro.observe(el);
      else mountedItems.current.delete(el);
    }
    return () => {
      ro.disconnect();
      observer.current = null;
    };
  }, [measureOne]);
  const measureElement = useCallback(
    (el: HTMLElement | null) => {
      if (!el) return;
      const known = mountedItems.current;
      // Bound the set where the observer never reports a removed element.
      if (known.size > 1024) {
        for (const old of known) {
          if (old.isConnected) continue;
          known.delete(old);
          observer.current?.unobserve(old);
        }
      }
      known.add(el);
      observer.current?.observe(el);
      if (measureOne(el)) setVersion((v) => v + 1);
    },
    [measureOne],
  );

  const scrollToIndex = useCallback<Virtualizer['scrollToIndex']>(
    (index, options = {}) => {
      const el = getScroll.current();
      const o = offsetsRef.current;
      if (!el || index < 0 || index >= o.length - 1) return;
      const align = options.align ?? 'auto';
      const itemTop = (o[index] ?? 0) + scrollMargin;
      const itemBottom = (o[index + 1] ?? 0) + scrollMargin;
      const viewTop = el.scrollTop + scrollMargin;
      const viewBottom = el.scrollTop + el.clientHeight;
      let top: number | null;
      if (align === 'start') top = itemTop - scrollMargin;
      else if (align === 'end') top = itemBottom - el.clientHeight;
      else if (align === 'center')
        top =
          itemTop - scrollMargin - (el.clientHeight - scrollMargin - (itemBottom - itemTop)) / 2;
      else if (itemTop < viewTop) top = itemTop - scrollMargin;
      else if (itemBottom > viewBottom) top = itemBottom - el.clientHeight;
      else top = null;
      if (top === null) return;
      const next = Math.max(0, top);
      if (options.behavior && options.behavior !== 'auto' && typeof el.scrollTo === 'function')
        el.scrollTo({ top: next, behavior: options.behavior });
      else el.scrollTop = next;
      readView();
    },
    [scrollMargin, readView],
  );

  let start: number;
  let end: number;
  let visibleRange: { start: number; end: number };
  if (count === 0) {
    start = 0;
    end = -1;
    visibleRange = { start: -1, end: -1 };
  } else if (view.height <= 0) {
    start = 0;
    end = Math.min(count, initialCount) - 1;
    visibleRange = { start, end };
  } else {
    const top = view.top - scrollMargin;
    const bottom = top + view.height;
    visibleRange = {
      start: findIndex(offsets, count, Math.max(0, top)),
      end: findIndex(offsets, count, Math.max(0, bottom - 1)),
    };
    start = findIndex(offsets, count, Math.max(0, top - overscan));
    end = findIndex(offsets, count, Math.max(0, bottom + overscan));
  }

  const indexes: number[] = [];
  const extra = (keepMounted ?? []).filter((i) => i >= 0 && i < count && (i < start || i > end));
  extra.sort((a, b) => a - b);
  let e = 0;
  for (let i = start; i <= end; i++) {
    while (e < extra.length && (extra[e] ?? 0) < i) indexes.push(extra[e++] ?? 0);
    indexes.push(i);
  }
  while (e < extra.length) indexes.push(extra[e++] ?? 0);

  const items = indexes.map((index) => ({
    index,
    key: keyOf(index),
    start: offsets[index] ?? 0,
    size: (offsets[index + 1] ?? 0) - (offsets[index] ?? 0),
  }));

  const getOffset = useCallback((index: number) => offsetsRef.current[index] ?? 0, []);

  return { items, totalSize, measureElement, scrollToIndex, getOffset, visibleRange };
}

/* ------------------------------------------------------------- component */

/** Imperative controls, through `handleRef`. */
export interface VirtualListHandle {
  /** Scrolls an item into view by index. */
  scrollToIndex: (
    index: number,
    options?: { align?: VirtualAlign | undefined; behavior?: ScrollBehavior | undefined },
  ) => void;
  /** Scrolls an item into view by key. False when no item has that key. */
  scrollToKey: (
    key: string,
    options?: { align?: VirtualAlign | undefined; behavior?: ScrollBehavior | undefined },
  ) => boolean;
}

export interface VirtualListMessages {
  /** Shown and announced at the end of the list while `loading` is true and items exist. */
  loadingMore: string;
  /** Announced while the first page loads (no items yet). */
  loading: string;
}

export const defaultVirtualListMessages: VirtualListMessages = {
  loadingMore: 'Loading more',
  loading: 'Loading',
};

export interface VirtualListProps<T> extends Omit<HTMLAttributes<HTMLDivElement>, 'children'> {
  /** Every item. Only the ones near the view are rendered. */
  items: readonly T[];
  /** Renders one item. Its height can vary: it is measured. */
  renderItem: (item: T, index: number) => ReactNode;
  /** Stable, unique key of an item: the React key and the measurement key. */
  getItemKey: (item: T, index: number) => string;
  /** Accessible name of the list, e.g. "Contacts". */
  label: string;
  /** Height in px used for items not measured yet. Default 48. */
  estimatedItemHeight?: number | undefined;
  /** Extra pixels rendered above and below the view. Default 320. */
  overscan?: number | undefined;
  /** Items rendered before the list knows its height (server rendering, first paint). Default 20. */
  initialRenderCount?: number | undefined;
  /** Height of the scroll area, e.g. `480` or `'60vh'`. Default: fill the parent (`100%`). */
  height?: number | string | undefined;
  /**
   * `list` (default) gives the items list semantics with their position in
   * the full list. `none` renders plain containers, for items that bring
   * their own semantics (articles, cards with headings).
   */
  semantics?: 'list' | 'none' | undefined;
  /** Called when the reader scrolls near the end, to load the next page. */
  onEndReached?: (() => void) | undefined;
  /** Pixels from the end that trigger `onEndReached`. Default 400. */
  endReachedThreshold?: number | undefined;
  /** More items are loading: shows a row at the end (or a status when there are no items). */
  loading?: boolean | undefined;
  /** Shown instead of the list when there are no items and nothing is loading. */
  empty?: ReactNode | undefined;
  /** Content after the last item, e.g. a LoadMore button or an end-of-list note. */
  footer?: ReactNode | undefined;
  /** Imperative scroll controls. */
  handleRef?: Ref<VirtualListHandle> | undefined;
  messages?: Partial<VirtualListMessages> | undefined;
}

function VirtualListInner<T>(
  {
    items,
    renderItem,
    getItemKey,
    label,
    estimatedItemHeight = 48,
    overscan = 320,
    initialRenderCount = 20,
    height,
    semantics = 'list',
    onEndReached,
    endReachedThreshold = 400,
    loading = false,
    empty,
    footer,
    handleRef,
    messages: messagesProp,
    className,
    style,
    ...rest
  }: VirtualListProps<T>,
  ref: Ref<HTMLDivElement>,
) {
  const m = { ...defaultVirtualListMessages, ...messagesProp };
  const scroller = useRef<HTMLDivElement | null>(null);
  const setScroller = useCallback(
    (el: HTMLDivElement | null) => {
      scroller.current = el;
      if (typeof ref === 'function') ref(el);
      else if (ref) (ref as { current: HTMLDivElement | null }).current = el;
    },
    [ref],
  );
  const keyAt = useCallback(
    (i: number) => {
      const item = items[i];
      return item === undefined ? i : getItemKey(item, i);
    },
    [items, getItemKey],
  );
  // The item that holds focus stays mounted when it scrolls out of the
  // window: unmounting it would drop keyboard focus to the page.
  const [focusedIndex, setFocusedIndex] = useState<number | null>(null);
  const v = useVirtualizer({
    count: items.length,
    getScrollElement: () => scroller.current,
    estimateSize: estimatedItemHeight,
    getItemKey: keyAt,
    overscan,
    initialCount: initialRenderCount,
    keepMounted: focusedIndex === null ? undefined : [focusedIndex],
  });
  useEffect(() => {
    const root = scroller.current;
    if (!root) return;
    const onFocusIn = (event: FocusEvent) => {
      // The item of this list that holds the target (not one of a nested list).
      let el = event.target as HTMLElement | null;
      while (el && el !== root && el.parentElement?.parentElement !== root) el = el.parentElement;
      const isItem = !!el && el !== root && el.classList.contains('mn-virtual-list__item');
      const index = isItem ? Number(el?.dataset.index) : NaN;
      setFocusedIndex(Number.isInteger(index) ? index : null);
    };
    const onFocusOut = (event: FocusEvent) => {
      if (!root.contains(event.relatedTarget as Node | null)) setFocusedIndex(null);
    };
    root.addEventListener('focusin', onFocusIn);
    root.addEventListener('focusout', onFocusOut);
    return () => {
      root.removeEventListener('focusin', onFocusIn);
      root.removeEventListener('focusout', onFocusOut);
    };
  }, []);

  const { scrollToIndex } = v;
  useImperativeHandle(
    handleRef,
    () => ({
      scrollToIndex,
      scrollToKey: (key, options) => {
        const i = items.findIndex((item, n) => getItemKey(item, n) === key);
        if (i < 0) return false;
        scrollToIndex(i, options);
        return true;
      },
    }),
    [scrollToIndex, items, getItemKey],
  );

  // Ask for the next page near the end, once per change in length.
  const asked = useRef(-1);
  const lastVisible = v.visibleRange.end;
  useEffect(() => {
    if (!onEndReached || loading || !items.length) return;
    const el = scroller.current;
    if (!el || el.clientHeight <= 0) return;
    const remaining = v.totalSize - (el.scrollTop + el.clientHeight);
    if (remaining <= endReachedThreshold && asked.current !== items.length) {
      asked.current = items.length;
      onEndReached();
    }
  }, [lastVisible, items.length, loading, onEndReached, endReachedThreshold, v.totalSize]);

  const list = semantics === 'list';
  const isEmpty = items.length === 0;

  return (
    <div
      ref={setScroller}
      // A scroll area must be reachable by keyboard so it can be scrolled with arrow keys.
      // eslint-disable-next-line jsx-a11y/no-noninteractive-tabindex
      tabIndex={0}
      role="group"
      aria-label={label}
      aria-busy={loading || undefined}
      {...rest}
      className={cx('mn-virtual-list', className)}
      data-empty={isEmpty || undefined}
      data-loading={loading || undefined}
      style={{ ...(height !== undefined ? { height } : {}), ...style }}
    >
      {isEmpty ? (
        loading ? (
          <div className="mn-virtual-list__status" role="status">
            <Spinner size="sm" label={m.loading} />
          </div>
        ) : (
          (empty ?? null)
        )
      ) : (
        <div
          className="mn-virtual-list__inner"
          role={list ? 'list' : undefined}
          style={{ height: v.totalSize }}
        >
          {v.items.map((vi) => {
            const item = items[vi.index] as T;
            return (
              <div
                key={vi.key}
                ref={v.measureElement}
                role={list ? 'listitem' : undefined}
                aria-setsize={list ? items.length : undefined}
                aria-posinset={list ? vi.index + 1 : undefined}
                data-index={vi.index}
                className="mn-virtual-list__item"
                style={{ transform: `translateY(${vi.start}px)` }}
              >
                {renderItem(item, vi.index)}
              </div>
            );
          })}
        </div>
      )}
      {!isEmpty && (loading || footer) ? (
        <div className="mn-virtual-list__footer">
          {loading ? (
            <span className="mn-virtual-list__loading" role="status">
              <Spinner size="sm" />
              {m.loadingMore}
            </span>
          ) : null}
          {footer}
        </div>
      ) : null}
    </div>
  );
}

/**
 * A windowed list for long collections (thousands of items): only the items
 * near the view are in the DOM, heights are measured so items can vary, and
 * the list keeps list semantics with each item's position in the whole set.
 * Supports infinite paging (`onEndReached`) and imperative scrolling.
 */
export const VirtualList = forwardRef(VirtualListInner) as <T>(
  props: VirtualListProps<T> & RefAttributes<HTMLDivElement>,
) => ReactElement | null;
