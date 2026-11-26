import { Progress } from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';

export default {
  id: 'progress-bar',
  title: 'Progress',
  summary: 'Linear progress, quota meters and stacked segments.',
  sections: {
    purpose: {
      summary:
        'Shows how far a task has got (uploads, syncs, imports), or that it is running when the amount is unknown.',
      useWhen: [
        'A task reports progress as a number (`value`).',
        'A long task runs with unknown duration (omit `value`).',
        'A measured quantity within a range, such as storage, seats or capacity (`semantics="meter"`).',
        'Parts of one total, such as done / in review / in progress (`segments`).',
      ],
      avoidWhen: [
        'A short wait inside a control: use Spinner.',
        'Placeholder content while a page loads: use Skeleton or LoadingState.',
      ],
    },
    designPrinciples: {
      items: [
        'Always named: give `label` (or `aria-labelledby`) so the bar says what is progressing.',
        'Omitting `value` means indeterminate; never fake a moving value.',
        'Progress is a task; a meter is a measurement. Choose the role that says which.',
        'Colour reinforces state (warning, over the limit) but the value text always says it too.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'The default progress bar is built on `@radix-ui/react-progress`; meters and segmented bars render their own `<div>` with the same `mn-progress` classes. The ref is forwarded to the root element in every form.',
        'Types `ProgressTone`, `ProgressSegment` and `ProgressSegmentTone` are exported.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        '`value` is clamped to 0–100% of `max` for the bar width.',
        'For meters, `aria-valuenow` is clamped to `max` and the real value is in `aria-valuetext`.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'size',
          values: ['sm', 'md'],
          description: 'Bar height. Default `md`. Exposed as `data-size`.',
        },
        {
          prop: 'tone',
          values: ['accent', 'success', 'warning', 'danger', 'neutral'],
          description:
            'Fill colour. Default `accent`. Exposed as `data-tone`. Meters without a tone default to `neutral`, follow `thresholds`, and turn `danger` over the limit.',
        },
        {
          prop: 'semantics',
          values: ['progressbar', 'meter'],
          description: 'ARIA role. Exposed as `data-semantics` on meters and segmented bars.',
        },
        {
          prop: 'segments',
          values: ['ProgressSegment[]'],
          description:
            'Stacked parts in order. Segment tones: the five tones plus `info` and `data-1`…`data-5` (default: the data palette in order).',
        },
      ],
    },
    states: {
      items: [
        { name: 'Determinate', description: 'The bar width is `value / max`, clamped to 0–100%.' },
        {
          name: 'Indeterminate',
          attribute: 'data-indeterminate',
          description:
            'Set when `value` is undefined or null; the bar animates and has no `aria-valuenow`. Meters are never indeterminate.',
        },
        {
          name: 'Warning and danger',
          attribute: 'data-tone',
          description:
            'Meter tone once the value reaches `thresholds.warning` or `thresholds.danger`.',
        },
        {
          name: 'Over the limit',
          attribute: 'data-over',
          description:
            'Meter value above `max`: full bar, danger tone, “over the limit” in the value text.',
        },
        {
          name: 'Segmented',
          attribute: 'data-segmented',
          description:
            'Stacked segments; the value is their sum and they never overflow the track.',
        },
      ],
    },
    interaction: { items: ['None. Progress is a read-only indicator.'] },
    keyboard: { items: [], notes: ['Not interactive, so there is no keyboard behaviour.'] },
    accessibility: {
      role: 'progressbar, or meter with `semantics="meter"`',
      name: '`label` becomes `aria-label`; or pass `aria-labelledby`.',
      items: [
        '`valueText` sets `aria-valuetext` (“7.2 of 10 GB used”); meters and segmented bars generate one (“Done 8, In review 4 (12 of 20)”) when it is not given.',
        'Determinate bars expose `aria-valuenow` (from Radix Progress).',
        'Indeterminate bars omit `aria-valuenow`.',
        'With reduced motion the indeterminate animation slows to 3s per cycle.',
      ],
    },
    responsive: { items: ['No breakpoints: the bar fills its container width.'] },
    theme: {
      items: [
        'Fill and track use Monarch tokens; `accent` follows the product accent. `neutral` (the meter default) uses `--mn-color-text-muted`, so a normal quota never reads as a status.',
        'Segment colours use the strong intent tokens and the `--mn-color-data-*` palette, separated by a 2px surface-coloured gap.',
        'Width transitions are removed under reduced motion.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Determinate and indeterminate',
          stack: true,
          render: () => (
            <>
              <Progress label="Upload progress" value={40} />
              <Progress label="Import progress" value={100} tone="success" size="sm" />
              <Progress label="Syncing members" />
            </>
          ),
        },
        {
          title: 'Quota meter',
          stack: true,
          render: () => (
            <>
              <Progress
                semantics="meter"
                label="Storage"
                value={4.2}
                max={10}
                valueText="4.2 of 10 GB used"
              />
              <Progress
                semantics="meter"
                label="Seats"
                value={46}
                max={50}
                thresholds={{ warning: 40, danger: 50 }}
                valueText="46 of 50 seats"
              />
              <Progress
                semantics="meter"
                label="Capacity"
                value={52}
                max={40}
                valueText="52 of 40 hours, overbooked"
              />
            </>
          ),
        },
        {
          title: 'Segmented',
          stack: true,
          render: () => (
            <>
              <Progress
                label="Sprint progress"
                max={24}
                segments={[
                  { label: 'Done', value: 11, tone: 'success' },
                  { label: 'In review', value: 4, tone: 'info' },
                  { label: 'In progress', value: 5, tone: 'warning' },
                ]}
              />
              <Progress
                semantics="meter"
                label="Logged hours"
                max={40}
                segments={[
                  { label: 'Billable', value: 22 },
                  { label: 'Internal', value: 9 },
                  { label: 'Leave', value: 4 },
                ]}
              />
            </>
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Name every bar with `label`.',
        'Switch to `tone="success"` or `"danger"` only when the task has finished that way.',
      ],
      dont: [
        'Do not animate a determinate value you cannot measure; use the indeterminate form.',
        'Do not leave `semantics` as `progressbar` for static quantities such as storage used: use `semantics="meter"`.',
        'Do not rely on segment colours alone: label each segment, and show a legend when the parts matter.',
      ],
    },
    related: {
      items: [
        { id: 'spinner', reason: 'Indeterminate progress inside a control.' },
        { id: 'loading-state', reason: 'Placeholder for a whole loading region.' },
        {
          id: 'stacked-proportion-bar',
          reason: 'Planned chart-style bar with a legend for proportions.',
        },
        { id: 'stat-card', reason: 'Headline figure that can sit above a meter.' },
      ],
    },
    dependencies: { packages: ['@radix-ui/react-progress', 'react'], components: [] },
    testing: {
      unit: [
        'Is a labelled progressbar with `aria-valuenow`; indeterminate has none.',
        'Default markup is unchanged from 0.3; `valueText` sets `aria-valuetext`.',
        'Meter role, value text, neutral default and threshold tones; an explicit tone wins.',
        'Over the limit: danger tone, clamped `aria-valuenow`, “over the limit” text, full bar.',
        'Segments: order, default tones, offsets, generated value text, no overflow.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
