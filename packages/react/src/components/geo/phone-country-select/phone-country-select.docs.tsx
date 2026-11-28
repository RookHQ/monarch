import { useState } from 'react';
import {
  Field,
  FieldDescription,
  FieldLabel,
  Input,
  PhoneCountrySelect,
  getPhoneCountry,
  matchPhoneCountry,
} from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';
// The scenario of the primary example, shared with iOS and Android (CROSS-PLATFORM.md section 8).
import fixture from './phone-country-select.fixture.json';

function PhoneNumberDemo() {
  const [country, setCountry] = useState<string | null>(fixture.data.country);
  const [number, setNumber] = useState<string>(fixture.data.number);
  const dial = country ? getPhoneCountry(country)?.dialCode : undefined;
  return (
    <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8, width: '100%', maxWidth: 420 }}>
      <Field style={{ flex: '1 1 11rem' }}>
        <FieldLabel>{fixture.data.countryLabel}</FieldLabel>
        <PhoneCountrySelect
          value={country}
          onValueChange={setCountry}
          preferred={fixture.data.preferred}
        />
      </Field>
      <Field style={{ flex: '1 1 11rem' }}>
        <FieldLabel>{fixture.data.phoneLabel}</FieldLabel>
        <Input
          type="tel"
          inputMode="tel"
          autoComplete="tel-national"
          value={number}
          onChange={(e) => setNumber(e.target.value)}
        />
        <FieldDescription>
          {fixture.data.savedAs} {dial ?? '—'} {number || fixture.data.numberPlaceholder}
        </FieldDescription>
      </Field>
    </div>
  );
}

function PasteDemo() {
  const [text, setText] = useState('+1 242 555 0100');
  const match = matchPhoneCountry(text);
  return (
    <Field style={{ width: '100%', maxWidth: 320 }}>
      <FieldLabel>International number</FieldLabel>
      <Input value={text} onChange={(e) => setText(e.target.value)} />
      <FieldDescription>
        Country: {match ? `${match.code} (${match.dialCode})` : 'not recognised'}
      </FieldDescription>
    </Field>
  );
}

export default {
  id: 'phone-country-select',
  title: 'PhoneCountrySelect',
  summary: 'Searchable picker for a phone number’s country calling code.',
  sections: {
    purpose: {
      summary:
        'Picks the country and calling code that go in front of a phone number, with names in the reader’s language from `Intl.DisplayNames` and one shared table of calling codes (`PHONE_COUNTRIES`). Needed by: Rook Dots (CRM party fields, `src/utils/countryCodes.ts`) and Rook Recruit (careers apply form phone field).',
      useWhen: [
        'A phone number field needs its country calling code chosen separately.',
        'You need to split a stored international number back into country and digits (`matchPhoneCountry`).',
      ],
      avoidWhen: [
        'Choosing a country for anything other than a phone number: use a country select.',
        'The number is always domestic: a single `type="tel"` input is enough.',
      ],
    },
    designPrinciples: {
      items: [
        'One calling-code table for the whole product, so the picker and any parser agree.',
        'Names come from the platform (`Intl.DisplayNames`), never from a bundled translation list.',
        'Search by what people know: the country name, its ISO code or the digits they dial.',
        'A phone number always needs a country, so clearing is off by default.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        '`PHONE_COUNTRIES` lists every ISO 3166-1 country and territory with its calling code; countries that share a code carry `leadingDigits` (for example the Bahamas under +1 242).',
        '`getPhoneCountry(code)` looks a code up; `matchPhoneCountry(text, preferred?)` finds the country of a number that starts with "+" or "00".',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Accepts every Combobox prop except the option, selection and search props it sets itself (`size`, `invalid`, `placeholder`, `disabled`, `open`, `limit` and the rest pass through).',
        '`onValueChange(code, dialCode)` gives both the ISO code to store and the calling code to show.',
        'Migrating from app copies: Rook Dots’ `<select>` of `countries` (value = `dial_code`) becomes `value` = ISO code plus `getPhoneCountry(code).dialCode`; its `splitPhone` helper becomes `matchPhoneCountry`. Rook Recruit’s `PhoneInput` combobox (`dialCodes`, `defaultCountry`, `searchLabel`, `hasError`) maps to `countries`, `defaultValue`, `aria-label` and `invalid`.',
      ],
    },
    variants: {
      items: [
        { prop: 'size', values: ['sm', 'md', 'lg'], description: 'Control height, as Combobox.' },
        {
          prop: 'showFlags',
          values: ['true', 'false'],
          description: 'Show the flag emoji beside each country. Default true.',
        },
      ],
      notes: ['`preferred` adds a "Suggested" group above "All countries".'],
    },
    states: {
      items: [
        {
          name: 'Open',
          description: 'The list of countries is showing.',
          attribute: 'aria-expanded',
        },
        { name: 'Invalid', description: 'From `invalid` or the Field.', attribute: 'data-invalid' },
        {
          name: 'Disabled',
          description: 'From `disabled` or the Field.',
          attribute: 'data-disabled',
        },
        { name: 'No match', description: 'The search matches no country; shows `messages.empty`.' },
      ],
    },
    interaction: {
      items: [
        'Typing filters by country name, ISO code or calling code ("44" and "+44" both find the United Kingdom).',
        'Matches whose name starts with the query come first (`sortByMatch` is on).',
        'Choosing closes the list and shows "Country (+code)" in the field.',
        'Controlled with `value` + `onValueChange`, or uncontrolled with `defaultValue`. Codes in any case are accepted.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'ArrowDown / ArrowUp', action: 'Open the list and move through the countries.' },
        { keys: 'Home / End', action: 'First or last country while the list is open.' },
        { keys: 'PageDown / PageUp', action: 'Move ten countries.' },
        { keys: 'Enter', action: 'Choose the highlighted country.' },
        { keys: 'Escape', action: 'Close the list and restore the chosen country.' },
      ],
      notes: ['The full Combobox keyboard model (WAI-ARIA APG combobox with a listbox popup).'],
    },
    accessibility: {
      role: 'combobox with a listbox popup',
      name: 'The Field label, or `aria-label`. The list is named "Country calling codes" (`messages.listLabel`).',
      items: [
        'Flags are decorative (`aria-hidden`); each option is named by the country and its code.',
        'The highlighted option is announced through `aria-activedescendant`.',
        'Group headings are given to the list as option groups.',
      ],
    },
    responsive: {
      items: [
        'Fills its container; place it beside the number input in a wrapping row so the two stack at 390px.',
        'The control is at least 44px tall on coarse pointers.',
      ],
    },
    theme: {
      items: [
        'Inherits Combobox styling, so it follows light, dark and more-contrast themes and the product accent.',
        'Flags are emoji and keep their own colours; forced-colors mode keeps a visible border.',
      ],
    },
    examples: {
      items: [
        { title: 'Beside a phone number', stack: true, render: () => <PhoneNumberDemo /> },
        {
          title: 'Restricted list without flags, in French',
          render: () => (
            <Field style={{ width: '100%', maxWidth: 280 }}>
              <FieldLabel>Indicatif</FieldLabel>
              <PhoneCountrySelect
                countries={['FR', 'BE', 'CH', 'CA', 'LU']}
                defaultValue="FR"
                locale="fr"
                showFlags={false}
                messages={{ empty: 'Aucun pays', listLabel: 'Indicatifs' }}
              />
            </Field>
          ),
        },
        { title: 'Find the country of a pasted number', render: () => <PasteDemo /> },
      ],
    },
    doDont: {
      do: [
        'Store the ISO code and the national digits; build the E.164 number from `getPhoneCountry(code).dialCode`.',
        'Put the visitor’s likely country in `preferred`.',
        'Give it a visible label such as "Country code".',
      ],
      dont: [
        'Do not store only the calling code: +1 is shared by many countries.',
        'Do not keep your own copy of the calling-code table in the app.',
      ],
    },
    related: {
      items: [
        { id: 'combobox', reason: 'The control it is built on.' },
        { id: 'phone-input', reason: 'A full phone number field that uses the same data.' },
        {
          id: 'country-select',
          reason: 'Choosing a country for anything other than a phone number.',
        },
      ],
    },
    dependencies: { packages: ['react'], components: ['combobox'] },
    testing: {
      unit: [
        'Takes its name from the Field label and shows the selected country with its code.',
        'Finds a country by its calling code digits and selects it with Enter; reports the ISO code and calling code.',
        'Lists preferred countries first under "Suggested", the rest alphabetically under "All countries".',
        'Translated names and messages; empty state; Escape closes.',
        'Data table has unique ISO codes and well-formed calling codes; `matchPhoneCountry` resolves shared codes by leading digits and preference.',
        'Server render shows the selected country.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
