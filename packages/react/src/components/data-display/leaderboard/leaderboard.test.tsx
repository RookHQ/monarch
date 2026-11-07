import { describe, expect, it, vi } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { createRef } from 'react';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { Leaderboard, rankEntries, type LeaderboardEntry } from './index.js';

const entries: LeaderboardEntry[] = [
  { id: 'b', name: 'Bea Lund', value: 31, change: { value: -0.05, locale: 'en' } },
  { id: 'a', name: 'Ada Moss', value: 42, href: '/people/a', secondary: 'Support' },
  { id: 'c', name: 'Cal Ortiz', value: 31, current: true },
  { id: 'd', name: 'Dee Park', value: 12 },
];

describe('Leaderboard', () => {
  it('renders a named ordered list with ranks, values and spoken units; forwards props and ref', async () => {
    const ref = createRef<HTMLDivElement>();
    render(
      <Leaderboard
        ref={ref}
        data-testid="root"
        className="extra"
        label="Tickets solved"
        valueLabel="tickets"
        sort="desc"
        locale="en"
        entries={entries}
      />,
    );
    const root = screen.getByTestId('root');
    expect(root).toHaveClass('mn-leaderboard', 'extra');
    expect(ref.current).toBe(root);
    const rows = within(screen.getByRole('list', { name: 'Tickets solved' })).getAllByRole(
      'listitem',
    );
    expect(rows.map((r) => r.getAttribute('data-rank'))).toEqual(['1', '2', '2', '4']);
    expect(rows[0]).toHaveTextContent('Rank 1:');
    expect(rows[0]).toHaveTextContent('42 tickets');
    expect(screen.getByRole('link', { name: 'Ada Moss' })).toHaveAttribute('href', '/people/a');
    await expectNoA11yViolations();
  });

  it('tags the viewer and marks the row current', () => {
    render(<Leaderboard entries={entries} />);
    const row = screen.getByText('Cal Ortiz').closest('li')!;
    expect(row).toHaveAttribute('aria-current', 'true');
    expect(within(row).getByText('(you)')).toBeInTheDocument();
  });

  it('shows changes through DeltaIndicator and bars relative to the leader', () => {
    const { container } = render(<Leaderboard entries={entries} sort="desc" />);
    expect(container).toHaveTextContent(/down 5%/);
    const fills = container.querySelectorAll<HTMLElement>('.mn-leaderboard__bar-fill');
    expect(fills[0]!.style.getPropertyValue('--mn-leaderboard-share')).toBe('1');
    expect(container.querySelector('.mn-leaderboard__bar')).toHaveAttribute('aria-hidden', 'true');
  });

  it('ranks ascending, keeps the given order with none, and shares ranks on ties', () => {
    expect(rankEntries(entries, 'asc').map((r) => [r.entry.id, r.rank])).toEqual([
      ['d', 1],
      ['b', 2],
      ['c', 2],
      ['a', 4],
    ]);
    expect(rankEntries(entries).map((r) => r.entry.id)).toEqual(['b', 'a', 'c', 'd']);
  });

  it('limits rows and links to the rest', () => {
    render(<Leaderboard entries={entries} limit={2} total={9} moreHref="/reports/agents" />);
    expect(screen.getAllByRole('listitem')).toHaveLength(2);
    expect(screen.getByRole('link', { name: '7 more' })).toHaveAttribute('href', '/reports/agents');
  });

  it('formats percent values and uses display text when given', () => {
    render(
      <Leaderboard
        locale="en"
        format="percent"
        entries={[
          { id: 'x', name: 'Step one', value: 0.38 },
          { id: 'y', name: 'Step two', value: 0.2, display: '20 of 100' },
        ]}
        showAvatars={false}
        showBars={false}
      />,
    );
    expect(screen.getByText('38%')).toBeInTheDocument();
    expect(screen.getByText('20 of 100')).toBeInTheDocument();
  });

  it('shows loading, error and empty states', async () => {
    const onRetry = vi.fn();
    const user = userEvent.setup();
    const { rerender } = render(<Leaderboard data-testid="root" entries={[]} loading />);
    expect(screen.getByRole('img', { name: 'Loading ranking' })).toBeInTheDocument();
    expect(screen.getByTestId('root')).toHaveAttribute('aria-busy', 'true');
    rerender(<Leaderboard entries={[]} error="Ranking did not load." onRetry={onRetry} />);
    await user.click(screen.getByRole('button', { name: 'Try again' }));
    expect(onRetry).toHaveBeenCalled();
    rerender(<Leaderboard entries={[]} />);
    expect(screen.getByText('Nothing to rank yet')).toBeInTheDocument();
  });

  it('renders on the server', () => {
    const html = renderToString(<Leaderboard entries={entries} locale="en" />);
    expect(html).toContain('mn-leaderboard');
    expect(html).toContain('Ada Moss');
  });
});
