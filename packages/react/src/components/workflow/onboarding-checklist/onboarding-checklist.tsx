'use client';

import { forwardRef, useId, useState, type HTMLAttributes, type ReactNode } from 'react';
import { cx } from '../../../internal/cx.js';
import { CheckGlyph, ChevronDownGlyph, CloseGlyph } from '../../../internal/icons.js';
import { IconButton } from '../../actions/button/index.js';
import { Progress } from '../../data-display/progress-bar/index.js';

/** One step of the checklist. */
export interface OnboardingStep {
  /** Stable, unique id. */
  id: string;
  title: ReactNode;
  /** One line on why or how. Shown while the step is not done. */
  description?: ReactNode;
  /** Whether the step is complete. The product decides, often from app data. */
  done: boolean;
  /** Call to action, such as a Button or a link. Shown while the step is not done. */
  action?: ReactNode;
}

/** Labels. Override them to translate. */
export interface OnboardingChecklistMessages {
  /** Visible progress text, e.g. "2 of 5 done". */
  progress: (done: number, total: number) => string;
  /** Accessible name of the progress bar. */
  progressLabel: string;
  /** Accessible name of the dismiss button. */
  dismiss: string;
  /** Hidden text after a step's title. */
  doneText: string;
  notDoneText: string;
  /** Shown when every step is done. */
  complete: string;
}

const DEFAULT_MESSAGES: OnboardingChecklistMessages = {
  progress: (d, t) => `${d} of ${t} done`,
  progressLabel: 'Setup progress',
  dismiss: 'Dismiss checklist',
  doneText: 'Done',
  notDoneText: 'Not done',
  complete: 'All done. You are set up.',
};

export interface OnboardingChecklistProps extends Omit<HTMLAttributes<HTMLElement>, 'title'> {
  /** Heading of the checklist, e.g. "Get started". */
  title: ReactNode;
  /** Text under the heading. */
  description?: ReactNode;
  steps: readonly OnboardingStep[];
  /** Show a toggle that hides the steps. Default true. */
  collapsible?: boolean;
  /** Steps hidden (controlled). The product persists it. */
  collapsed?: boolean;
  /** Steps hidden at first render (uncontrolled). */
  defaultCollapsed?: boolean;
  onCollapsedChange?: (collapsed: boolean) => void;
  /** Called by the dismiss button, which shows only when this is set. The product persists it. */
  onDismiss?: () => void;
  /** Shown when every step is done. Default `messages.complete`. */
  completeContent?: ReactNode;
  /** Heading level of the title. Default 2. */
  headingLevel?: 2 | 3 | 4 | 5 | 6;
  /** Override labels. */
  messages?: Partial<OnboardingChecklistMessages>;
}

/**
 * A first-run checklist: steps with a done state and an optional call to
 * action, a progress summary, and collapse and dismiss controls. The product
 * defines the steps, decides when each is done and persists collapse and dismiss.
 */
export const OnboardingChecklist = forwardRef<HTMLElement, OnboardingChecklistProps>(
  function OnboardingChecklist(
    {
      title,
      description,
      steps,
      collapsible = true,
      collapsed: collapsedProp,
      defaultCollapsed = false,
      onCollapsedChange,
      onDismiss,
      completeContent,
      headingLevel = 2,
      messages: messagesProp,
      className,
      ...rest
    },
    ref,
  ) {
    const m = { ...DEFAULT_MESSAGES, ...messagesProp };
    const Heading = `h${headingLevel}` as 'h2';
    const id = useId();
    const titleId = `${id}-title`;
    const bodyId = `${id}-body`;
    const [collapsedInternal, setCollapsedInternal] = useState(defaultCollapsed);
    const collapsed = collapsible && (collapsedProp ?? collapsedInternal);
    const done = steps.filter((s) => s.done).length;
    const total = steps.length;
    const complete = total > 0 && done === total;
    const current = steps.findIndex((s) => !s.done);

    const toggle = () => {
      const next = !collapsed;
      if (collapsedProp === undefined) setCollapsedInternal(next);
      onCollapsedChange?.(next);
    };

    return (
      <section
        ref={ref}
        aria-labelledby={titleId}
        {...rest}
        className={cx('mn-onboarding-checklist', className)}
        data-complete={complete || undefined}
        data-collapsed={collapsed || undefined}
      >
        <header className="mn-onboarding-checklist__header">
          <div className="mn-onboarding-checklist__heading">
            <Heading id={titleId} className="mn-onboarding-checklist__title">
              {title}
            </Heading>
            {description ? (
              <p className="mn-onboarding-checklist__description">{description}</p>
            ) : null}
          </div>
          <div className="mn-onboarding-checklist__controls">
            {collapsible ? (
              <button
                type="button"
                className="mn-onboarding-checklist__toggle"
                aria-expanded={!collapsed}
                aria-controls={bodyId}
                aria-labelledby={titleId}
                onClick={toggle}
              >
                <ChevronDownGlyph />
              </button>
            ) : null}
            {onDismiss ? (
              <IconButton size="sm" icon={<CloseGlyph />} label={m.dismiss} onClick={onDismiss} />
            ) : null}
          </div>
          <div className="mn-onboarding-checklist__progress">
            <Progress
              size="sm"
              value={done}
              max={Math.max(total, 1)}
              tone={complete ? 'success' : 'accent'}
              label={m.progressLabel}
              valueText={m.progress(done, total)}
            />
            <span className="mn-onboarding-checklist__count" aria-hidden>
              {m.progress(done, total)}
            </span>
          </div>
        </header>
        <div id={bodyId} className="mn-onboarding-checklist__body" hidden={collapsed}>
          {complete ? (
            <p className="mn-onboarding-checklist__complete">
              <CheckGlyph />
              <span>{completeContent ?? m.complete}</span>
            </p>
          ) : null}
          <ol className="mn-onboarding-checklist__steps">
            {steps.map((step, index) => (
              <li
                key={step.id}
                className="mn-onboarding-checklist__step"
                data-done={step.done || undefined}
                data-current={index === current || undefined}
                aria-current={index === current ? 'step' : undefined}
              >
                <span className="mn-onboarding-checklist__marker" aria-hidden>
                  {step.done ? <CheckGlyph /> : null}
                </span>
                <div className="mn-onboarding-checklist__text">
                  <p className="mn-onboarding-checklist__step-title">
                    {step.title}
                    <span className="mn-visually-hidden">
                      {`, ${step.done ? m.doneText : m.notDoneText}`}
                    </span>
                  </p>
                  {!step.done && step.description ? (
                    <p className="mn-onboarding-checklist__step-description">{step.description}</p>
                  ) : null}
                </div>
                {!step.done && step.action ? (
                  <div className="mn-onboarding-checklist__action">{step.action}</div>
                ) : null}
              </li>
            ))}
          </ol>
        </div>
      </section>
    );
  },
);
