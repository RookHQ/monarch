'use client';

import {
  forwardRef,
  useEffect,
  useRef,
  type ForwardedRef,
  type HTMLAttributes,
  type ReactElement,
  type ReactNode,
  type RefAttributes,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { EmptyState } from '../../feedback/empty-state/index.js';
import { ErrorState } from '../../feedback/error-state/index.js';
import { LoadingState } from '../../feedback/loading-state/index.js';
import { LoadMore, type LoadMoreProps } from '../load-more/index.js';

export interface CursorListMessages {
  /** LoadingState label while the first page loads. */
  loading: string;
  emptyTitle: string;
  emptyDescription: string;
  /** ErrorState title when the first page fails. */
  errorTitle: string;
  retry: string;
  /** Load more button. */
  loadMore: string;
  loadingMore: string;
  /** Shown when a later page fails. */
  loadMoreError: string;
  /** Shown after the last page. */
  end: string;
}

export const defaultCursorListMessages: CursorListMessages = {
  loading: 'Loading',
  emptyTitle: 'Nothing here yet',
  emptyDescription: 'Items appear here once they are added.',
  errorTitle: 'This list could not be loaded',
  retry: 'Retry',
  loadMore: 'Load more',
  loadingMore: 'Loading…',
  loadMoreError: 'Couldn’t load more.',
  end: 'That’s everything',
};

export interface CursorListProps<T>
  extends
    Omit<HTMLAttributes<HTMLDivElement>, 'children'>,
    Pick<LoadMoreProps, 'hasMore' | 'onLoadMore' | 'autoLoad' | 'autoLoadMargin' | 'noun'> {
  /** Every item loaded so far, in order. */
  items: readonly T[];
  /** Stable key of an item. */
  getKey: (item: T, index: number) => string;
  /** Content of one list item. Put a link or button inside when the row opens something. */
  renderItem: (item: T, index: number) => ReactNode;
  /** A page is being fetched. With no items yet this shows the loading state. */
  loading?: boolean;
  /** The last fetch failed. With no items yet this shows the error state. */
  error?: boolean;
  /** Repeats the failed request. Default: `onLoadMore`. */
  onRetry?: (() => void) | undefined;
  /** Total items, when the server knows it ("Showing 20 of 140 requests"). */
  totalCount?: number | undefined;
  /** Accepted so `useLoadMore().loadMoreProps` can be spread; the list counts `items` itself. */
  loadedCount?: number | undefined;
  /** Accessible name of the list. */
  label: string;
  /** Use `ol` for lists whose order matters (logs, rankings). Default `ul`. */
  ordered?: boolean;
  /** Separators between items. Default true. */
  divided?: boolean;
  /** Replaces the default empty state (for example an EmptyState with a first-run action). */
  empty?: ReactNode;
  /** Text after the last page; `null` hides it. Default `messages.end`. */
  endLabel?: ReactNode;
  /**
   * After Load more is pressed by keyboard, move focus to the first new item
   * so the reader continues where the new page starts. Default true.
   */
  focusNewItems?: boolean;
  /** Variant of the first-page loading skeleton. Default `list`. */
  loadingVariant?: 'list' | 'rows' | 'cards';
  /** Class for each `<li>`. */
  itemClassName?: string;
  messages?: Partial<CursorListMessages>;
}

function CursorListInner<T>(
  {
    items,
    getKey,
    renderItem,
    hasMore,
    loading = false,
    error = false,
    onLoadMore,
    onRetry,
    totalCount,
    loadedCount: _loadedCount,
    noun,
    autoLoad,
    autoLoadMargin,
    label,
    ordered = false,
    divided = true,
    empty,
    endLabel,
    focusNewItems = true,
    loadingVariant = 'list',
    itemClassName,
    messages,
    className,
    ...rest
  }: CursorListProps<T>,
  ref: ForwardedRef<HTMLDivElement>,
) {
  const m = { ...defaultCursorListMessages, ...messages };
  const listRef = useRef<HTMLUListElement & HTMLOListElement>(null);
  // Index of the first item of the page requested from the footer by keyboard.
  const pendingFocus = useRef<number | null>(null);
  const count = items.length;

  useEffect(() => {
    const from = pendingFocus.current;
    if (from === null || loading) return;
    pendingFocus.current = null;
    if (count <= from) return;
    const li = listRef.current?.children.item(from) as HTMLElement | null;
    if (!li) return;
    li.tabIndex = -1;
    // Only the item focused after a load gets a tabindex; drop it again when focus leaves.
    li.addEventListener('blur', () => li.removeAttribute('tabindex'), { once: true });
    li.focus();
  }, [count, loading]);

  const requestMore = () => {
    const active = typeof document === 'undefined' ? null : document.activeElement;
    const fromFooter = active instanceof HTMLElement && active.closest('.mn-cursor-list__footer');
    pendingFocus.current = focusNewItems && fromFooter ? count : null;
    onLoadMore();
  };
  const retry = onRetry ?? onLoadMore;

  const Tag = ordered ? 'ol' : 'ul';
  let body: ReactNode;
  if (count === 0 && error) {
    body = (
      <ErrorState
        size="compact"
        headingLevel="p"
        title={m.errorTitle}
        onRetry={retry}
        retryLabel={m.retry}
        retrying={loading}
      />
    );
  } else if (count === 0 && loading) {
    body = <LoadingState label={m.loading} variant={loadingVariant} rows={3} />;
  } else if (count === 0 && !hasMore) {
    body = empty ?? (
      <EmptyState
        size="compact"
        headingLevel="p"
        title={m.emptyTitle}
        description={m.emptyDescription}
      />
    );
  } else {
    body = (
      <>
        <Tag ref={listRef} aria-label={label} className="mn-cursor-list__list">
          {items.map((item, i) => (
            <li key={getKey(item, i)} className={cx('mn-cursor-list__item', itemClassName)}>
              {renderItem(item, i)}
            </li>
          ))}
        </Tag>
        <LoadMore
          className="mn-cursor-list__footer"
          hasMore={hasMore}
          loading={loading}
          error={error}
          onLoadMore={requestMore}
          onRetry={() => {
            const active = typeof document === 'undefined' ? null : document.activeElement;
            const fromFooter =
              active instanceof HTMLElement && active.closest('.mn-cursor-list__footer');
            pendingFocus.current = focusNewItems && fromFooter ? count : null;
            retry();
          }}
          loadedCount={count}
          totalCount={totalCount}
          {...(noun ? { noun } : {})}
          {...(autoLoad !== undefined ? { autoLoad } : {})}
          {...(autoLoadMargin !== undefined ? { autoLoadMargin } : {})}
          label={m.loadMore}
          loadingLabel={m.loadingMore}
          errorLabel={m.loadMoreError}
          retryLabel={m.retry}
          endLabel={endLabel === undefined ? m.end : endLabel}
        />
      </>
    );
  }

  return (
    <div
      ref={ref}
      {...rest}
      data-divided={divided || undefined}
      data-state={
        count === 0 ? (error ? 'error' : loading ? 'loading' : hasMore ? 'idle' : 'empty') : 'ready'
      }
      aria-busy={loading || undefined}
      className={cx('mn-cursor-list', className)}
    >
      {body}
    </div>
  );
}

/**
 * A list loaded page by page from a cursor: the items, a Load more footer
 * with counts, and the first-load, empty and error states. Fetching stays in
 * the product (`useLoadMore` or any data library).
 */
export const CursorList = forwardRef(CursorListInner) as <T>(
  props: CursorListProps<T> & RefAttributes<HTMLDivElement>,
) => ReactElement | null;
