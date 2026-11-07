import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { defineStatuses } from '../../data-display/status-badge/index.js';
import { SavingsPanel, type SavingsPanelProps } from './index.js';

const statuses = defineStatuses({
  parked: { label: 'Parked', tone: 'neutral' },
  no_dns: { label: 'No DNS', tone: 'warning' },
  keep: { label: 'Keep', tone: 'success' },
  let_lapse: { label: 'Let lapse', tone: 'danger' },
});

const props: SavingsPanelProps = {
  unusedCount: 3,
  costs: [
    { amount: 4500, currency: 'USD' },
    { amount: 2000, currency: 'EUR' },
  ],
  unpricedCount: 1,
  categories: [
    { id: 'parked', count: 2, href: '/items?reason=parked' },
    { id: 'no_dns', count: 1 },
    { id: 'empty', count: 0 },
  ],
  decisions: [
    { id: 'keep', label: 'Keep', count: 1 },
    { id: 'undecided', label: 'Undecided', count: 2 },
  ],
  potentialSavings: [{ amount: 1500, currency: 'USD' }],
  items: [
    {
      id: 'a',
      name: 'example.com',
      href: '/items/a',
      category: 'parked',
      date: '2027-01-10T00:00:00Z',
      amount: { amount: 1500, currency: 'USD' },
      decision: 'let_lapse',
    },
    { id: 'b', name: 'example.net', category: 'no_dns', amount: null },
    { id: 'c', name: 'example.org', category: 'parked' },
  ],
  statuses,
  minorUnits: true,
  locale: 'en-US',
  timeZone: 'UTC',
};

describe('SavingsPanel', () => {
  it('renders a region named by its heading with the headline', async () => {
    render(<SavingsPanel {...props} />);
    const region = screen.getByRole('region', { name: 'Savings' });
    expect(region).toHaveTextContent(
      '3 unused items cost $45.00 + €20.00 to renew (1 without a price).',
    );
    await expectNoA11yViolations();
  });

  it('lists categories with counts, skipping empty ones, and links them', async () => {
    render(<SavingsPanel {...props} />);
    const list = screen.getByRole('list', { name: 'Unused by reason' });
    expect(list.querySelectorAll('li')).toHaveLength(2);
    const link = screen.getByRole('link', { name: 'Parked 2' });
    expect(link).toHaveAttribute('href', '/items?reason=parked');
    await userEvent.tab();
    expect(link).toHaveFocus();
  });

  it('summarises decisions and potential savings', () => {
    render(<SavingsPanel {...props} />);
    expect(screen.getByText(/Keep 1 · Undecided 2/)).toBeInTheDocument();
    expect(screen.getByText('Letting the marked items lapse saves $15.00.')).toBeInTheDocument();
  });

  it('lists items with date, price or "No price", and caps the list', () => {
    render(<SavingsPanel {...props} maxItems={2} />);
    const list = screen.getByRole('list', { name: 'Unused items' });
    const rows = list.querySelectorAll('li');
    expect(rows).toHaveLength(2);
    expect(rows[0]).toHaveTextContent('Jan 10, 2027');
    expect(rows[0]).toHaveTextContent('$15.00');
    expect(rows[0]).toHaveTextContent('Let lapse');
    expect(rows[1]).toHaveTextContent('No price');
    expect(screen.getByText('and 1 more')).toBeInTheDocument();
  });

  it('shows the empty, loading and error states', async () => {
    const onRetry = vi.fn();
    const { rerender } = render(<SavingsPanel unusedCount={0} />);
    expect(screen.getByText('Nothing unused')).toBeInTheDocument();
    rerender(<SavingsPanel unusedCount={0} loading />);
    expect(screen.getByRole('region')).toHaveAttribute('aria-busy', 'true');
    rerender(<SavingsPanel unusedCount={0} error onRetry={onRetry} />);
    await userEvent.click(screen.getByRole('button', { name: /try again/i }));
    expect(onRetry).toHaveBeenCalled();
    rerender(<SavingsPanel unusedCount={0} error="Offline" />);
    expect(screen.getByRole('alert')).toHaveTextContent('Offline');
  });

  it('takes a custom title, heading level and messages', () => {
    render(
      <SavingsPanel
        {...props}
        title="Unused spend"
        headingLevel="h3"
        messages={{ headline: (n) => `${n} to review` }}
      />,
    );
    expect(screen.getByRole('heading', { level: 3, name: 'Unused spend' })).toBeInTheDocument();
    expect(screen.getByText('3 to review')).toBeInTheDocument();
  });

  it('renders on the server', () => {
    const html = renderToString(<SavingsPanel {...props} />);
    expect(html).toContain('mn-savings-panel');
    expect(html).toContain('example.com');
  });
});
