package dev.monarchui.components.intelligence.aisharedsession

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.internal.MonarchSurfaceColors
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens

/** Component tokens and colours of MonarchAiSharedSession. */
internal object MonarchAiSharedSessionTokens {
    val Gap: Dp = MonarchTokens.Space.space2.dp
    val SmallGap: Dp = MonarchTokens.Space.space1.dp
    val NoticePadding: Dp = MonarchTokens.Space.space3.dp
    val RequestPadding: Dp = MonarchTokens.Space.space2.dp
    val ListPadding: Dp = MonarchTokens.Space.space4.dp

    /** The status line while something happens. */
    fun statusColor(colors: MonarchColors): Color = colors.textDefault

    /** The status line when nothing happens. */
    fun quietColor(colors: MonarchColors): Color = colors.textSubtle

    /** Names. */
    fun nameColor(colors: MonarchColors): Color = colors.textDefault

    /** Details, previews and "Access requested". */
    fun detailColor(colors: MonarchColors): Color = colors.textMuted

    /** The viewer's notice: the muted fill with default text. */
    fun noticeColors(colors: MonarchColors): MonarchSurfaceColors =
        MonarchSurfaceColors(container = colors.bgMuted, content = colors.textDefault, border = null)

    /** A request in the queue: yours on the subtle AI fill with the AI border, others on the subtle fill. */
    fun requestColors(own: Boolean, colors: MonarchColors): MonarchSurfaceColors = if (own) {
        MonarchSurfaceColors(container = colors.aiSubtle, content = colors.textDefault, border = colors.aiBorder)
    } else {
        MonarchSurfaceColors(container = colors.bgSubtle, content = colors.textDefault, border = colors.borderSubtle)
    }
}
