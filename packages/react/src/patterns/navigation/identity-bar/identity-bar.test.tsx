import { describe, expect, it, vi } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { forwardRef, type AnchorHTMLAttributes } from 'react';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { IdentityBar } from './index.js';

describe('IdentityBar', () => {
  it('is the page banner with the product name linking home', async () => {
    render(
      <IdentityBar
        product={{ name: 'Acme Tickets', href: '/', logo: <svg /> }}
        search={<input type="search" aria-label="Search tickets" />}
        notifications={<button type="button">Notifications</button>}
      />,
    );
    const banner = screen.getByRole('banner');
    expect(within(banner).getByRole('link', { name: 'Acme Tickets' })).toHaveAttribute('href', '/');
    expect(within(banner).getByRole('searchbox', { name: 'Search tickets' })).toBeInTheDocument();
    expect(within(banner).getByRole('button', { name: 'Notifications' })).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('shows the name as text without a link, and renders start and actions slots', () => {
    render(
      <IdentityBar
        product={{ name: 'Acme Tickets' }}
        start={<span>Support queue</span>}
        actions={<button type="button">Help</button>}
      />,
    );
    expect(screen.queryByRole('link')).not.toBeInTheDocument();
    expect(screen.getByText('Acme Tickets')).toBeInTheDocument();
    expect(screen.getByText('Support queue')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Help' })).toBeInTheDocument();
  });

  it('opens the account menu from the keyboard, and signs out', async () => {
    const onSignOut = vi.fn();
    render(
      <IdentityBar
        product={{ name: 'Acme Tickets' }}
        account={{
          name: 'Grace Hopper',
          email: 'grace@example.com',
          profileHref: '/me',
          onSignOut,
        }}
      />,
    );
    const user = userEvent.setup();
    const trigger = screen.getByRole('button', { name: 'Account: Grace Hopper' });
    trigger.focus();
    await user.keyboard('{Enter}');
    const menu = await screen.findByRole('menu');
    expect(menu).toHaveTextContent('grace@example.com');
    expect(within(menu).getByRole('menuitem', { name: 'Profile and preferences' })).toHaveFocus();
    await expectNoA11yViolations();
    await user.keyboard('{ArrowDown}{Enter}');
    expect(onSignOut).toHaveBeenCalledTimes(1);
  });

  it('Escape closes the account menu and returns focus', async () => {
    render(<IdentityBar product={{ name: 'Acme' }} account={{ name: 'Grace Hopper' }} />);
    const user = userEvent.setup();
    const trigger = screen.getByRole('button', { name: 'Account: Grace Hopper' });
    await user.click(trigger);
    await screen.findByRole('menu');
    await user.keyboard('{Escape}');
    expect(screen.queryByRole('menu')).not.toBeInTheDocument();
    expect(trigger).toHaveFocus();
  });

  it('uses the app link component and translated messages', () => {
    const AppLink = forwardRef<HTMLAnchorElement, AnchorHTMLAttributes<HTMLAnchorElement>>(
      function AppLink({ children, ...props }, ref) {
        return (
          <a ref={ref} data-app-link="" {...props}>
            {children}
          </a>
        );
      },
    );
    render(
      <IdentityBar
        product={{ name: 'Acme', href: '/' }}
        linkComponent={AppLink}
        account={{ name: 'Grace Hopper' }}
        messages={{ accountMenu: (n) => `Compte : ${n}` }}
      />,
    );
    expect(screen.getByRole('link', { name: 'Acme' })).toHaveAttribute('data-app-link');
    expect(screen.getByRole('button', { name: 'Compte : Grace Hopper' })).toBeInTheDocument();
  });

  it('can stop being sticky', () => {
    const { rerender } = render(<IdentityBar product={{ name: 'Acme' }} />);
    expect(screen.getByRole('banner')).toHaveAttribute('data-sticky');
    rerender(<IdentityBar product={{ name: 'Acme' }} sticky={false} />);
    expect(screen.getByRole('banner')).not.toHaveAttribute('data-sticky');
  });

  it('renders on the server', () => {
    expect(renderToString(<IdentityBar product={{ name: 'Acme' }} />)).toContain('mn-identity-bar');
  });
});
