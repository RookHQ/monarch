package dev.monarchui.components.datagrid.duplicatepanel

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.internal.MonarchSurfaceColors
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens
import kotlin.math.roundToInt

/** Component tokens of MonarchDuplicatePanel, built from primitives and roles only. */
internal object MonarchDuplicatePanelTokens {
    val HintPadding: Dp = MonarchTokens.Space.space3.dp
    val SectionGap: Dp = MonarchTokens.Space.space2.dp
    val RowPadding: Dp = MonarchTokens.Space.space1_5.dp
    val ActionGap: Dp = MonarchTokens.Space.space3.dp
    val LineGap: Dp = MonarchTokens.Space.space1.dp
    val HeadingGap: Dp = MonarchTokens.Space.space1_5.dp
    private const val PERCENT = 100

    /** The box of the hint variant: the warning tint and border, as on the web. */
    fun hintColors(colors: MonarchColors): MonarchSurfaceColors =
        MonarchSurfaceColors(colors.warningSubtle, colors.warningStrong, colors.warningBorder)

    /** The heading colour: the strong warning colour in the hint, the default text colour in the panel. */
    fun headingColor(variant: MonarchDuplicatePanelVariant, colors: MonarchColors): Color = when (variant) {
        MonarchDuplicatePanelVariant.Hint -> colors.warningStrong
        MonarchDuplicatePanelVariant.Panel -> colors.textDefault
    }

    /** A score from 0 to 1 as a whole percentage, clamped. */
    fun percent(score: Float): Int = (score.coerceIn(0f, 1f) * PERCENT).roundToInt()
}
