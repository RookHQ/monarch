import { useState } from 'react';
import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { Field, FieldLabel } from '../form-field/index.js';
import { CurrencyInput, fromMinorUnits, toMinorUnits } from './index.js';

describe('minor-unit helpers', () => {
  it('converts without float loss and uses the currency decimals', () => {
    expect(toMinorUnits(19.99, 'USD')).toBe(1999);
    expect(toMinorUnits(0.29, 'USD')).toBe(29);
    expect(toMinorUnits(1.005, 'USD')).toBe(101);
    expect(fromMinorUnits(1999, 'USD')).toBe(19.99);
    expect(toMinorUnits(500, 'JPY')).toBe(500);
    expect(fromMinorUnits(1234, 'KWD')).toBe(1.234);
  });
});

describe('CurrencyInput', () => {
  it('formats the amount with the currency and is labelled by the Field', async () => {
    const onValueChange = vi.fn();
    render(
      <Field>
        <FieldLabel>Budget</FieldLabel>
        <CurrencyInput currency="USD" locale="en-US" onValueChange={onValueChange} />
      </Field>,
    );
    const input = screen.getByRole('spinbutton', { name: 'Budget' });
    const user = userEvent.setup();
    await user.type(input, '1234.5');
    await user.tab();
    expect(onValueChange).toHaveBeenCalledWith(1234.5);
    expect(input).toHaveValue('$1,234.50');
    expect(input).toHaveAttribute('aria-valuetext', '$1,234.50');
    expect(screen.queryByRole('button')).toBeNull();
    await expectNoA11yViolations();
  });

  it('reads and writes in the locale', async () => {
    render(<CurrencyInput aria-label="Preis" currency="EUR" locale="de-DE" defaultValue={0} />);
    const input = screen.getByRole('spinbutton');
    const user = userEvent.setup();
    await user.clear(input);
    await user.type(input, '1.234,5{Enter}');
    expect(input).toHaveValue('1.234,50 €');
    expect(input).toHaveAttribute('aria-valuenow', '1234.5');
  });

  it('works in integer minor units and rounds to the currency decimals', async () => {
    function Controlled() {
      const [cents, setCents] = useState<number | null>(1999);
      return (
        <>
          <CurrencyInput
            aria-label="Price"
            currency="USD"
            locale="en-US"
            minorUnits
            value={cents}
            onValueChange={setCents}
          />
          <output>{String(cents)}</output>
        </>
      );
    }
    render(<Controlled />);
    const input = screen.getByRole('spinbutton');
    expect(input).toHaveValue('$19.99');
    const user = userEvent.setup();
    await user.clear(input);
    await user.type(input, '0.295{Enter}');
    expect(screen.getByRole('status')).toHaveTextContent('30');
    expect(input).toHaveValue('$0.30');
    await user.keyboard('{ArrowUp}');
    expect(screen.getByRole('status')).toHaveTextContent('100'); // next whole unit
  });

  it('uses zero decimals for JPY and blocks negatives unless allowed', async () => {
    const { rerender } = render(
      <CurrencyInput aria-label="Fee" currency="JPY" locale="en-US" defaultValue={1500} />,
    );
    const input = screen.getByRole('spinbutton');
    expect(input).toHaveValue('¥1,500');
    expect(input).toHaveAttribute('aria-valuemin', '0');
    const user = userEvent.setup();
    await user.clear(input);
    await user.type(input, '-20{Enter}');
    expect(screen.getByRole('alert')).toHaveTextContent('Enter ¥0 or more.');
    rerender(
      <CurrencyInput
        aria-label="Fee"
        currency="JPY"
        locale="en-US"
        defaultValue={1500}
        allowNegative
      />,
    );
    await user.type(input, '{Enter}');
    expect(input).toHaveValue('-¥20');
  });

  it('shows the currency code when asked and can show steppers', async () => {
    render(
      <CurrencyInput
        aria-label="Deposit"
        currency="INR"
        locale="en-IN"
        currencyDisplay="code"
        defaultValue={150000}
        steppers
        step={500}
      />,
    );
    const input = screen.getByRole<HTMLInputElement>('spinbutton');
    expect(input.value).toMatch(/^INR\s1,50,000\.00$/);
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Increase' }));
    expect(input.value).toMatch(/1,50,500\.00$/);
  });

  it('renders on the server', () => {
    const html = renderToString(
      <CurrencyInput aria-label="Total" currency="GBP" locale="en-GB" defaultValue={42} />,
    );
    expect(html).toContain('mn-currency-input');
    expect(html).toContain('£42.00');
  });
});
