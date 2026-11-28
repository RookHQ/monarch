package dev.monarchui.components.layout.comparisonsection

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens

/** Component tokens of MonarchComparisonSection, built from primitives and roles only. */
internal object MonarchComparisonSectionTokens {
    val Gap: Dp = MonarchTokens.Space.space6.dp
    val CardGap: Dp = MonarchTokens.Space.space3.dp
    val CardPadding: Dp = MonarchTokens.Space.space5.dp
    val TextGap: Dp = MonarchTokens.Space.space2.dp
    val NotePadding: Dp = MonarchTokens.Space.space4.dp
    val MinCardWidth: Dp = (MonarchTokens.Space.space24 + MonarchTokens.Space.space16).dp

    /** A best-for card: the subtle accent with its border when highlighted, the surface with the default border. */
    fun cardFill(highlighted: Boolean, colors: MonarchColors): Color =
        if (highlighted) colors.accentSubtle else colors.bgSurface

    fun cardBorder(highlighted: Boolean, colors: MonarchColors): Color =
        if (highlighted) colors.accentBorder else colors.borderDefault

    /** The note sits on the subtle background in the muted text colour. */
    fun noteFill(colors: MonarchColors): Color = colors.bgSubtle
    fun noteText(colors: MonarchColors): Color = colors.textMuted
}
