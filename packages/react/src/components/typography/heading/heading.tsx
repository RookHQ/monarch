import { forwardRef, type HTMLAttributes } from 'react';
import { cx } from '../../../internal/cx.js';

/** The document outline level: renders `<h1>` to `<h6>`. */
export type HeadingLevel = 1 | 2 | 3 | 4 | 5 | 6;

/** Visual size, named after the `--mn-text-*` type scale. */
export type HeadingSize = 'sm' | 'base' | 'lg' | 'xl' | '2xl' | '3xl' | '4xl' | '5xl';

export type HeadingWeight = 'medium' | 'semibold' | 'bold';

export type HeadingTone = 'default' | 'muted';

/** The size each level gets when `size` is not set. */
export const HEADING_DEFAULT_SIZES: Record<HeadingLevel, HeadingSize> = {
  1: '3xl',
  2: '2xl',
  3: 'xl',
  4: 'lg',
  5: 'base',
  6: 'sm',
};

export interface HeadingProps extends HTMLAttributes<HTMLHeadingElement> {
  /**
   * The outline level, `<h1>` to `<h6>`. Choose it from the page structure
   * (one `h1` per page, no skipped levels), never from how big it should look.
   * Default 2.
   */
  level?: HeadingLevel;
  /**
   * How big it looks, independent of `level`: a card title can be an `h3`
   * that looks `base`. Defaults to the level's size (`h1` 3xl, `h2` 2xl,
   * `h3` xl, `h4` lg, `h5` base, `h6` sm). Exposed as `data-size`.
   */
  size?: HeadingSize;
  /** Font weight. Default `semibold`. */
  weight?: HeadingWeight;
  /** `muted` for secondary headings such as an empty section's title. Default `default`. */
  tone?: HeadingTone;
  /**
   * Keep to one line and end with an ellipsis when it does not fit. Pass the
   * full text as `title` when the cut-off part matters.
   */
  truncate?: boolean;
}

/**
 * A section heading whose outline level (`h1`–`h6`) and visual size are set
 * separately, so the document outline stays correct while the design picks
 * the size. Large sizes get tighter line height and letter spacing.
 */
export const Heading = forwardRef<HTMLHeadingElement, HeadingProps>(function Heading(
  { level = 2, size, weight = 'semibold', tone = 'default', truncate = false, className, ...rest },
  ref,
) {
  const Tag = `h${level}` as const;
  return (
    <Tag
      ref={ref}
      {...rest}
      data-level={level}
      data-size={size ?? HEADING_DEFAULT_SIZES[level]}
      data-weight={weight}
      data-tone={tone === 'default' ? undefined : tone}
      data-truncate={truncate || undefined}
      className={cx('mn-heading', className)}
    />
  );
});
