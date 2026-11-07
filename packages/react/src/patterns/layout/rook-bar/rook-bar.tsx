'use client';

import {
  forwardRef,
  useCallback,
  useEffect,
  useId,
  useMemo,
  useRef,
  useState,
  type CSSProperties,
  type HTMLAttributes,
  type PointerEvent as ReactPointerEvent,
  type ReactNode,
} from 'react';
import { Avatar } from '../../../components/identity/avatar/index.js';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '../../../components/overlays/dropdown-menu/index.js';
import { cx } from '../../../internal/cx.js';
import { ChevronDownGlyph, CloseGlyph, MenuGlyph } from '../../../internal/icons.js';
import { SignOutGlyph } from '../../../internal/shell/icons.js';
import { ShellLink, type ShellLinkComponent } from '../../../internal/shell/link.js';
import { RookBarPanelProvider, type RookBarPanelContext } from './rook-bar.context.js';

export * from './rook-bar.context.js';
export * from './rook-bar.panels.js';

/** A top-level link in the bar. */
export interface RookBarLinkItem {
  label: string;
  href: string;
  /** Another site: opens in a new tab. */
  external?: boolean | undefined;
}

/** A top-level menu: a button that opens `panel` under the bar (desktop) or a section of the sheet (mobile). */
export interface RookBarMenuItem {
  /** Stable id, used for the panel's element id. */
  id: string;
  label: string;
  /**
   * The menu's content: RookBarAppCatalog, RookBarFeaturePanel,
   * RookBarStoriesPanel, RookBarLinkList or your own. Read the layout and
   * `close` with `useRookBarPanel()`.
   */
  panel: ReactNode;
  /** Called when the menu opens (desktop or mobile): load its data here. */
  onOpen?: (() => void) | undefined;
}

export type RookBarItem = RookBarLinkItem | RookBarMenuItem;

const isMenu = (item: RookBarItem): item is RookBarMenuItem => 'panel' in item;

/** Who is signed in, for the account controls at the end of the bar. */
export interface RookBarAccount {
  /** `loading` renders nothing there, so the server and first client render match. */
  status: 'loading' | 'signed-out' | 'signed-in';
  /** The signed-in person's name (avatar initials and menu name). */
  name?: string | undefined;
  avatarUrl?: string | null | undefined;
  /** Signed in: "Open dashboard" link (the app's home). */
  dashboardHref?: string | undefined;
  /** Signed in: account settings (opens in a new tab). */
  accountHref?: string | undefined;
  /** Signed in: Sign out. */
  onSignOut?: (() => void) | undefined;
  /** Signed out: "Log in". */
  signInHref?: string | undefined;
  /** Signed out: "Sign up", the primary button. */
  signUpHref?: string | undefined;
}

export interface RookBarMessages {
  /** Name of the navigation landmark. Default "Rook". */
  nav: string;
  openMenu: string;
  closeMenu: string;
  /** Account menu button. */
  accountMenu: string;
  dashboard: string;
  account: string;
  signOut: string;
  signIn: string;
  signUp: string;
  /** Appended to links that open in a new tab. */
  newTab: string;
}

const DEFAULT_MESSAGES: RookBarMessages = {
  nav: 'Rook',
  openMenu: 'Open menu',
  closeMenu: 'Close menu',
  accountMenu: 'Account menu',
  dashboard: 'Open dashboard',
  account: 'Account settings',
  signOut: 'Sign out',
  signIn: 'Log in',
  signUp: 'Sign up',
  newTab: 'opens in a new tab',
};

export interface RookBarProps extends Omit<HTMLAttributes<HTMLDivElement>, 'children'> {
  /** The wordmark, normally a link home with an `<img alt="…">`. */
  brand: ReactNode;
  /** Top-level links and menus, in order. */
  items: RookBarItem[];
  /** Account controls at the end. Omit for none. */
  account?: RookBarAccount | undefined;
  /** Link element for internal links (Next.js `Link`, …). Default `<a>`. */
  linkComponent?: ShellLinkComponent | undefined;
  /** The current path: when it changes, open menus close. */
  currentPath?: string | undefined;
  /** `sticky` (default) keeps the bar at the top of the scrolling area; `static` scrolls away. */
  position?: 'sticky' | 'static' | undefined;
  /** A thin accent line along the bottom showing how far the page has scrolled. Default false. */
  scrollProgress?: boolean | undefined;
  /** Delay before a menu opens on hover, in ms. Default 120. */
  openDelay?: number | undefined;
  /** Delay before a hovered menu closes after the pointer leaves, in ms. Default 200. */
  closeDelay?: number | undefined;
  /** Interface text, for translation. */
  messages?: Partial<RookBarMessages> | undefined;
}

/**
 * The Rook company bar at the top of each app's public pages: wordmark, menus
 * that open wide panels (hover intent on desktop, click and keyboard
 * everywhere), plain links and the account controls. Under 64rem the items
 * move into a full-height sheet with collapsible sections. Every link, app and
 * post is data from the app; the bar never fetches.
 */
export const RookBar = forwardRef<HTMLDivElement, RookBarProps>(function RookBar(
  {
    brand,
    items,
    account,
    linkComponent,
    currentPath,
    position = 'sticky',
    scrollProgress = false,
    openDelay = 120,
    closeDelay = 200,
    messages: messagesProp,
    className,
    style,
    ...rest
  },
  ref,
) {
  const messages = useMemo(() => ({ ...DEFAULT_MESSAGES, ...messagesProp }), [messagesProp]);
  const baseId = useId();
  const sheetId = `${baseId}-sheet`;
  const [active, setActive] = useState<string | null>(null);
  const [sheetOpen, setSheetOpen] = useState(false);
  const [mobileSection, setMobileSection] = useState<string | null>(null);
  const [scrolled, setScrolled] = useState(false);
  const [progress, setProgress] = useState(0);
  const rootRef = useRef<HTMLDivElement | null>(null);
  const triggerRefs = useRef(new Map<string, HTMLButtonElement>());
  const toggleRef = useRef<HTMLButtonElement>(null);
  const openTimer = useRef<ReturnType<typeof setTimeout> | undefined>(undefined);
  const closeTimer = useRef<ReturnType<typeof setTimeout> | undefined>(undefined);

  const activeRef = useRef<string | null>(null);
  activeRef.current = active;

  const clearTimers = () => {
    clearTimeout(openTimer.current);
    clearTimeout(closeTimer.current);
  };
  const openMenu = (id: string | null) => {
    clearTimers();
    if (id && id !== activeRef.current) {
      const menu = items.find((item): item is RookBarMenuItem => isMenu(item) && item.id === id);
      menu?.onOpen?.();
    }
    activeRef.current = id;
    setActive(id);
  };
  const closeAll = useCallback(() => {
    clearTimeout(openTimer.current);
    clearTimeout(closeTimer.current);
    setActive(null);
    setSheetOpen(false);
  }, []);

  // Route change: close during render, so the new page never paints with a menu open.
  const [lastPath, setLastPath] = useState(currentPath);
  if (currentPath !== lastPath) {
    setLastPath(currentPath);
    setActive(null);
    setSheetOpen(false);
  }

  // Escape closes and returns focus; a press outside or leaving the window closes the desktop menu.
  useEffect(() => {
    if (!active && !sheetOpen) return;
    const onKey = (e: KeyboardEvent) => {
      if (e.key !== 'Escape' || e.defaultPrevented) return;
      if (active) {
        const trigger = triggerRefs.current.get(active);
        setActive(null);
        trigger?.focus();
      } else if (sheetOpen) {
        setSheetOpen(false);
        toggleRef.current?.focus();
      }
    };
    const onPointerDown = (e: PointerEvent) => {
      if (active && !rootRef.current?.contains(e.target as Node)) setActive(null);
    };
    const onBlur = () => setActive(null);
    document.addEventListener('keydown', onKey);
    document.addEventListener('pointerdown', onPointerDown);
    window.addEventListener('blur', onBlur);
    return () => {
      document.removeEventListener('keydown', onKey);
      document.removeEventListener('pointerdown', onPointerDown);
      window.removeEventListener('blur', onBlur);
    };
  }, [active, sheetOpen]);

  // The mobile sheet owns scrolling while it is open.
  useEffect(() => {
    if (!sheetOpen) return;
    const previous = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    return () => {
      document.body.style.overflow = previous;
    };
  }, [sheetOpen]);

  // Solid background once scrolled, and the optional progress line.
  useEffect(() => {
    const onScroll = () => {
      const top = window.scrollY;
      setScrolled(top > 8);
      if (scrollProgress) {
        const max = document.documentElement.scrollHeight - window.innerHeight;
        setProgress(max > 0 ? Math.min(1, top / max) : 0);
      }
    };
    onScroll();
    window.addEventListener('scroll', onScroll, { passive: true });
    return () => window.removeEventListener('scroll', onScroll);
  }, [scrollProgress]);

  useEffect(() => () => clearTimers(), []);

  const hoverOpen = (id: string, e: ReactPointerEvent) => {
    if (e.pointerType !== 'mouse') return;
    clearTimers();
    if (active) openMenu(id);
    else openTimer.current = setTimeout(() => openMenu(id), openDelay);
  };
  const scheduleClose = (e: ReactPointerEvent) => {
    if (e.pointerType !== 'mouse') return;
    clearTimers();
    closeTimer.current = setTimeout(() => setActive(null), closeDelay);
  };
  const keepOpen = () => clearTimeout(closeTimer.current);

  const desktopCtx: RookBarPanelContext = { layout: 'desktop', close: closeAll, linkComponent };
  const mobileCtx: RookBarPanelContext = { layout: 'mobile', close: closeAll, linkComponent };

  const renderLink = (item: RookBarLinkItem, className: string) =>
    item.external ? (
      <a
        className={className}
        href={item.href}
        target="_blank"
        rel="noopener noreferrer"
        onClick={closeAll}
      >
        {item.label}
        <span className="mn-visually-hidden"> ({messages.newTab})</span>
      </a>
    ) : (
      <ShellLink as={linkComponent} className={className} href={item.href} onClick={closeAll}>
        {item.label}
      </ShellLink>
    );

  const signedIn = account?.status === 'signed-in';
  const signedOut = account?.status === 'signed-out';

  const accountDesktop = signedIn ? (
    <DropdownMenu>
      <DropdownMenuTrigger className="mn-rook-bar__avatar">
        <span className="mn-visually-hidden">{messages.accountMenu}</span>
        <Avatar name={account.name ?? ''} src={account.avatarUrl ?? null} size={32} decorative />
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end" sideOffset={8} className="mn-rook-bar__account-menu">
        {account.dashboardHref ? (
          <DropdownMenuItem asChild>
            <ShellLink as={linkComponent} href={account.dashboardHref}>
              {messages.dashboard}
            </ShellLink>
          </DropdownMenuItem>
        ) : null}
        {account.accountHref ? (
          <DropdownMenuItem asChild>
            <a href={account.accountHref} target="_blank" rel="noopener noreferrer">
              {messages.account}
              <span className="mn-visually-hidden"> ({messages.newTab})</span>
            </a>
          </DropdownMenuItem>
        ) : null}
        {account.onSignOut ? (
          <>
            {account.dashboardHref || account.accountHref ? <DropdownMenuSeparator /> : null}
            <DropdownMenuItem icon={<SignOutGlyph />} onSelect={account.onSignOut}>
              {messages.signOut}
            </DropdownMenuItem>
          </>
        ) : null}
      </DropdownMenuContent>
    </DropdownMenu>
  ) : signedOut ? (
    <>
      {account.signInHref ? (
        <a className="mn-rook-bar__action" href={account.signInHref}>
          {messages.signIn}
        </a>
      ) : null}
      {account.signUpHref ? (
        <a className="mn-rook-bar__action" data-primary="" href={account.signUpHref}>
          {messages.signUp}
        </a>
      ) : null}
    </>
  ) : null;

  const accountMobile = signedIn ? (
    <>
      {account.dashboardHref ? (
        <ShellLink
          as={linkComponent}
          className="mn-rook-bar__action"
          data-primary=""
          data-wide=""
          href={account.dashboardHref}
          onClick={closeAll}
        >
          {messages.dashboard}
        </ShellLink>
      ) : null}
      {account.accountHref ? (
        <a
          className="mn-rook-bar__action"
          href={account.accountHref}
          target="_blank"
          rel="noopener noreferrer"
          onClick={closeAll}
        >
          {messages.account}
          <span className="mn-visually-hidden"> ({messages.newTab})</span>
        </a>
      ) : null}
      {account.onSignOut ? (
        <button
          type="button"
          className="mn-rook-bar__action"
          onClick={() => {
            closeAll();
            account.onSignOut?.();
          }}
        >
          {messages.signOut}
        </button>
      ) : null}
    </>
  ) : (
    accountDesktop
  );

  return (
    <div
      ref={(node) => {
        rootRef.current = node;
        if (typeof ref === 'function') ref(node);
        else if (ref) ref.current = node;
      }}
      {...rest}
      className={cx('mn-rook-bar', className)}
      style={
        scrollProgress
          ? ({ ...style, '--mn-rook-bar-progress': String(progress) } as CSSProperties)
          : style
      }
      data-position={position}
      data-solid={scrolled || active || sheetOpen ? '' : undefined}
      data-menu-open={active ? '' : undefined}
      data-sheet-open={sheetOpen ? '' : undefined}
    >
      {active ? <div className="mn-rook-bar__scrim" aria-hidden onClick={closeAll} /> : null}
      <nav className="mn-rook-bar__nav" aria-label={messages.nav}>
        <div className="mn-rook-bar__inner">
          <div className="mn-rook-bar__brand">{brand}</div>
          <ul className="mn-rook-bar__items" onPointerLeave={scheduleClose}>
            {items.map((item) => {
              if (!isMenu(item)) {
                return <li key={item.href}>{renderLink(item, 'mn-rook-bar__item')}</li>;
              }
              const open = active === item.id;
              const panelId = `${baseId}-${item.id}`;
              return (
                <li key={item.id}>
                  <button
                    ref={(node) => {
                      if (node) triggerRefs.current.set(item.id, node);
                      else triggerRefs.current.delete(item.id);
                    }}
                    type="button"
                    className="mn-rook-bar__item"
                    aria-expanded={open}
                    aria-controls={open ? panelId : undefined}
                    data-state={open ? 'open' : 'closed'}
                    onPointerEnter={(e) => hoverOpen(item.id, e)}
                    onClick={() => openMenu(open ? null : item.id)}
                  >
                    {item.label}
                    <ChevronDownGlyph className="mn-rook-bar__chevron" aria-hidden />
                  </button>
                  {open ? (
                    <div
                      id={panelId}
                      className="mn-rook-bar__panel"
                      onPointerEnter={keepOpen}
                      onPointerLeave={scheduleClose}
                    >
                      <div className="mn-rook-bar__panel-inner">
                        <RookBarPanelProvider value={desktopCtx}>{item.panel}</RookBarPanelProvider>
                      </div>
                    </div>
                  ) : null}
                </li>
              );
            })}
          </ul>
          <div className="mn-rook-bar__end">
            <div className="mn-rook-bar__account">{accountDesktop}</div>
            <button
              ref={toggleRef}
              type="button"
              className="mn-rook-bar__toggle"
              aria-expanded={sheetOpen}
              aria-controls={sheetOpen ? sheetId : undefined}
              aria-label={sheetOpen ? messages.closeMenu : messages.openMenu}
              onClick={() => {
                setActive(null);
                setSheetOpen((o) => !o);
              }}
            >
              {sheetOpen ? <CloseGlyph aria-hidden /> : <MenuGlyph aria-hidden />}
            </button>
          </div>
        </div>
        {scrollProgress ? <div className="mn-rook-bar__progress" aria-hidden /> : null}
      </nav>
      {sheetOpen ? (
        <div id={sheetId} className="mn-rook-bar__sheet">
          <div className="mn-rook-bar__sheet-body">
            <ul className="mn-rook-bar__sections">
              {items.map((item) => {
                if (!isMenu(item)) {
                  return <li key={item.href}>{renderLink(item, 'mn-rook-bar__section-link')}</li>;
                }
                const open = mobileSection === item.id;
                const regionId = `${sheetId}-${item.id}`;
                return (
                  <li key={item.id}>
                    <button
                      type="button"
                      className="mn-rook-bar__section-toggle"
                      aria-expanded={open}
                      aria-controls={open ? regionId : undefined}
                      data-state={open ? 'open' : 'closed'}
                      onClick={() => {
                        if (!open) item.onOpen?.();
                        setMobileSection(open ? null : item.id);
                      }}
                    >
                      {item.label}
                      <ChevronDownGlyph className="mn-rook-bar__chevron" aria-hidden />
                    </button>
                    {open ? (
                      <div id={regionId} className="mn-rook-bar__section">
                        <RookBarPanelProvider value={mobileCtx}>{item.panel}</RookBarPanelProvider>
                      </div>
                    ) : null}
                  </li>
                );
              })}
            </ul>
          </div>
          {accountMobile ? <div className="mn-rook-bar__sheet-footer">{accountMobile}</div> : null}
        </div>
      ) : null}
    </div>
  );
});
