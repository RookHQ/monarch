import { useState } from 'react';
import {
  LocationFields,
  type LocationAdapter,
  type LocationCityOption,
  type LocationValue,
} from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';

// Demo adapter with a handful of entries. Monarch ships no geographic data:
// a product implements LocationAdapter against its own API.
const REGIONS: Record<string, { label: string; options: { value: string; label: string }[] }> = {
  CA: {
    label: 'Province',
    options: [
      { value: 'CA-BC', label: 'British Columbia' },
      { value: 'CA-ON', label: 'Ontario' },
      { value: 'CA-QC', label: 'Quebec' },
    ],
  },
  IN: {
    label: 'State / Union Territory',
    options: [
      { value: 'IN-KA', label: 'Karnataka' },
      { value: 'IN-MH', label: 'Maharashtra' },
      { value: 'IN-TN', label: 'Tamil Nadu' },
    ],
  },
  AE: {
    label: 'Emirate',
    options: [
      { value: 'AE-AZ', label: 'Abu Dhabi' },
      { value: 'AE-DU', label: 'Dubai' },
    ],
  },
  SG: { label: 'Region', options: [] },
};
const CITIES: LocationCityOption[] = [
  { value: 'ca-van', label: 'Vancouver', region: { value: 'CA-BC', label: 'British Columbia' } },
  { value: 'ca-tor', label: 'Toronto', region: { value: 'CA-ON', label: 'Ontario' } },
  { value: 'ca-ott', label: 'Ottawa', region: { value: 'CA-ON', label: 'Ontario' } },
  { value: 'ca-mtl', label: 'Montreal', region: { value: 'CA-QC', label: 'Quebec' } },
  { value: 'in-blr', label: 'Bengaluru', region: { value: 'IN-KA', label: 'Karnataka' } },
  { value: 'in-bom', label: 'Mumbai', region: { value: 'IN-MH', label: 'Maharashtra' } },
  { value: 'in-maa', label: 'Chennai', region: { value: 'IN-TN', label: 'Tamil Nadu' } },
  { value: 'ae-dxb', label: 'Dubai', region: { value: 'AE-DU', label: 'Dubai' } },
  { value: 'ae-auh', label: 'Abu Dhabi', region: { value: 'AE-AZ', label: 'Abu Dhabi' } },
];
// Countries and regions answer at once so the page renders settled; city search
// waits a little so the loading state is visible while typing.
const wait = <T,>(value: T, signal: AbortSignal, ms = 0) =>
  new Promise<T>((resolve, reject) => {
    const t = setTimeout(() => resolve(value), ms);
    signal.addEventListener('abort', () => {
      clearTimeout(t);
      reject(new DOMException('Aborted', 'AbortError'));
    });
  });

const demoAdapter: LocationAdapter = {
  countries: (signal) =>
    wait(
      [
        { value: 'CA', label: 'Canada' },
        { value: 'IN', label: 'India' },
        { value: 'SG', label: 'Singapore' },
        { value: 'AE', label: 'United Arab Emirates' },
      ],
      signal,
    ),
  regions: (country, signal) => wait(REGIONS[country] ?? { options: [] }, signal),
  cities: ({ country, region, query }, signal) =>
    wait(
      CITIES.filter(
        (c) =>
          c.value.startsWith(country.toLowerCase()) &&
          (!region || c.region?.value === region) &&
          c.label.toLowerCase().includes(query.toLowerCase()),
      ).map((c) => (region ? c : { ...c, description: c.region?.label ?? '' })),
      signal,
      300,
    ),
};

const failingAdapter: LocationAdapter = {
  countries: () => Promise.reject(new Error('Service unavailable')),
};

function ControlledDemo() {
  const [value, setValue] = useState<LocationValue>({
    country: 'CA',
    region: 'CA-ON',
    regionName: 'Ontario',
    city: 'Toronto',
    cityId: 'ca-tor',
    postalCode: '',
  });
  return (
    <div style={{ display: 'grid', gap: 12, width: '100%', maxWidth: 640 }}>
      <LocationFields
        adapter={demoAdapter}
        legend="Billing address"
        value={value}
        onValueChange={setValue}
        required={['country', 'city']}
      />
      <pre style={{ margin: 0, fontSize: 12, overflowX: 'auto' }}>
        {JSON.stringify(value, null, 2)}
      </pre>
    </div>
  );
}

export default {
  id: 'location-fields',
  title: 'LocationFields',
  summary: 'Country, region, city and postal code as one composite, with the data source injected.',
  sections: {
    purpose: {
      summary:
        'The address part every form re-implements: a searchable country, a region list that uses each country’s own word for it, a server-searched city that can also be typed freely, and a postal code. The lists come from an adapter the product provides.',
      useWhen: [
        'Any form that takes a postal or business address.',
        'The product has a geographic data source and wants stored codes (country, region, city ids) beside the text.',
      ],
      avoidWhen: [
        'Only a country is needed: a single Combobox of countries is simpler.',
        'A street-level autocomplete from a maps provider is wanted; that is a different, provider-specific control.',
      ],
    },
    designPrinciples: {
      items: [
        'No data, no network: Monarch ships no geographic lists and makes no requests. `adapter` is the only source.',
        'Codes and text together: the value stores each code beside its label, so the server can verify and canonicalise it.',
        'Local words: the region label comes from the adapter ("Province", "Emirate"), never a hard-coded "State".',
        'Never a dead end: a city missing from the data can be typed, a country without regions gets a free-text field, and a failed list offers Try again.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Renders a `<fieldset class="mn-location-fields">` with a Field per part. The ref goes to the fieldset.',
        '`LocationAdapter` has `countries(signal)`, optional `regions(country, signal)` and optional `cities({ country, region, query }, signal)`. Omit `regions` or `cities` for free-text fields.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        '`onValueChange(value, changed)` receives the whole `LocationValue` and which field changed. Changing the country resets every other part; changing the region resets the city.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'fields',
          values: ["('country' | 'region' | 'city' | 'postalCode')[]"],
          description: 'Which parts to show, in order. Default all four.',
        },
        {
          prop: 'allowCustomCity',
          values: ['true', 'false'],
          description: 'Offer the typed text as a city when it is not in the list. Default true.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Loading',
          description: 'A list is loading: the combobox shows a spinner and "Loading…".',
        },
        {
          name: 'Waiting for a country',
          description:
            'Region and city are disabled with the placeholder "Choose a country first".',
        },
        {
          name: 'Load failed',
          description: 'The field is replaced by a `role="alert"` message with a Try again button.',
        },
        {
          name: 'Invalid',
          attribute: 'data-invalid on the Field, aria-invalid="true" on the control',
          description: 'A field with a message in `errors` shows it under the control.',
        },
        {
          name: 'Disabled',
          attribute: 'disabled on the fieldset',
          description: 'Every part is disabled.',
        },
      ],
    },
    interaction: {
      items: [
        'Countries load once on mount. Choosing a country loads its regions and clears the region, city and postal code.',
        'A country with no regions in the data gets an optional free-text region field.',
        'Cities are searched after `cityDebounce` ms (default 250) once `cityMinQuery` letters are typed (default 2), or listed at once when a region is chosen. Superseded requests are aborted.',
        'Choosing a city before a region fills in the region the city belongs to.',
        'A city not in the list is offered as "Not in the list: use as typed" and stored with `cityId: null`.',
        'Controlled with `value` + `onValueChange`, or uncontrolled with `defaultValue`.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Moves through country, region, city and postal code in order.' },
        { keys: 'ArrowDown / ArrowUp', action: 'Open and move through a list (Combobox).' },
        { keys: 'Enter', action: 'Choose the highlighted option.' },
        { keys: 'Escape', action: 'Close the list and restore the chosen text.' },
      ],
      notes: ['Each list is a Monarch Combobox and has its full keyboard model.'],
    },
    accessibility: {
      role: 'group (fieldset) of comboboxes and text inputs',
      name: 'The `legend`, or an `aria-label` on the fieldset; each part has its own visible label.',
      items: [
        'Each part is a Field, so labels, required state and errors are wired with `htmlFor`, `aria-required`, `aria-invalid` and `aria-describedby`.',
        'Load failures are announced with `role="alert"` and the retry is a real button.',
        'The region label follows the country, so screen reader users hear the same word sighted users see.',
        'Free-text parts use `autocomplete` (`address-level1`, `address-level2`, `postal-code`).',
      ],
    },
    responsive: {
      items: ['An auto-fit grid: two columns from about 30rem, one column below, down to 320px.'],
    },
    theme: {
      items: [
        'Built from Combobox, Input, Field and Button, so it follows light, dark and more-contrast themes and the product accent.',
        'The failure message uses the danger semantic tokens.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Controlled address',
          stack: true,
          render: () => <ControlledDemo />,
          code: `const adapter: LocationAdapter = {
  countries: (signal) => api.countries({ signal }),
  regions: (country, signal) => api.regions(country, { signal }),
  cities: (query, signal) => api.searchCities(query, { signal }),
};

<LocationFields
  adapter={adapter}
  legend="Billing address"
  value={value}
  onValueChange={setValue}
  required={['country', 'city']}
/>`,
        },
        {
          title: 'Subset and failure',
          stack: true,
          render: () => (
            <div style={{ display: 'grid', gap: 24, width: '100%', maxWidth: 640 }}>
              <LocationFields
                adapter={demoAdapter}
                aria-label="Office location"
                fields={['country', 'city']}
                defaultValue={{ country: 'IN' }}
              />
              <LocationFields
                adapter={failingAdapter}
                aria-label="Shipping address"
                fields={['country']}
              />
            </div>
          ),
          code: `<LocationFields adapter={adapter} fields={['country', 'city']} />
<LocationFields adapter={failingAdapter} fields={['country']} />`,
        },
      ],
    },
    doDont: {
      do: [
        'Pass the AbortSignal to your requests so superseded searches stop.',
        'Store the codes and verify them on the server when the form is saved.',
        'Return each country’s own word for its regions from `regions`.',
      ],
      dont: [
        'Do not bundle a copy of geographic lists into the app to avoid the adapter; it drifts out of date.',
        'Do not require a region for countries that have none.',
      ],
    },
    related: {
      items: [
        { id: 'combobox', reason: 'Each list part; use it alone for a single country select.' },
        { id: 'form-field', reason: 'Label, required and error wiring for each part.' },
        { id: 'text-field', reason: 'The free-text parts.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['button', 'combobox', 'form-field', 'text-field'],
    },
    testing: {
      unit: [
        'Loads countries, then regions under the adapter’s word; choosing reports the value and the changed field.',
        'Region and city wait for a country.',
        'Cities are searched after the minimum letters with one debounced request, and a city fills its region.',
        'A chosen region lists its cities at once.',
        'A city typed freely is kept with `cityId: null`.',
        'A country without regions gets a free-text region.',
        'A failed list shows an alert and Try again reloads it.',
        'Changing the country clears the other parts (controlled).',
        'Required, errors, custom labels and a subset of fields; disabled; server render calls no adapter method.',
      ],
      axe: true,
      ssr: true,
      browser: ['Visual baselines: /__gallery', 'Cross-browser axe: /components/location-fields'],
    },
  },
} satisfies ComponentDocs;
