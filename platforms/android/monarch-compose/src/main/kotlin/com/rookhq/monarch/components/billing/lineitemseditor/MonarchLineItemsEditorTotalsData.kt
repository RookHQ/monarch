package com.rookhq.monarch.components.billing.lineitemseditor

import com.rookhq.monarch.components.billing.money.MonarchMoneyCurrencyDisplay
import com.rookhq.monarch.components.billing.money.MonarchMoneyFormat
import java.math.BigDecimal
import java.util.Locale

/**
 * What the totals of the editor show and change.
 *
 * @property totals the worked-out figures.
 * @property currencyCode the currency of the document.
 * @property locale the locale amounts and rates are written in.
 * @property taxRate the tax rate, or `null` for no tax row (unless it can be edited).
 * @property onTaxRateChange edits the rate; `null` when it cannot be edited now.
 * @property taxRateOptions preset rates for the menu; empty for a typed rate.
 * @property discount the discount, or `null` for no discount row (unless it can be edited).
 * @property onDiscountChange edits the discount; `null` when it cannot be edited now.
 */
@Suppress("LongParameterList")
internal class MonarchLineItemsEditorTotalsData(
    val totals: MonarchLineItemsTotals,
    val currencyCode: String,
    val locale: Locale,
    val taxRate: BigDecimal?,
    val onTaxRateChange: ((BigDecimal) -> Unit)?,
    val taxRateOptions: List<MonarchLineItemsTaxRateOption>,
    val discount: MonarchLineItemsDiscount?,
    val onDiscountChange: ((MonarchLineItemsDiscount) -> Unit)?,
) {
    /** [minor] units written in the currency: "$2,257.13". */
    fun write(minor: Long): String = MonarchMoneyFormat.format(
        MonarchMoneyFormat.amountOfMinorUnits(minor, currencyCode),
        currencyCode,
        locale,
        currencyDisplay = MonarchMoneyCurrencyDisplay.NarrowSymbol,
    )
}
