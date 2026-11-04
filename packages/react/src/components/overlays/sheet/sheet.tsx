'use client';

import {
  forwardRef,
  type ComponentPropsWithoutRef,
  type ElementRef,
  type HTMLAttributes,
  type ReactNode,
} from 'react';
import * as DialogPrimitive from '@radix-ui/react-dialog';
import { cx } from '../../../internal/cx.js';
import { CloseGlyph } from '../../../internal/icons.js';
import { dismissHandlers } from '../../../internal/dismiss.js';
import {
  DialogBody,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '../dialog/index.js';

/**
 * A panel that slides in from an edge of the screen: detail views, filters,
 * longer forms and mobile navigation. It is a modal dialog (Radix Dialog), so
 * focus is trapped, the page behind does not scroll, Escape closes it and
 * focus returns to the trigger. Every sheet needs a SheetTitle.
 */
export const Sheet = DialogPrimitive.Root;
export const SheetTrigger = DialogPrimitive.Trigger;
export const SheetClose = DialogPrimitive.Close;

export type SheetSide = 'right' | 'left' | 'top' | 'bottom';
export type SheetSize = 'sm' | 'md' | 'lg' | 'xl' | 'full';

export interface SheetContentProps extends ComponentPropsWithoutRef<
  typeof DialogPrimitive.Content
> {
  /** The edge the sheet is attached to. Default `right`. */
  side?: SheetSide;
  /**
   * Width for `left`/`right` (20, 28, 36 or 48rem, or the full screen) and the
   * maximum height for `top`/`bottom` (40, 60, 80 or 92% of the screen, or all
   * of it). Default `md`. Side sheets never exceed the screen width.
   */
  size?: SheetSize;
  /** Accessible label of the built-in close button. Pass null to hide the button. */
  closeLabel?: string | null;
  /** Actions in a SheetFooter after the children; it stays visible while SheetBody scrolls. */
  footer?: ReactNode;
  /**
   * An action is in progress: Escape, outside clicks and the built-in close
   * button no longer close the sheet, and the content is `aria-busy`.
   */
  busy?: boolean;
  /** Whether a click outside closes the sheet. Default `true`. */
  closeOnOutsideClick?: boolean;
  /**
   * Ignore Escape while focus is on a control whose own popup is open (a
   * combobox or menu button with `aria-expanded="true"`). Default `true`.
   */
  escapeGuard?: boolean;
  /** Show the grab handle on a bottom sheet. Decorative; default `true` for `bottom`. */
  handle?: boolean;
}

export const SheetContent = forwardRef<
  ElementRef<typeof DialogPrimitive.Content>,
  SheetContentProps
>(function SheetContent(
  {
    side = 'right',
    size = 'md',
    closeLabel = 'Close',
    footer,
    busy = false,
    closeOnOutsideClick = true,
    escapeGuard = true,
    handle,
    onEscapeKeyDown,
    onInteractOutside,
    className,
    children,
    ...rest
  },
  ref,
) {
  const guards = dismissHandlers(
    { busy, closeOnOutsideClick, escapeGuard },
    { onEscapeKeyDown, onInteractOutside },
  );
  const showHandle = handle ?? side === 'bottom';
  return (
    <DialogPrimitive.Portal>
      <DialogPrimitive.Overlay className="mn-overlay" />
      <DialogPrimitive.Content
        ref={ref}
        {...rest}
        {...guards}
        aria-busy={busy || rest['aria-busy'] || undefined}
        data-side={side}
        data-size={size}
        data-busy={busy || undefined}
        className={cx('mn-sheet', className)}
      >
        {showHandle ? <div className="mn-sheet__handle" aria-hidden /> : null}
        {children}
        {footer ? <SheetFooter>{footer}</SheetFooter> : null}
        {closeLabel ? (
          <DialogPrimitive.Close
            className="mn-dialog__close mn-sheet__close"
            aria-label={closeLabel}
            disabled={busy || undefined}
          >
            <CloseGlyph />
          </DialogPrimitive.Close>
        ) : null}
      </DialogPrimitive.Content>
    </DialogPrimitive.Portal>
  );
});

/** Title and description block at the top of the sheet. */
export function SheetHeader({ className, ...rest }: HTMLAttributes<HTMLDivElement>) {
  return <DialogHeader {...rest} className={cx('mn-sheet__header', className)} />;
}

/** The sheet's accessible name. Required; wrap in VisuallyHidden to hide it. */
export const SheetTitle = forwardRef<
  ElementRef<typeof DialogTitle>,
  ComponentPropsWithoutRef<typeof DialogTitle>
>(function SheetTitle({ className, ...rest }, ref) {
  return <DialogTitle ref={ref} {...rest} className={cx('mn-sheet__title', className)} />;
});

/** Becomes the sheet's accessible description. */
export const SheetDescription = forwardRef<
  ElementRef<typeof DialogDescription>,
  ComponentPropsWithoutRef<typeof DialogDescription>
>(function SheetDescription({ className, ...rest }, ref) {
  return (
    <DialogDescription ref={ref} {...rest} className={cx('mn-sheet__description', className)} />
  );
});

/** The scrolling region between header and footer. */
export function SheetBody({ className, ...rest }: HTMLAttributes<HTMLDivElement>) {
  return <DialogBody {...rest} className={cx('mn-sheet__body', className)} />;
}

/** Sticky actions at the bottom of the sheet. */
export function SheetFooter({ className, ...rest }: HTMLAttributes<HTMLDivElement>) {
  return <DialogFooter {...rest} className={cx('mn-sheet__footer', className)} />;
}
