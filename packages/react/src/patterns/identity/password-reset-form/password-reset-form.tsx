'use client';

import {
  forwardRef,
  useEffect,
  useId,
  useRef,
  useState,
  type FormEvent,
  type HTMLAttributes,
  type ReactElement,
  type ReactNode,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { rememberFocus } from '../../../internal/focus-restore.js';
import { Button } from '../../../components/actions/button/index.js';
import { Alert } from '../../../components/feedback/alert/index.js';
import { Field, FieldError, FieldLabel } from '../../../components/forms/form-field/index.js';
import { PasswordInput } from '../../../components/forms/password-input/index.js';
import {
  DEFAULT_PASSWORD_RULES,
  PasswordStrengthMeter,
  type PasswordRule,
} from '../../../components/forms/password-strength-meter/index.js';
import { Input } from '../../../components/forms/text-field/index.js';
import { useResendCooldown } from '../../../components/identity/email-verification-banner/index.js';
import { plainAuthLink, type AuthFormLinkProps } from '../sign-in-form/index.js';

/**
 * The steps: `request` (enter email), `sent` (check your email), `reset`
 * (choose a new password, from the emailed link) and `done`.
 */
export type PasswordResetStep = 'request' | 'sent' | 'reset' | 'done';

/** Visible strings, for translation. */
export interface PasswordResetFormMessages {
  /** Default "Reset your password". */
  requestTitle: string;
  /** Under the request title. */
  requestDescription: string;
  /** Default "Email". */
  emailLabel: string;
  /** Default "Send reset link". */
  requestSubmit: string;
  /** Default "Check your email". */
  sentTitle: string;
  /** Under the sent title, with the address. */
  sentDescription: (email: ReactNode) => ReactNode;
  /** Default "Resend link". */
  resend: string;
  /** Default `Resend in ${s}s`. */
  resendIn: (seconds: number) => string;
  /** Announced after a resend. Default "We sent another link." */
  resent: string;
  /** Default "Use a different email". */
  differentEmail: string;
  /** Default "Choose a new password". */
  resetTitle: string;
  /** Under the reset title. */
  resetDescription: string;
  /** Default "New password". */
  passwordLabel: string;
  /** Default "Confirm new password". */
  confirmLabel: string;
  /** Default "Change password". */
  resetSubmit: string;
  /** Default "Password changed". */
  doneTitle: string;
  /** Under the done title. */
  doneDescription: string;
  /** Default "Back to sign in". */
  signIn: string;
  /** Empty email. */
  emailRequired: string;
  /** Not an email address. */
  emailInvalid: string;
  /** Empty password. */
  passwordRequired: string;
  /** Password missing a requirement. */
  passwordWeak: string;
  /** Confirmation does not match. */
  passwordMismatch: string;
  /** Shown when a callback rejects without a message. */
  error: string;
}

export const defaultPasswordResetFormMessages: PasswordResetFormMessages = {
  requestTitle: 'Reset your password',
  requestDescription:
    'Enter the email address of your account and we’ll send you a link to choose a new password.',
  emailLabel: 'Email',
  requestSubmit: 'Send reset link',
  sentTitle: 'Check your email',
  sentDescription: (email) => (
    <>
      If an account exists for {email}, you’ll get an email with a link to reset your password. The
      link works for a limited time.
    </>
  ),
  resend: 'Resend link',
  resendIn: (s) => `Resend in ${s}s`,
  resent: 'We sent another link.',
  differentEmail: 'Use a different email',
  resetTitle: 'Choose a new password',
  resetDescription: 'You’ll use it the next time you sign in.',
  passwordLabel: 'New password',
  confirmLabel: 'Confirm new password',
  resetSubmit: 'Change password',
  doneTitle: 'Password changed',
  doneDescription: 'You can now sign in with your new password.',
  signIn: 'Back to sign in',
  emailRequired: 'Enter your email address.',
  emailInvalid: 'Enter an email address like name@example.com.',
  passwordRequired: 'Choose a new password.',
  passwordWeak: 'Choose a password that meets every requirement.',
  passwordMismatch: 'The passwords don’t match.',
  error: 'Something went wrong. Try again.',
};

export interface PasswordResetFormProps extends Omit<
  HTMLAttributes<HTMLDivElement>,
  'title' | 'children' | 'onReset'
> {
  /** The current step (controlled). Use `reset` on the page the emailed link opens. */
  step?: PasswordResetStep | undefined;
  /** Initial step when uncontrolled. Default `request`. */
  defaultStep?: PasswordResetStep | undefined;
  /** Called when the step changes. */
  onStepChange?: ((step: PasswordResetStep) => void) | undefined;
  /** Sends the reset email. Return a promise; a rejection shows its message. */
  onRequest?: ((email: string) => unknown) | undefined;
  /** Saves the new password (the product holds the reset token). Return a promise; a rejection shows its message. */
  onReset?: ((password: string) => unknown) | undefined;
  /** Password requirements, all of which must be met. Default `DEFAULT_PASSWORD_RULES`. */
  passwordRules?: readonly PasswordRule[] | undefined;
  /** Ask for the new password twice. Default `true`. */
  confirmPassword?: boolean | undefined;
  /** Seconds between resends on the `sent` step. Default 60. */
  cooldownSeconds?: number | undefined;
  /** Prefills the email. */
  defaultEmail?: string | undefined;
  /** Link back to sign in. */
  signInHref?: string | undefined;
  /** Renders links with the product's router. */
  renderLink?: ((props: AuthFormLinkProps) => ReactElement) | undefined;
  /** Heading element of each step's title. Default `h2`. */
  headingLevel?: 'h1' | 'h2' | 'h3' | undefined;
  /** An error from outside, e.g. "This link has expired" on the `reset` step. */
  error?: ReactNode;
  /** Visible strings, for translation. */
  messages?: Partial<PasswordResetFormMessages> | undefined;
}

const EMAIL = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

function messageOf(error: unknown, fallback: string): ReactNode {
  return error instanceof Error && error.message ? error.message : fallback;
}

/**
 * Password reset in two halves: request a link by email (then “check your
 * email” with a resend cooldown), and, on the page the link opens, choose a
 * new password with a live strength meter, then a confirmation with a link
 * back to sign in. Each step's heading takes focus when the step changes.
 */
export const PasswordResetForm = forwardRef<HTMLDivElement, PasswordResetFormProps>(
  function PasswordResetForm(
    {
      step: stepProp,
      defaultStep = 'request',
      onStepChange,
      onRequest,
      onReset,
      passwordRules = DEFAULT_PASSWORD_RULES,
      confirmPassword: askConfirm = true,
      cooldownSeconds = 60,
      defaultEmail = '',
      signInHref,
      renderLink = plainAuthLink,
      headingLevel = 'h2',
      error: errorProp,
      messages,
      className,
      ...rest
    },
    ref,
  ) {
    const m = { ...defaultPasswordResetFormMessages, ...messages };
    const [stepState, setStepState] = useState(defaultStep);
    const step = stepProp ?? stepState;
    const [email, setEmail] = useState(defaultEmail);
    const [password, setPassword] = useState('');
    const [confirm, setConfirm] = useState('');
    const [errors, setErrors] = useState<
      Partial<Record<'email' | 'password' | 'confirm', ReactNode>>
    >({});
    const [formError, setFormError] = useState<ReactNode>(null);
    const [pending, setPending] = useState(false);
    const headingRef = useRef<HTMLHeadingElement>(null);
    const emailRef = useRef<HTMLInputElement>(null);
    const passwordRef = useRef<HTMLInputElement>(null);
    const confirmRef = useRef<HTMLInputElement>(null);
    const mounted = useRef(true);
    const id = useId();
    const meterId = `${id}-meter`;
    const titleId = `${id}-title`;
    useEffect(() => {
      mounted.current = true;
      return () => {
        mounted.current = false;
      };
    }, []);

    // Move focus to the new step's heading, but not on first render.
    const firstStep = useRef(step);
    useEffect(() => {
      if (firstStep.current === step) return;
      firstStep.current = '' as PasswordResetStep;
      // A new step starts without the previous step's errors, also when controlled.
      setErrors({});
      setFormError(null);
      headingRef.current?.focus();
    }, [step]);

    const goTo = (next: PasswordResetStep) => {
      setErrors({});
      setFormError(null);
      if (stepProp === undefined) setStepState(next);
      onStepChange?.(next);
    };

    const resendState = useResendCooldown({
      onResend: () => onRequest?.(email.trim()),
      cooldownSeconds,
      initialCooldown: 0,
      fallbackError: m.error,
    });

    const run = async (work: () => unknown, next: PasswordResetStep) => {
      // The fields are disabled while pending, which drops focus from the one Enter was pressed in.
      const restoreFocus = rememberFocus();
      setPending(true);
      setFormError(null);
      try {
        await work();
        if (!mounted.current) return;
        setPending(false);
        goTo(next);
      } catch (failure) {
        if (!mounted.current) return;
        setPending(false);
        setFormError(messageOf(failure, m.error));
        // Wait a tick: the fields are re-enabled by the render that ends the pending state.
        setTimeout(restoreFocus, 0);
      }
    };

    const submitRequest = (event: FormEvent<HTMLFormElement>) => {
      event.preventDefault();
      if (pending) return;
      const value = email.trim();
      const found = !value ? m.emailRequired : !EMAIL.test(value) ? m.emailInvalid : null;
      setErrors(found ? { email: found } : {});
      if (found) {
        emailRef.current?.focus();
        return;
      }
      void run(() => onRequest?.(value), 'sent');
    };

    const submitReset = (event: FormEvent<HTMLFormElement>) => {
      event.preventDefault();
      if (pending) return;
      const found: typeof errors = {};
      if (!password) found.password = m.passwordRequired;
      else if (!passwordRules.every((rule) => rule.test(password))) found.password = m.passwordWeak;
      if (askConfirm && password && confirm !== password) found.confirm = m.passwordMismatch;
      setErrors(found);
      if (found.password) passwordRef.current?.focus();
      else if (found.confirm) confirmRef.current?.focus();
      if (Object.keys(found).length > 0) return;
      void run(() => onReset?.(password), 'done');
    };

    const Heading = headingLevel;
    const titles: Record<PasswordResetStep, [string, ReactNode]> = {
      request: [m.requestTitle, m.requestDescription],
      sent: [
        m.sentTitle,
        m.sentDescription(
          <strong className="mn-password-reset-form__email">{email.trim()}</strong>,
        ),
      ],
      reset: [m.resetTitle, m.resetDescription],
      done: [m.doneTitle, m.doneDescription],
    };
    const [titleText, descriptionText] = titles[step];
    const shownError = formError ?? errorProp;
    const signInLink = signInHref
      ? renderLink({
          href: signInHref,
          className: 'mn-password-reset-form__link',
          children: m.signIn,
        })
      : null;

    return (
      <div ref={ref} {...rest} className={cx('mn-password-reset-form', className)} data-step={step}>
        <div className="mn-password-reset-form__header">
          <Heading
            ref={headingRef}
            id={titleId}
            tabIndex={-1}
            className="mn-password-reset-form__title"
          >
            {titleText}
          </Heading>
          <p className="mn-password-reset-form__description">{descriptionText}</p>
        </div>
        {shownError ? (
          <Alert tone="danger" live="assertive">
            {shownError}
          </Alert>
        ) : null}

        {step === 'request' ? (
          <form
            className="mn-password-reset-form__form"
            noValidate
            aria-labelledby={titleId}
            aria-busy={pending || undefined}
            onSubmit={submitRequest}
          >
            <Field invalid={!!errors.email} required>
              <FieldLabel>{m.emailLabel}</FieldLabel>
              <Input
                ref={emailRef}
                type="email"
                inputMode="email"
                autoComplete="email"
                autoCapitalize="off"
                spellCheck={false}
                value={email}
                disabled={pending}
                onChange={(event) => {
                  setEmail(event.target.value);
                  if (errors.email) setErrors({});
                }}
              />
              {errors.email ? <FieldError>{errors.email}</FieldError> : null}
            </Field>
            <Button type="submit" variant="primary" block loading={pending}>
              {m.requestSubmit}
            </Button>
            {signInLink ? <p className="mn-password-reset-form__footer">{signInLink}</p> : null}
          </form>
        ) : null}

        {step === 'sent' ? (
          <div className="mn-password-reset-form__form">
            {onRequest ? (
              <Button block loading={resendState.pending} asChild>
                {/* aria-disabled, not disabled, during the cooldown so focus stays on the button. */}
                <button
                  type="button"
                  {...(resendState.remaining > 0 ? { 'aria-disabled': true } : {})}
                  onClick={() => {
                    if (resendState.remaining === 0) resendState.resend();
                  }}
                >
                  {resendState.remaining > 0 ? m.resendIn(resendState.remaining) : m.resend}
                </button>
              </Button>
            ) : null}
            <p className="mn-password-reset-form__status" role="status">
              {resendState.sent ? m.resent : ''}
            </p>
            {resendState.error ? (
              <p className="mn-password-reset-form__error" role="alert">
                {resendState.error}
              </p>
            ) : null}
            <Button variant="ghost" block onClick={() => goTo('request')}>
              {m.differentEmail}
            </Button>
            {signInLink ? <p className="mn-password-reset-form__footer">{signInLink}</p> : null}
          </div>
        ) : null}

        {step === 'reset' ? (
          <form
            className="mn-password-reset-form__form"
            noValidate
            aria-labelledby={titleId}
            aria-busy={pending || undefined}
            onSubmit={submitReset}
          >
            {/* Lets password managers save the new password against the right account. */}
            {email ? (
              <input
                type="text"
                name="username"
                autoComplete="username"
                value={email}
                readOnly
                hidden
              />
            ) : null}
            <Field invalid={!!errors.password} required>
              <FieldLabel>{m.passwordLabel}</FieldLabel>
              <PasswordInput
                ref={passwordRef}
                purpose="new"
                value={password}
                disabled={pending}
                aria-describedby={passwordRules.length > 0 ? meterId : undefined}
                onChange={(event) => {
                  setPassword(event.target.value);
                  if (errors.password) setErrors((all) => ({ ...all, password: undefined }));
                }}
              />
              {errors.password ? <FieldError>{errors.password}</FieldError> : null}
              {passwordRules.length > 0 ? (
                <PasswordStrengthMeter id={meterId} password={password} rules={passwordRules} />
              ) : null}
            </Field>
            {askConfirm ? (
              <Field invalid={!!errors.confirm} required>
                <FieldLabel>{m.confirmLabel}</FieldLabel>
                <PasswordInput
                  ref={confirmRef}
                  purpose="new"
                  value={confirm}
                  disabled={pending}
                  onChange={(event) => {
                    setConfirm(event.target.value);
                    if (errors.confirm) setErrors((all) => ({ ...all, confirm: undefined }));
                  }}
                />
                {errors.confirm ? <FieldError>{errors.confirm}</FieldError> : null}
              </Field>
            ) : null}
            <Button type="submit" variant="primary" block loading={pending}>
              {m.resetSubmit}
            </Button>
          </form>
        ) : null}

        {step === 'done' && signInHref ? (
          <div className="mn-password-reset-form__form">
            {renderLink({
              href: signInHref,
              className: 'mn-password-reset-form__primary-link',
              children: m.signIn,
            })}
          </div>
        ) : null}
      </div>
    );
  },
);
