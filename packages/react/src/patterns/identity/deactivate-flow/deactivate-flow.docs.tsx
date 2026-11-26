import { useState } from 'react';
import { Button, DangerZone, DangerZoneItem } from '@rookcom/monarch-react';
import { DeactivateFlow } from '@rookcom/monarch-react/patterns';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const wait = (ms: number) => new Promise<void>((resolve) => window.setTimeout(resolve, ms));

function PausePartnership() {
  return (
    <DeactivateFlow
      trigger={<Button variant="warning">Deactivate</Button>}
      subject="this partnership"
      reactivateFrom="Settings › Partners"
      consequences={[
        { kind: 'changed', label: 'New referrals are not tracked' },
        { kind: 'changed', label: 'The partner cannot sign in to the portal' },
        { kind: 'kept', label: 'Past referrals and payouts stay in reports' },
      ]}
      reason
      onDeactivate={() => wait(900)}
    />
  );
}

function FromDangerZone() {
  const [active, setActive] = useState(true);
  return (
    <DangerZone>
      <DangerZoneItem
        title={active ? 'Deactivate this tracker' : 'Reactivate this tracker'}
        description={
          active
            ? 'It stops collecting events. Nothing is deleted.'
            : 'It is paused and collects no events.'
        }
        action={
          <DeactivateFlow
            trigger={
              <Button variant={active ? 'warning' : 'secondary'}>
                {active ? 'Deactivate' : 'Reactivate'}
              </Button>
            }
            mode={active ? 'deactivate' : 'reactivate'}
            subject="this tracker"
            reactivateFrom="this page"
            consequences={
              active
                ? [{ kind: 'changed', label: 'Events sent while it is off are ignored' }]
                : [{ kind: 'changed', label: 'It starts collecting events again straight away' }]
            }
            onDeactivate={async () => {
              await wait(600);
              setActive(false);
            }}
            onReactivate={async () => {
              await wait(600);
              setActive(true);
            }}
          />
        }
      />
    </DangerZone>
  );
}

export default {
  id: 'deactivate-flow',
  title: 'DeactivateFlow',
  summary:
    'Confirms a reversible pause (a partnership, a tracker, a person) and says where to turn it back on.',
  sections: {
    purpose: {
      summary:
        'Pausing something is consequential but not destructive. This flow confirms it with a warning tone instead of danger, tells the person exactly where they can reactivate it, lists what changes in the meantime and can record a reason. The same component confirms reactivation.',
      useWhen: [
        'An action stops something working but keeps its data: deactivating a user, pausing a partnership, turning off a tracker or integration.',
        'Reactivating it again, with `mode="reactivate"`.',
      ],
      avoidWhen: [
        'The action deletes data or cannot be undone: use ConfirmDialog with `tone="danger"` or a delete flow.',
        'The change is trivial and instant to reverse, like a toggle: change it directly, no dialog.',
      ],
    },
    designPrinciples: {
      items: [
        'Reversible actions use the warning tone, never danger, so danger keeps its meaning.',
        'Say where to undo it ("You can reactivate it at any time from …"), not just that it can be undone.',
        'List what stops while it is off and what is kept.',
        'Reactivation is a default-tone confirmation with the same layout.',
      ],
    },
    apiReference: { source: 'typescript' },
    typedProperties: {
      source: 'typescript',
      notes: [
        'All ConfirmDialog props pass through except the ones the flow sets itself (tone, copy, phrase, acknowledgement).',
        '`reason` is `true` for the default optional field, or a partial field (`{ label, required, … }`).',
        '`onDeactivate` and `onReactivate` receive `{ reason }` and may return a promise.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'mode',
          values: ['deactivate', 'reactivate'],
          description:
            '`deactivate` (default): warning tone and the reactivation line. `reactivate`: default tone and reactivation copy.',
        },
        {
          prop: 'reason',
          values: ['omitted', 'true', '{ label, required, … }'],
          description: 'Adds an optional or required reason field.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Mode',
          attribute: 'data-mode, data-tone',
          description: '`deactivate` with `data-tone="warning"`, or `reactivate` with `default`.',
        },
        {
          name: 'Pending',
          attribute: 'aria-busy',
          description:
            'The callback is running: the button shows a spinner and nothing dismisses the dialog.',
        },
        {
          name: 'Error',
          attribute: 'role="alert"',
          description:
            'The callback rejected: its message shows above the buttons and the person can retry.',
        },
      ],
    },
    interaction: {
      items: [
        'Confirming calls `onDeactivate` (or `onReactivate` in reactivate mode) with the trimmed reason, then closes.',
        'Cancel and Escape call `onCancel` and close, except while the callback runs.',
        'Every opening starts with an empty reason.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Tab / Shift+Tab',
          action: 'Moves between the reason field and the buttons; focus stays inside.',
        },
        { keys: 'Enter / Space', action: 'Activates the focused button.' },
        { keys: 'Escape', action: 'Cancels, except while the action runs.' },
      ],
    },
    accessibility: {
      role: 'alertdialog',
      name: 'The title ("Deactivate this partnership?"); the description is the accessible description.',
      items: [
        'Focus starts on Cancel, the least consequential action.',
        'The reactivation line has an icon and text, so it never relies on colour.',
        'Consequences are a list under a heading, each with an icon and a spoken kind.',
      ],
    },
    responsive: {
      items: [
        'Follows ConfirmDialog: small width, buttons fill the row on narrow screens.',
        'The reactivation line wraps; its icon stays at the first line.',
      ],
    },
    theme: {
      items: [
        'Warning icon and tone from ConfirmDialog in deactivate mode.',
        'The reactivation line sits on the subtle background token.',
        'In forced-colors mode the reactivation line gets a system-colour border.',
      ],
    },
    examples: {
      items: [
        { title: 'Deactivate with consequences and a reason', render: () => <PausePartnership /> },
        { title: 'Launched from a danger zone, both modes', render: () => <FromDangerZone /> },
      ],
    },
    doDont: {
      do: [
        'Pass `reactivateFrom` naming the real place to turn it back on.',
        'List what stops and what is kept.',
        'Return the request’s promise so pending and errors show in the dialog.',
      ],
      dont: [
        'Do not use it for deletion.',
        'Do not use the danger button style for a reversible pause.',
      ],
    },
    related: {
      items: [
        { id: 'confirm-dialog', reason: 'The dialog it is built on.' },
        { id: 'danger-zone', reason: 'Where the action usually starts.' },
        { id: 'consequence-list', reason: 'How the consequences are shown.' },
        { id: 'remove-member-flow', reason: 'When the person should lose access for good.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['consequence-list', 'confirm-dialog'],
    },
    testing: {
      unit: [
        'Opens a warning alertdialog with the reactivation line and consequences; focus on Cancel; axe passes.',
        'Passes the trimmed reason to onDeactivate and closes.',
        'A rejected onDeactivate shows the error and stays open.',
        'Reactivate mode swaps copy and tone and calls onReactivate.',
        'Escape cancels.',
        'Strings come from messages.',
        'Renders on the server.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
