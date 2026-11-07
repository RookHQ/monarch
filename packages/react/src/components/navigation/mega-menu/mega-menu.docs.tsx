import { MegaMenu, type MegaMenuItem } from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';
import { CalendarIcon, GearIcon, GridIcon, UsersIcon } from '../../../internal/docs-demo.js';

const items: MegaMenuItem[] = [
  {
    id: 'product',
    label: 'Product',
    sections: [
      {
        heading: 'Plan',
        links: [
          {
            label: 'Projects',
            href: '#projects',
            description: 'Track work from idea to launch',
            icon: <GridIcon />,
          },
          {
            label: 'Calendar',
            href: '#calendar',
            description: 'Deadlines and milestones in one view',
            icon: <CalendarIcon />,
            badge: 'New',
          },
        ],
      },
      {
        heading: 'Collaborate',
        links: [
          {
            label: 'Teams',
            href: '#teams',
            description: 'Roles, access and ownership',
            icon: <UsersIcon />,
          },
          {
            label: 'Settings',
            href: '#settings',
            description: 'Workspace preferences',
            icon: <GearIcon />,
          },
        ],
      },
    ],
    featured: {
      title: 'What changed this month',
      description: 'A short tour of the newest features and fixes.',
      href: '#changelog',
      icon: <GridIcon />,
      badge: 'Updated',
    },
  },
  {
    id: 'company',
    label: 'Company',
    sections: [
      {
        heading: 'About',
        links: [
          { label: 'Who we are', href: '#about', description: 'The team and how we work' },
          {
            label: 'Status',
            href: 'https://status.example.com',
            description: 'Service health',
            external: true,
          },
        ],
      },
    ],
  },
  { id: 'support', label: 'Support', href: '#support' },
];

export default {
  id: 'mega-menu',
  title: 'MegaMenu',
  summary:
    'Site navigation with large drop-down panels of described links and a featured card; an accordion on small screens.',
  sections: {
    purpose: {
      summary:
        'The top navigation of a marketing or public site whose sections have many destinations: each entry opens a panel of grouped links with descriptions and an optional featured card. It replaces the hand-built bento Navbar copied into every Rook app, with keyboard, screen-reader and touch support. Needed by: App, Atlas, Base, Calendar, Chat, Developers, Dossier, Dots, Friends, GTM, Intellect, People, Pulse, Ranker, Recruit, Signal, Stride, Tickets (public sites).',
      useWhen: [
        'A public or marketing header with several sections of 3–10 links each.',
        'Links benefit from a one-line description or icon.',
      ],
      avoidWhen: [
        'In-app navigation: use the app shell rail and tabs.',
        'A list of actions on an object: use DropdownMenu.',
        'Fewer than three links per section: use plain header links.',
      ],
    },
    designPrinciples: {
      items: [
        'Disclosure, not a menu: entries are buttons with `aria-expanded` and panels hold real links, so screen readers use their normal link navigation.',
        'Hover helps, never hides: a mouse can open a panel by hovering, but click, tap and keyboard always work, and a click after hovering keeps it open.',
        'One open panel at a time; Escape, outside clicks and leaving with focus close it.',
        'Same data, two layouts: a row with floating panels from 64rem, an accordion behind a Menu button below it.',
        'No product content: entries, copy and links come from the product.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        '`MegaMenu` renders a `nav` (default name "Main"; pass `aria-label`) and forwards its ref to it.',
        'Each `MegaMenuItem` is a panel (`sections` and/or `featured`) or, with only `href`, a plain link.',
        '`MEGA_MENU_MESSAGES` holds the English defaults for `messages`.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Links: `{ label, href, description?, icon?, badge?, external?, current? }`. External links open in a new tab and say so to screen readers.',
        '`featured`: `{ title, href, description?, icon?, badge?, cta?, external? }`; `cta` defaults to `messages.explore`.',
        'Migrating from the app copy (`components/layout/Navbar.tsx` in each app): each `MegaConfig` becomes a `MegaMenuItem` with `id` and `label` (its `title`); `bento.main` and `bento.secondary` become `sections` (`heading` stays, `links[].name` → `label`, `path` → `href`, `desc` → `description`, `icon: Icon` → `icon: <Icon />`); `bento.hero` becomes `featured` (`desc` → `description`, `path` → `href`). The extra "Support" link becomes an item with only `href`. Keep the logo, identity bar and scroll shadow in the app’s header around `<MegaMenu linkAs={Link} />`.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'openOnHover',
          values: ['true', 'false'],
          description: 'Open panels on mouse hover in the wide layout. Default `true`.',
        },
        {
          prop: 'item kind',
          values: ['panel', 'link'],
          description:
            'An item with `sections` or `featured` opens a panel; an item with only `href` is a plain link.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Open',
          attribute: 'data-state="open"',
          description: 'On the item: its button has `aria-expanded="true"` and its panel shows.',
        },
        {
          name: 'Menu open',
          attribute: 'data-mobile-open',
          description: 'Small screens: the entries are shown under the Menu button.',
        },
        {
          name: 'Current',
          attribute: 'data-current',
          description: 'A link or entry for the current page; links get `aria-current="page"`.',
        },
      ],
    },
    interaction: {
      items: [
        'Click or tap an entry to open its panel; again to close it. Opening another entry closes the first.',
        'With a mouse in the wide layout, hovering opens a panel and leaving closes it after 120ms; a click pins it open.',
        'Following a link closes the panel (and the small-screen menu) and calls `onNavigate`.',
        'Outside clicks and focus leaving the navigation close the panel.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Tab / Shift+Tab',
          action: 'Moves through entries and, when open, the panel’s links in reading order.',
        },
        { keys: 'Enter / Space', action: 'Opens or closes the focused entry’s panel.' },
        { keys: 'Arrow Right / Arrow Left', action: 'Moves between top-level entries, wrapping.' },
        {
          keys: 'Home / End',
          action: 'First or last top-level entry; inside a panel, its first or last link.',
        },
        {
          keys: 'Arrow Down',
          action: 'On an entry: opens its panel and focuses the first link. In a panel: next link.',
        },
        { keys: 'Arrow Up', action: 'In a panel: previous link, then back to the entry.' },
        {
          keys: 'Escape',
          action:
            'Closes the open panel and returns focus to its entry, or closes the small-screen menu.',
        },
      ],
    },
    accessibility: {
      role: 'navigation with disclosure buttons (`aria-expanded`, `aria-controls`) and lists of links',
      name: '`aria-label` (default "Main"). Each link list is named by its section heading.',
      items: [
        'Follows the APG disclosure navigation pattern; no `menu` or `menuitem` roles.',
        'Closed panels use the `hidden` attribute, so they are out of the tab order and the accessibility tree.',
        'External links announce "(opens in a new tab)" from `messages.newTab`.',
        'Icons and the featured call to action are decorative; link text carries the meaning.',
        'Touch targets are at least 44px on coarse pointers and in the small-screen layout.',
      ],
    },
    responsive: {
      items: [
        'Below 64rem: a Menu button toggles a scrollable accordion; panels open inline under their entry.',
        'From 64rem: entries sit in a row and panels float under the bar, at most 56rem wide and never wider than the viewport.',
        'Columns wrap inside a panel when there is not enough room.',
      ],
    },
    theme: {
      items: [
        'Panels use the raised surface, subtle border and extra-large shadow tokens; featured cards use the subtle background.',
        'Icons and the call-to-action arrow use the accent strong token.',
        'Forced colours: buttons use ButtonText borders, links LinkText, current items a Highlight outline.',
        'Reduced motion removes the panel fade and the chevron turn.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Product site header',
          stack: true,
          render: () => (
            <div style={{ minHeight: '26rem' }}>
              <MegaMenu items={items} aria-label="Site" />
            </div>
          ),
        },
        {
          title: 'Click only',
          stack: true,
          render: () => (
            <div style={{ minHeight: '22rem' }}>
              <MegaMenu items={items.slice(1)} aria-label="Company" openOnHover={false} />
            </div>
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Write a one-line description for each link; it is what makes a mega menu worth its size.',
        'Pass a router link through `linkAs` so internal navigation stays client-side.',
        'Keep three or fewer sections per panel, plus one featured card.',
      ],
      dont: [
        'Do not put buttons or forms in panels; they hold links.',
        'Do not use it for in-app navigation.',
        'Do not hide important destinations only in a featured card.',
      ],
    },
    related: {
      items: [
        { id: 'dropdown-menu', reason: 'Actions on an object, with menu semantics.' },
        { id: 'link', reason: 'A single link with consistent styling.' },
        { id: 'tabs', reason: 'Switching views within one page.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['badge'],
    },
    testing: {
      unit: [
        'Named navigation of disclosure buttons and plain links, no menu roles; no axe violations closed or open.',
        'Click opens a panel with named link lists, badges, an external link that says so, and a featured card; Escape closes and returns focus.',
        'One panel at a time; a second click closes.',
        'Arrow keys, Home and End between entries; Arrow Down into the panel; Up and Down through links and back.',
        'Closes on outside click and when focus leaves; stays open while focus is inside.',
        'Mouse hover opens in the wide layout, a click pins it, leaving closes it after a delay; touch and the narrow layout never open on hover; `openOnHover={false}`.',
        'Small-screen Menu toggle with Escape; current pages, custom link component, `onNavigate`; controlled open item.',
        'Renders on the server.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
