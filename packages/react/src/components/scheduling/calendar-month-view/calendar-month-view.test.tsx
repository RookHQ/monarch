import { describe, expect, it, vi } from 'vitest';
import { useState } from 'react';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import type { CalendarEvent } from '../event-chip/index.js';
import { CalendarMonthView } from './index.js';

const at = (day: string, time: string) => `${day}T${time}:00Z`;
const EVENTS: CalendarEvent[] = [
  { id: '1', title: 'Stand-up', start: at('2026-11-04', '09:00'), end: at('2026-11-04', '09:15') },
  {
    id: '2',
    title: 'Design review',
    start: at('2026-11-04', '11:00'),
    end: at('2026-11-04', '12:00'),
  },
  {
    id: '3',
    title: 'Lunch & learn',
    start: at('2026-11-04', '12:30'),
    end: at('2026-11-04', '13:30'),
  },
  {
    id: '4',
    title: 'Retro',
    start: at('2026-11-04', '16:00'),
    end: at('2026-11-04', '17:00'),
    status: 'cancelled',
  },
  { id: '5', title: 'Release day', start: '2026-11-04', allDay: true },
  { id: '6', title: 'Planning', start: at('2026-11-12', '10:00'), end: at('2026-11-12', '11:00') },
];

const base = { timeZone: 'UTC', locale: 'en-GB', today: '2026-11-04', weekStartsOn: 1 } as const;

// Many user-event steps plus axe: allow for a loaded CI runner.
describe('CalendarMonthView', { timeout: 15_000 }, () => {
  it('is a grid of weeks with weekday headers, today and outside-month days', async () => {
    render(<CalendarMonthView {...base} month="2026-11-01" events={EVENTS} />);
    const grid = screen.getByRole('grid', { name: 'November 2026' });
    const rows = within(grid).getAllByRole('row');
    expect(rows).toHaveLength(7);
    expect(
      within(rows[0] as HTMLElement)
        .getAllByRole('columnheader')
        .map((c) => c.textContent),
    ).toEqual([
      'MonMonday',
      'TueTuesday',
      'WedWednesday',
      'ThuThursday',
      'FriFriday',
      'SatSaturday',
      'SunSunday',
    ]);
    const cells = within(grid).getAllByRole('gridcell');
    expect(cells).toHaveLength(42);
    expect(cells[0]).toHaveAttribute('data-day', '2026-10-26');
    expect(cells[0]).toHaveAttribute('data-outside');
    const today = screen.getByRole('button', {
      name: 'Wednesday, 4 November 2026, 5 events, today',
    });
    expect(today).toHaveAttribute('aria-current', 'date');
    expect(today.closest('[role="gridcell"]')).toHaveAttribute('data-today');
    await expectNoA11yViolations();
  });

  it('starts the week on the given day and can show only the weeks needed', () => {
    render(<CalendarMonthView {...base} weekStartsOn={0} fixedWeeks={false} month="2026-11-20" />);
    const cells = screen.getAllByRole('gridcell');
    expect(cells[0]).toHaveAttribute('data-day', '2026-11-01');
    expect(cells).toHaveLength(35);
    expect(screen.getAllByRole('columnheader')[0]).toHaveTextContent('Sunday');
  });

  it('lists all-day events first, caps the cell and opens the rest in a popover', async () => {
    const onEventClick = vi.fn();
    render(
      <CalendarMonthView
        {...base}
        month="2026-11-01"
        events={EVENTS}
        onEventClick={onEventClick}
      />,
    );
    const cell = screen
      .getByRole('button', { name: /^Wednesday, 4 November/ })
      .closest('[role="gridcell"]') as HTMLElement;
    const chips = within(cell)
      .getAllByRole('button')
      .filter((b) => b.classList.contains('mn-event-chip'));
    expect(chips.map((c) => c.getAttribute('aria-label'))).toEqual([
      'Release day',
      'Stand-up, 09:00',
    ]);
    const user = userEvent.setup();
    const more = within(cell).getByRole('button', { name: '+3 more' });
    await user.click(more);
    const dialog = screen.getByRole('dialog', { name: 'Wednesday, 4 November 2026' });
    const listed = within(dialog).getAllByRole('button');
    expect(listed).toHaveLength(5);
    expect(listed[4]).toHaveAccessibleName('Retro, 16:00, Cancelled');
    await user.click(listed[2] as HTMLElement);
    expect(onEventClick).toHaveBeenCalledWith(EVENTS[1]);
    await user.keyboard('{Escape}');
    expect(screen.queryByRole('dialog')).toBeNull();
    expect(more).toHaveFocus();
  });

  it('calls back when a day is chosen', async () => {
    const onDayClick = vi.fn();
    render(<CalendarMonthView {...base} month="2026-11-01" onDayClick={onDayClick} />);
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: /^Friday, 13 November 2026, 0 events$/ }));
    expect(onDayClick).toHaveBeenCalledWith('2026-11-13');
  });

  it('moves between days with the arrow keys, Home/End, with one tab stop', async () => {
    const onDayClick = vi.fn();
    render(
      <CalendarMonthView {...base} month="2026-11-01" events={EVENTS} onDayClick={onDayClick} />,
    );
    const user = userEvent.setup();
    await user.tab();
    const today = screen.getByRole('button', { name: /^Wednesday, 4 November/ });
    expect(today).toHaveFocus();
    await user.keyboard('{ArrowRight}');
    expect(screen.getByRole('button', { name: /^Thursday, 5 November/ })).toHaveFocus();
    await user.keyboard('{ArrowDown}');
    expect(screen.getByRole('button', { name: /^Thursday, 12 November/ })).toHaveFocus();
    await user.keyboard('{Home}');
    expect(screen.getByRole('button', { name: /^Monday, 9 November/ })).toHaveFocus();
    await user.keyboard('{End}');
    expect(screen.getByRole('button', { name: /^Sunday, 15 November/ })).toHaveFocus();
    await user.keyboard('{ArrowLeft}{ArrowUp}');
    expect(screen.getByRole('button', { name: /^Saturday, 7 November/ })).toHaveFocus();
    await user.keyboard('{Enter}');
    expect(onDayClick).toHaveBeenCalledWith('2026-11-07');
    // Only the focused day's controls are in the tab order.
    const tabbable = screen.getAllByRole('button').filter((b) => b.tabIndex === 0);
    expect(tabbable).toHaveLength(1);
    // Without onNavigate, focus stops at the edge of the shown weeks.
    await user.keyboard('{ArrowUp}{ArrowUp}{ArrowUp}');
    expect(screen.getByRole('button', { name: /^Saturday, 31 October/ })).toHaveFocus();
  });

  it('makes the focused day’s events reachable with Tab', async () => {
    render(<CalendarMonthView {...base} month="2026-11-01" events={EVENTS} />);
    const user = userEvent.setup();
    await user.tab();
    await user.tab();
    expect(screen.getByRole('button', { name: 'Release day' })).toHaveFocus();
    await user.tab();
    await user.tab();
    expect(screen.getByRole('button', { name: '+3 more' })).toHaveFocus();
    // The narrow-layout summary (hidden by a container query in browsers).
    await user.tab();
    expect(
      screen.getByRole('button', { name: '5 events, Wednesday, 4 November 2026' }),
    ).toHaveFocus();
    await user.tab();
    expect(document.body).toHaveFocus();
    expect(screen.getByRole('button', { name: 'Planning, 10:00' })).toHaveAttribute(
      'tabindex',
      '-1',
    );
  });

  it('asks the product to change month at the edges and follows it (Page Down)', async () => {
    function Calendar() {
      const [month, setMonth] = useState('2026-11-01');
      return (
        <CalendarMonthView
          {...base}
          month={month}
          onNavigate={(day) => setMonth(day)}
          aria-label="Bookings"
        />
      );
    }
    render(<Calendar />);
    const user = userEvent.setup();
    await user.tab();
    await user.keyboard('{PageDown}');
    expect(screen.getByRole('grid', { name: 'Bookings' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /^Friday, 4 December 2026/ })).toHaveFocus();
    expect(screen.getAllByRole('gridcell')[0]).toHaveAttribute('data-day', '2026-11-30');
    await user.keyboard('{PageUp}{PageUp}');
    expect(screen.getByRole('button', { name: /^Sunday, 4 October 2026/ })).toHaveFocus();
  });

  it('decorates days from dayState and mirrors arrows in right-to-left', async () => {
    render(
      <div dir="rtl">
        <CalendarMonthView
          {...base}
          month="2026-11-01"
          dayState={(d) => (d === '2026-11-05' ? { hatched: true, badge: 'Holiday' } : undefined)}
        />
      </div>,
    );
    const thursday = screen.getByRole('button', {
      name: 'Thursday, 5 November 2026, 0 events, Holiday',
    });
    expect(thursday.closest('[role="gridcell"]')).toHaveAttribute('data-hatched');
    const user = userEvent.setup();
    await user.tab();
    await user.keyboard('{ArrowLeft}');
    expect(thursday).toHaveFocus();
  });

  it('marks no day as today with today={null}, and reads the clock when left out', () => {
    const { rerender } = render(<CalendarMonthView {...base} today={null} month="2026-11-01" />);
    expect(document.querySelector('[data-today]')).toBeNull();
    const { today: _today, ...rest } = base;
    void _today;
    rerender(<CalendarMonthView {...rest} month={new Date().toISOString().slice(0, 10)} />);
    expect(document.querySelector('[data-today]')).not.toBeNull();
  });

  it('renders on the server without the clock', () => {
    const { today: _today, ...rest } = base;
    void _today;
    const html = renderToString(<CalendarMonthView {...rest} month="2026-11-01" events={EVENTS} />);
    expect(html).toContain('mn-calendar-month-view');
    expect(html).not.toContain('data-today');
  });
});
