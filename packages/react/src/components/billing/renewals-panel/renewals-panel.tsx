'use client';

import { forwardRef, useId, type HTMLAttributes, type ReactNode } from 'react';
import { cx } from '../../../internal/cx.js';
import { WarningGlyph } from '../../../internal/icons.js';
import { Money, formatMoney, type MoneyAmount } from '../money/index.js';
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
import { Button } from '../../actions/button/index.js';
import { SegmentedControl } from '../../forms/segmented-control/index.js';
import { EmptyState } from '../../feedback/empty-state/index.js';
import { InlineErrorRetry } from '../../feedback/inline-error-retry/index.js';
import { Skeleton } from '../../feedback/skeleton/index.js';

/** A date as a `Date`, epoch milliseconds or an ISO 8601 string. */
export type RenewalDate = Date | number | string;

/** An amount in one currency. */
export interface RenewalMoney {
  amount: MoneyAmount;
  currency: string;
}

/** One upcoming (or past) renewal: a subscription, contract, licence or domain reaching its term end. */
export interface RenewalItem {
  /** Stable id, used as the React key. */
  id: string;
  /** What renews, e.g. the agreement or asset name. */
  name: ReactNode;
  /** Plain-text name for the action button's accessible name when `name` is not a string. */
  textName?: string;
  /** Opens the item. Rendered as a link (see `renderLink`). */
  href?: string;
  /** Secondary line under the name, e.g. type and customer. */
  description?: ReactNode;
  /** Renewal or expiry date. `null`: unknown. */
  renewalDate: RenewalDate | null;
  /**
   * Whole days until `renewalDate` (negative when past). Give it when the
   * server computes it; otherwise it is derived from `now`.
   */
  daysUntil?: number | null;
  /** The last day to give notice, shown under the date instead of the day count. */
  noticeDeadline?: RenewalDate | null;
  /** What the renewal costs or is worth. `null`: unpriced. */
  amount?: RenewalMoney | null;
  /** A short note under the amount, e.g. "per year" or "estimated". */
  amountNote?: ReactNode;
  /** Renews by itself. `undefined` hides the auto-renew note. */
  autoRenew?: boolean | null;
  /** Status key, looked up in `statuses` (default `renewalStatuses`). */
  status: string;
  /** Likelihood of renewing, 0–100. */
  probability?: number | null;
  /** Who owns the renewal. */
  owner?: ReactNode;
  /** The next step, e.g. "Send a quote". */
  nextAction?: ReactNode;
  /** When the next step is due. */
  nextActionDate?: RenewalDate | null;
  /** The next step is overdue: shown in the danger colour with an "Overdue" prefix. */
  nextActionOverdue?: boolean;
  /** How a closed renewal ended, e.g. "Renewed for two years". */
  outcome?: ReactNode;
}

/** A per-currency total for the items in view. Amounts are never converted or added across currencies. */
export interface RenewalTotal {
  currency: string;
  /** Sum of priced items in this currency. */
  amount: MoneyAmount;
  /** Number of items in the total. */
  count: number;
  /** Part of the total that is estimated rather than firm. */
  estimated?: MoneyAmount | null;
}

/** A filter option, e.g. a time window or a status group. */
export interface RenewalFilter {
  value: string;
  label: string;
  /** Optional count shown in the option. */
  count?: number;
}

export interface RenewalsPanelMessages {
  /** Column headings. */
  item: string;
  date: string;
  amount: string;
  owner: string;
  status: string;
  /** Accessible name of the hidden actions column. */
  actions: string;
  /** Accessible name of the filter group. */
  filters: string;
  /** Accessible name of the totals list. */
  totals: string;
  /** "in 12 days", "today", "3 days ago". */
  daysUntil: (days: number) => string;
  /** "Notice by 1 Mar 2027". */
  noticeBy: (date: string) => string;
  unknownDate: string;
  unpriced: string;
  autoRenews: string;
  manualRenewal: string;
  noOwner: string;
  noNextAction: string;
  overdue: string;
  /** Read before the date of an urgent open renewal. Default "Due soon". */
  dueSoon: string;
  /** "60% likely". */
  probability: (percent: number) => string;
  /** Per-currency total heading: "USD · 4 renewals". */
  totalIn: (currency: string, count: number) => string;
  /** "Includes $120.00 estimated". */
  estimated: (amount: string) => string;
  /** Unpriced card heading and hint. */
  unpricedCount: string;
  unpricedHint: string;
  /** Default row action label. */
  action: string;
  /** Accessible name of a row action: "Update Example agreement". */
  actionFor: (action: string, name: string) => string;
  emptyTitle: string;
  emptyDescription: string;
  error: string;
  loading: string;
}

const DEFAULT_MESSAGES: RenewalsPanelMessages = {
  item: 'Item',
  date: 'Renewal',
  amount: 'Amount',
  owner: 'Owner · next action',
  status: 'Status',
  actions: 'Actions',
  filters: 'Filter renewals',
  totals: 'Totals by currency',
  daysUntil: (d) =>
    d === 0
      ? 'today'
      : d > 0
        ? `in ${d} ${d === 1 ? 'day' : 'days'}`
        : `${-d} ${d === -1 ? 'day' : 'days'} ago`,
  noticeBy: (d) => `Notice by ${d}`,
  unknownDate: 'Unknown date',
  unpriced: 'No price',
  autoRenews: 'Auto-renews',
  manualRenewal: 'Manual renewal',
  noOwner: 'No owner',
  noNextAction: 'No next action',
  overdue: 'Overdue',
  dueSoon: 'Due soon',
  probability: (p) => `${p}% likely`,
  totalIn: (c, n) => `${c} · ${n} ${n === 1 ? 'renewal' : 'renewals'}`,
  estimated: (a) => `Includes ${a} estimated`,
  unpricedCount: 'Without a price',
  unpricedHint: 'Not included in the totals.',
  action: 'Update',
  actionFor: (a, n) => `${a} ${n}`,
  emptyTitle: 'No renewals coming up',
  emptyDescription: 'Items appear here when their renewal date comes into the selected window.',
  error: 'Renewals could not be loaded.',
  loading: 'Loading renewals',
};

/** The renewal statuses a renewals workflow usually needs. Extend with `defineStatuses`. */
export const renewalStatuses = defineStatuses({
  open: { label: 'Open', tone: 'info' },
  in_progress: { label: 'In progress', tone: 'warning' },
  renewed: { label: 'Renewed', tone: 'success' },
  lost: { label: 'Lost', tone: 'danger' },
  void: { label: 'No longer due', tone: 'neutral' },
});

const OPEN = new Set(['open', 'in_progress']);
const DAY = 86_400_000;

export interface RenderRenewalLinkProps {
  href: string;
  className: string;
  children: ReactNode;
}

export interface RenewalsPanelProps extends Omit<
  HTMLAttributes<HTMLElement>,
  'children' | 'title'
> {
  /** The renewals in view, already filtered and sorted by the product. */
  items: readonly RenewalItem[];
  /** Visible heading. Omit when the page heading already names the panel. */
  title?: ReactNode;
  /** Heading level for `title`. Default `h2`. */
  headingLevel?: 'h2' | 'h3' | 'h4';
  /** Help text under the heading, e.g. when items appear and close. */
  description?: ReactNode;
  /** Toolbar content at the end, e.g. a "Check now" button or a calendar feed link. */
  actions?: ReactNode;
  /** Filter options (time windows or status groups). The product filters the items. */
  filters?: readonly RenewalFilter[];
  /** The selected filter value. */
  filter?: string;
  /** Called with the new filter value. */
  onFilterChange?: ((value: string) => void) | undefined;
  /** Extra filter controls after the filter group, e.g. a Select for a customer. */
  filterControls?: ReactNode;
  /** Per-currency totals, shown as a row of figures above the table. */
  totals?: readonly RenewalTotal[];
  /** Count of items without a price, shown next to the totals. */
  unpricedCount?: number;
  /** Amounts are in minor units. Default false. */
  minorUnits?: boolean;
  /** BCP 47 locale for amounts, dates and day counts. Default: the runtime locale. */
  locale?: string;
  /** IANA time zone for dates. Pass the same value on server and client. */
  timeZone?: string;
  /** Reference time for day counts. Pass it from the server for identical server and client text. */
  now?: RenewalDate;
  /** Open renewals this many days away or fewer are marked urgent. Default 14. */
  urgentWithinDays?: number;
  /** Status registry. Default `renewalStatuses`. */
  statuses?: StatusRegistry;
  /** Row action, e.g. open an update dialog or mark as renewed. Omit for a read-only panel. */
  onItemAction?: ((item: RenewalItem) => void) | undefined;
  /** Label of the row action. Default "Update". */
  actionLabel?: string;
  /** Hide the row action for some items, e.g. closed ones. Default: shown for every item. */
  canAct?: ((item: RenewalItem) => boolean) | undefined;
  /** Shows skeleton rows and sets `aria-busy`. */
  loading?: boolean;
  /** Shows an inline error with a retry button instead of the table. */
  error?: ReactNode | boolean;
  /** Retry after an error. */
  onRetry?: (() => unknown) | undefined;
  /** Replaces the default empty state. */
  empty?: ReactNode;
  /** Renders links, e.g. with a router's Link. Default a plain `<a>`. */
  renderLink?: ((props: RenderRenewalLinkProps) => ReactNode) | undefined;
  /** Visible text overrides; English defaults. */
  messages?: Partial<RenewalsPanelMessages>;
}

function toDate(value: RenewalDate): Date {
  return value instanceof Date ? value : new Date(value);
}

function daysBetween(target: RenewalDate, now: RenewalDate): number {
  const a = toDate(target).getTime();
  const b = toDate(now).getTime();
  return Math.round((a - b) / DAY);
}

const defaultLink = ({ href, className, children }: RenderRenewalLinkProps) => (
  <a href={href} className={className}>
    {children}
  </a>
);

/**
 * A renewals workspace: what renews or expires next, when, for how much and
 * who owns it, with filters, per-currency totals and a row action. Data and
 * callbacks come in; fetching and filtering stay in the product.
 */
export const RenewalsPanel = forwardRef<HTMLElement, RenewalsPanelProps>(function RenewalsPanel(
  {
    items,
    title,
    headingLevel: Heading = 'h2',
    description,
    actions,
    filters,
    filter,
    onFilterChange,
    filterControls,
    totals,
    unpricedCount,
    minorUnits = false,
    locale,
    timeZone,
    now,
    urgentWithinDays = 14,
    statuses = renewalStatuses,
    onItemAction,
    actionLabel,
    canAct,
    loading = false,
    error,
    onRetry,
    empty,
    renderLink = defaultLink,
    messages,
    className,
    'aria-label': ariaLabel,
    'aria-labelledby': ariaLabelledBy,
    ...rest
  },
  ref,
) {
  const m = { ...DEFAULT_MESSAGES, ...messages };
  const headingId = useId();
  const dateFormat = new Intl.DateTimeFormat(locale, {
    dateStyle: 'medium',
    ...(timeZone !== undefined && { timeZone }),
  });
  const fmt = (v: RenewalDate) => dateFormat.format(toDate(v));
  const money = (amount: MoneyAmount, currency: string) => (
    <Money
      amount={amount}
      currency={currency}
      minorUnits={minorUnits}
      {...(locale !== undefined && { locale })}
    />
  );
  const showOwner = items.some(
    (i) => i.owner !== undefined || i.nextAction !== undefined || i.nextActionDate,
  );
  const label = actionLabel ?? m.action;
  const hasError = error !== undefined && error !== false && error !== null;
  const showTotals = !loading && !hasError && ((totals?.length ?? 0) > 0 || !!unpricedCount);

  let body: ReactNode;
  if (hasError) {
    const errorText = error === true ? m.error : error;
    body = onRetry ? (
      <InlineErrorRetry onRetry={onRetry}>{errorText}</InlineErrorRetry>
    ) : (
      <p className="mn-renewals-panel__error" role="alert">
        {errorText}
      </p>
    );
  } else if (loading) {
    body = (
      <div className="mn-renewals-panel__loading" role="status" aria-label={m.loading}>
        {[0, 1, 2, 3].map((i) => (
          <Skeleton key={i} shape="rect" height="2.5rem" />
        ))}
      </div>
    );
  } else if (items.length === 0) {
    body = empty ?? (
      <EmptyState size="compact" title={m.emptyTitle} description={m.emptyDescription} />
    );
  } else {
    body = (
      <Table className="mn-renewals-panel__table">
        <TableCaption className="mn-visually-hidden">
          {typeof title === 'string' ? title : (ariaLabel ?? m.item)}
        </TableCaption>
        <TableHeader>
          <TableRow>
            <TableHead>{m.item}</TableHead>
            <TableHead>{m.date}</TableHead>
            <TableHead numeric data-priority="medium">
              {m.amount}
            </TableHead>
            {showOwner ? <TableHead data-priority="low">{m.owner}</TableHead> : null}
            <TableHead>{m.status}</TableHead>
            {onItemAction ? (
              <TableHead>
                <span className="mn-visually-hidden">{m.actions}</span>
              </TableHead>
            ) : null}
          </TableRow>
        </TableHeader>
        <TableBody>
          {items.map((item) => {
            const days =
              item.daysUntil ??
              (item.renewalDate ? daysBetween(item.renewalDate, now ?? Date.now()) : null);
            const open = OPEN.has(item.status);
            const urgent = open && days !== null && days <= urgentWithinDays;
            const name =
              item.textName ?? (typeof item.name === 'string' ? item.name : String(item.id));
            const acts = onItemAction && (canAct ? canAct(item) : true);
            return (
              <TableRow
                key={item.id}
                data-status={item.status}
                data-urgent={urgent || undefined}
                data-overdue={(open && days !== null && days < 0) || undefined}
              >
                <TableCell className="mn-renewals-panel__main">
                  {item.href ? (
                    renderLink({
                      href: item.href,
                      className: 'mn-renewals-panel__name',
                      children: item.name,
                    })
                  ) : (
                    <span className="mn-renewals-panel__name">{item.name}</span>
                  )}
                  {item.description ? (
                    <span className="mn-renewals-panel__meta">{item.description}</span>
                  ) : null}
                  {item.autoRenew !== undefined && item.autoRenew !== null ? (
                    <span className="mn-renewals-panel__meta">
                      {item.autoRenew ? m.autoRenews : m.manualRenewal}
                    </span>
                  ) : null}
                  {item.amount ? (
                    <span className="mn-renewals-panel__narrow">
                      {money(item.amount.amount, item.amount.currency)}
                    </span>
                  ) : null}
                </TableCell>
                <TableCell className="mn-renewals-panel__date">
                  {item.renewalDate ? (
                    <time
                      className="mn-renewals-panel__when"
                      dateTime={toDate(item.renewalDate).toISOString()}
                      data-urgent={urgent || undefined}
                    >
                      {urgent ? (
                        <>
                          <WarningGlyph className="mn-renewals-panel__urgent-icon" aria-hidden />
                          <span className="mn-visually-hidden">{m.dueSoon}: </span>
                        </>
                      ) : null}
                      {fmt(item.renewalDate)}
                    </time>
                  ) : (
                    <span className="mn-renewals-panel__when">{m.unknownDate}</span>
                  )}
                  <span className="mn-renewals-panel__meta">
                    {item.noticeDeadline
                      ? m.noticeBy(fmt(item.noticeDeadline))
                      : days !== null
                        ? m.daysUntil(days)
                        : null}
                  </span>
                </TableCell>
                <TableCell numeric data-priority="medium">
                  {item.amount ? (
                    money(item.amount.amount, item.amount.currency)
                  ) : (
                    <span className="mn-renewals-panel__muted">{m.unpriced}</span>
                  )}
                  {item.amountNote ? (
                    <span className="mn-renewals-panel__meta">{item.amountNote}</span>
                  ) : null}
                </TableCell>
                {showOwner ? (
                  <TableCell data-priority="low" className="mn-renewals-panel__owner">
                    <span className="mn-renewals-panel__line">
                      {item.owner ?? <span className="mn-renewals-panel__muted">{m.noOwner}</span>}
                    </span>
                    <span
                      className="mn-renewals-panel__meta"
                      data-overdue={item.nextActionOverdue || undefined}
                    >
                      {item.nextActionOverdue ? `${m.overdue}: ` : null}
                      {item.nextAction ?? m.noNextAction}
                      {item.nextAction && item.nextActionDate
                        ? ` · ${fmt(item.nextActionDate)}`
                        : null}
                    </span>
                  </TableCell>
                ) : null}
                <TableCell>
                  <span className="mn-renewals-panel__status">
                    <StatusBadge status={item.status} statuses={statuses} size="sm" />
                    {item.probability !== null && item.probability !== undefined ? (
                      <span className="mn-renewals-panel__meta">
                        {m.probability(item.probability)}
                      </span>
                    ) : null}
                  </span>
                  {item.outcome ? (
                    <span className="mn-renewals-panel__meta">{item.outcome}</span>
                  ) : null}
                </TableCell>
                {onItemAction ? (
                  <TableCell className="mn-renewals-panel__action">
                    {acts ? (
                      <Button
                        size="sm"
                        variant="secondary"
                        aria-label={m.actionFor(label, name)}
                        onClick={() => onItemAction(item)}
                      >
                        {label}
                      </Button>
                    ) : null}
                  </TableCell>
                ) : null}
              </TableRow>
            );
          })}
        </TableBody>
      </Table>
    );
  }

  return (
    <section
      ref={ref}
      {...rest}
      aria-label={ariaLabel}
      aria-labelledby={ariaLabelledBy ?? (title ? headingId : undefined)}
      aria-busy={loading || undefined}
      data-loading={loading || undefined}
      data-error={hasError || undefined}
      className={cx('mn-renewals-panel', className)}
    >
      {title || description || actions ? (
        <div className="mn-renewals-panel__header">
          <div className="mn-renewals-panel__heading">
            {title ? (
              <Heading id={headingId} className="mn-renewals-panel__title">
                {title}
              </Heading>
            ) : null}
            {description ? <p className="mn-renewals-panel__description">{description}</p> : null}
          </div>
          {actions ? <div className="mn-renewals-panel__actions">{actions}</div> : null}
        </div>
      ) : null}
      {(filters && filters.length > 0) || filterControls ? (
        <div className="mn-renewals-panel__filters">
          {filters && filters.length > 0 ? (
            <SegmentedControl
              aria-label={m.filters}
              size="sm"
              options={filters.map((f) => ({
                value: f.value,
                label: f.label,
                ...(f.count !== undefined && { count: f.count }),
              }))}
              value={filter ?? filters[0]?.value ?? ''}
              onValueChange={(v) => onFilterChange?.(v)}
            />
          ) : null}
          {filterControls}
        </div>
      ) : null}
      {showTotals ? (
        <ul className="mn-renewals-panel__totals" aria-label={m.totals}>
          {totals?.map((t) => (
            <li key={t.currency} className="mn-renewals-panel__total">
              <span className="mn-renewals-panel__total-label">
                {m.totalIn(t.currency, t.count)}
              </span>
              <span className="mn-renewals-panel__total-value">{money(t.amount, t.currency)}</span>
              {t.estimated !== undefined && t.estimated !== null ? (
                <span className="mn-renewals-panel__meta">
                  {m.estimated(
                    formatMoney(t.estimated, t.currency, {
                      minorUnits,
                      ...(locale !== undefined && { locale }),
                    }),
                  )}
                </span>
              ) : null}
            </li>
          ))}
          {unpricedCount ? (
            <li className="mn-renewals-panel__total">
              <span className="mn-renewals-panel__total-label">{m.unpricedCount}</span>
              <span className="mn-renewals-panel__total-value">{unpricedCount}</span>
              <span className="mn-renewals-panel__meta">{m.unpricedHint}</span>
            </li>
          ) : null}
        </ul>
      ) : null}
      <div className="mn-renewals-panel__body">{body}</div>
    </section>
  );
});
