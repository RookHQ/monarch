import { useState } from 'react';
import {
  AddressAutocomplete,
  toAddressValue,
  type AddressSuggestion,
  type AddressValue,
} from '@rookcom/monarch-react/data';
import type { ComponentDocs } from '../../../internal/component-docs.js';
// The scenario of the primary example, shared with iOS and Android (CROSS-PLATFORM.md section 8).
import fixture from './address-autocomplete.fixture.json';

// A pretend provider with a few neutral addresses. Monarch ships no vendor
// SDK: a product implements `search` (and `resolve`) against its own endpoint.
interface FixturePlace {
  id: string;
  label: string;
  description: string;
  address?: {
    line1: string;
    city: string;
    region?: string;
    postalCode: string;
    countryCode: string;
  };
}

const PLACES: AddressSuggestion[] = (fixture.data.places as FixturePlace[]).map(
  ({ id, label, description, address }) => ({
    id,
    label,
    description,
    ...(address !== undefined && {
      address: {
        line1: address.line1,
        city: address.city,
        ...(address.region !== undefined && { regionName: address.region }),
        postalCode: address.postalCode,
        country: address.countryCode,
      },
    }),
  }),
);

function wait<T>(ms: number, value: T, signal: AbortSignal) {
  return new Promise<T>((resolve, reject) => {
    const timer = setTimeout(() => resolve(value), ms);
    signal.addEventListener('abort', () => {
      clearTimeout(timer);
      reject(new DOMException('Aborted', 'AbortError'));
    });
  });
}

function searchPlaces(query: string, { signal }: { signal: AbortSignal }) {
  const q = query.toLowerCase();
  return wait(
    200,
    PLACES.filter((p) => `${p.label} ${p.description}`.toLowerCase().includes(q)),
    signal,
  );
}

function resolvePlace(suggestion: AddressSuggestion, { signal }: { signal: AbortSignal }) {
  return wait<Partial<AddressValue>>(
    400,
    { line1: suggestion.label, city: 'Berlin', postalCode: '10115', country: 'DE' },
    signal,
  );
}

function CheckoutDemo() {
  const [value, setValue] = useState<AddressValue>(toAddressValue());
  return (
    <div style={{ display: 'grid', gap: 12, width: '100%', maxWidth: 560 }}>
      <AddressAutocomplete
        defaultQuery={fixture.data.query}
        search={searchPlaces}
        resolve={resolvePlace}
        value={value}
        onValueChange={setValue}
        description={fixture.data.helper}
        required={fixture.data.required}
        messages={{ label: fixture.data.label, enterManually: fixture.data.enterManually }}
      />
    </div>
  );
}

function failingSearch(): Promise<AddressSuggestion[]> {
  return Promise.reject(new Error('Provider unavailable'));
}

export default {
  id: 'address-autocomplete',
  title: 'AddressAutocomplete',
  summary: 'Address field with suggestions from your own provider and a manual-entry fallback.',
  sections: {
    purpose: {
      summary:
        'Lets people find their address by typing a few characters, then fills in the street, city, region, postal code and country for them to check. Suggestions come from a `search(query, { signal })` callback, so the product chooses and pays for the provider and Monarch bundles no vendor SDK. Needed by: Rook Dots (CRM address fields, `src/components/crm/AddressFields.tsx`) and Rook Recruit (careers apply address blocks).',
      useWhen: [
        'Collecting a postal or billing address where a lookup service is available.',
        'You want people to review a structured address before saving it.',
      ],
      avoidWhen: [
        'Only a country, region or city is needed: use LocationFields.',
        'Searching places on a map or for venues: use a search field with your own results.',
      ],
    },
    designPrinciples: {
      items: [
        'The provider is a callback: no SDK, no API key in Monarch, no request Monarch did not get told to make.',
        'Manual entry is never more than one step away, because no provider knows every address.',
        'A picked suggestion fills the fields for review instead of saving silently.',
        'Late answers never overwrite newer ones: every request gets an AbortSignal.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        '`toAddressValue(partial)` fills a partial address with empty parts, for initial state.',
        'Suggestions that already carry `address` skip `resolve`; others are resolved after the pick.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        '`AddressValue` is `LocationValue` (country, region, regionName, city, cityId, postalCode) plus `line1` and `line2`.',
        'Pass `locationAdapter` to give the manual fields real region and city lists; by default countries come from `Intl.DisplayNames` and region and city are typed freely.',
        'Migrating from app copies: Rook Dots’ `AddressFields` (`value.address`, `city`, `state`, `postalCode`, `country`, patch-style `onChange`) maps to `line1`, `city`, `regionName`, `postalCode`, `country` and a whole-value `onValueChange`; its LocationFields adapter becomes `locationAdapter`.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'mode',
          values: ['search', 'manual'],
          description: 'The search field, or the street lines with LocationFields.',
        },
        {
          prop: 'allowManualEntry',
          values: ['true', 'false'],
          description: 'Offer manual entry from the list and below the field. Default true.',
        },
      ],
      notes: ['`locationFields` chooses which location parts the manual mode shows.'],
    },
    states: {
      items: [
        {
          name: 'Search',
          description: 'Only the search field shows.',
          attribute: 'data-mode="search"',
        },
        {
          name: 'Manual',
          description: 'Street lines and LocationFields show, filled after a pick.',
          attribute: 'data-mode="manual"',
        },
        {
          name: 'Too short',
          description: 'Fewer than `minQueryLength` characters typed.',
          attribute: 'data-state="short"',
        },
        {
          name: 'Loading',
          description: 'A search is running; the input is `aria-busy`.',
          attribute: 'data-state="loading"',
        },
        {
          name: 'Results',
          description: 'Suggestions are listed, or "No addresses found".',
          attribute: 'data-state="ready"',
        },
        {
          name: 'Error',
          description: '`search` rejected; an alert offers Try again.',
          attribute: 'data-state="error"',
        },
        {
          name: 'Resolving',
          description: 'The picked suggestion’s details are loading.',
          attribute: 'data-state="resolving"',
        },
        { name: 'Disabled', description: 'Every part is disabled.', attribute: 'data-disabled' },
      ],
    },
    interaction: {
      items: [
        'Typing waits `debounce` ms (default 300), then calls `search` once `minQueryLength` (default 3) characters are typed.',
        'A newer query aborts the previous request; late answers are ignored.',
        'Picking a suggestion resolves it if needed, fills the fields, switches to manual mode and moves focus to the street line.',
        '"Enter the address manually" (last option and a button) keeps what was typed as the street line.',
        '"Search for an address" returns to the search field and keeps the address.',
        'If `resolve` fails, an alert says so and people can pick another suggestion or enter it manually.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'ArrowDown / ArrowUp',
          action: 'Open the suggestions and move through them, wrapping at the ends.',
        },
        { keys: 'Alt+ArrowUp', action: 'Close the suggestions.' },
        { keys: 'Enter', action: 'Pick the highlighted suggestion, or retry after an error.' },
        { keys: 'Escape', action: 'Close the suggestions; press again to clear the text.' },
        { keys: 'Tab', action: 'Close the suggestions and move on.' },
      ],
      notes: [
        'Follows the WAI-ARIA APG editable combobox with list autocomplete; Home and End stay in the text.',
      ],
    },
    accessibility: {
      role: 'combobox with a listbox popup; manual mode is labelled inputs and a fieldset',
      name: 'The visible label "Address" (`messages.label`); the list is named "Address suggestions".',
      items: [
        'The highlighted suggestion is announced through `aria-activedescendant`.',
        'The number of results and "Address filled in" are announced in a polite status region.',
        'Search and resolve failures use `role="alert"`; Try again is a real button.',
        'Manual inputs use `autocomplete` (`address-line1`, `address-line2`) so browser autofill still works.',
        'Focus moves to the street line after a pick and back to the search field when returning.',
      ],
    },
    responsive: {
      items: [
        'Suggestions open in the flow under the field, so they never overflow a 390px screen.',
        'Street lines and location parts use an auto-fit grid: one column on narrow screens.',
        'Options and the mode buttons are at least 44px tall on coarse pointers.',
      ],
    },
    theme: {
      items: [
        'Built from Input, Field, Button, Spinner and LocationFields, so it follows light, dark and more-contrast themes.',
        'The popup uses raised surface and shadow tokens; errors use the danger tokens.',
        'Forced-colors mode marks the highlighted option with Highlight and keeps borders visible.',
      ],
    },
    examples: {
      items: [
        { title: 'Search, pick and review', stack: true, render: () => <CheckoutDemo /> },
        {
          title: 'Provider failure',
          stack: true,
          render: () => (
            <div style={{ width: '100%', maxWidth: 480 }}>
              <AddressAutocomplete search={failingSearch} description="Type three letters." />
            </div>
          ),
        },
        {
          title: 'Starting in manual mode',
          stack: true,
          render: () => (
            <div style={{ width: '100%', maxWidth: 560 }}>
              <AddressAutocomplete
                search={searchPlaces}
                defaultMode="manual"
                defaultValue={{ line1: '5 Hill Road', city: 'Leeds', country: 'GB' }}
                locationFields={['country', 'city', 'postalCode']}
              />
            </div>
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Call your own endpoint from `search` and pass the signal to `fetch`, so cancelled searches stop.',
        'Return a stable `id` per suggestion and, when you can, the structured `address`.',
        'Validate the saved address on the server.',
      ],
      dont: [
        'Do not put a provider API key in the browser bundle: proxy it through your server.',
        'Do not remove manual entry; some addresses are not in any provider.',
      ],
    },
    related: {
      items: [
        {
          id: 'location-fields',
          reason: 'The country, region, city and postal code of manual mode.',
        },
        { id: 'combobox', reason: 'Searchable select when the value must be one of the options.' },
        { id: 'phone-country-select', reason: 'Its country table names the default country list.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: [
        'button',
        'form-field',
        'location-fields',
        'phone-country-select',
        'spinner',
        'text-field',
      ],
    },
    testing: {
      unit: [
        'Waits for the minimum length, searches, picks with ArrowDown and Enter, fills the fields and focuses the street line.',
        'Calls `resolve` with the suggestion and a signal when it has no address.',
        'Empty results offer manual entry, which keeps the typed text; returning to search focuses the field.',
        'A failed search shows an alert, and Try again searches again.',
        'A newer query aborts the previous request; the input is busy meanwhile.',
        'Escape closes, then clears.',
        'Manual mode with a controlled value; a failed resolve shows an alert.',
        'Server render.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
