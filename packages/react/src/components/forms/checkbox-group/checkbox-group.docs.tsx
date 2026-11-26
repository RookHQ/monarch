import { useState } from 'react';
import { CheckboxGroup, Field, FieldError, FieldLabel } from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';

function AtLeastOne() {
  const [value, setValue] = useState<string[]>([]);
  const invalid = value.length === 0;
  return (
    <Field invalid={invalid} required>
      <FieldLabel>Topics</FieldLabel>
      <CheckboxGroup
        name="topics"
        value={value}
        onValueChange={setValue}
        options={[
          { value: 'product', label: 'Product updates' },
          { value: 'events', label: 'Events and webinars' },
          { value: 'research', label: 'Research invitations' },
        ]}
      />
      <FieldError>{invalid ? 'Choose at least one topic.' : null}</FieldError>
    </Field>
  );
}

export default {
  id: 'checkbox-group',
  title: 'CheckboxGroup',
  summary: 'A labelled set of checkboxes with an optional tri-state select-all.',
  sections: {
    purpose: {
      summary:
        'Lets people pick any number of options from a short list, with one label, help and error for the whole question, and an optional "Select all" that shows a mixed state. Needed by: Monarch FieldRenderer (`checkboxes` questions), Rook Tickets (admin settings and team pages), Rook Chat (admin and personal settings), Rook Calendar (service editor and form fields) and Rook People (pay calendars), which today map over hand-placed checkboxes.',
      useWhen: [
        'Several independent options that can each be on or off, all visible at once.',
        'A form question whose answer is a list of values.',
        'A permission or channel list where "everything" is a common answer (select all).',
      ],
      avoidWhen: [
        'Only one option can be chosen: use RadioGroup.',
        'More than about ten options, or a searchable list: use MultiSelect.',
        'Settings that apply immediately: use Switch rows (SwitchSettingsList).',
      ],
    },
    designPrinciples: {
      items: [
        'One question, one label: the group carries the label, help and error; each checkbox carries only its own label.',
        'Select all reflects the real state (checked, mixed, unchecked) and never changes disabled choices.',
        'The value is always reported in option order, so it is stable to compare and store.',
        'Built on Checkbox, so every checkbox looks and behaves the same across Monarch.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Forwards its ref to the `role="group"` `<div>`.',
        'Each checkbox is a Monarch Checkbox (a `button role="checkbox"`); with `name`, Radix adds a hidden native input per checkbox inside a form.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        '`value` + `onValueChange(values)` (controlled) or `defaultValue` (uncontrolled), as string arrays in option order.',
        '`messages.selectAll` changes the select-all label (default "Select all").',
        '`required` only shows the marker: validate the count yourself and show a FieldError.',
        'Migrating from shadcn/ui: apps map options to `<Checkbox checked onCheckedChange>` plus `<Label>`, keeping an array in state. Replace the loop with `options` and `value`/`onValueChange`; per-item `disabled` and help text move into each option.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'orientation',
          values: ['vertical', 'horizontal'],
          description:
            '`vertical` (default) stacks the choices; `horizontal` wraps them in rows (`data-orientation`).',
        },
        {
          prop: 'selectAll',
          values: ['true', 'false'],
          description:
            'Adds a tri-state "Select all" above the choices, which are then indented (`data-nested`).',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Checked / unchecked',
          attribute: 'aria-checked and data-state on each checkbox',
          description: 'Accent fill with a check mark.',
        },
        {
          name: 'Mixed (select all)',
          attribute: 'aria-checked="mixed", data-state="indeterminate"',
          description: 'Some but not all enabled choices are checked; shows a dash.',
        },
        {
          name: 'Disabled',
          attribute: 'disabled on a checkbox, data-disabled on the group',
          description: 'A disabled choice keeps its state when select all toggles.',
        },
        {
          name: 'Invalid',
          attribute: 'aria-invalid on each checkbox, data-invalid on the group',
          description: 'Danger-coloured checkbox borders; the Field error is announced.',
        },
      ],
    },
    interaction: {
      items: [
        'Clicking a checkbox or its label toggles it and calls `onValueChange` with every checked value.',
        'Select all checks every enabled choice; when all are checked it clears them.',
        'With `name`, each checked value submits with the surrounding form under that name.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab / Shift+Tab', action: 'Moves between the checkboxes (each is a Tab stop).' },
        { keys: 'Space', action: 'Toggles the focused checkbox, including select all.' },
      ],
      notes: [
        'Checkboxes are separate Tab stops, as in the APG checkbox pattern; arrow keys are not used.',
      ],
    },
    accessibility: {
      role: 'group containing checkboxes',
      name: 'The `label` prop, the Field’s FieldLabel (`aria-labelledby`) or `aria-label`. Each checkbox is named by its label.',
      items: [
        'Follows the WAI-ARIA APG checkbox pattern, including the mixed state for select all.',
        'Select all names the choices it controls with `aria-controls`.',
        'The group description combines `description` (or FieldDescription) and FieldError when invalid.',
        'Each checkbox gets its own id, so a surrounding Field never produces duplicate ids.',
      ],
    },
    responsive: {
      items: [
        'Labels and descriptions wrap; no horizontal scroll at 390px.',
        '`horizontal` wraps onto more rows when the choices do not fit.',
        'Under `pointer: coarse` each row is at least 44px tall.',
      ],
    },
    theme: {
      items: [
        'Colours come from Checkbox and Monarch tokens, so every theme and dark mode apply.',
        'Forced-colours mode keeps the divider under select all visible.',
        'Indentation uses logical properties, so it mirrors in RTL.',
      ],
    },
    examples: {
      items: [
        {
          title: 'With select all',
          render: () => (
            <CheckboxGroup
              label="Notify me by"
              selectAll
              defaultValue={['email']}
              options={[
                { value: 'email', label: 'Email', description: 'To your sign-in address.' },
                { value: 'sms', label: 'Text message' },
                { value: 'push', label: 'Push notification' },
                { value: 'post', label: 'Post', disabled: true },
              ]}
            />
          ),
        },
        { title: 'Required, in a Field', render: () => <AtLeastOne /> },
        {
          title: 'Horizontal',
          render: () => (
            <CheckboxGroup
              label="Days"
              orientation="horizontal"
              defaultValue={['mon', 'wed', 'fri']}
              options={[
                { value: 'mon', label: 'Mon' },
                { value: 'tue', label: 'Tue' },
                { value: 'wed', label: 'Wed' },
                { value: 'thu', label: 'Thu' },
                { value: 'fri', label: 'Fri' },
              ]}
            />
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Phrase labels positively ("Email me"), so checked means yes.',
        'Use select all only when choosing everything is a common answer.',
        'Validate minimum or maximum counts and show a FieldError.',
      ],
      dont: [
        'Do not use it for a single yes/no: use Checkbox or Switch.',
        'Do not use it for exclusive options: use RadioGroup.',
        'Do not nest groups more than one level deep.',
      ],
    },
    related: {
      items: [
        { id: 'checkbox', reason: 'A single checkbox, and the control this group uses.' },
        { id: 'radio-group', reason: 'When only one option can be chosen.' },
        { id: 'multi-select', reason: 'Many options with search.' },
        { id: 'switch-settings-list', reason: 'Settings that apply immediately.' },
        { id: 'form-field', reason: 'Label, help and error wiring.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['checkbox', 'form-field'],
    },
    testing: {
      unit: [
        'Labelled group of named, described checkboxes; disabled choice.',
        'Click and Space toggle; values reported in option order.',
        'Select all: mixed, checked and unchecked states; skips disabled choices; keeps checked disabled choices; custom label.',
        'Controlled mode.',
        'Field integration (label, error, invalid) without duplicate ids or required checkboxes.',
        'Native form submission of every checked value under `name`.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
