import { Button, DangerZone, DangerZoneItem } from '@rookcom/monarch-react';
import { LeaveWorkspaceFlow } from '@rookcom/monarch-react/patterns';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const wait = (ms: number) => new Promise<void>((resolve) => window.setTimeout(resolve, ms));

export default {
  id: 'leave-workspace-flow',
  title: 'LeaveWorkspaceFlow',
  summary:
    'Confirmation to leave a workspace, or an explanation with a fix when leaving is blocked.',
  sections: {
    purpose: {
      summary:
        'Lets a member leave a workspace with one clear confirmation: what they lose access to, what stays, and that they need a new invite to come back. When leaving is not allowed, typically because they are the last owner, the same trigger opens a plain dialog that says why and offers the way out (such as transferring ownership) instead of a leave button.',
      useWhen: [
        'A "Leave workspace" (or team, organisation, project) action in settings or a member menu.',
        'The product has rules that can block leaving, such as the last owner.',
      ],
      avoidWhen: [
        'Removing someone else: use RemoveMemberFlow.',
        'Deleting the whole workspace: use DeleteWorkspaceFlow.',
        'Leaving loses nothing and is easy to undo: a plain action with an undo toast is enough.',
      ],
    },
    designPrinciples: {
      items: [
        'One step: leaving needs a decision, not a wizard, so it is a ConfirmDialog with the danger tone.',
        'Say what is lost and what is kept, so people are not afraid their work disappears.',
        'A blocked action explains itself and offers the fix; it never shows a leave button that cannot work.',
        'The blocked state is a normal dialog (no danger tone, no alertdialog), because nothing destructive is offered.',
        'Data and callbacks only: the product decides whether the person is the last owner and runs `onLeave`.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Ready: renders ConfirmDialog (`role="alertdialog"`, `data-tone="danger"`) with the root class `mn-leave-workspace-flow`; ConfirmDialog props such as `confirmPhrase`, `acknowledgement` and `reason` pass through.',
        'Blocked (`isLastOwner` or `blockedReason`): renders Dialog content with `data-blocked` and the same root class. The ref goes to the dialog content in both cases.',
        '`LEAVE_WORKSPACE_FLOW_MESSAGES` holds the English defaults.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        '`apps` fills the first consequence ("You lose access to Projects and Docs"); `consequences` replaces all three.',
        '`fixAction` is any element, usually a Button that opens TransferOwnershipFlow or links to the members page.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'isLastOwner / blockedReason',
          values: ['not set', 'set'],
          description:
            'Not set: a danger confirmation with a Leave workspace button. Set: an explanation, `fixAction` and Close, with no leave button.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Ready',
          attribute: 'data-tone="danger"',
          description: 'Consequences and the Leave workspace button.',
        },
        {
          name: 'Pending',
          attribute: 'aria-busy',
          description: 'While `onLeave` runs; Cancel and Escape are disabled.',
        },
        {
          name: 'Error',
          description:
            'A rejected `onLeave` shows its message in the dialog; the person can retry.',
        },
        {
          name: 'Blocked',
          attribute: 'data-blocked',
          description: 'The reason as the dialog description, the fix action and Close.',
        },
      ],
    },
    interaction: {
      items: [
        'The trigger opens the confirmation; Cancel, Escape or Leave workspace close it (Leave after `onLeave` succeeds).',
        'In the blocked state, Close or Escape closes the dialog and `fixAction` does whatever the product gave it.',
        'Focus returns to the trigger when the dialog closes.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Tab / Shift+Tab',
          action: 'Moves between the dialog buttons; focus stays in the dialog.',
        },
        { keys: 'Enter / Space', action: 'Activates the focused button.' },
        { keys: 'Escape', action: 'Cancels (not while leaving is pending).' },
      ],
    },
    accessibility: {
      role: 'alertdialog when ready; dialog when blocked',
      name: 'The title ("Leave Northfield?" or "You can’t leave yet"), described by the description or the reason.',
      items: [
        'Focus starts on Cancel in the confirmation (the least destructive action) and on Close when blocked.',
        'Consequences use ConsequenceList: each item has an icon and spoken kind, so colour is never the only signal.',
        'The blocked reason is the dialog description, so it is read when the dialog opens.',
      ],
    },
    responsive: {
      items: [
        'Small dialog that fills the width with a 16px gutter on phones; footer buttons wrap.',
        'Long workspace names and app lists wrap.',
      ],
    },
    theme: {
      items: [
        'Ready state: ConfirmDialog danger tone (`--mn-color-danger-*`).',
        'Blocked state: warning icon on `--mn-color-warning-subtle` in `--mn-color-warning-strong`.',
        'Forced colours: the icon gets a CanvasText border.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Leave from a danger zone',
          stack: true,
          render: () => (
            <DangerZone>
              <DangerZoneItem
                title="Leave this workspace"
                description="You lose access to its projects and documents."
                action={
                  <LeaveWorkspaceFlow
                    trigger={<Button variant="danger">Leave workspace</Button>}
                    workspaceName="Northfield"
                    apps={['Projects', 'Docs', 'Calendar']}
                    onLeave={() => wait(900)}
                  />
                }
              />
            </DangerZone>
          ),
        },
        {
          title: 'Blocked for the last owner',
          render: () => (
            <LeaveWorkspaceFlow
              trigger={<Button variant="danger">Leave workspace</Button>}
              workspaceName="Northfield"
              isLastOwner
              fixAction={<Button variant="primary">Transfer ownership</Button>}
              onLeave={() => wait(900)}
            />
          ),
        },
        {
          title: 'Blocked by a product rule',
          render: () => (
            <LeaveWorkspaceFlow
              trigger={<Button variant="danger">Leave workspace</Button>}
              blockedReason="Your account is managed by your organisation. Ask an admin to remove you."
              onLeave={() => wait(900)}
            />
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Name the apps or areas the person loses with `apps`.',
        'Pass `isLastOwner` from the server and give a `fixAction` that opens TransferOwnershipFlow.',
        'Keep the trigger enabled when blocked, so the dialog can explain why.',
      ],
      dont: [
        'Do not disable the leave button without saying why.',
        'Do not use `window.confirm` for leaving.',
        'Do not promise content will be kept unless the product keeps it; replace `consequences` if it does not.',
      ],
    },
    related: {
      items: [
        { id: 'confirm-dialog', reason: 'The base of the ready state.' },
        { id: 'consequence-list', reason: 'What is lost and what stays.' },
        { id: 'transfer-ownership-flow', reason: 'The usual fix for the last owner.' },
        { id: 'remove-member-flow', reason: 'Removing someone else.' },
        { id: 'danger-zone', reason: 'Where the action usually lives.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['button', 'consequence-list', 'confirm-dialog', 'dialog'],
    },
    testing: {
      unit: [
        'Ready: alertdialog with danger tone, consequences, focus on Cancel, axe passes; Leave runs `onLeave`, closes and returns focus to the trigger.',
        'Pending locks the dialog; a rejection shows its message.',
        'Last owner: reason as description, fix action works, no leave button, axe passes; Close closes without leaving.',
        'A product `blockedReason` blocks too; Escape calls onCancel.',
        'Strings come from messages; `consequences` replaces the defaults.',
        'Renders on the server, ready and blocked.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
