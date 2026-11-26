'use client';

import {
  forwardRef,
  useEffect,
  useId,
  useState,
  type HTMLAttributes,
  type MouseEvent,
} from 'react';
import { cx } from '../../../internal/cx.js';
import {
  dateTimeFormat,
  useClientLocale,
  useClientZone,
} from '../../../internal/dates/client-time.js';
import { CalendarGlyph } from '../../../internal/icons.js';
import { isSafeUrl } from '../../../internal/sanitize.js';
import { Avatar } from '../../identity/avatar/index.js';
import {
  PRIORITY_PICKER_MESSAGES,
  PriorityGlyph,
  PriorityPicker,
  type PriorityLevel,
} from '../priority-picker/index.js';
import { StatusGlyph, StatusPicker, type StatusOption } from '../status-picker/index.js';

/** A label on an issue. */
export interface IssueCardLabel {
  id: string;
  name: string;
}

/** The person an issue is assigned to. */
export interface IssueCardAssignee {
  name: string;
  avatarSrc?: string | null | undefined;
}

/** Every string an issue card shows or announces. */
export interface IssueCardMessages {
  /** Visible due date text. */
  due: (date: string) => string;
  /** Visible marker added when the due date has passed. */
  overdue: string;
  /** Accessible name of the label list. */
  labels: string;
  /** Spoken when there is no assignee. */
  unassigned: string;
  /** Accessible name of the assignee avatar. */
  assignee: (name: string) => string;
  /** Spoken status prefix when the status is read-only. */
  status: (name: string) => string;
  /** Spoken priority when it is read-only. */
  priority: (level: string) => string;
  /** Priority level names. */
  priorityLevels?: Partial<Record<PriorityLevel, string>> | undefined;
}

export const ISSUE_CARD_MESSAGES: IssueCardMessages = {
  due: (date) => `Due ${date}`,
  overdue: 'Overdue',
  labels: 'Labels',
  unassigned: 'Unassigned',
  assignee: (name) => `Assigned to ${name}`,
  status: (name) => `Status: ${name}`,
  priority: (level) => `Priority: ${level}`,
};

/** Parses a due date. Date-only strings (`2026-03-12`) are calendar days, not midnight UTC instants. */
function parseDue(value: string | number | Date): { date: Date; dateOnly: boolean } | null {
  if (value instanceof Date)
    return Number.isNaN(value.getTime()) ? null : { date: value, dateOnly: false };
  if (typeof value === 'string' && /^\d{4}-\d{2}-\d{2}$/.test(value)) {
    const date = new Date(`${value}T00:00:00Z`);
    return Number.isNaN(date.getTime()) ? null : { date, dateOnly: true };
  }
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? null : { date, dateOnly: false };
}

/** Whether a due date is before today (`today` as `YYYY-MM-DD` in the viewer's zone). */
export function isIssueOverdue(due: string | number | Date, today: string): boolean {
  const parsed = parseDue(due);
  if (!parsed) return false;
  const d = parsed.date;
  const day = parsed.dateOnly
    ? d.toISOString().slice(0, 10)
    : `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
  return day < today;
}

function localToday(): string {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}

export interface IssueCardProps extends Omit<HTMLAttributes<HTMLElement>, 'title'> {
  /** The issue key, e.g. `STR-142`. */
  issueKey: string;
  /** The issue title. */
  title: string;
  /** Where the issue opens. Only http(s) and relative URLs are linked. */
  href?: string | undefined;
  /** Opens the issue. Makes the title a button when there is no `href`. */
  onOpen?: ((event: MouseEvent<HTMLElement>) => void) | undefined;
  /** The current status. */
  status?: StatusOption | undefined;
  /** All statuses; with `onStatusChange` the status becomes a StatusPicker. */
  statuses?: readonly StatusOption[] | undefined;
  /** Changes the status from the card. */
  onStatusChange?: ((id: string) => void) | undefined;
  /** The priority. */
  priority?: PriorityLevel | undefined;
  /** Changes the priority from the card (shows a PriorityPicker). */
  onPriorityChange?: ((level: PriorityLevel) => void) | undefined;
  /** The assignee; `null` shows an empty avatar slot read as “Unassigned”. */
  assignee?: IssueCardAssignee | null | undefined;
  /** Labels, shown as small chips. */
  labels?: readonly IssueCardLabel[] | undefined;
  /** Due date: a `YYYY-MM-DD` day, an ISO date-time, epoch milliseconds or a Date. */
  dueDate?: string | number | Date | undefined;
  /**
   * Today as `YYYY-MM-DD`, for deciding overdue. Pass it from the server for
   * identical server and client output; without it overdue is worked out
   * after hydration from the viewer's clock.
   */
  today?: string | undefined;
  /** BCP 47 locale for the due date. Default: the runtime locale. */
  locale?: string | undefined;
  /** The card is selected (for example in a multi-select board). */
  selected?: boolean | undefined;
  /** The card is being dragged; lifts it visually. */
  dragging?: boolean | undefined;
  /** Translated strings. */
  messages?: Partial<IssueCardMessages> | undefined;
}

/**
 * An issue on a board or in a list: key, title, status, priority, labels, due
 * date and assignee. The title is the one link to the issue; status and
 * priority can be changed in place with their pickers.
 */
export const IssueCard = forwardRef<HTMLElement, IssueCardProps>(function IssueCard(
  {
    issueKey,
    title,
    href,
    onOpen,
    status,
    statuses,
    onStatusChange,
    priority,
    onPriorityChange,
    assignee,
    labels,
    dueDate,
    today: todayProp,
    locale,
    selected = false,
    dragging = false,
    messages: messagesProp,
    className,
    ...rest
  },
  ref,
) {
  const m = { ...ISSUE_CARD_MESSAGES, ...messagesProp };
  const levels = { ...PRIORITY_PICKER_MESSAGES.levels, ...m.priorityLevels };
  const titleId = useId();
  const [clientToday, setClientToday] = useState<string | null>(null);
  useEffect(() => {
    if (!todayProp) setClientToday(localToday());
  }, [todayProp]);
  const today = todayProp ?? clientToday;
  // A due time is shown in UTC and one fixed locale on the server and while hydrating,
  // then in the viewer's own: the server's zone can put it on a different day.
  const zone = useClientZone();
  const dueLocale = useClientLocale(locale);

  const due = dueDate === undefined ? null : parseDue(dueDate);
  const done = status?.category === 'completed' || status?.category === 'cancelled';
  const overdue = !!(
    due &&
    today &&
    !done &&
    isIssueOverdue(dueDate as string | number | Date, today)
  );
  const dueText = due
    ? dateTimeFormat(dueLocale, {
        day: 'numeric',
        month: 'short',
        timeZone: due.dateOnly ? 'UTC' : zone,
      }).format(due.date)
    : '';
  const dueIso = due
    ? due.dateOnly
      ? due.date.toISOString().slice(0, 10)
      : due.date.toISOString()
    : undefined;
  const safeHref = href && isSafeUrl(href, ['http', 'https']) ? href : undefined;

  const titleEl = safeHref ? (
    <a id={titleId} className="mn-issue-card__title" href={safeHref} onClick={onOpen}>
      {title}
    </a>
  ) : onOpen ? (
    <button id={titleId} type="button" className="mn-issue-card__title" onClick={onOpen}>
      {title}
    </button>
  ) : (
    <span id={titleId} className="mn-issue-card__title">
      {title}
    </span>
  );

  return (
    <article
      ref={ref}
      aria-labelledby={titleId}
      {...rest}
      className={cx('mn-issue-card', className)}
      data-selected={selected || undefined}
      data-dragging={dragging || undefined}
      data-overdue={overdue || undefined}
      data-interactive={(safeHref || onOpen) && true ? '' : undefined}
    >
      <div className="mn-issue-card__header">
        <span className="mn-issue-card__key">{issueKey}</span>
        {priority !== undefined ? (
          onPriorityChange ? (
            <PriorityPicker
              className="mn-issue-card__control"
              value={priority}
              onValueChange={onPriorityChange}
              showLabel={false}
              size="sm"
              messages={{ levels }}
            />
          ) : (
            <span
              className="mn-issue-card__priority"
              role="img"
              aria-label={m.priority(levels[priority])}
            >
              <PriorityGlyph priority={priority} />
            </span>
          )
        ) : null}
      </div>
      {titleEl}
      <div className="mn-issue-card__meta">
        {status ? (
          statuses && onStatusChange ? (
            <StatusPicker
              className="mn-issue-card__control"
              statuses={statuses}
              value={status.id}
              onValueChange={onStatusChange}
              size="sm"
            />
          ) : (
            <span className="mn-issue-card__status">
              <StatusGlyph category={status.category} />
              <span className="mn-visually-hidden">{m.status(status.name)}</span>
              <span aria-hidden>{status.name}</span>
            </span>
          )
        ) : null}
        {labels?.length ? (
          <ul className="mn-issue-card__labels" aria-label={m.labels}>
            {labels.map((l) => (
              <li key={l.id} className="mn-issue-card__label">
                {l.name}
              </li>
            ))}
          </ul>
        ) : null}
        {due ? (
          <span className="mn-issue-card__due" data-overdue={overdue || undefined}>
            <CalendarGlyph />
            <time dateTime={dueIso}>{m.due(dueText)}</time>
            {overdue ? <span className="mn-issue-card__overdue">{m.overdue}</span> : null}
          </span>
        ) : null}
        {assignee !== undefined ? (
          <span className="mn-issue-card__assignee">
            {assignee ? (
              <span role="img" aria-label={m.assignee(assignee.name)}>
                <Avatar
                  name={assignee.name}
                  src={assignee.avatarSrc ?? null}
                  size="xs"
                  decorative
                />
              </span>
            ) : (
              <span className="mn-issue-card__unassigned" role="img" aria-label={m.unassigned} />
            )}
          </span>
        ) : null}
      </div>
    </article>
  );
});
