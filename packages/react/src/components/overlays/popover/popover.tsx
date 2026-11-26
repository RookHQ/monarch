'use client';

import { forwardRef, type ComponentPropsWithoutRef, type ElementRef } from 'react';
import * as PopoverPrimitive from '@radix-ui/react-popover';
import { cx } from '../../../internal/cx.js';
import { useDirectionContext } from '../../../internal/direction.js';

/** Non-modal floating panel for filters, pickers and quick edits. */
export const Popover = PopoverPrimitive.Root;
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
  return (
    <PopoverPrimitive.Portal>
      <PopoverPrimitive.Content
        ref={ref}
        {...(dir ? { dir } : {})}
        sideOffset={sideOffset}
        align={align}
        {...rest}
        className={cx('mn-popover', className)}
      />
    </PopoverPrimitive.Portal>
  );
});
