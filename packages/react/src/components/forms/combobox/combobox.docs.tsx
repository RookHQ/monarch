import { useState } from 'react';
import { Combobox, Field, FieldDescription, FieldLabel } from '@rookcom/monarch-react';
import type { ComboboxOption } from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const timezones = [
  { value: 'asia-kolkata', label: 'Asia/Kolkata' },
  { value: 'europe-london', label: 'Europe/London' },
  { value: 'america-new-york', label: 'America/New_York' },
  { value: 'asia-tokyo', label: 'Asia/Tokyo' },
  { value: 'utc', label: 'UTC', description: 'Coordinated Universal Time' },
];

const reviewers: ComboboxOption[] = [
  { value: 'u1', label: 'Priya Raman', hint: 'Design', group: 'In this team' },
  { value: 'u2', label: 'Jonas Weber', hint: 'Platform', group: 'In this team' },
  { value: 'u3', label: 'Amara Okafor', group: 'Everyone else' },
  { value: 'u4', label: 'Lucas Martin', group: 'Everyone else' },
  { value: 'u5', label: 'Mei Chen', group: 'Everyone else' },
];

const CITIES = ['Lisbon', 'London', 'Lyon', 'Lagos', 'Lima', 'Leeds', 'Lucknow', 'Ljubljana'];

function TimezonePicker() {
  const [value, setValue] = useState<string | null>('utc');
  return (
    <Field style={{ width: '100%', maxWidth: 300 }}>
      <FieldLabel>Time zone</FieldLabel>
      <Combobox options={timezones} value={value} onValueChange={setValue} placeholder="Search" />
    </Field>
  );
}

function ReviewersPicker() {
  const [values, setValues] = useState(['u1', 'u3']);
  return (
    <Field style={{ width: '100%', maxWidth: 340 }}>
      <FieldLabel>Reviewers</FieldLabel>
      <Combobox
        multiple
        options={reviewers}
        values={values}
        onValuesChange={setValues}
        sortByMatch
        placeholder="Add a reviewer"
      />
    </Field>
  );
}

/** A pretend server: answers after a short delay and honours the abort signal. */
function searchCities(query: string, { signal }: { signal: AbortSignal }) {
  return new Promise<ComboboxOption[]>((resolve, reject) => {
    const timer = setTimeout(() => {
      const q = query.toLowerCase();
      resolve(
        CITIES.filter((c) => c.toLowerCase().includes(q)).map((c) => ({ value: c, label: c })),
      );
    }, 300);
    signal.addEventListener('abort', () => {
      clearTimeout(timer);
      reject(new DOMException('Aborted', 'AbortError'));
    });
  });
}

function CityPicker() {
  const [value, setValue] = useState<string | null>(null);
  const [created, setCreated] = useState<ComboboxOption[]>([]);
  return (
    <Field style={{ width: '100%', maxWidth: 300 }}>
      <FieldLabel>City</FieldLabel>
      <Combobox
        onSearch={searchCities}
        value={value}
        onValueChange={setValue}
        valueOptions={created}
        onCreate={(q) => {
          const option = { value: q, label: q };
          setCreated((list) => [...list, option]);
          return option;
        }}
        messages={{ create: (q) => `Use “${q}”` }}
        clearOption={{ label: 'No city' }}
        placeholder="Start typing"
      />
      <FieldDescription>
        Results come from a server; typing aborts the last request.
      </FieldDescription>
    </Field>
  );
}

export default {
  id: 'combobox',
  title: 'Combobox',
  summary:
    'Searchable single or multiple select with groups, async search, a create option and an inline mode.',
  sections: {
    purpose: {
      summary:
        'A select people type into: the input is the combobox and the options are a listbox. It covers long static lists, server search with debounce and abort, several values as chips, and the inline list that EntityPicker and TimezoneSelect build on.',
      useWhen: [
        'The list is long enough that people search rather than scan.',
        'Options come from a server: pass `onSearch` and Combobox handles debounce, abort, loading and errors.',
        'Several values from a known list: `multiple`.',
        'A picker list inside a Popover, Dialog or Sheet: `inline`.',
      ],
      avoidWhen: [
        'A short, fixed list: use Select.',
        'A record picker opened from a button or chip: use EntityPicker.',
        'Free-form values with no list: use TagInput.',
      ],
    },
    designPrinciples: {
      items: [
        'The input keeps focus; options follow the `aria-activedescendant` pattern (WAI-ARIA APG combobox).',
        'In single mode the input always reflects the selection: unmatched text is reverted on blur and Escape.',
        'The product owns data and rules: search, creation and option content come in through props.',
        'Late server answers never overwrite newer ones: a new query aborts the last request.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Forwards its ref to the `<input>`. Native input attributes pass through, except `value`, `defaultValue`, `onChange`, `size` and `type`.',
        '`matchComboboxOption(option, query)` is the default filter, exported for products that extend it.',
        'Single mode uses `value` / `defaultValue` / `onValueChange`; `multiple` uses `values` / `defaultValues` / `onValuesChange`.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        '`onSearch(query, { signal })` returns a promise of options. Pass the signal to `fetch` so superseded requests are cancelled.',
        '`onCreate(query)` may return the new option (or a promise of it) to select it.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'size',
          values: ['sm', 'md', 'lg'],
          description: 'Height, matching Input sizes. Default `md`.',
        },
        {
          prop: 'multiple',
          values: ['false', 'true'],
          description: 'One value, or several shown as removable chips.',
        },
        {
          prop: 'inline',
          values: ['false', 'true'],
          description: 'List in a popover under the input, or always open under a search box.',
        },
      ],
    },
    states: {
      items: [
        { name: 'Open', attribute: 'aria-expanded="true"', description: 'The listbox is shown.' },
        {
          name: 'Active option',
          attribute: 'data-highlighted',
          description: 'The option the keyboard or pointer is on.',
        },
        {
          name: 'Selected',
          attribute: 'aria-selected="true"',
          description: 'Shows a check indicator.',
        },
        {
          name: 'Disabled option',
          attribute: 'aria-disabled="true"',
          description: 'Cannot be selected.',
        },
        {
          name: 'Loading',
          attribute: 'aria-busy="true" on the listbox',
          description: 'A small Spinner in the input; "Loading…" while there are no options yet.',
        },
        {
          name: 'Error',
          attribute: 'data-tone="danger" on the status',
          description: '`onSearch` rejected: a message with a "Try again" button.',
        },
        { name: 'Empty', description: '`emptyMessage` (default "No matches") in a status region.' },
        {
          name: 'Invalid',
          attribute: 'data-invalid',
          description: 'Danger border, from `invalid` or the surrounding Field.',
        },
        {
          name: 'Disabled',
          attribute: 'data-disabled',
          description: 'Input and toggle are disabled; chips lose their remove buttons.',
        },
      ],
    },
    interaction: {
      items: [
        'Typing opens the list and filters it (label, hint and keywords; `sortByMatch` ranks prefix matches first).',
        'Clicking an option selects it. Single mode closes the list; `multiple` toggles the value and keeps it open.',
        'With `onSearch`, opening searches the empty query at once; typing searches after `searchDebounce` ms (default 250).',
        'With `onCreate`, a "Create “text”" option appears when no label matches the text exactly.',
        '`clearOption` adds a first option that clears the selection.',
        '`limit` renders only the first matches and says how many are hidden.',
        'Chip remove buttons and Backspace remove values; each change is announced in a polite status region.',
        'The chevron button toggles the list and returns focus to the input.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'ArrowDown / ArrowUp',
          action: 'Opens the list on the selected option, then moves the active option.',
        },
        { keys: 'Alt+ArrowDown / Alt+ArrowUp', action: 'Opens without moving / closes the list.' },
        { keys: 'PageDown / PageUp', action: 'Moves ten options.' },
        { keys: 'Home / End', action: 'Jumps to the first or last option while open.' },
        {
          keys: 'Enter',
          action:
            'Selects (or toggles) the active option unless disabled; retries a failed search when nothing is listed.',
        },
        { keys: 'Escape', action: 'Closes the list and restores the selected label.' },
        {
          keys: 'Backspace',
          action:
            'On an empty input: removes the last chip (`multiple`) or clears the selection (`clearable`).',
        },
      ],
    },
    accessibility: {
      role: 'combobox, controlling a listbox',
      name: 'A FieldLabel in the surrounding Field, or `aria-label`. The listbox is named by `listLabel`, the Field label or `aria-label`.',
      items: [
        '`aria-autocomplete="list"`, `aria-expanded`, `aria-controls` and `aria-activedescendant` are set on the input.',
        'Grouped options sit in `role="group"` elements labelled by their heading.',
        'With `multiple` the listbox is `aria-multiselectable`; chips are a list with named remove buttons.',
        'Loading, empty, error and "showing N of M" messages are `role="status"`.',
        'Option media is decorative (`aria-hidden`); the label names the option.',
      ],
    },
    responsive: {
      items: [
        'The popover list matches the input width (at least 12rem) and limits its height to the space available.',
        'In `multiple` mode chips wrap onto new lines and the control grows; the text input keeps at least 5rem.',
        'Long labels wrap; hints truncate with an ellipsis at 40% of the row.',
      ],
    },
    theme: {
      items: [
        'Input, chip and menu colours are semantic tokens shared with Input, TagInput and DropdownMenu, so light, dark and more-contrast themes apply.',
        'The menu entrance animation comes from the foundation and respects reduced motion.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Searchable picker',
          stack: true,
          render: () => <TimezonePicker />,
          code: `<Field>
  <FieldLabel>Time zone</FieldLabel>
  <Combobox options={timezones} value={value} onValueChange={setValue} placeholder="Search" />
</Field>`,
        },
        {
          title: 'Multiple with groups and hints',
          stack: true,
          render: () => <ReviewersPicker />,
          code: `<Combobox
  multiple
  options={reviewers} // { value, label, hint, group }
  values={values}
  onValuesChange={setValues}
  sortByMatch
/>`,
        },
        {
          title: 'Server search with a typed fallback',
          stack: true,
          render: () => <CityPicker />,
          code: `<Combobox
  onSearch={(query, { signal }) => fetch(\`/api/cities?q=\${query}\`, { signal }).then((r) => r.json())}
  value={value}
  onValueChange={setValue}
  onCreate={(q) => ({ value: q, label: q })}
  messages={{ create: (q) => \`Use “\${q}”\` }}
  clearOption={{ label: 'No city' }}
/>`,
        },
        {
          title: 'Inline list',
          stack: true,
          render: () => (
            <div
              style={{
                width: '100%',
                maxWidth: 300,
                border: '1px solid var(--mn-color-border-subtle)',
                borderRadius: 'var(--mn-radius-lg)',
              }}
            >
              <Combobox
                inline
                aria-label="Search people"
                listLabel="People"
                options={reviewers}
                defaultValue="u2"
                placeholder="Search…"
              />
            </div>
          ),
          code: `<Combobox inline aria-label="Search people" listLabel="People" options={people} />`,
        },
      ],
    },
    doDont: {
      do: [
        'Use `onSearch` for server lists rather than wiring `inputValue` and `filter={() => true}` by hand.',
        'Pass `valueOptions` when a selected value may not be in the current results, so it keeps its label.',
        'Write an `emptyMessage` that says what to try next.',
      ],
      dont: [
        'Do not use Combobox for free text entry without `onCreate`; single mode reverts unmatched text.',
        'Do not render thousands of options: use `onSearch` or `limit`.',
      ],
    },
    related: {
      items: [
        { id: 'select', reason: 'Short lists without search.' },
        {
          id: 'entity-picker',
          reason: 'Button-triggered record picker built on the inline Combobox.',
        },
        { id: 'tag-input', reason: 'Free-form values as chips.' },
        { id: 'form-field', reason: 'Label and error wiring.' },
      ],
    },
    dependencies: {
      packages: ['@radix-ui/react-popover', 'react'],
      components: ['form-field', 'spinner'],
      notes: ['Uses `.mn-input` from text-field and `.mn-menu` from the shared menu styles.'],
    },
    testing: {
      unit: [
        'Filters, navigates with the keyboard (aria-activedescendant) and selects with Enter; Escape restores the selection.',
        'Grouped options in labelled groups; hint and keyword matching; prefix ranking; custom rendering; limit message; clear option.',
        'PageDown/PageUp, Home/End, Alt+ArrowDown/Up; Backspace clears.',
        'Multiple: toggles, stays open, chips with remove buttons, Backspace removes, announcements, disabled chips.',
        'Async: debounce, abort of superseded queries, late answers ignored, error with retry (button and Enter), minSearchLength, valueOptions labels.',
        'Create option selects what onCreate returns; hidden on an exact match. Inline mode.',
      ],
      axe: true,
      ssr: true,
      browser: ['Visual baselines: /__gallery/combobox', 'Cross-browser axe: /components/combobox'],
    },
  },
} satisfies ComponentDocs;
