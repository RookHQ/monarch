import { forwardRef, type HTMLAttributes, type ReactNode } from 'react';
import { cx } from '../../../internal/cx.js';

/** The meaning a status dot carries. Tones name the meaning, never the colour. */
export type StatusDotTone = 'neutral' | 'accent' | 'info' | 'success' | 'warning' | 'danger';

export interface StatusDotProps extends Omit<HTMLAttributes<HTMLSpanElement>, 'children'> {
  /**
   * The words for the status ("Running", "Paused"). Required: the dot is
   * decorative and never the only signal. With `hideLabel` it is still read
   * by assistive technology.
   */
  label: ReactNode;
  /** Colour of the dot. Default `neutral`. Exposed as `data-tone`. */
  tone?: StatusDotTone | undefined;
  /** `sm` (default) next to small text and in table cells, `md` next to body text. */
  size?: 'sm' | 'md' | undefined;
  /**
   * A soft ring that pulses around the dot, for something that is live or in
   * progress ("Recording", "Deploying"). Static when the user prefers reduced motion.
   */
  pulse?: boolean | undefined;
  /**
   * Hide the label visually and keep it for assistive technology, for very
   * dense places such as an avatar corner. Pair it with a visible legend or a
   * tooltip, because sighted users then see only colour.
   */
  hideLabel?: boolean | undefined;
}

/**
 * A small coloured dot followed by a text label: "● Running". The label is
 * required, so colour is never the only way the status is conveyed.
 */
export const StatusDot = forwardRef<HTMLSpanElement, StatusDotProps>(function StatusDot(
  { label, tone = 'neutral', size = 'sm', pulse = false, hideLabel = false, className, ...rest },
  ref,
) {
  return (
    <span
      ref={ref}
      {...rest}
      className={cx('mn-status-dot', className)}
      data-tone={tone}
      data-size={size}
      data-pulse={pulse || undefined}
      data-label-hidden={hideLabel || undefined}
    >
      <span className="mn-status-dot__dot" aria-hidden="true" />
      <span className={hideLabel ? 'mn-visually-hidden' : 'mn-status-dot__label'}>{label}</span>
    </span>
  );
});
