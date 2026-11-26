import { describe, expect, it, vi } from 'vitest';
import { useState } from 'react';
import { fireEvent, render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { AreaChart, type AreaChartProps } from './index.js';

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
  { week: 'W1', web: 30, mobile: 10 },
  { week: 'W2', web: 40, mobile: 20 },
  { week: 'W3', web: 20, mobile: 30 },
  { week: 'W4', web: 50, mobile: 50 },
];
const SERIES = [
  { key: 'web', label: 'Web' },
  { key: 'mobile', label: 'Mobile' },
];

function chart(props: Partial<AreaChartProps> = {}) {
  return (
    <AreaChart
      title="Visits"
      data={DATA}
      xKey="week"
      xLabel="Week"
      series={SERIES}
      locale="en-US"
      {...props}
    />
  );
}

const plot = () => screen.getByRole('group', { name: /4 points from W1 to W4/ });
const live = () => document.querySelector('.mn-chart__plot [role="status"]')!;

async function tabToPlot(user: ReturnType<typeof userEvent.setup>) {
  for (let i = 0; i < 8 && document.activeElement !== plot(); i++) await user.tab();
}

describe('AreaChart', () => {
  it('draws overlapping areas with a legend and a generated summary', async () => {
    const { container } = render(chart());
    expect(plot()).toHaveClass('mn-area-chart');
    expect(plot()).toHaveAttribute('data-stacking', 'overlap');
    expect(container.querySelectorAll('.mn-area-chart__area')).toHaveLength(2);
    expect(container.querySelectorAll('.mn-chart__line')).toHaveLength(2);
    expect(plot()).toHaveAccessibleName(
      '4 points from W1 to W4. Web: low 20, high 50, latest 50. Mobile: low 10, high 50, latest 50',
    );
    expect(plot()).toHaveAccessibleDescription('Use the arrow keys to inspect values.');
    expect(screen.getByRole('toolbar', { name: 'Legend' })).toBeInTheDocument();
    expect(container.querySelector('svg')).toHaveAttribute('aria-hidden', 'true');
    await expectNoA11yViolations();
  });

  it('stacks series so the top edge is the total', async () => {
    const { container } = render(chart({ stacking: 'stacked' }));
    expect(plot()).toHaveAttribute('data-stacking', 'stacked');
    expect(plot()).toHaveAccessibleName(/Total: low 40, high 100, latest 100/);
    const table = within(container.querySelector('table')!);
    expect(table.getByRole('columnheader', { name: 'Total' })).toBeInTheDocument();
    expect(table.getAllByRole('row')[4]).toHaveTextContent('W45050100');

    const user = userEvent.setup();
    await tabToPlot(user);
    expect(live()).toHaveTextContent('W1: Web 30, Mobile 10, Total 40');
    const tooltip = document.querySelector('.mn-chart__tooltip')!;
    expect(tooltip).toHaveAttribute('aria-hidden', 'true');
    expect(tooltip).toHaveTextContent('Total 40');
    // Tooltip rows read top of the stack first.
    expect(tooltip.querySelectorAll('.mn-chart__tooltip-label')[0]).toHaveTextContent('Mobile');
    await expectNoA11yViolations();
  });

  it('stacks to 100% with shares in the tooltip and percent ticks', async () => {
    const { container } = render(chart({ stacking: 'expand' }));
    const ticks = [...container.querySelectorAll('[data-axis="value"] text')].map(
      (t) => t.textContent,
    );
    expect(ticks[0]).toBe('0%');
    expect(ticks[ticks.length - 1]).toBe('100%');
    const user = userEvent.setup();
    await tabToPlot(user);
    await user.keyboard('{ArrowRight}{ArrowRight}');
    expect(live()).toHaveTextContent('W3: Web 20 (40%), Mobile 30 (60%), Total 50');
  });

  it('inspects with the keyboard: arrows, Home, End and Escape', async () => {
    render(chart());
    const user = userEvent.setup();
    await tabToPlot(user);
    expect(plot()).toHaveFocus();
    expect(live()).toHaveTextContent('W1: Web 30, Mobile 10');
    await user.keyboard('{ArrowRight}');
    expect(live()).toHaveTextContent('W2: Web 40, Mobile 20');
    await user.keyboard('{End}');
    expect(live()).toHaveTextContent('W4');
    await user.keyboard('{Home}');
    expect(live()).toHaveTextContent('W1');
    await user.keyboard('{Escape}');
    expect(document.querySelector('.mn-chart__tooltip')).toBeNull();
  });

  it('inspects on hover without announcing', () => {
    render(chart());
    vi.spyOn(plot(), 'getBoundingClientRect').mockReturnValue({
      left: 0,
      top: 0,
      width: 640,
      height: 240,
      right: 640,
      bottom: 240,
      x: 0,
      y: 0,
      toJSON: () => ({}),
    });
    fireEvent.pointerMove(plot(), { clientX: 639, clientY: 20, pointerType: 'mouse' });
    expect(document.querySelector('.mn-chart__tooltip')).toHaveTextContent('W4');
    expect(document.querySelector('.mn-chart__crosshair')).not.toBeNull();
    expect(live()).toHaveTextContent('');
    fireEvent.pointerLeave(plot(), { pointerType: 'mouse' });
    expect(document.querySelector('.mn-chart__tooltip')).toBeNull();
  });

  it('hides a series from the legend and calls onHiddenSeriesChange', async () => {
    const onHiddenSeriesChange = vi.fn();
    const { container } = render(chart({ onHiddenSeriesChange }));
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Web' }));
    expect(onHiddenSeriesChange).toHaveBeenCalledWith(['web']);
    expect(container.querySelectorAll('.mn-area-chart__area')).toHaveLength(1);
    // Colours follow the series: Mobile keeps slot 2.
    expect(
      (container.querySelector('[data-series="mobile"]') as HTMLElement).style.getPropertyValue(
        '--_c',
      ),
    ).toBe('var(--mn-color-data-2)');
    // The table still lists every series.
    expect(
      within(container.querySelector('table')!).getByRole('columnheader', { name: 'Web' }),
    ).toBeInTheDocument();
  });

  it('works with controlled hidden series and active index', async () => {
    function Controlled() {
      const [active, setActive] = useState<number | null>(2);
      return (
        <>
          {chart({ hiddenSeries: ['mobile'], activeIndex: active, onActiveIndexChange: setActive })}
          <output>{String(active)}</output>
        </>
      );
    }
    const { container } = render(<Controlled />);
    expect(container.querySelectorAll('.mn-area-chart__area')).toHaveLength(1);
    expect(document.querySelector('.mn-chart__tooltip')).toHaveTextContent('W3');
    const user = userEvent.setup();
    plot().focus();
    await user.keyboard('{ArrowLeft}');
    expect(screen.getByText('1', { selector: 'output' })).toBeInTheDocument();
  });

  it('breaks the area at gaps and dots a lone point', () => {
    const { container } = render(
      chart({
        series: [{ key: 'v', label: 'Value' }],
        data: [
          { week: 'W1', v: 3 },
          { week: 'W2', v: 4 },
          { week: 'W3', v: null },
          { week: 'W4', v: 6 },
        ],
      }),
    );
    const d = container.querySelector('.mn-area-chart__area')!.getAttribute('d')!;
    expect(d.match(/Z/g)).toHaveLength(1);
    expect(container.querySelectorAll('.mn-chart__dot')).toHaveLength(1);
    // A single series has no legend.
    expect(screen.queryByRole('toolbar')).toBeNull();
  });

  it('calls onPointSelect from Enter and click', async () => {
    const onPointSelect = vi.fn();
    render(chart({ onPointSelect }));
    const user = userEvent.setup();
    await tabToPlot(user);
    await user.keyboard('{ArrowRight}{Enter}');
    expect(onPointSelect).toHaveBeenCalledWith(DATA[1], 1);
    expect(plot()).toHaveAttribute('data-selectable');
  });

  it('formats with the locale, draws reference lines and accepts a custom summary', () => {
    const { container } = render(
      chart({
        locale: 'de-DE',
        valueFormat: { style: 'currency', currency: 'EUR' },
        referenceLines: [{ value: 45, label: 'Target' }],
        summary: 'Custom summary',
        data: [{ week: 'W1', web: 1234.5, mobile: 1 }],
      }),
    );
    expect(screen.getByRole('group', { name: 'Custom summary' })).toBeInTheDocument();
    expect(container.querySelector('.mn-chart__reference')).toHaveTextContent('Target');
    expect(container.querySelector('table')).toHaveTextContent('1.234,50 €');
  });

  it('shows the empty state for no rows and passes loading through', () => {
    const { rerender, container } = render(chart({ data: [] }));
    expect(container.querySelector('figure')).toHaveAttribute('data-status', 'empty');
    rerender(chart({ status: 'loading' }));
    expect(container.querySelector('figure')).toHaveAttribute('data-status', 'loading');
  });

  it('renders on the server at the fallback width', () => {
    const html = renderToString(chart({ stacking: 'stacked' }));
    expect(html).toContain('mn-area-chart');
    expect(html).toContain('viewBox="0 0 640 240"');
  });
});
