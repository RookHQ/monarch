import { useState } from 'react';
import {
  Button,
  Field,
  FieldLabel,
  FormActionsBar,
  Input,
  UnsavedChangesGuard,
} from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';
// The scenario of the primary example, shared with iOS and Android (CROSS-PLATFORM.md section 8).
import fixture from './form-actions-bar.fixture.json';

const SAVED = { name: fixture.data.name.saved, email: fixture.data.email.value };

/** A stand-in for a network save that fails when the name is empty. */
function fakeSave(values: typeof SAVED) {
  return new Promise<void>((resolve, reject) =>
    setTimeout(() => (values.name.trim() ? resolve() : reject(new Error('Enter a name.'))), 700),
  );
}

/**
 * `edited` opens with the name already changed, so the bar shows its dirty
 * state; that demo has no leave guard, which would otherwise hold this page.
 * Each bar is a named region, so the two demos on one page get their own names.
 */
function SettingsDemo({ edited = false }: { edited?: boolean }) {
  const [saved, setSaved] = useState(SAVED);
  const [values, setValues] = useState(
    edited ? { ...SAVED, name: fixture.data.name.edited } : SAVED,
  );
  const dirty = values.name !== saved.name || values.email !== saved.email;
  const save = () => fakeSave(values).then(() => setSaved(values));
  return (
    <div style={{ display: 'grid', gap: 12, maxWidth: 420 }}>
      {edited ? null : <UnsavedChangesGuard when={dirty} onSave={save} />}
      <Field>
        <FieldLabel>{fixture.data.name.label}</FieldLabel>
        <Input
          value={values.name}
          onChange={(event) => setValues({ ...values, name: event.target.value })}
        />
      </Field>
      <Field>
        <FieldLabel>{fixture.data.email.label}</FieldLabel>
        <Input
          type="email"
          value={values.email}
          onChange={(event) => setValues({ ...values, email: event.target.value })}
        />
      </Field>
      <FormActionsBar
        dirty={dirty}
        sticky={false}
        onSave={save}
        onDiscard={() => setValues(saved)}
        messages={{ label: edited ? 'Profile form actions' : 'Settings form actions' }}
      />
    </div>
  );
}

export default {
  id: 'form-actions-bar',
  title: 'FormActionsBar',
  summary: 'Sticky Save / Discard bar with dirty, pending and error states.',
  sections: {
    purpose: {
      summary:
        'Keeps a form\'s Save and Discard in reach and honest: enabled only with changes, pending while saving, the error kept in view if it fails, and a confirmation before throwing work away. Needed by: Rook Base (`admin/[id]/design/page.tsx`, "Saving… / Save changes / Saved"), Rook Recruit (`w/[slug]/page/page.tsx`, `isDirty ? "Save changes" : "Saved"`) and Rook Links (`views/WorkspaceSettings.tsx`, its own saving flag and "Saving..." labels).',
      useWhen: [
        'A settings page or long form is saved explicitly.',
        'Save must stay visible while the person scrolls, especially on phones.',
      ],
      avoidWhen: [
        'Changes save automatically: use AutosaveIndicator.',
        'The form lives in a dialog: put the actions in DialogFooter.',
        'A one-field inline edit: use InlineEditText.',
      ],
    },
    designPrinciples: {
      items: [
        'Buttons tell the truth: disabled when there is nothing to save, busy while saving.',
        'Errors stay until the next attempt; they are never a toast that disappears.',
        'Discarding is destructive, so it asks first by default (ConfirmDialog, danger tone, focus on Keep editing).',
        'Works with or without a `<form>`: `onSave` for promise-based saves, or a submit button for `onSubmit` forms.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Forwards its ref to the bar `<div role="region">`; native attributes pass through.',
        'Renders the discard confirmation itself through `useConfirm`.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Pending and error are internal when `onSave` returns a promise; pass `pending` and `error` to control them yourself (e.g. from a mutation hook).',
        'Pair with UnsavedChangesGuard: `<UnsavedChangesGuard when={dirty} onSave={save} />`.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'sticky',
          values: ['true', 'false'],
          description:
            'Sticks to the bottom of the scrolling container with a shadow and safe-area padding (default), or sits in the flow. Exposed as `data-sticky`.',
        },
        {
          prop: 'hideWhenClean',
          values: ['true', 'false'],
          description:
            'Hides the bar while there is nothing to save, nothing pending and no error.',
        },
      ],
    },
    states: {
      items: [
        { name: 'Clean', description: 'Save and Discard disabled; no message.' },
        {
          name: 'Dirty',
          attribute: 'data-dirty',
          description: 'Save and Discard enabled; "You have unsaved changes".',
        },
        {
          name: 'Pending',
          attribute: 'data-pending; aria-busy on Save',
          description: 'Save shows a spinner and stays focusable; Discard is disabled.',
        },
        {
          name: 'Error',
          attribute: 'data-error',
          description:
            'The error is shown with a warning icon (`role="alert"`); Save stays enabled to try again.',
        },
      ],
    },
    interaction: {
      items: [
        "Save calls `onSave`; a returned promise keeps it pending; a rejection shows `errorMessage(error)`, the Error's message, or `messages.saveFailed`.",
        'Without `onSave`, Save is `type="submit"` (use `form` when the bar is outside the `<form>`).',
        'Discard opens a confirmation; Discard changes calls `onDiscard`, Keep editing or Escape returns focus to Discard. `confirmDiscard={false}` skips the question.',
        'Extra actions passed as children appear before Discard.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Moves through extra actions, Discard and Save.' },
        { keys: 'Enter / Space', action: 'Activates the focused button.' },
        { keys: 'Escape', action: 'In the discard confirmation: keeps editing.' },
      ],
    },
    accessibility: {
      role: 'region',
      name: '`messages.label` ("Form actions").',
      items: [
        'The unsaved-changes text is a polite status; save errors are announced with `role="alert"`.',
        'Save stays focusable while pending (`aria-busy`), so keyboard users keep their place.',
        'The discard confirmation is an alertdialog with focus on the safe choice.',
      ],
    },
    responsive: {
      items: [
        'Below 30 rem the buttons share the full width under the message, Save on the thumb side.',
        'Sticky mode adds the bottom safe-area inset on phones with a home indicator.',
        'Buttons are at least 44 px tall on coarse pointers.',
      ],
    },
    theme: {
      items: [
        'Surface, border, shadow and text tokens; Save uses the product accent and errors the danger tokens, in every theme.',
        'In forced-colors mode the top border and error text use CanvasText.',
      ],
    },
    examples: {
      items: [
        { title: 'Unsaved changes', stack: true, render: () => <SettingsDemo edited /> },
        { title: 'Settings form with a leave guard', stack: true, render: () => <SettingsDemo /> },
        {
          title: 'States',
          stack: true,
          render: () => (
            <>
              <FormActionsBar
                dirty={false}
                sticky={false}
                onSave={() => {}}
                onDiscard={() => {}}
                messages={{ label: 'Form actions: saved' }}
              />
              <FormActionsBar
                dirty
                sticky={false}
                onSave={() => {}}
                onDiscard={() => {}}
                messages={{ label: 'Form actions: unsaved changes' }}
              >
                <Button variant="ghost">Preview</Button>
              </FormActionsBar>
              <FormActionsBar
                dirty
                pending
                sticky={false}
                onSave={() => {}}
                messages={{ label: 'Form actions: saving' }}
              />
              <FormActionsBar
                dirty
                sticky={false}
                error="The server did not respond. Try again."
                onSave={() => {}}
                messages={{ label: 'Form actions: not saved' }}
              />
            </>
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Render it once per form, after the fields, and keep the default sticky mode on long forms.',
        'Use UnsavedChangesGuard with the same `dirty` value so leaving the page asks too.',
      ],
      dont: [
        'Do not disable Save while the form is invalid; let it validate and show what to fix.',
        'Do not turn off the discard confirmation for forms that take effort to fill in.',
      ],
    },
    related: {
      items: [
        { id: 'unsaved-changes-guard', reason: 'Asks before leaving with unsaved changes.' },
        { id: 'autosave-indicator', reason: 'Saving without a Save button.' },
        {
          id: 'save-status-indicator',
          reason: 'A quiet saved/saving status for the `status` slot.',
        },
        { id: 'confirm-dialog', reason: 'The discard confirmation.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['button', 'confirm-dialog'],
    },
    testing: {
      unit: [
        'Labelled region; Save and Discard enabled only when dirty; unsaved message as a status.',
        'Pending while `onSave` runs (no double save); a rejection shows the error as an alert; a successful retry clears it.',
        'Uses `errorMessage`, and the default message for unreadable errors.',
        'Discard asks first; Keep editing cancels and restores focus; Discard changes calls `onDiscard`.',
        '`confirmDiscard={false}` discards at once.',
        'Without `onSave`, Save submits the form named by `form`.',
        '`hideWhenClean` hides the bar; `status` replaces the message; `pending` is controllable.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
