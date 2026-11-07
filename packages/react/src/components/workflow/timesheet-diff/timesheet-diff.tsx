import { forwardRef, useId, type HTMLAttributes, type ReactNode } from 'react';
import { cx } from '../../../internal/cx.js';
import { formatDuration } from '../../forms/duration-input/index.js';

/** One time entry as it was in a version of a timesheet. */
export interface TimesheetDiffEntry {
  /** Stable id, the same in both versions for an entry that changed. */
  id: string;
  /** The day worked, as an ISO date (`2026-01-12`). */
  date: string;
  /** Time worked, in minutes. */
  minutes: number;
  /** Project, client or cost centre name. */
  project?: string | null | undefined;
  /** Activity or category name. */
  activity?: string | null | undefined;
  /** Task, issue key or short description. */
  task?: string | null | undefined;
  /** Whether the time is billable. Omit when billing does not apply. */
  billable?: boolean | undefined;
  /** A note in the person's own words. */
  note?: string | null | undefined;
}

/** An entry present in both versions with different values. */
export interface TimesheetDiffChange {
  id: string;
  /** Names of the fields that changed, keys of `TimesheetDiffEntry` or your own. */
  fields: string[];
  before: TimesheetDiffEntry;
  after: TimesheetDiffEntry;
}

/** The difference between two versions of a timesheet. */
export interface TimesheetDiffResult {
  added: TimesheetDiffEntry[];
  removed: TimesheetDiffEntry[];
  changed: TimesheetDiffChange[];
}

const COMPARED: Array<keyof TimesheetDiffEntry> = [
  'date',
  'minutes',
  'project',
  'activity',
  'task',
  'billable',
  'note',
];

/**
 * Compares two versions of a timesheet by entry id: entries only in `after`
 * are added, only in `before` removed, and in both with different values changed.
 */
export function diffTimesheetEntries(
  before: TimesheetDiffEntry[],
  after: TimesheetDiffEntry[],
): TimesheetDiffResult {
  const old = new Map(before.map((e) => [e.id, e]));
  const next = new Map(after.map((e) => [e.id, e]));
  const added = after.filter((e) => !old.has(e.id));
  const removed = before.filter((e) => !next.has(e.id));
  const changed: TimesheetDiffChange[] = [];
  for (const a of after) {
    const b = old.get(a.id);
    if (!b) continue;
    const fields = COMPARED.filter((k) => (b[k] ?? null) !== (a[k] ?? null));
    if (fields.length) changed.push({ id: a.id, fields, before: b, after: a });
  }
  return { added, removed, changed };
}

/** Total and billable minutes gained (positive) or lost (negative) between the versions. */
export function timesheetDiffTotals(diff: TimesheetDiffResult): {
  total: number;
  billable: number;
} {
  const billable = (e: TimesheetDiffEntry) => (e.billable === false ? 0 : e.minutes);
  let total = 0;
  let bill = 0;
  for (const e of diff.added) {
    total += e.minutes;
    bill += billable(e);
  }
  for (const e of diff.removed) {
    total -= e.minutes;
    bill -= billable(e);
  }
  for (const c of diff.changed) {
    total += c.after.minutes - c.before.minutes;
    bill += billable(c.after) - billable(c.before);
  }
  return { total, billable: bill };
}

/** Visible and announced strings. Override them to translate. */
export interface TimesheetDiffMessages {
  title: string;
  added: string;
  removed: string;
  /** "Changed time, project". */
  changed: (fields: string[]) => string;
  /** Labels for field names in "Changed …". Unknown fields are shown as given. */
  fields: Record<string, string>;
  /** Visually hidden labels for the two lines of a changed entry. */
  before: string;
  after: string;
  noProject: string;
  nonBillable: string;
  noChanges: string;
  /** Formats a signed number of minutes, such as "+1h 30m" or "−45m". */
  delta: (minutes: number) => string;
  /** The totals line. */
  totals: (total: string, billable: string | null) => string;
  /** Formats an ISO date for an entry line. */
  date: (isoDate: string, locale: string | undefined) => string;
}

function signed(minutes: number): string {
  if (minutes === 0) return '0m';
  return `${minutes > 0 ? '+' : '−'}${formatDuration(Math.abs(minutes))}`;
}

export const DEFAULT_TIMESHEET_DIFF_MESSAGES: TimesheetDiffMessages = {
  title: 'What changed',
  added: 'Added',
  removed: 'Removed',
  changed: (fields) => `Changed ${fields.join(', ')}`,
  fields: {
    date: 'date',
    minutes: 'time',
    project: 'project',
    activity: 'activity',
    task: 'task',
    billable: 'billable',
    note: 'note',
    start: 'start',
    end: 'end',
  },
  before: 'Before:',
  after: 'After:',
  noProject: 'No project',
  nonBillable: 'non-billable',
  noChanges: 'No changes.',
  delta: signed,
  totals: (total, billable) =>
    billable === null ? `Total ${total}` : `Total ${total} · billable ${billable}`,
  date: (iso, locale) => {
    const d = new Date(`${iso.slice(0, 10)}T00:00:00Z`);
    if (Number.isNaN(d.getTime())) return iso;
    return new Intl.DateTimeFormat(locale, {
      weekday: 'short',
      day: 'numeric',
      month: 'short',
      timeZone: 'UTC',
    }).format(d);
  },
};

export interface TimesheetDiffProps
  extends Omit<HTMLAttributes<HTMLElement>, 'children' | 'title'>, TimesheetDiffResult {
  /** Heading. Default "What changed". */
  title?: ReactNode;
  /** Quiet text after the heading, such as "since version 3". */
  subtitle?: ReactNode;
  /** Heading level of the title. Default 3. */
  headingLevel?: 2 | 3 | 4 | 5 | 6;
  /** Show the totals line. Default `true`. */
  showTotals?: boolean;
  /** Show billable totals; turn off when billing does not apply. Default `true`. */
  showBillable?: boolean;
  /** Draw an entry yourself instead of "Mon 12 Jan · 2h · Project · Activity · Task". */
  renderEntry?: ((entry: TimesheetDiffEntry) => ReactNode) | undefined;
  /** BCP 47 locale for dates. */
  locale?: string | undefined;
  /** Override visible strings. */
  messages?: Partial<TimesheetDiffMessages>;
}

/**
 * What changed between two versions of a timesheet: added, removed and
 * changed entries, each labelled in words and with its effect on the total.
 * For resubmissions, corrections and approval review.
 */
export const TimesheetDiff = forwardRef<HTMLElement, TimesheetDiffProps>(function TimesheetDiff(
  {
    added,
    removed,
    changed,
    title,
    subtitle,
    headingLevel = 3,
    showTotals = true,
    showBillable = true,
    renderEntry,
    locale,
    messages: messagesProp,
    className,
    ...rest
  },
  ref,
) {
  const messages = { ...DEFAULT_TIMESHEET_DIFF_MESSAGES, ...messagesProp };
  const fieldLabels = { ...DEFAULT_TIMESHEET_DIFF_MESSAGES.fields, ...messagesProp?.fields };
  const headingId = useId();
  const Heading = `h${headingLevel}` as const;
  const none = added.length === 0 && removed.length === 0 && changed.length === 0;
  const totals = timesheetDiffTotals({ added, removed, changed });

  const line = (e: TimesheetDiffEntry): ReactNode => {
    if (renderEntry) return renderEntry(e);
    return [
      messages.date(e.date, locale),
      formatDuration(e.minutes),
      e.project || messages.noProject,
      e.activity,
      e.task,
      e.billable === false ? messages.nonBillable : null,
    ]
      .filter(Boolean)
      .join(' · ');
  };

  return (
    <section
      ref={ref}
      aria-labelledby={headingId}
      {...rest}
      className={cx('mn-timesheet-diff', className)}
      data-empty={none || undefined}
    >
      <Heading id={headingId} className="mn-timesheet-diff__title">
        {title ?? messages.title}
        {subtitle ? <span className="mn-timesheet-diff__subtitle"> {subtitle}</span> : null}
      </Heading>
      {none ? (
        <p className="mn-timesheet-diff__empty">{messages.noChanges}</p>
      ) : (
        <ul className="mn-timesheet-diff__list">
          {added.map((e) => (
            <li key={`a-${e.id}`} className="mn-timesheet-diff__item" data-kind="added">
              <span className="mn-timesheet-diff__head">
                <span className="mn-timesheet-diff__kind">{messages.added}</span>
                <span className="mn-timesheet-diff__delta">{messages.delta(e.minutes)}</span>
              </span>
              <span className="mn-timesheet-diff__line">{line(e)}</span>
            </li>
          ))}
          {removed.map((e) => (
            <li key={`r-${e.id}`} className="mn-timesheet-diff__item" data-kind="removed">
              <span className="mn-timesheet-diff__head">
                <span className="mn-timesheet-diff__kind">{messages.removed}</span>
                <span className="mn-timesheet-diff__delta">{messages.delta(-e.minutes)}</span>
              </span>
              <del className="mn-timesheet-diff__line">{line(e)}</del>
            </li>
          ))}
          {changed.map((c) => {
            const delta = c.after.minutes - c.before.minutes;
            return (
              <li key={`c-${c.id}`} className="mn-timesheet-diff__item" data-kind="changed">
                <span className="mn-timesheet-diff__head">
                  <span className="mn-timesheet-diff__kind">
                    {messages.changed(c.fields.map((f) => fieldLabels[f] ?? f))}
                  </span>
                  {delta !== 0 ? (
                    <span className="mn-timesheet-diff__delta">{messages.delta(delta)}</span>
                  ) : null}
                </span>
                <del className="mn-timesheet-diff__line" data-version="before">
                  <span className="mn-visually-hidden">{messages.before} </span>
                  {line(c.before)}
                </del>
                <ins className="mn-timesheet-diff__line" data-version="after">
                  <span className="mn-visually-hidden">{messages.after} </span>
                  {line(c.after)}
                </ins>
              </li>
            );
          })}
        </ul>
      )}
      {showTotals ? (
        <p className="mn-timesheet-diff__totals">
          {messages.totals(
            messages.delta(totals.total),
            showBillable ? messages.delta(totals.billable) : null,
          )}
        </p>
      ) : null}
    </section>
  );
});
