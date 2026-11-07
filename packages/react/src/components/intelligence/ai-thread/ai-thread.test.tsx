import { afterEach, describe, expect, it, vi } from 'vitest';
import { act, fireEvent, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import {
  mockTransport,
  textDeltas,
  useAiConversation,
  type AiMessage,
} from '../ai-conversation/index.js';
import { AiThread, type AiThreadProps } from './index.js';

const NOW = '2026-03-03T12:00:00.000Z';

function msg(
  i: number,
  role: AiMessage['role'] = i % 2 ? 'assistant' : 'user',
  createdAt = NOW,
): AiMessage {
  return {
    id: `m${i}`,
    role,
    createdAt,
    parts: [{ type: 'text', text: `Message ${i}`, state: 'done' }],
  };
}
const list = (n: number, from = 0) => Array.from({ length: n }, (_, i) => msg(i + from));

/** Scroll metrics for the log: each rendered row is 100px tall. */
function metrics(clientHeight = 400) {
  const el = screen.getByRole('log');
  const m = { clientHeight, scrollTop: 0 };
  const height = () => el.querySelectorAll('[data-row]').length * 100;
  Object.defineProperties(el, {
    scrollHeight: { configurable: true, get: height },
    clientHeight: { configurable: true, get: () => m.clientHeight },
    scrollTop: {
      configurable: true,
      get: () => m.scrollTop,
      set: (v: number) => {
        m.scrollTop = Math.max(0, Math.min(v, height() - m.clientHeight));
      },
    },
  });
  return { el, m, height };
}

function Thread(props: Partial<AiThreadProps>) {
  return <AiThread messages={[]} now={NOW} timeZone="UTC" locale="en-GB" {...props} />;
}

describe('AiThread', () => {
  afterEach(() => {
    vi.useRealTimers();
  });

  it('is a labelled, focusable log with day separators and no live log chatter', async () => {
    const messages = [
      msg(0, 'user', '2026-03-02T09:00:00.000Z'),
      msg(1, 'assistant', '2026-03-02T09:00:05.000Z'),
      msg(2, 'user', NOW),
      msg(3, 'assistant', NOW),
    ];
    render(<Thread messages={messages} data-testid="root" />);
    expect(screen.getByTestId('root')).toHaveClass('mn-ai-thread');
    const log = screen.getByRole('log', { name: 'Conversation' });
    expect(log).toHaveAttribute('aria-live', 'off');
    expect(log).toHaveAttribute('tabindex', '0');
    expect(screen.getAllByRole('article')).toHaveLength(4);
    const days = Array.from(document.querySelectorAll('.mn-ai-thread__day')).map(
      (d) => d.textContent,
    );
    expect(days).toEqual(['Yesterday', 'Today']);
    await expectNoA11yViolations();
  });

  it('shows the empty slot when there are no messages', () => {
    render(<Thread empty={<p>How can I help?</p>} />);
    expect(screen.getByText('How can I help?')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /Jump to latest/ })).toBeNull();
  });

  it('stays pinned to the bottom as messages arrive', () => {
    const { rerender } = render(<Thread messages={list(6)} />);
    const { m, height } = metrics();
    rerender(<Thread messages={list(7)} />);
    expect(m.scrollTop).toBe(height() - 400);
    rerender(<Thread messages={list(9)} />);
    expect(m.scrollTop).toBe(height() - 400);
    expect(m.scrollTop).toBeGreaterThan(0);
  });

  it('"Jump to latest" appears when scrolled up, counts new messages and returns to the bottom', async () => {
    const { rerender } = render(<Thread messages={list(6)} />);
    const { el, m, height } = metrics();
    rerender(<Thread messages={list(6)} status="idle" />);
    m.scrollTop = 0;
    fireEvent.scroll(el);
    const jump = screen.getByRole('button', { name: 'Jump to latest' });
    rerender(<Thread messages={list(8)} />);
    expect(m.scrollTop).toBe(0);
    expect(jump).toHaveAccessibleName('Jump to latest, 2 new messages');
    jump.focus();
    await userEvent.setup().click(jump);
    expect(m.scrollTop).toBe(height() - 400);
    expect(screen.queryByRole('button', { name: /Jump to latest/ })).toBeNull();
    expect(el).toHaveFocus();
  });

  it('keeps the scroll position when earlier messages are prepended', async () => {
    const onLoadEarlier = vi.fn();
    const { rerender } = render(
      <Thread messages={list(6, 10)} hasEarlier onLoadEarlier={onLoadEarlier} />,
    );
    const { el, m } = metrics();
    rerender(<Thread messages={list(6, 10)} hasEarlier onLoadEarlier={onLoadEarlier} />);
    m.scrollTop = 50;
    fireEvent.scroll(el);
    await userEvent.setup().click(screen.getByRole('button', { name: 'Load earlier messages' }));
    expect(onLoadEarlier).toHaveBeenCalledOnce();
    rerender(<Thread messages={list(16)} hasEarlier={false} onLoadEarlier={onLoadEarlier} />);
    expect(m.scrollTop).toBe(50 + 1000);
    expect(screen.queryByRole('button', { name: 'Load earlier messages' })).toBeNull();
  });

  it('windows long threads: renders a subset near the viewport, always including the newest', async () => {
    const messages = list(500);
    const { rerender } = render(<Thread messages={messages} virtualizeAfter={200} keepLast={10} />);
    const el = screen.getByRole('log');
    expect(el.closest('.mn-ai-thread')).toHaveAttribute('data-virtualized');
    const m = { top: 0 };
    Object.defineProperties(el, {
      scrollHeight: { configurable: true, get: () => 60_000 },
      clientHeight: { configurable: true, get: () => 400 },
      scrollTop: { configurable: true, get: () => m.top, set: (v: number) => (m.top = v) },
    });
    // Scrolled to the end: only the tail and its neighbours are rendered.
    m.top = 59_600;
    fireEvent.scroll(el);
    await waitFor(() => expect(document.querySelector('[data-row="m0"]')).toBeNull());
    const rendered = screen.getAllByRole('article');
    expect(rendered.length).toBeLessThan(100);
    expect(rendered.length).toBeGreaterThanOrEqual(10);
    expect(document.querySelector('[data-row="m499"]')).not.toBeNull();
    expect(document.querySelector('.mn-ai-thread__spacer')).toBeInTheDocument();
    // Scrolled to the top: the first rows render, the tail stays, the middle does not.
    m.top = 0;
    fireEvent.scroll(el);
    await waitFor(() => expect(document.querySelector('[data-row="m0"]')).not.toBeNull());
    rerender(<Thread messages={messages} virtualizeAfter={200} keepLast={10} />);
    expect(document.querySelector('[data-row="m250"]')).toBeNull();
    expect(document.querySelector('[data-row="m499"]')).not.toBeNull();
  });

  it('does not window short threads', () => {
    render(<Thread messages={list(50)} />);
    expect(screen.getAllByRole('article')).toHaveLength(50);
    expect(document.querySelector('[data-virtualized]')).toBeNull();
  });

  it('Escape inside the thread stops a running reply, and only then', async () => {
    const onStop = vi.fn();
    const { rerender } = render(<Thread messages={list(2)} status="streaming" onStop={onStop} />);
    const user = userEvent.setup();
    screen.getByRole('log').focus();
    await user.keyboard('{Escape}');
    expect(onStop).toHaveBeenCalledOnce();
    expect(screen.getByRole('log')).toHaveAttribute('aria-busy', 'true');
    rerender(<Thread messages={list(2)} status="idle" onStop={onStop} />);
    await user.keyboard('{Escape}');
    expect(onStop).toHaveBeenCalledOnce();
  });

  it('shows the thinking indicator while submitted', () => {
    render(<Thread messages={list(1)} status="submitted" />);
    expect(document.querySelector('.mn-ai-thread__thinking')).toBeInTheDocument();
    expect(document.querySelector('.mn-ai-thread__announcer[role="status"]')).toHaveTextContent(
      'Thinking…',
    );
  });

  it('announces at sentence boundaries, throttled, then "Response complete"', () => {
    vi.useFakeTimers();
    const user0 = msg(0, 'user');
    const reply = (text: string, state: 'streaming' | 'done' = 'streaming'): AiMessage => ({
      id: 'r1',
      role: 'assistant',
      createdAt: NOW,
      parts: [{ type: 'text', text, state }],
    });
    const { rerender } = render(<Thread messages={[user0]} status="submitted" />);
    const polite = () => document.querySelector('.mn-ai-thread__announcer[role="status"]')!;
    rerender(<Thread messages={[user0, reply('Hello there. Ho')]} status="streaming" />);
    expect(polite()).toHaveTextContent(/^Hello there\.$/);
    rerender(
      <Thread messages={[user0, reply('Hello there. How are you? I')]} status="streaming" />,
    );
    // Within the interval: queued, not yet spoken.
    expect(polite()).toHaveTextContent(/^Hello there\.$/);
    act(() => {
      vi.advanceTimersByTime(1000);
    });
    expect(polite()).toHaveTextContent(/^How are you\?$/);
    rerender(
      <Thread
        messages={[user0, reply('Hello there. How are you? I am well.', 'done')]}
        status="idle"
      />,
    );
    expect(polite()).toHaveTextContent('I am well. Response complete');
  });

  it('does not read out history, and announces conversation errors assertively', () => {
    const { rerender } = render(<Thread messages={list(4)} status="idle" />);
    const polite = document.querySelector('.mn-ai-thread__announcer[role="status"]')!;
    const alert = document.querySelector('.mn-ai-thread__announcer[role="alert"]')!;
    expect(polite).toHaveTextContent('');
    rerender(
      <Thread
        messages={list(4)}
        status="error"
        error={{ code: 'network', message: 'The connection was lost.', retryable: true }}
      />,
    );
    expect(alert).toHaveTextContent('Error: The connection was lost.');
  });

  it('with announce={false} it renders no announcer and lets messages announce', () => {
    const renderMessage = vi.fn((m: AiMessage) => <p key={m.id}>{m.id}</p>);
    render(<Thread messages={list(2)} announce={false} renderMessage={renderMessage} />);
    expect(document.querySelector('.mn-ai-thread__announcer')).toBeNull();
    expect(renderMessage).toHaveBeenCalledWith(
      expect.objectContaining({ id: 'm1' }),
      expect.objectContaining({ announce: true, isLast: true, index: 1 }),
    );
  });

  it('works end to end with useAiConversation and mockTransport', async () => {
    const transport = mockTransport({
      reply: [
        { type: 'agent', agent: { name: 'Helper' } },
        ...textDeltas('Sure. Here is a plan.'),
        { type: 'done' },
      ],
      delayMs: 5,
    });
    function Chat() {
      const c = useAiConversation({ transport });
      return (
        <>
          <AiThread
            messages={c.messages}
            status={c.status}
            error={c.error}
            onStop={c.stop}
            messageProps={{ agent: c.agent, onRegenerate: (m) => c.regenerate(m.id) }}
          />
          <button type="button" onClick={() => c.send('Make a plan')}>
            Send
          </button>
        </>
      );
    }
    render(<Chat />);
    await userEvent.setup().click(screen.getByRole('button', { name: 'Send' }));
    await waitFor(() =>
      expect(document.querySelector('.mn-ai-thread__announcer[role="status"]')).toHaveTextContent(
        'Response complete',
      ),
    );
    expect(screen.getAllByRole('article')).toHaveLength(2);
    expect(screen.getByText('Helper')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Regenerate' })).toBeInTheDocument();
  });

  it('renders on the server', () => {
    const html = renderToString(<Thread messages={list(3)} status="streaming" />);
    expect(html).toContain('mn-ai-thread');
    expect(html).toContain('role="log"');
  });
});
