import { useState } from 'react';
import { MiniMonth, type MiniMonthDayInfo } from '@rookcom/monarch-react/scheduling';
import type { ComponentDocs } from '../../../internal/component-docs.js';

// Examples pin `today` so the page and its visual baselines never change with the date.
const TODAY = '2026-11-04';

const EVENTS: Record<string, MiniMonthDayInfo> = {
  '2026-11-05': { dots: ['accent'], description: '1 event' },
  '2026-11-10': { dots: ['accent', 'info'], description: '2 events' },
  '2026-11-18': { dots: ['warning'], description: 'Deadline' },
  '2026-11-24': { dots: ['accent', 'success', 'danger'], description: '3 events' },
};

function SidebarCalendar() {
  const [value, setValue] = useState<string | null>('2026-11-10');
  return (
    <MiniMonth
      locale="en-GB"
      today={TODAY}
      value={value}
      onValueChange={setValue}
      getDayInfo={(d) => EVENTS[d]}
      aria-label="Jump to date"
    />
  );
}

const isWeekend = (d: string) => [0, 6].includes(new Date(`${d}T00:00:00Z`).getUTCDay());

export default {
  id: 'mini-month',
  title: 'MiniMonth',
  summary:
    'A keyboard-navigable month grid (APG date-picker grid) with min/max, disabled days, week numbers and event dots.',
  sections: {
    purpose: {
      summary:
        'Shows one month (or several side by side) as a grid of days the user can move through with the keyboard and pick from. It is the calendar inside DatePicker and DateRangePicker, and works on its own as a sidebar calendar for jumping to a date. Import from `@rookcom/monarch-react/scheduling`.',
      useWhen: [
        'A sidebar or panel calendar that jumps a view to a date and marks days with events.',
        'Building a custom picker that needs the same grid and keyboard model as DatePicker.',
        'Showing availability for booking, with unavailable days blocked.',
      ],
      avoidWhen: [
        'The user needs to type or submit a date in a form: use DatePicker.',
        'Picking a period: use DateRangePicker.',
        'Showing events with titles and times across a month: use CalendarMonthView.',
      ],
    },
    designPrinciples: {
      items: [
        'Dates are ISO `YYYY-MM-DD` strings throughout, so a day never shifts with the time zone.',
        'One Tab stop for the whole grid; arrow keys, Home/End and PageUp/PageDown move inside it, as in the WAI-ARIA APG date-picker dialog.',
        'Selection, today and unavailable days each have a non-colour cue: a filled day, a ring and bold numeral, and a line-through (out-of-bounds days are muted and announced as unavailable).',
        'Locale decides names, numerals and the first day of the week; a prop can override the week start.',
        'Products own meaning: availability, events and holidays come in through `isDateDisabled` and `getDayInfo`.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'The ref goes to the root `<div>`; other props go there too.',
        '`range` and `rangePreview` turn the grid into a range display for DateRangePicker; `onValueChange` then reports each activated day.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        '`value`/`onValueChange` (controlled) or `defaultValue`; `month`/`onMonthChange` (controlled) or `defaultMonth` for the visible month.',
        '`today` defaults to the device date and refreshes at midnight. During server rendering and hydration the UTC date is used, then the device date; pass `today` to fix it (a workspace time zone, tests).',
        'Pass `locale` when server rendering, so the server and the browser format the same names.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'numberOfMonths',
          values: ['1', '2', '…'],
          description:
            'Consecutive months side by side; focus moves across them. Navigation buttons sit on the outer edges.',
        },
        {
          prop: 'showWeekNumbers',
          values: ['true', 'false'],
          description: 'A leading column of ISO 8601 week numbers (row headers).',
        },
        {
          prop: 'showOutsideDays',
          values: ['true', 'false'],
          description:
            'Dimmed, inert days of the neighbouring months (single month only). Default true.',
        },
        {
          prop: 'fixedWeeks',
          values: ['true', 'false'],
          description: 'Always six rows, so the height never jumps. Default true.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Selected',
          attribute: 'aria-selected="true" on the cell, data-selected on the day',
          description: 'Filled with the accent colour.',
        },
        {
          name: 'Today',
          attribute: 'aria-current="date", data-today',
          description: 'A ring and a bold numeral.',
        },
        {
          name: 'Outside min/max',
          attribute: 'aria-disabled="true", data-disabled',
          description:
            'Muted. Still focusable up to the bound so keyboard users keep their place, but cannot be chosen; navigation stops at the bound’s month.',
        },
        {
          name: 'Unavailable',
          attribute: 'aria-disabled="true", data-disabled, data-unavailable',
          description:
            'Blocked by `isDateDisabled` inside the bounds: muted and struck through, so it is not shown by colour alone. Focusable, cannot be chosen.',
        },
        {
          name: 'In range / range ends',
          attribute: 'data-in-range, data-range-start, data-range-end',
          description: 'A tinted band between the ends, which are filled.',
        },
        {
          name: 'Preview',
          attribute: 'data-preview',
          description: 'A provisional range (hover or keyboard focus while picking an end).',
        },
        { name: 'Focus', description: 'Focus ring on the focused day.' },
      ],
    },
    interaction: {
      items: [
        'Clicking a day, or pressing Enter or Space on it, selects it (unless unavailable).',
        'Moving focus past the visible month changes the month; the previous and next buttons do too, and stop at `min` and `max`.',
        'The month title is a polite live region, so month changes are announced.',
        'Left and Right follow the text direction (reversed in right-to-left layouts), and the chevrons mirror.',
        'Pointer hover and keyboard focus both report the day through `onDayHover`.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Tab',
          action: 'Enters the grid at the selected day, else today, else the first available day.',
        },
        { keys: '← / →', action: 'Previous / next day.' },
        { keys: '↑ / ↓', action: 'Same day of the previous / next week.' },
        { keys: 'Home / End', action: 'First / last day of the week.' },
        {
          keys: 'Page Up / Page Down',
          action: 'Same day of the previous / next month (clamped to the month’s length).',
        },
        { keys: 'Shift + Page Up / Page Down', action: 'Same day of the previous / next year.' },
        { keys: 'Enter / Space', action: 'Select the focused day.' },
      ],
      notes: ['Focus never moves past `min` or `max`: keys clamp to the bound.'],
    },
    accessibility: {
      role: 'grid (one per month), with column headers, gridcells and a button per day',
      name: 'Each grid is labelled by its month title; each day by its full date ("Wednesday 4 November 2026") plus the `getDayInfo` description.',
      items: [
        'Follows the WAI-ARIA APG date-picker grid: roving tabindex, `aria-selected` on cells, `aria-current="date"` on today.',
        'Weekday headers carry the full name in `abbr`; week numbers are row headers named "Week 45".',
        'Event dots are decorative (`aria-hidden`); their meaning must be in `description`.',
        'Outside days are hidden from assistive technology and cannot be focused.',
        'Day cells are 36px, 44px on touch screens that have the room.',
      ],
    },
    responsive: {
      items: [
        'One month fits a 320px screen, also with week numbers (cells shrink slightly under 352px).',
        'With several months the months wrap onto new lines when the container is too narrow.',
        'Touch screens get 44px day and navigation targets from 384px up.',
      ],
    },
    theme: {
      items: [
        'Every colour is a Monarch token: accent for selection, accent-subtle for ranges, status colours for dots, so all themes and more-contrast modes apply.',
        'Forced-colours mode outlines the selection and the range, and draws dots in the text colour.',
        'Transitions are removed under `prefers-reduced-motion: reduce`.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Sidebar calendar with events',
          render: () => <SidebarCalendar />,
          code: `const [value, setValue] = useState<string | null>('2026-11-10');

<MiniMonth
  locale="en-GB"
  value={value}
  onValueChange={setValue}
  getDayInfo={(d) => events[d]} // { dots: ['accent', 'info'], description: '2 events' }
  aria-label="Jump to date"
/>`,
        },
        {
          title: 'Bookable days only, week numbers',
          render: () => (
            <MiniMonth
              locale="en-GB"
              today={TODAY}
              min={TODAY}
              max="2026-12-20"
              isDateDisabled={isWeekend}
              showWeekNumbers
              defaultValue="2026-11-12"
            />
          ),
          code: `<MiniMonth
  locale="en-GB"
  min={today}
  max="2026-12-20"
  isDateDisabled={(d) => isWeekend(d)}
  showWeekNumbers
  defaultValue="2026-11-12"
/>`,
        },
        {
          title: 'US locale (week starts on Sunday)',
          render: () => <MiniMonth locale="en-US" today={TODAY} defaultValue="2026-11-26" />,
          code: `<MiniMonth locale="en-US" defaultValue="2026-11-26" />`,
        },
      ],
    },
    doDont: {
      do: [
        'Pass `locale` (and `today` if you use a workspace time zone) so server and browser agree.',
        'Describe what dots mean in `getDayInfo().description`.',
        'Use `isDateDisabled` for unavailable days rather than hiding them.',
      ],
      dont: [
        'Do not convert ISO dates to `Date` in local time to compare or format them; use the strings.',
        'Do not put a MiniMonth in a form as the only way to enter a date: use DatePicker, which also accepts typing.',
        'Do not show more than three dots per day; summarise in the description instead.',
      ],
    },
    related: {
      items: [
        { id: 'date-picker', reason: 'A form field that types or picks one date with this grid.' },
        { id: 'date-range-picker', reason: 'Picking a period, with two months and presets.' },
        { id: 'calendar-month-view', reason: 'A full month with event titles.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: [],
      notes: [
        'Date maths and formatting come from the internal Intl-based helpers (no date library).',
      ],
    },
    testing: {
      unit: [
        'Labelled grid per month with full weekday names in `abbr`; axe clean.',
        '`aria-selected` on the selection, `aria-current="date"` on today; one roving Tab stop.',
        'Arrow keys, Home/End, PageUp/PageDown and Shift+PageUp/PageDown, clamping the day and crossing months.',
        'Click, Enter and Space select; controlled value and controlled month.',
        'min/max clamp keys and disable navigation; `isDateDisabled` days stay focusable but cannot be chosen.',
        'Week start from the locale or the prop; ISO week numbers as row headers.',
        'Event dots (max three) and their description in the accessible name.',
        'Range and preview highlighting; `onDayHover` on hover and leave.',
        'Two months with focus moving between them; RTL reverses Left and Right; focusOnMount.',
      ],
      axe: true,
      ssr: true,
      browser: [
        'Visual baselines: /__gallery/mini-month',
        'Cross-browser axe: /components/mini-month',
      ],
    },
  },
} satisfies ComponentDocs;
