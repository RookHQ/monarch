import { DonutChart } from '@rookcom/monarch-react/charts';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const devices = [
  { key: 'laptop', label: 'Laptops', value: 412 },
  { key: 'phone', label: 'Phones', value: 268 },
  { key: 'monitor', label: 'Monitors', value: 131 },
  { key: 'tablet', label: 'Tablets', value: 54 },
  { key: 'other', label: 'Accessories', value: 9 },
];

export default {
  id: 'donut-chart',
  title: 'DonutChart',
  summary:
    'Parts of one whole as a donut in ChartFrame: total in the middle, a legend of values and shares, and a data table.',
  sections: {
    purpose: {
      summary:
        'Shows how a total splits into a few named parts (assets by type, tickets by channel), with the numbers always available as text in the legend and the table.',
      useWhen: [
        'Two to six parts of one whole, where the share matters more than exact comparison.',
        'A dashboard card that needs a total and its breakdown together.',
      ],
      avoidWhen: [
        'Comparing parts precisely or ranking many categories: use BarList or BarChart.',
        'A single bar of proportions in a tight row: use StackedProportionBar.',
        'Values that do not add up to a meaningful whole, or change over time.',
      ],
    },
    designPrinciples: {
      items: [
        'Built on ChartFrame: the title labels the figure, and loading, empty, error and "View as table" behave like every Monarch chart.',
        'Series colours come only from the data palette in a fixed order; status colours are never slices. Pin `color` so a slice keeps its colour when filters change.',
        'More than six slices fold the smallest into a neutral "Other"; the table still lists every segment.',
        'Text carries the data: the legend shows each value and share, the plot has a generated summary, and small shares read "<1%" instead of disappearing.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Takes every ChartFrame prop except `children`, `legend`, `table` and `height`; the ref goes to the `<figure>`.',
        'Exported from `@rookcom/monarch-react/charts` with the `DonutChartSegment` and `DonutChartLabels` types.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: ['Negative and non-finite values count as 0; a zero total shows the empty state.'],
    },
    variants: {
      items: [
        {
          prop: 'legendContent',
          values: ['both', 'value', 'percent', 'none'],
          description: 'What each legend entry shows after its label.',
        },
        {
          prop: 'sort',
          values: ['desc', 'none'],
          description: 'Largest slice first (default), or the given order.',
        },
        {
          prop: 'size / thickness',
          values: ['number (px)'],
          description: 'Outer diameter (default 160) and ring width (default 22% of size).',
        },
        {
          prop: 'status',
          values: ['ready', 'loading', 'empty', 'error'],
          description: 'ChartFrame states, exposed as `data-status` on the figure.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Ready',
          attribute: 'data-status="ready"',
          description: 'Donut, centre and legend.',
        },
        {
          name: 'Loading',
          attribute: 'data-status="loading"',
          description: 'ChartFrame skeleton with `aria-busy` and "Loading chart".',
        },
        {
          name: 'Empty',
          attribute: 'data-status="empty"',
          description: 'Automatically when the total is 0, or set explicitly.',
        },
        {
          name: 'Error',
          attribute: 'data-status="error"',
          description: 'ChartFrame error, with Try again when `onRetry` is set.',
        },
        {
          name: 'Table view',
          attribute: 'data-view="table"',
          description: 'The visible data table replaces the donut.',
        },
      ],
    },
    interaction: {
      items: [
        'Hovering a slice dims the others and shows its label, value and share as a native tooltip.',
        '"View as table" swaps the donut for the data table and back.',
      ],
    },
    keyboard: {
      items: [{ keys: 'Tab, then Enter or Space', action: 'Toggles "View as table".' }],
      notes: [
        'Slices are not focusable; their values are in the legend, the summary and the table.',
      ],
    },
    accessibility: {
      role: 'figure, with the donut as role="img"',
      name: 'The figure is labelled by `title`; the donut by the generated summary ("Total 864. Laptops 412 (48%), …") or `summary`.',
      items: [
        'The SVG and the centre text are hidden; the legend list and the data table are real text.',
        'The data table is always in the page for screen readers, and visible through the toggle.',
        'Slices are separated by gaps, so neighbours stay distinct in forced-colours mode, where every slice is drawn in CanvasText.',
        'Every word (Other, Segment, Value, Share, Total, ChartFrame labels) is replaceable through `labels`.',
      ],
    },
    responsive: {
      items: [
        'The legend sits beside the donut and wraps below it on narrow screens; the donut never shrinks below `size` unless the container is narrower.',
      ],
    },
    theme: {
      items: [
        'Slices use `--mn-color-data-1` … `-5`, contrast-checked against every chart background in light, dark and high-contrast themes; "Other" uses `--mn-color-text-subtle`.',
        'The empty ring is `--mn-color-bg-muted`; the frame uses ChartFrame tokens.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Assets by type',
          stack: true,
          render: () => (
            <DonutChart
              title="Assets by type"
              description="All active assets"
              segments={devices}
              centerLabel="assets"
              locale="en-US"
              style={{ width: '100%', maxWidth: '30rem' }}
            />
          ),
        },
        {
          title: 'Folding into Other, percentages only',
          stack: true,
          render: () => (
            <DonutChart
              title="Tickets by channel"
              segments={[
                { key: 'email', label: 'Email', value: 540 },
                { key: 'chat', label: 'Chat', value: 410 },
                { key: 'form', label: 'Web form', value: 160 },
                { key: 'phone', label: 'Phone', value: 70 },
                { key: 'api', label: 'API', value: 25 },
                { key: 'social', label: 'Social', value: 12 },
                { key: 'sms', label: 'SMS', value: 6 },
              ]}
              maxSegments={5}
              legendContent="percent"
              size={140}
              locale="en-US"
              style={{ width: '100%', maxWidth: '30rem' }}
            />
          ),
        },
        {
          title: 'Loading and empty',
          stack: true,
          render: () => (
            <div style={{ display: 'grid', gap: '1rem', width: '100%', maxWidth: '30rem' }}>
              <DonutChart title="Assets by type" segments={[]} status="loading" size={120} />
              <DonutChart
                title="Assets by type"
                segments={[]}
                size={120}
                emptyDescription="Assets appear here once you import them."
              />
            </div>
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Keep to a handful of parts; let `maxSegments` fold the long tail into Other.',
        'Pin `color` per entity when the same parts appear in several charts.',
        'Use `centerLabel` to name the unit of the total ("assets", "tickets").',
      ],
      dont: [
        'Do not use status colours for slices, even for statuses: label them instead.',
        'Do not use a donut for values that do not sum to a whole.',
        'Do not hide the legend unless the same breakdown is listed next to it.',
      ],
    },
    related: {
      items: [
        { id: 'chart-frame', reason: 'The container, states and table every chart shares.' },
        { id: 'stacked-proportion-bar', reason: 'The same breakdown in one compact bar.' },
        { id: 'bar-list', reason: 'Ranking or comparing parts precisely.' },
        { id: 'stat-card', reason: 'A single total without a breakdown.' },
      ],
    },
    dependencies: { packages: ['react'], components: ['chart-frame'] },
    testing: {
      unit: [
        'Figure named by the title; generated summary; centre total; legend with value and share; hidden table with a total row.',
        'One palette arc per non-zero segment, largest first; sort none and pinned colours.',
        'Folding into a neutral Other with every segment kept in the table.',
        'View-as-table toggle with the keyboard and back.',
        'Custom centre, formatter, legend content and summary; hiding centre and legend.',
        'Empty, loading and error states; tiny shares keep a sliver and read "<1%"; ref; SSR.',
      ],
      axe: true,
      ssr: true,
      browser: [
        'Visual baselines: /__gallery/donut-chart',
        'Cross-browser axe: /components/donut-chart',
      ],
    },
  },
} satisfies ComponentDocs;
