import { useState, type ChangeEvent } from 'react';
import {
  Field,
  FieldDescription,
  FieldLabel,
  Input,
  InputGroup,
  InputGroupAddon,
  InputGroupButton,
} from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';

function SearchDemo() {
  const [query, setQuery] = useState('quarterly report');
  return (
    <InputGroup style={{ maxWidth: 320 }}>
      <Input
        type="search"
        aria-label="Search files"
        value={query}
        onChange={(e: ChangeEvent<HTMLInputElement>) => setQuery(e.target.value)}
      />
      {query ? <InputGroupButton onClick={() => setQuery('')}>Clear</InputGroupButton> : null}
    </InputGroup>
  );
}

export default {
  id: 'input-group',
  title: 'InputGroup',
  summary: 'An Input with attached text, icons and buttons in one box with one focus ring.',
  sections: {
    purpose: {
      summary:
        'Attaches prefixes, suffixes and buttons to an Input so they read as one control: "https://" before a domain, "kg" after a weight, a Clear or Copy button inside a field. Needed by: URL and slug fields, unit fields and search boxes across the Rook frontends, which build these today with absolutely positioned icons. The apps have no shadcn/ui input-group copy yet.',
      useWhen: [
        'A fixed prefix or suffix the user should not type ("https://", "@", "%", "kg").',
        'An action that belongs to the field’s value: clear, copy, generate, search.',
      ],
      avoidWhen: [
        'Numbers with − / + buttons: use NumberInput.',
        'Passwords: use PasswordInput, which has the reveal toggle built in.',
        'Amounts: use CurrencyInput, which formats the currency symbol.',
        'Read-only values with a copy button: use CopyField.',
      ],
    },
    designPrinciples: {
      items: [
        'One box, one ring: the group draws the border and the focus ring while the input is focused; attached buttons keep their own ring.',
        'Composition over configuration: the control is a normal Input, so Field wiring, `size` and every input attribute keep working.',
        'Reading order is DOM order. `align` only says which side a part sits on, with logical sides that flip in RTL.',
        'Invalid and disabled follow the Input (`aria-invalid`, `disabled`); the group needs no extra state props.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        '`InputGroup` renders `div.mn-input-group`; `InputGroupAddon` renders `span.mn-input-group__addon`; `InputGroupButton` renders a Monarch Button (or IconButton with `icon` and `label`) with the class `mn-input-group__button`. All forward their refs.',
        'Put an `Input` from Monarch directly inside; the group styles any direct `.mn-input` child.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Migrating from shadcn/ui: `InputGroup` → `InputGroup`; `InputGroupAddon align="inline-start" | "inline-end"` → the same (`block-start` / `block-end` are not supported); `InputGroupText` → plain text inside `InputGroupAddon`; `InputGroupInput` → Monarch `Input`; `InputGroupButton size="icon-xs"` → `InputGroupButton icon={…} label="…"`.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'InputGroup size',
          values: ['sm', 'md', 'lg'],
          description: 'Height from `--mn-control-*`. Give the Input the same `size`.',
        },
        {
          prop: 'align (addon, button)',
          values: ['inline-start', 'inline-end'],
          description:
            'Side of the input. Addons default to `inline-start`, buttons to `inline-end`. Exposed as `data-align`.',
        },
        {
          prop: 'InputGroupButton variant / size',
          values: ['ButtonVariant', 'ButtonSize'],
          description: 'Any Button variant and size; defaults are `ghost` and `sm`.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Focused',
          attribute: ':has(> .mn-input:focus-visible)',
          description: 'Focus ring around the whole group.',
        },
        {
          name: 'Invalid',
          attribute: 'aria-invalid="true" on the Input',
          description: 'Danger border on the group, from the Input or its Field.',
        },
        {
          name: 'Disabled',
          attribute: 'disabled on the Input',
          description: 'Muted surface. Disable attached buttons yourself if they act on the value.',
        },
      ],
    },
    interaction: {
      items: [
        'Clicking an addon does nothing; clicking the input area focuses the input.',
        'Attached buttons are ordinary buttons: they take focus and run their action.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Moves through the input and attached buttons in DOM order.' },
        { keys: 'Enter / Space (on a button)', action: 'Runs the button.' },
      ],
      notes: ['The group adds no key handling; the Input and buttons behave as they do alone.'],
    },
    accessibility: {
      name: 'The Input is named by its FieldLabel or `aria-label`; icon-only buttons by `label`.',
      items: [
        'Addon text is not part of the input’s name. When it carries meaning ("https://", "kg"), give the addon an `id` and add it to the Input’s `aria-describedby`.',
        'Decorative icons in addons should be `aria-hidden`.',
        'Icon-only buttons require `label` (rendered as IconButton with `aria-label`).',
      ],
    },
    responsive: {
      items: [
        'Fills its container; addons and buttons keep their width and the input shrinks first, so nothing overflows at 390px.',
        'On coarse pointers the group is at least 44px tall and attached buttons have a 44px target.',
      ],
    },
    theme: {
      items: [
        'Uses the Input tokens (`--mn-color-border-input`, `--mn-color-bg-surface`, `--mn-color-focus-ring`); addon text is `--mn-color-text-muted`.',
        'Forced colours: system field colours, a Highlight outline on focus, a 2px border when invalid.',
      ],
    },
    examples: {
      items: [
        {
          title: 'URL prefix in a Field',
          stack: true,
          render: () => (
            <Field style={{ width: '100%', maxWidth: 360 }}>
              <FieldLabel>Website</FieldLabel>
              <InputGroup>
                <InputGroupAddon id="website-prefix">https://</InputGroupAddon>
                <Input aria-describedby="website-prefix" placeholder="example.com" />
              </InputGroup>
              <FieldDescription>Without the protocol.</FieldDescription>
            </Field>
          ),
        },
        {
          title: 'Unit suffix',
          render: () => (
            <InputGroup style={{ maxWidth: 200 }}>
              <Input aria-label="Weight" aria-describedby="weight-unit" inputMode="decimal" />
              <InputGroupAddon id="weight-unit" align="inline-end">
                kg
              </InputGroupAddon>
            </InputGroup>
          ),
        },
        { title: 'Search with a clear button', render: () => <SearchDemo /> },
        {
          title: 'Small, with a text button',
          render: () => (
            <InputGroup size="sm" style={{ maxWidth: 320 }}>
              <InputGroupAddon>@</InputGroupAddon>
              <Input size="sm" aria-label="Handle" defaultValue="sample-team" />
              <InputGroupButton>Check</InputGroupButton>
            </InputGroup>
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Keep addons short: a symbol, a unit or a protocol.',
        'Put parts in the order they are read; use `align` only for the side.',
        'Link meaningful addon text with `aria-describedby`.',
      ],
      dont: [
        'Do not put two inputs in one group.',
        'Do not use an addon for a label; use FieldLabel.',
        'Do not attach more than two buttons; move extra actions to a menu.',
      ],
    },
    related: {
      items: [
        { id: 'text-field', reason: 'The Input placed inside the group.' },
        { id: 'button', reason: 'InputGroupButton is a Button.' },
        { id: 'number-input', reason: 'Built-in − / + buttons for numbers.' },
        { id: 'password-input', reason: 'Built-in reveal toggle.' },
        { id: 'copy-field', reason: 'Read-only value with a copy button.' },
      ],
    },
    dependencies: { packages: ['react'], components: ['button'] },
    testing: {
      unit: [
        'Field wiring stays on the Input (name, invalid, description) and a text addon describes it.',
        'Buttons are reached with Tab in DOM order; an icon button is named by `label` and runs on Enter.',
        'Suffix addon, text button defaults (ghost, sm) and group size.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
