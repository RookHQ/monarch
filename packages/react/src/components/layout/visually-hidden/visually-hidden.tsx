import { forwardRef, type HTMLAttributes } from 'react';
import { cx } from '../../../internal/cx.js';

/** Hides content visually while keeping it available to assistive technology. */
export const VisuallyHidden = forwardRef<HTMLSpanElement, HTMLAttributes<HTMLSpanElement>>(
  function VisuallyHidden({ className, ...rest }, ref) {
    return <span {...rest} ref={ref} className={cx('mn-visually-hidden', className)} />;
  },
);
VisuallyHidden.displayName = 'VisuallyHidden';
