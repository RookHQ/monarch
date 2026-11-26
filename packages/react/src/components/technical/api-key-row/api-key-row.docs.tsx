import { useState } from 'react';
import { ApiKeyRow, KeyCreatedDialog } from '@rookcom/monarch-react/patterns';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const NOW = '2026-06-10T12:00:00Z';
const wait = () => new Promise((resolve) => setTimeout(resolve, 600));

function KeyList() {
  const [revoked, setRevoked] = useState<string[]>([]);
  const [rotated, setRotated] = useState(false);
  return (
    <>
      <ul style={{ margin: 0, padding: 0 }}>
        <ApiKeyRow
          as="li"
          name="Reporting job"
          prefix="sk_live_4f2a"
          createdAt="2026-05-02T09:00:00Z"
          lastUsedAt="2026-06-10T10:30:00Z"
          scopes={['items:read', 'reports:read']}
          now={NOW}
          status={revoked.includes('a') ? 'revoked' : 'active'}
          onRotate={async () => {
            await wait();
            setRotated(true);
          }}
          onRevoke={async () => {
            await wait();
            setRevoked((r) => [...r, 'a']);
          }}
        />
        <ApiKeyRow
          as="li"
          name="Import script"
          prefix="sk_test_91bd"
          createdAt="2026-06-01T09:00:00Z"
          lastUsedAt={null}
          expiresAt="2026-06-14T09:00:00Z"
          status={revoked.includes('b') ? 'revoked' : 'expiring'}
          scopes={['items:read', 'items:write', 'users:read', 'users:write', 'webhooks:write']}
          now={NOW}
          revokeConfirmPhrase="Import script"
          onRevoke={async () => {
            await wait();
            setRevoked((r) => [...r, 'b']);
          }}
        />
      </ul>
      <KeyCreatedDialog
        open={rotated}
        onOpenChange={setRotated}
        name="Reporting job"
        secret="sk_live_8c31d0f7a29e4b56"
      />
    </>
  );
}

export default {
  id: 'api-key-row',
  title: 'ApiKeyRow',
  summary:
    'One API key: name, status, masked prefix, dates, scopes, and confirmed Rotate and Revoke.',
  sections: {
    purpose: {
      summary:
        'Shows one API key in a list without exposing the secret, and gives Rotate and Revoke actions that always ask first, through ConfirmDialog. Needed by: Developers (credentials list and key details, today `CredentialsTable`, `RotateKeyDialog` and `RevokeKeyDialog`) and Dots (lead settings `ApiKeysTab`).',
      useWhen: [
        'A settings or developer page lists API keys, tokens or other credentials the person can rotate or revoke.',
        'You need the same confirmation and pending behaviour for rotate and revoke everywhere.',
      ],
      avoidWhen: [
        'Showing a newly created secret: use KeyCreatedDialog.',
        'A sortable, filterable table of many keys: use DataTable with these fields as columns.',
        'Editing a stored secret in a form: use SecretField.',
      ],
    },
    designPrinciples: {
      items: [
        'Never the secret: only the non-secret prefix is shown, followed by a mask.',
        'Ask before breaking things: Rotate uses a warning dialog and Revoke a danger dialog with consequences and an optional type-to-confirm phrase.',
        'Async-honest: return a promise from `onRotate` / `onRevoke` and the dialog stays pending; a rejection is shown inside it so the person can retry.',
        'Status is text and colour: the badge always says Active, Expiring soon, Expired or Revoked.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Uses `useConfirm()` from ConfirmDialog internally; focus returns to the Rotate or Revoke button when the dialog closes.',
        'Forwards its ref to the root (`div`, or `li` with `as="li"`).',
        'Dates use RelativeTime without its tooltip, so the row has no extra tab stops.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Pass `now` with a server timestamp for identical server and client output.',
        '`rotateConfirmOptions` and `revokeConfirmOptions` override any ConfirmDialog option, such as `consequences` or `reason`.',
        'After a rotate, open KeyCreatedDialog with the new secret; this row does not show secrets.',
        'Migrating from the Developers app: `CredentialsTable` columns → `name`, `prefix`, `createdAt`, `lastUsedAt` (null → "Never used"), `expiresAt`, `status` (`KeyStatusBadge` values map one to one); `RotateKeyDialog` → `onRotate`, `RevokeKeyDialog` → `onRevoke` with `revokeConfirmPhrase`. Dots `ApiKeysTab` maps `revokedAt` to `status="revoked"`.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'status',
          values: ['active', 'expiring', 'expired', 'revoked'],
          description:
            'Badge text and tone. `expired` and `revoked` dim the row and hide the actions.',
        },
        {
          prop: 'as',
          values: ['div', 'li'],
          description: 'Use `li` inside a `<ul>` of keys.',
        },
      ],
    },
    states: {
      items: [
        { name: 'Status', attribute: 'data-status', description: 'The key lifecycle.' },
        {
          name: 'Confirming',
          attribute: 'role="alertdialog" (ConfirmDialog)',
          description: 'Rotate or Revoke is waiting for the person to confirm.',
        },
        {
          name: 'Pending / error',
          attribute: 'from ConfirmDialog',
          description: 'The action is running, or failed with its message shown in the dialog.',
        },
        {
          name: 'Never used',
          description: 'No `lastUsedAt`: "Never used" instead of a time.',
        },
      ],
    },
    interaction: {
      items: [
        'Rotate opens a warning confirmation; confirming calls `onRotate`.',
        'Revoke opens a danger confirmation with consequences, and a phrase field when `revokeConfirmPhrase` is set; confirming calls `onRevoke`.',
        'Cancel, Escape or the close button dismiss without calling anything.',
        'Scopes past `maxScopes` collapse into a "+N" badge whose title and hidden text list them.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Moves to Rotate, Revoke and any extra actions.' },
        { keys: 'Enter / Space', action: 'Opens the confirmation.' },
        { keys: 'Escape', action: 'Cancels the confirmation (not while the action is running).' },
      ],
      notes: ['In the dialog, focus starts on Cancel, the least destructive choice.'],
    },
    accessibility: {
      role: 'none on a div root, listitem with `as="li"`; actions are a named group',
      name: 'Labelled by the key name. The prefix is named "Key starting with …" and the mask is hidden from assistive tech.',
      items: [
        'Dates are `<time>` elements in a description list (Created, Last used, Expires).',
        'Scopes are a named list; hidden scopes are read out in full.',
        'Confirmations are `alertdialog`s with a title naming the key.',
      ],
    },
    responsive: {
      items: [
        'Name, scopes and actions wrap onto separate lines as the row narrows; at 390px the actions take their own line.',
        'Long names and prefixes break instead of overflowing.',
        'Buttons keep 44px targets on coarse pointers.',
      ],
    },
    theme: {
      items: [
        'Uses `--mn-color-border-subtle`, `--mn-color-text-muted`, `--mn-font-mono` and Badge tones (success, warning, neutral, danger).',
        'Forced colours draw the divider in CanvasText and the mask in GrayText.',
        'Logical properties, so actions sit on the left in RTL.',
      ],
    },
    examples: {
      items: [{ title: 'Key list with rotate and revoke', stack: true, render: () => <KeyList /> }],
    },
    doDont: {
      do: [
        'Return a promise from `onRotate` and `onRevoke` so failures show in the dialog.',
        'Set `revokeConfirmPhrase` for production keys.',
        'Show the new secret with KeyCreatedDialog after rotating.',
      ],
      dont: [
        'Do not pass the full secret as `prefix`.',
        'Do not add your own confirm on top: the row already asks.',
      ],
    },
    related: {
      items: [
        { id: 'key-created-dialog', reason: 'Show a new or rotated secret once.' },
        { id: 'scope-selector', reason: 'Choose the scopes of a key.' },
        { id: 'secret-field', reason: 'Edit a stored secret in a form.' },
        { id: 'confirm-dialog', reason: 'The confirmation it uses.' },
        { id: 'data-table', reason: 'Many keys with sorting and filters.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['button', 'badge', 'relative-time', 'confirm-dialog'],
    },
    testing: {
      unit: [
        'Name, status, masked prefix, relative dates and collapsed scopes, with accessible names; axe passes.',
        '"Never used", "No scopes" and the expiry line.',
        'Rotate asks first with the keyboard, Escape cancels, focus returns, and confirming calls `onRotate`.',
        'Revoke needs the phrase, shows a rejection inside the dialog, and retries.',
        'Revoked keys hide actions; `as="li"` and translated messages.',
        'Renders on the server.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
