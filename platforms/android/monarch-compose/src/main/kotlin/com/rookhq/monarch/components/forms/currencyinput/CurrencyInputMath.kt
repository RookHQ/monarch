package com.rookhq.monarch.components.forms.currencyinput

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Exact conversions for MonarchCurrencyInput: rounding to the currency's
 * decimals and moving between major and minor units through `BigDecimal`, so
 * 19.99 dollars is always 1999 cents.
 */
internal object CurrencyInputMath {
    /** [value] rounded half away from zero to [digits] decimals, computed on its shortest decimal text. */
    fun rounded(value: Double, digits: Int): Double {
        if (!value.isFinite()) return value
        val result = BigDecimal(value.toString()).setScale(digits.coerceAtLeast(0), RoundingMode.HALF_UP).toDouble()
        return if (result == 0.0) 0.0 else result
    }

    /** A major-unit amount as whole minor units: 19.99 USD is 1999. `null` when it does not fit a `Long`. */
    fun minorUnits(major: Double, digits: Int): Long? {
        if (!major.isFinite()) return null
        val scaled = BigDecimal(major.toString()).movePointRight(digits).setScale(0, RoundingMode.HALF_UP)
        return runCatching { scaled.longValueExact() }.getOrNull()
    }

    /** Whole minor units as a major-unit amount: 1999 USD is 19.99. */
    fun majorUnits(minor: Long, digits: Int): Double = BigDecimal.valueOf(minor).movePointLeft(digits).toDouble()

    /** The lowest amount allowed: the given minimum, or zero when negative amounts are not allowed. */
    fun lowest(minimum: Double?, allowNegative: Boolean): Double? = minimum ?: if (allowNegative) null else 0.0
}
