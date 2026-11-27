import { useState } from 'react';
import { Button } from '@rookcom/monarch-react';
import {
  DataTable,
  TableToolbar,
  useDataTableColumns,
  type DataTableColumn,
} from '@rookcom/monarch-react/data';
import type { ComponentDocs } from '../../../internal/component-docs.js';
// The scenario of the primary example, shared with iOS and Android (CROSS-PLATFORM.md section 8).
import fixture from './table-toolbar.fixture.json';

interface Row {
  id: string;
  name: string;
  status: string;
  owner: string;
}

const ROWS: Row[] = fixture.data.rows.map((row, i) => ({ id: String(i + 1), ...row }));

const [NAME, STATUS, OWNER] = fixture.data.columns as [string, string, string];
const COLUMNS: DataTableColumn<Row>[] = [
  { id: 'name', header: NAME, hideable: false },
  { id: 'status', header: STATUS },
  { id: 'owner', header: OWNER },
];

function Demo() {
  const cols = useDataTableColumns(COLUMNS);
  const [query, setQuery] = useState('');
  const [view, setView] = useState('table');
  const [selected, setSelected] = useState<string[]>([]);
  const rows = ROWS.filter((r) => r.name.includes(query.trim().toLowerCase()));
  return (
    <div style={{ display: 'grid', gap: 12 }}>
      <TableToolbar
        search={{ value: query, onValueChange: setQuery }}
        filters={
          <Button size="sm" variant="secondary">
            {fixture.data.filter}
          </Button>
        }
        resultCount={rows.length}
        view={{ value: view, onValueChange: setView, views: fixture.data.views }}
        columns={{ ...cols.chooserProps, showAll: true }}
        actions={<Button size="sm">{fixture.data.action}</Button>}
        selectedCount={selected.length}
        totalCount={rows.length}
        onClearSelection={() => setSelected([])}
        bulkActions={[{ id: 'renew', label: 'Renew', onSelect: () => setSelected([]) }]}
      />
      <DataTable
        caption={fixture.data.caption}
        columns={COLUMNS}
        rows={rows}
        getRowId={(r) => r.id}
        selectable
        selectedRowIds={selected}
        onSelectedRowIdsChange={setSelected}
        {...cols.tableProps}
      />
    </div>
  );
}

export default {
  id: 'table-toolbar',
  title: 'TableToolbar',
  summary: 'Search, filters, view switcher, column menu and bulk selection above a table.',
  sections: {
    purpose: {
      summary:
        'Every data screen puts the same controls above its table in a slightly different order. TableToolbar fixes the layout: search and filters at the start, the result count, view switcher, column menu and actions at the end, and the bulk selection area taking over while rows are selected. It composes SearchInput, ViewSwitcher, ColumnMenu and BulkActionBar. Needed by: Atlas (domain table toolbar with ColumnMenu), Tickets (inbox list header with search and bulk bar).',
      useWhen: [
        'A DataTable or DataGrid needs search, filters, columns or bulk actions.',
        'Any list screen that switches between table, board and other views.',
      ],
      avoidWhen: [
        'Complex filter building: put a FilterBar or QueryBuilder in `filters`.',
        'A page header with a title and primary action only: use SectionHeader.',
      ],
    },
    designPrinciples: {
      items: [
        'Pieces are optional: it renders only what it is given.',
        'Selection mode replaces search and filters instead of stacking another bar.',
        'Wraps instead of scrolling: search takes the full width on phones.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        '`search` takes SearchInput props, `view` ViewSwitcher props, `columns` ColumnMenu props, `bulkActions` BulkActionBar actions.',
        'The ref goes to the root group `<div>`.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'The bulk area appears only when `onClearSelection` is set; it is open while `selectedCount > 0`.',
        'Migrating from shadcn/ui table toolbars (`DataTableToolbar` with an `Input` filter, faceted filter buttons and `DataTableViewOptions`): the `Input` → `search`; faceted filters → `filters`; `DataTableViewOptions` → `columns={useDataTableColumns(columns).chooserProps}`; a separate selected-rows bar → `selectedCount`, `onClearSelection`, `bulkActions`.',
      ],
    },
    variants: {
      items: [{ prop: 'size', values: ['sm', 'md'], description: 'Control size. Default `sm`.' }],
    },
    states: {
      items: [
        {
          name: 'Selecting',
          attribute: 'data-selecting',
          description: 'Rows are selected: the bulk area replaces search and filters.',
        },
      ],
    },
    interaction: {
      items: [
        'Typing in search calls `onValueChange` (and `onSearch` after `debounce`).',
        '“Clear selection” calls `onClearSelection` and moves focus to `returnFocusRef` or the bulk area.',
        'Search keeps its text while hidden during selection.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Tab',
          action: 'Moves through search, filters, view switcher, columns and actions.',
        },
        {
          keys: 'Arrow keys',
          action: 'Inside the view switcher and bulk bar (one tab stop each).',
        },
        { keys: 'Escape', action: 'Clears the search field.' },
      ],
    },
    accessibility: {
      role: 'group',
      name: '“Table controls” by default (`aria-label` overrides it).',
      items: [
        'The result count is a polite status, announced as filters change.',
        'Selection changes are announced by BulkActionBar.',
        'It is a group, not a toolbar, because it holds a text field and composite widgets with their own arrow keys.',
      ],
    },
    responsive: {
      items: [
        'Below 30rem the search takes the full row and the end controls wrap under it.',
        'At 390 px nothing scrolls sideways; view labels collapse to icons.',
      ],
    },
    theme: {
      items: [
        'Only layout and the muted count are styled; controls bring their own styles.',
        'Forced colours: the count uses CanvasText.',
      ],
    },
    examples: {
      items: [{ title: 'With DataTable and selection', stack: true, render: () => <Demo /> }],
    },
    doDont: {
      do: [
        'Use the same column state for the toolbar and the table (`useDataTableColumns`).',
        'Show the result count when filters can hide rows.',
      ],
      dont: [
        'Do not put more than one primary action in `actions`.',
        'Do not add a second bulk bar under the table.',
      ],
    },
    related: {
      items: [
        { id: 'data-table', reason: 'The table it usually sits above.' },
        { id: 'data-grid', reason: 'The editable, virtualized grid.' },
        { id: 'bulk-action-bar', reason: 'The selection area.' },
        { id: 'column-menu', reason: 'The columns control.' },
        { id: 'view-switcher', reason: 'The view control.' },
        { id: 'filter-bar', reason: 'For the filters slot.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['bulk-action-bar', 'column-menu', 'search-input', 'view-switcher'],
    },
    testing: {
      unit: [
        'Composes search, filters, count, view switcher, column menu and actions in tab order.',
        'Bulk area replaces search and filters while selecting; clearing restores them.',
        'Renders only given parts; messages.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
