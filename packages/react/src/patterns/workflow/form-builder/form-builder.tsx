'use client';

import {
  forwardRef,
  useEffect,
  useId,
  useRef,
  useState,
  type HTMLAttributes,
  type ReactNode,
  type RefObject,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { CopyGlyph, PlusGlyph, CloseGlyph } from '../../../internal/icons.js';
import { Button, IconButton } from '../../../components/actions/button/index.js';
import { Badge } from '../../../components/data-display/badge/index.js';
import { EmptyState } from '../../../components/feedback/empty-state/index.js';
import { Checkbox } from '../../../components/forms/checkbox/index.js';
import { Field, FieldDescription, FieldLabel } from '../../../components/forms/form-field/index.js';
import { Input, Textarea } from '../../../components/forms/text-field/index.js';
import { ReorderableList } from '../../../components/interaction/reorderable-list/index.js';
import {
  Tabs,
  TabsContent,
  TabsList,
  TabsTrigger,
} from '../../../components/navigation/tabs/index.js';
import {
  ConditionEditor,
  type ConditionField,
  type ConditionMatch,
} from '../../../components/workflow/condition-editor/index.js';
import {
  FieldRenderer,
  isFormFieldVisible,
  validateFormField,
  type FormFieldDefinition,
  type FormFieldOption,
  type FormFieldType,
  type FormFieldValue,
} from '../../../components/workflow/field-renderer/index.js';

/** A field type offered in the "Add a field" palette. */
export interface FormBuilderFieldType {
  type: FormFieldType;
  /** Visible name. Default: `messages.types[type]`. */
  label?: string | undefined;
  /** Palette heading. Default: the built-in group of the type. */
  group?: string | undefined;
}

/** Why a field cannot be saved. */
export type FormFieldProblem = 'label' | 'options' | 'condition-order' | 'condition';

/** Labels. Override them to translate. */
export interface FormBuilderMessages {
  addField: string;
  /** Accessible name of the Build / Preview tabs. */
  mode: string;
  build: string;
  preview: string;
  /** Accessible name of the field list. */
  fields: string;
  /** Names of field types. */
  types: Record<FormFieldType, string>;
  /** Palette group headings. */
  groups: { text: string; choice: string; other: string };
  emptyTitle: string;
  emptyDescription: string;
  selectHint: string;
  untitled: string;
  required: string;
  conditional: string;
  /** Accessible names of row buttons. */
  edit: (label: string) => string;
  duplicate: (label: string) => string;
  remove: (label: string) => string;
  /** Suffix of a duplicated field's label. */
  copySuffix: string;
  removed: (label: string) => string;
  undo: string;
  settings: (type: string) => string;
  label: string;
  helpText: string;
  placeholder: string;
  requiredField: string;
  options: string;
  option: (position: number) => string;
  addOption: string;
  removeOption: (label: string) => string;
  newOption: (position: number) => string;
  min: string;
  max: string;
  minLength: string;
  maxLength: string;
  consentText: string;
  accept: string;
  acceptHelp: string;
  maxFiles: string;
  logic: string;
  logicHelp: string;
  /** Text of a condition field that comes after the edited one. */
  comesLater: string;
  problems: Record<FormFieldProblem, string>;
  maxReached: (max: number) => string;
  previewEmpty: string;
}

const TYPE_LABELS: Record<FormFieldType, string> = {
  'short-text': 'Short text',
  'long-text': 'Long text',
  email: 'Email',
  phone: 'Phone',
  url: 'Web address',
  number: 'Number',
  date: 'Date',
  time: 'Time',
  select: 'Dropdown',
  'multi-select': 'Multiple choice list',
  radio: 'Single choice',
  checkboxes: 'Checkboxes',
  'yes-no': 'Yes / no',
  consent: 'Consent',
  rating: 'Rating',
  file: 'File upload',
  section: 'Section heading',
};

const DEFAULT_MESSAGES: FormBuilderMessages = {
  addField: 'Add a field',
  mode: 'Form view',
  build: 'Build',
  preview: 'Preview',
  fields: 'Form fields',
  types: TYPE_LABELS,
  groups: { text: 'Text', choice: 'Choice', other: 'Other' },
  emptyTitle: 'This form is empty',
  emptyDescription: 'Pick a field type from “Add a field” to ask your first question.',
  selectHint: 'Select a field to edit its settings.',
  untitled: 'Untitled',
  required: 'Required',
  conditional: 'Conditional',
  edit: (l) => `Edit ${l}`,
  duplicate: (l) => `Duplicate ${l}`,
  remove: (l) => `Remove ${l}`,
  copySuffix: ' (copy)',
  removed: (l) => `Removed ${l}.`,
  undo: 'Undo',
  settings: (t) => `${t} settings`,
  label: 'Question',
  helpText: 'Help text (optional)',
  placeholder: 'Placeholder (optional)',
  requiredField: 'Required',
  options: 'Options',
  option: (n) => `Option ${n}`,
  addOption: 'Add option',
  removeOption: (l) => `Remove option ${l}`,
  newOption: (n) => `Option ${n}`,
  min: 'Minimum',
  max: 'Maximum',
  minLength: 'Minimum characters',
  maxLength: 'Maximum characters',
  consentText: 'Statement to agree to',
  accept: 'Allowed file types (optional)',
  acceptHelp: 'For example .pdf,.docx or image/*',
  maxFiles: 'Most files',
  logic: 'Show this field when',
  logicHelp: 'Always shown when there are no conditions. Conditions can use earlier fields only.',
  comesLater: 'comes after this field',
  problems: {
    label: 'Write the question.',
    options: 'Add at least one option.',
    'condition-order':
      'A condition refers to a field that now comes after this one. Move the field or change the condition.',
    condition: 'Finish or remove the incomplete condition.',
  },
  maxReached: (max) => `A form can have up to ${max} fields.`,
  previewEmpty: 'Nothing to preview yet.',
};

const TYPE_GROUP: Record<FormFieldType, 'text' | 'choice' | 'other'> = {
  'short-text': 'text',
  'long-text': 'text',
  email: 'text',
  phone: 'text',
  url: 'text',
  number: 'text',
  date: 'other',
  time: 'other',
  select: 'choice',
  'multi-select': 'choice',
  radio: 'choice',
  checkboxes: 'choice',
  'yes-no': 'choice',
  consent: 'other',
  rating: 'choice',
  file: 'other',
  section: 'other',
};

const ALL_TYPES = Object.keys(TYPE_LABELS) as FormFieldType[];
const OPTION_TYPES: ReadonlySet<FormFieldType> = new Set([
  'select',
  'multi-select',
  'radio',
  'checkboxes',
]);
const TEXT_TYPES: ReadonlySet<FormFieldType> = new Set(['short-text', 'long-text']);
const PLACEHOLDER_TYPES: ReadonlySet<FormFieldType> = new Set([
  'short-text',
  'long-text',
  'email',
  'phone',
  'url',
  'number',
  'select',
  'multi-select',
]);

/** Whether a field type has options to edit. */
export const formFieldHasOptions = (type: FormFieldType) => OPTION_TYPES.has(type);

/**
 * The condition-editor view of a field, so later fields can be shown only
 * when it has some answer. Null for fields without an answer (sections).
 */
export function formFieldToConditionField(field: FormFieldDefinition): ConditionField | null {
  const base = { value: field.id, label: field.label || TYPE_LABELS[field.type] };
  switch (field.type) {
    case 'section':
      return null;
    case 'number':
    case 'rating':
      return { ...base, type: 'number' };
    case 'date':
      return { ...base, type: 'date' };
    case 'select':
    case 'radio':
      return { ...base, type: 'select', options: field.options ?? [] };
    case 'multi-select':
    case 'checkboxes':
      return { ...base, type: 'multi-select', options: field.options ?? [] };
    case 'yes-no':
    case 'consent':
      return { ...base, type: 'boolean' };
    case 'file':
      return { ...base, type: 'text', operators: ['is_not_empty', 'is_empty'] };
    default:
      return { ...base, type: 'text' };
  }
}

/**
 * Checks every field: a question, options for choice fields, and conditions
 * that are complete and refer only to earlier fields (or `contextFields`).
 * Returns problems keyed by field id; empty when the form can be saved.
 */
export function validateFormDefinition(
  fields: readonly FormFieldDefinition[],
  contextFields: readonly ConditionField[] = [],
): Record<string, FormFieldProblem> {
  const out: Record<string, FormFieldProblem> = {};
  const context = new Set(contextFields.map((f) => f.value));
  fields.forEach((field, index) => {
    if (!field.label.trim()) out[field.id] = 'label';
    else if (OPTION_TYPES.has(field.type) && !(field.options ?? []).some((o) => o.label.trim()))
      out[field.id] = 'options';
    else if (field.conditions?.length) {
      const earlier = new Set(fields.slice(0, index).map((f) => f.id));
      if (field.conditions.some((c) => !earlier.has(c.field) && !context.has(c.field)))
        out[field.id] = 'condition-order';
      else if (
        field.conditions.some(
          (c) =>
            !['is_empty', 'is_not_empty', 'is_true', 'is_false'].includes(c.operator) &&
            (c.value === undefined ||
              c.value === null ||
              c.value === '' ||
              (Array.isArray(c.value) && c.value.length === 0)),
        )
      )
        out[field.id] = 'condition';
    }
  });
  return out;
}

export interface FormBuilderProps extends Omit<HTMLAttributes<HTMLDivElement>, 'onChange'> {
  /** The fields, in order (controlled). */
  value: readonly FormFieldDefinition[];
  /** Called with the full new list after every change. */
  onChange: (fields: FormFieldDefinition[]) => void;
  /** Types in the palette, in order. Default: every type. */
  fieldTypes?: readonly (FormFieldType | FormBuilderFieldType)[] | undefined;
  /**
   * Extra things conditions can test that are not questions, such as the
   * booked service. Their values come from `previewContext` in the preview.
   */
  contextFields?: readonly ConditionField[] | undefined;
  /** Values of `contextFields` used by the preview, keyed by field value. */
  previewContext?: Readonly<Record<string, FormFieldValue>> | undefined;
  /** Problems to show per field id, for example from the server. Shown with the built-in checks. */
  errors?: Readonly<Record<string, ReactNode>> | undefined;
  /** Show the built-in problems (for example after a save attempt). Default false. */
  showErrors?: boolean | undefined;
  /** The selected field (controlled). */
  selectedId?: string | null | undefined;
  onSelectedIdChange?: ((id: string | null) => void) | undefined;
  /** Most fields. Default 100. */
  maxFields?: number | undefined;
  /** View only: no palette, settings disabled, no reordering. */
  readOnly?: boolean | undefined;
  /** Makes ids for new fields and options. Default: generated ids. */
  createId?: ((kind: 'field' | 'option') => string) | undefined;
  /** Override labels. */
  messages?: Partial<FormBuilderMessages> | undefined;
}

/**
 * Builds a form from fields: a palette of field types, a reorderable list,
 * settings for the selected field (question, help, required, options, limits
 * and "show this field when" logic), and a live preview rendered with
 * FieldRenderer. Fields are plain data the product saves and publishes.
 */
export const FormBuilder = forwardRef<HTMLDivElement, FormBuilderProps>(function FormBuilder(
  {
    value: fields,
    onChange,
    fieldTypes,
    contextFields = [],
    previewContext,
    errors: externalErrors,
    showErrors = false,
    selectedId: selectedProp,
    onSelectedIdChange,
    maxFields = 100,
    readOnly = false,
    createId,
    messages: messagesProp,
    className,
    ...rest
  },
  ref,
) {
  const m: FormBuilderMessages = {
    ...DEFAULT_MESSAGES,
    ...messagesProp,
    types: { ...TYPE_LABELS, ...messagesProp?.types },
    groups: { ...DEFAULT_MESSAGES.groups, ...messagesProp?.groups },
    problems: { ...DEFAULT_MESSAGES.problems, ...messagesProp?.problems },
  };
  const baseId = useId();
  const counter = useRef(0);
  const [selectedInternal, setSelectedInternal] = useState<string | null>(fields[0]?.id ?? null);
  const selectedId = selectedProp !== undefined ? selectedProp : selectedInternal;
  const select = (id: string | null) => {
    if (selectedProp === undefined) setSelectedInternal(id);
    onSelectedIdChange?.(id);
  };
  const [mode, setMode] = useState<'build' | 'preview'>('build');
  const [undo, setUndo] = useState<{
    fields: FormFieldDefinition[];
    label: string;
    id: string;
  } | null>(null);
  const [answers, setAnswers] = useState<Record<string, FormFieldValue>>({});
  const focusLabel = useRef(false);
  const labelRef = useRef<HTMLInputElement | null>(null);

  useEffect(() => {
    if (focusLabel.current) {
      focusLabel.current = false;
      labelRef.current?.focus();
    }
  });

  const newId = (kind: 'field' | 'option') => {
    counter.current += 1;
    return createId?.(kind) ?? `${baseId}-${kind}-${counter.current}`;
  };
  const labelOf = (f: FormFieldDefinition) => f.label.trim() || m.untitled;
  const builtIn = showErrors ? validateFormDefinition(fields, contextFields) : {};
  const problemOf = (id: string): ReactNode =>
    externalErrors?.[id] ?? (builtIn[id] ? m.problems[builtIn[id]] : null);

  const commit = (next: FormFieldDefinition[]) => {
    setUndo(null);
    onChange(next);
  };
  const update = (next: FormFieldDefinition) =>
    commit(fields.map((f) => (f.id === next.id ? next : f)));

  const types: FormBuilderFieldType[] = (fieldTypes ?? ALL_TYPES).map((t) =>
    typeof t === 'string' ? { type: t } : t,
  );
  const groupOf = (t: FormBuilderFieldType) => t.group ?? m.groups[TYPE_GROUP[t.type]];
  const groups: string[] = [];
  for (const t of types) if (!groups.includes(groupOf(t))) groups.push(groupOf(t));

  const add = (type: FormFieldType) => {
    if (fields.length >= maxFields) return;
    const field: FormFieldDefinition = { id: newId('field'), type, label: '' };
    if (OPTION_TYPES.has(type))
      field.options = [1, 2].map((n) => ({ value: newId('option'), label: m.newOption(n) }));
    const at = fields.findIndex((f) => f.id === selectedId);
    const index = at >= 0 ? at + 1 : fields.length;
    commit([...fields.slice(0, index), field, ...fields.slice(index)]);
    select(field.id);
    setMode('build');
    focusLabel.current = true;
  };

  const duplicate = (field: FormFieldDefinition) => {
    if (fields.length >= maxFields) return;
    const index = fields.findIndex((f) => f.id === field.id);
    const copy: FormFieldDefinition = {
      ...field,
      id: newId('field'),
      label: `${field.label}${m.copySuffix}`,
      ...(field.options
        ? {
            options: field.options.map((o) => ({ ...o, value: newId('option') })),
          }
        : {}),
    };
    commit([...fields.slice(0, index + 1), copy, ...fields.slice(index + 1)]);
    select(copy.id);
  };

  const remove = (field: FormFieldDefinition) => {
    const index = fields.findIndex((f) => f.id === field.id);
    // Conditions that tested the removed field go with it.
    const next = fields
      .filter((f) => f.id !== field.id)
      .map((f) => {
        if (!f.conditions?.some((c) => c.field === field.id)) return f;
        const copy: FormFieldDefinition = {
          ...f,
          conditions: f.conditions.filter((c) => c.field !== field.id),
        };
        if (!copy.conditions?.length) delete copy.conditions;
        return copy;
      });
    onChange(next);
    setUndo({ fields: [...fields], label: labelOf(field), id: field.id });
    if (selectedId === field.id) select(next[index]?.id ?? next[index - 1]?.id ?? null);
  };

  const selectedIndex = fields.findIndex((f) => f.id === selectedId);
  const selected = selectedIndex >= 0 ? fields[selectedIndex] : undefined;

  const palette = readOnly ? null : (
    <section className="mn-form-builder__palette" aria-labelledby={`${baseId}-palette`}>
      <h3 id={`${baseId}-palette`} className="mn-form-builder__heading">
        {m.addField}
      </h3>
      {groups.map((g) => (
        <div key={g} role="group" aria-label={g} className="mn-form-builder__palette-group">
          <p className="mn-form-builder__palette-title" aria-hidden>
            {g}
          </p>
          <div className="mn-form-builder__palette-items">
            {types
              .filter((t) => groupOf(t) === g)
              .map((t) => (
                <Button
                  key={t.type}
                  size="sm"
                  variant="ghost"
                  leadingIcon={<PlusGlyph />}
                  disabled={fields.length >= maxFields}
                  onClick={() => add(t.type)}
                >
                  {t.label ?? m.types[t.type]}
                </Button>
              ))}
          </div>
        </div>
      ))}
      {fields.length >= maxFields ? (
        <p className="mn-form-builder__note">{m.maxReached(maxFields)}</p>
      ) : null}
    </section>
  );

  const list =
    fields.length === 0 ? (
      <EmptyState
        size="compact"
        headingLevel="p"
        icon={<PlusGlyph />}
        title={m.emptyTitle}
        description={m.emptyDescription}
      />
    ) : (
      <ReorderableList
        className="mn-form-builder__list"
        aria-label={m.fields}
        items={fields}
        getKey={(f) => f.id}
        getLabel={labelOf}
        disabled={readOnly}
        onReorder={(next) => commit(next)}
        renderItem={(field) => {
          const problem = problemOf(field.id);
          const isSelected = field.id === selectedId;
          return (
            <div
              className="mn-form-builder__row"
              data-selected={isSelected || undefined}
              data-invalid={problem ? '' : undefined}
            >
              <button
                type="button"
                className="mn-form-builder__select"
                aria-pressed={isSelected}
                aria-label={m.edit(labelOf(field))}
                onClick={() => select(field.id)}
              >
                <span
                  className="mn-form-builder__row-label"
                  data-untitled={!field.label.trim() || undefined}
                >
                  {labelOf(field)}
                </span>
                <span className="mn-form-builder__row-meta">
                  <span>{m.types[field.type]}</span>
                  {field.required ? <Badge size="sm">{m.required}</Badge> : null}
                  {field.conditions?.length ? (
                    <Badge size="sm" tone="info">
                      {m.conditional}
                    </Badge>
                  ) : null}
                </span>
                {problem ? <span className="mn-form-builder__row-problem">{problem}</span> : null}
              </button>
              {!readOnly ? (
                <span className="mn-form-builder__row-actions">
                  <IconButton
                    size="sm"
                    icon={<CopyGlyph />}
                    label={m.duplicate(labelOf(field))}
                    disabled={fields.length >= maxFields}
                    onClick={() => duplicate(field)}
                  />
                  <IconButton
                    size="sm"
                    icon={<CloseGlyph />}
                    label={m.remove(labelOf(field))}
                    onClick={() => remove(field)}
                  />
                </span>
              ) : null}
            </div>
          );
        }}
      />
    );

  return (
    <div
      ref={ref}
      {...rest}
      className={cx('mn-form-builder', className)}
      data-read-only={readOnly || undefined}
    >
      <Tabs value={mode} onValueChange={(v) => setMode(v as 'build' | 'preview')}>
        <TabsList aria-label={m.mode} className="mn-form-builder__tabs">
          <TabsTrigger value="build">{m.build}</TabsTrigger>
          <TabsTrigger value="preview">{m.preview}</TabsTrigger>
        </TabsList>
        <TabsContent value="build" className="mn-form-builder__build">
          {palette}
          <div className="mn-form-builder__fields">
            {list}
            <div className="mn-form-builder__status" role="status" aria-live="polite">
              {undo ? (
                <>
                  <span>{m.removed(undo.label)}</span>
                  <Button
                    size="xs"
                    variant="secondary"
                    onClick={() => {
                      onChange(undo.fields);
                      select(undo.id);
                      setUndo(null);
                    }}
                  >
                    {m.undo}
                  </Button>
                </>
              ) : null}
            </div>
          </div>
          <section
            className="mn-form-builder__settings"
            aria-label={selected ? m.settings(m.types[selected.type]) : m.selectHint}
          >
            {selected ? (
              <FieldSettings
                key={selected.id}
                field={selected}
                index={selectedIndex}
                fields={fields}
                contextFields={contextFields}
                readOnly={readOnly}
                problem={problemOf(selected.id)}
                showErrors={showErrors}
                labelRef={labelRef}
                newOptionId={() => newId('option')}
                messages={m}
                onChange={update}
              />
            ) : (
              <p className="mn-form-builder__note">{m.selectHint}</p>
            )}
          </section>
        </TabsContent>
        <TabsContent value="preview" className="mn-form-builder__preview">
          {fields.length === 0 ? (
            <p className="mn-form-builder__note">{m.previewEmpty}</p>
          ) : (
            <form className="mn-form-builder__preview-form" onSubmit={(e) => e.preventDefault()}>
              {fields
                .filter((f) => isFormFieldVisible(f, { ...previewContext, ...answers }))
                .map((f) => (
                  <FieldRenderer
                    key={f.id}
                    field={f}
                    value={answers[f.id]}
                    error={answers[f.id] !== undefined ? validateFormField(f, answers[f.id]) : null}
                    onChange={(v) => setAnswers((a) => ({ ...a, [f.id]: v }))}
                  />
                ))}
            </form>
          )}
        </TabsContent>
      </Tabs>
    </div>
  );
});

interface FieldSettingsProps {
  field: FormFieldDefinition;
  index: number;
  fields: readonly FormFieldDefinition[];
  contextFields: readonly ConditionField[];
  readOnly: boolean;
  problem: ReactNode;
  showErrors: boolean;
  labelRef: RefObject<HTMLInputElement | null>;
  newOptionId: () => string;
  messages: FormBuilderMessages;
  onChange: (field: FormFieldDefinition) => void;
}

const numberOrUndefined = (text: string) => (text.trim() === '' ? undefined : Number(text));

function FieldSettings({
  field,
  index,
  fields,
  contextFields,
  readOnly,
  problem,
  showErrors,
  labelRef,
  newOptionId,
  messages: m,
  onChange,
}: FieldSettingsProps) {
  const id = useId();
  const set = (patch: Partial<FormFieldDefinition>) => {
    // Keys set to undefined are dropped, so saved definitions stay minimal.
    const next = Object.fromEntries(
      Object.entries({ ...field, ...patch }).filter(([, v]) => v !== undefined),
    ) as unknown as FormFieldDefinition;
    onChange(next);
  };
  const options = field.options ?? [];
  const setOption = (i: number, patch: Partial<FormFieldOption>) =>
    set({ options: options.map((o, j) => (j === i ? { ...o, ...patch } : o)) });

  // Earlier answerable fields can drive conditions; later ones are listed but disabled.
  const conditionFields: ConditionField[] = [
    ...contextFields,
    ...fields.flatMap((f, j) => {
      if (f.id === field.id) return [];
      const cf = formFieldToConditionField(f);
      if (!cf) return [];
      return j < index ? [cf] : [{ ...cf, disabled: true, disabledReason: m.comesLater }];
    }),
  ];

  const isSection = field.type === 'section';
  return (
    <fieldset className="mn-form-builder__settings-body" disabled={readOnly}>
      <legend className="mn-form-builder__heading">{m.settings(m.types[field.type])}</legend>
      {problem ? <p className="mn-form-builder__problem">{problem}</p> : null}
      <Field invalid={showErrors && !field.label.trim()} required>
        <FieldLabel>{m.label}</FieldLabel>
        <Input
          ref={labelRef}
          value={field.label}
          maxLength={300}
          onChange={(e) => set({ label: e.target.value })}
        />
      </Field>
      <Field>
        <FieldLabel>{m.helpText}</FieldLabel>
        <Textarea
          rows={2}
          value={field.description ?? ''}
          maxLength={500}
          onChange={(e) => set({ description: e.target.value || undefined })}
        />
      </Field>
      {PLACEHOLDER_TYPES.has(field.type) ? (
        <Field>
          <FieldLabel>{m.placeholder}</FieldLabel>
          <Input
            value={field.placeholder ?? ''}
            maxLength={120}
            onChange={(e) => set({ placeholder: e.target.value || undefined })}
          />
        </Field>
      ) : null}
      {field.type === 'consent' ? (
        <Field>
          <FieldLabel>{m.consentText}</FieldLabel>
          <Textarea
            rows={2}
            value={typeof field.consentText === 'string' ? field.consentText : ''}
            onChange={(e) => set({ consentText: e.target.value || undefined })}
          />
        </Field>
      ) : null}
      {!isSection ? (
        <Checkbox
          label={m.requiredField}
          checked={!!field.required}
          onCheckedChange={(on) => set({ required: on === true || undefined })}
        />
      ) : null}

      {OPTION_TYPES.has(field.type) ? (
        <div role="group" aria-labelledby={`${id}-options`} className="mn-form-builder__options">
          <p id={`${id}-options`} className="mn-form-builder__subheading">
            {m.options}
          </p>
          <ReorderableList
            items={options}
            getKey={(o) => o.value}
            getLabel={(o) => o.label || m.untitled}
            disabled={readOnly}
            onReorder={(next) => set({ options: next })}
            renderItem={(o, { index: i }) => (
              <div className="mn-form-builder__option">
                <Input
                  aria-label={m.option(i + 1)}
                  value={o.label}
                  maxLength={200}
                  onChange={(e) => setOption(i, { label: e.target.value })}
                />
                <IconButton
                  size="sm"
                  icon={<CloseGlyph />}
                  label={m.removeOption(o.label || m.option(i + 1))}
                  disabled={options.length <= 1}
                  onClick={() => set({ options: options.filter((x) => x.value !== o.value) })}
                />
              </div>
            )}
          />
          <Button
            size="sm"
            variant="secondary"
            leadingIcon={<PlusGlyph />}
            className="mn-form-builder__add-option"
            onClick={() =>
              set({
                options: [
                  ...options,
                  { value: newOptionId(), label: m.newOption(options.length + 1) },
                ],
              })
            }
          >
            {m.addOption}
          </Button>
        </div>
      ) : null}

      {field.type === 'number' || field.type === 'rating' ? (
        <div className="mn-form-builder__pair">
          <Field>
            <FieldLabel>{m.min}</FieldLabel>
            <Input
              type="number"
              value={field.min ?? ''}
              onChange={(e) => set({ min: numberOrUndefined(e.target.value) })}
            />
          </Field>
          <Field>
            <FieldLabel>{m.max}</FieldLabel>
            <Input
              type="number"
              value={field.max ?? ''}
              onChange={(e) => set({ max: numberOrUndefined(e.target.value) })}
            />
          </Field>
        </div>
      ) : null}
      {TEXT_TYPES.has(field.type) ? (
        <div className="mn-form-builder__pair">
          <Field>
            <FieldLabel>{m.minLength}</FieldLabel>
            <Input
              type="number"
              min={0}
              value={field.minLength ?? ''}
              onChange={(e) => set({ minLength: numberOrUndefined(e.target.value) })}
            />
          </Field>
          <Field>
            <FieldLabel>{m.maxLength}</FieldLabel>
            <Input
              type="number"
              min={1}
              value={field.maxLength ?? ''}
              onChange={(e) => set({ maxLength: numberOrUndefined(e.target.value) })}
            />
          </Field>
        </div>
      ) : null}
      {field.type === 'file' ? (
        <div className="mn-form-builder__pair">
          <Field>
            <FieldLabel>{m.accept}</FieldLabel>
            <Input
              value={field.accept ?? ''}
              onChange={(e) => set({ accept: e.target.value || undefined })}
            />
            <FieldDescription>{m.acceptHelp}</FieldDescription>
          </Field>
          <Field>
            <FieldLabel>{m.maxFiles}</FieldLabel>
            <Input
              type="number"
              min={1}
              max={20}
              value={field.maxFiles ?? 1}
              onChange={(e) => set({ maxFiles: numberOrUndefined(e.target.value) })}
            />
          </Field>
        </div>
      ) : null}

      <div role="group" aria-labelledby={`${id}-logic`} className="mn-form-builder__logic">
        <p id={`${id}-logic`} className="mn-form-builder__subheading">
          {m.logic}
        </p>
        <p className="mn-form-builder__note">{m.logicHelp}</p>
        <ConditionEditor
          value={field.conditions ?? []}
          onChange={(conditions) => set({ conditions: conditions.length ? conditions : undefined })}
          fields={conditionFields}
          match={field.conditionMatch ?? 'all'}
          onMatchChange={(match: ConditionMatch) => set({ conditionMatch: match })}
          showErrors={showErrors}
          maxConditions={10}
          disabled={readOnly}
        />
      </div>
    </fieldset>
  );
}
