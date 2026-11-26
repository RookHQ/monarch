'use client';

import {
  forwardRef,
  useEffect,
  useRef,
  useState,
  type FormEvent,
  type FormHTMLAttributes,
  type ReactElement,
  type ReactNode,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { rememberFocus } from '../../../internal/focus-restore.js';
import { Button } from '../../../components/actions/button/index.js';
import { Alert } from '../../../components/feedback/alert/index.js';
import { Checkbox } from '../../../components/forms/checkbox/index.js';
import { Field, FieldError, FieldLabel } from '../../../components/forms/form-field/index.js';
import { PasswordInput } from '../../../components/forms/password-input/index.js';
import { Input } from '../../../components/forms/text-field/index.js';

/** Props a `renderLink` function receives. Shared by the sign-in, sign-up and reset forms. */
export interface AuthFormLinkProps {
  href: string;
  className: string;
  children: ReactNode;
}

/** A plain `<a>`; pass `renderLink` to use the product's router link instead. */
export const plainAuthLink = ({ href, className, children }: AuthFormLinkProps) => (
  <a href={href} className={className}>
    {children}
  </a>
);

/** What `SignInForm` submits. */
export interface SignInValues {
  /** The email address or username, trimmed. */
  identifier: string;
  /** The password, as typed. */
  password: string;
  /** The “Keep me signed in” box; `false` when it is not shown. */
  remember: boolean;
}

/** What `onSubmit` may resolve with to show errors without throwing. */
export interface SignInResult {
  /** A form-level error, e.g. "Email or password is incorrect". */
  error?: ReactNode;
  /** Errors for single fields. */
  fieldErrors?: Partial<Record<'identifier' | 'password', ReactNode>> | undefined;
}

/** Visible strings, for translation. */
export interface SignInFormMessages {
  /** Label for `identifier="email"`. Default "Email". */
  emailLabel: string;
  /** Label for `identifier="username"`. Default "Username". */
  usernameLabel: string;
  /** Label for `identifier="email-or-username"`. Default "Email or username". */
  emailOrUsernameLabel: string;
  /** Default "Password". */
  passwordLabel: string;
  /** Default "Forgot password?". */
  forgotPassword: string;
  /** Default "Keep me signed in". */
  remember: string;
  /** Submit button. Default "Sign in". */
  submit: string;
  /** Passkey button. Default "Sign in with a passkey". */
  passkey: string;
  /** Divider between other methods and the form. Default "or". */
  divider: string;
  /** Before the sign-up link. Default "New here?". */
  signUpPrompt: string;
  /** Default "Create an account". */
  signUp: string;
  /** Empty identifier, with the field's label. Default `Enter your ${label}.` in lower case. */
  identifierRequired: (label: string) => string;
  /** Identifier that is not an email address. */
  emailInvalid: string;
  /** Empty password. */
  passwordRequired: string;
  /** Shown when `onSubmit` or `onPasskey` rejects without a message. */
  error: string;
  /** Shown when the passkey prompt is cancelled. */
  passkeyCancelled: string;
}

export const defaultSignInFormMessages: SignInFormMessages = {
  emailLabel: 'Email',
  usernameLabel: 'Username',
  emailOrUsernameLabel: 'Email or username',
  passwordLabel: 'Password',
  forgotPassword: 'Forgot password?',
  remember: 'Keep me signed in',
  submit: 'Sign in',
  passkey: 'Sign in with a passkey',
  divider: 'or',
  signUpPrompt: 'New here?',
  signUp: 'Create an account',
  identifierRequired: (label) => `Enter your ${label.toLowerCase()}.`,
  emailInvalid: 'Enter an email address like name@example.com.',
  passwordRequired: 'Enter your password.',
  error: 'Couldn’t sign you in. Try again.',
  passkeyCancelled: 'Passkey sign-in was cancelled.',
};

export interface SignInFormProps extends Omit<
  FormHTMLAttributes<HTMLFormElement>,
  'onSubmit' | 'children'
> {
  /**
   * Signs in. Return a promise for the pending state. Resolve with
   * `{ error, fieldErrors }` or reject to show errors; anything else counts
   * as success (the product navigates).
   */
  onSubmit: (values: SignInValues) => unknown;
  /** What people sign in with. Default `email`. */
  identifier?: 'email' | 'username' | 'email-or-username' | undefined;
  /** Show the password field. Default `true`; set `false` for passkey- or SSO-only sign-in. */
  password?: boolean | undefined;
  /**
   * Runs the product's WebAuthn sign-in (`navigator.credentials.get`). Shows
   * the passkey button. A rejection named `NotAllowedError` or `AbortError`
   * shows a neutral note.
   */
  onPasskey?: (() => unknown) | undefined;
  /** Single sign-on buttons (for example SignInWithRookButton), shown above the form. */
  providers?: ReactNode;
  /** Show “Keep me signed in”. Default `false`. */
  rememberMe?: boolean | undefined;
  /** Initial state of “Keep me signed in”. Default `false`. */
  defaultRemember?: boolean | undefined;
  /** Prefills the identifier, e.g. from a previous step. */
  defaultIdentifier?: string | undefined;
  /** Link to the password reset page. */
  forgotPasswordHref?: string | undefined;
  /** Link to the sign-up page. */
  signUpHref?: string | undefined;
  /** Renders links with the product's router (for example Next.js `Link`). */
  renderLink?: ((props: AuthFormLinkProps) => ReactElement) | undefined;
  /** A form-level error from outside, e.g. from an OAuth redirect. */
  error?: ReactNode;
  /** Visible strings, for translation. */
  messages?: Partial<SignInFormMessages> | undefined;
}

const EMAIL = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

function isCancel(error: unknown): boolean {
  const name =
    typeof error === 'object' && error !== null && 'name' in error
      ? String((error as { name: unknown }).name)
      : '';
  return name === 'NotAllowedError' || name === 'AbortError';
}

function messageOf(error: unknown, fallback: string): ReactNode {
  return error instanceof Error && error.message ? error.message : fallback;
}

/**
 * The sign-in form: email (or username) and password, an optional passkey
 * button and single sign-on buttons, “Keep me signed in”, and links to reset
 * the password or create an account. Validates on submit, focuses the first
 * problem and shows server errors on the right field. It has no page layout of its own, so it fits
 * inside any auth page.
 */
export const SignInForm = forwardRef<HTMLFormElement, SignInFormProps>(function SignInForm(
  {
    onSubmit,
    identifier: identifierType = 'email',
    password: withPassword = true,
    onPasskey,
    providers,
    rememberMe = false,
    defaultRemember = false,
    defaultIdentifier = '',
    forgotPasswordHref,
    signUpHref,
    renderLink = plainAuthLink,
    error: errorProp,
    messages,
    className,
    ...rest
  },
  ref,
) {
  const m = { ...defaultSignInFormMessages, ...messages };
  const [identifier, setIdentifier] = useState(defaultIdentifier);
  const [password, setPassword] = useState('');
  const [remember, setRemember] = useState(defaultRemember);
  const [fieldErrors, setFieldErrors] = useState<SignInResult['fieldErrors']>({});
  const [formError, setFormError] = useState<ReactNode>(null);
  const [note, setNote] = useState<ReactNode>(null);
  const [pending, setPending] = useState<'form' | 'passkey' | null>(null);
  const identifierRef = useRef<HTMLInputElement>(null);
  const passwordRef = useRef<HTMLInputElement>(null);
  const mounted = useRef(true);
  useEffect(() => {
    mounted.current = true;
    return () => {
      mounted.current = false;
    };
  }, []);

  const label =
    identifierType === 'username'
      ? m.usernameLabel
      : identifierType === 'email-or-username'
        ? m.emailOrUsernameLabel
        : m.emailLabel;

  const focusFirst = (errors: SignInResult['fieldErrors']) => {
    if (errors?.identifier) identifierRef.current?.focus();
    else if (errors?.password) passwordRef.current?.focus();
  };

  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (pending) return;
    const id = identifier.trim();
    const errors: NonNullable<SignInResult['fieldErrors']> = {};
    if (!id) errors.identifier = m.identifierRequired(label);
    else if (identifierType === 'email' && !EMAIL.test(id)) errors.identifier = m.emailInvalid;
    if (withPassword && !password) errors.password = m.passwordRequired;
    setFormError(null);
    setNote(null);
    setFieldErrors(errors);
    if (Object.keys(errors).length > 0) {
      focusFirst(errors);
      return;
    }
    // The fields are disabled while pending, which drops focus from the one Enter was pressed in.
    const restoreFocus = rememberFocus();
    setPending('form');
    try {
      const result = (await onSubmit({
        identifier: id,
        password,
        remember: rememberMe && remember,
      })) as SignInResult | undefined;
      if (!mounted.current) return;
      setPending(null);
      // Wait a tick: the fields are re-enabled by the render that ends the pending state.
      let refocus = restoreFocus;
      if (result && typeof result === 'object' && ('error' in result || 'fieldErrors' in result)) {
        setFormError(result.error ?? null);
        setFieldErrors(result.fieldErrors ?? {});
        const fieldErrors = result.fieldErrors;
        if (fieldErrors?.identifier || fieldErrors?.password) {
          refocus = () => focusFirst(fieldErrors);
        }
        if (result.fieldErrors?.password || result.error) setPassword('');
      }
      setTimeout(refocus, 0);
    } catch (failure) {
      if (!mounted.current) return;
      setPending(null);
      setFormError(messageOf(failure, m.error));
      setTimeout(restoreFocus, 0);
    }
  };

  const passkey = async () => {
    if (!onPasskey || pending) return;
    setFormError(null);
    setNote(null);
    setPending('passkey');
    try {
      await onPasskey();
      if (mounted.current) setPending(null);
    } catch (failure) {
      if (!mounted.current) return;
      setPending(null);
      if (isCancel(failure)) setNote(m.passkeyCancelled);
      else setFormError(messageOf(failure, m.error));
    }
  };

  const shownError = formError ?? errorProp;
  const alternatives = onPasskey || providers;

  return (
    <form
      ref={ref}
      noValidate
      {...rest}
      className={cx('mn-sign-in-form', className)}
      aria-busy={pending ? true : undefined}
      onSubmit={(event) => void submit(event)}
    >
      {shownError ? (
        <Alert tone="danger" live="assertive" className="mn-sign-in-form__error">
          {shownError}
        </Alert>
      ) : null}
      {alternatives ? (
        <div className="mn-sign-in-form__alternatives">
          {onPasskey ? (
            <Button
              block
              loading={pending === 'passkey'}
              disabled={pending === 'form'}
              leadingIcon={<KeyGlyph />}
              onClick={() => void passkey()}
            >
              {m.passkey}
            </Button>
          ) : null}
          {providers}
          {note ? (
            <p className="mn-sign-in-form__note" role="status">
              {note}
            </p>
          ) : null}
        </div>
      ) : null}
      {alternatives ? (
        <div className="mn-sign-in-form__divider" role="separator">
          <span>{m.divider}</span>
        </div>
      ) : null}
      <Field invalid={!!fieldErrors?.identifier} required>
        <FieldLabel>{label}</FieldLabel>
        <Input
          ref={identifierRef}
          // Stable names let password managers pair the fields and native form posts carry them.
          name={identifierType === 'email' ? 'email' : 'username'}
          type={identifierType === 'email' ? 'email' : 'text'}
          inputMode={identifierType === 'email' ? 'email' : undefined}
          autoComplete={onPasskey ? 'username webauthn' : 'username'}
          autoCapitalize="off"
          spellCheck={false}
          value={identifier}
          disabled={pending !== null}
          onChange={(event) => {
            setIdentifier(event.target.value);
            if (fieldErrors?.identifier) setFieldErrors((e) => ({ ...e, identifier: undefined }));
          }}
        />
        {fieldErrors?.identifier ? <FieldError>{fieldErrors.identifier}</FieldError> : null}
      </Field>
      {withPassword ? (
        <Field invalid={!!fieldErrors?.password} required>
          <div className="mn-sign-in-form__label-row">
            <FieldLabel>{m.passwordLabel}</FieldLabel>
            {forgotPasswordHref
              ? renderLink({
                  href: forgotPasswordHref,
                  className: 'mn-sign-in-form__link',
                  children: m.forgotPassword,
                })
              : null}
          </div>
          <PasswordInput
            ref={passwordRef}
            name="password"
            purpose="current"
            value={password}
            disabled={pending !== null}
            onChange={(event) => {
              setPassword(event.target.value);
              if (fieldErrors?.password) setFieldErrors((e) => ({ ...e, password: undefined }));
            }}
          />
          {fieldErrors?.password ? <FieldError>{fieldErrors.password}</FieldError> : null}
        </Field>
      ) : null}
      {rememberMe ? (
        <Checkbox
          label={m.remember}
          checked={remember}
          disabled={pending !== null}
          onCheckedChange={(next) => setRemember(next === true)}
        />
      ) : null}
      <Button
        type="submit"
        variant="primary"
        block
        loading={pending === 'form'}
        disabled={pending === 'passkey'}
      >
        {m.submit}
      </Button>
      {signUpHref ? (
        <p className="mn-sign-in-form__footer">
          {m.signUpPrompt}{' '}
          {renderLink({ href: signUpHref, className: 'mn-sign-in-form__link', children: m.signUp })}
        </p>
      ) : null}
    </form>
  );
});

function KeyGlyph() {
  return (
    <svg
      width="16"
      height="16"
      viewBox="0 0 16 16"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.5"
      strokeLinecap="round"
      strokeLinejoin="round"
    >
      <circle cx="5.5" cy="10.5" r="3" />
      <path d="m7.6 8.4 6-6M11.5 4.5l1.5 1.5M10 6l1.25 1.25" />
    </svg>
  );
}
