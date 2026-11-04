import type { HTMLAttributes, ReactNode } from 'react';
import { cx } from '../../../internal/cx.js';

export interface StatItem {
  /** The figure, e.g. "24" or "99.9%". Only real, current numbers. */
  value: ReactNode;
  /** What the figure measures. Read first by screen readers. */
  label: ReactNode;
  description?: ReactNode;
}

export interface StatStripProps extends HTMLAttributes<HTMLDListElement> {
  items: StatItem[];
}

/** A row of key figures as a description list. Server-component safe. */
export function StatStrip({ items, className, ...rest }: StatStripProps) {
  return (
    <dl {...rest} className={cx('mn-stat-strip', className)} data-count={items.length}>
      {items.map((item, i) => (
        <div key={i} className="mn-stat-strip__item">
          <dt className="mn-stat-strip__label">{item.label}</dt>
          <dd className="mn-stat-strip__value">{item.value}</dd>
          {item.description ? (
            <dd className="mn-stat-strip__description">{item.description}</dd>
          ) : null}
        </div>
      ))}
    </dl>
  );
}
