package dev.monarchui.components.intelligence.aimemorychip

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.components.intelligence.aiconversation.MonarchAiMemoryAction
import dev.monarchui.components.intelligence.aiconversation.MonarchAiMemoryPart
import dev.monarchui.internal.MonarchSurfaceColors
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens

/** Component tokens and the pure logic of MonarchAiMemoryChip. */
internal object MonarchAiMemoryChipTokens {
    val ChipHorizontalPadding: Dp = MonarchTokens.Space.space3.dp
    val ChipVerticalPadding: Dp = MonarchTokens.Space.space1.dp
    val RowHorizontalPadding: Dp = MonarchTokens.Space.space3.dp
    val RowVerticalPadding: Dp = MonarchTokens.Space.space2.dp
    val ContentGap: Dp = MonarchTokens.Space.space2.dp
    val LineGap: Dp = MonarchTokens.Space.space0_5.dp
    val ChipMinHeight: Dp = MonarchTokens.Control.sm.dp

    /** Whether the memory offers Edit and Forget: a callback is given and the memory is not forgotten. */
    fun actionable(memory: MonarchAiMemoryPart, canEdit: Boolean, canForget: Boolean): Boolean =
        memory.action != MonarchAiMemoryAction.Forget && (canEdit || canForget)

    /** Whether the scope line is shown: by default on the chip and not on the row; never when blank. */
    fun showsScope(memory: MonarchAiMemoryPart, variant: MonarchAiMemoryChipVariant, showScope: Boolean?): Boolean =
        !memory.scope.isNullOrBlank() && (showScope ?: (variant == MonarchAiMemoryChipVariant.Chip))

    /** Whether the prefix is shown: always on the chip; on the row only for a forgotten memory. */
    fun showsPrefix(memory: MonarchAiMemoryPart, variant: MonarchAiMemoryChipVariant): Boolean =
        variant == MonarchAiMemoryChipVariant.Chip || memory.action == MonarchAiMemoryAction.Forget

    /** The text a save would send, or `null` when it is empty or unchanged. */
    fun edited(original: String, draft: String): String? =
        draft.trim().takeIf { it.isNotEmpty() && it != original.trim() }

    /** The fill, text and border of the chip or the row. A forgotten memory is muted. */
    fun colors(
        variant: MonarchAiMemoryChipVariant,
        action: MonarchAiMemoryAction,
        colors: MonarchColors,
    ): MonarchSurfaceColors = when {
        action == MonarchAiMemoryAction.Forget ->
            MonarchSurfaceColors(container = colors.bgMuted, content = colors.textMuted, border = colors.borderSubtle)
        variant == MonarchAiMemoryChipVariant.Chip ->
            MonarchSurfaceColors(container = colors.aiSubtle, content = colors.textDefault, border = colors.aiBorder)
        else ->
            MonarchSurfaceColors(
                container = colors.bgSurface,
                content = colors.textDefault,
                border = colors.borderSubtle,
            )
    }
}
