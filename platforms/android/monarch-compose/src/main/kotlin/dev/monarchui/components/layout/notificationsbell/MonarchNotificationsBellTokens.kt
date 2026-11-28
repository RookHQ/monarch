package dev.monarchui.components.layout.notificationsbell

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.accessibility.MonarchTouchTarget
import dev.monarchui.internal.MonarchSurfaceColors
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens

/** Component tokens of MonarchNotificationsBell, built from primitives and roles only. */
internal object MonarchNotificationsBellTokens {
    /** The largest count the badge shows in digits; above it the badge reads "99+". */
    const val MAX_SHOWN: Int = 99

    val RowMinHeight: Dp = MonarchTouchTarget.Minimum
    val RowHorizontalPadding: Dp = MonarchTokens.Space.space3.dp
    val RowGap: Dp = MonarchTokens.Space.space3.dp

    /** The text of the badge: the count, or "99+" above [MAX_SHOWN]. */
    fun badgeText(count: Int): String = if (count > MAX_SHOWN) "$MAX_SHOWN+" else count.toString()

    /** Whether a badge is shown: only for a positive count. */
    fun showsBadge(count: Int): Boolean = count > 0

    /** The badge colours: the solid danger colour, as on the web. */
    fun badgeColors(colors: MonarchColors): MonarchSurfaceColors =
        MonarchSurfaceColors(container = colors.dangerSolid, content = colors.dangerOnSolid, border = null)

    /** The colour of the bell and the row title. */
    fun contentColor(enabled: Boolean, colors: MonarchColors): Color =
        if (enabled) colors.textDefault else colors.textDisabled
}
