import { useState } from 'react';
import { Badge } from '@rookcom/monarch-react';
import { MemberList, MemberRow } from '@rookcom/monarch-react/patterns';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const ROLES = [
  { value: 'owner', label: 'Owner' },
  { value: 'admin', label: 'Admin' },
  { value: 'member', label: 'Member' },
];

const wait = () => new Promise((r) => setTimeout(r, 700));

function TeamDemo() {
  const [people, setPeople] = useState([
    { id: '1', name: 'Priya Raman', email: 'priya@example.com', role: 'owner', self: true },
    { id: '2', name: 'Jonas Weber', email: 'jonas@example.com', role: 'admin' },
    { id: '3', name: 'Amara Okafor', email: 'amara@example.com', role: 'member' },
  ]);
  return (
    <MemberList aria-label="Workspace members" style={{ width: '100%' }}>
      {people.map((p) => (
        <MemberRow
          key={p.id}
          name={p.name}
          email={p.email}
          isSelf={p.self ?? false}
          roleOptions={ROLES}
          memberRole={p.role}
          onMemberRoleChange={async (role) => {
            await wait();
            setPeople((all) => all.map((x) => (x.id === p.id ? { ...x, role } : x)));
          }}
          onRemove={async () => {
            await wait();
            setPeople((all) => all.filter((x) => x.id !== p.id));
          }}
        />
      ))}
    </MemberList>
  );
}

export default {
  id: 'member-row',
  title: 'MemberRow',
  summary:
    'One member of a workspace or space: avatar, name, email, role, status (invited, active, suspended) and confirmed actions.',
  sections: {
    purpose: {
      summary:
        'The same member row everywhere people are listed with a role: workspace and space members, channel members, project and team members. The product passes the person, the roles allowed and callbacks; who may change what stays in the product. Import from `@rookcom/monarch-react/patterns`.',
      useWhen: [
        'A list of people with a role that some viewers may change.',
        'Pending invitations shown alongside members, with resend and revoke.',
      ],
      avoidWhen: [
        'Choosing people to add: use EntityPicker or AssigneePicker.',
        'Dense tables with many columns per person: use DataTable with Avatar cells.',
      ],
    },
    designPrinciples: {
      items: [
        'Identity first: avatar, name and email read as one unit; everything else is secondary.',
        'Only what differs is marked: invited and suspended get a badge, active members do not, so a long list stays quiet.',
        'Every control is named after the person (“Role for Jonas Weber”, “Remove Jonas Weber”), so a list of identical buttons is still clear to screen-reader users.',
        'Destructive actions are confirmed in a ConfirmDialog that can show what will happen.',
        'Async by default: role changes and resends show a busy state; failures are explained in the row and roll back.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'MemberRow renders an `<li>` (ref) by default, or a `<div>` with `as="div"`; MemberList is the `<ul>` that holds rows.',
        'The member’s role is `memberRole` / `defaultMemberRole` / `onMemberRoleChange` (not the ARIA `role`), chosen from `roleOptions`.',
        '`onMemberRoleChange`, `onResend` and `onRemove` may return promises; a rejected role change restores the previous role.',
        'Data attributes: `data-status` (`invited`, `active`, `suspended`) and `data-self`.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [
        {
          prop: 'status',
          values: ['active', 'invited', 'suspended'],
          description:
            '`invited` shows an Invited badge, Resend and Revoke; `suspended` a Suspended badge and a muted name.',
        },
        {
          prop: 'as',
          values: ['li', 'div'],
          description:
            '`li` (default) inside MemberList; `div` for a single row in a card or popover.',
        },
      ],
    },
    states: {
      items: [
        { name: 'Active', attribute: 'data-status="active"', description: 'No status badge.' },
        {
          name: 'Invited',
          attribute: 'data-status="invited"',
          description: 'Info badge; Resend invite; Remove becomes Revoke invite.',
        },
        {
          name: 'Suspended',
          attribute: 'data-status="suspended"',
          description: 'Warning badge and a muted name.',
        },
        {
          name: 'Self',
          attribute: 'data-self',
          description: '“(You)” after the name; role select and Remove hidden by default.',
        },
        {
          name: 'Role read-only',
          description:
            'Without `onMemberRoleChange` (or with `canChangeRole={false}`) the role label is text.',
        },
        {
          name: 'Busy',
          attribute: 'aria-busy on the select or button',
          description: 'While a role change or resend runs.',
        },
        {
          name: 'Error',
          attribute: 'role="alert" in the row',
          description: 'A failed role change or resend, in the product’s words.',
        },
        { name: 'Disabled', description: 'Every control in the row disabled.' },
      ],
    },
    interaction: {
      items: [
        'Changing the role calls `onMemberRoleChange`. Uncontrolled, the select updates at once and rolls back if the promise rejects; controlled, it waits for the product’s new `memberRole`.',
        'Resend calls `onResend` and announces “Invite sent again.”',
        'Remove opens a danger ConfirmDialog (title, description and optional consequences) and calls `onRemove`; errors stay in the dialog so the person can retry.',
        'Extra actions (for example a DropdownMenu) go in `actions`, before Remove.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Moves between the role select, Resend, extra actions and Remove.' },
        { keys: 'Arrow keys', action: 'Change the role in the native select.' },
        { keys: 'Enter / Space', action: 'Activates Resend or Remove.' },
        { keys: 'Escape', action: 'Closes the remove confirmation.' },
      ],
    },
    accessibility: {
      role: 'listitem (in MemberList, a list)',
      name: 'Rows are named by their text; controls are labelled after the member.',
      items: [
        'MemberList is a real `<ul>`, so screen readers announce “list, n items”.',
        'The avatar is decorative next to the visible name; presence is read as hidden text after the name (“Online”).',
        'Role select: “Role for {name}”. Remove: “Remove {name}” or “Revoke invite for {name}”. Resend: “Resend invite, {name}”.',
        'Resend success is a polite status; failures are `role="alert"`.',
        'Verified with axe in jsdom and in Chromium, Firefox and WebKit on /components/member-row.',
      ],
    },
    responsive: {
      items: [
        'Name and email truncate with an ellipsis instead of pushing the controls off screen.',
        'When the row is narrower than 26rem (container query), the controls move under the name, aligned with the text.',
        'Buttons keep their touch size at every width.',
      ],
    },
    theme: {
      items: [
        'Monarch tokens only: names in `--mn-color-text-default`, emails in `--mn-color-text-muted`, row separators `--mn-color-border-subtle`.',
        'Badges use the info and warning tones; follows light, dark and high-contrast themes.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Members with editable roles',
          stack: true,
          render: () => <TeamDemo />,
        },
        {
          title: 'Invited, suspended and read-only',
          stack: true,
          render: () => (
            <MemberList aria-label="People" style={{ width: '100%' }}>
              <MemberRow
                name="lee@example.com"
                status="invited"
                meta="Invite expires in 6 days"
                roleOptions={ROLES}
                memberRole="member"
                onResend={wait}
                onRemove={wait}
              />
              <MemberRow
                name="Sam Carter"
                email="sam@example.com"
                status="suspended"
                roleOptions={ROLES}
                memberRole="member"
              />
              <MemberRow
                name="Mei Tanaka"
                email="mei@example.com"
                presence="online"
                badges={<Badge size="sm">Guest</Badge>}
                meta="Joined 12 March"
                roleOptions={ROLES}
                memberRole="admin"
              />
            </MemberList>
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Pass only the roles the viewer may assign in `roleOptions`; enforce the same rule on the server.',
        'Add `removeConsequences` when removing someone affects their work (“3 open tickets go back to the queue”).',
        'Use `meta` for one useful fact: joined date, invite expiry or availability.',
      ],
      dont: [
        'Do not let people remove themselves from this row; offer “Leave” elsewhere.',
        'Do not put the role in a menu when there are only a few roles; the native select is faster.',
        'Do not show “Active” on every row; it is the default.',
      ],
    },
    related: {
      items: [
        { id: 'avatar', reason: 'The avatar used here; on its own for compact people chips.' },
        { id: 'assignee-picker', reason: 'Choose people to assign or add.' },
        { id: 'data-table', reason: 'Many columns per person, sorting and bulk actions.' },
        { id: 'confirm-dialog', reason: 'The removal confirmation.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['button', 'badge', 'form-field', 'confirm-dialog', 'avatar'],
    },
    testing: {
      unit: [
        'Identity, presence text, badges, email, meta and read-only role in a list; self row; axe.',
        'Labelled role select changes the role (uncontrolled); axe.',
        'A rejected role change restores the previous role and shows the error.',
        'Controlled role waits for the product and is busy meanwhile.',
        'Own row hides role select and Remove by default.',
        'Resend calls `onResend` and announces success; axe.',
        'Remove by keyboard opens a confirm dialog with consequences, focus on Cancel; rejection shown in the dialog, retry succeeds.',
        'Invited rows revoke; suspended rows are marked.',
        'Disabled, `as="div"` and translated messages; renders on the server.',
      ],
      axe: true,
      ssr: true,
      browser: [
        'Visual baselines: /__gallery/member-row',
        'Cross-browser axe: /components/member-row',
      ],
    },
  },
} satisfies ComponentDocs;
