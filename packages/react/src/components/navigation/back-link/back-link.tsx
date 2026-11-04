import { forwardRef, type AnchorHTMLAttributes, type ReactNode } from 'react';
import { Slot, Slottable } from '@radix-ui/react-slot';
import { cx } from '../../../internal/cx.js';

export interface BackLinkProps extends AnchorHTMLAttributes<HTMLAnchorElement> {
  /** The destination, named in the text: "Back to domains", "All invoices". */
  children: ReactNode;
  /** `subtle` (default) is muted text that darkens on hover; `standalone` uses the link colour. */
  variant?: 'subtle' | 'standalone';
  /**
   * Render the child element (e.g. a Next.js `<Link>` or React Router
   * `<NavLink>`) with back-link styling and the arrow, instead of an `<a>`.
   */
  asChild?: boolean;
}

function BackGlyph() {
  return (
    <svg
      aria-hidden
      focusable={false}
      width="16"
      height="16"
      viewBox="0 0 16 16"
      fill="none"
      stroke="currentColor"
      strokeWidth={1.75}
      strokeLinecap="round"
      strokeLinejoin="round"
    >
      <path d="M10 4 6 8l4 4" />
    </svg>
  );
}

/**
 * A link to the parent page, with a leading back arrow. Works with any
 * router through `asChild`.
 */
export const BackLink = forwardRef<HTMLAnchorElement, BackLinkProps>(function BackLink(
  { variant = 'subtle', asChild = false, className, children, ...rest },
  ref,
) {
  const Comp = asChild ? Slot : 'a';
  return (
    <Comp ref={ref} {...rest} data-variant={variant} className={cx('mn-back-link', className)}>
      <span className="mn-back-link__icon">
        <BackGlyph />
      </span>
      <Slottable>{children}</Slottable>
    </Comp>
  );
});
