import { useState } from 'react';
import {
  Badge,
  Table,
  TableBody,
  TableCaption,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
  TableSelectCell,
} from '@rookcom/monarch-react';
import {
  DataTable,
  DataTableColumnChooser,
  useDataTableColumns,
  type DataTableColumn,
  type DataTableSort,
} from '@rookcom/monarch-react/data';
import type { ComponentDocs } from '../../../internal/component-docs.js';

interface Invoice {
  id: string;
  customer: string;
  status: 'Paid' | 'Due' | 'Overdue';
  amount: number;
  issued: string;
  owner: string;
}

const INVOICES: Invoice[] = [
  {
    id: 'INV-1042',
    customer: 'Northfield Traders',
    status: 'Paid',
    amount: 1240,
    issued: '2026-09-02',
    owner: 'Ana',
  },
  {
    id: 'INV-1043',
    customer: 'Cobalt Ltd',
    status: 'Due',
    amount: 860,
    issued: '2026-09-11',
    owner: 'Ben',
  },
  {
    id: 'INV-1044',
    customer: 'Fernway Inc',
    status: 'Overdue',
    amount: 4300,
    issued: '2026-08-21',
    owner: 'Chen',
  },
  {
    id: 'INV-1045',
    customer: 'Tidewater Toys',
    status: 'Due',
    amount: 395,
    issued: '2026-09-18',
    owner: 'Ana',
  },
  {
    id: 'INV-1046',
    customer: 'Westbridge Imports',
    status: 'Paid',
    amount: 2150,
    issued: '2026-09-20',
    owner: 'Dev',
  },
  {
    id: 'INV-1047',
    customer: 'Lumen',
    status: 'Due',
    amount: 1720,
    issued: '2026-09-24',
    owner: 'Ben',
  },
];

const TONE = { Paid: 'success', Due: 'neutral', Overdue: 'danger' } as const;

const COLUMNS: DataTableColumn<Invoice>[] = [
  { id: 'customer', header: 'Customer', sortable: true },
  {
    id: 'status',
    header: 'Status',
    sortable: true,
    cell: (r) => <Badge tone={TONE[r.status]}>{r.status}</Badge>,
  },
  { id: 'issued', header: 'Issued', sortable: true, hideBelow: 520 },
  { id: 'owner', header: 'Owner', sortable: true, defaultHidden: false },
  {
    id: 'amount',
    header: 'Amount',
    numeric: true,
    sortable: true,
    cell: (r) => `$${r.amount.toLocaleString('en-US')}`,
  },
];

const shared = {
  rows: INVOICES,
  getRowId: (r: Invoice) => r.id,
  getRowLabel: (r: Invoice) => `${r.id} for ${r.customer}`,
};

function SortAndSelect() {
  const [selected, setSelected] = useState<string[]>(['INV-1044']);
  const [opened, setOpened] = useState<string>();
  return (
    <div style={{ display: 'grid', gap: 8, width: '100%' }}>
      <DataTable
        {...shared}
        columns={COLUMNS}
        caption="Invoices"
        defaultSort={[{ id: 'issued', direction: 'descending' }]}
        multiSort
        selectable
        selectedRowIds={selected}
        onSelectedRowIdsChange={setSelected}
        stickyFirstColumn
        onRowOpen={(r) => setOpened(r.id)}
        rowActions={(r) => [
          { id: 'send', label: 'Send reminder', onSelect: () => setOpened(`reminder ${r.id}`) },
          { id: 'void', label: 'Void invoice', tone: 'danger', onSelect: () => {} },
        ]}
      />
      <p style={{ margin: 0, fontSize: 13 }}>
        {selected.length} selected{opened ? ` · opened ${opened}` : ''}
      </p>
    </div>
  );
}

function ServerDriven() {
  // Stand-in for a server: sorts and pages a larger list on request.
  const all = Array.from({ length: 23 }, (_, i) => ({
    ...(INVOICES[i % INVOICES.length] as Invoice),
    id: `INV-${2000 + i}`,
    amount: 100 + ((i * 337) % 4000),
  }));
  const [sort, setSort] = useState<DataTableSort[]>([{ id: 'amount', direction: 'descending' }]);
  const [page, setPage] = useState(1);
  const s = sort[0];
  const ordered = s
    ? all.slice().sort((a, b) => {
        const k = s.id as keyof Invoice;
        const r = String(a[k]).localeCompare(String(b[k]), undefined, { numeric: true });
        return s.direction === 'ascending' ? r : -r;
      })
    : all;
  const rows = ordered.slice((page - 1) * 5, page * 5);
  return (
    <DataTable
      columns={COLUMNS}
      rows={rows}
      getRowId={(r) => r.id}
      caption="Invoices (server sorted)"
      sort={sort}
      onSortChange={(next) => {
        setSort(next);
        setPage(1);
      }}
      manualSorting
      manualPagination
      pageSize={5}
      page={page}
      onPageChange={setPage}
      totalRowCount={all.length}
      density="compact"
    />
  );
}

function ColumnsAndDetails() {
  const columns: DataTableColumn<Invoice>[] = COLUMNS.map((c) =>
    c.id === 'customer' ? { ...c, resizable: true, width: 220, hideable: false } : c,
  );
  const cols = useDataTableColumns(columns);
  return (
    <div style={{ display: 'grid', gap: 8, width: '100%' }}>
      <div style={{ display: 'flex', justifyContent: 'flex-end' }}>
        <DataTableColumnChooser {...cols.chooserProps} />
      </div>
      <DataTable
        {...shared}
        rows={INVOICES.slice(0, 3)}
        columns={columns}
        {...cols.tableProps}
        caption="Invoices with details"
        defaultExpandedRowIds={['INV-1043']}
        renderExpanded={(r) => (
          <p style={{ margin: 0 }}>
            {r.customer} was invoiced on {r.issued}. Owner: {r.owner}.
          </p>
        )}
      />
    </div>
  );
}

function States() {
  return (
    <div style={{ display: 'grid', gap: 24, width: '100%' }}>
      <DataTable {...shared} rows={[]} columns={COLUMNS} caption="Loading invoices" loading />
      <DataTable
        {...shared}
        rows={[]}
        columns={COLUMNS}
        caption="Filtered invoices"
        filtered
        onClearFilters={() => {}}
      />
      <DataTable
        {...shared}
        rows={[]}
        columns={COLUMNS}
        caption="Failed invoices"
        error="The server did not respond."
        onRetry={() => {}}
      />
    </div>
  );
}

function Cards() {
  return (
    <div style={{ width: '100%', maxWidth: 360 }}>
      <DataTable
        {...shared}
        rows={INVOICES.slice(0, 3)}
        columns={COLUMNS}
        caption="Invoices"
        layout="cards"
        selectable
        defaultSelectedRowIds={['INV-1042']}
        onRowOpen={() => {}}
      />
    </div>
  );
}

function Primitives() {
  const [sort, setSort] = useState<'ascending' | 'descending'>('ascending');
  return (
    <Table>
      <TableCaption>Open tickets</TableCaption>
      <TableHeader>
        <TableRow>
          <TableSelectCell header label="Select all" />
          <TableHead sort={sort} onSortChange={setSort}>
            Title
          </TableHead>
          <TableHead numeric>Age</TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        <TableRow selected>
          <TableSelectCell label="Select ticket 1" checked />
          <TableCell>Login link expired</TableCell>
          <TableCell numeric>3d</TableCell>
        </TableRow>
        <TableRow>
          <TableSelectCell label="Select ticket 2" />
          <TableCell>Export stuck at 90%</TableCell>
          <TableCell numeric>1d</TableCell>
        </TableRow>
      </TableBody>
    </Table>
  );
}

export default {
  id: 'data-table',
  title: 'DataTable',
  summary:
    'Column-driven data table with sorting, selection, column controls, pagination, states and keyboard grid navigation; plus the Table parts it is built on.',
  sections: {
    purpose: {
      summary:
        'Shows records in rows and columns that people compare, sort, select and act on. `DataTable` takes column definitions and rows and handles sorting, selection, column visibility, order and width, pagination, row actions, expandable rows, loading, empty and error states, keyboard navigation and a card layout for phones. The `Table*` parts are the styled native table it is built on, for fully custom tables.',
      useWhen: [
        'Tabular records the user compares across columns (tickets, invoices, members).',
        'Rows are sorted, selected for bulk actions, paged, or opened.',
        'The data comes from a server that sorts and pages (`manualSorting`, `manualPagination`).',
      ],
      avoidWhen: [
        'Label/value pairs about one record: use DescriptionList.',
        'A list without columns: use a list of rows or cards.',
        'Spreadsheet-style editing of every cell: that needs a dedicated editable grid.',
      ],
    },
    designPrinciples: {
      items: [
        'Native `<table>` semantics first: caption, `scope="col"` headers, the first column as row headers (`scope="row"`), and `aria-sort` on the primary sorted column.',
        'No business logic: the app owns data, filters and persistence. DataTable sorts and pages client-side only when the app has not taken that over, and always reports intent through callbacks.',
        'Every state is controlled or uncontrolled: `sort`/`defaultSort`, `selectedRowIds`/`defaultSelectedRowIds`, `page`/`defaultPage`, `expandedRowIds`/`defaultExpandedRowIds`, `columnWidths`/`defaultColumnWidths`.',
        'Built from Monarch parts: Checkbox, DropdownMenu, Pagination, EmptyState, LoadingState and ErrorState, so states look the same as everywhere else.',
        'Every control has a row-specific name ("Select INV-1042 for Northfield Traders", "Actions for …").',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        '`DataTable`, `DataTableColumnChooser`, `useDataTableColumns`, `sortRows`, `nextSort` and `columnChooserColumns` are imported from `@rookcom/monarch-react/data`. The `Table*` parts and `SortButton` are exported from both `@rookcom/monarch-react` (unchanged) and `@rookcom/monarch-react/data`.',
        '`DataTable` is generic over the row type: `DataTable<Invoice>` checks `columns`, `getRowId` and every callback against it. Its ref is the outer `div`.',
        '`DataTableColumn<T>`: `id`, `header`, optional `label`, `cell`, `accessor`, `sortable`, `sortFn`, `numeric`, `width`/`minWidth`/`maxWidth`, `resizable`, `hideable`, `defaultHidden`, `hideBelow`, `rowHeader`.',
        '`sortRows(rows, sort, columns)` is the client-side sort DataTable uses (stable, numeric-aware, empty values last); `nextSort(sort, id, additive)` is the header cycle (ascending → descending → off).',
        '`messages` replaces any user-visible string, for translation.',
        '`Table` gains two additive props: `stickyFirstColumn` and `maxHeight`.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [
        {
          prop: 'density',
          values: ['comfortable', 'compact'],
          description: 'Row height. Default `comfortable`. Exposed as `data-density` on the table.',
        },
        {
          prop: 'layout',
          values: ['table', 'cards', 'auto'],
          description:
            '`table` (default) scrolls sideways when narrow; `cards` renders a stacked list; `auto` switches to cards below `cardBreakpoint` (640px of the table’s own width). Exposed as `data-view`.',
        },
        {
          prop: 'stickyHeader / stickyFirstColumn',
          values: ['true', 'false'],
          description:
            'Keep the header row, and the selection/expand columns plus the first data column, visible while scrolling. Give `maxHeight` for a vertical scroll area.',
        },
        {
          prop: 'multiSort',
          values: ['true', 'false'],
          description:
            'Shift+click or Shift+Enter on a header adds it to the sort; headers show their sort position.',
        },
        {
          prop: 'manualSorting / manualPagination',
          values: ['true', 'false'],
          description:
            'Server-driven mode: rows arrive sorted and paged; DataTable only reports `onSortChange` and `onPageChange`. Pass `totalRowCount`.',
        },
        {
          prop: 'numeric (column)',
          values: ['true', 'false'],
          description: 'Right-aligns the header and cells with tabular figures (`data-numeric`).',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Loading',
          attribute: 'aria-busy, data-busy',
          description:
            'Without rows the body shows a LoadingState skeleton; with rows they stay, a progress line runs above them and the table is `aria-busy`. "Loading rows" is announced.',
        },
        {
          name: 'Empty / filtered empty',
          description:
            'An EmptyState fills the body. With `filtered` it explains that filters hide everything and offers `onClearFilters`. `emptyState` replaces it.',
        },
        {
          name: 'Error',
          description:
            'Without rows an ErrorState with "Try again" (`onRetry`) replaces the body; with rows a compact one appears above them.',
        },
        {
          name: 'Sorted column',
          attribute: 'aria-sort, data-direction',
          description:
            'The primary sorted column has `aria-sort`; the sort button’s name states the direction (and position in a multi-sort).',
        },
        {
          name: 'Selected row',
          attribute: 'aria-selected, data-selected',
          description:
            'Selected rows are highlighted. The header checkbox is checked, unchecked or mixed for the current page.',
        },
        {
          name: 'All matching selected',
          description:
            'With `totalRowCount` and `onSelectAllMatching`, selecting the whole page shows "Select all N"; `allMatchingSelected` shows "All N rows are selected" with "Clear selection".',
        },
        {
          name: 'Expanded row',
          attribute: 'aria-expanded, data-expanded',
          description: 'A detail row follows the row, linked by `aria-controls`.',
        },
      ],
    },
    interaction: {
      items: [
        'A sortable header cycles ascending → descending → unsorted. With `multiSort`, Shift adds or updates a secondary sort.',
        'Row checkboxes select one row; Shift+click selects the range since the last clicked row. The header checkbox selects or clears every selectable row on the page.',
        'With `onRowOpen`, clicking a table row anywhere except a control (or while text is selected) opens it; in the card layout each card has an explicit “Open …” button.',
        'Row actions marked `inline` are visible buttons; the rest are in an overflow menu.',
        'Resizable columns are dragged by their handle; double-click resets to the default width.',
        'DataTableColumnChooser shows, hides and reorders columns with checkboxes and move buttons (no drag and drop), and can reset them.',
        'Client-side sorting and paging happen when `manualSorting` / `manualPagination` are off.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Tab',
          action:
            'Without grid navigation: moves through sort buttons, resize handles, checkboxes and row controls. With grid navigation: enters and leaves the table (one tab stop).',
        },
        { keys: 'Arrow keys', action: 'Grid navigation: move one cell (RTL aware).' },
        { keys: 'Home / End', action: 'Grid navigation: first / last cell in the row.' },
        {
          keys: 'Ctrl+Home / Ctrl+End',
          action: 'Grid navigation: first / last cell of the table.',
        },
        { keys: 'Page Up / Page Down', action: 'Grid navigation: move five rows.' },
        {
          keys: 'Enter',
          action:
            'On a cell: opens the row (`onRowOpen`), or moves into a cell with several controls. On a sort button: sorts (Shift+Enter adds to a multi-sort).',
        },
        {
          keys: 'Space',
          action: 'On a row cell: toggles its selection. On a checkbox: toggles it.',
        },
        { keys: 'Escape', action: 'Leaves a cell with several controls, back to grid navigation.' },
        {
          keys: 'Arrow Left / Right, Home / End',
          action:
            'On a focused resize handle: narrower / wider by 16px (64px with Shift), minimum / maximum. In grid navigation use Alt+Arrow on the header cell.',
        },
      ],
      notes: [
        'Grid navigation (WAI-ARIA grid pattern) is on by default when `onRowOpen` is set; turn it on or off with `keyboardNavigation`. A cell with one control focuses that control directly; text inputs inside cells keep their arrow keys.',
      ],
    },
    accessibility: {
      role: 'table, or grid with keyboard navigation',
      name: '`caption` (required), rendered as a `<caption>`; visually hidden unless `showCaption`.',
      items: [
        'The first visible column renders as row headers (`th scope="row"`) so every cell is announced with its row.',
        '`aria-sort` is set on one header at a time (the primary sort), as ARIA recommends.',
        'Utility columns (expand, select, actions) have visually hidden header text, so no header cell is empty.',
        'Hidden columns (visibility or `hideBelow`) are removed from the accessibility tree too; the card layout shows every visible column as a labelled field.',
        'The resize handle is a focusable `separator` with `aria-valuenow/min/max`.',
        'Loading is announced through a polite status region; selection counts are announced by BulkActionBar.',
        'Card layout is a labelled list; each card has a labelled checkbox and an "Open …" button, so it never relies on a clickable container.',
      ],
    },
    responsive: {
      items: [
        'Default: the table scrolls sideways inside its wrapper; with `stickyFirstColumn` the row name stays visible at 320px.',
        '`hideBelow` on a column hides it while the table is narrower than that width (responsive column priority).',
        '`layout="auto"` switches to stacked cards under `cardBreakpoint`, measured on the table itself (works inside split panes); `layout="cards"` always uses them.',
        'Pagination switches to its compact variant in the card layout.',
      ],
    },
    theme: {
      items: [
        'Borders, hover, selected, pinned-column and banner backgrounds use Monarch semantic tokens, so light, dark and high-contrast themes apply.',
        'The loading line stops animating with `prefers-reduced-motion: reduce`; expand and resize transitions are removed.',
        'Wrap in `<Card flush>` for a framed table.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Sorting, selection, row actions and grid keyboard',
          stack: true,
          render: () => <SortAndSelect />,
        },
        {
          title: 'Server-driven sorting and pagination',
          stack: true,
          render: () => <ServerDriven />,
        },
        {
          title: 'Column chooser, resizing and expandable rows',
          stack: true,
          render: () => <ColumnsAndDetails />,
        },
        {
          title: 'Loading, filtered empty and error states',
          stack: true,
          render: () => <States />,
        },
        {
          title: 'Card layout for narrow screens',
          stack: true,
          render: () => <Cards />,
        },
        {
          title: 'Table parts (custom tables)',
          stack: true,
          render: () => <Primitives />,
        },
      ],
    },
    doDont: {
      do: [
        'Give every table a `caption` and rows a `getRowLabel`, so controls are named after the row.',
        'Use `manualSorting` and `manualPagination` when the server holds more rows than the page.',
        'Pair selection with BulkActionBar, and filters with FilterBar and `filtered` for the empty state.',
        'Mark numeric columns `numeric` and keep the record name in the first column.',
      ],
      dont: [
        'Do not hide important columns with `hideBelow` without offering the card layout or a detail view.',
        'Do not put several unrelated controls in one cell; give each its own column or a row action.',
        'Do not use a table for page layout.',
      ],
    },
    related: {
      items: [
        { id: 'bulk-action-bar', reason: 'Actions on the selected rows.' },
        { id: 'filter-bar', reason: 'Search and filters above the table.' },
        { id: 'saved-views', reason: 'Named combinations of filters, sort and columns.' },
        { id: 'pagination', reason: 'The pager DataTable renders with `pageSize`.' },
        { id: 'load-more', reason: 'Cursor pagination; pass it as `footer`.' },
        { id: 'empty-state', reason: 'The empty body.' },
        { id: 'description-list', reason: 'Details of a single record.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: [
        'button',
        'checkbox',
        'dropdown-menu',
        'popover',
        'empty-state',
        'error-state',
        'loading-state',
        'pagination',
      ],
      notes: ['No virtualisation: no product evidence needs more than a page of rows on screen.'],
    },
    testing: {
      unit: [
        'sortRows: stable, numeric-aware, empty last, multi-key; nextSort cycle and additive mode.',
        'Caption, row headers and numeric cells; client sort with aria-sort on the primary column; multi-sort with Shift and priority in the name.',
        'Server-driven mode reports sort and page without reordering; client pagination.',
        'Row selection, select all on page (mixed state), Shift range, isRowSelectable, controlled selection, "select all matching" banner.',
        'Column visibility, order and hideBelow; column chooser hides, reorders by button (focus kept) and resets.',
        'Loading, error (retry), empty and filtered-empty states; rows stay while reloading.',
        'Row actions with row-context names (inline and menu); expandable rows with aria-expanded/aria-controls.',
        'Row click opens except on controls; grid keyboard: one tab stop, arrows, Home/End, Ctrl+End, Enter opens, Space selects.',
        'Keyboard column resizing; pinned columns; card layout with labelled fields; server rendering.',
      ],
      axe: true,
      ssr: true,
      browser: [
        'Gallery visual baselines (every example) and cross-browser axe on #/components/data-table.',
      ],
    },
  },
} satisfies ComponentDocs;
