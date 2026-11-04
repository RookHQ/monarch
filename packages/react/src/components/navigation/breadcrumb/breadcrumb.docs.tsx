import { Breadcrumb } from '@rookcom/monarch-react';
import { incomplete, type ComponentDocs } from '../../../internal/component-docs.js';

export default {
  id: 'breadcrumb',
  title: 'Breadcrumb',
  summary: 'Location trail with the current page last and long trails collapsed into a menu.',
  sections: {
    purpose: {
      summary: 'Shows where the current page sits in a hierarchy and links back to each ancestor.',
      useWhen: ['Pages are nested more than one level deep (workspace → project → task).'],
      avoidWhen: [
        'The hierarchy is flat: a back link is enough.',
        'Switching between sibling sections of one page: use Tabs.',
      ],
    },
    designPrinciples: {
      items: [
        'The last item is always the current page and is not a link.',
        'Long trails keep the first item and the last `maxVisible - 2` items; the middle collapses into a "…" menu.',
        'Routing stays in the app: pass the router’s link component as `linkAs`.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: ['Breadcrumb is a function component; it does not forward a ref.'],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [],
      notes: [
        'Breadcrumb has no visual variants; the only layout option is `maxVisible`, documented under properties.',
      ],
    },
    states: {
      items: [
        {
          name: 'Current page',
          attribute: 'aria-current="page"',
          description: 'The last item renders as plain text (`.mn-breadcrumb__current`).',
        },
        {
          name: 'Collapsed',
          description:
            'When `items.length > maxVisible` (default 4), hidden levels move into a DropdownMenu behind a "…" trigger.',
        },
        {
          name: 'Item without href',
          description: 'A non-final item with no `href` renders as text, not a link.',
        },
      ],
    },
    interaction: {
      items: [
        'Ancestor items are links rendered with `linkAs` (default `a`).',
        'The "…" trigger opens a menu whose items are the hidden levels, each rendered as a link.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Moves through the links and the "…" trigger.' },
        {
          keys: 'Enter / Space / ArrowDown',
          action: 'Opens the collapsed-levels menu (DropdownMenu).',
        },
        { keys: 'Escape', action: 'Closes the menu and returns focus to the trigger.' },
      ],
    },
    accessibility: {
      role: 'navigation',
      name: '`label` prop, default "Breadcrumb".',
      items: [
        'Items are an ordered list; the chevron separators are `aria-hidden` list items.',
        'The current page has `aria-current="page"`.',
        'The collapse trigger is named "Show N more levels".',
      ],
    },
    responsive: incomplete(
      'The stylesheet has no width-specific rules; behaviour on narrow screens (wrapping of long labels) has not been specified or tested.',
    ),
    theme: {
      items: [
        'Colours are Monarch semantic tokens only, so light, dark and more-contrast themes apply automatically.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Collapsed trail',
          render: () => (
            <Breadcrumb
              items={[
                { label: 'Workspace', href: '#/components/breadcrumb' },
                { label: 'Projects', href: '#/components/breadcrumb' },
                { label: 'Website', href: '#/components/breadcrumb' },
                { label: 'Sprint 4', href: '#/components/breadcrumb' },
                { label: 'Fix sign-in redirect' },
              ]}
            />
          ),
          code: `<Breadcrumb
  linkAs={Link}
  items={[
    { label: 'Workspace', href: '/' },
    { label: 'Projects', href: '/projects' },
    { label: 'Website', href: '/projects/website' },
    { label: 'Sprint 4', href: '/projects/website/sprints/4' },
    { label: 'Fix sign-in redirect' },
  ]}
/>`,
        },
      ],
    },
    doDont: {
      do: ['Pass the router link component as `linkAs` so navigation stays client-side.'],
      dont: [
        'Do not give the last item an `href` expecting it to be a link; it is always the current page.',
        'Do not use a breadcrumb as the only way to navigate.',
      ],
    },
    related: {
      items: [
        { id: 'back-link', reason: 'Single step back in a shallow hierarchy.' },
        { id: 'page-header', reason: 'Takes a breadcrumb in its `eyebrow` slot.' },
        { id: 'dropdown-menu', reason: 'Used for the collapsed levels.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['dropdown-menu'],
    },
    testing: {
      unit: [
        'Is a navigation named "Breadcrumb"; the last item has aria-current="page".',
        'Collapses the middle levels and opens them in a menu.',
        'No axe violations in jsdom.',
        'Renders on the server.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
