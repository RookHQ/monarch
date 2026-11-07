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
import { EyeGlyph, EyeOffGlyph, LockGlyph } from '../../../internal/icons.js';
import { Button, IconButton } from '../../actions/button/index.js';
import { Alert } from '../../feedback/alert/index.js';
import { Field, FieldDescription, FieldError, FieldLabel } from '../../forms/form-field/index.js';
import { OTPPinInput } from '../../forms/otp-pin-input/index.js';
import { SegmentedControl } from '../../forms/segmented-control/index.js';
import { Input } from '../../forms/text-field/index.js';
import { Link } from '../../navigation/link/index.js';
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

/** A way to re-authenticate. */
export type StepUpMethod = 'password' | 'totp' | 'passkey';

/** What the person entered, passed to `onVerify`. */
export type StepUpCredential =
  | { method: 'password'; password: string }
  | { method: 'totp'; code: string }
  | { method: 'passkey' };

/** Every string the dialog shows or announces, for translation. */
export interface StepUpAuthMessages {
  /** Default "Confirm it's you". */
  title: string;
  /** Default "This action needs you to confirm your identity again." */
  description: string;
  /** Name of the method switcher. Default "Verification method". */
  methodSwitcher: string;
  /** Method names in the switcher. Default "Password", "Authenticator app", "Passkey". */
  methodNames: Record<StepUpMethod, string>;
  /** Default "Password". */
  passwordLabel: string;
  /** Name of the show/hide toggle (a pressed state says which). Default "Show password". */
  showPassword: string;
  /** Shown when Confirm is pressed with an empty password. Default "Enter your password." */
  passwordRequired: string;
  /** Default "Authentication code". */
  codeLabel: string;
  /** Help under the code field. Default "Enter the 6-digit code from your authenticator app." */
  codeDescription: string;
  /** Shown when Confirm is pressed with an incomplete code. Default "Enter all 6 digits." */
  codeIncomplete: string;
  /** Text shown for the passkey method. Default "Use your passkey, security key or device screen lock." */
  passkeyDescription: string;
  /** The passkey button. Default "Use passkey". */
  passkeyButton: string;
  /** The confirm button for password and code. Default "Confirm". */
  submit: string;
  /** Default "Cancel". */
  cancel: string;
  /** The forgot link under the password field. Default "Forgot your password?" */
  forgot: string;
  /** Used when `onVerify` rejects with something that is not an Error with a message. Default "That did not work. Try again." */
  genericError: string;
}

const DEFAULT_MESSAGES: StepUpAuthMessages = {
  title: 'Confirm it’s you',
  description: 'This action needs you to confirm your identity again.',
  methodSwitcher: 'Verification method',
  methodNames: { password: 'Password', totp: 'Authenticator app', passkey: 'Passkey' },
  passwordLabel: 'Password',
  showPassword: 'Show password',
  passwordRequired: 'Enter your password.',
  codeLabel: 'Authentication code',
  codeDescription: 'Enter the 6-digit code from your authenticator app.',
  codeIncomplete: 'Enter all 6 digits.',
  passkeyDescription: 'Use your passkey, security key or device screen lock.',
  passkeyButton: 'Use passkey',
  submit: 'Confirm',
  cancel: 'Cancel',
  forgot: 'Forgot your password?',
  genericError: 'That did not work. Try again.',
};

const CODE_LENGTH = 6;

export interface StepUpAuthDialogProps extends Omit<
  DialogContentProps,
  'title' | 'children' | 'footer' | 'busy' | 'onSubmit' | 'role' | 'size'
> {
  /** Controlled open state. */
  open?: boolean;
  /** Uncontrolled initial open state. */
  defaultOpen?: boolean;
  onOpenChange?: (open: boolean) => void;
  /** The element that opens the dialog (rendered with `asChild`), e.g. a Button. */
  trigger?: ReactElement;
  /**
   * The methods the person can use, most preferred first; the first one is
   * shown when the dialog opens. With more than one, a switcher lets the
   * person pick another. Default `['password']`.
   */
  methods?: readonly StepUpMethod[];
  /** Heading and accessible name. Default "Confirm it's you". */
  title?: ReactNode;
  /** Why they are asked, e.g. "Deleting a workspace needs your password." Becomes the accessible description. */
  description?: ReactNode;
  /**
   * Checks the credential. Resolve (or return) when it is accepted: the
   * dialog closes and `onVerified` runs. Reject (or throw) to show the error
   * inside the dialog; the code is cleared and focus returns to the field.
   * For `passkey`, run the WebAuthn ceremony here.
   */
  onVerify: (credential: StepUpCredential) => unknown;
  /** Called after `onVerify` succeeds, just before the dialog closes. */
  onVerified?: () => void;
  /** Called when the dialog is dismissed without verifying. */
  onCancel?: () => void;
  /** Turns a rejected `onVerify` into a message. Default: the Error's message, or `messages.genericError`. */
  errorMessage?: (error: unknown) => ReactNode;
  /** Link under the password field to recover the password. */
  forgotHref?: string;
  /** Called by the forgot button when there is no `forgotHref`. */
  onForgot?: () => void;
  /** Override any built-in string. */
  messages?: Partial<StepUpAuthMessages>;
  /** Dialog width. Default `sm`. */
  size?: DialogContentProps['size'];
}

/**
 * Asks the person to re-authenticate before a high-risk action (deleting a
 * workspace or account, erasing data, revoking keys, viewing a secret). It
 * supports a password (with show/hide), a 6-digit authenticator code that
 * submits itself when complete, and a passkey, with a switcher when several
 * are allowed. Verification runs inside the dialog: it cannot be dismissed
 * while checking, and a failure is announced in place so the person can retry.
 */
export const StepUpAuthDialog = forwardRef<ElementRef<typeof DialogContent>, StepUpAuthDialogProps>(
  function StepUpAuthDialog(
    {
      open: openProp,
      defaultOpen = false,
      onOpenChange,
      trigger,
      methods = ['password'],
      title,
      description,
      onVerify,
      onVerified,
      onCancel,
      errorMessage,
      forgotHref,
      onForgot,
      messages: messagesProp,
      size = 'sm',
      closeOnOutsideClick = false,
      closeLabel = null,
      onOpenAutoFocus,
      className,
      ...rest
    },
    ref,
  ) {
    const m = { ...DEFAULT_MESSAGES, ...messagesProp };
    const methodNames = { ...DEFAULT_MESSAGES.methodNames, ...messagesProp?.methodNames };
    const available: readonly StepUpMethod[] = methods.length > 0 ? methods : ['password'];
    const firstMethod = available[0] ?? 'password';

    const [openState, setOpenState] = useState(defaultOpen);
    const open = openProp ?? openState;
    const [method, setMethod] = useState<StepUpMethod>(firstMethod);
    const [password, setPassword] = useState('');
    const [revealed, setRevealed] = useState(false);
    const [code, setCode] = useState('');
    const [verifying, setVerifying] = useState(false);
    const [failure, setFailure] = useState<ReactNode>(null);
    const [missing, setMissing] = useState(false);
    const passwordRef = useRef<HTMLInputElement>(null);
    const codeRef = useRef<HTMLInputElement>(null);
    const passkeyRef = useRef<HTMLButtonElement>(null);
    const mounted = useRef(true);
    const busy = useRef(false);
    const errorId = useId();

    useEffect(() => {
      mounted.current = true;
      return () => {
        mounted.current = false;
      };
    }, []);

    // Every opening starts clean, on the preferred method.
    useEffect(() => {
      if (open) {
        setMethod(firstMethod);
        setPassword('');
        setRevealed(false);
        setCode('');
        setFailure(null);
        setMissing(false);
      }
      // Only on open: changing `methods` while open must not wipe what was typed.
      // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [open]);

    const current: StepUpMethod = available.includes(method) ? method : firstMethod;

    const setOpen = useCallback(
      (next: boolean) => {
        if (openProp === undefined) setOpenState(next);
        onOpenChange?.(next);
      },
      [openProp, onOpenChange],
    );

    const handleOpenChange = (next: boolean) => {
      if (!next && verifying) return;
      if (!next) onCancel?.();
      setOpen(next);
    };

    const focusField = (which: StepUpMethod) => {
      const target =
        which === 'password'
          ? passwordRef.current
          : which === 'totp'
            ? codeRef.current
            : passkeyRef.current;
      target?.focus();
      if (which === 'password') passwordRef.current?.select();
    };

    const verify = async (credential: StepUpCredential) => {
      if (busy.current) return;
      busy.current = true;
      setFailure(null);
      setMissing(false);
      try {
        const result = onVerify(credential);
        if (isThenable(result)) {
          setVerifying(true);
          await result;
        }
      } catch (error) {
        busy.current = false;
        if (!mounted.current) return;
        setVerifying(false);
        setFailure(
          errorMessage
            ? errorMessage(error)
            : error instanceof Error && error.message
              ? error.message
              : m.genericError,
        );
        if (credential.method === 'totp') setCode('');
        focusField(credential.method);
        return;
      }
      busy.current = false;
      if (!mounted.current) return;
      setVerifying(false);
      onVerified?.();
      setOpen(false);
    };

    const submit = (event?: FormEvent) => {
      event?.preventDefault();
      if (busy.current) return;
      if (current === 'password') {
        if (password === '') {
          setMissing(true);
          passwordRef.current?.focus();
          return;
        }
        void verify({ method: 'password', password });
      } else if (current === 'totp') {
        if (code.length < CODE_LENGTH) {
          setMissing(true);
          codeRef.current?.focus();
          return;
        }
        void verify({ method: 'totp', code });
      } else {
        void verify({ method: 'passkey' });
      }
    };

    const describedByError = failure ? errorId : undefined;
    const fieldInvalid = missing || failure !== null;

    return (
      <Dialog open={open} onOpenChange={handleOpenChange}>
        {trigger ? <DialogTrigger asChild>{trigger}</DialogTrigger> : null}
        <DialogContent
          ref={ref}
          {...rest}
          size={size}
          busy={verifying}
          closeLabel={closeLabel}
          closeOnOutsideClick={closeOnOutsideClick}
          data-method={current}
          className={cx('mn-step-up-auth-dialog', className)}
          onOpenAutoFocus={(event) => {
            onOpenAutoFocus?.(event);
            if (event.defaultPrevented) return;
            event.preventDefault();
            // Only the active method's control is mounted.
            (passwordRef.current ?? codeRef.current ?? passkeyRef.current)?.focus();
          }}
        >
          <form className="mn-step-up-auth-dialog__form" onSubmit={submit} noValidate>
            <DialogHeader className="mn-step-up-auth-dialog__header">
              <span className="mn-step-up-auth-dialog__icon" aria-hidden>
                <LockGlyph />
              </span>
              <DialogTitle>{title ?? m.title}</DialogTitle>
              <DialogDescription>{description ?? m.description}</DialogDescription>
            </DialogHeader>
            <DialogBody className="mn-step-up-auth-dialog__body">
              {available.length > 1 ? (
                <SegmentedControl
                  aria-label={m.methodSwitcher}
                  className="mn-step-up-auth-dialog__methods"
                  block
                  size="sm"
                  disabled={verifying}
                  options={available.map((value) => ({ value, label: methodNames[value] }))}
                  value={current}
                  onValueChange={(next) => {
                    setMethod(next);
                    setFailure(null);
                    setMissing(false);
                  }}
                />
              ) : null}
              {current === 'password' ? (
                <Field invalid={fieldInvalid} required>
                  <FieldLabel>{m.passwordLabel}</FieldLabel>
                  <div
                    className="mn-step-up-auth-dialog__password"
                    data-revealed={revealed || undefined}
                  >
                    <Input
                      ref={passwordRef}
                      type={revealed ? 'text' : 'password'}
                      name="password"
                      autoComplete="current-password"
                      autoCapitalize="off"
                      autoCorrect="off"
                      spellCheck={false}
                      value={password}
                      onChange={(event) => {
                        setPassword(event.target.value);
                        if (missing) setMissing(false);
                      }}
                      readOnly={verifying}
                      aria-describedby={describedByError}
                      className="mn-step-up-auth-dialog__password-control"
                    />
                    <IconButton
                      label={m.showPassword}
                      icon={revealed ? <EyeOffGlyph /> : <EyeGlyph />}
                      size="xs"
                      aria-pressed={revealed}
                      disabled={verifying}
                      onClick={() => setRevealed(!revealed)}
                      className="mn-step-up-auth-dialog__toggle"
                    />
                  </div>
                  <FieldError>{missing ? m.passwordRequired : null}</FieldError>
                  {forgotHref || onForgot ? (
                    <div className="mn-step-up-auth-dialog__forgot">
                      {forgotHref ? (
                        <Link href={forgotHref} variant="standalone" onClick={onForgot}>
                          {m.forgot}
                        </Link>
                      ) : (
                        <Button variant="ghost" size="sm" onClick={onForgot} disabled={verifying}>
                          {m.forgot}
                        </Button>
                      )}
                    </div>
                  ) : null}
                </Field>
              ) : null}
              {current === 'totp' ? (
                <Field invalid={fieldInvalid} required>
                  <FieldLabel>{m.codeLabel}</FieldLabel>
                  <FieldDescription>{m.codeDescription}</FieldDescription>
                  <OTPPinInput
                    ref={codeRef}
                    length={CODE_LENGTH}
                    groupSize={3}
                    value={code}
                    onValueChange={(next) => {
                      setCode(next);
                      if (missing) setMissing(false);
                    }}
                    onComplete={(next) => void verify({ method: 'totp', code: next })}
                    readOnly={verifying}
                    aria-describedby={describedByError}
                    className="mn-step-up-auth-dialog__code"
                  />
                  <FieldError>{missing ? m.codeIncomplete : null}</FieldError>
                </Field>
              ) : null}
              {current === 'passkey' ? (
                <p className="mn-step-up-auth-dialog__passkey">{m.passkeyDescription}</p>
              ) : null}
            </DialogBody>
            {failure ? (
              <div className="mn-step-up-auth-dialog__error">
                <Alert tone="danger" live="assertive" id={errorId}>
                  {failure}
                </Alert>
              </div>
            ) : null}
            <DialogFooter className="mn-step-up-auth-dialog__footer">
              <DialogClose asChild>
                <Button disabled={verifying}>{m.cancel}</Button>
              </DialogClose>
              <Button
                ref={current === 'passkey' ? passkeyRef : undefined}
                type="submit"
                variant="primary"
                loading={verifying}
                aria-describedby={current === 'passkey' ? describedByError : undefined}
              >
                {current === 'passkey' ? m.passkeyButton : m.submit}
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>
    );
  },
);

/** Options for one `requireStepUp()` call: the StepUpAuthDialog props that describe the check. */
export type StepUpAuthOptions = Omit<
  StepUpAuthDialogProps,
  'open' | 'defaultOpen' | 'onOpenChange' | 'trigger'
>;

/**
 * The `stepUp` prop of account and destructive flows: when given, the flow
 * asks the person to re-authenticate before running its action.
 */
export type StepUpRequirement = Pick<
  StepUpAuthOptions,
  'methods' | 'onVerify' | 'description' | 'forgotHref' | 'onForgot' | 'errorMessage' | 'messages'
>;

export interface UseStepUpAuthResult {
  /**
   * Opens the dialog and resolves `true` once `onVerify` succeeds, or `false`
   * when the person cancels.
   */
  requireStepUp: (options: StepUpAuthOptions) => Promise<boolean>;
  /** Render this once, anywhere in the component. */
  element: ReactElement;
}

/**
 * Imperative re-authentication:
 * `if (await requireStepUp({ methods: ['passkey', 'password'], onVerify })) deleteWorkspace()`.
 * Mirrors `useConfirm()`.
 */
export function useStepUpAuth(): UseStepUpAuthResult {
  const [request, setRequest] = useState<{ id: number; options: StepUpAuthOptions } | null>(null);
  const [open, setOpen] = useState(false);
  const resolver = useRef<((value: boolean) => void) | null>(null);
  const counter = useRef(0);
  const returnFocus = useRef<HTMLElement | null>(null);

  const settle = useCallback((value: boolean) => {
    resolver.current?.(value);
    resolver.current = null;
  }, []);

  useEffect(() => () => settle(false), [settle]);

  const requireStepUp = useCallback(
    (options: StepUpAuthOptions) => {
      settle(false);
      returnFocus.current = focusReturnTarget();
      counter.current += 1;
      setRequest({ id: counter.current, options });
      setOpen(true);
      return new Promise<boolean>((resolve) => {
        resolver.current = resolve;
      });
    },
    [settle],
  );

  const options = request?.options;
  const element = (
    <StepUpAuthDialog
      key={request?.id ?? 0}
      onVerify={() => {}}
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
        if (!next) settle(false);
      }}
      onVerified={() => {
        options?.onVerified?.();
        settle(true);
      }}
    />
  );

  return { requireStepUp, element };
}
