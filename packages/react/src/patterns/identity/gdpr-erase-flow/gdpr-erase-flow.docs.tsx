import { Button } from '@rookcom/monarch-react';
import { GdprEraseFlow } from '@rookcom/monarch-react/patterns';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const wait = (ms: number) => new Promise<void>((resolve) => window.setTimeout(resolve, ms));

const erased = [
  { label: 'Profile, name and contact details' },
  { label: 'messages and comments', count: 214 },
  { label: 'uploaded files', count: 37 },
  { label: 'Sign-in history and device records' },
];

const retained = [
  {
    label: 'Invoices and payment records',
    reason: 'Kept for the retention period your policy sets, then deleted',
  },
  { label: 'Audit log entries', detail: 'With the name replaced by a reference' },
];

function EraseSubject() {
  return (
    <GdprEraseFlow
      trigger={<Button variant="danger">Erase personal data</Button>}
      subjectName="Alex Kim"
      confirmPhrase="alex@example.com"
      erased={erased}
      retained={retained}
      stepUp={{ methods: ['password'], onVerify: () => wait(600) }}
      onErase={async () => {
        await wait(1200);
        return { auditId: 'evt_7Q2K9X' };
      }}
    />
  );
}

function Loading() {
  return (
    <GdprEraseFlow
      trigger={<Button variant="danger">Erase personal data</Button>}
      subjectName="Alex Kim"
      erased={[]}
      loading
      onErase={() => wait(800)}
    />
  );
}

function FailingErase() {
  return (
    <GdprEraseFlow
      trigger={<Button variant="danger">Erase personal data</Button>}
      subjectName="Alex Kim"
      erased={erased}
      onErase={async () => {
        await wait(800);
        throw new Error('The erasure could not start. Nothing was erased. Try again.');
      }}
    />
  );
}

export default {
  id: 'gdpr-erase-flow',
  title: 'GdprEraseFlow',
  summary:
    'Erases one person’s personal data: what is erased and kept, acknowledgement, type to confirm, step-up, audit reference.',
  sections: {
    purpose: {
      summary:
        'Handles an erasure request for one data subject, a level 4 legal action (docs/guides/destructive-actions.md). It shows exactly what is erased and what is kept and why, requires an explicit "I understand" and a typed phrase, optionally re-authenticates the operator, and ends with the audit-log reference. It is a two-step WizardDialog rather than a ConfirmDialog: the two lists and the notice need room to be read before the fields appear, and the done screen is where the audit reference is shown, which a ConfirmDialog (which closes on success) has no place for.',
      useWhen: [
        'An admin or privacy operator erases the personal data of one person on request.',
        'The erasure keeps some records (for example invoices) and the operator must see that.',
      ],
      avoidWhen: [
        'A person deletes their own account: use DeleteAccountFlow.',
        'Deleting ordinary records: use ConfirmDialog.',
      ],
    },
    designPrinciples: {
      items: [
        'Show what is erased and what is kept side by side; the kept list says why, in the product’s own policy words.',
        'Neutral wording: the component names no law and no retention period; whatever it says comes from props.',
        'Two separate guards: an acknowledgement checkbox and a typed phrase.',
        'End with proof: the audit-log reference, when the backend returns one.',
      ],
    },
    apiReference: { source: 'typescript' },
    typedProperties: {
      source: 'typescript',
      notes: [
        '`erased` and `retained` take ConsequenceList items without `kind`; retained items add an optional `reason`.',
        '`onErase` may resolve with `{ auditId }` to show the audit reference on the done screen.',
        '`stepUp` is a `StepUpRequirement` (`methods`, `onVerify`, `description`, …), shared by every level 4 flow.',
        'Other WizardDialog props pass through; `wizardMessages` translates its buttons and progress.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'confirmPhrase',
          values: ['undefined', 'string'],
          description: 'What to type: the subject’s name by default, or e.g. their email.',
        },
        {
          prop: 'stepUp',
          values: ['undefined', 'StepUpRequirement'],
          description: 'Requires re-authentication before the erasure runs.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Loading',
          attribute: 'aria-busy (erased list)',
          description: '`loading`: skeleton rows; Continue waits with a reason.',
        },
        {
          name: 'Error',
          attribute: 'data-state="error" (erased list)',
          description:
            '`error` replaces the lists; Continue stays disabled, since the operator cannot see what would be erased.',
        },
        {
          name: 'Not confirmed',
          attribute: 'disabled (erase button)',
          description: 'Until the box is ticked and the phrase matches.',
        },
        {
          name: 'Erasing',
          attribute: 'aria-busy, data-busy',
          description:
            'While step-up or `onErase` runs the flow cannot be dismissed; a rejection shows its message.',
        },
        {
          name: 'Current step',
          attribute: 'data-step, data-kind',
          description: '`review`, `confirm` or `done` (from WizardDialog).',
        },
      ],
    },
    interaction: {
      items: [
        'Continue moves from the review to the confirmation; Back returns.',
        'The erase button opens step-up first when `stepUp` is set; cancelling returns to the confirm step without erasing.',
        'After `onErase` resolves the done screen shows the result and, when given, the audit reference; closing it calls `onDone`.',
        'Every opening starts clean: unticked box, empty field.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Tab / Shift+Tab',
          action: 'Moves through the checkbox, the field and the footer buttons.',
        },
        { keys: 'Space (checkbox)', action: 'Ticks the acknowledgement.' },
        { keys: 'Enter (in the field)', action: 'Erases once both guards are met.' },
        { keys: 'Escape', action: 'Cancels the flow, or the step-up dialog when it is open.' },
      ],
    },
    accessibility: {
      role: 'dialog (aria-modal), with a nested dialog for step-up',
      name: '`messages.title` ("Erase personal data").',
      items: [
        'The erased and kept lists are named by their headings ("Will be erased", "Will be kept").',
        'Every list item has an icon and text; colour is not the only signal.',
        'The disabled erase button is described by what is still missing.',
        'Each step change moves focus to the step heading.',
      ],
    },
    responsive: {
      items: ['Works at 390px; long names, reasons and audit references wrap.'],
    },
    theme: {
      items: [
        'Lists, notice and fields come from ConsequenceList, Alert, Checkbox and TypeToConfirmInput; the audit box uses the subtle border and monospace tokens.',
        'Forced-colors mode draws the audit box border in CanvasText.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Erase with retained records, step-up and audit reference',
          render: () => <EraseSubject />,
        },
        { title: 'Lists still loading', render: () => <Loading /> },
        { title: 'A failing erasure', render: () => <FailingErase /> },
      ],
    },
    doDont: {
      do: [
        'List every category of data that is erased, with counts when you have them.',
        'Explain every retained category with the reason your policy gives.',
        'Return the audit id from `onErase`.',
        'Pass `stepUp`.',
      ],
      dont: [
        'Do not claim a specific law or retention period the product has not confirmed.',
        'Do not skip the retained list when something is kept.',
        'Do not erase before the confirm step.',
      ],
    },
    related: {
      items: [
        { id: 'delete-account-flow', reason: 'A person deleting their own account.' },
        { id: 'consequence-list', reason: 'The erased and kept lists.' },
        { id: 'audit-log-viewer', reason: 'Where the audit reference can be looked up.' },
        { id: 'wizard-dialog', reason: 'The base it is built on.' },
        { id: 'step-up-auth-dialog', reason: 'The re-authentication step.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: [
        'alert',
        'consequence-list',
        'checkbox',
        'type-to-confirm-input',
        'step-up-auth-dialog',
        'wizard-dialog',
      ],
    },
    testing: {
      unit: [
        'Reviews the lists, requires the acknowledgement and the phrase, erases, shows the audit id; onDone on close; axe passes on both steps.',
        'No audit line when onErase resolves without an id; the phrase defaults to the subject name.',
        'Loading and error block the review.',
        'Step-up cancel keeps the data; success erases.',
        'A failed erasure shows the error on the confirm step.',
        'Strings come from messages; renders on the server.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
