'use client';

import {
  forwardRef,
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
  type ForwardedRef,
  type HTMLAttributes,
  type KeyboardEvent as ReactKeyboardEvent,
  type ReactNode,
} from 'react';
import { useIsomorphicLayoutEffect } from '../../../internal/layout-effect.js';
import { cx } from '../../../internal/cx.js';
import { ArrowDownGlyph, ClockGlyph, WarningGlyph } from '../../../internal/icons.js';
import { dayLabel, type DayInput } from '../../../internal/day-groups.js';
import { Button } from '../../actions/button/index.js';
import {
  isStreaming,
  messageText,
  type AiConversationError,
  type AiConversationStatus,
  type AiMessage,
} from '../ai-conversation/index.js';
import { useAiScrollAnchor } from '../ai-scroll-anchor/index.js';
import { AiMessageView, type AiMessageViewProps } from '../ai-message/index.js';
import { AiThinkingIndicator } from '../ai-thinking-indicator/index.js';
import { cachedDayKey } from './ai-thread.days.js';
import { spokenMessage, type SpeechState } from './ai-thread.speech.js';

/** Every visible and assistive-technology string. English defaults. */
export interface AiThreadLabels {
  /** Accessible name of the log. */
  label: string;
  jumpToLatest: string;
  /** Count shown on the jump button when messages arrived while scrolled up. */
  newMessages: (count: number) => string;
  loadEarlier: string;
  loadingEarlier: string;
  /** Thinking indicator label while the reply is `submitted`. */
  thinking: string;
  /** Announced when a reply finishes. */
  responseComplete: string;
  /** Announced when the person stops a reply. */
  responseStopped: string;
  /** Announced assertively for a conversation error that no message shows. */
  error: (message: string) => string;
  /** Spoken in place of a code block's contents. */
  codeBlock: string;
  /** Spoken in place of a table's contents. */
  table: string;
  today: string;
  yesterday: string;
  /** Notice while the device is offline and nothing is queued. */
  offline: string;
  /** Notice while offline with messages waiting, from how many. */
  offlineQueued: (count: number) => string;
  /** Notice once the connection is back and queued messages are being sent. */
  sendingQueued: (count: number) => string;
  /** Hint when a running reply has sent nothing for a while. */
  slow: string;
}

const DEFAULT_LABELS: AiThreadLabels = {
  label: 'Conversation',
  jumpToLatest: 'Jump to latest',
  newMessages: (count) => (count === 1 ? '1 new message' : `${count} new messages`),
  loadEarlier: 'Load earlier messages',
  loadingEarlier: 'Loading earlier messages…',
  thinking: 'Thinking…',
  responseComplete: 'Response complete',
  responseStopped: 'Response stopped',
  error: (message) => `Error: ${message}`,
  codeBlock: 'Code block',
  table: 'Table',
  today: 'Today',
  yesterday: 'Yesterday',
  offline: 'You are offline. Messages you send will wait until you are back online.',
  offlineQueued: (count) =>
    count === 1
      ? 'You are offline. 1 message will send when you are back online.'
      : `You are offline. ${count} messages will send when you are back online.`,
  sendingQueued: (count) =>
    count === 1
      ? 'Back online. Sending 1 queued message…'
      : `Back online. Sending ${count} queued messages…`,
  slow: 'This is taking longer than usual. Still working…',
};

export interface AiThreadRenderContext {
  /** Position in `messages`. */
  index: number;
  /** True for the newest message. */
  isLast: boolean;
  /**
   * Whether the message should announce its own streaming text. False while
   * the thread owns the announcer (`announce`, the default), so pass it to
   * `AiStreamingText` / `AiMessageView` as `announce`.
   */
  announce: boolean;
}

export interface AiThreadProps extends Omit<HTMLAttributes<HTMLDivElement>, 'children'> {
  /** The visible conversation, oldest first (`useAiConversation().messages`). */
  messages: readonly AiMessage[];
  /** Renders one message. Default: `AiMessageView` with `messageProps`. */
  renderMessage?: ((message: AiMessage, context: AiThreadRenderContext) => ReactNode) | undefined;
  /**
   * Props for the default `AiMessageView`, the same for every message or per
   * message (e.g. `editing` for one of them).
   */
  messageProps?:
    | Partial<Omit<AiMessageViewProps, 'message'>>
    | ((message: AiMessage) => Partial<Omit<AiMessageViewProps, 'message'>>)
    | undefined;
  /**
   * `useAiConversation().status`. `submitted` shows the thinking indicator;
   * `submitted` and `streaming` make Escape call `onStop`.
   */
  status?: AiConversationStatus | undefined;
  /** `useAiConversation().error`, announced assertively when no message shows it. */
  error?: AiConversationError | null | undefined;
  /** Called by Escape inside the thread while a reply is running. */
  onStop?: (() => void) | undefined;
  /**
   * `useAiConversation().offline`. Shows a notice under the messages, and
   * announces it, while the device is offline.
   */
  offline?: boolean | undefined;
  /** `useAiConversation().queued.length`: how many messages wait for the connection, named in the notice. */
  queuedCount?: number | undefined;
  /**
   * `useAiConversation().slow`. Shows "taking longer than usual" under the
   * thinking indicator while a running reply is silent.
   */
  slow?: boolean | undefined;
  /** Shows "Load earlier messages" at the top while `hasEarlier`. Prepending keeps the scroll position. */
  onLoadEarlier?: (() => void) | undefined;
  /** Whether older messages exist. */
  hasEarlier?: boolean | undefined;
  /** Shows the load button as busy. */
  loadingEarlier?: boolean | undefined;
  /** Shown when there are no messages, e.g. a welcome with suggested prompts. */
  empty?: ReactNode | undefined;
  /** Shows a day label before the first message of each calendar day. Default true. */
  daySeparators?: boolean | undefined;
  /** BCP 47 locale for day labels and timestamps. */
  locale?: string | undefined;
  /** IANA time zone for day grouping. Default: the viewer's. */
  timeZone?: string | undefined;
  /** Reference time for "Today" / "Yesterday" (tests and stories). */
  now?: DayInput | undefined;
  /**
   * The thread announces streamed text to screen readers at sentence
   * boundaries through one polite live region, then "Response complete".
   * Default true. Set false to let each message announce itself instead.
   */
  announce?: boolean | undefined;
  /** Milliseconds between polite announcements. Default 1000. */
  announceInterval?: number | undefined;
  /** Render only the messages near the viewport once there are more than this many. Default 200. */
  virtualizeAfter?: number | undefined;
  /** While windowing, the newest messages that always stay rendered. Default 20. */
  keepLast?: number | undefined;
  /** Height guess, in CSS pixels, for a message not yet measured. Default 120. */
  estimatedMessageHeight?: number | undefined;
  /** Strings, for localisation. */
  labels?: Partial<AiThreadLabels> | undefined;
}

type Row =
  | { kind: 'day'; key: string; date: Date }
  | { kind: 'message'; key: string; message: AiMessage; index: number };

const DAY_ESTIMATE = 40;
// Sentence or paragraph ends: punctuation followed by whitespace, or a newline.
const BOUNDARY = /[.!?…。！？](?=["'”’)\]]*\s)|\n/g;

function lastBoundary(text: string, from: number): number {
  BOUNDARY.lastIndex = from;
  let end = -1;
  for (let m = BOUNDARY.exec(text); m; m = BOUNDARY.exec(text)) end = m.index + m[0].length;
  return end;
}

function setRef<T>(ref: ForwardedRef<T>, value: T | null) {
  if (typeof ref === 'function') ref(value);
  else if (ref) ref.current = value;
}

/**
 * Speaks a streaming reply in sentence-sized pieces through one polite live
 * region, then "Response complete" (or "Response stopped").
 */
function useThreadAnnouncer(
  messages: readonly AiMessage[],
  status: AiConversationStatus | undefined,
  enabled: boolean,
  interval: number,
  labels: AiThreadLabels,
) {
  const [polite, setPolite] = useState('');
  const spoken = useRef<{ id: string; length: number; live: boolean } | null>(null);
  const speech = useRef<SpeechState | null>(null);
  const queue = useRef('');
  const lastFlush = useRef(0);
  const timer = useRef<ReturnType<typeof setTimeout>>(undefined);
  const labelsRef = useRef(labels);
  labelsRef.current = labels;

  const flush = useCallback(() => {
    clearTimeout(timer.current);
    timer.current = undefined;
    const text = queue.current.replace(/\s+/g, ' ').trim();
    queue.current = '';
    if (!text) return;
    lastFlush.current = Date.now();
    setPolite(text);
  }, []);

  const schedule = useCallback(() => {
    if (timer.current) return;
    const wait = Math.max(0, lastFlush.current + interval - Date.now());
    if (wait === 0) flush();
    else timer.current = setTimeout(flush, wait);
  }, [flush, interval]);

  useEffect(() => () => clearTimeout(timer.current), []);

  // While a reply is submitted the region says "Thinking…" once; anything
  // still queued from an earlier reply is dropped.
  useEffect(() => {
    if (!enabled || status !== 'submitted') return;
    clearTimeout(timer.current);
    timer.current = undefined;
    queue.current = '';
    setPolite(labelsRef.current.thinking);
  }, [enabled, status]);

  const last = messages[messages.length - 1];
  // What is read out: Markdown as speech, with code blocks and tables named.
  let text = '';
  if (enabled && last && last.role === 'assistant') {
    const next = spokenMessage(last, speech.current, {
      codeBlock: labels.codeBlock,
      table: labels.table,
    });
    speech.current = next.state;
    text = next.text;
  }
  const streaming = last ? isStreaming(last) : false;

  useEffect(() => {
    if (!enabled || !last || last.role !== 'assistant') return;
    const running = status === 'submitted' || status === 'streaming';
    let s = spoken.current;
    if (!s || s.id !== last.id) {
      // Only replies that arrive while we watch are read out; a message first
      // seen already finished (history, a branch switch) is not.
      const live = streaming || running;
      s = { id: last.id, length: live ? 0 : text.length, live };
      spoken.current = s;
      if (!live) return;
    }
    if (!s.live) return;
    if (streaming) {
      const end = lastBoundary(text, s.length);
      if (end > s.length) {
        queue.current += ' ' + text.slice(s.length, end);
        s.length = end;
        schedule();
      }
      return;
    }
    // Not finished yet: an empty reply waiting for its first token, or a tool
    // call waiting for approval or for its result. "Response complete" follows
    // the reply, not the end of one text part.
    if (running || status === 'awaiting-approval' || status === 'awaiting-tool-result') return;
    s.live = false;
    queue.current += ' ' + text.slice(s.length);
    s.length = text.length;
    if (status !== 'error') {
      queue.current +=
        ' ' +
        (status === 'stopped'
          ? labelsRef.current.responseStopped
          : labelsRef.current.responseComplete);
    }
    flush();
  }, [enabled, last, text, streaming, status, schedule, flush]);

  return polite;
}

/**
 * The scrolling list of a conversation: messages oldest first with day
 * labels, pinned to the newest while the person is at the bottom, a "Jump to
 * latest" button when they are not, "Load earlier" at the top, a thinking
 * indicator while a reply is submitted, and one screen-reader announcer for
 * the whole conversation.
 */
export const AiThread = forwardRef<HTMLDivElement, AiThreadProps>(function AiThread(
  {
    messages,
    renderMessage,
    messageProps,
    status,
    error,
    onStop,
    offline = false,
    queuedCount = 0,
    slow = false,
    onLoadEarlier,
    hasEarlier = false,
    loadingEarlier = false,
    empty,
    daySeparators = true,
    locale,
    timeZone,
    now,
    announce = true,
    announceInterval = 1000,
    virtualizeAfter = 200,
    keepLast = 20,
    estimatedMessageHeight = 120,
    labels: labelsProp,
    className,
    onKeyDown,
    'aria-label': ariaLabel,
    'aria-labelledby': ariaLabelledBy,
    ...rest
  },
  ref,
) {
  const l = { ...DEFAULT_LABELS, ...labelsProp };
  const last = messages[messages.length - 1];
  const lastText = last ? messageText(last) : '';
  const busy = status === 'submitted' || status === 'streaming';

  // One notice at a time, the most important first. Offline wins: a reply
  // that is slow because the connection is gone is not "still working".
  const notice: { kind: 'offline' | 'sending' | 'slow'; text: string } | null = offline
    ? { kind: 'offline', text: queuedCount > 0 ? l.offlineQueued(queuedCount) : l.offline }
    : queuedCount > 0
      ? { kind: 'sending', text: l.sendingQueued(queuedCount) }
      : slow && busy
        ? { kind: 'slow', text: l.slow }
        : null;

  const anchor = useAiScrollAnchor({
    watch: `${messages.length}:${last?.id ?? ''}:${last?.parts.length ?? 0}:${lastText.length}:${status ?? ''}:${notice?.kind ?? ''}`,
  });
  const { scrollRef, contentRef, atBottom, scrollToBottom, isPinned } = anchor;

  // ------------------------------------------------------------ prepending
  // Remember the scroll metrics after every commit; when older messages are
  // added at the top, shift scrollTop by the height they added.
  const prevMetrics = useRef<{ firstId: string | undefined; height: number; top: number } | null>(
    null,
  );
  useIsomorphicLayoutEffect(() => {
    const el = scrollRef.current;
    const firstId = messages[0]?.id;
    const prev = prevMetrics.current;
    if (el && prev && prev.firstId !== undefined && firstId !== prev.firstId && !isPinned()) {
      const stillThere = messages.some((m) => m.id === prev.firstId);
      if (stillThere) el.scrollTop = prev.top + (el.scrollHeight - prev.height);
    }
    if (el) prevMetrics.current = { firstId, height: el.scrollHeight, top: el.scrollTop };
  });
  useEffect(() => {
    const el = scrollRef.current;
    if (!el) return;
    const onScroll = () => {
      if (prevMetrics.current) {
        prevMetrics.current.top = el.scrollTop;
        prevMetrics.current.height = el.scrollHeight;
      }
    };
    el.addEventListener('scroll', onScroll, { passive: true });
    return () => el.removeEventListener('scroll', onScroll);
  }, [scrollRef]);

  // ------------------------------------------------------------ new since
  const lastSeen = useRef<string | undefined>(last?.id);
  if (atBottom) lastSeen.current = last?.id;
  let newCount = 0;
  if (!atBottom && lastSeen.current !== undefined) {
    const i = messages.findIndex((m) => m.id === lastSeen.current);
    if (i !== -1) newCount = messages.length - 1 - i;
  }

  // ------------------------------------------------------------ rows
  // Rows keep their objects while their message is unchanged, and day keys
  // are cached, so a streaming reply does not rebuild the whole list.
  const rowCache = useRef(new WeakMap<AiMessage, Row>());
  const dayCache = useRef(new Map<string, Row>());
  const rows = useMemo<Row[]>(() => {
    const out: Row[] = [];
    let prevDay: string | null = null;
    messages.forEach((message, index) => {
      if (daySeparators) {
        const key = cachedDayKey(message.createdAt, timeZone);
        if (key !== 'invalid' && key !== prevDay) {
          const rowKey = `day:${key}:${message.id}`;
          let day = dayCache.current.get(rowKey);
          if (!day) {
            day = { kind: 'day', key: rowKey, date: new Date(message.createdAt) };
            dayCache.current.set(rowKey, day);
          }
          out.push(day);
        }
        prevDay = key;
      }
      let row = rowCache.current.get(message);
      if (!row || row.kind !== 'message' || row.index !== index) {
        row = { kind: 'message', key: message.id, message, index };
        rowCache.current.set(message, row);
      }
      out.push(row);
    });
    return out;
  }, [messages, daySeparators, timeZone]);

  // ------------------------------------------------------------ windowing
  const virtualize = messages.length > virtualizeAfter;
  const heights = useRef(new Map<string, number>());
  const [measureVersion, setMeasureVersion] = useState(0);
  const [viewport, setViewport] = useState<{ top: number; height: number } | null>(null);

  useEffect(() => {
    const el = scrollRef.current;
    if (!virtualize || !el) return;
    let frame = 0;
    const read = () => {
      frame = 0;
      setViewport((v) =>
        v && v.top === el.scrollTop && v.height === el.clientHeight
          ? v
          : { top: el.scrollTop, height: el.clientHeight },
      );
    };
    read();
    const onScroll = () => {
      if (!frame) frame = requestAnimationFrame(read);
    };
    el.addEventListener('scroll', onScroll, { passive: true });
    return () => {
      el.removeEventListener('scroll', onScroll);
      if (frame) cancelAnimationFrame(frame);
    };
  }, [virtualize, scrollRef]);

  // Measuring reads layout, so it runs only while windowing, at most once a frame.
  const measureFrame = useRef(0);
  useEffect(() => () => cancelAnimationFrame(measureFrame.current), []);
  useIsomorphicLayoutEffect(() => {
    if (!virtualize || measureFrame.current) return;
    const measure = () => {
      measureFrame.current = 0;
      const content = contentRef.current;
      if (!content) return;
      let changed = false;
      content.querySelectorAll<HTMLElement>('[data-row]').forEach((el) => {
        const key = el.dataset.row;
        if (!key) return;
        const h = el.offsetHeight;
        if (h > 0 && heights.current.get(key) !== h) {
          heights.current.set(key, h);
          changed = true;
        }
      });
      if (changed) setMeasureVersion((v) => v + 1);
    };
    if (typeof requestAnimationFrame === 'function')
      measureFrame.current = requestAnimationFrame(measure);
    else measure();
  });

  const windowed = useMemo(() => {
    if (!virtualize) return null;
    const size = (r: Row) =>
      heights.current.get(r.key) ?? (r.kind === 'day' ? DAY_ESTIMATE : estimatedMessageHeight);
    const offsets: number[] = [0];
    let sum = 0;
    for (const r of rows) offsets.push((sum += size(r)));
    const at = (i: number) => offsets[i] ?? sum;
    // The tail: the last `keepLast` messages and the day label before them.
    const tailMessage = Math.max(0, messages.length - keepLast);
    let tail = rows.findIndex((r) => r.kind === 'message' && r.index >= tailMessage);
    if (tail > 0 && rows[tail - 1]?.kind === 'day') tail -= 1;
    if (tail < 0) tail = rows.length;
    let start = tail;
    let end = tail;
    if (viewport) {
      const overscan = Math.max(viewport.height, 400);
      const from = viewport.top - overscan;
      const to = viewport.top + viewport.height + overscan;
      start = 0;
      while (start < tail && at(start + 1) <= from) start++;
      end = start;
      while (end < tail && at(end) < to) end++;
    }
    return {
      start,
      end,
      tail,
      before: at(start),
      between: at(tail) - at(end),
    };
    // measureVersion re-reads the measured heights.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [
    virtualize,
    rows,
    viewport,
    keepLast,
    estimatedMessageHeight,
    messages.length,
    measureVersion,
  ]);

  // ------------------------------------------------------------ announcer
  const polite = useThreadAnnouncer(messages, status, announce, announceInterval, l);
  const [assertive, setAssertive] = useState('');
  const lastHasError = !!last?.parts.some((p) => p.type === 'error');
  useEffect(() => {
    if (!announce) return;
    if (error && !lastHasError) setAssertive(l.error(error.message));
    else setAssertive('');
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [announce, error, lastHasError]);

  // ------------------------------------------------------------ rendering
  const dayOptions = { locale, timeZone, ...(now !== undefined ? { now } : {}) };
  const renderRow = (row: Row) => {
    if (row.kind === 'day') {
      const text = dayLabel(row.date, dayOptions, { today: l.today, yesterday: l.yesterday });
      return (
        <div key={row.key} data-row={row.key} className="mn-ai-thread__day">
          <span className="mn-ai-thread__day-label">{text}</span>
        </div>
      );
    }
    const { message, index } = row;
    const context: AiThreadRenderContext = {
      index,
      isLast: index === messages.length - 1,
      announce: !announce,
    };
    const props = typeof messageProps === 'function' ? messageProps(message) : messageProps;
    return (
      <div key={row.key} data-row={row.key} className="mn-ai-thread__item">
        {renderMessage ? (
          renderMessage(message, context)
        ) : (
          <AiMessageView locale={locale} {...props} message={message} announce={!announce} />
        )}
      </div>
    );
  };

  let body: ReactNode;
  if (messages.length === 0) {
    body = empty ? <div className="mn-ai-thread__empty">{empty}</div> : null;
  } else if (windowed) {
    body = (
      <>
        <div aria-hidden className="mn-ai-thread__spacer" style={{ height: windowed.before }} />
        {rows.slice(windowed.start, windowed.end).map(renderRow)}
        <div aria-hidden className="mn-ai-thread__spacer" style={{ height: windowed.between }} />
        {rows.slice(windowed.tail).map(renderRow)}
      </>
    );
  } else {
    body = rows.map(renderRow);
  }

  const handleKeyDown = (e: ReactKeyboardEvent<HTMLDivElement>) => {
    onKeyDown?.(e);
    if (e.defaultPrevented) return;
    if (e.key === 'Escape' && busy && onStop) {
      e.preventDefault();
      onStop();
    }
  };

  const jumpRef = useRef<HTMLButtonElement>(null);
  const jump = () => {
    const hadFocus = typeof document !== 'undefined' && document.activeElement === jumpRef.current;
    scrollToBottom();
    // The button disappears once at the bottom: keep keyboard focus in the thread.
    if (hadFocus) scrollRef.current?.focus({ preventScroll: true });
  };

  return (
    // Escape is handled for the whole thread (log and messages) here.
    // eslint-disable-next-line jsx-a11y/no-static-element-interactions
    <div
      ref={(node) => setRef(ref, node)}
      {...rest}
      className={cx('mn-ai-thread', className)}
      data-status={status}
      data-at-bottom={atBottom || undefined}
      data-virtualized={virtualize || undefined}
      onKeyDown={handleKeyDown}
    >
      <div
        ref={scrollRef}
        role="log"
        // The thread announces through its own throttled region below; a
        // live log would read every streamed token.
        aria-live="off"
        aria-label={ariaLabelledBy ? undefined : (ariaLabel ?? l.label)}
        aria-labelledby={ariaLabelledBy}
        aria-busy={busy || undefined}
        // A scrollable region must be reachable by keyboard.
        // eslint-disable-next-line jsx-a11y/no-noninteractive-tabindex
        tabIndex={0}
        className="mn-ai-thread__scroller"
      >
        <div ref={contentRef} className="mn-ai-thread__content">
          {hasEarlier && onLoadEarlier ? (
            <div className="mn-ai-thread__earlier">
              <Button size="sm" variant="ghost" loading={loadingEarlier} onClick={onLoadEarlier}>
                {loadingEarlier ? l.loadingEarlier : l.loadEarlier}
              </Button>
            </div>
          ) : null}
          {body}
          {status === 'submitted' ? (
            <AiThinkingIndicator
              className="mn-ai-thread__thinking"
              label={l.thinking}
              aria-hidden={announce || undefined}
            />
          ) : null}
        </div>
      </div>
      {/* Outside the log, so it is announced once as a status and not as a message. */}
      <div role="status" className="mn-ai-thread__notice-region">
        {notice ? (
          <p className="mn-ai-thread__notice" data-kind={notice.kind}>
            {notice.kind === 'slow' ? (
              <ClockGlyph className="mn-ai-thread__notice-icon" aria-hidden />
            ) : (
              <WarningGlyph className="mn-ai-thread__notice-icon" aria-hidden />
            )}
            <span className="mn-ai-thread__notice-text">{notice.text}</span>
          </p>
        ) : null}
      </div>
      {!atBottom && messages.length > 0 ? (
        <Button
          ref={jumpRef}
          size="sm"
          className="mn-ai-thread__jump"
          leadingIcon={<ArrowDownGlyph />}
          aria-label={newCount > 0 ? `${l.jumpToLatest}, ${l.newMessages(newCount)}` : undefined}
          onClick={jump}
        >
          {l.jumpToLatest}
          {newCount > 0 ? (
            <span className="mn-ai-thread__jump-count">{l.newMessages(newCount)}</span>
          ) : null}
        </Button>
      ) : null}
      {announce ? (
        <>
          <div role="status" className="mn-visually-hidden mn-ai-thread__announcer">
            {polite}
          </div>
          <div role="alert" className="mn-visually-hidden mn-ai-thread__announcer">
            {assertive}
          </div>
        </>
      ) : null}
    </div>
  );
});
