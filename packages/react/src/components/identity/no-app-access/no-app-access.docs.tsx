import { NoAppAccess } from '@rookcom/monarch-react/shell';
import type { ComponentDocs } from '../../../internal/component-docs.js';

function Basic() {
  return (
    <NoAppAccess
      appName="Example Desk"
      workspaceName="Acme"
      headingLevel="h2"
      onRequestAccess={() => new Promise((resolve) => setTimeout(resolve, 800))}
      homeHref="#workspace-home"
      onSwitchWorkspace={() => {}}
      contactHref="mailto:admin@example.com"
    />
  );
}

function Minimal() {
  return <NoAppAccess appName="Example Wiki" headingLevel="h2" homeHref="#workspace-home" />;
}

export default {
  id: 'no-app-access',
  title: 'NoAppAccess',
  summary:
    'The “you don’t have access to this app” screen: who can fix it, request access, contact an admin and switch workspace.',
  sections: {
    purpose: {
      summary:
        'A person can be a member of a Rook workspace without a grant for every app. NoAppAccess is that screen, built on ErrorPage, with the actions the person can actually take.',
      useWhen: [
        'In place of the app’s pages when Rook ID says this member has no grant for the app.',
        'Inside an embedded page (App Settings) for the same case; it still counts as an answer for the ready handshake.',
      ],
      avoidWhen: [
        'A missing permission inside the app (a page or an action): show an ErrorState or disable the action with a reason.',
        'Not signed in, or not a member of the workspace: those are sign-in and workspace flows.',
      ],
    },
    designPrinciples: {
      items: [
        'Say who can fix it: the person cannot grant themselves access, so the text names the admins (and the workspace).',
        'Always a way forward: request access, contact an admin, back to the workspace or another workspace.',
        'The grant check stays in the app; the screen only shows the outcome and runs the callbacks it is given.',
        'Calm, not alarming: a lock and plain words, no error code.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Forwards its ref to the root element (`section`, or `main` with `fullScreen`). Built on ErrorPage (`kind="forbidden"`) from `@rookcom/monarch-react`.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'With `onRequestAccess` returning a promise, the button shows pending, then "Access requested"; a rejection shows an alert and allows another try. `requestState` controls it (e.g. `sent` when a request is already open).',
        'Switch workspace is `onSwitchWorkspace`, `switchWorkspaceHref`, or your own control in `workspaceSwitcher` (such as WorkspaceSwitcher).',
      ],
    },
    variants: {
      items: [
        {
          prop: 'fullScreen',
          values: ['false', 'true'],
          description: 'Inside the app shell (default), or filling the viewport without one.',
        },
        {
          prop: 'headingLevel',
          values: ['h1', 'h2'],
          description: 'h1 when the screen replaces the page (default).',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Idle',
          attribute: 'data-request="idle"',
          description: 'Request access is offered.',
        },
        {
          name: 'Pending',
          attribute: 'data-request="pending"',
          description: 'The button is busy (`aria-busy`) and ignores further clicks.',
        },
        {
          name: 'Sent',
          attribute: 'data-request="sent"',
          description:
            'The button reads "Access requested" and is disabled; a status says what happens next.',
        },
        {
          name: 'Error',
          attribute: 'data-request="error"',
          description: 'An alert says the request failed; the button works again.',
        },
      ],
    },
    interaction: {
      items: [
        'Request access runs `onRequestAccess` once per attempt.',
        'Back to workspace and switch links render with `linkAs` (your router link) when given.',
        'Contact an admin is a plain link (a mailto: or a page listing the admins).',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Moves through the actions and the contact link.' },
        { keys: 'Enter / Space', action: 'Activates the focused action.' },
      ],
    },
    accessibility: {
      role: 'region (section labelled by its heading), or main with fullScreen',
      name: 'The heading, "You don’t have access to <app>".',
      items: [
        'The lock is decorative (`aria-hidden`), fixing the products’ unhidden icon.',
        'The sent message is in a status region, and a failure is an alert, so both are announced.',
        '`focusOnMount` moves focus to the heading after a client-side navigation.',
        'Security: the screen shows no data from the app and does not decide access; render it only after the app’s own grant check, and enforce access on the server too.',
      ],
    },
    responsive: {
      items: ['Centred column; the actions wrap onto several lines at 320px.'],
    },
    theme: {
      items: [
        'ErrorPage tokens; the status uses the success text colour and the error the danger text colour.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Every action',
          stack: true,
          render: () => <Basic />,
        },
        {
          title: 'Explanation and a way back',
          stack: true,
          render: () => <Minimal />,
        },
      ],
    },
    doDont: {
      do: [
        'Name the app the way the rail and Rook ID do.',
        'Offer request access only when the product can actually deliver the request to the admins.',
        'Keep the rail on screen so the person can switch apps.',
      ],
      dont: [
        'Do not show the app’s data or navigation behind the screen.',
        'Do not use it for a missing permission inside the app.',
      ],
    },
    related: {
      items: [
        { id: 'error-page', reason: 'The page-level error it is built on.' },
        { id: 'workspace-switcher', reason: 'Can fill the switch workspace slot.' },
        { id: 'error-state', reason: 'A failed region inside a page.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['button', 'error-page'],
    },
    testing: {
      unit: [
        'Heading, explanation with the workspace, forbidden ErrorPage root, decorative lock, no actions by default.',
        'Request access: pending, single call, then sent (status) and disabled.',
        'A failed request is an alert and can be retried; a synchronous request counts as sent; controlled state.',
        'Back to workspace, switch workspace and contact links; switch link, custom switcher slot and router link.',
        'Focus on mount, h2, custom text and full screen; translation.',
      ],
      axe: true,
      ssr: true,
      browser: [
        'Visual baselines: /__gallery/no-app-access',
        'Cross-browser axe: /components/no-app-access',
      ],
    },
  },
} satisfies ComponentDocs;
