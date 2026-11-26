'use client';

import {
  forwardRef,
  useEffect,
  useId,
  useRef,
  useState,
  type HTMLAttributes,
  type MouseEvent,
  type ReactNode,
} from 'react';
import * as PopoverPrimitive from '@radix-ui/react-popover';
import { cx } from '../../../internal/cx.js';
import { ChevronDownGlyph, CloseGlyph, MenuGlyph } from '../../../internal/icons.js';
import { SiteLinkView } from './marketing-site-shell.link.js';
import { isGroup, type LinkComponent, type SiteNavItem } from './marketing-site-shell.types.js';

export interface SiteHeaderProps extends HTMLAttributes<HTMLElement> {
  /**
   * Product wordmark, normally a link home: `<a href="/"><img alt="Rook Dossier" …/></a>`.
   * Monarch ships no artwork; use the product's logo from the Rook brand kit.
   */
  brand: ReactNode;
  /** Top-level links and groups. Groups open a disclosure panel of links. */
  nav?: SiteNavItem[];
  /** Calls to action on the trailing edge, e.g. "Log in" and one primary Button. */
  actions?: ReactNode;
  /** Accessible name of the navigation landmark. */
  navLabel?: string;
  /** Stick to the top while scrolling. Default true. */
  sticky?: boolean;
  /** Router link component for nav links (e.g. Next.js Link). Default `a`. */
  linkAs?: LinkComponent;
  /** Labels for the mobile menu toggle. */
  menuLabels?: { open: string; close: string };
  /**
   * The current path. When it changes (client-side navigation) every open
   * menu and the mobile panel close. Pass your router's pathname.
   */
  pathname?: string;
  /**
   * Called with the `href` of a navigation link that was activated. Menus
   * close first; use it to track navigation or to drive a router.
   */
  onNavigate?: (href: string) => void;
}

/**
 * Site header for marketing and public pages: wordmark, primary navigation with
 * disclosure menus, and actions. Under 768px the navigation collapses into a
 * menu button (`aria-expanded`), closed with Escape.
 */
export const SiteHeader = forwardRef<HTMLElement, SiteHeaderProps>(function SiteHeader(
  {
    brand,
    nav = [],
    actions,
    navLabel = 'Main',
    sticky = true,
    linkAs,
    menuLabels = { open: 'Open menu', close: 'Close menu' },
    pathname,
    onNavigate,
    className,
    ...rest
  },
  ref,
) {
  const [open, setOpen] = useState(false);
  const [openGroup, setOpenGroup] = useState<string | null>(null);
  const panelId = `mn-site-menu-${useId()}`;
  const toggleRef = useRef<HTMLButtonElement>(null);

  // Client-side navigation: close during render, so the new page never paints with a menu open.
  const [lastPath, setLastPath] = useState(pathname);
  if (pathname !== lastPath) {
    setLastPath(pathname);
    setOpen(false);
    setOpenGroup(null);
  }

  // Back and forward never re-render the header with a new pathname in every router.
  useEffect(() => {
    const onPop = () => {
      setOpen(false);
      setOpenGroup(null);
    };
    window.addEventListener('popstate', onPop);
    return () => window.removeEventListener('popstate', onPop);
  }, []);

  // A navigation link was activated: close menus, then report it. A native
  // listener delegates from each <nav>, so the markup carries no click handler
  // on a non-interactive element; the links keep their own keyboard behaviour.
  const navRef = useRef<HTMLElement>(null);
  const panelRef = useRef<HTMLElement>(null);
  const onNavigateRef = useRef(onNavigate);
  onNavigateRef.current = onNavigate;
  /** Closes menus and reports the href when a click inside `container` hit a link. */
  const activateLink = (container: Element, target: EventTarget | null) => {
    const anchor = target instanceof Element ? target.closest('a[href]') : null;
    if (!anchor || !container.contains(anchor)) return;
    setOpen(false);
    setOpenGroup(null);
    onNavigateRef.current?.(anchor.getAttribute('href') ?? '');
  };
  const activateLinkRef = useRef(activateLink);
  activateLinkRef.current = activateLink;
  /** Group menus render in a portal (a dialog, outside the <nav>), so they delegate here. */
  const onMenuClick = (event: MouseEvent<HTMLElement>) =>
    activateLink(event.currentTarget, event.target);
  useEffect(() => {
    const navs = [navRef.current, panelRef.current].filter((n): n is HTMLElement => n !== null);
    const onClick = (event: Event) =>
      activateLinkRef.current(event.currentTarget as HTMLElement, event.target);
    for (const n of navs) n.addEventListener('click', onClick);
    return () => {
      for (const n of navs) n.removeEventListener('click', onClick);
    };
  }, [open, nav.length]);

  useEffect(() => {
    if (!open) return;
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        setOpen(false);
        toggleRef.current?.focus();
      }
    };
    document.addEventListener('keydown', onKey);
    return () => document.removeEventListener('keydown', onKey);
  }, [open]);

  return (
    <header
      ref={ref}
      {...rest}
      className={cx('mn-site-header', className)}
      data-sticky={sticky || undefined}
    >
      <div className="mn-site-header__inner">
        <div className="mn-site-header__brand">{brand}</div>
        {nav.length ? (
          <nav ref={navRef} aria-label={navLabel} className="mn-site-header__nav">
            <ul className="mn-site-header__list">
              {nav.map((item) =>
                isGroup(item) ? (
                  <li key={item.label}>
                    <PopoverPrimitive.Root
                      open={openGroup === item.label}
                      onOpenChange={(o) => setOpenGroup(o ? item.label : null)}
                    >
                      <PopoverPrimitive.Trigger className="mn-site-header__link mn-site-header__trigger">
                        {item.label}
                        <ChevronDownGlyph className="mn-site-header__chevron" />
                      </PopoverPrimitive.Trigger>
                      <PopoverPrimitive.Portal>
                        <PopoverPrimitive.Content
                          className="mn-site-menu"
                          align="start"
                          sideOffset={10}
                          aria-label={item.label}
                          onClick={onMenuClick}
                        >
                          <ul className="mn-site-menu__list">
                            {item.items.map((link) => (
                              <li key={link.href}>
                                <SiteLinkView
                                  link={link}
                                  as={linkAs}
                                  className="mn-site-menu__item"
                                >
                                  <span className="mn-site-menu__label">{link.label}</span>
                                  {link.description ? (
                                    <span className="mn-site-menu__description">
                                      {link.description}
                                    </span>
                                  ) : null}
                                </SiteLinkView>
                              </li>
                            ))}
                          </ul>
                        </PopoverPrimitive.Content>
                      </PopoverPrimitive.Portal>
                    </PopoverPrimitive.Root>
                  </li>
                ) : (
                  <li key={item.href}>
                    <SiteLinkView link={item} as={linkAs} className="mn-site-header__link" />
                  </li>
                ),
              )}
            </ul>
          </nav>
        ) : null}
        <div className="mn-site-header__end">
          {actions ? <div className="mn-site-header__actions">{actions}</div> : null}
          {nav.length ? (
            <button
              ref={toggleRef}
              type="button"
              className="mn-site-header__toggle"
              aria-expanded={open}
              aria-controls={panelId}
              aria-label={open ? menuLabels.close : menuLabels.open}
              onClick={() => setOpen((o) => !o)}
            >
              {open ? <CloseGlyph /> : <MenuGlyph />}
            </button>
          ) : null}
        </div>
      </div>
      {open ? (
        <nav ref={panelRef} id={panelId} aria-label={navLabel} className="mn-site-header__panel">
          <ul className="mn-site-header__panel-list">
            {nav.map((item) =>
              isGroup(item) ? (
                <li key={item.label} className="mn-site-header__panel-group">
                  <p className="mn-site-header__panel-heading">{item.label}</p>
                  <ul>
                    {item.items.map((link) => (
                      <li key={link.href}>
                        <SiteLinkView
                          link={link}
                          as={linkAs}
                          className="mn-site-header__panel-link"
                        />
                      </li>
                    ))}
                  </ul>
                </li>
              ) : (
                <li key={item.href}>
                  <SiteLinkView link={item} as={linkAs} className="mn-site-header__panel-link" />
                </li>
              ),
            )}
          </ul>
          {actions ? <div className="mn-site-header__panel-actions">{actions}</div> : null}
        </nav>
      ) : null}
    </header>
  );
});
SiteHeader.displayName = 'SiteHeader';
