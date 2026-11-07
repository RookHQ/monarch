import { useState } from 'react';
import { ConnectedAppsList, type ConnectedApp } from '@rookcom/monarch-react/patterns';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const wait = (ms: number) => new Promise<void>((resolve) => window.setTimeout(resolve, ms));
const daysAgo = (d: number) => new Date(Date.now() - d * 86_400_000);

const SAMPLE: ConnectedApp[] = [
  {
    id: 'app-1',
    name: 'Notes',
    domain: 'notes.example.com',
    verified: true,
    workspace: 'Design team',
    scopes: ['See your name and profile picture', 'See your email address'],
    connectedAt: daysAgo(40),
    lastUsedAt: daysAgo(1),
  },
  {
    id: 'app-2',
    name: 'Planner',
    domain: 'planner.example.org',
    scopes: ['See your name and profile picture'],
    connectedAt: daysAgo(200),
    lastUsedAt: daysAgo(90),
  },
  {
    id: 'app-3',
    name: 'Survey tool',
    suspended: true,
    connectedAt: daysAgo(12),
  },
];

function Revocable() {
  const [apps, setApps] = useState(SAMPLE);
  return (
    <ConnectedAppsList
      apps={apps}
      onRevoke={async (id) => {
        await wait(600);
        setApps((all) => all.filter((a) => a.id !== id));
      }}
    />
  );
}

export default {
  id: 'connected-apps-list',
  title: 'ConnectedAppsList',
  summary: 'Apps a person signed in to with their account, what they can see, and removing access.',
  sections: {
    purpose: {
      summary:
        'When people sign in to other apps with their account, they need one place to see which apps hold access, what each can see, and to take that access away. ConnectedAppsList shows each app’s logo, name, domain, review status, workspace, scopes and dates, with "Remove access" behind a confirmation. Needed by: Rook ID (account settings).',
      useWhen: [
        'An account page listing apps authorised through single sign-on or OAuth.',
        'An admin view of the apps a person has granted access to.',
      ],
      avoidWhen: [
        'Choosing which workspace apps a member may open: use AppAccessPicker.',
        'Listing API keys or OAuth clients a developer owns: use a DataTable with RevokeAccessFlow.',
      ],
    },
    designPrinciples: {
      items: [
        'Everything an app says about itself is untrusted: names render as text and unsafe logo URLs are dropped for the initial.',
        'Show what each app can access in plain words, so people can judge what to remove.',
        'Removing access is recoverable (people can sign in again), so it uses the warning tone.',
      ],
    },
    apiReference: { source: 'typescript' },
    typedProperties: {
      source: 'typescript',
      notes: [
        '`apps` is `{ id, name, logoSrc?, domain?, verified?, suspended?, workspace?, scopes?, connectedAt?, lastUsedAt? }[]`.',
        'Migrating from the app copy (Rook ID `ConnectedAppsCard`): fetch `/identity/connected-apps` in the app, map `logoUrl` → `logoSrc`, `hostOf(homepageUrl)` → `domain`, `workspaceName` → `workspace`, `scopes[].text` → `scopes`, `connectedAt`/`lastUsedAt` as they are. The DELETE call becomes `onRevoke(id)`; return its promise. When the identity service is off (the old `hidden` case) do not render the component.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'state',
          values: ['ready', 'loading', 'error'],
          description: 'What the card body shows. An empty `apps` list shows the empty state.',
        },
        {
          prop: 'confirm',
          values: ['true', 'false'],
          description: 'Built-in confirmation, or call `onRevoke` directly.',
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
          name: 'Suspended',
          attribute: 'data-suspended',
          description: 'A "Suspended" badge and a dimmed logo.',
        },
        {
          name: 'Pending and error',
          attribute: 'aria-busy, role="alert" (dialog)',
          description: 'While `onRevoke` runs, and when it rejects.',
        },
      ],
    },
    interaction: {
      items: [
        '"Remove access" opens a confirmation naming the app; confirming calls `onRevoke(id)`.',
        'After success the result is announced and focus returns to the opener, or to the next "Remove access" button when that row is gone.',
        'A logo that fails to load falls back to the app’s initial.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab / Shift+Tab', action: 'Moves between the "Remove access" buttons.' },
        { keys: 'Enter / Space', action: 'Opens the confirmation for that app.' },
        { keys: 'Escape', action: 'Closes the confirmation without removing (not while pending).' },
      ],
    },
    accessibility: {
      role: 'region (section) containing a list',
      name: 'The card title; the list is named "Connected apps" and each scope list "What <app> can access".',
      items: [
        'Each button is named with its app ("Remove access for Notes").',
        'Logos are decorative because the name is next to them.',
        'Results are announced through a polite status region.',
      ],
    },
    responsive: {
      items: [
        'Below 480px "Remove access" moves under the app details and fills the row; scopes lose their indent.',
        'Long names and domains wrap instead of scrolling sideways.',
        'Buttons are at least 44px tall on touch screens.',
      ],
    },
    theme: {
      items: [
        'Rows on the subtle background and border tokens; logo fallbacks use the data palette tokens.',
        'Verified uses the success badge tone, suspended the warning tone; the button is danger-subtle.',
        'In forced-colors mode rows get system-colour borders.',
      ],
    },
    examples: {
      items: [
        { title: 'Remove access', stack: true, render: () => <Revocable /> },
        { title: 'Empty', stack: true, render: () => <ConnectedAppsList apps={[]} /> },
        {
          title: 'Error with retry',
          stack: true,
          render: () => <ConnectedAppsList apps={[]} state="error" onRetry={() => undefined} />,
        },
      ],
    },
    doDont: {
      do: [
        'Describe scopes in plain words ("See your email address"), not scope ids.',
        'Pass `domain` derived from a validated https URL.',
        'Return the request’s promise from `onRevoke`.',
      ],
      dont: [
        'Do not trust app-provided logos or links; pass only what you have validated.',
        'Do not mark apps verified unless they have actually been reviewed.',
      ],
    },
    related: {
      items: [
        { id: 'session-list', reason: 'Browsers and devices signed in to the account.' },
        { id: 'app-access-picker', reason: 'Choosing which apps a workspace member may open.' },
        { id: 'revoke-access-flow', reason: 'A richer revoke dialog, with `confirm={false}`.' },
        { id: 'sign-in-with-rook-button', reason: 'How apps get connected in the first place.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['button', 'avatar', 'badge', 'relative-time', 'confirm-dialog', 'section-card'],
    },
    testing: {
      unit: [
        'Lists apps with badges, domain, times, workspace and scopes; drops unsafe logo URLs; axe passes.',
        'Removes access after confirming, announces it and returns focus.',
        'A rejected onRevoke keeps the dialog open with the error.',
        'confirm={false} calls straight through.',
        'Loading, empty and error states.',
        'Translated messages.',
        'Renders on the server.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
