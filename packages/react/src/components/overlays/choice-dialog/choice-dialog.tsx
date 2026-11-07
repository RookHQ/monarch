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
import { focusReturnTarget, isThenable } from '../../../internal/dialog-helpers.js';
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

/** One option the person can pick. */
export interface ChoiceDialogChoice {
  /** Stable value, passed to `onChoose`. */
  value: string;
  /** Short name of the option, e.g. "Share with the team". Its accessible name. */
  label: ReactNode;
  /** What picking it means. Its accessible description. */
  description?: ReactNode;
  /** `danger` marks a destructive option: the confirm button turns red while it is selected. */
  tone?: 'default' | 'danger';
  disabled?: boolean;
}

export interface ChoiceDialogProps extends Omit<
  DialogContentProps,
  'title' | 'children' | 'footer' | 'busy' | 'size' | 'defaultValue'
> {
  /** Controlled open state. */
  open?: boolean;
  /** Uncontrolled initial open state. */
  defaultOpen?: boolean;
  onOpenChange?: (open: boolean) => void;
  /** The element that opens the dialog (rendered with `asChild`), e.g. a Button. */
  trigger?: ReactElement;
  /** The question, e.g. "Who can see this view?". Becomes the accessible name and the group's label. */
  title: ReactNode;
  /** Context for the question. Becomes the accessible description. */
  description?: ReactNode;
  /** The options, shown as a group of radio cards in this order. */
  choices: readonly ChoiceDialogChoice[];
  /**
   * The option selected when the dialog opens. Leave it out when there is no
   * safe default: the person then has to pick one before confirming.
   */
  defaultValue?: string | undefined;
  /** Extra content between the description and the options. */
  children?: ReactNode;
  /**
   * Label of the confirming button. Name the action ("Save view"), not "OK".
   * A function receives the selected value (or `null`), so the label can
   * follow the choice. Default "Continue".
   */
  confirmLabel?: ReactNode | ((value: string | null) => ReactNode);
  /** Label of the button that closes without choosing. Default "Cancel". */
  cancelLabel?: ReactNode;
  /** Shown when the person confirms with nothing selected. Default "Choose an option to continue." */
  requiredMessage?: ReactNode;
  /**
   * Receives the chosen value. Return a promise to keep the dialog open in a
   * pending state until it settles: on success it closes (see
   * `closeOnChoose`), on rejection the error is shown inside the dialog and
   * the person can retry or pick again.
   */
  onChoose: (value: string) => unknown;
  /** Called when the dialog is dismissed without choosing. */
  onCancel?: () => void;
  /** Controlled pending state, for actions the product tracks itself. */
  pending?: boolean;
  /** Controlled error message, shown above the actions. */
  error?: ReactNode;
  /** Turns a rejected `onChoose` into a message. Default: the Error's message, or a generic line. */
  errorMessage?: (error: unknown) => ReactNode;
  /** Close after `onChoose` succeeds. Default `true`. */
  closeOnChoose?: boolean;
  /** Dialog width. Default `sm`. */
  size?: DialogContentProps['size'];
}

const DEFAULT_ERROR = 'That did not work. Try again.';

function defaultErrorMessage(error: unknown): ReactNode {
  return error instanceof Error && error.message ? error.message : DEFAULT_ERROR;
}

/**
 * Asks the person to pick one of a few labelled options, then confirm. It
 * replaces using `window.confirm` as a choice ("OK for shared, Cancel for
 * private"): every option is named, Cancel only ever means "do nothing", and
 * the options are a radio group with one Tab stop and arrow-key navigation.
 * An async `onChoose` keeps the dialog pending and shows failures in place.
 */
export const ChoiceDialog = forwardRef<ElementRef<typeof DialogContent>, ChoiceDialogProps>(
  function ChoiceDialog(
    {
      open: openProp,
      defaultOpen = false,
      onOpenChange,
      trigger,
      title,
      description,
      choices,
      defaultValue,
      children,
      confirmLabel = 'Continue',
      cancelLabel = 'Cancel',
      requiredMessage = 'Choose an option to continue.',
      onChoose,
      onCancel,
      pending: pendingProp,
      error: errorProp,
      errorMessage = defaultErrorMessage,
      closeOnChoose = true,
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
    const [value, setValue] = useState<string | null>(defaultValue ?? null);
    const [running, setRunning] = useState(false);
    const [failure, setFailure] = useState<ReactNode>(null);
    const [attempted, setAttempted] = useState(false);
    const groupRef = useRef<HTMLDivElement>(null);
    const mounted = useRef(true);
    const auto = useId();
    const titleId = `${auto}-title`;
    const errorId = `${auto}-error`;
    const missingId = `${auto}-missing`;

    useEffect(() => {
      mounted.current = true;
      return () => {
        mounted.current = false;
      };
    }, []);

    // Every opening starts from the default choice, with no old errors.
    useEffect(() => {
      if (open) {
        setValue(defaultValue ?? null);
        setFailure(null);
        setAttempted(false);
      }
      // The default is read when the dialog opens, not tracked while it is open.
      // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [open]);

    const pending = pendingProp ?? running;
    const shownError = errorProp ?? failure;
    const selected = choices.find((choice) => choice.value === value && !choice.disabled);
    const missing = attempted && !selected;

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

    const radios = () =>
      Array.from(groupRef.current?.querySelectorAll<HTMLInputElement>('input[type="radio"]') ?? []);

    const submit = async (event?: FormEvent) => {
      event?.preventDefault();
      if (pending) return;
      if (!selected) {
        setAttempted(true);
        radios()
          .find((radio) => !radio.disabled)
          ?.focus();
        return;
      }
      setFailure(null);
      try {
        const result = onChoose(selected.value);
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
      if (closeOnChoose) setOpen(false);
    };

    const label =
      typeof confirmLabel === 'function' ? confirmLabel(selected?.value ?? null) : confirmLabel;

    return (
      <Dialog open={open} onOpenChange={handleOpenChange}>
        {trigger ? <DialogTrigger asChild>{trigger}</DialogTrigger> : null}
        <DialogContent
          ref={ref}
          aria-labelledby={titleId}
          {...rest}
          size={size}
          busy={pending}
          closeLabel={closeLabel}
          closeOnOutsideClick={closeOnOutsideClick}
          className={cx('mn-choice-dialog', className)}
          onOpenAutoFocus={(event) => {
            onOpenAutoFocus?.(event);
            if (event.defaultPrevented) return;
            event.preventDefault();
            const all = radios();
            (all.find((radio) => radio.checked) ?? all.find((radio) => !radio.disabled))?.focus();
          }}
        >
          <form className="mn-choice-dialog__form" onSubmit={submit} noValidate>
            <DialogHeader className="mn-choice-dialog__header">
              <DialogTitle id={titleId}>{title}</DialogTitle>
              {description ? <DialogDescription>{description}</DialogDescription> : null}
            </DialogHeader>
            <DialogBody className="mn-choice-dialog__body">
              {children}
              <div
                ref={groupRef}
                role="radiogroup"
                aria-labelledby={titleId}
                aria-required
                aria-invalid={missing || undefined}
                aria-describedby={missing ? missingId : undefined}
                className="mn-choice-dialog__choices"
              >
                {choices.map((choice, index) => {
                  const checked = choice.value === value;
                  const labelId = `${auto}-${index}-label`;
                  const descId = `${auto}-${index}-desc`;
                  return (
                    <label
                      key={choice.value}
                      className="mn-choice-dialog__choice"
                      data-state={checked ? 'checked' : 'unchecked'}
                      data-tone={choice.tone === 'danger' ? 'danger' : undefined}
                      data-disabled={choice.disabled || undefined}
                    >
                      <input
                        type="radio"
                        name={auto}
                        value={choice.value}
                        checked={checked}
                        disabled={choice.disabled}
                        aria-labelledby={labelId}
                        aria-describedby={choice.description ? descId : undefined}
                        className="mn-choice-dialog__radio"
                        onChange={() => {
                          if (pending) return;
                          setValue(choice.value);
                          setAttempted(false);
                        }}
                      />
                      <span className="mn-choice-dialog__text">
                        <span id={labelId} className="mn-choice-dialog__label">
                          {choice.label}
                        </span>
                        {choice.description ? (
                          <span id={descId} className="mn-choice-dialog__description">
                            {choice.description}
                          </span>
                        ) : null}
                      </span>
                    </label>
                  );
                })}
              </div>
              {missing ? (
                <p id={missingId} className="mn-choice-dialog__missing" aria-live="polite">
                  {requiredMessage}
                </p>
              ) : null}
            </DialogBody>
            {shownError ? (
              <div className="mn-choice-dialog__error">
                <Alert tone="danger" live="assertive" id={errorId}>
                  {shownError}
                </Alert>
              </div>
            ) : null}
            <DialogFooter className="mn-choice-dialog__footer">
              <DialogClose asChild>
                <Button disabled={pending}>{cancelLabel}</Button>
              </DialogClose>
              <Button
                type="submit"
                variant={selected?.tone === 'danger' ? 'danger' : 'primary'}
                loading={pending}
                aria-describedby={shownError ? errorId : undefined}
              >
                {label}
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>
    );
  },
);

/** Options for one `choose()` call: the ChoiceDialog props that describe the question. */
export type ChoiceOptions = Omit<
  ChoiceDialogProps,
  'open' | 'defaultOpen' | 'onOpenChange' | 'trigger' | 'onChoose' | 'pending' | 'error'
> & {
  /** Optional async work to run inside the dialog, with pending and error states. */
  onChoose?: (value: string) => unknown;
};

export interface UseChoiceResult {
  /**
   * Opens the dialog and resolves with the chosen value once `onChoose`
   * succeeds, or `null` when the person cancels.
   */
  choose: (options: ChoiceOptions) => Promise<string | null>;
  /** Render this once, anywhere in the component. */
  element: ReactElement;
}

/**
 * Imperative choice: `const scope = await choose({ title, choices })`.
 * Resolves with the chosen value, or `null` when cancelled. Calling it again
 * while open, or unmounting, resolves the earlier call with `null`.
 */
export function useChoice(): UseChoiceResult {
  const [request, setRequest] = useState<{ id: number; options: ChoiceOptions } | null>(null);
  const [open, setOpen] = useState(false);
  const resolver = useRef<((value: string | null) => void) | null>(null);
  const counter = useRef(0);
  const returnFocus = useRef<HTMLElement | null>(null);

  const settle = useCallback((value: string | null) => {
    resolver.current?.(value);
    resolver.current = null;
  }, []);

  useEffect(() => () => settle(null), [settle]);

  const choose = useCallback(
    (options: ChoiceOptions) => {
      settle(null);
      returnFocus.current = focusReturnTarget();
      counter.current += 1;
      setRequest({ id: counter.current, options });
      setOpen(true);
      return new Promise<string | null>((resolve) => {
        resolver.current = resolve;
      });
    },
    [settle],
  );

  const options = request?.options;
  const element = (
    <ChoiceDialog
      key={request?.id ?? 0}
      title=""
      choices={[]}
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
      onChoose={async (value) => {
        await options?.onChoose?.(value);
        settle(value);
      }}
    />
  );

  return { choose, element };
}
