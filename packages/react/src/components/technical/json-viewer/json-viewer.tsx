'use client';

import {
  forwardRef,
  useCallback,
  useEffect,
  useId,
  useMemo,
  useRef,
  useState,
  type HTMLAttributes,
  type KeyboardEvent,
  type MouseEvent,
  type ReactNode,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { useDirection } from '../../../internal/direction.js';
import { ChevronRightGlyph } from '../../../internal/icons.js';
import { Button } from '../../actions/button/index.js';
import { CopyButton } from '../../actions/copy-button/index.js';
import { SearchInput } from '../../search/search-input/index.js';

/** One step in a JSON path: an object key or an array index. */
export type JsonPathSegment = string | number;

/** The kind of a value, as shown and styled by JSONViewer (`data-type`). */
export type JsonValueType =
  | 'object'
  | 'array'
  | 'string'
  | 'number'
  | 'boolean'
  | 'null'
  | 'undefined'
  | 'other'
  | 'circular';

/** Every visible or announced string, with English defaults. */
export interface JSONViewerMessages {
  /** Accessible name of the tree. */
  label: string;
  /** Accessible name of the search field. */
  search: string;
  /** Placeholder of the search field. */
  searchPlaceholder: string;
  /** Match count after a search. */
  matches: (count: number, capped: boolean) => string;
  /** Collapses every node back to the top level. */
  collapseAll: string;
  copyPath: string;
  copyValue: string;
  /** Prefix before the selected node's path in the footer. */
  selectedPath: string;
  /** Collapsed object summary. */
  keys: (count: number) => string;
  /** Collapsed array summary. */
  items: (count: number) => string;
  /** Label of the row that renders more children. */
  showMore: (next: number, remaining: number) => string;
  /** Shown instead of a value that refers back to one of its ancestors. */
  circular: string;
  /** Appended to a string cut at `maxStringLength`. */
  truncated: (length: number) => string;
}

export const JSON_VIEWER_MESSAGES: JSONViewerMessages = {
  label: 'JSON',
  search: 'Search keys and values',
  searchPlaceholder: 'Search',
  matches: (n, capped) =>
    n === 0
      ? 'No matches'
      : `${n.toLocaleString()}${capped ? '+' : ''} ${n === 1 ? 'match' : 'matches'}`,
  collapseAll: 'Collapse all',
  copyPath: 'Copy path',
  copyValue: 'Copy value',
  selectedPath: 'Selected',
  keys: (n) => (n === 1 ? '1 key' : `${n.toLocaleString()} keys`),
  items: (n) => (n === 1 ? '1 item' : `${n.toLocaleString()} items`),
  showMore: (next, remaining) =>
    `Show ${next.toLocaleString()} more (${remaining.toLocaleString()} hidden)`,
  circular: '[Circular reference]',
  truncated: (n) => `… (${n.toLocaleString()} characters)`,
};

const IDENTIFIER = /^[A-Za-z_$][A-Za-z0-9_$]*$/;

/**
 * Formats a path as JSONPath-style text: `$.users[0].name`, with keys that
 * are not identifiers quoted (`$["first name"]`).
 */
export function formatJsonPath(segments: readonly JsonPathSegment[], root = '$'): string {
  let out = root;
  for (const s of segments) {
    if (typeof s === 'number') out += `[${s}]`;
    else if (IDENTIFIER.test(s)) out += out === '' ? s : `.${s}`;
    else out += `[${JSON.stringify(s)}]`;
  }
  return out;
}

/** JSON text of any value: indented, with circular references replaced instead of throwing. */
export function stringifyJsonValue(value: unknown, indent = 2): string {
  if (value === undefined) return 'undefined';
  const ancestors: unknown[] = [];
  const text = JSON.stringify(
    value,
    function replacer(this: unknown, _key: string, v: unknown) {
      if (typeof v === 'bigint') return v.toString();
      if (typeof v !== 'object' || v === null) return v;
      // `this` is the holder; trim the ancestor stack back to it.
      while (ancestors.length > 0 && ancestors[ancestors.length - 1] !== this) ancestors.pop();
      if (ancestors.includes(v)) return '[Circular]';
      ancestors.push(v);
      return v;
    },
    indent,
  );
  return text ?? String(value);
}

function typeOf(value: unknown): JsonValueType {
  if (value === null) return 'null';
  if (Array.isArray(value)) return 'array';
  switch (typeof value) {
    case 'object':
      return 'object';
    case 'string':
      return 'string';
    case 'number':
    case 'bigint':
      return 'number';
    case 'boolean':
      return 'boolean';
    case 'undefined':
      return 'undefined';
    default:
      return 'other';
  }
}

/** Objects with a `toJSON` (Dates) are shown as what JSON would write. */
function normalize(value: unknown): unknown {
  if (value && typeof value === 'object' && !Array.isArray(value)) {
    const toJSON = (value as { toJSON?: unknown }).toJSON;
    if (typeof toJSON === 'function') {
      try {
        return (toJSON as () => unknown).call(value);
      } catch {
        return String(value);
      }
    }
  }
  return value;
}

function childEntries(value: unknown): [JsonPathSegment, unknown][] {
  if (Array.isArray(value)) return value.map((v, i) => [i, v]);
  if (value && typeof value === 'object') return Object.entries(value as Record<string, unknown>);
  return [];
}

function childCount(value: unknown): number {
  if (Array.isArray(value)) return value.length;
  if (value && typeof value === 'object') return Object.keys(value).length;
  return 0;
}

const idOf = (segments: readonly JsonPathSegment[]) => JSON.stringify(segments);

interface Row {
  kind: 'node' | 'more';
  id: string;
  segments: JsonPathSegment[];
  parentId: string | null;
  /** Object key, array index, or null for the root. */
  name: JsonPathSegment | null;
  value: unknown;
  type: JsonValueType;
  depth: number;
  count: number;
  posinset: number;
  setsize: number;
  /** For `more` rows: how many children are still hidden. */
  remaining: number;
}

export interface JSONViewerProps extends Omit<
  HTMLAttributes<HTMLDivElement>,
  'children' | 'onCopy'
> {
  /** The parsed JSON to show (objects, arrays and primitives). Parse JSON text with `JSON.parse` first. */
  value: unknown;
  /** Levels expanded on first render. `1` shows the root's children; `0` shows only the root. Default 1. */
  defaultExpandDepth?: number | undefined;
  /** Children rendered per object or array before a "Show more" row; each activation adds this many. Default 100. */
  maxChildren?: number | undefined;
  /** Strings longer than this are cut in the tree (the copied value is always complete). Default 500. */
  maxStringLength?: number | undefined;
  /** Show the search field. Default true. */
  searchable?: boolean | undefined;
  /** Search stops after this many matches, to stay fast on large documents. Default 500. */
  maxMatches?: number | undefined;
  /** Show the copy path and copy value actions for the selected node. Default true. */
  copyable?: boolean | undefined;
  /** First segment of copied and shown paths. Default `$` (JSONPath). Pass `''` for `users[0].name`. */
  rootPath?: string | undefined;
  /** Called after a path or value is copied. */
  onCopy?: ((text: string, kind: 'path' | 'value') => void) | undefined;
  /** Called when the selected (focused) node changes. */
  onSelectPath?: ((segments: JsonPathSegment[], value: unknown) => void) | undefined;
  /** Extra controls at the end of the toolbar, e.g. a download button. */
  actions?: ReactNode | undefined;
  /** Replaces any of the default strings. */
  messages?: Partial<JSONViewerMessages> | undefined;
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
      <mark key={at} className="mn-json-viewer__mark">
        {text.slice(at, at + query.length)}
      </mark>,
    );
    from = at + query.length;
    at = lower.indexOf(query, from);
  }
  if (from < text.length) parts.push(text.slice(from));
  return <>{parts}</>;
}

/**
 * A collapsible, keyboard-navigable tree for a JSON document, with search,
 * copy path and copy value. Children render only when their parent is open,
 * large containers render in batches behind "Show more", and circular
 * references are shown instead of looping, so big or odd payloads stay fast.
 */
export const JSONViewer = forwardRef<HTMLDivElement, JSONViewerProps>(function JSONViewer(
  {
    value,
    defaultExpandDepth = 1,
    maxChildren = 100,
    maxStringLength = 500,
    searchable = true,
    maxMatches = 500,
    copyable = true,
    rootPath = '$',
    onCopy,
    onSelectPath,
    actions,
    messages,
    className,
    ...rest
  },
  ref,
) {
  const m = { ...JSON_VIEWER_MESSAGES, ...messages };
  const readDir = useDirection();
  const baseId = useId();
  const [baseDepth, setBaseDepth] = useState(defaultExpandDepth);
  const [overrides, setOverrides] = useState<Map<string, boolean>>(() => new Map());
  const [limits, setLimits] = useState<Map<string, number>>(() => new Map());
  const [query, setQuery] = useState('');
  const [activeId, setActiveId] = useState<string | null>(null);
  const treeRef = useRef<HTMLDivElement>(null);
  const focusPending = useRef(false);

  const needle = query.trim().toLowerCase();

  // Search: walk the whole document once per query, cycle-safe and capped.
  const search = useMemo(() => {
    const matched = new Set<string>();
    const open = new Set<string>();
    const reveal = new Map<string, number>();
    if (!needle) return { matched, open, reveal, count: 0, capped: false };
    let count = 0;
    let capped = false;
    const ancestors = new Set<unknown>();
    const walk = (raw: unknown, segments: JsonPathSegment[]): boolean => {
      if (capped) return false;
      const v = normalize(raw);
      const name = segments[segments.length - 1];
      const type = typeOf(v);
      let hit =
        (name !== undefined && String(name).toLowerCase().includes(needle)) ||
        (type !== 'object' && type !== 'array' && String(v).toLowerCase().includes(needle));
      if (hit) {
        matched.add(idOf(segments));
        count += 1;
        if (count >= maxMatches) capped = true;
      }
      if ((type === 'object' || type === 'array') && !ancestors.has(v)) {
        ancestors.add(v);
        let childHit = false;
        const entries = childEntries(v);
        for (const [i, [key, child]] of entries.entries()) {
          if (walk(child, [...segments, key])) {
            childHit = true;
            const pid = idOf(segments);
            reveal.set(pid, Math.max(reveal.get(pid) ?? 0, i + 1));
          }
          if (capped) break;
        }
        ancestors.delete(v);
        if (childHit) open.add(idOf(segments));
        hit = hit || childHit;
      }
      return hit;
    };
    walk(value, []);
    return { matched, open, reveal, count, capped };
  }, [value, needle, maxMatches]);

  const isOpen = useCallback(
    (id: string, depth: number) => {
      const o = overrides.get(id);
      if (o !== undefined) return o;
      if (search.open.has(id)) return true;
      return depth < baseDepth;
    },
    [overrides, search, baseDepth],
  );

  // Flatten what is visible. Only open containers are walked.
  const rows = useMemo(() => {
    const out: Row[] = [];
    const ancestors = new Set<unknown>();
    const visit = (
      raw: unknown,
      segments: JsonPathSegment[],
      parentId: string | null,
      depth: number,
      posinset: number,
      setsize: number,
    ) => {
      const v = normalize(raw);
      let type = typeOf(v);
      if ((type === 'object' || type === 'array') && ancestors.has(v)) type = 'circular';
      const id = idOf(segments);
      const count = type === 'object' || type === 'array' ? childCount(v) : 0;
      out.push({
        kind: 'node',
        id,
        segments,
        parentId,
        name: segments.at(-1) ?? null,
        value: v,
        type,
        depth,
        count,
        posinset,
        setsize,
        remaining: 0,
      });
      if (count === 0 || !isOpen(id, depth)) return;
      ancestors.add(v);
      const limit = Math.max(limits.get(id) ?? maxChildren, search.reveal.get(id) ?? 0);
      const entries = childEntries(v);
      const shown = Math.min(entries.length, limit);
      const size = entries.length;
      for (const [i, [key, child]] of entries.slice(0, shown).entries()) {
        visit(child, [...segments, key], id, depth + 1, i + 1, size);
      }
      if (shown < entries.length) {
        out.push({
          kind: 'more',
          id: `${id}#more`,
          segments,
          parentId: id,
          name: null,
          value: undefined,
          type: 'other',
          depth: depth + 1,
          count: 0,
          posinset: shown + 1,
          setsize: size,
          remaining: entries.length - shown,
        });
      }
      ancestors.delete(v);
    };
    visit(value, [], null, 0, 1, 1);
    return out;
  }, [value, isOpen, limits, maxChildren, search]);

  const indexById = useMemo(() => new Map(rows.map((r, i) => [r.id, i])), [rows]);
  const active = (activeId !== null && indexById.has(activeId) ? activeId : rows[0]?.id) ?? null;
  const activeRow = active !== null ? rows[indexById.get(active) ?? -1] : undefined;
  const selectedNode = activeRow?.kind === 'node' ? activeRow : undefined;

  const domId = (id: string) => `${baseId}-${indexById.get(id) ?? 0}`;

  useEffect(() => {
    if (!focusPending.current || active === null) return;
    focusPending.current = false;
    const el = treeRef.current?.ownerDocument.getElementById(domId(active));
    el?.focus();
    el?.scrollIntoView?.({ block: 'nearest' });
    // domId only reads `indexById`, which changes with `rows`.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [active, rows]);

  const onSelectRef = useRef(onSelectPath);
  onSelectRef.current = onSelectPath;
  useEffect(() => {
    if (selectedNode) onSelectRef.current?.(selectedNode.segments, selectedNode.value);
    // Only report when the selection itself changes.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [selectedNode?.id]);

  const moveTo = (id: string | undefined) => {
    if (id === undefined) return;
    focusPending.current = true;
    setActiveId(id);
  };

  const setOpen = (row: Row, open: boolean) => {
    setOverrides((prev) => new Map(prev).set(row.id, open));
  };

  const showMore = (row: Row) => {
    const parentId = row.parentId;
    if (parentId === null) return;
    setLimits((prev) => new Map(prev).set(parentId, row.posinset - 1 + maxChildren));
    // Keep focus on the first newly shown child.
    const first = childEntries(rowValue(row))[row.posinset - 1];
    if (first) {
      focusPending.current = true;
      setActiveId(idOf([...row.segments, first[0]]));
    }
  };

  const rowValue = (row: Row) => rows[indexById.get(row.parentId ?? '') ?? -1]?.value;

  const expandable = (row: Row) =>
    row.kind === 'node' && (row.type === 'object' || row.type === 'array') && row.count > 0;

  const activate = (row: Row) => {
    if (row.kind === 'more') showMore(row);
    else if (expandable(row)) setOpen(row, !isOpen(row.id, row.depth));
  };

  const onKeyDown = (event: KeyboardEvent<HTMLDivElement>) => {
    if (!activeRow) return;
    const i = indexById.get(activeRow.id) ?? 0;
    const open = expandable(activeRow) && isOpen(activeRow.id, activeRow.depth);
    // In right-to-left text the tree is mirrored (children indent to the left),
    // so the arrow that points at the children opens and the other one closes.
    const rtl = readDir(event.currentTarget) === 'rtl';
    const key =
      rtl && event.key === 'ArrowLeft'
        ? 'ArrowRight'
        : rtl && event.key === 'ArrowRight'
          ? 'ArrowLeft'
          : event.key;
    switch (key) {
      case 'ArrowDown':
        moveTo(rows[i + 1]?.id);
        break;
      case 'ArrowUp':
        moveTo(rows[i - 1]?.id);
        break;
      case 'Home':
        moveTo(rows[0]?.id);
        break;
      case 'End':
        moveTo(rows[rows.length - 1]?.id);
        break;
      case 'ArrowRight':
        if (!expandable(activeRow)) return;
        if (open) moveTo(rows[i + 1]?.id);
        else setOpen(activeRow, true);
        break;
      case 'ArrowLeft':
        if (open) setOpen(activeRow, false);
        else if (activeRow.parentId !== null) moveTo(activeRow.parentId);
        else return;
        break;
      case 'Enter':
      case ' ':
        activate(activeRow);
        break;
      case '*': {
        // APG: expand every sibling of the focused node.
        const siblings = rows.filter((r) => r.parentId === activeRow.parentId && expandable(r));
        setOverrides((prev) => {
          const next = new Map(prev);
          for (const s of siblings) next.set(s.id, true);
          return next;
        });
        break;
      }
      default:
        return;
    }
    event.preventDefault();
  };

  // Pointer counterpart of Enter: a click on a row focuses it and expands,
  // collapses or shows more, as Enter does.
  const onClick = (event: MouseEvent<HTMLDivElement>) => {
    const id = (event.target as HTMLElement).closest<HTMLElement>('[data-row-id]')?.dataset.rowId;
    const row = id === undefined ? undefined : rows[indexById.get(id) ?? -1];
    if (!row) return;
    setActiveId(row.id);
    activate(row);
  };

  const collapseAll = () => {
    setBaseDepth(Math.min(1, defaultExpandDepth));
    setOverrides(new Map());
    setLimits(new Map());
    setQuery('');
    if (rows[0]) setActiveId(rows[0].id);
  };

  const summary = (row: Row) => {
    if (row.type === 'circular') return m.circular;
    if (row.type === 'array') return `[ ] ${m.items(row.count)}`;
    if (row.type === 'object') return `{ } ${m.keys(row.count)}`;
    if (row.type === 'string') {
      const s = row.value as string;
      return s.length > maxStringLength
        ? `"${s.slice(0, maxStringLength)}${m.truncated(s.length)}"`
        : `"${s}"`;
    }
    return String(row.value);
  };

  const selectedPath = selectedNode ? formatJsonPath(selectedNode.segments, rootPath) || '$' : '';

  return (
    <div ref={ref} {...rest} className={cx('mn-json-viewer', className)}>
      {
        <div className="mn-json-viewer__toolbar">
          {searchable ? (
            <SearchInput
              size="sm"
              className="mn-json-viewer__search"
              aria-label={m.search}
              placeholder={m.searchPlaceholder}
              value={query}
              onValueChange={setQuery}
            />
          ) : null}
          {searchable ? (
            <span className="mn-json-viewer__count" role="status">
              {needle ? m.matches(search.count, search.capped) : ''}
            </span>
          ) : null}
          <div className="mn-json-viewer__actions">
            <Button size="sm" variant="ghost" onClick={collapseAll}>
              {m.collapseAll}
            </Button>
            {actions}
          </div>
        </div>
      }
      {/* Focus lives on the treeitems (roving tabindex); the tree handles their keys and clicks. */}
      {/* eslint-disable-next-line jsx-a11y/interactive-supports-focus */}
      <div
        ref={treeRef}
        role="tree"
        aria-label={m.label}
        className="mn-json-viewer__tree"
        onKeyDown={onKeyDown}
        onClick={onClick}
      >
        {rows.map((row) => {
          const isActive = row.id === active;
          const style = { paddingInlineStart: `calc(${row.depth} * 1.25rem + 0.25rem)` };
          if (row.kind === 'more') {
            return (
              // Keys and clicks are handled on the tree (APG roving focus).
              <div
                key={row.id}
                id={domId(row.id)}
                data-row-id={row.id}
                role="treeitem"
                aria-level={row.depth + 1}
                aria-posinset={row.posinset}
                aria-setsize={row.setsize}
                aria-selected={isActive}
                tabIndex={isActive ? 0 : -1}
                className="mn-json-viewer__row mn-json-viewer__more"
                style={style}
                onFocus={() => setActiveId(row.id)}
              >
                {m.showMore(Math.min(maxChildren, row.remaining), row.remaining)}
              </div>
            );
          }
          const canOpen = expandable(row);
          const open = canOpen && isOpen(row.id, row.depth);
          const name =
            row.name === null ? null : typeof row.name === 'number' ? String(row.name) : row.name;
          return (
            // Keys and clicks are handled on the tree (APG roving focus).
            <div
              key={row.id}
              id={domId(row.id)}
              data-row-id={row.id}
              role="treeitem"
              aria-level={row.depth + 1}
              aria-posinset={row.posinset}
              aria-setsize={row.setsize}
              aria-selected={isActive}
              aria-expanded={canOpen ? open : undefined}
              aria-label={name !== null ? `${name}: ${summary(row)}` : summary(row)}
              tabIndex={isActive ? 0 : -1}
              data-type={row.type}
              data-match={search.matched.has(row.id) ? '' : undefined}
              className="mn-json-viewer__row"
              style={style}
              onFocus={() => setActiveId(row.id)}
            >
              <span
                className="mn-json-viewer__toggle"
                aria-hidden="true"
                data-open={open ? '' : undefined}
              >
                {canOpen ? <ChevronRightGlyph /> : null}
              </span>
              {name !== null ? (
                <>
                  <span
                    className="mn-json-viewer__key"
                    data-index={typeof row.name === 'number' ? '' : undefined}
                  >
                    <Highlight text={name} query={needle} />
                  </span>
                  <span className="mn-json-viewer__colon">: </span>
                </>
              ) : null}
              <span className="mn-json-viewer__value">
                {canOpen || row.type === 'circular' ? (
                  summary(row)
                ) : (
                  <Highlight text={summary(row)} query={needle} />
                )}
              </span>
            </div>
          );
        })}
      </div>
      {copyable && selectedNode ? (
        <div className="mn-json-viewer__footer">
          <span className="mn-json-viewer__path">
            <span className="mn-visually-hidden">{m.selectedPath}: </span>
            {selectedPath}
          </span>
          <span className="mn-json-viewer__copy">
            <CopyButton
              size="sm"
              variant="ghost"
              label={m.copyPath}
              value={() => selectedPath}
              onCopy={(text) => onCopy?.(text, 'path')}
            />
            <CopyButton
              size="sm"
              variant="ghost"
              label={m.copyValue}
              value={() => {
                const v = selectedNode.value;
                return typeof v === 'string' ? v : stringifyJsonValue(v);
              }}
              onCopy={(text) => onCopy?.(text, 'value')}
            />
          </span>
        </div>
      ) : null}
    </div>
  );
});
