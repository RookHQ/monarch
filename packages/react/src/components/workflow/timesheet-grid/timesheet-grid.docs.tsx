import { useState } from 'react';
import { IconButton } from '@rookcom/monarch-react';
import {
  TimesheetGrid,
  type TimesheetDay,
  type TimesheetRow,
} from '@rookcom/monarch-react/patterns';
import type { ComponentDocs } from '../../../internal/component-docs.js';
import { TrashIcon } from '../../../internal/docs-demo.js';

const WEEK: TimesheetDay[] = [
  { date: '2026-01-12', expectedMinutes: 480 },
  { date: '2026-01-13', expectedMinutes: 480, today: true },
  { date: '2026-01-14', expectedMinutes: 480 },
  { date: '2026-01-15', expectedMinutes: 480, locked: 'Leave' },
  { date: '2026-01-16', expectedMinutes: 480 },
  { date: '2026-01-17', nonWorking: true },
  { date: '2026-01-18', nonWorking: true },
];

const START: TimesheetRow[] = [
  {
    id: 'web',
    label: 'Website refresh',
    description: 'Design · Billable',
    cells: { '2026-01-12': 240, '2026-01-13': 180 },
  },
  {
    id: 'app',
    label: 'Mobile app',
    description: 'Development · Billable',
    cells: { '2026-01-12': 180, '2026-01-14': 300 },
  },
  {
    id: 'internal',
    label: 'Internal',
    description: 'Meetings · Non-billable',
    cells: { '2026-01-13': 60 },
  },
  {
    id: 'timer',
    label: 'Timer and imported entries',
    description: 'Edit these in the day view',
    readOnly: true,
    cells: { '2026-01-14': 75 },
  },
];

function EditableDemo() {
  const [rows, setRows] = useState(START);
  return (
    <TimesheetGrid
      days={WEEK}
      rows={rows}
      locale="en-GB"
      onCellChange={(rowId, date, minutes) =>
        setRows((all) =>
          all.map((r) => (r.id === rowId ? { ...r, cells: { ...r.cells, [date]: minutes } } : r)),
        )
      }
      rowActions={(row) =>
        row.readOnly ? null : (
          <IconButton
            size="xs"
            icon={<TrashIcon />}
            label={`Clear ${String(row.label)}`}
            onClick={() =>
              setRows((all) => all.map((r) => (r.id === row.id ? { ...r, cells: {} } : r)))
            }
          />
        )
      }
      summaryRows={[
        {
          id: 'attendance',
          label: 'Attendance',
          cells: { '2026-01-12': '7h 55m', '2026-01-13': '4h 10m', '2026-01-14': '6h 30m' },
          total: '18h 35m',
        },
      ]}
    />
  );
}

export default {
  id: 'timesheet-grid',
  title: 'TimesheetGrid',
  summary:
    'A week of time entry: projects against days with fast keyboard entry, row, day and period totals, and locked days.',
  sections: {
    purpose: {
      summary:
        'Lets people log or review time per project, task or issue for each day of a period, typing durations quickly and moving like a spreadsheet, with totals and locked days. Needed by: People (WeekGrid on My timesheet and approvals), Stride (Timesheet per issue per day).',
      useWhen: [
        'People fill in a week of time against a handful of projects or tasks.',
        'A reviewer needs time per row and day with totals, in a read-only grid.',
        'Some days are closed (approved, payroll closed, on leave) and must say why.',
      ],
      avoidWhen: [
        'Time is tracked live: use RunningTimer.',
        'One duration in a form: use DurationInput.',
        'Many attributes per entry need sorting and filtering: use DataTable.',
      ],
    },
    designPrinciples: {
      items: [
        'Fast entry: type "1:30", "1.5", "90m" or "1h 30m"; Enter saves and moves down; arrows move between cells; empty or 0 clears.',
        'Every cell has a full name ("Website refresh, Mon 12 Jan") so it makes sense out of context.',
        'Closed days say why: a lock icon and reason under the heading, also read with each cell.',
        'The product owns saving and rules: `onCellChange` gets the minutes; return a promise for a saving state and rejection message.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'The ref is the root `<div>`; the table sits in a focusable, labelled scroll region.',
        'Rows are whatever your product groups by: project + activity + task (People) or issue (Stride). Build the rows and `cells` (minutes per ISO date) from your entries.',
        "Parsing and formatting use DurationInput's `parseDuration` and `formatDuration`, so the grid accepts the same text everywhere.",
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Migrating from People `WeekGrid` (`components/timesheets/WeekGrid.tsx`): `view.days` → `days` (`date`, `isWorkingDay` → `!nonWorking`, `date === view.today` → `today`, lock or blocking leave → `locked` with a reason, `expectedMinutes`; `DayMarkers` → `markers`); grouped duration rows → `rows` (`cells` keyed by `workDate`); timed and imported entries → a row with `readOnly`; `commit()` → `onCellChange(rowId, date, minutes)` (validation built in, `maxMinutesPerDay` from policy); `onOpenDay` → `onDayActivate`; the clear-row button → `rowActions`; the attendance row → `summaryRows`; `!view.permissions.canEdit` → `readOnly`. AddRow stays in the product.',
        'Migrating from Stride `Timesheet` (`components/time/Timesheet.tsx`): issues → `rows` (`label` the identifier and title, `labelText` for cell names); cells that opened the entries dialog → `onCellActivate(rowId, date)` without `onCellChange`. Week navigation and the add-time dialog stay in the product.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'readOnly / onCellChange',
          values: ['editable', 'read-only'],
          description:
            'With `onCellChange` cells are inputs; without it (or with `readOnly`) they show durations, as buttons when `onCellActivate` is given.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Today',
          attribute: 'data-today, aria-current="date"',
          description: 'The column heading uses the accent colour.',
        },
        {
          name: 'Non-working day',
          attribute: 'data-nonworking',
          description: 'The column is shaded; entry is still allowed.',
        },
        {
          name: 'Locked day',
          attribute: 'data-locked',
          description: 'No inputs; a lock and reason under the heading.',
        },
        {
          name: 'Read-only row',
          attribute: 'data-readonly on the row',
          description: 'Shaded; values cannot be typed.',
        },
        {
          name: 'Saving',
          attribute: 'aria-busy, data-busy',
          description: 'The cell waits for the `onCellChange` promise.',
        },
        {
          name: 'Invalid',
          attribute: 'aria-invalid',
          description:
            'Unreadable or too-long text, or a rejected save: danger border and a message below the grid.',
        },
        {
          name: 'Over the daily limit',
          attribute: 'data-over',
          description: 'A day total above `maxMinutesPerDay` is danger-coloured with hidden text.',
        },
      ],
    },
    interaction: {
      items: [
        'Focusing a cell selects its value, so typing replaces it.',
        'The value is saved on Enter, Tab or leaving the cell; unchanged values are not saved.',
        'Escape throws the typed text away and restores the saved value.',
        'Arrow keys move between cells (left and right only at the start or end of the text), skipping locked days and read-only rows; directions are mirrored right to left.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Tab / Shift+Tab',
          action: 'Save the cell and move to the next or previous control.',
        },
        { keys: 'Enter', action: 'Save and move to the cell below.' },
        { keys: 'ArrowUp / ArrowDown', action: 'Move to the cell above or below.' },
        {
          keys: 'ArrowLeft / ArrowRight',
          action:
            'At the edge of the text (or with it all selected), move to the previous or next day.',
        },
        { keys: 'Escape', action: 'Discard the typed text.' },
      ],
    },
    accessibility: {
      role: 'table inside a labelled region',
      name: 'The visually hidden caption, which also explains the input formats.',
      items: [
        'Row and column headers are real `th` elements; each input also has its own name, "row, day".',
        'Validation and save errors are announced in a polite status region and the cell gets `aria-invalid`.',
        'Locked days and over-limit totals are said in text, not only shown with colour or an icon.',
        'Inputs use `inputmode="decimal"` for a numeric keyboard on phones.',
      ],
    },
    responsive: {
      items: [
        'The table scrolls sideways inside its own region with the row headings pinned, so the page never scrolls sideways at 390px.',
        'On coarse pointers inputs and buttons are 44px tall.',
        'Row labels truncate; give a short `label` and put detail in `description`.',
      ],
    },
    theme: {
      items: [
        'Semantic tokens only: bg subtle for headers and footers, bg muted for non-working days, accent strong for today, danger for invalid cells and over-limit totals.',
        'Light, dark and high-contrast themes apply automatically; forced colours keep input borders and mark invalid cells.',
        'Reduced motion removes border and background transitions.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Editable week with a locked day, read-only row and attendance',
          stack: true,
          render: () => <EditableDemo />,
        },
        {
          title: 'Read-only review',
          stack: true,
          render: () => (
            <TimesheetGrid
              days={WEEK.slice(0, 5)}
              rows={START.slice(0, 3)}
              locale="en-GB"
              readOnly
            />
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Give each row a plain-text `labelText` when `label` is not a string, so cell names stay meaningful.',
        'Pass a short reason in `locked` ("Approved", "Leave").',
        'Return the save promise so failures show on the cell.',
      ],
      dont: [
        'Do not put pickers or notes inside cells; open a day view with `onDayActivate` or `onCellActivate`.',
        'Do not hide locked days; show them with their reason.',
      ],
    },
    related: {
      items: [
        { id: 'running-timer', reason: 'Tracking time live instead of typing it.' },
        { id: 'timesheet-diff', reason: 'What changed between submitted versions.' },
        { id: 'duration-input', reason: 'A single duration in a form, with the same parser.' },
        { id: 'data-table', reason: 'Sortable, filterable lists of entries.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['duration-input'],
    },
    testing: {
      unit: [
        'Headers, totals, locked days and read-only rows with accessible names; axe passes.',
        'Typing in several formats commits minutes; clearing sends 0; invalid and too-long values are refused with a message; Escape reverts.',
        'Saving and rejection from a returned promise.',
        'Arrow-key movement skipping locked cells.',
        'Read-only grid with activatable cells and days, row actions and summary rows; over-limit flag; empty message; translations; server rendering.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
