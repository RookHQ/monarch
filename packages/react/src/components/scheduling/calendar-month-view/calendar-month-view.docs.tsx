import { useState } from 'react';
import { Button } from '@rookcom/monarch-react';
import { CalendarMonthView, type CalendarEvent } from '@rookcom/monarch-react/scheduling';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const at = (day: string, time: string) => `${day}T${time}:00Z`;

const EVENTS: CalendarEvent[] = [
  { id: 'm1', title: 'Release day', start: '2026-11-04', allDay: true, color: '#16a34a' },
  {
    id: 'm2',
    title: 'Stand-up',
    start: at('2026-11-04', '09:00'),
    end: at('2026-11-04', '09:15'),
    color: '#0f766e',
  },
  {
    id: 'm3',
    title: 'Design review',
    start: at('2026-11-04', '11:00'),
    end: at('2026-11-04', '12:00'),
    color: '#7c3aed',
  },
  {
    id: 'm4',
    title: 'Lunch & learn',
    start: at('2026-11-04', '12:30'),
    end: at('2026-11-04', '13:30'),
    color: '#2563eb',
  },
  {
    id: 'm5',
    title: 'Retrospective',
    start: at('2026-11-04', '16:00'),
    end: at('2026-11-04', '17:00'),
    status: 'cancelled',
    color: '#c2410c',
  },
  {
    id: 'm6',
    title: 'Interview',
    start: at('2026-11-09', '10:00'),
    end: at('2026-11-09', '11:00'),
    color: '#0891b2',
  },
  {
    id: 'm7',
    title: 'Vendor call',
    start: at('2026-11-12', '14:00'),
    end: at('2026-11-12', '14:30'),
    status: 'tentative',
    color: '#2563eb',
  },
  {
    id: 'm8',
    title: 'Board meeting',
    start: at('2026-11-18', '15:00'),
    end: at('2026-11-18', '17:00'),
    color: '#db2777',
  },
  {
    id: 'm9',
    title: 'Conference',
    start: '2026-11-24',
    end: '2026-11-26',
    allDay: true,
    color: '#ca8a04',
  },
  {
    id: 'm10',
    title: 'Quarter close',
    start: at('2026-11-30', '17:00'),
    end: at('2026-11-30', '18:00'),
    color: '#7c3aed',
  },
];

const base = { timeZone: 'UTC', locale: 'en-GB', today: '2026-11-04', weekStartsOn: 1 } as const;

function NavigableMonth() {
  const [month, setMonth] = useState('2026-11-01');
  const label = new Intl.DateTimeFormat('en-GB', {
    month: 'long',
    year: 'numeric',
    timeZone: 'UTC',
  }).format(new Date(`${month.slice(0, 7)}-01T12:00:00Z`));
  const shift = (n: number) => {
    const [y, m] = month.split('-').map(Number) as [number, number];
    const d = new Date(Date.UTC(y, m - 1 + n, 1));
    setMonth(d.toISOString().slice(0, 10));
  };
  return (
    <div style={{ display: 'grid', gap: 12, width: '100%' }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
        <Button size="sm" onClick={() => shift(-1)}>
          Previous
        </Button>
        <Button size="sm" onClick={() => shift(1)}>
          Next
        </Button>
        <strong aria-live="polite">{label}</strong>
      </div>
      <CalendarMonthView
        {...base}
        month={month}
        events={EVENTS}
        aria-label={label}
        onNavigate={(day) => setMonth(day)}
      />
    </div>
  );
}

export default {
  id: 'calendar-month-view',
  title: 'CalendarMonthView',
  summary: 'A month grid of days with events, "+N more" popovers and arrow-key movement.',
  sections: {
    purpose: {
      summary:
        'Shows a month at a glance: which days are busy, what is on them, and a way into each day and event. Needed by: calendar, people, stride.',
      useWhen: [
        'The month view of a calendar (bookings, interviews, releases, shifts).',
        'Planning views where users pick or open a day (attendance, due dates on a board).',
      ],
      avoidWhen: [
        'The user needs times and overlaps: use TimeGrid.',
        'Picking a single date in a form: use DatePicker or MiniMonth.',
        'A long list of upcoming items: use AgendaList.',
      ],
    },
    designPrinciples: {
      items: [
        'A real grid (`role="grid"`): weekday column headers, one row per week, arrow keys between days.',
        'Month navigation belongs to the product: it passes `month` and changes it from its own toolbar or from `onNavigate`.',
        'Busy days stay calm: at most `maxEvents` lines per cell, the rest behind "+N more".',
        'Today, outside-month days and hatched days are distinguished by shape and text as well as colour.',
        'Phones get the same grid with dots instead of chips, not a different component.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Renders a `<div>` (ref) holding the `role="grid"` element. Days are ISO dates (`"2026-11-04"`); events are `CalendarEvent` objects read in `timeZone`.',
        'The focus stop can be controlled (`focusedDay`, `onFocusedDayChange`) or uncontrolled (`defaultFocusedDay`); it defaults to today when in the month, else the 1st.',
        'The overflow list uses Monarch Popover (Radix), labelled by the full date.',
        'Pass `timeZone`, `locale`, `weekStartsOn` and `today` for identical server and client output; without `today` the clock is read after mounting.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [
        {
          prop: 'weekStartsOn',
          values: ['0', '…', '6'],
          description:
            'First column, 0 = Sunday … 6 = Saturday (the convention MiniMonth and the date pickers use); defaults to the locale’s week start (Intl week info), else Monday.',
        },
        {
          prop: 'fixedWeeks',
          values: ['true', 'false'],
          description:
            '`true` (default) always shows six weeks so the height is stable; `false` shows only the weeks the month needs.',
        },
        {
          prop: 'maxEvents',
          values: ['number'],
          description: 'Lines per cell including the "+N more" button. Default 3.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Today',
          attribute: 'data-today, aria-current="date"',
          description: 'Accent-filled date and ", today" in the day’s name.',
        },
        {
          name: 'Outside month',
          attribute: 'data-outside',
          description: 'Subtle background and muted date.',
        },
        {
          name: 'Hatched',
          attribute: 'data-hatched',
          description: 'From `dayState`: time off or closures, with an optional badge in the name.',
        },
        {
          name: 'Focus stop',
          attribute: 'data-focused',
          description: 'The one day in the tab order; its events and "+N more" are tabbable.',
        },
        {
          name: 'Overflow open',
          attribute: 'aria-expanded="true"',
          description: 'The day’s full list in a popover.',
        },
      ],
    },
    interaction: {
      items: [
        'Activating a date calls `onDayClick(day)`; activating an event calls `onEventClick(event)` or follows its `href`.',
        '"+N more" opens a popover with all of the day’s events; Escape or an outside click closes it and focus returns to the button.',
        'Multi-day events appear in each day they touch; all-day events come first.',
        '`renderEvent` wraps each chip (for example in the product’s event popover).',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Tab',
          action:
            'Moves to the focused day’s date, then its events and "+N more", then out of the grid.',
        },
        {
          keys: 'ArrowLeft / ArrowRight',
          action: 'Previous or next day (mirrored in right-to-left).',
        },
        { keys: 'ArrowUp / ArrowDown', action: 'Same weekday in the previous or next week.' },
        { keys: 'Home / End', action: 'First or last day of the week.' },
        {
          keys: 'Page Up / Page Down',
          action: 'Same day in the previous or next month (Shift: year), through `onNavigate`.',
        },
        { keys: 'Enter / Space', action: 'Activates the date (`onDayClick`).' },
      ],
      notes: [
        'Moving into another month calls `onNavigate(day)`; the product changes `month` and focus stays on that day. Without `onNavigate`, focus moves only within the shown weeks.',
      ],
    },
    accessibility: {
      role: 'grid, row, columnheader, gridcell',
      name: 'The grid is named by `aria-label` (default: the month and year). Each date is named "Wednesday, 4 November 2026, 5 events, today".',
      items: [
        'Column headers carry full weekday names for assistive technology and short ones on screen.',
        'Roving tabindex: one day is in the tab order, so a month is not 42 tab stops.',
        'The overflow popover is a dialog named by the full date.',
        'Verified with axe in jsdom and in Chromium, Firefox and WebKit on /components/calendar-month-view.',
      ],
    },
    responsive: {
      items: [
        'A container query switches cells below 36rem to a dot summary (up to three dots and "+N"); the summary button opens the day’s list.',
        'The grid always fits its container (seven `minmax(0, 1fr)` columns); there is no horizontal scrolling at 320px.',
        'Cell height comes from `--mn-calendar-month-view-cell` (7rem) and `--mn-calendar-month-view-cell-narrow` (3.5rem).',
      ],
    },
    theme: {
      items: [
        'Surfaces, borders and text are Monarch tokens; light, dark and more-contrast themes apply automatically.',
        'Today uses `--mn-color-accent-solid` with `--mn-color-accent-on-solid`, so it follows the product accent.',
        'Event colours only tint dots and chips (see EventChip).',
      ],
    },
    examples: {
      items: [
        {
          title: 'Month with events',
          stack: true,
          render: () => (
            <div style={{ width: '100%' }}>
              <CalendarMonthView
                {...base}
                month="2026-11-01"
                events={EVENTS}
                dayState={(d) =>
                  d === '2026-11-20' ? { hatched: true, badge: 'Holiday' } : undefined
                }
              />
            </div>
          ),
          code: `<CalendarMonthView
  month="2026-11-01"
  events={events}
  timeZone={zone}
  locale={locale}
  today={todayInZone}
  onDayClick={(day) => openDay(day)}
  onEventClick={(e) => openEvent(e.id)}
  dayState={(day) => (holidays.has(day) ? { hatched: true, badge: 'Holiday' } : undefined)}
/>`,
        },
        {
          title: 'Product-owned navigation',
          stack: true,
          render: () => <NavigableMonth />,
          code: `const [month, setMonth] = useState('2026-11-01');
<Toolbar>…Previous / Next / Today…</Toolbar>
<CalendarMonthView month={month} events={events} onNavigate={setMonth} />`,
        },
        {
          title: 'Narrow (dot summary)',
          stack: true,
          render: () => (
            <div style={{ width: '100%', maxWidth: 340 }}>
              <CalendarMonthView {...base} month="2026-11-01" events={EVENTS} fixedWeeks={false} />
            </div>
          ),
          code: `<CalendarMonthView month={month} events={events} /> // in a container under 36rem`,
        },
      ],
    },
    doDont: {
      do: [
        'Keep `month` in your router or state and change it from `onNavigate` so keyboard users can leave the month.',
        'Pass the user’s week start and time zone from their settings.',
        'Use `dayState` for time off and holidays rather than fake all-day events.',
      ],
      dont: [
        'Do not raise `maxEvents` to fit everything; the popover is the overflow.',
        'Do not put controls other than events inside cells; open the day for detailed actions.',
      ],
    },
    related: {
      items: [
        { id: 'event-chip', reason: 'The chips inside each day.' },
        { id: 'mini-month', reason: 'A small month for picking a date or navigating.' },
        { id: 'time-grid', reason: 'Day and week views with times.' },
        { id: 'agenda-list', reason: 'A list of the same events by day.' },
        { id: 'popover', reason: 'Hosts the "+N more" list.' },
      ],
    },
    dependencies: { packages: ['react'], components: ['event-chip', 'popover'] },
    testing: {
      unit: [
        'Grid semantics: 7 rows, column headers with full names, 42 cells, outside days, today with `aria-current`.',
        'Week start and only-needed weeks.',
        'All-day first, capped cells, "+N more" popover with every event, event callback, Escape returns focus.',
        'Day click callback.',
        'Arrow keys, Home/End, Enter; one tab stop; focus stops at the edge without `onNavigate`.',
        'Focused day’s events are tabbable, others are not.',
        'Page Up/Down call `onNavigate` and focus follows the new month.',
        '`dayState` hatching and badge; right-to-left arrows.',
        '`today={null}`, clock fallback; no axe violations; renders on the server without the clock.',
      ],
      axe: true,
      ssr: true,
      browser: [
        'Visual baselines: /__gallery/calendar-month-view',
        'Open overflow popover: overlays/calendar-month-view-more-light',
        'Cross-browser axe: /components/calendar-month-view',
      ],
    },
  },
} satisfies ComponentDocs;
