import { forwardRef, type HTMLAttributes } from 'react';
import { cx } from '../../../internal/cx.js';

export interface SpinnerProps extends Omit<HTMLAttributes<HTMLSpanElement>, 'children'> {
  size?: 'sm' | 'md' | 'lg';
  /**
   * Accessible name. With a label the spinner is announced as a status;
   * without one it is decorative (use that inside a busy button).
   */
  label?: string;
}

/** Indeterminate progress indicator. Server-component safe. */
export const Spinner = forwardRef<HTMLSpanElement, SpinnerProps>(function Spinner(
  { size = 'md', label, className, ...rest },
  ref,
) {
  const a11y = label
    ? { role: 'status' as const, 'aria-label': label }
    : { 'aria-hidden': true as const };
  return (
    <span {...rest} {...a11y} ref={ref} className={cx('mn-spinner', className)} data-size={size} />
  );
});
Spinner.displayName = 'Spinner';
