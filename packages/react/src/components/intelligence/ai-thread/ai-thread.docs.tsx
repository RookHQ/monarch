import { useState } from 'react';
import { Button } from '@rookcom/monarch-react';
import {
  AiThread,
  messageText,
  mockTransport,
  textDeltas,
  useAiConversation,
  type AiMessage,
} from '@rookcom/monarch-react/ai';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const transport = mockTransport({
  delayMs: 60,
  reply: ({ messages }) => [
    { type: 'agent', agent: { name: 'Assistant', role: 'Writing assistant' } },
    { type: 'reasoning-delta', text: 'The person wants a short answer with next steps.' },
    { type: 'reasoning-end', durationMs: 2000 },
    ...textDeltas(
      `You asked: “${messageText(messages[messages.length - 1] ?? { parts: [] })}”. This reply streams from a mock transport. Scroll up while it streams to see Jump to latest, or press Escape in the thread to stop it.`,
    ),
    { type: 'done' },
  ],
});

function ChatDemo() {
  const chat = useAiConversation({ transport });
  const [editing, setEditing] = useState<string | null>(null);
  const prompts = [
    'Summarise the meeting notes',
    'Draft a reply to the team',
    'List the open tasks',
  ];
  return (
    <div style={{ display: 'grid', gap: 'var(--mn-space-3)', width: '100%', maxWidth: '44rem' }}>
      <div style={{ height: 360, border: '1px solid var(--mn-color-border-subtle)' }}>
        <AiThread
          messages={chat.messages}
          status={chat.status}
          error={chat.error}
          onStop={chat.stop}
          empty={
            <div style={{ display: 'grid', gap: 'var(--mn-space-2)', justifyItems: 'center' }}>
              <p style={{ margin: 0 }}>What can I help with?</p>
              <div style={{ display: 'flex', flexWrap: 'wrap', gap: 'var(--mn-space-2)' }}>
                {prompts.map((p) => (
                  <Button key={p} size="sm" onClick={() => chat.send(p)}>
                    {p}
                  </Button>
                ))}
              </div>
            </div>
          }
          messageProps={(m: AiMessage) => ({
            agent: chat.agent,
            onRegenerate: (msg) => chat.regenerate(msg.id),
            onFeedback: (f, msg) => chat.setFeedback(msg.id, f),
            onBranch: (i, msg) => chat.branchTo(msg.id, i),
            onEdit: (msg) => setEditing(msg.id),
            editing: editing === m.id,
            onEditSubmit: (text, msg) => {
              setEditing(null);
              chat.editAndResend(msg.id, text);
            },
            onEditCancel: () => setEditing(null),
          })}
        />
      </div>
      <div style={{ display: 'flex', gap: 'var(--mn-space-2)' }}>
        <Button
          variant="primary"
          onClick={() => chat.send('Plan the next sprint')}
          disabled={chat.busy}
        >
          Send a message
        </Button>
        <Button onClick={chat.stop} disabled={!chat.busy}>
          Stop
        </Button>
      </div>
    </div>
  );
}

const DAY = 86_400_000;
const BASE = Date.parse('2026-03-02T09:00:00.000Z');
const HISTORY: AiMessage[] = Array.from({ length: 12 }, (_, i) => ({
  id: `h${i}`,
  role: i % 2 ? 'assistant' : 'user',
  createdAt: new Date(BASE + (i < 6 ? 0 : DAY) + i * 60_000).toISOString(),
  agent: i % 2 ? { name: 'Assistant' } : undefined,
  parts: [
    {
      type: 'text',
      text:
        i % 2
          ? `Answer number ${(i + 1) / 2}, with enough text to wrap a little.`
          : `Question ${i / 2 + 1}?`,
      state: 'done',
    },
  ],
}));

function EarlierDemo() {
  const [count, setCount] = useState(4);
  return (
    <div
      style={{
        height: 320,
        width: '100%',
        maxWidth: '44rem',
        border: '1px solid var(--mn-color-border-subtle)',
      }}
    >
      <AiThread
        messages={HISTORY.slice(-count)}
        hasEarlier={count < HISTORY.length}
        onLoadEarlier={() => setCount((c) => Math.min(HISTORY.length, c + 4))}
        now={BASE + DAY}
      />
    </div>
  );
}

const LONG: AiMessage[] = Array.from({ length: 600 }, (_, i) => ({
  id: `l${i}`,
  role: i % 2 ? 'assistant' : 'user',
  createdAt: new Date(BASE + i * 30_000).toISOString(),
  parts: [{ type: 'text', text: `Message ${i + 1} of 600.`, state: 'done' }],
}));

export default {
  id: 'ai-thread',
  title: 'AiThread',
  summary:
    'The scrolling, bottom-pinned list of an AI conversation, with one screen-reader announcer.',
  sections: {
    purpose: {
      summary:
        'Shows a conversation from `useAiConversation` oldest first: day labels, auto-scroll that respects the reader, "Jump to latest", "Load earlier", a thinking indicator and sentence-level announcements for streamed replies.',
      useWhen: [
        'An assistant panel or page shows a back-and-forth conversation.',
        'Replies stream in and readers may scroll back while they do.',
      ],
      avoidWhen: [
        'A single answer is shown without a conversation: use AiStreamingText.',
        'People chat with people: use the collaboration MessageRow and related components.',
      ],
    },
    designPrinciples: {
      items: [
        'The reader is in charge of the scroll position: it follows new content only while they are at the bottom.',
        'One announcer per conversation: screen readers hear whole sentences, never tokens, then "Response complete".',
        'Never steals focus while a reply streams.',
        'Escape stops a running reply from anywhere in the thread; Stop is also a visible button in the prompt input.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'The ref goes to the root `<div>`; the scroller inside it has `role="log"`.',
        'String props are in `labels` (not `messages`, which is the conversation).',
        '`renderMessage(message, { index, isLast, announce })` replaces the default `AiMessageView`; pass `announce` to any streaming text you render.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [
        {
          prop: 'announce',
          values: ['true', 'false'],
          description:
            'true (default): the thread owns the polite and assertive live regions and messages stay quiet. false: no thread announcer; each message announces itself.',
        },
        {
          prop: 'daySeparators',
          values: ['true', 'false'],
          description:
            'Day labels ("Today", "Yesterday", a date) before the first message of each day.',
        },
        {
          prop: 'virtualizeAfter',
          values: ['number'],
          description:
            'Above this many messages (default 200) only the rows near the viewport and the last `keepLast` are rendered (`data-virtualized`).',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'At the bottom',
          attribute: 'data-at-bottom',
          description: 'New content keeps the view at the end.',
        },
        {
          name: 'Scrolled up',
          description:
            'A "Jump to latest" button appears, with the count of messages that arrived since.',
        },
        {
          name: 'Submitted',
          attribute: 'data-status="submitted"',
          description:
            'A thinking indicator follows the last message; "Thinking…" is announced once.',
        },
        {
          name: 'Streaming',
          attribute: 'data-status="streaming", aria-busy="true" on the log',
          description: 'Text is announced at sentence boundaries, at most once a second.',
        },
        {
          name: 'Empty',
          description: 'The `empty` slot is shown, e.g. a welcome with suggested prompts.',
        },
        {
          name: 'Windowed',
          attribute: 'data-virtualized',
          description: 'Long threads render only nearby rows between spacer elements.',
        },
      ],
    },
    interaction: {
      items: [
        'Scrolling up releases the bottom pin; "Jump to latest" or scrolling back down restores it.',
        '"Load earlier messages" calls `onLoadEarlier`; when older messages are prepended the visible messages stay where they were.',
        'Choosing "Jump to latest" with the keyboard moves focus to the log, because the button disappears.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Tab',
          action: 'Focuses the log (so it can be scrolled), then the controls inside messages.',
        },
        { keys: 'Arrow keys, Page Up / Page Down, Home / End', action: 'Scroll the focused log.' },
        {
          keys: 'Escape',
          action:
            'Calls `onStop` while a reply is submitted or streaming (not while editing a message).',
        },
      ],
    },
    accessibility: {
      role: 'log (with aria-live="off"; announcements go through the thread’s own regions)',
      name: '`labels.label` ("Conversation"), or `aria-label` / `aria-labelledby`.',
      items: [
        'A role="log" is implicitly live; it is set to `aria-live="off"` so streamed tokens are not read one by one.',
        'One polite `role="status"` region speaks finished sentences, throttled by `announceInterval`, then "Response complete" or "Response stopped".',
        'Messages already in the history are never read out; only replies that stream while the thread is mounted.',
        'Conversation errors are announced assertively only when no message shows them; an error part announces itself through AiErrorMessage.',
        'Each message carries a visually hidden "Assistant said:" / "You said:" prefix (see AiMessageView).',
        'Windowing limits: rows outside the window are not in the DOM, so find-in-page and a screen reader’s virtual cursor only reach rendered rows; heights are estimated until measured, so the scrollbar can shift slightly.',
      ],
    },
    responsive: {
      items: [
        'Fills its container’s height; content is limited to the reading width and centred.',
        'At narrow widths padding tightens; at 390px nothing scrolls horizontally.',
        'The jump button is 44px tall on coarse pointers.',
      ],
    },
    theme: {
      items: [
        'Uses surface, border and text tokens; the new-message count uses `--mn-color-ai-strong`.',
        'Forced colours give the jump button and day rules system colours.',
      ],
    },
    examples: {
      items: [
        { title: 'Live conversation with mockTransport', stack: true, render: () => <ChatDemo /> },
        { title: 'Load earlier messages, day labels', stack: true, render: () => <EarlierDemo /> },
        {
          title: 'Long thread (600 messages, windowed)',
          stack: true,
          render: () => (
            <div
              style={{
                height: 320,
                width: '100%',
                maxWidth: '44rem',
                border: '1px solid var(--mn-color-border-subtle)',
              }}
            >
              <AiThread messages={LONG} />
            </div>
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Give the thread a constrained height (a flex child or fixed height).',
        'Pass `status`, `error` and `onStop` from `useAiConversation`.',
        'Keep messages’ own announcers off when the thread announces (the default renderer does).',
      ],
      dont: [
        'Do not wrap it in another live region.',
        'Do not scroll it from your code on every token; the anchor already follows growth.',
      ],
    },
    related: {
      items: [
        { id: 'ai-message', reason: 'The default row renderer.' },
        { id: 'ai-scroll-anchor', reason: 'The headless bottom-pinning hook it is built on.' },
        { id: 'ai-prompt-input', reason: 'The composer that usually sits under it.' },
        { id: 'ai-thinking-indicator', reason: 'Shown while a reply is submitted.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: [
        'button',
        'ai-conversation',
        'ai-scroll-anchor',
        'ai-message',
        'ai-thinking-indicator',
      ],
    },
    testing: {
      unit: [
        'Labelled focusable log with aria-live off and day separators.',
        'Stays pinned to the bottom as messages arrive (mocked scroll metrics).',
        '"Jump to latest" appears when scrolled up, counts new messages, scrolls back and keeps focus in the log.',
        'Prepending earlier messages keeps the scroll position.',
        'Windowing renders a subset near the viewport and always the newest; short threads are not windowed.',
        'Escape calls onStop only while a reply runs.',
        'Thinking indicator while submitted.',
        'Sentence-boundary, throttled announcements then "Response complete" (fake timers); history is not read; errors are assertive.',
        'End to end with useAiConversation and mockTransport.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
