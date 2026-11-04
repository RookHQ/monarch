import { Switch } from '@rookcom/monarch-react';
import { incomplete, type ComponentDocs } from '../../../internal/component-docs.js';

export default {
  id: 'switch',
  title: 'Switch',
  summary: 'Immediate on/off setting.',
  sections: {
    purpose: {
      summary:
        'An on/off setting that applies immediately, with an optional inline label and description. Built on Radix Switch.',
      useWhen: [
        'A setting takes effect as soon as it is toggled (notifications, two-step sign-in).',
      ],
      avoidWhen: ['The choice is submitted with a form: use Checkbox.'],
    },
    designPrinciples: {
      items: [
        'The inline label sits before the control (`.mn-choice--switch`), reading like a settings row.',
        'Omit `label` when the switch sits in a Field or SettingsRow that labels it.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Forwards its ref to the Radix Switch root (a `<button role="switch">`).',
        'With `label`, `className` goes on the `.mn-choice` wrapper; without it, on the switch.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [
        {
          prop: 'size',
          values: ['sm', 'md'],
          description: 'Track size. Default `md`. Exposed as `data-size`.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Checked',
          attribute: "data-state='checked'",
          description: 'Accent track with the thumb moved to the end.',
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
        'Click on the switch or its inline label toggles it.',
        '`description` renders under the label and is added to `aria-describedby`.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Moves focus to the switch.' },
        { keys: 'Space', action: 'Toggles the switch.' },
      ],
    },
    accessibility: {
      role: 'switch',
      name: 'The inline `label`, a FieldLabel, or `aria-label`.',
      items: [
        'State is exposed as `aria-checked`.',
        'Inside a Field, `aria-invalid` and `aria-describedby` come from the Field.',
      ],
    },
    responsive: incomplete('No responsive behaviour is defined for the switch itself.'),
    theme: {
      items: ['Colours are semantic tokens; the checked track follows the product accent.'],
    },
    examples: {
      items: [
        {
          title: 'Settings row',
          stack: true,
          render: () => (
            <>
              <Switch
                label="Two-step sign-in"
                description="Ask for a PIN on new devices."
                defaultChecked
              />
              <Switch label="Compact rail" size="sm" />
            </>
          ),
          code: `<Switch label="Two-step sign-in" description="Ask for a PIN on new devices." defaultChecked />
<Switch label="Compact rail" size="sm" />`,
        },
      ],
    },
    doDont: {
      do: ['Save the setting as soon as it changes.'],
      dont: ['Do not put a switch in a form that needs a Save button; use Checkbox.'],
    },
    related: {
      items: [
        { id: 'checkbox', reason: 'Choices submitted with a form.' },
        {
          id: 'settings-section',
          reason: 'SettingsRow lays out a labelled switch on settings pages.',
        },
        { id: 'switch-settings-list', reason: 'Planned list of switch settings.' },
      ],
    },
    dependencies: {
      packages: ['@radix-ui/react-switch', 'react'],
      components: ['form-field'],
    },
    testing: {
      unit: ['Toggles with click and Space and exposes switch semantics (`aria-checked`).'],
      axe: true,
      ssr: true,
      browser: ['Visual baselines: /__gallery'],
    },
  },
} satisfies ComponentDocs;
