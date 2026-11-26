import { createRef } from 'react';
import { describe, expect, it, vi } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { DashboardOperational } from './index.js';

const kpis = [
  { id: 'a', label: 'Open', value: 12 },
  { id: 'b', label: 'Closed', value: 30 },
];
const insights = [
  {
    id: 'i1',
    severity: 'warning' as const,
    origin: 'rule' as const,
    title: 'Three items are overdue',
    why: 'Their due dates have passed.',
  },
];
const ranking = [
  { id: 'p1', name: 'Person A', value: 8 },
  { id: 'p2', name: 'Person B', value: 5 },
];
const events = [
  {
    id: 'e1',
    actor: { name: 'Person C' },
    verb: 'closed',
    object: 'Item 42',
    time: '2026-06-15T09:00:00Z',
  },
];
const NOW = '2026-06-15T10:00:00Z';

describe('DashboardOperational', () => {
  it('lays out the header, KPI row, insights and every section in a named grid', async () => {
    render(
      <DashboardOperational
        data-testid="root"
        title="Home"
        description="Sample workspace"
        scope={<button type="button">Space</button>}
        period={<button type="button">This week</button>}
        actions={<button type="button">Arrange</button>}
        kpis={kpis}
        insights={insights}
        activityProps={{ now: NOW }}
        status={<p>status content</p>}
        attention={<p>attention content</p>}
        queue={<p>queue content</p>}
        trend={<p>trend content</p>}
        workload={ranking}
        upcoming={<p>upcoming content</p>}
        activity={events}
      />,
    );
    expect(screen.getByTestId('root')).toHaveClass('mn-dashboard-operational');
    expect(screen.getByRole('heading', { level: 1, name: 'Home' })).toBeVisible();
    expect(screen.getByText('Sample workspace')).toBeVisible();
    expect(screen.getByRole('group', { name: 'Key figures' })).toHaveTextContent('Open');
    expect(screen.getByText('Three items are overdue')).toBeVisible();
    const grid = screen.getByRole('list', { name: 'Dashboard sections' });
    const cells = within(grid)
      .getAllByRole('listitem')
      .filter((li) => li.parentElement === grid);
    expect(cells).toHaveLength(7);
    expect(within(grid).getByRole('heading', { level: 2, name: 'Status' })).toBeVisible();
    expect(within(grid).getByRole('heading', { level: 2, name: 'Needs attention' })).toBeVisible();
    expect(within(grid).getByRole('heading', { level: 2, name: 'My queue' })).toBeVisible();
    expect(within(grid).getByRole('heading', { level: 2, name: 'Trend' })).toBeVisible();
    expect(within(grid).getByRole('heading', { level: 2, name: 'Team load' })).toBeVisible();
    expect(within(grid).getByRole('heading', { level: 2, name: 'Coming up' })).toBeVisible();
    expect(within(grid).getByRole('heading', { level: 2, name: 'Recent activity' })).toBeVisible();
    expect(screen.getByText('Person A')).toBeVisible();
    expect(screen.getByText('closed')).toBeVisible();
    await expectNoA11yViolations();
  });

  it('takes slot config objects, leaves out empty sections and appends extra widgets', () => {
    render(
      <DashboardOperational
        title="Home"
        status={{
          content: <p>Custom content</p>,
          title: 'Custom title',
          actions: <button type="button">Filter</button>,
        }}
        attention={null}
        widgets={[{ id: 'extra', label: 'Extra', content: <p>Extra content</p> }]}
      />,
    );
    const grid = screen.getByRole('list', { name: 'Dashboard sections' });
    const cells = within(grid)
      .getAllByRole('listitem')
      .filter((li) => li.parentElement === grid);
    expect(cells).toHaveLength(2);
    expect(within(cells[0]!).getByRole('heading', { name: 'Custom title' })).toBeVisible();
    expect(within(cells[0]!).getByRole('button', { name: 'Filter' })).toBeVisible();
    expect(cells[1]).toHaveTextContent('Extra content');
    expect(screen.queryByRole('group', { name: 'Key figures' })).toBeNull();
  });

  it('shows a loading widget from a slot config', () => {
    render(
      <DashboardOperational title="Home" status={{ content: <p>Hidden</p>, loading: true }} />,
    );
    expect(screen.queryByText('Hidden')).toBeNull();
    expect(screen.getByRole('heading', { name: 'Status' })).toBeVisible();
  });

  it('reaches header controls by keyboard and moves sections in arrange mode', async () => {
    const user = userEvent.setup();
    const onLayoutChange = vi.fn();
    render(
      <DashboardOperational
        title="Home"
        actions={<button type="button">Arrange</button>}
        editing
        onLayoutChange={onLayoutChange}
        status={<p>One</p>}
        attention={<p>Two</p>}
      />,
    );
    await user.tab();
    expect(screen.getByRole('button', { name: 'Arrange' })).toHaveFocus();
    await user.click(screen.getByRole('button', { name: 'Move Status later' }));
    expect(onLayoutChange).toHaveBeenCalledWith(
      ['attention', 'status'],
      expect.objectContaining({ id: 'status', from: 0, to: 1 }),
    );
  });

  it('translates its text, shifts heading levels and forwards the ref', () => {
    const ref = createRef<HTMLDivElement>();
    render(
      <DashboardOperational
        ref={ref}
        title="Inicio"
        headingLevel={2}
        kpis={kpis}
        status={<p>Uno</p>}
        messages={{ kpis: 'Cifras', grid: 'Secciones', status: 'Primero' }}
      />,
    );
    expect(ref.current).toHaveClass('mn-dashboard-operational');
    expect(screen.getByRole('heading', { level: 2, name: 'Inicio' })).toBeVisible();
    expect(screen.getByRole('group', { name: 'Cifras' })).toBeVisible();
    const grid = screen.getByRole('list', { name: 'Secciones' });
    expect(within(grid).getByRole('heading', { level: 3, name: 'Primero' })).toBeVisible();
  });

  it('renders on the server', () => {
    const html = renderToString(
      <DashboardOperational title="Home" kpis={kpis} status={<p>Server</p>} />,
    );
    expect(html).toContain('mn-dashboard-operational');
    expect(html).toContain('Server');
  });
});
