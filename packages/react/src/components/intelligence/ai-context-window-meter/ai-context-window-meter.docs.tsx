import { useState } from 'react';
import { AiContextWindowMeter } from '@rookcom/monarch-react/ai';
import type { ComponentDocs } from '../../../internal/component-docs.js';

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
                used={46000}
                limit={128000}
                breakdown={[
                  { label: 'Instructions', tokens: 3000 },
                  { label: 'Attachments', tokens: 18000 },
                  { label: 'Messages', tokens: 22000 },
                ]}
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
      ],
      dont: [
        'Do not summarize without the person asking.',
        'Do not show it for features with a fixed, short context.',
        'Do not use it for billing quotas.',
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
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
