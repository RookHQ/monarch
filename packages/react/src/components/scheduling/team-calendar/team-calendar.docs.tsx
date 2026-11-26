import { useState } from 'react';
import {
  CalendarToolbar,
  TeamCalendar,
  type TeamCalendarEntry,
  type TeamCalendarMember,
} from '@rookcom/monarch-react/scheduling';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const MEMBERS: TeamCalendarMember[] = [
  { id: 'a', name: 'Alex Kim', description: 'Design' },
  { id: 'b', name: 'Sam Lee', description: 'Engineering' },
  { id: 'c', name: 'Priya Nair', description: 'Engineering' },
  { id: 'd', name: 'Jordan Park', description: 'Support' },
];

const ENTRIES: TeamCalendarEntry[] = [
  {
    id: '1',
    memberId: 'a',
    start: '2026-11-09',
    end: '2026-11-13',
    label: 'Annual leave',
    color: '#2f7d6d',
  },
  {
    id: '2',
    memberId: 'b',
    start: '2026-11-04',
    label: 'Sick leave',
    color: '#b54a3a',
    portion: 'first-half',
  },
  {
    id: '3',
    memberId: 'c',
    start: '2026-11-18',
    end: '2026-11-20',
    label: 'Annual leave',
    color: '#2f7d6d',
    pending: true,
  },
  { id: '4', memberId: 'd', start: '2026-11-25', end: '2026-11-26', label: 'Away' },
];

const MONTHS = ['2026-10-01', '2026-11-01', '2026-12-01'];

function Demo() {
  const [i, setI] = useState(1);
  const [picked, setPicked] = useState('');
  const month = MONTHS[i] ?? '2026-11-01';
  return (
    <div style={{ display: 'grid', gap: 12, width: '100%' }}>
      <CalendarToolbar
        title={new Intl.DateTimeFormat('en-GB', {
          month: 'long',
          year: 'numeric',
          timeZone: 'UTC',
        }).format(new Date(`${month}T00:00:00Z`))}
        view="month"
        onToday={() => setI(1)}
        onPrevious={() => setI((n) => Math.max(0, n - 1))}
        onNext={() => setI((n) => Math.min(MONTHS.length - 1, n + 1))}
      />
      <TeamCalendar
        members={MEMBERS}
        entries={ENTRIES}
        month={month}
        today="2026-11-04"
        locale="en-GB"
        holidays={[{ date: '2026-11-11', name: 'Company holiday' }]}
        onEntryClick={(e: TeamCalendarEntry, m: TeamCalendarMember, d: string) =>
          setPicked(`${m.name}: ${e.label} on ${d}`)
        }
        onCellClick={(m: TeamCalendarMember, d: string) =>
          setPicked(`Add time away for ${m.name} on ${d}`)
        }
      />
      <p style={{ margin: 0, fontSize: 13 }} aria-live="polite">
        {picked}
      </p>
    </div>
  );
}

export default {
  id: 'team-calendar',
  title: 'TeamCalendar',
  summary: 'Who is away when: people by days, with leave bars, half days, pending and holidays.',
  sections: {
    purpose: {
      summary:
        'Shows a team’s time away across a month (or any range) so people can plan cover. Needed by: People (leave team calendar); also fits on-call and out-of-office views in Tickets and Calendar.',
      useWhen: [
        'Seeing who is away on which days across a team.',
        'Picking a person and day to add or review an absence.',
      ],
      avoidWhen: [
        'Hour-level schedules: use TimeGrid.',
        'Assigning shifts: use ShiftRoster.',
        'One person’s events: use CalendarMonthView.',
      ],
    },
    designPrinciples: {
      items: [
        'Data in, clicks out: members, entries and holidays come from the product; navigation is a CalendarToolbar above.',
        'Privacy is the product’s choice: pass a neutral label (“Away”) for private types; reasons are never shown.',
        'Meaning is never colour alone: each bar has hidden text with its label, half and pending state.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Renders a `<div>` (ref) around a `<table role="grid">`.',
        '`month` shows a calendar month; `from` + `to` show any range (a week, a sprint).',
        'Entries are clipped to the range; `portions` sets half days per date.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Migrating from the app copy (People `leave/TeamCalendar`): the fetch by `wsId`/`departmentId` stays in the product; `employees` → `members` (`photoUrl` → `avatarUrl`); each entry’s `days[]` → `start`/`end` plus `portions` (`FULL`/`FIRST_HALF`/`SECOND_HALF` → `full`/`first-half`/`second-half`); `status === "PENDING"` → `pending`; holidays → `holidays`; `onOpenRequest(id)` → `onEntryClick(entry)`; month buttons → a CalendarToolbar; people paging stays in the product.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'month | from+to',
          values: ['month', 'range'],
          description: 'A calendar month or a custom range.',
        },
        {
          prop: 'showLegend',
          values: ['true', 'false'],
          description: 'The explanation line under the grid.',
        },
      ],
    },
    states: {
      items: [
        { name: 'Weekend / holiday', attribute: 'data-shaded', description: 'Muted column.' },
        { name: 'Today', attribute: 'data-today', description: 'Accent day number.' },
        {
          name: 'Half day',
          attribute: 'data-portion',
          description: 'Half-width bar at the start or end of the cell.',
        },
        {
          name: 'Pending',
          attribute: 'data-pending',
          description: 'Faded, outlined bar; “pending” in the hidden text.',
        },
        {
          name: 'Loading / error / empty',
          attribute: 'aria-busy',
          description: 'Placeholder, danger alert or EmptyState instead of the grid.',
        },
      ],
    },
    interaction: {
      items: [
        'Clicking or pressing Enter on a day with an entry calls `onEntryClick`; on an empty day, `onCellClick`.',
        'The grid scrolls sideways inside its frame; the people column stays put.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Enters the grid at the last focused day (one tab stop).' },
        { keys: 'Arrow keys', action: 'Move one day or one person.' },
        { keys: 'Home / End', action: 'First or last day of the row.' },
        { keys: 'Ctrl+Home / Ctrl+End', action: 'First or last cell of the grid.' },
        { keys: 'Page Up / Page Down', action: 'Back or forward a week.' },
        { keys: 'Enter / Space', action: 'Activate the day.' },
      ],
    },
    accessibility: {
      role: 'grid (table)',
      name: '“Team calendar, November 2026” from a hidden caption.',
      items: [
        'Day headers carry the full date in `abbr`; holidays add hidden “Holiday: …” text.',
        'People are row headers; avatars are decorative beside the visible name.',
        'Strings are in `messages`; dates come from Intl in `locale`.',
      ],
    },
    responsive: {
      items: [
        'At 390px the grid scrolls inside its frame with the people column sticky; the page never scrolls sideways.',
      ],
    },
    theme: {
      items: [
        'Bars use the entry colour or the accent; shading `--mn-color-bg-muted`.',
        'Forced colours: bars in CanvasText, pending as outlines.',
      ],
    },
    examples: {
      items: [
        { title: 'Month with a toolbar', stack: true, render: () => <Demo /> },
        {
          title: 'One week, loading',
          stack: true,
          render: () => (
            <TeamCalendar
              members={MEMBERS}
              entries={[]}
              from="2026-11-02"
              to="2026-11-08"
              loading
            />
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Show a neutral label for private leave types.',
        'Page long teams in the product and keep the grid to one page of people.',
      ],
      dont: ['Do not rely on colour to tell leave types apart; the label is the meaning.'],
    },
    related: {
      items: [
        { id: 'calendar-toolbar', reason: 'Month navigation above the grid.' },
        { id: 'shift-roster', reason: 'Assigning shifts in the same people-by-days layout.' },
        { id: 'time-off-panel', reason: 'Listing and adding time off.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['avatar', 'empty-state', 'alert', 'skeleton'],
    },
    testing: {
      unit: [
        'Named grid, headers, entries with hidden text, clipping, holidays and legend; axe clean.',
        'Weekend, holiday and today markers.',
        'One tab stop; arrows, Home/End, Ctrl+Home, Page Up; Enter and Space activate.',
        'Click activates; custom range and per-day portions.',
        'Loading, error and empty; renders on the server.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
