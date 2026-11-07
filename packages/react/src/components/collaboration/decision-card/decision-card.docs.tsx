import { useState } from 'react';
import { Badge } from '@rookcom/monarch-react';
import { DecisionCard, type DecisionOption } from '@rookcom/monarch-react/collaboration';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const RENEWAL: DecisionOption[] = [
  { value: 'keep', label: 'Keep', tone: 'success' },
  { value: 'review', label: 'Review', tone: 'warning' },
  {
    value: 'lapse',
    label: 'Let lapse',
    tone: 'danger',
    note: 'Reminders stop and one final notice is sent before it expires.',
    reasonRequired: true,
  },
];

const APPROVAL: DecisionOption[] = [
  { value: 'approve', label: 'Approve', description: 'Goes ahead now', tone: 'success' },
  { value: 'defer', label: 'Defer', description: 'Back next cycle', tone: 'warning' },
  {
    value: 'reject',
    label: 'Reject',
    description: 'Closes the request',
    tone: 'danger',
    reasonRequired: true,
  },
];

const NOW = new Date(2026, 4, 10, 12);

const wait = () => new Promise((resolve) => setTimeout(resolve, 700));

function RenewalDemo() {
  const [saved, setSaved] = useState<{ value: string | null; reason: string | null; at: Date }>({
    value: 'review',
    reason: 'Check with the team first',
    at: new Date(2026, 4, 8, 9),
  });
  return (
    <div style={{ width: '100%', maxWidth: '32rem' }}>
      <DecisionCard
        title="Renewal decision"
        options={RENEWAL}
        value={saved.value}
        reason={saved.reason}
        decidedAt={saved.at}
        decidedBy="Alex Morgan"
        now={NOW}
        context={
          <p style={{ margin: 0 }}>
            Usage: <Badge tone="info">Active</Badge> Serves a website and email.
          </p>
        }
        onDecide={async ({ value, reason }) => {
          await wait();
          setSaved({ value, reason, at: NOW });
        }}
      />
    </div>
  );
}

function ApprovalDemo() {
  const [value, setValue] = useState<string | null>(null);
  return (
    <div style={{ width: '100%', maxWidth: '32rem' }}>
      <DecisionCard
        title="Budget request"
        options={APPROVAL}
        value={value}
        allowNone={false}
        reasonRequired
        onDecide={async (result) => {
          await wait();
          setValue(result.value);
        }}
      />
    </div>
  );
}

export default {
  id: 'decision-card',
  title: 'DecisionCard',
  summary: 'Record a decision about an item, with a reason, next to the evidence for it.',
  sections: {
    purpose: {
      summary:
        'A small form for recording a decision about one item (keep, review or let lapse; approve, defer or reject) with a reason others will read. It shows what the selected choice will do before it is saved, who decided and when, and a read-only view for people without write access. Needed by: Atlas (domain renewal DecisionControl); also fits approval and review flows in People and Chat.',
      useWhen: [
        'Someone must make and record a judgement about one item, and the reason matters later.',
        'A choice has consequences (stopping reminders, closing a request) that should be visible before saving.',
      ],
      avoidWhen: [
        'A one-click destructive action: use ConfirmDialog.',
        'Moving an item through many stages: use a status select or Stepper.',
        'Approving many items at once: use a table with bulk actions.',
      ],
    },
    designPrinciples: {
      items: [
        'Evidence first: the `context` slot sits above the choices so the decision is made with the facts in view.',
        'Say what happens: a choice’s `note` appears as soon as it is selected, before anything is saved.',
        'Nothing is saved until Save; Save stays disabled until something changed.',
        'The reason is asked for where it matters (`reasonRequired`, per choice or for all).',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        '`onDecide({ value, reason })` receives the chosen value (null for "No decision") and the trimmed reason (null when empty). Return a promise to show Save busy; reject to keep the form and show the error.',
        'Without `onDecide`, or with `readOnly`, the card shows the saved decision as a badge with its reason.',
        '`DECISION_CARD_MESSAGES` holds the English defaults.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Migrating from the app copy (Atlas `features/domains/components/DecisionControl`): the `d: Domain` prop is split into `value` (`d.decision.value`), `reason`, `decidedAt` (`d.decision.decidedAt`) and `context` (render the usage chip and reasons list there). The hard-coded keep / review / let_lapse choices become `options` with labels from the product’s translations, and `letLapseNote` becomes that option’s `note`. `useDecide().mutate` becomes `onDecide` (return the mutation promise); `canWrite === false` becomes `readOnly`. react-hook-form, zod and the success toast are no longer needed: validation and the "Decision saved." announcement are built in.',
      ],
    },
    variants: {
      items: [],
      notes: [
        'No visual variants. `allowNone` adds or removes the "No decision" choice; `readOnly` shows the result only.',
      ],
    },
    states: {
      items: [
        {
          name: 'Selected',
          attribute: 'data-checked on the choice',
          description: 'The chosen option, outlined in the accent colour.',
        },
        {
          name: 'Disabled choice',
          attribute: 'data-disabled on the choice',
          description: 'Cannot be chosen.',
        },
        {
          name: 'Saving',
          description: 'Save shows a spinner; the choices and reason are disabled.',
        },
        {
          name: 'Reason missing',
          description: 'The reason field is invalid with a message; nothing is sent.',
        },
        {
          name: 'Error',
          description:
            'A rejected `onDecide` shows its message as an alert; the form keeps the choice.',
        },
        {
          name: 'Read-only',
          attribute: 'data-readonly on the root',
          description: 'A badge with the decision, the reason and who decided.',
        },
      ],
    },
    interaction: {
      items: [
        'Clicking a choice selects it; its note appears below.',
        'Typing a reason enables Save; the character count appears when 50 or fewer are left.',
        'Save sends the decision and announces "Decision saved."; a new saved `value` or `reason` from the product resets the form.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Moves to the selected choice, the reason, then Save.' },
        { keys: 'Arrow keys', action: 'Move between choices and select (native radio group).' },
        {
          keys: 'Enter / Space on Save',
          action: 'Saves the decision. Enter in the reason adds a new line.',
        },
      ],
    },
    accessibility: {
      role: 'region (when titled) with a radio group in a fieldset',
      name: 'The title; the group by its legend (messages.choices)',
      items: [
        'Choices are native radio buttons named by their label and described by their description.',
        'The reason field is marked required when the choice needs one, and its error is tied to it.',
        'The note for the selected choice is in a polite live region.',
        'Saving is announced through a status; failures through an alert.',
      ],
    },
    responsive: {
      items: [
        'Choices form a grid of at least 7.5rem columns, so four sit in a row on desktop and two on a 390px phone.',
        'Every choice is at least 44px tall.',
        'The footer wraps the decided line above Save when space runs out.',
      ],
    },
    theme: {
      items: [
        'Choices: `--mn-color-bg-surface`, `--mn-color-border-default`; selected uses `--mn-color-accent-subtle` and `--mn-color-accent-solid`.',
        'Note: `--mn-color-bg-muted`; errors `--mn-color-danger-strong`.',
        'Read-only badge colours come from each option’s `tone`.',
        'In forced colours the selected choice gets a thick Highlight border; transitions stop under reduced motion.',
      ],
    },
    examples: {
      items: [
        { title: 'Renewal decision with evidence', render: () => <RenewalDemo /> },
        { title: 'Approval with a required reason', render: () => <ApprovalDemo /> },
        {
          title: 'Read-only',
          render: () => (
            <DecisionCard
              title="Renewal decision"
              options={RENEWAL}
              value="keep"
              reason="Still serves the main website"
              decidedAt={new Date(2026, 4, 1)}
              decidedBy="Sam Lee"
              now={NOW}
              readOnly
            />
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Use short verbs for choices and put consequences in `note`.',
        'Show the evidence in `context`: usage, cost, risk.',
        'Require a reason for choices that are hard to undo.',
      ],
      dont: [
        'Do not save on every click; the reason belongs with the choice.',
        'Do not use colours alone to tell choices apart; labels carry the meaning.',
        'Do not hide the form from people who cannot change it; show `readOnly` so they see the decision.',
      ],
    },
    related: {
      items: [
        {
          id: 'confirm-dialog',
          reason: 'For a single confirmed action instead of a recorded decision.',
        },
        { id: 'badge', reason: 'The read-only decision marker.' },
        { id: 'relative-time', reason: 'When the decision was made.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['button', 'badge', 'relative-time', 'form-field', 'text-field'],
    },
    testing: {
      unit: [
        'Named region, radio group, saved choice, description and who decided.',
        'Arrow keys, the note, required reason blocking save and trimming.',
        'No decision sends null; busy state disables the form; rejection shows an alert.',
        'Reset when the saved decision changes; read-only and undecided views; custom strings.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
