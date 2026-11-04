import { describe, expect, it, vi } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { MessageRow } from './index.js';

const NOW = '2026-03-02T12:00:00Z';
const author = { name: 'Ana Lopez' };

describe('MessageRow', () => {
  it('renders author, time and body as a labelled article', async () => {
    render(
      <MessageRow
        data-testid="row"
        author={author}
        time="2026-03-02T11:55:00Z"
        now={NOW}
        locale="en-GB"
        timeZone="UTC"
      >
        <p>Ship it on Thursday.</p>
      </MessageRow>,
    );
    const row = screen.getByRole('article');
    expect(row).toHaveClass('mn-message-row');
    expect(row).toHaveAccessibleName(/Ana Lopez 5 minutes ago/);
    expect(screen.getByText('Ship it on Thursday.')).toBeInTheDocument();
    const time = row.querySelector('time')!;
    expect(time).toHaveAttribute('datetime', '2026-03-02T11:55:00.000Z');
    expect(time.getAttribute('title')).toMatch(/2026/);
    await expectNoA11yViolations();
  });

  it('grouped rows hide the header but keep the author in the accessible name', () => {
    render(
      <MessageRow author={author} time="2026-03-02T11:55:00Z" grouped locale="en-GB" timeZone="UTC">
        Follow-up
      </MessageRow>,
    );
    const row = screen.getByRole('article');
    expect(row).toHaveAttribute('data-grouped');
    expect(row.querySelector('.mn-message-row__header')).toBeNull();
    expect(row.querySelector('.mn-avatar')).toBeNull();
    expect(row).toHaveAccessibleName(/Ana Lopez 11:55/);
  });

  it('shows the edited marker, and a history button when onShowEdits is set', async () => {
    const onShowEdits = vi.fn();
    const { rerender } = render(
      <MessageRow author={author} time={NOW} edited>
        Body
      </MessageRow>,
    );
    expect(screen.getByText('(edited)')).toBeInTheDocument();
    expect(screen.queryByRole('button')).toBeNull();
    rerender(
      <MessageRow author={author} time={NOW} edited onShowEdits={onShowEdits}>
        Body
      </MessageRow>,
    );
    await userEvent.click(screen.getByRole('button', { name: 'Edited. Show edit history' }));
    expect(onShowEdits).toHaveBeenCalledOnce();
  });

  it('pending shows Sending… and hides the actions', () => {
    render(
      <MessageRow author={author} time={NOW} status="pending" actions={<button>React</button>}>
        Hi
      </MessageRow>,
    );
    expect(screen.getByRole('status')).toHaveTextContent('Sending…');
    expect(screen.getByRole('article')).toHaveAttribute('aria-busy', 'true');
    expect(screen.queryByRole('button', { name: 'React' })).toBeNull();
  });

  it('failed shows the error with Retry and Discard', async () => {
    const onRetry = vi.fn();
    const onDiscard = vi.fn();
    render(
      <MessageRow
        author={author}
        time={NOW}
        status="failed"
        error="No connection"
        onRetry={onRetry}
        onDiscard={onDiscard}
      >
        Hi
      </MessageRow>,
    );
    const alert = screen.getByRole('alert');
    expect(alert).toHaveTextContent('No connection');
    const user = userEvent.setup();
    await user.click(within(alert).getByRole('button', { name: 'Retry' }));
    await user.click(within(alert).getByRole('button', { name: 'Discard' }));
    expect(onRetry).toHaveBeenCalledOnce();
    expect(onDiscard).toHaveBeenCalledOnce();
    await expectNoA11yViolations();
  });

  it('falls back to "Not sent" when failed without an error', () => {
    render(
      <MessageRow author={author} time={NOW} status="failed">
        Hi
      </MessageRow>,
    );
    expect(screen.getByRole('alert')).toHaveTextContent('Not sent');
  });

  it('reactions are toggle buttons with pressed state and spoken counts', async () => {
    const onToggle = vi.fn();
    render(
      <MessageRow
        author={author}
        time={NOW}
        reactions={[
          { emoji: '👍', label: 'thumbs up', count: 3, reacted: true },
          { emoji: '🎉', count: 1 },
        ]}
        onReactionToggle={onToggle}
      >
        Body
      </MessageRow>,
    );
    const group = screen.getByRole('group', { name: 'Reactions' });
    const up = within(group).getByRole('button', { name: 'thumbs up, 3' });
    expect(up).toHaveAttribute('aria-pressed', 'true');
    expect(within(group).getByRole('button', { name: '🎉, 1' })).toHaveAttribute(
      'aria-pressed',
      'false',
    );
    await userEvent.click(up);
    expect(onToggle).toHaveBeenCalledWith('👍');
    await expectNoA11yViolations();
  });

  it('read-only reactions are images, not buttons', () => {
    render(
      <MessageRow author={author} time={NOW} reactions={[{ emoji: '👍', count: 2 }]}>
        Body
      </MessageRow>,
    );
    expect(screen.getByRole('img', { name: '👍, 2' })).toBeInTheDocument();
    expect(screen.queryByRole('button')).toBeNull();
  });

  it('thread count opens the thread', async () => {
    const onOpenThread = vi.fn();
    render(
      <MessageRow author={author} time={NOW} replyCount={4} onOpenThread={onOpenThread}>
        Body
      </MessageRow>,
    );
    await userEvent.click(screen.getByRole('button', { name: '4 replies' }));
    expect(onOpenThread).toHaveBeenCalledOnce();
  });

  it('actions are keyboard reachable in a named group', async () => {
    render(
      <>
        <MessageRow
          author={author}
          time={NOW}
          actions={
            <>
              <button type="button">Reply</button>
              <button type="button">More</button>
            </>
          }
        >
          Body
        </MessageRow>
      </>,
    );
    const group = screen.getByRole('group', { name: 'Message actions' });
    const user = userEvent.setup();
    await user.tab();
    expect(within(group).getByRole('button', { name: 'Reply' })).toHaveFocus();
    await user.tab();
    expect(within(group).getByRole('button', { name: 'More' })).toHaveFocus();
  });

  it('deleted replaces the body and drops reactions and actions', () => {
    render(
      <MessageRow
        author={author}
        time={NOW}
        deleted
        reactions={[{ emoji: '👍', count: 2 }]}
        actions={<button type="button">Reply</button>}
        edited
      >
        Secret
      </MessageRow>,
    );
    expect(screen.getByText('This message was deleted.')).toBeInTheDocument();
    expect(screen.queryByText('Secret')).toBeNull();
    expect(screen.queryByText('(edited)')).toBeNull();
    expect(screen.queryByRole('group')).toBeNull();
  });

  it('internal, highlighted and system variants', () => {
    const { rerender } = render(
      <MessageRow
        author={author}
        time={NOW}
        variant="internal"
        tags={<span>Internal</span>}
        highlighted
      >
        Note
      </MessageRow>,
    );
    const row = screen.getByRole('article');
    expect(row).toHaveAttribute('data-variant', 'internal');
    expect(row).toHaveAttribute('data-highlighted');
    expect(screen.getByText('Internal')).toBeInTheDocument();
    rerender(
      <MessageRow author={author} time="2026-03-02T11:00:00Z" now={NOW} variant="system">
        Ana started a call
      </MessageRow>,
    );
    expect(screen.getByRole('article')).toHaveAttribute('data-variant', 'system');
    expect(screen.getByRole('article')).toHaveAccessibleName(/Ana started a call 1 hour ago/);
  });

  it('clock time display and custom messages', () => {
    render(
      <MessageRow
        author={author}
        time="2026-03-02T09:05:00Z"
        timeDisplay="clock"
        locale="en-GB"
        timeZone="UTC"
        replyCount={1}
        messages={{ replies: (n) => `${n} respuesta` }}
      >
        Body
      </MessageRow>,
    );
    expect(screen.getByText('09:05')).toBeInTheDocument();
    expect(screen.getByText('1 respuesta')).toBeInTheDocument();
  });

  it('forwards the ref and survives an invalid time', () => {
    const ref = { current: null as HTMLElement | null };
    render(
      <MessageRow ref={ref} author={author} time="not a date">
        Body
      </MessageRow>,
    );
    expect(ref.current?.tagName).toBe('ARTICLE');
  });

  it('renders on the server', () => {
    const html = renderToString(
      <MessageRow
        author={author}
        time="2026-03-02T11:55:00Z"
        now={NOW}
        reactions={[{ emoji: '👍', count: 1 }]}
      >
        Hello
      </MessageRow>,
    );
    expect(html).toContain('mn-message-row');
    expect(html).toContain('Ana Lopez');
  });
});
