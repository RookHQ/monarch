import { useState } from 'react';
import {
  ShiftRoster,
  type ShiftRosterMember,
  type ShiftRosterShift,
  type ShiftRosterValue,
} from '@rookcom/monarch-react/scheduling';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const SHIFTS: ShiftRosterShift[] = [
  { id: 'm', name: 'Morning', shortName: 'Morning', start: 360, end: 840, color: '#2f7d6d' },
  { id: 'e', name: 'Evening', start: 840, end: 1320, color: '#7a5bb5' },
  { id: 'n', name: 'Night', start: 1320, end: 360, color: '#3b5b8f' },
];

const MEMBERS: ShiftRosterMember[] = [
  { id: 'a', name: 'Alex Kim', description: 'Support' },
  { id: 'b', name: 'Sam Lee', description: 'Support' },
  { id: 'c', name: 'Priya Nair', description: 'Operations' },
];

const START: ShiftRosterValue = {
  a: {
    '2026-11-02': 'm',
    '2026-11-03': 'm',
    '2026-11-04': 'm',
    '2026-11-07': null,
    '2026-11-08': null,
  },
  b: { '2026-11-02': 'e', '2026-11-03': 'e', '2026-11-05': 'n', '2026-11-06': 'n' },
  c: { '2026-11-04': 'n', '2026-11-05': null },
};

function Demo() {
  const [value, setValue] = useState<ShiftRosterValue>(START);
  const [last, setLast] = useState('');
  return (
    <div style={{ display: 'grid', gap: 8, width: '100%' }}>
      <ShiftRoster
        members={MEMBERS}
        shifts={SHIFTS}
        from="2026-11-02"
        today="2026-11-04"
        locale="en-GB"
        value={value}
        onValueChange={(next: ShiftRosterValue, change: { memberId: string; day: string }) => {
          setValue(next);
          setLast(`Changed ${change.memberId} on ${change.day}`);
        }}
      />
      <p style={{ margin: 0, fontSize: 13 }} aria-live="polite">
        {last}
      </p>
    </div>
  );
}

export default {
  id: 'shift-roster',
  title: 'ShiftRoster',
  summary: 'Assign shifts to people by day, with hours per person and a working count per day.',
  sections: {
    purpose: {
      summary:
        'A rota: who works which shift on which day, edited cell by cell. Needed by: People (attendance roster and shifts), Tickets (agent shifts and on-call cover).',
      useWhen: [
        'Planning shifts for a team over one or two weeks.',
        'Showing a published rota read-only.',
      ],
      avoidWhen: [
        'Leave and absences: use TeamCalendar.',
        'Hour-by-hour scheduling: use TimeGrid.',
        'The same hours every week: use WeeklyHoursEditor.',
      ],
    },
    designPrinciples: {
      items: [
        'Data in, changes out: `onValueChange(value, change)` gives the product the one change to save.',
        'The value is plain data: member id → day → shift id, `null` for a day off, missing for not assigned.',
        'Every cell is named in full (“Alex Kim, Monday 2 November: Morning 06:00–14:00”), so colour is never the only signal.',
        'Bulk tools (assign a shift to many people, rotations) stay in the product and write the same value.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Renders a `<div>` (ref) around a `<table role="grid">`; each cell is a DropdownMenu trigger.',
        'Shifts whose `end` is not after `start` end the next day (marked “overnight”).',
        '`from` + `days` (default 7) set the range; put a CalendarToolbar or your own controls above it.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Migrating from the app copy (People `attendance/manage/RosterPanel`): the roster query stays in the product; `rows[].employee` → `members` (`photoUrl` → `avatarUrl`, department or code → `description`); `rows[].days[]` → `value[memberId][date]` (`working: false` → `null`); shift definitions from `ShiftsSection` (`startTime`/`endTime` "HH:mm") → `shifts` in minutes; the person button → `onMemberClick`; period buttons and the 1/2-week switch → `from` and `days`; the assign and rotation dialogs remain product flows.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'readOnly',
          values: ['true', 'false'],
          description: 'Cells are focusable text instead of menus.',
        },
        {
          prop: 'showTotals',
          values: ['true', 'false'],
          description: 'Hours column and working-count row.',
        },
        { prop: 'days', values: ['7', '14', 'any'], description: 'Number of days shown.' },
      ],
    },
    states: {
      items: [
        {
          name: 'Shift',
          attribute: 'data-state="shift"',
          description: 'Accent tint, coloured edge from the shift.',
        },
        { name: 'Off', attribute: 'data-state="off"', description: 'Muted “Off”.' },
        { name: 'Not assigned', attribute: 'data-state="empty"', description: 'Dashed outline.' },
        { name: 'Today', attribute: 'data-today', description: 'Accent header and tinted column.' },
        { name: 'Read-only', attribute: 'data-readonly', description: 'No menus.' },
        {
          name: 'Loading / error / empty',
          attribute: 'aria-busy',
          description: 'Placeholder, alert or EmptyState.',
        },
      ],
    },
    interaction: {
      items: [
        'Clicking or pressing Enter on a cell opens a menu of shifts, “Off” and “Clear”.',
        'Choosing an item updates the cell and calls `onValueChange` with the change.',
        'The grid scrolls sideways inside its frame with the people column sticky.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Enters the grid at the last focused cell (one tab stop).' },
        { keys: 'Arrow keys', action: 'Move one day or one person (they never open the menu).' },
        { keys: 'Home / End, Ctrl+Home / Ctrl+End', action: 'Row or grid start and end.' },
        { keys: 'Enter / Space', action: 'Open the cell menu.' },
        { keys: 'Delete / Backspace', action: 'Clear the cell’s assignment.' },
        { keys: 'Escape', action: 'Close the menu and return to the cell.' },
      ],
    },
    accessibility: {
      role: 'grid (table) with menu buttons',
      name: '“Shift roster” from a hidden caption; each cell button names person, day and shift.',
      items: [
        'Menus are labelled with the person and day; the current choice is a checked radio item.',
        'Avatars are decorative beside the visible name.',
        'All strings are in `messages`; times come from Intl in `locale`.',
      ],
    },
    responsive: {
      items: ['At 390px the grid scrolls inside its frame; cells stay at least 44px tall.'],
    },
    theme: {
      items: [
        'Accent tints and the shift colour for the cell edge; every theme via tokens.',
        'Forced colours: borders in CanvasText, assigned edges in Highlight.',
      ],
    },
    examples: {
      items: [
        { title: 'Editable week', stack: true, render: () => <Demo /> },
        {
          title: 'Read-only, three days',
          stack: true,
          render: () => (
            <ShiftRoster
              members={MEMBERS}
              shifts={SHIFTS}
              from="2026-11-02"
              days={3}
              defaultValue={START}
              locale="en-GB"
              readOnly
              showTotals={false}
            />
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Save each change as it happens, or batch them behind a Publish button in the product.',
        'Give shifts short names when there are many days.',
      ],
      dont: ['Do not rely on colour alone to tell shifts apart.'],
    },
    related: {
      items: [
        { id: 'team-calendar', reason: 'Absences in the same layout.' },
        { id: 'calendar-toolbar', reason: 'Week navigation above.' },
        { id: 'dropdown-menu', reason: 'The cell menu.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['avatar', 'alert', 'empty-state', 'skeleton', 'dropdown-menu'],
    },
    testing: {
      unit: [
        'Named grid, cell names with overnight, off and not assigned, hours and working counts; axe clean.',
        'Assign from the menu; change reported.',
        'Day off and clear.',
        'One tab stop; arrows do not open the menu; End, Ctrl+Home, Delete, Enter.',
        'Read-only and member clicks; axe clean.',
        'Loading, error and empty; renders on the server.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
