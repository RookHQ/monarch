import { createRef } from 'react';
import { describe, expect, it, vi } from 'vitest';
import { fireEvent, render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { UptimeBar, UptimeLegend, uptimeStatus, type UptimeBarProps } from './index.js';

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

const SEGMENTS = [
  { key: '2026-03-01', label: 'Mar 1', uptime: null },
  { key: '2026-03-02', label: 'Mar 2', uptime: 100, details: [{ label: 'Checks', value: 1440 }] },
  { key: '2026-03-03', label: 'Mar 3', uptime: 99.5 },
  { key: '2026-03-04', label: 'Mar 4', uptime: 97.25, note: 'Database failover' },
  { key: '2026-03-05', label: 'Mar 5', status: 'maintenance' as const, uptime: 100 },
];

function bar(props: Partial<UptimeBarProps> = {}) {
  return (
    <UptimeBar
      label="API"
      segments={SEGMENTS}
      startLabel="5 days ago"
      endLabel="Today"
      locale="en-US"
      {...props}
    />
  );
}

const plot = () => screen.getByRole('group', { name: /^API 5 periods/ });
const live = () => document.querySelector('[role="status"]')!;

describe('uptimeStatus', () => {
  it('derives a state from uptime and keeps an explicit one', () => {
    expect(uptimeStatus({ uptime: 99.95 })).toBe('operational');
    expect(uptimeStatus({ uptime: 99.2 })).toBe('degraded');
    expect(uptimeStatus({ uptime: 90 })).toBe('down');
    expect(uptimeStatus({ uptime: null })).toBe('nodata');
    expect(uptimeStatus({ status: 'partial', uptime: 100 })).toBe('partial');
    expect(uptimeStatus({ uptime: 99.5 }, { operational: 99.5, degraded: 98 })).toBe('operational');
  });
});

describe('UptimeBar', () => {
  it('draws a segment per period with states, overall uptime and a summary', async () => {
    const { container } = render(bar());
    const segs = container.querySelectorAll('.mn-uptime-bar__segment');
    expect([...segs].map((s) => s.getAttribute('data-status'))).toEqual([
      'nodata',
      'operational',
      'degraded',
      'down',
      'maintenance',
    ]);
    expect(plot()).toHaveAccessibleName(
      'API 5 periods: 1 operational, 1 degraded, 1 down, 1 maintenance, 1 no data. Uptime 99.19%.',
    );
    expect(container.querySelector('.mn-uptime-bar__footer')).toHaveTextContent(
      '5 days ago99.19% uptimeToday',
    );
    expect(container.querySelector('.mn-uptime-bar')).toHaveAttribute('data-size', 'md');
    await expectNoA11yViolations();
  });

  it('keeps a data table with states, uptime and details for screen readers', () => {
    render(bar());
    const table = screen.getByRole('table');
    expect(within(table).getAllByRole('row')).toHaveLength(6);
    expect(within(table).getByRole('rowheader', { name: 'Mar 4' })).toBeInTheDocument();
    expect(within(table).getByText('Database failover')).toBeInTheDocument();
    expect(within(table).getByText('Checks 1440')).toBeInTheDocument();
    expect(within(table).getAllByText('No data')).toHaveLength(2);
  });

  it('inspects segments with the keyboard, starting at the newest, and selects one', async () => {
    const onSegmentSelect = vi.fn();
    render(bar({ onSegmentSelect }));
    const user = userEvent.setup();
    await user.tab();
    expect(plot()).toHaveFocus();
    expect(live()).toHaveTextContent('Mar 5: Maintenance, Uptime 100.00%');
    await user.keyboard('{ArrowLeft}');
    expect(live()).toHaveTextContent('Mar 4: Down, Uptime 97.25%, Database failover');
    await user.keyboard('{Home}');
    expect(live()).toHaveTextContent('Mar 1: No data');
    await user.keyboard('{ArrowRight}');
    expect(live()).toHaveTextContent('Mar 2: Operational, Uptime 100.00%, Checks 1440');
    await user.keyboard('{Enter}');
    expect(onSegmentSelect).toHaveBeenCalledWith(SEGMENTS[1], 1);
    await user.keyboard('{End}{Escape}');
    expect(live()).toHaveTextContent('');
    await expectNoA11yViolations();
  });

  it('shows a tooltip on hover and selects on click', () => {
    const onSegmentSelect = vi.fn();
    const { container } = render(bar({ onSegmentSelect }));
    const seg = container.querySelector('[data-index="3"]')!;
    fireEvent.pointerMove(seg, { pointerType: 'mouse' });
    const tip = container.querySelector('.mn-uptime-bar__tooltip')!;
    expect(tip).toHaveTextContent('Mar 4');
    expect(tip).toHaveTextContent('Down');
    expect(tip).toHaveTextContent('97.25%');
    expect(tip).toHaveTextContent('Database failover');
    expect(tip).toHaveAttribute('data-side', 'left');
    expect(live()).toHaveTextContent('');
    fireEvent.click(seg);
    expect(onSegmentSelect).toHaveBeenCalledWith(SEGMENTS[3], 3);
    fireEvent.pointerLeave(plot(), { pointerType: 'mouse' });
    expect(container.querySelector('.mn-uptime-bar__tooltip')).toBeNull();
  });

  it('renders the small size, a meta slot, custom thresholds, labels and a hidden overall', () => {
    const { container } = render(
      bar({
        size: 'sm',
        meta: <span>120 ms</span>,
        uptime: null,
        thresholds: { operational: 99, degraded: 95 },
        labels: { status: { down: 'Panne' }, summary: (n) => `${n} jours` },
      }),
    );
    expect(container.querySelector('.mn-uptime-bar')).toHaveAttribute('data-size', 'sm');
    expect(screen.getByText('120 ms')).toBeInTheDocument();
    expect(container.querySelector('.mn-uptime-bar__overall')).toBeNull();
    expect(container.querySelector('[data-index="2"]')).toHaveAttribute(
      'data-status',
      'operational',
    );
    expect(screen.getByRole('group', { name: 'API 5 jours' })).toBeInTheDocument();
  });

  it('shows a legend that names every state', () => {
    render(bar({ showLegend: true }));
    const legend = screen.getByRole('list', { name: 'Legend' });
    expect(
      within(legend)
        .getAllByRole('listitem')
        .map((li) => li.textContent),
    ).toEqual(['Operational', 'Degraded', 'Partial outage', 'Down', 'Maintenance', 'No data']);
  });

  it('renders a standalone legend for a page of bars', async () => {
    render(<UptimeLegend statuses={['operational', 'down']} labels={{ legend: 'States' }} />);
    expect(screen.getByRole('list', { name: 'States' })).toHaveTextContent('OperationalDown');
    await expectNoA11yViolations();
  });

  it('supports a controlled active segment and an empty bar', () => {
    const onActiveIndexChange = vi.fn();
    const { container, rerender } = render(bar({ activeIndex: 2, onActiveIndexChange }));
    expect(container.querySelector('[data-active]')).toHaveAttribute('data-index', '2');
    fireEvent.pointerMove(container.querySelector('[data-index="0"]')!, { pointerType: 'mouse' });
    expect(onActiveIndexChange).toHaveBeenCalledWith(0);
    rerender(bar({ segments: [] }));
    expect(container.querySelector('.mn-uptime-bar')).toHaveAttribute('data-empty');
    expect(screen.queryByRole('table')).toBeNull();
  });

  it('forwards the ref and extra props', () => {
    const ref = createRef<HTMLDivElement>();
    render(<UptimeBar ref={ref} segments={SEGMENTS} data-testid="u" />);
    expect(ref.current).toBe(screen.getByTestId('u'));
  });

  it('renders on the server', () => {
    const html = renderToString(bar());
    expect(html).toContain('mn-uptime-bar');
    expect(html).toContain('data-status="down"');
  });
});
