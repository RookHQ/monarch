import { PageOutline } from '@rookcom/monarch-react/editor';
import type { ComponentDocs } from '../../../internal/component-docs.js';
// The scenario of the primary example, shared with iOS and Android (CROSS-PLATFORM.md section 8).
import fixture from './page-outline.fixture.json';

const HEADINGS = fixture.data.headings;

export default {
  id: 'page-outline',
  title: 'PageOutline',
  summary:
    'A document’s headings as a nested list of links, with the current section marked; live from a Tiptap editor or from a static page.',
  sections: {
    purpose: {
      summary:
        'Shows the structure of a long document and lets people jump to a section, following the editor as it changes. Needed by: Rook Dossier (src/components/document/EditorOutlinePane.tsx and collectOutline / readingStats in src/lib/tiptap/outline.ts).',
      useWhen: [
        'Beside a document editor (pass `editor`): headings update as people type and the entry under the caret is current.',
        'Beside a long rendered document (pass `headings` whose ids are the headings’ DOM ids): scroll-spy marks the section being read.',
      ],
      avoidWhen: [
        'A docs or marketing page whose sections are known when it is written: TableOfContents is the general page component.',
        'Navigation between documents: use PageTree.',
      ],
    },
    designPrinciples: {
      items: [
        'Derived, never stored: the outline is read from the document on every change, so it cannot drift from the headings.',
        'Real links in a navigation landmark, with `aria-current="location"` for the current section.',
        'Moving to a heading moves focus there too (the caret in an editor), so keyboard and screen-reader users land where sighted users look.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Renders `nav.mn-page-outline` > `__title`, nested `ul.__list[data-depth]` > `li.__item` > `a.__link[data-level]`, `__empty` and `__stats`.',
        'Helpers: `collectOutline(doc, maxLevel)` returns the headings of a ProseMirror document (empty headings skipped, unique slug ids, positions); `outlineStats(doc)` returns words and minutes.',
        'Import from `@rookcom/monarch-react/editor`.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        '`editor` wins over `headings`. In editor mode links point at slug ids but navigation moves the caret instead of changing the URL.',
        'Leave `activeId` undefined for scroll-spy; pass it to control the current entry yourself.',
        '`scrollRoot` is the scrolling element when the document scrolls inside a panel rather than the page.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'showStats',
          values: ['false', 'true'],
          description: 'Word count and reading time under the list (editor mode).',
        },
        {
          prop: 'hideTitle',
          values: ['false', 'true'],
          description: 'Hide the visible title; the landmark keeps its name.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Current',
          attribute: 'aria-current="location"',
          description: 'Accent bar and colour on the section being read or edited.',
        },
        { name: 'Empty', description: '`messages.empty` when the document has no headings.' },
        {
          name: 'Depth',
          attribute: 'data-depth, data-level',
          description: 'Nested lists indent by heading level.',
        },
      ],
    },
    interaction: {
      items: [
        'Choosing an entry scrolls to the heading (smoothly unless reduced motion is set) and focuses it; static pages update the URL hash.',
        'In editor mode the caret moves to the heading and the list updates as headings are added, renamed or removed.',
        'Scroll-spy marks the last heading that has passed the top 30% of the scroll area.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Moves through the entries.' },
        { keys: 'Enter', action: 'Goes to the heading and focuses it.' },
      ],
    },
    accessibility: {
      role: 'navigation with a nested list of links',
      name: 'The visible title (`messages.title`, default "On this page") via `aria-labelledby`, or `aria-label` when the title is hidden.',
      items: [
        'The current section is announced with `aria-current="location"`, not colour alone (an accent bar is drawn too).',
        'Headings of static pages get `tabindex="-1"` when targeted, so focus can move to them.',
      ],
    },
    responsive: {
      items: [
        'Long headings wrap; entries grow to 44px on coarse pointers.',
        'Put it in a side panel or a Sheet on narrow screens; the component itself has no fixed width.',
      ],
    },
    theme: {
      items: [
        '`--mn-color-accent-solid` / `-strong` for the current entry, `--mn-color-border-subtle` for the rail.',
        'Forced colours use Highlight for the current bar; transitions are removed under reduced motion.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Static headings',
          render: () => (
            <PageOutline
              headings={HEADINGS}
              activeId={fixture.data.activeId}
              messages={{ title: fixture.data.title }}
            />
          ),
        },
        {
          title: 'Empty document',
          render: () => <PageOutline headings={[]} messages={{ title: 'Outline' }} />,
        },
      ],
    },
    doDont: {
      do: [
        'Give static headings stable ids.',
        'Pass `scrollRoot` when the document scrolls in a panel.',
      ],
      dont: ['Do not list every heading level; two or three levels are enough.'],
    },
    related: {
      items: [
        { id: 'table-of-contents', reason: 'General page contents for docs and marketing pages.' },
        { id: 'page-tree', reason: 'Navigation between documents.' },
        { id: 'block-editor', reason: 'The editor it usually sits beside.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: [],
      notes: ['Tiptap is imported for types only; the editor is passed in.'],
    },
    testing: {
      unit: [
        'Named navigation landmark, nested list, `maxLevel`.',
        'Choosing a link scrolls to and focuses the heading and marks it current; controlled `activeId`.',
        'Scroll-spy through IntersectionObserver and `onActiveChange`.',
        'Empty message; editor mode updates live, shows stats and moves the caret; `collectOutline` skips empty headings and de-duplicates ids.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
