import { FunnelChart } from '@rookcom/monarch-react/charts';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const SIGNUP = [
  { key: 'visit', label: 'Visited pricing', value: 12480 },
  { key: 'start', label: 'Started sign-up', value: 3920 },
  { key: 'verify', label: 'Verified email', value: 3105 },
  { key: 'trial', label: 'Started trial', value: 1870 },
  { key: 'paid', label: 'Became customer', value: 412 },
];
const HIRING = [
  { key: 'applied', label: 'Applied', value: 240 },
  { key: 'screen', label: 'Screened', value: 96 },
  { key: 'interview', label: 'Interviewed', value: 31 },
  { key: 'offer', label: 'Offer', value: 6 },
  { key: 'hired', label: 'Hired', value: 4 },
];

export default {
  id: 'funnel-chart',
  title: 'FunnelChart',
  summary:
    'Conversion funnel: a bar per stage, the share that continues and drops between stages, overall conversion and the largest drop, with keyboard inspection and a data table.',
  sections: {
    purpose: {
      summary:
        'Shows how many reach each step of a process and where they drop off. Needed by: Rook Links (AdminAnalytics `conversionFunnel`: visitors, views, clicks, conversions; CreatorImpact funnel visual), Rook Recruit (HiringHome hiring funnel steps).',
      useWhen: [
        'An ordered process where each step is a subset of the one before: sign-up, checkout, hiring.',
        'You need step-to-step conversion and drop-off, not just counts.',
      ],
      avoidWhen: [
        'Categories that are not steps of one process: use BarChart or BarList.',
        'Counts that can grow between steps (not a funnel): use BarChart.',
        'Conversion over time: use LineChart of the rate.',
      ],
    },
    designPrinciples: {
      items: [
        'Bar length is proportional to the count (relative to the widest stage), never stylised into a trapezoid that distorts it.',
        'Every number is written as text: count and share of the first stage on each row, conversion and drop-off between rows.',
        'One measure, one colour: every stage uses slot 1 unless a stage pins its own `color`.',
        'The summary names what matters: overall conversion and the largest drop.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Renders ChartFrame (ref to its `<figure>`); ChartFrame props except `children`, `legend`, `table` and `height` pass through. The frame reserves 76px per stage (56px without steps) for loading and empty states.',
        'Rates are fractions (0–1) formatted with `Intl.NumberFormat` percent unless `formatRate` is set.',
        'A stage after a stage of 0 has a 0% step rate and is left out of the largest drop.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [
        {
          prop: 'align',
          values: ['center', 'start'],
          description:
            '`center` reads as a funnel; `start` aligns bars for easier length comparison. Exposed as `data-align`.',
        },
        {
          prop: 'showSteps',
          values: ['true', 'false'],
          description: 'The "62% continued · 30 dropped" line between stages.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Inspecting',
          attribute: 'data-active on the stage, data-dimmed on the others',
          description: 'The inspected stage is shaded and the other bars dim.',
        },
        {
          name: 'Selectable',
          attribute: 'data-selectable',
          description: 'With `onStageSelect`, the plot shows a pointer cursor.',
        },
        {
          name: 'Empty, loading, error',
          attribute: 'data-status',
          description: "No stages show ChartFrame's empty state; pass `status` for the others.",
        },
        {
          name: 'Focus',
          description: 'The plot shows the Monarch focus ring on `:focus-visible`.',
        },
      ],
    },
    interaction: {
      items: [
        'Hovering or tapping a stage inspects it; a mouse leaving clears it.',
        '`onStageSelect` runs on click, Enter or Space at the inspected stage, for drill-in (the people who dropped).',
        '`activeIndex` + `onActiveIndexChange` control the inspected stage.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Focuses the plot (one tab stop) and inspects the first stage.' },
        { keys: 'ArrowDown / ArrowRight', action: 'Next stage.' },
        { keys: 'ArrowUp / ArrowLeft', action: 'Previous stage.' },
        { keys: 'Home / End', action: 'First or last stage.' },
        { keys: 'Escape', action: 'Clears the inspected stage.' },
        { keys: 'Enter / Space', action: 'Calls `onStageSelect`.' },
      ],
    },
    accessibility: {
      role: 'figure (ChartFrame); the plot is a group with aria-roledescription "chart"',
      name: 'The figure by `title`; the plot by a generated summary ("5 stages from Visited pricing to Became customer. Overall conversion 3.3%. Largest drop: …") or `summary`.',
      items: [
        'The visual rows are `aria-hidden`; the summary, a polite live region and the data table carry the values.',
        'Keyboard inspection announces "Started trial: 1,870, 60.2% of previous, 15% of first".',
        'The data table has Stage, Count, From previous and Of first columns.',
        'Every string is translatable through `labels`.',
      ],
    },
    responsive: {
      items: [
        'Rows stack the label and value above the bar, so long stage names wrap and nothing scrolls sideways at 320px.',
        'Rows are at least 44px tall on coarse pointers.',
        'Logical properties throughout; bars grow from the start edge in right-to-left with `align="start"`.',
      ],
    },
    theme: {
      items: [
        'Bars use the data tokens (slot 1 by default); text and step markers use text tokens.',
        'Forced colours: bars are drawn in CanvasText, the inspected stage gets a Highlight outline.',
        'Reduced motion: the dim and highlight transitions become instant.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Sign-up funnel',
          stack: true,
          render: () => (
            <FunnelChart
              title="Sign-up conversion"
              description="Last 30 days"
              stages={SIGNUP}
              locale="en-US"
            />
          ),
        },
        {
          title: 'Aligned to the start, without step lines',
          stack: true,
          render: () => (
            <FunnelChart
              title="Hiring pipeline"
              stages={HIRING}
              align="start"
              showSteps={false}
              locale="en-US"
            />
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Order stages from the first step to the last.',
        'Name stages by what people did ("Started trial"), not by internal event names.',
        'Use `onStageSelect` to open the list of people who dropped at that step.',
      ],
      dont: [
        'Use it for categories that are not steps of one process.',
        'Colour stages by status; one measure keeps one colour.',
        'Hide the step lines when conversion between steps is the point.',
      ],
    },
    related: {
      items: [
        { id: 'bar-list', reason: 'A ranked list of categories with bars.' },
        { id: 'bar-chart', reason: 'Counts per category that are not a process.' },
        { id: 'chart-frame', reason: 'The frame FunnelChart renders.' },
        { id: 'delta-indicator', reason: 'A change in a conversion rate.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['chart-frame'],
      notes: [
        'Formatting, the keyboard model and the data table come from `src/internal/charts` (docs/adr/0010-charts-rendering-and-colour.md). Bars are HTML so labels wrap.',
      ],
    },
    testing: {
      unit: [
        'Bars, widths, slot colour, step lines and generated summary; no axe violations.',
        'Data table columns and rows.',
        'Keyboard: arrows, End, Escape, live region; active and dimmed stages.',
        '`onStageSelect` from Enter.',
        'Start alignment, hidden steps, translated labels and custom rate format.',
        'A zero first stage and no stages (empty state).',
        'Renders on the server.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
