'use client';

import {
  forwardRef,
  useEffect,
  useId,
  useRef,
  useState,
  type HTMLAttributes,
  type KeyboardEvent,
  type ReactNode,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { PlusGlyph } from '../../../internal/icons.js';
import { Button, IconButton } from '../../actions/button/index.js';
import { Input } from '../../forms/text-field/index.js';
import { NativeSelect } from '../../forms/form-field/index.js';
import { VisuallyHidden } from '../../layout/visually-hidden/index.js';
import { Money, currencyDigits } from '../money/index.js';

/** One row of an invoice, quote or proposal. Numbers are kept as typed (strings) while editing. */
export interface LineItem {
  /** Stable key for the row. Used for React keys and focus management. */
  id: string;
  /** What is being charged for. */
  description: string;
  /** Quantity as typed, e.g. `"2"` or `"1.5"`. A number is accepted and shown as is. */
  quantity: string | number;
  /** Price of one unit in major units (dollars, rupees) as typed, e.g. `"120.00"`. */
  unitPrice: string | number;
}

/** A discount on the subtotal: a percentage or a fixed amount in major units. */
export interface LineItemsDiscount {
  type: 'percent' | 'amount';
  value: number;
}

/** A preset tax rate offered in a select. `value` is a percentage. */
export interface LineItemsTaxRateOption {
  value: number;
  label: string;
}

/** Totals in the currency's minor units (cents, paise), so they add up exactly. */
export interface LineItemsTotals {
  /** Each row's amount (quantity × unit price), in row order. */
  lines: number[];
  subtotal: number;
  /** The discount taken off the subtotal, never more than the subtotal. */
  discount: number;
  /** Tax on the discounted subtotal. */
  tax: number;
  total: number;
}

export interface LineItemsEditorMessages {
  /** Group label shown above the table. */
  label: string;
  description: string;
  quantity: string;
  /** Unit price column heading; receives the currency code. */
  unitPrice: (currency: string) => string;
  amount: string;
  /** Visually hidden heading of the remove-button column. */
  actions: string;
  /** Accessible name of an input: column heading and 1-based row number. */
  cellLabel: (column: string, row: number) => string;
  descriptionPlaceholder: string;
  addItem: string;
  /** Accessible name of a row's remove button. */
  removeItem: (row: number, description: string) => string;
  /** Shown when there are no rows. */
  empty: string;
  /** Announced after a row is added. */
  added: (row: number) => string;
  /** Announced after a row is removed. */
  removed: (row: number) => string;
  /** Shown in place of a value that is not a number. */
  invalidNumber: string;
  subtotal: string;
  discount: string;
  /** Accessible name of the discount type select. */
  discountType: string;
  percentOption: string;
  /** Tax row label; receives the formatted rate ("18%") or an empty string when there is none. */
  tax: (rate: string) => string;
  /** Accessible name of the tax rate control. */
  taxRate: string;
  total: string;
}

const DEFAULT_MESSAGES: LineItemsEditorMessages = {
  label: 'Line items',
  description: 'Description',
  quantity: 'Qty',
  unitPrice: (currency) => `Unit price (${currency})`,
  amount: 'Amount',
  actions: 'Actions',
  cellLabel: (column, row) => `${column}, row ${row}`,
  descriptionPlaceholder: 'Item or service',
  addItem: 'Add item',
  removeItem: (row, description) =>
    description.trim() ? `Remove row ${row}: ${description.trim()}` : `Remove row ${row}`,
  empty: 'No items yet. Add one to start.',
  added: (row) => `Row ${row} added`,
  removed: (row) => `Row ${row} removed`,
  invalidNumber: 'Enter a number',
  subtotal: 'Subtotal',
  discount: 'Discount',
  discountType: 'Discount type',
  percentOption: '%',
  tax: (rate) => (rate ? `Tax (${rate})` : 'Tax'),
  taxRate: 'Tax rate',
  total: 'Total',
};

/**
 * Reads a typed decimal. Accepts `1234.5`, `1,234.5` and `1234,5` (a lone
 * comma is a decimal separator). Returns `0` for an empty value and `null`
 * when the text is not a number.
 */
export function parseDecimal(value: string | number): number | null {
  if (typeof value === 'number') return Number.isFinite(value) ? value : null;
  let text = value.trim().replace(/[\s\u00a0\u202f']/g, '');
  if (text === '') return 0;
  if (text.includes(',') && text.includes('.')) text = text.replace(/,/g, '');
  else if (/^-?\d*,\d+$/.test(text)) text = text.replace(',', '.');
  else text = text.replace(/,/g, '');
  if (!/^-?(\d+\.?\d*|\.\d+)$/.test(text)) return null;
  return Number(text);
}

/**
 * Works out row amounts, subtotal, discount, tax and total in minor units.
 * Each row is rounded to the currency's minor unit, the discount applies to
 * the subtotal, and tax applies to what is left. Unparseable values count
 * as zero.
 */
export function computeLineItemTotals(
  items: readonly Pick<LineItem, 'quantity' | 'unitPrice'>[],
  currency: string,
  options: { taxRate?: number | undefined; discount?: LineItemsDiscount | undefined } = {},
): LineItemsTotals {
  const scale = 10 ** currencyDigits(currency);
  const lines = items.map((item) => {
    const q = parseDecimal(item.quantity) ?? 0;
    const p = parseDecimal(item.unitPrice) ?? 0;
    return Math.round(q * p * scale);
  });
  const subtotal = lines.reduce((sum, n) => sum + n, 0);
  const { discount, taxRate } = options;
  let off = 0;
  if (discount && Number.isFinite(discount.value) && discount.value > 0) {
    off =
      discount.type === 'percent'
        ? Math.round((subtotal * Math.min(discount.value, 100)) / 100)
        : Math.round(discount.value * scale);
    off = Math.max(0, Math.min(off, Math.max(subtotal, 0)));
  }
  const taxable = subtotal - off;
  const tax = taxRate && Number.isFinite(taxRate) ? Math.round((taxable * taxRate) / 100) : 0;
  return { lines, subtotal, discount: off, tax, total: taxable + tax };
}

let rowSeed = 0;
/** A new empty row with a unique id. */
export function createLineItem(partial: Partial<LineItem> = {}): LineItem {
  rowSeed += 1;
  return {
    id: `mn-line-item-${Date.now().toString(36)}-${rowSeed}`,
    description: '',
    quantity: '1',
    unitPrice: '',
    ...partial,
  };
}

/** A text input for a decimal that keeps what the person typed ("7.") until it parses. */
function DecimalInput({
  value,
  onValue,
  label,
  disabled,
  className,
}: {
  value: number;
  onValue: (n: number) => void;
  label: string;
  disabled: boolean;
  className?: string;
}) {
  const [draft, setDraft] = useState(String(value));
  useEffect(() => {
    setDraft((d) => (parseDecimal(d) === value ? d : String(value)));
  }, [value]);
  const invalid = parseDecimal(draft) === null;
  return (
    <Input
      size="sm"
      inputMode="decimal"
      aria-label={label}
      value={draft}
      disabled={disabled}
      invalid={invalid}
      className={className}
      onChange={(e) => {
        setDraft(e.target.value);
        const n = parseDecimal(e.target.value);
        if (n !== null) onValue(n);
      }}
    />
  );
}

function TrashGlyph() {
  return (
    <svg
      width={16}
      height={16}
      viewBox="0 0 16 16"
      fill="none"
      stroke="currentColor"
      strokeWidth={1.75}
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden
      focusable={false}
    >
      <path d="M2.5 4h11M6.5 4V2.5h3V4M4 4l.7 9.5h6.6L12 4M6.75 6.5v4.5M9.25 6.5v4.5" />
    </svg>
  );
}

export interface LineItemsEditorProps extends Omit<HTMLAttributes<HTMLDivElement>, 'onChange'> {
  /** The rows. The editor is controlled: every edit calls `onChange` with a new array. */
  items: readonly LineItem[];
  /** Called with the full new array after any edit, add or remove. */
  onChange: (items: LineItem[]) => void;
  /** ISO 4217 code used for amounts and the unit price heading, e.g. `USD`. */
  currency: string;
  /** BCP 47 locale for amounts and percentages. Default: the runtime locale. */
  locale?: string | undefined;
  /** Tax as a percentage of the discounted subtotal. Omit for no tax row. */
  taxRate?: number | undefined;
  /** Makes the tax rate editable. Called with the new percentage. */
  onTaxRateChange?: ((rate: number) => void) | undefined;
  /** Offer preset rates in a select instead of a free number input. */
  taxRateOptions?: readonly LineItemsTaxRateOption[] | undefined;
  /** Discount on the subtotal. Omit for no discount row. */
  discount?: LineItemsDiscount | undefined;
  /** Makes the discount editable (amount and type). */
  onDiscountChange?: ((discount: LineItemsDiscount) => void) | undefined;
  /** Show the subtotal, discount, tax and total under the table. Default `true`. */
  showTotals?: boolean | undefined;
  /** Rows that cannot be removed below this count. Default `0`. */
  minItems?: number | undefined;
  /** Builds the row added by "Add item". Default: `createLineItem()`. */
  createItem?: (() => LineItem) | undefined;
  /** Marks the description of the group as required (adds an asterisk). */
  required?: boolean | undefined;
  /** Error for the whole list, e.g. "Add at least one item". Shown under the table. */
  error?: ReactNode;
  /** Show values as text with no inputs or buttons, for a sent invoice. */
  readOnly?: boolean | undefined;
  /** Disables every control. */
  disabled?: boolean | undefined;
  /** Replace any English text. */
  messages?: Partial<LineItemsEditorMessages> | undefined;
}

/**
 * Editable rows of description, quantity and unit price for invoices, quotes
 * and proposals, with each row's amount and an optional discount, tax and
 * total formatted by Money. Amounts are summed in minor units so totals add
 * up exactly.
 */
export const LineItemsEditor = forwardRef<HTMLDivElement, LineItemsEditorProps>(
  function LineItemsEditor(
    {
      items,
      onChange,
      currency,
      locale,
      taxRate,
      onTaxRateChange,
      taxRateOptions,
      discount,
      onDiscountChange,
      showTotals = true,
      minItems = 0,
      createItem = createLineItem,
      required = false,
      error,
      readOnly = false,
      disabled = false,
      messages: messagesProp,
      className,
      ...rest
    },
    ref,
  ) {
    const m = { ...DEFAULT_MESSAGES, ...messagesProp };
    const baseId = useId();
    const labelId = `${baseId}-label`;
    const errorId = `${baseId}-error`;
    const inputs = useRef(new Map<string, HTMLInputElement>());
    const addRef = useRef<HTMLButtonElement>(null);
    const [focusTarget, setFocusTarget] = useState<string | 'add' | null>(null);
    const [announcement, setAnnouncement] = useState('');

    useEffect(() => {
      if (!focusTarget) return;
      if (focusTarget === 'add') addRef.current?.focus();
      else inputs.current.get(focusTarget)?.focus();
      setFocusTarget(null);
    }, [focusTarget, items]);

    const totals = computeLineItemTotals(items, currency, { taxRate, discount });
    const money = (minor: number, extra?: { negative?: boolean }) => (
      <Money
        amount={extra?.negative && minor !== 0 ? -minor : minor}
        currency={currency}
        minorUnits
        {...(locale !== undefined && { locale })}
      />
    );
    const editable = !readOnly;
    const interactive = editable && !disabled;

    const update = (index: number, patch: Partial<LineItem>) =>
      onChange(items.map((item, i) => (i === index ? { ...item, ...patch } : item)));

    const add = () => {
      const next = createItem();
      onChange([...items, next]);
      setFocusTarget(next.id);
      setAnnouncement(m.added(items.length + 1));
    };

    const remove = (index: number) => {
      const neighbour = items[index + 1] ?? items[index - 1];
      onChange(items.filter((_, i) => i !== index));
      setFocusTarget(neighbour ? neighbour.id : 'add');
      setAnnouncement(m.removed(index + 1));
    };

    const percent = (value: number) =>
      new Intl.NumberFormat(locale, { style: 'percent', maximumFractionDigits: 3 }).format(
        value / 100,
      );

    const onRowKeyDown = (event: KeyboardEvent<HTMLInputElement>, index: number) => {
      // Enter in the last row adds a row, like a spreadsheet; elsewhere it moves down.
      if (event.key !== 'Enter' || event.nativeEvent.isComposing) return;
      event.preventDefault();
      const below = items[index + 1];
      if (below) inputs.current.get(below.id)?.focus();
      else if (interactive) add();
    };

    const quantityHeading = m.quantity;
    const priceHeading = m.unitPrice(currency);
    const removable = items.length > minItems;

    return (
      <div
        ref={ref}
        role="group"
        aria-labelledby={labelId}
        aria-describedby={error ? errorId : undefined}
        {...rest}
        className={cx('mn-line-items-editor', className)}
        data-readonly={readOnly || undefined}
        data-disabled={disabled || undefined}
        data-invalid={error ? '' : undefined}
      >
        <div className="mn-line-items-editor__label" id={labelId}>
          {m.label}
          {required ? (
            <span className="mn-line-items-editor__required" aria-hidden>
              {' *'}
            </span>
          ) : null}
        </div>
        <table className="mn-line-items-editor__table" aria-labelledby={labelId}>
          <thead>
            <tr>
              <th scope="col" className="mn-line-items-editor__col-description">
                {m.description}
              </th>
              <th scope="col" className="mn-line-items-editor__col-number">
                {quantityHeading}
              </th>
              <th scope="col" className="mn-line-items-editor__col-number">
                {priceHeading}
              </th>
              <th scope="col" className="mn-line-items-editor__col-number">
                {m.amount}
              </th>
              {editable ? (
                <th scope="col" className="mn-line-items-editor__col-actions">
                  <VisuallyHidden>{m.actions}</VisuallyHidden>
                </th>
              ) : null}
            </tr>
          </thead>
          <tbody>
            {items.length === 0 ? (
              <tr className="mn-line-items-editor__empty-row">
                <td colSpan={editable ? 5 : 4} className="mn-line-items-editor__empty">
                  {m.empty}
                </td>
              </tr>
            ) : (
              items.map((item, index) => {
                const row = index + 1;
                const qtyInvalid = parseDecimal(item.quantity) === null;
                const priceInvalid = parseDecimal(item.unitPrice) === null;
                return (
                  <tr
                    key={item.id}
                    className="mn-line-items-editor__row"
                    data-invalid={qtyInvalid || priceInvalid || undefined}
                  >
                    <td className="mn-line-items-editor__cell" data-part="description">
                      {editable ? (
                        <Input
                          ref={(el) => {
                            if (el) inputs.current.set(item.id, el);
                            else inputs.current.delete(item.id);
                          }}
                          size="sm"
                          aria-label={m.cellLabel(m.description, row)}
                          placeholder={m.descriptionPlaceholder}
                          value={item.description}
                          disabled={disabled}
                          onChange={(e) => update(index, { description: e.target.value })}
                          onKeyDown={(e) => onRowKeyDown(e, index)}
                        />
                      ) : (
                        item.description
                      )}
                    </td>
                    <td
                      className="mn-line-items-editor__cell"
                      data-part="quantity"
                      data-label={quantityHeading}
                    >
                      {editable ? (
                        <Input
                          size="sm"
                          inputMode="decimal"
                          aria-label={m.cellLabel(quantityHeading, row)}
                          value={String(item.quantity)}
                          disabled={disabled}
                          invalid={qtyInvalid}
                          title={qtyInvalid ? m.invalidNumber : undefined}
                          onChange={(e) => update(index, { quantity: e.target.value })}
                          onKeyDown={(e) => onRowKeyDown(e, index)}
                        />
                      ) : (
                        <span className="mn-line-items-editor__number">
                          {String(item.quantity)}
                        </span>
                      )}
                    </td>
                    <td
                      className="mn-line-items-editor__cell"
                      data-part="unit-price"
                      data-label={priceHeading}
                    >
                      {editable ? (
                        <Input
                          size="sm"
                          inputMode="decimal"
                          aria-label={m.cellLabel(priceHeading, row)}
                          value={String(item.unitPrice)}
                          disabled={disabled}
                          invalid={priceInvalid}
                          title={priceInvalid ? m.invalidNumber : undefined}
                          onChange={(e) => update(index, { unitPrice: e.target.value })}
                          onKeyDown={(e) => onRowKeyDown(e, index)}
                        />
                      ) : (
                        <Money
                          amount={parseDecimal(item.unitPrice) ?? 0}
                          currency={currency}
                          {...(locale !== undefined && { locale })}
                        />
                      )}
                    </td>
                    <td
                      className="mn-line-items-editor__cell"
                      data-part="amount"
                      data-label={m.amount}
                    >
                      {money(totals.lines[index] ?? 0)}
                    </td>
                    {editable ? (
                      <td className="mn-line-items-editor__cell" data-part="actions">
                        <IconButton
                          size="sm"
                          label={m.removeItem(row, item.description)}
                          icon={<TrashGlyph />}
                          disabled={disabled || !removable}
                          onClick={() => remove(index)}
                        />
                      </td>
                    ) : null}
                  </tr>
                );
              })
            )}
          </tbody>
        </table>

        {editable ? (
          <div className="mn-line-items-editor__toolbar">
            <Button
              ref={addRef}
              size="sm"
              leadingIcon={<PlusGlyph />}
              disabled={disabled}
              onClick={add}
            >
              {m.addItem}
            </Button>
          </div>
        ) : null}

        {error ? (
          <p className="mn-line-items-editor__error" id={errorId} role="alert">
            {error}
          </p>
        ) : null}

        {showTotals ? (
          <dl className="mn-line-items-editor__totals">
            <div className="mn-line-items-editor__total-row">
              <dt>{m.subtotal}</dt>
              <dd>{money(totals.subtotal)}</dd>
            </div>
            {discount !== undefined || (onDiscountChange && interactive) ? (
              <div className="mn-line-items-editor__total-row" data-part="discount">
                <dt>
                  {m.discount}
                  {onDiscountChange && interactive ? (
                    <span className="mn-line-items-editor__adjust">
                      <DecimalInput
                        className="mn-line-items-editor__adjust-input"
                        label={m.discount}
                        value={discount?.value ?? 0}
                        disabled={disabled}
                        onValue={(value) =>
                          onDiscountChange({ type: discount?.type ?? 'percent', value })
                        }
                      />
                      <NativeSelect
                        size="sm"
                        aria-label={m.discountType}
                        value={discount?.type ?? 'percent'}
                        disabled={disabled}
                        options={[
                          { value: 'percent', label: m.percentOption },
                          { value: 'amount', label: currency },
                        ]}
                        onChange={(e) =>
                          onDiscountChange({
                            type: e.target.value === 'amount' ? 'amount' : 'percent',
                            value: discount?.value ?? 0,
                          })
                        }
                      />
                    </span>
                  ) : discount?.type === 'percent' && discount.value > 0 ? (
                    ` (${percent(discount.value)})`
                  ) : null}
                </dt>
                <dd>{money(totals.discount, { negative: true })}</dd>
              </div>
            ) : null}
            {taxRate !== undefined || (onTaxRateChange && interactive) ? (
              <div className="mn-line-items-editor__total-row" data-part="tax">
                <dt>
                  {onTaxRateChange && interactive ? (
                    <>
                      {m.tax('')}
                      <span className="mn-line-items-editor__adjust">
                        {taxRateOptions ? (
                          <NativeSelect
                            size="sm"
                            aria-label={m.taxRate}
                            value={String(taxRate ?? 0)}
                            disabled={disabled}
                            options={taxRateOptions.map((o) => ({
                              value: String(o.value),
                              label: o.label,
                            }))}
                            onChange={(e) => onTaxRateChange(Number(e.target.value))}
                          />
                        ) : (
                          <>
                            <DecimalInput
                              className="mn-line-items-editor__adjust-input"
                              label={m.taxRate}
                              value={taxRate ?? 0}
                              disabled={disabled}
                              onValue={onTaxRateChange}
                            />
                            <span aria-hidden>%</span>
                          </>
                        )}
                      </span>
                    </>
                  ) : (
                    m.tax(taxRate ? percent(taxRate) : '')
                  )}
                </dt>
                <dd>{money(totals.tax)}</dd>
              </div>
            ) : null}
            <div className="mn-line-items-editor__total-row" data-part="total">
              <dt>{m.total}</dt>
              <dd>{money(totals.total)}</dd>
            </div>
          </dl>
        ) : null}

        <VisuallyHidden aria-live="polite">{announcement}</VisuallyHidden>
      </div>
    );
  },
);
