import { useEffect, useState } from 'react';
import { Button } from '@rookcom/monarch-react';
import { AiTableAnswer } from '@rookcom/monarch-react/ai';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const regions = [
  { Region: 'North', Deals: 42, Revenue: '1,240,000', 'Last update': '2026-03-04' },
  { Region: 'South', Deals: 31, Revenue: '980,500', 'Last update': '2026-01-15' },
  { Region: 'East', Deals: 57, Revenue: '1,710,250', 'Last update': '2026-02-20' },
  { Region: 'West', Deals: 18, Revenue: '455,000', 'Last update': '2026-03-11' },
];

function StreamingDemo() {
  const [count, setCount] = useState(0);
  const streaming = count < regions.length;
  useEffect(() => {
    if (!streaming) return undefined;
    const t = setTimeout(() => setCount((c) => c + 1), 500);
    return () => clearTimeout(t);
  }, [count, streaming]);
  return (
    <div style={{ display: 'grid', gap: 12 }}>
      <AiTableAnswer
        title="Pipeline by region"
        rows={regions.slice(0, count)}
        streaming={streaming}
      />
      <div>
        <Button size="sm" onClick={() => setCount(0)} disabled={streaming}>
          Replay
        </Button>
      </div>
    </div>
  );
}

export default {
  id: 'ai-table-answer',
  title: 'AiTableAnswer',
  summary: 'A model-produced table with inferred column types, sortable headers and Copy as CSV.',
  sections: {
    purpose: {
      summary:
        'Shows tabular model output as a real, accessible table: numbers align and sort as numbers, ISO dates sort by time, headers sort from the keyboard, and the data can be copied as CSV or opened as an artifact. Rows can stream in.',
      useWhen: [
        'The model answers with rows of the same shape: a comparison, a breakdown, extracted line items.',
        'People will sort, copy or reuse the data elsewhere.',
      ],
      avoidWhen: [
        'One record with labelled fields: use AiStructuredAnswer.',
        'Product data with filtering, selection or paging: use DataTable.',
        'A small Markdown table inside prose: AiMarkdown already renders it.',
      ],
    },
    designPrinciples: {
      items: [
        'Values are untrusted data, rendered as text; nothing is parsed as HTML.',
        'Types are inferred, never guessed per cell: a column is numeric only if every value is.',
        'Copy as CSV copies what people see, in the current sort order, with formula-safe escaping.',
        'Wide tables scroll inside the component, never the page.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'The ref goes to the root `<div>`; native attributes pass through.',
        '`toCsv(rows, columns?)`, `inferColumns(rows)` and `inferColumnType(values)` are exported for reuse, e.g. a download button.',
        'Strings are shown as written. Real `number` values are formatted with `Intl.NumberFormat(locale)`; `Date` values with a medium UTC date.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [
        {
          prop: 'columns[].type',
          values: ['text', 'number', 'date'],
          description:
            'Inferred when omitted. Numbers align to the end and sort numerically; dates sort by time.',
        },
      ],
      notes: [
        '`sortable`, `copyable` and `onOpenAsArtifact` turn the header controls and actions on or off.',
      ],
    },
    states: {
      items: [
        {
          name: 'Streaming',
          attribute: 'data-streaming',
          description:
            'The table is `aria-busy`, a shimmer row shows more rows are coming, and Open as artifact waits.',
        },
        {
          name: 'Sorted',
          attribute: 'aria-sort',
          description: 'On the sorted header: `ascending` or `descending`; others `none`.',
        },
        {
          name: 'Empty',
          description: '“No rows.” when there are no rows and nothing is streaming.',
        },
      ],
    },
    interaction: {
      items: [
        'Clicking a header sorts ascending, then toggles direction. Blank values always sort last.',
        'Copy as CSV copies the current order with a header row.',
        'Open as artifact calls `onOpenAsArtifact` once streaming has finished.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Tab',
          action: 'Moves through the actions, the scroll region, then each sortable header.',
        },
        { keys: 'Enter / Space', action: 'Sorts by the focused header, or toggles its direction.' },
        {
          keys: 'Arrow keys',
          action: 'Scroll the focused table region when it is wider than the screen.',
        },
      ],
    },
    accessibility: {
      role: 'table (inside a labelled, focusable region)',
      name: '“AI table” plus the title, as the caption and the region name',
      items: [
        'AI provenance is the visible “AI table” label, also part of the caption.',
        'Headers are `<th scope="col">` with sort buttons; `aria-sort` gives the state.',
        'Sorting and the end of streaming are announced politely; rows are not announced as they arrive.',
        'Empty cells read “Empty” instead of a dash.',
      ],
    },
    responsive: {
      items: [
        'At 390px the table scrolls sideways inside its region; the header and actions wrap.',
        'Sort buttons and actions are 44px tall on coarse pointers.',
        'Logical properties throughout; numbers align to the end edge in RTL too.',
      ],
    },
    theme: {
      items: [
        'Table: `--mn-color-bg-surface`, `--mn-color-border-default`; header `--mn-color-bg-subtle`.',
        'Label: `--mn-color-ai-subtle`, `--mn-color-ai-border`, `--mn-color-ai-strong`.',
        'Streaming row shimmers with `--mn-color-ai-subtle`; static under reduced motion.',
        'Forced colours: CanvasText borders and a Highlight focus outline.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Sortable table',
          stack: true,
          render: () => (
            <AiTableAnswer title="Pipeline by region" rows={regions} onOpenAsArtifact={() => {}} />
          ),
        },
        { title: 'Rows streaming in', stack: true, render: () => <StreamingDemo /> },
        {
          title: 'Explicit columns',
          stack: true,
          render: () => (
            <AiTableAnswer
              rows={regions}
              columns={[
                { key: 'Region' },
                { key: 'Deals', label: 'Open deals' },
                { key: 'Revenue', label: 'Revenue (USD)', type: 'number' },
              ]}
            />
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Give the table a title that says what the rows are.',
        'Pass `columns` when you know the schema, to fix the order and labels.',
      ],
      dont: [
        'Do not use it for editable data; open it as an artifact or in DataTable instead.',
        'Do not pre-format numbers inconsistently; mixed formats make a column text.',
      ],
    },
    related: {
      items: [
        { id: 'ai-structured-answer', reason: 'One record as labelled fields.' },
        { id: 'data-table', reason: 'Product data tables with selection and paging.' },
        { id: 'ai-artifact-canvas', reason: 'Where “Open as artifact” usually leads.' },
        { id: 'copy-button', reason: 'Used for Copy as CSV.' },
      ],
    },
    dependencies: { packages: ['react'], components: ['button', 'copy-button'] },
    testing: {
      unit: [
        'Type inference for numbers, dates and text; inferred columns.',
        'CSV escaping (quotes, commas, line breaks, edge spaces) and formula neutralisation.',
        'Keyboard sorting with `aria-sort` and announcements; numeric and date order.',
        'Copy as CSV uses the sorted rows; streaming busy state and completion; empty cells and tables; no HTML from model output.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
