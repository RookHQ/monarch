import { Link } from '@rookcom/monarch-react';
import { incomplete, type ComponentDocs } from '../../../internal/component-docs.js';

export default {
  id: 'link',
  title: 'Link',
  summary: 'Text link with inline, standalone and subtle styles and safe new-tab handling.',
  sections: {
    purpose: {
      summary: 'Navigates to another page or resource from text, lists and footers.',
      useWhen: [
        'Navigation inside running text (`inline`).',
        'Links in lists, cards and footers (`standalone`, `subtle`).',
      ],
      avoidWhen: ['The control performs an action on the page: use Button.'],
    },
    designPrinciples: {
      items: [
        '`inline` (default) for links in text, `standalone` for links in lists and footers.',
        'Links that open a new tab say so, visually and to screen readers.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Forwards its ref to the `<a>` (or the child element with `asChild`).',
        'All native anchor attributes pass through.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [
        {
          prop: 'variant',
          values: ['inline', 'standalone', 'subtle'],
          description: 'Default `inline`. Exposed as `data-variant`.',
        },
      ],
    },
    states: {
      items: [
        { name: 'Hover', description: 'Each variant has its own hover style in link.css.' },
        { name: 'Focus', description: 'Monarch focus ring on `:focus-visible`.' },
        {
          name: 'External',
          attribute: 'target="_blank"',
          description:
            'Adds `rel="noopener noreferrer"` (merged with any `rel` passed), an external icon and a visually hidden "(opens in a new tab)".',
        },
      ],
    },
    interaction: {
      items: [
        'Native link behaviour.',
        'With `asChild`, the child element (for example a router link) receives the classes and attributes.',
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
      name: 'The link text; external links append "(opens in a new tab)".',
      items: ['The external icon is `aria-hidden` and not focusable.'],
    },
    responsive: incomplete('No width-specific behaviour is defined or tested.'),
    theme: {
      items: [
        'Colours are Monarch semantic tokens only, so light, dark and more-contrast themes apply automatically.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Variants and external',
          render: () => (
            <>
              <Link href="#/components/link">Inline link</Link>
              <Link href="#/components/link" variant="standalone">
                Standalone link
              </Link>
              <Link href="#/components/link" variant="subtle">
                Subtle link
              </Link>
              <Link href="https://rookhq.com" target="_blank">
                Rook website
              </Link>
            </>
          ),
          code: `<Link href="/settings">Inline link</Link>
<Link href="/settings" variant="standalone">Standalone link</Link>
<Link href="/settings" variant="subtle">Subtle link</Link>
<Link href="https://rookhq.com" target="_blank">Rook website</Link>`,
        },
      ],
    },
    doDont: {
      do: [
        'Use `asChild` to style a router link.',
        'Write link text that makes sense out of context.',
      ],
      dont: ['Do not use a Link for actions such as "Delete"; use Button.'],
    },
    related: {
      items: [
        { id: 'button', reason: 'Actions, or a link that must look like a button (`asChild`).' },
        { id: 'back-link', reason: 'Navigation one level up.' },
      ],
    },
    dependencies: {
      packages: ['@radix-ui/react-slot', 'react'],
      components: ['visually-hidden'],
    },
    testing: {
      unit: [
        'External links get rel="noopener noreferrer" and a hidden new-tab note in their name.',
        'No axe violations in jsdom.',
        'Renders on the server.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
