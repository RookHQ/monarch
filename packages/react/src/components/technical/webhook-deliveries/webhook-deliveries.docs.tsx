import { Button } from '@rookcom/monarch-react';
import { WebhookDeliveries, type WebhookDelivery } from '@rookcom/monarch-react/patterns';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const minutesAgo = (m: number) => new Date(Date.now() - m * 60_000);

const sample: WebhookDelivery[] = [
  {
    id: 'd1',
    status: 'succeeded',
    eventType: 'order.created',
    eventId: 'evt_3c91a0',
    responseCode: 200,
    durationMs: 142,
    attempts: 1,
    at: minutesAgo(2),
  },
  {
    id: 'd2',
    status: 'retrying',
    eventType: 'order.updated',
    eventId: 'evt_3c8f12',
    responseCode: 503,
    error: 'The endpoint answered 503',
    attempts: 2,
    at: minutesAgo(9),
    nextAttemptAt: new Date(Date.now() + 5 * 60_000),
  },
  {
    id: 'd3',
    status: 'failed',
    eventType: 'invoice.paid',
    eventId: 'evt_3c7b55',
    error: 'No response within 10 seconds',
    attempts: 6,
    at: minutesAgo(65),
  },
  {
    id: 'd4',
    status: 'pending',
    eventType: 'order.created',
    eventId: 'evt_3c9d77',
    attempts: 0,
    at: minutesAgo(0.5),
  },
];

const wait = () => new Promise((resolve) => setTimeout(resolve, 800));

export default {
  id: 'webhook-deliveries',
  title: 'WebhookDeliveries',
  summary: 'A filterable delivery log for a webhook endpoint, with redeliver and paging.',
  sections: {
    purpose: {
      summary:
        'Shows every attempt to deliver an event to an endpoint: status, event, response, attempts and timing, with filters, copyable event IDs, Redeliver and cursor paging. The product supplies data and actions; nothing is fetched here. Needed by: Developers, Chat, People, ID.',
      useWhen: [
        'An endpoint’s details page needs its delivery history.',
        'Admins must see and retry failed outbound deliveries (webhooks, notifications to other apps).',
      ],
      avoidWhen: [
        'A general request log with many columns and sorting: use DataTable.',
        'A single delivery’s full record: open LogDrawer from `onSelect`.',
      ],
    },
    designPrinciples: {
      items: [
        'Readable outcomes: the response is "HTTP 503 · 142 ms" plus the error in plain words, never a raw code alone.',
        'Status is a StatusBadge with a distinct icon per state, so colour is never the only signal.',
        'Data in, callbacks out: filtering can be local or on the server (`manualFiltering`), and Redeliver is whatever the product does.',
        'Works at phone width: below 40rem of container width each delivery becomes a labelled card.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        '`webhookDeliveryStatuses` is the built-in status registry; `filterWebhookDeliveries` is the local filter, exported for server-side reuse or tests.',
        'Forwards its ref to the root `<div>`; other div attributes pass through.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        '`filters`/`onFiltersChange` (controlled) or `defaultFilters` (uncontrolled). Debounce the search on your side before fetching when `manualFiltering` is on.',
        '`onRedeliver` may return a promise: the row button shows a spinner until it settles and the outcome is announced.',
        '`error` accepts `true` (default message) or your own message.',
        'Migrating from the app copy: Developers `DeliveriesPanel` → `WebhookDeliveries`. `WebhookDelivery.responseCode`, `attempts`, `eventType`, `eventId` keep their names; `latencyMs` → `durationMs`; `deliveredAt ?? lastAttemptAt ?? createdAt` → `at`; `deliveryErrorText(error)` → `error`; `isAdmin && endpoint.status === "enabled"` → pass `onRedeliver` only then; `onSendTest` → `emptyAction`; `useWebhookDeliveries` paging → `hasMore`/`onLoadMore`/`loadingMore`; server filters → `manualFiltering` + `filters` + `onFiltersChange`, `endpoint.events` → `eventTypes`. Chat `OutgoingWebhooks` `Deliveries`: map PROCESSED/RECEIVED/FAILED/DEAD to succeeded/pending/retrying/gone, `lastError` → `error`. People `DeliveryLog`: `target` → `destination` with `showDestination`, `counts` → `counts`, Try again → `onRedeliver`, Discard → `renderActions` with a ConfirmDialog.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'filterable',
          values: ['true', 'false'],
          description: 'Show the status, event type and event ID filters.',
        },
        {
          prop: 'showDestination',
          values: ['false', 'true'],
          description: 'Adds a Destination column for logs that cover several targets.',
        },
        {
          prop: 'manualFiltering',
          values: ['false', 'true'],
          description: 'Filter locally, or let the server do it and show `deliveries` as given.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Loading',
          attribute: 'data-loading, aria-busy',
          description: 'Skeleton rows and a hidden "Loading deliveries" status.',
        },
        { name: 'Error', description: 'InlineErrorRetry with Try again (`onRetry`).' },
        { name: 'Empty', description: 'First-run empty state with `emptyAction`.' },
        {
          name: 'Filtered empty',
          attribute: 'data-filtered',
          description: '"No deliveries match" with Clear filters.',
        },
        {
          name: 'Redelivering',
          attribute: 'aria-busy on the row button',
          description: 'Spinner until the promise settles; repeated presses are ignored.',
        },
        {
          name: 'Row status',
          attribute: 'data-status on each row',
          description: 'The delivery’s status key.',
        },
      ],
    },
    interaction: {
      items: [
        'Changing a filter updates the list (or calls `onFiltersChange` for the server); Clear filters resets all three.',
        'Event ID search matches the whole ID (any case); it also matches event types by substring.',
        'Redeliver is offered for rows that are not pending or sending, unless `canRedeliver` says otherwise.',
        'With `onSelect`, the event name is a button that opens the delivery.',
        'Load more appends the next page and announces how many arrived.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Tab',
          action: 'Filters, then each row’s event button, copy button and actions, then Load more.',
        },
        { keys: 'Enter / Space', action: 'Activates the focused button.' },
        { keys: 'Arrow keys', action: 'Choose an option in a focused filter (native select).' },
      ],
    },
    accessibility: {
      role: 'table (group for the filters)',
      name: 'The table is named by its caption (`messages.caption`, "Webhook deliveries").',
      items: [
        'Redeliver buttons are named per row ("Redeliver order.created evt_3c91a0").',
        'Redeliver outcomes and the counts line are polite live regions.',
        'In the stacked layout, visible cell labels are hidden from assistive technology; the column headers still name the cells.',
        'Filters have accessible names from `messages`.',
      ],
    },
    responsive: {
      items: [
        'A container query stacks rows into cards below 40rem, so there is no sideways scrolling at 390px.',
        'Filters wrap and go full width in narrow containers.',
        'Buttons, filters and the event button are at least 44px tall on touch screens.',
      ],
    },
    theme: {
      items: [
        'Built from Table, StatusBadge, Button and NativeSelect tokens, so every theme and more-contrast mode applies.',
        'Errors use `--mn-color-danger-strong`; forced-colours mode uses system colours.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Delivery log with redeliver',
          stack: true,
          render: () => (
            <WebhookDeliveries
              deliveries={sample}
              eventTypes={['order.created', 'order.updated', 'invoice.paid']}
              onRedeliver={wait}
            />
          ),
        },
        {
          title: 'Several destinations with counts',
          stack: true,
          render: () => (
            <WebhookDeliveries
              deliveries={sample.slice(1, 3).map((d, i) => ({
                ...d,
                destination: i === 0 ? 'Email' : 'Chat',
              }))}
              showDestination
              counts={{ succeeded: 128, retrying: 1, failed: 1 }}
              onRedeliver={wait}
              renderActions={() => (
                <Button size="sm" variant="ghost">
                  Discard
                </Button>
              )}
            />
          ),
        },
        {
          title: 'Empty, with a test action',
          stack: true,
          render: () => (
            <WebhookDeliveries
              deliveries={[]}
              filterable={false}
              emptyAction={<Button size="sm">Send a test event</Button>}
            />
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Translate short error codes into plain words before passing `error`.',
        'Pass `onRedeliver` only when the person may redeliver and the endpoint is enabled.',
        'Use `manualFiltering` when the log is paged on the server.',
      ],
      dont: [
        'Do not show payloads in the table; open them in LogDrawer if they are stored at all.',
        'Do not use the accent colour for statuses; keep the tone registry.',
      ],
    },
    related: {
      items: [
        { id: 'log-drawer', reason: 'Opens one delivery’s full record from `onSelect`.' },
        { id: 'data-table', reason: 'The table primitives it uses; for richer grids.' },
        { id: 'status-badge', reason: 'The status cell.' },
        { id: 'load-more', reason: 'The paging footer.' },
        { id: 'empty-state', reason: 'The empty and filtered states.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: [
        'button',
        'copy-button',
        'data-table',
        'load-more',
        'relative-time',
        'status-badge',
        'empty-state',
        'inline-error-retry',
        'skeleton',
        'form-field',
        'text-field',
      ],
    },
    testing: {
      unit: [
        'Named table with status, response, error, next attempt and copy buttons.',
        'Local filtering by status and event ID; Clear filters; filtered empty state.',
        '`manualFiltering` shows rows as given and reports filter changes.',
        'Redeliver tracks the promise, ignores repeat presses and announces success or failure.',
        '`onSelect` from the keyboard and `renderActions`.',
        'Counts, loading, error with Try again, empty with action, Load more.',
        'Custom statuses, messages and the Destination column.',
        '`filterWebhookDeliveries` matching rules.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
