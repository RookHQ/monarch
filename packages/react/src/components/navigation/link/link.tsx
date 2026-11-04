import { forwardRef, type AnchorHTMLAttributes } from 'react';
import { Slot } from '@radix-ui/react-slot';
import { cx } from '../../../internal/cx.js';
import { VisuallyHidden } from '../../layout/visually-hidden/index.js';

export interface LinkProps extends AnchorHTMLAttributes<HTMLAnchorElement> {
  /** `inline` (default) for links in text, `standalone` for links in lists and footers. */
  variant?: 'inline' | 'standalone' | 'subtle';
  /** Render the child element (e.g. a Next.js Link) with link styling. */
  asChild?: boolean;
}

function ExternalGlyph() {
  return (
    <svg
      aria-hidden
      focusable={false}
      width="12"
      height="12"
      viewBox="0 0 16 16"
      fill="none"
      stroke="currentColor"
      strokeWidth={1.75}
      strokeLinecap="round"
      strokeLinejoin="round"
    >
      <path d="M6.5 3.5h-3v9h9v-3M9.5 2.5h4v4M13.5 2.5 7 9" />
    </svg>
  );
}

/**
 * Text link. When `target="_blank"` it adds `rel="noopener noreferrer"`, an
 * external icon and a hidden "opens in a new tab" note.
 */
export const Link = forwardRef<HTMLAnchorElement, LinkProps>(function Link(
  { variant = 'inline', asChild = false, target, rel, className, children, ...rest },
  ref,
) {
  const external = target === '_blank';
  const Comp = asChild ? Slot : 'a';
  return (
    <Comp
      ref={ref}
      {...rest}
      target={target}
      rel={external ? cx(rel, 'noopener', 'noreferrer') : rel}
      data-variant={variant}
      className={cx('mn-link', className)}
    >
      {children}
      {external ? (
        <>
          <span className="mn-link__external">
            <ExternalGlyph />
          </span>
          <VisuallyHidden> (opens in a new tab)</VisuallyHidden>
        </>
      ) : null}
    </Comp>
  );
});
