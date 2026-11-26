import { AnnouncementBar } from '@rookcom/monarch-react/marketing';
import type { ComponentDocs } from '../../../internal/component-docs.js';

export default {
  id: 'announcement-bar',
  title: 'AnnouncementBar',
  summary: 'A slim, dismissible bar across the top of a page for one timely announcement.',
  sections: {
    purpose: {
      summary:
        'Puts one short, timely message (a release, a maintenance window, a policy change) above a page, with a link and a close button whose dismissal the product remembers. Needed by: the product marketing sites that publish a changelog (Dots, Pulse, Ranker, Signal, Recruit) to point at new releases. No app has one yet; the nearest copy, Rook ID `src/components/SecurityBanner.tsx`, is an inline shadcn/ui Alert, not a page-wide bar.',
      useWhen: [
        'One site-wide announcement with a clear end date.',
        'Pointing readers at a new changelog entry or launch page.',
      ],
      avoidWhen: [
        'Errors or status inside a form or panel: use Alert.',
        'A response to something the person just did: use a toast.',
        'Several messages at once: show only the most important one.',
      ],
    },
    designPrinciples: {
      items: [
        'One sentence and at most one link; anything longer belongs on its own page.',
        'Dismissal is the reader’s choice and must stick: the bar reports it through `onDismiss`, and the product stores it.',
        'The bar stores nothing itself, so it is safe to render on the server.',
        'Tone follows meaning: `warning` only for something that affects the reader (such as downtime).',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'The ref and native attributes go to the root `<div role="region">`.',
        'While dismissed the component renders nothing.',
        '`defaultAnnouncementBarMessages` holds the English defaults.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Persisting a dismissal: keep an announcement id, store it in `onDismiss` (cookie, account setting or local storage), and pass `dismissed` (or `defaultDismissed` from a cookie on the server) when it is stored.',
        'Migrating from a shadcn/ui `Alert` used as a banner (Rook ID `SecurityBanner`): `AlertDescription` text → `children`, the lucide icon → `icon`, Tailwind gradient classes → `tone`; add `dismissible={false}` if it must stay.',
        '`action.href` passes `isSafeUrl`; unsafe URLs are not rendered.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'tone',
          values: ['accent', 'neutral', 'inverse', 'info', 'warning'],
          description: 'Background and border. `inverse` is the highest emphasis.',
        },
        {
          prop: 'sticky',
          values: ['false', 'true'],
          description: 'Stays at the top while scrolling.',
        },
        { prop: 'dismissible', values: ['true', 'false'], description: 'Shows the close button.' },
      ],
    },
    states: {
      items: [
        { name: 'Tone', attribute: 'data-tone', description: 'The current tone.' },
        {
          name: 'Sticky',
          attribute: 'data-sticky',
          description: 'Pinned to the top of the viewport.',
        },
        {
          name: 'Dismissed',
          description: 'Nothing is rendered (controlled by `dismissed` or internal state).',
        },
      ],
    },
    interaction: {
      items: [
        'The link follows its URL; external links open a new tab.',
        'The close button hides the bar and calls `onDismiss` once.',
        'After dismissal focus moves to the page body; place the bar before the main navigation so the next Tab lands there.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Moves to the link, then the close button.' },
        { keys: 'Enter / Space', action: 'Follows the link or dismisses the bar.' },
      ],
    },
    accessibility: {
      role: 'region',
      name: '`messages.label` ("Announcement").',
      items: [
        'The bar is a landmark, so screen-reader users can find or skip it.',
        'It is not a live region: it is page content, not an interruption.',
        'The close button is named "Dismiss announcement"; new-tab links say so.',
        'The icon and arrow are decorative.',
      ],
    },
    responsive: {
      items: [
        'Centred on wide screens; starts at the inline-start edge and wraps below 480px.',
        'Link and close button are 44px tall on coarse pointers.',
      ],
    },
    theme: {
      items: [
        'Accent, info, warning, inverse and neutral tokens; works in light, dark and every product accent.',
        'Logical properties throughout; the arrow flips in RTL.',
        'Forced-colours mode keeps a CanvasText rule under the bar.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Release announcement',
          render: () => (
            <AnnouncementBar action={{ label: 'Read the release notes', href: '#changelog' }}>
              Version 2 is available.
            </AnnouncementBar>
          ),
        },
        {
          title: 'Scheduled maintenance',
          render: () => (
            <AnnouncementBar
              tone="warning"
              dismissible={false}
              messages={{ label: 'Maintenance notice' }}
            >
              Scheduled maintenance on Saturday, 02:00–03:00 UTC.
            </AnnouncementBar>
          ),
        },
        {
          title: 'Inverse, with a new-tab link',
          render: () => (
            <AnnouncementBar
              tone="inverse"
              messages={{ label: 'Event announcement' }}
              action={{ label: 'Event details', href: 'https://example.com', external: true }}
            >
              Join the product walkthrough next week.
            </AnnouncementBar>
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Keep it to one sentence and one link.',
        'Store the dismissal per announcement id so a new announcement shows again.',
        'Remove the bar when the announcement is no longer current.',
      ],
      dont: [
        'Do not stack several bars.',
        'Do not use it for errors about the reader’s own data; use Alert.',
        'Do not make it undismissable unless it affects the reader (such as downtime).',
      ],
    },
    related: {
      items: [
        { id: 'alert', reason: 'In-page messages about the current task.' },
        { id: 'changelog-feed', reason: 'Where the announcement link usually points.' },
        { id: 'marketing-site-shell', reason: 'The page shell the bar sits above.' },
      ],
    },
    dependencies: { packages: ['react'], components: ['button'] },
    testing: {
      unit: [
        'Named region with message, link and close button; axe clean.',
        'Tab to link and close; Enter dismisses (uncontrolled) and calls `onDismiss`.',
        'Controlled `dismissed`; `defaultDismissed` renders nothing.',
        'Unsafe links dropped; new-tab links announced; action slot.',
        'Tone, sticky, no close button, translations, ref forwarding; axe clean.',
        'Server rendering.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
