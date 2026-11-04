import {
  Accordion,
  AccordionContent,
  AccordionItem,
  AccordionTrigger,
} from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';

export default {
  id: 'accordion',
  title: 'Accordion',
  summary: 'Stacked sections whose bodies expand and collapse under heading triggers.',
  sections: {
    purpose: {
      summary:
        'Shows a list of headings and reveals the body of one (or several) on demand, so long reference content stays scannable. FAQ in the marketing entry is built on it.',
      useWhen: [
        'Reference content where people look for one item: FAQs, policy sections, help topics.',
        'Optional detail under a summary, such as advanced settings explained in prose.',
      ],
      avoidWhen: [
        'Switching between views of the same object: use Tabs.',
        'Content everyone must read: show it; hiding it costs a click.',
        'Form steps: use a stepper or separate pages.',
      ],
    },
    designPrinciples: {
      items: [
        'Each trigger is a real heading (h3 by default, `headingAs` to change), so items appear in the page outline.',
        'Quiet chrome: hairline dividers, medium-weight labels, one chevron that rotates; no boxes or fills.',
        'Built on Radix Accordion for the keyboard model and ARIA wiring.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Accordion is Radix `Accordion.Root`: `type="single"` (with `collapsible`) or `type="multiple"`, controlled via `value`/`onValueChange` or uncontrolled via `defaultValue`.',
        'AccordionTrigger renders `Header` (as the chosen heading element) and `Trigger` together.',
        'AccordionContent wraps children in `.mn-accordion__body`, which caps line length at `--mn-width-narrow`.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [
        {
          prop: 'type',
          values: ['single', 'multiple'],
          description: 'One open item at a time, or any number.',
        },
        {
          prop: 'headingAs (AccordionTrigger)',
          values: ['h2', 'h3', 'h4'],
          description:
            'Heading level of each trigger. Pick the level below the surrounding heading.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Open',
          attribute: 'data-state="open"',
          description: 'Body shown; chevron rotated 180°.',
        },
        { name: 'Closed', attribute: 'data-state="closed"', description: 'Body removed.' },
        {
          name: 'Disabled',
          attribute: 'data-disabled',
          description: 'Set `disabled` on an item or the root; the trigger cannot toggle.',
        },
      ],
    },
    interaction: {
      items: [
        'Click, Enter or Space on a trigger toggles its item.',
        'With `type="single"`, opening one item closes the other; `collapsible` lets the open item close.',
        'The body animates its height over `--mn-duration-base`; reduced motion removes the animation.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Moves to the next trigger or into an open body.' },
        { keys: 'Enter / Space', action: 'Toggles the focused item.' },
        { keys: 'Arrow Down / Arrow Up', action: 'Moves focus to the next / previous trigger.' },
        { keys: 'Home / End', action: 'Moves focus to the first / last trigger.' },
      ],
    },
    accessibility: {
      role: 'button (inside a heading)',
      name: 'The trigger text.',
      items: [
        'Triggers expose `aria-expanded` and `aria-controls`; bodies are `role="region"` labelled by their trigger.',
        'The chevron is decorative (`aria-hidden`).',
        'The focus ring uses `--mn-color-focus-ring`, which inverse marketing sections remap.',
        'Trigger rows are at least 56px tall, beyond the 24px target size (WCAG 2.5.8).',
      ],
    },
    responsive: {
      items: [
        'Full width of its container at every size; labels wrap, and the chevron stays top-aligned to the first line.',
      ],
    },
    theme: {
      items: [
        'Borders use `--mn-color-border-default`, text `--mn-color-text-default` and `-muted`; light, dark and more-contrast apply automatically.',
        'Open/close animation is disabled under `prefers-reduced-motion: reduce` by the foundation stylesheet.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Single, collapsible',
          stack: true,
          render: () => (
            <Accordion type="single" collapsible defaultValue="billing">
              <AccordionItem value="billing">
                <AccordionTrigger>When am I billed?</AccordionTrigger>
                <AccordionContent>
                  On the first of each month, for the seats in use.
                </AccordionContent>
              </AccordionItem>
              <AccordionItem value="cancel">
                <AccordionTrigger>Can I cancel at any time?</AccordionTrigger>
                <AccordionContent>
                  Yes. Access continues to the end of the paid period.
                </AccordionContent>
              </AccordionItem>
            </Accordion>
          ),
          code: `<Accordion type="single" collapsible defaultValue="billing">
  <AccordionItem value="billing">
    <AccordionTrigger>When am I billed?</AccordionTrigger>
    <AccordionContent>On the first of each month, for the seats in use.</AccordionContent>
  </AccordionItem>
  <AccordionItem value="cancel">
    <AccordionTrigger>Can I cancel at any time?</AccordionTrigger>
    <AccordionContent>Yes. Access continues to the end of the paid period.</AccordionContent>
  </AccordionItem>
</Accordion>`,
        },
      ],
    },
    doDont: {
      do: [
        'Write triggers as the question or topic people scan for.',
        'Set `headingAs` so the outline has no skipped levels.',
      ],
      dont: [
        'Do not nest accordions.',
        'Do not hide the only copy of critical information (prices, legal terms) in a closed item.',
      ],
    },
    related: {
      items: [
        { id: 'tabs', reason: 'Switch between peer views instead of expanding in place.' },
        {
          id: 'marketing-section-blocks',
          reason: 'FAQ is Accordion with marketing defaults (one open, heading per question).',
        },
      ],
    },
    dependencies: {
      packages: ['@radix-ui/react-accordion', 'react'],
      components: [],
    },
    testing: {
      unit: [
        'Triggers sit inside a heading of the requested level.',
        'Enter toggles; Arrow keys, Home and End move between triggers; single mode closes the other item.',
        'No axe violations; renders on the server.',
      ],
      axe: true,
      ssr: true,
      browser: ['Visual and three-browser axe: /__marketing (FAQ)'],
    },
  },
} satisfies ComponentDocs;
