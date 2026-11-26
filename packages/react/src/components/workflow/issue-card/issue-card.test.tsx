import { describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { createRef } from 'react';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import type { StatusOption } from '../status-picker/index.js';
import { IssueCard, isIssueOverdue } from './index.js';

const STATUSES: StatusOption[] = [
  { id: 'todo', name: 'Todo', category: 'unstarted' },
  { id: 'doing', name: 'In progress', category: 'started' },
  { id: 'done', name: 'Done', category: 'completed' },
];
const BASE = {
  issueKey: 'STR-142',
  title: 'Export the roadmap as a PDF',
  status: STATUSES[1]!,
};

describe('isIssueOverdue', () => {
  it('compares calendar days', () => {
    expect(isIssueOverdue('2026-03-11', '2026-03-12')).toBe(true);
    expect(isIssueOverdue('2026-03-12', '2026-03-12')).toBe(false);
    expect(isIssueOverdue('not a date', '2026-03-12')).toBe(false);
  });
});

describe('IssueCard', () => {
  it('is an article named by its linked title, with key, status, priority, labels, due and assignee', async () => {
    const ref = createRef<HTMLElement>();
    render(
      <IssueCard
        ref={ref}
        {...BASE}
        href="/issues/STR-142"
        priority="high"
        labels={[
          { id: 'l1', name: 'Frontend' },
          { id: 'l2', name: 'Export' },
        ]}
        dueDate="2026-03-20"
        today="2026-03-12"
        locale="en-GB"
        assignee={{ name: 'Robin Diaz' }}
      />,
    );
    const card = screen.getByRole('article', { name: BASE.title });
    expect(card).toBe(ref.current);
    expect(card).toHaveClass('mn-issue-card');
    expect(screen.getByRole('link', { name: BASE.title })).toHaveAttribute(
      'href',
      '/issues/STR-142',
    );
    expect(screen.getByText('STR-142')).toBeInTheDocument();
    expect(screen.getByRole('img', { name: 'Priority: High' })).toBeInTheDocument();
    expect(screen.getByText('Status: In progress')).toBeInTheDocument();
    expect(
      within(screen.getByRole('list', { name: 'Labels' })).getAllByRole('listitem'),
    ).toHaveLength(2);
    expect(screen.getByText('Due 20 Mar').closest('time')).toHaveAttribute(
      'dateTime',
      '2026-03-20',
    );
    expect(card).not.toHaveAttribute('data-overdue');
    expect(screen.getByRole('img', { name: 'Assigned to Robin Diaz' })).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('marks overdue in words unless the issue is done', () => {
    const { container, rerender } = render(
      <IssueCard {...BASE} dueDate="2026-03-01" today="2026-03-12" />,
    );
    expect(container.firstElementChild).toHaveAttribute('data-overdue');
    expect(screen.getByText('Overdue')).toBeInTheDocument();
    rerender(<IssueCard {...BASE} status={STATUSES[2]} dueDate="2026-03-01" today="2026-03-12" />);
    expect(screen.queryByText('Overdue')).toBeNull();
  });

  it('works out today after hydration when not given', async () => {
    render(<IssueCard {...BASE} dueDate="2000-01-01" />);
    await waitFor(() => expect(screen.getByText('Overdue')).toBeInTheDocument());
  });

  it('opens with a button and changes status and priority in place', async () => {
    const user = userEvent.setup();
    const onOpen = vi.fn();
    const onStatusChange = vi.fn();
    const onPriorityChange = vi.fn();
    render(
      <IssueCard
        {...BASE}
        onOpen={onOpen}
        statuses={STATUSES}
        onStatusChange={onStatusChange}
        priority="low"
        onPriorityChange={onPriorityChange}
        assignee={null}
      />,
    );
    expect(screen.getByRole('img', { name: 'Unassigned' })).toBeInTheDocument();
    await user.tab();
    expect(screen.getByRole('button', { name: 'Priority: Low' })).toHaveFocus();
    await user.tab();
    expect(screen.getByRole('button', { name: BASE.title })).toHaveFocus();
    await user.keyboard('{Enter}');
    expect(onOpen).toHaveBeenCalled();
    await user.tab();
    expect(screen.getByRole('button', { name: 'Status: In progress' })).toHaveFocus();
    await user.keyboard('{Enter}');
    const menu = await screen.findByRole('menu');
    await user.click(within(menu).getByRole('menuitemradio', { name: 'Done' }));
    expect(onStatusChange).toHaveBeenCalledWith('done');
    await expectNoA11yViolations();
  });

  it('drops unsafe links, shows selected and dragging, and translates', () => {
    const { container } = render(
      <IssueCard
        {...BASE}
        href="javascript:alert(1)"
        selected
        dragging
        dueDate="2026-03-01"
        today="2026-03-12"
        messages={{ overdue: 'En retard', due: (d) => `Échéance ${d}` }}
      />,
    );
    expect(screen.queryByRole('link')).toBeNull();
    expect(container.firstElementChild).toHaveAttribute('data-selected');
    expect(container.firstElementChild).toHaveAttribute('data-dragging');
    expect(screen.getByText('En retard')).toBeInTheDocument();
    expect(screen.getByText(/Échéance/)).toBeInTheDocument();
  });

  it('renders a due time in UTC on the server, whatever zone the server is in', () => {
    // Just before and just after midnight UTC: any other zone moves one of them to another day.
    const late = renderToString(
      <IssueCard {...BASE} dueDate="2026-03-12T23:30:00Z" locale="en-GB" />,
    );
    const early = renderToString(
      <IssueCard {...BASE} dueDate="2026-03-12T00:30:00Z" locale="en-GB" />,
    );
    expect(late).toContain('Due 12 Mar');
    expect(early).toContain('Due 12 Mar');
  });

  it('renders on the server without guessing today', () => {
    const html = renderToString(<IssueCard {...BASE} dueDate="2000-01-01" />);
    expect(html).toContain('mn-issue-card');
    expect(html).not.toContain('Overdue');
  });
});
