'use client';

import {
  createContext,
  forwardRef,
  useCallback,
  useContext,
  useRef,
  type ComponentPropsWithoutRef,
  type ElementRef,
  type HTMLAttributes,
  type PointerEvent,
  type ReactNode,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { useControllable } from '../../../internal/use-controllable.js';
import {
  Sheet,
  SheetBody,
  SheetClose,
  SheetContent,
  SheetDescription,
  SheetFooter,
  SheetHeader,
  SheetTitle,
  SheetTrigger,
  type SheetContentProps,
} from '../sheet/index.js';

interface DrawerContextValue {
  close: () => void;
}

const DrawerContext = createContext<DrawerContextValue | null>(null);

export interface DrawerProps {
  /** Whether the drawer is open (controlled). Pair with `onOpenChange`. */
  open?: boolean | undefined;
  /** Whether the drawer starts open (uncontrolled). Default `false`. */
  defaultOpen?: boolean | undefined;
  /** Called when the drawer opens or closes, including after a drag down. */
  onOpenChange?: ((open: boolean) => void) | undefined;
  /** DrawerTrigger, DrawerContent and anything else. */
  children?: ReactNode;
}

/**
 * A panel that rises from the bottom of the screen on phones, with a grab
 * handle that can be dragged down to close it. Built on Sheet (a modal
 * dialog), so focus is trapped, Escape and the close button close it and
 * focus returns to the trigger. Every drawer needs a DrawerTitle.
 */
export function Drawer({
  open: openProp,
  defaultOpen = false,
  onOpenChange,
  children,
}: DrawerProps) {
  const [open, setOpen] = useControllable(openProp, defaultOpen, onOpenChange);
  const close = useCallback(() => setOpen(false), [setOpen]);
  return (
    <DrawerContext.Provider value={{ close }}>
      <Sheet open={open} onOpenChange={setOpen}>
        {children}
      </Sheet>
    </DrawerContext.Provider>
  );
}

/** The button that opens the drawer. Use `asChild` to render your own Button. */
export const DrawerTrigger = SheetTrigger;
/** Closes the drawer. Use `asChild` to render your own Button. */
export const DrawerClose = SheetClose;

/** Pointer travel (px) or speed (px/ms) past which a drag down closes the drawer. */
const MIN_CLOSE_DISTANCE = 80;
const CLOSE_RATIO = 0.25;
const CLOSE_VELOCITY = 0.5;

/**
 * Decides whether a finished drag closes the drawer: it moved down by a
 * quarter of the drawer's height (at least 80px), or was flicked down faster
 * than 0.5px per millisecond.
 */
export function shouldCloseDrawer(distance: number, durationMs: number, height: number): boolean {
  if (distance <= 0) return false;
  const threshold = Math.max(MIN_CLOSE_DISTANCE, height * CLOSE_RATIO);
  const velocity = distance / Math.max(durationMs, 1);
  return distance >= threshold || (distance > 10 && velocity >= CLOSE_VELOCITY);
}

export interface DrawerContentProps extends Omit<SheetContentProps, 'side' | 'handle'> {
  /**
   * Let people drag the handle or header down to close the drawer. Turned
   * off while `busy`. Default `true`.
   */
  dragToClose?: boolean | undefined;
}

/**
 * The drawer panel: a bottom Sheet with a grab handle. Drag the handle or
 * the header down to close it; the body scrolls normally.
 */
export const DrawerContent = forwardRef<ElementRef<typeof SheetContent>, DrawerContentProps>(
  function DrawerContent(
    {
      dragToClose = true,
      busy = false,
      size = 'md',
      className,
      children,
      onPointerDown,
      onPointerMove,
      onPointerUp,
      onPointerCancel,
      ...rest
    },
    forwardedRef,
  ) {
    const ctx = useContext(DrawerContext);
    const node = useRef<HTMLDivElement | null>(null);
    const drag = useRef<{ id: number; y: number; t: number; dy: number } | null>(null);
    const canDrag = dragToClose && !busy && ctx !== null;

    const setRef = useCallback(
      (el: HTMLDivElement | null) => {
        node.current = el;
        if (typeof forwardedRef === 'function') forwardedRef(el);
        else if (forwardedRef) forwardedRef.current = el;
      },
      [forwardedRef],
    );

    const reset = () => {
      const el = node.current;
      drag.current = null;
      if (!el) return;
      el.removeAttribute('data-dragging');
      el.style.removeProperty('transform');
    };

    const handlePointerDown = (event: PointerEvent<HTMLDivElement>) => {
      onPointerDown?.(event);
      if (event.defaultPrevented || !canDrag || event.button !== 0) return;
      const target = event.target as Element | null;
      if (!target?.closest?.('.mn-drawer__handle, .mn-sheet__header')) return;
      if (target.closest('button, a, input, select, textarea, [role="button"]')) return;
      drag.current = {
        id: event.pointerId,
        y: event.clientY,
        t: event.nativeEvent.timeStamp,
        dy: 0,
      };
      event.currentTarget.setPointerCapture?.(event.pointerId);
    };

    const handlePointerMove = (event: PointerEvent<HTMLDivElement>) => {
      onPointerMove?.(event);
      const d = drag.current;
      if (!d || d.id !== event.pointerId) return;
      d.dy = Math.max(0, event.clientY - d.y);
      const el = event.currentTarget;
      el.setAttribute('data-dragging', '');
      el.style.transform = `translateY(${d.dy}px)`;
    };

    const handlePointerUp = (event: PointerEvent<HTMLDivElement>) => {
      onPointerUp?.(event);
      const d = drag.current;
      if (!d || d.id !== event.pointerId) return;
      d.dy = Math.max(0, event.clientY - d.y);
      const close = shouldCloseDrawer(
        d.dy,
        event.nativeEvent.timeStamp - d.t,
        event.currentTarget.offsetHeight,
      );
      reset();
      if (close) ctx?.close();
    };

    const handlePointerCancel = (event: PointerEvent<HTMLDivElement>) => {
      onPointerCancel?.(event);
      reset();
    };

    return (
      <SheetContent
        ref={setRef}
        {...rest}
        side="bottom"
        size={size}
        busy={busy}
        handle={false}
        className={cx('mn-drawer', className)}
        data-drag={canDrag || undefined}
        onPointerDown={handlePointerDown}
        onPointerMove={handlePointerMove}
        onPointerUp={handlePointerUp}
        onPointerCancel={handlePointerCancel}
      >
        <div className="mn-drawer__handle" aria-hidden="true">
          <span className="mn-drawer__bar" />
        </div>
        {children}
      </SheetContent>
    );
  },
);

/** Title and description block at the top of the drawer; also a drag area. */
export const DrawerHeader = forwardRef<HTMLDivElement, HTMLAttributes<HTMLDivElement>>(
  function DrawerHeader({ className, ...rest }, ref) {
    return <SheetHeader {...rest} ref={ref} className={cx('mn-drawer__header', className)} />;
  },
);
DrawerHeader.displayName = 'DrawerHeader';

/** The drawer's accessible name. Required; wrap in VisuallyHidden to hide it. */
export const DrawerTitle = forwardRef<
  ElementRef<typeof SheetTitle>,
  ComponentPropsWithoutRef<typeof SheetTitle>
>(function DrawerTitle(props, ref) {
  return <SheetTitle ref={ref} {...props} />;
});

/** Becomes the drawer's accessible description. */
export const DrawerDescription = forwardRef<
  ElementRef<typeof SheetDescription>,
  ComponentPropsWithoutRef<typeof SheetDescription>
>(function DrawerDescription(props, ref) {
  return <SheetDescription ref={ref} {...props} />;
});

/** The scrolling region between header and footer. */
export const DrawerBody = forwardRef<HTMLDivElement, HTMLAttributes<HTMLDivElement>>(
  function DrawerBody(props, ref) {
    return <SheetBody {...props} ref={ref} />;
  },
);
DrawerBody.displayName = 'DrawerBody';

/** Actions at the bottom of the drawer; they stay visible while the body scrolls. */
export const DrawerFooter = forwardRef<HTMLDivElement, HTMLAttributes<HTMLDivElement>>(
  function DrawerFooter(props, ref) {
    return <SheetFooter {...props} ref={ref} />;
  },
);
DrawerFooter.displayName = 'DrawerFooter';
