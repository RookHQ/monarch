import { useRef } from 'react';
import { Prose, TableOfContents } from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const SECTIONS = [
  { id: 'toc-demo-overview', title: 'Overview', level: 2 },
  { id: 'toc-demo-setup', title: 'Setup', level: 2 },
  { id: 'toc-demo-accounts', title: 'Accounts', level: 3 },
  { id: 'toc-demo-permissions', title: 'Permissions', level: 3 },
  { id: 'toc-demo-usage', title: 'Daily use', level: 2 },
  { id: 'toc-demo-questions', title: 'Questions', level: 2 },
];

function Demo() {
  const scroller = useRef<HTMLDivElement>(null);
  const content = useRef<HTMLDivElement>(null);
  return (
    <div style={{ display: 'flex', gap: 24, width: '100%', flexWrap: 'wrap' }}>
      <div
        ref={scroller}
        // A scroll area so the demo works inside the docs page.
        // eslint-disable-next-line jsx-a11y/no-noninteractive-tabindex
        tabIndex={0}
        aria-label="Demo article"
        role="region"
        style={{ flex: '1 1 260px', height: 320, overflowY: 'auto' }}
      >
        <Prose ref={content} size="sm">
          {SECTIONS.map((s) =>
            s.level === 2 ? (
              <div key={s.id}>
                <h2 id={s.id}>{s.title}</h2>
                <p>
                  A paragraph about {s.title.toLowerCase()}. Scroll this area and the current
                  section is highlighted in the list beside it.
                </p>
                <p>Another paragraph so each section takes up some room.</p>
              </div>
            ) : (
              <div key={s.id}>
                <h3 id={s.id}>{s.title}</h3>
                <p>Details about {s.title.toLowerCase()}.</p>
                <p>More details, to make the section tall enough to scroll past.</p>
              </div>
            ),
          )}
        </Prose>
      </div>
      <TableOfContents
        contentRef={content}
        scrollRoot={scroller}
        updateHash={false}
        // The docs page has its own "On this page" navigation; name this one apart.
        messages={{ title: 'In this article' }}
        style={{ flex: '0 0 180px' }}
      />
    </div>
  );
}

export default {
  id: 'table-of-contents',
  title: 'TableOfContents',
  summary: 'In-page navigation from headings, highlighting the section being read.',
  sections: {
    purpose: {
      summary:
        'Lists the sections of a long page and marks the one in view (scroll-spy), so readers can see where they are and jump around. Needed by: the rook.com trademark page (`rook/src/app/[locale]/trademark/page.tsx`), which hand-writes an “On this page” list, and the Rook Developers docs pages (`docs/identity/quickstart`, `docs/geo`).',
      useWhen: [
        'A long document, guide, policy or settings page with several headed sections.',
        'Next to Prose content, usually sticky in a side column on wide screens.',
      ],
      avoidWhen: [
        'Short pages with two or three sections.',
        'Navigation between pages: use SidebarNav or TopNav.',
        'Sections that are alternatives shown one at a time: use Tabs.',
      ],
    },
    designPrinciples: {
      items: [
        'Plain links to `#id`: they work without JavaScript and can be opened or copied.',
        'Scroll-spy uses IntersectionObserver, never scroll listeners.',
        'Clicking moves focus to the heading, not only the scroll position, so keyboard users continue from there.',
        'Server-renderable from `items`; `contentRef` reads headings in the browser for content you do not control.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Renders `<nav>` (ref) named by its visible title; other attributes pass through.',
        '`collectHeadings(root, selector?)` returns `{ id, label, level }[]` from headings with ids.',
        '`useScrollSpy(ids, { offset, rootMargin, scrollRoot })` returns the current id, for building your own navigation.',
        '`TABLE_OF_CONTENTS_MESSAGES` holds the default strings.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [
        {
          prop: 'items / contentRef',
          values: ['items', 'contentRef + selector'],
          description:
            'Pass entries directly (server-rendered), or collect them from headings inside an element (updated when it changes).',
        },
        {
          prop: 'showTitle',
          values: ['true', 'false'],
          description:
            'Show “On this page” above the list. When hidden it still names the navigation.',
        },
        {
          prop: 'updateHash',
          values: ['true', 'false'],
          description: 'Write `#id` to the URL on click. Turn off with hash-based routers.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Current section',
          attribute: 'aria-current="location", data-active',
          description: 'Accent text and an accent bar on the start edge.',
        },
        { name: 'Hover', description: 'Default text colour.' },
        { name: 'Focus', description: 'Focus ring inside the link.' },
      ],
    },
    interaction: {
      items: [
        'Scrolling updates the current entry: the first heading in the top 40% of the view, or else the last heading scrolled past.',
        'Clicking an entry scrolls smoothly to its heading (instantly with reduced motion), focuses it and makes it current.',
        '`offset` accounts for a sticky header; `scrollRoot` for a scrolling container other than the window.',
        '`activeId` controls the current entry; `onActiveChange` reports changes.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab / Shift+Tab', action: 'Moves between entries.' },
        { keys: 'Enter', action: 'Scrolls to the section and moves focus to its heading.' },
      ],
    },
    accessibility: {
      role: 'navigation with nested lists of links',
      name: 'The visible title (“On this page”) via `aria-labelledby`, or `aria-label`.',
      items: [
        'The current section is announced with `aria-current="location"`.',
        'Nesting follows heading levels, so the structure matches the document outline.',
        'Focus moves to the target heading (given `tabindex="-1"` if needed).',
      ],
    },
    responsive: {
      items: [
        'Takes its container’s width; long titles wrap.',
        'Entries are 44px tall on touch screens. On phones, place it above the content or inside a Disclosure.',
      ],
    },
    theme: {
      items: [
        'Muted links on a `--mn-color-border-default` rail; the current entry uses the product accent.',
        'Forced colours: current entry in Highlight.',
      ],
    },
    examples: {
      items: [{ title: 'Scroll-spy over an article', stack: true, render: () => <Demo /> }],
    },
    doDont: {
      do: [
        'Give every heading a stable `id`.',
        'Set `offset` to the height of a sticky header.',
        'Pass `items` when the page is server-rendered.',
      ],
      dont: [
        'Do not list more than two heading levels.',
        'Do not use it for links to other pages.',
      ],
    },
    related: {
      items: [
        { id: 'prose', reason: 'The long-form content it usually indexes.' },
        { id: 'heading', reason: 'Headings with ids to link to.' },
        { id: 'skip-link', reason: 'Jumping straight to the main content.' },
        { id: 'sidebar-nav', reason: 'Navigation between pages.' },
      ],
    },
    dependencies: { packages: ['react'], components: [] },
    testing: {
      unit: [
        '`collectHeadings` reads ids, text and levels with the default or a custom selector.',
        'Titled navigation with entries nested by level; no axe violations.',
        'IntersectionObserver updates `aria-current="location"` and `onActiveChange`; `offset` sets the root margin.',
        'Falls back to the last heading scrolled past.',
        'Enter on an entry focuses the heading, updates the hash and marks it current.',
        '`contentRef` collects headings and follows changes; controlled `activeId`; `updateHash={false}`; translated title.',
        'Hidden title still names the navigation; ref forwarded.',
        'Renders on the server from `items`.',
        '`useScrollSpy` returns the heading in view.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
