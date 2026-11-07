import { describe, expect, it, vi } from 'vitest';
import { useState } from 'react';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import {
  LineItemsEditor,
  computeLineItemTotals,
  parseDecimal,
  type LineItem,
  type LineItemsDiscount,
  type LineItemsEditorProps,
} from './index.js';

const rows: LineItem[] = [
  { id: 'a', description: 'Design work', quantity: '2', unitPrice: '150.00' },
  { id: 'b', description: 'Hosting', quantity: '1', unitPrice: '25.50' },
];

function Harness(props: Partial<LineItemsEditorProps> & { initial?: LineItem[] }) {
  const { initial = rows, ...rest } = props;
  const [items, setItems] = useState<LineItem[]>(initial);
  const [taxRate, setTaxRate] = useState(rest.taxRate);
  const [discount, setDiscount] = useState<LineItemsDiscount | undefined>(rest.discount);
  return (
    <LineItemsEditor
      currency="USD"
      locale="en-US"
      {...rest}
      items={items}
      onChange={setItems}
      taxRate={taxRate}
      discount={discount}
      onTaxRateChange={rest.onTaxRateChange ? setTaxRate : undefined}
      onDiscountChange={rest.onDiscountChange ? setDiscount : undefined}
    />
  );
}

const totalRow = (name: string) =>
  screen.getByText(name, { selector: 'dt' }).closest('div') as HTMLElement;

describe('parseDecimal', () => {
  it('reads dots, grouping commas and lone decimal commas', () => {
    expect(parseDecimal('1,234.50')).toBe(1234.5);
    expect(parseDecimal('12,5')).toBe(12.5);
    expect(parseDecimal('')).toBe(0);
    expect(parseDecimal('abc')).toBeNull();
    expect(parseDecimal(3)).toBe(3);
  });
});

describe('computeLineItemTotals', () => {
  it('sums in minor units and applies discount before tax', () => {
    const t = computeLineItemTotals(rows, 'USD', {
      taxRate: 10,
      discount: { type: 'percent', value: 10 },
    });
    expect(t.lines).toEqual([30000, 2550]);
    expect(t.subtotal).toBe(32550);
    expect(t.discount).toBe(3255);
    expect(t.tax).toBe(2930);
    expect(t.total).toBe(32225);
  });

  it('rounds to the currency digits and caps an amount discount', () => {
    const t = computeLineItemTotals([{ quantity: '3', unitPrice: '0.1' }], 'USD');
    expect(t.subtotal).toBe(30);
    const yen = computeLineItemTotals([{ quantity: '1', unitPrice: '999' }], 'JPY', {
      discount: { type: 'amount', value: 5000 },
    });
    expect(yen.discount).toBe(999);
    expect(yen.total).toBe(0);
  });
});

describe('LineItemsEditor', () => {
  it('renders labelled inputs, row amounts and totals', async () => {
    render(<Harness />);
    expect(screen.getByRole('group', { name: 'Line items' })).toHaveClass('mn-line-items-editor');
    expect(screen.getByRole('textbox', { name: 'Description, row 1' })).toHaveValue('Design work');
    expect(screen.getByRole('textbox', { name: 'Unit price (USD), row 2' })).toHaveValue('25.50');
    expect(screen.getByText('$300.00')).toBeInTheDocument();
    expect(within(totalRow('Total')).getByText('$325.50')).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('recalculates as quantities change and flags a value that is not a number', async () => {
    const user = userEvent.setup();
    render(<Harness />);
    const qty = screen.getByRole('textbox', { name: 'Qty, row 2' });
    await user.clear(qty);
    await user.type(qty, '4');
    expect(within(totalRow('Subtotal')).getByText('$402.00')).toBeInTheDocument();
    await user.type(qty, 'x');
    expect(qty).toHaveAttribute('aria-invalid', 'true');
  });

  it('adds a row, focuses it and announces it', async () => {
    const user = userEvent.setup();
    render(<Harness />);
    await user.click(screen.getByRole('button', { name: 'Add item' }));
    const added = screen.getByRole('textbox', { name: 'Description, row 3' });
    expect(added).toHaveFocus();
    expect(screen.getByText('Row 3 added')).toBeInTheDocument();
  });

  it('Enter moves down a row and adds one from the last row', async () => {
    const user = userEvent.setup();
    render(<Harness />);
    await user.click(screen.getByRole('textbox', { name: 'Description, row 1' }));
    await user.keyboard('{Enter}');
    expect(screen.getByRole('textbox', { name: 'Description, row 2' })).toHaveFocus();
    await user.keyboard('{Enter}');
    expect(screen.getByRole('textbox', { name: 'Description, row 3' })).toHaveFocus();
  });

  it('removes a row and moves focus to the next one, then to Add item', async () => {
    const user = userEvent.setup();
    render(<Harness />);
    await user.click(screen.getByRole('button', { name: 'Remove row 1: Design work' }));
    expect(screen.getByRole('textbox', { name: 'Description, row 1' })).toHaveValue('Hosting');
    expect(screen.getByRole('textbox', { name: 'Description, row 1' })).toHaveFocus();
    expect(screen.getByText('Row 1 removed')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Remove row 1: Hosting' }));
    expect(screen.getByText('No items yet. Add one to start.')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Add item' })).toHaveFocus();
  });

  it('keeps minItems rows', () => {
    render(<Harness minItems={2} />);
    expect(screen.getByRole('button', { name: 'Remove row 1: Design work' })).toBeDisabled();
  });

  it('edits tax and discount', async () => {
    const user = userEvent.setup();
    render(
      <Harness
        taxRate={0}
        discount={{ type: 'percent', value: 0 }}
        onTaxRateChange={vi.fn()}
        onDiscountChange={vi.fn()}
      />,
    );
    const tax = screen.getByRole('textbox', { name: 'Tax rate' });
    await user.clear(tax);
    await user.type(tax, '10');
    expect(within(totalRow('Total')).getByText('$358.05')).toBeInTheDocument();
    await user.selectOptions(screen.getByRole('combobox', { name: 'Discount type' }), 'USD');
    const discount = screen.getByRole('textbox', { name: 'Discount' });
    await user.clear(discount);
    await user.type(discount, '25.5');
    expect(within(totalRow('Total')).getByText('$330.00')).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('offers preset tax rates', async () => {
    const user = userEvent.setup();
    render(
      <Harness
        taxRate={0}
        onTaxRateChange={vi.fn()}
        taxRateOptions={[
          { value: 0, label: 'None' },
          { value: 5, label: '5%' },
        ]}
      />,
    );
    await user.selectOptions(screen.getByRole('combobox', { name: 'Tax rate' }), '5%');
    expect(within(totalRow('Total')).getByText('$341.78')).toBeInTheDocument();
  });

  it('read-only shows text and no controls', async () => {
    render(<Harness readOnly taxRate={18} />);
    expect(screen.queryByRole('textbox')).toBeNull();
    expect(screen.queryByRole('button')).toBeNull();
    expect(screen.getByText('Design work')).toBeInTheDocument();
    expect(screen.getByText('Tax (18%)')).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('links the error to the group and uses custom messages', () => {
    render(
      <Harness error="Add at least one item" messages={{ label: 'Items', addItem: 'New row' }} />,
    );
    const group = screen.getByRole('group', { name: 'Items' });
    expect(group).toHaveAccessibleDescription('Add at least one item');
    expect(screen.getByRole('button', { name: 'New row' })).toBeInTheDocument();
  });

  it('renders on the server', () => {
    const html = renderToString(
      <LineItemsEditor items={rows} onChange={() => {}} currency="EUR" locale="de-DE" />,
    );
    expect(html).toContain('mn-line-items-editor');
    expect(html).toContain('325,50');
  });
});
