import { forwardRef, type ComponentPropsWithoutRef, type ElementRef } from 'react';
import * as SeparatorPrimitive from '@radix-ui/react-separator';
import { cx } from '../../../internal/cx.js';

/** Visual divider. `decorative` (default) hides it from assistive technology. */
export const Separator = forwardRef<
  ElementRef<typeof SeparatorPrimitive.Root>,
  ComponentPropsWithoutRef<typeof SeparatorPrimitive.Root>
>(function Separator({ className, orientation = 'horizontal', decorative = true, ...rest }, ref) {
  return (
    <SeparatorPrimitive.Root
      ref={ref}
      orientation={orientation}
      decorative={decorative}
      {...rest}
      className={cx('mn-separator', className)}
    />
  );
});
