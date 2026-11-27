import { useState } from 'react';
import { ChartLegend, LineChart } from '@rookcom/monarch-react/charts';
import type { ComponentDocs } from '../../../internal/component-docs.js';
// The scenario of the primary example, shared with iOS and Android (CROSS-PLATFORM.md section 8).
import fixture from './chart-legend.fixture.json';

const REGIONS = [
  { key: 'north', label: 'North' },
  { key: 'south', label: 'South' },
  { key: 'east', label: 'East' },
  { key: 'west', label: 'West', color: 'neutral' as const, dashed: true },
];
const WEEKS = ['W1', 'W2', 'W3', 'W4', 'W5', 'W6'];
const DATA = WEEKS.map((week, i) => ({
  week,
  north: [12, 15, 14, 18, 21, 24][i] ?? 0,
  south: [8, 9, 12, 11, 13, 15][i] ?? 0,
  east: [5, 7, 6, 9, 8, 10][i] ?? 0,
  west: [10, 10, 10, 10, 10, 10][i] ?? 0,
}));

function SharedLegendDemo() {
  const [hidden, setHidden] = useState<string[]>([]);
  return (
    <>
      <ChartLegend
        items={REGIONS}
        toggleable
        hiddenSeries={hidden}
        onHiddenSeriesChange={setHidden}
      />
      <LineChart
        title="Orders by region"
        data={DATA}
        xKey="week"
        xLabel="Week"
        series={REGIONS}
        hiddenSeries={hidden}
        onHiddenSeriesChange={setHidden}
        legendToggle={false}
        height={180}
        locale="en-US"
      />
    </>
  );
}

export default {
  id: 'chart-legend',
  title: 'ChartLegend',
  summary:
    'A standalone chart legend: labelled colour markers with optional values, and toggles that show or hide series as a keyboard toolbar.',
  sections: {
    purpose: {
      summary:
        'Names the series of one or more charts in their palette colours, and optionally lets people show and hide series. AreaChart and BurndownChart render it; use it directly for a legend shared by several charts or placed away from the plot. Needed by: Rook Links (shadcn `ChartLegendContent` in src/components/ui/chart.tsx), Rook Stride reports, Rook Ranker dashboard line chart.',
      useWhen: [
        'Several charts on a dashboard share the same series and should share one legend and one hidden list.',
        'The legend belongs in a side column or a header, away from the plot.',
        'A legend needs values beside each label (totals, latest values).',
      ],
      avoidWhen: [
        'A single chart: LineChart, BarChart, AreaChart and BurndownChart already render their legend.',
        'Proportions of one whole: StackedProportionBar and DonutChart have their own legends with shares.',
        'Status colours (danger, success): legends name data series, not states.',
      ],
    },
    designPrinciples: {
      items: [
        'Identity is never colour alone: every entry has its text label; dashed series show a dashed or outlined marker; hidden series are struck through.',
        'Colours come from the same slots as the charts (`--mn-color-data-1` … `-5`, then neutral), in the same order, so the legend always matches.',
        'A legend is a list until it does something: plain entries are a `<ul>`, toggles become a toolbar with one tab stop.',
        'Never leave a blank chart by accident: the last visible series cannot be hidden unless `keepOneVisible` is off.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'The hidden list is controlled with `hiddenSeries` + `onHiddenSeriesChange` or uncontrolled with `defaultHiddenSeries`, the same props charts take, so one state can drive a legend and several charts.',
        'Renders a `<ul>` when not toggleable and a `<div role="toolbar">` when toggleable; the ref points to that element.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Migrating from shadcn/ui `ChartLegendContent`: its `payload` (from recharts) becomes `items` (`key`, `label`, `color` slot); `nameKey` is the `label` you pass; `verticalAlign` is page layout (place the legend where you need it); `hideIcon` has no equivalent, because the marker is part of the second encoding.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'marker',
          values: ['line', 'square', 'dot'],
          description:
            'Marker beside each label: a short line for line charts, a square for bars and areas, a dot. Exposed as `data-marker`.',
        },
        {
          prop: 'orientation',
          values: ['horizontal', 'vertical'],
          description:
            'A wrapping row, or a column with values aligned at the end. Exposed as `data-orientation`.',
        },
        {
          prop: 'toggleable',
          values: ['false', 'true'],
          description: 'Entries become toggle buttons in a toolbar with a Show all reset.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Series hidden',
          attribute: 'data-hidden, aria-pressed="false"',
          description: 'Label struck through, marker faded; a Show all button appears.',
        },
        {
          name: 'Last visible series',
          attribute: 'aria-disabled="true"',
          description:
            'With `keepOneVisible` (default) its toggle stays focusable but does nothing.',
        },
        {
          name: 'Focus',
          description: 'Each toggle shows the Monarch focus ring on `:focus-visible`.',
        },
      ],
    },
    interaction: {
      items: [
        'Clicking or tapping an entry shows or hides its series and calls `onHiddenSeriesChange` with the new list.',
        'Show all clears the hidden keys the legend lists (keys it does not list stay hidden) and moves focus to the first entry.',
        '`onActiveChange` reports the entry under the pointer or focus, so a chart can emphasise that series.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Moves into the legend (one tab stop) and out again.' },
        {
          keys: 'ArrowRight / ArrowLeft',
          action:
            'Next or previous entry in a horizontal legend (reversed in right-to-left); wraps around.',
        },
        { keys: 'ArrowDown / ArrowUp', action: 'Next or previous entry in a vertical legend.' },
        { keys: 'Home / End', action: 'First or last entry.' },
        { keys: 'Enter / Space', action: 'Shows or hides the focused series, or runs Show all.' },
      ],
    },
    accessibility: {
      role: 'list (plain) or toolbar (toggleable) with toggle buttons',
      name: '`messages.label` (default "Legend"); each toggle is named by its label, or "{label}, {value}" when a text value is set.',
      items: [
        'Toggles use `aria-pressed`: pressed means the series is visible, which screen readers announce on change.',
        'The toolbar uses a roving tabindex as in the WAI-ARIA toolbar pattern, with `aria-orientation` matching the layout.',
        'Markers are decorative (`aria-hidden`); the label carries the meaning.',
      ],
    },
    responsive: {
      items: [
        'Horizontal legends wrap onto more lines; long labels break rather than overflow at 320px.',
        'On coarse pointers each toggle and Show all are at least 44px tall.',
        'Spacing uses logical properties, so the layout mirrors in right-to-left.',
      ],
    },
    theme: {
      items: [
        'Markers use the data tokens through the shared chart parts, so they match every chart in light, dark and high-contrast themes.',
        'Forced colours: markers keep their colour (`forced-color-adjust: none`), visible toggles get a system outline and Show all uses LinkText.',
        'Nothing animates.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Plain legend with values',
          render: () => (
            <ChartLegend items={fixture.data.items} marker={fixture.data.marker as 'square'} />
          ),
        },
        {
          title: 'Vertical toggles',
          render: () => (
            <ChartLegend
              items={REGIONS}
              toggleable
              orientation="vertical"
              defaultHiddenSeries={['west']}
            />
          ),
        },
        {
          title: 'One legend driving a chart',
          stack: true,
          render: () => <SharedLegendDemo />,
        },
      ],
    },
    doDont: {
      do: [
        'Pass the same `items` order and `color` slots as the charts it describes.',
        'Drive several charts from one `hiddenSeries` state when they share series.',
        'Use `dashed` for comparison or forecast series, matching the chart.',
      ],
      dont: [
        'Use it for status meanings (failed, healthy); use a status legend such as UptimeLegend.',
        'Hide the legend markers or rely on colour to tell series apart.',
        'Render a second legend next to a chart that already shows one; set `legendToggle={false}` on the chart if this legend controls it.',
      ],
    },
    related: {
      items: [
        {
          id: 'line-chart',
          reason: 'Takes the same `hiddenSeries` props, so a shared legend can control it.',
        },
        { id: 'area-chart', reason: 'Renders this legend above its plot.' },
        {
          id: 'burndown-chart',
          reason: 'Renders this legend for its remaining, ideal and scope lines.',
        },
        { id: 'chart-frame', reason: 'The frame most charts sit in.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: [],
      notes: [
        'Markers and spacing come from the shared chart parts (`styles/chart.css`, `.mn-chart__legend*`); colours and the hidden-list model from `src/internal/charts`.',
      ],
    },
    testing: {
      unit: [
        'Plain list: name, swatch colours in slot order, dashed marker, values; no axe violations.',
        'Toolbar: one tab stop, arrows wrap, Home/End, Enter and Space toggle with `aria-pressed`.',
        'Keeps the last visible series (`aria-disabled`), Show all restores and moves focus; `keepOneVisible={false}` allows hiding everything.',
        'Controlled hidden list keeps unknown keys on Show all.',
        '`onActiveChange` on hover, leave and focus.',
        'Vertical orientation uses ArrowUp/ArrowDown; translated messages.',
        'Renders on the server.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
