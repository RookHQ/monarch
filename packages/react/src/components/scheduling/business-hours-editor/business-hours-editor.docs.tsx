import { useState } from 'react';
import { Button } from '@rookcom/monarch-react';
import {
  BusinessHoursEditor,
  validateBusinessHours,
  type BusinessHoursValue,
} from '@rookcom/monarch-react/scheduling';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const START: BusinessHoursValue = {
  name: 'Support hours',
  timeZone: 'Europe/London',
  hours: [1, 2, 3, 4, 5].map((day) => ({ day, start: 540, end: 1050 })),
  exceptions: [
    { date: '2026-12-25', name: 'Christmas Day' },
    { date: '2026-12-31', name: 'New Year’s Eve', intervals: [{ start: 540, end: 780 }] },
  ],
};

function Demo() {
  const [value, setValue] = useState<BusinessHoursValue>(START);
  const [tried, setTried] = useState(false);
  const errors = validateBusinessHours(value, { requireName: true, requireOpen: true });
  const ok = Object.keys(errors).length === 0;
  return (
    <div style={{ display: 'grid', gap: 12, width: '100%', maxWidth: 720 }}>
      <BusinessHoursEditor
        value={value}
        onValueChange={(v: BusinessHoursValue) => setValue(v)}
        validation={{ requireName: true, requireOpen: true }}
        showErrors={tried}
        showDefault
        locale="en-GB"
        pinnedTimeZones={['Europe/London', 'America/New_York', 'Asia/Kolkata']}
      />
      <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
        <Button variant="primary" onClick={() => setTried(true)}>
          Save
        </Button>
        <span style={{ fontSize: 13 }} aria-live="polite">
          {tried ? (ok ? 'Saved.' : 'Fix the highlighted fields.') : ''}
        </span>
      </div>
    </div>
  );
}

export default {
  id: 'business-hours-editor',
  title: 'BusinessHoursEditor',
  summary: 'Edit a business-hours calendar: name, zone, weekly hours, holidays and special dates.',
  sections: {
    purpose: {
      summary:
        'One editor for “when are we open”: the hours SLA clocks run in, the hours customers can book, the hours a team covers. Needed by: Tickets (business hours and holidays for SLAs), Calendar (availability schedules with date overrides), People (office calendars).',
      useWhen: [
        'Defining a named calendar of weekly hours plus holidays or special dates, in a time zone.',
      ],
      avoidWhen: [
        'Only weekly hours, no name or dates: use WeeklyHoursEditor.',
        'Individual people’s time off: use TimeOffPanel.',
      ],
    },
    designPrinciples: {
      items: [
        'The value is plain data (`{ name, timeZone, hours, exceptions, isDefault, effectiveFrom, effectiveTo }`) that goes to the API unchanged.',
        'Weekly hours are WeeklyHoursEditor’s value, so TimeGrid can shade them directly.',
        'An exception replaces the weekly hours for its date: closed (no intervals) or open at other times.',
        '`validateBusinessHours` is exported; the product decides when to block saving and checks again on the server.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Controlled (`value` + `onValueChange(value, errors)`) or uncontrolled (`defaultValue`).',
        'Errors show once a field is touched, or all at once with `showErrors` (after a save attempt).',
        '`showName`, `showDefault`, `showEffectiveDates` and `allowSpecialHours` switch parts on and off; `children` adds product settings at the end.',
        'Special hours edit one interval per date.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Migrating from Tickets `admin/service/BusinessHoursPage` (the editor dialog): `schedule` (`{ mon: [["09:00","17:00"]] }`) → `hours` as `{ day: 1, start: 540, end: 1020 }[]`; `timezone` → `timeZone`; `holidays` → `exceptions` (no `intervals` = closed); `isDefault` unchanged; `validateSchedule` → `validateBusinessHours(value, { requireName: true, requireOpen: true })`. The list of calendars, delete confirmation and save stay in the product.',
        'Migrating from Calendar `availability/ScheduleEditor`: `name`, `timezone`, `effectiveFrom`/`effectiveTo` → `name`, `timeZone`, `effectiveFrom`/`effectiveTo` with `showEffectiveDates`; the weekly rows → `hours`; `OverridesCard` overrides → `exceptions` (`reason` → `name`, `intervals` kept); “Default schedule” → `showDefault`.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'readOnly',
          values: ['true', 'false'],
          description: 'Text only, no add or remove buttons.',
        },
        {
          prop: 'allowSpecialHours',
          values: ['true', 'false'],
          description: 'Exceptions can be open at other times, or only closed.',
        },
        {
          prop: 'showEffectiveDates',
          values: ['true', 'false'],
          description: 'Adds “Effective from/until”.',
        },
        {
          prop: 'showDefault',
          values: ['true', 'false'],
          description: 'Adds the “Default” checkbox.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Invalid exception',
          attribute: 'data-invalid (row), aria-invalid',
          description: 'Danger message under the row, linked to its fields.',
        },
        { name: 'No open hours', description: '“Open at least one interval.” with `requireOpen`.' },
        {
          name: 'Read-only',
          attribute: 'data-readonly',
          description: 'Controls read-only; weekly hours as text.',
        },
        { name: 'Disabled', attribute: 'aria-disabled', description: 'Every control disabled.' },
      ],
    },
    interaction: {
      items: [
        '“Add date” appends an empty row; choose its date and name.',
        'Switching a row to “Open at other times” adds 10:00–16:00 to edit.',
        'Weekly hours behave as in WeeklyHoursEditor (day switches, copy to all).',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab / Shift+Tab', action: 'Through every field in reading order.' },
        { keys: 'Arrow keys', action: 'Within selects, the zone list and date pickers.' },
        { keys: 'Enter / Space', action: 'Add date, remove, checkboxes and switches.' },
      ],
    },
    accessibility: {
      role: 'group (fieldsets for weekly hours and exceptions)',
      name: '“Weekly hours” and “Holidays and exceptions”; row fields are named “Date 1”, “Name of date 1”, “Hours on date 1”.',
      items: [
        'Remove buttons name the date (“Remove Christmas Day”).',
        'Errors use `role="alert"` and are linked through `aria-describedby`.',
        'All strings are in `messages`.',
      ],
    },
    responsive: {
      items: [
        'A container query stacks each exception row below 36rem; at 390px nothing scrolls sideways.',
      ],
    },
    theme: { items: ['Composed of Monarch fields, so every theme and the product accent apply.'] },
    examples: {
      items: [
        { title: 'Support calendar with validation', stack: true, render: () => <Demo /> },
        {
          title: 'Schedule with effective dates',
          stack: true,
          render: () => (
            <div style={{ width: '100%', maxWidth: 720 }}>
              <BusinessHoursEditor
                defaultValue={{
                  ...START,
                  name: 'Winter clinic',
                  effectiveFrom: '2026-11-01',
                  effectiveTo: '2027-02-28',
                }}
                showEffectiveDates
                locale="en-GB"
              />
            </div>
          ),
        },
        {
          title: 'Read-only',
          stack: true,
          render: () => (
            <div style={{ width: '100%', maxWidth: 720 }}>
              <BusinessHoursEditor defaultValue={START} readOnly showName={false} locale="en-GB" />
            </div>
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Say what the hours control (SLA clocks, bookings) next to the editor.',
        'Validate again on the server.',
      ],
      dont: [
        'Do not store times as display strings.',
        'Do not put one person’s leave in business exceptions.',
      ],
    },
    related: {
      items: [
        { id: 'weekly-hours-editor', reason: 'The weekly part.' },
        { id: 'timezone-select', reason: 'The zone picker.' },
        { id: 'time-off-panel', reason: 'People’s time off.' },
        { id: 'time-grid', reason: 'Shades the same weekly hours.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: [
        'button',
        'checkbox',
        'form-field',
        'text-field',
        'date-picker',
        'timezone-select',
        'weekly-hours-editor',
      ],
    },
    testing: {
      unit: [
        '`validateBusinessHours`: name, open, hours, exception date, name, duplicate, hours and effective order.',
        'Name, zone, weekly hours and exceptions render; axe clean.',
        'Add, edit and remove exceptions with value and errors.',
        'Special hours and their validation.',
        '`showErrors`, effective dates, default checkbox and read-only; renders on the server.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
