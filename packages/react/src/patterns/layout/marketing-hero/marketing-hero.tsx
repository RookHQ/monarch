import type { HTMLAttributes, ReactNode } from 'react';
import { cx } from '../../../internal/cx.js';

export interface MarketingHeroProps extends Omit<HTMLAttributes<HTMLElement>, 'title'> {
  /** Short context line above the title, in the product accent. */
  eyebrow?: ReactNode;
  /** The page's single h1. Keep it under ~8 words. */
  title: ReactNode;
  /** One or two sentences that say what the product does for whom. */
  lead?: ReactNode;
  /** One primary Button and at most one secondary action. */
  actions?: ReactNode;
  /** Small reassurance under the actions, e.g. "Free for teams of five". */
  note?: ReactNode;
  /** Product illustration, normally a ProductFrame. */
  media?: ReactNode;
  /** `centered` for launch pages; `split` puts media beside the copy on wide screens. */
  layout?: 'centered' | 'split';
  /** `wash`: a very subtle accent wash behind the hero. */
  background?: 'none' | 'wash';
  /** Heading element. Use `h2` only when the page title lives elsewhere. */
  headingAs?: 'h1' | 'h2';
}

/** The opening section of a marketing page. Server-component safe. */
export function MarketingHero({
  eyebrow,
  title,
  lead,
  actions,
  note,
  media,
  layout = 'centered',
  background = 'wash',
  headingAs: Heading = 'h1',
  className,
  ...rest
}: MarketingHeroProps) {
  return (
    <section
      {...rest}
      className={cx('mn-hero', className)}
      data-layout={layout}
      data-background={background}
    >
      <div className="mn-hero__inner">
        <div className="mn-hero__copy">
          {eyebrow ? <p className="mn-eyebrow">{eyebrow}</p> : null}
          <Heading className="mn-hero__title">{title}</Heading>
          {lead ? <p className="mn-hero__lead">{lead}</p> : null}
          {actions ? <div className="mn-hero__actions">{actions}</div> : null}
          {note ? <p className="mn-hero__note">{note}</p> : null}
        </div>
        {media ? <div className="mn-hero__media">{media}</div> : null}
      </div>
    </section>
  );
}
