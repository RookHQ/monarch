import { forwardRef, type HTMLAttributes } from 'react';
import { cx } from '../../../internal/cx.js';

/** A keyboard key or shortcut, e.g. <Kbd>⌘K</Kbd>. */
export const Kbd = forwardRef<HTMLElement, HTMLAttributes<HTMLElement>>(function Kbd(
  { className, ...rest },
  ref,
) {
  return <kbd {...rest} ref={ref} className={cx('mn-kbd', className)} />;
});
Kbd.displayName = 'Kbd';
