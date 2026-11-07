import { useState } from 'react';
import { DataExportRequest, type DataExportStatus } from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const wait = (ms: number) => new Promise<void>((resolve) => window.setTimeout(resolve, ms));

const scope = [
  { id: 'profile', label: 'Profile and settings', description: 'Name, email, preferences.' },
  { id: 'content', label: 'Projects and documents', description: 'Everything you created.' },
  { id: 'activity', label: 'Activity history', description: 'Sign-ins and changes you made.' },
];

const formats = [
  { value: 'zip', label: 'ZIP archive (JSON files)' },
  { value: 'csv', label: 'CSV spreadsheets' },
];

const note = 'Preparing an export can take up to two days. We will email you when it is ready.';

/** Walks through requested → preparing → ready, as a backend would report it. */
function Lifecycle() {
  const [status, setStatus] = useState<DataExportStatus>('idle');
  const [requestedAt, setRequestedAt] = useState<string>();
  const [readyAt, setReadyAt] = useState<string>();
  const request = async () => {
    await wait(700);
    setRequestedAt(new Date().toISOString());
    setStatus('requested');
    await wait(1500);
    setStatus('preparing');
    await wait(2500);
    setReadyAt(new Date().toISOString());
    setStatus('ready');
  };
  return (
    <DataExportRequest
      status={status}
      {...(requestedAt ? { requestedAt } : {})}
      {...(readyAt ? { readyAt, expiresAt: Date.parse(readyAt) + 7 * 86_400_000 } : {})}
      scope={scope}
      formats={formats}
      preparationNote={note}
      onRequest={request}
      onDownload={() => setStatus('expired')}
    />
  );
}

export default {
  id: 'data-export-request',
  title: 'DataExportRequest',
  summary:
    'A “Download your data” card: choose what to include and the format, request, follow progress in words, download until it expires.',
  sections: {
    purpose: {
      summary:
        'Lets a person ask for a copy of their account data and pick it up when it is ready. The card shows each status the product reports (requested, preparing, ready, expired, failed) as a badge and a sentence with dates, keeps the request form for the states where requesting makes sense, and offers the download link while it is valid. The product builds the file and decides what it contains, how long it takes and how long it stays available; the product’s legal team decides the wording.',
      useWhen: [
        'An account or privacy settings page offers a self-service copy of the person’s data.',
        'An export takes time to prepare and the person comes back later to download it.',
      ],
      avoidWhen: [
        'An instant download of one table or report: a Button that downloads the file is enough.',
        'A formal privacy request handled by people (access, deletion, correction): use DataSubjectRequestForm.',
      ],
    },
    designPrinciples: {
      items: [
        'Progress is text first: the badge and status sentence say where the export is; the indeterminate progress bar while preparing is an extra cue, never the only one.',
        'Everything is included by default; the person removes what they do not want.',
        'Dates come from the product (`requestedAt`, `readyAt`, `expiresAt`) and are formatted with Intl in the given locale and time zone.',
        'No promised timings: how long preparing takes is the product’s `preparationNote`.',
        'Built from Card, Badge, Checkbox, NativeSelect, Progress, Alert and Button.',
      ],
    },
    apiReference: { source: 'typescript' },
    typedProperties: {
      source: 'typescript',
      notes: [
        '`onRequest` receives `{ scope, format? }`: the ticked scope ids in option order (empty without `scope`) and the chosen format when `formats` is given.',
        'Status sentences in `messages` are functions that receive `<time>` elements (or null when the date is missing), so they can be reworded around the dates.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'status',
          values: ['idle', 'requested', 'preparing', 'ready', 'expired', 'failed'],
          description:
            'Where the export is; drives the badge, the sentence and which actions show.',
        },
        {
          prop: 'scope',
          values: ['undefined', 'DataExportScopeOption[]'],
          description: 'A checklist of what to include, all ticked at first.',
        },
        {
          prop: 'formats',
          values: ['undefined', 'DataExportFormatOption[]'],
          description: 'A format select; the first option (or `defaultFormat`) is chosen.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Idle',
          attribute: 'data-status="idle"',
          description: 'Nothing requested: scope, format, note and the request button.',
        },
        {
          name: 'Requested',
          attribute: 'data-status="requested"',
          description: '“We received your request on …” and the note; no actions.',
        },
        {
          name: 'Preparing',
          attribute: 'data-status="preparing"',
          description: 'A sentence, the note and an indeterminate progress bar.',
        },
        {
          name: 'Ready',
          attribute: 'data-status="ready"',
          description:
            'When it was prepared, until when it can be downloaded, and the download button or link.',
        },
        {
          name: 'Expired / failed',
          attribute: 'data-status="expired|failed"',
          description:
            'What happened (and `failureReason`), with the form to request a new export.',
        },
        {
          name: 'Requesting',
          attribute: 'data-pending, aria-busy',
          description: 'While the promise from `onRequest` is pending; a rejection shows an alert.',
        },
        {
          name: 'Nothing chosen',
          attribute: 'data-invalid (scope fieldset)',
          description:
            'Submitting with every scope option unticked shows an error and focuses the first checkbox.',
        },
      ],
    },
    interaction: {
      items: [
        'Request runs `onRequest` with the choices; the product then changes `status`.',
        'Repeated clicks while a request is pending are ignored.',
        'With `downloadHref` the download is a link with the `download` attribute; `onDownload` is called too. Without it, a button calls `onDownload` (for example to fetch a signed URL).',
        'Status changes are announced through a polite live region.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Tab / Shift+Tab',
          action: 'Moves through the checkboxes, the select and the buttons.',
        },
        { keys: 'Space', action: 'Ticks or unticks a scope checkbox.' },
        { keys: 'Enter / Space', action: 'Requests the export or starts the download.' },
      ],
    },
    accessibility: {
      role: 'group (the card), with a form and a fieldset',
      name: 'The title ("Download your data"), through `aria-labelledby`.',
      items: [
        'The status sentence sits in an `aria-live="polite"` region, so a change of status is read out.',
        'The badge has a dot and text; status never depends on colour.',
        'The progress bar is named ("Preparing your export") and indeterminate (no value).',
        'The scope fieldset is described by its error when nothing is chosen; focus moves to the first checkbox.',
        'A failed request is announced with `role="alert"`.',
      ],
    },
    responsive: {
      items: [
        'Works at 390px: the title and badge wrap, dates wrap, and below 30rem the buttons and the select take the full width.',
        'Buttons are 44px tall on coarse pointers.',
      ],
    },
    theme: {
      items: [
        'Card, Badge, Checkbox, NativeSelect and Progress bring their own token styles.',
        'The failed sentence and the scope error use `--mn-color-danger-strong`; CanvasText in forced-colors mode.',
      ],
    },
    examples: {
      items: [
        { title: 'Request, prepare and download', render: () => <Lifecycle /> },
        {
          title: 'Preparing',
          render: () => (
            <DataExportRequest
              status="preparing"
              requestedAt="2026-05-04T10:00:00.000Z"
              preparationNote={note}
              locale="en-GB"
              timeZone="UTC"
            />
          ),
        },
        {
          title: 'Ready to download',
          render: () => (
            <DataExportRequest
              status="ready"
              readyAt="2026-05-05T08:00:00.000Z"
              expiresAt="2026-05-12T08:00:00.000Z"
              downloadHref="#"
              locale="en-GB"
              timeZone="UTC"
            />
          ),
        },
        {
          title: 'Failed, with a reason',
          render: () => (
            <DataExportRequest
              status="failed"
              failureReason="Something went wrong while building the archive."
              scope={scope}
              onRequest={() => wait(800)}
            />
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Say in `preparationNote` how long preparing usually takes and how the person will be told.',
        'Pass `expiresAt` so the person knows how long the download stays available.',
        'Keep `status` in step with the backend (poll or push).',
      ],
      dont: [
        'Do not promise a legal deadline in the default copy; that wording belongs to the product’s legal team.',
        'Do not pre-untick scope options to steer what people download.',
        'Do not show a percentage you do not really know; the bar is indeterminate on purpose.',
      ],
    },
    related: {
      items: [
        { id: 'data-subject-request-form', reason: 'Formal privacy requests handled by people.' },
        {
          id: 'consent-history',
          reason: 'The person’s consent decisions, with their own download.',
        },
        { id: 'delete-account-flow', reason: 'Often offered next to the export on the same page.' },
        { id: 'card', reason: 'The frame it is built on.' },
        { id: 'progress-bar', reason: 'The indeterminate bar while preparing.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['button', 'badge', 'card', 'progress-bar', 'alert', 'checkbox', 'form-field'],
    },
    testing: {
      unit: [
        'Requests with the ticked scope and chosen format; everything is ticked at first; axe passes.',
        'Nothing ticked: error, focus on the first checkbox, no request.',
        'Busy while onRequest is pending, repeat clicks ignored, rejection shown as an alert.',
        'Requested and preparing sentences with dates in a fixed locale and zone; indeterminate progress bar; axe passes.',
        'Ready: dates, download link with `download`, or a button calling onDownload; axe passes.',
        'Expired and failed offer a new request and show the reason.',
        'Strings come from messages; renders on the server.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
