import { describe, expect, it, vi } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import type { CalendarEvent } from '../event-chip/index.js';
import { AgendaList } from './index.js';

const EVENTS: CalendarEvent[] = [
  { id: 'b', title: 'Design review', start: '2026-11-04T14:00:00Z', end: '2026-11-04T15:00:00Z' },
  { id: 'a', title: 'Stand-up', start: '2026-11-04T09:00:00Z', end: '2026-11-04T09:15:00Z' },
  { id: 'c', title: 'Company offsite', start: '2026-11-04', end: '2026-11-05', allDay: true },
  {
    id: 'd',
    title: 'Vendor call',
    start: '2026-11-06T11:00:00Z',
    end: '2026-11-06T11:30:00Z',
    status: 'tentative',
    detail: 'Video call',
  },
];

const base = { timeZone: 'UTC', locale: 'en-GB', today: '2026-11-04' } as const;

// Many user-event steps plus axe: allow for a loaded CI runner.
describe('AgendaList', { timeout: 15_000 }, () => {
  it('groups events by day, all-day first, with headings, counts and a Today marker', async () => {
    render(<AgendaList {...base} events={EVENTS} aria-label="Agenda" />);
    const days = screen.getAllByRole('region');
    expect(days.map((d) => d.querySelector('h3')?.textContent)).toEqual([
      'Wednesday 4 November, Today, 3 events',
      'Thursday 5 November, 1 event',
      'Friday 6 November, 1 event',
    ]);
    expect(days[0]).toHaveAttribute('data-today');
    const first = within(days[0] as HTMLElement).getAllByRole('button');
    expect(first.map((b) => b.getAttribute('aria-label'))).toEqual([
      'Company offsite, All day',
      'Stand-up, 09:00 – 09:15',
      'Design review, 14:00 – 15:00',
    ]);
    expect(
      screen.getByRole('button', { name: 'Vendor call, 11:00 – 11:30, Tentative, Video call' }),
    ).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('moves between events with arrow keys, Home/End and Page Up/Down, one tab stop', async () => {
    const onEventClick = vi.fn();
    render(<AgendaList {...base} events={EVENTS} onEventClick={onEventClick} />);
    const user = userEvent.setup();
    await user.tab();
    const [offsite] = screen.getAllByRole('button', { name: 'Company offsite, All day' });
    expect(offsite).toHaveFocus();
    expect(screen.getAllByRole('button').filter((b) => b.tabIndex === 0)).toHaveLength(1);
    await user.keyboard('{ArrowDown}');
    expect(screen.getByRole('button', { name: /^Stand-up/ })).toHaveFocus();
    await user.keyboard('{PageDown}');
    expect(screen.getAllByRole('button', { name: 'Company offsite, All day' })[1]).toHaveFocus();
    await user.keyboard('{PageUp}');
    expect(screen.getAllByRole('button', { name: 'Company offsite, All day' })[0]).toHaveFocus();
    await user.keyboard('{End}');
    const vendor = screen.getByRole('button', { name: /^Vendor call/ });
    expect(vendor).toHaveFocus();
    await user.keyboard('{ArrowDown}');
    expect(vendor).toHaveFocus();
    await user.keyboard('{Enter}');
    expect(onEventClick).toHaveBeenCalledWith(EVENTS[3]);
    await user.keyboard('{Home}');
    expect(screen.getAllByRole('button')[0]).toHaveFocus();
    // Tab leaves the list: there is a single tab stop.
    await user.tab();
    expect(document.body).toHaveFocus();
  });

  it('starts the tab stop at today or later', async () => {
    render(<AgendaList {...base} today="2026-11-06" events={EVENTS} />);
    const user = userEvent.setup();
    await user.tab();
    expect(screen.getByRole('button', { name: /^Vendor call/ })).toHaveFocus();
  });

  it('groups in the given time zone and shows a continuing event’s end only', () => {
    render(
      <AgendaList
        timeZone="Asia/Kolkata"
        locale="en-GB"
        today={null}
        events={[
          {
            id: 'late',
            title: 'Late sync',
            start: '2026-11-04T20:00:00Z',
            end: '2026-11-04T21:00:00Z',
          },
          {
            id: 'night',
            title: 'Night shift',
            start: '2026-11-05T16:30:00Z',
            end: '2026-11-06T02:30:00Z',
          },
        ]}
      />,
    );
    const headings = screen.getAllByRole('heading').map((h) => h.textContent);
    expect(headings).toEqual(['Thursday 5 November, 2 events', 'Friday 6 November, 1 event']);
    expect(screen.getByRole('button', { name: 'Late sync, 01:30 – 02:30' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Night shift, 22:00 – 08:00' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Night shift, – 08:00' })).toBeInTheDocument();
  });

  it('covers a fixed range with notes and optional empty days', () => {
    render(
      <AgendaList
        {...base}
        from="2026-11-03"
        days={4}
        showEmptyDays
        headingLevel={2}
        events={EVENTS}
        dayNotes={(day) => (day === '2026-11-05' ? ['Time off · Priya'] : undefined)}
        labels={{ count: (n) => `${n} items`, today: 'Now' }}
      />,
    );
    const headings = screen.getAllByRole('heading', { level: 2 }).map((h) => h.textContent);
    expect(headings).toEqual([
      'Tuesday 3 November, 0 items',
      'Wednesday 4 November, Now, 3 items',
      'Thursday 5 November, 1 items',
      'Friday 6 November, 1 items',
    ]);
    expect(screen.getByText('Time off · Priya')).toHaveClass('mn-agenda-list__note');
  });

  it('shows an empty state, or the one supplied', () => {
    const { rerender } = render(<AgendaList {...base} events={[]} />);
    expect(screen.getByRole('heading', { name: 'Nothing scheduled' })).toBeInTheDocument();
    rerender(<AgendaList {...base} events={[]} empty={<p>No bookings this week</p>} />);
    expect(screen.getByText('No bookings this week')).toBeInTheDocument();
  });

  it('wraps chips with renderEvent', () => {
    render(
      <AgendaList
        {...base}
        events={EVENTS.slice(0, 1)}
        renderEvent={(e, chip) => <div data-testid={`wrap-${e.id}`}>{chip}</div>}
      />,
    );
    expect(within(screen.getByTestId('wrap-b')).getByRole('button')).toHaveAccessibleName(
      'Design review, 14:00 – 15:00',
    );
  });

  it('renders on the server', () => {
    const html = renderToString(<AgendaList {...base} events={EVENTS} />);
    expect(html).toContain('mn-agenda-list');
    expect(html).toContain('Design review');
  });
});
