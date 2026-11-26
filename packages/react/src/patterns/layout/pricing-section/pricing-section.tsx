'use client';

import { forwardRef, useId, type ReactNode } from 'react';
import { cx } from '../../../internal/cx.js';
import { PlanCard, type PlanCardFeature } from '../../../components/billing/plan-card/index.js';
import {
  BillingPeriodToggle,
  PricingTable,
  planPriceFor,
  useBillingPeriod,
  type BillingPeriodOption,
  type PricingFeature,
  type PricingPlan,
} from '../../../components/billing/pricing-table/index.js';
import {
  MarketingSection,
  MarketingSectionHeader,
  type MarketingSectionProps,
} from '../marketing-section-blocks/index.js';

/** A plan in the section: a PricingTable plan plus the card's feature list. */
export interface PricingSectionPlan extends PricingPlan {
  /** The plan card's feature list (the comparison table has its own rows). */
  features?: readonly (string | PlanCardFeature)[] | undefined;
  /** Heading above the card's feature list, such as "Everything in Starter, plus". */
  featuresLabel?: ReactNode;
  /** Small print under the card's action. */
  footer?: ReactNode;
}

/** Visible and assistive-technology text. English defaults. */
export interface PricingSectionMessages {
  /** Accessible name of the billing-period toggle. Default "Billing period". */
  billingPeriod: string;
  /** Heading above the comparison table. Default "Compare plans". */
  comparisonTitle: string;
  /** Badge text for `current` plans. Default "Current plan". */
  currentPlan: string;
  /** Hidden text for a check. Default "Included". */
  included: string;
  /** Hidden text for a dash in the comparison. Default "Not included". */
  notIncluded: string;
  /** Hidden text before an excluded feature on a plan card. Default "Not included:". */
  planNotIncluded: string;
  /** Hidden header of the comparison's feature column. Default "Feature". */
  feature: string;
}

export const defaultPricingSectionMessages: PricingSectionMessages = {
  billingPeriod: 'Billing period',
  comparisonTitle: 'Compare plans',
  currentPlan: 'Current plan',
  included: 'Included',
  notIncluded: 'Not included',
  planNotIncluded: 'Not included:',
  feature: 'Feature',
};

export interface PricingSectionProps extends Omit<MarketingSectionProps, 'title'> {
  /** Short context line above the title. */
  eyebrow?: ReactNode;
  /** Section heading, e.g. "Pricing". */
  title: ReactNode;
  /** One or two sentences under the title. */
  description?: ReactNode;
  /** Heading element for the title. Default `h2`; plan names use the next level. */
  headingAs?: 'h2' | 'h3' | undefined;
  /** Header alignment. Default `center`. */
  align?: 'start' | 'center' | undefined;
  /** Plans, shown as cards in this order. */
  plans: readonly PricingSectionPlan[];
  /** Billing periods. With two or more, one toggle switches the cards and the comparison. */
  periods?: readonly BillingPeriodOption[] | undefined;
  /** Selected period (controlled). */
  period?: string | undefined;
  /** Initial period (uncontrolled). Default: the first period. */
  defaultPeriod?: string | undefined;
  onPeriodChange?: ((period: string) => void) | undefined;
  /** Rows for a full comparison table under the cards. Leave out for cards only. */
  comparison?: readonly PricingFeature[] | undefined;
  /** Heading of the comparison. Default `messages.comparisonTitle`. */
  comparisonTitle?: ReactNode;
  /** Small print under the cards, such as tax or currency notes. */
  note?: ReactNode;
  messages?: Partial<PricingSectionMessages> | undefined;
}

/**
 * The pricing band of a marketing page: heading, a monthly / yearly toggle,
 * one PlanCard per plan and an optional full PricingTable comparison, all
 * switched by the same period. Prices are shown exactly as given.
 */
export const PricingSection = forwardRef<HTMLElement, PricingSectionProps>(function PricingSection(
  {
    eyebrow,
    title,
    description,
    headingAs = 'h2',
    align = 'center',
    plans,
    periods,
    period: periodProp,
    defaultPeriod,
    onPeriodChange,
    comparison,
    comparisonTitle,
    note,
    messages,
    className,
    width = 'wide',
    ...rest
  },
  ref,
) {
  const m = { ...defaultPricingSectionMessages, ...messages };
  const [period, setPeriod] = useBillingPeriod(periods, periodProp, defaultPeriod, onPeriodChange);
  const titleId = useId();
  const periodOption = periods?.find((p) => p.value === period);
  const planHeading = headingAs === 'h2' ? 'h3' : 'h4';
  const ComparisonHeading = planHeading;

  return (
    <MarketingSection
      aria-labelledby={titleId}
      {...rest}
      ref={ref}
      width={width}
      className={cx('mn-pricing-section', className)}
      data-period={period}
    >
      <MarketingSectionHeader
        eyebrow={eyebrow}
        title={title}
        description={description}
        headingAs={headingAs}
        titleId={titleId}
        align={align}
      />
      {periods && periods.length > 1 ? (
        <div className="mn-pricing-section__toggle" data-align={align}>
          <BillingPeriodToggle
            periods={periods}
            value={period}
            onValueChange={setPeriod}
            label={m.billingPeriod}
          />
        </div>
      ) : null}
      <ul className="mn-pricing-section__plans" data-count={Math.min(plans.length, 4)}>
        {plans.map((plan) => {
          const periodic = period !== undefined && plan.prices?.[period] !== undefined;
          return (
            <li key={plan.id} className="mn-pricing-section__plan">
              <PlanCard
                headingAs={planHeading}
                name={plan.name}
                price={planPriceFor(plan, period)}
                period={plan.priceSuffix ?? (periodic ? periodOption?.priceSuffix : undefined)}
                priceNote={periodic ? periodOption?.priceNote : undefined}
                description={plan.description}
                features={plan.features}
                featuresLabel={plan.featuresLabel}
                action={plan.action}
                footer={plan.footer}
                highlighted={plan.highlighted}
                badge={plan.badge}
                current={plan.current}
                messages={{ currentPlan: m.currentPlan, notIncluded: m.planNotIncluded }}
              />
            </li>
          );
        })}
      </ul>
      {note ? <p className="mn-pricing-section__note">{note}</p> : null}
      {comparison && comparison.length > 0 ? (
        <div className="mn-pricing-section__comparison">
          <ComparisonHeading className="mn-pricing-section__comparison-title">
            {comparisonTitle ?? m.comparisonTitle}
          </ComparisonHeading>
          <PricingTable
            plans={plans}
            features={comparison}
            periods={periods}
            period={period}
            showPeriodToggle={false}
            caption={comparisonTitle ?? m.comparisonTitle}
            messages={{
              currentPlan: m.currentPlan,
              included: m.included,
              notIncluded: m.notIncluded,
              feature: m.feature,
            }}
          />
        </div>
      ) : null}
    </MarketingSection>
  );
});
PricingSection.displayName = 'PricingSection';
