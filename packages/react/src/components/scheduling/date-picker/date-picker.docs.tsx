import { useState } from 'react';
import {
  Dialog,
  DialogContent,
  DialogTrigger,
  DialogTitle,
  DialogBody,
  Button,
  Field,
  FieldLabel,
  FieldDescription,
} from '@rookcom/monarch-react';
import { DatePicker } from '@rookcom/monarch-react/scheduling';
import type { ComponentDocs } from '../../../internal/component-docs.js';

// Examples pin `today` so the page and its visual baselines never change with the date.
const TODAY = '2026-11-04';

const PRESETS = [
  { label: 'Today', days: 0 },
  { label: 'Tomorrow', days: 1 },
  { label: 'In a week', days: 7 },
  { label: 'In two weeks', days: 14 },
];

function DueDate() {
  const [value, setValue] = useState<string | null>(null);
  return (
    <Field>
      <FieldLabel>Due date</FieldLabel>
      <DatePicker
        locale="en-GB"
        today={TODAY}
        presets={PRESETS}
        value={value}
        onValueChange={setValue}
      />
      <FieldDescription>Type a date or pick one. Value: {value ?? 'none'}</FieldDescription>
    </Field>
  );
}

function InDialog() {
  return (
    <Dialog>
      <DialogTrigger asChild>
        <Button>Schedule review</Button>
      </DialogTrigger>
      <DialogContent size="sm">
        <DialogTitle>Schedule review</DialogTitle>
        <DialogBody>
          <Field>
            <FieldLabel>Review date</FieldLabel>
            <DatePicker locale="en-GB" today={TODAY} min={TODAY} />
          </Field>
        </DialogBody>
      </DialogContent>
    </Dialog>
  );
}

export default {
  id: 'date-picker',
  title: 'DatePicker',
  summary:
    'A date field: type in the locale’s format or pick from a calendar popover, with presets, min/max and Field errors.',
  sections: {
    purpose: {
      summary:
        'Lets people enter one calendar date in a form, by typing it the way their locale writes dates or by picking it from a calendar. The value is an ISO `YYYY-MM-DD` string with no time zone, so it never shifts by a day. Replaces native `type="date"` inputs, which render differently in every browser and cannot show presets. Import from `@rookcom/monarch-react/scheduling`.',
      useWhen: [
        'Due dates, start dates, birthdays, renewal and expiry dates in forms and editors.',
        'A date with quick picks such as Today, Tomorrow and In a week.',
      ],
      avoidWhen: [
        'A date and a time: use DateTimePicker.',
        'A period: use DateRangePicker.',
        'Showing a date read-only: format it with RelativeTime or plain text.',
      ],
    },
    designPrinciples: {
      items: [
        'Typing is first-class: the field accepts the locale’s numeric order, month names, ISO and digits without separators.',
        'Invalid text is kept so it can be fixed, and explained with the expected format; the value only changes to a valid date.',
        'The calendar is the same MiniMonth grid everywhere, with the same keyboard model.',
        'No time zones: a date is a calendar date.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'The ref goes to the text `<input>`; native input props (`id`, `aria-*`, `onBlur`, `autoFocus`, …) go there too. `className` styles the wrapper.',
        'With `name`, a hidden input submits the ISO value; the visible input has no name.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        '`value`/`onValueChange` (controlled) or `defaultValue`; `onValueChange` receives a valid ISO date or `null`, never invalid text.',
        '`presets`: `{ label, days }` relative to today or `{ label, date }` fixed.',
        '`onValidationChange` reports `invalid`, `before-min`, `after-max`, `unavailable` or `null`; set `showErrors={false}` to render your own FieldError.',
        '`messages` translates every string, including the error texts.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'size',
          values: ['sm', 'md', 'lg'],
          description: 'Control height, as Input. Default `md`.',
        },
        {
          prop: 'presets',
          values: ['none', 'list'],
          description: 'Quick picks beside the calendar (above it on phones).',
        },
        {
          prop: 'clearable',
          values: ['true', 'false'],
          description: 'A clear button while there is a value. Default true.',
        },
        {
          prop: 'formatOptions',
          values: ['numeric (default)', 'Intl options'],
          description: 'How the value is written in the field, e.g. `{ dateStyle: "medium" }`.',
        },
      ],
    },
    states: {
      items: [
        { name: 'Empty', description: 'Shows the format hint as placeholder, e.g. DD/MM/YYYY.' },
        {
          name: 'Open',
          attribute: 'data-state="open" on the calendar button',
          description: 'The calendar popover is showing.',
        },
        {
          name: 'Invalid',
          attribute: 'aria-invalid="true", data-invalid',
          description:
            'Unreadable text, a date outside min/max or a disabled date; a message explains why.',
        },
        {
          name: 'Disabled',
          attribute: 'disabled, data-disabled',
          description: 'From the prop or the Field; the calendar cannot open.',
        },
        {
          name: 'Read-only',
          attribute: 'readonly',
          description: 'The value can be read and copied; the calendar button is disabled.',
        },
      ],
    },
    interaction: {
      items: [
        'Typed text is read on blur and on Enter. A valid date is re-formatted in the locale; anything else stays as typed with an error.',
        'The calendar button or ↓ in the field opens the calendar, focused on the value (or today). Picking a day or a preset closes it and returns focus to the field.',
        'Emptying the field or pressing the clear button sets the value to `null`.',
        'The calendar is a modal popover: it traps focus while open and works inside Dialog and Sheet; Escape closes only the calendar.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Enter (in the field)', action: 'Read the typed date.' },
        { keys: '↓ (in the field)', action: 'Open the calendar.' },
        { keys: 'Arrows, Home/End, Page Up/Down', action: 'Move in the calendar (see MiniMonth).' },
        { keys: 'Enter / Space (in the calendar)', action: 'Pick the focused day and close.' },
        { keys: 'Escape', action: 'Close the calendar without changing the value.' },
        {
          keys: 'Tab',
          action:
            'Moves between the field, the clear and calendar buttons; inside the calendar, between presets, month buttons and the grid.',
        },
      ],
    },
    accessibility: {
      role: 'textbox, with a button that opens a dialog containing a grid',
      name: 'From FieldLabel (or `aria-label`). The calendar button is named "Choose date" or "Change date, <full date>"; the popover "Choose date".',
      items: [
        'The calendar button has `aria-haspopup="dialog"` and `aria-expanded`.',
        'Errors are rendered under the field, linked by `aria-describedby` and announced politely; the field gets `aria-invalid`.',
        'Inside a Field, `aria-required`, the description and `disabled` are wired automatically.',
        'The format hint is the placeholder and is repeated in the error text, so it is not the only cue.',
        'Presets are buttons with `aria-pressed` for the current value.',
      ],
    },
    responsive: {
      items: [
        'The field fills its container; the popover never exceeds the viewport minus 1rem.',
        'Below 576px, presets move above the calendar as wrapping chips.',
        'Larger buttons and presets on touch screens.',
      ],
    },
    theme: {
      items: [
        'Uses Input’s tokens for the field and MiniMonth’s for the calendar, so every theme and more-contrast mode applies.',
        'Transitions are removed under `prefers-reduced-motion: reduce`.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Due date with quick picks',
          render: () => <DueDate />,
        },
        {
          title: 'Min, max and an error',
          stack: true,
          render: () => (
            <Field>
              <FieldLabel>Renewal date</FieldLabel>
              <DatePicker
                locale="en-GB"
                today={TODAY}
                min="2026-11-01"
                max="2027-10-31"
                defaultValue="2027-12-01"
              />
            </Field>
          ),
        },
        {
          title: 'Sizes and formats',
          stack: true,
          render: () => (
            <>
              <DatePicker
                aria-label="Start"
                size="sm"
                locale="en-US"
                today={TODAY}
                defaultValue="2026-11-04"
              />
              <DatePicker
                aria-label="Published"
                size="lg"
                locale="de-DE"
                today={TODAY}
                defaultValue="2026-11-04"
                formatOptions={{ dateStyle: 'medium' }}
              />
              <DatePicker
                aria-label="Locked"
                disabled
                locale="en-GB"
                today={TODAY}
                defaultValue="2026-11-04"
              />
            </>
          ),
        },
        {
          title: 'Inside a dialog',
          render: () => <InDialog />,
        },
      ],
    },
    doDont: {
      do: [
        'Put it in a Field with a FieldLabel, and pass `locale` from the user’s settings.',
        'Store and send the ISO string as is; format it for display with Intl and `timeZone: "UTC"`.',
        'Offer presets for the common choices in task and deadline editors.',
      ],
      dont: [
        'Do not build a `Date` from the value in local time (`new Date("2026-11-04")` is UTC midnight and shows the previous day west of UTC).',
        'Do not show overdue state only by colouring the field; add a Badge or text.',
        'Do not use it for a date with a time: use DateTimePicker.',
      ],
    },
    related: {
      items: [
        { id: 'date-time-picker', reason: 'A date and a time.' },
        { id: 'date-range-picker', reason: 'A start and end date.' },
        { id: 'mini-month', reason: 'The calendar grid on its own.' },
        { id: 'form-field', reason: 'Label, description and errors.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['form-field', 'popover', 'mini-month'],
      notes: ['The popover is Monarch Popover (Radix), opened as modal.'],
    },
    testing: {
      unit: [
        'Labelled textbox wired to Field (required, description); calendar button with `aria-haspopup`; axe clean.',
        'Typed dates in en-GB and en-US order, on blur and Enter; month names; re-formatting.',
        'Invalid text kept with an error and `aria-invalid`, value unchanged, `onValidationChange`.',
        'min, max and disabled dates rejected with messages naming the bound.',
        'Calendar opens focused on the value, picks with the keyboard, closes and returns focus; ↓ opens on the typed month; Escape.',
        'Presets relative to today, disabled outside the bounds.',
        'Clear button and emptying the text; controlled value; hidden input for forms; disabled and read-only.',
        'Inside Dialog: the calendar takes focus and Escape closes only the calendar.',
      ],
      axe: true,
      ssr: true,
      browser: [
        'Visual baselines: /__gallery/date-picker and the open calendar (overlays/date-picker-*.png)',
        'Cross-browser axe: /components/date-picker',
      ],
    },
  },
} satisfies ComponentDocs;
