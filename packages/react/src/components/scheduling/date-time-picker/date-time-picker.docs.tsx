import { useState } from 'react';
import { Field, FieldLabel, FieldDescription } from '@rookcom/monarch-react';
import { DateTimePicker } from '@rookcom/monarch-react/scheduling';
import type { ComponentDocs } from '../../../internal/component-docs.js';

// Examples pin `today` so the page and its visual baselines never change with the date.
const TODAY = '2026-11-04';

function Interview() {
  const [value, setValue] = useState<string | null>('2026-11-10T14:30');
  return (
    <Field>
      <FieldLabel>Interview</FieldLabel>
      <DateTimePicker
        locale="en-GB"
        today={TODAY}
        min="2026-11-04T09:00"
        step={15}
        timeZone="IST"
        value={value}
        onValueChange={setValue}
      />
      <FieldDescription>Value: {value ?? 'none'}</FieldDescription>
    </Field>
  );
}

export default {
  id: 'date-time-picker',
  title: 'DateTimePicker',
  summary:
    'A date and a wall-clock time in one field: DatePicker plus a typed 12- or 24-hour time, a step and a time-zone slot.',
  sections: {
    purpose: {
      summary:
        'Collects a moment such as a meeting, interview or scheduled send as a date plus a wall-clock time. The value is `YYYY-MM-DDTHH:mm`, the same string as `<input type="datetime-local">`, so it is never converted between time zones by accident; the zone it belongs to is shown in the `timeZone` slot. Import from `@rookcom/monarch-react/scheduling`.',
      useWhen: [
        'Scheduling a meeting, an interview, a reminder or a send time.',
        'Any form that today uses `type="datetime-local"`.',
      ],
      avoidWhen: [
        'Only a date: use DatePicker.',
        'Choosing among offered slots (booking): show the slots as buttons instead of free entry.',
        'A length of time: use DurationInput.',
      ],
    },
    designPrinciples: {
      items: [
        'Wall-clock values only: the component never converts zones. The product converts to UTC with the zone it shows.',
        'The clock follows the locale (12 or 24 hours) and accepts either when typed.',
        'Picking a date keeps the time; picking a date before any time uses `defaultTime`.',
        'On the boundary day of `min` or `max`, a picked date clamps the time into range; a typed out-of-range time is explained, not silently changed.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'The ref goes to the root `role="group"` element.',
        'The date part is a DatePicker; its messages can be translated through the same `messages` prop.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        '`value`/`onValueChange` (controlled) or `defaultValue`, as `YYYY-MM-DDTHH:mm` or `null`.',
        '`min`/`max` are date-times; days outside are disabled, times outside on the boundary day are rejected.',
        '`timeZone` is a slot: a label ("IST", "Europe/London") or a time-zone select.',
        '`onValidationChange` reports the date part’s errors, `invalid-time`, or `null`.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'hourCycle',
          values: ['12', '24'],
          description: 'Clock format. Default: from the locale.',
        },
        {
          prop: 'step',
          values: ['minutes'],
          description: 'How far ↑ and ↓ move the time, snapping to multiples. Default 15.',
        },
        { prop: 'size', values: ['sm', 'md', 'lg'], description: 'Control height. Default `md`.' },
        {
          prop: 'timeZone',
          values: ['none', 'label', 'control'],
          description: 'Shown after the time.',
        },
      ],
    },
    states: {
      items: [
        { name: 'Empty', description: 'Placeholders show the date format and an example time.' },
        {
          name: 'Invalid time',
          attribute: 'aria-invalid="true" on the time input',
          description: 'Unreadable, or outside min/max on that day; a message explains it.',
        },
        {
          name: 'Invalid date',
          attribute: 'aria-invalid="true" on the date input',
          description: 'As DatePicker.',
        },
        { name: 'Disabled', attribute: 'disabled, data-disabled', description: 'Both parts.' },
      ],
    },
    interaction: {
      items: [
        'The date part behaves as DatePicker (typing, calendar, presets, clear). Clearing the date clears the value.',
        'The time is read on blur and Enter: `14:30`, `2:30 pm`, `2pm`, `1430`, `9.15` all work.',
        '↑ and ↓ in the time move to the next or previous multiple of `step`, wrapping at midnight on the same day.',
        'Typing a time before a date uses today (or the `min` day if later).',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Date field, its buttons, then the time field.' },
        { keys: '↑ / ↓ (time)', action: 'Later / earlier by `step` minutes.' },
        { keys: 'Enter (time)', action: 'Read the typed time.' },
        { keys: '↓ (date)', action: 'Open the calendar (see DatePicker).' },
      ],
    },
    accessibility: {
      role: 'group containing two textboxes',
      name: 'The group is named by FieldLabel (or `aria-label`); the parts are "<label> date" and "<label> time".',
      items: [
        'Each part gets `aria-invalid` and its own error, linked with `aria-describedby`.',
        'The time zone is visible text next to the time, not only a tooltip.',
        'Inside a Field, the label, required state and description apply to both parts.',
      ],
    },
    responsive: {
      items: [
        'Date and time sit in one row and wrap when the container is narrow; on phones the date takes the full width with the time and zone below.',
        'The calendar popover follows DatePicker’s phone layout.',
      ],
    },
    theme: {
      items: [
        'Input tokens for both parts; every theme and more-contrast mode applies.',
        'No motion of its own; the calendar respects reduced motion.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Interview time with a time zone',
          render: () => <Interview />,
          code: `const [value, setValue] = useState<string | null>('2026-11-10T14:30');

<Field>
  <FieldLabel>Interview</FieldLabel>
  <DateTimePicker
    locale="en-GB"
    min="2026-11-04T09:00"
    step={15}
    timeZone="IST"
    value={value}
    onValueChange={setValue}
  />
</Field>`,
        },
        {
          title: '12-hour clock (en-US) and sizes',
          stack: true,
          render: () => (
            <>
              <Field>
                <FieldLabel>Send at</FieldLabel>
                <DateTimePicker
                  locale="en-US"
                  today={TODAY}
                  defaultValue="2026-11-05T08:00"
                  timeZone="New York"
                />
              </Field>
              <DateTimePicker
                aria-label="Reminder"
                size="sm"
                locale="en-GB"
                today={TODAY}
                defaultValue="2026-11-05T17:45"
              />
            </>
          ),
          code: `<Field>
  <FieldLabel>Send at</FieldLabel>
  <DateTimePicker locale="en-US" defaultValue="2026-11-05T08:00" timeZone="New York" />
</Field>
<DateTimePicker aria-label="Reminder" size="sm" locale="en-GB" defaultValue="2026-11-05T17:45" />`,
        },
        {
          title: 'Disabled',
          render: () => (
            <Field disabled>
              <FieldLabel>Starts</FieldLabel>
              <DateTimePicker
                locale="en-GB"
                today={TODAY}
                defaultValue="2026-11-04T13:00"
                timeZone="UTC"
              />
            </Field>
          ),
          code: `<Field disabled>
  <FieldLabel>Starts</FieldLabel>
  <DateTimePicker locale="en-GB" defaultValue="2026-11-04T13:00" timeZone="UTC" />
</Field>`,
        },
      ],
    },
    doDont: {
      do: [
        'Always show the zone the value is in with `timeZone`, and convert to UTC on the server with that zone.',
        'Use `step` that matches the product’s slots (15 or 30 minutes for meetings).',
      ],
      dont: [
        'Do not pass `new Date().toISOString()` (UTC) as the value; use the wall-clock string in the shown zone.',
        'Do not hide the time part for all-day events; use DatePicker instead.',
      ],
    },
    related: {
      items: [
        { id: 'date-picker', reason: 'Only a date.' },
        { id: 'timezone-select', reason: 'Let people choose the zone shown in the slot.' },
        { id: 'duration-input', reason: 'A length of time.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['form-field', 'date-picker'],
    },
    testing: {
      unit: [
        'Labelled group with "date" and "time" parts and the zone slot; axe clean.',
        '12-hour clock for en-US and through `hourCycle`.',
        'Typed times in either clock, on Enter and blur.',
        '↑/↓ step and snap to multiples.',
        'Picking a date first uses `defaultTime`; typing a time first uses today.',
        'Unreadable time flagged without changing the value; `onValidationChange`.',
        'min/max: days disabled, time clamped on the boundary day, typed times and dates rejected with messages.',
        'Clearing the date clears the value; controlled value; hidden input; aria-label without Field; disabled.',
      ],
      axe: true,
      ssr: true,
      browser: [
        'Visual baselines: /__gallery/date-time-picker',
        'Cross-browser axe: /components/date-time-picker',
      ],
    },
  },
} satisfies ComponentDocs;
