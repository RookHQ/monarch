import { useState } from 'react';
import { WorkspaceSwitcher } from '@rookcom/monarch-react/shell';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const workspaces = [
  { id: 'w1', name: 'Acme', handle: 'acme' },
  { id: 'w2', name: 'Northwind Traders', handle: 'northwind' },
  { id: 'w3', name: 'Globex', handle: 'globex', keywords: 'globex corporation' },
];

function Basic() {
  const [workspace, setWorkspace] = useState('w1');
  return (
    <div style={{ width: '15rem' }}>
      <WorkspaceSwitcher
        workspaces={workspaces}
        value={workspace}
        onValueChange={setWorkspace}
        onCreate={() => {}}
        manageHref="https://id.example.com/workspaces"
      />
    </div>
  );
}

function Compact() {
  return (
    <div style={{ display: 'flex', gap: 'var(--mn-space-3)', alignItems: 'center' }}>
      <WorkspaceSwitcher workspaces={workspaces} defaultValue="w2" collapsed />
      <div style={{ width: '15rem' }}>
        <WorkspaceSwitcher workspaces={workspaces} createHref="#new-workspace" />
      </div>
    </div>
  );
}

export default {
  id: 'workspace-switcher',
  title: 'WorkspaceSwitcher',
  summary: 'Switch between the workspaces a person belongs to, with search and Create workspace.',
  sections: {
    purpose: {
      summary:
        'Every Rook app lets a person move between their workspaces from the rail. One shared control names its trigger, marks the current workspace for screen readers and supports search.',
      useWhen: ['The rail’s `workspaceSwitcher` slot, or the top of a settings sidebar.'],
      avoidWhen: [
        'Choosing a Space inside the workspace: use SpaceSwitcher.',
        'Picking a workspace as a form value (inviting to a workspace): use EntityPicker in a Field.',
      ],
    },
    designPrinciples: {
      items: [
        'The app does the switch: `onValueChange` gets the id and the app navigates (and may keep the sub-page).',
        'Square avatars for workspaces (round ones are people), with initials when there is no logo.',
        'The current workspace is `aria-selected`, never marked by a coloured dot alone.',
        'Search matches names, handles and hidden keywords such as old names.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Forwards its ref to the trigger button. Built on EntityPicker and Avatar from `@rookcom/monarch-react`.',
        '`WorkspaceOption` is `{ id, name, handle?, avatarUrl?, keywords? }`.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Controlled with `value` + `onValueChange`, or uncontrolled with `defaultValue`. There is no "none" choice.',
        '`onCreate` or `createHref` adds "Create workspace"; `manageHref` adds "All workspaces" (new tab unless `manageExternal={false}`).',
      ],
    },
    variants: {
      items: [
        {
          prop: 'collapsed',
          values: ['false', 'true'],
          description: 'Avatar-only trigger with the name in a tooltip, for the 48px rail.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Current workspace',
          attribute: 'aria-selected="true"',
          description: 'Checked in the list; the trigger shows its name and @handle.',
        },
        {
          name: 'Nothing chosen',
          description: 'The trigger reads "Workspace: Choose a workspace" with a neutral icon.',
        },
        { name: 'Loading', description: 'The list says "Loading workspaces…".' },
        { name: 'Open', attribute: 'aria-expanded="true"', description: 'On the trigger.' },
      ],
    },
    interaction: {
      items: [
        'Opening moves focus to the search box; choosing closes the popover and returns focus to the trigger.',
        'Choosing the current workspace again does nothing.',
        '"Create workspace" closes the popover first, then runs `onCreate` (open your dialog there).',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Enter / Space / ↓', action: 'Open from the trigger.' },
        { keys: '↓ / ↑ / Home / End', action: 'Move through the workspaces.' },
        { keys: 'Typing', action: 'Filter by name, handle or keyword.' },
        { keys: 'Enter', action: 'Switch to the highlighted workspace.' },
        { keys: 'Escape', action: 'Close and return focus to the trigger.' },
      ],
    },
    accessibility: {
      role: 'button opening a dialog with a search combobox and a listbox',
      name: 'The trigger is named by its text, "Workspace: <name> @handle" (`messages.label` is visually hidden), also when collapsed to an avatar.',
      items: [
        'Fixes the audited gaps: a collapsed trigger without a name and an active item shown by colour only.',
        'Avatars are decorative; the option label and @handle name each workspace.',
        'The All workspaces link announces the new tab.',
      ],
    },
    responsive: {
      items: [
        'The expanded trigger fills its container and truncates long names; the popover never exceeds the screen width.',
      ],
    },
    theme: {
      items: [
        'Tokens only; inside RookRail the trigger uses the accent panel colours.',
        'Transitions are removed under reduced motion.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Switch, create and manage',
          render: () => <Basic />,
          code: `<WorkspaceSwitcher
  workspaces={workspaces}
  value={workspaceId}
  onValueChange={(id) => router.push(\`/w/\${slugOf(id)}\`)}
  onCreate={openCreateWorkspace}
  manageHref={\`\${ROOK_ID}/workspaces\`}
/>`,
        },
        {
          title: 'Collapsed, and nothing chosen yet',
          render: () => <Compact />,
          code: `<WorkspaceSwitcher workspaces={workspaces} defaultValue="w2" collapsed />
<WorkspaceSwitcher workspaces={workspaces} createHref="/onboarding" />`,
        },
      ],
    },
    doDont: {
      do: [
        'Keep the sub-page when switching if the page exists in the other workspace.',
        'Pass workspace logos as `avatarUrl`; initials are the fallback.',
      ],
      dont: [
        'Do not list workspaces the person cannot open.',
        'Do not put Space choices in this list.',
      ],
    },
    related: {
      items: [
        { id: 'space-switcher', reason: 'The Space inside the workspace.' },
        { id: 'entity-picker', reason: 'The picker it is built on.' },
        { id: 'avatar', reason: 'Workspace logos and initials.' },
        { id: 'rook-rail', reason: 'Has a slot for it.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['avatar', 'entity-picker'],
    },
    testing: {
      unit: [
        'Trigger named with the current workspace and its handle.',
        'List with the current one selected; switching calls `onValueChange` and returns focus.',
        'Keyboard search over names, handles and keywords; re-choosing the current one is ignored; controlled mode.',
        'Create workspace (action and link) and All workspaces; placeholder and empty message; collapsed tooltip.',
      ],
      axe: true,
      ssr: true,
      browser: [
        'Visual baselines: /__gallery/workspace-switcher',
        'Cross-browser axe: /components/workspace-switcher',
      ],
    },
  },
} satisfies ComponentDocs;
