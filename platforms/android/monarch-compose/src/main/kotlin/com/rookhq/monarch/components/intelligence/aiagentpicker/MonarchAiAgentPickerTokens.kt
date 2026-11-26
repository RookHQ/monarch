package com.rookhq.monarch.components.intelligence.aiagentpicker

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens

/** Component tokens and the pure rules of MonarchAiAgentPicker. */
internal object MonarchAiAgentPickerTokens {
    val Gap: Dp = MonarchTokens.Space.space2.dp
    val MarkGap: Dp = MonarchTokens.Space.space1.dp
    val SelectedBorderWidth: Dp = MonarchTokens.Space.px.dp
    /** The opacity of a card that cannot be chosen: it stays visible and reads as unavailable. */
    const val DISABLED_ALPHA: Float = 0.5f
    const val GRID_COLUMNS: Int = 2

    /** Below this width the grid is one column, as the web's 16rem cards do on a phone. */
    val SingleColumnBelow: Dp = MonarchTokens.Breakpoint.sm.dp

    /** From this font scale the grid is one column. */
    const val SINGLE_COLUMN_FONT_SCALE: Float = 1.5f

    /** The number of columns for [layout] at [width] and [fontScale]. */
    fun columns(layout: MonarchAiAgentPickerLayout, width: Dp, fontScale: Float): Int = when {
        layout == MonarchAiAgentPickerLayout.List -> 1
        width < SingleColumnBelow || fontScale >= SINGLE_COLUMN_FONT_SCALE -> 1
        else -> GRID_COLUMNS
    }

    /** Whether the option can be chosen: the picker and the option are enabled. */
    fun choosable(option: MonarchAiAgentPickerOption, enabled: Boolean): Boolean = enabled && option.enabled

    /** The border drawn over the chosen card: the AI accent, so the choice also shows as a filled radio mark. */
    fun selectedBorder(colors: MonarchColors): Color = colors.aiSolid

    /** The colour of the empty message. */
    fun emptyColor(colors: MonarchColors): Color = colors.textMuted
}
