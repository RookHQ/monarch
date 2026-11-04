import { useState, type CSSProperties, type ReactNode } from 'react';
import {
  AppShell,
  RookRail,
  SpaceSwitcher,
  ThemeToggle,
  WorkspaceSwitcher,
  type RookRailProps,
  type RookRailSlotContext,
} from '@rookcom/monarch-react/shell';
import type { ComponentDocs } from '../../../internal/component-docs.js';
import {
  AppMark,
  BellIcon,
  CalendarIcon,
  CardIcon,
  GearIcon,
  HomeIcon,
  InboxIcon,
  PlusIcon,
  UsersIcon,
} from '../../../internal/docs-demo.js';

const frame: CSSProperties = {
  position: 'relative',
  width: '100%',
  height: '36rem',
  overflow: 'hidden',
  border: '1px solid var(--mn-color-border-default)',
  borderRadius: 'var(--mn-radius-lg)',
  background: 'var(--mn-color-bg-canvas)',
};
const page: CSSProperties = {
  padding: 'var(--mn-space-6)',
  color: 'var(--mn-color-text-muted)',
  fontSize: 'var(--mn-text-sm)',
};

/** Each example sits in an embedded AppShell, so it lays out like an app at every width. */
function Frame({ name, rail }: { name: string; rail: ReactNode }) {
  return (
    <div style={frame}>
      <AppShell
        style={{ height: '100%', minHeight: 0 }}
        mainRole="region"
        mainId={`rail-demo-${name}`}
        mainProps={{ 'aria-label': `Page (${name} example)` }}
        skipLinkLabel={`Skip to ${name} example content`}
        rail={rail}
      >
        <div style={page}>Page content</div>
      </AppShell>
    </div>
  );
}

/** Docs examples keep the theme choice in memory, so they never change this site's theme. */
function useMemoryCookies() {
  const [store] = useState(() => {
    const jar = new Map<string, string>();
    return {
      get: (n: string) => jar.get(n) ?? null,
      set: (n: string, v: string) => void jar.set(n, v),
    };
  });
  return store;
}

/** Built lazily: docs files are imported in Node, where module-scope JSX is not allowed. */
const base = (): Pick<
  RookRailProps,
  'brand' | 'sections' | 'workspaceSection' | 'currentPath' | 'account' | 'notifications'
> => ({
  brand: { label: 'Acme Planner home', href: '#planner', icon: <AppMark /> },
  currentPath: '/w/acme/calendar',
  sections: [
    {
      key: 'main',
      items: [
        { key: 'home', label: 'Home', icon: <HomeIcon />, href: '/w/acme/home' },
        { key: 'inbox', label: 'Inbox', icon: <InboxIcon />, href: '/w/acme/inbox', badge: 4 },
        { key: 'calendar', label: 'Calendar', icon: <CalendarIcon />, href: '/w/acme/calendar' },
      ],
    },
  ],
  workspaceSection: {
    key: 'workspace',
    items: [
      { key: 'members', label: 'Members', icon: <UsersIcon />, href: '/w/acme/members' },
      {
        key: 'settings',
        label: 'Settings',
        icon: <GearIcon />,
        href: 'https://id.example.com/settings',
        external: true,
        opensIn: 'Rook ID',
      },
      {
        key: 'billing',
        label: 'Billing',
        icon: <CardIcon />,
        href: 'https://id.example.com/billing',
        external: true,
        opensIn: 'Rook ID',
      },
    ],
  },
  notifications: { label: 'Notifications', href: '#notifications', count: 2, icon: <BellIcon /> },
  account: {
    name: 'Ada Lovelace',
    email: 'ada@example.com',
    status: 'online',
    profileHref: '#profile',
    accountHref: 'https://id.example.com',
    onSignOut: () => {},
  },
});

function CollapsedDemo() {
  const [supportOpen, setSupportOpen] = useState(false);
  const cookies = useMemoryCookies();
  return (
    <Frame
      name="collapsed"
      rail={
        <RookRail
          {...base()}
          persist={false}
          messages={{ nav: 'Main (collapsed example)' }}
          quickActions={[
            {
              key: 'new',
              label: 'New event',
              icon: <PlusIcon />,
              shortcut: 'C',
              onSelect: () => {},
            },
          ]}
          support={{ onOpen: () => setSupportOpen((o) => !o), open: supportOpen, unread: 1 }}
          themeToggle={({ collapsed }: RookRailSlotContext) => (
            <ThemeToggle
              variant="menu"
              size="sm"
              side="right"
              showLabel={!collapsed}
              cookies={cookies}
              applyToDocument={false}
            />
          )}
        />
      }
    />
  );
}

const workspaces = [
  { id: 'w1', name: 'Acme', handle: 'acme' },
  { id: 'w2', name: 'Northfield', handle: 'northwind' },
];
const spaces = [
  { id: 's1', name: 'Engineering' },
  { id: 's2', name: 'Marketing' },
];

function ExpandedDemo() {
  const [space, setSpace] = useState<string | null>('s1');
  return (
    <Frame
      name="expanded"
      rail={
        <RookRail
          {...base()}
          persist={false}
          messages={{ nav: 'Main (expanded example)' }}
          defaultExpanded
          support={{ onOpen: () => {} }}
          workspaceSwitcher={({ collapsed }: RookRailSlotContext) => (
            <WorkspaceSwitcher workspaces={workspaces} defaultValue="w1" collapsed={collapsed} />
          )}
          spaceSwitcher={({ collapsed }: RookRailSlotContext) => (
            <SpaceSwitcher
              spaces={spaces}
              value={space}
              onValueChange={setSpace}
              collapsed={collapsed}
            />
          )}
        />
      }
    />
  );
}

export default {
  id: 'rook-rail',
  title: 'RookRail',
  summary:
    'The shared Rook navigation rail: 48px icons that expand to labels, remembered across apps, with a drawer on phones.',
  sections: {
    purpose: {
      summary:
        'One primary navigation for every Rook app, so people find the same things in the same place everywhere: the app, its sections, the Workspace block, Help & support, the theme, notifications and their account. Apps pass data, links and slots; the rail owns markup, behaviour and accessibility. One shared component instead of a copy in every product.',
      useWhen: [
        'The primary navigation of a signed-in Rook app, inside AppShell.',
        'Any Rook surface that should match the other apps (shared expand state, shared items).',
      ],
      avoidWhen: [
        'Marketing and public pages: use the marketing site shell from `@rookcom/monarch-react/marketing`.',
        'Secondary navigation inside a page (tabs, settings sections): use Tabs or a sidebar list in the page.',
      ],
    },
    designPrinciples: {
      items: [
        'Data in, chrome out: the app supplies items and slots, never markup, so every Rook app keeps one look and one accessibility baseline.',
        'Icons are never the only label: collapsed items keep their label as visually hidden text and show it in a tooltip.',
        'The expand choice is the person’s, not the app’s: it lives in the shared `rook_rail` cookie, and a pre-paint script avoids a flash of the wrong width.',
        'On phones the same navigation moves into a modal drawer with a real focus trap, which the copied rails were missing.',
        'Active state is not colour alone: a raised background, a heavier weight and an inside bar.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        '`rookRailScript({ cookieName? })` returns the `<head>` script that marks `<html data-rook-rail="expanded">` before first paint.',
        '`ROOK_RAIL_COOKIE` (`rook_rail`, values `expanded` | `collapsed`) and `ROOK_RAIL_EVENT` (`rook-rail` on `window`) are the cross-app contract.',
        'Slots (`workspaceSwitcher`, `spaceSwitcher`, `themeToggle`) take a node, or a function of `{ collapsed, placement }` so a switcher can render compactly in the collapsed rail and in full in the drawer.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'RookRailItem: `key`, `label`, `icon`, `href` or `onSelect`, `external`, `opensIn`, `badge`, `active`/`exact`, `shortcut`.',
        'Expand state: uncontrolled and persisted (default), `persist={false}` (in memory), or controlled with `expanded` + `onExpandedChange` (nothing is written).',
        '`cookies` replaces `document.cookie` (tests, embedded frames); `cookieDomain` defaults to `.rookhq.com` on Rook hosts.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'expanded / defaultExpanded',
          values: ['false', 'true'],
          description: '48px icons (default) or 224px with labels (`--mn-layout-rail-*`).',
        },
        {
          prop: 'brand.wordmark',
          values: ['node'],
          description: 'Shown instead of the app icon while expanded and in the drawer.',
        },
      ],
      notes: ['The rail has one visual style: the product accent, so each app keeps its colour.'],
    },
    states: {
      items: [
        {
          name: 'Collapsed / expanded',
          attribute: 'data-state="collapsed|expanded"',
          description:
            'On the root. Before hydration a persisted rail has no state and follows `<html data-rook-rail>` from the pre-paint script.',
        },
        {
          name: 'Active item',
          attribute: 'data-active, aria-current="page"',
          description:
            'From `active`, or `href` matched against `currentPath` (prefix, or `exact`).',
        },
        {
          name: 'Badge',
          description:
            'A count (99+ above 99) while expanded, a dot while collapsed; the count is always in the accessible name.',
        },
        {
          name: 'Help & support open',
          attribute: 'aria-expanded="true", data-open, aria-controls',
          description: 'Styled like the active item while the support panel is open.',
        },
        { name: 'Focus', description: 'An on-accent focus ring inside the item.' },
      ],
    },
    interaction: {
      items: [
        'The expand button toggles labels, saves the choice for a year and tells every rail on the page; a change made in another Rook app is picked up when the tab regains focus.',
        'Tooltips (label, shortcut, “Opens in Rook ID”) show on hover and focus only while collapsed.',
        'External items open in a new tab with `rel="noopener noreferrer"`; internal links use `linkComponent` for client navigation.',
        'Help & support sits directly above the Workspace block and calls `support.onOpen`.',
        'The account button opens a menu: the person, Profile, extra entries, Rook account and Sign out.',
        'Below 768px a top bar replaces the rail; its menu button opens the same navigation in a drawer that closes on Escape, on the backdrop and after choosing an item.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Tab / Shift+Tab',
          action: 'Every item, switcher and button is a normal Tab stop.',
        },
        { keys: '↓ / ↑', action: 'Next / previous item in the rail or drawer (wraps).' },
        { keys: 'Home / End', action: 'First / last item.' },
        {
          keys: 'Enter / Space',
          action: 'Follow a link, press a button, open a menu or switcher.',
        },
        {
          keys: 'Escape',
          action: 'Close a tooltip, menu or the drawer; focus returns to what opened it.',
        },
      ],
    },
    accessibility: {
      role: 'a `navigation` landmark ("Main", `messages.nav`) around the items; the drawer is a modal `dialog` named after the app',
      name: 'Items are named by their label (plus ", 3 unread" and "(opens in Rook ID in a new tab)"); icon-only controls keep visually hidden text.',
      items: [
        'The rail itself is not a `complementary` region (it avoids `<aside>`): only its navigation is a landmark, so it stays valid when embedded and never adds an unnamed region.',
        'Section titles label their lists (`aria-labelledby`), also while visually hidden in the collapsed rail.',
        'The expand button has `aria-expanded` and `aria-controls` pointing at the rail.',
        'Help & support has `aria-haspopup="dialog"`, `aria-expanded` and, while open, `aria-controls`.',
        'The mobile drawer is a Monarch Sheet: focus moves in, is trapped, and returns to the menu button (the gap every audited copy had).',
        '`aria-keyshortcuts` announces item shortcuts; the app implements the keys.',
        'Forced-colours mode outlines the active item; motion is removed under reduced motion.',
      ],
    },
    responsive: {
      items: [
        '768px and up: a sticky full-height rail, 48px or 224px wide, whose middle scrolls on its own (`overscroll-behavior: contain`).',
        'Below 768px: a 56px top bar with the menu button and the app mark; the drawer is 20rem wide, never wider than the screen, with 44px rows.',
        'Slots get `{ collapsed, placement }` and can switch to their compact form.',
      ],
    },
    theme: {
      items: [
        'The panel is the product accent (`--mn-color-accent-solid` with `-on-solid` text), so it follows the product, light, dark and more-contrast themes.',
        'The drawer uses surface tokens, like other sheets.',
        'Pair `rookRailScript()` with the tokens `appearanceScript()` in `<head>`.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Collapsed, with Help & support and a theme toggle',
          stack: true,
          render: () => <CollapsedDemo />,
        },
        {
          title: 'Expanded, with workspace and Space switchers',
          stack: true,
          render: () => <ExpandedDemo />,
        },
      ],
    },
    doDont: {
      do: [
        'Render `rookRailScript()` in `<head>` and keep the default cookie, so the rail width is right on first paint and follows people between apps.',
        'Pass the router’s link as `linkComponent` and the current path as `currentPath`.',
        'Put workspace administration (settings, members, billing) in `workspaceSection`, so Help & support lands above it as in every app.',
        'Give every item a short, unique label: it is the tooltip and the accessible name.',
      ],
      dont: [
        'Do not fork the rail’s markup or style its internals from the app; ask for a slot or prop instead.',
        'Do not hide important items only in the expanded state; the collapsed rail must offer every destination.',
        'Do not use a badge for anything but a count people should act on.',
      ],
    },
    related: {
      items: [
        { id: 'app-shell', reason: 'Places the rail beside the identity bar and main region.' },
        { id: 'workspace-switcher', reason: 'The workspace slot.' },
        { id: 'space-switcher', reason: 'The Space slot.' },
        { id: 'theme-toggle', reason: 'The theme slot.' },
        { id: 'support-panel', reason: 'What Help & support opens.' },
        { id: 'sheet', reason: 'The mobile drawer.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['avatar', 'dropdown-menu', 'kbd', 'sheet', 'tooltip'],
    },
    testing: {
      unit: [
        'Main navigation landmark; active item by path; brand home link.',
        'Badges and external links in the accessible name; new-tab links are safe.',
        'Workspace block under its heading with Help & support right above it; support button popup state and panel id.',
        'Expand/collapse writes the `rook_rail` cookie (name, domain, max-age), mirrors `<html data-rook-rail>` and syncs other rails; starts from the saved state.',
        'Controlled mode writes nothing; `persist={false}` keeps state locally.',
        'Arrow keys, Home and End move focus; tooltips with label and shortcut while collapsed.',
        'Slots get their context; account menu with profile, Rook account and Sign out.',
        'Mobile drawer: focus moves in, closes after navigating and on Escape, focus returns.',
        'Link component for internal links only; translated messages; server render and `rookRailScript()`.',
      ],
      axe: true,
      ssr: true,
      browser: [
        'Visual baselines: /__gallery/rook-rail',
        'Cross-browser axe: /components/rook-rail',
      ],
    },
  },
} satisfies ComponentDocs;
