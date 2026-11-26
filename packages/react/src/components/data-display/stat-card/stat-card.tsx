import {
  forwardRef,
  type CSSProperties,
  type ElementType,
  type HTMLAttributes,
  type ReactNode,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { formatNumber } from '../../../internal/format-number.js';
import { InfoGlyph, WarningGlyph } from '../../../internal/icons.js';
import { IconButton } from '../../actions/button/index.js';
import { Money } from '../../billing/money/index.js';
import { DeltaIndicator, type DeltaIndicatorProps } from '../../charts/delta-indicator/index.js';
import { InlineErrorRetry } from '../../feedback/inline-error-retry/index.js';
import { Skeleton } from '../../feedback/skeleton/index.js';
import { VisuallyHidden } from '../../layout/visually-hidden/index.js';
import { Popover, PopoverContent, PopoverTrigger } from '../../overlays/popover/index.js';

export interface StatCardProps extends HTMLAttributes<HTMLDivElement> {
  label: ReactNode;
  /**
   * The value. Pre-formatted text ("₹12.4L", "143"), or a number formatted by
   * `format`. `null` or `undefined` shows the empty state.
   */
  value: ReactNode;
  /** Change versus the previous period, e.g. "+12%". For a computed, polarity-aware change use `change`. */
  delta?: ReactNode;
  /** Direction of the change; `up` is success unless `deltaTone` says otherwise. */
  deltaDirection?: 'up' | 'down' | 'flat';
  /** Override the colour of the delta, e.g. `danger` when up is bad (churn). */
  deltaTone?: 'success' | 'danger' | 'neutral';
  /** Period label, e.g. "vs last 30 days". With `change`, it becomes the change's spoken comparison. */
  caption?: ReactNode;
  /** Sparkline or icon slot. */
  visual?: ReactNode;

  /**
   * A DeltaIndicator in the footer, e.g. `{ value: 0.12 }` or
   * `{ current, previous, goodDirection: 'down' }`. Replaces `delta`; `caption`
   * is used as its comparison unless it has its own.
   */
  change?: Omit<DeltaIndicatorProps, 'ref'>;
  /**
   * Formats a numeric `value` with `Intl.NumberFormat`: `number`, `percent`
   * (a ratio: 0.12 is 12%) or `currency` (through Money, needs `currency`).
   * Without it a number is shown as given.
   */
  format?: 'number' | 'percent' | 'currency';
  /** ISO 4217 code for `format="currency"`. */
  currency?: string;
  /** BCP 47 locale for `format`. Default: the runtime locale. */
  locale?: string;
  /** `compact` writes 1.2K or $1.2M and gives the full value to assistive technology. Default `standard`. */
  notation?: 'standard' | 'compact';
  /** Extra `Intl.NumberFormat` options for `format`, e.g. `{ maximumFractionDigits: 1 }`. */
  formatOptions?: Intl.NumberFormatOptions;
  /** Unit shown smaller after the value, e.g. `ms` or `GB`. */
  unit?: ReactNode;
  /** A sub-line under the value, e.g. "38 of 52 people" or "Unique: 1,204". */
  hint?: ReactNode;
  /**
   * How the metric is defined, shown in a popover from an info button next to
   * the label, e.g. "Median time from ticket creation to first agent reply."
   */
  help?: ReactNode;
  /** Accessible name of the help button. Default "About {label}" when `label` is text, else "About this metric". */
  helpLabel?: string;
  /** Decorative icon before the label. */
  icon?: ReactNode;
  /** A trend line under the value (for example a Sparkline). Decorative: the value and change carry the data. */
  sparkline?: ReactNode;
  /**
   * Colour role for thresholds or emphasis. `accent` highlights the card;
   * `success`, `warning` and `danger` colour the value. Pair it with words in
   * `hint`: colour is not announced.
   */
  tone?: 'neutral' | 'accent' | 'success' | 'warning' | 'danger';
  /** `sm` is compact (dense rows), `md` (default), `lg` is a hero figure. */
  size?: 'sm' | 'md' | 'lg';
  /** `card` (default) has a border and surface; `plain` has neither, for use inside another surface. */
  variant?: 'card' | 'plain';
  /** Drill-down: the label becomes a link and the whole card is its click target. */
  href?: string;
  /** Component used for `href`, e.g. a router link. Default `a`. */
  linkComponent?: ElementType;
  /** Shows a placeholder for the value and sets `aria-busy`. */
  loading?: boolean;
  /** Spoken for a missing value, shown as a dash. Default "No data". */
  emptyLabel?: string;
  /** Replaces the value with this error message. */
  error?: ReactNode;
  /** With `error`, shows Try again (InlineErrorRetry) calling this. */
  onRetry?: () => unknown;
  /**
   * `text` (default) renders paragraphs. `description-list` renders the
   * label as `<dt>` and the value lines as `<dd>` inside a `<dl>`.
   */
  semantics?: 'text' | 'description-list';
}

function isEmpty(value: ReactNode) {
  return value === null || value === undefined;
}

function FormattedValue({
  value,
  format,
  currency,
  locale,
  notation,
  formatOptions,
}: {
  [K in 'value' | 'format' | 'currency' | 'locale' | 'notation' | 'formatOptions']:
    StatCardProps[K] | undefined;
}) {
  if (format === 'currency' && (typeof value === 'number' || typeof value === 'string')) {
    return (
      <Money
        amount={value}
        currency={currency ?? ''}
        {...(locale !== undefined && { locale })}
        {...(notation !== undefined && { notation })}
        {...(formatOptions?.maximumFractionDigits !== undefined && {
          maximumFractionDigits: formatOptions.maximumFractionDigits,
        })}
        {...(formatOptions?.minimumFractionDigits !== undefined && {
          minimumFractionDigits: formatOptions.minimumFractionDigits,
        })}
      />
    );
  }
  if ((format === 'number' || format === 'percent') && typeof value === 'number') {
    const opts = {
      kind: format,
      ...(locale !== undefined && { locale }),
      ...(formatOptions !== undefined && { options: formatOptions }),
    };
    const shown = formatNumber(value, { ...opts, notation: notation ?? 'standard' });
    if (notation === 'compact') {
      const full = formatNumber(value, opts);
      if (full !== shown) {
        return (
          <span title={full}>
            <span aria-hidden>{shown}</span>
            <VisuallyHidden>{full}</VisuallyHidden>
          </span>
        );
      }
    }
    return <>{shown}</>;
  }
  return <>{value}</>;
}

/**
 * A single KPI for dashboards: label, value, change, comparison period and
 * optional hint, help, sparkline and drill-down link. Group several with
 * StatCardGroup.
 */
export const StatCard = forwardRef<HTMLDivElement, StatCardProps>(function StatCard(
  {
    label,
    value,
    delta,
    deltaDirection = 'flat',
    deltaTone,
    caption,
    visual,
    change,
    format,
    currency,
    locale,
    notation,
    formatOptions,
    unit,
    hint,
    help,
    helpLabel,
    icon,
    sparkline,
    tone,
    size,
    variant,
    href,
    linkComponent: LinkComponent = 'a',
    loading = false,
    emptyLabel = 'No data',
    error,
    onRetry,
    semantics = 'text',
    className,
    ...rest
  },
  ref,
) {
  const dl = semantics === 'description-list';
  const Body = dl ? 'dl' : 'div';
  const Term = dl ? 'dt' : 'p';
  const hasError = !loading && error !== undefined && error !== null && error !== false;
  const blockValue = loading || (hasError && onRetry !== undefined);
  const Line = dl ? 'dd' : 'p';
  const ValueEl = dl ? 'dd' : blockValue ? 'div' : 'p';
  const SlotEl = dl ? 'dd' : 'div';

  const legacyTone =
    deltaTone ??
    (deltaDirection === 'up' ? 'success' : deltaDirection === 'down' ? 'danger' : 'neutral');
  const srDirection =
    deltaDirection === 'up' ? 'increase' : deltaDirection === 'down' ? 'decrease' : 'no change';

  const resolvedHelpLabel =
    helpLabel ??
    (typeof label === 'string' || typeof label === 'number'
      ? `About ${label}`
      : 'About this metric');

  const labelContent = href ? (
    <LinkComponent className="mn-stat__link" href={href}>
      {label}
    </LinkComponent>
  ) : (
    label
  );

  let valueContent: ReactNode;
  if (loading) {
    valueContent = <Skeleton shape="text" width="5ch" className="mn-stat__skeleton" />;
  } else if (hasError) {
    valueContent = onRetry ? (
      <InlineErrorRetry onRetry={onRetry}>{error}</InlineErrorRetry>
    ) : (
      <span className="mn-stat__error">
        <WarningGlyph />
        {error}
      </span>
    );
  } else if (isEmpty(value)) {
    valueContent = (
      <>
        <span aria-hidden>—</span>
        <VisuallyHidden>{emptyLabel}</VisuallyHidden>
      </>
    );
  } else {
    valueContent = (
      <>
        <FormattedValue
          value={value}
          format={format}
          currency={currency}
          locale={locale}
          notation={notation}
          formatOptions={formatOptions}
        />
        {unit !== undefined && unit !== null ? <span className="mn-stat__unit">{unit}</span> : null}
      </>
    );
  }

  const showFooter = !loading && !hasError;
  let footer: ReactNode = null;
  if (showFooter && change) {
    footer = (
      <Line className="mn-stat__footer">
        <DeltaIndicator
          size={size === 'lg' ? 'md' : 'sm'}
          {...change}
          comparison={change.comparison ?? caption}
          className={cx('mn-stat__change', change.className)}
        />
      </Line>
    );
  } else if (showFooter && (delta !== undefined || caption)) {
    footer = (
      <Line className="mn-stat__footer">
        {delta !== undefined ? (
          <span className="mn-stat__delta" data-tone={legacyTone} data-direction={deltaDirection}>
            <span aria-hidden>
              {deltaDirection === 'up' ? '↑' : deltaDirection === 'down' ? '↓' : '→'}
            </span>
            <VisuallyHidden>{srDirection} </VisuallyHidden>
            {delta}
          </span>
        ) : null}
        {caption ? <span className="mn-stat__caption">{caption}</span> : null}
      </Line>
    );
  }

  const state = loading ? 'loading' : hasError ? 'error' : isEmpty(value) ? 'empty' : undefined;

  return (
    <div
      ref={ref}
      {...rest}
      className={cx('mn-stat', className)}
      data-size={size}
      data-variant={variant}
      data-tone={tone}
      data-state={state}
      data-linked={href ? '' : undefined}
      aria-busy={loading || rest['aria-busy'] || undefined}
    >
      <Body className="mn-stat__body">
        <Term className="mn-stat__label">
          {icon ? (
            <span className="mn-stat__icon" aria-hidden>
              {icon}
            </span>
          ) : null}
          {icon || help ? (
            <span className="mn-stat__label-text">{labelContent}</span>
          ) : (
            labelContent
          )}
          {help ? (
            <Popover>
              <PopoverTrigger asChild>
                <IconButton
                  className="mn-stat__help"
                  size="xs"
                  label={resolvedHelpLabel}
                  icon={<InfoGlyph />}
                />
              </PopoverTrigger>
              <PopoverContent className="mn-stat__help-content">{help}</PopoverContent>
            </Popover>
          ) : null}
        </Term>
        <ValueEl className="mn-stat__value">{valueContent}</ValueEl>
        {footer}
        {hint && !loading && !hasError ? <Line className="mn-stat__hint">{hint}</Line> : null}
        {sparkline && !loading && !hasError ? (
          <SlotEl className="mn-stat__sparkline" aria-hidden>
            {sparkline}
          </SlotEl>
        ) : null}
      </Body>
      {visual ? (
        <div className="mn-stat__visual" aria-hidden>
          {visual}
        </div>
      ) : null}
    </div>
  );
});

export interface StatCardGroupProps extends HTMLAttributes<HTMLDivElement> {
  /** Names the group (`role="group"`), e.g. "This month". */
  label?: string;
  /**
   * Columns on wide screens (from 48rem). Narrower screens use two columns,
   * then one. Default: as many as fit at `minItemWidth`.
   */
  columns?: number;
  /** Narrowest a card gets in the automatic layout. Default `12rem`. */
  minItemWidth?: string;
}

/** A responsive grid of StatCards (a KPI row). */
export const StatCardGroup = forwardRef<HTMLDivElement, StatCardGroupProps>(function StatCardGroup(
  { label, columns, minItemWidth, className, style, ...rest },
  ref,
) {
  const vars: Record<string, string | number> = {};
  if (columns !== undefined) {
    const n = Math.max(1, Math.floor(columns));
    vars['--mn-stat-group-columns'] = n;
    vars['--mn-stat-group-columns-narrow'] = Math.min(2, n);
  }
  if (minItemWidth !== undefined) vars['--mn-stat-group-min'] = minItemWidth;
  return (
    <div
      ref={ref}
      role={label ? 'group' : undefined}
      aria-label={label}
      {...rest}
      className={cx('mn-stat-group', className)}
      data-columns={columns !== undefined ? '' : undefined}
      style={{ ...(vars as CSSProperties), ...style }}
    />
  );
});
