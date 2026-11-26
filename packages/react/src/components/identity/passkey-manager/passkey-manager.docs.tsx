import { useState } from 'react';
import { PasskeyManager, type Passkey } from '@rookcom/monarch-react/patterns';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const wait = (ms: number) => new Promise<void>((resolve) => window.setTimeout(resolve, ms));
const daysAgo = (n: number) => new Date(Date.now() - n * 86_400_000);

const SAMPLE: Passkey[] = [
  {
    id: 'pk-1',
    name: 'Work laptop',
    createdAt: daysAgo(60),
    lastUsedAt: daysAgo(0.1),
    synced: true,
  },
  { id: 'pk-2', name: 'Security key', createdAt: daysAgo(10) },
];

function Interactive() {
  const [passkeys, setPasskeys] = useState(SAMPLE);
  return (
    <PasskeyManager
      passkeys={passkeys}
      supported
      onAdd={async () => {
        // The product calls navigator.credentials.create() and its server here.
        await wait(900);
        setPasskeys((all) => [
          ...all,
          { id: `pk-${all.length + 1}`, name: 'New passkey', createdAt: new Date() },
        ]);
      }}
      onRename={async (id, name) => {
        await wait(400);
        setPasskeys((all) => all.map((p) => (p.id === id ? { ...p, name } : p)));
      }}
      onRemove={async (id) => {
        await wait(500);
        setPasskeys((all) => all.filter((p) => p.id !== id));
      }}
    />
  );
}

export default {
  id: 'passkey-manager',
  title: 'PasskeyManager',
  summary: 'List, add, rename and remove passkeys; WebAuthn stays in the product.',
  sections: {
    purpose: {
      summary:
        'Passkeys replace passwords with the device’s screen lock, and people need one place to see which ones they have, name them so they can tell them apart, add another, and remove one they lost. PasskeyManager does that, detects whether the browser can create passkeys, and treats a cancelled browser prompt as a normal outcome rather than an error. The WebAuthn ceremony itself stays in the product, through `onAdd`. Needed by: Rook ID security settings (`app/security/page.tsx`) once passkeys ship; no Rook app has passkeys yet.',
      useWhen: [
        'An account security page offering passkey sign-in.',
        'Right after sign-up, to offer adding a first passkey.',
      ],
      avoidWhen: [
        'Signing in with a passkey: use SignInForm’s passkey button.',
        'Other second factors (authenticator apps, codes): use TwoFactorSetup and RecoveryCodes.',
      ],
    },
    designPrinciples: {
      items: [
        'The product owns cryptography: the component never calls `navigator.credentials`.',
        'Cancelling the browser prompt is not a failure; it gets a calm, neutral note.',
        'Removing the last way to sign in is never silent: the confirmation says so.',
        'Feature detection happens after mount, so server and client render the same markup.',
      ],
    },
    apiReference: { source: 'typescript' },
    typedProperties: {
      source: 'typescript',
      notes: [
        '`passkeys` is `{ id, name, createdAt?, lastUsedAt?, synced?, detail? }[]`.',
        '`onAdd` runs `navigator.credentials.create()` and the server round trip; reject with the DOMException so cancels are recognised.',
        'No Rook app has its own copy of this list, so there is nothing to migrate.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'state',
          values: ['ready', 'loading', 'error'],
          description: 'What the card body shows. An empty list shows the empty state.',
        },
      ],
      notes: ['Add, rename and remove appear only when their callbacks are given.'],
    },
    states: {
      items: [
        {
          name: 'Support',
          attribute: 'data-supported',
          description:
            '`true` or `false` once detected (or from `supported`); `false` shows a notice and disables Add.',
        },
        {
          name: 'Adding',
          attribute: 'aria-busy (Add)',
          description: 'The add promise is running.',
        },
        {
          name: 'Cancelled',
          attribute: 'role="status"',
          description:
            'The person dismissed the browser prompt; a neutral note says nothing changed.',
        },
        {
          name: 'Error',
          attribute: 'role="alert"',
          description: 'Adding failed for another reason; its message shows.',
        },
        {
          name: 'Card state',
          attribute: 'data-state',
          description: '`ready`, `loading`, `empty` or `error`.',
        },
      ],
    },
    interaction: {
      items: [
        'Add a passkey calls `onAdd`; success is announced.',
        'Clicking a name edits it in place; Enter saves through `onRename`, Escape cancels.',
        'Remove opens a danger confirmation; with `lastSignInMethod` and one passkey left it adds a warning.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Moves between Add, each name and each Remove button.' },
        { keys: 'Enter (on a name)', action: 'Starts renaming.' },
        { keys: 'Enter / Escape (renaming)', action: 'Saves or cancels the new name.' },
        { keys: 'Escape (confirmation)', action: 'Closes without removing.' },
      ],
    },
    accessibility: {
      role: 'region (section) containing a list',
      name: 'The card title; the list is named "Your passkeys".',
      items: [
        'Each Remove button and rename field is named with the passkey.',
        'Results are announced politely; failures as alerts.',
        'The confirmation is an alertdialog that starts on Cancel.',
      ],
    },
    responsive: {
      items: [
        'Below 480px Remove moves under the details and fills the row.',
        'Long names wrap; buttons are at least 44px tall on touch screens.',
      ],
    },
    theme: {
      items: [
        'Rows on the subtle background with a key icon tile.',
        'Synced passkeys get an info badge.',
        'Forced-colors mode gives rows and icons system-colour borders.',
      ],
    },
    examples: {
      items: [
        { title: 'Add, rename and remove', stack: true, render: () => <Interactive /> },
        {
          title: 'Browser without passkey support',
          stack: true,
          render: () => (
            <PasskeyManager
              title="Your passkeys"
              passkeys={SAMPLE}
              supported={false}
              onAdd={() => undefined}
            />
          ),
        },
        {
          title: 'No passkeys yet',
          stack: true,
          render: () => (
            <PasskeyManager
              title="Sign-in passkeys"
              passkeys={[]}
              supported
              onAdd={() => wait(500)}
            />
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Suggest a name from the device or provider when a passkey is added; people can rename it.',
        'Set `lastSignInMethod` when the account has no password.',
        'Reject `onAdd` with the original DOMException so a cancel stays calm.',
      ],
      dont: [
        'Do not show credential ids or public keys.',
        'Do not hide the list in browsers without support; people may still want to remove a passkey.',
      ],
    },
    related: {
      items: [
        { id: 'recovery-codes', reason: 'Backup codes for when a device is lost.' },
        { id: 'two-factor-setup', reason: 'Authenticator app setup.' },
        { id: 'sign-in-form', reason: 'Signing in with a passkey.' },
        { id: 'session-list', reason: 'Where the person is signed in.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: [
        'button',
        'badge',
        'relative-time',
        'inline-notice',
        'inline-edit-text',
        'section-card',
        'confirm-dialog',
      ],
    },
    testing: {
      unit: [
        'Lists passkeys with times and a synced badge; axe passes.',
        'Detects missing support after mount and disables Add.',
        'Detects support from window.PublicKeyCredential.',
        'Adds with pending state; a cancelled prompt is neutral; other failures alert.',
        'Renames in place with the keyboard.',
        'Confirms before removing and warns for the last sign-in method.',
        'Empty state and translated messages.',
        'Renders on the server without touching window.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
