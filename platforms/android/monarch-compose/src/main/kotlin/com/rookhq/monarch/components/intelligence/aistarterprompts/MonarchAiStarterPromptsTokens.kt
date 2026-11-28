package com.rookhq.monarch.components.intelligence.aistarterprompts

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.internal.MonarchControlState
import com.rookhq.monarch.internal.MonarchSurfaceColors
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens

/** Component tokens of MonarchAiStarterPrompts, built from primitives and roles only. */
internal object MonarchAiStarterPromptsTokens {
    /** The smallest width of a card in the grid: the web's 13rem. */
    val MinCellWidth: Dp = (MonarchTokens.Space.space24 * 2 + MonarchTokens.Space.space4).dp

    /** The gap between cards in the grid. */
    val GridGap: Dp = MonarchTokens.Space.space2.dp

    /** The gap between rows in the list. */
    val ListGap: Dp = MonarchTokens.Space.space1_5.dp

    /** The gap between the icon and the title, and between the title and the prompt. */
    val ContentGap: Dp = MonarchTokens.Space.space1.dp

    /** The gap between the icon and the title. */
    val IconGap: Dp = MonarchTokens.Space.space2.dp

    /** The smallest height of a starter. */
    val MinHeight: Dp = MonarchTokens.Space.space12.dp

    /** The lines of the prompt before it is cut. TalkBack reads all of it. */
    const val PROMPT_MAX_LINES: Int = 2

    /** From this font scale on, the grid is one column: two do not fit a phone. */
    const val ONE_COLUMN_FONT_SCALE: Float = 1.5f

    /** The padding inside a starter. */
    fun contentPadding(layout: MonarchAiStarterPromptsLayout): PaddingValues = when (layout) {
        MonarchAiStarterPromptsLayout.Grid -> PaddingValues(MonarchTokens.Space.space3.dp)
        MonarchAiStarterPromptsLayout.List -> PaddingValues(
            horizontal = MonarchTokens.Space.space3.dp,
            vertical = MonarchTokens.Space.space2.dp,
        )
    }

    /** The number of columns for the available width. */
    fun columns(layout: MonarchAiStarterPromptsLayout, maxWidth: Dp, fontScale: Float): Int = when {
        layout == MonarchAiStarterPromptsLayout.List -> 1
        fontScale >= ONE_COLUMN_FONT_SCALE -> 1
        else -> maxOf(1, ((maxWidth + GridGap) / (MinCellWidth + GridGap)).toInt())
    }

    /** The fill, border and text colour of a starter. Pressed takes the subtle assistant fill and border. */
    fun colors(state: MonarchControlState, colors: MonarchColors): MonarchSurfaceColors = state.pick(
        resting = MonarchSurfaceColors(colors.bgSurface, colors.textDefault, colors.borderDefault),
        disabled = MonarchSurfaceColors(colors.bgSurface, colors.textDisabled, colors.borderSubtle),
        pressed = MonarchSurfaceColors(colors.aiSubtleHover, colors.textDefault, colors.aiBorder),
        hovered = MonarchSurfaceColors(colors.aiSubtle, colors.textDefault, colors.aiBorder),
    )

    /** The colour of the prompt under the title. */
    fun promptColor(enabled: Boolean, colors: MonarchColors) = if (enabled) colors.textMuted else colors.textDisabled

    /** The colour of the icon. */
    fun iconColor(enabled: Boolean, colors: MonarchColors) = if (enabled) colors.aiStrong else colors.textDisabled
}
