import { Button } from '@rookcom/monarch-react';
import {
  AiMessageView,
  AiReasoning,
  mockTransport,
  textDeltas,
  useAiConversation,
  type AiStreamEvent,
} from '@rookcom/monarch-react/ai';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const reasoningDeltas = (text: string): AiStreamEvent[] =>
  textDeltas(text).map((e) => ({
    type: 'reasoning-delta',
    text: e.type === 'text-delta' ? e.text : '',
  }));

const transport = mockTransport({
  delayMs: 60,
  reply: [
    ...reasoningDeltas(
      'The question asks for the main risks. First list what the notes say about timing, then budget, then staffing. Keep only risks with an owner.',
    ),
    { type: 'reasoning-end' },
    ...textDeltas(
      'There are **two main risks**: the review date may slip, and one role is still unfilled.',
    ),
    { type: 'done' },
  ],
});

function ConversationDemo() {
  const { messages, send, reset, busy } = useAiConversation({ transport });
  return (
    <div style={{ display: 'grid', gap: 'var(--mn-space-3)' }}>
      <div>
        <Button
          size="sm"
          disabled={busy}
          onClick={() => {
            reset();
            send('What are the main risks in the planning notes?');
          }}
        >
          Ask the assistant
        </Button>
      </div>
      {messages.map((message) => (
        <AiMessageView
          key={message.id}
          message={message}
          showActions={false}
          renderPart={(part) =>
            part.type === 'reasoning' ? <AiReasoning part={part} /> : undefined
          }
        />
      ))}
    </div>
  );
}

export default {
  id: 'ai-reasoning',
  title: 'AiReasoning',
  summary: 'The model’s reasoning as a disclosure: live "Thinking…", then "Thought for 12s".',
  sections: {
    purpose: {
      summary:
        'Shows that the model is reasoning and lets people inspect how it got to an answer, without the reasoning competing with the answer itself. It replaces the plain reasoning row of AiMessageView when you want live streaming, measured durations and localised durations.',
      useWhen: [
        'A message has an `AiReasoningPart` (the transport sends `reasoning-delta` events).',
        'Through `AiMessageView renderPart` for `part.type === "reasoning"`, or on its own in a custom layout.',
      ],
      avoidWhen: [
        'Nothing has arrived yet and there is no reasoning part: use AiThinkingIndicator.',
        'Showing the steps an agent takes (tools, actions): use AiChainOfSteps or AiToolCall.',
      ],
    },
    designPrinciples: {
      items: [
        'Open while the model reasons, so the wait has visible progress; collapsed once the answer starts, so the answer is what people read.',
        'Honest about time: the duration comes from the part, or is measured from the stream when the transport does not send one.',
        'Withheld reasoning is said plainly ("Reasoning hidden") with nothing to expand.',
        'Marked with the Bot glyph and the AI tokens, never sparkles.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'The ref goes to the root `<div>`; native `<div>` attributes pass through.',
        '`formatReasoningDuration(ms, locale?)` is exported and is the default `messages.duration`.',
        'Plug into a message with `renderPart={(part) => part.type === "reasoning" ? <AiReasoning part={part} /> : undefined}`.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [],
      notes: ['No variant prop. The look follows `part.state` and `part.redacted` (see States).'],
    },
    states: {
      items: [
        {
          name: 'Streaming',
          attribute: 'data-state="streaming"',
          description:
            'Summary "Thinking…" with a shimmer; open, with the text streaming in through AiStreamingText (caret, `aria-busy`).',
        },
        {
          name: 'Done',
          attribute: 'data-state="done"',
          description:
            'Collapses automatically to "Thought for 12s" (or "Thought" when no duration is known), unless the person toggled it while it streamed.',
        },
        {
          name: 'Open',
          attribute: 'data-open',
          description: 'Set while the disclosure is open.',
        },
        {
          name: 'Redacted',
          attribute: 'data-redacted',
          description:
            '"Reasoning hidden" as a static label with no body; "Thinking…" while it still runs.',
        },
      ],
    },
    interaction: {
      items: [
        'Click the summary to open or close the reasoning; `onOpenChange` reports every change, including the automatic collapse.',
        'If the person opens or closes it while it streams, it stays as they left it when the reasoning finishes.',
        'A regeneration that streams reasoning again reopens it.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Moves focus to the summary.' },
        { keys: 'Enter / Space', action: 'Opens or closes the reasoning (native `<details>`).' },
      ],
    },
    accessibility: {
      role: 'group (native `<details>` / `<summary>` disclosure)',
      name: 'The summary text, prefixed with a visually hidden "Assistant reasoning:"',
      items: [
        'Uses native `<details>`/`<summary>`, so the expanded state is exposed without extra ARIA.',
        'AI provenance: the visually hidden prefix "Assistant reasoning:" is the approach (not `aria-roledescription`), consistent with AiMessageView’s "Assistant said:".',
        'Reasoning never speaks on its own: AiStreamingText runs with `announce={false}`, so screen readers hear the answer, not every reasoning sentence. The body is `aria-busy` while it streams.',
        'The shimmer only moves a background band; text colour and contrast never change. It stops under reduced motion and in forced colours.',
        'Model text is rendered through AiMarkdown (no raw HTML; links are checked and get `rel="noopener noreferrer nofollow"`).',
      ],
    },
    responsive: {
      items: [
        'Fills its container; long summaries and reasoning wrap (`overflow-wrap: anywhere`).',
        'The summary grows to a 44px touch target on coarse pointers.',
        'RTL: logical properties throughout; the chevron mirrors.',
      ],
    },
    theme: {
      items: [
        'Summary in `--mn-color-text-muted`; chevron and Bot glyph in `--mn-color-ai-strong`.',
        'Shimmer band in `--mn-color-ai-thinking` over `--mn-duration-shimmer`.',
        'Body rule in `--mn-color-ai-border`. Forced colours use CanvasText.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Finished, collapsed',
          stack: true,
          render: () => (
            <AiReasoning
              part={{
                type: 'reasoning',
                text: 'Compare both drafts, then list what changed in the budget section.',
                state: 'done',
                durationMs: 12_000,
              }}
            />
          ),
        },
        {
          title: 'Open by default',
          stack: true,
          render: () => (
            <AiReasoning
              defaultOpen
              part={{
                type: 'reasoning',
                text: 'The request mentions **three** locations. Check opening hours for each, then pick the earliest common slot.',
                state: 'done',
                durationMs: 75_000,
              }}
            />
          ),
        },
        {
          title: 'Streaming',
          stack: true,
          render: () => (
            <AiReasoning
              part={{
                type: 'reasoning',
                text: 'Start with the totals, then look for',
                state: 'streaming',
              }}
            />
          ),
        },
        {
          title: 'Redacted',
          render: () => (
            <AiReasoning part={{ type: 'reasoning', text: '', state: 'done', redacted: true }} />
          ),
        },
        {
          title: 'Plugged into AiMessageView with renderPart',
          stack: true,
          render: () => <ConversationDemo />,
        },
      ],
    },
    doDont: {
      do: [
        'Render it for every reasoning part through `renderPart`, so streaming and history look the same.',
        'Pass `locale` so durations read naturally ("1 min 5 s" in French).',
      ],
      dont: [
        'Do not open finished reasoning by default in a conversation; it pushes the answer down.',
        'Do not show redacted reasoning text you happen to receive; respect `redacted`.',
      ],
    },
    related: {
      items: [
        {
          id: 'ai-message',
          reason: 'Renders a message’s parts; use `renderPart` to plug this in.',
        },
        { id: 'ai-thinking-indicator', reason: 'Before any reasoning or text arrives.' },
        {
          id: 'ai-chain-of-steps',
          reason: 'Visible steps an agent takes, rather than its reasoning.',
        },
        { id: 'ai-streaming-text', reason: 'Renders the reasoning body.' },
      ],
    },
    dependencies: { packages: ['react'], components: ['ai-streaming-text'] },
    testing: {
      unit: [
        'Finished reasoning renders collapsed with "Thought for 12s" and the hidden prefix; ref and props forwarding.',
        'Opens and closes through the summary and reports `onOpenChange`; `defaultOpen`.',
        'Streaming is open with "Thinking…", `aria-busy` and no live region; collapses on done with a duration measured from the stream.',
        'Stays open on done when the person toggled it while streaming.',
        '"Thought" without a duration; redacted renders "Reasoning hidden" with no body, "Thinking…" while running.',
        'Localised strings and durations; `formatReasoningDuration` for seconds, minutes and both.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
