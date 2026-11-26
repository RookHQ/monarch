import { describe, expect, it, vi } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { BurndownChart, type BurndownChartProps, type BurndownPoint } from './index.js';

const SPRINT: BurndownPoint[] = [
  { date: 'D1', remaining: 40, scope: 40 },
  { date: 'D2', remaining: 36, scope: 40 },
  { date: 'D3', remaining: 38, scope: 45 },
  { date: 'D4', remaining: 25, scope: 43 },
  { date: 'D5', remaining: null, scope: null },
];

function chart(props: Partial<BurndownChartProps> = {}) {
  return <BurndownChart title="Sprint 12" data={SPRINT} locale="en-US" {...props} />;
}

const plot = () => document.querySelector<HTMLElement>('.mn-burndown-chart')!;
const live = () => document.querySelector('.mn-chart__plot [role="status"]')!;

async function tabToPlot(user: ReturnType<typeof userEvent.setup>) {
  for (let i = 0; i < 8 && document.activeElement !== plot(); i++) await user.tab();
}

describe('BurndownChart', () => {
  it('draws remaining, ideal and scope with a status summary', async () => {
    const { container } = render(chart());
    expect(plot()).toHaveAttribute('role', 'group');
    expect(plot()).toHaveAccessibleName(
      'D4: 25 remaining, ideal 10, 15 behind ideal. Scope 43, 5 added and 2 removed since the start',
    );
    expect(plot()).toHaveAttribute('data-trend', 'behind');
    expect(container.querySelector('figure')).toHaveAttribute('data-trend', 'behind');
    expect(container.querySelector('[data-series="remaining"] .mn-chart__line')).not.toBeNull();
    expect(container.querySelector('[data-series="ideal"] .mn-chart__line')).toHaveAttribute(
      'data-dashed',
    );
    // Scope changes are marked +5 on day 3 and −2 on day 4.
    const changes = [...container.querySelectorAll('.mn-burndown-chart__change')];
    expect(changes.map((c) => c.textContent)).toEqual(['+5', '−2']);
    expect(changes[0]).toHaveAttribute('data-direction', 'up');
    // Today sits on the last day with data.
    expect(container.querySelector('.mn-burndown-chart__today')).toHaveTextContent('Today');
    expect(screen.getByRole('toolbar', { name: 'Legend' })).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('lists every day in the data table with scope changes', () => {
    const { container } = render(chart());
    const table = within(container.querySelector('table')!);
    const headers = table.getAllByRole('columnheader').map((h) => h.textContent);
    expect(headers).toEqual(['Day', 'Remaining', 'Ideal', 'Scope', 'Scope change']);
    expect(table.getAllByRole('row')[3]).toHaveTextContent('D3382045+5');
    expect(table.getAllByRole('row')[5]).toHaveTextContent('D5No data0No data');
  });

  it('inspects days with the keyboard and announces them', async () => {
    render(chart());
    const user = userEvent.setup();
    await tabToPlot(user);
    expect(plot()).toHaveFocus();
    expect(live()).toHaveTextContent('D1: Remaining 40, Ideal 40, Scope 40');
    await user.keyboard('{ArrowRight}{ArrowRight}');
    expect(live()).toHaveTextContent('D3: Remaining 38, Ideal 20, Scope 45, Scope change +5');
    const tooltip = document.querySelector('.mn-chart__tooltip')!;
    expect(tooltip).toHaveAttribute('aria-hidden', 'true');
    expect(tooltip).toHaveTextContent('Scope change +5');
    await user.keyboard('{End}');
    expect(live()).toHaveTextContent('D5: Remaining No data');
    await user.keyboard('{Escape}');
    expect(document.querySelector('.mn-chart__tooltip')).toBeNull();
  });

  it('hides lines from the legend but keeps one visible', async () => {
    const onHiddenSeriesChange = vi.fn();
    const { container } = render(chart({ onHiddenSeriesChange }));
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Ideal' }));
    expect(onHiddenSeriesChange).toHaveBeenLastCalledWith(['ideal']);
    expect(container.querySelector('[data-series="ideal"]')).toBeNull();
    await user.click(screen.getByRole('button', { name: 'Scope' }));
    expect(screen.getByRole('button', { name: 'Remaining' })).toHaveAttribute(
      'aria-disabled',
      'true',
    );
  });

  it('reports ahead of ideal and omits scope when no day has one', () => {
    const { container } = render(
      chart({
        data: [
          { date: 'D1', remaining: 20 },
          { date: 'D2', remaining: 8 },
          { date: 'D3', remaining: null },
        ],
        todayIndex: null,
      }),
    );
    expect(plot()).toHaveAccessibleName('D2: 8 remaining, ideal 10, 2 ahead of ideal');
    expect(screen.queryByRole('button', { name: 'Scope' })).toBeNull();
    expect(container.querySelector('.mn-burndown-chart__today')).toBeNull();
  });

  it('uses idealStart, custom formatting and translated labels', () => {
    render(
      chart({
        idealStart: 50,
        formatValue: (v) => `${v} pts`,
        labels: { remaining: 'Offen', status: (d, r) => `${d} ${r}`, onTrack: 'ok' },
        showScope: false,
      }),
    );
    expect(plot()).toHaveAccessibleName(/^D4 25 pts, 12.5 pts behind ideal$/);
    expect(screen.getByRole('button', { name: 'Offen' })).toBeInTheDocument();
  });

  it('calls onDaySelect from Enter', async () => {
    const onDaySelect = vi.fn();
    render(chart({ onDaySelect }));
    const user = userEvent.setup();
    await tabToPlot(user);
    await user.keyboard('{ArrowRight}{Enter}');
    expect(onDaySelect).toHaveBeenCalledWith(SPRINT[1], 1);
  });

  it('shows not started and the empty state', () => {
    const { rerender, container } = render(
      chart({ data: [{ date: 'D1', scope: 10 }, { date: 'D2' }] }),
    );
    expect(plot()).toHaveAccessibleName(/^Not started/);
    rerender(chart({ data: [] }));
    expect(container.querySelector('figure')).toHaveAttribute('data-status', 'empty');
  });

  it('renders on the server', () => {
    const html = renderToString(chart());
    expect(html).toContain('mn-burndown-chart');
    expect(html).toContain('viewBox="0 0 640 240"');
  });
});
