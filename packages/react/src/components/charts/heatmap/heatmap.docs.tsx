import { Heatmap } from '@rookcom/monarch-react/charts';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const weekdays = [
  { key: 'mon', label: 'Mon' },
  { key: 'tue', label: 'Tue' },
  { key: 'wed', label: 'Wed' },
  { key: 'thu', label: 'Thu' },
  { key: 'fri', label: 'Fri' },
];
const hours = Array.from({ length: 10 }, (_, i) => ({
  key: String(8 + i),
  label: `${8 + i}:00`,
}));
const activity = weekdays.flatMap((d, r) =>
  hours.map((h, c) => ({
    row: d.key,
    column: h.key,
    value: Math.round(((Math.sin(r + c / 2) + 1.2) * (c > 1 && c < 8 ? 12 : 4)) % 30),
  })),
);
const teams = [
  { key: 'design', label: 'Design' },
  { key: 'support', label: 'Support' },
  { key: 'finance', label: 'Finance' },
];
const weeks = ['W1', 'W2', 'W3', 'W4'].map((w) => ({ key: w, label: w }));
const attendance = [
  [96, 92, 98, 90],
  [88, 85, null, 91],
  [99, 97, 95, 94],
].flatMap((row, r) =>
  row.map((value, c) => ({
    row: teams[r]?.key ?? '',
    column: weeks[c]?.key ?? '',
    value,
    valueLabel: value === null ? undefined : `${value}%`,
  })),
);

export default {
  id: 'heatmap',
  title: 'Heatmap',
  summary:
    'A matrix of values as shaded cells (weekday × hour, team × week) in ChartFrame, with a Less–More legend, keyboard inspection and a data table.',
  sections: {
    purpose: {
      summary:
        'Shows where values concentrate across two categories, such as activity by weekday and hour or attendance by team and week, with every value still available as text. Needed by: People (attendance), Links (activity by hour).',
      useWhen: [
        'Two categorical axes and one value per pair, where the pattern matters more than exact comparison.',
        'Spotting busy hours, gaps or outliers in a grid of up to a few hundred cells.',
      ],
      avoidWhen: [
        'Values by calendar day: use CalendarHeatmap, which lays out weeks and months for you.',
        'Comparing a handful of values precisely: use BarChart or BarList.',
        'Showing change over time for one series: use LineChart.',
      ],
    },
    designPrinciples: {
      items: [
        'Built on ChartFrame: the title labels the figure, and loading, empty, error and "View as table" behave like every Monarch chart.',
        'One palette colour in four steps (`color`), never a rainbow: darker means more. Zero is the muted background and missing cells are hollow, so "none" and "no data" never look alike.',
        "The legend names each step's range in text, the tooltip and table give the exact value, and the plot has a generated summary naming the highest cell.",
        'Cells stay square and shrink with the container; column labels thin out instead of overlapping.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Takes every ChartFrame prop except `children`, `legend`, `table` and `height`; the ref goes to the `<figure>`.',
        'Also exports `heatmapScale(values, thresholds?)` (the four-step binning) and `HeatmapLegend` (the Less–More legend), which CalendarHeatmap and WorldMap share.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Cells are matched to `rows` and `columns` by key; a pair missing from `data` (or with `value: null`) is "no data".',
        'Default cut points split the largest value into quarters; pass `thresholds` (three ascending numbers) to keep the steps stable between periods.',
        'Migrating from the app copy: People\'s timesheet day strip and its attendance grids map their per-cell `intensity` / `totalMinutes` to `value` and keep their text (such as "7h 30m") in `valueLabel`; holiday or leave text goes in `note`. Links\' hourly activity becomes `rows` = weekdays, `columns` = hours. Inline `rgba()` / `hsl(var(--primary) / …)` intensities are replaced by `color` and the token ramp; `onClick` per cell becomes `onCellSelect`.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'color',
          values: ['1', '2', '3', '4', '5', 'neutral'],
          description: 'Palette slot of the ramp. Default 1.',
        },
        {
          prop: 'showValues',
          values: ['false', 'true'],
          description: 'Print values inside cells that are at least 24px wide.',
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
          name: 'Level',
          attribute: 'data-level="0"…"4"',
          description: 'Zero is level 0; positive values fall into levels 1–4.',
        },
        {
          name: 'No data',
          attribute: 'data-empty',
          description: 'Hollow cell with a subtle border.',
        },
        {
          name: 'Inspected',
          attribute: 'data-active',
          description: 'Outlined cell with its tooltip; announced when reached by keyboard.',
        },
        {
          name: 'Empty',
          attribute: 'data-status="empty"',
          description: 'Automatically when no cell has a value.',
        },
        {
          name: 'Selectable',
          attribute: 'data-selectable',
          description: 'Set when `onCellSelect` is given: cells show a pointer cursor.',
        },
      ],
    },
    interaction: {
      items: [
        'Hovering or tapping a cell outlines it and shows its row, column, value and note in a tooltip.',
        'Click, Enter or Space calls `onCellSelect` with the cell, for drill-in.',
        'The mouse leaving the grid hides the tooltip; touch keeps the last tapped cell.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Focuses the grid and inspects the first cell.' },
        { keys: 'Arrow keys', action: 'Move one cell in that direction.' },
        { keys: 'Home / End', action: 'First or last cell of the row.' },
        { keys: 'Ctrl + Home / Ctrl + End', action: 'First or last cell of the grid.' },
        { keys: 'PageUp / PageDown', action: 'Move a quarter of the row.' },
        { keys: 'Enter / Space', action: 'Select the inspected cell (`onCellSelect`).' },
        { keys: 'Escape', action: 'Hide the tooltip.' },
      ],
    },
    accessibility: {
      role: 'figure; the grid is role="group" with aria-roledescription "chart"',
      name: 'The figure is labelled by `title`; the grid by the generated summary ("5 rows by 10 columns. Highest Wed, 13:00 29") or `summary`.',
      items: [
        'The grid is one tab stop; arrow-key inspection is announced through a polite live region ("Tue, 10:00: 6, Busiest"); pointer hovering is not.',
        'The SVG and tooltip are hidden; the data table (rows × columns) is always in the page for screen readers and visible through "View as table".',
        'Legend steps are list items whose ranges ("< 8", "8–15", "≥ 23") are real text.',
        'Every word is replaceable through `labels`, including the summary.',
      ],
    },
    responsive: {
      items: [
        'Cells shrink to fit the container width (down to 4px) and never cause horizontal scrolling; `maxCellSize` caps them on wide screens.',
        'Row labels take at most a third of the width and truncate; column labels are thinned so they never overlap.',
      ],
    },
    theme: {
      items: [
        'The ramp mixes `--mn-color-data-*` into `--mn-color-bg-muted` at 25/50/75/100%, so it follows every theme; zero is `--mn-color-bg-muted` and missing cells are hollow.',
        'Forced colours keep the ramp (`forced-color-adjust: none`) with CanvasText cell borders and a Highlight outline on the inspected cell.',
        'The only motion is the outline transition, removed under reduced motion.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Activity by weekday and hour',
          stack: true,
          render: () => (
            <Heatmap
              title="Sign-ins by hour"
              description="Last four weeks"
              rows={weekdays}
              columns={hours}
              data={activity}
              rowHeader="Day"
              locale="en-US"
              style={{ width: '100%', maxWidth: '40rem' }}
            />
          ),
        },
        {
          title: 'Attendance by team, with values and a gap',
          stack: true,
          render: () => (
            <Heatmap
              title="Attendance by team"
              rows={teams}
              columns={weeks}
              data={attendance}
              rowHeader="Team"
              color={3}
              thresholds={[85, 90, 95]}
              showValues
              locale="en-US"
              style={{ width: '100%', maxWidth: '30rem' }}
            />
          ),
        },
        {
          title: 'Loading',
          stack: true,
          render: () => (
            <Heatmap
              title="Sign-ins by hour"
              rows={weekdays}
              columns={hours}
              data={[]}
              status="loading"
              style={{ width: '100%', maxWidth: '30rem' }}
            />
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Fix `thresholds` when people compare the same heatmap across periods.',
        'Put the unit or period in `description`, and readable text (durations, percentages) in `valueLabel`.',
        'Use `onCellSelect` to open the records behind a cell.',
      ],
      dont: [
        'Do not use status colours or several hues: one palette colour in steps reads as "more".',
        'Do not encode a value as zero when it is unknown: leave it out or use `null`.',
        'Do not use a heatmap for a handful of values that need exact comparison.',
      ],
    },
    related: {
      items: [
        { id: 'calendar-heatmap', reason: 'Values by calendar day, laid out as weeks or a month.' },
        { id: 'chart-frame', reason: 'The container, states and table every chart shares.' },
        { id: 'bar-chart', reason: 'Exact comparison of a few categories.' },
        { id: 'world-map', reason: 'The same sequential scale by country.' },
      ],
    },
    dependencies: { packages: ['react'], components: ['chart-frame'] },
    testing: {
      unit: [
        'heatmapScale quarters, zero and missing values, fixed thresholds.',
        'One cell per row and column with levels, empty cells, summary, legend ranges and colour slot.',
        'Data table with row headers, notes and "No data".',
        'Two-dimensional keyboard inspection, Ctrl+Home, Enter selection, Escape; live region.',
        'Hover tooltip, click selection, mouse leave; controlled active cell.',
        'Values in cells, value labels, translated words, empty state, legend hidden, ref, SSR.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
