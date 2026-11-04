import type { HTMLAttributes } from 'react';
import { cx } from '../../../internal/cx.js';

/** Hides content visually while keeping it available to assistive technology. */
export function VisuallyHidden({ className, ...rest }: HTMLAttributes<HTMLSpanElement>) {
  return <span {...rest} className={cx('mn-visually-hidden', className)} />;
}
