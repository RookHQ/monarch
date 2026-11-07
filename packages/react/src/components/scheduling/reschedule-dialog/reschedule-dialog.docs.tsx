import { useState } from 'react';
import { Button } from '@rookcom/monarch-react';
import {
  RescheduleDialog,
  type RescheduleDetails,
  type SlotPickerSlot,
} from '@rookcom/monarch-react/scheduling';
import type { ComponentDocs } from '../../../internal/component-docs.js';

function slotsFor(day: string): SlotPickerSlot[] {
  if (day.endsWith('-07') || day.endsWith('-08')) return [];
  return ['09:00', '09:30', '11:00', '14:00', '15:30'].map((t) => ({ start: `${day}T${t}:00Z` }));
}

function Demo({ outside = false }: { outside?: boolean }) {
  const [day, setDay] = useState<string>('2026-11-05');
  const [last, setLast] = useState<RescheduleDetails | null>(null);
  return (
    <div style={{ display: 'grid', gap: 8 }}>
      <RescheduleDialog
        trigger={<Button>Reschedule</Button>}
        subject="Consultation with Sam Lee"
        currentStart="2026-11-04T09:30:00Z"
        timeZone="Europe/London"
        locale="en-GB"
        allowOutsideHours={outside}
        notice={outside ? 'The customer is emailed the new time.' : undefined}
        slotPicker={{
          day,
          onDayChange: setDay,
          today: '2026-11-02',
          min: '2026-11-02',
          slots: slotsFor(day),
        }}
        onConfirm={(details: RescheduleDetails) =>
          new Promise<void>((resolve) =>
            setTimeout(() => {
              setLast(details);
              resolve();
            }, 600),
          )
        }
      />
      <p style={{ margin: 0, fontSize: 13 }}>
        {last
          ? `Moved to ${last.start}${last.outsideHours ? ' (outside hours)' : ''}`
          : 'Not moved yet'}
      </p>
    </div>
  );
}

export default {
  id: 'reschedule-dialog',
  title: 'RescheduleDialog',
  summary: 'Move a booking to a new open time, with an optional reason and outside-hours override.',
  sections: {
    purpose: {
      summary:
        'Moves an existing appointment, meeting or interview: the open slots for the same service, an optional switch for staff to type any time, and a reason kept in the history. Needed by: Calendar (appointments, suggest another time), Recruit (interviews).',
      useWhen: [
        'Changing the time of something that was booked from availability.',
        'Staff proposing another time for a pending request (pass a `notice` and your own labels).',
      ],
      avoidWhen: [
        'Editing every field of an event: use a form in a Dialog or Sheet.',
        'First-time booking: use BookingFlow or SlotPicker on the page.',
      ],
    },
    designPrinciples: {
      items: [
        'The product loads slots and saves; the dialog owns the choice, the reason and the pending and error states.',
        'Times are instants in a stated zone; a typed outside-hours time is converted from that zone before `onConfirm`.',
        'Nothing closes the dialog while saving, and a failed save keeps everything the person entered.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        '`onConfirm({ start, outsideHours, reason? })`: return a promise to keep the dialog pending; a rejection shows its message in the dialog.',
        '`slotPicker` takes SlotPicker props without the value (`slots`, `day`, `onDayChange`, `availableDays`, `loading`, `error`, `onRetry`, …). The chosen slot clears when the day changes.',
        '`children` go above the picker, for extra fields such as a host select.',
        'Ref goes to the dialog content element.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Migrating from the app copy (Calendar `appointments/RescheduleDialog`): `appointment` → `subject` + `currentStart`; the mutation → `onConfirm` (returning its promise); the toast stays in the product; `suggest` → `notice`, `title` and `messages.confirm`; the host select → `children`; `OutsideHoursInput` → `allowOutsideHours` (the result has `outsideHours: true`); the reason textarea → `showReason` (on by default); the internal SlotPicker fetch → `slotPicker.slots`/`loading`/`error`.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'allowOutsideHours',
          values: ['true', 'false'],
          description: 'Adds a switch that replaces the slots with a typed date and time.',
        },
        {
          prop: 'showReason',
          values: ['true', 'false'],
          description: 'The optional reason field.',
        },
        { prop: 'size', values: ['md', 'lg', 'xl'], description: 'Dialog width. Default `lg`.' },
      ],
    },
    states: {
      items: [
        { name: 'No time chosen', description: 'Confirming shows a warning instead of saving.' },
        {
          name: 'Pending',
          attribute: 'aria-busy, data-busy (dialog)',
          description: 'Confirm shows a spinner; Escape, outside click and Cancel do nothing.',
        },
        {
          name: 'Error',
          description: 'Danger Alert above the actions, linked to the confirm button.',
        },
        {
          name: 'Outside hours',
          description: 'The switch is on; a DateTimePicker replaces the slots.',
        },
      ],
    },
    interaction: {
      items: [
        'Each opening starts clean: no slot, switch off, the typed time set to the current start, no reason.',
        'Picking another day clears the chosen slot.',
        'When a day has no slots and outside hours are allowed, “Choose another time” turns the switch on.',
        'On success the dialog closes and focus returns to the trigger.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Tab / Shift+Tab',
          action: 'Moves through the fields; focus stays inside the dialog.',
        },
        { keys: 'Arrow keys', action: 'Move through days and through times (SlotPicker).' },
        { keys: 'Enter', action: 'Submits from the typed time; activates buttons.' },
        { keys: 'Escape', action: 'Closes, unless a save is running.' },
      ],
    },
    accessibility: {
      role: 'dialog',
      name: 'The title; the description names the subject and current time.',
      items: [
        'Errors use `role="alert"` (assertive) and are referenced by the confirm button.',
        'The outside-hours switch and reason field have visible labels.',
        'All strings are in `messages`.',
      ],
    },
    responsive: {
      items: [
        'The SlotPicker inside stacks the month above the times in narrow dialogs.',
        'At 390px the dialog is the viewport width minus the margin; the body scrolls, the footer stays.',
      ],
    },
    theme: {
      items: [
        'Built from Dialog, SlotPicker, Switch, DateTimePicker, Field and Alert, so every theme applies.',
      ],
    },
    examples: {
      items: [
        { title: 'Reschedule to an open slot', render: () => <Demo /> },
        { title: 'Staff: allow a time outside working hours', render: () => <Demo outside /> },
      ],
    },
    doDont: {
      do: [
        'Exclude the booking being moved when you compute its slots, so its own time is not a clash.',
        'Return the save promise from `onConfirm` so failures stay in the dialog.',
      ],
      dont: [
        'Do not offer outside hours to customers.',
        'Do not close the dialog yourself before the save resolves.',
      ],
    },
    related: {
      items: [
        { id: 'slot-picker', reason: 'The open-slot picker inside.' },
        { id: 'booking-flow', reason: 'First-time booking.' },
        { id: 'date-time-picker', reason: 'The typed outside-hours time.' },
        { id: 'confirm-dialog', reason: 'Same pending and error pattern.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: [
        'button',
        'alert',
        'form-field',
        'switch',
        'text-field',
        'dialog',
        'date-time-picker',
        'slot-picker',
      ],
    },
    testing: {
      unit: [
        'Opens from the trigger; name and description; Escape closes and returns focus; axe clean.',
        'Confirm without a time warns; slot and trimmed reason are sent; closes on success.',
        'Pending blocks Escape; a rejection shows its message and keeps the dialog open.',
        'Outside hours: typed time converted from the zone.',
        'Wall-time conversion across zones and DST; renders on the server.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
