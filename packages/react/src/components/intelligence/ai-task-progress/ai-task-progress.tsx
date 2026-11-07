'use client';

import {
  forwardRef,
  useEffect,
  useId,
  useRef,
  useState,
  type HTMLAttributes,
  type ReactNode,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { BellGlyph, ChevronDownGlyph } from '../../../internal/icons.js';
import { Button, IconButton } from '../../actions/button/index.js';
import { Progress } from '../../data-display/progress-bar/index.js';
import { RelativeTime, type DateInput } from '../../data-display/relative-time/index.js';
import {
  AiChainOfSteps,
  type AiChainOfStepsProps,
  type AiStep,
} from '../ai-chain-of-steps/index.js';

/** Where a long-running task is. */
export type AiTaskStatus = 'running' | 'paused' | 'done' | 'failed' | 'cancelled';

/** Every visible and assistive-technology string. English defaults. */
export interface AiTaskProgressMessages {
  /** Status text shown beside the title and read with it. */
  statuses: Record<AiTaskStatus, string>;
  /** Accessible name of the progress bar. */
  progressLabel: (title: string) => string;
  /** Text alternative of the bar, e.g. "40% complete". */
  progressValue: (percent: number) => string;
  /** Prefix before the start time, e.g. "Started". */
  started: string;
  /** Time-left text, from the estimate in milliseconds. */
  eta: (ms: number) => string;
  /** Cancel button. */
  cancel: string;
  /** Minimize button (icon only, so this is its accessible name). */
  minimize: string;
  /** Notify toggle (pressed while on). */
  notify: string;
  /** Announced when the status changes. */
  announce: (title: string, status: string) => string;
}

function roundMinutes(ms: number) {
  return Math.max(1, Math.round(ms / 60_000));
}

export const AI_TASK_PROGRESS_MESSAGES: AiTaskProgressMessages = {
  statuses: {
    running: 'Running',
    paused: 'Paused',
    done: 'Done',
    failed: 'Failed',
    cancelled: 'Cancelled',
  },
  progressLabel: (title) => `${title} progress`,
  progressValue: (p) => `${p}% complete`,
  started: 'Started',
  eta: (ms) => {
    if (ms < 60_000) return 'Less than a minute left';
    const min = roundMinutes(ms);
    if (min < 60) return `About ${min} ${min === 1 ? 'minute' : 'minutes'} left`;
    const h = Math.round(min / 6) / 10;
    return `About ${h} ${h === 1 ? 'hour' : 'hours'} left`;
  },
  cancel: 'Cancel',
  minimize: 'Minimize',
  notify: 'Notify me when done',
  announce: (title, status) => `${title}: ${status}`,
};

export interface AiTaskProgressProps extends Omit<HTMLAttributes<HTMLElement>, 'title'> {
  /** What the task is, e.g. "Summarising 40 documents". Also names the region. */
  title: string;
  /** The steps, in the `AiChainOfSteps` shape. Optional: a task may only have a bar. */
  steps?: readonly AiStep[] | undefined;
  /**
   * Completion from 0 to 1. Default: the share of finished steps; with no
   * steps and no value, the bar is indeterminate while running.
   */
  progress?: number | undefined;
  /** When the task started (Date, epoch milliseconds or ISO string). */
  startedAt: DateInput;
  /** Estimated time left in milliseconds, shown while running. */
  etaMs?: number | undefined;
  /** Where the task is. Changes are announced politely; `failed` assertively. */
  status: AiTaskStatus;
  /** Shows a Cancel button while running or paused. */
  onCancel?: (() => void) | undefined;
  /** Shows a Minimize icon button, e.g. to dock the task into a tray. */
  onMinimize?: (() => void) | undefined;
  /** Whether "Notify me when done" is on. Shown as a toggle when `onNotifyChange` is set. */
  notifyWhenDone?: boolean | undefined;
  /** Called with the new value when the person toggles "Notify me when done". */
  onNotifyChange?: ((next: boolean) => void) | undefined;
  /** Reference time for the start text; pass it for identical server rendering. */
  now?: DateInput | undefined;
  /** Props passed to the inner `AiChainOfSteps`, e.g. `collapsible`. */
  stepsProps?: Partial<Omit<AiChainOfStepsProps, 'steps'>> | undefined;
  /** Extra content under the steps, e.g. a link to the result. */
  children?: ReactNode;
  /** Replaces any of the English strings; `statuses` may be partial. */
  messages?:
    | (Partial<Omit<AiTaskProgressMessages, 'statuses'>> & {
        statuses?: Partial<Record<AiTaskStatus, string>> | undefined;
      })
    | undefined;
}

const finishedStep = (s: AiStep) =>
  s.state === 'done' || s.state === 'failed' || s.state === 'skipped';

const TONE = {
  running: 'accent',
  paused: 'warning',
  done: 'success',
  failed: 'danger',
  cancelled: 'neutral',
} as const;

/**
 * A long-running agent task: title and status, a progress bar, when it started
 * and how long is left, the steps it is working through, and controls to
 * cancel, minimize or be notified when it finishes. Status changes are
 * announced; the steps announce their own starts and failures.
 */
export const AiTaskProgress = forwardRef<HTMLElement, AiTaskProgressProps>(function AiTaskProgress(
  {
    title,
    steps,
    progress,
    startedAt,
    etaMs,
    status,
    onCancel,
    onMinimize,
    notifyWhenDone = false,
    onNotifyChange,
    now,
    stepsProps,
    children,
    messages: messagesProp,
    className,
    ...rest
  },
  ref,
) {
  const { statuses: _s, ...overrides } = messagesProp ?? {};
  const m = { ...AI_TASK_PROGRESS_MESSAGES, ...overrides };
  const statuses = { ...AI_TASK_PROGRESS_MESSAGES.statuses, ...messagesProp?.statuses };
  const titleId = useId();

  const derived =
    progress !== undefined
      ? progress
      : steps && steps.length
        ? steps.filter(finishedStep).length / steps.length
        : status === 'done'
          ? 1
          : undefined;
  const value = derived === undefined ? undefined : Math.min(1, Math.max(0, derived));
  const percent = value === undefined ? undefined : Math.round(value * 100);
  const active = status === 'running' || status === 'paused';

  // Announce status transitions, never the initial status.
  const previous = useRef<AiTaskStatus | null>(null);
  const [live, setLive] = useState<{ text: string; urgent: boolean }>({ text: '', urgent: false });
  useEffect(() => {
    const before = previous.current;
    previous.current = status;
    if (before === null || before === status) return;
    setLive({ text: m.announce(title, statuses[status]), urgent: status === 'failed' });
    // Only a status change is news.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [status]);

  return (
    <section
      ref={ref}
      aria-labelledby={titleId}
      {...rest}
      className={cx('mn-ai-task-progress', className)}
      data-status={status}
      aria-busy={status === 'running' || undefined}
    >
      <header className="mn-ai-task-progress__header">
        <div className="mn-ai-task-progress__heading">
          <h3 id={titleId} className="mn-ai-task-progress__title">
            {title}
          </h3>
          <span className="mn-ai-task-progress__status">{statuses[status]}</span>
        </div>
        {onMinimize ? (
          <IconButton
            size="sm"
            className="mn-ai-task-progress__minimize"
            label={m.minimize}
            icon={<ChevronDownGlyph />}
            onClick={onMinimize}
          />
        ) : null}
      </header>

      <Progress
        className="mn-ai-task-progress__bar"
        size="sm"
        label={m.progressLabel(title)}
        tone={TONE[status]}
        value={
          percent !== undefined
            ? percent
            : status === 'running'
              ? null
              : status === 'done'
                ? 100
                : 0
        }
        max={100}
        {...(percent !== undefined ? { valueText: m.progressValue(percent) } : {})}
      />

      <p className="mn-ai-task-progress__meta">
        <span>
          {m.started}{' '}
          <RelativeTime date={startedAt} tooltip={false} {...(now !== undefined ? { now } : {})} />
        </span>
        {percent !== undefined ? (
          <span className="mn-ai-task-progress__percent">{m.progressValue(percent)}</span>
        ) : null}
        {status === 'running' && etaMs !== undefined && etaMs >= 0 ? (
          <span className="mn-ai-task-progress__eta">{m.eta(etaMs)}</span>
        ) : null}
      </p>

      {steps && steps.length ? (
        <AiChainOfSteps className="mn-ai-task-progress__steps" steps={steps} {...stepsProps} />
      ) : null}
      {children}

      {active && (onCancel || onNotifyChange) ? (
        <div className="mn-ai-task-progress__actions">
          {onNotifyChange ? (
            <Button
              size="sm"
              variant="ghost"
              aria-pressed={notifyWhenDone}
              data-pressed={notifyWhenDone || undefined}
              className="mn-ai-task-progress__notify"
              leadingIcon={<BellGlyph />}
              onClick={() => onNotifyChange(!notifyWhenDone)}
            >
              {m.notify}
            </Button>
          ) : null}
          {onCancel ? (
            <Button size="sm" variant="secondary" onClick={onCancel}>
              {m.cancel}
            </Button>
          ) : null}
        </div>
      ) : null}

      <span className="mn-visually-hidden" role="status" aria-live="polite" aria-atomic="true">
        {live.urgent ? '' : live.text}
      </span>
      <span className="mn-visually-hidden" role="alert" aria-atomic="true">
        {live.urgent ? live.text : ''}
      </span>
    </section>
  );
});
