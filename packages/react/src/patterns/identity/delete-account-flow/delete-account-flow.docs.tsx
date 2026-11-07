import { useState } from 'react';
import { Button, DangerZone, DangerZoneItem } from '@rookcom/monarch-react';
import { DeleteAccountFlow, type OwnedWorkspace } from '@rookcom/monarch-react/patterns';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const wait = (ms: number) => new Promise<void>((resolve) => window.setTimeout(resolve, ms));

const reasons = [
  { value: 'not-needed', label: 'I no longer need it' },
  { value: 'other-tool', label: 'I use another tool' },
  { value: 'too-complex', label: 'It is too complicated' },
  { value: 'other', label: 'Something else' },
];

function FromDangerZone() {
  return (
    <DangerZone description="Deleting your account cannot be undone.">
      <DangerZoneItem
        title="Delete your account"
        description="Removes your profile and your access to every workspace."
        action={
          <DeleteAccountFlow
            trigger={<Button variant="danger">Delete account</Button>}
            confirmPhrase="sam@example.com"
            consequences={[
              { kind: 'removed', label: 'Your profile and sign-in methods' },
              { kind: 'changed', label: 'You leave 3 workspaces' },
              { kind: 'kept', label: 'Content you created stays in its workspace' },
            ]}
            onExport={() => wait(800)}
            reasons={reasons}
            stepUp={{ methods: ['passkey', 'password'], onVerify: () => wait(600) }}
            onDelete={() => wait(1200)}
          />
        }
      />
    </DangerZone>
  );
}

function OwnsWorkspaces() {
  const [owned, setOwned] = useState<OwnedWorkspace[]>([
    { id: 'w1', name: 'North studio', memberCount: 8 },
    { id: 'w2', name: 'Side project', memberCount: 1 },
  ]);
  return (
    <DeleteAccountFlow
      trigger={<Button variant="danger">Delete account</Button>}
      confirmPhrase="sam@example.com"
      ownedWorkspaces={owned}
      onResolveWorkspace={(ws) => setOwned((list) => list.filter((w) => w.id !== ws.id))}
      onDelete={() => wait(800)}
    />
  );
}

function FailingDelete() {
  return (
    <DeleteAccountFlow
      trigger={<Button variant="danger">Delete account</Button>}
      confirmPhrase="sam@example.com"
      onDelete={async () => {
        await wait(800);
        throw new Error('Your account could not be deleted. Try again.');
      }}
    />
  );
}

export default {
  id: 'delete-account-flow',
  title: 'DeleteAccountFlow',
  summary:
    'The full account deletion flow: ownership check, optional export and reason, type to confirm, step-up, farewell.',
  sections: {
    purpose: {
      summary:
        'Deleting one’s own account is a level 4 action (docs/guides/destructive-actions.md). This flow is the one sequence every product uses: it blocks while the person still owns workspaces alone (and offers a way to resolve each), offers an export and an optional reason, asks them to type their email, re-authenticates them, deletes, and says goodbye. It replaces the separate, diverging copies of this flow that products had.',
      useWhen: ['A signed-in person deletes their own account from account settings.'],
      avoidWhen: [
        'An admin removes someone else: use RemoveMemberFlow.',
        'The account should only be paused: use DeactivateFlow.',
        'Erasing a data subject’s personal data on request: use GdprEraseFlow.',
      ],
    },
    designPrinciples: {
      items: [
        'Never strand a workspace: deletion is blocked while the person is its only owner, and the block says how to fix it.',
        'The export and the reason are optional and never block the person.',
        'Typing the account email and re-authenticating are the two guards before the irreversible step.',
        'The last screen is a calm farewell, not an error-red banner.',
      ],
    },
    apiReference: { source: 'typescript' },
    typedProperties: {
      source: 'typescript',
      notes: [
        '`ownedWorkspaces` is `{ id, name, memberCount }[]`; each row gets `renderWorkspaceAction(ws)`, or a "Transfer or delete" button calling `onResolveWorkspace(ws)`.',
        '`onDelete` receives `{ reason?, details? }`: the chosen reason value and the trimmed written comment, each absent when empty.',
        '`stepUp` is a `StepUpRequirement` (`methods`, `onVerify`, `description`, …), shared by every level 4 flow.',
        'Other WizardDialog props pass through; `wizardMessages` translates its buttons and progress.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'ownedWorkspaces',
          values: ['[]', 'OwnedWorkspace[]'],
          description: 'A non-empty list blocks the first step and lists the workspaces.',
        },
        {
          prop: 'onExport',
          values: ['undefined', 'function'],
          description: 'Adds the optional "Download your data" step.',
        },
        {
          prop: 'reasons',
          values: ['undefined', 'DeleteAccountReason[]'],
          description:
            'Adds the optional "Why are you leaving?" step; `showDetails` controls its comment field.',
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
          name: 'Blocked by ownership',
          attribute: 'data-blocked',
          description:
            'Owned workspaces remain. Continue is disabled and described by `messages.ownedBlocked`.',
        },
        {
          name: 'Phrase not typed',
          attribute: 'disabled (delete button)',
          description: 'The delete button stays disabled until the phrase matches exactly.',
        },
        {
          name: 'Deleting',
          attribute: 'aria-busy, data-busy',
          description:
            'While step-up or `onDelete` runs the flow cannot be dismissed; a rejection shows its message.',
        },
        {
          name: 'Current step',
          attribute: 'data-step, data-kind',
          description: '`review`, `export`, `reason`, `confirm` or `done` (from WizardDialog).',
        },
      ],
    },
    interaction: {
      items: [
        'The first step unblocks as soon as the app passes an empty `ownedWorkspaces` list.',
        'Export data calls `onExport` and announces when it has started.',
        'The reason step sends the choice and the trimmed text to `onDelete`.',
        'The delete button opens step-up first when `stepUp` is set; cancelling it returns to the confirm step.',
        'Closing the farewell screen calls `onDone`, e.g. to sign out.',
        'Every opening starts clean: no typed phrase, reason or text.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab / Shift+Tab', action: 'Moves through the step content and footer buttons.' },
        {
          keys: 'Space / Enter / arrows (reason)',
          action: 'Opens the reason list and picks a reason.',
        },
        { keys: 'Enter (in the confirmation field)', action: 'Deletes once the phrase matches.' },
        {
          keys: 'Escape',
          action: 'Cancels the flow, or closes the open reason list or step-up dialog.',
        },
      ],
    },
    accessibility: {
      role: 'dialog (aria-modal), with a nested dialog for step-up',
      name: '`messages.title` ("Delete account").',
      items: [
        'Each step change moves focus to the step heading.',
        'The owned-workspace list is named by its introduction; each default action names its workspace ("Transfer or delete North studio").',
        'A blocked Continue is described by the reason it is blocked.',
        'The reason select and the text field have visible labels.',
      ],
    },
    responsive: {
      items: [
        'Works at 390px: workspace actions move under the name and fill the row.',
        'Long workspace names and emails wrap.',
      ],
    },
    theme: {
      items: [
        'The owned-workspace list uses the subtle border and muted text tokens; everything else comes from the base components.',
        'Forced-colors mode draws the list borders in CanvasText.',
      ],
    },
    examples: {
      items: [
        {
          title: 'From a danger zone, with export, reason and step-up',
          render: () => <FromDangerZone />,
        },
        { title: 'Blocked by owned workspaces', render: () => <OwnsWorkspaces /> },
        { title: 'A failing delete', render: () => <FailingDelete /> },
      ],
    },
    doDont: {
      do: [
        'Pass the workspaces the person owns alone, and refresh the list as they resolve them.',
        'Use the account email as `confirmPhrase`.',
        'Pass `stepUp` for every account deletion.',
        'Sign the person out in `onDone`.',
      ],
      dont: [
        'Do not let someone delete an account that is the only owner of a workspace.',
        'Do not make the reason required.',
        'Do not keep a product-specific copy of this flow.',
      ],
    },
    related: {
      items: [
        { id: 'danger-zone', reason: 'Where the flow is launched from.' },
        { id: 'transfer-ownership-flow', reason: 'Resolving an owned workspace.' },
        { id: 'delete-workspace-flow', reason: 'Deleting an owned workspace.' },
        { id: 'deactivate-flow', reason: 'Pausing an account instead of deleting it.' },
        { id: 'wizard-dialog', reason: 'The base it is built on.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: [
        'button',
        'consequence-list',
        'form-field',
        'select',
        'text-field',
        'type-to-confirm-input',
        'step-up-auth-dialog',
        'wizard-dialog',
      ],
    },
    testing: {
      unit: [
        'Walks review, export, reason and confirm to the farewell; onDelete gets the reason and trimmed text; onDone runs on close; axe passes.',
        'Owned workspaces block the first step, list each with an action, and unblock when the list empties; axe passes.',
        'renderWorkspaceAction replaces the default button.',
        'Optional steps are skipped without their props; no reason sends `{}`.',
        'Type-to-confirm is case-sensitive.',
        'Step-up cancel stays without deleting; success deletes.',
        'A failed delete shows the error on the confirm step.',
        'Strings come from messages; renders on the server.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
