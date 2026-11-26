import { describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { Field, FieldLabel } from '../../forms/form-field/index.js';
import {
  PHONE_COUNTRIES,
  PhoneCountrySelect,
  getPhoneCountry,
  matchPhoneCountry,
} from './index.js';

describe('PhoneCountrySelect', () => {
  it('names the combobox from its Field label and shows the selected country', async () => {
    render(
      <Field>
        <FieldLabel>Country code</FieldLabel>
        <PhoneCountrySelect defaultValue="gb" locale="en" data-testid="root" />
      </Field>,
    );
    const input = screen.getByRole('combobox', { name: 'Country code' });
    expect(input).toHaveValue('United Kingdom (+44)');
    expect(screen.getByTestId('root').closest('.mn-phone-country-select')).not.toBeNull();
    await expectNoA11yViolations();
  });

  it('finds a country by its calling code digits and selects it with the keyboard', async () => {
    const onValueChange = vi.fn();
    render(
      <PhoneCountrySelect aria-label="Country code" locale="en" onValueChange={onValueChange} />,
    );
    const user = userEvent.setup();
    const input = screen.getByRole('combobox', { name: 'Country code' });
    await user.type(input, '+49');
    const list = await screen.findByRole('listbox', { name: 'Country calling codes' });
    expect(within(list).getAllByRole('option')[0]).toHaveTextContent('Germany (+49)');
    await user.keyboard('{Enter}');
    expect(onValueChange).toHaveBeenCalledWith('DE', '+49');
    await waitFor(() => expect(input).toHaveAttribute('aria-expanded', 'false'));
    expect(input).toHaveValue('Germany (+49)');
    await expectNoA11yViolations();
  });

  it('lists preferred countries first under their own heading', async () => {
    render(
      <PhoneCountrySelect
        aria-label="Country code"
        locale="en"
        preferred={['in', 'US']}
        countries={['US', 'IN', 'FR', 'BR']}
        showFlags={false}
      />,
    );
    const user = userEvent.setup();
    await user.click(screen.getByRole('combobox'));
    await user.keyboard('{ArrowDown}');
    const list = await screen.findByRole('listbox');
    const options = within(list).getAllByRole('option');
    expect(options.map((o) => o.textContent)).toEqual([
      'India (+91)',
      'United States (+1)',
      'Brazil (+55)',
      'France (+33)',
    ]);
    expect(within(list).getByText('Suggested')).toBeInTheDocument();
    expect(within(list).getByText('All countries')).toBeInTheDocument();
  });

  it('shows the empty message and translated option text', async () => {
    render(
      <PhoneCountrySelect
        aria-label="Indicatif"
        locale="fr"
        countries={['FR', 'DE']}
        messages={{ empty: 'Aucun pays', option: (name, dial) => `${name} ${dial}` }}
      />,
    );
    const user = userEvent.setup();
    const input = screen.getByRole('combobox');
    await user.type(input, 'Allem');
    expect(await screen.findByRole('option', { name: /Allemagne \+49/ })).toBeInTheDocument();
    await user.clear(input);
    await user.type(input, 'zzz');
    expect(await screen.findByText('Aucun pays')).toBeInTheDocument();
    await user.keyboard('{Escape}');
    await waitFor(() => expect(input).toHaveAttribute('aria-expanded', 'false'));
  });

  it('renders on the server', () => {
    const html = renderToString(
      <PhoneCountrySelect aria-label="Country code" locale="en" defaultValue="JP" />,
    );
    expect(html).toContain('mn-phone-country-select');
    expect(html).toContain('Japan (+81)');
  });
});

describe('phone country data', () => {
  it('has unique ISO codes and well-formed calling codes', () => {
    const codes = PHONE_COUNTRIES.map((c) => c.code);
    expect(new Set(codes).size).toBe(codes.length);
    for (const c of PHONE_COUNTRIES) {
      expect(c.code).toMatch(/^[A-Z]{2}$/);
      expect(c.dialCode).toMatch(/^\+\d{1,3}$/);
    }
    expect(getPhoneCountry('in')?.dialCode).toBe('+91');
    expect(getPhoneCountry('ZZ')).toBeUndefined();
  });

  it('matches a number to its country, including shared calling codes', () => {
    expect(matchPhoneCountry('+44 20 7946 0000')?.code).toBe('GB');
    expect(matchPhoneCountry('+44 1534 123456')?.code).toBe('JE');
    expect(matchPhoneCountry('+1 242 555 0100')?.code).toBe('BS');
    expect(matchPhoneCountry('+1 415 555 0100')?.code).toBe('US');
    expect(matchPhoneCountry('+1 416 555 0100', 'CA')?.code).toBe('CA');
    expect(matchPhoneCountry('0091 98765 43210')?.code).toBe('IN');
    expect(matchPhoneCountry('+7 701 000 0000')?.code).toBe('KZ');
    expect(matchPhoneCountry('020 7946 0000')).toBeUndefined();
  });

  it('matches numbers typed with Arabic-Indic, Persian and full-width digits', () => {
    expect(matchPhoneCountry('+٤٤ ٢٠ ٧٩٤٦ ٠٠٠٠')?.code).toBe('GB');
    expect(matchPhoneCountry('+۹۸ ۲۱ ۱۲۳۴ ۵۶۷۸')?.code).toBe('IR');
    expect(matchPhoneCountry('＋１ ２４２ ５５５ ０１００')?.code).toBe('BS');
    expect(matchPhoneCountry('\u200e+44\u201120 7946 0000')?.code).toBe('GB');
  });

  it('does not throw on a locale the runtime does not know', () => {
    render(<PhoneCountrySelect aria-label="Country code" locale="not a locale" value="GB" />);
    expect(screen.getByRole('combobox', { name: 'Country code' })).toBeInTheDocument();
  });

  it('does not rebuild the list when equal inline lists are re-rendered', () => {
    const of = vi.spyOn(Intl.DisplayNames.prototype, 'of');
    const tree = () => (
      <PhoneCountrySelect
        aria-label="Country code"
        locale="en"
        countries={['GB', 'FR', 'DE']}
        preferred={['FR']}
      />
    );
    const { rerender } = render(tree());
    const calls = of.mock.calls.length;
    rerender(tree());
    rerender(tree());
    expect(of.mock.calls.length).toBe(calls);
    of.mockRestore();
  });
});
