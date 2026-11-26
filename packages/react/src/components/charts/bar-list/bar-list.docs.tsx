import { BarList } from '@rookcom/monarch-react/charts';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const pages = [
  { id: 'home', label: '/', value: 4820, href: '#/components/bar-list' },
  { id: 'pricing', label: '/pricing', value: 2310, href: '#/components/bar-list' },
  { id: 'docs', label: '/docs/getting-started', value: 1640, href: '#/components/bar-list' },
  { id: 'blog', label: '/blog/release-notes', value: 920, href: '#/components/bar-list' },
  { id: 'contact', label: '/contact', value: 310, href: '#/components/bar-list' },
];

export default {
  id: 'bar-list',
  title: 'BarList',
  summary: 'A ranked list with an inline bar per row: top pages, referrers, rivals by score.',
  sections: {
    purpose: {
      summary:
        'Compares a handful of named values at a glance. Each row keeps its label and value as text, so the list reads correctly without the bars.',
      useWhen: [
        'Top-N breakdowns: pages, referrers, campaigns, countries, engines.',
        'Comparing your entity against others (`highlighted`), with missing values shown honestly.',
      ],
      avoidWhen: [
        'Values over time: use a line or bar chart.',
        'Parts of one whole that should add to 100%: use StackedProportionBar or a donut.',
        'More than about 10 rows or several columns of numbers: use a DataTable.',
      ],
    },
    designPrinciples: {
      items: [
        'Text first: labels and values are real list text; bars are decorative (`aria-hidden`).',
        'Largest first by default; missing values sort last and read as "No value", never as zero.',
        'One series colour (the first data palette colour); a highlighted row keeps it and the others fade.',
        'Values are formatted with Intl.NumberFormat (or Money for currency) in the viewer’s locale.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'The ref and native attributes go to the root `<div>`; the list is a `<ul>`, or an `<ol>` when `ranked`.',
        'Exported from `@rookcom/monarch-react/charts`, with the `BarListItem` type.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Bars are scaled to the largest value unless `max` is set. For ratios of a whole, pass `max={1}`.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'layout',
          values: ['stacked', 'inline'],
          description:
            '`stacked` (default) puts a thin bar under each label; `inline` puts label, bar and value on one row from 30rem up (`data-layout`).',
        },
        {
          prop: 'format',
          values: ['number', 'percent', 'currency'],
          description:
            'How values are written; `notation="compact"`, `unit` and `formatOptions` refine it, `formatValue` replaces it.',
        },
        {
          prop: 'sort',
          values: ['desc', 'asc', 'none'],
          description: 'Row order; `none` keeps the given order.',
        },
        {
          prop: 'ranked',
          values: ['false', 'true'],
          description: 'Shows rank numbers and renders an ordered list (`data-ranked`).',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Highlighted row',
          attribute: 'data-highlighted',
          description: 'Bold label; the other rows’ bars fade. Put "you" in the label too.',
        },
        {
          name: 'Missing value',
          attribute: 'data-missing',
          description: 'A dash read as `missingLabel` ("No value"), no bar, sorted last.',
        },
        {
          name: 'Linked row',
          attribute: 'data-linked',
          description: 'The label is a link and the whole row is its click target.',
        },
        {
          name: 'Overflow',
          description: 'With `limit`, a "Top 5 of 12" footer (or `overflowLabel`).',
        },
        {
          name: 'Loading',
          attribute: 'data-loading',
          description: 'Placeholder rows, `aria-busy`, and a hidden "Loading {label}".',
        },
        {
          name: 'Empty',
          attribute: 'data-empty',
          description: '`emptyState` text instead of a list.',
        },
      ],
    },
    interaction: {
      items: [
        'Rows with `href` are links; clicking anywhere on the row follows the link.',
        'Hovering a linked row tints it and underlines the label.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab / Shift+Tab', action: 'Moves between linked rows.' },
        { keys: 'Enter', action: 'Follows the focused row’s link.' },
      ],
      notes: ['Rows without `href` are not focusable.'],
    },
    accessibility: {
      role: 'list (`<ul>` or `<ol>`, with an explicit role so Safari keeps it)',
      name: '`label`, e.g. "Top pages".',
      items: [
        'Each row reads its label, description and value; the bar is hidden from assistive technology.',
        'A missing value is announced as "No value", not only shown in a tooltip.',
        'Rank numbers are hidden because an ordered list already announces positions.',
        'Linked rows have a focus ring around the whole row and are named by their label.',
        'In forced-colours mode bars are drawn in CanvasText with an outlined track.',
      ],
    },
    responsive: {
      items: [
        'Labels and descriptions truncate with an ellipsis; values never wrap.',
        'The inline layout falls back to stacked below 30rem, so it works at 320px.',
      ],
    },
    theme: {
      items: [
        'Bars use `--mn-color-data-1` on a `--mn-color-bg-muted` track, which are defined for light, dark and high-contrast themes.',
        'Values use the default text colour; rank and descriptions are muted.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Top pages with links',
          stack: true,
          render: () => (
            <div style={{ width: '100%', maxWidth: '28rem' }}>
              <BarList label="Top pages" items={pages} unit="views" locale="en-US" />
            </div>
          ),
        },
        {
          title: 'Ranked comparison, inline layout',
          stack: true,
          render: () => (
            <div style={{ width: '100%', maxWidth: '36rem' }}>
              <BarList
                label="Visibility score"
                ranked
                layout="inline"
                max={100}
                limit={4}
                items={[
                  { id: 'a', label: 'Northfield', value: 82 },
                  { id: 'you', label: 'Your site (you)', value: 71, highlighted: true },
                  { id: 'b', label: 'Cobalt', value: 64 },
                  { id: 'c', label: 'Fernway', value: null },
                  { id: 'd', label: 'Tidewater', value: 38 },
                ]}
              />
            </div>
          ),
        },
        {
          title: 'Rates with a description and display value',
          stack: true,
          render: () => (
            <div style={{ width: '100%', maxWidth: '28rem' }}>
              <BarList
                label="Reply rate by channel"
                format="percent"
                max={1}
                items={[
                  {
                    id: 'e',
                    label: 'Email',
                    description: 'Spring outreach',
                    value: 0.12,
                    displayValue: '12% · 4 of 33',
                  },
                  {
                    id: 'l',
                    label: 'LinkedIn',
                    description: 'Founders list',
                    value: 0.21,
                    displayValue: '21% · 9 of 43',
                  },
                  { id: 'c', label: 'Calls', description: 'Renewals', value: 0.05 },
                ]}
                locale="en-US"
              />
            </div>
          ),
        },
        {
          title: 'Loading and empty',
          stack: true,
          render: () => (
            <div style={{ display: 'grid', gap: '1.5rem', width: '100%', maxWidth: '28rem' }}>
              <BarList label="Top referrers" items={[]} loading loadingRows={3} />
              <BarList label="Top referrers" items={[]} emptyState="No referrers in this period" />
            </div>
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Name the list with `label` and keep labels short; use `description` for the second detail.',
        'Pass `null` for an unknown value so it reads as "No value" instead of a misleading zero.',
        'Use `limit` with an honest "Top 5 of 12" footer, and link to the full list.',
      ],
      dont: [
        'Do not put information only in the bar length; the value text is what everyone reads.',
        'Do not colour rows with status colours to rank them.',
        'Do not use a BarList for shares that must add to 100%.',
      ],
    },
    related: {
      items: [
        { id: 'bar-chart', reason: 'Values over time or many categories with axes.' },
        { id: 'stacked-proportion-bar', reason: 'Parts of one whole in a single bar.' },
        { id: 'data-table', reason: 'Many rows or several numeric columns.' },
        { id: 'progress-bar', reason: 'One value against a target or quota.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['money', 'skeleton', 'visually-hidden'],
    },
    testing: {
      unit: [
        'Named list, largest first, text values and decorative bars scaled to the largest value.',
        'sort none/asc, ranked ordered list.',
        'Percent, currency, compact, unit and custom formatting.',
        'Missing values announced and sorted last; display values.',
        'Linked rows reachable with Tab, custom router link component.',
        'limit with default and custom overflow footer; highlighted rows.',
        'Empty and loading states; zero values; ref and layout.',
      ],
      axe: true,
      ssr: true,
      browser: ['Visual baselines: /__gallery/bar-list', 'Cross-browser axe: /components/bar-list'],
    },
  },
} satisfies ComponentDocs;
