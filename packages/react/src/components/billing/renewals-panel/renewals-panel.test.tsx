import { describe, expect, it, vi } from 'vitest';
import { useState } from 'react';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { RenewalsPanel, type RenewalItem } from './index.js';

const NOW = '2026-11-01T00:00:00Z';

const items: RenewalItem[] = [
  {
    id: 'r1',
    name: 'Example agreement',
    href: '/renewals/r1',
    description: 'Contract · Example Co',
    renewalDate: '2026-11-08T00:00:00Z',
    amount: { amount: 120000, currency: 'USD' },
    amountNote: 'per year',
    status: 'open',
    probability: 60,
    owner: 'Sam Lee',
    nextAction: 'Send a quote',
    nextActionDate: '2026-11-03T00:00:00Z',
    nextActionOverdue: true,
  },
  {
    id: 'r2',
    name: 'example.org',
    renewalDate: '2027-02-01T00:00:00Z',
    noticeDeadline: '2027-01-01T00:00:00Z',
    amount: null,
    autoRenew: true,
    status: 'in_progress',
  },
  {
    id: 'r3',
    name: 'Old licence',
    renewalDate: '2026-10-01T00:00:00Z',
    daysUntil: -31,
    status: 'renewed',
    outcome: 'Renewed for two years',
  },
];

const base = { minorUnits: true, locale: 'en-US', timeZone: 'UTC', now: NOW } as const;

function Filtered() {
  const [filter, setFilter] = useState('open');
  return (
    <RenewalsPanel
      {...base}
      title="Renewals"
      items={filter === 'open' ? items.slice(0, 2) : items}
      filters={[
        { value: 'open', label: 'Open' },
        { value: 'all', label: 'All' },
      ]}
      filter={filter}
      onFilterChange={setFilter}
    />
  );
}

describe('RenewalsPanel', () => {
  it('renders a labelled region with a table of renewals', async () => {
    render(<RenewalsPanel {...base} title="Renewals" items={items} />);
    expect(screen.getByRole('region', { name: 'Renewals' })).toBeInTheDocument();
    const table = screen.getByRole('table', { name: 'Renewals' });
    expect(within(table).getAllByRole('row')).toHaveLength(4);
    await expectNoA11yViolations();
  });

  it('shows dates, day counts, notice deadlines and urgency', () => {
    render(<RenewalsPanel {...base} title="Renewals" items={items} />);
    const [, first, second, third] = screen.getAllByRole('row');
    expect(first).toHaveTextContent('Due soon: Nov 8, 2026');
    expect(first).toHaveTextContent('in 7 days');
    expect(first).toHaveAttribute('data-urgent');
    expect(second).toHaveTextContent('Notice by Jan 1, 2027');
    expect(second).not.toHaveAttribute('data-urgent');
    expect(second).toHaveTextContent('Auto-renews');
    expect(third).toHaveTextContent('31 days ago');
    expect(third).not.toHaveAttribute('data-urgent');
  });

  it('shows amounts, owner, overdue next action, probability and outcome', () => {
    render(<RenewalsPanel {...base} title="Renewals" items={items} />);
    const [, first, second, third] = screen.getAllByRole('row');
    expect(first).toHaveTextContent('$1,200.00');
    expect(first).toHaveTextContent('Overdue: Send a quote · Nov 3, 2026');
    expect(first).toHaveTextContent('60% likely');
    expect(second).toHaveTextContent('No price');
    expect(second).toHaveTextContent('No owner');
    expect(third).toHaveTextContent('Renewed for two years');
  });

  it('runs the row action with a named button', async () => {
    const onItemAction = vi.fn();
    render(
      <RenewalsPanel
        {...base}
        title="Renewals"
        items={items}
        onItemAction={onItemAction}
        actionLabel="Mark renewed"
        canAct={(i) => i.status !== 'renewed'}
      />,
    );
    expect(screen.getAllByRole('button', { name: /^Mark renewed/ })).toHaveLength(2);
    const button = screen.getByRole('button', { name: 'Mark renewed Example agreement' });
    button.focus();
    await userEvent.keyboard('{Enter}');
    expect(onItemAction).toHaveBeenCalledWith(items[0]);
  });

  it('switches filters with the keyboard', async () => {
    render(<Filtered />);
    expect(screen.getAllByRole('row')).toHaveLength(3);
    const open = screen.getByRole('radio', { name: 'Open' });
    open.focus();
    await userEvent.keyboard('{ArrowRight}');
    expect(screen.getByRole('radio', { name: 'All' })).toBeChecked();
    expect(screen.getAllByRole('row')).toHaveLength(4);
  });

  it('shows per-currency totals and the unpriced count', () => {
    render(
      <RenewalsPanel
        {...base}
        title="Renewals"
        items={items}
        totals={[{ currency: 'USD', amount: 120000, count: 1, estimated: 2000 }]}
        unpricedCount={1}
      />,
    );
    const list = screen.getByRole('list', { name: 'Totals by currency' });
    expect(list).toHaveTextContent('USD · 1 renewal');
    expect(list).toHaveTextContent('$1,200.00');
    expect(list).toHaveTextContent('Includes $20.00 estimated');
    expect(list).toHaveTextContent('Without a price1');
  });

  it('shows loading, error and empty states', async () => {
    const onRetry = vi.fn();
    const { rerender } = render(<RenewalsPanel title="Renewals" items={[]} loading />);
    expect(screen.getByRole('region')).toHaveAttribute('aria-busy', 'true');
    expect(screen.getByRole('status', { name: 'Loading renewals' })).toBeInTheDocument();
    rerender(<RenewalsPanel title="Renewals" items={[]} error onRetry={onRetry} />);
    expect(screen.getByText('Renewals could not be loaded.')).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: /try again/i }));
    expect(onRetry).toHaveBeenCalled();
    rerender(<RenewalsPanel title="Renewals" items={[]} />);
    expect(screen.getByText('No renewals coming up')).toBeInTheDocument();
  });

  it('renders on the server', () => {
    const html = renderToString(<RenewalsPanel {...base} title="Renewals" items={items} />);
    expect(html).toContain('mn-renewals-panel');
    expect(html).toContain('Example agreement');
  });
});
