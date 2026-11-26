import { useState } from 'react';
import { Button } from '@rookcom/monarch-react';
import { KeyCreatedDialog } from '@rookcom/monarch-react/patterns';
import type { ComponentDocs } from '../../../internal/component-docs.js';

function CreateKey() {
  const [open, setOpen] = useState(false);
  return (
    <>
      <Button variant="primary" onClick={() => setOpen(true)}>
        Create key
      </Button>
      <KeyCreatedDialog
        open={open}
        onOpenChange={setOpen}
        name="Reporting job"
        secret="sk_live_4f9a1c7e2b8d6035"
      />
    </>
  );
}

function RotateSecret() {
  const [open, setOpen] = useState(false);
  return (
    <>
      <Button onClick={() => setOpen(true)}>Rotate secret</Button>
      <KeyCreatedDialog
        open={open}
        onOpenChange={setOpen}
        label="Signing secret"
        title="Your new signing secret"
        description="Update your receiver to verify with the new secret."
        secret="whsec_b71c09e4d2a85f36"
        masked
      >
        The old secret keeps working for 24 hours.
      </KeyCreatedDialog>
    </>
  );
}

function PublicKey() {
  const [open, setOpen] = useState(false);
  return (
    <>
      <Button onClick={() => setOpen(true)}>Create publishable key</Button>
      <KeyCreatedDialog
        open={open}
        onOpenChange={setOpen}
        kind="public"
        label="Publishable key"
        title="Publishable key created"
        secret="pk_live_83d0e5a1f7c2"
      />
    </>
  );
}

export default {
  id: 'key-created-dialog',
  title: 'KeyCreatedDialog',
  summary: 'Shows a newly created secret once, with copy, a warning and an acknowledgement.',
  sections: {
    purpose: {
      summary:
        'Hands a newly issued secret (API key, signing secret, client secret, device secret) to the person exactly once. The dialog cannot be dismissed until they confirm they saved it, so the secret is never lost to a stray Escape or click. Needed by: Developers, Chat, People, Intellect, Atlas.',
      useWhen: [
        'The server returns a secret it will never show again (it keeps only a hash).',
        'A key or secret is rotated and the new value must be copied before leaving.',
        'A publishable key is created and you want the same flow without the acknowledgement (`kind="public"`).',
      ],
      avoidWhen: [
        'The value can be read again at any time: show it with CopyField on its details page.',
        'Editing or replacing a saved secret in a form: use SecretField with `isSet`.',
      ],
    },
    designPrinciples: {
      items: [
        'Impossible to lose by accident: Escape, outside clicks and the close button stay off until the acknowledgement is ticked.',
        'Done is never a dead button. Pressing it early moves focus to the checkbox and says what is missing.',
        'Copy first: the value is a read-only, monospace field with a primary Copy button. If the clipboard fails, the value is selected so it can be copied by hand.',
        'Honest copy: the warning says it will not be shown again; the product decides what else to say.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Controlled: pass `open` and `onOpenChange`. Open it after the create or rotate request resolves; there is no trigger.',
        'Forwards its ref to the dialog content. Other DialogContent props (`size`, `fullScreenOnPhone`, `aria-*`) pass through.',
        'Focus returns to whatever had focus when it opened (usually the button that started the create).',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        '`onDone` runs once when the dialog closes after acknowledgement, whichever way it was closed. Clear the secret from your state there.',
        'The acknowledgement resets whenever the dialog opens or `secret` changes.',
        'Migrating from the app copy: Developers `SecretReveal` + `KeyDialogs` lock → `KeyCreatedDialog`. `secret` → `secret`, `keyName` → `name`, `noun` → `label`, `kind: "publishable"` → `kind="public"`, `kind: "webhook"` → `label="Signing secret"`, `note` → `children`, `onDone` → `onDone` (plus `onOpenChange`). The lockProps helper and `hideClose` are built in. Chat `OutgoingWebhooks` signing-secret dialog, People `DevicesSection` device-secret dialog and the Intellect notifications token dialog ("I have saved it" / "I have copied it" footers) map the same way with `label`; Atlas `CalendarFeedDialog` (a secret feed link shown once) maps to `label="Feed link"`.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'kind',
          values: ['secret', 'public'],
          description:
            '`secret`: warning notice and required acknowledgement. `public`: info notice, no acknowledgement.',
        },
        {
          prop: 'masked',
          values: ['false', 'true'],
          description:
            'Show the value plainly (default) or behind a reveal toggle; copy works either way.',
        },
        {
          prop: 'multiline',
          values: ['false', 'true'],
          description: 'A multi-line box for certificates and private keys.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Not acknowledged',
          attribute: 'no data-acknowledged',
          description: 'No close button; Escape and outside clicks are ignored.',
        },
        {
          name: 'Nudged',
          attribute: 'data-nudge on the acknowledgement, aria-invalid on the checkbox',
          description:
            'Done was pressed early: the checkbox is focused, outlined and described by the hint, which is announced.',
        },
        {
          name: 'Acknowledged',
          attribute: 'data-acknowledged, data-ready on Done',
          description: 'The close button appears; Done, Escape and the close button all close it.',
        },
        {
          name: 'Public',
          attribute: 'data-kind="public"',
          description: 'Acknowledged from the start.',
        },
      ],
    },
    interaction: {
      items: [
        'Copy copies the value and confirms in place ("Copied"), announced politely.',
        'Ticking the checkbox enables closing; Done closes and calls `onDone` then `onOpenChange(false)`.',
        'Pressing Done before ticking focuses the checkbox and shows "Confirm that you have saved it…".',
        'With `masked`, the eye button reveals the value; Copy never needs it revealed.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Tab / Shift+Tab',
          action:
            'Moves between the value, Copy, the checkbox and Done; focus stays in the dialog.',
        },
        { keys: 'Space', action: 'Ticks the acknowledgement.' },
        { keys: 'Enter / Space', action: 'Copies, or presses Done.' },
        { keys: 'Escape', action: 'Closes only after acknowledgement.' },
      ],
    },
    accessibility: {
      role: 'dialog (modal)',
      name: 'The title (`title` or `messages.title`); `description` becomes the accessible description.',
      items: [
        'The value field is named by the key name (or `label`); the copy button is "Copy {label}".',
        'The acknowledgement is a real checkbox with a visible label.',
        'The "confirm first" hint is a polite live region and the checkbox’s description while shown.',
        'Done keeps focus and is never disabled, so keyboard users are told why it did not close.',
      ],
    },
    responsive: {
      items: [
        'Uses the Dialog width presets, capped at the viewport; long values wrap anywhere instead of scrolling sideways at 390px.',
        'Pass `fullScreenOnPhone` to fill small screens.',
        'The acknowledgement row is at least 44px tall on touch screens.',
      ],
    },
    theme: {
      items: [
        'Built from Dialog, Alert, CopyField, SecretField and Checkbox, so every theme and the more-contrast modes apply.',
        'Warning uses the warning tone; the nudge outline uses `--mn-color-danger-strong`, and system colours in forced-colours mode.',
      ],
    },
    examples: {
      items: [
        { title: 'New API key', render: () => <CreateKey /> },
        { title: 'Rotated, masked secret with a note', render: () => <RotateSecret /> },
        { title: 'Publishable key (no acknowledgement)', render: () => <PublicKey /> },
      ],
    },
    doDont: {
      do: [
        'Open it as soon as the server returns the secret, and drop the secret from memory in `onDone`.',
        'Name it (`name`) the way the person named the key, so they know which one they are copying.',
        'Use `children` for consequences of a rotation, such as how long the old value keeps working.',
      ],
      dont: [
        'Do not use it for values that can be shown again; it makes people do needless work.',
        'Do not add your own Cancel button: the secret already exists, so cancelling would lose it.',
        'Do not log or send the secret anywhere from `onCopy`.',
      ],
    },
    related: {
      items: [
        {
          id: 'secret-field',
          reason: 'The masked field used with `masked`; also for editing saved secrets.',
        },
        { id: 'copy-field', reason: 'The read-only value with Copy it renders.' },
        { id: 'dialog', reason: 'The modal it is built on.' },
        { id: 'confirm-dialog', reason: 'For confirming the revoke or rotate that precedes it.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: [
        'button',
        'copy-field',
        'alert',
        'checkbox',
        'form-field',
        'dialog',
        'secret-field',
      ],
    },
    testing: {
      unit: [
        'Named by its title; shows the value, warning and unticked acknowledgement; no close button yet.',
        'Copies the value and calls `onCopy`.',
        'Escape and Done do nothing until acknowledged; Done focuses and describes the checkbox; then it closes, calls `onDone` once and returns focus.',
        'Escape closes once acknowledged.',
        'Reopening resets the acknowledgement.',
        '`kind="public"` needs no acknowledgement.',
        '`masked`, custom title, description, children and messages.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
