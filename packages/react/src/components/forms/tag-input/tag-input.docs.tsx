import { useState } from 'react';
import { Field, FieldDescription, FieldLabel, TagInput } from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const SKILLS = [
  'Accessibility',
  'Copywriting',
  'Data analysis',
  'Illustration',
  'Research',
  'TypeScript',
];
const isEmail = (v: string) => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(v);

function RecipientsDemo() {
  const [cc, setCc] = useState(['sam@example.com']);
  return (
    <Field>
      <FieldLabel>Cc</FieldLabel>
      <TagInput
        value={cc}
        onValueChange={setCc}
        separators={[',', ' ', ';']}
        validate={(v) => (isEmail(v) ? null : `“${v}” is not an email address.`)}
        normalize={(v) => v.trim().toLowerCase()}
        placeholder="Add addresses"
      />
      <FieldDescription>Separate with commas or spaces.</FieldDescription>
    </Field>
  );
}

export default {
  id: 'tag-input',
  title: 'TagInput',
  summary:
    'Chips for a list of short values: Enter or comma to add, paste to split, keys to remove.',
  sections: {
    purpose: {
      summary:
        'Edits a list of short free-form values (skills, labels, keywords, email recipients) as removable chips in one field, with validation, paste splitting and optional suggestions.',
      useWhen: [
        'The user types several values into one field: labels, skills, Cc addresses.',
        'Values are mostly free text, perhaps with suggestions of known ones.',
      ],
      avoidWhen: [
        'Values must come from a fixed list: use a multi-select.',
        'Each value has several properties to edit: use a list or table with an add row.',
        'Only one value is allowed: use Input or Combobox.',
      ],
    },
    designPrinciples: {
      items: [
        'The product owns the rules: `normalize` and `validate` decide what a valid tag is; the component handles entry, display and removal.',
        'A rejected value stays in the field with a message, so the user can fix it instead of retyping.',
        'Removing is never one stray key away: the first Backspace in an empty field moves to the last chip, the second removes it.',
        'Chips are quiet, neutral surfaces; colour is left for status, not decoration.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Renders a `.mn-tag-input` wrapper containing the chip control (`.mn-tag-input__control`, styled as `.mn-input`), the message and a hidden status region. The ref and native input props go to the text input; `className` goes to the wrapper.',
        'With `suggestions`, the text input becomes a `combobox` and the list is a Radix Popover, as in Combobox.',
        'With `name`, one `<input type="hidden">` per tag is rendered for native form posts.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        '`validate(tag, tags)` returns a message to reject the tag, or `null`/`undefined` to accept it. It runs after `normalize`, the duplicate check and `max`.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'size',
          values: ['sm', 'md', 'lg'],
          description:
            'Minimum height of the control from `--mn-control-*`. The control grows as chips wrap.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Rejected entry',
          attribute: 'aria-invalid="true" on the input',
          description:
            'The message appears under the field and is linked with `aria-describedby`; it clears when the user types.',
        },
        {
          name: 'Invalid',
          attribute: 'data-invalid on the control',
          description: 'Danger border, from `invalid`, the surrounding Field or a rejected entry.',
        },
        {
          name: 'Disabled',
          attribute: 'data-disabled on the control, disabled on the input',
          description: 'Chips stay visible; remove buttons are not rendered.',
        },
        {
          name: 'Read-only',
          attribute: 'readonly on the input',
          description: 'Chips are shown without remove buttons and nothing can be added.',
        },
        {
          name: 'Suggestions open',
          attribute: 'aria-expanded="true" on the input',
          description: 'A listbox of matching suggestions below the field.',
        },
      ],
    },
    interaction: {
      items: [
        'Enter or a separator (default comma) adds the typed text after `normalize`, the duplicate check (case-insensitive), `max` and `validate`.',
        'Pasting text that contains a separator, a new line or a tab adds each part; parts that are rejected stay in the field, joined by commas.',
        'Clicking a chip’s remove button removes it and returns focus to the input.',
        'Additions and removals are announced in a polite status region ("Added Research.").',
        'With `suggestions`, typing opens a listbox of matches that are not already added; clicking one adds it.',
        '`addOnBlur` commits the typed text when focus leaves the field.',
        'Controlled with `value` + `onValueChange` (and optionally `inputValue` + `onInputValueChange`), or uncontrolled with `defaultValue`.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Enter', action: 'Adds the typed text, or the highlighted suggestion.' },
        { keys: ', (or any `separators` key)', action: 'Adds the typed text.' },
        {
          keys: 'Backspace (empty field)',
          action: 'Moves focus to the last chip’s remove button; Backspace again removes it.',
        },
        { keys: 'ArrowLeft (caret at start)', action: 'Moves focus to the last chip.' },
        {
          keys: 'ArrowLeft / ArrowRight (on a chip)',
          action: 'Moves between chips; ArrowRight past the last chip returns to the input.',
        },
        {
          keys: 'Backspace / Delete (on a chip)',
          action: 'Removes it and focuses the previous chip (Backspace) or the next one (Delete).',
        },
        { keys: 'ArrowDown / ArrowUp', action: 'Opens and moves through suggestions.' },
        { keys: 'Escape', action: 'Closes the suggestions.' },
      ],
    },
    accessibility: {
      role: 'textbox, or combobox with a listbox when `suggestions` is set; chips are a list',
      name: 'A FieldLabel or `aria-label` on the field. Each remove button is named by `removeLabel` (default "Remove <tag>").',
      items: [
        'Chips are list items, so screen readers announce how many there are.',
        'Remove buttons are real buttons in the Tab order, so the arrow-key shortcuts are a convenience, not the only way.',
        'Suggestions follow the APG combobox pattern: the input keeps focus and points at the highlighted option with `aria-activedescendant`.',
        'Rejection messages are linked to the input and announced; additions and removals are announced in a separate status region.',
      ],
    },
    responsive: {
      items: [
        'Chips wrap onto new lines and the control grows; the text input keeps at least 6rem so there is always room to type.',
        'Long tags are truncated with an ellipsis inside their chip.',
      ],
    },
    theme: {
      items: [
        'Chips use `--mn-color-bg-muted` and `--mn-color-border-subtle`; the message uses `--mn-color-danger-strong`. All are semantic tokens, so light, dark and more-contrast themes apply.',
        'Remove-button hover transitions are removed under `prefers-reduced-motion`.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Skills with suggestions',
          stack: true,
          render: () => (
            <div style={{ width: '100%', maxWidth: 300 }}>
              <Field>
                <FieldLabel>Skills</FieldLabel>
                <TagInput
                  defaultValue={['Research', 'TypeScript']}
                  suggestions={SKILLS}
                  max={8}
                  placeholder="Add a skill"
                />
              </Field>
            </div>
          ),
          code: `<Field>
  <FieldLabel>Skills</FieldLabel>
  <TagInput
    defaultValue={['Research', 'TypeScript']}
    suggestions={skills}
    max={8}
  />
</Field>`,
        },
        {
          title: 'Validated emails',
          stack: true,
          render: () => (
            <div style={{ width: '100%', maxWidth: 300 }}>
              <RecipientsDemo />
            </div>
          ),
          code: `<TagInput
  value={cc}
  onValueChange={setCc}
  separators={[',', ' ', ';']}
  normalize={(v) => v.trim().toLowerCase()}
  validate={(v) => (isEmail(v) ? null : \`“\${v}” is not an email address.\`)}
/>`,
        },
        {
          title: 'Read-only and small',
          stack: true,
          render: () => (
            <div style={{ display: 'grid', gap: 12, width: '100%', maxWidth: 300 }}>
              <TagInput aria-label="Labels" readOnly defaultValue={['Billing', 'Priority']} />
              <TagInput aria-label="Keywords" size="sm" defaultValue={['quarterly', 'draft']} />
            </div>
          ),
          code: `<TagInput aria-label="Labels" readOnly defaultValue={['Billing', 'Priority']} />
<TagInput aria-label="Keywords" size="sm" defaultValue={['quarterly', 'draft']} />`,
        },
      ],
    },
    doDont: {
      do: [
        'Say in a FieldDescription how values are separated when it is not just commas.',
        'Write rejection messages that say what is wrong with that value.',
        'Pass `normalize` when case or spacing should not create a new tag.',
      ],
      dont: [
        'Do not use it for a fixed set of options; a multi-select makes the choices visible.',
        'Do not hide the reason a value was rejected; return a message from `validate`.',
      ],
    },
    related: {
      items: [
        { id: 'combobox', reason: 'Pick a single value from a list.' },
        { id: 'text-field', reason: 'A single free-text value.' },
        { id: 'filter-chips', reason: 'Chips that show active filters rather than edit a value.' },
        { id: 'form-field', reason: 'Label, help and error wiring.' },
      ],
    },
    dependencies: {
      packages: ['react', '@radix-ui/react-popover'],
      components: ['form-field'],
      notes: [
        'Uses the `.mn-input` styles from text-field for the control and `.mn-menu` for suggestions.',
      ],
    },
    testing: {
      unit: [
        'Adds on Enter and comma; trims and ignores blank entries; labels remove buttons; announces additions.',
        'Splits pasted lists; leaves pastes without separators to the browser.',
        'A validator rejection keeps the text, marks the input invalid and links the message; typing clears it.',
        'Rejects duplicates case-insensitively and enforces `max`.',
        'Backspace moves to the last chip, then removes it; arrows move between chips; Delete removes; focus lands sensibly.',
        'Click removal returns focus to the input.',
        'Suggestions: listbox, already-added values left out, arrow navigation with `aria-activedescendant`, Enter and click to add, Escape to close.',
        'Controlled tags and typed text; `addOnBlur`; hidden inputs for `name`.',
        'Disabled (from Field) and read-only render no remove buttons.',
      ],
      axe: true,
      ssr: true,
      browser: ['Visual baselines: /__gallery', 'Cross-browser axe: /components/tag-input'],
    },
  },
} satisfies ComponentDocs;
