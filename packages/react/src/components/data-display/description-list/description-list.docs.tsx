import { DescriptionItem, DescriptionList } from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';

export default {
  id: 'description-list',
  title: 'DescriptionList',
  summary:
    'Key/value facts on detail pages, with copyable values, empty placeholders and clamped long values.',
  sections: {
    purpose: {
      summary:
        'Key/value facts on a detail page (owner, stage, created, …), as a semantic `<dl>`. Items can offer a copy button, show a placeholder for empty values, and clamp long values behind "Show more".',
      useWhen: [
        'Showing read-only fields of one record.',
        'Ids, keys and URLs people copy: use `copyable`.',
        'Values that may be missing: the placeholder replaces hand-rolled em dashes.',
      ],
      avoidWhen: [
        'Comparing many records: use a table.',
        'Editable fields: use a form with Field, or InlineEditText per value.',
      ],
    },
    designPrinciples: {
      items: [
        'Semantic markup: each item is a `<dt>`/`<dd>` pair.',
        'Values wrap anywhere so long ids and URLs never overflow.',
        'An empty value is visibly empty (a muted dash) and audibly named ("Not set"), never a blank cell.',
        'Truncation never hides data: the full value is one "Show more" away, not only in a tooltip.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'DescriptionList renders a `<dl>`; DescriptionItem a `<div>` with `<dt>` and `<dd>`. Both forward their ref.',
        'Server-safe: the list and items have no state. The copy button and the clamp toggle are client components rendered only when used.',
        '`--mn-dl-min-column` sets the minimum column width of `columns="auto"` (default 12rem).',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [
        {
          prop: 'columns',
          values: ['1', '2', '3', '4', 'auto'],
          description:
            'Grid columns. Default `2`. `4` drops to two columns below 1024px; `auto` fits columns of at least `--mn-dl-min-column`. Exposed as `data-columns`.',
        },
        {
          prop: 'layout',
          values: ['stacked', 'inline'],
          description:
            '`stacked` (default, vertical) puts the term above the value; `inline` (horizontal) side by side. Exposed as `data-layout`.',
        },
        {
          prop: 'DescriptionItem wide',
          values: ['true', 'false'],
          description: 'Spans all columns (`data-wide`).',
        },
        {
          prop: 'DescriptionItem copyable',
          values: ['false', 'true', 'string'],
          description:
            '`true` copies a text value; a string copies that text (the full key behind a masked one).',
        },
        {
          prop: 'DescriptionItem truncate',
          values: ['false', 'true (1 line)', 'number of lines'],
          description: 'Clamps the value and adds "Show more" when it overflows.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Empty',
          attribute: 'data-empty on the item',
          description:
            '`null`, `undefined`, `false` or `""` render the placeholder (default a muted em dash read as "Not set"). `0` is a value.',
        },
        {
          name: 'Clamped / expanded',
          attribute: 'data-expanded on .mn-dl__clamp',
          description:
            'The toggle shows only when the text overflows the clamp; `aria-expanded` follows.',
        },
        {
          name: 'Copied',
          description:
            'The copy button shows a check and announces "Copied to clipboard" (CopyButton).',
        },
      ],
    },
    interaction: {
      items: [
        'The copy button copies the value and confirms in place.',
        '"Show more" expands the clamped value; "Show less" clamps it again.',
        'The overflow check runs after mount and on resize, so a value that fits never shows a toggle.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Tab',
          action: 'Moves to each copy button and "Show more" toggle in reading order.',
        },
        { keys: 'Enter / Space', action: 'Copies the value, or expands and collapses it.' },
      ],
      notes: ['A list without copyable or truncated items has nothing to focus.'],
    },
    accessibility: {
      role: 'none (native dl/dt/dd semantics)',
      items: [
        'Terms render as `<dt>` and values as `<dd>`, so screen readers pair them.',
        'The copy button is named "Copy {term}" when the term is text; pass `copyLabel` otherwise.',
        'The expand toggle is a button with `aria-expanded` and `aria-controls` pointing at the value.',
        'The default placeholder hides the dash from assistive technology and reads `emptyLabel` instead.',
      ],
    },
    responsive: {
      items: [
        'At 640px and below every column count collapses to one column.',
        '`columns={4}` shows two columns between 641px and 1023px.',
        'In `inline` layout terms take a fixed 9rem column, and stack above the value below 420px.',
      ],
    },
    theme: {
      items: [
        'Term, value, placeholder and link-coloured toggle use semantic text tokens.',
        'The toggle focus ring uses `--mn-color-focus-ring`.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Record facts',
          render: () => (
            <DescriptionList columns={2} style={{ width: '100%' }}>
              <DescriptionItem term="Owner">Ada Lovelace</DescriptionItem>
              <DescriptionItem term="Stage">Proposal</DescriptionItem>
              <DescriptionItem term="Created">3 March</DescriptionItem>
              <DescriptionItem term="Source">Inbound</DescriptionItem>
              <DescriptionItem term="Notes" wide>
                Follow up after the product review.
              </DescriptionItem>
            </DescriptionList>
          ),
        },
        {
          title: 'Copyable values and empty placeholders',
          render: () => (
            <DescriptionList columns={2} style={{ width: '100%' }}>
              <DescriptionItem term="Workspace ID" copyable>
                ws_8f2a91c4
              </DescriptionItem>
              <DescriptionItem term="Webhook URL" copyable>
                https://hooks.example.com/w/8f2a91c4
              </DescriptionItem>
              <DescriptionItem term="Phone">{null}</DescriptionItem>
              <DescriptionItem term="Tax ID" placeholder="Not provided">
                {''}
              </DescriptionItem>
            </DescriptionList>
          ),
        },
        {
          title: 'Inline layout with a clamped value',
          render: () => (
            <DescriptionList
              layout="inline"
              columns={1}
              style={{ width: '100%', maxWidth: '32rem' }}
            >
              <DescriptionItem term="Page title">
                Pricing plans for teams of every size
              </DescriptionItem>
              <DescriptionItem term="Meta description" truncate={2}>
                Compare plans side by side, see what each includes, and start a free trial. Every
                plan includes unlimited projects, guest access and priority support during business
                hours, with annual billing available at a discount.
              </DescriptionItem>
              <DescriptionItem term="Canonical">{null}</DescriptionItem>
            </DescriptionList>
          ),
        },
        {
          title: 'Responsive columns',
          render: () => (
            <DescriptionList columns="auto" style={{ width: '100%' }}>
              <DescriptionItem term="Plan">Team</DescriptionItem>
              <DescriptionItem term="Seats">12 of 15</DescriptionItem>
              <DescriptionItem term="Renews">1 June</DescriptionItem>
              <DescriptionItem term="Billing">Annual</DescriptionItem>
            </DescriptionList>
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Use `wide` for long values such as notes or addresses.',
        'Pass missing values straight through; the placeholder handles them.',
        'Make ids, keys and URLs `copyable`.',
      ],
      dont: [
        'Do not put form controls in a description list.',
        'Do not hide truncated values behind a `title` attribute only; use `truncate`.',
      ],
    },
    related: {
      items: [
        { id: 'record-header', reason: 'Renders its key facts with a DescriptionList.' },
        { id: 'copy-button', reason: 'The copy affordance on `copyable` items.' },
        { id: 'card', reason: 'A surface to group the list.' },
      ],
    },
    dependencies: { packages: ['react'], components: ['copy-button'] },
    testing: {
      unit: [
        'Renders terms as `<dt>` and values as `<dd>`.',
        'Exposes columns and layout and forwards refs.',
        'Empty values show the placeholder read as "Not set"; `0` is a value.',
        'Copies values with the keyboard and the pointer; no copy button for empty values.',
        'Clamps long values with an `aria-expanded` toggle shown only on overflow.',
        'Renders on the server.',
      ],
      axe: true,
      ssr: true,
      browser: [
        'Visual baselines: /__gallery/description-list',
        'Cross-browser axe: /components/description-list',
      ],
    },
  },
} satisfies ComponentDocs;
