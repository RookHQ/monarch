import { useState } from 'react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { useDataTableColumns } from '../data-table/index.js';
import { DataGrid, type DataGridColumn, type DataGridProps } from './index.js';

interface Row {
  id: string;
  name: string;
  status: string;
  amount: number;
  due: string;
}

const STATUSES = ['Open', 'Paid', 'Overdue'];
const ROWS: Row[] = Array.from({ length: 10_000 }, (_, i) => ({
  id: `r${i}`,
  name: `Record ${String(i).padStart(5, '0')}`,
  status: STATUSES[i % 3] ?? 'Open',
  amount: (i * 37) % 1000,
  due: `2026-0${(i % 9) + 1}-15`,
}));

const COLUMNS: DataGridColumn<Row>[] = [
  { id: 'name', header: 'Name', sortable: true, hideable: false, width: 200 },
  {
    id: 'status',
    header: 'Status',
    editable: true,
    editor: 'select',
    options: STATUSES.map((s) => ({ value: s, label: s })),
  },
  { id: 'amount', header: 'Amount', numeric: true, sortable: true, editable: true, min: 0 },
  { id: 'due', header: 'Due', editable: true, editor: 'date' },
];

function Grid({ rows: initial, ...props }: Partial<DataGridProps<Row>> & { rows?: Row[] }) {
  const [rows, setRows] = useState(initial ?? ROWS);
  return (
    <DataGrid
      label="Records"
      columns={COLUMNS}
      rows={rows}
      getRowId={(r) => r.id}
      getRowLabel={(r) => r.name}
      onCellEdit={(row, col, value) =>
        setRows((all) => all.map((r) => (r.id === row.id ? { ...r, [col]: value } : r)))
      }
      {...props}
    />
  );
}

afterEach(() => vi.restoreAllMocks());

const activeCell = () => document.activeElement?.closest('[data-mn-cell]');

describe('DataGrid', () => {
  it('renders an APG grid with virtualized rows and one tab stop', async () => {
    const user = userEvent.setup();
    render(<Grid rows={ROWS.slice(0, 50)} data-testid="root" />);
    expect(screen.getByTestId('root')).toHaveClass('mn-data-grid');
    const grid = screen.getByRole('grid', { name: 'Records' });
    expect(grid).toHaveAttribute('aria-rowcount', '51');
    expect(grid).toHaveAttribute('aria-colcount', '4');
    const rows = within(grid).getAllByRole('row');
    // Header + the initial window (20 rows before layout is known).
    expect(rows).toHaveLength(21);
    expect(rows[1]).toHaveAttribute('aria-rowindex', '2');
    expect(within(rows[1] as HTMLElement).getByRole('rowheader')).toHaveTextContent('Record 00000');
    expect(screen.getAllByRole('columnheader').map((h) => h.textContent)).toEqual([
      'Name',
      'Status',
      'Amount',
      'Due',
    ]);
    await user.tab();
    expect(screen.getByRole('columnheader', { name: 'Name' })).toHaveFocus();
    await user.tab();
    expect(grid.contains(document.activeElement)).toBe(false);
    await expectNoA11yViolations();
  });

  it('keeps 10,000 rows out of the DOM and keeps the focused row mounted', async () => {
    const user = userEvent.setup();
    render(<Grid />);
    expect(screen.getByRole('grid')).toHaveAttribute('aria-rowcount', '10001');
    expect(screen.getAllByRole('row').length).toBeLessThan(30);
    await user.tab();
    await user.keyboard('{Control>}{End}{/Control}');
    await waitFor(() =>
      expect(activeCell()?.closest('[role="row"]')).toHaveAttribute('aria-rowindex', '10001'),
    );
    expect(screen.getAllByRole('row').length).toBeLessThan(30);
    await user.keyboard('{ArrowUp}');
    expect(activeCell()?.closest('[role="row"]')).toHaveAttribute('aria-rowindex', '10000');
    await user.keyboard('{Control>}{Home}{/Control}');
    expect(activeCell()).toHaveAttribute('role', 'columnheader');
  });

  it('moves between cells with arrows, Home, End and Ctrl+End', async () => {
    const user = userEvent.setup();
    render(<Grid rows={ROWS.slice(0, 30)} />);
    await user.tab();
    await user.keyboard('{ArrowDown}');
    expect(activeCell()).toHaveTextContent('Record 00000');
    await user.keyboard('{ArrowRight}');
    expect(activeCell()).toHaveAttribute('data-column-id', 'status');
    await user.keyboard('{End}');
    expect(activeCell()).toHaveAttribute('data-column-id', 'due');
    await user.keyboard('{Home}');
    expect(activeCell()).toHaveAttribute('data-column-id', 'name');
    await user.keyboard('{Control>}{End}{/Control}');
    expect(activeCell()?.closest('[role="row"]')).toHaveAttribute('aria-rowindex', '31');
    expect(activeCell()).toHaveAttribute('data-column-id', 'due');
    await user.keyboard('{Control>}{Home}{/Control}');
    expect(activeCell()).toHaveAttribute('role', 'columnheader');
    // Leaving and coming back returns to the last cell.
    await user.keyboard('{ArrowDown}{ArrowDown}');
    await user.tab({ shift: true });
    await user.tab();
    expect(activeCell()).toHaveTextContent('Record 00001');
  });

  it('sorts from the header with Enter and click, and reports aria-sort', async () => {
    const user = userEvent.setup();
    const onSortChange = vi.fn();
    render(<Grid rows={ROWS.slice(0, 5)} onSortChange={onSortChange} />);
    await user.tab();
    await user.keyboard('{Enter}');
    expect(onSortChange).toHaveBeenLastCalledWith([{ id: 'name', direction: 'ascending' }]);
    const name = screen.getByRole('columnheader', { name: 'Name' });
    expect(name).toHaveAttribute('aria-sort', 'ascending');
    await user.keyboard('{Enter}');
    expect(name).toHaveAttribute('aria-sort', 'descending');
    expect(screen.getAllByRole('rowheader')[0]).toHaveTextContent('Record 00004');
    await user.click(screen.getByText('Amount'));
    expect(onSortChange).toHaveBeenLastCalledWith([{ id: 'amount', direction: 'ascending' }]);
    expect(name).not.toHaveAttribute('aria-sort');
  });

  it('pins, hides and sorts from the column menu and returns focus to the header', async () => {
    const user = userEvent.setup();
    render(<Grid rows={ROWS.slice(0, 5)} />);
    await user.tab();
    await user.keyboard('{ArrowRight}{ArrowRight}');
    const amount = screen.getByRole('columnheader', { name: 'Amount' });
    expect(amount).toHaveFocus();
    await user.keyboard('{Alt>}{ArrowDown}{/Alt}');
    const menu = await screen.findByRole('menu');
    await user.click(within(menu).getByRole('menuitem', { name: 'Pin to start' }));
    await waitFor(() => expect(screen.queryByRole('menu')).toBeNull());
    expect(screen.getAllByRole('columnheader').map((h) => h.textContent)).toEqual([
      'Amount',
      'Name',
      'Status',
      'Due',
    ]);
    const pinned = screen.getByRole('columnheader', { name: 'Amount' });
    expect(pinned).toHaveAttribute('data-pinned', 'start');
    await waitFor(() => expect(activeCell()).toHaveAttribute('data-column-id', 'amount'));

    await user.click(screen.getByRole('button', { name: 'Due column options' }));
    await user.click(await screen.findByRole('menuitem', { name: 'Hide column' }));
    await waitFor(() => expect(screen.queryByRole('columnheader', { name: /Due/ })).toBeNull());

    await user.click(screen.getByRole('button', { name: 'Name column options' }));
    expect(screen.queryByRole('menuitem', { name: 'Hide column' })).toBeNull();
    await user.click(await screen.findByRole('menuitem', { name: 'Sort descending' }));
    expect(screen.getByRole('columnheader', { name: 'Name' })).toHaveAttribute(
      'aria-sort',
      'descending',
    );
  });

  it('resizes a column with Alt+Arrow keys', async () => {
    const user = userEvent.setup();
    const onColumnWidthsChange = vi.fn();
    render(<Grid rows={ROWS.slice(0, 5)} onColumnWidthsChange={onColumnWidthsChange} />);
    await user.tab();
    await user.keyboard('{Alt>}{ArrowRight}{/Alt}');
    expect(onColumnWidthsChange).toHaveBeenLastCalledWith({ name: 216 });
    await user.keyboard('{Alt>}{Shift>}{ArrowLeft}{/Shift}{/Alt}');
    expect(onColumnWidthsChange).toHaveBeenLastCalledWith({ name: 152 });
  });

  it('edits cells with Enter, F2 and typing; Escape cancels; validation blocks saving', async () => {
    const user = userEvent.setup();
    const onCellEdit = vi.fn();
    render(<Grid rows={ROWS.slice(0, 5)} onCellEdit={onCellEdit} />);
    await user.tab();
    await user.keyboard('{ArrowDown}{ArrowRight}{ArrowRight}');
    expect(activeCell()).toHaveAttribute('data-column-id', 'amount');
    // Typing starts editing with that character.
    await user.keyboard('7');
    const input = screen.getByRole('textbox', { name: 'Amount for Record 00000' });
    expect(input).toHaveFocus();
    expect(input).toHaveValue('7');
    await user.keyboard('5{Enter}');
    expect(onCellEdit).toHaveBeenCalledWith(ROWS[0], 'amount', 75);
    await waitFor(() => expect(activeCell()).toHaveAttribute('data-column-id', 'amount'));
    expect(document.activeElement).toBe(activeCell());

    await user.keyboard('{F2}');
    const again = screen.getByRole('textbox', { name: 'Amount for Record 00000' });
    await user.clear(again);
    await user.type(again, '-1{Enter}');
    expect(screen.getByRole('alert')).toHaveTextContent('Enter 0 or more.');
    await user.keyboard('{Escape}');
    expect(screen.queryByRole('textbox')).toBeNull();
    expect(activeCell()).toHaveAttribute('data-column-id', 'amount');

    await user.keyboard('{ArrowLeft}{Enter}');
    const select = screen.getByRole('combobox', { name: 'Status for Record 00000' });
    await user.selectOptions(select, 'Paid');
    await user.keyboard('{Enter}');
    expect(onCellEdit).toHaveBeenLastCalledWith(ROWS[0], 'status', 'Paid');
    await expectNoA11yViolations();
  });

  it('Tab inside an editor saves and moves to the next cell', async () => {
    const user = userEvent.setup();
    render(<Grid rows={ROWS.slice(0, 3)} />);
    await user.tab();
    await user.keyboard('{ArrowDown}{ArrowRight}{ArrowRight}{Enter}');
    const input = screen.getByRole('textbox', { name: 'Amount for Record 00000' });
    await user.clear(input);
    await user.type(input, '12');
    await user.tab();
    await waitFor(() => expect(activeCell()).toHaveAttribute('data-column-id', 'due'));
    await waitFor(() => expect(screen.getAllByRole('row')[1]).toHaveTextContent(/Open12/));
  });

  it('selects rows with Space, Shift+Space ranges, checkboxes and Ctrl+A', async () => {
    const user = userEvent.setup();
    const onSelectedRowIdsChange = vi.fn();
    render(
      <Grid
        rows={ROWS.slice(0, 6)}
        selectable
        onSelectedRowIdsChange={onSelectedRowIdsChange}
        isRowSelectable={(r) => r.id !== 'r5'}
      />,
    );
    const grid = screen.getByRole('grid');
    expect(grid).toHaveAttribute('aria-multiselectable', 'true');
    await user.tab();
    expect(screen.getByRole('checkbox', { name: 'Select all rows' })).toHaveFocus();
    await user.keyboard('{ArrowDown}{ArrowRight}[Space]');
    expect(onSelectedRowIdsChange).toHaveBeenLastCalledWith(['r0']);
    expect(screen.getAllByRole('row')[1]).toHaveAttribute('aria-selected', 'true');
    await user.keyboard('{ArrowDown}{ArrowDown}{Shift>}[Space]{/Shift}');
    expect(onSelectedRowIdsChange).toHaveBeenLastCalledWith(['r0', 'r1', 'r2']);
    await user.keyboard('{Control>}a{/Control}');
    expect(onSelectedRowIdsChange).toHaveBeenLastCalledWith(['r0', 'r1', 'r2', 'r3', 'r4']);
    expect(screen.getByRole('checkbox', { name: 'Select all rows' })).toBeChecked();
    await user.click(screen.getByRole('checkbox', { name: 'Select Record 00003' }));
    expect(onSelectedRowIdsChange).toHaveBeenLastCalledWith(['r0', 'r1', 'r2', 'r4']);
    expect(screen.getByRole('checkbox', { name: 'Select Record 00005' })).toBeDisabled();
    await expectNoA11yViolations();
  });

  it('anchors a Shift range on the row, not its position, after the order changes', async () => {
    const user = userEvent.setup();
    const onSelectedRowIdsChange = vi.fn();
    const rows = ROWS.slice(0, 4);
    const { rerender } = render(
      <Grid rows={rows} selectable onSelectedRowIdsChange={onSelectedRowIdsChange} />,
    );
    await user.tab();
    await user.keyboard('{ArrowDown}{ArrowRight}[Space]');
    expect(onSelectedRowIdsChange).toHaveBeenLastCalledWith(['r0']);
    // Name descending: r3, r2, r1, r0. The toggled row moved from first to last.
    rerender(
      <Grid
        rows={rows}
        selectable
        onSelectedRowIdsChange={onSelectedRowIdsChange}
        sort={[{ id: 'name', direction: 'descending' }]}
      />,
    );
    expect(activeCell()?.closest('[role="row"]')).toHaveAttribute('data-row-id', 'r0');
    await user.keyboard('{ArrowUp}{Shift>}[Space]{/Shift}');
    expect([...(onSelectedRowIdsChange.mock.lastCall?.[0] as string[])].sort()).toEqual([
      'r0',
      'r1',
    ]);
  });

  it('opens rows and row action menus from the keyboard', async () => {
    const user = userEvent.setup();
    const onRowOpen = vi.fn();
    const onArchive = vi.fn();
    render(
      <Grid
        rows={ROWS.slice(0, 3)}
        onRowOpen={onRowOpen}
        rowActions={(r) => [
          { id: 'archive', label: 'Archive', onSelect: () => onArchive(r.id) },
          { id: 'delete', label: 'Delete', tone: 'danger', onSelect: () => {} },
        ]}
      />,
    );
    await user.tab();
    await user.keyboard('{ArrowDown}{Enter}');
    expect(onRowOpen).toHaveBeenCalledWith(ROWS[0]);
    await user.keyboard('{End}');
    const trigger = screen.getByRole('button', { name: 'Actions for Record 00000' });
    expect(trigger).toHaveFocus();
    // Arrow keys keep moving in the grid instead of opening the menu.
    await user.keyboard('{ArrowDown}');
    expect(screen.getByRole('button', { name: 'Actions for Record 00001' })).toHaveFocus();
    expect(screen.queryByRole('menu')).toBeNull();
    await user.keyboard('{Enter}');
    await user.click(await screen.findByRole('menuitem', { name: 'Archive' }));
    expect(onArchive).toHaveBeenCalledWith('r1');
  });

  it('works with useDataTableColumns for visibility and order', () => {
    function WithHook() {
      const cols = useDataTableColumns(COLUMNS, { visibility: { due: false } });
      return (
        <DataGrid
          label="Records"
          columns={COLUMNS}
          rows={ROWS.slice(0, 3)}
          getRowId={(r) => r.id}
          {...cols.tableProps}
          columnOrder={['amount', 'name', 'status', 'due']}
        />
      );
    }
    render(<WithHook />);
    expect(screen.getAllByRole('columnheader').map((h) => h.textContent)).toEqual([
      'Amount',
      'Name',
      'Status',
    ]);
  });

  it('shows loading and empty states', async () => {
    const { rerender } = render(<Grid rows={[]} loading />);
    expect(screen.getAllByText('Loading rows').length).toBeGreaterThan(0);
    rerender(<Grid rows={[]} />);
    expect(screen.getByText('Nothing here yet')).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('formats default Date cells in UTC on the server and never throws on an invalid date', () => {
    type Visit = { id: string; at: Date };
    const html = renderToString(
      <DataGrid<Visit>
        label="Visits"
        columns={[{ id: 'at', header: 'When' }]}
        rows={[
          { id: 'a', at: new Date('2024-01-02T23:30:00Z') },
          { id: 'b', at: new Date('nope') },
        ]}
        getRowId={(r) => r.id}
      />,
    );
    // en-US in UTC, whatever the zone and locale of the machine that renders.
    expect(html).toContain('1/2/2024');
    expect(html).not.toContain('Invalid Date');
  });

  it('renders on the server', () => {
    const html = renderToString(<Grid rows={ROWS.slice(0, 40)} />);
    expect(html).toContain('mn-data-grid');
    expect(html).toContain('role="grid"');
    expect(html).toContain('Record 00019');
    expect(html).not.toContain('Record 00020');
  });
});
