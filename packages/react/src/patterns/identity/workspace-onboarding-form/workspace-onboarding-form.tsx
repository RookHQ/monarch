'use client';

import {
  forwardRef,
  useEffect,
  useId,
  useRef,
  useState,
  type FormEvent,
  type FormHTMLAttributes,
  type ReactNode,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { Button } from '../../../components/actions/button/index.js';
import { Alert } from '../../../components/feedback/alert/index.js';
import {
  Field,
  FieldDescription,
  FieldError,
  FieldLabel,
} from '../../../components/forms/form-field/index.js';
import { Input } from '../../../components/forms/text-field/index.js';
import {
  UsernameAvailabilityInput,
  type AsyncValidationResult,
  type AsyncValidationStatus,
} from '../../../components/identity/username-availability-input/index.js';

/** What the form collects. */
export interface WorkspaceOnboardingValues {
  /** Display name, trimmed. */
  name: string;
  /** Unique handle. */
  username: string;
  /** The chosen type's `value`, when `types` is not empty. */
  type?: string | undefined;
}

/** One workspace type the person can choose. */
export interface WorkspaceTypeOption {
  value: string;
  label: string;
  description?: string | undefined;
  /** Decorative icon shown above the label. */
  icon?: ReactNode;
}

/** Length and character rules for the name and username. */
export interface WorkspaceOnboardingRules {
  /** Default 4. */
  nameMinLength: number;
  /** Default 25. */
  nameMaxLength: number;
  /** Default 5. */
  usernameMinLength: number;
  /** Default 30. */
  usernameMaxLength: number;
  /** Characters a username may contain. Default letters and digits: `/^[a-zA-Z0-9]+$/`. */
  usernamePattern: RegExp;
}

/** Visible strings, for translation. */
export interface WorkspaceOnboardingFormMessages {
  nameLabel: string;
  namePlaceholder: string;
  usernameLabel: string;
  usernamePlaceholder: string;
  /** Help under the username. Default: the allowed characters and length. */
  usernameDescription: (min: number, max: number) => string;
  typeLabel: string;
  nameTooShort: (min: number) => string;
  nameTooLong: (max: number) => string;
  usernameTooShort: (min: number) => string;
  usernameTooLong: (max: number) => string;
  usernameInvalidCharacters: string;
  /** Shown when the availability check says no and gives no message. */
  usernameTaken: string;
  usernameAvailable: string;
  usernameChecking: string;
  /** Shown when submitting while the check is still running. */
  usernameStillChecking: string;
  usernameCheckFailed: string;
  retry: string;
  typeRequired: string;
  /** Submit button. Default "Continue". */
  submit: string;
  /** Shown when `onSubmit` rejects without a message. */
  genericError: string;
}

export const defaultWorkspaceOnboardingFormMessages: WorkspaceOnboardingFormMessages = {
  nameLabel: 'Workspace name',
  namePlaceholder: 'Acme Inc.',
  usernameLabel: 'Workspace username',
  usernamePlaceholder: 'acme',
  usernameDescription: (min, max) => `Letters and numbers only, ${min} to ${max} characters.`,
  typeLabel: 'Type',
  nameTooShort: (min) => `Name must be at least ${min} characters`,
  nameTooLong: (max) => `Name must be at most ${max} characters`,
  usernameTooShort: (min) => `Username must be at least ${min} characters`,
  usernameTooLong: (max) => `Username must be at most ${max} characters`,
  usernameInvalidCharacters: 'Use letters and numbers only',
  usernameTaken: 'Username is taken',
  usernameAvailable: 'Username is available',
  usernameChecking: 'Checking availability…',
  usernameStillChecking: 'Wait for the availability check to finish',
  usernameCheckFailed: 'Couldn’t check availability.',
  retry: 'Try again',
  typeRequired: 'Choose a workspace type',
  submit: 'Continue',
  genericError: 'Something went wrong creating your workspace. Try again.',
};

const DEFAULT_RULES: WorkspaceOnboardingRules = {
  nameMinLength: 4,
  nameMaxLength: 25,
  usernameMinLength: 5,
  usernameMaxLength: 30,
  usernamePattern: /^[a-zA-Z0-9]+$/,
};

const glyph = {
  width: 24,
  height: 24,
  viewBox: '0 0 24 24',
  fill: 'none',
  stroke: 'currentColor',
  strokeWidth: 1.75,
  strokeLinecap: 'round' as const,
  strokeLinejoin: 'round' as const,
};

/** The two types the Rook apps offer by default: a person and an organisation. */
export const defaultWorkspaceTypes: WorkspaceTypeOption[] = [
  {
    value: 'creator',
    label: 'Creator',
    description: 'For individual creators and personal projects',
    icon: (
      <svg {...glyph}>
        <circle cx="12" cy="8" r="4" />
        <path d="M4 21a8 8 0 0 1 16 0" />
      </svg>
    ),
  },
  {
    value: 'business',
    label: 'Business',
    description: 'For teams and companies',
    icon: (
      <svg {...glyph}>
        <rect x="4" y="3" width="16" height="18" rx="1.5" />
        <path d="M9 7h1M14 7h1M9 11h1M14 11h1M9 15h1M14 15h1M10 21v-3h4v3" />
      </svg>
    ),
  },
];

export interface WorkspaceOnboardingFormProps extends Omit<
  FormHTMLAttributes<HTMLFormElement>,
  'onSubmit' | 'defaultValue'
> {
  /**
   * Creates the workspace. Return a promise to show a pending button; a
   * rejection shows its message (see `errorMessage`) above the button and
   * keeps the form. Its resolved value is passed to `onSuccess`.
   */
  onSubmit: (values: WorkspaceOnboardingValues) => unknown;
  /** Called after `onSubmit` succeeds, with its result. */
  onSuccess?: ((result: unknown, values: WorkspaceOnboardingValues) => void) | undefined;
  /**
   * The product's availability lookup for the username, called after the
   * local rules pass. Pass the signal to `fetch`. Without it only the local
   * rules are checked.
   */
  checkUsername?:
    | ((
        username: string,
        signal: AbortSignal,
      ) => AsyncValidationResult | null | Promise<AsyncValidationResult | null>)
    | undefined;
  /** The workspace types to choose from. Default `defaultWorkspaceTypes`; `[]` hides the choice. */
  types?: WorkspaceTypeOption[] | undefined;
  /** Initial values. */
  defaultValues?: Partial<WorkspaceOnboardingValues> | undefined;
  /** Overrides the length and character rules. */
  rules?: Partial<WorkspaceOnboardingRules> | undefined;
  /** Fixed text before the username. Default "@". */
  usernamePrefix?: ReactNode;
  /** Overrides the submit label, e.g. "Create workspace" in a dialog. */
  submitLabel?: ReactNode;
  /** A control beside the submit button, e.g. a Cancel button in a dialog. */
  secondaryAction?: ReactNode;
  /** Turns a rejected `onSubmit` into a message. Default: the Error's message, or `messages.genericError`. */
  errorMessage?: ((error: unknown) => ReactNode) | undefined;
  /** Disables every field and the button. */
  disabled?: boolean | undefined;
  /** Visible strings, for translation. */
  messages?: Partial<WorkspaceOnboardingFormMessages> | undefined;
}

type FieldName = 'name' | 'username' | 'type';

/**
 * The form that creates a workspace: name, a unique username with a live
 * availability check, and a workspace type, with inline validation and a
 * pending and error state while the product creates it.
 */
export const WorkspaceOnboardingForm = forwardRef<HTMLFormElement, WorkspaceOnboardingFormProps>(
  function WorkspaceOnboardingForm(
    {
      onSubmit,
      onSuccess,
      checkUsername,
      types = defaultWorkspaceTypes,
      defaultValues,
      rules: rulesProp,
      usernamePrefix = '@',
      submitLabel,
      secondaryAction,
      errorMessage,
      disabled = false,
      messages,
      className,
      ...rest
    },
    ref,
  ) {
    const m = { ...defaultWorkspaceOnboardingFormMessages, ...messages };
    const rules = { ...DEFAULT_RULES, ...rulesProp };
    const auto = useId();
    const [name, setName] = useState(defaultValues?.name ?? '');
    const [username, setUsername] = useState(defaultValues?.username ?? '');
    const [type, setType] = useState<string | undefined>(defaultValues?.type);
    const [touched, setTouched] = useState<Record<FieldName, boolean>>({
      name: false,
      username: false,
      type: false,
    });
    const [submitted, setSubmitted] = useState(false);
    const [usernameStatus, setUsernameStatus] = useState<AsyncValidationStatus>('idle');
    const [pending, setPending] = useState(false);
    const [failure, setFailure] = useState<ReactNode>(null);
    const nameRef = useRef<HTMLInputElement>(null);
    const usernameRef = useRef<HTMLInputElement>(null);
    const typeRef = useRef<HTMLInputElement>(null);
    const mounted = useRef(true);
    useEffect(() => {
      mounted.current = true;
      return () => {
        mounted.current = false;
      };
    }, []);

    const trimmedName = name.trim();
    const nameError =
      trimmedName.length < rules.nameMinLength
        ? m.nameTooShort(rules.nameMinLength)
        : trimmedName.length > rules.nameMaxLength
          ? m.nameTooLong(rules.nameMaxLength)
          : null;
    const usernameRuleError =
      username.length < rules.usernameMinLength
        ? m.usernameTooShort(rules.usernameMinLength)
        : username.length > rules.usernameMaxLength
          ? m.usernameTooLong(rules.usernameMaxLength)
          : !rules.usernamePattern.test(username)
            ? m.usernameInvalidCharacters
            : null;
    const usernameError =
      usernameRuleError ??
      (submitted && checkUsername && usernameStatus === 'checking'
        ? m.usernameStillChecking
        : null);
    const typeError = types.length > 0 && !type ? m.typeRequired : null;

    const show = (field: FieldName) => submitted || touched[field];
    const touch = (field: FieldName) => setTouched((t) => (t[field] ? t : { ...t, [field]: true }));

    const validate = (value: string, signal: AbortSignal) => {
      const local =
        value.length < rules.usernameMinLength ||
        value.length > rules.usernameMaxLength ||
        !rules.usernamePattern.test(value);
      if (local) return null;
      return checkUsername ? checkUsername(value, signal) : null;
    };

    const submit = async (event: FormEvent<HTMLFormElement>) => {
      event.preventDefault();
      if (pending || disabled) return;
      setSubmitted(true);
      setFailure(null);
      const blockedByCheck =
        checkUsername !== undefined &&
        (usernameStatus === 'checking' || usernameStatus === 'invalid');
      if (nameError) return nameRef.current?.focus();
      if (usernameRuleError || blockedByCheck) return usernameRef.current?.focus();
      if (typeError) return typeRef.current?.focus();
      const values: WorkspaceOnboardingValues =
        types.length > 0 ? { name: trimmedName, username, type } : { name: trimmedName, username };
      try {
        setPending(true);
        const result = await onSubmit(values);
        if (!mounted.current) return;
        setPending(false);
        onSuccess?.(result, values);
      } catch (error) {
        if (!mounted.current) return;
        setPending(false);
        setFailure(
          errorMessage
            ? errorMessage(error)
            : error instanceof Error && error.message
              ? error.message
              : m.genericError,
        );
      }
    };

    const busy = pending || disabled;

    return (
      <form
        ref={ref}
        noValidate
        {...rest}
        className={cx('mn-workspace-onboarding-form', className)}
        aria-busy={pending || undefined}
        data-pending={pending || undefined}
        onSubmit={(event) => void submit(event)}
      >
        <Field invalid={show('name') && !!nameError} required disabled={disabled}>
          <FieldLabel>{m.nameLabel}</FieldLabel>
          <Input
            ref={nameRef}
            name="name"
            value={name}
            placeholder={m.namePlaceholder}
            maxLength={rules.nameMaxLength}
            autoComplete="organization"
            readOnly={pending}
            onChange={(event) => setName(event.target.value)}
            onBlur={() => touch('name')}
          />
          <FieldError>{show('name') ? nameError : null}</FieldError>
        </Field>

        <Field
          invalid={(show('username') && !!usernameError) || usernameStatus === 'invalid'}
          required
          disabled={disabled}
        >
          <FieldLabel>{m.usernameLabel}</FieldLabel>
          <FieldDescription>
            {m.usernameDescription(rules.usernameMinLength, rules.usernameMaxLength)}
          </FieldDescription>
          <UsernameAvailabilityInput
            ref={usernameRef}
            name="username"
            value={username}
            onValueChange={setUsername}
            validate={validate}
            {...(checkUsername ? {} : { debounce: 0 })}
            prefix={usernamePrefix}
            placeholder={m.usernamePlaceholder}
            maxLength={rules.usernameMaxLength}
            readOnly={pending}
            onBlur={() => touch('username')}
            onStatusChange={(status) => setUsernameStatus(status)}
            messages={{
              checking: m.usernameChecking,
              valid: m.usernameAvailable,
              invalid: m.usernameTaken,
              error: m.usernameCheckFailed,
              retry: m.retry,
            }}
          />
          <FieldError>{show('username') ? usernameError : null}</FieldError>
        </Field>

        {types.length > 0 ? (
          <fieldset
            className="mn-workspace-onboarding-form__types"
            disabled={busy}
            data-invalid={show('type') && !!typeError ? true : undefined}
            aria-describedby={show('type') && typeError ? `${auto}-type-error` : undefined}
          >
            <legend className="mn-workspace-onboarding-form__legend">{m.typeLabel}</legend>
            <div className="mn-workspace-onboarding-form__type-grid">
              {types.map((option, index) => {
                const id = `${auto}-type-${index}`;
                const descId = option.description ? `${id}-description` : undefined;
                return (
                  <label
                    key={option.value}
                    htmlFor={id}
                    className="mn-workspace-onboarding-form__type"
                    data-selected={type === option.value || undefined}
                  >
                    <input
                      ref={index === 0 ? typeRef : undefined}
                      id={id}
                      type="radio"
                      name={`${auto}-type`}
                      value={option.value}
                      checked={type === option.value}
                      aria-labelledby={`${id}-label`}
                      aria-describedby={descId}
                      className="mn-workspace-onboarding-form__radio"
                      onChange={() => {
                        setType(option.value);
                        touch('type');
                      }}
                    />
                    {option.icon ? (
                      <span className="mn-workspace-onboarding-form__type-icon" aria-hidden="true">
                        {option.icon}
                      </span>
                    ) : null}
                    <span id={`${id}-label`} className="mn-workspace-onboarding-form__type-label">
                      {option.label}
                    </span>
                    {option.description ? (
                      <span id={descId} className="mn-workspace-onboarding-form__type-description">
                        {option.description}
                      </span>
                    ) : null}
                  </label>
                );
              })}
            </div>
            {show('type') && typeError ? (
              <p
                id={`${auto}-type-error`}
                className="mn-workspace-onboarding-form__error"
                aria-live="polite"
              >
                {typeError}
              </p>
            ) : null}
          </fieldset>
        ) : null}

        {failure ? (
          <Alert tone="danger" live="assertive" className="mn-workspace-onboarding-form__failure">
            {failure}
          </Alert>
        ) : null}

        <div className="mn-workspace-onboarding-form__actions">
          {secondaryAction}
          <Button
            type="submit"
            variant="primary"
            loading={pending}
            disabled={disabled}
            block={!secondaryAction}
          >
            {submitLabel ?? m.submit}
          </Button>
        </div>
      </form>
    );
  },
);
