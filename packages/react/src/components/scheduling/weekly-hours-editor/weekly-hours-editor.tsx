'use client';

import {
  forwardRef,
  useCallback,
  useId,
  useMemo,
  useRef,
  useState,
  type FieldsetHTMLAttributes,
  type ReactNode,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { CloseGlyph, CopyGlyph } from '../../../internal/icons.js';
import { Button, IconButton } from '../../actions/button/index.js';
import { NativeSelect } from '../../forms/form-field/index.js';
import { Switch } from '../../forms/switch/index.js';
import { type HourCycle, type Weekday } from '../../../internal/dates/index.js';

/**
 * One available interval: ISO weekday (1 = Monday … 7 = Sunday) and minutes
 * from midnight, `end` exclusive and at most 1440. A list of these is the
 * whole value, a plain shape that serialises as is.
 */
export interface WeeklyHoursInterval {
  day: number;
  start: number;
  end: number;
}

export type WeeklyHoursValue = readonly WeeklyHoursInterval[];

/** Why a day's intervals are invalid. */
export type WeeklyHoursError = 'range' | 'order' | 'overlap';

/**
 * Checks every day: times fall within the day (`range`), each interval ends
 * after it starts (`order`) and no two intervals overlap (`overlap`). Returns
 * the first problem per ISO weekday; an empty object means the value is valid.
 */
export function validateWeeklyHours(
  value: WeeklyHoursValue,
): Partial<Record<number, WeeklyHoursError>> {
  const out: Partial<Record<number, WeeklyHoursError>> = {};
  for (let day = 1; day <= 7; day++) {
    const ranges = value.filter((r) => r.day === day);
    let error: WeeklyHoursError | undefined;
    if (ranges.some((r) => r.start < 0 || r.end > 1440)) error = 'range';
    else if (ranges.some((r) => r.end <= r.start)) error = 'order';
    else {
      const sorted = [...ranges].sort((a, b) => a.start - b.start);
      for (let i = 1; i < sorted.length; i++)
        if ((sorted[i]?.start ?? 0) < (sorted[i - 1]?.end ?? 0)) error = 'overlap';
    }
    if (error) out[day] = error;
  }
  return out;
}

export interface WeeklyHoursEditorLabels {
  /** Shown for a day that is switched off. Default "Unavailable". */
  unavailable?: string;
  /** Default `${day} interval ${n} start`. */
  start?: (day: string, n: number) => string;
  /** Default `${day} interval ${n} end`. */
  end?: (day: string, n: number) => string;
  /** Default `Add ${day} interval`. */
  add?: (day: string) => string;
  /** Default `Remove ${day} interval ${n}`. */
  remove?: (day: string, n: number) => string;
  /**
   * Name of a day's copy button. Default `Copy to all days from ${day}`; keep
   * the visible `copyToAllShort` text at its start (WCAG label in name).
   */
  copyToAll?: (day: string) => string;
  /** Visible text of the copy button. Default "Copy to all". */
  copyToAllShort?: string;
  /** Error messages, shown under the day. */
  errors?: Partial<Record<WeeklyHoursError, string>>;
}

export interface WeeklyHoursEditorProps extends Omit<
  FieldsetHTMLAttributes<HTMLFieldSetElement>,
  'defaultValue' | 'onChange'
> {
  /** Controlled value. */
  value?: WeeklyHoursValue;
  /** Uncontrolled starting value. Default: no hours on any day. */
  defaultValue?: WeeklyHoursValue;
  /**
   * Called with every change, intervals ordered by ISO weekday, and the
   * current validation result (see `validateWeeklyHours`) so the product can
   * block saving while it is not empty.
   */
  onValueChange?: (
    value: WeeklyHoursInterval[],
    errors: Partial<Record<number, WeeklyHoursError>>,
  ) => void;
  /** Visible group title. Default "Weekly hours". */
  legend?: ReactNode;
  /**
   * Slot beside the legend for the zone the hours are in: text such as
   * "Times in Asia/Kolkata", or a timezone picker.
   */
  timeZoneLabel?: ReactNode;
  /**
   * First row: 0 = Sunday … 6 = Saturday, as in MiniMonth and the date
   * pickers. Default 1 (Monday). (Interval `day` values stay ISO 1–7.)
   */
  weekStartsOn?: Weekday;
  /** BCP 47 locale for day names and times. */
  locale?: string;
  /** Force a 12- or 24-hour clock (`'12'` or `'24'`, as in the date pickers); by default the locale decides. */
  hourCycle?: HourCycle;
  /** Minutes between time options. Default 15. */
  step?: number;
  /** The interval a day gets when switched on for the first time. Default 09:00–17:00. */
  defaultInterval?: { start: number; end: number };
  /** Most intervals per day. Default 6. */
  maxIntervals?: number;
  /** Shows the hours as text, without controls. */
  readOnly?: boolean;
  /** Submits the value as JSON in a hidden input with this name. */
  name?: string;
  labels?: WeeklyHoursEditorLabels;
}

const DEFAULT_ERRORS: Record<WeeklyHoursError, string> = {
  range: 'Times must fall within the day',
  order: 'Each interval must end after it starts',
  overlap: 'Intervals must not overlap',
};

function PlusGlyph() {
  return (
    <svg
      width={16}
      height={16}
      viewBox="0 0 16 16"
      fill="none"
      stroke="currentColor"
      strokeWidth={1.75}
      strokeLinecap="round"
      aria-hidden
      focusable={false}
    >
      <path d="M8 3.5v9M3.5 8h9" />
    </svg>
  );
}

function dayName(day: number, locale?: string) {
  // 2024-01-01 was a Monday.
  return new Intl.DateTimeFormat(locale, { weekday: 'long', timeZone: 'UTC' }).format(
    new Date(Date.UTC(2024, 0, day, 12)),
  );
}

function uses12h(locale?: string, hourCycle?: 'h12' | 'h23') {
  if (hourCycle) return hourCycle === 'h12';
  const resolved = new Intl.DateTimeFormat(locale, { hour: 'numeric' }).resolvedOptions().hourCycle;
  return resolved === 'h12' || resolved === 'h11';
}

function timeLabel(minutes: number, locale?: string, hourCycle?: 'h12' | 'h23') {
  if (minutes >= 1440) return uses12h(locale, hourCycle) ? 'Midnight' : '24:00';
  return new Intl.DateTimeFormat(locale, {
    hour: uses12h(locale, hourCycle) ? 'numeric' : '2-digit',
    minute: '2-digit',
    hourCycle,
    timeZone: 'UTC',
  }).format(new Date(Date.UTC(2000, 0, 1, Math.floor(minutes / 60), minutes % 60)));
}

const byDay = (value: WeeklyHoursValue) => {
  const map = new Map<number, { start: number; end: number }[]>();
  for (let d = 1; d <= 7; d++) map.set(d, []);
  for (const r of value) map.get(r.day)?.push({ start: r.start, end: r.end });
  return map;
};

const flatten = (map: Map<number, { start: number; end: number }[]>): WeeklyHoursInterval[] =>
  [1, 2, 3, 4, 5, 6, 7].flatMap((day) => (map.get(day) ?? []).map((r) => ({ day, ...r })));

/** After the last interval: an hour later and an hour long, within the day. */
function nextInterval(ranges: { start: number; end: number }[], step: number) {
  const last = [...ranges].sort((a, b) => a.start - b.start).at(-1);
  if (!last) return { start: 540, end: 1020 };
  const start = Math.min(last.end + 60, 1440 - step);
  return { start, end: Math.min(start + 60, 1440) };
}

/**
 * Edits weekly availability: each day switched on or off, one or more time
 * intervals per day, copying a day to the whole week and overlap checks. The
 * value is a plain list of `{ day, start, end }` intervals in minutes.
 */
export const WeeklyHoursEditor = forwardRef<HTMLFieldSetElement, WeeklyHoursEditorProps>(
  function WeeklyHoursEditor(
    {
      value: valueProp,
      defaultValue,
      onValueChange,
      legend = 'Weekly hours',
      timeZoneLabel,
      weekStartsOn = 1,
      locale,
      hourCycle,
      step = 15,
      defaultInterval = { start: 540, end: 1020 },
      maxIntervals = 6,
      readOnly = false,
      disabled,
      name,
      labels,
      className,
      ...rest
    },
    ref,
  ) {
    const uid = useId();
    const hc = hourCycle === '12' ? 'h12' : hourCycle === '24' ? 'h23' : undefined;
    const [inner, setInner] = useState<WeeklyHoursValue>(defaultValue ?? []);
    const value = valueProp ?? inner;
    const days = useMemo(() => byDay(value), [value]);
    const errors = useMemo(() => validateWeeklyHours(value), [value]);
    // Intervals of a day switched off, restored if it is switched back on.
    const remembered = useRef(new Map<number, { start: number; end: number }[]>());

    const commit = useCallback(
      (next: Map<number, { start: number; end: number }[]>) => {
        const flat = flatten(next);
        if (valueProp === undefined) setInner(flat);
        onValueChange?.(flat, validateWeeklyHours(flat));
      },
      [valueProp, onValueChange],
    );
    const setDay = (day: number, ranges: { start: number; end: number }[]) => {
      const next = new Map(days);
      next.set(day, ranges);
      commit(next);
    };

    const L = {
      unavailable: labels?.unavailable ?? 'Unavailable',
      start: labels?.start ?? ((d: string, n: number) => `${d} interval ${n} start`),
      end: labels?.end ?? ((d: string, n: number) => `${d} interval ${n} end`),
      add: labels?.add ?? ((d: string) => `Add ${d} interval`),
      remove: labels?.remove ?? ((d: string, n: number) => `Remove ${d} interval ${n}`),
      copyToAll: labels?.copyToAll ?? ((d: string) => `Copy to all days from ${d}`),
      copyToAllShort: labels?.copyToAllShort ?? 'Copy to all',
      errors: { ...DEFAULT_ERRORS, ...labels?.errors },
    };

    const step_ = Math.max(1, Math.min(60, step));
    const options = useMemo(() => {
      const list: number[] = [];
      for (let m = 0; m <= 1440; m += step_) list.push(m);
      return list;
    }, [step_]);
    const optionsFor = (min: number, max: number, current: number) => {
      const list = options.filter((m) => m >= min && m <= max);
      if (!list.includes(current)) list.push(current);
      return list
        .sort((a, b) => a - b)
        .map((m) => ({ value: String(m), label: timeLabel(m, locale, hc) }));
    };

    const isoStart = weekStartsOn === 0 ? 7 : weekStartsOn;
    const order = Array.from({ length: 7 }, (_, i) => ((isoStart - 1 + i) % 7) + 1);

    return (
      <fieldset
        ref={ref}
        {...rest}
        disabled={disabled}
        className={cx('mn-weekly-hours-editor', className)}
        data-readonly={readOnly || undefined}
      >
        <legend className="mn-weekly-hours-editor__legend">{legend}</legend>
        {timeZoneLabel ? <div className="mn-weekly-hours-editor__zone">{timeZoneLabel}</div> : null}
        {name ? <input type="hidden" name={name} value={JSON.stringify(flatten(days))} /> : null}
        <ul className="mn-weekly-hours-editor__days">
          {order.map((day) => {
            const ranges = days.get(day) ?? [];
            const on = ranges.length > 0;
            const label = dayName(day, locale);
            const labelId = `${uid}-day-${day}`;
            const switchId = `${uid}-switch-${day}`;
            const errorId = `${uid}-error-${day}`;
            const error = errors[day];
            return (
              <li
                key={day}
                className="mn-weekly-hours-editor__day"
                data-day={day}
                data-on={on || undefined}
                data-invalid={error ? '' : undefined}
              >
                <div className="mn-weekly-hours-editor__day-head">
                  {readOnly ? (
                    <span className="mn-weekly-hours-editor__day-name" id={labelId}>
                      {label}
                    </span>
                  ) : (
                    <>
                      <Switch
                        id={switchId}
                        size="sm"
                        checked={on}
                        aria-labelledby={labelId}
                        onCheckedChange={(checked) => {
                          if (checked) {
                            const back = remembered.current.get(day);
                            setDay(day, back?.length ? back : [{ ...defaultInterval }]);
                          } else {
                            remembered.current.set(day, ranges);
                            setDay(day, []);
                          }
                        }}
                      />
                      <label
                        className="mn-weekly-hours-editor__day-name"
                        id={labelId}
                        htmlFor={switchId}
                      >
                        {label}
                      </label>
                    </>
                  )}
                </div>
                <div
                  className="mn-weekly-hours-editor__intervals"
                  role="group"
                  aria-labelledby={labelId}
                >
                  {!on ? (
                    <p className="mn-weekly-hours-editor__off">{L.unavailable}</p>
                  ) : readOnly ? (
                    <p className="mn-weekly-hours-editor__text">
                      {ranges
                        .map(
                          (r) =>
                            `${timeLabel(r.start, locale, hc)} – ${timeLabel(r.end, locale, hc)}`,
                        )
                        .join(', ')}
                    </p>
                  ) : (
                    ranges.map((r, i) => (
                      <div key={i} className="mn-weekly-hours-editor__interval">
                        <NativeSelect
                          size="sm"
                          aria-label={L.start(label, i + 1)}
                          invalid={!!error}
                          aria-describedby={error ? errorId : undefined}
                          value={String(r.start)}
                          options={optionsFor(0, 1440 - step_, r.start)}
                          onChange={(e) =>
                            setDay(
                              day,
                              ranges.map((x, j) =>
                                j === i ? { ...x, start: Number(e.target.value) } : x,
                              ),
                            )
                          }
                        />
                        <span className="mn-weekly-hours-editor__dash" aria-hidden>
                          –
                        </span>
                        <NativeSelect
                          size="sm"
                          aria-label={L.end(label, i + 1)}
                          invalid={!!error}
                          aria-describedby={error ? errorId : undefined}
                          value={String(r.end)}
                          options={optionsFor(step_, 1440, r.end)}
                          onChange={(e) =>
                            setDay(
                              day,
                              ranges.map((x, j) =>
                                j === i ? { ...x, end: Number(e.target.value) } : x,
                              ),
                            )
                          }
                        />
                        <IconButton
                          size="sm"
                          label={L.remove(label, i + 1)}
                          icon={<CloseGlyph />}
                          onClick={() => {
                            const next = ranges.filter((_, j) => j !== i);
                            if (next.length === 0) remembered.current.set(day, ranges);
                            setDay(day, next);
                          }}
                        />
                        {i === ranges.length - 1 && ranges.length < maxIntervals ? (
                          <IconButton
                            size="sm"
                            label={L.add(label)}
                            icon={<PlusGlyph />}
                            onClick={() => setDay(day, [...ranges, nextInterval(ranges, step_)])}
                          />
                        ) : null}
                      </div>
                    ))
                  )}
                  {error ? (
                    <p className="mn-weekly-hours-editor__error" id={errorId} role="alert">
                      {L.errors[error]}
                    </p>
                  ) : null}
                </div>
                {!readOnly && on ? (
                  <Button
                    size="sm"
                    variant="ghost"
                    className="mn-weekly-hours-editor__copy"
                    leadingIcon={<CopyGlyph />}
                    aria-label={L.copyToAll(label)}
                    onClick={() => {
                      const next = new Map<number, { start: number; end: number }[]>();
                      for (let d = 1; d <= 7; d++)
                        next.set(
                          d,
                          ranges.map((x) => ({ ...x })),
                        );
                      commit(next);
                    }}
                  >
                    {L.copyToAllShort}
                  </Button>
                ) : null}
              </li>
            );
          })}
        </ul>
      </fieldset>
    );
  },
);
