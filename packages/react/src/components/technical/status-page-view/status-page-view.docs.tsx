import {
  StatusPageView,
  type StatusPageDay,
  type StatusPageIncident,
  type StatusPageService,
} from '@rookcom/monarch-react/patterns';
import type { ComponentDocs } from '../../../internal/component-docs.js';
// The scenario of the primary example, shared with iOS and Android (CROSS-PLATFORM.md section 8).
import fixture from './status-page-view.fixture.json';

const frame = { width: '100%', maxWidth: '44rem' } as const;

function history(seed: number, bad: Record<number, StatusPageDay['state']> = {}): StatusPageDay[] {
  return Array.from({ length: 30 }, (_, i) => {
    const state = bad[i] ?? 'operational';
    return {
      date: new Date(Date.UTC(2026, 2, 1 + i)).toISOString().slice(0, 10),
      state,
      uptimePercent: state === 'operational' ? 100 : state === 'down' ? 91.2 : 98.6,
      checks: 1440,
      avgMs: 110 + ((i * seed) % 40),
      p95Ms: 220 + ((i * seed) % 80),
    };
  });
}

type Mark = { day: number; state: StatusPageDay['state'] };
const SEEDS = [3, 5, 7, 2] as const;
const marks = (history: readonly { day: number; state: string }[]) =>
  Object.fromEntries(history.map((m) => [m.day - 1, m.state])) as Record<
    number,
    StatusPageDay['state']
  >;

type FixtureService = {
  name: string;
  description?: string;
  state: StatusPageService['state'];
  uptimePercent: number;
  responseTimeMs?: number;
  history: readonly Mark[];
};
type FixtureUpdate = { stage: string; body: string; time: string; automated: boolean };

const IDS = ['api', 'web', 'auth', 'mail'] as const;
const services: StatusPageService[] = (fixture.data.services as readonly FixtureService[]).map(
  (s, i) => ({
    id: IDS[i] ?? s.name,
    name: s.name,
    ...(i < 3 && { group: fixture.data.group }),
    ...(s.description !== undefined && { description: s.description }),
    state: s.state,
    uptimePercent: s.uptimePercent,
    ...(s.responseTimeMs !== undefined && { responseTimeMs: s.responseTimeMs }),
    history: history(SEEDS[i] ?? 1, marks(s.history)),
  }),
);

const toUpdates = (prefix: string, updates: readonly FixtureUpdate[]) =>
  updates.map((u, i) => ({
    id: `${prefix}${i + 1}`,
    stage: u.stage as NonNullable<StatusPageIncident['updates']>[number]['stage'],
    body: u.body,
    time: u.time,
    ...(u.automated && { automated: true }),
  }));

const activeData = fixture.data.activeIncident;
const active: StatusPageIncident[] = [
  {
    id: 'i1',
    title: activeData.title,
    component: activeData.component,
    status: 'acknowledged',
    severity: 'warning',
    openedAt: activeData.openedAt,
    updates: toUpdates('u', activeData.updates),
  },
];

const pastData = fixture.data.pastIncident;
const past: StatusPageIncident[] = [
  {
    id: 'i0',
    title: pastData.title,
    component: pastData.component,
    status: 'resolved',
    severity: 'critical',
    openedAt: pastData.openedAt,
    resolvedAt: '2026-03-05T14:49:00Z',
    durationSeconds: pastData.durationSeconds,
    updates: toUpdates('p', pastData.updates),
  },
];

const now = fixture.data.now;

export default {
  id: 'status-page-view',
  title: 'StatusPageView',
  summary:
    'A public status page: overall health, services with daily history, incidents, maintenance and subscribe.',
  sections: {
    purpose: {
      summary:
        'Renders the whole public status page from data: an overall banner, summary figures, maintenance notices, active incidents with their updates, each service with its daily history, past incidents and an email subscribe form. Fetching, refreshing, routes and branding stay in the product. Needed by: Pulse (public status pages at /status/[slug] and custom domains, and the dashboard preview).',
      useWhen: [
        'A product publishes the health of its services to customers.',
        'An admin screen previews a status page before it is published.',
      ],
      avoidWhen: [
        'A single service’s history inside a dashboard: use UptimeBar.',
        'An internal on-call view with actions: compose StatusBadge, IncidentTimeline and DataTable.',
      ],
    },
    designPrinciples: {
      items: [
        'Colour is never the only signal: every state has an icon and text, every day has its numbers, and a legend names the colours once.',
        'Incidents lead with what people want to know: is it planned (maintenance first), is anyone on it (updates), is it over (status and duration).',
        'Honest by omission: with `showIncidents={false}` the incident tile is left out, so a "0 active incidents" figure cannot contradict a red banner.',
        'The subscribe form gives one neutral answer whether or not an address is already subscribed, and separates "too many attempts" from a network failure.',
        'Pure data in: no fetching, timers or routing inside, so the same component serves the public page and an admin preview.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Also exports `deriveOverallState(services)` (the worst state, used when `overallState` is not given) and `formatIncidentDuration(seconds)`.',
        'Forwards its ref to the root `<div>`.',
        'Uses IncidentTimeline for incident updates, StatusBadge for states, RelativeTime for "Updated" and Field, Input and Button for the subscribe form.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        '`messages` merges key by key, including the nested `overall`, `states`, `incidentStatus` and `severity` records. Several messages are functions (`uptimeStat`, `daysAgo`, `uptime`, `lasted`, `historyLabel`) so translations can reorder words.',
        'Each service’s history is an `UptimeBar` (day tooltips with uptime, checks and, with `showResponseTime`, response times), with one `UptimeLegend` for the page. `renderHistory(service, days)` replaces it with your own rendering; the legend is then left to the product.',
        'Migrating from the app copy (Pulse `StatusPageView`, `payload` prop): `page.name/description/supportUrl/logoUrl` → `title`, `description`, `supportHref`, `logo` (pass an element); `page.showUptimePct/showResponseTime/showIncidents` → `showUptimePercent`, `showResponseTime`, `showIncidents`; `overallState` (`nodata` → `unknown`) and `overallUptimePct30d` → `overallState`, `overallUptimePercent`; `components[]` → `services[]` with `uptimePct30d` → `uptimePercent` and `bars[]` (`day`, `uptimePct`, `total`) → `history[]` (`date`, `uptimePercent`, `checks`); `incidents.open/recent` → `activeIncidents`/`pastIncidents` with `summary` → `title`, lower-case `status` and `severity`, and `updates[].createdAt` → `time`; `maintenance.active/upcoming` → `maintenance` with `scheduledFor/scheduledEnd` → `startsAt/endsAt`; `generatedAt` → `updatedAt`. The ten `page.theme` layouts become `layout="list" | "grid"` plus product CSS on the `mn-status-page-view__*` parts; the `?subscription=` banner → `notice`; the subscribe `fetch` → `onSubscribe` returning `"sent" | "rate-limited" | "error"`; `autoRefresh` (router.refresh) and the "Powered by" link stay in the product (`footer`).',
      ],
    },
    variants: {
      items: [
        {
          prop: 'layout',
          values: ['list', 'grid'],
          description:
            '`list`: a row per service with 30 day bars and an axis. `grid`: compact tiles with the last 14 days, for many services.',
        },
        {
          prop: 'showUptimePercent / showResponseTime / showIncidents / showStats',
          values: ['true', 'false'],
          description: 'Turn page sections and figures on or off, as a page owner configures them.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Overall state',
          attribute: 'data-state (root), data-tone (banner)',
          description:
            'operational, degraded, partial, down, maintenance, paused or unknown; the banner text and tone follow it.',
        },
        {
          name: 'Service and day state',
          attribute: 'data-state on each service and day',
          description: 'Drives the badge and the day colour; the text and tooltip say it too.',
        },
        {
          name: 'Incident',
          attribute: 'data-severity, data-status on each card',
          description: 'Severity colours the card edge (and is read out); status shows as a badge.',
        },
        {
          name: 'Subscribe',
          attribute: 'aria-busy on the button; aria-invalid on the input',
          description:
            'Idle, sending (button spinner), sent (status message replaces the form), rate-limited or error (field error).',
        },
        {
          name: 'Empty',
          description: '"No services on this page yet" and "No incidents reported" messages.',
        },
      ],
    },
    interaction: {
      items: [
        'Hovering a day shows its date, state, uptime, checks and (with `showResponseTime`) response times as a native tooltip.',
        'The subscribe form submits the trimmed address to `onSubscribe`; a thrown error shows the network message.',
        'The overall banner is a polite live region, so a refreshed page announces a change of state.',
        '"Updated … ago" keeps itself current; the product refreshes the data.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Tab',
          action: 'Support link, links in incident updates, the email field, then Subscribe.',
        },
        { keys: 'Enter', action: 'In the email field: subscribe.' },
      ],
      notes: [
        'Each history is one UptimeBar: a single tab stop named with a summary; arrow keys, Home and End move through the days and announce each one.',
      ],
    },
    accessibility: {
      role: 'Headings and regions: page title, overall banner, then named sections',
      name: 'Each section is labelled by its heading; incident cards are articles named by their title; each history bar is a group named with a summary ("API, last 30 days: 1 degraded, 29 operational").',
      items: [
        'Heading levels start at `headingLevel` (default 1) so the page fits inside an app or stands alone.',
        'Severity is read out with each incident, not only shown as a coloured edge.',
        'Times are `<time datetime>` elements in UTC by default, with the zone shown.',
        'The subscribe input is labelled and described by Field; errors are linked with `aria-describedby`; the confirmation is `role="status"`.',
        'The support link has a 44px target on coarse pointers.',
      ],
    },
    responsive: {
      items: [
        'Single column; summary tiles reflow from three across to fewer as space runs out.',
        'Day bars shrink to fit, so 30 days fit at 390px with no horizontal scroll.',
        'The grid layout fills as many 12rem tiles per row as fit.',
        'The subscribe field and button wrap onto two lines when narrow.',
      ],
    },
    theme: {
      items: [
        'Status colours come from the success, warning, danger and info tokens, so light, dark and both more-contrast themes keep their ratios.',
        'Forced colours: borders and icons use system colours; the history bars follow UptimeBar’s forced-colours rules.',
        'A product restyles a layout through the `mn-status-page-view__*` classes with `--mn-*` tokens only.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Degraded, with an active incident',
          stack: true,
          render: () => (
            <div style={frame}>
              <StatusPageView
                title={fixture.data.title}
                description={fixture.data.description}
                supportHref="https://example.com/support"
                services={services}
                activeIncidents={active}
                pastIncidents={past}
                overallUptimePercent={fixture.data.overallUptimePercent}
                showResponseTime
                updatedAt={fixture.data.updatedAt}
                now={now}
                headingLevel={2}
                onSubscribe={async () => 'sent'}
              />
            </div>
          ),
        },
        {
          title: 'Grid layout, all operational, with maintenance',
          stack: true,
          render: () => (
            <div style={frame}>
              <StatusPageView
                title="Example status"
                layout="grid"
                headingLevel={2}
                // Three pages on one docs page: distinct section names keep each region unique.
                messages={{
                  servicesHeading: 'Components',
                  pastIncidentsHeading: 'Incident history',
                }}
                services={services.map((s) => ({ ...s, state: 'operational' as const }))}
                overallUptimePercent={99.98}
                maintenance={[
                  {
                    id: 'm1',
                    title: 'Database upgrade',
                    component: 'API',
                    status: 'scheduled',
                    startsAt: '2026-04-02T01:00:00Z',
                    endsAt: '2026-04-02T02:00:00Z',
                    description: 'Writes may be slower for up to ten minutes.',
                  },
                ]}
              />
            </div>
          ),
        },
        {
          title: 'Incidents hidden, nothing checked yet',
          stack: true,
          render: () => (
            <div style={frame}>
              <StatusPageView
                title="New page"
                headingLevel={2}
                messages={{ servicesHeading: 'Monitored services' }}
                showIncidents={false}
                services={[{ id: 'a', name: 'API', state: 'unknown', uptimePercent: null }]}
              />
            </div>
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Pass `now` and `updatedAt` from the server for identical server and client text.',
        'Return `"rate-limited"` from `onSubscribe` on HTTP 429, so people are not told to wait for nothing.',
        'Show the same confirmation whether or not an address was already subscribed.',
        'Keep incident updates in order, oldest first, so they read as a story.',
      ],
      dont: [
        'Do not fetch or poll inside the page; refresh the data in the product.',
        'Do not show internal notes or responder names on a public page.',
        'Do not hide the legend unless `renderHistory` brings its own.',
      ],
    },
    related: {
      items: [
        { id: 'incident-timeline', reason: 'The update list inside each incident card.' },
        {
          id: 'uptime-bar',
          reason:
            'The default history of every service; use it alone for one service’s history elsewhere in a product.',
        },
        { id: 'status-badge', reason: 'The state badges it uses.' },
        { id: 'relative-time', reason: 'The "Updated … ago" text.' },
        { id: 'form-field', reason: 'The subscribe field.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: [
        'button',
        'form-field',
        'incident-timeline',
        'relative-time',
        'status-badge',
        'text-field',
        'uptime-bar',
      ],
    },
    testing: {
      unit: [
        'Renders title, overall banner, summary tiles, grouped services, history bar names and "Updated … ago" with the right heading levels.',
        'Shows active and past incidents as named articles with status, severity, duration and updates.',
        '`showIncidents={false}` removes incident sections and the incident tile.',
        'Maintenance notices and empty states; unknown overall state with no services.',
        '`renderHistory`, `layout="grid"` (14 days) and `headingLevel`.',
        'Subscribes from the keyboard with a trimmed address and a neutral `role="status"` confirmation.',
        'Rate-limit and network errors mark the field invalid with distinct messages.',
        'Translated messages, including nested records.',
        '`deriveOverallState` and `formatIncidentDuration`.',
        'Server render.',
      ],
      axe: true,
      ssr: true,
      browser: ['Visual baselines: /__gallery', 'Cross-browser axe: /components/status-page-view'],
    },
  },
} satisfies ComponentDocs;
