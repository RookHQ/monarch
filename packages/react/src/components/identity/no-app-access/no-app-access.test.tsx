import { forwardRef, type AnchorHTMLAttributes } from 'react';
import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { NoAppAccess } from './index.js';

describe('NoAppAccess', () => {
  it('explains the missing grant with a heading and a decorative lock', async () => {
    const { container } = render(<NoAppAccess appName="Rook Tickets" workspaceName="Acme" />);
    expect(
      screen.getByRole('heading', { level: 1, name: 'You don’t have access to Rook Tickets' }),
    ).toBeInTheDocument();
    expect(
      screen.getByText('Ask an admin of Acme to give you access to Rook Tickets.'),
    ).toBeInTheDocument();
    const root = container.firstElementChild as HTMLElement;
    expect(root.tagName).toBe('SECTION');
    expect(root).toHaveClass('mn-no-app-access', 'mn-error-page');
    expect(root).toHaveAttribute('data-kind', 'forbidden');
    expect(root.querySelector('svg')?.closest('[aria-hidden="true"]')).not.toBeNull();
    expect(screen.queryByRole('button')).not.toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('requests access: pending, then sent (announced)', async () => {
    const user = userEvent.setup();
    let resolve: () => void = () => {};
    const onRequestAccess = vi.fn(() => new Promise<void>((r) => (resolve = r)));
    render(<NoAppAccess appName="Rook Tickets" onRequestAccess={onRequestAccess} />);
    const button = screen.getByRole('button', { name: 'Request access' });
    await user.click(button);
    expect(button).toHaveAttribute('aria-busy', 'true');
    await user.click(button);
    expect(onRequestAccess).toHaveBeenCalledTimes(1);
    resolve();
    expect(await screen.findByText(/Request sent/)).toHaveAttribute('role', 'status');
    expect(screen.getByRole('button', { name: 'Access requested' })).toBeDisabled();
    await expectNoA11yViolations();
  });

  it('shows a failed request as an alert and allows another try', async () => {
    const user = userEvent.setup();
    const onRequestAccess = vi
      .fn()
      .mockRejectedValueOnce(new Error('nope'))
      .mockResolvedValueOnce(undefined);
    render(<NoAppAccess appName="Rook Tickets" onRequestAccess={onRequestAccess} />);
    await user.click(screen.getByRole('button', { name: 'Request access' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('didn’t go through');
    await user.click(screen.getByRole('button', { name: 'Request access' }));
    expect(await screen.findByRole('button', { name: 'Access requested' })).toBeInTheDocument();
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });

  it('treats a synchronous request as sent, and is controllable', () => {
    const { rerender } = render(
      <NoAppAccess appName="Rook Links" onRequestAccess={() => {}} requestState="sent" />,
    );
    expect(screen.getByRole('button', { name: 'Access requested' })).toBeDisabled();
    rerender(<NoAppAccess appName="Rook Links" onRequestAccess={() => {}} requestState="idle" />);
    expect(screen.getByRole('button', { name: 'Request access' })).toBeEnabled();
  });

  it('offers back to workspace, switch workspace and contact an admin', async () => {
    const user = userEvent.setup();
    const onSwitchWorkspace = vi.fn();
    render(
      <NoAppAccess
        appName="Rook Friends"
        homeHref="/w/acme/home"
        onSwitchWorkspace={onSwitchWorkspace}
        contactHref="mailto:admin@example.com"
      />,
    );
    expect(screen.getByRole('link', { name: 'Back to workspace' })).toHaveAttribute(
      'href',
      '/w/acme/home',
    );
    expect(screen.getByRole('link', { name: 'Contact an admin' })).toHaveAttribute(
      'href',
      'mailto:admin@example.com',
    );
    await user.click(screen.getByRole('button', { name: 'Switch workspace' }));
    expect(onSwitchWorkspace).toHaveBeenCalled();
  });

  it('takes a switch link, a custom switcher slot and a router link', () => {
    const Router = forwardRef<HTMLAnchorElement, AnchorHTMLAttributes<HTMLAnchorElement>>(
      function Router(props, ref) {
        // eslint-disable-next-line jsx-a11y/anchor-has-content -- children arrive in props
        return <a ref={ref} data-router="" {...props} />;
      },
    );
    const { rerender } = render(
      <NoAppAccess appName="Rook Chat" switchWorkspaceHref="/workspaces" linkAs={Router} />,
    );
    expect(screen.getByRole('link', { name: 'Switch workspace' })).toHaveAttribute('data-router');
    rerender(
      <NoAppAccess
        appName="Rook Chat"
        switchWorkspaceHref="/workspaces"
        workspaceSwitcher={<button type="button">Workspace: Acme</button>}
      />,
    );
    expect(screen.queryByRole('link', { name: 'Switch workspace' })).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Workspace: Acme' })).toBeInTheDocument();
  });

  it('focuses the heading on mount and takes an h2, custom text and full screen', () => {
    render(
      <NoAppAccess
        appName="Rook Base"
        headingLevel="h2"
        focusOnMount
        fullScreen
        title="This community is private"
        description="Ask its owner for an invite."
      />,
    );
    const heading = screen.getByRole('heading', { level: 2, name: 'This community is private' });
    expect(heading).toHaveFocus();
    expect(screen.getByRole('main')).toHaveAttribute('data-full-screen');
  });

  it('translates', () => {
    render(
      <NoAppAccess
        appName="Rook Tickets"
        messages={{ title: (a) => `Pas d’accès à ${a}` }}
        onRequestAccess={() => {}}
      />,
    );
    expect(screen.getByRole('heading', { name: 'Pas d’accès à Rook Tickets' })).toBeInTheDocument();
  });

  it('renders on the server', () => {
    const html = renderToString(<NoAppAccess appName="Rook Tickets" homeHref="/" />);
    expect(html).toContain('mn-no-app-access');
    expect(html).toContain('Back to workspace');
  });
});
