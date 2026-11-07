import { describe, expect, it } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { SubscriptionsTable, formatBillingInterval, type SubscriptionRow } from './index.js';

const rows: SubscriptionRow[] = [
  {
    id: 's1',
    name: 'Support plan',
    href: '/subscriptions/s1',
    plan: 'Standard',
    quantity: 3,
    customer: 'Example Co',
    status: 'active',
    amount: 12000,
    currency: 'USD',
    interval: 'month',
    monthly: { amount: 12000, currency: 'USD' },
    renewalDate: '2027-01-15T00:00:00Z',
    pendingChangeAt: '2026-12-01T00:00:00Z',
  },
  {
    id: 's2',
    name: 'Hosting',
    customer: 'Sample Ltd',
    status: 'past_due',
    amount: 90000,
    currency: 'EUR',
    interval: 'year',
    monthly: null,
    renewalDate: '2027-03-01T00:00:00Z',
    autoRenew: false,
  },
  {
    id: 's3',
    name: 'Setup',
    status: 'cancelled',
    amount: 5000,
    currency: 'USD',
    interval: null,
  },
];

const props = { minorUnits: true, locale: 'en-US', timeZone: 'UTC' } as const;

describe('SubscriptionsTable', () => {
  it('renders a named table with one row per subscription', async () => {
    render(<SubscriptionsTable subscriptions={rows} {...props} caption="Customer subscriptions" />);
    const table = screen.getByRole('table', { name: 'Customer subscriptions' });
    expect(within(table).getAllByRole('row')).toHaveLength(4);
    expect(screen.getByRole('columnheader', { name: 'Monthly' })).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('formats prices per currency and billing period', () => {
    render(<SubscriptionsTable subscriptions={rows} {...props} />);
    const [, first, second, third] = screen.getAllByRole('row');
    expect(first).toHaveTextContent('$120.00 / month');
    expect(first).toHaveTextContent('3 × Standard · changes Dec 1, 2026');
    expect(first).toHaveTextContent('Renews Jan 15, 2027');
    expect(second).toHaveTextContent('€900.00 / year');
    expect(second).toHaveTextContent('Ends Mar 1, 2027');
    expect(second).toHaveTextContent('Not converted');
    expect(third).toHaveTextContent('/ one time');
    expect(third).toHaveTextContent('Custom price');
    expect(third).toHaveTextContent('Evergreen');
  });

  it('shows statuses as badges with readable labels', () => {
    render(<SubscriptionsTable subscriptions={rows} {...props} />);
    expect(screen.getByText('Active')).toBeInTheDocument();
    expect(screen.getByText('Past due')).toBeInTheDocument();
    expect(screen.getByText('Cancelled')).toBeInTheDocument();
  });

  it('links names and lets the product render its own link', async () => {
    const { rerender } = render(<SubscriptionsTable subscriptions={rows} {...props} />);
    const link = screen.getByRole('link', { name: 'Support plan' });
    expect(link).toHaveAttribute('href', '/subscriptions/s1');
    await userEvent.tab();
    expect(link).toHaveFocus();
    rerender(
      <SubscriptionsTable
        subscriptions={rows}
        {...props}
        renderLink={({ href, className, children }) => (
          <a href={`/app${href}`} className={className} data-router="">
            {children}
          </a>
        )}
      />,
    );
    expect(screen.getByRole('link', { name: 'Support plan' })).toHaveAttribute(
      'href',
      '/app/subscriptions/s1',
    );
  });

  it('hides the customer and monthly columns when asked', () => {
    render(
      <SubscriptionsTable
        subscriptions={rows}
        {...props}
        showCustomer={false}
        showMonthly={false}
      />,
    );
    expect(screen.queryByRole('columnheader', { name: 'Customer' })).toBeNull();
    expect(screen.queryByRole('columnheader', { name: 'Monthly' })).toBeNull();
  });

  it('shows an empty state and uses message overrides', () => {
    render(
      <SubscriptionsTable subscriptions={[]} messages={{ emptyTitle: 'Nothing recurring yet' }} />,
    );
    expect(screen.getByText('Nothing recurring yet')).toBeInTheDocument();
    expect(screen.queryByRole('table')).toBeNull();
  });

  it('writes billing intervals', () => {
    expect(formatBillingInterval('month', 1)).toBe('month');
    expect(formatBillingInterval('month', 3)).toBe('quarter');
    expect(formatBillingInterval('week', 2)).toBe('2 weeks');
    expect(formatBillingInterval(null)).toBe('one time');
  });

  it('renders on the server', () => {
    const html = renderToString(<SubscriptionsTable subscriptions={rows} {...props} />);
    expect(html).toContain('mn-subscriptions-table');
    expect(html).toContain('Support plan');
  });
});
