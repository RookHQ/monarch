import { afterEach, describe, expect, it, vi } from 'vitest';
import { act, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { ThreadPanel } from './index.js';

function Panel(props: Partial<Parameters<typeof ThreadPanel>[0]>) {
  return (
    <ThreadPanel
      replyCount={2}
      root={<p>Where should the launch notes live?</p>}
      composer={<textarea aria-label="Reply" />}
      onClose={() => {}}
      {...props}
    >
      <p>In the shared folder.</p>
      <p>Agreed.</p>
    </ThreadPanel>
  );
}

afterEach(() => {
  vi.useRealTimers();
});

describe('ThreadPanel', () => {
  it('renders a labelled region with the root, reply count, replies log and composer', async () => {
    render(<Panel />);
    const panel = screen.getByRole('region', { name: 'Thread' });
    expect(panel).toHaveClass('mn-thread-panel');
    expect(screen.getByText('2 replies')).toBeInTheDocument();
    const log = screen.getByRole('log', { name: 'Replies' });
    expect(log).toHaveAttribute('tabindex', '0');
    expect(log).toHaveTextContent('Agreed.');
    expect(screen.getByRole('textbox', { name: 'Reply' })).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('closes from the header button', async () => {
    const onClose = vi.fn();
    render(<Panel onClose={onClose} />);
    await userEvent.setup().click(screen.getByRole('button', { name: 'Close thread' }));
    expect(onClose).toHaveBeenCalled();
  });

  it('toggles following with aria-pressed, stays busy and announces the result', async () => {
    let finish!: () => void;
    const onFollowingChange = vi.fn(
      () =>
        new Promise<void>((res) => {
          finish = res;
        }),
    );
    const user = userEvent.setup();
    const { rerender } = render(<Panel following={false} onFollowingChange={onFollowingChange} />);
    const toggle = screen.getByRole('button', { name: 'Follow' });
    expect(toggle).toHaveAttribute('aria-pressed', 'false');
    toggle.focus();
    await user.keyboard('{Enter}');
    expect(onFollowingChange).toHaveBeenCalledWith(true);
    expect(toggle).toHaveAttribute('aria-busy', 'true');
    await act(async () => finish());
    rerender(<Panel following onFollowingChange={onFollowingChange} />);
    expect(screen.getByRole('button', { name: 'Following' })).toHaveAttribute(
      'aria-pressed',
      'true',
    );
    expect(screen.getByText('You’ll be notified about replies')).toBeInTheDocument();
  });

  it('announces a failed follow change', async () => {
    const onFollowingChange = vi.fn(() => Promise.reject(new Error('x')));
    render(<Panel onFollowingChange={onFollowingChange} />);
    await userEvent.setup().click(screen.getByRole('button', { name: 'Follow' }));
    expect(
      await screen.findByText('Thread notifications didn’t change. Try again.'),
    ).toBeInTheDocument();
  });

  it('shows loading, error with retry and deleted states', async () => {
    const onRetry = vi.fn();
    const onClose = vi.fn();
    const user = userEvent.setup();
    const { rerender } = render(<Panel status="loading" />);
    expect(screen.getByRole('status', { name: 'Loading thread…' })).toBeInTheDocument();
    expect(screen.queryByText('2 replies')).toBeNull();

    rerender(<Panel status="error" onRetry={onRetry} />);
    expect(screen.getByText('This thread couldn’t be loaded.')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Try again' }));
    expect(onRetry).toHaveBeenCalled();

    rerender(<Panel status="deleted" onClose={onClose} />);
    expect(screen.getByText('This thread was deleted.')).toBeInTheDocument();
    const closes = screen.getAllByRole('button', { name: 'Close thread' });
    await user.click(closes[closes.length - 1]!);
    expect(onClose).toHaveBeenCalled();
    await expectNoA11yViolations();
  });

  it('calls onRepliesSeen after the delay with the reply count', async () => {
    vi.useFakeTimers();
    const onSeen = vi.fn();
    const { rerender } = render(<Panel onRepliesSeen={onSeen} seenDelay={300} />);
    expect(onSeen).not.toHaveBeenCalled();
    act(() => {
      vi.advanceTimersByTime(300);
    });
    expect(onSeen).toHaveBeenCalledWith(2);
    rerender(<Panel onRepliesSeen={onSeen} seenDelay={300} replyCount={3} />);
    act(() => {
      vi.advanceTimersByTime(300);
    });
    expect(onSeen).toHaveBeenLastCalledWith(3);
  });

  it('keeps the newest reply in view when pinned to the bottom', async () => {
    const { rerender } = render(<Panel />);
    const log = screen.getByRole('log');
    Object.defineProperty(log, 'scrollHeight', { configurable: true, value: 900 });
    rerender(<Panel replyCount={3} />);
    await waitFor(() => expect(log.scrollTop).toBe(900));
  });

  it('shows a notice, typing slot and translated text', () => {
    render(
      <Panel
        notice="Showing the first 500 replies."
        typing={<span>Ana is typing…</span>}
        messages={{ title: 'Unterhaltung', replyCount: (n) => `${n} Antworten` }}
      />,
    );
    expect(screen.getByRole('region', { name: 'Unterhaltung' })).toBeInTheDocument();
    expect(screen.getByText('2 Antworten')).toBeInTheDocument();
    expect(screen.getByText('Showing the first 500 replies.')).toBeInTheDocument();
    expect(screen.getByText('Ana is typing…')).toBeInTheDocument();
  });

  it('renders on the server', () => {
    const html = renderToString(<Panel />);
    expect(html).toContain('mn-thread-panel');
    expect(html).toContain('2 replies');
  });
});
