package dev.monarchui.components.technical.embeddedappframe

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens

/** Component tokens of MonarchEmbeddedAppFrame, built from primitives and roles only. */
internal object MonarchEmbeddedAppFrameTokens {
    /** The least height while loading or after an error, so the frame does not jump to nothing. */
    val MinHeight: Dp = (MonarchTokens.Space.space24 + MonarchTokens.Space.space16).dp
    val Padding: Dp = MonarchTokens.Space.space5.dp
    val Gap: Dp = MonarchTokens.Space.space3.dp
    val TextGap: Dp = MonarchTokens.Space.space1.dp
    val ActionGap: Dp = MonarchTokens.Space.space2.dp

    /** The frame: the surface with the default border, as the web's embedded page. */
    fun fill(colors: MonarchColors): Color = colors.bgSurface
    fun border(colors: MonarchColors): Color = colors.borderDefault
    fun muted(colors: MonarchColors): Color = colors.textMuted
}
