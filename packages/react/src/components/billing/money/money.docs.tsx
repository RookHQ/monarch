import { Money } from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';

export default {
  id: 'money',
  title: 'Money',
  summary: 'An amount of one currency, formatted by Intl.NumberFormat in tabular figures.',
  sections: {
    purpose: {
      summary:
        'Displays a monetary amount the same way everywhere: localised, in its own currency, from minor units without floating-point loss, with an accessible full value when shortened.',
      useWhen: [
        'Prices, invoice lines, payroll figures, renewal costs and balances.',
        'Amounts stored as integer minor units (cents, paise) next to an ISO 4217 currency code.',
      ],
      avoidWhen: [
        'Entering an amount: use a currency input.',
        'Adding amounts in different currencies: list them per currency instead; Money never converts.',
      ],
    },
    designPrinciples: {
      items: [
        'One amount, one currency. Conversion and summing across currencies stay in the product.',
        'Tabular figures and no wrapping, so columns of amounts line up.',
        'The sign carries the meaning; colour (`toneNegative`) only reinforces it.',
        'Shortened amounts never hide the real value: compact notation keeps the full amount for screen readers and as a hover title.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'The ref and native attributes go to the root `<span>`.',
        '`formatMoney(amount, currency, options)` returns the same string for exports, charts and `aria-label`s; `currencyDigits(code)` gives the minor-unit digits (USD 2, JPY 0, KWD 3).',
        'Decimal strings (`"1234.50"`) and bigints are formatted exactly; they need a runtime with Intl.NumberFormat v3 (all current evergreen browsers and Node 20+).',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'With `minorUnits`, `amount` must be an integer; a fractional string throws a RangeError.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'notation',
          values: ['standard', 'compact'],
          description:
            '`compact` writes $1.3M and adds the full value for assistive technology (`data-notation`).',
        },
        {
          prop: 'signDisplay',
          values: ['auto', 'always', 'exceptZero', 'never'],
          description: '`exceptZero` writes +$42.00 for gains.',
        },
        {
          prop: 'currencyDisplay',
          values: ['symbol', 'narrowSymbol', 'code', 'name'],
          description: '$, $, USD or “US dollars”.',
        },
        {
          prop: 'currencySign',
          values: ['standard', 'accounting'],
          description: '`accounting` writes negatives in parentheses where the locale does.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Negative',
          attribute: 'data-negative',
          description:
            'Always set for amounts below zero; `toneNegative` adds `data-tone="danger"`.',
        },
        {
          name: 'Empty',
          attribute: 'data-empty',
          description:
            '`amount` null or undefined: an em dash read as “No amount”, or `emptyLabel`.',
        },
        {
          name: 'Compact',
          attribute: 'data-notation="compact"',
          description: 'Short visible text, full amount in visually hidden text and `title`.',
        },
      ],
    },
    interaction: {
      items: [
        'None. In compact notation the native `title` shows the full amount on hover; screen readers read the full amount directly.',
      ],
    },
    keyboard: { items: [], notes: ['Not interactive, so there is no keyboard behaviour.'] },
    accessibility: {
      role: 'none (inline text)',
      name: 'The formatted amount is the text itself.',
      items: [
        'Compact amounts render the short form `aria-hidden` and the full form in visually hidden text, so “$1.3M” is read as “$1,250,000.00”.',
        'The empty dash is hidden from screen readers and replaced by “No amount”.',
        'Negative colour is never the only cue: the minus sign or parentheses are always present.',
      ],
    },
    responsive: {
      items: [
        'Amounts never wrap (`white-space: nowrap`). Use `notation="compact"` where a column is narrow on phones.',
      ],
    },
    theme: {
      items: [
        'Inherits the surrounding text colour; `toneNegative` uses `--mn-color-danger-strong`, which meets AA in every theme.',
        'The empty state uses `--mn-color-text-muted`.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Locales and minor units',
          stack: true,
          render: () => (
            <>
              <Money amount={129900} currency="USD" minorUnits locale="en-US" />
              <Money amount={129900} currency="EUR" minorUnits locale="de-DE" />
              <Money amount={12990000} currency="INR" minorUnits locale="en-IN" />
              <Money amount={4500} currency="JPY" minorUnits locale="ja-JP" />
            </>
          ),
          code: `<Money amount={129900} currency="USD" minorUnits locale="en-US" />   // $1,299.00
<Money amount={129900} currency="EUR" minorUnits locale="de-DE" />   // 1.299,00 €
<Money amount={12990000} currency="INR" minorUnits locale="en-IN" /> // ₹1,29,900.00
<Money amount={4500} currency="JPY" minorUnits locale="ja-JP" />     // ￥4,500`,
        },
        {
          title: 'Compact, signs and negatives',
          stack: true,
          render: () => (
            <>
              <Money amount={2_480_000} currency="USD" locale="en-US" notation="compact" />
              <Money amount={1250} currency="USD" locale="en-US" signDisplay="exceptZero" />
              <Money amount={-1250} currency="USD" locale="en-US" toneNegative />
              <Money amount={-1250} currency="USD" locale="en-US" currencySign="accounting" />
              <Money amount={null} currency="USD" />
            </>
          ),
          code: `<Money amount={2_480_000} currency="USD" notation="compact" /> // $2.5M, read in full
<Money amount={1250} currency="USD" signDisplay="exceptZero" />
<Money amount={-1250} currency="USD" toneNegative />
<Money amount={-1250} currency="USD" currencySign="accounting" />
<Money amount={null} currency="USD" />`,
        },
      ],
    },
    doDont: {
      do: [
        'Store and pass integer minor units with `minorUnits`; it avoids floating-point rounding.',
        'Pass the viewer’s `locale` so separators and symbol placement match their region.',
        'Show one line per currency when totals span currencies.',
      ],
      dont: [
        'Do not add amounts of different currencies together before displaying them.',
        'Do not rely on red alone to show a negative amount.',
        'Do not shorten amounts with `compact` where the exact figure is the point, such as an invoice total.',
      ],
    },
    related: {
      items: [
        { id: 'stat-card', reason: 'Headline figures; pass a Money as the value.' },
        { id: 'description-list', reason: 'Label and amount pairs in a summary.' },
        { id: 'data-table', reason: 'Right-aligned amount columns.' },
        { id: 'delta-indicator', reason: 'Planned up/down change next to an amount.' },
      ],
    },
    dependencies: { packages: ['react'], components: ['visually-hidden'] },
    testing: {
      unit: [
        'Formats in en-US, de-DE and en-IN.',
        'Minor units use the currency’s digits (USD, JPY, KWD), including small and negative values.',
        'Bigint and decimal-string amounts keep full precision; fractional minor units throw.',
        'Compact notation, sign display, accounting sign and currency display.',
        'Compact amounts expose the full value to assistive technology and as a title, without duplicating equal text.',
        'Negative marking and opt-in danger tone; empty state and `emptyLabel`.',
        'Forwards the ref and keeps a consumer `title`.',
      ],
      axe: true,
      ssr: true,
      browser: ['Visual baselines: /__gallery', 'Cross-browser axe: /components/money'],
    },
  },
} satisfies ComponentDocs;
