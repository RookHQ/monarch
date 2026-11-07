'use client';

import {
  forwardRef,
  useId,
  useRef,
  useState,
  type HTMLAttributes,
  type KeyboardEvent,
  type ReactNode,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { LockGlyph } from '../../../internal/icons.js';
import { formatDuration, parseDuration } from '../../forms/duration-input/index.js';

/** One column of the grid: a day. */
export interface TimesheetDay {
  /** ISO date, `2026-01-12`. Also the key of each row's `cells`. */
  date: string;
  /** Heading text. Default: the weekday and day of month in `locale`, such as "Mon 12". */
  label?: ReactNode;
  /** Highlights the column as today. */
  today?: boolean;
  /** A weekend or holiday: the column is shaded. Entry is still allowed unless `locked`. */
  nonWorking?: boolean;
  /**
   * The day cannot be edited (approved, payroll closed, leave). `true`, or a
   * short reason shown under the heading and read with each cell.
   */
  locked?: boolean | string;
  /** Small notes under the heading, such as a holiday or leave name. */
  markers?: ReactNode;
  /** Minutes expected that day; shown as "of 8h" under the day total. */
  expectedMinutes?: number;
}

/** One row of the grid: a project, task or issue. */
export interface TimesheetRow {
  /** Stable id, passed back to `onCellChange`. */
  id: string;
  /** Row heading, such as a project name. */
  label: ReactNode;
  /** Plain-text name of the row for cell labels when `label` is not a string. */
  labelText?: string;
  /** Secondary text under the label, such as the activity or "Billable". */
  description?: ReactNode;
  /** Minutes per day, keyed by ISO date. Missing days are empty. */
  cells: Record<string, number>;
  /** The row cannot be typed into, for example time from a timer or an import. */
  readOnly?: boolean;
}

/** An extra footer row of per-day values, such as attendance. Display only. */
export interface TimesheetSummaryRow {
  id: string;
  label: ReactNode;
  /** Value per ISO date. */
  cells: Record<string, ReactNode>;
  /** Value in the total column. */
  total?: ReactNode;
}

/** Visible and announced strings. Override them to translate. */
export interface TimesheetGridMessages {
  /** Table caption (visually hidden) and accessible name. */
  caption: string;
  /** Heading of the first column. */
  rowHeader: string;
  /** Heading of the total column and footer row. */
  total: string;
  dayTotal: string;
  /** Hidden heading of the actions column. */
  actions: string;
  /** Accessible name of a cell input or button, such as "Website refresh, Mon 12 Jan". */
  cellLabel: (row: string, day: string) => string;
  /** Read with a locked cell. */
  locked: string;
  /** Cell input that cannot be read. */
  invalid: string;
  /** Cell value above `maxMinutesPerDay`. */
  tooLong: (max: string) => string;
  /** A save rejected without a message. */
  saveFailed: string;
  /** Under a day total: "of 8h". */
  expected: (duration: string) => string;
  /** Hidden note on a day total above `maxMinutesPerDay`. */
  overLimit: string;
  /** Shown when there are no rows. */
  empty: string;
  /** Formats minutes in cells and totals. Default "1h 30m". */
  duration: (minutes: number) => string;
  /** Formats a day for headings and cell labels. */
  day: (isoDate: string, style: 'short' | 'long', locale: string | undefined) => string;
}

export const DEFAULT_TIMESHEET_GRID_MESSAGES: TimesheetGridMessages = {
  caption: 'Time per row and day. Type durations such as 1:30, 1.5 or 90m and press Enter.',
  rowHeader: 'Project',
  total: 'Total',
  dayTotal: 'Day total',
  actions: 'Row actions',
  cellLabel: (row, day) => `${row}, ${day}`,
  locked: 'locked',
  invalid: 'Use a time such as 1:30, 1.5 or 90m.',
  tooLong: (max) => `A day has at most ${max}.`,
  saveFailed: 'That time could not be saved.',
  expected: (d) => `of ${d}`,
  overLimit: 'over the daily limit',
  empty: 'No time logged for this period.',
  duration: (m) => formatDuration(m),
  day: (iso, style, locale) => {
    const d = new Date(`${iso.slice(0, 10)}T00:00:00Z`);
    if (Number.isNaN(d.getTime())) return iso;
    return new Intl.DateTimeFormat(
      locale,
      style === 'short'
        ? { weekday: 'short', day: 'numeric', timeZone: 'UTC' }
        : { weekday: 'short', day: 'numeric', month: 'short', timeZone: 'UTC' },
    ).format(d);
  },
};

export interface TimesheetGridProps extends Omit<HTMLAttributes<HTMLDivElement>, 'children'> {
  /** The columns, usually the seven days of a week. */
  days: TimesheetDay[];
  /** The rows. */
  rows: TimesheetRow[];
  /**
   * Called when someone commits a new value in a cell (Enter, Tab or leaving
   * it). `0` clears the cell. Return a promise to show the cell as saving; a
   * rejection shows its message and keeps the typed text. Without it, cells
   * are read-only.
   */
  onCellChange?: ((rowId: string, date: string, minutes: number) => unknown) | undefined;
  /** Makes read-only cells with time buttons, for example to open the entries behind them. */
  onCellActivate?: ((rowId: string, date: string) => void) | undefined;
  /** Makes day headings buttons, for example to open a day view. */
  onDayActivate?: ((date: string) => void) | undefined;
  /** Controls at the end of each row, such as a clear-row button. */
  rowActions?: ((row: TimesheetRow) => ReactNode) | undefined;
  /** Extra display-only footer rows, such as attendance. */
  summaryRows?: TimesheetSummaryRow[];
  /** Largest value accepted in one cell, and the day total above which a day is flagged. Default 1440 (24 hours). */
  maxMinutesPerDay?: number;
  /** The whole grid is read-only, for example after submission. */
  readOnly?: boolean;
  /** Shown in place of the rows when there are none. */
  emptyMessage?: ReactNode;
  /** BCP 47 locale for day headings. */
  locale?: string | undefined;
  /** Override visible and announced strings. */
  messages?: Partial<TimesheetGridMessages>;
}

function sum(values: number[]) {
  return values.reduce((a, b) => a + b, 0);
}

/**
 * A week of time entry: rows of projects or tasks against days, with fast
 * keyboard entry ("1:30", "1.5", "90m"), row, day and period totals, and
 * locked days. Arrow keys move between cells like a spreadsheet.
 */
export const TimesheetGrid = forwardRef<HTMLDivElement, TimesheetGridProps>(function TimesheetGrid(
  {
    days,
    rows,
    onCellChange,
    onCellActivate,
    onDayActivate,
    rowActions,
    summaryRows = [],
    maxMinutesPerDay = 1440,
    readOnly = false,
    emptyMessage,
    locale,
    messages: messagesProp,
    className,
    ...rest
  },
  ref,
) {
  const messages = { ...DEFAULT_TIMESHEET_GRID_MESSAGES, ...messagesProp };
  const captionId = useId();
  const tableRef = useRef<HTMLTableElement>(null);
  const [error, setError] = useState('');
  const editable = !readOnly && onCellChange !== undefined;
  const hasActions = rowActions !== undefined;
  const dayTotals = days.map((d) => sum(rows.map((r) => r.cells[d.date] ?? 0)));
  const grandTotal = sum(dayTotals);

  function move(row: number, col: number, dRow: number, dCol: number) {
    const table = tableRef.current;
    if (!table) return;
    let r = row + dRow;
    let c = col + dCol;
    // Skip cells that cannot take focus (locked days, read-only rows).
    while (r >= 0 && r < rows.length && c >= 0 && c < days.length) {
      const el = table.querySelector<HTMLElement>(`[data-cell="${r}:${c}"]`);
      if (el && !(el as HTMLInputElement).disabled) {
        el.focus();
        if (el instanceof HTMLInputElement) el.select();
        return;
      }
      r += dRow;
      c += dCol;
    }
  }

  return (
    <div
      ref={ref}
      {...rest}
      className={cx('mn-timesheet-grid', className)}
      data-readonly={!editable || undefined}
    >
      {/* The scroller is focusable so keyboard users can scroll a read-only grid. */}
      <div
        className="mn-timesheet-grid__scroller"
        role="region"
        aria-labelledby={captionId}
        // eslint-disable-next-line jsx-a11y/no-noninteractive-tabindex
        tabIndex={0}
      >
        <table ref={tableRef} className="mn-timesheet-grid__table">
          <caption id={captionId} className="mn-visually-hidden">
            {messages.caption}
          </caption>
          <thead>
            <tr>
              <th scope="col" className="mn-timesheet-grid__rowhead">
                {messages.rowHeader}
              </th>
              {days.map((d) => {
                const text = d.label ?? messages.day(d.date, 'short', locale);
                return (
                  <th
                    key={d.date}
                    scope="col"
                    className="mn-timesheet-grid__day"
                    data-today={d.today || undefined}
                    data-nonworking={d.nonWorking || undefined}
                    data-locked={d.locked ? '' : undefined}
                    aria-current={d.today ? 'date' : undefined}
                  >
                    {onDayActivate ? (
                      <button
                        type="button"
                        className="mn-timesheet-grid__day-button"
                        onClick={() => onDayActivate(d.date)}
                      >
                        {text}
                      </button>
                    ) : (
                      <span className="mn-timesheet-grid__day-text">{text}</span>
                    )}
                    {d.locked ? (
                      <span className="mn-timesheet-grid__marker">
                        <LockGlyph aria-hidden />
                        {typeof d.locked === 'string' ? (
                          d.locked
                        ) : (
                          <span className="mn-visually-hidden">{messages.locked}</span>
                        )}
                      </span>
                    ) : null}
                    {d.markers ? (
                      <span className="mn-timesheet-grid__marker">{d.markers}</span>
                    ) : null}
                  </th>
                );
              })}
              <th scope="col" className="mn-timesheet-grid__total">
                {messages.total}
              </th>
              {hasActions ? (
                <th scope="col" className="mn-timesheet-grid__actions">
                  <span className="mn-visually-hidden">{messages.actions}</span>
                </th>
              ) : null}
            </tr>
          </thead>
          <tbody>
            {rows.length === 0 ? (
              <tr>
                <td
                  colSpan={days.length + 2 + (hasActions ? 1 : 0)}
                  className="mn-timesheet-grid__empty"
                >
                  {emptyMessage ?? messages.empty}
                </td>
              </tr>
            ) : (
              rows.map((row, ri) => {
                const rowName =
                  row.labelText ?? (typeof row.label === 'string' ? row.label : row.id);
                return (
                  <tr key={row.id} data-readonly={row.readOnly || undefined}>
                    <th scope="row" className="mn-timesheet-grid__rowhead">
                      <span className="mn-timesheet-grid__row-label">{row.label}</span>
                      {row.description ? (
                        <span className="mn-timesheet-grid__row-description">
                          {row.description}
                        </span>
                      ) : null}
                    </th>
                    {days.map((d, ci) => {
                      const value = row.cells[d.date] ?? 0;
                      const label = messages.cellLabel(
                        rowName,
                        messages.day(d.date, 'long', locale),
                      );
                      const lockedReason = d.locked
                        ? typeof d.locked === 'string'
                          ? d.locked
                          : messages.locked
                        : null;
                      const cellEditable = editable && !row.readOnly && !d.locked;
                      return (
                        <td
                          key={d.date}
                          className="mn-timesheet-grid__cell"
                          data-nonworking={d.nonWorking || undefined}
                          data-today={d.today || undefined}
                        >
                          {cellEditable ? (
                            <CellInput
                              id={`${ri}:${ci}`}
                              value={value}
                              label={label}
                              max={maxMinutesPerDay}
                              messages={messages}
                              onCommit={(minutes) => onCellChange(row.id, d.date, minutes)}
                              onError={setError}
                              onMove={(dr, dc) => move(ri, ci, dr, dc)}
                            />
                          ) : onCellActivate && value > 0 ? (
                            <button
                              type="button"
                              data-cell={`${ri}:${ci}`}
                              className="mn-timesheet-grid__value-button"
                              aria-label={`${messages.duration(value)}, ${label}${lockedReason ? `, ${lockedReason}` : ''}`}
                              onClick={() => onCellActivate(row.id, d.date)}
                              onKeyDown={(e) => arrowMove(e, (dr, dc) => move(ri, ci, dr, dc))}
                            >
                              {messages.duration(value)}
                            </button>
                          ) : (
                            <span className="mn-timesheet-grid__value">
                              {value > 0 ? messages.duration(value) : <span aria-hidden>–</span>}
                              {lockedReason && editable && !row.readOnly ? (
                                <span className="mn-visually-hidden">, {lockedReason}</span>
                              ) : null}
                            </span>
                          )}
                        </td>
                      );
                    })}
                    <td className="mn-timesheet-grid__total">
                      {messages.duration(sum(days.map((d) => row.cells[d.date] ?? 0)))}
                    </td>
                    {hasActions ? (
                      <td className="mn-timesheet-grid__actions">{rowActions(row)}</td>
                    ) : null}
                  </tr>
                );
              })
            )}
          </tbody>
          <tfoot>
            <tr className="mn-timesheet-grid__footer">
              <th scope="row" className="mn-timesheet-grid__rowhead">
                {messages.dayTotal}
              </th>
              {days.map((d, i) => {
                const total = dayTotals[i] ?? 0;
                const over = total > maxMinutesPerDay;
                return (
                  <td
                    key={d.date}
                    className="mn-timesheet-grid__day-total"
                    data-over={over || undefined}
                  >
                    <span>{total > 0 ? messages.duration(total) : <span aria-hidden>–</span>}</span>
                    {over ? (
                      <span className="mn-visually-hidden"> ({messages.overLimit})</span>
                    ) : null}
                    {d.expectedMinutes ? (
                      <span className="mn-timesheet-grid__expected">
                        {messages.expected(messages.duration(d.expectedMinutes))}
                      </span>
                    ) : null}
                  </td>
                );
              })}
              <td className="mn-timesheet-grid__total">{messages.duration(grandTotal)}</td>
              {hasActions ? <td /> : null}
            </tr>
            {summaryRows.map((s) => (
              <tr key={s.id} className="mn-timesheet-grid__summary">
                <th scope="row" className="mn-timesheet-grid__rowhead">
                  {s.label}
                </th>
                {days.map((d) => (
                  <td key={d.date} className="mn-timesheet-grid__day-total">
                    {s.cells[d.date] ?? <span aria-hidden>–</span>}
                  </td>
                ))}
                <td className="mn-timesheet-grid__total">{s.total ?? null}</td>
                {hasActions ? <td /> : null}
              </tr>
            ))}
          </tfoot>
        </table>
      </div>
      <p className="mn-timesheet-grid__error" role="status" aria-live="polite">
        {error}
      </p>
    </div>
  );
});

function arrowMove(e: KeyboardEvent<HTMLElement>, onMove: (dr: number, dc: number) => void) {
  const rtl = getComputedStyle(e.currentTarget).direction === 'rtl';
  if (e.key === 'ArrowDown') onMove(1, 0);
  else if (e.key === 'ArrowUp') onMove(-1, 0);
  else if (e.key === 'ArrowRight') onMove(0, rtl ? -1 : 1);
  else if (e.key === 'ArrowLeft') onMove(0, rtl ? 1 : -1);
  else return;
  e.preventDefault();
}

interface CellInputProps {
  id: string;
  value: number;
  label: string;
  max: number;
  messages: TimesheetGridMessages;
  onCommit: (minutes: number) => unknown;
  onError: (message: string) => void;
  onMove: (dRow: number, dCol: number) => void;
}

function CellInput({ id, value, label, max, messages, onCommit, onError, onMove }: CellInputProps) {
  const [draft, setDraft] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [invalid, setInvalid] = useState(false);
  const shown = draft ?? (value > 0 ? messages.duration(value) : '');

  /** Resolves true when the value is accepted (or unchanged). */
  async function commit(): Promise<boolean> {
    if (draft === null) return true;
    const parsed = parseDuration(draft);
    const minutes = parsed === null ? 0 : parsed;
    if (Number.isNaN(minutes)) {
      setInvalid(true);
      onError(messages.invalid);
      return false;
    }
    if (minutes > max) {
      setInvalid(true);
      onError(messages.tooLong(messages.duration(max)));
      return false;
    }
    setInvalid(false);
    onError('');
    if (minutes === value) {
      setDraft(null);
      return true;
    }
    setBusy(true);
    try {
      await onCommit(minutes);
      setDraft(null);
      return true;
    } catch (e) {
      setInvalid(true);
      onError(e instanceof Error && e.message ? e.message : messages.saveFailed);
      return false;
    } finally {
      setBusy(false);
    }
  }

  return (
    <input
      data-cell={id}
      className="mn-timesheet-grid__input"
      aria-label={label}
      aria-invalid={invalid || undefined}
      aria-busy={busy || undefined}
      data-busy={busy || undefined}
      inputMode="decimal"
      autoComplete="off"
      readOnly={busy}
      value={shown}
      placeholder="–"
      onChange={(e) => setDraft(e.target.value)}
      onFocus={(e) => e.currentTarget.select()}
      onBlur={() => void commit()}
      onKeyDown={(e) => {
        const input = e.currentTarget;
        if (e.key === 'Enter') {
          e.preventDefault();
          void commit().then((ok) => ok && onMove(1, 0));
        } else if (e.key === 'Escape') {
          if (draft !== null) {
            e.preventDefault();
            setDraft(null);
            setInvalid(false);
            onError('');
          }
        } else if (e.key === 'ArrowDown' || e.key === 'ArrowUp') {
          arrowMove(e, onMove);
        } else if (
          (e.key === 'ArrowRight' || e.key === 'ArrowLeft') &&
          input.selectionStart === input.selectionEnd
        ) {
          const atEnd = input.selectionStart === input.value.length;
          const atStart = input.selectionStart === 0;
          const rtl = getComputedStyle(input).direction === 'rtl';
          const forward = e.key === (rtl ? 'ArrowLeft' : 'ArrowRight');
          if ((forward && atEnd) || (!forward && atStart)) arrowMove(e, onMove);
        } else if (
          (e.key === 'ArrowRight' || e.key === 'ArrowLeft') &&
          input.selectionStart === 0 &&
          input.selectionEnd === input.value.length
        ) {
          // Whole value selected (just focused): arrows move between cells.
          arrowMove(e, onMove);
        }
      }}
    />
  );
}
