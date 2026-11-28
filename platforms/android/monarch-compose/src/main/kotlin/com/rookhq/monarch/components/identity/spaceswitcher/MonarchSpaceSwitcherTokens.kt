package com.rookhq.monarch.components.identity.spaceswitcher

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens

/** Component tokens of MonarchSpaceSwitcher, from primitives and roles only. */
internal object MonarchSpaceSwitcherTokens {
    /** The space between the icon, the text and the arrow of the button. */
    val ButtonGap: Dp = MonarchTokens.Space.space2.dp

    /** The sheet: the raised surface. */
    fun sheetColor(colors: MonarchColors): Color = colors.bgRaised

    /** A row's name: default text. */
    fun nameColor(colors: MonarchColors): Color = colors.textDefault

    /** A row's detail, the button caption and messages: muted text. */
    fun detailColor(colors: MonarchColors): Color = colors.textMuted
}
