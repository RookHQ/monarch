import type { HTMLAttributes } from 'react';
import { cx } from '../../../internal/cx.js';

export type Tone = 'neutral' | 'accent' | 'info' | 'success' | 'warning' | 'danger';

export interface BadgeProps extends HTMLAttributes<HTMLSpanElement> {
  tone?: Tone;
  /** `subtle` (default) for status in tables, `solid` for counts, `outline` for metadata. */
  variant?: 'subtle' | 'solid' | 'outline';
  size?: 'sm' | 'md';
  /** Leading status dot. */
  dot?: boolean;
}

/** Short status or metadata label. Never the only way to convey meaning: keep the text. */
export function Badge({
  tone = 'neutral',
  variant = 'subtle',
  size = 'md',
  dot = false,
  className,
  children,
  ...rest
}: BadgeProps) {
  return (
    <span
      {...rest}
      className={cx('mn-badge', className)}
      data-tone={tone}
      data-variant={variant}
      data-size={size}
    >
      {dot ? <span className="mn-badge__dot" aria-hidden /> : null}
      {children}
    </span>
  );
}
