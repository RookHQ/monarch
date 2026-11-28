package com.rookhq.monarch.components.identity.workspaceswitcher

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens

/** Component tokens of MonarchWorkspaceSwitcher, from primitives and roles only. */
internal object MonarchWorkspaceSwitcherTokens {
    /** The space between the logo, the text and the arrow of the button. */
    val ButtonGap: Dp = MonarchTokens.Space.space2.dp

    /** The handle: muted text. */
    fun handleColor(colors: MonarchColors): Color = colors.textMuted
}
