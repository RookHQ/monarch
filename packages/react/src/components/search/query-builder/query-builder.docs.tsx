import { useState } from 'react';
import { Button } from '@rookcom/monarch-react';
import {
  QueryBuilder,
  evaluateQuery,
  validateQuery,
  type QueryField,
  type QueryGroup,
} from '@rookcom/monarch-react/data';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const FIELDS: QueryField[] = [
  {
    value: 'status',
    label: 'Status',
    type: 'select',
    options: [
      { value: 'open', label: 'Open' },
      { value: 'pending', label: 'Pending' },
      { value: 'closed', label: 'Closed' },
    ],
  },
  { value: 'title', label: 'Title', type: 'text' },
  { value: 'amount', label: 'Amount', type: 'number', placeholder: 'Amount' },
  { value: 'created', label: 'Created', type: 'date' },
  { value: 'flagged', label: 'Flagged', type: 'boolean' },
];

const START: QueryGroup = {
  id: 'root',
  combinator: 'and',
  rules: [{ id: 'r1', field: 'status', operator: 'eq', value: 'open' }],
  groups: [
    {
      id: 'g1',
      combinator: 'or',
      rules: [
        { id: 'r2', field: 'amount', operator: 'gte', value: 500 },
        { id: 'r3', field: 'flagged', operator: 'is_true' },
      ],
      groups: [],
    },
  ],
};

function NestedQuery() {
  const [query, setQuery] = useState<QueryGroup>(START);
  const matches = evaluateQuery(query, { status: 'open', amount: 120, flagged: true });
  return (
    <div style={{ display: 'grid', gap: 12 }}>
      <QueryBuilder value={query} onChange={setQuery} fields={FIELDS} />
      <p>Sample record (open, amount 120, flagged) {matches ? 'matches' : 'does not match'}.</p>
    </div>
  );
}

function FlatQuery() {
  const [query, setQuery] = useState<QueryGroup>({
    id: 'flat',
    combinator: 'or',
    rules: [{ id: 'f1', field: 'title', operator: 'contains', value: 'report' }],
    groups: [],
  });
  return <QueryBuilder value={query} onChange={setQuery} fields={FIELDS} maxDepth={1} />;
}

function ValidatedQuery() {
  const [query, setQuery] = useState<QueryGroup>({
    id: 'v',
    combinator: 'and',
    rules: [{ id: 'v1', field: 'title', operator: 'eq', value: '' }],
    groups: [],
  });
  const [submitted, setSubmitted] = useState(false);
  const problems = validateQuery(query, FIELDS);
  return (
    <div style={{ display: 'grid', gap: 12 }}>
      <QueryBuilder value={query} onChange={setQuery} fields={FIELDS} showErrors={submitted} />
      <Button style={{ justifySelf: 'start' }} onClick={() => setSubmitted(true)}>
        Apply {Object.keys(problems).length > 0 ? '(incomplete)' : ''}
      </Button>
    </div>
  );
}

export default {
  id: 'query-builder',
  title: 'QueryBuilder',
  summary: 'AND / OR groups of field, operator and value rules, nested to a set depth.',
  sections: {
    purpose: {
      summary:
        'Lets people build a precise filter or segment from rules ("Status is Open AND (Amount is at least 500 OR Flagged is yes)") without writing a query language. Each group is a ConditionEditor, so rules use the same typed operators and value inputs as the rest of Monarch. Needed by: Pulse (alert rules), Tickets (report filters), and saved segments in any list with more filters than a FilterBar can hold.',
      useWhen: [
        'Advanced search or a saved segment needs both AND and OR, or brackets.',
        'Rules are data a server evaluates, and people need to see and edit the whole logic.',
      ],
      avoidWhen: [
        'A flat list of conditions joined by all / any is enough: use ConditionEditor.',
        'Quick filters on a list: use FilterBar or FacetedFilterPanel.',
        'A whole automation (trigger, conditions, actions): use RuleBuilder.',
      ],
    },
    designPrinciples: {
      items: [
        'One rule editor everywhere: rules are ConditionEditor rows, so field types, operators, problems and translations are shared.',
        'Logic is visible: every group shows its combinator, nested groups are indented with a bar on the inline-start side, and OR groups use the accent bar.',
        'Bounded nesting: `maxDepth` (default 3) keeps queries readable; past it, the builder says why "Add group" is gone.',
        'Controlled and serialisable: the value is a plain tree of `{ id, combinator, rules, groups }` the server can store and evaluate.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        '`QueryRule` is `Condition` and `QueryField` is `ConditionField` from ConditionEditor, so field definitions and stored rules are interchangeable.',
        'Helpers: `createQueryGroup(id, combinator?)`, `flattenQueryRules(group)`, `validateQuery(group, fields)` (problems keyed by rule id) and `evaluateQuery(group, values)` for previews.',
        'Forwards its ref to the root `<div role="group">`.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'A new sub-group starts with the opposite combinator of its parent, since nesting the same combinator changes nothing.',
        "`conditionMessages` passes `operators`, `problems` and `choose` through to every rule editor; `messages` covers the builder's own strings, including the per-rule labels.",
        "Migrating from react-querybuilder-style trees: `combinator` is lower-case `and` / `or`; a group's rules and sub-groups are two lists (`rules`, `groups`) instead of one mixed `rules` list; `field` / `operator` / `value` keep their names, with Monarch operator ids (`eq`, `contains`, `gte`…).",
      ],
    },
    variants: {
      items: [
        {
          prop: 'maxDepth',
          values: ['1', '2', '3 (default)', 'n'],
          description:
            'How many levels of groups are allowed. `1` gives a flat AND / OR list with no sub-groups.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Combinator',
          attribute: 'data-combinator="and" | "or" on each group',
          description: 'Which way the group joins its rules and sub-groups.',
        },
        {
          name: 'Depth',
          attribute: 'data-depth on each group',
          description: 'Nesting level, from 1 for the top group. Nested groups are boxed.',
        },
        {
          name: 'Invalid rule',
          attribute: 'data-invalid on the rule row',
          description:
            'With `showErrors`, an incomplete rule is outlined and says what is missing.',
        },
        {
          name: 'Disabled',
          attribute: 'data-disabled',
          description: 'Every control is disabled.',
        },
      ],
    },
    interaction: {
      items: [
        'Add rule appends a rule on the first enabled field; Add group appends an empty sub-group.',
        "Changing a rule's field resets its operator and value; changing the operator keeps the value when it still fits.",
        'Remove (×) on a group removes it and everything inside it.',
        'Every change calls `onChange` with the whole new tree.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab / Shift+Tab', action: 'Moves through the controls in reading order.' },
        {
          keys: 'Arrow keys',
          action: 'In a combinator switch, choose AND or OR (radio group).',
        },
        { keys: 'Enter / Space', action: 'Activates Add rule, Add group and remove buttons.' },
      ],
      notes: [
        'Field, operator and value controls are native selects and inputs, with their usual keys.',
      ],
    },
    accessibility: {
      role: 'group (the builder and each group)',
      name: '`messages.label` for the builder; each group is labelled by its visible name ("Top group", "Group 1.2").',
      items: [
        'Every rule control is named with its group and position, e.g. "Group 1, rule 2 value", so screen reader users know where they are.',
        'Remove buttons name what they remove ("Remove group 1").',
        'Problems are linked to their control with `aria-describedby` and the control is marked invalid.',
        'The combinator text is a visible label next to the switch, never conveyed by colour alone.',
      ],
    },
    responsive: {
      items: [
        'Rule controls wrap onto new lines at narrow widths, so there is no horizontal scroll at 390px.',
        'Nested group padding shrinks below 30rem to keep space for the controls.',
        'Buttons and selects keep their 44px touch targets on coarse pointers.',
      ],
    },
    theme: {
      items: [
        'Nested groups use `--mn-color-bg-subtle`, `--mn-color-border-subtle` and `--mn-radius-md`; OR groups use `--mn-color-accent-border` for the bar.',
        'Forced colours draw group borders in CanvasText and an OR bar as a double line.',
        'Logical properties throughout, so the bar sits on the right in RTL.',
      ],
    },
    examples: {
      items: [
        { title: 'Nested AND / OR', stack: true, render: () => <NestedQuery /> },
        { title: 'Flat list (maxDepth 1)', stack: true, render: () => <FlatQuery /> },
        { title: 'Validation on apply', stack: true, render: () => <ValidatedQuery /> },
      ],
    },
    doDont: {
      do: [
        'Validate with `validateQuery` before running or saving, and turn on `showErrors` after the first attempt.',
        'Evaluate on the server; use `evaluateQuery` only for previews.',
        'Keep `maxDepth` low: two or three levels are as much logic as people can read.',
      ],
      dont: [
        'Do not use it for one flat condition list: ConditionEditor is lighter.',
        'Do not store labels in the query; store field keys and look labels up from `fields`.',
      ],
    },
    related: {
      items: [
        { id: 'condition-editor', reason: 'One flat list of conditions with all / any.' },
        { id: 'filter-bar', reason: 'Quick filters above a list.' },
        { id: 'faceted-filter-panel', reason: 'Checkbox and range facets with counts.' },
        { id: 'saved-views', reason: 'Save and name a query for later.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['button', 'segmented-control', 'condition-editor'],
    },
    testing: {
      unit: [
        'Nested groups and rule controls have their accessible names; axe passes.',
        'The combinator switch changes with arrow keys.',
        'Add rule, Add group, remove group and remove rule produce the right tree.',
        'Editing a nested rule value updates only that rule.',
        '`maxDepth` hides Add group at the limit and explains why; `maxDepth={1}` hides it entirely.',
        '`showErrors` shows problems; `disabled` disables the add buttons.',
        'Messages translate group names and buttons.',
        '`validateQuery`, `evaluateQuery` and `flattenQueryRules`.',
        'Renders on the server.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
