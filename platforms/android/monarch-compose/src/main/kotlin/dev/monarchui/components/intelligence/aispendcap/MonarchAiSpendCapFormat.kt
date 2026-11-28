package dev.monarchui.components.intelligence.aispendcap

import android.icu.text.DateFormat
import dev.monarchui.components.billing.money.MonarchMoneyFormat
import dev.monarchui.internal.MonarchNumberText
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import android.icu.util.TimeZone as IcuTimeZone

/** Writes the amounts, percentages and dates of one MonarchAiSpendCap in a locale. */
internal class MonarchAiSpendCapFormat(
    private val locale: Locale,
    private val currencyCode: String,
    private val unit: MonarchAiSpendCapUnit,
    private val timeZone: TimeZone,
) {
    /** An integer amount in the unit as money: 1240 minor GBP in en-GB is "£12.40". */
    fun money(amount: Long): String =
        MonarchMoneyFormat.format(MonarchAiSpendCapMath.toMajor(amount, currencyCode, unit), currencyCode, locale)

    /** A share from 0 to 1 as a whole percentage: 0.8 is "80%". */
    fun percent(share: Double): String = MonarchNumberText.percent(locale, maxFractionDigits = 0)(share)

    /** The day and month of a time, with the time of day for a daily limit, in the time zone. */
    fun date(millis: Long, period: MonarchAiSpendCapPeriod): String {
        val skeleton = if (period == MonarchAiSpendCapPeriod.Day) DAY_AND_TIME else DAY
        return DateFormat.getInstanceForSkeleton(skeleton, locale).apply {
            this.timeZone = IcuTimeZone.getTimeZone(this@MonarchAiSpendCapFormat.timeZone.id)
        }.format(Date(millis))
    }

    private companion object {
        const val DAY = "MMMMd"
        const val DAY_AND_TIME = "MMMMdjmm"
    }
}
