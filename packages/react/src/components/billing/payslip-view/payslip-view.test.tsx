import { describe, expect, it, vi } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { PayslipView, type PayslipLine, type PayslipViewProps } from './index.js';

const lines: PayslipLine[] = [
  { name: 'Basic', kind: 'earning', amount: '4000.00', note: '30 of 30 days' },
  { name: 'Allowance', kind: 'earning', amount: '1000.00' },
  { name: 'Income tax', kind: 'deduction', amount: '600.00' },
  { name: 'Travel', kind: 'reimbursement', amount: '120.00' },
  { name: 'Pension (employer)', kind: 'employer', amount: '400.00' },
];

function Slip(props: Partial<PayslipViewProps>) {
  return (
    <PayslipView
      issuer="Example Co."
      title="Payslip"
      subtitle="March 2026 · PS-0042"
      details={[
        { label: 'Employee', value: 'Sam Lee' },
        { label: 'Pay date', value: '31 Mar 2026' },
      ]}
      lines={lines}
      totals={{ gross: '5000.00', deductions: '600.00', reimbursements: '120.00', net: '4520.00' }}
      currency="USD"
      locale="en-US"
      {...props}
    />
  );
}

describe('PayslipView', () => {
  it('is an article named by its title with grouped lines and a summary', async () => {
    render(<Slip />);
    const article = screen.getByRole('article', { name: 'Payslip' });
    expect(article).toHaveClass('mn-payslip-view');
    expect(screen.getByRole('heading', { level: 2, name: 'Payslip' })).toBeInTheDocument();
    const earnings = screen.getByRole('region', { name: 'Earnings' });
    expect(within(earnings).getAllByRole('listitem')).toHaveLength(2);
    expect(within(earnings).getByText('30 of 30 days')).toBeInTheDocument();
    expect(screen.getByRole('region', { name: 'Employer contributions' })).toBeInTheDocument();
    expect(screen.getByText('Sam Lee')).toBeInTheDocument();
    expect(screen.getByText('$4,520.00')).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('orders groups and hides employer contributions on request', () => {
    render(<Slip hideEmployerContributions headingLevel={3} />);
    const headings = screen.getAllByRole('heading', { level: 4 }).map((h) => h.textContent);
    expect(headings).toEqual(['Earnings', 'Deductions', 'Reimbursements']);
    expect(screen.queryByText('Pension (employer)')).toBeNull();
  });

  it('omits zero reimbursements and shows year to date', () => {
    render(
      <Slip
        totals={{ gross: 5000, deductions: 600, reimbursements: 0, net: 4400 }}
        yearToDate={[
          { label: 'Gross', amount: 15000 },
          { label: 'Tax withheld', amount: 1800 },
        ]}
        yearToDateTitle="Year to date (from January)"
      />,
    );
    const summary = document.querySelector('.mn-payslip-view__summary') as HTMLElement;
    expect(within(summary).getByText('Net pay')).toBeInTheDocument();
    expect(within(summary).queryByText('Reimbursements')).toBeNull();
    const ytd = screen.getByRole('region', { name: 'Year to date (from January)' });
    expect(within(ytd).getByText('$1,800.00')).toBeInTheDocument();
  });

  it('formats minor units in the locale', () => {
    render(
      <Slip
        currency="INR"
        locale="en-IN"
        minorUnits
        lines={[{ name: 'Basic', kind: 'earning', amount: 12500000 }]}
        totals={{ gross: 12500000, deductions: 0, net: 12500000 }}
      />,
    );
    expect(screen.getAllByText('₹1,25,000.00').length).toBeGreaterThan(0);
  });

  it('calls onDownload and shows the busy state', async () => {
    const user = userEvent.setup();
    const onDownload = vi.fn();
    const { rerender } = render(<Slip onDownload={onDownload} />);
    await user.click(screen.getByRole('button', { name: 'Download PDF' }));
    expect(onDownload).toHaveBeenCalledTimes(1);
    rerender(<Slip onDownload={onDownload} downloading />);
    expect(screen.getByRole('button', { name: /Download PDF/ })).toHaveAttribute(
      'aria-busy',
      'true',
    );
  });

  it('uses translated labels and an empty message', () => {
    render(
      <Slip lang="hi" lines={[]} messages={{ netPay: 'शुद्ध वेतन', noLines: 'कोई पंक्ति नहीं' }} />,
    );
    expect(screen.getByRole('article')).toHaveAttribute('lang', 'hi');
    expect(screen.getByText('शुद्ध वेतन')).toBeInTheDocument();
    expect(screen.getByText('कोई पंक्ति नहीं')).toBeInTheDocument();
  });

  it('renders on the server', () => {
    const html = renderToString(<Slip />);
    expect(html).toContain('mn-payslip-view');
    expect(html).toContain('Net pay');
  });
});
