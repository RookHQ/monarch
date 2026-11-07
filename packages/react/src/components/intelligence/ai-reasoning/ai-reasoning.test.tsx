import { afterEach, describe, expect, it, vi } from 'vitest';
import { act, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { createRef } from 'react';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import type { AiReasoningPart } from '../ai-conversation/index.js';
import { AiReasoning, formatReasoningDuration } from './index.js';

const done: AiReasoningPart = {
  type: 'reasoning',
  text: 'Compare the two drafts, then list the differences.',
  state: 'done',
  durationMs: 12_000,
};
const streaming: AiReasoningPart = { type: 'reasoning', text: 'Compare the', state: 'streaming' };

function details(container: HTMLElement) {
  return container.querySelector('details') as HTMLDetailsElement;
}

describe('AiReasoning', () => {
  afterEach(() => {
    vi.useRealTimers();
  });

  it('renders finished reasoning collapsed with its duration, forwarding props and ref', async () => {
    const ref = createRef<HTMLDivElement>();
    const { container } = render(
      <AiReasoning ref={ref} part={done} data-testid="root" className="extra" />,
    );
    const root = screen.getByTestId('root');
    expect(root).toHaveClass('mn-ai-reasoning', 'extra');
    expect(ref.current).toBe(root);
    expect(root).toHaveAttribute('data-state', 'done');
    expect(details(container).open).toBe(false);
    expect(container.querySelector('summary')).toHaveTextContent(
      'Assistant reasoning: Thought for 12s',
    );
    await expectNoA11yViolations();
  });

  // Enter/Space on <summary> is native browser behaviour (jsdom does not
  // implement it), so activation is simulated with a click.
  it('opens and closes through the summary', async () => {
    const user = userEvent.setup();
    const onOpenChange = vi.fn();
    const { container } = render(<AiReasoning part={done} onOpenChange={onOpenChange} />);
    const summary = container.querySelector('summary') as HTMLElement;
    await user.click(summary);
    expect(details(container).open).toBe(true);
    expect(onOpenChange).toHaveBeenLastCalledWith(true);
    expect(screen.getByText(/list the differences/)).toBeVisible();
    await user.click(summary);
    expect(details(container).open).toBe(false);
    expect(onOpenChange).toHaveBeenLastCalledWith(false);
  });

  it('honours defaultOpen for finished reasoning', () => {
    const { container } = render(<AiReasoning part={done} defaultOpen />);
    expect(details(container).open).toBe(true);
  });

  it('is open and says "Thinking…" while streaming, then collapses with a measured duration', () => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date('2026-01-01T00:00:00Z'));
    const { container, rerender } = render(<AiReasoning part={streaming} />);
    expect(details(container).open).toBe(true);
    expect(container.querySelector('summary')).toHaveTextContent('Thinking…');
    expect(container.querySelector('[aria-busy="true"]')).not.toBeNull();
    // Reasoning never speaks: no live region of its own.
    expect(container.querySelector('[aria-live]')).toBeNull();
    vi.setSystemTime(new Date('2026-01-01T00:00:07Z'));
    act(() => {
      rerender(<AiReasoning part={{ ...streaming, text: 'Compare the drafts.', state: 'done' }} />);
    });
    expect(details(container).open).toBe(false);
    expect(container.querySelector('summary')).toHaveTextContent('Thought for 7s');
  });

  it('stays open when the person toggled it while streaming', async () => {
    const user = userEvent.setup();
    const { container, rerender } = render(<AiReasoning part={streaming} />);
    const summary = container.querySelector('summary') as HTMLElement;
    await user.click(summary); // close
    await user.click(summary); // reopen deliberately
    expect(details(container).open).toBe(true);
    rerender(<AiReasoning part={{ ...streaming, state: 'done', durationMs: 3000 }} />);
    expect(details(container).open).toBe(true);
    expect(summary).toHaveTextContent('Thought for 3s');
  });

  it('shows "Thought" when no duration is known', () => {
    const { durationMs: _omit, ...noDuration } = done;
    render(<AiReasoning part={noDuration} />);
    expect(screen.getByText('Thought')).toBeInTheDocument();
  });

  it('renders redacted reasoning as a static label with no body', async () => {
    const { container } = render(
      <AiReasoning part={{ ...done, text: 'secret', redacted: true }} />,
    );
    expect(container.querySelector('details')).toBeNull();
    expect(container).toHaveTextContent('Reasoning hidden');
    expect(container).not.toHaveTextContent('secret');
    expect(container.firstElementChild).toHaveAttribute('data-redacted');
    await expectNoA11yViolations();
  });

  it('says "Thinking…" for redacted reasoning that is still running', () => {
    const { container } = render(
      <AiReasoning part={{ type: 'reasoning', text: '', state: 'streaming', redacted: true }} />,
    );
    expect(container).toHaveTextContent('Thinking…');
  });

  it('localises strings and the duration', () => {
    render(
      <AiReasoning
        part={{ ...done, durationMs: 65_000 }}
        messages={{ thoughtFor: (d) => `Réfléchi pendant ${d}`, prefix: 'Raisonnement :' }}
        locale="fr"
      />,
    );
    expect(screen.getByText(/Réfléchi pendant/)).toHaveTextContent(/1\s?min 5\s?s/);
  });

  it('formats durations', () => {
    expect(formatReasoningDuration(400, 'en')).toBe('1s');
    expect(formatReasoningDuration(12_000, 'en')).toBe('12s');
    expect(formatReasoningDuration(65_000, 'en')).toBe('1m 5s');
    expect(formatReasoningDuration(120_000, 'en')).toBe('2m');
  });

  it('renders on the server', () => {
    const html = renderToString(<AiReasoning part={streaming} />);
    expect(html).toContain('mn-ai-reasoning');
    expect(html).toContain('Thinking…');
    expect(html).toContain('open=""');
  });
});
