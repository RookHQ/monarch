import { AiAgentRunTimeline, type AiRunEvent } from '@rookcom/monarch-react/ai';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const T = Date.UTC(2026, 0, 1, 12, 0, 0);

const RUN: AiRunEvent[] = [
  {
    id: '1',
    kind: 'model',
    name: 'Plan the answer',
    startedAt: T,
    endedAt: T + 1400,
    inputTokens: 1850,
    outputTokens: 240,
    costMicros: 3100,
  },
  {
    id: '2',
    kind: 'tool',
    name: 'search_records',
    startedAt: T + 1400,
    endedAt: T + 2600,
    detail: 'query: "renewals this quarter" · 14 results',
  },
  {
    id: '3',
    kind: 'handoff',
    name: 'To the analysis agent',
    startedAt: T + 2600,
    endedAt: T + 2700,
  },
  {
    id: '4',
    kind: 'model',
    name: 'Write the summary',
    startedAt: T + 2700,
    endedAt: T + 6100,
    inputTokens: 4200,
    outputTokens: 910,
    costMicros: 9800,
  },
];

const FAILED: AiRunEvent[] = [
  ...RUN.slice(0, 2),
  {
    id: 'e',
    kind: 'error',
    name: 'fetch_report',
    startedAt: T + 2600,
    endedAt: T + 7600,
    detail: 'Timed out after 5 seconds.',
  },
];

const RUNNING: AiRunEvent[] = [
  ...RUN.slice(0, 2),
  { id: 'r', kind: 'model', name: 'Write the summary', startedAt: T + 2600, inputTokens: 3900 },
];

export default {
  id: 'ai-agent-run-timeline',
  title: 'AiAgentRunTimeline',
  summary: 'An agent run trace: bars on a time axis, totals for time, tokens and cost.',
  sections: {
    purpose: {
      summary:
        'Shows what an agent did during one run and where the time, tokens and money went, so builders and admins can explain a slow or expensive answer and spot the failing step.',
      useWhen: [
        'Debugging or reviewing an agent run in a builder, an admin console or an evaluation view.',
        'Showing a person what an agent did on their behalf, step by step, with cost.',
      ],
      avoidWhen: [
        'Live progress of a running task for an end user: use AiTaskProgress or AiChainOfSteps.',
        'A history of actions the agent took on records: use AiActionLog.',
      ],
    },
    designPrinciples: {
      items: [
        'One shared time axis, so overlaps and gaps are visible at a glance.',
        'Every bar has its kind and duration as text in its row; colour only reinforces the kind.',
        'Totals first: total time, input and output tokens, and cost in the chosen currency.',
        'The same data is always available as a table, the accessible and copyable fallback.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'The ref goes to the root `<div role="group">`; native attributes pass through.',
        '`costMicros` is in millionths of `currency`; costs under 1 unit show up to four decimals.',
        'Events without `endedAt` are running: they end at `now`, or at the latest known timestamp without it, so server rendering is deterministic.',
        '`aiRunTotals(events, now?)` returns the same totals for use elsewhere.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [
        {
          prop: 'view',
          values: ['timeline', 'table'],
          description:
            'Bars on a time axis with expandable rows, or a table with one row per event. Controlled with `view` / `onViewChange`, or uncontrolled with `defaultView`.',
        },
      ],
    },
    states: {
      items: [
        { name: 'View', attribute: 'data-view', description: '`timeline` or `table`.' },
        {
          name: 'Kind',
          attribute: 'data-kind (on each event)',
          description: '`model`, `tool`, `handoff` or `error`; shown as text and bar colour.',
        },
        {
          name: 'Running',
          attribute: 'data-running (on each event)',
          description: 'No `endedAt` yet: the duration reads "Running" and the bar shimmers.',
        },
        {
          name: 'Expanded',
          attribute: 'data-expanded / aria-expanded',
          description: 'The event shows its start offset, duration, tokens, cost and detail.',
        },
        { name: 'Empty', description: 'No events: "No events yet".' },
      ],
    },
    interaction: {
      items: [
        'Click an event row to show or hide its details; several can be open.',
        'Switch between Timeline and Table with the segmented control.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Moves to the view switch, then through the event rows.' },
        { keys: 'Enter / Space', action: 'Expands or collapses the focused event.' },
        { keys: 'Left / Right arrows', action: 'Switch view inside the segmented control.' },
      ],
    },
    accessibility: {
      role: 'group',
      name: '`messages.label`, default "Agent run"',
      items: [
        'Events are an ordered list named "Run events"; each row is a disclosure button reading kind, name and duration, with `aria-expanded` and `aria-controls`.',
        'The bars and the axis are decorative (`aria-hidden`); every value they show is in text.',
        'The table view is a real `<table>` with a caption, column headers and the event name as row header: the fallback for screen reader users who want to compare values.',
        'Totals are a description list.',
      ],
    },
    responsive: {
      items: [
        'Container query: below 34rem the label sits above a full-width bar, so it works at 390px.',
        'The table scrolls horizontally inside its own wrapper, never the page.',
        'Rows are 44px touch targets on coarse pointers.',
        'RTL: bars are placed with logical insets, so time runs from the start side.',
      ],
    },
    theme: {
      items: [
        'Bars: model `--mn-color-ai-solid`, tool `--mn-color-ai-tool`, hand-off `--mn-color-info-solid`, error `--mn-color-danger-solid`.',
        'Running bars sweep `--mn-color-ai-thinking` over `--mn-duration-shimmer`; static under reduced motion.',
        'Forced colours draw bars in CanvasText with bordered tracks and kind labels.',
      ],
    },
    examples: {
      items: [
        {
          title: 'A finished run',
          stack: true,
          render: () => <AiAgentRunTimeline events={RUN} currency="EUR" locale="en-GB" />,
        },
        {
          title: 'Table view',
          stack: true,
          render: () => <AiAgentRunTimeline events={RUN} defaultView="table" />,
        },
        {
          title: 'A failed tool call',
          stack: true,
          render: () => <AiAgentRunTimeline events={FAILED} />,
        },
        {
          title: 'Still running',
          stack: true,
          render: () => <AiAgentRunTimeline events={RUNNING} now={T + 5000} />,
        },
      ],
    },
    doDont: {
      do: [
        'Pass the currency your billing uses and the viewer’s locale.',
        'Put tool arguments, results and errors in `detail` so the row stays short.',
      ],
      dont: [
        'Do not show cost to people who cannot act on it; leave `costMicros` out and the column disappears.',
        'Do not put secrets or raw prompts with personal data in `detail`.',
      ],
    },
    related: {
      items: [
        { id: 'ai-task-progress', reason: 'Live progress of a long task.' },
        { id: 'ai-chain-of-steps', reason: 'Steps as a list without timing.' },
        { id: 'ai-tool-call', reason: 'One tool call in a conversation.' },
        { id: 'ai-action-log', reason: 'What agents changed, as an audit list.' },
      ],
    },
    dependencies: { packages: ['react'], components: ['segmented-control'] },
    testing: {
      unit: [
        'Renders a named group with totals (time, tokens, cost via Intl) and events sorted by start; ref and props forwarding.',
        'Places bars on the shared axis by start offset and duration.',
        'Expands and collapses an event with Enter and Space, showing offset and detail.',
        'Switches to a table with captions, row headers and the same values; reports `onViewChange`.',
        'Running events end at `now` and read "Running"; `aiRunTotals`; empty state and translated strings.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
