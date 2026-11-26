import { forwardRef, type CSSProperties, type HTMLAttributes } from 'react';
import { cx } from '../../../internal/cx.js';
import { layoutGap, type LayoutElement, type LayoutGap } from '../stack/index.js';

/** Root class, declared before any other `mn-` string so dist/manifest.json reads it as the root. */
const ROOT = 'mn-grid';

/**
 * Column counts per breakpoint, mobile first. `base` applies from 0, `sm` from
 * 640px, `md` from 768px, `lg` from 1024px, `xl` from 1280px (the
 * `--mn-breakpoint-*` tokens). A missing step inherits the one below.
 */
export interface GridColumns {
  base?: number | undefined;
  sm?: number | undefined;
  md?: number | undefined;
  lg?: number | undefined;
  xl?: number | undefined;
}

export interface GridProps extends HTMLAttributes<HTMLElement> {
  /** The element to render. Default `div`; `ul` and `ol` lose their bullets and indent. */
  as?: LayoutElement | undefined;
  /**
   * Fixed columns: a number, or counts per breakpoint (`{ base: 1, md: 2, lg: 3 }`).
   * When set, `minItemWidth` is ignored.
   */
  columns?: number | GridColumns | undefined;
  /**
   * Responsive by item size instead: as many columns as fit items at least
   * this wide (a CSS length, or a number of pixels). Never wider than the
   * grid, so one item still fits a 320px screen. Default `16rem`.
   */
  minItemWidth?: string | number | undefined;
  /**
   * With `minItemWidth`: `fill` (default) keeps empty tracks so items keep
   * their size in a short last row; `fit` stretches the items to the full width.
   */
  fit?: 'fill' | 'fit' | undefined;
  /** Space between items, a step of the Monarch space scale. Default `'4'` (16px). */
  gap?: LayoutGap | undefined;
  /** Space between rows, when it should differ from `gap`. */
  rowGap?: LayoutGap | undefined;
  /** Vertical alignment of items within a row. Default `stretch` (equal heights). */
  align?: 'start' | 'center' | 'end' | 'stretch' | undefined;
}

const STEPS = ['base', 'sm', 'md', 'lg', 'xl'] as const;

/**
 * A responsive grid: as many columns as fit items of `minItemWidth`, or a set
 * number of columns per breakpoint. Use it for card grids, tiles and form
 * layouts instead of ad-hoc `grid grid-cols-*` wrappers.
 */
export const Grid = forwardRef<HTMLElement, GridProps>(function Grid(
  {
    as: Tag = 'div',
    columns,
    minItemWidth = '16rem',
    fit = 'fill',
    gap = '4',
    rowGap,
    align = 'stretch',
    className,
    style,
    ...rest
  },
  ref,
) {
  const vars: Record<string, string | number> = {
    '--mn-grid-gap': layoutGap(gap),
    '--mn-grid-row-gap': layoutGap(rowGap ?? gap),
  };
  let mode: 'columns' | 'auto';
  if (columns !== undefined) {
    mode = 'columns';
    const counts: GridColumns = typeof columns === 'number' ? { base: columns } : columns;
    for (const step of STEPS) {
      const n = counts[step];
      if (n !== undefined && Number.isFinite(n) && n >= 1)
        vars[`--mn-grid-cols-${step}`] = Math.floor(n);
    }
  } else {
    mode = 'auto';
    vars['--mn-grid-min'] = typeof minItemWidth === 'number' ? `${minItemWidth}px` : minItemWidth;
  }
  return (
    <Tag
      ref={ref as never}
      {...rest}
      className={cx(ROOT, className)}
      style={{ ...vars, ...style } as CSSProperties}
      data-mode={mode}
      data-fit={mode === 'auto' ? fit : undefined}
      data-gap={gap}
      data-align={align}
    />
  );
});
