package com.rookhq.monarch.components.datadisplay.periodselector

import android.icu.text.DateFormat
import android.icu.text.DateIntervalFormat
import android.icu.util.DateInterval
import android.icu.util.TimeZone
import android.icu.util.ULocale
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.R
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens
import java.text.FieldPosition
import java.util.Locale

/** Component tokens and the pure rules of MonarchPeriodSelector. */
internal object MonarchPeriodSelectorTokens {
    val Gap: Dp = MonarchTokens.Space.space2.dp
    val DialogPadding: Dp = MonarchTokens.Space.space6.dp

    /** The value after the person picks [preset]: a custom period starts from the dates shown until then. */
    fun next(value: MonarchPeriodValue, preset: MonarchPeriodPreset, shown: MonarchPeriodRange?): MonarchPeriodValue =
        if (preset == MonarchPeriodPreset.Custom) {
            MonarchPeriodValue(preset, value.range ?: shown)
        } else {
            MonarchPeriodValue(preset)
        }

    /** Whether the picked days can be kept: a first day, and no more than [maxDays] days. */
    fun accepts(start: Long?, end: Long?, maxDays: Int?): Boolean =
        start != null && !tooLong(start, end, maxDays)

    /** Whether the picked days are more than [maxDays]. */
    fun tooLong(start: Long?, end: Long?, maxDays: Int?): Boolean =
        start != null && maxDays != null && PeriodSelectorDays.length(start, end ?: start) > maxDays

    /** Whether [day] can be picked: not before [minDate] and not after [latest]. */
    fun selectable(day: Long, minDate: Long?, latest: Long): Boolean =
        (minDate == null || day >= PeriodSelectorDays.startOfDay(minDate)) && day <= latest

    /** The range written for [locale]: "Nov 9 – 15, 2026", or one date for a single day. */
    fun rangeText(range: MonarchPeriodRange, locale: Locale): String {
        val start = PeriodSelectorDays.startOfDay(range.startMillis)
        val end = PeriodSelectorDays.startOfDay(range.endMillis)
        val format = DateIntervalFormat.getInstance(DateFormat.YEAR_ABBR_MONTH_DAY, ULocale.forLocale(locale))
        format.timeZone = TimeZone.GMT_ZONE
        return format.format(DateInterval(minOf(start, end), maxOf(start, end)), StringBuffer(), FieldPosition(0))
            .toString()
    }

    /** What TalkBack reads for the custom dates button: "Custom period, Nov 1 – 15, 2026". */
    fun customText(name: String, dates: String): String = "$name, $dates"

    @StringRes
    fun presetName(preset: MonarchPeriodPreset): Int = PresetNames.getValue(preset)

    private val PresetNames: Map<MonarchPeriodPreset, Int> = mapOf(
        MonarchPeriodPreset.Today to R.string.monarch_periodselector_preset_today,
        MonarchPeriodPreset.Yesterday to R.string.monarch_periodselector_preset_yesterday,
        MonarchPeriodPreset.Last7Days to R.string.monarch_periodselector_preset_last_7_days,
        MonarchPeriodPreset.Last14Days to R.string.monarch_periodselector_preset_last_14_days,
        MonarchPeriodPreset.Last30Days to R.string.monarch_periodselector_preset_last_30_days,
        MonarchPeriodPreset.Last90Days to R.string.monarch_periodselector_preset_last_90_days,
        MonarchPeriodPreset.Last180Days to R.string.monarch_periodselector_preset_last_180_days,
        MonarchPeriodPreset.Last365Days to R.string.monarch_periodselector_preset_last_365_days,
        MonarchPeriodPreset.ThisWeek to R.string.monarch_periodselector_preset_this_week,
        MonarchPeriodPreset.ThisMonth to R.string.monarch_periodselector_preset_this_month,
        MonarchPeriodPreset.LastMonth to R.string.monarch_periodselector_preset_last_month,
        MonarchPeriodPreset.ThisQuarter to R.string.monarch_periodselector_preset_this_quarter,
        MonarchPeriodPreset.LastQuarter to R.string.monarch_periodselector_preset_last_quarter,
        MonarchPeriodPreset.ThisYear to R.string.monarch_periodselector_preset_this_year,
        MonarchPeriodPreset.Custom to R.string.monarch_periodselector_preset_custom,
    )

    /** The colour of the resolved dates under the control. */
    fun rangeColor(enabled: Boolean, colors: MonarchColors): Color =
        if (enabled) colors.textMuted else colors.textDisabled

    /** The colour of the too-long message in the picker. */
    fun errorColor(colors: MonarchColors): Color = colors.dangerStrong
}
