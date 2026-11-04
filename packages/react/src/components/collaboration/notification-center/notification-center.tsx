'use client';

import {
  forwardRef,
  useId,
  useState,
  type ButtonHTMLAttributes,
  type ReactElement,
  type ReactNode,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { BellGlyph, CheckGlyph } from '../../../internal/icons.js';
import { dayLabel, groupByDay } from '../../../internal/day-groups.js';
import { Popover, PopoverContent, PopoverTrigger } from '../../overlays/popover/index.js';
import { TabsFilter } from '../../navigation/tabs/index.js';
import { Button, IconButton } from '../../actions/button/index.js';
import { Avatar } from '../../identity/avatar/index.js';
import { RelativeTime, type DateInput } from '../../data-display/relative-time/index.js';
import { LoadMore } from '../../data-grid/load-more/index.js';
import { EmptyState } from '../../feedback/empty-state/index.js';
import { Skeleton } from '../../feedback/skeleton/index.js';

export interface NotificationItem {
  id: string;
  /** The headline, e.g. "Ana Lopez mentioned you in Design review". */
  title: ReactNode;
  /** A snippet or extra context, one or two lines. */
  description?: ReactNode;
  /** When it happened. A Date, epoch milliseconds or an ISO 8601 string. */
  time: DateInput;
  /** Already read. Unread items are bold, carry a dot and are named "Unread" for screen readers. */
  read?: boolean;
  /** Who caused it. Shown as an avatar unless `icon` is given. */
  actor?: { name: string; avatarSrc?: string | null; seed?: string };
  /** Replaces the avatar, e.g. a system or app glyph. Decorative. */
  icon?: ReactNode;
  /** Where the notification leads. Rendered as a link; `onSelect` is still called. */
  href?: string;
}

export type NotificationFilter = 'all' | 'unread';

export interface NotificationCenterMessages {
  /** Panel heading and trigger name. Default "Notifications". */
  title: string;
  /** Trigger name with the unread count. Default "Notifications, 3 unread". */
  triggerLabel: (unread: number) => string;
  all: string;
  unread: string;
  /** Name of the filter tabs. Default "Show". */
  filterLabel: string;
  markAllRead: string;
  markRead: string;
  /** Hidden text on unread items. Default "Unread". */
  unreadItem: string;
  today: string;
  yesterday: string;
  /** Empty state title. Default "You’re all caught up". */
  emptyTitle: string;
  /** Empty state text. Default "New activity will appear here." */
  emptyDescription: string;
  /** Empty state in the Unread tab. Default "No unread notifications". */
  emptyUnreadTitle: string;
  /** Spoken name of the loading skeleton. Default "Loading notifications". */
  loading: string;
}

const DEFAULT_MESSAGES: NotificationCenterMessages = {
  title: 'Notifications',
  triggerLabel: (n) => (n > 0 ? `Notifications, ${n} unread` : 'Notifications'),
  all: 'All',
  unread: 'Unread',
  filterLabel: 'Show',
  markAllRead: 'Mark all as read',
  markRead: 'Mark as read',
  unreadItem: 'Unread',
  today: 'Today',
  yesterday: 'Yesterday',
  emptyTitle: 'You’re all caught up',
  emptyDescription: 'New activity will appear here.',
  emptyUnreadTitle: 'No unread notifications',
  loading: 'Loading notifications',
};

export interface NotificationCenterProps extends Omit<
  ButtonHTMLAttributes<HTMLButtonElement>,
  'onSelect' | 'title'
> {
  /** Loaded notifications, newest first. */
  items: readonly NotificationItem[];
  /** Unread total from the server, which may exceed the loaded items. Default: unread items in `items`. */
  unreadCount?: number;
  open?: boolean;
  defaultOpen?: boolean;
  onOpenChange?: (open: boolean) => void;
  /** The All / Unread tab. The list is filtered by `read`; fetch server-side too if you page. */
  filter?: NotificationFilter;
  defaultFilter?: NotificationFilter;
  onFilterChange?: (filter: NotificationFilter) => void;
  /** An item was activated. Navigate here, or use `href`. */
  onSelect?: (item: NotificationItem) => void;
  /** Marks one item read. Without it items have no "Mark as read" button. */
  onMarkRead?: (id: string) => void;
  /** Marks everything read. Without it there is no "Mark all as read" button. */
  onMarkAllRead?: () => void;
  /** Call `onMarkRead` when an unread item is activated. Default `true`. */
  markReadOnSelect?: boolean;
  /** Another page exists. Shows LoadMore at the end of the list. */
  hasMore?: boolean;
  loadingMore?: boolean;
  loadMoreError?: boolean;
  onLoadMore?: () => void;
  /** First load: shows a skeleton. */
  loading?: boolean;
  /** First-load failure, e.g. an InlineErrorRetry. Replaces the list. */
  error?: ReactNode;
  /** Replaces the built-in empty state. */
  empty?: ReactNode;
  /** Below the list: a link to all notifications or to preferences. */
  footer?: ReactNode;
  /** Group items under Today, Yesterday and dates. Default `true`. */
  groupByDay?: boolean;
  /** A custom trigger element (a rail item, a menu row). Give it the unread count in its name. */
  trigger?: ReactElement;
  /** Panel alignment against the trigger. Default `end`. */
  align?: 'start' | 'center' | 'end';
  side?: 'top' | 'right' | 'bottom' | 'left';
  /** Reference time for relative times and day groups. */
  now?: DateInput;
  locale?: string;
  timeZone?: string;
  messages?: Partial<NotificationCenterMessages>;
  /** Class for the panel. */
  panelClassName?: string;
}

function badgeText(n: number): string {
  return n > 99 ? '99+' : String(n);
}

/**
 * A bell with the unread count that opens a panel of notifications: All and
 * Unread tabs, day groups, mark one or all as read, load more and an empty
 * state. The product supplies the data and callbacks.
 */
export const NotificationCenter = forwardRef<HTMLButtonElement, NotificationCenterProps>(
  function NotificationCenter(
    {
      items,
      unreadCount,
      open: openProp,
      defaultOpen = false,
      onOpenChange,
      filter: filterProp,
      defaultFilter = 'all',
      onFilterChange,
      onSelect,
      onMarkRead,
      onMarkAllRead,
      markReadOnSelect = true,
      hasMore = false,
      loadingMore = false,
      loadMoreError = false,
      onLoadMore,
      loading = false,
      error,
      empty,
      footer,
      groupByDay: grouping = true,
      trigger,
      align = 'end',
      side = 'bottom',
      now,
      locale,
      timeZone,
      messages: messagesProp,
      panelClassName,
      className,
      ...rest
    },
    ref,
  ) {
    const m = { ...DEFAULT_MESSAGES, ...messagesProp };
    const id = useId();
    const titleId = `${id}-title`;
    const [openInner, setOpenInner] = useState(defaultOpen);
    const open = openProp ?? openInner;
    const setOpen = (next: boolean) => {
      if (openProp === undefined) setOpenInner(next);
      onOpenChange?.(next);
    };
    const [filterInner, setFilterInner] = useState<NotificationFilter>(defaultFilter);
    const filter = filterProp ?? filterInner;
    const setFilter = (next: string) => {
      const value = next === 'unread' ? 'unread' : 'all';
      if (filterProp === undefined) setFilterInner(value);
      onFilterChange?.(value);
    };

    const unread = unreadCount ?? items.filter((i) => !i.read).length;
    const visible = filter === 'unread' ? items.filter((i) => !i.read) : items;
    const timeProps = {
      ...(now !== undefined ? { now } : {}),
      ...(locale !== undefined ? { locale } : {}),
      ...(timeZone !== undefined ? { timeZone } : {}),
    };

    const activate = (item: NotificationItem) => {
      if (!item.read && markReadOnSelect) onMarkRead?.(item.id);
      onSelect?.(item);
    };

    const row = (item: NotificationItem) => {
      const content = (
        <>
          <span className="mn-notification-center__media" aria-hidden>
            {item.icon ??
              (item.actor ? (
                <Avatar
                  name={item.actor.name}
                  src={item.actor.avatarSrc ?? null}
                  size="sm"
                  decorative
                  {...(item.actor.seed !== undefined ? { seed: item.actor.seed } : {})}
                />
              ) : (
                <span className="mn-notification-center__glyph">
                  <BellGlyph />
                </span>
              ))}
          </span>
          <span className="mn-notification-center__text">
            {!item.read ? <span className="mn-visually-hidden">{`${m.unreadItem}: `}</span> : null}
            <span className="mn-notification-center__title">{item.title}</span>
            {item.description ? (
              <span className="mn-notification-center__description">{item.description}</span>
            ) : null}
            <RelativeTime
              className="mn-notification-center__time"
              date={item.time}
              tooltip={false}
              {...timeProps}
            />
          </span>
          {!item.read ? <span className="mn-notification-center__dot" aria-hidden /> : null}
        </>
      );
      return (
        <li
          key={item.id}
          className="mn-notification-center__item"
          data-unread={!item.read || undefined}
        >
          {item.href ? (
            <a
              className="mn-notification-center__main"
              href={item.href}
              onClick={() => activate(item)}
            >
              {content}
            </a>
          ) : (
            <button
              type="button"
              className="mn-notification-center__main"
              onClick={() => activate(item)}
            >
              {content}
            </button>
          )}
          {!item.read && onMarkRead ? (
            <IconButton
              size="xs"
              className="mn-notification-center__mark"
              label={m.markRead}
              icon={<CheckGlyph />}
              onClick={() => onMarkRead(item.id)}
            />
          ) : null}
        </li>
      );
    };

    let list: ReactNode;
    if (loading) {
      list = (
        <div className="mn-notification-center__skeleton" role="img" aria-label={m.loading}>
          {[0, 1, 2].map((i) => (
            <div key={i} className="mn-notification-center__skeleton-row">
              <Skeleton shape="circle" width="1.75rem" height="1.75rem" />
              <div className="mn-notification-center__skeleton-lines">
                <Skeleton shape="text" width="85%" />
                <Skeleton shape="text" width="40%" />
              </div>
            </div>
          ))}
        </div>
      );
    } else if (error) {
      list = <div className="mn-notification-center__error">{error}</div>;
    } else if (visible.length === 0) {
      list = empty ?? (
        <EmptyState
          size="compact"
          headingLevel="h3"
          kind={filter === 'unread' ? 'success' : 'first-run'}
          icon={<BellGlyph />}
          title={filter === 'unread' ? m.emptyUnreadTitle : m.emptyTitle}
          description={m.emptyDescription}
        />
      );
    } else if (grouping) {
      list = groupByDay(visible, (i) => i.time, timeZone).map((g) => {
        const headingId = `${id}-day-${g.key}`;
        return (
          <div key={g.key} className="mn-notification-center__day">
            <h3 id={headingId} className="mn-notification-center__day-label">
              {dayLabel(
                g.date,
                { now, locale, timeZone },
                { today: m.today, yesterday: m.yesterday },
              )}
            </h3>
            <ul className="mn-notification-center__list" aria-labelledby={headingId}>
              {g.items.map(row)}
            </ul>
          </div>
        );
      });
    } else {
      list = <ul className="mn-notification-center__list">{visible.map(row)}</ul>;
    }

    return (
      <Popover open={open} onOpenChange={setOpen}>
        <PopoverTrigger asChild>
          {trigger ?? (
            <button
              ref={ref}
              type="button"
              {...rest}
              aria-label={m.triggerLabel(unread)}
              className={cx('mn-notification-center', className)}
              data-unread={unread > 0 || undefined}
            >
              <BellGlyph className="mn-notification-center__bell" />
              {unread > 0 ? (
                <span className="mn-notification-center__badge" aria-hidden>
                  {badgeText(unread)}
                </span>
              ) : null}
            </button>
          )}
        </PopoverTrigger>
        <PopoverContent
          align={align}
          side={side}
          aria-labelledby={titleId}
          className={cx('mn-notification-center__panel', panelClassName)}
        >
          <div className="mn-notification-center__header">
            <h2 id={titleId} className="mn-notification-center__heading">
              {m.title}
            </h2>
            {onMarkAllRead ? (
              <Button
                size="xs"
                variant="ghost"
                leadingIcon={<CheckGlyph />}
                disabled={unread === 0}
                onClick={onMarkAllRead}
              >
                {m.markAllRead}
              </Button>
            ) : null}
          </div>
          <TabsFilter
            aria-label={m.filterLabel}
            className="mn-notification-center__tabs"
            panelClassName="mn-notification-center__body"
            value={filter}
            onValueChange={setFilter}
            options={[
              { value: 'all', label: m.all },
              {
                value: 'unread',
                label: m.unread,
                ...(unread > 0 ? { count: badgeText(unread), countTone: 'accent' as const } : {}),
              },
            ]}
          >
            {list}
            {hasMore && onLoadMore && !loading && !error ? (
              <LoadMore
                hasMore
                loading={loadingMore}
                error={loadMoreError}
                onLoadMore={onLoadMore}
                showCount={false}
              />
            ) : null}
          </TabsFilter>
          {footer ? <div className="mn-notification-center__footer">{footer}</div> : null}
        </PopoverContent>
      </Popover>
    );
  },
);
