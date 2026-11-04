import { describe, expect, it, vi } from 'vitest';
import { useState } from 'react';
import { fireEvent, render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { LineChart, type LineChartProps } from './index.js';

// jsdom has no PointerEvent: without it fireEvent drops clientX and pointerType.
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

const DATA = [
  { day: 'Mon', visits: 120, signups: 8 },
  { day: 'Tue', visits: 98, signups: 5 },
  { day: 'Wed', visits: 143, signups: 11 },
  { day: 'Thu', visits: 160, signups: 9 },
];
const SERIES = [
  { key: 'visits', label: 'Visits' },
  { key: 'signups', label: 'Sign-ups' },
];

function chart(props: Partial<LineChartProps> = {}) {
  return (
    <LineChart
      title="Traffic"
      data={DATA}
      xKey="day"
      xLabel="Day"
      series={SERIES}
      locale="en-US"
      {...props}
    />
  );
}

const plot = () => screen.getByRole('group', { name: /4 points from Mon to Thu/ });
const live = () => document.querySelector('[role="status"]')!;

/** Tabs past the header toggle and legend buttons to the plot. */
async function tabToPlot(user: ReturnType<typeof userEvent.setup>) {
  for (let i = 0; i < 6 && document.activeElement !== plot(); i++) await user.tab();
}

describe('LineChart', () => {
  it('draws one line per series with a legend and a generated summary', async () => {
    const { container } = render(chart());
    expect(container.querySelectorAll('.mn-chart__line')).toHaveLength(2);
    expect(plot()).toHaveAttribute('aria-roledescription', 'chart');
    expect(plot()).toHaveAccessibleName(
      '4 points from Mon to Thu. Visits: low 98, high 160, latest 160. Sign-ups: low 5, high 11, latest 9',
    );
    expect(plot()).toHaveAccessibleDescription('Use the arrow keys to inspect values.');
    expect(screen.getByRole('list', { name: 'Legend' })).toBeInTheDocument();
    expect(container.querySelector('svg')).toHaveAttribute('aria-hidden', 'true');
    await expectNoA11yViolations();
  });

  it('inspects points with the keyboard and announces them', async () => {
    render(chart());
    const user = userEvent.setup();
    await tabToPlot(user);
    expect(plot()).toHaveFocus();
    expect(live()).toHaveTextContent('Mon: Visits 120, Sign-ups 8');
    await user.keyboard('{ArrowRight}');
    expect(live()).toHaveTextContent('Tue: Visits 98, Sign-ups 5');
    const tooltip = document.querySelector('.mn-chart__tooltip')!;
    expect(tooltip).toHaveAttribute('aria-hidden', 'true');
    expect(tooltip).toHaveTextContent('Tue');
    await user.keyboard('{End}');
    expect(live()).toHaveTextContent('Thu');
    await user.keyboard('{ArrowRight}');
    expect(live()).toHaveTextContent('Thu');
    await user.keyboard('{Home}');
    expect(live()).toHaveTextContent('Mon');
    await user.keyboard('{Escape}');
    expect(document.querySelector('.mn-chart__tooltip')).toBeNull();
    await user.keyboard('{ArrowLeft}');
    expect(live()).toHaveTextContent('Thu');
    await expectNoA11yViolations();
    await user.tab();
    expect(document.querySelector('.mn-chart__tooltip')).toBeNull();
  });

  it('shows the tooltip and crosshair at the nearest point on hover, without announcing', () => {
    const { container } = render(chart());
    // Fallback width 640: the plot runs from the y labels to x = 628.
    fireEvent.pointerMove(plot(), { clientX: 620, clientY: 50, pointerType: 'mouse' });
    expect(container.querySelector('.mn-chart__crosshair')).not.toBeNull();
    expect(container.querySelector('.mn-chart__tooltip')).toHaveTextContent('Thu');
    expect(container.querySelector('.mn-chart__tooltip')).toHaveAttribute('data-side', 'left');
    expect(live().textContent).toBe('');
    fireEvent.pointerLeave(plot(), { pointerType: 'mouse' });
    expect(container.querySelector('.mn-chart__tooltip')).toBeNull();
  });

  it('breaks the line at gaps and dots a lone point', () => {
    const data = [
      { x: 1, v: 1 },
      { x: 2, v: 2 },
      { x: 3, v: null },
      { x: 4, v: 4 },
      { x: 5, v: null },
      { x: 6, v: 5 },
      { x: 7, v: 6 },
    ];
    const { container } = render(
      <LineChart title="Gaps" data={data} xKey="x" series={[{ key: 'v', label: 'Value' }]} />,
    );
    const d = container.querySelector('.mn-chart__line')!.getAttribute('d')!;
    expect(d.match(/M/g)).toHaveLength(2);
    expect(container.querySelectorAll('.mn-chart__dot')).toHaveLength(1);
    // A single series needs no legend: the title names it.
    expect(screen.queryByRole('list', { name: 'Legend' })).toBeNull();
    expect(screen.getAllByText('No data').length).toBeGreaterThan(0);
  });

  it('hides a series from its legend toggle (uncontrolled)', async () => {
    const onHiddenSeriesChange = vi.fn();
    const { container } = render(chart({ onHiddenSeriesChange }));
    await userEvent.setup().click(screen.getByRole('button', { name: 'Sign-ups' }));
    expect(onHiddenSeriesChange).toHaveBeenCalledWith(['signups']);
    expect(container.querySelectorAll('.mn-chart__line')).toHaveLength(1);
    expect(screen.getByRole('button', { name: 'Sign-ups' })).toHaveAttribute(
      'aria-pressed',
      'false',
    );
    // Colour follows the entity: Visits keeps slot 1.
    expect(container.querySelector('[data-series="visits"]')).toHaveStyle({
      '--_c': 'var(--mn-color-data-1)',
    });
  });

  it('follows controlled hiddenSeries and activeIndex', async () => {
    function Synced() {
      const [active, setActive] = useState<number | null>(2);
      return (
        <>
          <span data-testid="active">{String(active)}</span>
          {chart({ hiddenSeries: ['visits'], activeIndex: active, onActiveIndexChange: setActive })}
        </>
      );
    }
    const { container } = render(<Synced />);
    expect(container.querySelectorAll('.mn-chart__line')).toHaveLength(1);
    expect(container.querySelector('.mn-chart__tooltip')).toHaveTextContent('Wed');
    expect(container.querySelector('.mn-chart__tooltip')).not.toHaveTextContent('Visits');
    plot().focus();
    await userEvent.setup().keyboard('{ArrowLeft}');
    expect(screen.getByTestId('active')).toHaveTextContent('1');
  });

  it('calls onPointSelect with Enter and click', async () => {
    const onPointSelect = vi.fn();
    render(chart({ onPointSelect }));
    const user = userEvent.setup();
    await tabToPlot(user);
    await user.keyboard('{ArrowRight}{Enter}');
    expect(onPointSelect).toHaveBeenLastCalledWith(DATA[1], 1);
    fireEvent.pointerMove(plot(), { clientX: 620, clientY: 40, pointerType: 'mouse' });
    fireEvent.click(plot());
    expect(onPointSelect).toHaveBeenLastCalledWith(DATA[3], 3);
  });

  it('formats numbers and dates with the locale', () => {
    const data = [
      { date: new Date(Date.UTC(2026, 2, 1, 12)), revenue: 1234.5 },
      { date: new Date(Date.UTC(2026, 2, 2, 12)), revenue: 2345.25 },
    ];
    render(
      <LineChart
        title="Umsatz"
        data={data}
        xKey="date"
        xLabel="Datum"
        series={[{ key: 'revenue', label: 'Umsatz' }]}
        locale="de-DE"
        valueFormat={{ style: 'currency', currency: 'EUR' }}
        xFormat={{ day: 'numeric', month: 'long', timeZone: 'UTC' }}
      />,
    );
    const table = screen.getByRole('table');
    expect(within(table).getByRole('rowheader', { name: '1. März' })).toBeInTheDocument();
    expect(within(table).getByText('1.234,50 €')).toBeInTheDocument();
  });

  it('draws an area variant, reference lines and dots on every point', () => {
    const { container } = render(
      chart({
        variant: 'area',
        dots: true,
        curve: 'monotone',
        referenceLines: [{ value: 150, label: 'Target' }],
      }),
    );
    expect(container.querySelector('.mn-line-chart')).toHaveAttribute('data-variant', 'area');
    expect(container.querySelectorAll('.mn-chart__area')).toHaveLength(2);
    expect(container.querySelectorAll('.mn-chart__dot')).toHaveLength(8);
    expect(container.querySelector('.mn-chart__reference')).toHaveTextContent('Target 150');
    expect(container.querySelector('.mn-chart__line')!.getAttribute('d')).toContain('C');
    expect(plot()).toHaveAccessibleName(/Target 150$/);
  });

  it('respects a fixed y domain and a custom summary', () => {
    const { container } = render(chart({ yDomain: [0, 400], summary: 'Traffic is steady.' }));
    const ticks = [...container.querySelectorAll('[data-axis="value"] text')].map(
      (t) => t.textContent,
    );
    expect(ticks[0]).toBe('0');
    expect(ticks[ticks.length - 1]).toBe('400');
    expect(screen.getByRole('group', { name: 'Traffic is steady.' })).toBeInTheDocument();
  });

  it('shows the empty state for no data and passes loading through', () => {
    const { rerender } = render(chart({ data: [] }));
    expect(screen.getByRole('figure')).toHaveAttribute('data-status', 'empty');
    rerender(chart({ status: 'loading' }));
    expect(screen.getByRole('figure')).toHaveAttribute('data-status', 'loading');
    expect(screen.queryByRole('group')).toBeNull();
  });

  it('lists every value in the data table, hidden series included', () => {
    render(chart({ defaultHiddenSeries: ['signups'] }));
    const table = screen.getByRole('table', { name: 'Traffic' });
    expect(
      within(table)
        .getAllByRole('columnheader')
        .map((c) => c.textContent),
    ).toEqual(['Day', 'Visits', 'Sign-ups']);
    expect(within(table).getAllByRole('row')).toHaveLength(5);
  });

  it('renders on the server without touching window', () => {
    const html = renderToString(chart());
    expect(html).toContain('mn-line-chart');
    expect(html).toContain('viewBox="0 0 640 240"');
  });
});
