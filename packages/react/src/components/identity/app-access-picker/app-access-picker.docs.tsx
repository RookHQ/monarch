import { useState } from 'react';
import {
  AppAccessPicker,
  AppAccessSummary,
  type AppAccessOption,
} from '@rookcom/monarch-react/patterns';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const APPS: AppAccessOption[] = [
  { id: 'notes', name: 'Notes', description: 'Shared notes and documents' },
  { id: 'planner', name: 'Planner', description: 'Projects, tasks and timelines' },
  { id: 'calendar', name: 'Calendar', description: 'Team calendars and booking pages' },
  { id: 'helpdesk', name: 'Helpdesk', description: 'Customer tickets', badge: 'Beta' },
];

function Invite() {
  const [value, setValue] = useState<string[]>(['notes']);
  return (
    <AppAccessPicker
      apps={APPS}
      value={value}
      onValueChange={setValue}
      description="Joining a workspace grants no app access on its own. These apps are applied when the invitation is accepted."
    />
  );
}

export default {
  id: 'app-access-picker',
  title: 'AppAccessPicker',
  summary: 'A checklist of apps to grant a workspace member, with a compact summary for tables.',
  sections: {
    purpose: {
      summary:
        'Workspaces decide which apps each member may open. AppAccessPicker is the controlled checklist for inviting someone or editing a member’s access, with select all and a count; AppAccessSummary shows the result in one line in a members table. Needed by: Rook ID (workspace members: invite and edit access).',
      useWhen: [
        'Choosing which apps a new or existing workspace member can open.',
        'Showing a member’s app access in a list (AppAccessSummary).',
      ],
      avoidWhen: [
        'Apps a person has authorised with their own account: use ConnectedAppsList.',
        'A long, searchable list of anything: use EntityPicker or a Combobox.',
      ],
    },
    designPrinciples: {
      items: [
        'The checkbox sits on the leading edge, so it stays visible however narrow the row gets.',
        'The whole row is the label: clicking the name or logo toggles it.',
        '"No app access" is a real state and is named, not left blank.',
        'Purely controlled: the product owns the selection and saving.',
      ],
    },
    apiReference: { source: 'typescript' },
    typedProperties: {
      source: 'typescript',
      notes: [
        '`apps` is `{ id, name, description?, logoSrc?, badge?, disabled? }[]`; `value` holds ids.',
        'Disabled apps keep their state through "Select all" and "Clear all".',
        'Migrating from the app copy (Rook ID `AppAccessPicker` and `AppAccessSummary` in `components/workspace/AppAccessPicker.tsx`): `selected` → `value`, `onToggle(slug)` → `onValueChange(next)` (pass the setter directly), `slug` → `id`, `logoUrl` → `logoSrc`. The entitlement tag becomes the generic `badge`. The members page’s own "Select all / Clear all" button is built in. `AppAccessSummary` takes `value` instead of `slugs`.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'showBulkActions',
          values: ['true', 'false'],
          description: 'The count and the "Select all / Clear all" button.',
        },
        {
          prop: 'hideLabel',
          values: ['false', 'true'],
          description: 'Hide the legend visually when a heading already names the list.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Selected',
          attribute: 'data-selected',
          description: 'Accent border and background on the row.',
        },
        {
          name: 'Disabled',
          attribute: 'data-disabled',
          description: 'On the root or a row; the row dims and does not toggle.',
        },
        {
          name: 'Loading',
          attribute: 'data-loading, aria-busy',
          description: 'Three placeholder rows and a spoken "Loading apps".',
        },
        { name: 'Empty', description: 'A dashed box saying no apps are available.' },
      ],
    },
    interaction: {
      items: [
        'Clicking a row (checkbox, logo or name) toggles that app.',
        '"Select all" selects every enabled app; it becomes "Clear all" when all are selected.',
        'The list scrolls inside itself past about seven rows (`--mn-app-access-picker-max-height`).',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab / Shift+Tab', action: 'Moves between the bulk button and each checkbox.' },
        { keys: 'Space', action: 'Toggles the focused checkbox.' },
        { keys: 'Enter / Space (bulk button)', action: 'Selects or clears every enabled app.' },
      ],
    },
    accessibility: {
      role: 'group (fieldset) of checkboxes',
      name: 'The legend ("App access"); each checkbox is named by its app and described by its description.',
      items: [
        'The count is a polite live region, so changes are announced.',
        'Logos are decorative.',
        'AppAccessSummary speaks the hidden names after "+N" ("and 2 more: …").',
      ],
    },
    responsive: {
      items: [
        'Names and descriptions truncate with an ellipsis instead of widening the row.',
        'Rows are at least 44px tall on touch screens.',
      ],
    },
    theme: {
      items: [
        'Rows on the subtle background; selected rows use the accent subtle and border tokens.',
        'Summary tags use the accent subtle background and accent strong text.',
        'In forced-colors mode rows get system-colour borders and the selected row a Highlight border.',
      ],
    },
    examples: {
      items: [
        { title: 'Invite with app access', stack: true, render: () => <Invite /> },
        {
          title: 'Loading',
          stack: true,
          render: () => <AppAccessPicker apps={[]} value={[]} onValueChange={() => {}} loading />,
        },
        {
          title: 'Summary in a members table',
          stack: true,
          render: () => (
            <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
              <AppAccessSummary apps={APPS} value={['notes', 'planner', 'calendar']} />
              <AppAccessSummary apps={APPS} value={[]} />
            </div>
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Say under the list when the grants take effect (`description`).',
        'Disable apps the member always has, rather than hiding them.',
      ],
      dont: [
        'Do not save on every toggle inside a dialog; save when the person confirms.',
        'Do not use `badge` for long text; keep it to one or two words.',
      ],
    },
    related: {
      items: [
        { id: 'no-app-access', reason: 'What a member sees when they have no access to an app.' },
        { id: 'member-row', reason: 'A members list row where AppAccessSummary fits.' },
        { id: 'connected-apps-list', reason: 'Apps a person authorised with their own account.' },
        { id: 'checkbox', reason: 'The control each row uses.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['button', 'avatar', 'checkbox', 'skeleton'],
    },
    testing: {
      unit: [
        'A named group of labelled, described checkboxes with a count; axe passes.',
        'Toggles by label click and Space; select all and clear all keep disabled apps.',
        'Disabled, loading and empty states.',
        'Help text as group description; translated messages.',
        'AppAccessSummary: sorted names, +N with hidden names spoken, empty and unknown ids.',
        'Both render on the server.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
