import { useState } from 'react';
import {
  Button,
  Dialog,
  DialogBody,
  DialogClose,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
  Field,
  FieldLabel,
  Input,
} from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';

function RenameDialog() {
  const [open, setOpen] = useState(false);
  const [saving, setSaving] = useState(false);
  const save = () => {
    setSaving(true);
    window.setTimeout(() => {
      setSaving(false);
      setOpen(false);
    }, 1200);
  };
  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger asChild>
        <Button>Rename board</Button>
      </DialogTrigger>
      <DialogContent
        busy={saving}
        closeOnOutsideClick={false}
        fullScreenOnPhone
        footer={
          <>
            <DialogClose asChild>
              <Button disabled={saving}>Cancel</Button>
            </DialogClose>
            <Button variant="primary" loading={saving} onClick={save}>
              Save
            </Button>
          </>
        }
      >
        <DialogHeader>
          <DialogTitle>Rename board</DialogTitle>
          <DialogDescription>Everyone with access sees the new name.</DialogDescription>
        </DialogHeader>
        <DialogBody>
          <Field>
            <FieldLabel>Board name</FieldLabel>
            <Input defaultValue="Q3 launch" />
          </Field>
        </DialogBody>
      </DialogContent>
    </Dialog>
  );
}

export default {
  id: 'dialog',
  title: 'Dialog',
  summary: 'Modal with focus trap, Escape and focus return.',
  sections: {
    purpose: {
      summary:
        'A modal window for a focused task or decision that must be finished or dismissed before returning to the page.',
      useWhen: [
        'A short form that should not leave the page.',
        'A focused decision with custom content.',
      ],
      avoidWhen: [
        'A yes/no confirmation: use ConfirmDialog, which adds the alertdialog role, pending and error states.',
        'Detail views or longer forms beside the page: use Sheet.',
        'Phone-first flows that should rise from the bottom: use ResponsiveDialog.',
        'Non-blocking supplementary content: use Popover.',
        'Long or multi-step work: use a page.',
      ],
    },
    designPrinciples: {
      items: [
        'Every dialog has a DialogTitle (visually hidden if necessary) so it has an accessible name.',
        'Header, body and footer are separate parts so long content scrolls while actions stay visible.',
        'Dismissal is predictable: Escape and the close button always work unless an action is running (`busy`); outside clicks can be turned off for forms with typed values.',
        'One Escape press closes one layer: a control whose own popup is open keeps the press (`escapeGuard`).',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        '`Dialog`, `DialogTrigger` and `DialogClose` are the Radix Dialog Root, Trigger and Close.',
        '`DialogContent` renders in a portal with the overlay and an optional built-in close button.',
        '`footer` is shorthand for a trailing `<DialogFooter>`; the parts still work on their own.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [
        {
          prop: 'size (DialogContent)',
          values: ['sm', 'md', 'lg', 'xl'],
          description:
            'Width 24rem, 32rem (default), 44rem or 60rem, never wider than the viewport minus 2rem. Exposed as `data-size`.',
        },
        {
          prop: 'fullScreenOnPhone (DialogContent)',
          values: ['false', 'true'],
          description:
            'Below 640px the dialog fills the screen with safe-area padding. Exposed as `data-fullscreen-phone`.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Open / closed',
          attribute: 'data-state',
          description: 'From Radix; content enters with the `mn-dialog-in` animation.',
        },
        {
          name: 'Without close button',
          description: '`closeLabel={null}` removes the built-in close button.',
        },
        {
          name: 'Busy',
          attribute: 'data-busy, aria-busy',
          description:
            '`busy` blocks Escape, outside clicks and the built-in close button while an action runs.',
        },
      ],
    },
    interaction: {
      items: [
        'Opening moves focus into the dialog; Tab and Shift+Tab stay inside it.',
        'Escape, the built-in close button or any DialogClose closes it, and focus returns to the trigger.',
        'A click on the overlay closes it, unless `closeOnOutsideClick={false}`.',
        'While `busy`, nothing closes it; disable your own Cancel button as well.',
        'Escape is ignored while focus is on a combobox or menu button whose popup is open (`escapeGuard`, on by default); Radix popups inside the dialog are already stacked layers.',
        'Controlled with `open` + `onOpenChange`, or uncontrolled.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Enter / Space', action: 'On the trigger, opens the dialog.' },
        { keys: 'Tab / Shift+Tab', action: 'Cycles focus within the dialog.' },
        { keys: 'Escape', action: 'Closes the dialog and returns focus to the trigger.' },
      ],
    },
    accessibility: {
      role: 'dialog',
      name: 'DialogTitle; DialogDescription becomes the accessible description.',
      items: [
        'The built-in close button is named by `closeLabel` (default "Close").',
        'Content outside the dialog is hidden from assistive technology while open (Radix).',
        'While `busy`, the content has `aria-busy="true"` and the close button is disabled.',
        'For confirmations of destructive actions use ConfirmDialog, which sets `role="alertdialog"`.',
      ],
    },
    responsive: {
      items: [
        'Width is `min(size, 100vw - 2rem)` and height at most `100dvh - 2rem`, so the dialog fits small screens.',
        'With `fullScreenOnPhone`, below 640px it fills the screen and pads for notches and home bars (`env(safe-area-inset-*)`).',
        'The body scrolls; header and footer stay put.',
      ],
    },
    theme: {
      items: [
        'Raised surface, border and shadow tokens; light, dark and more-contrast themes apply automatically.',
        'The entrance animation uses duration tokens, which are 0ms under `prefers-reduced-motion`.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Confirmation',
          render: () => (
            <Dialog>
              <DialogTrigger asChild>
                <Button variant="danger">Delete project</Button>
              </DialogTrigger>
              <DialogContent size="sm">
                <DialogHeader>
                  <DialogTitle>Delete project?</DialogTitle>
                  <DialogDescription>
                    This removes all tasks and sprints. It cannot be undone.
                  </DialogDescription>
                </DialogHeader>
                <DialogBody>Type the project name to confirm.</DialogBody>
                <DialogFooter>
                  <DialogClose asChild>
                    <Button>Cancel</Button>
                  </DialogClose>
                  <Button variant="danger">Delete</Button>
                </DialogFooter>
              </DialogContent>
            </Dialog>
          ),
          code: `<Dialog>
  <DialogTrigger asChild><Button variant="danger">Delete project</Button></DialogTrigger>
  <DialogContent size="sm">
    <DialogHeader>
      <DialogTitle>Delete project?</DialogTitle>
      <DialogDescription>This removes all tasks and sprints.</DialogDescription>
    </DialogHeader>
    <DialogFooter>
      <DialogClose asChild><Button>Cancel</Button></DialogClose>
      <Button variant="danger">Delete</Button>
    </DialogFooter>
  </DialogContent>
</Dialog>`,
        },
        {
          title: 'Form with a busy save',
          render: () => <RenameDialog />,
          code: `const [saving, setSaving] = useState(false);

<Dialog open={open} onOpenChange={setOpen}>
  <DialogTrigger asChild><Button>Rename board</Button></DialogTrigger>
  <DialogContent
    busy={saving}
    closeOnOutsideClick={false}
    fullScreenOnPhone
    footer={
      <>
        <DialogClose asChild><Button disabled={saving}>Cancel</Button></DialogClose>
        <Button variant="primary" loading={saving} onClick={save}>Save</Button>
      </>
    }
  >
    <DialogHeader>
      <DialogTitle>Rename board</DialogTitle>
      <DialogDescription>Everyone with access sees the new name.</DialogDescription>
    </DialogHeader>
    <DialogBody>
      <Field><FieldLabel>Board name</FieldLabel><Input defaultValue="Q3 launch" /></Field>
    </DialogBody>
  </DialogContent>
</Dialog>`,
        },
      ],
    },
    doDont: {
      do: [
        'Always include a DialogTitle.',
        'Put the confirming action last in the footer.',
        'Set `busy` while saving so a stray Escape cannot interrupt it.',
        'Turn off outside clicks for forms with typed values.',
      ],
      dont: [
        'Do not open a dialog from inside another dialog.',
        'Do not hide the only way to close it.',
        'Do not leave `busy` on after an error; let the person retry or cancel.',
      ],
    },
    related: {
      items: [
        { id: 'confirm-dialog', reason: 'Ready-made confirmation built on Dialog.' },
        { id: 'sheet', reason: 'Side or bottom panel for longer tasks.' },
        {
          id: 'responsive-dialog',
          reason: 'This dialog on wide screens, a bottom sheet on phones.',
        },
        { id: 'popover', reason: 'Non-modal floating content.' },
      ],
    },
    dependencies: {
      packages: ['@radix-ui/react-dialog', 'react'],
      components: [],
      notes: ['Dismissal rules are shared with Sheet through `src/internal/dismiss.ts`.'],
    },
    testing: {
      unit: [
        'Opens, traps focus, closes on Escape and restores focus to the trigger.',
        'Has an accessible name and description; built-in close button is present.',
        '`closeLabel={null}` removes the close button.',
        'No axe violations in the open dialog (jsdom).',
        '`footer` renders a DialogFooter after the children.',
        '`busy` blocks Escape, outside clicks and the close button, and sets aria-busy.',
        '`closeOnOutsideClick={false}` keeps it open on an overlay click; Escape still closes.',
        'Uncontrolled dialogs close on an overlay click by default.',
        'Escape guard: Escape on an expanded combobox does not close it; `escapeGuard={false}` lets it through.',
        'A consumer `onEscapeKeyDown` still runs and its preventDefault is honoured.',
        '`size` and `fullScreenOnPhone` are exposed as data attributes.',
        'Renders on the server (closed).',
      ],
      axe: true,
      ssr: true,
      browser: [
        'Visual baselines: gallery/dialog and overlays/dialog-light (open)',
        'Cross-browser axe: /components/dialog',
      ],
    },
  },
} satisfies ComponentDocs;
