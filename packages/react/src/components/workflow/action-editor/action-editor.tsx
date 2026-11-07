'use client';

import { forwardRef, useId, useRef, type HTMLAttributes, type ReactNode } from 'react';
import { cx } from '../../../internal/cx.js';
import { CloseGlyph, PlusGlyph, WarningGlyph } from '../../../internal/icons.js';
import { Button, IconButton } from '../../actions/button/index.js';
import { Checkbox } from '../../forms/checkbox/index.js';
import { Combobox } from '../../forms/combobox/index.js';
import {
  Field,
  FieldDescription,
  FieldError,
  FieldLabel,
  NativeSelect,
  type NativeSelectOption,
} from '../../forms/form-field/index.js';
import { TagInput } from '../../forms/tag-input/index.js';
import { Input, Textarea } from '../../forms/text-field/index.js';
import { ReorderableList } from '../../interaction/reorderable-list/index.js';

/** A parameter value: text, a number, a boolean, a list of values, or none. */
export type ActionParamValue = string | number | boolean | readonly string[] | null;

/** The parameters of one action, keyed by parameter `key`. */
export type ActionParams = Readonly<Record<string, ActionParamValue | undefined>>;

/** One input of an action type, such as "Team" for "Set team". */
export interface ActionParamDefinition {
  /** Key in `params`. */
  key: string;
  label: string;
  type: 'text' | 'textarea' | 'number' | 'select' | 'multi-select' | 'tags' | 'boolean';
  /** Choices for `select` and `multi-select`; suggestions for `tags`. */
  options?: readonly NativeSelectOption[] | undefined;
  /** Must be filled in before the action is complete. */
  required?: boolean | undefined;
  /** Help under the label. */
  description?: ReactNode;
  placeholder?: string | undefined;
  /** Text of a select's empty choice when the parameter is optional, e.g. "The ticket's current team". */
  emptyLabel?: string | undefined;
  min?: number | undefined;
  max?: number | undefined;
  /** Shown only while this holds, e.g. "Agent" only when "Notify" is "A specific agent". */
  visibleWhen?: ((params: ActionParams) => boolean) | undefined;
}

/** A kind of action, such as "Set priority" or "Notify agents". */
export interface ActionTypeDefinition {
  /** Stable value stored on the action. */
  value: string;
  label: string;
  /** One line on what it does, shown under the picker. */
  description?: ReactNode;
  /** Group heading in the picker, e.g. "Assignment". */
  group?: string | undefined;
  params?: readonly ActionParamDefinition[] | undefined;
  /** Starting parameters when the type is picked. */
  defaults?: ActionParams | undefined;
  /** Not available right now; still listed so existing actions keep their type. */
  disabled?: boolean | undefined;
  /** Why it is not available; shown in the picker and as a warning on actions of this type. */
  disabledReason?: string | undefined;
  /** Extra check. Return a message when the parameters are not usable. */
  validate?: ((params: ActionParams) => string | null | undefined) | undefined;
  /** Custom parameter editor for parameters that fields cannot express. Replaces `params` inputs. */
  render?:
    ((params: ActionParams, onChange: (params: ActionParams) => void) => ReactNode) | undefined;
}

/** One action. */
export interface RuleAction {
  /** Stable, unique id. */
  id: string;
  /** The `value` of an action type. */
  type: string;
  params: ActionParams;
}

/** Labels. Override them to translate. */
export interface ActionEditorMessages {
  /** Accessible name of the list. */
  label: string;
  /** Visible heading and accessible name of each action. */
  action: (position: number) => string;
  type: string;
  remove: (position: number) => string;
  add: string;
  empty: string;
  /** Default empty choice of an optional select. */
  choose: string;
  /** Problem for an empty required parameter. */
  required: (label: string) => string;
  /** Shown for a stored type the list no longer has. */
  unknownType: (value: string) => string;
  /** Prefix of a disabled type's warning. */
  unavailable: string;
  maxReached: (max: number) => string;
}

const DEFAULT_MESSAGES: ActionEditorMessages = {
  label: 'Actions',
  action: (n) => `Action ${n}`,
  type: 'Action',
  remove: (n) => `Remove action ${n}`,
  add: 'Add action',
  empty: 'No actions yet.',
  choose: 'Choose…',
  required: (label) => `Fill in “${label}”.`,
  unknownType: (v) => `${v} (not available)`,
  unavailable: 'This action cannot run right now.',
  maxReached: (max) => `You can add up to ${max} actions.`,
};

const isEmpty = (v: ActionParamValue | undefined) =>
  v === undefined || v === null || v === '' || (Array.isArray(v) && v.length === 0);

/** A new action of `type` with its default parameters. */
export function createAction(id: string, type: ActionTypeDefinition | undefined): RuleAction {
  return { id, type: type?.value ?? '', params: { ...type?.defaults } };
}

/**
 * What is wrong with an action: empty required parameters (only those
 * currently visible) and the type's own `validate`. Empty when complete.
 */
export function validateAction(
  action: RuleAction,
  actionTypes: readonly ActionTypeDefinition[],
  messages: Pick<ActionEditorMessages, 'required'> = DEFAULT_MESSAGES,
): string[] {
  const type = actionTypes.find((t) => t.value === action.type);
  if (!type) return [];
  const problems: string[] = [];
  if (!type.render) {
    for (const p of type.params ?? []) {
      if (p.visibleWhen && !p.visibleWhen(action.params)) continue;
      const v = action.params[p.key];
      if (p.required && (isEmpty(v) || (p.type === 'number' && Number.isNaN(v))))
        problems.push(messages.required(p.label));
    }
  }
  const custom = type.validate?.(action.params);
  if (custom) problems.push(custom);
  return problems;
}

/** Problems of every incomplete action, keyed by action id. Empty when all are complete. */
export function validateActions(
  actions: readonly RuleAction[],
  actionTypes: readonly ActionTypeDefinition[],
  messages?: Pick<ActionEditorMessages, 'required'>,
): Record<string, string[]> {
  const out: Record<string, string[]> = {};
  for (const a of actions) {
    const p = validateAction(a, actionTypes, messages);
    if (p.length) out[a.id] = p;
  }
  return out;
}

export interface ActionEditorProps extends Omit<
  HTMLAttributes<HTMLDivElement>,
  'onChange' | 'defaultValue'
> {
  /** The actions, in the order they run (controlled). */
  value: readonly RuleAction[];
  /** Called with the full new list after every change. */
  onChange: (actions: RuleAction[]) => void;
  /** The kinds of action available. */
  actionTypes: readonly ActionTypeDefinition[];
  /** Actions run in order and can be reordered. Default true. */
  ordered?: boolean | undefined;
  /** Most actions; the add button is hidden at this count. Default 20. */
  maxActions?: number | undefined;
  /** Show what is missing on each incomplete action (for example after a save attempt). Default false. */
  showErrors?: boolean | undefined;
  /** The type new actions start as. Default: the first type that is not disabled. */
  defaultType?: string | undefined;
  /** Makes the id of a new action. Default: a generated id. */
  createId?: (() => string) | undefined;
  /** Replaces the empty text. */
  emptyContent?: ReactNode;
  disabled?: boolean | undefined;
  /** Override labels. */
  messages?: Partial<ActionEditorMessages> | undefined;
}

/**
 * Edits an ordered list of actions. Each action has a type and the parameters
 * that type defines (team, priority, tags, a message…), rendered as labelled
 * inputs, with validation and accessible reordering. Types are data, so the
 * same editor serves routing rules, automations and macros.
 */
export const ActionEditor = forwardRef<HTMLDivElement, ActionEditorProps>(function ActionEditor(
  {
    value,
    onChange,
    actionTypes,
    ordered = true,
    maxActions = 20,
    showErrors = false,
    defaultType,
    createId,
    emptyContent,
    disabled = false,
    messages: messagesProp,
    className,
    ...rest
  },
  ref,
) {
  const m = { ...DEFAULT_MESSAGES, ...messagesProp };
  const baseId = useId();
  const counter = useRef(0);
  const typeFor = (v: string) => actionTypes.find((t) => t.value === v);
  const position = (a: RuleAction) => value.findIndex((x) => x.id === a.id) + 1;
  const update = (id: string, next: RuleAction) =>
    onChange(value.map((a) => (a.id === id ? next : a)));
  const add = () => {
    counter.current += 1;
    const id = createId?.() ?? `${baseId}-a${counter.current}`;
    const start =
      (defaultType !== undefined ? typeFor(defaultType) : undefined) ??
      actionTypes.find((t) => !t.disabled);
    onChange([...value, createAction(id, start)]);
  };

  const groups: string[] = [];
  for (const t of actionTypes) if (t.group && !groups.includes(t.group)) groups.push(t.group);
  const typeOption = (t: ActionTypeDefinition, current: string) => (
    <option key={t.value} value={t.value} disabled={t.disabled && t.value !== current}>
      {t.disabled && t.disabledReason ? `${t.label} (${t.disabledReason})` : t.label}
    </option>
  );

  const renderAction = (action: RuleAction, index: number) => {
    const n = index + 1;
    const type = typeFor(action.type);
    const titleId = `${baseId}-${action.id}-title`;
    // Required parameters show their error under the input; the list holds the type's own check.
    const custom = showErrors ? type?.validate?.(action.params) : null;
    const problems = custom ? [custom] : [];
    const missing = showErrors ? validateAction(action, actionTypes, m) : [];
    const setParams = (params: ActionParams) => update(action.id, { ...action, params });
    return (
      <div
        role="group"
        aria-labelledby={titleId}
        className="mn-action-editor__action"
        data-invalid={missing.length > 0 || undefined}
        data-unavailable={type?.disabled || undefined}
      >
        <div className="mn-action-editor__header">
          <span id={titleId} className="mn-action-editor__title">
            {m.action(n)}
          </span>
          <IconButton
            size="sm"
            icon={<CloseGlyph />}
            label={m.remove(n)}
            disabled={disabled}
            onClick={() => onChange(value.filter((a) => a.id !== action.id))}
          />
        </div>
        <Field>
          <FieldLabel>{m.type}</FieldLabel>
          <NativeSelect
            value={action.type}
            onChange={(e) => update(action.id, createAction(action.id, typeFor(e.target.value)))}
          >
            {!type ? <option value={action.type}>{m.unknownType(action.type)}</option> : null}
            {actionTypes.filter((t) => !t.group).map((t) => typeOption(t, action.type))}
            {groups.map((g) => (
              <optgroup key={g} label={g}>
                {actionTypes.filter((t) => t.group === g).map((t) => typeOption(t, action.type))}
              </optgroup>
            ))}
          </NativeSelect>
          {type?.description ? <FieldDescription>{type.description}</FieldDescription> : null}
        </Field>
        {type?.disabled ? (
          <p className="mn-action-editor__warning">
            <WarningGlyph />
            <span>
              {m.unavailable}
              {type.disabledReason ? ` ${type.disabledReason}` : null}
            </span>
          </p>
        ) : null}
        {type?.render ? (
          <div className="mn-action-editor__custom">{type.render(action.params, setParams)}</div>
        ) : type?.params?.length ? (
          <div className="mn-action-editor__params">
            {type.params
              .filter((p) => !p.visibleWhen || p.visibleWhen(action.params))
              .map((p) => (
                <ParamInput
                  key={p.key}
                  param={p}
                  value={action.params[p.key]}
                  choose={m.choose}
                  required={m.required(p.label)}
                  invalid={
                    showErrors &&
                    !!p.required &&
                    (isEmpty(action.params[p.key]) ||
                      (p.type === 'number' && Number.isNaN(action.params[p.key])))
                  }
                  onChange={(v) => setParams({ ...action.params, [p.key]: v })}
                />
              ))}
          </div>
        ) : null}
        {problems.length > 0 ? <p className="mn-action-editor__problem">{problems[0]}</p> : null}
      </div>
    );
  };

  return (
    <div
      ref={ref}
      {...rest}
      className={cx('mn-action-editor', className)}
      data-disabled={disabled || undefined}
    >
      {value.length === 0 ? (
        <p className="mn-action-editor__empty">{emptyContent ?? m.empty}</p>
      ) : ordered ? (
        <ReorderableList
          className="mn-action-editor__list"
          aria-label={m.label}
          items={value}
          onReorder={(items) => onChange(items)}
          getKey={(a) => a.id}
          getLabel={(a) => `${m.action(position(a))}, ${typeFor(a.type)?.label ?? a.type}`}
          disabled={disabled}
          moveButtons
          renderItem={(a, { index }) => renderAction(a, index)}
        />
      ) : (
        <ul className="mn-action-editor__list mn-action-editor__list--plain" aria-label={m.label}>
          {value.map((a, i) => (
            <li key={a.id} className="mn-action-editor__item">
              {renderAction(a, i)}
            </li>
          ))}
        </ul>
      )}
      {value.length < maxActions ? (
        <Button
          size="sm"
          variant="secondary"
          leadingIcon={<PlusGlyph />}
          className="mn-action-editor__add"
          disabled={disabled || actionTypes.every((t) => t.disabled)}
          onClick={add}
        >
          {m.add}
        </Button>
      ) : (
        <p className="mn-action-editor__empty">{m.maxReached(maxActions)}</p>
      )}
    </div>
  );
});

interface ParamInputProps {
  param: ActionParamDefinition;
  value: ActionParamValue | undefined;
  choose: string;
  /** The error text for an empty required parameter. */
  required: string;
  invalid: boolean;
  onChange: (value: ActionParamValue) => void;
}

function ParamInput({ param: p, value, choose, required, invalid, onChange }: ParamInputProps) {
  const list = Array.isArray(value) ? [...value] : isEmpty(value) ? [] : [String(value)];
  if (p.type === 'boolean') {
    return (
      <Checkbox
        className="mn-action-editor__check"
        label={p.label}
        description={p.description}
        checked={value === true}
        onCheckedChange={(v) => onChange(v === true)}
      />
    );
  }
  let control: ReactNode;
  switch (p.type) {
    case 'textarea':
      control = (
        <Textarea
          rows={3}
          value={typeof value === 'string' ? value : ''}
          placeholder={p.placeholder}
          onChange={(e) => onChange(e.target.value)}
        />
      );
      break;
    case 'number':
      control = (
        <Input
          type="number"
          inputMode="decimal"
          min={p.min}
          max={p.max}
          value={typeof value === 'number' && !Number.isNaN(value) ? String(value) : ''}
          placeholder={p.placeholder}
          onChange={(e) => onChange(e.target.value === '' ? null : Number(e.target.value))}
        />
      );
      break;
    case 'select':
      control = (
        <NativeSelect
          value={typeof value === 'string' ? value : ''}
          placeholder={p.emptyLabel ?? choose}
          options={p.options ?? []}
          onChange={(e) => onChange(e.target.value === '' ? null : e.target.value)}
        />
      );
      break;
    case 'multi-select':
      control = (
        <Combobox
          multiple
          options={(p.options ?? []).map((o) => ({ value: o.value, label: o.label }))}
          values={list}
          placeholder={p.placeholder ?? choose}
          onValuesChange={(v) => onChange(v)}
        />
      );
      break;
    case 'tags':
      control = (
        <TagInput
          value={list}
          placeholder={p.placeholder}
          suggestions={(p.options ?? []).map((o) => o.value)}
          onValueChange={(v) => onChange(v)}
        />
      );
      break;
    default:
      control = (
        <Input
          value={typeof value === 'string' ? value : ''}
          placeholder={p.placeholder}
          onChange={(e) => onChange(e.target.value)}
        />
      );
  }
  return (
    <Field
      required={!!p.required}
      invalid={invalid}
      className={cx(
        'mn-action-editor__param',
        p.type === 'textarea' && 'mn-action-editor__param--wide',
      )}
    >
      <FieldLabel>{p.label}</FieldLabel>
      {control}
      {p.description ? <FieldDescription>{p.description}</FieldDescription> : null}
      {invalid ? <FieldError>{required}</FieldError> : null}
    </Field>
  );
}
