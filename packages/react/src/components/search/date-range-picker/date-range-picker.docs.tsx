import { useState } from 'react';
import { Field, FieldLabel, FieldDescription } from '@rookcom/monarch-react';
import { DateRangePicker, type DateRange } from '@rookcom/monarch-react/scheduling';
import type { ComponentDocs } from '../../../internal/component-docs.js';

// Examples pin `today` so the page and its visual baselines never change with the date.
const TODAY = '2026-11-04';

function ReportRange() {
  const [value, setValue] = useState<DateRange | null>({ start: '2026-10-06', end: '2026-11-04' });
  return (
    <Field>
      <FieldLabel>Report period</FieldLabel>
      <DateRangePicker
        locale="en-GB"
        today={TODAY}
        max={TODAY}
        maxDays={366}
        presets={[
          { label: 'Last 7 days', lastDays: 7 },
          { label: 'Last 30 days', lastDays: 30 },
          { label: 'Last 90 days', lastDays: 90 },
          { label: 'This month', months: 0 },
          { label: 'Last month', months: -1 },
        ]}
        value={value}
        onValueChange={setValue}
      />
      <FieldDescription>
        {value ? `${value.start} to ${value.end}` : 'No period'}; at most 366 days.
      </FieldDescription>
    </Field>
  );
}

export default {
  id: 'date-range-picker',
  title: 'DateRangePicker',
  summary:
    'Start and end dates: typed or picked on a one- or two-month calendar with a live preview, presets and length limits.',
  sections: {
    purpose: {
      summary:
        'Picks a period such as a report range, a sprint, leave or a filter on created and due dates. Both ends can be typed in the locale’s format or picked on a calendar that previews the range as the pointer or focus moves; presets cover common periods. The value is `{ start, end }`, inclusive ISO dates. Import from `@rookcom/monarch-react/scheduling`.',
      useWhen: [
        'Report and analytics periods with presets (Last 7 days, Last month).',
        'Filtering lists by a created, due or updated date range.',
        'Booking a span of days (leave, sprints, campaigns) with a minimum or maximum length.',
      ],
      avoidWhen: [
        'Only a few fixed periods and no custom range: use SegmentedControl.',
        'Two unrelated dates (a start date and a separate review date): use two DatePickers.',
      ],
    },
    designPrinciples: {
      items: [
        'One control for one period: a single box with two inputs, one calendar, one error.',
        'The range is only committed when it is complete and valid, so filters never run on half a range.',
        'The calendar previews the range before the second click, and blocks days that would break the length limits.',
        'Two months where there is room, one on phones.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'The ref goes to the root `role="group"` element.',
        '`startName` and `endName` add hidden inputs for form submission.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        '`value`/`onValueChange` (controlled) or `defaultValue`; `onValueChange` receives a complete `{ start, end }` or `null`.',
        'Presets: `{ lastDays }` (ending today), `{ nextDays }` (starting today), `{ months }` (0 = this calendar month, -1 = last), or a fixed `{ range }`. Presets are clipped to `min` and `max` (with `max` today, “This month” is month to date) and disabled when what is left breaks the limits.',
        '`minDays`/`maxDays` count both ends: Monday to Sunday is 7 days.',
        '`onValidationChange` reports a date error, `end-before-start`, `too-short`, `too-long` or `null`.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'numberOfMonths',
          values: ['auto', '1', '2'],
          description: '`auto` shows two months from 640px wide, one below. Default `auto`.',
        },
        {
          prop: 'presets',
          values: ['none', 'list'],
          description: 'Quick picks beside the calendar (above it on phones).',
        },
        { prop: 'size', values: ['sm', 'md', 'lg'], description: 'Control height. Default `md`.' },
      ],
    },
    states: {
      items: [
        {
          name: 'Picking',
          attribute: 'data-preview on calendar cells',
          description:
            'After the first day: the start is marked and the range previews to the hovered or focused day.',
        },
        {
          name: 'Selected range',
          attribute: 'aria-selected="true", data-in-range',
          description: 'A tinted band with filled ends.',
        },
        {
          name: 'Invalid',
          attribute: 'aria-invalid="true", data-invalid',
          description:
            'A date error, end before start, or a range too short or too long; one message explains it.',
        },
        {
          name: 'Disabled',
          attribute: 'disabled, data-disabled',
          description: 'Both inputs and the calendar button.',
        },
      ],
    },
    interaction: {
      items: [
        'Typed dates are read on blur and Enter; the value changes once both ends are valid.',
        'In the calendar, the first pick sets the start, the second the end; picking an earlier day second swaps them. The calendar then closes and focus returns to the start field.',
        'After the first pick a polite announcement says to choose the end date.',
        'A preset sets both ends at once and closes the calendar; the current preset is pressed.',
        'Escape closes the calendar and discards a half-picked range.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Start field, end field, clear button, calendar button.' },
        { keys: '↓ (in a field)', action: 'Open the calendar.' },
        {
          keys: 'Arrows, Home/End, Page Up/Down',
          action: 'Move in the calendar; the preview follows focus.',
        },
        { keys: 'Enter / Space', action: 'Pick the start, then the end.' },
        { keys: 'Escape', action: 'Close the calendar.' },
      ],
    },
    accessibility: {
      role: 'group of two textboxes, with a button that opens a dialog containing one or two grids',
      name: 'The group is named by FieldLabel (or `aria-label`); the inputs are "<label> start date" and "<label> end date"; the button "Choose dates" or "Change dates, <start> to <end>".',
      items: [
        'Every day in the range is `aria-selected`; preview days are not, so only the real selection is announced as selected.',
        'Days blocked by the length limits are `aria-disabled` while picking.',
        'Presets are toggle buttons in a labelled group.',
        'One error message, linked to both inputs and announced politely.',
      ],
    },
    responsive: {
      items: [
        'The box fills its container; each input shrinks evenly.',
        'Two months side by side from 640px; one month below, so it fits a 320px screen.',
        'Below 640px, presets move above the calendar as wrapping chips.',
      ],
    },
    theme: {
      items: [
        'Input tokens for the box and MiniMonth tokens for the calendar; the range band is accent-subtle.',
        'Forced-colours mode outlines the range and the pressed preset; reduced motion removes transitions.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Report period with presets',
          render: () => <ReportRange />,
        },
        {
          title: 'Leave with a minimum length',
          stack: true,
          render: () => (
            <Field>
              <FieldLabel>Leave</FieldLabel>
              <DateRangePicker
                locale="en-GB"
                today={TODAY}
                min={TODAY}
                minDays={2}
                defaultValue={{ start: '2026-11-12', end: '2026-11-12' }}
              />
            </Field>
          ),
        },
        {
          title: 'Compact filter',
          render: () => (
            <DateRangePicker
              aria-label="Created"
              size="sm"
              locale="en-US"
              today={TODAY}
              defaultValue={{ start: '2026-11-01', end: '2026-11-30' }}
            />
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Send `start` and `end` as inclusive dates; if the API wants an exclusive end, add one day on the way out.',
        'Set `maxDays` when the backend limits the range, so the calendar blocks it up front.',
        'Offer presets for the periods people use most.',
      ],
      dont: [
        'Do not run a query on every keystroke; `onValueChange` only fires for a complete, valid range.',
        'Do not convert the dates to local-time `Date` objects to compute days; the strings are calendar dates.',
      ],
    },
    related: {
      items: [
        { id: 'date-picker', reason: 'A single date.' },
        { id: 'mini-month', reason: 'The calendar grid it uses.' },
        { id: 'filter-bar', reason: 'Combine the range with other filters.' },
        { id: 'segmented-control', reason: 'A few fixed periods without a custom range.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['form-field', 'popover', 'mini-month'],
    },
    testing: {
      unit: [
        'Labelled group with start and end inputs and a dialog button; axe clean.',
        'Typed ranges commit only when both ends are valid; end before start and length errors.',
        'Calendar picking with hover preview, closing and returning focus; keyboard picking with swap and focus-driven preview.',
        'Days outside the length limits disabled while picking.',
        'Presets (`lastDays`, `nextDays`, `months`, `range`), disabled when they break limits, pressed when current.',
        'min/max dates; two months with a range across them.',
        'Clear, controlled value, hidden inputs, disabled.',
      ],
      axe: true,
      ssr: true,
      browser: [
        'Visual baselines: /__gallery/date-range-picker and the open calendar (overlays/date-range-picker-*.png)',
        'Cross-browser axe: /components/date-range-picker',
      ],
    },
  },
} satisfies ComponentDocs;
