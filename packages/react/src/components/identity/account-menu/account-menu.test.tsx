import { describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { DropdownMenuItem } from '../../overlays/dropdown-menu/index.js';
import { AccountMenu, type AccountMenuWorkspace } from './index.js';

const USER = { name: 'Sam Rivera', email: 'sam@example.com', id: 'u-1' };
const WORKSPACES: AccountMenuWorkspace[] = [
  { id: 'w-1', name: 'Northwind' },
  { id: 'w-2', name: 'Harbor Labs' },
];

describe('AccountMenu', () => {
  it('opens from the keyboard with the person, links and sign out', async () => {
    const user = userEvent.setup();
    const onSignOut = vi.fn();
    render(
      <AccountMenu
        user={USER}
        profileHref="/profile"
        onSettings={() => undefined}
        onSignOut={onSignOut}
      />,
    );
    const trigger = screen.getByRole('button', { name: 'Account menu for Sam Rivera' });
    // Named by its content (label in name), not by an aria-label that hides the initials.
    expect(trigger).not.toHaveAttribute('aria-label');
    expect(trigger).toHaveClass('mn-account-menu');
    expect(trigger).toHaveAttribute('aria-haspopup', 'menu');
    trigger.focus();
    await user.keyboard('{Enter}');
    const menu = await screen.findByRole('menu');
    expect(menu).toHaveTextContent('sam@example.com');
    const profile = within(menu).getByRole('menuitem', { name: 'Profile' });
    expect(profile).toHaveAttribute('href', '/profile');
    await waitFor(() => expect(profile).toHaveFocus());
    await expectNoA11yViolations();
    await user.keyboard('{ArrowDown}{ArrowDown}{Enter}');
    expect(onSignOut).toHaveBeenCalledOnce();
    await waitFor(() => expect(screen.queryByRole('menu')).toBeNull());
    expect(trigger).toHaveFocus();
  });

  it('switches workspace and theme with radio items', async () => {
    const user = userEvent.setup();
    const onWorkspaceChange = vi.fn();
    const onThemeChange = vi.fn();
    render(
      <AccountMenu
        user={USER}
        workspaces={WORKSPACES}
        currentWorkspaceId="w-1"
        onWorkspaceChange={onWorkspaceChange}
        theme="light"
        onThemeChange={onThemeChange}
        defaultOpen
      />,
    );
    const current = await screen.findByRole('menuitemradio', { name: 'Northwind' });
    expect(current).toHaveAttribute('aria-checked', 'true');
    expect(screen.getByRole('group', { name: 'Switch workspace' })).toBeInTheDocument();
    await user.click(screen.getByRole('menuitemradio', { name: 'Harbor Labs' }));
    expect(onWorkspaceChange).toHaveBeenCalledWith('w-2');
    await user.click(screen.getByRole('button', { name: /Account menu/ }));
    await user.click(await screen.findByRole('menuitemradio', { name: 'Dark' }));
    expect(onThemeChange).toHaveBeenCalledWith('dark');
  });

  it('renders a workspace slot, extra items and a custom link component', async () => {
    const onHelp = vi.fn();
    render(
      <AccountMenu
        user={USER}
        defaultOpen
        showName
        settingsHref="/settings"
        renderLink={({ href, className, children }) => (
          <a href={href} className={className} data-router="yes">
            {children}
          </a>
        )}
        workspaceSlot={<DropdownMenuItem>All workspaces…</DropdownMenuItem>}
        items={<DropdownMenuItem onSelect={onHelp}>Help</DropdownMenuItem>}
      />,
    );
    expect(await screen.findByRole('menuitem', { name: 'Settings' })).toHaveAttribute(
      'data-router',
      'yes',
    );
    expect(screen.getByRole('menuitem', { name: 'All workspaces…' })).toBeInTheDocument();
    expect(screen.queryByRole('menuitem', { name: 'Sign out' })).toBeNull();
    await userEvent.setup().click(screen.getByRole('menuitem', { name: 'Help' }));
    expect(onHelp).toHaveBeenCalled();
  });

  it('takes translated messages', async () => {
    render(
      <AccountMenu
        user={USER}
        defaultOpen
        onSignOut={() => undefined}
        messages={{ triggerLabel: (n) => `Konto von ${n}`, signOut: 'Abmelden' }}
      />,
    );
    expect(
      screen.getByRole('button', { name: 'Konto von Sam Rivera', hidden: true }),
    ).toBeInTheDocument();
    expect(await screen.findByRole('menuitem', { name: 'Abmelden' })).toBeInTheDocument();
  });

  it('renders on the server', () => {
    const html = renderToString(<AccountMenu user={USER} />);
    expect(html).toContain('mn-account-menu');
    expect(html).toContain('Account menu for Sam Rivera');
  });
});
