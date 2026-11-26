@file:OptIn(ExperimentalMonarchApi::class)

package com.rookhq.monarch.components.billing.lineitemseditor

import androidx.compose.runtime.Composable
import com.rookhq.monarch.ExperimentalMonarchApi
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame
import java.math.BigDecimal

private val rows = listOf(
    MonarchLineItem("r1", "Discovery workshop", BigDecimal.ONE, BigDecimal(800)),
    MonarchLineItem("r2", "Design hours", BigDecimal(12), BigDecimal(95)),
)

@MonarchPreviews
@Composable
internal fun MonarchLineItemsEditorPreview() {
    PreviewFrame {
        MonarchLineItemsEditor(
            items = rows,
            onItemsChange = {},
            currencyCode = "USD",
            taxRate = BigDecimal(18),
            onTaxRateChange = {},
            discount = MonarchLineItemsDiscount(MonarchLineItemsDiscountKind.Percent, BigDecimal(5)),
            onDiscountChange = {},
            required = true,
        )
    }
}

@MonarchPreviews
@Composable
internal fun MonarchLineItemsEditorReadOnlyPreview() {
    PreviewFrame {
        MonarchLineItemsEditor(
            items = rows,
            onItemsChange = {},
            currencyCode = "EUR",
            taxRate = BigDecimal(19),
            readOnly = true,
        )
    }
}
