import { useState } from 'react';
import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { Field, FieldLabel } from '../form-field/index.js';
import { NumberInput, decimalPlaces, parseNumber, roundTo, stepValue } from './index.js';

describe('number helpers', () => {
  it('rounds and steps without float drift', () => {
    expect(roundTo(0.1 + 0.2, 2)).toBe(0.3);
    expect(roundTo(1.005, 2)).toBe(1.01);
    expect(stepValue(0.2, 0.1, 1)).toBe(0.3);
    expect(stepValue(0.3, 0.1, -1)).toBe(0.2);
    let v = 0;
    for (let i = 0; i < 10; i++) v = stepValue(v, 0.1, 1);
    expect(v).toBe(1);
    expect(stepValue(5, 10, 1)).toBe(10);
    expect(stepValue(5, 10, -1)).toBe(0);
    expect(stepValue(3, 5, 1, 1)).toBe(6);
    expect(decimalPlaces(0.25)).toBe(2);
    expect(decimalPlaces(1e-7)).toBe(7);
  });

  it.each([
    ['1,234.5', 'en-US', 1234.5],
    ['1.234,5', 'de-DE', 1234.5],
    ['1 234,5', 'fr-FR', 1234.5],
    ['1 234,5', 'fr-FR', 1234.5],
    ['-12', 'en-US', -12],
    ['−12', 'en-US', -12],
    ['(1,200.50)', 'en-US', -1200.5],
    ['$1,200.50', 'en-US', 1200.5],
    ['١٢٣', 'ar-EG', 123],
    ['.5', 'en-US', 0.5],
  ])('reads %s in %s', (text, locale, expected) => {
    expect(parseNumber(text, locale)).toBe(expected);
  });

  it('returns null for empty and NaN for unreadable text', () => {
    expect(parseNumber('  ', 'en-US')).toBeNull();
    for (const bad of ['abc', '1.2.3', '1 2x3', '12#']) expect(parseNumber(bad, 'en-US')).toBeNaN();
  });

  it('divides by 100 for percent', () => {
    expect(parseNumber('12%', 'en-US', { percent: true })).toBe(0.12);
    expect(parseNumber('7.5', 'en-US', { percent: true })).toBe(0.075);
  });
});

describe('parseNumber in right-to-left locales', () => {
  it.each([
    ['he', -1234.5, {}],
    ['ar-EG', -1234.5, {}],
    ['fa', -1234.5, {}],
    ['he', 12, { style: 'currency', currency: 'ILS' }],
    ['ar-EG', -12.5, { style: 'currency', currency: 'USD' }],
  ] as const)('reads back what Intl formats in %s (%s)', (locale, n, options) => {
    const text = new Intl.NumberFormat(locale, options).format(n);
    expect(parseNumber(text, locale)).toBe(n);
  });

  it('reads the Arabic percent sign', () => {
    const text = new Intl.NumberFormat('ar-EG', { style: 'percent' }).format(0.5);
    expect(parseNumber(text, 'ar-EG', { percent: true })).toBe(0.5);
  });
});

describe('NumberInput', () => {
  it('is a labelled spinbutton that commits on blur and formats in the locale', async () => {
    const onValueChange = vi.fn();
    render(
      <Field>
        <FieldLabel>Seats</FieldLabel>
        <NumberInput locale="de-DE" onValueChange={onValueChange} />
      </Field>,
    );
    const input = screen.getByRole('spinbutton', { name: 'Seats' });
    const user = userEvent.setup();
    await user.type(input, '1234,5');
    expect(onValueChange).not.toHaveBeenCalled();
    await user.tab();
    expect(onValueChange).toHaveBeenCalledWith(1234.5);
    expect(input).toHaveValue('1.234,5');
    expect(input).toHaveAttribute('aria-valuenow', '1234.5');
    expect(input).toHaveAttribute('aria-valuetext', '1.234,5');
    await expectNoA11yViolations();
  });

  it('steps with ArrowUp/Down, Shift and PageUp/Down, Home/End, without drift, and clamps', async () => {
    const onValueChange = vi.fn();
    render(
      <NumberInput
        aria-label="Ratio"
        defaultValue={0.1}
        step={0.1}
        min={0}
        max={2}
        onValueChange={onValueChange}
      />,
    );
    const input = screen.getByRole('spinbutton');
    const user = userEvent.setup();
    input.focus();
    await user.keyboard('{ArrowUp}{ArrowUp}');
    expect(input).toHaveValue('0.3');
    expect(onValueChange).toHaveBeenLastCalledWith(0.3);
    await user.keyboard('{ArrowDown}');
    expect(input).toHaveValue('0.2');
    await user.keyboard('{PageUp}');
    expect(input).toHaveValue('1'); // next point on the large-step grid
    await user.keyboard('{Shift>}{ArrowUp}{/Shift}');
    expect(input).toHaveValue('2');
    await user.keyboard('{PageDown}{PageDown}{PageDown}');
    expect(input).toHaveValue('0');
    await user.keyboard('{End}');
    expect(input).toHaveValue('2');
    await user.keyboard('{Home}');
    expect(input).toHaveValue('0');
    expect(onValueChange).toHaveBeenLastCalledWith(0);
  });

  it('has stepper buttons outside the tab order that disable at the bounds', async () => {
    const onValueChange = vi.fn();
    render(
      <NumberInput
        aria-label="Guests"
        defaultValue={1}
        min={1}
        max={3}
        onValueChange={onValueChange}
      />,
    );
    const user = userEvent.setup();
    const inc = screen.getByRole('button', { name: 'Increase' });
    const dec = screen.getByRole('button', { name: 'Decrease' });
    expect(dec).toBeDisabled();
    expect(inc).toHaveAttribute('tabindex', '-1');
    await user.click(inc);
    await user.click(inc);
    expect(screen.getByRole('spinbutton')).toHaveValue('3');
    expect(inc).toBeDisabled();
    expect(onValueChange).toHaveBeenLastCalledWith(3);
    // Pressing a stepper keeps focus in the input.
    expect(screen.getByRole('spinbutton')).toHaveFocus();
    await user.tab();
    expect(document.body).toHaveFocus();
    await expectNoA11yViolations();
  });

  it('flags unreadable and out-of-range text, keeps it, and Escape restores the value', async () => {
    const onValueChange = vi.fn();
    render(
      <NumberInput aria-label="Age" defaultValue={30} max={120} onValueChange={onValueChange} />,
    );
    const input = screen.getByRole('spinbutton');
    const user = userEvent.setup();
    await user.clear(input);
    await user.type(input, 'thirty{Enter}');
    expect(input).toHaveAttribute('aria-invalid', 'true');
    expect(input).toHaveValue('thirty');
    const alert = screen.getByRole('alert');
    expect(alert).toHaveTextContent('Enter a number.');
    expect(input.getAttribute('aria-describedby')).toContain(alert.id);
    await user.keyboard('{Escape}');
    expect(input).toHaveValue('30');
    expect(input).not.toHaveAttribute('aria-invalid');
    await user.clear(input);
    await user.type(input, '130{Enter}');
    expect(screen.getByRole('alert')).toHaveTextContent('Enter 120 or less.');
    expect(onValueChange).not.toHaveBeenCalled();
  });

  it('formats percent values and rounds to the shown precision', async () => {
    const onValueChange = vi.fn();
    render(
      <NumberInput
        aria-label="Discount"
        locale="en-US"
        formatOptions={{ style: 'percent' }}
        defaultValue={0.15}
        onValueChange={onValueChange}
      />,
    );
    const input = screen.getByRole('spinbutton');
    expect(input).toHaveValue('15%');
    const user = userEvent.setup();
    await user.keyboard('{Tab}');
    await user.keyboard('{ArrowUp}');
    expect(onValueChange).toHaveBeenLastCalledWith(0.16);
    expect(input).toHaveValue('16%');
    await user.clear(input);
    await user.type(input, '12.4{Enter}');
    expect(onValueChange).toHaveBeenLastCalledWith(0.12);
    expect(input).toHaveAttribute('aria-valuetext', '12%');
  });

  it('follows a controlled value, clears to null and submits the raw number', async () => {
    function Controlled() {
      const [v, setV] = useState<number | null>(1500);
      return (
        <form aria-label="Order">
          <NumberInput aria-label="Quantity" name="qty" value={v} onValueChange={setV} />
          <button type="button" onClick={() => setV(25)}>
            Set 25
          </button>
          <output>{String(v)}</output>
        </form>
      );
    }
    const { container } = render(<Controlled />);
    const input = screen.getByRole('spinbutton');
    expect(input).toHaveValue('1,500');
    expect(container.querySelector('input[type="hidden"]')).toHaveValue('1500');
    expect(input).not.toHaveAttribute('name');
    const user = userEvent.setup();
    await user.clear(input);
    await user.tab();
    expect(screen.getByRole('status')).toHaveTextContent('null');
    await user.click(screen.getByRole('button', { name: 'Set 25' }));
    expect(input).toHaveValue('25');
  });

  it('ignores keys and steppers when read-only, and hides steppers on request', async () => {
    const { rerender } = render(<NumberInput aria-label="Total" readOnly defaultValue={4} />);
    const input = screen.getByRole('spinbutton');
    const user = userEvent.setup();
    input.focus();
    await user.keyboard('{ArrowUp}');
    expect(input).toHaveValue('4');
    expect(screen.getByRole('button', { name: 'Increase' })).toBeDisabled();
    rerender(<NumberInput aria-label="Total" steppers={false} defaultValue={4} />);
    expect(screen.queryByRole('button')).toBeNull();
  });

  it('uses translated stepper names', () => {
    render(
      <NumberInput aria-label="Menge" messages={{ increment: 'Mehr', decrement: 'Weniger' }} />,
    );
    expect(screen.getByRole('button', { name: 'Mehr' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Weniger' })).toBeInTheDocument();
  });

  it('renders on the server', () => {
    const html = renderToString(
      <NumberInput aria-label="Amount" locale="en-US" defaultValue={12345.5} />,
    );
    expect(html).toContain('mn-number-input');
    expect(html).toContain('12,345.5');
  });

  it('keeps a negative value in a right-to-left locale on blur', async () => {
    const user = userEvent.setup();
    const onValueChange = vi.fn();
    render(
      <NumberInput
        aria-label="Offset"
        locale="he"
        defaultValue={-5}
        onValueChange={onValueChange}
      />,
    );
    const input = screen.getByRole('spinbutton');
    await user.click(input);
    await user.tab();
    expect(input).not.toHaveAttribute('aria-invalid');
    expect(screen.queryByRole('alert')).toBeNull();
    expect(onValueChange).not.toHaveBeenCalled();
  });

  it('does not submit its hidden input while disabled', () => {
    const { container } = render(
      <NumberInput aria-label="Total" name="total" defaultValue={3} disabled form="f1" />,
    );
    const hidden = container.querySelector('input[type="hidden"]');
    expect(hidden).toBeDisabled();
    expect(hidden).toHaveAttribute('form', 'f1');
  });
});
