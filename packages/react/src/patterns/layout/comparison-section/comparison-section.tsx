import { forwardRef, useId, type ReactNode } from 'react';
import { cx } from '../../../internal/cx.js';
import {
  PricingTable,
  type PricingFeature,
} from '../../../components/billing/pricing-table/index.js';
import {
  MarketingSection,
  MarketingSectionHeader,
  type MarketingSectionProps,
} from '../marketing-section-blocks/index.js';

/** One compared option: your product, a plan, or an alternative. */
export interface ComparisonColumn {
  /** Stable id, used as the key in every row's `values`. */
  id: string;
  /** Column heading, e.g. the product or approach name. */
  name: ReactNode;
  /** A short line under the name. */
  description?: ReactNode;
  /** Emphasises the column (normally your own product). */
  highlighted?: boolean | undefined;
  /** Short label on the column, such as "This product". */
  badge?: ReactNode;
}

/**
 * One compared capability. `values[columnId]`: `true` is a check ("Included"),
 * `false` or missing is a dash ("Not included"), anything else is shown as text.
 */
export type ComparisonRow = PricingFeature;

/** A "best for" summary card under the table. */
export interface ComparisonBestFor {
  id: string;
  /** e.g. "Best for teams that…". */
  title: ReactNode;
  description: ReactNode;
  /** Accent border, for the card about your own product. */
  highlighted?: boolean | undefined;
}

/** Visible and assistive-technology text. English defaults. */
export interface ComparisonSectionMessages {
  /** Hidden header of the capability column. Default "Feature". */
  feature: string;
  /** Hidden text of a check cell. Default "Included". */
  included: string;
  /** Hidden text of a dash cell. Default "Not included". */
  notIncluded: string;
}

export const defaultComparisonSectionMessages: ComparisonSectionMessages = {
  feature: 'Feature',
  included: 'Included',
  notIncluded: 'Not included',
};

export interface ComparisonSectionProps extends Omit<MarketingSectionProps, 'title'> {
  /** Short context line above the title. */
  eyebrow?: ReactNode;
  /** Section heading, e.g. "How it compares". */
  title: ReactNode;
  /** One or two sentences under the title. */
  description?: ReactNode;
  /** Heading element for the title. Default `h2`. */
  headingAs?: 'h2' | 'h3' | undefined;
  /** Header alignment. Default `start`. */
  align?: 'start' | 'center' | undefined;
  /** The compared options, as table columns. */
  columns: readonly ComparisonColumn[];
  /** The compared capabilities, as table rows (optionally grouped). */
  rows: readonly ComparisonRow[];
  /** Table caption (its accessible name, visually hidden). Default: `title`. */
  caption?: ReactNode;
  /** Summary cards under the table: who each option suits. */
  bestFor?: readonly ComparisonBestFor[] | undefined;
  /**
   * Small print, such as the date the comparison was checked, its sources and
   * trademark notices. Strongly recommended when naming other products.
   */
  note?: ReactNode;
  /** Calls to action under the comparison. */
  actions?: ReactNode;
  messages?: Partial<ComparisonSectionMessages> | undefined;
}

/**
 * A marketing comparison: your product beside alternatives (or plans beside
 * each other) as an accessible table of capabilities, with optional
 * "best for" cards, small print and calls to action.
 */
export const ComparisonSection = forwardRef<HTMLElement, ComparisonSectionProps>(
  function ComparisonSection(
    {
      eyebrow,
      title,
      description,
      headingAs = 'h2',
      align = 'start',
      columns,
      rows,
      caption,
      bestFor,
      note,
      actions,
      messages,
      className,
      ...rest
    },
    ref,
  ) {
    const m = { ...defaultComparisonSectionMessages, ...messages };
    const titleId = useId();
    const CardHeading = headingAs === 'h2' ? 'h3' : 'h4';
    return (
      // Not labelled by the title: the table's scroll region already carries that
      // name, and two landmarks must not share one.
      <MarketingSection {...rest} ref={ref} className={cx('mn-comparison-section', className)}>
        <MarketingSectionHeader
          eyebrow={eyebrow}
          title={title}
          description={description}
          headingAs={headingAs}
          titleId={titleId}
          align={align}
        />
        <PricingTable
          className="mn-comparison-section__table"
          plans={columns.map((c) => ({
            id: c.id,
            name: c.name,
            description: c.description,
            highlighted: c.highlighted,
            badge: c.badge,
          }))}
          features={rows}
          showPrices={false}
          caption={caption ?? title}
          messages={{ feature: m.feature, included: m.included, notIncluded: m.notIncluded }}
        />
        {bestFor && bestFor.length > 0 ? (
          <ul className="mn-comparison-section__best-for">
            {bestFor.map((card) => (
              <li
                key={card.id}
                className="mn-comparison-section__card"
                data-highlighted={card.highlighted || undefined}
              >
                <CardHeading className="mn-comparison-section__card-title">
                  {card.title}
                </CardHeading>
                <p className="mn-comparison-section__card-description">{card.description}</p>
              </li>
            ))}
          </ul>
        ) : null}
        {note ? <div className="mn-comparison-section__note">{note}</div> : null}
        {actions ? <div className="mn-comparison-section__actions">{actions}</div> : null}
      </MarketingSection>
    );
  },
);
ComparisonSection.displayName = 'ComparisonSection';
