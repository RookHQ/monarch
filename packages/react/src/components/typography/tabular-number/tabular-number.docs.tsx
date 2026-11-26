import { TabularNumber, Text, formatTabularNumber } from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const ROWS = [
  { label: 'Requests', value: 18420 },
  { label: 'Errors', value: 37 },
  { label: 'Median time (ms)', value: 112.4 },
];

export default {
  id: 'tabular-number',
  title: 'TabularNumber',
  summary: 'Numbers in fixed-width digits, formatted with Intl.NumberFormat.',
  sections: {
    purpose: {
      summary:
        'Sets numbers in tabular digits so columns line up and live values do not jitter, and formats them with `Intl.NumberFormat` for the reader’s locale. Replaces the `tabular-nums` class sprinkled through the apps. Needed by: Rook People, Rook Dots, Rook Tickets, Rook Atlas, Rook Stride, Rook Calendar, Rook Recruit, Rook Intellect.',
      useWhen: [
        'Numbers in table columns, stat rows or lists that should align digit by digit.',
        'Values that change in place: counters, timers, progress figures.',
        'Any number shown to people, so it is formatted for their locale rather than with `toFixed`.',
      ],
      avoidWhen: [
        'Money with currency rules (minor units, currency display): use Money.',
        'A metric with a label and a change indicator: use StatCard or DeltaIndicator.',
        'Numbers inside a sentence where proportional digits read better.',
      ],
    },
    designPrinciples: {
      items: [
        'Formatting is `Intl.NumberFormat` only; no hand-made separators.',
        'A number never breaks across lines (`white-space: nowrap`).',
        'A missing value is an em dash that screen readers announce as “No value”, never a misleading 0.',
        'The same formatter is exported (`formatTabularNumber`) so labels, tooltips and exports match what is shown.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Renders a `<span>` and forwards its ref; HTML attributes pass through.',
        '`formatTabularNumber(value, { locale, ...Intl.NumberFormatOptions })` returns the formatted string and caches formatters by locale and options.',
        '`TABULAR_NUMBER_MESSAGES` holds the default strings.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Migrating from `<span className="tabular-nums">{n.toLocaleString()}</span>`: use `<TabularNumber value={n} />`; keep pre-formatted strings as children: `<TabularNumber>{text}</TabularNumber>`.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'format',
          values: ['decimal', 'percent', 'currency', 'unit', 'compact notation', 'signDisplay'],
          description: 'Any `Intl.NumberFormat` option; the look stays the same.',
        },
        {
          prop: 'value / children',
          values: ['number', 'bigint', 'null', 'ReactNode'],
          description:
            '`value` is formatted; without `value`, `children` are shown as given. `null`, `undefined` passed as `value`, NaN and Infinity show the empty marker.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Empty',
          attribute: 'data-empty',
          description: 'An em dash in the subtle text colour, with a visually hidden “No value”.',
        },
        {
          name: 'Formatted',
          attribute: 'data-value',
          description: 'The raw value, for tests and analytics.',
        },
      ],
    },
    interaction: { items: [], notes: ['Not interactive.'] },
    keyboard: { items: [], notes: ['Not focusable.'] },
    accessibility: {
      items: [
        'The formatted text is what screen readers read, so locale formatting also improves speech.',
        'The empty marker is hidden from assistive technology and replaced by `messages.emptyLabel`.',
        'When a number changes live and the change matters, announce it with a polite live region in the app; TabularNumber does not announce updates.',
      ],
    },
    responsive: {
      items: [
        'Inline and never wraps inside a number; very long numbers should use `notation: "compact"` on narrow screens.',
      ],
    },
    theme: {
      items: [
        'Inherits the surrounding colour and font; only the empty marker uses `--mn-color-text-subtle` (GrayText in forced colours).',
        'Tabular digits come from the font’s `tnum` feature, which Monarch’s sans and mono fonts provide.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Aligned column',
          stack: true,
          render: () => (
            <table style={{ borderCollapse: 'collapse' }}>
              <tbody>
                {ROWS.map((r) => (
                  <tr key={r.label}>
                    <th scope="row" style={{ textAlign: 'start', paddingInlineEnd: 24 }}>
                      <Text as="span" size="sm" weight="medium">
                        {r.label}
                      </Text>
                    </th>
                    <td style={{ textAlign: 'end' }}>
                      <TabularNumber value={r.value} locale="en-US" />
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          ),
        },
        {
          title: 'Formats',
          stack: true,
          render: () => (
            <>
              <Text>
                Share:{' '}
                <TabularNumber
                  value={0.4237}
                  format={{ style: 'percent', maximumFractionDigits: 1 }}
                />
              </Text>
              <Text>
                Change: <TabularNumber value={128} format={{ signDisplay: 'always' }} />
              </Text>
              <Text>
                Storage: <TabularNumber value={1532} format={{ style: 'unit', unit: 'megabyte' }} />
              </Text>
              <Text>
                Visitors: <TabularNumber value={1250000} format={{ notation: 'compact' }} />
              </Text>
            </>
          ),
        },
        {
          title: 'Empty value and helper',
          stack: true,
          render: () => (
            <>
              <Text>
                Last score: <TabularNumber value={null} />
              </Text>
              <Text size="sm" tone="muted">
                Exported as {formatTabularNumber(2048.5, { minimumFractionDigits: 2 })}
              </Text>
            </>
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Right-align numeric table columns and use TabularNumber in each cell.',
        'Pass `locale` when the page is rendered on the server.',
        'Use `formatTabularNumber` for strings that must match the displayed number.',
      ],
      dont: [
        'Do not format with `toFixed` or string concatenation.',
        'Do not show 0 for a missing value: pass `null` to get the empty marker.',
        'Do not use it for money that needs currency rules: use Money.',
      ],
    },
    related: {
      items: [
        { id: 'money', reason: 'Currency amounts with minor units and currency display.' },
        { id: 'delta-indicator', reason: 'A change with direction and tone.' },
        { id: 'stat-card', reason: 'A labelled metric card.' },
        { id: 'text', reason: '`tabular` on Text for digits inside a longer text.' },
      ],
    },
    dependencies: { packages: ['react'], components: [] },
    testing: {
      unit: [
        '`formatTabularNumber` formats with locale and options (decimal, percent, bigint, sign) and caches formatters.',
        'Formats `value`, exposes `data-value`, forwards ref and props; no axe violations.',
        'Shows children as given when there is no value.',
        'Null and NaN show the em dash with an accessible “No value” (translatable); no axe violations.',
        'Renders on the server with the given locale.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
