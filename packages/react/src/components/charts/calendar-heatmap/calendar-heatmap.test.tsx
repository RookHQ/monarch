import { describe, expect, it, vi } from 'vitest';
import { fireEvent, render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { CalendarHeatmap, type CalendarHeatmapProps } from './index.js';

if (typeof window.PointerEvent === 'undefined') {
  class PointerEventStub extends MouseEvent {
    pointerType: string;
    constructor(type: string, init: PointerEventInit = {}) {
      super(type, init);
      this.pointerType = init.pointerType ?? 'mouse';
    }
  }
  window.PointerEvent = PointerEventStub as unknown as typeof PointerEvent;
}

// 2026-01-01 is a Thursday.
const DAYS = [
  { date: '2026-01-01', value: null, note: 'Holiday', inactive: true },
  { date: '2026-01-02', value: 3 },
  { date: '2026-01-05', value: 8, marker: true, note: 'Late' },
  { date: '2026-01-06', value: 0 },
  { date: '2026-01-07', value: 5, valueLabel: '5 commits' },
];

function chart(props: Partial<CalendarHeatmapProps> = {}) {
  return (
    <CalendarHeatmap
      title="Activity"
      days={DAYS}
      from="2026-01-01"
      to="2026-01-14"
      locale="en-US"
      today="2026-01-07"
      {...props}
    />
  );
}

const plot = () => screen.getByRole('group', { name: /14 days from/ });
const live = () => document.querySelector('[role="status"]')!;

describe('CalendarHeatmap', () => {
  it('draws every day in the range on a week grid with a summary', async () => {
    const { container } = render(chart());
    const cells = container.querySelectorAll('.mn-calendar-heatmap__cell');
    expect(cells).toHaveLength(14);
    expect(plot()).toHaveAccessibleName(
      '14 days from Thu, Jan 1, 2026 to Wed, Jan 14, 2026. Total 16. Highest Mon, Jan 5, 2026 8',
    );
    expect(container.querySelector('.mn-calendar-heatmap')).toHaveAttribute('data-layout', 'year');
    expect(cells[0]).toHaveAttribute('data-inactive');
    expect(cells[0]).toHaveAttribute('data-empty');
    expect(cells[4]).toHaveAttribute('data-level', '4');
    expect(cells[6]).toHaveAttribute('data-today');
    expect(container.querySelectorAll('.mn-calendar-heatmap__marker')).toHaveLength(1);
    // Monday-first weeks: Thursday 1 Jan sits in row 3 of the first column.
    const first = cells[0] as SVGRectElement;
    const monday = cells[4] as SVGRectElement;
    expect(Number(monday.getAttribute('x'))).toBeGreaterThan(Number(first.getAttribute('x')));
    expect(Number(monday.getAttribute('y'))).toBeLessThan(Number(first.getAttribute('y')));
    expect(screen.getByRole('list', { name: 'Legend' })).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('lists the days with values and notes in the table', () => {
    render(chart());
    const table = screen.getByRole('table');
    expect(within(table).getAllByRole('row')).toHaveLength(6);
    expect(within(table).getByRole('columnheader', { name: 'Note' })).toBeInTheDocument();
    expect(within(table).getByText('Holiday')).toBeInTheDocument();
    expect(within(table).getByText('5 commits')).toBeInTheDocument();
  });

  it('moves by day and week with the keyboard and selects a day', async () => {
    const onDaySelect = vi.fn();
    render(chart({ onDaySelect }));
    const user = userEvent.setup();
    for (let i = 0; i < 6 && document.activeElement !== plot(); i++) await user.tab();
    expect(plot()).toHaveFocus();
    expect(live()).toHaveTextContent('Thu, Jan 1, 2026: No data, Holiday');
    await user.keyboard('{ArrowDown}');
    expect(live()).toHaveTextContent('Fri, Jan 2, 2026: 3');
    await user.keyboard('{ArrowRight}');
    expect(live()).toHaveTextContent('Fri, Jan 9, 2026');
    await user.keyboard('{ArrowRight}');
    expect(live()).toHaveTextContent('Fri, Jan 9, 2026');
    await user.keyboard('{ArrowLeft}{ArrowUp}{ArrowUp}{ArrowUp}{ArrowUp}{ArrowUp}{ArrowUp}');
    expect(live()).toHaveTextContent('Thu, Jan 1, 2026');
    await user.keyboard('{End}');
    expect(live()).toHaveTextContent('Wed, Jan 14, 2026: No data');
    await user.keyboard('{Home}{ArrowDown}{Enter}');
    expect(onDaySelect).toHaveBeenCalledWith(DAYS[1]);
    await user.keyboard('{End}{Enter}');
    expect(onDaySelect).toHaveBeenLastCalledWith({ date: '2026-01-14', value: null });
    await user.keyboard('{Escape}');
    expect(live()).toHaveTextContent('');
  });

  it('lays out a month calendar with day numbers, values and arrow keys by day', async () => {
    const { container } = render(chart({ layout: 'month' }));
    expect(container.querySelector('.mn-calendar-heatmap')).toHaveAttribute('data-layout', 'month');
    const numbers = [...container.querySelectorAll('.mn-calendar-heatmap__day')].map(
      (t) => t.textContent,
    );
    expect(numbers.slice(0, 3)).toEqual(['1', '2', '3']);
    expect(container.querySelector('.mn-calendar-heatmap__value')).toHaveTextContent('Holiday');
    const weekdays = [...container.querySelectorAll('[data-axis="weekday"] text')].map(
      (t) => t.textContent,
    );
    expect(weekdays).toEqual(['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun']);
    const user = userEvent.setup();
    for (let i = 0; i < 6 && document.activeElement !== plot(); i++) await user.tab();
    await user.keyboard('{ArrowRight}');
    expect(live()).toHaveTextContent('Fri, Jan 2, 2026');
    await user.keyboard('{ArrowDown}');
    expect(live()).toHaveTextContent('Fri, Jan 9, 2026');
  });

  it('starts weeks on Sunday when asked', () => {
    const { container } = render(chart({ layout: 'month', weekStartsOn: 0 }));
    const first = container.querySelector('[data-axis="weekday"] text');
    expect(first).toHaveTextContent('Sun');
  });

  it('shows a tooltip on hover and opens a day on click', () => {
    const onDaySelect = vi.fn();
    const { container } = render(chart({ onDaySelect }));
    const cells = container.querySelectorAll('.mn-calendar-heatmap__cell');
    fireEvent.pointerMove(cells[4]!, { pointerType: 'mouse' });
    const tip = container.querySelector('.mn-chart__tooltip')!;
    expect(tip).toHaveTextContent('Mon, Jan 5, 2026');
    expect(tip).toHaveTextContent('Late');
    fireEvent.click(cells[4]!);
    expect(onDaySelect).toHaveBeenCalledWith(DAYS[2]);
  });

  it('defaults the range to the data and shows the empty state without days', () => {
    const { container, rerender } = render(<CalendarHeatmap title="Activity" days={DAYS} />);
    expect(container.querySelectorAll('.mn-calendar-heatmap__cell')).toHaveLength(7);
    rerender(<CalendarHeatmap title="Activity" days={[]} />);
    expect(container.querySelector('figure')).toHaveAttribute('data-status', 'empty');
  });

  it('accepts a controlled active date and translated words', () => {
    const onActiveDateChange = vi.fn();
    const { container } = render(
      chart({
        activeDate: '2026-01-06',
        onActiveDateChange,
        labels: { value: 'Valeur', summary: (n) => `${n} jours` },
      }),
    );
    expect(screen.getByRole('group', { name: '14 jours' })).toBeInTheDocument();
    expect(container.querySelector('[data-active]')).toHaveAttribute('data-level', '0');
    expect(container.querySelector('.mn-chart__tooltip')).toHaveTextContent('Valeur');
    fireEvent.pointerMove(container.querySelectorAll('.mn-calendar-heatmap__cell')[1]!, {
      pointerType: 'mouse',
    });
    expect(onActiveDateChange).toHaveBeenCalledWith('2026-01-02');
  });

  it('renders on the server', () => {
    const html = renderToString(chart({ layout: 'month' }));
    expect(html).toContain('mn-calendar-heatmap');
    expect(html).toContain('data-today');
  });
});
