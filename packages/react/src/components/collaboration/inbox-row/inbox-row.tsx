'use client';

import {
  forwardRef,
  useEffect,
  useRef,
  type AnchorHTMLAttributes,
  type HTMLAttributes,
  type MouseEvent,
  type ReactNode,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { CheckGlyph } from '../../../internal/icons.js';
import { Avatar } from '../../identity/avatar/index.js';

/** A date, a timestamp in milliseconds, or an ISO string. */
type DateInput = Date | number | string;

/** Every string the inbox row shows or announces. */
export interface InboxRowMessages {
  /** Name of the selection checkbox. */
  select: (title: string) => string;
  /** Read for an unread row; receives the count, or null for a plain unread mark. */
  unread: (count: number | null) => string;
  /** The compact age units. */
  age: CompactAgeUnits;
}

export const INBOX_ROW_MESSAGES: InboxRowMessages = {
  select: (title) => `Select ${title}`,
  unread: (n) => (n === null ? 'Unread' : n === 1 ? '1 unread' : `${n} unread`),
  age: {
    now: 'now',
    minutes: (n) => `${n}m`,
    hours: (n) => `${n}h`,
    days: (n) => `${n}d`,
    weeks: (n) => `${n}w`,
  },
};

/** The short age strings used by `formatCompactAge`. */
export interface CompactAgeUnits {
  now: string;
  minutes: (n: number) => string;
  hours: (n: number) => string;
  days: (n: number) => string;
  weeks: (n: number) => string;
}

const toDate = (d: DateInput) => (d instanceof Date ? d : new Date(d));

/**
 * A short age for dense lists: "now", "5m", "3h", "2d", "4w", then a short
 * date ("12 Mar"; with the year when it is not this year).
 */
export function formatCompactAge(
  date: DateInput,
  now: DateInput = Date.now(),
  locale?: string,
  units: CompactAgeUnits = INBOX_ROW_MESSAGES.age,
): string {
  const then = toDate(date);
  const diff = Math.max(0, toDate(now).getTime() - then.getTime());
  const min = Math.floor(diff / 60_000);
  if (min < 1) return units.now;
  if (min < 60) return units.minutes(min);
  const h = Math.floor(min / 60);
  if (h < 24) return units.hours(h);
  const d = Math.floor(h / 24);
  if (d < 7) return units.days(d);
  if (d < 30) return units.weeks(Math.floor(d / 7));
  const sameYear = then.getFullYear() === toDate(now).getFullYear();
  return new Intl.DateTimeFormat(locale, {
    day: 'numeric',
    month: 'short',
    ...(sameYear ? {} : { year: 'numeric' }),
  }).format(then);
}

export interface InboxRowProps extends Omit<HTMLAttributes<HTMLLIElement>, 'title'> {
  /** Who or what the conversation is with: the requester, channel or person. */
  title: string;
  /** Where the conversation opens. The row is a real link, so middle-click and "open in new tab" work. */
  href: string;
  /**
   * Opens the conversation in place on a plain click (the link's default is
   * prevented). Clicks with Ctrl, Cmd or a middle button keep the browser's
   * behaviour. Without it the link navigates normally.
   */
  onOpen?: ((event: MouseEvent<HTMLAnchorElement>) => void) | undefined;
  /** The latest message or a summary, on one line. */
  preview?: ReactNode | undefined;
  /** When the conversation last changed. Shown as a compact age ("5m"), with the full time on hover. */
  timestamp?: DateInput | undefined;
  /** "Now" for the age, for tests and server rendering. */
  now?: DateInput | undefined;
  /** BCP 47 locale for dates. */
  locale?: string | undefined;
  /** Avatar of the person, or initials of the title. Pass null for no avatar. */
  avatar?: { name?: string | undefined; src?: string | null | undefined } | null | undefined;
  /** Small icon after the title, such as the channel (email, chat, phone). Decorative; describe it in `iconLabel`. */
  icon?: ReactNode | undefined;
  /** Spoken description of `icon`, e.g. "via email". */
  iconLabel?: string | undefined;
  /** Unread: `true` for a dot and bold text, or a number for a count (shown as 99+ above 99). */
  unread?: boolean | number | undefined;
  /** This conversation is open next to the list (`aria-current`). */
  current?: boolean | undefined;
  /** The keyboard cursor of the list is on this row. */
  active?: boolean | undefined;
  /** Markers on the last line: status, SLA, priority. */
  badges?: ReactNode | undefined;
  /** End of the last line: the reference number, assignee avatar. */
  trailing?: ReactNode | undefined;
  /** Whether the row is selected for bulk actions. Shows the checkbox. */
  selected?: boolean | undefined;
  /** Something in the list is selected: checkboxes show on every row and a plain click selects instead of opening. */
  selecting?: boolean | undefined;
  /** Makes the row selectable. `shiftKey` is true for a range selection. Long-press selects on touch screens. */
  onSelectedChange?: ((selected: boolean, details: { shiftKey: boolean }) => void) | undefined;
  /** Props for the link element. */
  linkProps?: Omit<AnchorHTMLAttributes<HTMLAnchorElement>, 'href' | 'children'> | undefined;
  /** Override any string. */
  messages?: Partial<InboxRowMessages> | undefined;
}

const LONG_PRESS_MS = 500;

/**
 * One conversation in an inbox: avatar, name, age, a preview line and
 * markers, with unread, open and selected states. The row is a real link;
 * with `onSelectedChange` the avatar becomes a selection checkbox for bulk
 * actions. Use inside `InboxList`; it works at a fixed height in a
 * virtualised list.
 */
export const InboxRow = forwardRef<HTMLLIElement, InboxRowProps>(function InboxRow(
  {
    title,
    href,
    onOpen,
    preview,
    timestamp,
    now,
    locale,
    avatar,
    icon,
    iconLabel,
    unread = false,
    current = false,
    active = false,
    badges,
    trailing,
    selected = false,
    selecting = false,
    onSelectedChange,
    linkProps,
    messages: messagesProp,
    className,
    ...rest
  },
  ref,
) {
  const m = { ...INBOX_ROW_MESSAGES, ...messagesProp };
  const selectable = Boolean(onSelectedChange);
  const isUnread = unread !== false && unread !== 0;
  const count = typeof unread === 'number' ? unread : null;
  const timer = useRef<ReturnType<typeof setTimeout> | null>(null);
  const pressed = useRef(false);
  useEffect(
    () => () => {
      if (timer.current) clearTimeout(timer.current);
    },
    [],
  );
  const cancelPress = () => {
    if (timer.current) clearTimeout(timer.current);
    timer.current = null;
  };

  const showAvatar = avatar !== null;
  const avatarName = avatar?.name ?? title;
  const absolute =
    timestamp !== undefined
      ? new Intl.DateTimeFormat(locale, { dateStyle: 'medium', timeStyle: 'short' }).format(
          toDate(timestamp),
        )
      : undefined;

  const onClick = (event: MouseEvent<HTMLAnchorElement>) => {
    linkProps?.onClick?.(event);
    if (event.defaultPrevented) return;
    if (pressed.current) {
      event.preventDefault();
      pressed.current = false;
      return;
    }
    if (event.metaKey || event.ctrlKey || event.altKey || event.button !== 0) return;
    if (selectable && (event.shiftKey || selecting)) {
      event.preventDefault();
      onSelectedChange?.(!selected, { shiftKey: event.shiftKey });
      return;
    }
    if (onOpen) {
      event.preventDefault();
      onOpen(event);
    }
  };

  return (
    <li
      ref={ref}
      {...rest}
      className={cx('mn-inbox-row', className)}
      data-unread={isUnread || undefined}
      data-current={current || undefined}
      data-active={active || undefined}
      data-selected={selected || undefined}
      data-selecting={selecting || undefined}
    >
      {selectable ? (
        <button
          type="button"
          role="checkbox"
          aria-checked={selected}
          aria-label={m.select(title)}
          className="mn-inbox-row__select"
          onClick={(e) => onSelectedChange?.(!selected, { shiftKey: e.shiftKey })}
        >
          {showAvatar ? (
            <Avatar
              className="mn-inbox-row__avatar"
              name={avatarName}
              src={avatar?.src ?? null}
              size="sm"
              decorative
            />
          ) : null}
          <span className="mn-inbox-row__box" aria-hidden>
            {selected ? <CheckGlyph /> : null}
          </span>
        </button>
      ) : showAvatar ? (
        <span className="mn-inbox-row__lead">
          <Avatar
            className="mn-inbox-row__avatar"
            name={avatarName}
            src={avatar?.src ?? null}
            size="sm"
            decorative
          />
        </span>
      ) : null}
      <a
        {...linkProps}
        href={href}
        aria-current={current ? 'true' : undefined}
        className={cx('mn-inbox-row__link', linkProps?.className)}
        onClick={onClick}
        onPointerDown={(e) => {
          linkProps?.onPointerDown?.(e);
          if (!selectable || e.pointerType !== 'touch') return;
          cancelPress();
          timer.current = setTimeout(() => {
            pressed.current = true;
            onSelectedChange?.(!selected, { shiftKey: false });
          }, LONG_PRESS_MS);
        }}
        onPointerUp={cancelPress}
        onPointerCancel={cancelPress}
        onPointerMove={cancelPress}
        onContextMenu={(e) => {
          linkProps?.onContextMenu?.(e);
          if (pressed.current) e.preventDefault();
        }}
      >
        <span className="mn-inbox-row__line">
          {isUnread && count === null ? <span className="mn-inbox-row__dot" aria-hidden /> : null}
          <span className="mn-inbox-row__title">{title}</span>
          {icon ? (
            <span className="mn-inbox-row__icon" aria-hidden>
              {icon}
            </span>
          ) : null}
          {iconLabel ? <span className="mn-visually-hidden">, {iconLabel}</span> : null}
          {isUnread ? <span className="mn-visually-hidden">, {m.unread(count)}</span> : null}
          {timestamp !== undefined ? (
            <time
              className="mn-inbox-row__time"
              dateTime={toDate(timestamp).toISOString()}
              title={absolute}
            >
              <span aria-hidden>{formatCompactAge(timestamp, now, locale, m.age)}</span>
              <span className="mn-visually-hidden">, {absolute}</span>
            </time>
          ) : null}
          {count !== null && count > 0 ? (
            <span className="mn-inbox-row__count" aria-hidden>
              {count > 99 ? '99+' : count}
            </span>
          ) : null}
        </span>
        {preview ? <span className="mn-inbox-row__preview">{preview}</span> : null}
        {badges || trailing ? (
          <span className="mn-inbox-row__line mn-inbox-row__line--meta">
            {badges ? <span className="mn-inbox-row__badges">{badges}</span> : null}
            {trailing ? <span className="mn-inbox-row__trailing">{trailing}</span> : null}
          </span>
        ) : null}
      </a>
    </li>
  );
});

export type InboxListProps = HTMLAttributes<HTMLUListElement>;

/** The list that holds InboxRows. Name it with `aria-label`, e.g. "Open conversations, 24". */
export const InboxList = forwardRef<HTMLUListElement, InboxListProps>(function InboxList(
  { className, ...rest },
  ref,
) {
  return <ul ref={ref} {...rest} className={cx('mn-inbox-list', className)} />;
});
