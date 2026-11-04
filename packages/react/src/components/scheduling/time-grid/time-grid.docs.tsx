import { useState } from 'react';
import { TimeGrid, type CalendarEvent } from '@rookcom/monarch-react/scheduling';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const at = (day: string, time: string) => `${day}T${time}:00Z`;
const WEEK = ['2026-11-02', '2026-11-03', '2026-11-04', '2026-11-05', '2026-11-06'];
const HOURS = [1, 2, 3, 4, 5].flatMap((day) => [
  { day, start: 540, end: 720 },
  { day, start: 780, end: 1080 },
]);

const EVENTS: CalendarEvent[] = [
  {
    id: 't1',
    title: 'Conference',
    start: '2026-11-03',
    end: '2026-11-04',
    allDay: true,
    color: '#ca8a04',
  },
  {
    id: 't2',
    title: 'Stand-up',
    start: at('2026-11-02', '09:00'),
    end: at('2026-11-02', '09:15'),
    color: '#0f766e',
  },
  {
    id: 't3',
    title: 'Design review',
    start: at('2026-11-04', '09:30'),
    end: at('2026-11-04', '11:00'),
    detail: 'Room 4',
    color: '#7c3aed',
  },
  {
    id: 't4',
    title: 'Interview',
    start: at('2026-11-04', '10:00'),
    end: at('2026-11-04', '11:00'),
    status: 'tentative',
    color: '#0891b2',
  },
  {
    id: 't5',
    title: 'Lunch & learn',
    start: at('2026-11-05', '12:00'),
    end: at('2026-11-05', '13:00'),
    color: '#2563eb',
  },
  {
    id: 't6',
    title: 'Retrospective',
    start: at('2026-11-06', '15:00'),
    end: at('2026-11-06', '16:00'),
    status: 'cancelled',
    color: '#c2410c',
  },
  {
    id: 't7',
    title: 'Planning',
    start: at('2026-11-03', '14:00'),
    end: at('2026-11-03', '15:30'),
    color: '#db2777',
  },
];

const base = {
  timeZone: 'UTC',
  locale: 'en-GB',
  now: at('2026-11-04', '10:40'),
  scrollToMinutes: 8 * 60,
} as const;

function CreateDemo() {
  const [events, setEvents] = useState<CalendarEvent[]>(EVENTS.filter((e) => e.id === 't3'));
  return (
    <div style={{ width: '100%', maxWidth: 420 }}>
      <TimeGrid
        {...base}
        aria-label="Wednesday 4 November"
        days={['2026-11-04']}
        events={events}
        workingHours={HOURS}
        onSlotClick={(slot) =>
          setEvents((list) => [
            ...list,
            {
              id: `new-${list.length}`,
              title: 'New event',
              start: slot.start,
              end: new Date(slot.start.getTime() + 30 * 60000),
            },
          ])
        }
        onEventMove={({ event, start, end }) =>
          setEvents((list) => list.map((e) => (e.id === event.id ? { ...e, start, end } : e)))
        }
        style={{ ['--mn-time-grid-max-height' as string]: '22rem' }}
      />
    </div>
  );
}

export default {
  id: 'time-grid',
  title: 'TimeGrid',
  summary:
    'Day and week view: hours, an all-day row, overlap layout, now line, working hours and slots.',
  sections: {
    purpose: {
      summary:
        'Places events on a day or week by time, so people see overlaps and free time and can create or move events. Needed by: calendar.',
      useWhen: [
        'The day and week views of a calendar.',
        'Any schedule where free time matters: interviews, bookings, shifts.',
      ],
      avoidWhen: [
        'A whole month at once: use CalendarMonthView.',
        'Picking one free slot from a list: show the slots as buttons instead.',
        'Long-running work on a timeline (weeks or months per row): a timeline or Gantt view fits better.',
      ],
    },
    designPrinciples: {
      items: [
        'Overlapping events sit side by side: each cluster of overlapping events splits the column evenly (the Rook Calendar layout).',
        'Time is the structure: one hour gutter, hour lines and faint half-hour lines, shading outside working hours.',
        'Inject `now` (and `timeZone`, `locale`) for deterministic server rendering and tests.',
        'Creating works from both pointer and keyboard: slot buttons are one tab stop with arrow-key movement.',
        'Dragging is an enhancement; the product must also offer a non-drag way to reschedule.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'The root `<div role="group">` (ref) is the scroll container for both axes; name it with `aria-label` (for example "Week of 2 November").',
        'Days are ISO dates; events are `CalendarEvent` objects placed in `timeZone` with Intl. All-day events go in the all-day row; multi-day timed events are clipped to each day.',
        '`onSlotClick` receives `{ day, minutes, start }`; `onEventMove` receives `{ event, start, end }` with the event’s duration kept.',
        '`workingHours` takes the WeeklyHoursEditor value (`{ day, start, end }[]`).',
        'Resizing events by dragging their edge is out of scope: Rook Calendar only moves events by drag. Products change durations in their event form.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [
        {
          prop: 'days',
          values: ['1 day', '2–7 days'],
          description:
            'A day view or a week (or work-week) view. Several days get a minimum column width and scroll sideways on narrow screens.',
        },
        {
          prop: 'startHour / endHour',
          values: ['0–23', '1–24'],
          description: 'The visible range of hours. Events outside it are clipped.',
        },
        {
          prop: 'hourHeight',
          values: ['number (px)'],
          description: 'Height of one hour. Default 48.',
        },
        {
          prop: 'showAllDayRow',
          values: ['true', 'false'],
          description: 'Keep the all-day row when no all-day event is visible.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Today',
          attribute: 'data-today (header, column)',
          description:
            'Accent date in the header and the now line in its column; "Today" in the header name.',
        },
        {
          name: 'Outside working hours',
          description: 'Muted shading (`.mn-time-grid__shade`), decorative.',
        },
        { name: 'Unavailable', description: 'Hatched block with its label (time off, closures).' },
        {
          name: 'Creatable',
          attribute: 'data-creatable (column)',
          description: 'Cell cursor and hover highlight on slots.',
        },
        {
          name: 'Dragging',
          attribute: 'data-dragging (root), data-ghosted (event)',
          description: 'The event fades and a dashed ghost shows the new time.',
        },
        {
          name: 'Compact event',
          attribute: 'data-density="compact"',
          description: 'Events under about 38px show start time and title on one line.',
        },
      ],
    },
    interaction: {
      items: [
        'Clicking empty time calls `onSlotClick` with the time snapped down to `snapMinutes`.',
        'Clicking an event calls `onEventClick`; `renderEvent` can wrap it in a popover trigger.',
        'With `onEventMove`, a mouse or pen drag of more than 4px moves the event (snapped, across days); the following click is swallowed. Touch keeps scrolling. Escape cancels a drag.',
        'On mount and when the first day changes, the grid scrolls to `scrollToMinutes` (default an hour before the first event, else 08:00).',
        'Without `now`, the grid reads the clock after mounting and moves the line every minute.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Tab',
          action:
            'Day headers (with `onDayClick`), all-day events, then the slot buttons (one stop) and events in time order.',
        },
        { keys: 'ArrowUp / ArrowDown', action: 'Previous or next hour slot.' },
        {
          keys: 'ArrowLeft / ArrowRight',
          action: 'Same hour on the previous or next day (mirrored in right-to-left).',
        },
        {
          keys: 'Home / End',
          action: 'First or last visible hour; with Ctrl, also the first or last day.',
        },
        {
          keys: 'Enter / Space',
          action: 'On a slot: `onSlotClick` at that hour. On an event: activates it.',
        },
        { keys: 'Escape', action: 'Cancels a drag in progress.' },
      ],
      notes: [
        'Keyboard creation uses whole hours; pointer creation snaps to `snapMinutes`. Moving an event by keyboard is left to the product’s reschedule action.',
      ],
    },
    accessibility: {
      role: 'group (root and each day column)',
      name: 'Each column is named by its full date; slots are named "Create event, Wednesday 4 November, 09:00"; events "title, time range, status".',
      items: [
        'The hour gutter, shading, now line and drag ghost are decorative (`aria-hidden`).',
        'Day headers expose the full date to assistive technology and `aria-current="date"` for today.',
        'Dragging has a non-drag alternative by contract (WCAG 2.5.7): document a reschedule action next to `onEventMove`.',
        'Verified with axe in jsdom and in Chromium, Firefox and WebKit on /components/time-grid.',
      ],
    },
    responsive: {
      items: [
        'A multi-day grid keeps columns at least 5.5rem wide and scrolls sideways inside its own box, with the hour gutter and headers sticky.',
        'A single day fills its container and never scrolls sideways; show the day view on phones.',
        'Height is capped by `--mn-time-grid-max-height` (40rem); the body scrolls inside it.',
      ],
    },
    theme: {
      items: [
        'Lines, shading and surfaces are Monarch tokens; light, dark and more-contrast themes apply automatically.',
        'The now line uses `--mn-color-danger-solid`; today’s date uses the product accent.',
        'In forced-colours mode the now line uses Highlight.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Work week',
          stack: true,
          render: () => (
            <div style={{ width: '100%' }}>
              <TimeGrid
                {...base}
                aria-label="Week of 2 November"
                days={WEEK}
                events={EVENTS}
                workingHours={HOURS}
                unavailable={[
                  {
                    id: 'off',
                    start: at('2026-11-06', '09:00'),
                    end: at('2026-11-06', '12:00'),
                    label: 'Time off',
                  },
                ]}
                onDayClick={() => {}}
                style={{ ['--mn-time-grid-max-height' as string]: '26rem' }}
              />
            </div>
          ),
          code: `<TimeGrid
  aria-label="Week of 2 November"
  days={week}
  events={events}
  timeZone={zone}
  locale={locale}
  now={now}
  workingHours={schedule.weeklyHours}
  unavailable={timeOff}
  onDayClick={(day) => showDay(day)}
  onEventClick={(e) => openEvent(e.id)}
/>`,
        },
        {
          title: 'Day view: create and drag',
          stack: true,
          render: () => <CreateDemo />,
          code: `<TimeGrid
  aria-label="Wednesday 4 November"
  days={['2026-11-04']}
  events={events}
  onSlotClick={(slot) => openCreate(slot.start)}
  onEventMove={({ event, start, end }) => confirmMove(event, start, end)}
/>`,
        },
      ],
    },
    doDont: {
      do: [
        'Name the grid with the range it shows.',
        'Pass `now`, `timeZone` and `locale` when rendering on the server.',
        'Confirm or undo drag moves in the product (a toast with Undo, or a confirm step for bookings).',
        'Offer a reschedule action in the event popover for keyboard and touch users.',
      ],
      dont: [
        'Do not compute overlap layout yourself; pass all events for the range.',
        'Do not show a seven-day week on phones; switch to the day view or AgendaList.',
      ],
    },
    related: {
      items: [
        { id: 'event-chip', reason: 'The event blocks and all-day bars.' },
        { id: 'weekly-hours-editor', reason: 'Edits the value `workingHours` takes.' },
        { id: 'calendar-month-view', reason: 'The month view.' },
        { id: 'agenda-list', reason: 'The list view, and the phone layout of a week.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['event-chip'],
      notes: ['`workingHours` reuses the WeeklyHoursEditor value type (a type-only import).'],
    },
    testing: {
      unit: [
        'Events placed by time; overlapping events split into columns; short events compact with the full range in the name.',
        'All-day row, now line at the injected time in today’s column, Today in the header name.',
        '`now={null}` hides the line; the clock is read after mount when left out.',
        'Working-hours shading and unavailable blocks.',
        'Pointer slot click snapped to the grid.',
        'Keyboard slots: one tab stop, arrows, Home/End, Ctrl+End, Enter creates in the given time zone.',
        'Event and day-header callbacks.',
        'Drag moves an event across days keeping its length, shows a ghost and swallows the click; touch, tiny moves and non-movable events are ignored.',
        'Visible hour range clips events; no axe violations; renders on the server without the clock.',
      ],
      axe: true,
      ssr: true,
      browser: [
        'Visual baselines: /__gallery/time-grid',
        'Cross-browser axe: /components/time-grid',
      ],
    },
  },
} satisfies ComponentDocs;
