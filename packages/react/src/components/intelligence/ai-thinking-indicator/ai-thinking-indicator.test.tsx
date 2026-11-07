import { afterEach, describe, expect, it, vi } from 'vitest';
import { act, render, screen } from '@testing-library/react';
import { renderToString } from 'react-dom/server';
import { createRef } from 'react';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { AiThinkingIndicator } from './index.js';

function mockMatchMedia(matches: (query: string) => boolean) {
  const original = window.matchMedia;
  window.matchMedia = ((query: string) => ({
    matches: matches(query),
    media: query,
    onchange: null,
    addListener() {},
    removeListener() {},
    addEventListener() {},
    removeEventListener() {},
    dispatchEvent: () => false,
  })) as unknown as typeof window.matchMedia;
  return () => {
    window.matchMedia = original;
  };
}

describe('AiThinkingIndicator', () => {
  afterEach(() => {
    vi.useRealTimers();
  });

  it('renders the shimmer variant with its root class, forwards props and the ref', async () => {
    const ref = createRef<HTMLDivElement>();
    render(<AiThinkingIndicator ref={ref} data-testid="root" className="extra" />);
    const root = screen.getByTestId('root');
    expect(root).toHaveClass('mn-ai-thinking-indicator', 'extra');
    expect(root).toHaveAttribute('data-variant', 'shimmer');
    expect(ref.current).toBe(root);
    expect(root.querySelector('.mn-ai-thinking-indicator__mark')).toBeNull();
    await expectNoA11yViolations();
  });

  it('announces the label once through a polite status, not on every tick', () => {
    vi.useFakeTimers();
    render(<AiThinkingIndicator />);
    const status = screen.getByRole('status');
    expect(status).toHaveTextContent('Thinking…');
    const observed: string[] = [];
    const observer = new MutationObserver(() => observed.push(status.textContent ?? ''));
    observer.observe(status, { childList: true, characterData: true, subtree: true });
    act(() => {
      vi.advanceTimersByTime(10_000);
    });
    // Elapsed text is outside the live region.
    expect(screen.getByText('10s')).toBeInTheDocument();
    expect(status).toHaveTextContent(/^Thinking…$/);
    observer.disconnect();
    expect(observed).toEqual([]);
  });

  it('re-announces only when the label changes', () => {
    const { rerender } = render(<AiThinkingIndicator label="Reading files…" />);
    expect(screen.getByRole('status')).toHaveTextContent('Reading files…');
    rerender(<AiThinkingIndicator label="Searching the web…" />);
    expect(screen.getByRole('status')).toHaveTextContent('Searching the web…');
  });

  it('shows elapsed time after three seconds and updates it each second', () => {
    vi.useFakeTimers();
    const { container } = render(<AiThinkingIndicator />);
    const root = container.firstElementChild!;
    act(() => {
      vi.advanceTimersByTime(2_000);
    });
    expect(container.querySelector('.mn-ai-thinking-indicator__elapsed')).toBeNull();
    expect(root).not.toHaveAttribute('data-elapsed');
    act(() => {
      vi.advanceTimersByTime(1_000);
    });
    expect(screen.getByText('3s')).toBeInTheDocument();
    expect(root).toHaveAttribute('data-elapsed');
    act(() => {
      vi.advanceTimersByTime(2_000);
    });
    expect(screen.getByText('5s')).toBeInTheDocument();
    act(() => {
      vi.advanceTimersByTime(60_000);
    });
    expect(screen.getByText('1m 5s')).toBeInTheDocument();
  });

  it('counts from startedAt and honours showElapsedAfter and messages', () => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date('2026-01-01T00:00:20Z'));
    render(
      <AiThinkingIndicator
        startedAt="2026-01-01T00:00:08Z"
        showElapsedAfter={10}
        messages={{ thinking: 'Réflexion…', elapsed: (s) => `${s} s` }}
      />,
    );
    expect(screen.getByRole('status')).toHaveTextContent('Réflexion…');
    expect(screen.getByText('12 s')).toBeInTheDocument();
  });

  it('never shows elapsed time when showElapsedAfter is Infinity', () => {
    vi.useFakeTimers();
    const { container } = render(<AiThinkingIndicator showElapsedAfter={Infinity} />);
    act(() => {
      vi.advanceTimersByTime(30_000);
    });
    expect(container.querySelector('.mn-ai-thinking-indicator__elapsed')).toBeNull();
  });

  it('renders the bot mark variant, decorative to assistive technology', async () => {
    const { container } = render(<AiThinkingIndicator variant="mark" />);
    const mark = container.querySelector('.mn-ai-thinking-indicator__mark');
    expect(mark).toHaveAttribute('aria-hidden', 'true');
    expect(mark?.querySelector('svg')).not.toBeNull();
    await expectNoA11yViolations();
  });

  it('marks itself static under reduced motion or forced colours', () => {
    const restore = mockMatchMedia((q) => q.includes('prefers-reduced-motion'));
    try {
      const { container } = render(<AiThinkingIndicator />);
      expect(container.firstElementChild).toHaveAttribute('data-static');
    } finally {
      restore();
    }
  });

  it('animates when no motion preference is set', () => {
    const restore = mockMatchMedia(() => false);
    try {
      const { container } = render(<AiThinkingIndicator />);
      expect(container.firstElementChild).not.toHaveAttribute('data-static');
    } finally {
      restore();
    }
  });

  it('renders on the server without the timer or announcement', () => {
    const html = renderToString(<AiThinkingIndicator startedAt={0} />);
    expect(html).toContain('mn-ai-thinking-indicator');
    expect(html).toContain('Thinking…');
    expect(html).not.toContain('mn-ai-thinking-indicator__elapsed');
  });
});
