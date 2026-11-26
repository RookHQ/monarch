'use client';

import { forwardRef, type HTMLAttributes, type ReactNode } from 'react';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '../../overlays/dropdown-menu/index.js';
import { cx } from '../../../internal/cx.js';
import type { MonarchLinkComponent } from '../../../internal/link.js';
import { ChevronDownGlyph } from '../../../internal/icons.js';
import { isSafeUrl } from '../../../internal/sanitize.js';
import { pathMatches } from '../../../internal/shell/link.js';
import { useOverflowItems } from '../../../internal/overflow.js';

export interface TopNavItem {
  /** Stable key, unique within the navigation. */
  key: string;
  /** Visible label. */
  label: string;
  /** Optional decorative icon before the label. */
  icon?: ReactNode;
  /** Link target. Without it the item is a button that calls `onSelect`. Unsafe URLs are ignored. */
  href?: string | undefined;
  /** Open `href` in a new tab, for another app or site. */
  external?: boolean | undefined;
  /** Called on activation (for links: before navigating). */
  onSelect?: (() => void) | undefined;
  /** Count shown after the label; hidden when 0 or missing. */
  badge?: number | undefined;
  /** Spoken text for the badge. Default `messages.badge(count)`. */
  badgeLabel?: string | undefined;
  /** Explicit current state; otherwise `href` is matched against `currentPath`. */
  active?: boolean | undefined;
  /** Match `currentPath` exactly instead of by path prefix. */
  exact?: boolean | undefined;
}

export interface TopNavMessages {
  /** Name of the navigation landmark. Default "Main". */
  nav: string;
  /** The overflow menu button. Default "More". */
  more: string;
  /** Spoken badge text. Default "{count} new". */
  badge: (count: number) => string;
  /** Appended to external links. Default "opens in a new tab". */
  newTab: string;
}

export const TOP_NAV_MESSAGES: TopNavMessages = {
  nav: 'Main',
  more: 'More',
  badge: (count) => `${count} new`,
  newTab: 'opens in a new tab',
};

export interface TopNavProps extends Omit<HTMLAttributes<HTMLElement>, 'children'> {
  /** Destinations, in priority order: the last ones move into "More" first. */
  items: readonly TopNavItem[];
  /** The current path (e.g. `usePathname()`); the matching item is marked current and never overflows. */
  currentPath?: string | undefined;
  /** Link component, e.g. Next.js `Link`. Defaults to `a`. External links always use `a`. */
  linkAs?: MonarchLinkComponent | undefined;
  /** Content before the links, such as a brand link. */
  start?: ReactNode;
  /** Content after the links, such as search or account buttons. */
  end?: ReactNode;
  /** Called after any item is activated. */
  onNavigate?: ((item: TopNavItem) => void) | undefined;
  /** Visible and spoken strings, for translation. */
  messages?: Partial<TopNavMessages> | undefined;
}

const safeHref = (href: string | undefined) => (href && isSafeUrl(href) ? href : undefined);

function isActive(item: TopNavItem, path: string | undefined): boolean {
  if (item.active !== undefined) return item.active;
  const href = safeHref(item.href);
  return !!href && !item.external && path !== undefined && pathMatches(href, path, item.exact);
}

/**
 * Horizontal navigation for an app header or site: links in one row, with
 * the ones that do not fit moved into a "More" menu as the width shrinks
 * (the current page always stays visible). Router-agnostic: pass
 * `currentPath` and your router's link as `linkAs`.
 */
export const TopNav = forwardRef<HTMLElement, TopNavProps>(function TopNav(
  {
    items,
    currentPath,
    linkAs: LinkComp = 'a',
    start,
    end,
    onNavigate,
    messages: messagesProp,
    className,
    'aria-label': ariaLabel,
    ...rest
  },
  ref,
) {
  const messages = { ...TOP_NAV_MESSAGES, ...messagesProp };
  const activeIndex = items.findIndex((i) => isActive(i, currentPath));
  const { containerRef, measureRef, visible } = useOverflowItems(
    items.length,
    activeIndex,
    items.map((i) => `${i.key}:${i.label}:${i.badge ?? ''}`).join('|'),
  );
  const shown = visible ?? items.map((_, i) => i);
  const overflow = items.map((_, i) => i).filter((i) => !shown.includes(i));

  const body = (item: TopNavItem) => {
    const badge = item.badge && item.badge > 0 ? item.badge : 0;
    return (
      <>
        {item.icon ? (
          <span className="mn-top-nav__icon" aria-hidden>
            {item.icon}
          </span>
        ) : null}
        <span className="mn-top-nav__label">{item.label}</span>
        {badge ? (
          <>
            <span className="mn-top-nav__badge" aria-hidden>
              {badge > 99 ? '99+' : badge}
            </span>
            <span className="mn-visually-hidden">, {item.badgeLabel ?? messages.badge(badge)}</span>
          </>
        ) : null}
        {item.external && safeHref(item.href) ? (
          <span className="mn-visually-hidden"> ({messages.newTab})</span>
        ) : null}
      </>
    );
  };

  const control = (item: TopNavItem, className: string) => {
    const href = safeHref(item.href);
    const active = isActive(item, currentPath);
    const activate = () => {
      item.onSelect?.();
      onNavigate?.(item);
    };
    if (href) {
      const Comp = item.external ? 'a' : LinkComp;
      return (
        <Comp
          className={className}
          href={href}
          data-active={active ? '' : undefined}
          aria-current={active ? 'page' : undefined}
          onClick={activate}
          {...(item.external ? { target: '_blank', rel: 'noopener noreferrer' } : {})}
        >
          {body(item)}
        </Comp>
      );
    }
    return (
      <button
        type="button"
        className={className}
        data-active={active ? '' : undefined}
        aria-current={active ? 'true' : undefined}
        onClick={activate}
      >
        {body(item)}
      </button>
    );
  };

  return (
    <nav
      ref={ref}
      {...rest}
      aria-label={ariaLabel ?? messages.nav}
      className={cx('mn-top-nav', className)}
    >
      {start ? <div className="mn-top-nav__start">{start}</div> : null}
      <div
        ref={containerRef}
        className="mn-top-nav__bar"
        data-overflowing={overflow.length > 0 || undefined}
      >
        <ul className="mn-top-nav__list">
          {shown.map((i) => {
            const item = items[i] as TopNavItem;
            return (
              <li key={item.key} className="mn-top-nav__entry">
                {control(item, 'mn-top-nav__item')}
              </li>
            );
          })}
          {overflow.length > 0 ? (
            <li className="mn-top-nav__entry">
              <DropdownMenu>
                <DropdownMenuTrigger className="mn-top-nav__item mn-top-nav__more">
                  <span className="mn-top-nav__label">{messages.more}</span>
                  <span className="mn-top-nav__chevron" aria-hidden>
                    <ChevronDownGlyph />
                  </span>
                </DropdownMenuTrigger>
                <DropdownMenuContent align="end">
                  {overflow.map((i) => {
                    const item = items[i] as TopNavItem;
                    const href = safeHref(item.href);
                    const active = isActive(item, currentPath);
                    const activate = () => {
                      item.onSelect?.();
                      onNavigate?.(item);
                    };
                    if (!href)
                      return (
                        <DropdownMenuItem key={item.key} icon={item.icon} onSelect={activate}>
                          {item.label}
                        </DropdownMenuItem>
                      );
                    const Comp = item.external ? 'a' : LinkComp;
                    return (
                      <DropdownMenuItem key={item.key} asChild onSelect={activate}>
                        <Comp
                          href={href}
                          className="mn-top-nav__menu-link"
                          aria-current={active ? 'page' : undefined}
                          {...(item.external
                            ? { target: '_blank', rel: 'noopener noreferrer' }
                            : {})}
                        >
                          {body(item)}
                        </Comp>
                      </DropdownMenuItem>
                    );
                  })}
                </DropdownMenuContent>
              </DropdownMenu>
            </li>
          ) : null}
        </ul>
        <div ref={measureRef} className="mn-top-nav__measure" aria-hidden>
          {items.map((item) => (
            <span key={item.key} className="mn-top-nav__item">
              {item.icon ? <span className="mn-top-nav__icon">{item.icon}</span> : null}
              <span className="mn-top-nav__label">{item.label}</span>
              {item.badge && item.badge > 0 ? (
                <span className="mn-top-nav__badge">{item.badge > 99 ? '99+' : item.badge}</span>
              ) : null}
            </span>
          ))}
          <span className="mn-top-nav__item mn-top-nav__more">
            <span className="mn-top-nav__label">{messages.more}</span>
            <span className="mn-top-nav__chevron">
              <ChevronDownGlyph />
            </span>
          </span>
        </div>
      </div>
      {end ? <div className="mn-top-nav__end">{end}</div> : null}
    </nav>
  );
});
