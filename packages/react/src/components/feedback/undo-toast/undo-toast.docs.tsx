import { useState } from 'react';
import { Button, IconButton, ToastProvider, useUndoToast } from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';
import { TrashIcon } from '../../../internal/docs-demo.js';

const NOTES = ['Agenda for Monday', 'Questions for the review', 'Ideas for the next sprint'];

function NoteList() {
  const [notes, setNotes] = useState(NOTES);
  const [log, setLog] = useState('');
  const { undoable } = useUndoToast();

  const remove = async (note: string) => {
    const index = notes.indexOf(note);
    // Act at once: the note disappears before anything is asked.
    setNotes((list) => list.filter((n) => n !== note));
    const outcome = await undoable({
      title: 'Note deleted',
      description: note,
      onUndo: () =>
        setNotes((list) => {
          const next = [...list];
          next.splice(index, 0, note);
          return next;
        }),
      // A deferred server delete would run here.
      onCommit: () => setLog(`“${note}” was deleted for good.`),
    });
    if (outcome === 'undone') setLog(`“${note}” was restored.`);
  };

  return (
    <div style={{ display: 'grid', gap: 8, maxWidth: 360 }}>
      <ul style={{ display: 'grid', gap: 4, margin: 0, padding: 0, listStyle: 'none' }}>
        {notes.map((note) => (
          <li key={note} style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            <span style={{ flex: 1 }}>{note}</span>
            <IconButton
              label={`Delete ${note}`}
              icon={<TrashIcon />}
              variant="ghost"
              onClick={() => void remove(note)}
            />
          </li>
        ))}
      </ul>
      {notes.length === 0 ? <Button onClick={() => setNotes(NOTES)}>Reset the list</Button> : null}
      {log ? <p style={{ margin: 0 }}>{log}</p> : null}
    </div>
  );
}

function CountdownTile() {
  const [visible, setVisible] = useState(true);
  const { undoable } = useUndoToast();
  return visible ? (
    <Button
      variant="danger-subtle"
      onClick={() => {
        setVisible(false);
        void undoable({
          title: 'Tile deleted',
          countdown: true,
          duration: 10000,
          onUndo: () => setVisible(true),
        });
      }}
    >
      Delete the chart tile
    </Button>
  ) : (
    <p style={{ margin: 0 }}>The tile is gone. Undo it from the notification.</p>
  );
}

function QuietUndo() {
  const [removed, setRemoved] = useState(false);
  const { undoable } = useUndoToast({ messages: { undo: 'Bring back' } });
  if (removed) {
    return <Button onClick={() => setRemoved(false)}>Add the view again</Button>;
  }
  return (
    <Button
      onClick={() => {
        setRemoved(true);
        void undoable({
          title: 'View removed from the sidebar',
          restoredTitle: false,
          onUndo: () => setRemoved(false),
        });
      }}
    >
      Remove the view
    </Button>
  );
}

export default {
  id: 'undo-toast',
  title: 'useUndoToast',
  summary:
    'Act now, offer Undo: a toast for reversible, low-stakes deletes, with deferred commit and a restored confirmation.',
  sections: {
    purpose: {
      summary:
        'The preferred way to delete something small and reversible (a note, a comment, a tile, a saved view): do it at once and show "Note deleted. Undo" for a few seconds, instead of asking "Are you sure?" first. It is built on the toast system and tells you exactly once whether the person undid the action or let it stand, so a server delete can be deferred until then.',
      useWhen: [
        'Deleting, archiving or hiding one item that can be restored exactly as it was.',
        'Frequent actions where a confirmation dialog would slow people down.',
        'Deletes that can wait a few seconds on the server (`onCommit`), or that the server can reverse (`onUndo`).',
      ],
      avoidWhen: [
        'The action cannot be undone, or affects other people at once (a sent message, a revoked key): use ConfirmDialog.',
        'Large or bulk deletes where seeing the consequences first matters: confirm instead.',
        'The undo would have to recreate a lot of state the app no longer has.',
      ],
    },
    designPrinciples: {
      items: [
        'Act first, ask never: the change is visible at once, and Undo is the safety net.',
        'One outcome, once: `onUndo` or `onCommit` runs, never both and never twice, and the promise says which.',
        'Enough time to react: 8 seconds by default, kept between 5 and 10, paused while the toast is hovered or focused, and stretched by the provider’s `durationScale`.',
        'Close the loop quietly: after Undo a short, polite "Restored" toast confirms it, without moving focus.',
        'Name what happened in the past tense ("Note deleted"), and the item in the description.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        '`useUndoToast({ messages? })` returns `{ undoable }`. It must be used inside `ToastProvider`.',
        '`undoable({ title, description?, undoLabel?, undoAltText?, duration?, onUndo, onCommit?, restoredTitle?, countdown? })` returns `Promise<"undone" | "committed">`.',
        'Undo: `onUndo` runs, then the "Restored" toast shows and the promise resolves `"undone"`. If `onUndo` throws or rejects, an error toast shows (`getErrorMessage`, falling back to `messages.undoFailed`) and the promise rejects.',
        'Everything else commits: the timer running out, Dismiss, Escape, a swipe, a newer toast pushing it out of the `limit`, or the page being hidden (`pagehide`, so a deferred delete is not lost when the tab closes). `onCommit` runs and the promise resolves `"committed"`; if `onCommit` rejects, the promise rejects.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        '`messages` replaces `undo` ("Undo"), `restored` ("Restored") and `undoFailed` for translation.',
        '`duration` is clamped to 5000–10000 ms; `Infinity` keeps the toast until it is dismissed.',
        '`restoredTitle: false` turns the confirmation off for one call.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'countdown',
          values: ['false', 'true'],
          description:
            'A thin bar along the bottom of the toast that shrinks over `duration` and pauses with it. Hidden with reduced motion. It does not follow the provider’s `durationScale`.',
        },
        {
          prop: 'restoredTitle',
          values: ['"Restored" (default)', 'ReactNode', 'false'],
          description: 'The confirmation after Undo, or none.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Waiting',
          attribute: 'data-state="open" (on .mn-toast)',
          description: 'The toast is shown with its Undo button; the timer runs unless paused.',
        },
        {
          name: 'Paused',
          description:
            'While the pointer is over the toast region or focus is inside it, the timer and the countdown bar stop.',
        },
        {
          name: 'Counting down',
          attribute: 'data-countdown (on .mn-undo-toast)',
          description: 'With `countdown`, the bar shrinks over the remaining time.',
        },
        {
          name: 'Undone',
          attribute: 'data-tone="success"',
          description: 'The undo toast closes and a polite "Restored" toast appears.',
        },
        {
          name: 'Committed',
          description: 'The toast closed without Undo; `onCommit` has run.',
        },
      ],
    },
    interaction: {
      items: [
        'Showing the toast never moves focus.',
        'Pressing Undo runs `onUndo` and closes the toast; the item comes back and "Restored" is announced politely.',
        'Dismiss, Escape and swiping commit, as does letting the timer run out.',
        'Several undoable actions can be open at once; each settles on its own. When the provider’s `limit` is reached, the oldest commits first.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'F8', action: 'Moves focus to the toast region (the provider’s `hotkey`).' },
        { keys: 'Tab / Shift+Tab', action: 'Moves between Undo and Dismiss.' },
        { keys: 'Enter / Space', action: 'Activates Undo or Dismiss.' },
        { keys: 'Escape', action: 'Closes the focused toast, which commits it.' },
      ],
    },
    accessibility: {
      role: 'listitem in the toast region (role="region")',
      name: 'The toast is named by `title`; the Undo button by `undoLabel`.',
      items: [
        'Announced politely once when it appears; the "Restored" confirmation is polite too.',
        '`undoAltText` tells screen-reader users another way to undo, for when the toast has gone.',
        'The timer pauses while focus or the pointer is in the region, and `durationScale` on the provider gives more time (WCAG 2.2.1).',
        'The countdown bar is decorative (`aria-hidden`).',
      ],
    },
    responsive: {
      items: [
        'Uses the toast viewport: up to 24rem wide and never wider than the screen minus 2rem, so it fits a 390px phone.',
        'Long titles and descriptions wrap; the Undo button stays beside them.',
      ],
    },
    theme: {
      items: [
        'Toast surface, border and text tokens; the countdown bar uses `--mn-color-border-strong`.',
        'Light, dark and more-contrast themes apply automatically. In forced colours the bar is drawn in CanvasText; with reduced motion it is hidden.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Delete with undo',
          render: () => (
            // Docs page only: each example has its own provider. Apps mount exactly one.
            <ToastProvider label="Undo example notifications">
              <NoteList />
            </ToastProvider>
          ),
        },
        {
          title: 'With a countdown',
          render: () => (
            <ToastProvider label="Countdown example notifications" position="bottom-center">
              <CountdownTile />
            </ToastProvider>
          ),
        },
        {
          title: 'Custom label, no confirmation',
          render: () => (
            <ToastProvider label="Quiet undo example notifications" position="top-right">
              <QuietUndo />
            </ToastProvider>
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Apply the change in the interface before calling `undoable`.',
        'Defer the server delete to `onCommit`, or make `onUndo` restore it on the server.',
        'Restore the item where it was (same position, same state).',
        'Use a past-tense title and name the item in the description.',
      ],
      dont: [
        'Do not offer Undo for actions that cannot really be reversed.',
        'Do not also ask for confirmation first; pick one.',
        'Do not shorten the window below 5 seconds or rely on the toast as the only place to recover something important.',
      ],
    },
    related: {
      items: [
        { id: 'toast', reason: 'The toast system it is built on; mount ToastProvider once.' },
        { id: 'confirm-dialog', reason: 'For actions that cannot be undone.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['toast'],
    },
    testing: {
      unit: [
        'Undo calls onUndo once, resolves undone, never commits, and shows a polite success "Restored" toast; axe passes.',
        'Dismiss and Escape (after F8) commit once and resolve committed.',
        'Undo is reachable by keyboard: F8, Tab, Enter.',
        'A newer toast pushing it out of the limit commits it.',
        'pagehide commits, and the toast closing afterwards does not commit again.',
        'Custom labels and confirmation; restoredTitle false shows none.',
        'An async onUndo is awaited before resolving; a failed one shows a danger toast and rejects.',
        'undoAltText reaches the screen-reader text.',
        'Timing: 8s by default, clamped to 5–10s, Infinity stays open.',
        'The countdown bar is hidden from assistive technology and timed to the duration.',
        'Throws outside ToastProvider; renders on the server.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
