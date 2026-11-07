'use client';

import {
  forwardRef,
  useId,
  useMemo,
  useRef,
  useState,
  type HTMLAttributes,
  type KeyboardEvent,
  type ReactNode,
} from 'react';
import { cx } from '../../../internal/cx.js';
import {
  formatIsoDate,
  isoToUtcDate,
  startOfMonth,
  useToday,
  type HourCycle,
  type IsoDate,
  type Weekday,
} from '../../../internal/dates/index.js';
import { Button } from '../../actions/button/index.js';
import { Alert } from '../../feedback/alert/index.js';
import { Skeleton } from '../../feedback/skeleton/index.js';
import { Field, FieldLabel } from '../../forms/form-field/index.js';
import { MiniMonth } from '../mini-month/index.js';
import { TimezoneSelect } from '../timezone-select/index.js';
import { formatInstantTime, instantDay, sameInstant, zoneName } from './slot-picker.zone.js';

/** One bookable time. */
export interface SlotPickerSlot {
  /** Start instant, ISO 8601 with `Z` or an offset (`2026-11-04T09:30:00Z`). Also the slot's value. */
  start: string;
  /** End instant, when the product wants it passed back with the slot. Not shown. */
  end?: string | undefined;
  /** Places left on a group booking. Shown under the time when set. */
  seatsLeft?: number | null | undefined;
  /** Shown but not selectable, e.g. a time that was just taken. */
  disabled?: boolean | undefined;
}

/** Every fixed string SlotPicker shows, for translation. */
export interface SlotPickerMessages {
  /** Shown before a day is chosen. */
  chooseDay: string;
  /** Shown when the chosen day has no slots. */
  empty: string;
  /** Announced while slots load. */
  loading: string;
  /** Announced when slots arrive. */
  loaded: (count: number, day: string) => string;
  /** Accessible name of the group of times. */
  times: (day: string) => string;
  /** Under a time with limited places. */
  seatsLeft: (count: number) => string;
  /** Label of the time zone picker. */
  timeZone: string;
  /** Shown when the zone cannot be changed. */
  shownIn: (zone: string) => string;
  /** Shown when the business works in another zone. */
  convertedFrom: (zone: string) => string;
  /** Warning when the current value is not one of the day's open slots. */
  notOpen: (time: string) => string;
  /** Button for `onChooseOutside`. */
  chooseOutside: string;
  /** Retry button for `onRetry`. */
  retry: string;
}

const DEFAULT_MESSAGES: SlotPickerMessages = {
  chooseDay: 'Choose a day to see available times.',
  empty: 'No times are available on this day. Try another day.',
  loading: 'Loading available times',
  loaded: (n, day) =>
    n === 0 ? `No times available on ${day}` : `${n} ${n === 1 ? 'time' : 'times'} on ${day}`,
  times: (day) => `Available times on ${day}`,
  seatsLeft: (n) => `${n} left`,
  timeZone: 'Time zone',
  shownIn: (zone) => `Times shown in ${zone}`,
  convertedFrom: (zone) => `Times are converted from ${zone}.`,
  notOpen: (time) => `${time} is not an open time on this day.`,
  chooseOutside: 'Choose another time',
  retry: 'Try again',
};

export interface SlotPickerProps extends Omit<
  HTMLAttributes<HTMLDivElement>,
  'children' | 'defaultValue' | 'onChange'
> {
  /** Slots for the selected day, in display order. Loading them is the product's job. */
  slots: readonly SlotPickerSlot[];
  /** The selected slot's `start` (controlled). */
  value?: string | null | undefined;
  /** Initial selected slot (uncontrolled). */
  defaultValue?: string | null | undefined;
  /** Called with the chosen slot's `start` and the slot, or `null` when cleared. */
  onValueChange?: ((start: string | null, slot: SlotPickerSlot | null) => void) | undefined;
  /** Pressing the selected time again clears it. Default `false`. */
  allowDeselect?: boolean | undefined;
  /** The day whose slots are shown (controlled). Load `slots` for it. */
  day?: IsoDate | null | undefined;
  /** Initial day (uncontrolled). */
  defaultDay?: IsoDate | null | undefined;
  /** Called when the person picks a day. The selected slot is not cleared for you. */
  onDayChange?: ((day: IsoDate) => void) | undefined;
  /** The month shown (any date in it; controlled). */
  month?: IsoDate | undefined;
  /** Initial month (uncontrolled). Default: the day's month, or today's. */
  defaultMonth?: IsoDate | undefined;
  /** Called when the person moves to another month, e.g. to load its bookable days. */
  onMonthChange?: ((month: IsoDate) => void) | undefined;
  /** Bookable days. When given, every other day is disabled. */
  availableDays?: readonly IsoDate[] | undefined;
  /** Disable more days, e.g. days the product knows are full. */
  isDayDisabled?: ((day: IsoDate) => boolean) | undefined;
  /** First bookable day. */
  min?: IsoDate | undefined;
  /** Last bookable day (the booking window). */
  max?: IsoDate | undefined;
  /** Today, for the month's marker and for tests. Default: the viewer's today. */
  today?: IsoDate | undefined;
  /** Week start, 0 = Sunday … 6 = Saturday. Default: from the locale. */
  weekStartsOn?: Weekday | undefined;
  /** Locale for dates and times. */
  locale?: string | undefined;
  /** Force a 12- or 24-hour clock. Default: from the locale. */
  hourCycle?: HourCycle | undefined;
  /** IANA zone the times are shown in, e.g. the visitor's zone. */
  timeZone: string;
  /** When set, a time zone picker is shown and this is called with the new zone. */
  onTimeZoneChange?: ((zone: string) => void) | undefined;
  /** Zones listed first in the picker, e.g. the business's offices. */
  pinnedTimeZones?: string[] | undefined;
  /** The zone the business works in. When it differs from `timeZone`, a note says times are converted. */
  businessTimeZone?: string | undefined;
  /** Slots are loading for the day: shows placeholders and sets `aria-busy`. */
  loading?: boolean | undefined;
  /** Loading failed. Shown as a danger alert in place of the times. */
  error?: ReactNode | undefined;
  /** Shows a retry button in the error alert. */
  onRetry?: (() => void) | undefined;
  /**
   * Offers a way out when no slot fits (staff booking outside working hours,
   * "request another time"). Shown in the empty state and in the warning when
   * `value` is not an open slot.
   */
  onChooseOutside?: (() => void) | undefined;
  /** `auto` puts the month beside the times on wide containers; `stacked` always stacks. */
  layout?: 'auto' | 'stacked' | undefined;
  /** Heading level of the selected day's title. Default 3. */
  headingLevel?: 2 | 3 | 4 | 5 | 6 | undefined;
  /** Interface text, for translation. */
  messages?: Partial<SlotPickerMessages> | undefined;
}

/**
 * Pick a day on a month calendar, then one of that day's open times, shown in
 * the visitor's time zone with an optional zone picker. Data in, choices out:
 * the product loads `slots` for the chosen `day`. Times are one tab stop;
 * arrow keys move between them.
 */
export const SlotPicker = forwardRef<HTMLDivElement, SlotPickerProps>(function SlotPicker(
  {
    slots,
    value: valueProp,
    defaultValue = null,
    onValueChange,
    allowDeselect = false,
    day: dayProp,
    defaultDay = null,
    onDayChange,
    month: monthProp,
    defaultMonth,
    onMonthChange,
    availableDays,
    isDayDisabled,
    min,
    max,
    today: todayProp,
    weekStartsOn,
    locale,
    hourCycle,
    timeZone,
    onTimeZoneChange,
    pinnedTimeZones,
    businessTimeZone,
    loading = false,
    error,
    onRetry,
    onChooseOutside,
    layout = 'auto',
    headingLevel = 3,
    messages: messagesProp,
    className,
    ...rest
  },
  ref,
) {
  const m = { ...DEFAULT_MESSAGES, ...messagesProp };
  const today = useToday(todayProp);
  const [valueState, setValueState] = useState<string | null>(defaultValue);
  const value = valueProp === undefined ? valueState : valueProp;
  const [dayState, setDayState] = useState<IsoDate | null>(defaultDay);
  const day = dayProp === undefined ? dayState : dayProp;
  const [monthState, setMonthState] = useState<IsoDate>(
    () => defaultMonth ?? startOfMonth(day ?? today),
  );
  const month = monthProp ?? monthState;
  const [focusIndex, setFocusIndex] = useState<number | null>(null);
  const groupRef = useRef<HTMLDivElement>(null);
  const headingId = useId();

  const available = useMemo(() => (availableDays ? new Set(availableDays) : null), [availableDays]);
  const dayDisabled = (d: IsoDate) =>
    (available ? !available.has(d) : false) || (isDayDisabled ? isDayDisabled(d) : false);

  const dayLabel = day
    ? formatIsoDate(day, locale, { weekday: 'long', day: 'numeric', month: 'long' })
    : '';
  const selectedIndex = value ? slots.findIndex((s) => sameInstant(s.start, value)) : -1;
  const firstEnabled = slots.findIndex((s) => !s.disabled);
  const tabIndexAt =
    selectedIndex >= 0 && !slots[selectedIndex]?.disabled ? selectedIndex : firstEnabled;
  const showTimes = !!day && !loading && !error && slots.length > 0;
  const valueOffList =
    !!day &&
    !!value &&
    !loading &&
    !error &&
    selectedIndex < 0 &&
    instantDay(value, timeZone) === day;

  const select = (slot: SlotPickerSlot | null) => {
    const start = slot?.start ?? null;
    if (valueProp === undefined) setValueState(start);
    onValueChange?.(start, slot);
  };

  const pickDay = (d: IsoDate) => {
    if (dayProp === undefined) setDayState(d);
    setFocusIndex(null);
    onDayChange?.(d);
  };

  const changeMonth = (next: IsoDate) => {
    if (monthProp === undefined) setMonthState(next);
    onMonthChange?.(next);
  };

  const focusSlot = (index: number) => {
    setFocusIndex(index);
    groupRef.current?.querySelectorAll<HTMLButtonElement>('[data-slot]')[index]?.focus();
  };

  const onSlotKeyDown = (event: KeyboardEvent<HTMLButtonElement>, index: number) => {
    const enabled = slots.map((s, i) => (s.disabled ? -1 : i)).filter((i) => i >= 0);
    const pos = enabled.indexOf(index);
    const rtl = groupRef.current?.closest('[dir="rtl"]') != null;
    let next: number | undefined;
    switch (event.key) {
      case 'ArrowRight':
        next = enabled[rtl ? pos - 1 : pos + 1];
        break;
      case 'ArrowLeft':
        next = enabled[rtl ? pos + 1 : pos - 1];
        break;
      case 'ArrowDown':
        next = enabled[pos + 1];
        break;
      case 'ArrowUp':
        next = enabled[pos - 1];
        break;
      case 'Home':
        next = enabled[0];
        break;
      case 'End':
        next = enabled[enabled.length - 1];
        break;
      default:
        return;
    }
    event.preventDefault();
    if (next !== undefined) focusSlot(next);
  };

  const Heading = `h${headingLevel}` as 'h3';
  const zoneAt = isoToUtcDate(day ?? today);
  const status = !day
    ? ''
    : loading
      ? m.loading
      : error
        ? ''
        : m.loaded(slots.filter((s) => !s.disabled).length, dayLabel);
  const activeTab =
    focusIndex !== null && slots[focusIndex] && !slots[focusIndex].disabled
      ? focusIndex
      : tabIndexAt;

  return (
    <div
      ref={ref}
      {...rest}
      className={cx('mn-slot-picker', className)}
      data-layout={layout}
      data-loading={loading || undefined}
    >
      <div className="mn-slot-picker__body">
        <div className="mn-slot-picker__calendar">
          <MiniMonth
            value={day}
            onValueChange={pickDay}
            month={month}
            onMonthChange={changeMonth}
            isDateDisabled={available || isDayDisabled ? dayDisabled : undefined}
            min={min}
            max={max}
            today={today}
            locale={locale}
            weekStartsOn={weekStartsOn}
          />
        </div>
        <div className="mn-slot-picker__times" aria-busy={loading || undefined}>
          <p className="mn-visually-hidden" role="status">
            {status}
          </p>
          {!day ? (
            <p className="mn-slot-picker__hint">{m.chooseDay}</p>
          ) : (
            <>
              <Heading id={headingId} className="mn-slot-picker__day">
                {dayLabel}
              </Heading>
              {error ? (
                <Alert
                  tone="danger"
                  actions={
                    onRetry ? (
                      <Button size="sm" onClick={onRetry}>
                        {m.retry}
                      </Button>
                    ) : undefined
                  }
                >
                  {error}
                </Alert>
              ) : loading ? (
                <div className="mn-slot-picker__grid" aria-hidden="true">
                  {Array.from({ length: 6 }, (_, i) => (
                    <Skeleton key={i} shape="rect" className="mn-slot-picker__placeholder" />
                  ))}
                </div>
              ) : slots.length === 0 ? (
                <div className="mn-slot-picker__empty">
                  <p>{m.empty}</p>
                  {onChooseOutside ? (
                    <Button size="sm" onClick={onChooseOutside}>
                      {m.chooseOutside}
                    </Button>
                  ) : null}
                </div>
              ) : null}
              {showTimes ? (
                <div
                  ref={groupRef}
                  role="group"
                  aria-label={m.times(dayLabel)}
                  className="mn-slot-picker__grid"
                >
                  {slots.map((slot, i) => {
                    const on = i === selectedIndex;
                    return (
                      <button
                        key={slot.start}
                        type="button"
                        data-slot=""
                        className="mn-slot-picker__slot"
                        aria-pressed={on}
                        data-selected={on || undefined}
                        disabled={slot.disabled}
                        tabIndex={i === activeTab ? 0 : -1}
                        onFocus={() => setFocusIndex(i)}
                        onKeyDown={(e) => onSlotKeyDown(e, i)}
                        onClick={() => select(on ? (allowDeselect ? null : slot) : slot)}
                      >
                        <span className="mn-slot-picker__time">
                          {formatInstantTime(slot.start, timeZone, locale, hourCycle)}
                        </span>
                        {slot.seatsLeft != null ? (
                          <span className="mn-slot-picker__seats">
                            {m.seatsLeft(slot.seatsLeft)}
                          </span>
                        ) : null}
                      </button>
                    );
                  })}
                </div>
              ) : null}
              {valueOffList && value ? (
                <Alert
                  tone="warning"
                  className="mn-slot-picker__off"
                  actions={
                    onChooseOutside ? (
                      <Button size="sm" onClick={onChooseOutside}>
                        {m.chooseOutside}
                      </Button>
                    ) : undefined
                  }
                >
                  {m.notOpen(formatInstantTime(value, timeZone, locale, hourCycle))}
                </Alert>
              ) : null}
            </>
          )}
        </div>
      </div>
      <div className="mn-slot-picker__zone">
        {onTimeZoneChange ? (
          <Field className="mn-slot-picker__zone-field">
            <FieldLabel>{m.timeZone}</FieldLabel>
            <TimezoneSelect
              value={timeZone}
              onValueChange={(z) => z && onTimeZoneChange(z)}
              pinned={pinnedTimeZones}
              now={zoneAt}
              locale={locale}
            />
          </Field>
        ) : (
          <p className="mn-slot-picker__note">{m.shownIn(zoneName(timeZone, zoneAt, locale))}</p>
        )}
        {businessTimeZone && businessTimeZone !== timeZone ? (
          <p className="mn-slot-picker__note">
            {m.convertedFrom(zoneName(businessTimeZone, zoneAt, locale))}
          </p>
        ) : null}
      </div>
    </div>
  );
});
