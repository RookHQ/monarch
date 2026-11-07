'use client';

import { forwardRef, useId, type HTMLAttributes, type ReactNode } from 'react';
import { cx } from '../../../internal/cx.js';
import { BotGlyph } from '../../../internal/icons.js';
import { Button } from '../../actions/button/index.js';
import { FileUpload } from '../../files/file-upload/index.js';
import { Checkbox } from '../../forms/checkbox/index.js';
import { Combobox } from '../../forms/combobox/index.js';
import {
  Field,
  FieldDescription,
  FieldError,
  FieldLabel,
  NativeSelect,
} from '../../forms/form-field/index.js';
import { RatingPicker } from '../../forms/rating-picker/index.js';
import { Input, Textarea } from '../../forms/text-field/index.js';
import { DatePicker } from '../../scheduling/date-picker/index.js';
import {
  evaluateConditions,
  type Condition,
  type ConditionMatch,
} from '../condition-editor/index.js';

/** The kinds of question a form can ask. `section` is a heading with no answer. */
export type FormFieldType =
  | 'short-text'
  | 'long-text'
  | 'email'
  | 'phone'
  | 'url'
  | 'number'
  | 'date'
  | 'time'
  | 'select'
  | 'multi-select'
  | 'radio'
  | 'checkboxes'
  | 'yes-no'
  | 'consent'
  | 'rating'
  | 'file'
  | 'section';

/** A choice of a select, radio, checkboxes or multi-select field. */
export interface FormFieldOption {
  value: string;
  label: string;
}

/** A file answer. The product uploads `file` and may replace it with a stored `name`. */
export interface FormFieldFile {
  name: string;
  size?: number | undefined;
  file?: File | undefined;
}

/**
 * An answer: text (also dates as `YYYY-MM-DD` and times as `HH:MM`), a
 * number, a boolean (yes-no, consent), several option values, files, or none.
 */
export type FormFieldValue =
  string | number | boolean | readonly string[] | readonly FormFieldFile[] | null;

/** All answers, keyed by field id. */
export type FormFieldValues = Readonly<Record<string, FormFieldValue | undefined>>;

/** One question of a form, as data. */
export interface FormFieldDefinition {
  /** Stable, unique id; the key of the answer. */
  id: string;
  type: FormFieldType;
  label: string;
  /** Help under the label. */
  description?: string | undefined;
  placeholder?: string | undefined;
  required?: boolean | undefined;
  /** Choices of select, multi-select, radio and checkboxes fields. */
  options?: readonly FormFieldOption[] | undefined;
  /** Text length limits. */
  minLength?: number | undefined;
  maxLength?: number | undefined;
  /** Number limits, or the scale of a rating (default 1 to 5). */
  min?: number | undefined;
  max?: number | undefined;
  /** Date limits, `YYYY-MM-DD`. */
  minDate?: string | undefined;
  maxDate?: string | undefined;
  /** Selection limits of checkboxes and multi-select fields. */
  minSelections?: number | undefined;
  maxSelections?: number | undefined;
  /** Visible rows of a long-text field. Default 4. */
  rows?: number | undefined;
  /** Accepted file types, as the input `accept` attribute (e.g. `.pdf,image/*`). */
  accept?: string | undefined;
  /** Largest file in megabytes. */
  maxFileSizeMb?: number | undefined;
  /** Most files. Default 1. */
  maxFiles?: number | undefined;
  /** The statement a consent field asks people to agree to. Default: the label. */
  consentText?: ReactNode;
  /** Shown only when these conditions hold, tested against other answers by field id. */
  conditions?: readonly Condition[] | undefined;
  /** How `conditions` combine. Default `all`. */
  conditionMatch?: ConditionMatch | undefined;
  /** Shown but not editable, e.g. a verified email address. */
  readOnly?: boolean | undefined;
}

/** Labels and validation text. Override them to translate. */
export interface FieldRendererMessages {
  required: string;
  /** A required consent that is not ticked. */
  mustAgree: string;
  email: string;
  url: string;
  number: string;
  min: (n: number) => string;
  max: (n: number) => string;
  minLength: (n: number) => string;
  maxLength: (n: number) => string;
  minSelections: (n: number) => string;
  maxSelections: (n: number) => string;
  yes: string;
  no: string;
  choose: string;
  /** Note on an answer filled automatically with enough confidence. */
  aiFilled: string;
  /** Note on an answer filled automatically with low confidence. */
  aiCheck: string;
  /** Accessible name of a chosen file's remove button. */
  removeFile: (name: string) => string;
  remove: string;
}

const DEFAULT_MESSAGES: FieldRendererMessages = {
  required: 'This field is required.',
  mustAgree: 'Tick this to continue.',
  email: 'Enter an email address, like name@example.com.',
  url: 'Enter a web address, like https://example.com.',
  number: 'Enter a number.',
  min: (n) => `Enter ${n} or more.`,
  max: (n) => `Enter ${n} or less.`,
  minLength: (n) => `Use at least ${n} characters.`,
  maxLength: (n) => `Use ${n} characters or fewer.`,
  minSelections: (n) => `Choose at least ${n}.`,
  maxSelections: (n) => `Choose ${n} or fewer.`,
  yes: 'Yes',
  no: 'No',
  choose: 'Choose…',
  aiFilled: 'Filled in automatically. Check it is right.',
  aiCheck: 'Filled in automatically, but unsure. Please check.',
  removeFile: (name) => `Remove ${name}`,
  remove: 'Remove',
};

/** Whether an answer counts as empty. */
export function isFormFieldValueEmpty(value: FormFieldValue | undefined): boolean {
  return (
    value === undefined ||
    value === null ||
    value === '' ||
    (typeof value === 'number' && Number.isNaN(value)) ||
    (Array.isArray(value) && value.length === 0)
  );
}

/** Whether a field is shown, given every answer: its `conditions` must hold. */
export function isFormFieldVisible(field: FormFieldDefinition, values: FormFieldValues): boolean {
  if (!field.conditions?.length) return true;
  return evaluateConditions(field.conditions, values, field.conditionMatch ?? 'all');
}

/** Types whose input supports `readOnly`; others are disabled when read-only. */
const TEXT_LIKE: ReadonlySet<FormFieldType> = new Set([
  'short-text',
  'long-text',
  'email',
  'phone',
  'url',
  'number',
  'time',
]);

const EMAIL = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

/**
 * Checks one answer against its field: required, formats (email, web
 * address, number), limits and selections. Returns a message, or null.
 */
export function validateFormField(
  field: FormFieldDefinition,
  value: FormFieldValue | undefined,
  messages: Partial<FieldRendererMessages> = {},
): string | null {
  const m = { ...DEFAULT_MESSAGES, ...messages };
  if (field.type === 'section') return null;
  if (field.type === 'consent') return field.required && value !== true ? m.mustAgree : null;
  if (isFormFieldValueEmpty(value) || (field.type === 'yes-no' && typeof value !== 'boolean')) {
    if (typeof value === 'number' && Number.isNaN(value)) return m.number;
    return field.required ? m.required : null;
  }
  if (typeof value === 'string') {
    const text = value.trim();
    if (field.type === 'email' && !EMAIL.test(text)) return m.email;
    if (field.type === 'url') {
      try {
        const u = new URL(text);
        if (u.protocol !== 'https:' && u.protocol !== 'http:') return m.url;
      } catch {
        return m.url;
      }
    }
    if (field.minLength !== undefined && text.length < field.minLength)
      return m.minLength(field.minLength);
    if (field.maxLength !== undefined && text.length > field.maxLength)
      return m.maxLength(field.maxLength);
  }
  if (typeof value === 'number' && field.type === 'number') {
    if (field.min !== undefined && value < field.min) return m.min(field.min);
    if (field.max !== undefined && value > field.max) return m.max(field.max);
  }
  if (Array.isArray(value) && (field.type === 'checkboxes' || field.type === 'multi-select')) {
    if (field.minSelections !== undefined && value.length < field.minSelections)
      return m.minSelections(field.minSelections);
    if (field.maxSelections !== undefined && value.length > field.maxSelections)
      return m.maxSelections(field.maxSelections);
  }
  return null;
}

export interface FieldRendererProps extends Omit<HTMLAttributes<HTMLDivElement>, 'onChange'> {
  /** The question to render. */
  field: FormFieldDefinition;
  /** The current answer. */
  value: FormFieldValue | undefined;
  /** Called with the new answer. */
  onChange: (value: FormFieldValue) => void;
  /** An error to show, for example from `validateFormField` or the server. */
  error?: ReactNode;
  /**
   * Set when the answer was filled in automatically (for example from an
   * uploaded document): the confidence from 0 to 1. Shows a note with the
   * assistant glyph; below `confidenceThreshold` it asks people to check.
   */
  confidence?: number | undefined;
  /** Confidence under which the note asks for a check. Default 0.6. */
  confidenceThreshold?: number | undefined;
  disabled?: boolean | undefined;
  /** Heading level of a `section` field. Default 3. */
  headingLevel?: 2 | 3 | 4 | 5 | 6 | undefined;
  /** BCP 47 locale for the date picker. */
  locale?: string | undefined;
  /** Override labels and validation text. */
  messages?: Partial<FieldRendererMessages> | undefined;
}

/**
 * Renders one form question from data: the right input for its type (text,
 * email, phone, number, date, time, choices, yes / no, consent, rating, file
 * or a section heading), with label, help, required marker, error and an
 * optional "filled in automatically" note. Used by FormBuilder's preview and
 * by public forms.
 */
export const FieldRenderer = forwardRef<HTMLDivElement, FieldRendererProps>(function FieldRenderer(
  {
    field,
    value,
    onChange,
    error,
    confidence,
    confidenceThreshold = 0.6,
    disabled = false,
    headingLevel = 3,
    locale,
    messages: messagesProp,
    className,
    ...rest
  },
  ref,
) {
  const m = { ...DEFAULT_MESSAGES, ...messagesProp };
  const id = useId();
  const invalid = error !== undefined && error !== null && error !== false && error !== '';
  const aiFilled = confidence !== undefined;
  const lowConfidence = aiFilled && confidence < confidenceThreshold;
  const required = !!field.required;
  const off = disabled || !!field.readOnly;

  const rootProps = {
    ref,
    ...rest,
    className: cx('mn-field-renderer', className),
    'data-type': field.type,
    'data-ai': aiFilled ? (lowConfidence ? 'check' : 'filled') : undefined,
    'data-invalid': invalid || undefined,
  };

  const aiNote = aiFilled ? (
    <span className="mn-field-renderer__ai" data-low={lowConfidence || undefined}>
      <BotGlyph />
      <span>{lowConfidence ? m.aiCheck : m.aiFilled}</span>
    </span>
  ) : null;
  const help =
    field.description || aiNote ? (
      <>
        {field.description ? (
          <span className="mn-field-renderer__help">{field.description}</span>
        ) : null}
        {aiNote}
      </>
    ) : null;

  if (field.type === 'section') {
    const Heading = `h${headingLevel}` as 'h3';
    return (
      <div {...rootProps}>
        <Heading className="mn-field-renderer__section">{field.label}</Heading>
        {field.description ? <p className="mn-field-renderer__help">{field.description}</p> : null}
      </div>
    );
  }

  const str = typeof value === 'string' ? value : '';
  const list = Array.isArray(value)
    ? (value as readonly unknown[]).filter((x): x is string => typeof x === 'string')
    : [];
  const options = field.options ?? [];

  // Questions with several inputs are a fieldset with a legend.
  if (field.type === 'radio' || field.type === 'yes-no' || field.type === 'checkboxes') {
    const descId = help ? `${id}-desc` : undefined;
    const errId = invalid ? `${id}-err` : undefined;
    const choices: readonly FormFieldOption[] =
      field.type === 'yes-no'
        ? [
            { value: 'yes', label: m.yes },
            { value: 'no', label: m.no },
          ]
        : options;
    const current =
      field.type === 'yes-no' ? (value === true ? 'yes' : value === false ? 'no' : '') : str;
    return (
      <div {...rootProps}>
        <fieldset
          className="mn-field-renderer__group"
          aria-describedby={cx(descId, errId) || undefined}
          disabled={off}
        >
          <legend className="mn-field__label mn-field-renderer__legend">
            {field.label}
            {required ? (
              <span className="mn-field__required" aria-hidden>
                *
              </span>
            ) : null}
          </legend>
          {help ? (
            <p id={descId} className="mn-field__description">
              {help}
            </p>
          ) : null}
          <div
            className="mn-field-renderer__choices"
            data-inline={field.type === 'yes-no' || undefined}
          >
            {field.type === 'checkboxes'
              ? choices.map((o) => (
                  <Checkbox
                    key={o.value}
                    label={o.label}
                    checked={list.includes(o.value)}
                    onCheckedChange={(on) =>
                      onChange(on === true ? [...list, o.value] : list.filter((x) => x !== o.value))
                    }
                  />
                ))
              : choices.map((o) => (
                  <label key={o.value} className="mn-field-renderer__radio">
                    <input
                      type="radio"
                      name={id}
                      required={required}
                      value={o.value}
                      checked={current === o.value}
                      onChange={() =>
                        onChange(field.type === 'yes-no' ? o.value === 'yes' : o.value)
                      }
                    />
                    <span>{o.label}</span>
                  </label>
                ))}
          </div>
          {invalid ? (
            <p id={errId} className="mn-field__error" role="alert">
              {error}
            </p>
          ) : null}
        </fieldset>
      </div>
    );
  }

  if (field.type === 'consent') {
    return (
      <div {...rootProps}>
        <Field invalid={invalid} required={required} disabled={off}>
          <Checkbox
            label={
              <>
                {field.consentText ?? field.label}
                {required ? (
                  <span className="mn-field__required" aria-hidden>
                    *
                  </span>
                ) : null}
              </>
            }
            description={help}
            checked={value === true}
            onCheckedChange={(on) => onChange(on === true)}
          />
          <FieldError>{invalid ? error : null}</FieldError>
        </Field>
      </div>
    );
  }

  let control: ReactNode;
  switch (field.type) {
    case 'long-text':
      control = (
        <Textarea
          rows={field.rows ?? 4}
          value={str}
          readOnly={field.readOnly}
          maxLength={field.maxLength}
          placeholder={field.placeholder}
          onChange={(e) => onChange(e.target.value)}
        />
      );
      break;
    case 'number':
      control = (
        <Input
          type="number"
          inputMode="decimal"
          min={field.min}
          max={field.max}
          readOnly={field.readOnly}
          value={typeof value === 'number' && !Number.isNaN(value) ? String(value) : ''}
          placeholder={field.placeholder}
          onChange={(e) => onChange(e.target.value === '' ? null : Number(e.target.value))}
        />
      );
      break;
    case 'date':
      control = (
        <DatePicker
          value={str || null}
          onValueChange={(v) => onChange(v)}
          {...(field.minDate ? { min: field.minDate } : {})}
          {...(field.maxDate ? { max: field.maxDate } : {})}
          {...(locale ? { locale } : {})}
        />
      );
      break;
    case 'time':
      control = (
        <Input
          type="time"
          value={str}
          readOnly={field.readOnly}
          onChange={(e) => onChange(e.target.value)}
        />
      );
      break;
    case 'select':
      control = (
        <NativeSelect
          value={str}
          placeholder={field.placeholder ?? m.choose}
          options={options}
          onChange={(e) => onChange(e.target.value === '' ? null : e.target.value)}
        />
      );
      break;
    case 'multi-select':
      control = (
        <Combobox
          multiple
          options={options.map((o) => ({ value: o.value, label: o.label }))}
          values={list}
          placeholder={field.placeholder ?? m.choose}
          onValuesChange={(v) => onChange(v)}
        />
      );
      break;
    case 'rating':
      control = (
        <RatingPicker
          min={field.min ?? 1}
          max={field.max ?? 5}
          variant="stars"
          value={typeof value === 'number' ? value : null}
          clearable={!required}
          onValueChange={(v) => onChange(v)}
        />
      );
      break;
    case 'file': {
      const files = Array.isArray(value)
        ? (value as readonly unknown[]).filter(
            (x): x is FormFieldFile => typeof x === 'object' && x !== null,
          )
        : [];
      const maxFiles = field.maxFiles ?? 1;
      control = (
        <div className="mn-field-renderer__files">
          {files.length < maxFiles ? (
            <FileUpload
              variant="compact"
              maxFiles={maxFiles - files.length}
              {...(field.accept ? { accept: field.accept } : {})}
              {...(field.maxFileSizeMb ? { maxSize: field.maxFileSizeMb * 1024 * 1024 } : {})}
              disabled={off}
              invalid={invalid}
              onFiles={(accepted) =>
                onChange([
                  ...files,
                  ...accepted.map((f) => ({ name: f.name, size: f.size, file: f })),
                ])
              }
            />
          ) : null}
          {files.length > 0 ? (
            <ul className="mn-field-renderer__file-list">
              {files.map((f, i) => (
                <li key={`${f.name}-${i}`}>
                  <span className="mn-field-renderer__file-name">{f.name}</span>
                  {!off ? (
                    <Button
                      size="xs"
                      variant="ghost"
                      aria-label={m.removeFile(f.name)}
                      onClick={() => onChange(files.filter((_, j) => j !== i))}
                    >
                      {m.remove}
                    </Button>
                  ) : null}
                </li>
              ))}
            </ul>
          ) : null}
        </div>
      );
      break;
    }
    default: {
      const kind = field.type;
      control = (
        <Input
          type={
            kind === 'email' ? 'email' : kind === 'phone' ? 'tel' : kind === 'url' ? 'url' : 'text'
          }
          inputMode={
            kind === 'email'
              ? 'email'
              : kind === 'phone'
                ? 'tel'
                : kind === 'url'
                  ? 'url'
                  : undefined
          }
          autoComplete={
            kind === 'email'
              ? 'email'
              : kind === 'phone'
                ? 'tel'
                : kind === 'url'
                  ? 'url'
                  : undefined
          }
          value={str}
          readOnly={field.readOnly}
          maxLength={field.maxLength}
          placeholder={field.placeholder ?? (kind === 'url' ? 'https://' : undefined)}
          onChange={(e) => onChange(e.target.value)}
        />
      );
    }
  }

  return (
    <div {...rootProps}>
      <Field
        invalid={invalid}
        required={required}
        disabled={disabled || (!!field.readOnly && !TEXT_LIKE.has(field.type))}
      >
        <FieldLabel>{field.label}</FieldLabel>
        {help ? <FieldDescription>{help}</FieldDescription> : null}
        {control}
        <FieldError>{invalid ? error : null}</FieldError>
      </Field>
    </div>
  );
});
