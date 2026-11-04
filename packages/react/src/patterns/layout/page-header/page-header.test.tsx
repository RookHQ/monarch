import { describe, expect, it, vi } from 'vitest';
import { createRef } from 'react';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { Button } from '../../../components/actions/button/index.js';
import { Badge } from '../../../components/data-display/badge/index.js';
import { Breadcrumb } from '../../../components/navigation/breadcrumb/index.js';
import { PageHeader, type PageHeaderAction } from './index.js';

const actions = (spy = vi.fn()): PageHeaderAction[] => [
  { id: 'share', label: 'Share', onSelect: () => spy('share') },
  { id: 'export', label: 'Export', href: '/export.csv' },
  { id: 'duplicate', label: 'Duplicate', onSelect: () => spy('duplicate') },
  { id: 'archive', label: 'Archive', tone: 'danger', onSelect: () => spy('archive') },
];

describe('PageHeader', () => {
  it('renders the single h1 with actions', async () => {
    render(
      <main>
        <PageHeader
          title="Tickets"
          description="Everything assigned to you."
          actions={<Button variant="primary">New ticket</Button>}
        />
      </main>,
    );
    expect(screen.getByRole('heading', { level: 1, name: 'Tickets' })).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('renders back link, breadcrumbs, status, details and banner in order', async () => {
    const { container } = render(
      <main>
        <PageHeader
          title="example.com"
          backHref="/domains"
          backLabel="All domains"
          breadcrumbs={
            <Breadcrumb
              items={[{ label: 'Domains', href: '/domains' }, { label: 'example.com' }]}
            />
          }
          status={<Badge tone="success">Published</Badge>}
          meta={<Badge>Primary</Badge>}
          details={<span>Updated 5 minutes ago</span>}
          description="DNS and certificate status."
          banner={<p role="status">This domain is being verified.</p>}
          primaryAction={<Button variant="primary">Publish</Button>}
        />
      </main>,
    );
    const back = screen.getByRole('link', { name: 'All domains' });
    expect(back).toHaveAttribute('href', '/domains');
    expect(screen.getByRole('navigation', { name: /breadcrumb/i })).toBeInTheDocument();
    expect(screen.getByText('Published').closest('.mn-page-header__status')).not.toBeNull();
    expect(
      screen.getByText('Updated 5 minutes ago').closest('.mn-page-header__details'),
    ).not.toBeNull();
    const order = [...container.querySelectorAll('header > *')].map((n) => n.className);
    expect(order).toEqual([
      'mn-page-header__banner',
      'mn-page-header__back',
      'mn-page-header__breadcrumbs',
      'mn-page-header__row',
    ]);
    expect(screen.getByRole('button', { name: 'Publish' })).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('a custom `back` node wins over backHref', () => {
    render(<PageHeader title="Invoice" back={<a href="/billing">Billing</a>} backHref="/x" />);
    expect(screen.getByRole('link', { name: 'Billing' })).toBeInTheDocument();
    expect(screen.queryByRole('link', { name: 'Back' })).toBeNull();
  });

  it('shows the first secondary actions as buttons and the rest in the More menu', async () => {
    const spy = vi.fn();
    render(<PageHeader title="Project" secondaryActions={actions(spy)} maxVisibleActions={2} />);
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Share' }));
    expect(spy).toHaveBeenCalledWith('share');
    expect(screen.getByRole('link', { name: 'Export' })).toHaveAttribute('href', '/export.csv');
    expect(screen.queryByRole('button', { name: 'Duplicate' })).toBeNull();

    const more = screen.getByRole('button', { name: 'More actions' });
    expect(more).toHaveAttribute('data-overflow');
    more.focus();
    await user.keyboard('{Enter}');
    const menu = await screen.findByRole('menu');
    // Every action is in the menu; the ones shown as buttons are marked so CSS
    // hides them on wide screens and shows them below 640px.
    const items = within(menu).getAllByRole('menuitem');
    expect(items.map((i) => i.textContent)).toEqual(['Share', 'Export', 'Duplicate', 'Archive']);
    expect(items[0]).toHaveAttribute('data-wide-inline');
    expect(items[2]).not.toHaveAttribute('data-wide-inline');
    expect(items[3]).toHaveAttribute('data-tone', 'danger');
    await waitFor(() => expect(items[0]).toHaveFocus());
    await user.keyboard('{ArrowDown}{ArrowDown}{Enter}');
    expect(spy).toHaveBeenLastCalledWith('duplicate');
    await waitFor(() => expect(screen.queryByRole('menu')).toBeNull());
    expect(more).toHaveFocus();
  });

  it('keeps the More button for narrow screens when every action fits', () => {
    render(
      <PageHeader
        title="Project"
        secondaryActions={actions().slice(0, 2)}
        moreActionsLabel="More actions for Project"
      />,
    );
    const more = screen.getByRole('button', { name: 'More actions for Project' });
    expect(more).not.toHaveAttribute('data-overflow');
  });

  it('disabled actions stay buttons and are disabled in the menu', async () => {
    render(
      <PageHeader
        title="Project"
        maxVisibleActions={0}
        secondaryActions={[{ id: 'a', label: 'Archive', href: '/a', disabled: true }]}
      />,
    );
    expect(screen.queryByRole('link', { name: 'Archive' })).toBeNull();
    const user = userEvent.setup();
    screen.getByRole('button', { name: 'More actions' }).focus();
    await user.keyboard('{Enter}');
    const item = await screen.findByRole('menuitem', { name: 'Archive' });
    expect(item).toHaveAttribute('aria-disabled', 'true');
  });

  it('embedded mode keeps the heading but hides it visually; headingLevel changes the level', () => {
    const { container } = render(<PageHeader title="Billing" embedded headingLevel="h2" />);
    const heading = screen.getByRole('heading', { level: 2, name: 'Billing' });
    expect(heading).toHaveClass('mn-visually-hidden');
    expect(container.firstChild).toHaveAttribute('data-embedded');
  });

  it('exposes sticky mode and forwards the ref and native attributes', () => {
    const ref = createRef<HTMLElement>();
    render(<PageHeader ref={ref} title="Inbox" sticky data-testid="ph" id="top" />);
    const root = screen.getByTestId('ph');
    expect(ref.current).toBe(root);
    expect(root).toHaveAttribute('data-sticky');
    expect(root).toHaveAttribute('id', 'top');
    expect(root).toHaveClass('mn-page-header');
  });

  it('renders on the server', () => {
    const html = renderToString(
      <PageHeader
        title="Deals"
        eyebrow={<Breadcrumb items={[{ label: 'Home', href: '/' }, { label: 'Deals' }]} />}
        secondaryActions={actions()}
        sticky
      />,
    );
    expect(html).toContain('mn-page-header');
    expect(html).toContain('More actions');
  });
});
