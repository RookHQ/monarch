package com.rookhq.monarch.components.billing.lineitemseditor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.width
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.rookhq.monarch.components.billing.money.MonarchMoney
import com.rookhq.monarch.components.billing.money.MonarchMoneyCurrencyDisplay
import com.rookhq.monarch.components.billing.money.MonarchMoneyFormat
import com.rookhq.monarch.components.forms.currencyinput.MonarchCurrencyInput
import com.rookhq.monarch.components.forms.numberinput.MonarchNumberInput
import com.rookhq.monarch.components.forms.segmentedcontrol.MonarchSegmentedControl
import com.rookhq.monarch.components.forms.select.MonarchSelect
import com.rookhq.monarch.components.forms.select.MonarchSelectOption
import com.rookhq.monarch.components.layout.divider.MonarchDivider
import com.rookhq.monarch.components.layout.divider.MonarchDividerVariant
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.theme.MonarchTheme
import com.rookhq.monarch.tokens.MonarchTokens
import java.math.BigDecimal

/**
 * The totals: subtotal, the discount and the tax when there are any (with their editors when they can be
 * changed), then the total in the strong body style above a divider. Each figure is one TalkBack element read as
 * its title and amount: "Total, $2,257.13".
 */
@Composable
internal fun MonarchLineItemsEditorTotals(
    data: MonarchLineItemsEditorTotalsData,
    stacked: Boolean,
    text: MonarchLineItemsEditorText,
) {
    val totals = data.totals
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MonarchLineItemsEditorTokens.TotalsGap),
    ) {
        TotalLine(text.subtotal, totals.subtotal, data, text)
        if (data.discount != null || data.onDiscountChange != null) {
            val discount = data.discount
            val title = if (discount?.kind == MonarchLineItemsDiscountKind.Percent && discount.value.signum() > 0) {
                text.withRate(text.discount, MonarchLineItemsEditorTokens.percent(discount.value, data.locale))
            } else {
                text.discount
            }
            if (stacked || data.onDiscountChange == null) {
                TotalLine(title, -totals.discount, data, text)
                data.onDiscountChange?.let { DiscountEditor(data, discount, it, stacked, text) }
            } else {
                TotalLine(text.discount, -totals.discount, data, text, middle = {
                    DiscountEditor(data, discount, data.onDiscountChange, stacked, text)
                })
            }
        }
        if (data.taxRate != null || data.onTaxRateChange != null) {
            val rate = data.taxRate?.takeIf { it.signum() != 0 }
            val written = rate?.let { MonarchLineItemsEditorTokens.percent(it, data.locale) }.orEmpty()
            if (stacked || data.onTaxRateChange == null) {
                TotalLine(text.tax(written), totals.tax, data, text)
                data.onTaxRateChange?.let { TaxRateEditor(data, it, text, false) }
            } else {
                TotalLine(text.tax(""), totals.tax, data, text, middle = {
                    TaxRateEditor(data, data.onTaxRateChange, text, true)
                })
            }
        }
        MonarchDivider(variant = MonarchDividerVariant.Subtle)
        TotalLine(text.total, totals.total, data, text, strong = true)
    }
}

@Composable
private fun TotalLine(
    title: String,
    minor: Long,
    data: MonarchLineItemsEditorTotalsData,
    text: MonarchLineItemsEditorText,
    strong: Boolean = false,
    middle: (@Composable () -> Unit)? = null,
) {
    val spoken = text.pair(title, data.write(minor))
    val style = if (strong) MonarchTextStyle.BodyStrong else MonarchTextStyle.Body
    Row(
        modifier = Modifier.fillMaxWidth().then(
            if (middle == null) Modifier.clearAndSetSemantics { contentDescription = spoken } else Modifier,
        ),
        horizontalArrangement = Arrangement.spacedBy(MonarchLineItemsEditorTokens.FieldGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            modifier = if (middle == null) Modifier.weight(1f) else Modifier,
            style = if (strong) MonarchTheme.typography.bodyStrong else MonarchTheme.typography.body,
            color = MonarchTheme.colors.textDefault,
        )
        if (middle != null) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(MonarchLineItemsEditorTokens.FieldGap),
            ) {
                middle()
            }
        }
        MonarchMoney(
            minorUnits = minor,
            currencyCode = data.currencyCode,
            locale = data.locale,
            currencyDisplay = MonarchMoneyCurrencyDisplay.NarrowSymbol,
            style = style,
        )
    }
}

/** The discount's value (a number field for a percentage, a currency field for an amount) and its kind. */
@Composable
private fun DiscountEditor(
    data: MonarchLineItemsEditorTotalsData,
    discount: MonarchLineItemsDiscount?,
    onChange: (MonarchLineItemsDiscount) -> Unit,
    stacked: Boolean,
    text: MonarchLineItemsEditorText,
) {
    val kind = discount?.kind ?: MonarchLineItemsDiscountKind.Percent
    val value = discount?.value ?: BigDecimal.ZERO
    val digits = MonarchMoneyFormat.currencyDigits(data.currencyCode)
    val field = @Composable { modifier: Modifier ->
        if (kind == MonarchLineItemsDiscountKind.Percent) {
            MonarchNumberInput(
                value = value.toDouble(),
                onValueChange = {
                    val next = MonarchLineItemsEditorTokens.decimal(it) ?: BigDecimal.ZERO
                    onChange(MonarchLineItemsDiscount(kind, next))
                },
                modifier = modifier,
                label = text.discount.takeIf { stacked },
                minimum = 0.0,
                showSteppers = false,
            )
        } else {
            MonarchCurrencyInput(
                minorUnits = MonarchLineItemsEditorTokens.toMinor(value, digits),
                currencyCode = data.currencyCode,
                currencyDisplay = MonarchMoneyCurrencyDisplay.NarrowSymbol,
                onMinorUnitsChange = {
                    val next = MonarchLineItemsEditorTokens.fromMinor(it, digits) ?: BigDecimal.ZERO
                    onChange(MonarchLineItemsDiscount(kind, next))
                },
                modifier = modifier.semantics { contentDescription = text.discount },
                label = text.discount.takeIf { stacked },
            )
        }
    }
    val choice = @Composable {
        if (!stacked) {
            MonarchSelect(
                options = listOf(
                    MonarchSelectOption(MonarchLineItemsDiscountKind.Percent, text.percentOption),
                    MonarchSelectOption(MonarchLineItemsDiscountKind.Amount, data.currencyCode),
                ),
                selected = kind,
                onSelect = { if (it != kind) onChange(MonarchLineItemsDiscount(it, value)) },
                modifier = Modifier.width(MonarchTokens.Space.space24.dp),
                contentDescription = text.discountType,
            )
        } else {
        val options = listOf(
            MonarchLineItemsDiscountKind.Percent to text.percentOption,
            MonarchLineItemsDiscountKind.Amount to data.currencyCode,
        )
        MonarchSegmentedControl(modifier = Modifier.semantics { contentDescription = text.discountType }) {
            options.forEach { (option, label) ->
                Segment(
                    selected = option == kind,
                    onClick = { if (option != kind) onChange(MonarchLineItemsDiscount(option, value)) },
                    label = label,
                )
            }
        }
        }
    }
    if (stacked) {
        field(Modifier.fillMaxWidth())
        choice()
    } else {
        field(Modifier.width(MonarchTokens.Space.space20.dp))
        choice()
    }
}

/** The tax rate: a menu of the preset rates, or a number field for a typed percentage. */
@Composable
private fun TaxRateEditor(
    data: MonarchLineItemsEditorTotalsData,
    onChange: (BigDecimal) -> Unit,
    text: MonarchLineItemsEditorText,
    compact: Boolean = false,
) {
    val rate = data.taxRate
    val width = if (compact) {
        Modifier.width((MonarchTokens.Space.space24 + MonarchTokens.Space.space16).dp)
    } else {
        Modifier.fillMaxWidth()
    }
    if (data.taxRateOptions.isNotEmpty()) {
        MonarchSelect(
            options = data.taxRateOptions.map { MonarchSelectOption(it.value, it.label) },
            selected = data.taxRateOptions.firstOrNull { rate != null && it.value.compareTo(rate) == 0 }?.value,
            onSelect = onChange,
            modifier = width,
            label = text.taxRate.takeIf { !compact },
            contentDescription = text.taxRate,
        )
    } else {
        MonarchNumberInput(
            value = rate?.toDouble(),
            onValueChange = { onChange(MonarchLineItemsEditorTokens.decimal(it) ?: BigDecimal.ZERO) },
            modifier = width,
            label = text.taxRate.takeIf { !compact },
            minimum = 0.0,
            showSteppers = false,
        )
    }
}
