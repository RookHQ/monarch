import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { createRef } from 'react';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { AiInsightCallout, type AiInsightSeverity } from './index.js';

describe('AiInsightCallout', () => {
  it('names the group by its title and describes it by the reason; forwards props and ref', async () => {
    const ref = createRef<HTMLDivElement>();
    render(
      <AiInsightCallout
        ref={ref}
        data-testid="root"
        className="extra"
        severity="warning"
        origin="ai"
        title="Renewal at risk"
        why="Usage dropped 40% over the last 30 days."
      />,
    );
    const root = screen.getByTestId('root');
    expect(root).toHaveClass('mn-ai-insight-callout', 'extra');
    expect(ref.current).toBe(root);
    const group = screen.getByRole('group', { name: 'Warning: Renewal at risk' });
    expect(group).toBe(root);
    expect(group).toHaveAccessibleDescription('Usage dropped 40% over the last 30 days.');
    expect(root).toHaveAttribute('data-severity', 'warning');
    expect(root).toHaveAttribute('data-origin', 'ai');
    await expectNoA11yViolations();
  });

  it('labels AI insights with the AI chip and the Bot glyph', () => {
    const { container } = render(
      <AiInsightCallout severity="info" origin="ai" title="Trend" why="Reason." />,
    );
    const chip = container.querySelector('.mn-ai-insight-callout__origin')!;
    expect(chip).toHaveTextContent('AI insight');
    expect(chip).toHaveAttribute('data-origin', 'ai');
    expect(chip.querySelector('svg')).toHaveAttribute('aria-hidden');
  });

  it('labels rule-based insights honestly, with no AI label or AI colour hook', () => {
    const { container } = render(
      <AiInsightCallout severity="critical" origin="rule" title="Overdue" why="30 days late." />,
    );
    const chip = container.querySelector('.mn-ai-insight-callout__origin')!;
    expect(chip).toHaveTextContent('Rule-based');
    expect(chip).toHaveAttribute('data-origin', 'rule');
    expect(container).not.toHaveTextContent(/AI/);
  });

  it.each<[AiInsightSeverity, string]>([
    ['info', 'Info'],
    ['warning', 'Warning'],
    ['critical', 'Critical'],
    ['positive', 'Positive'],
  ])('announces the %s severity as text, never colour alone', (severity, label) => {
    render(<AiInsightCallout severity={severity} origin="rule" title="Title" why="Why" />);
    expect(screen.getByRole('group', { name: `${label}: Title` })).toBeInTheDocument();
  });

  it('keeps evidence in a collapsed disclosure', async () => {
    const user = userEvent.setup();
    const { container } = render(
      <AiInsightCallout
        severity="info"
        origin="ai"
        title="Title"
        why="Why"
        evidence={<p>Three tickets opened this week.</p>}
      />,
    );
    const details = container.querySelector('details')!;
    expect(details).not.toHaveAttribute('open');
    await user.click(screen.getByText('Show evidence'));
    expect(details).toHaveAttribute('open');
    await expectNoA11yViolations();
  });

  it('runs the action and the dismiss from the keyboard', async () => {
    const user = userEvent.setup();
    const onClick = vi.fn();
    const onDismiss = vi.fn();
    render(
      <AiInsightCallout
        severity="warning"
        origin="rule"
        title="Title"
        why="Why"
        action={{ label: 'Open account', onClick }}
        onDismiss={onDismiss}
      />,
    );
    await user.tab();
    expect(screen.getByRole('button', { name: 'Open account' })).toHaveFocus();
    await user.keyboard('{Enter}');
    expect(onClick).toHaveBeenCalledTimes(1);
    await user.tab();
    expect(screen.getByRole('button', { name: 'Dismiss insight' })).toHaveFocus();
    await user.keyboard(' ');
    expect(onDismiss).toHaveBeenCalledTimes(1);
  });

  it('renders no buttons without action or onDismiss', () => {
    render(<AiInsightCallout severity="info" origin="rule" title="Title" why="Why" />);
    expect(screen.queryByRole('button')).toBeNull();
  });

  it('renders the title as a heading when headingLevel is set', () => {
    render(
      <AiInsightCallout severity="info" origin="ai" title="Title" why="Why" headingLevel={3} />,
    );
    expect(screen.getByRole('heading', { level: 3, name: 'Info: Title' })).toBeInTheDocument();
  });

  it('accepts message overrides per key', () => {
    render(
      <AiInsightCallout
        severity="critical"
        origin="rule"
        title="Retard"
        why="Pourquoi"
        onDismiss={() => {}}
        messages={{
          severity: { critical: 'Critique' },
          origin: { rule: 'Règle' },
          dismiss: 'Fermer',
        }}
      />,
    );
    expect(screen.getByRole('group', { name: 'Critique: Retard' })).toHaveTextContent('Règle');
    expect(screen.getByRole('button', { name: 'Fermer' })).toBeInTheDocument();
  });

  it('renders on the server', () => {
    const html = renderToString(
      <AiInsightCallout severity="positive" origin="ai" title="Up" why="Why" evidence="x" />,
    );
    expect(html).toContain('mn-ai-insight-callout');
    expect(html).toContain('data-origin="ai"');
    expect(html).toContain('AI insight');
  });
});
