import { Input, Textarea } from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';

export default {
  id: 'text-field',
  title: 'Input',
  summary: 'Text entry at three sizes, with invalid and disabled states.',
  sections: {
    purpose: {
      summary:
        'Single-line (Input) and multi-line (Textarea) text entry styled with Monarch tokens.',
      useWhen: [
        'The user types free text: names, emails, search terms, notes.',
        'Inside a Field, to get the label, help and error wiring automatically.',
      ],
      avoidWhen: [
        'The value comes from a known list: use Select or Combobox.',
        'The choice is on/off: use Checkbox or Switch.',
      ],
    },
    designPrinciples: {
      items: [
        'Native `<input>` and `<textarea>` elements, so browser behaviour (autofill, IME, form submission) is unchanged.',
        'Inside a Field the Field’s `invalid`, `required` and `disabled` apply; explicit props win.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Both forward their ref to the native element; all native attributes pass through.',
        'Input replaces the native numeric `size` attribute with the Monarch `size` prop.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [
        {
          prop: 'size (Input)',
          values: ['sm', 'md', 'lg'],
          description:
            'Control height from the `--mn-control-*` tokens. Default `md`. Exposed as `data-size`.',
        },
        {
          prop: 'resizable (Textarea)',
          values: ['true', 'false'],
          description: 'Allows vertical resizing. Default true (`data-resizable`).',
        },
      ],
    },
    states: {
      items: [
        { name: 'Hover', description: 'Border darkens unless the control is disabled.' },
        { name: 'Focus', description: 'Focus ring on `:focus-visible` and a focus-colour border.' },
        {
          name: 'Invalid',
          attribute: 'aria-invalid="true"',
          description: 'Danger-coloured border. Set by `invalid` or by the surrounding Field.',
        },
        {
          name: 'Disabled',
          attribute: 'disabled',
          description: 'Subtle background, disabled text colour and a not-allowed cursor.',
        },
      ],
    },
    interaction: {
      items: [
        'Native text entry; Monarch adds no handlers.',
        'Textarea defaults to `rows={3}` with a minimum height of two control heights.',
      ],
    },
    keyboard: {
      items: [],
      notes: ['Native text-input keyboard behaviour; Monarch adds no key handling.'],
    },
    accessibility: {
      role: 'textbox',
      name: 'A FieldLabel in the surrounding Field, or `aria-label`.',
      items: [
        'Inside a Field, `id`, `aria-describedby`, `aria-invalid` and `aria-required` are set from the Field.',
        'Placeholder text is not a label: always provide a FieldLabel or `aria-label`.',
      ],
    },
    responsive: {
      items: ['Fills the container width (`width: 100%`) at every viewport width.'],
    },
    theme: {
      items: [
        'Border, background and text colours are semantic tokens, so light, dark and more-contrast themes apply automatically.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Sizes and states',
          stack: true,
          render: () => (
            <>
              <Input size="sm" aria-label="Small" placeholder="Small" />
              <Input aria-label="Default" placeholder="Default" />
              <Input size="lg" aria-label="Large" placeholder="Large" />
              <Input aria-label="Invalid" invalid defaultValue="Invalid value" />
              <Input aria-label="Disabled" disabled placeholder="Disabled" />
              <Textarea aria-label="Notes" placeholder="Notes" />
            </>
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Wrap inputs in a Field with a FieldLabel.',
        'Use the matching `type` (`email`, `tel`, `url`) so mobile keyboards and autofill work.',
      ],
      dont: ['Do not use the placeholder as the only label.'],
    },
    related: {
      items: [
        { id: 'form-field', reason: 'Label, help and error wiring.' },
        { id: 'combobox', reason: 'Text entry that picks from a list.' },
        { id: 'search-input', reason: 'Planned search-specific input.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['form-field'],
    },
    testing: {
      unit: [
        'Explicit `invalid` wins outside a Field.',
        'Sizes, disabled and Textarea render accessibly (axe).',
        'Textarea defaults to three rows and is resizable.',
        'Field tests cover required, invalid, description and disabled wiring through Input and Textarea.',
      ],
      axe: true,
      ssr: true,
      browser: ['Visual baselines: /__gallery'],
    },
  },
} satisfies ComponentDocs;
