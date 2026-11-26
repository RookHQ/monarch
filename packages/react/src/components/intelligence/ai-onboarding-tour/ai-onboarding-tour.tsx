'use client';

import {
  forwardRef,
  useEffect,
  useId,
  useRef,
  useState,
  type HTMLAttributes,
  type KeyboardEvent as ReactKeyboardEvent,
  type ReactNode,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { BotGlyph, CloseGlyph } from '../../../internal/icons.js';
import { Button, IconButton } from '../../actions/button/index.js';

/** Every string AiOnboardingTour shows or announces. English defaults. */
export interface AiOnboardingTourMessages {
  /** Small label above the step title. Default "AI". */
  eyebrow: string;
  /** Visible step counter. */
  stepOf: (step: number, total: number) => string;
  back: string;
  next: string;
  /** The last step's primary button. */
  finish: string;
  /** The close button's accessible name; closing skips the tour. */
  skip: string;
  /** The explicit opt-out, shown on every step. */
  disable: string;
}

const DEFAULT_MESSAGES: AiOnboardingTourMessages = {
  eyebrow: 'AI',
  stepOf: (step, total) => `Step ${step} of ${total}`,
  back: 'Back',
  next: 'Next',
  finish: 'Got it',
  skip: 'Skip introduction',
  disable: 'Turn off AI',
};

/** One step of the tour. */
export interface AiOnboardingTourStep {
  /** Stable key. Default: the step's index. */
  id?: string | undefined;
  /** Short heading, e.g. "What the assistant can see". */
  title: ReactNode;
  /** One or two sentences, or a short list. */
  body: ReactNode;
  /**
   * The element this step talks about: a CSS selector or a function that
   * returns the element. It is scrolled into view and outlined (it gets
   * `data-mn-ai-tour-target`) while the step is shown.
   */
  target?: string | (() => Element | null) | undefined;
}

export interface AiOnboardingTourProps extends Omit<HTMLAttributes<HTMLElement>, 'title'> {
  /**
   * The steps. Cover what the AI sees, what it can do and how to turn it
   * off; the "Turn off AI" action is on every step regardless.
   */
  steps: readonly AiOnboardingTourStep[];
  /** Called by the last step's button. */
  onFinish: () => void;
  /** Called by "Turn off AI". The product turns AI off (or opens its settings) and hides the tour. */
  onDisable: () => void;
  /** Called by the close button and by Escape. Default: the close button is hidden. */
  onSkip?: (() => void) | undefined;
  /** Current step index (controlled). */
  step?: number | undefined;
  /** First step shown when uncontrolled. Default 0. */
  defaultStep?: number | undefined;
  /** Called with the new index by Back and Next. */
  onStepChange?: ((step: number) => void) | undefined;
  /**
   * `inline` (default) renders where it is placed, e.g. at the top of a page.
   * `floating` docks to the bottom corner of the viewport as a non-modal dialog.
   */
  placement?: 'inline' | 'floating' | undefined;
  /** Replaces the Bot glyph beside the eyebrow. */
  icon?: ReactNode | undefined;
  /** Strings, for localisation. */
  messages?: Partial<AiOnboardingTourMessages> | undefined;
}

function resolveTarget(target: AiOnboardingTourStep['target']): Element | null {
  if (!target || typeof document === 'undefined') return null;
  if (typeof target === 'function') return target();
  try {
    return document.querySelector(target);
  } catch {
    return null;
  }
}

/**
 * A short, step-by-step introduction to AI in a product: what it can see,
 * what it can do and how to turn it off. Every step offers an explicit
 * "Turn off AI" action next to Back and Next, and a step can point at the
 * part of the page it describes.
 */
export const AiOnboardingTour = forwardRef<HTMLElement, AiOnboardingTourProps>(
  function AiOnboardingTour(
    {
      steps,
      onFinish,
      onDisable,
      onSkip,
      step: stepProp,
      defaultStep = 0,
      onStepChange,
      placement = 'inline',
      icon,
      messages: messagesProp,
      className,
      onKeyDown,
      ...rest
    },
    ref,
  ) {
    const m = { ...DEFAULT_MESSAGES, ...messagesProp };
    const auto = useId().replace(/:/g, '');
    const titleId = `mn-ai-tour-${auto}-title`;
    const bodyId = `mn-ai-tour-${auto}-body`;
    const headingRef = useRef<HTMLHeadingElement | null>(null);

    const [inner, setInner] = useState(defaultStep);
    const total = steps.length;
    const index = Math.min(Math.max(stepProp ?? inner, 0), Math.max(total - 1, 0));
    const current = steps[index];
    const last = index === total - 1;

    // Focus the new step's heading after Back / Next, never on first render.
    const moved = useRef(false);
    const go = (next: number) => {
      moved.current = true;
      if (stepProp === undefined) setInner(next);
      onStepChange?.(next);
    };
    useEffect(() => {
      if (!moved.current) return;
      moved.current = false;
      headingRef.current?.focus();
    }, [index]);

    // Point at the step's target.
    const target = current?.target;
    useEffect(() => {
      const el = resolveTarget(target);
      if (!el) return;
      el.setAttribute('data-mn-ai-tour-target', '');
      if (typeof (el as HTMLElement).scrollIntoView === 'function') {
        (el as HTMLElement).scrollIntoView({ block: 'nearest', inline: 'nearest' });
      }
      return () => el.removeAttribute('data-mn-ai-tour-target');
    }, [target]);

    if (!current) return null;

    const handleKeyDown = (e: ReactKeyboardEvent<HTMLElement>) => {
      onKeyDown?.(e);
      if (e.defaultPrevented) return;
      if (e.key === 'Escape' && onSkip) {
        e.preventDefault();
        onSkip();
      }
    };

    const floating = placement === 'floating';

    return (
      // Escape skips the tour from anywhere inside it.
      // eslint-disable-next-line jsx-a11y/no-noninteractive-element-interactions
      <section
        ref={ref}
        role={floating ? 'dialog' : undefined}
        aria-labelledby={titleId}
        aria-describedby={bodyId}
        {...rest}
        className={cx('mn-ai-onboarding-tour', className)}
        data-placement={placement}
        data-step={index}
        data-last={last || undefined}
        onKeyDown={handleKeyDown}
      >
        <div className="mn-ai-onboarding-tour__top">
          <span className="mn-ai-onboarding-tour__eyebrow">
            <span className="mn-ai-onboarding-tour__icon" aria-hidden>
              {icon ?? <BotGlyph />}
            </span>
            {m.eyebrow}
          </span>
          <span className="mn-ai-onboarding-tour__count">{m.stepOf(index + 1, total)}</span>
          {onSkip ? (
            <IconButton
              size="sm"
              variant="ghost"
              label={m.skip}
              icon={<CloseGlyph />}
              className="mn-ai-onboarding-tour__skip"
              onClick={onSkip}
            />
          ) : null}
        </div>
        <h2 ref={headingRef} id={titleId} tabIndex={-1} className="mn-ai-onboarding-tour__title">
          {current.title}
        </h2>
        <div id={bodyId} className="mn-ai-onboarding-tour__body">
          {current.body}
        </div>
        {total > 1 ? (
          <ol className="mn-ai-onboarding-tour__dots" aria-hidden>
            {steps.map((s, i) => (
              <li
                key={s.id ?? i}
                className="mn-ai-onboarding-tour__dot"
                data-current={i === index || undefined}
                data-done={i < index || undefined}
              />
            ))}
          </ol>
        ) : null}
        <div className="mn-ai-onboarding-tour__footer">
          <Button
            size="sm"
            variant="ghost"
            className="mn-ai-onboarding-tour__disable"
            onClick={onDisable}
          >
            {m.disable}
          </Button>
          <div className="mn-ai-onboarding-tour__nav">
            {index > 0 ? (
              <Button size="sm" onClick={() => go(index - 1)}>
                {m.back}
              </Button>
            ) : null}
            <Button size="sm" variant="primary" onClick={() => (last ? onFinish() : go(index + 1))}>
              {last ? m.finish : m.next}
            </Button>
          </div>
        </div>
      </section>
    );
  },
);
