package com.rookhq.monarch.components.billing.lineitemseditor

import androidx.compose.runtime.Immutable
import com.rookhq.monarch.components.billing.money.MonarchMoneyFormat
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * The figures of a list of line items in the currency's minor units (cents, paise), worked out exactly in
 * decimal arithmetic, as `MonarchLineItemsEditor` shows them and an app saves them.
 *
 * Each row's amount (quantity times unit price) is rounded half away from zero to the minor unit, so the subtotal
 * is the sum of the rows shown. The discount comes off the subtotal (never more than it, nothing off a negative
 * subtotal) and the tax is the rate of what is left, rounded the same way. Empty quantities and prices count as
 * zero.
 *
 * @param items the rows.
 * @param currencyCode the ISO 4217 code: sets the minor unit (USD 2, JPY 0, KWD 3).
 * @param taxRate the tax as a percentage of the discounted subtotal, or `null` for none.
 * @param discount the discount on the subtotal, or `null` for none.
 */
@Immutable
public class MonarchLineItemsTotals(
    items: List<MonarchLineItem>,
    currencyCode: String,
    taxRate: BigDecimal? = null,
    discount: MonarchLineItemsDiscount? = null,
) {
    /** Each row's amount, in row order. */
    public val lines: List<Long>

    /** The sum of the rows. */
    public val subtotal: Long

    /** What the discount takes off the subtotal, from zero to the subtotal. */
    public val discount: Long

    /** The tax on the discounted subtotal. */
    public val tax: Long

    /** The discounted subtotal plus the tax. */
    public val total: Long

    init {
        val digits = MonarchMoneyFormat.currencyDigits(currencyCode)
        fun minor(major: BigDecimal): BigDecimal = major.movePointRight(digits).setScale(0, RoundingMode.HALF_UP)
        val amounts = items.map { minor((it.quantity ?: BigDecimal.ZERO).multiply(it.unitPrice ?: BigDecimal.ZERO)) }
        val sum = amounts.fold(BigDecimal.ZERO, BigDecimal::add)
        val off = when {
            discount == null || discount.value.signum() <= 0 -> BigDecimal.ZERO
            discount.kind == MonarchLineItemsDiscountKind.Percent ->
                sum.multiply(discount.value.min(HUNDRED)).movePointLeft(2).setScale(0, RoundingMode.HALF_UP)
            else -> minor(discount.value)
        }.max(BigDecimal.ZERO).min(sum.max(BigDecimal.ZERO))
        val taxable = sum.subtract(off)
        val taxed = taxRate?.let { taxable.multiply(it).movePointLeft(2).setScale(0, RoundingMode.HALF_UP) }
            ?: BigDecimal.ZERO
        lines = amounts.map { it.longValueExact() }
        subtotal = sum.longValueExact()
        this.discount = off.longValueExact()
        tax = taxed.longValueExact()
        total = taxable.add(taxed).longValueExact()
    }

    private companion object {
        val HUNDRED: BigDecimal = BigDecimal(100)
    }
}
