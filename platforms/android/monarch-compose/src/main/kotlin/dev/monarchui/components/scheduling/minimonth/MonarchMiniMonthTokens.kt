package dev.monarchui.components.scheduling.minimonth

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.internal.MonarchSurfaceColors
import dev.monarchui.internal.MonarchTonePalette
import dev.monarchui.internal.scheduling.MonarchCalendarDates
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens

/** What a day cell of the grid is. */
internal data class MiniMonthDayState(
    val inMonth: Boolean,
    val available: Boolean,
    val selected: Boolean,
    val today: Boolean,
    val disabledByApp: Boolean,
)

/** Component tokens of MonarchMiniMonth, built from primitives and roles only. */
internal object MonarchMiniMonthTokens {
    /** The smallest width and height of a day: the 48dp touch target. */
    val CellSize: Dp = MonarchTokens.Space.space12.dp

    /** The circle behind the number of the selected day and today: 32dp, grows with the text. */
    val MarkSize: Dp = MonarchTokens.Control.sm.dp
    val DotSize: Dp = MonarchTokens.Space.space1_5.dp
    /** The gap between the month title and its chevron. */
    val TitleGap: Dp = MonarchTokens.Space.space1.dp
    val DotGap: Dp = MonarchTokens.Space.space0_5.dp
    val WeekColumnWidth: Dp = MonarchTokens.Space.space8.dp
    val HeaderGap: Dp = MonarchTokens.Space.space1.dp
    const val TODAY_WEIGHT: Int = MonarchTokens.FontWeight.semibold

    /** The latest day that can be chosen: [maximum], or [minimum] when the maximum is before it. */
    fun latest(minimum: Long?, maximum: Long?): Long? =
        if (minimum != null && maximum != null && maximum < minimum) minimum else maximum

    /** [month] kept between the months of [minimum] and [maximum]. */
    fun clampMonth(month: Long, minimum: Long?, maximum: Long?): Long {
        val index = MonarchCalendarDates.monthIndex(month)
        val last = latest(minimum, maximum)
        return when {
            minimum != null && index < MonarchCalendarDates.monthIndex(minimum) -> MonarchCalendarDates.monthOf(minimum)
            last != null && index > MonarchCalendarDates.monthIndex(last) -> MonarchCalendarDates.monthOf(last)
            else -> MonarchCalendarDates.monthOf(month)
        }
    }

    /** The state of [day] in the grid of [month]. */
    @Suppress("LongParameterList")
    fun dayState(
        day: Long,
        month: Long,
        selection: Long?,
        today: Long,
        minimum: Long?,
        maximum: Long?,
        enabled: Boolean,
        disabledByApp: Boolean,
    ): MiniMonthDayState {
        val inMonth = MonarchCalendarDates.monthIndex(day) == MonarchCalendarDates.monthIndex(month)
        val first = minimum?.let(MonarchCalendarDates::dayOf)
        val last = latest(minimum, maximum)?.let(MonarchCalendarDates::dayOf)
        val inRange = (first == null || day >= first) && (last == null || day <= last)
        val choosable = inMonth && inRange && !disabledByApp
        return MiniMonthDayState(
            inMonth = inMonth,
            available = choosable && enabled,
            selected = choosable && selection != null && MonarchCalendarDates.dayOf(selection) == day,
            today = day == today,
            disabledByApp = disabledByApp,
        )
    }

    /**
     * The colours of a day: the selected day is a solid accent circle, today
     * an accent ring with strong accent text, unavailable days and days of
     * the neighbouring months the disabled text colour.
     */
    fun dayColors(state: MiniMonthDayState, colors: MonarchColors): MonarchSurfaceColors = when {
        !state.inMonth -> MonarchSurfaceColors(Color.Transparent, colors.textDisabled, null)
        state.selected -> MonarchSurfaceColors(colors.accentSolid, colors.accentOnSolid, null)
        !state.available -> MonarchSurfaceColors(Color.Transparent, colors.textDisabled, null)
        state.today -> MonarchSurfaceColors(Color.Transparent, colors.accentStrong, colors.accentBorder)
        else -> MonarchSurfaceColors(Color.Transparent, colors.textDefault, null)
    }

    /** The colour of a dot: the accent for `null`, the subtle text colour for neutral, else the tone's solid. */
    fun dotColor(tone: MonarchTone?, colors: MonarchColors): Color = when (tone) {
        null -> colors.accentSolid
        MonarchTone.Neutral -> colors.textSubtle
        else -> MonarchTonePalette.of(tone, colors).solid
    }
}
