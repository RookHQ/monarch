import { forwardRef, useId, type HTMLAttributes, type ReactNode } from 'react';
import { cx } from '../../../internal/cx.js';
import { CheckGlyph, DashGlyph } from '../../../internal/icons.js';
import { Badge } from '../../data-display/badge/index.js';
import { VisuallyHidden } from '../../layout/visually-hidden/index.js';

/** One line in a plan's feature list. A plain string is an included feature. */
export interface PlanCardFeature {
  /** Stable key. Default: the label when it is a string, otherwise the index. */
  id?: string | undefined;
  /** What the plan offers, e.g. "Unlimited projects". */
  label: ReactNode;
  /** `false` lists the feature as not included (muted, with a dash). Default `true`. */
  included?: boolean | undefined;
  /** Short extra detail after the label, e.g. "up to 10 seats". */
  hint?: ReactNode;
}

/** Visible and assistive-technology text. English defaults. */
export interface PlanCardMessages {
  /** Badge text when `current` is set. Default "Current plan". */
  currentPlan: string;
  /** Hidden text read before an excluded feature. Default "Not included:". */
  notIncluded: string;
}

export const defaultPlanCardMessages: PlanCardMessages = {
  currentPlan: 'Current plan',
  notIncluded: 'Not included:',
};

export interface PlanCardProps extends Omit<HTMLAttributes<HTMLElement>, 'title'> {
  /** The plan name. Rendered as the card heading and used as its accessible name. */
  name: ReactNode;
  /** Heading element for the name. Default `h3` (a pricing section's title is the `h2`). */
  headingAs?: 'h2' | 'h3' | 'h4' | undefined;
  /**
   * The price as the product writes it: a string ("Contact us") or a `<Money>`
   * element. Leave it out when the plan has no published price.
   */
  price?: ReactNode;
  /** Text after the price, such as "/ month" or "per seat / month". */
  period?: ReactNode;
  /** A line under the price, such as "Billed yearly" or "Taxes extra". */
  priceNote?: ReactNode;
  /** One or two sentences on who the plan is for. */
  description?: ReactNode;
  /** A heading above the feature list, such as "Everything in Starter, plus". */
  featuresLabel?: ReactNode;
  /** What the plan includes (and, with `included: false`, what it does not). */
  features?: readonly (string | PlanCardFeature)[] | undefined;
  /** The plan's call to action: normally one Button or a link styled as one. */
  action?: ReactNode;
  /** Small print under the action. */
  footer?: ReactNode;
  /** Emphasises the card (accent border) as the recommended plan. */
  highlighted?: boolean | undefined;
  /** Short label shown on a highlighted card, such as "Recommended". */
  badge?: ReactNode;
  /** Marks the organisation's current plan with a "Current plan" badge. */
  current?: boolean | undefined;
  messages?: Partial<PlanCardMessages> | undefined;
}

function featureKey(feature: string | PlanCardFeature, index: number): string {
  if (typeof feature === 'string') return feature;
  if (feature.id) return feature.id;
  return typeof feature.label === 'string' ? feature.label : String(index);
}

/**
 * One plan in a pricing grid or an in-app plan picker: name, price, who it is
 * for, what it includes and a call to action. It shows prices exactly as given
 * and never formats or invents them. Server-component safe.
 */
export const PlanCard = forwardRef<HTMLElement, PlanCardProps>(function PlanCard(
  {
    name,
    headingAs: Heading = 'h3',
    price,
    period,
    priceNote,
    description,
    featuresLabel,
    features,
    action,
    footer,
    highlighted = false,
    badge,
    current = false,
    messages,
    className,
    ...rest
  },
  ref,
) {
  const m = { ...defaultPlanCardMessages, ...messages };
  const titleId = useId();
  const hasFeatures = features !== undefined && features.length > 0;
  return (
    <article
      ref={ref}
      aria-labelledby={rest['aria-label'] ? undefined : titleId}
      {...rest}
      className={cx('mn-plan-card', className)}
      data-highlighted={highlighted || undefined}
      data-current={current || undefined}
    >
      <div className="mn-plan-card__head">
        <Heading id={titleId} className="mn-plan-card__name">
          {name}
        </Heading>
        {badge || current ? (
          <div className="mn-plan-card__badges">
            {badge ? (
              <Badge tone={highlighted ? 'accent' : 'neutral'} variant="subtle" size="sm">
                {badge}
              </Badge>
            ) : null}
            {current ? (
              <Badge tone="neutral" variant="outline" size="sm">
                {m.currentPlan}
              </Badge>
            ) : null}
          </div>
        ) : null}
      </div>
      {price !== undefined || period !== undefined ? (
        <p className="mn-plan-card__price">
          {price !== undefined ? <span className="mn-plan-card__amount">{price}</span> : null}
          {period !== undefined ? (
            <>
              {' '}
              <span className="mn-plan-card__period">{period}</span>
            </>
          ) : null}
        </p>
      ) : null}
      {priceNote ? <p className="mn-plan-card__price-note">{priceNote}</p> : null}
      {description ? <p className="mn-plan-card__description">{description}</p> : null}
      {action ? <div className="mn-plan-card__action">{action}</div> : null}
      {hasFeatures ? (
        <div className="mn-plan-card__features">
          {featuresLabel ? <p className="mn-plan-card__features-label">{featuresLabel}</p> : null}
          <ul className="mn-plan-card__list">
            {features.map((feature, index) => {
              const item: PlanCardFeature =
                typeof feature === 'string' ? { label: feature } : feature;
              const included = item.included !== false;
              return (
                <li
                  key={featureKey(feature, index)}
                  className="mn-plan-card__feature"
                  data-included={included ? 'true' : 'false'}
                >
                  <span className="mn-plan-card__mark" aria-hidden>
                    {included ? <CheckGlyph /> : <DashGlyph />}
                  </span>
                  <span className="mn-plan-card__feature-text">
                    {included ? null : <VisuallyHidden>{m.notIncluded} </VisuallyHidden>}
                    {item.label}
                    {item.hint ? (
                      <>
                        {' '}
                        <span className="mn-plan-card__hint">{item.hint}</span>
                      </>
                    ) : null}
                  </span>
                </li>
              );
            })}
          </ul>
        </div>
      ) : null}
      {footer ? <div className="mn-plan-card__footer">{footer}</div> : null}
    </article>
  );
});
