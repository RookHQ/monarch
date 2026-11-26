import {
  Button,
  ChoiceDialog,
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
  useChoice,
} from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';
import { MoreIcon } from '../../../internal/docs-demo.js';

const wait = (ms: number) => new Promise<void>((resolve) => window.setTimeout(resolve, ms));

function ViewVisibility() {
  return (
    <ChoiceDialog
      trigger={<Button>Save view</Button>}
      title="Who can see this view?"
      description="You can change this later from the view’s settings."
      choices={[
        { value: 'private', label: 'Only me', description: 'The view is listed only for you.' },
        {
          value: 'shared',
          label: 'Everyone in the workspace',
          description: 'Members see it in their sidebar and can open it.',
        },
      ]}
      defaultValue="private"
      confirmLabel="Save view"
      onChoose={() => wait(800)}
    />
  );
}

function ImportConflicts() {
  return (
    <ChoiceDialog
      trigger={<Button>Import contacts</Button>}
      title="Some contacts already exist"
      description="14 of the 120 rows match existing contacts by email."
      choices={[
        {
          value: 'skip',
          label: 'Skip them',
          description: 'Existing contacts stay as they are.',
        },
        {
          value: 'merge',
          label: 'Fill in missing details',
          description: 'Empty fields are filled from the file; nothing is overwritten.',
        },
        {
          value: 'overwrite',
          label: 'Overwrite them',
          description: 'Values from the file replace what is there now.',
          tone: 'danger',
        },
      ]}
      confirmLabel={(value) => (value === 'overwrite' ? 'Overwrite and import' : 'Import')}
      onChoose={() => wait(1000)}
    />
  );
}

function ChoiceWithError() {
  return (
    <ChoiceDialog
      trigger={<Button>Move items</Button>}
      title="Move 3 items to"
      choices={[
        { value: 'backlog', label: 'Backlog' },
        { value: 'next', label: 'Next up' },
        {
          value: 'done',
          label: 'Done',
          disabled: true,
          description: 'You cannot move items here.',
        },
      ]}
      defaultValue="backlog"
      confirmLabel="Move items"
      onChoose={async () => {
        await wait(800);
        throw new Error('The list changed while you were choosing. Try again.');
      }}
    />
  );
}

function RowActions() {
  const { choose, element } = useChoice();
  return (
    <>
      <DropdownMenu>
        <DropdownMenuTrigger asChild>
          <Button trailingIcon={<MoreIcon />}>Report actions</Button>
        </DropdownMenuTrigger>
        <DropdownMenuContent>
          <DropdownMenuItem
            onSelect={async () => {
              const format = await choose({
                title: 'Download report as',
                choices: [
                  { value: 'pdf', label: 'PDF', description: 'For printing and sharing.' },
                  { value: 'csv', label: 'CSV', description: 'For spreadsheets.' },
                ],
                defaultValue: 'pdf',
                confirmLabel: 'Download',
              });
              if (format) {
                // Start the download here.
              }
            }}
          >
            Download
          </DropdownMenuItem>
        </DropdownMenuContent>
      </DropdownMenu>
      {element}
    </>
  );
}

export default {
  id: 'choice-dialog',
  title: 'ChoiceDialog',
  summary:
    'Pick one of a few labelled options, then confirm: a radio-card dialog with async pending and error, and a promise API.',
  sections: {
    purpose: {
      summary:
        'Asks the person to pick between a few explicit options before an action runs. It replaces `window.confirm` used as a choice ("OK for shared, Cancel for private"), where people cannot tell which button means what and Cancel is overloaded. Every option is named, and Cancel only ever means "do nothing".',
      useWhen: [
        'An action needs one decision first: visibility of a saved item, how to handle duplicates, a file format.',
        'Two to five mutually exclusive options that each deserve a sentence of explanation.',
      ],
      avoidWhen: [
        'A yes-or-no confirmation of one action: use ConfirmDialog.',
        'Typing a value: use PromptDialog.',
        'A setting that stays on the page: use a SegmentedControl or a radio group in the form.',
        'Many options or options with previews: use TemplatePicker or a Select.',
      ],
    },
    designPrinciples: {
      items: [
        'Explicit options: each choice has a label and, when it helps, a description of what it does.',
        'Cancel is never a choice: it closes without doing anything, and sits apart from the options.',
        'A default only when one is safe: leave `defaultValue` out to make the person pick.',
        'Name the confirm button after the action, and let it follow the selection when the result differs.',
        'Destructive options are marked with the danger tone, which also turns the confirm button red while selected.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        '`ChoiceDialog` is controlled (`open` + `onOpenChange`) or uncontrolled (`trigger`, `defaultOpen`). It forwards its ref to the dialog element.',
        '`onChoose(value)` runs the action. Return a promise for the pending state; reject (or throw) to show an error and keep the dialog open.',
        '`useChoice()` returns `{ choose, element }`: render `element` once and `await choose(options)`, which resolves with the chosen value or `null` when cancelled. A second call, or unmounting, resolves the earlier one with `null`.',
        'Other DialogContent props (`className`, `closeLabel`, `onEscapeKeyDown`, `data-*`, …) pass through to the dialog.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'A choice is `{ value, label, description?, tone?, disabled? }`.',
        '`confirmLabel` is a node, or a function of the selected value (`null` when none).',
        'Every visible string is a prop (`confirmLabel`, `cancelLabel`, `requiredMessage`, `errorMessage`) for translation.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'choices[].tone',
          values: ['default', 'danger'],
          description:
            '`danger` marks a destructive option: danger colours on its card when selected, and a danger confirm button. Exposed as `data-tone` on the card.',
        },
        {
          prop: 'defaultValue',
          values: ['undefined', 'string'],
          description:
            'Without it nothing is selected and confirming asks the person to pick an option first.',
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
          name: 'Selected',
          attribute: 'data-state="checked" (card), checked (radio)',
          description: 'The chosen card has an accent border and background.',
        },
        {
          name: 'Disabled choice',
          attribute: 'data-disabled (card), disabled (radio)',
          description: 'Shown dimmed; arrow keys skip it and it cannot be confirmed.',
        },
        {
          name: 'Nothing chosen',
          attribute: 'aria-invalid (radiogroup)',
          description:
            'Confirming with no selection shows `requiredMessage` under the options and focuses the first option.',
        },
        {
          name: 'Pending',
          attribute: 'aria-busy (dialog and confirm button), data-busy',
          description:
            'The confirm button shows a spinner; the selection is locked; Cancel is disabled; Escape and outside clicks are ignored.',
        },
        {
          name: 'Error',
          attribute: 'role="alert"',
          description:
            'A danger Alert above the actions, linked to the confirm button with aria-describedby. The person can retry, pick again or cancel.',
        },
      ],
    },
    interaction: {
      items: [
        'Opening focuses the selected option, or the first enabled one.',
        'Clicking anywhere on a card selects it.',
        'Every opening starts from `defaultValue`, with no old errors.',
        'Confirming calls `onChoose` with the selected value. A synchronous action closes the dialog at once; a promise keeps it pending until it settles.',
        'Escape and Cancel call `onCancel` and close it, except while pending. Outside clicks do not close it.',
        'From a DropdownMenu item, call `choose()` from `useChoice` in `onSelect`; focus returns to the menu trigger afterwards.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Arrow keys',
          action: 'Move between options and select as they go, skipping disabled ones.',
        },
        { keys: 'Space', action: 'Selects the focused option.' },
        { keys: 'Enter (on an option)', action: 'Confirms the selected option.' },
        {
          keys: 'Tab / Shift+Tab',
          action:
            'Moves between the option group (one Tab stop), Cancel and the confirm button; focus stays inside.',
        },
        {
          keys: 'Escape',
          action: 'Cancels and returns focus to the trigger; ignored while pending.',
        },
      ],
    },
    accessibility: {
      role: 'dialog (aria-modal) containing a radiogroup of native radio inputs',
      name: '`title` names both the dialog and the radio group; `description` becomes the dialog’s accessible description.',
      items: [
        'Each radio is named by its label and described by its description.',
        'The group is `aria-required`; when confirming without a choice it becomes `aria-invalid` and is described by the message, which is announced politely.',
        'An action error is announced assertively (`role="alert"`).',
        'Focus returns to the element that opened the dialog, or to the menu trigger when it was opened from a menu item.',
        'In forced-colors mode the system radio is shown and the selected card is outlined.',
      ],
    },
    responsive: {
      items: [
        'Default width 24rem (`sm`), never wider than the screen minus 2rem.',
        'Cards stack in one column and are at least 3rem tall; long labels wrap.',
        'Footer buttons are at least 44px tall on coarse pointers and wrap on narrow screens.',
      ],
    },
    theme: {
      items: [
        'Cards use the surface, border and accent tokens; danger choices use the danger tokens when selected.',
        'Light, dark and more-contrast themes apply automatically; transitions are removed with `prefers-reduced-motion`.',
      ],
    },
    examples: {
      items: [
        { title: 'Visibility of a saved view', render: () => <ViewVisibility /> },
        {
          title: 'No default, a danger option and a label that follows the choice',
          render: () => <ImportConflicts />,
        },
        { title: 'Disabled option and an error', render: () => <ChoiceWithError /> },
        { title: 'From a menu with useChoice', render: () => <RowActions /> },
      ],
    },
    doDont: {
      do: [
        'Describe each option by its result ("Members see it in their sidebar").',
        'Leave out `defaultValue` when no option is safe to assume.',
        'Mark options that destroy or overwrite data with `tone: "danger"`.',
        'Return the action’s promise from `onChoose` so progress and failure show in the dialog.',
      ],
      dont: [
        'Do not use OK and Cancel as the two answers to a question.',
        'Do not add a "Cancel" or "Do nothing" choice; the Cancel button already does that.',
        'Do not offer more than about five options here.',
        'Do not use it for a single yes-or-no confirmation.',
      ],
    },
    related: {
      items: [
        { id: 'confirm-dialog', reason: 'A yes-or-no confirmation of one action.' },
        { id: 'prompt-dialog', reason: 'Asking for a typed value instead of a choice.' },
        { id: 'segmented-control', reason: 'A choice that stays on the page as a setting.' },
        { id: 'template-picker', reason: 'Many options with previews, search and categories.' },
        { id: 'dialog', reason: 'The base it is built on, for custom content.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['button', 'alert', 'dialog'],
    },
    testing: {
      unit: [
        'Opens as a dialog with a radio group named by the title; focus on the selected option; options are named and described; axe passes; Escape cancels and restores focus.',
        'Arrow keys move the selection; confirming sends the chosen value and closes.',
        'Enter on an option confirms it.',
        'Clicking a card selects it; a danger option turns the confirm button red.',
        'Without a default, confirming marks the group invalid, describes it with the message and focuses the first option.',
        'confirmLabel can follow the selection; disabled options are skipped and cannot be picked.',
        'Async: pending sets aria-busy, locks the selection, disables Cancel and blocks Escape; success closes.',
        'Async: a rejection shows the error linked to the confirm button and allows retry.',
        'Controlled pending and error; generic message for non-Error rejections.',
        'Starts from the default choice on every opening.',
        'useChoice resolves with the value on confirm and null on cancel, and returns focus.',
        'useChoice settles an earlier call with null when replaced or unmounted.',
        'useChoice from a DropdownMenu item: focus lands on the selected option, and returns to the menu trigger after closing.',
        'Renders on the server (closed and open).',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
