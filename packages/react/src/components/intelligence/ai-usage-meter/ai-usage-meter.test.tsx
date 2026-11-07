import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import { renderToString } from 'react-dom/server';
import { createRef } from 'react';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { AiUsageMeter, aiUsageState, type AiUsageScope } from './index.js';

const fmt = {
  formatNumber: (v: number) => String(v),
  formatPercent: (v: number) => `${Math.round(v * 100)}%`,
};

const scopes: AiUsageScope[] = [
  { label: 'This turn', used: 1200, unit: 'tokens' },
  { label: 'Conversation', used: 40, limit: 100, unit: 'credits' },
  { label: 'This month', used: 850, limit: 1000, unit: 'credits' },
];

describe('aiUsageState', () => {
  it('maps usage to ok, warning and exceeded', () => {
    expect(aiUsageState({ used: 10, limit: 100 })).toBe('ok');
    expect(aiUsageState({ used: 80, limit: 100 })).toBe('warning');
    expect(aiUsageState({ used: 100, limit: 100 })).toBe('exceeded');
    expect(aiUsageState({ used: 150, limit: 100 })).toBe('exceeded');
    expect(aiUsageState({ used: 5000 })).toBe('ok');
    expect(aiUsageState({ used: 50, limit: 100 }, 0.5)).toBe('warning');
  });
});

describe('AiUsageMeter', () => {
  it('renders a named group, forwards props and the ref', async () => {
    const ref = createRef<HTMLDivElement>();
    render(
      <AiUsageMeter ref={ref} scopes={scopes} data-testid="root" className="x" messages={fmt} />,
    );
    const root = screen.getByTestId('root');
    expect(root).toHaveClass('mn-ai-usage-meter', 'x');
    expect(ref.current).toBe(root);
    expect(screen.getByRole('group', { name: 'AI usage' })).toBe(root);
    await expectNoA11yViolations();
  });

  it('shows a meter per scope with a limit, and the amount only without one', () => {
    render(<AiUsageMeter scopes={scopes} messages={fmt} />);
    expect(screen.getAllByRole('meter')).toHaveLength(2);
    const conversation = screen.getByRole('meter', { name: 'Conversation' });
    expect(conversation).toHaveAttribute('aria-valuetext', '40 of 100 credits');
    expect(screen.queryByRole('meter', { name: 'This turn' })).toBeNull();
    expect(screen.getByText('1200 tokens')).toBeInTheDocument();
  });

  it('warns at the default 80% and announces the most serious state', () => {
    render(<AiUsageMeter scopes={scopes} data-testid="root" messages={fmt} />);
    const root = screen.getByTestId('root');
    expect(root).toHaveAttribute('data-state', 'warning');
    expect(screen.getAllByText('This month: 85% of the limit used')).toHaveLength(2);
    expect(screen.getByRole('status')).toHaveTextContent('This month: 85% of the limit used');
    expect(screen.getByRole('meter', { name: 'This month' })).toHaveAttribute(
      'data-tone',
      'warning',
    );
  });

  it('honours warnAt and reports an exceeded limit over a warning', () => {
    render(
      <AiUsageMeter
        warnAt={0.3}
        scopes={[
          { label: 'Conversation', used: 40, limit: 100, unit: 'credits' },
          { label: 'This month', used: 1000, limit: 1000, unit: 'tokens' },
        ]}
        data-testid="root"
        messages={fmt}
      />,
    );
    expect(screen.getByTestId('root')).toHaveAttribute('data-state', 'exceeded');
    expect(screen.getByText('Conversation: 40% of the limit used')).toBeInTheDocument();
    expect(screen.getByRole('status')).toHaveTextContent('This month: limit reached');
    expect(screen.getByRole('meter', { name: 'This month' })).toHaveAttribute(
      'data-tone',
      'danger',
    );
  });

  it('stays quiet when every scope is within its limit', () => {
    render(
      <AiUsageMeter
        scopes={[{ label: 'This turn', used: 10, limit: 100, unit: 'tokens' }]}
        data-testid="root"
      />,
    );
    expect(screen.getByTestId('root')).toHaveAttribute('data-state', 'ok');
    expect(screen.getByRole('status')).toBeEmptyDOMElement();
  });

  it('localises strings', () => {
    render(
      <AiUsageMeter
        scopes={[{ label: 'Mois', used: 1, limit: 2, unit: 'credits' }]}
        messages={{
          ...fmt,
          label: 'Utilisation',
          unit: () => 'crédits',
          amount: (u, l, unit) => `${u}/${l} ${unit}`,
        }}
      />,
    );
    expect(screen.getByRole('group', { name: 'Utilisation' })).toBeInTheDocument();
    expect(screen.getByRole('meter', { name: 'Mois' })).toHaveAttribute(
      'aria-valuetext',
      '1/2 crédits',
    );
  });

  it('renders on the server', () => {
    const html = renderToString(<AiUsageMeter scopes={scopes} />);
    expect(html).toContain('mn-ai-usage-meter');
    expect(html).toContain('role="meter"');
  });
});
