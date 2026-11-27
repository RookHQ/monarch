package com.rookhq.monarch.components.billing.lineitemseditor

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.accessibility.MonarchTouchTarget
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.actions.iconbutton.MonarchIconButton
import com.rookhq.monarch.components.billing.money.MonarchMoney
import com.rookhq.monarch.components.billing.money.MonarchMoneyCurrencyDisplay
import com.rookhq.monarch.components.billing.money.MonarchMoneyFormat
import com.rookhq.monarch.components.forms.currencyinput.MonarchCurrencyInput
import com.rookhq.monarch.components.forms.numberinput.MonarchNumberInput
import com.rookhq.monarch.components.forms.textfield.MonarchTextField
import com.rookhq.monarch.foundation.MonarchFieldSize
import com.rookhq.monarch.internal.MonarchDefaultIcons
import com.rookhq.monarch.theme.MonarchTheme
import com.rookhq.monarch.tokens.MonarchTokens
import java.util.Locale

private val DescriptionWidth: Dp = (MonarchTokens.Space.space24 + MonarchTokens.Space.space6).dp
private val QuantityWidth: Dp = MonarchTokens.Space.space16.dp
private val PriceWidth: Dp = (MonarchTokens.Space.space24 + MonarchTokens.Space.space2).dp
private val AmountWidth: Dp = MonarchTokens.Space.space20.dp
private val ActionWidth: Dp = MonarchTokens.Space.space12.dp
private val Gap: Dp = MonarchTokens.Space.space1.dp

/**
 * The rows as a table, as on the web: a header (Description, Qty, Unit price, Amount) and one row per item with
 * compact fields, the amount and a trash button. It scrolls sideways when the columns are wider than the screen.
 */
@Composable
@Suppress("LongParameterList")
internal fun MonarchLineItemsEditorTable(
    items: List<MonarchLineItem>,
    lines: List<Long>,
    currencyCode: String,
    locale: Locale,
    enabled: Boolean,
    removable: Boolean,
    text: MonarchLineItemsEditorText,
    colors: MonarchLineItemsEditorColors,
    onChange: (MonarchLineItem) -> Unit,
    onRemove: (Int) -> Unit,
) {
    val digits = MonarchMoneyFormat.currencyDigits(currencyCode)
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        // The columns share the width the trash button leaves; below a least scale the table scrolls sideways.
        val fixed = ActionWidth + AmountWidth + Gap * ColumnGaps
        val flexible = DescriptionWidth + QuantityWidth + PriceWidth
        val scale = ((maxWidth - fixed) / flexible).coerceIn(MinScale, MaxScale)
        val descriptionWidth = DescriptionWidth * scale
        val quantityWidth = maxOf(QuantityWidth * scale, MonarchTouchTarget.Minimum)
        val priceWidth = maxOf(PriceWidth * scale, MonarchTouchTarget.Minimum)
        val amountWidth = AmountWidth
    Column(
        modifier = Modifier
            .horizontalScroll(rememberScrollState())
            .background(MonarchTheme.colors.bgSurface),
        verticalArrangement = Arrangement.spacedBy(Gap),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(Gap), verticalAlignment = Alignment.CenterVertically) {
            Header(text.description, descriptionWidth, colors)
            Header(text.quantity, quantityWidth, colors)
            Header(text.unitPrice, priceWidth, colors)
            Header(text.amount, amountWidth, colors)
            Header("", ActionWidth, colors)
        }
        items.forEachIndexed { index, item ->
            Box {
                val spoken = text.pair(
                    text.item(index + 1),
                    text.value(
                        text.amount,
                        MonarchLineItemsEditorRowData(item, index + 1, lines[index], currencyCode, locale).amountText,
                    ),
                )
                // The row's number and amount are read as one heading, as in the stacked layout.
                Box(
                    Modifier.size(MonarchTokens.Space.space0.dp).clearAndSetSemantics {
                        heading()
                        contentDescription = spoken
                    },
                )
                Row(horizontalArrangement = Arrangement.spacedBy(Gap), verticalAlignment = Alignment.CenterVertically) {
                MonarchTextField(
                    value = item.description,
                    onValueChange = { onChange(item.edited(description = it)) },
                    modifier = Modifier.width(descriptionWidth).semantics { contentDescription = text.description },
                    placeholder = text.descriptionPlaceholder,
                    enabled = enabled,
                    size = MonarchFieldSize.Small,
                )
                MonarchNumberInput(
                    value = item.quantity?.toDouble(),
                    onValueChange = { onChange(item.edited(quantity = MonarchLineItemsEditorTokens.decimal(it))) },
                    modifier = Modifier.width(quantityWidth).semantics { contentDescription = text.quantity },
                    enabled = enabled,
                    showSteppers = false,
                )
                MonarchCurrencyInput(
                    minorUnits = MonarchLineItemsEditorTokens.toMinor(item.unitPrice, digits),
                    currencyCode = currencyCode,
                    currencyDisplay = MonarchMoneyCurrencyDisplay.NarrowSymbol,
                    onMinorUnitsChange = {
                        onChange(item.edited(unitPrice = MonarchLineItemsEditorTokens.fromMinor(it, digits)))
                    },
                    modifier = Modifier.width(priceWidth).semantics { contentDescription = text.unitPrice },
                    enabled = enabled,
                )
                Row(modifier = Modifier.width(amountWidth), horizontalArrangement = Arrangement.End) {
                    MonarchMoney(
                        minorUnits = lines[index],
                        currencyCode = currencyCode,
                        locale = locale,
                        currencyDisplay = MonarchMoneyCurrencyDisplay.NarrowSymbol,
                    )
                }
                val removeName = text.removeItem(index + 1, item.description)
                MonarchIconButton(
                    label = text.remove,
                    onClick = { onRemove(index) },
                    modifier = Modifier.width(ActionWidth).semantics { contentDescription = removeName },
                    size = MonarchButtonSize.Sm,
                    enabled = enabled && removable,
                ) {
                    MonarchDefaultIcons.Trash()
                }
                }
            }
        }
    }
    }
}

@Composable
private fun Header(title: String, width: Dp, colors: MonarchLineItemsEditorColors) {
    Text(
        text = title,
        modifier = Modifier.width(width).padding(horizontal = MonarchTokens.Space.space1.dp),
        style = MonarchTheme.typography.caption,
        color = colors.itemHeader,
        maxLines = 1,
        softWrap = false,
    )
}

private const val ColumnGaps = 4
private const val MinScale = 0.6f
private const val MaxScale = 2f
