import {
  StatusPageView,
  type StatusPageDay,
  type StatusPageIncident,
  type StatusPageService,
} from '@rookcom/monarch-react/patterns';
import type { ComponentDocs } from '../../../internal/component-docs.js';

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

const services: StatusPageService[] = [
  {
    id: 'api',
    name: 'API',
    group: 'Platform',
    state: 'operational',
    uptimePercent: 99.97,
    responseTimeMs: 128,
    history: history(3, { 11: 'degraded' }),
  },
  {
    id: 'web',
    name: 'Web app',
    group: 'Platform',
    state: 'degraded',
    uptimePercent: 99.62,
    responseTimeMs: 410,
    history: history(5, { 29: 'degraded', 4: 'down' }),
  },
  {
    id: 'auth',
    name: 'Sign-in',
    group: 'Platform',
    state: 'operational',
    uptimePercent: 100,
    responseTimeMs: 96,
    history: history(7),
  },
  {
    id: 'mail',
    name: 'Email delivery',
    description: 'Outbound notifications',
    state: 'operational',
    uptimePercent: 99.99,
    history: history(2, { 20: 'maintenance' }),
  },
];

const active: StatusPageIncident[] = [
  {
    id: 'i1',
    title: 'Slow page loads in the web app',
    component: 'Web app',
    status: 'acknowledged',
    severity: 'warning',
    openedAt: '2026-03-30T08:40:00Z',
    updates: [
      {
        id: 'u1',
        stage: 'investigating',
        body: 'Response times are higher than usual. We are looking into it.',
        time: '2026-03-30T08:45:00Z',
        automated: true,
      },
      {
        id: 'u2',
        stage: 'identified',
        body: 'A slow database query is the cause. A fix is being deployed.',
        time: '2026-03-30T09:10:00Z',
      },
    ],
  },
];

const past: StatusPageIncident[] = [
  {
    id: 'i0',
    title: 'Sign-in errors',
    component: 'Sign-in',
    status: 'resolved',
    severity: 'critical',
    openedAt: '2026-03-05T14:02:00Z',
    resolvedAt: '2026-03-05T14:49:00Z',
    durationSeconds: 2820,
    updates: [
      {
        id: 'p1',
        stage: 'investigating',
        body: 'Some people cannot sign in.',
        time: '2026-03-05T14:05:00Z',
      },
      {
        id: 'p2',
        stage: 'resolved',
        body: 'Sign-in is working again for everyone.',
        time: '2026-03-05T14:49:00Z',
      },
    ],
  },
];

const now = '2026-03-30T09:30:00Z';

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
        '`renderHistory(service, days)` replaces the built-in day strip, for example with an UptimeBar; the legend is then left to the product.',
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
        'The built-in day strip is one image with a summary name, not 30 tab stops. For day-by-day keyboard detail pass an interactive history through `renderHistory`.',
      ],
    },
    accessibility: {
      role: 'Headings and regions: page title, overall banner, then named sections',
      name: 'Each section is labelled by its heading; incident cards are articles named by their title; each day strip is an image named with a summary ("API, last 30 days: 1 degraded, 29 operational").',
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
        'Forced colours: borders and icons use system colours; bad days are filled and good days outlined, so the strip still reads.',
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
                title="Example status"
                description="Live health of our services."
                supportHref="https://example.com/support"
                services={services}
                activeIncidents={active}
                pastIncidents={past}
                overallUptimePercent={99.89}
                showResponseTime
                updatedAt="2026-03-30T09:29:00Z"
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
            'A single service’s history elsewhere in a product; can be passed to `renderHistory`.',
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
      ],
    },
    testing: {
      unit: [
        'Renders title, overall banner, summary tiles, grouped services, day strip names and "Updated … ago" with the right heading levels.',
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
