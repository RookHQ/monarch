import { useState } from 'react';
import {
  AiThinkingIndicator,
  messageText,
  mockTransport,
  useAiConversation,
} from '@rookcom/monarch-react/ai';
import { Button } from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const slow = mockTransport({
  delayMs: 4500,
  reply: [{ type: 'text-delta', text: 'Here is a short summary.' }, { type: 'done' }],
});

function ConversationDemo() {
  const { status, messages, send, reset } = useAiConversation({ transport: slow });
  const last = messages[messages.length - 1];
  const reply = last?.role === 'assistant' ? last : undefined;
  return (
    <div style={{ display: 'grid', gap: 'var(--mn-space-3)', justifyItems: 'start' }}>
      <Button
        size="sm"
        onClick={() => {
          reset();
          send('Summarize the meeting notes.');
        }}
      >
        Ask the assistant
      </Button>
      {status === 'submitted' ? <AiThinkingIndicator /> : null}
      {reply && status !== 'submitted' ? <p>{messageText(reply)}</p> : null}
    </div>
  );
}

function AlreadyWaitingDemo() {
  // Started eight seconds before the indicator mounted; kept stable across renders.
  const [startedAt] = useState(() => Date.now() - 8000);
  return <AiThinkingIndicator label="Searching your files…" startedAt={startedAt} variant="mark" />;
}

export default {
  id: 'ai-thinking-indicator',
  title: 'AiThinkingIndicator',
  summary: 'The "Thinking…" state before an assistant’s first token, with elapsed time.',
  sections: {
    purpose: {
      summary:
        'Shows that the assistant has the request and is working before any text arrives, and after a few seconds how long it has been working, so a slow answer does not look like a broken one.',
      useWhen: [
        'A message was sent and the reply has not streamed its first token (`status === "submitted"`).',
        'A tool or retrieval step is running and you can name it ("Searching your files…").',
      ],
      avoidWhen: [
        'Text is already streaming: show the streaming text and its caret instead.',
        'A dock or list row with no room for a label: use AiTypingDots.',
        'Non-AI loading: use Spinner or LoadingState.',
      ],
    },
    designPrinciples: {
      items: [
        'Quiet and specific: a short label, a soft shimmer, no spinner competing with the conversation.',
        'Honest about time: elapsed seconds appear after 3 seconds, when a wait starts to feel long.',
        'The assistant is identified with the Bot mark used across Monarch AI features.',
        'Motion is decoration only and stops entirely under reduced motion and in forced colours.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Native `<div>` attributes pass through and the ref is forwarded to the root.',
        'Pass `startedAt` (for example the reasoning part’s `startedAt`) so the elapsed time survives a remount. Keep it stable: a new value restarts the clock.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [
        {
          prop: 'variant',
          values: ['shimmer', 'mark'],
          description:
            '`shimmer` (default) sweeps a band of the AI thinking tint behind the label; `mark` shows a pulsing bot mark before a still label. Exposed as `data-variant`.',
        },
      ],
    },
    states: {
      items: [
        { name: 'Thinking', description: 'The label, animated.' },
        {
          name: 'Long wait',
          attribute: 'data-elapsed',
          description:
            'After `showElapsedAfter` seconds (default 3) the elapsed time follows the label ("5s", then "1m 5s") and updates every second.',
        },
        {
          name: 'Static',
          attribute: 'data-static',
          description:
            'Set when the person prefers reduced motion or forced colours are active; shimmer and pulse stop (the CSS media queries also stop them before hydration).',
        },
      ],
    },
    interaction: {
      items: [
        'Not interactive: it appears while the assistant works and the app removes it when text arrives.',
      ],
    },
    keyboard: {
      items: [],
      notes: ['Not focusable and has no keyboard interaction.'],
    },
    accessibility: {
      role: 'status (a visually hidden live region inside the root)',
      name: 'The label, default "Thinking…"',
      items: [
        'The label is announced politely once, shortly after the indicator mounts, and again only if `label` changes.',
        'The elapsed time sits outside the live region, so it never re-announces each second but can still be read in browse mode.',
        'The visible label and bot mark are `aria-hidden`; the hidden status carries the text.',
        'Shimmer and pulse stop under `prefers-reduced-motion: reduce` and `forced-colors: active`; the high-contrast themes also make the AI tints solid.',
      ],
    },
    responsive: {
      items: ['An inline row that wraps the elapsed time below a long label; no fixed width.'],
    },
    theme: {
      items: [
        'Label in `--mn-color-text-muted` with a `--mn-color-ai-thinking` band moving over `--mn-duration-shimmer`; the text colour never changes, so contrast holds.',
        'Bot mark in `--mn-color-ai-strong` on `--mn-color-ai-subtle`; elapsed time in `--mn-color-text-subtle`.',
        'In forced colours everything is CanvasText with no background.',
      ],
    },
    examples: {
      items: [
        { title: 'Shimmer (default)', render: () => <AiThinkingIndicator /> },
        { title: 'Bot mark', render: () => <AiThinkingIndicator variant="mark" /> },
        {
          title: 'Custom label, already waiting',
          render: () => <AlreadyWaitingDemo />,
        },
        { title: 'In a conversation', stack: true, render: () => <ConversationDemo /> },
      ],
    },
    doDont: {
      do: [
        'Show it from send until the first token, then replace it with the reply.',
        'Name the step when you know it ("Reading the attachment…").',
      ],
      dont: [
        'Do not keep it next to streaming text.',
        'Do not change `label` every second; each change is announced.',
      ],
    },
    related: {
      items: [
        { id: 'ai-typing-dots', reason: 'A compact indicator for docks and small surfaces.' },
        { id: 'ai-streaming-text', reason: 'What replaces it once tokens arrive.' },
        { id: 'ai-message', reason: 'Renders it for a submitted assistant turn.' },
        { id: 'spinner', reason: 'Non-AI loading.' },
      ],
    },
    dependencies: { packages: ['react'], components: [] },
    testing: {
      unit: [
        'Announces the label once in a polite status; ten seconds of ticks never change the live region.',
        'Re-announces when the label changes.',
        'Shows elapsed time after three seconds and updates it each second (fake timers), including minutes.',
        'Counts from `startedAt`; honours `showElapsedAfter` and `messages`.',
        'Sets `data-static` under reduced motion and not otherwise.',
        'Bot mark variant is decorative; ref and props forwarding.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
