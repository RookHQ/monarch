'use client';

import { forwardRef, useId, useState, type HTMLAttributes, type ReactNode } from 'react';
import { cx } from '../../../internal/cx.js';
import { CloseGlyph, PlusGlyph } from '../../../internal/icons.js';
import {
  formatTime,
  type HourCycle,
  type IsoDate,
  type Weekday,
} from '../../../internal/dates/index.js';
import { Button, IconButton } from '../../actions/button/index.js';
import { Checkbox } from '../../forms/checkbox/index.js';
import {
  Field,
  FieldDescription,
  FieldError,
  FieldLabel,
  NativeSelect,
} from '../../forms/form-field/index.js';
import { Input } from '../../forms/text-field/index.js';
import { DatePicker } from '../date-picker/index.js';
import { TimezoneSelect } from '../timezone-select/index.js';
import {
  WeeklyHoursEditor,
  validateWeeklyHours,
  type WeeklyHoursError,
  type WeeklyHoursInterval,
} from '../weekly-hours-editor/index.js';

/** A date that differs from the weekly hours: a holiday (closed) or special hours. */
export interface BusinessHoursException {
  /** The date, `YYYY-MM-DD`. Empty while the person is still choosing. */
  date: IsoDate | '';
  /** What it is, e.g. "New Year's Day". */
  name: string;
  /** Open intervals that day, minutes from midnight. Empty or missing: closed all day. */
  intervals?: readonly { start: number; end: number }[] | undefined;
}

/** The whole value: plain, serialisable data. */
export interface BusinessHoursValue {
  /** Calendar name, e.g. "Support hours". */
  name?: string | undefined;
  /** IANA zone the hours are in. */
  timeZone: string;
  /** Weekly hours, as WeeklyHoursEditor takes them (ISO weekday 1-7, minutes). */
  hours: readonly WeeklyHoursInterval[];
  /** Holidays and special-hours dates. */
  exceptions: readonly BusinessHoursException[];
  /** The workspace default calendar. */
  isDefault?: boolean | undefined;
  /** First day these hours apply. */
  effectiveFrom?: IsoDate | null | undefined;
  /** Last day these hours apply. */
  effectiveTo?: IsoDate | null | undefined;
}

/** Why an exception row is invalid. */
export type BusinessHoursExceptionError = 'date' | 'name' | 'duplicate' | 'hours';

/** Problems in a value; an empty object means it is valid. */
export interface BusinessHoursErrors {
  name?: 'required';
  /** Weekly hours problems by ISO weekday, from `validateWeeklyHours`. */
  hours?: Partial<Record<number, WeeklyHoursError>>;
  /** No open interval in the week. */
  open?: 'none';
  /** Exception problems by row index. */
  exceptions?: Partial<Record<number, BusinessHoursExceptionError>>;
  /** `effectiveTo` is before `effectiveFrom`. */
  effective?: 'order';
}

/** What `validateBusinessHours` checks beyond the always-on rules. */
export interface ValidateBusinessHoursOptions {
  /** A name is required. Default `false`. */
  requireName?: boolean | undefined;
  /** At least one open interval is required (an SLA clock must be able to run). Default `false`. */
  requireOpen?: boolean | undefined;
}

/**
 * Checks a business-hours value: weekly hours (range, order, overlap),
 * exceptions (date, name, duplicates, special hours that end before they
 * start), effective dates in order, and optionally a name and at least one
 * open interval. Returns only the problems found.
 */
export function validateBusinessHours(
  value: BusinessHoursValue,
  options: ValidateBusinessHoursOptions = {},
): BusinessHoursErrors {
  const out: BusinessHoursErrors = {};
  if (options.requireName && !value.name?.trim()) out.name = 'required';
  const hours = validateWeeklyHours(value.hours);
  if (Object.keys(hours).length) out.hours = hours;
  if (options.requireOpen && !value.hours.some((h) => h.end > h.start)) out.open = 'none';
  const ex: Partial<Record<number, BusinessHoursExceptionError>> = {};
  value.exceptions.forEach((e, i) => {
    if (!e.date) ex[i] = 'date';
    else if (!e.name.trim()) ex[i] = 'name';
    else if (value.exceptions.findIndex((x) => x.date === e.date) !== i) ex[i] = 'duplicate';
    else if (e.intervals?.some((r) => r.end <= r.start || r.start < 0 || r.end > 1440))
      ex[i] = 'hours';
  });
  if (Object.keys(ex).length) out.exceptions = ex;
  if (value.effectiveFrom && value.effectiveTo && value.effectiveTo < value.effectiveFrom)
    out.effective = 'order';
  return out;
}

/** Every fixed string BusinessHoursEditor shows, for translation. */
export interface BusinessHoursEditorMessages {
  name: string;
  namePlaceholder: string;
  nameRequired: string;
  timeZone: string;
  timeZoneDescription: string;
  weeklyHours: string;
  noOpenHours: string;
  effectiveFrom: string;
  effectiveTo: string;
  effectiveOrder: string;
  exceptions: string;
  exceptionsDescription: string;
  noExceptions: string;
  addException: string;
  exceptionDate: (n: number) => string;
  exceptionName: (n: number) => string;
  exceptionNamePlaceholder: string;
  exceptionKind: (n: number) => string;
  closed: string;
  specialHours: string;
  exceptionStart: (n: number) => string;
  exceptionEnd: (n: number) => string;
  removeException: (n: number, name: string) => string;
  exceptionErrors: Record<BusinessHoursExceptionError, string>;
  isDefault: string;
  isDefaultDescription: string;
  /** Label of the end-of-day closing time. */
  endOfDay: string;
}

const DEFAULT_MESSAGES: BusinessHoursEditorMessages = {
  name: 'Name',
  namePlaceholder: 'Office hours',
  nameRequired: 'Enter a name.',
  timeZone: 'Time zone',
  timeZoneDescription: 'Hours below are in this zone, including daylight-saving changes.',
  weeklyHours: 'Weekly hours',
  noOpenHours: 'Open at least one interval.',
  effectiveFrom: 'Effective from',
  effectiveTo: 'Effective until',
  effectiveOrder: 'The end date must be on or after the start date.',
  exceptions: 'Holidays and exceptions',
  exceptionsDescription:
    'Dates that replace the weekly hours: closed all day, or open at other times.',
  noExceptions: 'No holidays or exceptions.',
  addException: 'Add date',
  exceptionDate: (n) => `Date ${n}`,
  exceptionName: (n) => `Name of date ${n}`,
  exceptionNamePlaceholder: 'Public holiday',
  exceptionKind: (n) => `Hours on date ${n}`,
  closed: 'Closed all day',
  specialHours: 'Open at other times',
  exceptionStart: (n) => `Date ${n} opens`,
  exceptionEnd: (n) => `Date ${n} closes`,
  removeException: (n, name) => (name ? `Remove ${name}` : `Remove date ${n}`),
  exceptionErrors: {
    date: 'Choose a date.',
    name: 'Give the date a name.',
    duplicate: 'This date is listed twice.',
    hours: 'The closing time must be after the opening time.',
  },
  isDefault: 'Default',
  isDefaultDescription: 'Used wherever no other hours are chosen.',
  endOfDay: 'End of day',
};

export interface BusinessHoursEditorProps extends Omit<
  HTMLAttributes<HTMLDivElement>,
  'defaultValue' | 'onChange'
> {
  /** Controlled value. */
  value?: BusinessHoursValue | undefined;
  /** Uncontrolled starting value. */
  defaultValue?: BusinessHoursValue | undefined;
  /** Called with every change and the current `validateBusinessHours` result. */
  onValueChange?: ((value: BusinessHoursValue, errors: BusinessHoursErrors) => void) | undefined;
  /** Show the name field. Default `true`. */
  showName?: boolean | undefined;
  /** Show the "Default" checkbox. Default `false`. */
  showDefault?: boolean | undefined;
  /** Show "Effective from/until" dates. Default `false`. */
  showEffectiveDates?: boolean | undefined;
  /** Exceptions can have special hours, not only "closed". Default `true`. */
  allowSpecialHours?: boolean | undefined;
  /** Validation options, also used for the inline errors. */
  validation?: ValidateBusinessHoursOptions | undefined;
  /** Show errors now (e.g. after a save attempt). Default: each field's error shows once it is touched. */
  showErrors?: boolean | undefined;
  /** Zones listed first in the time zone picker. */
  pinnedTimeZones?: string[] | undefined;
  /** First weekday row: 0 = Sunday … 6 = Saturday. Default 1. */
  weekStartsOn?: Weekday | undefined;
  /** Locale for day names, dates and times. */
  locale?: string | undefined;
  /** Force a 12- or 24-hour clock. */
  hourCycle?: HourCycle | undefined;
  /** Minutes between time options. Default 15. */
  step?: number | undefined;
  /** Show everything as text. */
  readOnly?: boolean | undefined;
  /** Disable every control. */
  disabled?: boolean | undefined;
  /** Interface text, for translation. */
  messages?: Partial<BusinessHoursEditorMessages> | undefined;
  /** Extra content at the end, e.g. product-specific settings. */
  children?: ReactNode | undefined;
}

const EMPTY: BusinessHoursValue = { timeZone: 'UTC', hours: [], exceptions: [] };

/**
 * Edits a business-hours calendar: name, time zone, weekly hours
 * (WeeklyHoursEditor), optional effective dates, and holidays or
 * special-hours dates, with `validateBusinessHours` exported for saving.
 * The value is plain data; the product saves it.
 */
export const BusinessHoursEditor = forwardRef<HTMLDivElement, BusinessHoursEditorProps>(
  function BusinessHoursEditor(
    {
      value: valueProp,
      defaultValue = EMPTY,
      onValueChange,
      showName = true,
      showDefault = false,
      showEffectiveDates = false,
      allowSpecialHours = true,
      validation,
      showErrors = false,
      pinnedTimeZones,
      weekStartsOn = 1,
      locale,
      hourCycle,
      step = 15,
      readOnly = false,
      disabled = false,
      messages: messagesProp,
      children,
      className,
      ...rest
    },
    ref,
  ) {
    const m = {
      ...DEFAULT_MESSAGES,
      ...messagesProp,
      exceptionErrors: { ...DEFAULT_MESSAGES.exceptionErrors, ...messagesProp?.exceptionErrors },
    };
    const [state, setState] = useState<BusinessHoursValue>(defaultValue);
    const value = valueProp ?? state;
    const errors = validateBusinessHours(value, validation);
    const [touched, setTouched] = useState<Set<string>>(() => new Set());
    const shows = (key: string) => showErrors || touched.has(key);
    const exceptionsId = useId();

    const update = (patch: Partial<BusinessHoursValue>, touch?: string) => {
      const next = { ...value, ...patch };
      if (valueProp === undefined) setState(next);
      if (touch) setTouched((t) => (t.has(touch) ? t : new Set(t).add(touch)));
      onValueChange?.(next, validateBusinessHours(next, validation));
    };

    const setException = (i: number, patch: Partial<BusinessHoursException>) =>
      update(
        { exceptions: value.exceptions.map((e, j) => (j === i ? { ...e, ...patch } : e)) },
        `ex${i}`,
      );

    const timeOptions = (from: number, to: number) => {
      const out: { value: string; label: string }[] = [];
      for (let t = from; t <= to; t += step)
        out.push({
          value: String(t),
          label: t >= 1440 ? m.endOfDay : formatTime(t, locale, hourCycle),
        });
      return out;
    };

    const nameError = errors.name && shows('name') ? m.nameRequired : null;
    const effectiveError = errors.effective ? m.effectiveOrder : null;
    const openError = errors.open && shows('hours') ? m.noOpenHours : null;

    return (
      <div
        ref={ref}
        {...rest}
        className={cx('mn-business-hours-editor', className)}
        data-readonly={readOnly || undefined}
        aria-disabled={disabled || undefined}
      >
        {showName || showEffectiveDates ? (
          <div className="mn-business-hours-editor__grid">
            {showName ? (
              <Field
                invalid={!!nameError}
                required={validation?.requireName ?? false}
                disabled={disabled}
              >
                <FieldLabel>{m.name}</FieldLabel>
                <Input
                  value={value.name ?? ''}
                  placeholder={m.namePlaceholder}
                  maxLength={100}
                  readOnly={readOnly}
                  onChange={(e) => update({ name: e.target.value }, 'name')}
                  onBlur={() => setTouched((t) => new Set(t).add('name'))}
                />
                <FieldError>{nameError}</FieldError>
              </Field>
            ) : null}
            {showEffectiveDates ? (
              <>
                <Field disabled={disabled}>
                  <FieldLabel>{m.effectiveFrom}</FieldLabel>
                  <DatePicker
                    value={value.effectiveFrom ?? null}
                    onValueChange={(v) => update({ effectiveFrom: v }, 'effective')}
                    locale={locale}
                    readOnly={readOnly}
                    disabled={disabled}
                    clearable
                  />
                </Field>
                <Field invalid={!!effectiveError} disabled={disabled}>
                  <FieldLabel>{m.effectiveTo}</FieldLabel>
                  <DatePicker
                    value={value.effectiveTo ?? null}
                    onValueChange={(v) => update({ effectiveTo: v }, 'effective')}
                    min={value.effectiveFrom ?? undefined}
                    locale={locale}
                    readOnly={readOnly}
                    disabled={disabled}
                    clearable
                    showErrors={false}
                  />
                  <FieldError>{effectiveError}</FieldError>
                </Field>
              </>
            ) : null}
          </div>
        ) : null}

        <Field disabled={disabled} className="mn-business-hours-editor__zone">
          <FieldLabel>{m.timeZone}</FieldLabel>
          <FieldDescription>{m.timeZoneDescription}</FieldDescription>
          <TimezoneSelect
            value={value.timeZone}
            onValueChange={(z) => z && update({ timeZone: z })}
            pinned={pinnedTimeZones}
            locale={locale}
            readOnly={readOnly}
            disabled={disabled}
          />
        </Field>

        <div className="mn-business-hours-editor__hours">
          <WeeklyHoursEditor
            legend={m.weeklyHours}
            value={value.hours}
            onValueChange={(hours) => update({ hours }, 'hours')}
            timeZoneLabel={value.timeZone}
            weekStartsOn={weekStartsOn}
            step={step}
            readOnly={readOnly}
            disabled={disabled}
            {...(locale ? { locale } : {})}
            {...(hourCycle ? { hourCycle } : {})}
          />
          {openError ? (
            <p className="mn-business-hours-editor__error" role="alert">
              {openError}
            </p>
          ) : null}
        </div>

        <fieldset
          className="mn-business-hours-editor__exceptions"
          disabled={disabled}
          aria-describedby={`${exceptionsId}-d`}
        >
          <legend className="mn-business-hours-editor__legend">{m.exceptions}</legend>
          <p id={`${exceptionsId}-d`} className="mn-business-hours-editor__hint">
            {m.exceptionsDescription}
          </p>
          {value.exceptions.length === 0 ? (
            <p className="mn-business-hours-editor__empty">{m.noExceptions}</p>
          ) : (
            <ul className="mn-business-hours-editor__list">
              {value.exceptions.map((e, i) => {
                const n = i + 1;
                const err = errors.exceptions?.[i];
                const shown = err && shows(`ex${i}`) ? m.exceptionErrors[err] : null;
                const errId = `${exceptionsId}-e${i}`;
                const special = (e.intervals?.length ?? 0) > 0;
                const iv = e.intervals?.[0];
                return (
                  <li
                    key={i}
                    className="mn-business-hours-editor__exception"
                    data-invalid={shown ? '' : undefined}
                  >
                    <div className="mn-business-hours-editor__exception-fields">
                      <div className="mn-business-hours-editor__date">
                        <DatePicker
                          aria-label={m.exceptionDate(n)}
                          value={e.date || null}
                          onValueChange={(v) => setException(i, { date: v ?? '' })}
                          locale={locale}
                          readOnly={readOnly}
                          disabled={disabled}
                          size="sm"
                          invalid={!!shown && (err === 'date' || err === 'duplicate')}
                          aria-describedby={shown ? errId : undefined}
                        />
                      </div>
                      <Input
                        size="sm"
                        aria-label={m.exceptionName(n)}
                        className="mn-business-hours-editor__name"
                        value={e.name}
                        placeholder={m.exceptionNamePlaceholder}
                        maxLength={100}
                        readOnly={readOnly}
                        aria-invalid={(!!shown && err === 'name') || undefined}
                        aria-describedby={shown ? errId : undefined}
                        onChange={(ev) => setException(i, { name: ev.target.value })}
                      />
                      {allowSpecialHours ? (
                        <NativeSelect
                          size="sm"
                          aria-label={m.exceptionKind(n)}
                          value={special ? 'open' : 'closed'}
                          disabled={readOnly || disabled}
                          options={[
                            { value: 'closed', label: m.closed },
                            { value: 'open', label: m.specialHours },
                          ]}
                          onChange={(ev) =>
                            setException(i, {
                              intervals:
                                ev.target.value === 'open' ? [{ start: 600, end: 960 }] : [],
                            })
                          }
                        />
                      ) : null}
                      {!readOnly ? (
                        <IconButton
                          size="sm"
                          variant="ghost"
                          label={m.removeException(n, e.name.trim())}
                          icon={<CloseGlyph />}
                          onClick={() =>
                            update(
                              { exceptions: value.exceptions.filter((_, j) => j !== i) },
                              'exceptions',
                            )
                          }
                        />
                      ) : null}
                    </div>
                    {special && iv ? (
                      <div className="mn-business-hours-editor__special">
                        <NativeSelect
                          size="sm"
                          aria-label={m.exceptionStart(n)}
                          value={String(iv.start)}
                          disabled={readOnly || disabled}
                          invalid={!!shown && err === 'hours'}
                          options={timeOptions(0, 1440 - step)}
                          onChange={(ev) =>
                            setException(i, {
                              intervals: [{ start: Number(ev.target.value), end: iv.end }],
                            })
                          }
                        />
                        <span aria-hidden="true">–</span>
                        <NativeSelect
                          size="sm"
                          aria-label={m.exceptionEnd(n)}
                          value={String(iv.end)}
                          disabled={readOnly || disabled}
                          invalid={!!shown && err === 'hours'}
                          options={timeOptions(step, 1440)}
                          onChange={(ev) =>
                            setException(i, {
                              intervals: [{ start: iv.start, end: Number(ev.target.value) }],
                            })
                          }
                        />
                      </div>
                    ) : null}
                    {shown ? (
                      <p id={errId} className="mn-business-hours-editor__error" role="alert">
                        {shown}
                      </p>
                    ) : null}
                  </li>
                );
              })}
            </ul>
          )}
          {!readOnly ? (
            <Button
              size="sm"
              leadingIcon={<PlusGlyph />}
              onClick={() => update({ exceptions: [...value.exceptions, { date: '', name: '' }] })}
            >
              {m.addException}
            </Button>
          ) : null}
        </fieldset>

        {showDefault ? (
          <Checkbox
            label={m.isDefault}
            description={m.isDefaultDescription}
            checked={value.isDefault ?? false}
            disabled={readOnly || disabled}
            onCheckedChange={(v) => update({ isDefault: v === true })}
          />
        ) : null}
        {children}
      </div>
    );
  },
);
