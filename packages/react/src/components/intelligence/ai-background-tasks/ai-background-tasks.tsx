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
import {
  BellGlyph,
  BotGlyph,
  ClockGlyph,
  CloseGlyph,
  DashGlyph,
  ErrorGlyph,
  SuccessGlyph,
} from '../../../internal/icons.js';
import { useControllable } from '../../../internal/use-controllable.js';
import { Button, IconButton } from '../../actions/button/index.js';
import { Badge } from '../../data-display/badge/index.js';
import { Progress, type ProgressTone } from '../../data-display/progress-bar/index.js';
import { formatRelativeTime, type DateInput } from '../../data-display/relative-time/index.js';
import { Spinner } from '../../feedback/spinner/index.js';
import { Popover, PopoverContent, PopoverTrigger } from '../../overlays/popover/index.js';

export {
  useAiRunNotifications,
  type AiNotificationDelivery,
  type AiNotificationPermission,
  type AiRunNotification,
  type UseAiRunNotificationsOptions,
  type UseAiRunNotificationsResult,
} from './ai-background-tasks.notifications.js';
import type { AiRunNotification } from './ai-background-tasks.notifications.js';

/** Where a background task is. */
export type AiBackgroundTaskStatus =
  'queued' | 'running' | 'needs-input' | 'succeeded' | 'failed' | 'cancelled';

/** The three sections of the tray, in display order. */
export type AiBackgroundTaskGroup = 'needs-input' | 'running' | 'finished';

/** One piece of work an agent is doing in the background. */
export interface AiBackgroundTask {
  /** Stable id; used as the React key and to tell status changes apart. */
  id: string;
  /** What the task is, e.g. "Summarise 40 contracts". */
  title: string;
  /** Name of the agent doing the work, e.g. "Research agent". */
  agent?: string | undefined;
  /** Where the task is. */
  status: AiBackgroundTaskStatus;
  /** Completion from 0 to 1, when the product can measure it. */
  progress?: number | undefined;
  /** Forces the animated bar even when `progress` is set (the figure is not trustworthy yet). */
  indeterminate?: boolean | undefined;
  /** What the agent is doing right now, e.g. "Reading contract 12 of 40". */
  step?: string | undefined;
  /** When the task started, as an ISO 8601 string. */
  startedAt?: string | undefined;
  /** When the task ended (succeeded, failed or was cancelled), as an ISO 8601 string. */
  endedAt?: string | undefined;
  /** One-line summary of the outcome, shown once the task has succeeded. */
  result?: string | undefined;
  /** What went wrong, shown once the task has failed. */
  error?: string | undefined;
}

/** Tasks split into the tray's three sections, each already sorted. */
export interface AiBackgroundTaskGroups {
  /** Waiting on the person; the one that has waited longest first. */
  needsInput: AiBackgroundTask[];
  /** Running, then queued; oldest start first within each. */
  running: AiBackgroundTask[];
  /** Succeeded, failed or cancelled; most recently ended first. */
  finished: AiBackgroundTask[];
}

function time(iso: string | undefined): number | null {
  if (!iso) return null;
  const t = Date.parse(iso);
  return Number.isFinite(t) ? t : null;
}

/** Ascending by time; tasks without a usable time go last, in their given order. */
function byTime(a: number | null, b: number | null, direction: 1 | -1): number {
  if (a === null && b === null) return 0;
  if (a === null) return 1;
  if (b === null) return -1;
  return (a - b) * direction;
}

/**
 * Splits tasks into "needs you", "running" and "finished" and sorts each
 * section. Pure: the input is not changed, and tasks that tie keep the order
 * they were given in.
 */
export function groupAiBackgroundTasks(tasks: readonly AiBackgroundTask[]): AiBackgroundTaskGroups {
  const needsInput: AiBackgroundTask[] = [];
  const running: AiBackgroundTask[] = [];
  const finished: AiBackgroundTask[] = [];
  for (const task of tasks) {
    if (task.status === 'needs-input') needsInput.push(task);
    else if (task.status === 'running' || task.status === 'queued') running.push(task);
    else finished.push(task);
  }
  needsInput.sort((a, b) => byTime(time(a.startedAt), time(b.startedAt), 1));
  running.sort(
    (a, b) =>
      Number(a.status === 'queued') - Number(b.status === 'queued') ||
      byTime(time(a.startedAt), time(b.startedAt), 1),
  );
  finished.sort((a, b) => byTime(time(a.endedAt), time(b.endedAt), -1));
  return { needsInput, running, finished };
}

/**
 * Formats a duration in milliseconds with `Intl.NumberFormat` units:
 * "42 sec", "3 min 12 sec", "1 hr 4 min". Negative durations read as zero.
 */
export function formatAiBackgroundTaskElapsed(ms: number, locale?: string): string {
  const total = Math.max(0, Math.floor(ms / 1000));
  const unit = (value: number, name: 'hour' | 'minute' | 'second') =>
    new Intl.NumberFormat(locale, { style: 'unit', unit: name, unitDisplay: 'short' }).format(
      value,
    );
  const hours = Math.floor(total / 3600);
  const minutes = Math.floor((total % 3600) / 60);
  const seconds = total % 60;
  if (hours > 0) return `${unit(hours, 'hour')} ${unit(minutes, 'minute')}`;
  if (minutes > 0) return `${unit(minutes, 'minute')} ${unit(seconds, 'second')}`;
  return unit(seconds, 'second');
}

/** Every visible and assistive-technology string. English defaults. */
export interface AiBackgroundTasksMessages {
  /** Trigger text and panel heading. */
  title: string;
  /** Trigger badge: how many tasks are running. */
  runningCount: (count: number) => string;
  /** Trigger badge when nothing runs yet but tasks are waiting to start. */
  queuedCount: (count: number) => string;
  /** Trigger badge: how many tasks wait on the person. */
  needsInputCount: (count: number) => string;
  /** Section headings. */
  groups: Record<AiBackgroundTaskGroup, string>;
  /** Section heading with its task count, e.g. "Running (2)". */
  groupHeading: (label: string, count: number) => string;
  /** Status text shown on every task beside its icon. */
  statuses: Record<AiBackgroundTaskStatus, string>;
  /** Elapsed time of a running task, from the formatted duration. */
  elapsed: (duration: string) => string;
  /** End time of a finished task, from relative text such as "5 minutes ago". */
  ended: (relative: string) => string;
  /** Accessible name of a task's progress bar. */
  progressLabel: (title: string) => string;
  /** Open button. */
  open: string;
  /** Cancel button (queued, running or waiting tasks). */
  cancel: string;
  /** Retry button (failed tasks). */
  retry: string;
  /** Respond button (tasks that need input). */
  respond: string;
  /** Dismiss button (finished tasks); icon only, so this is part of its accessible name. */
  dismiss: string;
  /** Button that removes every finished task. */
  clearFinished: string;
  /** Accessible name of a row action, so "Open" says which task it opens. */
  actionLabel: (action: string, title: string) => string;
  /** Shown when there are no tasks. */
  empty: string;
  /** Announced when a task succeeds, fails or starts needing input. */
  announce: (title: string, status: string) => string;
}

export const AI_BACKGROUND_TASKS_MESSAGES: AiBackgroundTasksMessages = {
  title: 'Background tasks',
  runningCount: (n) => `${n} running`,
  queuedCount: (n) => `${n} queued`,
  needsInputCount: (n) => (n === 1 ? '1 needs you' : `${n} need you`),
  groups: { 'needs-input': 'Needs you', running: 'Running', finished: 'Finished' },
  groupHeading: (label, n) => `${label} (${n})`,
  statuses: {
    queued: 'Queued',
    running: 'Running',
    'needs-input': 'Needs your input',
    succeeded: 'Done',
    failed: 'Failed',
    cancelled: 'Cancelled',
  },
  elapsed: (duration) => `Running for ${duration}`,
  ended: (relative) => `Ended ${relative}`,
  progressLabel: (title) => `${title} progress`,
  open: 'Open',
  cancel: 'Cancel',
  retry: 'Retry',
  respond: 'Respond',
  dismiss: 'Dismiss',
  clearFinished: 'Clear finished',
  actionLabel: (action, title) => `${action}: ${title}`,
  empty: 'No background tasks',
  announce: (title, status) => `${title}: ${status}`,
};

export interface AiBackgroundTasksProps extends Omit<
  HTMLAttributes<HTMLDivElement>,
  'title' | 'children'
> {
  /** Every background task, in any order; the tray groups and sorts them. */
  tasks: readonly AiBackgroundTask[];
  /**
   * Renders the panel in place, with no trigger button and no popover, e.g.
   * in a sidebar. Default `false`.
   */
  inline?: boolean | undefined;
  /** Whether the popover is open (controlled). Ignored when `inline`. */
  open?: boolean | undefined;
  /** Whether the popover starts open (uncontrolled). Default `false`. */
  defaultOpen?: boolean | undefined;
  /** Called with the next value when the popover opens or closes. */
  onOpenChange?: ((open: boolean) => void) | undefined;
  /** Which edge of the trigger the popover lines up with. Default `end`. */
  align?: 'start' | 'center' | 'end' | undefined;
  /**
   * Reference time for elapsed and end times. Pass it for identical server
   * rendering and stable screenshots; without it the tray reads the clock
   * after mount and ticks once a second while a visible task is running.
   */
  now?: DateInput | undefined;
  /** BCP 47 locale for durations, percentages and relative times. Default: the runtime locale. */
  locale?: string | undefined;
  /** Heading level of the panel title; section headings are one level below. Default `2`. */
  headingLevel?: 2 | 3 | 4 | 5 | undefined;
  /** Shows an Open button on every task, e.g. to go to its conversation or result. */
  onOpen?: ((task: AiBackgroundTask) => void) | undefined;
  /** Shows a Cancel button on queued, running and waiting tasks. */
  onCancel?: ((task: AiBackgroundTask) => void) | undefined;
  /** Shows a Retry button on failed tasks. */
  onRetry?: ((task: AiBackgroundTask) => void) | undefined;
  /** Shows a Respond button on tasks that need input. */
  onRespond?: ((task: AiBackgroundTask) => void) | undefined;
  /** Shows a Dismiss button on finished tasks. */
  onDismiss?: ((task: AiBackgroundTask) => void) | undefined;
  /** Shows "Clear finished" while there are finished tasks; called with them. */
  onClearFinished?: ((finished: AiBackgroundTask[]) => void) | undefined;
  /**
   * Called when a task succeeds, fails or needs input (the changes the tray
   * announces), e.g. `useAiRunNotifications().notify`, so the person hears
   * about it in another tab.
   */
  notify?: ((notification: AiRunNotification) => void) | undefined;
  /** Replaces any of the English strings; `groups` and `statuses` may be partial. */
  messages?:
    | (Partial<Omit<AiBackgroundTasksMessages, 'groups' | 'statuses'>> & {
        groups?: Partial<Record<AiBackgroundTaskGroup, string>> | undefined;
        statuses?: Partial<Record<AiBackgroundTaskStatus, string>> | undefined;
      })
    | undefined;
}

const ICONS: Record<AiBackgroundTaskStatus, ReactNode> = {
  queued: <ClockGlyph />,
  running: <Spinner size="sm" />,
  'needs-input': <BellGlyph />,
  succeeded: <SuccessGlyph />,
  failed: <ErrorGlyph />,
  cancelled: <DashGlyph />,
};

const TONES: Record<AiBackgroundTaskStatus, ProgressTone> = {
  queued: 'neutral',
  running: 'accent',
  'needs-input': 'warning',
  succeeded: 'success',
  failed: 'danger',
  cancelled: 'neutral',
};

/** The changes worth interrupting for; progress and step text never are. */
const ANNOUNCED: ReadonlySet<AiBackgroundTaskStatus> = new Set([
  'succeeded',
  'failed',
  'needs-input',
]);

function toMs(input: DateInput): number {
  return input instanceof Date ? input.getTime() : new Date(input).getTime();
}

/** True when the person asked for less motion. False on the server. */
function useReducedMotion() {
  const [reduced, setReduced] = useState(false);
  useEffect(() => {
    if (typeof window === 'undefined' || !window.matchMedia) return;
    const query = window.matchMedia('(prefers-reduced-motion: reduce)');
    const update = () => setReduced(query.matches);
    update();
    query.addEventListener?.('change', update);
    return () => query.removeEventListener?.('change', update);
  }, []);
  return reduced;
}

/**
 * A tray for the work agents are doing in the background, so a long task
 * stays visible after the chat is closed: a button with the running and
 * waiting counts opens a panel of tasks grouped into "Needs you", "Running"
 * and "Finished", each with its status, progress, elapsed time and actions.
 * It only reports and asks: every action is a callback, nothing runs here.
 */
export const AiBackgroundTasks = forwardRef<HTMLDivElement, AiBackgroundTasksProps>(
  function AiBackgroundTasks(
    {
      tasks,
      inline = false,
      open: openProp,
      defaultOpen = false,
      onOpenChange,
      align = 'end',
      now: nowProp,
      locale,
      headingLevel = 2,
      onOpen,
      onCancel,
      onRetry,
      onRespond,
      onDismiss,
      onClearFinished,
      notify,
      messages: messagesProp,
      className,
      ...rest
    },
    ref,
  ) {
    const { groups: _g, statuses: _s, ...overrides } = messagesProp ?? {};
    const m = { ...AI_BACKGROUND_TASKS_MESSAGES, ...overrides };
    const groupLabels = { ...AI_BACKGROUND_TASKS_MESSAGES.groups, ...messagesProp?.groups };
    const statuses = { ...AI_BACKGROUND_TASKS_MESSAGES.statuses, ...messagesProp?.statuses };

    const [open, setOpen] = useControllable(openProp, defaultOpen, onOpenChange);
    const titleId = useId();
    const panelRef = useRef<HTMLDivElement>(null);

    const groups = groupAiBackgroundTasks(tasks);
    const runningCount = tasks.filter((t) => t.status === 'running').length;
    const queuedCount = groups.running.length - runningCount;
    const needsCount = groups.needsInput.length;

    // The clock. An injected `now` is used as is; otherwise the time is read
    // after mount (so server and client markup agree) and ticks only while the
    // panel is visible and a task with a start time is running.
    const injected = nowProp === undefined ? undefined : toMs(nowProp);
    const [clock, setClock] = useState<number | null>(null);
    const reduced = useReducedMotion();
    const visible = inline || open;
    const ticking = tasks.some((t) => t.status === 'running' && time(t.startedAt) !== null);
    useEffect(() => {
      if (injected !== undefined || !visible) return;
      setClock(Date.now());
      // Reduced motion: one reading when the panel shows, no ticking text.
      if (!ticking || reduced) return;
      const timer = setInterval(() => setClock(Date.now()), 1000);
      return () => clearInterval(timer);
    }, [injected, visible, ticking, reduced]);
    const now = injected ?? clock;

    // Announce a task succeeding, failing or needing input: once per change,
    // never the first render, never a task that arrives already in that state.
    const seen = useRef<Map<string, AiBackgroundTaskStatus> | null>(null);
    const [live, setLive] = useState('');
    useEffect(() => {
      const before = seen.current;
      seen.current = new Map(tasks.map((t) => [t.id, t.status]));
      if (!before) return;
      const news = tasks.filter((t) => {
        const was = before.get(t.id);
        return was !== undefined && was !== t.status && ANNOUNCED.has(t.status);
      });
      if (news.length) {
        setLive(news.map((t) => m.announce(t.title, statuses[t.status])).join('. '));
        for (const t of news)
          notify?.({
            id: t.id,
            title: t.title,
            body:
              t.status === 'failed' && t.error
                ? `${statuses[t.status]}: ${t.error}`
                : t.status === 'succeeded' && t.result
                  ? `${statuses[t.status]}: ${t.result}`
                  : statuses[t.status],
          });
      }
      // Only a change in the tasks is news.
      // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [tasks]);

    // An action can remove the button that had focus (Dismiss, Clear finished,
    // Cancel…). When that leaves focus nowhere, put it on the panel. The button
    // itself is remembered, not just "something was pressed": an action that
    // keeps its button (Open, Respond) must not pull focus into the panel on a
    // later, unrelated update after the person has moved on.
    const acted = useRef<Element | null>(null);
    useEffect(() => {
      const pressed = acted.current;
      if (!pressed) return;
      acted.current = null;
      if (pressed.isConnected) return;
      const panel = panelRef.current;
      const active = document.activeElement;
      if (panel && (!active || active === document.body)) panel.focus();
    }, [tasks]);
    const act = <A,>(callback: (arg: A) => void, arg: A) => {
      acted.current = typeof document === 'undefined' ? null : document.activeElement;
      callback(arg);
    };

    const Heading = `h${headingLevel}` as 'h2';
    const GroupHeading = `h${headingLevel + 1}` as 'h3';
    const percentFormat = new Intl.NumberFormat(locale, { style: 'percent' });

    const renderTask = (task: AiBackgroundTask) => {
      const { status, title } = task;
      const finished = status === 'succeeded' || status === 'failed' || status === 'cancelled';
      const pending = !finished;
      const known =
        task.progress !== undefined && Number.isFinite(task.progress) && !task.indeterminate
          ? Math.min(1, Math.max(0, task.progress))
          : undefined;
      // A bar while running (animated when the amount is unknown) and, for a
      // task that waits, only when there is a real amount to show.
      const showBar = status === 'running' || (pending && known !== undefined);
      const started = time(task.startedAt);
      const ended = time(task.endedAt);
      const detail =
        status === 'failed'
          ? task.error
          : status === 'succeeded'
            ? task.result
            : pending
              ? task.step
              : undefined;
      const name = (action: string) => m.actionLabel(action, title);
      const hasActions =
        onOpen ||
        (onRespond && status === 'needs-input') ||
        (onCancel && pending) ||
        (onRetry && status === 'failed');

      return (
        <li key={task.id} className="mn-ai-background-tasks__task" data-status={status}>
          <div className="mn-ai-background-tasks__task-head">
            <span className="mn-ai-background-tasks__icon" aria-hidden>
              {ICONS[status]}
            </span>
            <div className="mn-ai-background-tasks__task-text">
              <p className="mn-ai-background-tasks__task-title" dir="auto">
                {title}
              </p>
              <p className="mn-ai-background-tasks__meta">
                <span className="mn-ai-background-tasks__status">{statuses[status]}</span>
                {task.agent ? (
                  <span className="mn-ai-background-tasks__agent">{task.agent}</span>
                ) : null}
                {status === 'running' && started !== null && now !== null ? (
                  <span className="mn-ai-background-tasks__elapsed">
                    {m.elapsed(formatAiBackgroundTaskElapsed(now - started, locale))}
                  </span>
                ) : null}
                {finished && ended !== null && now !== null ? (
                  <time
                    className="mn-ai-background-tasks__ended"
                    dateTime={new Date(ended).toISOString()}
                  >
                    {m.ended(formatRelativeTime(ended, { now, ...(locale ? { locale } : {}) }))}
                  </time>
                ) : null}
              </p>
            </div>
            {finished && onDismiss ? (
              <IconButton
                size="sm"
                className="mn-ai-background-tasks__dismiss"
                label={name(m.dismiss)}
                icon={<CloseGlyph />}
                onClick={() => act(onDismiss, task)}
              />
            ) : null}
          </div>

          {detail ? (
            <p
              className="mn-ai-background-tasks__detail"
              data-kind={finished ? status : 'step'}
              dir="auto"
            >
              {detail}
            </p>
          ) : null}

          {showBar ? (
            <Progress
              className="mn-ai-background-tasks__bar"
              size="sm"
              label={m.progressLabel(title)}
              tone={TONES[status]}
              value={known === undefined ? null : Math.round(known * 100)}
              max={100}
              {...(known !== undefined ? { valueText: percentFormat.format(known) } : {})}
            />
          ) : null}

          {hasActions ? (
            <div className="mn-ai-background-tasks__actions">
              {onRespond && status === 'needs-input' ? (
                <Button
                  size="sm"
                  variant="primary"
                  aria-label={name(m.respond)}
                  onClick={() => act(onRespond, task)}
                >
                  {m.respond}
                </Button>
              ) : null}
              {onRetry && status === 'failed' ? (
                <Button
                  size="sm"
                  variant="secondary"
                  aria-label={name(m.retry)}
                  onClick={() => act(onRetry, task)}
                >
                  {m.retry}
                </Button>
              ) : null}
              {onOpen ? (
                <Button
                  size="sm"
                  variant="secondary"
                  aria-label={name(m.open)}
                  onClick={() => act(onOpen, task)}
                >
                  {m.open}
                </Button>
              ) : null}
              {onCancel && pending ? (
                <Button
                  size="sm"
                  variant="ghost"
                  aria-label={name(m.cancel)}
                  onClick={() => act(onCancel, task)}
                >
                  {m.cancel}
                </Button>
              ) : null}
            </div>
          ) : null}
        </li>
      );
    };

    const renderGroup = (group: AiBackgroundTaskGroup, items: AiBackgroundTask[]) => {
      if (!items.length) return null;
      const headingId = `${titleId}-${group}`;
      return (
        <div className="mn-ai-background-tasks__group" data-group={group}>
          <GroupHeading id={headingId} className="mn-ai-background-tasks__group-title">
            {m.groupHeading(groupLabels[group], items.length)}
          </GroupHeading>
          <ul className="mn-ai-background-tasks__list" aria-labelledby={headingId}>
            {items.map(renderTask)}
          </ul>
        </div>
      );
    };

    const content = (
      <>
        <div className="mn-ai-background-tasks__header">
          <Heading id={titleId} className="mn-ai-background-tasks__title">
            {m.title}
          </Heading>
          {onClearFinished && groups.finished.length ? (
            <Button
              size="sm"
              variant="ghost"
              className="mn-ai-background-tasks__clear"
              onClick={() => act(onClearFinished, groups.finished)}
            >
              {m.clearFinished}
            </Button>
          ) : null}
        </div>
        {tasks.length ? (
          <div className="mn-ai-background-tasks__groups">
            {renderGroup('needs-input', groups.needsInput)}
            {renderGroup('running', groups.running)}
            {renderGroup('finished', groups.finished)}
          </div>
        ) : (
          <p className="mn-ai-background-tasks__empty">{m.empty}</p>
        )}
      </>
    );

    return (
      <div
        ref={ref}
        {...rest}
        className={cx('mn-ai-background-tasks', className)}
        data-inline={inline || undefined}
        data-open={(!inline && open) || undefined}
        data-busy={runningCount > 0 || undefined}
        data-attention={needsCount > 0 || undefined}
      >
        {inline ? (
          <div
            ref={panelRef}
            role="group"
            aria-labelledby={titleId}
            tabIndex={-1}
            className="mn-ai-background-tasks__panel"
          >
            {content}
          </div>
        ) : (
          <Popover open={open} onOpenChange={setOpen}>
            <PopoverTrigger asChild>
              <Button
                size="sm"
                variant="secondary"
                className="mn-ai-background-tasks__trigger"
                leadingIcon={<BotGlyph />}
              >
                <span className="mn-ai-background-tasks__trigger-label">{m.title}</span>
                {runningCount > 0 ? (
                  <Badge size="sm" tone="accent" className="mn-ai-background-tasks__count">
                    {m.runningCount(runningCount)}
                  </Badge>
                ) : queuedCount > 0 ? (
                  <Badge size="sm" className="mn-ai-background-tasks__count">
                    {m.queuedCount(queuedCount)}
                  </Badge>
                ) : null}
                {needsCount > 0 ? (
                  <Badge
                    size="sm"
                    tone="warning"
                    dot
                    className="mn-ai-background-tasks__count"
                    data-attention=""
                  >
                    {m.needsInputCount(needsCount)}
                  </Badge>
                ) : null}
              </Button>
            </PopoverTrigger>
            <PopoverContent
              ref={panelRef}
              align={align}
              aria-labelledby={titleId}
              className="mn-ai-background-tasks__panel mn-ai-background-tasks__popover"
              onOpenAutoFocus={(event) => {
                // Land on the named panel, not on whichever button comes first.
                event.preventDefault();
                panelRef.current?.focus();
              }}
            >
              {content}
            </PopoverContent>
          </Popover>
        )}
        <span className="mn-visually-hidden" role="status" aria-live="polite" aria-atomic="true">
          {live}
        </span>
      </div>
    );
  },
);
