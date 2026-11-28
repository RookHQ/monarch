import { useState } from 'react';
import { Field, FieldLabel, Input } from '@rookcom/monarch-react';
import {
  RuleBuilder,
  createRuleDraft,
  type ActionTypeDefinition,
  type ConditionField,
  type RuleDraft,
  type RuleTrigger,
} from '@rookcom/monarch-react/patterns';
import type { ComponentDocs } from '../../../internal/component-docs.js';
// The scenario of the primary example, shared with iOS and Android (CROSS-PLATFORM.md section 8).
import fixture from './rule-builder.fixture.json';

const scenario = fixture.data;

const TRIGGERS: RuleTrigger[] = scenario.triggers;

const [priorityField, subjectField, hoursField, tagsField] = scenario.fields as [
  { value: string; label: string; options: { value: string; label: string }[] },
  { value: string; label: string },
  { value: string; label: string; placeholder: string },
  { value: string; label: string },
];
const FIELDS: ConditionField[] = [
  { ...priorityField, type: 'select' },
  { ...subjectField, type: 'text' },
  { ...hoursField, type: 'number' },
  { ...tagsField, type: 'tags' },
];

const [setPriorityType, addTagsType, notifyType] = scenario.actionTypes as [
  { value: string; label: string },
  { value: string; label: string },
  { value: string; label: string },
];
const ACTIONS: ActionTypeDefinition[] = [
  {
    ...setPriorityType,
    params: [
      {
        key: scenario.action.param.key,
        label: scenario.action.param.label,
        type: 'select',
        required: true,
        options: scenario.action.options,
      },
    ],
  },
  {
    ...addTagsType,
    defaults: { tags: [] },
    params: [{ key: 'tags', label: 'Tags', type: 'tags', required: true }],
  },
  {
    ...notifyType,
    defaults: { message: '' },
    params: [{ key: 'message', label: 'Message', type: 'textarea', required: true }],
  },
];

function RuleDemo() {
  const [rule, setRule] = useState<RuleDraft>(
    createRuleDraft({
      name: scenario.name,
      triggers: [scenario.checkedTrigger],
      conditions: [{ id: 'c1', ...scenario.condition } as RuleDraft['conditions'][number]],
      actions: [
        {
          id: 'a1',
          type: scenario.action.type,
          params: { [scenario.action.param.key]: scenario.action.value },
        },
      ],
    }),
  );
  const [priority, setPriority] = useState(scenario.extra.value);
  const [saved, setSaved] = useState('');
  return (
    <div style={{ display: 'grid', gap: 12, width: '100%', maxWidth: 760 }}>
      <RuleBuilder
        value={rule}
        onChange={setRule}
        triggers={TRIGGERS}
        fields={FIELDS}
        actionTypes={ACTIONS}
        showStopProcessing
        onSubmit={async (r: RuleDraft) => {
          await new Promise((done) => setTimeout(done, 600));
          setSaved(`Saved “${r.name}” with ${r.actions.length} action(s).`);
        }}
        onCancel={() => setSaved('')}
      >
        <Field>
          <FieldLabel>{scenario.extra.label}</FieldLabel>
          <Input type="number" value={priority} onChange={(e) => setPriority(e.target.value)} />
        </Field>
      </RuleBuilder>
      <p role="status" style={{ margin: 0 }}>
        {saved}
      </p>
    </div>
  );
}

export default {
  id: 'rule-builder',
  title: 'RuleBuilder',
  summary:
    'A complete "when, if, then" rule form: name, triggers, conditions, ordered actions, options, validation and save.',
  sections: {
    purpose: {
      summary:
        'Builds an automation or routing rule from the product’s own vocabulary of triggers, fields and actions, with validation that explains what is missing. Needed by: Tickets (routing rules and automations), Chat (automations), People (payroll rules, through the options slot and custom action editors), Calendar (form logic uses the ConditionEditor inside it).',
      useWhen: [
        'Admins create rules of the form "when this happens, if these hold, do these things".',
        'The product has a rule engine that accepts triggers, conditions and actions as data.',
      ],
      avoidWhen: [
        'You only need conditions (for example "show this question when"): use ConditionEditor.',
        'You only need a list of steps: use ActionEditor or SequenceBuilder.',
        'The rule is a formula: keep a dedicated formula editor and use the options slot for it.',
      ],
    },
    designPrinciples: {
      items: [
        'The rule reads top to bottom as a sentence: When, If, Then.',
        'The product owns the vocabulary (triggers, fields, action types) and saving; the builder owns the form, validation and pending state.',
        'Errors appear after the first save attempt: inline at each control and in one summary that receives focus.',
        'Turning a rule off keeps it; stop-processing is offered only where the engine supports it.',
        'Product-specific settings go in the options slot (`children`) instead of forking the builder.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Controlled: `value` (a `RuleDraft`) + `onChange`. `createRuleDraft(overrides)` makes a new one.',
        '`onSubmit` is called only for a valid rule; return a promise for the pending state; a rejection’s message is shown.',
        '`validateRule(rule, { fields, actionTypes })` returns the same problems the form shows, for server-side or bulk checks.',
        'The ref is the root `<form>`.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Migrating from the app copies: Tickets `RuleEditor` state (`name`, `description`, `triggers`, `all` / `any`, `actions`, `active`, `stopProcessing`) → one `RuleDraft` (`active` → `enabled`; Tickets’ separate "all" and "any" groups become one list with `match`), `triggersFor(kind)` → `triggers`, `KIND_COPY` → `messages`, `problems` + Notice → built-in validation and summary, `FormActions` → `onSubmit` / `onCancel`. Keep the Dialog in the product and render RuleBuilder inside it. Chat automations: `trigger` → `triggers` with `triggerMode="single"`, `enabled` → `enabled`. People payroll `RuleEditorDialog`: keep its type-specific editors in `children` or as an action type `render`.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'triggerMode',
          values: ['multiple', 'single'],
          description:
            'Checkboxes where any trigger runs the rule (default), or one trigger picker.',
        },
        {
          prop: 'showStopProcessing',
          values: ['true', 'false'],
          description:
            'Offer "Stop processing" for engines that run rules in order. Default false.',
        },
        {
          prop: 'headingLevel',
          values: ['2', '3', '4', '5', '6'],
          description: 'Level of the section headings. Default 3.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Invalid',
          attribute: 'data-invalid',
          description:
            'After a blocked save: inline errors and a focused summary listing what is missing.',
        },
        { name: 'Saving', attribute: 'aria-busy', description: 'The save button shows a spinner.' },
        { name: 'Save failed', description: 'An assertive danger alert with the error message.' },
      ],
    },
    interaction: {
      items: [
        'Every edit calls `onChange` with the whole rule.',
        'Save validates first; when something is missing, focus moves to the summary and nothing is sent.',
        'Cancel calls `onCancel`; the product closes its dialog or page (and may guard unsaved changes).',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Tab',
          action: 'Moves through name, triggers, conditions, actions, options and the buttons.',
        },
        { keys: 'Space', action: 'Toggles a trigger or option checkbox.' },
        { keys: 'Enter in a text input', action: 'Submits the form (Save).' },
      ],
      notes: ['Condition and action keyboard support is described on their own pages.'],
    },
    accessibility: {
      role: 'form; each part is a section with a heading (not a landmark)',
      name: 'Pass `aria-label` or `aria-labelledby` (for example the dialog title).',
      items: [
        'Triggers are a group named "When"; the trigger error is linked with `aria-describedby`.',
        'The name input is required and becomes `aria-invalid` with its error.',
        'The problem summary is focusable and receives focus on a blocked save.',
        'Save failures are announced assertively.',
      ],
    },
    responsive: {
      items: [
        'Name and description sit side by side when there is room; triggers flow into columns.',
        'At narrow widths the If and Then panels tighten their padding and the buttons share the row. Works at 320px.',
      ],
    },
    theme: {
      items: [
        'Semantic tokens only; the If and Then panels use `--mn-color-bg-subtle`.',
        'Forced colours draw panel borders in `CanvasText`.',
        'Nothing animates here; child editors honour reduced motion.',
      ],
    },
    examples: {
      items: [
        { title: 'Scheduled rule with an extra option', stack: true, render: () => <RuleDemo /> },
        {
          title: 'Single trigger, new rule',
          stack: true,
          render: () => (
            <RuleBuilder
              style={{ width: '100%', maxWidth: 760 }}
              aria-label="New automation"
              value={createRuleDraft()}
              onChange={() => {}}
              triggers={TRIGGERS}
              triggerMode="single"
              fields={FIELDS}
              actionTypes={ACTIONS}
              showDescription={false}
            />
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Name triggers, fields and actions in the words admins use.',
        'Explain in `messages.ifHelp` what an empty condition list means for your engine.',
        'Run `validateRule` on the server too.',
      ],
      dont: [
        'Do not save rules with incomplete conditions or actions.',
        'Do not fork the builder for one extra setting; use `children`.',
      ],
    },
    related: {
      items: [
        { id: 'condition-editor', reason: 'Conditions on their own.' },
        { id: 'action-editor', reason: 'Actions on their own.' },
        { id: 'approval-chain-editor', reason: 'Who approves, step by step.' },
        {
          id: 'escalation-policy-editor',
          reason: 'Who is alerted, level by level, when nobody responds.',
        },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: [
        'action-editor',
        'alert',
        'button',
        'checkbox',
        'condition-editor',
        'form-field',
        'text-field',
      ],
    },
    testing: {
      unit: [
        'Name, When, If and Then are named; options render; axe passes before and after a blocked save.',
        'A blocked save lists problems, marks the name invalid and focuses the summary.',
        'A rule built from scratch with the keyboard and pointer is saved as data.',
        'Pending state and the error from a rejected save.',
        'Single trigger picker, no triggers, options slot, description hidden, cancel.',
        '`validateRule` reports every part; server rendering.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
