import { describe, expect, it, vi } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { createRef } from 'react';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { AiActionLog, type AiActionLogEntry } from './index.js';

const NOW = Date.UTC(2026, 0, 15, 12, 0);
const agent = { id: 'research', name: 'Research agent' };

const entries: AiActionLogEntry[] = [
  {
    id: '1',
    at: NOW - 5 * 60_000,
    agent,
    action: 'renamed',
    target: 'Quarterly report',
    href: '/docs/42',
  },
  {
    id: '2',
    at: NOW - 2 * 3_600_000,
    agent: { name: 'Scheduling agent' },
    action: 'moved a meeting to Friday',
  },
];

describe('AiActionLog', () => {
  it('lists each action with an AI-agent actor, target link and time', async () => {
    const ref = createRef<HTMLDivElement>();
    render(
      <AiActionLog ref={ref} entries={entries} now={NOW} data-testid="root" className="extra" />,
    );
    const root = screen.getByTestId('root');
    expect(root).toHaveClass('mn-ai-action-log', 'extra');
    expect(ref.current).toBe(root);
    const list = screen.getByRole('list', { name: 'AI activity' });
    const items = within(list).getAllByRole('listitem');
    expect(items).toHaveLength(2);
    expect(items[0]).toHaveTextContent('Research agent (AI agent) renamed Quarterly report');
    expect(within(items[0]!).getByRole('link', { name: 'Quarterly report' })).toHaveAttribute(
      'href',
      '/docs/42',
    );
    expect(items[0]!.querySelector('time')).toHaveTextContent('5 minutes ago');
    expect(items[1]).toHaveTextContent('Scheduling agent (AI agent) moved a meeting to Friday');
    expect(screen.queryByRole('button')).toBeNull();
    await expectNoA11yViolations();
  });

  it('drops unsafe target links', () => {
    render(<AiActionLog now={NOW} entries={[{ ...entries[0]!, href: 'javascript:alert(1)' }]} />);
    expect(screen.queryByRole('link')).toBeNull();
    expect(screen.getByText('Quarterly report')).toBeInTheDocument();
  });

  it('undoes an action, keeps focus on its row and announces it', async () => {
    const user = userEvent.setup();
    let resolve: () => void = () => {};
    const onUndo = vi.fn(() => new Promise<void>((r) => (resolve = r)));
    render(<AiActionLog now={NOW} entries={[{ ...entries[0]!, undoable: true, onUndo }]} />);
    const button = screen.getByRole('button', {
      name: 'Undo: Research agent renamed Quarterly report',
    });
    expect(button).toHaveTextContent('Undo');
    await user.tab();
    await user.tab();
    expect(button).toHaveFocus();
    await user.keyboard('{Enter}');
    expect(onUndo).toHaveBeenCalledTimes(1);
    expect(button).toHaveAttribute('aria-busy', 'true');
    await user.click(button);
    expect(onUndo).toHaveBeenCalledTimes(1);
    resolve();
    expect(
      await screen.findByText('Undone: Research agent renamed Quarterly report'),
    ).toBeInTheDocument();
    expect(screen.queryByRole('button')).toBeNull();
    const row = screen.getByRole('listitem');
    expect(row).toHaveAttribute('data-undone');
    expect(row).toHaveFocus();
    expect(row).toHaveTextContent('Undone');
    await expectNoA11yViolations();
  });

  it('announces a failed undo assertively and keeps the button', async () => {
    const user = userEvent.setup();
    const onUndo = vi.fn(() => Promise.reject(new Error('nope')));
    render(<AiActionLog now={NOW} entries={[{ ...entries[1]!, undoable: true, onUndo }]} />);
    await user.click(screen.getByRole('button'));
    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Could not undo: Scheduling agent moved a meeting to Friday',
    );
    expect(screen.getByRole('button')).toBeInTheDocument();
  });

  it('hides Undo for entries already undone or not undoable', () => {
    render(
      <AiActionLog
        now={NOW}
        entries={[
          { ...entries[0]!, undoable: true, onUndo: () => {}, undone: true },
          { ...entries[1]!, onUndo: () => {} },
        ]}
      />,
    );
    expect(screen.queryByRole('button')).toBeNull();
  });

  it('shows an empty message and takes translated strings', () => {
    const { rerender } = render(<AiActionLog entries={[]} />);
    expect(screen.getByText('No AI activity yet.')).toBeInTheDocument();
    rerender(
      <AiActionLog
        now={NOW}
        entries={entries}
        messages={{ label: 'KI-Aktivität', actorKind: 'KI-Agent' }}
      />,
    );
    expect(screen.getByRole('list', { name: 'KI-Aktivität' })).toHaveTextContent('KI-Agent');
  });

  it('renders on the server', () => {
    const html = renderToString(<AiActionLog entries={entries} now={NOW} locale="en" />);
    expect(html).toContain('mn-ai-action-log');
    expect(html).toContain('Research agent');
  });
});
