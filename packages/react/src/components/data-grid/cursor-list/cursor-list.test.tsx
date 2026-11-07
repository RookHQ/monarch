import { useState } from 'react';
import { describe, expect, it, vi } from 'vitest';
import { act, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { CursorList } from './index.js';

interface Row {
  id: string;
  name: string;
}
const page = (from: number, n: number): Row[] =>
  Array.from({ length: n }, (_, i) => ({ id: `r${from + i}`, name: `Request ${from + i}` }));

function Demo({ pages = 3 }: { pages?: number }) {
  const [items, setItems] = useState<Row[]>(page(1, 2));
  const [loading, setLoading] = useState(false);
  const [loaded, setLoaded] = useState(1);
  return (
    <CursorList
      label="Requests"
      items={items}
      getKey={(r) => r.id}
      renderItem={(r) => <a href={`#${r.id}`}>{r.name}</a>}
      hasMore={loaded < pages}
      loading={loading}
      totalCount={pages * 2}
      noun={{ one: 'request', other: 'requests' }}
      onLoadMore={() => {
        setLoading(true);
        setTimeout(() => {
          setItems((prev) => [...prev, ...page(prev.length + 1, 2)]);
          setLoaded((n) => n + 1);
          setLoading(false);
        }, 10);
      }}
    />
  );
}

describe('CursorList', () => {
  it('renders a named list with a counted footer', async () => {
    render(<Demo />);
    const list = screen.getByRole('list', { name: 'Requests' });
    expect(list.closest('.mn-cursor-list')).toHaveAttribute('data-state', 'ready');
    expect(screen.getAllByRole('listitem')).toHaveLength(2);
    expect(screen.getByText('Showing 2 of 6 requests')).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('loads by keyboard and moves focus to the first new item', async () => {
    const user = userEvent.setup();
    render(<Demo pages={2} />);
    await user.tab();
    await user.tab();
    await user.tab();
    expect(screen.getByRole('button', { name: 'Load more' })).toHaveFocus();
    await user.keyboard('{Enter}');
    await act(() => new Promise((r) => setTimeout(r, 20)));
    const items = screen.getAllByRole('listitem');
    expect(items).toHaveLength(4);
    expect(items[2]).toHaveFocus();
    expect(screen.getByRole('status')).toHaveTextContent('2 more requests loaded.');
    expect(screen.getByText('That’s everything')).toBeInTheDocument();
    await user.tab();
    expect(screen.getByRole('link', { name: 'Request 3' })).toHaveFocus();
    expect(items[2]).not.toHaveAttribute('tabindex');
  });

  it('does not move focus when loading was not started from the footer', async () => {
    const onLoadMore = vi.fn();
    const { rerender } = render(
      <CursorList
        label="L"
        items={page(1, 1)}
        getKey={(r) => r.id}
        renderItem={(r) => r.name}
        hasMore
        onLoadMore={onLoadMore}
      />,
    );
    screen.getByRole('button', { name: 'Load more' }).click();
    expect(onLoadMore).toHaveBeenCalledTimes(1);
    rerender(
      <CursorList
        label="L"
        items={page(1, 2)}
        getKey={(r) => r.id}
        renderItem={(r) => r.name}
        hasMore
        onLoadMore={onLoadMore}
      />,
    );
    expect(document.body).toHaveFocus();
  });

  it('shows first-load, empty and error states', async () => {
    const user = userEvent.setup();
    const onRetry = vi.fn();
    const base = {
      label: 'Keys',
      getKey: (r: Row) => r.id,
      renderItem: (r: Row) => r.name,
      onLoadMore: () => {},
    };
    const { rerender, container } = render(<CursorList {...base} items={[]} hasMore loading />);
    expect(container.firstChild).toHaveAttribute('data-state', 'loading');
    expect(container.firstChild).toHaveAttribute('aria-busy', 'true');
    rerender(<CursorList {...base} items={[]} hasMore={false} />);
    expect(screen.getByText('Nothing here yet')).toBeInTheDocument();
    rerender(<CursorList {...base} items={[]} hasMore={false} empty={<p>No keys yet</p>} />);
    expect(screen.getByText('No keys yet')).toBeInTheDocument();
    rerender(<CursorList {...base} items={[]} hasMore error onRetry={onRetry} />);
    expect(screen.getByText('This list could not be loaded')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Retry' }));
    expect(onRetry).toHaveBeenCalled();
    await expectNoA11yViolations();
  });

  it('retries a failed later page', async () => {
    const user = userEvent.setup();
    const onRetry = vi.fn();
    render(
      <CursorList
        label="Logs"
        ordered
        items={page(1, 2)}
        getKey={(r) => r.id}
        renderItem={(r) => r.name}
        hasMore
        error
        onLoadMore={() => {}}
        onRetry={onRetry}
        messages={{ loadMoreError: 'Nope.' }}
      />,
    );
    expect(screen.getByRole('list', { name: 'Logs' }).tagName).toBe('OL');
    expect(screen.getByText('Nope.')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Retry' }));
    expect(onRetry).toHaveBeenCalled();
  });

  it('renders on the server', () => {
    const html = renderToString(
      <CursorList
        label="L"
        items={page(1, 2)}
        getKey={(r) => r.id}
        renderItem={(r) => r.name}
        hasMore
        onLoadMore={() => {}}
      />,
    );
    expect(html).toContain('mn-cursor-list');
    expect(html).toContain('Request 2');
  });
});
