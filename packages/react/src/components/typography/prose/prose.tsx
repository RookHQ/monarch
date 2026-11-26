import { forwardRef, type HTMLAttributes, type Ref } from 'react';
import { cx } from '../../../internal/cx.js';

export type ProseSize = 'sm' | 'base' | 'lg';

export interface ProseProps extends HTMLAttributes<HTMLElement> {
  /** The wrapper element. Use `article` for a self-contained document. Default `div`. */
  as?: 'div' | 'article' | 'section';
  /**
   * Base font size: `sm` for comments, chat and side panels, `base` (default)
   * for documents, `lg` for long reads. Headings, code and spacing scale with it.
   */
  size?: ProseSize;
  /**
   * Limit the line length to the reading width (`--mn-layout-reading-max`,
   * about 70 characters). Default true; set false when the container already
   * sets the width.
   */
  measure?: boolean;
}

/**
 * Typography for long-form content you do not write element by element:
 * rendered Markdown, CMS or help-centre HTML, release notes. Styles headings,
 * paragraphs, links, lists, quotes, inline code, code blocks, tables, images,
 * figures and rules inside it.
 *
 * Prose only styles; it never renders HTML strings. Render Markdown with your
 * renderer (raw HTML off) or sanitise HTML on the server before passing it in.
 * Monarch components inside keep their own styles (anything with an `mn-`
 * class is skipped); wrap other content in ProseExclude to opt it out.
 */
export const Prose = forwardRef<HTMLElement, ProseProps>(function Prose(
  { as = 'div', size = 'base', measure = true, className, ...rest },
  ref,
) {
  const Tag = as as 'div';
  return (
    <Tag
      ref={ref as Ref<HTMLDivElement>}
      {...rest}
      data-size={size}
      data-measure={measure || undefined}
      className={cx('mn-prose', className)}
    />
  );
});

/**
 * Opts its content out of Prose styles: an embedded widget, a custom card or
 * anything that brings its own styling.
 */
export const ProseExclude = forwardRef<HTMLDivElement, HTMLAttributes<HTMLDivElement>>(
  function ProseExclude({ className, ...rest }, ref) {
    return <div ref={ref} {...rest} className={cx('mn-prose__exclude', className)} />;
  },
);
