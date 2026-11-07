'use client';

import {
  forwardRef,
  useId,
  useRef,
  useState,
  type FormEvent,
  type FormHTMLAttributes,
  type ReactNode,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { Button } from '../../../components/actions/button/index.js';
import { Alert } from '../../../components/feedback/alert/index.js';
import { Checkbox } from '../../../components/forms/checkbox/index.js';
import {
  Field,
  FieldError,
  FieldLabel,
  NativeSelect,
} from '../../../components/forms/form-field/index.js';
import { Input } from '../../../components/forms/text-field/index.js';
import {
  ActionEditor,
  validateActions,
  type ActionEditorMessages,
  type ActionTypeDefinition,
  type RuleAction,
} from '../../../components/workflow/action-editor/index.js';
import {
  ConditionEditor,
  validateConditions,
  type Condition,
  type ConditionEditorProps,
  type ConditionField,
  type ConditionMatch,
} from '../../../components/workflow/condition-editor/index.js';

/** An event that runs a rule, such as "Ticket created". */
export interface RuleTrigger {
  value: string;
  label: string;
  /** One line under the label. */
  description?: string | undefined;
}

/** A rule being edited. */
export interface RuleDraft {
  name: string;
  description?: string | undefined;
  /** Whether the rule runs. Disabled rules are kept but never run. */
  enabled: boolean;
  /** The trigger values that run the rule. */
  triggers: readonly string[];
  match: ConditionMatch;
  conditions: readonly Condition[];
  actions: readonly RuleAction[];
  /** Skip later rules for the same event when this one matches. */
  stopProcessing?: boolean | undefined;
}

/** What is wrong with a rule, by part. Empty when the rule can be saved. */
export interface RuleProblems {
  name?: true;
  triggers?: true;
  actions?: true;
  /** Ids of incomplete conditions. */
  conditions?: string[];
  /** Ids of incomplete actions. */
  incompleteActions?: string[];
}

/** Labels. Override them to translate. */
export interface RuleBuilderMessages {
  name: string;
  description: string;
  when: string;
  whenHelp: string;
  trigger: string;
  if: string;
  ifHelp: string;
  then: string;
  thenHelp: string;
  options: string;
  enabled: string;
  enabledHelp: string;
  stopProcessing: string;
  stopProcessingHelp: string;
  submit: string;
  cancel: string;
  /** Title of the summary shown when saving is blocked. */
  notSaved: string;
  nameRequired: string;
  triggerRequired: string;
  actionRequired: string;
  conditionsIncomplete: string;
  actionsIncomplete: string;
  /** Shown when `onSubmit` rejects without an Error message. */
  submitFailed: string;
}

const DEFAULT_MESSAGES: RuleBuilderMessages = {
  name: 'Name',
  description: 'Description (optional)',
  when: 'When',
  whenHelp: 'The rule is checked whenever any of these happen.',
  trigger: 'Trigger',
  if: 'If',
  ifHelp: 'Leave empty to apply every time the rule is checked.',
  then: 'Then',
  thenHelp: 'Actions run in this order.',
  options: 'Options',
  enabled: 'Rule is on',
  enabledHelp: 'Rules that are off are kept but never run.',
  stopProcessing: 'Stop processing',
  stopProcessingHelp: 'When this rule matches, later rules are skipped for the same event.',
  submit: 'Save rule',
  cancel: 'Cancel',
  notSaved: 'Not saved yet',
  nameRequired: 'Give the rule a name.',
  triggerRequired: 'Pick at least one trigger.',
  actionRequired: 'Add at least one action.',
  conditionsIncomplete: 'Finish or remove the incomplete conditions.',
  actionsIncomplete: 'Finish or remove the incomplete actions.',
  submitFailed: 'The rule could not be saved. Try again.',
};

/** A new, empty rule. */
export function createRuleDraft(overrides: Partial<RuleDraft> = {}): RuleDraft {
  return {
    name: '',
    enabled: true,
    triggers: [],
    match: 'all',
    conditions: [],
    actions: [],
    ...overrides,
  };
}

/** Checks a rule against its fields and action types. */
export function validateRule(
  rule: RuleDraft,
  options: {
    fields: readonly ConditionField[];
    actionTypes: readonly ActionTypeDefinition[];
    /** Require a trigger. Default true. */
    requireTrigger?: boolean | undefined;
    /** Require an action. Default true. */
    requireAction?: boolean | undefined;
  },
): RuleProblems {
  const out: RuleProblems = {};
  if (!rule.name.trim()) out.name = true;
  if ((options.requireTrigger ?? true) && rule.triggers.length === 0) out.triggers = true;
  if ((options.requireAction ?? true) && rule.actions.length === 0) out.actions = true;
  const conditions = Object.keys(validateConditions(rule.conditions, options.fields));
  if (conditions.length) out.conditions = conditions;
  const actions = Object.keys(validateActions(rule.actions, options.actionTypes));
  if (actions.length) out.incompleteActions = actions;
  return out;
}

export interface RuleBuilderProps extends Omit<
  FormHTMLAttributes<HTMLFormElement>,
  'onChange' | 'onSubmit' | 'defaultValue'
> {
  /** The rule being edited (controlled). Start a new one with `createRuleDraft()`. */
  value: RuleDraft;
  onChange: (rule: RuleDraft) => void;
  /** Events that can run the rule. Omit for rules without triggers (for example form logic). */
  triggers?: readonly RuleTrigger[] | undefined;
  /** `multiple` (default): checkboxes, any of them runs the rule. `single`: one picker. */
  triggerMode?: 'multiple' | 'single' | undefined;
  /** Fields conditions can test. */
  fields: readonly ConditionField[];
  /** Kinds of action available. */
  actionTypes: readonly ActionTypeDefinition[];
  /** Saves the rule. Called only when it is valid. Return a promise to show a pending state; a rejection shows its message. */
  onSubmit?: ((rule: RuleDraft) => unknown) | undefined;
  /** Adds a Cancel button. */
  onCancel?: (() => void) | undefined;
  /** Show the description input. Default true. */
  showDescription?: boolean | undefined;
  /** Show the "Stop processing" option. Default false. */
  showStopProcessing?: boolean | undefined;
  /** Require at least one action. Default true. */
  requireAction?: boolean | undefined;
  /** Extra settings shown in the options section, such as a priority or a schedule. */
  children?: ReactNode;
  /** Passed to the condition editor, e.g. `maxConditions`. */
  conditionEditorProps?:
    Partial<Omit<ConditionEditorProps, 'value' | 'onChange' | 'fields'>> | undefined;
  /** Override labels of the action editor. */
  actionMessages?: Partial<ActionEditorMessages> | undefined;
  /** Heading level of the section titles. Default 3. */
  headingLevel?: 2 | 3 | 4 | 5 | 6 | undefined;
  /** Override labels. */
  messages?: Partial<RuleBuilderMessages> | undefined;
}

/**
 * A complete "when, if, then" rule form: name, triggers, conditions (with all
 * or any), ordered actions, on / off and stop-processing options, validation
 * with a summary, and a save action. Composes ConditionEditor and
 * ActionEditor; the product supplies the vocabulary and saves the rule.
 */
export const RuleBuilder = forwardRef<HTMLFormElement, RuleBuilderProps>(function RuleBuilder(
  {
    value: rule,
    onChange,
    triggers,
    triggerMode = 'multiple',
    fields,
    actionTypes,
    onSubmit,
    onCancel,
    showDescription = true,
    showStopProcessing = false,
    requireAction = true,
    children,
    conditionEditorProps,
    actionMessages,
    headingLevel = 3,
    messages: messagesProp,
    className,
    ...rest
  },
  ref,
) {
  const m = { ...DEFAULT_MESSAGES, ...messagesProp };
  const id = useId();
  const Heading = `h${headingLevel}` as 'h3';
  const [submitted, setSubmitted] = useState(false);
  const [pending, setPending] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const summaryRef = useRef<HTMLDivElement | null>(null);
  const set = (patch: Partial<RuleDraft>) => onChange({ ...rule, ...patch });

  const problems = validateRule(rule, {
    fields,
    actionTypes,
    requireTrigger: !!triggers?.length,
    requireAction,
  });
  const messages = [
    problems.name && m.nameRequired,
    problems.triggers && m.triggerRequired,
    problems.actions && m.actionRequired,
    problems.conditions && m.conditionsIncomplete,
    problems.incompleteActions && m.actionsIncomplete,
  ].filter((x): x is string => typeof x === 'string');
  const show = submitted && messages.length > 0;

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSubmitted(true);
    setError(null);
    if (messages.length > 0) {
      // Wait for the summary to render, then move focus to it.
      requestAnimationFrame(() => summaryRef.current?.focus());
      return;
    }
    if (!onSubmit) return;
    setPending(true);
    try {
      await onSubmit(rule);
    } catch (e) {
      setError(e instanceof Error && e.message ? e.message : m.submitFailed);
    } finally {
      setPending(false);
    }
  }

  const section = (key: string, title: string, help: string | null, body: ReactNode) => (
    <section
      className="mn-rule-builder__section"
      aria-labelledby={`${id}-${key}`}
      data-section={key}
    >
      <div className="mn-rule-builder__section-head">
        <Heading id={`${id}-${key}`} className="mn-rule-builder__heading">
          {title}
        </Heading>
        {help ? <p className="mn-rule-builder__help">{help}</p> : null}
      </div>
      {body}
    </section>
  );

  return (
    <form
      ref={ref}
      noValidate
      {...rest}
      className={cx('mn-rule-builder', className)}
      data-invalid={show || undefined}
      aria-busy={pending || undefined}
      onSubmit={(e) => void submit(e)}
    >
      <div className="mn-rule-builder__basics">
        <Field invalid={submitted && !!problems.name} required>
          <FieldLabel>{m.name}</FieldLabel>
          <Input
            value={rule.name}
            maxLength={120}
            onChange={(e) => set({ name: e.target.value })}
          />
          {submitted && problems.name ? <FieldError>{m.nameRequired}</FieldError> : null}
        </Field>
        {showDescription ? (
          <Field>
            <FieldLabel>{m.description}</FieldLabel>
            <Input
              value={rule.description ?? ''}
              maxLength={500}
              onChange={(e) => set({ description: e.target.value || undefined })}
            />
          </Field>
        ) : null}
      </div>

      {triggers?.length
        ? section(
            'when',
            m.when,
            triggerMode === 'multiple' ? m.whenHelp : null,
            triggerMode === 'single' ? (
              <Field invalid={submitted && !!problems.triggers} required>
                <FieldLabel>{m.trigger}</FieldLabel>
                <NativeSelect
                  value={rule.triggers[0] ?? ''}
                  placeholder={m.trigger}
                  options={triggers.map((t) => ({ value: t.value, label: t.label }))}
                  onChange={(e) => set({ triggers: e.target.value ? [e.target.value] : [] })}
                />
                {submitted && problems.triggers ? (
                  <FieldError>{m.triggerRequired}</FieldError>
                ) : null}
              </Field>
            ) : (
              <div
                role="group"
                aria-labelledby={`${id}-when`}
                aria-describedby={submitted && problems.triggers ? `${id}-when-error` : undefined}
                className="mn-rule-builder__triggers"
              >
                {triggers.map((t) => (
                  <Checkbox
                    key={t.value}
                    label={t.label}
                    description={t.description}
                    checked={rule.triggers.includes(t.value)}
                    onCheckedChange={(on) =>
                      set({
                        triggers:
                          on === true
                            ? [...rule.triggers, t.value]
                            : rule.triggers.filter((x) => x !== t.value),
                      })
                    }
                  />
                ))}
                {submitted && problems.triggers ? (
                  <p id={`${id}-when-error`} className="mn-rule-builder__error">
                    {m.triggerRequired}
                  </p>
                ) : null}
              </div>
            ),
          )
        : null}

      {section(
        'if',
        m.if,
        m.ifHelp,
        <ConditionEditor
          {...conditionEditorProps}
          value={rule.conditions}
          onChange={(conditions) => set({ conditions })}
          fields={fields}
          match={rule.match}
          onMatchChange={(match) => set({ match })}
          showErrors={submitted}
        />,
      )}

      {section(
        'then',
        m.then,
        m.thenHelp,
        <>
          <ActionEditor
            value={rule.actions}
            onChange={(actions) => set({ actions })}
            actionTypes={actionTypes}
            showErrors={submitted}
            {...(actionMessages ? { messages: actionMessages } : {})}
          />
          {submitted && problems.actions ? (
            <p className="mn-rule-builder__error">{m.actionRequired}</p>
          ) : null}
        </>,
      )}

      {section(
        'options',
        m.options,
        null,
        <div className="mn-rule-builder__options">
          <Checkbox
            label={m.enabled}
            description={m.enabledHelp}
            checked={rule.enabled}
            onCheckedChange={(on) => set({ enabled: on === true })}
          />
          {showStopProcessing ? (
            <Checkbox
              label={m.stopProcessing}
              description={m.stopProcessingHelp}
              checked={!!rule.stopProcessing}
              onCheckedChange={(on) => set({ stopProcessing: on === true })}
            />
          ) : null}
          {children}
        </div>,
      )}

      {show ? (
        <div ref={summaryRef} tabIndex={-1} className="mn-rule-builder__summary">
          <Alert tone="danger" title={m.notSaved}>
            <ul className="mn-rule-builder__problems">
              {messages.map((p) => (
                <li key={p}>{p}</li>
              ))}
            </ul>
          </Alert>
        </div>
      ) : null}
      {error ? (
        <Alert tone="danger" live="assertive">
          {error}
        </Alert>
      ) : null}

      {onSubmit || onCancel ? (
        <div className="mn-rule-builder__footer">
          {onCancel ? (
            <Button variant="secondary" onClick={onCancel} disabled={pending}>
              {m.cancel}
            </Button>
          ) : null}
          {onSubmit ? (
            <Button type="submit" variant="primary" loading={pending}>
              {m.submit}
            </Button>
          ) : null}
        </div>
      ) : null}
    </form>
  );
});
