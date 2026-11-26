'use client';

import { forwardRef, useId, useRef, type HTMLAttributes } from 'react';
import { cx } from '../../../internal/cx.js';
import { useFocusAfterRemove } from '../../../internal/focus-after-remove.js';
import { freshId } from '../../../internal/unique-id.js';
import { CloseGlyph, PlusGlyph } from '../../../internal/icons.js';
import { Button, IconButton } from '../../actions/button/index.js';
import { SegmentedControl } from '../../forms/segmented-control/index.js';
import {
  ConditionEditor,
  evaluateCondition,
  validateCondition,
  type Condition,
  type ConditionEditorProps,
  type ConditionField,
  type ConditionProblem,
} from '../../workflow/condition-editor/index.js';

/** How the rules and sub-groups of a group combine: every one must hold (`and`) or at least one (`or`). */
export type QueryCombinator = 'and' | 'or';

/**
 * A field a rule can test. The same shape as ConditionEditor's
 * `ConditionField`: its `type` decides the operators and the value input, and
 * `operators` narrows them.
 */
export type QueryField = ConditionField;

/** One rule: field, operator and value. The same shape as ConditionEditor's `Condition`. */
export type QueryRule = Condition;

/** A group of rules and nested groups joined by one combinator. */
export interface QueryGroup {
  /** Stable, unique id. */
  id: string;
  combinator: QueryCombinator;
  /** The rules of this group, shown first. */
  rules: readonly QueryRule[];
  /** Nested groups, shown after the rules. */
  groups: readonly QueryGroup[];
}

/** Labels. Override them to translate. */
export interface QueryBuilderMessages {
  /** Accessible name of the whole builder. */
  label: string;
  /** Name of a group from its path, e.g. [1] for the top group and [1, 2] for its second sub-group. */
  group: (path: readonly number[]) => string;
  /** Text before the combinator switch. */
  combinatorBefore: string;
  and: string;
  or: string;
  /** Accessible name of a group's combinator switch. */
  combinatorLabel: (groupName: string) => string;
  /** Accessible name of a rule row. */
  rule: (groupName: string, position: number) => string;
  field: (groupName: string, position: number) => string;
  operator: (groupName: string, position: number) => string;
  value: (groupName: string, position: number) => string;
  removeRule: (groupName: string, position: number) => string;
  addRule: string;
  addGroup: string;
  removeGroup: (groupName: string) => string;
  /** Shown in a group with no rules. */
  emptyGroup: string;
  /** Shown instead of "Add group" when `maxDepth` is reached. */
  maxDepthReached: string;
}

const DEFAULT_MESSAGES: QueryBuilderMessages = {
  label: 'Query',
  group: (path) => (path.length === 1 ? 'Top group' : `Group ${path.slice(1).join('.')}`),
  combinatorBefore: 'Join rules with',
  and: 'AND',
  or: 'OR',
  combinatorLabel: (g) => `${g} combinator`,
  rule: (g, n) => `${g}, rule ${n}`,
  field: (g, n) => `${g}, rule ${n} field`,
  operator: (g, n) => `${g}, rule ${n} operator`,
  value: (g, n) => `${g}, rule ${n} value`,
  removeRule: (g, n) => `Remove ${g.toLowerCase()}, rule ${n}`,
  addRule: 'Add rule',
  addGroup: 'Add group',
  removeGroup: (g) => `Remove ${g.toLowerCase()}`,
  emptyGroup: 'No rules in this group.',
  maxDepthReached: 'Groups cannot be nested any deeper.',
};

/** A new, empty group. */
export function createQueryGroup(id: string, combinator: QueryCombinator = 'and'): QueryGroup {
  return { id, combinator, rules: [], groups: [] };
}

/** Every rule in the query, depth first. */
export function flattenQueryRules(group: QueryGroup): QueryRule[] {
  return [...group.rules, ...group.groups.flatMap(flattenQueryRules)];
}

/** Problems of every incomplete rule in the query, keyed by rule id. Empty when the query is complete. */
export function validateQuery(
  group: QueryGroup,
  fields: readonly QueryField[],
): Record<string, ConditionProblem> {
  const out: Record<string, ConditionProblem> = {};
  for (const rule of flattenQueryRules(group)) {
    const problem = validateCondition(rule, fields);
    if (problem) out[rule.id] = problem;
  }
  return out;
}

/**
 * Tests a query against values keyed by field, with ConditionEditor's
 * operator rules (text ignores case). An empty group matches. A useful
 * default for previews; the server stays the authority.
 */
export function evaluateQuery(
  group: QueryGroup,
  values: Readonly<Record<string, unknown>>,
): boolean {
  const tests: (() => boolean)[] = [
    ...group.rules.map((r) => () => evaluateCondition(r, values[r.field])),
    ...group.groups.map((g) => () => evaluateQuery(g, values)),
  ];
  if (tests.length === 0) return true;
  return group.combinator === 'and' ? tests.every((t) => t()) : tests.some((t) => t());
}

export interface QueryBuilderProps extends Omit<
  HTMLAttributes<HTMLDivElement>,
  'onChange' | 'defaultValue'
> {
  /** The query (controlled): the top group. */
  value: QueryGroup;
  /** Called with the whole new query after every change. */
  onChange: (query: QueryGroup) => void;
  /** The fields a rule can test. */
  fields: readonly QueryField[];
  /** How many levels of groups are allowed, counting the top group. Default 3; 1 turns off sub-groups. */
  maxDepth?: number | undefined;
  /** Most rules per group. Default 20. */
  maxRulesPerGroup?: number | undefined;
  /** Show what is missing on each incomplete rule (for example after a submit attempt). Default false. */
  showErrors?: boolean | undefined;
  /** Makes the id of a new rule or group. Default: a generated id. */
  createId?: (() => string) | undefined;
  disabled?: boolean | undefined;
  /** Override labels. */
  messages?: Partial<QueryBuilderMessages> | undefined;
  /** Passed to every rule editor, to rename operators or problems. */
  conditionMessages?:
    | Pick<NonNullable<ConditionEditorProps['messages']>, 'operators' | 'problems' | 'choose'>
    | undefined;
}

/**
 * Builds a filter query from AND / OR groups of field, operator and value
 * rules, nested up to `maxDepth`. Each group's rules are a ConditionEditor, so
 * rules get the same typed operators and value inputs everywhere.
 */
export const QueryBuilder = forwardRef<HTMLDivElement, QueryBuilderProps>(function QueryBuilder(
  {
    value,
    onChange,
    fields,
    maxDepth = 3,
    maxRulesPerGroup = 20,
    showErrors = false,
    createId,
    disabled = false,
    messages: messagesProp,
    conditionMessages,
    className,
    ...rest
  },
  ref,
) {
  const m: QueryBuilderMessages = { ...DEFAULT_MESSAGES, ...messagesProp };
  const baseId = useId();
  const counter = useRef(0);
  const focusAfterRemove = useFocusAfterRemove();
  // Skips every id already in the query: a saved query keeps ids from a previous mount,
  // and groups and rules are matched by id.
  const nextId = () => createId?.() ?? freshId(`${baseId}-q`, counter, queryIds(value));

  const renderGroup = (
    group: QueryGroup,
    path: readonly number[],
    replace: (next: QueryGroup) => void,
    remove: (() => void) | null,
  ) => {
    const name = m.group(path);
    const headingId = `${baseId}-${group.id}-name`;
    const depth = path.length;
    const childCount = group.rules.length + group.groups.length;
    return (
      <div
        key={group.id}
        role="group"
        aria-labelledby={headingId}
        className="mn-query-builder__group"
        data-depth={depth}
        data-combinator={group.combinator}
      >
        <div className="mn-query-builder__header">
          <span id={headingId} className="mn-query-builder__name">
            {name}
          </span>
          <span className="mn-query-builder__join" aria-hidden={childCount < 2 || undefined}>
            {m.combinatorBefore}
          </span>
          <SegmentedControl
            size="sm"
            aria-label={m.combinatorLabel(name)}
            value={group.combinator}
            disabled={disabled}
            onValueChange={(combinator) => replace({ ...group, combinator })}
            options={[
              { value: 'and', label: m.and },
              { value: 'or', label: m.or },
            ]}
          />
          {remove ? (
            <IconButton
              size="sm"
              className="mn-query-builder__remove"
              icon={<CloseGlyph />}
              label={m.removeGroup(name)}
              disabled={disabled}
              onClick={(e) => {
                // Focus stays in the builder: a neighbouring group, or "Add group".
                focusAfterRemove(e.currentTarget, {
                  root: '.mn-query-builder',
                  row: '.mn-query-builder__group',
                  target: '.mn-query-builder__remove',
                  fallback: '.mn-query-builder__add-group',
                });
                remove();
              }}
            />
          ) : null}
        </div>
        <ConditionEditor
          className="mn-query-builder__rules"
          value={group.rules}
          onChange={(rules) => replace({ ...group, rules })}
          fields={fields}
          maxConditions={maxRulesPerGroup}
          showErrors={showErrors}
          createId={nextId}
          disabled={disabled}
          emptyContent={m.emptyGroup}
          messages={{
            ...conditionMessages,
            label: name,
            condition: (n) => m.rule(name, n),
            field: (n) => m.field(name, n),
            operatorLabel: (n) => m.operator(name, n),
            value: (n) => m.value(name, n),
            remove: (n) => m.removeRule(name, n),
            add: m.addRule,
          }}
        />
        {group.groups.length > 0 ? (
          <div className="mn-query-builder__children">
            {group.groups.map((child, index) =>
              renderGroup(
                child,
                [...path, index + 1],
                (next) =>
                  replace({
                    ...group,
                    groups: group.groups.map((g, i) => (i === index ? next : g)),
                  }),
                () => replace({ ...group, groups: group.groups.filter((_, i) => i !== index) }),
              ),
            )}
          </div>
        ) : null}
        {depth < maxDepth ? (
          <Button
            size="sm"
            variant="ghost"
            leadingIcon={<PlusGlyph />}
            className="mn-query-builder__add-group"
            disabled={disabled || fields.length === 0}
            onClick={() =>
              replace({
                ...group,
                groups: [
                  ...group.groups,
                  createQueryGroup(nextId(), group.combinator === 'and' ? 'or' : 'and'),
                ],
              })
            }
          >
            {m.addGroup}
          </Button>
        ) : maxDepth > 1 ? (
          <p className="mn-query-builder__note">{m.maxDepthReached}</p>
        ) : null}
      </div>
    );
  };

  return (
    <div
      ref={ref}
      role="group"
      aria-label={m.label}
      {...rest}
      className={cx('mn-query-builder', className)}
      data-disabled={disabled || undefined}
    >
      {renderGroup(value, [1], onChange, null)}
    </div>
  );
});

/** Every group and rule id in a query. */
function queryIds(group: QueryGroup, out: string[] = []): string[] {
  out.push(group.id);
  for (const rule of group.rules) out.push(rule.id);
  for (const child of group.groups) queryIds(child, out);
  return out;
}
