package com.rookhq.monarch.components.intelligence.aicontextdivider

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.internal.MonarchSurfaceColors
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens
import java.util.Calendar
import java.util.TimeZone

/** How a day relates to today, which decides its label. */
internal enum class MonarchAiDayRelation { Today, Yesterday, ThisYear, OtherYear }

/** Component tokens and the pure logic of MonarchAiContextDivider. */
internal object MonarchAiContextDividerTokens {
    val LineWidth: Dp = MonarchTokens.Space.px.dp
    val LineMinLength: Dp = MonarchTokens.Space.space4.dp
    val Gap: Dp = MonarchTokens.Space.space2.dp
    val VerticalGap: Dp = MonarchTokens.Space.space1.dp
    val VerticalMargin: Dp = MonarchTokens.Space.space3.dp
    val SummaryPadding: Dp = MonarchTokens.Space.space3.dp
    val Dash: Dp = MonarchTokens.Space.space1.dp
    const val FONT_WEIGHT: Int = MonarchTokens.FontWeight.medium

    /** The date skeleton of a day in this year: weekday, month and day. */
    const val THIS_YEAR_SKELETON: String = "EEEEMMMMd"

    /** The date skeleton of a day in another year: with the year. */
    const val OTHER_YEAR_SKELETON: String = "yEEEEMMMMd"

    /** The colour of the lines: subtle for a day, default for a reset, the AI border for a summary. */
    fun lineColor(kind: MonarchAiContextDividerKind, colors: MonarchColors): Color = when (kind) {
        MonarchAiContextDividerKind.Day -> colors.borderSubtle
        MonarchAiContextDividerKind.ContextReset -> colors.borderDefault
        MonarchAiContextDividerKind.Summarized -> colors.aiBorder
    }

    /** The colour of the label: muted, or the strong AI role for a summary. */
    fun labelColor(kind: MonarchAiContextDividerKind, colors: MonarchColors): Color =
        if (kind == MonarchAiContextDividerKind.Summarized) colors.aiStrong else colors.textMuted

    /** The box of the summary: the subtle AI fill and the AI border, with default text. */
    fun summaryColors(colors: MonarchColors): MonarchSurfaceColors =
        MonarchSurfaceColors(container = colors.aiSubtle, content = colors.textDefault, border = colors.aiBorder)

    /** A context reset has dashed lines, so it differs by form and not only by colour. */
    fun dashed(kind: MonarchAiContextDividerKind): Boolean = kind == MonarchAiContextDividerKind.ContextReset

    /** Whether the divider has controls: a summary or a way to show the original messages. */
    fun interactive(kind: MonarchAiContextDividerKind, hasSummary: Boolean, canExpand: Boolean): Boolean =
        kind == MonarchAiContextDividerKind.Summarized && (hasSummary || canExpand)

    /** How the day of [dateMillis] relates to the day of [nowMillis], counted in [timeZone]. */
    fun relation(dateMillis: Long, nowMillis: Long, timeZone: TimeZone): MonarchAiDayRelation {
        val day = Calendar.getInstance(timeZone).apply { timeInMillis = dateMillis }
        val today = Calendar.getInstance(timeZone).apply { timeInMillis = nowMillis }
        val yesterday = (today.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }
        return when {
            sameDay(day, today) -> MonarchAiDayRelation.Today
            sameDay(day, yesterday) -> MonarchAiDayRelation.Yesterday
            day.get(Calendar.YEAR) == today.get(Calendar.YEAR) -> MonarchAiDayRelation.ThisYear
            else -> MonarchAiDayRelation.OtherYear
        }
    }

    private fun sameDay(a: Calendar, b: Calendar): Boolean =
        a.get(Calendar.YEAR) == b.get(Calendar.YEAR) && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)
}
