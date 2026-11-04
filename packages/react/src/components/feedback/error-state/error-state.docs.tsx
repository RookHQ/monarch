import { Button, ErrorState, ErrorStateBoundary } from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';

export default {
  id: 'error-state',
  title: 'ErrorState',
  summary: 'A region failed to load: what failed, why in plain words, and a retry.',
  sections: {
    purpose: {
      summary:
        'ErrorState says what failed to load in a list, panel or card and offers a retry. ErrorStateBoundary catches a render error in one panel and shows an ErrorState instead of breaking the page.',
      useWhen: [
        'A list or panel failed to load (`onRetry`).',
        'The person may not see a record (`kind="forbidden"`), or it no longer exists (`kind="not-found"`).',
        'A request never reached the server (`kind="offline"`).',
        'One pane of a multi-pane screen should fail on its own (ErrorStateBoundary).',
      ],
      avoidWhen: [
        'There is simply no data: use EmptyState.',
        'A whole route failed or is missing: use ErrorPage.',
        'A small section failed inside a card that otherwise works: use InlineErrorRetry.',
        'Content is still loading: use LoadingState.',
      ],
    },
    designPrinciples: {
      items: [
        'ErrorState is an EmptyState with a kind icon and a primary "Try again" button.',
        'Plain language only. The product maps its errors to a kind (`errorKindFromStatus` helps) and writes the copy; never show a raw error message, stack or vendor name.',
        'Retry is offered only when it can help: `forbidden` and `not-found` never render it.',
        'The request id is there for support conversations: monospace, selectable and copyable.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'ErrorState accepts every EmptyState prop except `kind`, `icon` and `actions`, which it redefines; the ref is forwarded to the root `<div>`.',
        '`errorKindFromStatus(status, { offline })` maps an HTTP status to a kind: 0 or offline → `offline`, 401/403 → `forbidden`, 404/410 → `not-found`, 5xx → `server`, anything else → `error`. `maintenance` is never inferred.',
        '`errorKindIcon(kind)` returns the default decorative glyph for a kind, for products composing their own layout.',
        'ErrorStateBoundary is a client component (React error boundary): `fallback` (node or `({ error, reset }) => node`), `onError`, `onReset` and `resetKeys`; without `fallback` it renders ErrorState with the given `title`, `description` and a retry that resets it.',
        'LoadingState moved to its own folder in 0.4 (`loading-state`); it is still exported from `@rookcom/monarch-react` with the same props and class names.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [
        {
          prop: 'kind',
          values: ['error', 'forbidden', 'not-found', 'offline', 'server', 'maintenance'],
          description:
            'Sets the default icon and tint and whether retry is offered. Non-default kinds are exposed as `data-kind`.',
        },
        {
          prop: 'size',
          values: ['inline', 'page', 'compact'],
          description: 'From EmptyState. `compact` fits a side panel or table body.',
        },
        {
          prop: 'retryLabel',
          values: ['string'],
          description: 'Retry button text. Default "Try again".',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Error with retry',
          description:
            'A primary retry button is rendered when `onRetry` is set and the kind allows it, before any extra `actions`.',
        },
        {
          name: 'Retrying',
          attribute: 'aria-busy="true" on the retry button',
          description:
            '`retrying` shows the Button loading state: a spinner, clicks blocked, focus kept on the button.',
        },
        {
          name: 'Forbidden / not found',
          attribute: 'data-kind',
          description: 'Neutral icon tile, no retry button.',
        },
        {
          name: 'Offline',
          attribute: 'data-kind="offline"',
          description: 'Warning-tinted icon tile; retry stays available.',
        },
      ],
    },
    interaction: {
      items: [
        'The retry button calls `onRetry`; while `retrying` is true further clicks are ignored.',
        'The request-id copy button copies the id and announces "Copied to clipboard".',
        'ErrorStateBoundary: "Try again" clears the error and renders the children again; a change in `resetKeys` does the same.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Tab / Shift+Tab',
          action: 'Moves through retry, extra actions and the copy button.',
        },
        { keys: 'Enter / Space', action: 'Activates the focused button.' },
      ],
    },
    accessibility: {
      role: 'alert',
      items: [
        'ErrorState has role="alert", so it is announced when it appears.',
        'The icon tile is `aria-hidden`; the title carries the meaning.',
        'While retrying, the retry button stays focusable (aria-disabled + aria-busy), so keyboard users keep their place.',
        'The copy button is named "Copy Request ID" (from `requestIdLabel`).',
        'Verified with axe in jsdom and in Chromium, Firefox and WebKit on /components/error-state.',
      ],
    },
    responsive: {
      items: [
        'Centred with a 28rem maximum width (from EmptyState); actions wrap.',
        'Long request ids break anywhere instead of overflowing at 320px.',
      ],
    },
    theme: {
      items: [
        'The `error` and `server` icon tiles use the danger subtle and strong tokens; `offline` uses warning; `forbidden`, `not-found` and `maintenance` use the muted surface.',
        'Colours are Monarch semantic tokens only, so light, dark and more-contrast themes apply automatically.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Error with retry and request id',
          stack: true,
          render: () => (
            <ErrorState
              title="Couldn't load invoices"
              description="Check your connection and try again."
              onRetry={() => {}}
              requestId="req_7f3a91c2"
            />
          ),
        },
        {
          title: 'Forbidden and offline',
          stack: true,
          render: () => (
            <>
              <ErrorState
                kind="forbidden"
                size="compact"
                title="You can't view this project"
                description="Ask a workspace admin for access."
                actions={<Button>Request access</Button>}
              />
              <ErrorState
                kind="offline"
                size="compact"
                title="You're offline"
                description="We'll show the latest data once you reconnect."
                onRetry={() => {}}
              />
            </>
          ),
        },
        {
          title: 'Panel error boundary',
          stack: true,
          render: () => (
            <ErrorStateBoundary
              title="This panel didn't load"
              description="The rest of the page still works."
            >
              <p>Panel content renders here until it throws.</p>
            </ErrorStateBoundary>
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Say what failed in the title.',
        'Pass the request id when your API returns one.',
        'Map statuses to kinds with `errorKindFromStatus`, then pick your own copy per kind.',
        'Wrap independent panes in ErrorStateBoundary so one failure does not blank the screen.',
      ],
      dont: [
        'Do not show raw error messages, codes or vendor names as the title.',
        'Do not offer retry for a permission error.',
      ],
    },
    related: {
      items: [
        { id: 'empty-state', reason: 'No data yet.' },
        { id: 'inline-error-retry', reason: 'Compact retry for one section inside a card.' },
        { id: 'error-page', reason: 'The whole route failed or is missing.' },
        { id: 'loading-state', reason: 'Shown before the data arrives.' },
      ],
    },
    dependencies: { packages: ['react'], components: ['button', 'copy-button', 'empty-state'] },
    testing: {
      unit: [
        'ErrorState is an alert and calls `onRetry`; retry renders before extra actions.',
        'Default props keep the 0.3 markup (no `data-kind`).',
        '`forbidden` and `not-found` hide retry; `offline` keeps it.',
        '`retrying` keeps focus on the busy button and blocks repeat activation.',
        'The request id renders as code with a named copy button.',
        '`errorKindFromStatus` maps statuses and offline to kinds.',
        'ErrorStateBoundary catches a render error without showing its message, calls `onError`, resets on retry and on `resetKeys` change, and accepts a render-prop fallback.',
      ],
      axe: true,
      ssr: true,
      browser: ['Visual baselines: /__gallery', 'Cross-browser axe: /components/error-state'],
    },
  },
} satisfies ComponentDocs;
