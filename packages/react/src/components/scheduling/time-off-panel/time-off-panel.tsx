'use client';

import {
  forwardRef,
  useEffect,
  useId,
  useRef,
  useState,
  type FormEvent,
  type HTMLAttributes,
  type ReactNode,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { isThenable } from '../../../internal/dialog-helpers.js';
import { PlusGlyph } from '../../../internal/icons.js';
import {
  formatIsoDate,
  todayIso,
  type HourCycle,
  type IsoDate,
  type IsoDateTime,
} from '../../../internal/dates/index.js';
import { Button, IconButton } from '../../actions/button/index.js';
import { Badge } from '../../data-display/badge/index.js';
import { Alert } from '../../feedback/alert/index.js';
import { EmptyState } from '../../feedback/empty-state/index.js';
import { Skeleton } from '../../feedback/skeleton/index.js';
import { Checkbox } from '../../forms/checkbox/index.js';
import { Field, FieldDescription, FieldLabel, NativeSelect } from '../../forms/form-field/index.js';
import { Textarea } from '../../forms/text-field/index.js';
import { ConfirmDialog } from '../../overlays/confirm-dialog/index.js';
import {
  Dialog,
  DialogBody,
  DialogClose,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '../../overlays/dialog/index.js';
import { DatePicker } from '../date-picker/index.js';
import { DateTimePicker } from '../date-time-picker/index.js';
import { wallTimeToInstant } from './time-off-panel.zone.js';

/** One period of time off. */
export interface TimeOffEntry {
  id: string;
  /** Whose time off, or `null` for a closure that applies to everyone. */
  personId: string | null;
  /** Name shown for the person. */
  personName?: string | undefined;
  /** `true`: `start`/`end` are days (`YYYY-MM-DD`, end inclusive). `false`: instants with an offset. */
  allDay: boolean;
  start: string;
  end: string;
  /** The kind, e.g. "Annual leave". Shown as a badge. */
  type?: string | undefined;
  /** A note in the person’s own words, e.g. "Conference". */
  reason?: string | undefined;
  /** Shown as a warning badge, e.g. "Pending". */
  status?: string | undefined;
}

/** A choice in the "Who" select. */
export interface TimeOffPerson {
  value: string;
  label: string;
}

/** What the add form produces. */
export interface TimeOffDraft {
  /** The chosen person, or `null` for a closure. */
  personId: string | null;
  allDay: boolean;
  /** First day (all day) or start instant in UTC. */
  start: string;
  /** Last day, inclusive (all day) or end instant in UTC. */
  end: string;
  /** The chosen type's value, when `types` is set. */
  type?: string;
  /** Trimmed reason, when given. */
  reason?: string;
}

/** Every fixed string TimeOffPanel shows, for translation. */
export interface TimeOffPanelMessages {
  title: string;
  description: string;
  add: string;
  includePast: string;
  closure: string;
  wholeTeam: string;
  past: string;
  allDay: string;
  /** Label of the all-day checkbox. */
  allDayLabel: string;
  remove: (who: string, when: string) => string;
  removeTitle: string;
  removeDescription: string;
  removeConfirm: string;
  emptyTitle: string;
  emptyDescription: string;
  retry: string;
  dialogTitle: string;
  dialogDescription: string;
  who: string;
  type: string;
  closureNotice: string;
  from: string;
  until: string;
  start: string;
  end: string;
  reason: string;
  reasonDescription: string;
  cancel: string;
  save: string;
  endBeforeStart: string;
  incomplete: string;
  failed: string;
}

const DEFAULT_MESSAGES: TimeOffPanelMessages = {
  title: 'Time off',
  description: 'Holidays, sickness, training or closures. Nobody is booked in these periods.',
  add: 'Add time off',
  includePast: 'Include past',
  closure: 'Closure',
  wholeTeam: 'Everyone',
  past: 'Past',
  allDay: 'all day',
  allDayLabel: 'All day',
  remove: (who, when) => `Remove time off for ${who}, ${when}`,
  removeTitle: 'Remove this time off?',
  removeDescription: 'Those times become available again if they fall within working hours.',
  removeConfirm: 'Remove',
  emptyTitle: 'No time off planned',
  emptyDescription: 'Add holidays or closures so nobody is offered times when no one is available.',
  retry: 'Try again',
  dialogTitle: 'Add time off',
  dialogDescription: 'Dates and times are in the time zone shown.',
  who: 'Who',
  type: 'Type',
  closureNotice: 'A closure blocks bookings for everyone.',
  from: 'From',
  until: 'Until (inclusive)',
  start: 'Start',
  end: 'End',
  reason: 'Reason (optional)',
  reasonDescription: 'Visible to your team.',
  cancel: 'Cancel',
  save: 'Add time off',
  endBeforeStart: 'The end must be after the start.',
  incomplete: 'Fill in the dates.',
  failed: 'That did not work. Try again.',
};

const CLOSURE = '__closure';

export interface TimeOffPanelProps extends Omit<HTMLAttributes<HTMLElement>, 'children'> {
  /** The periods to list, in display order. */
  entries: readonly TimeOffEntry[];
  /** IANA zone timed entries are shown and entered in. */
  timeZone: string;
  /** Locale for dates and times. */
  locale?: string | undefined;
  /** Force a 12- or 24-hour clock. */
  hourCycle?: HourCycle | undefined;
  /** "Now", to mark and hide past entries. Default: the time of render. */
  now?: Date | undefined;
  /** People the add form offers. One person hides the "Who" select. */
  people?: readonly TimeOffPerson[] | undefined;
  /** Adds a "closure" choice (everyone) to "Who". */
  allowClosure?: boolean | undefined;
  /** Types the add form offers, e.g. leave types. */
  types?: readonly TimeOffPerson[] | undefined;
  /** Adds the time off. Shows the add button. Return a promise to keep the form pending; a rejection shows in the form. */
  onAdd?: ((draft: TimeOffDraft) => unknown) | undefined;
  /** Removes an entry after a danger confirmation. Return a promise to keep the confirmation pending. */
  onRemove?: ((entry: TimeOffEntry) => unknown) | undefined;
  /** Which entries may be removed. Default: all, when `onRemove` is set. */
  canRemove?: ((entry: TimeOffEntry) => boolean) | undefined;
  /** Show past entries (controlled). */
  showPast?: boolean | undefined;
  /** Initial "Include past" (uncontrolled). Default `false`. */
  defaultShowPast?: boolean | undefined;
  /** Called when "Include past" changes; load past entries here if the product pages them. */
  onShowPastChange?: ((showPast: boolean) => void) | undefined;
  /** Shows placeholders instead of the list. */
  loading?: boolean | undefined;
  /** Shows a danger alert instead of the list. */
  error?: ReactNode | undefined;
  /** Retry button in the error alert. */
  onRetry?: (() => void) | undefined;
  /** A notice above the list, e.g. "2 appointments fall in this period". */
  notice?: ReactNode | undefined;
  /** Heading level of the title. Default 2. */
  headingLevel?: 2 | 3 | 4 | undefined;
  /** Interface text, for translation. */
  messages?: Partial<TimeOffPanelMessages> | undefined;
}

function entryLabel(
  e: TimeOffEntry,
  zone: string,
  locale: string | undefined,
  hourCycle: HourCycle | undefined,
  allDayText: string,
): string {
  if (e.allDay) {
    const opts: Intl.DateTimeFormatOptions = {
      weekday: 'short',
      day: 'numeric',
      month: 'short',
      year: 'numeric',
    };
    const a = formatIsoDate(e.start, locale, opts);
    const b = formatIsoDate(e.end, locale, opts);
    return a === b ? `${a} (${allDayText})` : `${a} – ${b} (${allDayText})`;
  }
  const s = new Date(e.start);
  const t = new Date(e.end);
  const dateOpts: Intl.DateTimeFormatOptions = {
    weekday: 'short',
    day: 'numeric',
    month: 'short',
    year: 'numeric',
  };
  const timeOpts: Intl.DateTimeFormatOptions = {
    hour: hourCycle === '24' ? '2-digit' : 'numeric',
    minute: '2-digit',
  };
  if (hourCycle) timeOpts.hourCycle = hourCycle === '12' ? 'h12' : 'h23';
  let dateF: Intl.DateTimeFormat;
  let timeF: Intl.DateTimeFormat;
  try {
    dateF = new Intl.DateTimeFormat(locale, { ...dateOpts, timeZone: zone });
    timeF = new Intl.DateTimeFormat(locale, { ...timeOpts, timeZone: zone });
  } catch {
    dateF = new Intl.DateTimeFormat(locale, { ...dateOpts, timeZone: 'UTC' });
    timeF = new Intl.DateTimeFormat(locale, { ...timeOpts, timeZone: 'UTC' });
  }
  const ds = dateF.format(s);
  const de = dateF.format(t);
  return ds === de
    ? `${ds}, ${timeF.format(s)} – ${timeF.format(t)}`
    : `${ds}, ${timeF.format(s)} – ${de}, ${timeF.format(t)}`;
}

function safeToday(now: Date, zone: string): IsoDate {
  try {
    return todayIso({ now, timeZone: zone });
  } catch {
    return todayIso({ now, timeZone: 'UTC' });
  }
}

function isPast(e: TimeOffEntry, now: Date, zone: string): boolean {
  if (!e.allDay) return new Date(e.end).getTime() < now.getTime();
  return e.end < safeToday(now, zone);
}

/**
 * Lists time off (holidays, sickness, closures) and adds or removes it: an
 * "Include past" switch, an add form in a Dialog (who, type, all day or
 * timed, dates, reason, validation) and a danger confirmation to remove.
 * Data in, callbacks out; the product saves.
 */
export const TimeOffPanel = forwardRef<HTMLElement, TimeOffPanelProps>(function TimeOffPanel(
  {
    entries,
    timeZone,
    locale,
    hourCycle,
    now: nowProp,
    people = [],
    allowClosure = false,
    types,
    onAdd,
    onRemove,
    canRemove,
    showPast: showPastProp,
    defaultShowPast = false,
    onShowPastChange,
    loading = false,
    error,
    onRetry,
    notice,
    headingLevel = 2,
    messages: messagesProp,
    className,
    ...rest
  },
  ref,
) {
  const m = { ...DEFAULT_MESSAGES, ...messagesProp };
  const [nowState] = useState(() => new Date());
  const now = nowProp ?? nowState;
  const [showPastState, setShowPastState] = useState(defaultShowPast);
  const showPast = showPastProp ?? showPastState;
  const [toRemove, setToRemove] = useState<TimeOffEntry | null>(null);
  const [adding, setAdding] = useState(false);
  const titleId = useId();
  const H = `h${headingLevel}` as 'h2';

  const visible = showPast ? entries : entries.filter((e) => !isPast(e, now, timeZone));
  const who = (e: TimeOffEntry) => (e.personId === null ? m.wholeTeam : (e.personName ?? ''));

  let list: ReactNode;
  if (loading) {
    list = (
      <div className="mn-time-off-panel__placeholder" aria-hidden="true">
        {[0, 1, 2].map((i) => (
          <Skeleton key={i} shape="rect" />
        ))}
      </div>
    );
  } else if (error) {
    list = (
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
    );
  } else if (visible.length === 0) {
    list = (
      <EmptyState
        size="compact"
        title={m.emptyTitle}
        description={m.emptyDescription}
        headingLevel={`h${Math.min(6, headingLevel + 1)}` as 'h3'}
      />
    );
  } else {
    list = (
      <ul className="mn-time-off-panel__list">
        {visible.map((e) => {
          const when = entryLabel(e, timeZone, locale, hourCycle, m.allDay);
          const past = isPast(e, now, timeZone);
          const removable = !!onRemove && (canRemove ? canRemove(e) : true);
          return (
            <li key={e.id} className="mn-time-off-panel__item" data-past={past || undefined}>
              <div className="mn-time-off-panel__text">
                <div className="mn-time-off-panel__line">
                  <span className="mn-time-off-panel__who">{who(e)}</span>
                  {e.personId === null ? <Badge tone="warning">{m.closure}</Badge> : null}
                  {e.type ? <Badge tone="neutral">{e.type}</Badge> : null}
                  {e.status ? <Badge tone="info">{e.status}</Badge> : null}
                  {past ? (
                    <Badge tone="neutral" variant="outline">
                      {m.past}
                    </Badge>
                  ) : null}
                </div>
                <p className="mn-time-off-panel__when">
                  {when}
                  {e.reason ? ` · ${e.reason}` : null}
                </p>
              </div>
              {removable ? (
                <IconButton
                  size="sm"
                  variant="ghost"
                  label={m.remove(who(e), when)}
                  icon={<RemoveGlyph />}
                  onClick={() => setToRemove(e)}
                />
              ) : null}
            </li>
          );
        })}
      </ul>
    );
  }

  return (
    <section
      ref={ref}
      aria-labelledby={titleId}
      {...rest}
      className={cx('mn-time-off-panel', className)}
      aria-busy={loading || undefined}
    >
      <header className="mn-time-off-panel__header">
        <div className="mn-time-off-panel__heading">
          <H id={titleId} className="mn-time-off-panel__title">
            {m.title}
          </H>
          <p className="mn-time-off-panel__description">{m.description}</p>
        </div>
        {onAdd ? (
          <Button
            size="sm"
            variant="primary"
            leadingIcon={<PlusGlyph />}
            onClick={() => setAdding(true)}
          >
            {m.add}
          </Button>
        ) : null}
      </header>
      {notice ? <div className="mn-time-off-panel__notice">{notice}</div> : null}
      <div className="mn-time-off-panel__filters">
        <Checkbox
          label={m.includePast}
          checked={showPast}
          onCheckedChange={(v) => {
            const next = v === true;
            if (showPastProp === undefined) setShowPastState(next);
            onShowPastChange?.(next);
          }}
        />
      </div>
      {list}
      {onAdd ? (
        <AddDialog
          open={adding}
          onOpenChange={setAdding}
          onAdd={onAdd}
          people={people}
          allowClosure={allowClosure}
          types={types}
          timeZone={timeZone}
          locale={locale}
          hourCycle={hourCycle}
          today={safeToday(now, timeZone)}
          m={m}
        />
      ) : null}
      {onRemove ? (
        <ConfirmDialog
          open={toRemove !== null}
          onOpenChange={(o) => {
            if (!o) setToRemove(null);
          }}
          tone="danger"
          title={m.removeTitle}
          description={
            toRemove
              ? `${who(toRemove)} · ${entryLabel(toRemove, timeZone, locale, hourCycle, m.allDay)}. ${m.removeDescription}`
              : m.removeDescription
          }
          confirmLabel={m.removeConfirm}
          cancelLabel={m.cancel}
          onConfirm={() => (toRemove ? onRemove(toRemove) : undefined)}
        />
      ) : null}
    </section>
  );
});

function RemoveGlyph() {
  return (
    <svg
      viewBox="0 0 16 16"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.5"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
    >
      <path d="M3 4.5h10M6.5 4.5V3h3v1.5M4.5 4.5l.6 8.5h5.8l.6-8.5" />
    </svg>
  );
}

interface AddDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onAdd: (draft: TimeOffDraft) => unknown;
  people: readonly TimeOffPerson[];
  allowClosure: boolean;
  types: readonly TimeOffPerson[] | undefined;
  timeZone: string;
  locale: string | undefined;
  hourCycle: HourCycle | undefined;
  today: IsoDate;
  m: TimeOffPanelMessages;
}

function AddDialog({
  open,
  onOpenChange,
  onAdd,
  people,
  allowClosure,
  types,
  timeZone,
  locale,
  hourCycle,
  today,
  m,
}: AddDialogProps) {
  const [person, setPerson] = useState('');
  const [type, setType] = useState('');
  const [allDay, setAllDay] = useState(true);
  const [fromDay, setFromDay] = useState<IsoDate | null>(today);
  const [untilDay, setUntilDay] = useState<IsoDate | null>(today);
  const [fromTime, setFromTime] = useState<IsoDateTime | null>(`${today}T09:00`);
  const [toTime, setToTime] = useState<IsoDateTime | null>(`${today}T17:00`);
  const [reason, setReason] = useState('');
  const [attempted, setAttempted] = useState(false);
  const [running, setRunning] = useState(false);
  const [failure, setFailure] = useState<ReactNode>(null);
  const mounted = useRef(true);

  useEffect(() => {
    mounted.current = true;
    return () => {
      mounted.current = false;
    };
  }, []);

  useEffect(() => {
    if (!open) return;
    setPerson(people[0]?.value ?? (allowClosure ? CLOSURE : ''));
    setType(types?.[0]?.value ?? '');
    setAllDay(true);
    setFromDay(today);
    setUntilDay(today);
    setFromTime(`${today}T09:00`);
    setToTime(`${today}T17:00`);
    setReason('');
    setAttempted(false);
    setFailure(null);
    // eslint-disable-next-line react-hooks/exhaustive-deps -- reset only when the dialog opens
  }, [open]);

  const options = [...people, ...(allowClosure ? [{ value: CLOSURE, label: m.wholeTeam }] : [])];
  const start = allDay ? fromDay : fromTime ? wallTimeToInstant(fromTime, timeZone) : null;
  const end = allDay ? untilDay : toTime ? wallTimeToInstant(toTime, timeZone) : null;
  const problem =
    !start || !end
      ? m.incomplete
      : allDay
        ? end < start
          ? m.endBeforeStart
          : null
        : end <= start
          ? m.endBeforeStart
          : null;

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    if (running) return;
    if (problem || !start || !end) {
      setAttempted(true);
      return;
    }
    setFailure(null);
    const trimmed = reason.trim();
    const draft: TimeOffDraft = {
      personId: person === CLOSURE || person === '' ? null : person,
      allDay,
      start,
      end,
      ...(types && type ? { type } : {}),
      ...(trimmed ? { reason: trimmed } : {}),
    };
    try {
      const result = onAdd(draft);
      if (isThenable(result)) {
        setRunning(true);
        await result;
      }
    } catch (error) {
      if (mounted.current) {
        setRunning(false);
        setFailure(error instanceof Error && error.message ? error.message : m.failed);
      }
      return;
    }
    if (!mounted.current) return;
    setRunning(false);
    onOpenChange(false);
  };

  return (
    <Dialog open={open} onOpenChange={(o) => (!o && running ? undefined : onOpenChange(o))}>
      <DialogContent
        size="md"
        busy={running}
        closeOnOutsideClick={false}
        className="mn-time-off-panel__dialog"
      >
        <form className="mn-time-off-panel__form" onSubmit={submit} noValidate>
          <DialogHeader>
            <DialogTitle>{m.dialogTitle}</DialogTitle>
            <DialogDescription>
              {m.dialogDescription} ({timeZone})
            </DialogDescription>
          </DialogHeader>
          <DialogBody className="mn-time-off-panel__fields">
            {options.length > 1 ? (
              <Field>
                <FieldLabel>{m.who}</FieldLabel>
                <NativeSelect
                  value={person}
                  onChange={(e) => setPerson(e.target.value)}
                  options={options}
                />
              </Field>
            ) : null}
            {person === CLOSURE ? <Alert tone="info">{m.closureNotice}</Alert> : null}
            {types && types.length > 0 ? (
              <Field>
                <FieldLabel>{m.type}</FieldLabel>
                <NativeSelect
                  value={type}
                  onChange={(e) => setType(e.target.value)}
                  options={types}
                />
              </Field>
            ) : null}
            <Checkbox
              label={m.allDayLabel}
              checked={allDay}
              onCheckedChange={(v) => {
                setAllDay(v === true);
                setAttempted(false);
              }}
            />
            {allDay ? (
              <div className="mn-time-off-panel__range">
                <Field invalid={attempted && !!problem}>
                  <FieldLabel>{m.from}</FieldLabel>
                  <DatePicker
                    value={fromDay}
                    onValueChange={(v) => {
                      setFromDay(v);
                      if (v && untilDay && v > untilDay) setUntilDay(v);
                    }}
                    locale={locale}
                  />
                </Field>
                <Field invalid={attempted && !!problem}>
                  <FieldLabel>{m.until}</FieldLabel>
                  <DatePicker
                    value={untilDay}
                    onValueChange={setUntilDay}
                    min={fromDay ?? undefined}
                    locale={locale}
                  />
                </Field>
              </div>
            ) : (
              <div className="mn-time-off-panel__range">
                <Field invalid={attempted && !!problem}>
                  <FieldLabel>{m.start}</FieldLabel>
                  <DateTimePicker
                    value={fromTime}
                    onValueChange={setFromTime}
                    locale={locale}
                    hourCycle={hourCycle}
                  />
                </Field>
                <Field invalid={attempted && !!problem}>
                  <FieldLabel>{m.end}</FieldLabel>
                  <DateTimePicker
                    value={toTime}
                    onValueChange={setToTime}
                    locale={locale}
                    hourCycle={hourCycle}
                  />
                </Field>
              </div>
            )}
            {attempted && problem ? (
              <p className="mn-time-off-panel__problem" role="alert">
                {problem}
              </p>
            ) : null}
            <Field>
              <FieldLabel>{m.reason}</FieldLabel>
              <FieldDescription>{m.reasonDescription}</FieldDescription>
              <Textarea
                value={reason}
                onChange={(e) => setReason(e.target.value)}
                rows={2}
                maxLength={200}
                readOnly={running}
              />
            </Field>
          </DialogBody>
          {failure ? (
            <div className="mn-time-off-panel__error">
              <Alert tone="danger" live="assertive">
                {failure}
              </Alert>
            </div>
          ) : null}
          <DialogFooter>
            <DialogClose asChild>
              <Button disabled={running}>{m.cancel}</Button>
            </DialogClose>
            <Button type="submit" variant="primary" loading={running}>
              {m.save}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
