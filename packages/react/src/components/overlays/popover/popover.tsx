'use client';

import { Fragment, forwardRef, type ComponentPropsWithoutRef, type ElementRef } from 'react';
import * as PopoverPrimitive from '@radix-ui/react-popover';
import { cx } from '../../../internal/cx.js';
import { useDirectionContext } from '../../../internal/direction.js';
import { stagedDismiss, useOverlayStage } from '../../../internal/overlay-stage.js';

/** Non-modal floating panel for filters, pickers and quick edits. */
export function Popover(props: PopoverPrimitive.PopoverProps) {
  // In a docs preview stage the popover is a picture in the page: never modal.
  const staged = useOverlayStage();
  return <PopoverPrimitive.Root {...props} {...(staged ? { modal: false } : {})} />;
}
export const PopoverTrigger = PopoverPrimitive.Trigger;
export const PopoverAnchor = PopoverPrimitive.Anchor;
export const PopoverClose = PopoverPrimitive.Close;

export const PopoverContent = forwardRef<
  ElementRef<typeof PopoverPrimitive.Content>,
  ComponentPropsWithoutRef<typeof PopoverPrimitive.Content>
>(function PopoverContent({ className, sideOffset = 6, align = 'start', ...rest }, ref) {
  // The panel is portalled to <body>, outside the element that carries `dir`:
  // hand it the DirectionProvider's direction so its text and logical CSS follow.
  const dir = useDirectionContext();
  const staged = useOverlayStage();
  const Portal = staged ? Fragment : PopoverPrimitive.Portal;
  return (
    <Portal>
      <PopoverPrimitive.Content
        ref={ref}
        {...(dir ? { dir } : {})}
        sideOffset={sideOffset}
        align={align}
        {...rest}
        {...(staged
          ? {
              ...stagedDismiss(staged, rest),
              // A staged overlay is a picture: nothing in it takes focus on mount.
              onOpenAutoFocus: (event: Event) => event.preventDefault(),
            }
          : {})}
        className={cx('mn-popover', className)}
      />
    </Portal>
  );
});
