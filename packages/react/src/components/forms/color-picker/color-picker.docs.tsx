import { useState } from 'react';
import { ColorPicker, Field, FieldDescription, FieldLabel } from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';

function LabelColourDemo() {
  const [color, setColor] = useState<string | null>('#2563eb');
  return (
    <Field style={{ width: '100%', maxWidth: 360 }}>
      <FieldLabel>Label colour</FieldLabel>
      <ColorPicker value={color} onValueChange={setColor} />
      <FieldDescription>Selected: {color ?? 'none'}</FieldDescription>
    </Field>
  );
}

const STATUS_SWATCHES = [
  { value: '#64748b', label: 'Neutral' },
  { value: '#2563eb', label: 'Planned' },
  { value: '#ca8a04', label: 'At risk' },
  { value: '#16a34a', label: 'Done' },
];

export default {
  id: 'color-picker',
  title: 'ColorPicker',
  summary: 'Named colour swatches plus hex text and the native colour picker.',
  sections: {
    purpose: {
      summary:
        'Chooses a colour for user content (labels, tags, calendars, project or board colours) from named presets or any hex value, and stores lowercase `#rrggbb`. Needed by: rookbase, rookcalendar, rookdots, rookpeople, rookrecruit, rookstride and rooktickets frontends, which use a bare `<input type="color">` today. There is no shadcn/ui colour copy in the apps.',
      useWhen: [
        'People pick a colour for something they own: a label, a calendar, a project, a board column.',
        'A small brand or theme colour setting with a few recommended values.',
      ],
      avoidWhen: [
        'Status or meaning: use Monarch intents (`danger`, `success`…) through Badge or StatusBadge, not a colour of your choosing.',
        'Picking among a few named options where colour is decoration: use RadioGroup or Select.',
      ],
    },
    designPrinciples: {
      items: [
        'Never colour only: every swatch has a name ("Blue, #2563eb") and the selected one shows a check mark.',
        'Presets first, anything second: swatches cover most needs; hex text and the native picker cover the rest.',
        'One value format: lowercase `#rrggbb`, whatever was typed (`ABC`, `#aabbcc`).',
        'Colours are data: swatch values are validated before they reach `style`, and are the only non-token colours.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Renders `div.mn-color-picker` (`role="group"`) with a swatch radio group, a custom row (`input[type=color]` and a hex text box) and an error line. The ref and HTML attributes go to the group.',
        'Helpers: `normalizeHex(text)` returns `#rrggbb` or `null`; `isHexColor(value)`.',
        '`DEFAULT_COLOR_SWATCHES` and `COLOR_PICKER_MESSAGES` are exported.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Swatch labels are data: translate them by passing your own `swatches`.',
        'A swatch whose `value` is not a hex colour is skipped, so untrusted data cannot inject CSS.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'swatches',
          values: ['ColorSwatch[]'],
          description: 'Presets; `[]` leaves only the custom row.',
        },
        {
          prop: 'allowCustom',
          values: ['true', 'false'],
          description: 'Show the native picker and hex text (default), or only presets.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Selected swatch',
          attribute: 'aria-checked="true", data-state="checked"',
          description: 'Ring and check mark on the swatch.',
        },
        {
          name: 'Custom colour',
          attribute: 'no swatch checked',
          description: 'A value that is not a preset shows in the hex box and native picker only.',
        },
        {
          name: 'Invalid hex',
          attribute: 'aria-invalid on the hex box, data-invalid on the group',
          description:
            '"Enter a hex colour like #2563eb."; the text is kept until fixed or Escape.',
        },
        { name: 'Disabled', attribute: 'data-disabled', description: 'All parts disabled.' },
      ],
    },
    interaction: {
      items: [
        'Clicking a swatch selects it.',
        'Hex text is applied on blur or Enter; empty text restores the current value.',
        'The native picker updates the value as it changes.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Tab',
          action:
            'Moves to the selected swatch (one tab stop), then the native picker, then the hex box.',
        },
        {
          keys: 'Arrow keys',
          action: 'Move between swatches and select (left/right flip in RTL); wraps around.',
        },
        { keys: 'Home / End', action: 'First / last swatch.' },
        { keys: 'Enter (hex box)', action: 'Apply the typed colour.' },
        { keys: 'Escape (hex box)', action: 'Restore the current colour.' },
      ],
    },
    accessibility: {
      role: 'group containing a radiogroup',
      name: 'FieldLabel inside a Field, or `aria-label`; the swatches, hex box and native picker are named by `messages`.',
      items: [
        'Swatches follow the APG radio group pattern with roving tabindex.',
        'Each swatch is named "Label, #hex" and has a `title` tooltip.',
        'The error is `role="alert"` and describes the hex box.',
        'Field description and error describe the group.',
      ],
    },
    responsive: {
      items: [
        'Swatches wrap; the hex box shrinks; nothing overflows at 390px.',
        'On coarse pointers swatches and the native picker are 44px.',
      ],
    },
    theme: {
      items: [
        'Chrome uses Input tokens; the selection ring uses `--mn-color-text-default` over a `--mn-color-bg-surface` gap so it shows on any colour.',
        'Forced colours: swatches keep their real colour (`forced-color-adjust: none`) with a CanvasText border and a Highlight outline when selected.',
      ],
    },
    examples: {
      items: [
        { title: 'In a Field', stack: true, render: () => <LabelColourDemo /> },
        {
          title: 'Presets only',
          render: () => (
            <ColorPicker
              aria-label="Status colour"
              swatches={STATUS_SWATCHES}
              allowCustom={false}
              defaultValue="#16a34a"
            />
          ),
        },
        {
          title: 'Custom only',
          render: () => (
            <ColorPicker aria-label="Brand colour" swatches={[]} defaultValue="#4f46e5" />
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Give swatches plain names people would say ("Blue", "Teal").',
        'Store the normalised `#rrggbb` value.',
        'Check text contrast where the colour is used (Button `brandColor` does this for you).',
      ],
      dont: [
        'Do not use colour to carry status meaning; use intents.',
        'Do not pass unvalidated colours from data into styles yourself; use `normalizeHex`.',
      ],
    },
    related: {
      items: [
        { id: 'radio-group', reason: 'The same keyboard pattern for non-colour choices.' },
        { id: 'input-group', reason: 'Attach text or buttons to an Input.' },
        { id: 'form-field', reason: 'Label, description and error wiring.' },
      ],
    },
    dependencies: { packages: ['react'], components: ['form-field'] },
    testing: {
      unit: [
        'isHexColor and normalizeHex.',
        'Group named and described by the Field; swatch names; invalid swatches skipped.',
        'Roving focus with arrows, Home/End and wrap; Tab to the custom row.',
        'Typed hex normalised; invalid text flagged, described and restored with Escape.',
        'Native picker and controlled value; hidden input.',
        'allowCustom={false}, disabled from Field, translated messages.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
