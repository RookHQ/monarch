import { useState } from 'react';
import { FormBuilder, type FormFieldDefinition } from '@rookcom/monarch-react/patterns';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const START: FormFieldDefinition[] = [
  { id: 'intro', type: 'section', label: 'About your visit' },
  {
    id: 'where',
    type: 'radio',
    label: 'Where will you join from?',
    required: true,
    options: [
      { value: 'remote', label: 'Remote' },
      { value: 'office', label: 'Office' },
    ],
  },
  {
    id: 'desk',
    type: 'short-text',
    label: 'Desk number',
    conditions: [{ id: 'c1', field: 'where', operator: 'eq', value: 'office' }],
  },
  { id: 'notes', type: 'long-text', label: 'Anything we should know?' },
];

function BuilderDemo() {
  const [fields, setFields] = useState(START);
  return (
    <FormBuilder
      style={{ width: '100%' }}
      value={fields}
      onChange={setFields}
      contextFields={[
        {
          value: '__service',
          label: 'Service',
          type: 'select',
          group: 'Booking details',
          options: [
            { value: 'intro', label: 'Intro call' },
            { value: 'review', label: 'Review' },
          ],
        },
      ]}
      previewContext={{ __service: 'intro' }}
    />
  );
}

export default {
  id: 'form-builder',
  title: 'FormBuilder',
  summary:
    'Builds a form from fields: a type palette, a reorderable list, per-field settings with show-if logic, and a live preview.',
  sections: {
    purpose: {
      summary:
        'Lets admins design the questions people answer, as data the product saves and publishes. Needed by: Calendar (booking forms), Recruit (application forms), Tickets (request forms).',
      useWhen: [
        'Admins add, order and configure questions without a developer.',
        'Some questions should appear only when earlier answers match.',
      ],
      avoidWhen: [
        'The form is fixed in code: build it with Field and inputs.',
        'You only render stored questions: use FieldRenderer.',
      ],
    },
    designPrinciples: {
      items: [
        'Three places, one job each: add (palette), order (list), configure (settings), plus a preview that behaves like the live form.',
        'New fields go after the selected one and their question is focused at once.',
        'Removing a field is reversible: it happens at once with an Undo, and conditions that tested it are dropped with it.',
        'Conditions may only use earlier questions (and `contextFields`), so a form never depends on something not yet answered.',
        'The product owns saving, publishing and versions; problems come in through `errors` and `showErrors`.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Controlled: `value` (FieldRenderer’s `FormFieldDefinition[]`) + `onChange`. Selection is internal unless you pass `selectedId`.',
        '`fieldTypes` limits and orders the palette; `contextFields` adds non-question condition sources, with values in `previewContext`.',
        'Helpers: `validateFormDefinition` (missing question, missing options, conditions out of order or incomplete), `formFieldToConditionField`, `formFieldHasOptions`.',
        'The ref is the root `<div>`.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Migrating from the app copies: Calendar `FormBuilder` + `FieldList` + `FieldSettings` + `FormPreview` → this component; `fields` → `value`, `checkFields` → `validateFormDefinition`, server `fieldErrors` → `errors`, `readOnly` stays, `PALETTE_GROUPS` → `fieldTypes` with `group`, booking details (`__service`, `__host`) → `contextFields`. Keep the page header, name, Save draft and Publish in the product. Recruit `ApplicationFormBuilder`: block types map as in FieldRenderer; system blocks that cannot be removed should be kept outside `value` or protected by the product. Tickets `FormEditor`: its field references (`fieldId`, `required`, `showWhen`) become definitions with `required` and `conditions`.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'readOnly',
          values: ['true', 'false'],
          description: 'View only: no palette, no reordering, settings disabled.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Selected field',
          attribute: 'data-selected, aria-pressed',
          description: 'Highlighted row; its settings are shown.',
        },
        {
          name: 'Field with a problem',
          attribute: 'data-invalid',
          description: 'Danger border and the problem under the label.',
        },
        { name: 'Removed, undo available', description: 'A status line with Undo.' },
        { name: 'Empty', description: 'EmptyState pointing to the palette.' },
        { name: 'Read-only', attribute: 'data-read-only', description: 'Editing is off.' },
      ],
    },
    interaction: {
      items: [
        'Palette buttons add a field after the selected one and select it.',
        'Rows select on click; duplicate and remove sit at the end of each row.',
        'The Preview tab renders visible fields with FieldRenderer and applies show-if logic as you answer.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Moves through tabs, palette, rows and settings.' },
        { keys: 'Arrow Left / Right', action: 'Switches between Build and Preview.' },
        {
          keys: 'Space / Enter on a handle',
          action: 'Picks a field up or drops it; arrows move it.',
        },
      ],
    },
    accessibility: {
      role: 'tabs; the field list is a list; palette and settings are labelled regions',
      name: 'Field rows are named "Edit <question>".',
      items: [
        'Selected rows expose `aria-pressed="true"`.',
        'Every setting has a visible label; option inputs are named "Option n".',
        'Removal and undo are announced in a polite status region.',
      ],
    },
    responsive: {
      items: [
        'Three columns on wide screens, two (palette on top) on tablets and one column on phones. Works at 320px.',
        'On coarse pointers, rows, handles, inputs and buttons are at least 44px.',
      ],
    },
    theme: {
      items: [
        'Semantic tokens only; selection uses the accent border, problems the danger tokens.',
        'Forced colours mark the selected row with `Highlight`.',
        'Nothing animates here; reordering honours reduced motion.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Booking form with conditional question',
          stack: true,
          render: () => <BuilderDemo />,
        },
      ],
    },
    doDont: {
      do: [
        'Run `validateFormDefinition` before saving and pass `showErrors`.',
        'Keep fields the product requires (name, email) outside the builder or locked by the product.',
      ],
      dont: [
        'Do not let conditions point at later questions.',
        'Do not confirm every field removal; Undo is enough for a draft.',
      ],
    },
    related: {
      items: [
        { id: 'field-renderer', reason: 'Renders the fields this builds.' },
        { id: 'condition-editor', reason: 'The show-if logic editor.' },
        { id: 'schema-driven-form', reason: 'Forms defined in code as a schema.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: [
        'badge',
        'button',
        'checkbox',
        'condition-editor',
        'empty-state',
        'field-renderer',
        'form-field',
        'reorderable-list',
        'tabs',
        'text-field',
      ],
    },
    testing: {
      unit: [
        'Palette, list and settings render with names; axe passes.',
        'Adding inserts after the selection and focuses the question.',
        'Settings: required, options add / remove, limits.',
        'Only earlier fields can drive conditions.',
        'Remove with undo drops dependent conditions; duplicate and keyboard reorder.',
        'Problems with `showErrors`; preview with show-if logic (axe passes); read-only; empty; helpers; server rendering.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
