'use client';

import {
  createContext,
  forwardRef,
  useCallback,
  useContext,
  useEffect,
  useId,
  useMemo,
  useState,
  type HTMLAttributes,
  type LabelHTMLAttributes,
  type ReactNode,
  type SelectHTMLAttributes,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { ChevronDownGlyph } from '../../../internal/icons.js';

interface FieldContextValue {
  controlId: string;
  labelId: string;
  descriptionId: string;
  errorId: string;
  invalid: boolean;
  required: boolean;
  disabled: boolean;
  hasDescription: boolean;
  hasError: boolean;
  register: (part: 'description' | 'error', present: boolean) => void;
}

const FieldContext = createContext<FieldContextValue | null>(null);

export interface FieldProps extends HTMLAttributes<HTMLDivElement> {
  /** Override the generated control id (e.g. to match an existing label). */
  id?: string;
  invalid?: boolean;
  required?: boolean;
  disabled?: boolean;
  /**
   * `stack` (default): label, control, help and error in a column.
   * `row`: label and help on the left, the control on the right, for a
   * Switch (or Checkbox, or a compact NativeSelect) with a description.
   * Exposed as `data-layout="row"`.
   */
  layout?: 'stack' | 'row';
  children: ReactNode;
}

/**
 * Groups a label, a control, help text and an error message, and wires their
 * ids together: the control gets `id`, `aria-describedby`, `aria-invalid`,
 * `aria-required` and `disabled` automatically. Descriptions and errors
 * register themselves, so `aria-describedby` only names elements that exist.
 *
 *   <Field invalid={!!error}>
 *     <FieldLabel>Email</FieldLabel>
 *     <Input type="email" />
 *     <FieldError>{error}</FieldError>
 *   </Field>
 */
export function Field({
  id,
  invalid = false,
  required = false,
  disabled = false,
  layout = 'stack',
  className,
  children,
  ...rest
}: FieldProps) {
  const auto = useId();
  const controlId = id ?? `mn-field-${auto}`;
  const [hasDescription, setHasDescription] = useState(false);
  const [hasError, setHasError] = useState(false);
  const register = useCallback((part: 'description' | 'error', present: boolean) => {
    if (part === 'description') setHasDescription(present);
    else setHasError(present);
  }, []);
  const value = useMemo<FieldContextValue>(
    () => ({
      controlId,
      labelId: `${controlId}-label`,
      descriptionId: `${controlId}-description`,
      errorId: `${controlId}-error`,
      invalid,
      required,
      disabled,
      hasDescription,
      hasError,
      register,
    }),
    [controlId, invalid, required, disabled, hasDescription, hasError, register],
  );
  return (
    <FieldContext.Provider value={value}>
      <div
        {...rest}
        className={cx('mn-field', className)}
        data-invalid={invalid || undefined}
        data-disabled={disabled || undefined}
        data-layout={layout === 'row' ? 'row' : undefined}
      >
        {children}
      </div>
    </FieldContext.Provider>
  );
}

export function useFieldContext(): FieldContextValue | null {
  return useContext(FieldContext);
}

export interface FieldControlProps {
  id?: string | undefined;
  disabled?: boolean | undefined;
  required?: boolean | undefined;
  'aria-describedby'?: string | undefined;
  'aria-invalid'?: boolean | 'true' | 'false' | 'grammar' | 'spelling' | undefined;
}

/**
 * Merges Field context into a control's props. Explicit props win, so a
 * control can still be used outside a Field.
 */
export function useFieldControl<P extends FieldControlProps>(
  props: P,
  options: { invalid?: boolean | undefined } = {},
): P & { 'aria-required'?: true; 'aria-invalid'?: true } {
  const field = useContext(FieldContext);
  const invalid = options.invalid ?? field?.invalid ?? false;
  const describedBy = [
    props['aria-describedby'],
    field?.hasDescription ? field.descriptionId : undefined,
    field?.hasError && invalid ? field.errorId : undefined,
  ]
    .filter(Boolean)
    .join(' ');
  const required = props.required ?? field?.required;
  return {
    ...props,
    id: props.id ?? field?.controlId,
    disabled: props.disabled ?? (field?.disabled || undefined),
    required,
    'aria-describedby': describedBy || undefined,
    ...(invalid ? { 'aria-invalid': true as const } : {}),
    ...(required ? { 'aria-required': true as const } : {}),
  };
}

export interface FieldLabelProps extends LabelHTMLAttributes<HTMLLabelElement> {
  /** Text shown after the label for optional fields, e.g. "Optional". */
  optionalText?: string;
}

export const FieldLabel = forwardRef<HTMLLabelElement, FieldLabelProps>(function FieldLabel(
  { className, children, optionalText, htmlFor, ...rest },
  ref,
) {
  const field = useContext(FieldContext);
  return (
    <label
      ref={ref}
      {...rest}
      id={rest.id ?? field?.labelId}
      htmlFor={htmlFor ?? field?.controlId}
      className={cx('mn-field__label', className)}
    >
      {children}
      {field?.required ? (
        <span className="mn-field__required" aria-hidden>
          *
        </span>
      ) : optionalText ? (
        <span className="mn-field__optional">{optionalText}</span>
      ) : null}
    </label>
  );
});

function useRegister(part: 'description' | 'error', present: boolean) {
  const field = useContext(FieldContext);
  const register = field?.register;
  useEffect(() => {
    if (!register || !present) return;
    register(part, true);
    return () => register(part, false);
  }, [register, part, present]);
  return field;
}

export function FieldDescription({ className, ...rest }: HTMLAttributes<HTMLParagraphElement>) {
  const field = useRegister('description', true);
  return (
    <p
      {...rest}
      id={rest.id ?? field?.descriptionId}
      className={cx('mn-field__description', className)}
    />
  );
}

/**
 * Renders nothing when empty, so it can always be present. Announced politely
 * when it appears.
 */
export function FieldError({ className, children, ...rest }: HTMLAttributes<HTMLParagraphElement>) {
  const empty =
    children === null || children === undefined || children === false || children === '';
  const field = useRegister('error', !empty);
  if (empty) return null;
  return (
    <p
      {...rest}
      id={rest.id ?? field?.errorId}
      className={cx('mn-field__error', className)}
      aria-live="polite"
    >
      {children}
    </p>
  );
}

export interface NativeSelectOption {
  value: string;
  label: string;
  disabled?: boolean;
}

export interface NativeSelectProps extends Omit<SelectHTMLAttributes<HTMLSelectElement>, 'size'> {
  /** Control height. `sm` is the compact toolbar size (`--mn-control-sm`). Default `md`. */
  size?: 'sm' | 'md' | 'lg';
  /** Marks the value invalid. Inside a Field, the Field's `invalid` is used. */
  invalid?: boolean;
  /** Options as data. `children` (`<option>`, `<optgroup>`) are rendered after them. */
  options?: readonly NativeSelectOption[];
  /**
   * First, empty option shown while no value is chosen. It is disabled once
   * the control is `required`, so it cannot be submitted.
   */
  placeholder?: string;
  /**
   * Visible label rendered inline before the select, for toolbars and filter
   * rows outside a Field ("Sort  Newest ▾"). Inside a Field use FieldLabel.
   */
  label?: ReactNode;
  /** Class for the wrapper `<span class="mn-native-select">`; `className` goes on the `<select>`. */
  wrapperClassName?: string;
}

/**
 * The platform `<select>`, styled like Input and wired to Field. Use it where
 * the native picker is right: long option lists on mobile, forms that must
 * work without JavaScript, compact toolbar filters. For a styled listbox use
 * Select; for search use Combobox.
 */
export const NativeSelect = forwardRef<HTMLSelectElement, NativeSelectProps>(function NativeSelect(
  {
    size = 'md',
    invalid,
    options,
    placeholder,
    label,
    wrapperClassName,
    className,
    children,
    ...rest
  },
  ref,
) {
  const auto = useId();
  const props = useFieldControl(rest, { invalid });
  const id = props.id ?? (label ? `mn-native-select-${auto}` : undefined);
  return (
    <span
      className={cx('mn-native-select', wrapperClassName)}
      data-size={size}
      data-inline-label={label ? '' : undefined}
      data-disabled={props.disabled || undefined}
    >
      {label ? (
        <label htmlFor={id} className="mn-native-select__label">
          {label}
        </label>
      ) : null}
      <span className="mn-native-select__box">
        <select
          ref={ref}
          {...props}
          id={id}
          data-size={size}
          className={cx('mn-input', 'mn-native-select__control', className)}
        >
          {placeholder !== undefined ? (
            <option value="" disabled={props.required || undefined}>
              {placeholder}
            </option>
          ) : null}
          {options?.map((o) => (
            <option key={o.value} value={o.value} disabled={o.disabled}>
              {o.label}
            </option>
          ))}
          {children}
        </select>
        <ChevronDownGlyph className="mn-native-select__chevron" />
      </span>
    </span>
  );
});
