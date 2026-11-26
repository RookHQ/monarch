import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { act, render, screen } from '@testing-library/react';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { Sticky } from './index.js';

type Callback = (entries: Partial<IntersectionObserverEntry>[]) => void;
let observers: { cb: Callback; options?: IntersectionObserverInit; target?: Element }[] = [];

class FakeObserver {
  private record: (typeof observers)[number];
  constructor(cb: Callback, options?: IntersectionObserverInit) {
    this.record = options ? { cb, options } : { cb };
    observers.push(this.record);
  }
  observe(target: Element) {
    this.record.target = target;
  }
  disconnect() {
    observers = observers.filter((o) => o !== this.record);
  }
  unobserve() {}
  takeRecords() {
    return [];
  }
}

const rect = (top: number, bottom = top) => ({ top, bottom }) as DOMRectReadOnly;

function fire(isIntersecting: boolean, top: number, rootTop = 0, rootBottom = 800) {
  act(() => {
    for (const o of observers)
      o.cb([
        {
          isIntersecting,
          boundingClientRect: rect(top),
          rootBounds: rect(rootTop, rootBottom),
        },
      ]);
  });
}

beforeEach(() => {
  observers = [];
  vi.stubGlobal('IntersectionObserver', FakeObserver);
});
afterEach(() => {
  vi.unstubAllGlobals();
});

describe('Sticky', () => {
  it('renders a top sticky element with a hidden marker before it', async () => {
    render(
      <Sticky as="header" aria-label="Page tools" data-testid="root">
        Tools
      </Sticky>,
    );
    const root = screen.getByTestId('root');
    expect(root.tagName).toBe('HEADER');
    expect(root).toHaveClass('mn-sticky');
    expect(root).toHaveAttribute('data-edge', 'top');
    expect(root).toHaveAttribute('data-elevate');
    expect(root).not.toHaveAttribute('data-stuck');
    const marker = root.previousElementSibling;
    expect(marker).toHaveClass('mn-sticky__sentinel');
    expect(marker).toHaveAttribute('aria-hidden', 'true');
    expect(observers[0]?.target).toBe(marker);
    await expectNoA11yViolations();
  });

  it('becomes stuck when the marker scrolls past the top edge, and reports it', () => {
    const onStuckChange = vi.fn();
    render(
      <Sticky data-testid="root" onStuckChange={onStuckChange}>
        Bar
      </Sticky>,
    );
    const root = screen.getByTestId('root');
    fire(false, -20);
    expect(root).toHaveAttribute('data-stuck');
    expect(onStuckChange).toHaveBeenLastCalledWith(true);
    // Same state again: no extra call.
    fire(false, -40);
    expect(onStuckChange).toHaveBeenCalledTimes(1);
    fire(true, 10);
    expect(root).not.toHaveAttribute('data-stuck');
    expect(onStuckChange).toHaveBeenLastCalledWith(false);
  });

  it('is not stuck when the marker is out of view below (not yet reached)', () => {
    render(<Sticky data-testid="root">Bar</Sticky>);
    fire(false, 900);
    expect(screen.getByTestId('root')).not.toHaveAttribute('data-stuck');
  });

  it('sticks to the bottom with an offset, marker after it', () => {
    render(
      <Sticky data-testid="root" edge="bottom" offset={12} elevateWhenStuck={false}>
        Actions
      </Sticky>,
    );
    const root = screen.getByTestId('root');
    expect(root).toHaveAttribute('data-edge', 'bottom');
    expect(root).not.toHaveAttribute('data-elevate');
    expect(root.style.getPropertyValue('--mn-sticky-offset')).toBe('12px');
    expect(root.nextElementSibling).toHaveClass('mn-sticky__sentinel');
    fire(false, 900, 0, 800);
    expect(root).toHaveAttribute('data-stuck');
  });

  it('accepts a CSS length offset', () => {
    render(<Sticky data-testid="root" offset="var(--mn-space-4)" />);
    expect(screen.getByTestId('root').style.getPropertyValue('--mn-sticky-offset')).toBe(
      'var(--mn-space-4)',
    );
  });

  it('does nothing without IntersectionObserver', () => {
    vi.stubGlobal('IntersectionObserver', undefined);
    render(<Sticky data-testid="root">Bar</Sticky>);
    expect(screen.getByTestId('root')).not.toHaveAttribute('data-stuck');
  });

  it('disconnects on unmount and forwards its ref', () => {
    const ref = { current: null as HTMLElement | null };
    const { unmount } = render(<Sticky ref={ref}>Bar</Sticky>);
    expect(ref.current).toHaveClass('mn-sticky');
    expect(observers).toHaveLength(1);
    unmount();
    expect(observers).toHaveLength(0);
  });

  it('renders on the server', () => {
    const html = renderToString(<Sticky>Bar</Sticky>);
    expect(html).toContain('mn-sticky');
    expect(html).toContain('mn-sticky__sentinel');
  });
});
