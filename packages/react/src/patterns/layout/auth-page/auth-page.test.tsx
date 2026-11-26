import { createRef } from 'react';
import { describe, expect, it } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { AuthPage } from './index.js';

function Form() {
  return (
    <form aria-label="Sign in form">
      <label htmlFor="email">Email</label>
      <input id="email" type="email" />
      <button type="submit">Continue</button>
    </form>
  );
}

describe('AuthPage', () => {
  it('renders brand, a section named by the title, description, form and footer', async () => {
    render(
      <AuthPage
        data-testid="root"
        brand={<span>Product</span>}
        brandHref="/"
        title="Sign in"
        description="Use your work email."
        footer={<p>No account yet? Create one.</p>}
      >
        <Form />
      </AuthPage>,
    );
    expect(screen.getByTestId('root')).toHaveClass('mn-auth-page');
    expect(screen.getByTestId('root')).toHaveAttribute('data-variant', 'card');
    const card = screen.getByRole('region', { name: 'Sign in' });
    expect(screen.getByRole('heading', { level: 1, name: 'Sign in' })).toBeVisible();
    expect(card).toHaveTextContent('Use your work email.');
    expect(within(card).getByRole('form', { name: 'Sign in form' })).toBeVisible();
    expect(card).toHaveTextContent('No account yet? Create one.');
    expect(screen.getByRole('link', { name: 'Product' })).toHaveAttribute('href', '/');
    await expectNoA11yViolations();
  });

  it('lists safe footer links in a named nav and drops unsafe ones', async () => {
    render(
      <AuthPage
        title="Sign in"
        links={[
          { label: 'Terms', href: '/terms' },
          { label: 'Bad', href: 'javascript:alert(1)' },
          { label: 'Help', href: 'https://example.com/help', external: true },
        ]}
      />,
    );
    const nav = screen.getByRole('navigation', { name: 'Legal and help' });
    const links = within(nav).getAllByRole('link');
    expect(links).toHaveLength(2);
    const help = screen.getByRole('link', { name: 'Help (opens in a new tab)' });
    expect(help).toHaveAttribute('rel', 'noopener noreferrer');
    expect(screen.queryByRole('link', { name: 'Bad' })).toBeNull();
    await expectNoA11yViolations();
  });

  it('keeps a logical tab order: brand, form, links', async () => {
    const user = userEvent.setup();
    render(
      <AuthPage
        brand="Product"
        brandHref="/"
        brandLabel="Product home"
        title="Sign in"
        links={[{ label: 'Privacy', href: '/privacy' }]}
      >
        <Form />
      </AuthPage>,
    );
    await user.tab();
    expect(screen.getByRole('link', { name: 'Product home' })).toHaveFocus();
    await user.tab();
    expect(screen.getByRole('textbox', { name: 'Email' })).toHaveFocus();
    await user.tab();
    expect(screen.getByRole('button', { name: 'Continue' })).toHaveFocus();
    await user.tab();
    expect(screen.getByRole('link', { name: 'Privacy' })).toHaveFocus();
  });

  it('ignores an unsafe brand link and renders the notice and side panel', () => {
    render(
      <AuthPage
        brand="Product"
        brandHref="javascript:alert(1)"
        title="Reset your password"
        headingLevel="h2"
        notice={<div role="alert">Your session expired.</div>}
        aside={<p>Side message</p>}
        asidePosition="end"
        variant="plain"
      />,
    );
    expect(screen.queryByRole('link')).toBeNull();
    expect(screen.getByRole('heading', { level: 2, name: 'Reset your password' })).toBeVisible();
    expect(screen.getByRole('alert')).toHaveTextContent('Your session expired.');
    expect(screen.getByText('Side message')).toBeInTheDocument();
    const root = screen.getByText('Side message').closest('.mn-auth-page');
    expect(root).toHaveAttribute('data-aside-position', 'end');
    expect(root).toHaveAttribute('data-variant', 'plain');
  });

  it('translates labels and forwards the ref', () => {
    const ref = createRef<HTMLDivElement>();
    render(
      <AuthPage
        ref={ref}
        title="Iniciar sesión"
        links={[{ label: 'Ayuda', href: '/ayuda', external: true }]}
        messages={{ linksLabel: 'Enlaces', newTab: '(nueva pestaña)' }}
      />,
    );
    expect(ref.current).toHaveClass('mn-auth-page');
    expect(screen.getByRole('navigation', { name: 'Enlaces' })).toBeVisible();
    expect(screen.getByRole('link', { name: 'Ayuda (nueva pestaña)' })).toBeVisible();
  });

  it('renders on the server', () => {
    const html = renderToString(
      <AuthPage title="Sign in" brand="Product">
        <Form />
      </AuthPage>,
    );
    expect(html).toContain('mn-auth-page');
    expect(html).toContain('Sign in');
  });
});
