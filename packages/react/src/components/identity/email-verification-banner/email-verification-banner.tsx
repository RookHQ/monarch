'use client';

import { forwardRef, useCallback, useEffect, useRef, useState, type ReactNode } from 'react';
import { cx } from '../../../internal/cx.js';
import { Button } from '../../actions/button/index.js';
import { CalloutBanner, type CalloutBannerProps } from '../../feedback/callout-banner/index.js';

export interface UseResendCooldownOptions {
  /** Sends the email or code again. Return a promise to track the pending state. */
  onResend: () => unknown;
  /** Seconds to wait after a successful send before another one. Default 60. */
  cooldownSeconds?: number | undefined;
  /** Seconds already remaining when the component mounts, e.g. right after sign-up. Default 0. */
  initialCooldown?: number | undefined;
  /** Turns a rejection into a message. Default: the Error's message, or `fallbackError`. */
  errorMessage?: ((error: unknown) => ReactNode) | undefined;
  /** Message when a rejection has none. Default "Couldn’t send. Try again." */
  fallbackError?: ReactNode;
}

export interface UseResendCooldownResult {
  /** Calls `onResend` unless pending or cooling down. */
  resend: () => void;
  /** The send is in flight. */
  pending: boolean;
  /** Whole seconds until another send is allowed; 0 when allowed. */
  remaining: number;
  /** The last send succeeded (cleared on the next attempt). */
  sent: boolean;
  /** The last failure's message, or `null`. */
  error: ReactNode;
}

/**
 * Resend with a cooldown: tracks the pending send, the seconds left before
 * another one is allowed, success and failure. Timers start only in the
 * browser and are cleared on unmount.
 */
export function useResendCooldown({
  onResend,
  cooldownSeconds = 60,
  initialCooldown = 0,
  errorMessage,
  fallbackError = 'Couldn’t send. Try again.',
}: UseResendCooldownOptions): UseResendCooldownResult {
  const [pending, setPending] = useState(false);
  const [remaining, setRemaining] = useState(initialCooldown);
  const [sent, setSent] = useState(false);
  const [error, setError] = useState<ReactNode>(null);
  const mounted = useRef(true);
  useEffect(() => {
    mounted.current = true;
    return () => {
      mounted.current = false;
    };
  }, []);

  useEffect(() => {
    if (remaining <= 0) return;
    const id = setTimeout(() => setRemaining((n) => Math.max(0, n - 1)), 1000);
    return () => clearTimeout(id);
  }, [remaining]);

  const toMessage = useCallback(
    (failure: unknown): ReactNode =>
      errorMessage
        ? errorMessage(failure)
        : failure instanceof Error && failure.message
          ? failure.message
          : fallbackError,
    [errorMessage, fallbackError],
  );

  const resend = useCallback(() => {
    if (pending || remaining > 0) return;
    setSent(false);
    setError(null);
    setPending(true);
    Promise.resolve()
      .then(() => onResend())
      .then(
        () => {
          if (!mounted.current) return;
          setPending(false);
          setSent(true);
          setRemaining(cooldownSeconds);
        },
        (failure: unknown) => {
          if (!mounted.current) return;
          setPending(false);
          setError(toMessage(failure));
        },
      );
  }, [pending, remaining, onResend, cooldownSeconds, toMessage]);

  return { resend, pending, remaining, sent, error };
}

/** Visible strings, for translation. */
export interface EmailVerificationBannerMessages {
  /** Bold first line. Default "Verify your email address". */
  title: string;
  /** The explanation, with the address. */
  description: (email: ReactNode) => ReactNode;
  /** Resend button. Default "Resend email". */
  resend: string;
  /** Resend button during the cooldown. Default `Resend in ${seconds}s`. */
  resendIn: (seconds: number) => string;
  /** Announced and shown after a successful send. Default "Verification email sent". */
  sent: string;
  /** Change-email button. Default "Change email". */
  changeEmail: string;
  /** Close button. Default "Dismiss". */
  dismiss: string;
  /** Shown when a send fails without a message. */
  error: string;
}

export const defaultEmailVerificationBannerMessages: EmailVerificationBannerMessages = {
  title: 'Verify your email address',
  description: (email) => (
    <>We sent a verification link to {email}. Open it to confirm the address belongs to you.</>
  ),
  resend: 'Resend email',
  resendIn: (s) => `Resend in ${s}s`,
  sent: 'Verification email sent',
  changeEmail: 'Change email',
  dismiss: 'Dismiss',
  error: 'Couldn’t send the email. Try again.',
};

export interface EmailVerificationBannerProps extends Omit<
  CalloutBannerProps,
  'title' | 'children' | 'actions' | 'tone' | 'live' | 'dismissLabel' | 'icon' | 'variant'
> {
  /** The address waiting for verification. */
  email: string;
  /** Sends the verification email again. Return a promise to show the pending state. */
  onResend: () => unknown;
  /** Seconds before another resend is allowed. Default 60. */
  cooldownSeconds?: number | undefined;
  /** Seconds already remaining on mount, e.g. right after sign-up sent the first email. */
  initialCooldown?: number | undefined;
  /** Shows a "Change email" button. */
  onChangeEmail?: (() => void) | undefined;
  /** Banner colour. Default `warning`. */
  tone?: 'info' | 'warning' | undefined;
  /** Visible strings, for translation. */
  messages?: Partial<EmailVerificationBannerMessages> | undefined;
}

/**
 * A notice that the person’s email address is not verified yet, with the
 * address, a Resend button that waits out a cooldown between sends, and an
 * optional Change email action.
 */
export const EmailVerificationBanner = forwardRef<HTMLElement, EmailVerificationBannerProps>(
  function EmailVerificationBanner(
    {
      email,
      onResend,
      cooldownSeconds = 60,
      initialCooldown = 0,
      onChangeEmail,
      tone = 'warning',
      messages,
      className,
      ...rest
    },
    ref,
  ) {
    const m = { ...defaultEmailVerificationBannerMessages, ...messages };
    const { resend, pending, remaining, sent, error } = useResendCooldown({
      onResend,
      cooldownSeconds,
      initialCooldown,
      fallbackError: m.error,
    });
    const cooling = remaining > 0;

    return (
      <CalloutBanner
        ref={ref}
        tone={tone}
        title={m.title}
        dismissLabel={m.dismiss}
        {...rest}
        className={cx('mn-email-verification-banner', className)}
        data-cooldown={cooling || undefined}
        actions={
          <>
            <Button
              size="sm"
              className="mn-email-verification-banner__resend"
              loading={pending}
              asChild
            >
              {/* aria-disabled, not disabled, during the cooldown so focus stays on the button. */}
              <button
                type="button"
                {...(cooling ? { 'aria-disabled': true } : {})}
                onClick={() => {
                  if (!cooling) resend();
                }}
              >
                {cooling ? m.resendIn(remaining) : m.resend}
              </button>
            </Button>
            {onChangeEmail ? (
              <Button
                size="sm"
                variant="ghost"
                className="mn-email-verification-banner__change"
                onClick={onChangeEmail}
              >
                {m.changeEmail}
              </Button>
            ) : null}
          </>
        }
      >
        <p className="mn-email-verification-banner__text">
          {m.description(<strong className="mn-email-verification-banner__email">{email}</strong>)}
        </p>
        <p className="mn-email-verification-banner__status" role="status">
          {sent ? m.sent : ''}
        </p>
        {error ? (
          <p className="mn-email-verification-banner__error" role="alert">
            {error}
          </p>
        ) : null}
      </CalloutBanner>
    );
  },
);
