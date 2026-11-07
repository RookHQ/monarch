'use client';

import {
  forwardRef,
  type ComponentType,
  useEffect,
  useRef,
  useState,
  type HTMLAttributes,
  type ReactNode,
  type SVGProps,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { ChevronRightGlyph, DashGlyph, ErrorGlyph, SuccessGlyph } from '../../../internal/icons.js';

/** Where a step is. `active` is the one running now. */
export type AiStepState = 'pending' | 'active' | 'done' | 'failed' | 'skipped';

/** One step the assistant takes. */
export interface AiStep {
  /** Stable id, used as the React key and to detect changes. */
  id: string;
  /** What the step does, e.g. "Read the attached contract". */
  label: string;
  /** Optional detail under the label: a result, a count, an error. */
  detail?: ReactNode;
  state: AiStepState;
}

/** Every visible and assistive-technology string. English defaults. */
export interface AiChainOfStepsMessages {
  /** Accessible name of the list. */
  label: string;
  /** State text, read after each step’s label and shown for failed and skipped steps. */
  states: Record<AiStepState, string>;
  /** Summary of the collapsed list once every step has finished, e.g. "4 steps". */
  summary: (count: number, failed: number) => string;
  /** Announced when a step starts. */
  started: (label: string, position: number, total: number) => string;
  /** Announced when a step fails. */
  failedStep: (label: string) => string;
  /** Announced once every step has finished. */
  finished: (count: number, failed: number) => string;
}

export const AI_CHAIN_OF_STEPS_MESSAGES: AiChainOfStepsMessages = {
  label: 'Steps',
  states: {
    pending: 'Not started',
    active: 'In progress',
    done: 'Done',
    failed: 'Failed',
    skipped: 'Skipped',
  },
  summary: (n, failed) => `${n} ${n === 1 ? 'step' : 'steps'}${failed ? `, ${failed} failed` : ''}`,
  started: (label, p, n) => `Step ${p} of ${n}: ${label}`,
  failedStep: (label) => `Step failed: ${label}`,
  finished: (n, failed) =>
    failed ? `Finished ${n} steps, ${failed} failed` : `Finished ${n} steps`,
};

export interface AiChainOfStepsProps extends Omit<HTMLAttributes<HTMLDivElement>, 'children'> {
  /** The steps, in order. Update their `state` as the assistant works. */
  steps: readonly AiStep[];
  /**
   * Once every step is done, failed or skipped, fold the list under a
   * "4 steps" summary the person can open. Default true.
   */
  collapsible?: boolean | undefined;
  /** Whether the finished list starts open. Default false. */
  defaultOpen?: boolean | undefined;
  /**
   * Announce step starts, failures and completion through a polite live
   * region. Turn it off when the surrounding thread announces progress
   * itself. Default true.
   */
  announce?: boolean | undefined;
  /** Replaces any of the English strings; `states` may be partial too. */
  messages?:
    | (Partial<Omit<AiChainOfStepsMessages, 'states'>> & {
        states?: Partial<Record<AiStepState, string>> | undefined;
      })
    | undefined;
}

function PendingGlyph(props: SVGProps<SVGSVGElement>) {
  return (
    <svg
      width={16}
      height={16}
      viewBox="0 0 16 16"
      fill="none"
      stroke="currentColor"
      strokeWidth={1.75}
      aria-hidden
      focusable={false}
      {...props}
    >
      <circle cx="8" cy="8" r="6.25" strokeDasharray="2.5 2.5" />
    </svg>
  );
}

function ActiveGlyph(props: SVGProps<SVGSVGElement>) {
  return (
    <svg
      width={16}
      height={16}
      viewBox="0 0 16 16"
      fill="none"
      stroke="currentColor"
      strokeWidth={1.75}
      aria-hidden
      focusable={false}
      {...props}
    >
      <circle cx="8" cy="8" r="6.25" />
      <circle cx="8" cy="8" r="2.75" fill="currentColor" stroke="none" />
    </svg>
  );
}

const ICONS: Record<AiStepState, ComponentType<SVGProps<SVGSVGElement>>> = {
  pending: PendingGlyph,
  active: ActiveGlyph,
  done: SuccessGlyph,
  failed: ErrorGlyph,
  skipped: DashGlyph,
};

const isFinished = (s: AiStepState) => s === 'done' || s === 'failed' || s === 'skipped';

/**
 * The steps an assistant is working through, as an ordered list that updates
 * live: each step shows its state with an icon and text, the running step has
 * a progress shimmer, and once all are finished the list folds under a
 * "4 steps" summary.
 */
export const AiChainOfSteps = forwardRef<HTMLDivElement, AiChainOfStepsProps>(
  function AiChainOfSteps(
    {
      steps,
      collapsible = true,
      defaultOpen = false,
      announce = true,
      messages: messagesProp,
      className,
      ...rest
    },
    ref,
  ) {
    const { states: _states, ...overrides } = messagesProp ?? {};
    const m = { ...AI_CHAIN_OF_STEPS_MESSAGES, ...overrides };
    const states = { ...AI_CHAIN_OF_STEPS_MESSAGES.states, ...messagesProp?.states };
    const total = steps.length;
    const failed = steps.filter((s) => s.state === 'failed').length;
    const allFinished = total > 0 && steps.every((s) => isFinished(s.state));
    const folded = collapsible && allFinished;
    const [open, setOpen] = useState(defaultOpen);

    // Announce transitions, never the initial state (history is not news).
    const previous = useRef<Map<string, AiStepState> | null>(null);
    const [live, setLive] = useState('');
    const signature = steps.map((s) => `${s.id}:${s.state}`).join('|');
    useEffect(() => {
      const before = previous.current;
      previous.current = new Map(steps.map((s) => [s.id, s.state]));
      if (!before || !announce) return;
      const lines: string[] = [];
      steps.forEach((s, i) => {
        const was = before.get(s.id);
        if (was === s.state) return;
        if (s.state === 'active') lines.push(m.started(s.label, i + 1, total));
        else if (s.state === 'failed') lines.push(m.failedStep(s.label));
      });
      const wasFinished =
        before.size === total && [...before.values()].every((st) => isFinished(st));
      if (allFinished && !wasFinished) lines.push(m.finished(total, failed));
      if (lines.length) setLive(lines.join('. '));
      // `signature` captures every change that matters.
      // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [signature, announce]);

    const list = (
      <ol className="mn-ai-chain-of-steps__list" aria-label={m.label}>
        {steps.map((step) => {
          const Icon = ICONS[step.state];
          const showState = step.state === 'failed' || step.state === 'skipped';
          return (
            <li
              key={step.id}
              className="mn-ai-chain-of-steps__step"
              data-state={step.state}
              aria-current={step.state === 'active' ? 'step' : undefined}
            >
              <span className="mn-ai-chain-of-steps__icon">
                <Icon />
              </span>
              <span className="mn-ai-chain-of-steps__text">
                <span className="mn-ai-chain-of-steps__label">{step.label}</span>
                {showState ? (
                  <span className="mn-ai-chain-of-steps__state">
                    <span className="mn-visually-hidden">, </span>
                    {states[step.state]}
                  </span>
                ) : (
                  <span className="mn-visually-hidden">, {states[step.state]}</span>
                )}
                {step.detail !== undefined && step.detail !== null ? (
                  <span className="mn-ai-chain-of-steps__detail">{step.detail}</span>
                ) : null}
              </span>
            </li>
          );
        })}
      </ol>
    );

    return (
      <div
        ref={ref}
        {...rest}
        className={cx('mn-ai-chain-of-steps', className)}
        data-finished={allFinished || undefined}
        data-failed={failed > 0 || undefined}
      >
        {folded ? (
          <details
            className="mn-ai-chain-of-steps__details"
            open={open}
            onToggle={(e) => {
              const next = e.currentTarget.open;
              if (next !== open) setOpen(next);
            }}
          >
            <summary className="mn-ai-chain-of-steps__summary">
              <ChevronRightGlyph className="mn-ai-chain-of-steps__chevron" />
              {failed ? <ErrorGlyph className="mn-ai-chain-of-steps__summary-icon" /> : null}
              <span>{m.summary(total, failed)}</span>
            </summary>
            {list}
          </details>
        ) : (
          list
        )}
        {announce ? (
          <span className="mn-visually-hidden" role="status" aria-live="polite" aria-atomic="true">
            {live}
          </span>
        ) : null}
      </div>
    );
  },
);
