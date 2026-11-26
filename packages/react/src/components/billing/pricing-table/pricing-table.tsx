'use client';

import {
  forwardRef,
  useCallback,
  useId,
  useState,
  type HTMLAttributes,
  type ReactNode,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { CheckGlyph, DashGlyph } from '../../../internal/icons.js';
import { Badge } from '../../data-display/badge/index.js';
import { SegmentedControl } from '../../forms/segmented-control/index.js';
import { VisuallyHidden } from '../../layout/visually-hidden/index.js';

/** A billing period the visitor can switch between, such as monthly and yearly. */
export interface BillingPeriodOption {
  /** Key used in each plan's `prices`. */
  value: string;
  /** Toggle text, e.g. "Monthly". */
  label: string;
  /** Short text after the toggle label, e.g. "2 months included". Keep it factual. */
  suffix?: ReactNode;
  /** Text shown after every price in this period, e.g. "/ month". */
  priceSuffix?: ReactNode;
  /** A line under every price in this period, e.g. "Billed yearly". */
  priceNote?: ReactNode;
}

/** One plan column. Prices are shown exactly as given; nothing is calculated. */
export interface PricingPlan {
  /** Stable id, used as the key in every feature's `values`. */
  id: string;
  /** Plan name. */
  name: ReactNode;
  /** One short line on who the plan is for. */
  description?: ReactNode;
  /** Price per billing period, keyed by `BillingPeriodOption.value`. */
  prices?: Readonly<Record<string, ReactNode>> | undefined;
  /** Price used when `prices` has no entry for the period, e.g. "Contact us". */
  price?: ReactNode;
  /** Replaces the period's `priceSuffix` for this plan, e.g. "per seat / month". */
  priceSuffix?: ReactNode;
  /** Emphasises the column as the recommended plan. */
  highlighted?: boolean | undefined;
  /** Short label for a highlighted plan, such as "Recommended". */
  badge?: ReactNode;
  /** Marks the organisation's current plan. */
  current?: boolean | undefined;
  /** The plan's call to action: one Button or link. */
  action?: ReactNode;
}

/**
 * What a cell shows: `true` is a check ("Included"), `false`, `null` or a
 * missing entry is a dash ("Not included"), anything else is shown as text,
 * e.g. "10 seats" or "Email and chat".
 */
export type PricingFeatureValue = ReactNode;

/** One row of the comparison. */
export interface PricingFeature {
  id: string;
  /** Row heading. */
  label: ReactNode;
  /** A short explanation under the label. */
  description?: ReactNode;
  /** Rows with the same `group` are shown under one group heading, in first-seen order. */
  group?: string | undefined;
  /** Cell per plan id. */
  values: Readonly<Record<string, PricingFeatureValue>>;
}

/** Visible and assistive-technology text. English defaults. */
export interface PricingTableMessages {
  /** Table caption when `caption` is not given. Default "Plan comparison". */
  caption: string;
  /** Hidden header of the feature column. Default "Feature". */
  feature: string;
  /** Hidden text of a check cell. Default "Included". */
  included: string;
  /** Hidden text of a dash cell. Default "Not included". */
  notIncluded: string;
  /** Badge text for `current` plans. Default "Current plan". */
  currentPlan: string;
  /** Accessible name of the billing-period toggle. Default "Billing period". */
  billingPeriod: string;
}

export const defaultPricingTableMessages: PricingTableMessages = {
  caption: 'Plan comparison',
  feature: 'Feature',
  included: 'Included',
  notIncluded: 'Not included',
  currentPlan: 'Current plan',
  billingPeriod: 'Billing period',
};

/** The price a plan shows for a billing period: `prices[period]`, else `price`. */
export function planPriceFor(plan: PricingPlan, period: string | undefined): ReactNode {
  const fromPeriod = period !== undefined ? plan.prices?.[period] : undefined;
  return fromPeriod !== undefined ? fromPeriod : plan.price;
}

/** Feature rows grouped by `group`, keeping first-seen order. Ungrouped rows have `group: undefined`. */
export function groupPricingFeatures(
  features: readonly PricingFeature[],
): { group: string | undefined; features: PricingFeature[] }[] {
  const groups: { group: string | undefined; features: PricingFeature[] }[] = [];
  for (const feature of features) {
    const existing = groups.find((g) => g.group === feature.group);
    if (existing) existing.features.push(feature);
    else groups.push({ group: feature.group, features: [feature] });
  }
  return groups;
}

/** Controlled or uncontrolled billing period, shared by PricingTable and PricingSection. */
export function useBillingPeriod(
  periods: readonly BillingPeriodOption[] | undefined,
  period: string | undefined,
  defaultPeriod: string | undefined,
  onPeriodChange: ((period: string) => void) | undefined,
): [string | undefined, (next: string) => void] {
  const [inner, setInner] = useState(defaultPeriod ?? periods?.[0]?.value);
  const controlled = period !== undefined;
  const current = controlled ? period : inner;
  const set = useCallback(
    (next: string) => {
      if (!controlled) setInner(next);
      onPeriodChange?.(next);
    },
    [controlled, onPeriodChange],
  );
  return [current, set];
}

export interface BillingPeriodToggleProps extends Omit<
  HTMLAttributes<HTMLDivElement>,
  'onChange' | 'defaultValue' | 'dir'
> {
  periods: readonly BillingPeriodOption[];
  /** The selected period value. */
  value: string | undefined;
  onValueChange: (value: string) => void;
  /** Accessible name. Default "Billing period". */
  label?: string | undefined;
}

/**
 * The monthly / yearly switch above a pricing table or plan grid. A radio
 * group (SegmentedControl): one Tab stop, arrow keys move and select.
 */
export const BillingPeriodToggle = forwardRef<HTMLDivElement, BillingPeriodToggleProps>(
  function BillingPeriodToggle(
    {
      periods,
      value,
      onValueChange,
      label,
      'aria-label': ariaLabel,
      'aria-labelledby': labelledBy,
      ...rest
    },
    ref,
  ) {
    return (
      <SegmentedControl
        ref={ref}
        {...rest}
        {...(labelledBy
          ? { 'aria-labelledby': labelledBy }
          : { 'aria-label': ariaLabel ?? label ?? defaultPricingTableMessages.billingPeriod })}
        options={periods.map((p) => ({ value: p.value, label: p.label, suffix: p.suffix }))}
        value={value ?? ''}
        onValueChange={onValueChange}
      />
    );
  },
);

export interface PricingTableProps extends HTMLAttributes<HTMLDivElement> {
  /** Plan columns, in display order. */
  plans: readonly PricingPlan[];
  /** Feature rows. Leave empty to show only the plan header row. */
  features: readonly PricingFeature[];
  /** Billing periods. With two or more, a toggle is shown above the table. */
  periods?: readonly BillingPeriodOption[] | undefined;
  /** Selected period (controlled). */
  period?: string | undefined;
  /** Initial period (uncontrolled). Default: the first period. */
  defaultPeriod?: string | undefined;
  onPeriodChange?: ((period: string) => void) | undefined;
  /** Show the period toggle. Default `true` when there are two or more periods; set `false` when the page renders its own. */
  showPeriodToggle?: boolean | undefined;
  /** Table caption (its accessible name). Default `messages.caption`. */
  caption?: ReactNode;
  /** Hide the caption visually; it stays the accessible name. Default `true`. */
  hideCaption?: boolean | undefined;
  /** Show the price row in the plan headers. Default `true`; `false` for comparisons without prices. */
  showPrices?: boolean | undefined;
  messages?: Partial<PricingTableMessages> | undefined;
}

function isBlank(value: PricingFeatureValue): boolean {
  return value === false || value === null || value === undefined;
}

function Cell({ value, m }: { value: PricingFeatureValue; m: PricingTableMessages }) {
  if (value === true) {
    return (
      <span className="mn-pricing-table__mark" data-value="included">
        <CheckGlyph aria-hidden />
        <VisuallyHidden>{m.included}</VisuallyHidden>
      </span>
    );
  }
  if (isBlank(value)) {
    return (
      <span className="mn-pricing-table__mark" data-value="not-included">
        <DashGlyph aria-hidden />
        <VisuallyHidden>{m.notIncluded}</VisuallyHidden>
      </span>
    );
  }
  return <span className="mn-pricing-table__text">{value}</span>;
}

/**
 * Plans side by side with a feature comparison and an optional monthly /
 * yearly toggle. A real `<table>`: plans are column headers, features are row
 * headers, and check or dash cells carry "Included" / "Not included" text.
 * Prices are shown exactly as given; nothing is calculated or invented.
 */
export const PricingTable = forwardRef<HTMLDivElement, PricingTableProps>(function PricingTable(
  {
    plans,
    features,
    periods,
    period: periodProp,
    defaultPeriod,
    onPeriodChange,
    showPeriodToggle,
    caption,
    hideCaption = true,
    showPrices = true,
    messages,
    className,
    ...rest
  },
  ref,
) {
  const m = { ...defaultPricingTableMessages, ...messages };
  const [period, setPeriod] = useBillingPeriod(periods, periodProp, defaultPeriod, onPeriodChange);
  const captionId = useId();
  const periodOption = periods?.find((p) => p.value === period);
  const toggle = showPeriodToggle ?? (periods !== undefined && periods.length > 1);
  const groups = groupPricingFeatures(features);
  const columns = plans.length + 1;

  return (
    <div
      ref={ref}
      {...rest}
      className={cx('mn-pricing-table', className)}
      data-period={period}
      data-columns={plans.length}
    >
      {toggle && periods ? (
        <div className="mn-pricing-table__toolbar">
          <BillingPeriodToggle
            periods={periods}
            value={period}
            onValueChange={setPeriod}
            label={m.billingPeriod}
          />
        </div>
      ) : null}
      {/* The scroller, not the page, scrolls sideways on narrow screens; it is
          focusable so keyboard users can scroll it (WCAG 2.1.1). */}
      <div
        className="mn-pricing-table__scroller"
        role="region"
        aria-labelledby={captionId}
        // eslint-disable-next-line jsx-a11y/no-noninteractive-tabindex
        tabIndex={0}
      >
        <table className="mn-pricing-table__table">
          <caption
            id={captionId}
            className={hideCaption ? 'mn-visually-hidden' : 'mn-pricing-table__caption'}
          >
            {caption ?? m.caption}
          </caption>
          <thead>
            <tr>
              <th scope="col" className="mn-pricing-table__corner">
                <VisuallyHidden>{m.feature}</VisuallyHidden>
              </th>
              {plans.map((plan) => {
                const price = showPrices ? planPriceFor(plan, period) : undefined;
                const hasPrice = price !== undefined && price !== null;
                // The period's suffix and note belong to period prices, not to a fallback such as "Contact us".
                const periodic = period !== undefined && plan.prices?.[period] !== undefined;
                const suffix =
                  plan.priceSuffix ?? (periodic ? periodOption?.priceSuffix : undefined);
                return (
                  <th
                    key={plan.id}
                    scope="col"
                    className="mn-pricing-table__plan"
                    data-highlighted={plan.highlighted || undefined}
                    data-current={plan.current || undefined}
                  >
                    <span className="mn-pricing-table__plan-name">{plan.name}</span>
                    {plan.badge || plan.current ? (
                      <span className="mn-pricing-table__badges">
                        {plan.badge ? (
                          <Badge tone={plan.highlighted ? 'accent' : 'neutral'} size="sm">
                            {plan.badge}
                          </Badge>
                        ) : null}
                        {plan.current ? (
                          <Badge tone="neutral" variant="outline" size="sm">
                            {m.currentPlan}
                          </Badge>
                        ) : null}
                      </span>
                    ) : null}
                    {hasPrice ? (
                      <span className="mn-pricing-table__price">
                        <span className="mn-pricing-table__amount">{price}</span>
                        {suffix ? (
                          <>
                            {' '}
                            <span className="mn-pricing-table__suffix">{suffix}</span>
                          </>
                        ) : null}
                      </span>
                    ) : null}
                    {hasPrice && periodic && periodOption?.priceNote ? (
                      <span className="mn-pricing-table__price-note">{periodOption.priceNote}</span>
                    ) : null}
                    {plan.description ? (
                      <span className="mn-pricing-table__plan-description">{plan.description}</span>
                    ) : null}
                    {plan.action ? (
                      <span className="mn-pricing-table__action">{plan.action}</span>
                    ) : null}
                  </th>
                );
              })}
            </tr>
          </thead>
          {groups.map(({ group, features: rows }, gi) => (
            <tbody key={group ?? `__ungrouped-${gi}`} className="mn-pricing-table__group">
              {group !== undefined ? (
                <tr className="mn-pricing-table__group-row">
                  <th scope="colgroup" colSpan={columns} className="mn-pricing-table__group-title">
                    {group}
                  </th>
                </tr>
              ) : null}
              {rows.map((feature) => (
                <tr key={feature.id} className="mn-pricing-table__row">
                  <th scope="row" className="mn-pricing-table__feature">
                    <span className="mn-pricing-table__feature-label">{feature.label}</span>
                    {feature.description ? (
                      <span className="mn-pricing-table__feature-description">
                        {feature.description}
                      </span>
                    ) : null}
                  </th>
                  {plans.map((plan) => (
                    <td
                      key={plan.id}
                      className="mn-pricing-table__cell"
                      data-highlighted={plan.highlighted || undefined}
                    >
                      <Cell value={feature.values[plan.id]} m={m} />
                    </td>
                  ))}
                </tr>
              ))}
            </tbody>
          ))}
        </table>
      </div>
    </div>
  );
});
