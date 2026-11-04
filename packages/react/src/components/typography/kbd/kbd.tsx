import type { HTMLAttributes } from 'react';
import { cx } from '../../../internal/cx.js';

/** A keyboard key or shortcut, e.g. <Kbd>⌘K</Kbd>. */
export function Kbd({ className, ...rest }: HTMLAttributes<HTMLElement>) {
  return <kbd {...rest} className={cx('mn-kbd', className)} />;
}
