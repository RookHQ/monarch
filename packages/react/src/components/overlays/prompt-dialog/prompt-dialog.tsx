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
  type KeyboardEvent,
  type ReactElement,
  type ReactNode,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { focusReturnTarget, isThenable } from '../../../internal/dialog-helpers.js';
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

/** The kind of single-line value asked for. `url` and `email` are checked before `validate`. */
export type PromptInputType = 'text' | 'url' | 'email';

export interface PromptDialogProps extends Omit<
  DialogContentProps,
  'title' | 'children' | 'footer' | 'busy' | 'onSubmit' | 'size' | 'defaultValue'
> {
  /** Controlled open state. */
  open?: boolean;
  /** Uncontrolled initial open state. */
  defaultOpen?: boolean;
  onOpenChange?: (open: boolean) => void;
  /** The element that opens the dialog (rendered with `asChild`), e.g. a Button. */
  trigger?: ReactElement;
  /** What is being asked, e.g. "Name this view". Becomes the accessible name. */
  title: ReactNode;
  /** Context for the question. Becomes the accessible description. */
  description?: ReactNode;
  /** Visible label of the field, e.g. "Name". Required: it is the field's accessible name. */
  label: ReactNode;
  /** Help text under the label. */
  fieldDescription?: ReactNode;
  /** The value the field starts with each time the dialog opens. It is selected on open. */
  defaultValue?: string;
  placeholder?: string;
  /** `url` and `email` set the matching keyboard and check the format on submit. Default `text`. */
  inputType?: PromptInputType;
  /** A multi-line field (textarea). Enter adds a line; Ctrl+Enter or Cmd+Enter submits. */
  multiline?: boolean;
  /** Rows of a multi-line field. Default 4. */
  rows?: number;
  /** Submitting an empty (or only spaces) value shows `requiredMessage` instead. */
  required?: boolean;
  /** Maximum number of characters the field accepts. */
  maxLength?: number;
  /** `autocomplete` attribute of the field. Default `off`. */
  autoComplete?: string;
  /** Remove spaces at the start and end before validating and submitting. Default `true`. */
  trim?: boolean;
  /**
   * Checks the value when the person submits. Return a message to show it as
   * the field's error (announced and linked to the field), or `null` when the
   * value is fine. Runs after the required and format checks.
   */
  validate?: (value: string) => ReactNode | null | undefined;
  /** Shown when a required field is submitted empty. Default "Enter a value." */
  requiredMessage?: ReactNode;
  /** Shown when a `url` or `email` value is not in the right format. Default depends on `inputType`. */
  invalidMessage?: ReactNode;
  /** Label of the submitting button. Name the action ("Save view", "Rename"), not "OK". Default "Save". */
  submitLabel?: ReactNode;
  /** Default "Cancel". */
  cancelLabel?: ReactNode;
  /**
   * Receives the (trimmed) value. Return a promise to keep the dialog open in
   * a pending state until it settles: on success it closes (see
   * `closeOnSubmit`), on rejection the error is shown inside the dialog and
   * the person can edit the value and retry.
   */
  onSubmit: (value: string) => unknown;
  /** Called when the dialog is dismissed without submitting. */
  onCancel?: () => void;
  /** Controlled pending state, for actions the product tracks itself. */
  pending?: boolean;
  /** Controlled error message, shown above the actions. */
  error?: ReactNode;
  /** Turns a rejected `onSubmit` into a message. Default: the Error's message, or a generic line. */
  errorMessage?: (error: unknown) => ReactNode;
  /** Close after `onSubmit` succeeds. Default `true`. */
  closeOnSubmit?: boolean;
  /** Dialog width. Default `sm`. */
  size?: DialogContentProps['size'];
}

const DEFAULT_ERROR = 'That did not work. Try again.';
const DEFAULT_INVALID: Record<PromptInputType, string> = {
  text: 'Enter a valid value.',
  url: 'Enter a full web address, starting with https://.',
  email: 'Enter an email address, like name@example.com.',
};

function defaultErrorMessage(error: unknown): ReactNode {
  return error instanceof Error && error.message ? error.message : DEFAULT_ERROR;
}

/** Whether a value is an absolute http(s), mailto or tel address. */
function isUrl(value: string): boolean {
  try {
    const url = new URL(value);
    if (url.protocol === 'http:' || url.protocol === 'https:') return url.hostname !== '';
    return url.protocol === 'mailto:' || url.protocol === 'tel:';
  } catch {
    return false;
  }
}

const EMAIL = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

/** True while an input method editor (IME) is composing text: Enter then commits, not submits. */
function composing(event: KeyboardEvent): boolean {
  return event.nativeEvent.isComposing || event.keyCode === 229;
}

/**
 * Asks for one value, a themed and accessible replacement for
 * `window.prompt`: a name, a link, alt text or a note. Built on Dialog with a
 * labelled field that is focused (and its prefilled text selected) on open.
 * Enter submits a single-line field; Ctrl+Enter or Cmd+Enter submits a
 * multi-line one. Required, URL and email checks and a custom `validate` show
 * as field errors, and an async `onSubmit` keeps the dialog pending and shows
 * failures in place.
 */
export const PromptDialog = forwardRef<ElementRef<typeof DialogContent>, PromptDialogProps>(
  function PromptDialog(
    {
      open: openProp,
      defaultOpen = false,
      onOpenChange,
      trigger,
      title,
      description,
      label,
      fieldDescription,
      defaultValue = '',
      placeholder,
      inputType = 'text',
      multiline = false,
      rows = 4,
      required = false,
      maxLength,
      autoComplete = 'off',
      trim = true,
      validate,
      requiredMessage = 'Enter a value.',
      invalidMessage,
      submitLabel = 'Save',
      cancelLabel = 'Cancel',
      onSubmit,
      onCancel,
      pending: pendingProp,
      error: errorProp,
      errorMessage = defaultErrorMessage,
      closeOnSubmit = true,
      size = 'sm',
      closeOnOutsideClick = false,
      closeLabel = null,
      onOpenAutoFocus,
      onEscapeKeyDown,
      className,
      ...rest
    },
    ref,
  ) {
    const [openState, setOpenState] = useState(defaultOpen);
    const open = openProp ?? openState;
    const [value, setValue] = useState(defaultValue);
    const [running, setRunning] = useState(false);
    const [failure, setFailure] = useState<ReactNode>(null);
    const [fieldError, setFieldError] = useState<ReactNode>(null);
    const fieldRef = useRef<HTMLInputElement & HTMLTextAreaElement>(null);
    const composingRef = useRef(false);
    const mounted = useRef(true);
    const errorId = useId();

    useEffect(() => {
      mounted.current = true;
      return () => {
        mounted.current = false;
      };
    }, []);

    // Every opening starts from the default value, with no old errors.
    useEffect(() => {
      if (open) {
        setValue(defaultValue);
        setFailure(null);
        setFieldError(null);
      }
      // The default is read when the dialog opens, not tracked while it is open.
      // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [open]);

    const pending = pendingProp ?? running;
    const shownError = errorProp ?? failure;

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

    const check = (submitted: string): ReactNode => {
      if (submitted === '') return required ? requiredMessage : null;
      if (inputType === 'url' && !isUrl(submitted)) return invalidMessage ?? DEFAULT_INVALID.url;
      if (inputType === 'email' && !EMAIL.test(submitted))
        return invalidMessage ?? DEFAULT_INVALID.email;
      return validate?.(submitted) ?? null;
    };

    const submit = async (event?: FormEvent) => {
      event?.preventDefault();
      if (pending) return;
      const submitted = trim ? value.trim() : value;
      const problem = check(submitted);
      if (problem !== null && problem !== undefined && problem !== false && problem !== '') {
        setFieldError(problem);
        fieldRef.current?.focus();
        return;
      }
      setFieldError(null);
      setFailure(null);
      try {
        const result = onSubmit(submitted);
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
      if (closeOnSubmit) setOpen(false);
    };

    const onKeyDown = (event: KeyboardEvent<HTMLInputElement | HTMLTextAreaElement>) => {
      if (event.key !== 'Enter' || composing(event)) return;
      if (multiline && !(event.metaKey || event.ctrlKey)) return;
      event.preventDefault();
      void submit();
    };

    const fieldProps = {
      ref: fieldRef,
      value,
      onChange: (event: { target: { value: string } }) => {
        setValue(event.target.value);
        if (fieldError !== null) setFieldError(null);
      },
      onKeyDown,
      onCompositionStart: () => {
        composingRef.current = true;
      },
      onCompositionEnd: () => {
        composingRef.current = false;
      },
      placeholder,
      maxLength,
      readOnly: pending,
      autoComplete,
    };

    return (
      <Dialog open={open} onOpenChange={handleOpenChange}>
        {trigger ? <DialogTrigger asChild>{trigger}</DialogTrigger> : null}
        <DialogContent
          ref={ref}
          {...rest}
          size={size}
          busy={pending}
          closeLabel={closeLabel}
          closeOnOutsideClick={closeOnOutsideClick}
          data-multiline={multiline || undefined}
          className={cx('mn-prompt-dialog', className)}
          onEscapeKeyDown={(event) => {
            // Escape while an IME is composing cancels the composition only.
            if (composingRef.current) event.preventDefault();
            onEscapeKeyDown?.(event);
          }}
          onOpenAutoFocus={(event) => {
            onOpenAutoFocus?.(event);
            if (event.defaultPrevented) return;
            event.preventDefault();
            const field = fieldRef.current;
            if (!field) return;
            field.focus();
            field.select();
          }}
        >
          <form className="mn-prompt-dialog__form" onSubmit={submit} noValidate>
            <DialogHeader className="mn-prompt-dialog__header">
              <DialogTitle>{title}</DialogTitle>
              {description ? <DialogDescription>{description}</DialogDescription> : null}
            </DialogHeader>
            <DialogBody className="mn-prompt-dialog__body">
              <Field required={required} invalid={fieldError !== null}>
                <FieldLabel>{label}</FieldLabel>
                {fieldDescription ? <FieldDescription>{fieldDescription}</FieldDescription> : null}
                {multiline ? (
                  <Textarea {...fieldProps} rows={rows} />
                ) : (
                  <Input
                    {...fieldProps}
                    type={inputType}
                    inputMode={inputType === 'text' ? undefined : inputType}
                    autoCapitalize={inputType === 'text' ? undefined : 'off'}
                    autoCorrect={inputType === 'text' ? undefined : 'off'}
                    spellCheck={inputType === 'text' ? undefined : false}
                  />
                )}
                <FieldError>{fieldError}</FieldError>
              </Field>
            </DialogBody>
            {shownError ? (
              <div className="mn-prompt-dialog__error">
                <Alert tone="danger" live="assertive" id={errorId}>
                  {shownError}
                </Alert>
              </div>
            ) : null}
            <DialogFooter className="mn-prompt-dialog__footer">
              <DialogClose asChild>
                <Button disabled={pending}>{cancelLabel}</Button>
              </DialogClose>
              <Button
                type="submit"
                variant="primary"
                loading={pending}
                aria-describedby={shownError ? errorId : undefined}
              >
                {submitLabel}
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>
    );
  },
);

/** Options for one `prompt()` call: the PromptDialog props that describe the question. */
export type PromptOptions = Omit<
  PromptDialogProps,
  'open' | 'defaultOpen' | 'onOpenChange' | 'trigger' | 'onSubmit' | 'pending' | 'error'
> & {
  /** Optional async work to run inside the dialog (e.g. saving), with pending and error states. */
  onSubmit?: (value: string) => unknown;
};

export interface UsePromptResult {
  /**
   * Opens the dialog and resolves with the submitted value once it passes
   * validation and `onSubmit` succeeds, or `null` when the person cancels.
   */
  prompt: (options: PromptOptions) => Promise<string | null>;
  /** Render this once, anywhere in the component. */
  element: ReactElement;
}

/**
 * Imperative prompt: `const name = await prompt({ title: 'Rename group', label: 'Name' })`.
 * A replacement for `window.prompt` that resolves with the value, or `null`
 * when cancelled. Calling it again while open, or unmounting, resolves the
 * earlier call with `null`.
 */
export function usePrompt(): UsePromptResult {
  const [request, setRequest] = useState<{ id: number; options: PromptOptions } | null>(null);
  const [open, setOpen] = useState(false);
  const resolver = useRef<((value: string | null) => void) | null>(null);
  const counter = useRef(0);
  const returnFocus = useRef<HTMLElement | null>(null);

  const settle = useCallback((value: string | null) => {
    resolver.current?.(value);
    resolver.current = null;
  }, []);

  useEffect(() => () => settle(null), [settle]);

  const prompt = useCallback(
    (options: PromptOptions) => {
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
    <PromptDialog
      key={request?.id ?? 0}
      title=""
      label=""
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
      onSubmit={async (value) => {
        await options?.onSubmit?.(value);
        settle(value);
      }}
    />
  );

  return { prompt, element };
}
