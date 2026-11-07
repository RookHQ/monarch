import { defineStatuses } from '@rookcom/monarch-react';
import { SavingsPanel } from '@rookcom/monarch-react/patterns';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const statuses = defineStatuses({
  parked: { label: 'Parked', tone: 'neutral' },
  no_website: { label: 'No website', tone: 'info' },
  no_dns: { label: 'No DNS', tone: 'warning' },
  keep: { label: 'Keep', tone: 'success' },
  review: { label: 'Review', tone: 'info' },
  let_lapse: { label: 'Let lapse', tone: 'danger' },
});

export default {
  id: 'savings-panel',
  title: 'SavingsPanel',
  summary: 'What unused items cost to renew, why they count as unused and what has been decided.',
  sections: {
    purpose: {
      summary:
        'Shows the cost of keeping things nobody uses: a headline count and renewal cost per currency, the reasons items count as unused, the keep / review / let-lapse decisions so far, what letting them lapse saves, and the top items. Needed by: Atlas (Renewals and Overview).',
      useWhen: [
        'A portfolio of renewable items where some are unused and cost money to renew.',
        'Prompting a review of spend without estimating unknown prices.',
      ],
      avoidWhen: [
        'A full list of items: use RenewalsPanel or a table.',
        'Forecasting spend over time: use a chart.',
      ],
    },
    designPrinciples: {
      items: [
        'Only real prices are added up, per currency; unpriced items are counted, never estimated.',
        'The product decides what “unused” means and passes the numbers in.',
        'Categories and decisions are StatusBadges from the product’s registry.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: ['The ref and native attributes go to the root `<section>`, named by its heading.'],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Migrating from the app copy (Atlas `features/renewals/components/SavingsPanel`): the `useSavings()` query stays in Atlas; `s.unusedCount` → `unusedCount`; `s.renewalCost` (`amountMinor`, `currency`) → `costs` (`amount`, `currency`) with `minorUnits`; `s.unpricedCount` → `unpricedCount`; `s.byClass.filter(unused)` → `categories` (`class` → `id`, with `href` to the filtered list); `s.decisions` → `decisions` as `{ id, label, count }[]`; `s.letLapseSavings` → `potentialSavings`; `s.items` → `items` (`usageClass` → `category`, `expiresAt` → `date`, `renewal.priceMinor` → `amount`, `decision`); the “Review all” link → `actions`; `UsageChip`/`DecisionChip` → a `statuses` registry; `isLoading`/`error`/`refetch` → `loading`/`error`/`onRetry`. The i18n strings → `messages`.',
      ],
    },
    variants: {
      items: [],
      notes: [
        'No visual variants. Sections without data (categories, decisions, savings, items) are simply left out.',
      ],
    },
    states: {
      items: [
        {
          name: 'Loading',
          attribute: 'data-loading',
          description: 'Skeleton; `aria-busy` on the section.',
        },
        {
          name: 'Error',
          attribute: 'data-error',
          description: 'InlineErrorRetry with `onRetry`, otherwise an alert.',
        },
        {
          name: 'Empty',
          attribute: 'data-empty',
          description: '`unusedCount` is 0: an EmptyState.',
        },
      ],
    },
    interaction: {
      items: [
        'Categories and items with `href` are links (use `renderLink` for a router). Nothing else is interactive.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Moves through header actions, category links and item links.' },
        { keys: 'Enter', action: 'Follows the focused link.' },
      ],
    },
    accessibility: {
      role: 'region',
      name: 'Its heading (`title`, default “Savings”).',
      items: [
        'Category and item lists are real lists with names (“Unused by reason”, “Unused items”).',
        'A category link reads its label and count, e.g. “Parked 2”.',
        'Decision counts are prefixed with a visually hidden “Decisions:”.',
      ],
    },
    responsive: {
      items: [
        'Everything wraps; item facts drop under the name on narrow widths, so 390px never scrolls sideways.',
        'Links reach 44px on coarse pointers.',
      ],
    },
    theme: {
      items: [
        'Only `--mn-*` tokens; badge tones come from the status registry. Forced-colours mode keeps borders and link colours.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Unused items',
          render: () => (
            <SavingsPanel
              statuses={statuses}
              description="Parked, without a website or without DNS."
              unusedCount={3}
              costs={[
                { amount: 4500, currency: 'USD' },
                { amount: 2000, currency: 'EUR' },
              ]}
              unpricedCount={1}
              categories={[
                { id: 'parked', count: 2, href: '#parked' },
                { id: 'no_dns', count: 1, href: '#no-dns' },
              ]}
              decisions={[
                { id: 'keep', label: 'Keep', count: 1 },
                { id: 'let_lapse', label: 'Let lapse', count: 1 },
                { id: 'undecided', label: 'Undecided', count: 1 },
              ]}
              potentialSavings={[{ amount: 1500, currency: 'USD' }]}
              items={[
                {
                  id: 'a',
                  name: 'example.com',
                  href: '#example-com',
                  category: 'parked',
                  date: '2027-01-10T00:00:00Z',
                  amount: { amount: 1500, currency: 'USD' },
                  decision: 'let_lapse',
                },
                {
                  id: 'b',
                  name: 'example.net',
                  category: 'no_dns',
                  date: '2027-03-02T00:00:00Z',
                  amount: null,
                  decision: 'review',
                },
                {
                  id: 'c',
                  name: 'example.org',
                  category: 'parked',
                  date: '2027-04-20T00:00:00Z',
                  amount: { amount: 2000, currency: 'EUR' },
                  decision: 'keep',
                },
              ]}
              actions={<a href="#all">Review all</a>}
              minorUnits
              locale="en-US"
              timeZone="UTC"
            />
          ),
        },
        {
          title: 'Nothing unused',
          render: () => <SavingsPanel unusedCount={0} />,
        },
      ],
    },
    doDont: {
      do: [
        'Pass one cost entry per currency.',
        'Link categories to the filtered list so the user can act.',
        'Pass `locale` and `timeZone` for stable dates.',
      ],
      dont: [
        'Do not estimate missing prices; pass `unpricedCount`.',
        'Do not convert currencies to make one total.',
      ],
    },
    related: {
      items: [
        { id: 'renewals-panel', reason: 'The full list of upcoming renewals.' },
        { id: 'money', reason: 'Formats costs and savings.' },
        { id: 'status-badge', reason: 'Category and decision badges.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['money', 'status-badge', 'empty-state', 'inline-error-retry', 'skeleton'],
    },
    testing: {
      unit: [
        'Region named by its heading with the headline; axe clean.',
        'Categories with counts, empty ones skipped, links and focus.',
        'Decision summary and potential savings.',
        'Items with date, price or “No price”; `maxItems` and “and N more”.',
        'Empty, loading, error with retry and error alert.',
        'Custom title, heading level and messages; server rendering.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
