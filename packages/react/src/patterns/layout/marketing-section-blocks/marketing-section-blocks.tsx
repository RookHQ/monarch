import type { HTMLAttributes, ReactNode } from 'react';
import { cx } from '../../../internal/cx.js';
type HeadingLevel = 'h2' | 'h3' | 'h4';

export interface MarketingSectionProps extends HTMLAttributes<HTMLElement> {
  /**
   * `default` on the page canvas, `subtle` for a quiet alternate band, `inverse`
   * for one high-emphasis band. Inside `inverse`, Monarch remaps text, borders,
   * surfaces, accent text and focus ring, so nested components stay readable.
   */
  tone?: 'default' | 'subtle' | 'inverse';
  /** Vertical padding from the section rhythm scale. */
  spacing?: 'sm' | 'md' | 'lg';
  /** Content width. */
  width?: 'narrow' | 'content' | 'wide';
}

/** A full-width band with a centred content column. Server-component safe. */
export function MarketingSection({
  tone = 'default',
  spacing = 'md',
  width = 'content',
  className,
  children,
  ...rest
}: MarketingSectionProps) {
  return (
    <section
      {...rest}
      className={cx('mn-msection', className)}
      data-tone={tone}
      data-spacing={spacing}
    >
      <div className="mn-msection__inner" data-width={width}>
        {children}
      </div>
    </section>
  );
}

export interface MarketingSectionHeaderProps extends Omit<HTMLAttributes<HTMLDivElement>, 'title'> {
  eyebrow?: ReactNode;
  title: ReactNode;
  description?: ReactNode;
  /** Optional link or button beside a start-aligned header. */
  actions?: ReactNode;
  align?: 'start' | 'center';
  /** Heading element; `h2` for page sections. */
  headingAs?: HeadingLevel;
  /** Id for the title, so the section can use `aria-labelledby`. */
  titleId?: string;
  /** Title size from the display scale. */
  size?: 'md' | 'lg';
}

/**
 * Eyebrow, display title and description at the top of a marketing section.
 * (In-app section headings are the planned `section-header`.) Server-component safe.
 */
export function MarketingSectionHeader({
  eyebrow,
  title,
  description,
  actions,
  align = 'start',
  headingAs: Heading = 'h2',
  titleId,
  size = 'md',
  className,
  ...rest
}: MarketingSectionHeaderProps) {
  return (
    <div
      {...rest}
      className={cx('mn-msection-header', className)}
      data-align={align}
      data-size={size}
    >
      <div className="mn-msection-header__text">
        {eyebrow ? <p className="mn-eyebrow">{eyebrow}</p> : null}
        <Heading id={titleId} className="mn-msection-header__title">
          {title}
        </Heading>
        {description ? <p className="mn-msection-header__description">{description}</p> : null}
      </div>
      {actions ? <div className="mn-msection-header__actions">{actions}</div> : null}
    </div>
  );
}
