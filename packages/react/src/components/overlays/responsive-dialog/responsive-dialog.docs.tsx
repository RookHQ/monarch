import {
  Button,
  DialogBody,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  Field,
  FieldLabel,
  Input,
  ResponsiveDialog,
  ResponsiveDialogClose,
  ResponsiveDialogContent,
  ResponsiveDialogTrigger,
} from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';

export default {
  id: 'responsive-dialog',
  title: 'ResponsiveDialog',
  summary: 'A Dialog on wide screens and a bottom sheet on phones, switched in CSS.',
  sections: {
    purpose: {
      summary:
        'One dialog that suits both screens: centred with a margin on desktop, rising from the bottom edge on a phone where the thumb can reach its actions. Same content, same focus trap, same Escape.',
      useWhen: [
        'A short form or picker used on both desktop and phones.',
        'Public or mobile-first pages where a centred dialog feels cramped on a phone.',
      ],
      avoidWhen: [
        'Desktop-only admin screens: use Dialog.',
        'A panel that should always come from a side: use Sheet.',
        'A confirmation: use ConfirmDialog.',
      ],
    },
    designPrinciples: {
      items: [
        'One API and one DOM for both layouts; nothing is rendered twice.',
        'The layout is chosen by a CSS media query, not by `matchMedia` in JavaScript, so the server render, the first client render and every later render agree: no hydration mismatch and no flash of the wrong layout.',
        'Behaviour never changes with the layout: the same focus trap, Escape, scroll lock and focus return in both.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        '`ResponsiveDialog`, `ResponsiveDialogTrigger` and `ResponsiveDialogClose` are Dialog, DialogTrigger and DialogClose.',
        '`ResponsiveDialogContent` is DialogContent with the `mn-responsive-dialog` class, `sheetBelow` and a grab handle.',
        'Compose the content with the Dialog parts: DialogHeader, DialogTitle, DialogDescription, DialogBody and DialogFooter.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Every DialogContent prop is accepted (`size`, `footer`, `busy`, `closeOnOutsideClick`, `escapeGuard`, `closeLabel`) except `fullScreenOnPhone`.',
        '`size` applies to the dialog layout only; the sheet always spans the screen width.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'sheetBelow',
          values: ['sm', 'md'],
          description:
            'Below 640px (`sm`, default) or 768px (`md`) it is a bottom sheet. Exposed as `data-sheet-below`.',
        },
        {
          prop: 'size',
          values: ['sm', 'md', 'lg', 'xl'],
          description: 'Dialog width on wide screens, as on Dialog.',
        },
        {
          prop: 'handle',
          values: ['true', 'false'],
          description: 'Decorative grab handle in the sheet layout; default `true`.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Open / closed',
          attribute: 'data-state',
          description: 'From Radix. The dialog fades and scales in; the sheet slides up.',
        },
        {
          name: 'Busy',
          attribute: 'data-busy, aria-busy',
          description: 'Nothing dismisses it while an action runs.',
        },
      ],
    },
    interaction: {
      items: [
        'Opening moves focus inside; Tab and Shift+Tab stay inside; the page does not scroll.',
        'Escape, the close button, a ResponsiveDialogClose or an overlay click closes it, and focus returns to the trigger.',
        '`closeOnOutsideClick={false}` keeps forms with typed values open on a stray tap.',
        'Rotating or resizing the screen switches the layout immediately, without re-mounting or losing input.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Enter / Space', action: 'On the trigger, opens it.' },
        { keys: 'Tab / Shift+Tab', action: 'Cycles focus within it.' },
        { keys: 'Escape', action: 'Closes it and returns focus to the trigger.' },
      ],
    },
    accessibility: {
      role: 'dialog (aria-modal)',
      name: 'DialogTitle; DialogDescription becomes the accessible description.',
      items: [
        'The role and name are the same in both layouts, so screen reader users get one consistent experience.',
        'The grab handle is `aria-hidden` and is not a control.',
      ],
    },
    responsive: {
      items: [
        'At and above the breakpoint: the centred Dialog, `min(size, 100vw - 2rem)` wide.',
        'Below it: full width, anchored to the bottom, rounded top corners, at most the screen height minus 2rem and the top safe area.',
        'The sheet pads for the home bar and side notches (`env(safe-area-inset-*)`).',
        'Swipe-to-dismiss is not implemented; the handle is a visual cue only.',
      ],
    },
    theme: {
      items: [
        'Uses the Dialog surface, border, overlay and shadow tokens; light, dark and more-contrast themes apply automatically.',
        'Both entrance animations use duration tokens, which are 0ms under `prefers-reduced-motion`.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Invite form',
          render: () => (
            <ResponsiveDialog>
              <ResponsiveDialogTrigger asChild>
                <Button>Invite people</Button>
              </ResponsiveDialogTrigger>
              <ResponsiveDialogContent closeOnOutsideClick={false}>
                <DialogHeader>
                  <DialogTitle>Invite people</DialogTitle>
                  <DialogDescription>They get an email with a link to join.</DialogDescription>
                </DialogHeader>
                <DialogBody>
                  <Field>
                    <FieldLabel>Email address</FieldLabel>
                    <Input type="email" placeholder="name@example.com" />
                  </Field>
                </DialogBody>
                <DialogFooter>
                  <ResponsiveDialogClose asChild>
                    <Button>Cancel</Button>
                  </ResponsiveDialogClose>
                  <Button variant="primary">Send invite</Button>
                </DialogFooter>
              </ResponsiveDialogContent>
            </ResponsiveDialog>
          ),
          code: `<ResponsiveDialog>
  <ResponsiveDialogTrigger asChild><Button>Invite people</Button></ResponsiveDialogTrigger>
  <ResponsiveDialogContent closeOnOutsideClick={false}>
    <DialogHeader>
      <DialogTitle>Invite people</DialogTitle>
      <DialogDescription>They get an email with a link to join.</DialogDescription>
    </DialogHeader>
    <DialogBody>
      <Field><FieldLabel>Email address</FieldLabel><Input type="email" /></Field>
    </DialogBody>
    <DialogFooter>
      <ResponsiveDialogClose asChild><Button>Cancel</Button></ResponsiveDialogClose>
      <Button variant="primary">Send invite</Button>
    </DialogFooter>
  </ResponsiveDialogContent>
</ResponsiveDialog>`,
        },
      ],
    },
    doDont: {
      do: [
        'Keep the content short enough to read on a phone.',
        'Put actions in DialogFooter so they sit at the bottom of the sheet, within thumb reach.',
      ],
      dont: [
        'Do not switch between Dialog and Sheet yourself with `matchMedia`: it renders the wrong layout on the server and flashes on hydration.',
        'Do not rely on the handle for dismissal.',
      ],
    },
    related: {
      items: [
        { id: 'dialog', reason: 'The wide-screen layout and the parts used inside.' },
        { id: 'sheet', reason: 'A panel from any edge, at every width.' },
        { id: 'confirm-dialog', reason: 'Confirmations with pending and error states.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['dialog'],
    },
    testing: {
      unit: [
        'Opens as a dialog, traps focus, closes on Escape and restores focus; axe passes on the open content.',
        'Passes Dialog options through (`sheetBelow`, `size`, `busy`, `footer`).',
        'Renders a decorative, `aria-hidden` handle, removable with `handle={false}`.',
        'Controlled open state; `closeOnOutsideClick={false}` keeps it open on an overlay click.',
        'Never calls `matchMedia`; the stylesheet holds the breakpoints.',
        'Renders on the server and hydrates with no recoverable errors and no console errors.',
        'Renders open on the server without touching browser globals.',
      ],
      axe: true,
      ssr: true,
      browser: [
        'Visual baselines: gallery/responsive-dialog, overlays/responsive-dialog-light (open, 1280, dialog layout) and overlays/responsive-dialog-light-390 (open, phone, sheet layout)',
        'Cross-browser axe: /components/responsive-dialog',
      ],
    },
  },
} satisfies ComponentDocs;
