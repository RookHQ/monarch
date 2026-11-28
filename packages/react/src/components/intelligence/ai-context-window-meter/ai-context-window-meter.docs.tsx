import { useState } from 'react';
import { AiContextWindowMeter } from '@rookcom/monarch-react/ai';
import type { ComponentDocs } from '../../../internal/component-docs.js';
// The scenario of the primary example, shared with iOS and Android (CROSS-PLATFORM.md section 8).
import fixture from './ai-context-window-meter.fixture.json';

function SummarizeDemo() {
  const [used, setUsed] = useState(110000);
  const [summarizing, setSummarizing] = useState(false);
  return (
    <div style={{ maxWidth: '24rem' }}>
      <AiContextWindowMeter
        used={used}
        limit={128000}
        breakdown={[
          { label: 'Instructions', tokens: 3000 },
          { label: 'Attachments', tokens: 22000 },
          { label: 'Messages', tokens: used - 25000 },
        ]}
        summarizing={summarizing}
        onSummarize={() => {
          setSummarizing(true);
          setTimeout(() => {
            setUsed(40000);
            setSummarizing(false);
          }, 800);
        }}
      />
    </div>
  );
}

export default {
  id: 'ai-context-window-meter',
  title: 'AiContextWindowMeter',
  summary: 'How full the model’s context window is, what fills it, and a way to summarize.',
  sections: {
    purpose: {
      summary:
        'Shows how much of the model’s context window a conversation uses and what it is made of, warns before earlier messages start being left out, and offers "Summarize earlier messages" to make room.',
      useWhen: [
        'Long conversations or large attachments can exceed the model’s context window.',
        'People should understand why the assistant may forget earlier parts of a conversation.',
        'Your backend can summarize earlier messages to free room.',
      ],
      avoidWhen: [
        'You are showing quota or billing usage: use AiUsageMeter.',
        'You only need to list what the AI can see: use AiContextChips.',
      ],
    },
    designPrinciples: {
      items: [
        'Show the parts (instructions, files, messages) so people know what to remove.',
        'Warn in plain words before content is dropped, not after.',
        'The summarize action is offered, never run automatically.',
        'Tokens not covered by the breakdown are shown as "Other", so the parts always add up to the total.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Native `<div>` attributes pass through and the ref is forwarded to the root.',
        '`aiContextWindowState(used, limit, warnAt?)` returns `ok`, `warning` or `full`.',
        'The bar is a Progress meter with `segments` when `breakdown` is given.',
        '`useAiContextUsage({ messages, limit, countTokens?, base?, perMessageTokens?, useReported?, labels? })` works the count out from a conversation, and the meter takes its result as `usage` (an explicit `used`, `limit` or `breakdown` still wins). When the latest reply reported `usage` in the stream, that figure (input plus output tokens) is the starting point and only newer messages are counted; its cached and reasoning tokens appear in the breakdown.',
        '`countTokens(text)` is your tokenizer and may return a promise. Each message is counted once and cached while the message object does not change; a reply that is still streaming is estimated, so the tokenizer is not called for every token. A counter that throws or rejects falls back to the estimate.',
        'Without `countTokens` the default is `estimateAiTokens(text)`: one token for every four characters and one for each Chinese, Japanese or Korean character. It is an approximation, good to about a fifth either way for English prose and worse for code, numbers and other scripts. The result then has `approximate: true` and the meter says "About …" (`messages.approximate`), with `data-approximate` on the root.',
        '`aiMessageTokenText(message)` is the text that is counted: text, visible reasoning, tool names with arguments and results, source titles, plan steps and memories. Attached files count by name only; add what they cost through `base`. Queued messages are not counted until they are sent.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [
        {
          prop: 'compact',
          values: ['false', 'true'],
          description:
            '`true` hides the heading, summary line and legend for a toolbar; the bar keeps its name and value text. Exposed as `data-compact`.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Approximate',
          attribute: 'data-approximate; data-source="reported|counted|estimated"',
          description:
            'With `usage`: the count is an estimate (the default estimator, a reply still streaming, or a count on its way), so the summary line and the bar’s value text read "About …".',
        },
        { name: 'Room left', attribute: 'data-state="ok"', description: 'No notice.' },
        {
          name: 'Nearly full',
          attribute: 'data-state="warning"',
          description: 'At or above `warnAt`: a warning notice describes the bar.',
        },
        {
          name: 'Full',
          attribute: 'data-state="full"',
          description: 'At or over the limit: a danger notice says earlier messages are left out.',
        },
        {
          name: 'Summarizing',
          attribute: 'aria-busy="true" (button)',
          description:
            'The summarize button shows a spinner and ignores clicks while keeping focus.',
        },
      ],
    },
    interaction: {
      items: [
        '"Summarize earlier messages" calls `onSummarize`; set `summarizing` while it runs and update `used` when it finishes.',
        'The button is quieter (ghost) while there is room and becomes secondary once the meter warns.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab, Enter / Space', action: 'Focus and press "Summarize earlier messages".' },
      ],
      notes: ['Without `onSummarize` nothing is focusable.'],
    },
    accessibility: {
      role: 'meter',
      name: '"Context window" (the `label` message)',
      items: [
        '`aria-valuetext` reads the summary, e.g. "45,000 of 128,000 tokens (35%)".',
        'The warning is in a polite status region, so it is announced when the state changes, and it is also the meter’s description.',
        'The legend is a named list; swatches are decorative and every part is named in text.',
      ],
    },
    responsive: {
      items: [
        'The header and legend wrap; no horizontal scroll at 390px. The button keeps the Button touch target on coarse pointers.',
      ],
    },
    theme: {
      items: [
        'Segments and swatches use the data palette `--mn-color-data-1` … `-5` in order; "Other" uses the neutral tone.',
        'Notices use `--mn-color-warning-strong` and `--mn-color-danger-strong`.',
        'In forced colours swatches get a CanvasText outline and notices use CanvasText.',
      ],
    },
    examples: {
      items: [
        {
          title: 'With a breakdown',
          stack: true,
          render: () => (
            <div style={{ maxWidth: '24rem' }}>
              <AiContextWindowMeter
                used={fixture.data.used}
                limit={fixture.data.limit}
                breakdown={fixture.data.breakdown}
              />
            </div>
          ),
        },
        {
          title: 'Nearly full, with summarize',
          stack: true,
          render: () => <SummarizeDemo />,
        },
        {
          title: 'Compact',
          stack: true,
          render: () => (
            <div style={{ maxWidth: '12rem' }}>
              <AiContextWindowMeter used={64000} limit={128000} compact />
            </div>
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Use the model’s real context size and token counts.',
        'Offer summarize before the window is full.',
        'Update `used` after summarizing so people see the result.',
        'Pass the model’s real tokenizer as `countTokens` where the number decides something, such as blocking a send or starting a summary.',
      ],
      dont: [
        'Do not summarize without the person asking.',
        'Do not show it for features with a fixed, short context.',
        'Do not use it for billing quotas.',
        'Do not bill, block or truncate on the default estimate: it is labelled approximate because it is.',
      ],
    },
    related: {
      items: [
        { id: 'ai-usage-meter', reason: 'Quota and billing usage per scope.' },
        { id: 'ai-context-chips', reason: 'What the AI will see, item by item.' },
        {
          id: 'ai-context-divider',
          reason: 'Marks where earlier messages were summarized in the thread.',
        },
        { id: 'progress-bar', reason: 'The segmented meter it is built on.' },
      ],
    },
    dependencies: { packages: ['react'], components: ['button', 'progress-bar'] },
    testing: {
      unit: [
        'aiContextWindowState maps usage to ok, warning and full.',
        'Named meter with value text; ref and prop forwarding.',
        'Breakdown segments in palette order with a legend, plus "Other" for uncovered tokens.',
        'Warning at warnAt and full state in the status region and as the meter description; cleared when back within range.',
        'Summarize button by keyboard, pending state; hidden without onSummarize; compact mode; localised strings.',
        '`estimateAiTokens` and `aiMessageTokenText`; `useAiContextUsage` estimates by default and says so, adds `base` and the per-message overhead, counts each message once with a synchronous counter, shows the estimate until an asynchronous counter answers, never sends a streaming reply to the counter, falls back when the counter fails, and starts from reported usage with cached and reasoning tokens in the breakdown.',
        'The meter takes `usage`, marks an estimate as "About …" with `data-approximate` and `data-source`, and lets an explicit `used` win.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
