'use client';

import {
  forwardRef,
  useEffect,
  useId,
  useRef,
  useState,
  type FormEvent,
  type HTMLAttributes,
  type ReactNode,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { isThenable } from '../../../internal/dialog-helpers.js';
import { Button } from '../../../components/actions/button/index.js';
import { Alert } from '../../../components/feedback/alert/index.js';
import { Checkbox } from '../../../components/forms/checkbox/index.js';
import {
  Field,
  FieldDescription,
  FieldError,
  FieldLabel,
  NativeSelect,
} from '../../../components/forms/form-field/index.js';
import { Input, Textarea } from '../../../components/forms/text-field/index.js';

/** One kind of privacy request the form offers. */
export interface DataSubjectRequestType {
  /** Stable id sent with the request, e.g. `access`. */
  id: string;
  /** Short label on the card, e.g. "Get a copy of my data". */
  label: string;
  /** One line under the label explaining the request. */
  description: string;
  /** The details field must be filled for this type (e.g. what to correct). */
  detailsRequired?: boolean;
}

/**
 * The request types most products offer. Pass your own `types` to reword,
 * remove or add some; which ones apply where is for the product's legal team.
 */
export const DEFAULT_DATA_SUBJECT_REQUEST_TYPES: readonly DataSubjectRequestType[] = [
  {
    id: 'access',
    label: 'Get a copy of my data',
    description: 'Receive a copy of the personal information held about you.',
  },
  {
    id: 'delete',
    label: 'Delete my data',
    description: 'Ask for your personal information to be deleted.',
  },
  {
    id: 'correct',
    label: 'Correct my data',
    description: 'Fix personal information that is wrong or incomplete.',
    detailsRequired: true,
  },
  {
    id: 'object',
    label: 'Object to or restrict processing',
    description: 'Ask for your information to stop being used, or to be used less.',
  },
  {
    id: 'opt-out',
    label: 'Opt out of sale or sharing',
    description: 'Stop your information from being sold or shared for advertising.',
  },
  {
    id: 'withdraw-consent',
    label: 'Withdraw consent',
    description: 'Take back consent you gave earlier, for example for marketing.',
  },
];

/** Who the request is for: the person themselves, or someone acting for them. */
export type DataSubjectRelationship = 'self' | 'authorized-agent';

/** What the form sends to `onSubmit`. Text values are trimmed. */
export interface DataSubjectRequest {
  /** The chosen type's `id`. */
  type: string;
  /** Name of the person the request is about. May be empty. */
  name: string;
  /** Email of the person the request is about. */
  email: string;
  relationship: DataSubjectRelationship;
  /** The agent's own details, when `relationship` is `authorized-agent`. */
  agent?: { name: string; email: string };
  /** Country code (from `countries`) or the typed country or region. */
  country: string;
  details: string;
  /** Always `true`: the form does not submit without the acknowledgement. */
  acknowledged: true;
}

/** What `onSubmit` may resolve with: a reference id, an object holding one, or nothing. */
export type DataSubjectRequestResult = { referenceId?: string } | string | undefined;

/** A country or region in the jurisdiction select. */
export interface DataSubjectRequestCountry {
  /** Stored value, e.g. an ISO 3166 code such as `DE` or `US-CA`. */
  code: string;
  label: string;
}

/** Every visible string of DataSubjectRequestForm. Pass any subset to translate or reword. */
export interface DataSubjectRequestFormMessages {
  typeLegend: string;
  typeRequired: string;
  relationshipLegend: string;
  relationshipSelf: string;
  relationshipAgent: string;
  nameLabel: string;
  /** Name label when an agent fills in the form. */
  nameLabelAgent: string;
  emailLabel: string;
  emailLabelAgent: string;
  emailDescription: string;
  emailRequired: string;
  emailInvalid: string;
  agentLegend: string;
  agentNote: string;
  agentNameLabel: string;
  agentNameRequired: string;
  agentEmailLabel: string;
  agentEmailRequired: string;
  countryLabel: string;
  countryPlaceholder: string;
  countryRequired: string;
  detailsLabel: string;
  detailsDescription: string;
  detailsRequired: string;
  acknowledgement: string;
  acknowledgementRequired: string;
  optional: string;
  submit: string;
  /** Title of the error summary shown after an invalid submit. */
  errorSummaryTitle: (count: number) => string;
  /** Shown when `onSubmit` rejects without an Error message. */
  submitFailed: string;
  successTitle: string;
  /** Label before the reference id. */
  successReference: string;
  successDescription: string;
}

const DEFAULT_MESSAGES: DataSubjectRequestFormMessages = {
  typeLegend: 'What would you like to do?',
  typeRequired: 'Choose what you would like to do.',
  relationshipLegend: 'Who is this request for?',
  relationshipSelf: 'Myself',
  relationshipAgent: 'Someone I am authorized to act for',
  nameLabel: 'Full name',
  nameLabelAgent: 'Their full name',
  emailLabel: 'Email',
  emailLabelAgent: 'Their email',
  emailDescription: 'Used to identify the account and to reply about this request.',
  emailRequired: 'Enter an email address.',
  emailInvalid: 'Enter an email address like name@example.com.',
  agentLegend: 'Your details as their agent',
  agentNote: 'You may be asked for proof that you can act for this person.',
  agentNameLabel: 'Your full name',
  agentNameRequired: 'Enter your full name.',
  agentEmailLabel: 'Your email',
  agentEmailRequired: 'Enter your email address.',
  countryLabel: 'Country or region where the person lives',
  countryPlaceholder: 'Choose a country or region',
  countryRequired: 'Choose a country or region.',
  detailsLabel: 'Details',
  detailsDescription: 'Anything that helps find the information or explains the request.',
  detailsRequired: 'Describe what the request is about.',
  acknowledgement: 'The information I gave is accurate',
  acknowledgementRequired: 'Confirm that the information is accurate.',
  optional: 'Optional',
  submit: 'Send request',
  errorSummaryTitle: (count) =>
    count === 1 ? 'There is a problem with 1 answer' : `There are problems with ${count} answers`,
  submitFailed: 'The request did not go through. Try again.',
  successTitle: 'Request received',
  successReference: 'Your reference',
  successDescription: 'Keep this reference in case you need to ask about the request.',
};

const EMAIL = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

type FieldKey =
  'type' | 'name' | 'email' | 'agentName' | 'agentEmail' | 'country' | 'details' | 'acknowledged';
const ORDER: FieldKey[] = [
  'type',
  'name',
  'email',
  'agentName',
  'agentEmail',
  'country',
  'details',
  'acknowledged',
];

export interface DataSubjectRequestFormProps extends Omit<
  HTMLAttributes<HTMLDivElement>,
  'onSubmit' | 'defaultValue'
> {
  /**
   * Sends the request. Return a promise: the button shows as busy until it
   * settles; resolving shows the success screen (with the reference id when
   * the value is a string or `{ referenceId }`); rejecting shows its message
   * and keeps everything the person typed.
   */
  onSubmit: (request: DataSubjectRequest) => unknown;
  /** The request types, shown as radio cards in this order. Default `DEFAULT_DATA_SUBJECT_REQUEST_TYPES`. */
  types?: readonly DataSubjectRequestType[];
  /**
   * Countries or regions for the jurisdiction select. Without it the field is
   * a text input. Monarch ships no country list; pass the product's own.
   */
  countries?: readonly DataSubjectRequestCountry[];
  /** Values filled in at first, e.g. the signed-in person's email or a type from the URL. */
  defaultValues?: Partial<
    Pick<DataSubjectRequest, 'type' | 'name' | 'email' | 'country' | 'relationship'>
  >;
  /** How identity will be checked, shown before the acknowledgement, e.g. "We will email you a link to confirm." */
  verificationNote?: ReactNode;
  /** When to expect a reply, shown on the success screen. No default: it is the product's to state. */
  responseTimeNote?: ReactNode;
  /** Turns a rejected `onSubmit` into a message. Default: the Error's message, or `messages.submitFailed`. */
  errorMessage?: (error: unknown) => ReactNode;
  /** Heading element of the success screen. Default `h2`. */
  headingLevel?: 'h2' | 'h3' | 'h4';
  /** Extra content on the success screen, after the notes (e.g. a link home). */
  successActions?: ReactNode;
  messages?: Partial<DataSubjectRequestFormMessages>;
}

function referenceOf(value: unknown): string | null {
  if (typeof value === 'string' && value) return value;
  if (value && typeof value === 'object' && 'referenceId' in value) {
    const id = (value as { referenceId?: unknown }).referenceId;
    return typeof id === 'string' && id ? id : null;
  }
  return null;
}

/**
 * A public form for privacy requests: what the person wants (radio cards),
 * who it is for (themselves or as an authorized agent), contact details,
 * country or region, details, an optional note on identity checks, and a
 * required accuracy acknowledgement. Validates on submit with an error
 * summary and focus on the first problem; shows the reference id once sent.
 */
export const DataSubjectRequestForm = forwardRef<HTMLDivElement, DataSubjectRequestFormProps>(
  function DataSubjectRequestForm(
    {
      onSubmit,
      types = DEFAULT_DATA_SUBJECT_REQUEST_TYPES,
      countries,
      defaultValues,
      verificationNote,
      responseTimeNote,
      errorMessage,
      headingLevel: Heading = 'h2',
      successActions,
      messages: messagesProp,
      className,
      ...rest
    },
    ref,
  ) {
    const messages = { ...DEFAULT_MESSAGES, ...messagesProp };
    const base = `mn-dsr-${useId().replace(/:/g, '')}`;
    const ids: Record<FieldKey, string> = {
      type: `${base}-type`,
      name: `${base}-name`,
      email: `${base}-email`,
      agentName: `${base}-agent-name`,
      agentEmail: `${base}-agent-email`,
      country: `${base}-country`,
      details: `${base}-details`,
      acknowledged: `${base}-ack`,
    };

    const [type, setType] = useState(defaultValues?.type ?? '');
    const [relationship, setRelationship] = useState<DataSubjectRelationship>(
      defaultValues?.relationship ?? 'self',
    );
    const [name, setName] = useState(defaultValues?.name ?? '');
    const [email, setEmail] = useState(defaultValues?.email ?? '');
    const [agentName, setAgentName] = useState('');
    const [agentEmail, setAgentEmail] = useState('');
    const [country, setCountry] = useState(defaultValues?.country ?? '');
    const [details, setDetails] = useState('');
    const [acknowledged, setAcknowledged] = useState(false);
    const [attempted, setAttempted] = useState(false);
    const [summaryKeys, setSummaryKeys] = useState<FieldKey[]>([]);
    const [summaryRun, setSummaryRun] = useState(0);
    const [running, setRunning] = useState(false);
    const [failure, setFailure] = useState<ReactNode>(null);
    const [done, setDone] = useState<{ reference: string | null } | null>(null);
    const rootRef = useRef<HTMLDivElement | null>(null);
    const successRef = useRef<HTMLHeadingElement>(null);
    const mounted = useRef(true);
    useEffect(() => {
      mounted.current = true;
      return () => {
        mounted.current = false;
      };
    }, []);
    useEffect(() => {
      if (done) successRef.current?.focus();
    }, [done]);

    const agent = relationship === 'authorized-agent';
    const chosenType = types.find((t) => t.id === type);

    const validate = (): Partial<Record<FieldKey, string>> => {
      const errors: Partial<Record<FieldKey, string>> = {};
      if (!chosenType) errors.type = messages.typeRequired;
      if (!email.trim()) errors.email = messages.emailRequired;
      else if (!EMAIL.test(email.trim())) errors.email = messages.emailInvalid;
      if (agent) {
        if (!agentName.trim()) errors.agentName = messages.agentNameRequired;
        if (!agentEmail.trim()) errors.agentEmail = messages.agentEmailRequired;
        else if (!EMAIL.test(agentEmail.trim())) errors.agentEmail = messages.emailInvalid;
      }
      if (!country.trim()) errors.country = messages.countryRequired;
      if (chosenType?.detailsRequired && !details.trim()) errors.details = messages.detailsRequired;
      if (!acknowledged) errors.acknowledged = messages.acknowledgementRequired;
      return errors;
    };
    const errors = attempted ? validate() : {};
    const summary = summaryKeys.filter((key) => errors[key]);

    const focusField = (key: FieldKey) => {
      const root = rootRef.current;
      if (!root) return;
      if (key === 'type') {
        const radios = Array.from(
          root.querySelectorAll<HTMLInputElement>(`input[name="${ids.type}"]`),
        );
        (radios.find((r) => r.checked) ?? radios[0])?.focus();
        return;
      }
      root.ownerDocument.getElementById(ids[key])?.focus();
    };

    const submit = (event: FormEvent<HTMLFormElement>) => {
      event.preventDefault();
      if (running) return;
      setAttempted(true);
      setFailure(null);
      const found = validate();
      const keys = ORDER.filter((key) => found[key]);
      setSummaryKeys(keys);
      setSummaryRun((n) => n + 1);
      if (keys[0]) {
        focusField(keys[0]);
        return;
      }
      const request: DataSubjectRequest = {
        type,
        name: name.trim(),
        email: email.trim(),
        relationship,
        ...(agent ? { agent: { name: agentName.trim(), email: agentEmail.trim() } } : {}),
        country: country.trim(),
        details: details.trim(),
        acknowledged: true,
      };
      const toMessage = (error: unknown): ReactNode =>
        errorMessage
          ? errorMessage(error)
          : error instanceof Error && error.message
            ? error.message
            : messages.submitFailed;
      let result: unknown;
      try {
        result = onSubmit(request);
      } catch (error) {
        setFailure(toMessage(error));
        return;
      }
      if (!isThenable(result)) {
        setDone({ reference: referenceOf(result) });
        return;
      }
      setRunning(true);
      Promise.resolve(result).then(
        (value) => {
          if (!mounted.current) return;
          setRunning(false);
          setDone({ reference: referenceOf(value) });
        },
        (error: unknown) => {
          if (!mounted.current) return;
          setRunning(false);
          setFailure(toMessage(error));
        },
      );
    };

    const setRefs = (node: HTMLDivElement | null) => {
      rootRef.current = node;
      if (typeof ref === 'function') ref(node);
      else if (ref) ref.current = node;
    };

    if (done) {
      return (
        <div
          ref={setRefs}
          {...rest}
          className={cx('mn-data-subject-request-form', className)}
          data-state="sent"
        >
          <div className="mn-data-subject-request-form__success">
            <Heading
              ref={successRef}
              tabIndex={-1}
              className="mn-data-subject-request-form__success-title"
            >
              {messages.successTitle}
            </Heading>
            {done.reference ? (
              <p className="mn-data-subject-request-form__reference">
                <span className="mn-data-subject-request-form__reference-label">
                  {messages.successReference}
                </span>{' '}
                <span className="mn-data-subject-request-form__reference-id">{done.reference}</span>
              </p>
            ) : null}
            {done.reference ? (
              <p className="mn-data-subject-request-form__success-text">
                {messages.successDescription}
              </p>
            ) : null}
            {responseTimeNote ? (
              <p className="mn-data-subject-request-form__success-text">{responseTimeNote}</p>
            ) : null}
            {successActions ? (
              <div className="mn-data-subject-request-form__success-actions">{successActions}</div>
            ) : null}
          </div>
        </div>
      );
    }

    const typeErrorId = `${ids.type}-error`;

    return (
      <div
        ref={setRefs}
        {...rest}
        className={cx('mn-data-subject-request-form', className)}
        data-state={running ? 'sending' : 'editing'}
      >
        <form
          className="mn-data-subject-request-form__form"
          onSubmit={submit}
          noValidate
          aria-busy={running || undefined}
        >
          {summary.length > 0 ? (
            <div key={summaryRun} role="alert" className="mn-data-subject-request-form__summary">
              <p className="mn-data-subject-request-form__summary-title">
                {messages.errorSummaryTitle(summary.length)}
              </p>
              <ul className="mn-data-subject-request-form__summary-list">
                {summary.map((key) => (
                  <li key={key}>
                    <a
                      href={`#${ids[key]}`}
                      onClick={(e) => {
                        e.preventDefault();
                        focusField(key);
                      }}
                    >
                      {errors[key]}
                    </a>
                  </li>
                ))}
              </ul>
            </div>
          ) : null}

          <fieldset
            className="mn-data-subject-request-form__group"
            aria-describedby={errors.type ? typeErrorId : undefined}
            data-invalid={errors.type ? '' : undefined}
          >
            <legend className="mn-data-subject-request-form__legend">
              {messages.typeLegend}
              <span className="mn-data-subject-request-form__required" aria-hidden="true">
                *
              </span>
            </legend>
            {errors.type ? (
              <p id={typeErrorId} className="mn-data-subject-request-form__error">
                {errors.type}
              </p>
            ) : null}
            <div className="mn-data-subject-request-form__types">
              {types.map((option, index) => {
                const checked = option.id === type;
                const labelId = `${ids.type}-${index}-label`;
                const descriptionId = `${ids.type}-${index}-description`;
                return (
                  <label
                    key={option.id}
                    className="mn-data-subject-request-form__choice"
                    data-state={checked ? 'checked' : 'unchecked'}
                  >
                    <input
                      type="radio"
                      name={ids.type}
                      value={option.id}
                      checked={checked}
                      required
                      onChange={() => setType(option.id)}
                      aria-labelledby={labelId}
                      aria-describedby={descriptionId}
                      className="mn-data-subject-request-form__radio"
                    />
                    <span className="mn-data-subject-request-form__choice-text">
                      <span id={labelId} className="mn-data-subject-request-form__choice-label">
                        {option.label}
                      </span>
                      <span
                        id={descriptionId}
                        className="mn-data-subject-request-form__choice-description"
                      >
                        {option.description}
                      </span>
                    </span>
                  </label>
                );
              })}
            </div>
          </fieldset>

          <fieldset className="mn-data-subject-request-form__group">
            <legend className="mn-data-subject-request-form__legend">
              {messages.relationshipLegend}
            </legend>
            <div className="mn-data-subject-request-form__relationship">
              {(['self', 'authorized-agent'] as const).map((value) => (
                <label
                  key={value}
                  className="mn-data-subject-request-form__choice"
                  data-compact=""
                  data-state={relationship === value ? 'checked' : 'unchecked'}
                >
                  <input
                    type="radio"
                    name={`${base}-relationship`}
                    value={value}
                    checked={relationship === value}
                    onChange={() => setRelationship(value)}
                    className="mn-data-subject-request-form__radio"
                  />
                  <span className="mn-data-subject-request-form__choice-label">
                    {value === 'self' ? messages.relationshipSelf : messages.relationshipAgent}
                  </span>
                </label>
              ))}
            </div>
          </fieldset>

          {agent ? (
            <fieldset className="mn-data-subject-request-form__group mn-data-subject-request-form__agent">
              <legend className="mn-data-subject-request-form__legend">
                {messages.agentLegend}
              </legend>
              <p className="mn-data-subject-request-form__hint">{messages.agentNote}</p>
              <Field id={ids.agentName} required invalid={!!errors.agentName}>
                <FieldLabel>{messages.agentNameLabel}</FieldLabel>
                <Input
                  autoComplete="name"
                  value={agentName}
                  onChange={(e) => setAgentName(e.target.value)}
                />
                <FieldError>{errors.agentName}</FieldError>
              </Field>
              <Field id={ids.agentEmail} required invalid={!!errors.agentEmail}>
                <FieldLabel>{messages.agentEmailLabel}</FieldLabel>
                <Input
                  type="email"
                  autoComplete="email"
                  value={agentEmail}
                  onChange={(e) => setAgentEmail(e.target.value)}
                />
                <FieldError>{errors.agentEmail}</FieldError>
              </Field>
            </fieldset>
          ) : null}

          <Field id={ids.name}>
            <FieldLabel optionalText={messages.optional}>
              {agent ? messages.nameLabelAgent : messages.nameLabel}
            </FieldLabel>
            <Input
              autoComplete={agent ? 'off' : 'name'}
              value={name}
              onChange={(e) => setName(e.target.value)}
            />
          </Field>
          <Field id={ids.email} required invalid={!!errors.email}>
            <FieldLabel>{agent ? messages.emailLabelAgent : messages.emailLabel}</FieldLabel>
            <Input
              type="email"
              autoComplete={agent ? 'off' : 'email'}
              value={email}
              onChange={(e) => setEmail(e.target.value)}
            />
            <FieldDescription>{messages.emailDescription}</FieldDescription>
            <FieldError>{errors.email}</FieldError>
          </Field>
          <Field id={ids.country} required invalid={!!errors.country}>
            <FieldLabel>{messages.countryLabel}</FieldLabel>
            {countries ? (
              <NativeSelect
                value={country}
                onChange={(e) => setCountry(e.target.value)}
                placeholder={messages.countryPlaceholder}
                options={countries.map((c) => ({ value: c.code, label: c.label }))}
              />
            ) : (
              <Input
                autoComplete={agent ? 'off' : 'country-name'}
                value={country}
                onChange={(e) => setCountry(e.target.value)}
              />
            )}
            <FieldError>{errors.country}</FieldError>
          </Field>
          <Field
            id={ids.details}
            required={chosenType?.detailsRequired === true}
            invalid={!!errors.details}
          >
            <FieldLabel optionalText={messages.optional}>{messages.detailsLabel}</FieldLabel>
            <Textarea rows={4} value={details} onChange={(e) => setDetails(e.target.value)} />
            <FieldDescription>{messages.detailsDescription}</FieldDescription>
            <FieldError>{errors.details}</FieldError>
          </Field>

          {verificationNote ? (
            <Alert tone="info" className="mn-data-subject-request-form__verification">
              {verificationNote}
            </Alert>
          ) : null}

          <Field id={ids.acknowledged} required invalid={!!errors.acknowledged}>
            <Checkbox
              label={messages.acknowledgement}
              checked={acknowledged}
              onCheckedChange={(value) => setAcknowledged(value === true)}
            />
            <FieldError>{errors.acknowledged}</FieldError>
          </Field>

          {failure ? (
            <Alert tone="danger" live="assertive" className="mn-data-subject-request-form__failure">
              {failure}
            </Alert>
          ) : null}

          <div className="mn-data-subject-request-form__actions">
            <Button type="submit" variant="primary" loading={running}>
              {messages.submit}
            </Button>
          </div>
        </form>
      </div>
    );
  },
);
