import {
  forwardRef,
  type AnchorHTMLAttributes,
  type JSXElementConstructor,
  type ReactNode,
  type Ref,
} from 'react';
import type { MonarchLinkComponent } from '../link.js';

/**
 * The element shell links render as. Pass your router's link (Next.js `Link`,
 * React Router `Link` adapted to `href`) for client navigation; the default is
 * a plain `<a>`. It must forward its ref and spread the props it receives.
 * The same type as `MonarchLinkComponent`.
 */
export type ShellLinkComponent = MonarchLinkComponent;

interface ShellLinkProps extends AnchorHTMLAttributes<HTMLAnchorElement> {
  href: string;
  as?: ShellLinkComponent | undefined;
  children?: ReactNode;
}

/**
 * A link component as rendered here: it also receives the forwarded ref, and a handler may be
 * `undefined` at runtime (React ignores it), which `MonarchLinkProps` leaves out for router types.
 */
type RefLink = JSXElementConstructor<
  AnchorHTMLAttributes<HTMLAnchorElement> & { href: string; ref?: Ref<HTMLAnchorElement> }
>;

/** Renders `as` (or `<a>`) with the props. External links open in a new tab safely. */
export const ShellLink = forwardRef<HTMLAnchorElement, ShellLinkProps>(function ShellLink(
  { as: Component, children, ...props },
  ref,
) {
  if (Component && Component !== 'a') {
    const Link = Component as RefLink;
    return (
      <Link ref={ref} {...props}>
        {children}
      </Link>
    );
  }
  return (
    <a ref={ref} {...props}>
      {children}
    </a>
  );
});

/** Whether a path matches an item's href: exact, or a prefix at a segment boundary. */
export function pathMatches(href: string, currentPath: string, exact = false): boolean {
  const path = href.split('?')[0]?.split('#')[0] ?? href;
  if (!path) return false;
  if (exact || path === '/') return currentPath === path;
  return currentPath === path || currentPath.startsWith(`${path}/`);
}
