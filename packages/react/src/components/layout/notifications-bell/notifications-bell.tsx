'use client';

import { forwardRef, useState } from 'react';
import {
  NotificationCenter,
  type NotificationCenterMessages,
  type NotificationCenterProps,
  type NotificationItem,
} from '../../collaboration/notification-center/index.js';
import { cx } from '../../../internal/cx.js';
import { BellGlyph } from '../../../internal/shell/icons.js';
import { ShellLink, type ShellLinkComponent } from '../../../internal/shell/link.js';
import { TipButton } from '../../../internal/shell/tip-button.js';

export interface NotificationsBellMessages extends NotificationCenterMessages {
  /** The footer link to the full notifications page. Default "See all notifications". */
  seeAll: string;
}

export interface NotificationsBellProps extends Omit<
  NotificationCenterProps,
  'trigger' | 'messages' | 'footer'
> {
  /**
   * `icon` (default): a square icon button for an identity bar or page header.
   * `row`: a full-width labelled row for a rail, a drawer or a mobile menu.
   */
  variant?: 'icon' | 'row' | undefined;
  /** With `variant="row"`: icon only, with the name in a tooltip (a collapsed rail). */
  collapsed?: boolean | undefined;
  /** Link to the full notifications page, shown under the list. */
  seeAllHref?: string | undefined;
  /** Link element for client navigation of `seeAllHref`. Default `<a>`. */
  linkComponent?: ShellLinkComponent | undefined;
  /** Close the panel when an item is chosen. Default true. */
  closeOnSelect?: boolean | undefined;
  /** Interface text, for translation. Includes every NotificationCenter message. */
  messages?: Partial<NotificationsBellMessages> | undefined;
}

function badgeText(n: number): string {
  return n > 99 ? '99+' : String(n);
}

/**
 * The shell's notifications entry: a bell (or a labelled row) with the unread
 * count that opens the NotificationCenter panel, with an optional "See all"
 * link. The app loads the notifications, marks them read and navigates; the
 * bell only shows them.
 */
export const NotificationsBell = forwardRef<HTMLButtonElement, NotificationsBellProps>(
  function NotificationsBell(
    {
      variant = 'icon',
      collapsed = false,
      seeAllHref,
      linkComponent,
      closeOnSelect = true,
      items,
      unreadCount,
      open: openProp,
      defaultOpen = false,
      onOpenChange,
      onSelect,
      side,
      align,
      messages: messagesProp,
      className,
      // NotificationCenter's own props go to the panel…
      filter,
      defaultFilter,
      onFilterChange,
      onMarkRead,
      onMarkAllRead,
      markReadOnSelect,
      hasMore,
      loadingMore,
      loadMoreError,
      onLoadMore,
      loading,
      error,
      empty,
      groupByDay,
      now,
      locale,
      timeZone,
      panelClassName,
      // …and the rest (data-*, id, handlers) to the trigger button.
      ...buttonProps
    },
    ref,
  ) {
    const [openInner, setOpenInner] = useState(defaultOpen);
    const open = openProp ?? openInner;
    const setOpen = (next: boolean) => {
      if (openProp === undefined) setOpenInner(next);
      onOpenChange?.(next);
    };
    const unread = unreadCount ?? items.filter((i) => !i.read).length;
    const title = messagesProp?.title ?? 'Notifications';
    const triggerLabel =
      messagesProp?.triggerLabel ?? ((n: number) => (n > 0 ? `${title}, ${n} unread` : title));
    const name = triggerLabel(unread);
    const seeAll = messagesProp?.seeAll ?? 'See all notifications';
    const row = variant === 'row';

    const trigger = (
      <TipButton
        ref={ref}
        {...buttonProps}
        className={cx('mn-notifications-bell', className)}
        data-variant={variant}
        data-collapsed={row && collapsed ? '' : undefined}
        data-unread={unread > 0 ? '' : undefined}
        // An icon button has no visible text, so it gets the tooltip and the full name.
        tip={!row || collapsed ? name : undefined}
        tipSide={row ? 'right' : 'bottom'}
        // Starts with the visible label ("Notifications"), then the unread count.
        aria-label={name}
      >
        <span className="mn-notifications-bell__icon" aria-hidden>
          <BellGlyph />
          {unread > 0 && (!row || collapsed) ? (
            <span className="mn-notifications-bell__badge">{badgeText(unread)}</span>
          ) : null}
        </span>
        {row && !collapsed ? (
          <>
            <span className="mn-notifications-bell__label">{title}</span>
            {unread > 0 ? (
              <>
                <span className="mn-notifications-bell__count" aria-hidden>
                  {badgeText(unread)}
                </span>
              </>
            ) : null}
          </>
        ) : null}
      </TipButton>
    );

    return (
      <NotificationCenter
        {...(filter !== undefined ? { filter } : {})}
        {...(defaultFilter !== undefined ? { defaultFilter } : {})}
        {...(onFilterChange ? { onFilterChange } : {})}
        {...(onMarkRead ? { onMarkRead } : {})}
        {...(onMarkAllRead ? { onMarkAllRead } : {})}
        {...(markReadOnSelect !== undefined ? { markReadOnSelect } : {})}
        {...(hasMore !== undefined ? { hasMore } : {})}
        {...(loadingMore !== undefined ? { loadingMore } : {})}
        {...(loadMoreError !== undefined ? { loadMoreError } : {})}
        {...(onLoadMore ? { onLoadMore } : {})}
        {...(loading !== undefined ? { loading } : {})}
        {...(error !== undefined ? { error } : {})}
        {...(empty !== undefined ? { empty } : {})}
        {...(groupByDay !== undefined ? { groupByDay } : {})}
        {...(now !== undefined ? { now } : {})}
        {...(locale !== undefined ? { locale } : {})}
        {...(timeZone !== undefined ? { timeZone } : {})}
        {...(panelClassName !== undefined ? { panelClassName } : {})}
        items={items}
        unreadCount={unread}
        open={open}
        onOpenChange={setOpen}
        onSelect={(item: NotificationItem) => {
          if (closeOnSelect) setOpen(false);
          onSelect?.(item);
        }}
        side={side ?? (row ? 'right' : 'bottom')}
        align={align ?? 'end'}
        trigger={trigger}
        messages={{ ...messagesProp, title, triggerLabel }}
        footer={
          seeAllHref ? (
            <ShellLink
              as={linkComponent}
              href={seeAllHref}
              className="mn-notifications-bell__see-all"
              onClick={() => setOpen(false)}
            >
              {seeAll}
            </ShellLink>
          ) : undefined
        }
      />
    );
  },
);
