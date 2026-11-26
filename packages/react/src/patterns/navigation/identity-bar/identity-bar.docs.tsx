import { IconButton, SearchInput } from '@rookcom/monarch-react';
import { IdentityBar, ThemeToggle } from '@rookcom/monarch-react/shell';
import type { ComponentDocs } from '../../../internal/component-docs.js';
import { AppMark, BellIcon } from '../../../internal/docs-demo.js';

const account = {
  name: 'Grace Hopper',
  email: 'grace@example.com',
  profileHref: '#profile',
  accountHref: 'https://id.example.com',
  onSignOut: () => {},
};

export default {
  id: 'identity-bar',
  title: 'IdentityBar',
  summary: 'The top bar of a Rook app: product name, search, notifications and the account menu.',
  sections: {
    purpose: {
      summary:
        'Shows which product you are in and who you are signed in as, with room for search and notifications.',
      useWhen: [
        'The top of a signed-in app page, inside AppShell next to RookRail, or alone in an app without a rail.',
      ],
      avoidWhen: [
        'Marketing pages with product menus: use the marketing site shell (`SiteHeader`) from `@rookcom/monarch-react/marketing`.',
        'A page title with actions: use PageHeader inside the main region.',
      ],
    },
    designPrinciples: {
      items: [
        'The product name is always visible text, never only a logo.',
        'Slots, not settings: search, start, actions and notifications are whatever the app passes.',
        'The account menu is the same as the rail’s, so Profile, Rook account and Sign out are in the same order everywhere.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: ['Forwards its ref to the `<header>`.', '`account` has the same shape as RookRail’s.'],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        '`product`: `{ name, href?, logo? }`. Without `href` the name is plain text.',
        '`sticky` (default true) keeps the bar at the top of its scrolling area.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'sticky',
          values: ['true', 'false'],
          description: 'Sticks to the top of the scroll container (`data-sticky`).',
        },
      ],
    },
    states: {
      items: [
        { name: 'Sticky', attribute: 'data-sticky', description: 'Default.' },
        {
          name: 'Account menu open',
          attribute: 'aria-expanded="true"',
          description: 'On the avatar button.',
        },
      ],
    },
    interaction: {
      items: [
        'The product name links home through `linkComponent` when `href` is set.',
        'The avatar button opens the account menu: the person, Profile, extra entries, Rook account (new tab) and Sign out.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Product link, slots in order, then the account button.' },
        {
          keys: 'Enter / Space / ↓',
          action: 'Open the account menu with focus on its first item.',
        },
        { keys: '↓ / ↑', action: 'Move through the menu.' },
        { keys: 'Escape', action: 'Close the menu and return focus to the avatar button.' },
      ],
    },
    accessibility: {
      role: '`banner` landmark (`<header>` outside main)',
      name: 'The product link is named by the product name; the avatar button is "Account: <name>".',
      items: [
        'The logo is decorative; the name is text.',
        'The account menu is a Monarch DropdownMenu (APG menu button), with the person’s name and email as its label.',
        'New-tab links say so to screen readers.',
      ],
    },
    responsive: {
      items: [
        'The product name truncates; below 640px the search slot moves to its own full-width row.',
        'The avatar button is 44px on touch screens.',
      ],
    },
    theme: {
      items: ['Surface, border and text tokens; works in every theme and more-contrast mode.'],
    },
    examples: {
      items: [
        {
          title: 'Product, search, notifications and account',
          stack: true,
          render: () => (
            <IdentityBar
              style={{ width: '100%' }}
              sticky={false}
              product={{ name: 'Acme Desk', href: '#home', logo: <AppMark /> }}
              search={<SearchInput aria-label="Search tickets" placeholder="Search tickets" />}
              notifications={<IconButton label="Notifications" icon={<BellIcon />} />}
              account={account}
            />
          ),
        },
        {
          title: 'Context and actions, without a link',
          stack: true,
          render: () => (
            <IdentityBar
              style={{ width: '100%' }}
              sticky={false}
              product={{ name: 'Acme Desk' }}
              start={<span>Support queue</span>}
              actions={
                <ThemeToggle
                  variant="menu"
                  size="sm"
                  showLabel={false}
                  cookies={null}
                  applyToDocument={false}
                />
              }
              account={{ name: 'Grace Hopper' }}
            />
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Use one IdentityBar per page, at the top of AppShell.',
        'Name search fields and icon buttons you pass into the slots.',
      ],
      dont: [
        'Do not put primary navigation here; that is the rail’s job.',
        'Do not repeat the account menu in the bar when the rail already shows it, unless the app has no rail.',
      ],
    },
    related: {
      items: [
        { id: 'app-shell', reason: 'Places the bar above the main region.' },
        { id: 'rook-rail', reason: 'The primary navigation beside it.' },
        { id: 'search-input', reason: 'For the search slot.' },
        { id: 'marketing-site-shell', reason: 'The header for marketing pages.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['avatar', 'dropdown-menu'],
    },
    testing: {
      unit: [
        'Banner landmark with the product link, search and notifications slots.',
        'Plain product name without `href`; start and actions slots.',
        'Account menu from the keyboard with the person, Profile and Sign out; Escape returns focus.',
        'Link component, translated messages, sticky off, server render.',
      ],
      axe: true,
      ssr: true,
      browser: [
        'Visual baselines: /__gallery/identity-bar',
        'Cross-browser axe: /components/identity-bar',
      ],
    },
  },
} satisfies ComponentDocs;
