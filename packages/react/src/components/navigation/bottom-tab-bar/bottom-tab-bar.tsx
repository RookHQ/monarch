'use client';

import { forwardRef, type HTMLAttributes, type ReactNode } from 'react';
import { cx } from '../../../internal/cx.js';
import type { MonarchLinkComponent } from '../../../internal/link.js';
import { isSafeUrl } from '../../../internal/sanitize.js';
import { pathMatches } from '../../../internal/shell/link.js';

export interface BottomTabBarItem {
  /** Stable key, unique within the bar. */
  key: string;
  /** Short visible label (one word is best). */
  label: string;
  /** Decorative icon (20–24px). The label names the tab. */
  icon: ReactNode;
  /** Icon for the current tab, e.g. a filled version. Defaults to `icon`. */
  activeIcon?: ReactNode;
  /**
   * Link target. Without it the tab is a button that calls `onSelect`, e.g.
   * a "More" tab that opens a sheet. Unsafe URLs are ignored.
   */
  href?: string | undefined;
  /** Called on activation (for links: before navigating). */
  onSelect?: (() => void) | undefined;
  /** Count shown on the icon; hidden when 0 or missing. */
  badge?: number | undefined;
  /** Spoken text for the badge. Default `messages.badge(count)`. */
  badgeLabel?: string | undefined;
  /** Explicit current state; otherwise `href` is matched against `currentPath`. */
  active?: boolean | undefined;
  /** Match `currentPath` exactly instead of by path prefix. */
  exact?: boolean | undefined;
}

export interface BottomTabBarMessages {
  /** Name of the navigation landmark. Default "Main". */
  nav: string;
  /** Spoken badge text. Default "{count} new". */
  badge: (count: number) => string;
}

export const BOTTOM_TAB_BAR_MESSAGES: BottomTabBarMessages = {
  nav: 'Main',
  badge: (count) => `${count} new`,
};

export interface BottomTabBarProps extends Omit<HTMLAttributes<HTMLElement>, 'children'> {
  /** Three to five destinations, in order. */
  items: readonly BottomTabBarItem[];
  /** The current path (e.g. `usePathname()`); the matching tab is marked current. */
  currentPath?: string | undefined;
  /** Link component, e.g. Next.js `Link`. Defaults to `a`. */
  linkAs?: MonarchLinkComponent | undefined;
  /**
   * `fixed` (default) pins the bar to the bottom of the viewport with the
   * device's safe-area inset; `static` leaves placement to the layout.
   */
  position?: 'fixed' | 'static';
  /**
   * Hide the bar from this viewport width up, where a sidebar takes over:
   * `sm` 640px, `md` 768px, `lg` 1024px. Default: always shown.
   */
  hideFrom?: 'sm' | 'md' | 'lg' | undefined;
  /** Called after any tab is activated. */
  onNavigate?: ((item: BottomTabBarItem) => void) | undefined;
  /** Visible and spoken strings, for translation. */
  messages?: Partial<BottomTabBarMessages> | undefined;
}

function isActive(item: BottomTabBarItem, href: string | undefined, path: string | undefined) {
  if (item.active !== undefined) return item.active;
  return !!href && path !== undefined && pathMatches(href, path, item.exact);
}

/**
 * Primary navigation on phones: three to five icon-and-label tabs pinned to
 * the bottom of the screen, clear of the home indicator (safe-area inset).
 * The current destination is marked with `aria-current="page"`. Tabs are
 * links (or buttons for actions such as "More"), so it is navigation, not an
 * ARIA tablist.
 */
export const BottomTabBar = forwardRef<HTMLElement, BottomTabBarProps>(function BottomTabBar(
  {
    items,
    currentPath,
    linkAs: LinkComp = 'a',
    position = 'fixed',
    hideFrom,
    onNavigate,
    messages: messagesProp,
    className,
    'aria-label': ariaLabel,
    ...rest
  },
  ref,
) {
  const messages = { ...BOTTOM_TAB_BAR_MESSAGES, ...messagesProp };
  return (
    <nav
      ref={ref}
      {...rest}
      aria-label={ariaLabel ?? messages.nav}
      data-position={position}
      data-hide-from={hideFrom}
      className={cx('mn-bottom-tab-bar', className)}
    >
      <ul className="mn-bottom-tab-bar__list">
        {items.map((item) => {
          const href = item.href && isSafeUrl(item.href) ? item.href : undefined;
          const active = isActive(item, href, currentPath);
          const badge = item.badge && item.badge > 0 ? item.badge : 0;
          const body = (
            <>
              <span className="mn-bottom-tab-bar__icon" aria-hidden>
                {active && item.activeIcon ? item.activeIcon : item.icon}
                {badge ? (
                  <span className="mn-bottom-tab-bar__badge">{badge > 99 ? '99+' : badge}</span>
                ) : null}
              </span>
              <span className="mn-bottom-tab-bar__label">{item.label}</span>
              {badge ? (
                <span className="mn-visually-hidden">
                  , {item.badgeLabel ?? messages.badge(badge)}
                </span>
              ) : null}
            </>
          );
          const activate = () => {
            item.onSelect?.();
            onNavigate?.(item);
          };
          const common = {
            className: 'mn-bottom-tab-bar__item',
            'data-active': active ? '' : undefined,
            onClick: activate,
          };
          return (
            <li key={item.key} className="mn-bottom-tab-bar__entry">
              {href ? (
                <LinkComp {...common} href={href} aria-current={active ? 'page' : undefined}>
                  {body}
                </LinkComp>
              ) : (
                <button type="button" {...common} aria-current={active ? 'true' : undefined}>
                  {body}
                </button>
              )}
            </li>
          );
        })}
      </ul>
    </nav>
  );
});
