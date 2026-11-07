import { IncidentTimeline, type IncidentUpdate } from '@rookcom/monarch-react/patterns';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const narrow = { width: '100%', maxWidth: '32rem' } as const;

const updates: IncidentUpdate[] = [
  {
    id: 'u1',
    stage: 'investigating',
    body: 'Requests to the API are failing for some customers. We are looking into it.',
    time: '2026-03-02T09:05:00Z',
    automated: true,
  },
  {
    id: 'u2',
    stage: 'identified',
    body: 'A configuration change caused the errors. We are rolling it back.',
    time: '2026-03-02T09:20:00Z',
  },
  {
    id: 'u3',
    stage: 'monitoring',
    body: 'The rollback is complete. Error rates are dropping.',
    time: '2026-03-02T09:40:00Z',
  },
  {
    id: 'u4',
    stage: 'resolved',
    body: 'Error rates are back to normal.',
    time: '2026-03-02T10:00:00Z',
  },
];

export default {
  id: 'incident-timeline',
  title: 'IncidentTimeline',
  summary: 'The updates posted during one incident, each with its stage, time and text.',
  sections: {
    purpose: {
      summary:
        'Shows the narrative of an incident as people read it: investigating, identified, monitoring, resolved, with the time and text of each update. Without it a status page can only change colour, which says something is wrong but not whether anyone is on it. Needed by: Pulse (public status page incidents and the incident detail view).',
      useWhen: [
        'A status page or incident card shows what has been posted about an incident.',
        'An internal incident view lists the updates sent to customers, with authors.',
      ],
      avoidWhen: [
        'A general record history (who changed what): use ActivityTimeline.',
        'A security or admin audit trail: use AuditLogViewer.',
      ],
    },
    designPrinciples: {
      items: [
        'The stage is always written out; the coloured marker only repeats it.',
        'System-posted updates are tagged "Automated", so the page never implies a person wrote them.',
        'Times default to UTC with the zone shown, so readers in different places quote the same time and the server and browser render the same text.',
        'Order is the caller’s: oldest first reads as a story, newest first suits a busy feed.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Also exports `resolveIncidentStage(stage, stages?, messages?)` and `formatIncidentTime(time, { locale, timeZone })` for products that show a stage or time elsewhere.',
        'Forwards its ref to the root `<div>`; other div attributes pass through.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Stage keys are matched case-insensitively against the four standard stages, so `INVESTIGATING` from an API works unchanged. Unknown keys get a readable label ("post_mortem" → "Post mortem") and a neutral tone unless `stages` defines them.',
        'Migrating from the app copy (Pulse `IncidentUpdates` in StatusPageView.tsx): `updates[].stage` keeps its values; `createdAt` → `time`; `automated` and `body` are unchanged; `STAGE_LABEL`/`STAGE_DOT` → `messages` and `stages`; the hard-coded "auto" chip → `messages.automated`.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'density',
          values: ['default', 'compact'],
          description: '`compact` uses smaller text and tighter spacing, for incident cards.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Stage',
          attribute: 'data-stage, data-tone (root and each item)',
          description:
            'Each item carries its stage and tone; the root carries the latest stage, so a product can style a card by it.',
        },
        {
          name: 'Automated',
          attribute: 'data-automated',
          description: 'The update was posted by the system and shows the "Automated" tag.',
        },
        {
          name: 'Empty',
          attribute: 'data-empty',
          description: 'No updates: shows `messages.empty` instead of the list.',
        },
      ],
    },
    interaction: {
      items: [
        'Read-only. Links or controls inside an update body keep their own behaviour.',
        'With `live`, updates appended later are announced politely by screen readers.',
        'The `footer` slot holds the product’s own form for posting an update.',
      ],
    },
    keyboard: {
      items: [],
      notes: [
        'Not interactive. Tab reaches only controls the product puts in bodies or the footer.',
      ],
    },
    accessibility: {
      role: 'list (ordered)',
      name: '`aria-label`, or `messages.label` ("Incident updates").',
      items: [
        'An `<ol>`, so screen readers announce the number of updates and each position.',
        'Each time is a `<time datetime>` with the ISO value.',
        'The marker is decorative (`aria-hidden`); the stage text carries the meaning.',
        'Opt-in polite live region (`live`) with `aria-relevant="additions"`.',
      ],
    },
    responsive: {
      items: [
        'Fluid width; long words and URLs in a body wrap instead of overflowing at 390px.',
        'The stage, time, author and tag wrap onto a second line when narrow.',
        'Logical properties, so the marker and connector move to the right in right-to-left layouts.',
      ],
    },
    theme: {
      items: [
        'Markers use the status solids (`--mn-color-danger-solid`, warning, info, success) and stage text the matching `-strong` tokens, so all four themes keep contrast.',
        'Forced colours: markers, connector and text switch to system colours.',
      ],
    },
    examples: {
      items: [
        {
          title: 'A resolved incident, oldest first',
          stack: true,
          render: () => (
            <div style={narrow}>
              <IncidentTimeline updates={updates} />
            </div>
          ),
        },
        {
          title: 'Compact, in a local time zone, with authors',
          stack: true,
          render: () => (
            <div style={narrow}>
              <IncidentTimeline
                density="compact"
                timeZone="America/New_York"
                updates={updates.slice(1, 3).map((u) => ({ ...u, author: 'On-call engineer' }))}
              />
            </div>
          ),
        },
        {
          title: 'Product stages and an empty state',
          stack: true,
          render: () => (
            <div style={{ ...narrow, display: 'grid', gap: 'var(--mn-space-4)' }}>
              <IncidentTimeline
                stages={{ escalated: { label: 'Escalated', tone: 'warning' } }}
                updates={[
                  {
                    id: 'e1',
                    stage: 'escalated',
                    body: 'Paged the team that owns the service.',
                    time: '2026-03-02T09:10:00Z',
                  },
                ]}
              />
              <IncidentTimeline updates={[]} />
            </div>
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Pass `automated` for anything the system posted.',
        'Keep the default UTC time zone on public pages, or show the zone you choose.',
        'Turn on `live` when the page refreshes while people read it.',
      ],
      dont: [
        'Do not put internal notes in a public timeline; filter them in the product.',
        'Do not rely on the marker colour to say the stage.',
      ],
    },
    related: {
      items: [
        { id: 'status-page-view', reason: 'Shows incidents with this timeline inside each card.' },
        { id: 'activity-timeline', reason: 'For general record history with actors and verbs.' },
        { id: 'audit-log-viewer', reason: 'For searchable, exportable audit trails.' },
        { id: 'status-badge', reason: 'For the incident’s current status on its own.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: [],
    },
    testing: {
      unit: [
        'Renders an ordered list named "Incident updates" with stage, `<time datetime>` and text.',
        'Tags automated updates and shows authors.',
        'Sets the latest stage and tone on the root; forwards the ref.',
        'Accepts product stages, readable labels for unknown stages and translated messages.',
        'Empty message and footer slot.',
        'Polite live region only with `live`.',
        'Formats times in UTC by default and in a given zone.',
        'Server render matches the client text.',
      ],
      axe: true,
      ssr: true,
      browser: ['Visual baselines: /__gallery', 'Cross-browser axe: /components/incident-timeline'],
    },
  },
} satisfies ComponentDocs;
