import { useState } from 'react';
import {
  WeeklyHoursEditor,
  validateWeeklyHours,
  type WeeklyHoursInterval,
} from '@rookcom/monarch-react/scheduling';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const WEEKDAYS: WeeklyHoursInterval[] = [1, 2, 3, 4, 5].flatMap((day) => [
  { day, start: 540, end: 750 },
  { day, start: 810, end: 1050 },
]);

function ControlledDemo() {
  const [value, setValue] = useState<WeeklyHoursInterval[]>(WEEKDAYS);
  const invalid = Object.keys(validateWeeklyHours(value)).length > 0;
  const hours = value.reduce((sum, r) => sum + Math.max(0, r.end - r.start), 0) / 60;
  return (
    <div style={{ display: 'grid', gap: 8, width: '100%', maxWidth: 640 }}>
      <WeeklyHoursEditor
        locale="en-GB"
        value={value}
        onValueChange={setValue}
        timeZoneLabel="Times in Europe/London"
      />
      <p style={{ margin: 0, fontSize: 13 }} aria-live="polite">
        {invalid ? 'Fix the highlighted days to save.' : `${hours} hours a week`}
      </p>
    </div>
  );
}

export default {
  id: 'weekly-hours-editor',
  title: 'WeeklyHoursEditor',
  summary:
    'Edits weekly availability: days on or off, several intervals a day, copy and overlap checks.',
  sections: {
    purpose: {
      summary:
        'Lets people set the hours they (or a team, or a support desk) are available each week. Needed by: calendar, tickets.',
      useWhen: [
        'Booking availability, business hours, support or on-call hours.',
        'Any setting that is "these times on these weekdays".',
      ],
      avoidWhen: [
        'One-off dates (holidays, time off): use a date list next to it.',
        'Shift rotas that differ week to week: that is a scheduling grid, not a weekly pattern.',
      ],
    },
    designPrinciples: {
      items: [
        'The value is plain data, `{ day, start, end }[]` in minutes, a plain, serialisable shape, so it goes to the API and to TimeGrid `workingHours` unchanged.',
        'Each day is switched on or off; switching off and on again restores the day’s intervals.',
        'Times are picked from a list (`step` minutes), never typed free-hand, so there are no parse errors.',
        'Validation is shown under the day and exported as `validateWeeklyHours`; the product decides whether to block saving.',
        'The time zone is shown, not assumed: `timeZoneLabel` holds text or the product’s timezone picker.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Renders a `<fieldset>` (ref) with the legend, the zone slot and one row per weekday in `weekStartsOn` order (0 = Sunday … 6 = Saturday, as in the date pickers). Interval `day` values are ISO weekdays (1 = Monday … 7 = Sunday), Rook Calendar’s stored shape.',
        'Controlled with `value` + `onValueChange(value, errors)`, or uncontrolled with `defaultValue`. `onValueChange` emits intervals ordered by weekday, each day’s intervals in the order the user arranged them.',
        '`name` adds a hidden input holding the value as JSON, for plain form posts.',
        '`validateWeeklyHours(value)` returns `{ [weekday]: "range" | "order" | "overlap" }` for invalid days.',
        'Times use Monarch NativeSelect (the platform list works well on phones and with 97 options); a separate TimeInput/TimeSelect component is not part of this wave.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [
        {
          prop: 'readOnly',
          values: ['true', 'false'],
          description: 'Shows each day’s hours as text ("09:00 – 17:00"), without controls.',
        },
        {
          prop: 'disabled',
          values: ['true', 'false'],
          description: 'Disables the whole fieldset.',
        },
        {
          prop: 'step',
          values: ['5', '10', '15', '30', '60'],
          description: 'Minutes between time options. Default 15.',
        },
        {
          prop: 'hourCycle',
          values: ['12', '24'],
          description:
            'Force a 12- or 24-hour clock; 24-hour shows "24:00" for the end of the day, 12-hour "Midnight".',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Day on',
          attribute: 'data-on (row)',
          description: 'Switch on, intervals shown, copy button available.',
        },
        { name: 'Day off', description: '"Unavailable" in muted text.' },
        {
          name: 'Invalid',
          attribute: 'data-invalid (row), aria-invalid (selects)',
          description:
            'Danger-coloured message under the day (`role="alert"`), linked to its selects.',
        },
        { name: 'Read-only', attribute: 'data-readonly', description: 'Text only.' },
        { name: 'Disabled', description: 'Native fieldset disabling of every control.' },
      ],
    },
    interaction: {
      items: [
        'Switching a day on adds `defaultInterval` (09:00–17:00), or restores the intervals it had when switched off.',
        '"+" adds an interval an hour after the last one (an hour long); "×" removes one. At most `maxIntervals` per day.',
        '"Copy to all" copies that day’s intervals to every day of the week.',
        'Errors appear as soon as a day is invalid: times outside the day, an interval ending before it starts, or overlapping intervals.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Tab / Shift+Tab',
          action:
            'Through each day’s switch, selects, remove/add buttons and copy button, in reading order.',
        },
        { keys: 'Space', action: 'Toggles a day’s switch.' },
        { keys: 'Arrow keys', action: 'Change a time in a focused select (platform behaviour).' },
        { keys: 'Enter / Space', action: 'Activate the add, remove and copy buttons.' },
      ],
    },
    accessibility: {
      role: 'group (fieldset with legend); one group per day',
      name: 'The fieldset is named by `legend`; each switch and day group by the weekday; selects "Monday interval 1 start".',
      items: [
        'Buttons have full names: "Add Monday interval", "Remove Monday interval 2", "Copy to all days from Monday" (the visible "Copy to all" starts the name).',
        'Errors use `role="alert"` and are referenced by the day’s selects through `aria-describedby`.',
        'Day and time names come from Intl in `locale`; all fixed strings are in `labels`.',
        'Verified with axe in jsdom and in Chromium, Firefox and WebKit on /components/weekly-hours-editor.',
      ],
    },
    responsive: {
      items: [
        'A container query stacks each day below 34rem: the name and copy button on one line, intervals underneath.',
        'Interval rows wrap, so two selects and their buttons fit at 320px.',
      ],
    },
    theme: {
      items: [
        'Built from Monarch Switch, NativeSelect and Button, so every theme and the product accent apply.',
        'Errors use `--mn-color-danger-strong`.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Controlled with validation',
          stack: true,
          render: () => <ControlledDemo />,
          code: `const [hours, setHours] = useState(schedule.weeklyHours);
const errors = validateWeeklyHours(hours);
<WeeklyHoursEditor
  value={hours}
  onValueChange={setHours}
  weekStartsOn={settings.weekStartsOn}
  timeZoneLabel={<TimezoneSelect value={zone} onValueChange={setZone} />}
/>
<Button disabled={Object.keys(errors).length > 0}>Save</Button>`,
        },
        {
          title: 'Uncontrolled, 12-hour clock, week from Sunday',
          stack: true,
          render: () => (
            <div style={{ width: '100%', maxWidth: 640 }}>
              <WeeklyHoursEditor
                locale="en-US"
                hourCycle="12"
                weekStartsOn={0}
                legend="Support hours"
                defaultValue={[{ day: 1, start: 480, end: 1440 }]}
                step={30}
                name="supportHours"
              />
            </div>
          ),
          code: `<WeeklyHoursEditor name="supportHours" defaultValue={hours} weekStartsOn={0} hourCycle="12" step={30} />`,
        },
        {
          title: 'Read-only',
          stack: true,
          render: () => (
            <div style={{ width: '100%', maxWidth: 480 }}>
              <WeeklyHoursEditor
                locale="en-GB"
                legend="Business hours"
                readOnly
                defaultValue={WEEKDAYS}
                timeZoneLabel="Times in Asia/Kolkata"
              />
            </div>
          ),
          code: `<WeeklyHoursEditor readOnly value={hours} timeZoneLabel="Times in Asia/Kolkata" />`,
        },
      ],
    },
    doDont: {
      do: [
        'Show the time zone the hours are in.',
        'Block saving while `validateWeeklyHours` returns errors, and validate again on the server.',
        'Pass the workspace’s week start so rows match the calendar.',
      ],
      dont: [
        'Do not store times as display strings; keep minutes from midnight.',
        'Do not put holidays or one-off closures in weekly hours.',
      ],
    },
    related: {
      items: [
        { id: 'time-grid', reason: 'Shades time outside the same value (`workingHours`).' },
        { id: 'timezone-select', reason: 'Goes in the `timeZoneLabel` slot.' },
        { id: 'switch', reason: 'The per-day on/off control.' },
        { id: 'form-field', reason: 'NativeSelect provides the time lists.' },
      ],
    },
    dependencies: { packages: ['react'], components: ['button', 'form-field', 'switch'] },
    testing: {
      unit: [
        '`validateWeeklyHours`: range, order and overlap per day.',
        'Switching a day on adds the default interval (uncontrolled) and reports it.',
        'Add, edit and remove intervals.',
        'Overlap shows an alert linked to the selects with `aria-invalid`.',
        'Copy to all days.',
        'Switching a day off and on restores its intervals.',
        'Controlled mode and the hidden form input.',
        'Week start order, 12-hour labels and Tab order.',
        'Read-only text, zone label and disabled fieldset; no axe violations; renders on the server.',
      ],
      axe: true,
      ssr: true,
      browser: [
        'Visual baselines: /__gallery/weekly-hours-editor',
        'Cross-browser axe: /components/weekly-hours-editor',
      ],
    },
  },
} satisfies ComponentDocs;
