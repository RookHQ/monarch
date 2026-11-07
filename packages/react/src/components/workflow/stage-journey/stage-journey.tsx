'use client';

import {
  forwardRef,
  useEffect,
  useId,
  useRef,
  useState,
  type HTMLAttributes,
  type ReactNode,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { CheckGlyph, ErrorGlyph, SuccessGlyph } from '../../../internal/icons.js';
import { Button } from '../../actions/button/index.js';
import { RelativeTime, type DateInput } from '../../data-display/relative-time/index.js';

/** One stage of a pipeline or lifecycle. */
export interface JourneyStage {
  /** Stable id, matched against `current`. */
  id: string;
  /** Short name, such as "Examination". */
  label: string;
  /** Phase or group heading; consecutive stages with the same group are shown together. */
  group?: string | undefined;
  /** What this stage means, shown in the current-stage card. */
  description?: ReactNode;
}

/** A move offered from the current stage. */
export interface JourneyTransition {
  /** Stable id, usually the target stage id. */
  id: string;
  /** Target stage name, such as "Granted". */
  label: string;
  /**
   * `forward` (default): the next steps; the first is the primary button.
   * `exit`: leaves the pipeline early (withdrawn, lost), shown quietly.
   * `back`: undoes the last move, shown as "Back to …".
   */
  kind?: 'forward' | 'exit' | 'back';
  /** The move will ask for more details (the product opens a form); said to screen readers. */
  needsInput?: boolean;
  /** The move is not possible right now. */
  disabled?: boolean;
}

/** Visible and announced strings. Override them to translate. */
export interface StageJourneyMessages {
  /** Accessible name of the stage strip. */
  stages: string;
  /** Heading of the current-stage card: "Now: Examination". */
  now: (stage: string) => string;
  /** Before the time in the stage: "since". */
  since: string;
  /** Hidden status of a finished stage. */
  done: string;
  /** Hidden status of the current stage. */
  current: string;
  /** Terminal outcome names. */
  success: string;
  closed: string;
  /** Heading above the move buttons. */
  nextSteps: string;
  /** Accessible name of a forward or exit move. */
  moveTo: (stage: string) => string;
  /** Label and accessible name of a back move. */
  backTo: (stage: string) => string;
  /** Hidden note on a move that asks for details. */
  needsInput: string;
  /** Busy label of the move button. */
  moving: string;
  /** Announced when `current` changes. */
  moved: (stage: string) => string;
  /** Shown when a move fails without a message. */
  failed: string;
}

export const DEFAULT_STAGE_JOURNEY_MESSAGES: StageJourneyMessages = {
  stages: 'Stages',
  now: (stage) => `Now: ${stage}`,
  since: 'since',
  done: 'done',
  current: 'current stage',
  success: 'Completed',
  closed: 'Closed',
  nextSteps: 'Next steps',
  moveTo: (stage) => `Move to ${stage}`,
  backTo: (stage) => `Back to ${stage}`,
  needsInput: '(asks for details)',
  moving: 'Moving…',
  moved: (stage) => `Moved to ${stage}`,
  failed: 'The move did not go through. Try again.',
};

export interface StageJourneyProps extends Omit<HTMLAttributes<HTMLElement>, 'children'> {
  /** Every stage, in pipeline order. */
  stages: JourneyStage[];
  /** Id of the current stage. Stages before it are shown as done. */
  current: string;
  /** The current stage ends the journey: `success` (granted, won, hired) or `closed` (withdrawn, lost). */
  terminal?: 'success' | 'closed' | undefined;
  /** When the item entered the current stage; shown as "since 3 days ago". */
  enteredAt?: DateInput | undefined;
  /** Badge or note next to the current stage name, such as the phase. */
  currentMeta?: ReactNode;
  /** `full` (default): the stage strip, a current-stage card and moves. `strip`: the stage strip only. */
  variant?: 'full' | 'strip';
  /** Moves offered from the current stage (full variant). */
  transitions?: JourneyTransition[];
  /**
   * Called when a move is chosen. Return a promise to show the button as busy;
   * a rejection shows its message. The product updates `current`.
   */
  onTransition?: ((transition: JourneyTransition) => unknown) | undefined;
  /** Extra content in the current-stage card, such as a checklist or a note that moves are locked. */
  children?: ReactNode;
  /** Reference time for `enteredAt`, for identical server rendering. */
  now?: DateInput | undefined;
  /** BCP 47 locale for times. */
  locale?: string | undefined;
  /** Override visible and announced strings. */
  messages?: Partial<StageJourneyMessages>;
}

function groupStages(stages: JourneyStage[]) {
  const out: Array<{ group: string | undefined; items: Array<JourneyStage & { index: number }> }> =
    [];
  stages.forEach((s, index) => {
    const last = out[out.length - 1];
    if (last && last.group === s.group) last.items.push({ ...s, index });
    else out.push({ group: s.group, items: [{ ...s, index }] });
  });
  return out;
}

function UndoGlyph() {
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
      <path d="M6 4L3 7l3 3" />
      <path d="M3 7h6.5a3.5 3.5 0 0 1 0 7H8" />
    </svg>
  );
}

/**
 * Where an item is in a pipeline or lifecycle and what can happen next: a
 * strip of stages grouped by phase, the current stage with how long it has
 * been there, and buttons for the moves the product allows.
 */
export const StageJourney = forwardRef<HTMLElement, StageJourneyProps>(function StageJourney(
  {
    stages,
    current,
    terminal,
    enteredAt,
    currentMeta,
    variant = 'full',
    transitions = [],
    onTransition,
    children,
    now,
    locale,
    messages: messagesProp,
    className,
    ...rest
  },
  ref,
) {
  const messages = { ...DEFAULT_STAGE_JOURNEY_MESSAGES, ...messagesProp };
  const currentIndex = stages.findIndex((s) => s.id === current);
  const currentStage = stages[currentIndex];
  const titleId = useId();
  const [busy, setBusy] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [announcement, setAnnouncement] = useState('');
  const mounted = useRef(true);
  useEffect(
    () => () => {
      mounted.current = false;
    },
    [],
  );

  const previous = useRef(current);
  useEffect(() => {
    if (previous.current === current) return;
    previous.current = current;
    setError(null);
    if (currentStage) setAnnouncement(messages.moved(currentStage.label));
  }, [current]); // eslint-disable-line react-hooks/exhaustive-deps

  async function go(t: JourneyTransition) {
    if (!onTransition || busy) return;
    setBusy(t.id);
    setError(null);
    try {
      await onTransition(t);
    } catch (e) {
      if (mounted.current) setError(e instanceof Error && e.message ? e.message : messages.failed);
    } finally {
      if (mounted.current) setBusy(null);
    }
  }

  const strip = (
    // The strip scrolls sideways on narrow screens; it takes focus so keyboards can scroll it.
    // eslint-disable-next-line jsx-a11y/no-noninteractive-tabindex
    <div className="mn-stage-journey__strip" role="group" aria-label={messages.stages} tabIndex={0}>
      <ol className="mn-stage-journey__groups">
        {groupStages(stages).map((g) => (
          <li key={`${g.group ?? ''}-${g.items[0]?.id ?? ''}`} className="mn-stage-journey__group">
            {g.group ? <span className="mn-stage-journey__group-label">{g.group}</span> : null}
            <ol className="mn-stage-journey__stages">
              {g.items.map((s) => {
                const status =
                  s.index < currentIndex
                    ? 'done'
                    : s.index === currentIndex
                      ? 'current'
                      : 'upcoming';
                return (
                  <li
                    key={s.id}
                    className="mn-stage-journey__stage"
                    data-status={status}
                    data-terminal={status === 'current' && terminal ? terminal : undefined}
                    aria-current={status === 'current' ? 'step' : undefined}
                  >
                    {status === 'done' ? <CheckGlyph aria-hidden /> : null}
                    {s.label}
                    {status === 'done' ? (
                      <span className="mn-visually-hidden"> ({messages.done})</span>
                    ) : null}
                    {status === 'current' ? (
                      <span className="mn-visually-hidden"> ({messages.current})</span>
                    ) : null}
                  </li>
                );
              })}
            </ol>
          </li>
        ))}
      </ol>
    </div>
  );

  const forward = transitions.filter((t) => (t.kind ?? 'forward') === 'forward');
  const exits = transitions.filter((t) => t.kind === 'exit');
  const back = transitions.find((t) => t.kind === 'back');
  const hasMoves = onTransition !== undefined && transitions.length > 0;

  return (
    <section
      ref={ref}
      aria-labelledby={variant === 'full' ? titleId : undefined}
      {...rest}
      className={cx('mn-stage-journey', className)}
      data-variant={variant}
      data-terminal={terminal}
    >
      {strip}
      {variant === 'full' && currentStage ? (
        <div className="mn-stage-journey__card" data-terminal={terminal}>
          <div className="mn-stage-journey__heading">
            {terminal === 'success' ? (
              <span className="mn-stage-journey__outcome" aria-hidden>
                <SuccessGlyph />
              </span>
            ) : terminal === 'closed' ? (
              <span className="mn-stage-journey__outcome" aria-hidden>
                <ErrorGlyph />
              </span>
            ) : null}
            <p id={titleId} className="mn-stage-journey__title">
              {messages.now(currentStage.label)}
              {terminal ? (
                <span className="mn-visually-hidden">
                  {' '}
                  ({terminal === 'success' ? messages.success : messages.closed})
                </span>
              ) : null}
            </p>
            {currentMeta}
            {enteredAt !== undefined ? (
              <span className="mn-stage-journey__since">
                {messages.since}{' '}
                <RelativeTime
                  date={enteredAt}
                  tooltip={false}
                  {...(now !== undefined ? { now } : {})}
                  {...(locale ? { locale } : {})}
                />
              </span>
            ) : null}
          </div>
          {currentStage.description ? (
            <p className="mn-stage-journey__description">{currentStage.description}</p>
          ) : null}
          {hasMoves ? (
            <div
              className="mn-stage-journey__moves"
              role="group"
              aria-labelledby={`${titleId}-next`}
            >
              <p id={`${titleId}-next`} className="mn-stage-journey__moves-label">
                {messages.nextSteps}
              </p>
              <div className="mn-stage-journey__buttons">
                {forward.map((t, i) => (
                  <Button
                    key={t.id}
                    size="sm"
                    variant={i === 0 ? 'primary' : 'secondary'}
                    disabled={t.disabled || (busy !== null && busy !== t.id)}
                    loading={busy === t.id}
                    aria-label={`${messages.moveTo(t.label)}${t.needsInput ? ` ${messages.needsInput}` : ''}`}
                    onClick={() => void go(t)}
                  >
                    {t.label}
                  </Button>
                ))}
                {exits.map((t) => (
                  <Button
                    key={t.id}
                    size="sm"
                    variant="ghost"
                    disabled={t.disabled || (busy !== null && busy !== t.id)}
                    loading={busy === t.id}
                    aria-label={`${messages.moveTo(t.label)}${t.needsInput ? ` ${messages.needsInput}` : ''}`}
                    onClick={() => void go(t)}
                  >
                    {t.label}
                  </Button>
                ))}
                {back ? (
                  <Button
                    size="sm"
                    variant="ghost"
                    leadingIcon={<UndoGlyph />}
                    disabled={back.disabled || (busy !== null && busy !== back.id)}
                    loading={busy === back.id}
                    onClick={() => void go(back)}
                  >
                    {messages.backTo(back.label)}
                  </Button>
                ) : null}
              </div>
            </div>
          ) : null}
          {error ? (
            <p className="mn-stage-journey__error" role="alert">
              {error}
            </p>
          ) : null}
          {children}
        </div>
      ) : null}
      <span className="mn-visually-hidden" role="status" aria-live="polite">
        {announcement}
      </span>
    </section>
  );
});

/** One move in a `StageHistory`. */
export interface StageHistoryEntry {
  id: string;
  /** Stage moved from; omit for the first stage ("Started at …"). */
  from?: string | undefined;
  /** Stage moved to. */
  to: string;
  /** When the move happened. */
  at: DateInput;
  /** Who or what made the move, such as a person's name or "Automation". */
  actor?: ReactNode;
  /** Extra facts, such as "2 deadlines added". */
  meta?: ReactNode;
  /** Why, in the mover's words. */
  note?: ReactNode;
  /** The move went back to an earlier stage. */
  backward?: boolean;
}

export interface StageHistoryMessages {
  started: (stage: string) => string;
  /** Hidden word between the two stages: "to". */
  to: string;
  movedBack: string;
  empty: string;
}

export const DEFAULT_STAGE_HISTORY_MESSAGES: StageHistoryMessages = {
  started: (stage) => `Started at ${stage}`,
  to: 'to',
  movedBack: '(moved back)',
  empty: 'No stage moves yet.',
};

export interface StageHistoryProps extends Omit<HTMLAttributes<HTMLOListElement>, 'children'> {
  /** Moves, newest first or oldest first as your product prefers. */
  entries: StageHistoryEntry[];
  /** Reference time for relative times, for identical server rendering. */
  now?: DateInput | undefined;
  /** BCP 47 locale for times. */
  locale?: string | undefined;
  /** Shown when there are no entries. */
  emptyMessage?: ReactNode;
  messages?: Partial<StageHistoryMessages>;
}

/** Every stage move of an item: from, to, when, who and why, as a vertical timeline. */
export const StageHistory = forwardRef<HTMLOListElement, StageHistoryProps>(function StageHistory(
  { entries, now, locale, emptyMessage, messages: messagesProp, className, ...rest },
  ref,
) {
  const messages = { ...DEFAULT_STAGE_HISTORY_MESSAGES, ...messagesProp };
  if (entries.length === 0) {
    return (
      <p className={cx('mn-stage-history__empty', className)}>{emptyMessage ?? messages.empty}</p>
    );
  }
  return (
    <ol ref={ref} {...rest} className={cx('mn-stage-history', className)}>
      {entries.map((h) => (
        <li key={h.id} className="mn-stage-history__item" data-backward={h.backward || undefined}>
          <span className="mn-stage-history__dot" aria-hidden />
          <div className="mn-stage-history__head">
            <p className="mn-stage-history__move">
              {h.from ? (
                <>
                  {h.from} <span aria-hidden>→</span>
                  <span className="mn-visually-hidden"> {messages.to} </span>{' '}
                  <strong>{h.to}</strong>
                </>
              ) : (
                messages.started(h.to)
              )}
              {h.backward ? (
                <span className="mn-stage-history__back"> {messages.movedBack}</span>
              ) : null}
            </p>
            <RelativeTime
              className="mn-stage-history__time"
              date={h.at}
              tooltip={false}
              {...(now !== undefined ? { now } : {})}
              {...(locale ? { locale } : {})}
            />
          </div>
          {h.actor || h.meta ? (
            <p className="mn-stage-history__meta">
              {h.actor}
              {h.actor && h.meta ? ' · ' : null}
              {h.meta}
            </p>
          ) : null}
          {h.note ? <p className="mn-stage-history__note">{h.note}</p> : null}
        </li>
      ))}
    </ol>
  );
});
