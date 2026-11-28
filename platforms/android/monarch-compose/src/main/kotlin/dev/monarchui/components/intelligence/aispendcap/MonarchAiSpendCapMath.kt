package dev.monarchui.components.intelligence.aispendcap

import dev.monarchui.components.billing.money.MonarchMoneyFormat
import java.math.BigDecimal
import java.math.RoundingMode

/** The integer amounts of MonarchAiSpendCap: converted exactly, never through a float. */
internal object MonarchAiSpendCapMath {
    private const val MICROS_DIGITS = 6

    /** The number of decimals the integers of [unit] carry for [currencyCode]. */
    fun digits(currencyCode: String, unit: MonarchAiSpendCapUnit): Int =
        if (unit == MonarchAiSpendCapUnit.Micros) MICROS_DIGITS else MonarchMoneyFormat.currencyDigits(currencyCode)

    /** An integer amount in major units: 1240 minor GBP is 12.40. */
    fun toMajor(amount: Long, currencyCode: String, unit: MonarchAiSpendCapUnit): BigDecimal =
        BigDecimal.valueOf(amount).movePointLeft(digits(currencyCode, unit))

    /** An integer amount in the currency's minor units, rounded half up from micros: for the amount field. */
    fun toMinor(amount: Long, currencyCode: String, unit: MonarchAiSpendCapUnit): Long =
        toMajor(amount, currencyCode, unit)
            .setScale(MonarchMoneyFormat.currencyDigits(currencyCode), RoundingMode.HALF_UP)
            .unscaledValue()
            .toLong()

    /** A typed amount in minor units as an integer in [unit]: 1240 minor GBP is 12 400 000 micros. */
    fun fromMinor(minor: Long, currencyCode: String, unit: MonarchAiSpendCapUnit): Long =
        if (unit == MonarchAiSpendCapUnit.Micros) {
            BigDecimal.valueOf(minor).movePointRight(MICROS_DIGITS - MonarchMoneyFormat.currencyDigits(currencyCode))
                .toLong()
        } else {
            minor
        }

    /** The warn-at choices: the options and the current value, strictly between 0 and 1, in order. */
    fun shares(options: List<Double>, warnAt: Double): List<Double> =
        (options + warnAt).filter { it > 0.0 && it < 1.0 }.distinct().sorted()

    /** The share the form starts on: the current one, or the nearest choice when it cannot be offered. */
    fun initialShare(shares: List<Double>, warnAt: Double): Double =
        if (warnAt in shares || shares.isEmpty()) warnAt else shares.minBy { kotlin.math.abs(it - warnAt) }
}
