import { useState } from 'react';
import { SlotPicker, type SlotPickerSlot } from '@rookcom/monarch-react/scheduling';
import type { ComponentDocs } from '../../../internal/component-docs.js';

/** Sample opening times: 09:00-12:00 UTC every 30 minutes, with a full slot and a small group. */
function sampleSlots(day: string): SlotPickerSlot[] {
  if (day.endsWith('-08') || day.endsWith('-15')) return [];
  return [0, 30, 60, 90, 120, 150].map((offset, i) => {
    const h = 9 + Math.floor(offset / 60);
    const mm = offset % 60;
    return {
      start: `${day}T${String(h).padStart(2, '0')}:${String(mm).padStart(2, '0')}:00Z`,
      disabled: i === 2,
      seatsLeft: i === 4 ? 3 : null,
    };
  });
}

const BOOKABLE = [
  '2026-11-04',
  '2026-11-05',
  '2026-11-06',
  '2026-11-09',
  '2026-11-10',
  '2026-11-11',
];

function BookingDemo() {
  const [day, setDay] = useState<string | null>('2026-11-04');
  const [value, setValue] = useState<string | null>(null);
  const [zone, setZone] = useState('Europe/London');
  return (
    <div style={{ display: 'grid', gap: 8, width: '100%', maxWidth: 720 }}>
      <SlotPicker
        locale="en-GB"
        today="2026-11-02"
        day={day}
        onDayChange={(d: string) => {
          setDay(d);
          setValue(null);
        }}
        availableDays={BOOKABLE}
        min="2026-11-02"
        max="2026-12-31"
        slots={day ? sampleSlots(day) : []}
        value={value}
        onValueChange={setValue}
        timeZone={zone}
        onTimeZoneChange={setZone}
        businessTimeZone="Europe/London"
        pinnedTimeZones={['Europe/London', 'America/New_York', 'Asia/Kolkata']}
      />
      <p style={{ margin: 0, fontSize: 13 }}>Selected: {value ?? 'none'}</p>
    </div>
  );
}

export default {
  id: 'slot-picker',
  title: 'SlotPicker',
  summary: 'Pick a day, then one of its open times, in the visitor’s time zone.',
  sections: {
    purpose: {
      summary:
        'The booking step where a person chooses when: a month calendar of bookable days beside the selected day’s open times, shown in the visitor’s time zone. Needed by: Calendar (public booking, new appointment, reschedule), Stride, Chat, Recruit (interview scheduling) and Tickets, which each keep a copy of the calendar kit’s SlotPicker today.',
      useWhen: [
        'Choosing an appointment, meeting or interview time from availability the server computed.',
        'Rescheduling: show the new open times for the same service and host.',
      ],
      avoidWhen: [
        'Picking any time, not one of a set: use DateTimePicker.',
        'Seeing many people’s calendars to find a gap: use TimeGrid.',
      ],
    },
    designPrinciples: {
      items: [
        'Data in, choices out: the product loads `slots` for `day` (and bookable `availableDays` for the month); the picker never fetches.',
        'Times are instants (`start` in ISO with `Z` or an offset) shown in `timeZone`, so a slot means the same moment for host and visitor.',
        'The zone is always visible: a picker when the visitor can change it, a line of text when not, plus a note when the business works in another zone.',
        'Every state has a place: choose a day, loading, no times (with an optional way out), error with retry, and a warning when the current value is no longer open.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Renders a `<div>` (ref) with MiniMonth, the day heading, the group of times and the zone row.',
        '`value`/`onValueChange(start, slot)` and `day`/`onDayChange` are each controlled or uncontrolled (`defaultValue`, `defaultDay`). Clearing the value when the day changes is up to the product.',
        '`month`/`onMonthChange` lets the product load `availableDays` month by month.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Migrating from the app copy (calendar kit `SlotPicker`): `month`/`onMonthChange` keep their names; `day`/`onDayChange` keep theirs; `available: Set` → `availableDays: string[]`; `minDay`/`maxDay` → `min`/`max`; `selected`/`onSelect` → `value`/`onValueChange`; `zone`/`onZoneChange` → `timeZone`/`onTimeZoneChange`; `businessZone` → `businessTimeZone`; `timeFormat` → `hourCycle` (`"12"`/`"24"`); `emptyText`/`chooseText`/`zoneText` → `messages.empty`/`chooseDay`/`timeZone`; `KitSlot` → `SlotPickerSlot`.',
        'Migrating from Calendar `appointments/SlotPicker`: the fetch by `serviceId`/`hostIds` stays in the product, which passes `slots`, `loading`, `error` and `onRetry`; `onUseOutsideHours` → `onChooseOutside`; toggling the selected slot off → `allowDeselect`.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'layout',
          values: ['auto', 'stacked'],
          description:
            '`auto` puts the month beside the times when the container is 40rem or wider; `stacked` always stacks (sidebars, dialogs).',
        },
        {
          prop: 'onTimeZoneChange',
          values: ['set', 'unset'],
          description: 'With it, a TimezoneSelect; without it, “Times shown in …” text.',
        },
      ],
    },
    states: {
      items: [
        { name: 'No day', description: '“Choose a day to see available times.”' },
        {
          name: 'Loading',
          attribute: 'data-loading, aria-busy',
          description: 'Placeholder tiles; “Loading available times” is announced.',
        },
        {
          name: 'Empty',
          description: 'Dashed box with `messages.empty` and the optional way out.',
        },
        { name: 'Error', description: 'Danger Alert with `error` and an optional retry button.' },
        {
          name: 'Selected',
          attribute: 'data-selected, aria-pressed (time)',
          description: 'Accent fill.',
        },
        {
          name: 'Unavailable time',
          attribute: 'disabled (time)',
          description: 'Struck through and skipped by the arrow keys.',
        },
        {
          name: 'Value not open',
          description: 'Warning Alert when `value` falls on the day but is not one of its slots.',
        },
      ],
    },
    interaction: {
      items: [
        'Picking a day calls `onDayChange`; the product loads that day’s slots.',
        'Pressing a time selects it; with `allowDeselect`, pressing it again clears it.',
        'A polite status line announces how many times the day has once they load.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Arrow keys, Page Up/Down, Home/End',
          action: 'Move through days in the month grid (MiniMonth).',
        },
        { keys: 'Tab', action: 'Moves from the month to the times; the times are one tab stop.' },
        { keys: 'Arrow keys', action: 'Move between times, skipping unavailable ones.' },
        { keys: 'Home / End', action: 'First or last time.' },
        { keys: 'Enter / Space', action: 'Select the focused time.' },
      ],
    },
    accessibility: {
      role: 'group of toggle buttons; MiniMonth grid',
      name: 'The group is “Available times on Wednesday 4 November”; each time is named by its text and seats left.',
      items: [
        'Selected time has `aria-pressed="true"`; unavailable times are `disabled`.',
        'Loading sets `aria-busy` on the times column and is announced by a status line.',
        'The time zone picker sits in a Field with a visible label.',
        'All strings are in `messages`; dates and times come from Intl in `locale`.',
      ],
    },
    responsive: {
      items: [
        'A container query decides the layout, so it works in a page, a dialog or a sidebar.',
        'Times wrap in an auto-fill grid; at 390px they make three columns under the month.',
        'Time buttons are at least 44px tall on coarse pointers.',
      ],
    },
    theme: {
      items: [
        'Selected time uses the product accent (`--mn-color-accent-solid`), hover the accent subtle tint.',
        'Forced colours: selected time in Highlight, unavailable in GrayText.',
      ],
    },
    examples: {
      items: [
        { title: 'Booking with a time zone picker', stack: true, render: () => <BookingDemo /> },
        {
          title: 'Stacked, loading',
          stack: true,
          render: () => (
            <div style={{ width: '100%', maxWidth: 360 }}>
              <SlotPicker
                layout="stacked"
                locale="en-GB"
                today="2026-11-02"
                defaultDay="2026-11-04"
                slots={[]}
                loading
                timeZone="Europe/London"
              />
            </div>
          ),
        },
        {
          title: 'No times, with a way out',
          stack: true,
          render: () => (
            <div style={{ width: '100%', maxWidth: 720 }}>
              <SlotPicker
                locale="en-US"
                hourCycle="12"
                today="2026-11-02"
                defaultDay="2026-11-08"
                slots={[]}
                timeZone="America/New_York"
                onChooseOutside={() => {}}
              />
            </div>
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Load slots for the visible day only, and pass `availableDays` so empty days are disabled up front.',
        'Show the business zone note when visitors book across zones.',
        'Check the slot again on the server when booking: it may have been taken.',
      ],
      dont: [
        'Do not send wall-clock strings without an offset as `start`.',
        'Do not hide the time zone.',
      ],
    },
    related: {
      items: [
        { id: 'booking-flow', reason: 'The step-by-step booking pattern that hosts this picker.' },
        { id: 'reschedule-dialog', reason: 'Moves an existing booking using this picker.' },
        { id: 'mini-month', reason: 'The month calendar on the left.' },
        { id: 'timezone-select', reason: 'The zone picker.' },
        { id: 'date-time-picker', reason: 'Any date and time, not a set of slots.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['button', 'alert', 'skeleton', 'form-field', 'mini-month', 'timezone-select'],
    },
    testing: {
      unit: [
        'Day heading, named group of times, seats left and disabled times; status line count.',
        'Times formatted in the given zone.',
        'Click selects and reports the slot; `allowDeselect` clears.',
        'One tab stop; arrows skip disabled times; Home/End; Enter selects.',
        'No day yet, day picking, `availableDays` disables other days.',
        'Loading, empty with way out, error with retry, value-not-open warning.',
        'Zone picker labelled; business zone note; no axe violations; renders on the server.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
