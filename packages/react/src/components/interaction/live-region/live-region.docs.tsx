import { useState } from 'react';
import { AnnouncerProvider, Button, LiveRegion, useAnnouncer } from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';

function CounterDemo() {
  const [count, setCount] = useState(0);
  return (
    <div style={{ display: 'flex', gap: 12, alignItems: 'center', flexWrap: 'wrap' }}>
      <Button onClick={() => setCount((c) => c + 1)}>Add item</Button>
      <LiveRegion visuallyHidden={false}>
        {count === 0 ? 'No items yet.' : `${count} ${count === 1 ? 'item' : 'items'} in the list.`}
      </LiveRegion>
    </div>
  );
}

function SaveButtons() {
  const { announce } = useAnnouncer();
  return (
    <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
      <Button onClick={() => announce('Draft saved.')}>Save draft</Button>
      <Button
        variant="secondary"
        onClick={() => announce('Could not save. Try again.', 'assertive')}
      >
        Simulate an error
      </Button>
    </div>
  );
}

function AnnouncerDemo() {
  return (
    <AnnouncerProvider>
      <SaveButtons />
    </AnnouncerProvider>
  );
}

export default {
  id: 'live-region',
  title: 'LiveRegion',
  summary:
    'A polite or assertive live region, plus `useAnnouncer()` to read a message aloud from any event handler.',
  sections: {
    purpose: {
      summary:
        'Tells screen-reader users about changes they cannot see happen: results counts, saves, uploads, the steps of a drag. Every Rook app hand-rolls `aria-live` spans today (rook-support-chat, rookatlas, rookbase, rookcalendar, rookchat, rookdevelopers, rookdossier, rookdots, rookfriends, rookid, rookintellect, rookpeople, rookpulse, rookrecruit, rookstride and rooktickets frontends); this is the one shared way. Needed by: all of them, and by SortableGrid, ReorderableList-style widgets and filters.',
      useWhen: [
        'Something changed on the page without focus moving: a filter updated the results, an autosave finished, a background job completed.',
        'A widget needs to narrate a keyboard interaction, such as each step of a drag.',
        'An event handler needs to speak once (`useAnnouncer().announce(...)`) without adding visible text.',
      ],
      avoidWhen: [
        'The message needs to be seen and acted on: use Toast, Alert or InlineNotice (they already announce).',
        'Focus moves to the new content (a dialog, a page): the move itself is announced.',
        'Every keystroke would announce, such as a character counter: debounce or announce only at limits.',
      ],
    },
    designPrinciples: {
      items: [
        'Polite by default. Assertive interrupts the user, so keep it for errors and for direct feedback on what they are doing right now.',
        'The region exists before the message: live regions inserted together with their text are often not read, so render `LiveRegion` empty first or use `useAnnouncer()`, whose regions are already mounted.',
        'Repeating the same message is read again: identical text gets an invisible change.',
        'Messages do not linger: `AnnouncerProvider` empties its regions after `clearAfter` (7 seconds), so stale text is not found while browsing.',
        'Works without setup: with no provider, `useAnnouncer()` uses two hidden regions it adds to `<body>` the first time it speaks.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        '`LiveRegion` renders a `<div aria-live aria-atomic class="mn-live-region">`, visually hidden unless `visuallyHidden={false}`.',
        '`useAnnouncer()` returns `{ announce(message, politeness?), clear() }`. `AnnouncerProvider` renders the two regions after its children.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Migrating from hand-written regions: `<span className="sr-only" aria-live="polite">{msg}</span>` becomes `<LiveRegion>{msg}</LiveRegion>`; a `role="status"` span used only to announce becomes `announce(msg)`.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'politeness',
          values: ['polite', 'assertive'],
          description: 'Polite waits for a pause; assertive interrupts.',
        },
        {
          prop: 'visuallyHidden',
          values: ['true', 'false'],
          description: 'Hidden (default) or also shown as small muted text.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Politeness',
          attribute: 'data-politeness="polite|assertive"',
          description: 'Mirrors `aria-live`, for styling the visible variant.',
        },
        {
          name: 'Announcer region',
          attribute: 'data-mn-announcer',
          description:
            'Marks the regions owned by `AnnouncerProvider` (empty value) or the page-level fallback (`global`).',
        },
      ],
    },
    interaction: {
      items: [
        '`announce(text)` writes into the polite region and empties the assertive one (and the other way round), so only the latest message is present.',
        'The same text twice in a row gets a trailing no-break space, which screen readers treat as a change.',
        'Inside a modal dialog, put an `AnnouncerProvider` in the dialog: regions outside an `aria-modal` dialog may be ignored by some screen readers.',
      ],
    },
    keyboard: {
      items: [],
      notes: ['Not interactive: it has no focusable parts.'],
    },
    accessibility: {
      role: 'none (an aria-live region)',
      name: 'Not needed: a live region is read by its content.',
      items: [
        '`aria-atomic="true"` by default so the whole message is read, not only the changed words.',
        'Text stays in the DOM until replaced or cleared, so it is reachable when browsing.',
        'Checked with axe in jsdom.',
      ],
    },
    responsive: {
      items: [
        'Hidden regions take no space. The visible variant wraps long text (`overflow-wrap: anywhere`) and works at 320px.',
      ],
    },
    theme: {
      items: [
        'Visible variant: `--mn-color-text-muted` at `--mn-text-sm`.',
        'Forced-colours mode uses CanvasText.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Visible status text that is also announced',
          render: () => <CounterDemo />,
        },
        {
          title: 'Announce from event handlers',
          render: () => <AnnouncerDemo />,
        },
      ],
    },
    doDont: {
      do: [
        'Keep messages short and complete: “12 results”, “Draft saved.”',
        'Announce the outcome, not the action: “Filters applied, 4 results” rather than “Applying filters”.',
        'Put an `AnnouncerProvider` near the root of the app, and one inside custom modals.',
      ],
      dont: [
        'Do not use assertive for routine confirmations.',
        'Do not mount a live region in the same render as its first message.',
        'Do not duplicate what a Toast or Alert already announces.',
      ],
    },
    related: {
      items: [
        { id: 'toast', reason: 'A visible, dismissible message that is announced for you.' },
        { id: 'alert', reason: 'A persistent visible status message in the page.' },
        { id: 'visually-hidden', reason: 'Hides text without making it live.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: [],
    },
    testing: {
      unit: [
        'LiveRegion defaults (polite, atomic, hidden) and the assertive, non-atomic, visible variant; ref; axe.',
        'useAnnouncer through a provider: polite default, assertive empties polite; axe.',
        'The same text twice changes the region; clear() and clearAfter empty it.',
        'Without a provider, page-level regions are added to <body>.',
        'Server rendering.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
