import { Button, VisuallyHidden } from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';

export default {
  id: 'visually-hidden',
  title: 'VisuallyHidden',
  summary: 'Screen-reader-only text.',
  sections: {
    purpose: {
      summary: 'Hides content visually while keeping it available to assistive technology.',
      useWhen: [
        'Adding context a sighted user gets from layout, e.g. "changes to the ticket" after "Save".',
      ],
      avoidWhen: [
        'Hiding content from everyone: use `hidden`.',
        'Naming an icon-only button: use IconButton `label`.',
      ],
    },
    designPrinciples: {
      items: [
        'Its class `.mn-visually-hidden` is a foundation utility (styles/foundation.css) that other components also use.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: ['Renders a `<span>`; native attributes pass through.'],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [],
      notes: ['VisuallyHidden has no variants.'],
    },
    states: { items: [], notes: ['No states: it renders static content.'] },
    interaction: { items: [], notes: ['Not interactive.'] },
    keyboard: { items: [], notes: ['Not interactive, so there is no keyboard behaviour.'] },
    accessibility: {
      items: [
        'Text stays in the accessibility tree and contributes to the accessible name of its parent.',
      ],
    },
    responsive: { items: ['Always 1px and clipped at every width.'] },
    theme: { items: ['Not visible, so themes do not apply.'] },
    examples: {
      items: [
        {
          title: 'Screen-reader-only text',
          render: () => (
            <Button>
              Save <VisuallyHidden>changes to the ticket</VisuallyHidden>
            </Button>
          ),
        },
      ],
    },
    doDont: {
      do: ['Use it to complete a visible label that relies on context.'],
      dont: ['Do not hide focusable elements with it.'],
    },
    related: { items: [{ id: 'button', reason: 'IconButton `label` names icon-only buttons.' }] },
    dependencies: { packages: [], components: [] },
    testing: {
      unit: ['Keeps text in the accessible name of its parent button.'],
      axe: true,
      ssr: true,
      browser: ['Visual baselines: /__gallery'],
    },
  },
} satisfies ComponentDocs;
