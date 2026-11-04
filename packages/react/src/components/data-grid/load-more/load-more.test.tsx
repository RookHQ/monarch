import { useState } from 'react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { act, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { LoadMore, useLoadMore, type LoadMorePage } from './index.js';

type Row = { id: number };

/** A fake cursor API: 3 pages of 2 rows; cursor is the next start index as a string. */
function fakeApi(opts: { failOnce?: string } = {}) {
  let failed = false;
  return vi.fn(async (cursor: string | undefined): Promise<LoadMorePage<Row, string>> => {
    const start = cursor ? Number(cursor) : 0;
    if (opts.failOnce === (cursor ?? 'first') && !failed) {
      failed = true;
      throw new Error('Network error');
    }
    const items = [start, start + 1].map((id) => ({ id }));
    return { items, nextCursor: start + 2 < 6 ? String(start + 2) : null, total: 6 };
  });
}

function List({ fetchPage }: { fetchPage: ReturnType<typeof fakeApi> }) {
  const list = useLoadMore<Row, string>({ fetchPage: (c) => fetchPage(c) });
  return (
    <>
      <ul aria-label="Rows">
        {list.items.map((r) => (
          <li key={r.id}>Row {r.id}</li>
        ))}
      </ul>
      <span data-testid="status">{list.status}</span>
      <button type="button" onClick={list.reset}>
        Reset
      </button>
      <LoadMore {...list.loadMoreProps} noun={{ one: 'row', other: 'rows' }} />
    </>
  );
}

const status = () => screen.getByRole('status');

afterEach(() => {
  vi.unstubAllGlobals();
});

describe('useLoadMore + LoadMore', () => {
  it('loads the first page on mount, then more on click, then reaches the end', async () => {
    const api = fakeApi();
    render(<List fetchPage={api} />);
    expect(await screen.findByText('Row 1')).toBeInTheDocument();
    expect(screen.getByText('Showing 2 of 6 rows')).toBeInTheDocument();
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Load more' }));
    expect(await screen.findByText('Row 3')).toBeInTheDocument();
    expect(status()).toHaveTextContent('2 more rows loaded.');
    await user.click(screen.getByRole('button', { name: 'Load more' }));
    expect(await screen.findByText('Row 5')).toBeInTheDocument();
    expect(screen.getByTestId('status')).toHaveTextContent('done');
    expect(screen.queryByRole('button', { name: 'Load more' })).toBeNull();
    expect(screen.getByText('That’s everything')).toBeInTheDocument();
    expect(status()).toHaveTextContent('2 more rows loaded. That’s everything');
    expect(api.mock.calls.map((c) => c[0])).toEqual([undefined, '2', '4']);
  });

  it('works by keyboard and keeps focus in place at the end of the list', async () => {
    const api = fakeApi();
    render(<List fetchPage={api} />);
    await screen.findByText('Row 1');
    const button = screen.getByRole('button', { name: 'Load more' });
    button.focus();
    const user = userEvent.setup();
    await user.keyboard('{Enter}');
    await screen.findByText('Row 3');
    expect(screen.getByRole('button', { name: 'Load more' })).toHaveFocus();
    await user.keyboard(' ');
    await screen.findByText('Row 5');
    await waitFor(() => expect(screen.getByText('That’s everything')).toHaveFocus());
  });

  it('shows the error, announces it and retries the same cursor', async () => {
    const api = fakeApi({ failOnce: '2' });
    render(<List fetchPage={api} />);
    await screen.findByText('Row 1');
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Load more' }));
    expect(await screen.findByText('Couldn’t load more.', { selector: 'p' })).toBeInTheDocument();
    expect(status()).toHaveTextContent('Couldn’t load more.');
    expect(screen.getByTestId('status')).toHaveTextContent('error');
    await user.click(screen.getByRole('button', { name: 'Retry' }));
    expect(await screen.findByText('Row 3')).toBeInTheDocument();
    expect(api.mock.calls.map((c) => c[0])).toEqual([undefined, '2', '2']);
  });

  it('retries a failed first page', async () => {
    const api = fakeApi({ failOnce: 'first' });
    render(<List fetchPage={api} />);
    const user = userEvent.setup();
    await user.click(await screen.findByRole('button', { name: 'Retry' }));
    expect(await screen.findByText('Row 0')).toBeInTheDocument();
  });

  it('reset reloads from the first page and ignores a stale response', async () => {
    let release: (() => void) | undefined;
    const api = vi.fn(async (cursor: string | undefined): Promise<LoadMorePage<Row, string>> => {
      if (cursor === '2') {
        await new Promise<void>((r) => (release = r));
        return { items: [{ id: 99 }], nextCursor: null };
      }
      return { items: [{ id: 0 }, { id: 1 }], nextCursor: '2' };
    });
    render(<List fetchPage={api as ReturnType<typeof fakeApi>} />);
    await screen.findByText('Row 1');
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Load more' }));
    expect(screen.getByRole('button', { name: 'Loading…' })).toHaveAttribute('aria-busy', 'true');
    await user.click(screen.getByRole('button', { name: 'Reset' }));
    await act(async () => release?.());
    await screen.findByText('Row 1');
    expect(screen.queryByText('Row 99')).toBeNull();
  });

  it('ignores loadMore while a page is loading', async () => {
    const api = fakeApi();
    render(<List fetchPage={api} />);
    await screen.findByText('Row 1');
    const button = screen.getByRole('button', { name: 'Load more' });
    act(() => {
      button.click();
      button.click();
    });
    await screen.findByText('Row 3');
    expect(api).toHaveBeenCalledTimes(2);
  });

  it('starts from initialPage without a request', () => {
    function Seeded() {
      const list = useLoadMore<Row, string>({
        fetchPage: async () => ({ items: [], nextCursor: null }),
        initialPage: { items: [{ id: 7 }], nextCursor: null },
      });
      return <LoadMore {...list.loadMoreProps} />;
    }
    render(<Seeded />);
    expect(screen.getByText('That’s everything')).toBeInTheDocument();
    expect(screen.getByText('Showing 1 item')).toBeInTheDocument();
  });

  it('auto-loads when it scrolls into view, pausing after an error', async () => {
    const observers: Array<{ cb: IntersectionObserverCallback; disconnect: () => void }> = [];
    vi.stubGlobal(
      'IntersectionObserver',
      class {
        cb: IntersectionObserverCallback;
        constructor(cb: IntersectionObserverCallback) {
          this.cb = cb;
          observers.push(this);
        }
        observe() {}
        disconnect() {}
      },
    );
    const onLoadMore = vi.fn();
    const { rerender } = render(<LoadMore hasMore onLoadMore={onLoadMore} autoLoad />);
    act(() => {
      observers.at(-1)!.cb([{ isIntersecting: true } as IntersectionObserverEntry], {} as never);
    });
    expect(onLoadMore).toHaveBeenCalledTimes(1);
    const count = observers.length;
    rerender(<LoadMore hasMore error onLoadMore={onLoadMore} autoLoad />);
    expect(observers.length).toBe(count);
    // The button is always there as the fallback.
    expect(screen.getByRole('button', { name: 'Retry' })).toBeInTheDocument();
  });

  it('is controlled by props when used without the hook', async () => {
    function Controlled() {
      const [n, setN] = useState(10);
      return (
        <LoadMore
          hasMore={n < 30}
          loadedCount={n}
          totalCount={30}
          onLoadMore={() => setN((v) => v + 10)}
          endLabel={null}
        />
      );
    }
    render(<Controlled />);
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Load more' }));
    expect(screen.getByText('Showing 20 of 30 items')).toBeInTheDocument();
    expect(status()).toHaveTextContent('10 more items loaded.');
    await user.click(screen.getByRole('button', { name: 'Load more' }));
    expect(screen.queryByRole('button')).toBeNull();
    expect(screen.queryByText('That’s everything')).toBeNull();
  });

  it('has no axe violations in each state', async () => {
    const { rerender } = render(
      <main>
        <LoadMore hasMore loadedCount={20} totalCount={60} onLoadMore={() => {}} />
      </main>,
    );
    await expectNoA11yViolations();
    rerender(
      <main>
        <LoadMore hasMore loading loadedCount={20} onLoadMore={() => {}} />
      </main>,
    );
    await expectNoA11yViolations();
    rerender(
      <main>
        <LoadMore hasMore error onLoadMore={() => {}} />
      </main>,
    );
    await expectNoA11yViolations();
    rerender(
      <main>
        <LoadMore hasMore={false} loadedCount={60} onLoadMore={() => {}} />
      </main>,
    );
    await expectNoA11yViolations();
  });

  it('renders on the server', () => {
    const html = renderToString(<LoadMore hasMore loadedCount={20} onLoadMore={() => {}} />);
    expect(html).toContain('mn-load-more');
    expect(html).toContain('Load more');
  });
});
