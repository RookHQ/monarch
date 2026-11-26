import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { act, fireEvent, render, screen } from '@testing-library/react';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { AiScrollAnchor, useAiScrollAnchor } from './index.js';

/** Gives an element controllable scroll metrics (jsdom has no layout). */
function metrics(el: HTMLElement, init: { scrollHeight: number; clientHeight: number }) {
  const m = { ...init, scrollTop: 0 };
  Object.defineProperties(el, {
    scrollHeight: { configurable: true, get: () => m.scrollHeight },
    clientHeight: { configurable: true, get: () => m.clientHeight },
    scrollTop: {
      configurable: true,
      get: () => m.scrollTop,
      set: (v: number) => {
        m.scrollTop = Math.max(0, Math.min(v, m.scrollHeight - m.clientHeight));
      },
    },
  });
  return m;
}

let observers: { cb: ResizeObserverCallback; targets: Element[] }[] = [];
class ResizeObserverMock {
  entry: { cb: ResizeObserverCallback; targets: Element[] };
  constructor(cb: ResizeObserverCallback) {
    this.entry = { cb, targets: [] };
    observers.push(this.entry);
  }
  observe(el: Element) {
    this.entry.targets.push(el);
  }
  unobserve() {}
  disconnect() {
    observers = observers.filter((o) => o !== this.entry);
  }
}
const fireResize = () =>
  act(() => {
    for (const o of observers) o.cb([], o as unknown as ResizeObserver);
  });

const original = globalThis.ResizeObserver;
beforeEach(() => {
  observers = [];
  globalThis.ResizeObserver = ResizeObserverMock as unknown as typeof ResizeObserver;
});
afterEach(() => {
  globalThis.ResizeObserver = original;
});

function Probe({ watch, onChange }: { watch?: unknown; onChange?: (v: boolean) => void }) {
  const { scrollRef, contentRef, atBottom, scrollToBottom } = useAiScrollAnchor({
    watch,
    onAtBottomChange: onChange,
  });
  return (
    <>
      {/* eslint-disable-next-line jsx-a11y/no-noninteractive-tabindex */}
      <div ref={scrollRef} data-testid="scroller" tabIndex={0} aria-label="Log">
        <div ref={contentRef}>content</div>
      </div>
      <output data-testid="state">{atBottom ? 'bottom' : 'away'}</output>
      <button type="button" onClick={() => scrollToBottom()}>
        Bottom
      </button>
    </>
  );
}

describe('useAiScrollAnchor', () => {
  it('pins to the bottom as content grows while at the bottom', () => {
    const { rerender } = render(<Probe watch={0} />);
    const el = screen.getByTestId('scroller');
    const m = metrics(el, { scrollHeight: 1000, clientHeight: 400 });
    rerender(<Probe watch={1} />);
    expect(m.scrollTop).toBe(600);
    m.scrollHeight = 1300;
    fireResize();
    expect(m.scrollTop).toBe(900);
    expect(screen.getByTestId('state')).toHaveTextContent('bottom');
  });

  it('releases the pin when the person scrolls up, and keeps their position', () => {
    const onChange = vi.fn();
    const { rerender } = render(<Probe watch={0} onChange={onChange} />);
    const el = screen.getByTestId('scroller');
    const m = metrics(el, { scrollHeight: 1000, clientHeight: 400 });
    rerender(<Probe watch={1} onChange={onChange} />);
    m.scrollTop = 200;
    fireEvent.scroll(el);
    expect(screen.getByTestId('state')).toHaveTextContent('away');
    expect(onChange).toHaveBeenLastCalledWith(false);
    m.scrollHeight = 1500;
    fireResize();
    rerender(<Probe watch={2} onChange={onChange} />);
    expect(m.scrollTop).toBe(200);
  });

  it('a small scroll within the threshold still counts as the bottom', () => {
    const { rerender } = render(<Probe watch={0} />);
    const el = screen.getByTestId('scroller');
    const m = metrics(el, { scrollHeight: 1000, clientHeight: 400 });
    rerender(<Probe watch={1} />);
    m.scrollTop = 570;
    fireEvent.scroll(el);
    expect(screen.getByTestId('state')).toHaveTextContent('bottom');
    m.scrollHeight = 1100;
    fireResize();
    expect(m.scrollTop).toBe(700);
  });

  it('scrollToBottom re-pins without moving focus, instantly under reduced motion', () => {
    const matchMedia = vi
      .spyOn(window, 'matchMedia')
      .mockImplementation(
        (q: string) => ({ matches: q.includes('reduce'), media: q }) as MediaQueryList,
      );
    const scrollTo = vi.fn();
    const { rerender } = render(<Probe watch={0} />);
    const el = screen.getByTestId('scroller');
    const m = metrics(el, { scrollHeight: 1000, clientHeight: 400 });
    el.scrollTo = scrollTo as unknown as typeof el.scrollTo;
    rerender(<Probe watch={1} />);
    m.scrollTop = 0;
    fireEvent.scroll(el);
    el.focus();
    fireEvent.click(screen.getByRole('button', { name: 'Bottom' }));
    expect(scrollTo).toHaveBeenCalledWith({ top: 1000, behavior: 'auto' });
    expect(screen.getByTestId('state')).toHaveTextContent('bottom');
    expect(el).toHaveFocus();
    matchMedia.mockRestore();
  });
});

describe('AiScrollAnchor', () => {
  it('renders a headless scroller with a content element and forwards props', async () => {
    render(
      <AiScrollAnchor data-testid="root" tabIndex={0} aria-label="Answer">
        {({ atBottom }) => <p>{atBottom ? 'At the end' : 'Earlier'}</p>}
      </AiScrollAnchor>,
    );
    const root = screen.getByTestId('root');
    expect(root).toHaveClass('mn-ai-scroll-anchor');
    expect(root).toHaveAttribute('data-at-bottom');
    expect(root.firstElementChild).toHaveClass('mn-ai-scroll-anchor__content');
    expect(screen.getByText('At the end')).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('renders on the server', () => {
    expect(renderToString(<AiScrollAnchor watch={1}>Hello</AiScrollAnchor>)).toContain(
      'mn-ai-scroll-anchor',
    );
  });
});
