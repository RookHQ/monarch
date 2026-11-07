import { afterEach, describe, expect, it, vi } from 'vitest';
import { act, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { createRef } from 'react';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { AiErrorMessage, aiErrorKind, type AiErrorKind } from './index.js';

describe('aiErrorKind', () => {
  it.each<[string | number | undefined, AiErrorKind]>([
    [429, 'rate-limit'],
    ['429', 'rate-limit'],
    ['http-429', 'rate-limit'],
    ['rate_limit_exceeded', 'rate-limit'],
    ['RATE-LIMIT', 'rate-limit'],
    ['overloaded_error', 'rate-limit'],
    ['insufficient_quota', 'rate-limit'],
    ['context_length', 'context-overflow'],
    ['context_length_exceeded', 'context-overflow'],
    ['context-overflow', 'context-overflow'],
    [413, 'context-overflow'],
    ['content_filter', 'safety-block'],
    ['safety', 'safety-block'],
    ['moderation_blocked', 'safety-block'],
    ['network', 'network'],
    ['ETIMEDOUT', 'network'],
    ['ECONNREFUSED', 'network'],
    ['http-503', 'network'],
    [504, 'network'],
    ['tool_error', 'tool-failure'],
    ['tool-failure', 'tool-failure'],
    ['http-500', 'unknown'],
    ['service', 'unknown'],
    ['', 'unknown'],
    [undefined, 'unknown'],
  ])('maps %s to %s', (code, kind) => {
    expect(aiErrorKind(code)).toBe(kind);
  });
});

describe('AiErrorMessage', () => {
  afterEach(() => {
    vi.useRealTimers();
  });

  it('renders the unknown card, forwards props and the ref', async () => {
    const ref = createRef<HTMLDivElement>();
    render(<AiErrorMessage ref={ref} data-testid="root" className="extra" onRetry={() => {}} />);
    const root = screen.getByTestId('root');
    expect(root).toHaveClass('mn-ai-error-message', 'extra');
    expect(root).toHaveAttribute('data-kind', 'unknown');
    expect(ref.current).toBe(root);
    expect(screen.getByRole('group', { name: 'Something went wrong' })).toHaveAttribute(
      'data-tone',
      'danger',
    );
    await expectNoA11yViolations();
  });

  it('announces title and description assertively once, without the countdown', () => {
    vi.useFakeTimers();
    render(<AiErrorMessage kind="rate-limit" retryAfter={5} onRetry={() => {}} />);
    const alerts = screen.getAllByRole('alert');
    expect(alerts).toHaveLength(1);
    const [alert] = alerts;
    expect(alert).toHaveTextContent('Too many requests');
    expect(alert).toHaveTextContent(/Wait a moment/);
    const changes: string[] = [];
    const observer = new MutationObserver(() => changes.push(alert!.textContent ?? ''));
    observer.observe(alert!, { childList: true, characterData: true, subtree: true });
    act(() => {
      vi.advanceTimersByTime(3_000);
    });
    observer.disconnect();
    expect(changes).toEqual([]);
    expect(alert).not.toHaveTextContent(/Try again in/);
  });

  it('uses the code when no kind is given, and kind wins over code', () => {
    const { rerender } = render(<AiErrorMessage code="context_length_exceeded" />);
    expect(screen.getByRole('alert')).toHaveTextContent('This conversation is too long');
    rerender(<AiErrorMessage code={429} kind="network" />);
    expect(screen.getByRole('alert')).toHaveTextContent('Connection lost');
  });

  it.each<[AiErrorKind, string, 'warning' | 'danger']>([
    ['rate-limit', 'Too many requests', 'warning'],
    ['context-overflow', 'This conversation is too long', 'warning'],
    ['safety-block', 'The assistant can’t respond to this', 'warning'],
    ['network', 'Connection lost', 'danger'],
    ['tool-failure', 'A tool didn’t work', 'danger'],
    ['unknown', 'Something went wrong', 'danger'],
  ])('shows the default %s copy and tone', (kind, title, tone) => {
    const { container } = render(<AiErrorMessage kind={kind} />);
    expect(screen.getByRole('alert')).toHaveTextContent(title);
    expect(container.querySelector('.mn-alert')).toHaveAttribute('data-tone', tone);
  });

  it('accepts title, description and messages overrides', () => {
    render(
      <AiErrorMessage
        kind="network"
        onRetry={() => {}}
        messages={{ titles: { network: 'Hors ligne' }, retry: 'Réessayer' }}
        description="Custom description."
      />,
    );
    expect(screen.getByRole('alert')).toHaveTextContent('Hors ligne');
    expect(screen.getByRole('alert')).toHaveTextContent('Custom description.');
    expect(screen.getByRole('button', { name: 'Réessayer' })).toBeInTheDocument();
  });

  it('shows only the actions that have handlers and runs them from the keyboard', async () => {
    const user = userEvent.setup();
    const onRetry = vi.fn();
    const onEditPrompt = vi.fn();
    const onSwitchModel = vi.fn();
    const onNewConversation = vi.fn();
    render(
      <AiErrorMessage
        kind="network"
        onRetry={onRetry}
        onEditPrompt={onEditPrompt}
        onSwitchModel={onSwitchModel}
        onNewConversation={onNewConversation}
      />,
    );
    const buttons = screen.getAllByRole('button').map((b) => b.textContent);
    expect(buttons).toEqual(['Try again', 'Edit message', 'Switch model', 'New conversation']);
    expect(screen.getByRole('button', { name: 'Try again' })).toHaveAttribute(
      'data-variant',
      'primary',
    );
    await user.tab();
    expect(screen.getByRole('button', { name: 'Try again' })).toHaveFocus();
    await user.keyboard('{Enter}');
    expect(onRetry).toHaveBeenCalledTimes(1);
    await user.click(screen.getByRole('button', { name: 'Edit message' }));
    await user.click(screen.getByRole('button', { name: 'Switch model' }));
    await user.click(screen.getByRole('button', { name: 'New conversation' }));
    expect(onEditPrompt).toHaveBeenCalledTimes(1);
    expect(onSwitchModel).toHaveBeenCalledTimes(1);
    expect(onNewConversation).toHaveBeenCalledTimes(1);
  });

  it('renders no action row without handlers', () => {
    const { container } = render(<AiErrorMessage kind="unknown" />);
    expect(screen.queryByRole('button')).toBeNull();
    expect(container.querySelector('.mn-ai-error-message__actions')).toBeNull();
  });

  it('leads context-overflow with New conversation and offers Summarize', () => {
    render(
      <AiErrorMessage
        kind="context-overflow"
        onRetry={() => {}}
        onNewConversation={() => {}}
        onSummarize={() => {}}
      />,
    );
    const buttons = screen.getAllByRole('button');
    expect(buttons.map((b) => b.textContent)).toEqual([
      'New conversation',
      'Summarize and continue',
      'Try again',
    ]);
    expect(buttons[0]).toHaveAttribute('data-variant', 'primary');
  });

  it('offers Summarize only for context-overflow', () => {
    render(<AiErrorMessage kind="network" onSummarize={() => {}} />);
    expect(screen.queryByRole('button', { name: 'Summarize and continue' })).toBeNull();
  });

  it('leads safety-block with Edit message', () => {
    render(<AiErrorMessage kind="safety-block" onRetry={() => {}} onEditPrompt={() => {}} />);
    expect(screen.getAllByRole('button')[0]).toHaveTextContent('Edit message');
  });

  it('counts down retryAfter, keeps Retry disabled, then enables it and announces it', () => {
    vi.useFakeTimers();
    const onRetry = vi.fn();
    render(<AiErrorMessage kind="rate-limit" retryAfter={12} onRetry={onRetry} />);
    const retry = () => screen.getByRole('button', { name: /Try again/ });
    expect(retry()).toHaveTextContent('Try again in 12s');
    expect(retry()).toBeDisabled();
    expect(screen.getByRole('status')).toBeEmptyDOMElement();
    act(() => {
      vi.advanceTimersByTime(2_000);
    });
    expect(retry()).toHaveTextContent('Try again in 10s');
    act(() => {
      retry().click();
    });
    expect(onRetry).not.toHaveBeenCalled();
    act(() => {
      vi.advanceTimersByTime(10_000);
    });
    expect(retry()).toHaveTextContent(/^Try again$/);
    expect(retry()).toBeEnabled();
    expect(screen.getByRole('status')).toHaveTextContent('You can try again now.');
    act(() => {
      retry().click();
    });
    expect(onRetry).toHaveBeenCalledTimes(1);
  });

  it('shows the countdown as text when there is no Retry action', () => {
    vi.useFakeTimers();
    render(<AiErrorMessage kind="rate-limit" retryAfter={3} />);
    expect(screen.getByText('Try again in 3s')).toBeInTheDocument();
    act(() => {
      vi.advanceTimersByTime(3_000);
    });
    expect(screen.queryByText(/Try again in/)).toBeNull();
  });

  it('shows a busy Retry while retrying', () => {
    render(<AiErrorMessage onRetry={() => {}} retrying />);
    expect(screen.getByRole('button', { name: /Try again/ })).toHaveAttribute('aria-busy', 'true');
  });

  it('puts technical details in a collapsed disclosure', async () => {
    const user = userEvent.setup();
    const { container } = render(
      <AiErrorMessage kind="tool-failure" details="request_id: req_123\nstatus: 502" />,
    );
    const details = container.querySelector('details')!;
    expect(details).not.toHaveAttribute('open');
    expect(screen.getByRole('alert')).not.toHaveTextContent('req_123');
    await user.click(screen.getByText('Technical details'));
    expect(details).toHaveAttribute('open');
    expect(container.querySelector('pre')).toHaveTextContent('req_123');
    await expectNoA11yViolations();
  });

  it('renders extra actions after the built-in ones', () => {
    render(
      <AiErrorMessage
        onRetry={() => {}}
        actions={
          <button type="button" className="custom">
            Report
          </button>
        }
      />,
    );
    expect(screen.getAllByRole('button').map((b) => b.textContent)).toEqual([
      'Try again',
      'Report',
    ]);
  });

  it('renders on the server with the initial countdown', () => {
    const html = renderToString(
      <AiErrorMessage code={429} retryAfter={9} onRetry={() => {}} details="id" />,
    );
    expect(html).toContain('mn-ai-error-message');
    expect(html).toContain('data-kind="rate-limit"');
    expect(html).toContain('Try again in 9s');
    expect(html).toContain('role="alert"');
  });
});
