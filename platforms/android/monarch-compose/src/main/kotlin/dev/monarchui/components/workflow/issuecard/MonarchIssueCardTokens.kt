package dev.monarchui.components.workflow.issuecard

import android.icu.text.DateFormat
import android.icu.util.TimeZone
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.components.workflow.statuspicker.MonarchStatusOption
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens
import java.util.Date
import java.util.Locale

/**
 * The surface colours of an issue card.
 *
 * @property container the fill.
 * @property border the border colour.
 * @property borderWidth the border width: thicker when selected, so selection is not colour alone.
 */
@Immutable
internal data class IssueCardSurface(val container: Color, val border: Color, val borderWidth: Dp)

/** Component tokens and pure logic of MonarchIssueCard, built from primitives and roles only. */
internal object MonarchIssueCardTokens {
    val ContentPadding: PaddingValues = PaddingValues(MonarchTokens.Space.space3.dp)
    val Gap: Dp = MonarchTokens.Space.space1_5.dp
    val MetaGap: Dp = MonarchTokens.Space.space2.dp
    val InlineGap: Dp = MonarchTokens.Space.space1.dp
    val BorderWidth: Dp = MonarchTokens.Space.px.dp
    val SelectedBorderWidth: Dp = MonarchTokens.Space.space0_5.dp
    val DashedBorderWidth: Dp = MonarchTokens.Space.px.dp
    const val DASH: Float = 3f
    const val DASH_GAP: Float = 2f

    private const val DAY_MILLIS: Long = 86_400_000L
    private const val DUE_SKELETON = "MMMd"

    /** The fill and border: the surface and default border, or the subtle accent fill and a thicker accent border. */
    fun surface(selected: Boolean, colors: MonarchColors): IssueCardSurface =
        if (selected) {
            IssueCardSurface(colors.accentSubtle, colors.accentSolid, SelectedBorderWidth)
        } else {
            IssueCardSurface(colors.bgSurface, colors.borderDefault, BorderWidth)
        }

    /** The due text colour: the strong danger colour when overdue, the muted text colour otherwise. */
    fun dueColor(
        overdue: Boolean,
        colors: MonarchColors,
    ): Color = if (overdue) colors.dangerStrong else colors.textMuted

    /** The border of the empty assignee slot. */
    fun unassignedColor(colors: MonarchColors): Color = colors.borderStrong

    /** The day number (days since 1970-01-01) of [millis], read in UTC as Material 3 date pickers store days. */
    fun day(millis: Long): Long = Math.floorDiv(millis, DAY_MILLIS)

    /** Today as a day number in the device's time zone, comparable with [day] of a date picker's value. */
    fun today(nowMillis: Long, zone: java.util.TimeZone): Long = day(nowMillis + zone.getOffset(nowMillis))

    /**
     * Whether an issue due on [dueMillis] is overdue on [todayMillis]: its
     * day is before today's. Days are compared, not times, and an issue whose
     * status is completed or cancelled is never overdue.
     */
    fun isOverdue(dueMillis: Long, todayDay: Long, status: MonarchStatusOption?): Boolean =
        status?.category?.isClosed != true && day(dueMillis) < todayDay

    /** The due day as a short month and day in [locale], read in UTC: "Mar 12". */
    fun formatDue(dueMillis: Long, locale: Locale): String =
        DateFormat.getInstanceForSkeleton(DUE_SKELETON, locale)
            .apply { timeZone = TimeZone.getTimeZone("UTC") }
            .format(Date(dueMillis))
}
