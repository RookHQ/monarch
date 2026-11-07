'use client';

import { forwardRef, useId, type HTMLAttributes, type ReactNode } from 'react';
import { cx } from '../../../internal/cx.js';
import { Money, formatMoney, type MoneyAmount } from '../money/index.js';
import { StatusBadge, type StatusRegistry } from '../../data-display/status-badge/index.js';
import { EmptyState } from '../../feedback/empty-state/index.js';
import { InlineErrorRetry } from '../../feedback/inline-error-retry/index.js';
import { Skeleton } from '../../feedback/skeleton/index.js';

/** An amount in one currency. */
export interface SavingsMoney {
  amount: MoneyAmount;
  currency: string;
}

/** A date as a `Date`, epoch milliseconds or an ISO 8601 string. */
export type SavingsDate = Date | number | string;

/** A group of unused items, e.g. "Parked" or "No website". */
export interface SavingsCategory {
  /** Stable id; also the status key looked up in `statuses`. */
  id: string;
  /** Visible label. Default: the status registry's label for `id`. */
  label?: string;
  /** Number of items in the group. Groups with 0 are not shown. */
  count: number;
  /** Opens the filtered list for this group. */
  href?: string;
}

/** A count of items per decision, e.g. keep / review / let lapse / undecided. */
export interface SavingsDecisionCount {
  id: string;
  label: string;
  count: number;
}

/** One unused item. */
export interface SavingsItem {
  id: string;
  /** Item name, e.g. the asset or subscription. */
  name: ReactNode;
  /** Opens the item. */
  href?: string;
  /** Category status key, shown as a badge. */
  category?: string;
  /** Next renewal or expiry date. */
  date?: SavingsDate | null;
  /** What renewing it costs. `null`: unpriced. */
  amount?: SavingsMoney | null;
  /** Decision status key, shown as a badge, e.g. "keep" or "let_lapse". */
  decision?: string;
}

export interface SavingsPanelMessages {
  /** Heading. Default "Savings". */
  title: string;
  /** The headline: count of unused items, their renewal cost (already formatted) and the unpriced count. */
  headline: (count: number, cost: string | null, unpriced: number) => string;
  /** Accessible name of the category list. */
  categories: string;
  /** Accessible name of the decision summary. */
  decisions: string;
  /** "Choosing to let these lapse saves $120.00". */
  potentialSavings: (amount: string) => string;
  /** Accessible name of the item list. */
  items: string;
  /** "and 4 more". */
  more: (count: number) => string;
  unpriced: string;
  emptyTitle: string;
  emptyDescription: string;
  error: string;
  loading: string;
}

const DEFAULT_MESSAGES: SavingsPanelMessages = {
  title: 'Savings',
  headline: (count, cost, unpriced) =>
    `${count} unused ${count === 1 ? 'item' : 'items'}` +
    (cost ? ` cost ${cost} to renew` : '') +
    (unpriced ? ` (${unpriced} without a price)` : '') +
    '.',
  categories: 'Unused by reason',
  decisions: 'Decisions',
  potentialSavings: (a) => `Letting the marked items lapse saves ${a}.`,
  items: 'Unused items',
  more: (n) => `and ${n} more`,
  unpriced: 'No price',
  emptyTitle: 'Nothing unused',
  emptyDescription: 'Everything renewing is in use.',
  error: 'Savings could not be loaded.',
  loading: 'Loading savings',
};

export interface RenderSavingsLinkProps {
  href: string;
  className: string;
  children: ReactNode;
}

export interface SavingsPanelProps extends Omit<HTMLAttributes<HTMLElement>, 'children' | 'title'> {
  /** Number of unused items. 0 shows the empty state. */
  unusedCount: number;
  /** Renewal cost of the unused items, one entry per currency. Never converted or added across currencies. */
  costs?: readonly SavingsMoney[];
  /** Unused items without a price; counted, never estimated. */
  unpricedCount?: number;
  /** Unused items grouped by reason. */
  categories?: readonly SavingsCategory[];
  /** Counts per decision. */
  decisions?: readonly SavingsDecisionCount[];
  /** What letting the marked items lapse would save, per currency. */
  potentialSavings?: readonly SavingsMoney[];
  /** The unused items to list, most urgent first. */
  items?: readonly SavingsItem[];
  /** Show at most this many items. Default 6. */
  maxItems?: number;
  /** Status registry for category and decision badges. */
  statuses?: StatusRegistry;
  /** Heading. Default `messages.title`. */
  title?: ReactNode;
  /** Heading level. Default `h2`. */
  headingLevel?: 'h2' | 'h3' | 'h4';
  /** Help text under the heading. */
  description?: ReactNode;
  /** Header action, e.g. a "Review all" link. */
  actions?: ReactNode;
  /** Icon for the empty state. */
  emptyIcon?: ReactNode;
  /** Amounts are in minor units. Default false. */
  minorUnits?: boolean;
  /** BCP 47 locale for amounts and dates. */
  locale?: string;
  /** IANA time zone for dates. */
  timeZone?: string;
  /** Shows a skeleton and sets `aria-busy`. */
  loading?: boolean;
  /** Shows an inline error with a retry button. */
  error?: ReactNode | boolean;
  onRetry?: (() => unknown) | undefined;
  /** Renders links, e.g. with a router's Link. Default a plain `<a>`. */
  renderLink?: ((props: RenderSavingsLinkProps) => ReactNode) | undefined;
  /** Visible text overrides; English defaults. */
  messages?: Partial<SavingsPanelMessages>;
}

const defaultLink = ({ href, className, children }: RenderSavingsLinkProps) => (
  <a href={href} className={className}>
    {children}
  </a>
);

function toDate(v: SavingsDate): Date {
  return v instanceof Date ? v : new Date(v);
}

/**
 * What unused items cost to keep: a headline count and renewal cost per
 * currency, the reasons they count as unused, the decisions made so far and
 * the top items. Only real prices are added up; unpriced items are counted.
 */
export const SavingsPanel = forwardRef<HTMLElement, SavingsPanelProps>(function SavingsPanel(
  {
    unusedCount,
    costs = [],
    unpricedCount = 0,
    categories,
    decisions,
    potentialSavings,
    items,
    maxItems = 6,
    statuses,
    title,
    headingLevel: Heading = 'h2',
    description,
    actions,
    emptyIcon,
    minorUnits = false,
    locale,
    timeZone,
    loading = false,
    error,
    onRetry,
    renderLink = defaultLink,
    messages,
    className,
    ...rest
  },
  ref,
) {
  const m = { ...DEFAULT_MESSAGES, ...messages };
  const headingId = useId();
  const opts = { minorUnits, ...(locale !== undefined && { locale }) };
  const join = (list: readonly SavingsMoney[]) =>
    list.map((x) => formatMoney(x.amount, x.currency, opts)).join(' + ');
  const dateFormat = new Intl.DateTimeFormat(locale, {
    dateStyle: 'medium',
    ...(timeZone !== undefined && { timeZone }),
  });
  const hasError = error !== undefined && error !== false && error !== null;
  const shownCategories = categories?.filter((c) => c.count > 0) ?? [];
  const shownItems = items?.slice(0, Math.max(0, maxItems)) ?? [];
  const hiddenItems = (items?.length ?? 0) - shownItems.length;

  let body: ReactNode;
  if (hasError) {
    const errorText = error === true ? m.error : error;
    body = onRetry ? (
      <InlineErrorRetry onRetry={onRetry}>{errorText}</InlineErrorRetry>
    ) : (
      <p className="mn-savings-panel__error" role="alert">
        {errorText}
      </p>
    );
  } else if (loading) {
    body = (
      <div className="mn-savings-panel__loading" role="status" aria-label={m.loading}>
        <Skeleton shape="text" width="60%" />
        <Skeleton shape="rect" height="6rem" />
      </div>
    );
  } else if (unusedCount === 0) {
    body = (
      <EmptyState
        size="compact"
        icon={emptyIcon}
        title={m.emptyTitle}
        description={m.emptyDescription}
      />
    );
  } else {
    body = (
      <div className="mn-savings-panel__content">
        <p className="mn-savings-panel__headline">
          {m.headline(unusedCount, costs.length ? join(costs) : null, unpricedCount)}
        </p>
        {shownCategories.length > 0 ? (
          <ul className="mn-savings-panel__categories" aria-label={m.categories}>
            {shownCategories.map((c) => {
              const chip = (
                <>
                  <StatusBadge
                    status={c.id}
                    {...(statuses && { statuses })}
                    {...(c.label !== undefined && { label: c.label })}
                    size="sm"
                  />
                  <span className="mn-savings-panel__count">{c.count}</span>
                </>
              );
              return (
                <li key={c.id}>
                  {c.href ? (
                    renderLink({
                      href: c.href,
                      className: 'mn-savings-panel__category',
                      children: chip,
                    })
                  ) : (
                    <span className="mn-savings-panel__category">{chip}</span>
                  )}
                </li>
              );
            })}
          </ul>
        ) : null}
        {decisions && decisions.length > 0 ? (
          <p className="mn-savings-panel__decisions">
            <span className="mn-visually-hidden">{m.decisions}: </span>
            {decisions.map((d) => `${d.label} ${d.count}`).join(' · ')}
          </p>
        ) : null}
        {potentialSavings && potentialSavings.length > 0 ? (
          <p className="mn-savings-panel__savings">{m.potentialSavings(join(potentialSavings))}</p>
        ) : null}
        {shownItems.length > 0 ? (
          <ul className="mn-savings-panel__items" aria-label={m.items}>
            {shownItems.map((i) => (
              <li key={i.id} className="mn-savings-panel__item">
                {i.href ? (
                  renderLink({
                    href: i.href,
                    className: 'mn-savings-panel__name',
                    children: i.name,
                  })
                ) : (
                  <span className="mn-savings-panel__name">{i.name}</span>
                )}
                <span className="mn-savings-panel__facts">
                  {i.category ? (
                    <StatusBadge
                      status={i.category}
                      {...(statuses && { statuses })}
                      size="sm"
                      variant="outline"
                    />
                  ) : null}
                  {i.date ? (
                    <time
                      className="mn-savings-panel__date"
                      dateTime={toDate(i.date).toISOString()}
                    >
                      {dateFormat.format(toDate(i.date))}
                    </time>
                  ) : null}
                  {i.amount ? (
                    <Money amount={i.amount.amount} currency={i.amount.currency} {...opts} />
                  ) : i.amount === null ? (
                    <span className="mn-savings-panel__muted">{m.unpriced}</span>
                  ) : null}
                  {i.decision ? (
                    <StatusBadge status={i.decision} {...(statuses && { statuses })} size="sm" />
                  ) : null}
                </span>
              </li>
            ))}
          </ul>
        ) : null}
        {hiddenItems > 0 ? <p className="mn-savings-panel__more">{m.more(hiddenItems)}</p> : null}
      </div>
    );
  }

  return (
    <section
      ref={ref}
      aria-labelledby={headingId}
      {...rest}
      aria-busy={loading || undefined}
      data-loading={loading || undefined}
      data-error={hasError || undefined}
      data-empty={(!loading && !hasError && unusedCount === 0) || undefined}
      className={cx('mn-savings-panel', className)}
    >
      <div className="mn-savings-panel__header">
        <div className="mn-savings-panel__heading">
          <Heading id={headingId} className="mn-savings-panel__title">
            {title ?? m.title}
          </Heading>
          {description ? <p className="mn-savings-panel__description">{description}</p> : null}
        </div>
        {actions ? <div className="mn-savings-panel__actions">{actions}</div> : null}
      </div>
      {body}
    </section>
  );
});
