import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { act, fireEvent, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { ScrollArea } from './index.js';

let resizeCallbacks: (() => void)[] = [];
class FakeResizeObserver {
  private cb: () => void;
  constructor(cb: () => void) {
    this.cb = cb;
    resizeCallbacks.push(cb);
  }
  observe() {}
  unobserve() {}
  disconnect() {
    resizeCallbacks = resizeCallbacks.filter((c) => c !== this.cb);
  }
}

function size(
  el: HTMLElement,
  s: Partial<
    Record<
      'scrollHeight' | 'clientHeight' | 'scrollWidth' | 'clientWidth' | 'scrollTop' | 'scrollLeft',
      number
    >
  >,
) {
  for (const [k, v] of Object.entries(s))
    Object.defineProperty(el, k, { configurable: true, writable: true, value: v });
}
const resize = () => act(() => resizeCallbacks.forEach((cb) => cb()));
const viewportOf = (root: HTMLElement) =>
  root.querySelector<HTMLElement>('.mn-scroll-area__viewport')!;

beforeEach(() => {
  resizeCallbacks = [];
  vi.stubGlobal('ResizeObserver', FakeResizeObserver);
});
afterEach(() => vi.unstubAllGlobals());

describe('ScrollArea', () => {
  it('is not a tab stop or a region while content fits', async () => {
    render(
      <ScrollArea label="Activity" data-testid="root">
        <p>Short</p>
      </ScrollArea>,
    );
    const root = screen.getByTestId('root');
    expect(root).toHaveClass('mn-scroll-area');
    expect(root).toHaveAttribute('data-orientation', 'vertical');
    expect(root).not.toHaveAttribute('data-scrollable');
    const vp = viewportOf(root);
    expect(vp).not.toHaveAttribute('tabindex');
    expect(vp).not.toHaveAttribute('role');
    expect(vp).not.toHaveAttribute('aria-label');
    await expectNoA11yViolations();
  });

  it('becomes a focusable, labelled region when content overflows, and back', async () => {
    const user = userEvent.setup();
    const onScrollableChange = vi.fn();
    render(
      <ScrollArea label="Activity" data-testid="root" onScrollableChange={onScrollableChange}>
        <p>Long</p>
      </ScrollArea>,
    );
    const root = screen.getByTestId('root');
    const vp = viewportOf(root);
    size(vp, { scrollHeight: 500, clientHeight: 200, scrollTop: 0 });
    resize();
    expect(root).toHaveAttribute('data-scrollable');
    const region = screen.getByRole('region', { name: 'Activity' });
    expect(region).toBe(vp);
    expect(region).toHaveAttribute('tabindex', '0');
    expect(onScrollableChange).toHaveBeenLastCalledWith(true);
    await user.tab();
    expect(region).toHaveFocus();
    await expectNoA11yViolations();
    size(vp, { scrollHeight: 200 });
    resize();
    expect(root).not.toHaveAttribute('data-scrollable');
    expect(onScrollableChange).toHaveBeenLastCalledWith(false);
  });

  it('marks which edges hide content as it scrolls', () => {
    const onScroll = vi.fn();
    render(
      <ScrollArea label="List" data-testid="root" viewportProps={{ onScroll }}>
        <p>Long</p>
      </ScrollArea>,
    );
    const root = screen.getByTestId('root');
    const vp = viewportOf(root);
    size(vp, { scrollHeight: 500, clientHeight: 200, scrollTop: 0 });
    resize();
    expect(root).not.toHaveAttribute('data-overflow-start');
    expect(root).toHaveAttribute('data-overflow-end');
    size(vp, { scrollTop: 100 });
    fireEvent.scroll(vp);
    expect(onScroll).toHaveBeenCalled();
    expect(root).toHaveAttribute('data-overflow-start');
    expect(root).toHaveAttribute('data-overflow-end');
    size(vp, { scrollTop: 300 });
    fireEvent.scroll(vp);
    expect(root).toHaveAttribute('data-overflow-start');
    expect(root).not.toHaveAttribute('data-overflow-end');
  });

  it('measures the horizontal axis (right-to-left too) and can hide shadows', () => {
    render(
      <ScrollArea label="Columns" orientation="horizontal" shadows={false} data-testid="root">
        <p>Wide</p>
      </ScrollArea>,
    );
    const root = screen.getByTestId('root');
    const vp = viewportOf(root);
    size(vp, {
      scrollWidth: 900,
      clientWidth: 300,
      scrollLeft: -50,
      scrollHeight: 999,
      clientHeight: 10,
    });
    resize();
    expect(root).toHaveAttribute('data-scrollable');
    expect(root).not.toHaveAttribute('data-overflow-start');
    expect(root).not.toHaveAttribute('data-overflow-end');
  });

  it('ignores the other axis', () => {
    render(
      <ScrollArea label="Rows" data-testid="root">
        <p>x</p>
      </ScrollArea>,
    );
    const vp = viewportOf(screen.getByTestId('root'));
    size(vp, { scrollWidth: 900, clientWidth: 300, scrollHeight: 100, clientHeight: 100 });
    resize();
    expect(screen.getByTestId('root')).not.toHaveAttribute('data-scrollable');
  });

  it('sets height variables and forwards the ref to the viewport', () => {
    const ref = { current: null as HTMLDivElement | null };
    render(
      <ScrollArea ref={ref} label="Log" height={240} maxHeight="50vh" data-testid="root">
        x
      </ScrollArea>,
    );
    const root = screen.getByTestId('root');
    expect(root.style.getPropertyValue('--mn-scroll-area-height')).toBe('240px');
    expect(root.style.getPropertyValue('--mn-scroll-area-max-height')).toBe('50vh');
    expect(ref.current).toHaveClass('mn-scroll-area__viewport');
  });

  it('works without ResizeObserver', () => {
    vi.stubGlobal('ResizeObserver', undefined);
    render(<ScrollArea label="Log">x</ScrollArea>);
    expect(document.querySelector('.mn-scroll-area')).toBeInTheDocument();
  });

  it('renders on the server', () => {
    const html = renderToString(<ScrollArea label="Log">Body</ScrollArea>);
    expect(html).toContain('mn-scroll-area__viewport');
    expect(html).not.toContain('tabindex');
  });
});
