import { useState } from 'react';
import { Badge, IconButton, TreeView, type TreeViewItem } from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const UNITS: TreeViewItem[] = [
  {
    id: 'ops',
    label: 'Operations',
    description: 'Business unit · 42 people',
    children: [
      { id: 'finance', label: 'Finance', description: 'Department · 12 people', children: [] },
      { id: 'legal', label: 'Legal', description: 'Department · 4 people' },
      {
        id: 'facilities',
        label: 'Facilities',
        description: 'Division · 26 people',
        children: [
          { id: 'workplace', label: 'Workplace', description: 'Department · 18 people' },
          { id: 'security', label: 'Security', description: 'Department · 8 people' },
        ],
      },
    ],
  },
  {
    id: 'product',
    label: 'Product',
    description: 'Business unit · 64 people',
    children: [
      { id: 'design', label: 'Design', description: 'Department · 14 people' },
      { id: 'eng', label: 'Engineering', description: 'Department · 50 people' },
    ],
  },
];

function Departments() {
  const [selected, setSelected] = useState<string[]>(['eng']);
  return (
    <div style={{ display: 'grid', gap: 8 }}>
      <TreeView
        label="Departments"
        items={UNITS}
        defaultExpanded={['ops', 'product']}
        selected={selected}
        onSelectedChange={setSelected}
        renderActions={(item, { tabIndex }) => (
          <IconButton
            size="xs"
            label={`Edit ${item.label}`}
            tabIndex={tabIndex}
            icon={<span aria-hidden>✎</span>}
            onClick={() => {}}
          />
        )}
      />
      <p style={{ margin: 0 }}>Selected: {selected.join(', ') || 'none'}</p>
    </div>
  );
}

const GOALS: TreeViewItem[] = [
  {
    id: 'o1',
    label: 'Make onboarding faster',
    meta: <Badge tone="success">On track</Badge>,
    children: [
      { id: 'k1', label: 'Time to first value under 2 days', meta: '70%' },
      { id: 'k2', label: 'Setup completion above 80%', meta: '55%' },
    ],
  },
  {
    id: 'o2',
    label: 'Improve support quality',
    meta: <Badge tone="warning">At risk</Badge>,
    children: [{ id: 'k3', label: 'Satisfaction above 4.5', meta: '40%' }],
  },
];

const OUTLINE: TreeViewItem[] = [
  {
    id: 'h-intro',
    label: 'Introduction',
    children: [
      { id: 'h-scope', label: 'Scope' },
      { id: 'h-terms', label: 'Terms' },
    ],
  },
  {
    id: 'h-plan',
    label: 'Plan',
    children: [
      { id: 'h-phase1', label: 'Phase one' },
      { id: 'h-phase2', label: 'Phase two' },
    ],
  },
  { id: 'h-risks', label: 'Risks' },
];

function Outline() {
  const [current, setCurrent] = useState('h-scope');
  return (
    <TreeView
      label="Document outline"
      variant="outline"
      selectionMode="none"
      defaultExpanded="all"
      items={OUTLINE}
      current={current}
      onActivate={(item) => setCurrent(item.id)}
    />
  );
}

function Lazy() {
  const [items, setItems] = useState<TreeViewItem[]>([
    { id: 'root', label: 'Shared folders', hasChildren: true },
  ]);
  return (
    <TreeView
      label="Folders"
      items={items}
      onLoadChildren={() =>
        new Promise<void>((resolve) =>
          setTimeout(() => {
            setItems([
              {
                id: 'root',
                label: 'Shared folders',
                children: [
                  { id: 'a', label: 'Contracts' },
                  { id: 'b', label: 'Templates' },
                ],
              },
            ]);
            resolve();
          }, 700),
        )
      }
    />
  );
}

export default {
  id: 'tree-view',
  title: 'TreeView',
  summary: 'A keyboard-complete hierarchy: departments, goals, document outlines.',
  sections: {
    purpose: {
      summary:
        'Shows nested items people expand, collapse, browse and pick from, with the full WAI-ARIA tree keyboard model. Needed by: People (department tree, goal tree), Dossier (document outline).',
      useWhen: [
        'Data has real parent–child structure of any depth (units, goals and key results, headings).',
        'People need to move through the hierarchy by keyboard and pick one or several items.',
      ],
      avoidWhen: [
        'Only two levels where every parent is always open: use a grouped list.',
        'A reporting-line diagram: use OrgChart.',
        'Navigation between pages of an app: use the app shell navigation.',
      ],
    },
    designPrinciples: {
      items: [
        'Data in, callbacks out: the tree never fetches; `onLoadChildren` lets the product load a branch.',
        'One tab stop for the whole tree, plus the focused row’s actions, so long trees are not a wall of tab stops.',
        'Selection, activation and “current” are separate ideas: a picker selects, an outline activates and marks the heading in view.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Renders `<div role="tree">` (ref) with `role="treeitem"` rows and `role="group"` children, followed by a visually hidden status region.',
        'Items are data (`TreeViewItem`): `id`, `label`, `description`, `icon`, `meta`, `children`, `hasChildren`, `disabled`.',
        '`renderItem` replaces the label and description; `renderActions` adds controls and receives the `tabIndex` to put on them.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Migrating from People `DepartmentTree`: build `items` from the department list (`children` by `parentId`, sorted by name); `label` = name, `description` = kind, head and headcount, `meta` = the Archived pill; the collapsed set becomes `expanded` / `onExpandedChange` (inverse); the row menu moves into `renderActions` (pass `tabIndex` to the menu trigger). Dialogs for edit, merge, split and archive stay in People.',
        'Migrating from People `GoalTree`: objectives are top-level items with key results as `children`; status Badge and progress go in `meta` or `renderItem`; History / Check in / the row menu go in `renderActions`; the per-row `open` state becomes `defaultExpanded="all"`.',
        'Migrating from Dossier `EditorOutlinePane`: nest headings by level into `items`, use `variant="outline"`, `selectionMode="none"`, `current={activeHeadingId}` and `onActivate={(h) => goTo(h)}`. The pane chrome (collapse button, reading stats) stays in Dossier.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'variant',
          values: ['default', 'outline'],
          description:
            '`default` rows with room for a description; `outline` compact rows with indentation guides (`data-variant`).',
        },
        {
          prop: 'selectionMode',
          values: ['single', 'multiple', 'none'],
          description:
            '`single` (default) picks one item; `multiple` sets `aria-multiselectable`; `none` for navigation trees.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Expanded / collapsed',
          attribute: 'aria-expanded',
          description: 'The chevron points down or across.',
        },
        {
          name: 'Selected',
          attribute: 'aria-selected, data-selected',
          description: 'Accent background and a leading accent bar.',
        },
        {
          name: 'Current',
          attribute: 'aria-current="true", data-current',
          description: 'Accent, bold label (the heading in view).',
        },
        { name: 'Focused', description: 'Focus ring on the row on `:focus-visible`.' },
        {
          name: 'Disabled',
          attribute: 'aria-disabled, data-disabled',
          description: 'Disabled text; skipped by arrow keys.',
        },
        {
          name: 'Loading children',
          attribute: 'aria-busy',
          description: 'A spinner replaces the chevron; “Loading” is announced.',
        },
      ],
    },
    interaction: {
      items: [
        'Clicking a row focuses, selects and activates it; clicking the chevron only expands or collapses.',
        'In multiple mode, Ctrl/⌘-click toggles one item.',
        'Expanding an item with `hasChildren` and no `children` calls `onLoadChildren`; a rejected promise collapses it again and announces the error.',
        'If the focused item is hidden by a collapse, its nearest visible ancestor takes the tab stop and focus.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'ArrowDown / ArrowUp',
          action: 'Next / previous visible item (disabled items are skipped).',
        },
        {
          keys: 'ArrowRight',
          action: 'Expands a closed parent; on an open parent, moves to its first child.',
        },
        { keys: 'ArrowLeft', action: 'Collapses an open parent; otherwise moves to the parent.' },
        { keys: 'Home / End', action: 'First / last visible item.' },
        { keys: 'Enter', action: 'Selects (single) or toggles (multiple) and calls `onActivate`.' },
        { keys: 'Space', action: 'Selects or toggles; activates when `selectionMode="none"`.' },
        { keys: '*', action: 'Expands every sibling of the focused item.' },
        {
          keys: 'Shift+ArrowDown / Shift+ArrowUp',
          action: 'Multiple mode: moves and adds the item to the selection.',
        },
        { keys: 'Ctrl+A / ⌘A', action: 'Multiple mode: selects every visible item.' },
        {
          keys: 'Letters',
          action: 'Typeahead: moves to the next item whose label starts with what was typed.',
        },
        { keys: 'Tab', action: 'From the focused row to its actions, then out of the tree.' },
      ],
    },
    accessibility: {
      role: 'tree, treeitem, group',
      name: '`label` (or `aria-labelledby`) for the tree; each item is named by its label only and described by its description.',
      items: [
        '`aria-level`, `aria-setsize` and `aria-posinset` are set on every item, so position is announced even with lazy children.',
        'Roving tabindex: exactly one item has `tabindex="0"`.',
        'Action controls are excluded from the item name and only the focused row’s actions are tabbable; arrow keys pressed on an action are left to the action.',
        'Loading and load errors are announced in a polite status region.',
      ],
    },
    responsive: {
      items: [
        'Labels and descriptions wrap instead of overflowing; indentation shrinks below 480 px so deep trees still fit at 390 px.',
        'On touch screens rows and action buttons are at least 44 px.',
      ],
    },
    theme: {
      items: [
        'Selection uses `--mn-color-accent-subtle` and `--mn-color-accent-solid`; hover `--mn-color-bg-subtle`; guides `--mn-color-border-subtle`.',
        'Forced colours: selection is a Highlight outline, focus a Highlight ring, disabled GrayText.',
        'Chevron and hover transitions stop when reduced motion is requested.',
      ],
    },
    examples: {
      items: [
        { title: 'Departments with actions', stack: true, render: () => <Departments /> },
        {
          title: 'Goals and key results',
          stack: true,
          render: () => (
            <TreeView label="Goals" items={GOALS} defaultExpanded="all" selectionMode="none" />
          ),
        },
        { title: 'Document outline', stack: true, render: () => <Outline /> },
        { title: 'Lazy children', stack: true, render: () => <Lazy /> },
      ],
    },
    doDont: {
      do: [
        'Give every item a short plain-text `label`; put counts and status in `description` or `meta`.',
        'Pass `tabIndex` from `renderActions` to every control you render.',
        'Use `current` for “where am I” and `selected` for “what did I pick”.',
      ],
      dont: [
        'Do not put links or buttons in `renderItem`; use `renderActions`.',
        'Do not nest a TreeView inside another interactive widget.',
        'Do not use expand state to hide required information.',
      ],
    },
    related: {
      items: [
        { id: 'org-chart', reason: 'Reporting lines as a diagram.' },
        { id: 'goal-tree', reason: 'Goals with progress built on the same data.' },
        { id: 'page-tree', reason: 'Document pages with drag to reorder.' },
        { id: 'accordion', reason: 'Collapsible sections of content, not a hierarchy.' },
      ],
    },
    dependencies: { packages: ['react'], components: ['spinner'] },
    testing: {
      unit: [
        'Tree, treeitem and group roles with level, set size, position, expanded, disabled and descriptions.',
        'Arrow keys, Home and End move, expand and collapse; disabled items are skipped; one tab stop.',
        '`*` expands siblings; typeahead with repeated letters and longer strings.',
        'Single selection and activation by Enter and click; chevron click only toggles.',
        'Multiple selection with Space, Shift+Arrow and Ctrl+A.',
        'Outline mode: no selection, `aria-current`, Space activates.',
        'Lazy children load; failures collapse and are announced.',
        'Row actions: only the focused row’s are tabbable; arrow keys on an action are ignored by the tree.',
        'A controlled collapse moves the tab stop to the visible ancestor.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
