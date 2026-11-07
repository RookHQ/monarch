import { AiUsageMeter } from '@rookcom/monarch-react/ai';
import type { ComponentDocs } from '../../../internal/component-docs.js';

export default {
  id: 'ai-usage-meter',
  title: 'AiUsageMeter',
  summary: 'AI usage against quotas per scope (turn, conversation, month), with warnings.',
  sections: {
    purpose: {
      summary:
        'Shows how much AI a person or workspace has used against its quotas, in tokens or credits, and warns before a limit is reached so nobody is cut off mid-task without notice.',
      useWhen: [
        'An AI feature has quotas and people should see how close they are (settings, the assistant panel footer, a billing page).',
        'Several scopes matter at once, for example this turn, this conversation and this month.',
        'You want a warning in text before a limit is reached.',
      ],
      avoidWhen: [
        'You need the trace of one run with tokens, latency and cost per step: use AiAgentRunTimeline.',
        'You are showing how full the model’s context window is: use AiContextWindowMeter.',
        'The quantity is not AI usage (storage, seats): use Progress with `semantics="meter"`.',
      ],
    },
    designPrinciples: {
      items: [
        'One row per scope, each with its label, the amount in words and a meter.',
        'Warnings are text with an icon, never colour alone, and are phrased as facts ("85% of the limit used").',
        'A scope with no limit shows the amount only: no bar, no warning.',
        'Normal usage is neutral grey; only the warning and limit-reached states take status colours.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Native `<div>` attributes pass through and the ref is forwarded to the root.',
        '`aiUsageState(scope, warnAt?)` returns `ok`, `warning` or `exceeded` for apps that branch on it (for example to disable sending).',
        '`warnAt` is a 0–1 share of each limit. At or over the limit is `exceeded`.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [],
      notes: ['No visual variants. Each scope picks its `unit` (`tokens` or `credits`).'],
    },
    states: {
      items: [
        {
          name: 'Within limit',
          attribute: 'data-state="ok"',
          description: 'Neutral bar, no notice.',
        },
        {
          name: 'Warning',
          attribute: 'data-state="warning"',
          description: 'At or above `warnAt`: warning bar and a "… of the limit used" notice.',
        },
        {
          name: 'Limit reached',
          attribute: 'data-state="exceeded"',
          description: 'At or over the limit: danger bar and a "limit reached" notice.',
        },
        {
          name: 'No limit',
          attribute: 'data-state="ok" (scope)',
          description: 'The amount is shown without a bar.',
        },
      ],
      notes: ['Each scope row has its own `data-state`; the root carries the most serious one.'],
    },
    interaction: {
      items: [],
      notes: [
        'Display only. Pair it with your own action (upgrade, contact an admin) when a limit is reached.',
      ],
    },
    keyboard: {
      items: [],
      notes: ['Nothing is focusable.'],
    },
    accessibility: {
      role: 'group of meters',
      name: '"AI usage" for the group; each meter is named by its scope label',
      items: [
        'Each limited scope is a `role="meter"` with `aria-valuetext` such as "850 of 1,000 credits".',
        'A visually hidden polite status region carries the most serious warning, so it is announced once when a threshold is crossed, not on every update.',
        'Warning icons are decorative; the state is always in text.',
      ],
    },
    responsive: {
      items: [
        'Rows fill their container. The label and amount wrap onto two lines when narrow, so there is no horizontal scroll at 390px.',
      ],
    },
    theme: {
      items: [
        'Bars use Progress meter tones: neutral within the limit, `--mn-color-warning-strong` at the warning and `--mn-color-danger-strong` when reached.',
        'Notices use the warning and danger strong text colours; amounts use `--mn-color-text-muted`.',
        'In forced colours the notices use CanvasText; the bars follow Progress.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Turn, conversation and month',
          stack: true,
          render: () => (
            <div style={{ maxWidth: '22rem' }}>
              <AiUsageMeter
                scopes={[
                  { label: 'This turn', used: 1840, unit: 'tokens' },
                  { label: 'Conversation', used: 42000, limit: 200000, unit: 'tokens' },
                  { label: 'This month', used: 870, limit: 1000, unit: 'credits' },
                ]}
              />
            </div>
          ),
        },
        {
          title: 'Limit reached',
          stack: true,
          render: () => (
            <div style={{ maxWidth: '22rem' }}>
              <AiUsageMeter
                scopes={[{ label: 'This month', used: 1000, limit: 1000, unit: 'credits' }]}
              />
            </div>
          ),
        },
        {
          title: 'Earlier warning',
          stack: true,
          render: () => (
            <div style={{ maxWidth: '22rem' }}>
              <AiUsageMeter
                warnAt={0.5}
                scopes={[{ label: 'Conversation', used: 120000, limit: 200000, unit: 'tokens' }]}
              />
            </div>
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Show the scopes that can actually stop someone (usually the month and the conversation).',
        'Say what happens next near a reached limit (an upgrade link or who to contact).',
        'Tune `warnAt` so people get enough notice for how fast the quota is used.',
      ],
      dont: [
        'Do not show estimated numbers as if they were measured.',
        'Do not colour normal usage as a status.',
        'Do not use it for the context window: that is AiContextWindowMeter.',
      ],
    },
    related: {
      items: [
        { id: 'ai-context-window-meter', reason: 'How full the model’s context window is.' },
        { id: 'ai-agent-run-timeline', reason: 'Tokens, latency and cost per step of one run.' },
        { id: 'progress-bar', reason: 'The meter it is built on, for non-AI quotas.' },
      ],
    },
    dependencies: { packages: ['react'], components: ['progress-bar'] },
    testing: {
      unit: [
        'aiUsageState maps usage to ok, warning and exceeded, with and without a limit and with a custom warnAt.',
        'Named group; one named meter per limited scope with its value text; amount only without a limit.',
        'Warns at 80% by default and at a custom warnAt; limit reached outranks a warning in the status region; quiet when within limits.',
        'Localised strings; ref and prop forwarding.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
