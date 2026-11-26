import { forwardRef, useId, type HTMLAttributes, type ReactNode } from 'react';
import { cx } from '../../../internal/cx.js';
import { Button } from '../../actions/button/index.js';
import { computeLineItemTotals, type LineItemsDiscount } from '../line-items-editor/index.js';
import { currencyDigits, Money, type MoneyAmount } from '../money/index.js';

/** One side of the invoice: who bills (`from`) or who is billed (`to`). */
export interface InvoiceParty {
  /** Organisation or person name. */
  name: ReactNode;
  /** Address, tax registration, email and similar lines, one per entry. */
  details?: readonly ReactNode[] | undefined;
}

/** One billed line. */
export interface InvoiceLineItem {
  /** Stable key. Default: the position. */
  id?: string | undefined;
  /** What was billed, e.g. "Design review". */
  description: ReactNode;
  /** A second line under the description, such as a period or SKU. */
  detail?: ReactNode;
  /** Quantity as a number or decimal text. */
  quantity: string | number;
  /** Price of one unit, in the same units as every other amount. */
  unitPrice: string | number;
  /**
   * The billed line amount as calculated by the billing system. When left
   * out it is worked out as quantity × unit price, rounded to the currency.
   */
  amount?: MoneyAmount | undefined;
}

/**
 * Totals as calculated by the billing system. Any value left out is worked
 * out from the line items, `taxRate` and `discount`.
 */
export interface InvoiceTotals {
  subtotal?: MoneyAmount | undefined;
  /** Shown only when present and not zero. Pass it as a positive amount. */
  discount?: MoneyAmount | undefined;
  /** Shown only when present (or a `taxRate` is set). */
  tax?: MoneyAmount | undefined;
  total?: MoneyAmount | undefined;
  /** Payments already received. Shown only when present. */
  amountPaid?: MoneyAmount | undefined;
  /** What is still owed. Shown only when present. */
  amountDue?: MoneyAmount | undefined;
}

/** A label and value in the invoice facts grid (number, dates, terms, reference). */
export interface InvoiceDetail {
  label: ReactNode;
  value: ReactNode;
}

/** Visible and assistive-technology text. English defaults. */
export interface InvoiceViewMessages {
  /** Default title. */
  title: string;
  /** Heading over the issuer. */
  from: string;
  /** Heading over the billed party. */
  to: string;
  issueDate: string;
  dueDate: string;
  /** Caption of the line-item table. */
  lineItems: string;
  description: string;
  quantity: string;
  unitPrice: string;
  amount: string;
  subtotal: string;
  discount: string;
  /** Tax row label; receives the formatted rate ("18%") when `taxRate` is set. */
  tax: (rate: string) => string;
  total: string;
  amountPaid: string;
  amountDue: string;
  /** Shown when there are no line items. */
  noItems: string;
  /** Heading over `notes`. */
  notes: string;
  /** Label of the built-in print button. */
  print: string;
}

export const defaultInvoiceViewMessages: InvoiceViewMessages = {
  title: 'Invoice',
  from: 'From',
  to: 'Bill to',
  issueDate: 'Issue date',
  dueDate: 'Due date',
  lineItems: 'Line items',
  description: 'Description',
  quantity: 'Qty',
  unitPrice: 'Unit price',
  amount: 'Amount',
  subtotal: 'Subtotal',
  discount: 'Discount',
  tax: (rate) => (rate ? `Tax (${rate})` : 'Tax'),
  total: 'Total',
  amountPaid: 'Amount paid',
  amountDue: 'Amount due',
  noItems: 'No line items.',
  notes: 'Notes',
  print: 'Print',
};

export interface InvoiceViewProps extends Omit<HTMLAttributes<HTMLElement>, 'title'> {
  /** Main heading. Default "Invoice" (from `messages.title`). */
  title?: ReactNode;
  /** Invoice number, shown after the title (and so part of the invoice's accessible name). */
  number?: ReactNode;
  /** A status beside the title, such as a StatusBadge for "Paid" or "Overdue". */
  status?: ReactNode;
  /** Issue date, already formatted (or a RelativeTime / DateTime element). */
  issueDate?: ReactNode;
  /** Due date, already formatted. */
  dueDate?: ReactNode;
  /** Extra facts after number and dates: payment terms, PO number, project. */
  details?: readonly InvoiceDetail[] | undefined;
  /** A logo or wordmark shown in the header. Decorative unless you give it a name. */
  brand?: ReactNode;
  /** Who issues the invoice. */
  from?: InvoiceParty | undefined;
  /** Who is billed. */
  to?: InvoiceParty | undefined;
  /** Billed lines, read-only. */
  items: readonly InvoiceLineItem[];
  /** ISO 4217 code for every amount. */
  currency: string;
  /** BCP 47 locale for amounts and quantities. Default: the runtime locale. */
  locale?: string | undefined;
  /** Every amount you pass is in integer minor units (cents, paise). Default `false`. */
  minorUnits?: boolean | undefined;
  /** Tax rate in percent, used to work out tax when `totals.tax` is not given. */
  taxRate?: number | undefined;
  /** Discount used to work out totals when `totals.discount` is not given. */
  discount?: LineItemsDiscount | undefined;
  /** Totals from the billing system; they win over anything worked out here. */
  totals?: InvoiceTotals | undefined;
  /** Payment instructions, terms or a thank-you note, under the totals. */
  notes?: ReactNode;
  /** Small print at the bottom (registered office, tax notices). */
  footer?: ReactNode;
  /** Extra header actions, such as Download PDF or Send. Hidden when printing. */
  actions?: ReactNode;
  /**
   * Shows a Print button in the header. It calls `onPrint` when given,
   * otherwise the browser's print dialog.
   */
  showPrintButton?: boolean | undefined;
  /** Replaces the browser print dialog for the Print button (for example to print a PDF). */
  onPrint?: (() => void) | undefined;
  /** Heading level of the title; section headings use the next level. Default 2. */
  headingLevel?: 1 | 2 | 3 | undefined;
  /** Replace any English text, for example with the invoice language's labels. */
  messages?: Partial<InvoiceViewMessages> | undefined;
}

function isZero(amount: MoneyAmount): boolean {
  return Number(amount) === 0;
}

/**
 * A read-only, printable invoice: issuer and billed party, number and dates,
 * a status, line items, totals and notes. Amounts the billing system has
 * calculated are shown as given; anything missing is worked out from the
 * line items with the same rounding as LineItemsEditor. Print styles drop
 * the actions and colour fills so the page prints cleanly on paper or PDF.
 */
export const InvoiceView = forwardRef<HTMLElement, InvoiceViewProps>(function InvoiceView(
  {
    title,
    number,
    status,
    issueDate,
    dueDate,
    details,
    brand,
    from,
    to,
    items,
    currency,
    locale,
    minorUnits = false,
    taxRate,
    discount,
    totals,
    notes,
    footer,
    actions,
    showPrintButton = false,
    onPrint,
    headingLevel = 2,
    messages,
    className,
    ...rest
  },
  ref,
) {
  const m = { ...defaultInvoiceViewMessages, ...messages };
  const titleId = useId();
  const Title = `h${headingLevel}` as 'h2';
  const Sub = `h${headingLevel + 1}` as 'h3';

  // Worked-out figures are in minor units; given figures follow `minorUnits`.
  const scale = minorUnits ? 10 ** currencyDigits(currency) : 1;
  const computed = computeLineItemTotals(
    minorUnits
      ? items.map((item) => ({
          quantity: item.quantity,
          unitPrice: Number(item.unitPrice) / scale,
        }))
      : items,
    currency,
    { taxRate, discount },
  );
  const given = (amount: MoneyAmount, extra?: string) => (
    <Money
      amount={amount}
      currency={currency}
      minorUnits={minorUnits}
      {...(locale !== undefined && { locale })}
      {...(extra !== undefined && { className: extra })}
    />
  );
  const worked = (amount: number, extra?: string) => (
    <Money
      amount={amount}
      currency={currency}
      minorUnits
      {...(locale !== undefined && { locale })}
      {...(extra !== undefined && { className: extra })}
    />
  );
  const pick = (value: MoneyAmount | undefined, fallback: number, extra?: string) =>
    value !== undefined ? given(value, extra) : worked(fallback, extra);

  const quantityFormat = new Intl.NumberFormat(locale, { maximumFractionDigits: 4 });
  const formatQuantity = (q: string | number) => {
    const n = typeof q === 'number' ? q : Number(q);
    return Number.isFinite(n) ? quantityFormat.format(n) : q;
  };
  const rateText =
    taxRate !== undefined && totals?.tax === undefined
      ? new Intl.NumberFormat(locale, { maximumFractionDigits: 3 }).format(taxRate) + '%'
      : '';

  const discountShown =
    totals?.discount !== undefined ? !isZero(totals.discount) : computed.discount !== 0;
  const taxShown = totals?.tax !== undefined || taxRate !== undefined;

  const facts: InvoiceDetail[] = [
    ...(issueDate !== undefined ? [{ label: m.issueDate, value: issueDate }] : []),
    ...(dueDate !== undefined ? [{ label: m.dueDate, value: dueDate }] : []),
    ...(details ?? []),
  ];

  const handlePrint = () => {
    if (onPrint) onPrint();
    else if (typeof window !== 'undefined') window.print();
  };

  // The parts of one invoice are headed sections, not landmarks: a page with two
  // invoices (or its own regions) would otherwise repeat "From" and "Bill to" regions.
  const party = (side: 'from' | 'to', value: InvoiceParty) => (
    <section className="mn-invoice-view__party" data-side={side}>
      <Sub className="mn-invoice-view__label" id={`${titleId}-${side}`}>
        {side === 'from' ? m.from : m.to}
      </Sub>
      <p className="mn-invoice-view__party-name">{value.name}</p>
      {value.details && value.details.length > 0 ? (
        <ul className="mn-invoice-view__party-lines">
          {value.details.map((line, i) => (
            <li key={i}>{line}</li>
          ))}
        </ul>
      ) : null}
    </section>
  );

  return (
    <article
      ref={ref}
      aria-labelledby={titleId}
      {...rest}
      className={cx('mn-invoice-view', className)}
    >
      <header className="mn-invoice-view__header">
        <div className="mn-invoice-view__heading">
          {brand ? <div className="mn-invoice-view__brand">{brand}</div> : null}
          <div className="mn-invoice-view__title-row">
            <Title className="mn-invoice-view__title" id={titleId}>
              {title ?? m.title}
              {number !== undefined ? (
                <>
                  {' '}
                  <span className="mn-invoice-view__number">{number}</span>
                </>
              ) : null}
            </Title>
            {status ? <span className="mn-invoice-view__status">{status}</span> : null}
          </div>
        </div>
        {showPrintButton || actions ? (
          <div className="mn-invoice-view__actions">
            {showPrintButton ? (
              <Button size="sm" variant="secondary" onClick={handlePrint}>
                {m.print}
              </Button>
            ) : null}
            {actions}
          </div>
        ) : null}
      </header>

      {from || to || facts.length > 0 ? (
        <div className="mn-invoice-view__meta">
          {from ? party('from', from) : null}
          {to ? party('to', to) : null}
          {facts.length > 0 ? (
            <dl className="mn-invoice-view__facts">
              {facts.map((fact, i) => (
                <div key={i} className="mn-invoice-view__fact">
                  <dt>{fact.label}</dt>
                  <dd>{fact.value}</dd>
                </div>
              ))}
            </dl>
          ) : null}
        </div>
      ) : null}

      {items.length === 0 ? (
        <p className="mn-invoice-view__empty">{m.noItems}</p>
      ) : (
        <table className="mn-invoice-view__items">
          <caption className="mn-invoice-view__caption">{m.lineItems}</caption>
          <thead>
            <tr>
              <th scope="col">{m.description}</th>
              <th scope="col" data-numeric="">
                {m.quantity}
              </th>
              <th scope="col" data-numeric="">
                {m.unitPrice}
              </th>
              <th scope="col" data-numeric="">
                {m.amount}
              </th>
            </tr>
          </thead>
          <tbody>
            {items.map((item, i) => (
              <tr key={item.id ?? i} className="mn-invoice-view__item">
                <th scope="row" className="mn-invoice-view__item-name">
                  <span>{item.description}</span>
                  {item.detail ? (
                    <span className="mn-invoice-view__item-detail">{item.detail}</span>
                  ) : null}
                </th>
                <td data-numeric="" data-label={m.quantity}>
                  {formatQuantity(item.quantity)}
                </td>
                <td data-numeric="" data-label={m.unitPrice}>
                  {given(item.unitPrice)}
                </td>
                <td data-numeric="" data-label={m.amount}>
                  {pick(item.amount, computed.lines[i] ?? 0, 'mn-invoice-view__line-amount')}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}

      <dl className="mn-invoice-view__totals">
        <div className="mn-invoice-view__total-row">
          <dt>{m.subtotal}</dt>
          <dd>{pick(totals?.subtotal, computed.subtotal)}</dd>
        </div>
        {discountShown ? (
          <div className="mn-invoice-view__total-row" data-part="discount">
            <dt>{m.discount}</dt>
            <dd>
              <span aria-hidden>−</span>
              {pick(totals?.discount, computed.discount)}
            </dd>
          </div>
        ) : null}
        {taxShown ? (
          <div className="mn-invoice-view__total-row">
            <dt>{m.tax(rateText)}</dt>
            <dd>{pick(totals?.tax, computed.tax)}</dd>
          </div>
        ) : null}
        <div className="mn-invoice-view__total-row" data-part="total">
          <dt>{m.total}</dt>
          <dd>{pick(totals?.total, computed.total, 'mn-invoice-view__grand-total')}</dd>
        </div>
        {totals?.amountPaid !== undefined ? (
          <div className="mn-invoice-view__total-row">
            <dt>{m.amountPaid}</dt>
            <dd>{given(totals.amountPaid)}</dd>
          </div>
        ) : null}
        {totals?.amountDue !== undefined ? (
          <div className="mn-invoice-view__total-row" data-part="due">
            <dt>{m.amountDue}</dt>
            <dd>{given(totals.amountDue)}</dd>
          </div>
        ) : null}
      </dl>

      {notes ? (
        <section className="mn-invoice-view__notes">
          <Sub className="mn-invoice-view__label" id={`${titleId}-notes`}>
            {m.notes}
          </Sub>
          <div className="mn-invoice-view__notes-body">{notes}</div>
        </section>
      ) : null}

      {footer ? <footer className="mn-invoice-view__footer">{footer}</footer> : null}
    </article>
  );
});
