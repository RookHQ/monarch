import { describe, expect, it, vi } from 'vitest';
import { useState } from 'react';
import { act, render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { TimesheetGrid, type TimesheetDay, type TimesheetRow } from './index.js';

const DAYS: TimesheetDay[] = [
  { date: '2026-01-12', expectedMinutes: 480 },
  { date: '2026-01-13', today: true },
  { date: '2026-01-14', locked: 'Approved' },
  { date: '2026-01-17', nonWorking: true },
];
const ROWS: TimesheetRow[] = [
  {
    id: 'web',
    label: 'Website refresh',
    description: 'Design',
    cells: { '2026-01-12': 120, '2026-01-14': 60 },
  },
  { id: 'app', label: 'Mobile app', cells: { '2026-01-13': 90 } },
  { id: 'timer', label: 'Timer entries', readOnly: true, cells: { '2026-01-12': 30 } },
];

function Editable(props: { onCellChange?: (r: string, d: string, m: number) => unknown }) {
  const [rows, setRows] = useState(ROWS);
  return (
    <TimesheetGrid
      days={DAYS}
      rows={rows}
      locale="en-GB"
      onCellChange={(rowId, date, minutes) => {
        const result = props.onCellChange?.(rowId, date, minutes);
        const apply = () =>
          setRows((rs) =>
            rs.map((r) => (r.id === rowId ? { ...r, cells: { ...r.cells, [date]: minutes } } : r)),
          );
        if (result instanceof Promise) return result.then(apply);
        apply();
        return undefined;
      }}
    />
  );
}

const cell = (name: string) => screen.getByRole('textbox', { name });

describe('TimesheetGrid', () => {
  it('renders rows, days, totals and locked days with accessible names', async () => {
    render(
      <TimesheetGrid
        days={DAYS}
        rows={ROWS}
        locale="en-GB"
        onCellChange={() => {}}
        data-testid="g"
      />,
    );
    expect(screen.getByTestId('g')).toHaveClass('mn-timesheet-grid');
    const table = screen.getByRole('table');
    expect(screen.getByRole('region', { name: /Time per row and day/ })).toBeInTheDocument();
    expect(within(table).getByRole('columnheader', { name: /Tue 13/ })).toHaveAttribute(
      'aria-current',
      'date',
    );
    expect(within(table).getByRole('columnheader', { name: /Wed 14.*Approved/ })).toHaveAttribute(
      'data-locked',
    );
    expect(cell('Website refresh, Mon 12 Jan')).toHaveValue('2h');
    // Locked day and read-only row have no inputs.
    expect(screen.queryByRole('textbox', { name: 'Website refresh, Wed 14 Jan' })).toBeNull();
    expect(screen.queryByRole('textbox', { name: 'Timer entries, Mon 12 Jan' })).toBeNull();
    const rows = within(table).getAllByRole('row');
    expect(rows[1]).toHaveTextContent('3h'); // Website refresh total
    const footer = rows[rows.length - 1]!;
    expect(footer).toHaveTextContent('Day total2h 30mof 8h1h 30m1h–5h');
    await expectNoA11yViolations();
  });

  it('commits typed durations in any format and updates totals', async () => {
    const onCellChange = vi.fn();
    render(<Editable onCellChange={onCellChange} />);
    const user = userEvent.setup();
    const input = cell('Mobile app, Mon 12 Jan');
    await user.click(input);
    await user.keyboard('1:30{Enter}');
    expect(onCellChange).toHaveBeenLastCalledWith('app', '2026-01-12', 90);
    expect(input).toHaveValue('1h 30m');
    // Enter moves down to the next editable cell (the timer row is skipped, so focus stays put).
    await user.click(cell('Website refresh, Tue 13 Jan'));
    await user.keyboard('1.5h{Tab}');
    expect(onCellChange).toHaveBeenLastCalledWith('web', '2026-01-13', 90);
    await user.click(cell('Website refresh, Mon 12 Jan'));
    await user.clear(cell('Website refresh, Mon 12 Jan'));
    await user.tab();
    expect(onCellChange).toHaveBeenLastCalledWith('web', '2026-01-12', 0);
  });

  it('rejects unreadable and too-long values with a message, Escape reverts', async () => {
    const onCellChange = vi.fn();
    render(<Editable onCellChange={onCellChange} />);
    const user = userEvent.setup();
    const input = cell('Mobile app, Mon 12 Jan');
    await user.click(input);
    await user.keyboard('lots{Enter}');
    expect(onCellChange).not.toHaveBeenCalled();
    expect(input).toHaveAttribute('aria-invalid', 'true');
    expect(screen.getByRole('status')).toHaveTextContent('Use a time such as 1:30, 1.5 or 90m.');
    await user.clear(input);
    await user.keyboard('25h{Enter}');
    expect(screen.getByRole('status')).toHaveTextContent('A day has at most 24h.');
    await user.keyboard('{Escape}');
    expect(input).toHaveValue('');
    expect(input).not.toHaveAttribute('aria-invalid');
    expect(screen.getByRole('status')).toBeEmptyDOMElement();
  });

  it('shows saving and failure from a returned promise', async () => {
    let reject!: (e: Error) => void;
    render(<Editable onCellChange={() => new Promise((_, r) => (reject = r))} />);
    const user = userEvent.setup();
    const input = cell('Mobile app, Mon 12 Jan');
    await user.click(input);
    await user.keyboard('2{Enter}');
    expect(input).toHaveAttribute('aria-busy', 'true');
    await act(async () => reject(new Error('Day is closed')));
    expect(input).not.toHaveAttribute('aria-busy');
    expect(input).toHaveValue('2');
    expect(screen.getByRole('status')).toHaveTextContent('Day is closed');
  });

  it('moves between cells with the arrow keys, skipping locked cells', async () => {
    render(<Editable />);
    const user = userEvent.setup();
    await user.click(cell('Website refresh, Mon 12 Jan'));
    await user.keyboard('{ArrowRight}');
    expect(cell('Website refresh, Tue 13 Jan')).toHaveFocus();
    await user.keyboard('{ArrowRight}');
    // Wed is locked, so focus jumps to Sat.
    expect(cell('Website refresh, Sat 17 Jan')).toHaveFocus();
    await user.keyboard('{ArrowDown}');
    expect(cell('Mobile app, Sat 17 Jan')).toHaveFocus();
    await user.keyboard('{ArrowUp}{ArrowLeft}');
    expect(cell('Website refresh, Tue 13 Jan')).toHaveFocus();
  });

  it('is read-only without onCellChange, with activatable cells and days', async () => {
    const onCellActivate = vi.fn();
    const onDayActivate = vi.fn();
    render(
      <TimesheetGrid
        days={DAYS}
        rows={ROWS}
        locale="en-GB"
        onCellActivate={onCellActivate}
        onDayActivate={onDayActivate}
        rowActions={(row) => <button type="button">Clear {row.id}</button>}
        summaryRows={[
          { id: 'att', label: 'Attendance', cells: { '2026-01-12': '7h 50m' }, total: '7h 50m' },
        ]}
      />,
    );
    expect(screen.queryByRole('textbox')).toBeNull();
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: '2h, Website refresh, Mon 12 Jan' }));
    expect(onCellActivate).toHaveBeenCalledWith('web', '2026-01-12');
    await user.click(screen.getByRole('button', { name: 'Mon 12' }));
    expect(onDayActivate).toHaveBeenCalledWith('2026-01-12');
    expect(
      screen.getByRole('button', { name: '1h, Website refresh, Wed 14 Jan, Approved' }),
    ).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Clear web' })).toBeInTheDocument();
    expect(screen.getByRole('rowheader', { name: 'Attendance' })).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('flags a day above the daily limit and shows an empty message', () => {
    const { rerender } = render(
      <TimesheetGrid days={DAYS.slice(0, 1)} rows={ROWS} maxMinutesPerDay={60} />,
    );
    const over = document.querySelector('[data-over]');
    expect(over).toHaveTextContent('2h 30m (over the daily limit)');
    rerender(<TimesheetGrid days={DAYS} rows={[]} emptyMessage="Nothing yet." />);
    expect(screen.getByText('Nothing yet.')).toBeInTheDocument();
  });

  it('accepts translated messages', () => {
    render(
      <TimesheetGrid
        days={DAYS.slice(0, 1)}
        rows={ROWS.slice(0, 1)}
        onCellChange={() => {}}
        messages={{ rowHeader: 'Projekt', cellLabel: (r, d) => `${r} am ${d}`, day: () => 'Mo' }}
      />,
    );
    expect(screen.getByRole('columnheader', { name: 'Projekt' })).toBeInTheDocument();
    expect(screen.getByRole('textbox', { name: 'Website refresh am Mo' })).toBeInTheDocument();
  });

  it('renders on the server', () => {
    const html = renderToString(<TimesheetGrid days={DAYS} rows={ROWS} onCellChange={() => {}} />);
    expect(html).toContain('mn-timesheet-grid');
    expect(html).toContain('2h');
  });
});
