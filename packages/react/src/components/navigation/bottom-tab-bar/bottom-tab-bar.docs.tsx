import { forwardRef, useState, type AnchorHTMLAttributes } from 'react';
import { BottomTabBar, type BottomTabBarItem } from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';
// The scenario of the primary example, shared with iOS and Android (CROSS-PLATFORM.md section 8).
import fixture from './bottom-tab-bar.fixture.json';
import {
  BellIcon,
  HomeIcon,
  InboxIcon,
  MoreIcon,
  SearchIcon,
} from '../../../internal/docs-demo.js';

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

// The router links and glyphs are web-only; the labels and the badge come from the fixture.
type FixtureItem = { key: string; label: string; badge?: number };
const [HOME, INBOX, SEARCH, ACTIVITY, MORE] = fixture.data.items as [
  FixtureItem,
  FixtureItem,
  FixtureItem,
  FixtureItem,
  FixtureItem,
];
const ITEMS: BottomTabBarItem[] = [
  { key: HOME.key, label: HOME.label, icon: <HomeIcon />, href: '/', exact: true },
  { key: INBOX.key, label: INBOX.label, icon: <InboxIcon />, href: '/inbox', badge: INBOX.badge },
  { key: SEARCH.key, label: SEARCH.label, icon: <SearchIcon />, href: '/search' },
  { key: ACTIVITY.key, label: ACTIVITY.label, icon: <BellIcon />, href: '/activity' },
];
const CURRENT_PATH = `/${fixture.data.current}`;

function Demo() {
  const [path, setPath] = useState(CURRENT_PATH);
  const [opened, setOpened] = useState(0);
  return (
    <div style={{ width: '100%', maxWidth: 390 }}>
      <p style={{ margin: '0 0 8px' }}>
        Current: {path}
        {opened ? ` · More opened ${opened}×` : ''}
      </p>
      <BottomTabBar
        aria-label="Demo"
        position="static"
        linkAs={DemoLink}
        currentPath={path}
        onNavigate={(item) => item.href && setPath(item.href)}
        items={[
          ...ITEMS,
          {
            key: MORE.key,
            label: MORE.label,
            icon: <MoreIcon />,
            onSelect: () => setOpened((n) => n + 1),
          },
        ]}
      />
    </div>
  );
}

export default {
  id: 'bottom-tab-bar',
  title: 'BottomTabBar',
  summary: 'Phone navigation: three to five icon tabs pinned to the bottom with safe-area insets.',
  sections: {
    purpose: {
      summary:
        'Primary navigation for phone layouts, within thumb reach and clear of the home indicator. Needed by: none recorded yet; the Rook apps currently open a drawer from the top bar on phones (`src/components/rook-bar/mobile-menu.tsx` in Rook Base, Rook Calendar, Rook People, Rook Pulse, Rook Ranker, Rook Signal, Rook Partner, Rook Dots, Rook App), and BottomTabBar is the option for app areas people switch between constantly.',
      useWhen: [
        'A phone layout with three to five top-level destinations people switch between often.',
        'Alongside SidebarNav or RookRail on wide screens, hidden there with `hideFrom`.',
      ],
      avoidWhen: [
        'More than five destinations: keep the most used four and put the rest behind a “More” tab, or use a drawer.',
        'Actions such as “Create”: use a button in the page or a floating action.',
        'Switching views within a page: use Tabs or SegmentedControl.',
      ],
    },
    designPrinciples: {
      items: [
        'Always icon and label: icons alone are ambiguous.',
        'The current tab is shown by accent colour, weight and an indicator pill behind the icon, and announced with `aria-current="page"`.',
        'Respects `env(safe-area-inset-*)`, so it never sits under the home indicator or rounded corners.',
        'Links, not an ARIA tablist: each tab is a destination with its own URL.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Renders `<nav>` (ref) with a list of links or buttons; other attributes pass through.',
        'Items: `{ key, label, icon, activeIcon?, href?, onSelect?, badge?, badgeLabel?, active?, exact? }`. Without `href` the tab is a button.',
        '`BOTTOM_TAB_BAR_MESSAGES` holds the default strings.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [
        {
          prop: 'position',
          values: ['fixed', 'static'],
          description:
            '`fixed` (default) pins it to the viewport bottom above content (`--mn-z-sticky`); `static` leaves placement to your layout. Exposed as `data-position`. With `fixed`, add bottom padding to the page so content is not hidden.',
        },
        {
          prop: 'hideFrom',
          values: ['sm', 'md', 'lg'],
          description: 'Hides the bar from 640, 768 or 1024px up (`data-hide-from`).',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Current',
          attribute: 'aria-current="page", data-active',
          description:
            'Accent text, semibold label, accent subtle pill behind the icon, `activeIcon` if given.',
        },
        { name: 'Badge', description: 'A count on the icon (99+ above 99), read with the label.' },
        { name: 'Focus', description: 'Focus ring inside the tab on `:focus-visible`.' },
      ],
    },
    interaction: {
      items: [
        'Tapping a tab calls its `onSelect`, then `onNavigate`, then follows the link.',
        'A tab without `href` is a button, e.g. “More” opening a Sheet.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab / Shift+Tab', action: 'Moves between tabs.' },
        { keys: 'Enter', action: 'Follows the link.' },
        { keys: 'Enter / Space', action: 'Activates a button tab.' },
      ],
    },
    accessibility: {
      role: 'navigation with a list of links',
      name: '`aria-label`, or `messages.nav` (“Main”). Use a different name from the sidebar’s if both are in the page.',
      items: [
        'The current tab has `aria-current="page"`.',
        'Icons are hidden from assistive technology; the label is the name, and the badge adds “3 new”.',
        'Tabs are at least 56px tall and share the width equally, above the 44px minimum target.',
        'Unsafe `href` values are dropped and the tab renders as a button.',
      ],
    },
    responsive: {
      items: [
        'Tabs share the width equally; labels truncate rather than wrap at 320px.',
        'Safe-area insets pad the bottom and sides on notched devices (add `viewport-fit=cover` to the viewport meta tag).',
        'Use `hideFrom` to switch to a sidebar on wider screens.',
      ],
    },
    theme: {
      items: [
        'Surface `--mn-color-bg-surface` with a `--mn-color-border-default` top border; the product accent marks the current tab; badges use the danger intent.',
        'Forced colours: current tab in Highlight with an outlined pill.',
      ],
    },
    examples: {
      items: [
        { title: 'Four destinations and a More button', stack: true, render: () => <Demo /> },
      ],
    },
    doDont: {
      do: [
        'Keep labels to one short word.',
        'Pad the page bottom when the bar is fixed.',
        'Use the same destinations as the desktop sidebar’s top level.',
      ],
      dont: [
        'Do not use more than five tabs or fewer than three.',
        'Do not hide labels.',
        'Do not change the tabs per page; the bar is global.',
      ],
    },
    related: {
      items: [
        { id: 'sidebar-nav', reason: 'The wide-screen counterpart.' },
        { id: 'sheet', reason: 'Where a “More” tab can open the remaining destinations.' },
        { id: 'tabs', reason: 'Switching panels inside a page.' },
      ],
    },
    dependencies: { packages: ['react'], components: [] },
    testing: {
      unit: [
        'Named navigation of links, current tab by prefix match, badge capped at 99+ and read in the name, `activeIcon`; no axe violations.',
        'Tab moves between tabs; a button tab runs `onSelect` and `onNavigate` with Enter.',
        '`linkAs`, explicit `active`, `position`, `hideFrom`, translations, forwarded ref; unsafe URLs become buttons.',
        'Renders on the server.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
