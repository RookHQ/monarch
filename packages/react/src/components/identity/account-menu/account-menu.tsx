'use client';

import {
  forwardRef,
  type ButtonHTMLAttributes,
  type ReactElement,
  type ReactNode,
  type SVGProps,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { ChevronDownGlyph } from '../../../internal/icons.js';
import { Avatar, type AvatarStatus } from '../avatar/index.js';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuGroup,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuRadioGroup,
  DropdownMenuRadioItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '../../overlays/dropdown-menu/index.js';

/** The signed-in person shown in the trigger and the menu header. */
export interface AccountMenuUser {
  name: string;
  email?: string | undefined;
  /** Profile photo URL. Falls back to initials. */
  avatarSrc?: string | null | undefined;
  /** Stable id used to pick the fallback avatar colour. */
  id?: string | undefined;
  /** Presence dot on the avatar. */
  status?: AvatarStatus | undefined;
}

/** A workspace the person can switch to from the menu. */
export interface AccountMenuWorkspace {
  id: string;
  name: string;
  /** Logo URL. Falls back to initials. */
  avatarSrc?: string | null | undefined;
}

/** Colour scheme choice. */
export type AccountMenuTheme = 'light' | 'dark' | 'system';

/** Visible strings, for translation. */
export interface AccountMenuMessages {
  /** Accessible name of the trigger. Default `Account menu for ${name}`. */
  triggerLabel: (name: string) => string;
  /** Default "Profile". */
  profile: string;
  /** Default "Settings". */
  settings: string;
  /** Label above the workspace list. Default "Switch workspace". */
  workspaces: string;
  /** Label above the theme choices. Default "Theme". */
  theme: string;
  /** Default "Light". */
  themeLight: string;
  /** Default "Dark". */
  themeDark: string;
  /** Default "System". */
  themeSystem: string;
  /** Default "Sign out". */
  signOut: string;
}

export const defaultAccountMenuMessages: AccountMenuMessages = {
  triggerLabel: (name) => `Account menu for ${name}`,
  profile: 'Profile',
  settings: 'Settings',
  workspaces: 'Switch workspace',
  theme: 'Theme',
  themeLight: 'Light',
  themeDark: 'Dark',
  themeSystem: 'System',
  signOut: 'Sign out',
};

/** Props a `renderLink` function receives for each link item. */
export interface AccountMenuLinkProps {
  href: string;
  className: string;
  children: ReactNode;
}

export interface AccountMenuProps extends Omit<
  ButtonHTMLAttributes<HTMLButtonElement>,
  'children' | 'type'
> {
  /** The signed-in person. */
  user: AccountMenuUser;
  /** Show the name beside the avatar in the trigger. Default `false` (avatar only). */
  showName?: boolean | undefined;
  /** Profile page link. */
  profileHref?: string | undefined;
  /** Called when Profile is chosen, instead of (or as well as) following `profileHref`. */
  onProfile?: (() => void) | undefined;
  /** Settings page link. */
  settingsHref?: string | undefined;
  /** Called when Settings is chosen. */
  onSettings?: (() => void) | undefined;
  /**
   * Renders link items with the product's router link (for example Next.js
   * `Link`). Default: a plain `<a>`.
   */
  renderLink?: ((props: AccountMenuLinkProps) => ReactElement) | undefined;
  /** Workspaces to switch between, listed as radio items. */
  workspaces?: AccountMenuWorkspace[] | undefined;
  /** Id of the active workspace. */
  currentWorkspaceId?: string | undefined;
  /** Called with the chosen workspace id. */
  onWorkspaceChange?: ((id: string) => void) | undefined;
  /**
   * Replaces the built-in workspace list with the product's own menu items
   * (DropdownMenu items, groups or a submenu).
   */
  workspaceSlot?: ReactNode;
  /** The current colour scheme. The theme choices show when `onThemeChange` is set. */
  theme?: AccountMenuTheme | undefined;
  /** Called with the chosen colour scheme. */
  onThemeChange?: ((theme: AccountMenuTheme) => void) | undefined;
  /** Extra DropdownMenu items, shown above Sign out. */
  items?: ReactNode;
  /** Called when Sign out is chosen. Without it there is no Sign out item. */
  onSignOut?: (() => void) | undefined;
  /** Controlled open state. */
  open?: boolean | undefined;
  /** Initial open state when uncontrolled. */
  defaultOpen?: boolean | undefined;
  onOpenChange?: ((open: boolean) => void) | undefined;
  /** Menu alignment against the trigger. Default `end`. */
  align?: 'start' | 'center' | 'end' | undefined;
  /** Visible strings, for translation. */
  messages?: Partial<AccountMenuMessages> | undefined;
}

const plainLink = ({ href, className, children }: AccountMenuLinkProps) => (
  <a href={href} className={className}>
    {children}
  </a>
);

/**
 * The signed-in person's menu: an avatar button that opens a menu with who
 * is signed in, Profile and Settings, a workspace switcher, the colour scheme,
 * product items and Sign out.
 */
export const AccountMenu = forwardRef<HTMLButtonElement, AccountMenuProps>(function AccountMenu(
  {
    user,
    showName = false,
    profileHref,
    onProfile,
    settingsHref,
    onSettings,
    renderLink = plainLink,
    workspaces,
    currentWorkspaceId,
    onWorkspaceChange,
    workspaceSlot,
    theme,
    onThemeChange,
    items,
    onSignOut,
    open,
    defaultOpen,
    onOpenChange,
    align = 'end',
    messages,
    className,
    ...rest
  },
  ref,
) {
  const m = { ...defaultAccountMenuMessages, ...messages };
  const rootProps = {
    ...(open !== undefined ? { open } : {}),
    ...(defaultOpen !== undefined ? { defaultOpen } : {}),
    ...(onOpenChange ? { onOpenChange } : {}),
  };

  const navItem = (
    label: string,
    href: string | undefined,
    onSelect: (() => void) | undefined,
    icon: ReactNode,
  ) => {
    if (!href && !onSelect) return null;
    if (href) {
      return (
        <DropdownMenuItem asChild {...(onSelect ? { onSelect } : {})}>
          {renderLink({
            href,
            className: 'mn-account-menu__link',
            children: (
              <>
                <span className="mn-account-menu__icon" aria-hidden="true">
                  {icon}
                </span>
                <span>{label}</span>
              </>
            ),
          })}
        </DropdownMenuItem>
      );
    }
    return (
      <DropdownMenuItem icon={icon} onSelect={onSelect}>
        {label}
      </DropdownMenuItem>
    );
  };

  const profile = navItem(m.profile, profileHref, onProfile, <PersonGlyph />);
  const settings = navItem(m.settings, settingsHref, onSettings, <GearGlyph />);
  const showWorkspaces = workspaceSlot != null || (workspaces && workspaces.length > 0);

  return (
    <DropdownMenu {...rootProps}>
      <DropdownMenuTrigger asChild>
        <button
          ref={ref}
          type="button"
          {...rest}
          className={cx('mn-account-menu', className)}
          data-show-name={showName || undefined}
        >
          <Avatar
            name={user.name}
            src={user.avatarSrc ?? null}
            size="sm"
            decorative
            {...(user.id ? { seed: user.id } : {})}
            {...(user.status ? { status: user.status } : {})}
          />
          {/* The full label as text, not aria-label, so the visible name (and the avatar's
              initials) never contradict an override: the label contains the name. */}
          <span className="mn-visually-hidden">{m.triggerLabel(user.name)}</span>
          {showName ? (
            <>
              <span className="mn-account-menu__trigger-name" aria-hidden="true">
                {user.name}
              </span>
              <ChevronDownGlyph className="mn-account-menu__chevron" aria-hidden="true" />
            </>
          ) : null}
        </button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align={align} className="mn-account-menu__content">
        <DropdownMenuLabel className="mn-account-menu__header">
          <span className="mn-account-menu__name">{user.name}</span>
          {user.email ? <span className="mn-account-menu__email">{user.email}</span> : null}
        </DropdownMenuLabel>
        {profile || settings ? (
          <>
            <DropdownMenuSeparator />
            <DropdownMenuGroup>
              {profile}
              {settings}
            </DropdownMenuGroup>
          </>
        ) : null}
        {showWorkspaces ? (
          <>
            <DropdownMenuSeparator />
            {workspaceSlot ?? (
              <DropdownMenuGroup aria-label={m.workspaces}>
                <DropdownMenuLabel className="mn-account-menu__section">
                  {m.workspaces}
                </DropdownMenuLabel>
                <DropdownMenuRadioGroup
                  value={currentWorkspaceId ?? ''}
                  onValueChange={(id) => onWorkspaceChange?.(id)}
                >
                  {workspaces?.map((workspace) => (
                    <DropdownMenuRadioItem key={workspace.id} value={workspace.id}>
                      <span className="mn-account-menu__workspace">
                        <Avatar
                          name={workspace.name}
                          src={workspace.avatarSrc ?? null}
                          size="xs"
                          shape="square"
                          seed={workspace.id}
                          decorative
                        />
                        <span>{workspace.name}</span>
                      </span>
                    </DropdownMenuRadioItem>
                  ))}
                </DropdownMenuRadioGroup>
              </DropdownMenuGroup>
            )}
          </>
        ) : null}
        {onThemeChange ? (
          <>
            <DropdownMenuSeparator />
            <DropdownMenuGroup aria-label={m.theme}>
              <DropdownMenuLabel className="mn-account-menu__section">{m.theme}</DropdownMenuLabel>
              <DropdownMenuRadioGroup
                value={theme ?? 'system'}
                onValueChange={(value) => onThemeChange(value as AccountMenuTheme)}
              >
                <DropdownMenuRadioItem value="light">{m.themeLight}</DropdownMenuRadioItem>
                <DropdownMenuRadioItem value="dark">{m.themeDark}</DropdownMenuRadioItem>
                <DropdownMenuRadioItem value="system">{m.themeSystem}</DropdownMenuRadioItem>
              </DropdownMenuRadioGroup>
            </DropdownMenuGroup>
          </>
        ) : null}
        {items ? (
          <>
            <DropdownMenuSeparator />
            {items}
          </>
        ) : null}
        {onSignOut ? (
          <>
            <DropdownMenuSeparator />
            <DropdownMenuItem icon={<SignOutGlyph />} onSelect={onSignOut}>
              {m.signOut}
            </DropdownMenuItem>
          </>
        ) : null}
      </DropdownMenuContent>
    </DropdownMenu>
  );
});

const glyph = {
  width: 16,
  height: 16,
  viewBox: '0 0 16 16',
  fill: 'none',
  stroke: 'currentColor',
  strokeWidth: 1.5,
  strokeLinecap: 'round' as const,
  strokeLinejoin: 'round' as const,
};

function PersonGlyph(props: SVGProps<SVGSVGElement>) {
  return (
    <svg {...glyph} {...props}>
      <circle cx="8" cy="5.5" r="2.75" />
      <path d="M2.75 14c.6-2.6 2.7-4 5.25-4s4.65 1.4 5.25 4" />
    </svg>
  );
}

function GearGlyph(props: SVGProps<SVGSVGElement>) {
  return (
    <svg {...glyph} {...props}>
      <circle cx="8" cy="8" r="2" />
      <path d="M8 1.5v1.75M8 12.75v1.75M1.5 8h1.75M12.75 8h1.75M3.4 3.4l1.25 1.25M11.35 11.35l1.25 1.25M3.4 12.6l1.25-1.25M11.35 4.65l1.25-1.25" />
    </svg>
  );
}

function SignOutGlyph(props: SVGProps<SVGSVGElement>) {
  return (
    <svg {...glyph} {...props}>
      <path d="M6 13.5H3.5a1 1 0 0 1-1-1v-9a1 1 0 0 1 1-1H6M10.5 11 13.5 8l-3-3M13.5 8H6" />
    </svg>
  );
}
