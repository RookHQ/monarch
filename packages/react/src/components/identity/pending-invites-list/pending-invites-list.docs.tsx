import { useState } from 'react';
import { PendingInvitesList, type PendingInvite } from '@rookcom/monarch-react/patterns';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const wait = (ms: number) => new Promise<void>((resolve) => window.setTimeout(resolve, ms));
const days = (n: number) => new Date(Date.now() + n * 86_400_000);

const SAMPLE: PendingInvite[] = [
  {
    id: 'inv-1',
    email: 'ana@example.com',
    role: 'Member',
    invitedBy: 'Sam Rivera',
    sentAt: days(-2),
    expiresAt: days(5),
  },
  {
    id: 'inv-2',
    email: 'bo@example.com',
    role: 'Admin',
    invitedBy: 'Sam Rivera',
    sentAt: days(-9),
    expiresAt: days(-2),
  },
];

function Interactive() {
  const [invites, setInvites] = useState(SAMPLE);
  return (
    <PendingInvitesList
      invites={invites}
      onResend={() => wait(700)}
      onRevoke={async (id) => {
        await wait(600);
        setInvites((all) => all.filter((i) => i.id !== id));
      }}
      getInviteLink={(id) => `https://example.com/join/${id}`}
    />
  );
}

export default {
  id: 'pending-invites-list',
  title: 'PendingInvitesList',
  summary: 'Invitations not yet accepted, with Resend, Copy link and Revoke.',
  sections: {
    purpose: {
      summary:
        'After inviting people, admins need to see who has not joined yet, nudge them, share the link another way, or take the invitation back. PendingInvitesList shows each invitation with its role, sender, when it was sent and when it expires, and offers Resend (with a pending and “Sent” state), Copy link, and Revoke behind a confirmation. Needed by: Rook ID (`app/workspaces/[id]/members/page.tsx`, pending invites section) and the members pages of Rook Stride, Rook Base and Rook Tickets.',
      useWhen: [
        'A members or team page, below the member list.',
        'An admin view of outstanding invitations.',
      ],
      avoidWhen: [
        'People who already joined: use MemberList.',
        'The invitee’s side (accepting an invitation): that is a page, not a list.',
      ],
    },
    designPrinciples: {
      items: [
        'Expired invitations stay visible and say so, with Resend as the way forward.',
        'Revoking is destructive for the invitee’s link, so it is confirmed with useConfirm.',
        'Resend gives feedback on the row it was pressed in, not a global toast.',
        'Data and callbacks in: the component never fetches or decides expiry rules beyond the dates given.',
      ],
    },
    apiReference: { source: 'typescript' },
    typedProperties: {
      source: 'typescript',
      notes: [
        '`invites` is `{ id, email, role?, invitedBy?, sentAt?, expiresAt?, expired?, detail? }[]`.',
        'Migrating from the app copy (Rook ID members page): the pending invites map with `formatJoinDate(invite.expires_at)`, the role badge and the `handleRevokeInvite` button with `revokingId` becomes `<PendingInvitesList invites onRevoke />`; `revokingId` is no longer needed because a returned promise keeps the confirmation pending.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'state',
          values: ['ready', 'loading', 'error'],
          description: 'What the card body shows. An empty `invites` list shows the empty state.',
        },
      ],
      notes: [
        'Actions appear only for the callbacks given: `onResend`, `onRevoke`, `getInviteLink`.',
      ],
    },
    states: {
      items: [
        {
          name: 'Card state',
          attribute: 'data-state',
          description: '`ready`, `loading`, `empty` or `error`, on the root.',
        },
        {
          name: 'Expired',
          attribute: 'data-expired',
          description:
            'The invitation is past `expiresAt`; an “Expired” badge shows and Copy link is hidden.',
        },
        {
          name: 'Resending',
          attribute: 'aria-busy',
          description: 'The row’s Resend shows a spinner until the promise settles.',
        },
        {
          name: 'Sent',
          attribute: 'data-sent',
          description: 'Resend reads “Sent” for three seconds after success.',
        },
        {
          name: 'Row error',
          attribute: 'role="alert"',
          description: 'A failed resend shows its message on the row.',
        },
      ],
    },
    interaction: {
      items: [
        'Resend calls `onResend(id)`; success announces it and briefly shows “Sent”.',
        'Copy link copies the value from `getInviteLink(id)`.',
        'Revoke opens a danger confirmation; confirming calls `onRevoke(id)` and announces the result.',
        'Focus returns to the Revoke button after the confirmation closes.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab / Shift+Tab', action: 'Moves between the row actions.' },
        { keys: 'Enter / Space', action: 'Activates the focused action.' },
        { keys: 'Escape', action: 'Closes the revoke confirmation without revoking.' },
      ],
    },
    accessibility: {
      role: 'region (section) containing a list',
      name: 'The card title; the list is named "Pending invitations".',
      items: [
        'Each action is named with the address ("Revoke invitation to ana@example.com").',
        'Times are `<time>` elements with the exact date in `datetime`.',
        'Results are announced through a polite status region.',
      ],
    },
    responsive: {
      items: [
        'Below 480px the actions move under the details.',
        'Long addresses wrap anywhere.',
        'Actions are at least 44px tall on touch screens.',
      ],
    },
    theme: {
      items: [
        'Rows on the subtle background with a subtle border.',
        'The role badge is outlined; the Expired badge uses the warning tone.',
        'Revoke uses the danger-subtle button.',
      ],
    },
    examples: {
      items: [
        { title: 'Resend, copy and revoke', stack: true, render: () => <Interactive /> },
        {
          title: 'Empty',
          stack: true,
          render: () => <PendingInvitesList title="Invitations" invites={[]} />,
        },
      ],
    },
    doDont: {
      do: [
        'Pass `expiresAt` so people can see which invitations need a resend.',
        'Return the request’s promise from `onRevoke` so failures show in the confirmation.',
      ],
      dont: [
        'Do not revoke without confirmation; the invitee’s email link stops working.',
        'Do not show the invitation token itself; offer Copy link instead.',
      ],
    },
    related: {
      items: [
        { id: 'invite-members-dialog', reason: 'Where invitations are sent.' },
        { id: 'member-row', reason: 'People who have joined.' },
        { id: 'confirm-dialog', reason: 'The revoke confirmation (useConfirm).' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: [
        'button',
        'copy-button',
        'badge',
        'relative-time',
        'section-card',
        'confirm-dialog',
      ],
    },
    testing: {
      unit: [
        'Lists invitations with role, sender, times and expiry; axe passes.',
        'Resends with a pending state and announces it.',
        'Shows a resend failure on the row.',
        'Confirms before revoking from the keyboard; Escape cancels and focus returns.',
        'Copies the link for live invitations only.',
        'Loading, empty, error states and translated messages.',
        'Renders on the server.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
