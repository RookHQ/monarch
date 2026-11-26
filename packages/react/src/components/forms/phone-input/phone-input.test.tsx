import { useState } from 'react';
import { describe, expect, it, vi } from 'vitest';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { Field, FieldDescription, FieldLabel } from '../form-field/index.js';
import { PhoneInput, isValidE164, parsePhoneNumber, toE164 } from './index.js';

describe('phone helpers', () => {
  it('builds E.164, dropping separators and a trunk 0 (not in Italy)', () => {
    expect(toE164('+44', '020 7946 0958')).toBe('+442079460958');
    expect(toE164('+1', '(555) 010-0123')).toBe('+15550100123');
    expect(toE164('+39', '06 1234 5678', 'IT')).toBe('+390612345678');
    expect(toE164('+44', ' - ')).toBeNull();
  });

  it('validates E.164 length', () => {
    expect(isValidE164('+442079460958')).toBe(true);
    expect(isValidE164('+4412')).toBe(false);
    expect(isValidE164('+1234567890123456')).toBe(false);
    expect(isValidE164('442079460958')).toBe(false);
  });

  it('splits international numbers, including shared calling codes', () => {
    expect(parsePhoneNumber('+44 20 7946 0958')).toEqual({
      country: 'GB',
      dialCode: '+44',
      national: '2079460958',
    });
    expect(parsePhoneNumber('0049 30 1234567')?.country).toBe('DE');
    expect(parsePhoneNumber('+1 242 555 0100')?.country).toBe('BS');
    expect(parsePhoneNumber('+15550100123', 'CA')?.country).toBe('CA');
    expect(parsePhoneNumber('555 0100')).toBeNull();
  });
});

describe('PhoneInput', () => {
  it('labels the number from the Field and the country picker separately', async () => {
    render(
      <Field>
        <FieldLabel>Phone</FieldLabel>
        <PhoneInput defaultCountry="GB" locale="en" />
        <FieldDescription>For delivery updates.</FieldDescription>
      </Field>,
    );
    const number = screen.getByRole('textbox', { name: 'Phone' });
    expect(number).toHaveAttribute('type', 'tel');
    expect(number).toHaveAttribute('autocomplete', 'tel-national');
    expect(number).toHaveAccessibleDescription('For delivery updates.');
    const country = screen.getByRole('combobox', { name: 'Country code' });
    expect(country).toHaveValue('United Kingdom (+44)');
    expect(country.id).not.toBe(number.id);
    await expectNoA11yViolations();
  });

  it('emits E.164 on blur, never while typing', async () => {
    const onValueChange = vi.fn();
    render(
      <PhoneInput
        aria-label="Phone"
        defaultCountry="GB"
        locale="en"
        onValueChange={onValueChange}
      />,
    );
    const number = screen.getByRole('textbox', { name: 'Phone' });
    const user = userEvent.setup();
    await user.type(number, '020 7946 0958');
    expect(onValueChange).not.toHaveBeenCalled();
    await user.tab();
    expect(onValueChange).toHaveBeenCalledWith('+442079460958', {
      country: 'GB',
      dialCode: '+44',
      national: '2079460958',
    });
  });

  it('switches country and splits a pasted international number', async () => {
    const onCountryChange = vi.fn();
    const onValueChange = vi.fn();
    render(
      <PhoneInput
        aria-label="Phone"
        locale="en"
        name="phone"
        onCountryChange={onCountryChange}
        onValueChange={onValueChange}
      />,
    );
    const number = screen.getByRole('textbox', { name: 'Phone' });
    fireEvent.change(number, { target: { value: '+44 20 7946 0958' } });
    expect(onCountryChange).toHaveBeenCalledWith('GB');
    expect(number).toHaveValue('2079460958');
    expect(screen.getByRole('combobox', { name: 'Country code' })).toHaveValue(
      'United Kingdom (+44)',
    );
    fireEvent.blur(number);
    expect(onValueChange).toHaveBeenLastCalledWith('+442079460958', expect.anything());
    expect(document.querySelector('input[name="phone"]')).toHaveValue('+442079460958');
  });

  it('re-emits when the country changes with the keyboard', async () => {
    const onValueChange = vi.fn();
    render(
      <PhoneInput
        aria-label="Phone"
        locale="en"
        defaultValue="+15550100123"
        onValueChange={onValueChange}
      />,
    );
    const country = screen.getByRole('combobox', { name: 'Country code' });
    expect(screen.getByRole('textbox', { name: 'Phone' })).toHaveValue('5550100123');
    const user = userEvent.setup();
    await user.clear(country);
    await user.type(country, '+49');
    await screen.findByRole('listbox');
    await user.keyboard('{Enter}');
    await waitFor(() =>
      expect(onValueChange).toHaveBeenLastCalledWith('+495550100123', expect.anything()),
    );
  });

  it('flags an incomplete number, keeps it, and Escape restores', async () => {
    const onValueChange = vi.fn();
    render(
      <PhoneInput
        aria-label="Phone"
        defaultCountry="DE"
        locale="en"
        defaultValue="+49301234567"
        onValueChange={onValueChange}
      />,
    );
    const number = screen.getByRole('textbox', { name: 'Phone' });
    const user = userEvent.setup();
    await user.clear(number);
    await user.type(number, '12{Enter}');
    expect(number).toHaveAttribute('aria-invalid', 'true');
    expect(number).toHaveValue('12');
    const alert = screen.getByRole('alert');
    expect(alert).toHaveTextContent('Enter a complete phone number.');
    expect(number.getAttribute('aria-describedby')).toContain(alert.id);
    expect(onValueChange).not.toHaveBeenCalled();
    await user.keyboard('{Escape}');
    expect(number).toHaveValue('301234567');
    expect(number).not.toHaveAttribute('aria-invalid');
  });

  it('clears to null and follows a controlled value', async () => {
    function Controlled() {
      const [v, setV] = useState<string | null>('+442079460958');
      return (
        <>
          <PhoneInput aria-label="Phone" locale="en" value={v} onValueChange={setV} />
          <button type="button" onClick={() => setV('+33142685300')}>
            Use Paris
          </button>
          <output>{String(v)}</output>
        </>
      );
    }
    render(<Controlled />);
    const number = screen.getByRole('textbox', { name: 'Phone' });
    const user = userEvent.setup();
    await user.clear(number);
    await user.tab();
    expect(screen.getByRole('status')).toHaveTextContent('null');
    await user.click(screen.getByRole('button', { name: 'Use Paris' }));
    expect(number).toHaveValue('142685300');
    expect(screen.getByRole('combobox', { name: 'Country code' })).toHaveValue('France (+33)');
  });

  it('takes disabled from the Field and translated text', () => {
    render(
      <Field disabled>
        <FieldLabel>Telefon</FieldLabel>
        <PhoneInput locale="de" messages={{ country: 'Ländervorwahl' }} />
      </Field>,
    );
    expect(screen.getByRole('textbox', { name: 'Telefon' })).toBeDisabled();
    expect(screen.getByRole('combobox', { name: 'Ländervorwahl' })).toBeDisabled();
  });

  it('renders on the server', () => {
    const html = renderToString(
      <PhoneInput aria-label="Phone" locale="en" defaultValue="+442079460958" />,
    );
    expect(html).toContain('mn-phone-input');
    expect(html).toContain('2079460958');
  });

  it('keeps the country fixed while read-only and does not submit while disabled', () => {
    const { container, rerender } = render(
      <PhoneInput
        aria-label="Phone"
        locale="en"
        name="phone"
        readOnly
        defaultValue="+442079460958"
      />,
    );
    expect(screen.getByRole('combobox', { name: 'Country code' })).toBeDisabled();
    expect(container.querySelector('input[type="hidden"]')).not.toBeDisabled();
    rerender(
      <PhoneInput
        aria-label="Phone"
        locale="en"
        name="phone"
        disabled
        defaultValue="+442079460958"
      />,
    );
    expect(container.querySelector('input[type="hidden"]')).toBeDisabled();
  });

  it('reads digits typed in another script', () => {
    expect(toE164('+20', '\u0661\u0660\u0660 \u0661\u0662\u0663 \u0664\u0665\u0666\u0667')).toBe(
      '+201001234567',
    );
    const onValueChange = vi.fn();
    render(
      <PhoneInput
        aria-label="Phone"
        locale="en"
        defaultCountry="EG"
        onValueChange={onValueChange}
      />,
    );
    const input = screen.getByRole('textbox', { name: 'Phone' });
    fireEvent.change(input, {
      target: { value: '\u0661\u0660\u0660\u0661\u0662\u0663\u0664\u0665\u0666\u0667' },
    });
    fireEvent.blur(input);
    expect(onValueChange).toHaveBeenCalledWith('+201001234567', expect.anything());
  });
});
