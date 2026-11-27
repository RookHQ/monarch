package com.rookhq.monarch.components.billing.lineitemseditor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.rookhq.monarch.ExperimentalMonarchApi
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.layout.divider.MonarchDivider
import com.rookhq.monarch.components.layout.divider.MonarchDividerVariant
import com.rookhq.monarch.internal.MonarchDefaultIcons
import com.rookhq.monarch.internal.MonarchLiveStatus
import com.rookhq.monarch.theme.MonarchTheme
import java.math.BigDecimal

/**
 * Editable billable rows of an invoice, quote or proposal (description, quantity and unit price), each with its
 * amount, and an optional discount, tax and total worked out exactly in decimal arithmetic.
 *
 * Built by Monarch from Monarch fields in a `Column` that sits in the app's own scrolling form: a row is a header
 * ("Item 2" and its amount, read as one heading) with a Remove button, a `MonarchTextField` for the description,
 * and a `MonarchNumberInput` for the quantity beside a `MonarchCurrencyInput` for the unit price (stacked at large
 * font sizes). A row can also be swiped toward the start to remove it, with the Material 3 `SwipeToDismissBox`.
 * The discount kind is a Material 3 segmented button row and preset tax rates are a `MonarchSelect` menu. Totals
 * are `MonarchLineItemsTotals`: each row rounded to the currency's minor unit, the discount on the subtotal and the
 * tax on what is left, so the total equals the rows shown. Adding and removing a row is announced politely. With
 * [readOnly] the rows are text ("12 × $95.00") for a sent document.
 *
 * The editor is controlled: every edit, addition and removal calls [onItemsChange] with a new list. Numbers are
 * read when a field loses focus or on Done, as `MonarchNumberInput` does.
 *
 * @param items the rows, each with a unique, stable id.
 * @param onItemsChange called with the whole new list after an edit, an addition or a removal.
 * @param currencyCode the ISO 4217 code of the document: the symbol, the unit price heading and the minor unit
 * (USD 2, JPY 0, KWD 3). The editor never converts.
 * @param modifier the modifier of the editor.
 * @param taxRate the tax as a percentage of the discounted subtotal (18 for 18%). `null` for no tax row.
 * @param onTaxRateChange makes the tax rate editable in the totals: called with the new percentage.
 * @param taxRateOptions preset rates offered in a menu when the rate is editable. Empty for a typed percentage.
 * @param discount a percentage or an amount off the subtotal, capped at it. `null` for no discount row.
 * @param onDiscountChange makes the discount's value and kind editable in the totals.
 * @param error an error for the whole list, such as "Add at least one item", shown under the rows and announced.
 * @param minimumItems rows cannot be removed below this count.
 * @param required marks the heading with an asterisk, read as "Required".
 * @param readOnly shows the rows as text without fields, buttons or swipe, and the rates in the titles.
 * @param enabled whether anything can be changed. When off, the fields and buttons are disabled and the rate
 * editors are not shown.
 * @param showTotals whether the subtotal, discount, tax and total are shown.
 * @param labels replacements for every built-in string.
 * @param makeItem builds the row added by Add item. `null` adds an empty row of one unit with a random id.
 */
@ExperimentalMonarchApi
@Composable
@Suppress("CyclomaticComplexMethod")
public fun MonarchLineItemsEditor(
    items: List<MonarchLineItem>,
    onItemsChange: (List<MonarchLineItem>) -> Unit,
    currencyCode: String,
    modifier: Modifier = Modifier,
    taxRate: BigDecimal? = null,
    onTaxRateChange: ((BigDecimal) -> Unit)? = null,
    taxRateOptions: List<MonarchLineItemsTaxRateOption> = emptyList(),
    discount: MonarchLineItemsDiscount? = null,
    onDiscountChange: ((MonarchLineItemsDiscount) -> Unit)? = null,
    error: String? = null,
    minimumItems: Int = 0,
    required: Boolean = false,
    readOnly: Boolean = false,
    enabled: Boolean = true,
    showTotals: Boolean = true,
    labels: MonarchLineItemsEditorLabels = MonarchLineItemsEditorLabels(),
    makeItem: (() -> MonarchLineItem)? = null,
) {
    val text = lineItemsEditorText(labels, currencyCode)
    val colors = MonarchLineItemsEditorTokens.colors(MonarchTheme.colors)
    val locale = LocalConfiguration.current.locales[0]
    val fontStacked = MonarchLineItemsEditorTokens.stacks(LocalDensity.current.fontScale)
    val interactive = enabled && !readOnly
    val totals = remember(items, currencyCode, taxRate, discount) {
        MonarchLineItemsTotals(items, currencyCode, taxRate, discount)
    }
    var announcement by rememberSaveable { mutableStateOf("") }
    val removable = interactive && MonarchLineItemsEditorTokens.removable(items.size, minimumItems)
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        // A table that would squeeze its fields until their text is cut off gives way to one card per item.
        val stacked = fontStacked || maxWidth < MonarchLineItemsEditorTokens.TableMinWidth
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(MonarchLineItemsEditorTokens.SectionGap),
        ) {
            Heading(text, colors, required)
            if (!readOnly && !stacked && items.isNotEmpty()) {
                MonarchLineItemsEditorTable(
                    items = items,
                    lines = totals.lines,
                    currencyCode = currencyCode,
                    locale = locale,
                    enabled = enabled,
                    removable = removable,
                    text = text,
                    colors = colors,
                    onChange = { next -> onItemsChange(items.map { if (it.id == next.id) next else it }) },
                    onRemove = { index ->
                        onItemsChange(items.filterIndexed { i, _ -> i != index })
                        announcement = text.removed(index + 1)
                    },
                )
            } else Column(modifier = Modifier.fillMaxWidth()) {
                if (items.isEmpty()) {
                    Text(text = text.empty, style = MonarchTheme.typography.body, color = colors.figures)
                }
                items.forEachIndexed { index, item ->
                    key(item.id) {
                        if (index > 0) {
                            MonarchDivider(variant = MonarchDividerVariant.Subtle)
                        }
                        val row =
                            MonarchLineItemsEditorRowData(item, index + 1, totals.lines[index], currencyCode, locale)
                        if (readOnly) {
                            MonarchLineItemsEditorReadOnlyRow(row, text, colors)
                        } else {
                            MonarchLineItemsEditorEditableRow(
                                row = row,
                                enabled = enabled,
                                stacked = stacked,
                                text = text,
                                colors = colors,
                                onChange = { next -> onItemsChange(items.map { if (it.id == next.id) next else it }) },
                                onRemove = {
                                    onItemsChange(items.filterNot { it.id == item.id })
                                    announcement = text.removed(index + 1)
                                }.takeIf { removable },
                            )
                        }
                    }
                }
            }
            if (!readOnly) {
                MonarchButton(
                    text = text.addItem,
                    onClick = {
                        onItemsChange(items + (makeItem?.invoke() ?: MonarchLineItem()))
                        announcement = text.added(items.size + 1)
                    },
                    size = MonarchButtonSize.Sm,
                    leadingIcon = MonarchDefaultIcons.Plus,
                    enabled = enabled,
                )
            }
            if (error != null) {
                Text(
                    text = error,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    style = MonarchTheme.typography.caption,
                    color = colors.error,
                )
            }
            if (showTotals) {
                MonarchLineItemsEditorTotals(
                    data = MonarchLineItemsEditorTotalsData(
                        totals = totals,
                        currencyCode = currencyCode,
                        locale = locale,
                        taxRate = taxRate,
                        onTaxRateChange = onTaxRateChange.takeIf { interactive },
                        taxRateOptions = taxRateOptions,
                        discount = discount,
                        onDiscountChange = onDiscountChange.takeIf { interactive },
                    ),
                    stacked = fontStacked,
                    text = text,
                )
            }
            // Additions and removals are announced through this polite live region, present from the start.
            MonarchLiveStatus(text = announcement.takeIf { it.isNotEmpty() }, visible = false)
        }
    }
}

/** "Line items" with the required marker, read as one heading: "Line items, Required". */
@Composable
private fun Heading(text: MonarchLineItemsEditorText, colors: MonarchLineItemsEditorColors, required: Boolean) {
    val spoken = if (required) text.pair(text.label, text.required) else text.label
    Row(
        modifier = Modifier.clearAndSetSemantics {
            heading()
            contentDescription = spoken
        },
    ) {
        Text(text = text.label, style = MonarchTheme.typography.bodyStrong, color = colors.heading)
        if (required) {
            Text(text = RequiredMark, style = MonarchTheme.typography.bodyStrong, color = colors.required)
        }
    }
}

private const val RequiredMark = " *"
