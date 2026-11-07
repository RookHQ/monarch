import { SubscriptionsTable } from '@rookcom/monarch-react/patterns';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const sample = [
  {
    id: 's1',
    name: 'Support plan',
    href: '#support-plan',
    plan: 'Standard',
    quantity: 3,
    customer: 'Example Co',
    status: 'active',
    amount: 12000,
    currency: 'USD',
    interval: 'month' as const,
    monthly: { amount: 12000, currency: 'USD' },
    renewalDate: '2027-01-15T00:00:00Z',
  },
  {
    id: 's2',
    name: 'Hosting',
    plan: 'Annual hosting',
    customer: 'Sample Ltd',
    status: 'past_due',
    amount: 90000,
    currency: 'EUR',
    interval: 'year' as const,
    monthly: null,
    renewalDate: '2027-03-01T00:00:00Z',
    autoRenew: false,
  },
  {
    id: 's3',
    name: 'Workshop series',
    customer: 'Acme Studio',
    status: 'trial',
    amount: 45000,
    currency: 'USD',
    interval: 'month' as const,
    intervalCount: 3,
    monthly: { amount: 15000, currency: 'USD' },
    cancelAt: '2027-02-01T00:00:00Z',
  },
];

export default {
  id: 'subscriptions-table',
  title: 'SubscriptionsTable',
  summary:
    'Recurring agreements with status, price per period, normalised monthly amount and renewal.',
  sections: {
    purpose: {
      summary:
        'Lists recurring agreements the same way in every product: what it is, who it is for, its status, its price per billing period in its own currency, the normalised monthly figure and when it renews or ends. Needed by: Dots (Retainers → Subscriptions and the client Subscriptions tab).',
      useWhen: [
        'A list of subscriptions, retainers or plans, across customers or for one customer.',
        'Showing what each agreement is worth per period without converting currencies in the UI.',
      ],
      avoidWhen: [
        'Large lists that need sorting, selection or column choice: use DataGrid with Money and StatusBadge cells.',
        'A single subscription’s details: use DescriptionList.',
      ],
    },
    designPrinciples: {
      items: [
        'Data in, links out: the product filters, sorts and fetches; the table only displays.',
        'Each amount stays in its own currency. A missing monthly figure on a live subscription says “Not converted” instead of guessing.',
        'Statuses come from a registry (`subscriptionStatuses` by default), so text and an icon carry the meaning, never colour alone.',
        'On narrow screens secondary columns fold under the name instead of scrolling sideways.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'The ref and native attributes go to the root `<div>`.',
        '`subscriptionStatuses` is the default registry (draft, trial, active, past_due, paused, cancelled, expired); pass `statuses` built with `defineStatuses` to add or rename.',
        '`formatBillingInterval(interval, count)` returns “month”, “quarter”, “2 weeks” or “one time”.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Migrating from the app copy (Dots `components/commercial/SubscriptionsTable`): `items` → `subscriptions`; `slug` is gone, give each row an `href` and pass `renderLink` for the router; `showClient` → `showCustomer`; `s.client.businessName` → `customer`; `s.product.name` → `plan`; `s.netAmount` → `amount`; `s.interval`/`s.intervalCount` → `interval` (lower case) / `intervalCount`; `s.countedMrr` + `s.baseCurrency` → `monthly: { amount, currency }`, and `baseMonthlyAmount === null` → `monthly: null`; `s.renewalDate`, `s.autoRenew`, `s.cancelAt`, `s.pendingChangeAt` keep their names. Status keys are lower case (`ACTIVE` → `active`). Pass `minorUnits` if amounts are in cents.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'showCustomer',
          values: ['true', 'false'],
          description: 'Hide the customer column on a single customer’s page.',
        },
        {
          prop: 'showMonthly',
          values: ['true', 'false'],
          description: 'Normalised monthly column. Default: shown when any row has `monthly`.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Empty',
          attribute: 'data-empty',
          description: 'No rows: an EmptyState (or `empty`) instead of the table.',
        },
        {
          name: 'Row status',
          attribute: 'data-status',
          description: 'Each row carries its status key for styling or tests.',
        },
        {
          name: 'Not converted',
          description:
            'A live (active or past due) row with `monthly: null` shows “Not converted” with an explanation in its title.',
        },
      ],
    },
    interaction: {
      items: [
        'Rows with `href` link their name. Nothing else is interactive, so the table never traps focus.',
      ],
    },
    keyboard: {
      items: [{ keys: 'Tab', action: 'Moves between subscription links in reading order.' }],
    },
    accessibility: {
      role: 'table',
      name: 'The caption (`caption`, default “Subscriptions”), visually hidden unless `hideCaption` is false.',
      items: [
        'Real `<table>` markup with column headers, so screen readers announce the column for each cell.',
        'Statuses are StatusBadges with text and a distinct icon.',
        'Amounts use Money: tabular figures and a spoken “No amount” for a missing value.',
      ],
    },
    responsive: {
      items: [
        'Uses a container query on its own width, so it adapts inside narrow panels as well as on phones.',
        'Below 48rem the monthly column is hidden; below 36rem the customer and renewal columns fold into a line under the name, so a 390px screen never scrolls sideways.',
        'Links grow to 44px tall on coarse pointers.',
      ],
    },
    theme: {
      items: [
        'Colours come from `--mn-color-*` tokens and work in light, dark and both more-contrast themes.',
        '“Not converted” uses `--mn-color-warning-strong`; forced-colours mode falls back to system colours.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Across customers',
          render: () => (
            <SubscriptionsTable subscriptions={sample} minorUnits locale="en-US" timeZone="UTC" />
          ),
        },
        {
          title: 'One customer, no monthly column',
          render: () => (
            <SubscriptionsTable
              subscriptions={sample.slice(0, 2)}
              showCustomer={false}
              showMonthly={false}
              minorUnits
              locale="en-US"
              timeZone="UTC"
              caption="Example Co subscriptions"
            />
          ),
        },
        {
          title: 'Empty',
          render: () => <SubscriptionsTable subscriptions={[]} />,
        },
      ],
    },
    doDont: {
      do: [
        'Give amounts in minor units with `minorUnits` and the currency of each row.',
        'Pass `locale` and `timeZone` from the server so dates match after hydration.',
        'Pass `monthly: null` for a live subscription you could not convert, so it reads “Not converted”.',
      ],
      dont: [
        'Do not convert or add currencies in the UI to fill the monthly column.',
        'Do not show raw enum statuses; add them to a registry with readable labels.',
      ],
    },
    related: {
      items: [
        { id: 'renewals-panel', reason: 'The renewals that come from these subscriptions.' },
        { id: 'money', reason: 'Formats every amount in the table.' },
        { id: 'status-badge', reason: 'Status cells and the status registry.' },
        { id: 'data-table', reason: 'The table primitives it is built on.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['data-table', 'money', 'status-badge', 'empty-state'],
    },
    testing: {
      unit: [
        'Named table with one row per subscription; axe clean.',
        'Price per period, quantity, plan, scheduled changes, renewal, end and evergreen text.',
        '“Not converted” for live rows without a monthly figure.',
        'Links, keyboard focus and a custom `renderLink`.',
        'Hiding the customer and monthly columns; empty state and message overrides.',
        'Server rendering.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
