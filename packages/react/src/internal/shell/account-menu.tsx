'use client';

import type { ReactNode } from 'react';
import type { AvatarStatus } from '../../components/identity/avatar/index.js';
import {
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
} from '../../components/overlays/dropdown-menu/index.js';
import { SignOutGlyph } from './icons.js';

/** The signed-in person, as the rail and the identity bar show them. */
export interface ShellAccount {
  name: string;
  email?: string | undefined;
  avatarUrl?: string | null | undefined;
  /** Presence on the avatar. */
  status?: AvatarStatus | undefined;
  /** Personal preferences page, listed first in the account menu. */
  profileHref?: string | undefined;
  /** Rook account (Rook ID) page, opened in a new tab. */
  accountHref?: string | undefined;
  /** Extra account-menu entries (DropdownMenuItem elements), above Sign out. */
  menu?: ReactNode;
  onSignOut?: (() => void) | undefined;
}

export interface AccountMenuMessages {
  /** The account menu button. */
  accountMenu: (name: string) => string;
  profile: string;
  account: string;
  signOut: string;
  /** Appended to links that open in a new tab. */
  newTab: (product?: string) => string;
}

/** The account menu's items: who is signed in, profile, extra entries, Rook account, Sign out. */
export function AccountMenuItems({
  account,
  messages,
}: {
  account: ShellAccount;
  messages: AccountMenuMessages;
}) {
  return (
    <>
      <DropdownMenuLabel>
        <span className="mn-shell-account__name">{account.name}</span>
        {account.email ? <span className="mn-shell-account__email">{account.email}</span> : null}
      </DropdownMenuLabel>
      <DropdownMenuSeparator />
      {account.profileHref ? (
        <DropdownMenuItem asChild>
          <a className="mn-shell-account__link" href={account.profileHref}>
            {messages.profile}
          </a>
        </DropdownMenuItem>
      ) : null}
      {account.menu}
      {account.accountHref ? (
        <DropdownMenuItem asChild>
          <a
            className="mn-shell-account__link"
            href={account.accountHref}
            target="_blank"
            rel="noopener noreferrer"
          >
            {messages.account}
            <span className="mn-visually-hidden"> ({messages.newTab()})</span>
          </a>
        </DropdownMenuItem>
      ) : null}
      {account.onSignOut ? (
        <>
          <DropdownMenuSeparator />
          <DropdownMenuItem icon={<SignOutGlyph />} onSelect={account.onSignOut}>
            {messages.signOut}
          </DropdownMenuItem>
        </>
      ) : null}
    </>
  );
}
