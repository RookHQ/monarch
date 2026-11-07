'use client';

import { forwardRef, useId, useState, type HTMLAttributes, type ReactNode } from 'react';
import { cx } from '../../../internal/cx.js';
import { CopyField } from '../../actions/copy-field/index.js';
import { StatusBadge } from '../../data-display/status-badge/index.js';
import { Field, FieldLabel, NativeSelect } from '../../forms/form-field/index.js';

/** One DNS record the user has to create at their DNS provider. */
export interface DnsRecord {
  /** Stable key. Default: type, name and value joined. */
  id?: string | undefined;
  /** Record type: `CNAME`, `TXT`, `A`, `AAAA`, `MX`, `CAA`… */
  type: string;
  /** The full record name, e.g. `book.example.com`. Shown relative to `zone` when given. */
  name: string;
  /** The record value or target. */
  value: string;
  /**
   * What the record is for. `routing`, `verification` and `certificate` have
   * built-in labels; any other string is shown as given.
   */
  purpose?: 'routing' | 'verification' | 'certificate' | (string & {}) | undefined;
  /** TTL to set, in seconds or as the provider writes it ("Auto"). */
  ttl?: number | string | undefined;
  /** MX or SRV priority. */
  priority?: number | undefined;
  /**
   * Whether public DNS already shows this record. `true` "Found", `false`
   * "Waiting"; leave unset when the product has not checked.
   */
  ok?: boolean | undefined;
}

export interface DnsRecordTableMessages {
  type: string;
  name: string;
  value: string;
  ttl: string;
  priority: string;
  status: string;
  found: string;
  waiting: string;
  /** Label above a card, by the record's `purpose`. */
  purpose: (purpose: string | undefined) => string;
  /** Names the copy box and button of a record's name. */
  copyName: (record: DnsRecord, shownName: string) => string;
  /** Names the copy box and button of a record's value. */
  copyValue: (record: DnsRecord, shownName: string) => string;
  /** Under a card whose name is shown relative to the zone. */
  fullName: (name: string) => string;
  /** Accessible name of the list or table. */
  label: (zone: string | undefined) => string;
  /** Announced politely when a record's found state changes. */
  summary: (found: number, total: number) => string;
}

const DEFAULT_MESSAGES: DnsRecordTableMessages = {
  type: 'Type',
  name: 'Name',
  value: 'Value',
  ttl: 'TTL',
  priority: 'Priority',
  status: 'Status',
  found: 'Found',
  waiting: 'Waiting',
  purpose: (p) =>
    p === 'routing'
      ? 'Points your address here'
      : p === 'certificate'
        ? 'Allows the HTTPS certificate'
        : p === 'verification' || p === undefined
          ? 'Proves you own the domain'
          : p,
  copyName: (r) => `name of the ${r.type} record`,
  copyValue: (r, shown) => `value of the ${r.type} record ${shown}`,
  fullName: (name) => `Full name: ${name}`,
  label: (zone) => (zone ? `DNS records for ${zone}` : 'DNS records'),
  summary: (found, total) => `${found} of ${total} records found.`,
};

/**
 * The record name as most DNS providers want it typed: relative to the zone
 * (`book` for `book.example.com` in `example.com`), `@` for the zone apex.
 * Returns the full name unchanged when it is not inside the zone.
 */
export function relativeDnsName(name: string, zone: string | undefined): string {
  if (!zone) return name;
  const n = name.replace(/\.$/, '').toLowerCase();
  const z = zone.replace(/\.$/, '').toLowerCase();
  if (n === z) return '@';
  return n.endsWith(`.${z}`) ? name.slice(0, n.length - z.length - 1) : name;
}

const recordKey = (r: DnsRecord) => r.id ?? `${r.type}:${r.name}:${r.value}`;

export interface DnsRecordTableProps extends Omit<HTMLAttributes<HTMLDivElement>, 'onCopy'> {
  /** The records to show, in the order the user should create them. */
  records: readonly DnsRecord[];
  /**
   * The DNS zone the user manages (`example.com`). Names are then shown the
   * way providers want them typed (`book`, `@`) with the full name below.
   */
  zone?: string | undefined;
  /**
   * `cards` (default) stacks one card per record and fits any width; `table`
   * is a compact grid for wide screens and scrolls sideways inside itself
   * when narrow.
   */
  layout?: 'cards' | 'table' | undefined;
  /** Heading level of each card's purpose label. Default `h3`. */
  headingLevel?: 'h2' | 'h3' | 'h4' | 'h5' | undefined;
  /** Called with the copied text after a name or value is copied. */
  onCopy?: ((text: string) => void) | undefined;
  /** Interface text, for translation. */
  messages?: Partial<DnsRecordTableMessages> | undefined;
}

/**
 * The DNS records a user must add at their DNS provider to connect a custom
 * domain or verify a sender: type, name and value with copy buttons, what each
 * record is for, and whether public DNS already shows it.
 */
export const DnsRecordTable = forwardRef<HTMLDivElement, DnsRecordTableProps>(
  function DnsRecordTable(
    {
      records,
      zone,
      layout = 'cards',
      headingLevel = 'h3',
      onCopy,
      messages,
      className,
      'aria-label': ariaLabel,
      ...rest
    },
    ref,
  ) {
    const m = { ...DEFAULT_MESSAGES, ...messages };
    const label = ariaLabel ?? m.label(zone);
    const checked = records.filter((r) => r.ok !== undefined);
    const found = checked.filter((r) => r.ok).length;
    const hasTtl = records.some((r) => r.ttl !== undefined);
    const hasPriority = records.some((r) => r.priority !== undefined);
    const H = headingLevel;
    const copyProps = onCopy ? { onCopy } : {};

    const status = (r: DnsRecord) =>
      r.ok === undefined ? null : (
        <StatusBadge
          size="sm"
          status={r.ok ? 'success' : 'neutral'}
          label={r.ok ? m.found : m.waiting}
          className="mn-dns-record-table__status"
        />
      );

    return (
      <div
        ref={ref}
        {...rest}
        className={cx('mn-dns-record-table', className)}
        data-layout={layout}
      >
        {layout === 'table' ? (
          <div
            className="mn-dns-record-table__scroll"
            role="region"
            aria-label={label}
            // The region is focusable so keyboard users can scroll a wide table (WCAG 2.1.1).
            // eslint-disable-next-line jsx-a11y/no-noninteractive-tabindex
            tabIndex={0}
          >
            <table className="mn-dns-record-table__table">
              <thead>
                <tr>
                  <th scope="col">{m.type}</th>
                  <th scope="col">{m.name}</th>
                  <th scope="col">{m.value}</th>
                  {hasPriority ? <th scope="col">{m.priority}</th> : null}
                  {hasTtl ? <th scope="col">{m.ttl}</th> : null}
                  {checked.length ? <th scope="col">{m.status}</th> : null}
                </tr>
              </thead>
              <tbody>
                {records.map((r) => {
                  const shown = relativeDnsName(r.name, zone);
                  return (
                    <tr key={recordKey(r)} data-ok={r.ok === undefined ? undefined : String(r.ok)}>
                      <td className="mn-dns-record-table__type">{r.type}</td>
                      <td>
                        <CopyField
                          size="sm"
                          iconOnly
                          value={shown}
                          label={m.copyName(r, shown)}
                          {...copyProps}
                        />
                      </td>
                      <td>
                        <CopyField
                          size="sm"
                          iconOnly
                          value={r.value}
                          label={m.copyValue(r, shown)}
                          {...copyProps}
                        />
                      </td>
                      {hasPriority ? <td>{r.priority ?? ''}</td> : null}
                      {hasTtl ? <td>{r.ttl ?? ''}</td> : null}
                      {checked.length ? <td>{status(r)}</td> : null}
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        ) : (
          <ul className="mn-dns-record-table__list" aria-label={label}>
            {records.map((r) => {
              const shown = relativeDnsName(r.name, zone);
              return (
                <li
                  key={recordKey(r)}
                  className="mn-dns-record-table__card"
                  data-ok={r.ok === undefined ? undefined : String(r.ok)}
                >
                  <div className="mn-dns-record-table__head">
                    <H className="mn-dns-record-table__purpose">{m.purpose(r.purpose)}</H>
                    {status(r)}
                  </div>
                  <dl className="mn-dns-record-table__grid">
                    <div className="mn-dns-record-table__cell" data-part="type">
                      <dt>{m.type}</dt>
                      <dd className="mn-dns-record-table__type">{r.type}</dd>
                    </div>
                    <div className="mn-dns-record-table__cell" data-part="name">
                      <dt>{m.name}</dt>
                      <dd>
                        <CopyField
                          iconOnly
                          value={shown}
                          label={m.copyName(r, shown)}
                          {...copyProps}
                        />
                      </dd>
                    </div>
                    <div className="mn-dns-record-table__cell" data-part="value">
                      <dt>{m.value}</dt>
                      <dd>
                        <CopyField
                          iconOnly
                          value={r.value}
                          label={m.copyValue(r, shown)}
                          {...copyProps}
                        />
                      </dd>
                    </div>
                    {r.priority !== undefined ? (
                      <div className="mn-dns-record-table__cell" data-part="priority">
                        <dt>{m.priority}</dt>
                        <dd className="mn-dns-record-table__type">{r.priority}</dd>
                      </div>
                    ) : null}
                    {r.ttl !== undefined ? (
                      <div className="mn-dns-record-table__cell" data-part="ttl">
                        <dt>{m.ttl}</dt>
                        <dd className="mn-dns-record-table__type">{r.ttl}</dd>
                      </div>
                    ) : null}
                  </dl>
                  {shown !== r.name ? (
                    <p className="mn-dns-record-table__full">{m.fullName(r.name)}</p>
                  ) : null}
                </li>
              );
            })}
          </ul>
        )}
        {checked.length ? (
          <p className="mn-visually-hidden" role="status">
            {m.summary(found, checked.length)}
          </p>
        ) : null}
      </div>
    );
  },
);

/* ------------------------------------------------------------------ */
/* Provider guide                                                      */
/* ------------------------------------------------------------------ */

/** The record a provider guide explains, with the name in both forms. */
export interface DnsGuideRecord {
  type: string;
  /** Name relative to the zone (`book`, `@`). */
  name: string;
  /** The full name (`book.example.com`). */
  fullName: string;
  value: string;
}

/** Step-by-step instructions for one DNS provider. */
export interface DnsProviderInstructions {
  id: string;
  /** Shown in the provider picker. */
  label: string;
  /** One list item per step. */
  steps: (record: DnsGuideRecord) => ReactNode[];
}

const strong = (text: string) => <strong>{text}</strong>;
const code = (text: string) => <code>{text}</code>;
const field = (label: string, value: string) => (
  <>
    {strong(label)} {code(value)}
  </>
);

/**
 * Built-in English steps for the most common DNS providers, current at the
 * time of writing. Pass your own list to `DnsProviderGuide` to translate or
 * extend it.
 */
export const defaultDnsProviders: readonly DnsProviderInstructions[] = [
  {
    id: 'cloudflare',
    label: 'Cloudflare',
    steps: (r) => [
      <>
        Open your domain in the Cloudflare dashboard, then {strong('DNS → Records → Add record')}.
      </>,
      <>
        {field('Type', r.type)}, {field('Name', r.name)}, {field('Target', r.value)}.
      </>,
      <>
        Set {strong('Proxy status')} to {strong('DNS only')} (grey cloud). The proxy stops the HTTPS
        certificate from being issued.
      </>,
      <>
        Save. If a record named {code(r.name)} already exists (A, AAAA or CNAME), edit or delete it
        first.
      </>,
    ],
  },
  {
    id: 'godaddy',
    label: 'GoDaddy',
    steps: (r) => [
      <>
        My Products → your domain → {strong('DNS')} → {strong('Add New Record')}.
      </>,
      <>
        {field('Type', r.type)}, {field('Name', r.name)}, {field('Value', r.value)}, TTL 1 hour or
        lower.
      </>,
      <>Save. GoDaddy adds the domain to the name for you, so enter only {code(r.name)}.</>,
    ],
  },
  {
    id: 'namecheap',
    label: 'Namecheap',
    steps: (r) => [
      <>
        Domain List → Manage → {strong('Advanced DNS')} → {strong('Add New Record')}.
      </>,
      <>
        {field('Type', `${r.type} Record`)}, {field('Host', r.name)}, {field('Value', r.value)}, TTL
        Automatic.
      </>,
      <>Save with the tick. Remove any other record with the host {code(r.name)}.</>,
    ],
  },
  {
    id: 'squarespace',
    label: 'Squarespace',
    steps: (r) => [
      <>
        Domains → your domain → {strong('DNS')} → {strong('Custom records')} → Add record.
      </>,
      <>
        {field('Type', r.type)}, {field('Host', r.name)}, {field('Data', r.value)}.
      </>,
      <>Save.</>,
    ],
  },
  {
    id: 'route53',
    label: 'AWS Route 53',
    steps: (r) => [
      <>Route 53 → Hosted zones → your domain → {strong('Create record')}.</>,
      <>
        {field('Record name', r.name)}, {field('Record type', r.type)}, {field('Value', r.value)},
        TTL 300.
      </>,
      <>Leave the routing policy as Simple and create the record.</>,
    ],
  },
  {
    id: 'other',
    label: 'Other',
    steps: (r) => [
      <>In your DNS provider, add a new {r.type} record.</>,
      <>
        Name or host: {code(r.name)} (some providers want the full {code(r.fullName)}; if so, use
        that). Value or target: {code(r.value)}.
      </>,
      <>
        Delete any other A, AAAA or CNAME record with the same name, and turn off any proxy or
        forwarding on it.
      </>,
    ],
  },
];

export interface DnsProviderGuideProps extends HTMLAttributes<HTMLDivElement> {
  /** The record to explain, usually the first one that is not found yet. */
  record: DnsRecord;
  /** The DNS zone, so the name can be given relative to it. */
  zone?: string | undefined;
  /** Providers in the picker. Default `defaultDnsProviders`. */
  providers?: readonly DnsProviderInstructions[] | undefined;
  /** Controlled provider id. */
  provider?: string | undefined;
  /** Initial provider id when uncontrolled. Default: the first provider. */
  defaultProvider?: string | undefined;
  /** Called when the user picks another provider. */
  onProviderChange?: ((provider: string) => void) | undefined;
  /** Label of the provider picker. Default "Where is your DNS managed?". */
  pickerLabel?: string | undefined;
}

/**
 * Step-by-step instructions for adding a DNS record at the user's DNS
 * provider, picked from a list. Pairs with DnsRecordTable.
 */
export const DnsProviderGuide = forwardRef<HTMLDivElement, DnsProviderGuideProps>(
  function DnsProviderGuide(
    {
      record,
      zone,
      providers = defaultDnsProviders,
      provider,
      defaultProvider,
      onProviderChange,
      pickerLabel = 'Where is your DNS managed?',
      className,
      ...rest
    },
    ref,
  ) {
    const [own, setOwn] = useState(defaultProvider ?? providers[0]?.id ?? '');
    const current = provider ?? own;
    const chosen = providers.find((p) => p.id === current) ?? providers[0];
    const stepsId = useId();
    const guideRecord: DnsGuideRecord = {
      type: record.type,
      name: relativeDnsName(record.name, zone),
      fullName: record.name,
      value: record.value,
    };
    return (
      <div ref={ref} {...rest} className={cx('mn-dns-provider-guide', className)}>
        <Field>
          <FieldLabel>{pickerLabel}</FieldLabel>
          <NativeSelect
            value={chosen?.id ?? ''}
            aria-controls={stepsId}
            options={providers.map((p) => ({ value: p.id, label: p.label }))}
            onChange={(e) => {
              if (provider === undefined) setOwn(e.target.value);
              onProviderChange?.(e.target.value);
            }}
          />
        </Field>
        <ol id={stepsId} className="mn-dns-provider-guide__steps" aria-live="polite">
          {chosen?.steps(guideRecord).map((s, i) => (
            <li key={`${chosen.id}-${i}`}>{s}</li>
          ))}
        </ol>
      </div>
    );
  },
);
