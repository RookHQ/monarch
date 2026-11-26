'use client';

import {
  forwardRef,
  memo,
  useCallback,
  useEffect,
  useId,
  useMemo,
  useRef,
  useState,
  type CSSProperties,
  type FocusEvent,
  type HTMLAttributes,
  type KeyboardEvent,
  type MouseEvent,
  type ReactNode,
  type UIEvent,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { ChevronRightGlyph, ErrorGlyph } from '../../../internal/icons.js';
import { useControllable } from '../../../internal/use-controllable.js';
import { Button } from '../../actions/button/index.js';
import { CopyButton } from '../../actions/copy-button/index.js';
import { SegmentedControl } from '../../forms/segmented-control/index.js';

import type {
  AiTracePartLink,
  AiTraceSpan,
  AiTraceSpanKind,
  AiTraceSpanStatus,
} from './ai-trace-view.types.js';

export type {
  AiTraceAttributeValue,
  AiTracePartLink,
  AiTraceSpan,
  AiTraceSpanKind,
  AiTraceSpanStatus,
} from './ai-trace-view.types.js';

/** Which rows are shown and in what order. */
export type AiTraceFilter = 'all' | 'errors' | 'slowest';

/** A sum of money in one currency, in millionths of the currency unit. */
export interface AiTraceCost {
  currency: string;
  micros: number;
}

/** Totals over a trace, from `aiTraceTotals`. */
export interface AiTraceTotals {
  /** Earliest `startMs`, or 0 for an empty trace. */
  startMs: number;
  /** Latest known time: the last `endMs` or `startMs`, or `nowMs` when a span is still running. */
  endMs: number;
  /** Wall-clock time from the first start to the last end. */
  durationMs: number;
  inputTokens: number;
  outputTokens: number;
  /** `inputTokens + outputTokens`. */
  totalTokens: number;
  /** Cost per currency, in the order the currencies first appear. Empty when no span has a cost. */
  costs: AiTraceCost[];
  /** Spans with status `error`. */
  errorCount: number;
  /** Spans with no `endMs`. */
  runningCount: number;
  spanCount: number;
}

export interface AiTraceTotalsOptions {
  /** The current time in milliseconds from the run start; the end of spans that are still running. */
  nowMs?: number | undefined;
  /** Currency of spans that do not name one. Default `USD`. */
  currency?: string | undefined;
}

/**
 * Totals over a trace: wall-clock duration, tokens, cost per currency and the
 * number of errors. Token and cost values are added across every span, so a
 * parent must carry only what it used itself.
 */
export function aiTraceTotals(
  spans: readonly AiTraceSpan[],
  options: AiTraceTotalsOptions = {},
): AiTraceTotals {
  const fallback = options.currency ?? 'USD';
  let start = Infinity;
  let end = -Infinity;
  let inputTokens = 0;
  let outputTokens = 0;
  let errorCount = 0;
  let runningCount = 0;
  const costs = new Map<string, number>();
  for (const span of spans) {
    if (Number.isFinite(span.startMs)) {
      start = Math.min(start, span.startMs);
      end = Math.max(end, span.startMs);
    }
    if (span.endMs === undefined) runningCount += 1;
    else if (Number.isFinite(span.endMs)) end = Math.max(end, span.endMs);
    inputTokens += span.inputTokens ?? 0;
    outputTokens += span.outputTokens ?? 0;
    if (span.status === 'error') errorCount += 1;
    if (span.costMicros !== undefined) {
      const currency = span.currency ?? fallback;
      costs.set(currency, (costs.get(currency) ?? 0) + span.costMicros);
    }
  }
  if (start === Infinity) start = 0;
  if (end < start) end = start;
  if (runningCount > 0 && options.nowMs !== undefined && Number.isFinite(options.nowMs)) {
    end = Math.max(end, options.nowMs);
  }
  return {
    startMs: start,
    endMs: end,
    durationMs: end - start,
    inputTokens,
    outputTokens,
    totalTokens: inputTokens + outputTokens,
    costs: [...costs].map(([currency, micros]) => ({ currency, micros })),
    errorCount,
    runningCount,
    spanCount: spans.length,
  };
}

/** The unit labels written after a duration. */
export interface AiTraceDurationUnits {
  ms: string;
  s: string;
  min: string;
}

export interface FormatAiTraceDurationOptions {
  /** BCP 47 locale for the digits. Default: the runtime locale. */
  locale?: string | undefined;
  /** Unit labels. Default `ms`, `s`, `min`. */
  units?: Partial<AiTraceDurationUnits> | undefined;
}

const UNITS: AiTraceDurationUnits = { ms: 'ms', s: 's', min: 'min' };

function durationFormatter(locale: string | undefined, units: AiTraceDurationUnits) {
  const whole = new Intl.NumberFormat(locale, { maximumFractionDigits: 0 });
  const one = new Intl.NumberFormat(locale, { maximumFractionDigits: 1 });
  const two = new Intl.NumberFormat(locale, { maximumFractionDigits: 2 });
  return (ms: number): string => {
    const v = Number.isFinite(ms) ? Math.max(0, ms) : 0;
    if (v < 999.5) {
      // Sub-millisecond spans keep one decimal so they do not all read "0 ms".
      return `${(v > 0 && v < 10 ? one : whole).format(v)} ${units.ms}`;
    }
    const seconds = v / 1000;
    if (seconds < 9.995) return `${two.format(seconds)} ${units.s}`;
    if (seconds < 59.95) return `${one.format(seconds)} ${units.s}`;
    const total = Math.round(seconds);
    const min = Math.floor(total / 60);
    const rest = total % 60;
    return rest
      ? `${whole.format(min)} ${units.min} ${whole.format(rest)} ${units.s}`
      : `${whole.format(min)} ${units.min}`;
  };
}

/**
 * A duration in milliseconds as readable text: `850 ms`, `1.42 s`, `12.5 s`,
 * `2 min 5 s`. Digits follow `locale`; the unit labels can be replaced.
 */
export function formatAiTraceDuration(
  ms: number,
  options: FormatAiTraceDurationOptions = {},
): string {
  return durationFormatter(options.locale, { ...UNITS, ...options.units })(ms);
}

function costFormatter(locale: string | undefined) {
  const cache = new Map<string, Intl.NumberFormat>();
  return (micros: number, currency: string): string => {
    const value = micros / 1_000_000;
    const small = value !== 0 && Math.abs(value) < 1;
    const key = `${currency}:${small ? 's' : 'l'}`;
    let format = cache.get(key);
    if (!format) {
      format = new Intl.NumberFormat(locale, {
        style: 'currency',
        currency,
        minimumFractionDigits: 2,
        maximumFractionDigits: small ? 4 : 2,
      });
      cache.set(key, format);
    }
    return format.format(value);
  };
}

/**
 * Money from micros with `Intl.NumberFormat`: 1 250 000 micros of `USD` is
 * `$1.25`. Amounts under one unit keep up to four decimals, since a single
 * model call often costs a fraction of a cent.
 */
export function formatAiTraceCost(micros: number, currency: string, locale?: string): string {
  return costFormatter(locale)(micros, currency);
}

/**
 * The spans expanded when the view first opens: every top-level span, every
 * failed span, and every ancestor of a failed span, so an error is never
 * hidden inside a collapsed parent.
 */
export function aiTraceDefaultExpandedIds(spans: readonly AiTraceSpan[]): string[] {
  const byId = new Map<string, AiTraceSpan>();
  for (const span of spans) byId.set(span.id, span);
  const out = new Set<string>();
  for (const span of spans) {
    if (span.parentId === undefined || !byId.has(span.parentId)) out.add(span.id);
    if (span.status !== 'error') continue;
    // The `seen` guard stops a parent loop in bad data from spinning forever.
    const seen = new Set<string>();
    let at: AiTraceSpan | undefined = span;
    while (at && !seen.has(at.id)) {
      seen.add(at.id);
      out.add(at.id);
      at = at.parentId === undefined ? undefined : byId.get(at.parentId);
    }
  }
  return [...out];
}

/** Every visible and assistive-technology string. English defaults. */
export interface AiTraceViewMessages {
  /** Accessible name of the view and of the tree grid. */
  label: string;
  /** Column headers. */
  columns: Record<'name' | 'kind' | 'duration' | 'tokens' | 'cost' | 'timeline', string>;
  kinds: Record<AiTraceSpanKind, string>;
  statuses: Record<AiTraceSpanStatus, string>;
  /** Accessible name of the filter. */
  filterLabel: string;
  filters: Record<AiTraceFilter, string>;
  /** Unit labels after durations. */
  units: AiTraceDurationUnits;
  /** Header total: wall-clock time of the run. */
  totalDuration: string;
  /** Header total: input plus output tokens. */
  totalTokens: string;
  /** Header total: cost. */
  totalCost: string;
  /** Header total: number of failed spans. */
  errors: string;
  /** Shown for a value that is not known. */
  notAvailable: string;
  /** Shown when there are no spans. */
  empty: string;
  /** Shown when "Errors only" finds nothing. */
  noErrors: string;
  /** An offset from the run start, given the formatted duration, e.g. "+1.2 s". */
  offset: (duration: string) => string;
  /** Text alternative of a finished span's bar. */
  barText: (start: string, duration: string) => string;
  /** Text alternative of the bar of a span that is still running. */
  barRunningText: (start: string) => string;
  /** Accessible name of the details region, given the span name. */
  details: (name: string) => string;
  /** Polite announcement when a span's details open. */
  detailsShown: (name: string) => string;
  closeDetails: string;
  /** Button in the details that shows the span's conversation part. Shown with `onOpenPart`. */
  openPart: string;
  /** Detail labels. */
  status: string;
  start: string;
  end: string;
  duration: string;
  inputTokens: string;
  outputTokens: string;
  cost: string;
  model: string;
  error: string;
  attributes: string;
  input: string;
  output: string;
  /** Copy button label, its copied and failed labels, and the announcements. */
  copy: string;
  copied: string;
  copyFailed: string;
  copiedMessage: string;
  copyFailedMessage: string;
}

export const AI_TRACE_VIEW_MESSAGES: AiTraceViewMessages = {
  label: 'Run trace',
  columns: {
    name: 'Span',
    kind: 'Kind',
    duration: 'Duration',
    tokens: 'Tokens',
    cost: 'Cost',
    timeline: 'Timeline',
  },
  kinds: {
    model: 'Model',
    tool: 'Tool',
    retrieval: 'Retrieval',
    agent: 'Agent',
    guardrail: 'Guardrail',
    other: 'Other',
  },
  statuses: { ok: 'OK', error: 'Error', cancelled: 'Cancelled', running: 'Running' },
  filterLabel: 'Show',
  filters: { all: 'All', errors: 'Errors only', slowest: 'Slowest first' },
  units: UNITS,
  totalDuration: 'Total time',
  totalTokens: 'Tokens',
  totalCost: 'Cost',
  errors: 'Errors',
  notAvailable: '–',
  empty: 'No spans recorded',
  noErrors: 'No errors in this run',
  offset: (duration) => `+${duration}`,
  barText: (start, duration) => `Starts at ${start}, takes ${duration}`,
  barRunningText: (start) => `Starts at ${start}, still running`,
  details: (name) => `Details: ${name}`,
  detailsShown: (name) => `Showing details for ${name}`,
  closeDetails: 'Close details',
  openPart: 'Show in conversation',
  status: 'Status',
  start: 'Start',
  end: 'End',
  duration: 'Duration',
  inputTokens: 'Input tokens',
  outputTokens: 'Output tokens',
  cost: 'Cost',
  model: 'Model',
  error: 'Error',
  attributes: 'Attributes',
  input: 'Input',
  output: 'Output',
  copy: 'Copy',
  copied: 'Copied',
  copyFailed: "Couldn't copy",
  copiedMessage: 'Copied to clipboard',
  copyFailedMessage: "Couldn't copy. Select the text and copy it manually.",
};

type NestedMessageKey = 'columns' | 'kinds' | 'statuses' | 'filters' | 'units';

/** Overrides for the English strings; the grouped records may be partial. */
export type AiTraceViewMessagesInput = Partial<Omit<AiTraceViewMessages, NestedMessageKey>> & {
  [K in NestedMessageKey]?: Partial<AiTraceViewMessages[K]> | undefined;
};

export interface AiTraceViewProps extends Omit<
  HTMLAttributes<HTMLDivElement>,
  'children' | 'onSelect'
> {
  /**
   * The spans of one run, in any order. Children are found through `parentId`
   * and shown in start order. Keep the array identity stable between renders
   * (state or `useMemo`) so large traces are not rebuilt.
   */
  spans: readonly AiTraceSpan[];
  /** Controlled ids of the expanded spans. */
  expandedIds?: readonly string[] | undefined;
  /**
   * Expanded spans when uncontrolled. Default `aiTraceDefaultExpandedIds(spans)`:
   * top-level spans, failed spans and their ancestors, read once on mount.
   */
  defaultExpandedIds?: readonly string[] | undefined;
  /** Called with the new list when a span is expanded or collapsed. */
  onExpandedChange?: ((ids: string[]) => void) | undefined;
  /** Controlled selection: the span whose details are open, or `null` for none. */
  selectedId?: string | null | undefined;
  /** Selected span when uncontrolled. Default none. */
  defaultSelectedId?: string | null | undefined;
  /** Called when a span is selected, or with `null` when the details are closed. */
  onSelect?: ((id: string | null) => void) | undefined;
  /** Controlled filter: every span, failed spans with their ancestors, or siblings sorted slowest first. */
  filter?: AiTraceFilter | undefined;
  /** Filter when uncontrolled. Default `all`. */
  defaultFilter?: AiTraceFilter | undefined;
  /** Called when the person changes the filter. */
  onFilterChange?: ((filter: AiTraceFilter) => void) | undefined;
  /**
   * The current time in milliseconds from the run start: where the bars of
   * running spans end. Without it they end at the latest known time, so server
   * rendering stays deterministic.
   */
  nowMs?: number | undefined;
  /**
   * Shows a "Show in conversation" button in the details of a span that has a
   * `partId` or `messageId`, and is called with them when it is pressed.
   */
  onOpenPart?: ((link: AiTracePartLink, span: AiTraceSpan) => void) | undefined;
  /**
   * Rows are windowed when more than this many are visible: only the rows in
   * view (and the focused row) are in the document, with `aria-rowcount` and
   * `aria-rowindex` giving their place. Default 200; `Infinity` renders every row.
   */
  windowThreshold?: number | undefined;
  /** ISO 4217 currency for spans that do not name one. Default `USD`. */
  currency?: string | undefined;
  /** BCP 47 locale for numbers, durations and money. Default: the runtime locale. */
  locale?: string | undefined;
  /**
   * Replaces any of the English strings. Pass a stable object (a constant or
   * `useMemo`): a new object on every render re-renders every row.
   */
  messages?: AiTraceViewMessagesInput | undefined;
}

interface Formatters {
  m: AiTraceViewMessages;
  duration: (ms: number) => string;
  number: (n: number) => string;
  cost: (micros: number, currency: string) => string;
  currency: string;
}

interface VisibleRow {
  span: AiTraceSpan;
  level: number;
  pos: number;
  size: number;
  hasChildren: boolean;
  expanded: boolean;
}

const ROOT = '\u0000root';

function spanTokens(span: AiTraceSpan): number | undefined {
  return span.inputTokens === undefined && span.outputTokens === undefined
    ? undefined
    : (span.inputTokens ?? 0) + (span.outputTokens ?? 0);
}

interface RowProps {
  row: VisibleRow;
  index: number;
  selected: boolean;
  tabbable: boolean;
  /** Bar position on the shared scale, in percent. */
  left: number;
  width: number;
  /** 1-based row number in the whole grid, header included; set only while rows are windowed. */
  rowIndex: number | undefined;
  f: Formatters;
}

/** Row height before the first row is measured, and the viewport height where layout is unknown. */
const ROW_HEIGHT = 33;
const VIEWPORT_HEIGHT = 576;
/** Rows kept rendered beyond each edge of the viewport. */
const OVERSCAN = 8;
const COLUMN_COUNT = 6;

/** One span row. Memoised: moving focus or selection re-renders two rows, not five hundred. */
const TraceRow = memo(function TraceRow({
  row,
  index,
  selected,
  tabbable,
  left,
  width,
  rowIndex,
  f,
}: RowProps) {
  const { span, level, hasChildren, expanded } = row;
  const { m } = f;
  const running = span.endMs === undefined;
  const duration = running ? m.statuses.running : f.duration((span.endMs as number) - span.startMs);
  const tokens = spanTokens(span);
  const start = m.offset(f.duration(span.startMs));
  return (
    <tr
      role="row"
      className="mn-ai-trace-view__row"
      aria-level={level}
      aria-posinset={row.pos}
      aria-setsize={row.size}
      aria-expanded={hasChildren ? expanded : undefined}
      aria-selected={selected}
      aria-rowindex={rowIndex}
      tabIndex={tabbable ? 0 : -1}
      data-row-index={index}
      data-part-id={span.partId}
      data-message-id={span.messageId}
      data-kind={span.kind}
      data-status={span.status}
      data-selected={selected || undefined}
    >
      <td
        role="gridcell"
        className="mn-ai-trace-view__cell mn-ai-trace-view__cell--name"
        style={{ '--_level': level - 1 } as CSSProperties}
      >
        <span className="mn-ai-trace-view__name-line">
          {hasChildren ? (
            <span className="mn-ai-trace-view__toggle" data-toggle="" aria-hidden>
              <ChevronRightGlyph className="mn-ai-trace-view__chevron" />
            </span>
          ) : (
            <span className="mn-ai-trace-view__toggle" aria-hidden />
          )}
          {span.status === 'error' ? (
            <ErrorGlyph className="mn-ai-trace-view__status-icon" />
          ) : null}
          <bdi className="mn-ai-trace-view__name">{span.name}</bdi>
          {span.status !== 'ok' ? (
            <span className="mn-ai-trace-view__status">{m.statuses[span.status]}</span>
          ) : null}
        </span>
      </td>
      <td role="gridcell" className="mn-ai-trace-view__cell">
        {m.kinds[span.kind]}
      </td>
      <td role="gridcell" className="mn-ai-trace-view__cell" data-numeric="">
        {duration}
      </td>
      <td role="gridcell" className="mn-ai-trace-view__cell" data-numeric="">
        {tokens === undefined ? m.notAvailable : f.number(tokens)}
      </td>
      <td role="gridcell" className="mn-ai-trace-view__cell" data-numeric="">
        {span.costMicros === undefined
          ? m.notAvailable
          : f.cost(span.costMicros, span.currency ?? f.currency)}
      </td>
      <td role="gridcell" className="mn-ai-trace-view__cell mn-ai-trace-view__cell--timeline">
        <span className="mn-ai-trace-view__track" aria-hidden>
          <span
            className="mn-ai-trace-view__bar"
            style={{ insetInlineStart: `${left}%`, inlineSize: `${width}%` }}
          />
        </span>
        <span className="mn-visually-hidden">
          {running ? m.barRunningText(start) : m.barText(start, duration)}
        </span>
      </td>
    </tr>
  );
});

function rowIndexOf(target: EventTarget): number {
  const row = (target as HTMLElement).closest?.('[data-row-index]');
  return row ? Number(row.getAttribute('data-row-index')) : -1;
}

/**
 * The technical trace of one agent run: every model call, tool call,
 * retrieval and sub-agent as a row in a tree grid, with duration, tokens,
 * cost and a bar on a shared time scale. Selecting a row opens its details:
 * status, timings, error, attributes and plain-text input and output. Data
 * comes in through props; the component never fetches.
 */
export const AiTraceView = forwardRef<HTMLDivElement, AiTraceViewProps>(function AiTraceView(
  {
    spans,
    expandedIds: expandedProp,
    defaultExpandedIds,
    onExpandedChange,
    selectedId: selectedProp,
    defaultSelectedId = null,
    onSelect,
    filter: filterProp,
    defaultFilter = 'all',
    onFilterChange,
    nowMs,
    onOpenPart,
    windowThreshold = 200,
    currency = 'USD',
    locale,
    messages: messagesProp,
    className,
    ...rest
  },
  ref,
) {
  const m = useMemo<AiTraceViewMessages>(() => {
    const d = AI_TRACE_VIEW_MESSAGES;
    const { columns, kinds, statuses, filters, units, ...flat } = messagesProp ?? {};
    const defined = Object.fromEntries(Object.entries(flat).filter(([, v]) => v !== undefined));
    return {
      ...d,
      ...defined,
      columns: { ...d.columns, ...columns },
      kinds: { ...d.kinds, ...kinds },
      statuses: { ...d.statuses, ...statuses },
      filters: { ...d.filters, ...filters },
      units: { ...d.units, ...units },
    };
  }, [messagesProp]);

  const f = useMemo<Formatters>(() => {
    const numbers = new Intl.NumberFormat(locale);
    return {
      m,
      duration: durationFormatter(locale, m.units),
      number: (n) => numbers.format(n),
      cost: costFormatter(locale),
      currency,
    };
  }, [m, locale, currency]);

  const [initialExpanded] = useState<readonly string[]>(
    () => defaultExpandedIds ?? aiTraceDefaultExpandedIds(spans),
  );
  const [expandedList, setExpandedList] = useControllable<readonly string[]>(
    expandedProp,
    initialExpanded,
    onExpandedChange as ((ids: readonly string[]) => void) | undefined,
  );
  const expanded = useMemo(() => new Set(expandedList), [expandedList]);
  const [selectedId, setSelectedId] = useControllable<string | null>(
    selectedProp,
    defaultSelectedId,
    onSelect,
  );
  const [filter, setFilter] = useControllable<AiTraceFilter>(
    filterProp,
    defaultFilter,
    onFilterChange,
  );
  const [activeId, setActiveId] = useState<string | null>(null);

  const totals = useMemo(() => aiTraceTotals(spans, { nowMs, currency }), [spans, nowMs, currency]);

  const tree = useMemo(() => {
    const byId = new Map<string, AiTraceSpan>();
    for (const span of spans) byId.set(span.id, span);
    // Spans whose parents loop back to them have no path from the top and
    // would never be shown: every member of such a loop is treated as top level.
    const cyclic = new Set<string>();
    const visited = new Set<string>();
    for (const span of byId.values()) {
      if (visited.has(span.id)) continue;
      const path: string[] = [];
      const placeOnPath = new Map<string, number>();
      let at: AiTraceSpan | undefined = span;
      while (at && !visited.has(at.id)) {
        visited.add(at.id);
        placeOnPath.set(at.id, path.length);
        path.push(at.id);
        at = at.parentId === undefined ? undefined : byId.get(at.parentId);
      }
      const from = at ? placeOnPath.get(at.id) : undefined;
      if (from !== undefined) for (const id of path.slice(from)) cyclic.add(id);
    }
    const children = new Map<string, AiTraceSpan[]>();
    for (const span of byId.values()) {
      const key =
        span.parentId !== undefined &&
        span.parentId !== span.id &&
        byId.has(span.parentId) &&
        !cyclic.has(span.id)
          ? span.parentId
          : ROOT;
      const list = children.get(key);
      if (list) list.push(span);
      else children.set(key, [span]);
    }
    const startOf = (span: AiTraceSpan) => (Number.isFinite(span.startMs) ? span.startMs : 0);
    for (const list of children.values()) list.sort((a, b) => startOf(a) - startOf(b));
    return { byId, children };
  }, [spans]);

  const rows = useMemo<VisibleRow[]>(() => {
    const { byId, children } = tree;
    let keep: Set<string> | undefined;
    if (filter === 'errors') {
      keep = new Set();
      for (const span of byId.values()) {
        if (span.status !== 'error') continue;
        let at: AiTraceSpan | undefined = span;
        while (at && !keep.has(at.id)) {
          keep.add(at.id);
          at = at.parentId === undefined ? undefined : byId.get(at.parentId);
        }
      }
    }
    const length = (s: AiTraceSpan) => (s.endMs ?? totals.endMs) - s.startMs;
    const kids = (id: string): AiTraceSpan[] => {
      let list = children.get(id) ?? [];
      if (keep) list = list.filter((s) => (keep as Set<string>).has(s.id));
      if (filter === 'slowest') list = [...list].sort((a, b) => length(b) - length(a));
      return list;
    };
    const out: VisibleRow[] = [];
    // Only the children of expanded parents are walked, so a collapsed
    // 500-span trace costs a handful of rows.
    const walk = (id: string, level: number) => {
      const list = kids(id);
      list.forEach((span, i) => {
        const hasChildren = kids(span.id).length > 0;
        const isOpen = hasChildren && expanded.has(span.id);
        out.push({
          span,
          level,
          pos: i + 1,
          size: list.length,
          hasChildren,
          expanded: isOpen,
        });
        if (isOpen) walk(span.id, level + 1);
      });
    };
    walk(ROOT, 1);
    return out;
  }, [tree, filter, expanded, totals.endMs]);

  const baseId = useId();
  const labelId = `${baseId}-label`;
  const bodyRef = useRef<HTMLTableSectionElement>(null);
  const scrollRef = useRef<HTMLDivElement>(null);
  /** A row to focus once it is in the document: windowing may not have rendered it yet. */
  const pendingFocus = useRef<number | null>(null);
  const [firstVisible, setFirstVisible] = useState(0);
  const [metrics, setMetrics] = useState({ rowHeight: ROW_HEIGHT, viewport: VIEWPORT_HEIGHT });
  const windowed = rows.length > windowThreshold;

  // Handlers read the latest values through a ref so they stay one stable
  // function on the table instead of a closure per row.
  const latest = useRef({ rows, expanded, expandedList });
  latest.current = { rows, expanded, expandedList };

  const focusRow = useCallback((index: number) => {
    const row = latest.current.rows[index];
    if (!row) return;
    setActiveId(row.span.id);
    const element = bodyRef.current?.querySelector<HTMLElement>(`[data-row-index="${index}"]`);
    if (element) element.focus();
    // Outside the window: the active row is always rendered, so focus it after this render.
    else pendingFocus.current = index;
  }, []);

  useEffect(() => {
    const index = pendingFocus.current;
    if (index === null) return;
    pendingFocus.current = null;
    bodyRef.current?.querySelector<HTMLElement>(`[data-row-index="${index}"]`)?.focus();
  });

  // Windowing needs the real row and viewport heights; until they are known
  // (the server, a hidden panel) the defaults keep a sensible number of rows.
  useEffect(() => {
    if (!windowed) return undefined;
    const scroller = scrollRef.current;
    if (!scroller) return undefined;
    const measure = () => {
      const row = bodyRef.current?.querySelector<HTMLElement>('[data-row-index]');
      const rowHeight = row ? row.getBoundingClientRect().height : 0;
      const viewport = scroller.clientHeight;
      setMetrics((prev) => {
        const next = {
          rowHeight: rowHeight > 0 ? rowHeight : prev.rowHeight,
          viewport: viewport > 0 ? viewport : prev.viewport,
        };
        return next.rowHeight === prev.rowHeight && next.viewport === prev.viewport ? prev : next;
      });
    };
    measure();
    if (typeof ResizeObserver === 'undefined') return undefined;
    const observer = new ResizeObserver(measure);
    observer.observe(scroller);
    return () => observer.disconnect();
  }, [windowed]);

  const handleScroll = (event: UIEvent<HTMLDivElement>) => {
    setFirstVisible(Math.max(0, Math.floor(event.currentTarget.scrollTop / metrics.rowHeight)));
  };

  const setOpen = useCallback(
    (id: string, open: boolean) => {
      const { expanded: set, expandedList: list } = latest.current;
      if (set.has(id) === open) return;
      setExpandedList(open ? [...list, id] : list.filter((x) => x !== id));
    },
    [setExpandedList],
  );

  const handleKeyDown = (event: KeyboardEvent<HTMLTableSectionElement>) => {
    if (event.altKey || event.ctrlKey || event.metaKey) return;
    const index = rowIndexOf(event.target);
    const current = rows[index];
    if (!current) return;
    const rtl =
      typeof getComputedStyle === 'function' &&
      getComputedStyle(event.currentTarget).direction === 'rtl';
    const forward = rtl ? 'ArrowLeft' : 'ArrowRight';
    const back = rtl ? 'ArrowRight' : 'ArrowLeft';
    let handled = true;
    switch (event.key) {
      case 'ArrowDown':
        focusRow(Math.min(rows.length - 1, index + 1));
        break;
      case 'ArrowUp':
        focusRow(Math.max(0, index - 1));
        break;
      case 'Home':
        focusRow(0);
        break;
      case 'End':
        focusRow(rows.length - 1);
        break;
      case forward:
        if (!current.hasChildren) break;
        if (current.expanded) focusRow(index + 1);
        else setOpen(current.span.id, true);
        break;
      case back:
        if (current.expanded) setOpen(current.span.id, false);
        else {
          // Move to the parent: the nearest earlier row one level up.
          for (let i = index - 1; i >= 0; i -= 1) {
            if ((rows[i] as VisibleRow).level < current.level) {
              focusRow(i);
              break;
            }
          }
        }
        break;
      case 'Enter':
      case ' ':
        setSelectedId(current.span.id);
        break;
      default:
        handled = false;
    }
    if (handled) event.preventDefault();
  };

  const handleClick = (event: MouseEvent<HTMLTableSectionElement>) => {
    const index = rowIndexOf(event.target);
    const current = rows[index];
    if (!current) return;
    setActiveId(current.span.id);
    if ((event.target as HTMLElement).closest('[data-toggle]')) {
      setOpen(current.span.id, !current.expanded);
      return;
    }
    setSelectedId(current.span.id);
  };

  const handleFocus = (event: FocusEvent<HTMLTableSectionElement>) => {
    const current = rows[rowIndexOf(event.target)];
    if (current) setActiveId(current.span.id);
  };

  // One Tab stop: the row last focused, else the selected row, else the first.
  let tabIndex = rows.findIndex((r) => r.span.id === activeId);
  if (tabIndex < 0) tabIndex = rows.findIndex((r) => r.span.id === selectedId);
  const hasTabRow = tabIndex >= 0;
  if (!hasTabRow) tabIndex = 0;

  const scale = Math.max(1, totals.durationMs);
  const selected = selectedId === null ? undefined : tree.byId.get(selectedId);
  const selectedLink: AiTracePartLink | undefined =
    selected && onOpenPart && (selected.partId !== undefined || selected.messageId !== undefined)
      ? { partId: selected.partId, messageId: selected.messageId }
      : undefined;

  const renderRow = (index: number): ReactNode => {
    const row = rows[index];
    if (!row) return null;
    const { span } = row;
    const startMs = Number.isFinite(span.startMs) ? span.startMs : totals.startMs;
    const end = Math.max(startMs, span.endMs ?? totals.endMs);
    const left = Math.min(100, Math.max(0, ((startMs - totals.startMs) / scale) * 100));
    const width = Math.max(0, Math.min(100 - left, ((end - startMs) / scale) * 100));
    return (
      <TraceRow
        key={span.id}
        row={row}
        index={index}
        selected={span.id === selectedId}
        tabbable={index === tabIndex}
        left={left}
        width={Number.isFinite(width) ? width : 0}
        rowIndex={windowed ? index + 2 : undefined}
        f={f}
      />
    );
  };

  let body: ReactNode[];
  if (!windowed) {
    body = rows.map((_, index) => renderRow(index));
  } else {
    const inView = Math.ceil(metrics.viewport / metrics.rowHeight);
    const top = Math.max(0, Math.min(firstVisible, rows.length - inView));
    // With no focused or selected row the Tab stop is the first row in view, not row one.
    if (!hasTabRow) tabIndex = top;
    const from = Math.max(0, top - OVERSCAN);
    const to = Math.min(rows.length, top + inView + OVERSCAN);
    const indices: number[] = [];
    // The Tab stop stays in the document when it scrolls out of view, so
    // focus is never lost and the grid stays reachable.
    if (tabIndex < from) indices.push(tabIndex);
    for (let index = from; index < to; index += 1) indices.push(index);
    if (tabIndex >= to) indices.push(tabIndex);
    body = [];
    let next = 0;
    const gap = (start: number, end: number) => (
      <tr key={`gap-${start}`} aria-hidden="true">
        <td
          className="mn-ai-trace-view__gap"
          colSpan={COLUMN_COUNT}
          style={{ blockSize: `${(end - start) * metrics.rowHeight}px` }}
        />
      </tr>
    );
    for (const index of indices) {
      if (index > next) body.push(gap(next, index));
      body.push(renderRow(index));
      next = index + 1;
    }
    if (next < rows.length) body.push(gap(next, rows.length));
  }

  const detail = (label: string, value: string | undefined) =>
    value === undefined ? null : (
      <div>
        <dt>{label}</dt>
        <dd>{value}</dd>
      </div>
    );
  const preview = (key: 'input' | 'output', label: string, text: string | undefined) => {
    if (text === undefined || text === '') return null;
    const headingId = `${baseId}-${key}`;
    return (
      <div className="mn-ai-trace-view__preview">
        <div className="mn-ai-trace-view__preview-header">
          <span id={headingId} className="mn-ai-trace-view__preview-label">
            {label}
          </span>
          <CopyButton
            size="xs"
            variant="ghost"
            value={text}
            label={m.copy}
            target={label}
            copiedLabel={m.copied}
            errorLabel={m.copyFailed}
            copiedMessage={m.copiedMessage}
            errorMessage={m.copyFailedMessage}
          />
        </div>
        {/* Focusable so keyboard users can scroll it. The text is a React
            string child: markup in a preview is shown, never interpreted. */}
        <div
          role="group"
          aria-labelledby={headingId}
          // A scrolling preview must be reachable by keyboard (WCAG 2.1.1).
          // eslint-disable-next-line jsx-a11y/no-noninteractive-tabindex
          tabIndex={0}
          className="mn-ai-trace-view__preview-scroll"
        >
          <pre className="mn-ai-trace-view__pre" dir="auto">
            {text}
          </pre>
        </div>
      </div>
    );
  };

  return (
    <div
      ref={ref}
      role="group"
      aria-labelledby={labelId}
      {...rest}
      className={cx('mn-ai-trace-view', className)}
      data-filter={filter}
      data-details={selected ? '' : undefined}
    >
      <div className="mn-ai-trace-view__header">
        <span id={labelId} className="mn-ai-trace-view__label">
          {m.label}
        </span>
        <SegmentedControl<AiTraceFilter>
          size="sm"
          aria-label={m.filterLabel}
          value={filter}
          onValueChange={setFilter}
          options={[
            { value: 'all', label: m.filters.all },
            { value: 'errors', label: m.filters.errors },
            { value: 'slowest', label: m.filters.slowest },
          ]}
        />
      </div>
      <dl className="mn-ai-trace-view__totals">
        <div>
          <dt>{m.totalDuration}</dt>
          <dd>{f.duration(totals.durationMs)}</dd>
        </div>
        <div>
          <dt>{m.totalTokens}</dt>
          <dd>{f.number(totals.totalTokens)}</dd>
        </div>
        {totals.costs.length > 0 ? (
          <div>
            <dt>{m.totalCost}</dt>
            <dd>{totals.costs.map((c) => f.cost(c.micros, c.currency)).join(' + ')}</dd>
          </div>
        ) : null}
        <div data-errors={totals.errorCount > 0 ? '' : undefined}>
          <dt>{m.errors}</dt>
          <dd>
            {totals.errorCount > 0 ? (
              <ErrorGlyph className="mn-ai-trace-view__status-icon" />
            ) : null}
            {f.number(totals.errorCount)}
          </dd>
        </div>
      </dl>
      <div className="mn-ai-trace-view__body">
        {rows.length === 0 ? (
          <p className="mn-ai-trace-view__empty">
            {filter === 'errors' && spans.length > 0 ? m.noErrors : m.empty}
          </p>
        ) : (
          <div
            ref={scrollRef}
            className="mn-ai-trace-view__scroll"
            data-windowed={windowed ? '' : undefined}
            onScroll={windowed ? handleScroll : undefined}
          >
            {/* A native table keeps the columns aligned; the tree grid role and
                the explicit row and cell roles give it the APG treegrid semantics. */}
            <table
              // eslint-disable-next-line jsx-a11y/no-noninteractive-element-to-interactive-role
              role="treegrid"
              aria-labelledby={labelId}
              aria-rowcount={windowed ? rows.length + 1 : undefined}
              className="mn-ai-trace-view__table"
            >
              <thead>
                <tr role="row" aria-rowindex={windowed ? 1 : undefined}>
                  <th role="columnheader" scope="col">
                    {m.columns.name}
                  </th>
                  <th role="columnheader" scope="col">
                    {m.columns.kind}
                  </th>
                  <th role="columnheader" scope="col" data-numeric="">
                    {m.columns.duration}
                  </th>
                  <th role="columnheader" scope="col" data-numeric="">
                    {m.columns.tokens}
                  </th>
                  <th role="columnheader" scope="col" data-numeric="">
                    {m.columns.cost}
                  </th>
                  <th role="columnheader" scope="col">
                    <span className="mn-ai-trace-view__scale">
                      <span>{m.columns.timeline}</span>
                      <span aria-hidden>{f.duration(totals.durationMs)}</span>
                    </span>
                  </th>
                </tr>
              </thead>
              {/* One delegated handler for every row, not three per row. */}
              {/* eslint-disable-next-line jsx-a11y/no-noninteractive-element-interactions */}
              <tbody
                ref={bodyRef}
                onKeyDown={handleKeyDown}
                onClick={handleClick}
                onFocus={handleFocus}
              >
                {body}
              </tbody>
            </table>
          </div>
        )}
        {selected ? (
          <section
            className="mn-ai-trace-view__details"
            aria-label={m.details(selected.name)}
            data-status={selected.status}
          >
            <div className="mn-ai-trace-view__details-header">
              <bdi className="mn-ai-trace-view__details-title">{selected.name}</bdi>
              <span className="mn-ai-trace-view__details-actions">
                {selectedLink ? (
                  <Button
                    size="xs"
                    variant="secondary"
                    onClick={() => onOpenPart?.(selectedLink, selected)}
                  >
                    {m.openPart}
                  </Button>
                ) : null}
                <Button size="xs" variant="ghost" onClick={() => setSelectedId(null)}>
                  {m.closeDetails}
                </Button>
              </span>
            </div>
            <dl className="mn-ai-trace-view__facts">
              {detail(m.status, m.statuses[selected.status])}
              {detail(m.columns.kind, m.kinds[selected.kind])}
              {detail(m.start, m.offset(f.duration(selected.startMs)))}
              {detail(
                m.end,
                selected.endMs === undefined ? undefined : m.offset(f.duration(selected.endMs)),
              )}
              {detail(
                m.duration,
                selected.endMs === undefined
                  ? m.statuses.running
                  : f.duration(selected.endMs - selected.startMs),
              )}
              {detail(
                m.inputTokens,
                selected.inputTokens === undefined ? undefined : f.number(selected.inputTokens),
              )}
              {detail(
                m.outputTokens,
                selected.outputTokens === undefined ? undefined : f.number(selected.outputTokens),
              )}
              {detail(
                m.cost,
                selected.costMicros === undefined
                  ? undefined
                  : f.cost(selected.costMicros, selected.currency ?? currency),
              )}
              {detail(m.model, selected.model)}
            </dl>
            {selected.error ? (
              <p className="mn-ai-trace-view__error">
                <ErrorGlyph className="mn-ai-trace-view__status-icon" />
                <span dir="auto">
                  <span className="mn-visually-hidden">{m.error}: </span>
                  {selected.error}
                </span>
              </p>
            ) : null}
            {selected.attributes && Object.keys(selected.attributes).length > 0 ? (
              <div className="mn-ai-trace-view__attributes">
                <span className="mn-ai-trace-view__preview-label">{m.attributes}</span>
                <dl className="mn-ai-trace-view__attribute-list">
                  {Object.entries(selected.attributes).map(([key, value]) => (
                    <div key={key}>
                      <dt>{key}</dt>
                      <dd>{typeof value === 'number' ? f.number(value) : String(value)}</dd>
                    </div>
                  ))}
                </dl>
              </div>
            ) : null}
            {preview('input', m.input, selected.input)}
            {preview('output', m.output, selected.output)}
          </section>
        ) : null}
      </div>
      {/* Opening details is a change away from the focused row: say it once, politely. */}
      <div role="status" className="mn-visually-hidden">
        {selected ? m.detailsShown(selected.name) : ''}
      </div>
    </div>
  );
});
