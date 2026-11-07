import { forwardRef, useId, type HTMLAttributes, type ReactNode } from 'react';
import { cx } from '../../../internal/cx.js';
import { ArrowDownGlyph } from '../../../internal/icons.js';
import { Button } from '../../actions/button/index.js';
import { DescriptionItem, DescriptionList } from '../../data-display/description-list/index.js';
import { Money, type MoneyAmount } from '../money/index.js';

/** Which group a payslip line belongs to. */
export type PayslipLineKind = 'earning' | 'deduction' | 'reimbursement' | 'employer';

/** One calculated line of a payslip. */
export interface PayslipLine {
  /** Stable key; falls back to the name and position. */
  id?: string | undefined;
  /** Component name, e.g. "Basic", "Income tax". */
  name: ReactNode;
  kind: PayslipLineKind;
  /** Amount in `currency`, as a positive number (deductions are not negated). */
  amount: MoneyAmount;
  /** How the amount was worked out, shown under the name. */
  note?: ReactNode;
}

/** A label and value in the details grid (employee, period, paid days, …). */
export interface PayslipDetail {
  label: ReactNode;
  value: ReactNode;
}

/** Totals as calculated by payroll. PayslipView never recalculates them. */
export interface PayslipTotals {
  gross: MoneyAmount;
  deductions: MoneyAmount;
  /** Shown only when present and not zero. */
  reimbursements?: MoneyAmount | undefined;
  net: MoneyAmount;
}

/** One year-to-date figure. */
export interface PayslipYearToDateItem {
  label: ReactNode;
  amount: MoneyAmount;
}

export interface PayslipViewMessages {
  earning: string;
  deduction: string;
  reimbursement: string;
  employer: string;
  grossEarnings: string;
  totalDeductions: string;
  reimbursements: string;
  netPay: string;
  /** Heading of the year-to-date block. */
  yearToDate: string;
  /** Shown when there are no lines. */
  noLines: string;
  download: string;
}

const DEFAULT_MESSAGES: PayslipViewMessages = {
  earning: 'Earnings',
  deduction: 'Deductions',
  reimbursement: 'Reimbursements',
  employer: 'Employer contributions',
  grossEarnings: 'Gross earnings',
  totalDeductions: 'Total deductions',
  reimbursements: 'Reimbursements',
  netPay: 'Net pay',
  yearToDate: 'Year to date',
  noLines: 'No pay lines.',
  download: 'Download PDF',
};

const GROUPS: PayslipLineKind[] = ['earning', 'deduction', 'reimbursement', 'employer'];

function isZero(amount: MoneyAmount): boolean {
  return typeof amount === 'string' ? Number(amount) === 0 : Number(amount) === 0;
}

export interface PayslipViewProps extends Omit<HTMLAttributes<HTMLElement>, 'title'> {
  /** Main heading, e.g. "Payslip" or "Final settlement". */
  title: ReactNode;
  /** Who pays: the organisation or legal entity name, shown above the title. */
  issuer?: ReactNode;
  /** Short line under the title: period, payslip number and so on. */
  subtitle?: ReactNode;
  /** A status shown beside the title, such as a StatusBadge for “Paid”. */
  status?: ReactNode;
  /** Employee and period facts in a grid. */
  details?: readonly PayslipDetail[] | undefined;
  /** Calculated lines, grouped by `kind` in the order earnings, deductions, reimbursements, employer. */
  lines: readonly PayslipLine[];
  /** Totals from payroll. */
  totals: PayslipTotals;
  /** Year-to-date figures shown as small tiles. */
  yearToDate?: readonly PayslipYearToDateItem[] | undefined;
  /** Replaces the year-to-date heading, e.g. "Year to date (from April)". */
  yearToDateTitle?: ReactNode;
  /** ISO 4217 code for every amount. */
  currency: string;
  /** BCP 47 locale for amounts. Default: the runtime locale. */
  locale?: string | undefined;
  /** Amounts are integer minor units (cents, paise). Default `false`. */
  minorUnits?: boolean | undefined;
  /** Hide employer contributions, for the employee’s own copy where policy requires it. */
  hideEmployerContributions?: boolean | undefined;
  /** Shows a Download PDF button. The product generates the file. */
  onDownload?: (() => void) | undefined;
  /** The download is being prepared: the button shows a spinner. */
  downloading?: boolean | undefined;
  /** Extra actions beside (or instead of) Download, such as a Print button. */
  actions?: ReactNode;
  /** Heading level of the title; group headings use the next level. Default 2. */
  headingLevel?: 2 | 3 | 4 | undefined;
  /** Replace any English text (for example with the payslip language’s labels). */
  messages?: Partial<PayslipViewMessages> | undefined;
}

/**
 * An on-screen payslip: issuer and title, employee and period details, pay
 * lines grouped into earnings, deductions, reimbursements and employer
 * contributions, the gross-to-net summary and optional year-to-date figures.
 * It displays numbers payroll has already calculated.
 */
export const PayslipView = forwardRef<HTMLElement, PayslipViewProps>(function PayslipView(
  {
    title,
    issuer,
    subtitle,
    status,
    details,
    lines,
    totals,
    yearToDate,
    yearToDateTitle,
    currency,
    locale,
    minorUnits = false,
    hideEmployerContributions = false,
    onDownload,
    downloading = false,
    actions,
    headingLevel = 2,
    messages: messagesProp,
    className,
    ...rest
  },
  ref,
) {
  const m = { ...DEFAULT_MESSAGES, ...messagesProp };
  const titleId = useId();
  const Title = `h${headingLevel}` as 'h2';
  const Sub = `h${headingLevel + 1}` as 'h3';
  const money = (amount: MoneyAmount, extra?: string) => (
    <Money
      amount={amount}
      currency={currency}
      minorUnits={minorUnits}
      {...(locale !== undefined && { locale })}
      {...(extra !== undefined && { className: extra })}
    />
  );
  const groups = GROUPS.filter((g) => !(hideEmployerContributions && g === 'employer'))
    .map((kind) => ({ kind, lines: lines.filter((l) => l.kind === kind) }))
    .filter((g) => g.lines.length > 0);

  return (
    <article
      ref={ref}
      aria-labelledby={titleId}
      {...rest}
      className={cx('mn-payslip-view', className)}
    >
      <header className="mn-payslip-view__header">
        <div className="mn-payslip-view__heading">
          {issuer ? <p className="mn-payslip-view__issuer">{issuer}</p> : null}
          <div className="mn-payslip-view__title-row">
            <Title className="mn-payslip-view__title" id={titleId}>
              {title}
            </Title>
            {status}
          </div>
          {subtitle ? <p className="mn-payslip-view__subtitle">{subtitle}</p> : null}
        </div>
        {onDownload || actions ? (
          <div className="mn-payslip-view__actions">
            {onDownload ? (
              <Button
                size="sm"
                leadingIcon={<ArrowDownGlyph />}
                loading={downloading}
                onClick={onDownload}
              >
                {m.download}
              </Button>
            ) : null}
            {actions}
          </div>
        ) : null}
      </header>

      {details && details.length > 0 ? (
        <DescriptionList columns={3} className="mn-payslip-view__details">
          {details.map((d, i) => (
            <DescriptionItem key={i} term={d.label}>
              {d.value}
            </DescriptionItem>
          ))}
        </DescriptionList>
      ) : null}

      {groups.length === 0 ? (
        <p className="mn-payslip-view__empty">{m.noLines}</p>
      ) : (
        groups.map((g) => {
          const headingId = `${titleId}-${g.kind}`;
          return (
            <section
              key={g.kind}
              className="mn-payslip-view__group"
              data-kind={g.kind}
              aria-labelledby={headingId}
            >
              <Sub className="mn-payslip-view__group-title" id={headingId}>
                {m[g.kind]}
              </Sub>
              <ul className="mn-payslip-view__lines">
                {g.lines.map((line, i) => (
                  <li key={line.id ?? `${g.kind}-${i}`} className="mn-payslip-view__line">
                    <span className="mn-payslip-view__line-name">
                      <span>{line.name}</span>
                      {line.note ? (
                        <span className="mn-payslip-view__line-note">{line.note}</span>
                      ) : null}
                    </span>
                    {money(line.amount, 'mn-payslip-view__line-amount')}
                  </li>
                ))}
              </ul>
            </section>
          );
        })
      )}

      <dl className="mn-payslip-view__summary">
        <div className="mn-payslip-view__summary-row">
          <dt>{m.grossEarnings}</dt>
          <dd>{money(totals.gross)}</dd>
        </div>
        <div className="mn-payslip-view__summary-row">
          <dt>{m.totalDeductions}</dt>
          <dd>{money(totals.deductions)}</dd>
        </div>
        {totals.reimbursements !== undefined && !isZero(totals.reimbursements) ? (
          <div className="mn-payslip-view__summary-row">
            <dt>{m.reimbursements}</dt>
            <dd>{money(totals.reimbursements)}</dd>
          </div>
        ) : null}
        <div className="mn-payslip-view__summary-row" data-part="net">
          <dt>{m.netPay}</dt>
          <dd>{money(totals.net, 'mn-payslip-view__net')}</dd>
        </div>
      </dl>

      {yearToDate && yearToDate.length > 0 ? (
        <section className="mn-payslip-view__ytd" aria-labelledby={`${titleId}-ytd`}>
          <Sub className="mn-payslip-view__group-title" id={`${titleId}-ytd`}>
            {yearToDateTitle ?? m.yearToDate}
          </Sub>
          <dl className="mn-payslip-view__ytd-grid">
            {yearToDate.map((item, i) => (
              <div key={i} className="mn-payslip-view__ytd-item">
                <dt>{item.label}</dt>
                <dd>{money(item.amount)}</dd>
              </div>
            ))}
          </dl>
        </section>
      ) : null}
    </article>
  );
});
