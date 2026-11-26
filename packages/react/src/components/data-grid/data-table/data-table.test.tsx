import { describe, expect, it, vi } from 'vitest';
import { createRef, useState } from 'react';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import {
  Table,
  TableBody,
  TableCaption,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
  TableSelectCell,
  SortButton,
  DataTable,
  DataTableColumnChooser,
  nextSort,
  sortRows,
  useDataTableColumns,
  type DataTableColumn,
  type DataTableSort,
} from './index.js';

describe('Table', () => {
  it('forwards refs from every part to its element', () => {
    const refs = {
      caption: createRef<HTMLTableCaptionElement>(),
      header: createRef<HTMLTableSectionElement>(),
      body: createRef<HTMLTableSectionElement>(),
      row: createRef<HTMLTableRowElement>(),
      head: createRef<HTMLTableCellElement>(),
      sort: createRef<HTMLButtonElement>(),
      cell: createRef<HTMLTableCellElement>(),
      select: createRef<HTMLTableCellElement>(),
    };
    render(
      <Table>
        <TableCaption ref={refs.caption}>Tickets</TableCaption>
        <TableHeader ref={refs.header}>
          <TableRow ref={refs.row}>
            <TableHead ref={refs.head}>
              <SortButton ref={refs.sort} direction="none" onClick={() => {}}>
                Title
              </SortButton>
            </TableHead>
          </TableRow>
        </TableHeader>
        <TableBody ref={refs.body}>
          <TableRow>
            <TableSelectCell ref={refs.select} label="Select 1" />
            <TableCell ref={refs.cell}>Login broken</TableCell>
          </TableRow>
        </TableBody>
      </Table>,
    );
    expect(refs.caption.current?.tagName).toBe('CAPTION');
    expect(refs.header.current?.tagName).toBe('THEAD');
    expect(refs.body.current?.tagName).toBe('TBODY');
    expect(refs.row.current?.tagName).toBe('TR');
    expect(refs.head.current?.tagName).toBe('TH');
    expect(refs.sort.current?.tagName).toBe('BUTTON');
    expect(refs.cell.current?.tagName).toBe('TD');
    expect(refs.select.current?.tagName).toBe('TD');
  });

  it('exposes sort state and toggles direction', async () => {
    function Demo() {
      const [sort, setSort] = useState<'ascending' | 'descending'>('ascending');
      return (
        <Table>
          <TableCaption>Tickets</TableCaption>
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
              <TableCell>Login broken</TableCell>
              <TableCell numeric>3d</TableCell>
            </TableRow>
          </TableBody>
        </Table>
      );
    }
    render(<Demo />);
    const th = screen.getByRole('columnheader', { name: /title/i });
    expect(th).toHaveAttribute('aria-sort', 'ascending');
    expect(screen.getByRole('button', { name: /title.*sorted ascending/i })).toBeInTheDocument();
    await userEvent.setup().click(screen.getByRole('button', { name: /title/i }));
    expect(th).toHaveAttribute('aria-sort', 'descending');
    expect(screen.getByRole('checkbox', { name: 'Select all' })).toBeInTheDocument();
    expect(screen.getByRole('row', { name: /login broken/i })).toHaveAttribute(
      'aria-selected',
      'true',
    );
    await expectNoA11yViolations();
  });

  it('takes translated sort state words', () => {
    render(
      <Table>
        <TableCaption>Tickets</TableCaption>
        <TableHeader>
          <TableRow>
            <TableHead
              sort="descending"
              onSortChange={() => {}}
              sortStateLabels={{ descending: 'absteigend sortiert' }}
            >
              Titel
            </TableHead>
            <TableHead sort="none" onSortChange={() => {}}>
              Status
            </TableHead>
          </TableRow>
        </TableHeader>
      </Table>,
    );
    expect(
      screen.getByRole('button', { name: /^Titel ?, absteigend sortiert$/ }),
    ).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /^Status ?, not sorted$/ })).toBeInTheDocument();
  });

  it('renders on the server', () => {
    const html = renderToString(
      <Table aria-label="Deals">
        <TableHeader>
          <TableRow>
            <TableHead sort="none" onSortChange={() => {}}>
              Name
            </TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          <TableRow>
            <TableCell>Acme</TableCell>
          </TableRow>
        </TableBody>
      </Table>,
    );
    expect(html).toContain('mn-table');
    expect(html).toContain('not sorted');
  });
});

type Deal = { id: string; name: string; stage: string; amount: number; owner: string | null };
const DEALS: Deal[] = [
  { id: 'd1', name: 'Northfield renewal', stage: 'Won', amount: 1200, owner: 'Ana' },
  { id: 'd2', name: 'Cobalt pilot', stage: 'Open', amount: 300, owner: null },
  { id: 'd3', name: 'Fernway upgrade', stage: 'Open', amount: 4500, owner: 'Ben' },
];
const COLUMNS: DataTableColumn<Deal>[] = [
  { id: 'name', header: 'Name', sortable: true },
  { id: 'stage', header: 'Stage', sortable: true },
  { id: 'amount', header: 'Amount', numeric: true, sortable: true },
  { id: 'owner', header: 'Owner', sortable: true },
];
const names = () => screen.getAllByRole('rowheader').map((c) => c.textContent);
const base = {
  columns: COLUMNS,
  rows: DEALS,
  getRowId: (r: Deal) => r.id,
  getRowLabel: (r: Deal) => r.name,
  caption: 'Deals',
};

describe('sortRows / nextSort', () => {
  it('sorts stable, numeric-aware, empty values last, by several keys', () => {
    const asc = sortRows(DEALS, [{ id: 'amount', direction: 'ascending' }], COLUMNS);
    expect(asc.map((d) => d.id)).toEqual(['d2', 'd1', 'd3']);
    const owners = sortRows(DEALS, [{ id: 'owner', direction: 'descending' }], COLUMNS);
    expect(owners.map((d) => d.owner)).toEqual(['Ben', 'Ana', null]);
    const multi = sortRows(
      DEALS,
      [
        { id: 'stage', direction: 'ascending' },
        { id: 'amount', direction: 'descending' },
      ],
      COLUMNS,
    );
    expect(multi.map((d) => d.id)).toEqual(['d3', 'd2', 'd1']);
  });
  it('compares text numeric-aware and case-insensitively', () => {
    const rows = [{ n: 'item 10' }, { n: 'Item 2' }, { n: 'item 1' }];
    const sorted = sortRows(
      rows,
      [{ id: 'n', direction: 'ascending' }],
      [{ id: 'n', header: 'N' }],
    );
    expect(sorted.map((r) => r.n)).toEqual(['item 1', 'Item 2', 'item 10']);
  });
  it('cycles ascending, descending, off, and appends when additive', () => {
    expect(nextSort([], 'a')).toEqual([{ id: 'a', direction: 'ascending' }]);
    expect(nextSort([{ id: 'a', direction: 'ascending' }], 'a')).toEqual([
      { id: 'a', direction: 'descending' },
    ]);
    expect(nextSort([{ id: 'a', direction: 'descending' }], 'a')).toEqual([]);
    expect(nextSort([{ id: 'a', direction: 'ascending' }], 'b', true)).toEqual([
      { id: 'a', direction: 'ascending' },
      { id: 'b', direction: 'ascending' },
    ]);
  });
});

describe('DataTable', () => {
  it('derives row ids from a precomputed index, never rows.indexOf', () => {
    const rows = [...DEALS];
    const indexOf = vi.spyOn(rows, 'indexOf');
    const getRowId = vi.fn((r: Deal, i: number) => `${r.id}@${i}`);
    render(
      <DataTable
        {...base}
        rows={rows}
        getRowId={getRowId}
        sort={[{ id: 'amount', direction: 'ascending' }]}
      />,
    );
    expect(indexOf).not.toHaveBeenCalled();
    // The index is the row's position in `rows`, not in the sorted view.
    expect(getRowId).toHaveBeenCalledWith(DEALS[2], 2);
    expect(getRowId).toHaveBeenCalledWith(DEALS[1], 1);
  });

  it('renders columns with a caption, row headers and numeric cells', async () => {
    render(<DataTable {...base} />);
    expect(screen.getByRole('table', { name: 'Deals' })).toBeInTheDocument();
    expect(screen.getAllByRole('columnheader')).toHaveLength(4);
    expect(names()).toEqual(['Northfield renewal', 'Cobalt pilot', 'Fernway upgrade']);
    expect(screen.getByRole('cell', { name: '4500' })).toHaveAttribute('data-numeric', 'true');
    await expectNoA11yViolations();
  });

  it('sorts client-side (uncontrolled) and sets aria-sort on the primary column', async () => {
    const onSortChange = vi.fn();
    render(<DataTable {...base} onSortChange={onSortChange} />);
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: /^Amount/ }));
    expect(names()).toEqual(['Cobalt pilot', 'Northfield renewal', 'Fernway upgrade']);
    expect(screen.getByRole('columnheader', { name: /Amount/ })).toHaveAttribute(
      'aria-sort',
      'ascending',
    );
    expect(
      screen.getByRole('button', { name: /^Amount ?, sorted ascending$/ }),
    ).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: /^Amount/ }));
    expect(names()[0]).toBe('Fernway upgrade');
    await user.click(screen.getByRole('button', { name: /^Amount/ }));
    expect(onSortChange).toHaveBeenLastCalledWith([]);
    expect(screen.getByRole('columnheader', { name: /Amount/ })).not.toHaveAttribute('aria-sort');
  });

  it('multi-sorts with Shift and announces the sort priority', async () => {
    render(<DataTable {...base} multiSort />);
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: /^Stage/ }));
    await user.keyboard('{Shift>}');
    await user.click(screen.getByRole('button', { name: /^Amount/ }));
    await user.click(screen.getByRole('button', { name: /^Amount/ }));
    await user.keyboard('{/Shift}');
    expect(names()).toEqual(['Fernway upgrade', 'Cobalt pilot', 'Northfield renewal']);
    expect(
      screen.getByRole('button', { name: /^Amount ?, sorted descending, sort 2$/ }),
    ).toBeInTheDocument();
    expect(screen.getByRole('columnheader', { name: /Amount/ })).not.toHaveAttribute('aria-sort');
  });

  it('server-driven: reports sort and page intent without reordering or slicing rows', async () => {
    function Server() {
      const [sort, setSort] = useState<DataTableSort[]>([]);
      const [page, setPage] = useState(1);
      return (
        <>
          <DataTable
            {...base}
            sort={sort}
            onSortChange={setSort}
            manualSorting
            manualPagination
            pageSize={3}
            totalRowCount={9}
            page={page}
            onPageChange={setPage}
          />
          <output data-testid="state">{JSON.stringify({ sort, page })}</output>
        </>
      );
    }
    render(<Server />);
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: /^Amount/ }));
    expect(names()[0]).toBe('Northfield renewal');
    await user.click(screen.getByRole('button', { name: 'Page 2' }));
    expect(screen.getByTestId('state')).toHaveTextContent(
      '{"sort":[{"id":"amount","direction":"ascending"}],"page":2}',
    );
    expect(names()).toHaveLength(3);
  });

  it('passes translated strings and the locale to the pagination footer', () => {
    render(
      <DataTable
        {...base}
        pageSize={1}
        locale="de-DE"
        paginationMessages={{ next: 'Weiter', page: (p) => `Seite ${p}` }}
      />,
    );
    expect(screen.getByRole('button', { name: 'Weiter' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Seite 2' })).toBeInTheDocument();
  });

  it('paginates client-side', async () => {
    render(<DataTable {...base} pageSize={2} />);
    expect(names()).toEqual(['Northfield renewal', 'Cobalt pilot']);
    await userEvent.setup().click(screen.getByRole('button', { name: 'Page 2' }));
    expect(names()).toEqual(['Fernway upgrade']);
    expect(screen.getByRole('navigation', { name: 'Table pages' })).toBeInTheDocument();
  });

  it('selects rows, selects all on the page (indeterminate), and selects a range with Shift', async () => {
    const onChange = vi.fn();
    render(<DataTable {...base} selectable onSelectedRowIdsChange={onChange} />);
    const user = userEvent.setup();
    const all = screen.getByRole('checkbox', { name: 'Select all rows on this page' });
    await user.click(screen.getByRole('checkbox', { name: 'Select Cobalt pilot' }));
    expect(onChange).toHaveBeenLastCalledWith(['d2']);
    expect(all).toHaveAttribute('aria-checked', 'mixed');
    expect(screen.getByRole('row', { name: /Cobalt pilot/ })).toHaveAttribute(
      'aria-selected',
      'true',
    );
    await user.click(all);
    expect(onChange).toHaveBeenLastCalledWith(['d2', 'd1', 'd3']);
    expect(all).toHaveAttribute('aria-checked', 'true');
    await user.click(all);
    expect(onChange).toHaveBeenLastCalledWith([]);
    await user.click(screen.getByRole('checkbox', { name: 'Select Northfield renewal' }));
    await user.keyboard('{Shift>}');
    await user.click(screen.getByRole('checkbox', { name: 'Select Fernway upgrade' }));
    await user.keyboard('{/Shift}');
    expect(onChange).toHaveBeenLastCalledWith(['d1', 'd2', 'd3']);
    await expectNoA11yViolations();
  });

  it('respects isRowSelectable and controlled selection', async () => {
    render(
      <DataTable
        {...base}
        selectable
        selectedRowIds={['d1']}
        isRowSelectable={(r) => r.stage !== 'Won'}
      />,
    );
    expect(screen.getByRole('checkbox', { name: 'Select Northfield renewal' })).toBeDisabled();
    await userEvent.setup().click(screen.getByRole('checkbox', { name: 'Select Cobalt pilot' }));
    // Controlled without a handler: nothing changes.
    expect(screen.getByRole('checkbox', { name: 'Select Cobalt pilot' })).toHaveAttribute(
      'aria-checked',
      'false',
    );
  });

  it('offers "select all matching" once the whole page is selected', async () => {
    const onAll = vi.fn();
    function Demo() {
      const [ids, setIds] = useState<string[]>(['d1', 'd2', 'd3']);
      const [allMatching, setAllMatching] = useState(false);
      return (
        <DataTable
          {...base}
          selectable
          selectedRowIds={ids}
          onSelectedRowIdsChange={(next) => {
            setIds(next);
            setAllMatching(false);
          }}
          totalRowCount={40}
          allMatchingSelected={allMatching}
          onSelectAllMatching={() => {
            onAll();
            setAllMatching(true);
          }}
        />
      );
    }
    render(<Demo />);
    expect(screen.getByText('All 3 rows on this page are selected.')).toBeInTheDocument();
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Select all 40' }));
    expect(onAll).toHaveBeenCalledOnce();
    expect(screen.getByText('All 40 rows are selected.')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Clear selection' }));
    expect(screen.queryByText(/rows are selected/)).toBeNull();
  });

  it('hides columns by visibility, honours order, and drops hideBelow columns when narrow', () => {
    const spy = vi
      .spyOn(HTMLElement.prototype, 'getBoundingClientRect')
      .mockReturnValue({ width: 500 } as DOMRect);
    const cols = COLUMNS.map((c) => (c.id === 'owner' ? { ...c, hideBelow: 600 } : c));
    render(
      <DataTable
        {...base}
        columns={cols}
        columnVisibility={{ stage: false }}
        columnOrder={['amount', 'name']}
      />,
    );
    expect(screen.getAllByRole('columnheader').map((h) => h.textContent)).toEqual([
      'Amount, not sorted',
      'Name, not sorted',
    ]);
    spy.mockRestore();
  });

  it('shows loading, error and empty states built from LoadingState, ErrorState and EmptyState', async () => {
    const onRetry = vi.fn();
    const onClear = vi.fn();
    const { rerender } = render(<DataTable {...base} rows={[]} loading />);
    expect(screen.getByRole('table')).toHaveAttribute('aria-busy', 'true');
    expect(screen.getAllByText('Loading rows').length).toBeGreaterThan(0);
    rerender(<DataTable {...base} rows={[]} error="Network error" onRetry={onRetry} />);
    expect(screen.getByText('These rows could not be loaded')).toBeInTheDocument();
    await userEvent.setup().click(screen.getByRole('button', { name: /try again/i }));
    expect(onRetry).toHaveBeenCalledOnce();
    rerender(<DataTable {...base} rows={[]} />);
    expect(screen.getByText('Nothing here yet')).toBeInTheDocument();
    rerender(<DataTable {...base} rows={[]} filtered onClearFilters={onClear} />);
    await userEvent.setup().click(screen.getByRole('button', { name: 'Clear filters' }));
    expect(onClear).toHaveBeenCalledOnce();
    rerender(<DataTable {...base} loading />);
    expect(names()).toHaveLength(3);
    await expectNoA11yViolations();
  });

  it('renders row actions with row-context labels', async () => {
    const onArchive = vi.fn();
    const onRemove = vi.fn();
    render(
      <DataTable
        {...base}
        rowActions={(r) => [
          { id: 'remove', label: 'Remove', inline: true, onSelect: () => onRemove(r.id) },
          { id: 'archive', label: 'Archive', onSelect: () => onArchive(r.id) },
        ]}
      />,
    );
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Remove Cobalt pilot' }));
    expect(onRemove).toHaveBeenCalledWith('d2');
    screen.getByRole('button', { name: 'Actions for Fernway upgrade' }).focus();
    await user.keyboard('{Enter}');
    await user.click(await screen.findByRole('menuitem', { name: 'Archive' }));
    expect(onArchive).toHaveBeenCalledWith('d3');
    expect(screen.getByRole('columnheader', { name: 'Actions' })).toBeInTheDocument();
  });

  it('expands rows (uncontrolled) with aria-expanded and aria-controls', async () => {
    render(<DataTable {...base} renderExpanded={(r) => <p>Notes for {r.name}</p>} />);
    const toggle = screen.getByRole('button', { name: 'Details for Cobalt pilot' });
    expect(toggle).toHaveAttribute('aria-expanded', 'false');
    await userEvent.setup().click(toggle);
    expect(toggle).toHaveAttribute('aria-expanded', 'true');
    const detail = document.getElementById(toggle.getAttribute('aria-controls')!);
    expect(detail).toHaveTextContent('Notes for Cobalt pilot');
    await expectNoA11yViolations();
  });

  it('opens rows on click but not when a control inside is used', async () => {
    const onOpen = vi.fn();
    render(<DataTable {...base} selectable onRowOpen={(r) => onOpen(r.id)} />);
    const user = userEvent.setup();
    await user.click(screen.getByRole('gridcell', { name: 'Won' }));
    expect(onOpen).toHaveBeenCalledWith('d1');
    await user.click(screen.getByRole('checkbox', { name: 'Select Northfield renewal' }));
    expect(onOpen).toHaveBeenCalledTimes(1);
  });

  it('marks the row-open chevron so CSS can mirror it in RTL', () => {
    const { container } = render(<DataTable {...base} layout="cards" onRowOpen={() => {}} />);
    expect(container.querySelectorAll('.mn-data-table__row-open-icon')).toHaveLength(DEALS.length);
  });

  it('keyboard grid: one tab stop, arrows, Home/End, Enter opens, Space selects', async () => {
    const onOpen = vi.fn();
    render(
      <>
        <button type="button">Before</button>
        <DataTable {...base} selectable onRowOpen={(r) => onOpen(r.id)} />
        <button type="button">After</button>
      </>,
    );
    const grid = screen.getByRole('grid', { name: 'Deals' });
    const user = userEvent.setup();
    screen.getByRole('button', { name: 'Before' }).focus();
    await user.tab();
    const selectAll = screen.getByRole('checkbox', { name: 'Select all rows on this page' });
    expect(selectAll).toHaveFocus();
    await user.tab();
    expect(screen.getByRole('button', { name: 'After' })).toHaveFocus();
    await user.tab({ shift: true });
    expect(selectAll).toHaveFocus();
    await user.keyboard('{ArrowRight}');
    expect(screen.getByRole('button', { name: /^Name/ })).toHaveFocus();
    await user.keyboard('{ArrowDown}');
    const firstName = within(grid).getByRole('rowheader', { name: 'Northfield renewal' });
    expect(firstName).toHaveFocus();
    await user.keyboard('{Enter}');
    expect(onOpen).toHaveBeenCalledWith('d1');
    await user.keyboard(' ');
    expect(screen.getByRole('checkbox', { name: 'Select Northfield renewal' })).toHaveAttribute(
      'aria-checked',
      'true',
    );
    await user.keyboard('{End}');
    expect(within(grid).getByRole('gridcell', { name: 'Ana' })).toHaveFocus();
    await user.keyboard('{Home}');
    expect(screen.getByRole('checkbox', { name: 'Select Northfield renewal' })).toHaveFocus();
    await user.keyboard('{Control>}{End}{/Control}');
    expect(within(grid).getByRole('gridcell', { name: 'Ben' })).toHaveFocus();
    await user.keyboard('{ArrowUp}{ArrowUp}{ArrowUp}{ArrowUp}{ArrowUp}');
    expect(screen.getByRole('button', { name: /^Owner/ })).toHaveFocus();
    await expectNoA11yViolations();
  });

  it('resizes a column by keyboard and reports widths', async () => {
    const onWidths = vi.fn();
    const cols = COLUMNS.map((c) => (c.id === 'name' ? { ...c, resizable: true, width: 200 } : c));
    render(<DataTable {...base} columns={cols} onColumnWidthsChange={onWidths} />);
    const handle = screen.getByRole('separator', { name: 'Resize Name column' });
    expect(handle).toHaveAttribute('aria-valuenow', '200');
    handle.focus();
    const user = userEvent.setup();
    await user.keyboard('{ArrowRight}');
    expect(onWidths).toHaveBeenLastCalledWith({ name: 216 });
    expect(handle).toHaveAttribute('aria-valuenow', '216');
    await user.keyboard('{Home}');
    expect(handle).toHaveAttribute('aria-valuenow', '64');
    expect(screen.getByRole('table')).toHaveAttribute('data-layout', 'fixed');
    await expectNoA11yViolations();
  });

  it('pins the selection column and first data column with stickyFirstColumn', () => {
    render(<DataTable {...base} selectable stickyFirstColumn stickyHeader maxHeight={300} />);
    const head = screen.getByRole('columnheader', { name: /Name/ });
    expect(head).toHaveAttribute('data-pinned', 'true');
    expect(head.style.insetInlineStart).toBe('2.5rem');
    expect(screen.getByRole('table')).toHaveAttribute('data-sticky', 'true');
  });

  it('only the pinned header cell gets an inline offset, so a sticky header does not pile up', () => {
    render(<DataTable {...base} selectable stickyFirstColumn stickyHeader maxHeight={300} />);
    for (const name of [/Stage/, /Amount/, /Owner/]) {
      const head = screen.getByRole('columnheader', { name });
      expect(head).not.toHaveAttribute('data-pinned');
      expect(head.style.insetInlineStart).toBe('');
    }
  });

  it('numbers the rows of a server page so aria-rowcount is read correctly', () => {
    render(
      <DataTable
        {...base}
        keyboardNavigation
        manualPagination
        pageSize={3}
        page={2}
        totalRowCount={9}
      />,
    );
    expect(screen.getByRole('grid')).toHaveAttribute('aria-rowcount', '10');
    const rows = screen.getAllByRole('row');
    expect(rows.map((r) => r.getAttribute('aria-rowindex'))).toEqual(['1', '5', '6', '7']);
  });

  it('leaves aria-rowindex off when every row is in the DOM', () => {
    render(<DataTable {...base} keyboardNavigation />);
    for (const row of screen.getAllByRole('row')) expect(row).not.toHaveAttribute('aria-rowindex');
  });

  it('formats default Date cells with the locale in UTC on the server, and never throws on an invalid date', () => {
    type Visit = { id: string; at: Date };
    const html = renderToString(
      <DataTable<Visit>
        columns={[{ id: 'at', header: 'When' }]}
        rows={[
          { id: 'a', at: new Date('2024-01-02T23:30:00Z') },
          { id: 'b', at: new Date('nope') },
        ]}
        getRowId={(r) => r.id}
        caption="Visits"
        locale="en-GB"
      />,
    );
    expect(html).toContain('02/01/2024');
    expect(html).not.toContain('Invalid Date');
  });

  it('adds no stray attribute to inline row action buttons', () => {
    render(
      <DataTable
        {...base}
        rowActions={() => [{ id: 'remove', label: 'Remove', inline: true, onSelect: () => {} }]}
      />,
    );
    const button = screen.getByRole('button', { name: 'Remove Cobalt pilot' });
    expect(button.getAttributeNames().filter((n) => n === 'data-')).toEqual([]);
  });

  it('renders the card layout with labelled fields, selection and an open button', async () => {
    const onOpen = vi.fn();
    render(<DataTable {...base} layout="cards" selectable onRowOpen={(r) => onOpen(r.id)} />);
    const list = screen.getByRole('list', { name: 'Deals' });
    expect(within(list).getAllByRole('listitem')).toHaveLength(3);
    expect(within(list).getAllByText('Stage')).toHaveLength(3);
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Open Cobalt pilot' }));
    expect(onOpen).toHaveBeenCalledWith('d2');
    await user.click(screen.getByRole('checkbox', { name: 'Select Cobalt pilot' }));
    expect(onOpen).toHaveBeenCalledTimes(1);
    await expectNoA11yViolations();
  });

  it('column chooser hides and reorders columns by button, keeping focus', async () => {
    function Demo() {
      const cols = useDataTableColumns(COLUMNS);
      return (
        <>
          <DataTableColumnChooser {...cols.chooserProps} />
          <DataTable {...base} {...cols.tableProps} />
        </>
      );
    }
    render(<Demo />);
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Columns' }));
    const dialog = await screen.findByRole('dialog', { name: 'Show columns' });
    await user.click(within(dialog).getByRole('checkbox', { name: 'Stage' }));
    expect(screen.queryByRole('columnheader', { name: /Stage/ })).toBeNull();
    await user.click(within(dialog).getByRole('button', { name: 'Move Name down' }));
    await user.click(within(dialog).getByRole('button', { name: 'Move Name down' }));
    await waitFor(() =>
      expect(within(dialog).getByRole('button', { name: 'Move Name down' })).toHaveFocus(),
    );
    expect(screen.getAllByRole('columnheader')[0]).toHaveTextContent('Amount');
    await expectNoA11yViolations(dialog);
    await user.click(within(dialog).getByRole('button', { name: 'Reset columns' }));
    expect(screen.getAllByRole('columnheader')).toHaveLength(4);
  });

  it('anchors a Shift range on the row, not its position, after the order changes', async () => {
    const onChange = vi.fn();
    render(<DataTable {...base} selectable onSelectedRowIdsChange={onChange} />);
    const user = userEvent.setup();
    await user.click(screen.getByRole('checkbox', { name: 'Select Fernway upgrade' }));
    // Amount descending: Fernway, Northfield, Cobalt. Fernway moved from last to first.
    await user.click(screen.getByRole('button', { name: /^Amount/ }));
    await user.click(screen.getByRole('button', { name: /^Amount/ }));
    expect(names()).toEqual(['Fernway upgrade', 'Northfield renewal', 'Cobalt pilot']);
    await user.keyboard('{Shift>}');
    await user.click(screen.getByRole('checkbox', { name: 'Select Northfield renewal' }));
    await user.keyboard('{/Shift}');
    expect([...(onChange.mock.lastCall?.[0] as string[])].sort()).toEqual(['d1', 'd3']);
  });

  it('renders on the server', () => {
    const html = renderToString(
      <DataTable {...base} selectable stickyFirstColumn rowActions={() => []} layout="auto" />,
    );
    expect(html).toContain('mn-data-table');
    expect(html).toContain('Northfield renewal');
    expect(html).toContain('data-view="table"');
  });
});

describe('Table scroll region', () => {
  function stubSizes(scrollWidth: number, clientWidth: number) {
    const proto = HTMLElement.prototype;
    const sw = Object.getOwnPropertyDescriptor(proto, 'scrollWidth');
    const cw = Object.getOwnPropertyDescriptor(proto, 'clientWidth');
    Object.defineProperty(proto, 'scrollWidth', { configurable: true, get: () => scrollWidth });
    Object.defineProperty(proto, 'clientWidth', { configurable: true, get: () => clientWidth });
    const RO = globalThis.ResizeObserver;
    globalThis.ResizeObserver = class {
      observe() {}
      unobserve() {}
      disconnect() {}
    } as unknown as typeof ResizeObserver;
    return () => {
      if (sw) Object.defineProperty(proto, 'scrollWidth', sw);
      else delete (proto as { scrollWidth?: number }).scrollWidth;
      if (cw) Object.defineProperty(proto, 'clientWidth', cw);
      else delete (proto as { clientWidth?: number }).clientWidth;
      globalThis.ResizeObserver = RO;
    };
  }
  const table = (
    <Table aria-label="Invoices">
      <tbody>
        <tr>
          <td>One</td>
        </tr>
      </tbody>
    </Table>
  );

  it('is a focusable region named after the table while it overflows', async () => {
    const restore = stubSizes(800, 300);
    try {
      render(table);
      const region = await screen.findByRole('region', { name: 'Invoices' });
      expect(region).toHaveAttribute('tabindex', '0');
      await expectNoA11yViolations(region);
    } finally {
      restore();
    }
  });

  it('is a plain wrapper when it fits', () => {
    const restore = stubSizes(300, 300);
    try {
      const { container } = render(table);
      const wrap = container.querySelector('.mn-table-wrap') as HTMLElement;
      expect(wrap).not.toHaveAttribute('tabindex');
      expect(wrap).not.toHaveAttribute('role');
    } finally {
      restore();
    }
  });
});
