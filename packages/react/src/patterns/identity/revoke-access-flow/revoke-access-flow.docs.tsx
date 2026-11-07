import { Button, DangerZone, DangerZoneItem } from '@rookcom/monarch-react';
import { RevokeAccessFlow } from '@rookcom/monarch-react/patterns';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const wait = (ms: number) => new Promise<void>((resolve) => window.setTimeout(resolve, ms));
const hoursAgo = (h: number) => new Date(Date.now() - h * 3_600_000);

function RevokeKey() {
  return (
    <RevokeAccessFlow
      trigger={<Button variant="danger">Revoke key</Button>}
      kind="api-key"
      items={[
        {
          id: 'key-1',
          label: 'Build server',
          detail: 'sk_live_…4f2a · read and write',
          lastUsed: hoursAgo(3),
        },
      ]}
      requireConfirmPhrase="Build server"
      stepUp={{
        methods: ['password', 'totp'],
        onVerify: async () => {
          await wait(600);
        },
      }}
      onRevoke={() => wait(800)}
    />
  );
}

function SignOutSessions() {
  return (
    <RevokeAccessFlow
      trigger={<Button>Sign out other sessions</Button>}
      kind="session"
      items={[
        { id: 's-1', label: 'Firefox on Linux', detail: 'Lisbon', lastUsed: hoursAgo(26) },
        { id: 's-2', label: 'Safari on iPhone', detail: 'Porto', lastUsed: hoursAgo(1) },
        { id: 's-3', label: 'Chrome on Windows', detail: 'Unknown location', lastUsed: null },
      ]}
      onRevoke={() => wait(800)}
    />
  );
}

function FromDangerZone() {
  return (
    <DangerZone>
      <DangerZoneItem
        title="Revoke this OAuth client"
        description="Apps using it can no longer sign people in."
        action={
          <RevokeAccessFlow
            trigger={<Button variant="danger">Revoke client</Button>}
            kind="oauth-client"
            items={[
              {
                id: 'c-1',
                label: 'Reporting app',
                detail: 'Client ID 9f1c…e2',
                lastUsed: hoursAgo(50),
              },
            ]}
            onRevoke={() => wait(800)}
          />
        }
      />
    </DangerZone>
  );
}

export default {
  id: 'revoke-access-flow',
  title: 'RevokeAccessFlow',
  summary:
    'Confirms revoking API keys, OAuth clients, portal access, sessions or devices, with when each was last used.',
  sections: {
    purpose: {
      summary:
        'Revoking a credential is one decision, but the person needs to see exactly which credential, when it was last used and what stops working. This flow is a single ConfirmDialog with default copy for each kind of access, an item list with relative "last used" times, optional type-to-confirm and optional step-up.',
      useWhen: [
        'Revoking one or more API keys or OAuth clients.',
        'Removing someone’s portal access, or signing out sessions or devices.',
      ],
      avoidWhen: [
        'Removing a member from a workspace, with transfer of their work: use RemoveMemberFlow.',
        'Pausing access that should come back: use DeactivateFlow.',
      ],
    },
    designPrinciples: {
      items: [
        'One dialog, not a wizard: there is nothing to choose or export first, so the list, the consequences and the button fit on one screen.',
        'Show "last used" so the person can tell a live key from a forgotten one.',
        'Keys and clients cannot be restored, so they use the danger tone; sessions, devices and portal access use warning because people can sign in again.',
        'For keys and clients in production, ask for the name to be typed and a step-up.',
      ],
    },
    apiReference: { source: 'typescript' },
    typedProperties: {
      source: 'typescript',
      notes: [
        '`items` is `{ id, label, detail?, lastUsed? }[]`; `lastUsed: null` reads "Never used".',
        '`stepUp` is `{ methods, onVerify }`, the same shape as in the other destructive flows.',
        '`messages.kinds[kind]` overrides the title, button, description and consequences for one kind; plain strings sit at the top level.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'kind',
          values: ['api-key', 'oauth-client', 'portal-access', 'session', 'device'],
          description: 'Drives the default copy and tone.',
        },
        {
          prop: 'tone',
          values: ['default', 'warning', 'danger'],
          description: 'Default: `danger` for keys and clients, `warning` for the rest.',
        },
        {
          prop: 'requireConfirmPhrase',
          values: ['omitted', 'string'],
          description: 'Type-to-confirm; meant for `api-key` and `oauth-client`.',
        },
        {
          prop: 'stepUp',
          values: ['omitted', '{ methods, onVerify }'],
          description: 'Re-authentication before `onRevoke` runs.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Kind and tone',
          attribute: 'data-kind, data-tone',
          description: 'Which kind of access, and the resulting tone.',
        },
        {
          name: 'Blocked',
          attribute: 'disabled (confirm button)',
          description: 'The confirm phrase has not been typed.',
        },
        {
          name: 'Pending',
          attribute: 'aria-busy',
          description:
            'Step-up or `onRevoke` is running: the button shows a spinner and nothing dismisses the dialog.',
        },
        {
          name: 'Error',
          attribute: 'role="alert"',
          description:
            '`onRevoke` rejected: the message shows above the buttons and the person can retry.',
        },
      ],
    },
    interaction: {
      items: [
        'Confirming first opens the step-up dialog when `stepUp` is set; cancelling it keeps the revoke dialog open and revokes nothing.',
        'Then `onRevoke` is called with every item id; the dialog closes when it resolves.',
        'Cancel and Escape close it, except while it is pending.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Tab / Shift+Tab',
          action: 'Moves between the phrase field and the buttons; focus stays inside.',
        },
        { keys: 'Enter (in the phrase field)', action: 'Revokes, or reports a mismatch.' },
        { keys: 'Escape', action: 'Cancels, except while pending.' },
      ],
    },
    accessibility: {
      role: 'alertdialog',
      name: 'The title ("Revoke API key?"); the description is the accessible description.',
      items: [
        'Focus starts on Cancel.',
        'The items are a list named "To revoke"; "last used" is a `<time>` element with the exact date.',
        'Consequences are a list under "What happens", each with an icon and a spoken kind.',
      ],
    },
    responsive: {
      items: [
        'Follows ConfirmDialog: buttons fill the row on narrow screens and the body scrolls.',
        'Long key names and details wrap instead of scrolling sideways.',
      ],
    },
    theme: {
      items: [
        'Item list on the subtle border token, details in the muted text colour.',
        'Danger or warning icon and button from ConfirmDialog.',
        'In forced-colors mode list borders use system colours.',
      ],
    },
    examples: {
      items: [
        { title: 'API key with type-to-confirm and step-up', render: () => <RevokeKey /> },
        { title: 'Sign out several sessions', render: () => <SignOutSessions /> },
        { title: 'OAuth client from a danger zone', render: () => <FromDangerZone /> },
      ],
    },
    doDont: {
      do: [
        'Pass `lastUsed` so people can see what is still in use.',
        'Use `requireConfirmPhrase` and `stepUp` for production keys and clients.',
        'Return the request’s promise from `onRevoke`.',
      ],
      dont: [
        'Do not use it to pause access that should come back; use DeactivateFlow.',
        'Do not show secrets in `detail`; show a prefix or suffix only.',
      ],
    },
    related: {
      items: [
        { id: 'confirm-dialog', reason: 'The dialog it is built on.' },
        { id: 'step-up-auth-dialog', reason: 'The re-authentication step.' },
        { id: 'relative-time', reason: 'How "last used" is shown.' },
        { id: 'danger-zone', reason: 'Where the action usually starts.' },
        { id: 'remove-member-flow', reason: 'Removing a member from a workspace.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['relative-time', 'consequence-list', 'step-up-auth-dialog', 'confirm-dialog'],
    },
    testing: {
      unit: [
        'Shows the item, detail, last use and kind copy; danger tone; focus on Cancel; axe passes.',
        'Revokes every id and closes; sessions use the warning tone; null lastUsed reads "Never used".',
        'Requires the phrase when requireConfirmPhrase is set.',
        'A rejected onRevoke shows the error and stays open.',
        'Step-up: cancelling keeps the dialog without revoking; verifying revokes and closes.',
        'Copy per kind from messages.',
        'Renders on the server.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
