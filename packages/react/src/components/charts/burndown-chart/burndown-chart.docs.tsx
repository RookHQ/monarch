import { BurndownChart } from '@rookcom/monarch-react/charts';
import type { ComponentDocs } from '../../../internal/component-docs.js';
// The scenario of the primary example, shared with iOS and Android (CROSS-PLATFORM.md section 8).
import fixture from './burndown-chart.fixture.json';

const DAYS = Array.from(
  { length: 10 },
  (_, i) => new Date(Date.UTC(2026, 2, 2 + i + (i >= 5 ? 2 : 0), 12)),
);
const SPRINT = fixture.data.days.map((day) => ({
  date: new Date(day.date),
  remaining: ('remaining' in day ? day.remaining : null) as number | null,
  scope: ('scope' in day ? day.scope : null) as number | null,
}));
const DONE_SPRINT = DAYS.map((date, i) => ({
  date,
  remaining: [30, 28, 24, 21, 18, 14, 11, 7, 3, 0][i] ?? null,
}));
const UTC_DAY: Intl.DateTimeFormatOptions = { month: 'short', day: 'numeric', timeZone: 'UTC' };

export default {
  id: 'burndown-chart',
  title: 'BurndownChart',
  summary:
    'Sprint burndown: work remaining per day against the ideal line, with the scope line, scope changes, a today marker and an ahead / behind summary.',
  sections: {
    purpose: {
      summary:
        'Shows whether a sprint or milestone will finish on time: remaining work each day against a straight ideal line, with scope added or removed along the way. Needed by: Rook Stride (reports page `Burndown`, remaining and scope points from daily snapshots).',
      useWhen: [
        'A time-boxed iteration with an estimate of remaining work per day.',
        'You need to show scope changes next to progress, so a flat line is explained.',
      ],
      avoidWhen: [
        'Open-ended work without a fixed end: use LineChart of throughput or a cumulative flow AreaChart.',
        'Comparing several sprints: use BarChart of completed work per sprint.',
      ],
    },
    designPrinciples: {
      items: [
        'The ideal line runs from the starting scope to zero on the last day; the actual line stops at the last day with data.',
        'Scope is a step line, because scope changes at a moment, and each change is labelled +N or −N where it happens.',
        'The headline is the gap to ideal, in words: "10.67 behind ideal" or "2 ahead of ideal".',
        'Days are evenly spaced (sprint days), so weekends you leave out of `data` do not create flat stretches.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Renders ChartFrame (ref to its `<figure>`), with ChartLegend above the plot; ChartFrame props except `children`, `legend` and `table` pass through.',
        'Pass one `BurndownPoint` per sprint day, including future days without `remaining`, so the x axis covers the whole sprint and the ideal reaches zero on the last day.',
        'The units are yours: format them with `formatValue` (for example points or issues).',
        '`data-trend` (`ahead`, `behind`, `on-track`) is set on the figure and the plot for the latest day with data.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        "Migrating from Rook Stride's hand-drawn `Burndown`: its `points` become `data` (`day` → `date`, `remainingPoints` → `remaining`, `scopePoints` → `scope`); the points / issues switch stays in the product and swaps the fields it passes.",
      ],
    },
    variants: {
      items: [
        {
          prop: 'showScope',
          values: ['true', 'false'],
          description:
            'The scope line and its change labels; on by default when any day has a `scope`.',
        },
        {
          prop: 'todayIndex',
          values: ['number', 'null'],
          description:
            'Where the today marker sits; by default the last day with data when later days exist.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Ahead / behind / on track',
          attribute: 'data-trend',
          description: 'Compares the latest remaining value with the ideal on that day.',
        },
        {
          name: 'Not started',
          description:
            'No day has `remaining` yet: the summary says "Not started" and only the ideal (and scope) draw.',
        },
        {
          name: 'Inspecting',
          description:
            'Crosshair, a dot on the actual line and a tooltip with remaining, ideal, scope and the scope change that day.',
        },
        {
          name: 'Line hidden',
          attribute: 'aria-pressed="false" on the legend toggle',
          description: 'Ideal, scope or remaining can be hidden; the last visible line stays.',
        },
        {
          name: 'Empty, loading, error',
          attribute: 'data-status',
          description: 'Handled by ChartFrame.',
        },
      ],
    },
    interaction: {
      items: [
        'Hovering or tapping inspects the nearest day.',
        'The legend toggles the remaining, ideal and scope lines (`hiddenSeries` / `defaultHiddenSeries`).',
        '`onDaySelect` runs on click, Enter or Space for drill-in, such as the issues closed that day.',
        '`activeIndex` + `onActiveIndexChange` share the inspected day with other charts.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Tab',
          action: 'Moves to the legend toolbar, then to the plot, which inspects the first day.',
        },
        { keys: 'ArrowRight / ArrowDown', action: 'Next day.' },
        { keys: 'ArrowLeft / ArrowUp', action: 'Previous day.' },
        { keys: 'Home / End', action: 'First or last day.' },
        { keys: 'Escape', action: 'Hides the tooltip.' },
        { keys: 'Enter / Space', action: 'Calls `onDaySelect`.' },
      ],
    },
    accessibility: {
      role: 'figure (ChartFrame); the plot is a group with aria-roledescription "chart"',
      name: 'The figure by `title`; the plot by a generated status ("Mar 10: 24 remaining, ideal 13.33, 10.67 behind ideal. Scope 43, 5 added and 2 removed since the start") or `summary`.',
      items: [
        'Keyboard inspection announces each day, including scope changes, through a polite live region.',
        'The SVG and tooltip are hidden from assistive technology; the data table lists every day with remaining, ideal, scope and scope change.',
        'Lines differ by more than colour: the ideal is dashed, scope is a thinner step line, and the legend names each.',
        'Every word is translatable through `labels`, including the status sentences.',
      ],
    },
    responsive: {
      items: [
        'Measures its container; day labels thin out down to 320px.',
        'Renders at a 640px fallback width on the server.',
        'The legend wraps above the plot; its toggles are 44px tall on coarse pointers.',
      ],
    },
    theme: {
      items: [
        'Remaining uses data slot 1, scope slot 2, the ideal the neutral slot; the today marker and labels use border and text tokens.',
        'Forced colours: today marker, crosshair and labels use system colours.',
        'Reduced motion: only the shared tooltip fade, which becomes instant.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Sprint in progress with scope changes',
          stack: true,
          render: () => (
            <BurndownChart
              title={fixture.data.title}
              description={fixture.data.description}
              data={SPRINT}
              formatValue={(v) => `${Math.round(v * 10) / 10} ${fixture.data.unit}`}
              formatTick={(v) => String(v)}
              locale={fixture.data.locale}
              xFormat={UTC_DAY}
            />
          ),
        },
        {
          title: 'Finished sprint without scope',
          stack: true,
          render: () => (
            <BurndownChart
              title="Sprint 13"
              data={DONE_SPRINT}
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
        'Include future days without `remaining`, so the ideal line spans the sprint.',
        'Pass daily `scope` whenever it can change, so stalls are explained.',
        'Use `onDaySelect` to show what was closed or added on a day.',
      ],
      dont: [
        'Reset the ideal line when scope changes; the scope line tells that story.',
        'Interpolate missing days; leave `remaining` out instead.',
        'Use it for work with no fixed end date.',
      ],
    },
    related: {
      items: [
        { id: 'line-chart', reason: 'General trends with reference lines.' },
        { id: 'area-chart', reason: 'Stacked work by state over time (cumulative flow).' },
        { id: 'chart-legend', reason: 'The legend BurndownChart renders.' },
        { id: 'chart-frame', reason: 'The frame BurndownChart renders.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['chart-frame', 'chart-legend'],
      notes: [
        'Hand-written SVG on the shared chart internals (docs/adr/0010-charts-rendering-and-colour.md).',
      ],
    },
    testing: {
      unit: [
        'Remaining, dashed ideal, scope with +5 / −2 labels, today marker, behind summary and `data-trend`; no axe violations.',
        'Data table with scope change column and future days.',
        'Keyboard inspection with scope change in the live region and tooltip; End and Escape.',
        'Legend hides lines and keeps the last one.',
        'Ahead of ideal, no scope, no today marker.',
        '`idealStart`, custom formatting and translated labels.',
        '`onDaySelect`, not started and the empty state.',
        'Renders on the server.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
