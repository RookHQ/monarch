import { Button } from '@rookcom/monarch-react';
import {
  ComparisonSection,
  type ComparisonColumn,
  type ComparisonRow,
} from '@rookcom/monarch-react/marketing';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const columns: ComparisonColumn[] = [
  { id: 'product', name: 'Product A', highlighted: true, badge: 'This product' },
  { id: 'sheets', name: 'Spreadsheets' },
  { id: 'email', name: 'Email threads' },
];

const rows: ComparisonRow[] = [
  {
    id: 'history',
    label: 'Change history',
    values: { product: true, sheets: 'Per file', email: false },
  },
  {
    id: 'assign',
    label: 'Assign an owner',
    values: { product: true, sheets: false, email: false },
  },
  {
    id: 'roles',
    label: 'Roles and permissions',
    group: 'Access',
    values: { product: true, sheets: 'Per file', email: false },
  },
  {
    id: 'export',
    label: 'Export to CSV',
    group: 'Access',
    values: { product: true, sheets: true, email: false },
  },
];

export default {
  id: 'comparison-section',
  title: 'ComparisonSection',
  summary: 'Marketing comparison: your product beside alternatives in an accessible table.',
  sections: {
    purpose: {
      summary:
        'A public-page section that compares your product with alternatives (or approaches such as spreadsheets) capability by capability, with optional "best for" cards and the small print a fair comparison needs. Needed by: Recruit (`components/marketing/shared/ComparePage.tsx`, the "vs" pages).',
      useWhen: [
        'A "how it compares" or "X vs Y" page.',
        'Showing what changes when a team moves from a manual approach to the product.',
      ],
      avoidWhen: [
        'Comparing your own plans with prices: use PricingSection or PricingTable.',
        'Unverified claims about other products: leave the row out instead.',
      ],
    },
    designPrinciples: {
      items: [
        'Factual and fair: every cell is text or a written check / dash, and `note` carries dates, sources and trademark notices.',
        'A comparison is a table, so every value is announced with its row and column.',
        'Your own column is emphasised with a border colour and a written badge, never colour alone.',
        'Built from MarketingSection and PricingTable, so it matches the pricing page.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Like the other marketing blocks, a function component without a ref; native attributes go to the `<section>`.',
        'The section is not labelled by its title (the table’s scroll region already carries that name, and two landmarks may not share one). Pass `aria-label` for a different section landmark name.',
        '`defaultComparisonSectionMessages` holds the English defaults.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        '`ComparisonRow` is PricingTable’s `PricingFeature`: `values[columnId]` is `true` (check), `false` or missing (dash), or any text.',
        'Migrating from Recruit `ComparePage`: `compare.rows[] { feature, them, rook }` → `rows` with `values: { them, ours }` and two `columns`; `bestForThem` / `bestForRook` → `bestFor` cards; the trademark paragraph → `note`; `CTABand` stays a separate section. Not a shadcn/ui copy.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'align',
          values: ['start', 'center'],
          description: 'Header alignment. Default `start`.',
        },
        {
          prop: 'tone',
          values: ['default', 'subtle', 'inverse'],
          description: 'MarketingSection band tone.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Highlighted column',
          attribute: 'data-highlighted',
          description: 'On the column header and cells (PricingTable).',
        },
        {
          name: 'Highlighted card',
          attribute: 'data-highlighted',
          description: 'On a "best for" card: accent border and subtle accent surface.',
        },
      ],
    },
    interaction: {
      items: [
        'Not interactive apart from `actions` and the table area, which scrolls sideways on narrow screens.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Moves to the table area, then to the actions.' },
        { keys: 'Arrow Left / Right', action: 'In the focused table area: scroll sideways.' },
      ],
    },
    accessibility: {
      role: 'section containing a table inside a named, focusable region',
      name: 'The table caption, default the section title (or `caption`).',
      items: [
        'Columns use `scope="col"`, capabilities `scope="row"`, groups `scope="colgroup"`.',
        'Checks and dashes have hidden "Included" / "Not included" text.',
        '"Best for" cards are a list with headings one level below the title.',
      ],
    },
    responsive: {
      items: [
        'At 390px the table area scrolls on its own with the capability column pinned; the page does not scroll sideways.',
        '"Best for" cards stack below 640px and sit side by side above it.',
        'Logical properties throughout, so RTL mirrors correctly.',
      ],
    },
    theme: {
      items: [
        'Inherits MarketingSection tones, including the inverse remap.',
        'Forced-colours mode draws card borders in CanvasText and the highlighted card in Highlight.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Product beside manual alternatives',
          stack: true,
          render: () => (
            <ComparisonSection
              eyebrow="Compare"
              title="How it compares"
              description="Neutral sample rows for documentation."
              columns={columns}
              rows={rows}
              bestFor={[
                {
                  id: 'product',
                  title: 'Product A suits',
                  description: 'Teams that share and hand over work every day.',
                  highlighted: true,
                },
                {
                  id: 'sheets',
                  title: 'Spreadsheets suit',
                  description: 'One-off lists kept by one person.',
                },
              ]}
              note="Sample note: say when the comparison was checked and against which public sources, and credit trademarks to their owners."
              actions={<Button variant="primary">Try Product A</Button>}
              spacing="sm"
            />
          ),
        },
        {
          title: 'Table only, centred',
          stack: true,
          render: () => (
            <ComparisonSection
              title="Before and after"
              align="center"
              tone="subtle"
              spacing="sm"
              columns={columns.slice(0, 2)}
              rows={rows.slice(0, 2)}
            />
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Compare only what you can verify, and date it in `note`.',
        'Write values as words where a check would oversimplify ("Per file").',
        'Credit other products’ trademarks in `note`.',
      ],
      dont: [
        'Do not invent customer names, metrics or claims about other products.',
        'Do not show a dash where you do not know the answer; leave the row out.',
        'Do not compare more than four options in one table.',
      ],
    },
    related: {
      items: [
        { id: 'pricing-table', reason: 'The table it is built on.' },
        { id: 'pricing-section', reason: 'Your own plans with prices.' },
        { id: 'marketing-section-blocks', reason: 'The section band and header.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['pricing-table', 'marketing-section-blocks'],
    },
    testing: {
      unit: [
        'Section with a table named by the title, highlighted column with a written badge and group headers; axe clean.',
        'Checks, dashes and text values are written as text.',
        'Best-for cards, note and actions; Tab reaches the table area then the actions; axe clean.',
        'Messages translate the cell text; server rendering.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
