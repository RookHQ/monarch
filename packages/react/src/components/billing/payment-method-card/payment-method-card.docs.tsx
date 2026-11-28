import { PaymentMethodCard } from '@rookcom/monarch-react/patterns';
import type { ComponentDocs } from '../../../internal/component-docs.js';
// The scenario of the primary example, shared with iOS and Android (CROSS-PLATFORM.md section 8).
import fixture from './payment-method-card.fixture.json';

const today = new Date(fixture.data.now);
type Card = {
  brand: string;
  last4: string;
  expiryMonth: number;
  expiryYear: number;
};
const [visa, mastercard, rupay] = fixture.data.cards as unknown as readonly [
  Card & { holderName: string; isDefault: boolean; removeDisabled: boolean },
  Card,
  Card,
];
const noop = () => {};

export default {
  id: 'payment-method-card',
  title: 'PaymentMethodCard',
  summary:
    'A saved card: network, last four digits, expiry, default badge, make-default and confirmed remove.',
  sections: {
    purpose: {
      summary:
        'Lists a saved payment card the same way on every billing page, with a safe, confirmed way to remove it. Needed by: Rook ID workspace billing (`rookid/app/workspaces/[id]/billing/page.tsx`, today a shadcn/ui Card with a lucide CreditCard icon). No app lists saved cards yet; this is the shared building block for when they do.',
      useWhen: [
        'A "Payment methods" list on a billing settings page.',
        'Showing which card a subscription or invoice will be charged to.',
      ],
      avoidWhen: [
        'Entering card details: use the payment provider’s hosted fields or checkout; Monarch never collects card numbers.',
        'Bank accounts or UPI handles with different facts: compose Card and DescriptionList.',
      ],
    },
    designPrinciples: {
      items: [
        'Only what the provider returns: network, last four and expiry. Never the full number or CVC.',
        'Removing a card is consequential: it always goes through a danger ConfirmDialog.',
        'Status is written ("Default", "Expires soon", "Expired"), not shown by colour alone.',
        'Every action names the card it acts on, so a list of cards reads clearly to screen readers.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'The ref and native attributes go to the root `<article>`.',
        '`cardExpiryStatus(month, year, now?, soonMonths?)` works out `valid`, `expiring` or `expired`.',
        '`cardBrandNames` maps network ids to display names; any other `brand` is shown as given.',
        '`defaultPaymentMethodCardMessages` holds the English defaults.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'From a Stripe or Razorpay card object: `card.brand` → `brand`, `card.last4` → `last4`, `card.exp_month` / `card.exp_year` → `expiryMonth` / `expiryYear`, `billing_details.name` → `holderName`, and "is the customer’s default" → `isDefault`.',
        'Migrating from shadcn/ui: the Rook ID billing page builds this from `Card` + `CardContent` + a lucide `CreditCard` icon. `CardContent` padding is built in; the icon becomes the default glyph or `brandIcon`; text rows become `brand`, `last4` and expiry props; buttons become `onMakeDefault` / `onRemove`.',
        'Pass `expiryStatus` when the provider already flags expiry, or `now` to keep server and client renders in step near a month boundary.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'isDefault',
          values: ['false', 'true'],
          description: 'Accent border and a "Default" badge; hides Make default.',
        },
        {
          prop: 'headingLevel',
          values: ['2', '3', '4'],
          description: 'Heading level of the card name, to fit the page outline.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Default',
          attribute: 'data-default',
          description: 'The method charged by default.',
        },
        {
          name: 'Expiry',
          attribute: 'data-expiry',
          description:
            '`valid`, `expiring` (this month or next, with a badge) or `expired` (badge, red expiry line, Make default disabled).',
        },
        {
          name: 'Making default',
          attribute: 'aria-busy',
          description:
            'While a returned promise from `onMakeDefault` runs, the button shows a spinner.',
        },
        {
          name: 'Removing',
          description:
            'While `onRemove`’s promise runs, the confirmation stays open and pending; a rejection is shown inside it.',
        },
      ],
    },
    interaction: {
      items: [
        'Make default runs `onMakeDefault` straight away; it is not destructive.',
        'Remove opens a danger confirmation with focus on Cancel; confirming runs `onRemove`.',
        'Cancel or Escape closes the confirmation and returns focus to Remove.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Moves between Make default, Remove and any extra actions.' },
        { keys: 'Enter / Space', action: 'Activates the focused action.' },
        { keys: 'Escape', action: 'Closes the remove confirmation without removing.' },
      ],
    },
    accessibility: {
      role: 'article',
      name: 'The card name heading, e.g. "Visa ending in 4242".',
      items: [
        'Action buttons are named with the card ("Remove, Visa ending in 4242"), starting with their visible label.',
        'The confirmation is an alertdialog titled with the card name.',
        'The card glyph or brand logo is decorative; the name carries the network.',
      ],
    },
    responsive: {
      items: [
        'Glyph and details stay on one line; actions wrap under them below 480px.',
        'Action buttons are 44px tall on coarse pointers.',
      ],
    },
    theme: {
      items: [
        'Surface, border, accent and danger tokens; works in light, dark and every product accent.',
        'Forced-colours mode draws borders in CanvasText and the default card in Highlight.',
      ],
    },
    examples: {
      items: [
        {
          title: 'A list of saved cards',
          render: () => (
            <div style={{ display: 'grid', gap: 12, maxWidth: 560 }}>
              <PaymentMethodCard
                brand={visa.brand}
                last4={visa.last4}
                expiryMonth={visa.expiryMonth}
                expiryYear={visa.expiryYear}
                holderName={visa.holderName}
                isDefault={visa.isDefault}
                now={today}
                onRemove={noop}
                removeDisabled={visa.removeDisabled}
              />
              <PaymentMethodCard
                brand={mastercard.brand}
                last4={mastercard.last4}
                expiryMonth={mastercard.expiryMonth}
                expiryYear={mastercard.expiryYear}
                now={today}
                onMakeDefault={noop}
                onRemove={noop}
              />
              <PaymentMethodCard
                brand={rupay.brand}
                last4={rupay.last4}
                expiryMonth={rupay.expiryMonth}
                expiryYear={rupay.expiryYear}
                now={today}
                onMakeDefault={noop}
                onRemove={noop}
              />
            </div>
          ),
        },
        {
          title: 'Read-only (no actions)',
          render: () => (
            <div style={{ maxWidth: 420 }}>
              <PaymentMethodCard brand="amex" last4="1000" expiryStatus="valid" />
            </div>
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Disable Remove (with a note nearby) on the only card of an active subscription.',
        'Return a promise from `onRemove` so errors from the provider appear in the confirmation.',
        'Keep the card list in the provider’s order, default first.',
      ],
      dont: [
        'Do not render full card numbers, CVC or anything beyond last four and expiry.',
        'Do not remove a card without the confirmation (or call `window.confirm`).',
        'Do not use the accent border alone to mean "default"; the badge is required.',
      ],
    },
    related: {
      items: [
        { id: 'confirm-dialog', reason: 'The confirmation used by Remove.' },
        { id: 'subscriptions-table', reason: 'Subscriptions charged to these cards.' },
        { id: 'invoice-view', reason: 'Invoices paid with these cards.' },
        { id: 'badge', reason: 'Default and expiry badges.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['button', 'badge', 'confirm-dialog'],
    },
    testing: {
      unit: [
        'Article named by brand and last four; holder and expiry shown; axe clean.',
        '`cardExpiryStatus` for valid, expiring and expired cards; expired badge and disabled Make default.',
        'Default badge and no Make default on the default card.',
        'Make default reachable by Tab and Enter; busy while its promise runs.',
        'Remove asks first: focus on Cancel, Escape cancels and returns focus, Remove card confirms.',
        'Remove can be disabled; messages translate names; ref forwarded.',
        'Server rendering.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
