import {
  ColumnChooser,
  DataTable,
  useDataTableColumns,
  type DataTableColumn,
} from '@rookcom/monarch-react/data';
import type { ComponentDocs } from '../../../internal/component-docs.js';

interface Record_ {
  id: string;
  name: string;
  status: string;
  owner: string;
  region: string;
  renews: string;
}

const ROWS: Record_[] = [
  {
    id: '1',
    name: 'Alpha',
    status: 'Active',
    owner: 'Sam Lee',
    region: 'EU',
    renews: '2027-02-01',
  },
  {
    id: '2',
    name: 'Bravo',
    status: 'Paused',
    owner: 'Ana Ruiz',
    region: 'US',
    renews: '2026-11-30',
  },
  {
    id: '3',
    name: 'Charlie',
    status: 'Active',
    owner: 'Kim Park',
    region: 'APAC',
    renews: '2027-06-15',
  },
];

const COLUMNS: DataTableColumn<Record_>[] = [
  { id: 'name', header: 'Name', hideable: false },
  { id: 'status', header: 'Status' },
  { id: 'owner', header: 'Owner' },
  { id: 'region', header: 'Region', defaultHidden: true },
  { id: 'renews', header: 'Renews' },
];

function WithTable() {
  const cols = useDataTableColumns(COLUMNS);
  return (
    <div
      style={{
        display: 'grid',
        gap: 16,
        gridTemplateColumns: 'repeat(auto-fit, minmax(14rem, 1fr))',
      }}
    >
      <ColumnChooser {...cols.chooserProps} />
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

const MANY = Array.from({ length: 12 }, (_, i) => ({ id: `c${i}`, label: `Field ${i + 1}` }));

function Searchable() {
  const cols = useDataTableColumns(MANY.map((c) => ({ id: c.id, header: c.label })));
  return <ColumnChooser {...cols.chooserProps} style={{ maxWidth: 360 }} />;
}

export default {
  id: 'column-chooser',
  title: 'ColumnChooser',
  summary: 'An inline panel to show, hide, find and reorder table columns.',
  sections: {
    purpose: {
      summary:
        'Lets people pick which columns a table shows and in what order, in a panel that sits in a Sheet, a settings page or a saved-view editor. It shares its state with DataTable through `useDataTableColumns`. Needed by: Atlas (domain table columns and saved views).',
      useWhen: [
        'Tables with many optional columns, where people also reorder or search columns.',
        'A saved-view or table settings screen that edits the column set.',
      ],
      avoidWhen: [
        'A quick show/hide toggle from the toolbar: use ColumnMenu or DataTableColumnChooser.',
        'Fewer than four columns: show them all.',
      ],
    },
    designPrinciples: {
      items: [
        'Same state shape as DataTable (`visibility`, `order`), so one hook drives the table, the menu and the panel.',
        'Reordering with buttons, not drag and drop, so it works by keyboard, switch and screen reader.',
        'A table is never empty: the last `minVisible` columns cannot be hidden.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Renders a `<div role="group">` (ref) named by its title and described by the count. Other div attributes pass through.',
        'Spread `useDataTableColumns(columns).chooserProps` to wire it to a DataTable; `columnChooserColumns(columns)` converts column definitions by hand.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Migrating from the app copy (Atlas `ColumnMenu` + `useDomainColumns`): Atlas stored a list of visible column ids; convert it with `Object.fromEntries(all.map((c) => [c, visible.includes(c)]))` for `visibility`, and back with `Object.keys(v).filter((k) => v[k])`. `disabled={c === "domain"}` → `hideable: false` on that column; Reset → `onReset`. Keep the localStorage persistence in Atlas.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'onOrderChange',
          values: ['undefined', 'function'],
          description: 'With it, each row gets Move up and Move down buttons.',
        },
        {
          prop: 'searchThreshold',
          values: ['8 (default)', 'number', 'Infinity'],
          description: 'A search box appears when there are at least this many columns.',
        },
      ],
    },
    states: {
      items: [
        { name: 'Shown', description: 'Checked checkbox.' },
        {
          name: 'Hidden',
          attribute: 'data-hidden (on the row)',
          description: 'Unchecked checkbox.',
        },
        {
          name: 'Always shown',
          description: '`hideable: false`: a disabled, checked checkbox with “Always shown”.',
        },
        {
          name: 'Last visible',
          description: 'The last visible columns (`minVisible`) cannot be unchecked.',
        },
        {
          name: 'Filtering',
          description: 'Only matching columns are listed and the move buttons are hidden.',
        },
        { name: 'No matches', description: '“No columns match.”' },
      ],
    },
    interaction: {
      items: [
        'Checking a box calls `onVisibilityChange` with the whole new record.',
        'Move up / Move down swap a column with its neighbour and call `onOrderChange` with every id in the new order.',
        'Show all appears while any column is hidden; Reset calls `onReset`.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Tab / Shift+Tab',
          action: 'Moves through the search box, checkboxes, move buttons and footer.',
        },
        { keys: 'Space', action: 'Toggles a column.' },
        {
          keys: 'Enter / Space (move buttons)',
          action:
            'Moves the column; focus stays on the button, or moves to its partner when the column reaches an end.',
        },
      ],
    },
    accessibility: {
      role: 'group',
      name: 'The title (“Columns”), described by “5 of 8 shown”.',
      items: [
        'Moves are announced in a polite status region (“Status moved to position 3 of 8.”).',
        'Each move button names its column (“Move Status up”).',
        'Columns that cannot be hidden say so in their description, not only by being disabled.',
      ],
    },
    responsive: {
      items: [
        'Fills its container; labels wrap and the move buttons stay at the end of the row.',
        'On touch screens rows, move buttons and footer buttons are at least 44 px.',
      ],
    },
    theme: {
      items: [
        'Row separators `--mn-color-border-subtle`; count and empty text `--mn-color-text-muted`.',
        'Forced colours: CanvasText separators and text.',
      ],
    },
    examples: {
      items: [
        { title: 'Next to a DataTable', stack: true, render: () => <WithTable /> },
        { title: 'Many columns with search', render: () => <Searchable /> },
      ],
    },
    doDont: {
      do: [
        'Mark the identifying column `hideable: false`.',
        'Persist `visibility` and `order` (saved view, local storage) if the choice should last.',
      ],
      dont: [
        'Do not put it inside a menu; use ColumnMenu there.',
        'Do not rely on drag and drop for ordering.',
      ],
    },
    related: {
      items: [
        { id: 'column-menu', reason: 'A compact “Columns” dropdown for the toolbar.' },
        {
          id: 'data-table',
          reason: 'The table it controls, and DataTableColumnChooser (a popover).',
        },
        { id: 'saved-views', reason: 'Saving a column set as a named view.' },
        { id: 'sheet', reason: 'A side panel to host it.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['button', 'checkbox', 'search-input'],
    },
    testing: {
      unit: [
        'Named group with a count; toggles columns; required columns are disabled.',
        'Reorders by keyboard, keeps focus (or moves it to the partner at an end) and announces.',
        'Search appears at the threshold, hides move buttons while filtering and shows no matches.',
        'Keeps the last visible column; Reset.',
        'Drives a DataTable through `useDataTableColumns` (order, visibility, reset).',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
