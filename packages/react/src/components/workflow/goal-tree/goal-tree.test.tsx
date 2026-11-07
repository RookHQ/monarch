import { describe, expect, it, vi } from 'vitest';
import { useState } from 'react';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { GoalTree, type GoalTreeItem } from './index.js';

const GOALS: GoalTreeItem[] = [
  {
    id: 'o1',
    title: 'Grow repeat customers',
    progress: 62,
    status: 'on-track',
    owner: { name: 'Sam Lee' },
    meta: 'weight 40%',
    alignedTo: 'Company: Customers come back',
  },
  { id: 'k1', parentId: 'o1', title: 'Launch loyalty emails', progress: 100, status: 'done' },
  { id: 'k2', parentId: 'o1', title: 'Cut churn to 3%', progress: 30, status: 'at-risk' },
  { id: 'k3', parentId: 'o1', title: 'Survey 200 customers', progress: 50, status: 'on-track' },
  { id: 'o2', title: 'Ship the mobile app', progress: 10, status: 'off-track' },
];

describe('GoalTree', () => {
  it('shows goals with status, owner, alignment and progress', async () => {
    render(<GoalTree goals={GOALS} data-testid="t" />);
    expect(screen.getByTestId('t')).toHaveClass('mn-goal-tree');
    const bar = screen.getByRole('progressbar', { name: 'Progress of Grow repeat customers' });
    expect(bar).toHaveAttribute('aria-valuenow', '62');
    expect(bar).toHaveAttribute('aria-valuetext', '62%');
    const top = screen.getByTestId('t').querySelector('[data-depth="0"]')!;
    expect(top).toHaveTextContent('Grow repeat customersOn trackweight 40%');
    expect(top).toHaveTextContent('Sam Lee');
    expect(top).toHaveTextContent('Aligned to Company: Customers come back');
    // Quiet children: on-track key results have no visible badge but keep the status as text.
    const k3 = screen.getByText('Survey 200 customers').closest('li')!;
    expect(k3.querySelector('.mn-badge')).toBeNull();
    expect(k3).toHaveTextContent('On track');
    const k2 = screen.getByText('Cut churn to 3%').closest('li')!;
    expect(k2.querySelector('.mn-badge')).toHaveTextContent('At risk');
    await expectNoA11yViolations();
  });

  it('expands and collapses with an accessible disclosure button', async () => {
    render(<GoalTree goals={GOALS} />);
    const user = userEvent.setup();
    const toggle = screen.getByRole('button', { name: 'Collapse Grow repeat customers' });
    expect(toggle).toHaveAttribute('aria-expanded', 'true');
    expect(document.getElementById(toggle.getAttribute('aria-controls')!)).toBeInTheDocument();
    toggle.focus();
    await user.keyboard('{Enter}');
    expect(toggle).toHaveAttribute('aria-expanded', 'false');
    expect(toggle).toHaveAccessibleName('Expand Grow repeat customers');
    expect(screen.queryByText('Cut churn to 3%')).toBeNull();
    expect(screen.getByText('3 key results')).toBeInTheDocument();
    await user.keyboard(' ');
    expect(screen.getByText('Cut churn to 3%')).toBeInTheDocument();
  });

  it('supports controlled expansion', async () => {
    const onExpandedChange = vi.fn();
    function Controlled() {
      const [expanded, setExpanded] = useState<string[]>([]);
      return (
        <GoalTree
          goals={GOALS}
          expanded={expanded}
          onExpandedChange={(next) => {
            onExpandedChange(next);
            setExpanded(next);
          }}
        />
      );
    }
    render(<Controlled />);
    expect(screen.queryByText('Cut churn to 3%')).toBeNull();
    await userEvent.click(screen.getByRole('button', { name: 'Expand Grow repeat customers' }));
    expect(onExpandedChange).toHaveBeenCalledWith(['o1']);
    expect(screen.getByText('Cut churn to 3%')).toBeInTheDocument();
  });

  it('renders row actions and an add-child button', async () => {
    const onAddChild = vi.fn();
    render(
      <GoalTree
        goals={GOALS}
        defaultExpanded={['o1', 'o2']}
        onAddChild={onAddChild}
        renderActions={(g) => (
          <button type="button">Check in on {g.titleText ?? String(g.title)}</button>
        )}
      />,
    );
    expect(screen.getByRole('button', { name: 'Check in on Cut churn to 3%' })).toBeInTheDocument();
    const adds = screen.getAllByRole('button', { name: /^Add key result to/ });
    expect(adds).toHaveLength(2);
    await userEvent.click(
      screen.getByRole('button', { name: 'Add key result to Ship the mobile app' }),
    );
    expect(onAddChild).toHaveBeenCalledWith(GOALS[4]);
    await expectNoA11yViolations();
  });

  it('ignores parent cycles and missing parents', () => {
    render(
      <GoalTree
        goals={[
          { id: 'a', parentId: 'b', title: 'A', progress: 0, status: 'not-started' },
          { id: 'b', parentId: 'a', title: 'B', progress: 0, status: 'not-started' },
          { id: 'c', parentId: 'missing', title: 'C', progress: 0, status: 'not-started' },
        ]}
      />,
    );
    // C becomes top-level; A and B form a cycle with no root, so only C shows.
    expect(screen.getByText('C')).toBeInTheDocument();
  });

  it('shows the empty message, translations and renders on the server', () => {
    const { unmount } = render(<GoalTree goals={[]} emptyMessage="Set your first objective." />);
    expect(screen.getByText('Set your first objective.')).toBeInTheDocument();
    unmount();
    render(
      <GoalTree
        goals={GOALS.slice(4)}
        messages={{
          status: { 'off-track': 'Gefährdet' } as never,
          progress: (t) => `Fortschritt ${t}`,
        }}
      />,
    );
    expect(within(document.body).getByText('Gefährdet')).toBeInTheDocument();
    expect(
      screen.getByRole('progressbar', { name: 'Fortschritt Ship the mobile app' }),
    ).toBeInTheDocument();
    expect(renderToString(<GoalTree goals={GOALS} />)).toContain('mn-goal-tree');
  });
});
