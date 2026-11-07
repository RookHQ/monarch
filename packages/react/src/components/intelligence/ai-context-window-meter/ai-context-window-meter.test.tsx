import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { createRef } from 'react';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { AiContextWindowMeter, aiContextWindowState } from './index.js';

const fmt = {
  formatNumber: (v: number) => String(v),
  formatPercent: (v: number) => `${Math.round(v * 100)}%`,
};

const breakdown = [
  { label: 'Instructions', tokens: 2000 },
  { label: 'Attachments', tokens: 10000 },
  { label: 'Messages', tokens: 20000 },
];

describe('aiContextWindowState', () => {
  it('maps usage to ok, warning and full', () => {
    expect(aiContextWindowState(10, 100)).toBe('ok');
    expect(aiContextWindowState(80, 100)).toBe('warning');
    expect(aiContextWindowState(100, 100)).toBe('full');
    expect(aiContextWindowState(60, 100, 0.5)).toBe('warning');
    expect(aiContextWindowState(10, 0)).toBe('ok');
  });
});

describe('AiContextWindowMeter', () => {
  it('renders a named meter, forwards props and the ref', async () => {
    const ref = createRef<HTMLDivElement>();
    render(
      <AiContextWindowMeter
        ref={ref}
        used={32000}
        limit={128000}
        data-testid="root"
        className="x"
        messages={fmt}
      />,
    );
    const root = screen.getByTestId('root');
    expect(root).toHaveClass('mn-ai-context-window-meter', 'x');
    expect(ref.current).toBe(root);
    expect(root).toHaveAttribute('data-state', 'ok');
    const meter = screen.getByRole('meter', { name: 'Context window' });
    expect(meter).toHaveAttribute('aria-valuetext', '32000 of 128000 tokens (25%)');
    expect(meter).toHaveAttribute('aria-valuemax', '128000');
    await expectNoA11yViolations();
  });

  it('draws the breakdown as segments with a legend, adding "Other" for the rest', async () => {
    render(
      <AiContextWindowMeter used={40000} limit={128000} breakdown={breakdown} messages={fmt} />,
    );
    const meter = screen.getByRole('meter', { name: 'Context window' });
    const segments = meter.querySelectorAll('.mn-progress__segment');
    expect(segments).toHaveLength(4);
    expect(segments[0]).toHaveAttribute('data-tone', 'data-1');
    expect(segments[3]).toHaveAttribute('data-tone', 'neutral');
    const legend = screen.getByRole('list', { name: 'What fills the context window' });
    const items = legend.querySelectorAll('li');
    expect(items).toHaveLength(4);
    expect(items[1]).toHaveTextContent('Attachments10000 tokens');
    expect(items[3]).toHaveTextContent('Other8000 tokens');
    await expectNoA11yViolations();
  });

  it('warns at warnAt and when full, describing the meter', () => {
    const { rerender } = render(
      <AiContextWindowMeter used={70} limit={100} warnAt={0.7} data-testid="root" messages={fmt} />,
    );
    const root = screen.getByTestId('root');
    expect(root).toHaveAttribute('data-state', 'warning');
    const meter = screen.getByRole('meter');
    expect(screen.getByRole('status')).toHaveTextContent('nearly at its context limit');
    expect(meter).toHaveAccessibleDescription(/nearly at its context limit/);
    expect(meter).toHaveAttribute('data-tone', 'warning');

    rerender(<AiContextWindowMeter used={120} limit={100} data-testid="root" messages={fmt} />);
    expect(root).toHaveAttribute('data-state', 'full');
    expect(screen.getByRole('status')).toHaveTextContent('The context window is full.');
    expect(screen.getByRole('meter')).toHaveAttribute('aria-valuetext', '120 of 100 tokens (100%)');

    rerender(<AiContextWindowMeter used={10} limit={100} data-testid="root" messages={fmt} />);
    expect(screen.getByRole('status')).toBeEmptyDOMElement();
    expect(screen.getByRole('meter')).not.toHaveAttribute('aria-describedby');
  });

  it('summarizes earlier messages from the keyboard and shows pending', async () => {
    const user = userEvent.setup();
    const onSummarize = vi.fn();
    const { rerender } = render(
      <AiContextWindowMeter used={90} limit={100} onSummarize={onSummarize} />,
    );
    const button = screen.getByRole('button', { name: 'Summarize earlier messages' });
    await user.tab();
    expect(button).toHaveFocus();
    await user.keyboard('{Enter}');
    expect(onSummarize).toHaveBeenCalledTimes(1);
    rerender(<AiContextWindowMeter used={90} limit={100} onSummarize={onSummarize} summarizing />);
    expect(screen.getByRole('button', { name: /Summarize earlier messages/ })).toHaveAttribute(
      'aria-busy',
      'true',
    );
    await expectNoA11yViolations();
  });

  it('hides the button without onSummarize and the header in compact mode', () => {
    render(
      <AiContextWindowMeter
        used={10}
        limit={100}
        compact
        breakdown={breakdown}
        data-testid="root"
      />,
    );
    expect(screen.queryByRole('button')).toBeNull();
    expect(screen.queryByRole('list')).toBeNull();
    expect(screen.getByTestId('root')).toHaveAttribute('data-compact');
    expect(screen.getByRole('meter', { name: 'Context window' })).toBeInTheDocument();
  });

  it('localises strings', () => {
    render(
      <AiContextWindowMeter
        used={1}
        limit={2}
        onSummarize={() => {}}
        messages={{ ...fmt, label: 'Contexte', summarize: 'Résumer' }}
      />,
    );
    expect(screen.getByRole('meter', { name: 'Contexte' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Résumer' })).toBeInTheDocument();
  });

  it('renders on the server', () => {
    const html = renderToString(
      <AiContextWindowMeter used={100} limit={200} breakdown={breakdown} onSummarize={() => {}} />,
    );
    expect(html).toContain('mn-ai-context-window-meter');
    expect(html).toContain('role="meter"');
  });
});
