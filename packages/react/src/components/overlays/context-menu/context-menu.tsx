'use client';

import {
  createContext,
  forwardRef,
  useCallback,
  useContext,
  useEffect,
  useRef,
  useState,
  type ComponentPropsWithoutRef,
  type ElementRef,
  type HTMLAttributes,
  type KeyboardEvent,
  type MouseEvent,
  type PointerEvent,
  type ReactNode,
} from 'react';
import { Slot } from '@radix-ui/react-slot';
import { cx } from '../../../internal/cx.js';
import {
  DropdownMenu,
  DropdownMenuCheckboxItem,
  DropdownMenuContent,
  DropdownMenuGroup,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuRadioGroup,
  DropdownMenuRadioItem,
  DropdownMenuSeparator,
  DropdownMenuSub,
  DropdownMenuSubContent,
  DropdownMenuSubTrigger,
  DropdownMenuTrigger,
} from '../dropdown-menu/index.js';

/** How long a touch must be held to open the menu, in milliseconds. */
const LONG_PRESS_MS = 500;
/** A touch that moves further than this (px) is a scroll, not a long press. */
const LONG_PRESS_SLOP = 10;

interface Point {
  x: number;
  y: number;
}

interface ContextMenuContextValue {
  open: boolean;
  openAt: (point: Point) => void;
  point: Point;
  returnFocus: () => void;
  label: string;
}

const ContextMenuContext = createContext<ContextMenuContextValue | null>(null);

function useContextMenu(part: string): ContextMenuContextValue {
  const context = useContext(ContextMenuContext);
  if (!context) throw new Error(`${part} must be used inside <ContextMenu>.`);
  return context;
}

export interface ContextMenuProps {
  /** A ContextMenuTrigger (the region) and a ContextMenuContent (the items). */
  children: ReactNode;
  /** Whether the menu is open (controlled). */
  open?: boolean | undefined;
  /** Called when the menu opens or closes. */
  onOpenChange?: ((open: boolean) => void) | undefined;
  /**
   * When `true` (default), the rest of the page ignores the pointer while the
   * menu is open, as with a native context menu.
   */
  modal?: boolean | undefined;
  /** Accessible name of the menu. Default "Actions". */
  label?: string | undefined;
}

/**
 * A menu of actions for a region, opened where the person right-clicks,
 * with Shift+F10 or the Menu key while something inside the region has
 * focus, or with a long press on touch screens. Built on DropdownMenu, so
 * items, keyboard and styles are the same. Every action in it must also be
 * reachable another way (a button or a "More" menu): context menus are hard
 * to discover.
 */
export function ContextMenu({
  children,
  open: openProp,
  onOpenChange,
  modal = true,
  label = 'Actions',
}: ContextMenuProps) {
  const [openState, setOpenState] = useState(false);
  const open = openProp ?? openState;
  const [point, setPoint] = useState<Point>({ x: 0, y: 0 });
  const returnTo = useRef<HTMLElement | null>(null);

  const setOpen = useCallback(
    (next: boolean) => {
      if (openProp === undefined) setOpenState(next);
      onOpenChange?.(next);
    },
    [openProp, onOpenChange],
  );

  const openAt = useCallback(
    (next: Point) => {
      const active = typeof document !== 'undefined' ? document.activeElement : null;
      returnTo.current = active instanceof HTMLElement && active !== document.body ? active : null;
      setPoint(next);
      setOpen(true);
    },
    [setOpen],
  );

  const returnFocus = useCallback(() => {
    const target = returnTo.current;
    returnTo.current = null;
    if (target?.isConnected) target.focus();
  }, []);

  return (
    <ContextMenuContext.Provider value={{ open, openAt, point, returnFocus, label }}>
      <DropdownMenu open={open} onOpenChange={setOpen} modal={modal}>
        {children}
      </DropdownMenu>
    </ContextMenuContext.Provider>
  );
}

export interface ContextMenuTriggerProps extends HTMLAttributes<HTMLDivElement> {
  /** Merge the behaviour into the single child element instead of rendering a `<div>`. */
  asChild?: boolean | undefined;
  /** The browser's own context menu shows instead, and long press does nothing. */
  disabled?: boolean | undefined;
}

/**
 * The region that opens the menu: right-click, Shift+F10 or the Menu key
 * while it or something inside has focus, or a long press on touch. It does
 * not add a Tab stop: put focusable content in it, or give it `tabIndex={0}`
 * and an accessible name.
 */
export const ContextMenuTrigger = forwardRef<HTMLDivElement, ContextMenuTriggerProps>(
  function ContextMenuTrigger(
    {
      asChild = false,
      disabled = false,
      className,
      children,
      onContextMenu,
      onKeyDown,
      onPointerDown,
      onPointerMove,
      onPointerUp,
      onPointerCancel,
      onClickCapture,
      ...rest
    },
    ref,
  ) {
    const { openAt, point } = useContextMenu('ContextMenuTrigger');
    const timer = useRef<ReturnType<typeof setTimeout> | null>(null);
    const start = useRef<Point | null>(null);
    const suppressClick = useRef(false);
    const [pressing, setPressing] = useState(false);

    const cancelPress = useCallback(() => {
      if (timer.current !== null) clearTimeout(timer.current);
      timer.current = null;
      start.current = null;
      setPressing(false);
    }, []);

    useEffect(() => cancelPress, [cancelPress]);

    const openAtElement = (element: Element) => {
      const rect = element.getBoundingClientRect();
      openAt({ x: rect.left, y: rect.bottom });
    };

    const Comp = asChild ? Slot : 'div';
    return (
      <>
        <Comp
          ref={ref}
          {...rest}
          data-disabled={disabled || undefined}
          data-pressing={pressing || undefined}
          className={cx('mn-context-menu', className)}
          onContextMenu={(event: MouseEvent<HTMLDivElement>) => {
            onContextMenu?.(event);
            if (disabled || event.defaultPrevented) return;
            event.preventDefault();
            cancelPress();
            // A keyboard-opened context menu event has no pointer position.
            if (event.clientX === 0 && event.clientY === 0) openAtElement(event.target as Element);
            else openAt({ x: event.clientX, y: event.clientY });
          }}
          onKeyDown={(event: KeyboardEvent<HTMLDivElement>) => {
            onKeyDown?.(event);
            if (disabled || event.defaultPrevented) return;
            if ((event.key === 'F10' && event.shiftKey) || event.key === 'ContextMenu') {
              event.preventDefault();
              openAtElement(event.target as Element);
            }
          }}
          onPointerDown={(event: PointerEvent<HTMLDivElement>) => {
            onPointerDown?.(event);
            suppressClick.current = false;
            if (disabled || event.pointerType !== 'touch') return;
            const at = { x: event.clientX, y: event.clientY };
            start.current = at;
            setPressing(true);
            timer.current = setTimeout(() => {
              timer.current = null;
              suppressClick.current = true;
              setPressing(false);
              openAt(at);
            }, LONG_PRESS_MS);
          }}
          onPointerMove={(event: PointerEvent<HTMLDivElement>) => {
            onPointerMove?.(event);
            const from = start.current;
            if (!from || timer.current === null) return;
            if (Math.hypot(event.clientX - from.x, event.clientY - from.y) > LONG_PRESS_SLOP)
              cancelPress();
          }}
          onPointerUp={(event: PointerEvent<HTMLDivElement>) => {
            onPointerUp?.(event);
            cancelPress();
          }}
          onPointerCancel={(event: PointerEvent<HTMLDivElement>) => {
            onPointerCancel?.(event);
            cancelPress();
          }}
          onClickCapture={(event: MouseEvent<HTMLDivElement>) => {
            onClickCapture?.(event);
            // The finger lifting after a long press must not also activate what is under it.
            if (suppressClick.current) {
              suppressClick.current = false;
              event.preventDefault();
              event.stopPropagation();
            }
          }}
        >
          {children}
        </Comp>
        <Anchor point={point} />
      </>
    );
  },
);

/**
 * The invisible point the menu is positioned against. It is the
 * DropdownMenu trigger, so the menu gets Radix positioning and collision
 * handling; it is never focusable or announced.
 */
function Anchor({ point }: { point: Point }) {
  return (
    <DropdownMenuTrigger asChild disabled>
      <span
        aria-hidden
        className="mn-context-menu__anchor"
        style={{ left: point.x, top: point.y }}
      />
    </DropdownMenuTrigger>
  );
}

export type ContextMenuContentProps = ComponentPropsWithoutRef<typeof DropdownMenuContent>;

/**
 * The menu panel. Opens at the pointer (or under the focused element when
 * opened from the keyboard), and returns focus to where it was on close.
 */
export const ContextMenuContent = forwardRef<
  ElementRef<typeof DropdownMenuContent>,
  ContextMenuContentProps
>(function ContextMenuContent(
  { className, onCloseAutoFocus, side = 'bottom', align = 'start', sideOffset = 2, ...rest },
  ref,
) {
  const { returnFocus, label } = useContextMenu('ContextMenuContent');
  return (
    <DropdownMenuContent
      ref={ref}
      aria-label={label}
      {...rest}
      aria-labelledby={rest['aria-labelledby']}
      side={side}
      align={align}
      sideOffset={sideOffset}
      className={cx('mn-context-menu__content', className)}
      onCloseAutoFocus={(event) => {
        onCloseAutoFocus?.(event);
        if (event.defaultPrevented) return;
        event.preventDefault();
        returnFocus();
      }}
    />
  );
});

/** A menu action. Same props as DropdownMenuItem (`tone`, `icon`, `shortcut`, `onSelect`). */
export const ContextMenuItem = DropdownMenuItem;
export const ContextMenuCheckboxItem = DropdownMenuCheckboxItem;
export const ContextMenuRadioGroup = DropdownMenuRadioGroup;
export const ContextMenuRadioItem = DropdownMenuRadioItem;
export const ContextMenuLabel = DropdownMenuLabel;
export const ContextMenuSeparator = DropdownMenuSeparator;
export const ContextMenuGroup = DropdownMenuGroup;
export const ContextMenuSub = DropdownMenuSub;
export const ContextMenuSubTrigger = DropdownMenuSubTrigger;
export const ContextMenuSubContent = DropdownMenuSubContent;
