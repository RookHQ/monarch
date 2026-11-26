import { StackedBarChart } from '@rookcom/monarch-react/charts';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const MONTHS = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun'];
const TICKETS = MONTHS.map((month, i) => ({
  month,
  resolved: [120, 132, 101, 134, 190, 230][i] ?? 0,
  pending: [22, 18, 30, 25, 19, 21][i] ?? 0,
  escalated: [8, 6, 12, 9, 7, 5][i] ?? 0,
}));
const STATUS_SERIES = [
  { key: 'resolved', label: 'Resolved' },
  { key: 'pending', label: 'Pending' },
  { key: 'escalated', label: 'Escalated' },
];
const TEAMS = [
  { team: 'Platform', done: 34, review: 6, todo: 10 },
  { team: 'Design systems', done: 21, review: 9, todo: 4 },
  { team: 'Growth', done: 12, review: 3, todo: 18 },
  { team: 'Support tooling', done: 27, review: 2, todo: 6 },
];
const WORK_SERIES = [
  { key: 'done', label: 'Done' },
  { key: 'review', label: 'In review' },
  { key: 'todo', label: 'To do', color: 'neutral' as const },
];

export default {
  id: 'stacked-bar-chart',
  title: 'StackedBarChart',
  summary:
    'Stacked and grouped bars, vertical or horizontal, as values or shares of 100%, with totals, tooltips, keyboard inspection, legend toggles and a data table.',
  sections: {
    purpose: {
      summary:
        'Shows how a total splits into parts for each category or period, or compares parts side by side. It is BarChart with stacking on by default plus a 100% offset. Needed by: none recorded yet; products that pass `layout="stacked"` to BarChart can move to it for the 100% offset.',
      useWhen: [
        'A total per period or category and what it is made of: tickets by status per month.',
        'Comparing the mix across categories regardless of size (`offset="expand"`).',
        'Long category names or rankings: `orientation="horizontal"`.',
      ],
      avoidWhen: [
        'One series per category: use BarChart (or BarList for a ranked list).',
        'A continuous trend of parts over many points: use AreaChart with `stacking="stacked"`.',
        'A single whole: use StackedProportionBar or DonutChart.',
      ],
    },
    designPrinciples: {
      items: [
        'Bars always start at zero; only the far end of each stack is rounded.',
        'The first series sits on the baseline, where its length is easiest to compare; put the most important part first.',
        'Totals are always one glance away: in the tooltip footer, the table and, with `valueLabels`, at the end of each bar.',
        'Shares in `expand` are computed over every series, so hiding one in the legend leaves a gap instead of silently rescaling the rest.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Every BarChart prop passes through; `layout` defaults to `stacked` here. The root `<figure>` gets `mn-stacked-bar-chart` and `data-offset`.',
        'With `offset="expand"` the values become shares (0–1) formatted as percentages in the tooltip, table, labels and summary; `onBarSelect` still receives the original row. Reference lines are not drawn in `expand`.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Migrating from recharts / shadcn `<BarChart>`: each `<Bar dataKey name stackId>` becomes a `series` entry; a shared `stackId` is the default `layout="stacked"`; `layout="vertical"` in recharts is `orientation="horizontal"` here; `stackOffset="expand"` is `offset="expand"`.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'layout',
          values: ['stacked', 'grouped'],
          description:
            'One bar made of parts per category, or the parts side by side. Exposed as `data-layout` on the plot.',
        },
        {
          prop: 'offset',
          values: ['normal', 'expand'],
          description:
            'Stack values, or stack shares so every bar is 100%. Exposed as `data-offset` on the figure.',
        },
        {
          prop: 'orientation',
          values: ['vertical', 'horizontal'],
          description:
            'Columns, or bars for long labels and rankings. Exposed as `data-orientation`.',
        },
        {
          prop: 'valueLabels',
          values: ['false', 'true'],
          description: 'Prints the stack total at the end of each bar.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Inspecting',
          description:
            'The category band is shaded and the tooltip lists every part plus the total.',
        },
        {
          name: 'Highlighted category',
          attribute: 'data-dimmed on other bars',
          description: '`highlightIndex` keeps one category at full strength and dims the rest.',
        },
        {
          name: 'Series hidden',
          attribute: 'aria-pressed="false" on the legend toggle',
          description: 'The part leaves the bars; it stays in the table.',
        },
        {
          name: 'Empty, loading, error',
          attribute: 'data-status',
          description: 'Handled by ChartFrame; the height is kept.',
        },
      ],
    },
    interaction: {
      items: [
        'Hover, tap or arrow keys inspect one category at a time.',
        'Legend entries toggle their series (`legendToggle`, default on for two or more series).',
        '`onBarSelect` runs on click, Enter or Space for drill-in, with the original data row.',
        '`activeIndex` + `onActiveIndexChange` sync the inspected category with other charts.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Tab',
          action: 'Moves through the header toggle and legend to the plot (one tab stop).',
        },
        { keys: 'ArrowRight / ArrowDown', action: 'Next category.' },
        { keys: 'ArrowLeft / ArrowUp', action: 'Previous category.' },
        { keys: 'Home / End', action: 'First or last category.' },
        { keys: 'Escape', action: 'Hides the tooltip.' },
        { keys: 'Enter / Space', action: 'Calls `onBarSelect`.' },
      ],
    },
    accessibility: {
      role: 'figure (ChartFrame); the plot is a group with aria-roledescription "chart"',
      name: 'The figure by `title`; the plot by a generated summary (categories, then total and highest category per series) or `summary`.',
      items: [
        'Keyboard inspection announces "Mar: Resolved 101, Pending 30, Escalated 12, Total 143" through a polite live region.',
        'The SVG and tooltip are hidden from assistive technology; the data table has every value and a Total column.',
        'Series differ by label and, with `dashed`, by an outlined bar, not by colour alone.',
      ],
    },
    responsive: {
      items: [
        'Measures its container; category labels thin out (vertical) or truncate (horizontal), down to 320px.',
        'Horizontal charts grow 32px per category unless `height` is set.',
        'Renders at a 640px fallback width on the server.',
      ],
    },
    theme: {
      items: [
        'Series colours are the data tokens; stacked parts are separated by a hairline in the surface colour.',
        'Forced colours: bars keep their hue with a Canvas hairline between parts.',
        'Reduced motion: dimming and the tooltip fade become instant.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Stacked with totals',
          stack: true,
          render: () => (
            <StackedBarChart
              title="Tickets by status"
              description="First half of the year"
              data={TICKETS}
              categoryKey="month"
              categoryLabel="Month"
              series={STATUS_SERIES}
              valueLabels
              locale="en-US"
            />
          ),
        },
        {
          title: 'Horizontal, 100%',
          stack: true,
          render: () => (
            <StackedBarChart
              title="Work by status"
              data={TEAMS}
              categoryKey="team"
              categoryLabel="Team"
              series={WORK_SERIES}
              orientation="horizontal"
              offset="expand"
              locale="en-US"
            />
          ),
        },
        {
          title: 'Grouped',
          stack: true,
          render: () => (
            <StackedBarChart
              title="Tickets by status"
              data={TICKETS.slice(3)}
              categoryKey="month"
              categoryLabel="Month"
              series={STATUS_SERIES}
              layout="grouped"
              locale="en-US"
              height={200}
            />
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Keep to five parts or fewer; fold the rest into "Other".',
        'Use `offset="expand"` when the question is about mix, not size.',
        'Give a neutral colour to a "remaining" or "to do" part.',
      ],
      dont: [
        'Stack parts that do not add up to a meaningful total.',
        'Mix negative and positive parts in one stack without explaining the baseline.',
        'Use status colours for series; danger and success are never series.',
      ],
    },
    related: {
      items: [
        {
          id: 'bar-chart',
          reason: 'The chart this composes; single-series bars and all shared props.',
        },
        { id: 'area-chart', reason: 'Stacked parts over a continuous time axis.' },
        { id: 'stacked-proportion-bar', reason: 'One whole split into parts, without axes.' },
        { id: 'chart-legend', reason: 'A shared legend for several charts.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['bar-chart'],
      notes: [
        "Rendering, tooltip, keyboard model and table are BarChart's (docs/adr/0010-charts-rendering-and-colour.md).",
      ],
    },
    testing: {
      unit: [
        'Stacks by default: root class, `data-offset`, Total column, keyboard announcements with totals; no axe violations.',
        '100% offset: shares in the table, percent ticks and announcements.',
        'Grouped and horizontal layouts.',
        'Legend toggle hides a series.',
        '`onBarSelect` receives the original row in 100% mode.',
        'Renders on the server.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
