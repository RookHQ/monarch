package com.rookhq.monarch.components.intelligence.aiconversationheader

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.internal.MonarchShapeDefaults
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens

/** Component tokens and the pure logic of MonarchAiConversationHeader. */
internal object MonarchAiConversationHeaderTokens {
    /** The least height of the bar, the height of a top app bar. */
    val MinHeight: Dp = MonarchTokens.Space.space16.dp

    /** The padding at the start and end of the bar. */
    val HorizontalPadding: Dp = MonarchTokens.Space.space2.dp

    /** The padding above and below the content. */
    val VerticalPadding: Dp = MonarchTokens.Space.space2.dp

    /** The gap between the navigation icon, the title block and the actions. */
    val Gap: Dp = MonarchTokens.Space.space2.dp

    /** The gap inside the title row and the meta row. */
    val InnerGap: Dp = MonarchTokens.Space.space1.dp

    /** The width of the line under the bar. */
    val BorderWidth: Dp = MonarchShapeDefaults.BorderWidth

    /** The bar's fill. */
    fun container(colors: MonarchColors): Color = colors.bgSurface

    /** The line under the bar. */
    fun border(colors: MonarchColors): Color = colors.borderSubtle

    /** The title: muted while it is a placeholder (untitled or being named). */
    fun titleColor(placeholder: Boolean, colors: MonarchColors): Color =
        if (placeholder) colors.textMuted else colors.textDefault

    /** The assistant's name and the model. */
    fun metaColor(colors: MonarchColors): Color = colors.textMuted

    /** The title shown: the trimmed title, or [untitled] when it is empty. */
    fun shownTitle(title: String, untitled: String): String = title.trim().ifEmpty { untitled }

    /** Whether the title can be renamed now. */
    fun renamable(hasRename: Boolean, naming: Boolean): Boolean = hasRename && !naming
}
