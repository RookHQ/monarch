import { useState } from 'react';
import { AssigneePicker, Field, FieldLabel } from '@rookcom/monarch-react';
import type { AssigneePerson } from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const members: AssigneePerson[] = [
  {
    id: 'm1',
    name: 'Priya Raman',
    email: 'priya@example.com',
    hint: '4 open',
    group: 'In this team',
  },
  {
    id: 'm2',
    name: 'Jonas Weber',
    email: 'jonas@example.com',
    status: 'online',
    group: 'In this team',
  },
  { id: 'm3', name: 'Amara Okafor', email: 'amara@example.com', group: 'Everyone else' },
  {
    id: 'm4',
    name: 'Lucas Martin',
    email: 'lucas@example.com',
    status: 'away',
    group: 'Everyone else',
  },
  { id: 'm5', name: 'Mei Chen', email: 'mei@example.com', group: 'Everyone else' },
];

function SingleDemo() {
  const [value, setValue] = useState<string | null>('m2');
  return (
    <Field style={{ width: '100%', maxWidth: 300 }}>
      <FieldLabel>Assignee</FieldLabel>
      <AssigneePicker people={members} currentUserId="m3" value={value} onValueChange={setValue} />
    </Field>
  );
}

function MultipleDemo() {
  const [values, setValues] = useState(['m1', 'm2', 'm4', 'm5']);
  return (
    <Field style={{ width: '100%', maxWidth: 300 }}>
      <FieldLabel>Reviewers</FieldLabel>
      <AssigneePicker
        multiple
        people={members}
        currentUserId="m3"
        values={values}
        onValuesChange={setValues}
        allowUnassigned={false}
        placeholder="Add reviewers"
      />
    </Field>
  );
}

export default {
  id: 'assignee-picker',
  title: 'AssigneePicker',
  summary: 'People picker with avatars, "you" first, Unassigned, Assign to me and an avatar stack.',
  sections: {
    purpose: {
      summary:
        'Assigns one or several people (owners, assignees, reviewers) from a searchable list of avatar rows. It is EntityPicker preset for people: the product passes its members, Monarch handles avatars, ordering, the shortcuts and the trigger summary.',
      useWhen: [
        'Assigning a ticket, issue, task or alert to a person.',
        'Choosing reviewers, watchers or members (`multiple`).',
      ],
      avoidWhen: [
        'Records that are not people: use EntityPicker.',
        'Mentioning people inside text: use a mention menu in the editor.',
      ],
    },
    designPrinciples: {
      items: [
        'People are recognised by face and name: every row has an Avatar, with presence when the product knows it.',
        'The most likely choices come first: Unassigned, then you, then the product’s order (groups such as "In this team").',
        'The product owns the people: members come in as `people` or from `onSearch`; nothing is fetched by Monarch.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Accepts every EntityPicker prop except `options`, `onSearch`, `valueOptions`, `clearOption`, `onCreate` and `renderValue`, which it builds from people.',
        'Forwards its ref to the trigger button. Values are person ids; Unassigned is `null` (single) or `[]` (multiple).',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        '`AssigneePerson` is `{ id, name, email?, avatarUrl?, status?, hint?, group?, disabled? }`.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'multiple',
          values: ['false', 'true'],
          description:
            'One assignee with avatar and name, or several as an avatar stack and a count.',
        },
        {
          prop: 'size',
          values: ['sm', 'md', 'lg'],
          description: 'Trigger height, as EntityPicker.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Unassigned',
          attribute: 'data-placeholder',
          description: 'No one assigned: "Unassigned" in muted text.',
        },
        {
          name: 'Assigned',
          description: 'The trigger shows the avatar and name ("(you)" for the current user).',
        },
        {
          name: 'Several',
          description: 'An AvatarGroup (up to `maxAvatars`, then +N) and "N people".',
        },
        {
          name: 'Open',
          attribute: 'aria-expanded="true"',
          description: 'The people list with search.',
        },
        {
          name: 'Invalid / disabled',
          attribute: 'data-invalid, disabled',
          description: 'From `invalid`, `disabled` or the Field.',
        },
      ],
    },
    interaction: {
      items: [
        'Search matches name and email.',
        'The current user (`currentUserId`) is listed first as "Name (you)"; "Assign to me" in the footer assigns them in one click.',
        '"Unassigned" (on by default, `allowUnassigned={false}` to hide) clears the assignment.',
        'Single closes after a pick; multiple toggles people and stays open.',
        'With `onSearch`, people are searched on the server with debounce and abort, as in Combobox.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Enter / Space / ArrowDown (trigger)', action: 'Opens the picker.' },
        { keys: 'ArrowDown / ArrowUp, Home / End', action: 'Moves through people.' },
        { keys: 'Enter', action: 'Assigns (or toggles) the active person.' },
        { keys: 'Tab', action: 'Reaches "Assign to me" in the footer.' },
        { keys: 'Escape', action: 'Closes and returns focus to the trigger.' },
      ],
    },
    accessibility: {
      role: 'select-only combobox opening a dialog with a search combobox and listbox',
      name: 'The Field label or `aria-label`; the current assignee is the trigger’s content.',
      items: [
        'Avatars in rows and the trigger are decorative; the name (and "(you)") carries the meaning.',
        'Presence is shown on row avatars; give status in `hint` too when it matters, as colour alone is not enough.',
        'Inherits EntityPicker’s dialog, combobox and listbox semantics.',
      ],
    },
    responsive: {
      items: [
        'The list is at least 16rem wide; names and emails truncate.',
        'The trigger keeps the avatar stack and truncates the text.',
      ],
    },
    theme: {
      items: [
        'Avatars fall back to initials on seeded colours; everything else uses semantic tokens in all themes.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Assignee with you and Unassigned',
          stack: true,
          render: () => <SingleDemo />,
        },
        {
          title: 'Several reviewers',
          stack: true,
          render: () => <MultipleDemo />,
        },
      ],
    },
    doDont: {
      do: [
        'Pass `currentUserId` so people can assign themselves quickly.',
        'Group people the way the product thinks ("In this team", "Everyone else") with `group`.',
        'Pass `valuePeople` when the assignee may not be in the loaded list.',
      ],
      dont: [
        'Do not use a native select of members; it loses search and avatars.',
        'Do not put the person’s availability only in the presence dot colour.',
      ],
    },
    related: {
      items: [
        { id: 'entity-picker', reason: 'The generic record picker it is built on.' },
        { id: 'avatar', reason: 'The avatar and avatar stack.' },
        { id: 'combobox', reason: 'Input-style search field.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['avatar', 'entity-picker'],
    },
    testing: {
      unit: [
        'Lists Unassigned, then you, then everyone with avatars; search matches email; picks and updates the trigger.',
        '"Assign to me" assigns and disappears once assigned; Unassigned clears.',
        'Multiple stays open and summarises as an avatar stack with a count.',
        'Server search keeps the picked name; valuePeople and translated messages.',
      ],
      axe: true,
      ssr: true,
      browser: [
        'Visual baselines: /__gallery/assignee-picker',
        'Cross-browser axe: /components/assignee-picker',
      ],
    },
  },
} satisfies ComponentDocs;
