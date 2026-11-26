import { useState } from 'react';
import { TabsFilter } from '@rookcom/monarch-react';
import { ChartFrame, Sparkline, type ChartTableData } from '@rookcom/monarch-react/charts';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const WEEK = [
  { day: 'Mon', requests: 48 },
  { day: 'Tue', requests: 61 },
  { day: 'Wed', requests: 55 },
  { day: 'Thu', requests: 72 },
  { day: 'Fri', requests: 66 },
];

const TABLE: ChartTableData = {
  columns: [
    { key: 'day', label: 'Day' },
    { key: 'requests', label: 'Requests' },
  ],
  rows: WEEK.map((d) => ({ day: d.day, requests: d.requests })),
};

const RANGES = [
  { value: '7d', label: '7 days' },
  { value: '30d', label: '30 days' },
];

function FrameDemo() {
  const [range, setRange] = useState('7d');
  return (
    <ChartFrame
      title="Requests"
      description="New requests per day"
      headingLevel={3}
      height={96}
      actions={
        <TabsFilter
          aria-label="Range"
          variant="pill"
          options={RANGES}
          value={range}
          onValueChange={setRange}
        />
      }
      legend={[{ key: 'requests', label: 'Requests', color: 1 }]}
      table={TABLE}
      footer="Counts update every hour."
    >
      <Sparkline
        values={WEEK.map((d) => d.requests)}
        variant="area"
        color={1}
        height={96}
        responsive
      />
    </ChartFrame>
  );
}

export default {
  id: 'chart-frame',
  title: 'ChartFrame',
  summary:
    'The container every chart sits in: title, controls, legend, loading / empty / error states and an accessible data table.',
  sections: {
    purpose: {
      summary:
        'Gives every chart the same frame and the same accessibility: a labelled figure, a header with period or metric controls, a legend, consistent loading, empty and error states, and the data as a real table, visible on demand and always present for screen readers.',
      useWhen: [
        'Wrapping a custom visualisation that Monarch does not ship, so it still gets the header, states and table fallback.',
        'Building a new chart component: LineChart and BarChart render ChartFrame for you, and the other ./charts components build on it.',
      ],
      avoidWhen: [
        'A single headline number with no plot: use StatCard.',
        'A plain list of labelled values: use DescriptionList or BarList.',
      ],
    },
    designPrinciples: {
      items: [
        'The data is never only a picture: `table` is always rendered for assistive technology and one click away for everyone else.',
        'States keep the reserved `height`, so loading, empty, error and ready swap without the page jumping.',
        'Controls belong in the header (`actions`), above the plot, in one row that wraps on narrow screens.',
        'A legend only for two or more series; a single series is named by the title.',
        'Series colours are palette slots (`chartColor(1…5)` → `--mn-color-data-*`), never status colours and never raw values.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        '`ChartFrame` renders a `<figure>` (ref to it) labelled by `title` and described by `description`; other HTML attributes pass through.',
        '`chartColor(slot)` returns the CSS colour for a palette slot (`1`–`5` → `var(--mn-color-data-N)`, `neutral` → `var(--mn-color-text-subtle)`), for custom marks drawn inside a frame.',
        'Types shared by every chart are exported from here: `ChartSeries`, `ChartDatum`, `ChartColor`, `ChartLegendItem`, `ChartTableData`, `ChartTableColumn`.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [
        {
          prop: 'status',
          values: ['ready', 'loading', 'empty', 'error'],
          description:
            'Which body shows: the chart, a skeleton, an EmptyState or an error (InlineErrorRetry with `onRetry`, otherwise an alert). Exposed as `data-status`.',
        },
        {
          prop: 'headingLevel',
          values: ['2', '3', '4', '5', '6'],
          description: 'Renders the title as that heading; without it the title is a plain block.',
        },
        {
          prop: 'legendMarker',
          values: ['line', 'square'],
          description: 'Short line (line charts) or square (bars) beside each legend label.',
        },
        {
          prop: 'tableToggle',
          values: ['true', 'false'],
          description:
            'Shows the "View as table" button (default when `table` is set). Off, the table is still present for screen readers.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Ready',
          attribute: 'data-status="ready", data-view="chart"',
          description: 'The chart, plus the visually hidden table.',
        },
        {
          name: 'Table view',
          attribute: 'data-view="table"',
          description:
            'The table replaces the chart in a focusable, scrollable region; the legend hides.',
        },
        {
          name: 'Loading',
          attribute: 'data-status="loading", aria-busy="true"',
          description: 'A skeleton of the reserved height and a polite "Loading chart" status.',
        },
        {
          name: 'Empty',
          attribute: 'data-status="empty"',
          description: 'A compact EmptyState with `emptyDescription` and `emptyActions`.',
        },
        {
          name: 'Error',
          attribute: 'data-status="error"',
          description:
            'InlineErrorRetry with Try again when `onRetry` is set; otherwise a message with `role="alert"`.',
        },
        {
          name: 'Hidden series',
          attribute: 'aria-pressed="false", data-hidden',
          description: 'A toggled-off legend entry: struck-through label and faded swatch.',
        },
      ],
    },
    interaction: {
      items: [
        '"View as table" swaps the chart for the table and changes to "View as chart"; controlled with `showTable` + `onShowTableChange`, or uncontrolled with `defaultShowTable`.',
        'With `onLegendToggle`, each legend entry is a toggle button; the chart decides what hiding means.',
        'Try again calls `onRetry`; return the refetch promise and the button stays busy until it settles.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Tab',
          action:
            'Moves through the header controls, the table toggle, legend toggles and the chart.',
        },
        { keys: 'Enter / Space', action: 'Activates the table toggle or a legend toggle.' },
        {
          keys: 'Arrow keys (table view)',
          action: 'Scroll the focused table region when it overflows.',
        },
      ],
    },
    accessibility: {
      role: 'figure',
      name: '`title` (aria-labelledby); `description` is the accessible description.',
      items: [
        'The data table has a caption (the title unless `table.caption` is set), column headers with `scope="col"` and row headers with `scope="row"`.',
        'While the chart shows, the table sits in a visually hidden container, so screen readers always reach the values.',
        'The visible table is a focusable region (`tabindex="0"`), so keyboard users can scroll it.',
        'Legend toggles are buttons with `aria-pressed`; the legend list is named "Legend" (`labels.legend`).',
        'Loading sets `aria-busy` and announces "Loading chart"; a retry-less error is `role="alert"`.',
        'Identity is never colour alone: every legend entry has its label, dashed series show a dashed marker.',
        'All text is translatable through `labels`.',
      ],
    },
    responsive: {
      items: [
        'The header wraps: actions move under the title when there is no room.',
        'Padding tightens below 30rem; the frame works at 320px.',
        'Legend toggles grow to a 44px touch target on coarse pointers.',
      ],
    },
    theme: {
      items: [
        'Surface, border and text use Monarch tokens and follow light, dark and high-contrast themes.',
        'Series colours are `--mn-color-data-1` … `-5`, chosen and contrast-checked per theme in @rookcom/monarch-tokens (3:1 on every chart background, 4.5:1 in high contrast).',
        'Focus rings use `--mn-color-focus-ring`.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Header controls, legend and table toggle',
          stack: true,
          render: () => <FrameDemo />,
        },
        {
          title: 'Loading, empty and error',
          stack: true,
          render: () => (
            <>
              <ChartFrame title="Loading" status="loading" height={80} />
              <ChartFrame
                title="Empty"
                status="empty"
                height={80}
                emptyDescription="Requests appear here after the first one arrives."
              />
              <ChartFrame title="Error" status="error" height={80} onRetry={() => undefined} />
            </>
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Pass `table` with every value the chart shows, formatted as the chart formats it.',
        'Reserve a `height` close to the real chart so states do not shift the layout.',
        'Put period and metric switches in `actions`.',
      ],
      dont: [
        'Hide the only text alternative: never set `tableToggle={false}` and leave out `table`.',
        'Colour series with status tokens or hex values; use `chartColor` slots.',
        'Put a legend on a single-series chart.',
      ],
    },
    related: {
      items: [
        { id: 'line-chart', reason: 'Trends over time; renders ChartFrame for you.' },
        { id: 'bar-chart', reason: 'Comparisons across categories; renders ChartFrame for you.' },
        { id: 'sparkline', reason: 'A word-sized trend with no frame, for tables and cards.' },
        { id: 'stat-card', reason: 'One headline number, optionally with a sparkline.' },
        { id: 'card', reason: 'A general container when there is no chart.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['button', 'empty-state', 'inline-error-retry', 'skeleton'],
      notes: [
        'Shared chart internals (legend, table, palette) come from `src/internal/charts`; their styles are `styles/chart.css` (`.mn-chart__*`).',
      ],
    },
    testing: {
      unit: [
        'Figure named by the title and described by the description; no axe violations.',
        'The data table is always present (visually hidden) with row and column headers.',
        'The toggle swaps chart and table, uncontrolled and controlled; the visible table is a focusable region.',
        '`tableToggle={false}` removes the button but keeps the hidden table.',
        'Heading level and actions slot render.',
        'Loading (aria-busy, status text), empty (description, actions) and error (Try again calls onRetry; alert without it; translated labels).',
        'Legend toggles expose aria-pressed and report the key; a static legend has no buttons.',
        '`chartColor` maps slots to data tokens only.',
        'Renders on the server.',
      ],
      axe: true,
      ssr: true,
      browser: [
        'Visual baselines: /__gallery/chart-frame',
        'Cross-browser axe: /components/chart-frame',
      ],
    },
  },
} satisfies ComponentDocs;
