import { useState } from 'react';
import {
  Field,
  FieldDescription,
  FieldError,
  FieldLabel,
  RatingPicker,
  type RatingOption,
} from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const reviewScale: RatingOption[] = [
  { value: 1, label: 'Below' },
  { value: 2, label: 'Partly meets' },
  { value: 3, label: 'Meets' },
  { value: 4, label: 'Exceeds' },
  { value: 5, label: 'Outstanding' },
];

function ReviewQuestion() {
  const [value, setValue] = useState<number | null>(null);
  return (
    <Field invalid={value === null} required>
      <FieldLabel>Overall rating</FieldLabel>
      <RatingPicker options={reviewScale} value={value} onValueChange={setValue} clearable />
      <FieldDescription>Pick the statement closest to the last six months.</FieldDescription>
      <FieldError>Choose a rating to submit the review.</FieldError>
    </Field>
  );
}

export default {
  id: 'rating-picker',
  title: 'RatingPicker',
  summary:
    'Pick one value on a rating scale, with a label per value. A keyboard-ready radio group.',
  sections: {
    purpose: {
      summary:
        'Collects one rating on a fixed scale (1–5 by default, or any scale with a label per value) for reviews, surveys and feedback forms. It is a real radio group, so it is announced and operated like one. Needed by: People (performance reviews and competency ratings).',
      useWhen: [
        'A review or survey question answered on a numbered scale.',
        'Each point on the scale has a meaning people should see ("Meets", "Exceeds").',
        'A quick star rating for feedback (`variant="stars"`).',
      ],
      avoidWhen: [
        'Showing a rating someone else gave, with no input: render the number or stars as text.',
        'Choosing between named options that are not a scale: use SegmentedControl or a Select.',
        'Continuous values (0–100): use a slider or number input.',
      ],
    },
    designPrinciples: {
      items: [
        'One radio group, one tab stop: arrow keys move and select, exactly as native radio buttons do.',
        'Meaning beside the number: each tile shows its label, and each radio’s accessible name includes it.',
        'Wraps instead of scrolling, so a 5- or 10-point scale fits a 390px screen.',
        'Clearing is opt-in (`clearable`) because ordinary radio groups cannot be unset.',
        'Fits a Field like any other control: label, help text and error are wired automatically.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        '`RatingPicker` forwards its ref to the `role="radiogroup"` element; other div props pass through.',
        'Controlled with `value` + `onValueChange`, or uncontrolled with `defaultValue`. `null` means not rated.',
        '`RATING_PICKER_MESSAGES` holds the English defaults for `messages`.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        '`options` is `{ value, label?, textLabel? }[]`. Use `textLabel` when `label` is not plain text, so star names stay readable.',
        'Without `options`, the scale is every whole number from `min` (default 1) to `max` (default 5).',
        'Migrating from the app copy (People `components/performance/RatingPicker`): `scale` → `options` (same `{ value, label }` shape), `onChange` → `onValueChange`, `label` → `aria-label` (or put it in a Field with a FieldLabel), `disabled` is unchanged. The old copy cleared the rating when the selected tile was clicked again; pass `clearable` to keep that.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'variant',
          values: ['scale', 'stars'],
          description:
            '`scale` (default) shows tiles with the number and its label; `stars` shows a row of stars with the selected label beside them. Exposed as `data-variant`.',
        },
        {
          prop: 'size',
          values: ['sm', 'md'],
          description: 'Tile or star size. Default `md`. Exposed as `data-size`.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Checked',
          attribute: 'data-state="checked"',
          description: 'The selected value (`aria-checked="true"`), drawn with the accent tint.',
        },
        {
          name: 'Filled',
          attribute: 'data-filled',
          description: 'In the stars variant, every star up to the selected value.',
        },
        {
          name: 'Invalid',
          attribute: 'data-invalid',
          description: 'From `invalid` or a surrounding invalid Field; sets `aria-invalid`.',
        },
        {
          name: 'Disabled',
          attribute: 'data-disabled',
          description: 'Every radio is disabled and skipped by Tab.',
        },
        {
          name: 'Read-only',
          attribute: 'data-readonly',
          description:
            'Shows the value and stays focusable, but clicks and keys do not change it (`aria-readonly`).',
        },
      ],
    },
    interaction: {
      items: [
        'Clicking or tapping a value selects it.',
        'With `clearable`, activating the selected value again clears the rating.',
        'With `name`, a hidden input carries the value (empty when not rated) for native form submission.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Tab',
          action: 'Moves into the group, onto the selected value (or the first one).',
        },
        {
          keys: 'Arrow Right / Arrow Down',
          action:
            'Selects the next value, wrapping to the first. Reversed in right-to-left layouts.',
        },
        {
          keys: 'Arrow Left / Arrow Up',
          action: 'Selects the previous value, wrapping to the last.',
        },
        { keys: 'Home / End', action: 'Selects the first or last value.' },
        {
          keys: 'Space / Enter',
          action: 'Selects the focused value (or clears it when `clearable`).',
        },
        { keys: 'Delete / Backspace', action: 'Clears the rating when `clearable`.' },
      ],
      notes: ['When read-only, the arrow keys move focus without changing the value.'],
    },
    accessibility: {
      role: 'radiogroup, with one radio per value',
      name: '`aria-label`, `aria-labelledby`, or the FieldLabel of a surrounding Field.',
      items: [
        'Roving tab index: one tab stop for the whole scale (WCAG 2.1.1).',
        'Each radio’s name is its number and label ("4 Exceeds"); stars are named "4 of 5, Exceeds" through `messages.starLabel`.',
        'Inside a Field, `aria-describedby`, `aria-invalid` and `aria-required` are set on the group.',
        'The selected state is not shown by colour alone: tiles get a thicker border, stars are filled.',
        'Touch targets are at least 44px on coarse pointers.',
      ],
    },
    responsive: {
      items: [
        'Tiles wrap onto more lines on narrow screens; nothing scrolls sideways at 390px.',
        'Long labels wrap inside their tile.',
      ],
    },
    theme: {
      items: [
        'Checked tiles use the accent subtle and border tokens; stars use the warning strong token.',
        'Forced colours: the checked tile uses Highlight, filled stars use Highlight, focus uses Highlight.',
        'Reduced motion removes the colour transitions.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Labelled scale',
          render: () => (
            <RatingPicker aria-label="Overall rating" options={reviewScale} defaultValue={3} />
          ),
        },
        {
          title: 'In a Field, required and clearable',
          stack: true,
          render: () => <ReviewQuestion />,
        },
        {
          title: 'Numeric 0–10 scale',
          render: () => (
            <RatingPicker
              aria-label="How likely are you to recommend us?"
              min={0}
              max={10}
              size="sm"
            />
          ),
        },
        {
          title: 'Stars',
          render: () => (
            <RatingPicker
              aria-label="Rate this article"
              variant="stars"
              defaultValue={4}
              options={[
                { value: 1, label: 'Poor' },
                { value: 2, label: 'Fair' },
                { value: 3, label: 'Good' },
                { value: 4, label: 'Very good' },
                { value: 5, label: 'Excellent' },
              ]}
            />
          ),
        },
        {
          title: 'Read-only',
          render: () => (
            <RatingPicker aria-label="Manager rating" options={reviewScale} value={4} readOnly />
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Give every value a short label when the numbers alone are ambiguous.',
        'Use `clearable` for optional questions so people can take an answer back.',
        'Put it in a Field so the question, help and error are announced with it.',
      ],
      dont: [
        'Do not use stars for a review scale where each value has a defined meaning; show the labels.',
        'Do not use it to display a rating that cannot be changed; use text or `readOnly` only inside a form.',
        'Do not build scales longer than 11 values.',
      ],
    },
    related: {
      items: [
        {
          id: 'segmented-control',
          reason: 'Choosing between a few named options that are not a scale.',
        },
        { id: 'form-field', reason: 'Supplies the label, description and error for the group.' },
        { id: 'select', reason: 'A long list of values where a scale would not fit.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['form-field'],
      notes: [
        'Uses `--mn-color-accent-subtle` and `--mn-color-accent-border` for the checked tile and `--mn-color-warning-strong` for stars.',
      ],
    },
    testing: {
      unit: [
        'Named radio group with one radio per value; labels are part of each radio’s name; no axe violations.',
        'Default 1–5 scale and custom `min`/`max`.',
        'Click selects and reports; clicking the selection again is ignored unless `clearable`.',
        'Arrow keys move focus and selection with wrapping; Home and End; Tab leaves in one step.',
        '`clearable` clears on re-activation and on Delete.',
        'Controlled value; disabled and read-only never change the value.',
        'Stars variant: "n of max" names, filled stars, selected caption, translated names.',
        'Inside a Field: name, description, `aria-invalid`, `aria-required`.',
        'Hidden input submits the value; ref and props are forwarded; renders on the server.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
