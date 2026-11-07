'use client';
import { forwardRef, useId, useState, type HTMLAttributes } from 'react';
import { cx } from '../../../internal/cx.js';
import {
  addDays,
  addMonths,
  dateFormatter,
  isoToUtcDate,
  localeWeekStart,
  startOfMonth,
  endOfMonth,
  startOfWeek,
  useToday,
  type IsoDate,
  type Weekday,
} from '../../../internal/dates/index.js';
import { SegmentedControl } from '../../forms/segmented-control/index.js';
import { Select, SelectContent, SelectItem, SelectTrigger } from '../../forms/select/index.js';
import {
  DateRangePicker,
  type DateRange,
  type DateRangePickerProps,
} from '../../search/date-range-picker/index.js';

/** A named period. Rolling periods (`7d`, `30d`…) end today and count today. */
export type PeriodPreset =
  | 'today'
  | 'yesterday'
  | '7d'
  | '14d'
  | '30d'
  | '90d'
  | '180d'
  | '365d'
  | 'this-week'
  | 'this-month'
  | 'last-month'
  | 'this-quarter'
  | 'last-quarter'
  | 'this-year'
  | 'custom';

/** The chosen period. `range` is set only for `custom`. */
export interface PeriodValue {
  preset: PeriodPreset;
  /** The dates chosen for a `custom` period. */
  range?: DateRange | undefined;
}

/** The words PeriodSelector shows. Override them to translate. */
export interface PeriodSelectorMessages {
  /** Accessible name of the control. */
  label: string;
  /** Visible name of each preset. */
  presets: Record<PeriodPreset, string>;
  /** Accessible name of the custom date-range field. */
  customRange: string;
}

export const PERIOD_SELECTOR_MESSAGES: PeriodSelectorMessages = {
  label: 'Period',
  presets: {
    today: 'Today',
    yesterday: 'Yesterday',
    '7d': 'Last 7 days',
    '14d': 'Last 14 days',
    '30d': 'Last 30 days',
    '90d': 'Last 90 days',
    '180d': 'Last 180 days',
    '365d': 'Last 12 months',
    'this-week': 'This week',
    'this-month': 'This month',
    'last-month': 'Last month',
    'this-quarter': 'This quarter',
    'last-quarter': 'Last quarter',
    'this-year': 'This year',
    custom: 'Custom',
  },
  customRange: 'Custom period',
};

const ROLLING: Partial<Record<PeriodPreset, number>> = {
  '7d': 7,
  '14d': 14,
  '30d': 30,
  '90d': 90,
  '180d': 180,
  '365d': 365,
};

function startOfQuarter(iso: IsoDate): IsoDate {
  const month = Number(iso.slice(5, 7));
  const back = (month - 1) % 3;
  return startOfMonth(addMonths(startOfMonth(iso), -back));
}

/**
 * The dates a period covers, both ends included, relative to `today`.
 * Returns `null` for a `custom` period with no range yet.
 */
export function resolvePeriod(
  value: PeriodValue,
  options: { today: IsoDate; weekStartsOn?: Weekday | undefined },
): DateRange | null {
  const { today } = options;
  const rolling = ROLLING[value.preset];
  if (rolling !== undefined) return { start: addDays(today, -(rolling - 1)), end: today };
  switch (value.preset) {
    case 'today':
      return { start: today, end: today };
    case 'yesterday': {
      const day = addDays(today, -1);
      return { start: day, end: day };
    }
    case 'this-week':
      return { start: startOfWeek(today, options.weekStartsOn ?? 1), end: today };
    case 'this-month':
      return { start: startOfMonth(today), end: today };
    case 'last-month': {
      const first = addMonths(startOfMonth(today), -1);
      return { start: first, end: endOfMonth(first) };
    }
    case 'this-quarter':
      return { start: startOfQuarter(today), end: today };
    case 'last-quarter': {
      const first = addMonths(startOfQuarter(today), -3);
      return { start: first, end: endOfMonth(addMonths(first, 2)) };
    }
    case 'this-year':
      return { start: `${today.slice(0, 4)}-01-01`, end: today };
    default:
      return value.range ?? null;
  }
}

/** Formats a resolved period for display, e.g. "1 Oct – 30 Oct 2026". */
export function formatPeriodRange(range: DateRange, locale?: string): string {
  const formatter = dateFormatter(locale, { dateStyle: 'medium' }) as Intl.DateTimeFormat & {
    formatRange?: (a: Date, b: Date) => string;
  };
  const start = isoToUtcDate(range.start);
  const end = isoToUtcDate(range.end);
  if (range.start === range.end) return formatter.format(start);
  return formatter.formatRange
    ? formatter.formatRange(start, end)
    : `${formatter.format(start)} – ${formatter.format(end)}`;
}

export interface PeriodSelectorProps extends Omit<
  HTMLAttributes<HTMLDivElement>,
  'defaultValue' | 'onChange'
> {
  /** The period (controlled). */
  value?: PeriodValue | undefined;
  /** The starting period when uncontrolled. Default: the first preset that is not `custom`. */
  defaultValue?: PeriodValue | undefined;
  /**
   * Called when a preset is picked or a custom range is completed, with the
   * resolved dates (both ends included), or `null` while a custom period has no
   * range yet.
   */
  onValueChange?: ((value: PeriodValue, range: DateRange | null) => void) | undefined;
  /** The presets offered, in order. Default: today, 7 days, 30 days, this quarter, custom. */
  presets?: readonly PeriodPreset[] | undefined;
  /**
   * `segmented` (default) shows every preset as a segment (it scrolls sideways
   * when narrow); `select` is a compact dropdown for toolbars and phones.
   */
  variant?: 'segmented' | 'select' | undefined;
  /** Control height. Default `sm`. */
  size?: 'sm' | 'md' | undefined;
  /** Shows the resolved dates after the control, e.g. "1 Oct – 30 Oct 2026". */
  showRange?: boolean | undefined;
  /** Today's date for the presets. Default: the device's. Pass it for server rendering or a workspace time zone. */
  today?: IsoDate | undefined;
  /** First day of the week for `this-week`. Default: from the locale. */
  weekStartsOn?: Weekday | undefined;
  /** BCP 47 locale for dates. */
  locale?: string | undefined;
  /** Earliest date a custom range may start. */
  min?: IsoDate | undefined;
  /** Latest date a custom range may end. Default: `today`. */
  max?: IsoDate | undefined;
  /** Longest custom range in days, e.g. 366 for reports. */
  maxDays?: number | undefined;
  /** Strings passed to the custom DateRangePicker. */
  dateRangeMessages?: DateRangePickerProps['messages'];
  disabled?: boolean | undefined;
  /** Strings, for localisation. `presets` merges per key. */
  messages?:
    | (Partial<Omit<PeriodSelectorMessages, 'presets'>> & {
        presets?: Partial<Record<PeriodPreset, string>>;
      })
    | undefined;
}

const DEFAULT_PRESETS: readonly PeriodPreset[] = ['today', '7d', '30d', 'this-quarter', 'custom'];

/**
 * Picks the period a dashboard or report covers: today, the last 7 or 30
 * days, this quarter and other presets, or a custom range through
 * DateRangePicker. Reports the choice with its resolved dates.
 */
export const PeriodSelector = forwardRef<HTMLDivElement, PeriodSelectorProps>(
  function PeriodSelector(
    {
      value: valueProp,
      defaultValue,
      onValueChange,
      presets = DEFAULT_PRESETS,
      variant = 'segmented',
      size = 'sm',
      showRange = false,
      today: todayProp,
      weekStartsOn,
      locale,
      min,
      max,
      maxDays,
      dateRangeMessages,
      disabled,
      messages: messagesProp,
      className,
      'aria-label': ariaLabel,
      ...rest
    },
    ref,
  ) {
    const messages = {
      ...PERIOD_SELECTOR_MESSAGES,
      ...messagesProp,
      presets: { ...PERIOD_SELECTOR_MESSAGES.presets, ...messagesProp?.presets },
    };
    const today = useToday(todayProp);
    const rangeId = useId();
    const firstPreset = presets.find((p) => p !== 'custom') ?? 'custom';
    const [internal, setInternal] = useState<PeriodValue>(defaultValue ?? { preset: firstPreset });
    const value = valueProp ?? internal;
    const weekStart = weekStartsOn ?? localeWeekStart(locale);
    const range = resolvePeriod(value, { today, weekStartsOn: weekStart });
    const label = ariaLabel ?? messages.label;
    const showDates = showRange && range !== null && value.preset !== 'custom';

    const change = (next: PeriodValue) => {
      if (valueProp === undefined) setInternal(next);
      onValueChange?.(next, resolvePeriod(next, { today, weekStartsOn: weekStart }));
    };

    const pick = (preset: PeriodPreset) => {
      if (preset === value.preset) return;
      change(
        preset === 'custom' ? { preset, range: value.range ?? range ?? undefined } : { preset },
      );
    };

    const options = presets.map((preset) => ({ value: preset, label: messages.presets[preset] }));
    const sizeProp = { size } as const;

    return (
      <div
        ref={ref}
        role="group"
        aria-label={label}
        {...rest}
        className={cx('mn-period-selector', className)}
        data-preset={value.preset}
        data-variant={variant}
      >
        {variant === 'select' ? (
          <Select
            value={value.preset}
            onValueChange={(next) => pick(next as PeriodPreset)}
            {...(disabled !== undefined && { disabled })}
          >
            <SelectTrigger
              {...sizeProp}
              className="mn-period-selector__select"
              aria-label={label}
              aria-describedby={showDates ? rangeId : undefined}
            />
            <SelectContent>
              {options.map((o) => (
                <SelectItem key={o.value} value={o.value}>
                  {o.label}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        ) : (
          <SegmentedControl<PeriodPreset>
            className="mn-period-selector__segments"
            aria-label={label}
            {...sizeProp}
            options={options}
            value={value.preset}
            onValueChange={pick}
            {...(disabled !== undefined && { disabled })}
          />
        )}
        {value.preset === 'custom' ? (
          <DateRangePicker
            className="mn-period-selector__custom"
            aria-label={messages.customRange}
            size={size}
            value={value.range ?? null}
            onValueChange={(next) => change({ preset: 'custom', range: next ?? undefined })}
            max={max ?? today}
            today={today}
            clearable={false}
            weekStartsOn={weekStart}
            {...(min !== undefined && { min })}
            {...(maxDays !== undefined && { maxDays })}
            {...(locale !== undefined && { locale })}
            {...(disabled !== undefined && { disabled })}
            {...(dateRangeMessages !== undefined && { messages: dateRangeMessages })}
          />
        ) : null}
        {showDates && range ? (
          <span id={rangeId} className="mn-period-selector__range">
            {formatPeriodRange(range, locale)}
          </span>
        ) : null}
      </div>
    );
  },
);
