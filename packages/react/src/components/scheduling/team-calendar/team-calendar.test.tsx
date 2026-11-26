import { describe, expect, it, vi } from 'vitest';
import { act, render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { hydrateRoot } from 'react-dom/client';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import {
  TeamCalendar,
  type TeamCalendarEntry,
  type TeamCalendarMember,
  type TeamCalendarProps,
} from './index.js';
import { hydrationErrors } from '../../../../test/hydration.js';

const MEMBERS: TeamCalendarMember[] = [
  { id: 'a', name: 'Alex Kim', description: 'Design' },
  { id: 'b', name: 'Sam Lee' },
];
const ENTRIES: TeamCalendarEntry[] = [
  { id: 'l1', memberId: 'a', start: '2026-11-03', end: '2026-11-05', label: 'Annual leave' },
  {
    id: 'l2',
    memberId: 'b',
    start: '2026-11-04',
    label: 'Sick leave',
    portion: 'first-half',
    pending: true,
  },
  { id: 'l3', memberId: 'b', start: '2026-10-30', end: '2026-11-02', label: 'Away' },
];

function Cal(props: Partial<TeamCalendarProps>) {
  return (
    <TeamCalendar
      members={MEMBERS}
      entries={ENTRIES}
      month="2026-11-01"
      today="2026-11-04"
      locale="en-GB"
      holidays={[{ date: '2026-11-11', name: 'Remembrance' }]}
      {...props}
    />
  );
}

describe('TeamCalendar', () => {
  it('renders a named grid of people by days with entries and no axe violations', async () => {
    render(<Cal />);
    const grid = screen.getByRole('grid', { name: 'Team calendar, November 2026' });
    expect(within(grid).getAllByRole('columnheader')).toHaveLength(31);
    expect(within(grid).getByRole('rowheader', { name: /Alex Kim/ })).toBeInTheDocument();
    expect(within(grid).getAllByText('Annual leave')).toHaveLength(3);
    expect(within(grid).getByText('Sick leave, first half, pending')).toBeInTheDocument();
    // Entries clipped to the month.
    expect(within(grid).getAllByText('Away')).toHaveLength(2);
    expect(within(grid).getByText('Holiday: Remembrance')).toBeInTheDocument();
    expect(screen.getByText(/Shaded columns are weekends/)).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('shades weekends and holidays and marks today', () => {
    const { container } = render(<Cal />);
    const headers = container.querySelectorAll('thead th.mn-team-calendar__day');
    expect(headers[0]).toHaveAttribute('data-shaded'); // Sunday 1 November
    expect(headers[1]).not.toHaveAttribute('data-shaded');
    expect(headers[10]).toHaveAttribute('data-shaded'); // holiday
    expect(headers[3]).toHaveAttribute('data-today');
  });

  it('mirrors the left and right arrows in RTL', async () => {
    const { container } = render(
      <div dir="rtl">
        <Cal />
      </div>,
    );
    const user = userEvent.setup();
    await user.tab();
    await user.keyboard('{ArrowLeft}{ArrowLeft}');
    expect(container.querySelector('[data-cell="0:2"]')).toHaveFocus();
    await user.keyboard('{ArrowRight}');
    expect(container.querySelector('[data-cell="0:1"]')).toHaveFocus();
  });

  it('is one tab stop with arrow, Home/End and Page keys; Enter activates', async () => {
    const onEntryClick = vi.fn();
    const onCellClick = vi.fn();
    const { container } = render(<Cal onEntryClick={onEntryClick} onCellClick={onCellClick} />);
    const user = userEvent.setup();
    expect(container.querySelectorAll('td[tabindex="0"]')).toHaveLength(1);
    await user.tab();
    const first = container.querySelector('[data-cell="0:0"]')!;
    expect(first).toHaveFocus();
    await user.keyboard('{ArrowRight}{ArrowRight}');
    expect(container.querySelector('[data-cell="0:2"]')).toHaveFocus();
    await user.keyboard('{Enter}');
    expect(onEntryClick).toHaveBeenCalledWith(ENTRIES[0], MEMBERS[0], '2026-11-03');
    await user.keyboard('{ArrowDown}{End}');
    expect(container.querySelector('[data-cell="1:29"]')).toHaveFocus();
    await user.keyboard('{PageUp} ');
    expect(onCellClick).toHaveBeenCalledWith(MEMBERS[1], '2026-11-23');
    await user.keyboard('{Control>}{Home}{/Control}');
    expect(first).toHaveFocus();
  });

  it('activates on click', async () => {
    const onCellClick = vi.fn();
    const { container } = render(<Cal onCellClick={onCellClick} />);
    await userEvent.setup().click(container.querySelector('[data-cell="1:9"]')!);
    expect(onCellClick).toHaveBeenCalledWith(MEMBERS[1], '2026-11-10');
  });

  it('exposes day cells as grid cells', () => {
    render(<Cal />);
    expect(screen.getAllByRole('gridcell')).toHaveLength(MEMBERS.length * 30);
  });

  it('supports a custom range and per-day portions', () => {
    render(
      <Cal
        from="2026-11-02"
        to="2026-11-08"
        entries={[
          {
            id: 'x',
            memberId: 'a',
            start: '2026-11-02',
            end: '2026-11-03',
            label: 'Training',
            portions: { '2026-11-03': 'second-half' },
          },
        ]}
      />,
    );
    expect(
      screen.getByRole('grid', { name: 'Team calendar, 2 Nov – 8 Nov 2026' }),
    ).toBeInTheDocument();
    expect(screen.getAllByRole('columnheader')).toHaveLength(8);
    expect(screen.getByText('Training, second half')).toBeInTheDocument();
  });

  it('shows loading, error and empty states', () => {
    const { rerender, container } = render(<Cal loading />);
    expect(container.firstElementChild).toHaveAttribute('aria-busy', 'true');
    expect(screen.queryByRole('grid')).not.toBeInTheDocument();
    rerender(<Cal error="Could not load the calendar." />);
    expect(screen.getByText('Could not load the calendar.')).toBeInTheDocument();
    rerender(<Cal members={[]} />);
    expect(screen.getByText('Nobody to show')).toBeInTheDocument();
  });

  it('leaves out an entry whose days are not dates instead of throwing', () => {
    render(
      <Cal
        entries={[
          ...ENTRIES,
          { id: 'bad', memberId: 'a', start: '2026-11-20T00:00:00Z', label: 'Broken' },
          { id: 'bad-end', memberId: 'a', start: '2026-11-21', end: 'x', label: 'Broken end' },
        ]}
      />,
    );
    expect(screen.getByRole('grid')).toBeInTheDocument();
    expect(screen.queryByText(/Broken/)).not.toBeInTheDocument();
  });

  it('hydrates without a mismatch when the server and browser locales differ', async () => {
    let machineLocale = 'en-US';
    const original = Intl.DateTimeFormat.prototype.resolvedOptions;
    const spy = vi
      .spyOn(Intl.DateTimeFormat.prototype, 'resolvedOptions')
      .mockImplementation(function (this: Intl.DateTimeFormat) {
        return { ...original.call(this), locale: machineLocale };
      });
    const error = vi.spyOn(console, 'error').mockImplementation(() => {});
    const ui = <Cal locale={undefined} />;
    const container = document.createElement('div');
    try {
      const html = renderToString(ui);
      expect(html).toContain('Sunday, November 1');
      machineLocale = 'en-GB';
      container.innerHTML = html;
      document.body.append(container);
      let root: ReturnType<typeof hydrateRoot> | undefined;
      await act(async () => {
        root = hydrateRoot(container, ui);
      });
      expect(hydrationErrors(error)).toEqual([]);
      expect(container.innerHTML).toContain('Sunday 1 November');
      act(() => root?.unmount());
    } finally {
      container.remove();
      spy.mockRestore();
      error.mockRestore();
    }
  });

  it('renders on the server', () => {
    expect(renderToString(<Cal />)).toContain('mn-team-calendar');
  });
});
