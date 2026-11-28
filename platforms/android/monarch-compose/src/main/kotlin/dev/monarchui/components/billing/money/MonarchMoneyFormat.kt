package dev.monarchui.components.billing.money

import android.icu.text.CompactDecimalFormat
import android.icu.text.DecimalFormat
import android.icu.text.NumberFormat
import android.icu.util.Currency
import android.icu.util.ULocale
import android.os.Build
import java.math.BigDecimal
import java.util.Locale
import android.icu.math.BigDecimal as IcuBigDecimal

/**
 * Writes amounts of money the way `MonarchMoney` shows them, for places that
 * need the text itself: a table export, a chart label, a content description.
 *
 * The formatting is the platform's `android.icu.text.NumberFormat` currency
 * styles, so the symbol, its position, the separators and the digits follow
 * the locale and the currency.
 */
public object MonarchMoneyFormat {
    private const val FALLBACK_DIGITS = 2

    /**
     * Writes [amount] of [currencyCode] for [locale].
     *
     * @param amount the amount in major units: 19.99 for $19.99.
     * @param currencyCode the ISO 4217 code, such as USD, EUR or INR. An unknown or malformed code writes the
     * number followed by the code.
     * @param locale the locale to write in.
     * @param notation every digit, or the short form ($1.3M).
     * @param signDisplay when a sign is written.
     * @param currencyDisplay symbol, narrow symbol, code or name.
     * @param accountingSign writes negative amounts in parentheses where the locale does: ($42.00). Applies to the
     * symbol forms only.
     * @param fractionDigits the least and the most digits after the decimal separator. `null` uses the currency's
     * own (USD 2, JPY 0, KWD 3). Ignored by the compact notation, which uses the locale's significant digits.
     * @return the formatted amount.
     */
    public fun format(
        amount: BigDecimal,
        currencyCode: String,
        locale: Locale,
        notation: MonarchMoneyNotation = MonarchMoneyNotation.Standard,
        signDisplay: MonarchMoneySignDisplay = MonarchMoneySignDisplay.Automatic,
        currencyDisplay: MonarchMoneyCurrencyDisplay = MonarchMoneyCurrencyDisplay.Symbol,
        accountingSign: Boolean = false,
        fractionDigits: IntRange? = null,
    ): String {
        val currency = currencyOf(currencyCode)
            ?: return NumberFormat.getNumberInstance(locale).format(amount) + " " + currencyCode
        val signum = amount.signum()
        val negative = signum < 0 && signDisplay != MonarchMoneySignDisplay.Never
        val plus = (signDisplay == MonarchMoneySignDisplay.Always && signum >= 0) ||
            (signDisplay == MonarchMoneySignDisplay.ExceptZero && signum > 0)
        val formatter = currencyFormat(currency, locale, currencyDisplay, accountingSign && negative, fractionDigits)
        val write: (BigDecimal) -> String = when (notation) {
            MonarchMoneyNotation.Standard -> { value -> formatter.format(value) }
            MonarchMoneyNotation.Compact -> compactWriter(currency, locale, currencyDisplay, accountingSign && negative)
        }
        val magnitude = amount.abs()
        val text = when {
            negative -> write(magnitude.negate())
            plus -> withPlus(write(magnitude.negate()), locale)
            else -> write(magnitude)
        }
        return narrowed(text, currency, locale, currencyDisplay)
    }

    /**
     * Converts an amount in minor units (cents, paise) to major units, exactly.
     *
     * @param minorUnits the amount in minor units: 1999 with USD is 19.99.
     * @param currencyCode the ISO 4217 code that decides the number of minor digits.
     * @return the amount in major units.
     */
    public fun amountOfMinorUnits(minorUnits: Long, currencyCode: String): BigDecimal =
        BigDecimal.valueOf(minorUnits).movePointLeft(currencyDigits(currencyCode))

    /**
     * The number of minor-unit digits of a currency: USD 2, JPY 0, KWD 3. An unknown code has 2.
     *
     * @param currencyCode the ISO 4217 code.
     * @return the number of digits after the decimal separator.
     */
    public fun currencyDigits(currencyCode: String): Int =
        currencyOf(currencyCode)?.defaultFractionDigits?.takeIf { it >= 0 } ?: FALLBACK_DIGITS

    private fun currencyOf(code: String): Currency? =
        if (code.length == ISO_LENGTH && code.all { it in 'A'..'Z' }) {
            runCatching { Currency.getInstance(code) }.getOrNull()
        } else {
            null
        }

    private fun currencyFormat(
        currency: Currency,
        locale: Locale,
        display: MonarchMoneyCurrencyDisplay,
        accounting: Boolean,
        fractionDigits: IntRange?,
    ): NumberFormat {
        val style = when (display) {
            MonarchMoneyCurrencyDisplay.Code -> NumberFormat.ISOCURRENCYSTYLE
            MonarchMoneyCurrencyDisplay.Name -> NumberFormat.PLURALCURRENCYSTYLE
            MonarchMoneyCurrencyDisplay.Symbol,
            MonarchMoneyCurrencyDisplay.NarrowSymbol,
            -> if (accounting) NumberFormat.ACCOUNTINGCURRENCYSTYLE else NumberFormat.CURRENCYSTYLE
        }
        return NumberFormat.getInstance(ULocale.forLocale(locale), style).apply {
            this.currency = currency
            roundingMode = IcuBigDecimal.ROUND_HALF_UP
            if (fractionDigits != null) {
                minimumFractionDigits = fractionDigits.first.coerceAtLeast(0)
                maximumFractionDigits = fractionDigits.last.coerceAtLeast(minimumFractionDigits)
            }
        }
    }

    /**
     * The compact form: the locale's short number ("1.3M") put where the digit
     * stands in the currency format of one unit ("$1"), so the symbol keeps
     * its place and spacing for the locale.
     */
    private fun compactWriter(
        currency: Currency,
        locale: Locale,
        display: MonarchMoneyCurrencyDisplay,
        accounting: Boolean,
    ): (BigDecimal) -> String {
        val compact = CompactDecimalFormat.getInstance(locale, CompactDecimalFormat.CompactStyle.SHORT)
        val template = currencyFormat(currency, locale, display, accounting, 0..0)
        // The plural name needs a plural count, or "1.3M US dollar" would be written.
        val unit = if (display == MonarchMoneyCurrencyDisplay.Name) 2L else 1L
        val digit = NumberFormat.getIntegerInstance(locale).format(unit)
        return { value ->
            val shape = template.format(if (value.signum() < 0) -unit else unit)
            shape.replaceFirst(digit, upperSuffix(compact.format(value.abs()), locale))
        }
    }

    /** The web writes the English short forms in capitals ("1.2M"); some ICU data has "1.2m" for en-GB. */
    private fun upperSuffix(text: String, locale: Locale): String =
        if (locale.language != "en") {
            text
        } else {
            text.replace(Regex("(\\d)([kmbt])(?!\\p{L})")) { it.groupValues[1] + it.groupValues[2].uppercase() }
        }

    /** [negativeText] with its minus sign turned into a plus sign, or a plus sign put first. */
    private fun withPlus(negativeText: String, locale: Locale): String {
        val symbols = (NumberFormat.getInstance(locale) as? DecimalFormat)?.decimalFormatSymbols
        val minus = symbols?.minusSign ?: '-'
        val plus = symbols?.plusSign ?: '+'
        return when {
            minus in negativeText -> negativeText.replaceFirst(minus, plus)
            '-' in negativeText -> negativeText.replaceFirst('-', plus)
            else -> "$plus$negativeText"
        }
    }

    private fun narrowed(
        text: String,
        currency: Currency,
        locale: Locale,
        display: MonarchMoneyCurrencyDisplay,
    ): String {
        if (display != MonarchMoneyCurrencyDisplay.NarrowSymbol || Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return text
        }
        val symbol = currency.getName(locale, Currency.SYMBOL_NAME, null)
        val narrow = currency.getName(locale, Currency.NARROW_SYMBOL_NAME, null)
        return if (symbol.isNullOrEmpty() || narrow.isNullOrEmpty()) text else text.replaceFirst(symbol, narrow)
    }

    private const val ISO_LENGTH = 3
}
