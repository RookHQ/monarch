import { useState } from 'react';
import { Button } from '@rookcom/monarch-react';
import { ScopeSelector, type ScopeGroup } from '@rookcom/monarch-react/patterns';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const groups: ScopeGroup[] = [
  {
    id: 'projects',
    label: 'Projects API',
    description: 'Example Inc.',
    scopes: [
      { value: 'projects:read', description: 'List and read projects.' },
      { value: 'projects:write', description: 'Create, update and archive projects.' },
    ],
  },
  {
    id: 'reports',
    label: 'Reports API',
    description: 'Example Inc.',
    scopes: [
      { value: 'reports:read', description: 'Read generated reports.' },
      { value: 'reports:export', description: 'Download reports as files.' },
    ],
  },
];

const width = { width: '100%', maxWidth: '32rem' } as const;

function KeyScopes() {
  const [value, setValue] = useState<string[]>(['projects:read']);
  return (
    <div style={width}>
      <ScopeSelector
        groups={groups}
        value={value}
        onValueChange={setValue}
        defaultExpanded={['projects']}
      />
    </div>
  );
}

function Loading() {
  const [value, setValue] = useState<string[]>([]);
  const [loaded, setLoaded] = useState(false);
  const shown: ScopeGroup[] = loaded
    ? groups
    : groups.map((g) => ({ ...g, scopes: [], loading: true }));
  return (
    <div style={{ ...width, display: 'grid', gap: 'var(--mn-space-2)' }}>
      <ScopeSelector groups={shown} value={value} onValueChange={setValue} autoSelect="all" />
      <Button onClick={() => setLoaded(true)} disabled={loaded}>
        Finish loading
      </Button>
      <p style={{ margin: 0, fontSize: 'var(--mn-text-xs)' }}>
        Selected: {value.join(', ') || 'none'}
      </p>
    </div>
  );
}

export default {
  id: 'scope-selector',
  title: 'ScopeSelector',
  summary: 'Choose the scopes (permissions) to grant, grouped by API or resource.',
  sections: {
    purpose: {
      summary:
        'Lets people grant only the permissions an integration needs: scopes are grouped by API or resource, each group collapses, shows how many of its scopes are chosen and has "Select all". Needed by: Developers (creating and editing API keys and OAuth apps).',
      useWhen: [
        'An API key, token or OAuth client is granted scopes from several APIs.',
        'A role or integration is given permissions that fall into resource groups.',
      ],
      avoidWhen: [
        'There are only a handful of options with no grouping: use Checkbox items in a Field.',
        'Only one option can be chosen: use a Select or radio group.',
        'Read-only display of granted scopes: list them as text or badges.',
      ],
    },
    designPrinciples: {
      items: [
        'Least privilege by default: nothing is selected unless the product asks for `autoSelect`.',
        'Every group says how many of its scopes are granted while collapsed, so a long grant can be reviewed at a glance.',
        'Data in, selection out: the product decides which scopes each group offers (for example, only scopes allowed for a key kind) and loads them; the selector only shows and chooses.',
        'Long lists page in steps of `pageSize`, so many APIs stay manageable.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Renders a `<fieldset>` with a legend and forwards its ref and HTML attributes to it.',
        'Selected values that belong to no group are kept in the value, so a product can strip or keep them as it chooses.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        '`value`/`onValueChange` (controlled) or `defaultValue` (uncontrolled).',
        '`autoSelect` replaces the app copy’s effect: `"all"` selects every scope of each group that has none selected once it loads; `"added"` does the same only for groups added after the first render.',
        'Migrating from the app copy (Developers `ScopePicker` in `components/console/KeyFields.tsx`): `apis` becomes `groups` (one ScopeGroup per API with its name, icon and scopes, already filtered for the key kind; set `loading` while details load); `selected` becomes `value`; `onChange` becomes `onValueChange`; `autoSelect` becomes `autoSelect="all"`; `autoSelect` with `skipInitial` becomes `autoSelect="added"`; `disabled` is unchanged. The ten-at-a-time paging is built in (`pageSize`). The "No scopes for publishable keys" text moves to `group.emptyMessage`.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'autoSelect',
          values: ['undefined', "'all'", "'added'"],
          description: 'Whether groups start with every scope selected once loaded.',
        },
        {
          prop: 'defaultExpanded',
          values: ['string[]', "'all'"],
          description: 'Groups open on first render. Default none.',
        },
        {
          prop: 'hideLabel',
          values: ['true', 'false'],
          description: 'Visually hide the legend when a heading above already names it.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Expanded',
          attribute: 'data-expanded (group), aria-expanded (trigger)',
          description: 'The group’s scopes are shown as checkboxes.',
        },
        {
          name: 'Loading',
          attribute: 'data-loading (group)',
          description: 'The summary says scopes are loading; no checkboxes or "Select all".',
        },
        {
          name: 'Has selection',
          attribute: 'data-selected (group)',
          description: 'At least one of the group’s scopes is selected.',
        },
        {
          name: 'Disabled',
          attribute: 'data-disabled',
          description:
            'Checkboxes are disabled and "Select all" is hidden; groups still expand for review.',
        },
        {
          name: 'Empty',
          attribute: 'data-empty',
          description: 'No groups; shows `emptyState` or the empty message.',
        },
      ],
    },
    interaction: {
      items: [
        'Clicking a group header expands or collapses it.',
        '"Select all" grants every enabled scope of the group; when all are granted it becomes "Clear all".',
        'Checking a scope adds it to the value; unchecking removes it.',
        '"Show more" reveals the next page of groups and moves focus to the first new group.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Tab / Shift+Tab',
          action: 'Moves between group headers, "Select all" buttons and checkboxes.',
        },
        {
          keys: 'Enter / Space',
          action: 'On a header: expand or collapse. On a button: activate it.',
        },
        { keys: 'Space', action: 'On a checkbox: grant or remove the scope.' },
      ],
    },
    accessibility: {
      role: 'group (fieldset), button, checkbox',
      name: 'The fieldset is named by its legend (`messages.label`). Each group header is a disclosure button with `aria-expanded` and `aria-controls`; the scope list is a group named by the group label. "Select all" is named "Select all {group}".',
      items: [
        'The scope count is part of the header button, so it is read with the group name.',
        'Disabled scopes stay in the list and are announced as disabled.',
        'Focus moves to the first newly shown group after "Show more", so it is never lost.',
        'Icons are decorative and hidden from assistive technology.',
      ],
    },
    responsive: {
      items: [
        'Fills its container; long group names truncate and scope values wrap anywhere.',
        'Rows are at least 44px tall, and headers and buttons grow to 44px on touch screens.',
        'Works at 390px width without horizontal scrolling.',
      ],
    },
    theme: {
      items: [
        'Surfaces, borders and text use `--mn-color-*` tokens; Checkbox and Button bring their own theming.',
        'Scope values use `--mn-font-mono`.',
        'Forced-colours mode keeps borders and focus outlines visible.',
      ],
    },
    examples: {
      items: [
        { title: 'Scopes for a key', stack: true, render: () => <KeyScopes /> },
        { title: 'Auto-select once loaded', stack: true, render: () => <Loading /> },
        {
          title: 'Read-only review',
          stack: true,
          render: () => (
            <div style={width}>
              <ScopeSelector
                groups={groups}
                defaultValue={['projects:read', 'reports:read']}
                disabled
                messages={{ label: 'Granted scopes' }}
              />
            </div>
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Filter the scopes each group offers in the product, for example by key kind or plan.',
        'Mark groups `loading` while their scopes are fetched, so `autoSelect` waits for them.',
        'Explain what a scope allows in `description`.',
      ],
      dont: [
        'Do not fetch inside the selector or pass product routes; it takes data only.',
        'Do not auto-select scopes on an existing grant’s initial groups: use `autoSelect="added"`.',
      ],
    },
    related: {
      items: [
        { id: 'checkbox', reason: 'A short, ungrouped list of options.' },
        {
          id: 'key-created-dialog',
          reason: 'Shows the new key once it is created with these scopes.',
        },
        { id: 'secret-field', reason: 'Shows or enters the key itself.' },
        { id: 'accordion', reason: 'Collapsible sections that are not a selection.' },
      ],
    },
    dependencies: { packages: ['react'], components: ['button', 'checkbox'] },
    testing: {
      unit: [
        'Fieldset named by its legend; collapsed groups show "n of m scopes".',
        'Expanding a group shows its checkboxes; toggling updates the value.',
        '"Select all" and "Clear all" skip disabled scopes.',
        'Keyboard: Tab order, Enter to expand, Space to check.',
        'Paging with "Show more" and focus moving to the first new group.',
        '`autoSelect="all"` waits for loading groups; `autoSelect="added"` skips initial groups.',
        'Disabled mode, empty state, messages and ref.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
