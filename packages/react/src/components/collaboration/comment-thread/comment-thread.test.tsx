import { describe, expect, it, vi } from 'vitest';
import { act, render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import type { CommentItem } from '../comments-panel/index.js';
import { CommentThread, type CommentThreadProps } from './index.js';

const NOW = new Date('2026-03-10T12:00:00Z');
const ADA = { name: 'Ada Lovelace' };
const BEN = { name: 'Ben Okafor', seed: 'u2' };

function comment(
  id: string,
  author = ADA,
  body = `Comment ${id}`,
  canDelete?: boolean,
): CommentItem {
  return { id, author, body, createdAt: '2026-03-10T11:00:00Z', canDelete };
}

const COMMENTS = [
  comment('c1', ADA, 'Should this say quarterly?'),
  comment('c2', BEN, 'Yes, quarterly.', true),
];

function thread(props: Partial<CommentThreadProps> = {}) {
  return <CommentThread comments={COMMENTS} now={NOW} locale="en-US" {...props} />;
}

function deferred() {
  let resolve!: () => void;
  let reject!: (e: unknown) => void;
  const promise = new Promise<void>((res, rej) => {
    resolve = res;
    reject = rej;
  });
  return { promise, resolve, reject };
}

describe('CommentThread', () => {
  it('shows the starting comment, replies and a named reply box', async () => {
    render(thread({ quote: 'Billed monthly', onReply: () => {}, onResolvedChange: () => {} }));
    const section = screen.getByRole('region', { name: 'Thread started by Ada Lovelace' });
    expect(section).toHaveClass('mn-comment-thread');
    const items = within(section).getAllByRole('listitem');
    expect(items).toHaveLength(2);
    expect(items[0]).toHaveAttribute('data-kind', 'root');
    expect(items[1]).toHaveAttribute('data-kind', 'reply');
    expect(screen.getByLabelText('Quoted text')).toHaveTextContent('Billed monthly');
    expect(screen.getByRole('textbox', { name: 'Reply' })).toHaveAccessibleDescription(
      'Enter to send, Shift+Enter for a new line.',
    );
    expect(screen.getByRole('button', { name: 'Send reply' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Resolve' })).toHaveAttribute(
      'aria-pressed',
      'false',
    );
    await expectNoA11yViolations();
  });

  it('sends a reply with Enter, keeps it busy, clears and announces', async () => {
    const d = deferred();
    const onReply = vi.fn(() => d.promise);
    render(thread({ onReply }));
    const user = userEvent.setup();
    const box = screen.getByRole('textbox', { name: 'Reply' });
    await user.type(box, 'Agreed{Shift>}{Enter}{/Shift}Thanks{Enter}');
    expect(onReply).toHaveBeenCalledWith('Agreed\nThanks');
    expect(box).toHaveAttribute('readonly');
    await act(async () => d.resolve());
    expect(box).toHaveValue('');
    expect(box).not.toHaveAttribute('readonly');
    expect(screen.getByText('Reply sent')).toBeInTheDocument();
  });

  it('keeps the draft and shows an error when the reply fails', async () => {
    const onReply = vi.fn(() => Promise.reject(new Error('offline')));
    render(thread({ onReply }));
    const user = userEvent.setup();
    const box = screen.getByRole('textbox', { name: 'Reply' });
    await user.type(box, 'Hello');
    await user.click(screen.getByRole('button', { name: 'Send reply' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('That didn’t work. Try again.');
    expect(box).toHaveValue('Hello');
    expect(box).toHaveAttribute('aria-invalid', 'true');
  });

  it('resolves and reopens, hiding the reply box while resolved', async () => {
    const onResolvedChange = vi.fn();
    const { rerender } = render(thread({ onResolvedChange, onReply: () => {} }));
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Resolve' }));
    expect(onResolvedChange).toHaveBeenCalledWith(true);
    expect(screen.getByText('Thread resolved')).toBeInTheDocument();
    rerender(thread({ onResolvedChange, onReply: () => {}, resolved: true }));
    expect(screen.getByRole('region')).toHaveAttribute('data-resolved');
    expect(screen.queryByRole('textbox')).toBeNull();
    expect(screen.getByText('This thread is resolved. Reopen it to reply.')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Reopen' }));
    expect(onResolvedChange).toHaveBeenLastCalledWith(false);
  });

  it('folds earlier replies and moves focus to the first revealed one', async () => {
    const many = [comment('r'), ...Array.from({ length: 6 }, (_, i) => comment(`x${i}`, BEN))];
    render(thread({ comments: many }));
    expect(screen.getAllByRole('listitem')).toHaveLength(1 + 1 + 3);
    expect(screen.queryByText('Comment x0')).toBeNull();
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Show 3 earlier replies' }));
    expect(screen.getByText('Comment x0')).toBeInTheDocument();
    await waitFor(() => expect(screen.getByText('Comment x0').closest('li')).toHaveFocus());
    expect(screen.queryByRole('button', { name: /earlier/ })).toBeNull();
  });

  it('confirms before deleting a comment', async () => {
    const onDeleteComment = vi.fn();
    render(thread({ onDeleteComment }));
    expect(screen.queryByRole('button', { name: 'Delete comment by Ada Lovelace' })).toBeNull();
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Delete comment by Ben Okafor' }));
    const dialog = await screen.findByRole('alertdialog');
    expect(dialog).toHaveTextContent('Delete this comment?');
    await user.click(within(dialog).getByRole('button', { name: 'Delete comment' }));
    await waitFor(() => expect(onDeleteComment).toHaveBeenCalledWith('c2'));
  });

  it('translates its text and has no reply box without onReply', () => {
    render(thread({ messages: { label: (a) => `Diskussion von ${a}` } }));
    expect(screen.getByRole('region', { name: 'Diskussion von Ada Lovelace' })).toBeInTheDocument();
    expect(screen.queryByRole('textbox')).toBeNull();
  });

  it('renders on the server', () => {
    const html = renderToString(thread({ onReply: () => {} }));
    expect(html).toContain('mn-comment-thread');
    expect(html).toContain('Should this say quarterly?');
  });
});
