import {
  Button,
  DescriptionItem,
  DescriptionList,
  Field,
  FieldLabel,
  Input,
  Sheet,
  SheetBody,
  SheetClose,
  SheetContent,
  SheetDescription,
  SheetFooter,
  SheetHeader,
  SheetTitle,
  SheetTrigger,
} from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';
import { FilterIcon } from '../../../internal/docs-demo.js';

export default {
  id: 'sheet',
  title: 'Sheet',
  summary: 'Side or bottom panel with focus trap, scroll lock, sizes and safe areas.',
  sections: {
    purpose: {
      summary:
        'A modal panel attached to an edge of the screen: a record’s details next to the list it came from, filters, a longer form, or navigation on a phone. It keeps the page in view while focus stays in the panel.',
      useWhen: [
        'Showing a record’s details without leaving the list.',
        'A form or settings panel too long for a dialog.',
        'Filters or navigation on narrow screens (left or bottom).',
      ],
      avoidWhen: [
        'A short decision: use Dialog or ConfirmDialog.',
        'Content the person needs alongside the page for a long time: use a SplitLayout pane.',
        'A centred dialog that should become a bottom sheet on phones: use ResponsiveDialog.',
      ],
    },
    designPrinciples: {
      items: [
        'A sheet is a dialog: one title, focus trapped inside, Escape closes it, focus returns to the trigger.',
        'Header and footer stay put; only SheetBody scrolls, so actions are always reachable.',
        'Side sheets never exceed the screen width, and every edge that touches the screen pads for notches and home bars.',
        'Calm surface: the raised background, one border on the inner edge and the standard overlay.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        '`Sheet`, `SheetTrigger` and `SheetClose` are the Radix Dialog Root, Trigger and Close.',
        '`SheetContent` renders in a portal with the overlay and an optional built-in close button.',
        'SheetHeader, SheetTitle, SheetDescription, SheetBody and SheetFooter build on the Dialog parts, so typography and spacing match Dialog.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        '`busy`, `closeOnOutsideClick`, `escapeGuard` and `footer` behave exactly as on DialogContent.',
        'Pass `modal={false}` to `Sheet` for a panel that leaves the page interactive (no overlay, no scroll lock, no focus trap).',
      ],
    },
    variants: {
      items: [
        {
          prop: 'side',
          values: ['right', 'left', 'top', 'bottom'],
          description: 'The edge it is attached to; default `right`. Exposed as `data-side`.',
        },
        {
          prop: 'size',
          values: ['sm', 'md', 'lg', 'xl', 'full'],
          description:
            'Left/right: width 20, 28 (default), 36 or 48rem, or the full screen. Top/bottom: maximum height 40, 60, 80 or 92% of the screen, or all of it. Exposed as `data-size`.',
        },
        {
          prop: 'handle',
          values: ['true', 'false'],
          description: 'Decorative grab handle; on by default for bottom sheets only.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Open / closed',
          attribute: 'data-state',
          description: 'From Radix; the panel slides in from its edge.',
        },
        {
          name: 'Busy',
          attribute: 'data-busy, aria-busy',
          description:
            'Escape, outside clicks and the close button do nothing while an action runs.',
        },
        {
          name: 'Without close button',
          description: '`closeLabel={null}`; give the person another way to close it.',
        },
      ],
    },
    interaction: {
      items: [
        'Opening moves focus into the sheet; Tab and Shift+Tab stay inside it.',
        'The page behind does not scroll while a modal sheet is open.',
        'Escape, the close button, a SheetClose or a click on the overlay closes it; focus returns to the trigger.',
        '`closeOnOutsideClick={false}` keeps it open on an overlay click (forms with typed values).',
        'Controlled with `open` + `onOpenChange` (for example from a URL parameter), or uncontrolled.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Enter / Space', action: 'On the trigger, opens the sheet.' },
        { keys: 'Tab / Shift+Tab', action: 'Cycles focus within the sheet.' },
        { keys: 'Escape', action: 'Closes the sheet and returns focus to the trigger.' },
      ],
    },
    accessibility: {
      role: 'dialog (aria-modal)',
      name: 'SheetTitle; SheetDescription becomes the accessible description.',
      items: [
        'Content outside the sheet is hidden from assistive technology while open (Radix).',
        'The built-in close button is named by `closeLabel` (default "Close") and is disabled while busy.',
        'The grab handle is `aria-hidden`; it is not a control.',
      ],
    },
    responsive: {
      items: [
        'Side sheets are `min(size, 100vw)` wide: on a phone a `md` sheet fills the width.',
        'Top and bottom sheets span the width and grow with their content up to the size cap; the body scrolls beyond that.',
        'Padding follows `env(safe-area-inset-*)` on every edge that touches the screen.',
        'Height uses `dvh`, so mobile browser bars do not hide the footer.',
      ],
    },
    theme: {
      items: [
        'Raised surface, subtle border, overlay scrim and shadow tokens; light, dark and more-contrast themes apply automatically.',
        'The slide-in uses duration tokens, which are 0ms under `prefers-reduced-motion`.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Record details',
          render: () => (
            <Sheet>
              <SheetTrigger asChild>
                <Button>Open details</Button>
              </SheetTrigger>
              <SheetContent>
                <SheetHeader>
                  <SheetTitle>Invoice INV-1042</SheetTitle>
                  <SheetDescription>Sent on 12 March to the billing contact.</SheetDescription>
                </SheetHeader>
                <SheetBody>
                  <DescriptionList>
                    <DescriptionItem term="Status">Awaiting payment</DescriptionItem>
                    <DescriptionItem term="Due">26 March</DescriptionItem>
                    <DescriptionItem term="Owner">Priya Raman</DescriptionItem>
                  </DescriptionList>
                </SheetBody>
                <SheetFooter>
                  <SheetClose asChild>
                    <Button>Close</Button>
                  </SheetClose>
                  <Button variant="primary">Send reminder</Button>
                </SheetFooter>
              </SheetContent>
            </Sheet>
          ),
          code: `<Sheet>
  <SheetTrigger asChild><Button>Open details</Button></SheetTrigger>
  <SheetContent>
    <SheetHeader>
      <SheetTitle>Invoice INV-1042</SheetTitle>
      <SheetDescription>Sent on 12 March to the billing contact.</SheetDescription>
    </SheetHeader>
    <SheetBody>…</SheetBody>
    <SheetFooter>
      <SheetClose asChild><Button>Close</Button></SheetClose>
      <Button variant="primary">Send reminder</Button>
    </SheetFooter>
  </SheetContent>
</Sheet>`,
        },
        {
          title: 'Filters from the left, outside clicks locked',
          render: () => (
            <Sheet>
              <SheetTrigger asChild>
                <Button leadingIcon={<FilterIcon />}>Filters</Button>
              </SheetTrigger>
              <SheetContent
                side="left"
                size="sm"
                closeOnOutsideClick={false}
                footer={
                  <>
                    <SheetClose asChild>
                      <Button>Cancel</Button>
                    </SheetClose>
                    <Button variant="primary">Apply</Button>
                  </>
                }
              >
                <SheetHeader>
                  <SheetTitle>Filters</SheetTitle>
                </SheetHeader>
                <SheetBody>
                  <Field>
                    <FieldLabel>Assignee</FieldLabel>
                    <Input placeholder="Anyone" />
                  </Field>
                </SheetBody>
              </SheetContent>
            </Sheet>
          ),
          code: `<SheetContent side="left" size="sm" closeOnOutsideClick={false} footer={…}>
  <SheetHeader><SheetTitle>Filters</SheetTitle></SheetHeader>
  <SheetBody>…</SheetBody>
</SheetContent>`,
        },
        {
          title: 'Bottom sheet',
          render: () => (
            <Sheet>
              <SheetTrigger asChild>
                <Button>Share</Button>
              </SheetTrigger>
              <SheetContent side="bottom" size="sm">
                <SheetHeader>
                  <SheetTitle>Share board</SheetTitle>
                  <SheetDescription>Anyone with the link can view.</SheetDescription>
                </SheetHeader>
                <SheetBody>
                  <Field>
                    <FieldLabel>Link</FieldLabel>
                    <Input readOnly value="https://example.com/b/q3-launch" />
                  </Field>
                </SheetBody>
              </SheetContent>
            </Sheet>
          ),
          code: `<SheetContent side="bottom" size="sm">
  <SheetHeader>
    <SheetTitle>Share board</SheetTitle>
    <SheetDescription>Anyone with the link can view.</SheetDescription>
  </SheetHeader>
  <SheetBody>…</SheetBody>
</SheetContent>`,
        },
      ],
    },
    doDont: {
      do: [
        'Always include a SheetTitle (wrap it in VisuallyHidden if the design has none).',
        'Put actions in SheetFooter or `footer` so they stay visible.',
        'Set `busy` while saving, and lock outside clicks on forms.',
      ],
      dont: [
        'Do not stack a sheet on top of another sheet.',
        'Do not use a full-screen sheet on desktop where a page would do.',
        'Do not rely on the grab handle: swipe-to-dismiss is not implemented.',
      ],
    },
    related: {
      items: [
        { id: 'dialog', reason: 'Centred modal for short tasks; Sheet builds on its parts.' },
        {
          id: 'responsive-dialog',
          reason: 'A Dialog on wide screens that becomes a bottom sheet on phones.',
        },
        {
          id: 'split-layout',
          reason: 'A persistent side pane that is part of the page, not modal.',
        },
      ],
    },
    dependencies: {
      packages: ['@radix-ui/react-dialog', 'react'],
      components: ['dialog'],
      notes: [
        'Reuses the `.mn-overlay` scrim and `.mn-dialog__close` button styles from dialog.css.',
        'Dismissal rules are shared with Dialog through `src/internal/dismiss.ts`.',
      ],
    },
    testing: {
      unit: [
        'Opens from its trigger, traps focus, closes on Escape and restores focus; axe passes on the open sheet.',
        'Opens from the keyboard; a SheetClose closes it.',
        'Locks page scroll while open.',
        'Closes on an overlay click unless `closeOnOutsideClick={false}`.',
        '`busy` blocks every dismissal and disables the close button.',
        'Controlled through `open` + `onOpenChange`; `side` and `size` are exposed as data attributes.',
        'Grab handle on bottom sheets only, removable with `handle={false}`.',
        '`footer` prop and `closeLabel={null}`.',
        'Non-modal sheets render no overlay and no scroll lock.',
        'Renders on the server (closed and open).',
      ],
      axe: true,
      ssr: true,
      browser: [
        'Visual baselines: gallery/sheet, overlays/sheet-light (open, 1280) and overlays/sheet-light-390 (open, phone)',
        'Cross-browser axe: /components/sheet',
      ],
    },
  },
} satisfies ComponentDocs;
