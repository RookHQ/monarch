'use client';

import {
  forwardRef,
  useCallback,
  useId,
  useState,
  type HTMLAttributes,
  type ReactNode,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { Checkbox } from '../checkbox/index.js';
import { useFieldContext, useFieldControl } from '../form-field/index.js';

/** One choice of a CheckboxGroup. */
export interface CheckboxGroupOption {
  /** Submitted with the form and included in `value` when checked. Unique within the group. */
  value: string;
  /** Visible label; it is also the checkbox's accessible name. */
  label: ReactNode;
  /** Secondary text under the label, announced as the checkbox's description. */
  description?: ReactNode;
  /** The choice cannot be changed. Select all leaves it as it is. */
  disabled?: boolean;
}

/** Visible text of CheckboxGroup. Override to translate. */
export interface CheckboxGroupMessages {
  /** Label of the select-all checkbox. Default "Select all". */
  selectAll: string;
}

const DEFAULT_MESSAGES: CheckboxGroupMessages = { selectAll: 'Select all' };

export interface CheckboxGroupProps extends Omit<
  HTMLAttributes<HTMLDivElement>,
  'defaultValue' | 'onChange'
> {
  /** The choices, in order. */
  options: readonly CheckboxGroupOption[];
  /** The checked values (controlled), in option order. */
  value?: readonly string[] | undefined;
  /** The initially checked values (uncontrolled). */
  defaultValue?: readonly string[] | undefined;
  /** Called with every checked value, in option order. */
  onValueChange?: ((value: string[]) => void) | undefined;
  /** Form field name; each checked value submits under it (read with `FormData.getAll`). */
  name?: string | undefined;
  /** Disables every choice. Inside a Field, the Field's `disabled` is used. */
  disabled?: boolean | undefined;
  /** Marks the group invalid. Inside a Field, the Field's `invalid` is used. */
  invalid?: boolean | undefined;
  /**
   * Shows the required marker after `label` (inside a Field, FieldLabel shows
   * it). A group cannot enforce a minimum by itself: validate the count and
   * show a FieldError.
   */
  required?: boolean | undefined;
  /**
   * Adds a "Select all" checkbox above the choices. It is checked when every
   * enabled choice is checked, mixed when some are, and toggles every enabled
   * choice at once.
   */
  selectAll?: boolean | undefined;
  /**
   * `vertical` (default) stacks the choices; `horizontal` puts them in a row
   * that wraps on narrow screens.
   */
  orientation?: 'vertical' | 'horizontal' | undefined;
  /**
   * Visible group label, for use outside a Field. Inside a Field, use
   * FieldLabel: the group is labelled by it automatically.
   */
  label?: ReactNode;
  /** Help text under `label`, announced as the group's description. */
  description?: ReactNode;
  /** Visible text. Override to translate. */
  messages?: Partial<CheckboxGroupMessages> | undefined;
}

/**
 * A labelled group of checkboxes for picking any number of options, with an
 * optional tri-state "Select all". Built on Checkbox; inside a Field it takes
 * the Field's label, description, error, `disabled` and `invalid`.
 */
export const CheckboxGroup = forwardRef<HTMLDivElement, CheckboxGroupProps>(function CheckboxGroup(
  {
    options,
    value: valueProp,
    defaultValue,
    onValueChange,
    name,
    disabled,
    invalid,
    required,
    selectAll = false,
    orientation = 'vertical',
    label,
    description,
    messages,
    className,
    ...rest
  },
  ref,
) {
  const m = { ...DEFAULT_MESSAGES, ...messages };
  const auto = useId();
  const field = useFieldContext();
  const control = useFieldControl(
    { id: rest.id, disabled, required, 'aria-describedby': rest['aria-describedby'] },
    { invalid },
  );
  const isInvalid = control['aria-invalid'] === true;
  const isDisabled = control.disabled === true;
  const isRequired = control.required === true;
  const groupId = control.id ?? `mn-checkbox-group-${auto}`;
  const labelId = `${groupId}-label`;
  const descriptionId = description ? `${groupId}-description` : undefined;
  const itemId = (index: number) => `${groupId}-option-${index}`;

  const [inner, setInner] = useState<readonly string[]>(defaultValue ?? []);
  const controlled = valueProp !== undefined;
  const current = controlled ? valueProp : inner;
  const commit = useCallback(
    (nextSet: ReadonlySet<string>) => {
      const next = options.map((o) => o.value).filter((v) => nextSet.has(v));
      if (!controlled) setInner(next);
      onValueChange?.(next);
    },
    [options, controlled, onValueChange],
  );

  const checked = new Set(current);
  const enabled = options.filter((o) => !o.disabled && !isDisabled);
  const enabledOn = enabled.filter((o) => checked.has(o.value)).length;
  const allState: boolean | 'indeterminate' =
    enabled.length > 0 && enabledOn === enabled.length
      ? true
      : enabledOn > 0
        ? 'indeterminate'
        : false;

  const toggleAll = () => {
    const next = new Set(checked);
    for (const o of enabled) {
      if (allState === true) next.delete(o.value);
      else next.add(o.value);
    }
    commit(next);
  };

  const labelledBy =
    rest['aria-labelledby'] ??
    (label ? labelId : rest['aria-label'] === undefined ? field?.labelId : undefined);

  return (
    <div
      ref={ref}
      role="group"
      {...rest}
      id={groupId}
      aria-labelledby={labelledBy}
      aria-describedby={cx(descriptionId, control['aria-describedby']) || undefined}
      data-orientation={orientation}
      data-disabled={isDisabled || undefined}
      data-invalid={isInvalid || undefined}
      className={cx('mn-checkbox-group', className)}
    >
      {label ? (
        <div id={labelId} className="mn-checkbox-group__label">
          {label}
          {isRequired ? (
            <span className="mn-checkbox-group__required" aria-hidden>
              *
            </span>
          ) : null}
        </div>
      ) : null}
      {description ? (
        <p id={descriptionId} className="mn-checkbox-group__description">
          {description}
        </p>
      ) : null}
      {selectAll ? (
        <Checkbox
          id={`${groupId}-all`}
          className="mn-checkbox-group__all"
          label={m.selectAll}
          checked={allState}
          onCheckedChange={toggleAll}
          disabled={enabled.length === 0}
          required={false}
          aria-controls={options.map((_, index) => itemId(index)).join(' ')}
        />
      ) : null}
      <div className="mn-checkbox-group__items" data-nested={selectAll || undefined}>
        {options.map((option, index) => (
          <Checkbox
            key={option.value}
            id={itemId(index)}
            className="mn-checkbox-group__item"
            label={option.label}
            description={option.description}
            name={name}
            value={option.value}
            checked={checked.has(option.value)}
            disabled={option.disabled === true || isDisabled}
            required={false}
            invalid={isInvalid}
            onCheckedChange={(on) => {
              const next = new Set(checked);
              if (on === true) next.add(option.value);
              else next.delete(option.value);
              commit(next);
            }}
          />
        ))}
      </div>
    </div>
  );
});
