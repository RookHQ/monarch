import { useState } from 'react';
import {
  ActionEditor,
  type ActionParams,
  type ActionTypeDefinition,
  type RuleAction,
} from '@rookcom/monarch-react/patterns';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const TEAMS = [
  { value: 'support', label: 'Support' },
  { value: 'billing', label: 'Billing' },
];

const TYPES: ActionTypeDefinition[] = [
  {
    value: 'set_team',
    label: 'Set team',
    group: 'Routing',
    params: [{ key: 'teamId', label: 'Team', type: 'select', options: TEAMS, required: true }],
  },
  {
    value: 'assign_round_robin',
    label: 'Assign in turn',
    group: 'Routing',
    description: 'The next available person in the team, in turn.',
    params: [
      {
        key: 'teamId',
        label: 'Team',
        type: 'select',
        options: TEAMS,
        emptyLabel: 'The current team',
      },
    ],
  },
  {
    value: 'set_priority',
    label: 'Set priority',
    group: 'Fields',
    params: [
      {
        key: 'priority',
        label: 'Priority',
        type: 'select',
        required: true,
        options: [
          { value: 'low', label: 'Low' },
          { value: 'high', label: 'High' },
        ],
      },
    ],
  },
  {
    value: 'add_tags',
    label: 'Add tags',
    group: 'Fields',
    defaults: { tags: [] },
    params: [
      {
        key: 'tags',
        label: 'Tags',
        type: 'tags',
        required: true,
        placeholder: 'Type a tag and press Enter',
      },
    ],
  },
  {
    value: 'notify',
    label: 'Notify people',
    group: 'Messages',
    defaults: { to: 'assignee', message: '' },
    params: [
      {
        key: 'to',
        label: 'Notify',
        type: 'select',
        required: true,
        options: [
          { value: 'assignee', label: 'Assignee' },
          { value: 'team', label: 'Everyone in the team' },
          { value: 'person', label: 'A specific person' },
        ],
      },
      {
        key: 'personId',
        label: 'Person',
        type: 'select',
        required: true,
        options: [
          { value: 'p1', label: 'Jo Park' },
          { value: 'p2', label: 'Ana Ruiz' },
        ],
        visibleWhen: (p: ActionParams) => p.to === 'person',
      },
      { key: 'message', label: 'Message', type: 'textarea', required: true },
    ],
  },
  {
    value: 'external_issue',
    label: 'Create an issue in another app',
    group: 'Messages',
    disabled: true,
    disabledReason: 'The connection is not set up.',
  },
];

function ActionsDemo() {
  const [actions, setActions] = useState<RuleAction[]>([
    { id: '1', type: 'set_team', params: { teamId: 'billing' } },
    { id: '2', type: 'notify', params: { to: 'person', message: '' } },
  ]);
  return (
    <ActionEditor
      style={{ width: '100%', maxWidth: 720 }}
      value={actions}
      onChange={setActions}
      actionTypes={TYPES}
      showErrors
    />
  );
}

export default {
  id: 'action-editor',
  title: 'ActionEditor',
  summary:
    'Edits an ordered list of actions, each a type with labelled parameters, validation and accessible reordering.',
  sections: {
    purpose: {
      summary:
        'Lets people say what should happen, step by step: set a team, add tags, notify someone, send an email. Action types and their parameters are data, so routing rules, automations and macros share one editor. Needed by: Tickets (routing rules, automations, macros), Chat (automation actions), People (payroll rule definitions through custom parameter editors).',
      useWhen: [
        'A rule, automation or macro runs one or more actions in order.',
        'Each action type needs a few parameters (a choice, text, tags, a number, a switch).',
      ],
      avoidWhen: [
        'You need conditions, a trigger and a name too: use RuleBuilder, which composes this.',
        'The steps are approvals by people: use ApprovalChainEditor.',
        'The steps are timed messages in a campaign: use SequenceBuilder.',
      ],
    },
    designPrinciples: {
      items: [
        'Types describe their own parameters, defaults and validation; the editor renders labelled inputs from them.',
        'Changing an action’s type starts it over from that type’s defaults, so stale parameters never leak.',
        'Parameters can depend on each other (`visibleWhen`), such as "Person" only for "A specific person".',
        'Unavailable types stay listed with the reason, and existing actions of that type warn instead of disappearing.',
        'Order is meaningful and editable by drag, keyboard or move buttons; `ordered={false}` turns it off.',
        '`render` is the escape hatch for parameters that fields cannot express.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Controlled: `value` + `onChange`. An action is `{ id, type, params }`.',
        'A type is `{ value, label, description?, group?, params?, defaults?, disabled?, disabledReason?, validate?, render? }`; a parameter is `{ key, label, type, options?, required?, description?, placeholder?, emptyLabel?, min?, max?, visibleWhen? }`.',
        'Helpers: `createAction`, `validateAction` (missing visible required parameters plus `validate`), `validateActions`.',
        'The ref is the root `<div>`.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Migrating from the app copy (Tickets `ActionEditor`): `actions` → `value` (add an `id` to each and move parameters under `params`), `ACTION_TYPES` + `ActionParams` switch → `actionTypes` with `params`, `blankAction` → `defaults`, `actionProblem` → `required` parameters and `validate`, `exclude` → filter `actionTypes` (for macros), the Stride availability check → `disabled` + `disabledReason`, `showProblems` → `showErrors`, `MoveButtons` → built in. Chat automations’ single `action` becomes a list of one with `maxActions={1}` and `ordered={false}`.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'ordered',
          values: ['true', 'false'],
          description: 'Reorderable list with handles and move buttons (default), or a plain list.',
        },
        {
          prop: 'ActionParamDefinition.type',
          values: ['text', 'textarea', 'number', 'select', 'multi-select', 'tags', 'boolean'],
          description: 'The input used for each parameter.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Invalid',
          attribute: 'data-invalid (action), aria-invalid (parameter)',
          description:
            'With `showErrors`, missing parameters show an error under the input and the type’s own check shows under the action.',
        },
        {
          name: 'Unavailable type',
          attribute: 'data-unavailable',
          description: 'A warning explains why the action cannot run now.',
        },
        { name: 'Empty', description: '`messages.empty` or `emptyContent`.' },
        { name: 'Full', description: 'At `maxActions` the add button becomes a note.' },
        {
          name: 'Disabled',
          attribute: 'data-disabled',
          description: 'Add, remove and move are disabled.',
        },
      ],
    },
    interaction: {
      items: [
        'Add action appends one of `defaultType` (or the first available type).',
        'Changing a parameter calls `onChange` with the full list; hidden parameters keep their values until the type changes.',
        'Remove deletes the action from the draft at once; the product confirms when saving changes to a live rule if it needs to.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Tab',
          action:
            'Moves through each action’s handle, remove button, type, parameters and move buttons.',
        },
        { keys: 'Space / Enter on a handle', action: 'Picks the action up or drops it.' },
        { keys: 'Arrow Up / Arrow Down', action: 'Moves a picked-up action.' },
        { keys: 'Escape', action: 'Cancels a move.' },
      ],
    },
    accessibility: {
      role: 'list; each action is a group named "Action n"',
      name: '`messages.label`, "Actions" by default.',
      items: [
        'The type picker and every parameter sit in a Field with a visible label; descriptions are linked.',
        'Missing parameters use FieldError, so the input is `aria-invalid` and described by the message.',
        'Moves are announced by ReorderableList.',
      ],
    },
    responsive: {
      items: [
        'Parameters sit side by side when there is room and stack at narrow widths; text areas take the full row. Works at 320px.',
        'On coarse pointers, handles, move buttons, fields and buttons are at least 44px.',
      ],
    },
    theme: {
      items: [
        'Semantic tokens only; warnings use the warning subtle, border and strong tokens.',
        'Forced colours draw card and warning borders in `CanvasText` and underline invalid titles.',
        'Motion comes from ReorderableList, which honours reduced motion.',
      ],
    },
    examples: {
      items: [
        { title: 'Routing actions with errors shown', stack: true, render: () => <ActionsDemo /> },
        {
          title: 'One action, not reorderable',
          stack: true,
          render: () => (
            <ActionEditor
              style={{ width: '100%', maxWidth: 720 }}
              value={[{ id: '1', type: 'add_tags', params: { tags: ['billing'] } }]}
              onChange={() => {}}
              actionTypes={TYPES}
              ordered={false}
              maxActions={1}
            />
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Give every type a short label and, when it is not obvious, a one-line description.',
        'Mark required parameters, and add `validate` for rules fields cannot express.',
        'Keep unavailable types listed with a reason.',
      ],
      dont: [
        'Do not silently drop actions whose type was removed.',
        'Do not hide the order when actions run in sequence.',
      ],
    },
    related: {
      items: [
        { id: 'rule-builder', reason: 'A complete rule with trigger, conditions and actions.' },
        { id: 'condition-editor', reason: 'When the actions run.' },
        { id: 'sequence-builder', reason: 'Timed steps of a campaign.' },
        { id: 'reorderable-list', reason: 'The reordering it is built on.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: [
        'button',
        'checkbox',
        'combobox',
        'form-field',
        'reorderable-list',
        'tag-input',
        'text-field',
      ],
    },
    testing: {
      unit: [
        'Actions are named groups with labelled type and parameters; descriptions are linked; axe passes.',
        'Changing the type starts from defaults; dependent parameters appear; booleans toggle.',
        'Add, `maxActions`, remove and reorder.',
        'Missing parameters and the type check show only with `showErrors`, linked to the input.',
        'Unavailable and unknown types warn; custom `render`; `ordered={false}`.',
        'Helpers create and validate; empty text; server rendering.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
