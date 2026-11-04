import { describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { forwardRef, type AnchorHTMLAttributes } from 'react';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import type { ShellCookieStore } from '../../../internal/shell/cookies.js';
import { RookRail, rookRailScript, type RookRailProps } from './index.js';

function memoryStore(initial: Record<string, string> = {}) {
  const jar = new Map(Object.entries(initial));
  const set = vi.fn((name: string, value: string) => void jar.set(name, value));
  const store: ShellCookieStore = { get: (n) => jar.get(n) ?? null, set };
  return { store, set };
}

const Icon = () => <svg data-testid="icon" />;
/** The desktop panel (the drawer has its own copy of the items). */
const panel = () => document.querySelector<HTMLElement>('.mn-rook-rail__panel')!;

function rail(props: Partial<RookRailProps> = {}) {
  const { store } = memoryStore();
  return (
    <RookRail
      cookies={store}
      brand={{ label: 'Acme Calendar home', href: '/w/acme', icon: <Icon /> }}
      currentPath="/w/acme/calendar/week"
      sections={[
        {
          key: 'main',
          items: [
            { key: 'overview', label: 'Overview', icon: <Icon />, href: '/w/acme/overview' },
            { key: 'calendar', label: 'Calendar', icon: <Icon />, href: '/w/acme/calendar' },
            { key: 'inbox', label: 'Inbox', icon: <Icon />, href: '/w/acme/inbox', badge: 3 },
          ],
        },
      ]}
      workspaceSection={{
        key: 'workspace',
        items: [
          {
            key: 'settings',
            label: 'Settings',
            icon: <Icon />,
            href: 'https://id.example.com/settings',
            external: true,
            opensIn: 'Rook ID',
          },
        ],
      }}
      account={{ name: 'Ada Lovelace', email: 'ada@example.com', onSignOut: () => {} }}
      {...props}
    />
  );
}

describe('RookRail', () => {
  it('renders a Main navigation landmark with the active item', async () => {
    const { container } = render(rail());
    const aside = container.querySelector<HTMLElement>('.mn-rook-rail__panel')!;
    const nav = within(aside).getByRole('navigation', { name: 'Main' });
    expect(within(nav).getByRole('link', { name: 'Calendar' })).toHaveAttribute(
      'aria-current',
      'page',
    );
    expect(within(nav).getByRole('link', { name: 'Overview' })).not.toHaveAttribute('aria-current');
    expect(within(aside).getByRole('link', { name: 'Acme Calendar home' })).toHaveAttribute(
      'href',
      '/w/acme',
    );
    await expectNoA11yViolations();
  });

  it('names badges and external links for screen readers', () => {
    render(rail());
    const aside = panel();
    expect(within(aside).getByRole('link', { name: 'Inbox , 3 unread' })).toBeInTheDocument();
    const settings = within(aside).getByRole('link', {
      name: 'Settings (opens in Rook ID in a new tab)',
    });
    expect(settings).toHaveAttribute('target', '_blank');
    expect(settings).toHaveAttribute('rel', 'noopener noreferrer');
  });

  it('puts the Workspace block under its heading, with Help & support right above it', async () => {
    const onOpen = vi.fn();
    render(rail({ support: { onOpen, unread: 2 } }));
    const aside = panel();
    const block = within(aside).getByRole('list', { name: 'Workspace' });
    expect(within(block).getByRole('link', { name: /Settings/ })).toBeInTheDocument();
    const help = within(aside).getByRole('button', { name: 'Help & support , 2 unread' });
    expect(help).toHaveAttribute('aria-haspopup', 'dialog');
    expect(help).toHaveAttribute('aria-expanded', 'false');
    expect(help.compareDocumentPosition(block) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
    await userEvent.setup().click(help);
    expect(onOpen).toHaveBeenCalledTimes(1);
  });

  it('marks Help & support open and points at its panel', () => {
    render(rail({ support: { onOpen: () => {}, open: true, panelId: 'support-panel' } }));
    const help = within(panel()).getByRole('button', { name: 'Help & support' });
    expect(help).toHaveAttribute('aria-expanded', 'true');
    expect(help).toHaveAttribute('aria-controls', 'support-panel');
  });

  it('expands and collapses, saving the choice in the rook_rail cookie', async () => {
    const { store, set } = memoryStore();
    const onExpandedChange = vi.fn();
    const { container } = render(rail({ cookies: store, onExpandedChange }));
    const root = container.querySelector('.mn-rook-rail')!;
    await waitFor(() => expect(root).toHaveAttribute('data-state', 'collapsed'));
    const toggle = screen.getByRole('button', { name: 'Expand navigation' });
    expect(toggle).toHaveAttribute('aria-expanded', 'false');
    expect(toggle).toHaveAttribute('aria-controls', panel().id);
    await userEvent.setup().click(toggle);
    expect(set).toHaveBeenCalledWith(
      'rook_rail',
      'expanded',
      expect.objectContaining({ maxAge: 31536000 }),
    );
    expect(onExpandedChange).toHaveBeenCalledWith(true);
    expect(root).toHaveAttribute('data-state', 'expanded');
    expect(document.documentElement).toHaveAttribute('data-rook-rail', 'expanded');
    const collapse = screen.getByRole('button', { name: 'Collapse navigation' });
    expect(collapse).toHaveAttribute('aria-expanded', 'true');
    await userEvent.setup().click(collapse);
    expect(set).toHaveBeenLastCalledWith('rook_rail', 'collapsed', expect.anything());
    expect(document.documentElement).not.toHaveAttribute('data-rook-rail');
  });

  it('starts expanded when the cookie says so, and follows a change from another rail', async () => {
    const { store } = memoryStore({ rook_rail: 'expanded' });
    const { container } = render(
      <>
        {rail({ cookies: store })}
        {rail({ cookies: store, id: 'second' })}
      </>,
    );
    const [a, b] = container.querySelectorAll('.mn-rook-rail');
    await waitFor(() => expect(a).toHaveAttribute('data-state', 'expanded'));
    await userEvent
      .setup()
      .click(within(a as HTMLElement).getByRole('button', { name: 'Collapse navigation' }));
    expect(b).toHaveAttribute('data-state', 'collapsed');
  });

  it('is controllable and can skip persistence', async () => {
    const { store, set } = memoryStore();
    const onExpandedChange = vi.fn();
    const { container, rerender } = render(
      rail({ cookies: store, expanded: false, onExpandedChange }),
    );
    await userEvent.setup().click(screen.getByRole('button', { name: 'Expand navigation' }));
    expect(onExpandedChange).toHaveBeenCalledWith(true);
    expect(container.querySelector('.mn-rook-rail')).toHaveAttribute('data-state', 'collapsed');
    rerender(rail({ cookies: store, expanded: true }));
    expect(container.querySelector('.mn-rook-rail')).toHaveAttribute('data-state', 'expanded');

    expect(set).not.toHaveBeenCalled();
  });

  it('keeps the state locally with persist={false}', async () => {
    const { store, set } = memoryStore({ rook_rail: 'collapsed' });
    const { container } = render(rail({ cookies: store, persist: false, defaultExpanded: true }));
    expect(container.querySelector('.mn-rook-rail')).toHaveAttribute('data-state', 'expanded');
    await userEvent.setup().click(screen.getByRole('button', { name: 'Collapse navigation' }));
    expect(container.querySelector('.mn-rook-rail')).toHaveAttribute('data-state', 'collapsed');
    expect(set).not.toHaveBeenCalled();
  });

  it('writes the cookie on the configured domain', async () => {
    const { store, set } = memoryStore();
    render(rail({ cookies: store, cookieName: 'acme_rail', cookieDomain: '.example.com' }));
    await userEvent.setup().click(screen.getByRole('button', { name: 'Expand navigation' }));
    expect(set).toHaveBeenCalledWith('acme_rail', 'expanded', {
      maxAge: 31536000,
      domain: '.example.com',
    });
  });

  it('moves focus with the arrow keys, Home and End', async () => {
    render(rail());
    const user = userEvent.setup();
    const aside = panel();
    within(aside).getByRole('link', { name: 'Overview' }).focus();
    await user.keyboard('{ArrowDown}');
    expect(within(aside).getByRole('link', { name: 'Calendar' })).toHaveFocus();
    await user.keyboard('{ArrowUp}{ArrowUp}');
    expect(within(aside).getByRole('link', { name: 'Acme Calendar home' })).toHaveFocus();
    await user.keyboard('{End}');
    expect(within(aside).getByRole('button', { name: /^Account: Ada Lovelace/ })).toHaveFocus();
    await user.keyboard('{Home}');
    expect(within(aside).getByRole('link', { name: 'Acme Calendar home' })).toHaveFocus();
  });

  it('shows a tooltip with the label and shortcut while collapsed', async () => {
    render(
      rail({
        quickActions: [{ key: 'new', label: 'New event', icon: <Icon />, shortcut: 'C' }],
      }),
    );
    const user = userEvent.setup();
    await waitFor(() =>
      expect(document.querySelector('.mn-rook-rail')).toHaveAttribute('data-state', 'collapsed'),
    );
    const action = screen.getByRole('button', { name: 'New event' });
    expect(action).toHaveAttribute('aria-keyshortcuts', 'C');
    await user.hover(action);
    const tip = await screen.findByRole('tooltip');
    expect(tip).toHaveTextContent('New event');
    expect(tip).toHaveTextContent('C');
  });

  it('renders slots with their context', () => {
    const workspace = vi.fn(({ collapsed }: { collapsed: boolean }) => (
      <button type="button">{collapsed ? 'W' : 'Workspace switcher'}</button>
    ));
    render(
      rail({
        expanded: true,
        workspaceSwitcher: workspace,
        spaceSwitcher: <button type="button">Space: All spaces</button>,
        themeToggle: <button type="button">Theme</button>,
      }),
    );
    expect(workspace).toHaveBeenCalledWith({ collapsed: false, placement: 'rail' });
    expect(screen.getByRole('button', { name: 'Space: All spaces' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Theme' })).toBeInTheDocument();
  });

  it('opens the account menu with the person and Sign out', async () => {
    const onSignOut = vi.fn();
    render(
      rail({
        account: {
          name: 'Ada Lovelace',
          email: 'ada@example.com',
          profileHref: '/me',
          accountHref: 'https://id.example.com',
          onSignOut,
        },
      }),
    );
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: /^Account: Ada Lovelace/ }));
    const menu = await screen.findByRole('menu');
    expect(menu).toHaveTextContent('ada@example.com');
    expect(within(menu).getByRole('menuitem', { name: 'Profile and preferences' })).toHaveAttribute(
      'href',
      '/me',
    );
    expect(
      within(menu).getByRole('menuitem', { name: 'Rook account (opens in a new tab)' }),
    ).toHaveAttribute('target', '_blank');
    await expectNoA11yViolations();
    await user.click(within(menu).getByRole('menuitem', { name: 'Sign out' }));
    expect(onSignOut).toHaveBeenCalledTimes(1);
  });

  it('opens the mobile drawer with focus inside, and closes it on navigation and Escape', async () => {
    const user = userEvent.setup();
    const onSelect = vi.fn();
    render(
      rail({
        sections: [
          {
            key: 'main',
            items: [
              { key: 'overview', label: 'Overview', icon: <Icon />, href: '#overview', onSelect },
            ],
          },
        ],
      }),
    );
    const open = screen.getByRole('button', { name: 'Open navigation' });
    await user.click(open);
    const drawer = await screen.findByRole('dialog', { name: 'Acme Calendar home' });
    expect(drawer).toContainElement(document.activeElement as HTMLElement);
    const nav = within(drawer).getByRole('navigation', { name: 'Main' });
    await user.click(within(nav).getByRole('link', { name: 'Overview' }));
    expect(onSelect).toHaveBeenCalled();
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());

    await user.click(open);
    await screen.findByRole('dialog');
    await expectNoA11yViolations();
    await user.keyboard('{Escape}');
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    expect(open).toHaveFocus();
  });

  it('uses the app link component for internal links only', () => {
    const AppLink = forwardRef<HTMLAnchorElement, AnchorHTMLAttributes<HTMLAnchorElement>>(
      function AppLink({ children, ...props }, ref) {
        return (
          <a ref={ref} data-app-link="" {...props}>
            {children}
          </a>
        );
      },
    );
    render(rail({ linkComponent: AppLink }));
    const aside = panel();
    expect(within(aside).getByRole('link', { name: 'Overview' })).toHaveAttribute('data-app-link');
    expect(within(aside).getByRole('link', { name: /Settings/ })).not.toHaveAttribute(
      'data-app-link',
    );
  });

  it('notifications and translated messages', () => {
    render(
      rail({
        notifications: { label: 'Notifications', href: '/w/acme/notifications', count: 120 },
        messages: { nav: 'Principal', expand: 'Déplier' },
      }),
    );
    expect(screen.getByRole('navigation', { name: 'Principal' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Déplier' })).toBeInTheDocument();
    const bell = screen.getByRole('link', { name: 'Notifications , 120 unread' });
    expect(bell).toHaveTextContent('99+');
  });

  it('renders on the server without a state when it will follow the cookie', () => {
    const root = (html: string) => /^<div class="mn-rook-rail"[^>]*>/.exec(html)?.[0] ?? '';
    expect(root(renderToString(rail()))).not.toContain('data-state');
    expect(root(renderToString(rail({ persist: false })))).toContain('data-state="collapsed"');
    expect(root(renderToString(rail({ expanded: true })))).toContain('data-state="expanded"');
  });

  it('rookRailScript marks <html> before paint, and validates the cookie name', () => {
    document.cookie = 'rook_rail=expanded; path=/';
    new Function(rookRailScript())();
    expect(document.documentElement).toHaveAttribute('data-rook-rail', 'expanded');
    document.documentElement.removeAttribute('data-rook-rail');
    document.cookie = 'rook_rail=; max-age=0; path=/';
    expect(() => rookRailScript({ cookieName: 'x;y' })).toThrow('Invalid cookie name');
  });
});
