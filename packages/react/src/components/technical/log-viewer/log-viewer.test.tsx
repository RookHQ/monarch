import { describe, expect, it, vi } from 'vitest';
import { act, fireEvent, render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { triggerResize } from '../../../../test/setup.js';
import { LogViewer, formatLogTime, type LogLine } from './index.js';

const t0 = new Date(2026, 0, 5, 9, 30, 0, 0).getTime();
const lines: LogLine[] = [
  { id: '1', level: 'debug', message: 'Loading configuration', time: t0 },
  {
    id: '2',
    level: 'info',
    message: 'Server listening on port 8080',
    time: t0 + 15,
    source: 'web',
  },
  { id: '3', level: 'warn', message: 'Slow query took 1200 ms', time: t0 + 1000 },
  { id: '4', level: 'error', message: 'Connection refused by upstream', time: t0 + 2000 },
  { id: '5', message: 'Request completed', time: t0 + 3000 },
];

const log = () => screen.getByRole('region', { name: 'Log' });
const lineItems = () => within(log()).queryAllByRole('listitem');

/** jsdom has no layout: give the viewport a size so scroll maths run. */
function fakeLayout(el: HTMLElement, scrollHeight: number, clientHeight: number) {
  Object.defineProperty(el, 'scrollHeight', { configurable: true, value: scrollHeight });
  Object.defineProperty(el, 'clientHeight', { configurable: true, value: clientHeight });
}

describe('LogViewer', () => {
  it('renders lines with level, time and source in a named, focusable region', async () => {
    render(<LogViewer lines={lines} data-testid="root" />);
    expect(screen.getByTestId('root')).toHaveClass('mn-log-viewer');
    expect(log()).toHaveAttribute('tabindex', '0');
    const items = lineItems();
    expect(items).toHaveLength(5);
    expect(items[1]).toHaveTextContent('09:30:00.015InfowebServer listening on port 8080');
    expect(items[1]).toHaveAttribute('aria-posinset', '2');
    expect(items[1]).toHaveAttribute('aria-setsize', '5');
    expect(items[4]).toHaveAttribute('data-level', 'info');
    expect(screen.getByText('5 lines')).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('filters by level with pressed toggle buttons', async () => {
    const user = userEvent.setup();
    const onLevelsChange = vi.fn();
    render(<LogViewer lines={lines} onLevelsChange={onLevelsChange} />);
    const group = screen.getByRole('group', { name: 'Levels' });
    const debug = within(group).getByRole('button', { name: /Debug/ });
    expect(debug).toHaveAttribute('aria-pressed', 'true');
    await user.click(debug);
    expect(debug).toHaveAttribute('aria-pressed', 'false');
    expect(onLevelsChange).toHaveBeenLastCalledWith(['info', 'warn', 'error']);
    expect(lineItems()).toHaveLength(4);
    expect(screen.getByText('4 of 5 lines')).toBeInTheDocument();
    await user.click(within(group).getByRole('button', { name: /Info/ }));
    await user.click(within(group).getByRole('button', { name: /Warn/ }));
    await user.click(within(group).getByRole('button', { name: /Error/ }));
    expect(screen.getByText('No lines match the filters.')).toBeInTheDocument();
  });

  it('searches with highlighting in filter and highlight modes', async () => {
    const user = userEvent.setup();
    const { rerender } = render(<LogViewer lines={lines} />);
    await user.type(screen.getByRole('searchbox', { name: 'Search log' }), 'conn');
    expect(lineItems()).toHaveLength(1);
    expect(lineItems()[0]!.querySelector('mark')).toHaveTextContent('Conn');
    rerender(<LogViewer lines={lines} searchMode="highlight" />);
    expect(lineItems()).toHaveLength(5);
    expect(log().querySelectorAll('mark')).toHaveLength(1);
  });

  it('pauses following when the reader scrolls up and resumes with End or the jump button', async () => {
    const user = userEvent.setup();
    const onFollowChange = vi.fn();
    const { rerender } = render(<LogViewer lines={lines} onFollowChange={onFollowChange} />);
    const follow = screen.getByRole('button', { name: 'Follow' });
    expect(follow).toHaveAttribute('aria-pressed', 'true');
    const viewport = log();
    fakeLayout(viewport, 1000, 200);
    viewport.scrollTop = 100;
    fireEvent.scroll(viewport);
    expect(onFollowChange).toHaveBeenLastCalledWith(false);
    expect(follow).toHaveAttribute('aria-pressed', 'false');
    expect(screen.getByRole('status')).toHaveTextContent('Following paused');
    rerender(
      <LogViewer
        lines={[...lines, { id: '6', message: 'Next', time: t0 + 4000 }]}
        onFollowChange={onFollowChange}
      />,
    );
    const jump = screen.getByRole('button', { name: '1 new line' });
    await user.click(jump);
    expect(follow).toHaveAttribute('aria-pressed', 'true');
    expect(screen.queryByRole('button', { name: /new line|Jump to latest/ })).toBeNull();
    viewport.focus();
    await user.keyboard('{Home}');
    expect(follow).toHaveAttribute('aria-pressed', 'false');
    await user.keyboard('{End}');
    expect(follow).toHaveAttribute('aria-pressed', 'true');
    expect(screen.getByRole('status')).toHaveTextContent('Following new lines');
  });

  it('toggles wrapping', async () => {
    const user = userEvent.setup();
    const onWrapChange = vi.fn();
    render(<LogViewer lines={lines} onWrapChange={onWrapChange} data-testid="root" />);
    const wrap = screen.getByRole('button', { name: 'Wrap lines' });
    await user.click(wrap);
    expect(wrap).toHaveAttribute('aria-pressed', 'true');
    expect(screen.getByTestId('root')).toHaveAttribute('data-wrap');
    expect(onWrapChange).toHaveBeenCalledWith(true);
  });

  it('renders only a window of a large log', () => {
    const many = Array.from({ length: 10_000 }, (_, i) => ({
      id: String(i),
      message: `Line ${i}`,
    }));
    render(<LogViewer lines={many} defaultFollow={false} />);
    const count = lineItems().length;
    expect(count).toBeGreaterThan(10);
    expect(count).toBeLessThan(100);
    expect(screen.getByText('10,000 lines')).toBeInTheDocument();
  });

  it('shows empty and loading states', () => {
    const { rerender } = render(<LogViewer lines={[]} />);
    expect(screen.getByText('No log lines yet.')).toBeInTheDocument();
    rerender(<LogViewer lines={[]} loading />);
    expect(log()).toHaveAttribute('aria-busy', 'true');
    expect(screen.getByText('Loading log')).toBeInTheDocument();
  });

  it('takes custom messages and a time formatter', () => {
    render(
      <LogViewer
        lines={lines.slice(3, 4)}
        formatTime={(d) => d.toISOString().slice(11, 19)}
        messages={{ label: 'Build output', levelNames: { error: 'ERR' } }}
      />,
    );
    expect(screen.getByRole('region', { name: 'Build output' })).toBeInTheDocument();
    expect(screen.getAllByText('ERR').length).toBeGreaterThan(0);
    expect(formatLogTime(new Date(t0 + 7))).toBe('09:30:00.007');
  });

  it('writes the default times in UTC on the server, whatever the server’s zone', () => {
    // Stand in for a server in another zone: local getters must not be read.
    const spy = vi.spyOn(Date.prototype, 'getHours').mockReturnValue(3);
    try {
      const at = Date.UTC(2026, 0, 5, 14, 30, 0, 15);
      const html = renderToString(
        <LogViewer lines={[{ id: 'a', message: 'Started', time: at }]} />,
      );
      expect(html).toContain('>14:30:00.015</time>');
    } finally {
      spy.mockRestore();
    }
  });

  it('measures the first rows, and stops measuring a row once it has left the window', async () => {
    const user = userEvent.setup();
    render(<LogViewer lines={lines} />);
    const row = lineItems()[0]!;
    const watched = () =>
      [
        ...(globalThis as unknown as { __mnResizeObservers: Set<{ targets: Set<Element> }> })
          .__mnResizeObservers,
      ].some((r) => r.targets.has(row));
    // Rows of the first render are measured too (their refs attach before the observer exists).
    expect(watched()).toBe(true);
    Object.defineProperty(row, 'offsetHeight', { configurable: true, value: 60 });
    act(() => triggerResize(row));
    expect(lineItems()[1]!.getAttribute('style')).toContain('60px');
    // Hide the debug line: its row unmounts, and the browser reports it once more.
    await user.click(screen.getByRole('button', { name: /Debug/ }));
    expect(row.isConnected).toBe(false);
    act(() => triggerResize(row));
    expect(watched()).toBe(false);
  });

  it('renders on the server', () => {
    const html = renderToString(<LogViewer lines={lines} />);
    expect(html).toContain('mn-log-viewer');
    expect(html).toContain('Connection refused by upstream');
  });
});
