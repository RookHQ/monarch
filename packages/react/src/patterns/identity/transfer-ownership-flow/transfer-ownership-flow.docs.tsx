import { Button, DangerZone, DangerZoneItem } from '@rookcom/monarch-react';
import {
  TransferOwnershipFlow,
  type TransferOwnershipCandidate,
} from '@rookcom/monarch-react/patterns';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const wait = (ms: number) => new Promise<void>((resolve) => window.setTimeout(resolve, ms));

const members: TransferOwnershipCandidate[] = [
  { id: 'me', name: 'Sam Rivera', email: 'sam@example.com', role: 'owner' },
  { id: 'm1', name: 'Alex Morgan', email: 'alex@example.com', role: 'admin' },
  { id: 'm2', name: 'Priya Shah', email: 'priya@example.com', role: 'admin' },
  { id: 'm3', name: 'Jordan Lee', email: 'jordan@example.com', role: 'member' },
  {
    id: 'm4',
    name: 'Chris Novak',
    email: 'chris@example.com',
    role: 'admin',
    ineligibleReason: 'Invite not accepted yet',
  },
];

const roles = [
  { value: 'admin', label: 'Admin' },
  { value: 'member', label: 'Member' },
];

export default {
  id: 'transfer-ownership-flow',
  title: 'TransferOwnershipFlow',
  summary:
    'Three-step dialog to hand a workspace to another member: pick, review what changes, confirm.',
  sections: {
    purpose: {
      summary:
        'Moves ownership of a workspace to another member without surprises. The person picks the new owner from the members the product passes in, sees what changes for themselves and for the new owner (and chooses their own role afterwards), confirms, optionally re-authenticates, and ends on a screen that says whether the transfer took effect or waits for the new owner to accept.',
      useWhen: [
        'A workspace, organisation or project has a single owner who can hand it over.',
        'The product needs the new owner to accept before the transfer takes effect.',
        'Only some members (for example admins) may become the owner and the others need a reason.',
      ],
      avoidWhen: [
        'Changing an ordinary member role: use the role select in MemberRow.',
        'Several owners can coexist: promote the member instead of transferring.',
        'The owner is leaving and must transfer first: start here from the blocked LeaveWorkspaceFlow `fixAction`.',
      ],
    },
    designPrinciples: {
      items: [
        'Data and callbacks only: the product passes members and runs `onTransfer`; the flow presents the steps.',
        'Say what changes for both sides before asking to confirm.',
        'Not destructive, but consequential: the confirm button uses the default tone and names the action, "Transfer ownership".',
        'Ineligible members stay visible with their reason, so nobody wonders where they went.',
        'The last screen tells the truth about the state: done, or waiting for acceptance with a way to withdraw.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Renders a WizardDialog (ref forwarded to its content) with the root class `mn-transfer-ownership-flow`. WizardDialog props such as `trigger`, `open`, `onOpenChange`, `onCancel` and `size` pass through.',
        '`TRANSFER_OWNERSHIP_FLOW_MESSAGES` holds the English defaults.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        '`onTransfer` receives `{ memberId, yourRole? }`; `yourRole` is present only with `yourRoleOptions`.',
        '`stepUp` takes the StepUpAuthDialog options `methods`, `onVerify`, `description`, `forgotHref`, `onForgot`, `errorMessage` and `messages`; every account flow uses the same shape.',
        '`messages` also accepts the WizardDialog strings (`back`, `cancel`, `continue`, `done`, `stepOf`, `error`).',
      ],
    },
    variants: {
      items: [
        {
          prop: 'requiresAcceptance',
          values: ['false', 'true'],
          description:
            'Closing screen: "<name> is now the owner", or "Waiting for <name> to accept" with an optional Cancel transfer button.',
        },
        {
          prop: 'yourRoleOptions',
          values: ['undefined', 'role options'],
          description:
            'Without it, the review says "You become an admin"; with it, a select picks your role.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Pick',
          attribute: 'data-step="pick"',
          description:
            'Continue stays disabled until a member is chosen, with the reason next to it.',
        },
        {
          name: 'No candidates',
          description:
            'A warning explains that nobody can become the owner; Continue stays disabled.',
        },
        {
          name: 'Review',
          attribute: 'data-step="review"',
          description:
            'Two consequence lists, "For you" and "For <name>", and the optional role select.',
        },
        {
          name: 'Confirm',
          attribute: 'data-step="confirm"',
          description: 'The chosen member as a card and the "Transfer ownership" button.',
        },
        {
          name: 'Pending',
          attribute: 'aria-busy',
          description: 'While step-up or `onTransfer` runs, the dialog is locked.',
        },
        {
          name: 'Error',
          description:
            'A rejected `onTransfer` shows its message on the confirm step; the person can retry.',
        },
        {
          name: 'Done',
          attribute: 'data-kind="done"',
          description: 'The new owner, or the waiting state when `data-acceptance="required"`.',
        },
      ],
    },
    interaction: {
      items: [
        'The picker lists every candidate except `currentOwnerId`; ineligible ones are disabled with their reason as the hint.',
        'Continue moves to the next step and focuses its heading; Back returns without losing the choice.',
        'With `stepUp`, confirming first opens StepUpAuthDialog; cancelling it stays on the confirm step and nothing is sent.',
        'Cancel transfer on the waiting screen calls `onCancelTransfer`, shows a pending button, and then confirms the cancellation.',
        'Every opening starts on the first step with nothing picked.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Tab / Shift+Tab',
          action: 'Moves between the picker, fields and buttons; focus stays in the dialog.',
        },
        {
          keys: 'Enter / Space / ArrowDown',
          action: 'Opens the member picker; type to search, Enter picks.',
        },
        { keys: 'Enter', action: 'Continues from a step (submits the step form).' },
        { keys: 'Escape', action: 'Closes the picker, then cancels the flow (not while pending).' },
      ],
    },
    accessibility: {
      role: 'dialog',
      name: 'The title, "Transfer ownership" by default.',
      items: [
        'Each step heading takes focus when the step changes, so it is announced.',
        'The disabled Continue is described by why it is disabled.',
        'The picker is a labelled select-only combobox ("New owner"); disabled options keep their reason in their text.',
        'Consequences are lists with icons and text, so colour is never the only signal.',
        'Errors are announced assertively and linked to the confirm button.',
      ],
    },
    responsive: {
      items: [
        'Inherits WizardDialog: full width with a 16px gutter on phones; footer buttons stretch and Back takes its own row below 640px.',
        'The picker and member card fill the step width; long names wrap.',
      ],
    },
    theme: {
      items: [
        'Member card: `--mn-color-bg-subtle` on a `--mn-color-border-subtle` border with `--mn-radius-md`.',
        'Everything else comes from WizardDialog, AssigneePicker, ConsequenceList and MemberRow tokens.',
        'Forced colours: the member card gets a CanvasText border.',
      ],
    },
    examples: {
      items: [
        {
          title: 'From a danger zone',
          stack: true,
          render: () => (
            <DangerZone>
              <DangerZoneItem
                title="Transfer ownership"
                description="Another member becomes the owner. You choose your role afterwards."
                action={
                  <TransferOwnershipFlow
                    trigger={<Button>Transfer ownership</Button>}
                    candidates={members}
                    currentOwnerId="me"
                    eligibleRoles={['admin']}
                    yourRoleOptions={roles}
                    onTransfer={() => wait(900)}
                  />
                }
              />
            </DangerZone>
          ),
        },
        {
          title: 'Waiting for acceptance, with re-authentication',
          render: () => (
            <TransferOwnershipFlow
              trigger={<Button>Transfer ownership</Button>}
              candidates={members}
              currentOwnerId="me"
              requiresAcceptance
              onCancelTransfer={() => wait(600)}
              stepUp={{ methods: ['password', 'totp'], onVerify: () => wait(700) }}
              onTransfer={() => wait(900)}
            />
          ),
        },
        {
          title: 'A failing request',
          render: () => (
            <TransferOwnershipFlow
              trigger={<Button>Transfer ownership</Button>}
              candidates={members}
              currentOwnerId="me"
              onTransfer={async () => {
                await wait(700);
                throw new Error('The new owner’s account is suspended.');
              }}
            />
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Pass every member and let `eligibleRoles` or `ineligibleReason` explain who cannot be picked.',
        'Set `requiresAcceptance` when the server waits for the new owner, and give `onCancelTransfer`.',
        'Ask for step-up when ownership controls billing or deletion.',
      ],
      dont: [
        'Do not use a danger tone: nothing is deleted, so keep the default tone with an explicit label.',
        'Do not hide ineligible members; say why they cannot be picked.',
        'Do not say "is now the owner" when the transfer still waits for acceptance.',
      ],
    },
    related: {
      items: [
        { id: 'wizard-dialog', reason: 'The base for the steps, progress, pending and errors.' },
        { id: 'assignee-picker', reason: 'The member picker on the first step.' },
        { id: 'consequence-list', reason: 'What changes for you and for the new owner.' },
        { id: 'step-up-auth-dialog', reason: 'Optional re-authentication before the transfer.' },
        {
          id: 'leave-workspace-flow',
          reason: 'Blocked for the last owner; its fix action usually opens this flow.',
        },
        { id: 'danger-zone', reason: 'Where the flow is usually started.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: [
        'button',
        'alert',
        'consequence-list',
        'form-field',
        'assignee-picker',
        'member-row',
        'step-up-auth-dialog',
        'wizard-dialog',
      ],
    },
    testing: {
      unit: [
        'Happy path: pick, review (role select changes the consequence), confirm with the default tone, `onTransfer` gets `{ memberId, yourRole }`, done screen focused, Done closes; axe passes on the first step.',
        'The current owner is left out; `eligibleRoles` and `ineligibleReason` disable members and show the reason.',
        'No candidates: a warning and a disabled Continue.',
        'Acceptance: the waiting screen and Cancel transfer.',
        'A rejected `onTransfer` stays on the confirm step with the message and can be retried.',
        'Step-up: cancelling stays and sends nothing; verifying runs the transfer.',
        'Renders on the server.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
