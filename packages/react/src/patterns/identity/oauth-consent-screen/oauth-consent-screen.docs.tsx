import {
  OAuthConsentScreen,
  type OAuthConsentApp,
  type OAuthConsentScope,
} from '@rookcom/monarch-react/patterns';
import type { ComponentDocs } from '../../../internal/component-docs.js';
// The scenario of the primary example, shared with iOS and Android (CROSS-PLATFORM.md section 8).
import fixture from './oauth-consent-screen.fixture.json';

const wait = (ms: number) => new Promise<void>((resolve) => window.setTimeout(resolve, ms));

const APP: OAuthConsentApp = fixture.data.app;

const SCOPE_IDS = ['openid profile', 'email', 'files.write'] as const;
const SCOPES: OAuthConsentScope[] = fixture.data.scopes.map((scope, i) => ({
  id: SCOPE_IDS[i] ?? scope.label,
  label: scope.label,
  ...('description' in scope && { description: scope.description }),
  ...(scope.kind === 'warning' && { sensitive: true }),
}));

const ACCOUNT = fixture.data.account;

export default {
  id: 'oauth-consent-screen',
  title: 'OAuthConsentScreen',
  summary: 'The “App wants to access your account” screen with scopes and Allow / Cancel.',
  sections: {
    purpose: {
      summary:
        'When a third-party app asks to use someone’s account, they must be able to tell who is asking, which account is being shared, and exactly what the app will be able to do, before choosing. OAuthConsentScreen shows the app (logo, name, domain, where they will return, verified or not), the signed-in account with a way to switch, the requested permissions in plain words with sensitive ones highlighted, extra steps through `children`, and equal-weight Cancel and Allow buttons. Needed by: Rook ID (`app/oauth2/interaction/[uid]/page.tsx` and `app/oauth/authorize/page.tsx`).',
      useWhen: [
        'An OAuth or OpenID Connect authorization page.',
        'Granting an integration access to a workspace.',
      ],
      avoidWhen: [
        'First-party sign-in between your own apps: skip consent.',
        'Reviewing apps that already have access: use ConnectedAppsList.',
      ],
    },
    designPrinciples: {
      items: [
        'Plain words, not scope strings: “See your email address”, never `email`.',
        'Cancel and Allow have equal weight; nothing nudges people to allow.',
        'Unverified apps are clearly marked, and sensitive permissions stand out.',
        'Logos and links from app registrations are untrusted: unsafe URLs are dropped.',
      ],
    },
    apiReference: { source: 'typescript' },
    typedProperties: {
      source: 'typescript',
      notes: [
        '`app` is `{ name, logoSrc?, domain?, redirectHost?, verified?, testing?, privacyUrl?, termsUrl? }`.',
        '`scopes` and `heldBackScopes` are `{ id, label, description?, sensitive? }[]`.',
        'Migrating from the app copy (Rook ID `app/oauth2/interaction/[uid]/page.tsx`): `d.app` maps to `app` (`logoUrl` → `logoSrc`), `d.account` to `account`, `d.scopes` / `d.heldBack` (`{ scope, text }`) to `scopes` / `heldBackScopes` (`{ id: scope, label: text }`), `onAllow` / `onCancel` to `onAllow` / `onDeny`, the “Use another account” link handler to `onSwitchAccount`. The workspace radio group and email PIN step move into `children` with `allowDisabled`; the blocked notice becomes `blocked`.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'blocked',
          values: ['unset', 'ReactNode'],
          description: 'The person cannot grant access: only Cancel shows, with the reason.',
        },
        {
          prop: 'headingLevel',
          values: ['h1', 'h2'],
          description: '`h1` (default) when the screen is its own page.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Pending',
          attribute: 'aria-busy',
          description:
            'Allow or Cancel is running; both buttons are disabled and the pressed one spins.',
        },
        {
          name: 'Sensitive scope',
          attribute: 'data-sensitive',
          description: 'Bold with a warning-coloured mark and hidden “(sensitive)” text.',
        },
        {
          name: 'Held back',
          attribute: 'data-held',
          description:
            'Permissions not granted until the app is verified, shown muted with a lock.',
        },
        {
          name: 'Blocked',
          attribute: 'data-blocked',
          description: 'Only Cancel and the reason show.',
        },
        {
          name: 'Error',
          attribute: 'role="alert"',
          description: 'A rejected callback’s message.',
        },
      ],
    },
    interaction: {
      items: [
        'Allow calls `onAllow`, Cancel calls `onDeny`; the product redirects afterwards.',
        '“Use another account” calls `onSwitchAccount`.',
        'Extra steps in `children` can keep Allow disabled with `allowDisabled`.',
        'Policy links open in a new tab and say so.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Tab',
          action: 'Moves through the account switch, extra steps, Cancel, Allow and links.',
        },
        { keys: 'Enter / Space', action: 'Activates the focused button.' },
      ],
    },
    accessibility: {
      role: 'region (section)',
      name: 'The heading, "{App} wants to access your account".',
      items: [
        'The account card reads “Signed in as {name} {email}”.',
        'Logos are decorative because the name is the heading.',
        'Permissions are a list; check and lock marks are decorative.',
      ],
    },
    responsive: {
      items: [
        'One column at any width; long names, domains and permissions wrap.',
        'The two buttons share the width equally; all buttons are 44px tall on touch screens.',
      ],
    },
    theme: {
      items: [
        'The account card on the subtle background.',
        'Granted permissions use a success mark, sensitive ones a warning mark.',
        'Forced-colors mode draws marks in CanvasText and links in LinkText.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Verified app with a sensitive permission',
          render: () => (
            <OAuthConsentScreen
              headingLevel="h2"
              app={APP}
              account={ACCOUNT}
              scopes={SCOPES}
              onAllow={() => wait(800)}
              onDeny={() => wait(400)}
              onSwitchAccount={() => undefined}
              footnote={fixture.data.footnote}
            />
          ),
        },
        {
          title: 'Unverified app with held-back permissions',
          render: () => (
            <OAuthConsentScreen
              headingLevel="h2"
              app={{ name: 'Side Project', domain: 'side.example.org', verified: false }}
              account={ACCOUNT}
              scopes={SCOPES.slice(0, 2)}
              heldBackScopes={SCOPES.slice(2)}
              onAllow={() => wait(800)}
              onDeny={() => wait(400)}
            />
          ),
        },
        {
          title: 'Blocked',
          render: () => (
            <OAuthConsentScreen
              headingLevel="h2"
              app={{ name: 'Example Calendar', domain: 'calendar.example.com', verified: true }}
              account={ACCOUNT}
              scopes={SCOPES}
              blocked="Your workspace admin hasn’t allowed this app."
              onAllow={() => undefined}
              onDeny={() => wait(400)}
            />
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Write each permission as what the app will be able to do.',
        'Show where the person will be sent back to when it differs from the app’s domain.',
      ],
      dont: [
        'Do not make Allow more prominent than Cancel through size or placement tricks.',
        'Do not show raw scope strings to people.',
      ],
    },
    related: {
      items: [
        { id: 'connected-apps-list', reason: 'Apps that already have access.' },
        { id: 'sign-in-form', reason: 'Signing in before consenting.' },
        { id: 'auth-page', reason: 'The page layout.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['button', 'badge', 'inline-notice', 'avatar'],
    },
    testing: {
      unit: [
        'App, account, scopes and only safe links; axe passes.',
        'Allows with the keyboard; both buttons are disabled while pending.',
        'Cancel shows a rejection.',
        'Unverified warning, testing notice, held-back scopes, unsafe logo dropped, account switch, blocked state and translated messages.',
        'Renders on the server without unsafe URLs.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
