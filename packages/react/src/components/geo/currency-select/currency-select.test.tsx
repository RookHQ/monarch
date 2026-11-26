import { describe, expect, it, vi } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { Field, FieldLabel } from '../../forms/form-field/index.js';
import {
  CURRENCY_CODES,
  CurrencySelect,
  getCurrencyName,
  getCurrencySymbol,
  listCurrencies,
} from './index.js';

const labelOf = (o: Element) => o.querySelector('.mn-menu__text')?.firstChild?.textContent;

describe('currency helpers', () => {
  it('lists active ISO 4217 codes only', () => {
    expect(CURRENCY_CODES).toContain('EUR');
    expect(CURRENCY_CODES).not.toContain('HRK');
    expect(new Set(CURRENCY_CODES).size).toBe(CURRENCY_CODES.length);
  });

  it('names currencies and finds symbols in a locale', () => {
    expect(getCurrencyName('usd')).toBe('US Dollar');
    expect(getCurrencyName('EUR', 'de')).toBe('Euro');
    expect(getCurrencySymbol('INR')).toBe('₹');
    expect(getCurrencySymbol('USD', 'en-CA', 'standard')).toBe('US$');
    expect(getCurrencySymbol('USD', 'en-CA')).toBe('$');
    expect(getCurrencySymbol('not-a-code')).toBe('NOT-A-CODE');
  });

  it('does not throw on a locale the runtime does not know', () => {
    expect(listCurrencies({ locale: 'not a locale', codes: ['EUR', 'USD'] })).toHaveLength(2);
  });

  it('builds a number format once per currency and locale, not on every call', () => {
    const NumberFormat = vi.spyOn(Intl, 'NumberFormat');
    expect(getCurrencySymbol('CHF', 'fr-CH', 'standard')).toBe(
      getCurrencySymbol('CHF', 'fr-CH', 'standard'),
    );
    getCurrencySymbol('chf', 'fr-CH', 'standard');
    expect(NumberFormat).toHaveBeenCalledTimes(1);
    NumberFormat.mockRestore();
  });

  it('sorts by name', () => {
    expect(listCurrencies({ codes: ['USD', 'EUR', 'JPY'] }).map((c) => c.code)).toEqual([
      'EUR',
      'JPY',
      'USD',
    ]);
  });
});

describe('CurrencySelect', () => {
  it('searches by name, code or symbol and selects with the keyboard', async () => {
    const onValueChange = vi.fn();
    render(
      <Field>
        <FieldLabel>Currency</FieldLabel>
        <CurrencySelect onValueChange={onValueChange} currencies={['EUR', 'GBP', 'INR', 'USD']} />
      </Field>,
    );
    const input = screen.getByRole('combobox', { name: 'Currency' });
    const user = userEvent.setup();
    await user.type(input, '₹');
    expect(screen.getAllByRole('option').map(labelOf)).toEqual(['Indian Rupee']);
    await user.clear(input);
    await user.type(input, 'gbp');
    await user.keyboard('{ArrowDown}{Enter}');
    expect(onValueChange).toHaveBeenLastCalledWith('GBP');
    expect(input).toHaveValue('British Pound');
    await expectNoA11yViolations();
  });

  it('shows the code and symbol, suggested currencies and other displays', async () => {
    render(
      <CurrencySelect
        aria-label="Currency"
        currencies={['EUR', 'JPY', 'USD']}
        priority={['USD']}
        display="code-name"
        defaultValue="eur"
      />,
    );
    const input = screen.getByRole('combobox');
    expect(input).toHaveValue('EUR · Euro');
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Show options' }));
    const groups = await screen.findAllByRole('group');
    expect(groups.map((g) => g.firstChild?.textContent)).toEqual(['Suggested', 'All currencies']);
    expect(within(groups[0]!).getByRole('option')).toHaveTextContent('USD $');
    await expectNoA11yViolations();
  });

  it('shows the empty message', async () => {
    render(<CurrencySelect aria-label="Currency" currencies={['EUR']} display="code" />);
    const user = userEvent.setup();
    await user.type(screen.getByRole('combobox'), 'zzz');
    expect(screen.getByRole('status')).toHaveTextContent('No currencies match');
  });

  it('renders on the server', () => {
    const html = renderToString(<CurrencySelect aria-label="Currency" defaultValue="JPY" />);
    expect(html).toContain('mn-currency-select');
    expect(html).toContain('Japanese Yen');
  });
});
