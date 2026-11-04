import { describe, expect, it, vi } from 'vitest';
import { createRef, forwardRef, type AnchorHTMLAttributes } from 'react';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { SectionHeader, SectionHeaderLink } from './index.js';

const RouterLink = forwardRef<HTMLAnchorElement, AnchorHTMLAttributes<HTMLAnchorElement>>(
  function RouterLink({ children, ...props }, ref) {
    return (
      <a ref={ref} data-router="" {...props}>
        {children}
      </a>
    );
  },
);

describe('SectionHeader', () => {
  it('renders title, description, meta, actions and a trailing link', async () => {
    const onClick = vi.fn();
    render(
      <SectionHeader
        title="Recent invoices"
        description="Invoices issued in the last 30 days."
        meta="12 invoices"
        actions={
          <button type="button" onClick={onClick}>
            Export
          </button>
        }
        link={<SectionHeaderLink href="/invoices">View all invoices</SectionHeaderLink>}
      />,
    );
    expect(screen.getByRole('heading', { level: 2, name: 'Recent invoices' })).toBeInTheDocument();
    expect(screen.getByText('Invoices issued in the last 30 days.')).toBeInTheDocument();
    expect(screen.getByText('12 invoices')).toHaveClass('mn-section-header__meta');
    const link = screen.getByRole('link', { name: 'View all invoices' });
    expect(link).toHaveAttribute('href', '/invoices');
    expect(link.querySelector('svg')).toHaveAttribute('aria-hidden', 'true');
    const user = userEvent.setup();
    await user.tab();
    expect(screen.getByRole('button', { name: 'Export' })).toHaveFocus();
    await user.keyboard('{Enter}');
    expect(onClick).toHaveBeenCalledOnce();
    await user.tab();
    expect(link).toHaveFocus();
    // Header only: no section landmark.
    expect(screen.queryByRole('region')).toBeNull();
    await expectNoA11yViolations();
  });

  it('with children, renders a section labelled by its heading', async () => {
    render(
      <SectionHeader title="Top pages" headingLevel="h3" headingId="top-pages">
        <p>Content</p>
      </SectionHeader>,
    );
    const region = screen.getByRole('region', { name: 'Top pages' });
    expect(region).toHaveAttribute('aria-labelledby', 'top-pages');
    expect(screen.getByRole('heading', { level: 3 })).toHaveAttribute('id', 'top-pages');
    expect(region).toHaveTextContent('Content');
    await expectNoA11yViolations();
  });

  it('busy: aria-busy, a named spinner and dimmed content; clears when done', async () => {
    const { rerender } = render(
      <SectionHeader title="Traffic" busy busyLabel="Refreshing traffic">
        <p>Chart</p>
      </SectionHeader>,
    );
    const region = screen.getByRole('region', { name: 'Traffic' });
    expect(region).toHaveAttribute('aria-busy', 'true');
    expect(region).toHaveAttribute('data-busy');
    expect(screen.getByRole('status', { name: 'Refreshing traffic' })).toBeInTheDocument();
    await expectNoA11yViolations();
    rerender(
      <SectionHeader title="Traffic">
        <p>Chart</p>
      </SectionHeader>,
    );
    expect(region).not.toHaveAttribute('aria-busy');
    expect(screen.queryByRole('status')).toBeNull();
  });

  it('forwards refs and passes attributes through', () => {
    const ref = createRef<HTMLElement>();
    render(<SectionHeader ref={ref} title="T" data-testid="sh" size="md" className="x" />);
    expect(ref.current).toBe(screen.getByTestId('sh'));
    expect(ref.current).toHaveAttribute('data-size', 'md');
    expect(ref.current).toHaveClass('mn-section-header', 'x');
  });

  it('SectionHeaderLink renders a router link with asChild', () => {
    render(
      <SectionHeaderLink asChild>
        <RouterLink href="/reports">View all reports</RouterLink>
      </SectionHeaderLink>,
    );
    const link = screen.getByRole('link', { name: 'View all reports' });
    expect(link).toHaveAttribute('data-router');
    expect(link).toHaveClass('mn-section-header__link');
  });

  it('renders on the server', () => {
    const html = renderToString(
      <SectionHeader title="Members" meta="8 people" busy>
        <p>List</p>
      </SectionHeader>,
    );
    expect(html).toContain('<section');
    expect(html).toContain('aria-busy="true"');
    expect(html).toContain('mn-section-header__meta');
  });
});
