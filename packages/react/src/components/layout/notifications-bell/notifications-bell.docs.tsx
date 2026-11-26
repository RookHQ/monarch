import { useState } from 'react';
import { type NotificationItem } from '@rookcom/monarch-react/collaboration';
import { NotificationsBell } from '@rookcom/monarch-react/shell';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const now = new Date('2026-03-02T12:00:00Z');
const initial: NotificationItem[] = [
  {
    id: 'n1',
    title: 'Ana Lopez assigned you “Quarterly plan”',
    time: '2026-03-02T11:40:00Z',
    actor: { name: 'Ana Lopez' },
  },
  {
    id: 'n2',
    title: 'Sam Lee replied to your comment',
    description: 'Looks good to me, shipping it after lunch.',
    time: '2026-03-02T09:15:00Z',
    actor: { name: 'Sam Lee' },
  },
  { id: 'n3', title: 'Your export is ready', time: '2026-03-01T17:05:00Z', read: true },
];

function useInbox() {
  const [items, setItems] = useState(initial);
  return {
    items,
    onMarkRead: (id: string) =>
      setItems((list) => list.map((n) => (n.id === id ? { ...n, read: true } : n))),
    onMarkAllRead: () => setItems((list) => list.map((n) => ({ ...n, read: true }))),
  };
}

function IconBell() {
  const inbox = useInbox();
  return <NotificationsBell {...inbox} now={now} seeAllHref="#notifications" />;
}

function Rows() {
  const inbox = useInbox();
  return (
    <div style={{ display: 'grid', gap: 'var(--mn-space-2)', width: '15rem' }}>
      <NotificationsBell {...inbox} now={now} variant="row" />
      <NotificationsBell {...inbox} now={now} variant="row" collapsed />
    </div>
  );
}

export default {
  id: 'notifications-bell',
  title: 'NotificationsBell',
  summary: 'The shell’s bell or menu row with the unread count, opening the notifications panel.',
  sections: {
    purpose: {
      summary:
        'Puts in-app notifications in the app shell: an icon button for the identity bar, or a labelled row for the rail, drawer or mobile menu, both opening the NotificationCenter panel. Needed by: Tickets (rail and mobile menu), People (rail, header and mobile drawer).',
      useWhen: [
        'An app shell needs a notifications entry with an unread count.',
        'The same notifications must be reachable from the desktop rail and the mobile menu.',
      ],
      avoidWhen: [
        'A full notifications page: list items with InboxRow or NotificationCenter content on the page itself.',
        'A link to a notifications page with no panel: use RookRail’s `notifications` item.',
      ],
    },
    designPrinciples: {
      items: [
        'One panel: the list, tabs, day groups and mark-read are NotificationCenter’s; this component adds the shell triggers and a See all link.',
        'Data in, callbacks out: the app polls or subscribes, marks read and navigates.',
        'The count is part of the name ("Notifications, 3 unread"), never only a red badge.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Forwards its ref to the trigger button. Takes every NotificationCenter prop except `trigger` and `footer`; other button attributes (`id`, `data-*`) go to the trigger.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        '`unreadCount` is the server total and can exceed the loaded items; counts above 99 show "99+".',
        'Migrating from the app copies (Tickets and People `NotificationsBell`): keep the query and mutation hooks in the app. Map each notification to `{ id, title, description: body, time: createdAt, read: !!readAt }`; pass `onMarkRead={(id) => mark.mutate([id])}`, `onMarkAllRead`, and navigate in `onSelect` (People’s `notificationHref` stays in the app). `variant="icon"` stays `icon`; `rail` and `menu` both become `row` (add `collapsed` in a collapsed rail); People’s footer link becomes `seeAllHref`. Loading and error states map to `loading` and `error`.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'variant',
          values: ['icon', 'row'],
          description: 'A square icon button, or a full-width row with the label and count.',
        },
        {
          prop: 'collapsed',
          values: ['false', 'true'],
          description: 'Row only: icon with a badge and a tooltip, for the 48px rail.',
        },
      ],
    },
    states: {
      items: [
        { name: 'Unread', attribute: 'data-unread', description: 'Badge or count is shown.' },
        { name: 'Open', attribute: 'data-state="open"', description: 'The panel is showing.' },
        { name: 'Collapsed', attribute: 'data-collapsed', description: 'Icon-only row.' },
        {
          name: 'Loading, error, empty',
          description:
            'Shown inside the panel by NotificationCenter (`loading`, `error`, `empty`).',
        },
      ],
    },
    interaction: {
      items: [
        'Clicking the trigger opens the panel; choosing an item marks it read, closes the panel and calls `onSelect`.',
        'See all closes the panel and follows `seeAllHref`.',
        'In a RookRail panel the trigger uses the rail’s on-accent colours.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Enter / Space', action: 'Open the panel from the trigger.' },
        { keys: 'Tab', action: 'Move through the tabs, items and See all.' },
        { keys: 'Escape', action: 'Close the panel and return focus to the trigger.' },
      ],
    },
    accessibility: {
      role: 'button opening a dialog (popover)',
      name: '"Notifications, 3 unread" (`messages.triggerLabel`); the row name starts with its visible label.',
      items: [
        'The badge digits are hidden from assistive technology because the name already carries the count.',
        'Icon-only forms show the name in a tooltip on hover and focus.',
        'The panel is named by its heading; unread items are prefixed "Unread".',
      ],
    },
    responsive: {
      items: [
        'The panel is NotificationCenter’s, capped to the viewport width.',
        'Triggers are 44px on touch screens; the row truncates long labels.',
      ],
    },
    theme: {
      items: [
        'Badge in `--mn-color-danger-solid`; hover in `--mn-color-bg-muted`, or the accent hover inside RookRail.',
        'Forced colours draw the badge in CanvasText; reduced motion removes transitions.',
      ],
    },
    examples: {
      items: [
        { title: 'Icon button with See all', render: () => <IconBell /> },
        { title: 'Row and collapsed row', render: () => <Rows /> },
      ],
    },
    doDont: {
      do: [
        'Pass the server’s unread total in `unreadCount` when you load only the latest page.',
        'Refresh the list when the socket reports a new notification, with slow polling as a fallback.',
      ],
      dont: [
        'Do not put two bells in the same view (rail and header); pick one per breakpoint.',
        'Do not navigate from `onMarkRead`; use `onSelect`.',
      ],
    },
    related: {
      items: [
        { id: 'notification-center', reason: 'The panel it opens.' },
        { id: 'rook-rail', reason: 'Has a simple notifications link item.' },
        { id: 'identity-bar', reason: 'Has a `notifications` slot for the icon variant.' },
      ],
    },
    dependencies: { packages: ['react'], components: ['notification-center'] },
    testing: {
      unit: [
        'Icon button named with the unread count and badge; extra props reach the trigger.',
        'Opens with Enter; choosing marks read, closes and returns focus.',
        'Row and collapsed row; server count and 99+.',
        'See all link closes the panel; translation.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
