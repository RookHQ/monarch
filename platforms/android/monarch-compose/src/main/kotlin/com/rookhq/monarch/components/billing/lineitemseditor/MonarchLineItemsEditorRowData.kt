package com.rookhq.monarch.components.billing.lineitemseditor

import com.rookhq.monarch.components.billing.money.MonarchMoneyCurrencyDisplay
import com.rookhq.monarch.components.billing.money.MonarchMoneyFormat
import java.util.Locale

/**
 * What one row of the editor needs.
 *
 * @property item the row.
 * @property number its 1-based number.
 * @property amount its amount in minor units.
 * @property currencyCode the currency of the document.
 * @property locale the locale amounts and quantities are written in.
 */
internal class MonarchLineItemsEditorRowData(
    val item: MonarchLineItem,
    val number: Int,
    val amount: Long,
    val currencyCode: String,
    val locale: Locale,
) {
    /** The amount as written: "$1,140.00". */
    val amountText: String
        get() = MonarchMoneyFormat.format(
            MonarchMoneyFormat.amountOfMinorUnits(amount, currencyCode),
            currencyCode,
            locale,
            currencyDisplay = MonarchMoneyCurrencyDisplay.NarrowSymbol,
        )
}
