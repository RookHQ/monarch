@file:OptIn(ExperimentalMonarchApi::class)

package com.rookhq.monarch.sample.sections.billing

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.rookhq.monarch.ExperimentalMonarchApi
import com.rookhq.monarch.components.billing.lineitemseditor.MonarchLineItem
import com.rookhq.monarch.components.billing.lineitemseditor.MonarchLineItemsDiscount
import com.rookhq.monarch.components.billing.lineitemseditor.MonarchLineItemsDiscountKind
import com.rookhq.monarch.components.billing.lineitemseditor.MonarchLineItemsEditor
import com.rookhq.monarch.components.billing.lineitemseditor.MonarchLineItemsTaxRateOption
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import java.math.BigDecimal

/** The catalogue page of LineItemsEditor. */
object MonarchLineItemsEditorSection : CataloguePage {
    override val title = "LineItemsEditor"

    private val rates = listOf(
        MonarchLineItemsTaxRateOption(BigDecimal.ZERO, "No tax"),
        MonarchLineItemsTaxRateOption(BigDecimal(5), "GST 5%"),
        MonarchLineItemsTaxRateOption(BigDecimal(18), "GST 18%"),
    )

    private val firstRows = listOf(
        MonarchLineItem("r1", "Discovery workshop", BigDecimal.ONE, BigDecimal(800)),
        MonarchLineItem("r2", "Design hours", BigDecimal(12), BigDecimal(95)),
    )
    private val startRate = BigDecimal(18)
    private val startDiscount = MonarchLineItemsDiscount(MonarchLineItemsDiscountKind.Percent, BigDecimal(5))

    @Composable
    override fun Content() {
        // The rows hold BigDecimal values, which are not saveable, so they live as long as the page.
        var items by remember { mutableStateOf(firstRows) }
        var taxRate by remember { mutableStateOf(startRate) }
        var discount by remember { mutableStateOf(startDiscount) }
        MonarchLineItemsEditor(
            items = items,
            onItemsChange = { items = it },
            currencyCode = "USD",
            taxRate = taxRate,
            onTaxRateChange = { taxRate = it },
            taxRateOptions = rates,
            discount = discount,
            onDiscountChange = { discount = it },
            error = "Add at least one item".takeIf { items.isEmpty() },
            required = true,
        )
        CatalogueCaption("A sent invoice, read-only")
        MonarchLineItemsEditor(
            items = items,
            onItemsChange = {},
            currencyCode = "USD",
            taxRate = taxRate,
            discount = discount,
            readOnly = true,
        )
    }
}
