import { describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { OAuthConsentScreen, type OAuthConsentApp, type OAuthConsentScope } from './index.js';

const APP: OAuthConsentApp = {
  name: 'Example Notes',
  domain: 'notes.example.com',
  redirectHost: 'app.example.net',
  verified: true,
  privacyUrl: 'https://notes.example.com/privacy',
  termsUrl: 'javascript:alert(1)',
};
const ACCOUNT = { name: 'Sam Rivera', email: 'sam@example.com' };
const SCOPES: OAuthConsentScope[] = [
  { id: 'profile', label: 'See your name and profile photo' },
  {
    id: 'files.write',
    label: 'Create and change your files',
    description: 'Only files in the workspace you choose.',
    sensitive: true,
  },
];

describe('OAuthConsentScreen', () => {
  it('shows the app, account, scopes and safe links', async () => {
    render(
      <OAuthConsentScreen
        app={APP}
        account={ACCOUNT}
        scopes={SCOPES}
        onAllow={() => undefined}
        onDeny={() => undefined}
        onSwitchAccount={() => undefined}
        footnote="You can remove access at any time."
      />,
    );
    const region = screen.getByRole('region', {
      name: 'Example Notes wants to access your account',
    });
    expect(region).toHaveClass('mn-oauth-consent-screen');
    expect(screen.getByRole('heading', { level: 1 })).toHaveTextContent('Example Notes');
    expect(region).toHaveTextContent('notes.example.com');
    expect(region).toHaveTextContent('You’ll go back to app.example.net');
    expect(region).toHaveTextContent('Verified');
    expect(region).toHaveTextContent('Signed in as Sam Rivera');
    const items = within(screen.getByRole('list')).getAllByRole('listitem');
    expect(items[1]).toHaveAttribute('data-sensitive', 'true');
    expect(items[1]).toHaveTextContent('Create and change your files (sensitive)');
    expect(screen.getByRole('link', { name: /privacy policy/ })).toHaveAttribute(
      'href',
      'https://notes.example.com/privacy',
    );
    expect(screen.queryByRole('link', { name: /terms/ })).toBeNull();
    await expectNoA11yViolations();
  });

  it('allows with the keyboard, disabling both buttons while pending', async () => {
    const user = userEvent.setup();
    let finish: () => void = () => undefined;
    const onAllow = vi.fn(() => new Promise<void>((resolve) => (finish = resolve)));
    const onDeny = vi.fn();
    render(
      <OAuthConsentScreen
        app={APP}
        account={ACCOUNT}
        scopes={SCOPES}
        onAllow={onAllow}
        onDeny={onDeny}
      />,
    );
    const allow = screen.getByRole('button', { name: 'Allow' });
    const deny = screen.getByRole('button', { name: 'Cancel' });
    allow.focus();
    await user.keyboard('{Enter}');
    expect(onAllow).toHaveBeenCalledOnce();
    expect(allow).toHaveAttribute('aria-busy', 'true');
    expect(deny).toBeDisabled();
    await user.click(deny);
    expect(onDeny).not.toHaveBeenCalled();
    finish();
    await waitFor(() => expect(allow).not.toHaveAttribute('aria-busy'));
  });

  it('denies and shows a rejection', async () => {
    const user = userEvent.setup();
    render(
      <OAuthConsentScreen
        app={APP}
        account={ACCOUNT}
        scopes={SCOPES}
        onAllow={() => undefined}
        onDeny={() => Promise.reject(new Error('Request expired'))}
      />,
    );
    await user.click(screen.getByRole('button', { name: 'Cancel' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('Request expired');
  });

  it('warns about unverified apps, holds back scopes and can block', async () => {
    const user = userEvent.setup();
    const onSwitchAccount = vi.fn();
    const { rerender } = render(
      <OAuthConsentScreen
        app={{ name: 'Side Project', verified: false, testing: true, logoSrc: 'javascript:x' }}
        account={{ email: 'sam@example.com' }}
        scopes={SCOPES.slice(0, 1)}
        heldBackScopes={SCOPES.slice(1)}
        onAllow={() => undefined}
        onDeny={() => undefined}
        onSwitchAccount={onSwitchAccount}
        allowDisabled
      >
        <p>Choose a workspace</p>
      </OAuthConsentScreen>,
    );
    expect(screen.getByText('This app hasn’t been verified')).toBeInTheDocument();
    expect(screen.getByText(/still being tested/)).toBeInTheDocument();
    expect(screen.getByText('Not shared until the app is verified:')).toBeInTheDocument();
    expect(screen.getByText('Choose a workspace')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Allow' })).toBeDisabled();
    expect(document.querySelector('img')).toBeNull();
    await user.click(screen.getByRole('button', { name: 'Use another account' }));
    expect(onSwitchAccount).toHaveBeenCalled();
    rerender(
      <OAuthConsentScreen
        app={APP}
        account={ACCOUNT}
        scopes={SCOPES}
        onAllow={() => undefined}
        onDeny={() => undefined}
        blocked="Your workspace doesn’t allow this app."
        messages={{ deny: 'Zurück' }}
      />,
    );
    expect(screen.queryByRole('button', { name: 'Allow' })).toBeNull();
    expect(screen.queryByRole('list')).toBeNull();
    expect(screen.getByText('Your workspace doesn’t allow this app.')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Zurück' })).toBeInTheDocument();
  });

  it('renders on the server', () => {
    const html = renderToString(
      <OAuthConsentScreen
        app={APP}
        account={ACCOUNT}
        scopes={SCOPES}
        onAllow={() => undefined}
        onDeny={() => undefined}
      />,
    );
    expect(html).toContain('mn-oauth-consent-screen');
    expect(html).not.toContain('javascript:');
  });
});
