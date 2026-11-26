import { forwardRef, type CSSProperties, type HTMLAttributes } from 'react';
import { cx } from '../../../internal/cx.js';
import { layoutGap, type LayoutElement, type LayoutGap } from '../stack/index.js';

/** Root class, declared before any other `mn-` string so dist/manifest.json reads it as the root. */
const ROOT = 'mn-inline';

export interface InlineProps extends HTMLAttributes<HTMLElement> {
  /** The element to render. Default `div`; `ul` and `ol` lose their bullets and indent. */
  as?: LayoutElement | undefined;
  /** Space between children, a step of the Monarch space scale. Default `'2'` (8px). */
  gap?: LayoutGap | undefined;
  /** Space between wrapped rows, when it should differ from `gap`. */
  rowGap?: LayoutGap | undefined;
  /** Vertical alignment of children in a row. Default `center`. */
  align?: 'start' | 'center' | 'end' | 'baseline' | 'stretch' | undefined;
  /** Horizontal distribution (follows the text direction). Default `start`. */
  justify?: 'start' | 'center' | 'end' | 'between' | undefined;
  /**
   * Let children wrap onto new rows when they do not fit. Default true, so a
   * row of buttons or tags never overflows a narrow screen.
   */
  wrap?: boolean | undefined;
}

/**
 * Lays children out in a horizontal row that wraps, with even spacing from
 * the Monarch space scale: button rows, tag lists, metadata lines. Use it
 * instead of ad-hoc `flex flex-wrap items-center gap-*` wrappers.
 */
export const Inline = forwardRef<HTMLElement, InlineProps>(function Inline(
  {
    as: Tag = 'div',
    gap = '2',
    rowGap,
    align = 'center',
    justify = 'start',
    wrap = true,
    className,
    style,
    ...rest
  },
  ref,
) {
  return (
    <Tag
      ref={ref as never}
      {...rest}
      className={cx(ROOT, className)}
      style={
        {
          '--mn-inline-gap': layoutGap(gap),
          '--mn-inline-row-gap': layoutGap(rowGap ?? gap),
          ...style,
        } as CSSProperties
      }
      data-gap={gap}
      data-align={align}
      data-justify={justify}
      data-wrap={wrap ? 'wrap' : 'nowrap'}
    />
  );
});
