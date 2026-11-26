'use client';

import { forwardRef, type ElementRef } from 'react';
import { cx } from '../../../internal/cx.js';
import {
  Dialog,
  DialogClose,
  DialogContent,
  DialogTrigger,
  type DialogContentProps,
} from '../dialog/index.js';

/**
 * A centred Dialog on wide screens and a bottom sheet on narrow ones, from one
 * API and one DOM. The switch is pure CSS (a media query), not `matchMedia`:
 * the server and the first client render are identical, so there is no
 * hydration mismatch and no flash of the wrong layout. Focus trap, Escape and
 * focus return are the Dialog's in both layouts.
 *
 * Compose it with the Dialog parts: DialogHeader, DialogTitle,
 * DialogDescription, DialogBody and DialogFooter.
 */
export const ResponsiveDialog = Dialog;
export const ResponsiveDialogTrigger = DialogTrigger;
export const ResponsiveDialogClose = DialogClose;

export interface ResponsiveDialogContentProps extends Omit<
  DialogContentProps,
  'fullScreenOnPhone'
> {
  /**
   * The viewport width below which it becomes a bottom sheet: `sm` (640px,
   * default) or `md` (768px).
   */
  sheetBelow?: 'sm' | 'md';
  /** Show the grab handle in the bottom-sheet layout. Decorative; default `true`. */
  handle?: boolean;
}

export const ResponsiveDialogContent = forwardRef<
  ElementRef<typeof DialogContent>,
  ResponsiveDialogContentProps
>(function ResponsiveDialogContent(
  { sheetBelow = 'sm', handle = true, className, children, ...rest },
  ref,
) {
  return (
    <DialogContent
      ref={ref}
      {...rest}
      data-sheet-below={sheetBelow}
      className={cx('mn-responsive-dialog', className)}
    >
      {handle ? <div className="mn-responsive-dialog__handle" aria-hidden /> : null}
      {children}
    </DialogContent>
  );
});
