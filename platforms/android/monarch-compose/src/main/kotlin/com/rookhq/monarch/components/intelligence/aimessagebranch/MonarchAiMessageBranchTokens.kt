package com.rookhq.monarch.components.intelligence.aimessagebranch

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens

/** Component tokens of MonarchAiMessageBranch, built from primitives and roles only. */
internal object MonarchAiMessageBranchTokens {
    /** The smallest width of the position, so the buttons do not move between "1 / 3" and "2 / 3". */
    val PositionMinWidth: Dp = MonarchTokens.Space.space10.dp

    /** The colour of the position and of an enabled button. */
    fun contentColor(enabled: Boolean, colors: MonarchColors): Color =
        if (enabled) colors.textMuted else colors.textDisabled

    /** The number of versions, at least 1. */
    fun total(count: Int): Int = maxOf(1, count)

    /** The version shown: [selection] clamped into the versions that exist, counted from 0. */
    fun current(selection: Int, count: Int): Int = selection.coerceIn(0, total(count) - 1)

    /** Whether the switcher is drawn at all. */
    fun visible(count: Int, showSingle: Boolean): Boolean = total(count) >= 2 || showSingle
}
