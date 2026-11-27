import { useState } from 'react';
import { RolePicker, type RoleOption } from '@rookcom/monarch-react/patterns';
import type { ComponentDocs } from '../../../internal/component-docs.js';
// The scenario of the primary example, shared with iOS and Android (CROSS-PLATFORM.md section 8).
import fixture from './role-picker.fixture.json';

const ROLES = fixture.data.roles as RoleOption[];

function Cards() {
  const [role, setRole] = useState(fixture.data.value);
  return <RolePicker roles={ROLES} value={role} onValueChange={setRole} />;
}

export default {
  id: 'role-picker',
  title: 'RolePicker',
  summary: 'Choose a role, with what each role can do in plain words.',
  sections: {
    purpose: {
      summary:
        'Giving someone a role is a permissions decision, and role names alone (“Admin”, “Member”) do not say what the person will be able to do. RolePicker shows each role with a plain description and optional capabilities, keeps roles the current person cannot assign visible with the reason, and comes in two layouts: radio cards for invite and settings forms, and a compact select for table rows. Needed by: Rook ID (workspace members and invites), and the team or members settings in Rook Stride, Rook Base, Rook Tickets, Rook People and Rook Calendar.',
      useWhen: [
        'Inviting someone or changing a member’s role.',
        'Any choice between named access levels where the difference matters.',
      ],
      avoidWhen: [
        'Editing what each role can do: use PermissionMatrix.',
        'Choosing which apps someone can use: use AppAccessPicker.',
      ],
    },
    designPrinciples: {
      items: [
        'Describe what the role can do, not what it is called.',
        'Order roles from least to most access.',
        'Never hide a role the person cannot assign; disable it and say why.',
        'One component, two densities: cards when there is room, a select in tables.',
      ],
    },
    apiReference: { source: 'typescript' },
    typedProperties: {
      source: 'typescript',
      notes: [
        '`roles` is `{ value, label, description?, capabilities?, disabled?, disabledReason? }[]`.',
        'Migrating from shadcn/ui: `<Select value onValueChange><SelectTrigger><SelectValue/></SelectTrigger><SelectContent><SelectItem value="member">…</SelectItem>…` (Rook ID `app/workspaces/[id]/members/page.tsx`) becomes `<RolePicker layout="compact" roles={…} value onValueChange />`; the text inside each `SelectItem` becomes the role’s `description`.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'layout',
          values: ['cards', 'compact'],
          description:
            '`cards` (default): every role visible as a radio card. `compact`: a select whose options show the descriptions.',
        },
        {
          prop: 'size',
          values: ['sm', 'md', 'lg'],
          description: 'Height of the compact trigger.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Checked',
          attribute: 'data-state="checked"',
          description: 'On the selected role card (from RadioCardGroup).',
        },
        {
          name: 'Disabled role',
          attribute: 'data-disabled',
          description:
            'On the card (from RadioCardGroup): the role cannot be assigned; its reason is shown and announced.',
        },
        {
          name: 'Invalid',
          attribute: 'data-invalid',
          description: 'A required choice is missing or rejected.',
        },
        {
          name: 'Layout',
          attribute: 'data-layout',
          description: '`cards` or `compact`, on the root.',
        },
      ],
    },
    interaction: {
      items: [
        'Clicking anywhere on a card selects that role.',
        'In the compact layout the select opens a list with each role’s description; the trigger shows only the name.',
        'Disabled roles cannot be selected but stay readable.',
        'With `name`, the value is posted with a surrounding form.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Moves into the group, onto the selected role.' },
        { keys: 'ArrowDown / ArrowRight', action: 'Selects the next enabled role (cards).' },
        { keys: 'ArrowUp / ArrowLeft', action: 'Selects the previous enabled role (cards).' },
        { keys: 'Enter / Space / ArrowDown', action: 'Opens the compact select.' },
        { keys: 'Escape', action: 'Closes the compact select without changing the role.' },
      ],
    },
    accessibility: {
      role: 'radiogroup of native radios (cards); combobox with a listbox (compact)',
      name: 'The visible "Role" label (`messages.label`) or `aria-label`.',
      items: [
        'Each radio is named by the role name and described by its description, capabilities and disabled reason.',
        'Follows the WAI-ARIA radio group pattern using native inputs, so arrow keys work everywhere.',
        'Help text from `description` describes the whole group or the trigger.',
      ],
    },
    responsive: {
      items: [
        'Cards stack in one column and their text wraps, so nothing scrolls sideways at 390px.',
        'Cards, options and the trigger are at least 44px tall on touch screens.',
      ],
    },
    theme: {
      items: [
        'The selected card uses the accent subtle background and accent border.',
        'Descriptions use the muted text colour; disabled roles the disabled colour.',
        'Forced-colors mode marks the selected card with a Highlight border.',
      ],
    },
    examples: {
      items: [
        { title: 'Radio cards', render: () => <Cards /> },
        {
          title: 'Compact, in a table row',
          render: () => (
            <RolePicker
              roles={ROLES}
              layout="compact"
              size="sm"
              aria-label="Role for Sam Rivera"
              defaultValue="viewer"
            />
          ),
        },
        {
          title: 'With help text',
          render: () => (
            <RolePicker
              roles={ROLES.slice(0, 3)}
              layout="compact"
              description="You can change this later in Members."
              defaultValue="member"
            />
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Write descriptions as what the person will be able to do.',
        'Give a `disabledReason` for every disabled role.',
        'Use the compact layout inside tables and the cards layout in forms.',
      ],
      dont: [
        'Do not filter out roles the person cannot assign; people then wonder where they went.',
        'Do not use colours alone to tell roles apart.',
      ],
    },
    related: {
      items: [
        { id: 'permission-matrix', reason: 'Edit what each role can do.' },
        { id: 'invite-members-dialog', reason: 'Uses the compact RolePicker.' },
        { id: 'app-access-picker', reason: 'Choose which apps someone can use.' },
        { id: 'select', reason: 'The select used by the compact layout.' },
        { id: 'radio-card-group', reason: 'The radio cards used by the cards layout.' },
      ],
    },
    dependencies: { packages: ['react'], components: ['radio-card-group', 'select'] },
    testing: {
      unit: [
        'Shows every role as a described radio; disabled reason is announced; arrow keys change the role; axe passes.',
        'Controlled value and form posting with `name`.',
        'Compact select lists descriptions, disables roles and selects.',
        'Label, help text, invalid state and translated placeholder.',
        'Disables every role.',
        'Renders on the server in both layouts.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
