# Destructive actions

One way to delete anything, in every product, with no native browser dialogs.

`window.confirm`, `alert` and `prompt` are unthemed (no dark or high-contrast
mode), cannot show a destructive tone, are blocked inside iframes and embeds,
cannot be translated and look like phishing. The conformance rule
`dialogs/no-native-dialog` reports every call as an error.

## Pick the level first

Match the friction to what is lost. Too much friction on small actions teaches
people to click through the dialogs that matter.

| Level | What is lost                      | Example                                                 | Use                                                                                                |
| ----- | --------------------------------- | ------------------------------------------------------- | -------------------------------------------------------------------------------------------------- |
| 0     | Nothing: the action is reversible | Delete a note, tile, comment or saved view              | Act immediately, then `useUndoToast()`                                                             |
| 1     | Recoverable from trash            | Delete a document or project that goes to trash         | `ConfirmDialog` with `tone="danger"`, and say where to restore it from                             |
| 2     | Irreversible, one record          | Delete an invoice, a status page, a job                 | `ConfirmDialog` with `tone="danger"` and `consequences`                                            |
| 3     | Irreversible and cascading        | Delete a product with its leads; bulk delete > 10 items | `ConfirmDialog` with `confirmPhrase`, or `BulkDestructiveFlow`                                     |
| 4     | Account, workspace or legal       | Delete a workspace or account, erase personal data      | The flow (`DeleteWorkspaceFlow`, `DeleteAccountFlow`, `GdprEraseFlow`) with step-up authentication |

Reversible alternatives come first: if the action can be undone, prefer
`deactivate` over `delete` (`DeactivateFlow`) and undo over a confirmation.

## Replacing native dialogs

### `window.confirm` → `useConfirm()`

```tsx
const { confirm, element } = useConfirm();

async function onDelete() {
  const ok = await confirm({
    title: 'Delete this tile?',
    description: 'It is removed from your page for everyone.',
    tone: 'danger',
    confirmLabel: 'Delete tile',
  });
  if (!ok) return;
  await deleteTile(id);
}

return (
  <>
    <Button variant="danger" onClick={onDelete}>
      Delete
    </Button>
    {element}
  </>
);
```

`confirm()` resolves with the details (`{ reason }`) or `null`. Pass
`onConfirm` to run the request inside the dialog, so pending and error states
show there and nothing dismisses it mid-request.

### `window.prompt` → `usePrompt()`

```tsx
const { prompt, element } = usePrompt();
const name = await prompt({ title: 'Name this view', label: 'View name', required: true });
if (name !== null) saveView(name);
```

Use `inputType="url"` for links and image URLs and `multiline` for notes.

### `confirm` used as a choice → `useChoice()`

`OK = shared, Cancel = private` is not a confirmation, it is a choice. Name
both options:

```tsx
const { choose, element } = useChoice();
const visibility = await choose({
  title: 'Who can see this view?',
  choices: [
    { value: 'shared', label: 'Everyone in the workspace' },
    { value: 'private', label: 'Only me' },
  ],
});
```

### `window.alert` → a toast or an inline `Alert`

Errors after an action go in a danger toast (`toast.error(getErrorMessage(error))`)
or next to the control that failed. Never block the page to report an error.

### Leave guards → `useUnsavedChangesGuard()`

```tsx
const { guard, element } = useUnsavedChangesGuard({ when: isDirty, onSave: save });
<Link
  onClick={(e) => {
    e.preventDefault();
    guard(() => router.push(href));
  }}
/>;
```

The hook also registers `beforeunload` while there are unsaved changes, which
is the only case where the browser's own prompt is right (closing the tab).

## Writing the dialog

- **Title is the question with the object**: "Delete this project?", not "Are you sure?".
- **The confirm button names the action**: "Delete project", never "OK" or "Yes".
- **Say what happens, with counts**: `consequences={['48 tasks are deleted', '6 people lose access']}`.
  For longer impact summaries use `ConsequenceList`, which separates what is
  removed from what is kept.
- **Type-to-confirm uses the record's own name** (the workspace slug, the
  product name), not "DELETE". Use it for level 3 and above only.
- **Focus starts on Cancel** for danger dialogs, and the dialog cannot be
  dismissed while the request is running. Both are built in.
- **Legal-grade actions** add `acknowledgement="I understand this cannot be undone"`.
- **High-risk actions** (level 4, revoking keys, revealing secrets) require
  step-up authentication: `useStepUpAuth()` or the `StepUpAuthDialog`.

## Components

| Need                                      | Component                                                                                                                                                                                                                      |
| ----------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| Yes/no confirmation                       | `ConfirmDialog`, `useConfirm()`                                                                                                                                                                                                |
| One text value                            | `PromptDialog`, `usePrompt()`                                                                                                                                                                                                  |
| Two or more named options                 | `ChoiceDialog`, `useChoice()`                                                                                                                                                                                                  |
| Reversible delete                         | `useUndoToast()`                                                                                                                                                                                                               |
| Leaving with unsaved changes              | `useUnsavedChangesGuard()`, `UnsavedChangesDialog`                                                                                                                                                                             |
| Re-authenticate                           | `StepUpAuthDialog`, `useStepUpAuth()`                                                                                                                                                                                          |
| Type the name to confirm                  | `TypeToConfirmInput`                                                                                                                                                                                                           |
| What will be removed and kept             | `ConsequenceList`                                                                                                                                                                                                              |
| Settings section for irreversible actions | `DangerZone`, `DangerZoneItem`                                                                                                                                                                                                 |
| Account and workspace flows               | `@rookcom/monarch-react/patterns`: `DeleteWorkspaceFlow`, `DeleteAccountFlow`, `TransferOwnershipFlow`, `LeaveWorkspaceFlow`, `RemoveMemberFlow`, `BulkDestructiveFlow`, `GdprEraseFlow`, `DeactivateFlow`, `RevokeAccessFlow` |

Flows take data and callbacks only. The product loads the impact counts, runs
the requests and decides what is allowed; the flow presents the steps.
