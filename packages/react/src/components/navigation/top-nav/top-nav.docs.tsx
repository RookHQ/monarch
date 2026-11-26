import { forwardRef, useState, type AnchorHTMLAttributes } from 'react';
import { Button, Text, TopNav, type TopNavItem } from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';
import { AppMark } from '../../../internal/docs-demo.js';

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

const ITEMS: TopNavItem[] = [
  { key: 'overview', label: 'Overview', href: '/', exact: true },
  { key: 'projects', label: 'Projects', href: '/projects', badge: 3 },
  { key: 'calendar', label: 'Calendar', href: '/calendar' },
  { key: 'reports', label: 'Reports', href: '/reports' },
  { key: 'people', label: 'People', href: '/people' },
  { key: 'settings', label: 'Settings', href: '/settings' },
];

function Demo({ width }: { width: number }) {
  const [path, setPath] = useState('/');
  return (
    <div style={{ width: '100%', maxWidth: width }}>
      <TopNav
        aria-label={`Demo ${width}`}
        items={ITEMS}
        currentPath={path}
        linkAs={DemoLink}
        onNavigate={(item) => item.href && setPath(item.href)}
        start={<AppMark />}
        end={
          <Button size="sm" variant="secondary">
            Share
          </Button>
        }
      />
      <Text size="xs" tone="muted">
        Current: {path}
      </Text>
    </div>
  );
}

export default {
  id: 'top-nav',
  title: 'TopNav',
  summary: 'Horizontal navigation whose extra links collapse into a “More” menu.',
  sections: {
    purpose: {
      summary:
        'A single row of destinations for an app header or a site, which moves the links that do not fit into a “More” menu instead of wrapping or scrolling. Needed by: the app top bars in Rook Tickets, Rook Stride, Rook Dossier and Rook Calendar (`TopBar`), and simpler marketing headers that do not need MegaMenu panels.',
      useWhen: [
        'Top-level navigation in a header, with a handful of destinations.',
        'A secondary row of links under a page title whose width varies.',
      ],
      avoidWhen: [
        'Site navigation with large drop-down panels: use MegaMenu.',
        'Sections of one record that look like tabs: use TabsNav or OverflowTabs.',
        'Many destinations in groups: use SidebarNav.',
      ],
    },
    designPrinciples: {
      items: [
        'Priority+: items are listed in priority order and the last ones move into “More” first.',
        'The current page never hides in the menu, so people always see where they are.',
        'Links stay links in the row and in the menu (menu items render your link with `asChild`).',
        'Router-agnostic: `currentPath` + `linkAs`.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Renders `<nav>` (ref) with optional `start` and `end` slots; other attributes pass through.',
        'Items: `{ key, label, icon?, href?, external?, onSelect?, badge?, badgeLabel?, active?, exact? }`.',
        'Widths are measured from hidden clones with a ResizeObserver; before measuring (and on the server) every item is shown and the row clips.',
        '`TOP_NAV_MESSAGES` holds the default strings.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [
        {
          prop: 'item kind',
          values: ['link', 'external link', 'button'],
          description:
            '`href` makes a link (`external` opens a new tab); no `href` makes a button.',
        },
      ],
      notes: [
        'One visual style; the layout around it (header height, background) belongs to the app.',
      ],
    },
    states: {
      items: [
        {
          name: 'Current page',
          attribute: 'aria-current="page", data-active',
          description: 'Accent subtle background and accent text; in the menu, accent text.',
        },
        {
          name: 'Overflowing',
          attribute: 'data-overflowing (on the bar)',
          description: 'Some items are in the More menu.',
        },
        {
          name: 'Menu open',
          attribute: 'data-state="open" (More button)',
          description: 'The More button keeps its hover background.',
        },
        { name: 'Focus', description: 'Focus ring inside the item on `:focus-visible`.' },
      ],
    },
    interaction: {
      items: [
        'Resizing the container re-measures and moves items between the row and the menu.',
        'Activating an item calls `onSelect`, then `onNavigate`, then navigates.',
        'The More button opens a DropdownMenu of the remaining links.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab / Shift+Tab', action: 'Moves between links and the More button.' },
        { keys: 'Enter / Space / ArrowDown', action: 'Opens the More menu on the first entry.' },
        { keys: 'ArrowUp / ArrowDown', action: 'Moves within the menu.' },
        { keys: 'Escape', action: 'Closes the menu and returns focus to More.' },
      ],
    },
    accessibility: {
      role: 'navigation; the overflow is a menu button',
      name: '`aria-label`, or `messages.nav` (“Main”).',
      items: [
        'Current page marked with `aria-current="page"`, in the row or in the menu.',
        'The More button has `aria-haspopup="menu"` and `aria-expanded`.',
        'Measuring clones are `aria-hidden` and contain nothing focusable.',
        'External links say “opens in a new tab”; unsafe URLs are dropped.',
      ],
    },
    responsive: {
      items: [
        'Never scrolls or wraps: at 390px most items move into More.',
        'Items are 44px tall on touch screens.',
      ],
    },
    theme: {
      items: [
        'Muted text, `--mn-color-bg-muted` hover and the product accent for the current page.',
        'Forced colours: current item in Highlight with a border.',
      ],
    },
    examples: {
      items: [
        { title: 'Wide', stack: true, render: () => <Demo width={760} /> },
        {
          title: 'Narrow: the rest move into More',
          stack: true,
          render: () => <Demo width={360} />,
        },
      ],
    },
    doDont: {
      do: [
        'Order items by importance.',
        'Keep labels short so more fit.',
        'Pass `currentPath` so the current page stays visible.',
      ],
      dont: [
        'Do not put actions that change data in the row; use the `end` slot for buttons.',
        'Do not use it for more than about eight destinations.',
      ],
    },
    related: {
      items: [
        { id: 'mega-menu', reason: 'Site navigation with drop-down panels.' },
        { id: 'overflow-tabs', reason: 'The same overflow behaviour for tabs that switch panels.' },
        { id: 'sidebar-nav', reason: 'Vertical app navigation.' },
        { id: 'dropdown-menu', reason: 'The menu used for the overflow.' },
      ],
    },
    dependencies: { packages: ['react'], components: ['dropdown-menu'] },
    testing: {
      unit: [
        '`fitItems` keeps all items when they fit, otherwise cuts in order leaving room for More, and keeps the pinned item.',
        'Named navigation, current page, badge in the name, external link, slots; no axe violations.',
        'With a measured width, extra links move into a More menu reachable by Tab, opened with Enter, navigated with arrows and closed with Escape back to More.',
        'The current page stays visible when it would overflow.',
        'Menu entries call `onSelect` and `onNavigate`; More label is translatable.',
        '`linkAs`, ref, unsafe URLs as buttons, `end` slot.',
        'Renders on the server with every item visible.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
