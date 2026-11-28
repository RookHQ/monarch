import { useState } from 'react';
import { CurrencyInput, Field, FieldDescription, FieldLabel } from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';
// The scenario of the primary example, shared with iOS and Android (CROSS-PLATFORM.md section 8).
import fixture from './currency-input.fixture.json';

function PriceDemo() {
  const [cents, setCents] = useState<number | null>(fixture.data.minorUnits);
  return (
    <Field style={{ width: '100%', maxWidth: 260 }}>
      <FieldLabel>{fixture.data.label}</FieldLabel>
      <CurrencyInput
        currency={fixture.data.currency}
        minorUnits
        value={cents}
        onValueChange={setCents}
      />
      <FieldDescription>Stored as {cents ?? 0} cents.</FieldDescription>
    </Field>
  );
}

export default {
  id: 'currency-input',
  title: 'CurrencyInput',
  summary: 'An amount of money in one currency, typed and shown in the user’s locale.',
  sections: {
    purpose: {
      summary:
        'Enters an amount in a given ISO 4217 currency with the right symbol, grouping and number of decimals, and stores either a major-unit number (19.99) or integer minor units (1999). Built on NumberInput with Money’s `formatMoney`. Needed by: rookdots, rookpeople, rookpartner, rookrecruit, rookatlas, rookcalendar and rookranker frontends, which format currency by hand around `type="number"` today. There is no shadcn/ui currency copy in the apps.',
      useWhen: [
        'Prices, budgets, salaries, invoice lines, refunds: any amount in one known currency.',
        'The API stores integer cents or paise: set `minorUnits`.',
      ],
      avoidWhen: [
        'Showing an amount that cannot be edited: use Money.',
        'A plain number or a percentage: use NumberInput.',
        'Choosing the currency itself: pair this with a Select or NativeSelect of codes and pass the choice as `currency`.',
      ],
    },
    designPrinciples: {
      items: [
        'The currency decides the decimals: JPY has none, USD two, KWD three. The committed amount is rounded to them, so it always equals what is shown.',
        'Minor units are converted with integer arithmetic (19.99 → 1999), never by multiplying floats.',
        'Formatting is Money’s, so an amount looks the same in an input and in a table.',
        'Negatives are opt-in (`allowNegative`), because most amounts cannot be below zero.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Accepts every NumberInput prop except `formatOptions`, `format` and `parse`; renders a NumberInput with the extra class `mn-currency-input`. The ref goes to the `<input>`.',
        'With `minorUnits`, `value`, `defaultValue`, `min`, `max`, `step`, `largeStep` and `onValueChange` are all in minor units.',
        'Helpers: `toMinorUnits(major, currency)` and `fromMinorUnits(minor, currency)`.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        '`step` defaults to one whole unit of the currency (100 in minor units for USD). Steps move to the next whole unit: from $0.30, ArrowUp goes to $1.00.',
        '`name` submits the major-unit number from a hidden input ("19.99"), never the formatted text.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'currencyDisplay',
          values: ['symbol', 'narrowSymbol', 'code', 'name'],
          description: '"$12.00", "$12.00", "USD 12.00" or "12.00 US dollars".',
        },
        {
          prop: 'steppers',
          values: ['false', 'true'],
          description:
            'Off by default: amounts are typed. Turn on for small counts of whole units.',
        },
        {
          prop: 'size',
          values: ['sm', 'md', 'lg'],
          description: 'Height from `--mn-control-*`.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Unreadable or out of range',
          attribute: 'aria-invalid="true", data-invalid',
          description:
            'Message under the field ("Enter ¥0 or more."); the text is kept and Escape restores the amount.',
        },
        {
          name: 'Disabled / read-only',
          attribute: 'data-disabled / data-readonly',
          description: 'As NumberInput.',
        },
      ],
    },
    interaction: {
      items: [
        'Type an amount with or without the symbol ("1234.5", "$1,234.50"); on blur or Enter it is shown as "$1,234.50".',
        'Pasting a formatted amount from a spreadsheet or invoice works: symbols, codes and grouping are ignored.',
        'Arrow keys step by one whole unit; PageUp/PageDown by ten.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'ArrowUp / ArrowDown', action: 'Add or remove `step` (one unit by default).' },
        { keys: 'PageUp / PageDown, Shift+Arrow', action: 'Add or remove `largeStep`.' },
        { keys: 'Home / End', action: 'Go to `min` / `max` when set.' },
        { keys: 'Enter / Escape', action: 'Commit the text / restore the amount.' },
      ],
    },
    accessibility: {
      role: 'spinbutton',
      name: 'FieldLabel inside a Field, or `aria-label`.',
      items: [
        '`aria-valuetext` is the formatted amount ("$1,234.50"), so screen readers say the currency.',
        '`aria-valuenow` is the major-unit number.',
        'Errors are `role="alert"` and joined to `aria-describedby`.',
        '`inputMode="decimal"` (or `numeric` for zero-decimal currencies) brings up a number keyboard.',
      ],
    },
    responsive: {
      items: [
        'Fills its container at every width, including 390px; long amounts keep their grouping and the box does not overflow.',
        'On coarse pointers the control is at least 44px tall.',
      ],
    },
    theme: {
      items: [
        'Inherits NumberInput’s Input tokens, so it matches Input and Select in every theme.',
        'Forced colours: system field colours and a 2px border when invalid.',
      ],
    },
    examples: {
      items: [
        { title: 'Minor units (cents)', stack: true, render: () => <PriceDemo /> },
        {
          title: 'Euro in German',
          render: () => (
            <CurrencyInput
              aria-label="Betrag"
              currency="EUR"
              locale="de-DE"
              defaultValue={1234.5}
            />
          ),
        },
        {
          title: 'Zero-decimal currency',
          render: () => <CurrencyInput aria-label="Fee" currency="JPY" defaultValue={1500} />,
        },
        {
          title: 'Currency code, negatives allowed',
          render: () => (
            <CurrencyInput
              aria-label="Adjustment"
              currency="INR"
              locale="en-IN"
              currencyDisplay="code"
              allowNegative
              defaultValue={-2500}
            />
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Store integer minor units when the API does, with `minorUnits`.',
        'Show the currency code (`currencyDisplay="code"`) when several currencies share a symbol on the page.',
        'Validate business limits with `min` and `max` so the message names the amount.',
      ],
      dont: [
        'Do not convert between currencies in the field; it edits one currency.',
        'Do not multiply by 100 yourself: use `minorUnits` or `toMinorUnits`.',
        'Do not hard-code "$" next to the input; the currency formatting adds the right symbol.',
      ],
    },
    related: {
      items: [
        { id: 'number-input', reason: 'The underlying field, for plain numbers and percentages.' },
        { id: 'money', reason: 'Shows an amount read-only with the same formatting.' },
        { id: 'form-field', reason: 'Label, description and error wiring.' },
      ],
    },
    dependencies: { packages: ['react'], components: ['money', 'number-input'] },
    testing: {
      unit: [
        'toMinorUnits/fromMinorUnits are exact and use the currency decimals (USD, JPY, KWD).',
        'Formats with the currency, labelled by the Field, aria-valuetext is the formatted amount, no steppers by default.',
        'Reads and writes in de-DE.',
        'Minor units in and out, rounding to the currency decimals, stepping.',
        'JPY has zero decimals; negatives are blocked unless `allowNegative`.',
        '`currencyDisplay="code"` and steppers with a custom step.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
