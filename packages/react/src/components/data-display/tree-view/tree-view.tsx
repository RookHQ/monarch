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
  type KeyboardEvent as ReactKeyboardEvent,
  type ReactNode,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { ChevronRightGlyph } from '../../../internal/icons.js';
import { Spinner } from '../../feedback/spinner/index.js';

/** One node of a TreeView. */
export interface TreeViewItem {
  /** Stable, unique id across the whole tree. */
  id: string;
  /** Plain-text label: the item's accessible name and the text typeahead matches. */
  label: string;
  /** Secondary line under the label, e.g. "Department · 12 people". Read as the description. */
  description?: ReactNode;
  /** Decorative icon before the label. */
  icon?: ReactNode;
  /** Content at the end of the row that is not an action, e.g. a Badge or a count. */
  meta?: ReactNode;
  /** Child items. An empty array is a parent with no children; `undefined` is a leaf unless `hasChildren`. */
  children?: readonly TreeViewItem[] | undefined;
  /** The item has children that are not loaded yet; expanding it calls `onLoadChildren`. */
  hasChildren?: boolean;
  /** Skipped by arrow keys and cannot be selected or activated. */
  disabled?: boolean;
}

export interface TreeViewItemState {
  level: number;
  expanded: boolean;
  selected: boolean;
  focused: boolean;
  current: boolean;
}

export interface TreeViewMessages {
  /** Announced while children load. */
  loading: string;
  /** Announced when `onLoadChildren` rejects. */
  loadError: (label: string) => string;
}

export const defaultTreeViewMessages: TreeViewMessages = {
  loading: 'Loading',
  loadError: (label) => `Could not load the items in ${label}.`,
};

export interface TreeViewProps extends Omit<
  HTMLAttributes<HTMLDivElement>,
  'children' | 'onSelect' | 'defaultValue'
> {
  /** The top-level items. */
  items: readonly TreeViewItem[];
  /** Accessible name of the tree. Use `aria-labelledby` instead when a visible heading names it. */
  label?: string;
  /** Expanded item ids (controlled). */
  expanded?: readonly string[];
  /** Initially expanded ids (uncontrolled). `'all'` expands every parent. Default: none. */
  defaultExpanded?: readonly string[] | 'all';
  onExpandedChange?: ((expanded: string[]) => void) | undefined;
  /** `none` for navigation-only trees (outlines), `single` (default) or `multiple`. */
  selectionMode?: 'none' | 'single' | 'multiple';
  /** Selected item ids (controlled). */
  selected?: readonly string[];
  /** Initially selected ids (uncontrolled). */
  defaultSelected?: readonly string[];
  onSelectedChange?: ((selected: string[]) => void) | undefined;
  /** Called on Enter and on click: open the record, scroll to the heading. */
  onActivate?: ((item: TreeViewItem) => void) | undefined;
  /** Id of the item that matches the current page or position (`aria-current`), e.g. the heading in view. */
  current?: string | undefined;
  /**
   * Loads children for items with `hasChildren` and no `children` yet. The
   * item shows a spinner until the promise settles; then update `items`.
   */
  onLoadChildren?: ((item: TreeViewItem) => Promise<unknown>) | undefined;
  /** Replaces the label and description of a row. Keep it non-interactive. */
  renderItem?: ((item: TreeViewItem, state: TreeViewItemState) => ReactNode) | undefined;
  /**
   * Buttons or a menu at the end of a row. Pass `tabIndex` to every control
   * you render: only the focused row's actions are in the tab order, so Tab
   * moves from the row to its actions and then out of the tree.
   */
  renderActions?:
    | ((item: TreeViewItem, context: { tabIndex: 0 | -1; state: TreeViewItemState }) => ReactNode)
    | undefined;
  /** `default` rows, or `outline` with indentation guides and compact rows for document outlines. */
  variant?: 'default' | 'outline';
  messages?: Partial<TreeViewMessages>;
}

interface Flat {
  item: TreeViewItem;
  level: number;
  parentId: string | null;
  posinset: number;
  setsize: number;
  /** Position in the whole tree, used for DOM ids (item ids may contain any character). */
  n: number;
}

const isParent = (item: TreeViewItem) => item.children !== undefined || item.hasChildren === true;

function collectParents(items: readonly TreeViewItem[], out: string[] = []): string[] {
  for (const item of items) {
    if (isParent(item)) out.push(item.id);
    if (item.children) collectParents(item.children, out);
  }
  return out;
}

function useControlled<T>(value: T | undefined, initial: () => T, onChange?: (v: T) => void) {
  const [inner, setInner] = useState<T>(initial);
  const current = value ?? inner;
  const set = useCallback(
    (next: T) => {
      if (value === undefined) setInner(next);
      onChange?.(next);
    },
    [value, onChange],
  );
  return [current, set] as const;
}

/**
 * A hierarchy people can browse and pick from: departments, goals and key
 * results, a document outline. Implements the WAI-ARIA tree pattern: one tab
 * stop, arrow keys to move, expand and collapse, typeahead, single or
 * multiple selection and lazily loaded children.
 */
export const TreeView = forwardRef<HTMLDivElement, TreeViewProps>(function TreeView(
  {
    items,
    label,
    expanded: expandedProp,
    defaultExpanded,
    onExpandedChange,
    selectionMode = 'single',
    selected: selectedProp,
    defaultSelected,
    onSelectedChange,
    onActivate,
    current,
    onLoadChildren,
    renderItem,
    renderActions,
    variant = 'default',
    messages,
    className,
    onKeyDown,
    ...rest
  },
  ref,
) {
  const m = { ...defaultTreeViewMessages, ...messages };
  const baseId = useId();
  const [expandedList, setExpandedList] = useControlled<readonly string[]>(
    expandedProp,
    () => (defaultExpanded === 'all' ? collectParents(items) : (defaultExpanded ?? [])),
    onExpandedChange as ((v: readonly string[]) => void) | undefined,
  );
  const [selectedList, setSelectedList] = useControlled<readonly string[]>(
    selectedProp,
    () => defaultSelected ?? [],
    onSelectedChange as ((v: readonly string[]) => void) | undefined,
  );
  const expandedSet = useMemo(() => new Set(expandedList), [expandedList]);
  const selectedSet = useMemo(() => new Set(selectedList), [selectedList]);
  const [loading, setLoading] = useState<ReadonlySet<string>>(() => new Set());
  const [announcement, setAnnouncement] = useState('');

  // Every item by id, with its parent, for Left-arrow and focus repair.
  const index = useMemo(() => {
    const map = new Map<string, Flat>();
    const walk = (list: readonly TreeViewItem[], level: number, parentId: string | null) => {
      list.forEach((item, i) => {
        map.set(item.id, {
          item,
          level,
          parentId,
          posinset: i + 1,
          setsize: list.length,
          n: map.size,
        });
        if (item.children) walk(item.children, level + 1, item.id);
      });
    };
    walk(items, 1, null);
    return map;
  }, [items]);

  // Visible items in reading order.
  const visible = useMemo(() => {
    const out: Flat[] = [];
    const walk = (list: readonly TreeViewItem[]) => {
      for (const item of list) {
        const flat = index.get(item.id);
        if (flat) out.push(flat);
        if (item.children && expandedSet.has(item.id)) walk(item.children);
      }
    };
    walk(items);
    return out;
  }, [items, index, expandedSet]);

  const [focusedId, setFocusedId] = useState<string | null>(null);
  // The tab stop: the focused item if still visible, else its nearest visible ancestor, else a selected or the first item.
  const tabStop = useMemo(() => {
    const visibleIds = new Set(visible.map((f) => f.item.id));
    let id = focusedId;
    while (id && !visibleIds.has(id)) id = index.get(id)?.parentId ?? null;
    if (id) return id;
    const sel = visible.find((f) => selectedSet.has(f.item.id) && !f.item.disabled);
    return (sel ?? visible.find((f) => !f.item.disabled) ?? visible[0])?.item.id ?? null;
  }, [focusedId, visible, index, selectedSet]);

  const nodes = useRef(new Map<string, HTMLElement | null>());
  const focusItem = (id: string) => {
    setFocusedId(id);
    nodes.current.get(id)?.focus();
  };
  // When focus was inside a branch that collapsed, the row that took the tab stop gets focus.
  const hadFocus = useRef(false);
  useEffect(() => {
    if (!hadFocus.current || !tabStop) return;
    const el = nodes.current.get(tabStop);
    const active = typeof document === 'undefined' ? null : document.activeElement;
    if (el && (active === null || active === document.body)) el.focus();
  }, [tabStop]);

  const setExpanded = (id: string, open: boolean) => {
    if (open === expandedSet.has(id)) return;
    setExpandedList(open ? [...expandedList, id] : expandedList.filter((x) => x !== id));
  };

  const expand = (item: TreeViewItem) => {
    if (!isParent(item) || item.disabled) return;
    if (item.children === undefined && item.hasChildren && onLoadChildren) {
      if (loading.has(item.id)) return;
      setLoading((s) => new Set(s).add(item.id));
      setAnnouncement(m.loading);
      onLoadChildren(item).then(
        () => {
          setLoading((s) => {
            const next = new Set(s);
            next.delete(item.id);
            return next;
          });
          setAnnouncement('');
        },
        () => {
          setLoading((s) => {
            const next = new Set(s);
            next.delete(item.id);
            return next;
          });
          setExpandedList(expandedListRef.current.filter((x) => x !== item.id));
          setAnnouncement(m.loadError(item.label));
        },
      );
    }
    setExpanded(item.id, true);
  };
  const expandedListRef = useRef(expandedList);
  expandedListRef.current = expandedList;

  const select = (item: TreeViewItem, mode: 'replace' | 'toggle' | 'add') => {
    if (selectionMode === 'none' || item.disabled) return;
    if (selectionMode === 'single') {
      if (!(selectedSet.size === 1 && selectedSet.has(item.id))) setSelectedList([item.id]);
      return;
    }
    if (mode === 'replace') setSelectedList([item.id]);
    else if (mode === 'add') {
      if (!selectedSet.has(item.id)) setSelectedList([...selectedList, item.id]);
    } else
      setSelectedList(
        selectedSet.has(item.id)
          ? selectedList.filter((x) => x !== item.id)
          : [...selectedList, item.id],
      );
  };

  const typeahead = useRef({ text: '', at: 0 });

  const handleKeyDown = (event: ReactKeyboardEvent<HTMLDivElement>) => {
    onKeyDown?.(event);
    if (event.defaultPrevented) return;
    const target = event.target as HTMLElement;
    // Keys pressed on an action control belong to that control.
    if (target.getAttribute('role') !== 'treeitem') return;
    const id = target.dataset.id;
    const flat = id ? index.get(id) : undefined;
    if (!flat) return;
    const { item } = flat;
    const enabled = visible.filter((f) => !f.item.disabled);
    const pos = enabled.findIndex((f) => f.item.id === item.id);
    const go = (f: Flat | undefined, extend = false) => {
      if (!f) return;
      focusItem(f.item.id);
      if (extend && selectionMode === 'multiple') select(f.item, 'add');
    };
    const multi = selectionMode === 'multiple';
    let handled = true;
    switch (event.key) {
      case 'ArrowDown':
        go(enabled[pos + 1], event.shiftKey && multi);
        break;
      case 'ArrowUp':
        go(enabled[pos - 1], event.shiftKey && multi);
        break;
      case 'Home':
        go(enabled[0]);
        break;
      case 'End':
        go(enabled[enabled.length - 1]);
        break;
      case 'ArrowRight':
        if (!isParent(item)) break;
        if (!expandedSet.has(item.id)) expand(item);
        else {
          const first = item.children?.find((c) => !c.disabled);
          if (first) focusItem(first.id);
        }
        break;
      case 'ArrowLeft':
        if (isParent(item) && expandedSet.has(item.id)) setExpanded(item.id, false);
        else if (flat.parentId) focusItem(flat.parentId);
        break;
      case 'Enter':
        if (item.disabled) break;
        if (selectionMode !== 'none') select(item, multi ? 'toggle' : 'replace');
        onActivate?.(item);
        break;
      case ' ':
        if (item.disabled) break;
        if (selectionMode === 'none') onActivate?.(item);
        else select(item, multi ? 'toggle' : 'replace');
        break;
      case '*': {
        const siblings = flat.parentId ? (index.get(flat.parentId)?.item.children ?? []) : items;
        const toOpen = siblings.filter((s) => isParent(s) && !s.disabled && !expandedSet.has(s.id));
        for (const s of toOpen) if (s.children === undefined) expand(s);
        if (toOpen.length) setExpandedList([...expandedList, ...toOpen.map((s) => s.id)]);
        break;
      }
      default:
        if (multi && (event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 'a') {
          setSelectedList(enabled.map((f) => f.item.id));
          break;
        }
        if (event.key.length === 1 && !event.ctrlKey && !event.metaKey && !event.altKey) {
          const now = Date.now();
          const t = typeahead.current;
          t.text = now - t.at > 500 ? event.key : t.text + event.key;
          t.at = now;
          const typed = t.text.toLocaleLowerCase();
          // The same letter pressed again cycles through matches ("f", "f" → next "F…").
          const repeated = [...typed].every((ch) => ch === typed[0]);
          const q = repeated ? (typed[0] ?? '') : typed;
          const order = [...enabled.slice(pos + 1), ...enabled.slice(0, pos + 1)];
          // A longer string may still match the current item.
          const pool = q.length > 1 ? [enabled[pos] as Flat, ...order] : order;
          const hit = pool.find((f) => f.item.label.toLocaleLowerCase().startsWith(q));
          if (hit) focusItem(hit.item.id);
          break;
        }
        handled = false;
    }
    if (handled) {
      event.preventDefault();
      event.stopPropagation();
    }
  };

  const renderLevel = (list: readonly TreeViewItem[]): ReactNode =>
    list.map((item) => {
      const flat = index.get(item.id) as Flat;
      const parent = isParent(item);
      const open = parent && expandedSet.has(item.id);
      const busy = loading.has(item.id);
      const state: TreeViewItemState = {
        level: flat.level,
        expanded: open,
        selected: selectedSet.has(item.id),
        focused: tabStop === item.id,
        current: current === item.id,
      };
      const domId = `${baseId}-${flat.n}`;
      const actions = renderActions?.(item, { tabIndex: state.focused ? 0 : -1, state });
      return (
        <div
          key={item.id}
          ref={(el) => {
            nodes.current.set(item.id, el);
          }}
          role="treeitem"
          id={domId}
          data-id={item.id}
          tabIndex={state.focused ? 0 : -1}
          aria-level={flat.level}
          aria-setsize={flat.setsize}
          aria-posinset={flat.posinset}
          aria-expanded={parent ? open : undefined}
          aria-selected={selectionMode === 'none' ? undefined : state.selected}
          aria-disabled={item.disabled || undefined}
          aria-current={state.current ? 'true' : undefined}
          aria-busy={busy || undefined}
          aria-labelledby={`${domId}-label`}
          aria-describedby={item.description && !renderItem ? `${domId}-desc` : undefined}
          className="mn-tree-view__item"
          data-selected={state.selected || undefined}
          data-current={state.current || undefined}
          data-disabled={item.disabled || undefined}
          onFocus={(e) => {
            if (e.target === e.currentTarget) setFocusedId(item.id);
          }}
        >
          {/* Pointer target for the row; keyboard handling lives on the tree. */}
          {/* eslint-disable-next-line jsx-a11y/click-events-have-key-events, jsx-a11y/no-static-element-interactions */}
          <div
            className="mn-tree-view__row"
            style={{ ['--mn-tree-view-level' as string]: flat.level - 1 }}
            onClick={(e) => {
              if ((e.target as HTMLElement).closest('.mn-tree-view__actions')) return;
              focusItem(item.id);
              if (item.disabled) return;
              if ((e.target as HTMLElement).closest('.mn-tree-view__toggle')) {
                if (open) setExpanded(item.id, false);
                else expand(item);
                return;
              }
              select(
                item,
                selectionMode === 'multiple' && (e.metaKey || e.ctrlKey) ? 'toggle' : 'replace',
              );
              onActivate?.(item);
            }}
          >
            <span className="mn-tree-view__toggle" aria-hidden="true" data-open={open || undefined}>
              {busy ? <Spinner size="sm" /> : parent ? <ChevronRightGlyph /> : null}
            </span>
            {item.icon ? (
              <span className="mn-tree-view__icon" aria-hidden="true">
                {item.icon}
              </span>
            ) : null}
            <span className="mn-tree-view__content" id={renderItem ? `${domId}-label` : undefined}>
              {renderItem ? (
                renderItem(item, state)
              ) : (
                <>
                  <span className="mn-tree-view__label" id={`${domId}-label`}>
                    {item.label}
                  </span>
                  {item.description ? (
                    <span className="mn-tree-view__description" id={`${domId}-desc`}>
                      {item.description}
                    </span>
                  ) : null}
                </>
              )}
            </span>
            {item.meta ? <span className="mn-tree-view__meta">{item.meta}</span> : null}
            {actions ? <span className="mn-tree-view__actions">{actions}</span> : null}
          </div>
          {open && item.children?.length ? (
            <div role="group" className="mn-tree-view__group">
              {renderLevel(item.children)}
            </div>
          ) : null}
        </div>
      );
    });

  return (
    <>
      {/* Focus lives on the items (roving tabindex), not on the tree itself. */}
      {/* eslint-disable-next-line jsx-a11y/interactive-supports-focus */}
      <div
        ref={ref}
        role="tree"
        aria-label={label}
        aria-multiselectable={selectionMode === 'multiple' || undefined}
        {...rest}
        data-variant={variant}
        className={cx('mn-tree-view', className)}
        onKeyDown={handleKeyDown}
        onFocus={() => {
          hadFocus.current = true;
        }}
        onBlur={(e) => {
          if (!e.currentTarget.contains(e.relatedTarget as Node | null)) hadFocus.current = false;
        }}
      >
        {renderLevel(items)}
      </div>
      <span role="status" aria-live="polite" className="mn-visually-hidden">
        {announcement}
      </span>
    </>
  );
});
