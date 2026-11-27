import { ImpersonationBanner } from '@rookcom/monarch-react/patterns';
import type { ComponentDocs } from '../../../internal/component-docs.js';
// The scenario of the primary example, shared with iOS and Android (CROSS-PLATFORM.md section 8).
import fixture from './impersonation-banner.fixture.json';

const wait = (ms: number) => new Promise<void>((resolve) => window.setTimeout(resolve, ms));
const minutesAgo = (n: number) => new Date(Date.now() - n * 60_000);

export default {
  id: 'impersonation-banner',
  title: 'ImpersonationBanner',
  summary: 'A persistent “You are viewing as … · Stop” bar while acting as another person.',
  sections: {
    purpose: {
      summary:
        'When support or an admin views the product as another person, it must be impossible to forget: anything they change happens in that person’s account. ImpersonationBanner is a high-contrast bar across the top of every page that names the person, shows when and why the session started, and has one way out: Stop. Needed by: Rook ID admin tools and any app that adds a support “view as” mode; no Rook app has this mode yet.',
      useWhen: [
        'An admin or support person is signed in as, or viewing as, another person.',
        'A preview of the product with another role’s permissions, when actions are real.',
      ],
      avoidWhen: [
        'A read-only preview with no real actions: a CalloutBanner is enough.',
        'Ordinary notices such as a trial ending: use CalloutBanner with `placement="page"`.',
      ],
    },
    designPrinciples: {
      items: [
        'Unmissable: inverse colours and a warning edge, different from every other banner.',
        'Not dismissible: the only way to remove it is to stop viewing as the person.',
        'Name the person, so the operator always knows whose account they are in.',
        'Keep the audit context visible: start time and reason.',
      ],
    },
    apiReference: { source: 'typescript' },
    typedProperties: {
      source: 'typescript',
      notes: [
        '`onStop` may return a promise: the Stop button shows a spinner until it settles, and a rejection shows its message in the banner.',
        'No Rook app has its own copy of this banner, so there is nothing to migrate.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'sticky',
          values: ['true', 'false'],
          description: 'Stick to the top of the scroll container (default), or sit in normal flow.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Sticky',
          attribute: 'data-sticky',
          description: 'The banner sticks to the top while the page scrolls.',
        },
        {
          name: 'Stopping',
          attribute: 'data-stopping',
          description: 'The stop promise is running; the button is busy and ignores clicks.',
        },
        {
          name: 'Error',
          attribute: 'role="alert"',
          description: 'Stopping failed; the message shows under the title.',
        },
      ],
    },
    interaction: {
      items: [
        'Stop calls `onStop`; while its promise runs the button is busy and further clicks are ignored.',
        'A failed stop shows the error and the banner stays.',
        'The product removes the banner once the session has ended.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Reaches the Stop button.' },
        { keys: 'Enter / Space', action: 'Stops viewing as the person.' },
      ],
    },
    accessibility: {
      role: 'status',
      name: 'Its text: "You are viewing as {name}".',
      items: [
        'The banner is a polite live region, so the change is announced when it appears.',
        'The Stop button is named "Stop viewing as {name}", which starts with its visible label.',
        'The icon is decorative.',
      ],
    },
    responsive: {
      items: [
        'Below 480px the Stop button moves under the text and fills the width.',
        'Long names, emails and reasons wrap.',
        'The Stop button is at least 44px tall on touch screens.',
      ],
    },
    theme: {
      items: [
        'Inverse background and text with a warning-coloured bottom edge and icon, in both light and dark themes.',
        'The Stop button uses the warning variant.',
        'Forced-colors mode draws a system-colour border around the whole bar.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Viewing as a person',
          stack: true,
          render: () => (
            <ImpersonationBanner
              name={fixture.data.name}
              detail={fixture.data.detail}
              reason={fixture.data.reason}
              startedAt={minutesAgo(fixture.data.startedMinutesAgo)}
              sticky={false}
              onStop={() => wait(800)}
            />
          ),
        },
        {
          title: 'Name only',
          stack: true,
          render: () => (
            <ImpersonationBanner name="Priya Natarajan" sticky={false} onStop={() => undefined} />
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Render it at the very top of every page for the whole session, above the app shell.',
        'Record the reason when the session starts and show it here.',
      ],
      dont: [
        'Do not let people hide or dismiss it.',
        'Do not reuse it for other notices; its look must only ever mean “not your account”.',
      ],
    },
    related: {
      items: [
        { id: 'callout-banner', reason: 'Ordinary page-wide notices.' },
        { id: 'session-list', reason: 'Where people see who is signed in to their account.' },
      ],
    },
    dependencies: { packages: ['react'], components: ['button', 'relative-time'] },
    testing: {
      unit: [
        'Announces the person with detail, start time and reason; Stop is named; no dismiss; axe passes.',
        'Stops from the keyboard; pending state blocks a second stop.',
        'A rejected stop shows the error.',
        'Translated messages and non-sticky placement.',
        'Renders on the server.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
