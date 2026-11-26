import { useRef, useState } from 'react';
import { Button, FocusTrap, Input } from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';

function PanelDemo() {
  const [open, setOpen] = useState(false);
  return (
    <div style={{ display: 'grid', gap: 12, width: '100%', maxWidth: 420 }}>
      <Button onClick={() => setOpen(true)} disabled={open}>
        Edit filters
      </Button>
      {open ? (
        <FocusTrap
          role="group"
          aria-label="Filters"
          onEscape={() => setOpen(false)}
          style={{
            display: 'grid',
            gap: 12,
            padding: 16,
            border: '1px solid var(--mn-color-border-default)',
            borderRadius: 'var(--mn-radius-lg)',
          }}
        >
          <Input aria-label="Owner" placeholder="Owner" />
          <Input aria-label="Label" placeholder="Label" />
          <div style={{ display: 'flex', gap: 8, justifyContent: 'flex-end' }}>
            <Button variant="secondary" onClick={() => setOpen(false)}>
              Cancel
            </Button>
            <Button onClick={() => setOpen(false)}>Apply</Button>
          </div>
        </FocusTrap>
      ) : null}
    </div>
  );
}

function InitialFocusDemo() {
  const [open, setOpen] = useState(false);
  const confirmRef = useRef<HTMLButtonElement>(null);
  return (
    <div style={{ display: 'grid', gap: 12 }}>
      <Button variant="secondary" onClick={() => setOpen((o) => !o)}>
        {open ? 'Close editor' : 'Open editor'}
      </Button>
      <FocusTrap
        active={open}
        initialFocus={confirmRef}
        hidden={!open}
        role="group"
        aria-label="Editor actions"
        onEscape={() => setOpen(false)}
        style={{ display: open ? 'flex' : undefined, gap: 8 }}
      >
        <Button variant="secondary" onClick={() => setOpen(false)}>
          Discard
        </Button>
        <Button ref={confirmRef} onClick={() => setOpen(false)}>
          Keep editing
        </Button>
      </FocusTrap>
    </div>
  );
}

export default {
  id: 'focus-trap',
  title: 'FocusTrap',
  summary:
    'Keeps keyboard focus inside a custom surface while it is open, and puts it back where it was afterwards.',
  sections: {
    purpose: {
      summary:
        'Custom popups, inline drawers and full-screen editors that are not built on Dialog let Tab wander behind them. FocusTrap (and `useFocusTrap`) contains focus and returns it. Needed by: rook-support-chat and rookid, which hand-roll focus traps today.',
      useWhen: [
        'A custom overlay or takeover panel that is modal in practice but is not a Dialog, Sheet or Popover.',
        'A full-screen editor or media viewer where Tab must cycle through its own controls.',
      ],
      avoidWhen: [
        'Anything a Dialog, Sheet, Popover or DropdownMenu can do: they already trap focus, handle Escape and label the surface.',
        'Non-modal content on the page: trapping focus there stops keyboard users from reaching the rest of the page.',
      ],
    },
    designPrinciples: {
      items: [
        'Return focus: when the trap turns off or unmounts, focus goes back to the element that had it.',
        'Never strand focus: with nothing focusable inside, the container itself takes focus.',
        'Escape is reported, not acted on: `onEscape` lets the owner decide what closing means.',
        'Nested traps work: only the innermost active trap acts.',
        'Headless: it adds no visual style beyond a focus ring when the container itself has focus.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        '`FocusTrap` renders a `<div class="mn-focus-trap">`; `useFocusTrap(ref, options)` does the same for any element you render.',
        '`getFocusableElements(container)` returns the elements Tab reaches, in tab order.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Migrating from `focus-trap-react`: `active` is the same; `focusTrapOptions.initialFocus` becomes `initialFocus` (a ref, `first` or `container`); `returnFocusOnDeactivate` becomes `returnFocus`; `escapeDeactivates` plus `onDeactivate` becomes `onEscape`.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'initialFocus',
          values: ["'first'", "'container'", 'ref', 'false'],
          description:
            'Where focus goes when the trap turns on. Default: the first focusable element.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Active',
          attribute: 'data-active',
          description: 'Present while the trap holds focus.',
        },
        {
          name: 'Container focused',
          description:
            'When the container takes focus itself it gets `tabindex="-1"` and the Monarch focus ring on `:focus-visible`.',
        },
      ],
    },
    interaction: {
      items: [
        'Turning on: focus moves to `initialFocus` unless it is already inside.',
        'Focus that leaves (a script, a click on something focusable outside) is brought back to the last focused element inside.',
        'Turning off or unmounting: focus returns to the previously focused element if it is still in the page (`returnFocus`, default true).',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Next focusable element; from the last one, wraps to the first.' },
        { keys: 'Shift + Tab', action: 'Previous element; from the first one, wraps to the last.' },
        { keys: 'Escape', action: 'Calls `onEscape` (the trap does not close itself).' },
      ],
    },
    accessibility: {
      role: 'none by default; set `role="dialog"` with `aria-modal` and a name, or `role="group"` with a label',
      name: 'From `aria-label` or `aria-labelledby` when a role is set.',
      items: [
        'A trap is only acceptable on surfaces that are modal: give them a role, a name and a visible way out.',
        'Disabled, hidden, inert and `tabindex="-1"` elements are skipped; positive tabindex values are ordered first, as browsers do.',
        'Checked with axe in jsdom.',
      ],
    },
    responsive: {
      items: ['No layout of its own; it takes whatever size its content and styles give it.'],
    },
    theme: {
      items: [
        'Only a focus ring on the container (`--mn-color-focus-ring`, `--mn-focus-ring-width`), Highlight in forced-colours mode.',
      ],
    },
    examples: {
      items: [
        {
          title: 'An inline panel that holds focus until applied or cancelled',
          stack: true,
          render: () => <PanelDemo />,
        },
        { title: 'Initial focus on a chosen element', render: () => <InitialFocusDemo /> },
      ],
    },
    doDont: {
      do: [
        'Provide a visible close or cancel control inside the trap, and wire `onEscape`.',
        'Label the surface with a role and an accessible name.',
      ],
      dont: [
        'Do not wrap a Dialog, Sheet or Popover in a FocusTrap: they already trap focus.',
        'Do not trap focus in content that is not modal.',
      ],
    },
    related: {
      items: [
        {
          id: 'dialog',
          reason: 'The default modal surface; traps focus, labels itself and handles Escape.',
        },
        {
          id: 'roving-focus-group',
          reason: 'Arrow-key movement inside a composite widget, without trapping Tab.',
        },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: [],
    },
    testing: {
      unit: [
        'Initial focus on the first element; Tab and Shift+Tab wrap; axe.',
        'Focus returns to the opener on unmount, and not with `returnFocus={false}`.',
        'Escaped focus is brought back; `onEscape` is called and nothing closes by itself.',
        'Container and ref initial focus; an empty trap keeps focus on the container.',
        'Inactive traps do nothing; nested traps: only the innermost acts and focus returns outwards.',
        'The hook on any element; `getFocusableElements` order and filtering; server rendering.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
