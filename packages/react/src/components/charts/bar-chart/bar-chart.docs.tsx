import { BarChart } from '@rookcom/monarch-react/charts';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const WEEKS = [
  { week: 'Week 1', drafted: 12, posted: 8 },
  { week: 'Week 2', drafted: 9, posted: 9 },
  { week: 'Week 3', drafted: 15, posted: 11 },
  { week: 'Week 4', drafted: 11, posted: 10 },
  { week: 'Week 5', drafted: 14, posted: 13 },
  { week: 'Week 6', drafted: 18, posted: 12 },
];
const MONTHS = [
  { month: 'May', earned: 320 },
  { month: 'Jun', earned: 410 },
  { month: 'Jul', earned: 380 },
  { month: 'Aug', earned: 520 },
  { month: 'Sep', earned: 610 },
  { month: 'Oct', earned: 680 },
];
const CATEGORIES = [
  { topic: 'Billing and invoices', open: 42 },
  { topic: 'Account access', open: 31 },
  { topic: 'Integrations', open: 18 },
  { topic: 'Feature requests', open: 12 },
  { topic: 'Other', open: 7 },
];

export default {
  id: 'bar-chart',
  title: 'BarChart',
  summary:
    'Vertical and horizontal bar charts, grouped or stacked, with value labels, highlight, reference lines, keyboard tooltips, drill-in and a data table.',
  sections: {
    purpose: {
      summary:
        'Compares amounts across categories or discrete periods, one or more series per category, with exact values on hover, tap or arrow keys and the full data as a table.',
      useWhen: [
        'Comparing categories: requests per topic, revenue per month, posts per week.',
        'Showing parts of a total per category (`layout="stacked"`).',
        'Long category names or rankings (`orientation="horizontal"`).',
      ],
      avoidWhen: [
        'A continuous trend with many points: use LineChart.',
        'A simple ranked list with values and links: use BarList.',
        'Shares of one whole: use StackedProportionBar or DonutChart.',
      ],
    },
    designPrinciples: {
      items: [
        'Bars always start at zero; the value axis can only fix its far end.',
        'Only the far end of a bar is rounded (4px), stacked segments are separated by a surface gap, and only the outer segment is rounded.',
        'Colour encodes the series, never the category; one series is one colour, and `highlightIndex` emphasises by dimming the rest.',
        'Value labels are optional and compact (1.2K); exact values live in the tooltip and table.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Renders ChartFrame (ref to its `<figure>`); ChartFrame props pass through except `children`, `legend` and `table`, which BarChart builds.',
        '`data` is one object per category; `categoryKey` names the category field and each series `key` a numeric field.',
        '`height` defaults to 240 for vertical charts; horizontal charts grow by 32px per category.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [
        {
          prop: 'orientation',
          values: ['vertical', 'horizontal'],
          description:
            'Columns, or bars with category labels on the left (truncated with an ellipsis). `data-orientation`.',
        },
        {
          prop: 'layout',
          values: ['grouped', 'stacked'],
          description:
            'Series side by side, or one bar per category with positive and negative parts stacked separately and a Total column in the table. `data-layout`.',
        },
        {
          prop: 'valueLabels',
          values: ['true', 'false'],
          description: 'Each bar’s value (the stack total when stacked) at its end.',
        },
        {
          prop: 'series[].dashed',
          values: ['true', 'false'],
          description:
            'An outlined bar instead of a filled one, as a second encoding (a forecast, a target).',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Inspecting',
          description:
            'A muted band behind the category and a tooltip with every visible series (and the total when stacked).',
        },
        {
          name: 'Highlighted',
          attribute: 'data-dimmed on the other bars',
          description: 'With `highlightIndex`, every other category is dimmed.',
        },
        {
          name: 'Selectable',
          attribute: 'data-selectable',
          description: 'With `onBarSelect` the plot shows a pointer cursor and Enter selects.',
        },
        {
          name: 'Series hidden',
          attribute: 'aria-pressed="false"',
          description: 'The series leaves the plot and the axis; it stays in the table.',
        },
        {
          name: 'Empty / loading / error',
          attribute: 'data-status',
          description: 'From ChartFrame.',
        },
      ],
    },
    interaction: {
      items: [
        'Hover or tap a category to inspect it; leaving with the mouse clears it.',
        '`onBarSelect(datum, index)` runs on click, Enter or Space, for drill-in such as opening a filtered list.',
        'Legend entries toggle series (controlled `hiddenSeries` or uncontrolled `defaultHiddenSeries`).',
        '`activeIndex` + `onActiveIndexChange` sync the inspected category with other charts.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Focuses the plot (one tab stop) and inspects the first category.' },
        {
          keys: 'ArrowRight / ArrowDown',
          action: 'Next category (either pair works in both orientations).',
        },
        { keys: 'ArrowLeft / ArrowUp', action: 'Previous category.' },
        { keys: 'Home / End', action: 'First or last category.' },
        { keys: 'PageUp / PageDown', action: 'Moves a tenth of the categories.' },
        { keys: 'Escape', action: 'Hides the tooltip.' },
        { keys: 'Enter / Space', action: 'Calls `onBarSelect` for the inspected category.' },
      ],
    },
    accessibility: {
      role: 'figure (ChartFrame); the plot is a group with aria-roledescription "chart"',
      name: 'The figure by `title`; the plot by a generated summary (count, then total and highest category per series) or `summary`.',
      items: [
        'The SVG is `aria-hidden`; the summary, a polite live region during keyboard inspection and the data table carry the values.',
        'The tooltip is visual only, so pointer use does not flood screen readers.',
        'The data table has every series and, when stacked, a Total column.',
        'Emphasis is not colour alone: the highlighted category keeps full opacity while others dim, and value labels can show the numbers.',
        'All text, including summaries and "Total", is translatable through `labels`.',
      ],
    },
    responsive: {
      items: [
        'Lays out at the measured container width (works at 320px); bar thickness is capped so few bars do not turn into slabs.',
        'Vertical category labels thin out when they would collide; horizontal labels truncate to at most 40% of the width.',
        'Server render and first paint use a 640px layout scaled to fit, matching on hydration.',
      ],
    },
    theme: {
      items: [
        'Series use `--mn-color-data-1` … `-5`; stacked segments are separated by a `--mn-color-bg-surface` stroke.',
        'Light, dark and high contrast follow the tokens; data colours meet 3:1 (4.5:1 in high contrast) on every chart background.',
        'Dimming uses opacity with a fast, token-timed transition that is instant under reduced motion.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Grouped columns',
          stack: true,
          render: () => (
            <BarChart
              title="Posts"
              description="Drafted and posted per week"
              data={WEEKS}
              categoryKey="week"
              categoryLabel="Week"
              series={[
                { key: 'drafted', label: 'Drafted' },
                { key: 'posted', label: 'Posted' },
              ]}
              locale="en-US"
            />
          ),
        },
        {
          title: 'Stacked with totals',
          stack: true,
          render: () => (
            <BarChart
              title="Posts by state"
              data={WEEKS}
              categoryKey="week"
              categoryLabel="Week"
              layout="stacked"
              valueLabels
              series={[
                { key: 'posted', label: 'Posted' },
                { key: 'drafted', label: 'Drafted' },
              ]}
              locale="en-US"
            />
          ),
        },
        {
          title: 'Highlighted month with a target',
          stack: true,
          render: () => (
            <BarChart
              title="Earnings"
              description="Last six months"
              data={MONTHS}
              categoryKey="month"
              categoryLabel="Month"
              series={[{ key: 'earned', label: 'Earned' }]}
              highlightIndex={MONTHS.length - 1}
              valueLabels
              referenceLines={[{ value: 500, label: 'Goal' }]}
              valueFormat={{ style: 'currency', currency: 'USD', maximumFractionDigits: 0 }}
              locale="en-US"
              height={200}
            />
          ),
        },
        {
          title: 'Horizontal, with drill-in',
          stack: true,
          render: () => (
            <BarChart
              title="Open requests by topic"
              data={CATEGORIES}
              categoryKey="topic"
              categoryLabel="Topic"
              orientation="horizontal"
              valueLabels
              series={[{ key: 'open', label: 'Open' }]}
              onBarSelect={() => undefined}
              locale="en-US"
            />
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Sort categories meaningfully: by time for periods, by value for rankings.',
        'Use horizontal bars when labels are longer than a word or two.',
        'Fill missing periods with zero in your data when zero is the truth; leave them out when it is not.',
      ],
      dont: [
        'Colour bars by category or by value; one series is one colour.',
        'Stack more than about four series; the inner segments become unreadable.',
        'Start the value axis anywhere but zero (BarChart does not allow it).',
      ],
    },
    related: {
      items: [
        { id: 'chart-frame', reason: 'The frame BarChart renders.' },
        { id: 'line-chart', reason: 'Continuous trends with many points.' },
        { id: 'bar-list', reason: 'A ranked list of labelled values with links, no axes.' },
        { id: 'stacked-proportion-bar', reason: 'Shares of a single total in one bar.' },
        { id: 'sparkline', reason: 'A tiny sparkbar (`variant="bar"`) inside a cell or card.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['chart-frame'],
      notes: ['Hand-written SVG from `src/internal/charts`; no charting library.'],
    },
    testing: {
      unit: [
        'Grouped bars per category, legend toggles and a generated summary; no axe violations.',
        'Stacked: Total column and tooltip footer, only the outer segment rounded.',
        'Keyboard inspection with live announcements; Enter and Space call `onBarSelect`; band highlight.',
        'Tap and click select a category.',
        'Horizontal layout: category labels, value labels, auto height, no legend for one series.',
        '`highlightIndex` dims the other categories.',
        'Negative values extend below zero.',
        'Hiding a series keeps the other colour.',
        'Locale-aware percent formatting.',
        'Renders on the server.',
      ],
      axe: true,
      ssr: true,
      browser: [
        'Visual baselines: /__gallery/bar-chart',
        'Cross-browser axe: /components/bar-chart',
      ],
    },
  },
} satisfies ComponentDocs;
