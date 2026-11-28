package dev.monarchui.components.identity.devicelist

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.internal.MonarchRelativeTime
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens
import java.util.Locale

/** Component tokens and the pure logic of MonarchDeviceList. */
internal object MonarchDeviceListTokens {
    /** The separator between the parts of a line. */
    const val SEPARATOR: String = " · "

    /** The gap between the header, the notice and the list, and between rows. */
    val SectionGap: Dp = MonarchTokens.Space.space4.dp

    /** The gap between the icon, the text and the actions, and between badges. */
    val RowGap: Dp = MonarchTokens.Space.space3.dp

    /** The side of the square tile behind a device's icon. */
    val IconTile: Dp = MonarchTokens.Space.space8.dp

    /** The gap between the lines of a row. */
    val LineGap: Dp = MonarchTokens.Space.space1.dp

    /** The colour of the device icon: the danger strong colour when blocked, muted otherwise. */
    fun iconColor(blocked: Boolean, colors: MonarchColors): Color =
        if (blocked) colors.dangerStrong else colors.textMuted

    /** [date] relative to [now] by the shared Monarch rule (the web rule): "30 seconds ago", "5 minutes ago". */
    fun relativeTime(date: Long, now: Long, locale: Locale = Locale.getDefault()): String =
        MonarchRelativeTime.format(date, now, locale)

    /** The name shown: [name], or [unnamed] when it is blank. */
    fun displayName(name: String, unnamed: String): String = name.ifBlank { unnamed }

    /** The parts that exist, joined with the separator, or `null` when there are none. */
    fun line(vararg parts: String?): String? =
        parts.filterNotNull().filter { it.isNotBlank() }.takeIf { it.isNotEmpty() }?.joinToString(SEPARATOR)

    /** What TalkBack reads after a device's name: its badges, the detail line and the times. */
    fun spokenDetails(badges: List<String>, detail: String?, times: String?): String =
        (badges + listOfNotNull(detail, times)).joinToString(", ")
}
