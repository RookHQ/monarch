import { forwardRef, useState, type AnchorHTMLAttributes } from 'react';
import { SidebarNav, type SidebarNavSection } from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';
import {
  CalendarIcon,
  GearIcon,
  HomeIcon,
  InboxIcon,
  UsersIcon,
} from '../../../internal/docs-demo.js';

const SECTIONS: SidebarNavSection[] = [
  {
    key: 'main',
    items: [
      { key: 'home', label: 'Home', icon: <HomeIcon />, href: '#/home', exact: true },
      { key: 'inbox', label: 'Inbox', icon: <InboxIcon />, href: '#/inbox', badge: 4 },
      { key: 'calendar', label: 'Calendar', icon: <CalendarIcon />, href: '#/calendar' },
    ],
  },
  {
    key: 'teams',
    label: 'Teams',
    items: [
      {
        key: 'design',
        label: 'Design',
        icon: <UsersIcon />,
        items: [
          {
            key: 'design-issues',
            label: 'Issues',
            href: '#/teams/design/issues',
            badge: 12,
            badgeTone: 'neutral',
          },
          {
            key: 'design-backlog',
            label: 'Backlog',
            href: '#/teams/design/backlog',
            badge: 48,
            badgeTone: 'neutral',
          },
        ],
      },
      {
        key: 'research',
        label: 'Research',
        icon: <UsersIcon />,
        items: [
          { key: 'research-issues', label: 'Issues', href: '#/teams/research/issues' },
          { key: 'research-notes', label: 'Notes', href: '#/teams/research/notes' },
        ],
      },
    ],
  },
  {
    key: 'admin',
    label: 'Workspace',
    items: [{ key: 'settings', label: 'Settings', icon: <GearIcon />, href: '#/settings' }],
  },
];

/** Stands in for a router link: the docs site's own URL must not change. */
const DemoLink = forwardRef<HTMLAnchorElement, AnchorHTMLAttributes<HTMLAnchorElement>>(
  function DemoLink({ onClick, href, children, ...props }, ref) {
    return (
      <a
        ref={ref}
        {...props}
        href={href}
        onClick={(e) => {
          e.preventDefault();
          onClick?.(e);
        }}
      >
        {children}
      </a>
    );
  },
);

function Demo({ start = false }: { start?: boolean }) {
  const [path, setPath] = useState('/teams/design/backlog');
  const [collapsed, setCollapsed] = useState(start);
  return (
    <div style={{ height: 460, borderInlineEnd: '1px solid var(--mn-color-border-default)' }}>
      <SidebarNav
        aria-label={start ? 'Collapsed demo' : 'Demo'}
        sections={SECTIONS}
        currentPath={path}
        linkAs={DemoLink}
        collapsible
        collapsed={collapsed}
        onCollapsedChange={setCollapsed}
        onNavigate={(item) => item.href && setPath(item.href.slice(1))}
        style={{ width: collapsed ? undefined : 240 }}
      />
    </div>
  );
}

export default {
  id: 'sidebar-nav',
  title: 'SidebarNav',
  summary: 'App sidebar navigation: sections, nested groups, current page, icon-only collapse.',
  sections: {
    purpose: {
      summary:
        'The navigation list inside an app sidebar: labelled sections of links, nested groups, count badges, the current page and an icon-only collapsed mode with tooltips. Replaces the per-app link lists in `src/components/layout/Sidebar.tsx`. Needed by: Rook Stride (team groups with Issues / Backlog / Sprints), Rook Tickets, Rook Base, Rook Intellect, Rook Recruit, Rook People, Rook Calendar, Rook Dots, Rook Atlas, Rook Chat, Rook Developers, Rook Dossier, Rook Friends, Rook GTM, Rook Pulse, Rook Ranker, Rook Signal, Rook App.',
      useWhen: [
        'An app or settings area needs a vertical list of destinations, grouped into sections.',
        'Some destinations have sub-pages that should be shown under their parent.',
        'Inside a custom sidebar, a Sheet on phones, or a settings page’s side column.',
      ],
      avoidWhen: [
        'The full Rook app shell with brand, workspace switcher and account menu: use RookRail, which already includes its navigation.',
        'Navigation between sections of one page: use TableOfContents or TabsNav.',
        'Fewer than four top-level destinations on a phone: use BottomTabBar.',
      ],
    },
    designPrinciples: {
      items: [
        'Links stay links: the APG disclosure-navigation pattern, not an ARIA menu or tree, so Tab, Enter and middle-click work as people expect.',
        'The current page is marked with `aria-current="page"`, an accent background and a start-edge bar, never by colour alone.',
        'Router-agnostic: the app passes `currentPath` and its link component (`linkAs`); matching is by path segment, or exact.',
        'Collapsing keeps every name available: labels are visually hidden, tooltips show them on hover and focus.',
        'Fills its container: the app decides the width, placement and the phone drawer.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Renders a `<nav>` (ref) wrapped in a TooltipProvider; other attributes pass through. `aria-label` overrides `messages.nav`.',
        'Items are data: `{ key, label, icon?, href?, external?, onSelect?, badge?, badgeTone?, badgeLabel?, active?, exact?, items?, defaultOpen? }`. An item with `items` is a group button; with `href` a link; otherwise a button.',
        'Groups open when they contain the current page or have `defaultOpen`; once a person opens or closes one, their choice is kept.',
        '`badgeTone` says what a badge means. `accent` (the default) is a solid bubble for something new or unread, read as “4 new”. `neutral` is a quiet count, such as the number of issues in a list, styled like a Tabs count and read as the number alone (`messages.count`).',
        '`SIDEBAR_NAV_MESSAGES` holds the default strings.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Migrating from the app `Sidebar.tsx` lists: `NAV.map(({ href, label, icon: Icon }) => …)` becomes `sections={[{ key: "main", items: NAV.map((n) => ({ key: n.href, label: n.label, icon: <n.icon />, href: n.href })) }]}`; `match: (p) => p.startsWith(…)` becomes `currentPath={usePathname()}` (prefix matching by default, `exact` for home), or `active` for custom rules; `<Link>` becomes `linkAs={Link}`.',
        'From the local RookRail item type: `key`, `label`, `icon`, `href`, `external`, `onSelect`, `badge`, `active` and `exact` mean the same here.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'collapsed / defaultCollapsed',
          values: ['false', 'true'],
          description:
            'Icon-only mode at `--mn-layout-rail-collapsed` width with tooltips (`data-collapsed`). Accent badges become a dot, neutral counts are not drawn (still spoken); nested groups close.',
        },
        {
          prop: 'item.badgeTone',
          values: ['accent', 'neutral'],
          description:
            '`accent` (default): solid accent bubble for unread or new items. `neutral`: muted count (`--mn-color-bg-muted`, `--mn-color-text-muted`) that does not read as a notification.',
        },
        {
          prop: 'collapsible',
          values: ['false', 'true'],
          description: 'Shows the collapse / expand button at the bottom.',
        },
        {
          prop: 'item kind',
          values: ['link', 'external link', 'button', 'group'],
          description:
            '`href` makes a link (`external` opens a new tab), no `href` a button, `items` a disclosure group.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Current page',
          attribute: 'aria-current="page", data-active',
          description: 'Accent subtle background, accent text and a start-edge bar.',
        },
        {
          name: 'Group open',
          attribute: 'aria-expanded="true"',
          description: 'The chevron turns down and the nested list is shown.',
        },
        {
          name: 'Group contains the current page',
          attribute: 'data-contains-current',
          description: 'Default text colour, so the path to the current page stays visible.',
        },
        {
          name: 'Collapsed',
          attribute: 'data-collapsed (on the nav)',
          description: 'Icons only; labels visually hidden; tooltips on hover and focus.',
        },
        { name: 'Hover', description: 'Muted background and default text colour.' },
        { name: 'Focus', description: 'Focus ring inside the item on `:focus-visible`.' },
      ],
    },
    interaction: {
      items: [
        'Clicking a link calls the item’s `onSelect`, then `onNavigate`, then navigates.',
        'Clicking a group toggles its nested list. When collapsed, it expands the sidebar and opens the group instead.',
        'The collapse button switches modes and calls `onCollapsedChange`.',
        'The body scrolls on its own when the list is taller than the sidebar; header, footer and the collapse button stay put.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab / Shift+Tab', action: 'Moves between items in order.' },
        { keys: 'Enter', action: 'Follows a link or activates a button.' },
        { keys: 'Enter / Space', action: 'Opens or closes a group.' },
        { keys: 'Escape', action: 'Hides a visible tooltip (collapsed mode).' },
      ],
      notes: ['No arrow-key roving: this is site navigation, not a menu or tree.'],
    },
    accessibility: {
      role: 'navigation, with lists of links and disclosure buttons',
      name: '`aria-label`, or `messages.nav` (“Main”). Name each navigation landmark differently when there are several.',
      items: [
        'Section labels name their list (`aria-labelledby`).',
        'Groups use `aria-expanded` and `aria-controls`.',
        'Badges are read as part of the item: “Inbox, 4 new” for an accent badge (`messages.badge`), “Issues, 12” for a neutral count (`messages.count`), or your `badgeLabel`.',
        'External links say “opens in a new tab”.',
        'In collapsed mode labels stay in the accessibility tree; tooltips are supplementary.',
        'Unsafe `href` values (such as `javascript:`) are dropped and the item renders as a button.',
      ],
    },
    responsive: {
      items: [
        'Takes the width of its container; long labels truncate with an ellipsis.',
        'Items are at least 44px tall on touch screens.',
        'On phones, place it in a Sheet or Drawer and close it from `onNavigate`.',
      ],
    },
    theme: {
      items: [
        'Neutral surfaces (`--mn-color-bg-muted` hover) and the product accent for the current page and accent badges; neutral counts use `--mn-color-bg-muted` and `--mn-color-text-muted`.',
        'Logical properties throughout, so the bar, indent and chevron mirror in right-to-left.',
        'Forced colours: the current item gets a Highlight border and text; focus uses Highlight.',
      ],
    },
    examples: {
      items: [
        { title: 'Sections, nested groups and badges', stack: true, render: () => <Demo /> },
        { title: 'Collapsed to icons', stack: true, render: () => <Demo start /> },
      ],
    },
    doDont: {
      do: [
        'Pass `currentPath` from your router and `linkAs` for client navigation.',
        'Use `badgeTone: "neutral"` for plain counts (items in a list) and keep the accent bubble for things that are new or unread.',
        'Give every top-level item an icon if the sidebar can collapse.',
        'Keep nesting to one level where possible.',
      ],
      dont: [
        'Do not put actions that change data in the navigation; use buttons in the page.',
        'Do not use `active` to highlight something that is not the current page.',
        'Do not show every count as an accent bubble: when everything looks like a notification, nothing does.',
        'Do not nest more than two levels.',
      ],
    },
    related: {
      items: [
        {
          id: 'rook-rail',
          reason: 'The complete Rook app sidebar with brand, workspace and account.',
        },
        { id: 'top-nav', reason: 'Horizontal navigation with overflow.' },
        { id: 'bottom-tab-bar', reason: 'Primary navigation on phones.' },
        { id: 'skip-link', reason: 'Lets keyboard users jump past the sidebar.' },
        { id: 'favorites-list', reason: 'A pinned-items section inside a sidebar.' },
      ],
    },
    dependencies: { packages: ['react'], components: ['tooltip'] },
    testing: {
      unit: [
        'Named landmark, labelled section lists, `aria-current="page"` by prefix match, badge in the name; no axe violations.',
        'Accent and neutral badges: `data-tone`, spoken text without “new” for counts, `badgeLabel` and `messages.count`; no axe violations.',
        'Groups toggle with Enter and Space via `aria-expanded` / `aria-controls`; a person’s choice outlives path changes.',
        'The group containing the current page opens on first render.',
        'External links open safely in a new tab; unsafe URLs are not rendered as links.',
        '`linkAs` renders router links; `onSelect` and `onNavigate` are called.',
        'Collapse button, collapsed names, and expanding from a group click; no axe violations collapsed.',
        'Tooltip shows the label on focus when collapsed.',
        'Controlled collapse, header and footer slots, translated strings and forwarded ref.',
        'Renders on the server.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
