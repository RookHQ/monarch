import { useState } from 'react';
import { Badge, Link, TabsFilter } from '@rookcom/monarch-react';
import { ActivityTimeline, type ActivityEvent } from '@rookcom/monarch-react/collaboration';
import type { ComponentDocs } from '../../../internal/component-docs.js';
import { PencilIcon, PlusIcon } from '../../../internal/docs-demo.js';

const NOW = '2026-03-02T12:00:00Z';
/** Built lazily: docs files cannot create JSX at module scope. */
const sampleEvents = (): (ActivityEvent & { type: 'change' | 'comment' })[] => [
  {
    id: 'e1',
    type: 'change',
    actor: { name: 'Ana Lopez' },
    verb: 'moved',
    object: (
      <>
        <Link href="#/components/activity-timeline">Pricing page refresh</Link> to In review
      </>
    ),
    time: '2026-03-02T11:20:00Z',
    tone: 'info',
    details: (
      <dl style={{ margin: 0, display: 'grid', gridTemplateColumns: 'auto 1fr', gap: '2px 12px' }}>
        <dt>Status</dt>
        <dd style={{ margin: 0 }}>In progress → In review</dd>
        <dt>Reviewer</dt>
        <dd style={{ margin: 0 }}>None → Ben Okafor</dd>
      </dl>
    ),
  },
  {
    id: 'e2',
    type: 'comment',
    actor: { name: 'Ben Okafor' },
    verb: 'commented',
    time: '2026-03-02T10:05:00Z',
    icon: <PencilIcon />,
    body: 'Copy looks good. Can we swap the hero image for the lighter one?',
  },
  {
    id: 'e3',
    type: 'change',
    actor: { name: 'Auto-assign rule', kind: 'automation' },
    verb: 'assigned',
    object: 'Chloé Martin',
    time: '2026-03-02T08:30:00Z',
    meta: (
      <Badge size="sm" tone="neutral">
        Rule: Round robin
      </Badge>
    ),
  },
  {
    id: 'e4',
    type: 'change',
    actor: { name: 'Dev Patel' },
    verb: 'created this task',
    time: '2026-03-01T16:45:00Z',
    icon: <PlusIcon />,
    tone: 'success',
  },
];

function FilteredDemo() {
  const [filter, setFilter] = useState('all');
  const events =
    filter === 'all' ? sampleEvents() : sampleEvents().filter((e) => e.type === filter);
  return (
    <div style={{ width: '100%', maxWidth: 560 }}>
      <ActivityTimeline
        events={events}
        defaultExpandedIds={['e1']}
        filters={
          <TabsFilter
            aria-label="Show"
            variant="pill"
            value={filter}
            onValueChange={setFilter}
            options={[
              { value: 'all', label: 'All' },
              { value: 'comment', label: 'Comments' },
              { value: 'change', label: 'Changes' },
            ]}
          />
        }
        hasMore
        onLoadMore={() => {}}
        now={NOW}
        timeZone="UTC"
      />
    </div>
  );
}

export default {
  id: 'activity-timeline',
  title: 'ActivityTimeline',
  summary:
    'Vertical feed of events: actor, verb, object, time and marker, grouped by day, with expandable details and filters.',
  sections: {
    purpose: {
      summary:
        'Tells the story of a record or a workspace as a readable list of sentences (“Ana Lopez moved Pricing page to In review”), with who did it, when, and optional details. Ten Rook products show this pattern; this is the shared one.',
      useWhen: [
        'A record’s history: a ticket, task, client, incident or IP record.',
        'Project or workspace activity, with comments and changes interleaved.',
        'Status-page incident updates (a stage per event, automated updates tagged).',
      ],
      avoidWhen: [
        'A live conversation with replies and reactions: use MessageTimeline and MessageRow.',
        'A security audit trail that needs IP, device and export: use AuditLogViewer.',
        'Unread inbox items: use NotificationCenter.',
      ],
    },
    designPrinciples: {
      items: [
        'Events read as sentences: actor in strong text, then the verb, then the object.',
        'People and automation are always distinguishable: automation and system actors carry an “Automated” tag in text.',
        'Detail on demand: diffs and payloads sit behind a disclosure, so the feed stays scannable.',
        'The product humanises events; Monarch lays them out. Verbs and objects are nodes the product writes.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Each event: `id`, `verb`, `time`, and optional `actor`, `object`, `icon`, `tone`, `meta`, `body`, `details`.',
        '`expandedIds` / `defaultExpandedIds` / `onExpandedChange` control the open details.',
        'The ref is forwarded to the root `<div>`.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [
        {
          prop: 'markers',
          values: ['dot', 'avatar'],
          description: 'Tone-coloured dots, or the actor’s avatar. An event `icon` wins over both.',
        },
        {
          prop: 'density',
          values: ['default', 'compact'],
          description: 'Compact for side panels.',
        },
        {
          prop: 'groupByDay',
          values: ['true', 'false'],
          description: 'Day headings, or one ordered list.',
        },
        {
          prop: 'ActivityEvent.tone',
          values: ['neutral', 'accent', 'info', 'success', 'warning', 'danger'],
          description:
            'Marker colour for stages such as resolved or failed. Never the only signal: say it in the verb too.',
        },
      ],
    },
    states: {
      items: [
        { name: 'Loading', description: 'Skeleton rows.' },
        { name: 'Empty', description: '“No activity yet”, or the `empty` node.' },
        { name: 'Error', description: 'The `error` node replaces the feed.' },
        {
          name: 'Details open',
          description: 'The toggle reads “Hide details” with `aria-expanded="true"`.',
        },
        { name: 'More to load', description: 'LoadMore after the feed.' },
      ],
    },
    interaction: {
      items: [
        '“Show details” toggles the event’s details; `onExpandedChange` reports the open ids.',
        'Filters are the product’s: put controls in `filters` and pass the filtered `events`.',
        'LoadMore calls `onLoadMore` and announces the new events.',
        'Timestamps show the absolute time on hover and focus (RelativeTime).',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Tab',
          action:
            'Moves through filters, links in events, timestamps, detail toggles and Load more.',
        },
        { keys: 'Enter / Space', action: 'Toggles details on a “Show details” button.' },
        { keys: 'Escape', action: 'On a focused timestamp, hides its tooltip.' },
      ],
    },
    accessibility: {
      role: 'ordered lists (one per day) labelled by their day heading',
      name: 'Each day list is labelled by its heading; an ungrouped list by `aria-label` (default “Activity”).',
      items: [
        'Markers and icons are decorative (`aria-hidden`); the sentence carries the meaning.',
        'Times are `<time datetime>` with the absolute time in a tooltip on focus.',
        'Detail toggles use `aria-expanded` and `aria-controls`.',
        'Day headings default to h3; set `dayHeadingLevel` to fit the page outline.',
      ],
    },
    responsive: {
      items: [
        'Sentences wrap; long objects break; filters wrap onto several lines.',
        'Nothing is hidden on narrow screens.',
      ],
    },
    theme: {
      items: [
        'The connector is `border-subtle`; dot tones use the status solids; bodies use a `bg-surface` card and details a `bg-subtle` panel.',
        'Forced colours draw markers and the connector in CanvasText.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Task activity with filters and details',
          render: () => <FilteredDemo />,
        },
        {
          title: 'Compact, with avatars',
          render: () => (
            <div style={{ width: '100%', maxWidth: 360 }}>
              <ActivityTimeline
                events={sampleEvents().slice(0, 3)}
                markers="avatar"
                density="compact"
                groupByDay={false}
                aria-label="Recent activity"
                now={NOW}
                timeZone="UTC"
              />
            </div>
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Write verbs in the past tense and objects as links where they lead somewhere.',
        'Set `actor.kind` for rules, imports and integrations.',
        'Use `details` for field diffs and payloads; keep the sentence short.',
      ],
      dont: [
        'Do not rely on marker colour alone for meaning.',
        'Do not put raw JSON in the sentence.',
        'Do not use it for chat: replies, reactions and grouping belong to MessageRow.',
      ],
    },
    related: {
      items: [
        { id: 'audit-log-viewer', reason: 'Security events with IP, device, export and a drawer.' },
        { id: 'message-timeline', reason: 'Conversations.' },
        { id: 'notification-center', reason: 'Unread inbox items.' },
        { id: 'load-more', reason: 'Paging the feed.' },
        { id: 'relative-time', reason: 'Event times.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['avatar', 'relative-time', 'load-more', 'skeleton'],
    },
    testing: {
      unit: [
        'Ordered list per day labelled by its heading, with the actor-verb-object sentence and `<time datetime>`.',
        'Tags automation actors.',
        'Details disclosure by keyboard, uncontrolled and controlled.',
        'Filter slot, ungrouped list and avatar markers with a dot fallback.',
        'Loading, empty, error and load more; heading level, density and translations.',
      ],
      axe: true,
      ssr: true,
      browser: [
        'Visual baselines: /__gallery/activity-timeline',
        'Cross-browser axe: /components/activity-timeline',
      ],
    },
  },
} satisfies ComponentDocs;
