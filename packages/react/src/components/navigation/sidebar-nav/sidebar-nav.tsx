'use client';

import {
  forwardRef,
  useCallback,
  useId,
  useState,
  type HTMLAttributes,
  type ReactElement,
  type ReactNode,
} from 'react';
import { Tooltip, TooltipProvider } from '../../overlays/tooltip/index.js';
import { cx } from '../../../internal/cx.js';
import { useDirectionContext } from '../../../internal/direction.js';
import type { MonarchLinkComponent } from '../../../internal/link.js';
import { ChevronRightGlyph } from '../../../internal/icons.js';
import { isSafeUrl } from '../../../internal/sanitize.js';
import { pathMatches } from '../../../internal/shell/link.js';

/**
 * How an item's badge reads. `accent` (default) is a solid bubble for something
 * that needs attention, such as unread messages. `neutral` is a quiet count, such
 * as the number of records in a category, styled like a Tabs count.
 */
export type SidebarNavBadgeTone = 'neutral' | 'accent';

export interface SidebarNavItem {
  /** Stable key, unique within the navigation. */
  key: string;
  /** Visible label; when collapsed, the tooltip and accessible name. */
  label: string;
  /** Decorative icon (16–18px). Needed for the collapsed, icon-only state. */
  icon?: ReactNode;
  /**
   * Link target. Without it (and without `items`) the item is a button that
   * calls `onSelect`. Unsafe URLs (`javascript:` and similar) are ignored.
   */
  href?: string | undefined;
  /** Open `href` in a new tab, for another app or site. */
  external?: boolean | undefined;
  /** Called on activation (for links: before navigating). */
  onSelect?: (() => void) | undefined;
  /** Count shown after the label (a dot when collapsed); hidden when 0 or missing. */
  badge?: number | undefined;
  /**
   * `accent` (default): a solid bubble for something new or unread.
   * `neutral`: a quiet count that does not call for attention (no dot when collapsed).
   */
  badgeTone?: SidebarNavBadgeTone | undefined;
  /**
   * Spoken text for the badge, e.g. "3 unread". Default `messages.badge(count)`
   * for `accent` and `messages.count(count)` for `neutral`.
   */
  badgeLabel?: string | undefined;
  /** Explicit current state; otherwise `href` is matched against `currentPath`. */
  active?: boolean | undefined;
  /** Match `currentPath` exactly instead of by path prefix. */
  exact?: boolean | undefined;
  /**
   * Nested items. The item becomes a disclosure button that shows and hides
   * them; it opens by itself when one of them is the current page.
   */
  items?: readonly SidebarNavItem[] | undefined;
  /** Start a nested group open. */
  defaultOpen?: boolean | undefined;
}

export interface SidebarNavSection {
  /** Stable key, unique within the navigation. */
  key: string;
  /** Optional heading above the section; names its list for screen readers. */
  label?: string | undefined;
  items: readonly SidebarNavItem[];
}

export interface SidebarNavMessages {
  /** Name of the navigation landmark. Default "Main". */
  nav: string;
  /** Collapse button label. Default "Collapse sidebar". */
  collapse: string;
  /** Expand button label. Default "Expand sidebar". */
  expand: string;
  /** Spoken text of an `accent` badge. Default "{count} new". */
  badge: (count: number) => string;
  /** Spoken text of a `neutral` badge (a plain count). Default "{count}". */
  count?: ((count: number) => string) | undefined;
  /** Appended to external links. Default "opens in a new tab". */
  newTab: string;
}

function spokenCount(count: number): string {
  return `${count}`;
}

export const SIDEBAR_NAV_MESSAGES: SidebarNavMessages = {
  nav: 'Main',
  collapse: 'Collapse sidebar',
  expand: 'Expand sidebar',
  badge: (count) => `${count} new`,
  count: spokenCount,
  newTab: 'opens in a new tab',
};

export type SidebarNavLinkComponent = MonarchLinkComponent;

export interface SidebarNavProps extends Omit<HTMLAttributes<HTMLElement>, 'children'> {
  /** The navigation, in sections. A single unlabelled section is fine. */
  sections: readonly SidebarNavSection[];
  /** The current path (e.g. `usePathname()`); items whose `href` matches are marked current. */
  currentPath?: string | undefined;
  /** Link component, e.g. Next.js `Link`. Defaults to `a`. External links always use `a`. */
  linkAs?: SidebarNavLinkComponent | undefined;
  /** Icon-only mode with tooltips. Controlled; pair with `onCollapsedChange`. */
  collapsed?: boolean | undefined;
  /** Initial collapsed state when uncontrolled. Default false. */
  defaultCollapsed?: boolean | undefined;
  onCollapsedChange?: ((collapsed: boolean) => void) | undefined;
  /** Show the collapse / expand button at the bottom. Default false. */
  collapsible?: boolean | undefined;
  /** Content above the sections, such as a brand link or workspace switcher. */
  header?: ReactNode;
  /** Content below the sections, above the collapse button. */
  footer?: ReactNode;
  /** Called after any item is activated, e.g. to close a surrounding drawer on phones. */
  onNavigate?: ((item: SidebarNavItem) => void) | undefined;
  /** Visible and spoken strings, for translation. */
  messages?: Partial<SidebarNavMessages> | undefined;
}

function safeHref(href: string | undefined): string | undefined {
  return href && isSafeUrl(href) ? href : undefined;
}

function isActive(item: SidebarNavItem, currentPath: string | undefined): boolean {
  if (item.active !== undefined) return item.active;
  const href = safeHref(item.href);
  if (!href || item.external || currentPath === undefined) return false;
  return pathMatches(href, currentPath, item.exact);
}

function containsActive(item: SidebarNavItem, currentPath: string | undefined): boolean {
  return (item.items ?? []).some((c) => isActive(c, currentPath) || containsActive(c, currentPath));
}

interface Ctx {
  baseId: string;
  collapsed: boolean;
  currentPath: string | undefined;
  linkAs: SidebarNavLinkComponent;
  messages: SidebarNavMessages;
  open: (item: SidebarNavItem) => boolean;
  toggle: (item: SidebarNavItem, next: boolean) => void;
  expandSidebar: () => void;
  onNavigate: ((item: SidebarNavItem) => void) | undefined;
}

function ItemBody({ item, ctx }: { item: SidebarNavItem; ctx: Ctx }) {
  const badge = item.badge && item.badge > 0 ? item.badge : 0;
  const tone = item.badgeTone ?? 'accent';
  const spoken =
    tone === 'neutral' ? (ctx.messages.count ?? spokenCount)(badge) : ctx.messages.badge(badge);
  return (
    <>
      {item.icon ? (
        <span className="mn-sidebar-nav__icon" aria-hidden>
          {item.icon}
        </span>
      ) : null}
      <span className="mn-sidebar-nav__label">{item.label}</span>
      {badge ? (
        <>
          <span className="mn-sidebar-nav__badge" data-tone={tone} aria-hidden>
            {badge > 99 ? '99+' : badge}
          </span>
          <span className="mn-visually-hidden">, {item.badgeLabel ?? spoken}</span>
        </>
      ) : null}
      {item.external && safeHref(item.href) ? (
        <span className="mn-visually-hidden"> ({ctx.messages.newTab})</span>
      ) : null}
    </>
  );
}

function WithTip({ ctx, label, children }: { ctx: Ctx; label: string; children: ReactElement }) {
  // Open away from the sidebar: Radix sides are physical, so flip them in RTL.
  const side = useDirectionContext() === 'rtl' ? 'left' : 'right';
  return (
    <Tooltip side={side} content={label} disabled={!ctx.collapsed}>
      {children}
    </Tooltip>
  );
}

function Entry({ item, ctx, depth }: { item: SidebarNavItem; ctx: Ctx; depth: number }) {
  const children = item.items ?? [];
  const active = isActive(item, ctx.currentPath);
  if (children.length > 0) {
    const open = ctx.open(item) && !ctx.collapsed;
    const groupId = `${ctx.baseId}-${item.key}-group`;
    const hasCurrent = containsActive(item, ctx.currentPath);
    return (
      <li className="mn-sidebar-nav__entry">
        <WithTip ctx={ctx} label={item.label}>
          <button
            type="button"
            className="mn-sidebar-nav__item"
            data-depth={depth}
            data-group=""
            data-contains-current={hasCurrent || undefined}
            aria-expanded={open}
            aria-controls={open ? groupId : undefined}
            onClick={() => {
              item.onSelect?.();
              if (ctx.collapsed) {
                ctx.expandSidebar();
                ctx.toggle(item, true);
              } else ctx.toggle(item, !open);
            }}
          >
            <ItemBody item={item} ctx={ctx} />
            <span className="mn-sidebar-nav__chevron" aria-hidden>
              <ChevronRightGlyph />
            </span>
          </button>
        </WithTip>
        {open ? (
          <ul id={groupId} className="mn-sidebar-nav__list" data-nested="">
            {children.map((c) => (
              <Entry key={c.key} item={c} ctx={ctx} depth={depth + 1} />
            ))}
          </ul>
        ) : null}
      </li>
    );
  }

  const href = safeHref(item.href);
  const common = {
    className: 'mn-sidebar-nav__item',
    'data-depth': depth,
    'data-active': active ? '' : undefined,
  };
  const activate = () => {
    item.onSelect?.();
    ctx.onNavigate?.(item);
  };
  let control: ReactElement;
  if (href) {
    const Comp = item.external ? 'a' : ctx.linkAs;
    control = (
      <Comp
        {...common}
        href={href}
        aria-current={active ? 'page' : undefined}
        onClick={activate}
        {...(item.external ? { target: '_blank', rel: 'noopener noreferrer' } : {})}
      >
        <ItemBody item={item} ctx={ctx} />
      </Comp>
    );
  } else {
    control = (
      <button
        type="button"
        {...common}
        aria-current={active ? 'true' : undefined}
        onClick={activate}
      >
        <ItemBody item={item} ctx={ctx} />
      </button>
    );
  }
  return (
    <li className="mn-sidebar-nav__entry">
      <WithTip ctx={ctx} label={item.label}>
        {control}
      </WithTip>
    </li>
  );
}

function PanelGlyph() {
  return (
    <svg
      width="16"
      height="16"
      viewBox="0 0 16 16"
      fill="none"
      stroke="currentColor"
      strokeWidth={1.75}
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden
      focusable={false}
    >
      <rect x="2" y="2.5" width="12" height="11" rx="1.5" />
      <path d="M6 2.5v11" />
    </svg>
  );
}

/**
 * The app's sidebar navigation: labelled sections of links, nested groups
 * that open and close, the current page marked with `aria-current="page"`,
 * count badges, and an icon-only collapsed mode where each item shows its
 * label in a tooltip. Router-agnostic: pass `currentPath` and your router's
 * link as `linkAs`.
 */
export const SidebarNav = forwardRef<HTMLElement, SidebarNavProps>(function SidebarNav(
  {
    sections,
    currentPath,
    linkAs = 'a',
    collapsed: collapsedProp,
    defaultCollapsed = false,
    onCollapsedChange,
    collapsible = false,
    header,
    footer,
    onNavigate,
    messages: messagesProp,
    className,
    'aria-label': ariaLabel,
    ...rest
  },
  ref,
) {
  const messages = { ...SIDEBAR_NAV_MESSAGES, ...messagesProp };
  const baseId = `mn-sidebar-nav-${useId().replace(/:/g, '')}`;
  const [innerCollapsed, setInnerCollapsed] = useState(defaultCollapsed);
  const collapsed = collapsedProp ?? innerCollapsed;
  const setCollapsed = useCallback(
    (next: boolean) => {
      if (collapsedProp === undefined) setInnerCollapsed(next);
      onCollapsedChange?.(next);
    },
    [collapsedProp, onCollapsedChange],
  );
  // Groups the person opened or closed; others follow defaultOpen / the current page.
  const [toggled, setToggled] = useState<Record<string, boolean>>({});
  const ctx: Ctx = {
    baseId,
    collapsed,
    currentPath,
    linkAs,
    messages,
    open: (item) =>
      toggled[item.key] ?? (item.defaultOpen === true || containsActive(item, currentPath)),
    toggle: (item, next) => setToggled((t) => ({ ...t, [item.key]: next })),
    expandSidebar: () => setCollapsed(false),
    onNavigate,
  };

  return (
    <TooltipProvider delayDuration={100}>
      <nav
        ref={ref}
        {...rest}
        aria-label={ariaLabel ?? messages.nav}
        data-collapsed={collapsed || undefined}
        className={cx('mn-sidebar-nav', className)}
      >
        {header ? <div className="mn-sidebar-nav__header">{header}</div> : null}
        <div className="mn-sidebar-nav__body">
          {sections.map((section) => {
            const labelId = `${baseId}-${section.key}-label`;
            return (
              <div key={section.key} className="mn-sidebar-nav__section">
                {section.label ? (
                  <div id={labelId} className="mn-sidebar-nav__section-label">
                    {section.label}
                  </div>
                ) : null}
                <ul
                  className="mn-sidebar-nav__list"

                  aria-labelledby={section.label ? labelId : undefined}
                >
                  {section.items.map((item) => (
                    <Entry key={item.key} item={item} ctx={ctx} depth={0} />
                  ))}
                </ul>
              </div>
            );
          })}
        </div>
        {footer ? <div className="mn-sidebar-nav__footer">{footer}</div> : null}
        {collapsible ? (
          <div className="mn-sidebar-nav__toggle-row">
            <WithTip ctx={ctx} label={messages.expand}>
              <button
                type="button"
                className="mn-sidebar-nav__item mn-sidebar-nav__toggle"
                onClick={() => setCollapsed(!collapsed)}
              >
                <span className="mn-sidebar-nav__icon" aria-hidden>
                  <PanelGlyph />
                </span>
                <span className="mn-sidebar-nav__label">
                  {collapsed ? messages.expand : messages.collapse}
                </span>
              </button>
            </WithTip>
          </div>
        ) : null}
      </nav>
    </TooltipProvider>
  );
});
