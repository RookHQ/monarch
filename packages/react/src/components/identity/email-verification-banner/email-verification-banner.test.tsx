import { describe, expect, it, vi } from 'vitest';
import { act, fireEvent, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { EmailVerificationBanner } from './index.js';

const flush = () => act(async () => {});

describe('EmailVerificationBanner', () => {
  it('shows the address with resend and change actions', async () => {
    const onChangeEmail = vi.fn();
    const user = userEvent.setup();
    render(
      <EmailVerificationBanner
        data-testid="root"
        email="sam@example.com"
        onResend={() => undefined}
        onChangeEmail={onChangeEmail}
      />,
    );
    const root = screen.getByTestId('root');
    expect(root).toHaveClass('mn-email-verification-banner');
    expect(root).toHaveTextContent('Verify your email address');
    expect(root).toHaveTextContent('sam@example.com');
    await user.click(screen.getByRole('button', { name: 'Change email' }));
    expect(onChangeEmail).toHaveBeenCalled();
    await expectNoA11yViolations();
  });

  it('resends from the keyboard, announces it and counts down the cooldown', async () => {
    vi.useFakeTimers();
    try {
      const onResend = vi.fn(() => Promise.resolve());
      render(
        <EmailVerificationBanner email="sam@example.com" onResend={onResend} cooldownSeconds={3} />,
      );
      const button = screen.getByRole('button', { name: 'Resend email' });
      button.focus();
      fireEvent.click(button);
      await flush();
      expect(onResend).toHaveBeenCalledOnce();
      expect(screen.getByRole('status')).toHaveTextContent('Verification email sent');
      expect(button).toHaveTextContent('Resend in 3s');
      expect(button).toHaveAttribute('aria-disabled', 'true');
      expect(button).toHaveFocus();
      fireEvent.click(button);
      await flush();
      expect(onResend).toHaveBeenCalledOnce();
      act(() => vi.advanceTimersByTime(1000));
      expect(button).toHaveTextContent('Resend in 2s');
      act(() => vi.advanceTimersByTime(1000));
      act(() => vi.advanceTimersByTime(1000));
      expect(button).toHaveTextContent('Resend email');
      expect(button).not.toHaveAttribute('aria-disabled');
    } finally {
      vi.useRealTimers();
    }
  });

  it('shows a failure and allows another try at once', async () => {
    const user = userEvent.setup();
    const onResend = vi.fn(() => Promise.reject(new Error('Too many requests')));
    render(<EmailVerificationBanner email="sam@example.com" onResend={onResend} />);
    await user.click(screen.getByRole('button', { name: 'Resend email' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('Too many requests');
    expect(screen.getByRole('button', { name: 'Resend email' })).not.toHaveAttribute(
      'aria-disabled',
    );
  });

  it('starts with an initial cooldown, dismisses, and takes translated messages', async () => {
    const user = userEvent.setup();
    const onDismiss = vi.fn();
    render(
      <EmailVerificationBanner
        email="sam@example.com"
        onResend={() => undefined}
        initialCooldown={30}
        onDismiss={onDismiss}
        messages={{ resendIn: (s) => `Erneut in ${s} s`, dismiss: 'Schließen' }}
      />,
    );
    expect(screen.getByRole('button', { name: 'Erneut in 30 s' })).toHaveAttribute(
      'aria-disabled',
      'true',
    );
    await user.click(screen.getByRole('button', { name: 'Schließen' }));
    expect(onDismiss).toHaveBeenCalled();
    expect(screen.queryByText('Verify your email address')).toBeNull();
  });

  it('renders on the server', () => {
    const html = renderToString(
      <EmailVerificationBanner email="sam@example.com" onResend={() => undefined} />,
    );
    expect(html).toContain('mn-email-verification-banner');
    expect(html).toContain('sam@example.com');
  });
});
