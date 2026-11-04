import {
  Button,
  ConfirmDialog,
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
  useConfirm,
} from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';
import { MoreIcon, TrashIcon } from '../../../internal/docs-demo.js';

const wait = (ms: number) => new Promise<void>((resolve) => window.setTimeout(resolve, ms));

function DeleteProject() {
  return (
    <ConfirmDialog
      trigger={<Button variant="danger">Delete project</Button>}
      tone="danger"
      title="Delete this project?"
      description="This cannot be undone."
      consequences={[
        '48 tasks and their comments are deleted',
        '6 people lose access',
        'Shared links stop working',
      ]}
      confirmPhrase="q3-launch"
      confirmLabel="Delete project"
      onConfirm={() => wait(1200)}
    />
  );
}

function DeclineRequest() {
  return (
    <ConfirmDialog
      trigger={<Button>Decline request</Button>}
      title="Decline this access request?"
      description="The requester is told by email."
      reason={{
        label: 'Reason',
        description: 'Shown to the requester.',
        required: true,
        placeholder: 'For example: use the shared team account instead',
      }}
      confirmLabel="Decline request"
      onConfirm={() => wait(800)}
    />
  );
}

function RevokeWithError() {
  return (
    <ConfirmDialog
      trigger={<Button>Revoke key</Button>}
      tone="danger"
      title="Revoke this API key?"
      description="Requests signed with it start failing straight away."
      confirmLabel="Revoke key"
      onConfirm={async () => {
        await wait(800);
        throw new Error('The key is in use by a running export. Try again when it finishes.');
      }}
    />
  );
}

function RowActions() {
  const { confirm, element } = useConfirm();
  return (
    <>
      <DropdownMenu>
        <DropdownMenuTrigger asChild>
          <Button trailingIcon={<MoreIcon />}>Note actions</Button>
        </DropdownMenuTrigger>
        <DropdownMenuContent>
          <DropdownMenuItem>Pin</DropdownMenuItem>
          <DropdownMenuItem
            tone="danger"
            icon={<TrashIcon />}
            onSelect={async () => {
              const result = await confirm({
                tone: 'danger',
                title: 'Delete this note?',
                description: 'It is removed for everyone on the account.',
                confirmLabel: 'Delete note',
                onConfirm: () => wait(600),
              });
              if (result) {
                // The note is deleted; update the list here.
              }
            }}
          >
            Delete
          </DropdownMenuItem>
        </DropdownMenuContent>
      </DropdownMenu>
      {element}
    </>
  );
}

export default {
  id: 'confirm-dialog',
  title: 'ConfirmDialog',
  summary:
    'Confirmation for consequential actions: danger tone, type-to-confirm, reason, async pending and error.',
  sections: {
    purpose: {
      summary:
        'Asks the person to confirm an action before it happens, says plainly what will change, and stays open while the action runs so a failure can be shown and retried. It replaces `window.confirm` and `window.prompt`, which cannot be themed, translated or show progress.',
      useWhen: [
        'Deleting, revoking, erasing or anything else that cannot be undone.',
        'Actions with wide effects (archive a workspace, remove a member) where a reason may be recorded.',
        'Irreversible actions that deserve a typed confirmation (the record’s name).',
      ],
      avoidWhen: [
        'Reversible actions: do them and offer Undo in a toast instead.',
        'Collecting more than a short reason: use a Dialog with a form.',
      ],
    },
    designPrinciples: {
      items: [
        'Say what happens, not "Are you sure?": the title asks the question, the description and consequences say what changes.',
        'Name the button after the action ("Delete project"), never "OK" or "Yes".',
        'Safe by default: focus starts on Cancel, the least destructive action; outside clicks do not dismiss it; Enter in a field never confirms a mismatched phrase.',
        'Honest progress: while the action runs nothing dismisses the dialog, and a failure is shown in place with the reason and a chance to retry.',
        'The danger tone is reserved for destructive actions: a red confirm button and a calm warning icon, nothing more.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        '`ConfirmDialog` is controlled (`open` + `onOpenChange`) or uncontrolled (`trigger`, `defaultOpen`). It forwards its ref to the dialog element.',
        '`onConfirm(details)` runs the action. Return a promise for the pending state; reject (or throw) to show an error and keep the dialog open.',
        '`useConfirm()` returns `{ confirm, element }`: render `element` once and `await confirm(options)`, which resolves with `{ reason? }` or `null` when cancelled.',
        'Other DialogContent props (`className`, `onEscapeKeyDown`, `data-*`, …) pass through to the dialog.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        '`reason` is `{ label, description?, placeholder?, required?, maxLength?, requiredMessage? }`; the trimmed text arrives as `details.reason`.',
        '`pending` and `error` can be controlled when the product tracks the mutation itself.',
        'Every visible string is a prop (`confirmLabel`, `cancelLabel`, `confirmPhraseLabel`, `confirmPhraseMismatch`, `errorMessage`, `reason.requiredMessage`) for translation.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'tone',
          values: ['default', 'danger'],
          description:
            '`danger` uses the danger Button and a warning icon in the header. Exposed as `data-tone`.',
        },
        {
          prop: 'confirmPhrase',
          values: ['undefined', 'string'],
          description:
            'Type-to-confirm. The confirm button is disabled until the phrase matches exactly (case-sensitive; surrounding spaces ignored).',
        },
        {
          prop: 'reason',
          values: ['undefined', 'optional', 'required'],
          description:
            'A labelled textarea; `required` keeps the confirm button disabled until it has text.',
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
          name: 'Blocked',
          attribute: 'disabled (confirm button)',
          description: 'The phrase does not match or a required reason is empty.',
        },
        {
          name: 'Mismatch',
          attribute: 'aria-invalid (field)',
          description:
            'Pressing Enter with a wrong phrase, or confirming without a required reason, shows a field error.',
        },
        {
          name: 'Pending',
          attribute: 'aria-busy (dialog and confirm button), data-busy',
          description:
            'The confirm button shows a spinner and keeps focus; Cancel is disabled; Escape and outside clicks are ignored; fields are read-only.',
        },
        {
          name: 'Error',
          attribute: 'role="alert"',
          description:
            'A danger Alert above the actions, also linked to the confirm button with aria-describedby. The person can retry or cancel.',
        },
      ],
    },
    interaction: {
      items: [
        'Opening focuses Cancel (`initialFocus="cancel"`), or the phrase or reason field with `initialFocus="field"`.',
        'Confirming calls `onConfirm`. A synchronous action closes the dialog at once; a promise keeps it pending until it settles.',
        'On success it closes (unless `closeOnConfirm={false}`); on failure it stays open with the error and the confirm button ready to retry.',
        'Escape and Cancel call `onCancel` and close it, except while pending.',
        'Every opening starts clean: typed phrase, reason and old errors are cleared.',
        'From a DropdownMenu item, call `confirm()` from `useConfirm` in `onSelect`; focus returns to the menu trigger afterwards.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Tab / Shift+Tab',
          action: 'Moves between the fields, Cancel and the confirm button; focus stays inside.',
        },
        {
          keys: 'Enter (phrase field)',
          action: 'Confirms when the phrase matches; otherwise shows the mismatch error.',
        },
        { keys: 'Enter / Space (button)', action: 'Activates Cancel or the confirm button.' },
        {
          keys: 'Escape',
          action: 'Cancels and returns focus to the trigger; ignored while pending.',
        },
      ],
    },
    accessibility: {
      role: 'alertdialog (aria-modal)',
      name: '`title`; `description` becomes the accessible description.',
      items: [
        'Initial focus is on the least destructive action (APG alert dialog pattern).',
        'The phrase field is labelled with the exact phrase and has autocomplete, autocorrect and spellcheck off.',
        'Field errors are announced politely and linked to the field; an action error is announced assertively (`role="alert"`).',
        'The pending confirm button keeps focus and sets `aria-busy`, so keyboard users do not lose their place.',
        'The warning icon is decorative (`aria-hidden`); the title carries the meaning.',
      ],
    },
    responsive: {
      items: [
        'Default width 24rem (`sm`), never wider than the screen minus 2rem.',
        'The footer wraps its buttons on narrow screens; long phrases wrap inside the label.',
        'Long consequence lists scroll inside the body while the actions stay visible.',
      ],
    },
    theme: {
      items: [
        'Dialog surface tokens, danger tokens for the icon and button, and the muted surface behind the phrase.',
        'Light, dark and more-contrast themes apply automatically; motion follows `prefers-reduced-motion`.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Type to confirm, with consequences',
          render: () => <DeleteProject />,
        },
        {
          title: 'Required reason',
          render: () => <DeclineRequest />,
        },
        {
          title: 'Error and retry',
          render: () => <RevokeWithError />,
        },
        {
          title: 'From a menu with useConfirm',
          render: () => <RowActions />,
        },
      ],
    },
    doDont: {
      do: [
        'Describe the concrete result, with counts, in `description` or `consequences`.',
        'Use `confirmPhrase` with the record’s own name for irreversible, high-impact actions.',
        'Return the mutation’s promise from `onConfirm` so progress and failure show in the dialog.',
        'Throw errors with a message the person can act on, or map them with `errorMessage`.',
      ],
      dont: [
        'Do not confirm reversible actions; offer Undo instead.',
        'Do not use "OK" / "Cancel" or a vague "Are you sure?" title.',
        'Do not use the danger tone for non-destructive actions.',
        'Do not close the dialog yourself before the action has finished.',
      ],
    },
    related: {
      items: [
        { id: 'dialog', reason: 'The base it is built on, for custom content.' },
        { id: 'dropdown-menu', reason: 'Destructive menu items that open a confirmation.' },
        { id: 'toast', reason: 'Undo for reversible actions instead of a confirmation.' },
        { id: 'alert', reason: 'Shows the error inside the dialog.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['button', 'alert', 'form-field', 'text-field', 'dialog'],
    },
    testing: {
      unit: [
        'Opens as an alertdialog with focus on Cancel; axe passes; Escape cancels (onCancel) and restores focus.',
        'Keeps focus inside and ignores outside clicks.',
        'Confirms a synchronous action and closes.',
        'Type-to-confirm: blocked until the exact phrase; Enter with a mismatch marks the field invalid and describes the error; surrounding spaces are ignored.',
        'Async: pending sets aria-busy, keeps focus on the confirm button, disables Cancel and blocks Escape; success closes.',
        'Async: a rejection shows the error, links it to the confirm button, keeps it open and allows retry.',
        'Generic message for non-Error rejections; `errorMessage` maps errors; a synchronous throw is shown too.',
        'Required reason blocks until filled and is passed trimmed; an empty optional reason is omitted.',
        'Consequences render as a list; children come before the fields.',
        'Controlled `pending` and `error`; controlled `open` with `closeOnConfirm={false}`.',
        'Starts clean on every opening.',
        'useConfirm resolves with the details on confirm and null on cancel, and runs async work inside the dialog first.',
        'useConfirm from a DropdownMenu item: focus lands on Cancel, and returns to the menu trigger after closing.',
        'Renders on the server (closed and open).',
      ],
      axe: true,
      ssr: true,
      browser: [
        'Visual baselines: gallery/confirm-dialog and overlays/confirm-dialog-light (open)',
        'Cross-browser axe: /components/confirm-dialog',
      ],
    },
  },
} satisfies ComponentDocs;
