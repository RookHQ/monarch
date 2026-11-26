import { useLoadMore } from '@rookcom/monarch-react';
import { CursorList } from '@rookcom/monarch-react/data';
import type { ComponentDocs } from '../../../internal/component-docs.js';

interface Entry {
  id: string;
  method: string;
  path: string;
  status: number;
}

const ALL: Entry[] = Array.from({ length: 23 }, (_, i) => ({
  id: `req_${1000 + i}`,
  method: i % 3 === 0 ? 'POST' : 'GET',
  path: i % 2 ? '/v1/records' : `/v1/records/${100 + i}`,
  status: i % 7 === 0 ? 404 : 200,
}));

function fetchEntries(cursor: number | undefined) {
  const start = cursor ?? 0;
  return new Promise<{ items: Entry[]; nextCursor: number | null; total: number }>((resolve) =>
    setTimeout(() => {
      const next = start + 8;
      resolve({
        items: ALL.slice(start, next),
        nextCursor: next < ALL.length ? next : null,
        total: ALL.length,
      });
    }, 500),
  );
}

function RequestLog() {
  const log = useLoadMore<Entry, number>({ fetchPage: (cursor) => fetchEntries(cursor) });
  return (
    <CursorList
      label="Recent requests"
      ordered
      items={log.items}
      getKey={(e) => e.id}
      noun={{ one: 'request', other: 'requests' }}
      {...log.loadMoreProps}
      renderItem={(e) => (
        <a
          href={`#${e.id}`}
          style={{
            display: 'flex',
            gap: 12,
            padding: '10px 4px',
            color: 'inherit',
            textDecoration: 'none',
          }}
        >
          <code>{e.method}</code>
          <span style={{ flex: 1, minWidth: 0, overflowWrap: 'anywhere' }}>{e.path}</span>
          <span>{e.status}</span>
        </a>
      )}
    />
  );
}

export default {
  id: 'cursor-list',
  title: 'CursorList',
  summary:
    'A list loaded page by page from a cursor, with Load more, counts and first-load states.',
  sections: {
    purpose: {
      summary:
        'Shows a list that the server returns in cursor pages (logs, deliveries, keys) with one consistent footer and the empty, loading and error states around it. Needed by: Developers (logs, webhook deliveries, credentials, projects, API details).',
      useWhen: [
        'An API returns `items` plus a `nextCursor` and readers scroll through recent entries.',
        'A list or card layout, not a table with sortable columns.',
      ],
      avoidWhen: [
        'Rows with columns, sorting and selection: use DataTable with LoadMore under it.',
        'Readers must jump to a specific page: use Pagination.',
        'Thousands of rows on screen at once: virtualise.',
      ],
    },
    designPrinciples: {
      items: [
        'The list owns presentation only; fetching stays in the product (`useLoadMore` or a data library).',
        'Keyboard readers continue where the new page starts: focus moves to the first new item after Load more.',
        'One empty, loading and error vocabulary across products, from EmptyState, LoadingState and ErrorState.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Generic over the item type: `CursorList<T>`. Renders a `<div>` (ref) holding a `<ul>`/`<ol>` and a LoadMore footer.',
        'Accepts `useLoadMore().loadMoreProps` as a spread (`hasMore`, `loading`, `error`, `onLoadMore`, `onRetry`, `totalCount`, `loadedCount`).',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Migrating from the app copy (Developers `LoadMore` in `components/console/CursorList.tsx`): `shown` → derived from `items.length`; `total` → `totalCount`; `noun="requests"` → `noun={{ one: "request", other: "requests" }}`; `hasMore`, `loading` are unchanged; `onMore` → `onLoadMore`; `endText` → `endLabel`. Pages that render a DataTable keep it and use LoadMore under it; pages that render cards move the card into `renderItem`.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'ordered',
          values: ['false', 'true'],
          description: '`ul` (default) or `ol` for lists whose order matters.',
        },
        {
          prop: 'divided',
          values: ['true', 'false'],
          description: 'Hairlines between items and above the footer (`data-divided`).',
        },
        {
          prop: 'loadingVariant',
          values: ['list', 'rows', 'cards'],
          description: 'Skeleton shape while the first page loads.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'First load',
          attribute: 'data-state="loading", aria-busy',
          description: 'LoadingState skeleton while there are no items yet.',
        },
        {
          name: 'Empty',
          attribute: 'data-state="empty"',
          description: 'EmptyState, or `empty` when given.',
        },
        {
          name: 'First page failed',
          attribute: 'data-state="error"',
          description: 'ErrorState with Retry.',
        },
        {
          name: 'Ready',
          attribute: 'data-state="ready"',
          description: 'Items and the LoadMore footer (idle, loading, error or end).',
        },
      ],
    },
    interaction: {
      items: [
        'Load more calls `onLoadMore`; Retry calls `onRetry` (default `onLoadMore`).',
        'When Load more or Retry was pressed and focus was on the footer, focus moves to the first new item once the page arrives. Turn it off with `focusNewItems={false}`.',
        '`autoLoad` loads the next page when the footer scrolls into view; the button stays as a fallback.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Moves through the controls inside items, then to Load more.' },
        {
          keys: 'Enter / Space',
          action: 'Loads the next page (or retries), then focus lands on the first new item.',
        },
      ],
    },
    accessibility: {
      role: 'list',
      name: '`label`.',
      items: [
        'LoadMore announces “8 more requests loaded.”, failures and the end of the list in a polite status region.',
        'The container is `aria-busy` while a page loads.',
        'The focused new item gets a temporary `tabindex="-1"`, removed again when it loses focus, so it does not add tab stops.',
      ],
    },
    responsive: {
      items: [
        'Items take the full width; the footer wraps its count and button and centres them.',
        'On touch screens the footer button is at least 44 px tall.',
      ],
    },
    theme: {
      items: [
        'Separators use `--mn-color-border-subtle`; the focus ring `--mn-color-focus-ring`.',
        'Forced colours: CanvasText separators and a Highlight focus outline.',
      ],
    },
    examples: {
      items: [
        { title: 'With useLoadMore', stack: true, render: () => <RequestLog /> },
        {
          title: 'Empty',
          render: () => (
            <CursorList
              label="Signing keys"
              items={[]}
              getKey={(k: string) => k}
              renderItem={(k: string) => k}
              hasMore={false}
              onLoadMore={() => {}}
              messages={{
                emptyTitle: 'No keys yet',
                emptyDescription: 'Keys you create appear here.',
              }}
            />
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Give a `noun` so counts read naturally.',
        'Reset the data (for example `useLoadMore().reset()`) when filters change.',
        'Put one link or button per item for what opens it.',
      ],
      dont: [
        'Do not make the whole `<li>` clickable without a real link or button inside.',
        'Do not fetch inside `renderItem`.',
      ],
    },
    related: {
      items: [
        { id: 'load-more', reason: 'The footer on its own, for tables and custom layouts.' },
        { id: 'data-table', reason: 'Tabular data with sorting and selection.' },
        { id: 'pagination', reason: 'Numbered pages.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['empty-state', 'error-state', 'loading-state', 'load-more'],
    },
    testing: {
      unit: [
        'Named list with a counted footer.',
        'Load more by keyboard moves focus to the first new item, announces the page and shows the end.',
        'Does not move focus when loading was not started from the footer.',
        'First-load, empty (default and custom) and error states with Retry.',
        'Retry of a failed later page; ordered list.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
