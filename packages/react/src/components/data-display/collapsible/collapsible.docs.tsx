import {
  Button,
  Collapsible,
  CollapsibleContent,
  CollapsibleTrigger,
} from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';

export default {
  id: 'collapsible',
  title: 'Collapsible',
  summary: 'One show/hide section with an animated height that respects reduced motion.',
  sections: {
    purpose: {
      summary:
        'Shows and hides one block of content behind a button: advanced settings, a details block, an expandable panel section. Needed by: Rook ID and Rook Links (shadcn/ui copies in `rookid/src/components/ui/collapsible.tsx`, `rookid/components/ui/collapsible.tsx` and `Rook Links/src/components/ui/collapsible.tsx`), Rook Tickets (`src/components/tickets/detail/PanelSection.tsx`) and Rook Calendar (`src/components/booking-pages/controls.tsx`), which build their own collapsible sections.',
      useWhen: [
        'A single optional section that most people do not need ("Advanced settings").',
        'A panel section that people collapse to save space, each section independent of the others.',
      ],
      avoidWhen: [
        'A set of related sections with arrow-key movement between headers, or only one open at a time: use Accordion.',
        'Long text that should show a few lines and then "Show more": use TruncatedText.',
        'Content that should float over the page: use Popover.',
      ],
    },
    designPrinciples: {
      items: [
        'WAI-ARIA disclosure: a real button with `aria-expanded` and `aria-controls`, nothing more.',
        'Closed means gone for everyone: the content has the `hidden` attribute, so it is out of the tab order and the accessibility tree.',
        'Motion is a courtesy: the height animation runs only after the first render and is removed under reduced motion.',
        'Children stay mounted, so typing inside a section is never lost by closing it.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Three parts: `Collapsible` (root `<div>`), `CollapsibleTrigger` (a `<button>`, or the child with `asChild`) and `CollapsibleContent` (`<div>`). All forward refs.',
        'Calling `event.preventDefault()` in the trigger’s `onClick` stops the toggle.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Migrating from shadcn/ui: the names and props match Radix Collapsible (`open`, `defaultOpen`, `onOpenChange`, `disabled`, `asChild` on the trigger), so changing the import is enough. Drop the `animate-collapsible-down/up` classes; the animation is built in.',
        'Collapsible vs Accordion: Accordion is several items with headings, arrow keys between headers and an optional single-open mode; Collapsible is one independent section.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'CollapsibleTrigger chevron',
          values: ['true', 'false'],
          description: 'Adds a decorative chevron that turns when open. Default `false`.',
        },
        {
          prop: 'CollapsibleTrigger asChild',
          values: ['true', 'false'],
          description:
            'Uses the child element (for example a Button) as the trigger, keeping its look. The built-in trigger is a quiet text button (`data-plain`).',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Open / closed',
          attribute: 'data-state="open|closed"',
          description:
            'On the root, trigger and content. The trigger also has `aria-expanded`; closed content has `hidden`.',
        },
        {
          name: 'Animating',
          attribute: 'data-animate',
          description:
            'Set on the content after the first render; the height animates between 0 and the measured height (`--_height`).',
        },
        {
          name: 'Disabled',
          attribute: 'data-disabled, disabled',
          description: 'The trigger is a disabled button; the content keeps its current state.',
        },
      ],
    },
    interaction: {
      items: [
        'Activating the trigger toggles the content and calls `onOpenChange`.',
        'While closing, the content stays visible until its animation ends, then becomes `hidden`. Without an animation (reduced motion) it hides at once.',
        'Content that starts open does not animate in.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Moves to the trigger, then into the content when it is open.' },
        { keys: 'Enter / Space', action: 'Toggles the content.' },
      ],
    },
    accessibility: {
      role: 'button (trigger) controlling a region of content',
      name: 'The trigger’s text; it should name the content ("Advanced settings").',
      items: [
        '`aria-expanded` and `aria-controls` connect the trigger to the content.',
        'Closed content has `hidden`, so screen readers and Tab skip it.',
        'The chevron is `aria-hidden`; the expanded state is announced from `aria-expanded`.',
        'Focus stays on the trigger when toggling.',
      ],
    },
    responsive: {
      items: [
        'Takes the width of its container; nothing is fixed.',
        'On coarse pointers the built-in trigger is at least 44px tall.',
      ],
    },
    theme: {
      items: [
        'The built-in trigger uses `--mn-color-text-default` and `--mn-color-text-link` on hover; the chevron `--mn-color-text-muted`.',
        'Animation timing uses `--mn-duration-base` and `--mn-ease-standard`; reduced motion removes it.',
        'In forced colours the trigger and chevron use `ButtonText`, a disabled trigger `GrayText`.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Advanced settings',
          stack: true,
          render: () => (
            <Collapsible>
              <CollapsibleTrigger chevron>Advanced settings</CollapsibleTrigger>
              <CollapsibleContent>
                <p>Retry failed deliveries up to five times with a growing delay.</p>
              </CollapsibleContent>
            </Collapsible>
          ),
        },
        {
          title: 'Button trigger, open by default',
          stack: true,
          render: () => (
            <Collapsible defaultOpen>
              <CollapsibleTrigger asChild>
                <Button size="sm">Toggle details</Button>
              </CollapsibleTrigger>
              <CollapsibleContent>
                <p>Created on 3 March by Sam Lee. Last edited yesterday.</p>
              </CollapsibleContent>
            </Collapsible>
          ),
        },
        {
          title: 'Disabled',
          render: () => (
            <Collapsible disabled>
              <CollapsibleTrigger chevron>Billing history</CollapsibleTrigger>
              <CollapsibleContent>No invoices yet.</CollapsibleContent>
            </Collapsible>
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Write a trigger label that names the content.',
        'Use `asChild` with a Button when the trigger should look like an action.',
        'Keep important information outside the collapsed content.',
      ],
      dont: [
        'Don’t stack many Collapsibles to build an accordion; use Accordion.',
        'Don’t put the only way to complete a task inside a closed section.',
        'Don’t add your own height transition; it fights the built-in one.',
      ],
    },
    related: {
      items: [
        { id: 'accordion', reason: 'Several sections with headings and arrow-key navigation.' },
        { id: 'truncated-text', reason: 'Long text clamped to a few lines with "Show more".' },
        { id: 'toggle-block', reason: 'A collapsible block inside the block editor.' },
        { id: 'popover', reason: 'Content that floats over the page instead of pushing it down.' },
      ],
    },
    dependencies: { packages: ['@radix-ui/react-slot', 'react'], components: [] },
    testing: {
      unit: [
        'Follows the disclosure pattern: `aria-expanded`, `aria-controls`, hidden closed content out of the tab order.',
        'Opens and closes with click, Enter and Space, calling `onOpenChange`.',
        'Keeps children mounted, so input survives closing.',
        'Works controlled.',
        'Does not toggle when disabled.',
        'Renders the child as the trigger with `asChild`; `preventDefault` in `onClick` stops the toggle.',
        'Throws a clear error when a part is used outside Collapsible.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
