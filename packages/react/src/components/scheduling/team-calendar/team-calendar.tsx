'use client';

import {
  forwardRef,
  useId,
  useMemo,
  useRef,
  useState,
  type CSSProperties,
  type HTMLAttributes,
  type KeyboardEvent,
  type ReactNode,
} from 'react';
import { cx } from '../../../internal/cx.js';
import {
  addDays,
  dayOfWeek,
  daysBetween,
  endOfMonth,
  formatIsoDate,
  startOfMonth,
  useToday,
  type IsoDate,
  type Weekday,
} from '../../../internal/dates/index.js';
import { Avatar } from '../../identity/avatar/index.js';
import { EmptyState } from '../../feedback/empty-state/index.js';
import { Alert } from '../../feedback/alert/index.js';
import { Skeleton } from '../../feedback/skeleton/index.js';

/** A row: one person. */
export interface TeamCalendarMember {
  id: string;
  name: string;
  /** Photo URL for the Avatar. */
  avatarUrl?: string | undefined;
  /** Second line, e.g. role or team. */
  description?: string | undefined;
}

/** Which part of a day an entry covers. */
export type TeamCalendarPortion = 'full' | 'first-half' | 'second-half';

/** One absence or other entry on a person's row. */
export interface TeamCalendarEntry {
  /** Stable id, passed back on click. */
  id: string;
  memberId: string;
  /** First day, `YYYY-MM-DD`. */
  start: IsoDate;
  /** Last day, inclusive. Default `start`. */
  end?: IsoDate | undefined;
  /** What it is, e.g. "Annual leave", or "Away" for private types. */
  label: string;
  /** Bar colour (any CSS colour). Default the accent. */
  color?: string | undefined;
  /** Half days: `first-half` or `second-half` on every day of the entry. Default `full`. */
  portion?: TeamCalendarPortion | undefined;
  /** Per-day portions, overriding `portion` for those days. */
  portions?: Readonly<Record<IsoDate, TeamCalendarPortion>> | undefined;
  /** Waiting for approval: drawn faded and outlined. */
  pending?: boolean | undefined;
}

/** A non-working day for everyone, e.g. a public holiday. */
export interface TeamCalendarHoliday {
  date: IsoDate;
  name: string;
}

/** Every fixed string TeamCalendar shows, for translation. */
export interface TeamCalendarMessages {
  /** Header of the people column. */
  person: string;
  /** Accessible name of the grid, e.g. "Team calendar, November 2026". */
  grid: (range: string) => string;
  firstHalf: string;
  secondHalf: string;
  pending: string;
  /** Hidden text after a holiday's day header. */
  holiday: (name: string) => string;
  emptyTitle: string;
  emptyDescription: string;
  /** The legend under the grid. */
  legend: string;
}

const DEFAULT_MESSAGES: TeamCalendarMessages = {
  person: 'Person',
  grid: (range) => `Team calendar, ${range}`,
  firstHalf: 'first half',
  secondHalf: 'second half',
  pending: 'pending',
  holiday: (name) => `Holiday: ${name}`,
  emptyTitle: 'Nobody to show',
  emptyDescription: 'People and their time away appear here.',
  legend:
    'Shaded columns are weekends and holidays. Half bars are half days; faded bars are waiting for approval.',
};

export interface TeamCalendarProps extends Omit<HTMLAttributes<HTMLDivElement>, 'children'> {
  /** The rows, in display order. */
  members: readonly TeamCalendarMember[];
  /** Entries for the visible range. Entries outside it are ignored. */
  entries: readonly TeamCalendarEntry[];
  /** The month to show (any date in it). Ignored when `from` and `to` are set. */
  month?: IsoDate | undefined;
  /** First day of a custom range. */
  from?: IsoDate | undefined;
  /** Last day of a custom range, inclusive. */
  to?: IsoDate | undefined;
  /** Shaded non-working days. */
  holidays?: readonly TeamCalendarHoliday[] | undefined;
  /** Weekend days, shaded. Default Saturday and Sunday. */
  weekendDays?: readonly Weekday[] | undefined;
  /** Today, highlighted. Default: the viewer's today. */
  today?: IsoDate | undefined;
  /** Locale for day and month names. */
  locale?: string | undefined;
  /** Called when an entry is activated (click, Enter or Space on its day). */
  onEntryClick?:
    ((entry: TeamCalendarEntry, member: TeamCalendarMember, day: IsoDate) => void) | undefined;
  /** Called when an empty day is activated, e.g. to add time away. */
  onCellClick?: ((member: TeamCalendarMember, day: IsoDate) => void) | undefined;
  /** Shows a placeholder instead of the grid. */
  loading?: boolean | undefined;
  /** Shows a danger alert instead of the grid. */
  error?: ReactNode | undefined;
  /** Replaces the default empty state when there are no members. */
  empty?: ReactNode | undefined;
  /** Shows the legend line under the grid. Default `true`. */
  showLegend?: boolean | undefined;
  /** Interface text, for translation. */
  messages?: Partial<TeamCalendarMessages> | undefined;
}

interface Cell {
  entry: TeamCalendarEntry;
  portion: TeamCalendarPortion;
}

/**
 * Who is away when: people down the side, days across, absences as bars
 * (half days as half bars, pending ones faded), weekends and holidays
 * shaded. A keyboard grid with one tab stop. Data in, clicks out; put a
 * CalendarToolbar above it for month navigation.
 */
export const TeamCalendar = forwardRef<HTMLDivElement, TeamCalendarProps>(function TeamCalendar(
  {
    members,
    entries,
    month,
    from: fromProp,
    to: toProp,
    holidays,
    weekendDays = [0, 6],
    today: todayProp,
    locale,
    onEntryClick,
    onCellClick,
    loading = false,
    error,
    empty,
    showLegend = true,
    messages: messagesProp,
    className,
    ...rest
  },
  ref,
) {
  const m = { ...DEFAULT_MESSAGES, ...messagesProp };
  const today = useToday(todayProp);
  const anchor = month ?? today;
  const from = fromProp && toProp ? fromProp : startOfMonth(anchor);
  const to = fromProp && toProp ? toProp : endOfMonth(anchor);
  const days = useMemo(() => {
    const n = Math.max(0, daysBetween(from, to)) + 1;
    return Array.from({ length: n }, (_, i) => addDays(from, i));
  }, [from, to]);
  const holidayMap = useMemo(
    () => new Map((holidays ?? []).map((h) => [h.date, h.name])),
    [holidays],
  );
  const cells = useMemo(() => {
    const map = new Map<string, Cell>();
    for (const e of entries) {
      const last = e.end ?? e.start;
      for (let d = e.start < from ? from : e.start; d <= last && d <= to; d = addDays(d, 1))
        map.set(`${e.memberId}|${d}`, {
          entry: e,
          portion: e.portions?.[d] ?? e.portion ?? 'full',
        });
    }
    return map;
  }, [entries, from, to]);

  const [active, setActive] = useState<{ row: number; col: number }>({ row: 0, col: 0 });
  const row = Math.min(active.row, Math.max(0, members.length - 1));
  const col = Math.min(active.col, Math.max(0, days.length - 1));
  const tableRef = useRef<HTMLTableElement>(null);
  const captionId = useId();

  const rangeLabel =
    fromProp && toProp
      ? `${formatIsoDate(from, locale, { day: 'numeric', month: 'short' })} – ${formatIsoDate(to, locale, { day: 'numeric', month: 'short', year: 'numeric' })}`
      : formatIsoDate(from, locale, { month: 'long', year: 'numeric' });

  const shaded = (d: IsoDate) => weekendDays.includes(dayOfWeek(d)) || holidayMap.has(d);

  const focusCell = (r: number, c: number) => {
    setActive({ row: r, col: c });
    tableRef.current?.querySelector<HTMLElement>(`[data-cell="${r}:${c}"]`)?.focus();
  };

  const activate = (member: TeamCalendarMember, day: IsoDate) => {
    const cell = cells.get(`${member.id}|${day}`);
    if (cell) onEntryClick?.(cell.entry, member, day);
    else onCellClick?.(member, day);
  };

  const onKeyDown = (event: KeyboardEvent<HTMLElement>, r: number, c: number) => {
    const rtl = tableRef.current?.closest('[dir="rtl"]') != null;
    const lastRow = members.length - 1;
    const lastCol = days.length - 1;
    let next: [number, number] | null = null;
    switch (event.key) {
      case 'ArrowRight':
        next = [r, Math.min(lastCol, Math.max(0, c + (rtl ? -1 : 1)))];
        break;
      case 'ArrowLeft':
        next = [r, Math.min(lastCol, Math.max(0, c + (rtl ? 1 : -1)))];
        break;
      case 'ArrowDown':
        next = [Math.min(lastRow, r + 1), c];
        break;
      case 'ArrowUp':
        next = [Math.max(0, r - 1), c];
        break;
      case 'Home':
        next = event.ctrlKey ? [0, 0] : [r, 0];
        break;
      case 'End':
        next = event.ctrlKey ? [lastRow, lastCol] : [r, lastCol];
        break;
      case 'PageDown':
        next = [r, Math.min(lastCol, c + 7)];
        break;
      case 'PageUp':
        next = [r, Math.max(0, c - 7)];
        break;
      case 'Enter':
      case ' ': {
        event.preventDefault();
        const member = members[r];
        const day = days[c];
        if (member && day) activate(member, day);
        return;
      }
      default:
        return;
    }
    event.preventDefault();
    focusCell(next[0], next[1]);
  };

  let content: ReactNode;
  if (loading) {
    content = <Skeleton shape="rect" className="mn-team-calendar__placeholder" />;
  } else if (error) {
    content = <Alert tone="danger">{error}</Alert>;
  } else if (members.length === 0) {
    content = empty ?? (
      <EmptyState size="compact" title={m.emptyTitle} description={m.emptyDescription} />
    );
  } else {
    content = (
      <div className="mn-team-calendar__scroll">
        <table
          ref={tableRef}
          // eslint-disable-next-line jsx-a11y/no-noninteractive-element-to-interactive-role -- APG data grid: a table with role="grid" and a roving tab stop
          role="grid"
          aria-labelledby={captionId}
          aria-rowcount={members.length + 1}
          className="mn-team-calendar__grid"
          style={{ ['--mn-team-calendar-days']: days.length } as CSSProperties}
        >
          <caption id={captionId} className="mn-visually-hidden">
            {m.grid(rangeLabel)}
          </caption>
          <thead>
            <tr>
              <th scope="col" className="mn-team-calendar__corner">
                {m.person}
              </th>
              {days.map((d) => {
                const holiday = holidayMap.get(d);
                return (
                  <th
                    key={d}
                    scope="col"
                    className="mn-team-calendar__day"
                    data-shaded={shaded(d) || undefined}
                    data-today={d === today || undefined}
                    title={holiday}
                    abbr={formatIsoDate(d, locale, {
                      weekday: 'long',
                      day: 'numeric',
                      month: 'long',
                    })}
                  >
                    <span className="mn-team-calendar__day-number">
                      {formatIsoDate(d, locale, { day: 'numeric' })}
                    </span>
                    <span className="mn-team-calendar__weekday" aria-hidden="true">
                      {formatIsoDate(d, locale, { weekday: 'narrow' })}
                    </span>
                    {holiday ? (
                      <span className="mn-visually-hidden">{m.holiday(holiday)}</span>
                    ) : null}
                  </th>
                );
              })}
            </tr>
          </thead>
          <tbody>
            {members.map((member, r) => (
              <tr key={member.id}>
                <th scope="row" className="mn-team-calendar__person">
                  <span className="mn-team-calendar__person-inner">
                    <Avatar
                      name={member.name}
                      src={member.avatarUrl ?? null}
                      size="xs"
                      decorative
                    />
                    <span className="mn-team-calendar__person-text">
                      <span className="mn-team-calendar__name">{member.name}</span>
                      {member.description ? (
                        <span className="mn-team-calendar__description">{member.description}</span>
                      ) : null}
                    </span>
                  </span>
                </th>
                {days.map((d, c) => {
                  const cell = cells.get(`${member.id}|${d}`);
                  const isActive = r === row && c === col;
                  return (
                    <td
                      key={d}
                      data-cell={`${r}:${c}`}
                      className="mn-team-calendar__cell"
                      data-shaded={shaded(d) || undefined}
                      data-today={d === today || undefined}
                      data-clickable={(cell ? !!onEntryClick : !!onCellClick) || undefined}
                      tabIndex={isActive ? 0 : -1}
                      onFocus={() => setActive({ row: r, col: c })}
                      onKeyDown={(e) => onKeyDown(e, r, c)}
                      onClick={() => activate(member, d)}
                    >
                      {cell ? (
                        <span
                          className="mn-team-calendar__bar"
                          data-portion={cell.portion}
                          data-pending={cell.entry.pending || undefined}
                          style={
                            cell.entry.color
                              ? ({
                                  ['--mn-team-calendar-color']: cell.entry.color,
                                } as CSSProperties)
                              : undefined
                          }
                          title={cell.entry.label}
                        >
                          <span className="mn-visually-hidden">
                            {cell.entry.label}
                            {cell.portion === 'first-half'
                              ? `, ${m.firstHalf}`
                              : cell.portion === 'second-half'
                                ? `, ${m.secondHalf}`
                                : ''}
                            {cell.entry.pending ? `, ${m.pending}` : ''}
                          </span>
                        </span>
                      ) : null}
                    </td>
                  );
                })}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    );
  }

  return (
    <div
      ref={ref}
      {...rest}
      className={cx('mn-team-calendar', className)}
      aria-busy={loading || undefined}
    >
      {content}
      {showLegend && !loading && !error && members.length > 0 ? (
        <p className="mn-team-calendar__legend">{m.legend}</p>
      ) : null}
    </div>
  );
});
