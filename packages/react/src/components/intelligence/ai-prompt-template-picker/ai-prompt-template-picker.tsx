'use client';

import {
  forwardRef,
  useEffect,
  useId,
  useRef,
  useState,
  type FormEvent,
  type HTMLAttributes,
  type ReactNode,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { Button } from '../../actions/button/index.js';
import { Field, FieldDescription, FieldError, FieldLabel } from '../../forms/form-field/index.js';
import { Input, Textarea } from '../../forms/text-field/index.js';
import { TemplatePicker, type TemplatePickerCategory } from '../../forms/template-picker/index.js';

// ------------------------------------------------------------------ data

/** One blank in a prompt template, written `{{name}}` in the body. */
export interface AiPromptTemplateVariable {
  /** The name used in the body: `{{name}}`. */
  name: string;
  /** Field label, e.g. "Customer name". */
  label: string;
  /** Must be filled before the template can be used. Default false. */
  required?: boolean | undefined;
  /** Example value shown in the empty field. */
  placeholder?: string | undefined;
  /** Help under the field. */
  description?: string | undefined;
  /** Use a multi-line field. */
  multiline?: boolean | undefined;
  /** Initial value. */
  defaultValue?: string | undefined;
}

/** A reusable prompt. */
export interface AiPromptTemplate {
  /** Stable id. */
  id: string;
  /** Short name, shown on the card. */
  title: string;
  /** The prompt, with `{{name}}` blanks. */
  body: string;
  /** What it is for, shown on the card. */
  description?: string | undefined;
  /** Id of a category in `categories`. */
  category?: string | undefined;
  /** Extra words the search matches. */
  keywords?: readonly string[] | undefined;
  /**
   * The blanks, in form order. When omitted they are read from the body
   * (see `extractPromptVariables`) and all are required.
   */
  variables?: readonly AiPromptTemplateVariable[] | undefined;
}

const VARIABLE = /\{\{\s*([\w.-]+)\s*\}\}/g;

/** The distinct `{{name}}` blanks in a template body, in order of first use. */
export function extractPromptVariables(body: string): string[] {
  const names: string[] = [];
  for (const match of body.matchAll(VARIABLE)) {
    const name = match[1];
    if (name && !names.includes(name)) names.push(name);
  }
  return names;
}

/**
 * Fills a template body: each `{{name}}` (spaces inside the braces are
 * allowed) becomes `values[name]`. Blanks with no value (undefined) are
 * left as they are, so nothing is silently dropped; an empty string removes
 * the blank.
 */
export function fillPromptTemplate(body: string, values: Readonly<Record<string, string>>): string {
  return body.replace(VARIABLE, (whole, name: string) =>
    Object.prototype.hasOwnProperty.call(values, name) && values[name] !== undefined
      ? (values[name] as string)
      : whole,
  );
}

const humanize = (name: string) => {
  const words = name
    .replace(/[_.-]+/g, ' ')
    .replace(/([a-z])([A-Z])/g, '$1 $2')
    .trim();
  return words.charAt(0).toUpperCase() + words.slice(1).toLowerCase();
};

/** A template's blanks: its `variables`, or the ones read from the body (all required). */
export function promptTemplateVariables(template: AiPromptTemplate): AiPromptTemplateVariable[] {
  if (template.variables) return [...template.variables];
  return extractPromptVariables(template.body).map((name) => ({
    name,
    label: humanize(name),
    required: true,
  }));
}

// ------------------------------------------------------------------ messages

/** Every string AiPromptTemplatePicker shows or announces. */
export interface AiPromptTemplatePickerMessages {
  /** Name of the template gallery. */
  label: string;
  searchLabel: string;
  /** Name of the body preview beside the gallery. */
  previewLabel: string;
  /** Continues to the form (templates with blanks). */
  next: string;
  /** Uses the template (no blanks, or from the form). */
  use: string;
  back: string;
  cancel: string;
  /** Heading of the form step. */
  fillTitle: (title: string) => string;
  /** Name of the filled preview in the form step. */
  filledPreview: string;
  /** Shown after the label of an optional field. */
  optional: string;
  /** Error under an empty required field. */
  missing: (label: string) => string;
  /** Shown when nothing is selected yet. */
  pickFirst: string;
  /** Shown when there are no templates. */
  empty: string;
}

export const AI_PROMPT_TEMPLATE_PICKER_MESSAGES: AiPromptTemplatePickerMessages = {
  label: 'Prompt templates',
  searchLabel: 'Search templates',
  previewLabel: 'Template preview',
  next: 'Continue',
  use: 'Use template',
  back: 'Back',
  cancel: 'Cancel',
  fillTitle: (title) => `Fill in “${title}”`,
  filledPreview: 'Prompt preview',
  optional: 'Optional',
  missing: (label) => `Enter ${label.toLowerCase()}.`,
  pickFirst: 'Choose a template to continue.',
  empty: 'No templates yet.',
};

export interface AiPromptTemplatePickerProps extends Omit<
  HTMLAttributes<HTMLDivElement>,
  'onChange'
> {
  /** The templates, from the product. */
  templates: readonly AiPromptTemplate[];
  /**
   * Called with the filled prompt (and the template and values). Put the
   * text in the prompt input; never send it without the person seeing it.
   */
  onUse: (
    filledText: string,
    details: { template: AiPromptTemplate; values: Record<string, string> },
  ) => void;
  /** Shows a Cancel button. Close the surrounding popover or sheet here. */
  onCancel?: (() => void) | undefined;
  /** Category filters for the gallery. */
  categories?: readonly TemplatePickerCategory[] | undefined;
  /** Template selected at first. */
  defaultTemplateId?: string | undefined;
  /** Heading level of the form step. Default 3. */
  headingLevel?: 2 | 3 | 4 | 5 | 6 | undefined;
  /** Override any string. */
  messages?: Partial<AiPromptTemplatePickerMessages> | undefined;
}

/** Renders a body with its blanks marked, filled from `values` where given. */
function TemplateBody({
  body,
  values,
  labels,
}: {
  body: string;
  values?: Readonly<Record<string, string>>;
  labels: Readonly<Record<string, string>>;
}) {
  const parts: ReactNode[] = [];
  let last = 0;
  let key = 0;
  for (const match of body.matchAll(VARIABLE)) {
    const index = match.index ?? 0;
    const name = match[1] ?? '';
    if (index > last) parts.push(body.slice(last, index));
    const value = values?.[name];
    parts.push(
      value ? (
        <span key={key++} className="mn-ai-prompt-template-picker__value">
          {value}
        </span>
      ) : (
        <span key={key++} className="mn-ai-prompt-template-picker__blank">
          {labels[name] ?? name}
        </span>
      ),
    );
    last = index + match[0].length;
  }
  if (last < body.length) parts.push(body.slice(last));
  return <>{parts}</>;
}

/**
 * Picks a reusable prompt and fills in its blanks. Step one is a searchable
 * template gallery with a preview of the prompt; step two is a short form,
 * one field per `{{blank}}`, with a live preview of the finished prompt.
 * "Use template" hands the filled text to `onUse`, typically to put it in
 * AiPromptInput for the person to review and send.
 */
export const AiPromptTemplatePicker = forwardRef<HTMLDivElement, AiPromptTemplatePickerProps>(
  function AiPromptTemplatePicker(
    {
      templates,
      onUse,
      onCancel,
      categories,
      defaultTemplateId,
      headingLevel = 3,
      messages: messagesProp,
      className,
      ...rest
    },
    ref,
  ) {
    const m = { ...AI_PROMPT_TEMPLATE_PICKER_MESSAGES, ...messagesProp };
    const uid = useId();
    const Heading = `h${headingLevel}` as const;
    const SubHeading = `h${Math.min(6, headingLevel + 1) as 3 | 4 | 5 | 6}` as const;
    const [selectedId, setSelectedId] = useState<string | null>(defaultTemplateId ?? null);
    const [step, setStep] = useState<'pick' | 'fill'>('pick');
    const [values, setValues] = useState<Record<string, string>>({});
    const [errors, setErrors] = useState<Record<string, boolean>>({});
    const headingRef = useRef<HTMLHeadingElement>(null);
    const nextRef = useRef<HTMLButtonElement>(null);
    const fieldRefs = useRef(new Map<string, HTMLInputElement | HTMLTextAreaElement>());
    const focusOnStep = useRef<'heading' | 'next' | null>(null);

    const template = templates.find((t) => t.id === selectedId);
    const variables = template ? promptTemplateVariables(template) : [];
    const labelsOf = (t: AiPromptTemplate) =>
      Object.fromEntries(promptTemplateVariables(t).map((v) => [v.name, v.label]));

    useEffect(() => {
      const target = focusOnStep.current;
      focusOnStep.current = null;
      if (target === 'heading') headingRef.current?.focus();
      else if (target === 'next') nextRef.current?.focus();
    }, [step]);

    const select = (id: string) => {
      if (id === selectedId) return;
      setSelectedId(id);
      const t = templates.find((x) => x.id === id);
      const initial: Record<string, string> = {};
      for (const v of t ? promptTemplateVariables(t) : []) initial[v.name] = v.defaultValue ?? '';
      setValues(initial);
      setErrors({});
    };

    const apply = (t: AiPromptTemplate, filled: Record<string, string>) => {
      onUse(fillPromptTemplate(t.body, filled), { template: t, values: filled });
    };

    const next = () => {
      if (!template) return;
      if (variables.length === 0) {
        apply(template, {});
        return;
      }
      // Make sure every blank has an entry, so optional empty ones are removed, not left as {{name}}.
      setValues((current) => {
        const all: Record<string, string> = {};
        for (const v of variables) all[v.name] = current[v.name] ?? v.defaultValue ?? '';
        return all;
      });
      focusOnStep.current = 'heading';
      setStep('fill');
    };

    const submit = (event: FormEvent<HTMLFormElement>) => {
      event.preventDefault();
      if (!template) return;
      const missing = variables.filter((v) => v.required && !(values[v.name] ?? '').trim());
      if (missing.length) {
        setErrors(Object.fromEntries(missing.map((v) => [v.name, true])));
        const first = missing[0];
        if (first) fieldRefs.current.get(first.name)?.focus();
        return;
      }
      const filled: Record<string, string> = {};
      for (const v of variables) filled[v.name] = values[v.name] ?? '';
      apply(template, filled);
    };

    const back = () => {
      focusOnStep.current = 'next';
      setStep('pick');
    };

    if (templates.length === 0) {
      return (
        <div ref={ref} {...rest} className={cx('mn-ai-prompt-template-picker', className)}>
          <p className="mn-ai-prompt-template-picker__empty">{m.empty}</p>
        </div>
      );
    }

    return (
      <div
        ref={ref}
        {...rest}
        className={cx('mn-ai-prompt-template-picker', className)}
        data-step={step}
      >
        {step === 'pick' ? (
          <>
            <TemplatePicker
              aria-label={m.label}
              className="mn-ai-prompt-template-picker__gallery"
              templates={templates.map((t) => ({
                id: t.id,
                name: t.title,
                ...(t.description !== undefined ? { description: t.description } : {}),
                ...(t.category !== undefined ? { category: t.category } : {}),
                ...(t.keywords !== undefined ? { keywords: t.keywords } : {}),
              }))}
              {...(categories ? { categories } : {})}
              value={selectedId}
              onValueChange={(id) => select(id)}
              minCardWidth="10rem"
              messages={{
                searchLabel: m.searchLabel,
                searchPlaceholder: m.searchLabel,
                previewLabel: m.previewLabel,
              }}
              renderPreview={() =>
                template ? (
                  <p className="mn-ai-prompt-template-picker__body">
                    <TemplateBody body={template.body} labels={labelsOf(template)} />
                  </p>
                ) : null
              }
            />
            <div className="mn-ai-prompt-template-picker__actions">
              {template ? null : (
                <p className="mn-ai-prompt-template-picker__hint">{m.pickFirst}</p>
              )}
              {onCancel ? (
                <Button variant="ghost" onClick={onCancel}>
                  {m.cancel}
                </Button>
              ) : null}
              <Button ref={nextRef} variant="primary" disabled={!template} onClick={next}>
                {template && variables.length === 0 ? m.use : m.next}
              </Button>
            </div>
          </>
        ) : template ? (
          <form className="mn-ai-prompt-template-picker__form" noValidate onSubmit={submit}>
            <Heading ref={headingRef} tabIndex={-1} className="mn-ai-prompt-template-picker__title">
              {m.fillTitle(template.title)}
            </Heading>
            <div className="mn-ai-prompt-template-picker__fields">
              {variables.map((v) => {
                const invalid = Boolean(errors[v.name]);
                const setRef = (el: HTMLInputElement | HTMLTextAreaElement | null) => {
                  if (el) fieldRefs.current.set(v.name, el);
                  else fieldRefs.current.delete(v.name);
                };
                const onChange = (value: string) => {
                  setValues((current) => ({ ...current, [v.name]: value }));
                  if (invalid && value.trim()) {
                    setErrors((current) => ({ ...current, [v.name]: false }));
                  }
                };
                return (
                  <Field key={v.name} invalid={invalid} required={Boolean(v.required)}>
                    <FieldLabel optionalText={m.optional}>{v.label}</FieldLabel>
                    {v.description ? <FieldDescription>{v.description}</FieldDescription> : null}
                    {v.multiline ? (
                      <Textarea
                        ref={setRef}
                        name={v.name}
                        rows={3}
                        value={values[v.name] ?? ''}
                        placeholder={v.placeholder}
                        onChange={(e) => onChange(e.target.value)}
                      />
                    ) : (
                      <Input
                        ref={setRef}
                        name={v.name}
                        value={values[v.name] ?? ''}
                        placeholder={v.placeholder}
                        onChange={(e) => onChange(e.target.value)}
                      />
                    )}
                    {invalid ? <FieldError>{m.missing(v.label)}</FieldError> : null}
                  </Field>
                );
              })}
            </div>
            <section
              className="mn-ai-prompt-template-picker__preview"
              aria-labelledby={`${uid}-preview`}
            >
              <SubHeading
                id={`${uid}-preview`}
                className="mn-ai-prompt-template-picker__preview-title"
              >
                {m.filledPreview}
              </SubHeading>
              <p className="mn-ai-prompt-template-picker__body">
                <TemplateBody body={template.body} values={values} labels={labelsOf(template)} />
              </p>
            </section>
            <div className="mn-ai-prompt-template-picker__actions">
              <Button variant="ghost" onClick={back}>
                {m.back}
              </Button>
              {onCancel ? (
                <Button variant="ghost" onClick={onCancel}>
                  {m.cancel}
                </Button>
              ) : null}
              <Button type="submit" variant="primary">
                {m.use}
              </Button>
            </div>
          </form>
        ) : null}
      </div>
    );
  },
);
