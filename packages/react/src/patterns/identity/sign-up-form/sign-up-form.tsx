'use client';

import {
  forwardRef,
  useEffect,
  useId,
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
import { Field, FieldError, FieldLabel } from '../../../components/forms/form-field/index.js';
import { PasswordInput } from '../../../components/forms/password-input/index.js';
import {
  DEFAULT_PASSWORD_RULES,
  PasswordStrengthMeter,
  type PasswordRule,
} from '../../../components/forms/password-strength-meter/index.js';
import {
  TermsAcceptance,
  type TermsAcceptanceDetails,
  type TermsDocument,
} from '../../../components/forms/terms-acceptance/index.js';
import { Input } from '../../../components/forms/text-field/index.js';
import { plainAuthLink, type AuthFormLinkProps } from '../sign-in-form/index.js';

/** The fields of SignUpForm, for errors. */
export type SignUpField = 'name' | 'email' | 'password' | 'confirmPassword' | 'terms';

/** What `SignUpForm` submits. */
export interface SignUpValues {
  /** The trimmed name; empty when the name field is not shown. */
  name: string;
  /** The trimmed email address. */
  email: string;
  password: string;
  /** Proof of acceptance (versions and time), when `terms` are shown. */
  terms?: TermsAcceptanceDetails;
}

/** What `onSubmit` may resolve with to show errors without throwing. */
export interface SignUpResult {
  error?: ReactNode;
  fieldErrors?: Partial<Record<SignUpField, ReactNode>> | undefined;
}

/** Visible strings, for translation. */
export interface SignUpFormMessages {
  /** Default "Full name". */
  nameLabel: string;
  /** Default "Email". */
  emailLabel: string;
  /** Default "Password". */
  passwordLabel: string;
  /** Default "Confirm password". */
  confirmPasswordLabel: string;
  /** Submit button. Default "Create account". */
  submit: string;
  /** Divider under the SSO buttons. Default "or". */
  divider: string;
  /** Before the sign-in link. Default "Already have an account?". */
  signInPrompt: string;
  /** Default "Sign in". */
  signIn: string;
  /** Default "Enter your name.". */
  nameRequired: string;
  /** Default "Enter your email address.". */
  emailRequired: string;
  /** Default "Enter an email address like name@example.com.". */
  emailInvalid: string;
  /** Default "Choose a password.". */
  passwordRequired: string;
  /** Default "Choose a password that meets every requirement.". */
  passwordWeak: string;
  /** Default "The passwords don’t match.". */
  passwordMismatch: string;
  /** Default "Accept the terms to continue". */
  termsRequired: string;
  /** Shown when `onSubmit` rejects without a message. */
  error: string;
}

export const defaultSignUpFormMessages: SignUpFormMessages = {
  nameLabel: 'Full name',
  emailLabel: 'Email',
  passwordLabel: 'Password',
  confirmPasswordLabel: 'Confirm password',
  submit: 'Create account',
  divider: 'or',
  signInPrompt: 'Already have an account?',
  signIn: 'Sign in',
  nameRequired: 'Enter your name.',
  emailRequired: 'Enter your email address.',
  emailInvalid: 'Enter an email address like name@example.com.',
  passwordRequired: 'Choose a password.',
  passwordWeak: 'Choose a password that meets every requirement.',
  passwordMismatch: 'The passwords don’t match.',
  termsRequired: 'Accept the terms to continue',
  error: 'Couldn’t create your account. Try again.',
};

export interface SignUpFormProps extends Omit<
  FormHTMLAttributes<HTMLFormElement>,
  'onSubmit' | 'children'
> {
  /**
   * Creates the account. Return a promise for the pending state. Resolve with
   * `{ error, fieldErrors }` (for example `{ fieldErrors: { email: 'Already
   * registered' } }`) or reject to show errors; anything else is success.
   */
  onSubmit: (values: SignUpValues) => unknown;
  /** Ask for a name. Default `true`. */
  askName?: boolean | undefined;
  /** Ask for the password twice. Default `false` (the reveal button makes typos visible). */
  confirmPassword?: boolean | undefined;
  /** Password requirements, all of which must be met. Default `DEFAULT_PASSWORD_RULES`; `[]` for none. */
  passwordRules?: readonly PasswordRule[] | undefined;
  /** Documents to accept (terms, privacy). Shows a required TermsAcceptance checkbox. */
  terms?: readonly TermsDocument[] | undefined;
  /** Where the terms open. `_blank` adds an “opens in a new tab” note. */
  termsTarget?: string | undefined;
  /** Single sign-on buttons, shown above the form. */
  providers?: ReactNode;
  /** Link to the sign-in page. */
  signInHref?: string | undefined;
  /** Renders links with the product's router. */
  renderLink?: ((props: AuthFormLinkProps) => ReactElement) | undefined;
  /** Prefills the email, e.g. from an invitation. */
  defaultEmail?: string | undefined;
  /** Makes the email read-only, e.g. when it comes from an invitation. */
  emailReadOnly?: boolean | undefined;
  /** Visible strings, for translation. */
  messages?: Partial<SignUpFormMessages> | undefined;
}

const EMAIL = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const ORDER: SignUpField[] = ['name', 'email', 'password', 'confirmPassword', 'terms'];

/**
 * The sign-up form: name, email and a new password with a live strength
 * meter and requirements, optional confirmation, required terms acceptance
 * and single sign-on buttons. Validates on submit, focuses the first problem
 * and maps server errors to fields. It has no page layout of its own.
 */
export const SignUpForm = forwardRef<HTMLFormElement, SignUpFormProps>(function SignUpForm(
  {
    onSubmit,
    askName = true,
    confirmPassword: askConfirm = false,
    passwordRules = DEFAULT_PASSWORD_RULES,
    terms,
    termsTarget,
    providers,
    signInHref,
    renderLink = plainAuthLink,
    defaultEmail = '',
    emailReadOnly = false,
    messages,
    className,
    ...rest
  },
  ref,
) {
  const m = { ...defaultSignUpFormMessages, ...messages };
  const [name, setName] = useState('');
  const [email, setEmail] = useState(defaultEmail);
  const [password, setPassword] = useState('');
  const [confirm, setConfirm] = useState('');
  const [accepted, setAccepted] = useState<TermsAcceptanceDetails | null>(null);
  const [errors, setErrors] = useState<Partial<Record<SignUpField, ReactNode>>>({});
  const [formError, setFormError] = useState<ReactNode>(null);
  const [pending, setPending] = useState(false);
  const meterId = `${useId()}-meter`;
  const refs = {
    name: useRef<HTMLInputElement>(null),
    email: useRef<HTMLInputElement>(null),
    password: useRef<HTMLInputElement>(null),
    confirmPassword: useRef<HTMLInputElement>(null),
    terms: useRef<HTMLDivElement>(null),
  };
  const mounted = useRef(true);
  useEffect(() => {
    mounted.current = true;
    return () => {
      mounted.current = false;
    };
  }, []);

  const focusFirst = (found: Partial<Record<SignUpField, ReactNode>>) => {
    const first = ORDER.find((key) => found[key]);
    if (!first) return;
    if (first === 'terms')
      refs.terms.current?.querySelector<HTMLElement>('[role="checkbox"]')?.focus();
    else refs[first].current?.focus();
  };

  const clear = (field: SignUpField) => {
    if (errors[field]) setErrors((all) => ({ ...all, [field]: undefined }));
  };

  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (pending) return;
    const found: Partial<Record<SignUpField, ReactNode>> = {};
    if (askName && !name.trim()) found.name = m.nameRequired;
    if (!email.trim()) found.email = m.emailRequired;
    else if (!EMAIL.test(email.trim())) found.email = m.emailInvalid;
    if (!password) found.password = m.passwordRequired;
    else if (!passwordRules.every((rule) => rule.test(password))) found.password = m.passwordWeak;
    if (askConfirm && password && confirm !== password) found.confirmPassword = m.passwordMismatch;
    if (terms && terms.length > 0 && !accepted) found.terms = m.termsRequired;
    setErrors(found);
    setFormError(null);
    if (Object.keys(found).length > 0) {
      focusFirst(found);
      return;
    }
    // The fields are disabled while pending, which drops focus from the one Enter was pressed in.
    const restoreFocus = rememberFocus();
    setPending(true);
    try {
      const result = (await onSubmit({
        name: askName ? name.trim() : '',
        email: email.trim(),
        password,
        ...(accepted ? { terms: accepted } : {}),
      })) as SignUpResult | undefined;
      if (!mounted.current) return;
      setPending(false);
      // Wait a tick: the fields are re-enabled by the render that ends the pending state.
      let refocus = restoreFocus;
      if (result && typeof result === 'object' && ('error' in result || 'fieldErrors' in result)) {
        setFormError(result.error ?? null);
        setErrors(result.fieldErrors ?? {});
        const fieldErrors = result.fieldErrors;
        if (fieldErrors && ORDER.some((key) => fieldErrors[key])) {
          refocus = () => focusFirst(fieldErrors);
        }
      }
      setTimeout(refocus, 0);
    } catch (failure) {
      if (!mounted.current) return;
      setPending(false);
      setFormError(failure instanceof Error && failure.message ? failure.message : m.error);
      setTimeout(restoreFocus, 0);
    }
  };

  return (
    <form
      ref={ref}
      noValidate
      {...rest}
      className={cx('mn-sign-up-form', className)}
      aria-busy={pending || undefined}
      onSubmit={(event) => void submit(event)}
    >
      {formError ? (
        <Alert tone="danger" live="assertive">
          {formError}
        </Alert>
      ) : null}
      {providers ? (
        <>
          <div className="mn-sign-up-form__providers">{providers}</div>
          <div className="mn-sign-up-form__divider" role="separator">
            <span>{m.divider}</span>
          </div>
        </>
      ) : null}
      {askName ? (
        <Field invalid={!!errors.name} required>
          <FieldLabel>{m.nameLabel}</FieldLabel>
          <Input
            ref={refs.name}
            name="name"
            autoComplete="name"
            value={name}
            disabled={pending}
            onChange={(event) => {
              setName(event.target.value);
              clear('name');
            }}
          />
          {errors.name ? <FieldError>{errors.name}</FieldError> : null}
        </Field>
      ) : null}
      <Field invalid={!!errors.email} required>
        <FieldLabel>{m.emailLabel}</FieldLabel>
        <Input
          ref={refs.email}
          name="email"
          type="email"
          inputMode="email"
          autoComplete="email"
          autoCapitalize="off"
          spellCheck={false}
          value={email}
          readOnly={emailReadOnly}
          disabled={pending}
          onChange={(event) => {
            setEmail(event.target.value);
            clear('email');
          }}
        />
        {errors.email ? <FieldError>{errors.email}</FieldError> : null}
      </Field>
      <Field invalid={!!errors.password} required>
        <FieldLabel>{m.passwordLabel}</FieldLabel>
        <PasswordInput
          ref={refs.password}
          name="new-password"
          purpose="new"
          value={password}
          disabled={pending}
          aria-describedby={passwordRules.length > 0 ? meterId : undefined}
          onChange={(event) => {
            setPassword(event.target.value);
            clear('password');
          }}
        />
        {errors.password ? <FieldError>{errors.password}</FieldError> : null}
        {passwordRules.length > 0 ? (
          <PasswordStrengthMeter id={meterId} password={password} rules={passwordRules} />
        ) : null}
      </Field>
      {askConfirm ? (
        <Field invalid={!!errors.confirmPassword} required>
          <FieldLabel>{m.confirmPasswordLabel}</FieldLabel>
          <PasswordInput
            ref={refs.confirmPassword}
            name="confirm-password"
            purpose="new"
            value={confirm}
            disabled={pending}
            onChange={(event) => {
              setConfirm(event.target.value);
              clear('confirmPassword');
            }}
          />
          {errors.confirmPassword ? <FieldError>{errors.confirmPassword}</FieldError> : null}
        </Field>
      ) : null}
      {terms && terms.length > 0 ? (
        <TermsAcceptance
          ref={refs.terms}
          documents={terms}
          checked={accepted !== null}
          invalid={!!errors.terms}
          requiredMessage={errors.terms ?? m.termsRequired}
          disabled={pending}
          {...(termsTarget ? { target: termsTarget } : {})}
          onAcceptedChange={(next, details) => {
            setAccepted(next ? details : null);
            clear('terms');
          }}
        />
      ) : null}
      <Button type="submit" variant="primary" block loading={pending}>
        {m.submit}
      </Button>
      {signInHref ? (
        <p className="mn-sign-up-form__footer">
          {m.signInPrompt}{' '}
          {renderLink({ href: signInHref, className: 'mn-sign-up-form__link', children: m.signIn })}
        </p>
      ) : null}
    </form>
  );
});
