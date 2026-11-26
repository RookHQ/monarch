import { describe, expect, it, vi } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { Field, FieldLabel } from '../../forms/form-field/index.js';
import {
  COUNTRY_CODES,
  CountrySelect,
  getCountryFlag,
  getCountryName,
  listCountries,
} from './index.js';

/** The option's label, without its hint. */
const labelOf = (o: Element) => o.querySelector('.mn-menu__text')?.firstChild?.textContent;

describe('country helpers', () => {
  it('lists every ISO 3166-1 alpha-2 code once', () => {
    expect(COUNTRY_CODES).toHaveLength(249);
    expect(new Set(COUNTRY_CODES).size).toBe(249);
    expect(COUNTRY_CODES.every((c) => /^[A-Z]{2}$/.test(c))).toBe(true);
  });

  it('names countries in a locale and falls back to the code', () => {
    expect(getCountryName('de')).toBe('Germany');
    expect(getCountryName('DE', 'fr')).toBe('Allemagne');
    expect(getCountryName('DE', 'not a locale!')).toBe('DE');
  });

  it('falls back to codes without Intl.DisplayNames', () => {
    const original = Intl.DisplayNames;
    (Intl as { DisplayNames?: unknown }).DisplayNames = undefined;
    try {
      expect(getCountryName('PT', 'xx-test')).toBe('PT');
      expect(listCountries({ locale: 'xx-test', codes: ['PT'] })[0]?.name).toBe('PT');
    } finally {
      (Intl as { DisplayNames?: unknown }).DisplayNames = original;
    }
  });

  it('builds flag emoji from regional indicators', () => {
    expect(getCountryFlag('pt')).toBe('\u{1F1F5}\u{1F1F9}');
    expect(getCountryFlag('PRT')).toBe('');
  });

  it('does not throw on a locale the runtime does not know', () => {
    const list = listCountries({ locale: 'not a locale', codes: ['PT', 'DE'] });
    expect(list.map((c) => c.code).sort()).toEqual(['DE', 'PT']);
  });

  it('sorts by name in the locale', () => {
    const names = listCountries({ codes: ['DE', 'AT', 'CH'], locale: 'de' }).map((c) => c.name);
    expect(names).toEqual(['Deutschland', 'Österreich', 'Schweiz']);
  });
});

describe('CountrySelect', () => {
  it('searches by name or code and selects with the keyboard', async () => {
    const onValueChange = vi.fn();
    render(
      <Field>
        <FieldLabel>Country</FieldLabel>
        <CountrySelect onValueChange={onValueChange} countries={['PT', 'PL', 'PE', 'US']} />
      </Field>,
    );
    const input = screen.getByRole('combobox', { name: 'Country' });
    const user = userEvent.setup();
    await user.type(input, 'po');
    expect(screen.getAllByRole('option').map(labelOf)).toEqual(['Poland', 'Portugal']);
    await user.keyboard('{ArrowDown}{ArrowDown}{Enter}');
    expect(onValueChange).toHaveBeenLastCalledWith('PT');
    expect(input).toHaveValue('Portugal');
    await user.clear(input);
    await user.type(input, 'us');
    await user.keyboard('{ArrowDown}{Enter}');
    expect(onValueChange).toHaveBeenLastCalledWith('US');
    await expectNoA11yViolations();
  });

  it('lists suggested countries first, with flags and codes', async () => {
    render(
      <CountrySelect aria-label="Country" countries={['FR', 'IN', 'JP']} priority={['jp', 'XX']} />,
    );
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Show options' }));
    const groups = await screen.findAllByRole('group');
    expect(groups.map((g) => g.firstChild?.textContent)).toEqual(['Suggested', 'All countries']);
    const japan = within(groups[0]!).getByRole('option');
    expect(japan).toHaveTextContent('Japan');
    expect(japan).toHaveTextContent('JP');
    expect(japan.querySelector('.mn-country-select__flag')).toHaveTextContent('\u{1F1EF}\u{1F1F5}');
    expect(within(groups[1]!).getAllByRole('option').map(labelOf)).toEqual([
      'France',
      'India',
      'Japan',
    ]);
    await expectNoA11yViolations();
  });

  it('localises names, matches English names, hides flags and excludes codes', async () => {
    render(
      <CountrySelect
        aria-label="Pays"
        locale="fr"
        countries={['DE', 'ES', 'GB']}
        exclude={['GB']}
        showFlag={false}
        showCode={false}
      />,
    );
    const user = userEvent.setup();
    await user.type(screen.getByRole('combobox'), 'germ');
    const options = screen.getAllByRole('option');
    expect(options.map(labelOf)).toEqual(['Allemagne']);
    expect(options[0]!.querySelector('.mn-country-select__flag')).toBeNull();
    await user.clear(screen.getByRole('combobox'));
    await user.type(screen.getByRole('combobox'), 'roya');
    expect(screen.getByRole('status')).toHaveTextContent('No countries match');
  });

  it('accepts lower-case and out-of-list values and custom names', () => {
    render(
      <CountrySelect
        aria-label="Country"
        countries={['US']}
        value="tw"
        countryName={(c) => (c === 'TW' ? 'Taiwan' : undefined)}
      />,
    );
    expect(screen.getByRole('combobox')).toHaveValue('Taiwan');
  });

  it('renders on the server with its root class', () => {
    const html = renderToString(<CountrySelect aria-label="Country" defaultValue="BR" />);
    expect(html).toContain('mn-country-select');
    expect(html).toContain('Brazil');
  });
});
