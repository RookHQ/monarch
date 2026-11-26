import { describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { useState } from 'react';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { ApprovalQueue, type ApprovalQueueItem, type ApprovalQueueProps } from './index.js';

const NOW = '2026-03-10T12:00:00Z';

const ITEMS: ApprovalQueueItem[] = [
  {
    id: '1',
    requester: { name: 'Alex Morgan', detail: 'Design' },
    title: 'Expense claim',
    summary: '3 receipts',
    submittedAt: '2026-03-10T09:00:00Z',
    overdue: true,
  },
  { id: '2', requester: { name: 'Sam Lee' }, title: 'Leave, 2 days' },
  { id: '3', requester: { name: 'Jo Park' }, title: 'Timesheet', canDecide: false },
];

function Harness(props: Partial<ApprovalQueueProps>) {
  const [items, setItems] = useState(ITEMS);
  const drop = (ids: string[]) => setItems((cur) => cur.filter((i) => !ids.includes(i.id)));
  return (
    <ApprovalQueue
      now={NOW}
      items={items}
      onApprove={(it) => drop([it.id])}
      onReject={(it) => drop([it.id])}
      {...props}
    />
  );
}

describe('ApprovalQueue', () => {
  it('lists requests with their details and named actions, without axe violations', async () => {
    render(<Harness onReview={() => {}} />);
    const list = screen.getByRole('list', { name: 'Requests waiting for you' });
    const rows = within(list).getAllByRole('listitem');
    expect(rows).toHaveLength(3);
    expect(rows[0]).toHaveTextContent('Alex Morgan');
    expect(rows[0]).toHaveTextContent('Design');
    expect(rows[0]).toHaveTextContent('3 hours ago');
    expect(rows[0]).toHaveTextContent('Past due');
    expect(rows[0]).toHaveAttribute('data-overdue');
    expect(screen.getByRole('button', { name: 'Approve request from Alex Morgan' })).toBeVisible();
    expect(screen.getByRole('button', { name: 'Review request from Jo Park' })).toBeVisible();
    // Jo Park cannot be decided by the viewer.
    expect(screen.queryByRole('button', { name: 'Approve request from Jo Park' })).toBeNull();
    await expectNoA11yViolations();
  });

  it('approves one request, shows a pending state and announces it', async () => {
    const user = userEvent.setup();
    let resolve: () => void = () => {};
    const onApprove = vi.fn(() => new Promise<void>((r) => (resolve = r)));
    render(<ApprovalQueue items={ITEMS} onApprove={onApprove} now={NOW} />);
    const button = screen.getByRole('button', { name: 'Approve request from Sam Lee' });
    await user.click(button);
    expect(onApprove).toHaveBeenCalledWith(ITEMS[1]);
    expect(button).toHaveAttribute('aria-busy', 'true');
    resolve();
    await waitFor(() =>
      expect(screen.getByRole('status')).toHaveTextContent('Approved the request from Sam Lee.'),
    );
  });

  it('shows an error on the row when approval fails', async () => {
    const user = userEvent.setup();
    render(
      <ApprovalQueue items={ITEMS} onApprove={() => Promise.reject(new Error('x'))} now={NOW} />,
    );
    await user.click(screen.getByRole('button', { name: 'Approve request from Alex Morgan' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('That did not work. Try again.');
  });

  it('sends back with a required reason and returns focus', async () => {
    const user = userEvent.setup();
    const onReject = vi.fn();
    render(<ApprovalQueue items={ITEMS} onReject={onReject} now={NOW} />);
    const trigger = screen.getByRole('button', { name: 'Send back request from Alex Morgan' });
    await user.click(trigger);
    const dialog = await screen.findByRole('alertdialog', {
      name: 'Send back the request from Alex Morgan?',
    });
    const field = within(dialog).getByRole('textbox', { name: /Reason/ });
    expect(field).toHaveFocus();
    const confirm = within(dialog).getByRole('button', { name: 'Send back' });
    expect(confirm).toBeDisabled();
    await user.type(field, 'Attach the invoice');
    await user.click(confirm);
    expect(onReject).toHaveBeenCalledWith(ITEMS[0], 'Attach the invoice');
    await waitFor(() => expect(screen.queryByRole('alertdialog')).toBeNull());
    await waitFor(() => expect(trigger).toHaveFocus());
  });

  it('keeps focus in the queue when the decided row disappears', async () => {
    const user = userEvent.setup();
    render(<Harness data-testid="queue" />);
    await user.click(screen.getByRole('button', { name: 'Approve request from Sam Lee' }));
    await waitFor(() => expect(screen.getByTestId('queue')).toHaveFocus());
    await user.click(screen.getByRole('button', { name: 'Send back request from Alex Morgan' }));
    const dialog = await screen.findByRole('alertdialog');
    await user.type(within(dialog).getByRole('textbox', { name: /Reason/ }), 'Missing receipt');
    await user.click(within(dialog).getByRole('button', { name: 'Send back' }));
    await waitFor(() => expect(screen.queryByRole('alertdialog')).toBeNull());
    await waitFor(() => expect(screen.getByTestId('queue')).toHaveFocus());
    expect(screen.getAllByRole('listitem')).toHaveLength(1);
  });

  it('sends back without a reason when rejectReason is none', async () => {
    const user = userEvent.setup();
    const onReject = vi.fn();
    render(<ApprovalQueue items={ITEMS} onReject={onReject} rejectReason="none" now={NOW} />);
    await user.click(screen.getByRole('button', { name: 'Send back request from Sam Lee' }));
    const dialog = await screen.findByRole('alertdialog');
    expect(within(dialog).queryByRole('textbox')).toBeNull();
    await user.click(within(dialog).getByRole('button', { name: 'Send back' }));
    expect(onReject).toHaveBeenCalledWith(ITEMS[1], undefined);
  });

  it('selects requests with the keyboard and approves them in bulk', async () => {
    const user = userEvent.setup();
    const onApproveMany = vi.fn();
    render(<Harness onApproveMany={onApproveMany} />);
    const selectAll = screen.getByRole('checkbox', { name: 'Select all' });
    selectAll.focus();
    await user.keyboard(' ');
    expect(screen.getByText('2 selected')).toBeInTheDocument();
    expect(screen.getByRole('checkbox', { name: 'Select request from Jo Park' })).toBeDisabled();
    await user.click(screen.getByRole('checkbox', { name: 'Select request from Sam Lee' }));
    expect(selectAll).toHaveAttribute('aria-checked', 'mixed');
    await user.click(screen.getByRole('button', { name: 'Approve selected' }));
    const dialog = await screen.findByRole('alertdialog', { name: 'Approve 1 request?' });
    await user.click(within(dialog).getByRole('button', { name: 'Approve all' }));
    expect(onApproveMany).toHaveBeenCalledWith([ITEMS[0]]);
    await waitFor(() =>
      expect(screen.getByRole('status')).toHaveTextContent('Approved 1 request.'),
    );
  });

  it('filters by search and clears the filter', async () => {
    const user = userEvent.setup();
    render(<Harness searchable />);
    const search = screen.getByRole('searchbox', { name: 'Search requests' });
    await user.type(search, 'sam');
    expect(screen.getAllByRole('listitem')).toHaveLength(1);
    await user.clear(search);
    await user.type(search, 'nobody');
    expect(screen.getByText('No requests match your search')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Clear filters' }));
    expect(screen.getAllByRole('listitem')).toHaveLength(3);
  });

  it('limits rows, shows a footer, and an empty state', () => {
    const { rerender } = render(
      <ApprovalQueue items={ITEMS} limit={1} footer={<a href="#all">See all 3</a>} now={NOW} />,
    );
    expect(screen.getAllByRole('listitem')).toHaveLength(1);
    expect(screen.getByRole('link', { name: 'See all 3' })).toBeInTheDocument();
    rerender(<ApprovalQueue items={[]} now={NOW} />);
    expect(screen.getByText('Nothing to review')).toBeInTheDocument();
  });

  it('renders on the server', () => {
    const html = renderToString(<ApprovalQueue items={ITEMS} now={NOW} onApprove={() => {}} />);
    expect(html).toContain('mn-approval-queue');
    expect(html).toContain('Alex Morgan');
  });
});
