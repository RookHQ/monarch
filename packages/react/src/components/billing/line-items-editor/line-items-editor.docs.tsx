import { useState } from 'react';
import {
  LineItemsEditor,
  type LineItem,
  type LineItemsDiscount,
} from '@rookcom/monarch-react/patterns';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const sample: LineItem[] = [
  { id: 'r1', description: 'Discovery workshop', quantity: '1', unitPrice: '800.00' },
  { id: 'r2', description: 'Design hours', quantity: '12', unitPrice: '95.00' },
  { id: 'r3', description: 'Hosting (monthly)', quantity: '3', unitPrice: '24.50' },
];

function InvoiceDemo() {
  const [items, setItems] = useState<LineItem[]>(sample);
  const [taxRate, setTaxRate] = useState(18);
  const [discount, setDiscount] = useState<LineItemsDiscount>({ type: 'percent', value: 5 });
  return (
    <LineItemsEditor
      items={items}
      onChange={setItems}
      currency="USD"
      locale="en-US"
      required
      taxRate={taxRate}
      onTaxRateChange={setTaxRate}
      taxRateOptions={[
        { value: 0, label: 'No tax' },
        { value: 5, label: '5%' },
        { value: 12, label: '12%' },
        { value: 18, label: '18%' },
      ]}
      discount={discount}
      onDiscountChange={setDiscount}
    />
  );
}

function EmptyDemo() {
  const [items, setItems] = useState<LineItem[]>([]);
  return (
    <LineItemsEditor
      items={items}
      onChange={setItems}
      currency="EUR"
      locale="de-DE"
      required
      error={items.length === 0 ? 'Add at least one item.' : undefined}
    />
  );
}

export default {
  id: 'line-items-editor',
  title: 'LineItemsEditor',
  summary:
    'Editable invoice or proposal rows (description, quantity, unit price) with discount, tax and exact totals.',
  sections: {
    purpose: {
      summary:
        'Lets people build the billable rows of an invoice, quote or proposal and see each row’s amount, the discount, the tax and the total update as they type, formatted in the document’s currency. Needed by: Dots (invoices and proposals).',
      useWhen: [
        'Creating or editing an invoice, quote, proposal or purchase order.',
        'Showing the rows of a sent document read-only, with the same layout and totals.',
      ],
      avoidWhen: [
        'Payroll lines that are calculated, not typed: use PayslipView.',
        'Generic editable tables of non-monetary data: use DataTable with editable cells.',
      ],
    },
    designPrinciples: {
      items: [
        'Controlled and data-only: the product owns the rows, saves them and decides tax rules; the editor only edits and adds up.',
        'Exact money: each row is rounded to the currency’s minor unit and totals are summed as integers, so the total always equals the rows shown.',
        'Keep what people type: quantities and prices stay as strings while editing, so “1.” or “12,5” are not reformatted under the cursor.',
        'One currency per document. The editor never converts.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'The ref and native attributes go to the root `<div role="group">`.',
        '`computeLineItemTotals(items, currency, { taxRate, discount })` returns the same minor-unit totals the editor shows; use it to save totals or validate on the server boundary.',
        '`parseDecimal(text)` reads `1,234.50`, `1234,5` and returns `null` for text that is not a number.',
        '`createLineItem(partial?)` makes an empty row with a unique id; pass `createItem` to add rows with your own ids or defaults.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Discount applies to the subtotal first and is capped at it; tax applies to the discounted subtotal. Products with per-line tax or several taxes compute their own totals and pass `showTotals={false}`.',
        'Migrating from the app copy (Dots `components/forms/LineItemsEditor.tsx`): `items` and `onChange` keep their shape, but every row now needs an `id` (use `createLineItem()` or your saved line id). `currencySymbol="₹"` becomes `currency="INR"` (plus `locale`). `error` is unchanged. The invoice page’s separate Tax select and Subtotal/Tax/Total box move into the editor as `taxRate`, `onTaxRateChange` and `taxRateOptions` (map `TAX_RATES` to `{ value, label }`). Totals come from `computeLineItemTotals` (minor units) instead of `toFixed(2)` arithmetic.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'readOnly',
          values: ['false', 'true'],
          description:
            'Text instead of inputs and no add/remove buttons, for sent documents (`data-readonly`).',
        },
        {
          prop: 'taxRateOptions',
          values: ['undefined', 'LineItemsTaxRateOption[]'],
          description:
            'With `onTaxRateChange`: a select of preset rates instead of a number input.',
        },
        {
          prop: 'showTotals',
          values: ['true', 'false'],
          description: 'Hide the totals block when the product shows its own.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Empty',
          description: 'No rows: a message row asks the person to add one.',
        },
        {
          name: 'Invalid number',
          attribute: 'data-invalid (row), aria-invalid (input)',
          description:
            'A quantity or price that is not a number is marked invalid and counts as zero in the totals.',
        },
        {
          name: 'Error',
          attribute: 'data-invalid (root)',
          description: '`error` is shown under the table, announced, and describes the group.',
        },
        { name: 'Read-only', attribute: 'data-readonly', description: 'Values as text.' },
        { name: 'Disabled', attribute: 'data-disabled', description: 'Every control disabled.' },
      ],
    },
    interaction: {
      items: [
        'Typing in a quantity or price updates that row’s amount and the totals immediately.',
        'Add item appends a row and moves focus to its description.',
        'Removing a row moves focus to the next row, the previous one, or Add item when none are left. Rows below `minItems` cannot be removed.',
        'Additions and removals are announced in a polite live region.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Tab / Shift+Tab',
          action: 'Move through description, quantity, price and remove for each row.',
        },
        {
          keys: 'Enter (in a row)',
          action: 'Move to the next row’s description; on the last row, add a row.',
        },
        { keys: 'Enter / Space (buttons)', action: 'Add a row or remove the focused row.' },
      ],
    },
    accessibility: {
      role: 'group containing a table',
      name: 'The visible label (“Line items”), via aria-labelledby.',
      items: [
        'Every input has a name with its column and row (“Qty, row 2”), so it stays clear in the stacked phone layout.',
        'Remove buttons name the row and its description (“Remove row 1: Design hours”).',
        'Invalid numbers set aria-invalid; the list-level `error` is a role="alert" paragraph linked with aria-describedby.',
        'The required asterisk is decorative; say what is required in `error`.',
      ],
    },
    responsive: {
      items: [
        'Below a 34rem container width each row becomes a small card: description and remove on top, quantity and price side by side, amount underneath, with visible column labels.',
        'The totals block takes the full width on narrow containers.',
        'On coarse pointers inputs, selects and buttons are at least 44px tall.',
      ],
    },
    theme: {
      items: [
        'Surface and header use `--mn-color-bg-surface` and `--mn-color-bg-subtle`; borders `--mn-color-border-subtle`.',
        'The total row uses `--mn-color-text-default` and a `--mn-color-border-default` rule; errors use `--mn-color-danger-strong`.',
        'In forced-colours mode the table, rows and totals keep system-colour borders.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Invoice with discount and preset tax rates',
          stack: true,
          render: () => <InvoiceDemo />,
        },
        {
          title: 'Read-only (sent invoice)',
          stack: true,
          render: () => (
            <LineItemsEditor
              items={sample}
              onChange={() => {}}
              currency="INR"
              locale="en-IN"
              readOnly
              taxRate={18}
            />
          ),
        },
        { title: 'Empty with a required error', stack: true, render: () => <EmptyDemo /> },
      ],
    },
    doDont: {
      do: [
        'Give each row a stable id from your data so focus and React keys survive edits.',
        'Save the minor-unit totals from `computeLineItemTotals`, not a recomputed float.',
        'Pass the document’s currency and the viewer’s locale.',
      ],
      dont: [
        'Do not mix currencies in one editor.',
        'Do not encode legal tax logic in the editor: compute it in the product and hide the built-in totals.',
        'Do not strip what the person typed to a number on every keystroke.',
      ],
    },
    related: {
      items: [
        { id: 'money', reason: 'Formats every amount the editor shows.' },
        { id: 'payslip-view', reason: 'Read-only, grouped payroll lines and net pay.' },
        { id: 'data-table', reason: 'General tabular data that is not a priced list.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['button', 'text-field', 'form-field', 'visually-hidden', 'money'],
    },
    testing: {
      unit: [
        'parseDecimal reads dot and comma decimals and rejects text.',
        'Totals are summed in minor units, rounded per currency (USD, JPY), with discount before tax and an amount discount capped at the subtotal.',
        'Labelled inputs per row and column; amounts and totals formatted by Money.',
        'Editing recalculates; non-numbers are marked aria-invalid.',
        'Add item focuses and announces the new row; Enter moves down or adds a row.',
        'Remove moves focus to the next row or Add item and announces it; minItems blocks removal.',
        'Tax rate input, preset tax select and discount amount/type editing.',
        'Read-only rendering, error description, custom messages, axe and SSR.',
      ],
      axe: true,
      ssr: true,
      browser: ['Visual baselines: /__gallery', 'Cross-browser axe: /components/line-items-editor'],
    },
  },
} satisfies ComponentDocs;
