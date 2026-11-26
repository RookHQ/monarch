'use client';

import {
  forwardRef,
  useId,
  useMemo,
  useRef,
  useState,
  type HTMLAttributes,
  type KeyboardEvent,
  type ReactNode,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { SearchGlyph } from '../../../internal/icons.js';
import type { ShellLinkComponent } from '../../../internal/shell/link.js';
import { Alert } from '../../feedback/alert/index.js';
import { Button } from '../../actions/button/index.js';
import { EmptyState } from '../../feedback/empty-state/index.js';
import { Skeleton } from '../../feedback/skeleton/index.js';
import { SearchResultItem, queryTerms, type MatchRange } from '../search-result-item/index.js';

/** One result. Text fields are plain text; highlights are added by the component. */
export interface SearchResultData {
  /** Stable id, unique across all groups. */
  id: string;
  title: string;
  /** Where the result goes. Unsafe URLs (such as `javascript:`) are dropped. */
  href?: string | undefined;
  /** Breadcrumb segments, such as `['Handbook', 'Travel']`. */
  path?: readonly string[] | undefined;
  /** Excerpt of the matching content. */
  snippet?: string | undefined;
  /** Exact match ranges in the title from the backend. */
  titleRanges?: readonly MatchRange[] | undefined;
  /** Exact match ranges in the snippet from the backend. */
  snippetRanges?: readonly MatchRange[] | undefined;
  /** The snippet carries `<mark>` markers from the backend (read as markers only, never HTML). */
  snippetMarkTags?: boolean | undefined;
  /** Leading icon, decorative. */
  icon?: ReactNode;
  /** Trailing details such as a type badge or date. */
  meta?: ReactNode;
}

/** A labelled group of results, such as "Documents" or "People". */
export interface SearchResultGroup {
  id: string;
  label: string;
  /** Results shown in this group. */
  items: readonly SearchResultData[];
  /** Total matches in the group, when more exist than are shown. Defaults to `items.length`. */
  total?: number | undefined;
  /** Leading icon for the group heading, decorative. */
  icon?: ReactNode;
  /** Shows a "Show all" button after the group when set. */
  onShowMore?: (() => void) | undefined;
}

export interface SearchResultsMessages {
  /** Live summary when results are shown. Default "12 results for “query”". */
  summary: (count: number, query: string) => string;
  /** Live summary while loading. Default "Searching…". */
  loading: string;
  /** Empty-state title. Default "No results for “query”" (or "No results"). */
  emptyTitle: (query: string) => string;
  /** Empty-state description. Default suggests other words or fewer filters. */
  emptyDescription: string;
  /** Error title. Default "Search failed". */
  errorTitle: string;
  /** Retry button label. Default "Try again". */
  retry: string;
  /** "Show all" button per group. Default "Show all 24 in Documents". */
  showMore: (group: string, total: number) => string;
  /** Visible count next to a group heading, also read with the heading. Default "24". */
  groupCount: (total: number) => string;
}

const DEFAULT_MESSAGES: SearchResultsMessages = {
  summary: (count, query) =>
    `${count} ${count === 1 ? 'result' : 'results'}${query ? ` for “${query}”` : ''}`,
  loading: 'Searching…',
  emptyTitle: (query) => (query ? `No results for “${query}”` : 'No results'),
  emptyDescription: 'Try other words, check the spelling, or remove a filter.',
  errorTitle: 'Search failed',
  retry: 'Try again',
  showMore: (group, total) => `Show all ${total} in ${group}`,
  groupCount: (total) => String(total),
};

export type SearchResultsHeadingLevel = 'h2' | 'h3' | 'h4' | 'h5' | 'h6';

export interface SearchResultsProps extends Omit<
  HTMLAttributes<HTMLDivElement>,
  'onSelect' | 'results'
> {
  /** Grouped results. Groups with no items are not shown. */
  groups?: readonly SearchResultGroup[] | undefined;
  /** Ungrouped results, used when `groups` is not given. */
  results?: readonly SearchResultData[] | undefined;
  /** What the user searched for. Its words are highlighted and named in the summary. */
  query?: string | undefined;
  /** Results are being fetched. Shows skeleton rows, or keeps current results dimmed. */
  loading?: boolean | undefined;
  /** The search failed. A string or node is shown as the error message. */
  error?: ReactNode;
  /** Shows a "Try again" button in the error state. */
  onRetry?: (() => void) | undefined;
  /** Called when a result is activated (click or Enter), before any link navigation. */
  onSelect?: ((result: SearchResultData, group: SearchResultGroup | undefined) => void) | undefined;
  /** Router link component for client-side navigation; defaults to `<a>`. */
  linkComponent?: ShellLinkComponent | undefined;
  /** Heading level for group labels. Default `h2`. */
  headingLevel?: SearchResultsHeadingLevel | undefined;
  /** Replaces the default empty state. */
  empty?: ReactNode;
  /** Skeleton rows while loading with no results yet. Default 4. */
  loadingRows?: number | undefined;
  /** Text overrides. */
  messages?: Partial<SearchResultsMessages> | undefined;
}

type Status = 'loading' | 'error' | 'empty' | 'ready';

/**
 * Search results in labelled groups with counts, highlighted matches, and
 * loading, empty and error states. The results are one Tab stop; arrow keys,
 * Home and End move between them and Enter opens one.
 */
export const SearchResults = forwardRef<HTMLDivElement, SearchResultsProps>(function SearchResults(
  {
    groups: groupsProp,
    results,
    query = '',
    loading = false,
    error,
    onRetry,
    onSelect,
    linkComponent,
    headingLevel: Heading = 'h2',
    empty,
    loadingRows = 4,
    messages: messagesProp,
    className,
    ...rest
  },
  ref,
) {
  const messages = { ...DEFAULT_MESSAGES, ...messagesProp };
  const uid = useId();
  const groups = useMemo<SearchResultGroup[]>(() => {
    if (groupsProp) return groupsProp.filter((g) => g.items.length > 0);
    return results?.length ? [{ id: 'results', label: '', items: results }] : [];
  }, [groupsProp, results]);
  const flat = useMemo(() => groups.flatMap((g) => g.items), [groups]);
  const terms = useMemo(() => queryTerms(query), [query]);
  const [activeId, setActiveId] = useState<string | null>(null);
  const targets = useRef(new Map<string, HTMLElement>());
  const tabStopId = flat.some((r) => r.id === activeId) ? activeId : (flat[0]?.id ?? null);
  const total = groups.reduce((n, g) => n + (g.total ?? g.items.length), 0);
  const hasError = error !== undefined && error !== null && error !== false;

  const status: Status = hasError ? 'error' : flat.length ? 'ready' : loading ? 'loading' : 'empty';

  const focusAt = (index: number) => {
    const result = flat[index];
    if (!result) return;
    setActiveId(result.id);
    targets.current.get(result.id)?.focus();
  };

  const handleKeyDown = (event: KeyboardEvent<HTMLDivElement>, id: string) => {
    if (event.defaultPrevented || event.altKey || event.ctrlKey || event.metaKey) return;
    // Only the result's own link or button navigates; controls in `meta` keep their keys.
    if (event.target !== targets.current.get(id)) return;
    const index = flat.findIndex((r) => r.id === id);
    if (index < 0) return;
    let next: number | null = null;
    if (event.key === 'ArrowDown') next = Math.min(index + 1, flat.length - 1);
    else if (event.key === 'ArrowUp') next = Math.max(index - 1, 0);
    else if (event.key === 'Home') next = 0;
    else if (event.key === 'End') next = flat.length - 1;
    if (next === null) return;
    event.preventDefault();
    focusAt(next);
  };

  const summary =
    status === 'loading' || (loading && status === 'ready')
      ? messages.loading
      : status === 'ready'
        ? messages.summary(total, query)
        : status === 'empty'
          ? messages.emptyTitle(query)
          : '';

  const renderItems = (
    group: SearchResultGroup | undefined,
    items: readonly SearchResultData[],
  ) => (
    <ul className="mn-search-results__list">
      {items.map((r) => (
        <li key={r.id} className="mn-search-results__entry">
          <SearchResultItem
            title={r.title}
            href={r.href}
            path={r.path}
            snippet={r.snippet}
            terms={terms}
            titleRanges={r.titleRanges}
            snippetRanges={r.snippetRanges}
            snippetMarkTags={r.snippetMarkTags}
            icon={r.icon}
            meta={r.meta}
            linkComponent={linkComponent}
            targetTabIndex={r.id === tabStopId ? 0 : -1}
            targetRef={(el) => {
              if (el) targets.current.set(r.id, el);
              else targets.current.delete(r.id);
            }}
            onFocus={() => setActiveId(r.id)}
            onKeyDown={(event) => handleKeyDown(event, r.id)}
            onSelect={onSelect ? () => onSelect(r, group) : undefined}
          />
        </li>
      ))}
    </ul>
  );

  let body: ReactNode = null;
  if (status === 'error') {
    body = (
      <Alert
        tone="danger"
        title={messages.errorTitle}
        live="assertive"
        actions={
          onRetry ? (
            <Button size="sm" onClick={onRetry}>
              {messages.retry}
            </Button>
          ) : undefined
        }
      >
        {error === true ? null : error}
      </Alert>
    );
  } else if (status === 'loading') {
    body = (
      <div className="mn-search-results__skeleton" aria-hidden="true">
        {Array.from({ length: loadingRows }, (_, i) => (
          <div key={i} className="mn-search-results__skeleton-row">
            <Skeleton shape="text" width="40%" />
            <Skeleton shape="text" width="85%" />
          </div>
        ))}
      </div>
    );
  } else if (status === 'empty') {
    body = empty ?? (
      <EmptyState
        kind="filtered"
        icon={<SearchGlyph />}
        title={messages.emptyTitle(query)}
        description={messages.emptyDescription}
        headingLevel={Heading}
      />
    );
  } else if (groupsProp) {
    body = groups.map((g) => {
      const headingId = `${uid}-group-${g.id}`;
      const groupTotal = g.total ?? g.items.length;
      return (
        <section key={g.id} className="mn-search-results__group" aria-labelledby={headingId}>
          <Heading id={headingId} className="mn-search-results__heading">
            {g.icon ? (
              <span className="mn-search-results__heading-icon" aria-hidden="true">
                {g.icon}
              </span>
            ) : null}
            <span className="mn-search-results__heading-label">{g.label}</span>
            <span className="mn-search-results__count">{messages.groupCount(groupTotal)}</span>
          </Heading>
          {renderItems(g, g.items)}
          {g.onShowMore && groupTotal > g.items.length ? (
            <div className="mn-search-results__more">
              <Button variant="ghost" size="sm" onClick={g.onShowMore}>
                {messages.showMore(g.label, groupTotal)}
              </Button>
            </div>
          ) : null}
        </section>
      );
    });
  } else {
    body = renderItems(undefined, flat);
  }

  return (
    <div
      ref={ref}
      {...rest}
      className={cx('mn-search-results', className)}
      data-state={status}
      data-refreshing={loading && status === 'ready' ? '' : undefined}
      aria-busy={loading || undefined}
    >
      <p
        className={cx('mn-search-results__summary', status !== 'ready' && 'mn-visually-hidden')}
        role="status"
      >
        {summary}
      </p>
      {body}
    </div>
  );
});
