import type { HTMLAttributes, ReactNode } from 'react';
import { cx } from '../../../internal/cx.js';
type HeadingLevel = 'h2' | 'h3' | 'h4';

export interface FeatureItem {
  /** Decorative icon (hidden from assistive technology). */
  icon?: ReactNode;
  title: ReactNode;
  description: ReactNode;
  /** Makes the whole card a link to this URL. */
  href?: string;
}

export interface FeatureGridProps extends HTMLAttributes<HTMLUListElement> {
  items: FeatureItem[];
  columns?: 2 | 3 | 4;
  /** `card` adds a bordered surface; `plain` is for dense lists. */
  variant?: 'card' | 'plain';
  /** Heading element for each item title. */
  headingAs?: HeadingLevel;
}

/**
 * Grid of features or benefits. Linked cards use one stretched link per card,
 * so each is a single tab stop with the title as its name. Server-component safe.
 */
export function FeatureGrid({
  items,
  columns = 3,
  variant = 'card',
  headingAs: Heading = 'h3',
  className,
  ...rest
}: FeatureGridProps) {
  return (
    <ul
      {...rest}
      className={cx('mn-feature-grid', className)}
      data-columns={columns}
      data-variant={variant}
    >
      {items.map((item, i) => (
        <li key={i} className="mn-feature" data-linked={item.href ? true : undefined}>
          {item.icon ? (
            <span className="mn-feature__icon" aria-hidden>
              {item.icon}
            </span>
          ) : null}
          <Heading className="mn-feature__title">
            {item.href ? (
              <a className="mn-feature__link" href={item.href}>
                {item.title}
              </a>
            ) : (
              item.title
            )}
          </Heading>
          <p className="mn-feature__description">{item.description}</p>
        </li>
      ))}
    </ul>
  );
}
