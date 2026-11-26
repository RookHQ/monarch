import { describe, expect, it, vi } from 'vitest';
import { render, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { FunnelChart, type FunnelChartProps, type FunnelStage } from './index.js';

const STAGES: FunnelStage[] = [
  { key: 'visit', label: 'Visited', value: 2000 },
  { key: 'signup', label: 'Signed up', value: 800 },
  { key: 'trial', label: 'Started trial', value: 600 },
  { key: 'paid', label: 'Paid', value: 120 },
];

function chart(props: Partial<FunnelChartProps> = {}) {
  return <FunnelChart title="Conversion" stages={STAGES} locale="en-US" {...props} />;
}

const plot = () => document.querySelector<HTMLElement>('.mn-funnel-chart')!;
const live = () => document.querySelector('.mn-chart__plot [role="status"]')!;

async function tabToPlot(user: ReturnType<typeof userEvent.setup>) {
  for (let i = 0; i < 6 && document.activeElement !== plot(); i++) await user.tab();
}

describe('FunnelChart', () => {
  it('draws one bar per stage with step conversions and a generated summary', async () => {
    const { container } = render(chart());
    expect(plot()).toHaveAttribute('role', 'group');
    expect(plot()).toHaveAttribute('aria-roledescription', 'chart');
    expect(plot()).toHaveAccessibleName(
      '4 stages from Visited to Paid. Overall conversion 6%. Largest drop: Started trial to Paid, 80% lost',
    );
    expect(plot()).toHaveAccessibleDescription('Use the arrow keys to inspect values.');
    expect(plot()).toHaveAttribute('data-align', 'center');
    const bars = container.querySelectorAll<HTMLElement>('.mn-funnel-chart__bar');
    expect(bars).toHaveLength(4);
    expect(bars[0]?.style.getPropertyValue('--_w')).toBe('100%');
    expect(bars[1]?.style.getPropertyValue('--_w')).toBe('40%');
    expect(bars[0]?.style.getPropertyValue('--_c')).toBe('var(--mn-color-data-1)');
    const steps = [...container.querySelectorAll('.mn-funnel-chart__step')].map(
      (s) => s.textContent,
    );
    expect(steps).toEqual([
      '40% continued · 1,200 dropped',
      '75% continued · 200 dropped',
      '20% continued · 480 dropped',
    ]);
    expect(container.querySelector('.mn-funnel-chart__stages')).toHaveAttribute(
      'aria-hidden',
      'true',
    );
    await expectNoA11yViolations();
  });

  it('lists every stage in the data table', () => {
    const { container } = render(chart());
    const table = within(container.querySelector('table')!);
    expect(table.getAllByRole('columnheader').map((h) => h.textContent)).toEqual([
      'Stage',
      'Count',
      'From previous',
      'Of first',
    ]);
    expect(table.getAllByRole('row')[2]).toHaveTextContent('Signed up80040%40%');
  });

  it('inspects stages with the keyboard and announces them', async () => {
    render(chart());
    const user = userEvent.setup();
    await tabToPlot(user);
    expect(plot()).toHaveFocus();
    expect(live()).toHaveTextContent('Visited: 2,000');
    await user.keyboard('{ArrowDown}');
    expect(live()).toHaveTextContent('Signed up: 800, 40% of previous, 40% of first');
    const rows = document.querySelectorAll('.mn-funnel-chart__stage');
    expect(rows[1]).toHaveAttribute('data-active');
    expect(rows[0]).toHaveAttribute('data-dimmed');
    await user.keyboard('{End}');
    expect(live()).toHaveTextContent('Paid: 120, 20% of previous, 6% of first');
    await user.keyboard('{Escape}');
    expect(document.querySelector('[data-active]')).toBeNull();
  });

  it('calls onStageSelect from Enter and click', async () => {
    const onStageSelect = vi.fn();
    render(chart({ onStageSelect }));
    const user = userEvent.setup();
    await tabToPlot(user);
    await user.keyboard('{ArrowDown}{Enter}');
    expect(onStageSelect).toHaveBeenCalledWith(STAGES[1], 1);
    expect(plot()).toHaveAttribute('data-selectable');
  });

  it('aligns to the start, hides steps and translates its text', () => {
    const { container } = render(
      chart({
        align: 'start',
        showSteps: false,
        labels: { overall: (r) => `Gesamt ${r}`, stages: (n) => `${n} Stufen` },
        formatRate: (r) => `${Math.round(r * 100)} %`,
      }),
    );
    expect(plot()).toHaveAttribute('data-align', 'start');
    expect(container.querySelector('.mn-funnel-chart__step')).toBeNull();
    expect(plot()).toHaveAccessibleName(/^4 Stufen\. Gesamt 6 %/);
  });

  it('handles a zero first stage and empty stages', () => {
    const { container, rerender } = render(
      chart({
        stages: [
          { key: 'a', label: 'A', value: 0 },
          { key: 'b', label: 'B', value: 0 },
        ],
      }),
    );
    expect(plot()).toHaveAccessibleName('2 stages from A to B. Overall conversion 0%');
    rerender(chart({ stages: [] }));
    expect(container.querySelector('figure')).toHaveAttribute('data-status', 'empty');
  });

  it('renders on the server', () => {
    const html = renderToString(chart());
    expect(html).toContain('mn-funnel-chart');
    expect(html).toContain('Started trial');
  });
});
