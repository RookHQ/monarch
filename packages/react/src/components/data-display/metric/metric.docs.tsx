import { Metric } from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';
// The scenario of the primary example, shared with iOS and Android (CROSS-PLATFORM.md section 8).
import fixture from './metric.fixture.json';

export default {
  id: 'metric',
  title: 'Metric',
  summary: 'One number with its label, unit and change, formatted with Intl; no card around it.',
  sections: {
    purpose: {
      summary:
        'Shows a single figure in place: a label, the number formatted for the locale, an optional unit and an optional change versus an earlier period. Needed by: Rook Tickets (`src/components/home/HomeDashboard.tsx` defines a local `Metric`), Rook Pulse (`src/components/pulse/home/SystemStatusHome.tsx` defines a local `Metric`).',
      useWhen: [
        'A number sits inside another surface: a card header, a panel, a table footer or a toolbar.',
        'Several small figures sit in a row and a full card for each would be too heavy.',
      ],
      avoidWhen: [
        'The figure is a dashboard tile with a border, help popover, sparkline, drill-down link, loading or error state: use StatCard.',
        'A row of dashboard tiles: use KpiRow or StatCardGroup.',
        'The value is a money amount in running text: use Money.',
      ],
    },
    designPrinciples: {
      items: [
        'Smaller than StatCard: no surface, no border, no help popover, no sparkline. It is the figure, not the tile.',
        'Numbers are formatted by `Intl.NumberFormat` for the locale; never hand-built strings like "1.2k".',
        'A compact figure always gives the full value to assistive technology.',
        'The change is drawn by DeltaIndicator, so its arrow, sign and spoken sentence carry the meaning without colour.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Forwards its ref to the root `<div>`; native `<div>` attributes pass through.',
        '`formatMetricValue(value, options)` returns the same text Metric shows, for table cells and exports that must match.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        '`change` takes every DeltaIndicator prop (`value`, or `current` + `previous`, `goodDirection`, `comparison`, `format`, …). Metric passes its `locale` down when the change has none.',
        'Currency formatting uses Money’s `formatMoney`, so currency digits follow ISO 4217 (JPY 0, KWD 3).',
      ],
    },
    variants: {
      items: [
        {
          prop: 'size',
          values: ['sm', 'md', 'lg'],
          description:
            '`sm` for dense rows, `md` (default), `lg` for a headline figure. Exposed as `data-size`.',
        },
        {
          prop: 'layout',
          values: ['stacked', 'inline'],
          description:
            '`stacked` (default) puts the label above the value; `inline` puts label, value and change on one line. Exposed as `data-layout`.',
        },
        {
          prop: 'align',
          values: ['start', 'end'],
          description: 'Alignment in the stacked layout; `end` for numeric columns. `data-align`.',
        },
        {
          prop: 'format',
          values: ['number', 'percent', 'currency'],
          description:
            'How a numeric value is written. `percent` takes a ratio (0.12 is 12%); `currency` needs `currency`.',
        },
        {
          prop: 'notation',
          values: ['standard', 'compact'],
          description: '`compact` writes 1.2K; the full value is spoken and shown as a title.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Empty',
          attribute: 'data-empty',
          description:
            '`null`, `undefined` or `""` shows a muted dash read as `messages.empty` ("No data"); the unit is hidden.',
        },
      ],
      notes: [
        'There is no loading or error state; a Metric sits inside a surface that owns those. Use StatCard when the figure needs them.',
      ],
    },
    interaction: {
      items: [],
      notes: ['Not interactive. Wrap it in a link or button when it should drill down.'],
    },
    keyboard: { items: [], notes: ['Not focusable: it is static text.'] },
    accessibility: {
      role: 'None (text)',
      name: 'Read in order: label, value, unit, change.',
      items: [
        'In compact notation the short form is `aria-hidden` and the full value is visually hidden text, so "12K" is read as "12,345".',
        'The empty dash is `aria-hidden`; `messages.empty` is read instead.',
        'The change is read as a sentence ("down 8% vs last week, better") by DeltaIndicator.',
      ],
    },
    responsive: {
      items: [
        'The value wraps (`overflow-wrap: anywhere`) rather than overflowing at 390px; the unit never splits from itself.',
        'The inline layout wraps onto a second line when it does not fit.',
      ],
    },
    theme: {
      items: [
        'Text uses `--mn-color-text-default`, the label and unit `--mn-color-text-muted`; the change uses DeltaIndicator’s status tokens.',
        'Figures use tabular numerals so stacked metrics line up.',
        'In forced colours all text is `CanvasText`.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Number, unit and change',
          render: () => (
            <Metric
              label={fixture.data.label}
              value={fixture.data.value}
              unit={fixture.data.unit}
              change={{
                value: fixture.data.change.value,
                goodDirection: fixture.data.change.goodDirection as 'down',
                comparison: fixture.data.change.comparison,
              }}
            />
          ),
        },
        {
          title: 'Percent, currency and compact',
          render: () => (
            <>
              <Metric label="Conversion" value={0.034} format="percent" />
              <Metric label="Revenue" value={48250} format="currency" currency="EUR" />
              <Metric label="Page views" value={1284311} notation="compact" />
            </>
          ),
        },
        {
          title: 'Sizes',
          render: () => (
            <>
              <Metric size="sm" label="Open" value={42} />
              <Metric label="Open" value={42} />
              <Metric size="lg" label="Open" value={42} change={{ current: 42, previous: 36 }} />
            </>
          ),
        },
        {
          title: 'Inline, in a toolbar',
          render: () => <Metric layout="inline" label="Selected" value={12} unit="of 240 rows" />,
        },
        {
          title: 'Empty',
          render: () => <Metric label="Uptime" value={null} unit="%" />,
        },
      ],
    },
    doDont: {
      do: [
        'Pass numbers, not pre-formatted strings, so the locale decides separators.',
        'Set `goodDirection: "down"` on changes where a fall is an improvement (latency, cost, churn).',
        'Use `align="end"` for figures in numeric columns.',
      ],
      dont: [
        'Don’t wrap a Metric in your own card to make a dashboard tile; use StatCard.',
        'Don’t use colour alone to say a figure is good or bad; use `change`.',
        'Don’t put a sentence in `unit`; keep it to a unit or a short "of N".',
      ],
    },
    related: {
      items: [
        {
          id: 'stat-card',
          reason:
            'The dashboard tile: surface, help popover, sparkline, link, loading and error states.',
        },
        { id: 'delta-indicator', reason: 'The change on its own; Metric uses it for `change`.' },
        { id: 'kpi-row', reason: 'A row of headline figures for a dashboard.' },
        { id: 'money', reason: 'A money amount in running text.' },
        { id: 'key-value', reason: 'Compact label and text pairs that are not numbers.' },
      ],
    },
    dependencies: { packages: ['react'], components: ['money', 'delta-indicator'] },
    testing: {
      unit: [
        'Shows the label, a locale-formatted number, its unit and a DeltaIndicator change.',
        'Formats percent and currency with Intl.',
        'In compact notation hides the short form and speaks the full value, with a title.',
        'Shows a dash read as `messages.empty` and no unit for a missing value.',
        'Shows text values as given and exposes layout, size and alignment.',
        '`formatMetricValue` matches what Metric renders.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
