import { useState } from 'react';
import { Button, type ButtonProps } from '@rookcom/monarch-react';
import {
  BillingPeriodToggle,
  PricingTable,
  type BillingPeriodOption,
  type PricingFeature,
  type PricingPlan,
} from '@rookcom/monarch-react/patterns';
import type { ComponentDocs } from '../../../internal/component-docs.js';
// The scenario of the primary example, shared with iOS and Android (CROSS-PLATFORM.md section 8).
import fixture from './pricing-table.fixture.json';

const periods = fixture.data.periods as unknown as BillingPeriodOption[];

const plans: PricingPlan[] = (
  fixture.data.plans as unknown as readonly (Omit<PricingPlan, 'action'> & {
    action: {
      label: string;
      variant: NonNullable<ButtonProps['variant']>;
      size: NonNullable<ButtonProps['size']>;
    };
  })[]
).map(({ action, ...plan }) => ({
  ...plan,
  action: (
    <Button variant={action.variant} size={action.size}>
      {action.label}
    </Button>
  ),
}));

const features = fixture.data.features as unknown as PricingFeature[];

function ControlledPeriod() {
  const [period, setPeriod] = useState('yearly');
  return (
    <div style={{ display: 'grid', gap: 16 }}>
      <BillingPeriodToggle periods={periods} value={period} onValueChange={setPeriod} />
      <PricingTable
        plans={plans}
        features={features.slice(0, 2)}
        periods={periods}
        period={period}
        showPeriodToggle={false}
        caption="Plans"
      />
    </div>
  );
}

export default {
  id: 'pricing-table',
  title: 'PricingTable',
  summary: 'Plans × features comparison table with a monthly / yearly toggle.',
  sections: {
    purpose: {
      summary:
        'Compares plans feature by feature in a real table, with prices per billing period and one call to action per plan. Needed by: Links (pricing and billing pages), Recruit (marketing pricing); used by PricingSection and ComparisonSection.',
      useWhen: [
        'Visitors need to see which plan includes which feature.',
        'Prices differ by billing period (monthly, yearly).',
      ],
      avoidWhen: [
        'Two to four plans with short feature lists: PlanCard grids read faster (PricingSection shows both).',
        'Comparing your product with alternatives on a marketing page: use ComparisonSection.',
        'Tabular data that is not plans: use DataTable.',
      ],
    },
    designPrinciples: {
      items: [
        'A comparison is a table: plans are column headers and features are row headers, so every cell is announced with both.',
        'Checks and dashes are written as "Included" / "Not included".',
        'Prices are shown exactly as given; a plan without a price for the period falls back to `price` (for example "Contact us") without the period suffix.',
        'The page never scrolls sideways: only the table area does, and the feature column stays in view.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'The ref and native attributes go to the root `<div>`; the table sits in a focusable scroll region named by the caption.',
        '`BillingPeriodToggle` is the same toggle on its own, for pages that place it elsewhere.',
        '`planPriceFor(plan, period)`, `groupPricingFeatures(features)` and `useBillingPeriod(...)` are the helpers PricingSection uses.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Each `PricingFeature.values[planId]`: `true` is a check, `false`, `null` or missing is a dash, anything else (text, a number, an element) is shown as is.',
        'Migrating from the app copies (Links `components/PricingSection.tsx`, Recruit `components/marketing/shared/PricingTiers.tsx`): each tier becomes a `PricingPlan` (`price` → `prices[period]` or `price`, `highlighted`, the CTA → `action`); each feature string becomes a `PricingFeature` row with a value per plan. Neither app used shadcn/ui here.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'showPrices',
          values: ['true', 'false'],
          description: '`false` for comparisons without prices (ComparisonSection uses it).',
        },
        {
          prop: 'showPeriodToggle',
          values: ['auto', 'true', 'false'],
          description: 'Default: shown when there are two or more periods.',
        },
        {
          prop: 'hideCaption',
          values: ['true', 'false'],
          description: 'The caption is visually hidden by default; it stays the accessible name.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Period',
          attribute: 'data-period',
          description: 'The selected billing period value, on the root.',
        },
        {
          name: 'Highlighted plan',
          attribute: 'data-highlighted',
          description: 'On the plan header and its cells: an accent-subtle column.',
        },
        {
          name: 'Current plan',
          attribute: 'data-current',
          description: 'On the plan header, with a written "Current plan" badge.',
        },
        {
          name: 'Cell value',
          attribute: 'data-value',
          description: '`included` or `not-included` on check and dash marks.',
        },
      ],
    },
    interaction: {
      items: [
        'Choosing a period updates every price at once and calls `onPeriodChange`.',
        'Controlled (`period`) or uncontrolled (`defaultPeriod`).',
        'On narrow screens the table area scrolls sideways with the feature column pinned.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Tab',
          action: 'Moves to the period toggle, then the table area, then each plan action.',
        },
        {
          keys: 'Arrow keys',
          action: 'In the toggle: move to and select the next or previous period.',
        },
        { keys: 'Home / End', action: 'In the toggle: select the first or last period.' },
        { keys: 'Arrow Left / Right', action: 'In the focused table area: scroll sideways.' },
      ],
    },
    accessibility: {
      role: 'table inside a named, focusable region; the toggle is a radiogroup',
      name: 'The table caption (default "Plan comparison"); the toggle is "Billing period".',
      items: [
        'Plan headers use `scope="col"`, features `scope="row"`, group headers `scope="colgroup"`.',
        'The empty corner header has hidden "Feature" text.',
        'Check and dash icons are hidden; their cells contain "Included" / "Not included" text.',
        'The toggle follows the WAI-ARIA radio group pattern (SegmentedControl).',
      ],
    },
    responsive: {
      items: [
        'At 390px the page does not scroll sideways: the table area does, with the feature column sticky on the inline start.',
        'Column widths shrink when the table itself is narrower than 44rem (a query on its own width, not the window), so three plans fit a 620px column without scrolling; long values wrap between words.',
        'The toggle meets 44px touch targets on coarse pointers (SegmentedControl).',
        'RTL: the sticky column and alignment use logical properties.',
      ],
    },
    theme: {
      items: [
        'Surface, border, accent-subtle and text tokens only, in every theme and product accent.',
        'Forced-colours mode draws borders in CanvasText and the highlighted plan with a Highlight top border.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Plans with a billing-period toggle',
          stack: true,
          render: () => (
            <PricingTable
              plans={plans}
              features={features}
              periods={periods}
              caption={fixture.data.caption}
            />
          ),
        },
        {
          title: 'Toggle placed by the page (controlled)',
          stack: true,
          render: () => <ControlledPeriod />,
        },
        {
          title: 'Feature comparison without prices',
          stack: true,
          render: () => (
            <PricingTable
              plans={plans.map(({ action: _action, ...plan }) => plan)}
              features={features}
              showPrices={false}
              caption="Features by plan"
              hideCaption={false}
            />
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Use the real published prices; placeholders here are only for documentation.',
        'Group long feature lists with `group`.',
        'Give the period a factual `suffix` ("2 months included") only when it is true.',
      ],
      dont: [
        'Do not compute discounts in the UI; pass the prices the billing system publishes.',
        'Do not use colour alone to show which plan includes a feature.',
        'Do not put more than five plans in one table.',
      ],
    },
    related: {
      items: [
        { id: 'plan-card', reason: 'One plan as a card.' },
        {
          id: 'pricing-section',
          reason: 'Cards plus this table under one toggle, on a marketing page.',
        },
        { id: 'comparison-section', reason: 'Your product beside alternatives.' },
        { id: 'segmented-control', reason: 'The billing-period toggle.' },
        { id: 'data-table', reason: 'Sortable, filterable data that is not plans.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['badge', 'segmented-control', 'visually-hidden'],
    },
    testing: {
      unit: [
        'Captioned table with plan column headers, feature row headers and a focusable named scroll region; axe clean.',
        'Check and dash cells read "Included" / "Not included"; text values shown as given.',
        'Group headers span every column.',
        'Billing-period radio group: arrow keys switch every price, suffix and note; fallback prices get no suffix.',
        'Controlled period and a hidden toggle; highlighted, current, hidden prices and translated messages.',
        'BillingPeriodToggle on its own; helpers; server rendering.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
