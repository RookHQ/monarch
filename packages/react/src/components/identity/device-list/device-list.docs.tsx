import { useState } from 'react';
import { DeviceList, type DeviceListItem } from '@rookcom/monarch-react/patterns';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const wait = (ms: number) => new Promise<void>((resolve) => window.setTimeout(resolve, ms));
const hoursAgo = (h: number) => new Date(Date.now() - h * 3_600_000);

const SAMPLE: DeviceListItem[] = [
  {
    id: 'd-1',
    name: 'Work laptop',
    os: 'macOS',
    browser: 'Chrome',
    location: 'Lisbon, Portugal',
    lastUsedAt: new Date(),
    firstSeenAt: hoursAgo(24 * 90),
    current: true,
  },
  {
    id: 'd-2',
    name: 'Phone',
    kind: 'mobile',
    os: 'Android',
    browser: 'Chrome',
    lastUsedAt: hoursAgo(5),
    firstSeenAt: hoursAgo(24 * 30),
  },
  {
    id: 'd-3',
    name: 'Shared tablet',
    kind: 'tablet',
    os: 'iPadOS',
    location: '198.51.100.24',
    lastUsedAt: hoursAgo(24 * 12),
    blocked: true,
  },
];

function Removable() {
  const [devices, setDevices] = useState(SAMPLE);
  return (
    <DeviceList
      devices={devices}
      onRemove={async (id) => {
        await wait(600);
        setDevices((all) => all.filter((d) => d.id !== id));
      }}
    />
  );
}

export default {
  id: 'device-list',
  title: 'DeviceList',
  summary: 'The devices that have accessed an account, with when each was last used.',
  sections: {
    purpose: {
      summary:
        'People and admins need to see which phones, laptops and tablets have accessed an account, spot one they do not recognise and remove it. DeviceList shows each device’s name, system and browser, location, first-seen and last-used times, and blocked or current badges, with an optional Remove action. Needed by: Rook ID (security settings).',
      useWhen: [
        'A security page listing known devices.',
        'An admin view of the devices a person has used.',
      ],
      avoidWhen: [
        'Listing live sign-ins with sign-out: use SessionList.',
        'Choosing a camera or microphone: use DevicePicker.',
      ],
    },
    designPrinciples: {
      items: [
        'Show what people recognise first (the name), then the system, browser and where it was used.',
        'The current device cannot be removed from here, so it never shows a Remove button.',
        'Removing is recoverable (the device can sign in again), so it uses the warning tone.',
        'Data and callbacks in; block, rename and other product actions go in `renderActions`.',
      ],
    },
    apiReference: { source: 'typescript' },
    typedProperties: {
      source: 'typescript',
      notes: [
        '`devices` is `{ id, name, kind?, os?, browser?, location?, lastUsedAt?, firstSeenAt?, blocked?, current? }[]`.',
        'Migrating from the app copy (Rook ID `DeviceManagementCard`): fetch `/users/me/devices` in the app and map `device_name` → `name`, `device_type` containing "mobile" or "phone" → `kind: "mobile"`, `ip_address` → `location`, `last_used_at` → `lastUsedAt`, `first_seen_at` → `firstSeenAt`, `is_blocked` → `blocked`. The spinner and empty box become `state` and the built-in empty state.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'state',
          values: ['ready', 'loading', 'error'],
          description: 'What the card body shows. An empty `devices` list shows the empty state.',
        },
        {
          prop: 'confirm',
          values: ['true', 'false'],
          description: 'Built-in confirmation, or call `onRemove` directly.',
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
          name: 'Blocked',
          attribute: 'data-blocked',
          description: 'A "Blocked" badge and a danger-tinted icon.',
        },
        {
          name: 'Current',
          attribute: 'data-current',
          description: 'A "This device" badge; no Remove button.',
        },
        {
          name: 'Pending and error',
          attribute: 'aria-busy, role="alert" (dialog)',
          description: 'While `onRemove` runs, and when it rejects.',
        },
      ],
    },
    interaction: {
      items: [
        'Remove opens a confirmation naming the device; confirming calls `onRemove(id)`.',
        'After success the result is announced and focus returns to the opener, or to the next Remove button when that row is gone.',
        '`renderActions(device)` adds product controls before Remove.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab / Shift+Tab', action: 'Moves between row actions.' },
        { keys: 'Enter / Space', action: 'Opens the confirmation for that device.' },
        { keys: 'Escape', action: 'Closes the confirmation without removing (not while pending).' },
      ],
    },
    accessibility: {
      role: 'region (section) containing a list',
      name: 'The card title; the list is named "Devices".',
      items: [
        'Each Remove button is named with its device ("Remove Phone").',
        'Device icons are decorative; the kind is not the only signal (the name and system are text).',
        'Results are announced through a polite status region.',
      ],
    },
    responsive: {
      items: [
        'Below 480px the actions move under the details.',
        'Long names, systems and addresses wrap instead of scrolling sideways.',
        'Buttons are at least 44px tall on touch screens.',
      ],
    },
    theme: {
      items: [
        'Rows on the subtle background and border tokens; blocked devices tint the icon with the danger tokens.',
        'Badges use the success (current) and danger (blocked) tones.',
        'In forced-colors mode rows and icons get system-colour borders.',
      ],
    },
    examples: {
      items: [
        { title: 'Remove a device', stack: true, render: () => <Removable /> },
        {
          title: 'Read only',
          stack: true,
          render: () => <DeviceList devices={SAMPLE} />,
        },
        { title: 'Empty', stack: true, render: () => <DeviceList devices={[]} /> },
      ],
    },
    doDont: {
      do: [
        'Pass `current` so people do not remove the device they are using.',
        'Give `lastUsedAt`: it is how people tell an old device from an active one.',
      ],
      dont: [
        'Do not show full IP addresses to people other than the account owner or an admin.',
        'Do not use it for live sessions; SessionList has sign-out semantics.',
      ],
    },
    related: {
      items: [
        { id: 'session-list', reason: 'Live sign-ins with sign-out.' },
        { id: 'security-alert-card', reason: 'Alerts such as a sign-in from a new device.' },
        { id: 'revoke-access-flow', reason: 'A richer revoke dialog, with `confirm={false}`.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['button', 'badge', 'relative-time', 'confirm-dialog', 'section-card'],
    },
    testing: {
      unit: [
        'Lists devices with system, location, times and badges; axe passes.',
        'Removes after confirming (keyboard), announces it and returns focus; no Remove on the current device.',
        'Escape cancels without removing.',
        'confirm={false} calls straight through; renderActions adds controls.',
        'Loading, empty and error states.',
        'Translated messages.',
        'Renders on the server.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
