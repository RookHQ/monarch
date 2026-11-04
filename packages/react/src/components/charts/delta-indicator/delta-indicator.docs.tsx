import { DeltaIndicator } from '@rookcom/monarch-react/charts';
import type { ComponentDocs } from '../../../internal/component-docs.js';

export default {
  id: 'delta-indicator',
  title: 'DeltaIndicator',
  summary:
    'An up, down or flat change with sign, arrow and a spoken sentence; colour follows which direction is better.',
  sections: {
    purpose: {
      summary:
        'Shows how a value moved between two periods, the same way everywhere: arrow, signed percentage or amount, an optional comparison, and green or red depending on whether that direction is good.',
      useWhen: [
        'A period-over-period change next to a KPI, in a table cell or in a list row.',
        'Changes where down is good, such as costs, response time, churn or a ranking position.',
        'Generating the same wording for a chart summary or `aria-label` (`formatDelta`).',
      ],
      avoidWhen: [
        'Showing a trend over many points: use a sparkline or line chart.',
        'Status that is not a change (healthy, failing): use StatusBadge.',
      ],
    },
    designPrinciples: {
      items: [
        'Meaning never depends on colour: the arrow, the sign (+ or a true minus) and the spoken direction carry it; colour only says better or worse.',
        '`goodDirection` decides colour, not the sign: a 12% fall in cost is green.',
        'Nothing to compare is not zero: a missing or zero baseline shows "No earlier data", not "+∞%" or "0%".',
        'A change that rounds to zero when written ("0%") is shown as "No change", never "+0%".',
        'Formatting is Intl.NumberFormat with the given locale; currency goes through Money.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'The ref and native `<span>` attributes go to the root.',
        '`formatDelta(options)` returns `{ value, direction, tone, text, magnitude, label }`, the same text and spoken sentence the component renders, for chart summaries and exports.',
        'Exported from `@rookcom/monarch-react/charts`.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'With `format="percent"` (the default) `value` is a ratio: 0.12 is 12%. Pass `current` and `previous` to have the change computed instead.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'format',
          values: ['percent', 'number', 'currency'],
          description:
            '`percent` (default) writes a ratio as a percentage; `number` with an optional `unit` (pt, ms); `currency` through Money with `currency`.',
        },
        {
          prop: 'goodDirection',
          values: ['up', 'down', 'neither'],
          description:
            'Which direction is an improvement. Default `up`. `neither` keeps the change neutral grey.',
        },
        {
          prop: 'appearance',
          values: ['plain', 'pill'],
          description: '`pill` puts the arrow and value in a tinted chip (`data-appearance`).',
        },
        {
          prop: 'size',
          values: ['sm', 'md'],
          description: '`sm` (default) for card footers and tables, `md` beside body text.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Up / down',
          attribute: 'data-direction="up|down"',
          description:
            'Arrow, signed value, visually hidden direction word and "better" or "worse".',
        },
        {
          name: 'Better / worse / neutral',
          attribute: 'data-tone="positive|negative|neutral"',
          description: 'Derived from the direction and `goodDirection`.',
        },
        {
          name: 'Flat',
          attribute: 'data-direction="flat"',
          description:
            'Zero, within `flatThreshold`, or rounding to zero: a dash and "No change", neutral.',
        },
        {
          name: 'No data',
          attribute: 'data-empty',
          description:
            '`value` null or not finite, `current` or `previous` missing, or a `previous` of 0 for a percentage: "No earlier data".',
        },
      ],
    },
    interaction: { items: ['None. DeltaIndicator is read-only text.'] },
    keyboard: { items: [], notes: ['Not interactive, so there is no keyboard behaviour.'] },
    accessibility: {
      role: 'none (inline text)',
      name: 'The visually hidden sentence, e.g. "up 12% vs last month, better".',
      items: [
        'The arrow and the signed value are `aria-hidden`; a visually hidden "up 12%" or "down 8%" is read instead, so screen readers never say "plus" or skip a glyph.',
        'The comparison is real text, read after the change, followed by a hidden "better" or "worse" when a direction is better.',
        'Every word is replaceable through `labels` for translation.',
        'In forced-colours mode the pill keeps a border; the arrow and sign remain.',
      ],
    },
    responsive: {
      items: [
        'The change never wraps; the comparison may wrap onto the next line in narrow cells.',
      ],
    },
    theme: {
      items: [
        'Better uses `--mn-color-success-strong`, worse `--mn-color-danger-strong`, neutral `--mn-color-text-muted`; all meet AA text contrast in light, dark and high-contrast themes.',
        'The pill tint is the matching `-subtle` token.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Better, worse and lower-is-better',
          stack: true,
          render: () => (
            <>
              <DeltaIndicator value={0.124} comparison="vs last month" locale="en-US" />
              <DeltaIndicator value={-0.052} comparison="vs last month" locale="en-US" />
              <DeltaIndicator
                current={182}
                previous={240}
                format="number"
                unit="ms"
                goodDirection="down"
                comparison="vs last week"
                locale="en-US"
              />
              <DeltaIndicator
                value={1250}
                format="currency"
                currency="USD"
                goodDirection="down"
                comparison="in spend"
                locale="en-US"
              />
            </>
          ),
        },
        {
          title: 'Flat, no data and pill',
          stack: true,
          render: () => (
            <>
              <DeltaIndicator value={0.0004} comparison="vs last week" locale="en-US" />
              <DeltaIndicator current={14} previous={0} />
              <DeltaIndicator value={0.08} appearance="pill" size="md" locale="en-US" />
              <DeltaIndicator
                value={-3}
                format="number"
                goodDirection="neither"
                appearance="pill"
              />
            </>
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Set `goodDirection="down"` for costs, latency, churn, bounce rate and rank positions.',
        'Pass the comparison period ("vs last month") so the change is never ambiguous.',
        'Use `formatDelta` for chart summaries so the words match the indicator.',
      ],
      dont: [
        'Do not compute a percentage against a zero baseline; let DeltaIndicator show "No earlier data".',
        'Do not invent a change when the product has no previous value: pass `null`.',
        'Do not use a status colour on its own to show direction.',
      ],
    },
    related: {
      items: [
        { id: 'stat-card', reason: 'A KPI tile that shows a DeltaIndicator through `change`.' },
        { id: 'money', reason: 'Formats currency changes.' },
        { id: 'sparkline', reason: 'A trend over many points rather than one change.' },
        { id: 'status-badge', reason: 'A state rather than a change.' },
      ],
    },
    dependencies: { packages: ['react'], components: ['money', 'visually-hidden'] },
    testing: {
      unit: [
        'Signed percentage with spoken direction, comparison and quality; arrow hidden.',
        'goodDirection down (costs) and neither; computed percentage and absolute changes with a unit.',
        'Currency through Money and a custom formatter.',
        'Flat for zero, inside the dead band and when rounding to zero.',
        'No-data label for null, NaN, missing or zero baselines.',
        'Translated labels and locale; pill, size, hideIcon; ref forwarding; formatDelta output.',
      ],
      axe: true,
      ssr: true,
      browser: [
        'Visual baselines: /__gallery/delta-indicator',
        'Cross-browser axe: /components/delta-indicator',
      ],
    },
  },
} satisfies ComponentDocs;
