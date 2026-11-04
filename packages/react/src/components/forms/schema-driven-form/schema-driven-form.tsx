'use client';

import {
  forwardRef,
  useEffect,
  useId,
  useRef,
  useState,
  type FormEvent,
  type FormHTMLAttributes,
  type ReactNode,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { Button } from '../../actions/button/index.js';
import { Alert } from '../../feedback/alert/index.js';
import { Checkbox } from '../checkbox/index.js';
import {
  Field,
  FieldDescription,
  FieldError,
  FieldLabel,
  NativeSelect,
  type NativeSelectOption,
} from '../form-field/index.js';
import { Switch } from '../switch/index.js';
import { Input, Textarea } from '../text-field/index.js';
import { DatePicker } from '../../scheduling/date-picker/index.js';

/**
 * A value in the form: text, a number, a boolean, a `YYYY-MM-DD` date, or
 * `null` for none. A number field holds `NaN` while its text is not a number.
 */
export type SchemaDrivenFormValue = string | number | boolean | null;

/** All values, keyed by field `name`. */
export type SchemaDrivenFormValues = Record<string, SchemaDrivenFormValue>;

/**
 * When a field or section is shown. The object form is plain data (it can
 * come from JSON); every key given must hold. The function form is for rules
 * that data cannot express.
 */
export type SchemaDrivenFormCondition =
  | {
      /** The field whose value decides. */
      field: string;
      /** Shown when the value equals this. */
      equals?: SchemaDrivenFormValue;
      /** Shown when the value does not equal this. */
      notEquals?: SchemaDrivenFormValue;
      /** Shown when the value is one of these. */
      oneOf?: readonly SchemaDrivenFormValue[];
      /** Shown when the value is filled in (`true`) or empty (`false`). */
      filled?: boolean;
    }
  | ((values: SchemaDrivenFormValues) => boolean);

interface FieldBase {
  /** Key of the value. Unique in the schema. */
  name: string;
  label: ReactNode;
  /** Help text under the label. */
  description?: ReactNode;
  /** Must be filled in; a required checkbox or switch must be on. */
  required?: boolean;
  disabled?: boolean;
  /** Shown only while this holds. Hidden fields are not validated or submitted. */
  visibleWhen?: SchemaDrivenFormCondition;
  /** Extra rule. Return a message to mark the field invalid. */
  validate?: (value: SchemaDrivenFormValue, values: SchemaDrivenFormValues) => ReactNode;
}

export type SchemaDrivenFormField =
  | (FieldBase & {
      type: 'text';
      /** Input type for the platform keyboard and autofill. Default `text`. */
      inputType?: 'text' | 'email' | 'url' | 'tel' | 'password';
      defaultValue?: string;
      placeholder?: string;
      autoComplete?: string;
      minLength?: number;
      maxLength?: number;
      /** Regular expression the whole value must match (as text, so it can come from JSON). */
      pattern?: string;
      /** Shown when `pattern` does not match. */
      patternMessage?: string;
    })
  | (FieldBase & {
      type: 'textarea';
      defaultValue?: string;
      placeholder?: string;
      rows?: number;
      minLength?: number;
      maxLength?: number;
    })
  | (FieldBase & {
      type: 'number';
      defaultValue?: number | null;
      placeholder?: string;
      min?: number;
      max?: number;
      /** Only whole numbers. */
      integer?: boolean;
    })
  | (FieldBase & {
      type: 'select';
      options: readonly NativeSelectOption[];
      defaultValue?: string;
      /** First, empty option. Default "Choose…". */
      placeholder?: string;
    })
  | (FieldBase & { type: 'checkbox'; defaultValue?: boolean })
  | (FieldBase & { type: 'switch'; defaultValue?: boolean })
  | (FieldBase & {
      type: 'date';
      /** `YYYY-MM-DD`. */
      defaultValue?: string | null;
      min?: string;
      max?: string;
    });

/** The kinds of field the form renders. */
export type SchemaDrivenFormFieldType = SchemaDrivenFormField['type'];

/** A titled group of fields, rendered as a fieldset with a legend. */
export interface SchemaDrivenFormSection {
  type: 'section';
  id: string;
  title: ReactNode;
  description?: ReactNode;
  fields: readonly SchemaDrivenFormField[];
  visibleWhen?: SchemaDrivenFormCondition;
}

/** Fields and sections, in order. */
export type SchemaDrivenFormSchema = ReadonlyArray<SchemaDrivenFormField | SchemaDrivenFormSection>;

/** Built-in validation and display strings. Override them to translate. */
export interface SchemaDrivenFormMessages {
  required: string;
  /** A required checkbox or switch that is off. */
  mustAccept: string;
  minLength: (n: number) => string;
  maxLength: (n: number) => string;
  pattern: string;
  number: string;
  integer: string;
  min: (n: number) => string;
  max: (n: number) => string;
  /** Default placeholder of a select. */
  choose: string;
  submit: string;
  /** Shown when `onSubmit` rejects without an Error message. */
  submitFailed: string;
  /** `formatSchemaValue` for booleans. */
  yes: string;
  no: string;
  /** `formatSchemaValue` for empty values. */
  empty: string;
}

const DEFAULT_MESSAGES: SchemaDrivenFormMessages = {
  required: 'This field is required.',
  mustAccept: 'Turn this on to continue.',
  minLength: (n) => `Use at least ${n} characters.`,
  maxLength: (n) => `Use ${n} characters or fewer.`,
  pattern: 'Check the format.',
  number: 'Enter a number.',
  integer: 'Enter a whole number.',
  min: (n) => `Enter ${n} or more.`,
  max: (n) => `Enter ${n} or less.`,
  choose: 'Choose…',
  submit: 'Save',
  submitFailed: 'The form could not be saved. Try again.',
  yes: 'Yes',
  no: 'No',
  empty: '—',
};

const isSection = (item: SchemaDrivenFormField | SchemaDrivenFormSection) =>
  item.type === 'section';

/** Every field in schema order, sections flattened. */
function allFields(schema: SchemaDrivenFormSchema): SchemaDrivenFormField[] {
  return schema.flatMap((item) =>
    item.type === 'section' ? [...item.fields] : [item as SchemaDrivenFormField],
  );
}

const isEmpty = (v: SchemaDrivenFormValue | undefined) => v === null || v === undefined || v === '';

function emptyValue(field: SchemaDrivenFormField): SchemaDrivenFormValue {
  if (field.type === 'checkbox' || field.type === 'switch') return field.defaultValue ?? false;
  if (field.type === 'number' || field.type === 'date') return field.defaultValue ?? null;
  return field.defaultValue ?? '';
}

/** The starting values of a schema: each field's `defaultValue`, or its empty value. */
export function schemaDefaultValues(
  schema: SchemaDrivenFormSchema,
  values: SchemaDrivenFormValues = {},
): SchemaDrivenFormValues {
  const out: SchemaDrivenFormValues = {};
  for (const field of allFields(schema)) out[field.name] = values[field.name] ?? emptyValue(field);
  return out;
}

/** Whether a field or section with this condition is shown for these values. */
export function isSchemaFieldVisible(
  condition: SchemaDrivenFormCondition | undefined,
  values: SchemaDrivenFormValues,
): boolean {
  if (!condition) return true;
  if (typeof condition === 'function') return condition(values);
  const value = values[condition.field] ?? null;
  if ('equals' in condition && value !== condition.equals) return false;
  if ('notEquals' in condition && value === condition.notEquals) return false;
  if (condition.oneOf && !condition.oneOf.includes(value)) return false;
  if (condition.filled !== undefined && condition.filled === (isEmpty(value) || value === false))
    return false;
  return true;
}

/** The fields shown for these values, with their section's condition applied. */
function visibleFields(schema: SchemaDrivenFormSchema, values: SchemaDrivenFormValues) {
  return schema.flatMap((item) => {
    if (item.type === 'section') {
      if (!isSchemaFieldVisible(item.visibleWhen, values)) return [];
      return item.fields.filter((f) => isSchemaFieldVisible(f.visibleWhen, values));
    }
    const field = item as SchemaDrivenFormField;
    return isSchemaFieldVisible(field.visibleWhen, values) ? [field] : [];
  });
}

/** Checks one field's value. Returns the message, or `null` when it is valid. */
export function validateSchemaField(
  field: SchemaDrivenFormField,
  value: SchemaDrivenFormValue,
  values: SchemaDrivenFormValues,
  messagesInput: Partial<SchemaDrivenFormMessages> = {},
): ReactNode {
  const m = { ...DEFAULT_MESSAGES, ...messagesInput };
  if (field.type === 'checkbox' || field.type === 'switch') {
    if (field.required && value !== true) return m.mustAccept;
  } else if (isEmpty(value)) {
    if (field.required) return m.required;
    return null;
  } else if (field.type === 'text' || field.type === 'textarea') {
    const text = String(value);
    if (field.minLength !== undefined && text.length < field.minLength)
      return m.minLength(field.minLength);
    if (field.maxLength !== undefined && text.length > field.maxLength)
      return m.maxLength(field.maxLength);
    if (field.type === 'text' && field.pattern && !new RegExp(`^(?:${field.pattern})$`).test(text))
      return field.patternMessage ?? m.pattern;
  } else if (field.type === 'number') {
    if (typeof value !== 'number' || Number.isNaN(value)) return m.number;
    if (field.integer && !Number.isInteger(value)) return m.integer;
    if (field.min !== undefined && value < field.min) return m.min(field.min);
    if (field.max !== undefined && value > field.max) return m.max(field.max);
  }
  return field.validate?.(value, values) || null;
}

/**
 * Validates every visible field. Returns the errors keyed by field name; an
 * empty object means the values can be submitted.
 */
export function validateSchemaForm(
  schema: SchemaDrivenFormSchema,
  values: SchemaDrivenFormValues,
  messages: Partial<SchemaDrivenFormMessages> = {},
): Record<string, ReactNode> {
  const errors: Record<string, ReactNode> = {};
  for (const field of visibleFields(schema, values)) {
    const error = validateSchemaField(field, values[field.name] ?? null, values, messages);
    if (error) errors[field.name] = error;
  }
  return errors;
}

/**
 * A field's value as display text, for read-only views, tables and
 * summaries: option labels for selects, Yes/No for booleans, a locale date,
 * a locale number, and "—" for empty values.
 */
export function formatSchemaValue(
  field: SchemaDrivenFormField,
  value: SchemaDrivenFormValue | undefined,
  options: { locale?: string; messages?: Partial<SchemaDrivenFormMessages> } = {},
): string {
  const m = { ...DEFAULT_MESSAGES, ...options.messages };
  if (field.type === 'checkbox' || field.type === 'switch') return value === true ? m.yes : m.no;
  if (isEmpty(value ?? null)) return m.empty;
  if (field.type === 'select')
    return field.options.find((o) => o.value === value)?.label ?? String(value);
  if (field.type === 'number' && typeof value === 'number')
    return new Intl.NumberFormat(options.locale).format(value);
  if (field.type === 'date' && typeof value === 'string' && /^\d{4}-\d{2}-\d{2}$/.test(value)) {
    const [y, mo, d] = value.split('-').map(Number) as [number, number, number];
    return new Intl.DateTimeFormat(options.locale, { dateStyle: 'medium', timeZone: 'UTC' }).format(
      new Date(Date.UTC(y, mo - 1, d)),
    );
  }
  if (field.type === 'text' && field.inputType === 'password') return '••••••••';
  return String(value);
}

/** Text the number box shows for a value. */
const numberText = (v: SchemaDrivenFormValue) => (typeof v === 'number' ? String(v) : '');

function parseNumber(text: string): number | null {
  const t = text.trim().replace(',', '.');
  if (t === '') return null;
  return /^[-+]?(?:\d+\.?\d*|\.\d+)(?:e[-+]?\d+)?$/i.test(t) ? Number(t) : Number.NaN;
}

interface NumberControlProps {
  field: Extract<SchemaDrivenFormField, { type: 'number' }>;
  value: SchemaDrivenFormValue;
  onValue: (v: number | null) => void;
  onBlur: () => void;
}

/** A text box for numbers that keeps what is typed ("-", "1.") while the value is a number. */
function NumberControl({ field, value, onValue, onBlur }: NumberControlProps) {
  const [text, setText] = useState(() => numberText(value));
  useEffect(() => {
    // Follow outside changes, but keep partial text that parses to the same value.
    const parsed = parseNumber(text);
    if (!(parsed === value || (Number.isNaN(parsed) && Number.isNaN(value as number))))
      setText(numberText(value));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [value]);
  return (
    <Input
      name={field.name}
      inputMode={field.integer ? 'numeric' : 'decimal'}
      value={text}
      placeholder={field.placeholder}
      autoComplete="off"
      onChange={(e) => {
        setText(e.target.value);
        onValue(parseNumber(e.target.value));
      }}
      onBlur={onBlur}
    />
  );
}

export interface SchemaDrivenFormProps extends Omit<
  FormHTMLAttributes<HTMLFormElement>,
  'onSubmit' | 'children' | 'defaultValue'
> {
  /** The fields and sections to render, in order. */
  schema: SchemaDrivenFormSchema;
  /** Values (controlled), keyed by field name. Missing keys use the field's empty value. */
  values?: SchemaDrivenFormValues;
  /** Initial values (uncontrolled). Default: each field's `defaultValue`. */
  defaultValues?: SchemaDrivenFormValues;
  /** Called on every change with all values and the name of the field that changed. */
  onValuesChange?: (values: SchemaDrivenFormValues, name: string) => void;
  /**
   * Called with the visible fields' values once they are valid. Return a
   * promise to show the submit button busy; reject with an Error to show its
   * message above the buttons.
   */
  onSubmit?: (values: SchemaDrivenFormValues) => unknown;
  /** Errors from the product, such as the server's, keyed by field name. Shown with the built-in ones. */
  errors?: Record<string, ReactNode>;
  /** When errors first show: when a field loses focus (`blur`, default) or only on submit. */
  validateOn?: 'blur' | 'submit';
  /** Label of the submit button, or `null` for no button (submit from outside with `form=`). */
  submitLabel?: ReactNode | null;
  /** Buttons after submit, such as Cancel. */
  secondaryActions?: ReactNode;
  /** Disables every field and the submit button. */
  disabled?: boolean;
  /** Show values as text in a description list instead of controls (no `<form>`, so no ref). */
  readOnly?: boolean;
  /** BCP 47 locale for dates and numbers. Pass it for server rendering. */
  locale?: string;
  /** Override built-in strings. */
  messages?: Partial<SchemaDrivenFormMessages>;
}

/**
 * Renders a form from a field schema: text, number, select, checkbox, switch,
 * date and textarea fields, with required and length, pattern and range
 * rules, titled sections and conditional visibility. Built on Field, so
 * labels, help and errors are wired for assistive technology. On submit it
 * validates, focuses the first invalid field, and passes the visible values.
 */
export const SchemaDrivenForm = forwardRef<HTMLFormElement, SchemaDrivenFormProps>(
  function SchemaDrivenForm(
    {
      schema,
      values: valuesProp,
      defaultValues,
      onValuesChange,
      onSubmit,
      errors: externalErrors,
      validateOn = 'blur',
      submitLabel,
      secondaryActions,
      disabled = false,
      readOnly = false,
      locale,
      messages: messagesProp,
      className,
      ...rest
    },
    ref,
  ) {
    const messages = { ...DEFAULT_MESSAGES, ...messagesProp };
    const [inner, setInner] = useState<SchemaDrivenFormValues>(() =>
      schemaDefaultValues(schema, defaultValues),
    );
    const controlled = valuesProp !== undefined;
    const values = controlled ? schemaDefaultValues(schema, valuesProp) : inner;
    const valuesRef = useRef(values);
    valuesRef.current = values;
    const [touched, setTouched] = useState<Record<string, boolean>>({});
    const [submitted, setSubmitted] = useState(false);
    const [pending, setPending] = useState(false);
    const [formError, setFormError] = useState<string | null>(null);
    const [dateErrors, setDateErrors] = useState<Record<string, boolean>>({});
    const formRef = useRef<HTMLFormElement | null>(null);
    const auto = useId();
    const mounted = useRef(true);
    useEffect(() => {
      mounted.current = true;
      return () => {
        mounted.current = false;
      };
    }, []);

    const setValue = (name: string, value: SchemaDrivenFormValue) => {
      const next = { ...valuesRef.current, [name]: value };
      valuesRef.current = next;
      if (!controlled) setInner(next);
      onValuesChange?.(next, name);
    };
    const touch = (name: string) => {
      if (validateOn === 'blur') setTouched((t) => (t[name] ? t : { ...t, [name]: true }));
    };

    const clientErrors = validateSchemaForm(schema, values, messages);
    const errorFor = (name: string): ReactNode =>
      externalErrors?.[name] ?? (submitted || touched[name] ? clientErrors[name] : null);

    const submit = async (event: FormEvent<HTMLFormElement>) => {
      event.preventDefault();
      if (pending || disabled || readOnly) return;
      setSubmitted(true);
      setFormError(null);
      const shown = visibleFields(schema, values);
      const firstInvalid = shown.find((f) => clientErrors[f.name] || dateErrors[f.name]);
      if (firstInvalid) {
        const wrapper = Array.from(
          formRef.current?.querySelectorAll<HTMLElement>('[data-schema-field]') ?? [],
        ).find((node) => node.dataset.schemaField === firstInvalid.name);
        const el = wrapper?.querySelector<HTMLElement>('input, textarea, select, button');
        el?.focus();
        return;
      }
      const out: SchemaDrivenFormValues = {};
      for (const f of shown) out[f.name] = values[f.name] ?? null;
      setPending(true);
      try {
        await onSubmit?.(out);
      } catch (error) {
        if (mounted.current)
          setFormError(
            error instanceof Error && error.message ? error.message : messages.submitFailed,
          );
      } finally {
        if (mounted.current) setPending(false);
      }
    };

    const renderField = (field: SchemaDrivenFormField) => {
      if (!isSchemaFieldVisible(field.visibleWhen, values)) return null;
      const value = values[field.name] ?? null;
      const error = errorFor(field.name);
      const fieldDisabled = disabled || field.disabled || pending;
      const common = {
        'data-schema-field': field.name,
        'data-type': field.type,
        className: 'mn-schema-driven-form__field',
        invalid: Boolean(error) || Boolean(dateErrors[field.name]),
        required: Boolean(field.required),
        disabled: fieldDisabled,
      };
      const label = <FieldLabel>{field.label}</FieldLabel>;
      const help = field.description ? (
        <FieldDescription>{field.description}</FieldDescription>
      ) : null;
      const err = <FieldError>{error}</FieldError>;

      switch (field.type) {
        case 'checkbox':
        case 'switch': {
          const Control = field.type === 'checkbox' ? Checkbox : Switch;
          return (
            <Field key={field.name} {...common}>
              <Control
                name={field.name}
                label={field.label}
                {...(field.description ? { description: field.description } : {})}
                checked={value === true}
                onCheckedChange={(c: boolean | 'indeterminate') => {
                  setValue(field.name, c === true);
                  touch(field.name);
                }}
              />
              {err}
            </Field>
          );
        }
        case 'select':
          return (
            <Field key={field.name} {...common}>
              {label}
              {help}
              <NativeSelect
                name={field.name}
                value={typeof value === 'string' ? value : ''}
                options={field.options}
                placeholder={field.placeholder ?? messages.choose}
                onChange={(e) => setValue(field.name, e.target.value)}
                onBlur={() => touch(field.name)}
              />
              {err}
            </Field>
          );
        case 'number':
          return (
            <Field key={field.name} {...common}>
              {label}
              {help}
              <NumberControl
                field={field}
                value={value}
                onValue={(v) => setValue(field.name, v)}
                onBlur={() => touch(field.name)}
              />
              {err}
            </Field>
          );
        case 'date':
          return (
            <Field key={field.name} {...common}>
              {label}
              {help}
              <DatePicker
                name={field.name}
                value={typeof value === 'string' && value ? value : null}
                onValueChange={(v) => setValue(field.name, v)}
                {...(field.min ? { min: field.min } : {})}
                {...(field.max ? { max: field.max } : {})}
                {...(locale ? { locale } : {})}
                onValidationChange={(e) =>
                  setDateErrors((d) =>
                    Boolean(d[field.name]) === Boolean(e) ? d : { ...d, [field.name]: Boolean(e) },
                  )
                }
                onBlur={() => touch(field.name)}
              />
              {/* The picker shows its own format and range errors; ours only when it has none. */}
              {dateErrors[field.name] ? null : err}
            </Field>
          );
        case 'textarea':
          return (
            <Field key={field.name} {...common}>
              {label}
              {help}
              <Textarea
                name={field.name}
                value={typeof value === 'string' ? value : ''}
                rows={field.rows ?? 3}
                placeholder={field.placeholder}
                maxLength={field.maxLength}
                onChange={(e) => setValue(field.name, e.target.value)}
                onBlur={() => touch(field.name)}
              />
              {err}
            </Field>
          );
        default:
          return (
            <Field key={field.name} {...common}>
              {label}
              {help}
              <Input
                name={field.name}
                type={field.inputType ?? 'text'}
                value={typeof value === 'string' ? value : ''}
                placeholder={field.placeholder}
                autoComplete={field.autoComplete}
                maxLength={field.maxLength}
                onChange={(e) => setValue(field.name, e.target.value)}
                onBlur={() => touch(field.name)}
              />
              {err}
            </Field>
          );
      }
    };

    if (readOnly) {
      const shown = visibleFields(schema, values);
      return (
        <div className={cx('mn-schema-driven-form', className)} data-readonly="">
          <dl className="mn-schema-driven-form__view">
            {shown.map((field) => (
              <div key={field.name} className="mn-schema-driven-form__view-row">
                <dt>{field.label}</dt>
                <dd>
                  {formatSchemaValue(field, values[field.name], {
                    ...(locale ? { locale } : {}),
                    messages,
                  })}
                </dd>
              </div>
            ))}
          </dl>
        </div>
      );
    }

    return (
      <form
        ref={(node) => {
          formRef.current = node;
          if (typeof ref === 'function') ref(node);
          else if (ref) ref.current = node;
        }}
        noValidate
        {...rest}
        aria-busy={pending || undefined}
        onSubmit={submit}
        className={cx('mn-schema-driven-form', className)}
      >
        {schema.map((item) => {
          if (!isSection(item)) return renderField(item as SchemaDrivenFormField);
          const section = item as SchemaDrivenFormSection;
          if (!isSchemaFieldVisible(section.visibleWhen, values)) return null;
          const descId = section.description ? `${auto}-${section.id}-desc` : undefined;
          return (
            <fieldset
              key={section.id}
              className="mn-schema-driven-form__section"
              aria-describedby={descId}
              data-section={section.id}
            >
              <legend className="mn-schema-driven-form__legend">{section.title}</legend>
              {section.description ? (
                <p id={descId} className="mn-schema-driven-form__section-description">
                  {section.description}
                </p>
              ) : null}
              <div className="mn-schema-driven-form__fields">{section.fields.map(renderField)}</div>
            </fieldset>
          );
        })}
        {formError ? (
          <Alert tone="danger" live="assertive" className="mn-schema-driven-form__error">
            {formError}
          </Alert>
        ) : null}
        {submitLabel !== null || secondaryActions ? (
          <div className="mn-schema-driven-form__actions">
            {submitLabel !== null ? (
              <Button type="submit" variant="primary" loading={pending} disabled={disabled}>
                {submitLabel ?? messages.submit}
              </Button>
            ) : null}
            {secondaryActions}
          </div>
        ) : null}
      </form>
    );
  },
);
