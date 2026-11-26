import { useState } from 'react';
import { Field, FieldDescription, FieldLabel } from '@rookcom/monarch-react';
import { LanguageSelect } from '@rookcom/monarch-react/data';
import type { ComponentDocs } from '../../../internal/component-docs.js';

function InterfaceLanguage() {
  const [language, setLanguage] = useState<string | null>('en');
  return (
    <Field style={{ width: '100%', maxWidth: 320 }}>
      <FieldLabel>Interface language</FieldLabel>
      <LanguageSelect
        value={language}
        onValueChange={setLanguage}
        languages={['ar', 'de', 'en', 'es', 'fr', 'hi', 'ja', 'pt-BR', 'zh-Hans']}
        priority={['en', 'es']}
        showTag
      />
      <FieldDescription>Each language is listed in its own name.</FieldDescription>
    </Field>
  );
}

export default {
  id: 'language-select',
  title: 'LanguageSelect',
  summary:
    'Searchable BCP 47 language picker that lists each language by its own name, with the name in the interface locale below.',
  sections: {
    purpose: {
      summary:
        'Chooses a language and stores its BCP 47 tag. Each option shows the endonym ("Deutsch", "日本語"), so people find their own language even when the interface is in one they do not read, with the name in the interface locale underneath. Names come from `Intl.DisplayNames`. Import from `@rookcom/monarch-react/data`. Needed by: the Rook marketing site footer’s region and language picker (`rook/src/components/footer.tsx`).',
      useWhen: [
        'Choosing the interface, content, document or spoken language.',
        'A product is translated into some languages: pass them as `languages`.',
      ],
      avoidWhen: [
        'Choosing a country: use CountrySelect; languages and countries are different lists.',
        'Two or three languages: a RadioGroup or SegmentedControl is quicker.',
      ],
    },
    designPrinciples: {
      items: [
        'Endonyms first: the person changing the language may not read the current one.',
        'Each name is marked with its `lang`, so screen readers pronounce it correctly.',
        'Tags are canonicalised (`EN-us` → `en-US`) and invalid tags are dropped.',
        'Same render on server and client: the default interface locale is `en`.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Built on Combobox and accepts its input props. Forwards its ref to the input.',
        'Helpers: `LANGUAGE_TAGS`, `getLanguageName(tag, locale?)` (endonym without a locale), `canonicalLanguageTag(tag)`, `listLanguages({ locale, tags })`.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Without `Intl.DisplayNames` names fall back to the tag.',
        'Migrating from a shadcn/ui Select or Popover + Command language menu: `value`/`onValueChange` keep their names; pass the tags you supported as `languages` instead of building items by hand.',
      ],
    },
    variants: {
      items: [
        { prop: 'size', values: ['sm', 'md', 'lg'], description: 'Height, as Combobox.' },
        {
          prop: 'labelStyle',
          values: ['endonym', 'localized'],
          description: 'Which name leads each option; the other is shown below it.',
        },
        {
          prop: 'showTag',
          values: ['true', 'false'],
          description: 'BCP 47 tag at the end of each option.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Sections',
          attribute: 'role="group"',
          description:
            '"Suggested" and "All languages" while nothing is typed and `priority` is set.',
        },
        { name: 'Searching', description: 'A flat list, best matches first.' },
        { name: 'Empty', attribute: 'role="status"', description: '"No languages match".' },
        { name: 'Selected / invalid / disabled', description: 'As Combobox.' },
      ],
    },
    interaction: {
      items: [
        'Typing filters by every word against the endonym, the localised name, the tag and the English name: "german", "deutsch" and "de" all find German.',
        'The list is sorted by the name in the interface locale.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'ArrowDown / ArrowUp', action: 'Opens the list, then moves.' },
        {
          keys: 'PageDown / PageUp, Home / End',
          action: 'Moves ten languages, or to the first or last.',
        },
        { keys: 'Enter', action: 'Selects the active language.' },
        { keys: 'Escape', action: 'Closes and restores the selected name.' },
      ],
    },
    accessibility: {
      role: 'combobox, controlling a listbox',
      name: 'A FieldLabel or `aria-label`.',
      items: [
        'The endonym carries `lang` set to its tag and the localised name carries the interface locale (WCAG 3.1.2 Language of Parts).',
        'Inherits Combobox’s APG keyboard and `aria-activedescendant` support.',
      ],
    },
    responsive: {
      items: [
        'Fills its container; the list matches the field width and names wrap.',
        'Options keep 44px touch targets on coarse pointers, as Combobox.',
      ],
    },
    theme: {
      items: ['Combobox tokens; no colours of its own.'],
    },
    examples: {
      items: [
        { title: 'Endonyms with suggestions', stack: true, render: () => <InterfaceLanguage /> },
        {
          title: 'Localised names first',
          stack: true,
          render: () => (
            <div style={{ width: '100%', maxWidth: 260 }}>
              <LanguageSelect
                aria-label="Document language"
                labelStyle="localized"
                defaultValue="fr"
                size="sm"
              />
            </div>
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Store the tag and pass it to `Intl` and the `lang` attribute.',
        'Suggest the browser’s languages (`navigator.languages`, read after mount) in `priority`.',
      ],
      dont: [
        'Do not use flags for languages; a flag is a country.',
        'Do not list languages the product is not translated into for the interface language.',
      ],
    },
    related: {
      items: [
        { id: 'combobox', reason: 'The searchable select it is built on.' },
        { id: 'country-select', reason: 'For countries.' },
        { id: 'timezone-select', reason: 'Often set beside the language in preferences.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['combobox'],
      notes: ['Uses only `Intl`; ships a default list of about 80 tags.'],
    },
    testing: {
      unit: [
        'Tags canonicalised and invalid ones rejected; the default list is canonical.',
        'Capitalised endonyms and localised names; sorted list.',
        'Options mark the endonym and the localised name with `lang`; search by English name and by script; keyboard selection.',
        'Suggested section, localised labels and tags.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
