import { forwardRef, type HTMLAttributes, type ReactNode } from 'react';
import { cx } from '../../../internal/cx.js';

/** The release stages LifecycleBadge knows: an API, SDK or feature moves from planned to deprecated. */
export type Lifecycle = 'planned' | 'preview' | 'stable' | 'deprecated';

/** The tones a lifecycle stage can take. */
export type LifecycleTone = 'neutral' | 'accent' | 'info' | 'success' | 'warning' | 'danger';

/** Visible labels for the built-in stages. */
export interface LifecycleBadgeMessages {
  planned: string;
  preview: string;
  stable: string;
  deprecated: string;
}

const DEFAULT_MESSAGES: LifecycleBadgeMessages = {
  planned: 'Planned',
  preview: 'Preview',
  stable: 'Stable',
  deprecated: 'Deprecated',
};

const DEFAULT_TONES: Record<Lifecycle, LifecycleTone> = {
  planned: 'neutral',
  preview: 'info',
  stable: 'success',
  deprecated: 'warning',
};

const isBuiltIn = (value: string): value is Lifecycle => value in DEFAULT_TONES;

export interface LifecycleBadgeProps extends Omit<HTMLAttributes<HTMLSpanElement>, 'children'> {
  /**
   * The stage. The built-in stages (`planned`, `preview`, `stable`,
   * `deprecated`) have a label and tone. Any other key renders with `label`
   * and `tone`, or, without a label, as the key with underscores turned into
   * spaces, so a stage that was since removed still shows something.
   */
  lifecycle: Lifecycle | (string & {});
  /** Visible text. Overrides the built-in label for the stage. */
  label?: ReactNode | undefined;
  /** Tone of the dot and border. Defaults to the built-in stage's tone, or `neutral`. */
  tone?: LifecycleTone | undefined;
  /** Show the leading dot. Default true. */
  dot?: boolean | undefined;
  /** `md` (default) or `sm` for dense rows. */
  size?: 'sm' | 'md' | undefined;
  /** Labels for the built-in stages. */
  messages?: Partial<LifecycleBadgeMessages> | undefined;
}

/** The label LifecycleBadge shows for a stage key without a `label`. */
export function formatLifecycleLabel(
  lifecycle: string,
  messages: Partial<LifecycleBadgeMessages> = {},
): string {
  if (isBuiltIn(lifecycle)) return messages[lifecycle] ?? DEFAULT_MESSAGES[lifecycle];
  const text = lifecycle.replace(/[_-]+/g, ' ').trim();
  return text.charAt(0).toUpperCase() + text.slice(1);
}

/**
 * The release or lifecycle stage of something: an API that is planned, in
 * preview, stable or deprecated, or a custom stage such as a customer's
 * lifecycle stage. A quiet chip with a coloured dot and always a text label.
 */
export const LifecycleBadge = forwardRef<HTMLSpanElement, LifecycleBadgeProps>(
  function LifecycleBadge(
    { lifecycle, label, tone, dot = true, size = 'md', messages, className, ...rest },
    ref,
  ) {
    const resolvedTone = tone ?? (isBuiltIn(lifecycle) ? DEFAULT_TONES[lifecycle] : 'neutral');
    return (
      <span
        ref={ref}
        {...rest}
        className={cx('mn-lifecycle-badge', className)}
        data-lifecycle={lifecycle}
        data-tone={resolvedTone}
        data-size={size}
      >
        {dot ? <span className="mn-lifecycle-badge__dot" aria-hidden /> : null}
        <span className="mn-lifecycle-badge__label">
          {label ?? formatLifecycleLabel(lifecycle, messages)}
        </span>
      </span>
    );
  },
);
