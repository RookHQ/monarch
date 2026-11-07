import { useState } from 'react';
import { Button } from '@rookcom/monarch-react';
import {
  ShareDialog,
  type ShareLink,
  type SharePerson,
} from '@rookcom/monarch-react/collaboration';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const ROLES = [
  { value: 'viewer', label: 'Viewer' },
  { value: 'commenter', label: 'Commenter' },
  { value: 'editor', label: 'Editor' },
];

const ACCESS = [
  {
    value: 'invited',
    label: 'Only people invited',
    description: 'Only the people listed above can open it.',
  },
  {
    value: 'workspace',
    label: 'Anyone in the workspace',
    description: 'Everyone in the workspace can find and open it.',
  },
];

const CANDIDATES = [
  { value: 'u4', label: 'Jordan Kim', description: 'jordan@example.com' },
  { value: 'u5', label: 'Riley Chen', description: 'riley@example.com' },
  { value: 'u6', label: 'Taylor Brooks', description: 'taylor@example.com' },
];

const wait = () => new Promise((resolve) => setTimeout(resolve, 600));

function PeopleDemo() {
  const [people, setPeople] = useState<SharePerson[]>([
    { id: 'u1', name: 'Alex Morgan', email: 'alex@example.com', role: 'editor', isSelf: true },
    { id: 'u2', name: 'Sam Lee', email: 'sam@example.com', role: 'commenter' },
    { id: 'u3', name: 'Jamie Fox', email: 'jamie@example.com', role: 'viewer', status: 'invited' },
  ]);
  const [access, setAccess] = useState('invited');
  return (
    <ShareDialog
      itemName="Quarterly plan"
      description="People you invite get the role you choose."
      trigger={<Button>Share</Button>}
      roles={ROLES}
      defaultInviteRole="viewer"
      inviteOptions={CANDIDATES}
      onInvite={async (values, role) => {
        await wait();
        setPeople((list) => [
          ...list,
          ...values.map((value) => {
            const known = CANDIDATES.find((c) => c.value === value);
            return {
              id: value,
              name: known?.label ?? value,
              email: known?.description ?? value,
              role,
              status: 'invited' as const,
            };
          }),
        ]);
      }}
      people={people}
      onRoleChange={async (id, role) => {
        await wait();
        setPeople((list) => list.map((p) => (p.id === id ? { ...p, role } : p)));
      }}
      onRemovePerson={async (id) => {
        await wait();
        setPeople((list) => list.filter((p) => p.id !== id));
      }}
      access={access}
      accessOptions={ACCESS}
      onAccessChange={async (value) => {
        await wait();
        setAccess(value);
      }}
      link="https://example.com/d/quarterly-plan"
    />
  );
}

function LinksDemo() {
  const [links, setLinks] = useState<ShareLink[]>([
    {
      id: 'l1',
      url: 'https://example.com/s/k3f9a',
      label: '/s/k3f9a',
      views: 12,
      expiresAt: new Date(2026, 11, 31),
    },
    {
      id: 'l2',
      url: 'https://example.com/s/p0q2z',
      label: '/s/p0q2z',
      views: 4,
      hasPassword: true,
    },
    {
      id: 'l3',
      url: 'https://example.com/s/x7y1m',
      label: '/s/x7y1m',
      views: 30,
      status: 'expired',
    },
  ]);
  return (
    <ShareDialog
      itemName="Release notes"
      description="Anyone with a link can read this page. They cannot edit it."
      trigger={<Button variant="secondary">Share links</Button>}
      links={links}
      onCreateLink={async ({ password }) => {
        await wait();
        const id = Math.random().toString(36).slice(2, 7);
        const link: ShareLink = {
          id,
          url: `https://example.com/s/${id}`,
          label: `/s/${id}`,
          views: 0,
          hasPassword: Boolean(password),
        };
        setLinks((list) => [link, ...list]);
        return link;
      }}
      onRevokeLink={async (id) => {
        await wait();
        setLinks((list) => list.map((l) => (l.id === id ? { ...l, status: 'revoked' } : l)));
      }}
    />
  );
}

export default {
  id: 'share-dialog',
  title: 'ShareDialog',
  summary: 'Invite people, set roles and general access, copy the link and manage share links.',
  sections: {
    purpose: {
      summary:
        'One dialog for sharing anything: invite people with a role, change or remove who has access, choose who else can open it, copy the link, and create or revoke share links with an optional password and expiry. Each section appears only when its data and callback are given, so the same dialog fits a document, a board or a folder. Needed by: Dossier (document ShareModal); also fits sharing in Stride, Calendar and Links.',
      useWhen: [
        'People share an item with teammates or outside readers from a Share button.',
        'An item has public or guest links that need passwords, expiry or revoking.',
      ],
      avoidWhen: [
        'Managing all members of a workspace: use MemberRow in a settings page.',
        'Copying a single link with nothing to configure: use CopyField or CopyButton.',
      ],
    },
    designPrinciples: {
      items: [
        'Show only what applies: sections render from the props given, never empty shells.',
        'Every change goes through a callback that may return a promise; controls show busy states and errors stay next to the control that failed.',
        'Removing access and revoking a link are confirmed first, because they take effect at once.',
        'Creating a link and copying it are one intent: when `onCreateLink` resolves with the link, it is copied.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Sections: invite (`onInvite`), people (`people`), general access (`access` + `accessOptions`), link (`link`), share links (`links` and/or `onCreateLink`). `children` adds product-specific settings at the end.',
        'Opening: pass `trigger`, or control it with `open` and `onOpenChange`.',
        '`SHARE_DIALOG_MESSAGES` and `SHARE_EXPIRY_OPTIONS` hold the English defaults.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Migrating from the app copy (Dossier `document/ShareModal`): `documentTitle` → `itemName`; `open` and `onOpenChange` are unchanged. The data hooks stay in the product: pass `useShareLinks` data as `links` (map `token` to `url` with the product’s `shareUrl`, `isActive`/`revokedAt` to `status`, `viewCount` to `views`), `createLink.mutate` as `onCreateLink` (it receives `{ password, expiresInDays }`; turn the days into `expiresAt` there) and `revokeLink.mutate` as `onRevokeLink`. `EXPIRY_PRESETS` → `expiryOptions`. The "Internal access" card (lock, specific members) maps to `access`/`accessOptions` plus `people`, or goes in `children` if it must stay as is. Toasts are no longer needed: results are announced in the dialog.',
      ],
    },
    variants: {
      items: [],
      notes: [
        'No visual variants; the sections shown depend on the props. On phones the dialog fills the screen.',
      ],
    },
    states: {
      items: [
        {
          name: 'Busy',
          description:
            'A button or select shows a spinner or `aria-busy` while its callback’s promise runs.',
        },
        {
          name: 'Error',
          description:
            'A rejected callback shows its message next to the control; selects go back to the previous value.',
        },
        {
          name: 'Password too short',
          description: 'An inline error under the password field; Create link is disabled.',
        },
        {
          name: 'Link status',
          attribute: 'data-status on the link row',
          description: '`active`, `expired` or `revoked`; only active links have Copy and Revoke.',
        },
        {
          name: 'Loading links',
          description: '`linksLoading` shows a loading line in place of the list.',
        },
      ],
    },
    interaction: {
      items: [
        'Type a name to pick suggestions, or a full email address and pick "Invite …"; several can be picked before pressing Invite.',
        'Role selects save as soon as they change; Remove and Revoke ask for confirmation.',
        'Changing general access saves at once and announces the new setting.',
        'Creating a link clears the password, announces it and copies the link when the product returns it.',
        'Outside clicks do not close the dialog, so typed invites are not lost; Escape and the close button do.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Tab / Shift+Tab',
          action: 'Moves through the controls; focus stays in the dialog.',
        },
        { keys: 'Arrow Down / Up, Enter', action: 'Moves and picks in the invite suggestions.' },
        { keys: 'Enter', action: 'Sends the invite or creates the link from inside its form.' },
        {
          keys: 'Escape',
          action: 'Closes the suggestions first, then the dialog; focus returns to the trigger.',
        },
      ],
    },
    accessibility: {
      role: 'dialog',
      name: '"Share “item name”" (messages.title)',
      items: [
        'Each section is a region named by its heading; people and links are lists.',
        'Icon-only Copy and the Revoke buttons are named after the link; role selects and Remove buttons after the person.',
        'Invites, access changes and new links are announced through a polite status.',
        'Errors are tied to their field with FieldError, or announced as alerts.',
      ],
    },
    responsive: {
      items: [
        'Below 640px the dialog fills the screen; the invite row wraps so the field gets the full width.',
        'Password and expiry sit side by side only when there is room.',
        'Long link URLs are truncated with an ellipsis; the full URL is what is copied.',
        'Buttons and selects follow Button and NativeSelect, which are 44px tall on coarse pointers.',
      ],
    },
    theme: {
      items: [
        'Built from Dialog, Combobox, NativeSelect, MemberRow, CopyField and Button, so it follows their tokens.',
        'Create link panel and link rows: `--mn-color-bg-subtle`, `--mn-color-border-subtle`; revoked and expired links use `--mn-color-text-muted`.',
        'Revoke uses `--mn-color-danger-strong`; errors use the field error colour.',
        'In forced colours the panels get CanvasText borders and inactive links use GrayText.',
      ],
    },
    examples: {
      items: [
        { title: 'Invite people and set access', render: () => <PeopleDemo /> },
        { title: 'Share links with password and expiry', render: () => <LinksDemo /> },
      ],
    },
    doDont: {
      do: [
        'Use role names people already know from the product, and keep them to three or four.',
        'Describe each general access choice in plain words.',
        'Return the new link from `onCreateLink` so it is copied straight away.',
      ],
      dont: [
        'Do not show a general access choice the person cannot change; leave `onAccessChange` out and it is read-only.',
        'Do not let people remove themselves or the owner: mark them `isSelf` or `removeLocked`.',
        'Do not toast results the dialog already announces.',
      ],
    },
    related: {
      items: [
        { id: 'member-row', reason: 'The row used for each person with access.' },
        { id: 'combobox', reason: 'The invite field.' },
        { id: 'copy-field', reason: 'The link field.' },
        { id: 'confirm-dialog', reason: 'Confirms removing people and revoking links.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: [
        'button',
        'copy-button',
        'copy-field',
        'combobox',
        'form-field',
        'text-field',
        'member-row',
        'confirm-dialog',
        'dialog',
      ],
    },
    testing: {
      unit: [
        'Opening from the trigger, the dialog name and description, focus returning on Escape.',
        'Inviting suggestions and typed emails with a role; announced on success, picks kept on failure.',
        'People list: locked self and owner, role change, confirmed removal.',
        'General access change, announcement and restore on failure.',
        'Creating a link with password validation and expiry; link details; confirmed revoke.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
