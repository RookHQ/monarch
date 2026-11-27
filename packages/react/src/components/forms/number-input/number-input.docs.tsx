import { useState } from 'react';
import { Field, FieldDescription, FieldLabel, NumberInput } from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';
// The scenario of the primary example, shared with iOS and Android (CROSS-PLATFORM.md section 8).
import fixture from './number-input.fixture.json';

function SeatsDemo() {
  const [seats, setSeats] = useState<number | null>(fixture.data.value);
  return (
    <Field style={{ width: '100%', maxWidth: 220 }}>
      <FieldLabel>{fixture.data.label}</FieldLabel>
      <NumberInput
        value={seats}
        onValueChange={setSeats}
        min={fixture.data.min}
        max={fixture.data.max}
      />
      <FieldDescription>{fixture.data.description}</FieldDescription>
    </Field>
  );
}

export default {
  id: 'number-input',
  title: 'NumberInput',
  summary: 'Locale-aware number field with steppers, arrow-key steps and no float drift.',
  sections: {
    purpose: {
      summary:
        'Enters a number the way the user writes it ("1,234.5" in English, "1.234,5" in German), steps it with keys or − / + buttons, and stores a plain JavaScript number. Replaces `<input type="number">`, which ignores the locale, accepts "e", changes value on scroll and drifts (0.1 + 0.2). Needed by: rookdots, rookpeople, rookrecruit, rookstride, rookcalendar, rooktickets, rookid, rookchat, rookgtm, rookpulse and rookranker frontends, which use `type="number"` today. There is no shadcn/ui number copy in the apps.',
      useWhen: [
        'Quantities, counts, limits, percentages, weights: any value that is a number and can be stepped.',
        'Numbers shown in the user’s locale with grouping ("12,500").',
        'A percentage stored as a ratio: `formatOptions={{ style: "percent" }}` shows 0.15 as 15%.',
      ],
      avoidWhen: [
        'Money: use CurrencyInput, which adds the currency symbol, decimals and minor units.',
        'Identifiers made of digits (postcodes, card or account numbers, codes): use Input with `inputMode="numeric"`; they are not numbers.',
        'Picking a value on a continuous scale where the exact number matters less than the position: use Slider.',
        'A length of time: use DurationInput.',
      ],
    },
    designPrinciples: {
      items: [
        'The value is a number or `null`; the text is presentation. Parsing and formatting follow `locale`.',
        'Commit, do not stream: `onValueChange` fires on blur, Enter, a key step or a button, never per keystroke.',
        'Never save a guess: unreadable or out-of-range text stays in the box, is flagged and explained, and Escape restores the last value.',
        'The committed value is rounded to what the formatter shows, so the value is always what the user sees.',
        'Stepping is exact: values move on the grid `min + k × step` using integer arithmetic, so 0.1 steps reach 1, not 0.9999999.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Renders `.mn-number-input` around a bordered `.mn-number-input__control` (− button, input, + button) and an error message. The ref and native input props go to the `<input>`; `className` and `style` go to the wrapper.',
        'With `name`, a hidden input submits the raw number ("1234.5"); the visible text input has no name, so forms never receive the formatted text.',
        'Helpers: `parseNumber(text, locale, { percent })` returns a number, `null` (empty) or `NaN`; `stepValue(value, step, delta, base)` steps on a grid without drift; `roundTo(n, places)` and `decimalPlaces(n)` round decimals exactly.',
        '`NUMBER_INPUT_MESSAGES` holds the English defaults for `messages`.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        '`step` and `largeStep` move to the next point on their own grid: from 0.2, PageUp with a large step of 1 goes to 1, not 1.2.',
        '`format` and `parse` replace the display and reading entirely; use them for units the locale cannot express.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'size',
          values: ['sm', 'md', 'lg'],
          description: 'Height from `--mn-control-*`, matching Input. Exposed as `data-size`.',
        },
        {
          prop: 'steppers',
          values: ['true', 'false'],
          description:
            '− and + buttons either side of the value (default). Without them the field looks like an Input and keys still step. Exposed as `data-steppers`.',
        },
        {
          prop: 'formatOptions',
          values: ['Intl.NumberFormatOptions'],
          description:
            'Any `Intl.NumberFormat` option: `style: "percent"`, `minimumFractionDigits`, `unit` styles, `signDisplay`.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Unreadable',
          attribute: 'aria-invalid="true", data-invalid',
          description: '"Enter a number." under the field; the text is kept.',
        },
        {
          name: 'Out of range',
          attribute: 'aria-invalid="true", data-invalid',
          description: '"Enter a number from 1 to 50." built from `min` and `max`, formatted.',
        },
        {
          name: 'At a bound',
          attribute: 'disabled on the − or + button',
          description: 'The button that would leave the range is disabled.',
        },
        {
          name: 'Disabled',
          attribute: 'data-disabled, disabled on the input and buttons',
          description: 'Muted surface; nothing responds.',
        },
        {
          name: 'Read-only',
          attribute: 'data-readonly, readonly on the input',
          description: 'The value can be selected and copied; keys and buttons do not change it.',
        },
      ],
    },
    interaction: {
      items: [
        'Typing changes only the text. Blur or Enter reads it, checks the range and commits.',
        'Pressing − or + steps immediately and keeps focus in the input, so the keyboard keeps working.',
        'From an empty field, the first step starts at `min` (or 0).',
        'Escape while the text differs from the value restores the value and clears the error.',
        'The wheel does not change the value (unlike `type="number"`).',
      ],
    },
    keyboard: {
      items: [
        { keys: 'ArrowUp / ArrowDown', action: 'Step by `step` and commit.' },
        { keys: 'Shift+ArrowUp / Shift+ArrowDown', action: 'Step by `largeStep`.' },
        { keys: 'PageUp / PageDown', action: 'Step by `largeStep` (default `step × 10`).' },
        { keys: 'Home / End', action: 'Go to `min` / `max` when they are set.' },
        { keys: 'Enter', action: 'Commit the typed text (the form still submits).' },
        { keys: 'Escape', action: 'Restore the last value.' },
        { keys: 'Tab', action: 'Moves past the field; the − and + buttons are not tab stops.' },
      ],
    },
    accessibility: {
      role: 'spinbutton',
      name: 'FieldLabel inside a Field, or `aria-label`.',
      items: [
        'Follows the WAI-ARIA spinbutton pattern: `aria-valuenow`, `aria-valuemin`, `aria-valuemax`, and `aria-valuetext` with the formatted value ("15%", "1.234,5").',
        'The − and + buttons have names ("Decrease", "Increase") and `tabindex="-1"`, as the pattern recommends, because the arrow keys do the same.',
        'Errors are `role="alert"` and joined to `aria-describedby` while shown.',
        '`inputMode="decimal"` (or `numeric` for whole non-negative numbers) brings up the right phone keyboard.',
        'Inside a Field it gets id, description, required and invalid wiring through `useFieldControl`.',
      ],
    },
    responsive: {
      items: [
        'Fills its container at every width; at 390px the buttons stay visible and the text shrinks first.',
        'On coarse pointers the control is at least 44px tall and each button at least 44px wide.',
      ],
    },
    theme: {
      items: [
        'Uses the Input tokens (`--mn-color-border-input`, `--mn-color-bg-surface`, `--mn-color-focus-ring`), so it matches Input in every theme.',
        'One focus ring is drawn around the whole control, not the inner input.',
        'Forced colours: system Field and ButtonText colours, a 2px border when invalid, and a Highlight outline on focus.',
      ],
    },
    examples: {
      items: [
        { title: 'In a Field with limits', stack: true, render: () => <SeatsDemo /> },
        {
          title: 'Decimal steps without drift',
          render: () => (
            <NumberInput aria-label="Ratio" defaultValue={0.7} step={0.1} min={0} max={1} />
          ),
        },
        {
          title: 'German locale',
          render: () => <NumberInput aria-label="Menge" locale="de-DE" defaultValue={12500.75} />,
        },
        {
          title: 'Percentage',
          render: () => (
            <NumberInput
              aria-label="Discount"
              formatOptions={{ style: 'percent' }}
              defaultValue={0.15}
              min={0}
              max={1}
            />
          ),
        },
        {
          title: 'Without steppers, small',
          render: () => (
            <NumberInput aria-label="Limit" size="sm" steppers={false} defaultValue={250} />
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Set `min` and `max` whenever the domain has them: they drive the error, the buttons and Home/End.',
        'Pass `locale` from the user’s settings when it differs from the browser.',
        'Store the number; let the component format it.',
      ],
      dont: [
        'Do not use it for codes and identifiers made of digits.',
        'Do not reformat the value in `onValueChange`; change `formatOptions` instead.',
        'Do not use `type="number"` beside it: mixed inputs read decimals differently.',
      ],
    },
    related: {
      items: [
        { id: 'currency-input', reason: 'NumberInput for money, with currency formatting.' },
        { id: 'slider', reason: 'Choose a number by position on a track.' },
        { id: 'duration-input', reason: 'A length of time typed as "1h 30m".' },
        { id: 'text-field', reason: 'Plain text, and digit codes that are not numbers.' },
        { id: 'form-field', reason: 'Label, description and error wiring.' },
      ],
    },
    dependencies: { packages: ['react'], components: ['form-field'] },
    testing: {
      unit: [
        'parseNumber reads en, de, fr (narrow no-break space), Arabic-Indic digits, accounting brackets, currency affixes and percent; rejects unreadable text.',
        'stepValue and roundTo do not drift (ten 0.1 steps reach exactly 1).',
        'Commits on blur only and formats in the locale; aria-valuenow and aria-valuetext.',
        'ArrowUp/Down, Shift+Arrow, PageUp/Down, Home/End step and clamp.',
        'Steppers are named, out of the tab order, keep focus in the input and disable at the bounds.',
        'Unreadable and out-of-range text is flagged with an alert and Escape restores.',
        'Percent formatting and rounding to the shown precision.',
        'Controlled value, clearing to null, hidden input submits the raw number.',
        'Read-only ignores keys; `steppers={false}`; translated messages.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
