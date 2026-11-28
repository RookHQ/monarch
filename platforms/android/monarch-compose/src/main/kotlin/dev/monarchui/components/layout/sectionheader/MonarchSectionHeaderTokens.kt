package dev.monarchui.components.layout.sectionheader

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.foundation.MonarchTypography
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.internal.MonarchTonePalette
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens

/** Component tokens of MonarchSectionHeader, built from primitives and roles only. */
internal object MonarchSectionHeaderTokens {
    /** The gap between the text and the actions, across or down. */
    val Gap: Dp = MonarchTokens.Space.space3.dp

    /** The gap between the title, the description and the meta line. */
    val TextGap: Dp = MonarchTokens.Space.space1.dp

    /** The gap between two actions. */
    val ActionGap: Dp = MonarchTokens.Space.space2.dp

    /** The gap between the title and the busy ring. */
    val BusyGap: Dp = MonarchTokens.Space.space2.dp

    /** The diameter of the busy ring at the default font size; it grows with the font size setting. */
    val BusySize: Dp = MonarchTokens.Space.space3.dp
    val BusyStroke: Dp = MonarchTokens.Space.space0_5.dp

    /** The share of the still ring drawn instead of the spinning one when animations are off. */
    const val STILL_BUSY_FRACTION: Float = 0.75f

    /** The largest share of the width the actions may take beside the text before they move below it. */
    const val MAX_ACTIONS_SHARE: Float = 0.5f

    /** The least of the row the title keeps beside the actions; below it the actions go under the title. */
    const val MIN_TEXT_SHARE: Float = 0.33f

    /** The title style: label size for `Sm`, body size for `Md`, both semibold. */
    fun titleStyle(size: MonarchSectionHeaderSize, typography: MonarchTypography): TextStyle = when (size) {
        MonarchSectionHeaderSize.Sm -> typography.label.copy(fontWeight = FontWeight(MonarchTokens.FontWeight.semibold))
        MonarchSectionHeaderSize.Md -> typography.bodyStrong
    }

    /** The description style: label size at regular weight. */
    fun descriptionStyle(typography: MonarchTypography): TextStyle =
        typography.label.copy(fontWeight = FontWeight(MonarchTokens.FontWeight.regular))

    /** The meta line style: caption. */
    fun metaStyle(typography: MonarchTypography): TextStyle = typography.caption

    /** The colour of the title. */
    fun titleColor(colors: MonarchColors, tone: MonarchTone? = null): Color = when (tone) {
        null, MonarchTone.Neutral -> colors.textDefault
        else -> MonarchTonePalette.of(tone, colors).strong
    }

    /** The colour of the description and the meta line: quieter than the title. */
    fun secondaryColor(colors: MonarchColors): Color = colors.textMuted

    /**
     * Whether the actions are placed below the text instead of beside it: when
     * they would take more than [MAX_ACTIONS_SHARE] of [maxWidth]. An unbounded
     * width never stacks.
     */
    fun stacksActions(maxWidth: Int, actionsWidth: Int, gap: Int, bounded: Boolean = true): Boolean =
        bounded && actionsWidth > 0 && actionsWidth + gap > maxWidth * MAX_ACTIONS_SHARE
}
