package com.rookhq.monarch.components.intelligence.aicomputerview

import android.icu.text.DateFormat
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.internal.MonarchSurfaceColors
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** What the log may show under an action's label: the detail, or that one is withheld. */
internal data class AiComputerViewDetail(val text: String?, val hidden: Boolean)

/** The colours of the last action, the selected action and the pointer drawn over the screenshot. */
internal data class AiComputerViewMarkerColors(
    val last: Color,
    val selected: Color,
    val pointer: Color,
    val pointerOutline: Color,
)

/** Which announcement a status change makes. */
internal enum class AiComputerViewNews { TakenOver, Released, Status }

/** Component tokens, times and colours of MonarchAiComputerView. */
internal object MonarchAiComputerViewTokens {
    val Gap: Dp = MonarchTokens.Space.space3.dp
    val ItemGap: Dp = MonarchTokens.Space.space2.dp
    val TextGap: Dp = MonarchTokens.Space.space0_5.dp
    val RowPaddingHorizontal: Dp = MonarchTokens.Space.space3.dp
    val RowPaddingVertical: Dp = MonarchTokens.Space.space2.dp
    val SelectedBarWidth: Dp = MonarchTokens.Space.space1.dp
    val MarkerRadius: Dp = MonarchTokens.Space.space3.dp
    val MarkerStroke: Dp = MonarchTokens.Space.space0_5.dp
    val PointerRadius: Dp = MonarchTokens.Space.space1_5.dp
    val PlaceholderPadding: Dp = MonarchTokens.Space.space4.dp
    val BannerPaddingHorizontal: Dp = MonarchTokens.Space.space3.dp
    val BannerPaddingVertical: Dp = MonarchTokens.Space.space2.dp
    val LogIconGap: Dp = MonarchTokens.Space.space2.dp

    /** The bar shown while the agent is in control: the solid AI fill with its text and icon colour. */
    fun banner(colors: MonarchColors): MonarchSurfaceColors =
        MonarchSurfaceColors(colors.aiSolid, colors.aiOnSolid, null)

    /** The border of the screen box: the AI border while the agent acts, the plain border otherwise. */
    fun frameBorder(agentActing: Boolean, colors: MonarchColors): Color =
        if (agentActing) colors.aiBorder else colors.borderDefault

    /** The box of the screen before a screenshot gives its size: 16 by 10, as on the web. */
    const val DEFAULT_ASPECT_RATIO: Float = 1.6f

    /** The shortest time between two action announcements by default, as on the web. */
    const val DEFAULT_ANNOUNCE_INTERVAL_MILLIS: Long = 4_000L

    /** How many of the newest actions the log shows by default, as on the web. */
    const val DEFAULT_VISIBLE_ACTIONS: Int = 200

    /** Remembered ids for announcements are pruned beyond this many. */
    const val MAX_SEEN: Int = 2_000

    /** The time of a screenshot or an action, with seconds, in [locale] and [timeZone]. */
    fun time(millis: Long, locale: Locale, timeZone: TimeZone): String {
        val format = DateFormat.getTimeInstance(DateFormat.MEDIUM, locale)
        format.timeZone = android.icu.util.TimeZone.getTimeZone(timeZone.id)
        return format.format(Date(millis))
    }

    /** What TalkBack reads for one action: kind, label, detail and time. */
    fun rowText(kind: String, label: String, detail: String?, time: String): String =
        listOfNotNull("$kind: $label", detail, time).joinToString(", ")

    /** The status text colour: the tone of the status, always written out too. */
    fun statusColor(status: MonarchAiComputerViewStatus, colors: MonarchColors): Color = when (status) {
        MonarchAiComputerViewStatus.Failed -> colors.dangerStrong
        MonarchAiComputerViewStatus.Done -> colors.successStrong
        MonarchAiComputerViewStatus.WaitingForYou -> colors.warningStrong
        else -> colors.textDefault
    }

    fun mutedColor(colors: MonarchColors): Color = colors.textMuted

    fun textColor(colors: MonarchColors): Color = colors.textDefault

    /** The colours of the markers over the screenshot. */
    fun markerColors(colors: MonarchColors): AiComputerViewMarkerColors = AiComputerViewMarkerColors(
        last = colors.aiSolid,
        selected = colors.accentSolid,
        pointer = colors.textDefault,
        pointerOutline = colors.bgSurface,
    )

    /** The fill of a logged action: the accent subtle fill when selected. */
    fun rowColor(selected: Boolean, colors: MonarchColors): Color =
        if (selected) colors.accentSubtle else colors.bgSurface

    /** The bar at the start of a logged action: the accent when selected, so selection is also a shape. */
    fun barColor(selected: Boolean, colors: MonarchColors): Color =
        if (selected) colors.accentSolid else colors.bgSurface
}
