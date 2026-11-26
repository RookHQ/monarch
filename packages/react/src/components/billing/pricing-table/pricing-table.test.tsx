import { describe, expect, it, vi } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import {
  BillingPeriodToggle,
  PricingTable,
  groupPricingFeatures,
  planPriceFor,
  type BillingPeriodOption,
  type PricingFeature,
  type PricingPlan,
} from './index.js';

const periods: BillingPeriodOption[] = [
  { value: 'monthly', label: 'Monthly', priceSuffix: '/ month' },
  { value: 'yearly', label: 'Yearly', priceSuffix: '/ year', priceNote: 'Billed yearly' },
];

const plans: PricingPlan[] = [
  { id: 'starter', name: 'Starter', prices: { monthly: '₹A', yearly: '₹B' } },
  {
    id: 'team',
    name: 'Team',
    prices: { monthly: '₹C', yearly: '₹D' },
    highlighted: true,
    badge: 'Recommended',
    action: <button type="button">Choose Team</button>,
  },
  { id: 'enterprise', name: 'Enterprise', price: 'Contact us' },
];

const features: PricingFeature[] = [
  { id: 'projects', label: 'Projects', values: { starter: '3', team: true, enterprise: true } },
  {
    id: 'sso',
    label: 'Single sign-on',
    description: 'SAML and OIDC',
    group: 'Security',
    values: { starter: false, team: true, enterprise: true },
  },
  { id: 'audit', label: 'Audit log', group: 'Security', values: { enterprise: true } },
];

describe('PricingTable', () => {
  it('is a captioned table with plan column headers and feature row headers', async () => {
    render(<PricingTable plans={plans} features={features} caption="Compare plans" />);
    const table = screen.getByRole('table', { name: 'Compare plans' });
    expect(within(table).getByRole('columnheader', { name: /Team/ })).toBeInTheDocument();
    expect(within(table).getByRole('rowheader', { name: /Single sign-on/ })).toBeInTheDocument();
    expect(screen.getByRole('region', { name: 'Compare plans' })).toHaveAttribute('tabindex', '0');
    await expectNoA11yViolations();
  });

  it('writes check and dash cells as text, and shows other values as given', () => {
    render(<PricingTable plans={plans} features={features} />);
    const sso = screen.getByRole('rowheader', { name: /Single sign-on/ }).closest('tr')!;
    const cells = within(sso).getAllByRole('cell');
    expect(cells[0]).toHaveTextContent('Not included');
    expect(cells[1]).toHaveTextContent('Included');
    const projects = screen.getByRole('rowheader', { name: 'Projects' }).closest('tr')!;
    expect(within(projects).getAllByRole('cell')[0]).toHaveTextContent('3');
    const audit = screen.getByRole('rowheader', { name: 'Audit log' }).closest('tr')!;
    expect(within(audit).getAllByRole('cell')[0]).toHaveTextContent('Not included');
  });

  it('groups rows under a group header', () => {
    render(<PricingTable plans={plans} features={features} />);
    expect(screen.getByRole('columnheader', { name: 'Security' })).toHaveAttribute('colspan', '4');
  });

  it('switches prices with the billing-period radio group (keyboard)', async () => {
    const onPeriodChange = vi.fn();
    render(
      <PricingTable
        plans={plans}
        features={features}
        periods={periods}
        onPeriodChange={onPeriodChange}
      />,
    );
    const group = screen.getByRole('radiogroup', { name: 'Billing period' });
    expect(within(group).getByRole('radio', { name: 'Monthly' })).toBeChecked();
    expect(screen.getByRole('columnheader', { name: /Starter/ })).toHaveTextContent('₹A / month');
    // Fallback prices do not get the period suffix.
    expect(screen.getByRole('columnheader', { name: /Enterprise/ })).toHaveTextContent(
      /^EnterpriseContact us$/,
    );
    await userEvent.tab();
    expect(within(group).getByRole('radio', { name: 'Monthly' })).toHaveFocus();
    await userEvent.keyboard('{ArrowRight}');
    expect(onPeriodChange).toHaveBeenCalledWith('yearly');
    expect(within(group).getByRole('radio', { name: 'Yearly' })).toBeChecked();
    const starter = screen.getByRole('columnheader', { name: /Starter/ });
    expect(starter).toHaveTextContent('₹B / year');
    expect(starter).toHaveTextContent('Billed yearly');
    expect(screen.getByRole('table').closest('.mn-pricing-table')).toHaveAttribute(
      'data-period',
      'yearly',
    );
  });

  it('follows a controlled period and can hide its toggle', () => {
    const { rerender } = render(
      <PricingTable
        plans={plans}
        features={features}
        periods={periods}
        period="yearly"
        showPeriodToggle={false}
      />,
    );
    expect(screen.queryByRole('radiogroup')).toBeNull();
    expect(screen.getByRole('columnheader', { name: /Team/ })).toHaveTextContent('₹D');
    rerender(
      <PricingTable
        plans={plans}
        features={features}
        periods={periods}
        period="monthly"
        showPeriodToggle={false}
      />,
    );
    expect(screen.getByRole('columnheader', { name: /Team/ })).toHaveTextContent('₹C');
  });

  it('marks highlighted and current plans with text, hides prices on request, translates', () => {
    render(
      <PricingTable
        plans={[{ ...plans[0]!, current: true }, plans[1]!]}
        features={features}
        periods={periods}
        showPrices={false}
        messages={{ currentPlan: 'Plan actual', billingPeriod: 'Periodo', caption: 'Planes' }}
      />,
    );
    expect(screen.getByRole('table', { name: 'Planes' })).toBeInTheDocument();
    expect(screen.getByRole('radiogroup', { name: 'Periodo' })).toBeInTheDocument();
    expect(screen.getByRole('columnheader', { name: /Starter/ })).toHaveTextContent('Plan actual');
    expect(screen.getByRole('columnheader', { name: /Team/ })).toHaveAttribute('data-highlighted');
    expect(screen.getByRole('columnheader', { name: /Team/ })).toHaveTextContent('Recommended');
    expect(screen.queryByText('₹A')).toBeNull();
  });

  it('BillingPeriodToggle is a named radio group', async () => {
    const onValueChange = vi.fn();
    render(<BillingPeriodToggle periods={periods} value="monthly" onValueChange={onValueChange} />);
    await userEvent.click(screen.getByRole('radio', { name: 'Yearly' }));
    expect(onValueChange).toHaveBeenCalledWith('yearly');
    await expectNoA11yViolations();
  });

  it('helpers resolve prices and group rows', () => {
    expect(planPriceFor(plans[0]!, 'yearly')).toBe('₹B');
    expect(planPriceFor(plans[2]!, 'yearly')).toBe('Contact us');
    expect(groupPricingFeatures(features).map((g) => g.group)).toEqual([undefined, 'Security']);
  });

  it('renders on the server', () => {
    const html = renderToString(
      <PricingTable plans={plans} features={features} periods={periods} defaultPeriod="yearly" />,
    );
    expect(html).toContain('mn-pricing-table');
    expect(html).toContain('₹B');
  });
});
