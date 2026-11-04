import { useState } from 'react';
import { InlineEditText } from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';

function wait(ms: number) {
  return new Promise<void>((resolve) => setTimeout(resolve, ms));
}

function TitleDemo() {
  const [title, setTitle] = useState('Update onboarding checklist');
  return (
    <div style={{ fontSize: '1.25rem', fontWeight: 600, width: '100%', maxWidth: 480 }}>
      <InlineEditText
        label="Title"
        value={title}
        maxLength={120}
        validate={(v) => (v.length === 0 ? 'Enter a title.' : null)}
        onSave={async (next) => {
          await wait(600);
          setTitle(next);
        }}
      />
    </div>
  );
}

function FailingDemo() {
  return (
    <div style={{ width: '100%', maxWidth: 480 }}>
      <InlineEditText
        label="Project name"
        defaultValue="Website refresh"
        onSave={async () => {
          await wait(600);
          throw new Error('The name could not be saved. Check your connection and try again.');
        }}
      />
    </div>
  );
}

function DescriptionDemo() {
  const [text, setText] = useState('');
  return (
    <div style={{ width: '100%', maxWidth: 480 }}>
      <InlineEditText
        label="Description"
        multiline
        placeholder="Add a description"
        value={text}
        onSave={async (next) => {
          await wait(400);
          setText(next);
        }}
      />
    </div>
  );
}

export default {
  id: 'inline-edit-text',
  title: 'InlineEditText',
  summary: 'Text that edits in place: async save, pending and error states, validation.',
  sections: {
    purpose: {
      summary:
        'Lets people rename or rewrite a short piece of text where it is shown, such as a record title or a description, without opening a form. The product supplies persistence; the component handles the edit cycle and its states.',
      useWhen: [
        'A single text value on a detail page should be editable in place: an issue or ticket title, a category name, a description.',
        'The save is per field and can fail, so the user needs a pending state and an error that keeps their draft.',
      ],
      avoidWhen: [
        'Several fields are edited and saved together: use a form with Field and an explicit Save.',
        'The value is not text (dates, people, options): use the matching picker.',
        'Rich text with formatting or mentions: that editor stays in the product.',
      ],
    },
    designPrinciples: {
      items: [
        'The text looks like text until it is hovered or focused; a pencil icon and a hover surface reveal that it is editable.',
        'View and edit modes share padding and border width, so switching does not move the text. Font size and weight are inherited, so it fits a page title or a table cell.',
        'A failed save never loses work: the editor stays open with the draft and the error.',
        'No business logic: saving, validation rules and conflict handling are callbacks from the product.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Renders `<div class="mn-inline-edit-text">` (ref) containing a `<button>` in view mode, or an `<input>` (`<textarea>` with `multiline`) in edit mode.',
        '`onSave(next)` may return a promise. While it is pending the input is read-only and a spinner shows; if it rejects, the editor stays open and `getErrorMessage(error)` is shown.',
        'Edit conflicts stay in the product: handle them in `onSaveError(error, draft)` and pass a message (or your own resolution UI) through `error` while keeping `editing` true.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [
        {
          prop: 'multiline',
          values: ['true', 'false'],
          description:
            'A textarea that grows with its content (`field-sizing: content` where supported). Enter adds a line; Ctrl/⌘+Enter saves. Shows Save and Cancel buttons by default (`data-multiline`).',
        },
        {
          prop: 'showActions',
          values: ['true', 'false'],
          description: 'Save and Cancel buttons under the editor. Default true for multiline.',
        },
        {
          prop: 'saveOnBlur',
          values: ['true', 'false'],
          description:
            'Save when focus leaves the editor (default). Turn off when only explicit saves should count.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Viewing',
          attribute: 'data-state="viewing"',
          description:
            'A button showing the value, or the placeholder in subtle text when empty (`data-empty`).',
        },
        {
          name: 'Editing',
          attribute: 'data-state="editing"',
          description: 'An input with the draft, focused with its text selected.',
        },
        {
          name: 'Saving',
          attribute: 'data-saving, aria-busy="true", readonly',
          description:
            'The input is read-only with a spinner named "Saving"; with actions, the Save button shows its loading state.',
        },
        {
          name: 'Error',
          attribute: 'data-invalid, aria-invalid="true"',
          description:
            'A validation message, a failed save, or the product’s `error`. Shown under the input with `role="alert"` and linked by `aria-describedby`.',
        },
        {
          name: 'Disabled',
          attribute: 'data-disabled',
          description: 'Plain text, not a button: nothing to activate.',
        },
      ],
    },
    interaction: {
      items: [
        'Click the text, or focus it and press Enter or Space, to edit.',
        'Enter saves (Ctrl/⌘+Enter with `multiline`); Escape cancels and restores the saved value; leaving the field saves unless `saveOnBlur` is false.',
        'Whitespace is trimmed (`trim`); an unchanged value closes the editor without calling `onSave`.',
        '`validate(draft)` runs before saving; a message blocks the save and keeps the editor open.',
        'A failed save keeps the draft and the error. Leaving the field does not resend the same failed draft; editing it, Enter or Save tries again.',
        'After Enter, Escape, Save or Cancel, focus returns to the text. After a blur save, focus stays where the user moved it.',
        'Controlled: `value` + `onSave`, and optionally `editing` + `onEditingChange`. Uncontrolled: `defaultValue`, `defaultEditing`.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Tab',
          action: 'Moves to the text (a button), or through the input and its buttons.',
        },
        { keys: 'Enter / Space', action: 'On the text: starts editing.' },
        { keys: 'Enter', action: 'In a single-line editor: saves.' },
        { keys: 'Ctrl/⌘ + Enter', action: 'In a multiline editor: saves.' },
        { keys: 'Escape', action: 'Cancels the edit and returns focus to the text.' },
      ],
      notes: ['Enter is ignored while an input method editor is composing text.'],
    },
    accessibility: {
      role: 'button (view), textbox (edit)',
      name: '`label`: the input is named by it, and the view button reads "{label}: {value}" with an "Edit" description (`editHint`).',
      items: [
        'Edit mode is not hidden behind hover: the text itself is the keyboard-reachable control.',
        'Errors use `role="alert"` and are linked to the input with `aria-describedby` while it is `aria-invalid`.',
        'The saving spinner is a named status ("Saving"); the input is `aria-busy` and read-only, not disabled, so focus stays in place.',
        'The pencil icon is decorative.',
      ],
    },
    responsive: {
      items: [
        'Fills its container and wraps long values (`overflow-wrap: anywhere`); no horizontal scrolling at 320px.',
        'On touch devices (`hover: none`) the pencil icon is always visible, since there is no hover to reveal it.',
        'Action buttons wrap under the editor on narrow screens.',
      ],
    },
    theme: {
      items: [
        'Surfaces, borders and text use Monarch semantic tokens, so light, dark and more-contrast themes apply automatically.',
        'The focus ring uses `--mn-color-focus-ring` (the product accent); errors use `--mn-color-danger-strong`.',
        'Transitions use the duration tokens, which are zero under `prefers-reduced-motion`.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Title with validation',
          stack: true,
          render: () => <TitleDemo />,
          code: `<h1>
  <InlineEditText
    label="Title"
    value={issue.title}
    maxLength={120}
    validate={(v) => (v.length === 0 ? 'Enter a title.' : null)}
    onSave={(title) => updateIssue({ title })}
  />
</h1>`,
        },
        {
          title: 'Failed save keeps the draft',
          stack: true,
          render: () => <FailingDemo />,
          code: `<InlineEditText
  label="Project name"
  value={project.name}
  onSave={(name) => api.rename(project.id, name)}   // rejects → error shown, editor stays open
  onSaveError={(error, draft) => {
    if (isConflict(error)) setConflict(draft);       // product-owned conflict handling
  }}
  error={conflict ? 'Someone else renamed this project.' : null}
/>`,
        },
        {
          title: 'Multiline with actions',
          stack: true,
          render: () => <DescriptionDemo />,
          code: `<InlineEditText
  label="Description"
  multiline
  placeholder="Add a description"
  value={description}
  onSave={saveDescription}
/>`,
        },
      ],
    },
    doDont: {
      do: [
        'Give every instance a `label` that names the value ("Title", "Category name").',
        'Return the save promise from `onSave`, so the pending and error states are real.',
        'Throw an Error with a message that says what to do next.',
        'Keep conflict detection in the product and show it through `error`.',
      ],
      dont: [
        'Do not use it for values that must be saved together with other fields.',
        'Do not swallow save errors and resolve the promise: the user would believe it saved.',
        'Do not hide the only edit affordance behind hover; the text itself is the control.',
      ],
    },
    related: {
      items: [
        { id: 'text-field', reason: 'A plain, always-editable input inside a form.' },
        { id: 'form-field', reason: 'Label, help and error wiring for form controls.' },
        { id: 'record-header', reason: 'Planned record header with an inline-edit title.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['button', 'spinner'],
    },
    testing: {
      unit: [
        'View mode is a button named "{label}: {value}" with an "Edit" description.',
        'Click to edit focuses the input; Enter saves trimmed text and returns focus (uncontrolled).',
        'Enter on the text starts editing; Escape cancels, calls onCancel and restores focus.',
        'Blur saves without stealing focus back; an unchanged value does not call onSave.',
        'Async save: aria-busy, read-only and a "Saving" status; on rejection the draft, aria-invalid, the alert and focus are kept, and onSaveError gets the error and draft.',
        'A failed draft is not resent on blur, but is retried after an edit.',
        'validate blocks the save and describes the input.',
        'Controlled value and editing follow the parent; an external error shows while editing.',
        'Multiline: Enter adds a line, Ctrl+Enter saves, Save and Cancel buttons work; tabbing to the buttons does not save.',
        'Disabled renders plain text; an empty value shows the placeholder; server rendering in both modes.',
      ],
      axe: true,
      ssr: true,
      browser: ['Visual baselines: /__gallery', 'Cross-browser axe: /components/inline-edit-text'],
    },
  },
} satisfies ComponentDocs;
