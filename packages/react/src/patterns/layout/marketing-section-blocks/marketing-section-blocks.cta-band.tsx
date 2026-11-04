import type { HTMLAttributes, ReactNode } from 'react';
import { cx } from '../../../internal/cx.js';
type HeadingLevel = 'h2' | 'h3' | 'h4';

export interface CTABandProps extends Omit<HTMLAttributes<HTMLElement>, 'title'> {
  title: ReactNode;
  description?: ReactNode;
  /** Buttons. On the `accent` tone, Monarch inverts Button colours automatically. */
  actions?: ReactNode;
  /** `accent` for the closing call to action; `subtle` and `inverse` for quieter moments. */
  tone?: 'accent' | 'subtle' | 'inverse';
  headingAs?: HeadingLevel;
}

/** A closing call to action. Server-component safe. */
export function CTABand({
  title,
  description,
  actions,
  tone = 'accent',
  headingAs: Heading = 'h2',
  className,
  ...rest
}: CTABandProps) {
  return (
    <section {...rest} className={cx('mn-cta', className)} data-tone={tone}>
      <div className="mn-cta__text">
        <Heading className="mn-cta__title">{title}</Heading>
        {description ? <p className="mn-cta__description">{description}</p> : null}
      </div>
      {actions ? <div className="mn-cta__actions">{actions}</div> : null}
    </section>
  );
}
