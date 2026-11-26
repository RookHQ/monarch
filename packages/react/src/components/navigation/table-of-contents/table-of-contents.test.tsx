import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { act, createRef, useRef } from 'react';
import { fireEvent, render, renderHook, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import {
  TableOfContents,
  collectHeadings,
  useScrollSpy,
  type TableOfContentsItem,
} from './index.js';

type Callback = (entries: Array<{ target: Element; isIntersecting: boolean }>) => void;
let observers: Array<{ cb: Callback; targets: Element[]; options: IntersectionObserverInit }> = [];

class MockIntersectionObserver {
  cb: Callback;
  targets: Element[] = [];
  options: IntersectionObserverInit;
  constructor(cb: Callback, options: IntersectionObserverInit) {
    this.cb = cb;
    this.options = options;
    observers.push(this);
  }
  observe(el: Element) {
    this.targets.push(el);
  }
  unobserve() {}
  disconnect() {
    observers = observers.filter((o) => o !== this);
  }
}

/** Reports which headings are inside the observed zone. */
function intersect(states: Record<string, boolean>) {
  act(() => {
    for (const o of observers)
      o.cb(
        o.targets
          .filter((t) => t.id in states)
          .map((t) => ({ target: t, isIntersecting: states[t.id] as boolean })),
      );
  });
}

const ITEMS: TableOfContentsItem[] = [
  { id: 'intro', label: 'Introduction', level: 2 },
  { id: 'setup', label: 'Setup', level: 2 },
  { id: 'setup-cli', label: 'Command line', level: 3 },
  { id: 'faq', label: 'Questions', level: 2 },
];

function Page({ toc }: { toc: React.ReactNode }) {
  return (
    <>
      {toc}
      <main>
        <h2 id="intro">Introduction</h2>
        <p>Text</p>
        <h2 id="setup">Setup</h2>
        <h3 id="setup-cli">Command line</h3>
        <h2 id="faq">Questions</h2>
      </main>
    </>
  );
}

beforeEach(() => {
  observers = [];
  vi.stubGlobal('IntersectionObserver', MockIntersectionObserver);
});
afterEach(() => {
  vi.unstubAllGlobals();
  vi.restoreAllMocks();
});

describe('collectHeadings', () => {
  it('reads headings with ids and their levels', () => {
    const root = document.createElement('div');
    root.innerHTML = '<h2 id="a"> A </h2><h3 id="b">B</h3><h2>No id</h2><h4 id="c">C</h4>';
    expect(collectHeadings(root)).toEqual([
      { id: 'a', label: 'A', level: 2 },
      { id: 'b', label: 'B', level: 3 },
    ]);
    expect(collectHeadings(root, 'h4[id]')).toEqual([{ id: 'c', label: 'C', level: 4 }]);
  });
});

describe('TableOfContents', () => {
  it('renders a titled navigation with nested links', async () => {
    render(<Page toc={<TableOfContents items={ITEMS} />} />);
    const nav = screen.getByRole('navigation', { name: 'On this page' });
    expect(nav).toHaveClass('mn-table-of-contents');
    const cli = screen.getByRole('link', { name: 'Command line' });
    expect(cli).toHaveAttribute('href', '#setup-cli');
    expect(cli.closest('ul')?.closest('li')).toHaveTextContent(/^Setup/);
    await expectNoA11yViolations();
  });

  it('marks the section being read with aria-current as headings scroll', async () => {
    const onActiveChange = vi.fn();
    render(
      <Page toc={<TableOfContents items={ITEMS} offset={64} onActiveChange={onActiveChange} />} />,
    );
    expect(observers).toHaveLength(1);
    expect(observers[0]?.options.rootMargin).toBe('-64px 0px -60% 0px');
    intersect({ intro: true });
    expect(screen.getByRole('link', { name: 'Introduction' })).toHaveAttribute(
      'aria-current',
      'location',
    );
    intersect({ intro: false, setup: true, 'setup-cli': true });
    expect(screen.getByRole('link', { name: 'Setup' })).toHaveAttribute('aria-current', 'location');
    expect(screen.getByRole('link', { name: 'Introduction' })).not.toHaveAttribute('aria-current');
    expect(onActiveChange).toHaveBeenLastCalledWith('setup');
  });

  it('falls back to the last heading scrolled past when none is in view', () => {
    render(<Page toc={<TableOfContents items={ITEMS} />} />);
    vi.spyOn(HTMLElement.prototype, 'getBoundingClientRect').mockImplementation(function (
      this: HTMLElement,
    ) {
      const top = { intro: -900, setup: -400, 'setup-cli': -200, faq: 600 }[this.id] ?? 0;
      return {
        top,
        bottom: top + 20,
        left: 0,
        right: 0,
        width: 0,
        height: 20,
        x: 0,
        y: top,
        toJSON() {},
      };
    });
    intersect({ intro: false, setup: false, 'setup-cli': false, faq: false });
    expect(screen.getByRole('link', { name: 'Command line' })).toHaveAttribute(
      'aria-current',
      'location',
    );
  });

  it('scrolls to and focuses the heading on click, updating the hash', async () => {
    const replace = vi.spyOn(history, 'replaceState');
    render(<Page toc={<TableOfContents items={ITEMS} />} />);
    const user = userEvent.setup();
    await user.tab();
    await user.tab();
    await user.tab();
    expect(screen.getByRole('link', { name: 'Command line' })).toHaveFocus();
    await user.keyboard('{Enter}');
    const heading = screen.getByRole('heading', { name: 'Command line' });
    expect(heading).toHaveFocus();
    expect(heading).toHaveAttribute('tabindex', '-1');
    expect(replace).toHaveBeenCalledWith(history.state, '', '#setup-cli');
    expect(screen.getByRole('link', { name: 'Command line' })).toHaveAttribute(
      'aria-current',
      'location',
    );
  });

  it('leaves modified clicks to the browser so an entry can open in a new tab', () => {
    render(<Page toc={<TableOfContents items={ITEMS} />} />);
    const link = screen.getByRole('link', { name: 'Command line' });
    // fireEvent returns false when the handler called preventDefault.
    expect(fireEvent.click(link, { metaKey: true })).toBe(true);
    expect(fireEvent.click(link, { ctrlKey: true })).toBe(true);
    expect(screen.getByRole('heading', { name: 'Command line' })).not.toHaveFocus();
    expect(fireEvent.click(link)).toBe(false);
    expect(screen.getByRole('heading', { name: 'Command line' })).toHaveFocus();
  });

  it('collects entries from contentRef and follows changes; translates and controls', async () => {
    function Auto({ extra }: { extra: boolean }) {
      const content = useRef<HTMLDivElement>(null);
      return (
        <>
          <TableOfContents
            contentRef={content}
            activeId="b"
            updateHash={false}
            messages={{ title: 'Contents' }}
          />
          <div ref={content}>
            <h2 id="a">Alpha</h2>
            <h2 id="b">Bravo</h2>
            {extra ? <h3 id="c">Charlie</h3> : null}
          </div>
        </>
      );
    }
    const replace = vi.spyOn(history, 'replaceState');
    const { rerender } = render(<Auto extra={false} />);
    expect(await screen.findByRole('link', { name: 'Alpha' })).toBeInTheDocument();
    expect(screen.getByRole('navigation', { name: 'Contents' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Bravo' })).toHaveAttribute('aria-current', 'location');
    rerender(<Auto extra />);
    expect(await screen.findByRole('link', { name: 'Charlie' })).toBeInTheDocument();
    await userEvent.setup().click(screen.getByRole('link', { name: 'Alpha' }));
    expect(replace).not.toHaveBeenCalled();
  });

  it('hides the title but keeps the name, and forwards its ref', () => {
    const ref = createRef<HTMLElement>();
    render(<TableOfContents ref={ref} items={ITEMS} showTitle={false} />);
    expect(screen.getByRole('navigation', { name: 'On this page' })).toBe(ref.current);
    expect(screen.queryByText('On this page')).not.toBeInTheDocument();
  });

  it('renders on the server from items', () => {
    const html = renderToString(<TableOfContents items={ITEMS} />);
    expect(html).toContain('mn-table-of-contents');
    expect(html).toContain('href="#setup-cli"');
    expect(html).not.toContain('aria-current');
  });
});

describe('useScrollSpy', () => {
  beforeEach(() => {
    document.body.innerHTML = '<h2 id="one">One</h2><h2 id="two">Two</h2>';
  });

  it('returns the first heading in view', async () => {
    const { result } = renderHook(() => useScrollSpy(['one', 'two']));
    expect(result.current).toBeNull();
    intersect({ two: true });
    await waitFor(() => expect(result.current).toBe('two'));
  });
});
