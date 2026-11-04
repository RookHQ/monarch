import { Select, SelectContent, SelectItem, SelectTrigger } from '@rookcom/monarch-react';
import { incomplete, type ComponentDocs } from '../../../internal/component-docs.js';

export default {
  id: 'select',
  title: 'Select',
  summary: 'Single-value picker with typeahead and keyboard navigation.',
  sections: {
    purpose: {
      summary:
        'Picks one value from a short, known list. Built on Radix Select, which provides typeahead, arrow-key navigation and screen-reader semantics.',
      useWhen: [
        'The list is short enough to scan without searching (statuses, priorities, roles).',
      ],
      avoidWhen: [
        'The list is long or needs searching: use Combobox.',
        'There are two to five options that should all be visible: a segmented control (planned) or radio choices.',
      ],
    },
    designPrinciples: {
      items: [
        'The trigger looks like an Input (`.mn-input`), so forms read as one family.',
        'Menu items share the `.mn-menu` styles with DropdownMenu and Combobox.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        '`Select`, `SelectGroup` and `SelectValue` are the Radix primitives re-exported unchanged.',
        '`SelectTrigger`, `SelectContent`, `SelectItem`, `SelectLabel` and `SelectSeparator` forward their refs to the Radix parts.',
        '`SelectContent` renders in a portal with `position="popper"` and `sideOffset={4}` by default.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [
        {
          prop: 'size (SelectTrigger)',
          values: ['sm', 'md', 'lg'],
          description:
            'Trigger height, matching Input sizes. Default `md`. Exposed as `data-size`.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Placeholder',
          attribute: 'data-placeholder',
          description:
            'The `placeholder` text shows in the subtle text colour until a value is chosen.',
        },
        {
          name: 'Invalid',
          attribute: 'aria-invalid="true"',
          description: 'Danger border, from `invalid` or the surrounding Field.',
        },
        {
          name: 'Highlighted item',
          attribute: 'data-highlighted',
          description:
            'The item under the pointer or keyboard focus; the selected item shows a check.',
        },
      ],
    },
    interaction: {
      items: [
        'Clicking the trigger opens the list; choosing an item closes it and calls `onValueChange`.',
        'The selected item is marked with a check indicator.',
      ],
    },
    keyboard: incomplete(
      'Keyboard handling comes from Radix Select and is not covered by Monarch tests yet.',
      { items: [], notes: ['Radix Select provides typeahead and arrow-key navigation.'] },
    ),
    accessibility: {
      role: 'combobox (trigger) with a listbox of options',
      name: 'A FieldLabel in the surrounding Field, or `aria-label` on SelectTrigger.',
      items: [
        'Inside a required Field the trigger gets `aria-required`, not `required` (invalid on a button).',
        'Inside an invalid Field the trigger gets `aria-invalid`.',
      ],
    },
    responsive: incomplete(
      'No responsive behaviour is defined; the content is positioned by Radix.',
    ),
    theme: {
      items: [
        'Trigger and menu colours are semantic tokens, so every theme applies automatically.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Status picker',
          render: () => (
            <Select defaultValue="open">
              <SelectTrigger aria-label="Status" placeholder="Status" style={{ width: '14rem' }} />
              <SelectContent>
                <SelectItem value="open">Open</SelectItem>
                <SelectItem value="pending">Pending</SelectItem>
                <SelectItem value="resolved">Resolved</SelectItem>
              </SelectContent>
            </Select>
          ),
        },
      ],
    },
    doDont: {
      do: ['Give the trigger a label through Field or `aria-label`.'],
      dont: ['Do not use Select for long lists that need searching; use Combobox.'],
    },
    related: {
      items: [
        { id: 'combobox', reason: 'Searchable single-select for long lists.' },
        { id: 'dropdown-menu', reason: 'Actions rather than a value.' },
        { id: 'form-field', reason: 'Label and error wiring.' },
      ],
    },
    dependencies: {
      packages: ['@radix-ui/react-select', 'react'],
      components: ['form-field'],
    },
    testing: {
      unit: [
        'Trigger inside an invalid required Field gets aria-invalid and aria-required, not required.',
        'A labelled trigger renders without axe violations.',
      ],
      axe: true,
      ssr: true,
      browser: ['Visual baselines: /__gallery', 'Cross-browser axe: /components/select'],
    },
  },
} satisfies ComponentDocs;
