import { useState } from 'react';
import {
  Button,
  Field,
  FieldLabel,
  Input,
  UnsavedChangesDialog,
  useUnsavedChangesGuard,
} from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const wait = (ms: number) => new Promise<void>((resolve) => window.setTimeout(resolve, ms));

function EditName() {
  const [saved, setSaved] = useState('Weekly planning');
  const [name, setName] = useState(saved);
  const [page, setPage] = useState('Editor');
  const dirty = name !== saved;
  const { guard, element } = useUnsavedChangesGuard({ when: dirty });
  return (
    <div style={{ display: 'grid', gap: 12, maxWidth: 360 }}>
      <Field>
        <FieldLabel>Board name</FieldLabel>
        <Input value={name} onChange={(event) => setName(event.target.value)} />
      </Field>
      <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
        <Button variant="primary" disabled={!dirty} onClick={() => setSaved(name)}>
          Save
        </Button>
        <Button
          onClick={() =>
            void guard(() => {
              setName(saved);
              setPage('Overview');
            })
          }
        >
          Go to overview
        </Button>
      </div>
      <p style={{ margin: 0 }}>Current page: {page}</p>
      {element}
    </div>
  );
}

function SaveAndLeave() {
  const [saved, setSaved] = useState('Ninety-minute sessions');
  const [text, setText] = useState(saved);
  const [left, setLeft] = useState(false);
  const dirty = text !== saved;
  const { guard, element } = useUnsavedChangesGuard({
    when: dirty,
    onSave: async () => {
      await wait(900);
      setSaved(text);
    },
  });
  return (
    <div style={{ display: 'grid', gap: 12, maxWidth: 360 }}>
      <Field>
        <FieldLabel>Session description</FieldLabel>
        <Input value={text} onChange={(event) => setText(event.target.value)} />
      </Field>
      <Button onClick={() => void guard(() => setLeft(true))}>Back to the list</Button>
      {left ? <p style={{ margin: 0 }}>Left the editor.</p> : null}
      {element}
    </div>
  );
}

function SaveFails() {
  const { guard, element } = useUnsavedChangesGuard({
    when: true,
    onSave: async () => {
      await wait(700);
      throw new Error('The name is already used by another board.');
    },
  });
  return (
    <>
      <Button onClick={() => void guard(() => {})}>Leave with a failing save</Button>
      {element}
    </>
  );
}

function RouterBlocker() {
  // In an app, `open`, stay and leave come from the router's blocker,
  // e.g. React Router's useBlocker: state, reset() and proceed().
  const [open, setOpen] = useState(false);
  const [outcome, setOutcome] = useState('');
  return (
    <>
      <Button onClick={() => setOpen(true)}>Simulate a blocked navigation</Button>
      {outcome ? <p style={{ margin: 0 }}>{outcome}</p> : null}
      <UnsavedChangesDialog
        open={open}
        onStay={() => {
          setOpen(false);
          setOutcome('Stayed on the page.');
        }}
        onLeave={(reason) => {
          setOpen(false);
          setOutcome(reason === 'save' ? 'Saved, then left.' : 'Left without saving.');
        }}
        onSave={() => wait(600)}
        messages={{ description: 'Two fields have changed since you last saved.' }}
      />
    </>
  );
}

export default {
  id: 'unsaved-changes-guard',
  title: 'UnsavedChangesGuard',
  summary:
    'Asks before leaving a form or editor with unsaved changes: keep editing, discard, or save and leave.',
  sections: {
    purpose: {
      summary:
        'Stops people losing work when they navigate away from a form or editor with unsaved changes. It replaces `window.confirm("You have unsaved changes")` with a themed, translatable dialog that can also save, and asks the browser to warn before the tab closes or reloads. It does not depend on a router: wrap your own navigation with `guard`.',
      useWhen: [
        'Settings pages and editors where changes are saved explicitly (a Save button), such as workspace settings or a page editor.',
        'In-app navigation from such a page: tabs, back buttons, sidebar links, closing a side panel.',
        'Router-level blockers (React Router `useBlocker`, a Next.js route-change guard): render `UnsavedChangesDialog` yourself.',
      ],
      avoidWhen: [
        'Forms that save automatically: there is nothing to lose, so do not ask.',
        'Confirming a destructive action: use ConfirmDialog.',
        'Tiny, quick-to-redo inputs (a search box, a filter): asking costs more than retyping.',
      ],
    },
    designPrinciples: {
      items: [
        'Ask only when something would be lost: while `when` is false, `guard` proceeds silently and nothing is registered.',
        'The safe answer is the default: focus starts on Keep editing, outside clicks do nothing, and Escape keeps editing.',
        'Offer the useful way out: with `onSave`, Save and leave saves first and leaves only if the save succeeds.',
        'A failed save never loses work: the error is shown in the dialog and the person stays on the page.',
        'Discard is a danger button set apart from the safe choices, and named after what it does.',
        'Respect the browser: the before-unload warning uses the browser’s own wording, because custom text is ignored.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        '`useUnsavedChangesGuard({ when, onSave?, messages?, interceptLinks?, beforeUnload? })` returns `{ guard, element, open }`. Render `element` once; call `guard(() => router.push(href))` to navigate.',
        '`guard(proceed)` resolves `true` when `proceed` ran (clean, discarded or saved) and `false` when the person stayed. Calling it again while the dialog is open settles the earlier call as `false`.',
        '`UnsavedChangesGuard` is the component form: `<UnsavedChangesGuard when={dirty} interceptLinks />`. Its ref exposes `{ guard }`.',
        '`UnsavedChangesDialog` is the presentational dialog, always controlled: `open`, `onStay`, `onLeave(reason)` and optional `onSave`. Other DialogContent props pass through, and its ref is the dialog element.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        '`messages` replaces any of `title`, `description`, `keepEditing`, `discard`, `save` and `saveFailed` for translation.',
        '`onSave` may return a promise; a rejection is shown with the Error’s message, or `messages.saveFailed` (or your `errorMessage`).',
        'On the dialog, `pending` and `error` can be controlled when the product tracks the save itself.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'onSave',
          values: ['undefined', 'function'],
          description:
            'Without it the dialog offers Keep editing and Discard changes; with it, Save and leave is added as the primary button.',
        },
        {
          prop: 'interceptLinks',
          values: ['false', 'true'],
          description:
            'Also guards plain same-origin link clicks anywhere in the document, then navigates with `window.location.assign`.',
        },
        {
          prop: 'beforeUnload',
          values: ['true', 'false'],
          description:
            'Whether the browser warns before the tab closes or reloads while `when` is true.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Clean',
          description: '`when` is false: `guard` proceeds at once, no listeners are registered.',
        },
        {
          name: 'Dirty',
          description:
            '`when` is true: `guard` asks first, the before-unload warning is on, and links are intercepted when asked.',
        },
        {
          name: 'Asking',
          attribute: 'role="alertdialog"',
          description: 'The dialog is open with focus on Keep editing.',
        },
        {
          name: 'Saving',
          attribute: 'aria-busy (dialog and Save button), data-busy',
          description:
            'Save and leave shows a spinner and keeps focus; Keep editing and Discard are disabled; Escape is ignored.',
        },
        {
          name: 'Save failed',
          attribute: 'role="alert"',
          description:
            'A danger Alert above the actions, linked to the Save button with aria-describedby. The person can retry, discard or keep editing.',
        },
      ],
    },
    interaction: {
      items: [
        'Keep editing, Escape and the close button keep the person on the page; `guard` resolves `false` and focus returns to where it was.',
        'Discard changes runs `proceed` straight away and resolves `true`.',
        'Save and leave runs `onSave`; on success it runs `proceed`, on failure it shows the error and stays.',
        'After the person agrees to leave, the before-unload warning is skipped so the browser does not ask a second time.',
        'With `interceptLinks`, modified clicks (Ctrl, Cmd, Shift, Alt), `target="_blank"`, `download` links, same-page `#hash` links, other origins and links marked `data-unsaved-changes="ignore"` are let through.',
        'Link interception runs in the capture phase, before a router’s own link handler, which then sees the click as handled.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab / Shift+Tab', action: 'Moves between the buttons; focus stays inside.' },
        { keys: 'Enter / Space', action: 'Activates the focused button.' },
        { keys: 'Escape', action: 'Keeps editing (closes the dialog); ignored while saving.' },
      ],
    },
    accessibility: {
      role: 'alertdialog (aria-modal)',
      name: '`messages.title`; `messages.description` becomes the accessible description.',
      items: [
        'Initial focus is on Keep editing, the least destructive answer (APG alert dialog pattern).',
        'A save error is announced assertively (`role="alert"`) and describes the Save button.',
        'The saving button keeps focus and sets `aria-busy`, so keyboard users keep their place.',
        'Focus returns to the control that started the navigation when the person stays.',
      ],
    },
    responsive: {
      items: [
        'Default width 24rem (`sm`), never wider than the screen minus 2rem.',
        'Below 30rem the buttons stack full width, with Keep editing and Save nearest the bottom edge and Discard on top.',
        'On touch screens every button is at least 44px tall.',
      ],
    },
    theme: {
      items: [
        'Dialog surface tokens; the danger Button for Discard and the primary Button for Save.',
        'Light, dark and more-contrast themes apply automatically; in forced colours the footer and Discard get system-colour borders.',
      ],
    },
    examples: {
      items: [
        { title: 'Guard in-app navigation', render: () => <EditName /> },
        { title: 'Save and leave', render: () => <SaveAndLeave /> },
        { title: 'A failed save keeps you on the page', render: () => <SaveFails /> },
        { title: 'With a router blocker', render: () => <RouterBlocker /> },
      ],
    },
    doDont: {
      do: [
        'Compute `when` from real changes (current values differ from saved ones), not from "the field was touched".',
        'Pass `onSave` when the page has a Save action, so leaving does not cost the work.',
        'Wrap every in-app way out with `guard`, or use your router’s blocker with `UnsavedChangesDialog`.',
        'Set `when` back to false after saving, so the next navigation is not interrupted.',
      ],
      dont: [
        'Do not use `window.confirm` or a custom before-unload message; browsers ignore the text.',
        'Do not guard forms that save automatically.',
        'Do not turn on `interceptLinks` when your router already guards navigation; you would ask twice.',
      ],
    },
    related: {
      items: [
        { id: 'confirm-dialog', reason: 'Confirmation before a destructive action.' },
        { id: 'dialog', reason: 'The base it is built on.' },
        { id: 'settings-section', reason: 'Settings forms that usually need a guard.' },
        { id: 'alert', reason: 'Shows a failed save inside the dialog.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['button', 'alert', 'dialog'],
    },
    testing: {
      unit: [
        'Proceeds at once when there are no unsaved changes.',
        'Asks with focus on Keep editing; axe passes; Keep editing stays, resolves false and restores focus.',
        'Escape stays; outside clicks do not close it.',
        'Discard changes runs proceed and resolves true.',
        'Save and leave: pending sets aria-busy, keeps focus, disables the other buttons and ignores Escape; success proceeds.',
        'A failed save shows the error, links it to the Save button, stays open and can be retried; fallback and custom messages.',
        'The before-unload warning is registered only while dirty, removed when clean, can be turned off, and is skipped after Discard.',
        'interceptLinks guards plain same-origin clicks only, and nothing while clean.',
        'A second guard call settles the first as stayed.',
        'UnsavedChangesGuard exposes guard through its ref and renders nothing until asked.',
        'UnsavedChangesDialog is controlled: onStay and onLeave report the choice; controlled pending and error.',
        'Renders on the server, closed and open.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
