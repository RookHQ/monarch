import { forwardRef, type HTMLAttributes } from 'react';
import { cx } from '../../../internal/cx.js';

/** The HTTP methods MethodBadge colours. Any other method renders in the neutral tone. */
export type HttpMethod = 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE' | 'HEAD' | 'OPTIONS';

/** The tone each HTTP method gets: reads are green, creates blue, updates yellow, deletes red. */
export type MethodTone = 'neutral' | 'info' | 'success' | 'warning' | 'danger';

const METHOD_TONES: Record<string, MethodTone> = {
  GET: 'success',
  HEAD: 'neutral',
  OPTIONS: 'neutral',
  POST: 'info',
  PUT: 'warning',
  PATCH: 'warning',
  DELETE: 'danger',
};

/** The tone MethodBadge uses for a method (case-insensitive). Unknown methods are `neutral`. */
export function methodTone(method: string): MethodTone {
  return METHOD_TONES[method.toUpperCase()] ?? 'neutral';
}

export interface MethodBadgeProps extends Omit<HTMLAttributes<HTMLSpanElement>, 'children'> {
  /**
   * The HTTP method, such as `GET` or `DELETE`. Shown upper-cased in a
   * monospace font. Methods outside the common set render in the neutral tone.
   */
  method: HttpMethod | (string & {});
  /** `md` (default) for tables and endpoint lists, `sm` for dense rows such as logs. */
  size?: 'sm' | 'md' | undefined;
  /** Overrides the tone chosen from the method. */
  tone?: MethodTone | undefined;
}

/**
 * An HTTP method label (GET, POST, PUT, PATCH, DELETE) for endpoint lists, API
 * references and request logs. Each method has its own tone, and the method
 * name is always the visible text, so colour is never the only signal.
 */
export const MethodBadge = forwardRef<HTMLSpanElement, MethodBadgeProps>(function MethodBadge(
  { method, size = 'md', tone, className, ...rest },
  ref,
) {
  const upper = method.toUpperCase();
  return (
    <span
      ref={ref}
      {...rest}
      className={cx('mn-method-badge', className)}
      data-method={upper.toLowerCase()}
      data-tone={tone ?? methodTone(upper)}
      data-size={size}
    >
      {upper}
    </span>
  );
});
