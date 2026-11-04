'use client';

import {
  createContext,
  forwardRef,
  useCallback,
  useContext,
  useEffect,
  useId,
  useImperativeHandle,
  useMemo,
  useRef,
  useState,
  useSyncExternalStore,
  type CSSProperties,
  type HTMLAttributes,
  type KeyboardEvent as ReactKeyboardEvent,
  type PointerEvent as ReactPointerEvent,
  type ReactNode,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { ChevronRightGlyph, ColumnsGlyph } from '../../../internal/icons.js';
import {
  Button,
  IconButton,
  type IconButtonProps,
} from '../../../components/actions/button/index.js';
import {
  Sheet,
  SheetBody,
  SheetContent,
  SheetHeader,
  SheetTitle,
} from '../../../components/overlays/sheet/index.js';

export type SplitInboxPane = 'list' | 'detail' | 'context';

/* ------------------------------------------------------------------ */
/* Breakpoints                                                         */
/* ------------------------------------------------------------------ */

// Kept in sync with split-inbox-layout.css. Below 768px one pane shows at a
// time; below 1280px the context pane is a drawer instead of a column.
const SINGLE_QUERY = '(max-width: 767px)';
const DRAWER_QUERY = '(max-width: 1279px)';

function mediaStore(query: string) {
  const get = () =>
    typeof window !== 'undefined' && typeof window.matchMedia === 'function'
      ? window.matchMedia(query).matches
      : false;
  const subscribe = (notify: () => void) => {
    if (typeof window === 'undefined' || typeof window.matchMedia !== 'function') return () => {};
    const mql = window.matchMedia(query);
    mql.addEventListener?.('change', notify);
    return () => mql.removeEventListener?.('change', notify);
  };
  return { get, subscribe };
}
const single = mediaStore(SINGLE_QUERY);
const drawer = mediaStore(DRAWER_QUERY);
const serverFalse = () => false;

/* ------------------------------------------------------------------ */
/* Context for the pane toggles                                        */
/* ------------------------------------------------------------------ */

export interface SplitInboxLayoutState {
  /** `single` below 768px (one pane at a time), `split` otherwise. */
  layout: 'single' | 'split';
  /** `drawer` below 1280px (the context pane opens as a Sheet), `inline` otherwise. */
  contextMode: 'inline' | 'drawer';
  /** Whether the context pane is showing, as a column or as the drawer. */
  contextVisible: boolean;
  /** Shows or hides the context pane: the column on wide screens, the drawer otherwise. */
  setContextVisible: (visible: boolean) => void;
  toggleContext: () => void;
  /** The list pane is collapsed on wide screens (focus / full-page mode). */
  listCollapsed: boolean;
  setListCollapsed: (collapsed: boolean) => void;
  /** Moves focus into a pane: the element focused there last, or the pane itself. */
  focusPane: (pane: SplitInboxPane) => void;
  /** Element ids of the panes, for `aria-controls`. */
  ids: Record<SplitInboxPane, string>;
}

const LayoutContext = createContext<SplitInboxLayoutState | null>(null);

/**
 * Reads the nearest SplitInboxLayout from inside one of its panes, for
 * custom pane toggles (a focus-mode button, a details button). Returns
 * `null` outside a layout.
 */
export function useSplitInboxLayout(): SplitInboxLayoutState | null {
  return useContext(LayoutContext);
}

export interface SplitInboxContextToggleProps extends Omit<IconButtonProps, 'label' | 'icon'> {
  /** Label while the context pane is hidden. Default "Show details". */
  showLabel?: string;
  /** Label while it is showing. Default "Hide details". */
  hideLabel?: string;
  /** Replaces the default panel icon. Decorative. */
  icon?: ReactNode;
}

/**
 * The button that shows and hides the context pane, for the detail pane's
 * header. It toggles the column on wide screens and the drawer otherwise,
 * and exposes the state with `aria-expanded`.
 */
export const SplitInboxContextToggle = forwardRef<HTMLButtonElement, SplitInboxContextToggleProps>(
  function SplitInboxContextToggle(
    { showLabel = 'Show details', hideLabel = 'Hide details', icon, onClick, ...rest },
    ref,
  ) {
    const layout = useSplitInboxLayout();
    const visible = layout?.contextVisible ?? false;
    return (
      <IconButton
        ref={ref}
        {...rest}
        label={visible ? hideLabel : showLabel}
        icon={icon ?? <ColumnsGlyph />}
        aria-expanded={visible}
        aria-controls={visible && layout?.contextMode === 'inline' ? layout.ids.context : undefined}
        onClick={(event) => {
          onClick?.(event);
          if (!event.defaultPrevented) layout?.toggleContext();
        }}
      />
    );
  },
);

/* ------------------------------------------------------------------ */
/* Separator                                                           */
/* ------------------------------------------------------------------ */

interface SplitterProps {
  label: string;
  controls: string;
  size: number;
  min: number;
  max: number;
  step: number;
  collapsed: boolean;
  /** `start`: the pane is before the splitter (list); `end`: after it (context). */
  edge: 'start' | 'end';
  onSize: (size: number) => void;
  onToggle: () => void;
}

function isRtl(el: Element) {
  return typeof window !== 'undefined' && window.getComputedStyle(el).direction === 'rtl';
}

function Splitter({
  label,
  controls,
  size,
  min,
  max,
  step,
  collapsed,
  edge,
  onSize,
  onToggle,
}: SplitterProps) {
  const drag = useRef<{ x: number; size: number; sign: number } | null>(null);
  const clamp = (n: number) => Math.min(max, Math.max(min, Math.round(n)));

  const onKeyDown = (event: ReactKeyboardEvent<HTMLDivElement>) => {
    const big = event.shiftKey ? step * 4 : step;
    // Arrow keys move the splitter, so the pane grows towards the arrow for the
    // list (start edge) and away from it for the context pane (end edge).
    const sign = (edge === 'start' ? 1 : -1) * (isRtl(event.currentTarget) ? -1 : 1);
    let next: number | null = null;
    switch (event.key) {
      case 'ArrowRight':
        next = size + big * sign;
        break;
      case 'ArrowLeft':
        next = size - big * sign;
        break;
      case 'Home':
        next = min;
        break;
      case 'End':
        next = max;
        break;
      case 'Enter':
        event.preventDefault();
        onToggle();
        return;
      default:
        return;
    }
    event.preventDefault();
    if (collapsed) onToggle();
    onSize(clamp(next));
  };

  const onPointerDown = (event: ReactPointerEvent<HTMLDivElement>) => {
    if (event.button !== 0) return;
    event.preventDefault();
    if (collapsed) {
      onToggle();
      return;
    }
    const sign = (edge === 'start' ? 1 : -1) * (isRtl(event.currentTarget) ? -1 : 1);
    drag.current = { x: event.clientX, size, sign };
    event.currentTarget.setPointerCapture?.(event.pointerId);
    event.currentTarget.focus({ preventScroll: true });
  };
  const onPointerMove = (event: ReactPointerEvent<HTMLDivElement>) => {
    const d = drag.current;
    if (!d) return;
    onSize(clamp(d.size + (event.clientX - d.x) * d.sign));
  };
  const onPointerEnd = (event: ReactPointerEvent<HTMLDivElement>) => {
    if (!drag.current) return;
    drag.current = null;
    event.currentTarget.releasePointerCapture?.(event.pointerId);
  };

  return (
    // A focusable separator is the APG window-splitter widget; jsx-a11y treats
    // every separator as static.
    // eslint-disable-next-line jsx-a11y/no-noninteractive-element-interactions
    <div
      role="separator"
      // eslint-disable-next-line jsx-a11y/no-noninteractive-tabindex
      tabIndex={0}
      aria-orientation="vertical"
      aria-label={label}
      aria-controls={controls}
      aria-valuemin={min}
      aria-valuemax={max}
      aria-valuenow={collapsed ? min : size}
      data-collapsed={collapsed || undefined}
      data-edge={edge}
      className="mn-split-inbox__splitter"
      onKeyDown={onKeyDown}
      onPointerDown={onPointerDown}
      onPointerMove={onPointerMove}
      onPointerUp={onPointerEnd}
      onPointerCancel={onPointerEnd}
    />
  );
}

/* ------------------------------------------------------------------ */
/* Layout                                                              */
/* ------------------------------------------------------------------ */

function useControllable<T>(value: T | undefined, initial: T, onChange?: (next: T) => void) {
  const [inner, setInner] = useState(initial);
  const controlled = value !== undefined;
  const current = controlled ? value : inner;
  const set = useCallback(
    (next: T) => {
      if (!controlled) setInner(next);
      onChange?.(next);
    },
    [controlled, onChange],
  );
  return [current, set] as const;
}

export interface SplitInboxLayoutProps extends HTMLAttributes<HTMLDivElement> {
  /** The list pane: queue switcher, search, the conversation list. */
  list: ReactNode;
  /** The detail pane: the open item, or an EmptyState when nothing is selected. */
  detail: ReactNode;
  /** The optional context pane: requester, properties, activity. */
  context?: ReactNode;
  /** Name of the list pane landmark. Default "List". */
  listLabel?: string;
  /** Name of the detail pane landmark. Default "Detail". */
  detailLabel?: string;
  /** Name of the context pane landmark and the drawer title. Default "Details". */
  contextLabel?: string;

  /**
   * Below 768px only one pane shows: the list, or the detail when this is
   * true. Keep it in the URL (an open item id) so Back works.
   */
  detailOpen?: boolean;
  /** Renders a back button at the top of the detail pane below 768px. */
  onBack?: () => void;
  /** Label of that back button. Default "Back to list". */
  backLabel?: string;

  /** Width of the list pane in px (controlled). */
  listSize?: number;
  /** Initial list width in px. Default 360. */
  defaultListSize?: number;
  /** Called while the list is resized. Persist it to remember the width. */
  onListSizeChange?: (size: number) => void;
  /** Default 240. */
  minListSize?: number;
  /** Default 560. */
  maxListSize?: number;
  /** Hides the list on wide screens: focus or full-page mode (controlled). */
  listCollapsed?: boolean;
  defaultListCollapsed?: boolean;
  onListCollapsedChange?: (collapsed: boolean) => void;

  /** Width of the context column in px (controlled). */
  contextSize?: number;
  /** Initial context width in px. Default 340. */
  defaultContextSize?: number;
  onContextSizeChange?: (size: number) => void;
  /** Default 260. */
  minContextSize?: number;
  /** Default 520. */
  maxContextSize?: number;
  /**
   * Whether the context column shows on wide screens (controlled). Below
   * 1280px the context pane is a drawer with its own open state, closed at
   * first, so a remembered "open" never opens a modal on page load.
   */
  contextOpen?: boolean;
  /** Default true. */
  defaultContextOpen?: boolean;
  onContextOpenChange?: (open: boolean) => void;
  /** Accessible name of the drawer's close button. Default "Close details". */
  contextCloseLabel?: string;

  /** Show the keyboard and pointer splitters between panes. Default true. */
  resizable?: boolean;
  /** Pixels per arrow key press (Shift: four times). Default 16. */
  resizeStep?: number;
  /** Default "Resize list". */
  listResizeLabel?: string;
  /** Default "Resize details". */
  contextResizeLabel?: string;
}

/**
 * The one-page inbox: list, detail and an optional context pane. Panes are
 * resizable with the pointer and the keyboard and collapsible; below 768px
 * it becomes one pane with back navigation, and below 1280px the context pane
 * becomes a drawer. F6 and Shift+F6 move focus between panes. The product
 * supplies every pane's content and owns the URL state.
 */
export const SplitInboxLayout = forwardRef<HTMLDivElement, SplitInboxLayoutProps>(
  function SplitInboxLayout(
    {
      list,
      detail,
      context,
      listLabel = 'List',
      detailLabel = 'Detail',
      contextLabel = 'Details',
      detailOpen = false,
      onBack,
      backLabel = 'Back to list',
      listSize: listSizeProp,
      defaultListSize = 360,
      onListSizeChange,
      minListSize = 240,
      maxListSize = 560,
      listCollapsed: listCollapsedProp,
      defaultListCollapsed = false,
      onListCollapsedChange,
      contextSize: contextSizeProp,
      defaultContextSize = 340,
      onContextSizeChange,
      minContextSize = 260,
      maxContextSize = 520,
      contextOpen: contextOpenProp,
      defaultContextOpen = true,
      onContextOpenChange,
      contextCloseLabel = 'Close details',
      resizable = true,
      resizeStep = 16,
      listResizeLabel = 'Resize list',
      contextResizeLabel = 'Resize details',
      className,
      style,
      onKeyDown,
      ...rest
    },
    ref,
  ) {
    const rootRef = useRef<HTMLDivElement>(null);
    useImperativeHandle(ref, () => rootRef.current as HTMLDivElement);
    const base = useId();
    const ids = useMemo(
      () => ({
        list: `${base}-list`,
        detail: `${base}-detail`,
        context: `${base}-context`,
      }),
      [base],
    );

    const isSingle = useSyncExternalStore(single.subscribe, single.get, serverFalse);
    const isDrawer = useSyncExternalStore(drawer.subscribe, drawer.get, serverFalse);
    const layout: 'single' | 'split' = isSingle ? 'single' : 'split';
    const contextMode: 'inline' | 'drawer' = isDrawer ? 'drawer' : 'inline';

    const [listSize, setListSize] = useControllable(
      listSizeProp,
      defaultListSize,
      onListSizeChange,
    );
    const [listCollapsed, setListCollapsed] = useControllable(
      listCollapsedProp,
      defaultListCollapsed,
      onListCollapsedChange,
    );
    const [contextSize, setContextSize] = useControllable(
      contextSizeProp,
      defaultContextSize,
      onContextSizeChange,
    );
    const [contextOpen, setContextOpen] = useControllable(
      contextOpenProp,
      defaultContextOpen,
      onContextOpenChange,
    );
    const [drawerOpen, setDrawerOpen] = useState(false);
    const hasContext = context !== undefined && context !== null && context !== false;
    const contextVisible = hasContext && (contextMode === 'drawer' ? drawerOpen : contextOpen);

    // Close the drawer when the screen widens: the column takes over.
    useEffect(() => {
      if (contextMode === 'inline') setDrawerOpen(false);
    }, [contextMode]);

    // The element focused last in each pane, to return to it.
    const lastFocus = useRef<Partial<Record<SplitInboxPane, HTMLElement>>>({});
    const paneOf = (target: EventTarget | null): SplitInboxPane | null => {
      const el = target instanceof HTMLElement ? target : null;
      const pane = el?.closest('[data-mn-pane]');
      if (!pane || !rootRef.current?.contains(pane)) return null;
      return pane.getAttribute('data-mn-pane') as SplitInboxPane;
    };
    const onFocusCapture = (event: { target: EventTarget | null }) => {
      const pane = paneOf(event.target);
      const el = event.target as HTMLElement;
      if (pane && !el.hasAttribute('data-mn-pane')) lastFocus.current[pane] = el;
    };

    const focusPane = useCallback(
      (pane: SplitInboxPane) => {
        const target = rootRef.current?.ownerDocument.getElementById(ids[pane]);
        if (!target || !rootRef.current?.contains(target)) return;
        const last = lastFocus.current[pane];
        const el = last && last.isConnected && target.contains(last) ? last : target;
        el.focus({ preventScroll: el === target });
      },
      [ids],
    );

    // One pane at a time: move focus with the visible pane so keyboard and
    // screen-reader users land in the content that just appeared.
    const prevDetailOpen = useRef(detailOpen);
    useEffect(() => {
      if (prevDetailOpen.current === detailOpen) return;
      prevDetailOpen.current = detailOpen;
      if (layout !== 'single') return;
      focusPane(detailOpen ? 'detail' : 'list');
    }, [detailOpen, layout, focusPane]);

    // The drawer has no Radix trigger, so remember what opened it and return there.
    const drawerReturn = useRef<HTMLElement | null>(null);
    const openDrawer = useCallback((open: boolean) => {
      if (open && typeof document !== 'undefined') {
        drawerReturn.current = document.activeElement as HTMLElement | null;
      }
      setDrawerOpen(open);
    }, []);

    const setContextVisible = useCallback(
      (visible: boolean) => {
        if (contextMode === 'drawer') openDrawer(visible);
        else setContextOpen(visible);
      },
      [contextMode, openDrawer, setContextOpen],
    );

    const visiblePanes = (): SplitInboxPane[] => {
      if (layout === 'single') return [detailOpen ? 'detail' : 'list'];
      const panes: SplitInboxPane[] = [];
      if (!listCollapsed) panes.push('list');
      panes.push('detail');
      if (hasContext && contextMode === 'inline' && contextOpen) panes.push('context');
      return panes;
    };

    const handleKeyDown = (event: ReactKeyboardEvent<HTMLDivElement>) => {
      onKeyDown?.(event);
      if (event.defaultPrevented || event.key !== 'F6') return;
      const panes = visiblePanes();
      if (panes.length < 2) return;
      const current = paneOf(event.target);
      const at = current ? panes.indexOf(current) : -1;
      const next = panes[(at + (event.shiftKey ? -1 : 1) + panes.length) % panes.length];
      if (!next) return;
      event.preventDefault();
      focusPane(next);
    };

    const state = useMemo<SplitInboxLayoutState>(
      () => ({
        layout,
        contextMode,
        contextVisible,
        setContextVisible,
        toggleContext: () => setContextVisible(!contextVisible),
        listCollapsed,
        setListCollapsed,
        focusPane,
        ids,
      }),
      [
        layout,
        contextMode,
        contextVisible,
        setContextVisible,
        listCollapsed,
        setListCollapsed,
        focusPane,
        ids,
      ],
    );

    const showInlineContext = hasContext && contextMode === 'inline' && contextOpen;
    const vars = {
      '--_list': `${listSize}px`,
      '--_context': `${contextSize}px`,
      ...style,
    } as CSSProperties;

    return (
      <LayoutContext.Provider value={state}>
        {/* F6 pane navigation listens on the container for keys from any pane. */}
        {/* eslint-disable-next-line jsx-a11y/no-static-element-interactions */}
        <div
          ref={rootRef}
          {...rest}
          style={vars}
          data-layout={layout}
          data-detail-open={detailOpen || undefined}
          data-list-collapsed={listCollapsed || undefined}
          data-context-open={showInlineContext || undefined}
          className={cx('mn-split-inbox', className)}
          onKeyDown={handleKeyDown}
          onFocusCapture={onFocusCapture}
        >
          <aside
            id={ids.list}
            aria-label={listLabel}
            tabIndex={-1}
            data-mn-pane="list"
            className="mn-split-inbox__pane mn-split-inbox__list"
          >
            {list}
          </aside>
          {resizable ? (
            <Splitter
              label={listResizeLabel}
              controls={ids.list}
              size={listSize}
              min={minListSize}
              max={maxListSize}
              step={resizeStep}
              collapsed={listCollapsed}
              edge="start"
              onSize={setListSize}
              onToggle={() => setListCollapsed(!listCollapsed)}
            />
          ) : null}
          <section
            id={ids.detail}
            aria-label={detailLabel}
            tabIndex={-1}
            data-mn-pane="detail"
            className="mn-split-inbox__pane mn-split-inbox__detail"
          >
            {onBack ? (
              <div className="mn-split-inbox__back">
                <Button
                  variant="ghost"
                  size="sm"
                  leadingIcon={<ChevronRightGlyph className="mn-split-inbox__back-glyph" />}
                  onClick={onBack}
                >
                  {backLabel}
                </Button>
              </div>
            ) : null}
            {detail}
          </section>
          {showInlineContext ? (
            <>
              {resizable ? (
                <Splitter
                  label={contextResizeLabel}
                  controls={ids.context}
                  size={contextSize}
                  min={minContextSize}
                  max={maxContextSize}
                  step={resizeStep}
                  collapsed={false}
                  edge="end"
                  onSize={setContextSize}
                  onToggle={() => {
                    focusPane('detail');
                    setContextOpen(false);
                  }}
                />
              ) : null}
              <aside
                id={ids.context}
                aria-label={contextLabel}
                tabIndex={-1}
                data-mn-pane="context"
                className="mn-split-inbox__pane mn-split-inbox__context"
              >
                {context}
              </aside>
            </>
          ) : null}
          {hasContext && contextMode === 'drawer' ? (
            <Sheet open={drawerOpen} onOpenChange={openDrawer}>
              <SheetContent
                onCloseAutoFocus={(event) => {
                  const el = drawerReturn.current;
                  if (el && el.isConnected) {
                    event.preventDefault();
                    el.focus();
                  }
                }}
                side="right"
                size="sm"
                closeLabel={contextCloseLabel}
                aria-describedby={undefined}
                className="mn-split-inbox__drawer"
              >
                <SheetHeader>
                  <SheetTitle>{contextLabel}</SheetTitle>
                </SheetHeader>
                <SheetBody>{context}</SheetBody>
              </SheetContent>
            </Sheet>
          ) : null}
        </div>
      </LayoutContext.Provider>
    );
  },
);
