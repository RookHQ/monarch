import { describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { SignUpForm } from './index.js';

const TERMS = [
  { label: 'Terms of Service', href: '/terms', version: '2026-01' },
  { label: 'Privacy Policy', href: '/privacy' },
];
const STRONG = 'Correct-Horse-9';

/** What a browser does when the focused field is disabled (jsdom does not): focus falls to <body>. */
function dropFocus() {
  const probe = document.createElement('button');
  document.body.append(probe);
  probe.focus();
  probe.remove();
  expect(document.body).toHaveFocus();
}

describe('SignUpForm', () => {
  it('renders labelled fields with new-password autocomplete and a strength meter', async () => {
    render(
      <SignUpForm
        aria-label="Create account"
        onSubmit={() => undefined}
        terms={TERMS}
        signInHref="/in"
      />,
    );
    expect(screen.getByRole('form', { name: 'Create account' })).toHaveClass('mn-sign-up-form');
    expect(screen.getByRole('textbox', { name: /Full name/ })).toHaveAttribute(
      'autocomplete',
      'name',
    );
    expect(screen.getByRole('textbox', { name: /Email/ })).toHaveAttribute('autocomplete', 'email');
    const password = screen.getByLabelText(/^Password/, { selector: 'input' });
    expect(password).toHaveAttribute('autocomplete', 'new-password');
    // Stable names for password managers and native form posts.
    expect(screen.getByRole('textbox', { name: /Full name/ })).toHaveAttribute('name', 'name');
    expect(screen.getByRole('textbox', { name: /Email/ })).toHaveAttribute('name', 'email');
    expect(password).toHaveAttribute('name', 'new-password');
    expect(password.getAttribute('aria-describedby')).toBeTruthy();
    expect(screen.getByText('At least 8 characters')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Sign in' })).toHaveAttribute('href', '/in');
    await expectNoA11yViolations();
  });

  it('validates every field and focuses the first problem', async () => {
    const user = userEvent.setup();
    const onSubmit = vi.fn();
    render(<SignUpForm onSubmit={onSubmit} terms={TERMS} confirmPassword />);
    await user.click(screen.getByRole('button', { name: 'Create account' }));
    expect(onSubmit).not.toHaveBeenCalled();
    expect(screen.getByRole('textbox', { name: /Full name/ })).toHaveFocus();
    expect(screen.getByText('Enter your name.')).toBeInTheDocument();
    expect(screen.getByText('Enter your email address.')).toBeInTheDocument();
    expect(screen.getByText('Choose a password.')).toBeInTheDocument();
    expect(screen.getByText('Accept the terms to continue')).toBeInTheDocument();
    await user.type(screen.getByRole('textbox', { name: /Full name/ }), 'Sam Rivera');
    await user.type(screen.getByRole('textbox', { name: /Email/ }), 'sam@example.com');
    await user.type(screen.getByLabelText(/^Password/, { selector: 'input' }), 'short');
    await user.type(screen.getByLabelText(/^Confirm password/, { selector: 'input' }), 'other');
    await user.click(screen.getByRole('button', { name: 'Create account' }));
    expect(screen.getByLabelText(/^Password/, { selector: 'input' })).toHaveFocus();
    expect(screen.getByText('Choose a password that meets every requirement.')).toBeInTheDocument();
    expect(screen.getByText('The passwords don’t match.')).toBeInTheDocument();
  });

  it('submits values with terms proof and shows a pending state', async () => {
    const user = userEvent.setup();
    let finish: () => void = () => undefined;
    const onSubmit = vi.fn(() => new Promise<void>((resolve) => (finish = resolve)));
    render(<SignUpForm onSubmit={onSubmit} terms={TERMS} />);
    await user.type(screen.getByRole('textbox', { name: /Full name/ }), ' Sam Rivera ');
    await user.type(screen.getByRole('textbox', { name: /Email/ }), 'sam@example.com');
    await user.type(screen.getByLabelText(/^Password/, { selector: 'input' }), STRONG);
    await user.click(screen.getByRole('checkbox'));
    await user.type(screen.getByLabelText(/^Password/, { selector: 'input' }), '{Enter}');
    expect(onSubmit).toHaveBeenCalledWith({
      name: 'Sam Rivera',
      email: 'sam@example.com',
      password: STRONG,
      terms: expect.objectContaining({ versions: { '/terms': '2026-01' } }),
    });
    expect(screen.getByRole('button', { name: 'Create account' })).toHaveAttribute(
      'aria-busy',
      'true',
    );
    finish();
    await waitFor(() =>
      expect(screen.getByRole('button', { name: 'Create account' })).not.toHaveAttribute(
        'aria-busy',
      ),
    );
  });

  it('maps server errors to fields and shows rejections', async () => {
    const user = userEvent.setup();
    const onSubmit = vi
      .fn()
      .mockResolvedValueOnce({ fieldErrors: { email: 'That email is already registered.' } })
      .mockRejectedValueOnce(new Error('Service unavailable'));
    render(<SignUpForm onSubmit={onSubmit} askName={false} passwordRules={[]} />);
    const email = screen.getByRole('textbox', { name: /Email/ });
    await user.type(email, 'sam@example.com');
    await user.type(screen.getByLabelText(/^Password/, { selector: 'input' }), 'anything{Enter}');
    await waitFor(() => expect(email).toHaveFocus());
    expect(email).toHaveAccessibleDescription('That email is already registered.');
    expect(onSubmit).toHaveBeenCalledWith({
      name: '',
      email: 'sam@example.com',
      password: 'anything',
    });
    await user.click(screen.getByRole('button', { name: 'Create account' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('Service unavailable');
  });

  it('returns focus to the field after a failed submit, where the browser dropped it', async () => {
    const user = userEvent.setup();
    let fail: (error: Error) => void = () => undefined;
    const onSubmit = vi.fn(() => new Promise<void>((_, reject) => (fail = reject)));
    render(<SignUpForm onSubmit={onSubmit} askName={false} passwordRules={[]} />);
    await user.type(screen.getByRole('textbox', { name: /Email/ }), 'sam@example.com');
    const password = screen.getByLabelText(/^Password/, { selector: 'input' });
    await user.type(password, 'anything{Enter}');
    expect(password).toBeDisabled();
    dropFocus();
    fail(new Error('Service unavailable'));
    expect(await screen.findByRole('alert')).toHaveTextContent('Service unavailable');
    await waitFor(() => expect(password).toHaveFocus());
  });

  it('takes providers, a read-only invited email and translated messages', () => {
    render(
      <SignUpForm
        onSubmit={() => undefined}
        defaultEmail="invited@example.com"
        emailReadOnly
        providers={<button type="button">Continue with Example</button>}
        messages={{ submit: 'Konto erstellen' }}
      />,
    );
    expect(screen.getByRole('textbox', { name: /Email/ })).toHaveAttribute('readonly');
    expect(screen.getByRole('textbox', { name: /Email/ })).toHaveValue('invited@example.com');
    expect(screen.getByRole('button', { name: 'Continue with Example' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Konto erstellen' })).toBeInTheDocument();
  });

  it('renders on the server', () => {
    const html = renderToString(<SignUpForm onSubmit={() => undefined} terms={TERMS} />);
    expect(html).toContain('mn-sign-up-form');
    expect(html).toContain('new-password');
  });
});
