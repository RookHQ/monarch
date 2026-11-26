import { forwardRef, type HTMLAttributes } from 'react';
import { cx } from '../../../internal/cx.js';

export type ProgressCircleSize = 'xs' | 'sm' | 'md' | 'lg' | 'xl';
export type ProgressCircleTone = 'accent' | 'success' | 'warning' | 'danger' | 'neutral';

/** Every announced string of ProgressCircle, for translation. */
export interface ProgressCircleMessages {
  /** Accessible name when no `label` is given and progress is known. Default "Progress". */
  progress: string;
  /** Accessible name when no `label` is given and progress is unknown. Default "Loading". */
  loading: string;
}

const DEFAULT_MESSAGES: ProgressCircleMessages = { progress: 'Progress', loading: 'Loading' };

export interface ProgressCircleProps extends Omit<
  HTMLAttributes<HTMLDivElement>,
  'children' | 'role'
> {
  /**
   * Progress from 0 to `max`. Leave it out (or pass `null`) for an
   * indeterminate ring that spins until the work finishes.
   */
  value?: number | null;
  /** The value that means done. Default 100. */
  max?: number;
  /** Diameter: `xs` 1rem, `sm` 1.25rem, `md` 2rem, `lg` 3rem, `xl` 4.5rem. Default `md`. */
  size?: ProgressCircleSize;
  /** Ring colour. Default `accent`. */
  tone?: ProgressCircleTone;
  /** Accessible name, e.g. "Uploading report.pdf". Default `messages.progress` or `messages.loading`. */
  label?: string;
  /** Show the value text in the middle of the ring (from `lg` up). Default `false`. */
  showValue?: boolean;
  /**
   * Turns the value into text for `aria-valuetext` and the centre label.
   * Default: a whole percentage formatted for `locale` ("42%").
   */
  formatValue?: ((value: number, max: number) => string) | undefined;
  /** BCP 47 locale for the default percentage. Default: the runtime locale. */
  locale?: string;
  /** Replaces any of the default strings. */
  messages?: Partial<ProgressCircleMessages>;
}

/** A whole-percentage formatter; a locale the runtime rejects falls back to its default instead of throwing. */
function percentFormat(locale: string | undefined): Intl.NumberFormat {
  const options: Intl.NumberFormatOptions = { style: 'percent', maximumFractionDigits: 0 };
  try {
    return new Intl.NumberFormat(locale, options);
  } catch {
    return new Intl.NumberFormat(undefined, options);
  }
}

/**
 * A circular progress indicator. With a `value` it is a determinate ring
 * (`role="progressbar"` with `aria-valuenow` and a readable
 * `aria-valuetext`); without one it spins as an indeterminate loader. When
 * the person prefers reduced motion the indeterminate ring stands still.
 * Server-component safe.
 */
export const ProgressCircle = forwardRef<HTMLDivElement, ProgressCircleProps>(
  function ProgressCircle(
    {
      value,
      max = 100,
      size = 'md',
      tone = 'accent',
      label,
      showValue = false,
      formatValue,
      locale,
      messages: messagesProp,
      className,
      ...rest
    },
    ref,
  ) {
    const messages = { ...DEFAULT_MESSAGES, ...messagesProp };
    const safeMax = Number.isFinite(max) && max > 0 ? max : 100;
    const determinate = typeof value === 'number' && Number.isFinite(value);
    const clamped = determinate ? Math.min(Math.max(value, 0), safeMax) : 0;
    const fraction = clamped / safeMax;
    const text = determinate
      ? formatValue
        ? formatValue(clamped, safeMax)
        : percentFormat(locale).format(fraction)
      : undefined;
    const state = !determinate ? 'indeterminate' : fraction >= 1 ? 'complete' : 'loading';
    return (
      <div
        ref={ref}
        role="progressbar"
        aria-label={
          rest['aria-labelledby']
            ? undefined
            : (label ?? (determinate ? messages.progress : messages.loading))
        }
        aria-valuemin={determinate ? 0 : undefined}
        aria-valuemax={determinate ? safeMax : undefined}
        aria-valuenow={determinate ? clamped : undefined}
        aria-valuetext={text}
        {...rest}
        data-state={state}
        data-size={size}
        data-tone={tone}
        className={cx('mn-progress-circle', className)}
      >
        <svg className="mn-progress-circle__svg" viewBox="0 0 24 24" aria-hidden focusable="false">
          <circle className="mn-progress-circle__track" cx="12" cy="12" r="10" />
          <circle
            className="mn-progress-circle__indicator"
            cx="12"
            cy="12"
            r="10"
            pathLength={100}
            // Start at twelve o'clock.
            transform="rotate(-90 12 12)"
            strokeDasharray={determinate ? `${fraction * 100} 100` : '28 100'}
          />
        </svg>
        {showValue && text ? (
          <span className="mn-progress-circle__value" aria-hidden>
            {text}
          </span>
        ) : null}
      </div>
    );
  },
);
