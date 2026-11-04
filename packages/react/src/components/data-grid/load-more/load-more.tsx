'use client';

import {
  forwardRef,
  useCallback,
  useEffect,
  useRef,
  useState,
  type HTMLAttributes,
  type ReactNode,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { Button } from '../../actions/button/index.js';

/* ------------------------------------------------------------------------ */
/* useLoadMore                                                               */
/* ------------------------------------------------------------------------ */

/** One page from a cursor-paginated source. `nextCursor` null or undefined means the end. */
export interface LoadMorePage<T, C> {
  items: T[];
  nextCursor?: C | null;
  /** Total number of items, when the server knows it. */
  total?: number;
}

export interface UseLoadMoreOptions<T, C> {
  /**
   * Fetches one page. `cursor` is undefined for the first page. Abort the
   * request when `signal` fires (on `reset` and unmount).
   */
  fetchPage: (
    cursor: C | undefined,
    context: { signal: AbortSignal },
  ) => Promise<LoadMorePage<T, C>>;
  /** Server-rendered or cached first page; skips the initial request. */
  initialPage?: LoadMorePage<T, C>;
  /** Load the first page on mount when there is no `initialPage`. Default `true`. */
  loadOnMount?: boolean;
}

export type LoadMoreStatus = 'idle' | 'loading' | 'error' | 'done';

export interface UseLoadMoreResult<T> {
  /** Every item loaded so far, in order. */
  items: T[];
  status: LoadMoreStatus;
  /** The last error thrown by `fetchPage`, cleared on the next attempt. */
  error: unknown;
  hasMore: boolean;
  /** Total from the latest page, when the server sent one. */
  total: number | undefined;
  /** Items added by the latest successful page. */
  lastPageCount: number;
  /** Loads the next page. Does nothing while loading or at the end. */
  loadMore: () => void;
  /** Repeats the request that failed. */
  retry: () => void;
  /** Drops every item and loads the first page again (for example after a filter change). */
  reset: () => void;
  /** Props for `<LoadMore>`, so the two wire together in one spread. */
  loadMoreProps: Pick<
    LoadMoreProps,
    'hasMore' | 'loading' | 'error' | 'onLoadMore' | 'onRetry' | 'loadedCount' | 'totalCount'
  >;
}

/**
 * Cursor pagination state: accumulates pages, tracks loading, error and end
 * states, ignores stale responses and aborts in-flight requests on reset and
 * unmount. Fetching stays in the product: pass `fetchPage`.
 */
export function useLoadMore<T, C = string>(
  options: UseLoadMoreOptions<T, C>,
): UseLoadMoreResult<T> {
  const { initialPage, loadOnMount = true } = options;
  const fetchRef = useRef(options.fetchPage);
  useEffect(() => {
    fetchRef.current = options.fetchPage;
  });

  const [items, setItems] = useState<T[]>(() => initialPage?.items ?? []);
  const [cursor, setCursor] = useState<C | undefined>(() => initialPage?.nextCursor ?? undefined);
  const [started, setStarted] = useState(initialPage !== undefined);
  const [status, setStatus] = useState<LoadMoreStatus>(() =>
    initialPage && (initialPage.nextCursor === null || initialPage.nextCursor === undefined)
      ? 'done'
      : 'idle',
  );
  const [error, setError] = useState<unknown>(undefined);
  const [total, setTotal] = useState<number | undefined>(initialPage?.total);
  const [lastPageCount, setLastPageCount] = useState(initialPage?.items.length ?? 0);

  const request = useRef<{ id: number; controller: AbortController } | null>(null);
  const seq = useRef(0);
  const state = useRef({ cursor, status, started });
  state.current = { cursor, status, started };

  const run = useCallback((from: C | undefined, replace: boolean) => {
    request.current?.controller.abort();
    const id = ++seq.current;
    const controller = new AbortController();
    request.current = { id, controller };
    // Synchronously, so a second call in the same tick sees the request in flight.
    state.current = { ...state.current, status: 'loading' };
    setStatus('loading');
    setError(undefined);
    fetchRef.current(from, { signal: controller.signal }).then(
      (page) => {
        if (request.current?.id !== id) return;
        request.current = null;
        setItems((prev) => (replace ? page.items : [...prev, ...page.items]));
        setLastPageCount(page.items.length);
        setTotal(page.total);
        const next = page.nextCursor ?? undefined;
        setCursor(next);
        setStarted(true);
        setStatus(next === undefined ? 'done' : 'idle');
      },
      (err: unknown) => {
        if (request.current?.id !== id || controller.signal.aborted) return;
        request.current = null;
        setError(err);
        setStatus('error');
      },
    );
  }, []);

  const loadMore = useCallback(() => {
    const s = state.current;
    if (s.status === 'loading' || s.status === 'done') return;
    run(s.started ? s.cursor : undefined, !s.started);
  }, [run]);

  const reset = useCallback(() => {
    setItems([]);
    setCursor(undefined);
    setStarted(false);
    setTotal(undefined);
    setLastPageCount(0);
    state.current = { cursor: undefined, status: 'idle', started: false };
    run(undefined, true);
  }, [run]);

  useEffect(() => {
    if (initialPage === undefined && loadOnMount) run(undefined, true);
    return () => request.current?.controller.abort();
    // Mount only: later loads are explicit (loadMore, retry, reset).
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  const hasMore = status !== 'done';
  return {
    items,
    status,
    error,
    hasMore,
    total,
    lastPageCount,
    loadMore,
    retry: loadMore,
    reset,
    loadMoreProps: {
      hasMore,
      loading: status === 'loading',
      error: status === 'error',
      onLoadMore: loadMore,
      onRetry: loadMore,
      loadedCount: items.length,
      totalCount: total,
    },
  };
}

/* ------------------------------------------------------------------------ */
/* LoadMore                                                                  */
/* ------------------------------------------------------------------------ */

export interface LoadMoreProps extends Omit<HTMLAttributes<HTMLDivElement>, 'children'> {
  /** There is another page to load. */
  hasMore: boolean;
  /** A page is being fetched. The button stays focusable and shows a spinner. */
  loading?: boolean;
  /** The last fetch failed. Shows `errorLabel` and turns the button into Retry. Auto-load pauses. */
  error?: boolean;
  /** Loads the next page. */
  onLoadMore: () => void;
  /** Called by the Retry button. Default: `onLoadMore`. */
  onRetry?: () => void;
  /** Items currently on screen. Changes are announced ("20 more items loaded"). */
  loadedCount?: number;
  /** Total items, when the server knows it. */
  totalCount?: number | undefined;
  /** Noun for counts and announcements. Default `{ one: 'item', other: 'items' }`. */
  noun?: { one: string; other: string };
  /** Show "Showing 40 of 120 items" next to the button. Default `true` when `loadedCount` is given. */
  showCount?: boolean;
  /**
   * Load the next page when the control scrolls into view
   * (IntersectionObserver). The button always stays as the fallback and for
   * keyboard users; auto-load pauses after an error. Default `false`.
   */
  autoLoad?: boolean;
  /** IntersectionObserver root margin for `autoLoad`. Default `200px`. */
  autoLoadMargin?: string;
  /** Button text. Default "Load more". */
  label?: ReactNode;
  /** Button text while loading. Default "Loading…". */
  loadingLabel?: ReactNode;
  /** Error message. Default "Couldn’t load more." */
  errorLabel?: ReactNode;
  /** Retry button text. Default "Retry". */
  retryLabel?: ReactNode;
  /** Shown when everything is loaded; `null` hides it. Default "That’s everything". */
  endLabel?: ReactNode;
}

function plural(n: number, noun: { one: string; other: string }) {
  return n === 1 ? noun.one : noun.other;
}

const DEFAULT_NOUN = { one: 'item', other: 'items' };

/**
 * The footer of a cursor-paginated list: a Load more button with loading,
 * error and retry, end-of-list states and polite announcements, plus
 * optional auto-load on scroll. Pair it with `useLoadMore`, or drive it from
 * any data library through its props.
 */
export const LoadMore = forwardRef<HTMLDivElement, LoadMoreProps>(function LoadMore(
  {
    hasMore,
    loading = false,
    error = false,
    onLoadMore,
    onRetry,
    loadedCount,
    totalCount,
    noun = DEFAULT_NOUN,
    showCount,
    autoLoad = false,
    autoLoadMargin = '200px',
    label = 'Load more',
    loadingLabel = 'Loading…',
    errorLabel = 'Couldn’t load more.',
    retryLabel = 'Retry',
    endLabel = 'That’s everything',
    className,
    ...rest
  },
  ref,
) {
  const rootRef = useRef<HTMLDivElement | null>(null);
  const buttonRef = useRef<HTMLButtonElement | null>(null);
  const endRef = useRef<HTMLParagraphElement | null>(null);
  const [announcement, setAnnouncement] = useState('');
  const prevCount = useRef(loadedCount);
  const prevError = useRef(error);
  const prevHasMore = useRef(hasMore);
  const buttonHadFocus = useRef(false);

  const setRefs = useCallback(
    (node: HTMLDivElement | null) => {
      rootRef.current = node;
      if (typeof ref === 'function') ref(node);
      else if (ref) ref.current = node;
    },
    [ref],
  );

  // Announce what changed: new items, a failure, or the end of the list.
  useEffect(() => {
    const parts: string[] = [];
    const before = prevCount.current;
    if (loadedCount !== undefined && before !== undefined && loadedCount > before) {
      const added = loadedCount - before;
      parts.push(`${added} more ${plural(added, noun)} loaded.`);
    }
    if (error && !prevError.current) {
      parts.push(typeof errorLabel === 'string' ? errorLabel : 'Couldn’t load more.');
    }
    if (!hasMore && prevHasMore.current && typeof endLabel === 'string') parts.push(endLabel);
    prevCount.current = loadedCount;
    prevError.current = error;
    prevHasMore.current = hasMore;
    if (parts.length) setAnnouncement(parts.join(' '));
  }, [loadedCount, error, hasMore, noun, errorLabel, endLabel]);

  // The button disappears at the end; keep keyboard focus on the list footer.
  useEffect(() => {
    if (!hasMore && buttonHadFocus.current) {
      buttonHadFocus.current = false;
      const active = typeof document === 'undefined' ? null : document.activeElement;
      // Only when focus was actually lost with the button, never stealing it from elsewhere.
      if (active === null || active === document.body) {
        (endRef.current ?? rootRef.current)?.focus();
      }
    }
  }, [hasMore]);

  // Auto-load: fire when the footer scrolls into view; the button stays as the fallback.
  useEffect(() => {
    const node = rootRef.current;
    if (!autoLoad || !hasMore || loading || error || !node) return;
    if (typeof IntersectionObserver === 'undefined') return;
    const observer = new IntersectionObserver(
      (entries) => {
        if (entries.some((e) => e.isIntersecting)) onLoadMore();
      },
      { rootMargin: autoLoadMargin },
    );
    observer.observe(node);
    return () => observer.disconnect();
  }, [autoLoad, hasMore, loading, error, onLoadMore, autoLoadMargin]);

  const state = error ? 'error' : loading ? 'loading' : hasMore ? 'idle' : 'done';
  const countVisible = (showCount ?? loadedCount !== undefined) && loadedCount !== undefined;
  const count = countVisible
    ? totalCount !== undefined
      ? `Showing ${loadedCount} of ${totalCount} ${plural(totalCount, noun)}`
      : `Showing ${loadedCount} ${plural(loadedCount, noun)}`
    : null;

  return (
    <div
      ref={setRefs}
      {...rest}
      tabIndex={-1}
      className={cx('mn-load-more', className)}
      data-state={state}
    >
      {count ? <p className="mn-load-more__count">{count}</p> : null}
      {error ? <p className="mn-load-more__error">{errorLabel}</p> : null}
      {hasMore ? (
        <Button
          ref={buttonRef}
          size="sm"
          loading={loading}
          onFocus={() => (buttonHadFocus.current = true)}
          onBlur={(event) => {
            // A blur caused by the button unmounting has no related target; keep the flag then.
            if (event.relatedTarget) buttonHadFocus.current = false;
          }}
          onClick={() => (error ? (onRetry ?? onLoadMore)() : onLoadMore())}
        >
          {loading ? loadingLabel : error ? retryLabel : label}
        </Button>
      ) : endLabel !== null ? (
        <p ref={endRef} tabIndex={-1} className="mn-load-more__end">
          {endLabel}
        </p>
      ) : null}
      <span role="status" aria-live="polite" aria-atomic="true" className="mn-visually-hidden">
        {announcement}
      </span>
    </div>
  );
});
