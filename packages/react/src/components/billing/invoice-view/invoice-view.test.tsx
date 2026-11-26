import { createRef } from 'react';
import { describe, expect, it, vi } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { InvoiceView, type InvoiceLineItem } from './index.js';

const items: InvoiceLineItem[] = [
  { id: 'a', description: 'Design review', detail: 'June', quantity: 2, unitPrice: '100.00' },
  { id: 'b', description: 'Hosting', quantity: '1', unitPrice: 50 },
];

describe('InvoiceView', () => {
  it('is an article named by its title and number, with parties, facts and a status', async () => {
    render(
      <InvoiceView
        number="INV-0001"
        status={<span>Sent</span>}
        issueDate="1 Jun 2026"
        dueDate="15 Jun 2026"
        details={[{ label: 'Terms', value: 'Net 14' }]}
        from={{ name: 'Studio A', details: ['1 Main Street'] }}
        to={{ name: 'Client B', details: ['billing@example.com'] }}
        items={items}
        currency="USD"
        locale="en-US"
      />,
    );
    const invoice = screen.getByRole('article', { name: 'Invoice INV-0001' });
    expect(invoice).toHaveClass('mn-invoice-view');
    expect(screen.getByRole('heading', { level: 2, name: 'Invoice INV-0001' })).toBeVisible();
    // Parties are headed sections, not landmarks: two invoices on a page stay unique.
    expect(screen.queryByRole('region')).toBeNull();
    expect(screen.getByRole('heading', { name: 'From' }).closest('section')).toHaveTextContent(
      'Studio A1 Main Street',
    );
    expect(screen.getByRole('heading', { name: 'Bill to' }).closest('section')).toHaveTextContent(
      'Client B',
    );
    expect(invoice).toHaveTextContent('Sent');
    expect(invoice).toHaveTextContent('Due date15 Jun 2026');
    expect(invoice).toHaveTextContent('TermsNet 14');
    await expectNoA11yViolations();
  });

  it('lists items in a captioned table and works out amounts and totals', () => {
    render(
      <InvoiceView
        items={items}
        currency="USD"
        locale="en-US"
        taxRate={10}
        discount={{ type: 'amount', value: 50 }}
      />,
    );
    const table = screen.getByRole('table', { name: 'Line items' });
    const rows = within(table).getAllByRole('row');
    expect(rows).toHaveLength(3);
    expect(within(rows[1]!).getByRole('rowheader')).toHaveTextContent('Design reviewJune');
    expect(rows[1]).toHaveTextContent('$200.00');
    const totals = screen.getByText('Subtotal').closest('dl')!;
    expect(totals).toHaveTextContent('Subtotal$250.00');
    expect(totals).toHaveTextContent('Discount−$50.00');
    expect(totals).toHaveTextContent('Tax (10%)$20.00');
    expect(totals).toHaveTextContent('Total$220.00');
  });

  it('shows totals from the billing system as given, including paid and due', () => {
    render(
      <InvoiceView
        items={[{ description: 'Retainer', quantity: 1, unitPrice: 1000, amount: 1000 }]}
        currency="INR"
        locale="en-IN"
        totals={{ subtotal: 1000, tax: 180, total: 1180, amountPaid: 500, amountDue: 680 }}
      />,
    );
    const totals = screen.getByText('Subtotal').closest('dl')!;
    expect(totals).toHaveTextContent('Tax₹180.00');
    expect(totals).toHaveTextContent('Total₹1,180.00');
    expect(totals).toHaveTextContent('Amount paid₹500.00');
    expect(totals).toHaveTextContent('Amount due₹680.00');
    expect(totals).not.toHaveTextContent('Discount');
  });

  it('works out totals from minor-unit amounts', () => {
    render(
      <InvoiceView
        items={[{ description: 'Seats', quantity: 3, unitPrice: 1999 }]}
        currency="USD"
        locale="en-US"
        minorUnits
      />,
    );
    expect(screen.getByRole('table')).toHaveTextContent('$19.99$59.97');
    expect(screen.getByText('Total').closest('div')).toHaveTextContent('$59.97');
  });

  it('prints with the browser dialog or onPrint, reachable by keyboard', async () => {
    const user = userEvent.setup();
    const print = vi.spyOn(window, 'print').mockImplementation(() => {});
    const { rerender } = render(
      <InvoiceView items={items} currency="USD" showPrintButton actions={<a href="#pdf">PDF</a>} />,
    );
    await user.tab();
    expect(screen.getByRole('button', { name: 'Print' })).toHaveFocus();
    await user.keyboard('{Enter}');
    expect(print).toHaveBeenCalledTimes(1);
    const onPrint = vi.fn();
    rerender(<InvoiceView items={items} currency="USD" showPrintButton onPrint={onPrint} />);
    await user.click(screen.getByRole('button', { name: 'Print' }));
    expect(onPrint).toHaveBeenCalledTimes(1);
    expect(print).toHaveBeenCalledTimes(1);
    print.mockRestore();
  });

  it('shows the empty message, translates text and forwards the ref', async () => {
    const ref = createRef<HTMLElement>();
    render(
      <InvoiceView
        ref={ref}
        items={[]}
        currency="EUR"
        notes="Pay by bank transfer."
        messages={{ title: 'Rechnung', noItems: 'Keine Positionen.', notes: 'Hinweise' }}
      />,
    );
    expect(ref.current?.tagName).toBe('ARTICLE');
    expect(screen.getByRole('heading', { name: 'Rechnung' })).toBeVisible();
    expect(screen.getByText('Keine Positionen.')).toBeVisible();
    expect(screen.getByRole('heading', { name: 'Hinweise' }).closest('section')).toHaveTextContent(
      'Pay by bank transfer.',
    );
    expect(screen.queryByRole('table')).toBeNull();
    await expectNoA11yViolations();
  });

  it('renders on the server', () => {
    const html = renderToString(
      <InvoiceView number="INV-1" items={items} currency="USD" showPrintButton />,
    );
    expect(html).toContain('mn-invoice-view');
    expect(html).toContain('INV-1');
  });
});
