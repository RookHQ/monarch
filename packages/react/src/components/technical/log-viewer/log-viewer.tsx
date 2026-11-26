'use client';

import {
  forwardRef,
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
  type HTMLAttributes,
  type KeyboardEvent,
  type ReactNode,
} from 'react';
import { useIsomorphicLayoutEffect } from '../../../internal/layout-effect.js';
import { cx } from '../../../internal/cx.js';
import { numberFormat, useClientLocale, useHydrated } from '../../../internal/dates/client-time.js';
import { ArrowDownGlyph } from '../../../internal/icons.js';
import { Button } from '../../actions/button/index.js';
import { SearchInput } from '../../search/search-input/index.js';

// useIsomorphicLayoutEffect warns during server rendering; scrolling only matters in the browser.
const useIsoLayoutEffect = typeof window === 'undefined' ? useEffect : useIsomorphicLayoutEffect;

/** Severity of a log line. */
export type LogLevel = 'debug' | 'info' | 'warn' | 'error';

/** The levels, lowest first. */
export const LOG_LEVELS: readonly LogLevel[] = ['debug', 'info', 'warn', 'error'];

/** One line of the log. */
export interface LogLine {
  /** Stable, unique id: the React key and the measurement key. */
  id: string;
  /** The text of the line. Rendered as text, never as HTML. */
  message: string;
  /** Severity. Lines without one count as `info`. */
  level?: LogLevel | undefined;
  /** When it was written: a Date, epoch milliseconds or an ISO 8601 string. */
  time?: Date | number | string | undefined;
  /** Where it came from (service, pod, step), shown before the message. */
  source?: string | undefined;
}

/** Every visible or announced string, with English defaults. */
export interface LogViewerMessages {
  /** Accessible name of the scrolling log. */
  label: string;
  /** Accessible name of the level filter group. */
  levels: string;
  /** Short level names shown on lines and filter buttons. */
  levelNames: Record<LogLevel, string>;
  search: string;
  searchPlaceholder: string;
  follow: string;
  wrap: string;
  /** Visible line count, e.g. "120 of 5,000 lines". */
  count: (shown: number, total: number) => string;
  /** Jump button while following is paused. */
  jumpToLatest: string;
  /** Jump button text when lines arrived while paused. */
  newLines: (count: number) => string;
  /** Announced when following stops or starts. */
  followPaused: string;
  followResumed: string;
  /** Shown when there are no lines at all. */
  empty: string;
  /** Shown when filters hide every line. */
  noMatches: string;
  loading: string;
}

export const LOG_VIEWER_MESSAGES: LogViewerMessages = {
  label: 'Log',
  levels: 'Levels',
  levelNames: { debug: 'Debug', info: 'Info', warn: 'Warn', error: 'Error' },
  search: 'Search log',
  searchPlaceholder: 'Search',
  follow: 'Follow',
  wrap: 'Wrap lines',
  count: (shown, total) =>
    shown === total
      ? `${total.toLocaleString()} ${total === 1 ? 'line' : 'lines'}`
      : `${shown.toLocaleString()} of ${total.toLocaleString()} lines`,
  jumpToLatest: 'Jump to latest',
  newLines: (n) => (n === 1 ? '1 new line' : `${n.toLocaleString()} new lines`),
  followPaused: 'Following paused',
  followResumed: 'Following new lines',
  empty: 'No log lines yet.',
  noMatches: 'No lines match the filters.',
  loading: 'Loading log',
};

const pad = (n: number, w = 2) => String(n).padStart(w, '0');

/** Default timestamp: local `HH:MM:SS.mmm`. */
export function formatLogTime(date: Date): string {
  return `${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}.${pad(date.getMilliseconds(), 3)}`;
}

/** The same shape in UTC, for the server and the hydrating render, which must not read the machine's zone. */
function formatLogTimeUtc(date: Date): string {
  return `${pad(date.getUTCHours())}:${pad(date.getUTCMinutes())}:${pad(date.getUTCSeconds())}.${pad(date.getUTCMilliseconds(), 3)}`;
}

function toDate(time: LogLine['time']): Date | null {
  if (time === undefined) return null;
  const d = time instanceof Date ? time : new Date(time);
  return Number.isNaN(d.getTime()) ? null : d;
}

/** Index of the last entry in sorted `offsets` that is ≤ `value`. */
function findIndex(offsets: readonly number[], value: number): number {
  let lo = 0;
  let hi = offsets.length - 1;
  while (lo < hi) {
    const mid = (lo + hi + 1) >> 1;
    if ((offsets[mid] ?? 0) <= value) lo = mid;
    else hi = mid - 1;
  }
  return Math.max(0, lo);
}

function Highlight({ text, query }: { text: string; query: string }) {
  if (!query) return <>{text}</>;
  const lower = text.toLowerCase();
  const parts: ReactNode[] = [];
  let from = 0;
  let at = lower.indexOf(query, from);
  while (at !== -1) {
    if (at > from) parts.push(text.slice(from, at));
    parts.push(
      <mark key={at} className="mn-log-viewer__mark">
        {text.slice(at, at + query.length)}
      </mark>,
    );
    from = at + query.length;
    at = lower.indexOf(query, from);
  }
  if (from < text.length) parts.push(text.slice(from));
  return <>{parts}</>;
}

export interface LogViewerProps extends Omit<HTMLAttributes<HTMLDivElement>, 'children'> {
  /** Oldest first. Append to the array to stream; only lines near the view are rendered. */
  lines: readonly LogLine[];
  /** Levels shown (controlled). */
  levels?: readonly LogLevel[] | undefined;
  /** Levels shown at first when uncontrolled. Default all four. */
  defaultLevels?: readonly LogLevel[] | undefined;
  /** Called when a level filter button is toggled. */
  onLevelsChange?: ((levels: LogLevel[]) => void) | undefined;
  /** Search text (controlled). */
  query?: string | undefined;
  /** Initial search text when uncontrolled. */
  defaultQuery?: string | undefined;
  /** Called on every keystroke in the search field. */
  onQueryChange?: ((query: string) => void) | undefined;
  /** `filter` hides lines without a match; `highlight` keeps every line and marks matches. Default `filter`. */
  searchMode?: 'filter' | 'highlight' | undefined;
  /** Follow (tail) mode, controlled: keeps the newest line in view. */
  follow?: boolean | undefined;
  /** Initial follow mode when uncontrolled. Default true. */
  defaultFollow?: boolean | undefined;
  /** Called when following starts or stops, including when the reader scrolls up. */
  onFollowChange?: ((follow: boolean) => void) | undefined;
  /** Wrap long lines (controlled). */
  wrap?: boolean | undefined;
  /** Initial wrap when uncontrolled. Default false. */
  defaultWrap?: boolean | undefined;
  /** Called when the wrap toggle changes. */
  onWrapChange?: ((wrap: boolean) => void) | undefined;
  /** Show the time column. Default true. */
  showTimestamps?: boolean | undefined;
  /** Show 1-based line numbers (position in `lines`). Default false. */
  showLineNumbers?: boolean | undefined;
  /**
   * Formats a line's time. Default local `HH:MM:SS.mmm`; on the server and
   * while hydrating the default writes UTC (so both agree), then switches to
   * the reader's zone. A formatter given here is used as is: make it
   * zone-independent if the log is server-rendered.
   */
  formatTime?: ((date: Date) => string) | undefined;
  /** Estimated row height in pixels before rows are measured. Default 22. */
  estimatedRowHeight?: number | undefined;
  /** Rows rendered above and below the view. Default 12. */
  overscan?: number | undefined;
  /** Pixels from the bottom that still count as "at the bottom". Default 24. */
  atBottomThreshold?: number | undefined;
  /** First load: shows a loading line and sets `aria-busy`. */
  loading?: boolean | undefined;
  /** Shown when `lines` is empty. Default `messages.empty`. */
  empty?: ReactNode | undefined;
  /** Extra controls at the end of the toolbar, e.g. download or clear. */
  actions?: ReactNode | undefined;
  /** Replaces any of the default strings. `levelNames` can be given in part. */
  messages?:
    | (Partial<Omit<LogViewerMessages, 'levelNames'>> & {
        levelNames?: Partial<Record<LogLevel, string>> | undefined;
      })
    | undefined;
}

function useControllable<T>(
  value: T | undefined,
  initial: T,
  onChange: ((next: T) => void) | undefined,
): [T, (next: T) => void] {
  const [inner, setInner] = useState(initial);
  const current = value !== undefined ? value : inner;
  const set = useCallback(
    (next: T) => {
      if (value === undefined) setInner(next);
      onChange?.(next);
    },
    [value, onChange],
  );
  return [current, set];
}

/**
 * A virtualized, streaming-friendly log: level filters, search with
 * highlighting, follow (tail) mode that pauses when the reader scrolls up and
 * a wrap toggle. Only the lines near the view are in the DOM, so tens of
 * thousands of lines stay smooth.
 */
export const LogViewer = forwardRef<HTMLDivElement, LogViewerProps>(function LogViewer(
  {
    lines,
    levels: levelsProp,
    defaultLevels = LOG_LEVELS,
    onLevelsChange,
    query: queryProp,
    defaultQuery = '',
    onQueryChange,
    searchMode = 'filter',
    follow: followProp,
    defaultFollow = true,
    onFollowChange,
    wrap: wrapProp,
    defaultWrap = false,
    onWrapChange,
    showTimestamps = true,
    showLineNumbers = false,
    formatTime: formatTimeProp,
    estimatedRowHeight = 22,
    overscan = 12,
    atBottomThreshold = 24,
    loading = false,
    empty,
    actions,
    messages,
    className,
    ...rest
  },
  ref,
) {
  const m = {
    ...LOG_VIEWER_MESSAGES,
    ...messages,
    levelNames: { ...LOG_VIEWER_MESSAGES.levelNames, ...messages?.levelNames },
  };
  const [levels, setLevels] = useControllable<readonly LogLevel[]>(
    levelsProp,
    defaultLevels,
    onLevelsChange as ((next: readonly LogLevel[]) => void) | undefined,
  );
  const [query, setQuery] = useControllable(queryProp, defaultQuery, onQueryChange);
  const [follow, setFollowState] = useControllable(followProp, defaultFollow, onFollowChange);
  const [wrap, setWrap] = useControllable(wrapProp, defaultWrap, onWrapChange);
  const [announcement, setAnnouncement] = useState('');
  const hydrated = useHydrated();
  const formatTime = formatTimeProp ?? (hydrated ? formatLogTime : formatLogTimeUtc);
  const countFormat = numberFormat(useClientLocale());

  const viewportRef = useRef<HTMLDivElement>(null);
  const [scrollTop, setScrollTop] = useState(0);
  const [viewport, setViewport] = useState(0);
  const heights = useRef(new Map<string, number>());
  const [measureVersion, setMeasureVersion] = useState(0);
  const observer = useRef<ResizeObserver | null>(null);

  const needle = query.trim().toLowerCase();
  const levelSet = useMemo(() => new Set(levels), [levels]);

  const counts = useMemo(() => {
    const c: Record<LogLevel, number> = { debug: 0, info: 0, warn: 0, error: 0 };
    for (const line of lines) c[line.level ?? 'info'] += 1;
    return c;
  }, [lines]);

  const visible = useMemo(() => {
    const out: { line: LogLine; index: number }[] = [];
    for (const [i, line] of lines.entries()) {
      if (!levelSet.has(line.level ?? 'info')) continue;
      if (
        needle &&
        searchMode === 'filter' &&
        !line.message.toLowerCase().includes(needle) &&
        !(line.source ?? '').toLowerCase().includes(needle)
      )
        continue;
      out.push({ line, index: i });
    }
    return out;
  }, [lines, levelSet, needle, searchMode]);

  // Prefix offsets; measured heights where known, the estimate elsewhere.
  const offsets = useMemo(() => {
    const out = new Array<number>(visible.length + 1);
    out[0] = 0;
    let sum = 0;
    for (const [i, { line }] of visible.entries()) {
      sum += heights.current.get(line.id) ?? estimatedRowHeight;
      out[i + 1] = sum;
    }
    return out;
    // measureVersion invalidates after new measurements.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [visible, estimatedRowHeight, measureVersion, wrap]);
  const totalHeight = offsets[visible.length] ?? 0;

  const viewHeight = viewport || estimatedRowHeight * 24;
  const start = Math.max(0, findIndex(offsets, scrollTop) - overscan);
  const end = Math.min(visible.length, findIndex(offsets, scrollTop + viewHeight) + 1 + overscan);

  // Measure rendered rows.
  useEffect(() => {
    if (typeof ResizeObserver === 'undefined') return;
    const ro = new ResizeObserver((entries, self) => {
      let changed = false;
      for (const entry of entries) {
        const el = entry.target as HTMLElement;
        // A row that scrolled out of the window is reported once more (size 0):
        // stop watching it, or a long stream keeps every row it ever showed.
        if (!el.isConnected) {
          self.unobserve(el);
          continue;
        }
        const id = el.dataset.lineId;
        if (!id) continue;
        const h = el.offsetHeight;
        if (h > 0 && heights.current.get(id) !== h) {
          heights.current.set(id, h);
          changed = true;
        }
      }
      if (changed) setMeasureVersion((v) => v + 1);
    });
    observer.current = ro;
    // Rows of the first render attached their refs before this effect ran, so
    // `measureRef` could not observe them: pick them up now.
    viewportRef.current
      ?.querySelectorAll<HTMLElement>('[data-line-id]')
      .forEach((el) => ro.observe(el));
    return () => {
      ro.disconnect();
      observer.current = null;
    };
  }, []);

  // Wrapping changes every height.
  useEffect(() => {
    heights.current.clear();
    setMeasureVersion((v) => v + 1);
  }, [wrap]);

  const measureRef = useCallback((el: HTMLDivElement | null) => {
    if (el) observer.current?.observe(el);
  }, []);

  useIsoLayoutEffect(() => {
    const el = viewportRef.current;
    if (!el) return;
    const update = () => setViewport(el.clientHeight);
    update();
    if (typeof ResizeObserver === 'undefined') return;
    const ro = new ResizeObserver(update);
    ro.observe(el);
    return () => ro.disconnect();
  }, []);

  // Follow: keep the newest line in view.
  const scrollToBottom = useCallback(() => {
    const el = viewportRef.current;
    if (!el) return;
    el.scrollTop = el.scrollHeight;
    setScrollTop(el.scrollTop);
  }, []);

  useIsoLayoutEffect(() => {
    if (follow) scrollToBottom();
  }, [follow, totalHeight, visible.length, scrollToBottom]);

  // Lines that arrived while paused.
  const pausedAt = useRef<number | null>(null);
  useEffect(() => {
    pausedAt.current = follow ? null : (pausedAt.current ?? lines.length);
  }, [follow, lines.length]);
  const unseen =
    !follow && pausedAt.current !== null ? Math.max(0, lines.length - pausedAt.current) : 0;

  const setFollow = (next: boolean) => {
    if (next === follow) return;
    setFollowState(next);
    setAnnouncement(next ? m.followResumed : m.followPaused);
  };

  const onScroll = () => {
    const el = viewportRef.current;
    if (!el) return;
    setScrollTop(el.scrollTop);
    const atBottom = el.scrollHeight - el.scrollTop - el.clientHeight <= atBottomThreshold;
    if (follow && !atBottom) setFollow(false);
    else if (!follow && atBottom && el.scrollHeight > el.clientHeight) setFollow(true);
  };

  const onKeyDown = (event: KeyboardEvent<HTMLDivElement>) => {
    const el = viewportRef.current;
    if (!el) return;
    if (event.key === 'End') {
      event.preventDefault();
      setFollow(true);
      scrollToBottom();
    } else if (event.key === 'Home') {
      event.preventDefault();
      setFollow(false);
      el.scrollTop = 0;
      setScrollTop(0);
    }
  };

  const toggleLevel = (level: LogLevel) => {
    const next = levelSet.has(level)
      ? levels.filter((l) => l !== level)
      : LOG_LEVELS.filter((l) => l === level || levelSet.has(l));
    setLevels(next);
  };

  const rows = visible.slice(start, end);
  const filtered = visible.length === 0 && lines.length > 0;

  return (
    <div
      ref={ref}
      {...rest}
      className={cx('mn-log-viewer', className)}
      data-wrap={wrap ? '' : undefined}
      data-follow={follow ? '' : undefined}
    >
      <div className="mn-log-viewer__toolbar">
        <SearchInput
          size="sm"
          className="mn-log-viewer__search"
          aria-label={m.search}
          placeholder={m.searchPlaceholder}
          value={query}
          onValueChange={setQuery}
        />
        <div className="mn-log-viewer__levels" role="group" aria-label={m.levels}>
          {LOG_LEVELS.map((level) => (
            <Button
              key={level}
              size="sm"
              variant="ghost"
              className="mn-log-viewer__level-filter"
              data-level={level}
              aria-pressed={levelSet.has(level)}
              onClick={() => toggleLevel(level)}
            >
              {m.levelNames[level]}
              <span className="mn-log-viewer__level-count">
                {countFormat.format(counts[level])}
              </span>
            </Button>
          ))}
        </div>
        <div className="mn-log-viewer__toggles">
          <Button
            size="sm"
            variant="ghost"
            className="mn-log-viewer__toggle"
            aria-pressed={follow}
            onClick={() => {
              const next = !follow;
              setFollow(next);
              if (next) scrollToBottom();
            }}
          >
            {m.follow}
          </Button>
          <Button
            size="sm"
            variant="ghost"
            className="mn-log-viewer__toggle"
            aria-pressed={wrap}
            onClick={() => setWrap(!wrap)}
          >
            {m.wrap}
          </Button>
          {actions}
        </div>
      </div>
      <div className="mn-log-viewer__body">
        {/* A focusable scroll region so keyboard users can scroll; Home and End also pause and resume following. */}
        {/* eslint-disable-next-line jsx-a11y/no-noninteractive-element-interactions */}
        <div
          ref={viewportRef}
          className="mn-log-viewer__viewport"
          role="region"
          aria-label={m.label}
          aria-busy={loading || undefined}
          // eslint-disable-next-line jsx-a11y/no-noninteractive-tabindex -- a scrollable region must be focusable.
          tabIndex={0}
          onScroll={onScroll}
          onKeyDown={onKeyDown}
        >
          {loading && lines.length === 0 ? (
            <p className="mn-log-viewer__placeholder">{m.loading}</p>
          ) : lines.length === 0 ? (
            <div className="mn-log-viewer__placeholder">{empty ?? m.empty}</div>
          ) : filtered ? (
            <p className="mn-log-viewer__placeholder">{m.noMatches}</p>
          ) : (
            <div
              role="list"
              className="mn-log-viewer__lines"
              style={{ blockSize: totalHeight, position: 'relative' }}
            >
              {rows.map(({ line, index }, i) => {
                const level = line.level ?? 'info';
                const date = showTimestamps ? toDate(line.time) : null;
                return (
                  <div
                    key={line.id}
                    ref={measureRef}
                    role="listitem"
                    aria-setsize={visible.length}
                    aria-posinset={start + i + 1}
                    data-line-id={line.id}
                    data-level={level}
                    className="mn-log-viewer__line"
                    style={{ insetBlockStart: offsets[start + i] }}
                  >
                    {showLineNumbers ? (
                      <span className="mn-log-viewer__number" aria-hidden="true">
                        {index + 1}
                      </span>
                    ) : null}
                    {date ? (
                      <time className="mn-log-viewer__time" dateTime={date.toISOString()}>
                        {formatTime(date)}
                      </time>
                    ) : null}
                    <span className="mn-log-viewer__level">{m.levelNames[level]}</span>
                    {line.source ? (
                      <span className="mn-log-viewer__source">
                        <Highlight text={line.source} query={needle} />
                      </span>
                    ) : null}
                    <span className="mn-log-viewer__message">
                      <Highlight text={line.message} query={needle} />
                    </span>
                  </div>
                );
              })}
            </div>
          )}
        </div>
        {!follow && lines.length > 0 ? (
          <Button
            size="sm"
            variant="secondary"
            className="mn-log-viewer__jump"
            leadingIcon={<ArrowDownGlyph />}
            onClick={() => {
              setFollow(true);
              scrollToBottom();
            }}
          >
            {unseen > 0 ? m.newLines(unseen) : m.jumpToLatest}
          </Button>
        ) : null}
      </div>
      <div className="mn-log-viewer__footer">
        <span className="mn-log-viewer__count">{m.count(visible.length, lines.length)}</span>
        <span className="mn-visually-hidden" role="status" aria-live="polite">
          {announcement}
        </span>
      </div>
    </div>
  );
});
