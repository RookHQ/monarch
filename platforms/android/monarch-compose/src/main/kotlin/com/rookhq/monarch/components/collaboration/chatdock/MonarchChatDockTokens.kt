package com.rookhq.monarch.components.collaboration.chatdock

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens

/** Component tokens and the pure logic of MonarchChatDock. */
internal object MonarchChatDockTokens {
    val LoadingPadding: Dp = MonarchTokens.Space.space6.dp
    val LoadingGap: Dp = MonarchTokens.Space.space3.dp

    /** The most unread messages written in the badge; more are written as "99+". */
    const val MAX_BADGE_COUNT: Int = 99

    /** The launcher is the inverse surface, as the web's dark launcher; its count badge is the danger solid. */
    fun launcherFill(colors: MonarchColors): Color = colors.bgInverse
    fun launcherContent(colors: MonarchColors): Color = colors.textInverse
    fun badgeFill(colors: MonarchColors): Color = colors.dangerSolid
    fun badgeContent(colors: MonarchColors): Color = colors.dangerOnSolid

    /** The text in the badge: nothing without unread messages, the count, or "99+". */
    fun badgeText(unread: Int): String? =
        when {
            unread <= 0 -> null
            unread > MAX_BADGE_COUNT -> "$MAX_BADGE_COUNT+"
            else -> unread.toString()
        }
}
