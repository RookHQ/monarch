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

/**
 * Modal dialog. Focus is trapped inside, Escape closes it, and focus returns
 * to the trigger. Every dialog needs a DialogTitle (visually hidden if
 * necessary) so it has an accessible name.
 */
export const Dialog = DialogPrimitive.Root;
export const DialogTrigger = DialogPrimitive.Trigger;
export const DialogClose = DialogPrimitive.Close;

export interface DialogContentProps extends ComponentPropsWithoutRef<
  typeof DialogPrimitive.Content
> {
  /** Width preset: 24rem, 32rem (default), 44rem or 60rem, capped at the viewport minus 2rem. */
  size?: 'sm' | 'md' | 'lg' | 'xl';
  /** Accessible label of the built-in close button. Pass null to hide the button. */
  closeLabel?: string | null;
  /**
   * Actions rendered in a DialogFooter after the children. The footer stays
   * visible while DialogBody scrolls.
   */
  footer?: ReactNode;
  /**
   * An action is in progress: Escape, outside clicks and the built-in close
   * button no longer close the dialog, and the content is `aria-busy`.
   * Disable your own Cancel button too.
   */
  busy?: boolean;
  /**
   * Whether a click outside the dialog closes it. Default `true`; turn it off
   * for forms with typed values so a stray click does not lose them. Escape
   * and the close button still work.
   */
  closeOnOutsideClick?: boolean;
  /**
   * Ignore Escape while focus is on a control whose own popup is open (a
   * combobox or menu button with `aria-expanded="true"`), so that press only
   * closes the popup. Default `true`.
   */
  escapeGuard?: boolean;
  /** Below 640px wide, fill the whole screen instead of floating with a margin. */
  fullScreenOnPhone?: boolean;
}

export const DialogContent = forwardRef<
  ElementRef<typeof DialogPrimitive.Content>,
  DialogContentProps
>(function DialogContent(
  {
    size = 'md',
    closeLabel = 'Close',
    footer,
    busy = false,
    closeOnOutsideClick = true,
    escapeGuard = true,
    fullScreenOnPhone = false,
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
  return (
    <DialogPrimitive.Portal>
      <DialogPrimitive.Overlay className="mn-overlay" />
      <DialogPrimitive.Content
        ref={ref}
        {...rest}
        {...guards}
        aria-busy={busy || rest['aria-busy'] || undefined}
        data-size={size}
        data-busy={busy || undefined}
        data-fullscreen-phone={fullScreenOnPhone || undefined}
        className={cx('mn-dialog', className)}
      >
        {children}
        {footer ? <DialogFooter>{footer}</DialogFooter> : null}
        {closeLabel ? (
          <DialogPrimitive.Close
            className="mn-dialog__close"
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

export function DialogHeader({ className, ...rest }: HTMLAttributes<HTMLDivElement>) {
  return <div {...rest} className={cx('mn-dialog__header', className)} />;
}

export const DialogTitle = forwardRef<
  ElementRef<typeof DialogPrimitive.Title>,
  ComponentPropsWithoutRef<typeof DialogPrimitive.Title>
>(function DialogTitle({ className, ...rest }, ref) {
  return (
    <DialogPrimitive.Title ref={ref} {...rest} className={cx('mn-dialog__title', className)} />
  );
});

export const DialogDescription = forwardRef<
  ElementRef<typeof DialogPrimitive.Description>,
  ComponentPropsWithoutRef<typeof DialogPrimitive.Description>
>(function DialogDescription({ className, ...rest }, ref) {
  return (
    <DialogPrimitive.Description
      ref={ref}
      {...rest}
      className={cx('mn-dialog__description', className)}
    />
  );
});

export function DialogBody({ className, ...rest }: HTMLAttributes<HTMLDivElement>) {
  return <div {...rest} className={cx('mn-dialog__body', className)} />;
}

export function DialogFooter({ className, ...rest }: HTMLAttributes<HTMLDivElement>) {
  return <div {...rest} className={cx('mn-dialog__footer', className)} />;
}
