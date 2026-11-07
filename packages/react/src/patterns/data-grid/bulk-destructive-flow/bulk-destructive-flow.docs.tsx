import { Button } from '@rookcom/monarch-react';
import { BulkDestructiveFlow, type BulkDestructiveFailure } from '@rookcom/monarch-react/patterns';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const wait = (ms: number) => new Promise<void>((resolve) => window.setTimeout(resolve, ms));

const contact = { one: 'contact', other: 'contacts' };

/** Pretends to delete `total` items in batches of five, reporting as it goes. */
async function deleteInBatches(total: number, report: (done: number) => void) {
  for (let done = 0; done < total;) {
    await wait(150);
    done = Math.min(total, done + 5);
    report(done);
  }
}

function DeleteMany() {
  return (
    <BulkDestructiveFlow
      trigger={<Button variant="danger">Delete 37 contacts</Button>}
      count={37}
      noun={contact}
      sample={['Ada Park', 'Ben Ortiz', 'Cleo Ngata']}
      consequences={[
        { kind: 'removed', label: 'notes and activity on these contacts', count: 214 },
        { kind: 'kept', label: 'Companies they belong to stay' },
      ]}
      onDelete={async (report) => {
        await deleteInBatches(37, report);
      }}
    />
  );
}

const locked: BulkDestructiveFailure[] = [
  { id: 'c-12', label: 'Dana Whitfield', error: 'Linked to an open invoice.' },
  { id: 'c-31', label: 'Eli Morgan', error: 'You do not have permission to delete it.' },
];

function PartialFailure() {
  return (
    <BulkDestructiveFlow
      trigger={<Button variant="danger">Delete 8 contacts</Button>}
      count={8}
      noun={contact}
      sample={['Dana Whitfield', 'Eli Morgan', 'Farah Aziz', 'Gus Lind']}
      onDelete={async (report) => {
        await deleteInBatches(8, report);
        return { failed: locked };
      }}
      onRetryFailed={async (failed, report) => {
        await wait(600);
        report(1);
        return { failed: failed.slice(1) };
      }}
    />
  );
}

function SmallSelection() {
  return (
    <BulkDestructiveFlow
      trigger={<Button variant="danger">Delete 3 contacts</Button>}
      count={3}
      noun={contact}
      sample={['Ada Park', 'Ben Ortiz', 'Cleo Ngata']}
      onDelete={async (report) => {
        await deleteInBatches(3, report);
      }}
    />
  );
}

export default {
  id: 'bulk-destructive-flow',
  title: 'BulkDestructiveFlow',
  summary:
    'Confirms and runs a bulk deletion ("Delete 37 contacts") with progress and a partial-failure report.',
  sections: {
    purpose: {
      summary:
        'Deleting many records at once needs more than a yes/no: the person should see what is selected, confirm the count, watch the deletion run, and learn which items failed. This flow does all four in one dialog, built on WizardDialog.',
      useWhen: [
        'A table or list deletes a selection of records in one action.',
        'The deletion runs item by item or in batches and some items may fail.',
      ],
      avoidWhen: [
        'One record is deleted: use ConfirmDialog, or an undo toast when it can be restored.',
        'The items can be restored afterwards: archive them and offer undo instead.',
      ],
    },
    designPrinciples: {
      items: [
        'Name the number and the noun everywhere: the title, the button and the result.',
        'Show a few of the selected items so the person can tell they picked the right ones.',
        'Above a threshold (default 10), make the person type the count before the button works.',
        'Never leave the person guessing: progress while it runs, then exactly which items failed and why.',
      ],
    },
    apiReference: { source: 'typescript' },
    typedProperties: {
      source: 'typescript',
      notes: [
        '`noun` is `{ one, other }` (plus `zero`, `two`, `few`, `many` where the language needs them), chosen with `Intl.PluralRules` in `locale`; or a function of the count.',
        '`onDelete(report)` calls `report(done)` as items finish and resolves with `{ failed: [{ id, label, error }] }`, or nothing when all succeeded. A rejection means nothing was deleted and is shown on the confirmation step.',
        '`stepUp` is `{ methods, onVerify }`, the same shape as in the other destructive flows.',
        'Every string is in `messages`; numbers reach the message functions already formatted.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'typeCountAbove',
          values: ['10 (default)', 'any number', 'Infinity'],
          description: 'When `count` is above it, the person types the count to confirm.',
        },
        {
          prop: 'onRetryFailed',
          values: ['omitted', 'function'],
          description: 'When given, a partial result offers a retry step for the failed items.',
        },
        {
          prop: 'stepUp',
          values: ['omitted', '{ methods, onVerify }'],
          description: 'Asks the person to re-authenticate before the deletion runs.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Blocked',
          attribute: 'disabled (delete button)',
          description: 'The count has not been typed yet; the reason describes the button.',
        },
        {
          name: 'Running',
          attribute: 'data-running, aria-busy',
          description:
            'A progress bar with "12 of 37 deleted". Cancel, Escape and the close button do nothing until it finishes.',
        },
        {
          name: 'Done',
          attribute: 'data-step="result"',
          description: 'Every item was deleted: the heading says so and one Done button closes.',
        },
        {
          name: 'Partial failure',
          attribute: 'data-step="result"',
          description:
            'A warning, the list of items that failed with their reasons, and a retry button when `onRetryFailed` is set.',
        },
        {
          name: 'Retry',
          attribute: 'data-phase="retry"',
          description: 'A danger step listing only the failed items, with "Retry 2 contacts".',
        },
        {
          name: 'Error',
          attribute: 'role="alert"',
          description: '`onDelete` rejected: the message shows on the confirmation step.',
        },
      ],
    },
    interaction: {
      items: [
        'The delete button names the action and count ("Delete 37 contacts") and uses the danger style.',
        'With `stepUp`, the step-up dialog opens first; cancelling it returns to the confirmation without deleting.',
        'While the deletion runs the dialog cannot be dismissed.',
        'Every opening starts again on the confirmation step with an empty count field.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab / Shift+Tab', action: 'Moves through the count field and the buttons.' },
        { keys: 'Enter (in the count field)', action: 'Deletes, once the count matches.' },
        { keys: 'Escape', action: 'Cancels, except while the deletion runs.' },
      ],
    },
    accessibility: {
      role: 'dialog (aria-modal)',
      name: 'The title, "Delete 37 contacts" by default.',
      items: [
        'Each step change moves focus to the step heading, so the result is announced.',
        'The progress bar is a `progressbar` named "Deletion progress" with the text "12 of 37 deleted" as its value.',
        'Failed items are a list under a "Not deleted" heading, each with its reason as text.',
        'A disabled delete button is described by "Type the number to continue."',
      ],
    },
    responsive: {
      items: [
        'Follows WizardDialog: full-width buttons below 640px and a scrolling body.',
        'Long item labels and error messages wrap instead of scrolling sideways.',
        'The retry button fills the row on narrow screens.',
      ],
    },
    theme: {
      items: [
        'Lists use the subtle border token; error lines use the danger text colour.',
        'The progress bar uses the danger tone.',
        'In forced-colors mode list borders and error text use system colours.',
      ],
    },
    examples: {
      items: [
        { title: 'Many items: type the count, then watch it run', render: () => <DeleteMany /> },
        { title: 'Partial failure with retry', render: () => <PartialFailure /> },
        { title: 'Small selection (no typing)', render: () => <SmallSelection /> },
      ],
    },
    doDont: {
      do: [
        'Pass the real selection count and a sample of three to five labels.',
        'Call `report` as batches finish so the bar moves.',
        'Return the failed items with an error the person can act on.',
      ],
      dont: [
        'Do not use it for deletions that can be undone; offer undo instead.',
        'Do not resolve successfully when nothing was deleted; reject so the error shows.',
        'Do not hide failures behind a generic "Something went wrong".',
      ],
    },
    related: {
      items: [
        { id: 'confirm-dialog', reason: 'Deleting a single item.' },
        { id: 'bulk-action-bar', reason: 'Where the bulk delete action usually starts.' },
        { id: 'undo-toast', reason: 'For deletions that can be restored.' },
        { id: 'type-to-confirm-input', reason: 'The count field.' },
        { id: 'progress-bar', reason: 'The progress shown while it runs.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: [
        'button',
        'progress-bar',
        'alert',
        'consequence-list',
        'type-to-confirm-input',
        'step-up-auth-dialog',
        'wizard-dialog',
      ],
    },
    testing: {
      unit: [
        'Shows the sample and "+N more", requires the count to be typed, runs and focuses the result heading; axe passes.',
        'Does not ask for the count at or below typeCountAbove.',
        'Shows "2 of 5 deleted" while running; Cancel is disabled and Escape is ignored.',
        'Reports a partial failure and retries the failed items; axe passes on the result.',
        'A rejected onDelete shows the error and stays on the confirmation step.',
        'Step-up: cancelling does not delete; verifying runs the deletion.',
        'Plural nouns and messages.',
        'Renders on the server.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
