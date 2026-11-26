import { Button, ToastProvider } from '@rookcom/monarch-react';
import { RemoveMemberFlow } from '@rookcom/monarch-react/patterns';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const wait = (ms: number) => new Promise<void>((resolve) => window.setTimeout(resolve, ms));

const member = { id: 'm3', name: 'Jordan Lee', email: 'jordan@example.com' };

const people = [
  { id: 'me', name: 'Sam Rivera', email: 'sam@example.com' },
  { id: 'm1', name: 'Alex Morgan', email: 'alex@example.com' },
  { id: 'm2', name: 'Priya Shah', email: 'priya@example.com' },
  member,
];

const work = [
  { id: 'issues', label: 'open issues', count: 12 },
  { id: 'reviews', label: 'pending reviews', count: 3 },
  { id: 'docs', label: 'draft documents', count: 5 },
];

export default {
  id: 'remove-member-flow',
  title: 'RemoveMemberFlow',
  summary:
    'Two-step dialog to remove a member: hand over their open work, then confirm, with an optional Undo toast.',
  sections: {
    purpose: {
      summary:
        'Removes a member without leaving their work stranded. The first step hands over what they still own, all at once or per kind of work, to another member or left unassigned; the second lists the consequences and confirms with the danger tone. The first step is skipped when they own nothing. With `onUndo`, a "<name> removed. Undo" toast follows.',
      useWhen: [
        'Removing a member who may own open work: issues, reviews, documents, deals.',
        'The product can reassign that work as part of the removal request.',
      ],
      avoidWhen: [
        'Removing a member who never owns work: MemberRow’s built-in remove confirmation is enough.',
        'Leaving a workspace yourself: use LeaveWorkspaceFlow.',
        'Revoking a pending invitation: use MemberRow with `status="invited"`.',
      ],
    },
    designPrinciples: {
      items: [
        'Hand over first, then remove: the person decides where the work goes before confirming.',
        'All at once by default, per kind of work when needed, and "Leave unassigned" is always a valid choice.',
        'The confirm step repeats where every kind of work goes, so the decision is checked once more.',
        'Danger tone and a button that names the action, "Remove member".',
        'Data and callbacks only: the product loads open work and runs `onRemove`.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Renders a WizardDialog (ref forwarded to its content) with the root class `mn-remove-member-flow`. WizardDialog props such as `trigger`, `open`, `onOpenChange`, `onCancel` and `size` pass through.',
        '`onUndo` uses `useUndoToast`, so it needs a `ToastProvider` above the flow. Without `onUndo` no toast code runs and no provider is needed.',
        '`REMOVE_MEMBER_FLOW_MESSAGES` holds the English defaults.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        '`onRemove` and `onUndo` receive `{ reassignments }`, one `{ workId, assigneeId }` per `openWork` item; `assigneeId: null` means unassigned.',
        '`stepUp` takes the StepUpAuthDialog options `methods`, `onVerify`, `description`, `forgotHref`, `onForgot`, `errorMessage` and `messages`; every account flow uses the same shape.',
        '`undoOptions` passes `duration`, `onCommit`, `countdown` and the other undo toast options.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'openWork',
          values: ['empty', 'one kind', 'several kinds'],
          description:
            'Empty: only the confirm step. One kind: one picker. Several: one picker plus "Choose a person for each kind of work".',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Reassign',
          attribute: 'data-step="reassign"',
          description: 'One picker for everything, or one per kind of work (`data-per-item`).',
        },
        {
          name: 'Confirm',
          attribute: 'data-step="confirm"',
          description: 'Member card, consequences and the danger "Remove member" button.',
        },
        {
          name: 'Pending',
          attribute: 'aria-busy',
          description: 'While step-up or `onRemove` runs; the dialog is locked.',
        },
        {
          name: 'Error',
          description:
            'A rejected `onRemove` shows its message on the confirm step; the person can retry.',
        },
        { name: 'Removed', description: 'The dialog closes; with `onUndo` an Undo toast appears.' },
      ],
    },
    interaction: {
      items: [
        'The pickers list `assignees` without the removed member, with "Leave unassigned" first and the signed-in user marked "(you)".',
        'Ticking "Choose a person for each kind of work" copies the current choice to every kind, then each can change.',
        'With `stepUp`, confirming first opens StepUpAuthDialog; cancelling it stays on the confirm step and nothing is sent.',
        'After success the dialog closes; Undo in the toast calls `onUndo` with the same request.',
        'Every opening starts with everything unassigned.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Tab / Shift+Tab',
          action: 'Moves between pickers, the checkbox and buttons; focus stays in the dialog.',
        },
        {
          keys: 'Enter / Space / ArrowDown',
          action: 'Opens a picker; type to search, Enter picks.',
        },
        { keys: 'Space', action: 'Toggles per-item reassignment.' },
        { keys: 'Escape', action: 'Closes a picker, then cancels the flow (not while pending).' },
        {
          keys: 'Alt+T',
          action: 'Moves focus to the toast region, to reach Undo (ToastProvider hotkey).',
        },
      ],
    },
    accessibility: {
      role: 'dialog',
      name: 'The title, "Remove <name>".',
      items: [
        'Each step heading takes focus when the step changes.',
        'Every picker is a labelled select-only combobox; per-item labels say the count and kind ("12 open issues").',
        'The member card shows avatar, name and email; the avatar is decorative.',
        'Consequences use icons and text; unassigned work is a warning item.',
        'The Undo toast is announced politely and stays while focused or hovered.',
      ],
    },
    responsive: {
      items: [
        'Inherits WizardDialog: full width with a 16px gutter on phones; footer buttons stretch below 640px.',
        'Pickers fill the step width and stack one per row.',
      ],
    },
    theme: {
      items: [
        'Member card: `--mn-color-bg-subtle` on a `--mn-color-border-subtle` border with `--mn-radius-md`.',
        'The work summary uses `--mn-color-text-muted`.',
        'Forced colours: the member card gets a CanvasText border.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Hand over open work, then remove, with Undo',
          render: () => (
            <ToastProvider>
              <RemoveMemberFlow
                trigger={<Button variant="danger">Remove Jordan Lee</Button>}
                member={member}
                openWork={work}
                assignees={people}
                currentUserId="me"
                onRemove={() => wait(900)}
                onUndo={() => wait(500)}
              />
            </ToastProvider>
          ),
        },
        {
          title: 'No open work, with re-authentication',
          render: () => (
            <RemoveMemberFlow
              trigger={<Button variant="danger">Remove Jordan Lee</Button>}
              member={member}
              stepUp={{ methods: ['password'], onVerify: () => wait(700) }}
              onRemove={() => wait(900)}
            />
          ),
        },
        {
          title: 'A failing request',
          render: () => (
            <RemoveMemberFlow
              trigger={<Button variant="danger">Remove Jordan Lee</Button>}
              member={member}
              openWork={work.slice(0, 1)}
              assignees={people}
              onRemove={async () => {
                await wait(700);
                throw new Error('Jordan Lee is the only admin of a project.');
              }}
            />
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Load the open work before opening, so the counts are real.',
        'Send the reassignments and the removal in one request, so they succeed or fail together.',
        'Give `onUndo` only when the product can truly restore the member and their assignments.',
      ],
      dont: [
        'Do not remove first and ask about the work afterwards.',
        'Do not use `onUndo` without a ToastProvider: `useUndoToast` throws outside one.',
        'Do not hide "Leave unassigned"; some work is better left in the queue.',
      ],
    },
    related: {
      items: [
        { id: 'wizard-dialog', reason: 'The base for the steps, pending and errors.' },
        { id: 'assignee-picker', reason: 'Picks who takes over the work.' },
        { id: 'member-row', reason: 'Shows the member being removed.' },
        { id: 'consequence-list', reason: 'Lists what happens on the confirm step.' },
        { id: 'undo-toast', reason: 'The Undo after the removal.' },
        { id: 'step-up-auth-dialog', reason: 'Optional re-authentication before the removal.' },
        { id: 'leave-workspace-flow', reason: 'Leaving a workspace yourself.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: [
        'consequence-list',
        'undo-toast',
        'checkbox',
        'form-field',
        'assignee-picker',
        'member-row',
        'step-up-auth-dialog',
        'wizard-dialog',
      ],
    },
    testing: {
      unit: [
        'Happy path: reassign all to one member (removed member not listed, "Leave unassigned" first), confirm with danger tone, `onRemove` gets one reassignment per kind, dialog closes; axe passes.',
        'Per-item reassignment copies the shared choice and allows leaving one kind unassigned.',
        'No open work skips the reassign step; axe passes on the confirm step.',
        'A rejected `onRemove` shows its message and stays.',
        'Step-up: cancelling sends nothing; verifying runs the removal.',
        'With `onUndo` and a ToastProvider, an Undo toast follows and calls `onUndo`; without `onUndo` no provider is needed.',
        'Renders on the server.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
