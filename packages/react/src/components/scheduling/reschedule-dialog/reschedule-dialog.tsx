'use client';

import {
  forwardRef,
  useEffect,
  useId,
  useRef,
  useState,
  type ElementRef,
  type FormEvent,
  type ReactElement,
  type ReactNode,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { isThenable } from '../../../internal/dialog-helpers.js';
import type { HourCycle, IsoDate, IsoDateTime } from '../../../internal/dates/index.js';
import { Button } from '../../actions/button/index.js';
import { Alert } from '../../feedback/alert/index.js';
import { Field, FieldDescription, FieldLabel } from '../../forms/form-field/index.js';
import { Switch } from '../../forms/switch/index.js';
import { Textarea } from '../../forms/text-field/index.js';
import {
  Dialog,
  DialogBody,
  DialogClose,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
  type DialogContentProps,
} from '../../overlays/dialog/index.js';
import { DateTimePicker } from '../date-time-picker/index.js';
import { SlotPicker, type SlotPickerProps } from '../slot-picker/index.js';
import { formatInstant, instantToWallTime, wallTimeToInstant } from './reschedule-dialog.zone.js';

/** What the person chose. */
export interface RescheduleDetails {
  /** The new start instant, ISO 8601 in UTC (`2026-11-05T10:00:00.000Z` or the slot's own `start`). */
  start: string;
  /** `true` when the time was typed outside the open slots. */
  outsideHours: boolean;
  /** The trimmed reason, when the reason field is shown and filled in. */
  reason?: string;
}

/** Every fixed string RescheduleDialog shows, for translation. */
export interface RescheduleDialogMessages {
  title: string;
  /** The line under the title naming the current time. */
  current: (when: string) => string;
  confirm: string;
  cancel: string;
  /** Label of the outside-hours switch. */
  outsideHours: string;
  /** Help under the outside-hours switch. */
  outsideHoursDescription: string;
  /** Label of the typed date and time. */
  newTime: string;
  /** Label of the reason field. */
  reason: string;
  /** Help under the reason field. */
  reasonDescription: string;
  /** Shown when the person tries to confirm with no time chosen. */
  noTime: string;
  /** Fallback error when `onConfirm` rejects without a message. */
  failed: string;
}

const DEFAULT_MESSAGES: RescheduleDialogMessages = {
  title: 'Reschedule',
  current: (when) => `Currently ${when}`,
  confirm: 'Reschedule',
  cancel: 'Cancel',
  outsideHours: 'Choose a time outside working hours',
  outsideHoursDescription: 'Pick any time. Clashes are still checked when you save.',
  newTime: 'New time',
  reason: 'Reason (optional)',
  reasonDescription: 'Kept in the history.',
  noTime: 'Choose a new time first.',
  failed: 'That did not work. Try again.',
};

/** The SlotPicker props the dialog passes through. The dialog owns the value. */
export type RescheduleSlotPickerProps = Omit<
  SlotPickerProps,
  | 'value'
  | 'defaultValue'
  | 'onValueChange'
  | 'timeZone'
  | 'onChooseOutside'
  | 'locale'
  | 'hourCycle'
>;

export interface RescheduleDialogProps extends Omit<
  DialogContentProps,
  'title' | 'children' | 'footer' | 'busy' | 'onSubmit'
> {
  /** Controlled open state. */
  open?: boolean | undefined;
  /** Uncontrolled initial open state. */
  defaultOpen?: boolean | undefined;
  onOpenChange?: ((open: boolean) => void) | undefined;
  /** The element that opens the dialog (rendered with `asChild`), e.g. a Button. */
  trigger?: ReactElement | undefined;
  /** Dialog title. Default "Reschedule". */
  title?: ReactNode | undefined;
  /** What is being moved, e.g. "Consultation with Sam Lee". Shown above the current time. */
  subject?: ReactNode | undefined;
  /** The booking's current start instant, shown as "Currently …" and used as the typed time's default. */
  currentStart?: string | undefined;
  /** IANA zone every time is shown and typed in. */
  timeZone: string;
  /** Locale for dates and times. */
  locale?: string | undefined;
  /** Force a 12- or 24-hour clock. */
  hourCycle?: HourCycle | undefined;
  /**
   * The open slots: SlotPicker props without the value, which the dialog
   * owns. Load `slots` for `day` as usual; the choice clears when the day changes.
   */
  slotPicker: RescheduleSlotPickerProps;
  /** Shows a switch to type any date and time instead of an open slot (staff only). */
  allowOutsideHours?: boolean | undefined;
  /** Earliest typed time, `YYYY-MM-DDTHH:mm` in `timeZone`. */
  outsideHoursMin?: IsoDateTime | undefined;
  /** Minutes between typed times. Default 15. */
  outsideHoursStep?: number | undefined;
  /** Shows the optional reason field. Default `true`. */
  showReason?: boolean | undefined;
  /** Information above the picker, e.g. "The customer is emailed the new time." */
  notice?: ReactNode | undefined;
  /** Extra fields above the picker, e.g. a host select. */
  children?: ReactNode | undefined;
  /**
   * Saves the new time. Return a promise to keep the dialog open and pending
   * until it settles: on success it closes, on rejection the error is shown
   * and the person can try again.
   */
  onConfirm: (details: RescheduleDetails) => unknown;
  /** Controlled pending state. */
  pending?: boolean | undefined;
  /** Controlled error message, shown above the actions. */
  error?: ReactNode | undefined;
  /** Turns a rejected `onConfirm` into a message. Default: the Error's message. */
  errorMessage?: ((error: unknown) => ReactNode) | undefined;
  /** Interface text, for translation. */
  messages?: Partial<RescheduleDialogMessages> | undefined;
}

/**
 * Moves a booking to a new time: the open slots in a SlotPicker, an optional
 * switch to type a time outside working hours, an optional reason, and an
 * async confirm with pending and error states. Built on Dialog. The product
 * loads the slots and saves the change.
 */
export const RescheduleDialog = forwardRef<ElementRef<typeof DialogContent>, RescheduleDialogProps>(
  function RescheduleDialog(
    {
      open: openProp,
      defaultOpen = false,
      onOpenChange,
      trigger,
      title,
      subject,
      currentStart,
      timeZone,
      locale,
      hourCycle,
      slotPicker,
      allowOutsideHours = false,
      outsideHoursMin,
      outsideHoursStep = 15,
      showReason = true,
      notice,
      children,
      onConfirm,
      pending: pendingProp,
      error: errorProp,
      errorMessage,
      messages: messagesProp,
      size = 'lg',
      closeOnOutsideClick = false,
      className,
      ...rest
    },
    ref,
  ) {
    const m = { ...DEFAULT_MESSAGES, ...messagesProp };
    const [openState, setOpenState] = useState(defaultOpen);
    const open = openProp ?? openState;
    const [slot, setSlot] = useState<string | null>(null);
    const [outside, setOutside] = useState(false);
    const [typed, setTyped] = useState<IsoDateTime | null>(null);
    const [reason, setReason] = useState('');
    const [running, setRunning] = useState(false);
    const [failure, setFailure] = useState<ReactNode>(null);
    const [attempted, setAttempted] = useState(false);
    const mounted = useRef(true);
    const errorId = useId();

    useEffect(() => {
      mounted.current = true;
      return () => {
        mounted.current = false;
      };
    }, []);

    // Every opening starts clean.
    useEffect(() => {
      if (!open) return;
      setSlot(null);
      setOutside(false);
      setTyped(currentStart ? instantToWallTime(currentStart, timeZone) : null);
      setReason('');
      setFailure(null);
      setAttempted(false);
      // eslint-disable-next-line react-hooks/exhaustive-deps -- reset only when the dialog opens
    }, [open]);

    const pending = pendingProp ?? running;
    const shownError = errorProp ?? failure;
    const start = outside ? (typed ? wallTimeToInstant(typed, timeZone) : null) : slot;

    const setOpen = (next: boolean) => {
      if (openProp === undefined) setOpenState(next);
      onOpenChange?.(next);
    };

    const submit = async (event: FormEvent) => {
      event.preventDefault();
      if (pending) return;
      if (!start) {
        setAttempted(true);
        return;
      }
      setFailure(null);
      const trimmed = reason.trim();
      const details: RescheduleDetails = {
        start,
        outsideHours: outside,
        ...(showReason && trimmed ? { reason: trimmed } : {}),
      };
      try {
        const result = onConfirm(details);
        if (isThenable(result)) {
          setRunning(true);
          await result;
        }
      } catch (error) {
        if (mounted.current) {
          setRunning(false);
          setFailure(
            errorMessage
              ? errorMessage(error)
              : error instanceof Error && error.message
                ? error.message
                : m.failed,
          );
        }
        return;
      }
      if (!mounted.current) return;
      setRunning(false);
      setOpen(false);
    };

    const onDayChange = (day: IsoDate) => {
      setSlot(null);
      slotPicker.onDayChange?.(day);
    };

    return (
      <Dialog open={open} onOpenChange={(next) => (!next && pending ? undefined : setOpen(next))}>
        {trigger ? <DialogTrigger asChild>{trigger}</DialogTrigger> : null}
        <DialogContent
          ref={ref}
          {...rest}
          size={size}
          busy={pending}
          closeOnOutsideClick={closeOnOutsideClick}
          className={cx('mn-reschedule-dialog', className)}
        >
          <form className="mn-reschedule-dialog__form" onSubmit={submit} noValidate>
            <DialogHeader>
              <DialogTitle>{title ?? m.title}</DialogTitle>
              {subject || currentStart ? (
                <DialogDescription>
                  {subject}
                  {subject && currentStart ? ' · ' : null}
                  {currentStart
                    ? m.current(formatInstant(currentStart, timeZone, locale, hourCycle))
                    : null}
                </DialogDescription>
              ) : null}
            </DialogHeader>
            <DialogBody className="mn-reschedule-dialog__body">
              {notice ? <Alert tone="info">{notice}</Alert> : null}
              {children}
              {allowOutsideHours ? (
                <Switch
                  className="mn-reschedule-dialog__outside"
                  label={m.outsideHours}
                  description={m.outsideHoursDescription}
                  checked={outside}
                  disabled={pending}
                  onCheckedChange={(next) => {
                    setOutside(next);
                    setAttempted(false);
                  }}
                />
              ) : null}
              {outside ? (
                <Field invalid={attempted && !start}>
                  <FieldLabel>{m.newTime}</FieldLabel>
                  <DateTimePicker
                    value={typed}
                    onValueChange={(v) => {
                      setTyped(v);
                      setAttempted(false);
                    }}
                    min={outsideHoursMin}
                    step={outsideHoursStep}
                    locale={locale}
                    hourCycle={hourCycle}
                    timeZone={timeZone}
                    readOnly={pending}
                  />
                </Field>
              ) : (
                <SlotPicker
                  layout="auto"
                  headingLevel={3}
                  {...slotPicker}
                  onDayChange={onDayChange}
                  value={slot}
                  onValueChange={(next) => {
                    setSlot(next);
                    setAttempted(false);
                  }}
                  timeZone={timeZone}
                  locale={locale}
                  hourCycle={hourCycle}
                  onChooseOutside={
                    allowOutsideHours
                      ? () => {
                          setOutside(true);
                          setAttempted(false);
                        }
                      : undefined
                  }
                />
              )}
              {showReason ? (
                <Field>
                  <FieldLabel>{m.reason}</FieldLabel>
                  <FieldDescription>{m.reasonDescription}</FieldDescription>
                  <Textarea
                    value={reason}
                    onChange={(e) => setReason(e.target.value)}
                    rows={2}
                    maxLength={1000}
                    readOnly={pending}
                  />
                </Field>
              ) : null}
            </DialogBody>
            {attempted && !start ? (
              <div className="mn-reschedule-dialog__error">
                <Alert tone="warning" live="polite">
                  {m.noTime}
                </Alert>
              </div>
            ) : null}
            {shownError ? (
              <div className="mn-reschedule-dialog__error">
                <Alert tone="danger" live="assertive" id={errorId}>
                  {shownError}
                </Alert>
              </div>
            ) : null}
            <DialogFooter>
              <DialogClose asChild>
                <Button disabled={pending}>{m.cancel}</Button>
              </DialogClose>
              <Button
                type="submit"
                variant="primary"
                loading={pending}
                aria-describedby={shownError ? errorId : undefined}
              >
                {m.confirm}
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>
    );
  },
);
