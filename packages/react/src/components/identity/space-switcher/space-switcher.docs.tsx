import { useState } from 'react';
import { SpaceSwitcher } from '@rookcom/monarch-react/shell';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const spaces = [
  { id: 's-eng', name: 'Engineering', description: '18 members' },
  { id: 's-mkt', name: 'Marketing', description: '9 members' },
  { id: 's-ops', name: 'Operations', description: '6 members' },
  { id: 's-sales', name: 'Sales', description: '12 members' },
];

function Basic() {
  const [space, setSpace] = useState<string | null>(null);
  return (
    <div style={{ width: '15rem' }}>
      <SpaceSwitcher
        spaces={spaces}
        value={space}
        onValueChange={setSpace}
        manageHref="https://id.example.com/spaces"
        onCreate={() => {}}
      />
    </div>
  );
}

function Compact() {
  return (
    <div style={{ display: 'flex', gap: 'var(--mn-space-3)', alignItems: 'center' }}>
      <SpaceSwitcher spaces={spaces} defaultValue="s-mkt" collapsed />
      <div style={{ width: '13rem' }}>
        <SpaceSwitcher spaces={spaces} defaultValue="s-ops" allowAll={false} />
      </div>
    </div>
  );
}

export default {
  id: 'space-switcher',
  title: 'SpaceSwitcher',
  summary: 'Pick "All spaces" or one Space of the workspace, with search, New and Manage links.',
  sections: {
    purpose: {
      summary:
        'Rook workspaces are split into Spaces (department-like slices managed in Rook ID). The switcher sets which Space the app shows. Its listbox supports arrow-key navigation.',
      useWhen: [
        'An app that scopes its content by Space, in the rail’s `spaceSwitcher` slot or a page header.',
      ],
      avoidWhen: [
        'Switching workspaces: use WorkspaceSwitcher.',
        'Filtering a single list by team or department: use a filter in FilterBar.',
      ],
    },
    designPrinciples: {
      items: [
        'Data in, choice out: the app passes the Spaces this person may open and stores the choice; the switcher never fetches or persists.',
        'One listbox model (EntityPicker): arrow keys, Home/End, type to search, Enter to choose, Escape to close; options are real `option`s, not buttons.',
        '"All spaces" is a first-class choice, shown first and checked when nothing is chosen.',
        'A Space that disappears (deleted, access removed) reads as "All spaces" instead of a stale name.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Forwards its ref to the trigger button. Built on EntityPicker from `@rookcom/monarch-react`.',
        '`SpaceOption` is `{ id, name, description?, icon? }`.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        '`value` is a Space id or `null` for "All spaces"; controlled with `value` + `onValueChange`, or uncontrolled with `defaultValue`.',
        '`manageHref` opens in a new tab unless `manageExternal={false}`; `onCreate` or `createHref` adds "New Space".',
      ],
    },
    variants: {
      items: [
        {
          prop: 'collapsed',
          values: ['false', 'true'],
          description: 'Icon-only trigger with the current Space in a tooltip, for the 48px rail.',
        },
        {
          prop: 'allowAll',
          values: ['true', 'false'],
          description: 'Offer "All spaces". Without it a Space must stay chosen.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'All spaces',
          attribute: 'data-all',
          description: 'Nothing chosen: the trigger reads "Space: All spaces".',
        },
        {
          name: 'Current Space',
          attribute: 'aria-selected="true"',
          description: 'Checked in the list and named on the trigger.',
        },
        { name: 'Loading', description: 'The list says "Loading spaces…" while `loading`.' },
        {
          name: 'Empty',
          description:
            'With no Spaces and nothing loading the switcher renders nothing (`hideWhenEmpty`), as the products did.',
        },
        { name: 'Open', attribute: 'aria-expanded="true"', description: 'On the trigger.' },
      ],
    },
    interaction: {
      items: [
        'Clicking the trigger (or Enter, Space, ↓) opens the popover with focus in the search box.',
        'Typing filters by name; choosing closes the popover and returns focus to the trigger.',
        '"New Space" closes the popover, then runs `onCreate` (or follows `createHref`); "Manage Spaces" opens Rook ID in a new tab.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Enter / Space / ↓', action: 'Open the popover from the trigger.' },
        { keys: '↓ / ↑', action: 'Move through the Spaces.' },
        { keys: 'Home / End', action: 'First / last Space.' },
        { keys: 'Enter', action: 'Choose the highlighted Space.' },
        { keys: 'Tab', action: 'From the search box to New / Manage.' },
        { keys: 'Escape', action: 'Close and return focus to the trigger.' },
      ],
    },
    accessibility: {
      role: 'button opening a dialog that contains a combobox (search) and a listbox',
      name: 'The trigger is "Space: <current>" (`messages.label`); the popover is named "Space".',
      items: [
        'Follows the APG combobox pattern with a listbox popup (EntityPicker); the active option is tracked with `aria-activedescendant`.',
        'The collapsed trigger keeps its full name and adds a tooltip.',
        'The Manage link announces that it opens in a new tab.',
      ],
    },
    responsive: {
      items: [
        'The trigger fills its container and truncates long names.',
        'The popover is 18rem wide, never wider than the screen minus 2rem; New / Manage rows are 44px tall on touch screens.',
      ],
    },
    theme: {
      items: [
        'Surface, border and text tokens; inside RookRail the trigger switches to the accent panel colours.',
        'Transitions are removed under reduced motion.',
      ],
    },
    examples: {
      items: [
        {
          title: 'All spaces, with New and Manage',
          render: () => <Basic />,
        },
        {
          title: 'Collapsed, and without All spaces',
          render: () => <Compact />,
        },
      ],
    },
    doDont: {
      do: [
        'Pass only the Spaces this person may open, and still check access on the server.',
        'Remember the choice per workspace in the app (Rook apps share it across apps).',
        'Use the rail slot function to pass `collapsed`.',
      ],
      dont: [
        'Do not fetch Spaces inside a slot render on every render; load them once and pass the list.',
        'Do not use it for navigation between pages.',
      ],
    },
    related: {
      items: [
        { id: 'workspace-switcher', reason: 'The workspace above the Space.' },
        { id: 'entity-picker', reason: 'The picker it is built on.' },
        { id: 'rook-rail', reason: 'Has a slot for it.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['entity-picker'],
    },
    testing: {
      unit: [
        'Trigger named with the current Space; starts on All spaces.',
        'Popover with search focused, the current Space selected and the Manage link.',
        'Typing filters; arrow keys and Enter choose; focus returns to the trigger.',
        'All spaces clears the choice; `allowAll={false}` hides it; controlled mode; a missing Space reads as All spaces.',
        'New Space runs after closing; Escape returns focus; hidden when empty, loading message; collapsed tooltip; translation.',
      ],
      axe: true,
      ssr: true,
      browser: [
        'Visual baselines: /__gallery/space-switcher',
        'Cross-browser axe: /components/space-switcher',
      ],
    },
  },
} satisfies ComponentDocs;
