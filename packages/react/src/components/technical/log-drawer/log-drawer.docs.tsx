import { useState } from 'react';
import { Button } from '@rookcom/monarch-react';
import { LogDrawer } from '@rookcom/monarch-react/patterns';
import type { ComponentDocs } from '../../../internal/component-docs.js';

function RequestDetails() {
  const [open, setOpen] = useState(false);
  return (
    <>
      <Button onClick={() => setOpen(true)}>View request</Button>
      <LogDrawer
        open={open}
        onOpenChange={setOpen}
        description="Request and response bodies are not stored."
        request={{
          method: 'POST',
          path: '/v1/orders',
          status: 429,
          errorCode: 'rate_limited',
          id: 'req_7f3a2c90d1',
          at: new Date(Date.now() - 4 * 60_000),
          durationMs: 84,
        }}
        fields={[
          { label: 'Credential', value: 'Reporting job' },
          {
            label: 'Origin',
            value: 'https://shop.example.com',
            mono: true,
            copyValue: 'https://shop.example.com',
          },
          { label: 'Units', value: '1 request' },
        ]}
      />
    </>
  );
}

function DeliveryDetails() {
  const [open, setOpen] = useState(false);
  return (
    <>
      <Button onClick={() => setOpen(true)}>View delivery</Button>
      <LogDrawer
        open={open}
        onOpenChange={setOpen}
        title="Delivery details"
        request={{ method: 'POST', status: 200, id: 'evt_3c91a0', durationMs: 142 }}
        messages={{ requestId: 'Event ID', copyRequestId: 'Copy event ID' }}
        fields={[{ label: 'Event', value: 'order.created', mono: true }]}
      >
        <pre
          style={{ margin: 0, fontFamily: 'var(--mn-font-mono)', fontSize: 'var(--mn-text-xs)' }}
        >
          {'{\n  "id": "evt_3c91a0",\n  "type": "order.created"\n}'}
        </pre>
      </LogDrawer>
    </>
  );
}

function Loading() {
  const [open, setOpen] = useState(false);
  return (
    <>
      <Button onClick={() => setOpen(true)}>Open while loading</Button>
      <LogDrawer open={open} onOpenChange={setOpen} loading />
    </>
  );
}

export default {
  id: 'log-drawer',
  title: 'LogDrawer',
  summary: 'A side drawer with the full record of one log entry: request, status, ID and facts.',
  sections: {
    purpose: {
      summary:
        'Opens one log entry (an API request, a webhook delivery, a job run) beside the list it came from, with its method, path, status, copyable ID, time and duration, then whatever facts and content the product adds. Needed by: Developers.',
      useWhen: [
        'A log table needs a details view without leaving the page.',
        'A delivery or request has more facts than fit in a row.',
      ],
      avoidWhen: [
        'The record deserves its own URL and page: use a details page.',
        'Editing anything: a log entry is read-only; use a Sheet with a form.',
      ],
    },
    designPrinciples: {
      items: [
        'Summary first: operation, status and ID come before product-specific facts.',
        'Every ID can be copied; empty facts show a dash read as "Not set".',
        'Status is a StatusBadge toned by HTTP class (2xx success, 3xx info, 4xx warning, 5xx danger).',
        'No fetching: open it with what the list already has, or pass `loading` while you fetch more.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Controlled: `open` and `onOpenChange`. Typically `open={selected !== null}`.',
        '`httpStatusTone(status)` is exported for tables that colour status cells the same way.',
        'Forwards its ref to the sheet content; other SheetContent props (`side`, `size`) pass through. Default `side="right"`, `size="md"`.',
        'Focus returns to what had focus when it opened (usually the row’s button).',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Every part of `request` is optional; rows are shown only for what is given.',
        '`fields` render in order after the summary; `children` render after the fields.',
        'Migrating from the app copy: Developers `LogDrawer` → `LogDrawer`. `log` → `open={log !== null}` + `request={{ method, path: operation, status, errorCode, id: requestId, at: createdAt, durationMs: latencyMs }}`; `onClose` → `onOpenChange`; Credential, API, Origin and Units rows → `fields`; the "bodies are not stored" line → `description`; `MethodBadge`/`HttpStatusBadge` are built in.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'side',
          values: ['right', 'left', 'bottom', 'top'],
          description: 'The edge it opens from. Default `right`.',
        },
        {
          prop: 'size',
          values: ['sm', 'md', 'lg', 'xl', 'full'],
          description: 'Sheet width. Default `md` (28rem), never wider than the screen.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Loading',
          attribute: 'aria-busy on the placeholder',
          description: 'Skeleton rows and a hidden "Loading details" status.',
        },
        { name: 'Error', description: 'InlineErrorRetry with Try again (`onRetry`).' },
        {
          name: 'Status tone',
          attribute: 'data-status-tone',
          description: 'success, info, warning, danger or neutral from the HTTP status.',
        },
        {
          name: 'Empty fact',
          attribute: 'data-empty on the item',
          description: 'Dash with "Not set" for screen readers.',
        },
      ],
    },
    interaction: {
      items: [
        'Escape, an outside click or the close button closes it.',
        'Copy buttons copy the request ID and any field with `copyValue`, and confirm in place.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Tab / Shift+Tab',
          action: 'Moves between copy buttons, your content and Close; focus stays in the drawer.',
        },
        { keys: 'Escape', action: 'Closes and returns focus.' },
        { keys: 'Enter / Space', action: 'Activates the focused button.' },
      ],
    },
    accessibility: {
      role: 'dialog (modal)',
      name: 'The title (`title` or "Request details"); `description` is the accessible description.',
      items: [
        'Facts are a description list: terms are `<dt>`, values `<dd>`.',
        'Copy buttons are named ("Copy request ID", "Copy Origin").',
        'The status shows its code as text, not colour alone.',
      ],
    },
    responsive: {
      items: [
        'The sheet is capped at the screen width, so at 390px it fills the screen.',
        'Long IDs, paths and hosts wrap anywhere instead of scrolling sideways.',
        'Copy buttons are 44px targets on touch screens.',
      ],
    },
    theme: {
      items: [
        'Built from Sheet, DescriptionList and StatusBadge, so every theme and more-contrast mode applies.',
        'The method chip uses `--mn-color-bg-muted` and the mono font; forced-colours mode draws it with system colours.',
      ],
    },
    examples: {
      items: [
        { title: 'API request', render: () => <RequestDetails /> },
        { title: 'Webhook delivery with payload', render: () => <DeliveryDetails /> },
        { title: 'Loading', render: () => <Loading /> },
      ],
    },
    doDont: {
      do: [
        'Open it from a button in the row (WebhookDeliveries `onSelect` does this).',
        'Say in `description` what is not recorded, so nobody looks for it.',
      ],
      dont: [
        'Do not put secrets or full payloads in it unless the product stores and may show them.',
        'Do not use it for editing.',
      ],
    },
    related: {
      items: [
        { id: 'webhook-deliveries', reason: 'A delivery log whose rows open this drawer.' },
        { id: 'sheet', reason: 'The drawer it is built on.' },
        { id: 'description-list', reason: 'The facts list.' },
        { id: 'method-badge', reason: 'For HTTP methods in tables.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: [
        'description-list',
        'relative-time',
        'status-badge',
        'inline-error-retry',
        'skeleton',
        'sheet',
      ],
    },
    testing: {
      unit: [
        'Named dialog with description, operation, status tone, error code, duration, fields and empty placeholder.',
        'Copies the request ID and copyable fields.',
        'Closes with Escape and the close button; focus returns to the opener.',
        'Loading and error with Try again.',
        'Custom title, children and messages.',
        '`httpStatusTone` mapping.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
