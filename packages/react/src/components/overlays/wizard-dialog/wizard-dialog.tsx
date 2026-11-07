'use client';

import {
  forwardRef,
  useCallback,
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
import { Button } from '../../actions/button/index.js';
import { Alert } from '../../feedback/alert/index.js';
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
} from '../dialog/index.js';

/** One step of a WizardDialog. */
export interface WizardStep {
  /** Stable id, used by the controlled `step` prop and `onStepChange`. */
  id: string;
  /** Step heading. It receives focus when the step changes, so screen readers announce it. */
  title: ReactNode;
  /** One line under the step heading. */
  description?: ReactNode;
  /** The step's content: fields, lists, summaries. */
  content?: ReactNode;
  /** Continue stays disabled while this is `false`. Default `true`. */
  canContinue?: boolean;
  /** Says why Continue is disabled, shown next to the actions. */
  blockedReason?: ReactNode;
  /** Label of the continue button on this step. Name the action on the last step ("Delete workspace"). */
  continueLabel?: ReactNode;
  /** `danger` gives the continue button the danger style, for the destructive step. */
  tone?: 'default' | 'danger';
  /**
   * Runs when the person continues. Return a promise to keep the dialog in a
   * pending state until it settles; a rejection shows its message and stays on
   * the step. Return `false` to stay on the step without an error.
   */
  onContinue?: () => unknown;
  /** Hide the Back button on this step. */
  hideBack?: boolean;
  /**
   * `done` marks a closing screen after the action has run: it is not counted
   * in the progress, has no Back or Cancel, and shows one close button.
   */
  kind?: 'step' | 'done';
}

/** Every visible string of the WizardDialog, for translation. */
export interface WizardDialogMessages {
  back: string;
  cancel: string;
  continue: string;
  /** Close button on a `done` step. */
  done: string;
  /** Progress text, e.g. "Step 2 of 4". */
  stepOf: (current: number, total: number) => string;
  /** Used when a rejection has no message. */
  error: string;
}

const DEFAULT_MESSAGES: WizardDialogMessages = {
  back: 'Back',
  cancel: 'Cancel',
  continue: 'Continue',
  done: 'Done',
  stepOf: (current, total) => `Step ${current} of ${total}`,
  error: 'That did not work. Try again.',
};

export interface WizardDialogProps extends Omit<
  DialogContentProps,
  'title' | 'children' | 'footer' | 'busy' | 'onSubmit'
> {
  /** Controlled open state. */
  open?: boolean;
  /** Uncontrolled initial open state. */
  defaultOpen?: boolean;
  onOpenChange?: (open: boolean) => void;
  /** The element that opens the dialog (rendered with `asChild`), e.g. a Button. */
  trigger?: ReactElement;
  /** What the whole flow does, e.g. "Delete workspace". The dialog's accessible name. */
  title: ReactNode;
  /** One line under the title for the whole flow. */
  description?: ReactNode;
  /** The steps, in order. Leave a step out of the array to skip it. */
  steps: WizardStep[];
  /** Controlled current step id. */
  step?: string;
  /** Uncontrolled first step id. Default: the first step. Every opening starts here. */
  defaultStep?: string;
  /** Called with the new step id whenever the step changes. */
  onStepChange?: (id: string) => void;
  /** Called after the last step continues successfully and there is no `done` step. The dialog closes. */
  onComplete?: () => void;
  /** Called when the dialog is dismissed before it completes. */
  onCancel?: () => void;
  /** Show "Step n of total" and the progress bar. Default `true` when there are two or more steps. */
  showProgress?: boolean;
  /** Turns a rejected `onContinue` into a message. Default: the Error's message, or `messages.error`. */
  errorMessage?: (error: unknown) => ReactNode;
  /** Visible strings, for translation. */
  messages?: Partial<WizardDialogMessages>;
}

/**
 * A dialog that walks through a short sequence of steps: review, choose,
 * confirm, done. It owns navigation (Back, Continue, Cancel), a progress line,
 * async steps with a pending lock and inline errors, and focus: each new step
 * moves focus to its heading. Account and destructive flows are built on it.
 */
export const WizardDialog = forwardRef<ElementRef<typeof DialogContent>, WizardDialogProps>(
  function WizardDialog(
    {
      open: openProp,
      defaultOpen = false,
      onOpenChange,
      trigger,
      title,
      description,
      steps,
      step: stepProp,
      defaultStep,
      onStepChange,
      onComplete,
      onCancel,
      showProgress,
      errorMessage,
      messages: messagesProp,
      size = 'md',
      closeOnOutsideClick = false,
      className,
      ...rest
    },
    ref,
  ) {
    const messages = { ...DEFAULT_MESSAGES, ...messagesProp };
    const [openState, setOpenState] = useState(defaultOpen);
    const open = openProp ?? openState;
    const firstId = defaultStep ?? steps[0]?.id ?? '';
    const [stepState, setStepState] = useState(firstId);
    const currentId = stepProp ?? stepState;
    const index = Math.max(
      0,
      steps.findIndex((s) => s.id === currentId),
    );
    const current = steps[index];
    const [pending, setPending] = useState(false);
    const [failure, setFailure] = useState<ReactNode>(null);
    const headingRef = useRef<HTMLHeadingElement>(null);
    const moved = useRef(false);
    const mounted = useRef(true);
    const errorId = useId();
    const blockedId = useId();

    useEffect(() => {
      mounted.current = true;
      return () => {
        mounted.current = false;
      };
    }, []);

    const counted = steps.filter((s) => s.kind !== 'done');
    const position = counted.findIndex((s) => s.id === current?.id) + 1;
    const progress = (showProgress ?? counted.length > 1) && current?.kind !== 'done';
    const isDone = current?.kind === 'done';

    const setOpen = useCallback(
      (next: boolean) => {
        if (openProp === undefined) setOpenState(next);
        onOpenChange?.(next);
      },
      [openProp, onOpenChange],
    );

    const goTo = useCallback(
      (id: string) => {
        setFailure(null);
        moved.current = true;
        if (stepProp === undefined) setStepState(id);
        onStepChange?.(id);
      },
      [stepProp, onStepChange],
    );

    // Every opening starts at the first step with no old error.
    useEffect(() => {
      if (open) {
        setFailure(null);
        setPending(false);
        moved.current = false;
        if (stepProp === undefined) setStepState(firstId);
      }
      // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [open]);

    // A new step moves focus to its heading, so it is announced.
    useEffect(() => {
      if (moved.current) headingRef.current?.focus();
    }, [currentId]);

    const handleOpenChange = (next: boolean) => {
      if (!next && pending) return;
      if (!next && !isDone) onCancel?.();
      setOpen(next);
    };

    const toMessage = (error: unknown): ReactNode =>
      errorMessage
        ? errorMessage(error)
        : error instanceof Error && error.message
          ? error.message
          : messages.error;

    const advance = () => {
      const next = steps[index + 1];
      if (next) goTo(next.id);
      else {
        onComplete?.();
        setOpen(false);
      }
    };

    const submit = async (event?: FormEvent) => {
      event?.preventDefault();
      if (!current || pending || isDone) return;
      if (current.canContinue === false) return;
      setFailure(null);
      try {
        let result = current.onContinue?.();
        if (isThenable(result)) {
          setPending(true);
          result = await result;
        }
        if (!mounted.current) return;
        setPending(false);
        if (result === false) return;
      } catch (error) {
        if (mounted.current) {
          setPending(false);
          setFailure(toMessage(error));
        }
        return;
      }
      advance();
    };

    const back = () => {
      const previous = steps[index - 1];
      if (previous && !pending) goTo(previous.id);
    };

    const blocked = current?.canContinue === false;
    const showBack = index > 0 && !isDone && !current?.hideBack;

    return (
      <Dialog open={open} onOpenChange={handleOpenChange}>
        {trigger ? <DialogTrigger asChild>{trigger}</DialogTrigger> : null}
        <DialogContent
          ref={ref}
          {...rest}
          size={size}
          busy={pending}
          closeOnOutsideClick={closeOnOutsideClick}
          data-step={current?.id}
          data-kind={isDone ? 'done' : 'step'}
          className={cx('mn-wizard-dialog', className)}
        >
          <form className="mn-wizard-dialog__form" onSubmit={submit} noValidate>
            <DialogHeader className="mn-wizard-dialog__header">
              <DialogTitle>{title}</DialogTitle>
              {description ? <DialogDescription>{description}</DialogDescription> : null}
              {progress && position > 0 ? (
                <div className="mn-wizard-dialog__progress">
                  <span className="mn-wizard-dialog__progress-text">
                    {messages.stepOf(position, counted.length)}
                  </span>
                  <span className="mn-wizard-dialog__progress-bar" aria-hidden>
                    {counted.map((s, i) => (
                      <span
                        key={s.id}
                        className="mn-wizard-dialog__progress-segment"
                        data-state={
                          i + 1 < position
                            ? 'complete'
                            : i + 1 === position
                              ? 'current'
                              : 'upcoming'
                        }
                      />
                    ))}
                  </span>
                </div>
              ) : null}
            </DialogHeader>
            {current ? (
              <DialogBody className="mn-wizard-dialog__body">
                <section className="mn-wizard-dialog__step" aria-labelledby={`${errorId}-heading`}>
                  <h3
                    ref={headingRef}
                    id={`${errorId}-heading`}
                    tabIndex={-1}
                    className="mn-wizard-dialog__step-title"
                  >
                    {current.title}
                  </h3>
                  {current.description ? (
                    <p className="mn-wizard-dialog__step-description">{current.description}</p>
                  ) : null}
                  {current.content}
                </section>
                {failure ? (
                  <Alert tone="danger" live="assertive" id={errorId}>
                    {failure}
                  </Alert>
                ) : null}
              </DialogBody>
            ) : null}
            <DialogFooter className="mn-wizard-dialog__footer">
              {blocked && current?.blockedReason ? (
                <p id={blockedId} className="mn-wizard-dialog__blocked">
                  {current.blockedReason}
                </p>
              ) : null}
              {isDone ? (
                <DialogClose asChild>
                  <Button variant="primary">{messages.done}</Button>
                </DialogClose>
              ) : (
                <>
                  {showBack ? (
                    <Button
                      variant="ghost"
                      className="mn-wizard-dialog__back"
                      disabled={pending}
                      onClick={back}
                    >
                      {messages.back}
                    </Button>
                  ) : null}
                  <DialogClose asChild>
                    <Button disabled={pending}>{messages.cancel}</Button>
                  </DialogClose>
                  <Button
                    type="submit"
                    variant={current?.tone === 'danger' ? 'danger' : 'primary'}
                    loading={pending}
                    disabled={blocked}
                    aria-describedby={
                      cx(
                        failure ? errorId : undefined,
                        blocked && current?.blockedReason ? blockedId : undefined,
                      ) || undefined
                    }
                  >
                    {current?.continueLabel ?? messages.continue}
                  </Button>
                </>
              )}
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>
    );
  },
);
