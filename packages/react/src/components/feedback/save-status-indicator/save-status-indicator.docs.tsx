import type { ComponentProps } from 'react';
import { SaveStatusIndicator } from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';
// The scenario of the primary example, shared with iOS and Android (CROSS-PLATFORM.md section 8).
import fixture from './save-status-indicator.fixture.json';

type Status = NonNullable<ComponentProps<typeof SaveStatusIndicator>['status']>;
const [unsaved, saving, saved, failed] = fixture.data.states as unknown as readonly [
  Status,
  Status,
  Status,
  Status,
];

export default {
  id: 'save-status-indicator',
  title: 'SaveStatusIndicator',
  summary: '"Saving…", "Saved · 2 minutes ago" or "Couldn\'t save · Retry", announced politely.',
  sections: {
    purpose: {
      summary:
        'Tells people whether their work is safe without interrupting them. Needed by: Rook Dossier (`components/document/EditorTopBar.tsx`, its own saving/saved labels) and Rook Tickets (`components/tickets/detail/Composer.tsx`, "Saving draft… / Draft saved / Draft not saved").',
      useWhen: [
        'An editor or form saves in the background (autosave, drafts).',
        'A manual save needs a quiet confirmation next to the Save button.',
      ],
      avoidWhen: [
        'You also need the debounce and retry logic: use AutosaveIndicator with `useAutosave`, which renders this component.',
        'The save failed in a way that blocks the person: show an Alert or InlineErrorRetry with the full explanation.',
      ],
    },
    designPrinciples: {
      items: [
        'Quiet by default: small muted text, colour only for the outcome.',
        'Announce outcomes, not activity: "Saved" and errors reach screen readers; "Saving…" only with `announceSaving`.',
        'Always offer a way out of an error: pass `onRetry` to show Retry.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Forwards its ref to the root `<div>`. Fully controlled: the caller passes `status` (or use AutosaveIndicator).',
        '`defaultSaveStatusMessages` holds the English strings for reuse in translations.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        '`savedAt` accepts a Date, epoch milliseconds or an ISO string (RelativeTime `DateInput`). Pass `now` for byte-identical server rendering.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'status',
          values: ['idle', 'unsaved', 'saving', 'saved', 'error'],
          description:
            '`idle` shows nothing; the others show an icon and text. Exposed as `data-status`.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Idle',
          attribute: 'data-status="idle"',
          description: 'Empty; the live region stays mounted.',
        },
        {
          name: 'Unsaved',
          attribute: 'data-status="unsaved"',
          description: 'A warning-coloured dot and "Unsaved changes".',
        },
        {
          name: 'Saving',
          attribute: 'data-status="saving"',
          description: 'A small spinner and "Saving…".',
        },
        {
          name: 'Saved',
          attribute: 'data-status="saved"',
          description: 'A success check, "Saved" and, with `savedAt`, a live relative time.',
        },
        {
          name: 'Error',
          attribute: 'data-status="error"',
          description: 'Danger text, the error message and, with `onRetry`, a Retry button.',
        },
      ],
    },
    interaction: {
      items: [
        'Retry calls `onRetry`; the caller moves the status back to `saving`.',
        'The relative time updates itself as it ages (RelativeTime `live`).',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab, Enter / Space', action: 'Reaches and activates Retry when it is shown.' },
      ],
      notes: ['Nothing else is focusable.'],
    },
    accessibility: {
      role: 'a polite status live region (role="status") inside a plain container',
      name: 'Not named; the text is read in place.',
      items: [
        'Changes to Saved or Error are announced once through a visually hidden `role="status"` region; repeated saves are announced again.',
        'Saving is silent unless `announceSaving` is set, so frequent autosaves do not interrupt.',
        'Icons and the "·" separator are hidden from assistive technology.',
        'The time is a `<time datetime>` element.',
      ],
    },
    responsive: {
      items: [
        'Wraps onto two lines in narrow headers instead of overflowing.',
        'Retry is at least 44 × 44 px on coarse pointers.',
      ],
    },
    theme: {
      items: [
        'Text uses `--mn-color-text-muted`; the saved check uses success, the unsaved dot warning and errors danger tokens, in every theme.',
        'In forced-colors mode all text and icons use CanvasText.',
      ],
    },
    examples: {
      items: [
        {
          title: 'States',
          stack: true,
          render: () => (
            <>
              <SaveStatusIndicator status={unsaved} />
              <SaveStatusIndicator status={saving} />
              <SaveStatusIndicator
                status={saved}
                savedAt={Date.now() - fixture.data.savedAgoSeconds * 1000}
              />
              <SaveStatusIndicator status={failed} onRetry={() => {}} />
            </>
          ),
        },
        {
          title: 'Specific error',
          render: () => (
            <SaveStatusIndicator status="error" error="You are offline" onRetry={() => {}} />
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Place it where people look after editing: the editor header or next to Save.',
        'Pass `savedAt` so people can see how fresh the last save is.',
      ],
      dont: [
        'Do not use it as the only error message for a blocking failure.',
        'Do not announce "Saving…" on every keystroke-driven save.',
      ],
    },
    related: {
      items: [
        {
          id: 'autosave-indicator',
          reason: 'Adds debounce, retry and status tracking (`useAutosave`).',
        },
        { id: 'form-actions-bar', reason: 'Save and Discard for forms saved by hand.' },
        { id: 'relative-time', reason: 'The relative time it shows.' },
        { id: 'inline-error-retry', reason: 'A larger inline error with Retry.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['button', 'relative-time', 'spinner'],
    },
    testing: {
      unit: [
        'Shows Saving, then Saved with a relative time; only Saved is announced.',
        'Shows and announces the error with a working Retry button.',
        'Supports a custom error, custom messages and `announceSaving`.',
        'Hides icons and the separator from assistive technology.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
