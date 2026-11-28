import { Button, StatusBadge } from '@rookcom/monarch-react';
import { InvoiceView } from '@rookcom/monarch-react/patterns';
import type { ComponentDocs } from '../../../internal/component-docs.js';

// The scenario of the primary example, shared with iOS and Android (CROSS-PLATFORM.md section 8).
import fixture from './invoice-view.fixture.json';

const { data } = fixture;
const day = (iso: string) =>
  new Date(iso).toLocaleDateString('en-GB', {
    day: 'numeric',
    month: 'long',
    year: 'numeric',
    timeZone: 'UTC',
  });
const sampleItems = (
  data.items as unknown as {
    id: string;
    description: string;
    detail?: string;
    quantity: number;
    unitPrice: number;
  }[]
).map(({ unitPrice, ...item }) => ({ ...item, unitPrice: unitPrice.toFixed(2) }));

export default {
  id: 'invoice-view',
  title: 'InvoiceView',
  summary:
    'A read-only, printable invoice: parties, number and dates, status, line items and totals.',
  sections: {
    purpose: {
      summary:
        'Shows an issued invoice on screen and on paper the same way in every product, so a client portal, an invoice detail page and a print preview agree. Needed by: Dots (workspace invoices, client billing tab and client portal: `rookdots-frontend/src/app/[locale]/(workspace)/w/[slug]/invoices/page.tsx`, `clients/[id]/billing/page.tsx`, `(portal)/portal/[workspaceId]/[clientId]/page.tsx`).',
      useWhen: [
        'Showing an invoice that has been issued (sent, paid, overdue or cancelled).',
        'A print or "save as PDF" view of an invoice.',
        'A client portal where the billed party reads its invoices.',
      ],
      avoidWhen: [
        'Creating or editing invoice lines: use LineItemsEditor.',
        'A list of many invoices: use DataTable with Money and StatusBadge cells.',
        'A payment receipt: use ReceiptPreview.',
      ],
    },
    designPrinciples: {
      items: [
        'The billing system is the source of truth: totals passed in `totals` are shown exactly; only missing figures are worked out, with the same rounding as LineItemsEditor.',
        'One document, two media: the same markup prints cleanly, with actions hidden and muted text at full strength.',
        'Real table semantics for line items, so screen readers announce description, quantity, unit price and amount per row.',
        'Status is a passed-in element (normally StatusBadge) so the product owns the vocabulary.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'The ref and native attributes go to the root `<article>`.',
        '`defaultInvoiceViewMessages` holds the English defaults.',
        'Totals are worked out with `computeLineItemTotals` from LineItemsEditor when not given.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Migrating from the Dots invoice model: `invoiceNumber` → `number`, `issuedDate` / `dueDate` → `issueDate` / `dueDate` (formatted), `client` → `to: { name, details }`, `items[].description / quantity / unitPrice / totalPrice` → `items[].description / quantity / unitPrice / amount`, `amount / taxAmount / totalAmount` → `totals.subtotal / tax / total`, `paidAmount` → `totals.amountPaid`, `status` → `status={<StatusBadge … />}`, `description` → `notes`, `pdfUrl` → an `actions` link.',
        'There is no shadcn/ui invoice component in the apps; Dots builds the view from Card and Table. Card padding moves to the page; Table rows map to `items`.',
        'Every amount is in the same unit: decimals by default, integer minor units with `minorUnits`.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'showPrintButton',
          values: ['false', 'true'],
          description:
            'Adds a Print button that opens the browser print dialog (or calls `onPrint`).',
        },
        {
          prop: 'headingLevel',
          values: ['1', '2', '3'],
          description: 'Heading level of the title; section headings use the next level.',
        },
      ],
      notes: ['There is no visual variant; the invoice uses the surrounding surface.'],
    },
    states: {
      items: [
        {
          name: 'Empty',
          description: 'No line items: the "No line items." message replaces the table.',
        },
        {
          name: 'Discount',
          attribute: 'data-part="discount"',
          description: 'Discount row, shown only when the discount is not zero.',
        },
        {
          name: 'Amount due',
          attribute: 'data-part="due"',
          description: 'Outstanding amount, shown only when `totals.amountDue` is given.',
        },
      ],
    },
    interaction: {
      items: [
        'The invoice is read-only.',
        'The Print button opens the browser print dialog unless `onPrint` takes over.',
        'Header `actions` (Download PDF, Send) keep their own behaviour and are hidden when printing.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Moves to the Print button and any header actions.' },
        { keys: 'Enter / Space', action: 'Activates the focused action.' },
      ],
    },
    accessibility: {
      role: 'article',
      name: 'The title heading, including the invoice number (`aria-labelledby`).',
      items: [
        'Parties and notes are sections with headings ("From", "Bill to", "Notes"), not landmarks, so two invoices on a page do not repeat regions.',
        'Line items are a `<table>` with a caption, column headers and a row header per item.',
        'Totals are a description list, so each label is read with its amount.',
        'The discount minus sign is decorative; the "Discount" label carries the meaning.',
      ],
    },
    responsive: {
      items: [
        'Parties and facts flow from three columns to one.',
        'Below 480px each line becomes a block with inline labels for quantity, unit price and amount, so nothing scrolls sideways at 390px.',
        'The Print button is 44px tall on coarse pointers.',
      ],
    },
    theme: {
      items: [
        'Text, border and spacing tokens only; works in light, dark and every product accent.',
        'Print styles hide actions, repeat the table header on each page and avoid splitting a line or the totals.',
        'Forced-colours mode keeps table and total borders.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Issued invoice, totals worked out',
          render: () => (
            <InvoiceView
              number={data.number}
              status={<StatusBadge status={data.status.tone} label={data.status.label} size="sm" />}
              issueDate={day(data.issueDate)}
              dueDate={day(data.dueDate)}
              details={[data.detail]}
              from={data.from}
              to={data.to}
              items={sampleItems}
              currency={data.currency}
              locale={data.locale}
              taxRate={data.taxRate}
              notes={data.notes}
              showPrintButton
            />
          ),
        },
        {
          title: 'Totals from the billing system, partly paid',
          render: () => (
            <InvoiceView
              number="INV-0043"
              status={<StatusBadge status="warning" label="Partly paid" size="sm" />}
              to={{ name: 'Client C' }}
              items={[
                {
                  id: 'r',
                  description: 'Monthly retainer',
                  quantity: 1,
                  unitPrice: 5000,
                  amount: 5000,
                },
              ]}
              currency="INR"
              locale="en-IN"
              totals={{ subtotal: 5000, tax: 900, total: 5900, amountPaid: 2000, amountDue: 3900 }}
              actions={<Button size="sm">Download PDF</Button>}
            />
          ),
        },
        {
          title: 'Empty draft',
          render: () => (
            <InvoiceView
              number="Draft"
              status={<StatusBadge status="neutral" label="Draft" size="sm" />}
              items={[]}
              currency="EUR"
              locale="en-IE"
            />
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Pass `totals` from the billing system whenever it has them, so the screen matches the PDF.',
        'Give the billed party enough `details` to identify them on paper.',
        'Use StatusBadge for `status` with the product’s own labels.',
      ],
      dont: [
        'Do not use InvoiceView to edit lines; switch to LineItemsEditor.',
        'Do not mix decimal and minor-unit amounts in one invoice.',
        'Do not put destructive actions (void, delete) in `actions` without a confirmation.',
      ],
    },
    related: {
      items: [
        { id: 'line-items-editor', reason: 'Editing the lines this view shows.' },
        { id: 'money', reason: 'Formats every amount.' },
        { id: 'receipt-preview', reason: 'A payment receipt rather than an invoice.' },
        { id: 'payslip-view', reason: 'The same document pattern for pay.' },
        { id: 'status-badge', reason: 'The invoice status.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['button', 'money', 'line-items-editor'],
    },
    testing: {
      unit: [
        'Article named by title and number; parties are headed sections, not regions; facts and status shown; axe clean.',
        'Captioned table with row headers; amounts, discount, tax and total worked out.',
        'Totals from the billing system shown as given, with amount paid and due.',
        'Minor-unit amounts worked out correctly.',
        'Print button reachable by Tab; opens the print dialog or calls `onPrint`.',
        'Empty message, translated text, ref forwarding; axe clean.',
        'Server rendering.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
