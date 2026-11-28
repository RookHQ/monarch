import { Link } from '@rookcom/monarch-react';
import { ActivityWidget } from '@rookcom/monarch-react/patterns';
import type { ComponentDocs } from '../../../internal/component-docs.js';

// The scenario of the primary example, shared with iOS and Android (CROSS-PLATFORM.md section 8).
import fixture from './activity-widget.fixture.json';

const now = fixture.data.now;
const [e1, e2, e3, e4] = fixture.data.events as unknown as [
  { id: string; actor: string; verb: string; object: string; time: string },
  {
    id: string;
    actor: string;
    kind: 'automation';
    verb: string;
    object: string;
    time: string;
    tone: 'warning';
  },
  { id: string; actor: string; verb: string; object: string; time: string },
  { id: string; actor: string; verb: string; object: string; time: string },
];
const events = [
  {
    id: e1.id,
    actor: { name: e1.actor },
    verb: e1.verb,
    object: <Link href="#r1">{e1.object}</Link>,
    time: e1.time,
  },
  {
    id: e2.id,
    actor: { name: e2.actor, kind: e2.kind },
    verb: e2.verb,
    object: e2.object,
    time: e2.time,
    tone: e2.tone,
  },
  {
    id: e3.id,
    actor: { name: e3.actor },
    verb: e3.verb,
    object: <Link href="#r2">{e3.object}</Link>,
    time: e3.time,
  },
  {
    id: e4.id,
    actor: { name: e4.actor },
    verb: e4.verb,
    object: e4.object,
    time: e4.time,
  },
];

export default {
  id: 'activity-widget',
  title: 'ActivityWidget',
  summary: 'A dashboard widget with the latest activity, built from Widget and ActivityTimeline.',
  sections: {
    purpose: {
      summary:
        'Shows the last few things that happened in a workspace or Space on its dashboard, with a link to the full activity log. People and automations are told apart. Needed by: Atlas, Intellect, Dots, Stride, Tickets.',
      useWhen: [
        'A dashboard should show what changed recently.',
        'The full log lives on another page.',
      ],
      avoidWhen: [
        'The full activity log or audit trail: use ActivityTimeline or AuditLogViewer.',
        'Notifications for the person: use NotificationCenter.',
      ],
    },
    designPrinciples: {
      items: [
        'Short: a handful of events and a view-all link, never an endless feed on a dashboard.',
        'Automations are labelled "Automated" so readers can tell people from rules.',
        'Same states as every Widget: loading, empty and error stay in the card.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Takes every Widget prop (period, menu, `viewAllHref`, `span`, `updatedAt`, loading and error) plus the timeline props.',
        'Renders a Widget `<section>`; the ref is forwarded to it.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Migrating from the app copy: Intellect `Activity` (`components/intellect/dashboard/widgets.tsx`) maps each item to an ActivityEvent: `actorName` → `actor.name`, `source === "system" | "registry"` → `actor.kind: "system"`, `actionLabel(action)` → `verb`, the asset link → `object`, `at` → `time`; "Audit log" → `viewAllHref`. Atlas `RecentActivity` (a DataTable in `OverviewView.tsx`) and Dots `Activity` (`components/home/HomeDashboard.tsx`) map the same way.',
      ],
    },
    variants: {
      items: [
        { prop: 'markers', values: ['dot', 'avatar'], description: 'Each event’s marker.' },
        {
          prop: 'groupByDay',
          values: ['false', 'true'],
          description: 'Group under Today, Yesterday and dates. Default false.',
        },
      ],
    },
    states: {
      items: [
        { name: 'Ready', description: 'Up to `limit` events, newest first.' },
        {
          name: 'Loading',
          attribute: 'data-state="loading"',
          description: 'The timeline’s own skeleton, with `aria-busy`.',
        },
        { name: 'Empty', description: '"No activity yet", or `emptyContent`.' },
        {
          name: 'Error',
          attribute: 'data-state="error"',
          description: 'The error with Try again.',
        },
      ],
    },
    interaction: {
      items: [
        'Links inside events and the view-all link navigate.',
        'Events with `details` expand behind a disclosure button.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Tab / Shift+Tab',
          action: 'Moves through links, disclosure buttons and widget controls.',
        },
        { keys: 'Enter / Space', action: 'Activates the focused control.' },
      ],
    },
    accessibility: {
      role: 'region containing a list',
      name: 'The title, default "Recent activity"; the list has the same name',
      items: [
        'Each event is a list item with a readable sentence: actor, verb, object and time.',
        'Automated actors carry a visible "Automated" tag.',
      ],
    },
    responsive: {
      items: ['Compact density; sentences wrap, so the widget fits 390px without sideways scroll.'],
    },
    theme: {
      items: [
        'Widget surface and border tokens; markers use the activity tone tokens.',
        'In forced colours the widget border is CanvasText.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Recent activity with a view-all link',
          stack: true,
          render: () => (
            <ActivityWidget
              style={{ maxWidth: '30rem' }}
              events={events}
              now={now}
              timeZone={fixture.data.timeZone}
              viewAllHref="#activity"
            />
          ),
        },
        {
          title: 'Grouped by day with avatars',
          stack: true,
          render: () => (
            <ActivityWidget
              style={{ maxWidth: '30rem' }}
              events={events}
              now={now}
              timeZone="UTC"
              groupByDay
              markers="avatar"
              title="Team activity"
            />
          ),
        },
      ],
    },
    doDont: {
      do: ['Link to the full log with `viewAllHref`.', 'Mark automations with `actor.kind`.'],
      dont: [
        'Do not raise `limit` above about ten on a dashboard.',
        'Do not use it for notifications.',
      ],
    },
    related: {
      items: [
        { id: 'activity-timeline', reason: 'The full feed with filters and load more.' },
        { id: 'audit-log-viewer', reason: 'A searchable audit trail.' },
        { id: 'widget', reason: 'The frame this is built on.' },
      ],
    },
    dependencies: { packages: ['react'], components: ['activity-timeline', 'widget'] },
    testing: {
      unit: [
        'Named region with the first `limit` events and a view-all link; props and ref forwarded.',
        'Automated actors tagged.',
        'limit, title and groupByDay.',
        'Empty text, loading with aria-busy and error with retry.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
