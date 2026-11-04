import { Button } from '@rookcom/monarch-react';
import { AgendaList, type CalendarEvent } from '@rookcom/monarch-react/scheduling';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const at = (day: string, time: string) => `${day}T${time}:00Z`;

const EVENTS: CalendarEvent[] = [
  { id: 'e1', title: 'Team offsite', start: '2026-11-04', allDay: true, color: '#16a34a' },
  {
    id: 'e2',
    title: 'Stand-up',
    start: at('2026-11-04', '09:00'),
    end: at('2026-11-04', '09:15'),
    color: '#0f766e',
  },
  {
    id: 'e3',
    title: 'Design review',
    start: at('2026-11-04', '11:00'),
    end: at('2026-11-04', '12:00'),
    detail: 'Room 4',
    color: '#7c3aed',
  },
  {
    id: 'e4',
    title: 'Vendor call',
    start: at('2026-11-05', '14:00'),
    end: at('2026-11-05', '14:30'),
    status: 'tentative',
    detail: 'Video call',
    color: '#2563eb',
  },
  {
    id: 'e5',
    title: 'Retrospective',
    start: at('2026-11-06', '16:00'),
    end: at('2026-11-06', '17:00'),
    status: 'cancelled',
    color: '#c2410c',
  },
];

const base = { timeZone: 'UTC', locale: 'en-GB', today: '2026-11-04' } as const;

export default {
  id: 'agenda-list',
  title: 'AgendaList',
  summary: 'Events grouped by day with sticky day headings, a Today marker and an empty state.',
  sections: {
    purpose: {
      summary:
        'Lists upcoming events in time order, one section per day, for an agenda view, a dashboard card or a narrow screen where a grid does not fit. Needed by: calendar.',
      useWhen: [
        'The agenda view of a calendar, or the phone layout of a week.',
        'An "Upcoming" card listing the next bookings, interviews or deadlines.',
      ],
      avoidWhen: [
        'Overlaps and free time matter: use TimeGrid.',
        'The user scans a whole month: use CalendarMonthView.',
        'The rows are not events (a task list, a feed): use a plain list.',
      ],
    },
    designPrinciples: {
      items: [
        'Days are sections with a real heading, so the outline and screen-reader heading navigation follow the days.',
        'All-day events come first in a day, then timed events by start time.',
        'A time column on the left keeps titles aligned; below 24rem it moves above the title.',
        'Today is marked by text ("Today") and colour, never colour alone.',
        'The product supplies data and callbacks; the list only orders, groups and formats.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Renders a `<div>` (ref) containing one `<section aria-labelledby>` per day with an `h3` (see `headingLevel`) and a `<ul>` of EventChip `bar` chips.',
        'Days come from the events (`from` to the last event) or from `from` + `days`; `showEmptyDays` lists days with nothing.',
        'Times are formatted with Intl in `timeZone` and `locale`; pass both (and `today`) for identical server and client output.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [
        {
          prop: 'headingLevel',
          values: ['2', '3', '4', '5', '6'],
          description: 'Level of the day headings, to fit the page outline. Default 3.',
        },
        {
          prop: 'showEmptyDays',
          values: ['true', 'false'],
          description:
            'List every day in the range, with a zero count, instead of only days with events or notes.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Today',
          attribute: 'data-today (section)',
          description: 'Link-coloured date and a "Today" pill in the heading.',
        },
        {
          name: 'Empty',
          attribute: 'data-empty (root)',
          description: 'EmptyState "Nothing scheduled", or your `empty` node.',
        },
        {
          name: 'Note rows',
          description:
            'Hatched informational rows from `dayNotes` (time off, holidays), shown first.',
        },
        {
          name: 'Event status',
          description:
            'Each row is an EventChip, so tentative, cancelled and declined read as in every view.',
        },
      ],
    },
    interaction: {
      items: [
        'Activating a row calls `onEventClick(event)`, or follows the event’s `href`.',
        '`renderEvent(event, chip)` wraps a row, for example in a PopoverTrigger.',
        'Give the list a height or max-height to scroll inside a card; the day headings stick to the top.',
        'A multi-day event appears on each day it touches; a timed event continuing from an earlier day shows "– end".',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Tab',
          action:
            'Enters the list at today’s first event (or the first event), then leaves it: one tab stop.',
        },
        { keys: 'ArrowDown / ArrowUp', action: 'Next or previous event, across days.' },
        { keys: 'Page Down / Page Up', action: 'First event of the next or previous day.' },
        { keys: 'Home / End', action: 'First or last event.' },
        { keys: 'Enter / Space', action: 'Activates the focused event.' },
      ],
    },
    accessibility: {
      role: 'region per day (section labelled by its heading)',
      name: 'Each day section is named by its heading: "Wednesday 4 November, Today, 3 events".',
      items: [
        'Rows are EventChip buttons named "title, time, status, detail".',
        'Roving tabindex keeps a long agenda to one tab stop while every event stays reachable.',
        'Verified with axe in jsdom and in Chromium, Firefox and WebKit on /components/agenda-list.',
      ],
    },
    responsive: {
      items: [
        'Uses a container query: below 24rem the time sits above the title and the status beside it.',
        'Titles and details truncate; nothing forces horizontal scrolling at 320px.',
      ],
    },
    theme: {
      items: [
        'Headings use `--mn-color-bg-surface`, so they cover scrolled rows in every theme.',
        'The Today pill uses `--mn-color-accent-subtle` with default text, which meets contrast in every theme.',
        'Note rows are hatched with `--mn-color-bg-muted` / `--mn-color-bg-subtle`.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Agenda',
          stack: true,
          render: () => (
            <div style={{ width: '100%', maxWidth: 560 }}>
              <AgendaList
                {...base}
                events={EVENTS}
                aria-label="Agenda"
                dayNotes={(day) => (day === '2026-11-05' ? ['Time off · Priya'] : undefined)}
                style={{ maxHeight: 360 }}
              />
            </div>
          ),
          code: `<AgendaList
  events={events}
  timeZone={zone}
  locale={locale}
  today={todayInZone}
  onEventClick={(e) => openEvent(e.id)}
  dayNotes={(day) => timeOff.get(day)}
  style={{ maxHeight: 480 }}
/>`,
        },
        {
          title: 'Empty',
          stack: true,
          render: () => (
            <div style={{ width: '100%', maxWidth: 560 }}>
              <AgendaList {...base} events={[]} />
            </div>
          ),
          code: `<AgendaList events={[]} />`,
        },
        {
          title: 'Custom empty state and range',
          stack: true,
          render: () => (
            <div style={{ width: '100%', maxWidth: 560 }}>
              <AgendaList
                {...base}
                events={[]}
                from="2026-11-09"
                days={7}
                empty={
                  <div style={{ display: 'grid', gap: 8, justifyItems: 'start', padding: 16 }}>
                    <p style={{ margin: 0 }}>No bookings next week.</p>
                    <Button size="sm">Share booking link</Button>
                  </div>
                }
              />
            </div>
          ),
          code: `<AgendaList events={events} from="2026-11-09" days={7} empty={<NoBookings />} />`,
        },
      ],
    },
    doDont: {
      do: [
        'Pass `timeZone`, `locale` and `today` from the user’s settings so grouping matches the rest of the product.',
        'Give it a bounded height inside cards so the sticky headings work.',
        'Use `dayNotes` for time off and holidays instead of fake events.',
      ],
      dont: [
        'Do not sort or group the events yourself; pass them in any order.',
        'Do not nest interactive controls inside a row; open details in a popover or page.',
      ],
    },
    related: {
      items: [
        { id: 'event-chip', reason: 'Each row is a `bar` EventChip.' },
        { id: 'calendar-month-view', reason: 'The month layout of the same events.' },
        { id: 'time-grid', reason: 'The day and week layout, with overlaps and free time.' },
        { id: 'empty-state', reason: 'The default empty state.' },
      ],
    },
    dependencies: { packages: ['react'], components: ['empty-state', 'event-chip'] },
    testing: {
      unit: [
        'Groups by day with headings, counts and a Today marker; all-day events first, then by start time.',
        'Arrow keys, Page Up/Down and Home/End move between events across days; one tab stop; Enter activates.',
        'The tab stop starts at today or later.',
        'Groups in the given time zone; continuing events show their end only.',
        'Fixed ranges with `showEmptyDays`, `dayNotes`, heading level and labels.',
        'Default and custom empty states; `renderEvent` wrapping.',
        'No axe violations in jsdom; renders on the server.',
      ],
      axe: true,
      ssr: true,
      browser: [
        'Visual baselines: /__gallery/agenda-list',
        'Cross-browser axe: /components/agenda-list',
      ],
    },
  },
} satisfies ComponentDocs;
