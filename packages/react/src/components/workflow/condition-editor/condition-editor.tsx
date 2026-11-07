'use client';

import { forwardRef, useId, useRef, type HTMLAttributes, type ReactNode } from 'react';
import { cx } from '../../../internal/cx.js';
import { CloseGlyph, PlusGlyph } from '../../../internal/icons.js';
import { Button, IconButton } from '../../actions/button/index.js';
import { Combobox } from '../../forms/combobox/index.js';
import { NativeSelect, type NativeSelectOption } from '../../forms/form-field/index.js';
import { SegmentedControl } from '../../forms/segmented-control/index.js';
import { TagInput } from '../../forms/tag-input/index.js';
import { Input } from '../../forms/text-field/index.js';

/** The kind of value a field holds; it decides the operators and the value input. */
export type ConditionFieldType =
  'text' | 'number' | 'date' | 'select' | 'multi-select' | 'tags' | 'boolean';

/** How a condition compares the field's value. */
export type ConditionOperator =
  | 'eq'
  | 'neq'
  | 'in'
  | 'nin'
  | 'contains'
  | 'not_contains'
  | 'gt'
  | 'gte'
  | 'lt'
  | 'lte'
  | 'before'
  | 'after'
  | 'is_true'
  | 'is_false'
  | 'is_empty'
  | 'is_not_empty';

/** A condition's value: text, a number, a boolean, a list of option values or tags, or none. */
export type ConditionValue = string | number | boolean | readonly string[] | null;

/** A field a condition can test, such as "Priority" or "Hours since created". */
export interface ConditionField {
  /** Stable key stored on the condition, e.g. `priority` or `requester.email`. */
  value: string;
  label: string;
  type: ConditionFieldType;
  /** Choices for `select` and `multi-select` fields (and suggestions for `tags`). */
  options?: readonly NativeSelectOption[] | undefined;
  /** Heading the field is listed under in the picker, e.g. "Custom fields". */
  group?: string | undefined;
  /** Operators to offer instead of the defaults for the type. */
  operators?: readonly ConditionOperator[] | undefined;
  /** Placeholder of a text or number value input, e.g. "Hours" or "example.com". */
  placeholder?: string | undefined;
  /** Listed but not choosable, e.g. a question that comes later in a form. */
  disabled?: boolean | undefined;
  /** Short text after a disabled field's name, e.g. "comes after this question". */
  disabledReason?: string | undefined;
}

/** One condition. */
export interface Condition {
  /** Stable, unique id. */
  id: string;
  /** The `value` of a ConditionField. */
  field: string;
  operator: ConditionOperator;
  value?: ConditionValue | undefined;
}

/** Whether every condition must hold, or at least one. */
export type ConditionMatch = 'all' | 'any';

/** Why a condition is incomplete. */
export type ConditionProblem = 'field' | 'value' | 'number' | 'option';

/** Labels. Override them to translate. */
export interface ConditionEditorMessages {
  /** Accessible name of the list of conditions. */
  label: string;
  /** Accessible name of each condition row. */
  condition: (position: number) => string;
  field: (position: number) => string;
  operatorLabel: (position: number) => string;
  value: (position: number) => string;
  remove: (position: number) => string;
  add: string;
  /** Text around the all / any switch: before, and after. */
  matchBefore: string;
  matchAfter: string;
  matchAll: string;
  matchAny: string;
  /** Shown when there are no conditions. */
  empty: string;
  choose: string;
  /** Visible names of operators. */
  operators: Record<ConditionOperator, string>;
  /** Shown for a stored field the list no longer has. */
  unknownField: (key: string) => string;
  /** Shown for a stored value no longer among the options. */
  removedOption: string;
  problems: Record<ConditionProblem, string>;
  /** Shown when `maxConditions` is reached. */
  maxReached: (max: number) => string;
}

const DEFAULT_MESSAGES: ConditionEditorMessages = {
  label: 'Conditions',
  condition: (n) => `Condition ${n}`,
  field: (n) => `Condition ${n} field`,
  operatorLabel: (n) => `Condition ${n} operator`,
  value: (n) => `Condition ${n} value`,
  remove: (n) => `Remove condition ${n}`,
  add: 'Add condition',
  matchBefore: 'Match',
  matchAfter: 'of these conditions',
  matchAll: 'all',
  matchAny: 'any',
  empty: 'No conditions. This applies every time.',
  choose: 'Choose…',
  operators: {
    eq: 'is',
    neq: 'is not',
    in: 'is any of',
    nin: 'is none of',
    contains: 'contains',
    not_contains: 'does not contain',
    gt: 'is greater than',
    gte: 'is at least',
    lt: 'is less than',
    lte: 'is at most',
    before: 'is before',
    after: 'is after',
    is_true: 'is yes',
    is_false: 'is no',
    is_empty: 'is empty',
    is_not_empty: 'is not empty',
  },
  unknownField: (key) => `${key} (removed)`,
  removedOption: '(removed option)',
  problems: {
    field: 'Choose a field.',
    value: 'Enter a value.',
    number: 'Enter a number.',
    option: 'The chosen value no longer exists. Pick another.',
  },
  maxReached: (max) => `You can add up to ${max} conditions.`,
};

const OPERATORS_BY_TYPE: Record<ConditionFieldType, readonly ConditionOperator[]> = {
  text: ['eq', 'neq', 'contains', 'not_contains', 'is_empty', 'is_not_empty'],
  number: ['eq', 'neq', 'gt', 'gte', 'lt', 'lte', 'is_empty', 'is_not_empty'],
  date: ['eq', 'before', 'after', 'is_empty', 'is_not_empty'],
  select: ['eq', 'neq', 'in', 'nin', 'is_empty', 'is_not_empty'],
  'multi-select': ['contains', 'not_contains', 'is_empty', 'is_not_empty'],
  tags: ['contains', 'not_contains', 'is_empty', 'is_not_empty'],
  boolean: ['is_true', 'is_false'],
};

const NO_VALUE: ReadonlySet<ConditionOperator> = new Set([
  'is_true',
  'is_false',
  'is_empty',
  'is_not_empty',
]);
const LIST_VALUE: ReadonlySet<ConditionOperator> = new Set(['in', 'nin']);

/** The operators offered for a field: its own `operators`, or the defaults for its type. */
export function conditionOperatorsFor(
  field: ConditionField | undefined,
): readonly ConditionOperator[] {
  if (!field) return ['eq'];
  return field.operators ?? OPERATORS_BY_TYPE[field.type];
}

/** Whether an operator takes a value (`is empty` and `is yes` do not). */
export const conditionOperatorTakesValue = (operator: ConditionOperator) => !NO_VALUE.has(operator);

/** The empty value a field and operator start with. */
function blankValue(
  field: ConditionField | undefined,
  operator: ConditionOperator,
): ConditionValue | undefined {
  if (NO_VALUE.has(operator)) return undefined;
  if (LIST_VALUE.has(operator)) return [];
  if (field?.type === 'multi-select' || field?.type === 'tags') return [];
  return field?.type === 'number' ? null : '';
}

/** A new condition on `field` with its first operator and an empty value. */
export function createCondition(id: string, field: ConditionField | undefined): Condition {
  const operator = conditionOperatorsFor(field)[0] ?? 'eq';
  const value = blankValue(field, operator);
  return value === undefined
    ? { id, field: field?.value ?? '', operator }
    : { id, field: field?.value ?? '', operator, value };
}

const isEmptyValue = (v: ConditionValue | undefined) =>
  v === undefined || v === null || v === '' || (Array.isArray(v) && v.length === 0);

/** What is missing from a condition, or null when it is complete. */
export function validateCondition(
  condition: Condition,
  fields: readonly ConditionField[],
): ConditionProblem | null {
  const field = fields.find((f) => f.value === condition.field);
  if (!field) return 'field';
  if (NO_VALUE.has(condition.operator)) return null;
  const v = condition.value;
  if (isEmptyValue(v)) return 'value';
  if (field.type === 'number' && (typeof v !== 'number' || Number.isNaN(v))) return 'number';
  if (field.options && (field.type === 'select' || field.type === 'multi-select')) {
    const known = new Set(field.options.map((o) => o.value));
    const values = Array.isArray(v) ? v : [String(v)];
    if (values.some((x) => !known.has(x))) return 'option';
  }
  return null;
}

/** Problems of every incomplete condition, keyed by condition id. Empty when all are complete. */
export function validateConditions(
  conditions: readonly Condition[],
  fields: readonly ConditionField[],
): Record<string, ConditionProblem> {
  const out: Record<string, ConditionProblem> = {};
  for (const c of conditions) {
    const p = validateCondition(c, fields);
    if (p) out[c.id] = p;
  }
  return out;
}

const asList = (v: unknown): string[] =>
  Array.isArray(v) ? v.map(String) : isEmptyValue(v as ConditionValue) ? [] : [String(v)];

/**
 * Tests one condition against a value. Text comparisons ignore case; dates
 * compare as `YYYY-MM-DD` text. A useful default for previews and client-side
 * logic such as "show this question when"; the server stays the authority.
 */
export function evaluateCondition(condition: Condition, actual: unknown): boolean {
  const expected = condition.value;
  const lower = (x: unknown) => String(x ?? '').toLowerCase();
  const empty =
    actual === undefined ||
    actual === null ||
    actual === '' ||
    (Array.isArray(actual) && actual.length === 0);
  switch (condition.operator) {
    case 'is_empty':
      return empty;
    case 'is_not_empty':
      return !empty;
    case 'is_true':
      return actual === true || actual === 'true';
    case 'is_false':
      return actual === false || actual === 'false';
    case 'eq':
      return typeof expected === 'number'
        ? Number(actual) === expected
        : lower(actual) === lower(expected);
    case 'neq':
      return typeof expected === 'number'
        ? Number(actual) !== expected
        : lower(actual) !== lower(expected);
    case 'in':
      return asList(expected).map(lower).includes(lower(actual));
    case 'nin':
      return !asList(expected).map(lower).includes(lower(actual));
    case 'contains':
    case 'not_contains': {
      const wanted = asList(expected).map(lower);
      const has = Array.isArray(actual)
        ? wanted.some((w) => actual.map(lower).includes(w))
        : wanted.some((w) => lower(actual).includes(w));
      return condition.operator === 'contains' ? has : !has;
    }
    case 'gt':
      return !empty && Number(actual) > Number(expected);
    case 'gte':
      return !empty && Number(actual) >= Number(expected);
    case 'lt':
      return !empty && Number(actual) < Number(expected);
    case 'lte':
      return !empty && Number(actual) <= Number(expected);
    case 'before':
      return !empty && String(actual) < String(expected);
    case 'after':
      return !empty && String(actual) > String(expected);
  }
}

/**
 * Tests a set of conditions against values keyed by field. No conditions
 * always match.
 */
export function evaluateConditions(
  conditions: readonly Condition[],
  values: Readonly<Record<string, unknown>>,
  match: ConditionMatch = 'all',
): boolean {
  if (conditions.length === 0) return true;
  const test = (c: Condition) => evaluateCondition(c, values[c.field]);
  return match === 'all' ? conditions.every(test) : conditions.some(test);
}

export interface ConditionEditorProps extends Omit<
  HTMLAttributes<HTMLDivElement>,
  'onChange' | 'defaultValue'
> {
  /** The conditions (controlled). */
  value: readonly Condition[];
  /** Called with the full new list after every change. */
  onChange: (conditions: Condition[]) => void;
  /** The fields a condition can test. */
  fields: readonly ConditionField[];
  /** All / any (controlled). Shows the switch when given with `onMatchChange`. */
  match?: ConditionMatch | undefined;
  onMatchChange?: ((match: ConditionMatch) => void) | undefined;
  /** Most conditions; the add button is hidden at this count. Default 20. */
  maxConditions?: number | undefined;
  /** Show what is missing on each incomplete condition (for example after a save attempt). Default false. */
  showErrors?: boolean | undefined;
  /** The field new conditions start on. Default: the first field that is not disabled. */
  defaultField?: string | undefined;
  /** Makes the id of a new condition. Default: a generated id. */
  createId?: (() => string) | undefined;
  /** Replaces the empty text. */
  emptyContent?: ReactNode;
  disabled?: boolean | undefined;
  /** Override labels. `operators` and `problems` may name only the entries to change. */
  messages?:
    | (Partial<Omit<ConditionEditorMessages, 'operators' | 'problems'>> & {
        operators?: Partial<Record<ConditionOperator, string>> | undefined;
        problems?: Partial<Record<ConditionProblem, string>> | undefined;
      })
    | undefined;
}

/**
 * Edits a list of conditions: field, operator and a value input that fits the
 * field (text, number, date, a choice, several choices, tags, or none), with
 * an all / any switch. Fields are data, so the same editor serves ticket
 * rules, form logic and payroll eligibility.
 */
export const ConditionEditor = forwardRef<HTMLDivElement, ConditionEditorProps>(
  function ConditionEditor(
    {
      value,
      onChange,
      fields,
      match,
      onMatchChange,
      maxConditions = 20,
      showErrors = false,
      defaultField,
      createId,
      emptyContent,
      disabled = false,
      messages: messagesProp,
      className,
      ...rest
    },
    ref,
  ) {
    const m: ConditionEditorMessages = {
      ...DEFAULT_MESSAGES,
      ...messagesProp,
      operators: { ...DEFAULT_MESSAGES.operators, ...messagesProp?.operators },
      problems: { ...DEFAULT_MESSAGES.problems, ...messagesProp?.problems },
    };
    const baseId = useId();
    const counter = useRef(0);
    const listId = `${baseId}-list`;
    const matchLabelId = `${baseId}-match`;

    const fieldFor = (key: string) => fields.find((f) => f.value === key);
    const update = (index: number, next: Condition) =>
      onChange(value.map((c, i) => (i === index ? next : c)));
    const add = () => {
      counter.current += 1;
      const id = createId?.() ?? `${baseId}-c${counter.current}`;
      const start =
        (defaultField !== undefined ? fieldFor(defaultField) : undefined) ??
        fields.find((f) => !f.disabled);
      onChange([...value, createCondition(id, start)]);
    };

    const groups: string[] = [];
    for (const f of fields) if (f.group && !groups.includes(f.group)) groups.push(f.group);
    const fieldOption = (f: ConditionField) => (
      <option key={f.value} value={f.value} disabled={f.disabled}>
        {f.disabledReason ? `${f.label} (${f.disabledReason})` : f.label}
      </option>
    );

    return (
      <div
        ref={ref}
        {...rest}
        className={cx('mn-condition-editor', className)}
        data-disabled={disabled || undefined}
      >
        {match && onMatchChange && value.length > 1 ? (
          <div className="mn-condition-editor__match">
            <span id={matchLabelId}>{m.matchBefore}</span>
            <SegmentedControl
              size="sm"
              aria-labelledby={matchLabelId}
              value={match}
              disabled={disabled}
              onValueChange={(v) => onMatchChange(v)}
              options={[
                { value: 'all', label: m.matchAll },
                { value: 'any', label: m.matchAny },
              ]}
            />
            <span>{m.matchAfter}</span>
          </div>
        ) : null}
        {value.length === 0 ? (
          <p className="mn-condition-editor__empty">{emptyContent ?? m.empty}</p>
        ) : (
          <ul id={listId} className="mn-condition-editor__list" aria-label={m.label}>
            {value.map((condition, index) => {
              const n = index + 1;
              const field = fieldFor(condition.field);
              const ops = conditionOperatorsFor(field);
              const problem = showErrors ? validateCondition(condition, fields) : null;
              const errorId = `${baseId}-${condition.id}-error`;
              const describedBy = problem ? errorId : undefined;
              return (
                <li
                  key={condition.id}
                  className="mn-condition-editor__row"
                  data-invalid={problem ? '' : undefined}
                >
                  <div
                    role="group"
                    aria-label={m.condition(n)}
                    className="mn-condition-editor__controls"
                  >
                    <NativeSelect
                      wrapperClassName="mn-condition-editor__field"
                      aria-label={m.field(n)}
                      value={condition.field}
                      disabled={disabled}
                      invalid={problem === 'field'}
                      aria-describedby={problem === 'field' ? describedBy : undefined}
                      onChange={(e) => {
                        const next = fieldFor(e.target.value);
                        update(index, createCondition(condition.id, next));
                      }}
                    >
                      {!field ? (
                        <option value={condition.field}>{m.unknownField(condition.field)}</option>
                      ) : null}
                      {fields.filter((f) => !f.group).map(fieldOption)}
                      {groups.map((g) => (
                        <optgroup key={g} label={g}>
                          {fields.filter((f) => f.group === g).map(fieldOption)}
                        </optgroup>
                      ))}
                    </NativeSelect>
                    <NativeSelect
                      wrapperClassName="mn-condition-editor__operator"
                      aria-label={m.operatorLabel(n)}
                      value={condition.operator}
                      disabled={disabled}
                      onChange={(e) => {
                        const operator = e.target.value as ConditionOperator;
                        update(index, withOperator(condition, operator, field));
                      }}
                    >
                      {(ops.includes(condition.operator) ? ops : [condition.operator, ...ops]).map(
                        (op) => (
                          <option key={op} value={op}>
                            {m.operators[op]}
                          </option>
                        ),
                      )}
                    </NativeSelect>
                    {conditionOperatorTakesValue(condition.operator) ? (
                      <div className="mn-condition-editor__value">
                        <ValueInput
                          condition={condition}
                          field={field}
                          label={m.value(n)}
                          choose={m.choose}
                          removedOption={m.removedOption}
                          invalid={problem !== null && problem !== 'field'}
                          describedBy={problem && problem !== 'field' ? describedBy : undefined}
                          disabled={disabled}
                          onChange={(v) => update(index, { ...condition, value: v })}
                        />
                      </div>
                    ) : null}
                    <IconButton
                      size="sm"
                      className="mn-condition-editor__remove"
                      icon={<CloseGlyph />}
                      label={m.remove(n)}
                      disabled={disabled}
                      onClick={() => onChange(value.filter((c) => c.id !== condition.id))}
                    />
                  </div>
                  {problem ? (
                    <p id={errorId} className="mn-condition-editor__error">
                      {m.problems[problem]}
                    </p>
                  ) : null}
                </li>
              );
            })}
          </ul>
        )}
        {value.length < maxConditions ? (
          <Button
            size="sm"
            variant="secondary"
            leadingIcon={<PlusGlyph />}
            className="mn-condition-editor__add"
            disabled={disabled || fields.length === 0}
            onClick={add}
          >
            {m.add}
          </Button>
        ) : (
          <p className="mn-condition-editor__note">{m.maxReached(maxConditions)}</p>
        )}
      </div>
    );
  },
);

/** Keeps the value when the new operator takes the same shape, converting between one and many. */
function withOperator(
  condition: Condition,
  operator: ConditionOperator,
  field: ConditionField | undefined,
): Condition {
  const base = { id: condition.id, field: condition.field, operator };
  if (NO_VALUE.has(operator)) return base;
  const prev = condition.value;
  const wasList = LIST_VALUE.has(condition.operator);
  const isList = LIST_VALUE.has(operator);
  if (NO_VALUE.has(condition.operator))
    return { ...base, value: blankValue(field, operator) ?? null };
  if (wasList === isList) return { ...base, value: prev ?? null };
  if (isList) return { ...base, value: isEmptyValue(prev) ? [] : asList(prev) };
  return { ...base, value: Array.isArray(prev) ? (prev[0] ?? '') : (prev ?? '') };
}

interface ValueInputProps {
  condition: Condition;
  field: ConditionField | undefined;
  label: string;
  choose: string;
  removedOption: string;
  invalid: boolean;
  describedBy: string | undefined;
  disabled: boolean;
  onChange: (value: ConditionValue) => void;
}

function ValueInput({
  condition,
  field,
  label,
  choose,
  removedOption,
  invalid,
  describedBy,
  disabled,
  onChange,
}: ValueInputProps) {
  const v = condition.value;
  const options = field?.options ?? [];
  const many =
    LIST_VALUE.has(condition.operator) || field?.type === 'multi-select' || field?.type === 'tags';
  const aria = { 'aria-label': label, 'aria-describedby': describedBy };

  if (field?.type === 'tags') {
    return (
      <TagInput
        {...aria}
        size="md"
        value={asList(v)}
        invalid={invalid}
        disabled={disabled}
        suggestions={options.map((o) => o.value)}
        onValueChange={(tags) => onChange(tags)}
      />
    );
  }
  if (options.length > 0 && many) {
    return (
      <Combobox
        {...aria}
        multiple
        options={options.map((o) => ({ value: o.value, label: o.label }))}
        values={asList(v)}
        invalid={invalid}
        disabled={disabled}
        placeholder={choose}
        onValuesChange={(values) => onChange(values)}
      />
    );
  }
  if (options.length > 0) {
    const current = typeof v === 'string' ? v : v === null || v === undefined ? '' : String(v);
    const stale = current !== '' && !options.some((o) => o.value === current);
    return (
      <NativeSelect
        {...aria}
        value={current}
        placeholder={choose}
        invalid={invalid}
        disabled={disabled}
        onChange={(e) => onChange(e.target.value)}
      >
        {stale ? <option value={current}>{removedOption}</option> : null}
        {options.map((o) => (
          <option key={o.value} value={o.value} disabled={o.disabled}>
            {o.label}
          </option>
        ))}
      </NativeSelect>
    );
  }
  if (many) {
    return (
      <TagInput
        {...aria}
        value={asList(v)}
        invalid={invalid}
        disabled={disabled}
        onValueChange={(tags) => onChange(tags)}
      />
    );
  }
  const numeric = field?.type === 'number';
  return (
    <Input
      {...aria}
      type={numeric ? 'number' : field?.type === 'date' ? 'date' : 'text'}
      inputMode={numeric ? 'decimal' : undefined}
      value={
        v === null || v === undefined || (typeof v === 'number' && Number.isNaN(v)) ? '' : String(v)
      }
      placeholder={field?.placeholder}
      invalid={invalid}
      disabled={disabled}
      onChange={(e) => {
        const text = e.target.value;
        onChange(numeric ? (text === '' ? null : Number(text)) : text);
      }}
    />
  );
}
