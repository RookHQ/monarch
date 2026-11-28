package dev.monarchui.components.billing.lineitemseditor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.actions.iconbutton.MonarchIconButton
import dev.monarchui.components.billing.money.MonarchMoney
import dev.monarchui.components.billing.money.MonarchMoneyCurrencyDisplay
import dev.monarchui.components.billing.money.MonarchMoneyFormat
import dev.monarchui.components.forms.currencyinput.MonarchCurrencyInput
import dev.monarchui.components.forms.numberinput.MonarchNumberInput
import dev.monarchui.components.forms.textfield.MonarchTextField
import dev.monarchui.foundation.MonarchTextStyle
import dev.monarchui.internal.MonarchDefaultIcons
import dev.monarchui.theme.MonarchTheme
import kotlinx.coroutines.launch

/**
 * One editable row: a header with the item number, its amount and a remove button, the description field, then the
 * quantity and unit price fields side by side (stacked at large font sizes). With [onRemove] the row can also be
 * swiped toward the start to remove it, with the Material 3 `SwipeToDismissBox`.
 */
@Composable
@Suppress("LongParameterList")
internal fun MonarchLineItemsEditorEditableRow(
    row: MonarchLineItemsEditorRowData,
    enabled: Boolean,
    stacked: Boolean,
    text: MonarchLineItemsEditorText,
    colors: MonarchLineItemsEditorColors,
    onChange: (MonarchLineItem) -> Unit,
    onRemove: (() -> Unit)?,
) {
    val content = @Composable { EditableRowContent(row, enabled, stacked, text, colors, onChange, onRemove) }
    if (onRemove == null) {
        content()
        return
    }
    val state = rememberSwipeToDismissBoxState()
    val scope = rememberCoroutineScope()
    SwipeToDismissBox(
        state = state,
        backgroundContent = {
            if (state.dismissDirection == SwipeToDismissBoxValue.EndToStart) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(colors.swipe)
                        .padding(horizontal = MonarchLineItemsEditorTokens.SwipePadding)
                        // The row's remove button offers the same action, so the swipe background is not read.
                        .clearAndSetSemantics { },
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    Text(text = text.remove, style = MonarchTheme.typography.label, color = colors.onSwipe)
                }
            }
        },
        enableDismissFromStartToEnd = false,
        onDismiss = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) onRemove()
            scope.launch { state.reset() }
        },
    ) {
        content()
    }
}

@Composable
@Suppress("LongParameterList")
private fun EditableRowContent(
    row: MonarchLineItemsEditorRowData,
    enabled: Boolean,
    stacked: Boolean,
    text: MonarchLineItemsEditorText,
    colors: MonarchLineItemsEditorColors,
    onChange: (MonarchLineItem) -> Unit,
    onRemove: (() -> Unit)?,
) {
    val item = row.item
    val digits = MonarchMoneyFormat.currencyDigits(row.currencyCode)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MonarchTheme.colors.bgSurface)
            .padding(vertical = MonarchLineItemsEditorTokens.RowPadding),
        verticalArrangement = Arrangement.spacedBy(MonarchLineItemsEditorTokens.FieldGap),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(MonarchLineItemsEditorTokens.FieldGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RowHeader(row, text, colors, Modifier.weight(1f))
            val removeName = text.removeItem(row.number, item.description)
            MonarchIconButton(
                label = text.remove,
                onClick = { onRemove?.invoke() },
                modifier = Modifier.semantics { contentDescription = removeName },
                size = MonarchButtonSize.Sm,
                enabled = enabled && onRemove != null,
            ) {
                MonarchDefaultIcons.Trash()
            }
        }
        MonarchTextField(
            value = item.description,
            onValueChange = { onChange(item.edited(description = it)) },
            modifier = Modifier.fillMaxWidth(),
            label = text.description,
            placeholder = text.descriptionPlaceholder,
            enabled = enabled,
            singleLine = false,
        )
        val quantity = @Composable { modifier: Modifier ->
            MonarchNumberInput(
                value = item.quantity?.toDouble(),
                onValueChange = { onChange(item.edited(quantity = MonarchLineItemsEditorTokens.decimal(it))) },
                modifier = modifier,
                label = text.quantity,
                enabled = enabled,
                showSteppers = false,
            )
        }
        val price = @Composable { modifier: Modifier ->
            MonarchCurrencyInput(
                minorUnits = MonarchLineItemsEditorTokens.toMinor(item.unitPrice, digits),
                currencyCode = row.currencyCode,
                currencyDisplay = MonarchMoneyCurrencyDisplay.NarrowSymbol,
                onMinorUnitsChange = {
                    onChange(item.edited(unitPrice = MonarchLineItemsEditorTokens.fromMinor(it, digits)))
                },
                modifier = modifier,
                label = text.unitPrice,
                enabled = enabled,
            )
        }
        if (stacked) {
            quantity(Modifier.fillMaxWidth())
            price(Modifier.fillMaxWidth())
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(MonarchLineItemsEditorTokens.FieldGap)) {
                quantity(Modifier.weight(1f))
                price(Modifier.weight(1f))
            }
        }
    }
}

/** "Item 2" and the row's amount, read as one heading: "Item 2, Amount $1,140.00". */
@Composable
private fun RowHeader(
    row: MonarchLineItemsEditorRowData,
    text: MonarchLineItemsEditorText,
    colors: MonarchLineItemsEditorColors,
    modifier: Modifier,
) {
    val title = text.item(row.number)
    val spoken = text.pair(title, text.value(text.amount, row.amountText))
    Row(
        modifier = modifier.clearAndSetSemantics {
            heading()
            contentDescription = spoken
        },
        horizontalArrangement = Arrangement.spacedBy(MonarchLineItemsEditorTokens.FieldGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = MonarchTheme.typography.label,
            color = colors.itemHeader,
        )
        MonarchMoney(
            minorUnits = row.amount,
            currencyCode = row.currencyCode,
            locale = row.locale,
            currencyDisplay = MonarchMoneyCurrencyDisplay.NarrowSymbol,
        )
    }
}

/** A row of a sent document: the description and "12 × $95.00", with the amount at the end, read as one. */
@Composable
internal fun MonarchLineItemsEditorReadOnlyRow(
    row: MonarchLineItemsEditorRowData,
    text: MonarchLineItemsEditorText,
    colors: MonarchLineItemsEditorColors,
) {
    val item = row.item
    val price = MonarchMoneyFormat.format(
        item.unitPrice ?: java.math.BigDecimal.ZERO,
        row.currencyCode,
        row.locale,
        currencyDisplay = MonarchMoneyCurrencyDisplay.NarrowSymbol,
    )
    val figures = text.quantityTimesPrice(MonarchLineItemsEditorTokens.quantity(item.quantity, row.locale), price)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = MonarchLineItemsEditorTokens.RowPadding)
            .semantics(mergeDescendants = true) { },
        horizontalArrangement = Arrangement.spacedBy(MonarchLineItemsEditorTokens.FieldGap),
        verticalAlignment = Alignment.Top,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            if (item.description.isNotBlank()) {
                Text(text = item.description, style = MonarchTheme.typography.body, color = colors.heading)
            }
            Text(text = figures, style = MonarchTheme.typography.caption, color = colors.figures)
        }
        MonarchMoney(
            minorUnits = row.amount,
            currencyCode = row.currencyCode,
            locale = row.locale,
            currencyDisplay = MonarchMoneyCurrencyDisplay.NarrowSymbol,
            style = MonarchTextStyle.Body,
            textAlign = TextAlign.End,
        )
    }
}
