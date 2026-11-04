'use client';

import {
  forwardRef,
  Fragment,
  useCallback,
  useEffect,
  useImperativeHandle,
  useLayoutEffect,
  useRef,
  useState,
  type HTMLAttributes,
  type ReactElement,
  type ReactNode,
  type Ref,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { ArrowDownGlyph } from '../../../internal/icons.js';
import { dayKey, dayLabel, sameDay, type DayInput } from '../../../internal/day-groups.js';
import { LoadMore } from '../../../components/data-grid/load-more/index.js';
import { Skeleton } from '../../../components/feedback/skeleton/index.js';

// useLayoutEffect warns during server rendering; the scroll work only matters in the browser.
const useIsoLayoutEffect = typeof window === 'undefined' ? useEffect : useLayoutEffect;

/** The fields MessageTimeline reads from each item. Your items can carry anything else. */
export interface MessageTimelineItem {
  /** Stable, unique id. Used as the React key and to keep the scroll position when older items are added. */
  id: string;
  /** When the message was sent. A Date, epoch milliseconds or an ISO 8601 string. */
  time: DayInput;
  /**
   * The author. Consecutive items with the same `authorId` within
   * `groupWindowMs` are grouped. Leave it out (or null) for items that never
   * group, such as system events.
   */
  authorId?: string | null;
}

export interface MessageTimelineRenderContext<T> {
  /** Follows a message from the same author: render the row `grouped`. */
  grouped: boolean;
  /** Position in `items`. */
  index: number;
  /** The item before this one, if loaded. */
  previous: T | undefined;
}

export interface MessageTimelineHandle {
  /** Scrolls to the newest message. */
  scrollToBottom: (behavior?: ScrollBehavior) => void;
  /** Scrolls a loaded item into the centre of the view. Returns false when it is not loaded. */
  scrollToItem: (id: string, behavior?: ScrollBehavior) => boolean;
}

export interface MessageTimelineMessages {
  /** Name of the log. Default "Messages". */
  label: string;
  /** Text of the unread divider. Default "New messages". */
  newMessages: string;
  today: string;
  yesterday: string;
  /** Jump button. Default "Jump to latest". */
  jumpToLatest: string;
  /** Jump button when messages arrived while scrolled up. Default "1 new message" / "{n} new messages". */
  unseen: (count: number) => string;
  /** Button that loads older history. Default "Load older messages". */
  loadOlder: string;
  /** Default "Loading older messages…". */
  loadingOlder: string;
  /** Default "Couldn’t load older messages." */
  olderError: string;
  /** Spoken name of the loading skeleton. Default "Loading messages". */
  loading: string;
}

const DEFAULT_MESSAGES: MessageTimelineMessages = {
  label: 'Messages',
  newMessages: 'New messages',
  today: 'Today',
  yesterday: 'Yesterday',
  jumpToLatest: 'Jump to latest',
  unseen: (n) => (n === 1 ? '1 new message' : `${n} new messages`),
  loadOlder: 'Load older messages',
  loadingOlder: 'Loading older messages…',
  olderError: 'Couldn’t load older messages.',
  loading: 'Loading messages',
};

const FIVE_MINUTES = 5 * 60_000;

function time(input: DayInput): number {
  return (input instanceof Date ? input : new Date(input)).getTime();
}

/**
 * Whether `next` continues `previous`'s group: same author, same day and
 * within `windowMs`. Exported for products that render rows in their own
 * virtualised list.
 */
export function shouldGroupMessages(
  previous: MessageTimelineItem | undefined,
  next: MessageTimelineItem,
  options: { windowMs?: number; timeZone?: string } = {},
): boolean {
  const { windowMs = FIVE_MINUTES, timeZone } = options;
  if (!previous || previous.authorId == null || next.authorId == null) return false;
  if (previous.authorId !== next.authorId) return false;
  if (!sameDay(previous.time, next.time, timeZone)) return false;
  const gap = time(next.time) - time(previous.time);
  return gap >= 0 && gap < windowMs;
}

/** "Today", "Yesterday" or a date: the text of a day separator. */
export function formatMessageDay(
  date: DayInput,
  options: {
    now?: DayInput;
    locale?: string;
    timeZone?: string;
    labels?: { today: string; yesterday: string };
  } = {},
): string {
  const { labels, ...rest } = options;
  return dayLabel(date, rest, labels);
}

export interface MessageTimelineSeparatorProps extends HTMLAttributes<HTMLDivElement> {
  children: ReactNode;
  /** The spoken text, when `children` is not plain text. */
  label?: string;
}

/** The "Today" line between days. Exported for virtualised lists. */
export function MessageTimelineDaySeparator({
  children,
  label,
  className,
  ...rest
}: MessageTimelineSeparatorProps) {
  return (
    <div
      role="separator"
      aria-label={label ?? (typeof children === 'string' ? children : undefined)}
      {...rest}
      className={cx('mn-message-timeline__separator', className)}
      data-kind="day"
    >
      <span className="mn-message-timeline__separator-text">{children}</span>
    </div>
  );
}

/** The "New messages" line before the first unread message. Exported for virtualised lists. */
export function MessageTimelineUnreadDivider({
  children,
  label,
  className,
  ...rest
}: MessageTimelineSeparatorProps) {
  return (
    <div
      role="separator"
      aria-label={label ?? (typeof children === 'string' ? children : undefined)}
      {...rest}
      className={cx('mn-message-timeline__separator', className)}
      data-kind="unread"
    >
      <span className="mn-message-timeline__separator-text">{children}</span>
    </div>
  );
}

export interface MessageTimelineProps<T extends MessageTimelineItem> extends Omit<
  HTMLAttributes<HTMLDivElement>,
  'children'
> {
  /** Loaded messages, oldest first. */
  items: readonly T[];
  /** Renders one item, normally a MessageRow with `grouped={context.grouped}`. */
  renderItem: (item: T, context: MessageTimelineRenderContext<T>) => ReactNode;
  /**
   * The first unread message. The "New messages" divider is drawn above it.
   * Keep it fixed while the conversation is open, so the divider does not
   * move as the reader catches up.
   */
  firstUnreadId?: string | null;
  /** There is older history to load. */
  hasOlder?: boolean;
  /** Older history is being fetched. */
  loadingOlder?: boolean;
  /** The last fetch of older history failed. Shows Retry and pauses loading on scroll. */
  olderError?: boolean;
  /**
   * Loads the next page of older history. Called when the reader scrolls
   * near the top (or the history does not fill the view), and from the
   * "Load older messages" button. The scroll position is kept when the
   * items are prepended.
   */
  onLoadOlder?: () => void;
  /** Called by the Retry button after `olderError`. Default `onLoadOlder`. */
  onRetryOlder?: () => void;
  /** Distance from the top, in pixels, that triggers `onLoadOlder`. Default 240. */
  loadOlderThreshold?: number;
  /** Follow new messages while the reader is at the bottom. Default `true`. */
  stickToBottom?: boolean;
  /** How close to the bottom, in pixels, counts as "at the bottom". Default 64. */
  atBottomThreshold?: number;
  /** Called when the reader reaches or leaves the bottom, for example to mark the conversation read. */
  onAtBottomChange?: (atBottom: boolean) => void;
  /** Show a "Jump to latest" button while scrolled up. Default `true`. */
  jumpToLatest?: boolean;
  /** Content above the first message once all history is loaded: the conversation intro. */
  header?: ReactNode;
  /** Content after the last message, such as messages still sending. Inside the log. */
  footer?: ReactNode;
  /**
   * Transient status such as "Ana is typing…". Rendered outside the log in
   * its own `role="status"` region, so it is announced once and never
   * recorded as a message.
   */
  status?: ReactNode;
  /** Shown instead of the list when there are no items and nothing is loading. */
  empty?: ReactNode;
  /** First load: shows a skeleton. */
  loading?: boolean;
  /** First-load failure, for example an InlineErrorRetry. Replaces the list. */
  error?: ReactNode;
  /** Highlights a message, for example after a deep link. */
  highlightedId?: string | null;
  /** Scrolls this message into view whenever it changes and is loaded (deep links). */
  scrollToId?: string | null;
  /** Consecutive messages from one author closer than this are grouped. Default 5 minutes. */
  groupWindowMs?: number;
  /** Reference time for "Today" and "Yesterday". */
  now?: DayInput;
  locale?: string;
  /** IANA time zone used to split days. Default: the viewer's zone. */
  timeZone?: string;
  /** Imperative scrolling: `scrollToBottom()` and `scrollToItem(id)`. */
  handleRef?: Ref<MessageTimelineHandle>;
  messages?: Partial<MessageTimelineMessages>;
  /** Class for the scrolling log element. */
  scrollerClassName?: string;
}

function prefersReducedMotion(): boolean {
  return (
    typeof window !== 'undefined' &&
    typeof window.matchMedia === 'function' &&
    window.matchMedia('(prefers-reduced-motion: reduce)').matches
  );
}

function MessageTimelineInner<T extends MessageTimelineItem>(
  {
    items,
    renderItem,
    firstUnreadId,
    hasOlder = false,
    loadingOlder = false,
    olderError = false,
    onLoadOlder,
    onRetryOlder,
    loadOlderThreshold = 240,
    stickToBottom = true,
    atBottomThreshold = 64,
    onAtBottomChange,
    jumpToLatest = true,
    header,
    footer,
    status,
    empty,
    loading = false,
    error,
    highlightedId,
    scrollToId,
    groupWindowMs = FIVE_MINUTES,
    now,
    locale,
    timeZone,
    handleRef,
    messages: messagesProp,
    scrollerClassName,
    className,
    ...rest
  }: MessageTimelineProps<T>,
  ref: Ref<HTMLDivElement>,
) {
  const m = { ...DEFAULT_MESSAGES, ...messagesProp };
  const scroller = useRef<HTMLDivElement | null>(null);
  const content = useRef<HTMLDivElement | null>(null);
  const [atBottom, setAtBottom] = useState(true);
  const [unseen, setUnseen] = useState(0);
  const [settling, setSettling] = useState(false);
  const atBottomRef = useRef(true);
  const snapshot = useRef<{
    first?: string | undefined;
    last?: string | undefined;
    height: number;
    top: number;
  }>({
    height: 0,
    top: 0,
  });
  const requested = useRef(false);
  const initialised = useRef(false);
  const onAtBottomRef = useRef(onAtBottomChange);
  onAtBottomRef.current = onAtBottomChange;

  const behavior = (wanted: ScrollBehavior = 'smooth'): ScrollBehavior =>
    prefersReducedMotion() ? 'auto' : wanted;

  const scrollToBottom = useCallback((how: ScrollBehavior = 'auto') => {
    const el = scroller.current;
    if (!el) return;
    if (typeof el.scrollTo === 'function') el.scrollTo({ top: el.scrollHeight, behavior: how });
    else el.scrollTop = el.scrollHeight;
  }, []);

  const scrollToItem = useCallback((id: string, how: ScrollBehavior = 'auto') => {
    const nodes = scroller.current?.querySelectorAll<HTMLElement>('[data-message-id]') ?? [];
    const el = Array.from(nodes).find((node) => node.dataset.messageId === id);
    if (!el) return false;
    el.scrollIntoView({ block: 'center', behavior: how });
    return true;
  }, []);

  useImperativeHandle(
    handleRef,
    () => ({
      scrollToBottom: (how) => scrollToBottom(behavior(how ?? 'smooth')),
      scrollToItem: (id, how) => scrollToItem(id, behavior(how ?? 'smooth')),
    }),
    [scrollToBottom, scrollToItem],
  );

  const updateAtBottom = useCallback(() => {
    const el = scroller.current;
    if (!el) return;
    const bottom = el.scrollHeight - el.scrollTop - el.clientHeight <= atBottomThreshold;
    if (bottom !== atBottomRef.current) {
      atBottomRef.current = bottom;
      setAtBottom(bottom);
      onAtBottomRef.current?.(bottom);
    }
    if (bottom) setUnseen(0);
  }, [atBottomThreshold]);

  const maybeLoadOlder = useCallback(() => {
    const el = scroller.current;
    if (!el || !onLoadOlder || !hasOlder || loadingOlder || olderError || requested.current) return;
    const nearTop = el.scrollTop <= loadOlderThreshold;
    // A hidden scroller (no height) never counts as unfilled, or it would load the whole history.
    const unfilled = el.clientHeight > 0 && el.scrollHeight <= el.clientHeight;
    if (nearTop || unfilled) {
      requested.current = true;
      onLoadOlder();
    }
  }, [onLoadOlder, hasOlder, loadingOlder, olderError, loadOlderThreshold]);

  // A new load can be requested once the previous one has settled.
  useEffect(() => {
    if (!loadingOlder) requested.current = false;
  }, [loadingOlder, items.length, olderError]);

  // Keep the reader's place: prepends keep the view still, appends follow the
  // bottom only when the reader is there. Runs before paint.
  useIsoLayoutEffect(() => {
    const el = scroller.current;
    if (!el) return;
    const prev = snapshot.current;
    const first = items[0]?.id;
    const last = items[items.length - 1]?.id;
    if (!initialised.current) {
      if (items.length > 0) {
        initialised.current = true;
        if (!(scrollToId && scrollToItem(scrollToId))) scrollToBottom();
      }
    } else if (prev.first !== undefined && first !== prev.first) {
      const k = items.findIndex((item) => item.id === prev.first);
      if (k > 0) {
        el.scrollTop = prev.top + (el.scrollHeight - prev.height);
        setSettling(true);
      }
    }
    if (initialised.current && last !== prev.last && prev.last !== undefined) {
      if (stickToBottom && atBottomRef.current) scrollToBottom();
      else {
        const from = items.findIndex((item) => item.id === prev.last);
        const added = from >= 0 ? items.length - 1 - from : 0;
        if (added > 0) setUnseen((n) => n + added);
      }
    }
    snapshot.current = { first, last, height: el.scrollHeight, top: el.scrollTop };
  }, [items]);

  // aria-busy for a moment after a prepend, so screen readers do not read old history as new.
  useEffect(() => {
    if (!settling) return;
    const t = setTimeout(() => setSettling(false), 300);
    return () => clearTimeout(t);
  }, [settling]);

  // Fill the view: keep loading older pages while the history is shorter than the scroller.
  useEffect(() => {
    maybeLoadOlder();
  }, [maybeLoadOlder, items.length]);

  // Deep links.
  useEffect(() => {
    if (scrollToId && initialised.current) scrollToItem(scrollToId, behavior('smooth'));
  }, [scrollToId, scrollToItem]);

  // Content that grows after render (images, link previews) keeps the bottom pinned.
  useEffect(() => {
    const node = content.current;
    if (!node || typeof ResizeObserver === 'undefined') return;
    const observer = new ResizeObserver(() => {
      const el = scroller.current;
      if (!el) return;
      if (stickToBottom && atBottomRef.current) scrollToBottom();
      snapshot.current = { ...snapshot.current, height: el.scrollHeight, top: el.scrollTop };
    });
    observer.observe(node);
    return () => observer.disconnect();
  }, [stickToBottom, scrollToBottom]);

  const onScroll = () => {
    const el = scroller.current;
    if (el) snapshot.current = { ...snapshot.current, height: el.scrollHeight, top: el.scrollTop };
    updateAtBottom();
    maybeLoadOlder();
  };

  const jump = () => {
    scrollToBottom(behavior('smooth'));
    setUnseen(0);
    // The button disappears; keep focus in the conversation.
    scroller.current?.focus({ preventScroll: true });
  };

  const showList = !loading && !error && items.length > 0;
  const rows: ReactNode[] = [];
  if (showList) {
    let previous: T | undefined;
    items.forEach((item, index) => {
      const newDay = !previous || dayKey(previous.time, timeZone) !== dayKey(item.time, timeZone);
      const unread = firstUnreadId != null && item.id === firstUnreadId;
      const grouped =
        !newDay &&
        !unread &&
        shouldGroupMessages(previous, item, {
          windowMs: groupWindowMs,
          ...(timeZone ? { timeZone } : {}),
        });
      const label = newDay
        ? dayLabel(item.time, { now, locale, timeZone }, { today: m.today, yesterday: m.yesterday })
        : '';
      rows.push(
        <Fragment key={item.id}>
          {newDay ? <MessageTimelineDaySeparator>{label}</MessageTimelineDaySeparator> : null}
          {unread ? (
            <MessageTimelineUnreadDivider>{m.newMessages}</MessageTimelineUnreadDivider>
          ) : null}
          <div
            className="mn-message-timeline__item"
            data-message-id={item.id}
            data-highlighted={highlightedId != null && highlightedId === item.id ? '' : undefined}
          >
            {renderItem(item, { grouped, index, previous })}
          </div>
        </Fragment>,
      );
      previous = item;
    });
  }

  return (
    <div ref={ref} {...rest} className={cx('mn-message-timeline', className)}>
      {/* The log scrolls; Tab reaches it so keyboard users can scroll with the arrow keys. */}
      <div
        ref={scroller}
        role="log"
        aria-live="polite"
        aria-relevant="additions"
        aria-label={m.label}
        aria-busy={loading || loadingOlder || settling || undefined}
        // A focusable scroll region (WCAG 2.1.1): keyboard users scroll the log with the arrow keys.
        // eslint-disable-next-line jsx-a11y/no-noninteractive-tabindex
        tabIndex={0}
        className={cx('mn-message-timeline__scroller', scrollerClassName)}
        onScroll={onScroll}
      >
        <div ref={content} className="mn-message-timeline__content">
          {loading ? (
            <div className="mn-message-timeline__skeleton" aria-label={m.loading} role="img">
              {[0.6, 0.85, 0.45, 0.7].map((w, i) => (
                <div key={i} className="mn-message-timeline__skeleton-row">
                  <Skeleton shape="circle" width="2rem" height="2rem" />
                  <div className="mn-message-timeline__skeleton-lines">
                    <Skeleton shape="text" width="8rem" />
                    <Skeleton shape="text" width={`${w * 100}%`} />
                  </div>
                </div>
              ))}
            </div>
          ) : error ? (
            <div className="mn-message-timeline__error">{error}</div>
          ) : items.length === 0 ? (
            <div className="mn-message-timeline__empty">{empty ?? header}</div>
          ) : (
            <>
              {hasOlder && onLoadOlder ? (
                <LoadMore
                  className="mn-message-timeline__older"
                  hasMore
                  loading={loadingOlder}
                  error={olderError}
                  onLoadMore={onLoadOlder}
                  {...(onRetryOlder ? { onRetry: onRetryOlder } : {})}
                  label={m.loadOlder}
                  loadingLabel={m.loadingOlder}
                  errorLabel={m.olderError}
                  endLabel={null}
                  showCount={false}
                />
              ) : header ? (
                <div className="mn-message-timeline__header">{header}</div>
              ) : null}
              {rows}
            </>
          )}
          {footer && !loading && !error ? (
            <div className="mn-message-timeline__footer">{footer}</div>
          ) : null}
        </div>
      </div>
      {jumpToLatest && showList && !atBottom ? (
        <button type="button" className="mn-message-timeline__jump" onClick={jump}>
          <ArrowDownGlyph />
          {unseen > 0 ? m.unseen(unseen) : m.jumpToLatest}
        </button>
      ) : null}
      <div role="status" aria-live="polite" className="mn-message-timeline__status">
        {status}
      </div>
    </div>
  );
}

/**
 * A scrolling conversation: MessageRows separated by day, a "New messages"
 * divider, older history loaded on scroll-up with the view held still, and
 * new messages followed while the reader is at the bottom. A polite
 * `role="log"` announces messages as they arrive.
 */
export const MessageTimeline = forwardRef(MessageTimelineInner) as <T extends MessageTimelineItem>(
  props: MessageTimelineProps<T> & { ref?: Ref<HTMLDivElement> },
) => ReactElement | null;
