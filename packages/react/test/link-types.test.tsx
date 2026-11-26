import {
  forwardRef,
  type AnchorHTMLAttributes,
  type ElementType,
  type ForwardRefExoticComponent,
  type MouseEvent,
  type ReactNode,
  type RefAttributes,
} from 'react';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import {
  Breadcrumb,
  SidebarNav,
  type BottomTabBarProps,
  type BreadcrumbProps,
  type FavoritesListProps,
  type MegaMenuProps,
  type MonarchLinkComponent,
  type SidebarNavLinkComponent,
  type SidebarNavProps,
  type TopNavProps,
} from '../src/index.js';
import type { IdentityBarProps, RookRailProps, ShellLinkComponent } from '../src/entries/shell.js';

/*
 * Type-level checks, compiled by `npm run typecheck` (tsc includes test/). `linkAs` and
 * `linkComponent` must accept a router link whose `href` is required, as Next.js `Link` is.
 * The stand-in below has the shape of `next/link`'s default export; Monarch does not depend on Next.
 */
type NextLinkLike = ForwardRefExoticComponent<
  Omit<AnchorHTMLAttributes<HTMLAnchorElement>, 'href' | 'onClick'> & {
    href: string | { pathname?: string };
    onClick?: (e: MouseEvent<HTMLAnchorElement>) => void;
    prefetch?: boolean | null;
    children?: ReactNode;
  } & RefAttributes<HTMLAnchorElement>
>;

const NextLink: NextLinkLike = forwardRef(function NextLink(
  { href, prefetch, children, ...props },
  ref,
) {
  return (
    <a
      ref={ref}
      {...props}
      data-next-link=""
      data-prefetch={prefetch === undefined ? undefined : String(prefetch)}
      href={typeof href === 'string' ? href : (href.pathname ?? '')}
    >
      {children}
    </a>
  );
});

/** A component that needs a prop Monarch never passes must be rejected. */
function NeedsTo(props: { href: string; to: string; children?: ReactNode }) {
  return <a href={props.href}>{props.to}</a>;
}

// Accepted everywhere a link component is taken.
export const accepted: Array<MonarchLinkComponent | undefined> = [
  NextLink,
  'a',
  NextLink satisfies SidebarNavLinkComponent,
  NextLink satisfies ShellLinkComponent,
  NextLink satisfies NonNullable<BreadcrumbProps['linkAs']>,
  NextLink satisfies NonNullable<SidebarNavProps['linkAs']>,
  NextLink satisfies NonNullable<TopNavProps['linkAs']>,
  NextLink satisfies NonNullable<MegaMenuProps['linkAs']>,
  NextLink satisfies NonNullable<BottomTabBarProps['linkAs']>,
  NextLink satisfies NonNullable<FavoritesListProps['linkAs']>,
  NextLink satisfies NonNullable<RookRailProps['linkComponent']>,
  NextLink satisfies NonNullable<IdentityBarProps['linkComponent']>,
];

// @ts-expect-error a link component that requires a prop Monarch does not pass
export const rejected: MonarchLinkComponent = NeedsTo;

// @ts-expect-error the old type made `href` optional, which Next's Link rejects
export const oldTypeRejectsNext: ElementType<AnchorHTMLAttributes<HTMLAnchorElement>> = NextLink;

describe('router links', () => {
  it('receive a string href, and items without a page are not links', async () => {
    render(
      <Breadcrumb
        linkAs={NextLink}
        maxVisible={4}
        items={[
          { label: 'Home', href: '/' },
          { label: 'Projects', href: '/projects' },
          { label: 'Archive' },
          { label: 'Alpha', href: '/projects/alpha' },
          { label: 'Settings' },
        ]}
      />,
    );
    expect(screen.getByRole('link', { name: 'Home' })).toHaveAttribute('data-next-link');
    expect(screen.getByRole('link', { name: 'Alpha' })).toHaveAttribute('href', '/projects/alpha');
    await userEvent.setup().click(screen.getByRole('button', { name: /more levels/ }));
    expect(await screen.findByRole('menuitem', { name: 'Projects' })).toHaveAttribute(
      'href',
      '/projects',
    );
    const archive = screen.getByRole('menuitem', { name: 'Archive' });
    expect(archive).not.toHaveAttribute('href');
    expect(archive).toHaveAttribute('aria-disabled', 'true');
  });

  it('work in SidebarNav', () => {
    render(
      <SidebarNav
        linkAs={NextLink}
        sections={[
          {
            key: 's',
            items: [
              { key: 'a', label: 'Alpha', href: '/a' },
              { key: 'b', label: 'Bravo' },
            ],
          },
        ]}
      />,
    );
    expect(screen.getByRole('link', { name: 'Alpha' })).toHaveAttribute('data-next-link');
    expect(screen.getByRole('button', { name: 'Bravo' })).not.toHaveAttribute('href');
  });
});
