import { useState } from 'react';
import { Button } from '@rookcom/monarch-react';
import {
  SchemaDrivenForm,
  type SchemaDrivenFormSchema,
  type SchemaDrivenFormValues,
} from '@rookcom/monarch-react/patterns';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const BOOKING: SchemaDrivenFormSchema = [
  {
    type: 'section',
    id: 'you',
    title: 'Your details',
    fields: [
      { type: 'text', name: 'name', label: 'Full name', required: true, autoComplete: 'name' },
      {
        type: 'text',
        name: 'email',
        label: 'Email',
        inputType: 'email',
        required: true,
        autoComplete: 'email',
        pattern: '[^@\\s]+@[^@\\s]+\\.[^@\\s]+',
        patternMessage: 'Enter an email address, such as name@example.com.',
      },
    ],
  },
  {
    type: 'section',
    id: 'visit',
    title: 'Your visit',
    description: 'Tell us what to prepare.',
    fields: [
      {
        type: 'select',
        name: 'topic',
        label: 'Topic',
        required: true,
        options: [
          { value: 'demo', label: 'Product demo' },
          { value: 'support', label: 'Support question' },
          { value: 'other', label: 'Something else' },
        ],
      },
      {
        type: 'textarea',
        name: 'other',
        label: 'What would you like to discuss?',
        required: true,
        maxLength: 500,
        visibleWhen: { field: 'topic', equals: 'other' },
      },
      { type: 'number', name: 'guests', label: 'Guests', min: 1, max: 10, integer: true },
      { type: 'date', name: 'date', label: 'Preferred date', min: '2026-01-01' },
      {
        type: 'switch',
        name: 'reminder',
        label: 'Email reminder',
        description: 'One day before.',
        defaultValue: true,
      },
      {
        type: 'checkbox',
        name: 'consent',
        label: 'I agree to be contacted about this booking',
        required: true,
      },
    ],
  },
];

const CONNECTION: SchemaDrivenFormSchema = [
  { type: 'text', name: 'url', label: 'Server URL', inputType: 'url', required: true },
  {
    type: 'text',
    name: 'token',
    label: 'Access token',
    inputType: 'password',
    required: true,
    description: 'Found in your server’s admin settings.',
  },
  {
    type: 'number',
    name: 'timeout',
    label: 'Timeout (seconds)',
    min: 1,
    max: 120,
    defaultValue: 30,
  },
  { type: 'checkbox', name: 'verifyTls', label: 'Verify TLS certificates', defaultValue: true },
];

function BookingDemo() {
  const [sent, setSent] = useState<SchemaDrivenFormValues | null>(null);
  return (
    <div style={{ display: 'grid', gap: 16, width: '100%', maxWidth: 520 }}>
      <SchemaDrivenForm
        aria-label="Booking form"
        schema={BOOKING}
        locale="en-GB"
        submitLabel="Book"
        onSubmit={async (values) => {
          await new Promise((r) => setTimeout(r, 700));
          setSent(values);
        }}
      />
      {sent ? (
        <pre style={{ margin: 0, fontSize: 12, whiteSpace: 'pre-wrap' }}>
          {JSON.stringify(sent, null, 2)}
        </pre>
      ) : null}
    </div>
  );
}

export default {
  id: 'schema-driven-form',
  title: 'SchemaDrivenForm',
  summary:
    'A form rendered from a field schema: seven field types, required and validation rules, sections and conditional fields.',
  sections: {
    purpose: {
      summary:
        'Renders forms whose fields are data rather than code: booking and intake forms people build, custom fields, integration settings defined by a provider list. The product owns the schema and what happens on submit. Import from `@rookcom/monarch-react/patterns`.',
      useWhen: [
        'The fields come from configuration or the server (custom fields, form builders, integration settings).',
        'The same fields must render identically in a builder preview and the live form.',
      ],
      avoidWhen: [
        'A fixed form written once: compose Field, Input and friends directly, so each field can be tuned.',
        'Settings that save as each switch flips: use SwitchSettingsList.',
      ],
    },
    designPrinciples: {
      items: [
        'Small and typed: a discriminated union of seven field types, no form library, no runtime schema parser.',
        'The schema is data: conditions such as `{ field: "topic", equals: "other" }` can come from JSON; a function is allowed when data cannot say it.',
        'Built on Field, so every label, help text and error is wired the same way as hand-written Monarch forms.',
        'Errors appear when a field is left or on submit, never while someone is still typing their first character.',
        'Hidden fields are neither validated nor submitted, so a condition can never block the form invisibly.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Renders a `<form noValidate>` (ref); sections are `<fieldset>` with a `<legend>`. Each field wrapper has `data-schema-field` and `data-type`.',
        'Fields: `text` (with `inputType` email, url, tel or password), `textarea`, `number`, `select`, `checkbox`, `switch` and `date`. Values are strings, numbers, booleans, `YYYY-MM-DD` strings or `null`.',
        'Rules: `required`, `minLength`, `maxLength`, `pattern` (a string, anchored), `min`, `max`, `integer` and a `validate(value, values)` function.',
        'Helpers: `schemaDefaultValues`, `isSchemaFieldVisible`, `validateSchemaField`, `validateSchemaForm` (reuse them on the server) and `formatSchemaValue` for read-only display.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [
        {
          prop: 'validateOn',
          values: ['blur', 'submit'],
          description:
            '`blur` (default) shows a field’s error once it loses focus; `submit` waits for the first submit.',
        },
        {
          prop: 'readOnly',
          values: ['false', 'true'],
          description:
            '`true` shows the visible values as a description list, formatted with `formatSchemaValue`.',
        },
        {
          prop: 'submitLabel',
          values: ['“Save” (default)', 'any text', 'null'],
          description:
            '`null` hides the button, for a submit button elsewhere (`form` attribute) or a dialog footer.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Invalid',
          attribute: 'aria-invalid on the control',
          description: 'Error under the field, linked by `aria-describedby`.',
        },
        { name: 'Required', attribute: 'aria-required', description: 'Asterisk after the label.' },
        {
          name: 'Hidden',
          description: 'A field or section whose `visibleWhen` does not hold is not rendered.',
        },
        {
          name: 'Submitting',
          attribute: 'aria-busy on the form and button',
          description: 'Fields disabled while the `onSubmit` promise runs.',
        },
        {
          name: 'Submit failed',
          attribute: 'role="alert"',
          description: 'The rejection’s message above the buttons.',
        },
        { name: 'Disabled', description: 'Every field and the submit button disabled.' },
        {
          name: 'Read-only',
          attribute: 'data-readonly',
          description: 'A description list instead of controls.',
        },
      ],
    },
    interaction: {
      items: [
        'Each change calls `onValuesChange(values, name)`; conditional fields appear and disappear immediately.',
        'Submit validates every visible field; if any fail, focus moves to the first invalid one and `onSubmit` is not called.',
        'Valid submits call `onSubmit` with only the visible fields’ values. Return a promise to show the busy state; reject to show an error.',
        'Number fields keep partial text such as “-” or “1.” while the value stays a number.',
        'Product errors (for example from the server) go in `errors` by field name and show at once.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab / Shift+Tab', action: 'Moves between fields and buttons in schema order.' },
        { keys: 'Enter', action: 'In a single-line field, submits the form.' },
        { keys: 'Space', action: 'Toggles a checkbox or switch.' },
      ],
      notes: ['The date field adds DatePicker’s calendar keys; selects use the platform’s keys.'],
    },
    accessibility: {
      role: 'form (when named) containing groups',
      name: 'Give the form an `aria-label` or `aria-labelledby`; sections are named by their legend.',
      items: [
        'Labels, descriptions and errors are wired by Field; required fields have `aria-required` and a visual asterisk.',
        'Section descriptions are linked to their fieldset with `aria-describedby`.',
        'Errors are announced politely when they appear; a failed submit is an alert.',
        'Focus moves to the first invalid field on submit, so keyboard and screen-reader users land on the problem.',
        'Verified with axe in jsdom and in Chromium, Firefox and WebKit on /components/schema-driven-form.',
      ],
    },
    responsive: {
      items: [
        'One column at every width; controls fill the form’s width, number fields are capped at 12rem.',
        'The read-only list puts labels beside values, and stacks them under 30rem.',
      ],
    },
    theme: {
      items: [
        'Monarch tokens only; sections are separated by `--mn-color-border-subtle`.',
        'Every control is a Monarch control, so light, dark, high-contrast and product accents apply unchanged.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Booking form with sections and a conditional field',
          stack: true,
          render: () => <BookingDemo />,
          code: `const schema = [
  { type: 'section', id: 'you', title: 'Your details', fields: [
    { type: 'text', name: 'name', label: 'Full name', required: true },
    { type: 'text', name: 'email', label: 'Email', inputType: 'email', required: true },
  ] },
  { type: 'section', id: 'visit', title: 'Your visit', fields: [
    { type: 'select', name: 'topic', label: 'Topic', required: true, options },
    { type: 'textarea', name: 'other', label: 'What would you like to discuss?',
      required: true, visibleWhen: { field: 'topic', equals: 'other' } },
    { type: 'number', name: 'guests', label: 'Guests', min: 1, max: 10, integer: true },
    { type: 'date', name: 'date', label: 'Preferred date' },
    { type: 'switch', name: 'reminder', label: 'Email reminder', defaultValue: true },
    { type: 'checkbox', name: 'consent', label: 'I agree to be contacted…', required: true },
  ] },
];

<SchemaDrivenForm aria-label="Booking form" schema={schema} submitLabel="Book" onSubmit={save} />`,
        },
        {
          title: 'Integration settings with a Cancel action',
          stack: true,
          render: () => (
            <div style={{ width: '100%', maxWidth: 520 }}>
              <SchemaDrivenForm
                aria-label="Connection settings"
                schema={CONNECTION}
                submitLabel="Connect"
                secondaryActions={<Button variant="ghost">Cancel</Button>}
              />
            </div>
          ),
          code: `<SchemaDrivenForm
  schema={providerFields}
  submitLabel="Connect"
  secondaryActions={<Button variant="ghost">Cancel</Button>}
  errors={serverErrors}
  onSubmit={connect}
/>`,
        },
        {
          title: 'Read-only',
          stack: true,
          render: () => (
            <div style={{ width: '100%', maxWidth: 520 }}>
              <SchemaDrivenForm
                schema={BOOKING}
                readOnly
                locale="en-GB"
                defaultValues={{
                  name: 'Amara Okafor',
                  email: 'amara@example.com',
                  topic: 'demo',
                  guests: 3,
                  date: '2026-03-12',
                  consent: true,
                }}
              />
            </div>
          ),
          code: `<SchemaDrivenForm schema={schema} values={submission} readOnly locale="en-GB" />`,
        },
      ],
    },
    doDont: {
      do: [
        'Validate again on the server with the same schema (`validateSchemaForm` runs anywhere).',
        'Write `patternMessage` whenever you use `pattern`, with an example of the expected format.',
        'Pass `locale` when rendering on the server, so dates and numbers match the client.',
      ],
      dont: [
        'Do not hide a required field behind a condition and expect it to be filled; hidden fields are skipped.',
        'Do not use a switch for consent; use a required checkbox.',
        'Do not reach for this for one fixed form; hand-written Fields are easier to tune.',
      ],
    },
    related: {
      items: [
        { id: 'form-field', reason: 'The label, help and error wiring every field uses.' },
        { id: 'date-picker', reason: 'The date field.' },
        { id: 'switch-settings-list', reason: 'Settings that apply as each switch flips.' },
        { id: 'description-list', reason: 'Read-only label and value pairs in other layouts.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: [
        'button',
        'alert',
        'checkbox',
        'form-field',
        'switch',
        'text-field',
        'date-picker',
      ],
    },
    testing: {
      unit: [
        '`schemaDefaultValues`, data and function conditions with `isSchemaFieldVisible`.',
        '`validateSchemaForm`: required, must-accept, pattern, integer, maxLength, max and number rules; hidden fields skipped.',
        '`formatSchemaValue`: option labels, Yes/No, empty, locale numbers and dates.',
        'Labelled controls of every type in named fieldsets with descriptions; conditional field hidden; axe.',
        'Error on blur, cleared by typing; conditional field appears; axe.',
        'Submit validates, focuses the first invalid field, then submits only visible values.',
        'Number text keeps partial input and reports text that is not a number.',
        'Controlled values; product errors; rejected submit alert; secondary actions.',
        'Busy while submitting; disabled; no submit button with `submitLabel={null}`.',
        'Read-only description list; axe; translated messages; renders on the server.',
      ],
      axe: true,
      ssr: true,
      browser: [
        'Visual baselines: /__gallery/schema-driven-form',
        'Cross-browser axe: /components/schema-driven-form',
      ],
    },
  },
} satisfies ComponentDocs;
