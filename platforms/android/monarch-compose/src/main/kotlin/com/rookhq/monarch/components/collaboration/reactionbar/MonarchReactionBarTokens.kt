package com.rookhq.monarch.components.collaboration.reactionbar

import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.foundation.MonarchTypography
import com.rookhq.monarch.internal.MonarchSurfaceColors
import com.rookhq.monarch.internal.MonarchTonePalette
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens

/** Component tokens and the pure rules of MonarchReactionBar. */
internal object MonarchReactionBarTokens {
    /** The space between chips, across and down. */
    val Gap: Dp = MonarchTokens.Space.space1.dp

    /** The space between the emoji and the count inside a chip. */
    val ContentGap: Dp = MonarchTokens.Space.space1.dp

    /** The space inside a read-only chip at the start and end. */
    val HorizontalPadding: Dp = MonarchTokens.Space.space2.dp

    /** The border of a chip you have not reacted with. */
    val BorderWidth: Dp = MonarchTokens.Space.px.dp

    /** The border of a chip you reacted with: thicker, so the state is not colour alone. */
    val ReactedBorderWidth: Dp = MonarchTokens.Space.space0_5.dp

    /** The visible minimum height of a chip. Text that grows makes it taller. */
    fun minHeight(size: MonarchReactionBarSize): Dp = when (size) {
        MonarchReactionBarSize.Sm -> MonarchTokens.Control.xs.dp
        MonarchReactionBarSize.Md -> MonarchTokens.Control.sm.dp
    }

    /** The text style of the emoji and the count. */
    fun textStyle(size: MonarchReactionBarSize, typography: MonarchTypography): TextStyle = when (size) {
        MonarchReactionBarSize.Sm -> typography.label
        MonarchReactionBarSize.Md -> typography.body
    }

    /** The colours of a chip: the accent when reacted, the subtle fill otherwise, the disabled text when off. */
    fun colors(reacted: Boolean, enabled: Boolean, colors: MonarchColors): MonarchSurfaceColors = when {
        !enabled -> MonarchSurfaceColors(colors.bgMuted, colors.textDisabled, colors.borderSubtle)
        reacted -> MonarchTonePalette.of(MonarchTone.Accent, colors).let {
            MonarchSurfaceColors(it.subtle, it.strong, it.border)
        }
        else -> MonarchSurfaceColors(colors.bgSubtle, colors.textDefault, colors.borderSubtle)
    }

    /** The border of a chip from its colours. */
    fun border(reacted: Boolean, surface: MonarchSurfaceColors): BorderStroke =
        BorderStroke(if (reacted) ReactedBorderWidth else BorderWidth, surface.border ?: surface.container)

    /** The chips shown: reactions with a count above 0, each emoji once, in the given order. */
    fun shown(reactions: List<MonarchReactionBarItem>): List<MonarchReactionBarItem> =
        reactions.filter { it.count > 0 }.distinctBy { it.emoji }

    /** Whether the add button is shown. */
    fun canAdd(
        interactive: Boolean,
        addable: Boolean,
        shownCount: Int,
        maxReactions: Int?,
        hasSource: Boolean,
    ): Boolean =
        interactive && addable && hasSource && (maxReactions == null || shownCount < maxReactions)
}
