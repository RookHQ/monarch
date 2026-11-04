import { IconButton, Tooltip } from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';
import { PlusIcon } from '../../../internal/docs-demo.js';

export default {
  id: 'tooltip',
  title: 'Tooltip',
  summary: 'Supplementary text on hover and focus.',
  sections: {
    purpose: {
      summary: 'Short supplementary text about a control, shown on hover and keyboard focus.',
      useWhen: ['Naming or clarifying an icon-only control.'],
      avoidWhen: ['Essential information or interactive content: use Popover or visible text.'],
    },
    designPrinciples: {
      items: [
        'Never put essential information or interactive content in a tooltip.',
        'Mount TooltipProvider once near the root so the open delay is shared.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Tooltip wraps a single focusable child as the Radix trigger (`asChild`).',
        'TooltipProvider sets `delayDuration` (default 400 ms) for all tooltips below it.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [
        {
          prop: 'side',
          values: ['top', 'right', 'bottom', 'left'],
          description: 'Preferred side. Default `top`.',
        },
        {
          prop: 'align',
          values: ['start', 'center', 'end'],
          description: 'Alignment on that side. Default `center`.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Open / closed',
          description:
            'Controlled with `open` / `onOpenChange` or uncontrolled with `defaultOpen`.',
        },
      ],
    },
    interaction: {
      items: [
        'Opens after the provider delay on hover, and on keyboard focus.',
        'Escape dismisses it.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Focusing the trigger opens the tooltip.' },
        { keys: 'Escape', action: 'Dismisses the tooltip.' },
      ],
    },
    accessibility: {
      role: 'tooltip',
      name: 'Its text becomes the trigger’s accessible description.',
      items: ['The trigger keeps its own accessible name; the tooltip only describes it.'],
    },
    responsive: {
      items: [
        'At most 18rem wide. Hover does not exist on touch screens, so never rely on a tooltip there.',
      ],
    },
    theme: {
      items: [
        'Inverse surface (`--mn-color-bg-inverse` / `--mn-color-text-inverse`), so it contrasts with the page in light and dark themes.',
      ],
    },
    examples: {
      items: [
        {
          title: 'On an icon button',
          render: () => (
            <Tooltip content="Copy link">
              <IconButton label="Copy link" icon={<PlusIcon />} />
            </Tooltip>
          ),
          code: `<TooltipProvider>   {/* once, near the root */}
  <Tooltip content="Copy link">
    <IconButton label="Copy link" icon={<LinkIcon />} />
  </Tooltip>
</TooltipProvider>`,
        },
      ],
    },
    doDont: {
      do: ['Wrap a single focusable element.'],
      dont: [
        'Do not use a tooltip as the only accessible name of a control; IconButton has `label` for that.',
      ],
    },
    related: {
      items: [
        { id: 'popover', reason: 'Interactive floating content.' },
        { id: 'button', reason: 'IconButton, the usual trigger.' },
      ],
    },
    dependencies: {
      packages: ['@radix-ui/react-tooltip'],
      components: [],
    },
    testing: {
      unit: [
        'Appears on keyboard focus and becomes the trigger’s accessible description.',
        'No axe violations in jsdom.',
        'Renders on the server.',
      ],
      axe: true,
      ssr: true,
      browser: ['Visual baselines: /__gallery'],
    },
  },
} satisfies ComponentDocs;
