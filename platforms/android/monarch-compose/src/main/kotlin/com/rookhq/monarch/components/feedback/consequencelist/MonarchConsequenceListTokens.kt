package com.rookhq.monarch.components.feedback.consequencelist

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens

/** Component tokens of MonarchConsequenceList, built from primitives and roles only. */
internal object MonarchConsequenceListTokens {
    val SectionGap: Dp = MonarchTokens.Space.space3.dp
    val GroupGap: Dp = MonarchTokens.Space.space1_5.dp
    val ItemGap: Dp = MonarchTokens.Space.space2.dp
    val DetailGap: Dp = MonarchTokens.Space.space0_5.dp
    val IconSize: Dp = MonarchTokens.Space.space4.dp
    val SkeletonRowGap: Dp = MonarchTokens.Space.space2_5.dp
    const val DEFAULT_LOADING_ROWS: Int = 3

    /** Widths of the placeholder lines while loading, as fractions of the row, so the rows differ. */
    val SkeletonWidths: List<Float> = listOf(0.7f, 0.55f, 0.8f)

    /** The groups of a grouped list, in the order they are shown. */
    val GroupOrder: List<MonarchConsequenceKind> = listOf(
        MonarchConsequenceKind.Removed,
        MonarchConsequenceKind.Changed,
        MonarchConsequenceKind.Warning,
        MonarchConsequenceKind.Kept,
    )

    /** The icon colour of a kind: danger for removed, success for kept, warning for warnings, muted for changes. */
    fun iconColor(kind: MonarchConsequenceKind, colors: MonarchColors): Color = when (kind) {
        MonarchConsequenceKind.Removed -> colors.dangerStrong
        MonarchConsequenceKind.Changed -> colors.textMuted
        MonarchConsequenceKind.Warning -> colors.warningStrong
        MonarchConsequenceKind.Kept -> colors.successStrong
    }

    /** The count of a removed item is in the danger colour; every other count is default text. */
    fun countColor(kind: MonarchConsequenceKind, colors: MonarchColors): Color =
        if (kind == MonarchConsequenceKind.Removed) colors.dangerStrong else colors.textDefault
}
