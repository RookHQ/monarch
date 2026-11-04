'use client';

import {
  forwardRef,
  useCallback,
  useEffect,
  useId,
  useRef,
  useState,
  type FormEvent,
  type HTMLAttributes,
  type ReactNode,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { CheckGlyph, ClockGlyph } from '../../../internal/icons.js';
import { Button } from '../../../components/actions/button/index.js';
import { CopyField } from '../../../components/actions/copy-field/index.js';
import { Badge, type Tone } from '../../../components/data-display/badge/index.js';
import { Alert } from '../../../components/feedback/alert/index.js';
import { Spinner } from '../../../components/feedback/spinner/index.js';
import {
  Field,
  FieldDescription,
  FieldError,
  FieldLabel,
  NativeSelect,
} from '../../../components/forms/form-field/index.js';
import { Input } from '../../../components/forms/text-field/index.js';
import { ConfirmDialog } from '../../../components/overlays/confirm-dialog/index.js';
import { Stepper } from '../../../components/workflow/stepper/index.js';

/** Where a connected domain is. The product sets it from its own DNS checks. */
export type CustomDomainStatus = 'pending' | 'verifying' | 'active' | 'failed';

/** Why a typed domain was rejected by `domainSyntaxError`. */
export type DomainSyntaxError = 'empty' | 'invalid' | 'tooLong' | 'needsSubdomain';

/** One DNS record the owner must add at their DNS provider. */
export interface CustomDomainRecord {
  /** Record type, such as `CNAME`, `TXT` or `A`. */
  type: string;
  /** Host or name exactly as it should be typed at the provider ("help" or "help.example.com"). */
  name: string;
  /** Target or content of the record. */
  value: string;
  /** Suggested TTL, shown as given ("300", "Auto"). */
  ttl?: ReactNode;
  /** What the record is for, in the product's words ("Points your address to the page"). */
  purpose?: ReactNode;
  /**
   * The product's last check of this record: `true` found, `false` not found
   * yet. Leave it out before the first check.
   */
  found?: boolean;
}

/**
 * Instructions for one DNS provider, written by the product. Monarch ships
 * no provider copy, because menus and field names change.
 */
export interface CustomDomainProviderHint {
  /** Stable id, used as the option value. */
  id: string;
  /** Provider name as shown in the picker. */
  label: string;
  /** Ordered steps for that provider. */
  steps: ReactNode[];
}

/** Visible and announced strings. Override them to translate. */
export interface CustomDomainMessages {
  /** Accessible name of the stepper; receives the domain once connected, so several on a page stay distinct. */
  stepsLabel: (domain: string | null) => string;
  /** The four step names: enter the domain, add records, verify, live. */
  steps: [string, string, string, string];
  domainLabel: string;
  placeholder: string;
  connect: string;
  /** Messages for `DomainSyntaxError` codes. */
  errors: Record<DomainSyntaxError, string>;
  /** Shown when `onConnect` rejects without an Error message. */
  connectFailed: string;
  /** Status badge text. */
  statusLabels: Record<CustomDomainStatus, string>;
  /** Status sentence under the domain, announced politely when it changes. */
  statusText: Record<CustomDomainStatus, string>;
  recordsHeading: string;
  recordsIntro: string;
  type: string;
  name: string;
  value: string;
  ttl: string;
  /** Copy-button target for a record's name, e.g. "CNAME record name". */
  recordNameTarget: (record: CustomDomainRecord, index: number) => string;
  /** Copy-button target for a record's value. */
  recordValueTarget: (record: CustomDomainRecord, index: number) => string;
  found: string;
  notFound: string;
  providerLabel: string;
  /** First option of the provider picker. */
  providerPlaceholder: string;
  verify: string;
  verifyAgain: string;
  /** Shown when `onVerify` rejects without an Error message. */
  verifyFailed: string;
  remove: string;
  removeTitle: (domain: string) => string;
  removeDescription: string;
  removeConfirm: string;
  cancel: string;
}

/** `messages` prop: any subset, including part of each nested map. */
export type CustomDomainMessagesInput = Partial<
  Omit<CustomDomainMessages, 'errors' | 'statusLabels' | 'statusText'>
> & {
  errors?: Partial<CustomDomainMessages['errors']>;
  statusLabels?: Partial<CustomDomainMessages['statusLabels']>;
  statusText?: Partial<CustomDomainMessages['statusText']>;
};

const DEFAULT_MESSAGES: CustomDomainMessages = {
  stepsLabel: (domain) => (domain ? `Setup of ${domain}` : 'Custom domain setup'),
  steps: ['Domain', 'DNS records', 'Verify', 'Live'],
  domainLabel: 'Domain',
  placeholder: 'help.example.com',
  connect: 'Connect domain',
  errors: {
    empty: 'Enter a domain.',
    invalid: 'Enter a domain such as help.example.com, without https:// or a path.',
    tooLong: 'That domain is too long. Domains are at most 253 characters.',
    needsSubdomain: 'Use a subdomain, such as help.example.com.',
  },
  connectFailed: 'The domain could not be connected. Try again.',
  statusLabels: {
    pending: 'Waiting for DNS',
    verifying: 'Verifying',
    active: 'Active',
    failed: 'Failed',
  },
  statusText: {
    pending: 'Add the DNS records below at your DNS provider, then verify.',
    verifying: 'Checking your DNS records.',
    active: 'Your domain is connected.',
    failed: 'Verification failed.',
  },
  recordsHeading: 'DNS records',
  recordsIntro: 'Add these records where the DNS for your domain is managed.',
  type: 'Type',
  name: 'Name',
  value: 'Value',
  ttl: 'TTL',
  recordNameTarget: (r, i) => `${r.type} record ${i + 1} name`,
  recordValueTarget: (r, i) => `${r.type} record ${i + 1} value`,
  found: 'Found',
  notFound: 'Not found yet',
  providerLabel: 'Where is your DNS managed?',
  providerPlaceholder: 'Choose a provider',
  verify: 'Verify DNS',
  verifyAgain: 'Check again',
  verifyFailed: 'The records could not be checked. Try again.',
  remove: 'Remove domain',
  removeTitle: (d) => `Remove ${d}?`,
  removeDescription: 'The domain stops working here. You can connect it again later.',
  removeConfirm: 'Remove domain',
  cancel: 'Cancel',
};

const STATUS_TONE: Record<CustomDomainStatus, Tone> = {
  pending: 'warning',
  verifying: 'info',
  active: 'success',
  failed: 'danger',
};

const LABEL = /^[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?$/;
const TLD = /^(?:[a-z]{2,63}|xn--[a-z0-9-]{1,59})$/;

/**
 * Turns what people paste into a bare host: trims, lowercases, drops a
 * scheme, path, query, port and trailing dot, and converts an international
 * name to its ASCII (punycode) form. Returns the text unchanged in shape
 * when it cannot be parsed, so `domainSyntaxError` can reject it.
 */
export function normalizeDomain(input: string): string {
  let host = input.trim().toLowerCase();
  host = host.replace(/^[a-z][a-z0-9+.-]*:\/\//, '');
  host = host.replace(/[/?#].*$/, '');
  host = host.replace(/:\d*$/, '');
  host = host.replace(/\.$/, '');
  if ([...host].some((c) => c.charCodeAt(0) > 127)) {
    try {
      host = new URL(`http://${host}`).hostname;
    } catch {
      // Leave it; the syntax check reports it.
    }
  }
  return host;
}

/**
 * Checks domain syntax on the client. Returns `null` for a valid host name or
 * the reason it is not. `requireSubdomain` asks for at least three labels
 * (`help.example.com`); it cannot know public suffixes such as `co.uk`, so pass
 * a product `validate` for exact rules.
 */
export function domainSyntaxError(
  input: string,
  options: { requireSubdomain?: boolean } = {},
): DomainSyntaxError | null {
  const host = normalizeDomain(input);
  if (!host) return 'empty';
  if (host.length > 253) return 'tooLong';
  const labels = host.split('.');
  if (labels.length < 2) return 'invalid';
  if (!labels.every((l) => LABEL.test(l))) return 'invalid';
  if (!TLD.test(labels[labels.length - 1] as string)) return 'invalid';
  if (options.requireSubdomain && labels.length < 3) return 'needsSubdomain';
  return null;
}

function messageOf(error: unknown, fallback: string): string {
  return error instanceof Error && error.message ? error.message : fallback;
}

export interface CustomDomainConnectProps extends Omit<HTMLAttributes<HTMLElement>, 'children'> {
  /** The connected domain (controlled), or `null` while none is connected. */
  domain?: string | null;
  /** Initial connected domain (uncontrolled). Default `null`. */
  defaultDomain?: string | null;
  /** Called with the domain after `onConnect` succeeds, and with `null` after removal. */
  onDomainChange?: (domain: string | null) => void;
  /**
   * Registers the domain with the product (normalised and syntax-checked
   * first). Return a promise to show a pending button; reject with an Error to
   * show its message under the field.
   */
  onConnect: (domain: string) => unknown;
  /**
   * Runs the product's DNS check. While its promise is pending the status
   * shows "Verifying"; the product then sets `status`. Reject to show an error.
   */
  onVerify?: () => unknown;
  /** Disconnects the domain, after a confirmation dialog. Omit to hide Remove. */
  onRemove?: () => unknown;
  /** Status of the connected domain, from the product's checks. Default `pending`. */
  status?: CustomDomainStatus;
  /** Why verification failed, in the product's words. Shown with `status="failed"`. */
  failureReason?: ReactNode;
  /** The records to add, from the product. */
  records?: CustomDomainRecord[];
  /** Keep showing the records once the domain is active. Default false. */
  showRecordsWhenActive?: boolean;
  /** Provider instructions written by the product; adds a "Where is your DNS managed?" picker. */
  providerHints?: CustomDomainProviderHint[];
  /** When the domain was last checked, already formatted ("Checked 2 minutes ago"). Not announced. */
  lastChecked?: ReactNode;
  /** Help under the domain field ("Use the address your visitors will type"). */
  inputDescription?: ReactNode;
  /** Ask for a subdomain rather than an apex domain (heuristic: at least three labels). */
  requireSubdomain?: boolean;
  /** Extra product check on the normalised domain. Return a message to block connecting. */
  validate?: (domain: string) => ReactNode | null | undefined;
  /** Disables every action, for people who may only view the setup. */
  disabled?: boolean;
  /** Level of the domain and records headings. Default 3. */
  headingLevel?: 2 | 3 | 4 | 5 | 6;
  /** Override visible and announced strings; nested maps may be partial. */
  messages?: CustomDomainMessagesInput;
  /** Product content after the records, such as a replacement or redirect option. */
  children?: ReactNode;
}

/**
 * Connects a custom domain in four steps: enter the domain (syntax checked in
 * the browser), copy the DNS records, verify, and see the result: pending,
 * verifying, active, or failed with a reason. The product runs every DNS
 * check through `onConnect`, `onVerify` and `onRemove`, and supplies any
 * provider-specific instructions.
 */
export const CustomDomainConnect = forwardRef<HTMLElement, CustomDomainConnectProps>(
  function CustomDomainConnect(
    {
      domain: domainProp,
      defaultDomain = null,
      onDomainChange,
      onConnect,
      onVerify,
      onRemove,
      status: statusProp = 'pending',
      failureReason,
      records = [],
      showRecordsWhenActive = false,
      providerHints,
      lastChecked,
      inputDescription,
      requireSubdomain = false,
      validate,
      disabled = false,
      headingLevel = 3,
      messages: messagesProp,
      className,
      children,
      ...rest
    },
    ref,
  ) {
    const messages: CustomDomainMessages = {
      ...DEFAULT_MESSAGES,
      ...messagesProp,
      errors: { ...DEFAULT_MESSAGES.errors, ...messagesProp?.errors },
      statusLabels: { ...DEFAULT_MESSAGES.statusLabels, ...messagesProp?.statusLabels },
      statusText: { ...DEFAULT_MESSAGES.statusText, ...messagesProp?.statusText },
    };
    const [innerDomain, setInnerDomain] = useState<string | null>(defaultDomain);
    const controlled = domainProp !== undefined;
    const domain = controlled ? domainProp : innerDomain;
    const setDomain = useCallback(
      (next: string | null) => {
        if (!controlled) setInnerDomain(next);
        onDomainChange?.(next);
      },
      [controlled, onDomainChange],
    );

    const [text, setText] = useState('');
    const [inputError, setInputError] = useState<ReactNode>(null);
    const [connecting, setConnecting] = useState(false);
    const [verifying, setVerifying] = useState(false);
    const [verifyError, setVerifyError] = useState<string | null>(null);
    const [provider, setProvider] = useState('');
    const headingRef = useRef<HTMLHeadingElement>(null);
    const inputRef = useRef<HTMLInputElement>(null);
    const focusAfter = useRef<'heading' | 'input' | null>(null);
    const mounted = useRef(true);
    useEffect(() => {
      mounted.current = true;
      return () => {
        mounted.current = false;
      };
    }, []);

    // Move focus to what replaced the control the person used.
    useEffect(() => {
      const target = focusAfter.current;
      focusAfter.current = null;
      if (target === 'heading') headingRef.current?.focus();
      else if (target === 'input') inputRef.current?.focus();
    }, [domain]);

    const auto = useId();
    const providerId = `${auto}-provider`;
    const Heading = `h${headingLevel}` as 'h3';

    const status: CustomDomainStatus =
      verifying && statusProp !== 'active' ? 'verifying' : statusProp;

    const submit = async (event: FormEvent<HTMLFormElement>) => {
      event.preventDefault();
      if (connecting || disabled) return;
      const code = domainSyntaxError(text, { requireSubdomain });
      if (code) {
        setInputError(messages.errors[code]);
        inputRef.current?.focus();
        return;
      }
      const host = normalizeDomain(text);
      const custom = validate?.(host);
      if (custom) {
        setInputError(custom);
        inputRef.current?.focus();
        return;
      }
      setInputError(null);
      setConnecting(true);
      try {
        await onConnect(host);
        if (!mounted.current) return;
        focusAfter.current = 'heading';
        setText('');
        setVerifyError(null);
        setDomain(host);
      } catch (error) {
        if (!mounted.current) return;
        setInputError(messageOf(error, messages.connectFailed));
        inputRef.current?.focus();
      } finally {
        if (mounted.current) setConnecting(false);
      }
    };

    const verify = async () => {
      if (!onVerify || verifying || disabled) return;
      setVerifyError(null);
      setVerifying(true);
      try {
        await onVerify();
      } catch (error) {
        if (mounted.current) setVerifyError(messageOf(error, messages.verifyFailed));
      } finally {
        if (mounted.current) setVerifying(false);
      }
    };

    const remove = async () => {
      await onRemove?.();
      if (!mounted.current) return;
      focusAfter.current = 'input';
      setVerifyError(null);
      setProvider('');
      setDomain(null);
    };

    const stepIndex = !domain ? 0 : status === 'pending' ? 1 : 2;
    const showRecords =
      domain && records.length > 0 && (status !== 'active' || showRecordsWhenActive);
    const hint = providerHints?.find((p) => p.id === provider);

    return (
      <section
        ref={ref}
        {...rest}
        className={cx('mn-custom-domain-connect', className)}
        data-step={!domain ? 'enter' : status}
      >
        {status !== 'active' || !domain ? (
          <Stepper
            aria-label={messages.stepsLabel(domain)}
            className="mn-custom-domain-connect__steps"
            current={stepIndex}
            steps={messages.steps.map((label, i) => ({
              id: String(i),
              label,
              ...(i === 2 && domain && status === 'failed' ? { status: 'error' as const } : {}),
            }))}
          />
        ) : null}

        {!domain ? (
          <form className="mn-custom-domain-connect__form" noValidate onSubmit={submit}>
            <Field invalid={Boolean(inputError)} disabled={disabled} required>
              <FieldLabel>{messages.domainLabel}</FieldLabel>
              {inputDescription ? <FieldDescription>{inputDescription}</FieldDescription> : null}
              <div className="mn-custom-domain-connect__entry">
                <Input
                  ref={inputRef}
                  value={text}
                  onChange={(e) => {
                    setText(e.target.value);
                    if (inputError) setInputError(null);
                  }}
                  placeholder={messages.placeholder}
                  inputMode="url"
                  autoComplete="off"
                  autoCapitalize="none"
                  spellCheck={false}
                  className="mn-custom-domain-connect__input"
                />
                <Button
                  type="submit"
                  variant="primary"
                  loading={connecting}
                  disabled={disabled}
                  className="mn-custom-domain-connect__connect"
                >
                  {messages.connect}
                </Button>
              </div>
              <FieldError>{inputError}</FieldError>
            </Field>
          </form>
        ) : (
          <>
            <div className="mn-custom-domain-connect__summary">
              <div className="mn-custom-domain-connect__identity">
                <Heading
                  ref={headingRef}
                  tabIndex={-1}
                  className="mn-custom-domain-connect__domain"
                >
                  {domain}
                </Heading>
                <Badge
                  tone={STATUS_TONE[status]}
                  dot
                  className="mn-custom-domain-connect__badge"
                  data-status={status}
                >
                  {messages.statusLabels[status]}
                </Badge>
              </div>
              {onRemove ? (
                <ConfirmDialog
                  tone="danger"
                  title={messages.removeTitle(domain)}
                  description={messages.removeDescription}
                  confirmLabel={messages.removeConfirm}
                  cancelLabel={messages.cancel}
                  onConfirm={remove}
                  trigger={
                    <Button
                      size="sm"
                      variant="ghost"
                      disabled={disabled}
                      className="mn-custom-domain-connect__remove"
                    >
                      {messages.remove}
                    </Button>
                  }
                />
              ) : null}
            </div>

            <div className="mn-custom-domain-connect__status">
              {/* Only the status sentence is announced; the ticking "last checked" text is not. */}
              <p role="status" aria-live="polite" className="mn-custom-domain-connect__status-text">
                {status === 'verifying' ? <Spinner size="sm" /> : null}
                {status === 'active' ? (
                  <span className="mn-custom-domain-connect__status-icon" aria-hidden>
                    <CheckGlyph />
                  </span>
                ) : null}
                <span>{messages.statusText[status]}</span>
              </p>
              {lastChecked ? (
                <p className="mn-custom-domain-connect__checked">
                  <span className="mn-custom-domain-connect__status-icon" aria-hidden>
                    <ClockGlyph />
                  </span>
                  {lastChecked}
                </p>
              ) : null}
            </div>

            {status === 'failed' && failureReason ? (
              <Alert tone="danger" live="assertive" className="mn-custom-domain-connect__failure">
                {failureReason}
              </Alert>
            ) : null}
            {verifyError ? (
              <Alert tone="danger" live="assertive" className="mn-custom-domain-connect__failure">
                {verifyError}
              </Alert>
            ) : null}

            {showRecords ? (
              <div className="mn-custom-domain-connect__records">
                <Heading className="mn-custom-domain-connect__heading">
                  {messages.recordsHeading}
                </Heading>
                <p className="mn-custom-domain-connect__intro">{messages.recordsIntro}</p>
                <ul className="mn-custom-domain-connect__list" aria-label={messages.recordsHeading}>
                  {records.map((record, index) => (
                    <li
                      key={`${record.type}-${record.name}-${index}`}
                      className="mn-custom-domain-connect__record"
                      data-found={
                        record.found === undefined ? undefined : record.found ? 'true' : 'false'
                      }
                    >
                      <div className="mn-custom-domain-connect__record-head">
                        <span className="mn-custom-domain-connect__type">
                          <span className="mn-visually-hidden">{messages.type}: </span>
                          {record.type}
                        </span>
                        {record.purpose ? (
                          <span className="mn-custom-domain-connect__purpose">
                            {record.purpose}
                          </span>
                        ) : null}
                        {record.found !== undefined ? (
                          <Badge
                            tone={record.found ? 'success' : 'neutral'}
                            size="sm"
                            className="mn-custom-domain-connect__found"
                          >
                            {record.found ? messages.found : messages.notFound}
                          </Badge>
                        ) : null}
                      </div>
                      <Field className="mn-custom-domain-connect__value">
                        <FieldLabel>{messages.name}</FieldLabel>
                        <CopyField
                          size="sm"
                          iconOnly
                          value={record.name}
                          label={messages.recordNameTarget(record, index)}
                        />
                      </Field>
                      <Field className="mn-custom-domain-connect__value">
                        <FieldLabel>{messages.value}</FieldLabel>
                        <CopyField
                          size="sm"
                          iconOnly
                          value={record.value}
                          label={messages.recordValueTarget(record, index)}
                        />
                      </Field>
                      {record.ttl !== undefined ? (
                        <p className="mn-custom-domain-connect__ttl">
                          {messages.ttl}: {record.ttl}
                        </p>
                      ) : null}
                    </li>
                  ))}
                </ul>
              </div>
            ) : null}

            {showRecords && providerHints && providerHints.length > 0 ? (
              <div className="mn-custom-domain-connect__provider">
                <label htmlFor={providerId} className="mn-custom-domain-connect__provider-label">
                  {messages.providerLabel}
                </label>
                <NativeSelect
                  id={providerId}
                  size="sm"
                  value={provider}
                  placeholder={messages.providerPlaceholder}
                  options={providerHints.map((p) => ({ value: p.id, label: p.label }))}
                  onChange={(e) => setProvider(e.target.value)}
                  wrapperClassName="mn-custom-domain-connect__provider-select"
                />
                {hint ? (
                  <ol className="mn-custom-domain-connect__provider-steps" aria-label={hint.label}>
                    {hint.steps.map((step, i) => (
                      <li key={i}>{step}</li>
                    ))}
                  </ol>
                ) : null}
              </div>
            ) : null}

            {children}

            {onVerify && status !== 'active' ? (
              <div className="mn-custom-domain-connect__actions">
                <Button
                  variant="primary"
                  loading={status === 'verifying'}
                  disabled={disabled}
                  onClick={verify}
                >
                  {status === 'failed' ? messages.verifyAgain : messages.verify}
                </Button>
              </div>
            ) : null}
          </>
        )}
      </section>
    );
  },
);
