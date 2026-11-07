'use client';

import { forwardRef, useId, useRef, type HTMLAttributes, type ReactNode } from 'react';
import { cx } from '../../../internal/cx.js';
import { CloseGlyph, PlusGlyph } from '../../../internal/icons.js';
import { Button, IconButton } from '../../actions/button/index.js';
import {
  Field,
  FieldError,
  FieldLabel,
  NativeSelect,
  type NativeSelectOption,
} from '../../forms/form-field/index.js';
import { Input } from '../../forms/text-field/index.js';
import { ReorderableList } from '../../interaction/reorderable-list/index.js';

/** A kind of approver a step can name, such as "Reporting manager" or "Anyone with a role". */
export interface ApprovalApproverType {
  /** Stable value stored on the step. */
  value: string;
  /** Visible name in the approver picker. */
  label: string;
  /**
   * When the type needs a choice (which role, which person), the options to
   * choose from. A step of this type is invalid until one is chosen.
   */
  targets?: readonly NativeSelectOption[] | undefined;
  /** Label of the target picker, e.g. "Role" or "Person". Default `messages.target`. */
  targetLabel?: string | undefined;
}

/** One step of a chain. */
export interface ApprovalChainStep {
  /** Stable, unique id. */
  id: string;
  /** The `value` of an approver type. */
  approver: string;
  /** The chosen target (role key, person id) for types with `targets`. */
  target?: string | undefined;
  /** Optional name shown in timelines, e.g. "Finance check". */
  label?: string | undefined;
}

/** Labels. Override them to translate. */
export interface ApprovalChainEditorMessages {
  /** Heading of the step list. */
  legend: string;
  /** Visible heading of each step. */
  step: (position: number) => string;
  approver: string;
  /** Default label of a target picker. */
  target: string;
  targetPlaceholder: string;
  label: string;
  labelPlaceholder: string;
  addStep: string;
  removeStep: (position: number) => string;
  /** Error under a target picker with nothing chosen. */
  targetRequired: string;
  /** Shown under the list when `maxSteps` is reached. */
  maxReached: (max: number) => string;
}

const DEFAULT_MESSAGES: ApprovalChainEditorMessages = {
  legend: 'Steps, in order',
  step: (n) => `Step ${n}`,
  approver: 'Approver',
  target: 'Choose',
  targetPlaceholder: 'Choose…',
  label: 'Label (optional)',
  labelPlaceholder: 'e.g. Finance check',
  addStep: 'Add step',
  removeStep: (n) => `Remove step ${n}`,
  targetRequired: 'Choose who decides this step.',
  maxReached: (max) => `A chain can have up to ${max} steps.`,
};

/** A problem with one step, keyed by step id. */
export type ApprovalChainErrors = Record<string, 'target'>;

/**
 * Checks a chain: every step whose approver type has `targets` needs one
 * chosen. Returns the steps with a problem, keyed by id; empty when valid.
 */
export function validateApprovalChain(
  steps: readonly ApprovalChainStep[],
  approverTypes: readonly ApprovalApproverType[],
): ApprovalChainErrors {
  const errors: ApprovalChainErrors = {};
  for (const step of steps) {
    const type = approverTypes.find((t) => t.value === step.approver);
    if (type?.targets && !type.targets.some((o) => o.value === step.target))
      errors[step.id] = 'target';
  }
  return errors;
}

/**
 * Plain-text summary of a chain, e.g. "Reporting manager → Finance (Role:
 * Finance team)". Useful for list rows and confirmation text.
 */
export function summarizeApprovalChain(
  steps: readonly ApprovalChainStep[],
  approverTypes: readonly ApprovalApproverType[],
  separator = ' → ',
): string {
  return steps.map((s) => stepSummary(s, approverTypes)).join(separator);
}

function stepSummary(step: ApprovalChainStep, approverTypes: readonly ApprovalApproverType[]) {
  const type = approverTypes.find((t) => t.value === step.approver);
  const target = type?.targets?.find((o) => o.value === step.target)?.label;
  const base = type ? (target ? `${type.label}: ${target}` : type.label) : step.approver;
  return step.label ? `${step.label} (${base})` : base;
}

export interface ApprovalChainEditorProps extends Omit<
  HTMLAttributes<HTMLFieldSetElement>,
  'onChange' | 'defaultValue'
> {
  /** The steps in order (controlled). */
  value: readonly ApprovalChainStep[];
  /** Called with the new steps after every edit, move, add or remove. */
  onChange: (steps: ApprovalChainStep[]) => void;
  /** The kinds of approver a step can name. The first is used for new steps. */
  approverTypes: readonly ApprovalApproverType[];
  /** Help text under the heading, e.g. how skipped steps work. */
  description?: ReactNode;
  /** Fewest steps; the remove button is disabled at this count. Default 1. */
  minSteps?: number | undefined;
  /** Most steps; the add button is hidden at this count. Default 10. */
  maxSteps?: number | undefined;
  /** Show the optional label input on each step. Default true. */
  showLabels?: boolean | undefined;
  /** Show "choose a target" errors (for example after a save attempt). Default false. */
  showErrors?: boolean | undefined;
  /** Makes a new step. Default: the first approver type with a generated id. */
  createStep?: (() => ApprovalChainStep) | undefined;
  disabled?: boolean | undefined;
  /** Override labels. */
  messages?: Partial<ApprovalChainEditorMessages> | undefined;
}

/**
 * Edits the ordered steps of an approval chain: who approves each step
 * (a kind of approver, plus a role or person where the kind needs one), an
 * optional label, and the order, with add, remove and accessible reordering.
 * The product owns the chain's other settings and saving.
 */
export const ApprovalChainEditor = forwardRef<HTMLFieldSetElement, ApprovalChainEditorProps>(
  function ApprovalChainEditor(
    {
      value,
      onChange,
      approverTypes,
      description,
      minSteps = 1,
      maxSteps = 10,
      showLabels = true,
      showErrors = false,
      createStep,
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
    const errors = showErrors ? validateApprovalChain(value, approverTypes) : {};

    const update = (id: string, patch: Partial<ApprovalChainStep>) =>
      onChange(value.map((s) => (s.id === id ? clean({ ...s, ...patch }, approverTypes) : s)));
    const remove = (id: string) => onChange(value.filter((s) => s.id !== id));
    const add = () => {
      counter.current += 1;
      const step = createStep?.() ?? {
        id: `${baseId}-new-${counter.current}`,
        approver: approverTypes[0]?.value ?? '',
      };
      onChange([...value, step]);
    };
    const position = (step: ApprovalChainStep) => value.findIndex((s) => s.id === step.id) + 1;

    return (
      <fieldset
        ref={ref}
        {...rest}
        disabled={disabled}
        className={cx('mn-approval-chain-editor', className)}
        data-disabled={disabled || undefined}
      >
        <legend className="mn-approval-chain-editor__legend">{m.legend}</legend>
        {description ? (
          <p className="mn-approval-chain-editor__description">{description}</p>
        ) : null}
        <ReorderableList
          className="mn-approval-chain-editor__steps"
          items={value}
          onReorder={(items) => onChange(items)}
          getKey={(s) => s.id}
          getLabel={(s) => `${m.step(position(s))}, ${stepSummary(s, approverTypes)}`}
          disabled={disabled}
          moveButtons
          renderItem={(step, { index }) => {
            const n = index + 1;
            const type = approverTypes.find((t) => t.value === step.approver);
            const titleId = `${baseId}-${step.id}-title`;
            const targetInvalid = errors[step.id] === 'target';
            return (
              <div
                role="group"
                aria-labelledby={titleId}
                className="mn-approval-chain-editor__step"
                data-invalid={targetInvalid || undefined}
              >
                <div className="mn-approval-chain-editor__step-header">
                  <span id={titleId} className="mn-approval-chain-editor__step-title">
                    {m.step(n)}
                  </span>
                  <IconButton
                    size="sm"
                    icon={<CloseGlyph />}
                    label={m.removeStep(n)}
                    disabled={disabled || value.length <= minSteps}
                    onClick={() => remove(step.id)}
                  />
                </div>
                <div className="mn-approval-chain-editor__fields">
                  <Field>
                    <FieldLabel>{m.approver}</FieldLabel>
                    <NativeSelect
                      value={step.approver}
                      options={approverTypes.map((t) => ({ value: t.value, label: t.label }))}
                      onChange={(e) => update(step.id, { approver: e.target.value })}
                    />
                  </Field>
                  {type?.targets ? (
                    <Field invalid={targetInvalid} required>
                      <FieldLabel>{type.targetLabel ?? m.target}</FieldLabel>
                      <NativeSelect
                        value={step.target ?? ''}
                        placeholder={m.targetPlaceholder}
                        options={type.targets}
                        onChange={(e) => update(step.id, { target: e.target.value || undefined })}
                      />
                      {targetInvalid ? <FieldError>{m.targetRequired}</FieldError> : null}
                    </Field>
                  ) : null}
                  {showLabels ? (
                    <Field>
                      <FieldLabel>{m.label}</FieldLabel>
                      <Input
                        value={step.label ?? ''}
                        maxLength={60}
                        placeholder={m.labelPlaceholder}
                        onChange={(e) => update(step.id, { label: e.target.value || undefined })}
                      />
                    </Field>
                  ) : null}
                </div>
              </div>
            );
          }}
        />
        {value.length < maxSteps ? (
          <Button
            size="sm"
            variant="secondary"
            leadingIcon={<PlusGlyph />}
            className="mn-approval-chain-editor__add"
            disabled={disabled}
            onClick={add}
          >
            {m.addStep}
          </Button>
        ) : (
          <p className="mn-approval-chain-editor__note">{m.maxReached(maxSteps)}</p>
        )}
      </fieldset>
    );
  },
);

/** Drops a target that the new approver type does not use. */
function clean(
  step: ApprovalChainStep,
  approverTypes: readonly ApprovalApproverType[],
): ApprovalChainStep {
  const type = approverTypes.find((t) => t.value === step.approver);
  const next: ApprovalChainStep = { id: step.id, approver: step.approver };
  if (type?.targets && step.target && type.targets.some((o) => o.value === step.target))
    next.target = step.target;
  if (step.label) next.label = step.label;
  return next;
}
