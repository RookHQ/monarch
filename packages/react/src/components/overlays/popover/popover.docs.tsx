import { Button, Checkbox, Popover, PopoverContent, PopoverTrigger } from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';

export default {
  id: 'popover',
  title: 'Popover',
  summary: 'Non-modal floating panel.',
  sections: {
    purpose: {
      summary:
        'A non-modal floating panel for filters, pickers and quick edits, anchored to a trigger.',
      useWhen: ['Interactive content that belongs to one control, such as a filter panel.'],
      avoidWhen: [
        'Plain descriptive text on hover: use Tooltip.',
        'A list of actions: use DropdownMenu.',
        'A blocking decision: use Dialog.',
      ],
    },
    designPrinciples: {
      items: ['Give the content an accessible name with `aria-label` or a heading.'],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Root, Trigger, Anchor and Close are the Radix Popover parts.',
        'PopoverContent defaults to `align="start"` and `sideOffset={6}` and renders in a portal.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: { items: [], notes: ['Popover has no visual variants.'] },
    states: {
      items: [
        {
          name: 'Open / closed',
          attribute: 'data-state',
          description: 'From Radix; opens with `mn-pop-in`.',
        },
      ],
    },
    interaction: {
      items: [
        'Clicking the trigger toggles the panel.',
        'Escape or a click outside closes it and focus returns to the trigger.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Enter / Space', action: 'On the trigger, opens the popover.' },
        { keys: 'Escape', action: 'Closes it and returns focus to the trigger.' },
      ],
    },
    accessibility: {
      role: 'dialog',
      name: '`aria-label` on PopoverContent.',
      items: ['The trigger exposes `aria-haspopup="dialog"` and `aria-expanded`.'],
    },
    responsive: {
      items: ['Width fits content up to `min(24rem, 100vw - 2rem)`.'],
    },
    theme: {
      items: ['Semantic surface, border and shadow tokens; themes apply automatically.'],
    },
    examples: {
      items: [
        {
          title: 'Filters',
          render: () => (
            <Popover>
              <PopoverTrigger asChild>
                <Button>Filters</Button>
              </PopoverTrigger>
              <PopoverContent aria-label="Filters">
                <Checkbox label="Only mine" defaultChecked />
                <Checkbox label="Unassigned" />
              </PopoverContent>
            </Popover>
          ),
        },
      ],
    },
    doDont: {
      do: ['Name the popover content.'],
      dont: ['Do not use a popover for content the user must act on before continuing.'],
    },
    related: {
      items: [
        { id: 'tooltip', reason: 'Non-interactive hint text.' },
        { id: 'dropdown-menu', reason: 'Lists of actions.' },
        { id: 'dialog', reason: 'Modal decisions.' },
      ],
    },
    dependencies: {
      packages: ['@radix-ui/react-popover', 'react'],
      components: [],
    },
    testing: {
      unit: [
        'Opens on click and closes with Escape, returning focus to the trigger.',
        'No axe violations in the open panel (jsdom).',
        'Renders on the server (closed).',
      ],
      axe: true,
      ssr: true,
      browser: ['Visual baselines: /__gallery'],
    },
  },
} satisfies ComponentDocs;
