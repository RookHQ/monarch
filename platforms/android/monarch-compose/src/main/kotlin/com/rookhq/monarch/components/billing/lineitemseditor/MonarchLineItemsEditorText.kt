package com.rookhq.monarch.components.billing.lineitemseditor

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.rookhq.monarch.R
import com.rookhq.monarch.internal.monarchString

/** The built-in strings of a line items editor, with the app's replacements, resolved for one currency. */
@Suppress("LongParameterList")
internal class MonarchLineItemsEditorText(
    val label: String,
    val item: (Int) -> String,
    val description: String,
    val quantity: String,
    val unitPrice: String,
    val amount: String,
    val descriptionPlaceholder: String,
    val addItem: String,
    val remove: String,
    val removeItem: (Int, String) -> String,
    val empty: String,
    val added: (Int) -> String,
    val removed: (Int) -> String,
    val subtotal: String,
    val discount: String,
    val discountType: String,
    val percentOption: String,
    val tax: (String) -> String,
    val taxRate: String,
    val total: String,
    val required: String,
    val quantityTimesPrice: (String, String) -> String,
    val pair: (String, String) -> String,
    val value: (String, String) -> String,
    val withRate: (String, String) -> String,
)

/** The strings of [labels] for [currencyCode], built-in ones where the app gives none. */
@Composable
internal fun lineItemsEditorText(
    labels: MonarchLineItemsEditorLabels,
    currencyCode: String,
): MonarchLineItemsEditorText {
    val item = stringResource(R.string.monarch_lineitemseditor_item)
    val unitPrice = stringResource(R.string.monarch_lineitemseditor_unit_price)
    val removeItem = stringResource(R.string.monarch_lineitemseditor_remove_item)
    val removeUntitled = stringResource(R.string.monarch_lineitemseditor_remove_item_untitled)
    val added = stringResource(R.string.monarch_lineitemseditor_added)
    val removed = stringResource(R.string.monarch_lineitemseditor_removed)
    val taxWithRate = stringResource(R.string.monarch_lineitemseditor_tax_rate_title)
    val tax = stringResource(R.string.monarch_lineitemseditor_tax)
    val times = stringResource(R.string.monarch_lineitemseditor_quantity_times_price)
    val pair = stringResource(R.string.monarch_lineitemseditor_pair)
    val value = stringResource(R.string.monarch_lineitemseditor_value)
    val withRate = stringResource(R.string.monarch_lineitemseditor_with_rate)
    return MonarchLineItemsEditorText(
        label = monarchString(labels.label, R.string.monarch_lineitemseditor_label),
        item = labels.item ?: { item.format(it) },
        description = monarchString(labels.description, R.string.monarch_lineitemseditor_description),
        quantity = monarchString(labels.quantity, R.string.monarch_lineitemseditor_quantity),
        unitPrice = labels.unitPrice?.invoke(currencyCode) ?: unitPrice.format(currencyCode),
        amount = monarchString(labels.amount, R.string.monarch_lineitemseditor_amount),
        descriptionPlaceholder = monarchString(
            labels.descriptionPlaceholder,
            R.string.monarch_lineitemseditor_description_placeholder,
        ),
        addItem = monarchString(labels.addItem, R.string.monarch_lineitemseditor_add_item),
        remove = monarchString(labels.remove, R.string.monarch_lineitemseditor_remove),
        removeItem = labels.removeItem ?: { number, description ->
            if (description.isBlank()) removeUntitled.format(number) else removeItem.format(number, description.trim())
        },
        empty = monarchString(labels.empty, R.string.monarch_lineitemseditor_empty),
        added = labels.added ?: { added.format(it) },
        removed = labels.removed ?: { removed.format(it) },
        subtotal = monarchString(labels.subtotal, R.string.monarch_lineitemseditor_subtotal),
        discount = monarchString(labels.discount, R.string.monarch_lineitemseditor_discount),
        discountType = monarchString(labels.discountType, R.string.monarch_lineitemseditor_discount_type),
        percentOption = monarchString(labels.percentOption, R.string.monarch_lineitemseditor_percent_option),
        tax = labels.tax ?: { rate -> if (rate.isEmpty()) tax else taxWithRate.format(rate) },
        taxRate = monarchString(labels.taxRate, R.string.monarch_lineitemseditor_tax_rate),
        total = monarchString(labels.total, R.string.monarch_lineitemseditor_total),
        required = monarchString(labels.required, R.string.monarch_lineitemseditor_required),
        quantityTimesPrice = labels.quantityTimesPrice ?: { quantity, price -> times.format(quantity, price) },
        pair = { first, second -> pair.format(first, second) },
        value = { name, amount -> value.format(name, amount) },
        withRate = { name, rate -> withRate.format(name, rate) },
    )
}
