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
  formatIsoDate,
  formatTime,
  useToday,
  type HourCycle,
  type IsoDate,
} from '../../../internal/dates/index.js';
import { Avatar } from '../../identity/avatar/index.js';
import { Alert } from '../../feedback/alert/index.js';
import { EmptyState } from '../../feedback/empty-state/index.js';
import { Skeleton } from '../../feedback/skeleton/index.js';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuRadioGroup,
  DropdownMenuRadioItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '../../overlays/dropdown-menu/index.js';

/** A shift definition. */
export interface ShiftRosterShift {
  id: string;
  /** e.g. "Morning". */
  name: string;
  /** Short label for narrow cells, e.g. "M". Default `name`. */
  shortName?: string | undefined;
  /** Start, minutes from midnight. */
  start: number;
  /** End, minutes from midnight. Less than or equal to `start` means it ends the next day. */
  end: number;
  /** Accent colour (any CSS colour) for the cell's edge. */
  color?: string | undefined;
}

/** A row: one person. */
export interface ShiftRosterMember {
  id: string;
  name: string;
  avatarUrl?: string | undefined;
  /** Second line, e.g. team or employee code. */
  description?: string | undefined;
}

/**
 * Assignments by member id, then by day: a shift id, `null` for a day off,
 * or missing for not assigned.
 */
export type ShiftRosterValue = Readonly<Record<string, Readonly<Record<IsoDate, string | null>>>>;

/** One change, passed with `onValueChange`. */
export interface ShiftRosterChange {
  memberId: string;
  day: IsoDate;
  /** The new shift id, `null` for a day off, `undefined` when cleared. */
  shiftId: string | null | undefined;
}

/** Every fixed string ShiftRoster shows, for translation. */
export interface ShiftRosterMessages {
  person: string;
  /** Accessible name of the grid. */
  grid: string;
  off: string;
  /** Shown for a day with no assignment. */
  unassigned: string;
  /** Menu item that removes the assignment. */
  clear: string;
  /** Heading of the cell menu. */
  menuLabel: (name: string, day: string) => string;
  /** Header of the hours column. */
  hours: string;
  /** Hours total, e.g. "40 h". */
  hoursValue: (hours: number) => string;
  /** Footer row label. */
  working: string;
  /** Footer cell, e.g. "3 working". */
  workingValue: (count: number) => string;
  overnight: string;
  emptyTitle: string;
  emptyDescription: string;
}

const DEFAULT_MESSAGES: ShiftRosterMessages = {
  person: 'Person',
  grid: 'Shift roster',
  off: 'Off',
  unassigned: 'Not assigned',
  clear: 'Clear',
  menuLabel: (name, day) => `${name}, ${day}`,
  hours: 'Hours',
  hoursValue: (h) => `${Math.round(h * 10) / 10} h`,
  working: 'Working',
  workingValue: (n) => String(n),
  overnight: 'overnight',
  emptyTitle: 'No one to schedule',
  emptyDescription: 'People appear here with their shift for each day.',
};

export interface ShiftRosterProps extends Omit<
  HTMLAttributes<HTMLDivElement>,
  'children' | 'defaultValue' | 'onChange'
> {
  /** The rows, in display order. */
  members: readonly ShiftRosterMember[];
  /** Shifts that can be assigned, in menu order. */
  shifts: readonly ShiftRosterShift[];
  /** First day shown. */
  from: IsoDate;
  /** Number of days shown. Default 7. */
  days?: number | undefined;
  /** Assignments (controlled). */
  value?: ShiftRosterValue | undefined;
  /** Initial assignments (uncontrolled). */
  defaultValue?: ShiftRosterValue | undefined;
  /** Called with the new assignments and the one change. Save the change here. */
  onValueChange?: ((value: ShiftRosterValue, change: ShiftRosterChange) => void) | undefined;
  /** View only: cells are focusable but have no menu. */
  readOnly?: boolean | undefined;
  /** Makes the person cell a button, e.g. to open their assignments. */
  onMemberClick?: ((member: ShiftRosterMember) => void) | undefined;
  /** Show the hours column and the working-count row. Default `true`. */
  showTotals?: boolean | undefined;
  /** Today, highlighted. Default: the viewer's today. */
  today?: IsoDate | undefined;
  /** Locale for day names and times. */
  locale?: string | undefined;
  /** Force a 12- or 24-hour clock. */
  hourCycle?: HourCycle | undefined;
  /** Shows a placeholder instead of the grid. */
  loading?: boolean | undefined;
  /** Shows a danger alert instead of the grid. */
  error?: ReactNode | undefined;
  /** Replaces the default empty state. */
  empty?: ReactNode | undefined;
  /** Interface text, for translation. */
  messages?: Partial<ShiftRosterMessages> | undefined;
}

const duration = (s: ShiftRosterShift) =>
  s.end > s.start ? s.end - s.start : s.end + 1440 - s.start;

/**
 * Who works which shift on which day: people down the side, days across,
 * each cell a menu of shifts (or a day off). A keyboard grid with one tab
 * stop, hours per person and a working count per day. Data in, changes
 * out; the product saves each change.
 */
export const ShiftRoster = forwardRef<HTMLDivElement, ShiftRosterProps>(function ShiftRoster(
  {
    members,
    shifts,
    from,
    days: dayCount = 7,
    value: valueProp,
    defaultValue = {},
    onValueChange,
    readOnly = false,
    onMemberClick,
    showTotals = true,
    today: todayProp,
    locale,
    hourCycle,
    loading = false,
    error,
    empty,
    messages: messagesProp,
    className,
    ...rest
  },
  ref,
) {
  const m = { ...DEFAULT_MESSAGES, ...messagesProp };
  const today = useToday(todayProp);
  const [state, setState] = useState<ShiftRosterValue>(defaultValue);
  const value = valueProp ?? state;
  const days = useMemo(
    () => Array.from({ length: Math.max(1, dayCount) }, (_, i) => addDays(from, i)),
    [from, dayCount],
  );
  const shiftById = useMemo(() => new Map(shifts.map((s) => [s.id, s])), [shifts]);
  const [active, setActive] = useState({ row: 0, col: 0 });
  const row = Math.min(active.row, Math.max(0, members.length - 1));
  const col = Math.min(active.col, days.length - 1);
  const tableRef = useRef<HTMLTableElement>(null);
  const captionId = useId();

  const range = (s: ShiftRosterShift) =>
    `${formatTime(s.start, locale, hourCycle)}–${formatTime(s.end % 1440, locale, hourCycle)}`;
  const describe = (assigned: string | null | undefined) => {
    if (assigned === null) return m.off;
    const s = assigned ? shiftById.get(assigned) : undefined;
    if (!s) return m.unassigned;
    return `${s.name} ${range(s)}${s.end <= s.start ? `, ${m.overnight}` : ''}`;
  };

  const assign = (member: ShiftRosterMember, day: IsoDate, shiftId: string | null | undefined) => {
    const row: Record<IsoDate, string | null> = Object.fromEntries(
      Object.entries(value[member.id] ?? {}).filter(([d]) => d !== day),
    );
    if (shiftId !== undefined) row[day] = shiftId;
    const next = { ...value, [member.id]: row };
    if (valueProp === undefined) setState(next);
    onValueChange?.(next, { memberId: member.id, day, shiftId });
  };

  const focusCell = (r: number, c: number) => {
    setActive({ row: r, col: c });
    tableRef.current?.querySelector<HTMLElement>(`[data-cell="${r}:${c}"]`)?.focus();
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
      case 'Delete':
      case 'Backspace': {
        const member = members[r];
        const day = days[c];
        if (!readOnly && member && day && value[member.id]?.[day] !== undefined) {
          event.preventDefault();
          assign(member, day, undefined);
        }
        return;
      }
      default:
        return;
    }
    // preventDefault also stops the menu trigger from opening on ArrowDown.
    event.preventDefault();
    focusCell(next[0], next[1]);
  };

  const hoursFor = (member: ShiftRosterMember) =>
    days.reduce((sum, d) => {
      const id = value[member.id]?.[d];
      const s = id ? shiftById.get(id) : undefined;
      return sum + (s ? duration(s) / 60 : 0);
    }, 0);
  const workingOn = (d: IsoDate) =>
    members.filter((p) => {
      const id = value[p.id]?.[d];
      return !!id && shiftById.has(id);
    }).length;

  let content: ReactNode;
  if (loading) {
    content = <Skeleton shape="rect" className="mn-shift-roster__placeholder" />;
  } else if (error) {
    content = <Alert tone="danger">{error}</Alert>;
  } else if (members.length === 0) {
    content = empty ?? (
      <EmptyState size="compact" title={m.emptyTitle} description={m.emptyDescription} />
    );
  } else {
    content = (
      <div className="mn-shift-roster__scroll">
        <table
          ref={tableRef}
          // eslint-disable-next-line jsx-a11y/no-noninteractive-element-to-interactive-role -- APG data grid: a table with role="grid" and a roving tab stop
          role="grid"
          aria-labelledby={captionId}
          className="mn-shift-roster__grid"
        >
          <caption id={captionId} className="mn-visually-hidden">
            {m.grid}
          </caption>
          <thead>
            <tr>
              <th scope="col" className="mn-shift-roster__corner">
                {m.person}
              </th>
              {days.map((d) => (
                <th
                  key={d}
                  scope="col"
                  className="mn-shift-roster__day"
                  data-today={d === today || undefined}
                  abbr={formatIsoDate(d, locale, {
                    weekday: 'long',
                    day: 'numeric',
                    month: 'long',
                  })}
                >
                  <span className="mn-shift-roster__weekday">
                    {formatIsoDate(d, locale, { weekday: 'short' })}
                  </span>{' '}
                  <span>{formatIsoDate(d, locale, { day: 'numeric', month: 'short' })}</span>
                </th>
              ))}
              {showTotals ? (
                <th scope="col" className="mn-shift-roster__total-head">
                  {m.hours}
                </th>
              ) : null}
            </tr>
          </thead>
          <tbody>
            {members.map((member, r) => (
              <tr key={member.id}>
                <th scope="row" className="mn-shift-roster__person">
                  {onMemberClick ? (
                    <button
                      type="button"
                      className="mn-shift-roster__person-button"
                      onClick={() => onMemberClick(member)}
                    >
                      <PersonLabel member={member} />
                    </button>
                  ) : (
                    <PersonLabel member={member} />
                  )}
                </th>
                {days.map((d, c) => {
                  const assigned = value[member.id]?.[d];
                  const shift = assigned ? shiftById.get(assigned) : undefined;
                  const dayLabel = formatIsoDate(d, locale, {
                    weekday: 'long',
                    day: 'numeric',
                    month: 'long',
                  });
                  const label = `${member.name}, ${dayLabel}: ${describe(assigned)}`;
                  const isActive = r === row && c === col;
                  const focusProps = {
                    'data-cell': `${r}:${c}`,
                    tabIndex: isActive ? 0 : -1,
                    onFocus: () => setActive({ row: r, col: c }),
                    onKeyDown: (e: KeyboardEvent<HTMLElement>) => onKeyDown(e, r, c),
                  };
                  const look = {
                    className: 'mn-shift-roster__slot',
                    'data-state': shift ? 'shift' : assigned === null ? 'off' : 'empty',
                    style: shift?.color
                      ? ({ ['--mn-shift-roster-color']: shift.color } as CSSProperties)
                      : undefined,
                  };
                  const face = shift ? (
                    <>
                      <span className="mn-shift-roster__shift-name">
                        {shift.shortName ?? shift.name}
                      </span>
                      <span className="mn-shift-roster__shift-time">{range(shift)}</span>
                    </>
                  ) : assigned === null ? (
                    <span>{m.off}</span>
                  ) : (
                    <span aria-hidden="true">–</span>
                  );
                  return (
                    <td
                      key={d}
                      className="mn-shift-roster__cell"
                      data-today={d === today || undefined}
                      {...(readOnly ? focusProps : {})}
                    >
                      {readOnly ? (
                        <>
                          <span aria-hidden="true" {...look}>
                            {face}
                          </span>
                          <span className="mn-visually-hidden">{describe(assigned)}</span>
                        </>
                      ) : (
                        <DropdownMenu>
                          <DropdownMenuTrigger asChild>
                            <button type="button" aria-label={label} {...focusProps} {...look}>
                              {face}
                            </button>
                          </DropdownMenuTrigger>
                          <DropdownMenuContent align="start">
                            <DropdownMenuLabel>
                              {m.menuLabel(member.name, dayLabel)}
                            </DropdownMenuLabel>
                            <DropdownMenuRadioGroup
                              value={assigned === null ? '__off' : (assigned ?? '')}
                              onValueChange={(v) => assign(member, d, v === '__off' ? null : v)}
                            >
                              {shifts.map((s) => (
                                <DropdownMenuRadioItem key={s.id} value={s.id}>
                                  {s.name} · {range(s)}
                                </DropdownMenuRadioItem>
                              ))}
                              <DropdownMenuRadioItem value="__off">{m.off}</DropdownMenuRadioItem>
                            </DropdownMenuRadioGroup>
                            {assigned !== undefined ? (
                              <>
                                <DropdownMenuSeparator />
                                <DropdownMenuItem onSelect={() => assign(member, d, undefined)}>
                                  {m.clear}
                                </DropdownMenuItem>
                              </>
                            ) : null}
                          </DropdownMenuContent>
                        </DropdownMenu>
                      )}
                    </td>
                  );
                })}
                {showTotals ? (
                  <td className="mn-shift-roster__total">{m.hoursValue(hoursFor(member))}</td>
                ) : null}
              </tr>
            ))}
          </tbody>
          {showTotals ? (
            <tfoot>
              <tr>
                <th scope="row" className="mn-shift-roster__person mn-shift-roster__foot-label">
                  {m.working}
                </th>
                {days.map((d) => (
                  <td key={d} className="mn-shift-roster__count">
                    {m.workingValue(workingOn(d))}
                  </td>
                ))}
                <td />
              </tr>
            </tfoot>
          ) : null}
        </table>
      </div>
    );
  }

  return (
    <div
      ref={ref}
      {...rest}
      className={cx('mn-shift-roster', className)}
      data-readonly={readOnly || undefined}
      aria-busy={loading || undefined}
    >
      {content}
    </div>
  );
});

function PersonLabel({ member }: { member: ShiftRosterMember }) {
  return (
    <span className="mn-shift-roster__person-inner">
      <Avatar name={member.name} src={member.avatarUrl ?? null} size="xs" decorative />
      <span className="mn-shift-roster__person-text">
        <span className="mn-shift-roster__name">{member.name}</span>
        {member.description ? (
          <span className="mn-shift-roster__description">{member.description}</span>
        ) : null}
      </span>
    </span>
  );
}
