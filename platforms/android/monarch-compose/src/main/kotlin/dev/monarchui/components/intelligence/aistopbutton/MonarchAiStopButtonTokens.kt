package dev.monarchui.components.intelligence.aistopbutton

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens

/** Component tokens of MonarchAiStopButton, built from primitives and roles only. */
internal object MonarchAiStopButtonTokens {
    /** The gap between the stopped text and the Continue button. */
    val Gap: Dp = MonarchTokens.Space.space2.dp

    /** The corner of the stop square: the web's 1.5 in a 9-unit square. */
    val GlyphShape: Shape = RoundedCornerShape(MonarchTokens.Space.space0_5.dp)

    /** The side of the stop square at each size. */
    fun glyphSize(size: MonarchButtonSize): Dp = when (size) {
        MonarchButtonSize.Sm -> MonarchTokens.Space.space2_5.dp
        MonarchButtonSize.Md -> MonarchTokens.Space.space3.dp
        MonarchButtonSize.Lg -> MonarchTokens.Space.space3.dp
    }

    /** The colour of the stopped text. */
    fun stoppedTextColor(colors: MonarchColors): Color = colors.textMuted
}
