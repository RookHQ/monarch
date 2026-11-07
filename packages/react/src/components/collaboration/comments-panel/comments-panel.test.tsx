import { describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { CommentsPanel, type CommentThread } from './index.js';

const NOW = new Date('2026-03-10T12:00:00Z');

const THREADS: CommentThread[] = [
  {
    id: 't1',
    quote: 'The rollout starts in the second week.',
    comments: [
      {
        id: 'c1',
        author: { name: 'Ana Ruiz' },
        body: 'Can we confirm the date?',
        createdAt: '2026-03-10T10:00:00Z',
      },
      {
        id: 'c2',
        author: { name: 'Ben Okafor' },
        body: 'Confirmed with the team.',
        createdAt: '2026-03-10T11:00:00Z',
        canDelete: true,
      },
    ],
  },
  {
    id: 't2',
    quote: 'Budget table',
    resolved: true,
    comments: [
      {
        id: 'c3',
        author: { name: 'Chen Li' },
        body: 'Numbers updated.',
        createdAt: '2026-03-09T09:00:00Z',
      },
    ],
  },
];

function deferred() {
  let resolve!: () => void;
  let reject!: (e: unknown) => void;
  const promise = new Promise<void>((res, rej) => {
    resolve = res;
    reject = rej;
  });
  return { promise, resolve, reject };
}

describe('CommentsPanel', () => {
  it('lists open threads in a labelled complementary region', async () => {
    render(<CommentsPanel threads={THREADS} now={NOW} onClose={() => {}} />);
    const panel = screen.getByRole('complementary', { name: 'Comments' });
    expect(panel).toHaveClass('mn-comments-panel');
    expect(within(panel).getByRole('button', { name: /Ana Ruiz/ })).toHaveTextContent('2 comments');
    expect(within(panel).queryByText('Budget table')).toBeNull();
    expect(screen.getByRole('button', { name: 'Close comments' })).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('switches to resolved threads and calls onFilterChange', async () => {
    const onFilterChange = vi.fn();
    render(<CommentsPanel threads={THREADS} now={NOW} onFilterChange={onFilterChange} />);
    await userEvent.setup().click(screen.getByRole('radio', { name: 'Resolved' }));
    expect(onFilterChange).toHaveBeenCalledWith('resolved');
    expect(screen.getByText('Budget table')).toBeInTheDocument();
    expect(screen.queryByText('The rollout starts in the second week.')).toBeNull();
  });

  it('shows an empty state per filter', () => {
    render(<CommentsPanel threads={[]} />);
    expect(screen.getByText('No open comments')).toBeInTheDocument();
    expect(screen.getByText('Select text to start a discussion.')).toBeInTheDocument();
  });

  it('opens a thread, moves focus into it and returns focus to its row on Back', async () => {
    const onSelect = vi.fn();
    const user = userEvent.setup();
    render(<CommentsPanel threads={THREADS} now={NOW} onSelectedThreadChange={onSelect} />);
    await user.click(screen.getByRole('button', { name: /Ana Ruiz/ }));
    expect(onSelect).toHaveBeenLastCalledWith('t1');
    expect(screen.getByRole('button', { name: 'All comments' })).toHaveFocus();
    expect(screen.getByText('Confirmed with the team.')).toBeInTheDocument();
    expect(screen.getByLabelText('Quoted text')).toHaveTextContent('second week');
    await expectNoA11yViolations();
    await user.keyboard('{Enter}');
    expect(onSelect).toHaveBeenLastCalledWith(null);
    expect(screen.getByRole('button', { name: /Ana Ruiz/ })).toHaveFocus();
  });

  it('sends a reply with Enter, keeps it busy while pending and clears on success', async () => {
    const d = deferred();
    const onReply = vi.fn(() => d.promise);
    const user = userEvent.setup();
    render(
      <CommentsPanel threads={THREADS} now={NOW} defaultSelectedThreadId="t1" onReply={onReply} />,
    );
    const box = screen.getByRole('textbox', { name: 'Reply' });
    expect(screen.getByRole('button', { name: 'Send reply' })).toBeDisabled();
    await user.type(box, 'Looks good{Shift>}{Enter}{/Shift}thanks');
    expect(box).toHaveValue('Looks good\nthanks');
    await user.keyboard('{Enter}');
    expect(onReply).toHaveBeenCalledWith('t1', 'Looks good\nthanks');
    expect(screen.getByRole('button', { name: 'Send reply' })).toHaveAttribute('aria-busy', 'true');
    d.resolve();
    await waitFor(() => expect(box).toHaveValue(''));
  });

  it('shows an error when a reply fails and keeps the text', async () => {
    const onReply = vi.fn(() => Promise.reject(new Error('nope')));
    const user = userEvent.setup();
    render(
      <CommentsPanel threads={THREADS} now={NOW} defaultSelectedThreadId="t1" onReply={onReply} />,
    );
    const box = screen.getByRole('textbox', { name: 'Reply' });
    await user.type(box, 'Hello');
    await user.click(screen.getByRole('button', { name: 'Send reply' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('That didn’t work. Try again.');
    expect(box).toHaveValue('Hello');
    expect(box).toHaveAttribute('aria-invalid', 'true');
  });

  it('resolves a thread, announces it and goes back to the list', async () => {
    const onResolved = vi.fn();
    const user = userEvent.setup();
    render(
      <CommentsPanel
        threads={THREADS}
        now={NOW}
        defaultSelectedThreadId="t1"
        onResolvedChange={onResolved}
      />,
    );
    await user.click(screen.getByRole('button', { name: 'Resolve' }));
    expect(onResolved).toHaveBeenCalledWith('t1', true);
    expect(screen.getByText('Thread resolved')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'All comments' })).toBeNull();
  });

  it('a resolved thread offers Reopen and no reply box', () => {
    render(
      <CommentsPanel
        threads={THREADS}
        now={NOW}
        defaultFilter="resolved"
        defaultSelectedThreadId="t2"
        onReply={() => {}}
        onResolvedChange={() => {}}
      />,
    );
    expect(screen.getByRole('button', { name: 'Reopen' })).toBeInTheDocument();
    expect(screen.queryByRole('textbox', { name: 'Reply' })).toBeNull();
    expect(screen.getByText('This thread is resolved. Reopen it to reply.')).toBeInTheDocument();
  });

  it('confirms before deleting a comment', async () => {
    const onDelete = vi.fn();
    const user = userEvent.setup();
    render(
      <CommentsPanel
        threads={THREADS}
        now={NOW}
        defaultSelectedThreadId="t1"
        onDeleteComment={onDelete}
      />,
    );
    expect(screen.queryByRole('button', { name: 'Delete comment by Ana Ruiz' })).toBeNull();
    await user.click(screen.getByRole('button', { name: 'Delete comment by Ben Okafor' }));
    const dialog = await screen.findByRole('alertdialog', { name: 'Delete this comment?' });
    expect(onDelete).not.toHaveBeenCalled();
    await user.click(within(dialog).getByRole('button', { name: 'Delete comment' }));
    expect(onDelete).toHaveBeenCalledWith('t1', 'c2');
  });

  it('shows loading and error states', async () => {
    const onRetry = vi.fn();
    const { rerender } = render(<CommentsPanel threads={null} />);
    expect(screen.getByRole('status', { name: 'Loading comments…' })).toBeInTheDocument();
    rerender(<CommentsPanel threads={null} error="Comments didn’t load." onRetry={onRetry} />);
    await userEvent.setup().click(screen.getByRole('button', { name: 'Try again' }));
    expect(onRetry).toHaveBeenCalled();
  });

  it('accepts translated text', () => {
    render(
      <CommentsPanel
        threads={[]}
        messages={{ title: 'Kommentare', emptyOpen: 'Keine offenen Kommentare' }}
      />,
    );
    expect(screen.getByRole('complementary', { name: 'Kommentare' })).toBeInTheDocument();
    expect(screen.getByText('Keine offenen Kommentare')).toBeInTheDocument();
  });

  it('renders on the server', () => {
    const html = renderToString(<CommentsPanel threads={THREADS} now={NOW} />);
    expect(html).toContain('mn-comments-panel');
    expect(html).toContain('Ana Ruiz');
  });
});
