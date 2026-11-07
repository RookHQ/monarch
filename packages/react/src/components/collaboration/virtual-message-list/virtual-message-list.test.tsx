import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { act, fireEvent, render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { createRef } from 'react';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import {
  VirtualMessageList,
  type VirtualMessageItem,
  type VirtualMessageListHandle,
} from './index.js';

interface Msg extends VirtualMessageItem {
  text: string;
}

const NOW = new Date('2026-05-10T12:00:00Z');

function makeMessages(count: number, startId = 0): Msg[] {
  return Array.from({ length: count }, (_, i) => {
    const n = startId + i;
    return {
      id: `m${n}`,
      // Two per author in a row, a minute apart, all today.
      time: new Date(NOW.getTime() - (count - i) * 60_000),
      authorId: Math.floor(n / 2) % 2 ? 'a' : 'b',
      text: `Message ${n}`,
    };
  });
}

const renderItem = (item: Msg, ctx: { grouped: boolean }) => (
  <p data-grouped={ctx.grouped || undefined}>{item.text}</p>
);

/** jsdom has no layout: give the scroller a height, rows 50px each, and a working scrollTop. */
function mockLayout(viewHeight = 400, rowHeight = 50) {
  const tops = new WeakMap<Element, number>();
  const isScroller = (el: Element) => el.classList.contains('mn-virtual-message-list__scroller');
  const contentHeight = (el: Element) => {
    const rows = el.querySelector<HTMLElement>('.mn-virtual-message-list__rows');
    if (!rows) return 0;
    const pad =
      (parseFloat(rows.style.paddingTop) || 0) + (parseFloat(rows.style.paddingBottom) || 0);
    return pad + rows.querySelectorAll('[data-message-id]').length * rowHeight;
  };
  const spies = [
    vi.spyOn(HTMLElement.prototype, 'clientHeight', 'get').mockImplementation(function (
      this: HTMLElement,
    ) {
      return isScroller(this) ? viewHeight : 0;
    }),
    vi.spyOn(HTMLElement.prototype, 'scrollHeight', 'get').mockImplementation(function (
      this: HTMLElement,
    ) {
      return isScroller(this) ? contentHeight(this) : 0;
    }),
    vi.spyOn(Element.prototype, 'scrollTop', 'get').mockImplementation(function (this: Element) {
      return tops.get(this) ?? 0;
    }),
    vi.spyOn(Element.prototype, 'scrollTop', 'set').mockImplementation(function (
      this: Element,
      v: number,
    ) {
      const max = isScroller(this) ? Math.max(0, contentHeight(this) - viewHeight) : 0;
      tops.set(this, Math.max(0, Math.min(v, max)));
    }),
    vi.spyOn(Element.prototype, 'getBoundingClientRect').mockImplementation(function (
      this: Element,
    ) {
      const h = (this as HTMLElement).dataset?.messageId ? rowHeight : 0;
      return {
        x: 0,
        y: 0,
        top: 0,
        left: 0,
        right: 0,
        bottom: h,
        width: 0,
        height: h,
        toJSON: () => ({}),
      };
    }),
  ];
  return () => spies.forEach((s) => s.mockRestore());
}

describe('VirtualMessageList without layout (first paint, SSR)', () => {
  it('renders the newest rows in a named log with separators and grouping', async () => {
    const ref = createRef<HTMLDivElement>();
    // The first 45 were sent yesterday, the last five today.
    const items = makeMessages(50).map((item, i) =>
      i < 45 ? { ...item, time: new Date((item.time as Date).getTime() - 86_400_000) } : item,
    );
    render(
      <div style={{ height: 400 }}>
        <VirtualMessageList
          ref={ref}
          items={items}
          renderItem={renderItem}
          firstUnreadId="m45"
          initialRenderCount={10}
          now={NOW}
        />
      </div>,
    );
    expect(ref.current).toHaveClass('mn-virtual-message-list');
    const log = screen.getByRole('log', { name: 'Messages' });
    expect(log).toHaveAttribute('tabindex', '0');
    expect(within(log).getAllByText(/^Message \d+$/)).toHaveLength(10);
    expect(screen.queryByText('Message 39')).not.toBeInTheDocument();
    expect(screen.getByText('Message 49')).toBeInTheDocument();
    expect(screen.getByRole('separator', { name: 'Today' })).toBeInTheDocument();
    expect(screen.getByRole('separator', { name: 'New messages' })).toBeInTheDocument();
    // m41 follows m40 by the same author within five minutes.
    expect(screen.getByText('Message 41')).toHaveAttribute('data-grouped', 'true');
    expect(screen.getByText('Message 40')).not.toHaveAttribute('data-grouped');
    const rows = log.querySelector('.mn-virtual-message-list__rows') as HTMLElement;
    expect(rows.style.paddingTop).toBe(`${40 * 64}px`);
    await expectNoA11yViolations();
  });

  it('shows loading, error and empty states', () => {
    const { rerender } = render(<VirtualMessageList items={[]} renderItem={renderItem} loading />);
    expect(screen.getByRole('img', { name: 'Loading messages' })).toBeInTheDocument();
    expect(screen.getByRole('log')).toHaveAttribute('aria-busy', 'true');
    rerender(<VirtualMessageList items={[]} renderItem={renderItem} error={<p>Failed</p>} />);
    expect(screen.getByText('Failed')).toBeInTheDocument();
    rerender(<VirtualMessageList items={[]} renderItem={renderItem} empty={<p>Say hello</p>} />);
    expect(screen.getByText('Say hello')).toBeInTheDocument();
  });

  it('renders on the server', () => {
    const html = renderToString(
      <VirtualMessageList items={makeMessages(30)} renderItem={renderItem} now={NOW} />,
    );
    expect(html).toContain('role="log"');
    expect(html).toContain('Message 29');
    expect(html).not.toContain('Message 9<');
  });
});

describe('VirtualMessageList with layout', () => {
  let restore: () => void;
  beforeEach(() => {
    restore = mockLayout();
  });
  afterEach(() => restore());

  it('windows the rows around the view and follows the bottom', () => {
    const items = makeMessages(200);
    const { rerender } = render(
      <VirtualMessageList
        items={items}
        renderItem={renderItem}
        overscan={{ top: 100, bottom: 100 }}
        now={NOW}
      />,
    );
    const log = screen.getByRole('log');
    const count = log.querySelectorAll('[data-message-id]').length;
    expect(count).toBeGreaterThan(5);
    expect(count).toBeLessThan(30);
    expect(screen.getByText('Message 199')).toBeInTheDocument();
    expect(screen.queryByText('Message 0')).not.toBeInTheDocument();

    rerender(
      <VirtualMessageList
        items={[...items, ...makeMessages(1, 200)]}
        renderItem={renderItem}
        overscan={{ top: 100, bottom: 100 }}
        now={NOW}
      />,
    );
    expect(screen.getByText('Message 200')).toBeInTheDocument();
    expect(screen.getByText('1 new message')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /Jump to latest/ })).not.toBeInTheDocument();
  });

  it('loads older history near the top and offers a jump back down with a count', async () => {
    const user = userEvent.setup();
    const onLoadOlder = vi.fn();
    const onAtBottomChange = vi.fn();
    const items = makeMessages(100);
    const { rerender } = render(
      <VirtualMessageList
        items={items}
        renderItem={renderItem}
        hasOlder
        onLoadOlder={onLoadOlder}
        onAtBottomChange={onAtBottomChange}
        now={NOW}
      />,
    );
    const log = screen.getByRole('log');
    act(() => {
      log.scrollTop = 0;
      fireEvent.scroll(log);
    });
    expect(onLoadOlder).toHaveBeenCalledTimes(1);
    expect(onAtBottomChange).toHaveBeenLastCalledWith(false);
    expect(screen.getByText('Message 0')).toBeInTheDocument();

    rerender(
      <VirtualMessageList
        items={[...items, ...makeMessages(2, 100)]}
        renderItem={renderItem}
        hasOlder
        onLoadOlder={onLoadOlder}
        onAtBottomChange={onAtBottomChange}
        now={NOW}
      />,
    );
    const jump = screen.getByRole('button', { name: '2 new messages' });
    await user.click(jump);
    expect(log).toHaveFocus();
  });

  it('keeps the view still when older messages are prepended', () => {
    const items = makeMessages(100, 100);
    const { rerender } = render(
      <VirtualMessageList items={items} renderItem={renderItem} now={NOW} />,
    );
    const log = screen.getByRole('log');
    act(() => {
      log.scrollTop = 1000;
      fireEvent.scroll(log);
    });
    const anchorId = (log.querySelector('[data-message-id]') as HTMLElement).dataset.messageId;
    const before = log.scrollTop;
    rerender(
      <VirtualMessageList
        items={[...makeMessages(20, 80), ...items]}
        renderItem={renderItem}
        now={NOW}
      />,
    );
    expect(log.scrollTop).toBe(before + 20 * 50);
    expect(log.querySelector(`[data-message-id="${anchorId}"]`)).not.toBeNull();
  });

  it('scrolls to an item through the handle and highlights it', () => {
    const handle = createRef<VirtualMessageListHandle>();
    render(
      <VirtualMessageList
        items={makeMessages(200)}
        renderItem={renderItem}
        handleRef={handle}
        highlightedId="m10"
        now={NOW}
      />,
    );
    expect(handle.current?.scrollToItem('missing')).toBe(false);
    act(() => {
      expect(handle.current?.scrollToItem('m10', 'auto')).toBe(true);
    });
    const row = screen.getByText('Message 10').closest('[data-message-id]');
    expect(row).toHaveAttribute('data-highlighted');
    // Scrolling away from the bottom stops following it and offers the way back.
    expect(screen.getByRole('button', { name: 'Jump to latest' })).toBeInTheDocument();
  });
});
