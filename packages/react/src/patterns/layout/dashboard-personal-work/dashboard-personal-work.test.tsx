import { createRef } from 'react';
import { describe, expect, it, vi } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { DashboardPersonalWork } from './index.js';

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

describe('DashboardPersonalWork', () => {
  it('lays out the header, KPI row, insights and every section in a named grid', async () => {
    render(
      <DashboardPersonalWork
        data-testid="root"
        title="Home"
        description="Sample workspace"
        scope={<button type="button">Space</button>}
        period={<button type="button">This week</button>}
        actions={<button type="button">Arrange</button>}
        kpis={kpis}
        insights={insights}
        activityProps={{ now: NOW }}
        today={{ today: '2026-06-15', tasks: [{ id: 't1', title: 'Send the summary' }] }}
        assigned={<p>assigned content</p>}
        dueSoon={<p>dueSoon content</p>}
        progress={<p>progress content</p>}
        announcements={<p>announcements content</p>}
        team={<p>team content</p>}
        activity={events}
      />,
    );
    expect(screen.getByTestId('root')).toHaveClass('mn-dashboard-personal-work');
    expect(screen.getByRole('heading', { level: 1, name: 'Home' })).toBeVisible();
    expect(screen.getByText('Sample workspace')).toBeVisible();
    expect(screen.getByRole('group', { name: 'Key figures' })).toHaveTextContent('Open');
    expect(screen.getByText('Three items are overdue')).toBeVisible();
    const grid = screen.getByRole('list', { name: 'Dashboard sections' });
    const cells = within(grid)
      .getAllByRole('listitem')
      .filter((li) => li.parentElement === grid);
    expect(cells).toHaveLength(7);
    expect(within(grid).getByRole('heading', { level: 2, name: 'Today' })).toBeVisible();
    expect(within(grid).getByRole('heading', { level: 2, name: 'Assigned to you' })).toBeVisible();
    expect(within(grid).getByRole('heading', { level: 2, name: 'Due soon' })).toBeVisible();
    expect(within(grid).getByRole('heading', { level: 2, name: 'Progress' })).toBeVisible();
    expect(within(grid).getByRole('heading', { level: 2, name: 'Announcements' })).toBeVisible();
    expect(within(grid).getByRole('heading', { level: 2, name: 'Your team' })).toBeVisible();
    expect(within(grid).getByRole('heading', { level: 2, name: 'Recent activity' })).toBeVisible();
    expect(screen.getByText('Send the summary')).toBeVisible();
    expect(screen.getByText('closed')).toBeVisible();
    await expectNoA11yViolations();
  });

  it('takes slot config objects, leaves out empty sections and appends extra widgets', () => {
    render(
      <DashboardPersonalWork
        title="Home"
        assigned={{
          content: <p>Custom content</p>,
          title: 'Custom title',
          actions: <button type="button">Filter</button>,
        }}
        dueSoon={null}
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
      <DashboardPersonalWork title="Home" assigned={{ content: <p>Hidden</p>, loading: true }} />,
    );
    expect(screen.queryByText('Hidden')).toBeNull();
    expect(screen.getByRole('heading', { name: 'Assigned to you' })).toBeVisible();
  });

  it('reaches header controls by keyboard and moves sections in arrange mode', async () => {
    const user = userEvent.setup();
    const onLayoutChange = vi.fn();
    render(
      <DashboardPersonalWork
        title="Home"
        actions={<button type="button">Arrange</button>}
        editing
        onLayoutChange={onLayoutChange}
        assigned={<p>One</p>}
        dueSoon={<p>Two</p>}
      />,
    );
    await user.tab();
    expect(screen.getByRole('button', { name: 'Arrange' })).toHaveFocus();
    await user.click(screen.getByRole('button', { name: 'Move Assigned to you later' }));
    expect(onLayoutChange).toHaveBeenCalledWith(
      ['dueSoon', 'assigned'],
      expect.objectContaining({ id: 'assigned', from: 0, to: 1 }),
    );
  });

  it('translates its text, shifts heading levels and forwards the ref', () => {
    const ref = createRef<HTMLDivElement>();
    render(
      <DashboardPersonalWork
        ref={ref}
        title="Inicio"
        headingLevel={2}
        kpis={kpis}
        assigned={<p>Uno</p>}
        messages={{ kpis: 'Cifras', grid: 'Secciones', assigned: 'Primero' }}
      />,
    );
    expect(ref.current).toHaveClass('mn-dashboard-personal-work');
    expect(screen.getByRole('heading', { level: 2, name: 'Inicio' })).toBeVisible();
    expect(screen.getByRole('group', { name: 'Cifras' })).toBeVisible();
    const grid = screen.getByRole('list', { name: 'Secciones' });
    expect(within(grid).getByRole('heading', { level: 3, name: 'Primero' })).toBeVisible();
  });

  it('renders on the server', () => {
    const html = renderToString(
      <DashboardPersonalWork title="Home" kpis={kpis} assigned={<p>Server</p>} />,
    );
    expect(html).toContain('mn-dashboard-personal-work');
    expect(html).toContain('Server');
  });
});
