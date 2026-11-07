import { useState } from 'react';
import {
  FieldRenderer,
  validateFormField,
  type FormFieldDefinition,
  type FormFieldValue,
} from '@rookcom/monarch-react/patterns';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const FIELDS: FormFieldDefinition[] = [
  { id: 'name', type: 'short-text', label: 'Full name', required: true },
  {
    id: 'email',
    type: 'email',
    label: 'Email',
    required: true,
    description: 'We only use it to reply.',
  },
  {
    id: 'where',
    type: 'radio',
    label: 'Where will you join from?',
    options: [
      { value: 'remote', label: 'Remote' },
      { value: 'office', label: 'Office' },
    ],
  },
  { id: 'relocate', type: 'yes-no', label: 'Can you relocate?' },
  { id: 'rating', type: 'rating', label: 'How was the process?' },
  {
    id: 'consent',
    type: 'consent',
    label: 'Consent',
    consentText: 'I agree to be contacted about this request.',
    required: true,
  },
];

function FormDemo() {
  const [values, setValues] = useState<Record<string, FormFieldValue>>({ name: 'Alex Morgan' });
  const [touched, setTouched] = useState<Record<string, boolean>>({});
  return (
    <form style={{ display: 'grid', gap: 16, width: '100%', maxWidth: 520 }}>
      {FIELDS.map((f) => (
        <FieldRenderer
          key={f.id}
          field={f}
          value={values[f.id]}
          confidence={f.id === 'name' ? 0.92 : undefined}
          error={touched[f.id] ? validateFormField(f, values[f.id]) : null}
          onChange={(v: FormFieldValue) => {
            setValues((s) => ({ ...s, [f.id]: v }));
            setTouched((t) => ({ ...t, [f.id]: true }));
          }}
        />
      ))}
    </form>
  );
}

export default {
  id: 'field-renderer',
  title: 'FieldRenderer',
  summary:
    'Renders one form question from data: the right input for its type, with label, help, required marker, error and an optional automatic-fill note.',
  sections: {
    purpose: {
      summary:
        'Turns a field definition (as stored by a form builder) into an accessible, working input, so public forms, previews and internal tools render questions the same way. Needed by: Recruit (application forms, with values filled in from an uploaded CV), Calendar (booking forms), Tickets (request forms and pre-chat forms).',
      useWhen: [
        'Questions are authored by admins and stored as data.',
        'A form shows answers that were filled in automatically and people must check them.',
      ],
      avoidWhen: [
        'The form is fixed in code: compose Field, Input and friends directly.',
        'You need a whole form with submit and validation from a schema: use SchemaDrivenForm.',
      ],
    },
    designPrinciples: {
      items: [
        'One definition shape (`FormFieldDefinition`) for builder, preview and live form.',
        'Questions with several inputs (single choice, yes / no, checkboxes) are a fieldset with a legend; the rest sit in a Field.',
        'Automatic answers say so with the assistant glyph; low confidence asks for a check instead of guessing silently.',
        'Validation is a pure helper (`validateFormField`) so the product decides when to show errors.',
        'Show-if logic reuses ConditionEditor’s conditions and `evaluateConditions`.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Controlled: `value` + `onChange`. Values are text, numbers, booleans (yes / no, consent), lists of option values, or files (`{ name, size?, file? }`).',
        'Helpers: `validateFormField`, `isFormFieldVisible`, `isFormFieldValueEmpty`.',
        'File fields hand chosen files to the product through `onChange`; the product uploads them.',
        'Difference from SchemaDrivenForm: that renders and submits a whole schema; FieldRenderer renders one admin-authored question and leaves the form to you.',
        'The ref is the root `<div>`.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Migrating from the app copy (Recruit `FieldRenderer`): `field.type` SHORT_TEXT / LONG_TEXT / EMAIL / URL / NUMBER / DATE / PHONE / BOOLEAN / CONSENT / RADIO / CHECKBOX_GROUP / SELECT / MULTI_SELECT / RATING / SECTION_HEADER → `short-text` / `long-text` / `email` / `url` / `number` / `date` / `phone` / `yes-no` / `consent` / `radio` / `checkboxes` / `select` / `multi-select` / `rating` / `section`; `helpText` → `description`; `config.min/max/rows/maxLength` → top-level props; `options[].id` → `value`; `isSystem` → `readOnly`; `confidence` stays `confidence`; `dependsOn` → `conditions`. Currency, country / region / city and dial-code phone inputs stay in the product (render them next to FieldRenderer). Calendar `FormPreview` field types map the same way (`yes_no` → `yes-no`, `checkboxes`, `info` → `section`).',
      ],
    },
    variants: {
      items: [
        {
          prop: 'field.type',
          values: [
            'short-text',
            'long-text',
            'email',
            'phone',
            'url',
            'number',
            'date',
            'time',
            'select',
            'multi-select',
            'radio',
            'checkboxes',
            'yes-no',
            'consent',
            'rating',
            'file',
            'section',
          ],
          description: 'The input rendered.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Invalid',
          attribute: 'data-invalid, aria-invalid',
          description: 'The error shows under the input and describes it.',
        },
        {
          name: 'Filled automatically',
          attribute: 'data-ai="filled"',
          description: 'An assistant note under the label.',
        },
        {
          name: 'Needs a check',
          attribute: 'data-ai="check"',
          description:
            'Confidence below `confidenceThreshold`: a warning note and a warning border.',
        },
        { name: 'Read-only', description: 'Text inputs are read-only; other inputs are disabled.' },
        { name: 'Disabled', description: 'Every input is disabled.' },
      ],
    },
    interaction: {
      items: [
        'Every change calls `onChange` with the new answer in its natural shape.',
        'Chosen files are listed with a remove button.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Moves to the input (or the first choice of a group).' },
        { keys: 'Arrow keys', action: 'Move between single choices and rating values.' },
        { keys: 'Space', action: 'Ticks a checkbox or consent.' },
      ],
    },
    accessibility: {
      role: 'Field (label + control) or fieldset with legend',
      name: 'The field `label`.',
      items: [
        'Help text, the automatic-fill note and the error are linked with `aria-describedby`.',
        'Required fields are marked visually and with `required` / `aria-required`.',
        'The automatic-fill note is text, not just an icon; the glyph is decorative.',
      ],
    },
    responsive: {
      items: [
        'Inputs take the full width; yes / no choices sit on one line and wrap. Works at 320px.',
        'On coarse pointers, choices, inputs and buttons are at least 44px tall.',
      ],
    },
    theme: {
      items: [
        'Semantic tokens only; the automatic-fill note uses `--mn-color-ai-*`, the check note the warning tokens.',
        'Forced colours draw note borders in `CanvasText`.',
        'Nothing animates.',
      ],
    },
    examples: {
      items: [
        { title: 'A short public form', stack: true, render: () => <FormDemo /> },
        {
          title: 'Filled in automatically, needs a check',
          stack: true,
          render: () => (
            <FieldRenderer
              style={{ width: '100%', maxWidth: 520 }}
              field={{ id: 'phone', type: 'phone', label: 'Phone' }}
              value="+44 20 7946 0000"
              confidence={0.4}
              onChange={() => {}}
            />
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Validate with `validateFormField` on blur or submit, not on every keystroke from empty.',
        'Pass `confidence` only for answers that were really filled in automatically.',
      ],
      dont: [
        'Do not flag every automatic answer as needing a check; people learn to ignore it.',
        'Do not use sections as questions; they have no answer.',
      ],
    },
    related: {
      items: [
        { id: 'form-builder', reason: 'Author the fields this renders.' },
        { id: 'schema-driven-form', reason: 'A whole form with submit from a schema.' },
        { id: 'condition-editor', reason: 'The show-if conditions.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: [
        'button',
        'checkbox',
        'combobox',
        'condition-editor',
        'date-picker',
        'file-upload',
        'form-field',
        'rating-picker',
        'text-field',
      ],
    },
    testing: {
      unit: [
        'Text input with label, help, required and error wiring; axe passes.',
        'Text, long text and number values.',
        'Radio, yes / no and checkboxes as fieldsets operated from the keyboard; axe passes.',
        'Select, consent and rating.',
        'Automatic-fill note and the low-confidence check.',
        'Section heading, read-only, file list with remove.',
        'Validation and visibility helpers; server rendering.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
