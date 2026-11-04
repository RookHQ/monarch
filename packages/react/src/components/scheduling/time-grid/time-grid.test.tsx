import { afterEach, describe, expect, it, vi } from 'vitest';
import { fireEvent, render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import type { CalendarEvent } from '../event-chip/index.js';
import { TimeGrid } from './index.js';

const WEEK = ['2026-11-02', '2026-11-03', '2026-11-04', '2026-11-05', '2026-11-06'];
const at = (day: string, time: string) => `${day}T${time}:00Z`;
const EVENTS: CalendarEvent[] = [
  {
    id: 'a',
    title: 'Design review',
    start: at('2026-11-04', '09:00'),
    end: at('2026-11-04', '10:00'),
  },
  { id: 'b', title: 'Interview', start: at('2026-11-04', '09:30'), end: at('2026-11-04', '10:30') },
  { id: 'c', title: 'Stand-up', start: at('2026-11-04', '11:00'), end: at('2026-11-04', '11:15') },
  { id: 'd', title: 'Conference', start: '2026-11-03', end: '2026-11-04', allDay: true },
];
const base = {
  timeZone: 'UTC',
  locale: 'en-GB',
  now: at('2026-11-04', '10:15'),
  'aria-label': 'Week of 2 November',
} as const;

const rect = (r: Partial<DOMRect>) =>
  ({
    x: 0,
    y: 0,
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
    width: 0,
    height: 0,
    toJSON: () => ({}),
    ...r,
  }) as DOMRect;

afterEach(() => vi.restoreAllMocks());

// jsdom has no PointerEvent; without it fireEvent drops clientX/pointerType.
if (typeof window.PointerEvent === 'undefined') {
  class PointerEventStub extends MouseEvent {
    pointerId: number;
    pointerType: string;
    constructor(type: string, init: PointerEventInit = {}) {
      super(type, init);
      this.pointerId = init.pointerId ?? 0;
      this.pointerType = init.pointerType ?? 'mouse';
    }
  }
  window.PointerEvent = PointerEventStub as unknown as typeof PointerEvent;
}

// Many user-event steps plus axe: allow for a loaded CI runner.
describe('TimeGrid', { timeout: 15_000 }, () => {
  it('lays out timed events by time and overlapping events in columns', async () => {
    render(<TimeGrid {...base} days={WEEK} events={EVENTS} />);
    const grid = screen.getByRole('group', { name: 'Week of 2 November' });
    expect(grid).toHaveClass('mn-time-grid');
    const wed = within(grid).getByRole('group', { name: 'Wednesday 4 November' });
    const review = within(wed).getByRole('button', { name: 'Design review, 09:00 – 10:00' });
    const box = review.closest('.mn-time-grid__event') as HTMLElement;
    expect(box.style.top).toBe('432px');
    expect(box.style.height).toBe('48px');
    expect(box.style.getPropertyValue('--mn-time-grid-col')).toBe('0');
    expect(box.style.getPropertyValue('--mn-time-grid-cols')).toBe('2');
    const interview = within(wed).getByRole('button', { name: 'Interview, 09:30 – 10:30' });
    expect(
      (interview.closest('.mn-time-grid__event') as HTMLElement).style.getPropertyValue(
        '--mn-time-grid-col',
      ),
    ).toBe('1');
    // A 15-minute event is compact: start time shown, full range in the name.
    const standup = within(wed).getByRole('button', { name: 'Stand-up, 11:00 – 11:15' });
    expect(standup).toHaveAttribute('data-density', 'compact');
    expect(
      (standup.closest('.mn-time-grid__event') as HTMLElement).style.getPropertyValue(
        '--mn-time-grid-cols',
      ),
    ).toBe('1');
    await expectNoA11yViolations();
  });

  it('shows all-day events in the all-day row and marks today with a now line', () => {
    const { container } = render(<TimeGrid {...base} days={WEEK} events={EVENTS} />);
    const tueAllDay = screen.getByRole('group', { name: 'Tuesday 3 November, All day' });
    expect(within(tueAllDay).getByRole('button', { name: 'Conference, All day' })).toHaveAttribute(
      'data-variant',
      'bar',
    );
    expect(
      within(screen.getByRole('group', { name: 'Wednesday 4 November, All day' })).getByRole(
        'button',
      ),
    ).toHaveAccessibleName('Conference, All day');
    const now = container.querySelectorAll('.mn-time-grid__now');
    expect(now).toHaveLength(1);
    expect((now[0] as HTMLElement).style.top).toBe(`${10.25 * 48}px`);
    expect(now[0]?.closest('[role="group"]')).toHaveAccessibleName('Wednesday 4 November');
    expect(screen.getByText('Wednesday 4 November, Today')).toHaveClass('mn-visually-hidden');
  });

  it('hides the now line with now={null}, and reads the clock after mount when left out', () => {
    const { container, rerender } = render(
      <TimeGrid {...base} now={null} days={WEEK} events={EVENTS} />,
    );
    expect(container.querySelector('.mn-time-grid__now')).toBeNull();
    const { now: _now, ...rest } = base;
    void _now;
    const today = new Date().toISOString().slice(0, 10);
    rerender(<TimeGrid {...rest} days={[today]} />);
    expect(container.querySelector('.mn-time-grid__day-head[data-today]')).not.toBeNull();
  });

  it('shades time outside working hours and draws unavailable blocks', () => {
    const { container } = render(
      <TimeGrid
        {...base}
        days={['2026-11-04', '2026-11-07']}
        workingHours={[
          { day: 3, start: 540, end: 720 },
          { day: 3, start: 780, end: 1020 },
        ]}
        unavailable={[
          {
            id: 'off',
            start: at('2026-11-04', '14:00'),
            end: at('2026-11-04', '15:00'),
            label: 'Time off',
          },
        ]}
      />,
    );
    const [wed, sat] = Array.from(container.querySelectorAll('.mn-time-grid__column'));
    const shades = Array.from(wed?.querySelectorAll<HTMLElement>('.mn-time-grid__shade') ?? []).map(
      (s) => [s.style.top, s.style.height],
    );
    expect(shades).toEqual([
      ['0px', '432px'],
      ['576px', '48px'],
      ['816px', '336px'],
    ]);
    // Saturday has no hours: shaded all day.
    expect(sat?.querySelectorAll('.mn-time-grid__shade')).toHaveLength(1);
    expect(within(wed as HTMLElement).getByText('Time off')).toBeInTheDocument();
  });

  it('creates from a pointer click, snapped to the grid', () => {
    const onSlotClick = vi.fn();
    render(<TimeGrid {...base} days={['2026-11-04']} onSlotClick={onSlotClick} snapMinutes={15} />);
    const column = document.querySelector('.mn-time-grid__column') as HTMLElement;
    vi.spyOn(column, 'getBoundingClientRect').mockReturnValue(
      rect({ top: 100, height: 1152, width: 200 }),
    );
    const slot = screen.getByRole('button', { name: 'Create event, Wednesday 4 November, 14:00' });
    // A click at 14:40 snaps down to 14:30 (15-minute snaps).
    fireEvent.click(slot, { clientY: 100 + (14 * 60 + 40) * 0.8, detail: 1 });
    expect(onSlotClick).toHaveBeenCalledTimes(1);
    const slotArg = onSlotClick.mock.lastCall?.[0];
    expect(slotArg.day).toBe('2026-11-04');
    expect(slotArg.minutes).toBe(14 * 60 + 30);
    expect(slotArg.start.toISOString()).toBe('2026-11-04T14:30:00.000Z');
  });

  it('creates from the keyboard: one tab stop, arrows move the slot, Enter creates', async () => {
    const onSlotClick = vi.fn();
    render(
      <TimeGrid
        {...base}
        timeZone="Asia/Kolkata"
        days={['2026-11-04', '2026-11-05']}
        onSlotClick={onSlotClick}
      />,
    );
    const user = userEvent.setup();
    await user.tab();
    expect(
      screen.getByRole('button', { name: 'Create event, Wednesday 4 November, 08:00' }),
    ).toHaveFocus();
    expect(screen.getAllByRole('button').filter((b) => b.tabIndex === 0)).toHaveLength(1);
    await user.keyboard('{ArrowDown}{ArrowDown}{ArrowRight}');
    const target = screen.getByRole('button', { name: 'Create event, Thursday 5 November, 10:00' });
    expect(target).toHaveFocus();
    await user.keyboard('{Enter}');
    const slot = onSlotClick.mock.lastCall?.[0];
    expect(slot.minutes).toBe(600);
    // 10:00 in Kolkata (UTC+5:30) is 04:30 UTC.
    expect(slot.start.toISOString()).toBe('2026-11-05T04:30:00.000Z');
    await user.keyboard('{Home}');
    expect(
      screen.getByRole('button', { name: 'Create event, Thursday 5 November, 00:00' }),
    ).toHaveFocus();
    await user.keyboard('{Control>}{End}{/Control}');
    expect(
      screen.getByRole('button', { name: 'Create event, Thursday 5 November, 23:00' }),
    ).toHaveFocus();
    await user.keyboard('{ArrowLeft}');
    expect(
      screen.getByRole('button', { name: 'Create event, Wednesday 4 November, 23:00' }),
    ).toHaveFocus();
    await expectNoA11yViolations();
  });

  it('calls back for events and day headers', async () => {
    const onEventClick = vi.fn();
    const onDayClick = vi.fn();
    render(
      <TimeGrid
        {...base}
        days={WEEK}
        events={EVENTS}
        onEventClick={onEventClick}
        onDayClick={onDayClick}
      />,
    );
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Interview, 09:30 – 10:30' }));
    expect(onEventClick).toHaveBeenCalledWith(EVENTS[1]);
    const header = screen.getByRole('button', { name: 'Wed 4, Open Wednesday 4 November' });
    expect(header).toHaveAttribute('aria-current', 'date');
    await user.click(header);
    expect(onDayClick).toHaveBeenCalledWith('2026-11-04');
  });

  it('drags an event to a new time and day, keeping its length, and swallows the click', () => {
    const onEventMove = vi.fn();
    const onEventClick = vi.fn();
    const { container } = render(
      <TimeGrid
        {...base}
        days={['2026-11-04', '2026-11-05']}
        events={EVENTS.slice(0, 1)}
        onEventMove={onEventMove}
        onEventClick={onEventClick}
      />,
    );
    const columns = container.querySelector('.mn-time-grid__columns') as HTMLElement;
    vi.spyOn(columns, 'getBoundingClientRect').mockReturnValue(
      rect({ top: 0, left: 0, width: 400, height: 1152 }),
    );
    const box = container.querySelector('.mn-time-grid__event') as HTMLElement;
    const chip = screen.getByRole('button', { name: 'Design review, 09:00 – 10:00' });
    expect(box).toHaveAttribute('data-movable');
    // Grab 15 minutes into the event (09:15), release over 13:45 on Thursday: it starts at 13:30.
    fireEvent.pointerDown(chip, {
      clientX: 50,
      clientY: 555 * 0.8,
      button: 0,
      pointerId: 1,
      pointerType: 'mouse',
    });
    fireEvent.pointerMove(chip, {
      clientX: 300,
      clientY: 825 * 0.8,
      pointerId: 1,
      pointerType: 'mouse',
    });
    expect(container.querySelector('.mn-time-grid__ghost')).toHaveTextContent(
      '13:30 · Design review',
    );
    expect(box).toHaveAttribute('data-ghosted');
    fireEvent.pointerUp(chip, {
      clientX: 300,
      clientY: 825 * 0.8,
      pointerId: 1,
      pointerType: 'mouse',
    });
    fireEvent.click(chip);
    expect(onEventClick).not.toHaveBeenCalled();
    expect(onEventMove).toHaveBeenCalledTimes(1);
    const move = onEventMove.mock.lastCall?.[0];
    expect(move.event.id).toBe('a');
    expect(move.start.toISOString()).toBe('2026-11-05T13:30:00.000Z');
    expect(move.end.toISOString()).toBe('2026-11-05T14:30:00.000Z');
    expect(container.querySelector('.mn-time-grid__ghost')).toBeNull();
  });

  it('cancels a drag with Escape', () => {
    const onEventMove = vi.fn();
    const { container } = render(
      <TimeGrid
        {...base}
        days={['2026-11-04']}
        events={EVENTS.slice(0, 1)}
        onEventMove={onEventMove}
      />,
    );
    const columns = container.querySelector('.mn-time-grid__columns') as HTMLElement;
    vi.spyOn(columns, 'getBoundingClientRect').mockReturnValue(rect({ width: 200, height: 1152 }));
    const chip = screen.getByRole('button', { name: /^Design review/ });
    fireEvent.pointerDown(chip, {
      clientX: 10,
      clientY: 440,
      button: 0,
      pointerId: 5,
      pointerType: 'mouse',
    });
    fireEvent.pointerMove(chip, { clientX: 10, clientY: 600, pointerId: 5, pointerType: 'mouse' });
    expect(container.querySelector('.mn-time-grid__ghost')).not.toBeNull();
    fireEvent.keyDown(chip, { key: 'Escape' });
    expect(container.querySelector('.mn-time-grid__ghost')).toBeNull();
    fireEvent.pointerUp(chip, { clientX: 10, clientY: 600, pointerId: 5, pointerType: 'mouse' });
    expect(onEventMove).not.toHaveBeenCalled();
  });

  it('ignores touch drags, small movements and events that may not move', () => {
    const onEventMove = vi.fn();
    const { container } = render(
      <TimeGrid
        {...base}
        days={['2026-11-04']}
        events={EVENTS.slice(0, 2)}
        onEventMove={onEventMove}
        canMoveEvent={(e) => e.id !== 'b'}
      />,
    );
    const columns = container.querySelector('.mn-time-grid__columns') as HTMLElement;
    vi.spyOn(columns, 'getBoundingClientRect').mockReturnValue(rect({ width: 200, height: 1152 }));
    const a = screen.getByRole('button', { name: /^Design review/ });
    fireEvent.pointerDown(a, {
      clientX: 10,
      clientY: 440,
      button: 0,
      pointerId: 2,
      pointerType: 'touch',
    });
    fireEvent.pointerMove(a, { clientX: 10, clientY: 600, pointerId: 2, pointerType: 'touch' });
    fireEvent.pointerUp(a, { clientX: 10, clientY: 600, pointerId: 2, pointerType: 'touch' });
    fireEvent.pointerDown(a, {
      clientX: 10,
      clientY: 440,
      button: 0,
      pointerId: 3,
      pointerType: 'mouse',
    });
    fireEvent.pointerMove(a, { clientX: 11, clientY: 442, pointerId: 3, pointerType: 'mouse' });
    fireEvent.pointerUp(a, { clientX: 11, clientY: 442, pointerId: 3, pointerType: 'mouse' });
    const b = screen.getByRole('button', { name: /^Interview/ });
    expect(b.closest('.mn-time-grid__event')).not.toHaveAttribute('data-movable');
    fireEvent.pointerDown(b, {
      clientX: 150,
      clientY: 470,
      button: 0,
      pointerId: 4,
      pointerType: 'mouse',
    });
    fireEvent.pointerMove(b, { clientX: 150, clientY: 700, pointerId: 4, pointerType: 'mouse' });
    fireEvent.pointerUp(b, { clientX: 150, clientY: 700, pointerId: 4, pointerType: 'mouse' });
    expect(onEventMove).not.toHaveBeenCalled();
  });

  it('limits the visible hours and clips events to them', () => {
    const { container } = render(
      <TimeGrid
        {...base}
        days={['2026-11-04']}
        events={EVENTS}
        startHour={9}
        endHour={11}
        hourHeight={60}
      />,
    );
    expect(container.querySelectorAll('.mn-time-grid__hour')).toHaveLength(1);
    const review = screen
      .getByRole('button', { name: /^Design review/ })
      .closest('.mn-time-grid__event') as HTMLElement;
    expect(review.style.top).toBe('0px');
    expect(screen.queryByRole('button', { name: /^Stand-up/ })).toBeNull();
  });

  it('renders on the server without reading the clock', () => {
    const { now: _now, ...rest } = base;
    void _now;
    const html = renderToString(
      <TimeGrid {...rest} days={WEEK} events={EVENTS} onSlotClick={() => {}} />,
    );
    expect(html).toContain('mn-time-grid');
    expect(html).toContain('Design review');
    expect(html).not.toContain('mn-time-grid__now');
  });
});
