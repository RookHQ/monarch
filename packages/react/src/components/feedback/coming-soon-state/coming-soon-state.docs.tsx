import { Button, ComingSoonState } from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';

export default {
  id: 'coming-soon-state',
  title: 'ComingSoonState',
  summary:
    'Placeholder for a planned feature: "Coming soon" heading, what it will do, what is planned.',
  sections: {
    purpose: {
      summary:
        'Fills a tab, card or page whose feature is planned but not available, so people learn what is coming instead of meeting a blank screen or a dead link. Built on EmptyState. Needed by: Pulse (planned monitoring tabs), Links (integrations that are not ready yet).',
      useWhen: [
        'A navigation item or tab exists but its feature has not shipped.',
        'A settings section is planned and you want to say what it will contain.',
      ],
      avoidWhen: [
        'The feature exists but has no data yet: use EmptyState.',
        'The feature is locked behind a plan or permission: say so with EmptyState and the upgrade or request action.',
        'A single card in a list of available options: use a Badge on that card.',
      ],
    },
    designPrinciples: {
      items: [
        'Say what is coming, not just that something is: a feature title, one sentence and a short list.',
        'No promises the product cannot keep: dates go in `tip` only when they are real.',
        'Calm, informative tone: the badge uses the info colour, not the accent or a warning.',
        'The badge is part of the heading’s accessible name ("Coming soon: Status pages"), so it is not lost to screen readers.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        '`ComingSoonState` forwards its ref to the EmptyState root and accepts EmptyState props (`icon`, `actions`, `tip`, `size`, `layout`, `headingLevel`, …) except `kind` and the clear-filters props.',
        '`COMING_SOON_STATE_MESSAGES` holds the English defaults for `messages`.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Without `title`, the heading is `messages.title` ("Coming soon") and no badge is shown.',
        'The default icon is a clock; pass `icon={null}` to hide it or any icon to replace it.',
        'Migrating from the app copy (Pulse `components/pulse/ComingSoon`): `icon={Icon}` → `icon={<Icon />}`, `title` and `description` are unchanged, `bullets` → `highlights`. The card border comes from the surrounding Card instead of `card-modern`.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'size',
          values: ['inline', 'page', 'compact'],
          description: 'From EmptyState: inside a card, a whole page, or a tight panel.',
        },
        {
          prop: 'layout',
          values: ['stack', 'line'],
          description: 'From EmptyState: centred column, or a single left-aligned row.',
        },
        {
          prop: 'badge',
          values: ['true', 'false'],
          description: 'Shows the "Coming soon" badge above a feature title. Default `true`.',
        },
      ],
    },
    states: {
      items: [],
      notes: ['Static content. EmptyState’s `data-size` and `data-layout` describe the layout.'],
    },
    interaction: {
      items: ['Only the actions you pass (for example "Notify me") are interactive.'],
    },
    keyboard: {
      items: [],
      notes: ['Not interactive itself; any actions you pass are ordinary buttons or links.'],
    },
    accessibility: {
      role: 'none (a heading, a paragraph and a list)',
      name: 'The heading: "Coming soon", or the badge plus the feature title.',
      items: [
        'The heading level is set with `headingLevel` to fit the page outline.',
        'The icon is decorative and `aria-hidden`.',
        'The badge text is inside the heading with a visually hidden colon, so it reads "Coming soon: Status pages".',
        'Highlights are a real list; name it with `highlightsLabel` when there are several lists on the page.',
      ],
    },
    responsive: {
      items: [
        'Text and highlights wrap; nothing scrolls sideways at 390px.',
        'Actions wrap onto more lines on narrow screens.',
      ],
    },
    theme: {
      items: [
        'The badge uses the info subtle, border and strong tokens; highlight bullets use the accent solid token.',
        'Forced colours: the badge and bullets use CanvasText.',
        'Nothing animates.',
      ],
    },
    examples: {
      items: [
        {
          title: 'A planned tab',
          stack: true,
          render: () => (
            <ComingSoonState
              title="Status pages"
              description="Publish a page that shows your uptime and incident history."
              highlights={[
                'A public page on your own domain',
                'Incident updates by email',
                'Ninety days of uptime history',
              ]}
              actions={<Button>Notify me</Button>}
            />
          ),
        },
        {
          title: 'Just the message',
          render: () => (
            <ComingSoonState
              size="compact"
              description="Exports to spreadsheets will be available here."
            />
          ),
        },
        {
          title: 'One line in a settings list',
          stack: true,
          render: () => (
            <ComingSoonState
              layout="line"
              size="compact"
              title="Automation"
              description="Trigger actions in other tools when something changes."
            />
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Name the feature and say what it will do in one sentence.',
        'Offer a real action (notify, read the plan) only when the product supports it.',
        'Remove the state as soon as the feature ships.',
      ],
      dont: [
        'Do not promise a date you cannot keep.',
        'Do not use it for features that are paywalled or permission-locked.',
        'Do not fill whole products with it; hide navigation to features that are far away.',
      ],
    },
    related: {
      items: [
        { id: 'empty-state', reason: 'The feature exists but there is nothing to show yet.' },
        { id: 'badge', reason: 'Marking a single option as not ready inside a list.' },
        { id: 'callout-banner', reason: 'A page-level notice about upcoming changes.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['empty-state'],
      notes: [
        'The badge uses `--mn-color-info-subtle`, `--mn-color-info-border` and `--mn-color-info-strong`.',
      ],
    },
    testing: {
      unit: [
        'Defaults to a "Coming soon" heading with a decorative icon; no axe violations.',
        'A feature title puts the badge into the heading name.',
        'The badge can be hidden; strings can be translated.',
        'Highlights render as a named list; actions are reachable and work.',
        'Custom or no icon; ref forwarding; renders on the server.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
