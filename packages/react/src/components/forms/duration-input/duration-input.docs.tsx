import { useState } from 'react';
import { Field, FieldDescription, FieldLabel } from '@rookcom/monarch-react';
import { DurationInput } from '@rookcom/monarch-react/scheduling';
import type { ComponentDocs } from '../../../internal/component-docs.js';

function EstimateDemo() {
  const [minutes, setMinutes] = useState<number | null>(90);
  return (
    <Field style={{ width: '100%', maxWidth: 240 }}>
      <FieldLabel>Estimate</FieldLabel>
      <DurationInput value={minutes} onValueChange={setMinutes} />
      <FieldDescription>Type 1h 30m, 90m or 1.5h. Arrow keys step by 15 minutes.</FieldDescription>
    </Field>
  );
}

export default {
  id: 'duration-input',
  title: 'DurationInput',
  summary:
    'Free-text duration field ("1h 30m", "90m", "1.5h") with a minutes value and arrow-key steps.',
  sections: {
    purpose: {
      summary:
        'Enters a length of time the way people say it and stores whole minutes: estimates, time logged, meeting lengths, SLA targets. Import from `@rookcom/monarch-react/scheduling`.',
      useWhen: [
        'A duration in minutes or hours: estimates, time tracking, buffers, response targets.',
      ],
      avoidWhen: [
        'A time of day: use a time picker.',
        'A date range: use DateRangePicker.',
        'A handful of fixed lengths (15, 30, 60 minutes): use SegmentedControl or Select.',
      ],
    },
    designPrinciples: {
      items: [
        'Accept what people type: "90", "90m", "1h 30m", "1h30", "1.5h", "1,5h", "1:30", "2 hours", "1d 4h".',
        'Show it back one way ("1h 30m") so the value is unambiguous.',
        'Never save a guess: unreadable or out-of-range text stays, is flagged and explained.',
        'The value is always whole minutes; formatting and parsing are replaceable for other conventions.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Renders `.mn-duration-input` wrapping the input and its message. The ref and native input props go to the input; `className` and `style` go to the wrapper.',
        'Helpers: `parseDuration(text, { minutesPerDay })` returns minutes, `null` (empty) or `NaN`; `formatDuration(minutes, { largestUnit, minutesPerDay })`.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: ['`onValueChange` fires on blur, Enter and arrow steps, never on every keystroke.'],
    },
    variants: {
      items: [
        {
          prop: 'size',
          values: ['sm', 'md', 'lg'],
          description: 'Input height from `--mn-control-*`.',
        },
        {
          prop: 'largestUnit',
          values: ['h', 'd'],
          description: '"26h" or "1d 2h". Use `minutesPerDay={480}` for 8-hour work days.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Unreadable',
          attribute: 'aria-invalid="true"',
          description:
            '"Use a duration like 1h 30m, 90m or 1.5h." under the field; the text is kept.',
        },
        {
          name: 'Out of range',
          attribute: 'aria-invalid="true"',
          description: '"Enter between 15m and 2h." from `min` and `max`.',
        },
        { name: 'Empty', description: 'The value is `null`; `aria-valuenow` is removed.' },
        {
          name: 'Disabled / read-only',
          attribute: 'disabled, readonly',
          description: 'Keys do nothing.',
        },
      ],
    },
    interaction: {
      items: [
        'Typing never changes the value; blur or Enter reads the text, saves minutes and reformats it.',
        'Clearing the text saves `null`.',
        'Escape restores the last saved value and clears the message.',
        'Arrow steps snap to the step grid (50m + 15 → 1h) and clamp to `min` and `max`.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'ArrowUp / ArrowDown', action: 'Adds or removes `step` minutes (default 15).' },
        { keys: 'PageUp / PageDown', action: 'Moves to the next or previous whole hour.' },
        { keys: 'Home / End', action: 'Sets `min` / `max` (End only with `max`).' },
        { keys: 'Enter', action: 'Reads and saves the text.' },
        { keys: 'Escape', action: 'Restores the saved value.' },
      ],
    },
    accessibility: {
      role: 'spinbutton',
      name: 'A FieldLabel or `aria-label`.',
      items: [
        '`aria-valuenow` is the minutes, `aria-valuetext` a spoken form ("1 hour 30 minutes"), `aria-valuemin` / `aria-valuemax` the limits.',
        'The error message is linked with `aria-describedby` and announced as an alert.',
        'Field descriptions and errors are wired as for Input.',
      ],
    },
    responsive: {
      items: [
        'Fills its container; set a width on the Field. Numbers are tabular so values do not jitter.',
      ],
    },
    theme: {
      items: ['Input tokens; the message uses `--mn-color-danger-strong` in every theme.'],
    },
    examples: {
      items: [
        {
          title: 'Estimate',
          stack: true,
          render: () => <EstimateDemo />,
        },
        {
          title: 'Limits, days and small',
          stack: true,
          render: () => (
            <div style={{ display: 'grid', gap: 12, width: '100%', maxWidth: 240 }}>
              <DurationInput
                aria-label="Buffer"
                size="sm"
                min={5}
                max={120}
                step={5}
                defaultValue={10}
              />
              <DurationInput aria-label="Response target" largestUnit="d" defaultValue={1680} />
            </div>
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Say in a FieldDescription which formats work when the field is new to people.',
        'Set `min` and `max` when the product has limits, so the message can say them.',
      ],
      dont: [
        'Do not use a number input with a separate unit select for durations people type.',
        'Do not save on every keystroke; wait for `onValueChange`.',
      ],
    },
    related: {
      items: [
        { id: 'text-field', reason: 'Plain text and number input.' },
        { id: 'form-field', reason: 'Label, help and error wiring.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['form-field'],
      notes: ['Uses the `.mn-input` styles from text-field.'],
    },
    testing: {
      unit: [
        'Parses 13 input shapes, rejects unreadable text, honours minutesPerDay, formats h/m and days.',
        'Commits on blur with spoken value; unreadable text is kept, flagged, explained and Escape restores.',
        'Range message; arrow, page, Home and End steps with snapping and clamping.',
        'Clearing to null, controlled updates, custom format and read-only.',
      ],
      axe: true,
      ssr: true,
      browser: [
        'Visual baselines: /__gallery/duration-input',
        'Cross-browser axe: /components/duration-input',
      ],
    },
  },
} satisfies ComponentDocs;
