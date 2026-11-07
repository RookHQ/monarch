'use client';

import {
  createContext,
  forwardRef,
  useContext,
  useEffect,
  useId,
  useMemo,
  useRef,
  useState,
  type HTMLAttributes,
  type ReactNode,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { isThenable } from '../../../internal/dialog-helpers.js';
import { CheckGlyph, ChevronRightGlyph } from '../../../internal/icons.js';
import { Button } from '../../../components/actions/button/index.js';
import { Alert } from '../../../components/feedback/alert/index.js';
import {
  SlotPicker,
  type SlotPickerProps,
} from '../../../components/scheduling/slot-picker/index.js';

/** One step of the flow. */
export interface BookingFlowStep {
  /** Stable id, used by `step`/`onStepChange`. */
  id: string;
  /** Short name in the progress list and the summary, e.g. "Date and time". */
  label: string;
  /** Heading while the step is current, e.g. "Choose a date and time". Default `label`. */
  title?: ReactNode | undefined;
  /** Line under the heading, e.g. the chosen service and duration. */
  description?: ReactNode | undefined;
  /** The step's content: options, a form, a review list. */
  content?: ReactNode | undefined;
  /** Renders a SlotPicker as the step's content (after `content`, if both are set). */
  slotPicker?: SlotPickerProps | undefined;
  /** What was chosen, shown for a completed step and in the summary. */
  summary?: ReactNode | undefined;
  /** Whether Continue is allowed. Default `true`. */
  complete?: boolean | undefined;
  /** Label of this step's Continue button. */
  continueLabel?: string | undefined;
  /** No Continue button: the content advances itself with `useBookingFlow().next()` (e.g. picking a card). */
  hideContinue?: boolean | undefined;
}

/** Every fixed string BookingFlow shows, for translation. */
export interface BookingFlowMessages {
  /** Accessible name of the progress list. */
  steps: string;
  /** "Step 2 of 4". */
  stepOf: (current: number, total: number) => string;
  /** Announced on each step change. */
  announce: (current: number, total: number, title: string) => string;
  /** Announced when the confirmation shows. */
  done: string;
  back: string;
  /** Accessible name of the back button. */
  backTo: (label: string) => string;
  continue: string;
  /** Visible text of a completed step's change button. */
  change: string;
  /** Accessible name of a completed step's change button. */
  changeStep: (label: string) => string;
  /** Status of a completed step in the progress list (visually hidden). */
  completed: string;
  /** Default label of the last step's button. */
  submit: string;
  /** Heading of the summary panel. */
  summary: string;
  /** Fallback error when `onSubmit` rejects without a message. */
  failed: string;
}

const DEFAULT_MESSAGES: BookingFlowMessages = {
  steps: 'Booking steps',
  stepOf: (n, total) => `Step ${n} of ${total}`,
  announce: (n, total, title) => `Step ${n} of ${total}: ${title}`,
  done: 'Booking complete',
  back: 'Back',
  backTo: (label) => `Back to ${label}`,
  continue: 'Continue',
  change: 'Change',
  changeStep: (label) => `Change ${label}`,
  completed: 'completed',
  submit: 'Confirm booking',
  summary: 'Summary',
  failed: 'That did not work. Try again.',
};

interface BookingFlowContextValue {
  /** The current step's id, or `null` once complete. */
  stepId: string | null;
  /** Index of the current step. */
  index: number;
  /** Go to the next step (or submit on the last one). */
  next: () => void;
  /** Go to the previous step. */
  back: () => void;
  /** Go to a step by id. */
  goTo: (id: string) => void;
}

const BookingFlowContext = createContext<BookingFlowContextValue | null>(null);

/** Step navigation for content inside a BookingFlow, e.g. a service card that advances on click. */
export function useBookingFlow(): BookingFlowContextValue {
  const ctx = useContext(BookingFlowContext);
  if (!ctx) throw new Error('useBookingFlow must be used inside a BookingFlow.');
  return ctx;
}

export interface BookingFlowProps extends Omit<HTMLAttributes<HTMLDivElement>, 'onSubmit'> {
  /** The steps, in order. Leave out steps that do not apply (one service: no service step). */
  steps: readonly BookingFlowStep[];
  /** Current step id (controlled). */
  step?: string | undefined;
  /** Initial step id (uncontrolled). Default: the first step. */
  defaultStep?: string | undefined;
  /** Called with the step id the person moves to. */
  onStepChange?: ((step: string) => void) | undefined;
  /**
   * Runs when the last step's button is pressed. Return a promise to show a
   * pending button until it settles: on success the confirmation shows, on
   * rejection the error is shown on the step.
   */
  onSubmit?: (() => unknown) | undefined;
  /** Label of the last step's button. Default "Confirm booking"; the step's `continueLabel` wins. */
  submitLabel?: string | undefined;
  /** Shown in place of the steps once the booking is made. */
  confirmation?: ReactNode | undefined;
  /** Show the confirmation (controlled). Default: after `onSubmit` succeeds. */
  completed?: boolean | undefined;
  /** Controlled pending state of the last step's button. */
  pending?: boolean | undefined;
  /** Controlled error, shown above the step's button. */
  error?: ReactNode | undefined;
  /** Turns a rejected `onSubmit` into a message. Default: the Error's message. */
  errorMessage?: ((error: unknown) => ReactNode) | undefined;
  /**
   * `steps`: one step at a time with a progress list and a back button.
   * `list`: every step listed; completed ones show their summary and a change button.
   */
  layout?: 'steps' | 'list' | undefined;
  /** Above the flow: the business or page name, an intro. */
  header?: ReactNode | undefined;
  /** Side panel content, e.g. the service card. Completed steps' summaries follow it. */
  aside?: ReactNode | undefined;
  /** List completed steps' summaries in the side panel. Default `true`. */
  showSummary?: boolean | undefined;
  /** Shown instead of the steps when nothing can be booked (no services, page paused). */
  unavailable?: ReactNode | undefined;
  /** Tenant brand colour (`#rrggbb`) for the Continue buttons (Button `variant="brand"`). */
  brandColor?: string | undefined;
  /** Level of the step headings. Default 2. */
  headingLevel?: 2 | 3 | 4 | undefined;
  /** Interface text, for translation. */
  messages?: Partial<BookingFlowMessages> | undefined;
}

/**
 * The booking pattern: one step at a time (service, person, date and time,
 * details, review), with a progress list, back and change controls, a
 * summary panel, focus moved to each new step's heading, and an async
 * submit that ends on a confirmation. Steps are data; the product renders
 * their content. A step can host a SlotPicker directly.
 */
export const BookingFlow = forwardRef<HTMLDivElement, BookingFlowProps>(function BookingFlow(
  {
    steps,
    step: stepProp,
    defaultStep,
    onStepChange,
    onSubmit,
    submitLabel,
    confirmation,
    completed: completedProp,
    pending: pendingProp,
    error: errorProp,
    errorMessage,
    layout = 'steps',
    header,
    aside,
    showSummary = true,
    unavailable,
    brandColor,
    headingLevel = 2,
    messages: messagesProp,
    className,
    ...rest
  },
  ref,
) {
  const m = { ...DEFAULT_MESSAGES, ...messagesProp };
  const [stepState, setStepState] = useState<string | undefined>(defaultStep ?? steps[0]?.id);
  const requested = stepProp ?? stepState;
  const foundIndex = steps.findIndex((s) => s.id === requested);
  const index = foundIndex >= 0 ? foundIndex : 0;
  const current = steps[index];
  const [doneState, setDoneState] = useState(false);
  const done = completedProp ?? doneState;
  const [running, setRunning] = useState(false);
  const [failure, setFailure] = useState<ReactNode>(null);
  const [announcement, setAnnouncement] = useState('');
  const pending = pendingProp ?? running;
  const shownError = errorProp ?? failure;
  const headingRef = useRef<HTMLHeadingElement & HTMLDivElement>(null);
  const firstRender = useRef(true);
  const mounted = useRef(true);
  const errorId = useId();
  const total = steps.length;
  const isLast = index === total - 1;

  useEffect(() => {
    mounted.current = true;
    return () => {
      mounted.current = false;
    };
  }, []);

  // Move focus to the new step's heading and announce it (not on first paint).
  const key = done ? '__done' : (current?.id ?? '');
  useEffect(() => {
    if (firstRender.current) {
      firstRender.current = false;
      return;
    }
    const title = current ? textOf(current.title) || current.label : '';
    setAnnouncement(done ? m.done : m.announce(index + 1, total, title));
    headingRef.current?.focus({ preventScroll: false });
    // eslint-disable-next-line react-hooks/exhaustive-deps -- run on step change only
  }, [key]);

  const goTo = (id: string) => {
    if (pending) return;
    setFailure(null);
    if (stepProp === undefined) setStepState(id);
    onStepChange?.(id);
  };

  const submit = async () => {
    if (pending) return;
    setFailure(null);
    try {
      const result = onSubmit?.();
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
    if (completedProp === undefined) setDoneState(true);
  };

  const next = () => {
    if (!current || current.complete === false) return;
    if (isLast) void submit();
    else {
      const following = steps[index + 1];
      if (following) goTo(following.id);
    }
  };
  const back = () => {
    const previous = steps[index - 1];
    if (previous) goTo(previous.id);
  };

  const ctx = useMemo<BookingFlowContextValue>(
    () => ({ stepId: done ? null : (current?.id ?? null), index, next, back, goTo }),
    // eslint-disable-next-line react-hooks/exhaustive-deps -- the handlers read fresh state each render
    [done, current, index, steps, pending, stepProp],
  );

  const H = `h${headingLevel}` as 'h2';
  const continueVariant = brandColor ? 'brand' : 'primary';

  const body = (s: BookingFlowStep) => (
    <>
      {s.content}
      {s.slotPicker ? (
        <SlotPicker headingLevel={(headingLevel + 1) as 3 | 4 | 5} {...s.slotPicker} />
      ) : null}
      {shownError ? (
        <Alert tone="danger" live="assertive" id={errorId}>
          {shownError}
        </Alert>
      ) : null}
      {!s.hideContinue ? (
        <div className="mn-booking-flow__actions">
          <Button
            variant={continueVariant}
            {...(brandColor ? { brandColor } : {})}
            disabled={s.complete === false}
            loading={isLast && pending}
            aria-describedby={shownError ? errorId : undefined}
            onClick={next}
          >
            {s.continueLabel ?? (isLast ? (submitLabel ?? m.submit) : m.continue)}
          </Button>
        </div>
      ) : null}
    </>
  );

  const summaries = steps
    .slice(0, done ? total : index)
    .filter((s) => s.summary !== undefined && s.summary !== null);

  let flow: ReactNode;
  if (unavailable !== undefined) {
    flow = <div className="mn-booking-flow__unavailable">{unavailable}</div>;
  } else if (done) {
    flow = (
      <div className="mn-booking-flow__confirmation">
        <div ref={headingRef} tabIndex={-1} className="mn-booking-flow__focus">
          {confirmation}
        </div>
      </div>
    );
  } else if (!current) {
    flow = null;
  } else if (layout === 'list') {
    flow = (
      <ol className="mn-booking-flow__list">
        {steps.map((s, i) => {
          const state = i < index ? 'done' : i === index ? 'current' : 'todo';
          return (
            <li key={s.id} className="mn-booking-flow__item" data-state={state}>
              <span className="mn-booking-flow__number" aria-hidden="true">
                {state === 'done' ? <CheckGlyph /> : i + 1}
              </span>
              <div className="mn-booking-flow__item-body">
                {state === 'current' ? (
                  <>
                    <H ref={headingRef} tabIndex={-1} className="mn-booking-flow__title">
                      {s.title ?? s.label}
                    </H>
                    {s.description ? (
                      <p className="mn-booking-flow__description">{s.description}</p>
                    ) : null}
                    <div className="mn-booking-flow__content">{body(s)}</div>
                  </>
                ) : (
                  <div className="mn-booking-flow__item-head">
                    <div className="mn-booking-flow__item-text">
                      <H className="mn-booking-flow__label">
                        {s.label}
                        {state === 'done' ? (
                          <span className="mn-visually-hidden">, {m.completed}</span>
                        ) : null}
                      </H>
                      {state === 'done' && s.summary ? (
                        <p className="mn-booking-flow__description">{s.summary}</p>
                      ) : null}
                    </div>
                    {state === 'done' ? (
                      <Button
                        size="sm"
                        variant="ghost"
                        aria-label={m.changeStep(s.label)}
                        onClick={() => goTo(s.id)}
                      >
                        {m.change}
                      </Button>
                    ) : null}
                  </div>
                )}
              </div>
            </li>
          );
        })}
      </ol>
    );
  } else {
    flow = (
      <div className="mn-booking-flow__stepper">
        {total > 1 ? (
          <nav aria-label={m.steps} className="mn-booking-flow__progress">
            <ol>
              {steps.map((s, i) => {
                const state = i < index ? 'done' : i === index ? 'current' : 'todo';
                return (
                  <li key={s.id} data-state={state}>
                    <span className="mn-booking-flow__number" aria-hidden="true">
                      {state === 'done' ? <CheckGlyph /> : i + 1}
                    </span>
                    {state === 'done' ? (
                      <button
                        type="button"
                        className="mn-booking-flow__progress-link"
                        aria-label={`${s.label}, ${m.completed}. ${m.change}`}
                        onClick={() => goTo(s.id)}
                        disabled={pending}
                      >
                        {s.label}
                      </button>
                    ) : (
                      <span aria-current={state === 'current' ? 'step' : undefined}>{s.label}</span>
                    )}
                  </li>
                );
              })}
            </ol>
          </nav>
        ) : null}
        <div className="mn-booking-flow__head">
          {index > 0 ? (
            <Button
              size="sm"
              variant="ghost"
              className="mn-booking-flow__back"
              leadingIcon={<ChevronRightGlyph className="mn-booking-flow__back-icon" />}
              aria-label={m.backTo(steps[index - 1]?.label ?? '')}
              disabled={pending}
              onClick={back}
            >
              {m.back}
            </Button>
          ) : null}
          <div className="mn-booking-flow__head-text">
            {total > 1 ? (
              <p className="mn-booking-flow__count">{m.stepOf(index + 1, total)}</p>
            ) : null}
            <H ref={headingRef} tabIndex={-1} className="mn-booking-flow__title">
              {current.title ?? current.label}
            </H>
            {current.description ? (
              <p className="mn-booking-flow__description">{current.description}</p>
            ) : null}
          </div>
        </div>
        <div key={current.id} className="mn-booking-flow__content">
          {body(current)}
        </div>
      </div>
    );
  }

  const hasAside = aside !== undefined || (showSummary && summaries.length > 0);

  return (
    <BookingFlowContext.Provider value={ctx}>
      <div
        ref={ref}
        {...rest}
        className={cx('mn-booking-flow', className)}
        data-layout={layout}
        data-state={done ? 'done' : 'active'}
        data-busy={pending || undefined}
      >
        <p className="mn-visually-hidden mn-booking-flow__status" role="status">
          {announcement}
        </p>
        {header ? <div className="mn-booking-flow__header">{header}</div> : null}
        <div className="mn-booking-flow__layout" data-aside={hasAside || undefined}>
          <div className="mn-booking-flow__main">{flow}</div>
          {hasAside ? (
            <aside className="mn-booking-flow__aside" aria-label={m.summary}>
              {aside}
              {showSummary && summaries.length > 0 ? (
                <dl className="mn-booking-flow__summary">
                  {summaries.map((s) => (
                    <div key={s.id} className="mn-booking-flow__summary-row">
                      <dt>{s.label}</dt>
                      <dd>{s.summary}</dd>
                    </div>
                  ))}
                </dl>
              ) : null}
            </aside>
          ) : null}
        </div>
      </div>
    </BookingFlowContext.Provider>
  );
});

function textOf(node: ReactNode): string {
  if (typeof node === 'string' || typeof node === 'number') return String(node);
  if (Array.isArray(node)) return node.map(textOf).join('');
  return '';
}
