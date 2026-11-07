import { useState } from 'react';
import { Button, TypeToConfirmInput } from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';

function Basic() {
  return (
    <TypeToConfirmInput
      phrase="acme-site"
      description="The site, its pages and its form entries are deleted."
    />
  );
}

function AfterSubmit() {
  const [value, setValue] = useState('');
  const [matches, setMatches] = useState(false);
  const [attempted, setAttempted] = useState(false);
  const [done, setDone] = useState(false);
  return (
    <form
      style={{ display: 'flex', flexDirection: 'column', gap: 12, maxWidth: 360 }}
      onSubmit={(event) => {
        event.preventDefault();
        setAttempted(true);
        if (matches) setDone(true);
      }}
    >
      <TypeToConfirmInput
        phrase="north-region"
        value={value}
        onValueChange={(next) => {
          setValue(next);
          setAttempted(false);
          setDone(false);
        }}
        onMatchChange={setMatches}
        showMismatch={attempted}
      />
      <div>
        <Button type="submit" variant="danger">
          Delete region
        </Button>
      </div>
      {done ? <p role="status">The region would be deleted now.</p> : null}
    </form>
  );
}

function CustomLabel() {
  return (
    <TypeToConfirmInput
      phrase="ERASE"
      label="Type ERASE to erase this person’s data"
      mismatchMessage="Type ERASE in capitals."
      matchAnnouncement="Ready to erase"
      showMismatch
      defaultValue="erase"
    />
  );
}

export default {
  id: 'type-to-confirm-input',
  title: 'TypeToConfirmInput',
  summary: 'The “type the name to confirm” field for irreversible actions.',
  sections: {
    purpose: {
      summary:
        'Makes the person type a known phrase, usually the record’s own name, before an irreversible action, so a destructive button cannot be pressed by habit. It wires the label, description, mismatch error and a polite “matches” announcement for you, and exports `phraseMatches` so the submit check uses the same rule.',
      useWhen: [
        'Deleting a workspace, project, site, region or account: anything with wide, permanent effects.',
        'Inside a custom destructive form or Dialog that ConfirmDialog’s `confirmPhrase` does not cover.',
        'Flows such as data erasure where an explicit, typed acknowledgement is expected.',
      ],
      avoidWhen: [
        'A simple confirmation dialog: use ConfirmDialog with `confirmPhrase`, which already contains this field.',
        'Reversible actions: do them and offer Undo instead.',
        'As a security check: typing a public name proves attention, not identity. Use StepUpAuthDialog for that.',
      ],
    },
    designPrinciples: {
      items: [
        'Ask for something meaningful: the record’s own name, shown in the label in monospace so it can be read and typed exactly.',
        'Forgiving where it is safe, strict where it matters: surrounding spaces are ignored and paste works; case and inner characters must match.',
        'No scolding while typing: the error only appears when the parent says so, typically after a submit attempt.',
        'Quiet confirmation: a polite announcement when the text matches, so screen-reader users know the action is now available.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        '`TypeToConfirmInput` forwards its ref to the `<input>`. `className` and `style` go on the root Field; `inputClassName` goes on the input. Other input props (`placeholder`, `readOnly`, `onKeyDown`, `name`, `data-*`, …) pass through to the input.',
        'Controlled (`value` + `onValueChange`) or uncontrolled (`defaultValue`). `onMatchChange(matches)` fires each time the match state flips, not on the first render.',
        '`phraseMatches(value, phrase)` is the pure matching rule (case-sensitive, surrounding whitespace ignored); use it to gate the submit.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Every visible or announced string is a prop for translation: `label`, `description`, `mismatchMessage`, `matchAnnouncement`, `mismatchAnnouncement`.',
        '`showMismatch` is decided by the parent; the component never shows the error on its own.',
      ],
    },
    variants: {
      items: [],
      notes: [
        'There are no visual variants. The label, description and announcements are content props; the input size is the standard Input `size`.',
      ],
    },
    states: {
      items: [
        {
          name: 'Matches',
          attribute: 'data-matches (root)',
          description: 'The typed text matches the phrase; the status region says “Text matches”.',
        },
        {
          name: 'Mismatch shown',
          attribute: 'aria-invalid (input), data-invalid (root)',
          description:
            '`showMismatch` is set and the text does not match: the error is shown, announced politely and linked to the input.',
        },
        {
          name: 'Disabled',
          attribute: 'disabled (input), data-disabled (root)',
          description: 'The field cannot be edited.',
        },
        {
          name: 'Read-only',
          attribute: 'readonly (input)',
          description:
            'Pass `readOnly` while the action runs, so the text stays focusable but fixed.',
        },
      ],
    },
    interaction: {
      items: [
        'Typing or pasting updates the value; the match is re-checked on every change.',
        'When the text starts matching, “Text matches” is announced politely; when it stops matching nothing is announced by default.',
        'The mismatch error appears only while `showMismatch` is true. Clear it when the person edits the text again.',
        'Enter behaves like any input: inside a form it submits, so check `phraseMatches` in the submit handler.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab / Shift+Tab', action: 'Moves focus into and out of the field.' },
        { keys: 'Typing, Ctrl/Cmd+V', action: 'Enters or pastes the phrase.' },
        { keys: 'Enter', action: 'Submits the surrounding form, if there is one.' },
      ],
    },
    accessibility: {
      role: 'textbox inside a Field, plus a polite status region',
      name: 'The label (default “Type <phrase> to confirm”); `description` becomes the accessible description.',
      items: [
        'The label contains the exact phrase as text, so screen readers read what to type.',
        'autocomplete, autocapitalize, autocorrect and spellcheck are off, so the browser does not alter the phrase.',
        'The mismatch error is linked with `aria-describedby`, sets `aria-invalid` and is announced politely when it appears.',
        'A visually hidden `role="status"` region announces when the text matches.',
      ],
    },
    responsive: {
      items: [
        'Fills the width of its container; long phrases wrap inside the label instead of overflowing.',
        'The input keeps the standard control height and font size, so mobile browsers do not zoom.',
      ],
    },
    theme: {
      items: [
        'Field and Input tokens; the phrase uses the muted background and the monospace font token.',
        'Light, dark and more-contrast themes apply automatically; in forced colours the phrase gets a system-colour border.',
      ],
    },
    examples: {
      items: [
        { title: 'Basic', render: () => <Basic /> },
        { title: 'Mismatch after a submit attempt', render: () => <AfterSubmit /> },
        { title: 'Custom label and messages', render: () => <CustomLabel /> },
      ],
    },
    doDont: {
      do: [
        'Use the record’s own name (or slug) as the phrase.',
        'Check `phraseMatches(value, phrase)` again when submitting, and keep the destructive button disabled or blocked until it matches.',
        'Show the mismatch only after a submit attempt, and clear it when the text changes.',
      ],
      dont: [
        'Do not use a generic phrase like “yes” for every action; it becomes muscle memory.',
        'Do not block paste: people copy long names, and that is fine.',
        'Do not treat a typed name as authentication; ask for a password or passkey with StepUpAuthDialog.',
      ],
    },
    related: {
      items: [
        { id: 'confirm-dialog', reason: 'Has the same field built in through `confirmPhrase`.' },
        {
          id: 'form-field',
          reason: 'The Field it renders in, for label, description and error wiring.',
        },
        { id: 'text-field', reason: 'The Input it wraps.' },
        {
          id: 'step-up-auth-dialog',
          reason: 'Re-authentication for actions that need proof of identity.',
        },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['form-field', 'text-field'],
    },
    testing: {
      unit: [
        '`phraseMatches` is case-sensitive and ignores surrounding whitespace.',
        'Labelled with the phrase; description wired; autocomplete, autocorrect, autocapitalize and spellcheck off; ref reaches the input; axe passes.',
        'Match flips are reported with `onMatchChange` and announced politely (not on the first render); surrounding spaces are ignored and case matters.',
        'Paste is accepted; custom announcements are used.',
        'The mismatch is shown only when the parent sets `showMismatch`, and is linked to the field.',
        'Custom label, mismatch message, `defaultValue` and `disabled`.',
        'Renders on the server.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
