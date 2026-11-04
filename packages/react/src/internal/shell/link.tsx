import {
  forwardRef,
  type AnchorHTMLAttributes,
  type ComponentType,
  type ReactNode,
  type Ref,
} from 'react';

/**
 * The element shell links render as. Pass your router's link (Next.js `Link`,
 * React Router `Link` adapted to `href`) for client navigation; the default is
 * a plain `<a>`. It must forward its ref and spread the props it receives.
 */
export type ShellLinkComponent = ComponentType<
  AnchorHTMLAttributes<HTMLAnchorElement> & { href: string; ref?: Ref<HTMLAnchorElement> }
>;

interface ShellLinkProps extends AnchorHTMLAttributes<HTMLAnchorElement> {
  href: string;
  as?: ShellLinkComponent | undefined;
  children?: ReactNode;
}

/** Renders `as` (or `<a>`) with the props. External links open in a new tab safely. */
export const ShellLink = forwardRef<HTMLAnchorElement, ShellLinkProps>(function ShellLink(
  { as: Component, children, ...props },
  ref,
) {
  if (Component)
    return (
      <Component ref={ref} {...props}>
        {children}
      </Component>
    );
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
