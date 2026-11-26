import { SkipLink, Text } from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';

export default {
  id: 'skip-link',
  title: 'SkipLink',
  summary: 'A “Skip to main content” link that appears on keyboard focus.',
  sections: {
    purpose: {
      summary:
        'Lets keyboard and switch users jump past repeated navigation straight to the page content (WCAG 2.4.1 Bypass Blocks). Needed by: Rook Tickets (portal chrome) and the Rook Design site, which hand-write one today; every app shell should have one.',
      useWhen: [
        'At the very top of every page or app shell that has navigation before the main content.',
        'Optionally a second link to another landmark, such as search, on pages with long navigation.',
      ],
      avoidWhen: [
        'Pages with no repeated content before the main area (a sign-in form alone).',
        'As a general in-page link: use Link or TableOfContents.',
      ],
    },
    designPrinciples: {
      items: [
        'Invisible until focused, then clearly visible: a solid accent chip in the top start corner above everything else.',
        'Moves focus, not just scroll position, so the next Tab continues inside the content.',
        'Does not change the URL, so it is safe with hash routers.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Renders `<a>` and forwards its ref; anchor attributes pass through. Your `onClick` runs first and can call `preventDefault()` to take over.',
        'When `href` points to an id that does not exist, the link behaves as a normal anchor.',
        '`SKIP_LINK_MESSAGES` holds the default text.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: { items: [], notes: ['SkipLink has no variants.'] },
    states: {
      items: [
        {
          name: 'Hidden',
          description: 'Clipped to 1px while not focused; still in the Tab order.',
        },
        {
          name: 'Focused',
          description: 'Shown at the top start corner with the focus ring (`:focus-visible`).',
        },
      ],
    },
    interaction: {
      items: [
        'Activating it focuses the element with the target id and scrolls it into view.',
        'A target that is not focusable gets `tabindex="-1"` first; an existing `tabindex` is kept.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'From the top of the page, focuses and reveals the link.' },
        { keys: 'Enter', action: 'Moves focus to the main content.' },
      ],
    },
    accessibility: {
      role: 'link',
      name: 'Its text: “Skip to main content” by default (`messages.label` or children).',
      items: [
        'Must be the first focusable element in the document; render it before the rail or header.',
        'Point it at the `<main>` landmark (`id="main-content"` by default).',
        'Visible on focus, so sighted keyboard users can see what the first Tab did (WCAG 2.4.7).',
      ],
    },
    responsive: {
      items: [
        'Position is fixed to the viewport, so it shows above a sticky header at any width; at least 44px tall on touch screens.',
      ],
    },
    theme: {
      items: [
        'Uses the product accent (`--mn-color-accent-solid` / `-on-solid`) and `--mn-shadow-md`.',
        'Forced colours: Canvas background, LinkText and a CanvasText border.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Focus to reveal',
          stack: true,
          render: () => (
            <>
              <Text size="sm" tone="muted">
                Click here, then press Tab: the link appears in the top corner of the window.
              </Text>
              <SkipLink href="#skip-link-demo-target" />
              <div id="skip-link-demo-target">
                <Text>Main content starts here.</Text>
              </div>
            </>
          ),
        },
        {
          title: 'Custom target and label',
          stack: true,
          render: () => (
            <>
              <SkipLink href="#skip-link-demo-search">Skip to search</SkipLink>
              <input id="skip-link-demo-search" aria-label="Search" placeholder="Search" />
            </>
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Render it first in the app shell layout, on every page.',
        'Give `<main>` the matching id.',
      ],
      dont: [
        'Do not hide it with `display: none`: it must stay focusable.',
        'Do not add many skip links; one, or two at most.',
      ],
    },
    related: {
      items: [
        { id: 'link', reason: 'Ordinary text links.' },
        { id: 'table-of-contents', reason: 'Links to sections within the content.' },
        { id: 'sidebar-nav', reason: 'The navigation the skip link lets people bypass.' },
      ],
    },
    dependencies: { packages: ['react'], components: [] },
    testing: {
      unit: [
        'It is the first tab stop with the default name; no axe violations.',
        'Enter moves focus to the target and makes it focusable.',
        'Keeps an existing tabindex, calls `onClick`, supports custom targets and labels.',
        'Translated label; ref forwarded.',
        'A missing target leaves the link as a plain anchor.',
        'Renders on the server.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
