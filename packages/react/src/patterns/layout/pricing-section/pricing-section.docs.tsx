import { Button } from '@rookcom/monarch-react';
import { PricingSection, type PricingSectionPlan } from '@rookcom/monarch-react/marketing';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const periods = [
  { value: 'monthly', label: 'Monthly', priceSuffix: '/ month' },
  { value: 'yearly', label: 'Yearly', priceSuffix: '/ year', priceNote: 'Billed yearly' },
];

const plans: PricingSectionPlan[] = [
  {
    id: 'starter',
    name: 'Starter',
    description: 'For individuals getting organised.',
    prices: { monthly: '₹A', yearly: '₹B' },
    features: ['Up to 3 projects', 'Email support', { label: 'Single sign-on', included: false }],
    action: <Button variant="secondary">Choose Starter</Button>,
  },
  {
    id: 'team',
    name: 'Team',
    description: 'For teams that share work every day.',
    prices: { monthly: '₹C', yearly: '₹D' },
    highlighted: true,
    badge: 'Recommended',
    featuresLabel: 'Everything in Starter, plus',
    features: ['Unlimited projects', 'Single sign-on', 'Audit log'],
    action: <Button variant="primary">Choose Team</Button>,
  },
  {
    id: 'enterprise',
    name: 'Enterprise',
    description: 'For organisations with procurement and security reviews.',
    price: 'Contact us',
    featuresLabel: 'Everything in Team, plus',
    features: ['Custom contract', 'Dedicated onboarding'],
    action: <Button variant="secondary">Talk to sales</Button>,
  },
];

const comparison = [
  {
    id: 'projects',
    label: 'Projects',
    values: { starter: 'Up to 3', team: true, enterprise: true },
  },
  {
    id: 'sso',
    label: 'Single sign-on',
    group: 'Security',
    values: { starter: false, team: true, enterprise: true },
  },
  {
    id: 'contract',
    label: 'Custom contract',
    group: 'Security',
    values: { enterprise: true },
  },
];

export default {
  id: 'pricing-section',
  title: 'PricingSection',
  summary: 'Marketing pricing band: heading, period toggle, plan cards and an optional comparison.',
  sections: {
    purpose: {
      summary:
        'The pricing part of a public page: a section heading, one monthly / yearly toggle, a PlanCard per plan and, optionally, the full PricingTable underneath, all switched together. Needed by: Links (`components/PricingSection.tsx`, `views/Pricing.tsx`), Recruit (`components/marketing/shared/PricingTiers.tsx`).',
      useWhen: [
        'A product site’s pricing page or pricing band.',
        'Plans whose prices differ by billing period.',
      ],
      avoidWhen: [
        'In-app plan pickers: use PlanCard or PricingTable directly inside the app layout.',
        'Comparing your product with other products: use ComparisonSection.',
      ],
    },
    designPrinciples: {
      items: [
        'One toggle drives every price on the section, so cards and table never disagree.',
        'Prices are shown exactly as given; nothing is calculated, discounted or invented.',
        'Cards are a list of articles headed one level below the section title, so the outline stays correct.',
        'Built from MarketingSection, so tone, spacing and width match the rest of the page.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Like the other marketing blocks, it is a function component without a ref; native attributes go to the `<section>` (MarketingSection).',
        '`defaultPricingSectionMessages` holds the English defaults.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        '`PricingSectionPlan` is a PricingTable `PricingPlan` plus the card’s `features`, `featuresLabel` and `footer`.',
        'Also takes MarketingSection’s `tone`, `spacing` and `width` (default `wide`).',
        'Migrating from Links `PricingSection` / Recruit `PricingTiers`: `plans[]` / `tiers[]` → `plans` (`price` → `prices` per period or `price`, `period` / `sub` → the period’s `priceSuffix` or the plan’s `priceSuffix`, `popular` / `highlighted` → `highlighted` + `badge`, `features` unchanged, CTA → `action`). The heading markup → `eyebrow`, `title`, `description`. Neither copy used shadcn/ui.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'align',
          values: ['center', 'start'],
          description: 'Header and toggle alignment. Default `center`.',
        },
        {
          prop: 'tone',
          values: ['default', 'subtle', 'inverse'],
          description: 'MarketingSection band tone.',
        },
        {
          prop: 'headingAs',
          values: ['h2', 'h3'],
          description:
            'Section heading level; plan names and the comparison heading use the next level.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Period',
          attribute: 'data-period',
          description: 'The selected billing period, on the section.',
        },
        {
          name: 'Plan states',
          attribute: 'data-highlighted / data-current',
          description: 'On each PlanCard and PricingTable column.',
        },
      ],
    },
    interaction: {
      items: [
        'Choosing a period updates every card and the comparison table, and calls `onPeriodChange`.',
        'With fewer than two periods no toggle is shown.',
        'Controlled (`period`) or uncontrolled (`defaultPeriod`).',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Tab',
          action: 'Moves through the toggle, each card’s action, then the comparison table area.',
        },
        {
          keys: 'Arrow keys',
          action: 'In the toggle: move to and select the next or previous period.',
        },
        { keys: 'Home / End', action: 'In the toggle: select the first or last period.' },
      ],
    },
    accessibility: {
      role: 'region (section labelled by its heading) containing a list of articles',
      name: 'The section title (`aria-labelledby`); each card is named by its plan name.',
      items: [
        'The toggle is a radio group named "Billing period".',
        'The comparison has a visible heading and a table captioned with the same text.',
        'Excluded card features read "Not included:"; table cells read "Included" / "Not included".',
      ],
    },
    responsive: {
      items: [
        'Columns follow the section’s own width, not the window’s: one card per row below 40rem (640px), two from 40rem, three or four from 56rem (896px). The comparison table switches to compact columns below 44rem of its own width and scrolls sideways only when the plans still do not fit.',
        'The comparison table scrolls inside its own area; the page never scrolls sideways.',
        'Toggle items are 44px tall on coarse pointers.',
      ],
    },
    theme: {
      items: [
        'Inherits MarketingSection tones, including the inverse band remap.',
        'Tokens only; forced-colours mode keeps borders and text readable.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Plans with a period toggle and comparison',
          stack: true,
          render: () => (
            <PricingSection
              eyebrow="Pricing"
              title="Plans that grow with your team"
              description="Placeholder prices for documentation; pass the prices your billing system publishes."
              plans={plans}
              periods={periods}
              comparison={comparison}
              note="Prices shown before taxes."
              spacing="sm"
            />
          ),
        },
        {
          title: 'Cards only, start-aligned on a subtle band',
          stack: true,
          render: () => (
            <PricingSection
              title="Pricing"
              align="start"
              tone="subtle"
              spacing="sm"
              plans={plans.slice(0, 2)}
            />
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Publish real prices from the billing system; use "Contact us" when there is no published price.',
        'Keep the card feature list short and put the long list in `comparison`.',
        'Write factual period suffixes such as "2 months included" only when true.',
      ],
      dont: [
        'Do not ship placeholder prices like the ones in these examples.',
        'Do not add a second period toggle for the table.',
        'Do not highlight more than one plan.',
      ],
    },
    related: {
      items: [
        { id: 'plan-card', reason: 'Each card in the section.' },
        { id: 'pricing-table', reason: 'The comparison under the cards, or a table on its own.' },
        { id: 'comparison-section', reason: 'Your product beside alternatives.' },
        { id: 'marketing-section-blocks', reason: 'The section band and header it is built on.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['plan-card', 'pricing-table', 'marketing-section-blocks'],
    },
    testing: {
      unit: [
        'Region named by its heading with one named article per plan, prices, excluded features and note; axe clean.',
        'One keyboard-operated toggle switches card prices, notes and the comparison table; axe clean.',
        'Controlled period, heading levels and a single period without a toggle.',
        'Messages translate the toggle, comparison and card text; server rendering.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
