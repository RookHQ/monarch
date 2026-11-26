import { useState } from 'react';
import { Field, FieldDescription, FieldLabel, RangeSlider } from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';

function PriceFilterDemo() {
  const [range, setRange] = useState<[number, number]>([200, 800]);
  const money = (v: number) => `$${v}`;
  return (
    <Field style={{ width: '100%', maxWidth: 360 }}>
      <FieldLabel>Price</FieldLabel>
      <RangeSlider
        min={0}
        max={1000}
        step={50}
        value={range}
        onValueChange={setRange}
        formatValue={money}
        showValue
      />
      <FieldDescription>Results update when you let go.</FieldDescription>
    </Field>
  );
}

export default {
  id: 'range-slider',
  title: 'RangeSlider',
  summary: 'Choose a low and a high value on one track; the thumbs never cross.',
  sections: {
    purpose: {
      summary:
        'Picks a range inside known bounds: price and salary filters, age or experience bands, working hours. Two WAI-ARIA sliders in a labelled group. Replaces the two-thumb use of the shadcn/ui Slider copies (`Rook Links/src/components/ui/slider.tsx`, `Rook ID/rookid/components/ui/slider.tsx`, `Rook ID/rookid/src/components/ui/slider.tsx`). Needed by: filter panels in rookpeople, rookrecruit, rookdots and rookatlas frontends.',
      useWhen: ['A filter or setting with a lower and an upper bound on one scale.'],
      avoidWhen: [
        'One value: use Slider.',
        'Exact bounds people type (invoice amounts): use two NumberInputs or CurrencyInputs.',
        'Date ranges: use DateRangePicker.',
      ],
    },
    designPrinciples: {
      items: [
        'Each thumb is its own slider with its own name ("Price Minimum", "Price Maximum") and keyboard support.',
        'Thumbs can meet but never cross; `minStepsBetween` keeps a gap. Each thumb’s `aria-valuemin`/`aria-valuemax` show its real limits.',
        'Stream and commit: `onValueChange` while moving, `onValueCommit` on release or key press.',
        'The value is always `[low, high]`, low first.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Renders `div.mn-range-slider` (`role="group"`) with the track, two thumbs (`role="slider"`, `data-thumb="low" | "high"`), optional marks and value. The ref and HTML attributes go to the group.',
        'With `name`, two hidden inputs with that name submit low then high (`formData.getAll(name)`).',
        '`RANGE_SLIDER_MESSAGES` holds the English defaults for `messages`.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Migrating from shadcn/ui (Radix): `<Slider value={[a, b]} onValueChange={…} />` → `<RangeSlider value={[a, b]} onValueChange={…} />`; `minStepsBetweenThumbs` → `minStepsBetween`; `onValueCommit` is the same.',
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
          description: 'Ticks under the track; marks inside the range are emphasised.',
        },
        {
          prop: 'showValue',
          values: ['true', 'false'],
          description: 'Shows "low – high" after the track (`messages.range`).',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Focused thumb',
          attribute: ':focus-visible',
          description: 'Focus ring; the focused thumb is drawn on top.',
        },
        {
          name: 'Thumbs together',
          attribute: 'equal aria-valuenow',
          description: 'Both thumbs at one value; arrows can only move them apart.',
        },
        {
          name: 'Disabled',
          attribute: 'data-disabled, aria-disabled on thumbs',
          description: 'Muted, out of the tab order, ignores input.',
        },
        {
          name: 'Invalid',
          attribute: 'data-invalid, aria-invalid on thumbs',
          description: 'Danger fill.',
        },
      ],
    },
    interaction: {
      items: [
        'Pressing the track moves the nearer thumb there and focuses it; dragging continues until release.',
        'When both thumbs sit at the maximum, pressing there picks the low thumb, so it can always be pulled back.',
        'A dragged thumb stops at the other thumb (plus any gap).',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Moves to the low thumb, then the high thumb.' },
        {
          keys: 'Arrow keys',
          action: 'Move the focused thumb by `step` (left/right flip in RTL).',
        },
        { keys: 'PageUp / PageDown', action: 'Move by `largeStep`.' },
        {
          keys: 'Home / End',
          action: 'Move the focused thumb to its lowest / highest allowed value.',
        },
      ],
    },
    accessibility: {
      role: 'group containing two sliders',
      name: 'The group takes the FieldLabel or `aria-label`; each thumb is labelled by the group name plus "Minimum" or "Maximum".',
      items: [
        'Each thumb has `aria-valuenow`, `aria-valuetext` from `formatValue`, and bounds that reflect the other thumb.',
        'Field description and error are joined to both thumbs.',
        'Marks and the shown range are `aria-hidden`; the thumbs say the same.',
      ],
    },
    responsive: {
      items: [
        'Fills its container at every width, including 390px.',
        'On coarse pointers the thumbs are larger with 44px hit areas.',
      ],
    },
    theme: {
      items: [
        'Range fill `--mn-color-accent-solid` on `--mn-color-bg-emphasis`; thumbs on `--mn-color-bg-surface`.',
        'Forced colours: outlined track, Highlight range, CanvasText thumbs.',
      ],
    },
    examples: {
      items: [
        { title: 'Price filter', stack: true, render: () => <PriceFilterDemo /> },
        {
          title: 'Working hours with a minimum gap',
          stack: true,
          render: () => (
            <RangeSlider
              aria-label="Working hours"
              min={0}
              max={24}
              defaultValue={[9, 17]}
              minStepsBetween={1}
              marks={[
                { value: 0, label: '00:00' },
                { value: 12, label: '12:00' },
                { value: 24, label: '24:00' },
              ]}
              formatValue={(v: number) => `${String(v).padStart(2, '0')}:00`}
              style={{ maxWidth: 360 }}
            />
          ),
        },
        {
          title: 'Small',
          stack: true,
          render: () => (
            <RangeSlider
              aria-label="Years"
              size="sm"
              max={30}
              defaultValue={[3, 10]}
              showValue
              style={{ maxWidth: 360 }}
            />
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Apply filters in `onValueCommit` so results do not reload on every pixel.',
        'Show the range (`showValue`) or the result count so people know where they are.',
      ],
      dont: [
        'Do not use two separate Sliders for one range: they can cross and are named poorly.',
        'Do not use it when the range is unbounded; type the bounds instead.',
      ],
    },
    related: {
      items: [
        { id: 'slider', reason: 'One value on a track.' },
        { id: 'number-input', reason: 'Typed exact bounds.' },
        { id: 'form-field', reason: 'Label, description and error wiring.' },
      ],
    },
    dependencies: { packages: ['react'], components: ['form-field', 'number-input', 'slider'] },
    testing: {
      unit: [
        'Group and thumbs named from the Field; bounds reflect the other thumb.',
        'Thumb names from aria-label and translated messages.',
        'Keyboard on each thumb; thumbs meet but never cross; commit on key press.',
        '`minStepsBetween` keeps a gap.',
        'Pointer picks the nearer thumb, drags with clamping, commits on release.',
        'Controlled value, shown range, marks and two hidden inputs.',
        'Disabled ignores keys.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
