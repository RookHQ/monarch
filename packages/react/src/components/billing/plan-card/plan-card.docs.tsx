import { Button } from '@rookcom/monarch-react';
import { PlanCard } from '@rookcom/monarch-react/patterns';
import type { ComponentDocs } from '../../../internal/component-docs.js';

export default {
  id: 'plan-card',
  title: 'PlanCard',
  summary: 'One plan: name, price as given, who it is for, what it includes and a call to action.',
  sections: {
    purpose: {
      summary:
        'Shows a single plan in a pricing grid or an in-app plan picker, with the price exactly as the product supplies it. Needed by: Links (pricing section and billing page), Recruit (marketing pricing tiers); used by PricingSection.',
      useWhen: [
        'A grid of two to four plans on a pricing page.',
        'An in-app upgrade screen that marks the current plan.',
      ],
      avoidWhen: [
        'Comparing many features across plans: use PricingTable.',
        'A full pricing band with heading and period toggle: use PricingSection.',
      ],
    },
    designPrinciples: {
      items: [
        'Prices are content, not calculations: the card shows what it is given (text or a Money element) and never formats, converts or invents a figure.',
        'One card is one heading: the plan name names the article, so screen-reader users can jump between plans.',
        'Emphasis is a border and a written badge, never colour alone.',
        'Excluded features are written as "Not included", not just greyed out.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'The ref and native attributes go to the root `<article>`.',
        '`defaultPlanCardMessages` holds the English defaults.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Migrating from the app copies (Links `components/PricingSection.tsx`, Recruit `components/marketing/shared/PricingTiers.tsx`): `tier.name` → `name`, `tier.price` → `price`, `tier.sub` / `plan.period` → `period`, `tier.description` → `description`, `tier.features` → `features`, `tier.highlighted` / `plan.popular` → `highlighted` with a `badge` text, and the CTA link → `action` (a Button with `asChild` around the link).',
        'Neither app copy used shadcn/ui for the card; there is no shadcn prop mapping.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'highlighted',
          values: ['false', 'true'],
          description: 'Accent border and shadow for the recommended plan; pair it with `badge`.',
        },
        {
          prop: 'headingAs',
          values: ['h2', 'h3', 'h4'],
          description: 'Heading level of the plan name, to fit the page outline.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Highlighted',
          attribute: 'data-highlighted',
          description: 'The recommended plan.',
        },
        {
          name: 'Current',
          attribute: 'data-current',
          description: 'The organisation’s current plan; a "Current plan" badge is written.',
        },
        {
          name: 'Feature included',
          attribute: 'data-included',
          description: 'Per feature: `true` (check) or `false` (dash, muted, "Not included").',
        },
      ],
    },
    interaction: {
      items: ['The card itself is not interactive; the `action` keeps its own behaviour.'],
    },
    keyboard: {
      items: [{ keys: 'Tab', action: 'Moves to the action inside the card.' }],
      notes: ['Nothing else to operate.'],
    },
    accessibility: {
      role: 'article',
      name: 'The plan name heading (`aria-labelledby`), unless `aria-label` is given.',
      items: [
        'Features are a list; excluded features start with hidden "Not included:" text.',
        'Check and dash icons are decorative (`aria-hidden`).',
        'Badges are text, so emphasis and current state are not colour-only.',
      ],
    },
    responsive: {
      items: [
        'The card fills its grid cell; the price, badges and features wrap at 390px.',
        'Padding tightens below 480px.',
      ],
    },
    theme: {
      items: [
        'Surface, border and accent tokens only; works in light, dark and every product accent.',
        'Forced-colours mode keeps borders and draws the highlighted card in Highlight.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Placeholder price and features',
          render: () => (
            <div style={{ maxWidth: 320 }}>
              <PlanCard
                name="Team"
                price="₹X"
                period="/ month"
                priceNote="Billed monthly"
                description="For teams that share work every day."
                featuresLabel="Everything in Starter, plus"
                features={[
                  'Shared projects',
                  { label: 'Audit log', hint: 'last 90 days' },
                  { label: 'Single sign-on', included: false },
                ]}
                highlighted
                badge="Recommended"
                action={<Button variant="primary">Choose Team</Button>}
              />
            </div>
          ),
        },
        {
          title: 'Current plan',
          render: () => (
            <div style={{ maxWidth: 320 }}>
              <PlanCard
                name="Starter"
                price="₹Y"
                period="/ month"
                current
                features={['Up to 3 projects', 'Email support']}
                action={<Button variant="secondary">Manage plan</Button>}
              />
            </div>
          ),
        },
        {
          title: 'No published price',
          render: () => (
            <div style={{ maxWidth: 320 }}>
              <PlanCard
                name="Enterprise"
                price="Contact us"
                description="For organisations with procurement and security reviews."
                features={['Custom contract', 'Dedicated onboarding']}
                action={<Button variant="secondary">Talk to sales</Button>}
              />
            </div>
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Pass the price the product publishes, as text or `<Money>`.',
        'Highlight at most one plan, and say why in `badge`.',
        'Write "Contact us" (or similar) when a plan has no published price.',
      ],
      dont: [
        'Do not put placeholder or made-up prices in production.',
        'Do not rely on the highlight colour alone to recommend a plan.',
        'Do not put several competing buttons in one card.',
      ],
    },
    related: {
      items: [
        { id: 'pricing-table', reason: 'Plans side by side with a feature comparison.' },
        { id: 'pricing-section', reason: 'A full marketing pricing band built from PlanCards.' },
        { id: 'money', reason: 'Formats a real amount for `price`.' },
        { id: 'subscriptions-table', reason: 'Subscriptions an organisation already has.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['badge', 'visually-hidden'],
    },
    testing: {
      unit: [
        'Article named by the plan heading; price, period, note and features; axe clean.',
        'Excluded features read "Not included:" and carry `data-included="false"`.',
        'Highlighted, badge and current-plan states written as text; axe clean.',
        'Messages translate the text; `headingAs` changes the level.',
        'Action reachable with Tab; ref forwarded; server rendering.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
