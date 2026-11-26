import { forwardRef, type HTMLAttributes, type ReactNode } from 'react';
import { cx } from '../../../internal/cx.js';
import { CheckGlyph } from '../../../internal/icons.js';
import { Progress } from '../../data-display/progress-bar/index.js';

/** The state a step is drawn in. */
export type StepperStepStatus = 'complete' | 'current' | 'upcoming' | 'error';

/** One step of a Stepper. */
export interface StepperStep {
  /** Stable identifier, passed back to `onStepSelect`. */
  id: string;
  /** Short name of the step, such as "Details". */
  label: ReactNode;
  /** Secondary text under the label. */
  description?: ReactNode;
  /** The step may be skipped: an "Optional" note is shown under the label. */
  optional?: boolean;
  /**
   * Overrides the status derived from `current`: `error` marks a step that
   * needs attention, `complete` a step finished out of order.
   */
  status?: 'complete' | 'error';
  /** The step cannot be selected even when it would otherwise be clickable. */
  disabled?: boolean;
}

/** Visible and announced strings. Override them to translate. */
export interface StepperMessages {
  /** The compact summary and the step number for assistive technology. */
  stepOf: (step: number, total: number) => string;
  /** Appended, visually hidden, to a finished step. */
  complete: string;
  /** Appended, visually hidden, to a step with an error. */
  error: string;
  /** Shown under the label of an optional step. */
  optional: string;
}

const DEFAULT_MESSAGES: StepperMessages = {
  stepOf: (step, total) => `Step ${step} of ${total}`,
  complete: 'completed',
  error: 'needs attention',
  optional: 'Optional',
};

export interface StepperProps extends Omit<HTMLAttributes<HTMLElement>, 'children'> {
  /** The steps, in order. */
  steps: StepperStep[];
  /** The current step: its `id` or its zero-based index. Steps before it are complete. */
  current: string | number;
  /**
   * The furthest step the user has reached (id or index), when they have
   * gone back. Steps up to it stay clickable. Default: the current step.
   */
  furthest?: string | number;
  /**
   * Makes reachable steps (complete ones and any up to `furthest`) buttons
   * that call this with the step and its index. Without it the stepper only
   * shows progress.
   */
  onStepSelect?: (step: StepperStep, index: number) => void;
  /** Default `horizontal`. A vertical stepper suits a sidebar or a long setup checklist. */
  orientation?: 'horizontal' | 'vertical';
  /**
   * When to show the compact "Step 2 of 5" summary with a progress bar instead of
   * the full list. `auto` (default) switches when a horizontal stepper's own width
   * is under 36rem, so it collapses at 320px and in narrow panels.
   */
  compact?: 'auto' | 'always' | 'never';
  /** Override visible and announced strings. */
  messages?: Partial<StepperMessages>;
}

function toIndex(steps: StepperStep[], ref: string | number | undefined, fallback: number) {
  if (ref === undefined) return fallback;
  if (typeof ref === 'number') return Math.max(0, Math.min(ref, steps.length - 1));
  const i = steps.findIndex((s) => s.id === ref);
  return i === -1 ? fallback : i;
}

/** Derives a step's status from its position and its own override. */
export function stepStatus(index: number, currentIndex: number, step: StepperStep) {
  if (step.status === 'error') return 'error' satisfies StepperStepStatus;
  if (index === currentIndex) return 'current' satisfies StepperStepStatus;
  if (step.status === 'complete' || index < currentIndex)
    return 'complete' satisfies StepperStepStatus;
  return 'upcoming' satisfies StepperStepStatus;
}

function ErrorMark() {
  return (
    <svg
      width="16"
      height="16"
      viewBox="0 0 16 16"
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      aria-hidden
      focusable={false}
    >
      <path d="M8 4v5M8 11.75v.01" />
    </svg>
  );
}

/**
 * Multi-step progress for wizards, onboarding and setup flows. An ordered
 * list inside a `nav` with `aria-current="step"` on the current step; finished
 * steps can be made clickable. Collapses to "Step 2 of 5" with a progress bar
 * when there is no room.
 */
export const Stepper = forwardRef<HTMLElement, StepperProps>(function Stepper(
  {
    steps,
    current,
    furthest,
    onStepSelect,
    orientation = 'horizontal',
    compact = 'auto',
    messages: messagesProp,
    className,
    'aria-label': ariaLabel = 'Progress',
    ...rest
  },
  ref,
) {
  const messages = { ...DEFAULT_MESSAGES, ...messagesProp };
  const total = steps.length;
  const currentIndex = toIndex(steps, current, 0);
  const furthestIndex = Math.max(currentIndex, toIndex(steps, furthest, currentIndex));
  const currentStep = steps[currentIndex];

  return (
    <nav
      ref={ref}
      {...rest}
      aria-label={ariaLabel}
      data-orientation={orientation}
      data-compact={compact}
      className={cx('mn-stepper', className)}
    >
      <ol className="mn-stepper__list">
        {steps.map((step, index) => {
          const status = stepStatus(index, currentIndex, step);
          const reachable = index <= furthestIndex || status === 'complete';
          const clickable =
            onStepSelect !== undefined && !step.disabled && index !== currentIndex && reachable;
          const hidden =
            status === 'complete'
              ? messages.complete
              : status === 'error'
                ? messages.error
                : undefined;
          const body = (
            <>
              <span className="mn-stepper__indicator" aria-hidden>
                {status === 'complete' ? (
                  <CheckGlyph />
                ) : status === 'error' ? (
                  <ErrorMark />
                ) : (
                  index + 1
                )}
              </span>
              <span className="mn-stepper__text">
                <span className="mn-visually-hidden">{messages.stepOf(index + 1, total)}: </span>
                <span className="mn-stepper__label">{step.label}</span>
                {hidden ? <span className="mn-visually-hidden"> ({hidden})</span> : null}
                {step.description ? (
                  <span className="mn-stepper__description">{step.description}</span>
                ) : null}
                {step.optional ? (
                  <span className="mn-stepper__optional">{messages.optional}</span>
                ) : null}
              </span>
            </>
          );
          return (
            <li
              key={step.id}
              className="mn-stepper__step"
              data-status={status}
              data-reached={index <= furthestIndex || undefined}
              aria-current={index === currentIndex ? 'step' : undefined}
            >
              {clickable ? (
                <button
                  type="button"
                  className="mn-stepper__item"
                  data-clickable=""
                  onClick={() => onStepSelect(step, index)}
                >
                  {body}
                </button>
              ) : (
                <span
                  className="mn-stepper__item"
                  data-disabled={step.disabled && onStepSelect ? '' : undefined}
                >
                  {body}
                </span>
              )}
            </li>
          );
        })}
      </ol>
      {total > 0 ? (
        <div className="mn-stepper__compact">
          <p className="mn-stepper__summary">
            <span className="mn-stepper__count">{messages.stepOf(currentIndex + 1, total)}</span>
            {currentStep ? (
              <span className="mn-stepper__summary-label">{currentStep.label}</span>
            ) : null}
          </p>
          <Progress
            size="sm"
            value={currentIndex + 1}
            max={total}
            aria-hidden
            className="mn-stepper__progress"
          />
        </div>
      ) : null}
    </nav>
  );
});
