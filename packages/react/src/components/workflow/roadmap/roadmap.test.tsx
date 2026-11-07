import { describe, expect, it, vi } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { Roadmap, type RoadmapGroup, type RoadmapItem } from './index.js';

const TODAY = '2026-01-14';
const GROUPS: RoadmapGroup[] = [
  {
    id: 'growth',
    label: 'Grow repeat customers',
    start: '2026-01-01',
    end: '2026-06-30',
    health: 'on-track',
  },
  { id: 'quality', label: 'Quality', health: 'at-risk' },
];
const ITEMS: RoadmapItem[] = [
  {
    id: 'p1',
    label: 'Loyalty emails',
    groupId: 'growth',
    start: '2026-01-05',
    end: '2026-03-20',
    progress: 40,
    progressText: '8/20',
    health: 'on-track',
  },
  {
    id: 'p2',
    label: 'Referral page',
    groupId: 'growth',
    start: '2026-02-01',
    end: '2026-04-15',
    health: null,
  },
  {
    id: 'p3',
    label: 'Crash fixes',
    groupId: 'quality',
    start: '2026-01-10',
    end: '2026-02-10',
    health: 'off-track',
  },
  { id: 'p4', label: 'Billing revamp', start: null, end: null },
];

describe('Roadmap', () => {
  it('plots projects by initiative with progress, health and counts', async () => {
    const { container } = render(
      <Roadmap items={ITEMS} groups={GROUPS} today={TODAY} locale="en-GB" data-testid="r" />,
    );
    const root = screen.getByTestId('r');
    expect(root).toHaveClass('mn-roadmap');
    expect(root).toHaveClass('mn-gantt-timeline');
    expect(root).toHaveAttribute('data-zoom', 'quarter');
    expect(root).toHaveAttribute('data-readonly');
    expect(screen.getByRole('group', { name: 'Roadmap' })).toBeInTheDocument();
    expect(container.querySelector('.mn-gantt-timeline__corner')).toHaveTextContent('Project');
    expect(screen.getByText('2 projects')).toBeInTheDocument();
    expect(screen.getByText('1 project')).toBeInTheDocument();
    expect(screen.getByText('8/20')).toBeInTheDocument();
    expect(screen.getAllByText('On track')).toHaveLength(2);
    expect(screen.getByText('No health reported')).toBeInTheDocument();
    expect(
      screen.getByRole('button', {
        name: /^Loyalty emails, 5 Jan 2026 to 20 Mar 2026, 40% complete/,
      }),
    ).toHaveAttribute('data-tone', 'accent');
    expect(screen.getByRole('button', { name: /^Crash fixes/ })).toHaveAttribute(
      'data-tone',
      'danger',
    );
    // Undated projects stay in their row.
    expect(screen.getByText('No dates')).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('opens a project and is not editable by default', async () => {
    const onOpen = vi.fn();
    render(<Roadmap items={ITEMS} groups={GROUPS} today={TODAY} onOpen={onOpen} />);
    const bar = screen.getByRole('button', { name: /^Referral page/ });
    expect(bar).not.toHaveAttribute('data-editable');
    await userEvent.click(bar);
    expect(onOpen).toHaveBeenCalledWith(ITEMS[1]);
  });

  it('can be made editable and passes the roadmap item back', async () => {
    const onChange = vi.fn();
    render(<Roadmap items={ITEMS} today={TODAY} onChange={onChange} />);
    const bar = screen.getByRole('button', { name: /^Loyalty emails/ });
    bar.focus();
    await userEvent.keyboard('{ArrowRight}');
    expect(onChange).toHaveBeenCalledWith(ITEMS[0], { start: '2026-01-06', end: '2026-03-21' });
  });

  it('translates roadmap and timeline strings and offers the table view', async () => {
    render(
      <Roadmap
        items={ITEMS}
        groups={GROUPS}
        today={TODAY}
        messages={{ count: (n) => `${n} Projekte`, health: { 'at-risk': 'Gefährdet' } as never }}
        timelineMessages={{ label: 'Fahrplan', table: 'Tabelle' }}
      />,
    );
    expect(screen.getByText('2 Projekte')).toBeInTheDocument();
    expect(screen.getByText('Gefährdet')).toBeInTheDocument();
    await userEvent.click(screen.getByRole('radio', { name: 'Tabelle' }));
    const table = screen.getByRole('table', { name: 'Fahrplan' });
    expect(within(table).getByRole('rowheader', { name: 'Crash fixes' })).toBeInTheDocument();
  });

  it('renders on the server', () => {
    expect(renderToString(<Roadmap items={ITEMS} groups={GROUPS} today={TODAY} />)).toContain(
      'mn-roadmap',
    );
  });
});
