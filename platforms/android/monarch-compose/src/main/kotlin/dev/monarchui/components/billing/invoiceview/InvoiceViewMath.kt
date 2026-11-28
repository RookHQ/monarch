package dev.monarchui.components.billing.invoiceview

import dev.monarchui.components.billing.money.MonarchMoneyFormat
import java.math.BigDecimal
import java.math.RoundingMode

/** The arithmetic of MonarchInvoiceView: worked-out figures and which total rows show. */
internal object InvoiceViewMath {
    private val Hundred: BigDecimal = BigDecimal(100)

    /**
     * Works out the line amounts, subtotal, discount, tax and total as the web
     * LineItemsEditor does: each line rounded half away from zero to the
     * currency's minor unit, the discount on the subtotal (never more than
     * it), and tax on what is left.
     */
    fun figures(
        items: List<MonarchInvoiceLineItem>,
        currencyCode: String,
        taxRate: BigDecimal?,
        discount: MonarchInvoiceDiscount?,
    ): InvoiceViewFigures {
        val digits = MonarchMoneyFormat.currencyDigits(currencyCode)
        fun round(value: BigDecimal) = value.setScale(digits, RoundingMode.HALF_UP)
        val lines = items.map { round(it.quantity.multiply(it.unitPrice)) }
        val subtotal = lines.fold(round(BigDecimal.ZERO), BigDecimal::add)
        val off = when (discount) {
            is MonarchInvoiceDiscount.Percent ->
                round(subtotal.multiply(discount.percent.min(Hundred)).divide(Hundred))
            is MonarchInvoiceDiscount.Amount -> round(discount.amount)
            null -> round(BigDecimal.ZERO)
        }.max(BigDecimal.ZERO).min(subtotal.max(BigDecimal.ZERO))
        val taxable = subtotal.subtract(off)
        val tax = round(taxRate?.let { taxable.multiply(it).divide(Hundred) } ?: BigDecimal.ZERO)
        return InvoiceViewFigures(lines, subtotal, off, tax, taxable.add(tax))
    }

    /** Whether the discount row shows: the given discount, or the worked-out one, is not zero. */
    fun discountShown(totals: MonarchInvoiceTotals, figures: InvoiceViewFigures): Boolean =
        (totals.discount ?: figures.discount).signum() != 0

    /** Whether the tax row shows: a tax amount is given or the invoice has a tax rate. */
    fun taxShown(totals: MonarchInvoiceTotals, taxRate: BigDecimal?): Boolean =
        totals.tax != null || taxRate != null

    /** Whether the tax label carries the rate: only when the tax is worked out from it. */
    fun rateShown(totals: MonarchInvoiceTotals, taxRate: BigDecimal?): Boolean =
        totals.tax == null && taxRate != null
}

/** The figures of an invoice worked out from its lines, in major units at the currency's scale. */
internal class InvoiceViewFigures(
    val lines: List<BigDecimal>,
    val subtotal: BigDecimal,
    val discount: BigDecimal,
    val tax: BigDecimal,
    val total: BigDecimal,
)
