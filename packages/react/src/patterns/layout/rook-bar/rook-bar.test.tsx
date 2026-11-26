import { describe, expect, it, vi } from 'vitest';
import { createRef } from 'react';
import { act, render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import {
  RookBar,
  RookBarAppCatalog,
  RookBarFeaturePanel,
  RookBarLinkList,
  RookBarStoriesPanel,
  type RookBarItem,
  type RookBarStory,
} from './index.js';

const categories = [
  {
    id: 'sales',
    name: 'Sales',
    promise: 'Win customers.',
    apps: [
      { id: 'crm', label: 'Pipeline', description: 'Leads and deals.', href: '/pipeline' },
      {
        id: 'desk',
        label: 'Help desk',
        description: 'Support with a portal.',
        href: 'https://desk.example.com',
        external: true,
      },
    ],
  },
  {
    id: 'ops',
    name: 'Operations',
    apps: [{ id: 'mon', label: 'Monitor', description: 'Uptime and alerts.', href: '/monitor' }],
  },
];

function items(stories?: RookBarStory[] | null, onOpen?: () => void): RookBarItem[] {
  return [
    {
      id: 'products',
      label: 'Products',
      panel: (
        <RookBarAppCatalog
          categories={categories}
          aside={
            <RookBarLinkList heading="Platform" links={[{ label: 'Accounts', href: '/id' }]} />
          }
        />
      ),
    },
    {
      id: 'services',
      label: 'Services',
      panel: (
        <RookBarFeaturePanel
          feature={{
            title: 'When you need more than software.',
            action: {
              label: 'Visit services',
              href: 'https://services.example.com',
              external: true,
            },
          }}
          links={[{ label: 'Consulting', description: 'Plans and reviews.', href: '#consulting' }]}
        />
      ),
    },
    { label: 'Pricing', href: '/pricing' },
    {
      id: 'news',
      label: 'News',
      onOpen,
      panel: (
        <RookBarStoriesPanel
          sections={[{ label: 'Blog', description: 'Ideas.', href: '/blog' }]}
          stories={stories}
        />
      ),
    },
  ];
}

/** jsdom has no PointerEvent; React reads `pointerType` from the native event. */
class MousePointerEvent extends MouseEvent {
  pointerType = 'mouse';
}
const hover = (el: Element) =>
  act(() => {
    el.dispatchEvent(new MousePointerEvent('pointerover', { bubbles: true }));
  });

const brand = (
  <a href="/">
    <img src="data:image/gif;base64,R0lGODlhAQABAAAAACw=" alt="Example" />
  </a>
);

describe('RookBar', () => {
  it('is a named navigation with menu buttons, links and account actions', async () => {
    render(
      <RookBar
        brand={brand}
        items={items()}
        account={{ status: 'signed-out', signInHref: '/login', signUpHref: '/signup' }}
      />,
    );
    const nav = screen.getByRole('navigation', { name: 'Rook' });
    expect(within(nav).getByRole('button', { name: 'Products' })).toHaveAttribute(
      'aria-expanded',
      'false',
    );
    expect(within(nav).getByRole('link', { name: 'Pricing' })).toHaveAttribute('href', '/pricing');
    expect(within(nav).getAllByRole('link', { name: 'Sign up' })[0]).toHaveAttribute(
      'href',
      '/signup',
    );
    expect(within(nav).getByRole('button', { name: 'Open menu' })).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('opens a panel with the keyboard, follows the trigger in tab order, and closes with Escape', async () => {
    const user = userEvent.setup();
    render(<RookBar brand={brand} items={items()} />);
    const products = screen.getByRole('button', { name: 'Products' });
    products.focus();
    await user.keyboard('{Enter}');
    expect(products).toHaveAttribute('aria-expanded', 'true');
    const panel = document.getElementById(products.getAttribute('aria-controls')!)!;
    expect(panel).toBeInTheDocument();
    await user.tab();
    expect(within(panel).getByRole('searchbox', { name: 'Find an app' })).toHaveFocus();
    await expectNoA11yViolations();
    await user.keyboard('{Escape}');
    expect(products).toHaveAttribute('aria-expanded', 'false');
    expect(products).toHaveFocus();
  });

  it('catalog: categories filter, search announces results, external apps say so', async () => {
    const user = userEvent.setup();
    render(<RookBar brand={brand} items={items()} />);
    await user.click(screen.getByRole('button', { name: 'Products' }));
    expect(screen.getByText('Every app')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: /Operations/ }));
    expect(screen.getByRole('button', { name: /Operations/ })).toHaveAttribute(
      'aria-pressed',
      'true',
    );
    expect(screen.queryByRole('link', { name: /Pipeline/ })).not.toBeInTheDocument();
    await user.type(screen.getByRole('searchbox'), 'desk');
    expect(screen.getByRole('status')).toHaveTextContent('1 app');
    expect(screen.getByRole('link', { name: /Help desk.*opens in a new tab/ })).toHaveAttribute(
      'target',
      '_blank',
    );
    await user.clear(screen.getByRole('searchbox'));
    await user.type(screen.getByRole('searchbox'), 'zzz');
    expect(screen.getByRole('status')).toHaveTextContent('No app matches “zzz”.');
  });

  it('opens on hover after a delay and switches instantly once open', async () => {
    vi.useFakeTimers();
    try {
      render(<RookBar brand={brand} items={items()} />);
      const products = screen.getByRole('button', { name: 'Products' });
      const services = screen.getByRole('button', { name: 'Services' });
      hover(products);
      expect(products).toHaveAttribute('aria-expanded', 'false');
      act(() => vi.advanceTimersByTime(150));
      expect(products).toHaveAttribute('aria-expanded', 'true');
      hover(services);
      expect(services).toHaveAttribute('aria-expanded', 'true');
      expect(screen.getByText('When you need more than software.')).toBeInTheDocument();
    } finally {
      vi.useRealTimers();
    }
  });

  it('calls onOpen once and shows loading, then stories', async () => {
    const user = userEvent.setup();
    const onOpen = vi.fn();
    const { rerender } = render(<RookBar brand={brand} items={items(null, onOpen)} />);
    await user.click(screen.getByRole('button', { name: 'News' }));
    expect(onOpen).toHaveBeenCalledTimes(1);
    expect(screen.getByRole('status', { name: 'Loading latest posts' })).toBeInTheDocument();
    rerender(
      <RookBar
        brand={brand}
        items={items(
          [
            {
              id: 'p1',
              title: 'A new release',
              href: '/blog/new',
              label: 'Blog',
              meta: 'Mar 2, 2026',
            },
          ],
          onOpen,
        )}
      />,
    );
    expect(screen.getByRole('link', { name: /A new release/ })).toHaveTextContent(
      'Blog · Mar 2, 2026',
    );
    expect(onOpen).toHaveBeenCalledTimes(1);
  });

  it('closes when a link inside a panel is followed and when the path changes', async () => {
    const user = userEvent.setup();
    const { rerender } = render(<RookBar brand={brand} items={items()} currentPath="/" />);
    await user.click(screen.getByRole('button', { name: 'Services' }));
    await user.click(screen.getByRole('link', { name: /Consulting/ }));
    expect(screen.getByRole('button', { name: 'Services' })).toHaveAttribute(
      'aria-expanded',
      'false',
    );
    await user.click(screen.getByRole('button', { name: 'Services' }));
    rerender(<RookBar brand={brand} items={items()} currentPath="/services" />);
    expect(screen.getByRole('button', { name: 'Services' })).toHaveAttribute(
      'aria-expanded',
      'false',
    );
  });

  it('mobile sheet: sections expand, Escape closes and returns focus', async () => {
    const user = userEvent.setup();
    const onSignOut = vi.fn();
    render(
      <RookBar
        brand={brand}
        items={items()}
        account={{ status: 'signed-in', name: 'Ada Lovelace', dashboardHref: '/home', onSignOut }}
      />,
    );
    const toggle = screen.getByRole('button', { name: 'Open menu' });
    await user.click(toggle);
    expect(toggle).toHaveAccessibleName('Close menu');
    const sheet = document.getElementById(toggle.getAttribute('aria-controls')!)!;
    const section = within(sheet).getByRole('button', { name: 'Products' });
    await user.click(section);
    expect(section).toHaveAttribute('aria-expanded', 'true');
    expect(within(sheet).getByText('Sales')).toBeInTheDocument();
    expect(within(sheet).getByRole('link', { name: 'Open dashboard' })).toHaveAttribute(
      'href',
      '/home',
    );
    await expectNoA11yViolations();
    await user.click(within(sheet).getByRole('button', { name: 'Sign out' }));
    expect(onSignOut).toHaveBeenCalled();
    expect(screen.queryByText('Sales')).not.toBeInTheDocument();
    await user.click(toggle);
    await user.keyboard('{Escape}');
    expect(toggle).toHaveAttribute('aria-expanded', 'false');
    expect(toggle).toHaveFocus();
  });

  it('phone sheet is a modal dialog: page inert, focus trapped, focus restored', async () => {
    const user = userEvent.setup();
    render(
      <>
        <RookBar
          brand={brand}
          items={items()}
          account={{ status: 'signed-in', name: 'Ada Lovelace', dashboardHref: '/home' }}
        />
        <main>
          <button type="button">Page action</button>
        </main>
      </>,
    );
    const toggle = screen.getByRole('button', { name: 'Open menu' });
    await user.click(toggle);
    const dialog = screen.getByRole('dialog', { name: 'Menu' });
    expect(dialog).toHaveAttribute('aria-modal', 'true');
    expect(dialog).toContainElement(document.activeElement as HTMLElement);
    expect(document.querySelector('main')).toHaveAttribute('inert');
    expect(toggle.closest('[inert]')).toBeNull();
    // Shift+Tab from the toggle wraps to the last control in the sheet; Tab wraps back.
    toggle.focus();
    await user.tab({ shift: true });
    expect(dialog).toContainElement(document.activeElement as HTMLElement);
    await user.tab();
    expect(toggle).toHaveFocus();
    await user.click(toggle);
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    expect(document.querySelector('main')).not.toHaveAttribute('inert');
    expect(toggle).toHaveFocus();
  });

  it('signed in: an account menu with dashboard and sign out', async () => {
    const user = userEvent.setup();
    const onSignOut = vi.fn();
    render(
      <RookBar
        brand={brand}
        items={[]}
        account={{
          status: 'signed-in',
          name: 'Ada Lovelace',
          dashboardHref: '/home',
          accountHref: 'https://id.example.com',
          onSignOut,
        }}
      />,
    );
    await user.click(screen.getByRole('button', { name: 'Account menu' }));
    const menu = await screen.findByRole('menu');
    expect(within(menu).getByRole('menuitem', { name: /Account settings/ })).toHaveAttribute(
      'target',
      '_blank',
    );
    await user.click(within(menu).getByRole('menuitem', { name: 'Sign out' }));
    expect(onSignOut).toHaveBeenCalled();
    await waitFor(() => expect(screen.queryByRole('menu')).not.toBeInTheDocument());
  });

  it('translates', () => {
    render(
      <RookBar
        brand={brand}
        items={[]}
        account={{ status: 'signed-out', signInHref: '/in' }}
        messages={{ nav: 'Empresa', openMenu: 'Abrir menú', signIn: 'Entrar' }}
      />,
    );
    expect(screen.getByRole('navigation', { name: 'Empresa' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Abrir menú' })).toBeInTheDocument();
    expect(screen.getAllByRole('link', { name: 'Entrar' }).length).toBeGreaterThan(0);
  });

  it('fixed position, stacking level and a forced colour scheme that reaches the account menu', async () => {
    const user = userEvent.setup();
    const { container } = render(
      <RookBar
        brand={brand}
        items={items()}
        position="fixed"
        zIndex={110}
        colorScheme="light"
        style={{ marginTop: 0 }}
        account={{ status: 'signed-in', name: 'Ada Lovelace', onSignOut: () => {} }}
      />,
    );
    const root = container.querySelector('.mn-rook-bar') as HTMLElement;
    expect(root).toHaveAttribute('data-position', 'fixed');
    expect(root).toHaveAttribute('data-mn-theme', 'light');
    expect(root.style.getPropertyValue('--mn-rook-bar-z')).toBe('110');
    expect(root.style.marginTop).toBe('0px');
    await user.click(screen.getByRole('button', { name: 'Account menu' }));
    const menu = await screen.findByRole('menu');
    expect(menu).toHaveAttribute('data-mn-theme', 'light');
    expect(root.contains(menu)).toBe(false);
  });

  it('keeps a data-mn-theme passed by the app when colorScheme is not set', () => {
    const { container } = render(<RookBar brand={brand} items={[]} data-mn-theme="dark" />);
    expect(container.querySelector('.mn-rook-bar')).toHaveAttribute('data-mn-theme', 'dark');
  });

  it('phone sheet uses the short account labels, falling back to the long ones', async () => {
    const user = userEvent.setup();
    const account = {
      status: 'signed-in' as const,
      name: 'Ada Lovelace',
      accountHref: 'https://id.example.com',
      onSignOut: () => {},
    };
    const { unmount } = render(
      <RookBar
        brand={brand}
        items={[]}
        account={account}
        messages={{
          account: 'Account Settings',
          accountShort: 'Account',
          signOut: 'Log out of Rook',
          signOutShort: 'Log out',
        }}
      />,
    );
    await user.click(screen.getByRole('button', { name: 'Open menu' }));
    const sheet = document.getElementById(
      screen.getByRole('button', { name: 'Close menu' }).getAttribute('aria-controls')!,
    )!;
    expect(within(sheet).getByRole('link', { name: /^Account \(opens/ })).toBeInTheDocument();
    expect(within(sheet).getByRole('button', { name: 'Log out' })).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Account menu' }));
    const menu = await screen.findByRole('menu');
    expect(within(menu).getByRole('menuitem', { name: /Account Settings/ })).toBeInTheDocument();
    expect(within(menu).getByRole('menuitem', { name: 'Log out of Rook' })).toBeInTheDocument();
    unmount();
    render(
      <RookBar brand={brand} items={[]} account={account} messages={{ signOut: 'Log out' }} />,
    );
    await user.click(screen.getByRole('button', { name: 'Open menu' }));
    expect(screen.getByRole('button', { name: 'Log out' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /^Account settings/ })).toBeInTheDocument();
  });

  it('marks the signed-out primary action, which stays beside the menu button from 40rem', () => {
    const { container } = render(
      <RookBar
        brand={brand}
        items={[]}
        account={{ status: 'signed-out', signInHref: '/login', signUpHref: '/signup' }}
      />,
    );
    const account = container.querySelector('.mn-rook-bar__account')!;
    expect(account.querySelector('[data-primary]')).toHaveTextContent('Sign up');
    expect(account.parentElement?.querySelector('.mn-rook-bar__toggle')).toBeInTheDocument();
  });

  it('catalog: icon images, aside action, and an mn- class on every rendered element', async () => {
    const user = userEvent.setup();
    const { container } = render(
      <RookBar
        brand={brand}
        items={[
          {
            id: 'products',
            label: 'Products',
            panel: (
              <RookBarAppCatalog
                categories={[
                  {
                    id: 'a',
                    name: 'All',
                    apps: [{ id: 'x', label: 'X', href: '/x', iconSrc: '/x.png' }],
                  },
                ]}
                asideAction={{ label: 'Create your account', href: '/signup' }}
              />
            ),
          },
          {
            id: 'news',
            label: 'News',
            panel: (
              <RookBarStoriesPanel
                sections={[{ label: 'Blog', href: '/blog' }]}
                stories={[{ id: 's', title: 'Post', href: '/p', image: '/p.png' }]}
              />
            ),
          },
        ]}
      />,
    );
    await user.click(screen.getByRole('button', { name: 'Products' }));
    const cta = screen.getByRole('link', { name: 'Create your account' });
    expect(cta).toHaveClass('mn-button', 'mn-rook-bar-catalog__action');
    expect(cta).toHaveAttribute('href', '/signup');
    expect(container.querySelector('.mn-rook-bar-catalog__image')).toHaveAttribute('src', '/x.png');
    const unclassed = () =>
      [...container.querySelectorAll('.mn-rook-bar :is(input, img, button, svg, a)')].filter(
        (el) => ![...el.classList].some((c) => c.startsWith('mn-')),
      );
    expect(unclassed().map((el) => el.outerHTML.slice(0, 60))).toEqual([
      // The brand is the app's own markup.
      expect.stringContaining('<a href="/">'),
      expect.stringContaining('<img'),
    ]);
    await user.keyboard('{Escape}');
    await user.click(screen.getByRole('button', { name: 'News' }));
    expect(container.querySelector('.mn-rook-bar-stories__img')).toHaveAttribute('src', '/p.png');
    expect(unclassed()).toHaveLength(2);
    await expectNoA11yViolations();
  });

  it('renders on the server, with nothing for a loading account', () => {
    const html = renderToString(
      <RookBar brand={brand} items={items()} account={{ status: 'loading' }} scrollProgress />,
    );
    expect(html).toContain('mn-rook-bar');
    expect(html).toContain('Products');
    expect(html).not.toContain('Log in');
  });

  it('panels forward their ref to the root element', () => {
    const refs = [1, 2, 3, 4].map(() => createRef<HTMLDivElement>());
    const links = [{ label: 'Accounts', href: '/id' }];
    render(
      <>
        <RookBarAppCatalog ref={refs[0]} categories={categories} />
        <RookBarLinkList ref={refs[1]} links={links} />
        <RookBarFeaturePanel ref={refs[2]} feature={{ title: 'New' }} links={links} />
        <RookBarStoriesPanel ref={refs[3]} sections={links} stories={[]} />
      </>,
    );
    expect(refs.map((r) => r.current?.className.split(' ')[0])).toEqual([
      'mn-rook-bar-catalog',
      'mn-rook-bar-links',
      'mn-rook-bar-feature',
      'mn-rook-bar-stories',
    ]);
  });
});
