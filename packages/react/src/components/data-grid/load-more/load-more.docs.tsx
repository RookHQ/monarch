import { useState } from 'react';
import { LoadMore, useLoadMore, type LoadMorePage } from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';

type Entry = { id: number; title: string };

const ALL: Entry[] = Array.from({ length: 9 }, (_, i) => ({
  id: i + 1,
  title: `Audit entry ${i + 1}`,
}));

/** A fake cursor endpoint: pages of 3, a short delay, and one failure on the third page. */
function demoFetch(): (cursor: number | undefined) => Promise<LoadMorePage<Entry, number>> {
  let failed = false;
  return (cursor) =>
    new Promise((resolve, reject) => {
      setTimeout(() => {
        const start = cursor ?? 0;
        if (start === 6 && !failed) {
          failed = true;
          reject(new Error('Network error'));
          return;
        }
        const items = ALL.slice(start, start + 3);
        resolve({
          items,
          nextCursor: start + 3 < ALL.length ? start + 3 : null,
          total: ALL.length,
        });
      }, 600);
    });
}

function CursorListDemo() {
  const [fetchPage] = useState(demoFetch);
  const list = useLoadMore<Entry, number>({
    fetchPage,
    initialPage: { items: ALL.slice(0, 3), nextCursor: 3, total: ALL.length },
  });
  return (
    <div style={{ width: '100%' }}>
      <ul aria-label="Audit log" style={{ margin: 0, paddingInlineStart: '1.25rem' }}>
        {list.items.map((e) => (
          <li key={e.id}>{e.title}</li>
        ))}
      </ul>
      <LoadMore {...list.loadMoreProps} noun={{ one: 'entry', other: 'entries' }} />
    </div>
  );
}

export default {
  id: 'load-more',
  title: 'LoadMore',
  summary: 'Cursor pagination footer with loading, error and retry, end of list and announcements.',
  sections: {
    purpose: {
      summary:
        'Appends the next page of a cursor-paginated list on request. `LoadMore` is the footer; `useLoadMore` holds the pages, cursor and status, while the product supplies the fetch.',
      useWhen: [
        'Feeds, audit logs, search results and message history served with a cursor (`?cursor=…&limit=…`).',
        'The reader scans forward and rarely needs to jump to page 7.',
      ],
      avoidWhen: [
        'Readers need to jump to a page or know where they are in a fixed set: use Pagination.',
        'The whole set is small enough to render at once.',
      ],
    },
    designPrinciples: {
      items: [
        'The button is always there while there is more, even with `autoLoad`: scrolling is a convenience, not the only way.',
        'Every outcome is announced politely: how many items arrived, a failure, or the end of the list.',
        'Errors are recoverable in place: the same button becomes Retry and repeats the failed cursor.',
        'Data fetching stays in the product. The hook takes any `fetchPage`, and the component works with any data library through its props.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        '`useLoadMore({ fetchPage, initialPage?, loadOnMount? })` returns `items`, `status` (`idle`, `loading`, `error`, `done`), `error`, `hasMore`, `total`, `loadMore`, `retry`, `reset` and `loadMoreProps` to spread onto `<LoadMore>`.',
        '`fetchPage(cursor, { signal })` receives an AbortSignal that fires on `reset` and unmount; responses that arrive after a reset are ignored.',
        'With react-query or SWR, skip the hook and pass `hasMore`, `loading`, `error`, `onLoadMore` and `loadedCount` from `useInfiniteQuery` directly.',
        'The ref is forwarded to the root `<div>`.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [
        {
          prop: 'autoLoad',
          values: ['false', 'true'],
          description:
            'Also loads when the footer scrolls into view (IntersectionObserver, `autoLoadMargin` ahead).',
        },
        {
          prop: 'showCount',
          values: ['true', 'false'],
          description:
            '“Showing 40 of 120 items” beside the button. On by default when `loadedCount` is given.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Idle',
          attribute: 'data-state="idle"',
          description: 'Load more button, optional count.',
        },
        {
          name: 'Loading',
          attribute: 'data-state="loading"',
          description:
            'The button shows a spinner and “Loading…”, stays focusable and ignores repeat clicks (`aria-busy`).',
        },
        {
          name: 'Error',
          attribute: 'data-state="error"',
          description:
            '`errorLabel` in danger text; the button reads Retry; auto-load pauses until it succeeds.',
        },
        {
          name: 'End of list',
          attribute: 'data-state="done"',
          description:
            'The button is replaced by `endLabel` (“That’s everything”), or nothing when `endLabel={null}`.',
        },
      ],
    },
    interaction: {
      items: [
        'Activating the button calls `onLoadMore`, or `onRetry` (default `onLoadMore`) after an error.',
        'The hook ignores `loadMore` while a request is in flight and after the last page.',
        '`reset()` clears the items and loads the first page again, aborting any request in flight.',
        'When the last page arrives while the button has focus, focus moves to the end message, so it is not lost to the page body.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Moves to the Load more (or Retry) button.' },
        { keys: 'Enter / Space', action: 'Loads the next page, or retries.' },
      ],
    },
    accessibility: {
      role: 'button inside a group with a polite status region',
      name: '“Load more”, “Loading…” or “Retry” (customisable labels).',
      items: [
        'A visually hidden `role="status"` region announces “20 more items loaded.”, the error and the end of the list.',
        'Loading uses `aria-busy` and `aria-disabled` instead of `disabled`, so keyboard focus stays on the button.',
        'Auto-load never removes the button, so keyboard and switch users can always load explicitly.',
      ],
    },
    responsive: {
      items: [
        'The count, message and button wrap onto separate lines on narrow screens and stay centred.',
      ],
    },
    theme: {
      items: [
        'Muted text for the count and end message, danger text for the error; the button is a secondary Button. All are Monarch tokens.',
      ],
    },
    examples: {
      items: [
        {
          title: 'With useLoadMore',
          render: () => <CursorListDemo />,
          code: `const list = useLoadMore<AuditEntry, string>({
  fetchPage: async (cursor, { signal }) => {
    const res = await api.get('/audit', { params: { cursor, limit: 50 }, signal });
    return { items: res.data, nextCursor: res.meta.nextCursor, total: res.meta.total };
  },
});

<ul>{list.items.map((e) => <AuditRow key={e.id} entry={e} />)}</ul>
<LoadMore {...list.loadMoreProps} noun={{ one: 'entry', other: 'entries' }} />`,
        },
        {
          title: 'States',
          stack: true,
          render: () => (
            <>
              <LoadMore hasMore loadedCount={50} totalCount={214} onLoadMore={() => {}} />
              <LoadMore hasMore loading loadedCount={50} onLoadMore={() => {}} />
              <LoadMore hasMore error loadedCount={50} onLoadMore={() => {}} />
              <LoadMore hasMore={false} loadedCount={214} totalCount={214} onLoadMore={() => {}} />
            </>
          ),
          code: `<LoadMore hasMore loadedCount={50} totalCount={214} onLoadMore={next} />
<LoadMore hasMore loading loadedCount={50} onLoadMore={next} />
<LoadMore hasMore error loadedCount={50} onLoadMore={next} />
<LoadMore hasMore={false} loadedCount={214} totalCount={214} onLoadMore={next} />`,
        },
        {
          title: 'With react-query',
          render: () => (
            <LoadMore hasMore loadedCount={20} onLoadMore={() => {}} autoLoad={false} />
          ),
          code: `const q = useInfiniteQuery({ queryKey, queryFn, getNextPageParam });
const rows = q.data?.pages.flatMap((p) => p.rows) ?? [];

<LoadMore
  hasMore={q.hasNextPage}
  loading={q.isFetchingNextPage}
  error={q.isFetchNextPageError}
  onLoadMore={() => q.fetchNextPage()}
  loadedCount={rows.length}
  autoLoad
/>`,
        },
      ],
    },
    doDont: {
      do: [
        'Give a `noun` so counts and announcements read naturally (“3 more entries loaded”).',
        'Pass `totalCount` when the server knows it.',
        'Call `reset()` when filters or search change.',
      ],
      dont: [
        'Do not hide the button when using `autoLoad`.',
        'Do not use LoadMore where readers must reach a specific page; use Pagination.',
        'Do not fetch inside the component; pass `fetchPage` or the query state.',
      ],
    },
    related: {
      items: [
        { id: 'pagination', reason: 'Numbered pages with jumps.' },
        { id: 'data-table', reason: 'Tables whose rows are loaded in pages.' },
        { id: 'button', reason: 'The control LoadMore renders.' },
        { id: 'loading-state', reason: 'The first load of an empty list.' },
      ],
    },
    dependencies: { packages: ['react'], components: ['button'] },
    testing: {
      unit: [
        'Loads the first page on mount, more on click, then shows the end of the list and announces each step.',
        'Works with Enter and Space and moves focus to the end message when the button disappears.',
        'Shows and announces an error; Retry repeats the same cursor, including a failed first page.',
        '`reset` reloads from the first page and ignores a stale response.',
        'Ignores repeat `loadMore` calls while loading.',
        'Starts from `initialPage` without a request.',
        'Auto-loads on intersection and pauses after an error, keeping the button.',
        'Works controlled by props without the hook.',
      ],
      axe: true,
      ssr: true,
      browser: ['Visual baselines: /__gallery', 'Cross-browser axe: /components/load-more'],
    },
  },
} satisfies ComponentDocs;
