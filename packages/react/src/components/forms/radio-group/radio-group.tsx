'use client';

import {
  createContext,
  forwardRef,
  useCallback,
  useContext,
  useId,
  useState,
  type ChangeEvent,
  type HTMLAttributes,
  type InputHTMLAttributes,
  type ReactNode,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { useFieldContext, useFieldControl } from '../form-field/index.js';

/** One choice of a RadioGroup given as data. */
export interface RadioOption {
  /** Submitted with the form and passed to `onValueChange`. Unique within the group. */
  value: string;
  /** Visible label; it is also the radio's accessible name. */
  label: ReactNode;
  /** Secondary text under the label, announced as the radio's description. */
  description?: ReactNode;
  /** The choice cannot be picked. Arrow keys skip it. */
  disabled?: boolean;
}

interface RadioGroupContextValue {
  name: string;
  value: string;
  select: (value: string) => void;
  disabled: boolean;
  required: boolean;
  invalid: boolean;
}

const RadioGroupContext = createContext<RadioGroupContextValue | null>(null);

/**
 * The state of the nearest RadioGroup, for custom radio layouts such as
 * RadioCardGroup. `null` outside a group.
 */
export function useRadioGroupContext(): RadioGroupContextValue | null {
  return useContext(RadioGroupContext);
}

export interface RadioGroupProps extends Omit<
  HTMLAttributes<HTMLDivElement>,
  'defaultValue' | 'onChange'
> {
  /** The choices as data. Alternatively, pass `RadioGroupItem` children. */
  options?: readonly RadioOption[] | undefined;
  /** The selected value (controlled). `''` means nothing is selected. */
  value?: string | undefined;
  /** The initially selected value (uncontrolled). */
  defaultValue?: string | undefined;
  /** Called with the newly selected value. */
  onValueChange?: ((value: string) => void) | undefined;
  /**
   * Form field name shared by the radios, so the selected value submits with
   * the surrounding form. Default: a generated name.
   */
  name?: string | undefined;
  /** A choice is required. Sets `required` on the radios and `aria-required` on the group. */
  required?: boolean | undefined;
  /** Disables every choice. Inside a Field, the Field's `disabled` is used. */
  disabled?: boolean | undefined;
  /** Marks the group invalid. Inside a Field, the Field's `invalid` is used. */
  invalid?: boolean | undefined;
  /**
   * `vertical` (default) stacks the choices; `horizontal` puts them in a row
   * that wraps on narrow screens. Arrow keys work the same in both.
   */
  orientation?: 'vertical' | 'horizontal' | undefined;
  /**
   * Visible group label, for use outside a Field. Inside a Field, use
   * FieldLabel instead: the group is labelled by it automatically.
   */
  label?: ReactNode;
  /** Help text under `label`, announced as the group's description. */
  description?: ReactNode;
}

/**
 * A set of mutually exclusive choices, built on native radio buttons inside
 * `role="radiogroup"`. The browser provides the APG keyboard model (one Tab
 * stop, arrow keys move and select, RTL aware), form submission and
 * `required` validation. Inside a Field it takes the Field's label,
 * description, error, `required`, `disabled` and `invalid`.
 */
export const RadioGroup = forwardRef<HTMLDivElement, RadioGroupProps>(function RadioGroup(
  {
    options,
    value: valueProp,
    defaultValue = '',
    onValueChange,
    name,
    required,
    disabled,
    invalid,
    orientation = 'vertical',
    label,
    description,
    className,
    children,
    ...rest
  },
  ref,
) {
  const auto = useId();
  const field = useFieldContext();
  const control = useFieldControl(
    { id: rest.id, disabled, required, 'aria-describedby': rest['aria-describedby'] },
    { invalid },
  );
  const isInvalid = control['aria-invalid'] === true;
  const isDisabled = control.disabled === true;
  const isRequired = control.required === true;
  const groupId = control.id ?? `mn-radio-group-${auto}`;
  const labelId = `${groupId}-label`;
  const descriptionId = description ? `${groupId}-description` : undefined;

  const [inner, setInner] = useState(defaultValue);
  const controlled = valueProp !== undefined;
  const current = controlled ? valueProp : inner;
  const select = useCallback(
    (next: string) => {
      if (!controlled) setInner(next);
      onValueChange?.(next);
    },
    [controlled, onValueChange],
  );

  const labelledBy =
    rest['aria-labelledby'] ??
    (label ? labelId : rest['aria-label'] === undefined ? field?.labelId : undefined);

  const context: RadioGroupContextValue = {
    name: name ?? `mn-radio-${auto}`,
    value: current,
    select,
    disabled: isDisabled,
    required: isRequired,
    invalid: isInvalid,
  };

  return (
    <RadioGroupContext.Provider value={context}>
      <div
        ref={ref}
        role="radiogroup"
        {...rest}
        id={groupId}
        aria-labelledby={labelledBy}
        aria-describedby={cx(descriptionId, control['aria-describedby']) || undefined}
        aria-required={isRequired || undefined}
        aria-invalid={isInvalid || undefined}
        aria-disabled={isDisabled || undefined}
        data-orientation={orientation}
        data-disabled={isDisabled || undefined}
        data-invalid={isInvalid || undefined}
        className={cx('mn-radio-group', className)}
      >
        {label ? (
          <div id={labelId} className="mn-radio-group__label">
            {label}
            {isRequired ? (
              <span className="mn-radio-group__required" aria-hidden>
                *
              </span>
            ) : null}
          </div>
        ) : null}
        {description ? (
          <p id={descriptionId} className="mn-radio-group__description">
            {description}
          </p>
        ) : null}
        <div className="mn-radio-group__items">
          {options?.map((option) => (
            <RadioGroupItem
              key={option.value}
              value={option.value}
              label={option.label}
              description={option.description}
              disabled={option.disabled}
            />
          ))}
          {children}
        </div>
      </div>
    </RadioGroupContext.Provider>
  );
});

export interface RadioGroupItemProps extends Omit<
  InputHTMLAttributes<HTMLInputElement>,
  'type' | 'value' | 'checked' | 'defaultChecked' | 'name'
> {
  /** The value this radio selects. */
  value: string;
  /** Visible label and accessible name. Omit only when you pass `aria-label`. */
  label?: ReactNode;
  /** Secondary text under the label, announced as the radio's description. */
  description?: ReactNode;
}

/**
 * One radio of a RadioGroup, for when the choices are written as JSX instead
 * of `options`. Renders a native `<input type="radio">` with its label.
 */
export const RadioGroupItem = forwardRef<HTMLInputElement, RadioGroupItemProps>(
  function RadioGroupItem(
    { value, label, description, disabled, className, id, onChange, ...rest },
    ref,
  ) {
    const group = useContext(RadioGroupContext);
    const auto = useId();
    const inputId = id ?? `mn-radio-${auto}`;
    const descriptionId = description ? `${inputId}-description` : undefined;
    const off = disabled === true || group?.disabled === true;
    const checked = group ? group.value === value : undefined;
    const input = (
      <input
        ref={ref}
        type="radio"
        {...rest}
        id={inputId}
        name={group?.name}
        value={value}
        checked={checked}
        required={group?.required || undefined}
        disabled={off || undefined}
        aria-describedby={cx(rest['aria-describedby'], descriptionId) || undefined}
        onChange={(event: ChangeEvent<HTMLInputElement>) => {
          onChange?.(event);
          if (!event.defaultPrevented && event.target.checked) group?.select(value);
        }}
        className={cx('mn-radio-group__radio', label ? undefined : className)}
      />
    );
    if (!label) return input;
    return (
      <div
        className={cx('mn-radio-group__item', className)}
        data-state={checked ? 'checked' : 'unchecked'}
        data-disabled={off || undefined}
      >
        {input}
        <div className="mn-radio-group__text">
          <label htmlFor={inputId} className="mn-radio-group__item-label">
            {label}
          </label>
          {description ? (
            <p id={descriptionId} className="mn-radio-group__item-description">
              {description}
            </p>
          ) : null}
        </div>
      </div>
    );
  },
);
