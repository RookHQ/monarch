import {
  forwardRef,
  useId,
  type AnchorHTMLAttributes,
  type HTMLAttributes,
  type ReactNode,
  type Ref,
} from 'react';
import { Slot, Slottable } from '@radix-ui/react-slot';
import { cx } from '../../../internal/cx.js';
import { ChevronRightGlyph } from '../../../internal/icons.js';
import { Spinner } from '../../feedback/spinner/index.js';

export interface SectionHeaderProps extends Omit<HTMLAttributes<HTMLElement>, 'title'> {
  /** The section title, rendered as a heading. */
  title: ReactNode;
  /** A sentence under the title explaining the section. */
  description?: ReactNode;
  /** A short secondary line: a count, a date range, "Updated 5 min ago". */
  meta?: ReactNode;
  /** Buttons or a menu at the end of the header. */
  actions?: ReactNode;
  /** A trailing navigation link, usually SectionHeaderLink ("View all"). Placed after `actions`. */
  link?: ReactNode;
  /** Heading element. Pick the level that fits the page outline. Default `h2`. */
  headingLevel?: 'h2' | 'h3' | 'h4' | 'h5' | 'h6';
  /** Id for the heading, to label something else with it. Generated when omitted. */
  headingId?: string;
  /** `sm` (default) for dashboard sections, `md` for page sections. */
  size?: 'sm' | 'md';
  /**
   * The section's content is loading or refreshing: sets `aria-busy`, shows a
   * spinner after the title and, with `children`, dims the content.
   */
  busy?: boolean;
  /** Accessible name of the busy spinner. Default "Loading". */
  busyLabel?: string;
  /**
   * The section's content. When given, SectionHeader renders a `<section>`
   * labelled by the heading, with the content under the header.
   */
  children?: ReactNode;
}

/**
 * An unboxed section header: title, optional description and meta line, and
 * trailing actions or a "View all" link. With `children` it becomes the
 * whole labelled `<section>`; without, it is the header row alone (for
 * example inside a Card).
 */
export const SectionHeader = forwardRef<HTMLElement, SectionHeaderProps>(function SectionHeader(
  {
    title,
    description,
    meta,
    actions,
    link,
    headingLevel: H = 'h2',
    headingId,
    size = 'sm',
    busy = false,
    busyLabel = 'Loading',
    className,
    children,
    ...rest
  },
  ref,
) {
  const auto = useId();
  const id = headingId ?? `mn-section-header-${auto}`;
  const hasBody = children !== undefined && children !== null && children !== false;
  const bar = (
    <div className="mn-section-header__bar">
      <div className="mn-section-header__text">
        <div className="mn-section-header__heading">
          <H id={id} className="mn-section-header__title">
            {title}
          </H>
          {busy ? (
            <Spinner size="sm" label={busyLabel} className="mn-section-header__spinner" />
          ) : null}
        </div>
        {description ? <p className="mn-section-header__description">{description}</p> : null}
        {meta ? <p className="mn-section-header__meta">{meta}</p> : null}
      </div>
      {actions || link ? (
        <div className="mn-section-header__trailing">
          {actions}
          {link}
        </div>
      ) : null}
    </div>
  );
  const common = {
    ...rest,
    'aria-busy': busy || undefined,
    'data-size': size,
    'data-busy': busy || undefined,
    className: cx('mn-section-header', className),
  };
  if (!hasBody) {
    return (
      <div ref={ref as Ref<HTMLDivElement>} {...common}>
        {bar}
      </div>
    );
  }
  return (
    <section ref={ref} aria-labelledby={id} {...common}>
      {bar}
      <div className="mn-section-header__body">{children}</div>
    </section>
  );
});

export interface SectionHeaderLinkProps extends AnchorHTMLAttributes<HTMLAnchorElement> {
  /** Render the child element (a router link) with this styling. */
  asChild?: boolean;
}

/**
 * The trailing "View all" link of a SectionHeader, with a decorative arrow.
 * Give it text that makes sense out of context ("View all invoices") or an
 * `aria-label`, since a page often has several.
 */
export const SectionHeaderLink = forwardRef<HTMLAnchorElement, SectionHeaderLinkProps>(
  function SectionHeaderLink({ asChild = false, className, children, ...rest }, ref) {
    const Comp = asChild ? Slot : 'a';
    return (
      <Comp ref={ref} {...rest} className={cx('mn-section-header__link', className)}>
        <Slottable>{children}</Slottable>
        <ChevronRightGlyph className="mn-section-header__arrow" />
      </Comp>
    );
  },
);
