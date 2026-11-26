import { describe, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import type { AiMessage, AiPart } from '../ai-conversation/index.js';
import { AiMessageView, useAiMessageWiring } from './index.js';
import { renderHook } from '@testing-library/react';
import { mockTransport, useAiConversation } from '../ai-conversation/index.js';

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
    // A plan part shows as a read-only step list.
    expect(
      within(screen.getByRole('list', { name: 'Plan' })).getByText('Hidden by default'),
    ).toBeInTheDocument();
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

  it('renders a plan part by default, and artifact, ui and edit-proposal parts once given what they need', async () => {
    const user = userEvent.setup();
    const parts: AiPart[] = [
      { type: 'plan', steps: [{ id: 's1', label: 'Read the brief', state: 'done' }] },
      { type: 'artifact', id: 'art1', kind: 'document', title: 'Launch plan', version: 2 },
      { type: 'ui', component: 'Stat', props: { label: 'Open deals' } },
      {
        type: 'edit-proposal',
        id: 'e1',
        target: 'doc-1',
        diff: { to: 'Hello there' },
        state: 'proposed',
      },
    ];
    const message = assistant(parts);
    const { rerender, container } = render(<AiMessageView message={message} />);
    expect(screen.getByText('Read the brief')).toBeInTheDocument();
    // Without the props below, these three stay hidden: nothing is guessed.
    expect(screen.queryByText('Launch plan')).toBeNull();
    expect(screen.queryByText('Open deals')).toBeNull();
    expect(container.querySelector('.mn-ai-message__edit-proposal')).toBeNull();

    const onOpenArtifact = vi.fn();
    rerender(
      <AiMessageView
        message={message}
        onOpenArtifact={onOpenArtifact}
        uiRegistry={{ Stat: ({ label }: { label: string }) => <p>{label}</p> }}
        getEditHunks={(part) => [
          { id: 'h1', before: 'Hello', after: (part.diff as { to: string }).to },
        ]}
      />,
    );
    await user.click(screen.getByRole('button', { name: /Launch plan/ }));
    expect(onOpenArtifact).toHaveBeenCalledWith(parts[1], message);
    expect(screen.getByText('Open deals')).toBeInTheDocument();
    expect(container.querySelector('.mn-ai-message__edit-proposal')).toHaveTextContent('there');
    await expectNoA11yViolations();
  });

  it('a ui part naming a component outside the registry renders no component', () => {
    render(
      <AiMessageView
        message={assistant([{ type: 'ui', component: 'Script', props: { label: 'Nope' } }])}
        uiRegistry={{ Stat: ({ label }: { label: string }) => <p>{label}</p> }}
      />,
    );
    expect(screen.queryByText('Nope')).toBeNull();
  });

  it('is memoised: the same props do not render the message again', () => {
    const renderPart = vi.fn(() => undefined);
    const message = assistant([{ type: 'text', text: 'Stable answer.', state: 'done' }]);
    const { rerender } = render(<AiMessageView message={message} renderPart={renderPart} />);
    const calls = renderPart.mock.calls.length;
    rerender(<AiMessageView message={message} renderPart={renderPart} />);
    expect(renderPart.mock.calls.length).toBe(calls);
    // A new message object (a streamed update) renders again.
    rerender(<AiMessageView message={{ ...message }} renderPart={renderPart} />);
    expect(renderPart.mock.calls.length).toBeGreaterThan(calls);
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

describe('AiMessageView: tool approval, finish reasons, RTL', () => {
  const pending: AiPart = {
    type: 'tool-call',
    id: 'mail',
    name: 'send_email',
    input: { to: 'a@b.test' },
    state: 'pending-approval',
    risk: 'high',
  };

  it('renders AiToolApproval for a pending call when approve or deny handlers are given', async () => {
    const onApproveTool = vi.fn();
    const onDenyTool = vi.fn();
    const onAlwaysAllowTool = vi.fn();
    render(
      <AiMessageView
        message={assistant([pending])}
        onApproveTool={onApproveTool}
        onDenyTool={onDenyTool}
        onAlwaysAllowTool={onAlwaysAllowTool}
      />,
    );
    expect(screen.getByText('High risk')).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: 'Approve' }));
    expect(onApproveTool).toHaveBeenCalledWith(
      'mail',
      undefined,
      expect.objectContaining({ id: 'a1' }),
    );
    cleanup();
    render(<AiMessageView message={assistant([pending])} onDenyTool={onDenyTool} />);
    await userEvent.click(screen.getByRole('button', { name: 'Deny' }));
    expect(onDenyTool).toHaveBeenCalledWith(
      'mail',
      undefined,
      expect.objectContaining({ id: 'a1' }),
    );
  });

  it('without handlers a pending call stays a status row', () => {
    render(<AiMessageView message={assistant([pending])} />);
    expect(screen.queryByRole('button', { name: 'Approve' })).toBeNull();
    expect(screen.getByText('Waiting for approval')).toBeInTheDocument();
  });

  it('labels calls waiting for a client result; toolStates merge per key', () => {
    render(
      <AiMessageView
        message={assistant([
          { type: 'tool-call', id: 't', name: 'x', input: {}, state: 'awaiting-result' },
        ])}
        messages={{ toolStates: { running: 'En cours' } }}
      />,
    );
    expect(screen.getByText('Waiting for result')).toBeInTheDocument();
  });

  it('shows a notice when the answer was cut short, filtered or refused', () => {
    const { rerender } = render(
      <AiMessageView
        message={assistant([{ type: 'text', text: 'Partial', state: 'done' }], {
          finishReason: 'length',
        })}
      />,
    );
    expect(screen.getByText(/cut short/)).toBeInTheDocument();
    rerender(
      <AiMessageView
        message={assistant([{ type: 'text', text: 'No', state: 'done' }], {
          finishReason: 'refusal',
        })}
        messages={{ finishNotices: { refusal: 'Refusé' } }}
      />,
    );
    expect(screen.getByText('Refusé')).toBeInTheDocument();
    rerender(
      <AiMessageView
        message={assistant([{ type: 'text', text: 'Ok', state: 'done' }], { finishReason: 'stop' })}
      />,
    );
    expect(document.querySelector('.mn-ai-message__finish')).toBeNull();
  });

  it('feeds the retry countdown from retryAfterMs', () => {
    render(
      <AiMessageView
        message={assistant([
          {
            type: 'error',
            code: 'rate-limited',
            message: 'Slow',
            retryable: true,
            retryAfterMs: 5000,
          },
        ])}
        onRetry={() => {}}
      />,
    );
    expect(screen.getByRole('button', { name: /Try again in 5s/ })).toBeDisabled();
  });

  it('marks model text dir="auto"', () => {
    render(<AiMessageView message={assistant([{ type: 'text', text: 'مرحبا', state: 'done' }])} />);
    expect(document.querySelector('.mn-ai-message__text')).toHaveAttribute('dir', 'auto');
  });
});

describe('AiMessageView: error announcements', () => {
  it('announces an error part once, not again when the message remounts', () => {
    const message = assistant([
      { type: 'error', code: 'network', message: 'down', retryable: true },
    ]);
    const first = render(<AiMessageView message={message} />);
    expect(screen.getByRole('alert')).toHaveTextContent('Connection lost');
    first.unmount();
    render(<AiMessageView message={message} />);
    expect(screen.queryByRole('alert')).toBeNull();
  });
});

describe('useAiMessageWiring', () => {
  it('keeps callback identity across renders and calls the latest conversation', () => {
    const { result, rerender } = renderHook(() => {
      const chat = useAiConversation({ transport: mockTransport({ reply: [], delayMs: 0 }) });
      return { chat, wire: useAiMessageWiring(chat) };
    });
    const a = result.current.wire(assistant([]));
    rerender();
    const b = result.current.wire(assistant([]));
    expect(b.onRetry).toBe(a.onRetry);
    expect(b.onApproveTool).toBe(a.onApproveTool);
    expect(b.onRegenerate).toBe(a.onRegenerate);
  });
});

describe('AiMessageView: memory, queued messages and resumable errors', () => {
  it('renders memory parts as chips that say what happened', async () => {
    const onForgetMemory = vi.fn();
    const message = assistant([
      { type: 'text', text: 'Noted.', state: 'done' },
      {
        type: 'memory',
        id: 'm1',
        action: 'write',
        text: 'Prefers short answers',
        scope: 'Project',
      },
      { type: 'memory', id: 'm2', action: 'read', text: 'Works in Leeds' },
      { type: 'memory', id: 'm3', action: 'forget', text: '' },
    ]);
    const { container } = render(
      <AiMessageView message={message} onForgetMemory={onForgetMemory} />,
    );
    const chips = container.querySelectorAll('.mn-ai-message__memory');
    expect(chips).toHaveLength(3);
    expect(chips[0]).toHaveTextContent('Remembered: Prefers short answers');
    expect(chips[0]).toHaveTextContent('Project');
    expect(chips[0]).toHaveAttribute('data-action', 'write');
    expect(chips[1]).toHaveTextContent('Used memory: Works in Leeds');
    expect(chips[2]).toHaveTextContent('Forgot: a saved memory');
    // Only the memory that was just saved can be forgotten from the reply.
    const forget = screen.getAllByRole('button', { name: /Forget memory/ });
    expect(forget).toHaveLength(1);
    await userEvent.click(forget[0]!);
    expect(onForgetMemory).toHaveBeenCalledWith('m1', message);
    await expectNoA11yViolations();
  });

  it('memory strings can be translated and renderPart can replace the chip', () => {
    const message = assistant([{ type: 'memory', id: 'm1', action: 'write', text: 'Tea' }]);
    const { rerender } = render(
      <AiMessageView message={message} messages={{ memoryActions: { write: 'Gemerkt:' } }} />,
    );
    expect(screen.getByText('Gemerkt:')).toBeInTheDocument();
    rerender(
      <AiMessageView
        message={message}
        renderPart={(part) => (part.type === 'memory' ? <b>custom memory</b> : undefined)}
      />,
    );
    expect(screen.getByText('custom memory')).toBeInTheDocument();
    expect(screen.queryByText('Remembered:')).toBeNull();
  });

  it('marks a queued message, hides its toolbar and offers Cancel', async () => {
    const onCancelQueued = vi.fn();
    const message = user('Send this later', { queued: true });
    render(<AiMessageView message={message} onCancelQueued={onCancelQueued} onEdit={() => {}} />);
    expect(screen.getByRole('article')).toHaveAttribute('data-queued', 'true');
    expect(screen.getByText('Queued. It will send when you are back online.')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Edit' })).toBeNull();
    await userEvent.click(screen.getByRole('button', { name: 'Cancel' }));
    expect(onCancelQueued).toHaveBeenCalledWith(message);
    await expectNoA11yViolations();
    cleanup();
    render(<AiMessageView message={user('Sent')} onCancelQueued={onCancelQueued} />);
    expect(screen.getByRole('article')).not.toHaveAttribute('data-queued');
    expect(screen.queryByRole('button', { name: 'Cancel' })).toBeNull();
  });

  it('offers Resume only on an error that can be resumed', async () => {
    const onResume = vi.fn();
    const resumable = assistant([
      { type: 'text', text: 'Half an ans', state: 'done' },
      { type: 'error', code: 'network', message: 'dropped', retryable: true, resumable: true },
    ]);
    render(<AiMessageView message={resumable} onResume={onResume} onRetry={() => {}} />);
    await userEvent.click(screen.getByRole('button', { name: 'Resume' }));
    expect(onResume).toHaveBeenCalledWith(resumable);
    expect(screen.getByRole('button', { name: 'Try again' })).toBeInTheDocument();
    cleanup();
    render(
      <AiMessageView
        message={assistant([
          { type: 'error', code: 'network', message: 'dropped', retryable: true },
        ])}
        onResume={onResume}
        messages={{ resume: 'Fortsetzen' }}
      />,
    );
    expect(screen.queryByRole('button', { name: 'Fortsetzen' })).toBeNull();
  });

  it('useAiMessageWiring wires Resume and Cancel to the conversation', () => {
    const { result } = renderHook(() => {
      const chat = useAiConversation({ transport: mockTransport({ reply: [], delayMs: 0 }) });
      return useAiMessageWiring(chat);
    });
    const props = result.current(assistant([]));
    expect(props.onResume).toBeTypeOf('function');
    expect(props.onCancelQueued).toBeTypeOf('function');
  });
});

describe('AiMessageView timestamp', () => {
  it('shows the time in the given locale and time zone', () => {
    const { container } = render(
      <AiMessageView message={user('Hello')} locale="en-GB" timeZone="Asia/Kolkata" />,
    );
    // 10:15 UTC is 15:45 in India.
    expect(container.textContent).toContain('15:45');
    expect(container.textContent).not.toMatch(/PM|AM/);
  });

  it('pads the hour on a 24-hour clock', () => {
    const { container } = render(
      <AiMessageView message={user('Hello')} locale="en-GB" timeZone="Europe/London" />,
    );
    // 10:15 UTC on 2 March is 10:15 GMT; a single-digit hour is padded below.
    expect(container.textContent).toContain('10:15');
    cleanup();
    const early = render(
      <AiMessageView
        message={user('Hello', { createdAt: '2026-03-02T09:05:00.000Z' })}
        locale="en-GB"
        timeZone="UTC"
      />,
    );
    expect(early.container.textContent).toContain('09:05');
  });
});
