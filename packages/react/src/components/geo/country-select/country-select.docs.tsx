import { useState } from 'react';
import { Field, FieldDescription, FieldLabel } from '@rookcom/monarch-react';
import { CountrySelect } from '@rookcom/monarch-react/data';
import type { ComponentDocs } from '../../../internal/component-docs.js';
// The scenario of the primary example, shared with iOS and Android (CROSS-PLATFORM.md section 8).
import fixture from './country-select.fixture.json';

function BillingCountry() {
  const [country, setCountry] = useState<string | null>(fixture.data.value);
  return (
    <Field style={{ width: '100%', maxWidth: 320 }}>
      <FieldLabel>{fixture.data.label}</FieldLabel>
      <CountrySelect
        defaultOpen
        value={country}
        onValueChange={setCountry}
        priority={fixture.data.priority}
        placeholder={fixture.data.placeholder}
      />
      <FieldDescription>Stored as {country ?? 'nothing yet'}.</FieldDescription>
    </Field>
  );
}

export default {
  id: 'country-select',
  title: 'CountrySelect',
  summary:
    'Searchable country picker with ISO 3166-1 codes, names from Intl.DisplayNames and optional flags.',
  sections: {
    purpose: {
      summary:
        'Chooses a country and stores its ISO 3166-1 alpha-2 code. Names come from the browser’s `Intl.DisplayNames` in any locale, so Monarch ships only the 249 codes, not translated name tables. Import from `@rookcom/monarch-react/data`. Needed by: Rook People (settings, profiles, expenses), Rook Links (verification), Rook ID (registry profile and jurisdiction), Rook Calendar (form builder geo fields).',
      useWhen: [
        'A form needs a country: billing, shipping, residence, tax or company registration.',
        'The interface is translated: pass `locale` and names and sort order follow it.',
        'Only some countries are allowed: pass `countries` or `exclude`.',
      ],
      avoidWhen: [
        'A full address: use LocationFields or AddressAutocomplete, which include the country.',
        'Picking a phone dial code: use PhoneCountrySelect.',
        'Picking a language or locale: use LanguageSelect; countries are not languages.',
      ],
    },
    designPrinciples: {
      items: [
        'The value is the code, never the name: names change with locale, codes do not.',
        'No bundled name tables: `Intl.DisplayNames` names every country in every locale the runtime knows.',
        'The likely answer first: `priority` lists suggested countries above the full list.',
        'Same render on server and client: the default locale is `en`, not the device’s.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Built on Combobox and accepts its input props (size, placeholder, invalid, disabled, clearable, limit, …). Forwards its ref to the input.',
        'Helpers: `COUNTRY_CODES`, `getCountryName(code, locale)`, `getCountryFlag(code)`, `listCountries({ locale, codes })`.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Without `Intl.DisplayNames` (very old runtimes) names fall back to the code; the picker still works.',
        'Migrating from the app copies (Rook People `components/geo/CountrySelect`, Rook Links `components/geo/CountrySelect`, Rook ID `components/registry/CountrySelect`): `onChange(iso2)` → `onValueChange(code)`; `label`, `hint` and `error` → wrap in `Field` with `FieldLabel`, `FieldDescription` and `FieldError`; `required`, `disabled`, `id` and `clearable` pass through; `invalid` stays `invalid`. The apps’ server-loaded country lists are no longer needed: names come from Intl. Rook Calendar (`components/forms/GeoFields` CountrySelect): `describedBy` → `aria-describedby`, `ariaLabel` → `aria-label`, `optional` → `clearable`.',
        'Migrating from a shadcn/ui Popover + Command country picker: `value`/`onValueChange` keep their names; the trigger button and CommandInput become the single combobox input.',
      ],
    },
    variants: {
      items: [
        { prop: 'size', values: ['sm', 'md', 'lg'], description: 'Height, as Combobox.' },
        {
          prop: 'showFlag',
          values: ['true', 'false'],
          description: 'Flag emoji before each name.',
        },
        {
          prop: 'showCode',
          values: ['true', 'false'],
          description: 'ISO code at the end of each option.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Sections',
          attribute: 'role="group"',
          description:
            '"Suggested" and "All countries" while nothing is typed and `priority` is set.',
        },
        { name: 'Searching', description: 'A flat list, best matches first.' },
        { name: 'Empty', attribute: 'role="status"', description: '"No countries match".' },
        { name: 'Selected / invalid / disabled', description: 'As Combobox.' },
      ],
    },
    interaction: {
      items: [
        'Typing filters by every word against the name, the code and, in other locales, the English name: "de", "germ" and "Allemagne" all find Germany.',
        'Matches whose name starts with the query come first.',
        'A value outside `countries` is still shown with its name.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'ArrowDown / ArrowUp', action: 'Opens the list, then moves.' },
        {
          keys: 'PageDown / PageUp, Home / End',
          action: 'Moves ten countries, or to the first or last.',
        },
        { keys: 'Enter', action: 'Selects the active country.' },
        { keys: 'Escape', action: 'Closes and restores the selected name.' },
        { keys: 'Backspace on empty input', action: 'Clears the selection when `clearable`.' },
      ],
    },
    accessibility: {
      role: 'combobox, controlling a listbox',
      name: 'A FieldLabel or `aria-label`.',
      items: [
        'Flags are decorative (`aria-hidden`); the name labels each option.',
        'Codes are read after the name as a hint.',
        'Inherits Combobox’s APG keyboard and `aria-activedescendant` support.',
      ],
    },
    responsive: {
      items: [
        'Fills its container; the list matches the field width and long names wrap.',
        'Options keep 44px touch targets on coarse pointers, as Combobox.',
      ],
    },
    theme: {
      items: [
        'Combobox tokens. Flags keep their colours in forced-colours mode; all text follows the system palette.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Suggested countries first',
          stack: true,
          stage: true,
          render: () => <BillingCountry />,
        },
        {
          title: 'Localised, no flags, limited list',
          stack: true,
          render: () => (
            <div style={{ width: '100%', maxWidth: 260 }}>
              <CountrySelect
                aria-label="Pays"
                locale="fr"
                countries={['BE', 'CA', 'CH', 'FR', 'LU', 'SN']}
                showFlag={false}
                defaultValue="CH"
                size="sm"
              />
            </div>
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Store the code the picker returns and format names at display time with `getCountryName`.',
        'Put the countries most of your people pick in `priority`.',
        'Pass `locale` from the interface language so names and sort order match.',
      ],
      dont: [
        'Do not store country names; they differ by locale and change over time.',
        'Do not rely on flags alone; some platforms draw letters instead of flags.',
      ],
    },
    related: {
      items: [
        { id: 'combobox', reason: 'The searchable select it is built on.' },
        { id: 'region-select', reason: 'The state or province of the chosen country.' },
        { id: 'phone-country-select', reason: 'For dial codes.' },
        { id: 'location-fields', reason: 'Country, region, city and postal code together.' },
        { id: 'world-map', reason: 'Shows values per country with the same codes.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['combobox'],
      notes: ['Uses only `Intl` for names; ships the 249 ISO 3166-1 alpha-2 codes.'],
    },
    testing: {
      unit: [
        '249 unique codes; names per locale; fallback to codes without Intl.DisplayNames or with an invalid locale.',
        'Flag emoji from regional indicators; locale-aware sort.',
        'Search by name and code, keyboard selection, suggested section, flags and codes shown.',
        'French names matched by English name; hidden flags; excluded codes; empty message.',
        'Lower-case and out-of-list values; custom names.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
