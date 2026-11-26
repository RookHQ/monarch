'use client';

import {
  Children,
  createContext,
  forwardRef,
  isValidElement,
  useCallback,
  useContext,
  useId,
  useMemo,
  useRef,
  useState,
  type CSSProperties,
  type ForwardedRef,
  type HTMLAttributes,
  type KeyboardEvent,
  type PointerEvent as ReactPointerEvent,
  type ReactElement,
  type ReactNode,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { useDirection } from '../../../internal/direction.js';

/** Root class, declared before any other `mn-` string so dist/manifest.json reads it as the root. */
const ROOT = 'mn-resizable-panels';

/** Labels read to screen-reader users. Override them to translate. */
export interface ResizablePanelsMessages {
  /** Accessible name of each separator. Default "Resize panels". */
  handleLabel: string;
}

const DEFAULT_MESSAGES: ResizablePanelsMessages = { handleLabel: 'Resize panels' };

export interface ResizablePanelProps extends HTMLAttributes<HTMLDivElement> {
  /** Starting size in percent of the group. Panels without one share what is left. */
  defaultSize?: number | undefined;
  /** Smallest size in percent. Default 10. */
  minSize?: number | undefined;
  /** Largest size in percent. Default 100. */
  maxSize?: number | undefined;
  /**
   * Can be collapsed: press Enter on the separator after it, or drag below
   * half its minimum size. Enter again restores the previous size.
   */
  collapsible?: boolean | undefined;
  /** Size in percent when collapsed. Default 0. */
  collapsedSize?: number | undefined;
}

export interface ResizablePanelsProps extends Omit<HTMLAttributes<HTMLDivElement>, 'dir'> {
  /** `horizontal` (default): panels side by side. `vertical`: stacked. */
  direction?: 'horizontal' | 'vertical' | undefined;
  /**
   * Starting sizes in percent, one per panel: pass what `onLayout` saved to
   * restore a layout. Overrides each panel's `defaultSize`.
   */
  defaultSizes?: readonly number[] | undefined;
  /**
   * Called with the sizes (percent, one per panel) when a resize finishes:
   * pointer released, or each keyboard step. Save them to restore the layout.
   */
  onLayout?: ((sizes: number[]) => void) | undefined;
  /** Percent moved by one arrow-key press. Default 5. */
  keyboardStep?: number | undefined;
  /**
   * Below 768px (`--mn-breakpoint-md`), stack the panels vertically at their
   * natural height and hide the separators. Default false.
   */
  stackOnNarrow?: boolean | undefined;
  /** Text direction; Left and Right swap in `rtl`. Default: read from the page. */
  dir?: 'ltr' | 'rtl' | undefined;
  /** Interface text, for translation. */
  messages?: Partial<ResizablePanelsMessages> | undefined;
}

export interface ResizableHandleProps extends HTMLAttributes<HTMLDivElement> {
  /** Show a visible grip in the middle of the separator. */
  withHandle?: boolean | undefined;
  /** Accessible name for this separator, instead of `messages.handleLabel`. */
  label?: string | undefined;
  /** Turn resizing off for this separator. */
  disabled?: boolean | undefined;
}

interface PanelSpec {
  min: number;
  max: number;
  collapsible: boolean;
  collapsed: number;
  id: string;
}

interface GroupContext {
  direction: 'horizontal' | 'vertical';
  sizes: number[];
  specs: PanelSpec[];
  messages: ResizablePanelsMessages;
  resize: (handle: number, to: number, commit: boolean) => void;
  toggle: (handle: number) => void;
  commit: () => void;
  keyboardStep: number;
  rtl: () => boolean;
  groupSize: () => number;
}

const Group = createContext<GroupContext | null>(null);
const Index = createContext(-1);

const clamp = (v: number, lo: number, hi: number) => Math.min(hi, Math.max(lo, v));
const round = (v: number) => Math.round(v * 100) / 100;

function useGroup(part: string): GroupContext {
  const ctx = useContext(Group);
  if (!ctx) throw new Error(`${part} must be a direct child of ResizablePanels.`);
  return ctx;
}

/** Starting sizes: saved sizes, else each panel's default, the rest shared; normalised to 100. */
function initialSizes(defaults: (number | undefined)[], saved: readonly number[] | undefined) {
  if (
    saved &&
    saved.length === defaults.length &&
    saved.every((n) => Number.isFinite(n) && n >= 0)
  ) {
    const total = saved.reduce((a, b) => a + b, 0) || 1;
    return saved.map((n) => (n / total) * 100);
  }
  const given = defaults.reduce<number>((a, b) => a + (b ?? 0), 0);
  const open = defaults.filter((d) => d === undefined).length;
  const share = open > 0 ? Math.max(0, 100 - given) / open : 0;
  const raw = defaults.map((d) => d ?? share);
  const total = raw.reduce((a, b) => a + b, 0) || 1;
  return raw.map((n) => (n / total) * 100);
}

/**
 * Split panes the user resizes by dragging or from the keyboard. Put
 * `ResizablePanel`s and `ResizableHandle`s directly inside, alternating.
 * Sizes are percentages; save them from `onLayout` and pass them back as
 * `defaultSizes` to restore a layout.
 */
export const ResizablePanels = forwardRef<HTMLDivElement, ResizablePanelsProps>(
  function ResizablePanels(
    {
      direction = 'horizontal',
      defaultSizes,
      onLayout,
      keyboardStep = 5,
      stackOnNarrow = false,
      dir,
      messages: messagesProp,
      className,
      children,
      ...rest
    },
    forwarded: ForwardedRef<HTMLDivElement>,
  ) {
    const readDir = useDirection(dir);
    const baseId = useId();
    const root = useRef<HTMLDivElement | null>(null);
    const messages = useMemo(() => ({ ...DEFAULT_MESSAGES, ...messagesProp }), [messagesProp]);

    // Panels and handles in order; each gets its index through context.
    const items = Children.toArray(children).filter(isValidElement) as ReactElement[];
    const panelProps = items
      .filter((c) => c.type === ResizablePanel)
      .map((c) => c.props as ResizablePanelProps);
    const specs: PanelSpec[] = panelProps.map((p, i) => ({
      min: clamp(p.minSize ?? 10, 0, 100),
      max: clamp(p.maxSize ?? 100, 0, 100),
      collapsible: p.collapsible ?? false,
      collapsed: clamp(p.collapsedSize ?? 0, 0, 100),
      id: p.id ?? `mn-resizable-${baseId}-panel-${i}`,
    }));

    const [sizes, setSizes] = useState<number[]>(() =>
      initialSizes(
        panelProps.map((p) => p.defaultSize),
        defaultSizes,
      ),
    );
    // Panels added or removed after mount: start over from their defaults.
    const shown =
      sizes.length === specs.length
        ? sizes
        : initialSizes(
            panelProps.map((p) => p.defaultSize),
            undefined,
          );
    const sizesRef = useRef(shown);
    sizesRef.current = shown;
    const specsRef = useRef(specs);
    specsRef.current = specs;
    const beforeCollapse = useRef(new Map<number, number>());
    const onLayoutRef = useRef(onLayout);
    onLayoutRef.current = onLayout;

    const commit = useCallback(() => {
      onLayoutRef.current?.(sizesRef.current.map(round));
    }, []);

    const apply = useCallback((next: number[], save: boolean) => {
      sizesRef.current = next;
      setSizes(next);
      if (save) onLayoutRef.current?.(next.map(round));
    }, []);

    /** Moves handle `h` so the panel before it is `to` percent, within both panels' limits. */
    const resize = useCallback(
      (h: number, to: number, save: boolean) => {
        const current = sizesRef.current;
        const a = specsRef.current[h];
        const b = specsRef.current[h + 1];
        const sa = current[h];
        const sb = current[h + 1];
        if (!a || !b || sa === undefined || sb === undefined) return;
        const pair = sa + sb;
        let target = to;
        if (a.collapsible && target < a.min / 2) target = a.collapsed;
        else target = clamp(target, a.min, a.max);
        let other = pair - target;
        if (b.collapsible && other < b.min / 2) other = b.collapsed;
        else other = clamp(other, b.min, b.max);
        target = pair - other;
        if (!(a.collapsible && target === a.collapsed))
          target = clamp(target, Math.min(a.min, pair), a.max);
        const next = current.slice();
        next[h] = target;
        next[h + 1] = pair - target;
        if (next[h] === sa && !save) return;
        apply(next, save);
      },
      [apply],
    );

    const toggle = useCallback(
      (h: number) => {
        const a = specsRef.current[h];
        const size = sizesRef.current[h];
        if (!a?.collapsible || size === undefined) return;
        if (size <= a.collapsed) {
          const restore = beforeCollapse.current.get(h) ?? a.min;
          resize(h, Math.max(restore, a.min), true);
        } else {
          beforeCollapse.current.set(h, size);
          resize(h, a.collapsed, true);
        }
      },
      [resize],
    );

    const rtl = useCallback(() => readDir(root.current) === 'rtl', [readDir]);

    const groupSize = useCallback(() => {
      const el = root.current;
      if (!el) return 0;
      // The panels' share of the group, without the separators.
      let total = 0;
      for (const child of Array.from(el.children)) {
        if (!child.classList.contains('mn-resizable-panels__panel')) continue;
        const r = child.getBoundingClientRect();
        total += direction === 'horizontal' ? r.width : r.height;
      }
      if (total > 0) return total;
      const r = el.getBoundingClientRect();
      return direction === 'horizontal' ? r.width : r.height;
    }, [direction]);

    const ctx: GroupContext = {
      direction,
      sizes: shown,
      specs,
      messages,
      resize,
      toggle,
      commit,
      keyboardStep,
      rtl,
      groupSize,
    };

    let panel = 0;
    let handle = 0;
    const content: ReactNode[] = items.map((child) => {
      if (child.type === ResizablePanel)
        return (
          <Index.Provider key={child.key ?? `p${panel}`} value={panel++}>
            {child}
          </Index.Provider>
        );
      if (child.type === ResizableHandle)
        return (
          <Index.Provider key={child.key ?? `h${handle}`} value={handle++}>
            {child}
          </Index.Provider>
        );
      return child;
    });

    return (
      <Group.Provider value={ctx}>
        <div
          ref={(node) => {
            root.current = node;
            if (typeof forwarded === 'function') forwarded(node);
            else if (forwarded) forwarded.current = node;
          }}
          {...rest}
          dir={dir}
          className={cx(ROOT, className)}
          data-direction={direction}
          data-stack-narrow={stackOnNarrow ? '' : undefined}
        >
          {content}
        </div>
      </Group.Provider>
    );
  },
);

/** The same component under the name shadcn/ui and react-resizable-panels use. */
export const ResizablePanelGroup = ResizablePanels;

/** One pane of a `ResizablePanels` group. Its size is a percentage of the group. */
export const ResizablePanel = forwardRef<HTMLDivElement, ResizablePanelProps>(
  function ResizablePanel(
    {
      defaultSize: _defaultSize,
      minSize: _minSize,
      maxSize: _maxSize,
      collapsible: _collapsible,
      collapsedSize: _collapsedSize,
      className,
      style,
      id: _id,
      ...rest
    },
    ref,
  ) {
    const ctx = useGroup('ResizablePanel');
    const i = useContext(Index);
    const spec = ctx.specs[i];
    const size = ctx.sizes[i] ?? 0;
    const collapsed = spec ? spec.collapsible && size <= spec.collapsed : false;
    return (
      <div
        ref={ref}
        {...rest}
        id={spec?.id}
        className={cx('mn-resizable-panels__panel', className)}
        style={{ '--mn-resizable-panels-size': round(size), ...style } as CSSProperties}
        data-collapsed={collapsed ? '' : undefined}
        data-size={round(size)}
        // A panel collapsed to a rail (`collapsedSize` above 0) stays on screen.
        {...(collapsed && size <= 0 ? { hidden: true } : null)}
      />
    );
  },
);

/** Pixels a pointer must travel before a press becomes a drag. */
const DRAG_THRESHOLD = 2;

/**
 * The separator between two panels: drag it, or focus it and use the arrow
 * keys. It exposes `role="separator"` with the size of the panel before it.
 */
export const ResizableHandle = forwardRef<HTMLDivElement, ResizableHandleProps>(
  function ResizableHandle(
    {
      withHandle = false,
      label,
      disabled = false,
      className,
      onKeyDown,
      onPointerDown,
      onPointerMove,
      onPointerUp,
      onPointerCancel,
      ...rest
    },
    ref,
  ) {
    const ctx = useGroup('ResizableHandle');
    const h = useContext(Index);
    const before = ctx.specs[h];
    const size = ctx.sizes[h] ?? 0;
    const [dragging, setDragging] = useState(false);
    const drag = useRef<{
      pointerId: number;
      start: number;
      size: number;
      px: number;
      moved: boolean;
    } | null>(null);
    const horizontal = ctx.direction === 'horizontal';

    const onKey = (e: KeyboardEvent<HTMLDivElement>) => {
      onKeyDown?.(e);
      if (e.defaultPrevented || disabled || !before) return;
      const step = ctx.keyboardStep;
      const rtl = ctx.rtl();
      let to: number | null = null;
      if (horizontal && e.key === 'ArrowLeft') to = size + (rtl ? step : -step);
      else if (horizontal && e.key === 'ArrowRight') to = size + (rtl ? -step : step);
      else if (!horizontal && e.key === 'ArrowUp') to = size - step;
      else if (!horizontal && e.key === 'ArrowDown') to = size + step;
      else if (e.key === 'Home') to = before.min;
      else if (e.key === 'End') to = before.max;
      else if (e.key === 'Enter' && before.collapsible) {
        e.preventDefault();
        ctx.toggle(h);
        return;
      }
      if (to === null) return;
      e.preventDefault();
      ctx.resize(h, to, true);
    };

    const onDown = (e: ReactPointerEvent<HTMLDivElement>) => {
      onPointerDown?.(e);
      if (e.defaultPrevented || disabled || e.button !== 0) return;
      e.preventDefault();
      e.currentTarget.setPointerCapture?.(e.pointerId);
      e.currentTarget.focus({ preventScroll: true });
      drag.current = {
        pointerId: e.pointerId,
        start: horizontal ? e.clientX : e.clientY,
        size,
        px: ctx.groupSize(),
        moved: false,
      };
    };

    const onMove = (e: ReactPointerEvent<HTMLDivElement>) => {
      onPointerMove?.(e);
      const d = drag.current;
      if (!d || d.pointerId !== e.pointerId || d.px <= 0) return;
      let delta = (horizontal ? e.clientX : e.clientY) - d.start;
      if (!d.moved && Math.abs(delta) < DRAG_THRESHOLD) return;
      if (!d.moved) {
        d.moved = true;
        setDragging(true);
      }
      if (horizontal && ctx.rtl()) delta = -delta;
      ctx.resize(h, d.size + (delta / d.px) * 100, false);
    };

    const end = (e: ReactPointerEvent<HTMLDivElement>) => {
      const d = drag.current;
      if (!d || d.pointerId !== e.pointerId) return;
      drag.current = null;
      setDragging(false);
      if (d.moved) ctx.commit();
    };

    return (
      // A focusable window splitter (WAI-ARIA APG): the separator role is interactive when focusable.
      // eslint-disable-next-line jsx-a11y/no-noninteractive-element-interactions
      <div
        ref={ref}
        role="separator"
        tabIndex={disabled ? -1 : 0}
        aria-label={label ?? ctx.messages.handleLabel}
        aria-orientation={horizontal ? 'vertical' : 'horizontal'}
        aria-controls={before?.id}
        aria-valuenow={Math.round(size)}
        aria-valuemin={before ? Math.round(before.collapsible ? before.collapsed : before.min) : 0}
        aria-valuemax={before ? Math.round(before.max) : 100}
        aria-disabled={disabled || undefined}
        {...rest}
        className={cx('mn-resizable-panels__handle', className)}
        data-dragging={dragging ? '' : undefined}
        data-disabled={disabled ? '' : undefined}
        onKeyDown={onKey}
        onPointerDown={onDown}
        onPointerMove={onMove}
        onPointerUp={(e) => {
          onPointerUp?.(e);
          end(e);
        }}
        onPointerCancel={(e) => {
          onPointerCancel?.(e);
          end(e);
        }}
        onLostPointerCapture={end}
      >
        {withHandle ? (
          <span className="mn-resizable-panels__grip" aria-hidden>
            <svg width="10" height="16" viewBox="0 0 10 16" fill="currentColor" focusable={false}>
              {[3, 7].flatMap((x) =>
                [4, 8, 12].map((y) => <circle key={`${x}-${y}`} cx={x} cy={y} r="1.1" />),
              )}
            </svg>
          </span>
        ) : null}
      </div>
    );
  },
);
