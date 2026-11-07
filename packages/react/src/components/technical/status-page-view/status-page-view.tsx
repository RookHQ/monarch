'use client';

import {
  forwardRef,
  useId,
  useState,
  type FormEvent,
  type HTMLAttributes,
  type ReactNode,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { DotGlyph, WrenchGlyph } from '../../../internal/icons.js';
import { Button } from '../../actions/button/index.js';
import { Field, FieldDescription, FieldError, FieldLabel } from '../../forms/form-field/index.js';
import { Input } from '../../forms/text-field/index.js';
import {
  StatusBadge,
  builtInStatuses,
  type StatusRegistry,
} from '../../data-display/status-badge/index.js';
import { RelativeTime } from '../../data-display/relative-time/index.js';
import {
  IncidentTimeline,
  formatIncidentTime,
  type IncidentDateInput,
  type IncidentStageDefinition,
  type IncidentUpdate,
} from '../incident-timeline/index.js';

/** Health of one service, or of the whole page. */
export type ServiceState =
  'operational' | 'degraded' | 'partial' | 'down' | 'maintenance' | 'paused' | 'unknown';

/** One day of a service's history. */
export interface StatusPageDay {
  /** The calendar day, `YYYY-MM-DD`. */
  date: string;
  state: ServiceState;
  /** Share of successful checks that day, 0–100. `null` when nothing was checked. */
  uptimePercent?: number | null;
  /** Number of checks that day. 0 means no data. */
  checks?: number;
  /** Average response time that day, in milliseconds. */
  avgMs?: number | null;
  /** 95th percentile response time that day, in milliseconds. */
  p95Ms?: number | null;
}

export interface StatusPageService {
  id: string;
  name: string;
  /** One line under the name. */
  description?: string | null;
  /** Services with the same group are listed together under that heading. */
  group?: string | null;
  state: ServiceState;
  /** Uptime over the history window, 0–100. `null` while there is no data. */
  uptimePercent?: number | null;
  /** Latest response time in milliseconds, shown with `showResponseTime`. */
  responseTimeMs?: number | null;
  /** Daily history, oldest first. */
  history?: readonly StatusPageDay[];
}

export type StatusPageIncidentStatus = 'open' | 'acknowledged' | 'resolved';
export type StatusPageIncidentSeverity = 'info' | 'warning' | 'critical';

export interface StatusPageIncident {
  id: string;
  /** One-line summary of what is wrong. */
  title: string;
  /** Affected service name. */
  component?: string | null;
  status: StatusPageIncidentStatus;
  severity: StatusPageIncidentSeverity;
  openedAt: IncidentDateInput;
  resolvedAt?: IncidentDateInput | null;
  /** How long it lasted, in seconds. Shown for resolved incidents. */
  durationSeconds?: number | null;
  /** Posted updates, in the order to show them (oldest first reads as a story). */
  updates?: readonly IncidentUpdate[];
}

export interface StatusPageMaintenance {
  id: string;
  title: string;
  description?: string | null;
  component?: string | null;
  status: 'scheduled' | 'in-progress';
  startsAt: IncidentDateInput;
  endsAt: IncidentDateInput;
}

/** The result of `onSubscribe`. */
export type StatusPageSubscribeResult = 'sent' | 'rate-limited' | 'error';

export interface StatusPageViewMessages {
  /** Overall banner text per state. */
  overall: Record<ServiceState, string>;
  /** Short state labels for services and the legend. */
  states: Record<ServiceState, string>;
  support: string;
  uptimeStat: (days: number) => string;
  servicesStat: string;
  activeIncidentsStat: string;
  servicesHeading: string;
  noServices: string;
  maintenanceHeading: string;
  maintenanceScheduled: string;
  maintenanceInProgress: string;
  activeIncidentsHeading: string;
  pastIncidentsHeading: string;
  noPastIncidents: string;
  incidentStatus: Record<StatusPageIncidentStatus, string>;
  severity: Record<StatusPageIncidentSeverity, string>;
  lasted: (duration: string) => string;
  daysAgo: (days: number) => string;
  today: string;
  uptime: (percent: string) => string;
  awaitingData: string;
  legendLabel: string;
  historyLabel: (service: string, days: number, summary: string) => string;
  dayUptime: string;
  dayChecks: string;
  dayAvg: string;
  dayP95: string;
  dayNoChecks: string;
  updated: string;
  subscribeLabel: string;
  subscribeDescription: string;
  subscribePlaceholder: string;
  subscribeButton: string;
  subscribeSending: string;
  subscribeSent: string;
  subscribeRateLimited: string;
  subscribeError: string;
}

const DEFAULT_MESSAGES: StatusPageViewMessages = {
  overall: {
    operational: 'All systems operational',
    degraded: 'Degraded performance',
    partial: 'Partial system outage',
    down: 'Service disruption',
    maintenance: 'Under maintenance',
    paused: 'Monitoring paused',
    unknown: 'Awaiting first checks',
  },
  states: {
    operational: 'Operational',
    degraded: 'Degraded',
    partial: 'Partial outage',
    down: 'Down',
    maintenance: 'Maintenance',
    paused: 'Paused',
    unknown: 'No data',
  },
  support: 'Support',
  uptimeStat: (days) => `Uptime · ${days}d`,
  servicesStat: 'Services',
  activeIncidentsStat: 'Active incidents',
  servicesHeading: 'Services',
  noServices: 'No services on this page yet.',
  maintenanceHeading: 'Scheduled maintenance',
  maintenanceScheduled: 'Scheduled',
  maintenanceInProgress: 'In progress',
  activeIncidentsHeading: 'Active incidents',
  pastIncidentsHeading: 'Past incidents',
  noPastIncidents: 'No incidents reported.',
  incidentStatus: { open: 'Ongoing', acknowledged: 'Investigating', resolved: 'Resolved' },
  severity: { info: 'Minor', warning: 'Major', critical: 'Critical' },
  lasted: (d) => `lasted ${d}`,
  daysAgo: (n) => `${n} days ago`,
  today: 'Today',
  uptime: (p) => `${p}% uptime`,
  awaitingData: 'Awaiting data',
  legendLabel: 'Legend',
  historyLabel: (service, days, summary) => `${service}, last ${days} days: ${summary}`,
  dayUptime: 'Uptime',
  dayChecks: 'Checks',
  dayAvg: 'Avg response',
  dayP95: 'p95 response',
  dayNoChecks: 'No checks recorded',
  updated: 'Updated',
  subscribeLabel: 'Subscribe to updates',
  subscribeDescription: 'Get an email when an incident is opened, updated or resolved.',
  subscribePlaceholder: 'you@example.com',
  subscribeButton: 'Subscribe',
  subscribeSending: 'Sending…',
  subscribeSent:
    'Check your inbox. If that address can be subscribed, a confirmation link is on its way.',
  subscribeRateLimited: 'Too many attempts right now. Please try again later.',
  subscribeError: 'Could not reach the subscription service. Please try again in a moment.',
};

const SEVERITY_ORDER: ServiceState[] = [
  'down',
  'partial',
  'degraded',
  'maintenance',
  'operational',
  'paused',
  'unknown',
];

/**
 * The worst state among `services`, for the overall banner: down, then
 * partial, degraded, maintenance, operational. Paused and unknown services
 * only count when nothing else is known.
 */
export function deriveOverallState(services: readonly { state: ServiceState }[]): ServiceState {
  for (const state of SEVERITY_ORDER) {
    if (services.some((s) => s.state === state)) return state;
  }
  return 'unknown';
}

/** "45s", "12m", "2h 5m". */
export function formatIncidentDuration(seconds: number): string {
  const s = Math.max(0, Math.round(seconds));
  if (s < 60) return `${s}s`;
  if (s < 3600) return `${Math.round(s / 60)}m`;
  const h = Math.floor(s / 3600);
  const m = Math.round((s % 3600) / 60);
  return m ? `${h}h ${m}m` : `${h}h`;
}

type Messages = StatusPageViewMessages;
type HeadingTag = 'h1' | 'h2' | 'h3' | 'h4' | 'h5' | 'h6';
const heading = (level: number): HeadingTag => `h${Math.min(6, Math.max(1, level))}` as HeadingTag;

function stateRegistry(m: Messages): StatusRegistry {
  return {
    operational: { ...builtInStatuses.operational, label: m.states.operational },
    degraded: { ...builtInStatuses.degraded, label: m.states.degraded },
    partial: { ...builtInStatuses.partial, label: m.states.partial },
    down: { ...builtInStatuses.down, label: m.states.down },
    maintenance: { ...builtInStatuses.maintenance, label: m.states.maintenance },
    paused: { label: m.states.paused, tone: 'neutral', icon: <DotGlyph /> },
    unknown: { ...builtInStatuses.unknown, label: m.states.unknown },
  };
}

function iso(value: IncidentDateInput): string | undefined {
  const d = value instanceof Date ? value : new Date(value);
  return Number.isNaN(d.getTime()) ? undefined : d.toISOString();
}

function formatDay(day: string, locale: string | undefined): string {
  const d = new Date(`${day}T00:00:00Z`);
  if (Number.isNaN(d.getTime())) return day;
  return new Intl.DateTimeFormat(locale, {
    month: 'short',
    day: 'numeric',
    timeZone: 'UTC',
  }).format(d);
}

function percent(value: number, locale: string | undefined): string {
  return new Intl.NumberFormat(locale, {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(value);
}

/** The text of one day, used for its tooltip and for the history summary. */
function dayText(day: StatusPageDay, m: Messages, locale: string | undefined, showMs: boolean) {
  const parts = [formatDay(day.date, locale), m.states[day.state]];
  if (!day.checks) {
    parts.push(m.dayNoChecks);
  } else {
    if (day.uptimePercent != null)
      parts.push(`${m.dayUptime} ${percent(day.uptimePercent, locale)}%`);
    parts.push(`${m.dayChecks} ${new Intl.NumberFormat(locale).format(day.checks)}`);
    if (showMs && day.avgMs != null) parts.push(`${m.dayAvg} ${day.avgMs} ms`);
    if (showMs && day.p95Ms != null) parts.push(`${m.dayP95} ${day.p95Ms} ms`);
  }
  return parts.join(' · ');
}

function historySummary(days: readonly StatusPageDay[], m: Messages): string {
  const counts = new Map<ServiceState, number>();
  for (const d of days) counts.set(d.state, (counts.get(d.state) ?? 0) + 1);
  return SEVERITY_ORDER.filter((s) => counts.has(s))
    .map((s) => `${counts.get(s)} ${m.states[s].toLowerCase()}`)
    .join(', ');
}

function DayStrip({
  service,
  days,
  m,
  locale,
  showMs,
}: {
  service: StatusPageService;
  days: readonly StatusPageDay[];
  m: Messages;
  locale: string | undefined;
  showMs: boolean;
}) {
  return (
    <div
      className="mn-status-page-view__strip"
      role="img"
      aria-label={m.historyLabel(service.name, days.length, historySummary(days, m))}
    >
      {days.map((day) => (
        <span
          key={day.date}
          className="mn-status-page-view__day"
          data-state={day.state}
          title={dayText(day, m, locale, showMs)}
        />
      ))}
    </div>
  );
}

export interface StatusPageViewProps extends Omit<HTMLAttributes<HTMLDivElement>, 'title'> {
  /** Page name, the main heading. */
  title: ReactNode;
  /** One line under the title. */
  description?: ReactNode;
  /** Logo before the title, for example an `<img>` or an Avatar. Decorative unless it has its own name. */
  logo?: ReactNode;
  /** Link to the product's support or contact page. */
  supportHref?: string;
  /** Overall state for the banner. Default: the worst service state (`deriveOverallState`). */
  overallState?: ServiceState;
  /** Uptime across all services over the history window, 0–100. */
  overallUptimePercent?: number | null;
  /** The services, in display order. Grouped by `group`, in order of first appearance. */
  services: readonly StatusPageService[];
  /** Incidents still open, shown above the services. */
  activeIncidents?: readonly StatusPageIncident[];
  /** Resolved incidents from the recent past, shown below the services. */
  pastIncidents?: readonly StatusPageIncident[];
  /** Maintenance in progress or announced. */
  maintenance?: readonly StatusPageMaintenance[];
  /** Days of history each service shows. Default: the longest `history`, or 30. */
  historyDays?: number;
  /** `list` (default): one row per service with its day bars. `grid`: compact tiles with the last 14 days, for pages with many services. */
  layout?: 'list' | 'grid';
  /** Show each service's uptime percentage. Default `true`. */
  showUptimePercent?: boolean;
  /** Show response times in rows and day details. Default `false`. */
  showResponseTime?: boolean;
  /** Show incident sections and the active-incidents count. Default `true`. */
  showIncidents?: boolean;
  /** Show the summary tiles (uptime, services, active incidents). Default `true`. */
  showStats?: boolean;
  /**
   * Replaces the default day strip for a service, for example with an
   * `UptimeBar`. Receives the service and the days to show.
   */
  renderHistory?:
    ((service: StatusPageService, days: readonly StatusPageDay[]) => ReactNode) | undefined;
  /** A notice above everything else, for example a "Subscription confirmed" Alert. */
  notice?: ReactNode;
  /**
   * Adds the email subscribe form. Called with the trimmed address; may
   * return a promise. Resolve with `'rate-limited'` or `'error'` (a
   * `StatusPageSubscribeResult`) to show that message; anything else counts
   * as sent, and a rejection shows the error message. The form shows one
   * neutral confirmation whether or not the address was already subscribed.
   */
  onSubscribe?: ((email: string) => unknown) | undefined;
  /** When the data was generated. Shown as "Updated 2 minutes ago". */
  updatedAt?: IncidentDateInput;
  /** Reference time for `updatedAt`, for byte-identical server rendering. */
  now?: IncidentDateInput;
  /** Content at the end of the footer, for example a "Powered by" link. */
  footer?: ReactNode;
  /** Stage overrides passed to every IncidentTimeline. */
  stages?: Readonly<Record<string, IncidentStageDefinition>>;
  /** Level of the page title. Section headings are one level below. Default `1`. */
  headingLevel?: 1 | 2 | 3 | 4;
  /** BCP 47 locale for dates and numbers. Default: the runtime locale. */
  locale?: string;
  /** IANA time zone for incident and maintenance times. Default `UTC`. */
  timeZone?: string;
  /** Text overrides for translation. Nested records are merged key by key. */
  messages?: Partial<
    Omit<StatusPageViewMessages, 'overall' | 'states' | 'incidentStatus' | 'severity'>
  > & {
    overall?: Partial<StatusPageViewMessages['overall']>;
    states?: Partial<StatusPageViewMessages['states']>;
    incidentStatus?: Partial<StatusPageViewMessages['incidentStatus']>;
    severity?: Partial<StatusPageViewMessages['severity']>;
  };
}

function IncidentCard({
  incident,
  m,
  level,
  locale,
  timeZone,
  stages,
}: {
  incident: StatusPageIncident;
  m: Messages;
  level: number;
  locale: string | undefined;
  timeZone: string | undefined;
  stages: StatusPageViewProps['stages'];
}) {
  const H = heading(level);
  const resolved = incident.status === 'resolved';
  const titleId = useId();
  return (
    <article
      className="mn-status-page-view__incident"
      data-severity={incident.severity}
      data-status={incident.status}
      aria-labelledby={titleId}
    >
      <div className="mn-status-page-view__incident-head">
        <div className="mn-status-page-view__incident-text">
          <H id={titleId} className="mn-status-page-view__incident-title">
            {incident.title}
          </H>
          <p className="mn-status-page-view__meta">
            <span className="mn-status-page-view__sr">{m.severity[incident.severity]} · </span>
            {incident.component ? <span>{incident.component} · </span> : null}
            <time dateTime={iso(incident.openedAt)}>
              {formatIncidentTime(incident.openedAt, { locale, timeZone })}
            </time>
            {resolved && incident.durationSeconds != null ? (
              <span> · {m.lasted(formatIncidentDuration(incident.durationSeconds))}</span>
            ) : null}
          </p>
        </div>
        <StatusBadge
          size="sm"
          status={resolved ? 'success' : incident.status === 'acknowledged' ? 'warning' : 'danger'}
          label={m.incidentStatus[incident.status]}
        />
      </div>
      {incident.updates && incident.updates.length > 0 ? (
        <IncidentTimeline
          className="mn-status-page-view__updates"
          updates={incident.updates}
          density="compact"
          stages={stages}
          locale={locale}
          timeZone={timeZone}
        />
      ) : null}
    </article>
  );
}

function SubscribeForm({
  onSubscribe,
  m,
}: {
  onSubscribe: NonNullable<StatusPageViewProps['onSubscribe']>;
  m: Messages;
}) {
  const [email, setEmail] = useState('');
  const [state, setState] = useState<'idle' | 'sending' | StatusPageSubscribeResult>('idle');

  async function submit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const value = email.trim();
    if (!value || state === 'sending') return;
    setState('sending');
    try {
      const result = await onSubscribe(value);
      setState(result === 'rate-limited' || result === 'error' ? result : 'sent');
    } catch {
      setState('error');
    }
  }

  const failed = state === 'rate-limited' || state === 'error';
  return (
    <div className="mn-status-page-view__subscribe">
      {state === 'sent' ? (
        <p className="mn-status-page-view__subscribe-sent" role="status">
          {m.subscribeSent}
        </p>
      ) : (
        <form className="mn-status-page-view__subscribe-form" onSubmit={submit}>
          <Field invalid={failed} required>
            <FieldLabel>{m.subscribeLabel}</FieldLabel>
            <FieldDescription>{m.subscribeDescription}</FieldDescription>
            <div className="mn-status-page-view__subscribe-row">
              <Input
                type="email"
                autoComplete="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder={m.subscribePlaceholder}
              />
              <Button type="submit" loading={state === 'sending'}>
                {state === 'sending' ? m.subscribeSending : m.subscribeButton}
              </Button>
            </div>
            {failed ? (
              <FieldError>
                {state === 'rate-limited' ? m.subscribeRateLimited : m.subscribeError}
              </FieldError>
            ) : null}
          </Field>
        </form>
      )}
    </div>
  );
}

/**
 * A public status page: overall health, summary figures, maintenance notices,
 * active incidents with their updates, every service with its daily history,
 * past incidents and an email subscribe form. Data and callbacks in; fetching,
 * refreshing and the page route stay in the product.
 */
export const StatusPageView = forwardRef<HTMLDivElement, StatusPageViewProps>(
  function StatusPageView(
    {
      title,
      description,
      logo,
      supportHref,
      overallState,
      overallUptimePercent,
      services,
      activeIncidents = [],
      pastIncidents = [],
      maintenance = [],
      historyDays,
      layout = 'list',
      showUptimePercent = true,
      showResponseTime = false,
      showIncidents = true,
      showStats = true,
      renderHistory,
      notice,
      onSubscribe,
      updatedAt,
      now,
      footer,
      stages,
      headingLevel = 1,
      locale,
      timeZone,
      messages: messagesProp,
      className,
      ...rest
    },
    ref,
  ) {
    const m: Messages = {
      ...DEFAULT_MESSAGES,
      ...messagesProp,
      overall: { ...DEFAULT_MESSAGES.overall, ...messagesProp?.overall },
      states: { ...DEFAULT_MESSAGES.states, ...messagesProp?.states },
      incidentStatus: { ...DEFAULT_MESSAGES.incidentStatus, ...messagesProp?.incidentStatus },
      severity: { ...DEFAULT_MESSAGES.severity, ...messagesProp?.severity },
    };
    const id = useId();
    const registry = stateRegistry(m);
    const overall = overallState ?? deriveOverallState(services);
    const overallDef = registry[overall] ?? builtInStatuses.unknown;
    const shownDays =
      historyDays ?? (Math.max(0, ...services.map((s) => s.history?.length ?? 0)) || 30);
    const H1 = heading(headingLevel);
    const H2 = heading(headingLevel + 1);
    const H3 = heading(headingLevel + 2);

    // Group services by `group`, keeping the order each group first appears.
    const groups: [string, StatusPageService[]][] = [];
    for (const s of services) {
      const key = s.group ?? '';
      const found = groups.find(([g]) => g === key);
      if (found) found[1].push(s);
      else groups.push([key, [s]]);
    }

    const tiles = [
      {
        key: 'uptime',
        label: m.uptimeStat(shownDays),
        value: overallUptimePercent != null ? `${percent(overallUptimePercent, locale)}%` : '—',
        danger: false,
      },
      {
        key: 'services',
        label: m.servicesStat,
        value: new Intl.NumberFormat(locale).format(services.length),
        danger: false,
      },
      // When incidents are hidden, a "0 active incidents" tile could contradict a red banner.
      ...(showIncidents
        ? [
            {
              key: 'incidents',
              label: m.activeIncidentsStat,
              value: new Intl.NumberFormat(locale).format(activeIncidents.length),
              danger: activeIncidents.length > 0,
            },
          ]
        : []),
    ];

    const incidentProps = { m, level: headingLevel + 2, locale, timeZone, stages };
    const legendStates: ServiceState[] = [
      'operational',
      'degraded',
      'down',
      'maintenance',
      'unknown',
    ];

    const renderStrip = (service: StatusPageService, max: number) => {
      const history = (service.history ?? []).slice(-max);
      if (renderHistory) return renderHistory(service, history);
      if (history.length === 0) return null;
      return (
        <DayStrip
          service={service}
          days={history}
          m={m}
          locale={locale}
          showMs={showResponseTime}
        />
      );
    };

    return (
      <div
        ref={ref}
        {...rest}
        className={cx('mn-status-page-view', className)}
        data-layout={layout}
        data-state={overall}
      >
        {notice ? <div className="mn-status-page-view__notice">{notice}</div> : null}

        <header className="mn-status-page-view__header">
          <div className="mn-status-page-view__brand">
            {logo ? <span className="mn-status-page-view__logo">{logo}</span> : null}
            <div className="mn-status-page-view__brand-text">
              <H1 className="mn-status-page-view__title">{title}</H1>
              {description ? (
                <p className="mn-status-page-view__description">{description}</p>
              ) : null}
            </div>
          </div>
          {supportHref ? (
            <a
              className="mn-status-page-view__support"
              href={supportHref}
              target="_blank"
              rel="noopener noreferrer"
            >
              {m.support}
            </a>
          ) : null}
        </header>

        <section
          className="mn-status-page-view__overall"
          data-tone={overallDef.tone}
          aria-live="polite"
        >
          <span className="mn-status-page-view__overall-icon" aria-hidden>
            {overall === 'maintenance' ? <WrenchGlyph /> : overallDef.icon}
          </span>
          <H2 className="mn-status-page-view__overall-text">{m.overall[overall]}</H2>
        </section>

        {showStats ? (
          <dl className="mn-status-page-view__stats">
            {tiles.map((t) => (
              <div
                key={t.key}
                className="mn-status-page-view__stat"
                data-danger={t.danger || undefined}
              >
                <dt className="mn-status-page-view__stat-label">{t.label}</dt>
                <dd className="mn-status-page-view__stat-value">{t.value}</dd>
              </div>
            ))}
          </dl>
        ) : null}

        {maintenance.length > 0 ? (
          <section className="mn-status-page-view__section" aria-labelledby={`${id}-maint`}>
            <H2 id={`${id}-maint`} className="mn-status-page-view__section-title">
              {m.maintenanceHeading}
            </H2>
            <ul className="mn-status-page-view__cards">
              {maintenance.map((w) => (
                <li key={w.id} className="mn-status-page-view__maintenance" data-status={w.status}>
                  <div className="mn-status-page-view__incident-head">
                    <div className="mn-status-page-view__incident-text">
                      <H3 className="mn-status-page-view__incident-title">{w.title}</H3>
                      <p className="mn-status-page-view__meta">
                        {w.component ? <span>{w.component} · </span> : null}
                        <time dateTime={iso(w.startsAt)}>
                          {formatIncidentTime(w.startsAt, { locale, timeZone })}
                        </time>
                        {' – '}
                        <time dateTime={iso(w.endsAt)}>
                          {formatIncidentTime(w.endsAt, { locale, timeZone })}
                        </time>
                      </p>
                    </div>
                    <StatusBadge
                      size="sm"
                      status="maintenance"
                      label={
                        w.status === 'in-progress'
                          ? m.maintenanceInProgress
                          : m.maintenanceScheduled
                      }
                    />
                  </div>
                  {w.description ? (
                    <p className="mn-status-page-view__maintenance-text">{w.description}</p>
                  ) : null}
                </li>
              ))}
            </ul>
          </section>
        ) : null}

        {showIncidents && activeIncidents.length > 0 ? (
          <section className="mn-status-page-view__section" aria-labelledby={`${id}-active`}>
            <H2 id={`${id}-active`} className="mn-status-page-view__section-title">
              {m.activeIncidentsHeading}
            </H2>
            <div className="mn-status-page-view__cards">
              {activeIncidents.map((i) => (
                <IncidentCard key={i.id} incident={i} {...incidentProps} />
              ))}
            </div>
          </section>
        ) : null}

        <section className="mn-status-page-view__section" aria-labelledby={`${id}-services`}>
          <H2 id={`${id}-services`} className="mn-status-page-view__section-title">
            {m.servicesHeading}
          </H2>
          {services.length === 0 ? (
            <p className="mn-status-page-view__empty">{m.noServices}</p>
          ) : (
            groups.map(([group, items]) => (
              <div key={group || '__ungrouped'} className="mn-status-page-view__group">
                {group ? <H3 className="mn-status-page-view__group-title">{group}</H3> : null}
                <ul className="mn-status-page-view__services">
                  {items.map((s) => (
                    <li key={s.id} className="mn-status-page-view__service" data-state={s.state}>
                      <div className="mn-status-page-view__service-head">
                        <div className="mn-status-page-view__service-text">
                          <p className="mn-status-page-view__service-name">
                            {s.name}
                            {showResponseTime && s.responseTimeMs != null && s.state !== 'down' ? (
                              <span className="mn-status-page-view__ms">{s.responseTimeMs} ms</span>
                            ) : null}
                          </p>
                          {s.description && layout === 'list' ? (
                            <p className="mn-status-page-view__service-description">
                              {s.description}
                            </p>
                          ) : null}
                        </div>
                        <StatusBadge size="sm" status={s.state} statuses={registry} />
                      </div>
                      {renderStrip(s, layout === 'grid' ? 14 : shownDays)}
                      {layout === 'list' ? (
                        <div className="mn-status-page-view__axis">
                          <span>{m.daysAgo(shownDays)}</span>
                          {showUptimePercent ? (
                            <span>
                              {s.uptimePercent != null
                                ? m.uptime(percent(s.uptimePercent, locale))
                                : m.awaitingData}
                            </span>
                          ) : null}
                          <span>{m.today}</span>
                        </div>
                      ) : showUptimePercent ? (
                        <p className="mn-status-page-view__axis">
                          {s.uptimePercent != null
                            ? m.uptime(percent(s.uptimePercent, locale))
                            : m.awaitingData}
                        </p>
                      ) : null}
                    </li>
                  ))}
                </ul>
              </div>
            ))
          )}
          {services.length > 0 && !renderHistory ? (
            <ul className="mn-status-page-view__legend" aria-label={m.legendLabel}>
              {legendStates.map((s) => (
                <li key={s} className="mn-status-page-view__legend-item" data-state={s}>
                  <span className="mn-status-page-view__legend-swatch" aria-hidden />
                  {m.states[s]}
                </li>
              ))}
            </ul>
          ) : null}
        </section>

        {showIncidents ? (
          <section className="mn-status-page-view__section" aria-labelledby={`${id}-past`}>
            <H2 id={`${id}-past`} className="mn-status-page-view__section-title">
              {m.pastIncidentsHeading}
            </H2>
            {pastIncidents.length === 0 ? (
              <p className="mn-status-page-view__empty">{m.noPastIncidents}</p>
            ) : (
              <div className="mn-status-page-view__cards">
                {pastIncidents.map((i) => (
                  <IncidentCard key={i.id} incident={i} {...incidentProps} />
                ))}
              </div>
            )}
          </section>
        ) : null}

        {onSubscribe ? <SubscribeForm onSubscribe={onSubscribe} m={m} /> : null}

        {updatedAt !== undefined || footer ? (
          <footer className="mn-status-page-view__footer">
            {updatedAt !== undefined ? (
              <span>
                {m.updated}{' '}
                <RelativeTime
                  date={updatedAt}
                  {...(now !== undefined ? { now } : {})}
                  {...(locale ? { locale } : {})}
                  live
                  tooltip={false}
                />
              </span>
            ) : null}
            {footer ? <span className="mn-status-page-view__footer-end">{footer}</span> : null}
          </footer>
        ) : null}
      </div>
    );
  },
);
