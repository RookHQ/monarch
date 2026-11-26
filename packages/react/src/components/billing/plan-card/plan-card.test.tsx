import { createRef } from 'react';
import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { PlanCard } from './index.js';

describe('PlanCard', () => {
  it('is an article named by its plan heading, with price, period and features', async () => {
    render(
      <PlanCard
        name="Team"
        price="₹X"
        period="/ month"
        priceNote="Billed monthly"
        description="For small teams."
        featuresLabel="Everything in Starter, plus"
        features={['Shared projects', { label: 'Audit log', hint: 'last 90 days' }]}
        action={<button type="button">Choose Team</button>}
        data-testid="root"
      />,
    );
    const card = screen.getByRole('article', { name: 'Team' });
    expect(card).toHaveClass('mn-plan-card');
    expect(screen.getByRole('heading', { level: 3, name: 'Team' })).toBeInTheDocument();
    expect(card).toHaveTextContent('₹X / month');
    expect(card).toHaveTextContent('Billed monthly');
    expect(card).toHaveTextContent('Everything in Starter, plus');
    const items = screen.getAllByRole('listitem');
    expect(items).toHaveLength(2);
    expect(items[1]).toHaveTextContent('Audit log last 90 days');
    await expectNoA11yViolations();
  });

  it('reads excluded features as "Not included" and marks them', () => {
    render(
      <PlanCard
        name="Starter"
        features={[{ label: 'Projects' }, { id: 'sso', label: 'Single sign-on', included: false }]}
      />,
    );
    const [included, excluded] = screen.getAllByRole('listitem');
    expect(included).toHaveAttribute('data-included', 'true');
    expect(included).not.toHaveTextContent('Not included');
    expect(excluded).toHaveAttribute('data-included', 'false');
    expect(excluded).toHaveTextContent('Not included: Single sign-on');
  });

  it('shows highlight, badge and current-plan state with text', async () => {
    render(<PlanCard name="Business" highlighted badge="Recommended" current />);
    const card = screen.getByRole('article', { name: 'Business' });
    expect(card).toHaveAttribute('data-highlighted');
    expect(card).toHaveAttribute('data-current');
    expect(card).toHaveTextContent('Recommended');
    expect(card).toHaveTextContent('Current plan');
    await expectNoA11yViolations();
  });

  it('translates its text through messages and supports another heading level', () => {
    render(
      <PlanCard
        name="Plan"
        headingAs="h2"
        current
        features={[{ label: 'Export', included: false }]}
        messages={{ currentPlan: 'Plan actual', notIncluded: 'No incluido:' }}
      />,
    );
    expect(screen.getByRole('heading', { level: 2, name: 'Plan' })).toBeInTheDocument();
    expect(screen.getByText('Plan actual')).toBeInTheDocument();
    expect(screen.getByRole('listitem')).toHaveTextContent('No incluido: Export');
  });

  it('keeps the action keyboard reachable and forwards the ref', async () => {
    const ref = createRef<HTMLElement>();
    render(<PlanCard ref={ref} name="Team" action={<button type="button">Choose Team</button>} />);
    expect(ref.current?.tagName).toBe('ARTICLE');
    await userEvent.tab();
    expect(screen.getByRole('button', { name: 'Choose Team' })).toHaveFocus();
  });

  it('renders on the server', () => {
    const html = renderToString(<PlanCard name="Team" price="Contact us" features={['A']} />);
    expect(html).toContain('mn-plan-card');
    expect(html).toContain('Contact us');
  });
});
