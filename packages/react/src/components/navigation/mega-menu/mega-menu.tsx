'use client';

import {
  forwardRef,
  useEffect,
  useId,
  useRef,
  useState,
  type AnchorHTMLAttributes,
  type ElementType,
  type FocusEvent,
  type HTMLAttributes,
  type KeyboardEvent,
  type ReactNode,
} from 'react';
import { cx } from '../../../internal/cx.js';
import {
  ChevronDownGlyph,
  ChevronRightGlyph,
  CloseGlyph,
  MenuGlyph,
} from '../../../internal/icons.js';
import { Badge } from '../../data-display/badge/index.js';

/** One link inside a panel. */
export interface MegaMenuLink {
  label: string;
  href: string;
  /** One line under the label. */
  description?: string;
  /** Small leading icon. Decorative. */
  icon?: ReactNode;
  /** Short tag after the label, e.g. "New". */
  badge?: string;
  /** Opens in a new tab, with a spoken "(opens in a new tab)". */
  external?: boolean;
  /** The current page: `aria-current="page"`. */
  current?: boolean;
}

/** A titled column of links. */
export interface MegaMenuSection {
  /** Column heading, e.g. "Platform". Optional. */
  heading?: string;
  links: readonly MegaMenuLink[];
}

/** The larger promoted card at the end of a panel. */
export interface MegaMenuFeatured {
  title: string;
  href: string;
  description?: string;
  icon?: ReactNode;
  badge?: string;
  /** Call to action under the text. Default `messages.explore`. */
  cta?: string;
  external?: boolean;
}

/** A top-level entry: a panel of sections, or a plain link when it has `href` and no sections. */
export interface MegaMenuItem {
  /** Stable, unique id. */
  id: string;
  label: string;
  sections?: readonly MegaMenuSection[];
  featured?: MegaMenuFeatured;
  /** Makes the entry a plain link (when there are no sections). */
  href?: string;
  external?: boolean;
  current?: boolean;
}

/** Visible and spoken strings, for translation. */
export interface MegaMenuMessages {
  /** Label of the small-screen menu button. Default "Menu". */
  menu: string;
  /** Default call to action of featured cards. Default "Explore". */
  explore: string;
  /** Spoken after external links. Default "(opens in a new tab)". */
  newTab: string;
}

export const MEGA_MENU_MESSAGES: MegaMenuMessages = {
  menu: 'Menu',
  explore: 'Explore',
  newTab: '(opens in a new tab)',
};

export interface MegaMenuProps extends HTMLAttributes<HTMLElement> {
  /** Top-level entries, left to right. */
  items: readonly MegaMenuItem[];
  /** Link component, e.g. Next.js `Link`. Defaults to `a`. External links always use `a`. */
  linkAs?: ElementType<AnchorHTMLAttributes<HTMLAnchorElement>> | undefined;
  /** Open panels when a mouse hovers the entry (with a short close delay). Default `true`. */
  openOnHover?: boolean;
  /** Controlled open panel id, or `null` for none. */
  openItem?: string | null;
  onOpenItemChange?: ((id: string | null) => void) | undefined;
  /** Called after any link in the menu is followed, e.g. to close a surrounding drawer. */
  onNavigate?: ((href: string) => void) | undefined;
  /** Visible and spoken strings, for translation. */
  messages?: Partial<MegaMenuMessages>;
}

const HOVER_CLOSE_DELAY = 120;
/** Keep in sync with the breakpoint in mega-menu.css. */
const WIDE_QUERY = '(min-width: 64rem)';

/**
 * Site navigation with large drop-down panels: columns of links with
 * descriptions and an optional featured card. It follows the disclosure
 * navigation pattern (buttons with `aria-expanded`, not an ARIA menu), so
 * links stay links. On small screens the entries collapse behind a Menu
 * button into an accordion.
 */
export const MegaMenu = forwardRef<HTMLElement, MegaMenuProps>(function MegaMenu(
  {
    items,
    linkAs: LinkComp = 'a',
    openOnHover = true,
    openItem: openItemProp,
    onOpenItemChange,
    onNavigate,
    messages: messagesProp,
    className,
    'aria-label': ariaLabel = 'Main',
    ...rest
  },
  ref,
) {
  const messages = { ...MEGA_MENU_MESSAGES, ...messagesProp };
  const baseId = useId();
  const [openState, setOpenState] = useState<string | null>(null);
  const openId = openItemProp !== undefined ? openItemProp : openState;
  const [mobileOpen, setMobileOpen] = useState(false);
  const root = useRef<HTMLElement | null>(null);
  const openedByHover = useRef(false);
  const closeTimer = useRef<ReturnType<typeof setTimeout> | null>(null);

  const setOpen = (id: string | null) => {
    if (openItemProp === undefined) setOpenState(id);
    if (id !== openId) onOpenItemChange?.(id);
  };

  const clearTimer = () => {
    if (closeTimer.current) clearTimeout(closeTimer.current);
    closeTimer.current = null;
  };
  useEffect(() => clearTimer, []);

  // Outside clicks close the open panel and the small-screen menu.
  useEffect(() => {
    if (openId === null && !mobileOpen) return;
    const onDown = (event: PointerEvent) => {
      if (root.current && !root.current.contains(event.target as Node)) {
        setOpen(null);
        setMobileOpen(false);
      }
    };
    document.addEventListener('pointerdown', onDown);
    return () => document.removeEventListener('pointerdown', onDown);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [openId, mobileOpen]);

  const buttonId = (id: string) => `${baseId}-${id}-button`;
  const panelId = (id: string) => `${baseId}-${id}-panel`;
  const listId = `${baseId}-list`;
  const toggleId = `${baseId}-toggle`;

  const topLevel = () =>
    Array.from(root.current?.querySelectorAll<HTMLElement>('[data-mn-mega-top]') ?? []);

  // Hover opens panels only in the wide layout; the small-screen accordion is click-only.
  const canHover = (pointerType: string) =>
    openOnHover &&
    pointerType === 'mouse' &&
    (typeof window === 'undefined' ||
      typeof window.matchMedia !== 'function' ||
      window.matchMedia(WIDE_QUERY).matches);

  const followed = (href: string) => {
    setOpen(null);
    setMobileOpen(false);
    onNavigate?.(href);
  };

  const onRootKeyDown = (event: KeyboardEvent<HTMLElement>) => {
    const target = event.target as HTMLElement;
    if (event.key === 'Escape') {
      if (openId !== null) {
        event.preventDefault();
        const id = openId;
        setOpen(null);
        document.getElementById(buttonId(id))?.focus();
      } else if (mobileOpen) {
        event.preventDefault();
        setMobileOpen(false);
        document.getElementById(toggleId)?.focus();
      }
      return;
    }
    if (target.hasAttribute('data-mn-mega-top')) {
      const tops = topLevel();
      const index = tops.indexOf(target);
      const move = (i: number) => {
        event.preventDefault();
        tops[(i + tops.length) % tops.length]?.focus();
      };
      if (event.key === 'ArrowRight') move(index + 1);
      else if (event.key === 'ArrowLeft') move(index - 1);
      else if (event.key === 'Home') move(0);
      else if (event.key === 'End') move(tops.length - 1);
      else if (event.key === 'ArrowDown' && target.getAttribute('aria-expanded') !== null) {
        event.preventDefault();
        const id = target.getAttribute('data-mn-mega-top') ?? '';
        if (openId !== id) {
          openedByHover.current = false;
          setOpen(id);
        }
        requestAnimationFrame(() => {
          document.getElementById(panelId(id))?.querySelector<HTMLElement>('a[href]')?.focus();
        });
      }
      return;
    }
    const panel = target.closest<HTMLElement>('[data-mn-mega-panel]');
    if (
      panel &&
      (event.key === 'ArrowDown' ||
        event.key === 'ArrowUp' ||
        event.key === 'Home' ||
        event.key === 'End')
    ) {
      const links = Array.from(panel.querySelectorAll<HTMLElement>('a[href]'));
      const index = links.indexOf(target);
      if (index === -1) return;
      event.preventDefault();
      const next =
        event.key === 'Home'
          ? 0
          : event.key === 'End'
            ? links.length - 1
            : event.key === 'ArrowDown'
              ? Math.min(index + 1, links.length - 1)
              : index - 1;
      if (next < 0)
        document.getElementById(buttonId(panel.getAttribute('data-mn-mega-panel') ?? ''))?.focus();
      else links[next]?.focus();
    }
  };

  // Focus leaving the whole navigation closes the open panel.
  const onRootBlur = (event: FocusEvent<HTMLElement>) => {
    const next = event.relatedTarget as Node | null;
    if (next && root.current && !root.current.contains(next)) setOpen(null);
  };

  const renderLink = (
    link: { href: string; external?: boolean | undefined; current?: boolean | undefined },
    className: string,
    children: ReactNode,
    extra: Record<string, string | undefined> = {},
  ) => {
    const Comp: ElementType = link.external ? 'a' : LinkComp;
    return (
      <Comp
        href={link.href}
        className={className}
        aria-current={link.current ? 'page' : undefined}
        data-current={link.current || undefined}
        {...(link.external ? { target: '_blank', rel: 'noopener noreferrer' } : {})}
        {...extra}
        onClick={() => followed(link.href)}
      >
        {children}
        {link.external ? <span className="mn-visually-hidden"> {messages.newTab}</span> : null}
      </Comp>
    );
  };

  return (
    // Keyboard handling is delegated from the buttons and links inside.
    // eslint-disable-next-line jsx-a11y/no-noninteractive-element-interactions
    <nav
      ref={(node) => {
        root.current = node;
        if (typeof ref === 'function') ref(node);
        else if (ref) ref.current = node;
      }}
      aria-label={ariaLabel}
      {...rest}
      className={cx('mn-mega-menu', className)}
      data-mobile-open={mobileOpen || undefined}
      onKeyDown={(event) => {
        rest.onKeyDown?.(event);
        if (!event.defaultPrevented) onRootKeyDown(event);
      }}
      onBlur={(event) => {
        rest.onBlur?.(event);
        onRootBlur(event);
      }}
    >
      <button
        id={toggleId}
        type="button"
        className="mn-mega-menu__toggle"
        aria-expanded={mobileOpen}
        aria-controls={listId}
        onClick={() => setMobileOpen(!mobileOpen)}
      >
        <span aria-hidden className="mn-mega-menu__toggle-icon">
          {mobileOpen ? <CloseGlyph /> : <MenuGlyph />}
        </span>
        {messages.menu}
      </button>
      <ul id={listId} className="mn-mega-menu__list">
        {items.map((item) => {
          const hasPanel = !!(item.sections?.length || item.featured);
          if (!hasPanel) {
            return (
              <li key={item.id} className="mn-mega-menu__item">
                {item.href
                  ? renderLink(
                      { href: item.href, external: item.external, current: item.current },
                      'mn-mega-menu__top',
                      <span className="mn-mega-menu__top-label">{item.label}</span>,
                      { 'data-mn-mega-top': item.id },
                    )
                  : null}
              </li>
            );
          }
          const open = openId === item.id;
          return (
            <li
              key={item.id}
              className="mn-mega-menu__item"
              data-state={open ? 'open' : 'closed'}
              onPointerEnter={(event) => {
                if (!canHover(event.pointerType)) return;
                clearTimer();
                if (!open) {
                  openedByHover.current = true;
                  setOpen(item.id);
                }
              }}
              onPointerLeave={(event) => {
                if (!canHover(event.pointerType) || !openedByHover.current) return;
                clearTimer();
                closeTimer.current = setTimeout(() => {
                  if (openedByHover.current) setOpen(null);
                }, HOVER_CLOSE_DELAY);
              }}
            >
              <button
                id={buttonId(item.id)}
                type="button"
                className="mn-mega-menu__top"
                aria-expanded={open}
                aria-controls={panelId(item.id)}
                data-mn-mega-top={item.id}
                data-current={item.current || undefined}
                onClick={() => {
                  clearTimer();
                  if (open && openedByHover.current) {
                    // A click after hovering pins the panel open instead of closing it.
                    openedByHover.current = false;
                    return;
                  }
                  openedByHover.current = false;
                  setOpen(open ? null : item.id);
                }}
              >
                <span className="mn-mega-menu__top-label">{item.label}</span>
                <ChevronDownGlyph className="mn-mega-menu__chevron" aria-hidden />
              </button>
              <div
                id={panelId(item.id)}
                className="mn-mega-menu__panel"
                data-mn-mega-panel={item.id}
                data-has-featured={item.featured ? '' : undefined}
                hidden={!open}
              >
                {item.sections?.map((section, s) => {
                  const headingId = `${panelId(item.id)}-h${s}`;
                  return (
                    <div key={s} className="mn-mega-menu__section">
                      {section.heading ? (
                        <p id={headingId} className="mn-mega-menu__heading">
                          {section.heading}
                        </p>
                      ) : null}
                      <ul
                        className="mn-mega-menu__links"
                        aria-labelledby={section.heading ? headingId : undefined}
                      >
                        {section.links.map((link) => (
                          <li key={`${link.href}-${link.label}`}>
                            {renderLink(
                              link,
                              'mn-mega-menu__link',
                              <>
                                {link.icon ? (
                                  <span className="mn-mega-menu__icon" aria-hidden>
                                    {link.icon}
                                  </span>
                                ) : null}
                                <span className="mn-mega-menu__text">
                                  <span className="mn-mega-menu__label">
                                    {link.label}
                                    {link.badge ? (
                                      <Badge
                                        tone="accent"
                                        size="sm"
                                        className="mn-mega-menu__badge"
                                      >
                                        {link.badge}
                                      </Badge>
                                    ) : null}
                                  </span>
                                  {link.description ? (
                                    <span className="mn-mega-menu__description">
                                      {link.description}
                                    </span>
                                  ) : null}
                                </span>
                              </>,
                            )}
                          </li>
                        ))}
                      </ul>
                    </div>
                  );
                })}
                {item.featured
                  ? renderLink(
                      item.featured,
                      'mn-mega-menu__featured',
                      <>
                        <span className="mn-mega-menu__featured-top">
                          {item.featured.icon ? (
                            <span className="mn-mega-menu__featured-icon" aria-hidden>
                              {item.featured.icon}
                            </span>
                          ) : null}
                          {item.featured.badge ? (
                            <Badge tone="accent" size="sm">
                              {item.featured.badge}
                            </Badge>
                          ) : null}
                        </span>
                        <span className="mn-mega-menu__featured-title">{item.featured.title}</span>
                        {item.featured.description ? (
                          <span className="mn-mega-menu__featured-description">
                            {item.featured.description}
                          </span>
                        ) : null}
                        <span className="mn-mega-menu__featured-cta" aria-hidden>
                          {item.featured.cta ?? messages.explore}
                          <ChevronRightGlyph />
                        </span>
                      </>,
                    )
                  : null}
              </div>
            </li>
          );
        })}
      </ul>
    </nav>
  );
});
