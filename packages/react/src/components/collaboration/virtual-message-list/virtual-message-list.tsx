'use client';

import {
  forwardRef,
  useCallback,
  useEffect,
  useImperativeHandle,
  useLayoutEffect,
  useMemo,
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
import { LoadMore } from '../../data-grid/load-more/index.js';
import { Skeleton } from '../../feedback/skeleton/index.js';

// useLayoutEffect warns during server rendering; the scroll work only matters in the browser.
const useIsoLayoutEffect = typeof window === 'undefined' ? useEffect : useLayoutEffect;

/** The fields VirtualMessageList reads from each item. Your items can carry anything else. */
export interface VirtualMessageItem {
  /** Stable, unique id: the React key, the measurement key and the scroll anchor. */
  id: string;
  /** When the message was sent. A Date, epoch milliseconds or an ISO 8601 string. */
  time: DayInput;
  /** Author id, for grouping consecutive messages. Leave out to never group. */
  authorId?: string | null | undefined;
}

/** What `renderItem` receives next to the item. */
export interface VirtualMessageRenderContext<T> {
  /** Continues the previous message's group: same author, same day, within `groupWindowMs`. Render a compact row. */
  grouped: boolean;
  /** Index in `items`. */
  index: number;
  /** The item before, if any. */
  previous: T | undefined;
}

/** Imperative controls, through `handleRef`. */
export interface VirtualMessageListHandle {
  /** Scrolls to the newest message. */
  scrollToBottom: (behavior?: ScrollBehavior) => void;
  /** Scrolls a message into the middle of the view, rendering it first if needed. False when the id is not loaded. */
  scrollToItem: (id: string, behavior?: ScrollBehavior) => boolean;
}

/** Every string the list shows or announces. Same keys as MessageTimeline’s. */
export interface VirtualMessageListMessages {
  /** Name of the log. */
  label: string;
  newMessages: string;
  today: string;
  yesterday: string;
  jumpToLatest: string;
  /** Jump button text while new messages arrived below. */
  unseen: (count: number) => string;
  /** Announced when messages arrive (the log itself is not live, so rows mounting while scrolling stay quiet). */
  arrived: (count: number) => string;
  loadOlder: string;
  loadingOlder: string;
  olderError: string;
  loading: string;
}

export const VIRTUAL_MESSAGE_LIST_MESSAGES: VirtualMessageListMessages = {
  label: 'Messages',
  newMessages: 'New messages',
  today: 'Today',
  yesterday: 'Yesterday',
  jumpToLatest: 'Jump to latest',
  unseen: (n) => (n === 1 ? '1 new message' : `${n} new messages`),
  arrived: (n) => (n === 1 ? '1 new message' : `${n} new messages`),
  loadOlder: 'Load older messages',
  loadingOlder: 'Loading older messages…',
  olderError: 'Couldn’t load older messages.',
  loading: 'Loading messages',
};

const FIVE_MINUTES = 5 * 60_000;
const ms = (input: DayInput) => (input instanceof Date ? input : new Date(input)).getTime();

function grouped(
  previous: VirtualMessageItem | undefined,
  next: VirtualMessageItem,
  windowMs: number,
  timeZone: string | undefined,
): boolean {
  if (!previous || previous.authorId == null || next.authorId == null) return false;
  if (previous.authorId !== next.authorId || !sameDay(previous.time, next.time, timeZone)) {
    return false;
  }
  const gap = ms(next.time) - ms(previous.time);
  return gap >= 0 && gap < windowMs;
}

function prefersReducedMotion(): boolean {
  return (
    typeof window !== 'undefined' &&
    typeof window.matchMedia === 'function' &&
    window.matchMedia('(prefers-reduced-motion: reduce)').matches
  );
}

/** Index of the first entry in sorted `offsets` greater than `value`, minus one (clamped). */
function findIndex(offsets: readonly number[], value: number): number {
  let lo = 0;
  let hi = offsets.length - 1;
  while (lo < hi) {
    const mid = (lo + hi + 1) >> 1;
    if ((offsets[mid] ?? 0) <= value) lo = mid;
    else hi = mid - 1;
  }
  return lo;
}

export interface VirtualMessageListProps<T extends VirtualMessageItem> extends Omit<
  HTMLAttributes<HTMLDivElement>,
  'children'
> {
  /** Oldest first. */
  items: readonly T[];
  /** Renders one message, usually a MessageRow. Only rows near the view are rendered. */
  renderItem: (item: T, context: VirtualMessageRenderContext<T>) => ReactNode;
  /** Id of the first unread message: a "New messages" divider goes above it. */
  firstUnreadId?: string | null | undefined;
  /** There is older history to load. */
  hasOlder?: boolean | undefined;
  /** Older history is loading. */
  loadingOlder?: boolean | undefined;
  /** Loading older history failed; shows a retry. */
  olderError?: boolean | undefined;
  /** Loads the previous page, called near the top. Prepend the result to `items`; the view stays still. */
  onLoadOlder?: (() => void) | undefined;
  /** Retries a failed older page. Default: `onLoadOlder`. */
  onRetryOlder?: (() => void) | undefined;
  /** Pixels from the top that trigger `onLoadOlder`. Default 600. */
  loadOlderThreshold?: number | undefined;
  /** Follow new messages while the reader is at the bottom. Default true. */
  stickToBottom?: boolean | undefined;
  /** Pixels from the bottom that still count as "at the bottom". Default 80. */
  atBottomThreshold?: number | undefined;
  /** Called when the reader reaches or leaves the bottom, e.g. to mark the conversation read. */
  onAtBottomChange?: ((atBottom: boolean) => void) | undefined;
  /** Show a "Jump to latest" button when scrolled up. Default true. */
  jumpToLatest?: boolean | undefined;
  /** Above the first message once all history is loaded, e.g. the conversation intro. */
  header?: ReactNode | undefined;
  /** Below the last message, e.g. messages still sending or a read receipt. */
  footer?: ReactNode | undefined;
  /** Under the list, in a polite live region: a typing indicator. */
  status?: ReactNode | undefined;
  /** Shown when there are no messages. Default: `header`. */
  empty?: ReactNode | undefined;
  /** First load: shows skeleton rows. */
  loading?: boolean | undefined;
  /** First load failed: shown instead of the list, e.g. an InlineErrorRetry. */
  error?: ReactNode | undefined;
  /** Briefly highlights a message, e.g. after a deep link. */
  highlightedId?: string | null | undefined;
  /** Scrolls a message into view once it is loaded (deep links). */
  scrollToId?: string | null | undefined;
  /** Consecutive messages by one author within this many ms are grouped. Default 5 minutes. */
  groupWindowMs?: number | undefined;
  /** Height used for rows not yet measured. Default 64. */
  estimatedItemHeight?: number | undefined;
  /** Extra pixels rendered above and below the view. Default `{ top: 600, bottom: 200 }`. */
  overscan?: { top: number; bottom: number } | undefined;
  /** Rows rendered before the list knows its height (server rendering, first paint). Default 20, from the newest. */
  initialRenderCount?: number | undefined;
  /** "Now" for Today and Yesterday. */
  now?: DayInput | undefined;
  locale?: string | undefined;
  timeZone?: string | undefined;
  /** Imperative scroll controls. */
  handleRef?: Ref<VirtualMessageListHandle> | undefined;
  /** Override any string. */
  messages?: Partial<VirtualMessageListMessages> | undefined;
  /** Class on the scrolling element. */
  scrollerClassName?: string | undefined;
}

function VirtualMessageListInner<T extends VirtualMessageItem>(
  {
    items,
    renderItem,
    firstUnreadId,
    hasOlder = false,
    loadingOlder = false,
    olderError = false,
    onLoadOlder,
    onRetryOlder,
    loadOlderThreshold = 600,
    stickToBottom = true,
    atBottomThreshold = 80,
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
    estimatedItemHeight = 64,
    overscan = { top: 600, bottom: 200 },
    initialRenderCount = 20,
    now,
    locale,
    timeZone,
    handleRef,
    messages: messagesProp,
    scrollerClassName,
    className,
    ...rest
  }: VirtualMessageListProps<T>,
  ref: Ref<HTMLDivElement>,
) {
  const m = { ...VIRTUAL_MESSAGE_LIST_MESSAGES, ...messagesProp };
  const scroller = useRef<HTMLDivElement | null>(null);
  const rowsEl = useRef<HTMLDivElement | null>(null);
  const heights = useRef(new Map<string, number>());
  const [measureVersion, setMeasureVersion] = useState(0);
  const [view, setView] = useState({ top: 0, height: 0 });
  const [atBottom, setAtBottom] = useState(true);
  const atBottomRef = useRef(true);
  const [unseen, setUnseen] = useState(0);
  const [arrived, setArrived] = useState('');
  const [settling, setSettling] = useState(false);
  const anchor = useRef<{ id: string; offset: number } | null>(null);
  const pendingScroll = useRef<{ id: string; behavior: ScrollBehavior } | null>(null);
  const prev = useRef<{ first?: string | undefined; last?: string | undefined; count: number }>({
    count: 0,
  });
  const initialised = useRef(false);
  const requested = useRef(false);
  const onAtBottomRef = useRef(onAtBottomChange);
  onAtBottomRef.current = onAtBottomChange;

  // Offsets of each row inside the rows container, from measured or estimated heights.
  const offsets = useMemo(() => {
    const out = new Array<number>(items.length + 1);
    out[0] = 0;
    items.forEach((item, i) => {
      out[i + 1] = (out[i] ?? 0) + (heights.current.get(item.id) ?? estimatedItemHeight);
    });
    return out;
    // measureVersion: heights changed.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [items, estimatedItemHeight, measureVersion]);
  const total = offsets[items.length] ?? 0;

  // The window of rows to render.
  let start: number;
  let end: number;
  if (view.height <= 0) {
    start = Math.max(0, items.length - initialRenderCount);
    end = items.length - 1;
  } else {
    const rowsTop = rowsEl.current?.offsetTop ?? 0;
    const top = view.top - rowsTop - overscan.top;
    const bottom = view.top - rowsTop + view.height + overscan.bottom;
    start = Math.max(0, findIndex(offsets, Math.max(0, top)));
    end = Math.min(items.length - 1, findIndex(offsets, Math.max(0, bottom)));
  }
  // A pending deep link must be rendered so it can be measured and centred.
  const pendingIndex = pendingScroll.current
    ? items.findIndex((i) => i.id === pendingScroll.current?.id)
    : -1;
  if (pendingIndex >= 0) {
    start = Math.min(start, pendingIndex);
    end = Math.max(end, pendingIndex);
  }

  const readView = useCallback(() => {
    const el = scroller.current;
    if (!el) return;
    setView((v) =>
      v.top === el.scrollTop && v.height === el.clientHeight
        ? v
        : { top: el.scrollTop, height: el.clientHeight },
    );
  }, []);

  const scrollToBottom = useCallback((how: ScrollBehavior = 'auto') => {
    const el = scroller.current;
    if (!el) return;
    if (typeof el.scrollTo === 'function') el.scrollTo({ top: el.scrollHeight, behavior: how });
    else el.scrollTop = el.scrollHeight;
  }, []);

  const scrollToItem = useCallback(
    (id: string, how: ScrollBehavior = 'auto') => {
      const index = items.findIndex((i) => i.id === id);
      const el = scroller.current;
      if (index < 0 || !el) return false;
      const rowsTop = rowsEl.current?.offsetTop ?? 0;
      el.scrollTop = Math.max(0, rowsTop + (offsets[index] ?? 0) - el.clientHeight / 2);
      pendingScroll.current = { id, behavior: how };
      // Leaving the bottom on purpose: stop following it.
      const bottom = el.scrollHeight - el.scrollTop - el.clientHeight <= atBottomThreshold;
      if (bottom !== atBottomRef.current) {
        atBottomRef.current = bottom;
        setAtBottom(bottom);
        onAtBottomRef.current?.(bottom);
      }
      readView();
      setMeasureVersion((v) => v + 1);
      return true;
    },
    [items, offsets, readView, atBottomThreshold],
  );

  const behavior = (wanted: ScrollBehavior = 'smooth'): ScrollBehavior =>
    prefersReducedMotion() ? 'auto' : wanted;

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

  // Remember which message is at the top of the view, to keep it still when rows above change.
  const captureAnchor = useCallback(() => {
    const el = scroller.current;
    if (!el || !items.length) return;
    const rowsTop = rowsEl.current?.offsetTop ?? 0;
    const y = el.scrollTop - rowsTop;
    const i = findIndex(offsets, Math.max(0, y));
    const item = items[i];
    if (item) anchor.current = { id: item.id, offset: y - (offsets[i] ?? 0) };
  }, [items, offsets]);

  const maybeLoadOlder = useCallback(() => {
    const el = scroller.current;
    if (!el || !onLoadOlder || !hasOlder || loadingOlder || olderError || requested.current) return;
    const nearTop = el.scrollTop <= loadOlderThreshold;
    const unfilled = el.clientHeight > 0 && el.scrollHeight <= el.clientHeight;
    if ((nearTop && el.clientHeight > 0) || unfilled) {
      requested.current = true;
      onLoadOlder();
    }
  }, [onLoadOlder, hasOlder, loadingOlder, olderError, loadOlderThreshold]);

  useEffect(() => {
    if (!loadingOlder) requested.current = false;
  }, [loadingOlder, items.length, olderError]);

  // Measure rendered rows after every render, and whenever they resize (images, previews).
  const observer = useRef<ResizeObserver | null>(null);
  const measure = useCallback(() => {
    const host = rowsEl.current;
    if (!host) return;
    let changed = false;
    host.querySelectorAll<HTMLElement>('[data-message-id]').forEach((row) => {
      const id = row.dataset.messageId;
      const h = row.getBoundingClientRect().height;
      if (id && h > 0 && heights.current.get(id) !== h) {
        heights.current.set(id, h);
        changed = true;
      }
    });
    if (changed) setMeasureVersion((v) => v + 1);
  }, []);
  useEffect(() => {
    if (typeof ResizeObserver === 'undefined') return;
    observer.current = new ResizeObserver(() => measure());
    return () => observer.current?.disconnect();
  }, [measure]);
  const observeRow = useCallback((node: HTMLDivElement | null) => {
    if (node) observer.current?.observe(node);
  }, []);

  // Keep the reader's place before paint: follow the bottom, or hold the anchor message still.
  useIsoLayoutEffect(() => {
    measure();
    const el = scroller.current;
    if (!el) return;
    const first = items[0]?.id;
    const last = items[items.length - 1]?.id;
    const before = prev.current;

    if (!initialised.current) {
      if (items.length > 0) {
        initialised.current = true;
        if (!(scrollToId && scrollToItem(scrollToId))) scrollToBottom();
      }
    } else if (pendingScroll.current) {
      const wanted = pendingScroll.current.id;
      const target = Array.from(el.querySelectorAll<HTMLElement>('[data-message-id]')).find(
        (node) => node.dataset.messageId === wanted,
      );
      if (target) {
        target.scrollIntoView?.({ block: 'center', behavior: pendingScroll.current.behavior });
        pendingScroll.current = null;
      }
    } else if (stickToBottom && atBottomRef.current) {
      scrollToBottom();
    } else if (anchor.current) {
      const i = items.findIndex((item) => item.id === anchor.current?.id);
      if (i >= 0) {
        const rowsTop = rowsEl.current?.offsetTop ?? 0;
        const want = rowsTop + (offsets[i] ?? 0) + anchor.current.offset;
        if (Math.abs(el.scrollTop - want) > 1) el.scrollTop = want;
      }
    }

    if (initialised.current && before.first !== undefined && first !== before.first) {
      setSettling(true);
    }
    if (initialised.current && before.last !== undefined && last !== before.last) {
      const from = items.findIndex((item) => item.id === before.last);
      const added = from >= 0 ? items.length - 1 - from : 0;
      if (added > 0) {
        setArrived(m.arrived(added));
        if (!(stickToBottom && atBottomRef.current)) setUnseen((n) => n + added);
      }
    }
    prev.current = { first, last, count: items.length };
    readView();
  });

  // Clear the arrival announcement so the next one is read even if the text is the same.
  useEffect(() => {
    if (!arrived) return;
    const t = setTimeout(() => setArrived(''), 1500);
    return () => clearTimeout(t);
  }, [arrived]);

  useEffect(() => {
    if (!settling) return;
    const t = setTimeout(() => setSettling(false), 300);
    return () => clearTimeout(t);
  }, [settling]);

  useEffect(() => {
    maybeLoadOlder();
  }, [maybeLoadOlder, items.length]);

  useEffect(() => {
    if (scrollToId && initialised.current) scrollToItem(scrollToId, behavior('smooth'));
    // Only when the target changes.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [scrollToId]);

  // The view height changes with the window or the layout around the list.
  useEffect(() => {
    const el = scroller.current;
    if (!el || typeof ResizeObserver === 'undefined') return;
    const ro = new ResizeObserver(() => {
      readView();
      if (stickToBottom && atBottomRef.current) scrollToBottom();
    });
    ro.observe(el);
    return () => ro.disconnect();
  }, [readView, stickToBottom, scrollToBottom]);

  const onScroll = () => {
    captureAnchor();
    readView();
    updateAtBottom();
    maybeLoadOlder();
  };

  const jump = () => {
    scrollToBottom(behavior('smooth'));
    setUnseen(0);
    scroller.current?.focus({ preventScroll: true });
  };

  const showList = !loading && !error && items.length > 0;
  const rows: ReactNode[] = [];
  if (showList) {
    for (let index = start; index <= end; index++) {
      const item = items[index];
      if (!item) continue;
      const previous = items[index - 1];
      const newDay = !previous || dayKey(previous.time, timeZone) !== dayKey(item.time, timeZone);
      const unread = firstUnreadId != null && item.id === firstUnreadId;
      const isGrouped = !newDay && !unread && grouped(previous, item, groupWindowMs, timeZone);
      const label = newDay
        ? dayLabel(item.time, { now, locale, timeZone }, { today: m.today, yesterday: m.yesterday })
        : '';
      rows.push(
        <div
          key={item.id}
          ref={observeRow}
          className="mn-virtual-message-list__row"
          data-message-id={item.id}
          data-index={index}
          data-highlighted={highlightedId != null && highlightedId === item.id ? '' : undefined}
        >
          {newDay ? (
            <div
              role="separator"
              aria-label={label}
              className="mn-virtual-message-list__separator"
              data-kind="day"
            >
              <span className="mn-virtual-message-list__separator-text">{label}</span>
            </div>
          ) : null}
          {unread ? (
            <div
              role="separator"
              aria-label={m.newMessages}
              className="mn-virtual-message-list__separator"
              data-kind="unread"
            >
              <span className="mn-virtual-message-list__separator-text">{m.newMessages}</span>
            </div>
          ) : null}
          {renderItem(item, { grouped: isGrouped, index, previous })}
        </div>,
      );
    }
  }
  const padTop = showList ? (offsets[start] ?? 0) : 0;
  const padBottom = showList ? total - (offsets[end + 1] ?? total) : 0;

  return (
    <div ref={ref} {...rest} className={cx('mn-virtual-message-list', className)}>
      <div
        ref={scroller}
        role="log"
        // Rows mount and unmount while scrolling; a live log would read them out. New
        // messages are announced through the status below instead.
        aria-live="off"
        aria-label={m.label}
        aria-busy={loading || loadingOlder || settling || undefined}
        // A focusable scroll region (WCAG 2.1.1): keyboard users scroll the log with the arrow keys.
        // eslint-disable-next-line jsx-a11y/no-noninteractive-tabindex
        tabIndex={0}
        className={cx('mn-virtual-message-list__scroller', scrollerClassName)}
        onScroll={onScroll}
      >
        <div className="mn-virtual-message-list__content">
          {loading ? (
            <div className="mn-virtual-message-list__skeleton" aria-label={m.loading} role="img">
              {[0.6, 0.85, 0.45, 0.7].map((w, i) => (
                <div key={i} className="mn-virtual-message-list__skeleton-row">
                  <Skeleton shape="circle" width="2rem" height="2rem" />
                  <div className="mn-virtual-message-list__skeleton-lines">
                    <Skeleton shape="text" width="8rem" />
                    <Skeleton shape="text" width={`${w * 100}%`} />
                  </div>
                </div>
              ))}
            </div>
          ) : error ? (
            <div className="mn-virtual-message-list__error">{error}</div>
          ) : items.length === 0 ? (
            <div className="mn-virtual-message-list__empty">{empty ?? header}</div>
          ) : (
            <>
              {hasOlder && onLoadOlder ? (
                <LoadMore
                  className="mn-virtual-message-list__older"
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
                <div className="mn-virtual-message-list__header">{header}</div>
              ) : null}
              <div
                ref={rowsEl}
                className="mn-virtual-message-list__rows"
                style={{ paddingTop: padTop, paddingBottom: padBottom }}
                data-rendered={`${start}-${end}`}
              >
                {rows}
              </div>
            </>
          )}
          {footer && !loading && !error ? (
            <div className="mn-virtual-message-list__footer">{footer}</div>
          ) : null}
        </div>
      </div>
      {jumpToLatest && showList && !atBottom ? (
        <button type="button" className="mn-virtual-message-list__jump" onClick={jump}>
          <ArrowDownGlyph aria-hidden />
          {unseen > 0 ? m.unseen(unseen) : m.jumpToLatest}
        </button>
      ) : null}
      <div role="status" aria-live="polite" className="mn-virtual-message-list__status">
        {status}
        <span className="mn-visually-hidden">{arrived}</span>
      </div>
    </div>
  );
}

/**
 * MessageTimeline for long histories: only the messages near the view are
 * rendered, with measured variable heights. Day separators, a "New messages"
 * divider, older pages loaded on scroll-up with the view held still, new
 * messages followed at the bottom and a jump-to-latest button work the same
 * way, without any dependency.
 */
export const VirtualMessageList = forwardRef(VirtualMessageListInner) as <
  T extends VirtualMessageItem,
>(
  props: VirtualMessageListProps<T> & { ref?: Ref<HTMLDivElement> },
) => ReactElement | null;
