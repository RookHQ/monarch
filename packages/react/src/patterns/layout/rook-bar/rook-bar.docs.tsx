import { useState } from 'react';
import {
  RookBar,
  RookBarAppCatalog,
  RookBarFeaturePanel,
  RookBarLinkList,
  RookBarStoriesPanel,
  type RookBarItem,
  type RookBarStory,
} from '@rookcom/monarch-react/shell';
import { AppMark } from '../../../internal/docs-demo.js';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const categories = [
  {
    id: 'sales',
    name: 'Sales & Support',
    promise: 'Win customers, then look after them.',
    apps: [
      {
        id: 'pipeline',
        label: 'Pipeline',
        description: 'Leads, deals and invoices.',
        href: '#pipeline',
        icon: <AppMark />,
      },
      {
        id: 'desk',
        label: 'Help desk',
        description: 'Support with a branded portal.',
        href: 'https://desk.example.com',
        external: true,
        icon: <AppMark />,
      },
    ],
  },
  {
    id: 'team',
    name: 'Collaboration',
    promise: 'Everyone on the same page.',
    apps: [
      {
        id: 'chat',
        label: 'Team chat',
        description: 'Channels, calls and search.',
        href: '#chat',
        icon: <AppMark />,
      },
      {
        id: 'docs',
        label: 'Documents',
        description: 'Docs the whole workspace can find.',
        href: '#docs',
        icon: <AppMark />,
      },
      {
        id: 'planner',
        label: 'Planner',
        description: 'Plan sprints across teams.',
        href: '#planner',
        icon: <AppMark />,
      },
    ],
  },
];

const STORIES: RookBarStory[] = [
  {
    id: 's1',
    title: 'What we learned shipping a design system',
    href: '#post-1',
    label: 'Blog',
    meta: 'Mar 2, 2026',
  },
  {
    id: 's2',
    title: 'Quarterly product update',
    href: '#post-2',
    label: 'Press',
    meta: 'Feb 18, 2026',
  },
  {
    id: 's3',
    title: 'Our plans for the year',
    href: '#post-3',
    label: 'Letters',
    meta: 'Jan 9, 2026',
  },
];

function useItems(): RookBarItem[] {
  const [stories, setStories] = useState<RookBarStory[] | null>(null);
  return [
    {
      id: 'products',
      label: 'Products',
      panel: (
        <RookBarAppCatalog
          categories={categories}
          messages={{ allTitle: 'Every app', allPromise: 'One account opens all of them.' }}
          aside={
            <RookBarLinkList
              heading="Platform"
              links={[
                { label: 'Accounts', description: 'One sign-in for every app.', href: '#accounts' },
                { label: 'Developers', description: 'Guides and API docs.', href: '#developers' },
              ]}
            />
          }
        />
      ),
    },
    {
      id: 'services',
      label: 'Services',
      panel: (
        <RookBarFeaturePanel
          feature={{
            title: 'When you need more than software.',
            description: 'Tell us what you need. We will build it with you.',
            action: {
              label: 'Visit our services site',
              href: 'https://services.example.com',
              external: true,
            },
          }}
          links={[
            {
              label: 'Product engineering',
              description: 'Designed and built end to end.',
              href: '#engineering',
            },
            {
              label: 'Implementation',
              description: 'Brought into the way you work.',
              href: '#implementation',
            },
            { label: 'Consulting', description: 'Plans, reviews and audits.', href: '#consulting' },
            {
              label: 'Enterprise systems',
              description: 'Engineered for scale.',
              href: '#enterprise',
            },
          ]}
        />
      ),
    },
    { label: 'Pricing', href: '#pricing' },
    {
      id: 'news',
      label: 'News',
      onOpen: () => {
        if (!stories) setTimeout(() => setStories(STORIES), 600);
      },
      panel: (
        <RookBarStoriesPanel
          sections={[
            { label: 'Blog', description: 'Ideas from the team.', href: '#blog' },
            { label: 'Press releases', description: 'Announcements.', href: '#press' },
          ]}
          stories={stories}
        />
      ),
    },
  ];
}

const brand = (
  <a
    href="#home"
    style={{
      display: 'inline-flex',
      alignItems: 'center',
      gap: 'var(--mn-space-2)',
      color: 'inherit',
      fontWeight: 600,
      textDecoration: 'none',
    }}
  >
    <AppMark />
    Northwind
  </a>
);

function SignedOut() {
  return (
    <RookBar
      position="static"
      brand={brand}
      items={useItems()}
      account={{ status: 'signed-out', signInHref: '#login', signUpHref: '#signup' }}
    />
  );
}

function SignedIn() {
  return (
    <RookBar
      position="static"
      brand={brand}
      items={useItems()}
      account={{
        status: 'signed-in',
        name: 'Ada Lovelace',
        dashboardHref: '#home',
        accountHref: 'https://id.example.com',
        onSignOut: () => {},
      }}
    />
  );
}

export default {
  id: 'rook-bar',
  title: 'RookBar',
  summary: 'The company bar on public pages: wordmark, wide menus, links and account controls.',
  sections: {
    purpose: {
      summary:
        'Every Rook app’s public pages carry the same company bar as the main site: Products, services, news and the account controls, with a full-height sheet on phones. RookBar is that bar, with panels for an app catalog, a featured card with links, and recent posts. The data (apps, links, posts) comes from the app. Needed by: every Rook app with public pages (Links, Tickets, App, Dossier, Intellect, Calendar, Friends, Atlas, Signal, Base, Chat, Dots, Pulse, People, Stride, Recruit, GTM, Ranker).',
      useWhen: [
        'The top of a public or signed-out page that should match the company site.',
        'A product site that needs wide menus (mega-menu panels) rather than short dropdowns.',
      ],
      avoidWhen: [
        'Inside the signed-in app: use IdentityBar or RookRail.',
        'A simple product site header with a few links and short menus: use SiteHeader from the marketing entry.',
      ],
    },
    designPrinciples: {
      items: [
        'Data in: every app, link and post is a prop; the bar never fetches. Load posts in a menu’s `onOpen`.',
        'Disclosure navigation, not an application menu: triggers are buttons with `aria-expanded`, and each panel follows its trigger in the tab order.',
        'Hover intent only for a mouse: a short delay before the first open, instant switching once open, a forgiving close. Touch and keyboard open on click.',
        'Panels know where they are: `useRookBarPanel()` tells a panel whether it is in the desktop panel or the mobile sheet, and gives it `close`.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Forwards its ref to the root `<div>`. Also exports RookBarAppCatalog, RookBarFeaturePanel, RookBarStoriesPanel, RookBarLinkList and `useRookBarPanel()`.',
        'Items are `{ label, href, external? }` links or `{ id, label, panel, onOpen? }` menus.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        '`account.status` is `loading` (nothing rendered, so server and client match), `signed-out` (Log in, Sign up) or `signed-in` (account menu).',
        'Migrating from the app copies (`components/rook-bar/*` plus `components/RookIdentityBar.tsx`, generated from the main site): keep `data.ts` (APP_CATEGORIES, PLATFORM, PROGRAMS, SERVICES, HAPPENINGS) and the happenings fetch in the app. `navItems` becomes `items`: Products → `{ id, label, panel: <RookBarAppCatalog categories={APP_CATEGORIES mapped to { id, label: name, description: tagline, href, external, icon: <img alt="" …/> }} aside={<RookBarLinkList heading="Underneath it all" links={PLATFORM}/> …} /> }`; What We Do → `<RookBarFeaturePanel feature={{ title, description, media: Realm logo, action }} links={SERVICES} />`; Happenings → `<RookBarStoriesPanel sections={HAPPENINGS} stories={latest} />` with `onOpen` starting `useLatestHappenings`. `useAuth()` maps to `account` (`isLoading` → `loading`, `getRookIdLoginUrl()` → `signInHref`, `getRookIdSignupUrl()` → `signUpHref`, `logout` → `onSignOut`, `DASHBOARD_HREF` → `dashboardHref`, `${ROOK_ID_URL}/profile` → `accountHref`). `usePathname()` → `currentPath`; the scroll progress line is `scrollProgress`. framer-motion is no longer needed.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'position',
          values: ['sticky', 'static'],
          description: 'Stick to the top of the scrolling area, or scroll away.',
        },
        {
          prop: 'scrollProgress',
          values: ['false', 'true'],
          description: 'An accent line along the bottom showing how far the page has scrolled.',
        },
      ],
      notes: [
        'Panels: RookBarAppCatalog (search, categories, app tiles, an aside), RookBarFeaturePanel (a featured card beside numbered links), RookBarStoriesPanel (section links beside recent posts), RookBarLinkList (described links), or your own.',
      ],
    },
    states: {
      items: [
        {
          name: 'Menu open',
          attribute: 'data-menu-open',
          description: 'A desktop panel is showing; the page behind dims.',
        },
        {
          name: 'Trigger open',
          attribute: 'aria-expanded="true"',
          description: 'Also `data-state="open"`.',
        },
        {
          name: 'Sheet open',
          attribute: 'data-sheet-open',
          description: 'The mobile sheet covers the page and owns scrolling.',
        },
        {
          name: 'Solid',
          attribute: 'data-solid',
          description: 'Scrolled or open: the bottom border shows.',
        },
        { name: 'Category', attribute: 'aria-pressed', description: 'The catalog category shown.' },
        {
          name: 'Posts loading',
          attribute: 'role="status"',
          description: '`stories={null}` shows placeholders named "Loading latest posts".',
        },
      ],
    },
    interaction: {
      items: [
        'Desktop (64rem and wider): pointing at a menu with a mouse opens it after `openDelay`; moving to another switches instantly; leaving closes after `closeDelay`. Clicking toggles.',
        'Following any link closes every menu; so does a press outside, leaving the window, a route change (`currentPath`) and Escape.',
        'Narrower than 64rem: the menu button opens a full-height sheet; menus become collapsible sections and the account actions sit at the bottom.',
        'In the catalog, pointing at or focusing a category shows its apps; typing in the search shows matches across all categories.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Tab',
          action: 'Move through the bar; an open panel comes right after its trigger.',
        },
        { keys: 'Enter / Space', action: 'Open or close a menu, a sheet section or the sheet.' },
        {
          keys: 'Escape',
          action: 'Close the open menu or the sheet and return focus to its button.',
        },
      ],
    },
    accessibility: {
      role: 'navigation landmark with disclosure buttons',
      name: '"Rook" (`messages.nav`); the sheet button is "Open menu" / "Close menu".',
      items: [
        'Triggers use `aria-expanded` and `aria-controls`; panels are plain content, not ARIA menus, so links are read normally.',
        'Search results are announced in a status region; catalog categories are toggle buttons with `aria-pressed`.',
        'Links that open another site say "opens in a new tab"; app icons and post pictures are decorative.',
        'The account trigger is named "Account menu" and opens a Monarch DropdownMenu.',
      ],
    },
    responsive: {
      items: [
        'Desktop from 64rem; below it the sheet replaces the items. At 390px nothing scrolls sideways.',
        'Panels are at most 72.5rem wide and scroll inside when taller than the window.',
        'Sheet sections and actions are at least 44px tall.',
      ],
    },
    theme: {
      items: [
        'Surface, raised and subtle backgrounds; the featured card and primary action use `--mn-color-bg-inverse`, so they flip in dark themes.',
        'The progress line is `--mn-color-accent-solid`, so it takes the product’s accent.',
        'Forced colours outline panels and the sheet; reduced motion removes the panel entrance and chevron turns.',
      ],
    },
    examples: {
      items: [
        { title: 'Signed out', stack: true, render: () => <SignedOut /> },
        { title: 'Signed in', stack: true, render: () => <SignedIn /> },
      ],
    },
    doDont: {
      do: [
        'Keep the app list and links in one shared data module so every app shows the same bar.',
        'Load posts in `onOpen` and cache them; pass `null` while loading.',
        'Pass `currentPath` from your router so menus close on navigation.',
      ],
      dont: [
        'Do not put forms or settings in a panel; it is navigation.',
        'Do not render the account controls before the session is known; use `status: "loading"`.',
      ],
    },
    related: {
      items: [
        { id: 'marketing-site-shell', reason: 'SiteHeader for simpler product sites.' },
        { id: 'identity-bar', reason: 'The signed-in app’s top bar.' },
        { id: 'mega-menu', reason: 'A general wide-menu component.' },
        { id: 'dropdown-menu', reason: 'The account menu.' },
      ],
    },
    dependencies: { packages: ['react'], components: ['avatar', 'dropdown-menu'] },
    testing: {
      unit: [
        'Named navigation with menu buttons, links, account actions and the sheet button.',
        'Keyboard: open, panel next in tab order, Escape returns focus.',
        'Catalog categories (`aria-pressed`), search status, external links.',
        'Hover intent with fake timers; instant switching once open.',
        '`onOpen` once; loading placeholders then posts.',
        'Closes on link follow and route change; mobile sheet sections, sign out, Escape; signed-in account menu; translation; SSR with a loading account.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
