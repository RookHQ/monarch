import { describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { PasswordResetForm } from './index.js';

const STRONG = 'Correct-Horse-9';
const pw = (name: RegExp) => screen.getByLabelText(name, { selector: 'input' });

/** What a browser does when the focused field is disabled (jsdom does not): focus falls to <body>. */
function dropFocus() {
  const probe = document.createElement('button');
  document.body.append(probe);
  probe.focus();
  probe.remove();
  expect(document.body).toHaveFocus();
}

describe('PasswordResetForm', () => {
  it('requests a link, validates the email and moves focus to the next step', async () => {
    const user = userEvent.setup();
    const onRequest = vi.fn(() => Promise.resolve());
    const onStepChange = vi.fn();
    render(
      <PasswordResetForm
        data-testid="root"
        onRequest={onRequest}
        onStepChange={onStepChange}
        signInHref="/sign-in"
      />,
    );
    expect(screen.getByTestId('root')).toHaveClass('mn-password-reset-form');
    expect(screen.getByRole('form', { name: 'Reset your password' })).toBeInTheDocument();
    await expectNoA11yViolations();
    const email = screen.getByRole('textbox', { name: /Email/ });
    await user.click(screen.getByRole('button', { name: 'Send reset link' }));
    expect(email).toHaveFocus();
    expect(email).toHaveAccessibleDescription('Enter your email address.');
    await user.type(email, 'sam@example.com{Enter}');
    expect(onRequest).toHaveBeenCalledWith('sam@example.com');
    const heading = await screen.findByRole('heading', { name: 'Check your email' });
    await waitFor(() => expect(heading).toHaveFocus());
    expect(screen.getByTestId('root')).toHaveAttribute('data-step', 'sent');
    expect(screen.getByTestId('root')).toHaveTextContent('sam@example.com');
    expect(onStepChange).toHaveBeenCalledWith('sent');
  });

  it('resends with a cooldown and goes back to change the email', async () => {
    const user = userEvent.setup();
    const onRequest = vi.fn(() => Promise.resolve());
    render(
      <PasswordResetForm
        defaultStep="sent"
        defaultEmail="sam@example.com"
        onRequest={onRequest}
        cooldownSeconds={30}
      />,
    );
    const resend = screen.getByRole('button', { name: 'Resend link' });
    await user.click(resend);
    expect(onRequest).toHaveBeenCalledWith('sam@example.com');
    await waitFor(() => expect(resend).toHaveTextContent('Resend in 30s'));
    expect(resend).toHaveAttribute('aria-disabled', 'true');
    expect(screen.getByRole('status')).toHaveTextContent('We sent another link.');
    await user.click(resend);
    expect(onRequest).toHaveBeenCalledTimes(1);
    await user.click(screen.getByRole('button', { name: 'Use a different email' }));
    expect(screen.getByRole('textbox', { name: /Email/ })).toHaveValue('sam@example.com');
  });

  it('sets a new password with strength rules and confirmation', async () => {
    const user = userEvent.setup();
    const onReset = vi.fn(() => Promise.resolve());
    render(<PasswordResetForm step="reset" onReset={onReset} signInHref="/sign-in" />);
    expect(pw(/^New password/)).toHaveAttribute('autocomplete', 'new-password');
    await expectNoA11yViolations();
    await user.type(pw(/^New password/), 'short{Enter}');
    expect(pw(/^New password/)).toHaveFocus();
    expect(screen.getByText('Choose a password that meets every requirement.')).toBeInTheDocument();
    await user.clear(pw(/^New password/));
    await user.type(pw(/^New password/), STRONG);
    await user.type(pw(/^Confirm new password/), 'different{Enter}');
    expect(pw(/^Confirm new password/)).toHaveFocus();
    expect(onReset).not.toHaveBeenCalled();
    await user.clear(pw(/^Confirm new password/));
    await user.type(pw(/^Confirm new password/), `${STRONG}{Enter}`);
    expect(onReset).toHaveBeenCalledWith(STRONG);
  });

  it('shows the done step with a sign-in link when uncontrolled', async () => {
    const user = userEvent.setup();
    render(
      <PasswordResetForm
        defaultStep="reset"
        confirmPassword={false}
        onReset={() => Promise.resolve()}
        signInHref="/sign-in"
      />,
    );
    await user.type(pw(/^New password/), `${STRONG}{Enter}`);
    const heading = await screen.findByRole('heading', { name: 'Password changed' });
    await waitFor(() => expect(heading).toHaveFocus());
    expect(screen.getByRole('link', { name: 'Back to sign in' })).toHaveAttribute(
      'href',
      '/sign-in',
    );
  });

  it('shows rejections and outside errors, with translated messages', async () => {
    const user = userEvent.setup();
    const { rerender } = render(
      <PasswordResetForm
        onRequest={() => Promise.reject(new Error('Too many requests'))}
        messages={{ requestSubmit: 'Link senden' }}
      />,
    );
    await user.type(screen.getByRole('textbox', { name: /Email/ }), 'sam@example.com');
    await user.click(screen.getByRole('button', { name: 'Link senden' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('Too many requests');
    rerender(<PasswordResetForm step="reset" error="This link has expired." />);
    expect(screen.getByRole('alert')).toHaveTextContent('This link has expired.');
  });

  it('returns focus to the field after a failed request, where the browser dropped it', async () => {
    const user = userEvent.setup();
    let fail: (error: Error) => void = () => undefined;
    render(
      <PasswordResetForm onRequest={() => new Promise<void>((_, reject) => (fail = reject))} />,
    );
    const email = screen.getByRole('textbox', { name: /Email/ });
    await user.type(email, 'sam@example.com{Enter}');
    expect(email).toBeDisabled();
    dropFocus();
    fail(new Error('Too many requests'));
    expect(await screen.findByRole('alert')).toHaveTextContent('Too many requests');
    await waitFor(() => expect(email).toHaveFocus());
  });

  it('renders on the server', () => {
    expect(renderToString(<PasswordResetForm />)).toContain('mn-password-reset-form');
    expect(renderToString(<PasswordResetForm step="reset" />)).toContain('new-password');
  });
});
