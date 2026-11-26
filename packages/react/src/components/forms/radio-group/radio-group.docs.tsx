import { useState } from 'react';
import {
  Button,
  Field,
  FieldDescription,
  FieldError,
  FieldLabel,
  RadioGroup,
  RadioGroupItem,
  type RadioGroupProps,
} from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';
// The scenario of the primary example, shared with iOS and Android (CROSS-PLATFORM.md section 8).
import fixture from './radio-group.fixture.json';

function ControlledFrequency() {
  const [value, setValue] = useState('weekly');
  return (
    <Field>
      <FieldLabel>Summary email</FieldLabel>
      <FieldDescription>Sent on Monday mornings for weekly, the 1st for monthly.</FieldDescription>
      <RadioGroup
        value={value}
        onValueChange={setValue}
        options={[
          { value: 'daily', label: 'Daily' },
          { value: 'weekly', label: 'Weekly' },
          { value: 'monthly', label: 'Monthly' },
          { value: 'never', label: 'Never' },
        ]}
      />
    </Field>
  );
}

function RequiredWithError() {
  const [value, setValue] = useState('');
  const [submitted, setSubmitted] = useState(false);
  const invalid = submitted && value === '';
  return (
    <form
      noValidate
      onSubmit={(event) => {
        event.preventDefault();
        setSubmitted(true);
      }}
      style={{ display: 'grid', gap: '0.75rem', justifyItems: 'start' }}
    >
      <Field required invalid={invalid}>
        <FieldLabel>Contact method</FieldLabel>
        <RadioGroup
          name="contact"
          value={value}
          onValueChange={setValue}
          options={[
            { value: 'email', label: 'Email' },
            { value: 'phone', label: 'Phone' },
            { value: 'none', label: 'Do not contact me' },
          ]}
        />
        <FieldError>{invalid ? 'Choose a contact method.' : null}</FieldError>
      </Field>
      <Button type="submit" size="sm">
        Continue
      </Button>
    </form>
  );
}

export default {
  id: 'radio-group',
  title: 'RadioGroup',
  summary: 'One choice from a short list of options, built on native radio buttons.',
  sections: {
    purpose: {
      summary:
        'Lets people pick exactly one option from a short list where every option should be visible, with optional help text per option. It wires into Field for the label, help, error and required state. Needed by: Rook ID and Rook Links (shadcn/ui RadioGroup copies), Rook Calendar, Rook Chat, Rook People, Rook Tickets, Rook Developers and Rook Recruit (hand-written `<input type="radio">` lists), and Monarch FieldRenderer.',
      useWhen: [
        'Two to about seven mutually exclusive options that people should compare side by side.',
        'Options need a sentence of explanation each.',
        'A form question that must submit natively and validate `required`.',
      ],
      avoidWhen: [
        'Options with a title, icon and description shown as large tiles: use RadioCardGroup.',
        'A compact view switch that applies immediately: use SegmentedControl.',
        'Long lists or limited space: use Select or Combobox.',
        'Several options can be on at once: use CheckboxGroup.',
        'A single on/off setting: use Switch or Checkbox.',
      ],
    },
    designPrinciples: {
      items: [
        'Native radios first: the browser already implements the APG radio group keyboard model, RTL arrows, form submission and required validation, so the component adds styling and Field wiring and nothing else.',
        'Every option has a visible label; help text sits under the label and is announced as the radio description.',
        'Selection is a filled dot, not a colour change alone, and forced-colours mode falls back to the system radio.',
        'The whole option row is the hit area, at least 44px tall on touch screens.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'RadioGroup forwards its ref to the `role="radiogroup"` `<div>`; RadioGroupItem forwards its ref to the `<input type="radio">`.',
        'Pass the choices as `options` data or as `RadioGroupItem` children (both may be combined; options render first).',
        '`useRadioGroupContext()` returns the group state (`name`, `value`, `select`, `disabled`, `required`, `invalid`) for custom radio layouts; RadioCardGroup is built on it.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        "`value` + `onValueChange` (controlled) or `defaultValue` (uncontrolled). `''` means nothing is selected.",
        'Inside a Field, `required`, `disabled` and `invalid` default to the Field; explicit props win.',
        'Migrating from shadcn/ui (`@/components/ui/radio-group`): `RadioGroup` keeps `value`, `defaultValue`, `onValueChange`, `name`, `required`, `disabled` and `orientation`; `RadioGroupItem value` is the same, but give it `label` (and optional `description`) instead of a separate `<Label htmlFor>`. `asChild`, `loop` and `dir` are not needed: the browser handles wrapping and text direction.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'orientation',
          values: ['vertical', 'horizontal'],
          description:
            '`vertical` (default) stacks the options; `horizontal` puts them in a row that wraps on narrow screens (`data-orientation`).',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Checked',
          attribute: ':checked on the radio, data-state="checked" on the row',
          description: 'Accent-filled circle with a surface-coloured ring that draws the dot.',
        },
        {
          name: 'Focus',
          attribute: ':focus-visible',
          description: 'Focus ring around the radio.',
        },
        {
          name: 'Disabled',
          attribute: 'disabled on the radio, data-disabled on the row or group',
          description: 'Dimmed radio and disabled-text label; arrow keys skip it.',
        },
        {
          name: 'Invalid',
          attribute: 'aria-invalid on the group, data-invalid',
          description: 'Danger-coloured radio borders; the Field error is announced.',
        },
        {
          name: 'Required',
          attribute: 'aria-required on the group, required on each radio',
          description: 'A choice must be made before the form submits.',
        },
      ],
    },
    interaction: {
      items: [
        'Clicking a radio or its label selects it and calls `onValueChange` with its value.',
        'Arrow keys move to the next or previous enabled option and select it, wrapping at the ends (native radio behaviour).',
        'In controlled mode the checked radio always follows `value`.',
        'With `name`, the selected value submits with the surrounding form.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Tab / Shift+Tab',
          action:
            'Moves into the group at the checked radio (or the first enabled one when none is checked), then out of it.',
        },
        { keys: '↓ / →', action: 'Next enabled option, selected. Wraps to the first.' },
        { keys: '↑ / ←', action: 'Previous enabled option, selected. Wraps to the last.' },
        { keys: 'Space', action: 'Selects the focused option when none is checked.' },
      ],
      notes: ['In right-to-left text the browser swaps Left and Right, as the APG pattern asks.'],
    },
    accessibility: {
      role: 'radiogroup containing native radio inputs',
      name: 'The `label` prop, the Field’s FieldLabel (`aria-labelledby`), or `aria-label`. Each radio is named by its label.',
      items: [
        'Follows the WAI-ARIA APG radio group pattern using native `<input type="radio">`; one Tab stop for the group.',
        'The group description combines `description` (or FieldDescription) and FieldError when invalid.',
        'Each option description is linked with `aria-describedby`.',
        '`aria-required` and `aria-invalid` sit on the group; `required` on each radio for native validation.',
        'In forced-colours mode the system radio is drawn, so the checked state is never lost.',
      ],
    },
    responsive: {
      items: [
        'Labels and descriptions wrap; the group never causes horizontal scrolling at 390px.',
        '`horizontal` wraps onto more rows when the options do not fit.',
        'Under `pointer: coarse` each option row is at least 44px tall.',
      ],
    },
    theme: {
      items: [
        'Border, fill, surface and text colours are Monarch tokens, so every product theme and dark mode apply.',
        'Forced-colours mode uses the system radio; the transition is removed under `prefers-reduced-motion: reduce`.',
        'Spacing uses logical properties, so the layout mirrors in RTL.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Options with descriptions',
          render: () => (
            <RadioGroup
              label={fixture.data.label}
              defaultValue={fixture.data.selected}
              options={fixture.data.options as unknown as RadioGroupProps['options']}
            />
          ),
        },
        { title: 'In a Field, controlled', render: () => <ControlledFrequency /> },
        { title: 'Required with an error', render: () => <RequiredWithError /> },
        {
          title: 'Horizontal, written as items',
          render: () => (
            <RadioGroup label="Text size" orientation="horizontal" defaultValue="medium">
              <RadioGroupItem value="small" label="Small" />
              <RadioGroupItem value="medium" label="Medium" />
              <RadioGroupItem value="large" label="Large" />
            </RadioGroup>
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Preselect a sensible default when one exists, so the question has an answer.',
        'Keep labels short and parallel; put explanations in `description`.',
        'Use a Field with FieldLabel and FieldError in forms so errors are announced.',
      ],
      dont: [
        'Do not use radios for more than about seven options: use Select or Combobox.',
        'Do not trigger navigation or slow work on selection; arrow keys select as they move.',
        'Do not hide the label: every radio needs a visible name.',
      ],
    },
    related: {
      items: [
        { id: 'radio-card-group', reason: 'The same choice shown as large cards with icons.' },
        { id: 'segmented-control', reason: 'A compact, immediately applied view switch.' },
        { id: 'checkbox-group', reason: 'Several options can be chosen.' },
        { id: 'select', reason: 'Many options in little space.' },
        { id: 'form-field', reason: 'Label, help and error wiring.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['form-field'],
    },
    testing: {
      unit: [
        'Labelled radiogroup with named and described radios; disabled option.',
        'Click on radio and label selects and reports the value.',
        'One Tab stop on the checked radio; arrow keys move, select and skip disabled; Space selects.',
        'Controlled mode follows `value` only.',
        'Field integration: label, description, error, required, invalid, disabled.',
        'Native form submission under `name`; RadioGroupItem children; horizontal orientation.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
