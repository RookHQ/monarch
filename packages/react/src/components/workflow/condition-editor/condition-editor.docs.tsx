import { useState } from 'react';
import {
  ConditionEditor,
  evaluateConditions,
  type Condition,
  type ConditionField,
  type ConditionMatch,
} from '@rookcom/monarch-react/patterns';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const FIELDS: ConditionField[] = [
  {
    value: 'priority',
    label: 'Priority',
    type: 'select',
    options: [
      { value: 'low', label: 'Low' },
      { value: 'normal', label: 'Normal' },
      { value: 'high', label: 'High' },
      { value: 'urgent', label: 'Urgent' },
    ],
  },
  { value: 'subject', label: 'Subject', type: 'text' },
  {
    value: 'requester.domain',
    label: 'Requester email domain',
    type: 'text',
    placeholder: 'example.com',
  },
  { value: 'hoursOpen', label: 'Hours since created', type: 'number', placeholder: 'Hours' },
  { value: 'businessHours', label: 'Within business hours', type: 'boolean' },
  { value: 'tags', label: 'Tags', type: 'tags', options: [{ value: 'billing', label: 'billing' }] },
  {
    value: 'custom.plan',
    label: 'Plan',
    type: 'select',
    group: 'Custom fields',
    options: [
      { value: 'starter', label: 'Starter' },
      { value: 'team', label: 'Team' },
    ],
  },
];

function EditorDemo() {
  const [conditions, setConditions] = useState<Condition[]>([
    { id: '1', field: 'priority', operator: 'in', value: ['high', 'urgent'] },
    { id: '2', field: 'subject', operator: 'contains', value: 'refund' },
  ]);
  const [match, setMatch] = useState<ConditionMatch>('all');
  const sample = { priority: 'high', subject: 'Refund for order 1042', hoursOpen: 3 };
  return (
    <div style={{ display: 'grid', gap: 12, width: '100%', maxWidth: 720 }}>
      <ConditionEditor
        value={conditions}
        onChange={setConditions}
        fields={FIELDS}
        match={match}
        onMatchChange={setMatch}
      />
      <p style={{ margin: 0 }}>
        A high-priority refund request{' '}
        {evaluateConditions(conditions, sample, match) ? 'matches' : 'does not match'}.
      </p>
    </div>
  );
}

export default {
  id: 'condition-editor',
  title: 'ConditionEditor',
  summary:
    'Edits a list of conditions (field, operator, value) with type-aware operators and value inputs and an all / any switch.',
  sections: {
    purpose: {
      summary:
        'Lets people describe when something applies, as rows of "field operator value". Fields are data, so the same editor serves ticket rules, form logic and payroll eligibility. Needed by: Tickets (routing rules and automations), Calendar (form logic: show a question when), People (payroll rule applicability), Chat (automation conditions).',
      useWhen: [
        'A rule, automation, filter or form question applies only in some situations.',
        'The conditions are a flat list combined with all or any.',
      ],
      avoidWhen: [
        'You need conditions and actions together with a name and an on switch: use RuleBuilder, which composes this.',
        'People filter a list on screen: use FilterBar.',
        'Logic needs nested groups or hand-written formulas: keep a dedicated formula editor.',
      ],
    },
    designPrinciples: {
      items: [
        'The field type decides the operators and the value input: a picker for choices, several choices for "is any of", a number input, a date input, tags, or nothing for "is empty" and "is yes".',
        'Changing the field starts the condition over; changing the operator keeps the value when it still fits (one value becomes a list of one, and back).',
        'Stored values that no longer exist stay visible as "(removed)" so nothing changes silently.',
        'Problems are shown when the product asks (`showErrors`), next to the control, in words.',
        'Evaluation helpers make previews easy; the server stays the authority.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Controlled: `value` + `onChange`. The all / any switch shows when `match` and `onMatchChange` are given and there are two or more conditions.',
        'Fields: `{ value, label, type, options?, group?, operators?, placeholder?, disabled?, disabledReason? }`. `group` lists fields under an optgroup; `disabled` with `disabledReason` keeps a field visible but unavailable (for example a question that comes later).',
        'Helpers: `conditionOperatorsFor`, `conditionOperatorTakesValue`, `createCondition`, `validateCondition`, `validateConditions`, `evaluateCondition`, `evaluateConditions`.',
        'The ref is the root `<div>`.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Migrating from the app copies: Tickets `ConditionGroup` `conditions` → `value`, `{ field, op, value }` → `{ id, field, operator, value }` (add ids), `CONDITION_FIELDS` + `optionsFor(lookups)` → `fields` with `options`, `OP_LABEL` → `messages.operators`, `conditionProblem` → `validateCondition`, `showProblems` → `showErrors`. Render two editors for Tickets’ "all of" and "any of" groups, or one with `match`. Calendar forms `ConditionEditor`: `field.conditions` → `value`, `conditionMode` → `match`, `fieldId` → `field`, operators `equals` / `not_equals` / `includes` / `is_answered` / `is_empty` / `is_true` / `is_false` → `eq` / `neq` / `contains` / `is_not_empty` / `is_empty` / `is_true` / `is_false`; later questions become fields with `disabled` and `disabledReason`; booking details become a `group`. Chat automations: `path` → `field`.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'match',
          values: ['all', 'any'],
          description:
            'Whether every condition must hold or one is enough; shown as a segmented switch.',
        },
        {
          prop: 'ConditionField.type',
          values: ['text', 'number', 'date', 'select', 'multi-select', 'tags', 'boolean'],
          description: 'Decides the operators and the value input of each row.',
        },
      ],
    },
    states: {
      items: [
        { name: 'Empty', description: 'No conditions: `messages.empty` or `emptyContent`.' },
        {
          name: 'Invalid row',
          attribute: 'data-invalid, aria-invalid on the control',
          description:
            'With `showErrors`, the row border turns danger and the problem shows under it.',
        },
        { name: 'Full', description: 'At `maxConditions` the add button becomes a note.' },
        { name: 'Disabled', attribute: 'data-disabled', description: 'Every control is disabled.' },
      ],
    },
    interaction: {
      items: [
        'Add condition starts a row on `defaultField` (or the first available field) with its first operator.',
        'Field, operator and value changes call `onChange` with the full list.',
        'Remove deletes the row at once: conditions are a draft until the product saves.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Tab',
          action:
            'Moves through each row’s field, operator, value and remove button, then Add condition.',
        },
        { keys: 'Arrow keys', action: 'Change a native select, or move between all and any.' },
        { keys: 'Enter / comma', action: 'Adds a tag in a tags value.' },
        {
          keys: 'Arrow Down, Enter',
          action: 'Open and pick in a several-choices value (Combobox).',
        },
      ],
    },
    accessibility: {
      role: 'list of rows; each row is a group named "Condition n"',
      name: '`messages.label`, "Conditions" by default.',
      items: [
        'Every control has a name that includes the row number ("Condition 2 operator").',
        'Problems are linked with `aria-describedby` and set `aria-invalid`.',
        'The all / any switch is a radio group labelled by the visible "Match" text.',
      ],
    },
    responsive: {
      items: [
        'Field, operator and value sit on one line when there is room and stack at narrow widths; works at 320px.',
        'On coarse pointers, selects, inputs and buttons are at least 44px.',
      ],
    },
    theme: {
      items: [
        'Semantic tokens only; invalid rows use `--mn-color-danger-border` and `--mn-color-danger-strong`.',
        'Forced colours draw row borders in `CanvasText` and thicken invalid ones.',
        'Nothing animates.',
      ],
    },
    examples: {
      items: [
        { title: 'Ticket conditions with a live check', stack: true, render: () => <EditorDemo /> },
        {
          title: 'Show a question when (form logic)',
          stack: true,
          render: () => (
            <ConditionEditor
              style={{ width: '100%', maxWidth: 720 }}
              value={[{ id: '1', field: 'attending', operator: 'is_true' }]}
              onChange={() => {}}
              fields={[
                { value: 'attending', label: 'Will you attend in person?', type: 'boolean' },
                {
                  value: 'diet',
                  label: 'Dietary needs',
                  type: 'multi-select',
                  options: [
                    { value: 'vegetarian', label: 'Vegetarian' },
                    { value: 'vegan', label: 'Vegan' },
                  ],
                },
                {
                  value: 'company',
                  label: 'Company',
                  type: 'text',
                  disabled: true,
                  disabledReason: 'comes after this question',
                },
              ]}
            />
          ),
        },
        {
          title: 'Empty',
          render: () => <ConditionEditor value={[]} onChange={() => {}} fields={FIELDS} />,
        },
      ],
    },
    doDont: {
      do: [
        'Give fields the most specific type, so people get the right operators and inputs.',
        'Explain in the surrounding copy what an empty list means.',
        'Validate on save with `validateConditions` and pass `showErrors`.',
      ],
      dont: [
        'Do not drop a stored value just because its option was removed; let people see and fix it.',
        'Do not offer operators the server cannot evaluate; pass `operators` per field.',
      ],
    },
    related: {
      items: [
        { id: 'rule-builder', reason: 'A complete rule: name, trigger, conditions and actions.' },
        { id: 'action-editor', reason: 'What happens when the conditions match.' },
        { id: 'filter-bar', reason: 'Filtering a list on screen.' },
        {
          id: 'form-builder',
          reason: 'Uses conditions to show questions only in some situations.',
        },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: [
        'button',
        'combobox',
        'form-field',
        'segmented-control',
        'tag-input',
        'text-field',
      ],
    },
    testing: {
      unit: [
        'Rows, controls and the all / any switch are named; axe passes with select, several-choices and tags values.',
        'Changing the field resets operator and value; numbers are stored as numbers.',
        'Operators without a value hide the input; list operators convert the value.',
        'Add, remove, `maxConditions` and keyboard switching of all / any.',
        'Problems only with `showErrors`, linked to the control; removed fields and options stay visible.',
        'Helpers: operators, creation, validation and evaluation; disabled; server rendering.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
