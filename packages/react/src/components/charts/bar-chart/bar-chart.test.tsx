import { describe, expect, it, vi } from 'vitest';
import { fireEvent, render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { BarChart, type BarChartProps } from './index.js';

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
  { week: 'W1', drafted: 12, posted: 8 },
  { week: 'W2', drafted: 9, posted: 9 },
  { week: 'W3', drafted: 15, posted: 11 },
];
const SERIES = [
  { key: 'drafted', label: 'Drafted' },
  { key: 'posted', label: 'Posted' },
];

function chart(props: Partial<BarChartProps> = {}) {
  return (
    <BarChart
      title="Posts"
      data={DATA}
      categoryKey="week"
      categoryLabel="Week"
      series={SERIES}
      locale="en-US"
      {...props}
    />
  );
}

const plot = () => screen.getByRole('group', { name: /3 points from W1 to W3/ });
const live = () => document.querySelector('[role="status"]')!;
async function tabToPlot(user: ReturnType<typeof userEvent.setup>) {
  for (let i = 0; i < 6 && document.activeElement !== plot(); i++) await user.tab();
}

describe('BarChart', () => {
  it('draws grouped bars per category with a legend and a summary', async () => {
    const { container } = render(chart());
    expect(container.querySelectorAll('.mn-chart__bar')).toHaveLength(6);
    expect(container.querySelector('.mn-bar-chart')).toHaveAttribute('data-layout', 'grouped');
    expect(plot()).toHaveAccessibleName(
      '3 points from W1 to W3. Drafted: total 36, highest W3 at 15. Posted: total 28, highest W3 at 11',
    );
    expect(screen.getByRole('button', { name: 'Drafted' })).toHaveAttribute('aria-pressed', 'true');
    await expectNoA11yViolations();
  });

  it('stacks series, totals them and rounds only the outer segment', () => {
    const { container } = render(chart({ layout: 'stacked' }));
    expect(container.querySelector('.mn-bar-chart')).toHaveAttribute('data-layout', 'stacked');
    const table = screen.getByRole('table');
    expect(within(table).getByRole('columnheader', { name: 'Total' })).toBeInTheDocument();
    expect(within(table).getByText('26')).toBeInTheDocument();
    const first = container.querySelectorAll('[data-index="0"] .mn-chart__bar');
    expect(first[0]!.getAttribute('d')).not.toContain('Q');
    expect(first[1]!.getAttribute('d')).toContain('Q');
    fireEvent.pointerMove(plot(), { clientX: 580, clientY: 100, pointerType: 'mouse' });
    expect(container.querySelector('.mn-chart__tooltip')).toHaveTextContent('W3');
    expect(container.querySelector('.mn-chart__tooltip-footer')).toHaveTextContent('Total 26');
  });

  it('inspects categories with the keyboard and selects one with Enter', async () => {
    const onBarSelect = vi.fn();
    render(chart({ onBarSelect, layout: 'stacked' }));
    const user = userEvent.setup();
    await tabToPlot(user);
    expect(plot()).toHaveFocus();
    expect(live()).toHaveTextContent('W1: Drafted 12, Posted 8, Total 20');
    await user.keyboard('{ArrowDown}');
    expect(live()).toHaveTextContent('W2');
    await user.keyboard('{Enter}');
    expect(onBarSelect).toHaveBeenCalledWith(DATA[1], 1);
    await user.keyboard(' ');
    expect(onBarSelect).toHaveBeenCalledTimes(2);
    expect(document.querySelector('.mn-chart__band')).not.toBeNull();
    await expectNoA11yViolations();
  });

  it('selects a category by click', () => {
    const onBarSelect = vi.fn();
    render(chart({ onBarSelect }));
    fireEvent.pointerDown(plot(), { clientX: 100, clientY: 100, pointerType: 'touch' });
    fireEvent.click(plot());
    expect(onBarSelect).toHaveBeenCalledWith(DATA[0], 0);
  });

  it('lays out horizontally with category labels and grows with the rows', () => {
    const { container } = render(
      chart({ orientation: 'horizontal', series: [SERIES[0]!], valueLabels: true }),
    );
    expect(container.querySelector('.mn-bar-chart')).toHaveAttribute(
      'data-orientation',
      'horizontal',
    );
    // 3 rows × 32px + 32px, at least 120.
    expect(container.querySelector('svg')).toHaveAttribute('height', '128');
    const labels = [...container.querySelectorAll('[data-axis="category"] text')].map(
      (t) => t.textContent,
    );
    expect(labels).toEqual(['W1', 'W2', 'W3']);
    expect([...container.querySelectorAll('.mn-chart__value')].map((t) => t.textContent)).toEqual([
      '12',
      '9',
      '15',
    ]);
    expect(screen.queryByRole('list', { name: 'Legend' })).toBeNull();
  });

  it('dims every category but the highlighted one', () => {
    const { container } = render(chart({ highlightIndex: 2 }));
    expect(container.querySelectorAll('[data-dimmed]')).toHaveLength(4);
    expect(container.querySelectorAll('[data-index="2"] [data-dimmed]')).toHaveLength(0);
  });

  it('draws negative values below the zero line', () => {
    const { container } = render(
      <BarChart
        title="Net change"
        data={[
          { m: 'Jan', v: 5 },
          { m: 'Feb', v: -3 },
        ]}
        categoryKey="m"
        series={[{ key: 'v', label: 'Change' }]}
      />,
    );
    const ticks = [...container.querySelectorAll('[data-axis="value"] text')].map((t) =>
      Number(t.textContent),
    );
    expect(Math.min(...ticks)).toBeLessThan(0);
    expect(container.querySelectorAll('.mn-chart__bar')).toHaveLength(2);
  });

  it('hides a series from the legend and keeps the other colour', async () => {
    const { container } = render(chart());
    await userEvent.setup().click(screen.getByRole('button', { name: 'Drafted' }));
    expect(container.querySelectorAll('.mn-chart__bar')).toHaveLength(3);
    expect(container.querySelector('[data-series="posted"]')).toHaveStyle({
      '--_c': 'var(--mn-color-data-2)',
    });
  });

  it('formats values with the locale and options', () => {
    render(
      chart({
        locale: 'fr-FR',
        valueFormat: { style: 'percent' },
        data: [{ week: 'S1', drafted: 0.5, posted: 0.25 }],
      }),
    );
    expect(within(screen.getByRole('table')).getByText(/50\s%/)).toBeInTheDocument();
  });

  it('renders on the server', () => {
    const html = renderToString(chart({ layout: 'stacked', orientation: 'horizontal' }));
    expect(html).toContain('mn-bar-chart');
    expect(html).toContain('data-orientation="horizontal"');
  });
});
