import { useState } from 'react';
import { Button } from '@rookcom/monarch-react';
import { RenewalsPanel } from '@rookcom/monarch-react/patterns';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const NOW = '2026-11-01T00:00:00Z';

const sample = [
  {
    id: 'r1',
    name: 'Example agreement',
    href: '#example-agreement',
    description: 'Contract · Example Co',
    renewalDate: '2026-11-08T00:00:00Z',
    amount: { amount: 120000, currency: 'USD' },
    amountNote: 'per year',
    status: 'open',
    probability: 60,
    owner: 'Sam Lee',
    nextAction: 'Send a quote',
    nextActionDate: '2026-11-03T00:00:00Z',
    nextActionOverdue: true,
  },
  {
    id: 'r2',
    name: 'example.org',
    description: 'Domain',
    renewalDate: '2027-02-01T00:00:00Z',
    noticeDeadline: '2027-01-01T00:00:00Z',
    amount: null,
    autoRenew: true,
    status: 'in_progress',
  },
  {
    id: 'r3',
    name: 'Design licence',
    description: 'Subscription · Sample Ltd',
    renewalDate: '2026-10-01T00:00:00Z',
    amount: { amount: 30000, currency: 'EUR' },
    status: 'renewed',
    outcome: 'Renewed for two years',
  },
];

function FilteredExample() {
  const [filter, setFilter] = useState('open');
  const items = filter === 'open' ? sample.filter((i) => i.status !== 'renewed') : sample;
  return (
    <RenewalsPanel
      title="Renewals"
      description="Items appear when their notice window opens."
      items={items}
      filters={[
        { value: 'open', label: 'Open' },
        { value: 'all', label: 'All' },
      ]}
      filter={filter}
      onFilterChange={setFilter}
      totals={[
        { currency: 'USD', amount: 120000, count: 1 },
        { currency: 'EUR', amount: 30000, count: 1 },
      ]}
      unpricedCount={1}
      actions={
        <Button size="sm" variant="secondary">
          Check now
        </Button>
      }
      onItemAction={() => undefined}
      canAct={(i: { status: string }) => i.status !== 'renewed'}
      minorUnits
      locale="en-US"
      timeZone="UTC"
      now={NOW}
    />
  );
}

export default {
  id: 'renewals-panel',
  title: 'RenewalsPanel',
  summary:
    'Upcoming renewals and expiries with dates, notice deadlines, amounts, owners and a row action.',
  sections: {
    purpose: {
      summary:
        'One workspace for “what renews next”: subscriptions, contracts, licences or domains reaching their term end, how soon, what they cost, who owns them and their renewal status, with filters and per-currency totals. Needed by: Dots (Retainers → Renewals and the client Renewals tab) and Atlas (Renewals).',
      useWhen: [
        'A list of term ends the team has to act on before a date.',
        'Showing renewal cost per currency without converting.',
      ],
      avoidWhen: [
        'A calendar view of dates: use a calendar component.',
        'A single item’s renewal settings: use a form in its detail page.',
      ],
    },
    designPrinciples: {
      items: [
        'Data and callbacks in: the product fetches, filters, and opens its own update dialog from `onItemAction`.',
        'Urgency is text plus an icon (“Due soon”), never colour alone.',
        'Totals are per currency; unpriced items are counted, never estimated.',
        'Day counts come from the server (`daysUntil`) or from a `now` you pass, so server and client agree.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'The ref and native attributes go to the root `<section>`, named by `title` (or `aria-label`).',
        '`renewalStatuses` is the default registry: open, in_progress, renewed, lost, void (“No longer due”). Extend with `defineStatuses`, e.g. for urgency keys.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Migrating from the app copies. Dots `components/commercial/RenewalsPanel`: the fetch, refresh call and RenewalDialog stay in Dots; `status` filter state → `filters`/`filter`/`onFilterChange`; the source `<select>` → `filterControls`; the “Check for renewals now” button → `actions`; `canEdit` + Update button → `onItemAction` and `actionLabel="Update"`; per row `r.source.name` → `name`, type and client → `description`, `renewalDate`, `noticeDeadline`, `daysToRenewal` → `daysUntil`, `contractValue` + `baseCurrency` → `amount`, “/ year · expansion” → `amountNote`, `owner.name` → `owner`, `nextAction`, `nextActionAt` → `nextActionDate`, `nextActionOverdue`, `probability`, `outcome`; statuses lower case.',
        'Atlas `features/renewals/components/RenewalsView`: window buttons → `filters`; brand, client and date pickers → `filterControls`; `extra.totals` (`firmMinor`, `estimatedMinor`) → `totals` (`amount`, `estimated`) with `minorUnits`; `extra.unpricedCount` → `unpricedCount`; `daysUntilExpiry` → `daysUntil`; `renewal.priceMinor` → `amount`; `autoRenew` → `autoRenew`; registrar and last reminder → `description`; “Mark renewed” → `onItemAction` with `actionLabel="Mark renewed"`; urgency → a registry entry via `statuses`. The forecast and calendar feed stay in Atlas.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'filters',
          values: ['RenewalFilter[]'],
          description: 'A segmented filter row (time windows or status groups).',
        },
        {
          prop: 'onItemAction',
          values: ['function', 'undefined'],
          description: 'Adds an action column; omit for a read-only panel.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Loading',
          attribute: 'data-loading',
          description: 'Skeleton rows; the section has `aria-busy`.',
        },
        {
          name: 'Error',
          attribute: 'data-error',
          description: 'InlineErrorRetry with `onRetry`, or an alert without it.',
        },
        { name: 'Empty', description: 'EmptyState, or `empty`.' },
        {
          name: 'Urgent',
          attribute: 'data-urgent',
          description:
            'Open renewal within `urgentWithinDays` (default 14): warning colour, icon and “Due soon”.',
        },
        {
          name: 'Overdue',
          attribute: 'data-overdue',
          description: 'Open renewal whose date has passed.',
        },
      ],
    },
    interaction: {
      items: [
        'Choosing a filter calls `onFilterChange`; the product passes the new `items`.',
        'The row action calls `onItemAction(item)`; `canAct` hides it for some rows.',
        'Names with `href` are links; `renderLink` swaps in a router link.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Tab',
          action: 'Moves through header actions, the filter group, links and row actions.',
        },
        { keys: 'Arrow keys', action: 'Move between filter options (SegmentedControl).' },
        { keys: 'Enter / Space', action: 'Activates the focused link or action button.' },
      ],
    },
    accessibility: {
      role: 'region containing a table',
      name: 'The `title` heading, or `aria-label`.',
      items: [
        'Row actions are named with the item (“Update Example agreement”), so a list of buttons is distinguishable.',
        'The filter group is named by `messages.filters` (“Filter renewals”).',
        'On phones the header row is visually hidden but kept for screen readers.',
      ],
    },
    responsive: {
      items: [
        'Container query on its own width: below 48rem the owner column is hidden; below 34rem each row becomes a stacked card with the amount under the name, so 390px never scrolls sideways.',
        'Totals wrap into as many columns as fit.',
        'Links and buttons reach 44px on coarse pointers.',
      ],
    },
    theme: {
      items: [
        'Only `--mn-*` tokens; urgent dates use `--mn-color-warning-strong`, overdue next actions `--mn-color-danger-strong`.',
        'Forced-colours mode uses system colours and keeps borders.',
      ],
    },
    examples: {
      items: [
        { title: 'Filters, totals and actions', render: () => <FilteredExample /> },
        {
          title: 'Read only',
          render: () => (
            <RenewalsPanel
              aria-label="Renewals"
              items={sample.slice(1)}
              minorUnits
              locale="en-US"
              timeZone="UTC"
              now={NOW}
            />
          ),
        },
        {
          title: 'Loading and empty',
          stack: true,
          render: () => (
            <>
              <RenewalsPanel title="Loading" items={[]} loading />
              <RenewalsPanel title="Nothing due" items={[]} />
            </>
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Pass `now` (or `daysUntil`) from the server so day counts match after hydration.',
        'Keep totals per currency and count unpriced items.',
        'Open your own dialog from `onItemAction` and refresh `items` afterwards.',
      ],
      dont: [
        'Do not fetch inside the panel or hard-code product routes; use `href` and `renderLink`.',
        'Do not mark urgency with colour only.',
      ],
    },
    related: {
      items: [
        { id: 'subscriptions-table', reason: 'The agreements these renewals come from.' },
        { id: 'savings-panel', reason: 'Unused items you could let lapse.' },
        { id: 'status-badge', reason: 'Renewal statuses and the registry.' },
        { id: 'money', reason: 'Amounts and totals.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: [
        'data-table',
        'money',
        'status-badge',
        'segmented-control',
        'button',
        'empty-state',
        'inline-error-retry',
        'skeleton',
      ],
    },
    testing: {
      unit: [
        'Named region and table; axe clean.',
        'Dates, day counts, notice deadlines and urgency (`data-urgent`, “Due soon”).',
        'Amounts, unpriced, owner, overdue next action, probability and outcome.',
        'Named row action by keyboard; `canAct`.',
        'Filter change with arrow keys.',
        'Per-currency totals with estimated part and unpriced count.',
        'Loading, error with retry and empty states; server rendering.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
