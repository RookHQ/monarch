import { useState } from 'react';
import { SecurityAlertCard, type SecurityAlert } from '@rookcom/monarch-react/patterns';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const hoursAgo = (h: number) => new Date(Date.now() - h * 3_600_000);

const SAMPLE: SecurityAlert[] = [
  {
    id: 'a-1',
    title: 'New sign-in from Firefox on Linux',
    description: 'Lisbon, Portugal. If this was not you, sign that session out.',
    severity: 'high',
    createdAt: hoursAgo(1),
    action: { label: 'Review sessions', href: '#sessions' },
  },
  {
    id: 'a-2',
    title: 'Two-step verification turned on',
    severity: 'info',
    createdAt: hoursAgo(26),
  },
  {
    id: 'a-3',
    title: 'Password changed',
    severity: 'medium',
    createdAt: hoursAgo(24 * 6),
    read: true,
  },
];

function Managed() {
  const [alerts, setAlerts] = useState(SAMPLE);
  return (
    <SecurityAlertCard
      alerts={alerts}
      onMarkRead={(id) =>
        setAlerts((all) => all.map((a) => (a.id === id ? { ...a, read: true } : a)))
      }
      onMarkAllRead={() => setAlerts((all) => all.map((a) => ({ ...a, read: true })))}
      onDismiss={(id) => setAlerts((all) => all.filter((a) => a.id !== id))}
    />
  );
}

export default {
  id: 'security-alert-card',
  title: 'SecurityAlertCard',
  summary: 'Recent security alerts for an account, with severity, unread markers and follow-ups.',
  sections: {
    purpose: {
      summary:
        'Security events such as a sign-in from a new device or a changed password need to reach the person with enough context to act. SecurityAlertCard lists recent alerts with a severity badge, time and unread marker, and offers a follow-up action, mark as read and dismiss. Needed by: Rook ID (security settings).',
      useWhen: [
        'A security or account page showing recent security events.',
        'A dashboard card summarising alerts that need attention.',
      ],
      avoidWhen: [
        'A full, filterable audit trail: use AuditLogViewer.',
        'General notifications that are not about account security: use NotificationCenter.',
      ],
    },
    designPrinciples: {
      items: [
        'Severity is spoken text in a badge as well as an icon and colour, never colour alone.',
        'Each alert says what happened and, when there is one, what to do next.',
        'Unread alerts are counted in the header and marked on the row, including for screen readers.',
        'Dismissing keeps focus in the list so people can work through alerts with the keyboard.',
      ],
    },
    apiReference: { source: 'typescript' },
    typedProperties: {
      source: 'typescript',
      notes: [
        '`alerts` is `{ id, title, description?, severity?, createdAt?, read?, action? }[]`; `action` is `{ label, href?, onClick? }`.',
        '`severity` is `info`, `low`, `medium`, `high` or `critical`; `messages.severity` renames them.',
        'Migrating from the app copy (Rook ID `SecurityAlertsCard`): fetch `/users/me/security-alerts` in the app and map `created_at` → `createdAt`, `is_read` → `read`, and the free-text `severity` onto the five levels. The spinner and empty box become `state` and the built-in empty state.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'severity (per alert)',
          values: ['info', 'low', 'medium', 'high', 'critical'],
          description: 'Icon, badge tone and, for critical, a danger border.',
        },
        {
          prop: 'state',
          values: ['ready', 'loading', 'error'],
          description: 'What the card body shows. An empty `alerts` list shows the empty state.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Card state',
          attribute: 'data-state',
          description: '`ready`, `loading`, `empty` or `error`, on the root.',
        },
        {
          name: 'Severity',
          attribute: 'data-severity',
          description: 'On each row.',
        },
        {
          name: 'Unread',
          attribute: 'data-unread',
          description: 'A dot, a bolder title and a spoken "Unread:" prefix.',
        },
      ],
    },
    interaction: {
      items: [
        '"Mark as read" appears on unread alerts when `onMarkRead` is set; "Mark all as read" in the header when `onMarkAllRead` is set and any are unread.',
        'The dismiss button calls `onDismiss(id)`, announces it and moves focus to the next alert’s dismiss button.',
        'An `action` with `href` renders a link styled as a button; without `href` a button calling `onClick`.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Tab / Shift+Tab',
          action: 'Moves through follow-up, mark-as-read and dismiss controls.',
        },
        { keys: 'Enter / Space', action: 'Activates the focused control.' },
      ],
    },
    accessibility: {
      role: 'region (section) containing a list',
      name: 'The card title; the list is named "Security alerts".',
      items: [
        'Dismiss buttons are named with the alert ("Dismiss Password changed").',
        'Severity icons are decorative; the badge carries the severity as text.',
        'Dismissals are announced through a polite status region.',
      ],
    },
    responsive: {
      items: [
        'Text wraps inside the row; actions wrap onto more lines when narrow.',
        'Buttons are at least 44px tall and the dismiss button 44px square on touch screens.',
      ],
    },
    theme: {
      items: [
        'Icon backgrounds use the info, warning and danger subtle tokens; critical rows get the danger border.',
        'The unread dot uses the accent solid colour.',
        'In forced-colors mode rows and icons get system-colour borders and the dot uses Highlight.',
      ],
    },
    examples: {
      items: [
        { title: 'Mark read and dismiss', stack: true, render: () => <Managed /> },
        { title: 'Empty', stack: true, render: () => <SecurityAlertCard alerts={[]} /> },
      ],
    },
    doDont: {
      do: [
        'Give high and critical alerts an `action` that lets the person respond.',
        'Write titles that say what happened and where ("New sign-in from Firefox on Linux").',
      ],
      dont: [
        'Do not mark everything critical; keep critical for events that need action now.',
        'Do not put secrets or full IP histories in the description.',
      ],
    },
    related: {
      items: [
        { id: 'session-list', reason: 'Where a "review sessions" follow-up usually leads.' },
        { id: 'device-list', reason: 'Known devices behind new-device alerts.' },
        { id: 'audit-log-viewer', reason: 'The full, filterable activity log.' },
        { id: 'notification-center', reason: 'General notifications.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['button', 'badge', 'relative-time', 'section-card'],
    },
    testing: {
      unit: [
        'Lists alerts with severity, time, unread marker, count and link action; axe passes.',
        'Marks one or all alerts read.',
        'Dismisses with the keyboard, announces it and keeps focus in the list.',
        'Runs a button action.',
        'Loading, empty and error states.',
        'Translated messages including severity names.',
        'Renders on the server.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
