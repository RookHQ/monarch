import { useState } from 'react';
import { AutosaveIndicator, Field, FieldLabel, Input, useAutosave } from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';

/** A stand-in for a network save: waits a moment, fails when the text contains "fail". */
function fakeSave(value: string) {
  return new Promise<void>((resolve, reject) => {
    setTimeout(
      () => (value.includes('fail') ? reject(new Error('Network error')) : resolve()),
      600,
    );
  });
}

function AutosaveDemo() {
  const [title, setTitle] = useState('Weekly notes');
  const autosave = useAutosave({ value: title, onSave: fakeSave, delay: 800, retries: 0 });
  return (
    <>
      <Field>
        <FieldLabel>Title</FieldLabel>
        <Input
          value={title}
          onChange={(event) => setTitle(event.target.value)}
          onBlur={() => void autosave.flush()}
        />
      </Field>
      <AutosaveIndicator state={autosave} />
    </>
  );
}

export default {
  id: 'autosave-indicator',
  title: 'AutosaveIndicator',
  summary: 'useAutosave (debounce, one save at a time, retry, flush) plus its status indicator.',
  sections: {
    purpose: {
      summary:
        'Saves a form or document automatically after the person stops typing and shows where that stands. `useAutosave` holds the logic every editor re-implements (debounce, no overlapping saves, retry, flush before leaving); AutosaveIndicator shows its status. Needed by: Rook Dossier (`components/document/DocumentEditor.tsx`, hand-written 2 s debounce with refs) and Rook Tickets (`components/tickets/detail/Composer.tsx`, draft autosave with its own saving/saved/error state).',
      useWhen: [
        'Edits should be kept without a Save button: documents, notes, drafts, settings that apply as you go.',
        'A form saves in the background but must show failures and let people retry.',
      ],
      avoidWhen: [
        'Changes must be reviewed before they apply: use FormActionsBar with an explicit Save.',
        'Only the status is needed and saving is tracked elsewhere: use SaveStatusIndicator.',
      ],
    },
    designPrinciples: {
      items: [
        'Typing is never blocked: saves run in the background, one at a time, and the newest value always wins.',
        'Failures are visible and recoverable: automatic retries with back-off, then "Couldn\'t save · Retry".',
        'Nothing is lost on the way out: `flush()` saves at once, and `isDirty` feeds UnsavedChangesGuard.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        '`useAutosave(options)` returns `{ status, savedAt, error, isDirty, retry, flush }`.',
        'AutosaveIndicator forwards its ref to the SaveStatusIndicator root and accepts its props (`messages`, `announceSaving`, `locale`, …) except the ones the hook supplies.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'The initial `value` counts as saved. Pass `initialSavedAt` to show when it was saved.',
        'The default `isEqual` compares with `Object.is`, then `JSON.stringify`; pass your own for large values or values with functions or dates that need care.',
      ],
    },
    variants: {
      items: [],
      notes: ['No visual variants; the look comes from SaveStatusIndicator.'],
    },
    states: {
      items: [
        {
          name: 'Idle',
          attribute: 'data-status="idle"',
          description: 'Nothing changed since the initial value.',
        },
        {
          name: 'Unsaved',
          attribute: 'data-status="unsaved"',
          description:
            'Changed; the debounce timer is running (or autosave is paused with `enabled={false}`).',
        },
        {
          name: 'Saving',
          attribute: 'data-status="saving"',
          description: '`onSave` is running, or an automatic retry is waiting.',
        },
        {
          name: 'Saved',
          attribute: 'data-status="saved"',
          description: 'The latest value is saved; shows the time.',
        },
        {
          name: 'Error',
          attribute: 'data-status="error"',
          description: 'Retries are used up; shows the error and Retry.',
        },
      ],
    },
    interaction: {
      items: [
        'Each change restarts the `delay` timer; re-renders with identical content do not.',
        'Edits made while a save is running are saved right after it, after the usual delay.',
        'A failed save is retried `retries` times, waiting `retryDelay`, then twice as long, and so on.',
        'Retry (or `retry()`) saves again at once; `flush()` skips the delay and resolves whether it succeeded.',
        'Changing the value back to the saved one cancels the pending save.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab, Enter / Space', action: 'Reaches and activates Retry after a failure.' },
      ],
      notes: ['The indicator is otherwise not focusable.'],
    },
    accessibility: {
      role: 'polite status (role="status") from SaveStatusIndicator',
      name: 'Not named; its text is read in place.',
      items: [
        '"Saved" and the error are announced politely once each; "Saving…" stays silent unless `announceSaving` is set.',
        'Retry is a real button with a visible label.',
      ],
    },
    responsive: {
      items: [
        'Inherits SaveStatusIndicator: wraps in narrow headers; Retry is 44 px on coarse pointers.',
      ],
    },
    theme: {
      items: [
        'Inherits SaveStatusIndicator tokens: muted text, success, warning and danger accents in every theme.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Autosaving field (type "fail" to see the error)',
          stack: true,
          render: () => <AutosaveDemo />,
        },
      ],
    },
    doDont: {
      do: [
        'Call `flush()` on blur or before navigating, and pass `isDirty` to UnsavedChangesGuard `when`.',
        'Pause with `enabled={false}` while the value is invalid; the status still shows "Unsaved changes".',
      ],
      dont: [
        'Do not save on every keystroke: keep a `delay` of at least a few hundred milliseconds.',
        'Do not hide save errors; keep the indicator visible near the content.',
      ],
    },
    related: {
      items: [
        { id: 'save-status-indicator', reason: 'The status display on its own.' },
        { id: 'form-actions-bar', reason: 'Explicit Save and Discard instead of autosave.' },
        { id: 'unsaved-changes-guard', reason: 'Asks before leaving with unsaved changes.' },
      ],
    },
    dependencies: { packages: ['react'], components: ['save-status-indicator'] },
    testing: {
      unit: [
        'Does not save the initial value; debounces and saves the latest value once.',
        'Ignores re-created identical objects and same-content re-renders.',
        'Runs one save at a time and saves edits made during a save afterwards.',
        'Retries with doubling back-off, then reports the error; `retry()` saves again.',
        '`flush()` saves at once and resolves true; `enabled={false}` stays unsaved.',
        'Changing back to the saved value cancels the pending save.',
        'The indicator follows typing (unsaved, saving, saved), announces Saved, maps errors and retries from its button.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
