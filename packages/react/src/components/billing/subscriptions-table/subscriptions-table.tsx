import { forwardRef, type HTMLAttributes, type ReactNode } from 'react';
import { cx } from '../../../internal/cx.js';
import { Money, type MoneyAmount } from '../money/index.js';
import {
  StatusBadge,
  defineStatuses,
  type StatusRegistry,
} from '../../data-display/status-badge/index.js';
import {
  Table,
  TableBody,
  TableCaption,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '../../data-grid/data-table/index.js';
import { EmptyState } from '../../feedback/empty-state/index.js';

/** A billing period unit. `null` means a one-time charge. */
export type BillingInterval = 'day' | 'week' | 'month' | 'year';

/** A date as a `Date`, epoch milliseconds or an ISO 8601 string. */
export type BillingDate = Date | number | string;

/** One recurring agreement shown as a row. */
export interface SubscriptionRow {
  /** Stable id, used as the React key. */
  id: string;
  /** The subscription's name, shown as the row's primary text. */
  name: ReactNode;
  /** Plain-text name for accessible labels when `name` is not a string. */
  textName?: string;
  /** Opens the subscription. Rendered as a link (see `renderLink`). */
  href?: string;
  /** The plan or product it is for, e.g. "Standard plan". Shown under the name. */
  plan?: ReactNode;
  /** Number of seats or units. Shown as "3 ×" before the plan when above 1. */
  quantity?: number;
  /** The customer it belongs to. Shown in its own column when `showCustomer` is true. */
  customer?: ReactNode;
  /** Status key, looked up in `statuses` (default `subscriptionStatuses`). */
  status: string;
  /** The price per billing period, in its own currency. */
  amount: MoneyAmount | null;
  /** ISO 4217 currency of `amount`. */
  currency: string;
  /** Billing period unit. `null` or omitted: a one-time charge. */
  interval?: BillingInterval | null;
  /** Number of `interval`s per billing period, e.g. 3 months. Default 1. */
  intervalCount?: number;
  /**
   * The price normalised to one month in the reporting currency (MRR). `null`
   * on a live subscription means it could not be converted and is shown as
   * "Not converted"; omit it to show a dash.
   */
  monthly?: { amount: MoneyAmount; currency: string } | null;
  /** Next renewal or end date. Omitted or `null`: no end ("Evergreen"). */
  renewalDate?: BillingDate | null;
  /** True: renews on `renewalDate`. False: ends on it. Default true. */
  autoRenew?: boolean;
  /** A scheduled cancellation date, shown under the name. */
  cancelAt?: BillingDate | null;
  /** A scheduled plan or price change date, shown under the name. */
  pendingChangeAt?: BillingDate | null;
}

export interface SubscriptionsTableMessages {
  /** Column headings. */
  subscription: string;
  customer: string;
  status: string;
  price: string;
  monthly: string;
  renewal: string;
  /** Shown in place of a missing plan. Default "Custom price". */
  customPrice: string;
  /** "3 ×" before the plan. */
  quantity: (quantity: number) => string;
  /** The billing period after the price: "month", "3 months", "one time". */
  interval: (interval: BillingInterval | null, count: number) => string;
  /** Under the name. */
  cancels: (date: string) => string;
  changes: (date: string) => string;
  renews: (date: string) => string;
  ends: (date: string) => string;
  /** No renewal date. Default "Evergreen". */
  evergreen: string;
  /** A live subscription with no monthly figure. Default "Not converted". */
  notConverted: string;
  /** Explains `notConverted` in its hover title. */
  notConvertedHint: string;
  /** Empty table heading and description. */
  emptyTitle: string;
  emptyDescription: string;
}

const UNITS: Record<BillingInterval, [string, string]> = {
  day: ['day', 'days'],
  week: ['week', 'weeks'],
  month: ['month', 'months'],
  year: ['year', 'years'],
};

/** "month", "quarter", "3 weeks", "one time". */
export function formatBillingInterval(interval: BillingInterval | null, count = 1): string {
  if (!interval) return 'one time';
  if (interval === 'month' && count === 3) return 'quarter';
  return count === 1 ? UNITS[interval][0] : `${count} ${UNITS[interval][1]}`;
}

const DEFAULT_MESSAGES: SubscriptionsTableMessages = {
  subscription: 'Subscription',
  customer: 'Customer',
  status: 'Status',
  price: 'Price',
  monthly: 'Monthly',
  renewal: 'Renewal',
  customPrice: 'Custom price',
  quantity: (q) => `${q} ×`,
  interval: formatBillingInterval,
  cancels: (d) => `cancels ${d}`,
  changes: (d) => `changes ${d}`,
  renews: (d) => `Renews ${d}`,
  ends: (d) => `Ends ${d}`,
  evergreen: 'Evergreen',
  notConverted: 'Not converted',
  notConvertedHint: 'No exchange rate is available, so this is left out of the monthly total.',
  emptyTitle: 'No subscriptions',
  emptyDescription: 'Subscriptions appear here once they are created.',
};

/** The subscription statuses a recurring-billing product usually needs. Extend with `defineStatuses`. */
export const subscriptionStatuses = defineStatuses({
  draft: { label: 'Draft', tone: 'neutral' },
  trial: { label: 'Trial', tone: 'info' },
  active: { label: 'Active', tone: 'success' },
  past_due: { label: 'Past due', tone: 'warning' },
  paused: { label: 'Paused', tone: 'neutral' },
  cancelled: { label: 'Cancelled', tone: 'danger' },
  expired: { label: 'Expired', tone: 'neutral' },
});

/** Statuses for which a missing monthly figure means "not converted" rather than "not counted". */
const LIVE = new Set(['active', 'past_due']);

export interface RenderLinkProps {
  href: string;
  className: string;
  children: ReactNode;
}

export interface SubscriptionsTableProps extends Omit<HTMLAttributes<HTMLDivElement>, 'children'> {
  /** The rows, in display order. Sorting and filtering stay in the product. */
  subscriptions: readonly SubscriptionRow[];
  /** Show the customer column. Turn off on a single customer's page. Default true. */
  showCustomer?: boolean;
  /** Show the normalised monthly (MRR) column. Default: when any row has `monthly`. */
  showMonthly?: boolean;
  /** Amounts are in minor units (cents). Default false. */
  minorUnits?: boolean;
  /** BCP 47 locale for amounts and dates. Default: the runtime locale. */
  locale?: string;
  /** IANA time zone for dates. Pass the same value on server and client. */
  timeZone?: string;
  /** Status registry. Default `subscriptionStatuses`. */
  statuses?: StatusRegistry;
  /** Table caption, its accessible name. Default "Subscriptions". */
  caption?: ReactNode;
  /** Hide the caption visually (it still names the table). Default true. */
  hideCaption?: boolean;
  /** Renders links, e.g. with a router's Link. Default a plain `<a>`. */
  renderLink?: ((props: RenderLinkProps) => ReactNode) | undefined;
  /** Replaces the default empty state. */
  empty?: ReactNode;
  /** Visible text overrides; English defaults. */
  messages?: Partial<SubscriptionsTableMessages>;
}

function toDate(value: BillingDate): Date {
  return value instanceof Date ? value : new Date(value);
}

const defaultLink = ({ href, className, children }: RenderLinkProps) => (
  <a href={href} className={className}>
    {children}
  </a>
);

/**
 * A table of recurring agreements: name and plan, customer, status, price
 * per billing period, normalised monthly amount and the next renewal. Each
 * amount stays in its own currency; nothing is converted or summed here.
 */
export const SubscriptionsTable = forwardRef<HTMLDivElement, SubscriptionsTableProps>(
  function SubscriptionsTable(
    {
      subscriptions,
      showCustomer = true,
      showMonthly,
      minorUnits = false,
      locale,
      timeZone,
      statuses = subscriptionStatuses,
      caption = 'Subscriptions',
      hideCaption = true,
      renderLink = defaultLink,
      empty,
      messages,
      className,
      ...rest
    },
    ref,
  ) {
    const m = { ...DEFAULT_MESSAGES, ...messages };
    const dateFormat = new Intl.DateTimeFormat(locale, {
      dateStyle: 'medium',
      ...(timeZone !== undefined && { timeZone }),
    });
    const date = (v: BillingDate) => dateFormat.format(toDate(v));
    const monthlyColumn = showMonthly ?? subscriptions.some((s) => s.monthly !== undefined);

    if (subscriptions.length === 0) {
      return (
        <div ref={ref} {...rest} className={cx('mn-subscriptions-table', className)} data-empty="">
          {empty ?? (
            <EmptyState size="compact" title={m.emptyTitle} description={m.emptyDescription} />
          )}
        </div>
      );
    }

    return (
      <div ref={ref} {...rest} className={cx('mn-subscriptions-table', className)}>
        <Table className="mn-subscriptions-table__table">
          <TableCaption className={hideCaption ? 'mn-visually-hidden' : undefined}>
            {caption}
          </TableCaption>
          <TableHeader>
            <TableRow>
              <TableHead>{m.subscription}</TableHead>
              {showCustomer ? <TableHead data-priority="medium">{m.customer}</TableHead> : null}
              <TableHead>{m.status}</TableHead>
              <TableHead numeric>{m.price}</TableHead>
              {monthlyColumn ? (
                <TableHead numeric data-priority="low">
                  {m.monthly}
                </TableHead>
              ) : null}
              <TableHead data-priority="medium">{m.renewal}</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {subscriptions.map((s) => {
              const quantity = s.quantity ?? 1;
              const notes = [
                s.cancelAt ? m.cancels(date(s.cancelAt)) : null,
                s.pendingChangeAt ? m.changes(date(s.pendingChangeAt)) : null,
              ].filter(Boolean);
              const renewal = s.renewalDate
                ? (s.autoRenew ?? true)
                  ? m.renews(date(s.renewalDate))
                  : m.ends(date(s.renewalDate))
                : m.evergreen;
              return (
                <TableRow key={s.id} data-status={s.status}>
                  <TableCell className="mn-subscriptions-table__main">
                    {s.href ? (
                      renderLink({
                        href: s.href,
                        className: 'mn-subscriptions-table__name',
                        children: s.name,
                      })
                    ) : (
                      <span className="mn-subscriptions-table__name">{s.name}</span>
                    )}
                    <span className="mn-subscriptions-table__meta">
                      {quantity > 1 ? `${m.quantity(quantity)} ` : null}
                      {s.plan ?? m.customPrice}
                      {notes.map((n) => ` · ${n}`)}
                    </span>
                    {/* Shown only when the customer and renewal columns are hidden (narrow containers). */}
                    <span className="mn-subscriptions-table__narrow">
                      {showCustomer && s.customer ? (
                        <span className="mn-subscriptions-table__narrow-customer">
                          {s.customer}
                          {' · '}
                        </span>
                      ) : null}
                      {renewal}
                    </span>
                  </TableCell>
                  {showCustomer ? (
                    <TableCell data-priority="medium" className="mn-subscriptions-table__customer">
                      {s.customer ?? <span aria-hidden>—</span>}
                    </TableCell>
                  ) : null}
                  <TableCell>
                    <StatusBadge status={s.status} statuses={statuses} size="sm" />
                  </TableCell>
                  <TableCell numeric className="mn-subscriptions-table__price">
                    <Money
                      amount={s.amount}
                      currency={s.currency}
                      minorUnits={minorUnits}
                      {...(locale !== undefined && { locale })}
                    />
                    <span className="mn-subscriptions-table__interval">
                      {' / '}
                      {m.interval(s.interval ?? null, s.intervalCount ?? 1)}
                    </span>
                  </TableCell>
                  {monthlyColumn ? (
                    <TableCell numeric data-priority="low">
                      {s.monthly ? (
                        <Money
                          amount={s.monthly.amount}
                          currency={s.monthly.currency}
                          minorUnits={minorUnits}
                          {...(locale !== undefined && { locale })}
                        />
                      ) : s.monthly === null && LIVE.has(s.status) ? (
                        <span
                          className="mn-subscriptions-table__unconverted"
                          title={m.notConvertedHint}
                        >
                          {m.notConverted}
                        </span>
                      ) : (
                        <Money amount={null} currency={s.currency} />
                      )}
                    </TableCell>
                  ) : null}
                  <TableCell data-priority="medium" className="mn-subscriptions-table__renewal">
                    {renewal}
                  </TableCell>
                </TableRow>
              );
            })}
          </TableBody>
        </Table>
      </div>
    );
  },
);
