'use client';

import {
  forwardRef,
  useRef,
  useState,
  type HTMLAttributes,
  type KeyboardEvent,
  type ReactNode,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { StarGlyph } from '../../../internal/icons.js';
import { useFieldContext, useFieldControl } from '../form-field/index.js';

/** One point on the scale. */
export interface RatingOption {
  /** The number stored and sent to `onValueChange`. */
  value: number;
  /** Short meaning shown under the number, e.g. "Meets expectations". */
  label?: ReactNode;
  /** Plain-text label for screen readers when `label` is not a string. */
  textLabel?: string;
}

/** Visible strings, for translation. */
export interface RatingPickerMessages {
  /** Accessible name of each star in the `stars` variant. Default "3 of 5", plus the option's label. */
  starLabel: (value: number, max: number, label?: string) => string;
}

export const RATING_PICKER_MESSAGES: RatingPickerMessages = {
  starLabel: (value, max, label) => (label ? `${value} of ${max}, ${label}` : `${value} of ${max}`),
};

export interface RatingPickerProps extends Omit<
  HTMLAttributes<HTMLDivElement>,
  'onChange' | 'defaultValue'
> {
  /**
   * The scale, with a label per value. When omitted, the scale is the
   * whole numbers from `min` to `max` with no labels.
   */
  options?: readonly RatingOption[];
  /** First value of a numeric scale when `options` is not given. Default 1. */
  min?: number;
  /** Last value of a numeric scale when `options` is not given. Default 5. */
  max?: number;
  /** Controlled value; `null` means no rating yet. */
  value?: number | null;
  /** Uncontrolled initial value. */
  defaultValue?: number | null;
  /** Called with the new value, or `null` when a `clearable` rating is cleared. */
  onValueChange?: ((value: number | null) => void) | undefined;
  /**
   * Activating the selected value again clears the rating (and Delete or
   * Backspace clears it from the keyboard). Use it for optional ratings.
   */
  clearable?: boolean;
  /**
   * `scale` (default): a wrapping row of tiles showing the number and its
   * label. `stars`: a compact row of stars, with the selected option's label
   * shown beside them. Exposed as `data-variant`.
   */
  variant?: 'scale' | 'stars';
  /** Size of the tiles or stars. Default `md`. Exposed as `data-size`. */
  size?: 'sm' | 'md';
  disabled?: boolean;
  /** Shows the value but does not allow changes. */
  readOnly?: boolean;
  required?: boolean;
  /** Marks the group invalid (also inherited from a surrounding Field). */
  invalid?: boolean;
  /** Form field name; renders a hidden input carrying the value. */
  name?: string;
  /** Visible strings, for translation. */
  messages?: Partial<RatingPickerMessages>;
}

function textOf(option: RatingOption): string | undefined {
  if (option.textLabel !== undefined) return option.textLabel;
  return typeof option.label === 'string' || typeof option.label === 'number'
    ? String(option.label)
    : undefined;
}

function rangeOptions(min: number, max: number): RatingOption[] {
  const out: RatingOption[] = [];
  for (let v = Math.round(min); v <= Math.round(max); v++) out.push({ value: v });
  return out;
}

/**
 * Picks one value on a rating scale (1–5 by default, or any scale with a
 * label per value). It is a radio group: Tab reaches the group, arrow keys
 * move and select, and each value is announced with its label. Inside a
 * Field it takes the Field's label, description and error.
 */
export const RatingPicker = forwardRef<HTMLDivElement, RatingPickerProps>(function RatingPicker(
  {
    options: optionsProp,
    min = 1,
    max = 5,
    value: valueProp,
    defaultValue = null,
    onValueChange,
    clearable = false,
    variant = 'scale',
    size = 'md',
    disabled: disabledProp,
    readOnly = false,
    required: requiredProp,
    invalid,
    name,
    messages: messagesProp,
    className,
    onKeyDown,
    ...rest
  },
  ref,
) {
  const messages = { ...RATING_PICKER_MESSAGES, ...messagesProp };
  const options = optionsProp ?? rangeOptions(min, max);
  const [state, setState] = useState<number | null>(defaultValue);
  const value = valueProp !== undefined ? valueProp : state;
  const field = useFieldContext();
  const control = useFieldControl(
    {
      id: rest.id,
      disabled: disabledProp,
      required: requiredProp,
      'aria-describedby': rest['aria-describedby'],
    },
    { invalid },
  );
  const disabled = !!control.disabled;
  const labelledBy =
    rest['aria-labelledby'] ?? (rest['aria-label'] || !field ? undefined : field.labelId);
  const buttons = useRef<(HTMLButtonElement | null)[]>([]);

  const selectedIndex = options.findIndex((o) => o.value === value);
  const tabIndexAt = selectedIndex === -1 ? 0 : selectedIndex;
  const scaleMax = options[options.length - 1]?.value ?? max;

  const commit = (next: number | null) => {
    if (disabled || readOnly || next === value) return;
    if (valueProp === undefined) setState(next);
    onValueChange?.(next);
  };

  const activate = (option: RatingOption) => {
    if (option.value === value) {
      if (clearable) commit(null);
      return;
    }
    commit(option.value);
  };

  const moveTo = (index: number) => {
    const count = options.length;
    if (!count) return;
    const i = ((index % count) + count) % count;
    buttons.current[i]?.focus();
    const option = options[i];
    if (option) commit(option.value);
  };

  const handleKeyDown = (event: KeyboardEvent<HTMLDivElement>) => {
    onKeyDown?.(event);
    if (event.defaultPrevented || disabled) return;
    const current = buttons.current.findIndex((b) => b === event.target);
    if (current === -1) return;
    const rtl = event.currentTarget.closest('[dir]')?.getAttribute('dir') === 'rtl';
    const forward = rtl ? 'ArrowLeft' : 'ArrowRight';
    const backward = rtl ? 'ArrowRight' : 'ArrowLeft';
    switch (event.key) {
      case forward:
      case 'ArrowDown':
        event.preventDefault();
        if (readOnly) buttons.current[(current + 1) % options.length]?.focus();
        else moveTo(current + 1);
        break;
      case backward:
      case 'ArrowUp':
        event.preventDefault();
        if (readOnly) buttons.current[(current - 1 + options.length) % options.length]?.focus();
        else moveTo(current - 1);
        break;
      case 'Home':
        event.preventDefault();
        if (readOnly) buttons.current[0]?.focus();
        else moveTo(0);
        break;
      case 'End':
        event.preventDefault();
        if (readOnly) buttons.current[options.length - 1]?.focus();
        else moveTo(options.length - 1);
        break;
      case 'Delete':
      case 'Backspace':
        if (clearable && value !== null) {
          event.preventDefault();
          commit(null);
        }
        break;
      default:
    }
  };

  const selected = selectedIndex === -1 ? undefined : options[selectedIndex];

  return (
    // Roving focus: the radios inside take focus, not the group.
    // eslint-disable-next-line jsx-a11y/interactive-supports-focus
    <div
      ref={ref}
      role="radiogroup"
      {...rest}
      id={control.id}
      aria-labelledby={labelledBy}
      aria-describedby={control['aria-describedby']}
      aria-invalid={control['aria-invalid']}
      aria-required={control['aria-required']}
      aria-disabled={disabled || undefined}
      aria-readonly={readOnly || undefined}
      className={cx('mn-rating-picker', className)}
      data-variant={variant}
      data-size={size}
      data-disabled={disabled || undefined}
      data-readonly={readOnly || undefined}
      data-invalid={control['aria-invalid'] ? '' : undefined}
      onKeyDown={handleKeyDown}
    >
      {options.map((option, index) => {
        const checked = option.value === value;
        const filled = variant === 'stars' && value !== null && option.value <= (value ?? 0);
        const label = textOf(option);
        return (
          <button
            key={option.value}
            ref={(el) => {
              buttons.current[index] = el;
            }}
            type="button"
            role="radio"
            aria-checked={checked}
            aria-label={
              variant === 'stars' ? messages.starLabel(option.value, scaleMax, label) : undefined
            }
            tabIndex={index === tabIndexAt ? 0 : -1}
            disabled={disabled}
            className="mn-rating-picker__option"
            data-state={checked ? 'checked' : 'unchecked'}
            data-filled={filled || undefined}
            onClick={() => {
              if (readOnly) return;
              activate(option);
            }}
          >
            {variant === 'stars' ? (
              <StarGlyph className="mn-rating-picker__star" />
            ) : (
              <>
                <span className="mn-rating-picker__value">{option.value}</span>
                {option.label !== undefined ? (
                  <span className="mn-rating-picker__label">{option.label}</span>
                ) : null}
              </>
            )}
          </button>
        );
      })}
      {variant === 'stars' && selected?.label !== undefined ? (
        <span className="mn-rating-picker__caption" aria-hidden>
          {selected.label}
        </span>
      ) : null}
      {name ? (
        <input type="hidden" name={name} value={value === null ? '' : String(value)} />
      ) : null}
    </div>
  );
});
