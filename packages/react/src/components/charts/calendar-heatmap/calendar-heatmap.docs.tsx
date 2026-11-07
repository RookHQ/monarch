import { CalendarHeatmap } from '@rookcom/monarch-react/charts';
import type { ComponentDocs } from '../../../internal/component-docs.js';

// A deterministic half year of daily counts (no Date.now, so server and client agree).
const DAY = 86_400_000;
const start = Date.UTC(2026, 0, 1);
const year = Array.from({ length: 181 }, (_, i) => {
  const date = new Date(start + i * DAY).toISOString().slice(0, 10);
  const weekday = new Date(start + i * DAY).getUTCDay();
  const weekend = weekday === 0 || weekday === 6;
  const value = weekend ? (i % 5 === 0 ? 1 : 0) : (i * 7 + weekday * 3) % 11;
  return { date, value };
});

const month = [
  { date: '2026-03-02', value: 480, valueLabel: '8h' },
  { date: '2026-03-03', value: 450, valueLabel: '7h 30m' },
  { date: '2026-03-04', value: 0, note: 'Missing hours', marker: true },
  { date: '2026-03-05', value: 510, valueLabel: '8h 30m' },
  { date: '2026-03-06', value: 240, valueLabel: '4h' },
  { date: '2026-03-07', value: null, inactive: true },
  { date: '2026-03-08', value: null, inactive: true },
  { date: '2026-03-09', value: null, note: 'Holiday', inactive: true },
  { date: '2026-03-10', value: 465, valueLabel: '7h 45m' },
  { date: '2026-03-11', value: 420, valueLabel: '7h' },
  { date: '2026-03-12', value: 600, valueLabel: '10h' },
];

export default {
  id: 'calendar-heatmap',
  title: 'CalendarHeatmap',
  summary:
    'Values by day on a calendar grid — a contribution-style year or a month calendar — in ChartFrame, with a Less–More legend, keyboard inspection and a data table.',
  sections: {
    purpose: {
      summary:
        'Shows daily activity or attendance at a glance — streaks, gaps, busy weeks — on a grid people already read as a calendar, with every day available as text. Needed by: People (timesheet month strip, attendance), activity views in other apps.',
      useWhen: [
        'One value per day over weeks or a year (contributions, sign-ins, hours logged).',
        'A month view where each day opens its details (`layout="month"`).',
      ],
      avoidWhen: [
        'Two arbitrary categories (weekday × hour): use Heatmap.',
        'A trend over time where exact values matter: use LineChart or BarChart.',
        'Scheduling or showing events on dates: use a calendar month view.',
      ],
    },
    designPrinciples: {
      items: [
        'Built on ChartFrame and the same four-step scale and legend as Heatmap, so a darker day always means more.',
        'Dates are calendar dates (`YYYY-MM-DD`), read without time zones; pass `today` from the app so the server and the browser draw the same grid.',
        'Zero, "no data" and non-working days look different: muted, hollow and dashed. Notes ("Holiday") and markers (a dot) add meaning without colour.',
        'Weekday and month names come from `Intl` in `locale`, and `weekStartsOn` follows the workspace setting.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Takes every ChartFrame prop except `children`, `legend`, `table` and `height`; the ref goes to the `<figure>`.',
        'Reuses `heatmapScale` and `HeatmapLegend` from Heatmap.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'The range defaults to the earliest and latest day in `days`; set `from` and `to` to draw a fixed year or month. Days in the range without an entry are "no data".',
        'Migrating from the app copy: People\'s `MonthSummary` day strip becomes `layout="month"` with `days` = `{ date, value: totalMinutes, valueLabel: fmtMinutes(…), note: holiday or leave, inactive: !isWorkingDay }`, `weekStartsOn` from settings, and `onOpenDay` → `onDaySelect`. The lead blank cells and the inline `hsl(var(--primary) / …)` intensity are handled by the component.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'layout',
          values: ['year', 'month'],
          description:
            '`year`: weeks as columns, weekdays as rows. `month`: a calendar with day numbers and values. Exposed as `data-layout`.',
        },
        {
          prop: 'weekStartsOn',
          values: ['0–6'],
          description: 'First day of the week; default 1 (Monday).',
        },
        {
          prop: 'color',
          values: ['1', '2', '3', '4', '5', 'neutral'],
          description: 'Palette slot of the ramp. Default 1.',
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
        { name: 'Level', attribute: 'data-level="0"…"4"', description: "The day's step." },
        { name: 'No data', attribute: 'data-empty', description: 'Hollow day.' },
        {
          name: 'Non-working',
          attribute: 'data-inactive',
          description: 'Dashed, subtle day unless it has a value.',
        },
        { name: 'Today', attribute: 'data-today', description: 'Accent outline.' },
        {
          name: 'Inspected',
          attribute: 'data-active',
          description: 'Outlined day with its tooltip.',
        },
        {
          name: 'Empty',
          attribute: 'data-status="empty"',
          description: 'Automatically when no day has an entry.',
        },
      ],
    },
    interaction: {
      items: [
        'Hovering or tapping a day outlines it and shows its date, value and note.',
        'Click, Enter or Space calls `onDaySelect` with the day (days without an entry come with `value: null`).',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Focuses the calendar and inspects the first day.' },
        {
          keys: 'Arrow keys',
          action: "Move by day and week in the grid's direction; stop at the ends of the range.",
        },
        { keys: 'Home / End', action: 'First or last day.' },
        { keys: 'PageUp / PageDown', action: 'Same day in the previous or next month.' },
        { keys: 'Enter / Space', action: 'Select the inspected day (`onDaySelect`).' },
        { keys: 'Escape', action: 'Hide the tooltip.' },
      ],
    },
    accessibility: {
      role: 'figure; the calendar is role="group" with aria-roledescription "chart"',
      name: 'The figure is labelled by `title`; the calendar by the generated summary ("181 days from … to …. Total 812. Highest …") or `summary`.',
      items: [
        'One tab stop; keyboard inspection is announced in a polite live region ("Mon, Mar 2, 2026: 8h").',
        'The SVG and tooltip are hidden; the data table (date, value, note) is always in the page and visible through "View as table".',
        'Every word, including the summary, is replaceable through `labels`; dates and weekday names follow `locale`.',
      ],
    },
    responsive: {
      items: [
        'Cells shrink to fit the width (a full year fits 390px at about 6px per day) and never scroll sideways; month labels skip when crowded.',
        'In the month layout, day numbers show from 28px cells and values from 44px cells.',
      ],
    },
    theme: {
      items: [
        'Same ramp as Heatmap (`--mn-color-data-*` mixed into `--mn-color-bg-muted`); today uses `--mn-color-accent-solid`, markers `--mn-color-warning-solid`.',
        'Forced colours keep the ramp with CanvasText borders; today is LinkText and the inspected day Highlight.',
        'The outline transition is removed under reduced motion.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Half a year of activity',
          stack: true,
          render: () => (
            <CalendarHeatmap
              title="Contributions"
              description="January to June 2026"
              days={year}
              from="2026-01-01"
              to="2026-06-30"
              today="2026-06-30"
              locale="en-US"
              style={{ width: '100%', maxWidth: '44rem' }}
            />
          ),
        },
        {
          title: 'Month calendar of hours logged',
          stack: true,
          render: () => (
            <CalendarHeatmap
              title="March 2026"
              description="Hours logged per day"
              layout="month"
              days={month}
              from="2026-03-01"
              to="2026-03-31"
              today="2026-03-12"
              color={2}
              locale="en-US"
              style={{ width: '100%', maxWidth: '28rem' }}
            />
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Pass `today` and `locale` from the app so the server and browser render the same grid.',
        'Mark non-working days with `inactive` and explain flagged days in `note`.',
        'Use the month layout when each day opens its details.',
      ],
      dont: [
        "Do not pass `Date` objects or timestamps: use `YYYY-MM-DD` strings in the workspace's calendar.",
        'Do not colour days by status (present, absent): the ramp means "more"; use notes or a status calendar.',
      ],
    },
    related: {
      items: [
        { id: 'heatmap', reason: 'Two arbitrary categories instead of dates.' },
        { id: 'calendar-month-view', reason: 'Events and scheduling on a month grid.' },
        { id: 'chart-frame', reason: 'The container, states and table every chart shares.' },
      ],
    },
    dependencies: { packages: ['react'], components: ['chart-frame', 'heatmap'] },
    testing: {
      unit: [
        'Every day of the range on a Monday-first week grid; inactive, empty, today and marker days; summary.',
        'Table of days with values and notes.',
        'Keyboard by day and week, stopping at the range ends; Home, End, Enter and Escape; live region.',
        'Month layout with day numbers, values, weekday header and arrow keys by day; Sunday-first weeks.',
        'Hover tooltip and click selection; controlled active date; translated words.',
        'Range defaults to the data; empty state; SSR.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
