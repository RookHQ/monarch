'use client';

import {
  forwardRef,
  useCallback,
  useId,
  useMemo,
  useState,
  type HTMLAttributes,
  type KeyboardEvent,
  type ReactNode,
} from 'react';
import { Tooltip, TooltipProvider } from '../../../components/overlays/tooltip/index.js';
import {
  Sheet,
  SheetBody,
  SheetContent,
  SheetTitle,
  SheetTrigger,
} from '../../../components/overlays/sheet/index.js';
import { Kbd } from '../../../components/typography/kbd/index.js';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuTrigger,
} from '../../../components/overlays/dropdown-menu/index.js';
import { Avatar } from '../../../components/identity/avatar/index.js';
import { cx } from '../../../internal/cx.js';
import { AccountMenuItems, type ShellAccount } from '../../../internal/shell/account-menu.js';
import { documentCookieStore, type ShellCookieStore } from '../../../internal/shell/cookies.js';
import {
  BellGlyph,
  ExternalGlyph,
  HelpGlyph,
  MenuGlyph,
  PanelCloseGlyph,
  PanelOpenGlyph,
} from '../../../internal/shell/icons.js';
import { ShellLink, pathMatches, type ShellLinkComponent } from '../../../internal/shell/link.js';
import { ROOK_RAIL_COOKIE, useRailState } from './rook-rail.state.js';

export { ROOK_RAIL_COOKIE, ROOK_RAIL_EVENT, rookRailScript } from './rook-rail.state.js';
export type { RookRailScriptOptions } from './rook-rail.state.js';

export interface RookRailItem {
  /** Stable key, unique within the rail. */
  key: string;
  /** Visible label when expanded; tooltip and accessible name when collapsed. */
  label: string;
  /** Decorative icon (16–18px). The label names the item. */
  icon: ReactNode;
  /** Link target. Without it the item is a button that calls `onSelect`. */
  href?: string | undefined;
  /** Open `href` in a new tab (another app or site). */
  external?: boolean | undefined;
  /**
   * The product an external link leaves for, e.g. "Rook ID". Adds the
   * external marker and "Opens in Rook ID" to the tooltip and the accessible name.
   */
  opensIn?: string | undefined;
  /** Called on activation (for links: before navigating). */
  onSelect?: (() => void) | undefined;
  /** Count badge; hidden when 0 or missing. */
  badge?: number | undefined;
  /** Explicit active state; otherwise `href` is matched against `currentPath`. */
  active?: boolean | undefined;
  /** Match `currentPath` exactly instead of by prefix. */
  exact?: boolean | undefined;
  /** Keyboard shortcut shown in the tooltip, e.g. "C". The app handles the key itself. */
  shortcut?: string | undefined;
}

export interface RookRailSection {
  key: string;
  /** Heading shown above the items while expanded (and kept for screen readers while collapsed). */
  title?: string | undefined;
  items: RookRailItem[];
}

export interface RookRailBrand {
  /** The current app, e.g. "Rook Calendar". Names the home link. */
  label: string;
  href: string;
  /** App icon (about 28px), shown collapsed. Decorative. */
  icon: ReactNode;
  /** Wordmark shown instead of the icon while expanded and in the drawer. Decorative. */
  wordmark?: ReactNode;
}

export interface RookRailSupport {
  /** Opens the Help & support panel. */
  onOpen: () => void;
  /** Whether the panel is open (`aria-expanded`). */
  open?: boolean | undefined;
  /** Unread replies from support. */
  unread?: number | undefined;
  /** Id of the panel element, for `aria-controls` while it is open. */
  panelId?: string | undefined;
}

export interface RookRailNotifications {
  label: string;
  href?: string | undefined;
  onSelect?: (() => void) | undefined;
  count?: number | undefined;
  icon?: ReactNode;
}

/** The signed-in person: avatar, account menu and Sign out. */
export type RookRailAccount = ShellAccount;

/** What a slot render function is told about where it renders. */
export interface RookRailSlotContext {
  /** True in the collapsed 48px rail; false when expanded and in the mobile drawer. */
  collapsed: boolean;
  placement: 'rail' | 'drawer';
}

export type RookRailSlot = ReactNode | ((context: RookRailSlotContext) => ReactNode);

export interface RookRailMessages {
  /**
   * Name of the rail's navigation landmark. Default "Main". Give each rail on
   * a page its own name when there is more than one (docs, previews).
   */
  nav: string;
  expand: string;
  collapse: string;
  openMenu: string;
  closeMenu: string;
  support: string;
  workspace: string;
  signOut: string;
  profile: string;
  account: string;
  /** Account menu button (the identity bar's avatar-only button). */
  accountMenu: (name: string) => string;
  /** Read before the person's name on the rail's account button. Default "Account: ". */
  accountMenuPrefix: string;
  /** Appended to an item with a badge. */
  unread: (count: number) => string;
  /** Appended to external links. */
  newTab: (product?: string) => string;
  /** Tooltip hint of an external link. */
  opensIn: (product: string) => string;
}

const DEFAULT_MESSAGES: RookRailMessages = {
  nav: 'Main',
  expand: 'Expand navigation',
  collapse: 'Collapse navigation',
  openMenu: 'Open navigation',
  closeMenu: 'Close navigation',
  support: 'Help & support',
  workspace: 'Workspace',
  signOut: 'Sign out',
  profile: 'Profile and preferences',
  account: 'Rook account',
  accountMenu: (name) => `Account: ${name}`,
  accountMenuPrefix: 'Account: ',
  unread: (n) => `${n} unread`,
  newTab: (product) => (product ? `opens in ${product} in a new tab` : 'opens in a new tab'),
  opensIn: (product) => `Opens in ${product}`,
};

export interface RookRailProps extends Omit<HTMLAttributes<HTMLDivElement>, 'children'> {
  /** The current app: its icon, wordmark and home link. */
  brand: RookRailBrand;
  /** The app's own navigation, in order. */
  sections: RookRailSection[];
  /**
   * The Workspace block: workspace settings, members, billing and similar
   * (usually links into Rook ID). Rendered after the sections, under its title,
   * with Help & support directly above it.
   */
  workspaceSection?: RookRailSection | undefined;
  /** Frequent actions above the expand toggle (New, Search). Listed first in the drawer. */
  quickActions?: RookRailItem[] | undefined;
  /** Workspace switcher, under the brand. */
  workspaceSwitcher?: RookRailSlot;
  /** Space switcher, under the workspace switcher. */
  spaceSwitcher?: RookRailSlot;
  /** Theme toggle, above the account (and in the drawer footer). */
  themeToggle?: RookRailSlot;
  /** The Help & support entry. `false` or missing hides it. */
  support?: RookRailSupport | false | undefined;
  notifications?: RookRailNotifications | undefined;
  /** The signed-in person: avatar, account menu and Sign out. */
  account?: RookRailAccount | undefined;
  /** The current path, matched against item hrefs for the active item. */
  currentPath?: string | undefined;
  /** Link element for client navigation (Next.js `Link`, …). Default `<a>`. */
  linkComponent?: ShellLinkComponent | undefined;
  /** Expanded (controlled). */
  expanded?: boolean | undefined;
  /** Initial state when nothing is saved, and for the server render. Default false (48px). */
  defaultExpanded?: boolean | undefined;
  onExpandedChange?: ((expanded: boolean) => void) | undefined;
  /** Remember the state in the `rook_rail` cookie, shared across Rook apps. Default true. */
  persist?: boolean | undefined;
  /** Default `rook_rail`. */
  cookieName?: string | undefined;
  /** Default: `.rookhq.com` on Rook hosts, otherwise the current host. */
  cookieDomain?: string | undefined;
  /** Where the state is stored. Default `document.cookie`. */
  cookies?: ShellCookieStore | undefined;
  /** The mobile drawer (controlled). */
  drawerOpen?: boolean | undefined;
  onDrawerOpenChange?: ((open: boolean) => void) | undefined;
  /** Interface text, for translation. */
  messages?: Partial<RookRailMessages> | undefined;
}

const renderSlot = (slot: RookRailSlot, ctx: RookRailSlotContext) =>
  typeof slot === 'function' ? slot(ctx) : slot;

const ITEM_SELECTOR = '[data-rail-item]';

/** Up/Down, Home/End move focus between the rail's items (Tab still works). */
function onArrowKeys(e: KeyboardEvent<HTMLElement>) {
  const keys = ['ArrowDown', 'ArrowUp', 'Home', 'End'];
  if (!keys.includes(e.key) || e.altKey || e.ctrlKey || e.metaKey) return;
  const target = e.target as HTMLElement;
  if (!target.matches(ITEM_SELECTOR)) return;
  const items = [...e.currentTarget.querySelectorAll<HTMLElement>(ITEM_SELECTOR)].filter(
    (el) => !el.hasAttribute('disabled') && el.offsetParent !== null,
  );
  const all = items.length
    ? items
    : [...e.currentTarget.querySelectorAll<HTMLElement>(ITEM_SELECTOR)];
  const i = all.indexOf(target);
  if (i < 0) return;
  const next =
    e.key === 'Home'
      ? all[0]
      : e.key === 'End'
        ? all[all.length - 1]
        : all[(i + (e.key === 'ArrowDown' ? 1 : -1) + all.length) % all.length];
  if (!next) return;
  e.preventDefault();
  next.focus();
}

interface ItemProps {
  item: RookRailItem;
  collapsed: boolean;
  placement: 'rail' | 'drawer';
  currentPath: string | undefined;
  linkComponent: ShellLinkComponent | undefined;
  messages: RookRailMessages;
  /** Extra props for the control (aria-haspopup, …). */
  control?: Record<string, unknown> | undefined;
  /** Called after activation (the drawer closes itself). */
  onActivate?: (() => void) | undefined;
}

function RailItemControl({
  item,
  collapsed,
  placement,
  currentPath,
  linkComponent,
  messages,
  control,
  onActivate,
}: ItemProps) {
  const active =
    item.active ??
    (item.href && !item.external && currentPath !== undefined
      ? pathMatches(item.href, currentPath, item.exact)
      : false);
  const badge = item.badge && item.badge > 0 ? item.badge : 0;
  const inner = (
    <>
      <span className="mn-rook-rail__icon" aria-hidden>
        {item.icon}
      </span>
      <span className="mn-rook-rail__label">{item.label}</span>
      {badge ? (
        <span className="mn-rook-rail__badge" aria-hidden>
          {badge > 99 ? '99+' : badge}
        </span>
      ) : null}
      {item.opensIn || item.external ? (
        <span className="mn-rook-rail__external" aria-hidden>
          <ExternalGlyph />
        </span>
      ) : null}
      {badge ? <span className="mn-visually-hidden">, {messages.unread(badge)}</span> : null}
      {item.href && item.external ? (
        <span className="mn-visually-hidden"> ({messages.newTab(item.opensIn)})</span>
      ) : null}
    </>
  );
  const common = {
    className: 'mn-rook-rail__item',
    'data-rail-item': '',
    'data-active': active ? '' : undefined,
    'aria-keyshortcuts': item.shortcut,
    ...control,
  };
  const activate = () => {
    item.onSelect?.();
    onActivate?.();
  };
  const el = item.href ? (
    <ShellLink
      as={item.external ? undefined : linkComponent}
      href={item.href}
      {...common}
      {...(item.external ? { target: '_blank', rel: 'noopener noreferrer' } : {})}
      aria-current={active ? 'page' : undefined}
      onClick={activate}
    >
      {inner}
    </ShellLink>
  ) : (
    <button type="button" {...common} aria-current={active ? 'true' : undefined} onClick={activate}>
      {inner}
    </button>
  );
  if (placement === 'drawer') return el;
  const hint = item.opensIn ? messages.opensIn(item.opensIn) : undefined;
  return (
    <Tooltip
      side="right"
      {...(collapsed ? {} : { open: false })}
      content={
        <span className="mn-rook-rail__tip">
          {item.label}
          {item.shortcut ? <Kbd>{item.shortcut}</Kbd> : null}
          {hint ? <span className="mn-rook-rail__tip-hint">{hint}</span> : null}
        </span>
      }
    >
      {el}
    </Tooltip>
  );
}

interface ListProps extends Omit<ItemProps, 'item'> {
  section: RookRailSection;
  before?: ReactNode;
}

function SectionList({ section, before, ...rest }: ListProps) {
  const headingId = useId();
  return (
    <div className="mn-rook-rail__section" data-section={section.key}>
      {before}
      {section.title ? (
        <p className="mn-rook-rail__section-title" id={headingId}>
          {section.title}
        </p>
      ) : null}
      <ul
        className="mn-rook-rail__list"

        aria-labelledby={section.title ? headingId : undefined}
      >
        {section.items.map((item) => (
          <li key={item.key}>
            <RailItemControl item={item} {...rest} />
          </li>
        ))}
      </ul>
    </div>
  );
}

function AccountMenu({
  account,
  collapsed,
  placement,
  messages,
}: {
  account: RookRailAccount;
  collapsed: boolean;
  placement: 'rail' | 'drawer';
  messages: RookRailMessages;
}) {
  const trigger = (
    <DropdownMenuTrigger className="mn-rook-rail__item mn-rook-rail__account" data-rail-item="">
      {/* Named by its text ("Account: Ada Lovelace ada@…"), so the visible name is part of it. */}
      <span className="mn-visually-hidden">{messages.accountMenuPrefix}</span>
      <Avatar
        name={account.name}
        src={account.avatarUrl ?? null}
        size={placement === 'drawer' ? 28 : 24}
        decorative
        {...(account.status ? { status: account.status } : {})}
      />
      <span className="mn-rook-rail__label mn-rook-rail__identity">
        <span className="mn-rook-rail__name">{account.name}</span>{' '}
        {account.email ? <span className="mn-rook-rail__email">{account.email}</span> : null}
      </span>
    </DropdownMenuTrigger>
  );
  return (
    <DropdownMenu>
      {placement === 'rail' ? (
        <Tooltip side="right" content={account.name} {...(collapsed ? {} : { open: false })}>
          {trigger}
        </Tooltip>
      ) : (
        trigger
      )}
      <DropdownMenuContent
        side={placement === 'rail' ? 'right' : 'top'}
        align="end"
        sideOffset={8}
        className="mn-rook-rail__account-menu"
      >
        <AccountMenuItems account={account} messages={messages} />
      </DropdownMenuContent>
    </DropdownMenu>
  );
}

/**
 * The Rook primary navigation rail, shared by every Rook app: a 48px icon rail
 * that expands to labels (remembered across apps in the `rook_rail` cookie),
 * with the app's brand, workspace and Space switchers, the app's sections, the
 * Workspace block, Help & support, a theme toggle slot, notifications and the
 * account menu. Below 768px it becomes a top bar whose menu button opens the
 * same navigation in a drawer (a modal Sheet: focus moves in, is trapped and
 * returns to the button). Apps pass data, links and slots; the markup,
 * behaviour and accessibility are the rail's.
 */
export const RookRail = forwardRef<HTMLDivElement, RookRailProps>(function RookRail(
  {
    brand,
    sections,
    workspaceSection,
    quickActions,
    workspaceSwitcher,
    spaceSwitcher,
    themeToggle,
    support,
    notifications,
    account,
    currentPath,
    linkComponent,
    expanded: expandedProp,
    defaultExpanded = false,
    onExpandedChange,
    persist = true,
    cookieName = ROOK_RAIL_COOKIE,
    cookieDomain,
    cookies = documentCookieStore,
    drawerOpen: drawerOpenProp,
    onDrawerOpenChange,
    messages: messagesProp,
    className,
    id,
    ...rest
  },
  ref,
) {
  const messages = useMemo(() => ({ ...DEFAULT_MESSAGES, ...messagesProp }), [messagesProp]);
  const { expanded, setExpanded, ready } = useRailState({
    expanded: expandedProp,
    defaultExpanded,
    onExpandedChange,
    persist,
    cookieName,
    cookieDomain,
    cookies,
  });
  const autoId = useId();
  const panelId = id ?? `mn-rook-rail-${autoId.replace(/:/g, '')}`;
  const [drawerLocal, setDrawerLocal] = useState(false);
  const drawerOpen = drawerOpenProp ?? drawerLocal;
  const setDrawerOpen = useCallback(
    (next: boolean) => {
      if (drawerOpenProp === undefined) setDrawerLocal(next);
      onDrawerOpenChange?.(next);
    },
    [drawerOpenProp, onDrawerOpenChange],
  );

  const supportItem: RookRailItem | null = support
    ? {
        key: 'mn-support',
        label: messages.support,
        icon: <HelpGlyph />,
        onSelect: support.onOpen,
        badge: support.unread,
        active: false,
      }
    : null;
  const supportControl = support
    ? {
        'aria-haspopup': 'dialog',
        'aria-expanded': support.open ?? false,
        'aria-controls': support.open ? support.panelId : undefined,
        'data-rook-support-trigger': '',
        'data-open': support.open ? '' : undefined,
      }
    : undefined;
  const notificationItem: RookRailItem | null = notifications
    ? {
        key: 'mn-notifications',
        label: notifications.label,
        icon: notifications.icon ?? <BellGlyph />,
        href: notifications.href,
        onSelect: notifications.onSelect,
        badge: notifications.count,
        active: false,
      }
    : null;

  const navigation = (placement: 'rail' | 'drawer') => {
    const collapsed = placement === 'rail' && !expanded;
    const shared = {
      collapsed,
      placement,
      currentPath,
      linkComponent,
      messages,
      onActivate: placement === 'drawer' ? () => setDrawerOpen(false) : undefined,
    };
    const ctx: RookRailSlotContext = { collapsed, placement };
    const supportBlock = supportItem ? (
      <ul className="mn-rook-rail__list">
        <li>
          <RailItemControl item={supportItem} {...shared} control={supportControl} />
        </li>
      </ul>
    ) : null;
    return (
      <>
        {workspaceSwitcher || spaceSwitcher ? (
          <div className="mn-rook-rail__switchers">
            {workspaceSwitcher ? (
              <div className="mn-rook-rail__slot" data-slot="workspace">
                {renderSlot(workspaceSwitcher, ctx)}
              </div>
            ) : null}
            {spaceSwitcher ? (
              <div className="mn-rook-rail__slot" data-slot="space">
                {renderSlot(spaceSwitcher, ctx)}
              </div>
            ) : null}
          </div>
        ) : null}
        <div className="mn-rook-rail__scroll">
          <nav aria-label={messages.nav} className="mn-rook-rail__nav">
            {placement === 'drawer' && quickActions?.length ? (
              <SectionList section={{ key: 'mn-quick', items: quickActions }} {...shared} />
            ) : null}
            {sections.map((section) => (
              <SectionList key={section.key} section={section} {...shared} />
            ))}
            {workspaceSection ? (
              <SectionList
                section={{
                  ...workspaceSection,
                  title: workspaceSection.title ?? messages.workspace,
                }}
                before={supportBlock}
                {...shared}
              />
            ) : (
              supportBlock && <div className="mn-rook-rail__section">{supportBlock}</div>
            )}
          </nav>
        </div>
        <div className="mn-rook-rail__footer">
          {placement === 'rail' && quickActions?.length ? (
            <ul className="mn-rook-rail__list">
              {quickActions.map((item) => (
                <li key={item.key}>
                  <RailItemControl item={item} {...shared} />
                </li>
              ))}
            </ul>
          ) : null}
          {themeToggle ? (
            <div className="mn-rook-rail__slot" data-slot="theme">
              {renderSlot(themeToggle, ctx)}
            </div>
          ) : null}
          <ul className="mn-rook-rail__list">
            {placement === 'rail' ? (
              <li>
                <Tooltip
                  side="right"
                  content={messages.expand}
                  {...(collapsed ? {} : { open: false })}
                >
                  <button
                    type="button"
                    className="mn-rook-rail__item mn-rook-rail__toggle"
                    data-rail-item=""
                    aria-expanded={expanded}
                    aria-controls={panelId}
                    onClick={() => setExpanded(!expanded)}
                  >
                    <span className="mn-rook-rail__icon" aria-hidden>
                      {expanded ? <PanelCloseGlyph /> : <PanelOpenGlyph />}
                    </span>
                    <span className="mn-rook-rail__label">
                      {expanded ? messages.collapse : messages.expand}
                    </span>
                  </button>
                </Tooltip>
              </li>
            ) : null}
            {notificationItem ? (
              <li>
                <RailItemControl item={notificationItem} {...shared} />
              </li>
            ) : null}
            {account ? (
              <li>
                <AccountMenu
                  account={account}
                  collapsed={collapsed}
                  placement={placement}
                  messages={messages}
                />
              </li>
            ) : null}
          </ul>
        </div>
      </>
    );
  };

  const brandLink = (placement: 'rail' | 'drawer' | 'bar') => (
    <ShellLink
      as={linkComponent}
      href={brand.href}
      className="mn-rook-rail__brand"
      data-rail-item={placement === 'bar' ? undefined : ''}
      aria-label={brand.label}
      onClick={placement === 'drawer' ? () => setDrawerOpen(false) : undefined}
    >
      {brand.wordmark ? (
        <>
          <span className="mn-rook-rail__brand-icon" aria-hidden>
            {brand.icon}
          </span>
          <span className="mn-rook-rail__brand-wordmark" aria-hidden>
            {brand.wordmark}
          </span>
        </>
      ) : (
        <span className="mn-rook-rail__brand-icon" aria-hidden>
          {brand.icon}
        </span>
      )}
    </ShellLink>
  );

  return (
    <div
      ref={ref}
      {...rest}
      className={cx('mn-rook-rail', className)}
      // Before hydration a persisted rail has no state yet: its CSS follows
      // <html data-rook-rail> from the pre-paint script, so the first paint is right.
      data-state={
        persist && expandedProp === undefined && !ready
          ? undefined
          : expanded
            ? 'expanded'
            : 'collapsed'
      }
      data-wordmark={brand.wordmark ? '' : undefined}
    >
      <TooltipProvider delayDuration={100}>
        {/* No landmark of its own: the rail sits beside <main> and may be embedded in
            another page, so only its <nav> is a landmark (a complementary region must be
            top level). Arrow keys are delegated to the focusable items inside. */}
        {/* eslint-disable-next-line jsx-a11y/no-static-element-interactions */}
        <div id={panelId} className="mn-rook-rail__panel" onKeyDown={onArrowKeys}>
          <div className="mn-rook-rail__head">{brandLink('rail')}</div>
          {navigation('rail')}
        </div>
      </TooltipProvider>
      <div className="mn-rook-rail__bar">
        <Sheet open={drawerOpen} onOpenChange={setDrawerOpen}>
          <SheetTrigger asChild>
            <button
              type="button"
              className="mn-rook-rail__menu-button"
              aria-label={messages.openMenu}
            >
              <MenuGlyph />
            </button>
          </SheetTrigger>
          <SheetContent
            side="left"
            size="sm"
            closeLabel={messages.closeMenu}
            className="mn-rook-rail__drawer"
            aria-describedby={undefined}
          >
            <div className="mn-rook-rail__drawer-head">
              {brandLink('drawer')}
              <SheetTitle className="mn-visually-hidden">{brand.label}</SheetTitle>
            </div>
            <SheetBody className="mn-rook-rail__drawer-body" onKeyDown={onArrowKeys}>
              {navigation('drawer')}
            </SheetBody>
          </SheetContent>
        </Sheet>
        {brandLink('bar')}
      </div>
    </div>
  );
});
