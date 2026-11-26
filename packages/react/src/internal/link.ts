import type {
  AnchorHTMLAttributes,
  JSXElementConstructor,
  MouseEventHandler,
  ReactNode,
  TouchEventHandler,
} from 'react';

/**
 * The props Monarch passes to a link component. `href` is always a string:
 * where there is no target, Monarch renders a button or text instead of a link.
 *
 * `onClick`, `onMouseEnter` and `onTouchStart` are declared without `| undefined`
 * because router links (Next.js `Link`) declare them that way, and an app built
 * with `exactOptionalPropertyTypes` would otherwise reject them.
 */
export interface MonarchLinkProps extends Omit<
  AnchorHTMLAttributes<HTMLAnchorElement>,
  'href' | 'onClick' | 'onMouseEnter' | 'onTouchStart'
> {
  href: string;
  onClick?: MouseEventHandler<HTMLAnchorElement>;
  onMouseEnter?: MouseEventHandler<HTMLAnchorElement>;
  onTouchStart?: TouchEventHandler<HTMLAnchorElement>;
  children?: ReactNode;
}

/**
 * A link component for `linkAs` / `linkComponent` props: `'a'` (the default) or
 * your router's link, such as Next.js `Link` or a React Router link adapted to
 * `href`. It must render an `<a>`, spread the props it receives and forward
 * its ref.
 *
 * Typed as `JSXElementConstructor` rather than `ComponentType` so that
 * components with their own static fields (Next.js `Link`) are accepted.
 */
export type MonarchLinkComponent = 'a' | JSXElementConstructor<MonarchLinkProps>;
