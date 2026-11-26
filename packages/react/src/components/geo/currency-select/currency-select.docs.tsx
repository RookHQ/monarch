import { useState } from 'react';
import { Field, FieldDescription, FieldLabel } from '@rookcom/monarch-react';
import { CurrencySelect } from '@rookcom/monarch-react/data';
import type { ComponentDocs } from '../../../internal/component-docs.js';

function ReportingCurrency() {
  const [currency, setCurrency] = useState<string | null>('EUR');
  return (
    <Field style={{ width: '100%', maxWidth: 320 }}>
      <FieldLabel>Reporting currency</FieldLabel>
      <CurrencySelect
        value={currency}
        onValueChange={setCurrency}
        priority={['EUR', 'GBP', 'USD']}
        placeholder="Search name, code or symbol"
      />
      <FieldDescription>Totals convert to {currency ?? 'the chosen currency'}.</FieldDescription>
    </Field>
  );
}

export default {
  id: 'currency-select',
  title: 'CurrencySelect',
  summary:
    'Searchable ISO 4217 currency picker with names from Intl.DisplayNames and symbols from Intl.NumberFormat.',
  sections: {
    purpose: {
      summary:
        'Chooses a currency and stores its ISO 4217 code. Names and symbols come from the browser’s `Intl`, so they follow the interface locale with no bundled tables. Import from `@rookcom/monarch-react/data`. Needed by: Rook People (settings, payroll, expenses `CurrencyPicker`, rates panel).',
      useWhen: [
        'A workspace, invoice, price list, payroll run or expense needs a currency.',
        'Only some currencies are supported (a payment provider’s list): pass `currencies`.',
      ],
      avoidWhen: [
        'Entering an amount: use CurrencyInput, which can sit next to this picker.',
        'Showing an amount: use Money.',
      ],
    },
    designPrinciples: {
      items: [
        'The value is the code; names and symbols are presentation and follow `locale`.',
        'Only currencies in circulation by default: no funds, metals, test codes or retired currencies.',
        'Symbols help search ("€", "₹") but never stand alone: "$" means many currencies.',
        'Same render on server and client: the default locale is `en`.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Built on Combobox and accepts its input props. Forwards its ref to the input.',
        'Helpers: `CURRENCY_CODES`, `getCurrencyName(code, locale)`, `getCurrencySymbol(code, locale, display)`, `listCurrencies({ locale, codes })`.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Without `Intl.DisplayNames` names fall back to the code.',
        'Migrating from Rook People `components/geo/CurrencySelect` and `components/expenses/CurrencyPicker`: `onChange(code)` → `onValueChange(code)`; the `CODE · Name` label is `display="code-name"`; `label`, `hint` and `error` → `Field`, `FieldLabel`, `FieldDescription`, `FieldError`; `required`, `disabled` and `id` pass through. The currency list derived from the countries API is no longer needed.',
        'Migrating from a shadcn/ui Popover + Command picker: `value`/`onValueChange` keep their names.',
      ],
    },
    variants: {
      items: [
        { prop: 'size', values: ['sm', 'md', 'lg'], description: 'Height, as Combobox.' },
        {
          prop: 'display',
          values: ['name', 'code', 'code-name'],
          description: 'What the input shows for the selection: "Euro", "EUR" or "EUR · Euro".',
        },
        {
          prop: 'showSymbol',
          values: ['true', 'false'],
          description: 'Symbol after the code in each option.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Sections',
          attribute: 'role="group"',
          description:
            '"Suggested" and "All currencies" while nothing is typed and `priority` is set.',
        },
        { name: 'Searching', description: 'A flat list, best matches first.' },
        { name: 'Empty', attribute: 'role="status"', description: '"No currencies match".' },
        { name: 'Selected / invalid / disabled', description: 'As Combobox.' },
      ],
    },
    interaction: {
      items: [
        'Typing filters by every word against the name, code, symbol and, in other locales, the English name.',
        'Matches whose name starts with the query come first.',
        'A value outside `currencies` is still shown with its name.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'ArrowDown / ArrowUp', action: 'Opens the list, then moves.' },
        {
          keys: 'PageDown / PageUp, Home / End',
          action: 'Moves ten currencies, or to the first or last.',
        },
        { keys: 'Enter', action: 'Selects the active currency.' },
        { keys: 'Escape', action: 'Closes and restores the selected label.' },
      ],
    },
    accessibility: {
      role: 'combobox, controlling a listbox',
      name: 'A FieldLabel or `aria-label`.',
      items: [
        'Each option reads its name, then the code and symbol.',
        'Inherits Combobox’s APG keyboard and `aria-activedescendant` support.',
      ],
    },
    responsive: {
      items: [
        'Fills its container; the list matches the field width.',
        'Options keep 44px touch targets on coarse pointers, as Combobox.',
      ],
    },
    theme: {
      items: ['Combobox tokens; the input uses tabular numbers so codes line up.'],
    },
    examples: {
      items: [
        { title: 'Suggested currencies first', stack: true, render: () => <ReportingCurrency /> },
        {
          title: 'Code and name, limited list, German',
          stack: true,
          render: () => (
            <div style={{ width: '100%', maxWidth: 260 }}>
              <CurrencySelect
                aria-label="Währung"
                locale="de"
                currencies={['CHF', 'EUR', 'GBP', 'USD']}
                display="code-name"
                defaultValue="CHF"
                size="sm"
              />
            </div>
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Store the ISO code and format amounts with Money or `Intl.NumberFormat`.',
        'Limit `currencies` to what the product can actually charge or pay in.',
      ],
      dont: [
        'Do not show a symbol without the code where currencies can mix: "$" is ambiguous.',
        'Do not hard-code currency names; they differ by locale.',
      ],
    },
    related: {
      items: [
        { id: 'combobox', reason: 'The searchable select it is built on.' },
        { id: 'currency-input', reason: 'Enters an amount in the chosen currency.' },
        { id: 'money', reason: 'Displays an amount.' },
        { id: 'country-select', reason: 'The same picker for countries.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['combobox'],
      notes: ['Uses only `Intl` for names and symbols; ships the list of active ISO 4217 codes.'],
    },
    testing: {
      unit: [
        'Active codes only, unique; names per locale; narrow and standard symbols; unknown codes.',
        'Sorted by name.',
        'Search by symbol and code, keyboard selection, `code-name` display, suggested section, empty message.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
