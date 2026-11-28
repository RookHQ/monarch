import { useState } from 'react';
import { Field, FieldLabel } from '@rookcom/monarch-react';
import {
  CountrySelect,
  RegionSelect,
  getBuiltInRegions,
  type RegionOption,
} from '@rookcom/monarch-react/data';
import type { ComponentDocs } from '../../../internal/component-docs.js';
// The scenario of the primary example, shared with iOS and Android (CROSS-PLATFORM.md section 8).
import fixture from './region-select.fixture.json';

function CountryAndRegion() {
  const [country, setCountry] = useState<string | null>(fixture.data.country.value);
  const [region, setRegion] = useState<string | null>(fixture.data.region.value);
  return (
    <div style={{ display: 'grid', gap: 12, width: '100%', maxWidth: 320 }}>
      <Field>
        <FieldLabel>{fixture.data.country.label}</FieldLabel>
        <CountrySelect
          value={country}
          onValueChange={(next) => {
            setCountry(next);
            setRegion(null);
          }}
          countries={fixture.data.country.options}
        />
      </Field>
      <Field>
        <FieldLabel>{(country && getBuiltInRegions(country)?.label) ?? 'Region'}</FieldLabel>
        <RegionSelect country={country} value={region} onValueChange={setRegion} />
      </Field>
    </div>
  );
}

const SAMPLE: Record<string, RegionOption[]> = {
  FR: [
    { code: 'FR-ARA', name: 'Auvergne-Rhône-Alpes' },
    { code: 'FR-BRE', name: 'Bretagne' },
    { code: 'FR-IDF', name: 'Île-de-France' },
    { code: 'FR-OCC', name: 'Occitanie' },
  ],
};

/** A pretend API: answers after a short delay; built-in lists answer for the countries it lacks. */
function loadRegions(country: string, signal: AbortSignal) {
  return new Promise<RegionOption[]>((resolve, reject) => {
    const timer = setTimeout(
      () => resolve(SAMPLE[country] ?? getBuiltInRegions(country)?.regions ?? []),
      400,
    );
    signal.addEventListener('abort', () => {
      clearTimeout(timer);
      reject(new DOMException('Aborted', 'AbortError'));
    });
  });
}

export default {
  id: 'region-select',
  title: 'RegionSelect',
  summary:
    'Picks a state, province or territory of a country from data you pass, a loader, or compact built-in lists.',
  sections: {
    purpose: {
      summary:
        'Chooses a country’s first-level subdivision. Monarch ships only compact built-in lists (United States, Canada, Australia, United Kingdom nations and India, with ISO 3166-2 codes); every other country comes from the `regions` prop or an async `loadRegions(country)` loader backed by the product’s data. A country with no list gets a plain text field. Import from `@rookcom/monarch-react/data`. Needed by: Rook Calendar (`components/forms/GeoFields` subdivision picker), Rook People (organisation and compliance settings).',
      useWhen: [
        'A form has a country field and needs the state or province that goes with it.',
        'Tax, payroll or shipping rules depend on the subdivision.',
      ],
      avoidWhen: [
        'A whole address with city search: use LocationFields, which takes a regions adapter too.',
        'Typing an address: use AddressAutocomplete.',
      ],
    },
    designPrinciples: {
      items: [
        'Ship little data: five countries built in, everything else from the product, so the bundle stays small and the data stays current.',
        'Never a dead end: no country disables the field with a prompt; a country with no list becomes a text field.',
        'One request per country: loader results are cached for the field’s life and aborted when it unmounts.',
        'Values are stable codes (ISO 3166-2 for the built-in lists), never names.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Built on Combobox (and Input for typed text) and accepts their input props. Forwards its ref to the input.',
        'Data: `BUILT_IN_REGIONS`, `getBuiltInRegions(country)` → `{ label, regions } | null`. `label` is the English word for the country’s regions ("Province or territory"), useful for the FieldLabel.',
        'Source order: `regions` prop, then `loadRegions`, then the built-in list, then typed text.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        '`loadRegions(country, signal)` replaces the built-in lists. To keep them, return `getBuiltInRegions(country)?.regions` for countries your API lacks, as in the loader example.',
        'Changing `country` does not clear `value`; clear it in the country’s `onValueChange`.',
        'Migrating from Rook Calendar `GeoFields` subdivisions: `GeoSubdivision` items map to `{ code, name }` and the `/geo` request becomes `loadRegions`; the `subdivisionLabel` word goes in your FieldLabel.',
        'Migrating from a shadcn/ui Select of states: pass the items as `regions` (`value` → `code`, label → `name`); `value`/`onValueChange` keep their names.',
      ],
    },
    variants: {
      items: [
        { prop: 'size', values: ['sm', 'md', 'lg'], description: 'Height, as Combobox.' },
        {
          prop: 'showCode',
          values: ['true', 'false'],
          description: 'Short region code at the end of each option.',
        },
        {
          prop: 'allowTextEntry',
          values: ['true', 'false'],
          description: 'A text field when the country has no list (default), or an empty select.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'No country',
          attribute: 'data-source="none"',
          description: 'Disabled, "Choose a country first".',
        },
        {
          name: 'Loading',
          attribute: 'data-source="loading"',
          description: 'Combobox shows "Loading…" while the loader runs.',
        },
        {
          name: 'Error',
          description: '"Couldn’t load options." with "Try again", which calls the loader again.',
        },
        { name: 'List', attribute: 'data-source="list"', description: 'The country’s regions.' },
        {
          name: 'Text entry',
          attribute: 'data-source="text-entry"',
          description: 'An Input with `autocomplete="address-level1"`.',
        },
      ],
    },
    interaction: {
      items: [
        'Typing filters by name or code; names that start with the query come first.',
        'The loader runs as soon as the country is known, so a saved value shows its name before the list opens.',
        'A failed load is forgotten, so "Try again" asks again.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'ArrowDown / ArrowUp', action: 'Opens the list, then moves.' },
        {
          keys: 'PageDown / PageUp, Home / End',
          action: 'Moves ten regions, or to the first or last.',
        },
        { keys: 'Enter', action: 'Selects the active region.' },
        { keys: 'Escape', action: 'Closes and restores the selected name.' },
      ],
    },
    accessibility: {
      role: 'combobox, controlling a listbox (textbox in text-entry mode)',
      name: 'A FieldLabel or `aria-label`; use the country’s own word ("Province") as the label.',
      items: [
        'Disabled until a country is chosen, with the reason as the placeholder.',
        'Loading and errors are announced by Combobox’s status region.',
        '`autocomplete="address-level1"` lets browsers fill it.',
      ],
    },
    responsive: {
      items: [
        'Fills its container; the list matches the field width.',
        'Options keep 44px touch targets on coarse pointers, as Combobox.',
      ],
    },
    theme: {
      items: ['Combobox and Input tokens; no colours of its own.'],
    },
    examples: {
      items: [
        {
          title: 'With CountrySelect and built-in lists',
          stack: true,
          render: () => <CountryAndRegion />,
        },
        {
          title: 'Loaded from an API',
          stack: true,
          render: () => (
            <div style={{ width: '100%', maxWidth: 320 }}>
              <RegionSelect aria-label="Region" country="FR" loadRegions={loadRegions} />
            </div>
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Clear the region when the country changes.',
        'Label the field with the country’s word for its regions.',
        'Back `loadRegions` with your own data source for countries beyond the built-in five.',
      ],
      dont: [
        'Do not ask for a region before the country.',
        'Do not store region names; store the code.',
      ],
    },
    related: {
      items: [
        { id: 'country-select', reason: 'The country this field depends on.' },
        { id: 'location-fields', reason: 'Country, region, city and postal code together.' },
        { id: 'combobox', reason: 'The searchable select it is built on.' },
        { id: 'address-autocomplete', reason: 'For typing a whole address.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['combobox', 'text-field'],
      notes: ['Built-in data covers US, CA, AU, GB and IN only, in English.'],
    },
    testing: {
      unit: [
        'Built-in lists: countries, counts, ISO 3166-2 code shape, label word.',
        'Search by name and code; keyboard selection.',
        'Disabled with a prompt without a country; `regions` prop wins; text-entry field for a country with no list.',
        'Loader: error with "Try again", one load per country, aborted on unmount.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
