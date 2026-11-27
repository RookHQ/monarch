import { useState } from 'react';
import { Button, Link } from '@rookcom/monarch-react';
import { NotificationCenter, type NotificationItem } from '@rookcom/monarch-react/collaboration';
import type { ComponentDocs } from '../../../internal/component-docs.js';
// The scenario of the primary example, shared with iOS and Android (CROSS-PLATFORM.md section 8).
import fixture from './notification-center.fixture.json';

const NOW = fixture.data.now;
const SAMPLE = fixture.data.items.map((item) => ({
  ...item,
  ...('actor' in item && { actor: { name: item.actor } }),
})) as NotificationItem[];

function Demo() {
  const [items, setItems] = useState(SAMPLE);
  const markRead = (id: string) =>
    setItems((list) => list.map((i) => (i.id === id ? { ...i, read: true } : i)));
  return (
    <NotificationCenter
      defaultOpen
      items={items}
      onMarkRead={markRead}
      onMarkAllRead={() => setItems((list) => list.map((i) => ({ ...i, read: true })))}
      onSelect={() => {}}
      hasMore={fixture.data.hasMore}
      onLoadMore={() => {}}
      footer={<Link href="#/components/notification-center">{fixture.data.footer}</Link>}
      now={NOW}
      timeZone="UTC"
      align="start"
    />
  );
}

export default {
  id: 'notification-center',
  title: 'NotificationCenter',
  summary:
    'Bell with an unread count and a panel: All and Unread tabs, day groups, mark read, load more and an empty state.',
  sections: {
    purpose: {
      summary:
        'Gives every app the same in-app inbox: a bell in the header or rail that shows how many notifications are unread and opens a panel to read, open and clear them. The product supplies the items and handles reading and navigation.',
      useWhen: [
        'An app header or icon rail that needs mentions, assignments and replies.',
        'Any surface that marks notifications read when opened.',
      ],
      avoidWhen: [
        'A full-page notifications inbox with bulk actions: build it from ActivityTimeline or a list.',
        'Transient confirmations: use Toast.',
        'Per-type delivery settings: use SwitchSettingsList on a settings page and link to it in `footer`.',
      ],
    },
    designPrinciples: {
      items: [
        'Unread is never colour alone: bold text, a dot and a hidden “Unread:” prefix.',
        'The count on the bell is in its accessible name (“Notifications, 3 unread”), the visual badge is hidden.',
        'One panel, two tabs: All and Unread, through Monarch TabsFilter, rather than separate pages.',
        'Reading is the product’s decision: `markReadOnSelect` is on by default, and both read actions are callbacks.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'The ref and button attributes go to the built-in bell. With `trigger`, pass your own element and give it the count in its name.',
        '`open` / `defaultOpen` / `onOpenChange` and `filter` / `defaultFilter` / `onFilterChange` work controlled or uncontrolled.',
        'Items are filtered client-side by `read` on the Unread tab; refetch in `onFilterChange` when you page on the server.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [
        {
          prop: 'groupByDay',
          values: ['true', 'false'],
          description: 'Today, Yesterday and dated headings, or one flat list.',
        },
        {
          prop: 'trigger',
          values: ['bell (default)', 'custom element'],
          description: 'A rail item or menu row instead of the bell button.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Unread',
          attribute: 'data-unread',
          description: 'Bold title, accent dot, “Mark as read” button when `onMarkRead` is set.',
        },
        { name: 'Read', description: 'Muted text, no dot.' },
        { name: 'Loading', description: 'Skeleton rows.' },
        { name: 'Error', description: 'The `error` node replaces the list.' },
        {
          name: 'Empty',
          description: '“You’re all caught up”, or “No unread notifications” on the Unread tab.',
        },
        {
          name: 'Open',
          attribute: 'data-state="open"',
          description: 'The bell stays highlighted while the panel is open.',
        },
      ],
    },
    interaction: {
      items: [
        'Clicking the bell opens the panel; clicking outside or Escape closes it and returns focus to the bell.',
        'Activating an item calls `onMarkRead` (if unread and `markReadOnSelect`) and `onSelect`; with `href` it is a real link.',
        '“Mark all as read” is disabled when nothing is unread.',
        'LoadMore at the end of the list calls `onLoadMore`.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Enter / Space', action: 'On the bell, opens the panel and moves focus into it.' },
        {
          keys: 'Tab',
          action:
            'Moves through Mark all as read, the tabs, each item and its Mark as read button.',
        },
        {
          keys: 'Arrow Left / Right, Home / End',
          action: 'On the tabs, switch between All and Unread.',
        },
        { keys: 'Escape', action: 'Closes the panel and returns focus to the bell.' },
      ],
    },
    accessibility: {
      role: 'button (bell) controlling a dialog (panel)',
      name: 'Bell: “Notifications, N unread”. Panel: labelled by its heading.',
      items: [
        'The bell has `aria-haspopup="dialog"` and `aria-expanded`.',
        'Day headings label their lists, so a screen reader hears “Today, list, 2 items”.',
        'Unread items start with hidden “Unread:” text; the dot is decorative.',
        'The tabs are a real tablist with one panel; the panel is scrollable and focusable.',
      ],
    },
    responsive: {
      items: [
        'The panel is 24rem wide, never wider than the viewport minus 1rem, and at most 34rem tall; the list scrolls inside it.',
        'Day headings stick to the top while scrolling.',
      ],
    },
    theme: {
      items: [
        'The panel is a Monarch popover surface. The badge uses `danger-solid` with a ring in the page surface colour; the unread dot uses `accent-solid`.',
        'Forced colours draw the badge and dot in CanvasText.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Bell and panel',
          stage: true,
          render: () => <Demo />,
        },
        {
          title: 'Custom trigger',
          render: () => (
            <NotificationCenter
              items={SAMPLE}
              trigger={<Button size="sm">Inbox · 2 unread</Button>}
              now={NOW}
              timeZone="UTC"
              align="start"
            />
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Write titles as sentences with the actor first: “Ana Lopez mentioned you in Launch checklist”.',
        'Pass `unreadCount` from the server when you page.',
        'Link to notification settings in `footer`.',
      ],
      dont: [
        'Do not show the badge count anywhere without the bell’s accessible name.',
        'Do not mark items read just because the panel opened.',
        'Do not put actions other than read state inside the list rows.',
      ],
    },
    related: {
      items: [
        { id: 'activity-timeline', reason: 'Full-page feeds of events.' },
        { id: 'tabs', reason: 'TabsFilter drives the All and Unread tabs.' },
        { id: 'popover', reason: 'The panel surface.' },
        { id: 'load-more', reason: 'Paging older notifications.' },
        { id: 'toast', reason: 'Transient confirmations.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: [
        'popover',
        'tabs',
        'button',
        'avatar',
        'relative-time',
        'load-more',
        'empty-state',
        'skeleton',
      ],
    },
    testing: {
      unit: [
        'Names the bell with the unread count; caps the badge at 99+; no badge when all are read.',
        'Opens a labelled dialog, moves focus in, and Escape returns focus to the bell.',
        'Groups by day and marks unread items for screen readers; href items are links.',
        'Selecting marks unread items read and calls onSelect; Mark as read and Mark all as read.',
        'All and Unread tabs by keyboard, uncontrolled and controlled; controlled open.',
        'Empty states for both tabs; load more; loading and error.',
        'Flat list, footer, custom trigger, translations, forwarded ref.',
      ],
      axe: true,
      ssr: true,
      browser: [
        'Visual baselines: /__gallery/notification-center (closed and open)',
        'Cross-browser axe: /components/notification-center',
      ],
    },
  },
} satisfies ComponentDocs;
