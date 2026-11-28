import { useState } from 'react';
import { Field, FieldDescription, FieldLabel, Slider } from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';
// The scenario of the primary example, shared with iOS and Android (CROSS-PLATFORM.md section 8).
import fixture from './slider.fixture.json';

function VolumeDemo() {
  const [volume, setVolume] = useState(fixture.data.value);
  const [saved, setSaved] = useState(fixture.data.value);
  return (
    <Field style={{ width: '100%', maxWidth: 360 }}>
      <FieldLabel>{fixture.data.label}</FieldLabel>
      <Slider
        value={volume}
        min={fixture.data.min}
        max={fixture.data.max}
        onValueChange={setVolume}
        onValueCommit={setSaved}
        showValue
      />
      <FieldDescription>Saved at {saved}.</FieldDescription>
    </Field>
  );
}

const SIZE_MARKS = [
  { value: 0, label: 'XS' },
  { value: 1, label: 'S' },
  { value: 2, label: 'M' },
  { value: 3, label: 'L' },
  { value: 4, label: 'XL' },
];
const SIZE_NAMES = ['Extra small', 'Small', 'Medium', 'Large', 'Extra large'];

export default {
  id: 'slider',
  title: 'Slider',
  summary: 'Choose one number on a track by dragging, clicking or the keyboard.',
  sections: {
    purpose: {
      summary:
        'Picks a value in a known range where the position matters more than the exact digit: volume, opacity, zoom, a 1–5 level. A WAI-ARIA slider with a formatted spoken value. Replaces the shadcn/ui copies at `Rook Links/src/components/ui/slider.tsx`, `Rook ID/rookid/components/ui/slider.tsx` and `Rook ID/rookid/src/components/ui/slider.tsx`. Needed by: rookatlas, rookcalendar, rookdots, rookid, rookpartner, rookpeople, rookstride and rooktickets frontends.',
      useWhen: [
        'A bounded value people adjust and watch the effect of (volume, opacity, zoom).',
        'A short ordered scale with named points: `marks` plus `formatValue`.',
      ],
      avoidWhen: [
        'A low and a high value: use RangeSlider.',
        'An exact number people know already (a price, a count): use NumberInput, or pair it with a Slider.',
        'Two to five unordered choices: use SegmentedControl or RadioGroup.',
      ],
    },
    designPrinciples: {
      items: [
        'Follows the WAI-ARIA APG slider pattern: one focusable thumb, arrows, PageUp/PageDown, Home/End.',
        'Stream and commit: `onValueChange` fires while dragging; `onValueCommit` fires once on release or key press, which is when to save.',
        'The spoken value is words, not just a number: `formatValue` turns 0.25 into "25%" or 2 into "Medium".',
        'Steps snap with decimal-safe arithmetic, so step 0.1 never produces 0.30000000000000004.',
        'Right-to-left aware: the track and the arrow keys flip.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Renders `div.mn-slider` with the track, the thumb (`role="slider"`), optional marks and an optional value. The ref and HTML attributes go to the root; `id`, `aria-label`, `aria-labelledby` and `aria-describedby` go to the thumb.',
        'Helpers shared with RangeSlider: `snapToStep`, `percentOf`, `valueFromPointer`, `sliderKeyValue`, `defaultLargeStep`, `isRtl`.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Migrating from shadcn/ui (Radix): `value={[n]}` → `value={n}`; `onValueChange(([n]) => …)` → `onValueChange((n) => …)`; `onValueCommit` is the same with a number; `defaultValue={[n]}` → `defaultValue={n}`; `orientation="vertical"` and `inverted` are not supported. For two thumbs use RangeSlider.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'size',
          values: ['sm', 'md'],
          description: 'Track and thumb size. Exposed as `data-size`.',
        },
        {
          prop: 'marks',
          values: ['SliderMark[]'],
          description:
            'Ticks under the track, with optional labels. Marks at or below the value are emphasised (`data-active`).',
        },
        {
          prop: 'showValue',
          values: ['true', 'false'],
          description: 'Shows the formatted value after the track.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Focused',
          attribute: ':focus-visible on the thumb',
          description: 'Focus ring around the thumb.',
        },
        {
          name: 'Disabled',
          attribute: 'data-disabled, aria-disabled on the thumb',
          description: 'Muted colours, removed from the tab order, ignores input.',
        },
        {
          name: 'Invalid',
          attribute: 'data-invalid, aria-invalid on the thumb',
          description: 'Danger fill, from `invalid` or the Field.',
        },
      ],
    },
    interaction: {
      items: [
        'Pressing anywhere on the track moves the thumb there and focuses it; dragging continues while the pointer is held, even outside the track.',
        '`onValueCommit` fires on pointer release and after each key press.',
        'Touch: the track does not scroll the page while dragging (`touch-action: none`).',
      ],
    },
    keyboard: {
      items: [
        { keys: 'ArrowRight / ArrowUp', action: 'Increase by `step` (ArrowLeft in RTL).' },
        { keys: 'ArrowLeft / ArrowDown', action: 'Decrease by `step` (ArrowRight in RTL).' },
        {
          keys: 'PageUp / PageDown',
          action: 'Increase or decrease by `largeStep` (a tenth of the range by default).',
        },
        { keys: 'Home / End', action: 'Go to `min` / `max`.' },
      ],
    },
    accessibility: {
      role: 'slider',
      name: 'FieldLabel inside a Field (via aria-labelledby), or `aria-label`.',
      items: [
        '`aria-valuenow`, `aria-valuemin`, `aria-valuemax`, `aria-orientation` and `aria-valuetext` from `formatValue`.',
        'Field description and error are joined to the thumb’s `aria-describedby`.',
        'Marks and the shown value are `aria-hidden`; the thumb’s value text says the same.',
        'With `name`, a hidden input submits the number.',
      ],
    },
    responsive: {
      items: [
        'Fills its container at every width, including 390px; mark labels centre on their points.',
        'On coarse pointers the thumb is larger and its hit area is 44px.',
      ],
    },
    theme: {
      items: [
        'Fill uses `--mn-color-accent-solid` (danger when invalid) on `--mn-color-bg-emphasis`; the thumb uses `--mn-color-bg-surface`.',
        'Forced colours: outlined track, Highlight fill, CanvasText thumb border.',
      ],
    },
    examples: {
      items: [
        { title: 'Stream and commit', stack: true, render: () => <VolumeDemo /> },
        {
          title: 'Named points',
          stack: true,
          render: () => (
            <Slider
              aria-label="Text size"
              min={0}
              max={4}
              defaultValue={2}
              marks={SIZE_MARKS}
              formatValue={(v: number) => SIZE_NAMES[v] ?? String(v)}
              style={{ maxWidth: 360 }}
            />
          ),
        },
        {
          title: 'Decimal steps, small',
          stack: true,
          render: () => (
            <Slider
              aria-label="Opacity"
              size="sm"
              min={0}
              max={1}
              step={0.05}
              defaultValue={0.8}
              showValue
              formatValue={(v: number) => `${Math.round(v * 100)}%`}
              style={{ maxWidth: 360 }}
            />
          ),
        },
        {
          title: 'Disabled',
          stack: true,
          render: () => (
            <Slider
              aria-label="Locked level"
              defaultValue={30}
              disabled
              style={{ maxWidth: 360 }}
            />
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Save in `onValueCommit`, not `onValueChange`.',
        'Give a `formatValue` whenever the number alone is not the meaning (percent, size names, units).',
        'Show the value (`showValue`) when the exact number matters.',
      ],
      dont: [
        'Do not use a slider for values people must hit exactly over a wide range; offer a NumberInput.',
        'Do not hide the label; a slider needs a name.',
      ],
    },
    related: {
      items: [
        { id: 'range-slider', reason: 'Two thumbs for a low and a high value.' },
        { id: 'number-input', reason: 'Exact numbers, typed.' },
        { id: 'segmented-control', reason: 'A few discrete choices.' },
        { id: 'form-field', reason: 'Label, description and error wiring.' },
      ],
    },
    dependencies: { packages: ['react'], components: ['form-field', 'number-input'] },
    testing: {
      unit: [
        'snapToStep, sliderKeyValue (with RTL), valueFromPointer, percentOf and defaultLargeStep.',
        'APG attributes, name and description from the Field.',
        'Arrows, PageUp/PageDown, Home/End, clamping, commit on each key.',
        'Arrow keys flip in RTL.',
        'formatValue for aria-valuetext and the shown value.',
        'Pointer press jumps and focuses, drag streams, release commits.',
        'Controlled value, marks and hidden input.',
        'Disabled from the Field ignores keys and pointers.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
