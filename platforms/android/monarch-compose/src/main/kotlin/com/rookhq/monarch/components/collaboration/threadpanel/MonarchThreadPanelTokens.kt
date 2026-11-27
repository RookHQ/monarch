package com.rookhq.monarch.components.collaboration.threadpanel

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens

/** Component tokens and pure logic of MonarchThreadPanel, built from primitives and roles only. */
internal object MonarchThreadPanelTokens {
    val Inset: Dp = MonarchTokens.Space.space4.dp
    val Gap: Dp = MonarchTokens.Space.space2.dp

    /** The time the replies stay on screen before `onRepliesSeen`, as on the web. */
    const val SEEN_DELAY_MILLIS: Long = 400L

    /** The placeholder rows while the thread loads. */
    const val PLACEHOLDER_ROWS: Int = 3

    /** Whether the follow chip is shown: the app can change it and the thread is on screen. */
    fun showsFollow(canFollow: Boolean, status: MonarchThreadPanelStatus): Boolean =
        canFollow && status == MonarchThreadPanelStatus.Ready

    /** What is announced after a follow change: the failure, or the new state. */
    fun followNotice(succeeded: Boolean, following: Boolean, followed: String, unfollowed: String, failed: String) =
        when {
            !succeeded -> failed
            following -> followed
            else -> unfollowed
        }

    /** The colour of the reply count, the notice and the deleted text. */
    fun mutedColor(colors: MonarchColors): Color = colors.textMuted

    /** The colour of the heading. */
    fun titleColor(colors: MonarchColors): Color = colors.textDefault

    /** The colour of a loading error without a retry. */
    fun errorColor(colors: MonarchColors): Color = colors.dangerStrong
}
