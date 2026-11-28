package dev.monarchui.components.forms.currencyinput

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import dev.monarchui.components.billing.money.MonarchMoneyCurrencyDisplay
import dev.monarchui.components.billing.money.MonarchMoneyFormat
import dev.monarchui.components.forms.numberinput.MonarchNumberInputLabels
import dev.monarchui.components.forms.numberinput.NumberInputBounds
import dev.monarchui.components.forms.numberinput.NumberInputField
import dev.monarchui.components.forms.numberinput.NumberInputOptions
import java.math.BigDecimal

/**
 * An amount of money in one currency, typed and shown in the person's
 * locale ("1.234,50 €" in German) with the currency's own decimals.
 *
 * Built on `MonarchNumberInput` (the Material 3 `OutlinedTextField` with the
 * decimal keyboard) and the currency format of `MonarchMoneyFormat`
 * (`android.icu.text.NumberFormat`): the label, help and error text, the
 * locale-aware reading of the text (currency signs and codes around the
 * number are ignored, "(12.00)" is negative), the range message and the
 * optional − and + buttons come from there. Every edit that reads as an
 * amount, in range or not, is passed to `onValueChange` at once, so a button
 * tapped right after typing reads the typed amount (validate the bounds before
 * saving); the text is shown in the currency
 * format when the field loses focus or on Done. The amount is
 * rounded half up to the currency's decimals (JPY 0, USD 2, KWD 3), so the
 * stored amount is exactly what is shown. Negative amounts are refused
 * unless [allowNegative] is set or a negative [minimum] is given.
 *
 * @param value the amount in major units, such as 19.99, or `null` while the field is empty.
 * @param currencyCode the ISO 4217 code, such as USD, EUR, JPY or KWD. Sets the symbol and the decimals.
 * @param onValueChange called with the new amount, rounded to the currency's decimals, or `null` when cleared.
 * @param modifier the modifier of the field.
 * @param label the visible label.
 * @param placeholder shown while the field is empty and focused.
 * @param helperText guidance below the field.
 * @param errorText an error from the app. It replaces the field's own messages while present.
 * @param currencyDisplay how the currency is written: symbol ($ or US$), narrow symbol ($), code (USD) or name.
 * @param minimum the smallest amount. Without it the smallest is 0, unless [allowNegative].
 * @param maximum the largest amount.
 * @param step the amount one press of − or + adds or removes.
 * @param allowNegative allows amounts below zero, for refunds and adjustments.
 * @param enabled whether the user can edit the field.
 * @param readOnly whether the amount can be selected and copied but not changed.
 * @param showSteppers shows the − and + buttons. Amounts are usually typed, so they are off.
 * @param labels replacements for the number field's built-in strings: the button names, "Enter a number." and
 * the range message.
 * @param interactionSource the source of the focus and press interactions of the field.
 */
@Composable
@Suppress("LongParameterList")
public fun MonarchCurrencyInput(
    value: Double?,
    currencyCode: String,
    onValueChange: (Double?) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    helperText: String? = null,
    errorText: String? = null,
    currencyDisplay: MonarchMoneyCurrencyDisplay = MonarchMoneyCurrencyDisplay.Symbol,
    minimum: Double? = null,
    maximum: Double? = null,
    step: Double = 1.0,
    allowNegative: Boolean = false,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    showSteppers: Boolean = false,
    labels: MonarchNumberInputLabels = MonarchNumberInputLabels(),
    interactionSource: MutableInteractionSource? = null,
) {
    val locale = LocalConfiguration.current.locales[0]
    val digits = MonarchMoneyFormat.currencyDigits(currencyCode)
    val format = remember(currencyCode, currencyDisplay, locale) {
        val writer: (Double) -> String = { amount ->
            MonarchMoneyFormat.format(
                amount = BigDecimal(amount.toString()),
                currencyCode = currencyCode,
                locale = locale,
                currencyDisplay = currencyDisplay,
            )
        }
        writer
    }
    NumberInputField(
        value = value,
        onValueChange = { amount -> onValueChange(amount?.let { CurrencyInputMath.rounded(it, digits) }) },
        modifier = modifier,
        label = label,
        placeholder = placeholder,
        helperText = helperText,
        errorText = errorText,
        bounds = NumberInputBounds(
            minimum = CurrencyInputMath.lowest(minimum, allowNegative),
            maximum = maximum,
            step = step,
            fractionDigits = digits,
        ),
        options = NumberInputOptions(enabled, readOnly, showSteppers),
        format = format,
        parse = null,
        labels = labels,
        interactionSource = interactionSource,
    )
}

/**
 * An amount of money in whole minor units, such as 1999 cents for 19.99
 * dollars, for APIs that store integers. The same field as the major-unit
 * overload; the conversion goes through `BigDecimal`, so no cent is lost.
 *
 * @param minorUnits the amount in minor units (cents, paise), or `null` while the field is empty.
 * @param currencyCode the ISO 4217 code, such as USD, EUR, JPY or KWD. Sets the symbol and the decimals.
 * @param onMinorUnitsChange called with the new amount in minor units, or `null` when cleared.
 * @param modifier the modifier of the field.
 * @param label the visible label.
 * @param placeholder shown while the field is empty and focused.
 * @param helperText guidance below the field.
 * @param errorText an error from the app. It replaces the field's own messages while present.
 * @param currencyDisplay how the currency is written: symbol, narrow symbol, code or name.
 * @param minimum the smallest amount, in minor units. Without it the smallest is 0, unless [allowNegative].
 * @param maximum the largest amount, in minor units.
 * @param step the amount one press of − or + adds or removes, in minor units. `null` is one major unit.
 * @param allowNegative allows amounts below zero, for refunds and adjustments.
 * @param enabled whether the user can edit the field.
 * @param readOnly whether the amount can be selected and copied but not changed.
 * @param showSteppers shows the − and + buttons.
 * @param labels replacements for the number field's built-in strings.
 * @param interactionSource the source of the focus and press interactions of the field.
 */
@Composable
@Suppress("LongParameterList")
public fun MonarchCurrencyInput(
    minorUnits: Long?,
    currencyCode: String,
    onMinorUnitsChange: (Long?) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    helperText: String? = null,
    errorText: String? = null,
    currencyDisplay: MonarchMoneyCurrencyDisplay = MonarchMoneyCurrencyDisplay.Symbol,
    minimum: Long? = null,
    maximum: Long? = null,
    step: Long? = null,
    allowNegative: Boolean = false,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    showSteppers: Boolean = false,
    labels: MonarchNumberInputLabels = MonarchNumberInputLabels(),
    interactionSource: MutableInteractionSource? = null,
) {
    val digits = MonarchMoneyFormat.currencyDigits(currencyCode)
    val major: (Long) -> Double = { CurrencyInputMath.majorUnits(it, digits) }
    MonarchCurrencyInput(
        value = minorUnits?.let(major),
        currencyCode = currencyCode,
        onValueChange = { amount -> onMinorUnitsChange(amount?.let { CurrencyInputMath.minorUnits(it, digits) }) },
        modifier = modifier,
        label = label,
        placeholder = placeholder,
        helperText = helperText,
        errorText = errorText,
        currencyDisplay = currencyDisplay,
        minimum = minimum?.let(major),
        maximum = maximum?.let(major),
        step = step?.let(major) ?: 1.0,
        allowNegative = allowNegative,
        enabled = enabled,
        readOnly = readOnly,
        showSteppers = showSteppers,
        labels = labels,
        interactionSource = interactionSource,
    )
}
