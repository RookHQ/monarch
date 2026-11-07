import { describe, expect, it, vi } from 'vitest';
import { fireEvent, render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import type { AiMessage, AiPart } from '../ai-conversation/index.js';
import { AiMessageView } from './index.js';

const T = '2026-03-02T10:15:00.000Z';

function assistant(parts: AiPart[], extra: Partial<AiMessage> = {}): AiMessage {
  return {
    id: 'a1',
    role: 'assistant',
    createdAt: T,
    agent: { name: 'Helper', role: 'Writing assistant' },
    parts,
    ...extra,
  };
}

function user(text: string, extra: Partial<AiMessage> = {}): AiMessage {
  return {
    id: 'u1',
    role: 'user',
    createdAt: T,
    parts: [{ type: 'text', text, state: 'done' }],
    ...extra,
  };
}

const RICH: AiPart[] = [
  { type: 'reasoning', text: 'Look at the outline first.', state: 'done', durationMs: 4200 },
  { type: 'text', text: 'Here is the summary.', state: 'done' },
  {
    type: 'tool-call',
    id: 't1',
    name: 'search_documents',
    input: {},
    state: 'succeeded',
    startedAt: '2026-03-02T10:15:00.000Z',
    endedAt: '2026-03-02T10:15:02.400Z',
  },
  {
    type: 'source',
    id: 's1',
    title: 'Quarterly notes',
    url: 'https://example.com/notes',
    kind: 'doc',
  },
  { type: 'source', id: 's2', title: 'Unsafe link', url: 'javascript:alert(1)', kind: 'web' },
  { type: 'citation', id: 'c1', sourceId: 's1' },
  { type: 'file', name: 'outline.pdf', mediaType: 'application/pdf', size: 2048 },
  { type: 'plan', steps: [{ id: 'p1', label: 'Hidden by default', state: 'todo' }] },
];

describe('AiMessageView', () => {
  it('renders parts in order with the assistant layout and an AI prefix', async () => {
    const { container } = render(<AiMessageView message={assistant(RICH)} />);
    const article = screen.getByRole('article');
    expect(article).toHaveClass('mn-ai-message');
    expect(article).toHaveAttribute('data-role', 'assistant');
    expect(screen.getByText('Helper')).toBeInTheDocument();
    expect(screen.getByText('Assistant said:')).toHaveClass('mn-visually-hidden');
    const order = Array.from(
      container.querySelectorAll(
        '.mn-ai-message__reasoning, .mn-ai-message__text, .mn-ai-message__tool, .mn-ai-message__sources, .mn-ai-message__citation, .mn-ai-message__file',
      ),
    ).map((el) => el.className.split(' ').find((c) => c.startsWith('mn-ai-message__')));
    expect(order).toEqual([
      'mn-ai-message__reasoning',
      'mn-ai-message__text',
      'mn-ai-message__tool',
      'mn-ai-message__sources',
      'mn-ai-message__citation',
      'mn-ai-message__file',
    ]);
    expect(screen.getByText('Here is the summary.')).toBeInTheDocument();
    expect(screen.queryByText('Hidden by default')).toBeNull();
    await expectNoA11yViolations();
  });

  it('shows the tool call name, state and duration', () => {
    render(<AiMessageView message={assistant(RICH)} />);
    const row = document.querySelector('.mn-ai-message__tool')!;
    expect(row).toHaveAttribute('data-state', 'succeeded');
    expect(row).toHaveTextContent('search_documents');
    expect(row).toHaveTextContent('Done');
    expect(row).toHaveTextContent('2s');
  });

  it('lists sources with safe links only and links citations to them', () => {
    render(<AiMessageView message={assistant(RICH)} />);
    const list = screen.getByRole('list', { name: 'Sources' });
    const link = within(list).getByRole('link', { name: 'Quarterly notes' });
    expect(link).toHaveAttribute('rel', 'noopener noreferrer nofollow');
    expect(within(list).queryByRole('link', { name: 'Unsafe link' })).toBeNull();
    expect(within(list).getByText('Unsafe link')).toBeInTheDocument();
    const citation = screen.getByRole('link', { name: 'Source 1' });
    expect(citation.getAttribute('href')).toBe(`#${link.closest('li')!.id}`);
    expect(screen.getByText('outline.pdf')).toBeInTheDocument();
    expect(document.querySelector('.mn-ai-message__file-size')).toHaveTextContent('2');
  });

  it('reasoning is a closed <details> "Thought for Ns"; live label while streaming; redacted is hidden', async () => {
    const { rerender } = render(<AiMessageView message={assistant(RICH)} />);
    const details = document.querySelector(
      'details.mn-ai-message__reasoning',
    ) as HTMLDetailsElement;
    expect(details.open).toBe(false);
    const summary = screen.getByText('Thought for 4s');
    await userEvent.setup().click(summary);
    expect(details.open).toBe(true);
    expect(screen.getByText('Look at the outline first.')).toBeVisible();

    rerender(
      <AiMessageView
        message={assistant([{ type: 'reasoning', text: 'Checking', state: 'streaming' }])}
      />,
    );
    expect(screen.getByText('Thinking…')).toBeInTheDocument();
    expect(screen.getByRole('article')).toHaveAttribute('data-streaming');
    // Busy only when the thread announces; otherwise it would silence the text's own region.
    expect(screen.getByRole('article')).not.toHaveAttribute('aria-busy');
    rerender(
      <AiMessageView
        announce={false}
        message={assistant([{ type: 'reasoning', text: 'Checking', state: 'streaming' }])}
      />,
    );
    expect(screen.getByRole('article')).toHaveAttribute('aria-busy', 'true');

    rerender(
      <AiMessageView
        message={assistant([{ type: 'reasoning', text: '', state: 'done', redacted: true }])}
      />,
    );
    expect(screen.getByText('Reasoning hidden')).toBeInTheDocument();
    expect(document.querySelector('details')).toBeNull();
  });

  it('wires actions by role and hides them while streaming', async () => {
    const onRegenerate = vi.fn();
    const onFeedback = vi.fn();
    const onEdit = vi.fn();
    const msg = assistant([{ type: 'text', text: 'Answer.', state: 'done' }], { feedback: 'up' });
    const { rerender } = render(
      <AiMessageView
        message={msg}
        onRegenerate={onRegenerate}
        onFeedback={onFeedback}
        onEdit={onEdit}
        onCopy={() => {}}
      />,
    );
    const user1 = userEvent.setup();
    expect(screen.getByRole('button', { name: 'Good response' })).toHaveAttribute(
      'aria-pressed',
      'true',
    );
    expect(screen.queryByRole('button', { name: 'Edit' })).toBeNull();
    await user1.click(screen.getByRole('button', { name: 'Regenerate' }));
    expect(onRegenerate).toHaveBeenCalledWith(msg);
    await user1.click(screen.getByRole('button', { name: 'Bad response' }));
    expect(onFeedback).toHaveBeenCalledWith('down', msg);

    const mine = user('Question?');
    rerender(<AiMessageView message={mine} onRegenerate={onRegenerate} onEdit={onEdit} />);
    expect(screen.getByRole('article')).toHaveAttribute('data-role', 'user');
    expect(screen.getByText('You said:')).toHaveClass('mn-visually-hidden');
    expect(screen.queryByRole('button', { name: 'Regenerate' })).toBeNull();
    await user1.click(screen.getByRole('button', { name: 'Edit' }));
    expect(onEdit).toHaveBeenCalledWith(mine);

    rerender(
      <AiMessageView
        message={assistant([{ type: 'text', text: 'Partial', state: 'streaming' }])}
        onRegenerate={onRegenerate}
      />,
    );
    expect(screen.queryByRole('toolbar')).toBeNull();
  });

  it('built-in Copy writes the message text to the clipboard', async () => {
    const user1 = userEvent.setup();
    const writeText = vi.spyOn(navigator.clipboard, 'writeText');
    render(
      <AiMessageView message={assistant([{ type: 'text', text: 'Copy me.', state: 'done' }])} />,
    );
    await user1.click(screen.getByRole('button', { name: 'Copy' }));
    expect(writeText).toHaveBeenCalledWith('Copy me.');
    expect(await screen.findByRole('button', { name: 'Copied' })).toBeInTheDocument();
  });

  it('branch switcher shows "2 / 3" and calls onBranch with the new index', async () => {
    const onBranch = vi.fn();
    const msg = assistant([{ type: 'text', text: 'B', state: 'done' }], {
      branch: { index: 1, count: 3 },
    });
    const { rerender } = render(<AiMessageView message={msg} onBranch={onBranch} />);
    const group = screen.getByRole('group', { name: 'Versions' });
    expect(group).toHaveTextContent('2 / 3');
    const user1 = userEvent.setup();
    await user1.click(within(group).getByRole('button', { name: 'Previous version' }));
    expect(onBranch).toHaveBeenLastCalledWith(0, msg);
    await user1.click(within(group).getByRole('button', { name: 'Next version' }));
    expect(onBranch).toHaveBeenLastCalledWith(2, msg);
    rerender(
      <AiMessageView message={{ ...msg, branch: { index: 0, count: 3 } }} onBranch={onBranch} />,
    );
    expect(screen.getByRole('button', { name: 'Previous version' })).toBeDisabled();
  });

  it('edit mode: focused textarea, Enter saves, Shift+Enter adds a line, Escape cancels', async () => {
    const onEditSubmit = vi.fn();
    const onEditCancel = vi.fn();
    const outerKey = vi.fn();
    const mine = user('Draft a note');
    render(
      // eslint-disable-next-line jsx-a11y/no-static-element-interactions
      <div onKeyDown={outerKey}>
        <AiMessageView
          message={mine}
          editing
          onEditSubmit={onEditSubmit}
          onEditCancel={onEditCancel}
        />
      </div>,
    );
    const box = screen.getByRole('textbox', { name: 'Edit message' });
    expect(box).toHaveFocus();
    expect(box).toHaveValue('Draft a note');
    expect(box).toHaveAccessibleDescription(/Enter to save/);
    const user1 = userEvent.setup();
    await user1.type(box, ' today{Shift>}{Enter}{/Shift}please');
    expect(box).toHaveValue('Draft a note today\nplease');
    await user1.keyboard('{Enter}');
    expect(onEditSubmit).toHaveBeenCalledWith('Draft a note today\nplease', mine);
    await user1.keyboard('{Escape}');
    expect(onEditCancel).toHaveBeenCalledWith(mine);
    expect(outerKey.mock.calls.some(([e]) => e.key === 'Escape')).toBe(false);
    await user1.click(screen.getByRole('button', { name: 'Cancel' }));
    expect(onEditCancel).toHaveBeenCalledTimes(2);
    await expectNoA11yViolations();
  });

  it('renders error parts with Try again when retryable', async () => {
    const onRetry = vi.fn();
    const msg = assistant([
      { type: 'error', code: 'network', message: 'The connection dropped.', retryable: true },
    ]);
    render(<AiMessageView message={msg} onRetry={onRetry} />);
    fireEvent.click(screen.getByRole('button', { name: 'Try again' }));
    expect(onRetry).toHaveBeenCalledWith(msg);
  });

  it('renderPart overrides a part, returns undefined for the default, null for nothing', () => {
    render(
      <AiMessageView
        message={assistant(RICH)}
        renderPart={(part) =>
          part.type === 'plan' ? <p>Custom plan</p> : part.type === 'file' ? null : undefined
        }
      />,
    );
    expect(screen.getByText('Custom plan')).toBeInTheDocument();
    expect(screen.queryByText('outline.pdf')).toBeNull();
    expect(screen.getByText('Here is the summary.')).toBeInTheDocument();
  });

  it('uses the avatar image with a bot badge, and takes translated strings', () => {
    render(
      <AiMessageView
        message={assistant([{ type: 'text', text: 'Hola', state: 'done' }], {
          agent: { name: 'Helper', avatar: 'https://example.com/a.png' },
        })}
        messages={{ assistantSaid: 'El asistente dijo:' }}
      />,
    );
    expect(document.querySelector('.mn-ai-message__badge')).toHaveAttribute('aria-hidden', 'true');
    expect(screen.getByText('El asistente dijo:')).toBeInTheDocument();
    expect(document.querySelector('time')).toHaveAttribute('datetime', T);
  });

  it('renders on the server', () => {
    const html = renderToString(<AiMessageView message={assistant(RICH)} />);
    expect(html).toContain('mn-ai-message');
    expect(html).toContain('Thought for 4s');
  });
});
