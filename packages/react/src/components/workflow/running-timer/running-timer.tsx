'use client';

import {
  forwardRef,
  useEffect,
  useRef,
  useState,
  type HTMLAttributes,
  type ReactNode,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { ClockGlyph, ChevronDownGlyph } from '../../../internal/icons.js';
import { Button, IconButton } from '../../actions/button/index.js';
import { ConfirmDialog } from '../../overlays/confirm-dialog/index.js';
import {
  Dialog,
  DialogBody,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from '../../overlays/dialog/index.js';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '../../overlays/dropdown-menu/index.js';
import { Field, FieldDescription, FieldError, FieldLabel } from '../../forms/form-field/index.js';
import { Input } from '../../forms/text-field/index.js';

/** Anything `new Date()` accepts: a Date, epoch milliseconds or an ISO 8601 string. */
export type TimerDateInput = Date | number | string;

/** Formats whole seconds as a stopwatch: `0:04:09`, `12:30:00`. */
export function formatElapsed(seconds: number): string {
  const s = Math.max(0, Math.floor(seconds));
  const h = Math.floor(s / 3600);
  const m = Math.floor((s % 3600) / 60);
  return `${h}:${String(m).padStart(2, '0')}:${String(s % 60).padStart(2, '0')}`;
}

/** Formats whole minutes as "2h 05m". */
function formatHoursMinutes(minutes: number): string {
  const m = Math.max(0, Math.round(minutes));
  return `${Math.floor(m / 60)}h ${String(m % 60).padStart(2, '0')}m`;
}

/** Visible and announced strings. Override them to translate. */
export interface RunningTimerMessages {
  /** Start button (panel). */
  start: string;
  /** Stop button (panel). */
  stop: string;
  /** Accessible name of the stop button in the indicator. */
  stopAndLog: string;
  /** Accessible name of the indicator's menu button. */
  moreActions: string;
  /** Discard action. */
  discard: string;
  /** Discard confirmation. */
  discardTitle: string;
  discardDescription: string;
  discardConfirm: string;
  /** "I stopped earlier" action and dialog. */
  stoppedEarlier: string;
  stoppedEarlierTitle: string;
  stoppedEarlierDescription: string;
  stoppedAtLabel: string;
  started: (formatted: string) => string;
  workDuration: (duration: string) => string;
  save: string;
  cancel: string;
  /** Validation of the stopped-at time. */
  stoppedAtRequired: string;
  stoppedAtInvalid: string;
  stoppedAtFuture: string;
  stoppedAtTooEarly: string;
  /** Shown when a save or action fails and the error has no message. */
  failed: string;
  /** State, visually hidden next to the clock and announced on change. */
  running: string;
  stopped: string;
  /** Accessible name of the indicator's open button. */
  openLabel: (label: string, elapsed: string) => string;
  /** Default label when none is given. */
  defaultLabel: string;
  /** Warning shown once the timer has run longer than `warnAfter`. */
  longRunning: (duration: string) => string;
}

export const DEFAULT_RUNNING_TIMER_MESSAGES: RunningTimerMessages = {
  start: 'Start timer',
  stop: 'Stop',
  stopAndLog: 'Stop timer and log time',
  moreActions: 'More timer actions',
  discard: 'Discard without logging',
  discardTitle: 'Discard the running timer?',
  discardDescription: 'The tracked time will not be saved.',
  discardConfirm: 'Discard',
  stoppedEarlier: 'I stopped earlier',
  stoppedEarlierTitle: 'When did you stop?',
  stoppedEarlierDescription:
    'The time between the start and the moment you enter is saved. It cannot be in the future or before the timer started.',
  stoppedAtLabel: 'I stopped at',
  started: (formatted) => `Started ${formatted}`,
  workDuration: (duration) => `That is ${duration} of work.`,
  save: 'Save this time',
  cancel: 'Cancel',
  stoppedAtRequired: 'Choose the time you stopped.',
  stoppedAtInvalid: 'That is not a valid time.',
  stoppedAtFuture: 'The time cannot be in the future.',
  stoppedAtTooEarly: 'It must be at least a minute after the timer started.',
  failed: 'That did not work. Try again.',
  running: 'Timer running',
  stopped: 'Timer stopped',
  openLabel: (label, elapsed) => `Timer running on ${label}, ${elapsed} elapsed. Open`,
  defaultLabel: 'Timer',
  longRunning: (duration) =>
    `This timer has been running for over ${duration}. If you forgot to stop it, enter the time you finished or discard it and add the time by hand.`,
};

function toMs(input: TimerDateInput | null | undefined): number | null {
  if (input === null || input === undefined) return null;
  const ms = (input instanceof Date ? input : new Date(input)).getTime();
  return Number.isNaN(ms) ? null : ms;
}

const pad = (n: number) => String(n).padStart(2, '0');
/** A time as the local value a `datetime-local` input expects. */
function toLocalInput(ms: number): string {
  const d = new Date(ms);
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

/** Hydration-safe clock: server and first client render agree, then it ticks. */
function useClock(injected: number | null, intervalMs: number | false): number {
  const [clock, setClock] = useState<number>(() => injected ?? Date.now());
  useEffect(() => {
    if (injected !== null) return;
    setClock(Date.now());
    if (intervalMs === false) return;
    const timer = setInterval(() => setClock(Date.now()), intervalMs);
    return () => clearInterval(timer);
  }, [injected, intervalMs]);
  return injected ?? clock;
}

function errorText(error: unknown, fallback: string): string {
  return error instanceof Error && error.message ? error.message : fallback;
}

export interface RunningTimerProps extends Omit<HTMLAttributes<HTMLDivElement>, 'children'> {
  /** When the running timer started; `null` or omitted when no timer runs. */
  startedAt?: TimerDateInput | null | undefined;
  /** What is being timed, such as an issue key or project name. Shown in the indicator and part of its accessible name. */
  label?: string | undefined;
  /** Longer description, shown as the indicator's hover text. */
  description?: string | undefined;
  /**
   * `indicator`: a compact chip for a top bar, hidden while stopped unless
   * `showWhenStopped`. `panel` (default): a large clock with Start and Stop
   * buttons and room for the fields of the next entry (`children`).
   */
  variant?: 'panel' | 'indicator';
  /** Keep the indicator visible (showing 0:00:00) while no timer runs. */
  showWhenStopped?: boolean;
  /** Fields for the entry being timed, such as project and note pickers (panel only). */
  children?: ReactNode;
  /** Starts a timer. Return a promise to show the button as busy until it settles. */
  onStart?: (() => unknown) | undefined;
  /** Stops the timer and logs the time. Return a promise to show the button as busy. */
  onStop?: (() => unknown) | undefined;
  /** Throws the running time away. Adds a Discard action, confirmed first. */
  onDiscard?: (() => unknown) | undefined;
  /**
   * Stops the timer at an earlier time. Adds "I stopped earlier", which asks
   * for the time (between the start plus one minute and now).
   */
  onStopAt?: ((stoppedAt: Date) => unknown) | undefined;
  /** Opens what is being timed. Makes the indicator's label a button. */
  onOpen?: (() => void) | undefined;
  /** Starting is not allowed yet, for example until a required project is chosen. */
  startDisabled?: boolean;
  /** An action is running outside the component: buttons show as busy. */
  pending?: boolean;
  /** Show a warning once the timer has run this many milliseconds. Default 12 hours; `false` turns it off. */
  warnAfter?: number | false;
  /** The reference time, for identical server rendering and tests. It also stops the ticking. */
  now?: TimerDateInput | undefined;
  /** BCP 47 locale for the start time in the "I stopped earlier" dialog. */
  locale?: string | undefined;
  /** Override visible and announced strings. */
  messages?: Partial<RunningTimerMessages>;
}

/**
 * A running stopwatch for time tracking: a top-bar chip or a panel with
 * Start, Stop, "I stopped earlier" and a confirmed Discard. The clock ticks
 * silently; only starting and stopping are announced.
 */
export const RunningTimer = forwardRef<HTMLDivElement, RunningTimerProps>(function RunningTimer(
  {
    startedAt,
    label,
    description,
    variant = 'panel',
    showWhenStopped = false,
    children,
    onStart,
    onStop,
    onDiscard,
    onStopAt,
    onOpen,
    startDisabled = false,
    pending = false,
    warnAfter = 12 * 3_600_000,
    now: nowProp,
    locale,
    messages: messagesProp,
    className,
    ...rest
  },
  ref,
) {
  const messages = { ...DEFAULT_RUNNING_TIMER_MESSAGES, ...messagesProp };
  const start = toMs(startedAt);
  const running = start !== null;
  const now = useClock(toMs(nowProp), running ? 1000 : false);
  const seconds = running ? Math.max(0, Math.floor((now - start) / 1000)) : 0;
  const clock = formatElapsed(seconds);

  const [busy, setBusy] = useState<null | 'start' | 'stop' | 'discard' | 'stopAt'>(null);
  const [error, setError] = useState<string | null>(null);
  const [discardOpen, setDiscardOpen] = useState(false);
  const [earlierOpen, setEarlierOpen] = useState(false);
  const [stoppedAt, setStoppedAt] = useState('');
  const [announcement, setAnnouncement] = useState('');
  const mounted = useRef(true);
  useEffect(
    () => () => {
      mounted.current = false;
    },
    [],
  );

  const wasRunning = useRef(running);
  useEffect(() => {
    if (wasRunning.current === running) return;
    wasRunning.current = running;
    setAnnouncement(running ? messages.running : messages.stopped);
    if (!running) {
      setEarlierOpen(false);
      setDiscardOpen(false);
    }
  }, [running]); // eslint-disable-line react-hooks/exhaustive-deps

  async function run(kind: NonNullable<typeof busy>, action: () => unknown): Promise<boolean> {
    setBusy(kind);
    setError(null);
    try {
      await action();
      return true;
    } catch (e) {
      if (mounted.current) setError(errorText(e, messages.failed));
      return false;
    } finally {
      if (mounted.current) setBusy(null);
    }
  }

  const stoppedAtMs = stoppedAt ? new Date(stoppedAt).getTime() : null;
  const stoppedAtError =
    !running || !earlierOpen
      ? null
      : stoppedAtMs === null
        ? messages.stoppedAtRequired
        : Number.isNaN(stoppedAtMs)
          ? messages.stoppedAtInvalid
          : stoppedAtMs > now
            ? messages.stoppedAtFuture
            : stoppedAtMs - start < 60_000
              ? messages.stoppedAtTooEarly
              : null;

  function openEarlier() {
    if (start === null) return;
    setError(null);
    setStoppedAt(toLocalInput(Math.min(Date.now(), start + 3_600_000)));
    setEarlierOpen(true);
  }

  const longRunning = running && warnAfter !== false && now - start > warnAfter;
  const isBusy = pending || busy !== null;
  const labelText = label ?? messages.defaultLabel;

  const dialogs = running ? (
    <>
      {onDiscard ? (
        <ConfirmDialog
          open={discardOpen}
          onOpenChange={setDiscardOpen}
          title={messages.discardTitle}
          description={messages.discardDescription}
          tone="danger"
          confirmLabel={messages.discardConfirm}
          cancelLabel={messages.cancel}
          onConfirm={() => onDiscard()}
        />
      ) : null}
      {onStopAt ? (
        <Dialog
          open={earlierOpen}
          onOpenChange={(open) => busy !== 'stopAt' && setEarlierOpen(open)}
        >
          <DialogContent
            size="sm"
            busy={busy === 'stopAt'}
            closeOnOutsideClick={false}
            footer={
              <>
                <Button disabled={busy === 'stopAt'} onClick={() => setEarlierOpen(false)}>
                  {messages.cancel}
                </Button>
                <Button
                  variant="primary"
                  loading={busy === 'stopAt'}
                  disabled={stoppedAtError !== null}
                  onClick={() => {
                    if (stoppedAtMs === null || stoppedAtError) return;
                    void run('stopAt', () => onStopAt(new Date(stoppedAtMs))).then((ok) => {
                      if (ok && mounted.current) setEarlierOpen(false);
                    });
                  }}
                >
                  {messages.save}
                </Button>
              </>
            }
          >
            <DialogHeader>
              <DialogTitle>{messages.stoppedEarlierTitle}</DialogTitle>
              <DialogDescription>{messages.stoppedEarlierDescription}</DialogDescription>
            </DialogHeader>
            <DialogBody className="mn-running-timer__earlier">
              <p className="mn-running-timer__started">
                {messages.started(
                  new Intl.DateTimeFormat(locale, {
                    dateStyle: 'medium',
                    timeStyle: 'short',
                  }).format(new Date(start)),
                )}
              </p>
              <Field invalid={stoppedAtError !== null}>
                <FieldLabel>{messages.stoppedAtLabel}</FieldLabel>
                <Input
                  type="datetime-local"
                  value={stoppedAt}
                  min={toLocalInput(start)}
                  max={toLocalInput(now)}
                  onChange={(e) => setStoppedAt(e.target.value)}
                />
                {stoppedAtError ? (
                  <FieldError>{stoppedAtError}</FieldError>
                ) : stoppedAtMs !== null ? (
                  <FieldDescription>
                    {messages.workDuration(formatHoursMinutes((stoppedAtMs - start) / 60_000))}
                  </FieldDescription>
                ) : null}
              </Field>
              {error && busy === null ? (
                <p className="mn-running-timer__error" role="alert">
                  {error}
                </p>
              ) : null}
            </DialogBody>
          </DialogContent>
        </Dialog>
      ) : null}
    </>
  ) : null;

  const live = (
    <span className="mn-visually-hidden" role="status" aria-live="polite">
      {announcement}
    </span>
  );

  if (variant === 'indicator') {
    if (!running && !showWhenStopped) {
      return (
        // Only the live region stays, so "Timer stopped" is still announced.
        <div
          ref={ref}
          {...rest}
          className={cx('mn-running-timer', className)}
          data-variant="indicator"
          data-empty=""
        >
          {live}
        </div>
      );
    }
    const content = (
      <>
        <span className="mn-running-timer__icon" aria-hidden>
          <ClockGlyph />
        </span>
        <span className="mn-running-timer__label">{labelText}</span>
        <span className="mn-running-timer__clock" suppressHydrationWarning>
          {clock}
        </span>
      </>
    );
    const hasMenu = running && (onDiscard !== undefined || onStopAt !== undefined);
    return (
      <div
        ref={ref}
        {...rest}
        className={cx('mn-running-timer', className)}
        data-variant="indicator"
        data-running={running || undefined}
        data-long={longRunning || undefined}
      >
        {onOpen && running ? (
          <button
            type="button"
            className="mn-running-timer__open"
            title={description}
            aria-label={messages.openLabel(labelText, clock)}
            onClick={onOpen}
          >
            {content}
          </button>
        ) : (
          <span className="mn-running-timer__open" title={description}>
            {content}
            <span className="mn-visually-hidden">
              {' '}
              ({running ? messages.running : messages.stopped})
            </span>
          </span>
        )}
        {running && onStop ? (
          <button
            type="button"
            className="mn-running-timer__part"
            aria-label={messages.stopAndLog}
            title={messages.stopAndLog}
            disabled={isBusy}
            aria-busy={busy === 'stop' || undefined}
            onClick={() => void run('stop', onStop)}
          >
            <StopGlyph />
          </button>
        ) : null}
        {hasMenu ? (
          <DropdownMenu>
            <DropdownMenuTrigger
              className="mn-running-timer__part"
              aria-label={messages.moreActions}
              disabled={isBusy}
            >
              <ChevronDownGlyph aria-hidden />
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end">
              {onStopAt ? (
                <DropdownMenuItem onSelect={openEarlier}>
                  {messages.stoppedEarlier}
                </DropdownMenuItem>
              ) : null}
              {onDiscard ? (
                <DropdownMenuItem tone="danger" onSelect={() => setDiscardOpen(true)}>
                  {messages.discard}
                </DropdownMenuItem>
              ) : null}
            </DropdownMenuContent>
          </DropdownMenu>
        ) : null}
        {error && !earlierOpen ? (
          <span className="mn-visually-hidden" role="alert">
            {error}
          </span>
        ) : null}
        {live}
        {dialogs}
      </div>
    );
  }

  return (
    <div
      ref={ref}
      {...rest}
      className={cx('mn-running-timer', className)}
      data-variant="panel"
      data-running={running || undefined}
      data-long={longRunning || undefined}
    >
      <div className="mn-running-timer__readout">
        <span className="mn-running-timer__clock" role="timer" suppressHydrationWarning>
          {clock}
        </span>
        <span className="mn-visually-hidden">{running ? messages.running : messages.stopped}</span>
        {label ? (
          <span className="mn-running-timer__label" title={description}>
            {label}
          </span>
        ) : null}
      </div>
      {children ? <div className="mn-running-timer__fields">{children}</div> : null}
      <div className="mn-running-timer__actions">
        {running ? (
          <>
            {onStop ? (
              <Button
                variant="danger"
                leadingIcon={<StopGlyph />}
                loading={busy === 'stop'}
                disabled={isBusy && busy !== 'stop'}
                onClick={() => void run('stop', onStop)}
              >
                {messages.stop}
              </Button>
            ) : null}
            {onStopAt ? (
              <Button disabled={isBusy} onClick={openEarlier}>
                {messages.stoppedEarlier}
              </Button>
            ) : null}
            {onDiscard ? (
              <IconButton
                label={messages.discard}
                icon={<TrashGlyph />}
                disabled={isBusy}
                onClick={() => setDiscardOpen(true)}
              />
            ) : null}
          </>
        ) : onStart ? (
          <Button
            variant="primary"
            leadingIcon={<PlayGlyph />}
            loading={busy === 'start' || (pending && busy === null)}
            disabled={startDisabled || (isBusy && busy !== 'start')}
            onClick={() => void run('start', onStart)}
          >
            {messages.start}
          </Button>
        ) : null}
      </div>
      {longRunning ? (
        <p className="mn-running-timer__warning" role="status">
          {messages.longRunning(
            formatHoursMinutes((warnAfter as number) / 60_000).replace(/ 00m$/, ''),
          )}
        </p>
      ) : null}
      {error && !earlierOpen ? (
        <p className="mn-running-timer__error" role="alert">
          {error}
        </p>
      ) : null}
      {live}
      {dialogs}
    </div>
  );
});

function StopGlyph() {
  return (
    <svg viewBox="0 0 16 16" width="16" height="16" aria-hidden focusable={false}>
      <rect x="4" y="4" width="8" height="8" rx="1.5" fill="currentColor" />
    </svg>
  );
}

function PlayGlyph() {
  return (
    <svg viewBox="0 0 16 16" width="16" height="16" aria-hidden focusable={false}>
      <path d="M5 3.5v9l7-4.5z" fill="currentColor" />
    </svg>
  );
}

function TrashGlyph() {
  return (
    <svg
      viewBox="0 0 16 16"
      width="16"
      height="16"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.75"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden
      focusable={false}
    >
      <path d="M3 4.5h10M6.5 4.5V3h3v1.5M4.5 4.5l.5 8.5h6l.5-8.5" />
    </svg>
  );
}
