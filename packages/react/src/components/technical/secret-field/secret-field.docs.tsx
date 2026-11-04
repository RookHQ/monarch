import { useState } from 'react';
import { Field, FieldDescription, FieldLabel, SecretField } from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const narrow = { width: '100%', maxWidth: '28rem' } as const;

function NewKey() {
  return (
    <Field style={narrow}>
      <FieldLabel>Signing secret</FieldLabel>
      <SecretField label="Signing secret" defaultValue="whsec_9d2f7a61c0b84e3f" readOnly copyable />
      <FieldDescription>
        Shown once. Store it somewhere safe before you leave this page.
      </FieldDescription>
    </Field>
  );
}

function SavedToken() {
  const [value, setValue] = useState('');
  return (
    <Field style={narrow}>
      <FieldLabel>API token</FieldLabel>
      <SecretField
        label="API token"
        isSet
        value={value}
        onChange={(e) => setValue(e.target.value)}
      />
      <FieldDescription>
        {value ? 'The new token replaces the saved one when you save.' : 'Stored encrypted.'}
      </FieldDescription>
    </Field>
  );
}

export default {
  id: 'secret-field',
  title: 'SecretField',
  summary: 'A masked secret with reveal and hide, optional copy, and a "set, not shown" state.',
  sections: {
    purpose: {
      summary:
        'Enters or shows a secret (API key, bot token, signing secret) masked by default, with a toggle to reveal it and an optional copy button. The "set, not shown" state covers secrets the server stores but never sends back.',
      useWhen: [
        'A settings form takes a credential for an integration.',
        'A newly created key is shown once and the user must copy it.',
        'The server holds a saved secret: show that one exists, and let the user replace it.',
      ],
      avoidWhen: [
        'The user signs in: sign-in belongs to the identity flow, not a settings field.',
        'The value is not sensitive: use CopyField or Input.',
      ],
    },
    designPrinciples: {
      items: [
        'Masked until the user asks. Revealing is a deliberate, reversible toggle.',
        'Copy never needs reveal: the copy button copies the value while it stays masked.',
        'Honest about saved secrets: an `isSet` field is empty, says a value is saved, and offers nothing to reveal or copy, because the browser does not have it.',
        'Kept out of password managers and autocorrect, so a token is never saved as a login or "corrected".',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Forwards its ref to the `<input>`. All Input props pass through except `type`, which the reveal state controls.',
        'Works inside a Field: the FieldLabel names the input, and Field descriptions and errors merge with the `isSet` description.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        '`value`/`onChange` (controlled) or `defaultValue` (uncontrolled); copy uses the current text in both modes.',
        '`revealed`/`onRevealedChange` (controlled) or `defaultRevealed` (uncontrolled).',
      ],
    },
    variants: {
      items: [
        {
          prop: 'size',
          values: ['sm', 'md', 'lg'],
          description: 'Input size; the inner buttons scale with it.',
        },
        {
          prop: 'revealable',
          values: ['true', 'false'],
          description: 'Show the reveal toggle. Default true.',
        },
        {
          prop: 'copyable',
          values: ['true', 'false'],
          description: 'Show a copy button. Default false.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Masked',
          attribute: 'type="password"',
          description: 'Default. The toggle is `aria-pressed="false"`.',
        },
        {
          name: 'Revealed',
          attribute: 'data-revealed, type="text", aria-pressed="true"',
          description: 'Plain text; the toggle shows the eye-off icon.',
        },
        {
          name: 'Set, not shown',
          attribute: 'data-set',
          description:
            'Empty with the `setPlaceholder`; described as saved and hidden. Reveal and copy are disabled until the user types a replacement.',
        },
        {
          name: 'Empty',
          attribute: 'disabled on the inner buttons',
          description: 'Nothing to reveal or copy yet.',
        },
        {
          name: 'Invalid',
          attribute: 'aria-invalid',
          description: 'From Field or the `invalid` prop.',
        },
        {
          name: 'Disabled / read-only',
          attribute: 'disabled, readonly',
          description: 'Native input states.',
        },
      ],
    },
    interaction: {
      items: [
        'The eye button toggles between masked and plain text.',
        'The copy button copies the current value (typed or given) and confirms like CopyButton.',
        'With `isSet`, leaving the field blank keeps the saved secret: the product sends nothing. Typing a value replaces it.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Input, then the reveal toggle, then the copy button.' },
        {
          keys: 'Enter / Space',
          action: 'On the toggle: reveal or hide. On the copy button: copy.',
        },
      ],
    },
    accessibility: {
      role: 'textbox (password), toggle button, button',
      name: 'Outside a Field, `label` is the input’s `aria-label`; inside one, the FieldLabel. The toggle is "Show {label}" with `aria-pressed`, the copy button "Copy {label}".',
      items: [
        'The toggle keeps one name and reports its state with `aria-pressed`, as the WAI-ARIA toggle-button pattern requires.',
        'The `isSet` state is in `aria-describedby`, not only the placeholder.',
        'The copy result is announced by CopyButton’s live region.',
        'Inner buttons are at least 28px, above the WCAG 2.2 target-size minimum.',
      ],
    },
    responsive: {
      items: [
        'Full width of its container; the buttons sit inside the input’s end edge and the text is padded to clear them.',
        'Logical properties throughout, so the buttons move to the left in right-to-left layouts.',
      ],
    },
    theme: {
      items: [
        'Built from Input and IconButton, so every theme and more-contrast mode applies.',
        'The value uses `--mn-font-mono`; the placeholder stays in the sans font.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Shown once, with copy',
          stack: true,
          render: () => <NewKey />,
          code: `<Field>
  <FieldLabel>Signing secret</FieldLabel>
  <SecretField label="Signing secret" defaultValue={secret} readOnly copyable />
  <FieldDescription>Shown once. Store it somewhere safe before you leave this page.</FieldDescription>
</Field>`,
        },
        {
          title: 'Set, not shown',
          stack: true,
          render: () => <SavedToken />,
          code: `const [value, setValue] = useState('');

<Field>
  <FieldLabel>API token</FieldLabel>
  <SecretField label="API token" isSet value={value} onChange={(e) => setValue(e.target.value)} />
  <FieldDescription>Stored encrypted.</FieldDescription>
</Field>
// On save: send the token only when value !== ''.`,
        },
        {
          title: 'Sizes, standalone',
          stack: true,
          render: () => (
            <div style={{ ...narrow, display: 'grid', gap: 'var(--mn-space-3)' }}>
              <SecretField label="Bot token" size="sm" defaultValue="bot-4f1a9c" />
              <SecretField label="Access key" size="lg" defaultValue="AKIA4EXAMPLE7Q" copyable />
            </div>
          ),
          code: `<SecretField label="Bot token" size="sm" defaultValue={token} />
<SecretField label="Access key" size="lg" defaultValue={key} copyable />`,
        },
      ],
    },
    doDont: {
      do: [
        'Use `isSet` for saved secrets and send a new value only when the user typed one.',
        'Pair a one-time secret with `copyable` and a description that says it will not be shown again.',
        'Keep validation and storage in the product; SecretField only masks and copies.',
      ],
      dont: [
        'Do not prefill a saved secret into the field; the server should never return it.',
        'Do not reveal by default.',
        'Do not use it for sign-in passwords.',
      ],
    },
    related: {
      items: [
        { id: 'copy-field', reason: 'For values that are not secret.' },
        { id: 'copy-button', reason: 'The copy button it uses.' },
        { id: 'text-field', reason: 'The Input it renders.' },
        { id: 'form-field', reason: 'Label, help and errors.' },
        { id: 'alert', reason: 'For a prominent "copy it now" warning around a one-time secret.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['button', 'copy-button', 'form-field', 'text-field'],
    },
    testing: {
      unit: [
        'Masked by default; the toggle reveals and hides with a stable name and `aria-pressed`.',
        'Toggles from the keyboard.',
        'Controlled reveal and controlled value.',
        'Copies the current value without revealing it, in controlled and uncontrolled modes.',
        '"Set, not shown": empty, described, reveal and copy disabled until a new value is typed.',
        'Turns off autocomplete, spell-check and autocapitalise, and marks the input for password managers to ignore.',
        'Inside a Field: FieldLabel names it, descriptions merge, invalid applies.',
        'Forwards the ref to the input; `revealable={false}` hides the toggle.',
      ],
      axe: true,
      ssr: true,
      browser: ['Visual baselines: /__gallery', 'Cross-browser axe: /components/secret-field'],
    },
  },
} satisfies ComponentDocs;
