import { useState } from 'react';
import { Button, Popconfirm } from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';

function RemoveFilter() {
  const [filters, setFilters] = useState(['Status: Open', 'Owner: Me']);
  return (
    <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.5rem' }}>
      {filters.map((filter) => (
        <Popconfirm
          key={filter}
          trigger={<Button size="sm">{filter}</Button>}
          title="Remove this filter?"
          description="The list shows matching items again."
          confirmLabel="Remove"
          onConfirm={() => setFilters((all) => all.filter((f) => f !== filter))}
        />
      ))}
      {filters.length === 0 ? (
        <Button size="sm" onClick={() => setFilters(['Status: Open', 'Owner: Me'])}>
          Restore filters
        </Button>
      ) : null}
    </div>
  );
}

function SlowReset() {
  const [attempts, setAttempts] = useState(0);
  return (
    <Popconfirm
      trigger={<Button>Reset layout</Button>}
      title="Reset the dashboard layout?"
      description="Widgets go back to their default places."
      confirmLabel="Reset layout"
      onConfirm={() =>
        new Promise<void>((resolve, reject) => {
          setAttempts((n) => n + 1);
          // The first attempt fails, to show the error and retry.
          setTimeout(
            () => (attempts === 0 ? reject(new Error('Could not reach the server.')) : resolve()),
            800,
          );
        })
      }
    />
  );
}

export default {
  id: 'popconfirm',
  title: 'Popconfirm',
  summary: 'A small confirmation anchored to its button, for low-stakes actions.',
  sections: {
    purpose: {
      summary:
        'Asks "are you sure?" right next to the button that was pressed, without the weight of a dialog, for small actions that are easy to redo but annoying to trigger by accident: remove a filter, discard a draft reply, reset a layout, leave a channel. Needed by: the Wave 10 move away from `window.confirm` in the Rook apps, for confirmations below level 1 of the destructive-actions guide.',
      useWhen: [
        'The action is quick to redo, but a misclick would still be a nuisance.',
        'The confirmation needs one short question and two buttons, nothing more.',
        'The context of the trigger (a chip, a row, a toolbar) matters to the question.',
      ],
      avoidWhen: [
        'The action is fully reversible: act immediately and offer undo (`useUndoToast()`), level 0.',
        'Work is lost or goes to the trash (levels 1–4 in docs/guides/destructive-actions.md): use ConfirmDialog or `useConfirm()`.',
        'You need consequences, a reason, type-to-confirm or step-up authentication: use ConfirmDialog or a flow.',
        'The trigger is in a menu that closes: use `useConfirm()`, which returns focus correctly.',
      ],
    },
    designPrinciples: {
      items: [
        'Friction matches stakes: lighter than ConfirmDialog, heavier than undo. When in doubt, use the dialog.',
        'Same contract as ConfirmDialog: a question title, named actions, Cancel focused first, async pending and error states.',
        'Anchored to the trigger, so it is obvious what is being confirmed.',
        'Modal while open: focus stays inside and the rest of the page is hidden from assistive technology until it is answered or dismissed.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'One component. It forwards its ref to the `role="alertdialog"` panel; `trigger` is rendered with `asChild`.',
        'Takes PopoverContent props for placement (`side`, default `top`; `align`, default `center`; `sideOffset`; `collisionPadding`).',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        '`title`, `description`, `confirmLabel` (default "Confirm"), `cancelLabel` (default "Cancel") and `errorMessage` are the visible text; override them to translate.',
        '`onConfirm` may return a promise: the popover stays open and pending until it settles, and shows the error on rejection.',
        '`open`/`defaultOpen`/`onOpenChange` for control; `onCancel` for dismissals.',
        'Migrating: the Rook apps have no shadcn/ui popconfirm. Replace `if (window.confirm("Remove filter?")) remove()` on a small action with `<Popconfirm trigger={button} title="Remove filter?" confirmLabel="Remove" onConfirm={remove} />`; anything that deletes data goes to `useConfirm()` instead.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'tone',
          values: ['default', 'danger'],
          description:
            '`danger` adds a warning icon and a red confirm button, for removing or discarding something small (`data-tone`).',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Open',
          attribute: 'data-state="open"',
          description: 'The panel shows beside the trigger with focus on Cancel.',
        },
        {
          name: 'Pending',
          attribute: 'aria-busy="true"',
          description:
            'The confirm button shows a spinner, Cancel is disabled and nothing dismisses the panel.',
        },
        {
          name: 'Error',
          attribute: 'role="alert" on the message',
          description:
            'A failed action shows its message above the buttons; confirming again retries.',
        },
      ],
    },
    interaction: {
      items: [
        'Pressing the trigger opens the panel; focus moves to Cancel (or Confirm with `initialFocus="confirm"`).',
        'Confirm runs `onConfirm`; when it succeeds the panel closes and focus returns to the trigger.',
        'Cancel, Escape or a click outside closes the panel, calls `onCancel`, and returns focus to the trigger.',
        'While a returned promise is pending, the panel cannot be dismissed.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Enter / Space (on the trigger)', action: 'Opens the panel with focus on Cancel.' },
        {
          keys: 'Tab / Shift+Tab',
          action: 'Moves between Cancel and Confirm; focus stays inside.',
        },
        { keys: 'Enter / Space', action: 'Activates the focused button.' },
        { keys: 'Escape', action: 'Cancels and returns focus to the trigger.' },
      ],
    },
    accessibility: {
      role: 'alertdialog (modal popover)',
      name: '`title` (`aria-labelledby`); `description` is the accessible description.',
      items: [
        'Follows the WAI-ARIA APG alert dialog pattern: focus moves in, stays in, and returns to the trigger.',
        'Focus starts on the least destructive action, as in ConfirmDialog.',
        'An error is announced with `role="alert"` and linked to the confirm button.',
        'The trigger gets `aria-haspopup="dialog"` and `aria-expanded`.',
      ],
    },
    responsive: {
      items: [
        'At most 20rem wide and never wider than the viewport minus 2rem; flips and shifts to stay on screen at 390px.',
        'The buttons wrap when the labels are long; on touch screens they are at least 44px tall.',
      ],
    },
    theme: {
      items: [
        'Uses the popover surface and Button tones, so every theme and dark mode apply.',
        'Forced-colours mode draws a CanvasText border; the open animation stops under `prefers-reduced-motion: reduce`.',
      ],
    },
    examples: {
      items: [
        { title: 'Remove a filter', render: () => <RemoveFilter /> },
        {
          title: 'Danger tone',
          render: () => (
            <Popconfirm
              tone="danger"
              trigger={<Button variant="danger-subtle">Discard reply</Button>}
              title="Discard this reply?"
              description="Your draft is not saved."
              confirmLabel="Discard"
              onConfirm={() => {}}
            />
          ),
        },
        { title: 'Async with an error and retry', render: () => <SlowReset /> },
      ],
    },
    doDont: {
      do: [
        'Phrase the title as a question about the specific action ("Remove this filter?").',
        'Name the confirm button with the verb ("Remove", "Discard").',
        'Prefer undo over any confirmation when the action can be reversed.',
      ],
      dont: [
        'Do not use it for deleting records, accounts or anything that goes to the trash: use ConfirmDialog.',
        'Do not add fields or long text; that is a dialog.',
        'Do not chain two popconfirms for one action.',
      ],
    },
    related: {
      items: [
        { id: 'confirm-dialog', reason: 'Confirmation for consequential and destructive actions.' },
        { id: 'popover', reason: 'The floating panel it is built on.' },
        { id: 'context-menu', reason: 'Menus whose small actions may need a confirmation.' },
        { id: 'button', reason: 'The usual trigger.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['button', 'popover'],
    },
    testing: {
      unit: [
        'Opens as a named, described alertdialog with focus on Cancel.',
        'Escape and Cancel cancel, call `onCancel` and return focus to the trigger.',
        'Confirm runs the action and closes; Tab stays inside while open.',
        'Async: pending state blocks dismissal; a rejection shows the error, linked to Confirm; retry succeeds.',
        '`initialFocus="confirm"`; controlled `open`.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
