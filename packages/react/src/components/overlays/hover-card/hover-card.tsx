'use client';

import {
  createContext,
  forwardRef,
  useCallback,
  useContext,
  useEffect,
  useId,
  useRef,
  useState,
  type ComponentPropsWithoutRef,
  type ElementRef,
  type KeyboardEvent,
  type ReactElement,
  type ReactNode,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { useComposedRef } from '../../../internal/compose-refs.js';
import { Popover, PopoverAnchor, PopoverContent } from '../popover/index.js';

const FOCUSABLE =
  'a[href], button:not([disabled]), input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])';

interface HoverCardContextValue {
  open: boolean;
  /** Opens or closes after `delay` ms; a later call replaces a pending one. */
  schedule: (next: boolean, delay: number) => void;
  /** Cancels a pending open or close. */
  hold: () => void;
  openDelay: number;
  closeDelay: number;
  triggerId: string;
  contentId: string;
  triggerRef: { current: HTMLElement | null };
  contentRef: { current: HTMLDivElement | null };
}

const HoverCardContext = createContext<HoverCardContextValue | null>(null);

function useHoverCard(part: string): HoverCardContextValue {
  const context = useContext(HoverCardContext);
  if (!context) throw new Error(`${part} must be used inside <HoverCard>.`);
  return context;
}

export interface HoverCardProps {
  /** A HoverCardTrigger and a HoverCardContent. */
  children: ReactNode;
  /**
   * Milliseconds the pointer must rest on the trigger, or keyboard focus stay
   * on it, before the card opens. The delay keeps cards from flashing open
   * while people move the mouse or Tab through a list. Default 600.
   */
  openDelay?: number | undefined;
  /** Milliseconds before the card closes after pointer and focus leave. Default 300. */
  closeDelay?: number | undefined;
  /** Whether the card is open (controlled). */
  open?: boolean | undefined;
  /** Initial open state when uncontrolled. */
  defaultOpen?: boolean | undefined;
  /** Called when the card opens or closes, e.g. to load the preview lazily. */
  onOpenChange?: ((open: boolean) => void) | undefined;
}

/**
 * A preview card that opens when the pointer rests on a trigger (usually a
 * link) or when the trigger has keyboard focus, after a short delay. For
 * extra context only, such as a profile or page preview: touch screens never
 * show it, so nothing in it may be essential.
 */
export function HoverCard({
  children,
  openDelay = 600,
  closeDelay = 300,
  open: openProp,
  defaultOpen = false,
  onOpenChange,
}: HoverCardProps) {
  const [openState, setOpenState] = useState(defaultOpen);
  const open = openProp ?? openState;
  const timer = useRef<ReturnType<typeof setTimeout> | null>(null);
  const triggerRef = useRef<HTMLElement | null>(null);
  const contentRef = useRef<HTMLDivElement | null>(null);
  const id = useId();

  const hold = useCallback(() => {
    if (timer.current !== null) clearTimeout(timer.current);
    timer.current = null;
  }, []);
  useEffect(() => hold, [hold]);

  const setOpen = useCallback(
    (next: boolean) => {
      if (openProp === undefined) setOpenState(next);
      onOpenChange?.(next);
    },
    [openProp, onOpenChange],
  );

  const schedule = useCallback(
    (next: boolean, delay: number) => {
      hold();
      if (next === open) return;
      if (delay <= 0) setOpen(next);
      else timer.current = setTimeout(() => setOpen(next), delay);
    },
    [hold, open, setOpen],
  );

  return (
    <HoverCardContext.Provider
      value={{
        open,
        schedule,
        hold,
        openDelay,
        closeDelay,
        triggerId: `mn-hover-card-${id}-trigger`,
        contentId: `mn-hover-card-${id}-content`,
        triggerRef,
        contentRef,
      }}
    >
      <Popover
        open={open}
        onOpenChange={(next) => {
          hold();
          setOpen(next);
        }}
      >
        {children}
      </Popover>
    </HoverCardContext.Provider>
  );
}

export interface HoverCardTriggerProps {
  /** One focusable element, usually a link or a button. It keeps its own role and name. */
  children: ReactElement<{ id?: string }>;
  /** Accepted for shadcn/ui compatibility; the child is always used as the trigger. */
  asChild?: true | undefined;
}

/**
 * The element that opens the card. It is rendered as is (its props are
 * merged), so it must be focusable by itself: a link or a button.
 */
export function HoverCardTrigger({ children }: HoverCardTriggerProps) {
  const {
    open,
    schedule,
    hold,
    openDelay,
    closeDelay,
    triggerId,
    contentId,
    triggerRef,
    contentRef,
  } = useHoverCard('HoverCardTrigger');
  const touched = useRef(false);
  const id = children.props.id ?? triggerId;

  const focusables = () =>
    Array.from(contentRef.current?.querySelectorAll<HTMLElement>(FOCUSABLE) ?? []);

  return (
    <PopoverAnchor
      asChild
      ref={(node: HTMLElement | null) => {
        triggerRef.current = node;
      }}
      id={id}
      aria-describedby={open ? contentId : undefined}
      onPointerDown={(event) => {
        touched.current = event.pointerType === 'touch';
      }}
      onPointerEnter={(event) => {
        if (event.pointerType !== 'touch') schedule(true, openDelay);
      }}
      onPointerLeave={(event) => {
        if (event.pointerType !== 'touch') schedule(false, closeDelay);
      }}
      onFocus={() => {
        // A tap focuses the link too; on touch the card never opens.
        if (touched.current) {
          touched.current = false;
          return;
        }
        schedule(true, openDelay);
      }}
      onBlur={(event) => {
        const next = event.relatedTarget as Node | null;
        if (next && contentRef.current?.contains(next)) return;
        schedule(false, closeDelay);
      }}
      onKeyDown={(event: KeyboardEvent<HTMLElement>) => {
        if (!open) return;
        if (event.key === 'Escape') {
          event.preventDefault();
          schedule(false, 0);
          return;
        }
        // Tab moves into the card when it has links or buttons, so keyboard users can reach them.
        if (event.key === 'Tab' && !event.shiftKey) {
          const first = focusables()[0];
          if (first) {
            event.preventDefault();
            hold();
            first.focus();
          }
        }
      }}
    >
      {children}
    </PopoverAnchor>
  );
}

export type HoverCardContentProps = Omit<
  ComponentPropsWithoutRef<typeof PopoverContent>,
  'onOpenAutoFocus' | 'onCloseAutoFocus'
>;

/**
 * The card. It never takes focus when it opens; it stays open while the
 * pointer is over it, and Tab from the trigger moves into its links and
 * buttons. Escape closes it and focus stays on (or returns to) the trigger.
 */
export const HoverCardContent = forwardRef<
  ElementRef<typeof PopoverContent>,
  HoverCardContentProps
>(function HoverCardContent(
  {
    className,
    side = 'bottom',
    align = 'center',
    onPointerEnter,
    onPointerLeave,
    onKeyDown,
    onFocusOutside,
    onEscapeKeyDown,
    onBlur,
    ...rest
  },
  ref,
) {
  const { schedule, hold, closeDelay, triggerId, contentId, triggerRef, contentRef } =
    useHoverCard('HoverCardContent');
  const setContentRef = useComposedRef(contentRef, ref);

  const focusables = () =>
    Array.from(contentRef.current?.querySelectorAll<HTMLElement>(FOCUSABLE) ?? []);

  return (
    <PopoverContent
      ref={setContentRef}
      id={contentId}
      // The card is named by its trigger (a person's name, a page title). The content
      // only renders while open, so the trigger, and any id of its own, exists by then.
      aria-labelledby={triggerRef.current?.id || triggerId}
      side={side}
      align={align}
      {...rest}
      className={cx('mn-hover-card', className)}
      onOpenAutoFocus={(event) => event.preventDefault()}
      onCloseAutoFocus={(event) => event.preventDefault()}
      onPointerEnter={(event) => {
        onPointerEnter?.(event);
        hold();
      }}
      onPointerLeave={(event) => {
        onPointerLeave?.(event);
        if (event.pointerType !== 'touch') schedule(false, closeDelay);
      }}
      onBlur={(event) => {
        onBlur?.(event);
        const next = event.relatedTarget as Node | null;
        if (next && (contentRef.current?.contains(next) || triggerRef.current?.contains(next)))
          return;
        schedule(false, closeDelay);
      }}
      onFocusOutside={(event) => {
        onFocusOutside?.(event);
        if (triggerRef.current?.contains(event.target as Node)) event.preventDefault();
      }}
      onKeyDown={(event) => {
        onKeyDown?.(event);
        if (event.defaultPrevented || event.key !== 'Tab') return;
        const items = focusables();
        const index = items.indexOf(document.activeElement as HTMLElement);
        const leavingBack = event.shiftKey && index <= 0;
        const leavingForward = !event.shiftKey && index === items.length - 1;
        if (leavingBack || leavingForward) {
          event.preventDefault();
          triggerRef.current?.focus();
          if (leavingForward) schedule(false, 0);
        }
      }}
      onEscapeKeyDown={(event) => {
        onEscapeKeyDown?.(event);
        triggerRef.current?.focus();
      }}
    />
  );
});
