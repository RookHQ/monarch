'use client';

import {
  forwardRef,
  useEffect,
  useId,
  useMemo,
  useRef,
  useState,
  type CSSProperties,
  type HTMLAttributes,
  type KeyboardEvent,
  type PointerEvent as ReactPointerEvent,
  type ReactNode,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { ChevronRightGlyph, WarningGlyph } from '../../../internal/icons.js';
import { Button, IconButton } from '../../actions/button/index.js';
import { SegmentedControl } from '../../forms/segmented-control/index.js';
import { Input } from '../../forms/text-field/index.js';
import {
  addDays,
  daysBetween,
  isIsoDate,
  localToday,
  mondayOf,
  weekday,
} from './gantt-timeline.dates.js';

/** Zoom levels: how much time the visible window covers. */
export type GanttZoom = 'week' | 'month' | 'quarter' | 'year';

/** Fill colour of a bar. Pair it with words (`conflict`, `meta`); colour is never the only signal. */
export type GanttTone = 'accent' | 'neutral' | 'success' | 'warning' | 'danger';

/** One row of the timeline: a task, issue, project or phase. */
export interface GanttItem {
  /** Stable id. */
  id: string;
  /** Plain-text name, used in the bar's accessible name and the table. */
  label: string;
  /** Row heading content when it should differ from `label`, such as a key and title. */
  title?: ReactNode;
  /** First day, ISO `2026-01-12`. With only one of `start`/`end` the item is a single-day marker. */
  start?: string | null | undefined;
  /** Last day (inclusive), ISO. */
  end?: string | null | undefined;
  /** Parent item id, for nesting (sub-tasks). */
  parentId?: string | null | undefined;
  /** Group id (see `groups`). */
  groupId?: string | null | undefined;
  /** Completion from 0 to 100, drawn as a fill inside the bar. */
  progress?: number | undefined;
  tone?: GanttTone | undefined;
  /** Set `false` to lock this item's dates when the timeline is editable. */
  editable?: boolean | undefined;
  /** A scheduling problem, such as "Starts before WEB-12 ends". Shown with a warning icon and read with the bar. */
  conflict?: string | null | undefined;
  /** Extra content at the end of the row heading, such as counts or a status dot. */
  meta?: ReactNode;
}

/** A heading row grouping items, such as an initiative or epic. */
export interface GanttGroup {
  id: string;
  label: ReactNode;
  /** Optional span drawn as a thin bar on the group row. */
  start?: string | null | undefined;
  end?: string | null | undefined;
  /** Extra content in the group heading, such as a count or health badge. */
  meta?: ReactNode;
}

/** A link between two items. */
export interface GanttDependency {
  id: string;
  /** The item that must happen first. */
  from: string;
  /** The item that depends on it. */
  to: string;
  /** Default `finish-to-start`. */
  kind?: 'finish-to-start' | 'start-to-start' | 'finish-to-finish';
  /** The dates break this dependency: drawn dashed in danger colour. */
  conflict?: boolean;
}

/** A dated checkpoint, drawn as a diamond. */
export interface GanttMilestone {
  id: string;
  label: string;
  date: string;
  done?: boolean;
  /** Draw it on this item's row; without it, milestones share a lane at the top. */
  itemId?: string | undefined;
}

/** New dates for an item. */
export interface GanttDates {
  start: string | null;
  end: string | null;
}

/** Visible and announced strings. Override them to translate. */
export interface GanttTimelineMessages {
  /** Accessible name of the timeline and caption of the table. */
  label: string;
  /** Heading of the row-label column. */
  rowHeader: string;
  zoom: string;
  zoomLevels: Record<GanttZoom, string>;
  earlier: string;
  later: string;
  today: string;
  view: string;
  chart: string;
  table: string;
  milestones: string;
  /** Visible hint shown when bars can be edited. */
  hint: string;
  /** Keyboard instructions read with editable bars. */
  instructions: string;
  /** Accessible name of a bar. */
  bar: (label: string, start: string, end: string, progress: number | undefined) => string;
  conflict: (message: string) => string;
  milestone: (label: string, date: string, done: boolean) => string;
  /** Name cell of a milestone in the table. */
  milestoneRow: (label: string) => string;
  expand: (label: string) => string;
  collapse: (label: string) => string;
  /** Row text when an item is outside the visible window. */
  ended: (date: string) => string;
  starts: (date: string) => string;
  noDates: string;
  /** Heading of the list of items without dates. */
  unscheduled: (count: number) => string;
  /** Announced after a keyboard change. */
  changed: (label: string, start: string, end: string) => string;
  failed: (label: string) => string;
  /** Table headings. */
  columns: {
    name: string;
    start: string;
    end: string;
    duration: string;
    progress: string;
    dependsOn: string;
  };
  /** Accessible names of the table date inputs. */
  startOf: (label: string) => string;
  endOf: (label: string) => string;
  days: (n: number) => string;
  /** Formats an ISO date for labels and the table. */
  date: (iso: string, locale: string | undefined) => string;
  /** Formats the visible range. */
  range: (from: string, to: string, locale: string | undefined) => string;
}

const fmt = (iso: string, locale: string | undefined, opts: Intl.DateTimeFormatOptions) =>
  new Intl.DateTimeFormat(locale, { ...opts, timeZone: 'UTC' }).format(
    new Date(`${iso}T00:00:00Z`),
  );

export const DEFAULT_GANTT_TIMELINE_MESSAGES: GanttTimelineMessages = {
  label: 'Timeline',
  rowHeader: 'Item',
  zoom: 'Zoom',
  zoomLevels: { week: 'Weeks', month: 'Months', quarter: 'Quarters', year: 'Years' },
  earlier: 'Earlier',
  later: 'Later',
  today: 'Today',
  view: 'View',
  chart: 'Chart',
  table: 'Table',
  milestones: 'Milestones',
  hint: 'Drag a bar to move it and its edges to change dates. Focus a bar: arrows move a day, Shift+arrows change the end, Alt+arrows change the start.',
  instructions:
    'Left and right arrows move by a day. Shift with an arrow changes the end date, Alt with an arrow changes the start date. Up and down arrows go to the previous or next bar. Enter opens.',
  bar: (label, start, end, progress) =>
    `${label}, ${start === end ? start : `${start} to ${end}`}${progress === undefined ? '' : `, ${Math.round(progress)}% complete`}`,
  conflict: (m) => `Conflict: ${m}`,
  milestone: (label, date, done) => `Milestone ${label}, ${date}${done ? ', done' : ''}`,
  milestoneRow: (label) => `Milestone: ${label}`,
  expand: (l) => `Expand ${l}`,
  collapse: (l) => `Collapse ${l}`,
  ended: (d) => `Ended ${d}`,
  starts: (d) => `Starts ${d}`,
  noDates: 'No dates',
  unscheduled: (n) => `Unscheduled · ${n} without dates`,
  changed: (label, start, end) => `${label}: ${start === end ? start : `${start} to ${end}`}`,
  failed: (label) => `${label} could not be changed. The dates are back as they were.`,
  columns: {
    name: 'Name',
    start: 'Start',
    end: 'End',
    duration: 'Duration',
    progress: 'Progress',
    dependsOn: 'Depends on',
  },
  startOf: (l) => `Start of ${l}`,
  endOf: (l) => `End of ${l}`,
  days: (n) => `${n} ${n === 1 ? 'day' : 'days'}`,
  date: (iso, locale) => fmt(iso, locale, { day: 'numeric', month: 'short', year: 'numeric' }),
  range: (a, b, locale) =>
    `${fmt(a, locale, { day: 'numeric', month: 'short', year: 'numeric' })} – ${fmt(b, locale, { day: 'numeric', month: 'short', year: 'numeric' })}`,
};

/** Pixels per day, visible days and the Earlier/Later step for each zoom. */
const ZOOM: Record<GanttZoom, { day: number; span: number; step: number }> = {
  week: { day: 40, span: 28, step: 7 },
  month: { day: 16, span: 91, step: 30 },
  quarter: { day: 5, span: 273, step: 91 },
  year: { day: 1.6, span: 730, step: 182 },
};
const HEADER = 44;

export interface GanttTimelineProps extends Omit<
  HTMLAttributes<HTMLDivElement>,
  'children' | 'onChange'
> {
  /** The rows. Nesting comes from `parentId`, grouping from `groupId`. */
  items: GanttItem[];
  groups?: GanttGroup[] | undefined;
  dependencies?: GanttDependency[] | undefined;
  milestones?: GanttMilestone[] | undefined;
  /**
   * Called when someone moves or resizes a bar (pointer, keyboard or the
   * table's date fields). Return a promise to show the bar as saving; a
   * rejection puts it back and announces it. Without it the timeline is read-only.
   */
  onChange?: ((item: GanttItem, dates: GanttDates) => unknown) | undefined;
  /** Called when a bar is clicked or Enter is pressed on it. */
  onOpen?: ((item: GanttItem) => void) | undefined;
  /** Zoom (controlled). */
  zoom?: GanttZoom | undefined;
  /** Zoom at first (uncontrolled). Default `month`. */
  defaultZoom?: GanttZoom;
  onZoomChange?: ((zoom: GanttZoom) => void) | undefined;
  /** The zoom levels offered. Default month, quarter and year. */
  zoomLevels?: GanttZoom[];
  /** First visible day at first, ISO. Default: a week before `today`, or before the earliest item. */
  defaultStart?: string | undefined;
  /** Today, ISO. Pass it for identical server rendering; otherwise it is read after mount. */
  today?: string | undefined;
  /** `chart` (default) or the accessible `table` view at first. */
  defaultView?: 'chart' | 'table';
  /** Items without dates: listed under the chart (`list`, default) or kept as rows saying "No dates" (`row`). */
  undated?: 'list' | 'row';
  /** Show the zoom, navigation and view controls. Default `true`. */
  toolbar?: boolean;
  /** Extra controls at the end of the toolbar. */
  toolbarExtra?: ReactNode;
  /** BCP 47 locale for dates. */
  locale?: string | undefined;
  /** Override visible and announced strings. */
  messages?: Partial<GanttTimelineMessages>;
}

interface Span {
  start: string;
  end: string;
}

function spanOf(start: string | null | undefined, end: string | null | undefined): Span | null {
  const s = isIsoDate(start) ? start.slice(0, 10) : isIsoDate(end) ? end.slice(0, 10) : null;
  const e = isIsoDate(end) ? end.slice(0, 10) : s;
  if (!s || !e) return null;
  return e < s ? { start: e, end: s } : { start: s, end: e };
}

type DragMode = 'move' | 'start' | 'end';
interface Drag {
  id: string;
  mode: DragMode;
  originX: number;
  delta: number;
}

/** Applies a shift of `delta` days to an item's dates. Edges never cross. */
export function shiftGanttDates(
  item: Pick<GanttItem, 'start' | 'end'>,
  mode: DragMode,
  delta: number,
): GanttDates {
  const start = isIsoDate(item.start) ? item.start.slice(0, 10) : null;
  const end = isIsoDate(item.end) ? item.end.slice(0, 10) : null;
  if (delta === 0) return { start, end };
  const shift = (x: string | null) => (x ? addDays(x, delta) : x);
  if (mode === 'move') return { start: shift(start), end: shift(end) };
  if (mode === 'start') {
    const next = shift(start ?? end);
    const last = end ?? start;
    return { start: next && last && next > last ? last : next, end };
  }
  const next = shift(end ?? start);
  const first = start ?? end;
  return { start, end: next && first && next < first ? first : next };
}

type Line =
  | { kind: 'group'; group: GanttGroup }
  | { kind: 'item'; item: GanttItem; depth: number; hasChildren: boolean; rollup: Span | null };

function useCoarsePointer() {
  const [coarse, setCoarse] = useState(false);
  useEffect(() => {
    if (typeof window.matchMedia !== 'function') return;
    const mq = window.matchMedia('(pointer: coarse)');
    setCoarse(mq.matches);
    const on = () => setCoarse(mq.matches);
    mq.addEventListener?.('change', on);
    return () => mq.removeEventListener?.('change', on);
  }, []);
  return coarse;
}

/**
 * A Gantt timeline: items as bars on a calendar, moved and resized by
 * pointer or keyboard, with dependency arrows, milestones, a today line,
 * week to year zoom and a table view that lists (and edits) every date.
 */
export const GanttTimeline = forwardRef<HTMLDivElement, GanttTimelineProps>(function GanttTimeline(
  {
    items,
    groups = [],
    dependencies = [],
    milestones = [],
    onChange,
    onOpen,
    zoom: zoomProp,
    defaultZoom = 'month',
    onZoomChange,
    zoomLevels = ['month', 'quarter', 'year'],
    defaultStart,
    today: todayProp,
    defaultView = 'chart',
    undated = 'list',
    toolbar = true,
    toolbarExtra,
    locale,
    messages: messagesProp,
    className,
    ...rest
  },
  ref,
) {
  const messages = {
    ...DEFAULT_GANTT_TIMELINE_MESSAGES,
    ...messagesProp,
    zoomLevels: { ...DEFAULT_GANTT_TIMELINE_MESSAGES.zoomLevels, ...messagesProp?.zoomLevels },
    columns: { ...DEFAULT_GANTT_TIMELINE_MESSAGES.columns, ...messagesProp?.columns },
  };
  const baseId = useId();
  const instructionsId = `${baseId}-instructions`;
  const date = (iso: string) => messages.date(iso, locale);

  // Today: injected, or read after mount so server and client agree.
  const [clockToday, setClockToday] = useState<string | null>(null);
  useEffect(() => {
    if (!todayProp) setClockToday(localToday());
  }, [todayProp]);
  const today = todayProp ?? clockToday;

  const [zoomState, setZoomState] = useState<GanttZoom>(defaultZoom);
  const zoom = zoomProp ?? zoomState;
  const z = ZOOM[zoom];
  const setZoom = (next: GanttZoom) => {
    if (zoomProp === undefined) setZoomState(next);
    onZoomChange?.(next);
  };

  const earliest = useMemo(() => {
    let min: string | null = null;
    for (const i of items) {
      const s = spanOf(i.start, i.end);
      if (s && (!min || s.start < min)) min = s.start;
    }
    return min;
  }, [items]);
  const [anchorState, setAnchor] = useState<string | null>(() =>
    defaultStart
      ? defaultStart.slice(0, 10)
      : todayProp
        ? mondayOf(addDays(todayProp, -7))
        : earliest
          ? mondayOf(addDays(earliest, -7))
          : null,
  );
  // With no dates at all, wait for the clock.
  const anchor = anchorState ?? (today ? mondayOf(addDays(today, -7)) : '1970-01-05');
  const rangeEnd = addDays(anchor, z.span - 1);
  const x = (iso: string) => daysBetween(anchor, iso) * z.day;
  const gridWidth = Math.round(z.span * z.day);

  const coarse = useCoarsePointer();
  const rowH = coarse ? 44 : 36;

  const [view, setView] = useState<'chart' | 'table'>(defaultView);
  const [collapsed, setCollapsed] = useState<Set<string>>(() => new Set());
  const [drag, setDrag] = useState<Drag | null>(null);
  const dragRef = useRef<Drag | null>(null);
  const draggedAt = useRef(0);
  const [overrides, setOverrides] = useState<{ for: GanttItem[]; dates: Map<string, GanttDates> }>({
    for: items,
    dates: new Map(),
  });
  const optimistic = overrides.for === items ? overrides.dates : null;
  const [pending, setPending] = useState<Set<string>>(() => new Set());
  const [announcement, setAnnouncement] = useState('');
  const gridRef = useRef<HTMLDivElement>(null);

  const editableItem = (i: GanttItem) => onChange !== undefined && i.editable !== false;

  /** Dates shown for an item: drag preview, then a pending save, then its own. */
  const shownDates = (i: GanttItem): GanttDates => {
    const own = optimistic?.get(i.id) ?? { start: i.start ?? null, end: i.end ?? null };
    if (drag && drag.id === i.id) return shiftGanttDates(own, drag.mode, drag.delta);
    return own;
  };

  // Rows: groups, then nested items; items without dates are listed apart.
  const { lines, unscheduled } = useMemo(() => {
    const ids = new Set(items.map((i) => i.id));
    const children = new Map<string, GanttItem[]>();
    for (const i of items) {
      if (i.parentId && ids.has(i.parentId) && i.parentId !== i.id)
        children.set(i.parentId, [...(children.get(i.parentId) ?? []), i]);
    }
    const rollupOf = (id: string, seen: Set<string>): Span | null => {
      let start: string | null = null;
      let end: string | null = null;
      for (const c of children.get(id) ?? []) {
        if (seen.has(c.id)) continue;
        const s = spanOf(c.start, c.end) ?? rollupOf(c.id, new Set(seen).add(c.id));
        if (s) {
          if (!start || s.start < start) start = s.start;
          if (!end || s.end > end) end = s.end;
        }
      }
      return start && end ? { start, end } : null;
    };
    const out: Line[] = [];
    const none: GanttItem[] = [];
    const visit = (i: GanttItem, depth: number, seen: Set<string>) => {
      const kids = children.get(i.id) ?? [];
      const own = spanOf(i.start, i.end);
      const rollup = own ? null : rollupOf(i.id, new Set([i.id]));
      if (!own && !rollup && depth === 0 && undated === 'list' && kids.length === 0) {
        none.push(i);
        return;
      }
      out.push({ kind: 'item', item: i, depth, hasChildren: kids.length > 0, rollup });
      if (!collapsed.has(i.id))
        for (const k of kids) if (!seen.has(k.id)) visit(k, depth + 1, new Set(seen).add(k.id));
    };
    const topLevel = items.filter(
      (i) => !i.parentId || !ids.has(i.parentId) || i.parentId === i.id,
    );
    const groupIds = new Set(groups.map((g) => g.id));
    for (const g of groups) {
      out.push({ kind: 'group', group: g });
      for (const i of topLevel) if (i.groupId === g.id) visit(i, 0, new Set([i.id]));
    }
    for (const i of topLevel)
      if (!i.groupId || !groupIds.has(i.groupId)) visit(i, 0, new Set([i.id]));
    return { lines: out, unscheduled: none };
  }, [items, groups, collapsed, undated]);

  const laneMilestones = milestones.filter((m) => !m.itemId && isIsoDate(m.date));
  const hasLane = laneMilestones.length > 0;
  const lineTop = (index: number) => (hasLane ? rowH : 0) + index * rowH;
  const gridHeight = (hasLane ? rowH : 0) + lines.length * rowH;
  const rowOfItem = new Map<string, number>();
  lines.forEach((l, i) => {
    if (l.kind === 'item') rowOfItem.set(l.item.id, i);
  });
  const itemById = new Map(items.map((i) => [i.id, i]));
  const barItems = lines.flatMap((l) =>
    l.kind === 'item' && spanOf(shownDates(l.item).start, shownDates(l.item).end) ? [l.item] : [],
  );

  async function commit(item: GanttItem, dates: GanttDates) {
    if (!onChange) return;
    const before = { start: item.start ?? null, end: item.end ?? null };
    if (dates.start === before.start && dates.end === before.end) return;
    setOverrides((o) => {
      const map = new Map(o.for === items ? o.dates : []);
      map.set(item.id, dates);
      return { for: items, dates: map };
    });
    setPending((p) => new Set(p).add(item.id));
    try {
      await onChange(item, dates);
    } catch {
      setOverrides((o) => {
        const map = new Map(o.dates);
        map.delete(item.id);
        return { ...o, dates: map };
      });
      setAnnouncement(messages.failed(item.label));
    } finally {
      setPending((p) => {
        const next = new Set(p);
        next.delete(item.id);
        return next;
      });
    }
  }

  // Pointer drag: preview locally, save once on release.
  useEffect(() => {
    if (!drag) return;
    const move = (e: PointerEvent) => {
      const d = dragRef.current;
      if (!d) return;
      const delta = Math.round((e.clientX - d.originX) / z.day);
      if (delta === d.delta) return;
      const next = { ...d, delta };
      dragRef.current = next;
      setDrag(next);
    };
    const up = () => {
      const d = dragRef.current;
      dragRef.current = null;
      setDrag(null);
      if (!d || d.delta === 0) return;
      draggedAt.current = Date.now();
      const item = itemById.get(d.id);
      if (!item) return;
      const own = optimistic?.get(item.id) ?? { start: item.start ?? null, end: item.end ?? null };
      void commit(item, shiftGanttDates(own, d.mode, d.delta));
    };
    const key = (e: globalThis.KeyboardEvent) => {
      if (e.key !== 'Escape') return;
      dragRef.current = null;
      setDrag(null);
    };
    window.addEventListener('pointermove', move);
    window.addEventListener('pointerup', up);
    window.addEventListener('pointercancel', up);
    window.addEventListener('keydown', key);
    return () => {
      window.removeEventListener('pointermove', move);
      window.removeEventListener('pointerup', up);
      window.removeEventListener('pointercancel', up);
      window.removeEventListener('keydown', key);
    };
    // Re-bind only when a drag starts or ends, or the scale changes.
  }, [drag?.id, drag?.mode, z.day]); // eslint-disable-line react-hooks/exhaustive-deps

  function startDrag(e: ReactPointerEvent, item: GanttItem, mode: DragMode) {
    if (e.button !== 0 || !editableItem(item)) return;
    e.stopPropagation();
    e.preventDefault();
    const d = { id: item.id, mode, originX: e.clientX, delta: 0 };
    dragRef.current = d;
    setDrag(d);
  }

  function focusBar(id: string) {
    for (const el of gridRef.current?.querySelectorAll<HTMLElement>('[data-bar-id]') ?? []) {
      if (el.dataset.barId === id) {
        el.focus();
        return;
      }
    }
  }

  function onBarKeyDown(e: KeyboardEvent<HTMLButtonElement>, item: GanttItem) {
    if (e.key === 'ArrowUp' || e.key === 'ArrowDown') {
      const i = barItems.findIndex((b) => b.id === item.id);
      const next = barItems[i + (e.key === 'ArrowDown' ? 1 : -1)];
      if (next) {
        e.preventDefault();
        focusBar(next.id);
      }
      return;
    }
    if (e.key !== 'ArrowLeft' && e.key !== 'ArrowRight') return;
    if (!editableItem(item)) return;
    e.preventDefault();
    const delta = e.key === 'ArrowLeft' ? -1 : 1;
    const mode: DragMode = e.shiftKey ? 'end' : e.altKey ? 'start' : 'move';
    const own = optimistic?.get(item.id) ?? { start: item.start ?? null, end: item.end ?? null };
    const next = shiftGanttDates(own, mode, delta);
    const s = spanOf(next.start, next.end);
    if (s) setAnnouncement(messages.changed(item.label, date(s.start), date(s.end)));
    void commit(item, next);
  }

  // Header ticks.
  const ticks = useMemo(() => {
    const major: Array<{ iso: string; label: string }> = [];
    const minor: Array<{ iso: string; label: string }> = [];
    for (let d = 0; d < z.span; d++) {
      const iso = addDays(anchor, d);
      const dt = new Date(`${iso}T00:00:00Z`);
      const month = dt.getUTCMonth();
      if (dt.getUTCDate() === 1 || d === 0) {
        if (zoom !== 'year' || month % 3 === 0 || d === 0) {
          const withYear = month === 0 || d === 0 || zoom === 'quarter' || zoom === 'year';
          major.push({
            iso,
            label: new Intl.DateTimeFormat(locale, {
              month: 'short',
              ...(withYear ? { year: 'numeric' as const } : {}),
              timeZone: 'UTC',
            }).format(dt),
          });
        }
      }
      if (zoom === 'week')
        minor.push({
          iso,
          label: new Intl.DateTimeFormat(locale, {
            weekday: 'narrow',
            day: 'numeric',
            timeZone: 'UTC',
          }).format(dt),
        });
      else if (zoom === 'month' && weekday(iso) === 1)
        minor.push({ iso, label: String(dt.getUTCDate()) });
    }
    return { major, minor };
  }, [anchor, z.span, zoom, locale]);

  const depPaths = dependencies.flatMap((dep) => {
    const ra = rowOfItem.get(dep.from);
    const rb = rowOfItem.get(dep.to);
    const a = itemById.get(dep.from);
    const b = itemById.get(dep.to);
    if (ra === undefined || rb === undefined || !a || !b) return [];
    const da = shownDates(a);
    const db = shownDates(b);
    const sa = spanOf(da.start, da.end);
    const sb = spanOf(db.start, db.end);
    if (!sa || !sb) return [];
    const kind = dep.kind ?? 'finish-to-start';
    const fromX = kind === 'start-to-start' ? x(sa.start) : x(sa.end) + z.day;
    const toX = kind === 'finish-to-finish' ? x(sb.end) + z.day : x(sb.start);
    const y1 = lineTop(ra) + rowH / 2;
    const y2 = lineTop(rb) + rowH / 2;
    let d: string;
    if (kind === 'finish-to-finish') {
      const mid = Math.max(fromX, toX) + 10;
      d = `M${fromX},${y1} H${mid} V${y2} H${toX + 2}`;
    } else {
      const mid = Math.min(fromX + 10, toX - 10);
      d =
        mid > fromX
          ? `M${fromX},${y1} H${mid} V${y2} H${toX - 2}`
          : `M${fromX},${y1} h8 V${(y1 + y2) / 2} H${toX - 12} V${y2} H${toX - 2}`;
    }
    return [{ id: dep.id, d, conflict: dep.conflict === true }];
  });

  const percent = (p: number | undefined) =>
    p === undefined ? undefined : Math.max(0, Math.min(100, p));

  const renderBar = (item: GanttItem, top: number) => {
    const dates = shownDates(item);
    const s = spanOf(dates.start, dates.end);
    if (!s) return null;
    const label = messages.bar(item.label, date(s.start), date(s.end), percent(item.progress));
    const full = item.conflict ? `${label}. ${messages.conflict(item.conflict)}` : label;
    const can = editableItem(item);
    const point = !isIsoDate(dates.start) || !isIsoDate(dates.end);
    const left = x(s.start);
    const width = (daysBetween(s.start, s.end) + 1) * z.day;
    if (s.end < anchor || s.start > rangeEnd) {
      return (
        <span
          key={item.id}
          className="mn-gantt-timeline__offscreen"
          style={{ top: top + rowH / 2 }}
        >
          {s.end < anchor ? messages.ended(date(s.end)) : messages.starts(date(s.start))}
        </span>
      );
    }
    const style: CSSProperties = point
      ? { left: left + z.day / 2 - 8, top: top + rowH / 2 - 8 }
      : { left, width: Math.max(width, 6), top: top + 5, height: rowH - 10 };
    return (
      <button
        key={item.id}
        type="button"
        className="mn-gantt-timeline__bar"
        data-bar-id={item.id}
        data-shape={point ? 'point' : 'bar'}
        data-tone={item.conflict ? 'danger' : (item.tone ?? 'accent')}
        data-conflict={item.conflict ? '' : undefined}
        data-dragging={drag?.id === item.id || undefined}
        data-editable={can || undefined}
        aria-busy={pending.has(item.id) || undefined}
        aria-label={full}
        aria-describedby={can ? instructionsId : undefined}
        title={item.conflict ?? undefined}
        style={style}
        onPointerDown={can ? (e) => startDrag(e, item, 'move') : undefined}
        onClick={() => {
          if (Date.now() - draggedAt.current > 250) onOpen?.(item);
        }}
        onKeyDown={(e) => onBarKeyDown(e, item)}
      >
        {!point && item.progress !== undefined ? (
          <span
            className="mn-gantt-timeline__progress"
            style={{ width: `${percent(item.progress)}%` }}
          />
        ) : null}
        {!point && can ? (
          <span
            className="mn-gantt-timeline__handle"
            data-edge="start"
            onPointerDown={(e) => startDrag(e, item, 'start')}
          />
        ) : null}
        {!point && item.conflict ? (
          <span className="mn-gantt-timeline__warn">
            <WarningGlyph />
          </span>
        ) : null}
        {!point && width > 70 ? (
          <span className="mn-gantt-timeline__bar-label">{item.label}</span>
        ) : null}
        {!point && can ? (
          <span
            className="mn-gantt-timeline__handle"
            data-edge="end"
            onPointerDown={(e) => startDrag(e, item, 'end')}
          />
        ) : null}
      </button>
    );
  };

  const milestoneMark = (m: GanttMilestone, centerY: number, withLabel: boolean) => {
    if (!isIsoDate(m.date) || m.date < anchor || m.date > rangeEnd) return null;
    return (
      <span
        key={m.id}
        className="mn-gantt-timeline__milestone"
        data-done={m.done || undefined}
        style={{ left: x(m.date) + z.day / 2 - 6, top: centerY - 6 }}
        role="img"
        aria-label={messages.milestone(m.label, date(m.date), m.done === true)}
        title={`${m.label} · ${date(m.date)}`}
      >
        <span className="mn-gantt-timeline__diamond" />
        {withLabel ? (
          <span className="mn-gantt-timeline__milestone-label" aria-hidden>
            {m.label}
          </span>
        ) : null}
      </span>
    );
  };

  const toggleCollapsed = (id: string) =>
    setCollapsed((c) => {
      const next = new Set(c);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });

  const editable = onChange !== undefined;

  const tableView = (
    <div className="mn-gantt-timeline__table-wrap">
      <table className="mn-gantt-timeline__table">
        <caption className="mn-visually-hidden">{messages.label}</caption>
        <thead>
          <tr>
            <th scope="col">{messages.columns.name}</th>
            <th scope="col">{messages.columns.start}</th>
            <th scope="col">{messages.columns.end}</th>
            <th scope="col">{messages.columns.duration}</th>
            <th scope="col">{messages.columns.progress}</th>
            <th scope="col">{messages.columns.dependsOn}</th>
          </tr>
        </thead>
        <tbody>
          {[
            ...lines.map((l) => l),
            ...unscheduled.map((i) => ({
              kind: 'item' as const,
              item: i,
              depth: 0,
              hasChildren: false,
              rollup: null,
            })),
          ].map((l) => {
            if (l.kind === 'group') {
              return (
                <tr key={`g-${l.group.id}`} className="mn-gantt-timeline__table-group">
                  <th scope="rowgroup" colSpan={6}>
                    {l.group.label}
                  </th>
                </tr>
              );
            }
            const item = l.item;
            const dates = optimistic?.get(item.id) ?? {
              start: item.start ?? null,
              end: item.end ?? null,
            };
            const s = spanOf(dates.start, dates.end);
            const deps = dependencies
              .filter((d) => d.to === item.id)
              .map((d) => itemById.get(d.from)?.label)
              .filter(Boolean)
              .join(', ');
            const can = editableItem(item);
            return (
              <tr key={item.id} data-conflict={item.conflict ? '' : undefined}>
                <th
                  scope="row"
                  style={{
                    paddingInlineStart: `calc(${l.depth} * var(--mn-space-4) + var(--mn-space-2))`,
                  }}
                >
                  {item.label}
                  {item.conflict ? (
                    <span className="mn-gantt-timeline__table-conflict">
                      {messages.conflict(item.conflict)}
                    </span>
                  ) : null}
                </th>
                <td>
                  {can ? (
                    <Input
                      type="date"
                      size="sm"
                      aria-label={messages.startOf(item.label)}
                      defaultValue={dates.start ?? ''}
                      key={`s-${dates.start ?? ''}`}
                      onBlur={(e) => {
                        const v = e.currentTarget.value || null;
                        if (v !== dates.start)
                          void commit(item, {
                            start: v,
                            end: dates.end && v && v > dates.end ? v : dates.end,
                          });
                      }}
                    />
                  ) : s ? (
                    date(s.start)
                  ) : (
                    '–'
                  )}
                </td>
                <td>
                  {can ? (
                    <Input
                      type="date"
                      size="sm"
                      aria-label={messages.endOf(item.label)}
                      defaultValue={dates.end ?? ''}
                      key={`e-${dates.end ?? ''}`}
                      onBlur={(e) => {
                        const v = e.currentTarget.value || null;
                        if (v !== dates.end)
                          void commit(item, {
                            start: dates.start && v && v < dates.start ? v : dates.start,
                            end: v,
                          });
                      }}
                    />
                  ) : s ? (
                    date(s.end)
                  ) : (
                    '–'
                  )}
                </td>
                <td>{s ? messages.days(daysBetween(s.start, s.end) + 1) : messages.noDates}</td>
                <td>
                  {item.progress === undefined
                    ? '–'
                    : `${Math.round(percent(item.progress) ?? 0)}%`}
                </td>
                <td>{deps || '–'}</td>
              </tr>
            );
          })}
          {milestones.map((m) => (
            <tr key={`m-${m.id}`} className="mn-gantt-timeline__table-milestone">
              <th scope="row">{messages.milestoneRow(m.label)}</th>
              <td colSpan={2}>{isIsoDate(m.date) ? date(m.date) : '–'}</td>
              <td>–</td>
              <td>{m.done ? '100%' : '–'}</td>
              <td>{m.itemId ? (itemById.get(m.itemId)?.label ?? '–') : '–'}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );

  const showToday = today !== null && today >= anchor && today <= rangeEnd;

  return (
    <div
      ref={ref}
      {...rest}
      className={cx('mn-gantt-timeline', className)}
      data-zoom={zoom}
      data-view={view}
      data-dragging={drag ? drag.mode : undefined}
      data-readonly={!editable || undefined}
    >
      {toolbar ? (
        <div className="mn-gantt-timeline__toolbar">
          {zoomLevels.length > 1 ? (
            <SegmentedControl
              size="sm"
              aria-label={messages.zoom}
              value={zoom}
              onValueChange={(v) => setZoom(v as GanttZoom)}
              options={zoomLevels.map((level) => ({
                value: level,
                label: messages.zoomLevels[level],
              }))}
            />
          ) : null}
          <span className="mn-gantt-timeline__nav">
            <IconButton
              size="sm"
              variant="secondary"
              label={messages.earlier}
              icon={<ChevronRightGlyph style={{ transform: 'scaleX(-1)' }} />}
              onClick={() => setAnchor(addDays(anchor, -z.step))}
            />
            <Button
              size="sm"
              onClick={() => setAnchor(mondayOf(addDays(todayProp ?? localToday(), -7)))}
            >
              {messages.today}
            </Button>
            <IconButton
              size="sm"
              variant="secondary"
              label={messages.later}
              icon={<ChevronRightGlyph />}
              onClick={() => setAnchor(addDays(anchor, z.step))}
            />
          </span>
          <span className="mn-gantt-timeline__range" aria-live="polite">
            {messages.range(anchor, rangeEnd, locale)}
          </span>
          <SegmentedControl
            size="sm"
            aria-label={messages.view}
            value={view}
            onValueChange={(v) => setView(v as 'chart' | 'table')}
            options={[
              { value: 'chart', label: messages.chart },
              { value: 'table', label: messages.table },
            ]}
          />
          {toolbarExtra}
        </div>
      ) : null}
      {editable && view === 'chart' ? (
        <p className="mn-gantt-timeline__hint">{messages.hint}</p>
      ) : null}
      {view === 'table' ? (
        tableView
      ) : (
        <div className="mn-gantt-timeline__scroller" role="group" aria-label={messages.label}>
          <div
            className="mn-gantt-timeline__canvas"
            style={{ width: `calc(var(--mn-gantt-timeline-label-width) + ${gridWidth}px)` }}
          >
            <div className="mn-gantt-timeline__labels">
              <div className="mn-gantt-timeline__corner" style={{ height: HEADER }}>
                {messages.rowHeader}
              </div>
              {hasLane ? (
                <div className="mn-gantt-timeline__label" data-kind="lane" style={{ height: rowH }}>
                  {messages.milestones}
                </div>
              ) : null}
              {lines.map((l) =>
                l.kind === 'group' ? (
                  <div
                    key={`g-${l.group.id}`}
                    className="mn-gantt-timeline__label"
                    data-kind="group"
                    style={{ height: rowH }}
                  >
                    <span className="mn-gantt-timeline__label-text">{l.group.label}</span>
                    {l.group.meta ? (
                      <span className="mn-gantt-timeline__meta">{l.group.meta}</span>
                    ) : null}
                  </div>
                ) : (
                  <div
                    key={l.item.id}
                    className="mn-gantt-timeline__label"
                    data-kind="item"
                    style={{
                      height: rowH,
                      paddingInlineStart: `calc(var(--mn-space-2) + ${l.depth} * var(--mn-space-4))`,
                    }}
                  >
                    {l.hasChildren ? (
                      <button
                        type="button"
                        className="mn-gantt-timeline__toggle"
                        aria-expanded={!collapsed.has(l.item.id)}
                        aria-label={
                          collapsed.has(l.item.id)
                            ? messages.expand(l.item.label)
                            : messages.collapse(l.item.label)
                        }
                        onClick={() => toggleCollapsed(l.item.id)}
                      >
                        <ChevronRightGlyph aria-hidden />
                      </button>
                    ) : (
                      <span className="mn-gantt-timeline__toggle-space" aria-hidden />
                    )}
                    <span className="mn-gantt-timeline__label-text">
                      {l.item.title ?? l.item.label}
                    </span>
                    {l.item.meta ? (
                      <span className="mn-gantt-timeline__meta">{l.item.meta}</span>
                    ) : null}
                  </div>
                ),
              )}
            </div>
            <div className="mn-gantt-timeline__grid-col" style={{ width: gridWidth }}>
              <div className="mn-gantt-timeline__header" style={{ height: HEADER }} aria-hidden>
                {ticks.major.map((t) => (
                  <span key={t.iso} className="mn-gantt-timeline__major" style={{ left: x(t.iso) }}>
                    {t.label}
                  </span>
                ))}
                {ticks.minor.map((t) => (
                  <span
                    key={t.iso}
                    className="mn-gantt-timeline__minor"
                    style={{ left: x(t.iso) + 2 }}
                  >
                    {t.label}
                  </span>
                ))}
              </div>
              <div ref={gridRef} className="mn-gantt-timeline__grid" style={{ height: gridHeight }}>
                <div className="mn-gantt-timeline__decor" aria-hidden>
                  {zoom === 'week' || zoom === 'month'
                    ? Array.from({ length: z.span }, (_, d) => addDays(anchor, d))
                        .filter((iso) => weekday(iso) === 0 || weekday(iso) === 6)
                        .map((iso) => (
                          <span
                            key={iso}
                            className="mn-gantt-timeline__weekend"
                            style={{ left: x(iso), width: z.day }}
                          />
                        ))
                    : null}
                  {(zoom === 'week' || zoom === 'month' ? ticks.minor : ticks.major).map((t) => (
                    <span
                      key={t.iso}
                      className="mn-gantt-timeline__gridline"
                      style={{ left: x(t.iso) }}
                    />
                  ))}
                  {lines.map((l, i) => (
                    <span
                      key={l.kind === 'group' ? `g-${l.group.id}` : l.item.id}
                      className="mn-gantt-timeline__rowline"
                      data-kind={l.kind}
                      style={{ top: lineTop(i), height: rowH }}
                    />
                  ))}
                </div>
                {showToday && today ? (
                  <span
                    className="mn-gantt-timeline__today"
                    style={{ left: x(today) + z.day / 2 }}
                    aria-hidden
                  >
                    <span className="mn-gantt-timeline__today-label">{messages.today}</span>
                  </span>
                ) : null}
                {laneMilestones.map((m) => milestoneMark(m, rowH / 2, true))}
                {lines.map((l, i) => {
                  const top = lineTop(i);
                  if (l.kind === 'group') {
                    const s = spanOf(l.group.start, l.group.end);
                    if (!s || s.end < anchor || s.start > rangeEnd) return null;
                    const left = Math.max(0, x(s.start));
                    return (
                      <span
                        key={`g-${l.group.id}`}
                        className="mn-gantt-timeline__group-span"
                        style={{
                          left,
                          width: Math.max(4, x(s.end) + z.day - left),
                          top: top + rowH / 2 - 4,
                        }}
                        aria-hidden
                      />
                    );
                  }
                  const own = spanOf(shownDates(l.item).start, shownDates(l.item).end);
                  if (!own) {
                    if (l.rollup && l.rollup.end >= anchor && l.rollup.start <= rangeEnd) {
                      const left = x(l.rollup.start);
                      return (
                        <span
                          key={l.item.id}
                          className="mn-gantt-timeline__rollup"
                          style={{
                            left,
                            width: (daysBetween(l.rollup.start, l.rollup.end) + 1) * z.day,
                            top: top + rowH / 2 - 4,
                          }}
                          aria-hidden
                        />
                      );
                    }
                    return (
                      <span
                        key={l.item.id}
                        className="mn-gantt-timeline__offscreen"
                        style={{ top: top + rowH / 2 }}
                      >
                        {messages.noDates}
                      </span>
                    );
                  }
                  return renderBar(l.item, top);
                })}
                {milestones
                  .filter((m) => m.itemId && rowOfItem.has(m.itemId))
                  .map((m) =>
                    milestoneMark(
                      m,
                      lineTop(rowOfItem.get(m.itemId as string) ?? 0) + rowH - 7,
                      false,
                    ),
                  )}
                {depPaths.length > 0 ? (
                  <svg
                    className="mn-gantt-timeline__deps"
                    width={gridWidth}
                    height={gridHeight}
                    aria-hidden
                    focusable={false}
                  >
                    <defs>
                      <marker
                        id={`${baseId}-arrow`}
                        viewBox="0 0 8 8"
                        refX="7"
                        refY="4"
                        markerWidth="6"
                        markerHeight="6"
                        orient="auto"
                      >
                        <path d="M0,0 L8,4 L0,8 z" className="mn-gantt-timeline__arrowhead" />
                      </marker>
                      <marker
                        id={`${baseId}-arrow-bad`}
                        viewBox="0 0 8 8"
                        refX="7"
                        refY="4"
                        markerWidth="6"
                        markerHeight="6"
                        orient="auto"
                      >
                        <path
                          d="M0,0 L8,4 L0,8 z"
                          className="mn-gantt-timeline__arrowhead"
                          data-conflict=""
                        />
                      </marker>
                    </defs>
                    {depPaths.map((p) => (
                      <path
                        key={p.id}
                        d={p.d}
                        className="mn-gantt-timeline__dep"
                        data-conflict={p.conflict || undefined}
                        markerEnd={`url(#${p.conflict ? `${baseId}-arrow-bad` : `${baseId}-arrow`})`}
                      />
                    ))}
                  </svg>
                ) : null}
              </div>
            </div>
          </div>
        </div>
      )}
      {view === 'chart' && unscheduled.length > 0 ? (
        <details className="mn-gantt-timeline__unscheduled">
          <summary>{messages.unscheduled(unscheduled.length)}</summary>
          <ul>
            {unscheduled.map((i) => (
              <li key={i.id}>
                {onOpen ? (
                  <button
                    type="button"
                    className="mn-gantt-timeline__link"
                    onClick={() => onOpen(i)}
                  >
                    {i.title ?? i.label}
                  </button>
                ) : (
                  (i.title ?? i.label)
                )}
                {i.meta ? <span className="mn-gantt-timeline__meta">{i.meta}</span> : null}
              </li>
            ))}
          </ul>
        </details>
      ) : null}
      <p id={instructionsId} className="mn-visually-hidden">
        {messages.instructions}
      </p>
      <p className="mn-visually-hidden" role="status" aria-live="polite">
        {announcement}
      </p>
    </div>
  );
});
