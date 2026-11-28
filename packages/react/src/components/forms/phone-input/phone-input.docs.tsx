import { useState } from 'react';
import { Field, FieldDescription, FieldLabel, PhoneInput } from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';
// The scenario of the primary example, shared with iOS and Android (CROSS-PLATFORM.md section 8).
import fixture from './phone-input.fixture.json';

function ContactDemo() {
  const [phone, setPhone] = useState<string | null>(fixture.data.value);
  return (
    <Field style={{ width: '100%', maxWidth: 420 }}>
      <FieldLabel>{fixture.data.label}</FieldLabel>
      <PhoneInput value={phone} onValueChange={setPhone} preferred={fixture.data.preferred} />
      <FieldDescription>
        {fixture.data.storedAs} {phone ?? fixture.data.nothingYet}.
      </FieldDescription>
    </Field>
  );
}

export default {
  id: 'phone-input',
  title: 'PhoneInput',
  summary: 'Country calling code plus national number, with one E.164 value out.',
  sections: {
    purpose: {
      summary:
        'Enters a phone number the way people know it (their country and local number) and stores one unambiguous E.164 string (`+442079460958`). The country part is PhoneCountrySelect. Needed by: rookdots (`components/crm/PartyFields.tsx`, leads), rookrecruit (`components/careers/apply/FieldRenderer.tsx`), rookcalendar, rookchat and rooktickets frontends, which use a bare `type="tel"` with no country today. There is no shadcn/ui phone copy in the apps.',
      useWhen: [
        'Contact, profile, booking and application forms that store a callable number.',
        'Numbers from many countries that must be stored in one format.',
      ],
      avoidWhen: [
        'One-time codes sent by SMS: use OtpPinInput.',
        'Only picking a calling code: use PhoneCountrySelect.',
        'Full formatting and carrier validation: this component checks length only; validate on the server with a phone library.',
      ],
    },
    designPrinciples: {
      items: [
        'One value: E.164 out, whatever was typed ("020 7946 0958", "(555) 010-0123").',
        'Pasting works: a full international number picks its country and splits the digits.',
        'Commit, do not stream: the value is sent on blur, Enter or a country change.',
        'Never save a guess: an incomplete number stays in the box, is flagged and explained.',
        'No phone library: length checks only (8–15 digits); shared codes (+1, +7, +44) are resolved by leading digits.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Renders `.mn-phone-input`: a PhoneCountrySelect and a `type="tel"` input. The ref and native input props go to the number input; `className` and `style` go to the wrapper.',
        'With `name`, a hidden input submits the E.164 value.',
        'Helpers: `toE164(dialCode, national, country?)`, `parsePhoneNumber(phone, preferred?)`, `isValidE164(value)`. Country data comes from PhoneCountrySelect (`PHONE_COUNTRIES`, `matchPhoneCountry`).',
        '`PHONE_INPUT_MESSAGES` holds the English defaults for `messages`.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        '`defaultCountry` is "US" so the +1 shared code has a sensible owner; set it from the user’s profile or locale.',
        'One leading trunk 0 is dropped ("020…" → "+4420…"), except for Italy, San Marino and Vatican City where it is part of the number.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'size',
          values: ['sm', 'md', 'lg'],
          description: 'Height of both parts from `--mn-control-*`.',
        },
        {
          prop: 'preferred',
          values: ['string[]'],
          description: 'Countries listed first in the picker under "Suggested".',
        },
        {
          prop: 'countries',
          values: ['string[]'],
          description: 'Limit the picker to the countries you serve.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Incomplete or too long',
          attribute: 'aria-invalid="true", data-invalid',
          description: '"Enter a complete phone number." under the field; the text is kept.',
        },
        {
          name: 'Disabled',
          attribute: 'data-disabled',
          description: 'Both parts disabled, also from a Field.',
        },
      ],
    },
    interaction: {
      items: [
        'Type the local number; on blur or Enter it is joined with the calling code.',
        'Paste or type "+44 20 7946 0958" or "0044…": the country switches and the national digits remain.',
        'Changing the country re-sends the number with the new code.',
        'A +1 number starting 242 switches to the Bahamas (shared codes by leading digits).',
        'Escape restores the last saved number.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Country picker, then the number.' },
        {
          keys: 'Typing in the picker',
          action: 'Searches by country name, ISO code or digits ("44", "+44").',
        },
        {
          keys: 'ArrowDown / Enter / Escape (picker)',
          action: 'Combobox keys: open, choose, close.',
        },
        { keys: 'Enter (number)', action: 'Commit the number.' },
        { keys: 'Escape (number)', action: 'Restore the last value.' },
      ],
    },
    accessibility: {
      role: 'combobox (country) and textbox (number)',
      name: 'The number takes the FieldLabel or `aria-label`; the picker is named "Country code" (`messages.country`).',
      items: [
        'The picker has its own id, so the Field label points only at the number.',
        'Errors are `role="alert"` and joined to the number’s `aria-describedby`.',
        '`type="tel"`, `inputMode="tel"` and `autoComplete="tel-national"` bring up the phone keypad and autofill.',
        'The number is `dir="ltr"`, so digits read correctly on right-to-left pages.',
      ],
    },
    responsive: {
      items: [
        'Picker and number sit side by side; below about 22rem the number wraps under the picker, so nothing overflows at 390px.',
        'Both parts are at least 44px tall on coarse pointers.',
      ],
    },
    theme: {
      items: [
        'Both parts use the Input tokens, so they match other fields in every theme.',
        'Forced colours: a 2px border on an invalid number; the message uses CanvasText.',
      ],
    },
    examples: {
      items: [
        {
          title: 'In a Field with suggested countries',
          stack: true,
          render: () => <ContactDemo />,
        },
        {
          title: 'Limited countries, small',
          stack: true,
          render: () => (
            <PhoneInput
              aria-label="Mobile"
              size="sm"
              countries={['IN', 'AE', 'SG']}
              defaultCountry="IN"
              style={{ maxWidth: 420 }}
            />
          ),
        },
        {
          title: 'Disabled',
          stack: true,
          render: () => (
            <PhoneInput
              aria-label="Office phone"
              disabled
              defaultValue="+15550100123"
              style={{ maxWidth: 420 }}
            />
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Store the E.164 value and format it for display where it is shown.',
        'Set `defaultCountry` from the user or organisation.',
        'Validate the number on the server before sending messages to it.',
      ],
      dont: [
        'Do not ask for the country in a separate field; the picker is part of the input.',
        'Do not strip the + when storing; E.164 starts with it.',
      ],
    },
    related: {
      items: [
        { id: 'phone-country-select', reason: 'The country calling-code picker inside.' },
        { id: 'otp-pin-input', reason: 'Codes sent to the number.' },
        { id: 'text-field', reason: 'The number input styling.' },
        { id: 'form-field', reason: 'Label, description and error wiring.' },
      ],
    },
    dependencies: { packages: ['react'], components: ['form-field', 'phone-country-select'] },
    testing: {
      unit: [
        'toE164 (separators, trunk 0, Italy), isValidE164 and parsePhoneNumber (00 prefix, shared +1 codes).',
        'Number labelled by the Field, picker named separately, different ids.',
        'Emits E.164 on blur only, with details.',
        'Pasted international number switches country and splits; hidden input.',
        'Changing the country by keyboard re-emits.',
        'Incomplete number flagged with an alert; Escape restores.',
        'Clearing to null; controlled value updates country and number.',
        'Disabled from Field; translated picker name.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
