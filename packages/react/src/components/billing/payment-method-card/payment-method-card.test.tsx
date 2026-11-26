import { createRef } from 'react';
import { describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { cardExpiryStatus, PaymentMethodCard } from './index.js';

const now = new Date(2026, 5, 15); // June 2026

describe('PaymentMethodCard', () => {
  it('is an article named by brand and last four, with holder and expiry', async () => {
    render(
      <PaymentMethodCard
        brand="visa"
        last4="4242"
        expiryMonth={8}
        expiryYear={2028}
        holderName="A. Person"
        now={now}
      />,
    );
    const card = screen.getByRole('article', { name: 'Visa ending in 4242' });
    expect(card).toHaveClass('mn-payment-method-card');
    expect(card).toHaveAttribute('data-expiry', 'valid');
    expect(card).toHaveTextContent('A. Person');
    expect(card).toHaveTextContent('Expires 08/28');
    await expectNoA11yViolations();
  });

  it('works out expiring and expired cards and says so in text', () => {
    expect(cardExpiryStatus(6, 2026, now)).toBe('expiring');
    expect(cardExpiryStatus(7, 26, now)).toBe('expiring');
    expect(cardExpiryStatus(8, 2026, now)).toBe('valid');
    expect(cardExpiryStatus(5, 2026, now)).toBe('expired');
    render(
      <PaymentMethodCard
        brand="Mastercard"
        last4="0005"
        expiryMonth={1}
        expiryYear={2026}
        now={now}
        onMakeDefault={() => {}}
      />,
    );
    const card = screen.getByRole('article', { name: 'Mastercard ending in 0005' });
    expect(card).toHaveAttribute('data-expiry', 'expired');
    expect(card).toHaveTextContent('Expired');
    expect(card).toHaveTextContent('Expired 01/26');
    expect(
      screen.getByRole('button', { name: 'Make default, Mastercard ending in 0005' }),
    ).toBeDisabled();
  });

  it('shows the default badge and hides Make default on the default card', () => {
    render(<PaymentMethodCard brand="amex" last4="1000" isDefault onMakeDefault={() => {}} />);
    const card = screen.getByRole('article', { name: 'American Express ending in 1000' });
    expect(card).toHaveAttribute('data-default');
    expect(card).toHaveTextContent('Default');
    expect(screen.queryByRole('button', { name: /Make default/ })).toBeNull();
  });

  it('makes a card the default, showing progress while it runs', async () => {
    const user = userEvent.setup();
    let finish!: () => void;
    const onMakeDefault = vi.fn(() => new Promise<void>((resolve) => (finish = resolve)));
    render(<PaymentMethodCard brand="rupay" last4="6011" onMakeDefault={onMakeDefault} />);
    const button = screen.getByRole('button', { name: 'Make default, RuPay ending in 6011' });
    await user.tab();
    expect(button).toHaveFocus();
    await user.keyboard('{Enter}');
    expect(onMakeDefault).toHaveBeenCalledTimes(1);
    expect(button).toHaveAttribute('aria-busy', 'true');
    finish();
    await waitFor(() => expect(button).not.toHaveAttribute('aria-busy', 'true'));
  });

  it('reports a failed Make default and returns the button to idle', async () => {
    const unhandled = vi.fn();
    process.on('unhandledRejection', unhandled);
    const error = new Error('declined');
    const onMakeDefaultError = vi.fn();
    render(
      <PaymentMethodCard
        brand="visa"
        last4="4242"
        onMakeDefault={() => Promise.reject(error)}
        onMakeDefaultError={onMakeDefaultError}
      />,
    );
    const button = screen.getByRole('button', { name: /Make default/ });
    await userEvent.setup().click(button);
    await waitFor(() => expect(onMakeDefaultError).toHaveBeenCalledWith(error));
    expect(button).not.toHaveAttribute('aria-busy', 'true');
    await new Promise((resolve) => setImmediate(resolve));
    process.off('unhandledRejection', unhandled);
    expect(unhandled).not.toHaveBeenCalled();
  });

  it('does not read the clock on the server, and works the status out on the client', () => {
    const card = <PaymentMethodCard brand="visa" last4="4242" expiryMonth={1} expiryYear={2001} />;
    // The server cannot know the viewer's date: no "Expired" until hydration.
    expect(renderToString(card)).not.toContain('Expired');
    render(card);
    expect(screen.getByText('Expired')).toBeInTheDocument();
  });

  it('asks before removing; Cancel keeps the card and Remove card confirms', async () => {
    const user = userEvent.setup();
    const onRemove = vi.fn();
    render(<PaymentMethodCard brand="visa" last4="4242" onRemove={onRemove} />);
    const remove = screen.getByRole('button', { name: 'Remove, Visa ending in 4242' });
    await user.click(remove);
    const dialog = await screen.findByRole('alertdialog', { name: 'Remove Visa ending in 4242?' });
    expect(dialog).toBeVisible();
    expect(screen.getByRole('button', { name: 'Cancel' })).toHaveFocus();
    await user.keyboard('{Escape}');
    expect(onRemove).not.toHaveBeenCalled();
    await waitFor(() => expect(remove).toHaveFocus());
    await user.click(remove);
    await user.click(await screen.findByRole('button', { name: 'Remove card' }));
    expect(onRemove).toHaveBeenCalledTimes(1);
  });

  it('can disable Remove, translates text and forwards the ref', () => {
    const ref = createRef<HTMLElement>();
    render(
      <PaymentMethodCard
        ref={ref}
        brand="visa"
        last4="4242"
        onRemove={() => {}}
        removeDisabled
        messages={{ name: (b, l) => `${b} ···· ${l}`, remove: 'Quitar' }}
      />,
    );
    expect(ref.current?.tagName).toBe('ARTICLE');
    expect(screen.getByRole('article', { name: 'Visa ···· 4242' })).toBeVisible();
    expect(screen.getByRole('button', { name: 'Quitar, Visa ···· 4242' })).toBeDisabled();
  });

  it('renders on the server', () => {
    const html = renderToString(
      <PaymentMethodCard
        brand="visa"
        last4="4242"
        expiryMonth={8}
        expiryYear={28}
        expiryStatus="valid"
        onRemove={() => {}}
      />,
    );
    expect(html).toContain('mn-payment-method-card');
    expect(html).toContain('Visa ending in 4242');
  });
});
