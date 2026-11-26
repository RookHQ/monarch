import { ProgressCircle } from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';

export default {
  id: 'progress-circle',
  title: 'ProgressCircle',
  summary: 'Determinate or indeterminate progress ring with value text.',
  sections: {
    purpose: {
      summary:
        'Shows how far a task has got in a compact ring: an upload in a file row, a sync in a toolbar, a step in a card. Without a value it is a loader. Needed by: none recorded; file uploads, imports and sync status are the intended uses.',
      useWhen: [
        'Progress belongs next to an item or icon where a bar would not fit.',
        'A small loader must say what is loading (`label`).',
      ],
      avoidWhen: [
        'Progress spans a page or form section: use Progress (the bar).',
        'The number is a score or rating, not progress towards done: use ScoreRing.',
        'Loading lives inside a button: Button `loading` already shows a Spinner.',
      ],
    },
    designPrinciples: {
      items: [
        'One element, two modes: give a `value` for determinate, leave it out for indeterminate.',
        'Always named: the ring is a progressbar with a label, never a silent decoration.',
        'Motion is optional: with reduced motion the indeterminate ring stands still.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Forwards its ref to the root `<div role="progressbar">`. No client JavaScript: safe in server components.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Values outside 0…`max` are clamped. `formatValue` controls both `aria-valuetext` and the centre text.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'size',
          values: ['xs', 'sm', 'md', 'lg', 'xl'],
          description:
            '1, 1.25, 2, 3 and 4.5 rem. The centre value is shown from `lg` up. Exposed as `data-size`.',
        },
        {
          prop: 'tone',
          values: ['accent', 'success', 'warning', 'danger', 'neutral'],
          description: 'Ring colour from the status tokens. Exposed as `data-tone`.',
        },
        {
          prop: 'showValue',
          values: ['true', 'false'],
          description:
            'Prints the formatted value inside the ring (decorative; the value is already announced).',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Indeterminate',
          attribute: 'data-state="indeterminate"',
          description: 'No value: a quarter arc spins. No `aria-valuenow`.',
        },
        {
          name: 'In progress',
          attribute: 'data-state="loading"',
          description: 'The arc length follows the value.',
        },
        {
          name: 'Complete',
          attribute: 'data-state="complete"',
          description: 'The value has reached `max`: a full ring.',
        },
      ],
    },
    interaction: {
      items: [],
      notes: ['Not interactive. Changes to the value animate the arc unless reduced motion is on.'],
    },
    keyboard: {
      items: [],
      notes: ['Not focusable; it is read in place by screen readers.'],
    },
    accessibility: {
      role: 'progressbar',
      name: '`label`, `aria-labelledby`, or the default "Progress" / "Loading" from `messages`.',
      items: [
        'Determinate rings expose `aria-valuemin`, `aria-valuemax`, `aria-valuenow` and a readable `aria-valuetext` ("42%").',
        'Indeterminate rings omit the value attributes, as the ARIA spec requires.',
        'The SVG and centre text are hidden from assistive technology so the value is not read twice.',
        'Progress is not announced as it changes; announce completion separately (for example with a toast) when it matters.',
      ],
    },
    responsive: {
      items: ['Fixed sizes in rem; it scales with the root font size and never overflows.'],
    },
    theme: {
      items: [
        'Track uses `--mn-color-bg-emphasis`, the arc the tone’s `-strong` token, so it reads in light, dark and more-contrast themes.',
        'In forced-colors mode the arc is Highlight and the track GrayText.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Determinate',
          render: () => (
            <>
              <ProgressCircle value={25} label="Upload" size="sm" />
              <ProgressCircle value={60} label="Import" />
              <ProgressCircle value={72} label="Sync" size="lg" showValue />
              <ProgressCircle value={100} label="Backup" size="xl" showValue tone="success" />
            </>
          ),
        },
        {
          title: 'Indeterminate',
          render: () => (
            <>
              <ProgressCircle size="xs" />
              <ProgressCircle label="Loading comments" />
              <ProgressCircle label="Preparing export" size="lg" tone="neutral" />
            </>
          ),
        },
        {
          title: 'Custom value text',
          render: () => (
            <ProgressCircle
              value={3}
              max={8}
              size="xl"
              showValue
              label="Files uploaded"
              formatValue={(value, max) => `${value}/${max}`}
            />
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Give every ring a `label` that says what is progressing.',
        'Switch from indeterminate to a value as soon as the total is known.',
      ],
      dont: [
        'Do not use colour alone to signal failure; show an error message next to it.',
        'Do not show a centre value on rings smaller than `lg`.',
      ],
    },
    related: {
      items: [
        { id: 'progress-bar', reason: 'Linear progress for wider areas.' },
        { id: 'spinner', reason: 'A minimal unlabelled loader inside controls.' },
        { id: 'score-ring', reason: 'A score or rating shown as a ring.' },
      ],
    },
    dependencies: { packages: ['react'], components: [] },
    testing: {
      unit: [
        'Determinate: progressbar name, min, max, now, value text, arc length and centre value; SVG hidden.',
        'Indeterminate without a value, named "Loading", with no value attributes.',
        'Clamps values, supports `max`, `formatValue` and `messages`.',
        'Uses `aria-labelledby` instead of the default name; exposes tone and size.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
