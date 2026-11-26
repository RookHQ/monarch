import {
  Button,
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
  PromptDialog,
  usePrompt,
} from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';
import { MoreIcon, PencilIcon } from '../../../internal/docs-demo.js';

const wait = (ms: number) => new Promise<void>((resolve) => window.setTimeout(resolve, ms));

function NameView() {
  return (
    <PromptDialog
      trigger={<Button>Save view as</Button>}
      title="Name this view"
      description="Saved views appear in the sidebar."
      label="View name"
      placeholder="For example: Open this week"
      required
      maxLength={60}
      submitLabel="Save view"
      onSubmit={() => wait(800)}
    />
  );
}

function AddLink() {
  return (
    <PromptDialog
      trigger={<Button>Add link</Button>}
      title="Add a link"
      label="Link address"
      inputType="url"
      placeholder="https://"
      required
      submitLabel="Add link"
      onSubmit={() => {}}
    />
  );
}

function AddNote() {
  return (
    <PromptDialog
      trigger={<Button>Add note</Button>}
      title="Add a note to this alert"
      label="Note"
      fieldDescription="Press Ctrl+Enter or Cmd+Enter to save."
      multiline
      required
      maxLength={2000}
      submitLabel="Add note"
      onSubmit={() => wait(600)}
    />
  );
}

function RenameWithError() {
  return (
    <PromptDialog
      trigger={<Button>Rename group</Button>}
      title="Rename group"
      label="Group name"
      defaultValue="Design team"
      required
      validate={(value) => (value === 'Design team' ? 'Enter a different name.' : null)}
      submitLabel="Rename"
      onSubmit={async () => {
        await wait(800);
        throw new Error('A group with this name already exists.');
      }}
    />
  );
}

function RowActions() {
  const { prompt, element } = usePrompt();
  return (
    <>
      <DropdownMenu>
        <DropdownMenuTrigger asChild>
          <Button trailingIcon={<MoreIcon />}>Image actions</Button>
        </DropdownMenuTrigger>
        <DropdownMenuContent>
          <DropdownMenuItem
            icon={<PencilIcon />}
            onSelect={async () => {
              const alt = await prompt({
                title: 'Describe this image',
                description: 'Screen readers read this text aloud.',
                label: 'Alt text',
                defaultValue: 'A bar chart of weekly sign-ups',
                maxLength: 250,
                submitLabel: 'Save alt text',
                onSubmit: () => wait(500),
              });
              if (alt !== null) {
                // Store the new alt text here.
              }
            }}
          >
            Edit alt text
          </DropdownMenuItem>
        </DropdownMenuContent>
      </DropdownMenu>
      {element}
    </>
  );
}

export default {
  id: 'prompt-dialog',
  title: 'PromptDialog',
  summary:
    'Asks for one value (a name, link, alt text or note) in a dialog: validation, async save, and a promise API that replaces window.prompt.',
  sections: {
    purpose: {
      summary:
        'Collects a single value without leaving the page: naming a saved view, renaming a group, image alt text, a link address or a short note. It replaces `window.prompt`, which cannot be themed, labelled, validated or show progress, and blocks the whole page.',
      useWhen: [
        'One short value is needed to finish an action ("Name this view", "Rename group").',
        'A link or email address that must be checked before it is used.',
        'A note or description of a few lines, with `multiline`.',
      ],
      avoidWhen: [
        'Several fields or choices: use a Dialog with a form.',
        'Renaming in place, where the text is already on screen: use InlineEditText.',
        'Picking between fixed options: use ChoiceDialog.',
        'Confirming a destructive action, with or without a reason: use ConfirmDialog.',
      ],
    },
    designPrinciples: {
      items: [
        'One question, one labelled field: the title says what is asked, the label names the value.',
        'Ready to type: the field is focused when the dialog opens and any prefilled text is selected, so typing replaces it.',
        'Name the button after the action ("Save view", "Add link"), never "OK".',
        'Errors appear where they are fixed: format and validation problems on the field, save failures above the actions, and the typed value is kept.',
        'Honest progress: while saving nothing dismisses the dialog, and Cancel is disabled.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        '`PromptDialog` is controlled (`open` + `onOpenChange`) or uncontrolled (`trigger`, `defaultOpen`). It forwards its ref to the dialog element.',
        '`onSubmit(value)` receives the trimmed value. Return a promise for the pending state; reject (or throw) to show an error and keep the dialog open.',
        '`usePrompt()` returns `{ prompt, element }`: render `element` once and `await prompt(options)`, which resolves with the value or `null` when cancelled. A second call, or unmounting, resolves the earlier one with `null`.',
        'Other DialogContent props (`className`, `closeLabel`, `onEscapeKeyDown`, `data-*`, …) pass through to the dialog.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Checks run on submit in this order: `required` (empty after trimming), the `url` or `email` format, then `validate(value)`. The first message found is shown on the field.',
        '`url` accepts absolute `http`, `https`, `mailto` and `tel` addresses.',
        'Every visible string is a prop (`submitLabel`, `cancelLabel`, `requiredMessage`, `invalidMessage`, `errorMessage`) for translation.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'inputType',
          values: ['text', 'url', 'email'],
          description:
            'Sets the input type and the on-screen keyboard; `url` and `email` check the format on submit and turn off autocorrect.',
        },
        {
          prop: 'multiline',
          values: ['false', 'true'],
          description:
            'A textarea instead of a one-line input. Enter adds a line; Ctrl+Enter or Cmd+Enter submits. Exposed as `data-multiline`.',
        },
        {
          prop: 'size',
          values: ['sm', 'md', 'lg', 'xl'],
          description: 'Dialog width; default `sm`.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Invalid',
          attribute: 'aria-invalid (field)',
          description:
            'Submitting an empty required value, a malformed address or a value `validate` rejects shows the message under the field and moves focus back to it. Typing clears it.',
        },
        {
          name: 'Pending',
          attribute: 'aria-busy (dialog and submit button), data-busy',
          description:
            'The submit button shows a spinner; the field is read-only; Cancel is disabled; Escape and outside clicks are ignored.',
        },
        {
          name: 'Error',
          attribute: 'role="alert"',
          description:
            'A danger Alert above the actions, linked to the submit button with aria-describedby. The value is kept so the person can retry or edit it.',
        },
      ],
    },
    interaction: {
      items: [
        'Opening focuses the field and selects its prefilled text.',
        'Every opening starts from `defaultValue`, with no old errors.',
        'Submitting runs the checks, then `onSubmit`. A synchronous handler closes the dialog at once; a promise keeps it pending until it settles.',
        'On success it closes (unless `closeOnSubmit={false}`); on failure it stays open with the error and the value.',
        'Escape and Cancel call `onCancel` and close it, except while pending. Outside clicks do not close it, so typed text is not lost.',
        'From a DropdownMenu item, call `prompt()` from `usePrompt` in `onSelect`; focus returns to the menu trigger afterwards.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Enter (one-line field)', action: 'Submits.' },
        { keys: 'Enter (multi-line field)', action: 'Adds a new line.' },
        { keys: 'Ctrl+Enter / Cmd+Enter (multi-line field)', action: 'Submits.' },
        {
          keys: 'Enter while composing (IME)',
          action: 'Commits the composed text; it does not submit.',
        },
        {
          keys: 'Escape',
          action:
            'Cancels and returns focus to the trigger; ignored while pending or while an IME is composing.',
        },
        {
          keys: 'Tab / Shift+Tab',
          action: 'Moves between the field, Cancel and the submit button; focus stays inside.',
        },
      ],
    },
    accessibility: {
      role: 'dialog (aria-modal)',
      name: '`title`; `description` becomes the accessible description.',
      items: [
        'The field is labelled by `label` and described by `fieldDescription` and, when present, the error.',
        'Field errors are announced politely; a save error is announced assertively (`role="alert"`).',
        'Required fields expose `required` and `aria-required`; the asterisk is decorative.',
        'The pending submit button sets `aria-busy`, and focus stays where it was.',
        'Focus returns to the element that opened the dialog, or to the menu trigger when it was opened from a menu item.',
      ],
    },
    responsive: {
      items: [
        'Default width 24rem (`sm`), never wider than the screen minus 2rem.',
        'The field fills the width; the footer wraps its buttons on narrow screens.',
        'Buttons and the field keep 44px touch targets on coarse pointers.',
      ],
    },
    theme: {
      items: [
        'Dialog surface, input and Alert tokens; no colours of its own.',
        'Light, dark and more-contrast themes apply automatically; forced-colors mode keeps the footer divider visible.',
      ],
    },
    examples: {
      items: [
        { title: 'Name a saved view', render: () => <NameView /> },
        { title: 'Link address (url)', render: () => <AddLink /> },
        { title: 'Multi-line note', render: () => <AddNote /> },
        { title: 'Prefilled, with validation and a save error', render: () => <RenameWithError /> },
        { title: 'From a menu with usePrompt', render: () => <RowActions /> },
      ],
    },
    doDont: {
      do: [
        'Give the field a visible label, even when the title already asks the question.',
        'Prefill `defaultValue` when editing an existing value; it is selected on open.',
        'Use `inputType="url"` or `"email"` for addresses so the format is checked and the right keyboard appears.',
        'Return the save’s promise from `onSubmit` so progress and failure show in the dialog.',
      ],
      dont: [
        'Do not use it for more than one value; build a form in a Dialog.',
        'Do not label the button "OK".',
        'Do not use it to confirm destructive actions; use ConfirmDialog with a reason.',
        'Do not close the dialog yourself before the save has finished.',
      ],
    },
    related: {
      items: [
        { id: 'confirm-dialog', reason: 'Confirming an action, with an optional reason.' },
        { id: 'choice-dialog', reason: 'Picking one of a few fixed options.' },
        { id: 'inline-edit-text', reason: 'Renaming text in place where it is shown.' },
        { id: 'dialog', reason: 'The base it is built on, for forms with several fields.' },
        { id: 'text-field', reason: 'The input and textarea it uses.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['button', 'alert', 'form-field', 'text-field', 'dialog'],
    },
    testing: {
      unit: [
        'Opens as a labelled dialog with the field focused and the prefilled text selected; axe passes; Escape cancels (onCancel) and restores focus.',
        'Enter submits the trimmed value and closes.',
        'Enter and Escape while an IME is composing neither submit nor close.',
        'Required: an empty value shows an announced field error linked to the field; typing clears it.',
        'url and email formats are checked; invalidMessage overrides the message.',
        'validate blocks the submit, describes the field and returns focus to it.',
        'Multi-line: Enter adds a line; Ctrl+Enter and Cmd+Enter submit.',
        'Async: pending sets aria-busy, makes the field read-only, disables Cancel and blocks Escape; success closes.',
        'Async: a rejection shows the error linked to the submit button and allows retry; non-Error rejections use a generic message.',
        'Starts from the default value on every opening; maxLength, placeholder and default labels.',
        'usePrompt resolves with the value on submit and null on cancel, returns focus, and runs async work inside the dialog first.',
        'usePrompt settles an earlier call with null when replaced or unmounted.',
        'usePrompt from a DropdownMenu item: focus lands on the field, and returns to the menu trigger after closing.',
        'Renders on the server (closed and open).',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
