package dev.monarchui.components.billing.money

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import dev.monarchui.R
import dev.monarchui.components.typography.tabularnumberstyle.MonarchTabularNumber
import dev.monarchui.foundation.MonarchTextStyle
import dev.monarchui.internal.monarchString
import dev.monarchui.theme.MonarchTheme
import java.math.BigDecimal
import java.util.Locale

/**
 * An amount of money in one currency, in tabular figures.
 *
 * The text is written by the platform's `android.icu.text.NumberFormat`
 * currency styles (see [MonarchMoneyFormat]); Monarch adds the type role,
 * fixed-width digits, the empty state and the spoken form. With the compact
 * notation the short form is shown ($1.3M) and TalkBack reads the full
 * amount. A missing amount shows an em dash read as "No amount".
 *
 * @param amount the amount in major units (19.99 for $19.99), or `null` when there is none.
 * @param currencyCode the ISO 4217 code, such as USD, EUR or INR. The amount is never converted.
 * @param modifier the modifier of the text.
 * @param notation every digit, or the short form.
 * @param signDisplay when a sign is written.
 * @param currencyDisplay symbol, narrow symbol, code or name.
 * @param accountingSign writes negative amounts in parentheses where the locale does: ($42.00).
 * @param fractionDigits the least and the most digits after the decimal separator. `null` uses the currency's own.
 * @param locale the locale to write in. `null` uses the first locale of the configuration.
 * @param style the type role.
 * @param toneNegative colours a negative amount with the strong danger role. The sign or the parentheses always
 * carry the meaning.
 * @param color the text colour. Unspecified uses the content colour of the surrounding surface.
 * @param textAlign the alignment inside the available width, for example `End` in a numeric column.
 * @param emptyText replaces the built-in dash shown for a missing amount (`monarch_money_empty`).
 * @param emptyLabel replaces the built-in "No amount" that TalkBack reads for a missing amount
 * (`monarch_money_empty_label`).
 */
@Composable
public fun MonarchMoney(
    amount: BigDecimal?,
    currencyCode: String,
    modifier: Modifier = Modifier,
    notation: MonarchMoneyNotation = MonarchMoneyNotation.Standard,
    signDisplay: MonarchMoneySignDisplay = MonarchMoneySignDisplay.Automatic,
    currencyDisplay: MonarchMoneyCurrencyDisplay = MonarchMoneyCurrencyDisplay.Symbol,
    accountingSign: Boolean = false,
    fractionDigits: IntRange? = null,
    locale: Locale? = null,
    style: MonarchTextStyle = MonarchTextStyle.Body,
    toneNegative: Boolean = false,
    color: Color = Color.Unspecified,
    textAlign: TextAlign? = null,
    emptyText: String? = null,
    emptyLabel: String? = null,
) {
    val colors = MonarchTheme.colors
    if (amount == null) {
        val spoken = monarchString(emptyLabel, R.string.monarch_money_empty_label)
        MonarchTabularNumber(
            text = monarchString(emptyText, R.string.monarch_money_empty),
            // TalkBack reads the content description in place of the dash.
            modifier = modifier.semantics { contentDescription = spoken },
            style = style,
            color = MonarchMoneyTokens.textColor(negativeToned = false, empty = true, color = color, colors = colors),
            textAlign = textAlign,
        )
        return
    }
    val resolvedLocale = locale ?: LocalConfiguration.current.locales[0]
    val written = remember(
        amount,
        currencyCode,
        resolvedLocale,
        notation,
        signDisplay,
        currencyDisplay,
        accountingSign,
        fractionDigits,
    ) {
        fun write(form: MonarchMoneyNotation) = MonarchMoneyFormat.format(
            amount, currencyCode, resolvedLocale, form, signDisplay, currencyDisplay, accountingSign, fractionDigits,
        )
        val shown = write(notation)
        shown to if (notation == MonarchMoneyNotation.Compact) write(MonarchMoneyNotation.Standard) else shown
    }
    val (shown, full) = written
    MonarchTabularNumber(
        text = shown,
        modifier = if (full != shown) modifier.semantics { contentDescription = full } else modifier,
        style = style,
        color = MonarchMoneyTokens.textColor(
            negativeToned = toneNegative && amount.signum() < 0,
            empty = false,
            color = color,
            colors = colors,
        ),
        textAlign = textAlign,
    )
}

/**
 * An amount of money given in minor units (cents, paise), in tabular figures.
 *
 * The amount is converted exactly with the currency's own number of digits
 * (USD 2, JPY 0, KWD 3), then shown as by the major-unit overload.
 *
 * @param minorUnits the amount in minor units (1999 with USD is $19.99), or `null` when there is none.
 * @param currencyCode the ISO 4217 code, such as USD, EUR or INR.
 * @param modifier the modifier of the text.
 * @param notation every digit, or the short form.
 * @param signDisplay when a sign is written.
 * @param currencyDisplay symbol, narrow symbol, code or name.
 * @param accountingSign writes negative amounts in parentheses where the locale does.
 * @param fractionDigits the least and the most digits after the decimal separator. `null` uses the currency's own.
 * @param locale the locale to write in. `null` uses the first locale of the configuration.
 * @param style the type role.
 * @param toneNegative colours a negative amount with the strong danger role.
 * @param color the text colour. Unspecified uses the content colour of the surrounding surface.
 * @param textAlign the alignment inside the available width.
 * @param emptyText replaces the built-in dash shown for a missing amount.
 * @param emptyLabel replaces the built-in "No amount" that TalkBack reads for a missing amount.
 */
@Composable
public fun MonarchMoney(
    minorUnits: Long?,
    currencyCode: String,
    modifier: Modifier = Modifier,
    notation: MonarchMoneyNotation = MonarchMoneyNotation.Standard,
    signDisplay: MonarchMoneySignDisplay = MonarchMoneySignDisplay.Automatic,
    currencyDisplay: MonarchMoneyCurrencyDisplay = MonarchMoneyCurrencyDisplay.Symbol,
    accountingSign: Boolean = false,
    fractionDigits: IntRange? = null,
    locale: Locale? = null,
    style: MonarchTextStyle = MonarchTextStyle.Body,
    toneNegative: Boolean = false,
    color: Color = Color.Unspecified,
    textAlign: TextAlign? = null,
    emptyText: String? = null,
    emptyLabel: String? = null,
) {
    MonarchMoney(
        amount = minorUnits?.let { MonarchMoneyFormat.amountOfMinorUnits(it, currencyCode) },
        currencyCode = currencyCode,
        modifier = modifier,
        notation = notation,
        signDisplay = signDisplay,
        currencyDisplay = currencyDisplay,
        accountingSign = accountingSign,
        fractionDigits = fractionDigits,
        locale = locale,
        style = style,
        toneNegative = toneNegative,
        color = color,
        textAlign = textAlign,
        emptyText = emptyText,
        emptyLabel = emptyLabel,
    )
}
