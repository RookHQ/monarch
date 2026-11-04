import { createRef } from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { act, fireEvent, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import {
  formatMessageDay,
  MessageTimeline,
  shouldGroupMessages,
  type MessageTimelineHandle,
  type MessageTimelineItem,
} from './index.js';

type Msg = MessageTimelineItem & { text: string };

const ROW = 50;
const VIEW = 300;
const scrollTops = new WeakMap<Element, number>();

/** jsdom has no layout: give the scroller a 300px viewport and 50px per message. */
function fakeLayout() {
  const isScroller = (el: Element) => el.classList.contains('mn-message-timeline__scroller');
  const height = (el: Element) => el.querySelectorAll('[data-message-id]').length * ROW;
  vi.spyOn(HTMLElement.prototype, 'scrollHeight', 'get').mockImplementation(function (
    this: HTMLElement,
  ) {
    return isScroller(this) ? Math.max(height(this), VIEW) : 0;
  });
  vi.spyOn(HTMLElement.prototype, 'clientHeight', 'get').mockImplementation(function (
    this: HTMLElement,
  ) {
    return isScroller(this) ? VIEW : 0;
  });
  vi.spyOn(Element.prototype, 'scrollTop', 'get').mockImplementation(function (this: Element) {
    return scrollTops.get(this) ?? 0;
  });
  vi.spyOn(Element.prototype, 'scrollTop', 'set').mockImplementation(function (
    this: Element,
    v: number,
  ) {
    const max = Math.max(0, this.scrollHeight - this.clientHeight);
    scrollTops.set(this, Math.min(Math.max(0, v), max));
  });
}

const minute = 60_000;
const base = Date.parse('2026-03-02T09:00:00Z');
function msgs(from: number, to: number, author = (i: number) => (i % 3 === 0 ? 'a' : 'b')): Msg[] {
  const out: Msg[] = [];
  for (let i = from; i < to; i++)
    out.push({ id: `m${i}`, time: base + i * minute, authorId: author(i), text: `Message ${i}` });
  return out;
}

const renderItem = (item: Msg, ctx: { grouped: boolean }) => (
  <p data-grouped={ctx.grouped || undefined}>{item.text}</p>
);
const log = () => screen.getByRole('log');

beforeEach(() => fakeLayout());
afterEach(() => vi.restoreAllMocks());

describe('MessageTimeline', () => {
  it('is a polite log with day separators, an unread divider and grouping', async () => {
    const items: Msg[] = [
      { id: '1', time: '2026-03-01T10:00:00Z', authorId: 'a', text: 'Yesterday one' },
      { id: '2', time: '2026-03-02T09:00:00Z', authorId: 'a', text: 'Today one' },
      { id: '3', time: '2026-03-02T09:01:00Z', authorId: 'a', text: 'Today two' },
      { id: '4', time: '2026-03-02T09:02:00Z', authorId: 'a', text: 'Unread' },
      { id: '5', time: '2026-03-02T09:30:00Z', authorId: 'a', text: 'Later' },
    ];
    render(
      <MessageTimeline
        items={items}
        renderItem={renderItem}
        firstUnreadId="4"
        now="2026-03-02T12:00:00Z"
        timeZone="UTC"
      />,
    );
    expect(log()).toHaveAttribute('aria-live', 'polite');
    expect(log()).toHaveAttribute('aria-relevant', 'additions');
    expect(log()).toHaveAccessibleName('Messages');
    const separators = screen.getAllByRole('separator');
    expect(separators.map((s) => s.getAttribute('aria-label'))).toEqual([
      'Yesterday',
      'Today',
      'New messages',
    ]);
    // A new day, the unread divider and a 28-minute gap all start a new group.
    expect(screen.getByText('Today one')).not.toHaveAttribute('data-grouped');
    expect(screen.getByText('Today two')).toHaveAttribute('data-grouped');
    expect(screen.getByText('Unread')).not.toHaveAttribute('data-grouped');
    expect(screen.getByText('Later')).not.toHaveAttribute('data-grouped');
    await expectNoA11yViolations();
  });

  it('starts at the bottom and follows new messages while there', () => {
    const { rerender } = render(<MessageTimeline items={msgs(0, 10)} renderItem={renderItem} />);
    expect(log().scrollTop).toBe(10 * ROW - VIEW);
    rerender(<MessageTimeline items={msgs(0, 12)} renderItem={renderItem} />);
    expect(log().scrollTop).toBe(12 * ROW - VIEW);
  });

  it('keeps the view still when older messages are prepended', () => {
    const { rerender } = render(
      <MessageTimeline
        items={msgs(10, 30)}
        renderItem={renderItem}
        hasOlder
        onLoadOlder={() => {}}
      />,
    );
    log().scrollTop = 260; // reading mid-history, just below the load threshold
    fireEvent.scroll(log());
    rerender(
      <MessageTimeline
        items={msgs(5, 30)}
        renderItem={renderItem}
        hasOlder
        onLoadOlder={() => {}}
      />,
    );
    expect(log().scrollTop).toBe(260 + 5 * ROW);
    expect(screen.getByText('Message 10')).toBeInTheDocument();
  });

  it('loads older history near the top, once per load, and from the button', async () => {
    const onLoadOlder = vi.fn();
    const { rerender } = render(
      <MessageTimeline
        items={msgs(10, 30)}
        renderItem={renderItem}
        hasOlder
        onLoadOlder={onLoadOlder}
      />,
    );
    expect(onLoadOlder).not.toHaveBeenCalled();
    log().scrollTop = 100;
    fireEvent.scroll(log());
    fireEvent.scroll(log());
    expect(onLoadOlder).toHaveBeenCalledTimes(1);
    rerender(
      <MessageTimeline
        items={msgs(10, 30)}
        renderItem={renderItem}
        hasOlder
        loadingOlder
        onLoadOlder={onLoadOlder}
      />,
    );
    expect(log()).toHaveAttribute('aria-busy', 'true');
    expect(screen.getByRole('button', { name: /Loading older messages/ })).toBeInTheDocument();
    rerender(
      <MessageTimeline
        items={msgs(0, 30)}
        renderItem={renderItem}
        hasOlder
        onLoadOlder={onLoadOlder}
      />,
    );
    await userEvent.click(screen.getByRole('button', { name: 'Load older messages' }));
    expect(onLoadOlder).toHaveBeenCalledTimes(2);
  });

  it('loads older pages until the view is filled', () => {
    const onLoadOlder = vi.fn();
    render(
      <MessageTimeline
        items={msgs(0, 2)}
        renderItem={renderItem}
        hasOlder
        onLoadOlder={onLoadOlder}
      />,
    );
    expect(onLoadOlder).toHaveBeenCalledTimes(1);
  });

  it('shows Retry after a failed older load and pauses scroll loading', async () => {
    const onLoadOlder = vi.fn();
    const onRetryOlder = vi.fn();
    render(
      <MessageTimeline
        items={msgs(10, 30)}
        renderItem={renderItem}
        hasOlder
        olderError
        onLoadOlder={onLoadOlder}
        onRetryOlder={onRetryOlder}
      />,
    );
    log().scrollTop = 0;
    fireEvent.scroll(log());
    expect(onLoadOlder).not.toHaveBeenCalled();
    expect(screen.getByText('Couldn’t load older messages.')).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: 'Retry' }));
    expect(onRetryOlder).toHaveBeenCalledOnce();
  });

  it('counts messages that arrive while scrolled up and jumps to the latest', async () => {
    const onAtBottomChange = vi.fn();
    const { rerender } = render(
      <MessageTimeline
        items={msgs(0, 20)}
        renderItem={renderItem}
        onAtBottomChange={onAtBottomChange}
      />,
    );
    expect(screen.queryByRole('button', { name: /latest|new message/ })).toBeNull();
    log().scrollTop = 400;
    fireEvent.scroll(log());
    expect(onAtBottomChange).toHaveBeenLastCalledWith(false);
    expect(screen.getByRole('button', { name: 'Jump to latest' })).toBeInTheDocument();
    rerender(
      <MessageTimeline
        items={msgs(0, 22)}
        renderItem={renderItem}
        onAtBottomChange={onAtBottomChange}
      />,
    );
    expect(log().scrollTop).toBe(400);
    const jump = screen.getByRole('button', { name: '2 new messages' });
    await userEvent.click(jump);
    expect(log().scrollTop).toBe(22 * ROW - VIEW);
    fireEvent.scroll(log());
    expect(onAtBottomChange).toHaveBeenLastCalledWith(true);
    expect(screen.queryByRole('button', { name: /new messages|latest/ })).toBeNull();
    expect(log()).toHaveFocus();
  });

  it('does not follow when stickToBottom is off', () => {
    const { rerender } = render(
      <MessageTimeline items={msgs(0, 10)} renderItem={renderItem} stickToBottom={false} />,
    );
    const before = log().scrollTop;
    rerender(<MessageTimeline items={msgs(0, 12)} renderItem={renderItem} stickToBottom={false} />);
    expect(log().scrollTop).toBe(before);
  });

  it('keeps typing status outside the log', () => {
    render(<MessageTimeline items={msgs(0, 3)} renderItem={renderItem} status="Ana is typing…" />);
    const status = screen.getByRole('status');
    expect(status).toHaveTextContent('Ana is typing…');
    expect(log()).not.toContainElement(status);
  });

  it('header, footer, empty, loading and error states', () => {
    const { rerender } = render(
      <MessageTimeline
        items={msgs(0, 3)}
        renderItem={renderItem}
        header={<p>Start of #design</p>}
        footer={<p>Sending…</p>}
      />,
    );
    expect(screen.getByText('Start of #design')).toBeInTheDocument();
    expect(screen.getByText('Sending…')).toBeInTheDocument();
    rerender(<MessageTimeline items={[]} renderItem={renderItem} empty={<p>No messages yet</p>} />);
    expect(screen.getByText('No messages yet')).toBeInTheDocument();
    expect(screen.queryByRole('separator')).toBeNull();
    rerender(<MessageTimeline items={[]} renderItem={renderItem} loading />);
    expect(screen.getByRole('img', { name: 'Loading messages' })).toBeInTheDocument();
    expect(log()).toHaveAttribute('aria-busy', 'true');
    rerender(<MessageTimeline items={msgs(0, 3)} renderItem={renderItem} error={<p>Failed</p>} />);
    expect(screen.getByText('Failed')).toBeInTheDocument();
    expect(screen.queryByText('Message 0')).toBeNull();
  });

  it('scrolls to and highlights a deep-linked message', () => {
    const spy = vi.spyOn(HTMLElement.prototype, 'scrollIntoView');
    const handle = createRef<MessageTimelineHandle>();
    render(
      <MessageTimeline
        items={msgs(0, 20)}
        renderItem={renderItem}
        scrollToId="m4"
        highlightedId="m4"
        handleRef={handle}
      />,
    );
    const target = screen.getByText('Message 4').parentElement!;
    expect(target).toHaveAttribute('data-highlighted');
    expect(spy.mock.contexts).toContain(target);
    expect(handle.current!.scrollToItem('missing')).toBe(false);
    act(() => handle.current!.scrollToBottom('auto'));
    expect(log().scrollTop).toBe(20 * ROW - VIEW);
  });

  it('log is a focusable scroll region (keyboard scrolling)', async () => {
    render(<MessageTimeline items={msgs(0, 3)} renderItem={renderItem} />);
    await userEvent.tab();
    expect(log()).toHaveFocus();
  });

  it('translates its built-in text', () => {
    render(
      <MessageTimeline
        items={msgs(0, 2)}
        renderItem={renderItem}
        firstUnreadId="m1"
        now={base}
        timeZone="UTC"
        messages={{ label: 'Mensajes', newMessages: 'Nuevos', today: 'Hoy' }}
      />,
    );
    expect(log()).toHaveAccessibleName('Mensajes');
    expect(screen.getByRole('separator', { name: 'Hoy' })).toBeInTheDocument();
    expect(screen.getByRole('separator', { name: 'Nuevos' })).toBeInTheDocument();
  });

  it('renders on the server', () => {
    vi.restoreAllMocks();
    const html = renderToString(
      <MessageTimeline items={msgs(0, 3)} renderItem={renderItem} now={base} timeZone="UTC" />,
    );
    expect(html).toContain('role="log"');
    expect(html).toContain('Message 2');
  });
});

describe('shouldGroupMessages / formatMessageDay', () => {
  it('groups the same author within the window on the same day', () => {
    const a = { id: '1', time: '2026-03-02T09:00:00Z', authorId: 'a' };
    expect(shouldGroupMessages(a, { id: '2', time: '2026-03-02T09:04:00Z', authorId: 'a' })).toBe(
      true,
    );
    expect(shouldGroupMessages(a, { id: '2', time: '2026-03-02T09:06:00Z', authorId: 'a' })).toBe(
      false,
    );
    expect(shouldGroupMessages(a, { id: '2', time: '2026-03-02T09:01:00Z', authorId: 'b' })).toBe(
      false,
    );
    expect(shouldGroupMessages(a, { id: '2', time: '2026-03-02T09:01:00Z' })).toBe(false);
    expect(shouldGroupMessages(undefined, a)).toBe(false);
    expect(
      shouldGroupMessages(
        { id: '1', time: '2026-03-01T23:59:00Z', authorId: 'a' },
        { id: '2', time: '2026-03-02T00:01:00Z', authorId: 'a' },
        { timeZone: 'UTC' },
      ),
    ).toBe(false);
  });

  it('formats days relative to now', () => {
    const now = '2026-03-02T12:00:00Z';
    const o = { now, timeZone: 'UTC', locale: 'en-GB' };
    expect(formatMessageDay('2026-03-02T01:00:00Z', o)).toBe('Today');
    expect(formatMessageDay('2026-03-01T23:00:00Z', o)).toBe('Yesterday');
    expect(formatMessageDay('2026-02-20T10:00:00Z', o)).toMatch(/^Friday,? 20 February$/);
    expect(formatMessageDay('2025-02-20T10:00:00Z', o)).toMatch(/^Thursday,? 20 February 2025$/);
  });
});
