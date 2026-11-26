import { TruncatedText } from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';
// The scenario of the primary example, shared with iOS and Android (CROSS-PLATFORM.md section 8).
import fixture from './truncated-text.fixture.json';

const NOTES = fixture.data.text;

export default {
  id: 'truncated-text',
  title: 'TruncatedText',
  summary: 'Text clamped to a few lines with "Show more", the full text always accessible.',
  sections: {
    purpose: {
      summary:
        'Shows the first few lines of a long description, note or message and lets people expand the rest in place. Needed by: Rook Dots, Rook Base, Rook Chat, Rook Dossier, Rook Calendar, Rook Atlas and Rook App, which clamp text with Tailwind `line-clamp-*` classes.',
      useWhen: [
        'Descriptions, notes and comments in cards and lists that are usually short but sometimes long.',
        'A one-line title in a narrow column, with the full title in a tooltip (`lines={1} tooltip`).',
      ],
      avoidWhen: [
        'A whole section that people open and close: use Collapsible.',
        'A value in a DescriptionList: use DescriptionItem `truncate`, which has the same behaviour built in.',
        'Text that must always be read in full, such as warnings and legal terms: do not clamp it.',
      ],
    },
    designPrinciples: {
      items: [
        'The clamp is visual only: the full text stays in the page for screen readers, find-in-page and copy.',
        'No button unless it is needed: "Show more" appears only when the text actually overflows, measured on mount and on resize.',
        'Expanding happens in place; focus stays on the button.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: ['Forwards its ref to the root `<div>`; native `<div>` attributes pass through.'],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Migrating from `line-clamp-N` classes: wrap the text in `<TruncatedText lines={N}>` and remove the class.',
        '`expanded` with `onExpandedChange` makes it controlled, for example to expand every item in a list at once.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'lines',
          values: ['1', '2', '3 (default)', 'any whole number'],
          description:
            'Lines shown before clamping. `1` cuts a single line with an ellipsis (`data-single`).',
        },
        {
          prop: 'expandable',
          values: ['true', 'false'],
          description: 'Show the "Show more" button when clamped. Default `true`.',
        },
        {
          prop: 'tooltip',
          values: ['true', 'false'],
          description: 'Show the full text in a Tooltip on hover and focus while it is clamped.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Clamped',
          attribute: 'data-clamped',
          description: 'The text overflows the line limit and is cut.',
        },
        {
          name: 'Expanded',
          attribute: 'data-expanded (on the text), aria-expanded="true" (on the button)',
          description: 'All lines are shown; the button reads `messages.showLess`.',
        },
        {
          name: 'Fits',
          description: 'The text is within the limit; no button and no tooltip.',
        },
      ],
    },
    interaction: {
      items: [
        'The button toggles between the clamped and full text and calls `onExpandedChange`.',
        'Overflow is re-measured when the element resizes or the text changes.',
        'With `tooltip`, hovering or focusing the clamped text shows the whole text; Escape hides it.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Tab',
          action:
            'Moves to "Show more" (or to the clamped text when it has a tooltip and no button).',
        },
        { keys: 'Enter / Space', action: 'Expands or collapses the text.' },
        { keys: 'Escape', action: 'Hides the tooltip.' },
      ],
    },
    accessibility: {
      role: 'None for the text; button for the toggle',
      name: 'The toggle is named by `messages.showMore` / `messages.showLess`.',
      items: [
        'The full text is always in the accessibility tree; the clamp is CSS only.',
        'The toggle has `aria-expanded` and `aria-controls` pointing at the text.',
        'With `tooltip` and no button, the clamped text gets `tabindex="0"` so keyboard users can open the tooltip.',
      ],
    },
    responsive: {
      items: [
        'Re-measures on resize, so the button appears or disappears as the width changes.',
        'Long words break anywhere instead of overflowing at 390px.',
        'On coarse pointers the toggle is at least 44px tall.',
      ],
    },
    theme: {
      items: [
        'The toggle uses `--mn-color-text-link`; the text inherits its colour.',
        'In forced colours the toggle uses `LinkText`.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Three lines with Show more',
          stack: true,
          render: () => <TruncatedText lines={fixture.data.lines}>{NOTES}</TruncatedText>,
        },
        {
          title: 'One line with a tooltip',
          stack: true,
          render: () => (
            <div style={{ maxWidth: '16rem' }}>
              <TruncatedText lines={1} tooltip expandable={false}>
                Migration of the reporting service to the new storage layer
              </TruncatedText>
            </div>
          ),
        },
        {
          title: 'Short text: no button',
          render: () => <TruncatedText lines={2}>Ready for review.</TruncatedText>,
        },
      ],
    },
    doDont: {
      do: [
        'Pick the line count from the layout: 1 for titles, 2–3 for descriptions in cards.',
        'Keep `expandable` on unless the full text is one click away.',
        'Translate both labels through `messages`.',
      ],
      dont: [
        'Don’t clamp warnings, errors or anything people must read in full.',
        'Don’t use `tooltip` for paragraphs; tooltips are for a line or two.',
        'Don’t add your own `line-clamp` class on top.',
      ],
    },
    related: {
      items: [
        { id: 'collapsible', reason: 'Show and hide a whole section.' },
        {
          id: 'description-list',
          reason: '`truncate` on DescriptionItem clamps a value the same way.',
        },
        { id: 'tooltip', reason: 'The tooltip used for the full text.' },
      ],
    },
    dependencies: { packages: ['react'], components: ['tooltip'] },
    testing: {
      unit: [
        'Keeps the full text in the page and adds "Show more" only when it overflows.',
        'Expands and collapses with Enter and Space, keeping focus on the button and calling `onExpandedChange`.',
        'Shows no button for text that fits.',
        'Translates its labels and can start expanded.',
        'Shows the full text in a keyboard-reachable tooltip when not expandable; Escape hides it.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
