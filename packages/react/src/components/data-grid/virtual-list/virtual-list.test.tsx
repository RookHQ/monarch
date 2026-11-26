import { afterEach, describe, expect, it, vi } from 'vitest';
import { act, fireEvent, render, renderHook, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { createRef } from 'react';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { triggerResize } from '../../../../test/setup.js';
import { VirtualList, useVirtualizer, type VirtualListHandle } from './index.js';

interface Item {
  id: string;
  name: string;
}

const ITEMS: Item[] = Array.from({ length: 10_000 }, (_, i) => ({
  id: `i${i}`,
  name: `Item ${i}`,
}));

/** jsdom has no layout: give the scroller a height and a working scrollTop. */
function mockLayout(viewHeight = 400) {
  const tops = new WeakMap<Element, number>();
  const isScroller = (el: Element) => el.classList.contains('mn-virtual-list');
  return [
    vi.spyOn(HTMLElement.prototype, 'clientHeight', 'get').mockImplementation(function (
      this: HTMLElement,
    ) {
      return isScroller(this) ? viewHeight : 0;
    }),
    vi.spyOn(Element.prototype, 'scrollTop', 'get').mockImplementation(function (this: Element) {
      return tops.get(this) ?? 0;
    }),
    vi.spyOn(Element.prototype, 'scrollTop', 'set').mockImplementation(function (
      this: Element,
      v: number,
    ) {
      tops.set(this, v);
    }),
  ];
}

afterEach(() => vi.restoreAllMocks());

function List(props: Partial<Parameters<typeof VirtualList<Item>>[0]>) {
  return (
    <VirtualList
      label="Items"
      items={ITEMS}
      getItemKey={(i) => i.id}
      renderItem={(i) => <span>{i.name}</span>}
      height={400}
      {...props}
    />
  );
}

describe('VirtualList', () => {
  it('renders only a window of a long list, with list semantics and positions', async () => {
    render(<List data-testid="root" />);
    const root = screen.getByTestId('root');
    expect(root).toHaveClass('mn-virtual-list');
    expect(root).toHaveAttribute('role', 'group');
    expect(root).toHaveAccessibleName('Items');
    const items = within(screen.getByRole('list')).getAllByRole('listitem');
    expect(items).toHaveLength(20);
    expect(items[0]).toHaveAttribute('aria-setsize', '10000');
    expect(items[0]).toHaveAttribute('aria-posinset', '1');
    await expectNoA11yViolations();
  });

  it('follows the scroll position and is reachable by keyboard', async () => {
    mockLayout(400);
    const user = userEvent.setup();
    render(<List estimatedItemHeight={40} overscan={0} />);
    const root = screen.getByRole('group', { name: 'Items' });
    await user.tab();
    expect(root).toHaveFocus();
    // 400px view / 40px items = 10 items (+1 for the partly visible one).
    expect(screen.getAllByRole('listitem').length).toBeLessThanOrEqual(11);
    act(() => {
      root.scrollTop = 40 * 5000;
      fireEvent.scroll(root);
    });
    expect(screen.getByText('Item 5000')).toBeInTheDocument();
    expect(screen.queryByText('Item 0')).toBeNull();
    expect(screen.getByText('Item 5000').closest('[role="listitem"]')).toHaveAttribute(
      'aria-posinset',
      '5001',
    );
  });

  it('scrolls to an item through the handle', () => {
    mockLayout(400);
    const handle = createRef<VirtualListHandle>();
    render(<List estimatedItemHeight={40} overscan={0} handleRef={handle} />);
    act(() => {
      expect(handle.current?.scrollToKey('i9000', { align: 'start' })).toBe(true);
    });
    expect(screen.getByText('Item 9000')).toBeInTheDocument();
    expect(handle.current?.scrollToKey('missing')).toBe(false);
  });

  it('asks for the next page near the end, once per length', () => {
    mockLayout(400);
    const onEndReached = vi.fn();
    const short = ITEMS.slice(0, 12);
    const { rerender } = render(
      <List items={short} estimatedItemHeight={40} onEndReached={onEndReached} />,
    );
    expect(onEndReached).toHaveBeenCalledTimes(1);
    rerender(<List items={short} estimatedItemHeight={40} onEndReached={onEndReached} />);
    expect(onEndReached).toHaveBeenCalledTimes(1);
    rerender(<List items={short} estimatedItemHeight={40} onEndReached={onEndReached} loading />);
    expect(screen.getByRole('status')).toHaveTextContent('Loading more');
  });

  it('shows the empty content and a loading status without items', async () => {
    const { rerender } = render(<List items={[]} empty={<p>No items</p>} />);
    expect(screen.getByText('No items')).toBeInTheDocument();
    expect(screen.queryByRole('list')).toBeNull();
    rerender(<List items={[]} loading />);
    expect(screen.getByRole('status', { name: 'Loading' })).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('can drop list semantics for items with their own', () => {
    render(<List semantics="none" renderItem={(i) => <article aria-label={i.name} />} />);
    expect(screen.queryByRole('list')).toBeNull();
    expect(screen.getAllByRole('article')).toHaveLength(20);
  });

  it('useVirtualizer keeps requested indexes mounted', () => {
    mockLayout(400);
    const el = document.createElement('div');
    el.className = 'mn-virtual-list';
    const { result } = renderHook(() =>
      useVirtualizer({
        count: 1000,
        getScrollElement: () => el,
        estimateSize: 40,
        overscan: 0,
        keepMounted: [900],
      }),
    );
    const indexes = result.current.items.map((i) => i.index);
    expect(indexes.at(-1)).toBe(900);
    expect(indexes.slice(0, 3)).toEqual([0, 1, 2]);
    expect(result.current.totalSize).toBe(40_000);
    expect(result.current.getOffset(10)).toBe(400);
  });

  it('re-measures the items of the first render when they resize', () => {
    mockLayout(400);
    render(<List estimatedItemHeight={40} overscan={0} />);
    const [first, second] = screen.getAllByRole('listitem');
    expect(second).toHaveStyle({ transform: 'translateY(40px)' });
    vi.spyOn(first as HTMLElement, 'getBoundingClientRect').mockReturnValue({
      height: 100,
    } as DOMRect);
    act(() => triggerResize(first, { height: 100 }));
    expect(screen.getByText('Item 1').closest('[role="listitem"]')).toHaveStyle({
      transform: 'translateY(100px)',
    });
  });

  it('keeps the item that holds focus mounted when it scrolls out of view', async () => {
    mockLayout(400);
    render(
      <List
        estimatedItemHeight={40}
        overscan={0}
        renderItem={(i) => <button type="button">{i.name}</button>}
      />,
    );
    const root = screen.getByRole('group', { name: 'Items' });
    const button = screen.getByRole('button', { name: 'Item 2' });
    act(() => button.focus());
    act(() => {
      root.scrollTop = 40 * 5000;
      fireEvent.scroll(root);
    });
    expect(screen.getByRole('button', { name: 'Item 5000' })).toBeInTheDocument();
    expect(button).toBeInTheDocument();
    expect(button).toHaveFocus();
    expect(screen.queryByRole('button', { name: 'Item 1' })).toBeNull();
    // Once focus leaves the list the item is released.
    act(() => button.blur());
    expect(screen.queryByRole('button', { name: 'Item 2' })).toBeNull();
  });

  it('renders on the server', () => {
    const html = renderToString(<List />);
    expect(html).toContain('mn-virtual-list');
    expect(html).toContain('Item 19');
    expect(html).not.toContain('Item 20<');
  });
});
