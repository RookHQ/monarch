import { Checkbox } from '@rookcom/monarch-react';
import { incomplete, type ComponentDocs } from '../../../internal/component-docs.js';

export default {
  id: 'checkbox',
  title: 'Checkbox',
  summary: 'Binary or indeterminate choice with inline label.',
  sections: {
    purpose: {
      summary:
        'A binary (or indeterminate) choice, with an optional inline label and description. Built on Radix Checkbox.',
      useWhen: [
        'The choice is submitted with a form, or several independent options can be on at once.',
        'A "select all" control that is partly selected (`checked="indeterminate"`).',
      ],
      avoidWhen: ['The setting applies immediately: use Switch.'],
    },
    designPrinciples: {
      items: [
        'Pass `label` for an inline label; omit it when the checkbox sits in a Field with a FieldLabel.',
        'Participates in native forms through `name` and `value`.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Forwards its ref to the Radix Checkbox root (a `<button role="checkbox">`).',
        'With `label`, `className` goes on the `.mn-choice` wrapper; without it, on the checkbox.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [],
      notes: ['Checkbox has a single size and style; it has no variant props.'],
    },
    states: {
      items: [
        {
          name: 'Checked / indeterminate',
          attribute: "data-state='checked' | 'indeterminate'",
          description: 'Filled accent box with a check or a dash glyph.',
        },
        {
          name: 'Invalid',
          attribute: 'aria-invalid="true"',
          description: 'Danger border; from `invalid` or the surrounding Field.',
        },
        {
          name: 'Disabled',
          attribute: 'disabled',
          description: 'Dimmed; the inline label uses the disabled text colour.',
        },
        { name: 'Focus', description: 'Visible focus ring on `:focus-visible`.' },
      ],
    },
    interaction: {
      items: [
        'Click on the box or the inline label toggles it.',
        '`description` renders under the label and is added to `aria-describedby`.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Moves focus to the checkbox.' },
        { keys: 'Space', action: 'Toggles the checkbox.' },
      ],
    },
    accessibility: {
      role: 'checkbox',
      name: 'The inline `label` (a `<label htmlFor>`), a FieldLabel, or `aria-label`.',
      items: [
        'Indeterminate is exposed as `aria-checked="mixed"`.',
        'The description is linked with `aria-describedby`.',
      ],
    },
    responsive: incomplete(
      'No responsive behaviour is defined; the inline label wraps with its container.',
    ),
    theme: {
      items: ['Colours are semantic tokens; the checked fill follows the product accent.'],
    },
    examples: {
      items: [
        {
          title: 'Inline label and indeterminate',
          stack: true,
          render: () => (
            <>
              <Checkbox label="Email me updates" description="About once a month." defaultChecked />
              <Checkbox label="Select all" checked="indeterminate" />
              <Checkbox label="Unavailable" disabled />
            </>
          ),
        },
      ],
    },
    doDont: {
      do: ['Write the label as the positive statement ("Email me updates").'],
      dont: ['Do not use a checkbox for a setting that saves immediately; use Switch.'],
    },
    related: {
      items: [
        { id: 'switch', reason: 'On/off settings that apply immediately.' },
        {
          id: 'form-field',
          reason: 'Label and error wiring for a checkbox without an inline label.',
        },
        { id: 'data-table', reason: 'TableSelectCell uses Checkbox for row selection.' },
      ],
    },
    dependencies: {
      packages: ['@radix-ui/react-checkbox', 'react'],
      components: ['form-field'],
    },
    testing: {
      unit: [
        'Toggles with Space and click, including a click on the inline label, and is described by its description.',
        'Supports indeterminate (`aria-checked="mixed"`).',
      ],
      axe: true,
      ssr: true,
      browser: ['Visual baselines: /__gallery'],
    },
  },
} satisfies ComponentDocs;
