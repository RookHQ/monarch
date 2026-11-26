import { forwardRef, type HTMLAttributes } from 'react';
import { cx } from '../../../internal/cx.js';

export interface InlineCodeProps extends HTMLAttributes<HTMLElement> {
  /**
   * `subtle` (default) sits on a muted chip; `plain` only switches to the
   * monospace font, for code inside dense UI such as table cells.
   */
  variant?: 'subtle' | 'plain';
  /**
   * Keep the code on one line instead of breaking long tokens such as hashes
   * or paths. It may then overflow its container, so use it only for short
   * code. Default false.
   */
  nowrap?: boolean;
}

/**
 * A code fragment inside running text: a function name, a path, a command or
 * a value to type. Renders `<code>` in the monospace font at 0.9em of the
 * surrounding text, so it fits any size. Long tokens break anywhere rather
 * than widening the page.
 */
export const InlineCode = forwardRef<HTMLElement, InlineCodeProps>(function InlineCode(
  { variant = 'subtle', nowrap = false, className, ...rest },
  ref,
) {
  return (
    <code
      ref={ref}
      {...rest}
      data-variant={variant}
      data-nowrap={nowrap || undefined}
      className={cx('mn-inline-code', className)}
    />
  );
});
