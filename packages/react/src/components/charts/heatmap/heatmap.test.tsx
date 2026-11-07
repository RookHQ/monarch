import { createRef } from 'react';
import { describe, expect, it, vi } from 'vitest';
import { fireEvent, render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { Heatmap, heatmapScale, type HeatmapProps } from './index.js';

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

const ROWS = [
  { key: 'mon', label: 'Mon' },
  { key: 'tue', label: 'Tue' },
];
const COLUMNS = [
  { key: '9', label: '9:00' },
  { key: '10', label: '10:00' },
  { key: '11', label: '11:00' },
];
const DATA = [
  { row: 'mon', column: '9', value: 0 },
  { row: 'mon', column: '10', value: 4 },
  { row: 'mon', column: '11', value: 8 },
  { row: 'tue', column: '9', value: 2 },
  { row: 'tue', column: '10', value: 6, note: 'Busiest' },
];

function chart(props: Partial<HeatmapProps> = {}) {
  return (
    <Heatmap
      title="Sign-ins"
      rows={ROWS}
      columns={COLUMNS}
      data={DATA}
      rowHeader="Day"
      locale="en-US"
      {...props}
    />
  );
}

const plot = () => screen.getByRole('group', { name: /2 rows by 3 columns/ });
const live = () => document.querySelector('[role="status"]')!;

describe('heatmapScale', () => {
  it('splits the largest value into quarters and keeps zero and gaps apart', () => {
    const s = heatmapScale([0, 4, 8, null]);
    expect(s.thresholds).toEqual([2, 4, 6]);
    expect([null, 0, 1, 2, 4, 6, 8].map((v) => s.level(v))).toEqual([null, 0, 1, 2, 3, 4, 4]);
  });

  it('uses fixed thresholds', () => {
    const s = heatmapScale([1000], [10, 20, 30]);
    expect([5, 10, 25, 1000].map((v) => s.level(v))).toEqual([1, 2, 3, 4]);
  });
});

describe('Heatmap', () => {
  it('draws one cell per row and column with levels, a summary and a legend', async () => {
    const { container } = render(chart());
    const cells = container.querySelectorAll('.mn-heatmap__cell');
    expect(cells).toHaveLength(6);
    expect([...cells].map((c) => c.getAttribute('data-level'))).toEqual([
      '0',
      '3',
      '4',
      '2',
      '4',
      null,
    ]);
    expect(cells[5]).toHaveAttribute('data-empty');
    expect(plot()).toHaveAccessibleName('2 rows by 3 columns. Highest Mon, 11:00 8');
    const legend = screen.getByRole('list', { name: 'Legend' });
    expect(within(legend).getAllByRole('listitem')).toHaveLength(6);
    expect(legend).toHaveTextContent('No data');
    expect(legend).toHaveTextContent('≥ 6');
    expect(container.querySelector('.mn-heatmap')).toHaveStyle({
      '--_c': 'var(--mn-color-data-1)',
    });
    await expectNoA11yViolations();
  });

  it('lists every value in the data table', () => {
    render(chart());
    const table = screen.getByRole('table');
    expect(within(table).getByRole('columnheader', { name: 'Day' })).toBeInTheDocument();
    expect(within(table).getByRole('rowheader', { name: 'Tue' })).toBeInTheDocument();
    expect(within(table).getByText('6, Busiest')).toBeInTheDocument();
    expect(within(table).getAllByText('No data')).toHaveLength(1);
  });

  it('inspects cells in two dimensions with the keyboard and selects one', async () => {
    const onCellSelect = vi.fn();
    render(chart({ onCellSelect }));
    const user = userEvent.setup();
    for (let i = 0; i < 6 && document.activeElement !== plot(); i++) await user.tab();
    expect(plot()).toHaveFocus();
    expect(live()).toHaveTextContent('Mon, 9:00: 0');
    await user.keyboard('{ArrowRight}');
    expect(live()).toHaveTextContent('Mon, 10:00: 4');
    await user.keyboard('{ArrowDown}');
    expect(live()).toHaveTextContent('Tue, 10:00: 6, Busiest');
    await user.keyboard('{End}');
    expect(live()).toHaveTextContent('Tue, 11:00: No data');
    await user.keyboard('{Control>}{Home}{/Control}');
    expect(live()).toHaveTextContent('Mon, 9:00');
    await user.keyboard('{ArrowUp}{ArrowLeft}');
    expect(live()).toHaveTextContent('Mon, 9:00');
    await user.keyboard('{Enter}');
    expect(onCellSelect).toHaveBeenCalledWith(DATA[0]);
    await user.keyboard('{Escape}');
    expect(live()).toHaveTextContent('');
    expect(document.querySelector('.mn-chart__tooltip')).toBeNull();
    await expectNoA11yViolations();
  });

  it('shows a tooltip on hover and selects on click', () => {
    const onCellSelect = vi.fn();
    const { container } = render(chart({ onCellSelect }));
    const cell = container.querySelector('[data-cell="1:1"]')!;
    fireEvent.pointerMove(cell, { pointerType: 'mouse' });
    const tip = container.querySelector('.mn-chart__tooltip')!;
    expect(tip).toHaveTextContent('Tue · 10:00');
    expect(tip).toHaveTextContent('Busiest');
    expect(live()).toHaveTextContent('');
    fireEvent.click(cell);
    expect(onCellSelect).toHaveBeenCalledWith(DATA[4]);
    fireEvent.pointerLeave(plot(), { pointerType: 'mouse' });
    expect(container.querySelector('.mn-chart__tooltip')).toBeNull();
  });

  it('supports a controlled active cell', () => {
    const onActiveCellChange = vi.fn();
    const { container } = render(
      chart({ activeCell: { row: 'tue', column: '9' }, onActiveCellChange }),
    );
    expect(container.querySelector('[data-active]')).toHaveAttribute('data-cell', '1:0');
    fireEvent.pointerMove(container.querySelector('[data-cell="0:2"]')!, { pointerType: 'mouse' });
    expect(onActiveCellChange).toHaveBeenCalledWith({ row: 'mon', column: '11' });
    expect(container.querySelector('[data-active]')).toHaveAttribute('data-cell', '1:0');
  });

  it('prints values, uses value labels, a colour slot and translated words', () => {
    const { container } = render(
      chart({
        showValues: true,
        color: 3,
        data: [{ row: 'mon', column: '9', value: 90, valueLabel: '1h 30m' }],
        labels: { less: 'Moins', more: 'Plus', summary: (r, c) => `Grille ${r}×${c}` },
      }),
    );
    expect(container.querySelector('.mn-heatmap__value')).toHaveTextContent('1h 30m');
    expect(container.querySelector('.mn-heatmap')).toHaveStyle({
      '--_c': 'var(--mn-color-data-3)',
    });
    expect(screen.getByRole('group', { name: 'Grille 2×3' })).toBeInTheDocument();
    expect(screen.getByText('Moins')).toBeInTheDocument();
  });

  it('shows the empty state without values and hides the legend on request', () => {
    const { container, rerender } = render(chart({ data: [] }));
    expect(container.querySelector('figure')).toHaveAttribute('data-status', 'empty');
    rerender(chart({ showLegend: false }));
    expect(screen.queryByRole('list', { name: 'Legend' })).toBeNull();
  });

  it('forwards the ref to the figure', () => {
    const ref = createRef<HTMLElement>();
    render(<Heatmap ref={ref} title="Sign-ins" rows={ROWS} columns={COLUMNS} data={DATA} />);
    expect(ref.current?.tagName).toBe('FIGURE');
  });

  it('renders on the server', () => {
    const html = renderToString(chart({ showValues: true }));
    expect(html).toContain('mn-heatmap');
    expect(html).toContain('data-level="4"');
  });
});
