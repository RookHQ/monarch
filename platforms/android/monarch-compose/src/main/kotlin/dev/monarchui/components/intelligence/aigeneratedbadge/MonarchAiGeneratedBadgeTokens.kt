package dev.monarchui.components.intelligence.aigeneratedbadge

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.internal.MonarchSurfaceColors
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens

/** Component tokens and the pure logic of MonarchAiGeneratedBadge. */
internal object MonarchAiGeneratedBadgeTokens {
    val BorderWidth: Dp = MonarchTokens.Space.px.dp
    val ContentGap: Dp = MonarchTokens.Space.space1.dp
    const val FONT_WEIGHT: Int = MonarchTokens.FontWeight.medium

    /** The length of a dash and of a gap of the edited border. */
    val Dash: Dp = MonarchTokens.Space.space1.dp

    fun minHeight(size: MonarchAiGeneratedBadgeSize): Dp = when (size) {
        MonarchAiGeneratedBadgeSize.Sm -> MonarchTokens.Space.space5.dp
        MonarchAiGeneratedBadgeSize.Md -> MonarchTokens.Space.space6.dp
    }

    fun horizontalPadding(size: MonarchAiGeneratedBadgeSize, iconOnly: Boolean): Dp = when {
        iconOnly -> MonarchTokens.Space.space1.dp
        size == MonarchAiGeneratedBadgeSize.Sm -> MonarchTokens.Space.space1_5.dp
        else -> MonarchTokens.Space.space2.dp
    }

    fun iconSize(size: MonarchAiGeneratedBadgeSize): Dp = when (size) {
        MonarchAiGeneratedBadgeSize.Sm -> MonarchTokens.Space.space3.dp
        MonarchAiGeneratedBadgeSize.Md -> MonarchTokens.Space.space4.dp
    }

    /** The AI roles: subtle fill, strong text and icon, AI border, in every theme. */
    fun colors(colors: MonarchColors): MonarchSurfaceColors =
        MonarchSurfaceColors(container = colors.aiSubtle, content = colors.aiStrong, border = colors.aiBorder)

    /** The edited kind has a dashed border, so it differs by form and not only by its words. */
    fun dashed(kind: MonarchAiGeneratedBadgeKind): Boolean = kind == MonarchAiGeneratedBadgeKind.Edited

    /** The model to name in the provenance line: `null` for none or blank text. */
    fun shownModel(model: String?): String? = model?.trim()?.takeIf { it.isNotEmpty() }
}
