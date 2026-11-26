import type { ReactNode } from 'react';
import { describe, expect, it, vi } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import {
  HelpCenterHome,
  filterHelpArticles,
  groupHelpArticles,
  type HelpArticle,
} from './index.js';

const articles: HelpArticle[] = [
  {
    id: 'a1',
    title: 'Reset your password',
    category: 'Account',
    href: '#a1',
    updated: 'Mar 2, 2026',
  },
  { id: 'a2', title: 'Change your email address', category: 'Account', href: '#a2' },
  { id: 'a3', title: 'Download an invoice', category: 'Billing', href: '#a3' },
  { id: 'a4', title: 'Café opening hours', category: null, href: '#a4' },
];

describe('HelpCenterHome', () => {
  it('shows a titled page with a search and articles grouped by category', async () => {
    render(<HelpCenterHome articles={articles} />);
    expect(screen.getByRole('heading', { level: 1, name: 'Help center' })).toBeInTheDocument();
    expect(screen.getByRole('search', { name: 'Search articles' })).toBeInTheDocument();
    // Categories are headed sections, not landmarks; each heading names its list.
    expect(screen.queryByRole('region')).toBeNull();
    const account = screen.getByRole('list', { name: 'Account' });
    expect(within(account).getAllByRole('link')).toHaveLength(2);
    expect(screen.getByRole('list', { name: 'General' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Download an invoice' })).toHaveAttribute(
      'href',
      '#a3',
    );
    await expectNoA11yViolations();
  });

  it('filters as you type, ignoring accents, and announces the count', async () => {
    const user = userEvent.setup();
    const onQueryChange = vi.fn();
    render(<HelpCenterHome articles={articles} onQueryChange={onQueryChange} />);
    await user.type(screen.getByRole('searchbox', { name: 'Search articles' }), 'cafe');
    expect(onQueryChange).toHaveBeenLastCalledWith('cafe');
    expect(screen.getByRole('link', { name: 'Café opening hours' })).toBeInTheDocument();
    expect(screen.queryByRole('link', { name: 'Reset your password' })).not.toBeInTheDocument();
    expect(screen.getByRole('status')).toHaveTextContent('1 matching article');
  });

  it('shows no results with the contact action', async () => {
    const user = userEvent.setup();
    render(<HelpCenterHome articles={articles} contact={<a href="#new">Submit a request</a>} />);
    await user.type(screen.getByRole('searchbox'), 'refund policy');
    expect(screen.getByText('No articles match “refund policy”.')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Submit a request' })).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('loading, error, empty and unavailable states', async () => {
    const { rerender, container } = render(<HelpCenterHome articles={null} />);
    expect(container.firstElementChild).toHaveAttribute('aria-busy', 'true');
    expect(screen.getByText('Loading articles')).toBeInTheDocument();
    rerender(<HelpCenterHome articles={[]} error="The help center is unavailable right now." />);
    expect(
      screen.getByText('The help center is unavailable right now.').closest('.mn-alert'),
    ).toHaveAttribute('role', 'status');
    rerender(<HelpCenterHome articles={[]} />);
    expect(screen.getByText('No articles have been published yet.')).toBeInTheDocument();
    rerender(
      <HelpCenterHome articles={null} unavailable contact={<a href="#new">Submit a request</a>} />,
    );
    expect(screen.queryByRole('search')).not.toBeInTheDocument();
    expect(screen.getByText('This site doesn’t have a help center.')).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('renders each layout, with a controlled query and h2 heading', async () => {
    const { rerender } = render(
      <HelpCenterHome articles={articles} layout="rows" query="password" headingAs="h2" />,
    );
    expect(screen.getByRole('heading', { level: 2, name: 'Help center' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { level: 3, name: 'Account' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /Reset your password/ })).toHaveTextContent(
      'Updated Mar 2, 2026',
    );
    rerender(<HelpCenterHome articles={articles} layout="cards" />);
    expect(screen.getByRole('link', { name: /Download an invoice/ })).toHaveTextContent(
      'Read article',
    );
    rerender(<HelpCenterHome articles={articles} layout="dense" />);
    expect(screen.getByRole('link', { name: 'Download an invoice' })).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('uses a router link and translates', () => {
    const Link = ({
      href,
      children,
      className,
    }: {
      href: string;
      children?: ReactNode;
      className?: string;
    }) => (
      <a href={href} className={className} data-router="">
        {children}
      </a>
    );
    render(
      <HelpCenterHome
        articles={articles}
        linkAs={Link}
        messages={{ title: 'Centro de ayuda', search: 'Buscar artículos', general: 'General' }}
      />,
    );
    expect(screen.getByRole('heading', { name: 'Centro de ayuda' })).toBeInTheDocument();
    expect(screen.getByRole('searchbox', { name: 'Buscar artículos' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Download an invoice' })).toHaveAttribute(
      'data-router',
    );
  });

  it('names each list by its own heading when categories are not Latin text', () => {
    render(
      <HelpCenterHome
        articles={[
          { id: 'x1', title: 'إعادة تعيين كلمة المرور', category: 'الحساب', href: '/a/1' },
          { id: 'x2', title: 'تنزيل فاتورة', category: 'الفوترة', href: '/a/2' },
        ]}
      />,
    );
    const ids = screen.getAllByRole('heading', { level: 2 }).map((h) => h.id);
    expect(new Set(ids).size).toBe(2);
    expect(screen.getByRole('list', { name: 'الحساب' })).toBeInTheDocument();
    expect(screen.getByRole('list', { name: 'الفوترة' })).toBeInTheDocument();
  });

  it('never links an unsafe article URL, and keeps the contact action when there are no articles', () => {
    const { rerender } = render(
      <HelpCenterHome
        articles={[
          { id: 'x', title: 'Injected', category: 'Account', href: 'javascript:alert(1)' },
        ]}
      />,
    );
    expect(screen.getByRole('link', { name: 'Injected' })).toHaveAttribute('href', '#');
    rerender(<HelpCenterHome articles={[]} contact={<a href="/contact">Contact support</a>} />);
    expect(screen.getByRole('link', { name: 'Contact support' })).toBeInTheDocument();
  });

  it('helpers filter on every word and group in order', () => {
    expect(filterHelpArticles(articles, 'your account').map((a) => a.id)).toEqual(['a1', 'a2']);
    expect(groupHelpArticles(articles).map(([c]) => c)).toEqual(['Account', 'Billing', 'General']);
  });

  it('renders on the server', () => {
    const html = renderToString(<HelpCenterHome articles={articles} defaultQuery="invoice" />);
    expect(html).toContain('mn-help-center');
    expect(html).toContain('Download an invoice');
    expect(html).not.toContain('Reset your password');
  });
});
