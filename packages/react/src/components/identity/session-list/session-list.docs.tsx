import { useState } from 'react';
import {
  SessionList,
  describeUserAgent,
  type SessionListItem,
} from '@rookcom/monarch-react/patterns';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const wait = (ms: number) => new Promise<void>((resolve) => window.setTimeout(resolve, ms));
const hoursAgo = (h: number) => new Date(Date.now() - h * 3_600_000);

const SAMPLE: SessionListItem[] = [
  {
    id: 's-1',
    label: 'Chrome on macOS',
    current: true,
    signedInAt: hoursAgo(30),
    lastActiveAt: new Date(),
  },
  {
    id: 's-2',
    label: 'Safari on iPhone',
    kind: 'mobile',
    detail: 'Lisbon, Portugal',
    signedInAt: hoursAgo(200),
    lastActiveAt: hoursAgo(3),
  },
  {
    id: 's-3',
    ...describeUserAgent('Mozilla/5.0 (X11; Linux x86_64; rv:121.0) Gecko/20100101 Firefox/121.0'),
    signedInAt: hoursAgo(500),
    lastActiveAt: hoursAgo(72),
  },
];

function Interactive() {
  const [sessions, setSessions] = useState(SAMPLE);
  return (
    <SessionList
      sessions={sessions}
      onSignOut={async (id) => {
        await wait(600);
        setSessions((all) => all.filter((s) => s.id !== id));
      }}
      onSignOutOthers={async (ids) => {
        await wait(600);
        setSessions((all) => all.filter((s) => !ids.includes(s.id)));
      }}
      footnote="Apps already open on a device you sign out can stay signed in for a few minutes."
    />
  );
}

export default {
  id: 'session-list',
  title: 'SessionList',
  summary: 'Where the person is signed in, with sign-out for one session or every other session.',
  sections: {
    purpose: {
      summary:
        'People need to see every browser and device signed in to their account and sign out the ones they do not recognise. SessionList shows one row per session with the device, when it signed in and was last active, marks the current device, and signs out one session or every other session behind a confirmation. Needed by: Rook ID (security settings).',
      useWhen: [
        'An account security page listing active sign-ins.',
        'An admin view of one person’s sessions.',
      ],
      avoidWhen: [
        'Listing known devices that are not sessions (trusted or blocked hardware): use DeviceList.',
        'Apps that hold a grant to the account: use ConnectedAppsList.',
      ],
    },
    designPrinciples: {
      items: [
        'Name sessions the way people recognise them: "Chrome on macOS", not a token id.',
        'The current device is always marked, and signing it out says so in the confirmation.',
        'Signing out is recoverable (the device can sign in again), so it uses the warning tone, not danger.',
        'Data and callbacks in: the component never fetches or decides what a session is.',
      ],
    },
    apiReference: { source: 'typescript' },
    typedProperties: {
      source: 'typescript',
      notes: [
        '`sessions` is `{ id, label, kind?, detail?, hint?, signedInAt?, lastActiveAt?, current? }[]`.',
        '`describeUserAgent(ua)` returns `{ label, browser, os, kind }`; spread it into an item.',
        'Migrating from the app copy (Rook ID `SessionManagementCard`): fetch `/sessions` in the app and map `user_agent` → `describeUserAgent(...)`, `signed_in_at` → `signedInAt`, `last_active_at` → `lastActiveAt`, `is_current` → `current`. The delete calls become `onSignOut(id)` and `onSignOutOthers(ids)`; return their promises. Toasts are replaced by the built-in announcement; the loading spinner and "Try again" become `state` and `onRetry`. The old `describeDevice` is `describeUserAgent`.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'state',
          values: ['ready', 'loading', 'error'],
          description: 'What the card body shows. An empty `sessions` list shows the empty state.',
        },
        {
          prop: 'confirm',
          values: ['true', 'false'],
          description:
            'Built-in confirmation, or call straight through when the product confirms itself.',
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
          name: 'Current session',
          attribute: 'data-current',
          description: 'On the row of the session in use; it shows a "This device" badge.',
        },
        {
          name: 'Pending',
          attribute: 'aria-busy (dialog)',
          description: 'A returned promise is running: the confirm button shows a spinner.',
        },
        {
          name: 'Error',
          attribute: 'role="alert" (dialog)',
          description: 'The callback rejected: the message shows in the dialog and it stays open.',
        },
      ],
    },
    interaction: {
      items: [
        'A row’s Sign out button opens a confirmation naming the session; confirming calls `onSignOut(id)`.',
        '"Sign out other devices" appears when `onSignOutOthers` is set and there are other sessions; it passes every non-current id.',
        'After success the result is announced and focus returns to the button that opened the dialog, or to the next Sign out button when that row is gone.',
        'With `confirm={false}` the buttons call the callbacks directly.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab / Shift+Tab', action: 'Moves between the Sign out buttons.' },
        { keys: 'Enter / Space', action: 'Opens the confirmation for that session.' },
        {
          keys: 'Escape',
          action: 'Closes the confirmation without signing out (not while pending).',
        },
      ],
    },
    accessibility: {
      role: 'region (section) containing a list',
      name: 'The card title; the list is named "Signed-in sessions".',
      items: [
        'Each Sign out button is named with its session ("Sign out Safari on iPhone").',
        'Times are `<time>` elements with the exact date in `datetime`.',
        'Results are announced through a polite status region.',
        'The confirmation is an alertdialog with focus starting on Cancel.',
      ],
    },
    responsive: {
      items: [
        'Below 480px the Sign out button moves under the session details and fills the row.',
        'Long device names and locations wrap instead of scrolling sideways.',
        'Buttons are at least 44px tall on touch screens.',
      ],
    },
    theme: {
      items: [
        'Rows on the subtle background and border tokens; details in the muted text colour.',
        'The "This device" badge uses the success tone.',
        'In forced-colors mode rows and icons get system-colour borders.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Sign out one or every other session',
          stack: true,
          render: () => <Interactive />,
        },
        {
          title: 'Loading',
          stack: true,
          render: () => <SessionList sessions={[]} state="loading" />,
        },
        {
          title: 'Error with retry',
          stack: true,
          render: () => <SessionList sessions={[]} state="error" onRetry={() => undefined} />,
        },
      ],
    },
    doDont: {
      do: [
        'Mark the current session so people do not sign themselves out by accident.',
        'Return the request’s promise from `onSignOut` so failures show in the dialog.',
        'Say in `footnote` how long already-open apps stay signed in, if that is true for the product.',
      ],
      dont: [
        'Do not show raw user-agent strings as the label; put them in `hint`.',
        'Do not use the danger tone: signing out can be undone by signing in again.',
      ],
    },
    related: {
      items: [
        { id: 'device-list', reason: 'Known devices, rather than live sessions.' },
        { id: 'connected-apps-list', reason: 'Apps holding access to the account.' },
        { id: 'revoke-access-flow', reason: 'A richer revoke dialog, with `confirm={false}`.' },
        { id: 'section-card', reason: 'The card it is built on.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['button', 'badge', 'relative-time', 'confirm-dialog', 'section-card'],
    },
    testing: {
      unit: [
        'Lists sessions with the current badge, detail and relative times; axe passes.',
        'Confirms before signing one out, announces the result and returns focus.',
        'Current-session copy; a rejected callback shows the error and keeps the dialog open.',
        'Signs out every other session with their ids.',
        'Calls straight through with confirm={false}.',
        'Loading, empty and error states with retry.',
        'Translated messages and footnote.',
        'describeUserAgent labels and device kinds.',
        'Renders on the server.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
