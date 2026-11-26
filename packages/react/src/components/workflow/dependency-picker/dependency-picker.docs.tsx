import { useState } from 'react';
import {
  DependencyPicker,
  type DependencyItem,
  type DependencyLink,
} from '@rookcom/monarch-react/patterns';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const ITEMS: DependencyItem[] = [
  {
    id: '11',
    key: 'APP-11',
    title: 'Agree the data model',
    status: { id: 'done', name: 'Done', category: 'completed' },
  },
  {
    id: '12',
    key: 'APP-12',
    title: 'Build the import endpoint',
    status: { id: 'doing', name: 'In progress', category: 'started' },
  },
  {
    id: '13',
    key: 'APP-13',
    title: 'Write the migration guide',
    status: { id: 'todo', name: 'Todo', category: 'unstarted' },
  },
  {
    id: '14',
    key: 'APP-14',
    title: 'Add import to settings',
    status: { id: 'todo', name: 'Todo', category: 'unstarted' },
  },
  {
    id: '15',
    key: 'APP-15',
    title: 'Import from spreadsheets',
    status: { id: 'backlog', name: 'Backlog', category: 'backlog' },
  },
];

const DATA_MODEL: DependencyItem = ITEMS[0] ?? { id: '11', title: 'Agree the data model' };
const SETTINGS: DependencyItem = ITEMS[3] ?? { id: '14', title: 'Add import to settings' };

function DependenciesDemo() {
  const [links, setLinks] = useState<DependencyLink[]>([
    { id: 'a', relation: 'blocked-by', item: DATA_MODEL },
    { id: 'b', relation: 'blocks', item: SETTINGS },
  ]);
  return (
    <div style={{ width: '100%', maxWidth: 480 }}>
      <DependencyPicker
        value={links}
        items={ITEMS}
        excludeIds={['12']}
        onAdd={(relation, item) =>
          setLinks((l) => [...l, { id: `${relation}-${item.id}`, relation, item }])
        }
        onRemove={(link) => setLinks((l) => l.filter((x) => x.id !== link.id))}
      />
    </div>
  );
}

export default {
  id: 'dependency-picker',
  title: 'DependencyPicker',
  summary:
    'Lists and adds blocking, blocked-by, related and duplicate links between items, with search.',
  sections: {
    purpose: {
      summary:
        'Shows what an issue waits on and what waits on it, and lets people add or remove those links by searching for the other item. Needed by: Rook Stride (`components/issues/IssueDetail.tsx` `Relations`, `REL_LABEL`; relation types `BLOCKS`, `RELATES`, `DUPLICATES` with direction), GanttTimeline dependencies and Rook Tickets linked tickets.',
      useWhen: ['An issue or task detail shows and edits its relations to other items.'],
      avoidWhen: [
        'Parent and sub-task hierarchy: use a tree or sub-issue list.',
        'Drawing dependencies on a timeline: use GanttTimeline, and this picker in its detail panel.',
      ],
    },
    designPrinciples: {
      items: [
        'The direction is always written out: “Blocked by” and “Blocks” are separate groups, never an arrow colour.',
        'Pick the relation first, then the item: one select and one APG combobox (Monarch Combobox), both keyboard-complete.',
        'Items already linked, and the item itself (`excludeIds`), are never offered.',
        'The picker never saves: `onAdd` and `onRemove` hand the change to the product.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Search locally with `items`, or on a server with `onSearch(query, { signal })` (debounced; superseded queries are aborted by Combobox).',
        '`DEPENDENCY_RELATIONS` lists the relations in display order.',
        'The ref is the root `<div>`.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Migrating from Stride relations: `{ type: "BLOCKS", direction: "outgoing" }` → `blocks`, `incoming` → `blocked-by`; `RELATES` → `relates`; `DUPLICATES` outgoing → `duplicates`, incoming → `duplicated-by`. `r.issue.identifier` → `item.key`, the state → `item.status`; call `strideApi.removeRelation` from `onRemove`.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'relations',
          values: ['blocks', 'blocked-by', 'relates', 'duplicates', 'duplicated-by'],
          description: 'Offer a subset, in your order.',
        },
        {
          prop: 'readOnly',
          values: ['true', 'false'],
          description: 'Only the grouped list, without add and remove.',
        },
      ],
    },
    states: {
      items: [
        { name: 'Empty', description: '“No linked items.” above the add controls.' },
        {
          name: 'Disabled',
          attribute: 'data-disabled',
          description: 'Select, search and remove buttons are disabled.',
        },
        {
          name: 'Searching',
          description: 'Combobox shows loading, empty and error states with retry.',
        },
      ],
    },
    interaction: {
      items: [
        'Choose a relation, type to search, and pick an item: `onAdd(relation, item)` runs, the search clears and keeps focus for the next one.',
        'Each link has a remove button.',
        'Additions and removals are announced.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Tab',
          action: 'Moves through item links, remove buttons, the relation select and the search.',
        },
        {
          keys: 'Arrow Up / Down',
          action: 'Change the relation (select) or move through results (search).',
        },
        { keys: 'Enter', action: 'Link the highlighted result.' },
        { keys: 'Escape', action: 'Close the results.' },
      ],
    },
    accessibility: {
      role: 'region per relation group; native select; combobox with listbox',
      name: 'Groups are named by their heading (“Blocked by”); the search is “Find an item to link”.',
      items: [
        'Linked items’ statuses are spoken (“Status: Done”) next to the glyph.',
        'Remove buttons name the relation and the item.',
        'Changes are announced in a `role="status"` region.',
      ],
    },
    responsive: {
      items: [
        'Rows truncate long titles; the search wraps under the relation select at 390px.',
        'Rows, the select and remove buttons are 44px on coarse pointers.',
      ],
    },
    theme: {
      items: [
        'Headings `--mn-color-text-muted`; row hover `--mn-color-bg-subtle`; links `--mn-color-text-link`.',
        'Forced colours: the select border is `ButtonText`.',
      ],
    },
    examples: {
      items: [{ title: 'Issue dependencies', stack: true, render: () => <DependenciesDemo /> }],
    },
    doDont: {
      do: [
        'Exclude the current item with `excludeIds`.',
        'Warn in the product when a new link would create a cycle.',
      ],
      dont: [
        'Do not show direction only with arrows or colour.',
        'Do not remove a blocking link silently; announce or offer undo.',
      ],
    },
    related: {
      items: [
        { id: 'issue-card', reason: 'The items being linked.' },
        { id: 'combobox', reason: 'The search it is built on.' },
        { id: 'gantt-timeline', reason: 'Draws the dependencies on a timeline.' },
        { id: 'status-picker', reason: 'Exports the status glyph.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['button', 'combobox', 'status-picker'],
    },
    testing: {
      unit: [
        'Relation groups with headings, spoken status, safe links, read-only without add controls.',
        'Add with the chosen relation from the keyboard; linked and excluded items not offered; announcement; search cleared and refocused.',
        'Remove with a named button and announcement.',
        'Server search with exclusions; limited relations, translations, disabled; axe; server rendering.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
