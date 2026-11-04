import { describe, expect, it, vi } from 'vitest';
import { useState } from 'react';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import {
  LocationFields,
  type LocationAdapter,
  type LocationCityOption,
  type LocationValue,
} from './index.js';

// A small in-memory adapter; real products call their own API.
const COUNTRIES = [
  { value: 'AE', label: 'United Arab Emirates' },
  { value: 'CA', label: 'Canada' },
  { value: 'SG', label: 'Singapore' },
];
const REGIONS: Record<string, { label: string; options: { value: string; label: string }[] }> = {
  AE: {
    label: 'Emirate',
    options: [
      { value: 'AE-DU', label: 'Dubai' },
      { value: 'AE-AZ', label: 'Abu Dhabi' },
    ],
  },
  CA: {
    label: 'Province',
    options: [
      { value: 'CA-ON', label: 'Ontario' },
      { value: 'CA-QC', label: 'Quebec' },
    ],
  },
  SG: { label: 'Region', options: [] },
};
const CITIES: Record<string, LocationCityOption[]> = {
  CA: [
    { value: 'c-tor', label: 'Toronto', region: { value: 'CA-ON', label: 'Ontario' } },
    { value: 'c-ott', label: 'Ottawa', region: { value: 'CA-ON', label: 'Ontario' } },
    { value: 'c-mtl', label: 'Montreal', region: { value: 'CA-QC', label: 'Quebec' } },
  ],
};

function makeAdapter(overrides: { countries?: LocationAdapter['countries'] } = {}) {
  return {
    countries: vi.fn(async () => COUNTRIES),
    regions: vi.fn(async (country: string) => REGIONS[country] ?? { options: [] }),
    cities: vi.fn(
      async ({ country, region, query }: Parameters<NonNullable<LocationAdapter['cities']>>[0]) =>
        (CITIES[country] ?? []).filter(
          (c) =>
            (!region || c.region?.value === region) &&
            c.label.toLowerCase().includes(query.toLowerCase()),
        ),
    ),
    ...overrides,
  };
}

async function pick(
  user: ReturnType<typeof userEvent.setup>,
  name: string,
  text: string,
  option: string,
) {
  const box = screen.getByRole('combobox', { name });
  await user.clear(box);
  await user.type(box, text);
  await user.click(await screen.findByRole('option', { name: new RegExp(`^${option}`) }));
}

describe('LocationFields', () => {
  it('loads countries, then the regions under the country’s own word', async () => {
    const adapter = makeAdapter();
    const onValueChange = vi.fn();
    render(
      <LocationFields adapter={adapter} legend="Business address" onValueChange={onValueChange} />,
    );
    expect(screen.getByRole('group', { name: 'Business address' })).toBeInTheDocument();
    const user = userEvent.setup();
    await waitFor(() => expect(adapter.countries).toHaveBeenCalledTimes(1));
    await pick(user, 'Country', 'arab', 'United Arab Emirates');
    expect(onValueChange).toHaveBeenLastCalledWith(
      expect.objectContaining({ country: 'AE', region: null, city: '' }),
      'country',
    );
    const region = await screen.findByRole('combobox', { name: 'Emirate' });
    expect(adapter.regions).toHaveBeenCalledWith('AE', expect.any(AbortSignal));
    await user.click(region);
    await user.keyboard('{ArrowDown}');
    expect(within(screen.getByRole('listbox')).getAllByRole('option')).toHaveLength(2);
    await user.keyboard('{Enter}');
    expect(onValueChange).toHaveBeenLastCalledWith(
      expect.objectContaining({ region: 'AE-DU', regionName: 'Dubai' }),
      'region',
    );
    await expectNoA11yViolations();
  });

  it('disables region and city until a country is chosen', async () => {
    render(<LocationFields adapter={makeAdapter()} />);
    await screen.findByRole('combobox', { name: 'Country' });
    expect(screen.getByRole('textbox', { name: 'State / Region' })).toBeDisabled();
    expect(screen.getByRole('combobox', { name: 'City' })).toBeDisabled();
    expect(screen.getByRole('textbox', { name: 'Postal code' })).toHaveAttribute(
      'autocomplete',
      'postal-code',
    );
  });

  it('searches cities as the user types (debounced) and fills the region from the city', async () => {
    const adapter = makeAdapter();
    const onValueChange = vi.fn();
    render(
      <LocationFields
        adapter={adapter}
        defaultValue={{ country: 'CA' }}
        cityDebounce={300}
        onValueChange={onValueChange}
      />,
    );
    const user = userEvent.setup();
    const city = await screen.findByRole('combobox', { name: 'City' });
    await user.type(city, 'o');
    expect(await screen.findByText('Type at least 2 letters to search.')).toBeInTheDocument();
    await user.type(city, 'tt');
    await user.click(await screen.findByRole('option', { name: 'Ottawa' }));
    // One search for "ott": "o" was too short, and the debounce merged the rest.
    // (The second call lists the region that picking Ottawa filled in.)
    expect(adapter.cities.mock.calls.map((c) => c[0])).toEqual([
      { country: 'CA', region: null, query: 'ott' },
      { country: 'CA', region: 'CA-ON', query: '' },
    ]);
    expect(onValueChange).toHaveBeenLastCalledWith(
      expect.objectContaining({
        city: 'Ottawa',
        cityId: 'c-ott',
        region: 'CA-ON',
        regionName: 'Ontario',
      }),
      'city',
    );
    expect(city).toHaveValue('Ottawa');
  });

  it('lists a chosen region’s cities straight away', async () => {
    const adapter = makeAdapter();
    render(
      <LocationFields
        adapter={adapter}
        defaultValue={{ country: 'CA', region: 'CA-QC', regionName: 'Quebec' }}
      />,
    );
    await waitFor(() =>
      expect(adapter.cities).toHaveBeenCalledWith(
        { country: 'CA', region: 'CA-QC', query: '' },
        expect.any(AbortSignal),
      ),
    );
    const user = userEvent.setup();
    await user.click(screen.getByRole('combobox', { name: 'City' }));
    await user.keyboard('{ArrowDown}');
    expect(await screen.findByRole('option', { name: 'Montreal' })).toBeInTheDocument();
  });

  it('keeps a city typed freely when it is not in the list', async () => {
    const onValueChange = vi.fn();
    render(
      <LocationFields
        adapter={makeAdapter()}
        defaultValue={{ country: 'CA' }}
        cityDebounce={0}
        onValueChange={onValueChange}
      />,
    );
    const user = userEvent.setup();
    const city = await screen.findByRole('combobox', { name: 'City' });
    await user.type(city, 'Smalltown');
    await user.click(await screen.findByRole('option', { name: /Smalltown/ }));
    expect(onValueChange).toHaveBeenLastCalledWith(
      expect.objectContaining({ city: 'Smalltown', cityId: null }),
      'city',
    );
    expect(city).toHaveValue('Smalltown');
  });

  it('uses a free-text region when the country has no regions', async () => {
    const onValueChange = vi.fn();
    render(
      <LocationFields
        adapter={makeAdapter()}
        defaultValue={{ country: 'SG' }}
        onValueChange={onValueChange}
      />,
    );
    const region = await screen.findByRole('textbox', { name: 'Region' });
    expect(region).toHaveAttribute('placeholder', 'Optional');
    await userEvent.setup().type(region, 'Central');
    expect(onValueChange).toHaveBeenLastCalledWith(
      expect.objectContaining({ regionName: 'Central', region: null }),
      'region',
    );
  });

  it('shows an alert with Try again when a list fails, and retries', async () => {
    let fail = true;
    const adapter = makeAdapter({
      countries: vi.fn(async () => {
        if (fail) throw new Error('offline');
        return COUNTRIES;
      }),
    });
    render(<LocationFields adapter={adapter} />);
    const alert = await screen.findByRole('alert');
    expect(alert).toHaveTextContent('Couldn’t load countries.');
    fail = false;
    await userEvent.setup().click(within(alert).getByRole('button', { name: 'Try again' }));
    expect(await screen.findByRole('combobox', { name: 'Country' })).toBeInTheDocument();
    expect(adapter.countries).toHaveBeenCalledTimes(2);
  });

  it('clears region, city and postal code when the country changes (controlled)', async () => {
    function Controlled() {
      const [v, setV] = useState<LocationValue>({
        country: 'CA',
        region: 'CA-ON',
        regionName: 'Ontario',
        city: 'Toronto',
        cityId: 'c-tor',
        postalCode: 'M5V 2T6',
      });
      return (
        <>
          <LocationFields adapter={makeAdapter()} value={v} onValueChange={setV} />
          <output>{JSON.stringify(v)}</output>
        </>
      );
    }
    render(<Controlled />);
    const user = userEvent.setup();
    expect(await screen.findByRole('combobox', { name: 'City' })).toHaveValue('Toronto');
    await waitFor(() =>
      expect(screen.getByRole('combobox', { name: 'Country' })).toHaveValue('Canada'),
    );
    await pick(user, 'Country', 'sing', 'Singapore');
    expect(JSON.parse(document.querySelector('output')!.textContent!)).toEqual({
      country: 'SG',
      region: null,
      regionName: '',
      city: '',
      cityId: null,
      postalCode: '',
    });
  });

  it('marks required fields and shows errors, and can show a subset of fields', async () => {
    render(
      <LocationFields
        adapter={{ countries: makeAdapter().countries }}
        fields={['country', 'city']}
        required={['country', 'city']}
        errors={{ city: 'Enter a city.' }}
        labels={{ city: 'Town' }}
      />,
    );
    const country = await screen.findByRole('combobox', { name: 'Country' });
    expect(country).toBeRequired();
    const town = screen.getByRole('textbox', { name: 'Town' });
    expect(town).toHaveAttribute('aria-invalid', 'true');
    expect(town).toHaveAccessibleDescription('Enter a city.');
    expect(screen.queryByRole('textbox', { name: 'Postal code' })).not.toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('disables every field', async () => {
    render(<LocationFields adapter={makeAdapter()} disabled defaultValue={{ country: 'CA' }} />);
    expect(await screen.findByRole('combobox', { name: 'Country' })).toBeDisabled();
    expect(screen.getByRole('textbox', { name: 'Postal code' })).toBeDisabled();
  });

  it('renders on the server without calling the adapter', () => {
    const adapter = makeAdapter();
    const html = renderToString(<LocationFields adapter={adapter} legend="Address" />);
    expect(html).toContain('mn-location-fields');
    expect(adapter.countries).not.toHaveBeenCalled();
  });
});
