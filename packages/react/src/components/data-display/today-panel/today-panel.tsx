'use client';
import { forwardRef, useId, type ElementType, type HTMLAttributes, type ReactNode } from 'react';
import { cx } from '../../../internal/cx.js';
import {
  formatIsoDate,
  useToday,
  type HourCycle,
  type IsoDate,
} from '../../../internal/dates/index.js';
import { Badge } from '../badge/index.js';
import { Checkbox } from '../../forms/checkbox/index.js';
import { EmptyState } from '../../feedback/empty-state/index.js';
import { InlineErrorRetry } from '../../feedback/inline-error-retry/index.js';
import { Skeleton } from '../../feedback/skeleton/index.js';
import { AgendaList } from '../../scheduling/agenda-list/index.js';
import type { CalendarEvent } from '../../scheduling/event-chip/index.js';

/** Something to do today, or overdue. */
export interface TodayTask {
  /** Stable key. */
  id: string;
  /** What to do, in plain text, e.g. "Review 4 new applicants". */
  title: string;
  /** A second line, e.g. the project or the record it belongs to. */
  detail?: ReactNode;
  /** Short text before the title, such as a time ("10:30") or a key ("PRJ-142"). */
  lead?: ReactNode;
  /** Opens the task. The title becomes a link. */
  href?: string | undefined;
  /** It was due before today. Shown with an "Overdue" tag, never by colour alone. */
  overdue?: boolean | undefined;
  /** Replaces the default due text, e.g. "2 days late" or "By 17:00". */
  due?: ReactNode;
  /** Done (with `onTaskToggle`, the checkbox is ticked). */
  done?: boolean | undefined;
  /** Trailing content, such as a priority badge or an assignee avatar. */
  meta?: ReactNode;
}

/** The words TodayPanel shows. Override them to translate. */
export interface TodayPanelMessages {
  /** The panel heading. */
  title: string;
  /** Heading of the agenda. */
  schedule: string;
  /** Heading of the task list. */
  tasks: string;
  /** Tag on overdue tasks. */
  overdue: string;
  /** Accessible name of a task's checkbox. Receives the task title. */
  markDone: (title: string) => string;
  /** Shown when there is nothing scheduled and nothing due. */
  empty: string;
  /** Shown in the agenda when there are tasks but no events. */
  noEvents: string;
  /** Shown in the task list when there are events but no tasks. */
  noTasks: string;
  /** Link to the full task list. Receives how many tasks are not shown. */
  moreTasks: (hidden: number) => string;
  /** Spoken name of the loading placeholder. */
  loading: string;
}

export const TODAY_PANEL_MESSAGES: TodayPanelMessages = {
  title: 'Today',
  schedule: 'Schedule',
  tasks: 'Due today',
  overdue: 'Overdue',
  markDone: (title) => `Mark "${title}" done`,
  empty: 'Nothing scheduled and nothing due today.',
  noEvents: 'Nothing scheduled',
  noTasks: 'Nothing due today',
  moreTasks: (hidden) => `${hidden} more`,
  loading: 'Loading today',
};

export interface TodayPanelProps extends Omit<HTMLAttributes<HTMLElement>, 'title'> {
  /**
   * Today's meetings and events, shown as an AgendaList. Leave out to show
   * tasks only (a person with no calendar).
   */
  events?: readonly CalendarEvent[] | undefined;
  /** Tasks due today and overdue ones. Overdue tasks are listed first. Leave out to show the agenda only. */
  tasks?: readonly TodayTask[] | undefined;
  /** Shows a checkbox on each task and is called when it is ticked or cleared. The app updates `done`. */
  onTaskToggle?: ((id: string, done: boolean) => void) | undefined;
  /** Called when an event chip is activated. */
  onEventClick?: ((event: CalendarEvent) => void) | undefined;
  /** Component used for task links and the more-tasks link, e.g. a router link. Default `a`. */
  linkComponent?: ElementType | undefined;
  /** Show at most this many tasks. With `tasksHref`, a "N more" link follows. Default: all. */
  maxTasks?: number | undefined;
  /** The full task list ("My work"). */
  tasksHref?: string | undefined;
  /** Today, `'2026-11-07'`. Inject for server rendering or a workspace time zone. Default: the device's. */
  today?: IsoDate | undefined;
  /** IANA zone events are placed in. */
  timeZone?: string | undefined;
  /** BCP 47 locale for the date and times. */
  locale?: string | undefined;
  /** Force a 12- or 24-hour clock in the agenda. */
  hourCycle?: HourCycle | undefined;
  /** Replaces the heading ("Today"). */
  title?: ReactNode;
  /** Level of the panel heading; the agenda and task headings are one below. Default `2`. */
  headingLevel?: 2 | 3 | 4 | undefined;
  /** Shows today's date beside the heading. Default `true`. */
  showDate?: boolean | undefined;
  /** Placeholder while the day loads. */
  loading?: boolean | undefined;
  /** Replaces the content with this failure. */
  error?: ReactNode;
  /** With `error`, shows Try again calling this. */
  onRetry?: (() => unknown) | undefined;
  /** Replaces the default "nothing today" state. */
  empty?: ReactNode;
  /** Strings, for localisation. */
  messages?: Partial<TodayPanelMessages> | undefined;
}

/**
 * A person's day on their home page: today's agenda beside the tasks due
 * today (overdue first), with optional checkboxes to tick tasks off.
 */
export const TodayPanel = forwardRef<HTMLElement, TodayPanelProps>(function TodayPanel(
  {
    events,
    tasks,
    onTaskToggle,
    onEventClick,
    linkComponent: LinkComponent = 'a',
    maxTasks,
    tasksHref,
    today: todayProp,
    timeZone,
    locale,
    hourCycle,
    title,
    headingLevel = 2,
    showDate = true,
    loading = false,
    error,
    onRetry,
    empty,
    messages: messagesProp,
    className,
    ...rest
  },
  ref,
) {
  const messages = { ...TODAY_PANEL_MESSAGES, ...messagesProp };
  const today = useToday(todayProp);
  const titleId = useId();
  const scheduleId = useId();
  const tasksId = useId();
  const Heading = `h${headingLevel}` as const;
  const SubHeading = `h${headingLevel + 1}` as 'h3' | 'h4' | 'h5';
  const hasError = !loading && error !== undefined && error !== null && error !== false;

  const sortedTasks = (tasks ?? [])
    .map((task, index) => ({ task, index }))
    .sort((a, b) => Number(!!b.task.overdue) - Number(!!a.task.overdue) || a.index - b.index)
    .map(({ task }) => task);
  const limit = maxTasks !== undefined && maxTasks > 0 ? maxTasks : sortedTasks.length;
  const shownTasks = sortedTasks.slice(0, limit);
  const hiddenTasks = sortedTasks.length - shownTasks.length;
  const noEvents = (events ?? []).length === 0;
  const noTasks = sortedTasks.length === 0;
  const nothing = noEvents && noTasks;

  let content: ReactNode;
  if (loading) {
    content = (
      <div className="mn-today-panel__skeleton" role="img" aria-label={messages.loading}>
        {[0, 1, 2, 3].map((i) => (
          <Skeleton key={i} shape="text" width={i % 2 ? '70%' : '90%'} />
        ))}
      </div>
    );
  } else if (hasError) {
    content = onRetry ? (
      <InlineErrorRetry onRetry={onRetry}>{error}</InlineErrorRetry>
    ) : (
      <p className="mn-today-panel__error" role="alert">
        {error}
      </p>
    );
  } else if (nothing) {
    content = empty ?? (
      <EmptyState
        size="compact"
        layout="line"
        headingLevel="p"
        kind="success"
        title={messages.empty}
      />
    );
  } else {
    content = (
      <div className="mn-today-panel__columns">
        {events !== undefined ? (
          <section className="mn-today-panel__section" aria-labelledby={scheduleId}>
            <SubHeading id={scheduleId} className="mn-today-panel__section-title">
              {messages.schedule}
            </SubHeading>
            {noEvents ? (
              <p className="mn-today-panel__none">{messages.noEvents}</p>
            ) : (
              <AgendaList
                className="mn-today-panel__agenda"
                events={events}
                from={today}
                days={1}
                today={today}
                headingLevel={(headingLevel + 2) as 4 | 5 | 6}
                {...(timeZone !== undefined && { timeZone })}
                {...(locale !== undefined && { locale })}
                {...(hourCycle !== undefined && { hourCycle })}
                {...(onEventClick !== undefined && { onEventClick })}
              />
            )}
          </section>
        ) : null}
        {tasks !== undefined ? (
          <section className="mn-today-panel__section" aria-labelledby={tasksId}>
            <SubHeading id={tasksId} className="mn-today-panel__section-title">
              {messages.tasks}
            </SubHeading>
            {noTasks ? (
              <p className="mn-today-panel__none">{messages.noTasks}</p>
            ) : (
              <ul className="mn-today-panel__tasks">
                {shownTasks.map((task) => (
                  <li
                    key={task.id}
                    className="mn-today-panel__task"
                    data-overdue={task.overdue ? '' : undefined}
                    data-done={task.done ? '' : undefined}
                  >
                    {onTaskToggle ? (
                      <Checkbox
                        className="mn-today-panel__check"
                        checked={!!task.done}
                        aria-label={messages.markDone(task.title)}
                        onCheckedChange={(checked) => onTaskToggle(task.id, checked === true)}
                      />
                    ) : null}
                    {task.lead !== undefined ? (
                      <span className="mn-today-panel__lead">{task.lead}</span>
                    ) : null}
                    <span className="mn-today-panel__task-body">
                      {task.href ? (
                        <LinkComponent className="mn-today-panel__task-title" href={task.href}>
                          {task.title}
                        </LinkComponent>
                      ) : (
                        <span className="mn-today-panel__task-title">{task.title}</span>
                      )}
                      {task.detail ? (
                        <span className="mn-today-panel__task-detail">{task.detail}</span>
                      ) : null}
                    </span>
                    {task.overdue || task.due ? (
                      <span className="mn-today-panel__due">
                        {task.overdue ? (
                          <Badge tone="danger" size="sm">
                            {messages.overdue}
                          </Badge>
                        ) : null}
                        {task.due ? (
                          <span className="mn-today-panel__due-text">{task.due}</span>
                        ) : null}
                      </span>
                    ) : null}
                    {task.meta ? <span className="mn-today-panel__meta">{task.meta}</span> : null}
                  </li>
                ))}
              </ul>
            )}
            {hiddenTasks > 0 && tasksHref ? (
              <LinkComponent className="mn-today-panel__more" href={tasksHref}>
                {messages.moreTasks(hiddenTasks)}
              </LinkComponent>
            ) : null}
          </section>
        ) : null}
      </div>
    );
  }

  return (
    <section
      ref={ref}
      aria-labelledby={titleId}
      aria-busy={loading || undefined}
      {...rest}
      className={cx('mn-today-panel', className)}
      data-state={loading ? 'loading' : hasError ? 'error' : nothing ? 'empty' : undefined}
    >
      <header className="mn-today-panel__header">
        <Heading id={titleId} className="mn-today-panel__title">
          {title ?? messages.title}
        </Heading>
        {showDate ? (
          <time className="mn-today-panel__date" dateTime={today}>
            {formatIsoDate(today, locale, { weekday: 'long', day: 'numeric', month: 'long' })}
          </time>
        ) : null}
      </header>
      {content}
    </section>
  );
});
