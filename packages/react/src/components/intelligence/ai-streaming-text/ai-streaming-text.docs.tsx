import { Button } from '@rookcom/monarch-react';
import {
  AiStreamingText,
  isStreaming,
  messageText,
  mockTransport,
  textDeltas,
  useAiConversation,
} from '@rookcom/monarch-react/ai';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const REPLY = `## Summary

The sample project has **three open tasks** and one is overdue. Here is the order I suggest:

1. Review the *migration plan* first, since two tasks depend on it.
2. Update the changelog.
3. Draft the handover notes.

| Task | Due |
| :-- | --: |
| Migration plan | Today |
| Changelog | Friday |

\`\`\`bash
npm run release -- --dry-run
\`\`\`

Let me know if you want the plan as a checklist.`;

const transport = mockTransport({
  delayMs: 35,
  reply: [{ type: 'agent', agent: { name: 'Assistant' } }, ...textDeltas(REPLY), { type: 'done' }],
});

function StreamingDemo({ announce = true }: { announce?: boolean }) {
  const chat = useAiConversation({ transport });
  const answer = [...chat.messages].reverse().find((m) => m.role === 'assistant');
  return (
    <div style={{ display: 'grid', gap: 'var(--mn-space-3)', width: '100%' }}>
      <div style={{ display: 'flex', gap: 'var(--mn-space-2)', flexWrap: 'wrap' }}>
        <Button
          variant="primary"
          disabled={chat.busy}
          onClick={() => {
            chat.reset();
            chat.send('Summarise the open tasks');
          }}
        >
          Stream an answer
        </Button>
        <Button onClick={chat.stop} disabled={!chat.busy}>
          Stop
        </Button>
      </div>
      {answer ? (
        <AiStreamingText
          text={messageText(answer)}
          streaming={isStreaming(answer)}
          announce={announce}
        />
      ) : (
        <p style={{ margin: 0, fontSize: 'var(--mn-text-xs)' }}>
          Press the button to stream a sample answer.
        </p>
      )}
    </div>
  );
}

export default {
  id: 'ai-streaming-text',
  title: 'AiStreamingText',
  summary:
    'Renders a Markdown answer while it streams: finished blocks stay put, half-written syntax never flashes, and screen readers hear whole sentences.',
  sections: {
    purpose: {
      summary:
        'Shows an AI answer as it arrives without flicker or layout jumps, and tells screen-reader users about it at a humane pace. Finished blocks are parsed and rendered once; only the last block updates per token. Needed by: AiMessageView, AiThread.',
      useWhen: [
        'Rendering the text of an assistant message while it streams, and after it finishes.',
        'Any growing Markdown string (a live summary, a transcript).',
      ],
      avoidWhen: [
        'The text is complete and will not change: AiMarkdown is enough.',
        'Content people type themselves: use RichTextEditor or MessageComposer.',
      ],
    },
    designPrinciples: {
      items: [
        'Stream into a visible node, announce at sentence and paragraph boundaries, never per token.',
        'Nothing flashes: open bold, italics, strikethrough and code spans are closed for display, a link shows its text until its target arrives, table rows wait for the delimiter row, and an unclosed fence is already a code block.',
        'Completed blocks are memoised by identity (incremental parsing), so long answers stay fast.',
        'The caret is decorative and static under reduced motion.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'AiStreamingText forwards its ref to the root `<div>`. Inside it: an AiMarkdown (`.mn-ai-streaming-text__content`, which carries `aria-busy`) and, unless `announce={false}`, a visually hidden polite live region.',
        '`completeStreamingMarkdown(src)` is the display repair applied to the last block; it is exported for custom renderers.',
        'Markdown options (`headingLevelOffset`, `allowImages`, `externalLinksInNewTab`, `renderCode`, `codeBlockProps`) are passed to AiMarkdown; `markdownMessages` becomes its `messages`.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Keep `renderCode` and `codeBlockProps` stable so finished blocks are not re-rendered.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'announce',
          values: ['true', 'false'],
          description:
            'Turn off the built-in announcements when the thread announces messages itself. Default true.',
        },
        {
          prop: 'caret',
          values: ['true', 'false'],
          description: 'Show the caret at the end while streaming. Default true.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Streaming',
          attribute: 'data-streaming',
          description:
            'Text is arriving: caret shown, `aria-busy="true"` on the content, sentences announced.',
        },
        {
          name: 'Done',
          description:
            '`streaming` became false: the caret goes, the rest of the text and "Response complete" are announced once.',
        },
        {
          name: 'History',
          description: 'Rendered with `streaming={false}` from the start: nothing is announced.',
        },
      ],
    },
    interaction: {
      items: [
        'New complete sentences (ending in . ! ? …) and finished paragraphs are announced through a polite live region, at most once per `announceInterval` (minimum and default 1000ms); text that arrives in between is combined.',
        'A code block or table is announced by name ("Code block", "Table"), not read out.',
        'When the text no longer starts with what was announced (regenerate), announcements start over.',
        'Never moves focus; links and code controls in finished blocks stay usable while the rest streams.',
      ],
    },
    keyboard: {
      items: [],
      notes: [
        'Not interactive itself. Links and code block controls inside it work as in AiMarkdown; Stop and Escape belong to the thread or input (AiStopButton, AiPromptInput).',
      ],
    },
    accessibility: {
      role: 'None on the root; a `role="status"` polite live region sits outside the busy content.',
      name: 'None; the containing message names and identifies the answer as AI-generated (AiMessageView).',
      items: [
        '`aria-busy="true"` on the content while streaming.',
        'The live region is outside the `aria-busy` subtree, so screen readers do not hold announcements back.',
        'Announcements are throttled to one per second or slower and only contain complete sentences or paragraphs.',
        '"Response complete" (`messages.complete`) is announced when streaming ends.',
        'The caret is `aria-hidden` and does not blink under `prefers-reduced-motion: reduce`.',
      ],
    },
    responsive: {
      items: [
        'Same as AiMarkdown: tables and code scroll inside their own regions at any width.',
        'The caret takes no width, so lines do not re-wrap when it disappears.',
      ],
    },
    theme: {
      items: [
        'The caret uses `--mn-color-ai-solid` and blinks at `--mn-duration-stream-caret`; CanvasText in forced colours.',
        'All other colours come from AiMarkdown and AiCodeBlock.',
      ],
    },
    examples: {
      items: [
        { title: 'Streaming from a mock transport', stack: true, render: () => <StreamingDemo /> },
        {
          title: 'A finished answer (nothing is announced)',
          stack: true,
          render: () => <AiStreamingText text={REPLY} streaming={false} />,
        },
        {
          title: 'Half-written syntax while streaming',
          stack: true,
          render: () => (
            <AiStreamingText text={'Open **bold, a [link to the gu'} streaming announce={false} />
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Pass the whole text received so far, not the latest delta.',
        'Set `announce={false}` when a surrounding thread already announces new messages.',
        'Keep rendering through AiStreamingText after the stream ends, so the finished blocks are not rebuilt.',
      ],
      dont: [
        'Do not move focus into the answer while it streams.',
        'Do not put your own live region around it; it would read every token.',
        'Do not pass a new `renderCode` function each render.',
      ],
    },
    related: {
      items: [
        {
          id: 'ai-markdown',
          reason: 'The renderer it uses; enough for text that is already complete.',
        },
        { id: 'ai-message', reason: 'Wraps it in a message that is identified as AI-generated.' },
        { id: 'ai-conversation', reason: 'Provides the streaming messages (`useAiConversation`).' },
        { id: 'ai-thinking-indicator', reason: 'Shown before the first token arrives.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['ai-markdown'],
      notes: ['Tokens: `--mn-color-ai-solid`, `--mn-duration-stream-caret`.'],
    },
    testing: {
      unit: [
        'Closes open emphasis and code spans, hides partial links and lone delimiters, leaves open fences alone.',
        'Holds table rows back until the delimiter row arrives.',
        'Only the last block re-renders as text grows (render counter through `renderCode`, DOM identity).',
        'A half-open fence renders as code with Apply disabled until it is complete.',
        'Caret and `aria-busy` only while streaming.',
        'Announces complete sentences at most once a second, then the rest and "Response complete" (fake timers).',
        'Finished paragraphs are announced; code blocks are announced by name.',
        'Nothing is announced for history or with `announce={false}`.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
