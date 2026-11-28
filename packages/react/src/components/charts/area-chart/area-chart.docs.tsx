import { AreaChart, type AreaChartProps } from '@rookcom/monarch-react/charts';
import type { ComponentDocs } from '../../../internal/component-docs.js';
// The scenario of the primary example, shared with iOS and Android (CROSS-PLATFORM.md section 8).
import fixture from './area-chart.fixture.json';

const DAYS = fixture.data.days.map((day) => new Date(day));
const [WEB, IOS, ANDROID] = fixture.data.series.map((series) => series.values);
const TRAFFIC = DAYS.map((date, i) => ({
  date,
  web: WEB?.[i] ?? 0,
  ios: IOS?.[i] ?? 0,
  android: ANDROID?.[i] ?? 0,
}));
const PLATFORMS = fixture.data.series.map(({ key, label }) => ({ key, label }));
const USERS = DAYS.map((date, i) => ({
  date,
  total: 1000 + i * 85,
  signups: [40, 52, 47, 60, 88, 91, 85, 70, 76, 95, 90, 99, 104, 101][i] ?? 0,
}));
const UTC_DAY: Intl.DateTimeFormatOptions = { month: 'short', day: 'numeric', timeZone: 'UTC' };

export default {
  id: 'area-chart',
  title: 'AreaChart',
  summary:
    'Area chart for volumes over time: overlapping, stacked to a total or stacked to 100%, with the crosshair tooltip, keyboard inspection, legend toggles and data table of LineChart.',
  sections: {
    purpose: {
      summary:
        'Shows how a volume changes over time and, when stacked, how its parts add up to the total or share it. Needed by: Rook Links (Analytics and AdminAnalytics daily traffic, recharts `<AreaChart>`), Rook ID admin analytics (user growth).',
      useWhen: [
        'A total over time made of parts: traffic by platform, revenue by plan (`stacked`).',
        'How the mix changes over time when the total does not matter (`expand`).',
        'One or two volumes where the filled area helps read magnitude (`overlap`).',
      ],
      avoidWhen: [
        'Comparing trends of independent measures: use LineChart; overlapping fills hide each other beyond two series.',
        'Values that go negative: stacks of negative and positive parts mislead; use BarChart.',
        'Parts of one whole at a single moment: use DonutChart or StackedProportionBar.',
      ],
    },
    designPrinciples: {
      items: [
        'Stacks start at zero; the value axis never cuts a stacked band.',
        'The first series sits at the bottom of the stack, next to the baseline, where it is easiest to read; put the most important part first.',
        'Bands are translucent fills with a solid top edge in the same hue, so neighbouring parts stay distinct and the legend and tooltip name them.',
        'Gaps are honest: a missing value breaks the band instead of drawing it at zero.',
        'Stacked tooltips list parts top to bottom, matching the picture, with the total underneath.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Renders ChartFrame (ref to its `<figure>`); every ChartFrame prop except `children`, `legend` and `table` passes through.',
        'Same data model as LineChart: `data` rows with an `xKey` field and one numeric field per series `key`.',
        'In `stacked` and `expand` mode negative values count as zero in the stack; the table and tooltip still show the real value.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Migrating from recharts / shadcn `<AreaChart>`: each `<Area dataKey name>` becomes a `series` entry (`key`, `label`); a shared `stackId` becomes `stacking="stacked"`; `stackOffset="expand"` becomes `stacking="expand"`; `type="monotone"` becomes `curve="monotone"`; gradient `<defs>` and hex colours go away (colour slots).',
      ],
    },
    variants: {
      items: [
        {
          prop: 'stacking',
          values: ['overlap', 'stacked', 'expand'],
          description:
            'Overlapping areas from the baseline, a stack whose top is the total, or a stack of shares of 100%. Exposed as `data-stacking`.',
        },
        {
          prop: 'curve',
          values: ['linear', 'monotone'],
          description: '`monotone` smooths the edges without overshooting the data.',
        },
        {
          prop: 'xScale',
          values: ['time', 'category'],
          description:
            '`time` (default for dates and numbers) spaces points by value; `category` spaces them evenly.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Inspecting',
          description:
            'Crosshair, a dot on each visible top edge and a tooltip with every value (and the total when stacked).',
        },
        {
          name: 'Series hidden',
          attribute: 'aria-pressed="false" on the legend toggle',
          description:
            'The series leaves the plot and the stack re-forms; it stays in the data table.',
        },
        {
          name: 'Empty, loading, error',
          attribute: 'data-status',
          description:
            "No rows show ChartFrame's empty state; pass `status` and `onRetry` for the others.",
        },
        {
          name: 'Focus',
          description: 'The plot shows the Monarch focus ring on `:focus-visible`.',
        },
      ],
    },
    interaction: {
      items: [
        'Hovering or tapping the plot inspects the nearest x; a mouse leaving clears it, a tap keeps it.',
        'The legend (two or more series) toggles series; control it with `hiddenSeries` or seed it with `defaultHiddenSeries`.',
        '`activeIndex` + `onActiveIndexChange` share the crosshair with other charts on the same x values.',
        '`onPointSelect` runs on click, Enter or Space at the inspected x, for drill-in.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Tab',
          action: 'Moves to the legend toolbar, then to the plot, which inspects the first point.',
        },
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
      name: 'The figure is named by `title`; the plot by a generated summary (points, low / high / latest per series, the total when stacked) or `summary`.',
      items: [
        'The SVG is `aria-hidden`; the summary, a polite live region and the data table carry the values.',
        'Keyboard inspection announces "Mar 4: Web 480, iOS 240, Android 160, Total 880"; pointer hovering is not announced.',
        'In `expand` mode values are announced with their share: "Web 480 (54.5%)".',
        'The data table lists every series, hidden ones included, plus a Total column when stacked.',
        'Every string is translatable through `labels`, including the share format.',
      ],
    },
    responsive: {
      items: [
        'Measures its container and lays out at that width; x labels thin out, down to 320px.',
        'Renders at a 640px fallback width on the server so hydration matches.',
        'Touch: tap to inspect; vertical scrolling stays available.',
      ],
    },
    theme: {
      items: [
        'Series use `--mn-color-data-1` … `-5`, contrast-checked per theme; stacked bands are more opaque than overlapping ones.',
        'Forced colours: fills keep their hue; the crosshair and focus ring use system colours.',
        'Reduced motion: the only motion is the shared tooltip fade, which becomes instant.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Stacked by platform',
          stack: true,
          render: () => (
            <AreaChart
              title={fixture.data.title}
              description={fixture.data.description}
              data={TRAFFIC}
              xKey="date"
              xLabel={fixture.data.xLabel}
              series={PLATFORMS}
              stacking={fixture.data.stacking as AreaChartProps['stacking']}
              curve={fixture.data.curve as AreaChartProps['curve']}
              locale={fixture.data.locale}
              xFormat={UTC_DAY}
            />
          ),
        },
        {
          title: 'Share of 100%',
          stack: true,
          render: () => (
            <AreaChart
              title="Platform mix"
              data={TRAFFIC}
              xKey="date"
              xLabel="Day"
              series={PLATFORMS}
              stacking="expand"
              locale="en-US"
              xFormat={UTC_DAY}
              height={200}
            />
          ),
        },
        {
          title: 'Overlapping areas',
          stack: true,
          render: () => (
            <AreaChart
              title="Users"
              data={USERS}
              xKey="date"
              xLabel="Day"
              series={[
                { key: 'total', label: 'Total users' },
                { key: 'signups', label: 'New sign-ups' },
              ]}
              locale="en-US"
              xFormat={UTC_DAY}
              height={200}
            />
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Order series so the most important part is at the bottom of the stack.',
        'Pin `color` on series a filter can remove, so colours follow the entity.',
        'Pass `locale` (and `xFormat` with a `timeZone`) for consistent server and client output.',
      ],
      dont: [
        'Stack measures that do not add up to a meaningful total.',
        'Overlap more than two or three areas; switch to LineChart or stack them.',
        'Replace gaps with zeros.',
      ],
    },
    related: {
      items: [
        {
          id: 'line-chart',
          reason: 'Trends without fills, including a simple single-area variant.',
        },
        {
          id: 'stacked-bar-chart',
          reason: 'The same parts-of-a-total story for discrete periods or categories.',
        },
        {
          id: 'chart-legend',
          reason: 'The legend AreaChart renders; use it to share one across charts.',
        },
        { id: 'chart-frame', reason: 'The frame AreaChart renders.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['chart-frame', 'chart-legend'],
      notes: [
        'Hand-written SVG with no charting library (docs/adr/0010-charts-rendering-and-colour.md); scales, paths, tooltip and axes come from `src/internal/charts`. Stacked areas were listed as out of scope in ADR 0010; this component adds them on the same internals.',
      ],
    },
    testing: {
      unit: [
        'Overlapping areas, legend toolbar and generated summary; no axe violations.',
        'Stacked: total in the summary, table and tooltip footer; tooltip rows top of the stack first.',
        'Expand: percent ticks and shares in the announcement.',
        'Keyboard: arrows, Home, End, Escape and the live region; hover inspects without announcing.',
        'Legend toggle hides a series, keeps colours and the table; controlled hidden series and active index.',
        'Gaps split the band; a lone point gets a dot; a single series has no legend.',
        '`onPointSelect`, locale formatting, reference lines, custom summary, empty and loading states.',
        'Renders on the server at the fallback width.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
