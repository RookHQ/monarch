'use client';

import { forwardRef, type AnchorHTMLAttributes, type MouseEvent } from 'react';
import { cx } from '../../../internal/cx.js';

export interface SkipLinkMessages {
  /** The link text. Default "Skip to main content". */
  label: string;
}

export const SKIP_LINK_MESSAGES: SkipLinkMessages = {
  label: 'Skip to main content',
};

export interface SkipLinkProps extends AnchorHTMLAttributes<HTMLAnchorElement> {
  /**
   * The id of the element to jump to, with a leading `#`. Default
   * `"#main-content"`: put `id="main-content"` on your `<main>`.
   */
  href?: string;
  /** Visible and spoken strings, for translation. `children` overrides `label`. */
  messages?: Partial<SkipLinkMessages> | undefined;
}

/**
 * The first focusable element on the page: a link that is hidden until it
 * receives keyboard focus, then lets people jump past the navigation to the
 * main content. Activating it moves focus to the target (making it focusable
 * with `tabindex="-1"` if needed) without changing the URL, so it also works
 * with hash-based routers.
 */
export const SkipLink = forwardRef<HTMLAnchorElement, SkipLinkProps>(function SkipLink(
  { href = '#main-content', messages, children, className, onClick, ...rest },
  ref,
) {
  const label = messages?.label ?? SKIP_LINK_MESSAGES.label;
  const handleClick = (event: MouseEvent<HTMLAnchorElement>) => {
    onClick?.(event);
    if (event.defaultPrevented || !href.startsWith('#') || href.length < 2) return;
    let id = href.slice(1);
    try {
      id = decodeURIComponent(id);
    } catch {
      // A stray "%" is not an escape: use the id as written.
    }
    const target = event.currentTarget.ownerDocument.getElementById(id);
    if (!target) return;
    event.preventDefault();
    if (!target.hasAttribute('tabindex') && target.tabIndex < 0) {
      target.setAttribute('tabindex', '-1');
    }
    target.focus();
    target.scrollIntoView?.({ block: 'start' });
  };
  return (
    <a
      ref={ref}
      {...rest}
      href={href}
      onClick={handleClick}
      className={cx('mn-skip-link', className)}
    >
      {children ?? label}
    </a>
  );
});
