import { describe, expect, it } from 'vitest';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { SiteFooter, SiteHeader } from './index.js';

const nav = [
  {
    label: 'Product',
    items: [
      { label: 'Editor', href: '/editor', description: 'Pages and blocks' },
      { label: 'Search', href: '/search' },
    ],
  },
  { label: 'Pricing', href: '/pricing', current: true },
];

function Shell() {
  return (
    <>
      <SiteHeader
        brand={<a href="/">Rook Example</a>}
        nav={nav}
        actions={<button type="button">Start free</button>}
      />
      <main>Content</main>
      <SiteFooter
        columns={[{ title: 'Company', links: [{ label: 'About', href: '/about' }] }]}
        legal="© 2026 Rook"
        legalLinks={[{ label: 'Status', href: 'https://status.example', external: true }]}
      />
    </>
  );
}

describe('SiteHeader', () => {
  it('opens a group as a disclosure panel of links and marks the current page', async () => {
    render(<Shell />);
    const user = userEvent.setup();
    const trigger = screen.getByRole('button', { name: 'Product' });
    expect(trigger).toHaveAttribute('aria-expanded', 'false');
    await user.click(trigger);
    expect(trigger).toHaveAttribute('aria-expanded', 'true');
    const panel = await screen.findByRole('dialog', { name: 'Product' });
    expect(within(panel).getByRole('link', { name: /Editor/ })).toHaveAttribute('href', '/editor');
    expect(screen.getAllByRole('link', { name: 'Pricing' })[0]).toHaveAttribute(
      'aria-current',
      'page',
    );
  });

  it('mobile menu toggles with aria-expanded and closes on Escape, restoring focus', async () => {
    render(<Shell />);
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Open menu' }));
    const close = screen.getByRole('button', { name: 'Close menu' });
    expect(close).toHaveAttribute('aria-expanded', 'true');
    const id = close.getAttribute('aria-controls') ?? '';
    const panel = document.getElementById(id);
    expect(panel).not.toBeNull();
    expect(within(panel as HTMLElement).getByRole('link', { name: 'Search' })).toBeInTheDocument();
    await user.keyboard('{Escape}');
    await waitFor(() => expect(screen.getByRole('button', { name: 'Open menu' })).toHaveFocus());
    expect(document.getElementById(id)).toBeNull();
  });
});

describe('SiteFooter', () => {
  it('uses real column headings and announces external links', () => {
    render(<Shell />);
    expect(screen.getByRole('heading', { level: 2, name: 'Company' })).toBeInTheDocument();
    const ext = screen.getByRole('link', { name: 'Status (opens in a new tab)' });
    expect(ext).toHaveAttribute('target', '_blank');
    expect(ext).toHaveAttribute('rel', 'noopener noreferrer');
  });
});

describe('site shell', () => {
  it('exposes banner, navigation and contentinfo landmarks with no axe violations', async () => {
    render(<Shell />);
    expect(screen.getByRole('banner')).toBeInTheDocument();
    expect(screen.getByRole('navigation', { name: 'Main' })).toBeInTheDocument();
    expect(screen.getByRole('navigation', { name: 'Footer' })).toBeInTheDocument();
    expect(screen.getByRole('contentinfo')).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('renders on the server', () => {
    const html = renderToString(<Shell />);
    expect(html).toContain('mn-site-header');
    expect(html).toContain('mn-site-footer');
  });
});
