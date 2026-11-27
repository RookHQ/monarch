import { ScrollArea } from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';
// The scenario of the primary example, shared with iOS and Android (CROSS-PLATFORM.md section 8).
import fixture from './scroll-area.fixture.json';

const { events } = fixture.data;
const EVENTS = Array.from(
  { length: events.count },
  (_, i) => `${events.prefix}${i + 1}${events.suffix}`,
);
const COLUMNS = ['Name', 'Owner', 'Status', 'Priority', 'Due', 'Created', 'Updated', 'Labels'];

const frame = {
  width: '100%',
  maxWidth: fixture.data.maxWidth,
  border: '1px solid var(--mn-color-border-default)',
  borderRadius: 'var(--mn-radius-lg)',
} as const;

export default {
  id: 'scroll-area',
  title: 'ScrollArea',
  summary:
    'A scroll container with thin themed scrollbars that is a labelled, focusable region only when it actually scrolls.',
  sections: {
    purpose: {
      summary:
        'Panels, lists and logs that scroll inside the page need a keyboard way in, a name, and scrollbars that match the theme. The shadcn/ui copies in Rook Links (`src/components/ui/scroll-area.tsx`) and Rook ID (`rookid/components/ui/scroll-area.tsx`, `rookid/src/components/ui/scroll-area.tsx`) wrap Radix with custom scrollbars that are not focusable. ScrollArea uses native scrolling with styled scrollbars and fixes focus. Needed by: Rook Links and Rook ID, and any panel that scrolls.',
      useWhen: [
        'A bounded list, log, message history or side panel that scrolls inside the page.',
        'A wide table or row of cards that scrolls horizontally on phones.',
      ],
      avoidWhen: [
        'The whole page: let the document scroll.',
        'Very long lists: use VirtualList, which renders only what is visible.',
        'Content inside Dialog or Sheet bodies, which already scroll.',
      ],
    },
    designPrinciples: {
      items: [
        'Native scrolling: touch momentum, scroll chaining rules and assistive technology all keep working.',
        'A tab stop only when it helps: focusable and named while content overflows (axe `scrollable-region-focusable`), plain otherwise.',
        'Hints where content is hidden: the edges fade only on the sides with more content.',
        'Thin scrollbars from tokens, system scrollbars in forced-colours mode.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Renders `<div class="mn-scroll-area">` (rest props) around `<div class="mn-scroll-area__viewport">` (the ref, and `viewportProps`) around `<div class="mn-scroll-area__content">`.',
        'Overflow is measured with a ResizeObserver on the viewport and content, and on scroll.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Migrating from shadcn/ui: `<ScrollArea className="h-72">` becomes `<ScrollArea label="…" height={288}>` (or keep the class); `type`, `scrollHideDelay` and `dir` are not needed with native scrollbars; `<ScrollBar orientation="horizontal" />` becomes `orientation="horizontal"` (or `both`); a ref to the Radix viewport becomes the `ref` itself.',
        '`label` is required: it names the region whenever it can be scrolled.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'orientation',
          values: ['vertical', 'horizontal', 'both'],
          description: 'Which axis scrolls.',
        },
        {
          prop: 'height / maxHeight',
          values: ['CSS length', 'number of px'],
          description: 'A fixed height, or a cap after which it scrolls.',
        },
        {
          prop: 'shadows',
          values: ['true', 'false'],
          description: 'Fade the edges where content is hidden.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Scrollable',
          attribute: 'data-scrollable',
          description: 'Content overflows; the viewport is a focusable, labelled region.',
        },
        {
          name: 'Hidden content before',
          attribute: 'data-overflow-start',
          description: 'Scrolled away from the start; the start edge fades.',
        },
        {
          name: 'Hidden content after',
          attribute: 'data-overflow-end',
          description: 'More content after; the end edge fades.',
        },
        {
          name: 'Focus',
          description: 'Monarch focus ring inside the viewport on `:focus-visible`.',
        },
      ],
    },
    interaction: {
      items: [
        'Wheel, trackpad, touch and scrollbar drag are native.',
        '`overscroll-behavior: contain`: reaching the end does not scroll the page behind.',
        '`onScrollableChange` reports when overflow starts or stops.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Focuses the viewport, only while it can scroll.' },
        { keys: 'Arrow keys', action: 'Scroll a little (native).' },
        { keys: 'Page Up / Page Down / Space', action: 'Scroll by a page (native).' },
        { keys: 'Home / End', action: 'Scroll to the start or end (native).' },
      ],
    },
    accessibility: {
      role: 'region, while scrollable',
      name: 'The required `label` prop.',
      items: [
        'Meets axe `scrollable-region-focusable` without adding an empty tab stop when nothing scrolls.',
        'The edge fades are decorative pseudo-elements.',
        'Checked with axe in jsdom.',
      ],
    },
    responsive: {
      items: [
        'Takes the width of its container; horizontal mode lets wide content scroll instead of the page at 390px.',
        'Scrollbars are thin on desktop; touch devices use their own overlay scrollbars.',
      ],
    },
    theme: {
      items: [
        'Scrollbar thumb `--mn-color-border-strong` (hover `--mn-color-text-subtle`); fades use `--mn-scroll-area-shade`, `--mn-color-bg-surface` by default (set it when the area sits on another background).',
        'Fade transitions are removed under reduced motion; forced-colours mode uses system scrollbars and drops the fades.',
      ],
    },
    examples: {
      items: [
        {
          title: 'A bounded list',
          stack: true,
          render: () => (
            <ScrollArea label={fixture.data.label} maxHeight={fixture.data.maxHeight} style={frame}>
              <ul style={{ margin: 0, padding: 12, display: 'grid', gap: 8, listStyle: 'none' }}>
                {EVENTS.map((e) => (
                  <li key={e}>{e}</li>
                ))}
              </ul>
            </ScrollArea>
          ),
        },
        {
          title: 'Horizontal',
          stack: true,
          render: () => (
            <ScrollArea label="Columns" orientation="horizontal" style={frame}>
              <div style={{ display: 'flex', gap: 8, padding: 12 }}>
                {COLUMNS.map((c) => (
                  <span
                    key={c}
                    style={{
                      padding: '8px 16px',
                      borderRadius: 'var(--mn-radius-md)',
                      background: 'var(--mn-color-bg-muted)',
                      whiteSpace: 'nowrap',
                    }}
                  >
                    {c}
                  </span>
                ))}
              </div>
            </ScrollArea>
          ),
        },
        {
          title: 'Content that fits is not a tab stop',
          stack: true,
          render: () => (
            <ScrollArea label="Notes" maxHeight={220} style={frame}>
              <p style={{ margin: 0, padding: 12 }}>Short content: nothing to scroll.</p>
            </ScrollArea>
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Give every area a short, specific label.',
        'Set `--mn-scroll-area-shade` when the background is not the surface colour.',
      ],
      dont: ['Do not nest scroll areas on the same axis.', 'Do not hide scrollbars entirely.'],
    },
    related: {
      items: [
        { id: 'sticky', reason: 'Headers that stay pinned inside a scroll area.' },
        { id: 'resizable-panels', reason: 'Split layouts whose panels each scroll.' },
      ],
    },
    dependencies: { packages: ['react'], components: [] },
    testing: {
      unit: [
        'Not a tab stop or region while content fits; axe.',
        'Focusable, labelled region while overflowing, and back; onScrollableChange; axe.',
        'Edge attributes follow scroll position; viewport onScroll still called.',
        'Horizontal (RTL magnitude), shadows off, other axis ignored.',
        'Height variables; ref to the viewport; no ResizeObserver; server rendering without a tab stop.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
