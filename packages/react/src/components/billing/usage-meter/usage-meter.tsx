'use client';

import { forwardRef, useId, type HTMLAttributes, type ReactNode } from 'react';
import { cx } from '../../../internal/cx.js';
import { ErrorGlyph, WarningGlyph } from '../../../internal/icons.js';
import { Progress } from '../../data-display/progress-bar/index.js';
import { InlineErrorRetry } from '../../feedback/inline-error-retry/index.js';
import { Skeleton } from '../../feedback/skeleton/index.js';

/** A date as a `Date`, epoch milliseconds or an ISO 8601 string. */
export type UsageDate = Date | number | string;

/** One metered quantity: requests, messages, storage, seats, minutes. */
export interface UsageMeterItem {
  /** Stable id, used as the React key. */
  id: string;
  /** What is measured, e.g. "Requests". Also the meter's accessible name. */
  label: string;
  /** Amount used in the period. */
  used: number;
  /** The limit or quota. Omit or `null` for a plain count with no meter. */
  limit?: number | null;
  /** Unit written after the figures, e.g. "GB" or "minutes". */
  unit?: string;
  /** One line under the figure, e.g. what counts as a unit. */
  description?: ReactNode;
  /** Formats `used` and `limit`. Default: `Intl.NumberFormat` in `locale` and `notation`. */
  format?: ((value: number) => string) | undefined;
}

/** The level a metered item has reached. */
export type UsageLevel = 'normal' | 'warning' | 'over' | 'unlimited';

export interface UsageMeterMessages {
  /** "of 10,000" after the used figure. */
  of: (limit: string) => string;
  /** Meter value text: "7,200 of 10,000 GB used". */
  valueText: (used: string, limit: string, unit: string | undefined) => string;
  /** "82% used". */
  percent: (percent: number) => string;
  nearLimit: string;
  overLimit: string;
  /** "Resets 1 Dec 2026". */
  resets: (date: string) => string;
  /** Live announcement when items reach a level, e.g. "Requests: over the limit". */
  announce: (label: string, level: 'warning' | 'over') => string;
  error: string;
  loading: string;
}

const DEFAULT_MESSAGES: UsageMeterMessages = {
  of: (l) => `of ${l}`,
  valueText: (u, l, unit) => `${u} of ${l}${unit ? ` ${unit}` : ''} used`,
  percent: (p) => `${p}% used`,
  nearLimit: 'Near the limit',
  overLimit: 'Over the limit',
  resets: (d) => `Resets ${d}`,
  announce: (label, level) => `${label}: ${level === 'over' ? 'over the limit' : 'near the limit'}`,
  error: 'Usage could not be loaded.',
  loading: 'Loading usage',
};

/** The level for `used` against `limit` with `warning` as a fraction of the limit (0.8 = 80%). */
export function usageLevel(
  used: number,
  limit: number | null | undefined,
  warning = 0.8,
): UsageLevel {
  if (limit === null || limit === undefined || !(limit > 0)) return 'unlimited';
  if (used > limit) return 'over';
  if (used >= limit * warning) return 'warning';
  return 'normal';
}

export interface UsageMeterProps extends Omit<HTMLAttributes<HTMLElement>, 'children' | 'title'> {
  /** The metered items, in display order. */
  items: readonly UsageMeterItem[];
  /** Heading. Omit when the page heading already names the section; then pass `aria-label`. */
  title?: ReactNode;
  /** Heading level. Default `h2`. */
  headingLevel?: 'h2' | 'h3' | 'h4';
  /** The period measured, e.g. "This month" or "Last 7 days". Shown next to the heading. */
  period?: ReactNode;
  /** When the quota resets. */
  resetsAt?: UsageDate | null;
  /** Fraction of the limit at which an item is "near the limit". Default 0.8. */
  warningAt?: number;
  /** `grid` (default) lays items out as tiles; `list` stacks them in rows. */
  layout?: 'grid' | 'list';
  /** Number notation for the default formatter. `compact` writes 1.2K. Default `standard`. */
  notation?: 'standard' | 'compact';
  /** BCP 47 locale for numbers and dates. Default: the runtime locale. */
  locale?: string;
  /** IANA time zone for `resetsAt`. */
  timeZone?: string;
  /** Header actions, e.g. a range picker or an "Upgrade" link. */
  actions?: ReactNode;
  /** Shows skeleton tiles and sets `aria-busy`. */
  loading?: boolean;
  /** Shows an inline error with a retry button. */
  error?: ReactNode | boolean;
  onRetry?: (() => unknown) | undefined;
  /** Visible text overrides; English defaults. */
  messages?: Partial<UsageMeterMessages>;
}

function toDate(v: UsageDate): Date {
  return v instanceof Date ? v : new Date(v);
}

/**
 * Usage against limits: each metered item shows what was used, the limit and
 * a quota meter that turns to warning near the limit and danger over it, with
 * text so colour is never the only signal. Items without a limit show the
 * count alone. Changes in level are announced politely.
 */
export const UsageMeter = forwardRef<HTMLElement, UsageMeterProps>(function UsageMeter(
  {
    items,
    title,
    headingLevel: Heading = 'h2',
    period,
    resetsAt,
    warningAt = 0.8,
    layout = 'grid',
    notation = 'standard',
    locale,
    timeZone,
    actions,
    loading = false,
    error,
    onRetry,
    messages,
    className,
    'aria-labelledby': ariaLabelledBy,
    ...rest
  },
  ref,
) {
  const m = { ...DEFAULT_MESSAGES, ...messages };
  const headingId = useId();
  const number = new Intl.NumberFormat(locale, { notation, maximumFractionDigits: 1 });
  const hasError = error !== undefined && error !== false && error !== null;
  const resets = resetsAt
    ? new Intl.DateTimeFormat(locale, {
        dateStyle: 'medium',
        ...(timeZone !== undefined && { timeZone }),
      }).format(toDate(resetsAt))
    : null;
  const levels = items.map((item) => usageLevel(item.used, item.limit, warningAt));
  const announcement = items
    .map((item, i) => {
      const level = levels[i];
      return level === 'over' || level === 'warning' ? m.announce(item.label, level) : null;
    })
    .filter(Boolean)
    .join('. ');

  let body: ReactNode;
  if (hasError) {
    const errorText = error === true ? m.error : error;
    body = onRetry ? (
      <InlineErrorRetry onRetry={onRetry}>{errorText}</InlineErrorRetry>
    ) : (
      <p className="mn-usage-meter__error" role="alert">
        {errorText}
      </p>
    );
  } else if (loading) {
    body = (
      <div className="mn-usage-meter__items" role="status" aria-label={m.loading}>
        {[0, 1, 2].map((i) => (
          <Skeleton key={i} shape="rect" height="5.5rem" />
        ))}
      </div>
    );
  } else {
    body = (
      <ul className="mn-usage-meter__items">
        {items.map((item, i) => {
          const level = levels[i] ?? 'unlimited';
          const fmt = item.format ?? ((v: number) => number.format(v));
          const used = fmt(item.used);
          const limited = level !== 'unlimited';
          const limit = limited ? fmt(item.limit as number) : '';
          const pct = limited ? Math.round((item.used / (item.limit as number)) * 100) : 0;
          const valueId = `${headingId}-${item.id}`;
          return (
            <li key={item.id} className="mn-usage-meter__item" data-level={level}>
              <span className="mn-usage-meter__label" id={valueId}>
                {item.label}
              </span>
              <span className="mn-usage-meter__value">
                <span className="mn-usage-meter__used">{used}</span>
                {limited ? (
                  <span className="mn-usage-meter__limit">
                    {' '}
                    {m.of(limit)}
                    {item.unit ? ` ${item.unit}` : null}
                  </span>
                ) : item.unit ? (
                  <span className="mn-usage-meter__limit"> {item.unit}</span>
                ) : null}
              </span>
              {limited ? (
                <>
                  <Progress
                    semantics="meter"
                    size="sm"
                    value={item.used}
                    max={item.limit as number}
                    aria-labelledby={valueId}
                    thresholds={{ warning: (item.limit as number) * warningAt }}
                    valueText={m.valueText(used, limit, item.unit)}
                    className="mn-usage-meter__meter"
                  />
                  <span className="mn-usage-meter__status">
                    {level === 'over' ? (
                      <ErrorGlyph className="mn-usage-meter__icon" aria-hidden />
                    ) : level === 'warning' ? (
                      <WarningGlyph className="mn-usage-meter__icon" aria-hidden />
                    ) : null}
                    {level === 'over'
                      ? m.overLimit
                      : level === 'warning'
                        ? m.nearLimit
                        : m.percent(pct)}
                    {level !== 'normal' ? (
                      <span className="mn-usage-meter__percent"> · {m.percent(pct)}</span>
                    ) : null}
                  </span>
                </>
              ) : null}
              {item.description ? (
                <span className="mn-usage-meter__description">{item.description}</span>
              ) : null}
            </li>
          );
        })}
      </ul>
    );
  }

  return (
    <section
      ref={ref}
      aria-labelledby={ariaLabelledBy ?? (title ? headingId : undefined)}
      {...rest}
      aria-busy={loading || undefined}
      data-layout={layout}
      data-loading={loading || undefined}
      data-error={hasError || undefined}
      className={cx('mn-usage-meter', className)}
    >
      {title || period || resets || actions ? (
        <div className="mn-usage-meter__header">
          <div className="mn-usage-meter__heading">
            {title ? (
              <Heading id={headingId} className="mn-usage-meter__title">
                {title}
              </Heading>
            ) : null}
            {period || resets ? (
              <p className="mn-usage-meter__period">
                {period}
                {period && resets ? ' · ' : null}
                {resets ? m.resets(resets) : null}
              </p>
            ) : null}
          </div>
          {actions ? <div className="mn-usage-meter__actions">{actions}</div> : null}
        </div>
      ) : null}
      {body}
      <span className="mn-visually-hidden" role="status" aria-live="polite">
        {loading || hasError ? '' : announcement}
      </span>
    </section>
  );
});
