import { KPIRow } from '@rookcom/monarch-react/patterns';
import type { ComponentDocs } from '../../../internal/component-docs.js';

export default {
  id: 'kpi-row',
  title: 'KPIRow',
  summary: 'A row of key figures with their changes, built from StatCard.',
  sections: {
    purpose: {
      summary:
        'Shows the handful of figures a dashboard leads with (open items, revenue, time to reply), each with its change against the previous period, laid out responsively and loading or failing as one row. Needed by: Pulse, Tickets, Dots, Recruit, GTM, Atlas, Intellect, Stride, People, Developers.',
      useWhen: [
        'A dashboard or report opens with three to six headline figures.',
        'Figures come from one request and should load, fail and retry together.',
      ],
      avoidWhen: [
        'One figure: use StatCard.',
        'A ranked list: use Leaderboard.',
        'Figures that each need their own retry: render StatCards in a StatCardGroup yourself.',
      ],
    },
    designPrinciples: {
      items: [
        'Every change is polarity-aware: an increase in churn reads "worse", not green.',
        'A missing value is a dash read as "No data", never a zero.',
        'Same frame for every figure, so the eye compares values, not styles.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Each item takes every StatCard prop (`label`, `value`, `change`, `caption`, `hint`, `help`, `sparkline`, `href`, `format`, `tone`…) plus an `id`.',
        'The root is a StatCardGroup (`mn-stat-group mn-kpi-row`); native attributes pass through and the ref is forwarded.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Migrating from the app copy: People `StatTile`/`TileGrid` (`reports/viz.tsx`), Atlas `MetricCard` row, Intellect `TypeTile` row, Stride `Metric` and Tickets `Kpi` become one `items` array. `label` → `label`; `value` → `value` (numbers with `format`); `hint`/`description` → `hint`; `tone="warning"` → `tone`; `href` → `href`; hand-written "+3 in 30 days" deltas → `change={{ value, format: "number" }}` with `caption`; horizontal snap rows (Intellect) → `overflow="scroll"`.',
      ],
    },
    variants: {
      items: [
        { prop: 'size', values: ['sm', 'md', 'lg'], description: 'Card size for every figure.' },
        {
          prop: 'variant',
          values: ['card', 'plain'],
          description: '`plain` for a row inside a Widget.',
        },
        {
          prop: 'overflow',
          values: ['wrap', 'scroll'],
          description:
            '`scroll` keeps one sideways-scrolling row on narrow screens. Exposed as `data-overflow`.',
        },
      ],
    },
    states: {
      items: [
        { name: 'Ready', description: 'Every figure with its change.' },
        {
          name: 'Loading',
          attribute: 'data-state="loading"',
          description:
            'Value placeholders (or `loadingCount` placeholder cards before the first load) and `aria-busy`.',
        },
        {
          name: 'Error',
          attribute: 'data-state="error"',
          description:
            'The whole row is replaced by the error, with Try again when `onRetry` is set.',
        },
      ],
    },
    interaction: {
      items: [
        'Cards with `href` are links; the whole card is the click target.',
        'Cards with `help` open a popover from an info button.',
        'Try again calls `onRetry`.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Tab / Shift+Tab',
          action:
            'Moves through card links and help buttons (and the row itself with `overflow="scroll"`).',
        },
        { keys: 'Arrow Left / Arrow Right', action: 'Scroll a focused `overflow="scroll"` row.' },
      ],
    },
    accessibility: {
      role: 'group (when `label` is set)',
      name: '`label`, e.g. "This month"',
      items: [
        'Each change speaks its direction and whether it is better or worse (DeltaIndicator).',
        'Compact values give the full number to assistive technology.',
        'A scrolling row is a tab stop so keyboard users can scroll it.',
      ],
    },
    responsive: {
      items: [
        'Wraps to as many columns as fit at `minItemWidth` (default 12rem), two then one on narrow screens.',
        'With `overflow="scroll"` below 48rem the row scrolls sideways with snap points instead of wrapping.',
      ],
    },
    theme: {
      items: [
        'Cards use StatCard tokens: `--mn-color-bg-surface`, `--mn-color-border-default`, status tokens for tones and deltas.',
        'In forced colours borders and text follow system colours.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Headline figures with changes',
          stack: true,
          render: () => (
            <KPIRow
              label="Last 30 days"
              items={[
                {
                  id: 'open',
                  label: 'Open tickets',
                  value: 128,
                  format: 'number',
                  change: { value: 0.12, goodDirection: 'down' },
                  caption: 'vs previous 30 days',
                },
                {
                  id: 'sla',
                  label: 'Within target',
                  value: 0.94,
                  format: 'percent',
                  change: { value: 0.02 },
                },
                {
                  id: 'reply',
                  label: 'First reply',
                  value: 42,
                  unit: 'min',
                  change: { value: -0.08, goodDirection: 'down' },
                },
                { id: 'csat', label: 'Satisfaction', value: null, hint: 'No survey answers yet' },
              ]}
            />
          ),
        },
        {
          title: 'Loading before the first result',
          stack: true,
          render: () => <KPIRow label="Key figures" items={[]} loading loadingCount={3} />,
        },
        {
          title: 'Failed with retry',
          stack: true,
          render: () => <KPIRow items={[]} error="Figures did not load." onRetry={() => {}} />,
        },
      ],
    },
    doDont: {
      do: [
        'Keep it to three to six figures.',
        'Set `goodDirection: "down"` for costs, times and churn.',
        'Say the comparison period in `caption`.',
      ],
      dont: [
        'Do not show zero for a figure you could not compute; pass `null`.',
        'Do not mix currencies in one figure.',
      ],
    },
    related: {
      items: [
        { id: 'stat-card', reason: 'One figure, or figures that load separately.' },
        { id: 'delta-indicator', reason: 'A change on its own, next to text.' },
        { id: 'widget', reason: 'Frame a plain KPIRow with a title and menu.' },
      ],
    },
    dependencies: { packages: ['react'], components: ['inline-error-retry', 'stat-card'] },
    testing: {
      unit: [
        'Named group of StatCards with polarity-aware changes and spoken empty values; props and ref forwarded.',
        'Placeholder cards before the first load; value placeholders while refreshing.',
        'Row-level error with Try again.',
        'Scrolling row is a keyboard stop; size, variant and columns reach every card.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
