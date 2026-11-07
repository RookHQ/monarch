import { useState } from 'react';
import { AiSuggestedActions, type AiSuggestedAction } from '@rookcom/monarch-react/ai';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const wait = (ms: number) => new Promise<void>((resolve) => window.setTimeout(resolve, ms));

const ACTIONS: AiSuggestedAction[] = [
  {
    id: 'reminder',
    label: 'Send the renewal reminder',
    description: 'The contract ends in 14 days and no renewal is recorded.',
  },
  {
    id: 'task',
    label: 'Create a follow-up task for next Monday',
    risk: 'medium',
  },
  {
    id: 'email',
    label: 'Email the account team a status update',
    description: 'Goes to 3 people.',
    requiresApproval: true,
    consequences: ['3 people will get an email', 'The email is logged on the account'],
    confirmLabel: 'Send email',
  },
  {
    id: 'close',
    label: 'Close 4 stale tickets',
    description: 'No reply in 60 days.',
    risk: 'destructive',
    consequences: ['4 tickets will be closed', 'Requesters are notified'],
    confirmLabel: 'Close tickets',
  },
];

function Demo() {
  const [actions, setActions] = useState(ACTIONS);
  return (
    <AiSuggestedActions
      actions={actions}
      onRun={() => wait(800)}
      onDismiss={(id) => setActions((list) => list.filter((a) => a.id !== id))}
    />
  );
}

export default {
  id: 'ai-suggested-actions',
  title: 'AiSuggestedActions',
  summary: 'Next best actions for a record, with Run, approval for risky ones and dismiss.',
  sections: {
    purpose: {
      summary:
        'Offers a short list of next steps a model (or a rule set) suggests for the current record, each one runnable in place. Actions that need approval, and every destructive one, are confirmed in a dialog first, so nothing consequential happens on one click.',
      useWhen: [
        'A record page should suggest what to do next and let people do it without leaving.',
        'Some suggestions change data or contact people and must be approved first.',
      ],
      avoidWhen: [
        'An agent wants to call a tool during a conversation: show the approval in the thread.',
        'A single insight with one action: use AiInsightCallout.',
        'Fixed navigation or commands that are always available: use a menu or toolbar.',
      ],
    },
    designPrinciples: {
      items: [
        'Human in the loop: approval-required and destructive actions open a ConfirmDialog with the consequences listed, focus on Cancel and a danger-toned confirm for destructive ones.',
        'Risk and approval are text labels with icons, never colour alone.',
        'Honest origin: "Suggested by AI" with the Bot glyph, or "Rule-based" in neutral colours.',
        'Every outcome is visible: Running…, Done, or an error that says to try again.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Native `<div>` attributes pass through and the ref is forwarded to the root.',
        '`onRun(id)` may return a promise: the action shows Running… until it settles, Done on success and a failed line on rejection. Inside the approval dialog a rejection is shown in the dialog and the person can retry.',
        '`aiActionNeedsApproval(action)` is true for `requiresApproval` or `risk: "destructive"`.',
        'Pass `status` on an action to control its state yourself (for example when the work finishes on the server later).',
        '`messages.origin` and `messages.risk` merge per key; `AI_SUGGESTED_ACTIONS_MESSAGES` holds the English defaults.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [
        {
          prop: 'origin',
          values: ['ai', 'rule'],
          description: 'Provenance chip next to the heading. Exposed as `data-origin`.',
        },
        {
          prop: 'actions[].risk',
          values: ['low', 'medium', 'high', 'destructive'],
          description:
            'Shown as a text tag from medium up. Sets the approval dialog tone: warning for high, danger for destructive. Destructive always needs approval.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Ready',
          attribute: 'data-status="ready" on the item',
          description: 'Run is available.',
        },
        {
          name: 'Running',
          attribute: 'data-status="running"',
          description: 'Run shows a spinner and "Running…", stays focusable and ignores clicks.',
        },
        {
          name: 'Done',
          attribute: 'data-status="done"',
          description:
            'Run becomes a "Done" label; focus moves to it if Run had focus. "“…” done." is announced politely.',
        },
        {
          name: 'Failed',
          attribute: 'data-status="failed"',
          description: 'An assertive line says it did not work; Run is available again.',
        },
        {
          name: 'Awaiting approval',
          description: 'The ConfirmDialog is open with the action’s consequences.',
        },
        { name: 'Empty', description: '"No suggestions right now."' },
      ],
    },
    interaction: {
      items: [
        'Run calls `onRun(id)` directly, or opens the approval dialog first when the action needs it.',
        'Cancelling or pressing Escape in the dialog runs nothing and returns focus to Run.',
        'Dismiss calls `onDismiss(id)`; when the action leaves `actions`, focus moves to the next action, else the previous, else the heading.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab / Shift+Tab', action: 'Moves through each action’s Run and Dismiss.' },
        { keys: 'Enter / Space', action: 'Runs or dismisses the focused action.' },
        { keys: 'Escape', action: 'In the approval dialog, cancels and returns focus to Run.' },
      ],
      notes: ['In the approval dialog focus starts on Cancel, so Enter never approves by default.'],
    },
    accessibility: {
      role: 'group containing a list',
      name: 'The heading (aria-labelledby)',
      items: [
        'Each Run button is named "Run" plus the action label ("Run Send the renewal reminder") and described by the action description.',
        'Dismiss buttons are named "Dismiss “<label>”".',
        'Risk, approval and origin are visible text.',
        'A polite status region announces finished actions; failures use role="alert".',
        'The approval dialog is a ConfirmDialog (`role="alertdialog"`) named by "Run “<label>”?".',
        'Focus is never lost: after Done, after closing the dialog and after a dismiss.',
      ],
    },
    responsive: {
      items: [
        'Below 30rem the Run and Dismiss buttons move under the text; labels wrap, so there is no horizontal scroll at 390px.',
        'On coarse pointers Run, Done and Dismiss are at least 44px.',
      ],
    },
    theme: {
      items: [
        'List: `--mn-color-bg-surface`, `--mn-color-border-default`, `--mn-color-border-subtle` between items.',
        'Risk tags use `--mn-color-warning-strong` (medium, high) and `--mn-color-danger-strong` (destructive); Done uses `--mn-color-success-strong`.',
        'The AI chip uses the AI subtle, strong and border tokens.',
        'In forced colours the list and chip borders use CanvasText.',
      ],
    },
    examples: {
      items: [
        { title: 'Run, approve and dismiss', stack: true, render: () => <Demo /> },
        {
          title: 'Rule-based suggestions',
          stack: true,
          render: () => (
            <AiSuggestedActions
              origin="rule"
              title="Next steps"
              actions={[
                { id: 'a', label: 'Assign an owner', description: 'This record has no owner.' },
                { id: 'b', label: 'Add a due date' },
              ]}
              onRun={() => {}}
            />
          ),
        },
        {
          title: 'Empty',
          stack: true,
          render: () => <AiSuggestedActions actions={[]} onRun={() => {}} />,
        },
      ],
    },
    doDont: {
      do: [
        'Write labels as verb phrases that say exactly what will happen.',
        'Require approval for anything that contacts people, spends money or changes many records.',
        'List concrete consequences for approval-required actions.',
      ],
      dont: [
        'Do not show more than about five suggestions; rank them.',
        'Do not mark a destructive action as low risk to skip the dialog.',
        'Do not run suggestions automatically on page load.',
      ],
    },
    related: {
      items: [
        { id: 'confirm-dialog', reason: 'The approval dialog it opens.' },
        { id: 'ai-insight-callout', reason: 'One insight with one action.' },
        { id: 'ai-summary-card', reason: 'Context for the suggestions.' },
      ],
    },
    dependencies: { packages: ['react'], components: ['button', 'confirm-dialog'] },
    testing: {
      unit: [
        '`aiActionNeedsApproval` for requiresApproval and destructive risk.',
        'Named group marked as AI; Run named and described per action; props and ref forwarded.',
        'Risk and approval tags as text; rule-based origin.',
        'Direct run from the keyboard, Done keeps focus and is announced; Running… then failed on rejection.',
        'Approval dialog: cancel runs nothing and returns focus; destructive uses the danger tone and does not focus confirm; errors stay in the dialog.',
        'Dismiss moves focus to the next, previous or heading; controlled status; messages.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
