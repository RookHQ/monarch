import { createRef } from 'react';
import { describe, expect, it, vi } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { DashboardPipeline } from './index.js';

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

describe('DashboardPipeline', () => {
  it('lays out the header, KPI row, insights and every section in a named grid', async () => {
    render(
      <DashboardPipeline
        data-testid="root"
        title="Home"
        description="Sample workspace"
        scope={<button type="button">Space</button>}
        period={<button type="button">This week</button>}
        actions={<button type="button">Arrange</button>}
        kpis={kpis}
        insights={insights}
        activityProps={{ now: NOW }}
        pipeline={<p>pipeline content</p>}
        today={{ today: '2026-06-15', tasks: [{ id: 't1', title: 'Send the summary' }] }}
        followUps={<p>followUps content</p>}
        upcoming={<p>upcoming content</p>}
        performance={<p>performance content</p>}
        leaders={ranking}
        activity={events}
      />,
    );
    expect(screen.getByTestId('root')).toHaveClass('mn-dashboard-pipeline');
    expect(screen.getByRole('heading', { level: 1, name: 'Home' })).toBeVisible();
    expect(screen.getByText('Sample workspace')).toBeVisible();
    expect(screen.getByRole('group', { name: 'Key figures' })).toHaveTextContent('Open');
    expect(screen.getByText('Three items are overdue')).toBeVisible();
    const grid = screen.getByRole('list', { name: 'Dashboard sections' });
    const cells = within(grid)
      .getAllByRole('listitem')
      .filter((li) => li.parentElement === grid);
    expect(cells).toHaveLength(7);
    expect(within(grid).getByRole('heading', { level: 2, name: 'Pipeline' })).toBeVisible();
    expect(within(grid).getByRole('heading', { level: 2, name: 'Today' })).toBeVisible();
    expect(within(grid).getByRole('heading', { level: 2, name: 'Waiting on you' })).toBeVisible();
    expect(within(grid).getByRole('heading', { level: 2, name: 'Coming up' })).toBeVisible();
    expect(within(grid).getByRole('heading', { level: 2, name: 'Performance' })).toBeVisible();
    expect(within(grid).getByRole('heading', { level: 2, name: 'Top performers' })).toBeVisible();
    expect(within(grid).getByRole('heading', { level: 2, name: 'Recent activity' })).toBeVisible();
    expect(screen.getByText('Send the summary')).toBeVisible();
    expect(screen.getByText('Person A')).toBeVisible();
    expect(screen.getByText('closed')).toBeVisible();
    await expectNoA11yViolations();
  });

  it('takes slot config objects, leaves out empty sections and appends extra widgets', () => {
    render(
      <DashboardPipeline
        title="Home"
        pipeline={{
          content: <p>Custom content</p>,
          title: 'Custom title',
          actions: <button type="button">Filter</button>,
        }}
        followUps={null}
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
    render(<DashboardPipeline title="Home" pipeline={{ content: <p>Hidden</p>, loading: true }} />);
    expect(screen.queryByText('Hidden')).toBeNull();
    expect(screen.getByRole('heading', { name: 'Pipeline' })).toBeVisible();
  });

  it('reaches header controls by keyboard and moves sections in arrange mode', async () => {
    const user = userEvent.setup();
    const onLayoutChange = vi.fn();
    render(
      <DashboardPipeline
        title="Home"
        actions={<button type="button">Arrange</button>}
        editing
        onLayoutChange={onLayoutChange}
        pipeline={<p>One</p>}
        followUps={<p>Two</p>}
      />,
    );
    await user.tab();
    expect(screen.getByRole('button', { name: 'Arrange' })).toHaveFocus();
    await user.click(screen.getByRole('button', { name: 'Move Pipeline later' }));
    expect(onLayoutChange).toHaveBeenCalledWith(
      ['followUps', 'pipeline'],
      expect.objectContaining({ id: 'pipeline', from: 0, to: 1 }),
    );
  });

  it('translates its text, shifts heading levels and forwards the ref', () => {
    const ref = createRef<HTMLDivElement>();
    render(
      <DashboardPipeline
        ref={ref}
        title="Inicio"
        headingLevel={2}
        kpis={kpis}
        pipeline={<p>Uno</p>}
        messages={{ kpis: 'Cifras', grid: 'Secciones', pipeline: 'Primero' }}
      />,
    );
    expect(ref.current).toHaveClass('mn-dashboard-pipeline');
    expect(screen.getByRole('heading', { level: 2, name: 'Inicio' })).toBeVisible();
    expect(screen.getByRole('group', { name: 'Cifras' })).toBeVisible();
    const grid = screen.getByRole('list', { name: 'Secciones' });
    expect(within(grid).getByRole('heading', { level: 3, name: 'Primero' })).toBeVisible();
  });

  it('renders on the server', () => {
    const html = renderToString(
      <DashboardPipeline title="Home" kpis={kpis} pipeline={<p>Server</p>} />,
    );
    expect(html).toContain('mn-dashboard-pipeline');
    expect(html).toContain('Server');
  });
});
