import { useState } from 'react';
import { Field, FieldDescription, FieldLabel, MultiSelect } from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const labels = [
  { value: 'bug', label: 'Bug' },
  { value: 'design', label: 'Design' },
  { value: 'docs', label: 'Documentation' },
  { value: 'perf', label: 'Performance' },
  { value: 'security', label: 'Security' },
  { value: 'support', label: 'Support' },
];

function Controlled() {
  const [value, setValue] = useState<string[]>(['design']);
  return (
    <Field>
      <FieldLabel>Labels</FieldLabel>
      <FieldDescription>Type to filter. Picked labels show as chips.</FieldDescription>
      <MultiSelect
        options={labels}
        value={value}
        onValueChange={setValue}
        placeholder="Add labels"
      />
    </Field>
  );
}

export default {
  id: 'multi-select',
  title: 'MultiSelect',
  summary: 'Pick several values from a searchable list; picks show as removable chips.',
  sections: {
    purpose: {
      summary:
        'Selects several values from a list too long for checkboxes, with type-to-filter, chips for the picked values and native form submission. Needed by: Rook Tickets (`components/ui/picker.tsx` `multiple` mode, field and condition editors, reports filters), Rook Stride (issue pickers), Rook ID (AppAccessPicker), Rook People (EmployeePicker) and Monarch FieldRenderer (`multi-select` questions).',
      useWhen: [
        'More than about eight options, or options loaded from a server, and several can be picked.',
        'Labels, people, tags or apps chosen from a known list.',
      ],
      avoidWhen: [
        'A short list where every option should be visible: use CheckboxGroup.',
        'Free-form values that are not in a list: use TagInput.',
        'Only one value can be picked: use Combobox or Select.',
      ],
    },
    designPrinciples: {
      items: [
        'One searchable combobox, not a new widget: MultiSelect is Combobox in `multiple` mode, so search, keyboard and announcements match everywhere.',
        'Picked values are visible as chips in the field and can be removed with the mouse or Backspace.',
        'The list stays open between picks; the search text clears after each pick.',
        'A limit is shown as a count, not only enforced silently.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Forwards its ref to the `<input role="combobox">`. The wrapper `<div class="mn-multi-select">` takes `wrapperClassName`.',
        'Every Combobox prop except single-value and `multiple` props passes through: `onSearch`, `onCreate`, `renderOption`, `limit`, `size`, `invalid`, `placeholder`, `valueOptions` and more.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        '`value` + `onValueChange(values)` (controlled) or `defaultValue`, as string arrays in pick order.',
        '`maxSelections` disables the other options once reached and shows `messages.limit(count, max)`.',
        '`messages` includes every Combobox message (`remove`, `added`, `removed`, …) plus `limit`.',
        'Migrating from shadcn/ui: there is no shadcn multi-select; apps use a Command list inside a Popover with Badges, or Rook Tickets’ `Picker multiple`. Map `selected`/`onChange` (or `value`/`onChange`) to `value`/`onValueChange`, the item list to `options` (`value`, `label`, optional `description`, `media`, `group`), and drop the hand-built Badge chips.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'size',
          values: ['sm', 'md', 'lg'],
          description: 'Field height, from Combobox. Default `md`.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Open',
          attribute: 'aria-expanded="true" on the input',
          description: 'The listbox shows under the field.',
        },
        {
          name: 'Selected option',
          attribute: 'aria-selected="true"',
          description: 'A check mark beside the option; the value is a chip in the field.',
        },
        {
          name: 'Limit reached',
          attribute: 'data-full on the wrapper; aria-disabled on the other options',
          description: 'Only picked options can be toggled; the count is emphasised.',
        },
        {
          name: 'Disabled',
          attribute: 'data-disabled',
          description: 'No remove buttons and nothing submits.',
        },
        {
          name: 'Invalid',
          attribute: 'aria-invalid, data-invalid on the field',
          description: 'Danger border, from Combobox.',
        },
      ],
    },
    interaction: {
      items: [
        'Typing filters the list; Enter or a click toggles the active option and clears the text.',
        'The list stays open after a pick; Escape or a click outside closes it.',
        'Each chip has a remove button; Backspace in an empty input removes the last chip.',
        'Additions and removals are announced politely.',
      ],
    },
    keyboard: {
      items: [
        { keys: '↓ / Alt+↓', action: 'Opens the list, or moves to the next option.' },
        { keys: '↑', action: 'Previous option.' },
        { keys: 'Home / End', action: 'First / last option.' },
        { keys: 'Page Down / Page Up', action: 'Skips ten options.' },
        { keys: 'Enter', action: 'Toggles the active option.' },
        { keys: 'Escape / Alt+↑', action: 'Closes the list; focus stays in the input.' },
        { keys: 'Backspace', action: 'With an empty input, removes the last chip.' },
        { keys: 'Tab', action: 'Leaves the field (chip remove buttons are also Tab stops).' },
      ],
    },
    accessibility: {
      role: 'combobox controlling a listbox with aria-multiselectable="true"',
      name: 'The Field’s FieldLabel or `aria-label`; the listbox takes the same name unless `listLabel` is given.',
      items: [
        'Follows the WAI-ARIA APG combobox pattern with `aria-activedescendant`, so focus stays in the input.',
        'Chips are a list; each remove button is named "Remove <label>".',
        'The `maxSelections` count is linked to the input with `aria-describedby` and updates politely.',
      ],
    },
    responsive: {
      items: [
        'Chips wrap inside the field; the field never scrolls horizontally at 390px.',
        'The list is as wide as the field and stays within the viewport.',
      ],
    },
    theme: {
      items: [
        'Field, chips and list use Combobox and menu tokens, so every theme and dark mode apply.',
        'Forced-colours mode and reduced motion are handled by Combobox; the count turns GrayText when disabled.',
      ],
    },
    examples: {
      items: [
        { title: 'In a Field, controlled', render: () => <Controlled /> },
        {
          title: 'With a limit',
          render: () => (
            <MultiSelect
              aria-label="Reviewers"
              maxSelections={2}
              defaultValue={['ana']}
              options={[
                { value: 'ana', label: 'Ana Ruiz' },
                { value: 'ben', label: 'Ben Okafor' },
                { value: 'chen', label: 'Chen Wei' },
                { value: 'dana', label: 'Dana Novak' },
              ]}
            />
          ),
        },
        {
          title: 'Grouped options with descriptions',
          render: () => (
            <MultiSelect
              aria-label="Apps"
              placeholder="Choose apps"
              options={[
                { value: 'calendar', label: 'Calendar', group: 'Work' },
                { value: 'chat', label: 'Chat', group: 'Work' },
                { value: 'tickets', label: 'Tickets', group: 'Support', description: 'Help desk' },
                { value: 'links', label: 'Links', group: 'Support' },
              ]}
            />
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Give it a visible label with a Field and FieldLabel.',
        'Use `name` so the values submit with a form, and validate counts on submit.',
        'Use `onSearch` for long or remote lists instead of loading everything.',
      ],
      dont: [
        'Do not use it for five or fewer options: show them as a CheckboxGroup.',
        'Do not use it for values people invent: use TagInput.',
        'Do not hide the limit: set `maxSelections` so the count is shown.',
      ],
    },
    related: {
      items: [
        { id: 'combobox', reason: 'The single-value searchable select it is built on.' },
        { id: 'checkbox-group', reason: 'A short list shown in full.' },
        { id: 'tag-input', reason: 'Free-form values.' },
        { id: 'entity-picker', reason: 'Picking people and records with avatars.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['combobox', 'form-field'],
    },
    testing: {
      unit: [
        'Labelled combobox with a multi-selectable listbox; filtering, Enter and click picks.',
        'Arrow keys, End, Enter and Escape with focus kept in the input.',
        'Chip remove buttons and Backspace; controlled value.',
        '`maxSelections`: other options disabled, count as the input description, custom message.',
        'Form submission of each value under `name`, never the search text; disabled state.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
