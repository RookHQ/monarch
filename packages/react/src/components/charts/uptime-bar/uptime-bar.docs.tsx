import { UptimeBar, UptimeLegend } from '@rookcom/monarch-react/charts';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const days = Array.from({ length: 30 }, (_, i) => {
  const date = new Date(Date.UTC(2026, 2, 1 + i));
  const label = `Mar ${i + 1}`;
  const uptime = i === 0 ? null : i === 11 ? 97.8 : i === 19 ? 99.4 : 100;
  return {
    key: date.toISOString().slice(0, 10),
    label,
    uptime,
    ...(i === 24 ? { status: 'maintenance' as const } : {}),
    ...(i === 11 ? { note: 'Elevated error rate' } : {}),
    ...(uptime !== null ? { details: [{ label: 'Checks', value: 1440 }] } : {}),
  };
});

export default {
  id: 'uptime-bar',
  title: 'UptimeBar',
  summary:
    'A row of per-day status segments for uptime and status pages, with tooltips on hover, touch and keyboard, overall uptime and a data table.',
  sections: {
    purpose: {
      summary:
        "Shows a service's recent history at a glance — one segment per day (or hour) coloured by state — with the exact uptime and details one hover, tap or arrow key away. Needed by: Pulse (status pages, system status home, status tiles).",
      useWhen: [
        'Status pages and monitoring dashboards: the last 30 or 90 days of a service.',
        'Compact rows or tiles (`size="sm"`) listing many services.',
      ],
      avoidWhen: [
        'A continuous metric such as response time: use LineChart or Sparkline.',
        'The current state alone: use StatusBadge.',
        'A timeline of incidents with descriptions: use IncidentTimeline.',
      ],
    },
    designPrinciples: {
      items: [
        'Status colours are allowed here because they carry status, not a series — and never alone: each state has a name in the tooltip, legend, table and summary, and "No data" is hollow.',
        'Segments can bring their own `status`, or one is derived from `uptime` with `thresholds` (99.9 / 99 by default), so products keep their rules.',
        'Lightweight, not a ChartFrame: many bars sit in one list. Render one `UptimeLegend` for the page instead of a legend per bar.',
        'The bar is one tab stop; inspection starts at the newest segment.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Renders a `<div>`; extra props and the ref go to it.',
        'Also exports `UptimeLegend` (the list of states), `uptimeStatus(segment, thresholds?)` and `DEFAULT_UPTIME_STATUS_LABELS`.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        "Overall uptime defaults to the mean of the segments' `uptime`; pass the product's own figure (or `null` to hide it).",
        'Migrating from the app copy: Pulse\'s `BarStrip` / `ComponentTile` `bars` map to `segments` with `{ key: day, label: fmtDay(day), status: state, uptime: uptimePct, details: [checks, avg, p95] }`; `component.name` → `label`, `StatePill` → `meta`, "{n} days ago" / "Today" → `startLabel` / `endLabel`, `uptimePct30d` → `uptime`. `SystemStatusHome`\'s `uptimeBar()` thresholds become `thresholds={{ operational: 99.9, degraded: 99 }}`, the default. The page legend row becomes `UptimeLegend`.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'size',
          values: ['md', 'sm'],
          description:
            '32px bars for status pages, 20px for tiles and rows. Exposed as `data-size`.',
        },
        {
          prop: 'status (per segment)',
          values: ['operational', 'degraded', 'partial', 'down', 'maintenance', 'nodata'],
          description: 'The segment state, exposed as `data-status`.',
        },
        {
          prop: 'showLegend',
          values: ['false', 'true'],
          description: 'A legend under this bar; prefer one `UptimeLegend` per page.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Segment state',
          attribute: 'data-status',
          description:
            'Success, warning, strong warning, danger and info fills; no data is hollow.',
        },
        {
          name: 'Inspected',
          attribute: 'data-active',
          description: 'Outlined segment, others dimmed, tooltip above.',
        },
        {
          name: 'Selectable',
          attribute: 'data-selectable',
          description: 'Set when `onSegmentSelect` is given.',
        },
        {
          name: 'Empty',
          attribute: 'data-empty',
          description: 'No segments: an empty, unfocusable bar.',
        },
      ],
    },
    interaction: {
      items: [
        'Hovering or tapping a segment shows its period, state, uptime, details and note above the bar.',
        "Click, Enter or Space calls `onSegmentSelect`, e.g. to open that day's incidents.",
        'Dragging a finger along the bar inspects each segment in turn.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Focuses the bar and inspects the newest segment.' },
        { keys: 'Left / Right (Up / Down)', action: 'Previous or next segment.' },
        { keys: 'Home / End', action: 'Oldest or newest segment.' },
        { keys: 'PageUp / PageDown', action: 'Move a tenth of the bar.' },
        { keys: 'Enter / Space', action: 'Select the inspected segment.' },
        { keys: 'Escape', action: 'Hide the tooltip.' },
      ],
    },
    accessibility: {
      role: 'role="group" with aria-roledescription "chart"',
      name: 'The `label` plus a generated summary ("30 periods: 27 operational, 1 degraded, 1 maintenance, 1 no data. Uptime 99.91%.") or `summary`.',
      items: [
        'Keyboard inspection is announced in a polite live region ("Mar 12: Down, Uptime 97.80%, Checks 1440, Elevated error rate").',
        'A data table (period, status, uptime, details) is always in the page for screen readers; turn it off with `table={false}` only when the same data is listed nearby.',
        'The segments, tooltip and axis labels are hidden from assistive technology; the summary carries them.',
        'State names and every other word are replaceable through `labels`.',
      ],
    },
    responsive: {
      items: [
        'Segments share the width (2px gaps, 1px under 480px) and never cause horizontal scrolling; 90 days fit at 390px.',
        "On coarse pointers the bar's hit area is at least 44px tall.",
        'Long labels truncate with an ellipsis; `meta` stays on the right.',
      ],
    },
    theme: {
      items: [
        'Operational `--mn-color-success-solid`, degraded `--mn-color-warning-solid`, partial outage `--mn-color-warning-strong`, down `--mn-color-danger-solid`, maintenance `--mn-color-info-solid`, no data `--mn-color-bg-muted` with a border.',
        'Forced colours: CanvasText, Mark, Highlight and LinkText fills by state, hollow GrayText for no data, a Highlight outline when inspected.',
        'The dimming transition is removed under reduced motion.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Status page row',
          stack: true,
          render: () => (
            <div style={{ display: 'grid', gap: '1.5rem', width: '100%', maxWidth: '40rem' }}>
              <UptimeBar
                label="Public API"
                description="api.example.com"
                segments={days}
                startLabel="30 days ago"
                endLabel="Today"
                locale="en-US"
              />
              <UptimeLegend />
            </div>
          ),
        },
        {
          title: 'Compact tiles',
          stack: true,
          render: () => (
            <div style={{ display: 'grid', gap: '1rem', width: '100%', maxWidth: '20rem' }}>
              <UptimeBar label="Web app" size="sm" segments={days.slice(-14)} locale="en-US" />
              <UptimeBar
                label="Webhooks"
                size="sm"
                segments={days.slice(-14).map((d, i) => ({
                  ...d,
                  uptime: i === 6 ? 98.2 : d.uptime,
                  status: i === 6 ? ('partial' as const) : undefined,
                }))}
                locale="en-US"
              />
            </div>
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Give every segment a readable `label` (the date) and the checks or response times in `details`.',
        'Render one `UptimeLegend` per page of bars.',
        "Pass the product's overall uptime when it is computed differently from a mean of days.",
      ],
      dont: [
        'Do not show "no data" days as operational: leave `uptime` null.',
        'Do not use the uptime bar for series data or non-status categories.',
      ],
    },
    related: {
      items: [
        { id: 'status-badge', reason: 'The current state next to the bar (`meta`).' },
        { id: 'incident-timeline', reason: 'The incidents behind a bad day.' },
        { id: 'sparkline', reason: 'Response time trend in the same row.' },
        { id: 'sla-indicator', reason: 'Time left against a service-level target.' },
      ],
    },
    dependencies: { packages: ['react'], components: [] },
    testing: {
      unit: [
        'uptimeStatus thresholds and explicit states.',
        'Segment states, summary, overall uptime and start/end labels.',
        'Screen-reader table with states, uptime, details and notes.',
        'Keyboard from the newest segment, Home, arrows, Enter selection, Escape; live region.',
        'Hover tooltip with details and side, click selection, mouse leave.',
        'Small size, meta, custom thresholds and words, hidden overall; legends; controlled index; empty bar; ref; SSR.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
