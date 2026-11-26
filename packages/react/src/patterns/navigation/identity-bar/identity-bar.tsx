'use client';

import { forwardRef, useMemo, type HTMLAttributes, type ReactNode } from 'react';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuTrigger,
} from '../../../components/overlays/dropdown-menu/index.js';
import { Avatar } from '../../../components/identity/avatar/index.js';
import { cx } from '../../../internal/cx.js';
import {
  AccountMenuItems,
  type AccountMenuMessages,
  type ShellAccount,
} from '../../../internal/shell/account-menu.js';
import { ShellLink, type ShellLinkComponent } from '../../../internal/shell/link.js';

export interface IdentityBarProduct {
  /** Product name, always visible, e.g. "Rook Tickets". */
  name: string;
  /** Home link. Without it the name is plain text. */
  href?: string | undefined;
  /** Product logo or icon. Decorative: the name names it. */
  logo?: ReactNode;
}

export type IdentityBarMessages = AccountMenuMessages;

const DEFAULT_MESSAGES: IdentityBarMessages = {
  accountMenu: (name) => `Account: ${name}`,
  profile: 'Profile and preferences',
  account: 'Rook account',
  signOut: 'Sign out',
  newTab: (product) => (product ? `opens in ${product} in a new tab` : 'opens in a new tab'),
};

export interface IdentityBarProps extends Omit<HTMLAttributes<HTMLElement>, 'children'> {
  product: IdentityBarProduct;
  /** After the product name: a breadcrumb, a page title or a Space switcher. */
  start?: ReactNode;
  /** Search field or command-palette button. Takes the free space; wraps below on narrow screens. */
  search?: ReactNode;
  /** Notifications button or popover trigger. */
  notifications?: ReactNode;
  /** Other trailing actions (help, theme toggle), before notifications. */
  actions?: ReactNode;
  /** The signed-in person: avatar button and account menu. */
  account?: ShellAccount | undefined;
  /** Link element for client navigation. Default `<a>`. */
  linkComponent?: ShellLinkComponent | undefined;
  /** Stick to the top of the scrolling area. Default true. */
  sticky?: boolean | undefined;
  /** Interface text, for translation. */
  messages?: Partial<IdentityBarMessages> | undefined;
}

/**
 * The top identity bar of a Rook app: the product name, an optional start
 * slot (breadcrumb, Space), a search slot, trailing actions, notifications and
 * the account menu. It is the page banner (`<header>`); pair it with RookRail
 * inside AppShell, or use it alone in apps without a rail.
 */
export const IdentityBar = forwardRef<HTMLElement, IdentityBarProps>(function IdentityBar(
  {
    product,
    start,
    search,
    notifications,
    actions,
    account,
    linkComponent,
    sticky = true,
    messages: messagesProp,
    className,
    ...rest
  },
  ref,
) {
  const messages = useMemo(() => ({ ...DEFAULT_MESSAGES, ...messagesProp }), [messagesProp]);
  const productInner = (
    <>
      {product.logo ? (
        <span className="mn-identity-bar__logo" aria-hidden>
          {product.logo}
        </span>
      ) : null}
      <span className="mn-identity-bar__name">{product.name}</span>
    </>
  );
  return (
    <header
      ref={ref}
      {...rest}
      className={cx('mn-identity-bar', className)}
      data-sticky={sticky ? '' : undefined}
    >
      <div className="mn-identity-bar__start">
        {product.href ? (
          <ShellLink as={linkComponent} href={product.href} className="mn-identity-bar__product">
            {productInner}
          </ShellLink>
        ) : (
          <span className="mn-identity-bar__product">{productInner}</span>
        )}
        {start ? <div className="mn-identity-bar__context">{start}</div> : null}
      </div>
      {search ? <div className="mn-identity-bar__search">{search}</div> : null}
      <div className="mn-identity-bar__end">
        {actions}
        {notifications}
        {account ? (
          <DropdownMenu>
            <DropdownMenuTrigger className="mn-identity-bar__account">
              {/* Named by text, not aria-label: the visible initials are decorative. */}
              <span className="mn-visually-hidden">{messages.accountMenu(account.name)}</span>
              <Avatar
                name={account.name}
                src={account.avatarUrl ?? null}
                size={28}
                decorative
                {...(account.status ? { status: account.status } : {})}
              />
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end" sideOffset={6} className="mn-identity-bar__menu">
              <AccountMenuItems account={account} messages={messages} />
            </DropdownMenuContent>
          </DropdownMenu>
        ) : null}
      </div>
    </header>
  );
});
