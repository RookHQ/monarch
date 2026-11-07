import { Button, DangerZone, DangerZoneItem } from '@rookcom/monarch-react';
import { DeleteWorkspaceFlow } from '@rookcom/monarch-react/patterns';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const wait = (ms: number) => new Promise<void>((resolve) => window.setTimeout(resolve, ms));

function FromDangerZone() {
  return (
    <DangerZone description="These actions affect everyone in the workspace.">
      <DangerZoneItem
        title="Delete this workspace"
        description="Removes the workspace, its apps and its records for every member."
        action={
          <DeleteWorkspaceFlow
            trigger={<Button variant="danger">Delete workspace</Button>}
            workspaceName="north-studio"
            impact={{ apps: 4, members: 12, records: 18340, subscriptions: 1, domains: 2 }}
            onExport={() => wait(800)}
            onTransferInstead={() => {}}
            stepUp={{ methods: ['password', 'totp'], onVerify: () => wait(600) }}
            onDelete={() => wait(1200)}
          />
        }
      />
    </DangerZone>
  );
}

function Scheduled() {
  return (
    <DeleteWorkspaceFlow
      trigger={<Button variant="danger">Delete workspace</Button>}
      workspaceName="north-studio"
      impact={{ apps: 2, members: 5, records: 940 }}
      gracePeriodDays={30}
      onDelete={() => wait(800)}
    />
  );
}

function LoadingImpact() {
  return (
    <DeleteWorkspaceFlow
      trigger={<Button variant="danger">Delete workspace</Button>}
      workspaceName="north-studio"
      impactLoading
      onDelete={() => wait(800)}
    />
  );
}

function FailingDelete() {
  return (
    <DeleteWorkspaceFlow
      trigger={<Button variant="danger">Delete workspace</Button>}
      workspaceName="north-studio"
      impact={{ apps: 1, members: 3 }}
      onDelete={async () => {
        await wait(800);
        throw new Error('The workspace could not be deleted. Try again.');
      }}
    />
  );
}

export default {
  id: 'delete-workspace-flow',
  title: 'DeleteWorkspaceFlow',
  summary:
    'The full workspace deletion flow: impact, optional export and transfer, type the name, step-up, done.',
  sections: {
    purpose: {
      summary:
        'Deleting a workspace is a level 4 action (docs/guides/destructive-actions.md): it removes everything for every member. This flow gives every product the same sequence: what will be deleted, a chance to export, a chance to transfer ownership instead, typing the workspace name, re-authentication, then the delete and a closing screen. The product supplies the counts and the callbacks; the flow presents the steps.',
      useWhen: [
        'An owner deletes a whole workspace, team or organisation.',
        'The backend deletes after a grace period and the person should know until when they can cancel (`gracePeriodDays`).',
      ],
      avoidWhen: [
        'Deleting one record or project: use ConfirmDialog (with `confirmPhrase` if it cascades).',
        'Deleting the person’s own account: use DeleteAccountFlow.',
        'Removing many items from a table: use BulkDestructiveFlow.',
      ],
    },
    designPrinciples: {
      items: [
        'Show the impact before asking for anything: counts, not vague warnings.',
        'Offer the safer alternatives (export, transfer ownership) before the destructive step, never after.',
        'The destructive button names the action ("Delete workspace", "Schedule deletion") and is the only danger-styled button.',
        'Typing the workspace name proves the person means this workspace; step-up proves it is them.',
        'With a grace period, say when the deletion happens and until when it can be cancelled.',
      ],
    },
    apiReference: { source: 'typescript' },
    typedProperties: {
      source: 'typescript',
      notes: [
        '`impact` takes `{ apps?, members?, records?, subscriptions?, domains? }`; counts above 0 become lines. `extraConsequences` adds ConsequenceList items.',
        '`stepUp` is a `StepUpRequirement` (`methods`, `onVerify`, `description`, …), shared by every level 4 flow.',
        'All other WizardDialog props (`open`, `onOpenChange`, `trigger`, `onCancel`, `size`, …) pass through; `wizardMessages` translates Back, Cancel, Continue, Done and the progress text.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'gracePeriodDays',
          values: ['undefined', 'number'],
          description:
            'Immediate deletion, or scheduled deletion with "you can cancel until <date>" copy and a "Schedule deletion" button.',
        },
        {
          prop: 'onExport',
          values: ['undefined', 'function'],
          description: 'Adds the optional "Keep a copy" step.',
        },
        {
          prop: 'onTransferInstead',
          values: ['undefined', 'function'],
          description: 'Adds the "Transfer it instead?" step.',
        },
        {
          prop: 'stepUp',
          values: ['undefined', 'StepUpRequirement'],
          description: 'Requires re-authentication before the delete runs.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Impact loading',
          attribute: 'aria-busy (impact list)',
          description: '`impactLoading`: skeleton rows, and Continue waits with a reason.',
        },
        {
          name: 'Impact error',
          attribute: 'data-state="error" (impact list)',
          description:
            '`impactError` replaces the list. The person can still continue: the name check and step-up still guard the delete.',
        },
        {
          name: 'Name not typed',
          attribute: 'disabled (delete button)',
          description:
            'The delete button stays disabled until the name matches; the reason describes it.',
        },
        {
          name: 'Deleting',
          attribute: 'aria-busy, data-busy',
          description:
            'While step-up or `onDelete` runs, the flow cannot be dismissed. A rejection shows its message on the confirm step.',
        },
        {
          name: 'Scheduled',
          attribute: 'data-scheduled',
          description: 'Set when `gracePeriodDays` is above 0.',
        },
        {
          name: 'Current step',
          attribute: 'data-step, data-kind',
          description: '`impact`, `export`, `transfer`, `confirm` or `done` (from WizardDialog).',
        },
      ],
    },
    interaction: {
      items: [
        'Continue moves through the steps; Back returns; Cancel, Escape and the close button dismiss it (except while deleting).',
        'Export data calls `onExport` and announces when it has started; it never blocks Continue.',
        'Transfer ownership instead closes the flow without `onCancel` and calls `onTransferInstead`, so the app can open its transfer flow.',
        'The delete button opens the step-up dialog first when `stepUp` is set; cancelling it returns to the confirm step without deleting.',
        'After `onDelete` resolves, the done screen appears; closing it calls `onDone`.',
        'Every opening starts at the first step with an empty name field.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab / Shift+Tab', action: 'Moves through the step content and footer buttons.' },
        { keys: 'Enter (in the name field)', action: 'Deletes once the name matches.' },
        { keys: 'Escape', action: 'Cancels the flow, or the step-up dialog when it is open.' },
      ],
    },
    accessibility: {
      role: 'dialog (aria-modal), with a nested dialog for step-up',
      name: '`messages.title` ("Delete workspace").',
      items: [
        'Each new step moves focus to its heading, so it is announced.',
        'The impact list has an icon and text for each line; colour is not the only signal.',
        'The disabled delete button is described by why it is disabled.',
        'The export result is announced through a polite status region.',
        'Errors from `onDelete` are announced assertively and linked to the delete button.',
      ],
    },
    responsive: {
      items: [
        'Works at 390px: footer buttons fill the row, step action buttons take the full width.',
        'Long workspace names wrap instead of overflowing.',
      ],
    },
    theme: {
      items: [
        'Uses WizardDialog, ConsequenceList and Button styling; only the step action row is added.',
        'Forced-colors mode keeps the status text in CanvasText.',
      ],
    },
    examples: {
      items: [
        {
          title: 'From a danger zone, with export, transfer and step-up',
          render: () => <FromDangerZone />,
        },
        { title: 'Scheduled deletion after 30 days', render: () => <Scheduled /> },
        { title: 'Impact still loading', render: () => <LoadingImpact /> },
        { title: 'A failing delete', render: () => <FailingDelete /> },
      ],
    },
    doDont: {
      do: [
        'Launch it from a DangerZoneItem in workspace settings.',
        'Pass real counts; load them when the flow opens and pass `impactLoading` meanwhile.',
        'Pass `stepUp` for every workspace deletion.',
        'Return the request’s promise from `onDelete`.',
      ],
      dont: [
        'Do not run the delete before the confirm step.',
        'Do not replace it with `window.confirm`.',
        'Do not promise a grace period the backend does not honour.',
      ],
    },
    related: {
      items: [
        { id: 'danger-zone', reason: 'Where the flow is launched from.' },
        {
          id: 'transfer-ownership-flow',
          reason: 'The alternative offered by `onTransferInstead`.',
        },
        { id: 'delete-account-flow', reason: 'Deleting the person’s own account.' },
        { id: 'wizard-dialog', reason: 'The base it is built on.' },
        { id: 'step-up-auth-dialog', reason: 'The re-authentication step.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: [
        'button',
        'consequence-list',
        'type-to-confirm-input',
        'step-up-auth-dialog',
        'wizard-dialog',
      ],
    },
    testing: {
      unit: [
        'Walks impact, export, transfer and confirm to the done screen; onDone runs when it closes; axe passes.',
        'Export and transfer steps are omitted without their callbacks.',
        'Impact loading blocks Continue; an impact error is shown.',
        'Transfer instead closes without onCancel and calls back.',
        'Step-up cancel stays without deleting; step-up success deletes.',
        'A failed delete shows the error on the confirm step.',
        'Grace period copy uses the formatted date on the impact and done screens.',
        'Every opening clears the typed name; all strings come from messages.',
        'Renders on the server.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
