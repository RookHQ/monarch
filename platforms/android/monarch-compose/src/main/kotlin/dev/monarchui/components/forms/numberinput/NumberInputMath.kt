package dev.monarchui.components.forms.numberinput

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.util.Locale

/**
 * Decimal arithmetic and locale-aware reading of numbers for MonarchNumberInput.
 *
 * Values are `Double`s, but every step and rounding goes through the
 * shortest decimal form of the number, so 0.1 + 0.2 is 0.3.
 */
internal object NumberInputMath {
    private const val MAX_PLACES = 20

    private fun decimal(value: Double): BigDecimal = BigDecimal(value.toString())

    /** The number of digits after the decimal point of [value]: 0.1 gives 1, 1e-7 gives 7, 100.0 gives 0. */
    fun decimalPlaces(value: Double): Int {
        if (!value.isFinite()) return 0
        return decimal(value).stripTrailingZeros().scale().coerceAtLeast(0)
    }

    /** [value] rounded half up to [places] decimals without binary drift. */
    fun roundTo(value: Double, places: Int): Double {
        if (!value.isFinite()) return value
        val rounded = decimal(value).setScale(places.coerceIn(0, MAX_PLACES), RoundingMode.HALF_UP).toDouble()
        return if (rounded == 0.0) 0.0 else rounded
    }

    /**
     * The point [delta] steps from [value] on the grid of [step] that passes
     * through [base]. A value between two points moves to the next point in
     * the direction of [delta] first: from 0.2 with step 1, one step up is 1.
     */
    fun stepValue(value: Double, step: Double, delta: Int, base: Double = 0.0): Double {
        val s = decimal(step)
        if (s.signum() <= 0) return value
        val b = decimal(base)
        val offset = decimal(value).subtract(b)
        val parts = offset.divideAndRemainder(s)
        val k = when {
            parts[1].signum() == 0 -> parts[0].toBigInteger().toLong() + delta
            delta > 0 -> offset.divide(s, 0, RoundingMode.FLOOR).toLong() + delta
            else -> offset.divide(s, 0, RoundingMode.CEILING).toLong() + delta
        }
        val result = b.add(s.multiply(BigDecimal.valueOf(k)))
        val places = maxOf(decimalPlaces(value), decimalPlaces(step), decimalPlaces(base))
        return roundTo(result.toDouble(), places)
    }

    /** [value] kept within [minimum] and [maximum]; a missing bound does not limit it. */
    fun clamp(value: Double, minimum: Double?, maximum: Double?): Double =
        value.coerceAtLeast(minimum ?: Double.NEGATIVE_INFINITY).coerceAtMost(maximum ?: Double.POSITIVE_INFINITY)

    /** Whether [value] is outside [minimum] and [maximum]. */
    fun outOfRange(value: Double, minimum: Double?, maximum: Double?): Boolean =
        (minimum != null && value < minimum) || (maximum != null && value > maximum)

    /** The number format of [locale], showing up to [fractionDigits] digits after the decimal point. */
    fun formatter(locale: Locale, fractionDigits: Int): NumberFormat =
        NumberFormat.getNumberInstance(locale).apply { maximumFractionDigits = fractionDigits }

    private val bidiMarks = Regex("[\u200e\u200f\u061c\u202a-\u202e\u2066-\u2069]")
    private val spaces = Regex("[\\s\u00a0\u202f\u2009]")
    private val number = Regex("""^([^0-9.]*)(\d*\.?\d*)([^0-9.]*)$""")
    private val affix = Regex("""^[\p{Sc}\p{L}%+\-\u2212()\s]*$""")

    /**
     * Reads a number typed in the conventions of [locale]: 1,234.5 in English,
     * 1.234,5 in German, 1 234,5 in French, and the locale's own digits.
     * Currency signs and codes and a percent sign around the number are ignored;
     * (1,234.50) is negative.
     *
     * @return the number, or `null` when the text cannot be read. Blank text is not passed here.
     */
    fun parse(text: String, locale: Locale): Double? {
        val symbols = DecimalFormatSymbols.getInstance(locale)
        // Digits of any script (Arabic-Indic, Devanagari, …) are read as 0 to 9.
        var s = text.replace(bidiMarks, "").trim().map { ch ->
            if (Character.isDigit(ch)) '0' + Character.digit(ch, DIGITS) else ch
        }.joinToString("")
        var negative = s.startsWith("(") && s.endsWith(")")
        val group = symbols.groupingSeparator.toString()
        s = if (spaces.matches(group)) s.replace(spaces, "") else s.replace(group, "").replace(spaces, "")
        s = s.replace(symbols.decimalSeparator, '.')
        val (prefix, digits, suffix) = number.find(s)?.destructured ?: return null
        val around = prefix + suffix
        val value = digits.takeIf { affix.matches(around) && it.any(Char::isDigit) }?.toDoubleOrNull() ?: return null
        if (around.contains('-') || around.contains('\u2212') || around.contains(symbols.minusSign)) negative = true
        val signed = if (negative) -value else value
        return if (signed == 0.0) 0.0 else signed
    }

    private const val DIGITS = 10
}
