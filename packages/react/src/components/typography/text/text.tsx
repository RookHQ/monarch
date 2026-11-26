import { forwardRef, type CSSProperties, type HTMLAttributes, type Ref } from 'react';
import { cx } from '../../../internal/cx.js';

/** Font size, named after the `--mn-text-*` type scale. */
export type TextSize = '2xs' | 'xs' | 'sm' | 'base' | 'lg';

/** Text colour. Status tones use each intent's `strong` token, which meets text contrast. */
export type TextTone =
  'default' | 'muted' | 'subtle' | 'accent' | 'info' | 'success' | 'warning' | 'danger';

export type TextWeight = 'regular' | 'medium' | 'semibold' | 'bold';

/** The elements Text can render. Phrasing elements for inline use, block ones for paragraphs. */
export type TextElement =
  | 'p'
  | 'span'
  | 'div'
  | 'label'
  | 'strong'
  | 'em'
  | 'small'
  | 'li'
  | 'dt'
  | 'dd'
  | 'figcaption'
  | 'legend';

export interface TextProps extends HTMLAttributes<HTMLElement> {
  /** The element to render. Default `p`; use `span` inside other text. */
  as?: TextElement;
  /**
   * Font size. Default `base` for block elements (`p`, `div`, `li`, …); the
   * inline elements `span`, `strong`, `em` and `small` inherit the
   * surrounding size unless it is set. Exposed as `data-size`.
   */
  size?: TextSize | undefined;
  /** Text colour. Default `default`. Never use a status tone as the only signal of meaning. */
  tone?: TextTone;
  /** Font weight. Default `regular`. */
  weight?: TextWeight;
  /**
   * Cut overflowing text with an ellipsis. `true` keeps it to one line; a
   * number clamps it to that many lines. Pass the full text as `title` when
   * the hidden part matters.
   */
  truncate?: boolean | number;
  /** Text alignment, using logical values so it mirrors in right-to-left. */
  align?: 'start' | 'center' | 'end';
  /** Tabular (fixed-width) digits, for numbers that line up or change in place. */
  tabular?: boolean;
  /** `for` when `as="label"`. */
  htmlFor?: string;
}

const INLINE = new Set<TextElement>(['span', 'strong', 'em', 'small']);

/**
 * Body text: paragraphs, captions, helper and meta text, with the Monarch
 * type scale, tones and weights, and single- or multi-line truncation.
 */
export const Text = forwardRef<HTMLElement, TextProps>(function Text(
  {
    as = 'p',
    size,
    tone = 'default',
    weight = 'regular',
    truncate = false,
    align,
    tabular = false,
    className,
    style,
    ...rest
  },
  ref,
) {
  const Tag = as as 'p';
  const resolvedSize = size ?? (INLINE.has(as) ? undefined : 'base');
  const lines = typeof truncate === 'number' && truncate > 0 ? Math.floor(truncate) : undefined;
  const mode = lines && lines > 1 ? 'lines' : truncate ? 'line' : undefined;
  const nextStyle =
    mode === 'lines' ? ({ ...style, '--mn-text-lines': lines } as CSSProperties) : style;
  return (
    <Tag
      ref={ref as Ref<HTMLParagraphElement>}
      {...rest}
      style={nextStyle}
      data-size={resolvedSize}
      data-tone={tone === 'default' ? undefined : tone}
      data-weight={weight === 'regular' ? undefined : weight}
      data-truncate={mode}
      data-align={align}
      data-tabular={tabular || undefined}
      className={cx('mn-text', className)}
    />
  );
});
