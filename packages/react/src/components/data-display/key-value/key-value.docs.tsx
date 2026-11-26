import { KeyValue, KeyValueItem } from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';

export default {
  id: 'key-value',
  title: 'KeyValue',
  summary: 'Inline label and value pairs for compact metadata, with optional copy buttons.',
  sections: {
    purpose: {
      summary:
        'Shows a few facts about a record in one compact line, such as "Region eu-west · Plan Team · ID ws_8f2a91", with a copy button on values people paste elsewhere. Needed by: Rook Dots (`src/components/crm/primitives.tsx` `KeyValue`), Rook Intellect (`src/components/intellect/ui.tsx` `KeyValues`), Rook Developers credential and API detail views.',
      useWhen: [
        'Metadata under a title, in a list row, a card footer or a panel header.',
        'Ids, keys and hosts that people copy (`copyable`, `mono`).',
      ],
      avoidWhen: [
        'A detail page with many facts in columns: use DescriptionList, which lays them out in a grid with the term above the value.',
        'A single number with a change: use Metric.',
        'Status alone: use StatusDot or Badge.',
      ],
    },
    designPrinciples: {
      items: [
        'Semantic pairs: a `<dl>` with `<dt>`/`<dd>`, so the label is announced with its value.',
        'Separators are decoration drawn in CSS, never characters a screen reader would read.',
        'Empty values are shown and spoken, never silently dropped.',
        'Copy uses CopyButton, so every copy has a named button, a "Copied" state and a polite announcement.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        '`KeyValue` forwards its ref to the `<dl>`, `KeyValueItem` to the pair’s wrapping `<div>`. Native attributes pass through.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'KeyValue vs DescriptionList: both are `<dl>`. DescriptionList is the detail-page grid (columns, stacked or side-by-side, clamping); KeyValue is one wrapping line of small pairs, with a separator between them.',
        'Migrating from the app copies: Rook Dots `<KeyValue label>{value}</KeyValue>` becomes `<KeyValueItem label>` inside a `<KeyValue>`; Rook Intellect `<KeyValues rows={[[k, v]]} />` maps each row to a KeyValueItem (or use DescriptionList when it is a two-column grid).',
      ],
    },
    variants: {
      items: [
        {
          prop: 'separator',
          values: ['dot', 'divider', 'none'],
          description:
            'Decoration between pairs in the `wrap` layout. Default `dot`. Exposed as `data-separator`.',
        },
        {
          prop: 'size',
          values: ['sm', 'md'],
          description: '`sm` (default) for metadata, `md` next to body text. `data-size`.',
        },
        {
          prop: 'layout',
          values: ['wrap', 'stack'],
          description:
            '`wrap` (default) flows pairs along the line; `stack` puts one pair per line. `data-layout`.',
        },
        {
          prop: 'KeyValueItem mono',
          values: ['true', 'false'],
          description: 'Monospace value for ids and keys (`data-mono`).',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Empty value',
          attribute: 'data-empty',
          description:
            'The value shows a muted dash read as `messages.empty` ("Not set"); no copy button.',
        },
        {
          name: 'Copied',
          attribute: 'data-state="copied" (on the copy button)',
          description: 'CopyButton shows its copied state and announces "Copied to clipboard".',
        },
      ],
    },
    interaction: {
      items: [
        'The pairs are static text; only the copy buttons are interactive.',
        'Activating a copy button copies the value (or the `copyable` string) and calls `onCopyValue`.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Moves between copy buttons.' },
        { keys: 'Enter / Space', action: 'Copies the value.' },
      ],
      notes: ['Without `copyable` there is nothing to focus.'],
    },
    accessibility: {
      role: 'list of terms and definitions (`<dl>`)',
      name: 'Each value is named by its `<dt>` label.',
      items: [
        'Separators are CSS pseudo-elements with no text, so they are never read.',
        'The copy button’s name is "Copy {label}" when the label is text, "Copy value" otherwise; override with `messages.copy`.',
        'The empty dash is `aria-hidden`; `messages.empty` is read instead.',
      ],
    },
    responsive: {
      items: [
        'Pairs wrap onto new lines; long values break anywhere instead of overflowing at 390px.',
        'On coarse pointers the copy button gets a 44px target without making the row taller.',
        'Separators use logical properties, so they sit on the correct side in right-to-left layouts.',
      ],
    },
    theme: {
      items: [
        'Labels use `--mn-color-text-muted`, values `--mn-color-text-default`, separators `--mn-color-text-subtle` and `--mn-color-border-default`.',
        'In forced colours text and separators become `CanvasText`, empty values `GrayText`.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Metadata line',
          render: () => (
            <KeyValue>
              <KeyValueItem label="Region">eu-west</KeyValueItem>
              <KeyValueItem label="Plan">Team</KeyValueItem>
              <KeyValueItem label="Seats">24</KeyValueItem>
            </KeyValue>
          ),
        },
        {
          title: 'Copyable ids',
          render: () => (
            <KeyValue separator="divider">
              <KeyValueItem label="Workspace ID" copyable mono>
                ws_8f2a91
              </KeyValueItem>
              <KeyValueItem label="Key" copyable="sk_test_51Hx0000000000" mono>
                sk_test_51Hx…
              </KeyValueItem>
            </KeyValue>
          ),
        },
        {
          title: 'Stacked, with an empty value',
          render: () => (
            <KeyValue layout="stack" size="md">
              <KeyValueItem label="Owner">Sam Lee</KeyValueItem>
              <KeyValueItem label="Due">{null}</KeyValueItem>
            </KeyValue>
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Keep it to a handful of short pairs.',
        'Use `copyable` with a string to copy the full value behind a shortened one.',
        'Use `mono` for ids, hashes and keys.',
      ],
      dont: [
        'Don’t type "·" or "|" between pairs; use `separator`.',
        'Don’t use it for a page of facts; that is DescriptionList.',
        'Don’t hide empty values; let them show the dash.',
      ],
    },
    related: {
      items: [
        { id: 'description-list', reason: 'Facts in a grid on a detail page.' },
        { id: 'copy-button', reason: 'The copy control KeyValueItem uses for `copyable`.' },
        { id: 'metric', reason: 'A single number with a label and change.' },
        { id: 'status-dot', reason: 'A status shown as a dot and a word.' },
      ],
    },
    dependencies: { packages: ['react'], components: ['copy-button'] },
    testing: {
      unit: [
        'Renders pairs as `<dl>` terms and definitions with default separator and layout.',
        'Shows a dash read as "Not set" (or `messages.empty`) for empty values, with no copy button.',
        'Copies values with the keyboard, calls `onCopyValue`, and names copy buttons from the label.',
        'Exposes separator, size and layout, and forwards refs.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
