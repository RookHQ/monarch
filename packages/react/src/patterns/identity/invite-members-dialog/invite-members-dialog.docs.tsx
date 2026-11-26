import { Button } from '@rookcom/monarch-react';
import {
  InviteMembersDialog,
  type AppAccessOption,
  type InviteMembersPayload,
  type RoleOption,
} from '@rookcom/monarch-react/patterns';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const wait = (ms: number) => new Promise<void>((resolve) => window.setTimeout(resolve, ms));

const ROLES: RoleOption[] = [
  { value: 'viewer', label: 'Viewer', description: 'Can see everything but change nothing.' },
  { value: 'member', label: 'Member', description: 'Can create and edit work.' },
  { value: 'admin', label: 'Admin', description: 'Can also manage members and settings.' },
];

const APPS: AppAccessOption[] = [
  { id: 'notes', name: 'Notes', description: 'Shared documents' },
  { id: 'tasks', name: 'Tasks', description: 'Projects and issues' },
  { id: 'calendar', name: 'Calendar', description: 'Shared calendars' },
];

async function sendInvites({ emails }: InviteMembersPayload) {
  await wait(800);
  // Sample rule for the demo: addresses at example.org are "already members".
  const failed = emails
    .filter((email) => email.endsWith('@example.org'))
    .map((email) => ({ email, message: 'Already a member of this workspace' }));
  return { failed };
}

export default {
  id: 'invite-members-dialog',
  title: 'InviteMembersDialog',
  summary: 'Invite people by email with a role, app access and an optional note.',
  sections: {
    purpose: {
      summary:
        'Inviting people is how workspaces grow, and every app re-built the same dialog: an address field, a role select, app access and a send button, usually with one toast for the whole batch. InviteMembersDialog takes many addresses at once (paste a list), explains each role, grants apps with AppAccessPicker, and when some addresses fail keeps exactly those with their reasons while the rest are cleared. Needed by: Rook ID (`app/workspaces/[id]/members/page.tsx`), and the team or members pages of Rook Stride, Rook Base, Rook Tickets, Rook People, Rook Calendar and Rook Links.',
      useWhen: [
        'Adding people to a workspace, team or project by email.',
        'Re-inviting a list of people with the same role.',
      ],
      avoidWhen: [
        'Adding people who are already members to a team: use an EntityPicker of members.',
        'Sharing one document: use ShareButton.',
      ],
    },
    designPrinciples: {
      items: [
        'Many addresses at once: pasting a spreadsheet column just works.',
        'Partial failure is normal: say who was invited, keep who was not, and say why.',
        'Explain roles in plain words right where they are chosen.',
        'Nothing is lost by a stray click: outside clicks do not close it and it stays open while sending.',
      ],
    },
    apiReference: { source: 'typescript' },
    typedProperties: {
      source: 'typescript',
      notes: [
        '`onInvite({ emails, role, apps, message? })` may resolve with `{ failed: [{ email, message }] }`.',
        '`roles` are RolePicker options; `apps` are AppAccessPicker options.',
        'Migrating from the app copy (Rook ID members page): the `Dialog` with an `Input` for “username or email”, a `Select` of roles and the access checklist becomes `<InviteMembersDialog roles apps onInvite />`. Pass `validateEmail={() => null}` (or a username check) to keep accepting usernames. The per-invite `toast.success` / `toast.error` calls become the returned `failed` list.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'apps',
          values: ['unset', 'AppAccessOption[]'],
          description: 'Shows the app access picker when given.',
        },
        {
          prop: 'allowMessage',
          values: ['false', 'true'],
          description: 'Shows the optional personal note.',
        },
        {
          prop: 'size',
          values: ['sm', 'md', 'lg', 'xl'],
          description: 'Dialog width. Default `md`.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Sending',
          attribute: 'aria-busy',
          description:
            'The submit button shows a spinner; fields, Cancel, Escape and the close button are disabled.',
        },
        {
          name: 'Invalid',
          attribute: 'aria-invalid',
          description: 'No address, an invalid address, or too many addresses.',
        },
        {
          name: 'Partial failure',
          attribute: 'role="alert"',
          description:
            'Lists the addresses that failed with their reasons; only those stay in the field.',
        },
        {
          name: 'Error',
          attribute: 'role="alert"',
          description: '`onInvite` rejected; its message shows above the buttons.',
        },
      ],
    },
    interaction: {
      items: [
        'Type an address and press Enter, comma or space to make it a token; paste a list to add many.',
        'Text left in the field when sending is added automatically.',
        'Sending calls `onInvite`; if everything succeeds the dialog closes and the result is announced.',
        'If some fail, the dialog stays open with only those addresses, and focus moves to the error list.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Enter / , / Space', action: 'Turns the typed address into a token.' },
        { keys: 'Backspace (empty field)', action: 'Selects and removes the last token.' },
        { keys: 'Tab', action: 'Moves through the field, role, apps, note and buttons.' },
        { keys: 'Enter (empty field)', action: 'Sends the invitations.' },
        { keys: 'Escape', action: 'Closes the dialog (not while sending).' },
      ],
    },
    accessibility: {
      role: 'dialog',
      name: 'The title, "Invite people".',
      items: [
        'Every control has a visible label; the address field’s help explains separators.',
        'Rejected addresses and the failure list are announced.',
        'Focus starts in the address field and returns to the trigger on close.',
      ],
    },
    responsive: {
      items: [
        'The dialog fits 390px screens; tokens and long addresses wrap.',
        'Buttons and options are at least 44px tall on touch screens (Button and Select).',
      ],
    },
    theme: {
      items: [
        'Dialog surface and controls from Dialog, TagInput, RolePicker and AppAccessPicker.',
        'The failure list uses the danger subtle background and border.',
        'Forced-colors mode gives the failure list a system-colour border.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Invite with role, apps and a note',
          render: () => (
            <InviteMembersDialog
              trigger={<Button variant="primary">Invite people</Button>}
              roles={ROLES}
              defaultRole="member"
              apps={APPS}
              allowMessage
              onInvite={sendInvites}
            />
          ),
        },
        {
          title: 'Addresses only',
          render: () => (
            <InviteMembersDialog
              trigger={<Button>Invite</Button>}
              roles={ROLES}
              onInvite={() => wait(600)}
            />
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Return per-address failures instead of failing the whole batch.',
        'Default to the least-access role that makes sense.',
        'Tell people in the description how invitations work (email, expiry).',
      ],
      dont: [
        'Do not show a toast for each address.',
        'Do not offer roles the current person cannot assign; disable them with a reason.',
      ],
    },
    related: {
      items: [
        { id: 'role-picker', reason: 'The role choice inside.' },
        { id: 'app-access-picker', reason: 'The app access inside.' },
        { id: 'pending-invites-list', reason: 'Where sent invitations are listed.' },
        { id: 'tag-input', reason: 'The address field.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: [
        'button',
        'alert',
        'form-field',
        'tag-input',
        'text-field',
        'app-access-picker',
        'role-picker',
        'dialog',
      ],
    },
    testing: {
      unit: [
        'Opens from its trigger with labelled fields, invites everyone, closes, announces and returns focus; axe passes.',
        'Splits a pasted list, rejects invalid addresses and requires at least one.',
        'Keeps failed addresses with their reasons and clears the rest.',
        'Pending state, rejection message and role change.',
        'Custom validator for usernames and translated messages.',
        'Renders on the server.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
