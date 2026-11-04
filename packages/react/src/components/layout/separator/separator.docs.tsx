import { Separator } from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';

export default {
  id: 'separator',
  title: 'Separator',
  summary: 'Horizontal or vertical divider.',
  sections: {
    purpose: {
      summary: 'A thin visual divider between groups of content.',
      useWhen: ['Separating groups inside a toolbar, menu or panel.'],
      avoidWhen: ['Separating page sections that need headings: use headings and spacing.'],
    },
    designPrinciples: {
      items: [
        'Decorative by default; set `decorative={false}` only when the division is meaningful to screen-reader users.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: ['Built on `@radix-ui/react-separator`; the ref is forwarded.'],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [
        {
          prop: 'orientation',
          values: ['horizontal', 'vertical'],
          description: 'Default `horizontal`. Exposed as `data-orientation`.',
        },
        {
          prop: 'decorative',
          values: ['true', 'false'],
          description: 'Default `true` (no separator role).',
        },
      ],
    },
    states: { items: [{ name: 'Static', description: 'No interactive states.' }] },
    interaction: { items: ['None.'] },
    keyboard: { items: [], notes: ['Not interactive, so there is no keyboard behaviour.'] },
    accessibility: {
      role: 'none when decorative; separator otherwise',
      items: ['Decorative separators are hidden from assistive technology (Radix).'],
    },
    responsive: {
      items: [
        'Horizontal separators fill the width; vertical ones fill the height of their container.',
      ],
    },
    theme: { items: ['Colour is a Monarch border token.'] },
    examples: {
      items: [
        {
          title: 'Horizontal',
          stack: true,
          render: () => (
            <>
              <p>Profile</p>
              <Separator />
              <p>Security</p>
            </>
          ),
          code: `<p>Profile</p>
<Separator />
<p>Security</p>`,
        },
      ],
    },
    doDont: {
      do: ['Keep separators decorative unless the grouping matters to screen-reader users.'],
      dont: ['Do not stack separators to create spacing.'],
    },
    related: {
      items: [{ id: 'toolbar', reason: 'Groups controls; separators can divide its groups.' }],
    },
    dependencies: { packages: ['@radix-ui/react-separator', 'react'], components: [] },
    testing: {
      unit: ['Is decorative by default and a separator when `decorative={false}`.'],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
