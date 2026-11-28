import { useState } from 'react';
import { DataGrid, type DataGridColumn } from '@rookcom/monarch-react/data';
import type { ComponentDocs } from '../../../internal/component-docs.js';
// The scenario of the primary example, shared with iOS and Android (CROSS-PLATFORM.md section 8).
import fixture from './data-grid.fixture.json';

interface Invoice {
  id: string;
  number: string;
  customer: string;
  status: string;
  amount: number;
  due: string;
}

const CUSTOMERS = fixture.data.customers;
const STATUSES = fixture.data.statuses;

const INVOICES: Invoice[] = Array.from({ length: fixture.data.rowCount }, (_, i) => ({
  id: `inv-${i}`,
  number: `INV-${String(i + 1).padStart(5, '0')}`,
  customer: CUSTOMERS[i % CUSTOMERS.length] ?? 'Example Ltd',
  status: STATUSES[i % STATUSES.length] ?? 'Draft',
  amount: ((i * 7919) % 50_000) / 100,
  due: `2026-${String((i % 12) + 1).padStart(2, '0')}-${String((i % 27) + 1).padStart(2, '0')}`,
}));

// What the columns do on the web only; the headers and widths are the fixture's.
const COLUMN_BEHAVIOUR: Record<string, Partial<DataGridColumn<Invoice>>> = {
  number: { sortable: true, hideable: false, pinned: 'start' },
  customer: { sortable: true, editable: true, required: true },
  status: {
    sortable: true,
    editable: true,
    editor: 'select',
    options: STATUSES.map((s) => ({ value: s, label: s })),
  },
  amount: {
    numeric: true,
    sortable: true,
    editable: true,
    min: 0,
    cell: (r) => r.amount.toFixed(2),
  },
  due: { sortable: true, editable: true, editor: 'date' },
};

const COLUMNS = fixture.data.columns.map((c) => ({
  id: c.id,
  header: c.header,
  width: c.width,
  ...COLUMN_BEHAVIOUR[c.id],
})) as DataGridColumn<Invoice>[];

function EditableGrid() {
  const [rows, setRows] = useState(INVOICES);
  const [selected, setSelected] = useState<string[]>([]);
  return (
    <DataGrid
      label={fixture.data.label}
      columns={COLUMNS}
      rows={rows}
      getRowId={(r) => r.id}
      getRowLabel={(r) => r.number}
      height={420}
      selectable
      selectedRowIds={selected}
      onSelectedRowIdsChange={setSelected}
      onCellEdit={(row, column, value) =>
        setRows((all) => all.map((r) => (r.id === row.id ? { ...r, [column]: value } : r)))
      }
      rowActions={(r) => [
        { id: 'open', label: fixture.data.rowActions[0] ?? '', onSelect: () => {} },
        {
          id: 'delete',
          label: fixture.data.rowActions[1] ?? '',
          tone: 'danger',
          onSelect: () => setRows((all) => all.filter((x) => x.id !== r.id)),
          confirm: { title: `Delete ${r.number}?`, confirmLabel: 'Delete invoice' },
        },
      ]}
    />
  );
}

export default {
  id: 'data-grid',
  title: 'DataGrid',
  summary:
    'A virtualized, editable WAI-ARIA grid: cell navigation, sortable, resizable and pinnable columns, selection.',
  sections: {
    purpose: {
      summary:
        'Spreadsheet-like screens need what a table cannot give: 10,000+ rows without slowing down, cell-by-cell keyboard movement, in-place editing, and columns people can resize, pin and hide. DataGrid does this with DataTable’s column model (`DataTableColumn`, `useDataTableColumns`, `sortRows`), so moving a screen from DataTable is mostly a component swap. Needed by: Tickets (inbox list virtualization, today hand-rolled in `components/inbox/InboxList.tsx`).',
      useWhen: [
        'Thousands of rows in one scroll area.',
        'People edit values in place, spreadsheet-style.',
        'Wide data where key columns must stay pinned.',
      ],
      avoidWhen: [
        'A paged list of records people open: DataTable (pagination, cards on phones, expandable rows).',
        'Fewer than a few hundred read-only rows: DataTable.',
        'A plain long list without columns: VirtualList.',
      ],
    },
    designPrinciples: {
      items: [
        'One tab stop; arrow keys move between cells (WAI-ARIA APG grid). Focus never gets lost: the focused row stays mounted while it scrolls out of view.',
        'Rows are virtualized but the semantics count all of them (`aria-rowcount`, `aria-rowindex`).',
        'Same column model and helpers as DataTable; RowActionsMenu, EditableCell and VirtualList are the building blocks.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        '`DataGridColumn<T>` extends `DataTableColumn<T>` with `pinned`, `pinnable`, `editable`, `editor`, `options`, `required`, `min`, `max` and `validate`.',
        'Columns are resizable unless `resizable: false` (DataTable’s default is the opposite).',
        'The ref goes to the root `<div>`; the grid is inside a scroll area of `height` (default 480).',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Spread `useDataTableColumns(columns).tableProps` for visibility and order, as with DataTable; pass `onColumnVisibilityChange` so “Hide column” updates your state.',
        'Migrating from DataTable: `caption` → `label`; `pageSize` and `layout="cards"` have no equivalent (the grid scrolls); `renderExpanded` is not supported; `rowActions` and `onRowOpen` keep their meaning.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'density',
          values: ['comfortable', 'compact'],
          description: 'Row padding and text size.',
        },
        {
          prop: 'columns[].editor',
          values: ['text', 'number', 'select', 'date'],
          description: 'The in-place editor of an editable column.',
        },
        {
          prop: 'columns[].pinned',
          values: ['start', 'end'],
          description: 'Keeps a column visible while scrolling sideways.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Selected row',
          attribute: 'aria-selected="true", data-selected',
          description: 'Accent background.',
        },
        {
          name: 'Sorted column',
          attribute: 'aria-sort',
          description: 'Arrow in the header; priority number for multi-sort.',
        },
        {
          name: 'Pinned column',
          attribute: 'data-pinned="start|end"',
          description: 'Sticky, with an edge line.',
        },
        {
          name: 'Editable cell',
          attribute: 'data-editable',
          description: 'Opens an editor with Enter, F2, typing or double-click.',
        },
        {
          name: 'Editing',
          attribute: 'data-editing',
          description: 'The editor is open in the cell.',
        },
        {
          name: 'Loading',
          attribute: 'data-busy, aria-busy',
          description: 'Skeleton without rows; a progress line with rows.',
        },
      ],
    },
    interaction: {
      items: [
        'Click a cell to focus it; click a sortable header to sort (Shift adds to the sort with `multiSort`).',
        'Drag a header edge to resize; double-click it to reset.',
        'The header “⋯” menu sorts, pins to start or end, unpins and hides the column.',
        'Double-click edits an editable cell or opens the row (`onRowOpen`).',
        'Shift+click a row checkbox selects the range.',
        '`onEndReached` fires when the last rows come into view.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'ArrowUp / ArrowDown / ArrowLeft / ArrowRight',
          action: 'Moves one cell (Left and Right follow RTL).',
        },
        { keys: 'Home / End', action: 'First or last cell of the row.' },
        { keys: 'Ctrl+Home / Ctrl+End', action: 'First header cell or last cell of the last row.' },
        { keys: 'PageUp / PageDown', action: 'Moves by a screen of rows.' },
        { keys: 'Enter (header)', action: 'Sorts the column; Shift+Enter adds it to the sort.' },
        { keys: 'Alt+ArrowDown, Shift+F10 (header)', action: 'Opens the column menu.' },
        {
          keys: 'Alt+ArrowLeft / Alt+ArrowRight (header)',
          action: 'Resizes the column by 16 px (64 with Shift).',
        },
        {
          keys: 'Enter / F2 (editable cell)',
          action: 'Opens the editor; Enter saves, Escape cancels, Tab saves and moves.',
        },
        { keys: 'Any character (editable cell)', action: 'Starts editing with that character.' },
        {
          keys: 'Enter (other cell)',
          action:
            'Opens the row (`onRowOpen`), or enters a cell with several controls (Escape leaves).',
        },
        { keys: 'Space', action: 'Toggles the row’s selection; Shift+Space selects the range.' },
        { keys: 'Ctrl+A / ⌘A', action: 'Selects or clears all rows.' },
      ],
    },
    accessibility: {
      role: 'grid with rowgroups, rows, columnheaders, rowheaders and gridcells',
      name: '`label` (or `aria-labelledby`).',
      items: [
        '`aria-rowcount`, `aria-rowindex`, `aria-colcount` and `aria-colindex` describe the whole grid although only some rows are rendered.',
        'Header cells are named by their label only and carry `aria-sort`; the column menu button is labelled “{column} column options”.',
        'With editable columns, read-only cells set `aria-readonly`; editors are named “{column} for {row}”.',
        '`aria-multiselectable` and per-row `aria-selected` when `selectable`.',
      ],
    },
    responsive: {
      items: [
        'Scrolls both ways inside its own area; pinned columns and the header stay in place.',
        'At 390 px pin the identifying column so rows stay recognisable while scrolling sideways.',
        'On touch screens header menu buttons are always visible and 44 px.',
      ],
    },
    theme: {
      items: [
        'Surface, border, accent-subtle (selection) and focus-ring tokens; follows every theme and density.',
        'Forced colours: selected rows use Highlight / HighlightText; focus uses Highlight.',
      ],
    },
    examples: {
      items: [
        { title: '10,000 editable invoices', stack: true, render: () => <EditableGrid /> },
        {
          title: 'Read-only, compact',
          stack: true,
          render: () => (
            <DataGrid
              label="Invoices"
              density="compact"
              height={240}
              columns={COLUMNS.map((c) => ({ ...c, editable: false }))}
              rows={INVOICES.slice(0, 200)}
              getRowId={(r) => r.id}
            />
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Give rows a readable `getRowLabel`: it names checkboxes, menus and editors.',
        'Reject the `onCellEdit` promise when the server refuses so the draft stays.',
        'Pin the identifying column on wide grids.',
      ],
      dont: [
        'Do not put the grid in a container without a height limit.',
        'Do not use it for a short read-only list; DataTable reads better.',
      ],
    },
    related: {
      items: [
        { id: 'data-table', reason: 'Paged tables; the shared column model.' },
        { id: 'editable-cell', reason: 'The cell editor.' },
        { id: 'row-actions-menu', reason: 'The row “⋯” menu.' },
        { id: 'virtual-list', reason: 'The virtualizer.' },
        { id: 'table-toolbar', reason: 'Search, columns and bulk actions above it.' },
        { id: 'column-menu', reason: 'Show and hide columns from a toolbar.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: [
        'checkbox',
        'column-menu',
        'data-table',
        'dropdown-menu',
        'editable-cell',
        'empty-state',
        'loading-state',
        'row-actions-menu',
        'virtual-list',
      ],
    },
    testing: {
      unit: [
        'APG grid roles, counts, indexes and one tab stop; 10,000 rows stay virtualized and the focused row stays mounted.',
        'Arrow, Home, End, Ctrl+Home/End movement; focus returns to the last cell.',
        'Sorting by Enter and click with aria-sort; pin, hide and sort from the column menu with focus return; Alt+Arrow resizing.',
        'Editing by typing, Enter, F2; Escape; validation; select editor; Tab saves and moves.',
        'Selection with Space, Shift+Space, checkboxes and Ctrl+A; row open and row action menus by keyboard.',
        'useDataTableColumns visibility and order; loading and empty states.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
