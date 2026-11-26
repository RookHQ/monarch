import {
  Button,
  Drawer,
  DrawerBody,
  DrawerClose,
  DrawerContent,
  DrawerDescription,
  DrawerFooter,
  DrawerHeader,
  DrawerTitle,
  DrawerTrigger,
} from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';

export default {
  id: 'drawer',
  title: 'Drawer',
  summary: 'Bottom drawer for phones with a grab handle and drag-to-close, built on Sheet.',
  sections: {
    purpose: {
      summary:
        'Shows a short task or a set of actions in a panel that rises from the bottom of a phone screen, and closes with a drag down, the close button or Escape. Needed by: Rook ID and Rook Links, which ship vaul-based shadcn/ui copies (`rookid/src/components/ui/drawer.tsx`, `rookid/components/ui/drawer.tsx`, `Rook Links/src/components/ui/drawer.tsx`).',
      useWhen: [
        'A phone layout needs a short form, a list of actions or a picker within thumb reach.',
        'People expect to swipe the panel away, as with native bottom sheets.',
      ],
      avoidWhen: [
        'The panel attaches to the side or top, or is mainly for desktop: use Sheet.',
        'One component should be a centred dialog on desktop and a bottom sheet on phones: use ResponsiveDialog.',
        'Long or multi-step forms: use a page or WizardDialog.',
      ],
    },
    designPrinciples: {
      items: [
        'Drawer vs Sheet: a Drawer is always a bottom Sheet, plus a real grab handle and drag-to-close. Sheet is the general edge panel (any side, any screen) whose bottom handle is only decoration.',
        'Dragging is a shortcut, never the only way out: the close button and Escape always work.',
        'Only the handle and header start a drag, so the body scrolls and its controls work as usual.',
        'It is a modal dialog: focus trap, scroll lock and focus return come from Sheet.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Parts: `Drawer` (root, controlled or uncontrolled), `DrawerTrigger`, `DrawerContent`, `DrawerHeader`, `DrawerTitle`, `DrawerDescription`, `DrawerBody`, `DrawerFooter`, `DrawerClose`.',
        '`DrawerContent` takes every SheetContent prop except `side` and `handle` (`size`, `busy`, `closeLabel`, `footer`, `closeOnOutsideClick`, `escapeGuard`) and forwards its ref.',
        '`shouldCloseDrawer(distance, durationMs, height)` is the rule a drag uses, exported for tests.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Migrating from shadcn/ui (vaul): the part names match. `shouldScaleBackground`, `snapPoints` and `direction` have no equivalent; drop them. Remove the hand-drawn handle `<div>` inside `DrawerContent`; the handle is built in.',
        'A drag closes the drawer when it travels a quarter of the drawer’s height (at least 80px) or is flicked faster than 0.5px per millisecond; otherwise it snaps back.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'size',
          values: ['sm', 'md', 'lg', 'xl', 'full'],
          description:
            'Maximum height: 40, 60 (default), 80 or 92% of the screen, or all of it. From Sheet; `data-size`.',
        },
        {
          prop: 'dragToClose',
          values: ['true', 'false'],
          description:
            'Drag the handle or header down to close. Default `true`; `data-drag` when on.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Open / closed',
          attribute: 'data-state="open|closed"',
          description: 'From Radix Dialog, on the content and the trigger.',
        },
        {
          name: 'Dragging',
          attribute: 'data-dragging',
          description:
            'The panel follows the pointer down with an inline `translateY`; no transition.',
        },
        {
          name: 'Busy',
          attribute: 'data-busy, aria-busy="true"',
          description: 'Escape, outside clicks, the close button and dragging no longer close it.',
        },
      ],
    },
    interaction: {
      items: [
        'Pressing on the handle or header and moving down drags the panel; releasing past the threshold closes it, otherwise it slides back.',
        'Drags never start on buttons, links or fields inside the header, nor anywhere in the body.',
        'Upward movement is ignored; the panel never rises above its open position.',
        'Clicking the overlay closes it unless `closeOnOutsideClick` is false.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Escape', action: 'Closes the drawer and returns focus to the trigger.' },
        { keys: 'Tab / Shift+Tab', action: 'Moves between controls inside; focus cannot leave.' },
        { keys: 'Enter / Space', action: 'Activates the close button or DrawerClose.' },
      ],
      notes: [
        'Dragging is pointer-only by design; the keyboard always has Escape and the close button.',
      ],
    },
    accessibility: {
      role: 'dialog (aria-modal)',
      name: 'DrawerTitle; DrawerDescription becomes the description.',
      items: [
        'The handle is `aria-hidden`: it is a pointer affordance, and the close button is the accessible equivalent.',
        'Focus moves into the drawer on open and back to the trigger on close.',
        'Content behind is inert and does not scroll while it is open.',
      ],
    },
    responsive: {
      items: [
        'Full width and attached to the bottom at every width; it keeps clear of the home bar with the safe-area inset.',
        'On coarse pointers the handle area is 44px tall.',
        'On wide screens consider ResponsiveDialog, which becomes a centred dialog.',
      ],
    },
    theme: {
      items: [
        'Surface, border and shadow come from Sheet (`--mn-color-bg-raised`, `--mn-shadow-xl`); the handle bar uses `--mn-color-border-strong`.',
        'The snap-back uses `--mn-duration-base`; reduced motion removes it and Sheet’s slide-in.',
        'In forced colours the handle bar is `CanvasText`.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Actions drawer',
          render: () => (
            <Drawer>
              <DrawerTrigger asChild>
                <Button>Open drawer</Button>
              </DrawerTrigger>
              <DrawerContent>
                <DrawerHeader>
                  <DrawerTitle>Share link</DrawerTitle>
                  <DrawerDescription>Anyone with the link can view this page.</DrawerDescription>
                </DrawerHeader>
                <DrawerBody>
                  <p>Drag the handle down, press Escape or use Close to dismiss.</p>
                </DrawerBody>
                <DrawerFooter>
                  <DrawerClose asChild>
                    <Button>Cancel</Button>
                  </DrawerClose>
                  <Button variant="primary">Copy link</Button>
                </DrawerFooter>
              </DrawerContent>
            </Drawer>
          ),
        },
        {
          title: 'Tall drawer without drag',
          render: () => (
            <Drawer>
              <DrawerTrigger asChild>
                <Button>Choose a time</Button>
              </DrawerTrigger>
              <DrawerContent size="lg" dragToClose={false}>
                <DrawerHeader>
                  <DrawerTitle>Choose a time</DrawerTitle>
                </DrawerHeader>
                <DrawerBody>
                  <p>09:00, 09:30, 10:00, 10:30, 11:00 and 11:30 are open.</p>
                </DrawerBody>
              </DrawerContent>
            </Drawer>
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Give every drawer a DrawerTitle.',
        'Keep the main action in DrawerFooter so it stays in reach while the body scrolls.',
        'Set `busy` while saving so a stray drag cannot close it.',
      ],
      dont: [
        'Don’t make dragging the only way to close it.',
        'Don’t use a Drawer for desktop side panels; use Sheet.',
        'Don’t put drag-sensitive widgets (sliders, maps) in the header.',
      ],
    },
    related: {
      items: [
        { id: 'sheet', reason: 'The general edge panel Drawer is built on; any side, any screen.' },
        {
          id: 'responsive-dialog',
          reason: 'A centred dialog on desktop that becomes a bottom sheet on phones.',
        },
        { id: 'dialog', reason: 'A centred modal for short tasks.' },
        { id: 'peek-panel', reason: 'A non-modal side panel that previews a record.' },
      ],
    },
    dependencies: { packages: ['react'], components: ['sheet'] },
    testing: {
      unit: [
        'Opens as a modal bottom sheet with a hidden handle; Escape closes and returns focus.',
        'Traps focus; the close button and DrawerClose close it and call `onOpenChange(false)`.',
        'Closes when the handle is dragged past the threshold, following the pointer while dragging.',
        'Snaps back after a short, slow drag from the header.',
        'Closes on a quick flick; ignores drags from the body and while busy.',
        '`shouldCloseDrawer` distance and velocity rules.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
