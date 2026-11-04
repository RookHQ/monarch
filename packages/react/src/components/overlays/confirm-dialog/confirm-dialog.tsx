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
import { WarningGlyph } from '../../../internal/icons.js';
import { Button } from '../../actions/button/index.js';
import { Alert } from '../../feedback/alert/index.js';
import { Field, FieldDescription, FieldError, FieldLabel } from '../../forms/form-field/index.js';
import { Input, Textarea } from '../../forms/text-field/index.js';
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

/** What the person entered before confirming. */
export interface ConfirmDetails {
  /** The trimmed reason, when a `reason` field is shown and filled in. */
  reason?: string;
}

/** An optional or required free-text reason, sent to `onConfirm`. */
export interface ConfirmReasonField {
  /** Visible label of the field. */
  label: ReactNode;
  /** Help text under the label. */
  description?: ReactNode;
  placeholder?: string;
  /** The confirm button stays disabled until the reason has text. */
  required?: boolean;
  /** Default 1000 characters. */
  maxLength?: number;
  /** Shown when the person tries to confirm without a required reason. */
  requiredMessage?: ReactNode;
}

export interface ConfirmDialogProps extends Omit<
  DialogContentProps,
  'title' | 'children' | 'footer' | 'busy' | 'onSubmit' | 'size'
> {
  /** Controlled open state. */
  open?: boolean;
  /** Uncontrolled initial open state. */
  defaultOpen?: boolean;
  onOpenChange?: (open: boolean) => void;
  /** The element that opens the dialog (rendered with `asChild`), e.g. a Button. */
  trigger?: ReactElement;
  /** The question, e.g. "Delete this project?". Becomes the accessible name. */
  title: ReactNode;
  /** What will happen. Becomes the accessible description. */
  description?: ReactNode;
  /** Concrete consequences, shown as a list ("12 tasks will be deleted"). */
  consequences?: ReactNode[];
  /** Extra content between the description and the fields, e.g. a step-up slot. */
  children?: ReactNode;
  /** `danger` for destructive or irreversible actions: red confirm button and a warning icon. */
  tone?: 'default' | 'danger';
  /** Label of the confirming button. Name the action ("Delete project"), not "OK". Default "Confirm". */
  confirmLabel?: ReactNode;
  /** Default "Cancel". */
  cancelLabel?: ReactNode;
  /**
   * Type-to-confirm: the confirm button stays disabled until this exact
   * phrase (case-sensitive, surrounding spaces ignored) is typed.
   */
  confirmPhrase?: string;
  /** Label of the phrase field. Default: Type "<phrase>" to confirm. */
  confirmPhraseLabel?: ReactNode;
  /** Shown when the person submits a phrase that does not match. */
  confirmPhraseMismatch?: ReactNode;
  /** Ask for a reason (optional or required); it is passed to `onConfirm`. */
  reason?: ConfirmReasonField;
  /**
   * Runs the action. Return a promise to keep the dialog open in a pending
   * state until it settles: on success it closes (see `closeOnConfirm`), on
   * rejection the error is shown inside the dialog and the person can retry.
   */
  onConfirm: (details: ConfirmDetails) => unknown;
  /** Called when the dialog is dismissed without confirming. */
  onCancel?: () => void;
  /** Controlled pending state, for actions the product tracks itself. */
  pending?: boolean;
  /** Controlled error message, shown above the actions. */
  error?: ReactNode;
  /** Turns a rejected `onConfirm` into a message. Default: the Error's message, or a generic line. */
  errorMessage?: (error: unknown) => ReactNode;
  /** Close after `onConfirm` succeeds. Default `true`. */
  closeOnConfirm?: boolean;
  /**
   * Where focus goes on open. `cancel` (default) is the least destructive
   * action; `field` focuses the phrase or reason field when there is one.
   */
  initialFocus?: 'cancel' | 'field';
  /** Dialog width. Default `sm`. */
  size?: DialogContentProps['size'];
}

const DEFAULT_ERROR = 'That did not work. Try again.';

function defaultErrorMessage(error: unknown): ReactNode {
  return error instanceof Error && error.message ? error.message : DEFAULT_ERROR;
}

function isThenable(value: unknown): value is PromiseLike<unknown> {
  return (
    typeof value === 'object' &&
    value !== null &&
    typeof (value as { then?: unknown }).then === 'function'
  );
}

/**
 * Confirmation for consequential actions, built on Dialog with
 * `role="alertdialog"`. Supports a danger tone, a consequence list,
 * type-to-confirm, an optional or required reason, and async actions with
 * pending and error states. Focus starts on Cancel, the least destructive
 * action, and nothing dismisses the dialog while the action runs.
 */
export const ConfirmDialog = forwardRef<ElementRef<typeof DialogContent>, ConfirmDialogProps>(
  function ConfirmDialog(
    {
      open: openProp,
      defaultOpen = false,
      onOpenChange,
      trigger,
      title,
      description,
      consequences,
      children,
      tone = 'default',
      confirmLabel = 'Confirm',
      cancelLabel = 'Cancel',
      confirmPhrase,
      confirmPhraseLabel,
      confirmPhraseMismatch = 'The text does not match.',
      reason,
      onConfirm,
      onCancel,
      pending: pendingProp,
      error: errorProp,
      errorMessage = defaultErrorMessage,
      closeOnConfirm = true,
      initialFocus = 'cancel',
      size = 'sm',
      closeOnOutsideClick = false,
      closeLabel = null,
      onOpenAutoFocus,
      className,
      ...rest
    },
    ref,
  ) {
    const [openState, setOpenState] = useState(defaultOpen);
    const open = openProp ?? openState;
    const [phrase, setPhrase] = useState('');
    const [reasonText, setReasonText] = useState('');
    const [running, setRunning] = useState(false);
    const [failure, setFailure] = useState<ReactNode>(null);
    const [attempted, setAttempted] = useState(false);
    const cancelRef = useRef<HTMLButtonElement>(null);
    const fieldRef = useRef<HTMLInputElement & HTMLTextAreaElement>(null);
    const mounted = useRef(true);
    const errorId = useId();

    useEffect(() => {
      mounted.current = true;
      return () => {
        mounted.current = false;
      };
    }, []);

    // Every opening starts clean: no typed phrase, reason or old error.
    useEffect(() => {
      if (open) {
        setPhrase('');
        setReasonText('');
        setFailure(null);
        setAttempted(false);
      }
    }, [open]);

    const pending = pendingProp ?? running;
    const shownError = errorProp ?? failure;
    const phraseOk = confirmPhrase === undefined || phrase.trim() === confirmPhrase.trim();
    const reasonOk = !reason?.required || reasonText.trim() !== '';
    const blocked = !phraseOk || !reasonOk;

    const setOpen = useCallback(
      (next: boolean) => {
        if (openProp === undefined) setOpenState(next);
        onOpenChange?.(next);
      },
      [openProp, onOpenChange],
    );

    const handleOpenChange = (next: boolean) => {
      if (!next && pending) return;
      if (!next) onCancel?.();
      setOpen(next);
    };

    const submit = async (event?: FormEvent) => {
      event?.preventDefault();
      if (pending) return;
      if (blocked) {
        setAttempted(true);
        return;
      }
      setFailure(null);
      const trimmed = reasonText.trim();
      const details: ConfirmDetails = reason && trimmed !== '' ? { reason: trimmed } : {};
      try {
        const result = onConfirm(details);
        if (isThenable(result)) {
          setRunning(true);
          await result;
        }
      } catch (error) {
        if (mounted.current) {
          setRunning(false);
          setFailure(errorMessage(error));
        }
        return;
      }
      if (!mounted.current) return;
      setRunning(false);
      if (closeOnConfirm) setOpen(false);
    };

    const hasField = confirmPhrase !== undefined || reason !== undefined;
    const hasBody = children !== undefined || (consequences && consequences.length > 0) || hasField;

    return (
      <Dialog open={open} onOpenChange={handleOpenChange}>
        {trigger ? <DialogTrigger asChild>{trigger}</DialogTrigger> : null}
        <DialogContent
          ref={ref}
          role="alertdialog"
          {...rest}
          size={size}
          busy={pending}
          closeLabel={closeLabel}
          closeOnOutsideClick={closeOnOutsideClick}
          data-tone={tone}
          className={cx('mn-confirm-dialog', className)}
          onOpenAutoFocus={(event) => {
            onOpenAutoFocus?.(event);
            if (event.defaultPrevented) return;
            event.preventDefault();
            const target = initialFocus === 'field' && fieldRef.current ? fieldRef.current : null;
            (target ?? cancelRef.current)?.focus();
          }}
        >
          <form className="mn-confirm-dialog__form" onSubmit={submit} noValidate>
            <DialogHeader className="mn-confirm-dialog__header">
              {tone === 'danger' ? (
                <span className="mn-confirm-dialog__icon" aria-hidden>
                  <WarningGlyph />
                </span>
              ) : null}
              <DialogTitle>{title}</DialogTitle>
              {description ? <DialogDescription>{description}</DialogDescription> : null}
            </DialogHeader>
            {hasBody ? (
              <DialogBody className="mn-confirm-dialog__body">
                {children}
                {consequences && consequences.length > 0 ? (
                  <ul className="mn-confirm-dialog__consequences">
                    {consequences.map((item, index) => (
                      <li key={index}>{item}</li>
                    ))}
                  </ul>
                ) : null}
                {reason ? (
                  <Field required={reason.required ?? false} invalid={attempted && !reasonOk}>
                    <FieldLabel>{reason.label}</FieldLabel>
                    {reason.description ? (
                      <FieldDescription>{reason.description}</FieldDescription>
                    ) : null}
                    <Textarea
                      ref={confirmPhrase === undefined ? fieldRef : undefined}
                      value={reasonText}
                      onChange={(event) => {
                        setReasonText(event.target.value);
                        if (attempted) setAttempted(false);
                      }}
                      placeholder={reason.placeholder}
                      maxLength={reason.maxLength ?? 1000}
                      readOnly={pending}
                      rows={3}
                    />
                    <FieldError>
                      {attempted && !reasonOk
                        ? (reason.requiredMessage ?? 'Enter a reason to continue.')
                        : null}
                    </FieldError>
                  </Field>
                ) : null}
                {confirmPhrase !== undefined ? (
                  <Field invalid={attempted && !phraseOk}>
                    <FieldLabel>
                      {confirmPhraseLabel ?? (
                        <>
                          Type <span className="mn-confirm-dialog__phrase">{confirmPhrase}</span> to
                          confirm
                        </>
                      )}
                    </FieldLabel>
                    <Input
                      ref={fieldRef}
                      value={phrase}
                      onChange={(event) => {
                        setPhrase(event.target.value);
                        if (attempted) setAttempted(false);
                      }}
                      onKeyDown={(event) => {
                        // A disabled submit button blocks implicit submission, so
                        // handle Enter here to report a mismatch instead of ignoring it.
                        if (event.key === 'Enter' && !event.nativeEvent.isComposing) {
                          event.preventDefault();
                          void submit();
                        }
                      }}
                      readOnly={pending}
                      autoComplete="off"
                      autoCapitalize="off"
                      autoCorrect="off"
                      spellCheck={false}
                    />
                    <FieldError>{attempted && !phraseOk ? confirmPhraseMismatch : null}</FieldError>
                  </Field>
                ) : null}
              </DialogBody>
            ) : null}
            {shownError ? (
              <div className="mn-confirm-dialog__error">
                <Alert tone="danger" live="assertive" id={errorId}>
                  {shownError}
                </Alert>
              </div>
            ) : null}
            <DialogFooter className="mn-confirm-dialog__footer">
              <DialogClose asChild>
                <Button ref={cancelRef} disabled={pending}>
                  {cancelLabel}
                </Button>
              </DialogClose>
              <Button
                type="submit"
                variant={tone === 'danger' ? 'danger' : 'primary'}
                loading={pending}
                disabled={blocked}
                aria-describedby={shownError ? errorId : undefined}
              >
                {confirmLabel}
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>
    );
  },
);

/** Options for one `confirm()` call: the ConfirmDialog props that describe the question. */
export type ConfirmOptions = Omit<
  ConfirmDialogProps,
  'open' | 'defaultOpen' | 'onOpenChange' | 'trigger' | 'onConfirm' | 'pending' | 'error'
> & {
  /** Optional async work to run inside the dialog, with pending and error states. */
  onConfirm?: (details: ConfirmDetails) => unknown;
};

export interface UseConfirmResult {
  /**
   * Opens the dialog and resolves with the details (`{ reason }`) when the
   * person confirms and `onConfirm` succeeds, or `null` when they cancel.
   */
  confirm: (options: ConfirmOptions) => Promise<ConfirmDetails | null>;
  /** Render this once, anywhere in the component. */
  element: ReactElement;
}

/**
 * Where focus should return when the dialog closes: the focused element, or,
 * when that is an item of a menu that is about to close, the menu's trigger.
 */
function focusReturnTarget(): HTMLElement | null {
  if (typeof document === 'undefined') return null;
  const active = document.activeElement instanceof HTMLElement ? document.activeElement : null;
  const menuOwner = active?.closest('[role="menu"]')?.getAttribute('aria-labelledby');
  const trigger = menuOwner ? document.getElementById(menuOwner) : null;
  return trigger ?? active;
}

/**
 * Imperative confirmation: `if (await confirm({ title: 'Delete note?' }))`.
 * A replacement for `window.confirm` and `window.prompt` that is themed,
 * accessible and can show consequences, a reason field and async progress.
 */
export function useConfirm(): UseConfirmResult {
  const [request, setRequest] = useState<{ id: number; options: ConfirmOptions } | null>(null);
  const [open, setOpen] = useState(false);
  const resolver = useRef<((value: ConfirmDetails | null) => void) | null>(null);
  const counter = useRef(0);
  const returnFocus = useRef<HTMLElement | null>(null);

  const settle = useCallback((value: ConfirmDetails | null) => {
    resolver.current?.(value);
    resolver.current = null;
  }, []);

  useEffect(() => () => settle(null), [settle]);

  const confirm = useCallback(
    (options: ConfirmOptions) => {
      settle(null);
      returnFocus.current = focusReturnTarget();
      counter.current += 1;
      setRequest({ id: counter.current, options });
      setOpen(true);
      return new Promise<ConfirmDetails | null>((resolve) => {
        resolver.current = resolve;
      });
    },
    [settle],
  );

  const options = request?.options;
  const element = (
    <ConfirmDialog
      key={request?.id ?? 0}
      title=""
      {...options}
      open={open && request !== null}
      onCloseAutoFocus={(event) => {
        options?.onCloseAutoFocus?.(event);
        const target = returnFocus.current;
        if (event.defaultPrevented || !target?.isConnected) return;
        event.preventDefault();
        target.focus();
      }}
      onOpenChange={(next) => {
        setOpen(next);
        if (!next) settle(null);
      }}
      onConfirm={async (details) => {
        await options?.onConfirm?.(details);
        settle(details);
      }}
    />
  );

  return { confirm, element };
}
