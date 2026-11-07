import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { createRef } from 'react';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { AiConfidenceMeter, aiConfidenceLevel, type AiConfidenceLevel } from './index.js';

const pct = { formatValue: (v: number) => `${Math.round(v * 100)}%` };

describe('aiConfidenceLevel', () => {
  it.each<[number, AiConfidenceLevel]>([
    [0, 'low'],
    [0.59, 'low'],
    [0.6, 'medium'],
    [0.84, 'medium'],
    [0.85, 'high'],
    [1, 'high'],
    [1.5, 'high'],
    [-1, 'low'],
    [Number.NaN, 'low'],
  ])('maps %s to %s', (value, level) => {
    expect(aiConfidenceLevel(value)).toBe(level);
  });

  it('accepts partial thresholds', () => {
    expect(aiConfidenceLevel(0.5, { low: 0.4 })).toBe('medium');
    expect(aiConfidenceLevel(0.7, { high: 0.7 })).toBe('high');
  });
});

describe('AiConfidenceMeter', () => {
  it('renders a badge, forwards props and the ref', async () => {
    const ref = createRef<HTMLDivElement>();
    render(
      <AiConfidenceMeter
        ref={ref}
        value={0.42}
        data-testid="root"
        className="extra"
        messages={pct}
      />,
    );
    const root = screen.getByTestId('root');
    expect(root).toHaveClass('mn-ai-confidence-meter', 'extra');
    expect(root).toHaveAttribute('data-variant', 'badge');
    expect(root).toHaveAttribute('data-level', 'low');
    expect(ref.current).toBe(root);
    expect(root).not.toHaveAttribute('tabindex');
    await expectNoA11yViolations();
  });

  it.each<[number, AiConfidenceLevel, string]>([
    [0.3, 'low', 'Needs check'],
    [0.7, 'medium', 'Medium confidence'],
    [0.9, 'high', 'High confidence'],
  ])('shows %s as %s with text and an icon', (value, level, label) => {
    render(<AiConfidenceMeter value={value} data-testid="root" messages={pct} />);
    const root = screen.getByTestId('root');
    expect(root).toHaveAttribute('data-level', level);
    expect(root.querySelector('.mn-ai-confidence-meter__label')).toHaveTextContent(label);
    expect(root.querySelector('.mn-ai-confidence-meter__icon svg')).not.toBeNull();
    expect(root.querySelector('.mn-visually-hidden')).toHaveTextContent(
      `${label}, ${Math.round(value * 100)}% confidence`,
    );
  });

  it('shows the value on request and honours thresholds', () => {
    render(
      <AiConfidenceMeter
        value={0.5}
        showValue
        thresholds={{ low: 0.4, high: 0.5 }}
        data-testid="root"
        messages={pct}
      />,
    );
    const root = screen.getByTestId('root');
    expect(root).toHaveAttribute('data-level', 'high');
    expect(root.querySelector('.mn-ai-confidence-meter__value')).toHaveTextContent('50%');
  });

  it('badge explanation is read and shown on keyboard focus', async () => {
    const user = userEvent.setup();
    render(
      <AiConfidenceMeter
        value={0.4}
        explanation="Two sources disagree on this date."
        data-testid="root"
        messages={pct}
      />,
    );
    const root = screen.getByTestId('root');
    expect(root).toHaveTextContent('Two sources disagree on this date.');
    await user.tab();
    expect(root).toHaveFocus();
    expect(await screen.findByRole('tooltip')).toHaveTextContent(
      'Two sources disagree on this date.',
    );
    await expectNoA11yViolations();
  });

  it('renders the bar as a named meter with value text and explanation', async () => {
    render(
      <AiConfidenceMeter
        variant="bar"
        value={0.72}
        explanation="Matched three of four fields."
        messages={pct}
      />,
    );
    const meter = screen.getByRole('meter', { name: 'Confidence' });
    expect(meter).toHaveAttribute('aria-valuenow', '72');
    expect(meter).toHaveAttribute('aria-valuetext', 'Medium confidence, 72% confidence');
    expect(meter).toHaveAccessibleDescription('Matched three of four fields.');
    expect(screen.getByText('72%')).toBeInTheDocument();
    expect(meter.querySelector('.mn-ai-confidence-meter__fill')).toHaveStyle({
      inlineSize: '72%',
    });
    await expectNoA11yViolations();
  });

  it('clamps out-of-range values and localises strings', () => {
    render(
      <AiConfidenceMeter
        variant="bar"
        value={4}
        messages={{
          ...pct,
          name: 'Certainty',
          labels: { high: 'Sure' },
          valueText: (label, value) => `${label} (${value})`,
        }}
      />,
    );
    const meter = screen.getByRole('meter', { name: 'Certainty' });
    expect(meter).toHaveAttribute('aria-valuenow', '100');
    expect(meter).toHaveAttribute('aria-valuetext', 'Sure (100%)');
  });

  it('renders on the server', () => {
    expect(renderToString(<AiConfidenceMeter value={0.5} />)).toContain('Needs check');
    expect(renderToString(<AiConfidenceMeter value={0.9} variant="bar" />)).toContain(
      'role="meter"',
    );
  });
});
