import { StackedProportionBar } from '@rookcom/monarch-react/charts';
import type { ComponentDocs } from '../../../internal/component-docs.js';

export default {
  id: 'stacked-proportion-bar',
  title: 'StackedProportionBar',
  summary:
    'One horizontal bar split into proportions, with a legend of values and shares as a real list.',
  sections: {
    purpose: {
      summary:
        'Shows how one whole divides (pipeline stages, storage by type, monitors by state) in a single compact bar, with the numbers in a legend that also serves as the accessible version.',
      useWhen: [
        'A breakdown inside a card or row where a donut would be too large.',
        'Usage of a capacity (`total` larger than the parts, with a "Free" remainder).',
      ],
      avoidWhen: [
        'One value against a target: use Progress (meter).',
        'Ranking or comparing many parts: use BarList.',
        'Breakdowns over time: use a stacked BarChart.',
      ],
    },
    designPrinciples: {
      items: [
        'The bar is decorative; the legend is a real list ("Replied 18 · 9%"), so nothing depends on colour.',
        'Series colours come only from the data palette, in order or pinned. The one exception is status mode: when every segment is a state (operational, degraded, down), each takes a status `tone` from the intent tokens instead.',
        'Palette and status colours never mix in one bar: the types reject it, and a mixed array from untyped code logs a development warning and falls back to the palette.',
        'Small parts stay visible: segments have a minimum width and shares under 1% read "<1%".',
        'Gaps separate neighbours, so adjacent segments never merge.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'The ref and native attributes go to the root `<div>`.',
        '`formatShare(part, total, { locale, maximumFractionDigits })` is exported for the same "<1%" / ">99%" wording elsewhere.',
        'Exported from `@rookcom/monarch-react/charts`; the legend is the shared chart legend (`.mn-chart__legend`).',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: ['Negative and non-finite values count as 0.'],
    },
    variants: {
      items: [
        {
          prop: 'size',
          values: ['sm', 'md', 'lg'],
          description: 'Bar thickness 4, 8 (default) or 12px (`data-size`).',
        },
        {
          prop: 'legendContent',
          values: ['both', 'value', 'percent', 'none'],
          description: 'What each legend entry shows after its label.',
        },
        {
          prop: 'segment.tone (status mode)',
          values: ['success', 'warning', 'danger', 'info', 'neutral'],
          description:
            'Every segment gets an intent colour (`--mn-color-<tone>-solid`, `neutral` is `--mn-color-text-subtle`); the root has `data-mode="status"`. All segments or none.',
        },
        {
          prop: 'legend',
          values: ['true', 'false'],
          description: '`false` hides the legend visually but keeps it for assistive technology.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Remainder',
          attribute: 'data-remainder',
          description:
            '`total` above the sum leaves empty track; with `remainderLabel` it is listed in the legend.',
        },
        {
          name: 'Status mode',
          attribute: 'data-mode="status"',
          description:
            'Every segment has a `tone`; segments carry `data-tone` and the legend swatches use the same intent colours.',
        },
        {
          name: 'Zero segments',
          description: 'Left out of the bar and legend unless `hideZero={false}`.',
        },
        {
          name: 'Empty',
          attribute: 'data-empty',
          description: 'Every value 0: an empty track and `emptyLabel` ("No data yet").',
        },
      ],
    },
    interaction: {
      items: ['Hovering a segment shows its label, value and share as a native tooltip.'],
    },
    keyboard: { items: [], notes: ['Not interactive, so there is no keyboard behaviour.'] },
    accessibility: {
      role: 'list (the legend)',
      name: '`label` names the list, e.g. "Lead pipeline".',
      items: [
        'The bar is `aria-hidden`; every part is a list item with its label, value and share.',
        'With `legend={false}` the list is visually hidden, not removed.',
        'The empty state is announced with the label ("Lead pipeline: No data yet").',
        'In forced-colours mode the track is outlined and segments are drawn in CanvasText, separated by gaps.',
      ],
    },
    responsive: {
      items: [
        'The bar fills its container at any width; the legend wraps onto more lines.',
        'Minimum segment widths can exceed the bar only with many tiny parts; the overflow is clipped.',
      ],
    },
    theme: {
      items: [
        'Segments use `--mn-color-data-*`, contrast-checked per theme; in status mode the `-solid` intent tokens. The track is `--mn-color-bg-muted`.',
        'The remainder swatch matches the track, outlined with `--mn-color-border-strong`.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Pipeline stages',
          stack: true,
          render: () => (
            <div style={{ width: '100%', maxWidth: '32rem' }}>
              <StackedProportionBar
                label="Lead pipeline"
                locale="en-US"
                segments={[
                  { key: 'new', label: 'New', value: 1240 },
                  { key: 'enrolled', label: 'In a campaign', value: 610 },
                  { key: 'replied', label: 'Replied', value: 182 },
                  { key: 'converted', label: 'Converted', value: 41 },
                  { key: 'unsubscribed', label: 'Unsubscribed', value: 6 },
                ]}
              />
            </div>
          ),
        },
        {
          title: 'Storage capacity with a remainder',
          stack: true,
          render: () => (
            <div style={{ width: '100%', maxWidth: '32rem' }}>
              <StackedProportionBar
                label="Storage"
                size="lg"
                total={100}
                remainderLabel="Free"
                legendContent="value"
                formatValue={(v) => `${v} GB`}
                segments={[
                  { key: 'docs', label: 'Documents', value: 38 },
                  { key: 'media', label: 'Media', value: 21 },
                  { key: 'backups', label: 'Backups', value: 12 },
                ]}
              />
            </div>
          ),
        },
        {
          title: 'Status mode: monitors by state',
          stack: true,
          render: () => (
            <div style={{ width: '100%', maxWidth: '32rem' }}>
              <StackedProportionBar
                label="Monitors by state"
                legendContent="value"
                segments={[
                  { key: 'up', label: 'Operational', value: 42, tone: 'success' },
                  { key: 'degraded', label: 'Degraded', value: 3, tone: 'warning' },
                  { key: 'down', label: 'Down', value: 1, tone: 'danger' },
                  { key: 'paused', label: 'Paused', value: 2, tone: 'neutral' },
                ]}
              />
            </div>
          ),
        },
        {
          title: 'Compact, legend for screen readers only',
          stack: true,
          render: () => (
            <div style={{ width: '100%', maxWidth: '20rem' }}>
              <StackedProportionBar
                label="Campaign progress"
                size="sm"
                legend={false}
                segments={[
                  { key: 'replied', label: 'Replied', value: 12 },
                  { key: 'done', label: 'Completed', value: 30 },
                  { key: 'active', label: 'In progress', value: 58 },
                ]}
              />
            </div>
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Name the whole with `label`, and keep segment labels short.',
        'Use `total` and `remainderLabel` for capacities so the empty part is explained.',
        'Pin `color` when the same stages appear elsewhere on the page.',
        'Use status mode only when every segment is a state, and keep a text label on each one (the legend or the hidden list always shows it).',
      ],
      dont: [
        'Do not use status tones for categories that are not states (stages, file types): use the palette.',
        'Do not mix tones and palette colours in one bar, or rely on the tone colour alone to say which state a part is.',
        'Do not hide the legend unless the same numbers are visible nearby.',
        'Do not use it for more than about six parts.',
      ],
    },
    related: {
      items: [
        { id: 'progress-bar', reason: 'One value against a target, or a quota meter.' },
        { id: 'donut-chart', reason: 'The same breakdown as a larger chart with a table.' },
        { id: 'bar-list', reason: 'Ranking parts against each other.' },
      ],
    },
    dependencies: { packages: ['react'], components: [] },
    testing: {
      unit: [
        'Legend list of non-zero parts with value and share; decorative bar; proportional flex sizes.',
        'Palette colours by position and pinned slots; hideZero false.',
        'Status mode: intent colours on segments and swatches, text legend; mixed arrays rejected by the types and warned about at runtime.',
        'Capacity remainder with a named legend entry.',
        'Legend content modes and the visually hidden legend.',
        '"<1%" and ">99%" shares; formatShare edges.',
        'Minimum width and size; empty state; negative and NaN values; ref; SSR.',
      ],
      axe: true,
      ssr: true,
      browser: [
        'Visual baselines: /__gallery/stacked-proportion-bar',
        'Cross-browser axe: /components/stacked-proportion-bar',
      ],
    },
  },
} satisfies ComponentDocs;
