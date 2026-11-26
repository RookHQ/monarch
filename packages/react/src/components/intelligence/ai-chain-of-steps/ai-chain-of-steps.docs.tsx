import { useEffect, useState } from 'react';
import { Button } from '@rookcom/monarch-react';
import { AiChainOfSteps, type AiStep } from '@rookcom/monarch-react/ai';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const LABELS = [
  'Read the attached file',
  'Search earlier notes',
  'Compare the figures',
  'Draft a summary',
];

function stepsAt(progress: number, failAt?: number): AiStep[] {
  return LABELS.map((label, i) => ({
    id: String(i),
    label,
    state:
      failAt !== undefined && i === failAt && progress > i
        ? 'failed'
        : failAt !== undefined && i > failAt && progress > failAt
          ? 'skipped'
          : i < progress
            ? 'done'
            : i === progress
              ? 'active'
              : 'pending',
    ...(i === 1 && progress > 1 ? { detail: '3 notes matched' } : {}),
  }));
}

function LiveDemo() {
  const [progress, setProgress] = useState(-1);
  useEffect(() => {
    if (progress < 0 || progress >= LABELS.length) return;
    const t = setTimeout(() => setProgress((p) => p + 1), 1200);
    return () => clearTimeout(t);
  }, [progress]);
  return (
    <div style={{ display: 'grid', gap: 'var(--mn-space-3)', justifyItems: 'start' }}>
      <Button size="sm" onClick={() => setProgress(0)} disabled={progress >= 0 && progress < 4}>
        Run the steps
      </Button>
      {progress >= 0 ? <AiChainOfSteps steps={stepsAt(progress)} /> : null}
    </div>
  );
}

export default {
  id: 'ai-chain-of-steps',
  title: 'AiChainOfSteps',
  summary: 'An agent’s steps as a live ordered list that folds to "4 steps" when finished.',
  sections: {
    purpose: {
      summary:
        'Shows the steps an assistant or agent is working through and where it is, so a multi-step task feels like progress rather than a long wait, and folds away once it is finished so the result stays in focus.',
      useWhen: [
        'An agent runs several named steps (read, search, compare, draft) and reports each one’s state.',
        'Showing an approved AiPlan while it runs (AiPlan does this for you).',
      ],
      avoidWhen: [
        'A person moves through a form or wizard: use Stepper.',
        'One tool call with inputs and outputs: use AiToolCall.',
        'Free-form reasoning text: use AiReasoning.',
      ],
    },
    designPrinciples: {
      items: [
        'State is shown by icon shape and text, never colour alone: dashed circle, filled dot, check, cross, dash.',
        'Only the running step moves; finished steps are still.',
        'Once everything is finished the list gets out of the way under a one-line summary that still says how many failed.',
        'Announcements are about change (a step starts, fails, the run finishes), never the initial state.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'The ref goes to the root `<div>`; native `<div>` attributes pass through.',
        'Steps are matched by `id` between renders to decide what to announce; keep ids stable.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [
        {
          prop: 'collapsible',
          values: ['true', 'false'],
          description:
            'When true (default), a finished list folds under a "4 steps" `<details>` summary. When false it always shows every step.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Step state',
          attribute: 'data-state (on each step)',
          description:
            '`pending` "Not started", `active` "In progress" (with `aria-current="step"` and a shimmer), `done`, `failed` and `skipped` (the last two also show their state as visible text).',
        },
        {
          name: 'Finished',
          attribute: 'data-finished',
          description: 'Every step is done, failed or skipped; with `collapsible` the list folds.',
        },
        {
          name: 'Has failures',
          attribute: 'data-failed',
          description:
            'At least one step failed; the folded summary adds "1 failed" and an error icon.',
        },
      ],
    },
    interaction: {
      items: [
        'Not interactive while running.',
        'When folded, clicking the summary opens or closes the full list.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Moves focus to the summary when the list is folded.' },
        { keys: 'Enter / Space', action: 'Opens or closes the folded list (native `<details>`).' },
      ],
      notes: ['While running there is nothing to focus.'],
    },
    accessibility: {
      role: 'list (an `<ol>`)',
      name: '`messages.label`, default "Steps"',
      items: [
        'Every step reads its label followed by its state ("Search earlier notes, In progress"); the running step has `aria-current="step"`.',
        'A polite live region announces "Step 3 of 4: Draft a summary", "Step failed: …" and "Finished 4 steps", throttled by nature (one line per change, never the initial render). Turn it off with `announce={false}` when a surrounding thread announces progress.',
        'Icons are decorative; their meaning is in the text.',
        'The shimmer only moves a background band; under reduced motion it becomes a static tint, and in forced colours the running step is underlined.',
      ],
    },
    responsive: {
      items: [
        'Labels and details wrap; nothing has a fixed width, so it fits a 390px screen.',
        'The folded summary grows to a 44px touch target on coarse pointers.',
        'RTL: logical properties; the connector line and chevron follow the writing direction.',
      ],
    },
    theme: {
      items: [
        'Running step icon in `--mn-color-ai-strong` with a `--mn-color-ai-thinking` shimmer (`--mn-duration-shimmer`); static `--mn-color-ai-subtle` under reduced motion.',
        'Done in `--mn-color-success-strong`, failed in `--mn-color-danger-strong`, pending and skipped in muted text.',
        'Forced colours use CanvasText throughout.',
      ],
    },
    examples: {
      items: [
        { title: 'Running', stack: true, render: () => <AiChainOfSteps steps={stepsAt(2)} /> },
        {
          title: 'Finished (folded)',
          stack: true,
          render: () => <AiChainOfSteps steps={stepsAt(4)} />,
        },
        {
          title: 'A failed step',
          stack: true,
          render: () => <AiChainOfSteps steps={stepsAt(4, 2)} defaultOpen />,
        },
        { title: 'Live', stack: true, render: () => <LiveDemo /> },
      ],
    },
    doDont: {
      do: [
        'Use short, specific step labels that say what the agent does.',
        'Put results in `detail` ("3 notes matched") rather than in the label.',
      ],
      dont: [
        'Do not mark several steps active at once; run them one after another or group them.',
        'Do not change step ids between updates, or changes are announced as new steps.',
      ],
    },
    related: {
      items: [
        { id: 'ai-plan', reason: 'The plan a person approves before these steps run.' },
        { id: 'ai-tool-call', reason: 'One tool call with its input and output.' },
        { id: 'ai-reasoning', reason: 'The model’s reasoning text.' },
        { id: 'stepper', reason: 'A person’s progress through a multi-step form.' },
      ],
    },
    dependencies: { packages: ['react'], components: [] },
    testing: {
      unit: [
        'Renders a labelled `<ol>` with state text on every step and `aria-current="step"` on the running one; ref and props forwarding.',
        'Failed and skipped steps show their state as visible text.',
        'Announces step starts, failures and completion, not the initial state; no live region with `announce={false}`.',
        'Folds under "4 steps" once finished and opens on click; `collapsible={false}`, `defaultOpen` and translated strings.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
