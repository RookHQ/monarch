import { createRef } from 'react';
import { describe, expect, it, vi } from 'vitest';
import { readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { PricingSection, type PricingSectionPlan } from './index.js';

const periods = [
  { value: 'monthly', label: 'Monthly', priceSuffix: '/ month' },
  { value: 'yearly', label: 'Yearly', priceSuffix: '/ year', priceNote: 'Billed yearly' },
];

const plans: PricingSectionPlan[] = [
  {
    id: 'starter',
    name: 'Starter',
    prices: { monthly: '₹A', yearly: '₹B' },
    features: ['Projects', { label: 'Single sign-on', included: false }],
    action: <a href="#start">Start with Starter</a>,
  },
  {
    id: 'team',
    name: 'Team',
    prices: { monthly: '₹C', yearly: '₹D' },
    highlighted: true,
    badge: 'Recommended',
    features: ['Projects', 'Single sign-on'],
  },
  { id: 'enterprise', name: 'Enterprise', price: 'Contact us', features: ['Everything'] },
];

const comparison = [
  { id: 'sso', label: 'Single sign-on', values: { starter: false, team: true, enterprise: true } },
];

describe('PricingSection', () => {
  it('forwards its ref to the section', () => {
    const ref = createRef<HTMLElement>();
    render(<PricingSection ref={ref} title="Pricing" plans={plans} />);
    expect(ref.current).toHaveClass('mn-pricing-section');
  });

  it('is a region named by its heading with one plan card per plan', async () => {
    render(
      <PricingSection
        eyebrow="Pricing"
        title="Plans for every team"
        description="Pick a plan."
        plans={plans}
        periods={periods}
        note="Taxes extra."
      />,
    );
    const region = screen.getByRole('region', { name: 'Plans for every team' });
    expect(region).toHaveClass('mn-msection', 'mn-pricing-section');
    expect(screen.getByRole('heading', { level: 2, name: 'Plans for every team' })).toBeVisible();
    const cards = within(region).getAllByRole('article');
    expect(cards.map((c) => c.getAttribute('aria-labelledby') !== null)).toEqual([
      true,
      true,
      true,
    ]);
    expect(screen.getByRole('heading', { level: 3, name: 'Team' })).toBeInTheDocument();
    expect(screen.getByRole('article', { name: 'Starter' })).toHaveTextContent('₹A / month');
    expect(screen.getByRole('article', { name: 'Enterprise' })).toHaveTextContent('Contact us');
    expect(screen.getByRole('article', { name: 'Starter' })).toHaveTextContent(
      'Not included: Single sign-on',
    );
    expect(region).toHaveTextContent('Taxes extra.');
    await expectNoA11yViolations();
  });

  it('one toggle switches card prices and the comparison table (keyboard)', async () => {
    const onPeriodChange = vi.fn();
    render(
      <PricingSection
        title="Pricing"
        plans={plans}
        periods={periods}
        comparison={comparison}
        onPeriodChange={onPeriodChange}
      />,
    );
    expect(screen.getAllByRole('radiogroup')).toHaveLength(1);
    const monthly = screen.getByRole('radio', { name: 'Monthly' });
    monthly.focus();
    await userEvent.keyboard('{ArrowRight}');
    expect(onPeriodChange).toHaveBeenCalledWith('yearly');
    expect(screen.getByRole('article', { name: 'Starter' })).toHaveTextContent('₹B / year');
    expect(screen.getByRole('article', { name: 'Starter' })).toHaveTextContent('Billed yearly');
    const table = screen.getByRole('table', { name: 'Compare plans' });
    expect(within(table).getByRole('columnheader', { name: /Team/ })).toHaveTextContent('₹D');
    expect(screen.getByRole('heading', { level: 3, name: 'Compare plans' })).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('has no toggle with a single period and follows a controlled period', () => {
    render(
      <PricingSection
        title="Pricing"
        plans={plans}
        periods={periods}
        period="yearly"
        headingAs="h3"
      />,
    );
    expect(screen.getByRole('article', { name: 'Team' })).toHaveTextContent('₹D');
    expect(screen.getByRole('heading', { level: 4, name: 'Team' })).toBeInTheDocument();
    render(<PricingSection title="Only one" plans={plans} periods={[periods[0]!]} />);
    expect(screen.getAllByRole('radiogroup')).toHaveLength(1);
  });

  it('translates its text through messages', () => {
    render(
      <PricingSection
        title="Precios"
        plans={plans}
        periods={periods}
        comparison={comparison}
        messages={{
          billingPeriod: 'Periodo',
          comparisonTitle: 'Comparar planes',
          planNotIncluded: 'No incluido:',
          notIncluded: 'No incluido',
        }}
      />,
    );
    expect(screen.getByRole('radiogroup', { name: 'Periodo' })).toBeInTheDocument();
    expect(screen.getByRole('table', { name: 'Comparar planes' })).toBeInTheDocument();
    expect(screen.getByRole('article', { name: 'Starter' })).toHaveTextContent(
      'No incluido: Single sign-on',
    );
  });

  it('renders on the server', () => {
    const html = renderToString(
      <PricingSection title="Pricing" plans={plans} periods={periods} comparison={comparison} />,
    );
    expect(html).toContain('mn-pricing-section');
    expect(html).toContain('₹A');
  });

  it('lays out its plan columns by its own width, not the window', () => {
    // In a 620px docs column on a wide window, window breakpoints gave three cramped
    // columns, and the Enterprise price broke mid-word ("Conta ct us").
    const css = readFileSync(join(dirname(__filename), 'pricing-section.css'), 'utf8');
    expect(css).toMatch(/\.mn-pricing-section \{[^}]*container: mn-pricing-section \/ inline-size/);
    expect(css).not.toMatch(/@media \(min-width/);
    const card = readFileSync(
      join(dirname(__filename), '../../../components/billing/plan-card/plan-card.css'),
      'utf8',
    );
    expect(card).toMatch(/\.mn-plan-card__price \{[^}]*overflow-wrap: break-word/);
  });
});
