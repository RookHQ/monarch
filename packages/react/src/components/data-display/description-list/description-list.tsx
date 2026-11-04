import { forwardRef, type HTMLAttributes, type ReactNode } from 'react';
import { cx } from '../../../internal/cx.js';
import { CopyButton } from '../../actions/copy-button/index.js';
import { DescriptionValueClamp } from './description-list.clamp.js';

export interface DescriptionListProps extends HTMLAttributes<HTMLDListElement> {
  /**
   * Grid columns. `auto` fits as many columns of at least
   * `--mn-dl-min-column` (default 12rem) as the width allows. Every value
   * becomes one column below 640px. Default 2.
   */
  columns?: 1 | 2 | 3 | 4 | 'auto';
  /**
   * `stacked` (default, vertical) puts the term above the value; `inline`
   * (horizontal) puts them side by side, and stacks again below 420px.
   */
  layout?: 'stacked' | 'inline';
}

/** Key/value facts on detail pages (owner, stage, created, …). */
export const DescriptionList = forwardRef<HTMLDListElement, DescriptionListProps>(
  function DescriptionList({ columns = 2, layout = 'stacked', className, ...rest }, ref) {
    return (
      <dl
        ref={ref}
        {...rest}
        data-columns={columns}
        data-layout={layout}
        className={cx('mn-dl', className)}
      />
    );
  },
);

export interface DescriptionItemProps {
  term: ReactNode;
  /** The value. `null`, `undefined`, `false` and `''` render the `placeholder`. */
  children?: ReactNode;
  /** Span all columns. */
  wide?: boolean;
  className?: string;
  /** Shown for an empty value. Default an em dash, read as `emptyLabel`. */
  placeholder?: ReactNode;
  /** Screen-reader text for the default placeholder. Default "Not set". */
  emptyLabel?: string;
  /**
   * Adds an icon CopyButton after the value. `true` copies the value when it
   * is a string or number; pass a string to copy something else (the full id
   * behind a shortened one). Not shown for an empty value.
   */
  copyable?: boolean | string;
  /** Accessible name of the copy button. Default "Copy" followed by the term when it is text. */
  copyLabel?: string;
  /**
   * Clamp the value to this many lines (`true` is 1) with a "Show more"
   * button that expands it. The button appears only when the text overflows.
   */
  truncate?: boolean | number;
  /** Label of the expand button. Default "Show more". */
  expandLabel?: string;
  /** Label of the collapse button. Default "Show less". */
  collapseLabel?: string;
}

function isEmpty(value: ReactNode) {
  return value === null || value === undefined || value === false || value === '';
}

export const DescriptionItem = forwardRef<HTMLDivElement, DescriptionItemProps>(
  function DescriptionItem(
    {
      term,
      children,
      wide,
      className,
      placeholder,
      emptyLabel = 'Not set',
      copyable,
      copyLabel,
      truncate,
      expandLabel = 'Show more',
      collapseLabel = 'Show less',
    },
    ref,
  ) {
    const empty = isEmpty(children);
    const copyText =
      typeof copyable === 'string'
        ? copyable
        : copyable && (typeof children === 'string' || typeof children === 'number')
          ? String(children)
          : null;
    const lines = truncate === true ? 1 : typeof truncate === 'number' ? truncate : 0;
    let value: ReactNode;
    if (empty) {
      value =
        placeholder !== undefined ? (
          placeholder
        ) : (
          <>
            <span aria-hidden="true">—</span>
            <span className="mn-visually-hidden">{emptyLabel}</span>
          </>
        );
    } else if (lines > 0) {
      value = (
        <DescriptionValueClamp
          lines={lines}
          expandLabel={expandLabel}
          collapseLabel={collapseLabel}
        >
          {children}
        </DescriptionValueClamp>
      );
    } else {
      value = children;
    }
    return (
      <div
        ref={ref}
        className={cx('mn-dl__item', className)}
        data-wide={wide || undefined}
        data-empty={empty || undefined}
      >
        <dt className="mn-dl__term">{term}</dt>
        <dd className="mn-dl__value">
          {copyText !== null && !empty ? (
            <span className="mn-dl__copy-row">
              <span className="mn-dl__copy-value">{value}</span>
              <CopyButton
                value={copyText}
                iconOnly
                size="xs"
                label={copyLabel ?? (typeof term === 'string' ? `Copy ${term}` : 'Copy')}
                className="mn-dl__copy"
              />
            </span>
          ) : (
            value
          )}
        </dd>
      </div>
    );
  },
);
