import { useState } from 'react';
import { LineChart } from '@rookcom/monarch-react/charts';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const DAYS = Array.from({ length: 14 }, (_, i) => new Date(Date.UTC(2026, 2, 1 + i, 12)));
const SESSIONS = [820, 932, 901, 934, 1290, 1330, 1320, 1180, 1250, 1400, 1380, 1460, 1510, 1490];
const SIGNUPS = [42, 51, 47, 60, 88, 91, 85, 70, 76, 95, 90, 99, 104, 101];
const TRAFFIC = DAYS.map((date, i) => ({
  date,
  sessions: SESSIONS[i] ?? 0,
  previous: Math.round((SESSIONS[i] ?? 0) * 0.82),
}));
const SIGNUP_DATA = DAYS.map((date, i) => ({ date, signups: SIGNUPS[i] ?? 0 }));
const LATENCY = DAYS.map((date, i) => ({
  date,
  p95: [210, 220, null, null, 260, 240, 410, 380, 250, 230, 225, null, 240, 235][i] ?? null,
}));
const UTC_DAY: Intl.DateTimeFormatOptions = { month: 'short', day: 'numeric', timeZone: 'UTC' };

function SyncedDemo() {
  const [active, setActive] = useState<number | null>(null);
  return (
    <>
      <LineChart
        title="Sessions"
        data={TRAFFIC}
        xKey="date"
        xLabel="Day"
        series={[{ key: 'sessions', label: 'Sessions' }]}
        locale="en-US"
        xFormat={UTC_DAY}
        height={160}
        activeIndex={active}
        onActiveIndexChange={setActive}
      />
      <LineChart
        title="Sign-ups"
        data={SIGNUP_DATA}
        xKey="date"
        xLabel="Day"
        series={[{ key: 'signups', label: 'Sign-ups', color: 2 }]}
        locale="en-US"
        xFormat={UTC_DAY}
        height={160}
        activeIndex={active}
        onActiveIndexChange={setActive}
      />
    </>
  );
}

export default {
  id: 'line-chart',
  title: 'LineChart',
  summary:
    'Line and area charts for trends: multiple series, gaps, reference lines, crosshair tooltip on hover, touch and keyboard, legend toggles and a data table.',
  sections: {
    purpose: {
      summary:
        'Shows how one or more measures change over time or across ordered categories, with exact values one hover, tap or arrow key away and the full data as a table.',
      useWhen: [
        'A trend over days, weeks or months: traffic, sign-ups, revenue, latency.',
        'Comparing a few series on the same scale, or a period against the previous one (`dashed`).',
        'Showing a target, baseline or SLA against the trend (`referenceLines`).',
      ],
      avoidWhen: [
        'Comparing unordered categories: use BarChart.',
        'A trend inside a table cell or stat card: use Sparkline.',
        'Two measures on different scales: draw two charts with a shared `activeIndex` instead of a second axis.',
      ],
    },
    designPrinciples: {
      items: [
        'One value axis. Different scales get separate charts, synced through `activeIndex`, never a dual axis.',
        'Gaps are honest: a missing value breaks the line instead of drawing a fake segment, and a lone point gets a dot.',
        'Recessive grid and axes, 2px lines, colour slots in fixed order; colours follow the series, so hiding one never repaints the rest.',
        'Exact values on demand (tooltip), not on every point.',
        'Every number and date goes through `Intl` with the `locale` prop.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Renders ChartFrame (ref to its `<figure>`); every ChartFrame prop except `children`, `legend` and `table` passes through, and LineChart builds the legend and table from `series` and `data`.',
        '`data` is an array of plain objects; `xKey` names the x field (Date, number or label) and each series `key` names a numeric field. Missing, `null` or non-finite values are gaps.',
        'Pass `locale` when rendering on the server so server and browser format numbers and dates the same way.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [
        {
          prop: 'variant',
          values: ['line', 'area'],
          description:
            '`area` adds a soft fill from each line to the baseline. Exposed as `data-variant`.',
        },
        {
          prop: 'curve',
          values: ['linear', 'monotone'],
          description: '`monotone` smooths the line without overshooting the data.',
        },
        {
          prop: 'xScale',
          values: ['time', 'category'],
          description:
            '`time` (default when every x is a Date or number) spaces points by value, so uneven dates show; `category` spaces them evenly.',
        },
        {
          prop: 'series[].dashed',
          values: ['true', 'false'],
          description: 'A dashed line as a second encoding: previous period, forecast.',
        },
        {
          prop: 'dots',
          values: ['true', 'false'],
          description: 'A dot on every point; by default only lone points and the inspected point.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Inspecting',
          description:
            'Crosshair, enlarged dots on each visible series and a tooltip with every visible value at that x.',
        },
        {
          name: 'Series hidden',
          attribute: 'aria-pressed="false" on the legend toggle',
          description: 'The series leaves the plot and the y range; it stays in the data table.',
        },
        {
          name: 'Empty',
          attribute: 'data-status="empty"',
          description: 'No rows: ChartFrame shows its empty state.',
        },
        {
          name: 'Loading / error',
          attribute: 'data-status',
          description: 'Pass `status` (and `onRetry`); the frame keeps the chart height.',
        },
        {
          name: 'Focus',
          description: 'The plot shows the Monarch focus ring on `:focus-visible`.',
        },
      ],
    },
    interaction: {
      items: [
        'Hovering or tapping the plot inspects the nearest point; the tooltip flips to stay inside the chart.',
        'Moving the mouse out clears it; a tap keeps the last position until the next tap.',
        'Legend entries toggle their series (`legendToggle`), controlled with `hiddenSeries` or uncontrolled with `defaultHiddenSeries`.',
        '`activeIndex` + `onActiveIndexChange` share one crosshair between charts with the same x values.',
        '`onPointSelect` runs on click, Enter or Space at the inspected point, for drill-in.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Focuses the plot (one tab stop) and inspects the first point.' },
        { keys: 'ArrowRight / ArrowDown', action: 'Next point.' },
        { keys: 'ArrowLeft / ArrowUp', action: 'Previous point.' },
        { keys: 'Home / End', action: 'First or last point.' },
        { keys: 'PageUp / PageDown', action: 'Moves a tenth of the points.' },
        { keys: 'Escape', action: 'Hides the tooltip.' },
        { keys: 'Enter / Space', action: 'Calls `onPointSelect` for the inspected point.' },
      ],
    },
    accessibility: {
      role: 'figure (ChartFrame); the plot is a group with aria-roledescription "chart"',
      name: 'The figure is named by `title`; the plot by a generated summary (points, then low / high / latest per series, then reference lines) or `summary`.',
      items: [
        'The SVG is `aria-hidden`; the summary, a live region and the data table carry the information.',
        'While inspecting with the keyboard, a polite live region announces "Mar 4: Sessions 1,204, Previous 988".',
        'The tooltip is visual only (`aria-hidden`), so pointer hovering does not flood screen readers.',
        'A hint ("Use the arrow keys to inspect values.") is the plot\'s description.',
        'The data table lists every series, hidden ones included, with row and column headers.',
        'Series differ by label and, where set, dash pattern, not by colour alone.',
        'Every string, including the summary sentences, is translatable through `labels`.',
      ],
    },
    responsive: {
      items: [
        'The chart measures its container (ResizeObserver) and lays out at that width, so text never scales; it works down to 320px.',
        'X labels thin out to keep about 72px between them; the first and last stay.',
        'Before the width is known (server render, first paint) it draws at 640px scaled to fit, so the markup matches on hydration.',
        'Touch: tap to inspect; vertical page scrolling stays available (`touch-action: pan-y`).',
      ],
    },
    theme: {
      items: [
        'Series use `--mn-color-data-1` … `-5`, contrast-checked per theme; a sixth series and later fall back to the neutral slot.',
        'Grid, ticks, crosshair and reference lines use border and text tokens; the tooltip uses the raised surface and shadow.',
        'Light, dark and high-contrast themes follow the tokens with no extra CSS. Reduced motion: the only motion is the tooltip fade, which uses duration tokens that become 0ms.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Two series with a previous period and a target',
          stack: true,
          render: () => (
            <LineChart
              title="Sessions"
              description="Last 14 days against the previous 14"
              data={TRAFFIC}
              xKey="date"
              xLabel="Day"
              series={[
                { key: 'sessions', label: 'This period' },
                { key: 'previous', label: 'Previous period', color: 'neutral', dashed: true },
              ]}
              referenceLines={[{ value: 1200, label: 'Target' }]}
              locale="en-US"
              xFormat={UTC_DAY}
            />
          ),
          code: `<LineChart
  title="Sessions"
  description="Last 14 days against the previous 14"
  data={traffic}               // [{ date: Date, sessions: 820, previous: 672 }, …]
  xKey="date"
  xLabel="Day"
  series={[
    { key: 'sessions', label: 'This period' },
    { key: 'previous', label: 'Previous period', color: 'neutral', dashed: true },
  ]}
  referenceLines={[{ value: 1200, label: 'Target' }]}
  locale="en-US"
/>`,
        },
        {
          title: 'Area with gaps',
          stack: true,
          render: () => (
            <LineChart
              title="p95 latency"
              description="Days with no traffic are gaps"
              variant="area"
              curve="monotone"
              data={LATENCY}
              xKey="date"
              xLabel="Day"
              series={[{ key: 'p95', label: 'p95', color: 3 }]}
              includeZero={false}
              formatValue={(v) => `${v} ms`}
              locale="en-US"
              xFormat={UTC_DAY}
              height={180}
            />
          ),
          code: `<LineChart
  title="p95 latency"
  variant="area"
  curve="monotone"
  data={latency}               // p95: null on days with no traffic
  xKey="date"
  series={[{ key: 'p95', label: 'p95', color: 3 }]}
  includeZero={false}
  formatValue={(v) => \`\${v} ms\`}
/>`,
        },
        {
          title: 'Two charts sharing one crosshair',
          stack: true,
          render: () => <SyncedDemo />,
          code: `const [active, setActive] = useState<number | null>(null);

<LineChart title="Sessions" … activeIndex={active} onActiveIndexChange={setActive} />
<LineChart title="Sign-ups" … activeIndex={active} onActiveIndexChange={setActive} />`,
        },
      ],
    },
    doDont: {
      do: [
        'Pin `color` on series that a filter can remove, so colours follow the entity.',
        'Use `dashed` for a comparison period or forecast, so it differs by more than colour.',
        'Pass `locale` (and `xFormat` with `timeZone` for dates) for consistent server and client output.',
        'Fold a sixth series and beyond into "Other", or split the chart.',
      ],
      dont: [
        'Put two measures with different units on one chart; use two charts with a shared `activeIndex`.',
        'Replace gaps with zeros: a missing day is not a zero day.',
        'Label every point; the tooltip and table carry exact values.',
      ],
    },
    related: {
      items: [
        {
          id: 'chart-frame',
          reason: 'The frame LineChart renders; use it directly for custom charts.',
        },
        { id: 'bar-chart', reason: 'Comparisons across categories or discrete periods.' },
        { id: 'sparkline', reason: 'A tiny trend without axes, for tables and cards.' },
        { id: 'stat-card', reason: 'One headline number with its trend.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['chart-frame'],
      notes: [
        'Hand-written SVG with no charting library (docs/adr/0010-charts-rendering-and-colour.md). Scales, paths, tooltip, legend and axes come from `src/internal/charts`.',
      ],
    },
    testing: {
      unit: [
        'One line per series, legend, generated summary, keyboard hint; no axe violations.',
        'Keyboard: Tab focuses the plot and inspects the first point; arrows, Home, End, Escape; live region announces; leaving clears the tooltip.',
        'Hover inspects the nearest point with a crosshair and flipped tooltip, without announcing; leaving clears it.',
        'Gaps split the path and a lone point gets a dot; a single series has no legend.',
        'Legend toggle hides a series (uncontrolled) and keeps the other colour; controlled `hiddenSeries` and `activeIndex`.',
        '`onPointSelect` from Enter and click.',
        'Locale-aware numbers and dates (de-DE currency and long month).',
        'Area, monotone curve, dots and reference lines (also in the summary); fixed y domain and custom summary.',
        'Empty data shows the empty state; loading passes through; the table lists every series.',
        'Renders on the server at the fallback width.',
      ],
      axe: true,
      ssr: true,
      browser: [
        'Visual baselines: /__gallery/line-chart',
        'Cross-browser axe: /components/line-chart',
      ],
    },
  },
} satisfies ComponentDocs;
