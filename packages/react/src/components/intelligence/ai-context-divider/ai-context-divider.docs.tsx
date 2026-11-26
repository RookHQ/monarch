import { useState } from 'react';
import { AiContextDivider } from '@rookcom/monarch-react/ai';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const DAY = 86_400_000;

function ExpandDemo() {
  const [expanded, setExpanded] = useState(false);
  return (
    <div style={{ display: 'grid', gap: 'var(--mn-space-2)', width: '100%', maxWidth: '36rem' }}>
      {expanded ? (
        <p style={{ margin: 0, color: 'var(--mn-color-text-muted)' }}>
          (The original messages would load here.)
        </p>
      ) : (
        <AiContextDivider
          kind="summarized"
          summary="We agreed on three goals for the quarter, assigned owners and set the launch for week 9."
          onExpand={() => setExpanded(true)}
        />
      )}
      <p style={{ margin: 0 }}>Can you turn the goals into a checklist?</p>
    </div>
  );
}

export default {
  id: 'ai-context-divider',
  title: 'AiContextDivider',
  summary: 'Markers in a thread for a new day, a context reset or summarized messages.',
  sections: {
    purpose: {
      summary:
        'Shows where something changed in a long conversation: a new calendar day, a point where the assistant stopped seeing earlier messages, or where earlier messages were condensed into a summary to save context.',
      useWhen: [
        'The assistant’s context was reset (a new topic, or the person cleared it).',
        'Earlier messages were summarized and the summary replaces them in the model’s context.',
        'A custom thread needs day labels between messages.',
      ],
      avoidWhen: [
        'AiThread already adds day labels between messages; do not add a second set.',
        'Handing over to another agent or a person: use AiHandoff.',
      ],
    },
    designPrinciples: {
      items: [
        'Honest about memory: people should know what the assistant can and cannot see.',
        'Quiet: a thin rule with muted text, so it does not compete with messages.',
        'A summary is AI-generated, so it is tinted in the AI colour and identified as AI-generated to assistive tech.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Native `<div>` attributes pass through and the ref is forwarded to the root.',
        'Day labels use the same rules as AiThread: "Today", "Yesterday", a weekday and date, or a full date in other years.',
        '`summary` and `onExpand` only apply to `kind="summarized"`.',
        '`AI_CONTEXT_DIVIDER_MESSAGES` holds the English defaults.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [
        {
          prop: 'kind',
          values: ['day', 'context-reset', 'summarized'],
          description:
            '`day` is a plain rule with a date; `context-reset` is a dashed rule with an explanation; `summarized` uses the AI colour and can reveal the summary. Exposed as `data-kind`.',
        },
      ],
    },
    states: {
      items: [
        { name: 'Collapsed summary', description: 'Only "Show summary" is visible.' },
        {
          name: 'Open summary',
          description: 'The `<details>` element is open and shows the summary.',
        },
      ],
    },
    interaction: {
      items: [
        '"Show summary" opens and closes the summary (native `<details>`).',
        '"Show earlier messages" calls `onExpand`; load or reveal the originals and remove the divider.',
        'Day and reset dividers are not interactive.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Moves to "Show summary" and "Show earlier messages".' },
        { keys: 'Enter / Space', action: 'Toggles the summary or shows the earlier messages.' },
      ],
      notes: ['Day and context-reset dividers have no focusable parts.'],
    },
    accessibility: {
      role: 'separator, or group when it has controls',
      name: 'The label (aria-label); a context reset includes its explanation',
      items: [
        'Decorative rule lines are aria-hidden.',
        'Day labels are `<time>` elements with an ISO `datetime`.',
        'The summary starts with a visually hidden "AI-generated summary:".',
        'The disclosure uses native `<details>`/`<summary>` semantics.',
      ],
    },
    responsive: {
      items: [
        'Labels wrap and the rules shrink, so 390px has no horizontal scroll.',
        'On coarse pointers the controls are at least 44px tall.',
      ],
    },
    theme: {
      items: [
        'Rules: `--mn-color-border-subtle`; context reset `--mn-color-border-default` dashed; summarized `--mn-color-ai-border`.',
        'Summary panel: `--mn-color-ai-subtle` with `--mn-color-ai-border`.',
        'In forced colours rules use CanvasText and controls LinkText.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Days',
          stack: true,
          render: () => (
            <div style={{ width: '100%', maxWidth: '36rem' }}>
              <AiContextDivider kind="day" date={Date.now() - DAY} />
              <AiContextDivider kind="day" date={Date.now()} />
            </div>
          ),
        },
        {
          title: 'Context reset',
          stack: true,
          render: () => (
            <div style={{ width: '100%', maxWidth: '36rem' }}>
              <AiContextDivider kind="context-reset" />
            </div>
          ),
        },
        { title: 'Summarized, with summary and expand', stack: true, render: () => <ExpandDemo /> },
      ],
    },
    doDont: {
      do: [
        'Show a context reset wherever the assistant stops seeing earlier messages.',
        'Offer the summary so people can check what the assistant now works from.',
      ],
      dont: [
        'Do not hide that messages were summarized.',
        'Do not use a divider as a section heading for unrelated content.',
      ],
    },
    related: {
      items: [
        { id: 'ai-thread', reason: 'The thread the dividers sit in; it adds day labels itself.' },
        { id: 'ai-context-window-meter', reason: 'Offers "Summarize earlier messages".' },
        { id: 'ai-handoff', reason: 'Marks a handover between agents or to a person.' },
      ],
    },
    dependencies: { packages: ['react'], components: [] },
    testing: {
      unit: [
        'Day separator named by the label with a `<time>` element; Today, Yesterday and full dates; props and ref forwarded.',
        'Context reset named with its explanation.',
        'Summarized: plain separator, or a group with a disclosure marked AI-generated and an expand button.',
        'Messages.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
