import { BackLink } from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';

export default {
  id: 'back-link',
  title: 'BackLink',
  summary: 'A link to the parent page with a leading back arrow; works with any router.',
  sections: {
    purpose: {
      summary:
        'Takes the reader one level up from a detail page (“Back to domains”, “All invoices”) with a consistent arrow, colour, focus ring and touch target.',
      useWhen: [
        'A detail or settings page has a single, known parent list.',
        'Breadcrumbs would be one item deep.',
      ],
      avoidWhen: [
        'The hierarchy is two or more levels deep: use Breadcrumb.',
        'You need browser history (“wherever I came from”): that is a product decision; pass the resolved `href` or handle `onClick` yourself.',
      ],
    },
    designPrinciples: {
      items: [
        'Name the destination, not the direction: “Back to domains” beats “Back”.',
        'Quiet by default (`subtle`): it sits above the page title and must not compete with it.',
        'Router-agnostic: `asChild` styles the product’s own link component, the same way Link and Button do.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Forwards its ref to the `<a>` (or the child element with `asChild`). All native anchor attributes pass through.',
        'With `asChild`, the arrow is inserted before the child’s own children through Radix `Slottable`.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [
        {
          prop: 'variant',
          values: ['subtle', 'standalone'],
          description:
            '`subtle` is muted text that darkens on hover; `standalone` uses the link colour and underlines on hover (`data-variant`).',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Hover',
          description:
            'Text darkens (or underlines) and the arrow nudges 2px towards the start edge.',
        },
        { name: 'Focus', description: 'Visible focus ring on `:focus-visible` only.' },
      ],
    },
    interaction: {
      items: [
        'Native link behaviour: click, Enter, middle-click and “open in new tab” all work.',
        'With `asChild`, navigation is handled by the router link you pass.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Moves focus to the link.' },
        { keys: 'Enter', action: 'Follows the link.' },
      ],
    },
    accessibility: {
      role: 'link',
      name: 'The visible text.',
      items: [
        'The arrow is `aria-hidden`, so it never adds “left arrow” to the name.',
        'Minimum height of 24px meets WCAG 2.2 target size (2.5.8).',
        'The arrow mirrors in right-to-left documents (`dir="rtl"`).',
      ],
    },
    responsive: {
      items: [
        'Inline size; the text may wrap on narrow screens, with the arrow centred beside it.',
      ],
    },
    theme: {
      items: [
        '`subtle` uses `--mn-color-text-muted` and `--mn-color-text-default` on hover; `standalone` uses `--mn-color-text-link`. Both meet AA in every theme.',
        'The arrow nudge is turned off under reduced motion.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Variants',
          stack: true,
          render: () => (
            <>
              <BackLink href="#/components/back-link">Back to domains</BackLink>
              <BackLink href="#/components/back-link" variant="standalone">
                All invoices
              </BackLink>
            </>
          ),
          code: `<BackLink href="/domains">Back to domains</BackLink>
<BackLink href="/invoices" variant="standalone">All invoices</BackLink>`,
        },
        {
          title: 'With a router link',
          render: () => (
            <BackLink asChild>
              <a href="#/components/back-link">All campaigns</a>
            </BackLink>
          ),
          code: `import NextLink from 'next/link';

<BackLink asChild>
  <NextLink href={\`/w/\${slug}/campaigns\`}>All campaigns</NextLink>
</BackLink>`,
        },
      ],
    },
    doDont: {
      do: [
        'Place it above the page title, aligned with the content edge.',
        'Name the parent page in the text.',
      ],
      dont: [
        'Do not use it for a “Cancel” action in a form: use a Button.',
        'Do not show a BackLink and a one-item Breadcrumb together.',
      ],
    },
    related: {
      items: [
        { id: 'breadcrumb', reason: 'Deeper hierarchies.' },
        { id: 'link', reason: 'Inline and standalone text links.' },
        { id: 'page-header', reason: 'The page title the back link sits above.' },
      ],
    },
    dependencies: { packages: ['@radix-ui/react-slot', 'react'], components: [] },
    testing: {
      unit: [
        'Is a link named by its text, with an `aria-hidden` arrow and the default variant.',
        'Renders a router link with `asChild`, keeping the arrow, classes and variant.',
        'Reachable with Tab and activated with Enter.',
        'Forwards the ref.',
      ],
      axe: true,
      ssr: true,
      browser: ['Visual baselines: /__gallery', 'Cross-browser axe: /components/back-link'],
    },
  },
} satisfies ComponentDocs;
