import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { Money, currencyDigits, formatMoney } from './index.js';

describe('formatMoney', () => {
  it('formats with Intl.NumberFormat in the given locale', () => {
    expect(formatMoney(1234.5, 'USD', { locale: 'en-US' })).toBe('$1,234.50');
    expect(formatMoney(1234.5, 'EUR', { locale: 'de-DE' })).toBe('1.234,50\u00a0€');
    expect(formatMoney(1234567, 'INR', { locale: 'en-IN' })).toBe('₹12,34,567.00');
  });

  it('reads minor units with the currency’s own digits', () => {
    expect(currencyDigits('USD')).toBe(2);
    expect(currencyDigits('JPY')).toBe(0);
    expect(currencyDigits('KWD')).toBe(3);
    expect(formatMoney(1999, 'USD', { locale: 'en-US', minorUnits: true })).toBe('$19.99');
    expect(formatMoney(1999, 'JPY', { locale: 'en-US', minorUnits: true })).toBe('¥1,999');
    expect(formatMoney(5, 'USD', { locale: 'en-US', minorUnits: true })).toBe('$0.05');
    expect(formatMoney(-250, 'USD', { locale: 'en-US', minorUnits: true })).toBe('-$2.50');
  });

  it('keeps precision for bigint and decimal-string amounts', () => {
    expect(formatMoney(9007199254740993n, 'USD', { locale: 'en-US', minorUnits: true })).toBe(
      '$90,071,992,547,409.93',
    );
    expect(formatMoney('0.10', 'USD', { locale: 'en-US' })).toBe('$0.10');
    expect(() => formatMoney('12.5', 'USD', { minorUnits: true })).toThrow(RangeError);
  });

  it('supports compact notation, sign display and accounting', () => {
    expect(formatMoney(1_250_000, 'USD', { locale: 'en-US', notation: 'compact' })).toBe('$1.3M');
    expect(formatMoney(42, 'USD', { locale: 'en-US', signDisplay: 'exceptZero' })).toBe('+$42.00');
    expect(formatMoney(-42, 'USD', { locale: 'en-US', currencySign: 'accounting' })).toBe(
      '($42.00)',
    );
    expect(formatMoney(42, 'USD', { locale: 'en-US', currencyDisplay: 'code' })).toMatch(
      /USD\s42\.00/,
    );
  });
});

describe('Money', () => {
  it('renders the formatted amount in tabular figures with data attributes', () => {
    render(<Money data-testid="m" amount={1999} currency="USD" minorUnits locale="en-US" />);
    const el = screen.getByTestId('m');
    expect(el).toHaveClass('mn-money');
    expect(el).toHaveTextContent('$19.99');
    expect(el).toHaveAttribute('data-currency', 'USD');
    expect(el).toHaveAttribute('data-notation', 'standard');
    expect(el).not.toHaveAttribute('title');
  });

  it('gives the full value to assistive technology and as a title when compact', () => {
    render(
      <Money data-testid="m" amount={1_250_000} currency="USD" locale="en-US" notation="compact" />,
    );
    const el = screen.getByTestId('m');
    expect(el).toHaveAttribute('title', '$1,250,000.00');
    expect(el.querySelector('[aria-hidden]')).toHaveTextContent('$1.3M');
    expect(el.querySelector('.mn-visually-hidden')).toHaveTextContent('$1,250,000.00');
  });

  it('does not duplicate text when the compact form equals the full form', () => {
    render(
      <Money
        data-testid="m"
        amount={12}
        currency="USD"
        locale="en-US"
        notation="compact"
        maximumFractionDigits={0}
      />,
    );
    const el = screen.getByTestId('m');
    expect(el.querySelector('.mn-visually-hidden')).toBeNull();
    expect(el).not.toHaveAttribute('title');
  });

  it('marks negatives and colours them only when asked', () => {
    const { rerender } = render(
      <Money data-testid="m" amount={-5} currency="USD" locale="en-US" />,
    );
    expect(screen.getByTestId('m')).toHaveAttribute('data-negative');
    expect(screen.getByTestId('m')).not.toHaveAttribute('data-tone');
    rerender(<Money data-testid="m" amount={-5} currency="USD" locale="en-US" toneNegative />);
    expect(screen.getByTestId('m')).toHaveAttribute('data-tone', 'danger');
    rerender(<Money data-testid="m" amount={5} currency="USD" locale="en-US" toneNegative />);
    expect(screen.getByTestId('m')).not.toHaveAttribute('data-tone');
  });

  it('renders an empty state for a missing amount', () => {
    const { rerender } = render(<Money data-testid="m" amount={null} currency="USD" />);
    expect(screen.getByTestId('m')).toHaveAttribute('data-empty');
    expect(screen.getByTestId('m')).toHaveTextContent('No amount');
    rerender(<Money data-testid="m" amount={undefined} currency="USD" emptyLabel="Not set" />);
    expect(screen.getByTestId('m')).toHaveTextContent('Not set');
  });

  it('forwards the ref and keeps a consumer title', () => {
    let node: HTMLSpanElement | null = null;
    render(
      <Money
        ref={(n) => {
          node = n;
        }}
        title="Quoted"
        amount={1_250_000}
        currency="USD"
        notation="compact"
      />,
    );
    expect(node).not.toBeNull();
    expect(node!).toHaveAttribute('title', 'Quoted');
  });

  it('has no axe violations', async () => {
    render(
      <main>
        <Money amount={1999} currency="USD" minorUnits />
        <Money amount={1_250_000} currency="EUR" notation="compact" />
        <Money amount={null} currency="USD" />
      </main>,
    );
    await expectNoA11yViolations();
  });

  it('renders on the server', () => {
    const html = renderToString(<Money amount={1999} currency="USD" minorUnits locale="en-US" />);
    expect(html).toContain('mn-money');
    expect(html).toContain('$19.99');
  });
});
