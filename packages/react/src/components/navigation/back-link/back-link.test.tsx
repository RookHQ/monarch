import { forwardRef, type AnchorHTMLAttributes } from 'react';
import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { BackLink } from './index.js';

/** Stands in for a router link (Next.js Link, React Router Link): renders an <a>. */
const RouterLink = forwardRef<
  HTMLAnchorElement,
  AnchorHTMLAttributes<HTMLAnchorElement> & { to: string }
>(function RouterLink({ to, children, ...rest }, ref) {
  return (
    <a ref={ref} href={to} data-router="" {...rest}>
      {children}
    </a>
  );
});

describe('BackLink', () => {
  it('is a link named by its text, with a decorative arrow', () => {
    render(<BackLink href="/domains">Back to domains</BackLink>);
    const link = screen.getByRole('link', { name: 'Back to domains' });
    expect(link).toHaveAttribute('href', '/domains');
    expect(link).toHaveClass('mn-back-link');
    expect(link).toHaveAttribute('data-variant', 'subtle');
    expect(link.querySelector('svg')).toHaveAttribute('aria-hidden', 'true');
  });

  it('renders a router link with asChild, keeping the arrow', () => {
    render(
      <BackLink asChild variant="standalone">
        <RouterLink to="/invoices">All invoices</RouterLink>
      </BackLink>,
    );
    const link = screen.getByRole('link', { name: 'All invoices' });
    expect(link).toHaveAttribute('href', '/invoices');
    expect(link).toHaveAttribute('data-router');
    expect(link).toHaveAttribute('data-variant', 'standalone');
    expect(link).toHaveClass('mn-back-link');
    expect(link.querySelector('.mn-back-link__icon svg')).not.toBeNull();
  });

  it('is reachable with Tab and activates with Enter', async () => {
    const onClick = vi.fn((e: { preventDefault: () => void }) => e.preventDefault());
    render(
      <BackLink href="/domains" onClick={onClick}>
        Back to domains
      </BackLink>,
    );
    const user = userEvent.setup();
    await user.tab();
    expect(screen.getByRole('link')).toHaveFocus();
    await user.keyboard('{Enter}');
    expect(onClick).toHaveBeenCalledTimes(1);
  });

  it('forwards the ref', () => {
    let node: HTMLAnchorElement | null = null;
    render(
      <BackLink
        ref={(n) => {
          node = n;
        }}
        href="/"
      >
        Home
      </BackLink>,
    );
    expect(node).toBe(screen.getByRole('link'));
  });

  it('has no axe violations', async () => {
    render(
      <main>
        <BackLink href="/domains">Back to domains</BackLink>
        <BackLink asChild>
          <RouterLink to="/x">All campaigns</RouterLink>
        </BackLink>
      </main>,
    );
    await expectNoA11yViolations();
  });

  it('renders on the server', () => {
    const html = renderToString(<BackLink href="/domains">Back to domains</BackLink>);
    expect(html).toContain('mn-back-link');
    expect(html).toContain('href="/domains"');
  });
});
