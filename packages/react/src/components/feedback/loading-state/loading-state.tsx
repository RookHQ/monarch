import { forwardRef, type HTMLAttributes } from 'react';
import { cx } from '../../../internal/cx.js';
import { Skeleton } from '../skeleton/index.js';
import { Spinner } from '../spinner/index.js';

export type LoadingStateVariant = 'list' | 'rows' | 'cards' | 'detail' | 'spinner';

export interface LoadingStateProps extends HTMLAttributes<HTMLDivElement> {
  /** Announced to screen readers, e.g. "Loading tickets". */
  label: string;
  /** Skeleton rows to render (`list`, `rows` and `cards`). Default 4. */
  rows?: number;
  /**
   * `list` (default) stacks avatar + text rows; `rows` stacks plain text rows
   * (table and list-row preset); `cards` renders a grid; `detail` a title +
   * paragraphs; `spinner` a centred spinner with the label shown.
   */
  variant?: LoadingStateVariant;
}

const widths = (i: number) => [`${55 + ((i * 17) % 30)}%`, `${30 + ((i * 11) % 25)}%`] as const;

/** Skeleton placeholder with an aria-busy region and a live announcement. */
export const LoadingState = forwardRef<HTMLDivElement, LoadingStateProps>(function LoadingState(
  { label, rows = 4, variant = 'list', className, ...rest },
  ref,
) {
  const count = Math.max(0, Math.floor(rows));
  return (
    <div
      ref={ref}
      {...rest}
      aria-busy="true"
      className={cx('mn-loading-state', className)}
      data-variant={variant}
    >
      {variant === 'spinner' ? (
        <div className="mn-loading-state__spinner">
          <Spinner size="md" />
          <span role="status" className="mn-loading-state__label">
            {label}
          </span>
        </div>
      ) : (
        <span role="status" className="mn-visually-hidden">
          {label}
        </span>
      )}
      {variant === 'spinner' ? null : variant === 'detail' ? (
        <>
          <Skeleton shape="text" width="40%" height="1.25rem" />
          <Skeleton shape="text" width="90%" />
          <Skeleton shape="text" width="75%" />
          <Skeleton shape="text" width="60%" />
        </>
      ) : (
        Array.from({ length: count }, (_, i) => (
          <div key={i} className="mn-loading-state__row">
            {variant === 'cards' ? (
              <Skeleton height="6rem" />
            ) : variant === 'rows' ? (
              <>
                <Skeleton shape="text" width={widths(i)[0]} />
                <Skeleton shape="text" width="18%" className="mn-loading-state__meta" />
              </>
            ) : (
              <>
                <Skeleton shape="circle" width="2rem" height="2rem" />
                <div className="mn-loading-state__lines">
                  <Skeleton shape="text" width={widths(i)[0]} />
                  <Skeleton shape="text" width={widths(i)[1]} />
                </div>
              </>
            )}
          </div>
        ))
      )}
    </div>
  );
});
