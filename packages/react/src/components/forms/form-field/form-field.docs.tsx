import { useState } from 'react';
import {
  Field,
  FieldDescription,
  FieldError,
  FieldLabel,
  Input,
  NativeSelect,
  Switch,
} from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';

function EmailField({ initial = '' }: { initial?: string }) {
  const [v, setV] = useState(initial);
  const error = v.length > 0 && !v.includes('@') ? 'Enter a valid email address.' : null;
  return (
    <Field invalid={!!error} required>
      <FieldLabel>Email</FieldLabel>
      <Input
        type="email"
        value={v}
        onChange={(e) => setV(e.target.value)}
        placeholder="you@example.com"
      />
      <FieldDescription>Used for sign-in codes only.</FieldDescription>
      <FieldError>{error}</FieldError>
    </Field>
  );
}

const TIMEZONES = [
  { value: 'UTC', label: 'UTC' },
  { value: 'Europe/London', label: 'London' },
  { value: 'Asia/Kolkata', label: 'Kolkata' },
  { value: 'America/New_York', label: 'New York' },
];

const SORTS = [
  { value: 'updated', label: 'Last updated' },
  { value: 'created', label: 'Newest' },
  { value: 'priority', label: 'Priority' },
];

function DigestRow() {
  const [on, setOn] = useState(true);
  return (
    <Field layout="row">
      <FieldLabel>Weekly digest</FieldLabel>
      <FieldDescription>A summary of activity, sent on Monday mornings.</FieldDescription>
      <Switch checked={on} onCheckedChange={setOn} />
    </Field>
  );
}

export default {
  id: 'form-field',
  title: 'Field',
  summary: 'Label, control, help and error wired together with the right ARIA.',
  sections: {
    purpose: {
      summary:
        'Groups a label, one form control, help text and an error message, and wires their ids and ARIA attributes together so every Monarch control is labelled and described correctly.',
      useWhen: [
        'Any Monarch form control (Input, Textarea, Select, Checkbox, Switch, Combobox) needs a visible label, help text or validation message.',
        'A custom control should pick up the same wiring: call `useFieldControl(props)` inside it.',
        'The platform picker is the better control (long lists on phones, no-JavaScript forms, compact toolbar filters): use NativeSelect.',
        'A Switch needs a label and description beside it: `layout="row"`.',
      ],
      avoidWhen: [
        'A Checkbox or Switch with its own inline `label` prop: it already labels itself.',
        'A control labelled only by surrounding context (a search box with `aria-label`).',
      ],
    },
    designPrinciples: {
      items: [
        'Wiring is automatic: the control receives `id`, `aria-describedby`, `aria-invalid`, `aria-required` and `disabled` from the Field.',
        '`aria-describedby` only names parts that are rendered: FieldDescription and FieldError register themselves.',
        'Explicit props on the control always win, so controls also work outside a Field.',
        'FieldError can always be present: it renders nothing when its children are empty.',
        'NativeSelect is the real `<select>`: it keeps the platform picker, form submission and typeahead, and only restyles the closed control.',
        'Layout is a prop, not a new component: `layout="row"` rearranges the same parts.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        '`Field` renders a `<div class="mn-field">` and provides context; `FieldLabel` forwards its ref to the `<label>`.',
        '`useFieldControl(props, { invalid })` merges Field context into a control’s props; `useFieldContext()` returns the raw context, or null outside a Field.',
        '`NativeSelect` forwards its ref to the `<select>`; all native select attributes pass through. It renders `<span class="mn-native-select">` around the select and its chevron.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        '`Field` `id` overrides the generated control id; the label, description and error ids derive from it (`<id>-label`, `<id>-description`, `<id>-error`).',
      ],
    },
    variants: {
      items: [
        {
          prop: 'FieldLabel optionalText',
          values: ['string'],
          description:
            'Text after the label for optional fields, for example "Optional". Not shown when the Field is `required`, which shows a red asterisk instead.',
        },
        {
          prop: 'Field layout',
          values: ['stack', 'row'],
          description:
            '`stack` (default) is a column. `row` puts the label and description on the left and the control on the right, with the error under both: the switch-with-description row. Exposed as `data-layout="row"`.',
        },
        {
          prop: 'NativeSelect size',
          values: ['sm', 'md', 'lg'],
          description:
            'Height from `--mn-control-*`, matching Input. `sm` (2rem) is the compact toolbar size.',
        },
        {
          prop: 'NativeSelect label',
          values: ['ReactNode'],
          description:
            'Inline visible label before the control, for toolbars outside a Field (`data-inline-label`). Inside a Field, use FieldLabel instead.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Invalid',
          attribute: 'data-invalid on the Field, aria-invalid="true" on the control',
          description:
            'The error message is added to `aria-describedby` only while the Field is invalid and FieldError has content.',
        },
        {
          name: 'Required',
          attribute: 'required and aria-required="true" on the control',
          description:
            'FieldLabel shows an `aria-hidden` asterisk; the requirement is announced from the control.',
        },
        {
          name: 'Disabled',
          attribute: 'data-disabled on the Field, disabled on the control',
          description: 'The label switches to the disabled text colour.',
        },
        {
          name: 'NativeSelect placeholder',
          attribute: '<option value="" disabled> when required',
          description:
            '`placeholder` adds an empty first option; on a required control it is disabled so it cannot be chosen again.',
        },
      ],
    },
    interaction: {
      items: [
        'Clicking the label focuses the control (`htmlFor` points at the control id).',
        'When validation changes, FieldError registers and unregisters itself, so `aria-describedby` and `aria-invalid` update with it.',
        'FieldError is an `aria-live="polite"` region, so a new message is announced.',
        'NativeSelect opens the platform picker; clicking its inline `label` focuses it.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Tab',
          action: 'Moves to the control; Field and its label add no tab stop.',
        },
        {
          keys: 'Arrow keys, typing, Space / Alt+ArrowDown',
          action:
            'NativeSelect: the platform behaviour of `<select>` (change option, typeahead, open the picker).',
        },
      ],
      notes: [
        'Field adds no key handling of its own. Keyboard behaviour is the control’s; the label is reachable by clicking only.',
      ],
    },
    accessibility: {
      name: 'FieldLabel, linked to the control with `htmlFor`.',
      items: [
        'The control is described by FieldDescription and, while invalid, FieldError, in that order.',
        'The required asterisk is `aria-hidden`; `aria-required` carries the meaning.',
        'Select trigger receives `aria-required` but not `required`, which is invalid on a button.',
        'Ids come from `useId`, so they are stable between server and client render.',
        'NativeSelect is a real `<select>` (role combobox or listbox by browser), so screen readers announce it natively. Outside a Field, give it `label` or `aria-label`.',
        'The chevron is decorative (`aria-hidden`).',
      ],
    },
    responsive: {
      items: [
        'A vertical stack (label, control, help, error) that fills its container at every width.',
        '`layout="row"` keeps the control on the right at every width; the text column shrinks and wraps first (`minmax(0, 1fr)`).',
        'NativeSelect fills a stacked Field; on its own it is inline and shrinks with an ellipsis rather than overflowing.',
      ],
    },
    theme: {
      items: [
        'Label, help and error colours are semantic tokens (`--mn-color-text-*`, `--mn-color-danger-strong`), so they follow light, dark and more-contrast themes.',
        'The error colour does not change with the product accent.',
        'NativeSelect reuses the Input surface, border and focus tokens; the open picker is drawn by the platform and follows `color-scheme`.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Validation wiring',
          stack: true,
          render: () => <EmailField initial="not-an-email" />,
        },
        {
          title: 'Native select',
          stack: true,
          render: () => (
            <Field required>
              <FieldLabel>Time zone</FieldLabel>
              <NativeSelect options={TIMEZONES} placeholder="Choose a time zone" defaultValue="" />
              <FieldDescription>Used for reminders and reports.</FieldDescription>
            </Field>
          ),
        },
        {
          title: 'Compact toolbar select',
          render: () => (
            <NativeSelect size="sm" label="Sort" options={SORTS} defaultValue="updated" />
          ),
        },
        {
          title: 'Switch with description',
          stack: true,
          render: () => <DigestRow />,
        },
      ],
    },
    doDont: {
      do: [
        'Keep FieldError mounted and pass `null` when there is no error, so the wiring updates in place.',
        'Set `invalid` on the Field, not on the control, when the control is inside a Field.',
        'Write errors that say how to fix the value ("Enter a valid email address.").',
      ],
      dont: [
        'Do not put more than one control in a Field: the label and descriptions point at one id.',
        'Do not rely on the asterisk alone to explain a required field when most fields are optional; use `optionalText` on the optional ones instead.',
        'Do not style a raw `<select>` per page: use NativeSelect so it picks up Field wiring and tokens.',
      ],
    },
    related: {
      items: [
        { id: 'text-field', reason: 'Input and Textarea, the most common Field controls.' },
        { id: 'select', reason: 'Single-value picker that reads Field context.' },
        { id: 'combobox', reason: 'Searchable picker that reads Field context.' },
        { id: 'settings-section', reason: 'SettingsRow builds on Field for settings pages.' },
        { id: 'switch', reason: 'The control in a `layout="row"` switch row.' },
        {
          id: 'switch-settings-list',
          reason:
            'A list of switch rows that each save on their own, with saving and error states.',
        },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: [],
    },
    testing: {
      unit: [
        'Wires label, description and error to the control (required, aria-invalid, accessible description).',
        'Only references descriptions that exist.',
        'Adds and removes the error reference as validation changes.',
        'Propagates `disabled` to the control.',
        'Works in StrictMode (double effects).',
        'Text-field tests prove explicit props win outside a Field; Select tests prove a trigger gets aria-required, not required.',
        'NativeSelect: Field wiring (name, description, error, required), disabled placeholder and options, uncontrolled and controlled changes, inline label naming and focus, Tab focus, disabled from Field.',
        '`layout="row"` labels and describes a Switch, the label toggles it, and the default stacked markup has no `data-layout`.',
      ],
      axe: true,
      ssr: true,
      browser: ['Visual baselines: /__gallery', 'Cross-browser axe: /components/field'],
    },
  },
} satisfies ComponentDocs;
