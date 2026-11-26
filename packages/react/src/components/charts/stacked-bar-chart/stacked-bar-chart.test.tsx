import { describe, expect, it, vi } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { StackedBarChart, type StackedBarChartProps } from './index.js';

const DATA = [
  { team: 'North', open: 6, done: 14 },
  { team: 'South', open: 10, done: 10 },
  { team: 'East', open: 2, done: 6 },
];
const SERIES = [
  { key: 'done', label: 'Done' },
  { key: 'open', label: 'Open' },
];

function chart(props: Partial<StackedBarChartProps> = {}) {
  return (
    <StackedBarChart
      title="Tickets by team"
      data={DATA}
      categoryKey="team"
      categoryLabel="Team"
      series={SERIES}
      locale="en-US"
      {...props}
    />
  );
}

const plot = () => screen.getByRole('group', { name: /3 points from North to East/ });
const live = () => document.querySelector('.mn-chart__plot [role="status"]')!;

async function tabToPlot(user: ReturnType<typeof userEvent.setup>) {
  for (let i = 0; i < 8 && document.activeElement !== plot(); i++) await user.tab();
}

describe('StackedBarChart', () => {
  it('stacks by default with totals in the table and the tooltip', async () => {
    const { container } = render(chart());
    const figure = container.querySelector('figure')!;
    expect(figure).toHaveClass('mn-stacked-bar-chart');
    expect(figure).toHaveAttribute('data-offset', 'normal');
    expect(plot()).toHaveAttribute('data-layout', 'stacked');
    expect(container.querySelectorAll('.mn-chart__bar')).toHaveLength(6);
    const table = within(container.querySelector('table')!);
    expect(table.getByRole('columnheader', { name: 'Total' })).toBeInTheDocument();
    expect(table.getAllByRole('row')[1]).toHaveTextContent('North14620');

    const user = userEvent.setup();
    await tabToPlot(user);
    expect(live()).toHaveTextContent('North: Done 14, Open 6, Total 20');
    await user.keyboard('{ArrowRight}');
    expect(live()).toHaveTextContent('South: Done 10, Open 10, Total 20');
    await expectNoA11yViolations();
  });

  it('stacks to 100% with shares everywhere', async () => {
    const { container } = render(chart({ offset: 'expand' }));
    expect(container.querySelector('figure')).toHaveAttribute('data-offset', 'expand');
    const table = within(container.querySelector('table')!);
    expect(table.getAllByRole('row')[1]).toHaveTextContent('North70%30%100%');
    const ticks = [...container.querySelectorAll('[data-axis="value"] text')].map(
      (t) => t.textContent,
    );
    expect(ticks[ticks.length - 1]).toBe('100%');
    const user = userEvent.setup();
    await tabToPlot(user);
    await user.keyboard('{End}');
    expect(live()).toHaveTextContent('East: Done 75%, Open 25%');
  });

  it('draws grouped bars and horizontal bars on request', () => {
    const { container, rerender } = render(chart({ layout: 'grouped' }));
    expect(plot()).toHaveAttribute('data-layout', 'grouped');
    expect(container.querySelector('figure')).not.toHaveAttribute('data-offset');
    rerender(chart({ orientation: 'horizontal' }));
    expect(plot()).toHaveAttribute('data-orientation', 'horizontal');
  });

  it('toggles series from the legend', async () => {
    const onHiddenSeriesChange = vi.fn();
    const { container } = render(chart({ onHiddenSeriesChange }));
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Open' }));
    expect(onHiddenSeriesChange).toHaveBeenCalledWith(['open']);
    expect(container.querySelectorAll('.mn-chart__bar')).toHaveLength(3);
  });

  it('passes the original row to onBarSelect in 100% mode', async () => {
    const onBarSelect = vi.fn();
    render(chart({ offset: 'expand', onBarSelect }));
    const user = userEvent.setup();
    await tabToPlot(user);
    await user.keyboard('{Enter}');
    expect(onBarSelect).toHaveBeenCalledWith(DATA[0], 0);
  });

  it('renders on the server', () => {
    const html = renderToString(chart({ valueLabels: true }));
    expect(html).toContain('mn-stacked-bar-chart');
    expect(html).toContain('viewBox="0 0 640 240"');
  });
});
