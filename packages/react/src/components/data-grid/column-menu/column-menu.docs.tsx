import {
  ColumnMenu,
  DataTable,
  useDataTableColumns,
  type DataTableColumn,
} from '@rookcom/monarch-react/data';
import type { ComponentDocs } from '../../../internal/component-docs.js';

interface Row {
  id: string;
  name: string;
  status: string;
  owner: string;
  expires: string;
}

const ROWS: Row[] = [
  { id: '1', name: 'example.com', status: 'Active', owner: 'Sam Lee', expires: '2027-02-01' },
  { id: '2', name: 'example.org', status: 'Expiring', owner: 'Ana Ruiz', expires: '2026-12-01' },
];

const COLUMNS: DataTableColumn<Row>[] = [
  { id: 'name', header: 'Name', hideable: false },
  { id: 'status', header: 'Status' },
  { id: 'owner', header: 'Owner', defaultHidden: true },
  { id: 'expires', header: 'Expires' },
];

function Toolbar() {
  const cols = useDataTableColumns(COLUMNS);
  return (
    <div style={{ display: 'grid', gap: 12 }}>
      <div style={{ display: 'flex', justifyContent: 'flex-end' }}>
        <ColumnMenu {...cols.chooserProps} showAll />
      </div>
      <DataTable
        caption="Records"
        columns={COLUMNS}
        rows={ROWS}
        getRowId={(r) => r.id}
        {...cols.tableProps}
      />
    </div>
  );
}

export default {
  id: 'column-menu',
  title: 'ColumnMenu',
  summary: 'A “Columns” dropdown with one checkable item per column.',
  sections: {
    purpose: {
      summary:
        'The quickest way to show and hide table columns from a toolbar: a menu of checkable items that stays open while people toggle several. It takes DataTable’s column state, so it fills the DataTable gap for a menu-style chooser. Needed by: Atlas (domain table).',
      useWhen: [
        'A table toolbar needs a compact show/hide control.',
        'The column set is short enough to scan in a menu (about 15 or fewer).',
      ],
      avoidWhen: [
        'People also reorder or search columns: use ColumnChooser, or DataTableColumnChooser (a popover with reordering).',
        'Filtering rows: use FilterBar.',
      ],
    },
    designPrinciples: {
      items: [
        'Menu semantics (`menuitemcheckbox`): arrow keys, typeahead and Escape come from DropdownMenu.',
        'Toggling does not close the menu, so several columns can be changed at once.',
        'The trigger’s name includes the count (“Columns, 5 of 8 shown”).',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Renders a DropdownMenu; the ref goes to the trigger `<button>`.',
        'Spread `useDataTableColumns(columns).chooserProps`; `onOrderChange` from the spread is ignored (the menu only lists columns in `order`).',
        '`orderColumns(columns, order)` is exported: the ordering rule the menu and DataTable share.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Migrating from the app copy (Atlas `ColumnMenu`): `columns: DomainColumn[]` (visible ids) → `visibility` (a record by id; convert with `Object.fromEntries(ALL.map((c) => [c, visible.includes(c)]))`); `onChange` → `onVisibilityChange`; the always-on `domain` column → `hideable: false`; the reset item → `onReset={() => setColumns(DEFAULT)}`. Labels come from `columns[].label` instead of `t("column.*")`. `useDomainColumns` (localStorage) stays in Atlas.',
      ],
    },
    variants: {
      items: [
        { prop: 'size', values: ['xs', 'sm', 'md'], description: 'Trigger size. Default `sm`.' },
        {
          prop: 'showAll',
          values: ['false', 'true'],
          description: 'Adds “Show all columns” while any column is hidden.',
        },
        {
          prop: 'align',
          values: ['start', 'center', 'end'],
          description: 'Menu alignment. Default `end`.',
        },
      ],
    },
    states: {
      items: [
        { name: 'Shown', attribute: 'aria-checked="true"', description: 'Check mark.' },
        { name: 'Hidden', attribute: 'aria-checked="false"', description: 'No check mark.' },
        {
          name: 'Locked',
          attribute: 'aria-disabled="true"',
          description: '`hideable: false`, or one of the last `minVisible` visible columns.',
        },
        {
          name: 'Open',
          attribute: 'data-state="open" (trigger)',
          description: 'The menu is showing.',
        },
      ],
    },
    interaction: {
      items: [
        'Selecting a column toggles it and keeps the menu open.',
        '“Show all columns” shows every column and keeps the menu open; “Reset columns” calls `onReset` and closes.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Enter / Space / ArrowDown', action: 'Opens the menu from the trigger.' },
        { keys: 'ArrowUp / ArrowDown', action: 'Moves between columns.' },
        { keys: 'Enter / Space', action: 'Toggles the focused column.' },
        { keys: 'Type a letter', action: 'Jumps to the column starting with it.' },
        { keys: 'Escape', action: 'Closes and returns focus to the trigger.' },
      ],
    },
    accessibility: {
      role: 'button (trigger) and menu with menuitemcheckbox items',
      name: '“Columns, N of M shown” for the trigger; each item by its column label.',
      items: [
        'The menu is labelled by its “Visible columns” heading.',
        'Locked columns are `aria-disabled` and skipped by arrow keys.',
      ],
    },
    responsive: {
      items: [
        'The menu scrolls when it is taller than the screen.',
        'On touch screens items are at least 44 px tall.',
      ],
    },
    theme: {
      items: [
        'Uses DropdownMenu’s surface, item and check styles, so it follows every theme.',
        'Forced colours: a CanvasText border around the menu.',
      ],
    },
    examples: {
      items: [{ title: 'With DataTable', stack: true, render: () => <Toolbar /> }],
    },
    doDont: {
      do: [
        'Mark the identifying column `hideable: false`.',
        'Use the same hook state for the menu and the table.',
      ],
      dont: [
        'Do not put more than about 15 columns in a menu; use ColumnChooser with search.',
        'Do not close the menu after each toggle.',
      ],
    },
    related: {
      items: [
        { id: 'column-chooser', reason: 'Inline panel with reordering and search.' },
        {
          id: 'data-table',
          reason: 'The table it controls; DataTableColumnChooser is the popover version.',
        },
        { id: 'dropdown-menu', reason: 'The menu it is built on.' },
      ],
    },
    dependencies: { packages: ['react'], components: ['button', 'dropdown-menu'] },
    testing: {
      unit: [
        'Opens by keyboard, lists columns in order, toggles with Enter and Space while staying open, Escape returns focus.',
        'Keeps the last visible column.',
        'Show all and Reset.',
        'Drives a DataTable through `useDataTableColumns`.',
        '`orderColumns` ordering.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
