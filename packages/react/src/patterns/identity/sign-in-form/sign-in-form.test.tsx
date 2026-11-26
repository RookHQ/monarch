import { describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { SignInForm } from './index.js';

/** What a browser does when the focused field is disabled (jsdom does not): focus falls to <body>. */
function dropFocus() {
  const probe = document.createElement('button');
  document.body.append(probe);
  probe.focus();
  probe.remove();
  expect(document.body).toHaveFocus();
}

describe('SignInForm', () => {
  it('renders labelled fields, links and autocomplete hints', async () => {
    render(
      <SignInForm
        aria-label="Sign in"
        onSubmit={() => undefined}
        forgotPasswordHref="/reset"
        signUpHref="/sign-up"
        rememberMe
      />,
    );
    const form = screen.getByRole('form', { name: 'Sign in' });
    expect(form).toHaveClass('mn-sign-in-form');
    expect(screen.getByRole('textbox', { name: /Email/ })).toHaveAttribute(
      'autocomplete',
      'username',
    );
    expect(screen.getByLabelText(/^Password/)).toHaveAttribute('autocomplete', 'current-password');
    // Stable names for password managers and native form posts.
    expect(screen.getByRole('textbox', { name: /Email/ })).toHaveAttribute('name', 'email');
    expect(screen.getByLabelText(/^Password/)).toHaveAttribute('name', 'password');
    expect(screen.getByRole('link', { name: 'Forgot password?' })).toHaveAttribute(
      'href',
      '/reset',
    );
    expect(screen.getByRole('link', { name: 'Create an account' })).toHaveAttribute(
      'href',
      '/sign-up',
    );
    expect(screen.getByRole('checkbox', { name: 'Keep me signed in' })).not.toBeChecked();
    await expectNoA11yViolations();
  });

  it('validates on submit and focuses the first invalid field', async () => {
    const user = userEvent.setup();
    const onSubmit = vi.fn();
    render(<SignInForm onSubmit={onSubmit} />);
    await user.click(screen.getByRole('button', { name: 'Sign in' }));
    expect(onSubmit).not.toHaveBeenCalled();
    const email = screen.getByRole('textbox', { name: /Email/ });
    expect(email).toHaveFocus();
    expect(email).toHaveAccessibleDescription('Enter your email.');
    await user.type(email, 'not-an-email{Enter}');
    expect(email).toHaveAccessibleDescription('Enter an email address like name@example.com.');
    await user.clear(email);
    await user.type(email, 'sam@example.com{Enter}');
    expect(screen.getByLabelText(/^Password/)).toHaveFocus();
    expect(onSubmit).not.toHaveBeenCalled();
  });

  it('submits with the keyboard and shows a pending state', async () => {
    const user = userEvent.setup();
    let finish: () => void = () => undefined;
    const onSubmit = vi.fn(() => new Promise<void>((resolve) => (finish = resolve)));
    render(<SignInForm onSubmit={onSubmit} rememberMe />);
    await user.type(screen.getByRole('textbox', { name: /Email/ }), ' sam@example.com ');
    await user.type(screen.getByLabelText(/^Password/), 'correct horse');
    await user.click(screen.getByRole('checkbox', { name: 'Keep me signed in' }));
    await user.type(screen.getByLabelText(/^Password/), '{Enter}');
    expect(onSubmit).toHaveBeenCalledWith({
      identifier: 'sam@example.com',
      password: 'correct horse',
      remember: true,
    });
    expect(screen.getByRole('button', { name: 'Sign in' })).toHaveAttribute('aria-busy', 'true');
    finish();
    await waitFor(() =>
      expect(screen.getByRole('button', { name: 'Sign in' })).not.toHaveAttribute('aria-busy'),
    );
  });

  it('shows server errors on the form and fields, and rejections as alerts', async () => {
    const user = userEvent.setup();
    const onSubmit = vi
      .fn()
      .mockResolvedValueOnce({ fieldErrors: { password: 'That password is not right.' } })
      .mockRejectedValueOnce(new Error('Too many attempts'));
    render(<SignInForm onSubmit={onSubmit} identifier="email-or-username" />);
    await user.type(screen.getByRole('textbox', { name: /Email or username/ }), 'sam');
    const password = screen.getByLabelText(/^Password/);
    await user.type(password, 'wrong{Enter}');
    await waitFor(() => expect(password).toHaveFocus());
    expect(password).toHaveAccessibleDescription('That password is not right.');
    expect(password).toHaveValue('');
    await user.type(password, 'again{Enter}');
    expect(await screen.findByRole('alert')).toHaveTextContent('Too many attempts');
  });

  it('returns focus to the field after a failed sign-in, where the browser dropped it', async () => {
    const user = userEvent.setup();
    let fail: (error: Error) => void = () => undefined;
    const onSubmit = vi.fn(() => new Promise<void>((_, reject) => (fail = reject)));
    render(<SignInForm onSubmit={onSubmit} />);
    await user.type(screen.getByRole('textbox', { name: /Email/ }), 'sam@example.com');
    const password = screen.getByLabelText(/^Password/);
    await user.type(password, 'wrong{Enter}');
    expect(password).toBeDisabled();
    dropFocus();
    fail(new Error('Email or password is incorrect'));
    expect(await screen.findByRole('alert')).toHaveTextContent('Email or password is incorrect');
    await waitFor(() => expect(password).toHaveFocus());
  });

  it('signs in with a passkey and treats a cancel as neutral', async () => {
    const user = userEvent.setup();
    const cancel = Object.assign(new Error('cancelled'), { name: 'NotAllowedError' });
    const onPasskey = vi.fn().mockRejectedValueOnce(cancel).mockResolvedValueOnce(undefined);
    render(
      <SignInForm
        onSubmit={() => undefined}
        onPasskey={onPasskey}
        providers={<button type="button">Continue with Example</button>}
      />,
    );
    expect(screen.getByRole('textbox', { name: /Email/ })).toHaveAttribute(
      'autocomplete',
      'username webauthn',
    );
    expect(screen.getByRole('button', { name: 'Continue with Example' })).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Sign in with a passkey' }));
    expect(await screen.findByRole('status')).toHaveTextContent('Passkey sign-in was cancelled.');
    expect(screen.queryByRole('alert')).toBeNull();
    await user.click(screen.getByRole('button', { name: 'Sign in with a passkey' }));
    expect(onPasskey).toHaveBeenCalledTimes(2);
  });

  it('supports passkey-only sign-in, custom links and translated messages', async () => {
    render(
      <SignInForm
        onSubmit={() => undefined}
        password={false}
        identifier="username"
        signUpHref="/join"
        renderLink={({ href, className, children }) => (
          <a href={href} className={className} data-router="">
            {children}
          </a>
        )}
        error="Your session expired. Sign in again."
        messages={{ submit: 'Weiter', usernameLabel: 'Benutzername' }}
      />,
    );
    expect(screen.queryByLabelText(/^Password/)).toBeNull();
    expect(screen.getByRole('textbox', { name: /Benutzername/ })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Weiter' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Create an account' })).toHaveAttribute('data-router');
    expect(screen.getByRole('alert')).toHaveTextContent('Your session expired.');
  });

  it('renders on the server', () => {
    const html = renderToString(<SignInForm onSubmit={() => undefined} />);
    expect(html).toContain('mn-sign-in-form');
    expect(html).toContain('current-password');
  });
});
