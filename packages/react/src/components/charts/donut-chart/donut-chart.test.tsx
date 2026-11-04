import { describe, expect, it, vi } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { DonutChart, type DonutChartSegment } from './index.js';

const devices: DonutChartSegment[] = [
  { key: 'phone', label: 'Phone', value: 1000 },
  { key: 'laptop', label: 'Laptop', value: 1200 },
  { key: 'tablet', label: 'Tablet', value: 300 },
];

function arcs(container: HTMLElement) {
  return Array.from(container.querySelectorAll<SVGCircleElement>('.mn-donut-chart__segment'));
}

describe('DonutChart', () => {
  it('is a titled figure with a summary, centre total, legend and hidden table', async () => {
    const { container } = render(
      <main>
        <DonutChart title="Devices" description="Last 30 days" segments={devices} locale="en-US" />
      </main>,
    );
    expect(screen.getByRole('figure', { name: 'Devices' })).toBeInTheDocument();
    expect(
      screen.getByRole('img', {
        name: 'Total 2,500. Laptop 1,200 (48%), Phone 1,000 (40%), Tablet 300 (12%)',
      }),
    ).toBeInTheDocument();
    expect(container.querySelector('.mn-donut-chart__value')).toHaveTextContent('2,500');
    const legend = screen.getByRole('list', { name: 'Legend' });
    expect(
      within(legend)
        .getAllByRole('listitem')
        .map((li) => li.textContent),
    ).toEqual(['Laptop 1,200 · 48%', 'Phone 1,000 · 40%', 'Tablet 300 · 12%']);
    const table = screen.getByRole('table');
    expect(within(table).getAllByRole('row')).toHaveLength(5);
    expect(within(table).getByRole('rowheader', { name: 'Total' })).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('draws one palette-coloured arc per non-zero segment, largest first', () => {
    const { container } = render(
      <DonutChart title="D" segments={[...devices, { key: 'tv', label: 'TV', value: 0 }]} />,
    );
    const a = arcs(container);
    expect(a).toHaveLength(3);
    expect(a[0]!.style.getPropertyValue('--_c')).toBe('var(--mn-color-data-1)');
    expect(a[0]!.querySelector('title')).toHaveTextContent(/^Laptop/);
    expect(container.querySelector('svg')).toHaveAttribute('aria-hidden', 'true');
  });

  it('keeps the given order with sort="none" and honours pinned colours', () => {
    const { container } = render(
      <DonutChart
        title="D"
        sort="none"
        segments={[
          { key: 'a', label: 'A', value: 1, color: 3 },
          { key: 'b', label: 'B', value: 5 },
        ]}
      />,
    );
    const [first, second] = arcs(container);
    expect(first!.querySelector('title')).toHaveTextContent(/^A/);
    expect(first!.style.getPropertyValue('--_c')).toBe('var(--mn-color-data-3)');
    expect(second!.style.getPropertyValue('--_c')).toBe('var(--mn-color-data-2)');
  });

  it('folds the smallest segments into a neutral Other but keeps them in the table', () => {
    const many = Array.from({ length: 8 }, (_, i) => ({
      key: `s${i}`,
      label: `S${i}`,
      value: 80 - i * 10,
    }));
    const { container } = render(
      <DonutChart
        title="D"
        segments={many}
        maxSegments={4}
        labels={{ other: 'Everything else' }}
      />,
    );
    const a = arcs(container);
    expect(a).toHaveLength(4);
    expect(a[3]!.style.getPropertyValue('--_c')).toBe('var(--mn-color-text-subtle)');
    expect(screen.getByRole('list', { name: 'Legend' })).toHaveTextContent('Everything else');
    expect(within(screen.getByRole('table')).getAllByRole('row')).toHaveLength(10);
  });

  it('switches to the visible table and back with the toggle', async () => {
    const user = userEvent.setup();
    const onShowTableChange = vi.fn();
    render(<DonutChart title="Devices" segments={devices} onShowTableChange={onShowTableChange} />);
    await user.tab();
    const toggle = screen.getByRole('button', { name: 'View as table' });
    expect(toggle).toHaveFocus();
    await user.keyboard('{Enter}');
    expect(onShowTableChange).toHaveBeenCalledWith(true);
    expect(screen.queryByRole('img')).toBeNull();
    expect(screen.getByRole('region', { name: 'Devices' })).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'View as chart' }));
    expect(screen.getByRole('img')).toBeInTheDocument();
  });

  it('supports a custom centre, formatting, legend content and summary', () => {
    const { container } = render(
      <DonutChart
        title="Spend"
        segments={devices}
        centerValue="$25K"
        centerLabel="this quarter"
        legendContent="percent"
        formatValue={(v) => `$${v}`}
        summary="Spend by device"
      />,
    );
    expect(container.querySelector('.mn-donut-chart__center')).toHaveTextContent(
      '$25Kthis quarter',
    );
    expect(screen.getByRole('list', { name: 'Legend' })).toHaveTextContent('Laptop 48%');
    expect(screen.getByRole('img', { name: 'Spend by device' })).toBeInTheDocument();
    expect(within(screen.getByRole('table')).getByText('$1200')).toBeInTheDocument();
  });

  it('can hide the centre and the legend', () => {
    const { container } = render(
      <DonutChart title="D" segments={devices} centerValue={null} showLegend={false} />,
    );
    expect(container.querySelector('.mn-donut-chart__center')).toBeNull();
    expect(screen.queryByRole('list', { name: 'Legend' })).toBeNull();
  });

  it('shows the empty state when the total is zero', () => {
    const { container } = render(
      <DonutChart title="D" segments={[{ key: 'a', label: 'A', value: 0 }]} />,
    );
    expect(container.querySelector('figure')).toHaveAttribute('data-status', 'empty');
    expect(screen.getByText('No data yet')).toBeInTheDocument();
  });

  it('passes loading and error states to ChartFrame', async () => {
    const onRetry = vi.fn();
    const { container, rerender } = render(
      <DonutChart title="D" segments={devices} status="loading" />,
    );
    expect(container.querySelector('[aria-busy="true"]')).not.toBeNull();
    rerender(<DonutChart title="D" segments={devices} status="error" onRetry={onRetry} />);
    await userEvent.setup().click(screen.getByRole('button', { name: 'Try again' }));
    expect(onRetry).toHaveBeenCalled();
  });

  it('keeps a visible sliver for tiny shares and sizes the ring', () => {
    const { container } = render(
      <DonutChart
        title="D"
        size={200}
        thickness={30}
        segments={[
          { key: 'big', label: 'Big', value: 100000 },
          { key: 'tiny', label: 'Tiny', value: 1 },
        ]}
        locale="en-US"
      />,
    );
    const [, tiny] = arcs(container);
    expect(Number(tiny!.getAttribute('stroke-dasharray')!.split(' ')[0])).toBeGreaterThan(0);
    expect(tiny!.getAttribute('stroke-width')).toBe('30');
    expect(screen.getByRole('list', { name: 'Legend' })).toHaveTextContent('Tiny 1 · <1%');
  });

  it('forwards the ref to the figure', () => {
    let node: HTMLElement | null = null;
    render(
      <DonutChart
        ref={(el) => {
          node = el;
        }}
        title="D"
        segments={devices}
      />,
    );
    expect(node!.tagName).toBe('FIGURE');
  });

  it('renders on the server', () => {
    const html = renderToString(<DonutChart title="Devices" segments={devices} locale="en-US" />);
    expect(html).toContain('mn-donut-chart');
    expect(html).toContain('2,500');
  });
});
